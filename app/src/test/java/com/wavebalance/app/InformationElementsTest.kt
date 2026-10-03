package com.wavebalance.app

import com.wavebalance.app.model.AccessPoint
import com.wavebalance.app.model.InformationElements
import com.wavebalance.app.model.PmfMode
import com.wavebalance.app.model.RawInformationElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InformationElementsTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    private fun ie(id: Int, vararg values: Int, idExt: Int = 0) = RawInformationElement(id, idExt, bytes(*values))

    // RSN: version 1, CCMP group, 1 CCMP pairwise suite, 1 PSK AKM suite, then the capabilities
    private fun rsn(vararg capabilities: Int) = ie(
        48,
        0x01, 0x00,
        0x00, 0x0F, 0xAC, 0x04,
        0x01, 0x00, 0x00, 0x0F, 0xAC, 0x04,
        0x01, 0x00, 0x00, 0x0F, 0xAC, 0x02,
        *capabilities
    )

    @Test
    fun spatialStreams_takesHighestAcrossHtVhtAndHe() {
        // HT: Rx MCS bitmask with streams 1-2
        val ht = ie(45, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00)
        // VHT: Rx map with streams 1-3 supported (0b10), the rest 0b11
        val vht = ie(191, 0x00, 0x00, 0x00, 0x00, 0xEA, 0xFF, 0x00, 0x00)
        // HE: 6 MAC + 11 PHY capability bytes, then Rx map with streams 1-2
        val he = RawInformationElement(255, 35, ByteArray(17) + bytes(0xFA, 0xFF, 0xFA, 0xFF))

        assertEquals(2, InformationElements.parse(listOf(ht)).maxSpatialStreams)
        assertEquals(3, InformationElements.parse(listOf(ht, vht)).maxSpatialStreams)
        assertEquals(3, InformationElements.parse(listOf(ht, vht, he)).maxSpatialStreams)
        assertEquals(2, InformationElements.parse(listOf(he)).maxSpatialStreams)
    }

    @Test
    fun spatialStreams_nullWhenNotAdvertisedOrTruncated() {
        assertNull(InformationElements.parse(emptyList()).maxSpatialStreams)
        assertNull(InformationElements.parse(listOf(ie(45, 0x00, 0x00))).maxSpatialStreams)
    }

    @Test
    fun pmf_readFromRsnCapabilities() {
        assertEquals(PmfMode.CAPABLE, InformationElements.parse(listOf(rsn(0x80, 0x00))).pmf)
        assertEquals(PmfMode.REQUIRED, InformationElements.parse(listOf(rsn(0xC0, 0x00))).pmf)
        assertEquals(PmfMode.DISABLED, InformationElements.parse(listOf(rsn(0x00, 0x00))).pmf)
        // Capabilities field omitted means no PMF
        assertEquals(PmfMode.DISABLED, InformationElements.parse(listOf(rsn())).pmf)
        assertNull(InformationElements.parse(emptyList()).pmf)
    }

    @Test
    fun roamingSupport_detectsKvr() {
        val none = InformationElements.parse(listOf(ie(127, 0x00, 0x00, 0x00)))
        assertFalse(none.radioMeasurement)
        assertFalse(none.bssTransition)
        assertFalse(none.fastTransition)

        val all = InformationElements.parse(
            listOf(
                ie(70, 0x00, 0x00, 0x00, 0x00, 0x00),
                // Extended Capabilities bit 19 = byte 2, bit 3
                ie(127, 0x00, 0x00, 0x08),
                ie(54, 0x12, 0x34, 0x00)
            )
        )
        assertTrue(all.radioMeasurement)
        assertTrue(all.bssTransition)
        assertTrue(all.fastTransition)
    }

    @Test
    fun securityLabels_matchAndroidCapabilityStrings() {
        assertEquals("WPA2 Personal", AccessPoint.parseSecurity("[WPA2-PSK-CCMP-128][RSN-PSK-CCMP-128][ESS]"))
        assertEquals("WPA2/WPA3 Personal", AccessPoint.parseSecurity("[RSN-PSK+SAE-CCMP][ESS]"))
        assertEquals("WPA3 Personal", AccessPoint.parseSecurity("[RSN-SAE-CCMP][ESS]"))
        assertEquals("WPA/WPA2 Personal", AccessPoint.parseSecurity("[WPA-PSK-CCMP][WPA2-PSK-CCMP][ESS]"))
        assertEquals("WPA2 Enterprise", AccessPoint.parseSecurity("[WPA2-EAP-CCMP][RSN-EAP-CCMP][ESS]"))
        assertEquals("OWE (Enhanced Open)", AccessPoint.parseSecurity("[RSN-OWE-CCMP][ESS]"))
        assertEquals("Open", AccessPoint.parseSecurity("[ESS]"))
    }
}
