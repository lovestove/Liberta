package com.liberta.vpn.service

import com.liberta.vpn.data.ConnectionProfile
import com.liberta.vpn.data.LabSettings
import com.liberta.vpn.data.LibertaSettings
import com.liberta.vpn.data.ServerCandidate
import com.liberta.vpn.data.VlessParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class PhantomCallSession(
    val id: String,
    val provider: String,
    val roomUrl: String,
    val roomToken: String,
    val bridgeUrl: String,
    val mimicry: String,
    val expiresInMinutes: Int,
    val bridgeReady: Boolean,
    val tunnelCandidate: ServerCandidate?
)

class PhantomCallCoordinator {
    suspend fun prepare(settings: LibertaSettings): PhantomCallSession? = withContext(Dispatchers.IO) {
        val labs = settings.labs
        if (!labs.phantomCall) return@withContext null

        val room = if (labs.phantomAutoGenerateRooms) {
            createMeeting(labs)
        } else {
            val manualUrl = labs.phantomCustomRoomUrl.trim()
            if (manualUrl.isBlank()) error("Phantom Call: укажите ссылку комнаты или включите автогенерацию")
            PhantomRoom(manualUrl, manualUrl.substringAfterLast('/').ifBlank { UUID.randomUUID().toString() })
        }

        val session = PhantomCallSession(
            id = UUID.randomUUID().toString(),
            provider = labs.phantomTransportService.label,
            roomUrl = room.url,
            roomToken = room.token,
            bridgeUrl = labs.phantomBridgeUrl.trim().ifBlank { AUTO_BRIDGE_URL },
            mimicry = labs.phantomMimicryType.label,
            expiresInMinutes = labs.phantomSessionMinutes,
            bridgeReady = false,
            tunnelCandidate = null
        )
        val bridgeResponse = signalBridge("join", session, labs)
        session.copy(
            bridgeReady = session.bridgeUrl != AUTO_BRIDGE_URL && bridgeResponse != null,
            tunnelCandidate = parseBridgeCandidate(bridgeResponse)
        )
    }

    suspend fun cleanup(session: PhantomCallSession?) = withContext(Dispatchers.IO) {
        if (session == null) return@withContext
        if (session.bridgeUrl == AUTO_BRIDGE_URL) return@withContext
        runCatching {
            val payload = JSONObject()
                .put("action", "leave")
                .put("session_id", session.id)
                .put("room_token", session.roomToken)
            postJson(session.bridgeUrl, payload)
        }
    }

    private fun createMeeting(labs: LabSettings): PhantomRoom {
        val providerSlug = labs.phantomTransportService.name.lowercase().replace('_', '-')
        val token = "liberta-$providerSlug-${UUID.randomUUID()}"
        return PhantomRoom("https://meet.jit.si/$token", token)
    }

    private fun signalBridge(action: String, session: PhantomCallSession, labs: LabSettings): String? {
        if (session.bridgeUrl == AUTO_BRIDGE_URL) return null
        val payload = JSONObject()
            .put("action", action)
            .put("session_id", session.id)
            .put("provider", session.provider)
            .put("room_url", session.roomUrl)
            .put("room_token", session.roomToken)
            .put("mimicry", labs.phantomMimicryType.name.lowercase())
            .put("noise", labs.phantomCamouflageNoise.name.lowercase())
            .put("expires_in_minutes", labs.phantomSessionMinutes)
        return postJson(session.bridgeUrl, payload)
    }

    private fun parseBridgeCandidate(response: String?): ServerCandidate? {
        if (response.isNullOrBlank()) return null
        val json = runCatching { JSONObject(response) }.getOrNull()
        val link = if (json != null) {
            firstNonBlank(
                json.optString("vless"),
                json.optString("vless_link"),
                json.optString("server_link"),
                json.optString("outbound")
            )
        } else {
            response
        }
        return VlessParser.parseLink(link, ConnectionProfile.BLACKLISTS)?.copy(name = "Phantom bridge")
    }

    private fun postJson(endpoint: String, payload: JSONObject): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Liberta/0.1 Android")
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("Phantom Call HTTP $code: ${body.take(180)}")
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun firstNonBlank(vararg values: String): String =
        values.firstOrNull { it.isNotBlank() }.orEmpty()

    private data class PhantomRoom(val url: String, val token: String)

    companion object {
        const val AUTO_BRIDGE_URL = "auto://free-jitsi-room"
    }
}
