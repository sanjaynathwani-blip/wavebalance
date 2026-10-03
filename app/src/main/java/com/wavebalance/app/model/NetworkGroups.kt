package com.wavebalance.app.model

import kotlin.math.abs

/**
 * Groups scanned access points into networks: all the radios one router or mesh
 * system broadcasts. A home mesh easily shows up as 15+ BSSIDs (each node has a
 * radio per band and a BSSID per SSID on it), and none of them are interference
 * the user can fix by changing channel, because the whole system moves together.
 *
 * Two APs are in the same network when they share an SSID, or when their BSSIDs
 * are siblings derived from the same radio's base address (see [isSiblingBssid]).
 */
object NetworkGroups {

    /**
     * Maps each AP's lowercase BSSID to the lowercase BSSIDs of every AP in its network,
     * itself included.
     */
    fun group(aps: List<AccessPoint>): Map<String, Set<String>> {
        val bssids = aps.map { it.bssid.lowercase() }.distinct()
        val parent = IntArray(bssids.size) { it }
        fun find(i: Int): Int {
            var root = i
            while (parent[root] != root) root = parent[root]
            parent[i] = root
            return root
        }
        fun union(a: Int, b: Int) {
            parent[find(a)] = find(b)
        }

        val index = bssids.withIndex().associate { (i, bssid) -> bssid to i }
        aps.filter { it.hasRealSsid() }
            .groupBy { it.ssid }
            .values
            .forEach { sameSsid ->
                val first = index.getValue(sameSsid.first().bssid.lowercase())
                sameSsid.forEach { union(first, index.getValue(it.bssid.lowercase())) }
            }
        for (i in bssids.indices) {
            for (j in i + 1 until bssids.size) {
                if (isSiblingBssid(bssids[i], bssids[j])) union(i, j)
            }
        }

        val members = bssids.indices.groupBy(::find).mapValues { (_, ids) -> ids.map { bssids[it] }.toSet() }
        return bssids.withIndex().associate { (i, bssid) -> bssid to members.getValue(find(i)) }
    }

    /**
     * Lowercase BSSIDs of the user's own network(s): the one they're connected to, plus
     * any containing an AP they've tagged as home. Uses [activeSsid] to find the network
     * when the connected BSSID isn't in the scan results.
     */
    fun ownNetwork(aps: List<AccessPoint>, activeBssid: String?, activeSsid: String?): Set<String> {
        val groups = group(aps)
        val anchors = aps.filter {
            it.isUserTaggedHome ||
                it.isConnected ||
                it.bssid.equals(activeBssid, ignoreCase = true) ||
                (!activeSsid.isNullOrBlank() && it.ssid == activeSsid)
        }
        return anchors.flatMap { groups[it.bssid.lowercase()].orEmpty() }.toSet()
    }

    /**
     * True when two BSSIDs look like addresses one router derived from the same base
     * MAC: a radio's guest/IoT SSIDs and its other bands. Routers set the locally
     * administered bit and change octets 1 and 4 for virtual SSIDs, and step the last
     * octet per band, e.g. 3c:52:a1:7e:10:c5 (5 GHz), 3c:52:a1:7e:10:c4 (2.4 GHz) and
     * 3e:52:a1:5e:10:c5 (guest).
     *
     * So octets 2, 3 and 5 must match and the last octet be within 8. Octet 4 may
     * differ only together with octet 1, which keeps two routers of the same vendor
     * (same OUI, different serials) apart.
     */
    fun isSiblingBssid(a: String, b: String): Boolean {
        val x = parseMac(a) ?: return false
        val y = parseMac(b) ?: return false
        if (x.contentEquals(y)) return false
        return x[1] == y[1] && x[2] == y[2] && x[4] == y[4] &&
            abs(x[5] - y[5]) <= 8 &&
            (x[3] == y[3] || x[0] != y[0])
    }

    private fun parseMac(mac: String): IntArray? {
        val parts = mac.trim().split(":")
        if (parts.size != 6) return null
        val octets = parts.map { it.toIntOrNull(16) ?: return null }
        return octets.toIntArray()
    }

    private fun AccessPoint.hasRealSsid(): Boolean = ssid.isNotBlank() && ssid != "<unknown ssid>"
}
