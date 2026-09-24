package com.liberta.vpn

import com.liberta.vpn.data.ConnectionProfile
import com.liberta.vpn.data.VlessParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Base64

class VlessParserTest {
    @Test
    fun parsesRealityTcpVlessLink() {
        val link = "vless://01bd3a30-ad45-4ec2-b40d-426a6448504c@example.com:443?flow=xtls-rprx-vision&encryption=none&type=tcp&security=reality&fp=chrome&sni=max.ru&pbk=public&sid=abcd#Canada%20VK"

        val candidate = VlessParser.parseLink(link, ConnectionProfile.WHITELISTS)

        assertNotNull(candidate)
        candidate!!
        assertEquals("example.com", candidate.host)
        assertEquals(443, candidate.port)
        assertEquals("tcp", candidate.transport)
        assertEquals("reality", candidate.security)
        assertEquals("max.ru", candidate.sni)
        assertEquals("Canada VK", candidate.name)
    }

    @Test
    fun parsesBase64Subscription() {
        val content = """
            # profile-title: test
            vless://00000000-0000-0000-0000-000000000000@one.example:443?type=ws&security=tls&path=%2Fws#One
            vless://00000000-0000-0000-0000-000000000001@two.example:8443?type=grpc&security=reality&serviceName=svc&pbk=key#Two
        """.trimIndent()
        val encoded = Base64.getEncoder().encodeToString(content.toByteArray())

        val candidates = VlessParser.parseSubscription(encoded, ConnectionProfile.BLACKLISTS)

        assertEquals(2, candidates.size)
        assertEquals("ws", candidates[0].transport)
        assertEquals("/ws", candidates[0].path)
        assertEquals("grpc", candidates[1].transport)
    }

    @Test
    fun rejectsLinksWithoutVlessUuid() {
        val link = "vless://TelegramChannel@example.com:443?type=tcp&security=reality#bad"

        val candidate = VlessParser.parseLink(link, ConnectionProfile.BLACKLISTS)

        assertNull(candidate)
    }

    @Test
    fun parsesLinksWithSlashBeforeQuery() {
        val link = "vless://320f370f-2302-475d-8bb3-379ee7a9a9b3@fi19.skytunnel.pw:8443/?type=tcp&security=reality&sni=bunny.net&pbk=key#White"

        val candidate = VlessParser.parseLink(link, ConnectionProfile.WHITELISTS)

        assertNotNull(candidate)
        candidate!!
        assertEquals("fi19.skytunnel.pw", candidate.host)
        assertEquals(8443, candidate.port)
        assertEquals("bunny.net", candidate.sni)
    }

    @Test
    fun preservesTransportTlsCompatibilityParameters() {
        val link = "vless://00000000-0000-0000-0000-000000000002@example.com:443?type=ws&security=tls&path=%2Fedge&host=cdn.example.com&alpn=h2%2Chttp%2F1.1&allowInsecure=1#Ws"

        val candidate = VlessParser.parseLink(link, ConnectionProfile.BLACKLISTS)

        assertNotNull(candidate)
        candidate!!
        assertEquals(listOf("h2", "http/1.1"), candidate.alpn)
        assertEquals(true, candidate.allowInsecure)
        assertEquals("cdn.example.com", candidate.hostHeader)
        assertEquals("/edge", candidate.path)
    }
}
