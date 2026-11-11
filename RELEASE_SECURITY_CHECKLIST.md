# NEXORA X — چکلیست امنیتی پیش از انتشار عمومی (Release Security Checklist)

این چکلیست بر اساس استاندارد **OWASP MASVS v2.1.0** و الزامات امنیتی **Google Play VpnService Policy** برای انتشار نسخه عمومی **NEXORA X (`com.nexora.x` v1.0.0)** تدوین شده است.

---

## ۱. امنیت ذخیرهسازی و رمزنگاری (`MASVS-STORAGE` & `MASVS-CRYPTO`)
- [x] **رمزنگاری اطلاعات حساس در دیتابیس**: فیلدهای `credential`، `realityPublicKey` و `realityShortId` پیش از ذخیره در Room SQLite توسط `CredentialVaultCipher` با الگوریتم `AES/GCM/NoPadding` (کلید ۲۵۶ بیتی در `AndroidKeyStore` و IV تصادفی ۱۲ بایتی) رمزنگاری میشوند.
- [ ] **حذف بازگشت خام در خطای Keystore (`SEC-001`)**: باید بلوک `catch` در `CredentialVaultCipher.encryptForStorage` اصلاح شود تا در صورت بروز خطای Keystore، هیچگاه رشته خام (`plaintext`) برگردانده نشود.
- [x] **سانسور متد `toString()` در Entity**: متد `ProfileEntity.toString()` مقادیر حساس را با `[REDACTED]` جایگزین میکند.
- [x] **عدم ذخیره اطلاعات حساس در DataStore**: در `NexoraPreferencesDataSource.kt` صرفاً تم برنامه، شناسه پروفایل انتخابی و وضعیت تأیید دیالوگ افشای VPN ذخیره میشود.
- [x] **غیرفعالسازی Backup ابری و استخراج داده**: در `AndroidManifest.xml` ویژگی `android:allowBackup="false"` تنظیم شده و فایلهای `backup_rules.xml` و `data_extraction_rules.xml` دیتابیس `nexora_profiles.db` را مستثنی کردهاند.
- [ ] **انتقال `workDir` از حافظه خارجی به حافظه داخلی (`SEC-005`)**: در `LibboxManager.kt` خط ۲۱۱ باید `getExternalFilesDir(null)` حذف شده و صرفاً از `appContext.filesDir` استفاده شود و فایل `stderr.log` هنگام قطع اتصال پاکسازی گردد.

---

## ۲. امنیت شبکه، تونل VPN و `sing-box` (`MASVS-NETWORK`)
- [x] **حفاظت از سوکتهای خروجی (`VpnService.protect`)**: در `NexoraVpnService.kt` و `LibboxManager.kt` تمامی سوکتهای خروجی هسته `sing-box` با `protect(fd)` محافظت میشوند و در صورت شکست `protect`، خطا پرتاب میشود تا حلقه مسیردهی رخ ندهد.
- [x] **حذف پکیج خود برنامه از TUN**: در `NexoraVpnService.kt` خط ۱۴۹ پکیج برنامه با `builder.addDisallowedApplication(packageName)` از تونل خارج شده است.
- [x] **مدیریت صحیح File Descriptor رابط TUN**: استفاده از `ParcelFileDescriptor.dup(...).detachFd()` در `NexoraVpnService.kt` خط ۱۶۶ از بسته شدن مضاعف (Double-Close) بین کاتلین و هسته Go جلوگیری میکند.
- [x] **پیکربندی امن DNS و جلوگیری از DNS Leak**: استفاده از DoH (`https://1.1.1.1/dns-query` از طریق `"proxy"`) بههمراه قانون Bootstrap (`"outbound": "any" -> "dns-direct"`) و رهگیری پورت ۵۳ (`"protocol": "dns" -> "dns-out"`).
- [x] **پوشش مسیر پیشفرض IPv4 و IPv6**: هم مسیر `0.0.0.0/0` (`172.19.0.1/30`) و هم مسیر `::/0` (`fdfe:dcba:9876::1/126`) در `NexoraVpnService.kt` و `SingBoxConfigGenerator.kt` تعریف شدهاند.
- [x] **مسدودسازی ترافیک Cleartext**: در `network_security_config.xml` ویژگی `cleartextTrafficPermitted="false"` بهصورت پایه فعال است (بهجز دامنه تست استاندارد `cp.cloudflare.com` برای بررسی HTTP 204).
- [x] **هشدار صریح MITM برای `allowInsecure`**: در صورت فعال کردن `allowInsecure`، هشدار امنیتی هم در رابط کاربری (`AddProfileScreen.kt:502`) و هم در خروجی `SingBoxConfigGenerator` نمایش داده میشود.
- [ ] **جداسازی تنظیم `endpointIdentificationAlgorithm` از `SNIHostName` در Stage D (`SEC-003`)**: در `TunnelConnectivityVerifier.kt` خط ۱۶۸ باید الگوریتم `HTTPS` حتی در صورت IP بودن `sniHost` اعمال شود.
- [ ] **Escape کردن کاراکترهای کنترلی در JSON (`SEC-011`)**: تکمیل متد `esc()` در `SingBoxConfigGenerator.kt` برای `\n`, `\r`, `\t`.

---

## ۳. امنیت پلتفرم اندروید و لاگها (`MASVS-PLATFORM` & `MASVS-RESILIENCE`)
- [x] **عدم وجود لاگ خام در کد**: هیچ فراخوانی به `Log.d`, `Log.e`, `println` یا `printStackTrace` در کدهای کاتلین وجود ندارد و تمام رویدادهای تشخیصی از `LogRedactor.redact` عبور میکنند.
- [x] **محافظت از سرویس VPN (`android:exported="false"`)**: سرویس `NexoraVpnService` غیر-exported بوده و با مجوز سیستمی `android.permission.BIND_VPN_SERVICE` قفل شده است.
- [x] **تنظیم `PendingIntent.FLAG_IMMUTABLE`**: تمام `PendingIntent`های اعلان در `VpnNotificationManager.kt` دارای فلگ `FLAG_IMMUTABLE` هستند.
- [x] **مخفیسازی لینکهای حساس در کلیپبورد اندروید ۱۳+**: در `ServersScreen.kt` خط ۳۳۶ فلگ `android.content.extra.IS_SENSITIVE = true` روی کلیپبورد اعمال شده است.
- [x] **حفاظت از اعلان در صفحه قفل (Lock Screen)**: در `VpnNotificationManager.kt` از `VISIBILITY_PRIVATE` و نسخه عمومی سانسورشده (`setPublicVersion`) استفاده شده است.
- [ ] **بهبود قواعد مبهمسازی R8 (`SEC-012`)**: محدود کردن `-keep class com.nexora.x.core.engine.**` و افزودن `-assumenosideeffects class android.util.Log` در `app/proguard-rules.pro`.

---

## ۴. امنیت زنجیره تأمین، CI/CD و سرور همراه
- [x] **عدم ذخیره Keystore در مخزن کد**: هیچ فایل `.jks` یا `.keystore` در مخزن وجود ندارد و امضای نسخه Release صرفاً از طریق متغیرهای محیطی GitHub Secrets انجام میشود.
- [x] **حداقل سطح دسترسی در GitHub Actions**: فایل `.github/workflows/android-apk.yml` دارای `permissions: contents: read` است.
- [ ] **اعتبارسنجی خودکار SHA-256 برای فایلهای دانلودی `libbox` (`SEC-006`)**: افزودن بررسی `sha256sum -c` برای فایلهای دانلودشده در `scripts/fetch-libbox-aar.sh` و `android-apk.yml`.
- [ ] **حذف فایل موقت `/tmp/nexora-upload.jks` با `trap EXIT` در CI**: تضمین پاکسازی Keystore موقت حتی در صورت شکست بیلد Gradle.
- [ ] **بستن امکان پورت‌اسکن داخلی در `server.ts` (`SEC-007`)**: مسدودسازی IPهای خصوصی و Loopback در مسیرهای `/api/network/ping` و `/api/network/tunnel-check` سرور وب همراه.

---

## ۵. تستهای نهایی روی دستگاه واقعی پیش از انتشار در Google Play
- [ ] نصب فایل APK/AAB ساخته‌شده در GitHub Actions روی دستگاه فیزیکی اندروید (حداقل یک دستگاه با اندروید ۱۰ تا ۱۵).
- [ ] بررسی برقراری تونل واقعی با هر ۴ پروتکل (`VLESS Reality`, `VMess WS`, `Trojan TLS`, `Shadowsocks AEAD`) و مشاهده وضعیت `INTERNET AVAILABLE` (گذر از مراحل Stage A تا Stage F).
- [ ] تست تغییر شبکه از Wi-Fi به Mobile Data در حین اتصال VPN و اطمینان از عدم نشت DNS یا IPv6.
- [ ] تست دکمه `Stop VPN` از داخل اعلان سیستم و بررسی بسته شدن کامل رابط `tun0`.
