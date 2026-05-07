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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import java.net.HttpURLConnection
import java.net.URL
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
                            phantomCall = phantomCall ?: saved.labs.phantomCall,
                            sovereignRelay = sovereignRelay ?: saved.labs.sovereignRelay
                        )
                    )
                }
                compactNotification = settings.compactNotification
                updateNotification("Liberta запускается")
                if (settings.labs.phantomCall) {
                    LibertaRuntime.update(
                        ConnectionPhase.CONNECTING,
                        settings.connectionMethod,
                        profile,
                        "Мимикрия под звонки: создаю комнату"
                    )
                    phantomSession = container.phantomCallCoordinator.prepare(settings)
                    LibertaRuntime.update(
                        ConnectionPhase.CONNECTING,
                        settings.connectionMethod,
                        profile,
                        "Мимикрия под звонки: auto-bridge готов",
                        error = null
                    )
                }

                val cached = container.subscriptionRepository.cached(profile)
                val refreshIntervalMs = settings.autoRefreshIntervalMinutes.coerceIn(5, 10_080) * 60L * 1_000L
                val cacheIsStale = cached == null || System.currentTimeMillis() - cached.lastUpdatedEpochMs >= refreshIntervalMs
                val refresh = forceRefresh || (settings.autoRefresh && cacheIsStale)

                LibertaRuntime.update(ConnectionPhase.REFRESHING, settings.connectionMethod, profile, "Обновление подписки")
                val primarySnapshot = container.subscriptionRepository.load(profile, forceRefresh = refresh)
                val snapshot = if (primarySnapshot.candidates.isEmpty() && profile == ConnectionProfile.WHITELISTS) {
                    Log.w("LibertaVpnService", "profile=WHITELISTS has no VLESS endpoints, using free transport pool")
                    container.subscriptionRepository.load(ConnectionProfile.BLACKLISTS, forceRefresh = true)
                } else {
                    primarySnapshot
                }
                if (snapshot.candidates.isEmpty()) error("Подписка не содержит VLESS серверов")

                LibertaRuntime.update(
                    ConnectionPhase.RACING,
                    settings.connectionMethod,
                    profile,
                    "Проверка ${snapshot.candidates.size} серверов",
                    lastUpdatedEpochMs = snapshot.lastUpdatedEpochMs
                )
                val prioritizedCandidates = container.workingServersRepository.prioritize(profile, snapshot.candidates)
                val racing = container.serverRacer.race(prioritizedCandidates)
                val attempts = racing.tested
                    .filter { it.latencyMs != null }
                    .sortedBy { it.latencyMs }
                    .take(CONNECT_ATTEMPT_LIMIT)
                if (attempts.isEmpty()) error(racing.error ?: "Не удалось выбрать сервер")

                var connected = false
                var lastError = racing.error
                for ((index, selected) in attempts.withIndex()) {
                    LibertaRuntime.update(
                        ConnectionPhase.CONNECTING,
                        settings.connectionMethod,
                        profile,
                        "Подключение ${index + 1}/${attempts.size}",
                        activeServer = selected,
                        lastUpdatedEpochMs = snapshot.lastUpdatedEpochMs
                    )
                    val config = container.configBuilder.build(selected, settings)
                    val validation = container.coreEngine.validateConfig(config)
                    if (!validation.ok) {
                        lastError = validation.message
                        continue
                    }

                    val tunFd = establishTun(settings, selected)
                    val start = container.coreEngine.start(
                        configJson = config,
                        tunFd = tunFd,
                        cacheDir = cacheDir,
                        protect = { fd -> protect(fd) }
                    )
                    if (!start.ok) {
                        closeDetachedFd(tunFd)
                        lastError = start.message
                        continue
                    }
                    detachedTunFd = tunFd
                    
                    // Даем системе время применить маршруты
                    delay(500)

                    val processInternetOk = probeInternet()
                    LibertaRuntime.update(
                        ConnectionPhase.CONNECTED,
                        settings.connectionMethod,
                        profile,
                        "VPN активен; системный трафик направлен в TUN",
                        activeServer = selected,
                        lastUpdatedEpochMs = snapshot.lastUpdatedEpochMs,
                        error = if (processInternetOk) null else "Процесс приложения исключен из VPN; нужна внешняя проверка трафика"
                    )
                    Log.i(
                        "LibertaVpnService",
                        "phase=tunnel_validation selected=${selected.endpoint} process_online=$processInternetOk app_uid_excluded=true"
                    )
                    container.workingServersRepository.rememberWorking(selected)
                    connected = true
                    break
                }
                if (!connected) error(lastError ?: "Не удалось запустить рабочий туннель")
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

        // libbox runs inside this process; keeping the app outside the VPN avoids proxying
        // the core's own VLESS socket back into the TUN.
        runCatching { builder.addDisallowedApplication(packageName) }

        val descriptor = builder.establish() ?: error("Android не выдал TUN интерфейс")
        return descriptor.detachFd()
    }

    private suspend fun probeInternet(): Boolean = withContext(Dispatchers.IO) {
        HEALTH_PROBES.any { endpoint ->
            runCatching {
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Liberta/0.1 Android")
                }
                try {
                    val healthy = connection.responseCode in 200..399
                    Log.i("LibertaVpnService", "phase=tunnel_validation endpoint=$endpoint online=$healthy code=${connection.responseCode}")
                    healthy
                } finally {
                    connection.disconnect()
                }
            }.getOrElse { error ->
                Log.w("LibertaVpnService", "phase=tunnel_validation endpoint=$endpoint online=false error=${error.message}")
                false
            }
        }
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
        private const val CONNECT_ATTEMPT_LIMIT = 6
        private val HEALTH_PROBES = listOf(
            "http://connectivitycheck.gstatic.com/generate_204",
            "http://cp.cloudflare.com/generate_204",
            "https://www.google.com/generate_204",
            "https://connectivitycheck.gstatic.com/generate_204",
            "https://cloudflare.com/cdn-cgi/trace"
        )
    }
}

private fun Intent.optionalBooleanExtra(name: String): Boolean? =
    if (hasExtra(name)) getBooleanExtra(name, false) else null

