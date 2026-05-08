package com.liberta.vpn.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liberta.vpn.LibertaApplication
import com.liberta.vpn.data.ConnectionPhase
import com.liberta.vpn.data.ConnectionMethod
import com.liberta.vpn.data.ConnectionProfile
import com.liberta.vpn.data.DnsProvider
import com.liberta.vpn.data.LabSettings
import com.liberta.vpn.data.LibertaSettings
import com.liberta.vpn.data.VpnStatus
import com.liberta.vpn.service.LibertaRuntime
import com.liberta.vpn.service.LibertaVpnService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max

class LibertaViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as LibertaApplication).container

    val settings: StateFlow<LibertaSettings> = container.settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibertaSettings()
    )

    val status: StateFlow<VpnStatus> = LibertaRuntime.status

    fun connect(
        context: Context,
        method: ConnectionMethod,
        forceRefresh: Boolean = false,
        phantomCall: Boolean? = null,
        meshAccess: Boolean? = null,
        sovereignRelay: Boolean? = null
    ) {
        val profile = method.profile
        ContextCompat.startForegroundService(
            context,
            Intent(context, LibertaVpnService::class.java).apply {
                action = LibertaVpnService.ACTION_CONNECT
                putExtra(LibertaVpnService.EXTRA_METHOD, method.name)
                putExtra(LibertaVpnService.EXTRA_PROFILE, profile.name)
                putExtra(LibertaVpnService.EXTRA_FORCE_REFRESH, forceRefresh)
                phantomCall?.let { putExtra(LibertaVpnService.EXTRA_PHANTOM_CALL, it) }
                meshAccess?.let { putExtra(LibertaVpnService.EXTRA_MESH_ACCESS, it) }
                sovereignRelay?.let { putExtra(LibertaVpnService.EXTRA_SOVEREIGN_RELAY, it) }
            }
        )
    }

    fun disconnect(context: Context) {
        context.startService(Intent(context, LibertaVpnService::class.java).apply {
            action = LibertaVpnService.ACTION_DISCONNECT
        })
    }

    fun recover(context: Context) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, LibertaVpnService::class.java).apply {
                action = LibertaVpnService.ACTION_RECOVER
            }
        )
    }

    fun refreshAll(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            val activeProfile = settings.value.profile
            val activeMethod = settings.value.connectionMethod
            if (!forceRefresh) {
                val snapshots = ConnectionProfile.entries.mapNotNull { profile ->
                    container.subscriptionRepository.cached(profile)
                }
                val total = snapshots.sumOf { it.candidates.size }
                val lastUpdated = snapshots.maxOfOrNull { it.lastUpdatedEpochMs } ?: 0L
                LibertaRuntime.update(
                    ConnectionPhase.DISCONNECTED,
                    connectionMethod = activeMethod,
                    profile = activeProfile,
                    message = if (total > 0) "Кеш готов: $total серверов" else "Готов к подключению",
                    lastUpdatedEpochMs = lastUpdated.takeIf { it > 0L }
                )
                return@launch
            }
            LibertaRuntime.update(
                ConnectionPhase.REFRESHING,
                connectionMethod = activeMethod,
                profile = activeProfile,
                message = "Обновляю все подписки"
            )
            runCatching {
                val intervalMs = settings.value.autoRefreshIntervalMinutes.coerceIn(5, 10_080) * 60L * 1_000L
                val now = System.currentTimeMillis()
                var total = 0
                var lastUpdated = 0L
                ConnectionProfile.entries.forEach { profile ->
                    val cached = container.subscriptionRepository.cached(profile)
                    val stale = cached == null || now - cached.lastUpdatedEpochMs >= intervalMs
                    val snapshot = container.subscriptionRepository.load(profile, forceRefresh || stale)
                    total += snapshot.candidates.size
                    lastUpdated = max(lastUpdated, snapshot.lastUpdatedEpochMs)
                }
                total to lastUpdated
            }.onSuccess { (total, lastUpdated) ->
                LibertaRuntime.update(
                    ConnectionPhase.DISCONNECTED,
                    connectionMethod = activeMethod,
                    profile = activeProfile,
                    message = "Обновлено: $total серверов",
                    lastUpdatedEpochMs = lastUpdated.takeIf { it > 0L }
                )
            }.onFailure { error ->
                LibertaRuntime.update(
                    ConnectionPhase.ERROR,
                    connectionMethod = activeMethod,
                    profile = activeProfile,
                    message = error.message ?: "Не удалось обновить подписки",
                    error = error.message
                )
            }
        }
    }

    fun setProfile(profile: ConnectionProfile) {
        viewModelScope.launch { container.settingsRepository.setProfile(profile) }
    }

    fun setAutoRefresh(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAutoRefresh(enabled) }
    }

    fun setAutoRefreshOnLaunch(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAutoRefreshOnLaunch(enabled) }
    }

    fun setAutoRefreshIntervalMinutes(minutes: Int) {
        viewModelScope.launch { container.settingsRepository.setAutoRefreshIntervalMinutes(minutes) }
    }

    fun setAutoStart(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAutoStart(enabled) }
    }

    fun setDnsProvider(provider: DnsProvider) {
        viewModelScope.launch { container.settingsRepository.setDnsProvider(provider) }
    }

    fun setMtu(mtu: Int) {
        viewModelScope.launch { container.settingsRepository.setMtu(mtu) }
    }

    fun updateSettings(transform: (LibertaSettings) -> LibertaSettings) {
        viewModelScope.launch { container.settingsRepository.updateSettings(transform) }
    }

    fun updateLabs(transform: (LabSettings) -> LabSettings) {
        viewModelScope.launch { container.settingsRepository.updateLabs(transform) }
    }
}
