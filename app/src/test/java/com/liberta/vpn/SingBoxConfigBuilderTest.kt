package com.liberta.vpn

import com.liberta.vpn.core.SingBoxConfigBuilder
import com.liberta.vpn.data.ConnectionProfile
import com.liberta.vpn.data.DnsProvider
import com.liberta.vpn.data.LabSettings
import com.liberta.vpn.data.LibertaSettings
import com.liberta.vpn.data.ServerCandidate
import org.junit.Assert.assertTrue
import org.junit.Test

class SingBoxConfigBuilderTest {
    @Test
    fun buildsProxyFinalTunConfig() {
        val json = SingBoxConfigBuilder().build(testCandidate(), LibertaSettings())

        assertTrue(json.contains("\"type\": \"tun\""))
        assertTrue(json.contains("\"type\": \"vless\""))
        assertTrue(json.contains("\"server\": \"vpn.example\""))
        assertTrue(json.contains("\"final\": \"proxy\""))
        assertTrue(json.contains("\"final\": \"dns-direct\""))
        assertTrue(json.contains("\"type\": \"https\""))
        assertTrue(json.contains("\"type\": \"local\""))
        assertTrue(json.contains("\"server_name\": \"cloudflare-dns.com\""))
        assertTrue(json.contains("\"path\": \"/dns-query\""))
        assertTrue(json.contains("\"detour\": \"proxy\""))
        assertTrue(json.contains("\"domain_resolver\": \"dns-direct\""))
        assertTrue(json.contains("\"connect_timeout\": \"1200ms\""))
        assertTrue(json.contains("\"domain\": [\"vpn.example\"]"))
        assertTrue(json.contains("\"server\": \"dns-direct\""))
        assertTrue(json.contains("\"port\": 53, \"action\": \"hijack-dns\""))
        assertTrue(json.contains("\"action\": \"hijack-dns\""))
        assertTrue(json.contains("\"reality\""))
    }

    @Test
    fun appliesAdvancedNetworkSettings() {
        val json = SingBoxConfigBuilder().build(
            testCandidate(),
            LibertaSettings(
                dnsProvider = DnsProvider.CUSTOM,
                customDns = "9.9.9.9",
                autoMtu = false,
                mtu = 1420,
                ipv6Enabled = false,
                killSwitch = true,
                proxyEnabled = true,
                proxyPort = 10880
            )
        )

        assertTrue(json.contains("\"server\": \"9.9.9.9\""))
        assertTrue(json.contains("\"type\": \"tcp\""))
        assertTrue(json.contains("\"mtu\": 1420"))
        assertTrue(json.contains("\"strict_route\": true"))
        assertTrue(json.contains("\"type\": \"mixed\""))
        assertTrue(json.contains("\"listen_port\": 10880"))
    }

    @Test
    fun mapsKnownCustomDnsToDoh() {
        val json = SingBoxConfigBuilder().build(
            testCandidate(),
            LibertaSettings(
                dnsProvider = DnsProvider.CUSTOM,
                customDns = "8.8.8.8"
            )
        )

        assertTrue(json.contains("\"type\": \"https\""))
        assertTrue(json.contains("\"server\": \"8.8.8.8\""))
        assertTrue(json.contains("\"server_name\": \"dns.google\""))
    }

    @Test
    fun phantomCallRoutesSipAndRtpDirectly() {
        val json = SingBoxConfigBuilder().build(
            testCandidate(),
            LibertaSettings(labs = LabSettings(phantomCall = true))
        )

        assertTrue(json.contains("\"port\": [5060, 5061]"))
        assertTrue(json.contains("\"port_range\": \"10000:20000\""))
        assertTrue(json.contains("\"outbound\": \"direct\""))
    }

    private fun testCandidate(): ServerCandidate =
        ServerCandidate(
            id = "id",
            profile = ConnectionProfile.BLACKLISTS,
            rawLink = "vless://raw",
            uuid = "00000000-0000-0000-0000-000000000000",
            host = "vpn.example",
            port = 443,
            name = "Test",
            transport = "tcp",
            security = "reality",
            flow = "xtls-rprx-vision",
            fingerprint = "chrome",
            sni = "example.org",
            publicKey = "public-key",
            shortId = "abcd"
        )
}
