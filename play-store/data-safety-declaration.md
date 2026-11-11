# Google Play Data Safety Declaration — NEXORA X (`com.nexora.x`)

This document provides the exact, truthful answers for the **Google Play Console -> App Content -> Data Safety** questionnaire based on a complete code audit of `com.nexora.x`.

---

## 1. Summary of Data Collection & Sharing

Per Google Play's Data Safety definitions:
- **"Collected"** means transmitting data off the user's device to the developer or a third party hired by the developer.Data that remains strictly on the user's device (local Room SQLite database `nexora_profiles.db` and local DataStore `nexora_preferences.preferences_pb`) is **not** considered "collected" by Google Play.
- Because NEXORA X has **zero developer backend servers**, **zero analytics SDKs** (no Firebase Analytics, Crashlytics, Sentry, Adjust, or AppsFlyer), **zero advertising SDKs**, and **disabled cloud auto-backup** (`android:allowBackup="false"`), NEXORA X **does not collect or share any user data**.

---

## 2. Step-by-Step Google Play Console Questionnaire Answers

### Step 1: Data Collection and Security

| Question | Answer | Technical Justification |
| :--- | :--- | :--- |
| **Does your app collect or share any of the required user data types?** | **No** | All imported VPN profiles (`nexora_profiles.db`), preferences (`nexora_preferences`), and redacted ring-buffer logs remain 100% local on the user's Android device. No data is transmitted to the app publisher. When the user connects to a VPN server, they connect strictly to the server endpoint they imported themselves. |
| **Is all of the user data collected by your app encrypted in transit?** | *(Not applicable when "No" is selected above; if asked in supplemental review)* **Yes** | Outbound tunnel traffic uses user-configured encryption (`TLS`, `Reality`, `XTLS`, `Shadowsocks AEAD/2022`), and app-level cleartext traffic is blocked by `network_security_config.xml` (`cleartextTrafficPermitted="false"`). |
| **Do you provide a way for users to request that their data is deleted?** | *(Not applicable when "No" is selected above; local deletion is built in)* | Users have full local control to delete individual profiles or all profiles at any time inside the **Servers** screen, or clear app storage / uninstall the app. |

---

## 3. Audit of All Google Play Data Categories

| Google Play Data Category | Collected Off-Device? | Shared With Third Parties? | Code Audit Verification |
| :--- | :---: | :---: | :--- |
| **Location** (Approximate / Precise) | No | No | Neither `ACCESS_FINE_LOCATION` nor `ACCESS_COARSE_LOCATION` is declared in `AndroidManifest.xml`. |
| **Personal Info** (Name, Email, User IDs, Address, Phone) | No | No | No account creation, login flow, or user identity fields exist in the codebase. |
| **Financial Info** (Payment info, Purchase history) | No | No | Free app with zero billing or in-app purchase dependencies (`com.android.billingclient` is absent). |
| **Health and Fitness** | No | No | Not accessed. |
| **Messages** (Emails, SMS/MMS, In-app messages) | No | No | Not accessed. |
| **Photos and Videos** | No | No | No media/gallery read permissions requested. |
| **Audio Files / Voice or Sound Recordings** | No | No | `RECORD_AUDIO` is not declared. |
| **Files and Docs** | No | No | No external storage permissions (`READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`) declared. |
| **Calendar** | No | No | Not accessed. |
| **Contacts** | No | No | `READ_CONTACTS` is not declared. |
| **App Activity** (App interactions, In-app search history, Installed apps, Web browsing history) | No | No | Search queries in `ServersScreen` filter local Room records in-memory. `QUERY_ALL_PACKAGES` is **not** declared. Browsing history is never inspected or logged. |
| **App Info and Performance** (Crash logs, Diagnostics, Other app performance data) | No | No | Diagnostics (`EngineDiagnosticsSnapshot`, `TunnelStageDiagnostics`) and `LogRedactor` logs are kept strictly in an in-memory ring buffer for local user inspection on `ToolsScreen` and are never uploaded. |
| **Device or Other IDs** (Advertising ID, Android ID, IMEI, MAC) | No | No | `com.google.android.gms.permission.AD_ID` is not declared; no device fingerprinting or identifier APIs are called. |

---

## 4. Verification of Dependencies (`gradle/libs.versions.toml` & `app/build.gradle.kts`)

Every library bundled in `com.nexora.x` has been audited for hidden data collection:

1. `app/libs/libbox.aar` (`io.nekohasekai.libbox`, `sing-box v1.10.7`, `GPL-3.0-or-later`) — Pure local proxy/TUN engine; contains no telemetry or analytics beacons.
2. `androidx.core:core-ktx`, `androidx.activity:activity-compose`, `androidx.lifecycle:*`, `androidx.compose.*`, `androidx.navigation:navigation-compose` — Standard Android Jetpack UI/lifecycle libraries; zero network transmission.
3. `androidx.room:room-runtime` & `room-ktx` — Local SQLite ORM; stores `nexora_profiles.db` strictly in `/data/data/com.nexora.x/databases/`.
4. `androidx.datastore:datastore-preferences` — Local key-value store; stores `nexora_preferences.preferences_pb` strictly in `/data/data/com.nexora.x/files/datastore/`.
5. `com.google.dagger:hilt-android` — Compile-time dependency injection; zero network capability.
