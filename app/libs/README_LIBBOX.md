# Building or Placing `libbox.aar` (sing-box v1.10.7)

## Why `libbox.aar` Is Required Here
Per the official `SagerNet/sing-box` and `SagerNet/sing-box-for-android` architecture, `io.nekohasekai.libbox` is **not** published as a standard Maven Central artifact. It is compiled from the `sing-box` Go source tree (`experimental/libbox`) using `gomobile` into an Android Archive (`libbox.aar`) containing JNI shared libraries (`libgojni.so`) for `arm64-v8a`, `armeabi-v7a`, `x86_64`, and `x86`.

`app/build.gradle.kts` automatically links any `.aar` placed in this `app/libs/` directory:
```kotlin
implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
```

## How to Build `libbox.aar` Locally (Pinned Version: `v1.10.7`)

1. Install **Go 1.22+**, **Android NDK (r26b+)**, and **JDK 17** on your workstation.
2. Clone `sing-box` at tag `v1.10.7` and run the official `libbox` Android build tool:
   ```bash
   git clone --branch v1.10.7 --depth 1 https://github.com/SagerNet/sing-box.git
   cd sing-box
   go run ./cmd/internal/build_libbox -target android -platform android/arm64,android/arm,android/amd64,android/386
   ```
3. Copy the generated `libbox.aar` into this directory:
   ```bash
   cp libbox.aar /path/to/NEXORA_X/app/libs/libbox.aar
   ```
4. Once `app/libs/libbox.aar` is present, `LibboxManager.isNativeLibboxLoaded()` automatically detects `io.nekohasekai.libbox.Libbox` at runtime and enables native TUN tunneling.
