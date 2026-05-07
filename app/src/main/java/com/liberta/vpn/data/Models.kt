package com.liberta.vpn.data

import java.util.Locale

enum class ConnectionProfile(
    val title: String,
    val shortTitle: String,
    val subscriptionUrl: String
) {
    WHITELISTS(
        title = "Белые списки",
        shortTitle = "Белые",
        subscriptionUrl = "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/WHITE-CIDR-RU-all.txt"
    ),
    BLACKLISTS(
        title = "Черные списки",
        shortTitle = "Черные",
        subscriptionUrl = "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS.txt"
    );

    companion object {
        fun fromName(value: String?): ConnectionProfile =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BLACKLISTS
    }
}

enum class ConnectionMethod(
    val title: String,
    val profile: ConnectionProfile
) {
    BLACKLISTS("Черные списки", ConnectionProfile.BLACKLISTS),
    WHITELISTS("Белые списки", ConnectionProfile.WHITELISTS),
    PHANTOM_CALL("Мимикрия под звонки", ConnectionProfile.BLACKLISTS),
    MESH_ACCESS("Меш-сеть", ConnectionProfile.BLACKLISTS);

    companion object {
        fun fromName(value: String?): ConnectionMethod =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BLACKLISTS

        fun fromProfile(profile: ConnectionProfile): ConnectionMethod =
            if (profile == ConnectionProfile.WHITELISTS) WHITELISTS else BLACKLISTS
    }
}

enum class ConnectionPhase {
    DISCONNECTED,
    REFRESHING,
    RACING,
    CONNECTING,
    CONNECTED,
    DEGRADED,
    RECOVERING,
    ERROR
}

enum class DnsProvider(val label: String, val servers: List<String>) {
    SMART("Smart DNS", listOf("1.1.1.1", "8.8.8.8", "94.140.14.14")),
    CLOUDFLARE("Cloudflare", listOf("1.1.1.1", "1.0.0.1")),
    GOOGLE("Google", listOf("8.8.8.8", "8.8.4.4")),
    ADGUARD("AdGuard", listOf("94.140.14.14", "94.140.15.15")),
    CUSTOM("Свой DNS", emptyList());

    companion object {
        fun fromName(value: String?): DnsProvider =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SMART
    }
}

enum class PhantomTransportService(val label: String, val createEndpoint: String) {
    VK_CALLS("VK Звонки", "https://api.vk.com/method/calls.create"),
    YANDEX_TELEMOST("Яндекс Телемост", "https://telemost.yandex.ru/api/conferences");

    companion object {
        fun fromName(value: String?): PhantomTransportService =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: YANDEX_TELEMOST
    }
}

enum class PhantomMimicryType(val label: String) {
    AUDIO_OPUS("Только аудио Opus"),
    VIDEO_H264("Видео H.264"),
    VIDEO_VP9("Видео VP9");

    companion object {
        fun fromName(value: String?): PhantomMimicryType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: VIDEO_H264
    }
}

enum class PhantomNoiseProfile(val label: String) {
    NONE("Без шума"),
    WHITE_NOISE("Белый шум"),
    AMBIENT_VOICES("Фоновые голоса");

    companion object {
        fun fromName(value: String?): PhantomNoiseProfile =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: WHITE_NOISE
    }
}

enum class TlsFingerprintProfile(val label: String) {
    CHROME_WINDOWS("Chrome на Windows"),
    SAFARI_IOS("Safari на iOS"),
    FIREFOX_LINUX("Firefox на Linux");

    companion object {
        fun fromName(value: String?): TlsFingerprintProfile =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CHROME_WINDOWS
    }
}

enum class RelayRole(val label: String) {
    RELAY("Ретрансляция"),
    BRIDGE("Мост с выходом");

    companion object {
        fun fromName(value: String?): RelayRole =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: RELAY
    }
}

data class LabSettings(
    val phantomCall: Boolean = false,
    val phantomTransportService: PhantomTransportService = PhantomTransportService.YANDEX_TELEMOST,
    val phantomMimicryType: PhantomMimicryType = PhantomMimicryType.VIDEO_H264,
    val phantomAutoGenerateRooms: Boolean = true,
    val phantomCustomRoomUrl: String = "",
    val phantomBridgeUrl: String = "",
    val phantomCamouflageNoise: PhantomNoiseProfile = PhantomNoiseProfile.WHITE_NOISE,
    val phantomSessionMinutes: Int = 20,
    val socialSteganography: Boolean = false,
    val stegoTelegram: Boolean = true,
    val stegoReddit: Boolean = true,
    val stegoTwitter: Boolean = false,
    val stegoPhotoHosts: Boolean = true,
    val stegoScanDepth: Int = 25,
    val polymorphicCore: Boolean = false,
    val mutationLevel: Int = 1,
    val tlsFingerprintProfile: TlsFingerprintProfile = TlsFingerprintProfile.CHROME_WINDOWS,
    val dynamicPadding: Boolean = true,
    val jitterShaping: Boolean = true,
    val sovereignRelay: Boolean = false,
    val relayRole: RelayRole = RelayRole.RELAY,
    val relayOnlyCharging: Boolean = true,
    val relayStopBelowPercent: Int = 30,
    val relayWifiOnly: Boolean = true,
    val relayBandwidthGb: Int = 10,
    val homeostasis: Boolean = true,
    val adaptiveFrequency: Boolean = true,
    val smartReconnect: Boolean = true,
    val thermalGuard: Boolean = true
)

data class LibertaSettings(
    val connectionMethod: ConnectionMethod = ConnectionMethod.BLACKLISTS,
    val profile: ConnectionProfile = ConnectionProfile.BLACKLISTS,
    val autoRefresh: Boolean = true,
    val autoRefreshOnLaunch: Boolean = true,
    val autoRefreshIntervalMinutes: Int = 30,
    val autoStart: Boolean = false,
    val dnsProvider: DnsProvider = DnsProvider.SMART,
    val customDns: String = "",
    val autoMtu: Boolean = true,
    val mtu: Int = 1500,
    val ipv6Enabled: Boolean = true,
    val killSwitch: Boolean = true,
    val proxyEnabled: Boolean = false,
    val proxyPort: Int = 10808,
    val splitTunneling: Boolean = false,
    val dynamicTheme: Boolean = true,
    val compactNotification: Boolean = true,
    val labs: LabSettings = LabSettings()
)

data class ServerCandidate(
    val id: String,
    val profile: ConnectionProfile,
    val rawLink: String,
    val uuid: String,
    val host: String,
    val port: Int,
    val name: String,
    val transport: String = "tcp",
    val security: String = "reality",
    val flow: String? = null,
    val fingerprint: String? = null,
    val sni: String? = null,
    val publicKey: String? = null,
    val shortId: String? = null,
    val path: String? = null,
    val serviceName: String? = null,
    val latencyMs: Long? = null
) {
    val endpoint: String = "$host:$port"

    fun withLatency(value: Long): ServerCandidate = copy(latencyMs = value)

    fun stableDisplayName(): String =
        name.ifBlank { "${transport.uppercase(Locale.US)} $endpoint" }
}

data class SubscriptionSnapshot(
    val profile: ConnectionProfile,
    val candidates: List<ServerCandidate>,
    val lastUpdatedEpochMs: Long,
    val fromCache: Boolean,
    val sourceUrl: String
)

data class VpnStatus(
    val phase: ConnectionPhase = ConnectionPhase.DISCONNECTED,
    val connectionMethod: ConnectionMethod = ConnectionMethod.BLACKLISTS,
    val profile: ConnectionProfile = ConnectionProfile.BLACKLISTS,
    val message: String = "Готов к подключению",
    val activeServer: ServerCandidate? = null,
    val lastUpdatedEpochMs: Long? = null,
    val trafficPulse: Float = 0f,
    val helpedUsers: Int = 0,
    val error: String? = null
) {
    val isBusy: Boolean = phase in setOf(
        ConnectionPhase.REFRESHING,
        ConnectionPhase.RACING,
        ConnectionPhase.CONNECTING,
        ConnectionPhase.RECOVERING
    )

    val isConnected: Boolean = phase == ConnectionPhase.CONNECTED
}

data class RacingResult(
    val selected: ServerCandidate?,
    val tested: List<ServerCandidate>,
    val error: String? = null
)
