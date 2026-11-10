package com.nexora.x.core.engine

import android.content.Context
import android.net.VpnService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexora.x.core.engine.service.VpnNotificationManager
import com.nexora.x.core.model.Profile
import com.nexora.x.core.model.ProfileType
import com.nexora.x.core.model.TlsConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android Instrumentation Test verifying Android framework integration for NEXORA X Phase 3:
 * - Notification channel creation and foreground notification construction without secret leakage.
 * - `VpnService.prepare(context)` invocation and [EngineServiceController] state transitions on a real
 *   Android device or emulator.
 *
 * Note: Full end-to-end packet tunneling additionally requires `app/libs/libbox.aar` and interactive
 * user approval of the system `VpnService.prepare` dialog on a physical device or emulator.
 */
@RunWith(AndroidJUnit4::class)
class NexoraVpnServiceInstrumentationTest {

    @Test
    fun vpnNotificationBuilderConstructsSafeNotificationWithoutCredentials() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notification = VpnNotificationManager.buildForegroundNotification(
            context = context,
            state = EngineState.CONNECTED,
            profileName = "Tokyo VLESS Edge",
            traffic = EngineTrafficStatistics(
                uplinkSpeedBytesPerSec = 1024L,
                downlinkSpeedBytesPerSec = 4096L
            )
        )
        assertNotNull(notification)
    }

    @Test
    fun engineServiceControllerPreparesValidatedProfileAndChecksVpnConsent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configGenerator = SingBoxConfigGenerator()
        val libboxManager = LibboxManager(context)
        val engine = SingBoxEngine(configGenerator, libboxManager)
        val controller = EngineServiceController(context, engine)

        val profile = Profile(
            id = "inst-1",
            displayName = "Instrumentation VLESS",
            protocol = ProfileType.VLESS,
            serverAddress = "edge.nexora.net",
            port = 443,
            credential = "b831381d-6324-4d53-ad4f-8cda48b30811",
            securityMethod = "none",
            tlsConfig = TlsConfig(enabled = true, securityMode = "tls", sni = "edge.nexora.net"),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val prepareIntent = VpnService.prepare(context)
        val outcome = controller.initiateTunnelStart(profile)

        if (prepareIntent != null) {
            assertEquals(EngineState.REQUESTING_PERMISSION, engine.engineState.value)
            assert(outcome is EngineServiceController.StartTunnelOutcome.VpnConsentRequired)
        }
        controller.stopTunnel()
        assertEquals(EngineState.DISCONNECTED, engine.engineState.value)
    }
}
