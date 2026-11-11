# Google Play Store Listing — NEXORA X (`com.nexora.x`)

---

## 1. App Title (Maximum 30 Characters)

```text
NEXORA X — sing-box VPN Client
```
*(Character count: 30 / 30)*

**Alternative Ultra-Concise Title (8 Characters):**
```text
NEXORA X
```

---

## 2. Short Description (Maximum 80 Characters)

```text
User-configured VLESS, VMess, Trojan & Shadowsocks VPN client on sing-box.
```
*(Character count: 74 / 80)*

---

## 3. Full Description (Maximum 4,000 Characters)

```text
NEXORA X is a clean, transparent, user-configured Android VPN and proxy client powered by the open-source sing-box (libbox v1.10.7) engine.

Designed for network engineers, developers, and privacy-conscious users who manage their own proxy infrastructure, NEXORA X lets you import, validate, diagnose, and connect to your own VLESS, VMess, Trojan, and Shadowsocks servers with zero ads, zero mandatory accounts, and 100% local profile storage.

IMPORTANT: BRING YOUR OWN CONFIGURATION
NEXORA X is a client utility only. It does NOT provide, bundle, or sell built-in VPN servers or subscriptions. To connect, you must import or configure your own compatible server profile (VLESS, VMess, Trojan, or Shadowsocks).

SUPPORTED PROTOCOLS & TRANSPORTS
• VLESS — Supports standard TLS, Reality (PublicKey & ShortId), XTLS (xtls-rprx-vision), and XUDP packet encoding.
• VMess — Supports AEAD security methods, TLS, WebSocket, gRPC, HTTP, and TCP transports.
• Trojan — Supports TLS authentication over TCP, WebSocket, and gRPC transports.
• Shadowsocks — Supports modern AEAD and Shadowsocks 2022 ciphers (including 2022-blake3-aes-128-gcm, 2022-blake3-aes-256-gcm, aes-256-gcm, and chacha20-ietf-poly1305).

REAL 6-STAGE TUNNEL CONNECTIVITY VERIFICATION
Unlike basic clients that display "Connected" as soon as a local interface opens, NEXORA X verifies every stage of your tunnel lifecycle in real time:
• Stage A — Android Foreground VpnService active.
• Stage B — Non-blocking local TUN interface (172.19.0.1/30, gVisor stack) established.
• Stage C — Embedded sing-box (libbox v1.10.7) runtime started.
• Stage D — Protected TCP handshake to your remote outbound server verified.
• Stage E — Tunnel DNS resolution verified.
• Stage F — End-to-end HTTP 204 connectivity probe verified, distinguishing between "VPN ACTIVE", "INTERNET AVAILABLE", and "NO INTERNET".

CORE FEATURES
• Clipboard & Multi-Line Batch URI Import: Import vless://, vmess://, trojan://, and ss:// links individually or in bulk, or create and edit profiles manually.
• Live TCP Handshake Latency Monitor: Periodically measures real socket connect RTT (ms) to your selected server endpoint without fabricating ping values.
• Credential-Redacted Diagnostics: Inspect generated sing-box JSON configurations and engine ring-buffer logs with automatic scrubbing of UUIDs, passwords, and Reality keys (LogRedactor).
• Persistent Local Storage: All profiles are stored locally on your device using Android Room SQLite and Jetpack DataStore, with cloud auto-backup explicitly disabled to protect your credentials.
• Free & Ad-Free: No subscriptions, no in-app purchases, no tracking SDKs, and no telemetry.

HOW ANDROID VPNSERVICE IS USED
NEXORA X uses Android's VpnService permission exclusively to create a local virtual network interface (TUN) when you explicitly tap Connect, routing device traffic through your selected server configuration. You can disconnect at any time from the Home dashboard or the persistent foreground notification.
```
*(Character count: ~2,960 / 4,000)*

---

## 4. Feature Bullet List (Quick Reference)

- **Native `sing-box` (`libbox v1.10.7`) Engine** supporting `arm64-v8a`, `armeabi-v7a`, `x86_64`, and `x86`.
- **Multi-Protocol Support:** `VLESS` (Reality / XTLS / TLS), `VMess`, `Trojan`, and `Shadowsocks` (AEAD / 2022).
- **6-Stage Tunnel Verification (Stages A–F):** Separates local TUN activation (`VPN ACTIVE`) from verified end-to-end internet reachability (`INTERNET AVAILABLE` vs. `NO INTERNET`).
- **Real TCP Latency Checker:** Periodic and on-demand socket handshake latency checks (`ms`, `Checking...`, `Timeout`, `N/A`).
- **Redacted Diagnostics Workbench:** View `sing-box` JSON schemas and runtime event logs with automatic credential scrubbing.
- **100% Local & Private:** Zero ads, zero analytics, zero accounts, and cloud backup disabled for credential databases.

---

## 5. "What's New" Release Notes Template (`v1.0.0` — Version Code `4`)

```text
NEXORA X v1.0.0 — Initial Google Play Production Release:
• Integrated native sing-box (libbox v1.10.7) VPN core supporting VLESS (Reality/XTLS), VMess, Trojan, and Shadowsocks.
• Added 6-Stage Tunnel Connectivity Verifier (Stages A–F) to verify real outbound handshakes, DNS resolution, and HTTP 204 internet reachability.
• Added periodic and on-demand TCP handshake latency checker on the Home dashboard.
• Added in-app VpnService Prominent Disclosure and credential-redacted diagnostics.
• Hardened local Room SQLite storage to exclude proxy credentials from cloud auto-backup.
```

---

## 6. Store Category & Support Info Checklist

| Google Play Console Field | Recommended Value |
| :--- | :--- |
| **App or Game** | App |
| **Category** | Tools |
| **Tags** | Network Tools, Proxy / VPN Client, Utilities |
| **Free or Paid** | Free |
| **Contains Ads** | No (`No, my app does not contain ads`) |
| **In-App Purchases** | No |
| **Target Audience** | 18 and over (or 16+ general utility; not directed at children under 13) |
| **Developer Contact Email** | `[CONTACT_EMAIL]` *(Required by Google Play)* |
| **Privacy Policy URL** | `[PRIVACY_POLICY_URL]` *(Required; host `privacy-policy.html` on HTTPS)* |
| **Developer Website** | `[DEVELOPER_WEBSITE_OR_GITHUB_REPO_URL]` *(Optional but recommended)* |
