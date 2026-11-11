# Google Play `VpnService` Declaration & Review Guide — NEXORA X (`com.nexora.x`)

Google Play restricts the use of `android.net.VpnService` to apps whose **core functionality** is a VPN client (or specific enterprise/security categories) and enforces strict policy requirements:
1. Core functionality justification in **Play Console -> App Content -> VPN Service**.
2. Encryption of data in transit from the device to the VPN tunnel endpoint.
3. Compliance with Developer Program Policies (no ad fraud, no hidden traffic redirection, no bypassing other apps' security controls).
4. **Prominent In-App Disclosure & Consent** shown *before* invoking `VpnService.prepare()`.
5. **Organization Developer Account Requirement** for apps using `VpnService`.

---

## 1. Play Console `VpnService` Declaration Form Answers

Navigate to **Google Play Console -> Policy and programs -> App content -> VPN Service** and use the following truthful responses:

### Q1: Does your app use the `VpnService` class?
- **Answer:** **Yes**

### Q2: Why does your app require `VpnService`? (Permitted Core Functionality Category)
- **Answer:** **VPN Client** (Establishing a secure device-level virtual private network tunnel to a user-configured remote server is the primary purpose of the application).

### Q3: Core Functionality Explanation (Copy-Paste for Google Play Reviewers)

```text
NEXORA X (com.nexora.x) is a dedicated, user-configured VPN client utility powered by the open-source sing-box (libbox v1.10.7) engine.

1. Core VPN Functionality:
The sole core purpose of NEXORA X is to allow users to import their own VPN/proxy server configurations (VLESS, VMess, Trojan, and Shadowsocks) and establish an encrypted device-level VPN tunnel to their chosen server. Without android.net.VpnService, the app cannot create the local TUN virtual network interface (172.19.0.1/30) required to route device network traffic through the user's configured VPN endpoint.

2. Prominent In-App Disclosure & Explicit User Consent:
Before NEXORA X ever invokes Android's VpnService.prepare() system permission dialog, the app presents a mandatory in-app "VPN Service Prominent Disclosure" dialog (VpnDisclosureDialog.kt) that clearly explains:
- Why android.net.VpnService is used (to open a local TUN interface routing traffic to the user's selected server).
- How traffic is encrypted between the device and the user-configured remote endpoint.
- That all profiles and settings are stored strictly locally on-device with zero remote telemetry or data collection by the publisher.
The user must explicitly tap "Accept & Continue" before VpnService.prepare() is launched.

3. Encryption & Foreground Visibility:
Traffic between the device and the remote endpoint is encrypted using the protocol specified in the user's profile (TLS, Reality, XTLS, or AEAD ciphers). While connected, NexoraVpnService runs as an Android Foreground Service with a persistent notification displaying live connection status and a one-tap "Stop VPN" disconnect button.
```

---

## 2. Review-Video Storyboard (YouTube Unlisted Video for Google Play Review)

Google Play's `VpnService` declaration form requires a short video link (e.g., an Unlisted YouTube video, 60–90 seconds) demonstrating the in-app **Prominent Disclosure**, the system VPN permission prompt, and the core VPN connection flow on a real Android device or emulator.

Record a continuous screen capture following these 6 steps:

| Step | Timestamp | Action on Screen | What Google Play Reviewers Look For |
| :---: | :---: | :--- | :--- |
| **1** | `0:00 – 0:15` | Fresh launch of **NEXORA X** (`com.nexora.x`). Navigate to **Servers -> Add Profile** and import or create a valid `VLESS` / `Trojan` / `Shadowsocks` profile. | Shows that the app is a genuine user-configured VPN client utility. |
| **2** | `0:15 – 0:35` | Return to the **Home** tab and tap the center **Connect** button for the first time. Pause on the **VPN SERVICE PROMINENT DISCLOSURE** modal (`VpnDisclosureDialog`) and scroll slowly through all 4 disclosure sections. | **CRITICAL:** Proves that the Prominent Disclosure appears **before** `VpnService.prepare()` and includes explicit **Decline** and **Accept & Continue** buttons. |
| **3** | `0:35 – 0:45` | Tap **Accept & Continue** on the in-app disclosure modal. The Android OS system dialog (*"Connection request — NEXORA X wants to set up a VPN connection..."*) immediately appears. Tap **OK**. | Proves the two-step consent sequence (In-App Prominent Disclosure -> Android OS `VpnService.prepare()` dialog). |
| **4** | `0:45 – 1:05` | Show the Home dashboard transitioning through `STARTING` -> `VPN ACTIVE` -> `CONNECTIVITY CHECK` -> `INTERNET AVAILABLE`, along with the Android status bar VPN key icon and the persistent foreground notification (*"NEXORA X — VPN Active"*). | Demonstrates real `VpnService` foreground execution and transparent connection state reporting. |
| **5** | `1:05 – 1:15` | Open the **Tools** tab to show the **6-Stage Tunnel Connectivity Verifier (Stages A–F)** and redacted `sing-box` configuration summary. | Demonstrates technical transparency and credential redaction. |
| **6** | `1:15 – 1:25` | Pull down the Android notification shade and tap **Stop VPN** (or tap **Disconnect** on the Home dashboard) to show clean tunnel teardown back to `DISCONNECTED`. | Proves immediate user control to terminate the VPN session at any time. |

---

## 3. Critical Google Play Developer Account Requirements for VPN Apps

Before submitting `com.nexora.x` to Google Play, verify the following mandatory policy requirements:

1. **Organization Developer Account Required:**
   - Under Google Play's financial and VPN security policies, only **Organization** developer accounts (verified with a D-U-N-S number and official business registration documents) are permitted to publish new apps that declare `android.net.VpnService`. Personal developer accounts are blocked from publishing new VPN apps on Google Play.
2. **Hosted HTTPS Privacy Policy URL:**
   - Host `/privacy-policy.html` (after replacing `[PUBLISHER_NAME]` and `[CONTACT_EMAIL]`) on a publicly accessible HTTPS URL (such as your organization domain or GitHub Pages) that loads without login or JavaScript walls, and enter that exact URL in **Play Console -> App content -> Privacy policy**.
3. **App Signing by Google Play:**
   - Upload the signed release Android App Bundle (`app-release.aab`) generated by `./gradlew bundleRelease` with your production upload keystore configured via GitHub Secrets (`RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`).
