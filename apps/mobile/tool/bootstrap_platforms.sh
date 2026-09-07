#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v flutter >/dev/null 2>&1; then
  echo "flutter 未安装或不在 PATH 中" >&2
  exit 1
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

flutter create "$TMP_DIR/motolink" \
  --platforms=android,ios \
  --org=com.motolink \
  --project-name=motolink

rm -rf android ios
cp -R "$TMP_DIR/motolink/android" ./android
cp -R "$TMP_DIR/motolink/ios" ./ios

ANDROID_DIR="android/app/src/main/kotlin/com/motolink/motolink"
mkdir -p "$ANDROID_DIR"
cp native_templates/android/MainActivity.kt "$ANDROID_DIR/MainActivity.kt"
cp native_templates/ios/AppDelegate.swift ios/Runner/AppDelegate.swift

flutter pub get

echo "Android/iOS 工程已生成，原生 Riding Core 模板已写入。"
