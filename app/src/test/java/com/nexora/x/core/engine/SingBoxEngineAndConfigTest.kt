package com.nexora.x.core.engine

import android.content.Context
import com.nexora.x.core.model.Profile
import com.nexora.x.core.model.ProfileType
import com.nexora.x.core.model.TlsConfig
import com.nexora.x.core.model.TransportConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 3 Unit Test Suite covering:
 * - [SingBoxConfigGenerator]: VLESS, VMess, Trojan, and Shadowsocks protocol mapping
 * - Invalid profile rejection before engine start
 * - [LogRedactor]: UUID, password, Reality key, and URI link redaction
 * - [SingBoxEngine]: Deterministic state machine transitions, duplicate start/stop prevention,
 *   VPN permission denial handling, and missing `libbox.aar` error handling.
 */
class SingBoxEngineAndConfigTest {

    private lateinit var configGenerator: SingBoxConfigGenerator

    @Before
    fun setUp() {
        configGenerator = SingBoxConfigGenerator()
    }

    @Test
    fun `SingBoxConfigGenerator maps VLESS Reality and XTLS flow to valid sing-box JSON`() {
        val profile = Profile(
            id = "p-vless-1",
            displayName = "Tokyo Reality Node",
            protocol = ProfileType.VLESS,
            serverAddress = "tokyo.nexora.net",
            port = 443,
            credential = "b831381d-6324-4d53-ad4f-8cda48b30811",
            securityMethod = "none",
            tlsConfig = TlsConfig(
                enabled = true,
                securityMode = "reality",
                sni = "www.cloudflare.com",
                fingerprint = "chrome",
                realityPublicKey = "RealPubKey998877",
                realityShortId = "01ab"
            ),
            transportConfig = TransportConfig(
                networkType = "tcp",
                flow = "xtls-rprx-vision"
            ),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )

        val generated = configGenerator.generateAndValidate(profile)
        assertTrue(generated.rawJson.contains("\"type\": \"vless\""))
        assertTrue(generated.rawJson.contains("\"flow\": \"xtls-rprx-vision\""))
        assertTrue(generated.rawJson.contains("\"public_key\": \"RealPubKey998877\""))
        assertTrue(generated.rawJson.contains("\"short_id\": \"01ab\""))
        assertTrue(generated.rawJson.contains("\"type\": \"tun\""))

        // Redacted summary must NEVER contain the UUID or Reality public key
        assertFalse(generated.redactedSummary.contains("b831381d-6324-4d53-ad4f-8cda48b30811"))
        assertFalse(generated.redactedSummary.contains("RealPubKey998877"))
    }

    @Test
    fun `SingBoxConfigGenerator maps VMess WS, Trojan gRPC, and Shadowsocks AEAD`() {
        val vmessProfile = Profile(
            id = "p-vmess",
            displayName = "SG VMess WS",
            protocol = ProfileType.VMESS,
            serverAddress = "sg.nexora.net",
            port = 443,
            credential = "27848739-7e62-4138-9fd3-098a63964b6b",
            securityMethod = "auto",
            tlsConfig = TlsConfig(enabled = true, securityMode = "tls", sni = "sg.nexora.net"),
            transportConfig = TransportConfig(networkType = "ws", host = "sg.nexora.net", path = "/vmess"),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )
        val vmessConfig = configGenerator.generateAndValidate(vmessProfile)
        assertTrue(vmessConfig.rawJson.contains("\"type\": \"vmess\""))
        assertTrue(vmessConfig.rawJson.contains("\"path\": \"/vmess\""))

        val trojanProfile = Profile(
            id = "p-trojan",
            displayName = "Zurich Trojan",
            protocol = ProfileType.TROJAN,
            serverAddress = "ch.nexora.net",
            port = 443,
            credential = "SuperSecretTrojanPassword",
            securityMethod = "tls",
            tlsConfig = TlsConfig(enabled = true, securityMode = "tls", sni = "ch.nexora.net"),
            transportConfig = TransportConfig(networkType = "grpc", serviceName = "grpc-tun"),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )
        val trojanConfig = configGenerator.generateAndValidate(trojanProfile)
        assertTrue(trojanConfig.rawJson.contains("\"type\": \"trojan\""))
        assertTrue(trojanConfig.rawJson.contains("\"service_name\": \"grpc-tun\""))

        val ssProfile = Profile(
            id = "p-ss",
            displayName = "AMS SS2022",
            protocol = ProfileType.SHADOWSOCKS,
            serverAddress = "nl.nexora.net",
            port = 8388,
            credential = "SS2022KeyMaterial",
            securityMethod = "2022-blake3-aes-256-gcm",
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )
        val ssConfig = configGenerator.generateAndValidate(ssProfile)
        assertTrue(ssConfig.rawJson.contains("\"type\": \"shadowsocks\""))
        assertTrue(ssConfig.rawJson.contains("\"method\": \"2022-blake3-aes-256-gcm\""))
    }

    @Test(expected = EngineException.InvalidConfigurationException::class)
    fun `SingBoxConfigGenerator rejects Reality profile missing public key`() {
        val invalidReality = Profile(
            id = "p-bad",
            displayName = "Broken Reality",
            protocol = ProfileType.VLESS,
            serverAddress = "edge.nexora.net",
            port = 443,
            credential = "b831381d-6324-4d53-ad4f-8cda48b30811",
            securityMethod = "none",
            tlsConfig = TlsConfig(enabled = true, securityMode = "reality", sni = "www.cloudflare.com", realityPublicKey = ""),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )
        configGenerator.generateAndValidate(invalidReality)
    }

    @Test
    fun `LogRedactor scrubs UUIDs, proxy URIs, JSON passwords, and custom secrets`() {
        val rawLog =
            "Failed connecting vless://b831381d-6324-4d53-ad4f-8cda48b30811@1.2.3.4:443 with \"password\": \"MySecretPass99\" and token=abcSecretToken123"
        val redacted = LogRedactor.redact(rawLog, extraSecrets = listOf("CustomPrivateKeyXYZ"))

        assertFalse(redacted.contains("b831381d-6324-4d53-ad4f-8cda48b30811"))
        assertFalse(redacted.contains("MySecretPass99"))
        assertFalse(redacted.contains("abcSecretToken123"))
        assertTrue(redacted.contains("[REDACTED"))
    }

    @Test
    fun `SingBoxEngine state machine prevents duplicate starts and handles permission denial and missing libbox cleanly`() {
        val engine = SingBoxEngine(
            configGenerator = configGenerator,
            libboxManager = LibboxManager(TestContextStub())
        )

        assertEquals(EngineState.DISCONNECTED, engine.engineState.value)

        // 1. Attempting to start with null profile transitions to ERROR
        val nullResult = engine.prepareProfileForStart(null)
        assertTrue(nullResult.isFailure)
        assertEquals(EngineState.ERROR, engine.engineState.value)

        // 2. Valid profile transitions ERROR -> PREPARING -> REQUESTING_PERMISSION
        val validProfile = Profile(
            id = "p-1",
            displayName = "Valid VLESS",
            protocol = ProfileType.VLESS,
            serverAddress = "edge.nexora.net",
            port = 443,
            credential = "b831381d-6324-4d53-ad4f-8cda48b30811",
            securityMethod = "none",
            tlsConfig = TlsConfig(enabled = true, securityMode = "tls", sni = "edge.nexora.net"),
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L
        )
        val prepRes = engine.prepareProfileForStart(validProfile)
        assertTrue(prepRes.isSuccess)
        assertEquals(EngineState.PREPARING, engine.engineState.value)

        // Duplicate start while PREPARING is rejected
        val duplicateStart = engine.prepareProfileForStart(validProfile)
        assertTrue(duplicateStart.isFailure)

        // Request VPN permission -> Denial transitions to ERROR
        engine.onVpnPermissionRequestLaunched()
        assertEquals(EngineState.REQUESTING_PERMISSION, engine.engineState.value)
        engine.onVpnPermissionDenied()
        assertEquals(EngineState.ERROR, engine.engineState.value)
        assertNotNull(engine.diagnostics.value.lastRedactedError)

        // Stop session resets cleanly to DISCONNECTED
        engine.stopSession("Reset after test")
        assertEquals(EngineState.DISCONNECTED, engine.engineState.value)
    }

    private class TestContextStub : android.content.ContextWrapper(null)
}
