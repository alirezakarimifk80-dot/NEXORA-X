# Privacy Policy for NEXORA X (`com.nexora.x`)

**Effective Date:** `[EFFECTIVE_DATE_YYYY_MM_DD]`  
**Last Updated:** `[LAST_UPDATED_DATE_YYYY_MM_DD]`  
**Application Name:** NEXORA X  
**Android Package Name:** `com.nexora.x`  
**Publisher / Developer Name:** `[PUBLISHER_NAME]`  
**Developer Contact Email:** `[CONTACT_EMAIL]`  
**Published Policy URL:** `[PRIVACY_POLICY_URL]`

> **PUBLISHER ACTION REQUIRED BEFORE GOOGLE PLAY SUBMISSION:**  
> Replace all bracketed placeholders (`[PUBLISHER_NAME]`, `[CONTACT_EMAIL]`, `[PRIVACY_POLICY_URL]`, `[EFFECTIVE_DATE_YYYY_MM_DD]`, and `[LAST_UPDATED_DATE_YYYY_MM_DD]`) with your verified Google Play Developer Account details before hosting this policy publicly and linking it in Google Play Console.

---

## 1. Overview & Product Scope

**NEXORA X** (`com.nexora.x`) is a free, open-architecture Android VPN and proxy client utility powered by the open-source `sing-box` (`libbox v1.10.7`) core engine.

- **Bring-Your-Own-Configuration Utility:** NEXORA X does **not** provide, sell, rent, or operate public or commercial VPN servers. Users import and manage their own server configurations (`VLESS`, `VMess`, `Trojan`, and `Shadowsocks`).
- **No Accounts or Subscriptions:** NEXORA X requires no user registration, no login, no in-app purchases, and contains no advertising SDKs.
- **No Developer Backend:** NEXORA X does not communicate with any backend server, analytics service, or telemetry endpoint operated by `[PUBLISHER_NAME]`.

---

## 2. How NEXORA X Uses Android `VpnService`

NEXORA X uses the Android system `android.net.VpnService` API solely to provide its core user-initiated VPN client functionality:

1. **Prominent In-App Disclosure & Explicit Consent:** Before requesting Android's system VPN permission (`VpnService.prepare()`), NEXORA X displays an in-app prominent disclosure explaining how `VpnService` operates and requires your explicit acceptance.
2. **Local Virtual Network Interface (`TUN`):** When you explicitly tap **Connect**, `NexoraVpnService` creates a local virtual network interface (`172.19.0.1/30`, `gVisor` user-space stack) on your Android device.
3. **Encrypted Tunneling to Your Configured Server:** Network packets captured by the local `TUN` interface are processed in memory by the embedded `sing-box` (`libbox v1.10.7`) engine and forwarded over an encrypted connection (using the TLS, Reality, XTLS, or AEAD cipher parameters defined in your selected profile) directly to the remote server address and port that **you** configured.
4. **User Control & Foreground Visibility:** While a VPN session is active, NEXORA X displays a persistent Android foreground notification showing the active profile name, connection state, and a one-tap **Stop VPN** action so you can terminate the tunnel at any time.

---

## 3. Data Stored Locally on Your Device

All data created or imported in NEXORA X is stored **exclusively on your Android device** inside the application's private internal storage directory (`/data/data/com.nexora.x/`), protected by Android OS process sandboxing:

| Data Category | Storage Location | Purpose | Cloud Backup Status |
| :--- | :--- | :--- | :--- |
| **Imported VPN Profiles** (server host/IP, port, protocol, UUID/password, TLS/SNI/Reality parameters) | Local Room SQLite Database (`nexora_profiles.db`) | Allows you to save, edit, select, and connect to your configured VPN endpoints | **Excluded** (`android:allowBackup="false"`, excluded in `backup_rules.xml` & `data_extraction_rules.xml`) |
| **Application Preferences** (selected profile ID, dark/light theme, sort order, `VpnService` disclosure consent flag) | Local Jetpack DataStore (`nexora_preferences.preferences_pb`) | Remembers your UI preferences and disclosure consent across app restarts | **Excluded** (`android:allowBackup="false"`) |
| **Ephemeral Runtime Diagnostics** (redacted engine state logs, session byte counters, TCP latency measurements) | In-memory ring buffer & private `singbox/` working directory | Displays live connection status, throughput, and troubleshooting diagnostics | **Not persisted to cloud**; sensitive credentials (`UUID`, `password`, `publicKey`, `shortId`) are scrubbed by `LogRedactor` |

You can permanently delete any saved profile at any time from the **Servers** screen, or erase all local data by clearing storage or uninstalling NEXORA X in Android Settings.

---

## 4. Network Connections Initiated by the Application

NEXORA X initiates network traffic **only** in direct response to your configuration and actions:

1. **Your Selected VPN / Proxy Server:** When you connect or run a TCP latency check (`NetworkLatencyChecker`), your device connects directly to the hostname/IP and port specified in your selected profile. Because that server is provided by you or your chosen server operator, its logging and privacy practices are governed by that server operator—not by `[PUBLISHER_NAME]`.
2. **Tunnel DNS & Connectivity Verification (`Stage E` & `Stage F`):**
   - When establishing a tunnel, `sing-box` uses `1.1.1.1` (Cloudflare DNS) to bootstrap outbound domain resolution and `https://1.1.1.1/dns-query` (DNS-over-HTTPS through your proxy tunnel) for remote DNS resolution.
   - To verify whether internet traffic actually passes through your active tunnel (`TunnelConnectivityVerifier`), the app performs a lightweight connectivity check against `cp.cloudflare.com` (`http://cp.cloudflare.com/generate_204`), which returns an empty HTTP `204 No Content` response without transmitting personal data.

---

## 5. Information We Do NOT Collect, Log, or Share

`[PUBLISHER_NAME]` **does not collect, transmit, store, inspect, or share** any of the following:

- Personal identifiers (name, email address, phone number, account ID, or advertising ID / AAID).
- Browsing history, destination URLs, DNS queries, or payload contents of your network traffic.
- Your imported VPN server addresses, UUIDs, passwords, or cryptographic keys.
- Device location (GPS or coarse network location), contacts, photos, microphone, or camera data.
- Third-party analytics, crash-reporting telemetry, or advertising profiles.

Because `[PUBLISHER_NAME]` collects zero user data, **no user data is ever sold, rented, or shared with third parties.**

---

## 6. Permissions Requested by NEXORA X

NEXORA X requests only the minimum Android permissions necessary for a functional VPN client:

- `android.permission.INTERNET` — Required to establish encrypted outbound connections to your configured VPN server.
- `android.permission.ACCESS_NETWORK_STATE` — Required to detect active underlying network interfaces for Android `VpnService`.
- `android.permission.FOREGROUND_SERVICE` & `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` — Required by Android 14+ (API 34–36) to maintain an active `VpnService` session with a visible foreground notification.
- `android.permission.POST_NOTIFICATIONS` (Android 13+ / API 33+) — Used to display the persistent VPN tunnel status notification and the **Stop VPN** quick action.
- `android.net.VpnService` (`BIND_VPN_SERVICE`) — System-managed VPN service binding, activated only after you accept both the in-app prominent disclosure and the Android OS VPN connection prompt.

---

## 7. Children's Privacy

NEXORA X is a technical network utility and does not knowingly collect personal information from anyone, including children under the age of 13 (or the applicable age of digital consent in your jurisdiction).

---

## 8. Changes to This Privacy Policy

If future updates to NEXORA X change how data is handled locally or introduce new optional features, this Privacy Policy will be updated prior to release and the "Last Updated" date at the top of this document will be revised.

---

## 9. Contact Us

If you have questions about this Privacy Policy or the privacy practices of NEXORA X, please contact the publisher at:

- **Publisher / Developer:** `[PUBLISHER_NAME]`
- **Email:** `[CONTACT_EMAIL]`
