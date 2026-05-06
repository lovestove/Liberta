package com.liberta.vpn

import com.liberta.vpn.data.LabSettings
import com.liberta.vpn.data.LibertaSettings
import com.liberta.vpn.service.PhantomCallCoordinator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhantomCallCoordinatorTest {
    @Test
    fun enabledPhantomCallUsesFreeAutoBridgeWithoutManualSetup() = runTest {
        val session = PhantomCallCoordinator().prepare(
            LibertaSettings(labs = LabSettings(phantomCall = true))
        )

        assertNotNull(session)
        assertEquals(PhantomCallCoordinator.AUTO_BRIDGE_URL, session?.bridgeUrl)
        assertTrue(session?.roomUrl.orEmpty().startsWith("https://meet.jit.si/liberta-"))
    }
}
