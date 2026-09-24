package com.liberta.vpn.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.random.Random

data class ServerMaintenanceResult(
    val refreshedProfiles: Int,
    val totalCandidates: Int,
    val validatedServers: Int,
    val lastUpdatedEpochMs: Long
)

class ServerMaintenanceCoordinator(
    private val settingsRepository: SettingsRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val workingServersRepository: WorkingServersRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val broadRacer = ServerRacer(timeoutMs = 2_800, maxCandidates = 192)
    private val refreshMutex = Mutex()
    private var loopJob: Job? = null

    fun start() {
        if (loopJob != null) return
        loopJob = scope.launch {
            refreshSoon("startup")
            while (isActive) {
                val settings = settingsRepository.settings.first()
                delay(settings.refreshDelayWithJitterMs())
                if (settings.autoRefresh) {
                    refreshAndValidate(forceRefresh = false, reason = "periodic")
                }
            }
        }
    }

    fun refreshSoon(reason: String, forceRefresh: Boolean = false) {
        scope.launch {
            delay(1_500)
            refreshAndValidate(forceRefresh = forceRefresh, reason = reason)
        }
    }

    suspend fun refreshAndValidate(forceRefresh: Boolean, reason: String): ServerMaintenanceResult =
        refreshMutex.withLock {
            val settings = settingsRepository.settings.first()
            val now = System.currentTimeMillis()
            val refreshIntervalMs = settings.autoRefreshIntervalMinutes.coerceIn(5, 10_080) * 60L * 1_000L
            var refreshedProfiles = 0
            var totalCandidates = 0
            var validatedServers = 0
            var lastUpdated = 0L

            ConnectionProfile.entries.forEach { profile ->
                val cached = subscriptionRepository.cached(profile)
                val stale = cached == null || now - cached.lastUpdatedEpochMs >= refreshIntervalMs
                val snapshot = subscriptionRepository.load(profile, forceRefresh = forceRefresh || stale)
                refreshedProfiles += if (!snapshot.fromCache) 1 else 0
                totalCandidates += snapshot.candidates.size
                lastUpdated = max(lastUpdated, snapshot.lastUpdatedEpochMs)

                if (snapshot.candidates.isNotEmpty()) {
                    val racing = broadRacer.race(snapshot.candidates)
                    val tcpWorking = racing.tested
                        .filter { it.latencyMs != null }
                        .sortedWith(
                            compareBy<ServerCandidate> { if (it.port == 443) 0 else 1 }
                                .thenBy { it.latencyMs }
                        )
                    workingServersRepository.rememberBackground(profile, tcpWorking)
                    validatedServers += tcpWorking.size
                    Log.i(
                        "LibertaMaintenance",
                        "profile=$profile reason=$reason candidates=${snapshot.candidates.size} tcp_ok=${tcpWorking.size} refreshed=${!snapshot.fromCache}"
                    )
                }
            }

            ServerMaintenanceResult(
                refreshedProfiles = refreshedProfiles,
                totalCandidates = totalCandidates,
                validatedServers = validatedServers,
                lastUpdatedEpochMs = lastUpdated
            )
        }

    private fun LibertaSettings.refreshDelayWithJitterMs(): Long {
        val baseMinutes = autoRefreshIntervalMinutes.coerceIn(5, 10_080)
        val jitterMinutes = Random.nextInt(-10, 11)
        return (baseMinutes + jitterMinutes)
            .coerceAtLeast(5)
            .toLong() * 60L * 1_000L
    }
}
