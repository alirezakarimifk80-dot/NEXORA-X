# Third-Party Licenses & GPL-3.0-or-later Obligations for NEXORA X

## 1. Upstream Engine Identification & Copyright

**Engine:** `sing-box` (`experimental/libbox` Android library via Go Mobile)  
**Pinned Version:** `v1.10.7`  
**Upstream Repository:** https://github.com/SagerNet/sing-box  
**Reference Android Client:** https://github.com/SagerNet/sing-box-for-android  
**Copyright:** Copyright (C) 2022–2026 nekohasekai (`<contact-sagernet@sekai.icu>`) and SagerNet contributors.  
**Upstream License:** **GNU General Public License v3.0 or later (`GPL-3.0-or-later`)**

---

## 2. GPL-3.0-or-later Copyleft Obligations for NEXORA X

Because `sing-box` and its native mobile library `libbox` (`io.nekohasekai.libbox` / `libbox.so`) are licensed under **GNU General Public License v3.0 or later (`GPL-3.0-or-later`)**, linking and bundling `libbox.aar` into the **NEXORA X** Android application creates a combined derivative work governed by the terms of **GPL-3.0-or-later**.

Accordingly:
1. **Application License:** NEXORA X is distributed as open-source software under **GPL-3.0-or-later** (not proprietary software).
2. **Source Code Availability Requirement:** Whenever you distribute a compiled NEXORA X APK containing `libbox.aar`, you **must** make the complete corresponding machine-readable source code of both NEXORA X and the exact `sing-box` / `libbox` version used available to recipients under GPL-3.0-or-later.
3. **Preservation of Notices:** All upstream copyright, license, and warranty disclaimers must be preserved intact in both source distributions and the in-app **Settings -> About -> Open-Source Licenses** section.
4. **No Tivoization / Installation Restrictions:** Recipients of the binary APK must retain the freedom to modify and reinstall modified versions of the software on their own devices (subject to standard user-controlled Android APK signing).

---

## 3. Additional Open-Source Libraries Used in NEXORA X

| Component / Library | License | Copyright / Upstream |
| :--- | :--- | :--- |
| **sing-box / libbox (`v1.10.7`)** | `GPL-3.0-or-later` | Copyright (C) 2022–2026 nekohasekai |
| **Android Jetpack Compose & Material 3** | `Apache-2.0` | Copyright (C) The Android Open Source Project |
| **AndroidX Room (`2.6.1`) & DataStore (`1.1.1`)** | `Apache-2.0` | Copyright (C) The Android Open Source Project |
| **Dagger Hilt (`2.52`)** | `Apache-2.0` | Copyright (C) Google Inc. |
| **Kotlin Standard Library & Coroutines (`2.0.21` / `1.9.0`)** | `Apache-2.0` | Copyright (C) JetBrains s.r.o. |

*(Note: Apache-2.0 is one-way compatible with GPLv3, permitting Apache-2.0 libraries to be combined with GPL-3.0-or-later code when the combined work is licensed under GPL-3.0-or-later.)*
