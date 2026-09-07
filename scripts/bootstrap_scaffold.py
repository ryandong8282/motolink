#!/usr/bin/env python3
"""One-shot repository bootstrap; removed after extraction."""
from __future__ import annotations

import base64
import io
from pathlib import Path
import shutil
import tarfile


def main() -> None:
    root = Path.cwd().resolve()
    payload_dir = root / "scripts/payload"
    payload = "".join(path.read_text(encoding="ascii") for path in sorted(payload_dir.glob("part*.txt")))
    archive = base64.b64decode(payload)
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
