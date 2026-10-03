package com.wavebalance.app

import com.wavebalance.app.model.NetworkGroups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkGroupsTest {

    @Test
    fun ownNetwork_containsEveryMeshRadioAndNoNeighbors() {
        val own = NetworkGroups.ownNetwork(TestMesh.all, TestMesh.connected.bssid, "Home_5G")

        assertEquals(TestMesh.home.map { it.bssid }.toSet(), own)
    }

    @Test
    fun ownNetwork_usesSsidWhenConnectedApIsNotInScan() {
        val scanWithoutConnected = TestMesh.all.filter { it != TestMesh.connected }

        val own = NetworkGroups.ownNetwork(scanWithoutConnected, TestMesh.connected.bssid, "Home_5G")

        assertEquals((TestMesh.home - TestMesh.connected).map { it.bssid }.toSet(), own)
    }

    @Test
    fun ownNetwork_includesNetworksTaggedAsHome() {
        val tagged = TestMesh.printer.copy(isUserTaggedHome = true)
        val aps = TestMesh.home + tagged

        val own = NetworkGroups.ownNetwork(aps, TestMesh.connected.bssid, "Home_5G")

        assertTrue(tagged.bssid in own)
    }

    @Test
    fun ownNetwork_isEmptyWhenNotConnected() {
        val disconnected = TestMesh.all.map { it.copy(isConnected = false) }

        assertTrue(NetworkGroups.ownNetwork(disconnected, null, null).isEmpty())
    }

    @Test
    fun siblings_guestSsidAndOtherBandOfOneRadio() {
        // Guest SSID: locally administered bit set and octet 4 changed
        assertTrue(NetworkGroups.isSiblingBssid("3c:52:a1:7e:10:c5", "3e:52:a1:5e:10:c5"))
        // 2.4 GHz radio one address below the 5 GHz radio
        assertTrue(NetworkGroups.isSiblingBssid("3c:52:a1:7e:10:c5", "3c:52:a1:7e:10:c4"))
        assertTrue(NetworkGroups.isSiblingBssid("AE:61:20:44:2B:51", "b2:61:20:44:2b:50"))
    }

    @Test
    fun notSiblings_sameVendorDifferentRouter() {
        assertFalse(NetworkGroups.isSiblingBssid("3c:52:a1:7e:10:c5", "3c:52:a1:12:34:56"))
        // Same OUI and nearly the same serial, but octet 4 differs without the first octet changing
        assertFalse(NetworkGroups.isSiblingBssid("3c:52:a1:7e:10:c5", "3c:52:a1:7f:10:c5"))
        // Last octet too far apart
        assertFalse(NetworkGroups.isSiblingBssid("3c:52:a1:7e:10:c5", "3c:52:a1:7e:10:05"))
    }

    @Test
    fun notSiblings_identicalOrMalformed() {
        assertFalse(NetworkGroups.isSiblingBssid("3c:52:a1:7e:10:c5", "3C:52:A1:7E:10:C5"))
        assertFalse(NetworkGroups.isSiblingBssid("3c:52:a1:7e:10:c5", "not a mac"))
        assertFalse(NetworkGroups.isSiblingBssid("", ""))
    }

    @Test
    fun group_keepsHiddenNetworksSeparate() {
        val other = TestMesh.hiddenNeighbor.copy(bssid = "12:34:56:78:9a:bc")
        val groups = NetworkGroups.group(listOf(TestMesh.hiddenNeighbor, other))

        assertEquals(setOf(TestMesh.hiddenNeighbor.bssid), groups[TestMesh.hiddenNeighbor.bssid])
    }
}
