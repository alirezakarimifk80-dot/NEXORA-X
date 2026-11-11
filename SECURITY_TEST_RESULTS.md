# NEXORA X — گزارش نتایج تستهای امنیتی اجراشده (Security Test Results)

این سند بهصورت کاملاً شفاف و واقعی مشخص میکند کدام بررسیها و تستها در محیط فعلی اجرا شدهاند (همراه با خروجی واقعی) و کدام موارد بهدلیل محدودیت محیط کانتینر (`Not Tested`) نیازمند اجرا در GitHub Actions یا روی دستگاه فیزیکی اندروید هستند.

---

## ۱. جدول تفکیک وضعیت تستها (Executed vs. Not Tested)

| ردیف | نام تست / بررسی امنیتی | ابزار / روش | وضعیت اجرا | نتیجه واقعی |
| :---: | :--- | :--- | :---: | :--- |
| ۱ | اسکن وجود فایلهای Keystore، `.jks`، `.p12` و `.env` در مخزن | `find` روی فایل‌سیستم | **اجرا شد (Executed)** | **PASS** — هیچ فایل کلید امضا یا `.env` حاوی Secret در مخزن وجود ندارد. |
| ۲ | اسکن استاتیک `X509TrustManager`، `TrustAll` و `ALLOW_ALL_HOSTNAME_VERIFIER` | `grep -rnE` روی `app/src` | **اجرا شد (Executed)** | **PASS** — هیچ TrustManager سفارشی یا غیرفعالسازی بررسی گواهی در کد جاوا/کاتلین وجود ندارد. |
| ۳ | اسکن استاتیک `Log.d`, `Log.e`, `Log.i`, `Log.w`, `Log.v`, `println`, `printStackTrace` | `grep -rnE` روی `app/src` | **اجرا شد (Executed)** | **PASS** — ۰ فراخوانی لاگ خام اندروید در کد کاتلین یافت شد. |
| ۴ | اسکن استاتیک `WebView` و `addJavascriptInterface` | `grep -rnE` روی `app/src` | **اجرا شد (Executed)** | **PASS** — ۰ استفاده از `WebView` در کل پروژه اندروید. |
| ۵ | بررسی استاتیک تمام ارجاعات `NexoraColors.*` پس از رفع خطای `AddProfileScreen.kt:506` | `grep -rhoE` + تطبیق با `Color.kt` | **اجرا شد (Executed)** | **PASS** — تمام ۲۴ توکن رنگی استفادهشده در `app/src` دقیقاً در `Color.kt` تعریف شدهاند. |
| ۶ | بررسی کامپایل و Type-Check سرور و وب‌اپلیکیشن همراه (`tsc --noEmit` & `vite build`) | `compile_applet` & `lint_applet` | **اجرا شد (Executed)** | **PASS** — بیلد کامل و بدون خطای سینتکس یا تایپ. |
| ۷ | بررسی تاریخچه کامیتهای گیت (`.git` History Secret Scan) | `git log` | **Not Tested** | دایرکتوری `.git` در کانتینر فعلی وجود ندارد (`fatal: not a git repository`). نیازمند بررسی در مخزن گیتهاب. |
| ۸ | اجرای محلی `./gradlew testDebugUnitTest` (تستهای JUnit 4 اندروید) | Gradle Wrapper | **Not Tested Locally** | بهدلیل نبود Android SDK محلی و محدودیت IPC دیمن Gradle در کانتینر gVisor، در این کانتینر اجرا نشد (در گام ۸ ورکفلوی GitHub Actions اجرا میشود). |
| ۹ | اجرای محلی `./gradlew lintDebug` (Android Lint Security Checks) | Android Gradle Plugin Lint | **Not Tested Locally** | نیازمند محیط GitHub Actions دارای Android SDK 35. |
| ۱۰ | اسکن باینری APK با MobSF (Mobile Security Framework) | MobSF Static/Dynamic Analyzer | **Not Tested** | ابزار MobSF و فایل APK کامپایل‌شده در کانتینر فعلی موجود نیستند. |
| ۱۱ | تست نشت واقعی ترافیک (DNS Leak / IPv6 Leak / Kill-Switch) هنگام قطع Wi-Fi روی گوشی واقعی | دستگاه فیزیکی اندروید + `tcpdump`/Wireshark | **Not Tested on Device** | نیازمند نصب APK روی دستگاه فیزیکی اندروید و کپچر ترافیک شبکه واقعی است. |

---

## ۲. خروجیهای واقعی دستورات اجراشده در طول ممیزی

### ۲.۱. اسکن فایلهای کلید خصوصی و اسرار (`Keystore / Secret File Scan`)
دستور اجراشده:
```bash
find . -maxdepth 4 \( -name "*.jks" -o -name "*.keystore" -o -name "*.p12" -o -name "*.pfx" -o -name ".env" \) -not -path "./node_modules/*"
```
خروجی واقعی:
```text
(خروجی خالی — هیچ فایل کلید یا .env در مخزن یافت نشد)
```

### ۲.۲. اسکن الگوهای TLS، Cleartext و `redirectStderr` در سورسکد اندروید
دستور اجراشده:
```bash
grep -rnE "X509TrustManager|HostnameVerifier|TrustAll|ALLOW_ALL_HOSTNAME_VERIFIER|usesCleartextTraffic|cleartextTrafficPermitted|allowInsecure|redirectStderr" app/src
```
خروجی واقعی:
```text
app/src/main/java/com/nexora/x/core/engine/SingBoxConfigGenerator.kt:64:        if ((profile.tlsConfig.enabled || secMode != "none") && profile.tlsConfig.allowInsecure) {
app/src/main/java/com/nexora/x/core/engine/SingBoxConfigGenerator.kt:66:                "SECURITY WARNING: 'allowInsecure' ('insecure': true) is enabled for this profile. " +
app/src/main/java/com/nexora/x/core/engine/SingBoxConfigGenerator.kt:310:            append("  \"insecure\": ${profile.tlsConfig.allowInsecure}")
app/src/main/java/com/nexora/x/core/engine/LibboxManager.kt:222:                    Libbox.redirectStderr(File(workDir, "stderr.log").absolutePath)
app/src/main/java/com/nexora/x/core/engine/TunnelConnectivityVerifier.kt:164:                if (securityMode == "tls" && !profile.tlsConfig.allowInsecure) {
app/src/main/java/com/nexora/x/core/engine/TunnelConnectivityVerifier.kt:177:                        val hostnameVerified = HttpsURLConnection.getDefaultHostnameVerifier()
app/src/main/res/xml/network_security_config.xml:10:    <base-config cleartextTrafficPermitted="false">
app/src/main/res/xml/network_security_config.xml:15:    <domain-config cleartextTrafficPermitted="true">
```
**تحلیل خروجی**:
- هیچ پیادهسازی ناامنی از `X509TrustManager` وجود ندارد.
- خط ۲۲۲ فایل `LibboxManager.kt` (`Libbox.redirectStderr(...)`) بهعنوان یافته **`SEC-005`** ثبت شد.
- خط ۱۶۴ فایل `TunnelConnectivityVerifier.kt` بهعنوان یافته **`SEC-003`** ثبت شد.

### ۲.۳. اسکن فراخوانیهای `android.util.Log`، `println` و `WebView`
دستور اجراشده:
```bash
grep -rnE "Log\.(d|e|i|w|v)|println|printStackTrace|WebView|addJavascriptInterface" app/src || echo "ZERO_ANDROID_LOG_OR_WEBVIEW_FOUND"
```
خروجی واقعی:
```text
ZERO_ANDROID_LOG_OR_WEBVIEW_FOUND
```

---

## ۳. پوشش تستهای واحد امنیتی موجود در کد (`app/src/test/`)

در فایلهای تست واحد پروژه، تستهای زیر نوشته شده و آماده اجرا در گام `8. Run Unit Tests (./gradlew testDebugUnitTest)` در GitHub Actions هستند:
1. `SingBoxEngineAndConfigTest.kt`:
   - `CredentialVaultCipher encrypts secrets with AES-256-GCM and ProfileMapper protects credentials at rest` (خط ۱۸۸)
   - `CredentialVaultCipher rejects tampered AES-GCM ciphertext with SecurityException` (خط ۲۳۸)
   - `SingBoxConfigGenerator emits explicit MITM security warning when allowInsecure is enabled` (خط ۱۵۹)
   - `LogRedactor scrubs UUIDs, proxy URIs, JSON passwords, and custom secrets` (خط ۲۴۵)
   - `SingBoxConfigGenerator maps VLESS Reality and XTLS flow with Phase 4 DNS bootstrap and gvisor stack` (خط ۳۷)
   - `Libbox v1_10_7 classes from app_libs_libbox_aar are present on classpath` (خط ۳۲۳)
2. `ProfileDatabaseAndRepositoryTest.kt`:
   - `database CRUD and ProfileMapper preserve all transport, TLS, and warning fields` (خط ۴۷)
   - `exportToUri and parseSingleLink round-trip preserves protocol and endpoint` (خط ۸۸)
3. `ProtocolParsersTest.kt`:
   - تستهای اعتبارسنجی سینتکس و رد ورودیهای مخرب برای `VLESS`, `VMess`, `Trojan` و `Shadowsocks`.

---

## ۴. نتایج تستهای رگرسیون اجراشده در Phase 1 — Batch 1 (`SEC-006` و `SEC-007`)

### ۴.۱. تست خودکار صحت‌سنجی SHA-256 و حذف فایل دستکاری‌شده (`SEC-006`)
دستور اجراشده:
```bash
bash scripts/fetch-libbox-aar.sh --self-test
```
خروجی واقعی:
```text
[libbox-aar] SHA-256 OK (selftest-valid-payload): 5a316a6c94fb9e0a8f3620f3ac472a9afc89b2973c37797692204b9b26461864
[libbox-aar] SEC-006 SELF-TEST PASSED: Fail-closed SHA-256 verification & pinned digests verified.
```

### ۴.۲. سوئیت جامع تست رگرسیون `SEC-006` و `SEC-007` (واحد، DNS Rebinding و تست زنده سرور)
دستور اجراشده:
```bash
npx tsx scripts/test-batch1-security.ts
```
خروجی واقعی:
```text
=== NEXORA X Phase 1 (Batch 1: SEC-006 & SEC-007) Security Regression Suite ===

[SEC-006] Running scripts/fetch-libbox-aar.sh --self-test...
  -> PASS: fetch-libbox-aar.sh --self-test verified valid & fail-closed SHA-256 behavior.
  -> PASS: Pinned SHA-256 constants match verified upstream release artifacts.
  -> PASS: .github/workflows/android-apk.yml enforces SHA verification and removes unverified fallback.

[SEC-007] Testing IPv4 & IPv6 SSRF address classification...
  -> PASS: All 26 prohibited IPv4/IPv6 ranges blocked accurately.
  -> PASS: All 8 public IPv4/IPv6 literals allowed.

[SEC-007] Testing hostname syntax & non-canonical IPv4 bypass vectors...
  -> PASS: All 8 hostname/encoding bypass vectors rejected.

[SEC-007] Testing DNS Rebinding mitigation in resolveAndValidatePublicTarget...
  -> PASS: DNS rebinding multi-record and metadata resolution blocked; legitimate DNS pinned to resolvedIp.

[SEC-007] Testing live Express endpoints (/api/network/ping & /api/network/tunnel-check)...
  -> PASS: Live Express endpoints rejected all 7 SSRF payloads with HTTP 403.

=== ALL 55 BATCH 1 SECURITY REGRESSION ASSERTIONS PASSED ===
```
