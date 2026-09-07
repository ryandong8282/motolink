#!/usr/bin/env python3
"""Generate the MotoLink MVP scaffold, then remove bootstrap artifacts."""
from __future__ import annotations

import base64
import hashlib
import io
from pathlib import Path
import shutil
import tarfile

ARCHIVE_SHA256 = "bf8c8e4736f1d05675067b7b65e7427331a3c81c60803b91cb4788e7f36decfc"
PAYLOAD_LENGTH = 50_252


def read(root: Path, relative_path: str) -> str:
    return (root / relative_path).read_text(encoding="ascii")


def build_payload(root: Path) -> str:
    part03_c1_p2 = "".join(
        read(root, path)
        for path in (
            "scripts/fixes/part03/c1pieces/p2parts/q0.txt",
            "scripts/fixes/part03/c1pieces/p2parts/q1.txt",
            "scripts/fixes/part03/c1pieces/p2parts/q2.txt",
            "scripts/fixes/part03/c1pieces/p2parts/q3fixed.txt",
            "scripts/fixes/part03/c1pieces/p2parts/q4.txt",
        )
    )
    part03_c1 = "".join(
        (
            read(root, "scripts/fixes/part03/c1pieces/p0.txt"),
            read(root, "scripts/fixes/part03/c1pieces/p1.txt"),
            part03_c1_p2,
            read(root, "scripts/fixes/part03/c1pieces/p3.txt"),
        )
    )
    part03 = "".join(
        (
            read(root, "scripts/fixes/part03/c0.txt"),
            part03_c1,
            read(root, "scripts/fixes/part03/c2.txt"),
            read(root, "scripts/fixes/part03/c3.txt"),
            read(root, "scripts/fixes/part03_1.txt"),
        )
    )
    part05 = read(root, "scripts/fixes/part05_0.txt") + read(
        root, "scripts/fixes/part05_1.txt"
    )

    return "".join(
        (
            read(root, "scripts/payload/part01.txt"),
            read(root, "scripts/payload/part02.txt"),
            part03,
            read(root, "scripts/payload/part04.txt"),
            part05,
            read(root, "scripts/payload/part06.txt"),
            read(root, "scripts/payload/part07.txt"),
        )
    )


def main() -> None:
    root = Path.cwd().resolve()
    payload = build_payload(root)
    if len(payload) != PAYLOAD_LENGTH:
        raise RuntimeError(
            f"Unexpected scaffold payload length: {len(payload)} != {PAYLOAD_LENGTH}"
        )

    archive = base64.b64decode(payload, validate=True)
    digest = hashlib.sha256(archive).hexdigest()
    if digest != ARCHIVE_SHA256:
        raise RuntimeError(f"Scaffold archive integrity check failed: {digest}")

    with tarfile.open(fileobj=io.BytesIO(archive), mode="r:gz") as tar:
        for member in tar.getmembers():
            destination = (root / member.name).resolve()
            if root not in destination.parents and destination != root:
                raise RuntimeError(f"Unsafe archive member: {member.name}")
        tar.extractall(root)

    shutil.rmtree(root / "scripts", ignore_errors=True)
    (root / ".github/workflows/bootstrap-scaffold.yml").unlink(missing_ok=True)


if __name__ == "__main__":
    main()
