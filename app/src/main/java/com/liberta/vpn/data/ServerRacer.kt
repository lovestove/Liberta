package com.liberta.vpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

class ServerRacer(
    private val timeoutMs: Int = 1_800,
    private val maxCandidates: Int = 24
) {
    suspend fun race(candidates: List<ServerCandidate>): RacingResult = coroutineScope {
        val eligible = candidates
            .filter { it.port in 1..65535 && it.host.isNotBlank() }
        val sample = eligible.stableProbeWindow(maxCandidates)
        if (sample.isEmpty()) return@coroutineScope RacingResult(null, emptyList(), "Нет VLESS серверов в подписке")

        val tested = sample.map { candidate ->
            async {
                val latency = probe(candidate)
                if (latency == Long.MAX_VALUE) candidate else candidate.withLatency(latency)
            }
        }.awaitAll()

        val successful = tested.filter { it.latencyMs != null }.sortedBy { it.latencyMs }
        RacingResult(
            selected = successful.firstOrNull(),
            tested = tested,
            error = if (successful.isEmpty()) "Серверы не ответили на TCP probe" else null
        )
    }

    private suspend fun probe(candidate: ServerCandidate): Long = withContext(Dispatchers.IO) {
        runCatching {
            var elapsed = 0L
            Socket().use { socket ->
                elapsed = measureTimeMillis {
                    socket.connect(InetSocketAddress(candidate.host, candidate.port), timeoutMs)
                }
            }
            elapsed
        }.getOrElse { Long.MAX_VALUE }
    }

    private fun List<ServerCandidate>.stableProbeWindow(limit: Int): List<ServerCandidate> {
        if (size <= limit) return this
        val head = take(limit / 2)
        val spreadCount = limit - head.size
        val stride = size.toDouble() / spreadCount.toDouble()
        val spread = List(spreadCount) { index -> this[(index * stride).toInt().coerceIn(indices)] }
        return (head + spread).distinctBy { it.id }.take(limit)
    }
}
