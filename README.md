# NEXORA X — Native Android V2Ray / `sing-box` Client & GitHub Actions APK CI

**Application Name:** `NEXORA X`  
**Package Name:** `com.nexora.x` (Debug Application ID: `com.nexora.x.debug`)  
**Version:** `1.2.0-phase3` (`versionCode = 3`)  
**Platform:** Native Android (`minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`, `JDK 17`)  
**Language & UI Framework:** Kotlin 2.0.21 + Jetpack Compose (Material 3)  
**Build System:** Gradle 8.9 (`:app` Android Application Module)  
**CI/CD Workflow:** `.github/workflows/android-apk.yml` (`NEXORA-X-APK` Artifact, 14-day retention)

---

## 1. GitHub Actions Automatic APK Builder (`.github/workflows/android-apk.yml`)

This repository is configured with a complete GitHub Actions pipeline at [`.github/workflows/android-apk.yml`](./.github/workflows/android-apk.yml) that automatically tests and builds the **NEXORA X Debug APK** (`app-debug.apk`) and generates a clean `build-info.txt` manifest.

### 1.1 How Automatic Builds Work
Whenever new commits are pushed to the `main` branch, GitHub Actions automatically starts the **`NEXORA X — Android Debug APK CI`** workflow on an `ubuntu-latest` runner:
1. **Checkout**: Checks out the repository (`actions/checkout@v4`).
2. **JDK 17**: Installs Eclipse Temurin JDK 17 (`actions/setup-java@v4`).
3. **Android SDK**: Sets up the Android SDK (`android-actions/setup-android@v3`).
4. **Gradle 8.9 & Caching**: Configures Gradle 8.9 with dependency and wrapper caching (`gradle/actions/setup-gradle@v4`) and ensures `chmod +x ./gradlew`.
5. **Unit Tests**: Executes `./gradlew testDebugUnitTest --stacktrace` (running Phase 1, Phase 2, and Phase 3 JUnit 4 suites).
6. **Assemble Debug APK**: Executes `./gradlew assembleDebug --stacktrace` on the `:app` module.
7. **Verify APK & Generate `build-info.txt`**: Verifies that a non-empty `.apk` file exists in `app/build/outputs/apk/debug/` and generates `build-info.txt` (App name, Package name, Build date, Git commit SHA, Build variant, Version name, Version code, `sing-box` version `1.10.7`, APK size, and SHA-256 hash—with zero secrets).
8. **Upload Artifact (`NEXORA-X-APK`)**: Uploads `app/build/outputs/apk/debug/*.apk` and `build-info.txt` as a downloadable artifact named **`NEXORA-X-APK`** with **14 days** retention (`actions/upload-artifact@v4`, `if-no-files-found: error`).

---

### 1.2 How to Upload Changes to GitHub

You can upload changes to GitHub using any of the following three methods:

#### Method A: One-Click "Auto-Upload to GitHub" Button in the Workbench
1. Configure either `GITHUB_TOKEN` (a GitHub Personal Access Token with `repo` and `workflow` scopes) or `GITHUB_CLIENT_ID` + `GITHUB_CLIENT_SECRET` in the AI Studio **Secrets** panel.
2. Click the **"Auto-Upload to GitHub / آپلود خودکار در گیت‌هاب"** button in the top navigation bar of the workbench.
3. Enter your target repository name (e.g., `NEXORA-X`) and branch (`main`) and click **Upload All Project Files & Trigger APK Build**. The server automatically pushes all Android project files and `.github/workflows/android-apk.yml` to `main`.

#### Method B: Using Git CLI from Your Computer or Termux
```bash
git init
git branch -M main
git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>.git
git add .
git commit -m "Configure NEXORA X Android project and GitHub Actions APK CI"
git push -u origin main
```

#### Method C: Using GitHub Website or GitHub Android App
1. Open your repository on **github.com** (or in the GitHub Android app).
2. Tap **Add file** -> **Create new file**.
3. Type the exact file path: `.github/workflows/android-apk.yml`
4. Copy the contents of `.github/workflows/android-apk.yml` (available with 1-click Copy or Download in the workbench) and paste it into the editor.
5. Select **Commit directly to the `main` branch** and tap **Commit changes**.

---

### 1.3 How to Manually Start a Build (`workflow_dispatch`)

You can trigger a fresh APK build at any time without pushing new code:
1. Open your GitHub repository in a browser.
2. Click the **Actions** tab at the top of the repository.
3. In the left sidebar, select **NEXORA X — Android Debug APK CI**.
4. Click the **Run workflow** dropdown button on the right, keep branch **`main`** selected, and click the green **Run workflow** button.
5. *(Optional)* If connected in the workbench, you can also click **Trigger Manual Build (`workflow_dispatch`)** inside the GitHub Auto-Upload modal.

---

### 1.4 Where to Download the Built APK (`NEXORA-X-APK`)

1. Go to your repository on GitHub and click the **Actions** tab.
2. Click on the latest completed workflow run (marked with a green checkmark ✅).
3. Scroll down to the **Artifacts** section at the bottom of the workflow run summary page.
4. Click **`NEXORA-X-APK`** to download the `.zip` archive.
5. Extract `NEXORA-X-APK.zip` to access:
   - `app-debug.apk` (the installable Android Debug APK for `com.nexora.x.debug`)
   - `build-info.txt` (complete build metadata and SHA-256 checksum)

---

### 1.5 How to Inspect Failed Builds

If a workflow run fails (marked with a red ❌):
1. Click on the failed workflow run in the **Actions** tab.
2. Scroll to the **Summary** view to read the **`NEXORA X Build Failed`** diagnostic card generated by Step 10 (`Diagnose Build or Test Failure`). It identifies the exact failing task (`:app:testDebugUnitTest`, `:app:assembleDebug`, or `APK Verification`), explains the root cause, and provides the concrete fix.
3. Click the **Test & Assemble NEXORA X Debug APK** job to expand the full `--stacktrace` output from Gradle.
4. **Safety Guarantee**: When any test or build step fails, Step 9 (`actions/upload-artifact`) is skipped (`if: success()`) so an incomplete or fake APK is never uploaded.

---

### 1.6 How to Build Locally on Your Workstation

#### Prerequisites
- **Android Studio Ladybug (2024.2.1+)** or newer
- **JDK 17** (`Eclipse Temurin` or Android Studio bundled JBR 17)
- **Android SDK Platform 35** (`platforms;android-35`)

#### Commands
```bash
# 1. Grant executable permission to gradlew
chmod +x ./gradlew

# 2. Run Phase 1 + Phase 2 + Phase 3 Unit Tests
./gradlew testDebugUnitTest

# 3. Assemble the Debug APK
./gradlew assembleDebug
```
The generated Debug APK will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

#### Future Release Signing Preparation
`app/build.gradle.kts` is pre-configured to keep `debug` APK builds 100% independent of release signing. For future signed `release` builds, supply environment variables (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) in CI secrets without ever committing `.jks` or `.keystore` files to Git.
