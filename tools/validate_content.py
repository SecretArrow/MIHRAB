#!/usr/bin/env python3
"""Validasi konten keagamaan sesuai SOURCES.md.

Aturan:
- Setiap JSON di app/src/main/assets/content/ harus JSON valid.
- Setiap "entri" (objek yang memuat teks keagamaan) sebaiknya punya `sumber_url`
  yang domainnya termasuk whitelist resmi.
- Nilai yang tampak URL (kunci apa pun berisi "sumber") harus domain resmi.

Mode:
  (default)  -> galat hanya untuk JSON rusak / domain tidak resmi. Entri tanpa
                sumber_url dilaporkan sebagai peringatan (tidak menggagalkan).
  --strict   -> entri tanpa sumber_url juga menggagalkan (dipakai setelah
                seluruh konten dilengkapi).

Sumber resmi: *.kemenag.go.id, *.kemenag.dev, sunnah.com, bmkg.go.id, nu.or.id
"""
from __future__ import annotations

import json
import sys
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
CONTENT_DIR = ROOT / "app" / "src" / "main" / "assets" / "content"

ALLOWED_HOSTS = ["kemenag.go.id", "kemenag.dev", "sunnah.com", "bmkg.go.id", "nu.or.id"]
ENTRY_KEYS = {"arab", "latin", "terjemahan", "judul", "nama", "dalil"}


def host_allowed(host: str) -> bool:
    host = (host or "").lower()
    return any(host == h or host.endswith("." + h) for h in ALLOWED_HOSTS)


def is_official(url: str) -> bool:
    try:
        return host_allowed(urlparse(url).hostname or "")
    except Exception:
        return False


def walk_entries(node, inherited_source=None, out=None):
    """Kumpulkan objek yang terlihat seperti entri teks keagamaan."""
    if out is None:
        out = []
    if isinstance(node, dict):
        local_source = inherited_source
        for k in ("sumber_url", "sumberUrl"):
            if isinstance(node.get(k), str) and node[k].strip():
                local_source = node[k]
        if ENTRY_KEYS & set(node.keys()):
            out.append((node, local_source))
        for v in node.values():
            walk_entries(v, local_source, out)
    elif isinstance(node, list):
        for v in node:
            walk_entries(v, inherited_source, out)
    return out


def main() -> int:
    strict = "--strict" in sys.argv
    if not CONTENT_DIR.exists():
        print(f"[validate_content] direktori tidak ditemukan: {CONTENT_DIR}")
        return 1

    errors: list[str] = []
    warnings: list[str] = []
    checked = 0

    for path in sorted(CONTENT_DIR.glob("*.json")):
        rel = path.relative_to(ROOT)
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            errors.append(f"{rel}: JSON tidak valid — {e}")
            continue

        entries = walk_entries(data)
        checked += len(entries)
        for node, source in entries:
            label = node.get("judul") or node.get("nama") or node.get("id") or "(entri)"
            if not source:
                msg = f"{rel}: entri '{label}' tanpa sumber_url"
                (errors if strict else warnings).append(msg)
            elif not is_official(source):
                errors.append(f"{rel}: entri '{label}' sumber_url di luar whitelist — {source}")

        # nilai apa pun yang tampak URL di kunci berisi 'sumber'
        def scan_urls(n, where):
            if isinstance(n, dict):
                for k, v in n.items():
                    if isinstance(v, str) and v.startswith("http") and "sumber" in k.lower():
                        if not is_official(v):
                            errors.append(f"{rel}: {where}.{k} domain tidak resmi — {v}")
                    else:
                        scan_urls(v, f"{where}.{k}")
            elif isinstance(n, list):
                for i, v in enumerate(n):
                    scan_urls(v, f"{where}[{i}]")

        scan_urls(data, rel.name)

    for w in warnings:
        print(f"[peringatan] {w}")
    for e in errors:
        print(f"[GAGAL] {e}")

    print(f"[validate_content] {checked} entri diperiksa, "
          f"{len(errors)} galat, {len(warnings)} peringatan"
          f"{' (mode strict)' if strict else ''}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
