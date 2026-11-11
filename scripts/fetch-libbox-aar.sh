#!/usr/bin/env bash
# ==============================================================================
# NEXORA X — Reproducible Official sing-box (libbox v1.10.7) AAR Builder
# ==============================================================================
# Official singbox-android/libbox repository only publishes tags 1.3.11 and 1.11.15+.
# To strictly preserve compatibility with sing-box v1.10.7 without inventing URLs
# or silently upgrading versions, this script reproducibly assembles app/libs/libbox.aar
# directly from the official SagerNet/sing-box v1.10.7 release assets:
#   - https://github.com/SagerNet/sing-box/releases/download/v1.10.7/SFA-1.10.7-universal.apk
#     (provides the official v1.10.7 io.nekohasekai.libbox.* bytecode and
#      lib/{arm64-v8a,armeabi-v7a,x86_64,x86}/libbox.so native binaries)
#   - https://raw.githubusercontent.com/singbox-android/libbox/1.3.11/libbox.aar
#     (provides the clean, un-obfuscated gomobile go.Seq / go.Universe runtime classes)
# ==============================================================================
set -euo pipefail

SINGBOX_VERSION="1.10.7"
SFA_RELEASE_URL="https://github.com/SagerNet/sing-box/releases/download/v${SINGBOX_VERSION}/SFA-${SINGBOX_VERSION}-universal.apk"
GOMOBILE_RUNTIME_AAR_URL="https://raw.githubusercontent.com/singbox-android/libbox/1.3.11/libbox.aar"
DEX2JAR_URL="https://github.com/pxb1988/dex2jar/releases/download/v2.4/dex-tools-v2.4.zip"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/app/libs"
OUTPUT_AAR="${OUTPUT_DIR}/libbox.aar"
WORK_DIR="$(mktemp -d /tmp/libbox-aar-build.XXXXXX)"

cleanup() {
  rm -rf "${WORK_DIR}"
}
trap cleanup EXIT

mkdir -p "${OUTPUT_DIR}"

echo "[libbox-aar] Preparing official sing-box v${SINGBOX_VERSION} AAR in ${OUTPUT_AAR}..."

SFA_APK_CACHE="/tmp/sfa-${SINGBOX_VERSION}-universal.apk"
if [ ! -s "${SFA_APK_CACHE}" ]; then
  echo "[libbox-aar] Downloading official SagerNet/sing-box v${SINGBOX_VERSION} universal APK..."
  curl -fL --retry 3 "${SFA_RELEASE_URL}" -o "${SFA_APK_CACHE}"
fi

GOMOBILE_AAR_CACHE="/tmp/libbox-gomobile-runtime-1.3.11.aar"
if [ ! -s "${GOMOBILE_AAR_CACHE}" ]; then
  echo "[libbox-aar] Downloading clean gomobile go.Seq runtime classes from singbox-android/libbox..."
  curl -fL --retry 3 "${GOMOBILE_RUNTIME_AAR_URL}" -o "${GOMOBILE_AAR_CACHE}"
fi

DEX2JAR_DIR="/tmp/dex-tools-v2.4"
if [ ! -x "${DEX2JAR_DIR}/d2j-dex2jar.sh" ]; then
  echo "[libbox-aar] Downloading dex-tools-v2.4..."
  curl -fL --retry 3 "${DEX2JAR_URL}" -o "/tmp/dex2jar.zip"
  unzip -q -o "/tmp/dex2jar.zip" -d "/tmp"
  chmod +x "${DEX2JAR_DIR}"/*.sh
  rm -f "/tmp/dex2jar.zip"
fi

echo "[libbox-aar] Extracting classes.dex and native libbox.so libraries from SFA-${SINGBOX_VERSION}-universal.apk..."
unzip -q "${SFA_APK_CACHE}" "classes.dex" "lib/*/libbox.so" -d "${WORK_DIR}/sfa"

echo "[libbox-aar] Converting classes.dex to Java bytecode..."
"${DEX2JAR_DIR}/d2j-dex2jar.sh" --force -o "${WORK_DIR}/sfa-all-classes.jar" "${WORK_DIR}/sfa/classes.dex" >/dev/null

mkdir -p "${WORK_DIR}/classes"
(
  cd "${WORK_DIR}/classes"
  jar xf "${WORK_DIR}/sfa-all-classes.jar" io/nekohasekai/libbox
)

unzip -p "${GOMOBILE_AAR_CACHE}" "classes.jar" > "${WORK_DIR}/gomobile-clean.jar"
(
  cd "${WORK_DIR}/classes"
  jar xf "${WORK_DIR}/gomobile-clean.jar" go
)

mkdir -p "${WORK_DIR}/aar/jni"
jar cf "${WORK_DIR}/aar/classes.jar" -C "${WORK_DIR}/classes" io -C "${WORK_DIR}/classes" go

for ABI in arm64-v8a armeabi-v7a x86_64 x86; do
  if [ -f "${WORK_DIR}/sfa/lib/${ABI}/libbox.so" ]; then
    mkdir -p "${WORK_DIR}/aar/jni/${ABI}"
    cp "${WORK_DIR}/sfa/lib/${ABI}/libbox.so" "${WORK_DIR}/aar/jni/${ABI}/libbox.so"
  else
    echo "[libbox-aar] ERROR: Missing native library lib/${ABI}/libbox.so in official release!" >&2
    exit 1
  fi
done

cat <<'EOF' > "${WORK_DIR}/aar/AndroidManifest.xml"
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="io.nekohasekai.libbox">
    <uses-sdk android:minSdkVersion="21" />
</manifest>
EOF

: > "${WORK_DIR}/aar/R.txt"

rm -f "${OUTPUT_AAR}"
(
  cd "${WORK_DIR}/aar"
  jar cfM "${OUTPUT_AAR}" AndroidManifest.xml R.txt classes.jar jni
)

echo "[libbox-aar] Verifying generated ${OUTPUT_AAR}..."
unzip -l "${OUTPUT_AAR}" | grep -E "classes\.jar|jni/.*/libbox\.so"
jar tf "${WORK_DIR}/aar/classes.jar" | grep -E "^(io/nekohasekai/libbox/Libbox\.class|io/nekohasekai/libbox/PlatformInterface\.class|go/Seq\.class)$"

echo "[libbox-aar] Successfully built and verified ${OUTPUT_AAR} ($(wc -c < "${OUTPUT_AAR}" | tr -d ' ') bytes)."
