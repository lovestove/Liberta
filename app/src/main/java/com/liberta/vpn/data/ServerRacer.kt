package com.liberta.vpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

class ServerRacer(
    private val timeoutMs: Int = 2_500,
    private val maxCandidates: Int = 96
) {
    suspend fun race(candidates: List<ServerCandidate>): RacingResult = coroutineScope {
        val eligible = candidates
            .filter { it.port in 1..65535 && it.host.isNotBlank() }
            .distinctBy { it.endpoint }
        val sample = eligible.stableProbeWindow(maxCandidates)
        if (sample.isEmpty()) return@coroutineScope RacingResult(null, emptyList(), "Нет VLESS серверов в подписке")

        val tested = sample.map { candidate ->
            async {
                val latency = probe(candidate)
                if (latency == Long.MAX_VALUE) candidate else candidate.withLatency(latency)
            }
        }.awaitAll()

        val successful = tested.filter { it.latencyMs != null }.sortedWith(
            compareBy<ServerCandidate> { if (it.port == 443) 0 else 1 }
                .thenBy { it.latencyMs }
        )
        RacingResult(
            selected = successful.firstOrNull(),
            tested = tested,
            error = if (successful.isEmpty()) "Серверы не ответили на TCP probe" else null
        )
    }

    private suspend fun probe(candidate: ServerCandidate): Long = withContext(Dispatchers.IO) {
        runCatching {
            val addresses = resolveProbeAddresses(candidate.host)
            var best = Long.MAX_VALUE
            for (address in addresses) {
                Socket().use { socket ->
                    val elapsed = measureTimeMillis {
                        socket.connect(InetSocketAddress(address, candidate.port), timeoutMs)
                    }
                    if (elapsed < best) best = elapsed
                }
            }
            best
        }.getOrElse { Long.MAX_VALUE }
    }

    private fun resolveProbeAddresses(host: String): List<InetAddress> {
        if (host.isIpLiteral()) return listOf(InetAddress.getByName(host))
        val resolved = InetAddress.getAllByName(host).toList()
        val ipv4 = resolved.filterIsInstance<Inet4Address>()
        return (ipv4.ifEmpty { resolved }).take(2)
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

private fun String.isIpLiteral(): Boolean =
    all { it.isDigit() || it == '.' } || contains(':')
