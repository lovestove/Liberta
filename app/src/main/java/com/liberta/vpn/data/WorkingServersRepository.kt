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
            val records = load(profile)
            if (records.isEmpty()) return@withContext candidates

            val byId = candidates.associateBy { it.id }
            val prioritized = records
                .sorted()
                .map { record -> byId[record.candidate.id] ?: record.candidate }
            val usedIds = prioritized.mapTo(LinkedHashSet()) { it.id }
            val rest = candidates.filterNot { usedIds.contains(it.id) }
            (prioritized + rest).distinctBy { it.id }
        }

    suspend fun remembered(
        profile: ConnectionProfile,
        candidates: List<ServerCandidate>,
        userVerifiedOnly: Boolean = true
    ): List<ServerCandidate> =
        withContext(Dispatchers.IO) {
            val byId = candidates.associateBy { it.id }
            load(profile)
                .asSequence()
                .sorted()
                .filter { !userVerifiedOnly || it.priority == WorkingPriority.USER_VERIFIED }
                .map { record -> byId[record.candidate.id] ?: record.candidate }
                .distinctBy { it.id }
                .take(MAX_WARM_ATTEMPTS)
                .toList()
        }

    suspend fun rememberWorking(server: ServerCandidate) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = load(server.profile)
        val updated = mergeRecords(
            existing = existing,
            incoming = listOf(server),
            priority = WorkingPriority.USER_VERIFIED,
            now = now
        )
        write(server.profile, updated)
    }

    suspend fun rememberBackground(profile: ConnectionProfile, servers: List<ServerCandidate>) =
        withContext(Dispatchers.IO) {
            if (servers.isEmpty()) return@withContext
            val now = System.currentTimeMillis()
            val updated = mergeRecords(
                existing = load(profile),
                incoming = servers.distinctBy { it.id }.take(MAX_BACKGROUND_VALIDATED_PER_PROFILE),
                priority = WorkingPriority.BACKGROUND_VALIDATED,
                now = now
            )
            write(profile, updated)
        }

    suspend fun forget(server: ServerCandidate) = withContext(Dispatchers.IO) {
        val updated = load(server.profile).filterNot { it.candidate.id == server.id }
        write(server.profile, updated)
    }

    private fun mergeRecords(
        existing: List<WorkingServerRecord>,
        incoming: List<ServerCandidate>,
        priority: WorkingPriority,
        now: Long
    ): List<WorkingServerRecord> {
        val byId = LinkedHashMap<String, WorkingServerRecord>()
        existing.sorted().forEach { byId[it.candidate.id] = it }
        incoming.forEach { candidate ->
            val current = byId[candidate.id]
            val nextPriority = when {
                current?.priority == WorkingPriority.USER_VERIFIED -> WorkingPriority.USER_VERIFIED
                else -> priority
            }
            byId[candidate.id] = WorkingServerRecord(candidate, nextPriority, now)
        }
        return byId.values
            .sorted()
            .take(MAX_REMEMBERED_PER_PROFILE)
    }

    private fun load(profile: ConnectionProfile): List<WorkingServerRecord> {
        val file = fileFor(profile)
        if (!file.exists()) return emptyList()
        val root = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return emptyList()
        val items = root.optJSONArray("servers") ?: JSONArray()
        return buildList {
            for (index in 0 until items.length()) {
                val raw = items.optJSONObject(index) ?: continue
                parseCandidate(raw, profile)?.let { candidate ->
                    val source = WorkingPriority.fromSource(raw.optString("source"))
                    val lastSeen = raw.optLong("lastSeen", root.optLong("savedAt", 0L))
                        .takeIf { it > 0L }
                        ?: System.currentTimeMillis()
                    add(WorkingServerRecord(candidate, source, lastSeen))
                }
            }
        }
    }

    private fun write(profile: ConnectionProfile, records: List<WorkingServerRecord>) {
        val root = JSONObject().apply {
            put("profile", profile.name)
            put("savedAt", System.currentTimeMillis())
            put("servers", JSONArray().apply {
                records.sorted().forEach { put(it.toJson()) }
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
            alpn = raw.optJSONArray("alpn").toStringList(),
            allowInsecure = raw.optBoolean("allowInsecure", false),
            publicKey = raw.optString("publicKey").takeIf { it.isNotBlank() },
            shortId = raw.optString("shortId").takeIf { it.isNotBlank() },
            path = raw.optString("path").takeIf { it.isNotBlank() },
            hostHeader = raw.optString("hostHeader").takeIf { it.isNotBlank() },
            serviceName = raw.optString("serviceName").takeIf { it.isNotBlank() }
        )
    }

    private fun WorkingServerRecord.toJson(): JSONObject = candidate.toJson().apply {
        put("source", priority.source)
        put("lastSeen", lastSeenEpochMs)
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
        put("alpn", JSONArray().apply { alpn.forEach { put(it) } })
        put("allowInsecure", allowInsecure)
        put("publicKey", publicKey)
        put("shortId", shortId)
        put("path", path)
        put("hostHeader", hostHeader)
        put("serviceName", serviceName)
    }

    private data class WorkingServerRecord(
        val candidate: ServerCandidate,
        val priority: WorkingPriority,
        val lastSeenEpochMs: Long
    ) : Comparable<WorkingServerRecord> {
        override fun compareTo(other: WorkingServerRecord): Int =
            compareValuesBy(this, other, { it.priority.rank }, { -it.lastSeenEpochMs })
    }

    private enum class WorkingPriority(val source: String, val rank: Int) {
        USER_VERIFIED("user_verified", 0),
        BACKGROUND_VALIDATED("background_validated", 1);

        companion object {
            fun fromSource(value: String): WorkingPriority =
                entries.firstOrNull { it.source == value } ?: USER_VERIFIED
        }
    }

    companion object {
        private const val MAX_WARM_ATTEMPTS = 8
        private const val MAX_BACKGROUND_VALIDATED_PER_PROFILE = 48
        private const val MAX_REMEMBERED_PER_PROFILE = 96
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}
