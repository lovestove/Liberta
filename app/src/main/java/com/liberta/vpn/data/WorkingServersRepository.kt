package com.liberta.vpn.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class WorkingServersRepository(private val context: Context) {
    suspend fun prioritize(profile: ConnectionProfile, candidates: List<ServerCandidate>): List<ServerCandidate> =
        withContext(Dispatchers.IO) {
            if (candidates.isEmpty()) return@withContext emptyList()
            val remembered = load(profile)
            if (remembered.isEmpty()) return@withContext candidates

            val byId = candidates.associateBy { it.id }
            val prioritizedRemembered = remembered.map { byId[it.id] ?: it }
            val usedIds = prioritizedRemembered.mapTo(LinkedHashSet()) { it.id }
            val rest = candidates.filterNot { usedIds.contains(it.id) }
            (prioritizedRemembered + rest).distinctBy { it.id }
        }

    suspend fun rememberWorking(server: ServerCandidate) = withContext(Dispatchers.IO) {
        val existing = load(server.profile)
        val updated = (listOf(server) + existing.filterNot { it.id == server.id })
            .take(MAX_REMEMBERED_PER_PROFILE)
        write(server.profile, updated)
    }

    private fun load(profile: ConnectionProfile): List<ServerCandidate> {
        val file = fileFor(profile)
        if (!file.exists()) return emptyList()
        val root = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return emptyList()
        val items = root.optJSONArray("servers") ?: JSONArray()
        return buildList {
            for (index in 0 until items.length()) {
                val raw = items.optJSONObject(index) ?: continue
                parseCandidate(raw, profile)?.let(::add)
            }
        }
    }

    private fun write(profile: ConnectionProfile, candidates: List<ServerCandidate>) {
        val root = JSONObject().apply {
            put("profile", profile.name)
            put("savedAt", System.currentTimeMillis())
            put("servers", JSONArray().apply {
                candidates.forEach { put(it.toJson()) }
            })
        }
        fileFor(profile).writeText(root.toString())
    }

    private fun fileFor(profile: ConnectionProfile): File {
        val dir = File(context.filesDir, "working_servers").also { it.mkdirs() }
        return File(dir, "${profile.name.lowercase()}.json")
    }

    private fun parseCandidate(raw: JSONObject, profile: ConnectionProfile): ServerCandidate? {
        val id = raw.optString("id")
        val host = raw.optString("host")
        val rawLink = raw.optString("rawLink")
        val uuid = raw.optString("uuid")
        val port = raw.optInt("port", -1)
        if (id.isBlank() || host.isBlank() || rawLink.isBlank() || uuid.isBlank() || port !in 1..65535) {
            return null
        }
        return ServerCandidate(
            id = id,
            profile = profile,
            rawLink = rawLink,
            uuid = uuid,
            host = host,
            port = port,
            name = raw.optString("name"),
            transport = raw.optString("transport", "tcp"),
            security = raw.optString("security", "reality"),
            flow = raw.optString("flow").takeIf { it.isNotBlank() },
            fingerprint = raw.optString("fingerprint").takeIf { it.isNotBlank() },
            sni = raw.optString("sni").takeIf { it.isNotBlank() },
            publicKey = raw.optString("publicKey").takeIf { it.isNotBlank() },
            shortId = raw.optString("shortId").takeIf { it.isNotBlank() },
            path = raw.optString("path").takeIf { it.isNotBlank() },
            serviceName = raw.optString("serviceName").takeIf { it.isNotBlank() }
        )
    }

    private fun ServerCandidate.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("rawLink", rawLink)
        put("uuid", uuid)
        put("host", host)
        put("port", port)
        put("name", name)
        put("transport", transport)
        put("security", security)
        put("flow", flow)
        put("fingerprint", fingerprint)
        put("sni", sni)
        put("publicKey", publicKey)
        put("shortId", shortId)
        put("path", path)
        put("serviceName", serviceName)
    }

    companion object {
        private const val MAX_REMEMBERED_PER_PROFILE = 24
    }
}
