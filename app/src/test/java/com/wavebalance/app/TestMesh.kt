package com.wavebalance.app

import com.wavebalance.app.model.AccessPoint
import com.wavebalance.app.model.ChannelWidth
import com.wavebalance.app.model.FrequencyBand

/**
 * A three-node home mesh as Android scans it, with addresses following the patterns
 * real mesh systems use: a guest SSID on each radio (locally administered BSSID, octets
 * 1 and 4 changed), a 2.4 GHz radio one address below the 5 GHz one, and nodes that
 * only broadcast locally administered addresses. Plus neighbours, one of them with the
 * same router vendor (same OUI).
 */
object TestMesh {

    private fun ap(bssid: String, ssid: String, frequencyMhz: Int, rssi: Int, connected: Boolean = false): AccessPoint {
        val band = FrequencyBand.fromFrequency(frequencyMhz)
        return AccessPoint(
            bssid = bssid,
            ssid = ssid,
            rssi = rssi,
            frequencyMhz = frequencyMhz,
            channel = FrequencyBand.frequencyToChannel(frequencyMhz),
            band = band,
            channelWidth = if (band == FrequencyBand.BAND_2_4_GHZ) ChannelWidth.WIDTH_20 else ChannelWidth.WIDTH_40,
            capabilities = "[WPA2-PSK-CCMP-128][RSN-PSK-CCMP-128][ESS]",
            isConnected = connected
        )
    }

    // Node A: the connected node, on 5 GHz channel 149
    val connected = ap("ae:61:20:44:2b:51", "Home_5G", 5745, -58, connected = true)
    val connectedGuest = ap("b2:61:20:44:2b:51", "Home_Guest", 5745, -58)
    val node2g = ap("b2:61:20:44:2b:50", "Home", 2462, -54)

    // Node B: vendor-registered base address, also on channel 149, far away
    val farNode = ap("3c:52:a1:7e:10:c5", "Home_5G", 5745, -86)
    val farNodeGuest = ap("3e:52:a1:5e:10:c5", "Home_Guest", 5745, -85)
    val farNode2g = ap("3c:52:a1:7e:10:c4", "Home", 2412, -72)

    // Node C: on channel 36
    val nodeC = ap("ae:61:20:44:29:9d", "Home_5G", 5180, -77)
    val nodeCGuest = ap("b2:61:20:44:29:9d", "Home_Guest", 5180, -77)

    val home = listOf(connected, connectedGuest, node2g, farNode, farNodeGuest, farNode2g, nodeC, nodeCGuest)

    // Same OUI as node B (a neighbour with the same router brand), channel 149
    val sameVendorNeighbor = ap("3c:52:a1:12:34:56", "Neighbor", 5745, -80)
    val hiddenNeighbor = ap("86:2d:4c:42:4d:2b", "", 2462, -89)
    val printer = ap("66:c6:d2:ba:9b:58", "DIRECT-H6-Printer", 2412, -75)

    val neighbors = listOf(sameVendorNeighbor, hiddenNeighbor, printer)

    val all = home + neighbors
}
