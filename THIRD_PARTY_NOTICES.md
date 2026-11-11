# Third-Party Open-Source Software Notices — NEXORA X (`com.nexora.x`)

This document attributes all third-party open-source software libraries integrated into **NEXORA X** (`com.nexora.x`) and documents license compliance for public distribution on Google Play and GitHub.

---

## 1. Core VPN Engine (`GPL-3.0-or-later` Copyleft Notice)

### `sing-box` / `libbox` (`io.nekohasekai.libbox`)
- **Upstream Project:** [SagerNet/sing-box](https://github.com/SagerNet/sing-box) & [SagerNet/sing-box-for-android](https://github.com/SagerNet/sing-box-for-android)
- **Pinned Version:** `v1.10.7` (`app/libs/libbox.aar`, reproducible assembly via `scripts/fetch-libbox-aar.sh`)
- **Copyright:** Copyright (C) 2022–2024 nekohasekai (`<contact@sagernet.org>`) and `sing-box` contributors
- **License:** **GNU General Public License v3.0 or later (`GPL-3.0-or-later`)**
- **Source Code & Build Reproducibility Compliance:**
  - Because `sing-box` (`libbox v1.10.7`) is licensed under `GPL-3.0-or-later` and linked into the `com.nexora.x` Android application, any public binary distribution of NEXORA X (including on Google Play or GitHub Releases) must be accompanied by the complete corresponding source code (`app/`, `build.gradle.kts`, `settings.gradle.kts`, `scripts/fetch-libbox-aar.sh`) under a `GPL-3.0-or-later`-compatible license, or provide a written offer / public repository link where users can obtain the complete corresponding source code.
  - The upstream `sing-box v1.10.7` source code is publicly available at: `https://github.com/SagerNet/sing-box/tree/v1.10.7`.

### `gVisor` (Embedded User-Space Network Stack inside `sing-box`)
- **Upstream Project:** [google/gvisor](https://github.com/google/gvisor)
- **Copyright:** Copyright 2018 The gVisor Authors
- **License:** Apache License, Version 2.0 (`Apache-2.0`)

### Go Mobile (`gomobile` / `go.Seq` Runtime Bridge)
- **Upstream Project:** [golang.org/x/mobile](https://pkg.go.dev/golang.org/x/mobile)
- **Copyright:** Copyright (c) 2014 The Go Authors. All rights reserved.
- **License:** BSD 3-Clause License (`BSD-3-Clause`)

---

## 2. Android Jetpack, Kotlin & Dependency Injection Libraries (`Apache-2.0`)

All libraries listed below are licensed under the **Apache License, Version 2.0 (`Apache-2.0`)**:

1. **AndroidX Core, Activity & Lifecycle (`androidx.core:core-ktx`, `androidx.activity:activity-compose`, `androidx.lifecycle:*`)**
   - Copyright (C) The Android Open Source Project — Licensed under `Apache-2.0`.
2. **AndroidX Jetpack Compose & Material 3 (`androidx.compose.ui:*`, `androidx.compose.material3:material3`, `androidx.navigation:navigation-compose`)**
   - Copyright (C) The Android Open Source Project — Licensed under `Apache-2.0`.
3. **AndroidX Room SQLite Persistence (`androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler`)**
   - Copyright (C) The Android Open Source Project — Licensed under `Apache-2.0`.
4. **AndroidX Jetpack DataStore Preferences (`androidx.datastore:datastore-preferences`)**
   - Copyright (C) The Android Open Source Project — Licensed under `Apache-2.0`.
5. **Dagger Hilt Dependency Injection (`com.google.dagger:hilt-android`, `androidx.hilt:hilt-navigation-compose`)**
   - Copyright (C) The Dagger Authors — Licensed under `Apache-2.0`.
6. **Kotlin Standard Library & Kotlinx Coroutines (`org.jetbrains.kotlin:*`, `org.jetbrains.kotlinx:kotlinx-coroutines-android`)**
   - Copyright (C) 2010–2024 JetBrains s.r.o. and Kotlin Programming Language contributors — Licensed under `Apache-2.0`.
7. **JUnit 4 (`junit:junit`, Test-Only Dependency)**
   - Copyright (C) 2002–2021 JUnit — Licensed under Eclipse Public License 1.0 (`EPL-1.0`).

---

## 3. License Summary Text

### Apache License 2.0 Summary
Licensed under the Apache License, Version 2.0 (the "License"); you may not use these files except in compliance with the License. You may obtain a copy of the License at:
`http://www.apache.org/licenses/LICENSE-2.0`

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

### GNU General Public License v3.0 Summary (`sing-box` / `libbox`)
This program integrates `sing-box` (`libbox v1.10.7`), which is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. You may obtain a copy of the GNU General Public License v3.0 at:
`https://www.gnu.org/licenses/gpl-3.0.html`
