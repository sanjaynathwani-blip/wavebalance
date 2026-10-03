package com.wavebalance.app

import com.wavebalance.app.model.WifiVendorLookup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class WifiVendorLookupTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun loadRegistry() {
            // Unit tests run with the module directory as the working directory
            WifiVendorLookup.useRegistry { File("src/main/assets/${WifiVendorLookup.REGISTRY_ASSET}").inputStream() }
        }
    }

    @Test
    fun testKnownVendors() {
        assertEquals("Google", WifiVendorLookup.getVendor("F4:F5:D8:12:34:56"))
        assertEquals("Apple", WifiVendorLookup.getVendor("AC:BC:32:AA:BB:CC"))
        assertEquals("Netgear", WifiVendorLookup.getVendor("9C:3D:CF:00:11:22"))
        assertEquals("Ubiquiti", WifiVendorLookup.getVendor("E0:63:DA:99:88:77"))
        assertEquals("Cisco Meraki", WifiVendorLookup.getVendor("0C:8D:DB:01:02:03"))
        assertEquals("ASUS", WifiVendorLookup.getVendor("04:D9:F5:01:02:03"))
    }

    @Test
    fun testTpLinkPrefixesMissingFromTheOldTable() {
        assertEquals("TP-Link", WifiVendorLookup.getVendor("3c:84:6a:00:00:01"))
        assertEquals("TP-Link", WifiVendorLookup.getVendor("00:5f:67:00:00:01"))
        assertEquals("TP-Link", WifiVendorLookup.getVendor("40:3f:8c:00:00:01"))
    }

    @Test
    fun testPrefixesTheOldTableHadWrong() {
        // Were listed as Ubiquiti, Netgear and Google
        assertEquals("TP-Link", WifiVendorLookup.getVendor("A4:2B:B0:99:88:77"))
        assertEquals("Belkin", WifiVendorLookup.getVendor("C4:41:1E:00:11:22"))
        assertEquals("HPE", WifiVendorLookup.getVendor("70:3A:0E:12:34:56"))
    }

    @Test
    fun testRandomizedMacAddresses() {
        // First byte has second least-significant bit set:
        // 0xDA = 1101 1010b -> bit 1 is 1 -> locally administered (randomized)
        assertTrue(WifiVendorLookup.isRandomizedMac("DA:A1:19:2B:3C:4D"))
        assertEquals(WifiVendorLookup.LOCALLY_ADMINISTERED, WifiVendorLookup.getVendor("DA:A1:19:2B:3C:4D"))
        // A TP-Link guest SSID derived from 3C:84:6A by setting the bit isn't TP-Link's registration
        assertEquals(WifiVendorLookup.LOCALLY_ADMINISTERED, WifiVendorLookup.getVendor("3E:84:6A:12:34:56"))

        // 0x02 = 0000 0010b -> randomized
        assertTrue(WifiVendorLookup.isRandomizedMac("02:00:00:00:00:00"))

        // Standard globally administered:
        // 0x00 = 0000 0000b -> false
        assertFalse(WifiVendorLookup.isRandomizedMac("00:1A:11:22:33:44"))
    }

    @Test
    fun testBlankOrUnknownVendor() {
        assertEquals("Unknown vendor", WifiVendorLookup.getVendor(""))
        assertEquals("Unknown vendor", WifiVendorLookup.getVendor("00:00:00:00:00:00"))
        assertEquals("Unknown vendor", WifiVendorLookup.getVendor("not a mac"))
    }

    @Test
    fun testMissingRegistry_namesNoVendorInsteadOfCrashing() {
        try {
            WifiVendorLookup.useRegistry { throw java.io.FileNotFoundException("oui_registry.tsv") }
            assertEquals("Unknown vendor", WifiVendorLookup.getVendor("3c:84:6a:00:00:01"))
        } finally {
            loadRegistry()
        }
    }

    @Test
    fun testParseRegistry_skipsMalformedLines() {
        val parsed = WifiVendorLookup.parseRegistry("3C846A\tTP-Link\nbad line\nZZZZZZ\tNope\n005F67\tTP-Link\n".byteInputStream())

        assertEquals(mapOf(0x3C846A to "TP-Link", 0x005F67 to "TP-Link"), parsed)
    }
}
