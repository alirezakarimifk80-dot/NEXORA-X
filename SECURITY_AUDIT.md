# NEXORA X (`com.nexora.x`) — گزارش جامع ممیزی امنیتی (Comprehensive Security Audit Report)

- **نام پروژه**: `NEXORA X` (`com.nexora.x`)
- **نسخه مورد بررسی**: `1.0.0` (`versionCode = 4`, `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`)
- **موتور هسته VPN**: `sing-box` (`io.nekohasekai.libbox` نسخه `1.10.7`)
- **چارچوب ارزیابی**: **OWASP MASVS v2.1.0 / OWASP MASTG** (`MASVS-STORAGE`, `MASVS-CRYPTO`, `MASVS-NETWORK`, `MASVS-PLATFORM`, `MASVS-CODE`, `MASVS-RESILIENCE`, `MASVS-PRIVACY`) + بررسی امنیتی Backend/CI-CD
- **روش ممیزی**: بررسی دستی خطبهخط سورسکد Kotlin، فایلهای پیکربندی اندروید و Gradle، اسکریپتهای CI/CD گیتهاب، و سرور همراه Express (`server.ts`) + اسکن استاتیک الگوهای پرخطر (بدون تغییر در کدهای اجرایی پروژه در طول ممیزی).

---

## ۱. خلاصه مدیریتی (Executive Summary)

در این ممیزی عمیق امنیتی، کل مخزن پروژه **NEXORA X** بهصورت مستقل از گزارشهای پیشین بازبینی شد. ابتدا اصلاحات قبلی (`SEC-01` تا `SEC-07`) مستقیماً روی سورسکد راستیآزمایی شدند و سپس تمام نقاط کور معماری در ۱۰ حوزه (ذخیرهسازی و رمزنگاری، سرویس VPN و شبکه، تولید کانفیگ `sing-box`، امنیت پلتفرم اندروید، سرور همراه `server.ts`، پایپلاین GitHub Actions و زنجیره تأمین `libbox`) بررسی گردیدند.

### خلاصه آماری یافتههای فعلی (در کد فعلی مخزن)

| سطح خطر (Severity) | تعداد یافتههای باز / نیازمند بهبود در کد فعلی | شناسه‌ها |
| :--- | :---: | :--- |
| **Critical (بحرانی)** | **0** | — |
| **High (بالا)** | **2** | `SEC-006` (زنجیره تأمین CI/CD بدون مقایسه SHA-256 پین‌شده)، `SEC-007` (پورت‌اسکن/SSRF در `/api/network/ping` و `/api/network/tunnel-check` در `server.ts`) |
| **Medium (متوسط)** | **6** | `SEC-001`، `SEC-003`، `SEC-004`، `SEC-005`، `SEC-008`، `SEC-009` |
| **Low (پایین)** | **5** | `SEC-002`، `SEC-010`، `SEC-011`، `SEC-012`، `SEC-013` |
| **Informational (اطلاعاتی)** | **1** | `SEC-014` |
| **مجموع کل یافتهها** | **14 یافته** | ثبت‌شده با جزئیات خط و فایل در `VULNERABILITY_REGISTER.md` |

---

## ۲. راستیآزمایی مستقل اصلاحات قبلی (`SEC-01` تا `SEC-07`) روی سورسکد واقعی

برخلاف تکیه صرف به گزارش قبلی، هر ۷ مورد مستقیماً در کد فعلی بازرسی شدند:

| شناسه قبلی | عنوان | وضعیت واقعی در سورسکد فعلی | یافتههای جدید کشف‌شده در بازرسی عمیق |
| :--- | :--- | :--- | :--- |
| **SEC-01** | رمزنگاری `credential`, `realityPublicKey`, `realityShortId` در Room با AES-256-GCM | **اعمال شده اما دارای نقطه ضعف در مدیریت خطا** (`CredentialVaultCipher.kt:54-82` و `ProfileMapper.kt:26-40`) | در `CredentialVaultCipher.kt` خطوط ۷۸ تا ۸۱، بلوک `catch (_: Throwable)` در صورت بروز خطا در Keystore، مقدار **`plaintext` خام** را بدون رمزنگاری برمیگرداند (`SEC-001`). همچنین `ProfileMapper.kt` خط ۳۲ خطای دستکاری تگ GCM (`SecurityException`) را با `runCatching { ... }.getOrDefault("")` بیصدا نادیده میگیرد (`SEC-002`). |
| **SEC-02** | بررسی تطابق Hostname در اتصال TLS مرحله Stage D | **اعمال شده اما مشروط به `!allowInsecure`** (`TunnelConnectivityVerifier.kt:162-183`) | الگوریتم `HTTPS` و `HttpsURLConnection.getDefaultHostnameVerifier().verify(...)` در خطوط ۱۶۸ و ۱۷۶ اضافه شده است؛ اما در خط ۱۶۲ اگر `allowInsecure == true` باشد، کل هندشیک TLS در Stage D رد میشود و فقط اتصال خام TCP تست میگردد (`SEC-003`). |
| **SEC-03** | جلوگیری از نشت ترافیک IPv6 در شبکه‌های Dual-Stack | **اعمال شده اما هاردکد شده در `NexoraVpnService`** (`NexoraVpnService.kt:138-141` و `SingBoxConfigGenerator.kt:102`) | آدرس `fdfe:dcba:9876::1/126` و مسیر `::/0` در `NexoraVpnService.kt` اضافه شده‌اند، اما در `LibboxManager.kt` خطوط ۳۸۶ تا ۴۱۲ متد `openTun(options: TunOptions?)` فیلد `options?.getInet6Address()` را از `libbox` نمیخواند (`SEC-004`). |
| **SEC-04** | مخفیسازی لینک کپی‌شده در کلیپبورد اندروید ۱۳+ | **بهطور کامل اعمال شده** (`ServersScreen.kt:332-342`) | فلگ `android.content.extra.IS_SENSITIVE = true` روی `ClipData` تنظیم شده است. تنها نکته تکمیلی، نبود پاکسازی خودکار کلیپبورد پس از ۶۰ ثانیه برای اندروید ۸ تا ۱۲ است. |
| **SEC-05** | عدم نمایش نام پروفایل روی صفحه قفل (Lock Screen) | **بهطور کامل اعمال شده** (`VpnNotificationManager.kt:85-102`) | `NotificationCompat.VISIBILITY_PRIVATE` بههمراه `.setPublicVersion(...)` با متن سانسورشده اعمال شده است. |
| **SEC-06** | هشدار صریح MITM هنگام فعال بودن `allowInsecure` | **بهطور کامل اعمال شده** (`SingBoxConfigGenerator.kt:64-69` و `AddProfileScreen.kt:502-511`) | هم در خروجی `warnings` کانفیگ و هم در رابط کاربری (با رنگ `NexoraColors.StatusPendingAmber` در خط ۵۰۶) اعمال شده است. |
| **SEC-07** | بررسی فایلهای باینری `libbox.so` در اسکریپت ساخت AAR | **ناقص (فقط چاپ هش بدون مقایسه با هش مرجع)** (`scripts/fetch-libbox-aar.sh:78-89` و `.github/workflows/android-apk.yml:127-147`) | اسکریپت `fetch-libbox-aar.sh` غیرخالی بودن (`-s`) را چک کرده و هش SHA-256 را **چاپ** میکند، اما آن را با هش ثابت و پین‌شده مقایسه (`sha256sum -c`) نمیکند! همچنین بلوک fallback داخل `android-apk.yml` (خطوط ۱۲۷ تا ۱۴۷) اصلاً بررسی SHA-256 ندارد (`SEC-006`). |

---

## ۳. بررسی عمیق حوزههای ده‌گانه امنیتی

### بخش ۱: امنیت اطلاعات و رمزنگاری (`MASVS-STORAGE` & `MASVS-CRYPTO`)
1. **ذخیرهسازی کلید در Android Keystore (`CredentialVaultCipher.kt:123-156`)**:
   - از الگوریتم `AES/GCM/NoPadding` با طول کلید ۲۵۶ بیت، IV تصادفی ۱۲ بایتی (۹۶ بیتی) و تگ احراز اصالت ۱۲۸ بیتی استفاده شده که از نظر رمزنگاری استاندارد است.
   - **آسیبپذیری `SEC-001` (Medium)**: در `CredentialVaultCipher.kt` خطوط ۷۸ تا ۸۱:
     ```kotlin
     } catch (_: Throwable) {
         // Fail-safe fallback so profile persistence never crashes on broken OEM Keystore firmware
         plaintext
     }
     ```
     اگر به هر دلیل (مثلاً باطل شدن کلید پس از تغییر قفل صفحه یا خطای موقت Keystore) استثنایی رخ دهد، متد `encryptForStorage` بدون اطلاع کاربر، رمز یا UUID را بهصورت **متن خام (Plaintext)** در دیتابیس ذخیره میکند.
2. **ذخیرهسازی مسیر کاری و لاگ `stderr` موتور `libbox` (`LibboxManager.kt:211, 222`)**:
   - **آسیبپذیری `SEC-005` (Medium)**: در `LibboxManager.kt` خط ۲۱۱:
     ```kotlin
     val workDir = File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "singbox_work").apply { mkdirs() }
     ```
     و در خط ۲۲۲:
     ```kotlin
     Libbox.redirectStderr(File(workDir, "stderr.log").absolutePath)
     ```
     استفاده از `getExternalFilesDir(null)` دایرکتوری کاری `singbox_work` و فایل `stderr.log` را روی حافظه خارجی اختصاصی اپ (`/sdcard/Android/data/com.nexora.x/files/singbox_work/stderr.log`) قرار میدهد، نه در حافظه داخلی محافظت‌شده (`context.filesDir`). علاوه بر این، خروجی `redirectStderr` مستقیماً از سطح هسته Go به فایل `stderr.log` نوشته میشود و از فیلتر `LogRedactor` عبور نمیکند.
3. **وضعیت ادعای پاکسازی `active-config.json` در گزارش قبلی**:
   - بررسی واقعی `SingBoxEngine.kt` (خطوط ۷۳، ۱۰۶، ۲۱۷ و ۳۵۶) نشان میدهد کانفیگ JSON اصلاً در فایلی به نام `active-config.json` نوشته نمیشود، بلکه در متغیر حافظه `@Volatile private var preparedConfigJson: String? = null` نگهداری شده و مستقیماً به `Libbox.newService(configJson, platformInterface)` داده میشود. این معماری از نوشتن کانفیگ خام روی دیسک جلوگیری میکند که مثبت است؛ اما در `SingBoxEngine.kt` خط ۳۴۷ اگر وضعیت موتور `DISCONNECTED` یا `ERROR` باشد و `stopSession()` صدا زده شود، متغیر `preparedProfile` در حافظه پاک نمیشود.
4. **بکاپ و انتقال داده (`AndroidManifest.xml:14-16`, `backup_rules.xml`, `data_extraction_rules.xml`)**:
   - ویژگی `android:allowBackup="false"` فعال است و قوانین XML بهدرستی دیتابیس `nexora_profiles.db` را از Cloud Backup و Device-to-Device Transfer خارج کرده‌اند.

---

### بخش ۲: امنیت VPN و شبکه (`MASVS-NETWORK`)
1. **مدیریت File Descriptor و `VpnService.protect()` (`NexoraVpnService.kt:113-170`, `LibboxManager.kt:377-384`)**:
   - متد `protectSocketFd(fd: Int)` در `NexoraVpnService.kt` خط ۱۱۴ فراخوانی `protect(fd)` را انجام میدهد و در `LibboxManager.kt` خط ۳۸۰ اگر `protect(fd)` مقدار `false` برگرداند، استثنای `IllegalStateException` پرتاب میشود تا ترافیک خروجی پراکسی وارد حلقه بی‌نهایت (Routing Loop) نشود.
   - همچنین در `NexoraVpnService.kt` خط ۱۴۹ پکیج خود برنامه با `builder.addDisallowedApplication(packageName)` از تونل مستثنی شده است.
   - مدیریت descriptor با `ParcelFileDescriptor.dup(pfd.fileDescriptor).detachFd()` در خط ۱۶۶ از بروز خطای Double-Close بین جاوا و هسته Go جلوگیری میکند.
2. **جلوگیری از DNS Leak (`SingBoxConfigGenerator.kt:71-103`)**:
   - سرور نهایی DNS روی `dns-remote` (`https://1.1.1.1/dns-query` با `"detour": "proxy"`) تنظیم شده و قانون `"protocol": "dns" -> "dns-out"` تمام درخواستهای DNS داخل تونل را رهگیری میکند.
   - تنها قانون استثنا، `"outbound": "any" -> "dns-direct"` (`1.1.1.1`) است که صرفاً برای Bootstrap کردن دامنه خود سرور پراکسی استفاده میشود تا بن‌بست (Deadlock) اولیه رخ ندهد.
3. **بررسی TLS و گواهیها (`TunnelConnectivityVerifier.kt:161-183`)**:
   - هیچ پیادهسازی ناامنی از `X509TrustManager` یا `HostnameVerifier` سفارشی در کل پروژه وجود ندارد.
   - **آسیبپذیری `SEC-003` (Medium)**: در `TunnelConnectivityVerifier.kt` خط ۱۶۴، اگر کاربر تیک `allowInsecure` را زده باشد (`profile.tlsConfig.allowInsecure == true`)، مرحله Stage D بهطور کامل تست TLS را دور میزند و صرفاً اتصال خام TCP را کافی میداند. همچنین در خطوط ۱۶۷ تا ۱۷۱ تنظیم `sslSocket.sslParameters` داخل `runCatching` قرار گرفته که اگر `sniHost` یک آدرس IP باشد (`SNIHostName` روی رشته IP خطا میدهد)، تنظیم `endpointIdentificationAlgorithm = "HTTPS"` نیز ممکن است قبل از خطای `SNIHostName` اعمال نشود اگر ترتیب خطوط رعایت نشود (در کد فعلی `params.endpointIdentificationAlgorithm = "HTTPS"` قبل از `SNIHostName(sniHost)` است، اما چون `sslSocket.sslParameters = params` بعد از `SNIHostName` قرار دارد، در صورت IP بودن `sniHost`، خط ۱۷۰ اجرا نمیشود!).

---

### بخش ۳: امنیت `sing-box` و اعتبارسنجی کانفیگها (`MASVS-CODE`)
1. **اعتبارسنجی پروفایلها (`SingBoxConfigGenerator.kt:165-237` و `UriParserUtils.kt:77-95`)**:
   - پورت (`1..65535`)، ساختار UUID (فرمت استاندارد ۳۶ کاراکتری هگزادسیمال برای VLESS/VMess)، سایفرهای مجاز Shadowsocks (`ShadowsocksParser.SUPPORTED_CIPHERS`)، و الزام کلید عمومی و SNI در حالت Reality بهطور کامل بررسی میشوند.
2. **احتمال تزریق کاراکترهای کنترلی در JSON (`SingBoxConfigGenerator.kt:377-378`)**:
   - **آسیبپذیری `SEC-011` (Low)**: متد `private fun esc(s: String)` در خط ۳۷۷ فقط کاراکترهای `\` و `"` را escape میکند:
     ```kotlin
     private fun esc(s: String): String =
         s.replace("\\", "\\\\").replace("\"", "\\\"")
     ```
     کاراکترهای کنترلی مانند `\n`، `\r`، `\t` یا کاراکترهای کنترلی یونیکد (`\u0000` تا `\u001F`) در صورت وجود در فیلدهای ورودی (مثل رمز Trojan یا مسیر WebSocket) escape نمیشوند که میتواند ساختار JSON تولیدشده را بشکند.

---

### بخش ۴: امنیت پلتفرم اندروید (`MASVS-PLATFORM` & `MASVS-RESILIENCE`)
1. **`AndroidManifest.xml` و Exported Components**:
   - `MainActivity` دارای `android:exported="true"` صرفاً برای اکشن `MAIN` / `LAUNCHER` است و هیچ Deep Link خارجی برای اجرای خودکار دستورها ندارد.
   - `NexoraVpnService` دارای `android:exported="false"` و مجوز سیستمی `android.permission.BIND_VPN_SERVICE` است.
2. **قوانین R8 / ProGuard (`app/proguard-rules.pro:1-35`)**:
   - **آسیبپذیری `SEC-012` (Low)**: بررسی واقعی فایل `app/proguard-rules.pro` نشان میدهد این فایل فقط ۳۵ خط دارد و برخلاف آنچه در گزارش قبلی نوشته شده بود، قاعده `-assumenosideeffects class android.util.Log` در آن **وجود ندارد**. همچنین قاعده `-keep class com.nexora.x.core.engine.** { *; }` (خط ۱۶) مانع از مبهمسازی (Obfuscation) نام تمام کلاسها و متدهای موتور VPN در نسخه Release میشود.

---

### بخش ۵: امنیت Backend و سرور همراه (`server.ts`)
پروژه دارای یک سرور Express در فایل `server.ts` برای محیط وب/پیش‌نمایش، بررسی پینگ و اتصال خودکار به GitHub است. بررسی خطبهخط `server.ts` یافتههای مهم زیر را نشان داد:
1. **آسیبپذیری `SEC-007` (High — SSRF / پورت‌اسکن شبکه داخلی از طریق `/api/network/ping` و `/api/network/tunnel-check`)**:
   - در `server.ts` خطوط ۸۲۶ تا ۸۸۰ (`/api/network/ping`) و خطوط ۸۸۳ تا ۹۳۳ (`/api/network/tunnel-check`)، سرور هر `host` و `port` (`1..65535`) دریافتی از کلاینت را بدون بررسی IPهای خصوصی/داخلی (`127.0.0.1`, `localhost`, `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.169.254`, `::1`) مستقیماً با `socket.connect(port, rawHost)` باز میکند. این موضوع امکان اسکن پورتهای داخلی سرور میزبان (Blind SSRF via TCP Timing/Error) را فراهم میکند.
2. **آسیبپذیری `SEC-008` (Medium — نگهداری توکن گیتهاب در متغیر سراسری حافظه `sessionGitHubToken`)**:
   - در `server.ts` خط ۱۴ (`let sessionGitHubToken: string | null = null;`) و خط ۱۰۲، توکن گیتهاب در یک متغیر سراسری در پروسه Node.js ذخیره میشود. اگر چندین کاربر بهصورت همزمان به یک نمونه (Instance) مشترک از سرور وب متصل شوند، توکن در حافظه بین درخواستها مشترک خواهد بود.
3. **آسیبپذیری `SEC-009` (Medium — عدم استفاده از پارامتر `state` در OAuth و استفاده از `'*'` در `postMessage`)**:
   - در `server.ts` خطوط ۲۷۳ تا ۲۹۹ (`/api/auth/github/url`) پارامتر ضد-CSRF یعنی `state` تولید نمیشود و در خط ۳۵۲ (`window.opener.postMessage({ type: 'OAUTH_AUTH_SUCCESS' }, '*')`) از wildcard `'*'` استفاده شده است.
4. **آسیبپذیری `SEC-010` (Low — نبود Rate Limiting و محدودیت بالای `15mb` روی JSON در تمام مسیرها)**:
   - در `server.ts` خط ۱۵۶ (`app.use(express.json({ limit: '15mb' }))`) روی تمام مسیرها (حتی `/api/network/ping`) اعمال شده و هیچ میان‌افزار Rate Limiter روی مسیرهای شبکه و احراز هویت وجود ندارد.

---

### بخش ۶: امنیت GitHub Actions و زنجیره تأمین (`CI/CD & Supply Chain`)
1. **آسیبپذیری `SEC-006` (High — عدم مقایسه خودکار هش SHA-256 برای فایلهای دانلودشده در `fetch-libbox-aar.sh` و `android-apk.yml`)**:
   - در `scripts/fetch-libbox-aar.sh` (خطوط ۳۹، ۴۵، ۵۱) و `.github/workflows/android-apk.yml` (خطوط ۷۷ و ۱۲۹ تا ۱۳۱)، فایلهای باینری خارجی (`SFA-1.10.7-universal.apk`، `libbox-1.3.11.aar`، `dex-tools-v2.4.zip` و `commandlinetools-linux-11076708_latest.zip`) با `curl -fL` دانلود میشوند اما هش SHA-256 هیچ‌یک از این فایلها با یک مقدار ثابت و پین‌شده مقایسه (`sha256sum -c`) نمیشود (در خط ۸۳ اسکریپت `fetch-libbox-aar.sh` هش فقط با `echo` چاپ میشود ولی اعتبارسنجی نمیشود).
2. **مجوزهای Workflow و مدیریت Keystore**:
   - در `.github/workflows/android-apk.yml` خط ۱۵ مجوز `permissions: contents: read` بهدرستی روی حداقل سطح دسترسی تنظیم شده است.
   - در خطوط ۱۸۷ تا ۲۰۷، فایل Keystore از Secret به `/tmp/nexora-upload.jks` دیکد شده و در خط ۲۰۶ حذف میشود؛ بااینحال اگر گام `bundleRelease` در خط ۲۰۵ با خطا مواجه شود، بهدلیل `set -o pipefail` دستور `rm -f /tmp/nexora-upload.jks` در خط ۲۰۶ اجرا نمیشود (گرچه کانتینر GitHub Runner موقت است، استفاده از `trap 'rm -f /tmp/nexora-upload.jks' EXIT` امنتر است).

---

## ۴. امتیاز امنیتی نهایی (Security Score: **82 / 100**)

### روش محاسبه امتیاز (Scoring Methodology)
امتیاز پایه از **100** بر اساس ۷ حوزه وزن‌دار زیر محاسبه شده است:

| حوزه امنیتی | وزن | امتیاز کسب‌شده | دلایل کسر امتیاز |
| :--- | :---: | :---: | :--- |
| **۱. رمزنگاری و امنیت داده در حالت سکون (Storage & Crypto)** | 20 | **16 / 20** | استفاده عالی از `AndroidKeyStore` AES-256-GCM و غیرفعال بودن Backup؛ کسر ۴ امتیاز بابت fallback خام در `catch` (`SEC-001`) و بلعیدن خطای GCM در `ProfileMapper` (`SEC-002`). |
| **۲. امنیت تونل VPN، شبکه و TLS (VPN & Network)** | 25 | **21 / 25** | پیادهسازی صحیح `VpnService.protect(fd)`، حذف لوپ با `addDisallowedApplication`، پوشش IPv4+IPv6 و DoH؛ کسر ۴ امتیاز بابت باگ ترتیب `SNIHostName` روی IP و پرش از TLS در Stage D هنگام `allowInsecure` (`SEC-003`) و عدم قرائت مستقیم IPv6 از `TunOptions` (`SEC-004`). |
| **۳. امنیت موتور `sing-box` و عدم نشت لاگ (Engine & Logging)** | 15 | **12 / 15** | عدم وجود هرگونه `Log.d`/`println` و سانسور قوی در `LogRedactor`؛ کسر ۳ امتیاز بابت ذخیره `stderr.log` در `getExternalFilesDir` بدون عبور از `LogRedactor` (`SEC-005`) و escape ناقص کاراکترهای کنترلی JSON (`SEC-011`). |
| **۴. امنیت پلتفرم اندروید و R8 (Platform & Resilience)** | 15 | **13 / 15** | `exported="false"` روی سرویس VPN، `FLAG_IMMUTABLE` روی تمام `PendingIntent`ها، مخفیسازی کلیپبورد (`EXTRA_IS_SENSITIVE`) و اعلان `VISIBILITY_PRIVATE`؛ کسر ۲ امتیاز بابت `-keep` گسترده در `proguard-rules.pro` (`SEC-012`). |
| **۵. امنیت CI/CD و زنجیره تأمین (Supply Chain)** | 10 | **7 / 10** | کسر ۳ امتیاز بابت عدم مقایسه خودکار هش SHA-256 فایلهای دانلودی در `fetch-libbox-aar.sh` و `android-apk.yml` (`SEC-006`). |
| **۶. امنیت سرور همراه وب (`server.ts`)** | 10 | **6 / 10** | کسر ۴ امتیاز بابت امکان پورت‌اسکن داخلی در `/api/network/ping` (`SEC-007`) و متغیر سراسری `sessionGitHubToken` (`SEC-008`). |
| **۷. حریم خصوصی و انطباق با Google Play (Privacy)** | 5 | **5 / 5** | نبود هیچ‌گونه SDK تبلیغاتی/ردیابی، دیالوگ شفاف `VpnDisclosureDialog` و تطابق کامل با سیاست حریم خصوصی. |
| **جمع کل (Total Security Score)** | **100** | **80 + 2 = 82 / 100** | *(+2 امتیاز بابت پوشش تست واحد رمزنگاری و سانسور لاگ)* |

### مواردی که در این محیط تست نشدهاند (`Not Tested`)
بهدلیل اجرای این ممیزی در محیط کانتینر ابری (بدون دستگاه فیزیکی اندروید و بدون اجرای محلی Gradle Daemon):
1. **تست پویا (Dynamic / Runtime Instrumentation) روی دستگاه واقعی اندروید**: بررسی نشت واقعی پکتهای IPv6/DNS با Wireshark/`tcpdump` روی اینترفیسهای `tun0` و `wlan0` هنگام قطع ناگهانی Wi-Fi و سوییچ به Mobile Data (`Not Tested on Physical Device`).
2. **اجرای محلی `./gradlew lintDebug` و MobSF روی فایل APK نهایی**: فایل APK در GitHub Actions ساخته میشود و در این کانتینر محلی تولید نشده است (`Not Tested Locally`).
3. **بازرسی تاریخچه Git (`.git`)**: دایرکتوری کاری فعلی در کانتینر فاقد پوشه `.git` است (`fatal: not a git repository`)، بنابراین تاریخچه کامیتهای قبلی در مخزن ریموت گیتهاب از داخل این کانتینر قابل اسکن نبود (`Not Tested — No Local .git Directory`).
