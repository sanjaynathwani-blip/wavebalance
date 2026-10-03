package com.wavebalance.app.model

object WifiVendorLookup {

    private val ouiPrefixMap = mapOf(
        // Google / Nest
        "70:3A:0E" to "Google",
        "D4:F5:47" to "Google Nest",
        "E4:F0:42" to "Google",
        "54:60:09" to "Google",
        "94:DB:56" to "Google",
        "F4:F5:D8" to "Google",
        "8C:C8:CD" to "Google Nest",
        "30:FD:38" to "Google",

        // Apple
        "AC:BC:32" to "Apple",
        "00:1E:58" to "Apple",
        "F0:18:98" to "Apple",
        "38:CA:DA" to "Apple",
        "A4:83:E7" to "Apple",
        "BC:92:6B" to "Apple",
        "DC:2B:61" to "Apple",
        "F4:37:B7" to "Apple",

        // Netgear
        "C4:41:1E" to "Netgear",
        "20:E5:2A" to "Netgear",
        "00:26:F2" to "Netgear Nighthawk",
        "9C:3D:CF" to "Netgear Orbi",
        "B0:39:56" to "Netgear",
        "E0:46:9A" to "Netgear",

        // Cisco / Meraki
        "00:1A:11" to "Cisco Systems",
        "F0:9F:C2" to "Cisco Meraki",
        "0C:8D:DB" to "Cisco Meraki",
        "18:64:72" to "Cisco Systems",
        "88:F0:31" to "Cisco Meraki",

        // Ubiquiti Networks (UniFi)
        "A4:2B:B0" to "Ubiquiti UniFi",
        "E0:63:DA" to "Ubiquiti Networks",
        "80:2A:A8" to "Ubiquiti UniFi",
        "78:45:58" to "Ubiquiti Networks",
        "B4:FB:E4" to "Ubiquiti UniFi",
        "F4:92:BF" to "Ubiquiti Networks",

        // TP-Link
        "50:C7:BF" to "TP-Link",
        "74:DA:38" to "TP-Link",
        "B0:BE:76" to "TP-Link Deco",
        "AC:84:C6" to "TP-Link",
        "00:31:92" to "TP-Link",
        "98:42:65" to "TP-Link Archer",

        // ASUSTeK
        "24:4B:FE" to "ASUS ROG/ZenWiFi",
        "58:D9:C3" to "ASUSTeK Computer",
        "04:D9:F5" to "ASUSTeK Computer",
        "AC:9E:17" to "ASUSTeK Computer",

        // Amazon / Eero
        "F4:39:09" to "eero (Amazon)",
        "44:65:0D" to "Amazon eero Pro",
        "CC:F4:11" to "eero (Amazon)",
        "50:DC:E7" to "Amazon eero",

        // Samsung
        "BC:D0:74" to "Samsung Electronics",
        "94:65:2D" to "Samsung",
        "34:82:C5" to "Samsung",
        "40:4E:36" to "Samsung SmartThings",

        // Broadcom / Qualcomm / Intel / Mediatek
        "00:10:18" to "Broadcom",
        "00:03:7F" to "Qualcomm Atheros",
        "00:15:00" to "Intel Corporation",
        "8C:FD:F0" to "Intel Wi-Fi",
        "00:0C:E7" to "MediaTek",

        // Linksys / Belkin
        "00:14:BF" to "Linksys",
        "C0:56:27" to "Linksys Velop",
        "14:91:82" to "Belkin",

        // Synology
        "00:11:32" to "Synology",

        // Raspberry Pi
        "B8:27:EB" to "Raspberry Pi",
        "DC:A6:32" to "Raspberry Pi",
        "E4:5F:01" to "Raspberry Pi"
    )

    fun getVendor(bssid: String): String {
        if (bssid.isBlank() || bssid == "00:00:00:00:00:00") return "Unknown OEM"

        val cleaned = bssid.trim().uppercase()
        // Extract first 3 octets (e.g. "C4:41:1E")
        val parts = cleaned.split(":")
        if (parts.size >= 3) {
            val prefix = "${parts[0]}:${parts[1]}:${parts[2]}"
            ouiPrefixMap[prefix]?.let { return it }

            // Check if it's locally administered (randomized MAC address)
            // The second least significant bit of the first byte is 1 for locally administered addresses
            try {
                val firstByte = parts[0].toInt(16)
                if ((firstByte and 0x02) != 0) {
                    return "Private / Randomized MAC"
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        return "Standard Wi-Fi OEM"
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
