package com.liberta.vpn.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class SubscriptionRepository(private val context: Context) {
    suspend fun load(profile: ConnectionProfile, forceRefresh: Boolean): SubscriptionSnapshot =
        withContext(Dispatchers.IO) {
            val cache = cacheFile(profile)
            val content = if (forceRefresh || !cache.exists()) {
                fetch(profile).onSuccess { cache.writeText(it) }.getOrElse {
                    if (cache.exists()) cache.readText() else throw it
                }
            } else {
                cache.readText()
            }
            val candidates = VlessParser.parseSubscription(content, profile)
            SubscriptionSnapshot(
                profile = profile,
                candidates = candidates,
                lastUpdatedEpochMs = cache.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis(),
                fromCache = !forceRefresh,
                sourceUrl = profile.subscriptionUrl
            )
        }

    suspend fun cached(profile: ConnectionProfile): SubscriptionSnapshot? =
        withContext(Dispatchers.IO) {
            val cache = cacheFile(profile)
            if (!cache.exists()) return@withContext null
            val content = cache.readText()
            SubscriptionSnapshot(
                profile = profile,
                candidates = VlessParser.parseSubscription(content, profile),
                lastUpdatedEpochMs = cache.lastModified(),
                fromCache = true,
                sourceUrl = profile.subscriptionUrl
            )
        }

    private fun fetch(profile: ConnectionProfile): Result<String> = runCatching {
        val connection = (URL(profile.subscriptionUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Liberta/0.1 Android")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("subscription HTTP $code")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheFile(profile: ConnectionProfile): File {
        val dir = File(context.filesDir, "subscriptions").also { it.mkdirs() }
        return File(dir, "${profile.name.lowercase()}.txt")
    }
}
