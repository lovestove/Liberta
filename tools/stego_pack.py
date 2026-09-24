#!/usr/bin/env python3
"""Pack Liberta subscription snapshots into encrypted PNG images.

GitHub Actions runs this script hourly. VK publishing is intentionally a stub:
when VK credentials are added, publish_to_vk() is the only function that needs
real API wiring.
"""

from __future__ import annotations

import argparse
import base64
import binascii
import hashlib
import json
import math
import os
import struct
import sys
import time
import urllib.request
import zlib
from pathlib import Path

from cryptography.hazmat.primitives.ciphers.aead import AESGCM


SOURCES = {
    "whitelists": "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/WHITE-CIDR-RU-all.txt",
    "blacklists": "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS.txt",
}

MAGIC = b"LIBERTAIMG1"
AAD = b"liberta-stego-v1"
PNG_WIDTH = 256


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="build/stego", help="Output directory")
    args = parser.parse_args()

    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    snapshot = fetch_snapshot()
    packed = pack_snapshot(snapshot)
    image_path = out_dir / "liberta-subscriptions.png"
    write_png(image_path, packed)

    manifest = {
        "version": 1,
        "created_at": snapshot["created_at"],
        "image": image_path.name,
        "sources": {name: data["url"] for name, data in snapshot["sources"].items()},
        "sha256": hashlib.sha256(packed).hexdigest(),
        "vk_publish": publish_to_vk([image_path]),
    }
    (out_dir / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(json.dumps(manifest, ensure_ascii=False))
    return 0


def fetch_snapshot() -> dict:
    sources = {}
    for name, url in SOURCES.items():
        request = urllib.request.Request(url, headers={"User-Agent": "Liberta-Stego/1.0"})
        with urllib.request.urlopen(request, timeout=25) as response:
            content = response.read().decode("utf-8")
        sources[name] = {
            "url": url,
            "bytes": len(content.encode("utf-8")),
            "content_b64": base64.b64encode(content.encode("utf-8")).decode("ascii"),
        }
    return {
        "version": 1,
        "created_at": int(time.time()),
        "sources": sources,
    }


def pack_snapshot(snapshot: dict) -> bytes:
    raw = json.dumps(snapshot, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    compressed = zlib.compress(raw, level=9)
    key_material = os.environ.get("LIBERTA_STEGO_KEY", "liberta-local-development-key").encode("utf-8")
    if "LIBERTA_STEGO_KEY" not in os.environ:
        print("warning: LIBERTA_STEGO_KEY is not set; using non-production development key", file=sys.stderr)
    key = hashlib.sha256(key_material).digest()
    nonce = os.urandom(12)
    encrypted = AESGCM(key).encrypt(nonce, compressed, AAD)
    blob = nonce + encrypted
    return MAGIC + struct.pack(">I", len(blob)) + blob


def write_png(path: Path, payload: bytes) -> None:
    pixel_count = math.ceil(len(payload) / 3)
    height = max(1, math.ceil(pixel_count / PNG_WIDTH))
    capacity = PNG_WIDTH * height * 3
    padded = payload + os.urandom(capacity - len(payload))
    rows = []
    for y in range(height):
        start = y * PNG_WIDTH * 3
        rows.append(b"\x00" + padded[start : start + PNG_WIDTH * 3])
    raw_image = b"".join(rows)

    def chunk(name: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + name
            + data
            + struct.pack(">I", binascii.crc32(name + data) & 0xFFFFFFFF)
        )

    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", PNG_WIDTH, height, 8, 2, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw_image, level=9))
        + chunk(b"IEND", b"")
    )
    path.write_bytes(png)


def publish_to_vk(images: list[Path]) -> dict:
    token = os.environ.get("VK_ACCESS_TOKEN")
    group_id = os.environ.get("VK_GROUP_ID")
    if not token or not group_id:
        return {"enabled": False, "reason": "VK_ACCESS_TOKEN or VK_GROUP_ID is not configured"}
    return {
        "enabled": False,
        "reason": "VK upload scaffold is ready; wire photos.getWallUploadServer/photos.saveWallPhoto/wall.post here",
        "images": [image.name for image in images],
    }


if __name__ == "__main__":
    raise SystemExit(main())
