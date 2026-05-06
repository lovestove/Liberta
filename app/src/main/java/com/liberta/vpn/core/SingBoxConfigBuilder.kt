package com.liberta.vpn.core

import com.liberta.vpn.data.LibertaSettings
import com.liberta.vpn.data.ServerCandidate

class SingBoxConfigBuilder {
    fun build(selected: ServerCandidate, settings: LibertaSettings): String {
        val dnsServer = effectiveDnsServer(settings)
        val mtu = effectiveMtu(settings)
        val inbounds = buildList {
            add(tunInbound(mtu, settings.ipv6Enabled, settings.killSwitch))
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

        return """
            {
              "log": { "level": "info", "timestamp": true },
              "dns": {
                "servers": [
                  { "tag": "primary", "address": "${escape(dnsServer)}", "strategy": "prefer_ipv4", "detour": "direct" }
                ],
                "final": "primary"
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
                  { "inbound": "tun-in", "protocol": "dns", "action": "hijack-dns" }
                ],
                "auto_detect_interface": true,
                "final": "proxy"
              }
            }
        """.trimIndent()
    }

    private fun tunInbound(mtu: Int, ipv6Enabled: Boolean, killSwitch: Boolean): String {
        val addresses = if (ipv6Enabled) {
            "\"172.19.0.1/30\", \"fdfe:dcba:9876::1/126\""
        } else {
            "\"172.19.0.1/30\""
        }
        return """
            {
              "type": "tun",
              "tag": "tun-in",
              "interface_name": "liberta0",
              "address": [ $addresses ],
              "mtu": $mtu,
              "auto_route": false,
              "strict_route": $killSwitch,
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
            fields += "\"fragment\": { \"enabled\": true }"
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
        if (settings.autoMtu) 1500 else settings.mtu.coerceIn(1280, 9000)

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
