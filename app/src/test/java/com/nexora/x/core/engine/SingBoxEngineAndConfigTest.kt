package com.nexora.x.core.engine

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
 * Phase 3 & Phase 4 Unit Test Suite covering:
 * - [SingBoxConfigGenerator]: VLESS, VMess, Trojan, and Shadowsocks protocol mapping,
 *   DNS bootstrap rule (`"outbound": "any", "server": "dns-direct"`), `"address_resolver": "dns-direct"`,
 *   `"stack": "gvisor"`, and `"packet_encoding": "xudp"`.
 * - Invalid profile rejection before engine start.
 * - [LogRedactor]: UUID, password, Reality key, and URI link redaction.
 * - [SingBoxEngine]: Deterministic state machine transitions (`DISCONNECTED`, `PREPARING`,
 *   `REQUESTING_PERMISSION`, `STARTING`, `VPN_ACTIVE`, `CONNECTIVITY_CHECK`, `INTERNET_AVAILABLE`,
 *   `NO_INTERNET`, `STOPPING`, `ERROR`), duplicate start/stop prevention, andStage B tracking.
 */
class SingBoxEngineAndConfigTest {

    private lateinit var configGenerator: SingBoxConfigGenerator

    @Before
    fun setUp() {
        configGenerator = SingBoxConfigGenerator()
    }

    @Test
    fun `SingBoxConfigGenerator maps VLESS Reality and XTLS flow with Phase 4 DNS bootstrap and gvisor stack`() {
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
        assertTrue(generated.rawJson.contains("\"packet_encoding\": \"xudp\""))
        assertTrue(generated.rawJson.contains("\"public_key\": \"RealPubKey998877\""))
        assertTrue(generated.rawJson.contains("\"short_id\": \"01ab\""))
        assertTrue(generated.rawJson.contains("\"type\": \"tun\""))

        // Phase 4 connectivity invariants: gvisor stack + DNS bootstrap rule + direct DNS resolver
        assertTrue(generated.rawJson.contains("\"stack\": \"gvisor\""))
        assertTrue(generated.rawJson.contains("\"address_resolver\": \"dns-direct\""))
        assertTrue(generated.rawJson.contains("\"outbound\": \"any\""))
        assertTrue(generated.rawJson.contains("\"server\": \"dns-direct\""))
        assertTrue(generated.rawJson.contains("\"ip_is_private\": true"))

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
        assertTrue(vmessConfig.rawJson.contains("\"packet_encoding\": \"xudp\""))

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
    fun `SingBoxEngine state machine prevents duplicate starts and tracks Phase 4 Stage B and permission denial`() {
        val engine = SingBoxEngine(
            configGenerator = configGenerator,
            libboxManager = LibboxManager(TestContextStub())
        )

        assertEquals(EngineState.DISCONNECTED, engine.engineState.value)
        assertFalse(engine.engineState.value.isTunnelRunning)

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

        // Stage B tracking records TUN file descriptor and CIDR
        engine.onTunInterfaceOpened(fd = 42, mtu = 1500, ipv4Address = "172.19.0.1", ipv4Prefix = 30)
        assertTrue(engine.diagnostics.value.stageDiagnostics.stageBTunInterfaceEstablished)
        assertEquals("172.19.0.1/30", engine.diagnostics.value.stageDiagnostics.tunIpv4Cidr)

        // Request VPN permission -> Denial transitions to ERROR
        engine.onVpnPermissionRequestLaunched()
        assertEquals(EngineState.REQUESTING_PERMISSION, engine.engineState.value)
        engine.onVpnPermissionDenied()
        assertEquals(EngineState.ERROR, engine.engineState.value)
        assertNotNull(engine.diagnostics.value.lastRedactedError)

        // Stop session resets cleanly to DISCONNECTED
        engine.stopSession("Reset after test")
        assertEquals(EngineState.DISCONNECTED, engine.engineState.value)
        assertFalse(engine.diagnostics.value.stageDiagnostics.stageBTunInterfaceEstablished)
    }

    @Test
    fun `Phase 4 EngineState tunnel running flags cover VPN_ACTIVE, CONNECTIVITY_CHECK, INTERNET_AVAILABLE, and NO_INTERNET`() {
        assertTrue(EngineState.VPN_ACTIVE.isTunnelRunning)
        assertTrue(EngineState.CONNECTIVITY_CHECK.isTunnelRunning)
        assertTrue(EngineState.INTERNET_AVAILABLE.isTunnelRunning)
        assertTrue(EngineState.CONNECTED.isTunnelRunning)
        assertTrue(EngineState.NO_INTERNET.isTunnelRunning)
        assertFalse(EngineState.DISCONNECTED.isTunnelRunning)
        assertFalse(EngineState.STARTING.isTunnelRunning)
        assertFalse(EngineState.ERROR.isTunnelRunning)
    }

    @Test
    fun `Libbox v1_10_7 classes from app_libs_libbox_aar are present on classpath`() {
        val libboxManager = LibboxManager(TestContextStub())
        assertTrue(
            "Expected io.nekohasekai.libbox.Libbox from app/libs/libbox.aar (v1.10.7) to be present on classpath",
            libboxManager.isLibboxClassPresent()
        )

        val cl = LibboxManager::class.java.classLoader
        assertNotNull(Class.forName("io.nekohasekai.libbox.Libbox", false, cl))
        assertNotNull(Class.forName("io.nekohasekai.libbox.PlatformInterface", false, cl))
        assertNotNull(Class.forName("io.nekohasekai.libbox.SetupOptions", false, cl))
        assertNotNull(Class.forName("io.nekohasekai.libbox.BoxService", false, cl))
        assertNotNull(Class.forName("io.nekohasekai.libbox.CommandServer", false, cl))
        assertNotNull(Class.forName("go.Seq", false, cl))
    }

    @Test
    fun `Production v1_0_0 release metadata and pinned sing-box version are aligned`() {
        assertEquals("NEXORA X", com.nexora.x.core.common.AppConstants.APP_NAME)
        assertEquals("1.0.0", com.nexora.x.core.common.AppConstants.VERSION_NAME)
        assertEquals(4, com.nexora.x.core.common.AppConstants.VERSION_CODE)
        assertEquals("1.10.7", LibboxManager.PINNED_SINGBOX_VERSION)
    }

    private class TestContextStub : android.content.ContextWrapper(null)
}
