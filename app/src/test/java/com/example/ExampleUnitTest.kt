package com.example

import com.example.data.model.RouterConfig
import com.example.data.network.SimulatedRouterEngine
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `formatBps formats kbps mbps and gbps`() {
        assertEquals("500 bps", Formatters.formatBps(500L))
        assertEquals("250.0 Kbps", Formatters.formatBps(250_000L))
        assertEquals("45.50 Mbps", Formatters.formatBps(45_500_000L))
        assertEquals("1.25 Gbps", Formatters.formatBps(1_250_000_000L))
    }

    @Test
    fun `simulated router engine transitions offline and cpu spike`() {
        val engine = SimulatedRouterEngine()
        val router = RouterConfig(
            id = 1L,
            name = "Core-Lab",
            host = "10.10.0.1",
            isSimulated = true
        )

        val onlineTelemetry = engine.fetchTelemetry(router)
        assertTrue(onlineTelemetry.isOnline)
        assertTrue(onlineTelemetry.activePppoeCount > 0)

        engine.toggleForcedCpuSpike(1L)
        val spikeTelemetry = engine.fetchTelemetry(router)
        assertTrue(spikeTelemetry.cpuLoadPercent >= 90)

        engine.toggleForcedOffline(1L)
        val downTelemetry = engine.fetchTelemetry(router)
        assertFalse(downTelemetry.isOnline)
    }
}
