package com.liberta.vpn.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libertaSettingsStore by preferencesDataStore(name = "liberta_settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<LibertaSettings> = context.libertaSettingsStore.data.map { prefs ->
        prefs.toSettings()
    }

    suspend fun updateSettings(transform: (LibertaSettings) -> LibertaSettings) {
        context.libertaSettingsStore.edit { prefs ->
            prefs.write(transform(prefs.toSettings()))
        }
    }

    suspend fun setProfile(profile: ConnectionProfile) {
        updateSettings { it.copy(profile = profile, connectionMethod = ConnectionMethod.fromProfile(profile)) }
    }

    suspend fun setAutoRefresh(enabled: Boolean) {
        updateSettings { it.copy(autoRefresh = enabled) }
    }

    suspend fun setAutoRefreshOnLaunch(enabled: Boolean) {
        updateSettings { it.copy(autoRefreshOnLaunch = enabled) }
    }

    suspend fun setAutoRefreshIntervalMinutes(minutes: Int) {
        updateSettings { it.copy(autoRefreshIntervalMinutes = minutes.coerceIn(5, 10_080)) }
    }

    suspend fun setAutoStart(enabled: Boolean) {
        updateSettings { it.copy(autoStart = enabled) }
    }

    suspend fun setDnsProvider(provider: DnsProvider) {
        updateSettings { it.copy(dnsProvider = provider) }
    }

    suspend fun setMtu(mtu: Int) {
        updateSettings { it.copy(mtu = mtu.coerceIn(1280, 9000)) }
    }

    suspend fun updateLabs(transform: (LabSettings) -> LabSettings) {
        updateSettings { it.copy(labs = transform(it.labs)) }
    }

    private fun Preferences.toSettings(): LibertaSettings {
        val hasConnectionMethod = contains(Keys.connectionMethod)
        val rawSovereignRelay = this[Keys.sovereignRelay] ?: false
        val labs = LabSettings(
            phantomCall = this[Keys.phantomCall] ?: false,
            phantomTransportService = PhantomTransportService.fromName(this[Keys.phantomTransportService]),
            phantomMimicryType = PhantomMimicryType.fromName(this[Keys.phantomMimicryType]),
            phantomAutoGenerateRooms = this[Keys.phantomAutoGenerateRooms] ?: true,
            phantomCustomRoomUrl = this[Keys.phantomCustomRoomUrl] ?: "",
            phantomBridgeUrl = this[Keys.phantomBridgeUrl] ?: "",
            phantomCamouflageNoise = PhantomNoiseProfile.fromName(this[Keys.phantomCamouflageNoise]),
            phantomSessionMinutes = (this[Keys.phantomSessionMinutes] ?: 20).coerceIn(5, 120),
            socialSteganography = this[Keys.socialSteganography] ?: false,
            stegoTelegram = this[Keys.stegoTelegram] ?: true,
            stegoReddit = this[Keys.stegoReddit] ?: true,
            stegoTwitter = this[Keys.stegoTwitter] ?: false,
            stegoPhotoHosts = this[Keys.stegoPhotoHosts] ?: true,
            stegoScanDepth = (this[Keys.stegoScanDepth] ?: 25).coerceIn(5, 250),
            polymorphicCore = this[Keys.polymorphicCore] ?: false,
            mutationLevel = (this[Keys.mutationLevel] ?: 1).coerceIn(0, 3),
            tlsFingerprintProfile = TlsFingerprintProfile.fromName(this[Keys.tlsFingerprintProfile]),
            dynamicPadding = this[Keys.dynamicPadding] ?: true,
            jitterShaping = this[Keys.jitterShaping] ?: true,
            sovereignRelay = rawSovereignRelay && hasConnectionMethod,
            relayRole = RelayRole.fromName(this[Keys.relayRole]),
            relayOnlyCharging = this[Keys.relayOnlyCharging] ?: true,
            relayStopBelowPercent = (this[Keys.relayStopBelowPercent] ?: 30).coerceIn(5, 95),
            relayWifiOnly = this[Keys.relayWifiOnly] ?: true,
            relayBandwidthGb = (this[Keys.relayBandwidthGb] ?: 10).coerceIn(1, 500),
            homeostasis = this[Keys.homeostasis] ?: true,
            adaptiveFrequency = this[Keys.adaptiveFrequency] ?: true,
            smartReconnect = this[Keys.smartReconnect] ?: true,
            thermalGuard = this[Keys.thermalGuard] ?: true
        )

        val profile = ConnectionProfile.fromName(this[Keys.profile])
        val method = this[Keys.connectionMethod]?.let(ConnectionMethod::fromName)
            ?: if (rawSovereignRelay) {
                ConnectionMethod.MESH_ACCESS
            } else if (this[Keys.phantomCall] == true) {
                ConnectionMethod.PHANTOM_CALL
            } else {
                ConnectionMethod.fromProfile(profile)
            }

        return LibertaSettings(
            connectionMethod = method,
            profile = profile,
            autoRefresh = this[Keys.autoRefresh] ?: true,
            autoRefreshOnLaunch = this[Keys.autoRefreshOnLaunch] ?: true,
            autoRefreshIntervalMinutes = (
                this[Keys.autoRefreshIntervalMinutes]
                    ?: this[Keys.autoRefreshIntervalHours]?.times(60)
                    ?: 30
                ).coerceIn(5, 10_080),
            autoStart = this[Keys.autoStart] ?: false,
            dnsProvider = DnsProvider.fromName(this[Keys.dnsProvider]),
            customDns = this[Keys.customDns] ?: "",
            autoMtu = this[Keys.autoMtu] ?: true,
            mtu = (this[Keys.mtu] ?: 1500).coerceIn(1280, 9000),
            ipv6Enabled = this[Keys.ipv6Enabled] ?: false,
            killSwitch = this[Keys.killSwitch] ?: true,
            proxyEnabled = this[Keys.proxyEnabled] ?: false,
            proxyPort = (this[Keys.proxyPort] ?: 10808).coerceIn(1024, 65535),
            splitTunneling = this[Keys.splitTunneling] ?: false,
            dynamicTheme = this[Keys.dynamicTheme] ?: true,
            compactNotification = this[Keys.compactNotification] ?: true,
            labs = labs
        )
    }

    private fun MutablePreferences.write(settings: LibertaSettings) {
        this[Keys.connectionMethod] = settings.connectionMethod.name
        this[Keys.profile] = settings.profile.name
        this[Keys.autoRefresh] = settings.autoRefresh
        this[Keys.autoRefreshOnLaunch] = settings.autoRefreshOnLaunch
        this[Keys.autoRefreshIntervalMinutes] = settings.autoRefreshIntervalMinutes.coerceIn(5, 10_080)
        this[Keys.autoStart] = settings.autoStart
        this[Keys.dnsProvider] = settings.dnsProvider.name
        this[Keys.customDns] = settings.customDns
        this[Keys.autoMtu] = settings.autoMtu
        this[Keys.mtu] = settings.mtu.coerceIn(1280, 9000)
        this[Keys.ipv6Enabled] = settings.ipv6Enabled
        this[Keys.killSwitch] = settings.killSwitch
        this[Keys.proxyEnabled] = settings.proxyEnabled
        this[Keys.proxyPort] = settings.proxyPort.coerceIn(1024, 65535)
        this[Keys.splitTunneling] = settings.splitTunneling
        this[Keys.dynamicTheme] = settings.dynamicTheme
        this[Keys.compactNotification] = settings.compactNotification

        val labs = settings.labs
        this[Keys.phantomCall] = labs.phantomCall
        this[Keys.phantomTransportService] = labs.phantomTransportService.name
        this[Keys.phantomMimicryType] = labs.phantomMimicryType.name
        this[Keys.phantomAutoGenerateRooms] = labs.phantomAutoGenerateRooms
        this[Keys.phantomCustomRoomUrl] = labs.phantomCustomRoomUrl
        this[Keys.phantomBridgeUrl] = labs.phantomBridgeUrl
        this[Keys.phantomCamouflageNoise] = labs.phantomCamouflageNoise.name
        this[Keys.phantomSessionMinutes] = labs.phantomSessionMinutes.coerceIn(5, 120)
        this[Keys.socialSteganography] = labs.socialSteganography
        this[Keys.stegoTelegram] = labs.stegoTelegram
        this[Keys.stegoReddit] = labs.stegoReddit
        this[Keys.stegoTwitter] = labs.stegoTwitter
        this[Keys.stegoPhotoHosts] = labs.stegoPhotoHosts
        this[Keys.stegoScanDepth] = labs.stegoScanDepth.coerceIn(5, 250)
        this[Keys.polymorphicCore] = labs.polymorphicCore
        this[Keys.mutationLevel] = labs.mutationLevel.coerceIn(0, 3)
        this[Keys.tlsFingerprintProfile] = labs.tlsFingerprintProfile.name
        this[Keys.dynamicPadding] = labs.dynamicPadding
        this[Keys.jitterShaping] = labs.jitterShaping
        this[Keys.sovereignRelay] = labs.sovereignRelay
        this[Keys.relayRole] = labs.relayRole.name
        this[Keys.relayOnlyCharging] = labs.relayOnlyCharging
        this[Keys.relayStopBelowPercent] = labs.relayStopBelowPercent.coerceIn(5, 95)
        this[Keys.relayWifiOnly] = labs.relayWifiOnly
        this[Keys.relayBandwidthGb] = labs.relayBandwidthGb.coerceIn(1, 500)
        this[Keys.homeostasis] = labs.homeostasis
        this[Keys.adaptiveFrequency] = labs.adaptiveFrequency
        this[Keys.smartReconnect] = labs.smartReconnect
        this[Keys.thermalGuard] = labs.thermalGuard
    }

    private object Keys {
        val connectionMethod = stringPreferencesKey("connection_method")
        val profile = stringPreferencesKey("profile")
        val autoRefresh = booleanPreferencesKey("auto_refresh")
        val autoRefreshOnLaunch = booleanPreferencesKey("auto_refresh_on_launch")
        val autoRefreshIntervalMinutes = intPreferencesKey("auto_refresh_interval_minutes")
        val autoRefreshIntervalHours = intPreferencesKey("auto_refresh_interval_hours")
        val autoStart = booleanPreferencesKey("auto_start")
        val dnsProvider = stringPreferencesKey("dns_provider")
        val customDns = stringPreferencesKey("custom_dns")
        val autoMtu = booleanPreferencesKey("auto_mtu")
        val mtu = intPreferencesKey("mtu")
        val ipv6Enabled = booleanPreferencesKey("ipv6_enabled")
        val killSwitch = booleanPreferencesKey("kill_switch")
        val proxyEnabled = booleanPreferencesKey("proxy_enabled")
        val proxyPort = intPreferencesKey("proxy_port")
        val splitTunneling = booleanPreferencesKey("split_tunneling")
        val dynamicTheme = booleanPreferencesKey("dynamic_theme")
        val compactNotification = booleanPreferencesKey("compact_notification")

        val phantomCall = booleanPreferencesKey("lab_phantom_call")
        val phantomTransportService = stringPreferencesKey("lab_phantom_transport_service")
        val phantomMimicryType = stringPreferencesKey("lab_phantom_mimicry_type")
        val phantomAutoGenerateRooms = booleanPreferencesKey("lab_phantom_auto_generate_rooms")
        val phantomCustomRoomUrl = stringPreferencesKey("lab_phantom_custom_room_url")
        val phantomBridgeUrl = stringPreferencesKey("lab_phantom_bridge_url")
        val phantomCamouflageNoise = stringPreferencesKey("lab_phantom_camouflage_noise")
        val phantomSessionMinutes = intPreferencesKey("lab_phantom_session_minutes")
        val socialSteganography = booleanPreferencesKey("lab_social_steganography")
        val stegoTelegram = booleanPreferencesKey("lab_stego_telegram")
        val stegoReddit = booleanPreferencesKey("lab_stego_reddit")
        val stegoTwitter = booleanPreferencesKey("lab_stego_twitter")
        val stegoPhotoHosts = booleanPreferencesKey("lab_stego_photo_hosts")
        val stegoScanDepth = intPreferencesKey("lab_stego_scan_depth")
        val polymorphicCore = booleanPreferencesKey("lab_polymorphic_core")
        val mutationLevel = intPreferencesKey("lab_mutation_level")
        val tlsFingerprintProfile = stringPreferencesKey("lab_tls_fingerprint_profile")
        val dynamicPadding = booleanPreferencesKey("lab_dynamic_padding")
        val jitterShaping = booleanPreferencesKey("lab_jitter_shaping")
        val sovereignRelay = booleanPreferencesKey("lab_sovereign_relay")
        val relayRole = stringPreferencesKey("lab_relay_role")
        val relayOnlyCharging = booleanPreferencesKey("lab_relay_only_charging")
        val relayStopBelowPercent = intPreferencesKey("lab_relay_stop_below")
        val relayWifiOnly = booleanPreferencesKey("lab_relay_wifi_only")
        val relayBandwidthGb = intPreferencesKey("lab_relay_bandwidth_gb")
        val homeostasis = booleanPreferencesKey("lab_homeostasis")
        val adaptiveFrequency = booleanPreferencesKey("lab_adaptive_frequency")
        val smartReconnect = booleanPreferencesKey("lab_smart_reconnect")
        val thermalGuard = booleanPreferencesKey("lab_thermal_guard")
    }
}
