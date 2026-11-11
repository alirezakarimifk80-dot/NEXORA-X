# NEXORA X — برنامه اجرایی اصلاح آسیبپذیریها (Security Remediation Plan)

این سند برنامه گامبهگام و اولویتبندی‌شده برای رفع ۱۴ یافته ثبت‌شده در `VULNERABILITY_REGISTER.md` را پیش از انتشار عمومی نسخه `1.0.0` مشخص میکند.

---

## اولویت ۱: آسیبپذیریهای سطح بالا (`High`) — الزامی پیش از انتشار عمومی

### ۱.۱. رفع `SEC-006`: پین کردن و اعتبارسنجی خودکار هش SHA-256 در `scripts/fetch-libbox-aar.sh` و `.github/workflows/android-apk.yml`
- **فایلهای هدف**:
  - `scripts/fetch-libbox-aar.sh` (خطوط ۳۶ تا ۵۶)
  - `.github/workflows/android-apk.yml` (خطوط ۷۷ و ۱۲۹ تا ۱۳۱)
- **تغییر دقیق موردنیاز**:
  1. تعریف متغیرهای ثابت `EXPECTED_SFA_SHA256`، `EXPECTED_GOMOBILE_SHA256` و `EXPECTED_DEX2JAR_SHA256` در ابتدای `scripts/fetch-libbox-aar.sh`.
  2. اجرای بررسی سختگیرانه `echo "${EXPECTED_SFA_SHA256}  ${SFA_APK_CACHE}" | sha256sum -c -` بلافاصله پس از دانلود هر فایل و توقف اسکریپت (`exit 1`) در صورت عدم تطابق هش.
  3. افزودن `trap 'rm -f /tmp/nexora-upload.jks' EXIT` در گام `9b` فایل `.github/workflows/android-apk.yml` (خط ۱۹۵) تا در صورت شکست دستور `./gradlew bundleRelease`، فایل Keystore موقت حتماً از `/tmp` پاک شود.

### ۱.۲. رفع `SEC-007`: مسدودسازی آدرسهای داخلی و خصوصی (Anti-SSRF) در `/api/network/ping` و `/api/network/tunnel-check`
- **فایل هدف**:
  - `server.ts` (خطوط ۸۲۶ تا ۹۳۴)
- **تغییر دقیق موردنیاز**:
  1. نوشتن تابع کمکی `isPrivateOrReservedIp(ip: string): boolean` در `server.ts` که رنجهای زیر را مسدود کند:
     - IPv4 Loopback (`127.0.0.0/8`)
     - IPv4 Private (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`)
     - IPv4 Link-Local & Cloud Metadata (`169.254.0.0/16`, `0.0.0.0/8`, `100.64.0.0/10`)
     - IPv6 Loopback (`::1`), Link-Local (`fe80::/10`), Unique Local (`fc00::/7`, `fd00::/8`), و IPv4-mapped IPv6 (`::ffff:127.0.0.1` و غیره)
  2. فراخوانی `dns.promises.lookup(rawHost, { all: true })` پیش از `socket.connect` و رد کردن درخواست با کد `403 Forbidden` اگر هر یک از IPهای برگشتی خصوصی یا رزروشده باشد، و سپس اتصال مستقیم سوکت به IP عمومی تأییدشده (برای جلوگیری از حمله DNS Rebinding).

---

## اولویت ۲: آسیبپذیریهای سطح متوسط (`Medium`) — توصیه‌شده پیش از انتشار

### ۲.۱. رفع `SEC-001` و `SEC-002`: حذف بازگشت خام (`plaintext`) در `CredentialVaultCipher.kt` و مدیریت خطای رمزگشایی در `ProfileMapper.kt`
- **فایلهای هدف**:
  - `app/src/main/java/com/nexora/x/core/security/CredentialVaultCipher.kt` (خطوط ۵۸ تا ۸۱)
  - `app/src/main/java/com/nexora/x/core/database/ProfileMapper.kt` (خطوط ۲۶ تا ۴۵)
- **تغییر دقیق موردنیاز**:
  1. در `CredentialVaultCipher.encryptForStorage`: در صورت بروز استثنا در کلید `AndroidKeyStore`، کلید معیوب حذف (`keyStore.deleteEntry(KEYSTORE_ALIAS)`) و کلید جدید ساخته شود و رمزنگاری مجدداً انجام گیرد؛ در صورت شکست مجدد روی رامهای معیوب، با کلید AES-256 پشتیبان رمزنگاری شود و **هیچگاه `plaintext` خام برگردانده نشود**.
  2. در `ProfileMapper.toDomain`: اگر `decryptFromStorage` با `SecurityException` مواجه شد، یک پیام هشدار به لیست `warnings` پروفایل اضافه شود تا کاربر متوجه نیاز به وارد کردن مجدد رمز شود.

### ۲.۲. رفع `SEC-005`: انتقال `workDir` از `getExternalFilesDir` به `filesDir` داخلی و مدیریت امن `stderr.log`
- **فایل هدف**:
  - `app/src/main/java/com/nexora/x/core/engine/LibboxManager.kt` (خطوط ۲۱۱، ۲۲۲ و ۳۱۴ تا ۳۳۶)
- **تغییر دقیق موردنیاز**:
  1. تغییر خط ۲۱۱ از:
     ```kotlin
     val workDir = File(appContext.getExternalFilesDir(null) ?: appContext.filesDir, "singbox_work").apply { mkdirs() }
     ```
     به:
     ```kotlin
     val workDir = File(appContext.filesDir, "singbox_work").apply { mkdirs() }
     ```
  2. در `stopNativeBoxService()`، فایل `File(appContext.filesDir, "singbox_work/stderr.log")` و فایلهای موقت داخل `singbox_tmp` پاکسازی شوند.

### ۲.۳. رفع `SEC-003` و `SEC-004`: بهبود بررسی TLS در `TunnelConnectivityVerifier.kt` و خواندن IPv6 از `TunOptions` در `LibboxManager.kt`
- **فایلهای هدف**:
  - `app/src/main/java/com/nexora/x/core/engine/TunnelConnectivityVerifier.kt` (خطوط ۱۶۲ تا ۱۷۴)
  - `app/src/main/java/com/nexora/x/core/engine/LibboxManager.kt` (خطوط ۳۸۶ تا ۴۱۲)
- **تغییر دقیق موردنیاز**:
  1. در `TunnelConnectivityVerifier.kt`، تنظیم `params.endpointIdentificationAlgorithm = "HTTPS"` و `sslSocket.sslParameters = params` را خارج از بلوک `SNIHostName(sniHost)` انجام دهید تا اگر `sniHost` یک آدرس IP بود، الگوریتم تطابق Hostname همچنان روی سوکت ثبت شود.
  2. در `LibboxManager.openTun`، فیلد `options?.getInet6Address()` نیز خوانده شود.

### ۲.۴. رفع `SEC-008` و `SEC-009`: حذف `sessionGitHubToken` سراسری و افزودن پارامتر `state` به OAuth در `server.ts`
- **فایل هدف**:
  - `server.ts` (خطوط ۱۴، ۹۵ تا ۱۱۲، ۲۷۳ تا ۳۶۶)
- **تغییر دقیق موردنیاز**:
  1. جایگزینی متغیر سراسری `sessionGitHubToken` با یک `Map<string, { token: string; expiresAt: number }>` مبتنی بر یک شناسه نشست تصادفی (`crypto.randomUUID()`) که در کوکی `HttpOnly` یا هدر اختصاصی کلاینت ارسال میشود.
  2. افزودن پارامتر تصادفی `state` در `/api/auth/github/url` و اعتبارسنجی آن در `/auth/callback`.

---

## اولویت ۳: آسیبپذیریهای سطح پایین (`Low`) و بهینهسازیهای دفاعی

### ۳.۱. رفع `SEC-011`: Escape کامل کاراکترهای کنترلی در `SingBoxConfigGenerator.kt`
- **فایل هدف**: `app/src/main/java/com/nexora/x/core/engine/SingBoxConfigGenerator.kt` (خطوط ۳۷۷ تا ۳۷۸)
- **تغییر دقیق**: Escape کردن `\n`، `\r`، `\t` و حذف کاراکترهای کنترلی کمتر از `0x20`.

### ۳.۲. رفع `SEC-012`: سفتسازی قوانین R8 در `app/proguard-rules.pro`
- **فایل هدف**: `app/proguard-rules.pro` (خطوط ۱۴ تا ۳۵)
- **تغییر دقیق**:
  1. افزودن قاعده حذف لاگهای دیباگ در نسخه Release:
     ```proguard
     -assumenosideeffects class android.util.Log {
         public static boolean isLoggable(java.lang.String, int);
         public static int v(...);
         public static int d(...);
     }
     ```
  2. محدود کردن `-keep class com.nexora.x.core.engine.**` صرفاً به اینترفیسها و مدلهای داده موردنیاز تا کلاسهای داخلی موتور مبهمسازی شوند.

### ۳.۳. رفع `SEC-010`: افزودن Rate Limiter و کاهش سقف JSON در `server.ts`
- **فایل هدف**: `server.ts` (خط ۱۵۶)
- **تغییر دقیق**: کاهش سقف پیش‌فرض `express.json` به `'512kb'` و افزودن محدودکننده نرخ درخواست (مثلاً حداکثر ۳۰ درخواست در دقیقه برای هر IP) روی `/api/network/*`.
