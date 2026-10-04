package com.wavebalance.app

import com.wavebalance.app.data.MLabNdt7Server
import com.wavebalance.app.model.SpeedTestException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MLabLocateTest {

    // Shaped like a real locate v2 answer; tokens shortened
    private val response = """
        {"results":[
          {"machine":"mlab1-lga03.mlab-oti.measurement-lab.org",
           "location":{"city":"New York","country":"US"},
           "urls":{
             "ws:///ndt/v7/download":"ws://ndt-mlab1-lga03.mlab-oti.measurement-lab.org/ndt/v7/download?access_token=a",
             "ws:///ndt/v7/upload":"ws://ndt-mlab1-lga03.mlab-oti.measurement-lab.org/ndt/v7/upload?access_token=b",
             "wss:///ndt/v7/download":"wss://ndt-mlab1-lga03.mlab-oti.measurement-lab.org/ndt/v7/download?access_token=c",
             "wss:///ndt/v7/upload":"wss://ndt-mlab1-lga03.mlab-oti.measurement-lab.org/ndt/v7/upload?access_token=d"}},
          {"machine":"mlab2-iad05.mlab-oti.measurement-lab.org",
           "location":{"city":"Washington","country":"US"},
           "urls":{
             "wss:///ndt/v7/download":"wss://ndt-mlab2-iad05.mlab-oti.measurement-lab.org/ndt/v7/download?access_token=e",
             "wss:///ndt/v7/upload":"wss://ndt-mlab2-iad05.mlab-oti.measurement-lab.org/ndt/v7/upload?access_token=f"}}
        ]}
    """.trimIndent()

    @Test
    fun readsEveryServerWithSecureUrls() {
        val servers = MLabNdt7Server.parseLocateResponse(response)

        assertEquals(listOf("New York", "Washington"), servers.map { it.city })
        val server = servers.first()

        assertEquals("ndt-mlab1-lga03.mlab-oti.measurement-lab.org", server.host)
        assertEquals("New York", server.city)
        assertEquals("wss://ndt-mlab1-lga03.mlab-oti.measurement-lab.org/ndt/v7/download?access_token=c", server.downloadUrl)
        assertEquals("wss://ndt-mlab1-lga03.mlab-oti.measurement-lab.org/ndt/v7/upload?access_token=d", server.uploadUrl)
    }

    @Test
    fun skipsResultsWithoutSecureUrls() {
        val insecureOnly = """{"results":[{"location":{},"urls":{"ws:///ndt/v7/download":"ws://a/ndt/v7/download","ws:///ndt/v7/upload":"ws://a/ndt/v7/upload"}},
            {"urls":{"wss:///ndt/v7/download":"wss://b.example/ndt/v7/download","wss:///ndt/v7/upload":"wss://b.example/ndt/v7/upload"}}]}"""

        val servers = MLabNdt7Server.parseLocateResponse(insecureOnly)

        assertEquals(1, servers.size)
        assertEquals("b.example", servers[0].host)
        assertNull(servers[0].city)
    }

    @Test(expected = SpeedTestException::class)
    fun noResults_isATestError() {
        MLabNdt7Server.parseLocateResponse("""{"results":[]}""")
    }

    @Test(expected = SpeedTestException::class)
    fun unreadableAnswer_isATestError() {
        MLabNdt7Server.parseLocateResponse("<html>busy</html>")
    }
}
