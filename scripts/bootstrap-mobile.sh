#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MOBILE_DIR="$ROOT_DIR/apps/mobile"

if ! command -v flutter >/dev/null 2>&1; then
  echo "Flutter is not installed or not on PATH." >&2
  exit 1
fi

if [[ ! -d "$MOBILE_DIR/android" || ! -d "$MOBILE_DIR/ios" ]]; then
  echo "Generating Android and iOS host projects..."
  (
    cd "$MOBILE_DIR"
    flutter create \
      --platforms=android,ios \
      --org com.motolink \
      --project-name motolink_mobile \
      .
  )
fi

python3 "$ROOT_DIR/scripts/patch_mobile_platforms.py"
(
  cd "$MOBILE_DIR"
  flutter pub get
)

echo "MotoLink mobile host projects are ready."
