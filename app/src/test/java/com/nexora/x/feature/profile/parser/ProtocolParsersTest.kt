package com.nexora.x.feature.profile.parser

import com.nexora.x.core.model.ParseResult
import com.nexora.x.core.model.ProfileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit test suite for:
 * - [VlessParser]
 * - [VmessParser]
 * - [TrojanParser]
 * - [ShadowsocksParser]
 * - [ProfileImportEngine]
 * - Invalid links, URL encoding, Base64 decoding, port validation, and credential redaction.
 */
class ProtocolParsersTest {

    private lateinit var vlessParser: VlessParser
    private lateinit var vmessParser: VmessParser
    private lateinit var trojanParser: TrojanParser
    private lateinit var shadowsocksParser: ShadowsocksParser
    private lateinit var importEngine: ProfileImportEngine

    @Before
    fun setUp() {
        vlessParser = VlessParser()
        vmessParser = VmessParser()
        trojanParser = TrojanParser()
        shadowsocksParser = ShadowsocksParser()
        importEngine = ProfileImportEngine(
            vlessParser = vlessParser,
            vmessParser = vmessParser,
            trojanParser = trojanParser,
            shadowsocksParser = shadowsocksParser
        )
    }

    @Test
    fun `vless parser extracts Reality and XTLS parameters and URL-decoded display name`() {
        val uri =
            "vless://b831381d-6324-4d53-ad4f-8cda48b30811@tokyo-edge.nexora.net:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=www.cloudflare.com&fp=chrome&pbk=pubkey12345&sid=01ab&type=tcp&headerType=none#Tokyo%20Reality%20%231"
        val result = vlessParser.parse(uri)
        assertTrue(result is ParseResult.Success)
        val profile = (result as ParseResult.Success).profile

        assertEquals(ProfileType.VLESS, profile.protocol)
        assertEquals("Tokyo Reality #1", profile.displayName)
        assertEquals("tokyo-edge.nexora.net", profile.serverAddress)
        assertEquals(443, profile.port)
        assertEquals("b831381d-6324-4d53-ad4f-8cda48b30811", profile.credential)
        assertEquals("reality", profile.tlsConfig.securityMode)
        assertEquals("www.cloudflare.com", profile.tlsConfig.sni)
        assertEquals("pubkey12345", profile.tlsConfig.realityPublicKey)
        assertEquals("01ab", profile.tlsConfig.realityShortId)
        assertEquals("xtls-rprx-vision", profile.transportConfig.flow)
        // Verify credential is redacted in toString()
        assertFalse(profile.toString().contains("b831381d-6324-4d53-ad4f-8cda48b30811"))
    }

    @Test
    fun `vless parser preserves warnings for unsupported parameters without discarding profile`() {
        val uri =
            "vless://b831381d-6324-4d53-ad4f-8cda48b30811@198.51.100.12:8443?type=ws&security=tls&sni=cdn.nexora.net&path=%2Fws-tunnel&customExperimentalFlag=enabled#Frankfurt%20WS"
        val result = vlessParser.parse(uri)
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        assertEquals("/ws-tunnel", success.profile.transportConfig.path)
        assertTrue(success.warnings.any { it.contains("customExperimentalFlag") })
    }

    @Test
    fun `vmess parser decodes Base64 JSON configuration and validates transport and TLS`() {
        val rawJson = """
            {
              "v": "2",
              "ps": "Singapore VMess WS",
              "add": "sg-ws.nexora.net",
              "port": "443",
              "id": "27848739-7e62-4138-9fd3-098a63964b6b",
              "aid": "0",
              "scy": "chacha20-poly1305",
              "net": "ws",
              "type": "none",
              "host": "sg-ws.nexora.net",
              "path": "/vmess-stream",
              "tls": "tls",
              "sni": "sg-ws.nexora.net",
              "alpn": "h2,http/1.1",
              "fp": "firefox"
            }
        """.trimIndent()
        val encoded = UriParserUtils.encodeBase64(rawJson)
        val result = vmessParser.parse("vmess://$encoded")

        assertTrue(result is ParseResult.Success)
        val profile = (result as ParseResult.Success).profile
        assertEquals(ProfileType.VMESS, profile.protocol)
        assertEquals("Singapore VMess WS", profile.displayName)
        assertEquals("sg-ws.nexora.net", profile.serverAddress)
        assertEquals(443, profile.port)
        assertEquals("ws", profile.transportConfig.networkType)
        assertEquals("/vmess-stream", profile.transportConfig.path)
        assertTrue(profile.tlsConfig.enabled)
        assertEquals("firefox", profile.tlsConfig.fingerprint)
    }

    @Test
    fun `trojan parser parses gRPC TLS link and URL-encoded password`() {
        val uri =
            "trojan://Secr3t%21Pass%402026@zurich.nexora.net:443?security=tls&type=grpc&serviceName=trojan-grpc-svc&sni=zurich.nexora.net&alpn=h2#Zurich%20Trojan%20Core"
        val result = trojanParser.parse(uri)

        assertTrue(result is ParseResult.Success)
        val profile = (result as ParseResult.Success).profile
        assertEquals(ProfileType.TROJAN, profile.protocol)
        assertEquals("Secr3t!Pass@2026", profile.credential)
        assertEquals("zurich.nexora.net", profile.serverAddress)
        assertEquals("grpc", profile.transportConfig.networkType)
        assertEquals("trojan-grpc-svc", profile.transportConfig.serviceName)
        assertEquals("Zurich Trojan Core", profile.displayName)
    }

    @Test
    fun `shadowsocks parser handles SIP002 Base64 userinfo and legacy full Base64 formats`() {
        // SIP002 format
        val userInfoB64 = UriParserUtils.encodeBase64("2022-blake3-aes-256-gcm:MySuperKey123", urlSafeNoPadding = true)
        val sip002Uri = "ss://$userInfoB64@amsterdam.nexora.net:8388#Amsterdam%20SS2022"
        val res1 = shadowsocksParser.parse(sip002Uri)
        assertTrue(res1 is ParseResult.Success)
        val p1 = (res1 as ParseResult.Success).profile
        assertEquals(ProfileType.SHADOWSOCKS, p1.protocol)
        assertEquals("2022-blake3-aes-256-gcm", p1.securityMethod)
        assertEquals("MySuperKey123", p1.credential)
        assertEquals("Amsterdam SS2022", p1.displayName)

        // Legacy full Base64 format
        val fullRaw = "aes-256-gcm:LegacyPass99@192.0.2.55:9001"
        val legacyUri = "ss://${UriParserUtils.encodeBase64(fullRaw)}#LegacyNode"
        val res2 = shadowsocksParser.parse(legacyUri)
        assertTrue(res2 is ParseResult.Success)
        val p2 = (res2 as ParseResult.Success).profile
        assertEquals("192.0.2.55", p2.serverAddress)
        assertEquals(9001, p2.port)
        assertEquals("LegacyPass99", p2.credential)
    }

    @Test
    fun `invalid links and out-of-range ports are rejected without leaking credentials`() {
        // Out of range port 70000
        val badPortUri = "vless://b831381d-6324-4d53-ad4f-8cda48b30811@example.com:70000?security=none#BadPort"
        val badPortRes = importEngine.parseSingleLink(badPortUri)
        assertTrue(badPortRes is ParseResult.Failure)
        val errMsg = (badPortRes as ParseResult.Failure).errorMessage
        assertTrue(errMsg.contains("65535"))
        assertFalse(errMsg.contains("b831381d-6324-4d53-ad4f-8cda48b30811"))

        // Unsupported scheme
        val badSchemeRes = importEngine.parseSingleLink("http://example.com:8080")
        assertTrue(badSchemeRes is ParseResult.Failure)

        // Malformed Base64 VMess
        val badVmess = importEngine.parseSingleLink("vmess://not-valid-base64-json!!!")
        assertTrue(badVmess is ParseResult.Failure)
    }
}
