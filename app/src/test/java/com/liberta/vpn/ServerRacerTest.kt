package com.liberta.vpn

import com.liberta.vpn.data.ConnectionProfile
import com.liberta.vpn.data.ServerCandidate
import com.liberta.vpn.data.ServerRacer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerRacerTest {
    @Test
    fun racesEndpointDiverseWindowInsteadOfDuplicateSubscriptionHead() = runTest {
        val repeatedHead = List(20) { index ->
            testCandidate("dup-$index", "127.0.0.1", 1)
        }
        val tail = listOf(
            testCandidate("tail-1", "127.0.0.2", 1),
            testCandidate("tail-2", "127.0.0.3", 1)
        )

        val result = ServerRacer(timeoutMs = 1, maxCandidates = 10).race(repeatedHead + tail)

        assertEquals(result.tested.size, result.tested.map { it.endpoint }.distinct().size)
        assertTrue(result.tested.any { it.host == "127.0.0.2" })
        assertTrue(result.tested.any { it.host == "127.0.0.3" })
    }

    private fun testCandidate(id: String, host: String, port: Int): ServerCandidate =
        ServerCandidate(
            id = id,
            profile = ConnectionProfile.WHITELISTS,
            rawLink = "vless://$id@$host:$port",
            uuid = "00000000-0000-4000-8000-000000000000",
            host = host,
            port = port,
            name = id
        )
}
