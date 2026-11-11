# NEXORA X (`com.nexora.x`) — PHASE 0 Final CI Verification Report

- **Project**: `NEXORA X` (`com.nexora.x`)
- **Version**: `1.0.0` (`versionCode = 4`)
- **Verification Timestamp (UTC)**: `2026-11-11T21:32:00Z`
- **Overall CI Execution Status**: **`BLOCKED` (Awaiting GitHub Push / Token Trigger from User UI; Local Container Lacks Android SDK & `.git` Remote)**
- **Workflow & Source Readiness Status**: **`PASS` (All 6 Gradle Tasks, SDK/JDK 17 Setup, `libbox v1.10.7` Assembly, and APK/AAB/Lint Artifact Uploads Configured & Static-Verified)**

---

## 1. Workflow Run Result & Commit SHA

| Field | Status / Value | Evidence |
| :--- | :--- | :--- |
| **Workflow File** | `.github/workflows/android-apk.yml` | Present & verified (lines 1–406) |
| **Workflow Run Result** | **`BLOCKED`** | Cannot be triggered automatically from inside the AI Studio container because no GitHub Personal Access Token (`GITHUB_TOKEN`) is connected in the container session (`GET /api/github/status` -> `{"connected":false,"hasOAuthClient":false,"authMethod":null,"user":null,"filesCount":118}`) and the container workspace has no `.git` directory (`fatal: not a git repository`). |
| **Commit SHA** | **`N/A (Blocked — No Local .git Repository or Active GitHub Token in Container)`** | Once pushed via the **Auto-Upload to GitHub** panel in the web UI, GitHub Actions assigns the commit SHA (`${GITHUB_SHA}`) and records it in `build-info.txt`. |

---

## 2. Exact Configured Gradle, JDK & Android SDK Versions

Verified directly from `.github/workflows/android-apk.yml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/libs.versions.toml`, and `app/build.gradle.kts`:

| Component | Exact Version | Source File & Line | Verification Status |
| :--- | :--- | :--- | :---: |
| **JDK Distribution & Version** | **Eclipse Temurin JDK `17`** (`JAVA_VERSION: '17'`) | `.github/workflows/android-apk.yml:19, 40–45`, `app/build.gradle.kts:89–96` | **PASS (Verified in Config)** |
| **Gradle Wrapper Version** | **Gradle `8.9`** (`gradle-8.9-bin.zip`) | `gradle/wrapper/gradle-wrapper.properties:3`, `.github/workflows/android-apk.yml:20, 152–165` | **PASS (Verified in Config)** |
| **Android Gradle Plugin (AGP)** | **`8.7.2`** | `gradle/libs.versions.toml:2` | **PASS (Verified in Config)** |
| **Kotlin & Compose Compiler** | **Kotlin `2.0.21`**, **KSP `2.0.21-1.0.27`** | `gradle/libs.versions.toml:3–4` | **PASS (Verified in Config)** |
| **Android `compileSdk` & `targetSdk`** | **API `35`** (`platforms;android-35`) | `app/build.gradle.kts:11, 16`, `.github/workflows/android-apk.yml:21, 95–97, 109` | **PASS (Verified in Config)** |
| **Android `buildToolsVersion`** | **`35.0.0`** (`build-tools;35.0.0`) | `.github/workflows/android-apk.yml:22, 98–100, 110` | **PASS (Verified in Config)** |
| **Android `minSdk`** | **API `26`** (Android 8.0) | `app/build.gradle.kts:15` | **PASS (Verified in Config)** |
| **Pinned `sing-box` / `libbox` AAR** | **`1.10.7`** (`app/libs/libbox.aar`) | `.github/workflows/android-apk.yml:26, 116–151`, `scripts/fetch-libbox-aar.sh:17` | **PASS (Verified in Config)** |

---

## 3. Verification of `libbox v1.10.7` Pre-Compilation Step

In `.github/workflows/android-apk.yml` (**Step 4: `Assemble & Verify Official sing-box libbox.aar (v1.10.7)`**, lines 116–151) and `scripts/fetch-libbox-aar.sh` (lines 1–112):
- Runs **before** Gradle compilation (`Step 7b`).
- Downloads the official `SFA-1.10.7-universal.apk` from `SagerNet/sing-box` release `v1.10.7` and extracts `io/nekohasekai/libbox/*` bytecode plus native `lib/{arm64-v8a,armeabi-v7a,x86_64,x86}/libbox.so` binaries, combining them with `go/Seq` runtime classes into `app/libs/libbox.aar`.
- Validates that `app/libs/libbox.aar` is non-empty (`test -s "app/libs/libbox.aar"`), contains `classes.jar` (`io/nekohasekai/libbox/Libbox.class`, `PlatformInterface.class`, `go/Seq.class`), and packages all 4 ABI native binaries (`jni/arm64-v8a/libbox.so`, `jni/armeabi-v7a/libbox.so`, `jni/x86_64/libbox.so`, `jni/x86/libbox.so`).

---

## 4. Result of Every Required Command

Per the strict truthfulness rule (*"Do not report PASS unless the corresponding GitHub Actions step actually completed successfully. If GitHub Actions cannot run, report BLOCKED and explain why"*):

| Order | Command | Workflow Step in `.github/workflows/android-apk.yml` | Execution Status | Reason / Details |
| :---: | :--- | :--- | :---: | :--- |
| **1** | `./gradlew clean` | `Step 7` (`id: gradle_clean`, lines 167–171) | **`BLOCKED`** | Configured in workflow; awaiting GitHub Actions execution (no local Android SDK in container; no GitHub token connected in container session). |
| **2** | `./gradlew :app:compileDebugKotlin` | `Step 7b` (`id: compile_debug_kotlin`, lines 173–178) | **`BLOCKED`** | Configured in workflow; previously reported `AddProfileScreen.kt:506` (`StatusWarningAmber`) is fixed in source (`NexoraColors.StatusPendingAmber`), awaiting CI run. |
| **3** | `./gradlew :app:testDebugUnitTest` | `Step 8` (`id: unit_tests`, lines 180–185) | **`BLOCKED`** | Configured in workflow (`CredentialVaultCipher.isEncryptedPayload` & `ProfileEntity.toString()` test assertions aligned in source), awaiting CI run. |
| **4** | `./gradlew :app:lintDebug` | `Step 8b` (`id: lint_debug`, lines 187–192) | **`BLOCKED`** | Configured in workflow and `app/build.gradle.kts` (`xmlReport = true`, `htmlReport = true`), awaiting CI run. |
| **5** | `./gradlew :app:assembleDebug` | `Step 9` (`id: assemble_debug`, lines 194–199) | **`BLOCKED`** | Configured in workflow with post-build verification of all 4 `lib/*/libbox.so` ABIs inside the generated `.apk` (`Step 10`), awaiting CI run. |
| **6** | `./gradlew :app:bundleRelease` | `Step 9b` (`id: bundle_release`, lines 201–220) | **`BLOCKED`** | Configured in workflow with R8 minification (`isMinifyEnabled = true`, `isShrinkResources = true`), awaiting CI run. |
| **—** | `compile_applet` & `lint_applet` (Companion Web Workbench & Express Server) | Local AI Studio Build & TypeScript Checker | **`PASS`** | Executed locally in container (`tsc --noEmit` & Vite production build succeeded with 0 errors). |

---

## 5. APK, AAB, and Lint Artifacts Configuration & Status

| Artifact Name | Configured Path(s) in `.github/workflows/android-apk.yml` | Generated File Size & SHA-256 | Status |
| :--- | :--- | :--- | :---: |
| **`NEXORA-X-APK`** (`Step 11`, lines 318–327) | `app/build/outputs/apk/debug/*.apk` (`app-debug.apk`) + `build-info.txt` | **N/A (`BLOCKED` — Awaiting GitHub Actions Run)** | Configured (`if-no-files-found: error`) |
| **`NEXORA-X-RELEASE-AAB`** (`Step 11b`, lines 329–338) | `app/build/outputs/bundle/release/**/*.aab` (`app-release.aab`) + `build-info.txt` | **N/A (`BLOCKED` — Awaiting GitHub Actions Run)** | Configured (`if-no-files-found: warn`) |
| **`NEXORA-X-LINT-REPORTS`** (`Step 11c`, lines 340–350) | `app/build/reports/lint-results-debug.html`, `app/build/reports/lint-results-debug.xml`, `app/build/reports/tests/testDebugUnitTest/**`, `build-info.txt` | **N/A (`BLOCKED` — Awaiting GitHub Actions Run)** | Configured (`if: always()`) |

---

## 6. Lint Issue Summary

- **Runtime Android Lint (`./gradlew :app:lintDebug`)**: **`BLOCKED / NOT TESTED`** in local container (requires Android SDK 35 on GitHub Actions runner; `.github/workflows/android-apk.yml` lines 254–261 automatically extracts `LINT_ERRORS` and `LINT_WARNINGS` from `app/build/reports/lint-results-debug.xml` and uploads the full HTML/XML report in `NEXORA-X-LINT-REPORTS`).
- **Static Source Pre-Checks Executed in Container**:
  - `NexoraColors.*` reference check across `app/src`: **0 unresolved color references** (`AddProfileScreen.kt:506` verified as `NexoraColors.StatusPendingAmber`).
  - Raw `android.util.Log` / `println` / `WebView` scan across `app/src`: **0 occurrences**.
  - `AndroidManifest.xml` service export & permission check: `NexoraVpnService` has `android:exported="false"`, `android:permission="android.permission.BIND_VPN_SERVICE"`, and `android:foregroundServiceType="specialUse"`.

---

## 7. Remaining Blockers & How to Unblock

1. **Blocker 1 — No Active GitHub Token in Container Session (`BLOCKED`)**:
   - **Cause**: The container's `/api/github/status` endpoint reports `connected: false` (`sessionGitHubToken` is null and `GITHUB_TOKEN` is not set in `.env`), so the agent cannot push to GitHub or trigger GitHub Actions directly from the shell.
   - **How to Unblock**: Use the **Auto-Upload to GitHub** button in the web preview (which uses your GitHub Personal Access Token to commit all 118 project files — including the fixed `AddProfileScreen.kt`, `CredentialVaultCipher.kt`, `SingBoxEngineAndConfigTest.kt`, and `.github/workflows/android-apk.yml` — and triggers the workflow run on GitHub's `ubuntu-latest` runner).
2. **Blocker 2 — Local Container Environment Lacks Android SDK & Blocks Gradle Daemon Loopback IPC (`BLOCKED`)**:
   - **Cause**: The AI Studio Cloud Run (gVisor) container is a Node.js/Linux container without `/usr/local/lib/android/sdk` (`platforms;android-35`) and restricts Gradle Daemon local loopback sockets.
   - **Resolution**: All 6 Gradle commands (`clean`, `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:bundleRelease`) are wired into `.github/workflows/android-apk.yml` in exact sequential order.
