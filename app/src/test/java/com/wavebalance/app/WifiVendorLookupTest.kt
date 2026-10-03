package com.wavebalance.app

import com.wavebalance.app.model.WifiVendorLookup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiVendorLookupTest {

    @Test
    fun testKnownVendors() {
        assertEquals("Google", WifiVendorLookup.getVendor("70:3A:0E:12:34:56"))
        assertEquals("Apple", WifiVendorLookup.getVendor("AC:BC:32:AA:BB:CC"))
        assertEquals("Netgear", WifiVendorLookup.getVendor("C4:41:1E:00:11:22"))
        assertEquals("Ubiquiti UniFi", WifiVendorLookup.getVendor("A4:2B:B0:99:88:77"))
        assertEquals("TP-Link", WifiVendorLookup.getVendor("50:C7:BF:11:22:33"))
    }

    @Test
    fun testRandomizedMacAddresses() {
        // First byte has second least-significant bit set:
        // 0xDA = 1101 1010b -> bit 1 is 1 -> locally administered (randomized)
        assertTrue(WifiVendorLookup.isRandomizedMac("DA:A1:19:2B:3C:4D"))
        assertEquals("Private / Randomized MAC", WifiVendorLookup.getVendor("DA:A1:19:2B:3C:4D"))

        // 0x02 = 0000 0010b -> randomized
        assertTrue(WifiVendorLookup.isRandomizedMac("02:00:00:00:00:00"))

        // Standard globally administered:
        // 0x00 = 0000 0000b -> false
        assertFalse(WifiVendorLookup.isRandomizedMac("00:1A:11:22:33:44"))
    }

    @Test
    fun testBlankOrUnknownVendor() {
        assertEquals("Unknown OEM", WifiVendorLookup.getVendor(""))
        assertEquals("Unknown OEM", WifiVendorLookup.getVendor("00:00:00:00:00:00"))
        // Unrecognized globally unique OUI prefix
        assertEquals("Standard Wi-Fi OEM", WifiVendorLookup.getVendor("00:99:88:77:66:55"))
    }
}
