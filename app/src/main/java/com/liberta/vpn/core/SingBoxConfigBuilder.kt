package com.liberta.vpn.core

import com.liberta.vpn.data.ConnectionProfile
import com.liberta.vpn.data.LibertaSettings
import com.liberta.vpn.data.ServerCandidate

class SingBoxConfigBuilder {
    fun build(selected: ServerCandidate, settings: LibertaSettings): String {
        val dnsServer = effectiveDnsServer(settings)
        val mtu = effectiveMtu(settings)
        val profile = settings.profile
        val isWhitelists = profile == ConnectionProfile.WHITELISTS
        
        val inbounds = buildList {
            add(tunInbound(mtu, settings.ipv6Enabled))
            if (settings.proxyEnabled) {
                add(
                    """
                    {
                      "type": "mixed",
                      "tag": "proxy-in",
                      "listen": "0.0.0.0",
                      "listen_port": ${settings.proxyPort.coerceIn(1024, 65535)}
                    }
                    """.trimIndent()
                )
            }
        }.joinToString(",\n")
        
        val routeRules = buildRouteRules(isWhitelists, settings.labs.phantomCall)

        return """
            {
              "log": { "level": "info", "timestamp": true },
              "dns": {
                "servers": [
                  { "tag": "dns-remote", "address": "$dnsServer", "strategy": "ipv4_only" },
                  { "tag": "dns-direct", "address": "8.8.8.8", "strategy": "ipv4_only" }
                ],
                "final": "dns-remote"
              },
              "inbounds": [
                $inbounds
              ],
              "outbounds": [
                ${vlessOutbound(selected, settings)},
                { "type": "direct", "tag": "direct" },
                { "type": "block", "tag": "block" }
              ],
              "route": {
                "rules": [
                  $routeRules
                ],
                "auto_detect_interface": true,
                "final": ${if (isWhitelists) "\"direct\"" else "\"proxy\""}
              }
            }
        """.trimIndent()
    }
    
    private fun buildRouteRules(isWhitelists: Boolean, phantomCall: Boolean): String {
        val rules = mutableListOf<String>()
        
        // Исключаем SIP/RTP для звонков (Phantom Call)
        if (phantomCall) {
            rules.add("{ \"network\": \"udp\", \"port\": [5060, 5061, 10000, 20000], \"outbound\": \"direct\" }")
            rules.add("{ \"network\": \"tcp\", \"port\": [5060, 5061], \"outbound\": \"direct\" }")
        }
        
        if (isWhitelists) {
            // Белые списки: российские сайты идут напрямую, остальное через VPN
            rules.add("{ \"domain_suffix\": [\".ru\", \".рф\", \".su\", \"yandex.ru\", \"vk.com\", \"mail.ru\", \"sberbank.ru\", \"gosuslugi.ru\", \"rutube.ru\", \"kinopoisk.ru\"], \"outbound\": \"direct\" }")
            rules.add("{ \"ip_cidr\": [\"10.0.0.0/8\", \"172.16.0.0/12\", \"192.168.0.0/16\", \"127.0.0.0/8\", \"100.64.0.0/10\", \"169.254.0.0/16\", \"224.0.0.0/4\"], \"outbound\": \"direct\" }")
        } else {
            // Черные списки: локальные адреса идут напрямую, остальное через VPN
            rules.add("{ \"ip_cidr\": [\"10.0.0.0/8\", \"172.16.0.0/12\", \"192.168.0.0/16\", \"127.0.0.0/8\", \"100.64.0.0/10\", \"169.254.0.0/16\", \"224.0.0.0/4\"], \"outbound\": \"direct\" }")
        }
        
        return rules.joinToString(",\n                  ")
    }

    private fun tunInbound(mtu: Int, ipv6Enabled: Boolean): String {
        val addresses = if (ipv6Enabled) {
            "\"172.19.0.1/28\", \"fdfe:dcba:9876::1/126\""
        } else {
            "\"172.19.0.1/28\""
        }
        return """
            {
              "type": "tun",
              "tag": "tun-in",
              "address": [ $addresses ],
              "mtu": $mtu,
              "auto_route": true,
              "stack": "gvisor"
            }
        """.trimIndent()
    }

    private fun vlessOutbound(server: ServerCandidate, settings: LibertaSettings): String {
        val parts = mutableListOf(
            "\"type\": \"vless\"",
            "\"tag\": \"proxy\"",
            "\"server\": \"${escape(server.host)}\"",
            "\"server_port\": ${server.port}",
            "\"uuid\": \"${escape(server.uuid)}\"",
            "\"packet_encoding\": \"xudp\""
        )
        server.flow?.takeIf { it.isNotBlank() }?.let { parts += "\"flow\": \"${escape(it)}\"" }
        if (server.security.equals("tls", true) || server.security.equals("reality", true)) {
            parts += "\"tls\": ${tlsBlock(server, settings)}"
        }
        transportBlock(server)?.let { parts += "\"transport\": $it" }
        return "{ ${parts.joinToString(", ")} }"
    }

    private fun tlsBlock(server: ServerCandidate, settings: LibertaSettings): String {
        val fields = mutableListOf(
            "\"enabled\": true",
            "\"server_name\": \"${escape(server.sni ?: server.host)}\"",
            "\"utls\": { \"enabled\": true, \"fingerprint\": \"${escape(server.fingerprint ?: "chrome")}\" }"
        )
        if (server.security.equals("reality", true)) {
            fields += "\"reality\": { \"enabled\": true, \"public_key\": \"${escape(server.publicKey.orEmpty())}\", \"short_id\": \"${escape(server.shortId.orEmpty())}\" }"
        }
        if (settings.labs.polymorphicCore) {
            fields += "\"fragment\": { \"enabled\": true, \"packets\": \"1-3\", \"length\": \"5-10\", \"interval\": \"1-5\" }"
        }
        return "{ ${fields.joinToString(", ")} }"
    }

    private fun transportBlock(server: ServerCandidate): String? =
        when (server.transport.lowercase()) {
            "ws" -> "{ \"type\": \"ws\", \"path\": \"${escape(server.path ?: "/")}\" }"
            "grpc" -> "{ \"type\": \"grpc\", \"service_name\": \"${escape(server.serviceName ?: server.path ?: "")}\" }"
            "xhttp" -> "{ \"type\": \"xhttp\", \"path\": \"${escape(server.path ?: "/")}\" }"
            "tcp", "raw" -> null
            else -> null
        }

    private fun effectiveDnsServer(settings: LibertaSettings): String =
        when (settings.dnsProvider) {
            com.liberta.vpn.data.DnsProvider.CUSTOM -> settings.customDns.takeIf { it.isNotBlank() } ?: "1.1.1.1"
            else -> settings.dnsProvider.servers.firstOrNull() ?: "1.1.1.1"
        }

    private fun effectiveMtu(settings: LibertaSettings): Int =
        if (settings.autoMtu) 1280 else settings.mtu.coerceIn(1280, 9000)

    private fun escape(value: String): String =
        buildString(value.length) {
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
}
