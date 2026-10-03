package com.wavebalance.app.model

import java.io.IOException
import java.io.InputStream

/**
 * Names the vendor of a BSSID from the IEEE OUI registry, which ships as
 * assets/oui_registry.tsv (regenerate it with tools/generate_oui_registry.py).
 */
object WifiVendorLookup {

    const val REGISTRY_ASSET = "oui_registry.tsv"

    @Volatile
    private var openRegistry: (() -> InputStream)? = null

    // Keyed by the first three octets as a 24-bit number; null until read
    @Volatile
    private var registry: Map<Int, String>? = null

    /**
     * Sets where the registry is read from. It is read on the first lookup
     * or on [preload]; lookups before this name no vendor.
     */
    fun useRegistry(open: () -> InputStream) {
        synchronized(this) {
            openRegistry = open
            registry = null
        }
    }

    /** Reads the registry now, so the first lookup on the UI thread doesn't have to. */
    fun preload() {
        registry()
    }

    private fun registry(): Map<Int, String> {
        registry?.let { return it }
        synchronized(this) {
            registry?.let { return it }
            val open = openRegistry ?: return emptyMap()
            // Vendor names are a nicety: if the registry can't be read, name no vendors
            // rather than fail, and don't retry on every lookup
            val parsed = try {
                open().use(::parseRegistry)
            } catch (e: IOException) {
                emptyMap()
            }
            return parsed.also { registry = it }
        }
    }

    internal fun parseRegistry(input: InputStream): Map<Int, String> {
        val map = HashMap<Int, String>(48_000)
        input.bufferedReader().forEachLine { line ->
            val tab = line.indexOf('\t')
            if (tab == 6) {
                line.substring(0, 6).toIntOrNull(16)?.let { map[it] = line.substring(7) }
            }
        }
        return map
    }

    // Locally administered addresses aren't registered to a vendor. Routers use them for
    // their extra virtual networks (guest SSIDs, mesh backhaul); phones use them for MAC randomization.
    const val LOCALLY_ADMINISTERED = "Locally administered (virtual AP)"

    fun getVendor(bssid: String): String {
        if (bssid.isBlank() || bssid == "00:00:00:00:00:00") return "Unknown vendor"

        val parts = bssid.trim().split(":")
        if (parts.size >= 3) {
            val oui = (parts[0] + parts[1] + parts[2]).toIntOrNull(16)
            if (oui != null) {
                // The locally administered bit (0x02 of the first octet) means the address
                // isn't registered, even if the remaining bits happen to match an OUI
                if ((oui shr 16) and 0x02 != 0) return LOCALLY_ADMINISTERED
                registry()[oui]?.let { return it }
            }
        }

        return "Unknown vendor"
    }

    fun isRandomizedMac(bssid: String): Boolean {
        val parts = bssid.trim().split(":")
        if (parts.isNotEmpty()) {
            try {
                val firstByte = parts[0].toInt(16)
                return (firstByte and 0x02) != 0
            } catch (e: Exception) {
                return false
            }
        }
        return false
    }
}
