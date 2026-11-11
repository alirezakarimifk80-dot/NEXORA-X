# NEXORA X (`com.nexora.x`) — Phase 1 Security Remediation Status

**Document Version:** 1.0 (Phase 1 — Batch 1 Completed)  
**Target Package:** `com.nexora.x`  
**VPN Core Engine:** `sing-box` (`io.nekohasekai.libbox` v1.10.7)  
**Remediation Protocol:** Strict Staged Batch Execution (**Batch 1 (`SEC-006`, `SEC-007`) Completed**; Batches 2–5 Pending User Approval)

---

## Executive Summary Table (`SEC-001` through `SEC-014`)

| ID | Batch | Severity | Title (`VULNERABILITY_REGISTER.md`) | Source Location | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`SEC-006`** | **Batch 1** | **HIGH** | Missing Automated SHA-256 Verification for Downloaded Binaries in CI/CD & `fetch-libbox-aar.sh` | `scripts/fetch-libbox-aar.sh`, `.github/workflows/android-apk.yml` | **FIXED** |
| **`SEC-007`** | **Batch 1** | **HIGH** | SSRF, Internal Port-Scanning & DNS Rebinding in `/api/network/ping` and `/api/network/tunnel-check` | `server.ts`, `src/utils/ssrfGuard.ts` | **FIXED** |
| **`SEC-001`** | Batch 2 | MEDIUM/HIGH | Fail-Open Plaintext Return on AES-GCM Encryption Error in `CredentialVaultCipher` | `app/src/main/java/com/nexora/x/core/security/CredentialVaultCipher.kt:78-81` | **NOT FIXED** *(Queued for Batch 2)* |
| **`SEC-002`** | Batch 2 | LOW/MEDIUM | Silent Swallowing of `SecurityException` on Tampered Ciphertext in `ProfileMapper` & Legacy Room Migration | `app/src/main/java/com/nexora/x/core/database/ProfileMapper.kt:32-40` | **NOT FIXED** *(Queued for Batch 2)* |
| **`SEC-003`** | Batch 3 | MEDIUM | `SNIHostName` Ordering Bug on IP Literals & Stage D TLS Skip in `TunnelConnectivityVerifier` | `app/src/main/java/com/nexora/x/core/engine/TunnelConnectivityVerifier.kt:162-173` | **NOT FIXED** *(Queued for Batch 3)* |
| **`SEC-004`** | Batch 3 | MEDIUM | Hardcoded IPv6 Address Instead of Reading `TunOptions.getInet6Address()` in `LibboxManager.openTun` | `app/src/main/java/com/nexora/x/core/engine/LibboxManager.kt:386-412` | **NOT FIXED** *(Queued for Batch 3)* |
| **`SEC-005`** | Batch 4 | MEDIUM | `singbox_work` Stored in `getExternalFilesDir` & Unredacted `stderr.log` Left on Disk | `app/src/main/java/com/nexora/x/core/engine/LibboxManager.kt:211, 221-223` | **NOT FIXED** *(Queued for Batch 4)* |
| **`SEC-008`** | Batch 4 | MEDIUM | Global In-Memory `sessionGitHubToken` Shared Across Requests in `server.ts` | `server.ts:14, 102, 249, 341, 392` | **NOT FIXED** *(Queued for Batch 4)* |
| **`SEC-009`** | Batch 4 | MEDIUM | Missing Anti-CSRF `state` Parameter in GitHub OAuth & Wildcard `'*'` `postMessage` Target Origin | `server.ts:273-299, 351-353` | **NOT FIXED** *(Queued for Batch 4)* |
| **`SEC-010`** | Batch 5 | LOW | Missing Rate Limiting on `/api/network/*` & Oversized `15mb` JSON Body Limit in `server.ts` | `server.ts:156` | **NOT FIXED** *(Queued for Batch 5)* |
| **`SEC-011`** | Batch 5 | LOW | Missing Control Character (`\n`, `\r`, `\t`) Escaping in `SingBoxConfigGenerator.esc()` | `app/src/main/java/com/nexora/x/core/engine/SingBoxConfigGenerator.kt:377-378` | **NOT FIXED** *(Queued for Batch 5)* |
| **`SEC-012`** | Batch 5 | LOW | Overly Broad `-keep` on `core.engine.**` & Missing `android.util.Log` Stripping in `proguard-rules.pro` | `app/proguard-rules.pro:14-18` | **NOT FIXED** *(Queued for Batch 5)* |
| **`SEC-013`** | Batch 5 | LOW | Frequent 10s Background TCP Ping Cadence in `NetworkLatencyChecker` | `app/src/main/java/com/nexora/x/core/engine/NetworkLatencyChecker.kt:71-75` | **NOT FIXED** *(Queued for Batch 5)* |
| **`SEC-014`** | Batch 5 | INFO / LOW | Unencrypted Server Metadata in SQLite `ProfileEntity.kt` & CI Keystore Cleanup Hardening | `ProfileEntity.kt:40-64`, `.github/workflows/android-apk.yml` | **PARTIALLY FIXED** *(CI Keystore `trap` added in Batch 1; Batch 5 pending)* |

---

## Detailed Remediation Records (`SEC-001` through `SEC-014`)

### `SEC-006` — Supply-Chain SHA-256 Integrity Verification (Batch 1)
- **Original finding:** `scripts/fetch-libbox-aar.sh` downloaded `SFA-1.10.7-universal.apk`, `libbox-gomobile-runtime-1.3.11.aar`, and `dex-tools-v2.4.zip` over HTTPS and printed `sha256sum` values for informational logging, but did not compare them against pinned expected SHA-256 digests (`sha256sum -c`). Additionally, `.github/workflows/android-apk.yml` downloaded `commandlinetools-linux-11076708_latest.zip` without checksum verification and had an `else` fallback branch that downloaded an unverified AAR if `scripts/fetch-libbox-aar.sh` was missing.
- **Root cause verified in source:**
  - Verified in `/scripts/fetch-libbox-aar.sh` (lines 28–85 prior to fix): `curl -fL` wrote archives to `/tmp` and extracted them without any hash comparison.
  - Verified in `/.github/workflows/android-apk.yml` (lines 77–80 and 118–138 prior to fix): `cmdtools.zip` was extracted without SHA-1 validation, and Step 4 contained an unverified fallback `curl` command.
- **Files changed:**
  - `/scripts/fetch-libbox-aar.sh`
  - `/.github/workflows/android-apk.yml`
  - `/scripts/test-batch1-security.ts`
- **Fix implemented:**
  - Independently downloaded all upstream artifacts, verified release metadata against the GitHub Releases API (`https://api.github.com/repos/SagerNet/sing-box/releases/tags/v1.10.7`, asset ID `196452618`, size `42,829,973` bytes) and Google's official Android SDK repository manifest (`https://dl.google.com/android/repository/repository2-3.xml`), and pinned the real cryptographic hashes in `/scripts/fetch-libbox-aar.sh`:
    - `SFA-1.10.7-universal.apk`: `bd1ee2fbcbe93551bfb0a7ef64c97e75b7cd265ba6429952f8fde6058a6f4115`
    - `libbox-gomobile-runtime-1.3.11.aar`: `87ef0a9cc714abdd1b2b73adb84e2f09e96c7a343263a62c0b1b8cc1c0fa0715`
    - `dex-tools-v2.4.zip`: `ee7c45eb3c1d2474a6145d8d447e651a736a22d9664b6d3d3be5a5a817dda23a`
    - `SFA-1.10.7 classes.dex`: `e6e3b9df2127d86d257cebc374762a90f78714d8996f1403a1a83e402de01afc`
    - `lib/arm64-v8a/libbox.so`: `47827f8fca6b02f17501125f1ccb12df597302737cd5abf982bd84c0a10761f2`
    - `lib/armeabi-v7a/libbox.so`: `b224b08c1992c2cde1bfb78b3fdcb0156761e0bb4d927a737613fdc4123e52ae`
    - `lib/x86_64/libbox.so`: `e97b264b5d62640ba9de258e5e6a6a319bf23d221797238db3223c3e8c2dc4ce`
    - `lib/x86/libbox.so`: `d36ad72fc5d4074348e755961c5d5116ad3e0753d9c7ad71a05125724a20d873`
  - Added fail-closed `verify_sha256()` function in `/scripts/fetch-libbox-aar.sh` that deletes the target file (`rm -f`) and exits with code `1` if the file is missing, empty, or its SHA-256 digest does not match the pinned hash.
  - Added `--self-test` mode to `/scripts/fetch-libbox-aar.sh` to regression-test both valid and tampered payloads.
  - Updated `/.github/workflows/android-apk.yml` to verify `commandlinetools-linux-11076708_latest.zip` against Google's official SHA-1 (`d313adb7aedccf6cf0cfca51ec180f0059f5f8f8`), removed the unverified fallback download in Step 4, and enforced `./scripts/fetch-libbox-aar.sh --self-test` before building `app/libs/libbox.aar`.
- **Test added:**
  - `bash scripts/fetch-libbox-aar.sh --self-test`
  - `/scripts/test-batch1-security.ts` (Part 1: `SEC-006` assertions).
- **Actual test result:**
  - `PASS` (`[libbox-aar] SEC-006 SELF-TEST PASSED: Fail-closed SHA-256 verification & pinned digests verified.` — 3/3 assertions passed in `npx tsx scripts/test-batch1-security.ts`).
- **Remaining risk:**
  - None for sing-box `v1.10.7`. Future version upgrades require updating the pinned SHA-256 constants in `scripts/fetch-libbox-aar.sh`.
- **Status:** **`FIXED`**

---

### `SEC-007` — SSRF & DNS Rebinding Protection on Server Endpoints (Batch 1)
- **Original finding:** `POST /api/network/ping` and `POST /api/network/tunnel-check` in `server.ts` accepted arbitrary `host` and `port` inputs and opened TCP connections (`socket.connect(port, rawHost)`) without blocking loopback (`127.0.0.0/8`, `::1`), RFC 1918 private networks (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`), link-local / cloud metadata (`169.254.0.0/16`, `fe80::/10`), IPv6 ULA (`fc00::/7`), IPv4-mapped IPv6 (`::ffff:0:0/96`), or DNS rebinding attacks.
- **Root cause verified in source:**
  - Verified in `/server.ts` lines 836–892 (`/api/network/ping`) and lines 895–1028 (`/api/network/tunnel-check`) prior to fix: `socket.connect(port, rawHost)` was called directly on `req.body.host`, and Stage F `fetch` probes did not disable HTTP redirects.
- **Files changed:**
  - `/src/utils/ssrfGuard.ts` (created)
  - `/server.ts` (hardened `/api/network/ping` and `/api/network/tunnel-check`)
  - `/scripts/test-batch1-security.ts` (created)
- **Fix implemented:**
  - Created `/src/utils/ssrfGuard.ts` with:
    - `validateHostSyntax()`: rejects internal hostnames (`localhost`, `metadata.google.internal`, `.local`, `.internal`, `.lan`, `.corp`), URL characters, and non-canonical numeric/octal/hex IPv4 representations (`0177.0.0.1`, `0x7f.0.0.1`, `2130706433`).
    - `getIpv4ProhibitedReason()` & `getIpv6ProhibitedReason()`: blocks all non-public IPv4 CIDRs (`0.0.0.0/8`, `10.0.0.0/8`, `100.64.0.0/10`, `127.0.0.0/8`, `169.254.0.0/16`, `172.16.0.0/12`, `192.0.0.0/24`, `192.0.2.0/24`, `192.88.99.0/24`, `192.168.0.0/16`, `198.18.0.0/15`, `198.51.100.0/24`, `203.0.113.0/24`, `224.0.0.0/4`, `240.0.0.0/4`) and all non-public IPv6 ranges (`::/128`, `::1/128`, `::ffff:0:0/96` IPv4-mapped, `64:ff9b::/96` NAT64, `2002::/16` 6to4, `2001::/32` Teredo, `2001:db8::/32`, `fc00::/7` ULA, `fe80::/10` link-local, `fec0::/10`, `ff00::/8` multicast).
    - `resolveAndValidatePublicTarget()`: resolves all `A`/`AAAA` records via `dns.promises.lookup(hostname, { all: true, verbatim: true })`, fails closed if *any* returned record is non-public, and returns the pinned `{ resolvedIp, family }`.
  - Updated `/api/network/ping` and `/api/network/tunnel-check` in `/server.ts` to validate targets with `resolveAndValidatePublicTarget(rawHost)` (returning HTTP `403` on prohibited targets) and connect `net.Socket` directly to `{ host: resolvedIp, port, family }` to eliminate DNS rebinding.
  - Updated Stage F in `/api/network/tunnel-check` to use `redirect: 'manual'` and reject HTTP `3xx` redirects.
- **Test added:**
  - `/scripts/test-batch1-security.ts` (Part 2: 45 unit & DNS-rebinding assertions; Part 3: 7 live Express endpoint assertions against `http://127.0.0.1:3000/api/network/ping` and `/api/network/tunnel-check`).
- **Actual test result:**
  - `PASS` (52/52 `SEC-007` assertions in `npx tsx scripts/test-batch1-security.ts` passed; all 7 live SSRF POST requests returned `HTTP 403` with `SSRF Protection:` error reason).
- **Remaining risk:**
  - Public routable IP addresses on arbitrary ports remain reachable by design so users can test TCP/TLS reachability of their own public VPN servers.
- **Status:** **`FIXED`**

---

### `SEC-001` — Fail-Open Plaintext Return on AES-GCM Encryption Error in `CredentialVaultCipher` (Batch 2)
- **Original finding:** `CredentialVaultCipher.encryptForStorage(plaintext)` catches `Throwable` and returns raw `plaintext` if Keystore initialization or AES-GCM encryption fails.
- **Root cause verified in source:** Verified in `app/src/main/java/com/nexora/x/core/security/CredentialVaultCipher.kt` lines 78–81.
- **Files changed:** None yet (scheduled for Batch 2).
- **Fix implemented:** Pending Batch 2 approval.
- **Test added:** Pending Batch 2.
- **Actual test result:** Pending Batch 2.
- **Remaining risk:** Medium/High until Batch 2 replaces silent plaintext fallback with fail-closed key recovery / exception propagation.
- **Status:** **`NOT FIXED`** *(Queued for Batch 2)*

---

### `SEC-002` — Silent Swallowing of `SecurityException` in `ProfileMapper` & Legacy Plaintext Migration (Batch 2)
- **Original finding:** `ProfileMapper.toDomain` wraps `CredentialVaultCipher.decryptFromStorage` in `runCatching { ... }.getOrDefault("")`, silently swallowing `SecurityException` on tampered ciphertext, and legacy plaintext rows in Room are not automatically re-encrypted into `ENCv1:` format.
- **Root cause verified in source:** Verified in `app/src/main/java/com/nexora/x/core/database/ProfileMapper.kt` lines 32–40 and `ProfileRepository.kt`.
- **Files changed:** None yet (scheduled for Batch 2).
- **Fix implemented:** Pending Batch 2 approval.
- **Test added:** Pending Batch 2.
- **Actual test result:** Pending Batch 2.
- **Remaining risk:** Low/Medium until Batch 2 remediation.
- **Status:** **`NOT FIXED`** *(Queued for Batch 2)*

---

### `SEC-003` — `SNIHostName` Ordering Bug on IP Literals & Stage D TLS Skip in `TunnelConnectivityVerifier` (Batch 3)
- **Original finding:** `TunnelConnectivityVerifier.kt` sets `params.endpointIdentificationAlgorithm = "HTTPS"` inside the same `runCatching` block as `SNIHostName(sniHost)`, which throws `IllegalArgumentException` when `sniHost` is an IP literal before `sslSocket.sslParameters = params` is called; also skips TLS handshake test in Stage D when `allowInsecure` is true.
- **Root cause verified in source:** Verified in `app/src/main/java/com/nexora/x/core/engine/TunnelConnectivityVerifier.kt` lines 162–173.
- **Files changed:** None yet (scheduled for Batch 3).
- **Fix implemented:** Pending Batch 3 approval.
- **Test added:** Pending Batch 3.
- **Actual test result:** Pending Batch 3.
- **Remaining risk:** Medium until Batch 3 remediation.
- **Status:** **`NOT FIXED`** *(Queued for Batch 3)*

---

### `SEC-004` — Hardcoded IPv6 Address Instead of Reading `TunOptions.getInet6Address()` in `LibboxManager.openTun` (Batch 3)
- **Original finding:** `LibboxManager.openTun` only reads `options?.getInet4Address()` and relies on a hardcoded `fdfe:dcba:9876::1/126` address in `NexoraVpnService.kt`.
- **Root cause verified in source:** Verified in `app/src/main/java/com/nexora/x/core/engine/LibboxManager.kt` lines 386–412 and `NexoraVpnService.kt` lines 118–141.
- **Files changed:** None yet (scheduled for Batch 3).
- **Fix implemented:** Pending Batch 3 approval.
- **Test added:** Pending Batch 3.
- **Actual test result:** Pending Batch 3.
- **Remaining risk:** Medium until Batch 3 remediation.
- **Status:** **`NOT FIXED`** *(Queued for Batch 3)*

---

### `SEC-005` — `singbox_work` Stored in `getExternalFilesDir` & Unredacted `stderr.log` Left on Disk (Batch 4)
- **Original finding:** `LibboxManager.kt` creates `singbox_work` inside `appContext.getExternalFilesDir(null)` and redirects Go stderr to `stderr.log` without redaction or cleanup on disconnect.
- **Root cause verified in source:** Verified in `app/src/main/java/com/nexora/x/core/engine/LibboxManager.kt` lines 211 and 221–223.
- **Files changed:** None yet (scheduled for Batch 4).
- **Fix implemented:** Pending Batch 4 approval.
- **Test added:** Pending Batch 4.
- **Actual test result:** Pending Batch 4.
- **Remaining risk:** Medium until Batch 4 moves `singbox_work` exclusively to `appContext.filesDir` and scrubs/deletes `stderr.log` on stop.
- **Status:** **`NOT FIXED`** *(Queued for Batch 4)*

---

### `SEC-008` — Global In-Memory `sessionGitHubToken` Shared Across Requests in `server.ts` (Batch 4)
- **Original finding:** `server.ts` stores `sessionGitHubToken` in a module-scoped variable returned by `resolveGitHubToken(req)` even when a request has no auth header or session cookie. Also audited `PendingIntent.FLAG_IMMUTABLE` in `NexoraVpnService.kt` (already compliant).
- **Root cause verified in source:** Verified in `server.ts` lines 14, 102, 249, 341, 392.
- **Files changed:** None yet (scheduled for Batch 4).
- **Fix implemented:** Pending Batch 4 approval.
- **Test added:** Pending Batch 4.
- **Actual test result:** Pending Batch 4.
- **Remaining risk:** Medium until Batch 4 binds tokens strictly to per-session HttpOnly cookies or request headers.
- **Status:** **`NOT FIXED`** *(Queued for Batch 4)*

---

### `SEC-009` — Missing Anti-CSRF `state` Parameter in GitHub OAuth & Service Intent Hardening (Batch 4)
- **Original finding:** `/api/auth/github/url` and `/auth/callback` in `server.ts` do not generate or verify a cryptographic `state` parameter and use `'*'` as the `postMessage` targetOrigin.
- **Root cause verified in source:** Verified in `server.ts` lines 273–299 and 351–353.
- **Files changed:** None yet (scheduled for Batch 4).
- **Fix implemented:** Pending Batch 4 approval.
- **Test added:** Pending Batch 4.
- **Actual test result:** Pending Batch 4.
- **Remaining risk:** Medium until Batch 4 remediation.
- **Status:** **`NOT FIXED`** *(Queued for Batch 4)*

---

### `SEC-010` — Missing Rate Limiting on `/api/network/*` & Oversized `15mb` JSON Body Limit (Batch 5)
- **Original finding:** `server.ts` applies `express.json({ limit: '15mb' })` globally and lacks rate limiting on `/api/network/*`.
- **Root cause verified in source:** Verified in `server.ts` line 156.
- **Files changed:** None yet (scheduled for Batch 5).
- **Fix implemented:** Pending Batch 5 approval.
- **Test added:** Pending Batch 5.
- **Actual test result:** Pending Batch 5.
- **Remaining risk:** Low until Batch 5 remediation.
- **Status:** **`NOT FIXED`** *(Queued for Batch 5)*

---

### `SEC-011` — Missing Control Character (`\n`, `\r`, `\t`) Escaping in `SingBoxConfigGenerator.esc()` (Batch 5)
- **Original finding:** `SingBoxConfigGenerator.esc(s: String)` only escapes `\` and `"` and does not escape `\n`, `\r`, `\t`, or strip control characters `< 0x20`.
- **Root cause verified in source:** Verified in `app/src/main/java/com/nexora/x/core/engine/SingBoxConfigGenerator.kt` lines 377–378.
- **Files changed:** None yet (scheduled for Batch 5).
- **Fix implemented:** Pending Batch 5 approval.
- **Test added:** Pending Batch 5.
- **Actual test result:** Pending Batch 5.
- **Remaining risk:** Low until Batch 5 remediation.
- **Status:** **`NOT FIXED`** *(Queued for Batch 5)*

---

### `SEC-012` — Overly Broad `-keep` on `core.engine.**` & Missing `android.util.Log` Stripping in `proguard-rules.pro` (Batch 5)
- **Original finding:** `app/proguard-rules.pro` keeps all classes in `com.nexora.x.core.engine.**` and lacks `-assumenosideeffects class android.util.Log` for debug/verbose log stripping.
- **Root cause verified in source:** Verified in `app/proguard-rules.pro` lines 14–18.
- **Files changed:** None yet (scheduled for Batch 5).
- **Fix implemented:** Pending Batch 5 approval.
- **Test added:** Pending Batch 5.
- **Actual test result:** Pending Batch 5.
- **Remaining risk:** Low until Batch 5 remediation.
- **Status:** **`NOT FIXED`** *(Queued for Batch 5)*

---

### `SEC-013` — Background TCP Ping Cadence & `FLAG_SECURE` Hardening (Batch 5)
- **Original finding:** `NetworkLatencyChecker` / `HomeViewModel` runs TCP pings every 10 seconds while on the home screen.
- **Root cause verified in source:** Verified in `app/src/main/java/com/nexora/x/core/engine/NetworkLatencyChecker.kt` lines 71–75.
- **Files changed:** None yet (scheduled for Batch 5).
- **Fix implemented:** Pending Batch 5 approval.
- **Test added:** Pending Batch 5.
- **Actual test result:** Pending Batch 5.
- **Remaining risk:** Low until Batch 5 remediation.
- **Status:** **`NOT FIXED`** *(Queued for Batch 5)*

---

### `SEC-014` — Unencrypted Server Metadata in SQLite & CI Keystore Cleanup Hardening (Batch 5)
- **Original finding:** `ProfileEntity.kt` stores `server_address`, `port`, and `sni` in unencrypted columns for SQLite indexing while encrypting `credential`, `realityPublicKey`, and `realityShortId`; and `.github/workflows/android-apk.yml` Step 9b needed a `trap` to guarantee `/tmp/nexora-upload.jks` deletion on build failure.
- **Root cause verified in source:** Verified in `ProfileEntity.kt` lines 40–64 and `.github/workflows/android-apk.yml` lines 211–228.
- **Files changed:** `/.github/workflows/android-apk.yml` (added `trap 'rm -f /tmp/nexora-upload.jks' EXIT` in Batch 1).
- **Fix implemented:** Added shell `trap` in `.github/workflows/android-apk.yml` Step 9b; remaining `SEC-014` review scheduled for Batch 5.
- **Test added:** Queued for Batch 5.
- **Actual test result:** Verified in `.github/workflows/android-apk.yml`.
- **Remaining risk:** Informational / Low.
- **Status:** **`PARTIALLY FIXED`**
