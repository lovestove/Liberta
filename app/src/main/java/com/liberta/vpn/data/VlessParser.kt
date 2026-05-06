package com.liberta.vpn.data

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

object VlessParser {
    fun parseSubscription(content: String, profile: ConnectionProfile): List<ServerCandidate> {
        val direct = parseLines(content, profile)
        if (direct.isNotEmpty()) return direct

        val decoded = decodeBase64Subscription(content) ?: return emptyList()
        return parseLines(decoded, profile)
    }

    fun parseLink(rawLink: String, profile: ConnectionProfile): ServerCandidate? {
        val trimmed = rawLink.trim()
        if (!trimmed.startsWith("vless://", ignoreCase = true)) return null

        val body = trimmed.removePrefix("vless://").removePrefix("VLESS://")
        val fragmentSplit = body.split("#", limit = 2)
        val beforeFragment = fragmentSplit[0]
        val name = fragmentSplit.getOrNull(1)?.let(::decodeUrl)?.trim().orEmpty()
        val querySplit = beforeFragment.split("?", limit = 2)
        val authority = querySplit[0]
        val query = querySplit.getOrNull(1).orEmpty()
        val atIndex = authority.indexOf('@')
        if (atIndex <= 0 || atIndex == authority.lastIndex) return null

        val uuid = authority.substring(0, atIndex)
        val hostPort = authority.substring(atIndex + 1)
        val host: String
        val port: Int
        if (hostPort.startsWith("[")) {
            val end = hostPort.indexOf(']')
            if (end <= 0 || hostPort.getOrNull(end + 1) != ':') return null
            host = hostPort.substring(1, end)
            port = hostPort.substring(end + 2).toIntOrNull() ?: return null
        } else {
            val colon = hostPort.lastIndexOf(':')
            if (colon <= 0 || colon == hostPort.lastIndex) return null
            host = hostPort.substring(0, colon)
            port = hostPort.substring(colon + 1).toIntOrNull() ?: return null
        }

        val params = parseQuery(query)
        return ServerCandidate(
            id = stableId(trimmed),
            profile = profile,
            rawLink = trimmed,
            uuid = uuid,
            host = host,
            port = port,
            name = name,
            transport = params["type"]?.ifBlank { "tcp" } ?: "tcp",
            security = params["security"]?.ifBlank { "none" } ?: "none",
            flow = params["flow"],
            fingerprint = params["fp"],
            sni = params["sni"],
            publicKey = params["pbk"],
            shortId = params["sid"],
            path = params["path"] ?: params["spx"],
            serviceName = params["serviceName"]
        )
    }

    private fun parseLines(content: String, profile: ConnectionProfile): List<ServerCandidate> =
        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { parseLink(it, profile) }
            .distinctBy { it.id }
            .toList()

    private fun parseQuery(query: String): Map<String, String> =
        query.split("&")
            .asSequence()
            .filter { it.isNotBlank() }
            .map {
                val pair = it.split("=", limit = 2)
                decodeUrl(pair[0]) to decodeUrl(pair.getOrElse(1) { "" })
            }
            .toMap()

    private fun decodeBase64Subscription(content: String): String? {
        val compact = content.lineSequence()
            .filterNot { it.trimStart().startsWith("#") }
            .joinToString("") { it.trim() }
        if (compact.isBlank()) return null
        return runCatching {
            val padded = compact + "=".repeat((4 - compact.length % 4) % 4)
            String(Base64.getDecoder().decode(padded), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    private fun decodeUrl(value: String): String =
        runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }.getOrDefault(value)

    private fun stableId(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
