#!/usr/bin/env python3
"""Patch generated Flutter host projects with MotoLink MVP permissions.

The script is idempotent. It does not configure vendor RTC credentials.
"""

from __future__ import annotations

import plistlib
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
IOS_PLIST = ROOT / "apps/mobile/ios/Runner/Info.plist"
ANDROID_MANIFEST = ROOT / "apps/mobile/android/app/src/main/AndroidManifest.xml"
ANDROID_NS = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", ANDROID_NS)


def patch_ios() -> None:
    if not IOS_PLIST.exists():
        raise SystemExit(f"Missing {IOS_PLIST}; run flutter create first")

    with IOS_PLIST.open("rb") as handle:
        data = plistlib.load(handle)

    data.update(
        {
            "NSMicrophoneUsageDescription": "用于车队骑行中的按住说话语音对讲。",
            "NSLocationWhenInUseUsageDescription": "用于显示队友距离并记录本次骑行轨迹。",
            "NSLocationAlwaysAndWhenInUseUsageDescription": "骑行开始后用于在锁屏或切换应用时持续记录轨迹和共享队内位置。",
        }
    )
    modes = list(dict.fromkeys([*data.get("UIBackgroundModes", []), "audio", "location"]))
    data["UIBackgroundModes"] = modes

    with IOS_PLIST.open("wb") as handle:
        plistlib.dump(data, handle, sort_keys=False)


def patch_android() -> None:
    if not ANDROID_MANIFEST.exists():
        raise SystemExit(f"Missing {ANDROID_MANIFEST}; run flutter create first")

    tree = ET.parse(ANDROID_MANIFEST)
    root = tree.getroot()
    required = [
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_MICROPHONE",
        "android.permission.FOREGROUND_SERVICE_LOCATION",
        "android.permission.POST_NOTIFICATIONS",
    ]
    existing = {
        node.attrib.get(f"{{{ANDROID_NS}}}name")
        for node in root.findall("uses-permission")
    }
    insert_at = 0
    for permission in required:
        if permission not in existing:
            node = ET.Element("uses-permission")
            node.set(f"{{{ANDROID_NS}}}name", permission)
            root.insert(insert_at, node)
            insert_at += 1

    tree.write(ANDROID_MANIFEST, encoding="utf-8", xml_declaration=True)


if __name__ == "__main__":
    patch_ios()
    patch_android()
    print("Patched iOS and Android permissions/background modes.")
