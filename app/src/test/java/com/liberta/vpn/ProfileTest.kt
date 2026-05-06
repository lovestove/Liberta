package com.liberta.vpn

import com.liberta.vpn.data.ConnectionProfile
import com.liberta.vpn.data.DnsProvider
import com.liberta.vpn.data.ConnectionMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {
    @Test
    fun specSubscriptionUrlsArePinned() {
        assertTrue(ConnectionProfile.WHITELISTS.subscriptionUrl.endsWith("WHITE-CIDR-RU-all.txt"))
        assertTrue(ConnectionProfile.BLACKLISTS.subscriptionUrl.endsWith("BLACK_VLESS_RUS.txt"))
    }

    @Test
    fun invalidNamesUseSafeDefaults() {
        assertEquals(ConnectionProfile.BLACKLISTS, ConnectionProfile.fromName("missing"))
        assertEquals(DnsProvider.SMART, DnsProvider.fromName("missing"))
        assertEquals(ConnectionMethod.BLACKLISTS, ConnectionMethod.fromName("missing"))
    }

    @Test
    fun meshAccessIsSeparateFromRelayHelping() {
        assertEquals(ConnectionProfile.BLACKLISTS, ConnectionMethod.MESH_ACCESS.profile)
    }
}
