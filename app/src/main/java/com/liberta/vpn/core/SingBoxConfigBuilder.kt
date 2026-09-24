package com.liberta.vpn.core

import com.liberta.vpn.data.LibertaSettings
import com.liberta.vpn.data.ServerCandidate
import com.liberta.vpn.data.TlsFingerprintProfile

class SingBoxConfigBuilder {
    fun build(selected: ServerCandidate, settings: LibertaSettings): String {
        val dnsServer = effectiveDnsServer(settings)
        val dnsRules = dnsRules(selected)
        val mtu = effectiveMtu(settings)
        
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
        
        val routeRules = buildRouteRules(selected, settings.labs.phantomCall)

        return """
            {
              "log": { "level": "info", "timestamp": true },
              "dns": {
                "servers": [
                  ${dnsServer.remoteJson()},
                  { "type": "tcp", "tag": "dns-direct", "server": "8.8.8.8", "server_port": 53 }
                ],
                $dnsRules
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
                "override_android_vpn": false,
                "final": "proxy"
              }
            }
        """.trimIndent()
    }
    
    private fun buildRouteRules(selected: ServerCandidate, phantomCall: Boolean): String {
        val rules = mutableListOf<String>()
        selectedServerDirectRule(selected)?.let { rules.add(it) }
        rules.add("{ \"ip_cidr\": \"172.19.0.2/32\", \"port\": 53, \"action\": \"hijack-dns\" }")
        rules.add("{ \"network\": [\"tcp\", \"udp\"], \"port\": 53, \"action\": \"hijack-dns\" }")
        rules.add("{ \"protocol\": \"dns\", \"action\": \"hijack-dns\" }")
        
        // Исключаем SIP/RTP для звонков (Phantom Call)
        if (phantomCall) {
            rules.add("{ \"network\": \"udp\", \"port\": [5060, 5061], \"action\": \"route\", \"outbound\": \"direct\" }")
            rules.add("{ \"network\": \"udp\", \"port_range\": \"10000:20000\", \"action\": \"route\", \"outbound\": \"direct\" }")
            rules.add("{ \"network\": \"tcp\", \"port\": [5060, 5061], \"action\": \"route\", \"outbound\": \"direct\" }")
        }
        
        rules.add("{ \"ip_cidr\": [\"10.0.0.0/8\", \"172.16.0.0/12\", \"192.168.0.0/16\", \"127.0.0.0/8\", \"100.64.0.0/10\", \"169.254.0.0/16\", \"224.0.0.0/4\"], \"action\": \"route\", \"outbound\": \"direct\" }")
        
        return rules.joinToString(",\n                  ")
    }

    private fun selectedServerDirectRule(selected: ServerCandidate): String? {
        val host = selected.host.trim()
        if (host.isBlank()) return null
        return if (host.isIpLiteral()) {
            val cidr = if (host.contains(':')) "$host/128" else "$host/32"
            "{ \"ip_cidr\": \"${escape(cidr)}\", \"action\": \"route\", \"outbound\": \"direct\" }"
        } else {
            "{ \"domain\": \"${escape(host)}\", \"action\": \"route\", \"outbound\": \"direct\" }"
        }
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
              "strict_route": true,
              "stack": "gvisor"
            }
        """.trimIndent()
    }

    private fun vlessOutbound(server: ServerCandidate, settings: LibertaSettings): String {
        val connectTimeout = if (settings.profile == com.liberta.vpn.data.ConnectionProfile.WHITELISTS) {
            "3500ms"
        } else {
            "1200ms"
        }
        val parts = mutableListOf(
            "\"type\": \"vless\"",
            "\"tag\": \"proxy\"",
            "\"server\": \"${escape(server.host)}\"",
            "\"server_port\": ${server.port}",
            "\"uuid\": \"${escape(server.uuid)}\"",
            "\"packet_encoding\": \"xudp\"",
            "\"domain_resolver\": \"dns-direct\"",
            "\"connect_timeout\": \"$connectTimeout\""
        )
        server.flow?.takeIf { it.isNotBlank() }?.let { parts += "\"flow\": \"${escape(it)}\"" }
        if (server.security.equals("tls", true) || server.security.equals("reality", true)) {
            parts += "\"tls\": ${tlsBlock(server, settings)}"
        }
        transportBlock(server)?.let { parts += "\"transport\": $it" }
        return "{ ${parts.joinToString(", ")} }"
    }

    private fun tlsBlock(server: ServerCandidate, settings: LibertaSettings): String {
        val fingerprint = server.fingerprint?.takeIf { it.isNotBlank() }
            ?: settings.labs.tlsFingerprintProfile.toSingBoxFingerprint()
        val fields = mutableListOf(
            "\"enabled\": true",
            "\"server_name\": \"${escape(server.sni ?: server.host)}\"",
            "\"utls\": { \"enabled\": true, \"fingerprint\": \"${escape(fingerprint)}\" }"
        )
        if (server.security.equals("reality", true)) {
            fields += "\"reality\": { \"enabled\": true, \"public_key\": \"${escape(server.publicKey.orEmpty())}\", \"short_id\": \"${escape(server.shortId.orEmpty())}\" }"
        }
        if (server.allowInsecure && !server.security.equals("reality", true)) {
            fields += "\"insecure\": true"
        }
        if (server.alpn.isNotEmpty()) {
            fields += "\"alpn\": [${server.alpn.joinToString(", ") { "\"${escape(it)}\"" }}]"
        }
        return "{ ${fields.joinToString(", ")} }"
    }

    private fun transportBlock(server: ServerCandidate): String? =
        when (server.transport.lowercase()) {
            "ws" -> wsTransportBlock(server)
            "grpc" -> "{ \"type\": \"grpc\", \"service_name\": \"${escape(server.serviceName ?: server.path ?: "")}\" }"
            "xhttp" -> "{ \"type\": \"xhttp\", \"path\": \"${escape(server.path ?: "/")}\" }"
            "tcp", "raw" -> null
            else -> null
        }

    private fun wsTransportBlock(server: ServerCandidate): String {
        val parts = mutableListOf(
            "\"type\": \"ws\"",
            "\"path\": \"${escape(server.path ?: "/")}\""
        )
        server.hostHeader?.takeIf { it.isNotBlank() }?.let { host ->
            parts += "\"headers\": { \"Host\": \"${escape(host)}\" }"
        }
        return "{ ${parts.joinToString(", ")} }"
    }

    private fun effectiveDnsServer(settings: LibertaSettings): DnsEndpoint =
        when (settings.dnsProvider) {
            com.liberta.vpn.data.DnsProvider.CLOUDFLARE -> DnsEndpoint.tcp("1.1.1.1")
            com.liberta.vpn.data.DnsProvider.GOOGLE -> DnsEndpoint.tcp("8.8.8.8")
            com.liberta.vpn.data.DnsProvider.ADGUARD -> DnsEndpoint.tcp("94.140.14.14")
            com.liberta.vpn.data.DnsProvider.CUSTOM -> {
                val custom = settings.customDns.trim()
                DnsEndpoint.forAddress(custom.takeIf { it.isNotBlank() } ?: "1.1.1.1")
            }
            else -> DnsEndpoint.tcp("1.1.1.1")
        }

    private fun effectiveMtu(settings: LibertaSettings): Int =
        if (settings.autoMtu) 1280 else settings.mtu.coerceIn(1280, 9000)

    private fun dnsRules(selected: ServerCandidate): String {
        val host = selected.host.trim()
        if (host.isBlank() || host.isIpLiteral()) return "\"rules\": [],"
        return """"rules": [
                  { "domain": ["${escape(host)}"], "action": "route", "server": "dns-direct", "strategy": "ipv4_only" }
                ],"""
    }

    private fun DnsEndpoint.remoteJson(): String {
        val escapedServer = escape(server)
        val host = dohHost
        return if (host != null) {
            val escapedHost = escape(host)
            """{ "type": "https", "tag": "dns-remote", "server": "$escapedServer", "server_port": 443, "path": "/dns-query", "headers": { "Host": "$escapedHost" }, "tls": { "enabled": true, "server_name": "$escapedHost" }, "detour": "proxy" }"""
        } else {
            """{ "type": "tcp", "tag": "dns-remote", "server": "$escapedServer", "server_port": 53, "detour": "proxy" }"""
        }
    }

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

    private data class DnsEndpoint(
        val server: String,
        val dohHost: String? = null
    ) {
        companion object {
            fun doh(server: String, host: String): DnsEndpoint = DnsEndpoint(server, host)
            fun tcp(server: String): DnsEndpoint = DnsEndpoint(server)

            fun forAddress(server: String): DnsEndpoint = tcp(server)
        }
    }
}

private fun String.isIpLiteral(): Boolean =
    all { it.isDigit() || it == '.' } || contains(':')

private fun TlsFingerprintProfile.toSingBoxFingerprint(): String =
    when (this) {
        TlsFingerprintProfile.CHROME_WINDOWS -> "chrome"
        TlsFingerprintProfile.SAFARI_IOS -> "safari"
        TlsFingerprintProfile.FIREFOX_LINUX -> "firefox"
    }
