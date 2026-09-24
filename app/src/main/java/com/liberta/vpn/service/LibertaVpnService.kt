package com.liberta.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.system.Os
import android.util.Log
import androidx.core.app.NotificationCompat
import com.liberta.vpn.LibertaApplication
import com.liberta.vpn.MainActivity
import com.liberta.vpn.R
import com.liberta.vpn.data.ConnectionPhase
import com.liberta.vpn.data.ConnectionMethod
import com.liberta.vpn.data.ConnectionProfile
import com.liberta.vpn.data.LibertaSettings
import com.liberta.vpn.data.ServerCandidate
import com.liberta.vpn.data.SubscriptionSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import java.net.InetAddress
import java.net.InetSocketAddress
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.math.min

class LibertaVpnService : VpnService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var connectJob: Job? = null
    private var monitorJob: Job? = null
    private var detachedTunFd: Int? = null
    private var phantomSession: PhantomCallSession? = null
    private var compactNotification: Boolean = true

    private val container by lazy {
        (application as LibertaApplication).container
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val profile = ConnectionProfile.fromName(intent.getStringExtra(EXTRA_PROFILE))
                val method = ConnectionMethod.fromName(intent.getStringExtra(EXTRA_METHOD)).let { parsed ->
                    if (intent.hasExtra(EXTRA_METHOD)) parsed else ConnectionMethod.fromProfile(profile)
                }
                connect(
                    profile,
                    method,
                    forceRefresh = intent.getBooleanExtra(EXTRA_FORCE_REFRESH, false),
                    phantomCall = intent.optionalBooleanExtra(EXTRA_PHANTOM_CALL),
                    meshAccess = intent.optionalBooleanExtra(EXTRA_MESH_ACCESS),
                    sovereignRelay = intent.optionalBooleanExtra(EXTRA_SOVEREIGN_RELAY)
                )
            }
            ACTION_RECOVER -> {
                val profile = LibertaRuntime.status.value.profile
                val method = LibertaRuntime.status.value.connectionMethod
                connect(profile, method, forceRefresh = true, recovering = true)
            }
            ACTION_DISCONNECT -> disconnect()
        }
        return START_STICKY
    }

    override fun onRevoke() {
        disconnect()
        super.onRevoke()
    }

    override fun onDestroy() {
        disconnect()
        scope.cancel()
        super.onDestroy()
    }

    private fun connect(
        profile: ConnectionProfile,
        method: ConnectionMethod,
        forceRefresh: Boolean,
        recovering: Boolean = false,
        phantomCall: Boolean? = null,
        meshAccess: Boolean? = null,
        sovereignRelay: Boolean? = null
    ) {
        connectJob?.cancel()
        connectJob = scope.launch {
            val connectStartedMs = SystemClock.elapsedRealtime()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, buildNotification("Liberta запускается"), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, buildNotification("Liberta запускается"))
            }
            if (recovering) {
                LibertaRuntime.update(ConnectionPhase.RECOVERING, method, profile, "Восстановление туннеля")
            }
            runCatching {
                stopCoreOnly()
                val settings = container.settingsRepository.settings.first().let { saved ->
                    val resolvedMethod = if (meshAccess == true) ConnectionMethod.MESH_ACCESS else method
                    saved.copy(
                        connectionMethod = resolvedMethod,
                        profile = profile,
                        labs = saved.labs.copy(
                            phantomCall = phantomCall
                                ?: (resolvedMethod == ConnectionMethod.PHANTOM_CALL || saved.labs.phantomCall),
                            sovereignRelay = sovereignRelay ?: saved.labs.sovereignRelay
                        )
                    )
                }
                compactNotification = settings.compactNotification
                updateNotification("Liberta запускается")
                val phantomMode = settings.connectionMethod == ConnectionMethod.PHANTOM_CALL
                if (phantomMode) {
                    LibertaRuntime.update(
                        ConnectionPhase.CONNECTING,
                        settings.connectionMethod,
                        profile,
                        "Мимикрия: подключаюсь к звонку"
                    )
                    phantomSession = container.phantomCallCoordinator.prepare(settings)
                    val session = phantomSession
                        ?: error("Мимикрия звонков не смогла создать сессию")
                    LibertaRuntime.update(
                        ConnectionPhase.CONNECTING,
                        settings.connectionMethod,
                        profile,
                        "Звонок подключен: ${session.provider}",
                        error = null
                    )
                }

                val phantomCandidate = phantomSession?.tunnelCandidate
                if (phantomMode && phantomCandidate == null) {
                    error(
                        "Мимикрия не подключается к обычному серверу напрямую. Звонок создан, но для передачи VPN через звонок нужен Bridge URL/media-runtime."
                    )
                }

                val cached = if (phantomMode) null else container.subscriptionRepository.cached(profile)
                val refreshIntervalMs = settings.autoRefreshIntervalMinutes.coerceIn(5, 10_080) * 60L * 1_000L
                val cacheIsStale = cached == null || System.currentTimeMillis() - cached.lastUpdatedEpochMs >= refreshIntervalMs
                val useFastCache = cached != null && !forceRefresh

                LibertaRuntime.update(
                    ConnectionPhase.REFRESHING,
                    settings.connectionMethod,
                    profile,
                    if (useFastCache) "Быстрый старт из кеша" else "Обновление подписки"
                )
                val primarySnapshot = if (phantomCandidate != null) {
                    SubscriptionSnapshot(
                        profile = profile,
                        candidates = listOf(phantomCandidate.copy(profile = profile)),
                        lastUpdatedEpochMs = System.currentTimeMillis(),
                        fromCache = false,
                        sourceUrl = phantomSession?.roomUrl.orEmpty()
                    )
                } else if (useFastCache) {
                    container.serverMaintenanceCoordinator.refreshSoon("connect_fast_cache")
                    cached
                } else {
                    val refresh = forceRefresh || (settings.autoRefresh && cacheIsStale)
                    container.subscriptionRepository.load(profile, forceRefresh = refresh)
                }
                val snapshot = primarySnapshot
                if (snapshot.candidates.isEmpty()) error("Подписка не содержит VLESS серверов")
                Log.i(
                    "LibertaVpnService",
                    "phase=subscription_ready profile=$profile from_cache=${snapshot.fromCache} candidates=${snapshot.candidates.size} elapsed_ms=${SystemClock.elapsedRealtime() - connectStartedMs}"
                )

                LibertaRuntime.update(
                    ConnectionPhase.RACING,
                    settings.connectionMethod,
                    profile,
                    "Проверка ${snapshot.candidates.size} серверов",
                    lastUpdatedEpochMs = snapshot.lastUpdatedEpochMs
                )
                val supportedCandidates = snapshot.candidates.filter { it.isSupportedByCurrentCore() }
                val skippedUnsupported = snapshot.candidates.size - supportedCandidates.size
                if (skippedUnsupported > 0) {
                    Log.i(
                        "LibertaVpnService",
                        "phase=unsupported_filtered profile=$profile skipped=$skippedUnsupported elapsed_ms=${SystemClock.elapsedRealtime() - connectStartedMs}"
                    )
                }
                if (supportedCandidates.isEmpty()) error("Подписка не содержит серверов, совместимых с текущим ядром")
                val profileCandidates = supportedCandidates.prioritizeProfileHints(profile)
                val prioritizedCandidates = container.workingServersRepository
                    .prioritize(profile, profileCandidates)
                    .filter { it.isSupportedByCurrentCore() }
                val priorityRank = prioritizedCandidates.mapIndexed { index, candidate -> candidate.id to index }.toMap()
                val attemptLimit = connectAttemptLimit(profile, settings.connectionMethod)
                var connected = false
                var lastError: String? = null
                val warmAttempts = if (!forceRefresh && snapshot.fromCache) {
                    container.workingServersRepository
                        .remembered(profile, supportedCandidates)
                        .filter { it.isSupportedByCurrentCore() }
                        .take(attemptLimit)
                } else {
                    emptyList()
                }
                if (warmAttempts.isNotEmpty()) {
                    Log.i(
                        "LibertaVpnService",
                        "phase=warm_attempts_ready attempts=${warmAttempts.size} elapsed_ms=${SystemClock.elapsedRealtime() - connectStartedMs}"
                    )
                }

                suspend fun racedAttempts(excludedIds: Set<String>): List<ServerCandidate> {
                    val racingInput = prioritizedCandidates.filterNot { excludedIds.contains(it.id) }
                    if (racingInput.isEmpty()) return emptyList()
                    val racing = container.serverRacer.race(racingInput)
                    lastError = racing.error
                    val attempts = racing.tested
                        .filter { it.latencyMs != null }
                        .sortedWith(
                            compareBy<ServerCandidate> { if (it.hasMobileWhitelistHint(profile)) 0 else 1 }
                                .thenBy { if (it.port == 443) 0 else 1 }
                                .thenBy { it.latencyMs }
                                .thenBy { if (it.host.isIpLiteral()) 0 else 1 }
                                .thenBy { priorityRank[it.id] ?: Int.MAX_VALUE }
                        )
                        .take(attemptLimit)
                        .ifEmpty {
                            racing.tested
                                .ifEmpty { racingInput }
                                .sortedWith(
                                    compareBy<ServerCandidate> { if (it.hasMobileWhitelistHint(profile)) 0 else 1 }
                                        .thenBy { if (it.port == 443) 0 else 1 }
                                        .thenBy { if (it.host.isIpLiteral()) 0 else 1 }
                                        .thenBy { priorityRank[it.id] ?: Int.MAX_VALUE }
                                )
                                .distinctBy { it.endpoint }
                                .take(attemptLimit)
                        }
                    Log.i(
                        "LibertaVpnService",
                        "phase=racing_ready tested=${racing.tested.size} tcp_ok=${racing.tested.count { it.latencyMs != null }} attempts=${attempts.size} fallback=${racing.tested.none { it.latencyMs != null }} elapsed_ms=${SystemClock.elapsedRealtime() - connectStartedMs}"
                    )
                    return attempts
                }

                suspend fun tryAttempts(attempts: List<ServerCandidate>): Boolean {
                    for ((index, selected) in attempts.withIndex()) {
                        LibertaRuntime.update(
                            ConnectionPhase.CONNECTING,
                            settings.connectionMethod,
                            profile,
                            "Подключение ${index + 1}/${attempts.size}",
                            activeServer = selected,
                            lastUpdatedEpochMs = snapshot.lastUpdatedEpochMs
                        )
                        val selectedForCore = selected.withResolvedHostForCore()
                        val config = container.configBuilder.build(selectedForCore, settings)
                        val validation = container.coreEngine.validateConfig(config)
                        if (!validation.ok) {
                            lastError = validation.message
                            continue
                        }

                        val tunFd = establishTun(settings, selectedForCore)
                        val start = container.coreEngine.start(
                            configJson = config,
                            tunFd = tunFd,
                            cacheDir = cacheDir,
                            protect = { fd -> protect(fd) }
                        )
                        if (!start.ok) {
                            closeDetachedFd(tunFd)
                            lastError = start.message
                            container.workingServersRepository.forget(selectedForCore)
                            continue
                        }
                        detachedTunFd = tunFd

                        // Даем системе время применить маршруты
                        delay(250)

                        val vpnInternetOk = probeInternet()
                        Log.i(
                            "LibertaVpnService",
                            "phase=tunnel_validation selected=${selected.endpoint} vpn_online=$vpnInternetOk elapsed_ms=${SystemClock.elapsedRealtime() - connectStartedMs}"
                        )
                        if (!vpnInternetOk) {
                            lastError = "Сервер ${selected.endpoint} не прошел интернет-проверку через VPN"
                            LibertaRuntime.update(
                                ConnectionPhase.CONNECTING,
                                settings.connectionMethod,
                                profile,
                                "Сервер ${index + 1}/${attempts.size} не прошел интернет-проверку",
                                activeServer = selectedForCore,
                                lastUpdatedEpochMs = snapshot.lastUpdatedEpochMs,
                                error = lastError
                            )
                            container.workingServersRepository.forget(selectedForCore)
                            stopCoreOnly()
                            delay(150)
                            continue
                        }
                        LibertaRuntime.update(
                            ConnectionPhase.CONNECTED,
                            settings.connectionMethod,
                            profile,
                            "VPN активен; системный трафик направлен в TUN",
                            activeServer = selectedForCore,
                            lastUpdatedEpochMs = snapshot.lastUpdatedEpochMs,
                            error = null
                        )
                        container.workingServersRepository.rememberWorking(selectedForCore)
                        Log.i(
                            "LibertaVpnService",
                            "phase=connect_ready selected=${selectedForCore.endpoint} elapsed_ms=${SystemClock.elapsedRealtime() - connectStartedMs}"
                        )
                        return true
                    }
                    return false
                }

                val initialAttempts = if (phantomCandidate != null) {
                    listOf(phantomCandidate.copy(profile = profile))
                } else {
                    warmAttempts.ifEmpty { racedAttempts(emptySet()) }
                }
                if (initialAttempts.isEmpty()) error(lastError ?: "Не удалось выбрать сервер")
                connected = tryAttempts(initialAttempts)
                if (!connected && warmAttempts.isNotEmpty()) {
                    connected = tryAttempts(racedAttempts(warmAttempts.mapTo(mutableSetOf()) { it.id }))
                }
                if (!connected) error(recoveryMessage(settings.connectionMethod, lastError))
                startTrafficMonitor()
                updateNotification("Liberta активна")
            }.getOrElse { error ->
                Log.e("LibertaVpnService", "Connection failed", error)
                stopCoreOnly()
                LibertaRuntime.update(
                    ConnectionPhase.ERROR,
                    method,
                    profile,
                    error.message ?: "Ошибка подключения",
                    error = error.message
                )
                updateNotification("Ошибка Liberta")
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
    }

    private fun disconnect() {
        connectJob?.cancel()
        monitorJob?.cancel()
        scope.launch {
            stopCoreOnly()
            LibertaRuntime.update(ConnectionPhase.DISCONNECTED, message = "Готов к подключению")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun stopCoreOnly() {
        monitorJob?.cancel()
        container.coreEngine.stop()
        detachedTunFd?.let { closeDetachedFd(it) }
        detachedTunFd = null
        container.phantomCallCoordinator.cleanup(phantomSession)
        phantomSession = null
    }

    private fun establishTun(settings: LibertaSettings, selected: ServerCandidate): Int {
        val mtu = if (settings.autoMtu) 1280 else settings.mtu.coerceIn(1280, 9000)
        Log.i("LibertaVpnService", "Establishing TUN: mtu=$mtu, ipv6=${settings.ipv6Enabled}")
        val builder = Builder()
            .setSession("Liberta ${selected.profile.shortTitle}")
            .setMtu(mtu)
            .addAddress("172.19.0.1", 28)
            .addDnsServer("172.19.0.2")
            .addRoute("0.0.0.0", 0)

        if (settings.ipv6Enabled) {
            Log.i("LibertaVpnService", "Adding IPv6 routes and addresses")
            builder
                .addAddress("fdfe:dcba:9876::1", 126)
                .addRoute("::", 0)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        // Блокируем трафик до полной готовности туннеля
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && settings.killSwitch) {
            // builder.setBlocking(true) // Это может вызвать проблемы на некоторых устройствах, лучше использовать маршруты
        }

        val descriptor = builder.establish() ?: error("Android не выдал TUN интерфейс")
        return descriptor.detachFd()
    }

    private suspend fun probeInternet(): Boolean = withContext(Dispatchers.IO) {
        coroutineScope {
            HEALTH_PROBES
                .map { probe -> async { probeHealthEndpoint(probe) } }
                .awaitAll()
                .any { it }
        }
    }

    private fun probeHealthEndpoint(probe: HealthProbe): Boolean =
        runCatching {
            val sslSocket = SSLSocketFactory.getDefault().createSocket() as SSLSocket
            sslSocket.use { socket ->
                socket.connect(InetSocketAddress(probe.ip, probe.port), HEALTH_TIMEOUT_MS)
                socket.soTimeout = HEALTH_TIMEOUT_MS
                socket.sslParameters = socket.sslParameters.apply {
                    serverNames = listOf(SNIHostName(probe.sni))
                }
                socket.startHandshake()
            }
            Log.i("LibertaVpnService", "phase=tunnel_validation endpoint=${probe.ip}:${probe.port} online=true sni=${probe.sni}")
            true
        }.getOrElse { error ->
            Log.w("LibertaVpnService", "phase=tunnel_validation endpoint=${probe.ip}:${probe.port} online=false error=${error.message}")
            false
        }

    private fun startTrafficMonitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            var previousRx = android.net.TrafficStats.getTotalRxBytes()
            var previousTx = android.net.TrafficStats.getTotalTxBytes()
            while (isActive) {
                delay(1_200)
                val rx = android.net.TrafficStats.getTotalRxBytes()
                val tx = android.net.TrafficStats.getTotalTxBytes()
                val delta = ((rx - previousRx).coerceAtLeast(0) + (tx - previousTx).coerceAtLeast(0)).toFloat()
                previousRx = rx
                previousTx = tx
                LibertaRuntime.pulse(min(1f, delta / 512_000f))
            }
        }
    }

    private fun closeDetachedFd(fd: Int) {
        runCatching {
            val fileDescriptor = FileDescriptor()
            val descriptorField = FileDescriptor::class.java.getDeclaredField("descriptor")
            descriptorField.isAccessible = true
            descriptorField.setInt(fileDescriptor, fd)
            Os.close(fileDescriptor)
        }.recoverCatching {
            ParcelFileDescriptor.adoptFd(fd).close()
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.vpn_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("Liberta")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(compactNotification)
            .setPriority(if (compactNotification) NotificationCompat.PRIORITY_MIN else NotificationCompat.PRIORITY_LOW)
            .setColor(getColor(R.color.liberta_notification))
            .build()
    }

    private fun connectAttemptLimit(profile: ConnectionProfile, method: ConnectionMethod): Int =
        when {
            method == ConnectionMethod.PHANTOM_CALL -> 1
            profile == ConnectionProfile.WHITELISTS -> WHITELIST_CONNECT_ATTEMPT_LIMIT
            else -> DEFAULT_CONNECT_ATTEMPT_LIMIT
        }

    companion object {
        const val ACTION_CONNECT = "com.liberta.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.liberta.vpn.DISCONNECT"
        const val ACTION_RECOVER = "com.liberta.vpn.RECOVER"
        const val EXTRA_METHOD = "method"
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_FORCE_REFRESH = "force_refresh"
        const val EXTRA_PHANTOM_CALL = "phantom_call"
        const val EXTRA_MESH_ACCESS = "mesh_access"
        const val EXTRA_SOVEREIGN_RELAY = "sovereign_relay"

        private const val CHANNEL_ID = "liberta_vpn"
        private const val NOTIFICATION_ID = 1001
        private const val DEFAULT_CONNECT_ATTEMPT_LIMIT = 8
        private const val WHITELIST_CONNECT_ATTEMPT_LIMIT = 16
        private const val HEALTH_TIMEOUT_MS = 2_500
        private val HEALTH_PROBES = listOf(
            HealthProbe("1.1.1.1", 443, "one.one.one.one"),
            HealthProbe("1.0.0.1", 443, "one.one.one.one")
        )
    }
}

private data class HealthProbe(
    val ip: String,
    val port: Int,
    val sni: String
)

private fun recoveryMessage(method: ConnectionMethod, lastError: String?): String {
    val base = lastError ?: "Не удалось запустить рабочий туннель"
    val next = when (method) {
        ConnectionMethod.BLACKLISTS,
        ConnectionMethod.WHITELISTS ->
            "Сначала обновите серверы, затем попробуйте мимикрию звонков, затем меш-сеть."
        ConnectionMethod.PHANTOM_CALL ->
            "Затем попробуйте меш-сеть или укажите рабочий Bridge URL для звонков."
        ConnectionMethod.MESH_ACCESS ->
            "Обновите серверы и попробуйте черные или белые списки."
    }
    return "$base. $next"
}

private fun Intent.optionalBooleanExtra(name: String): Boolean? =
    if (hasExtra(name)) getBooleanExtra(name, false) else null

private suspend fun ServerCandidate.withResolvedHostForCore(): ServerCandidate =
    if (host.isIpLiteral()) {
        this
    } else {
        withContext(Dispatchers.IO) {
            val resolved = runCatching {
                InetAddress.getAllByName(host)
                    .firstOrNull { address -> address.hostAddress?.contains(':') == false }
                    ?.hostAddress
            }.getOrNull()
            resolved?.let { copy(host = it, sni = sni ?: host) } ?: this@withResolvedHostForCore
        }
    }

private fun String.isIpLiteral(): Boolean =
    all { it.isDigit() || it == '.' } || contains(':')

private fun List<ServerCandidate>.prioritizeProfileHints(profile: ConnectionProfile): List<ServerCandidate> =
    if (profile != ConnectionProfile.WHITELISTS) {
        this
    } else {
        sortedBy { if (it.hasMobileWhitelistHint(profile)) 0 else 1 }
    }

private fun ServerCandidate.hasMobileWhitelistHint(profile: ConnectionProfile): Boolean {
    if (profile != ConnectionProfile.WHITELISTS) return false
    val text = listOf(host, sni.orEmpty(), name).joinToString(" ")
    return WHITE_MOBILE_HINTS.any { hint -> text.contains(hint, ignoreCase = true) }
}

private fun ServerCandidate.isSupportedByCurrentCore(): Boolean =
    transport.lowercase() in CORE_SUPPORTED_TRANSPORTS

private val CORE_SUPPORTED_TRANSPORTS = setOf("tcp", "raw", "ws", "grpc")

private val WHITE_MOBILE_HINTS = listOf(
    "yandex",
    "ya.ru",
    "vk",
    "vkontakte",
    "userapi",
    "vkvideo",
    "telemost"
)

