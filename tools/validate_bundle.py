#!/usr/bin/env python3
"""Validasi bundel pembaruan konten.

`plan.txt`: *"Sediakan mekanisme pembaruan konten dengan validasi sumber."* Aplikasi memasang
bundel yang dipilih pengguna sendiri; skrip ini adalah sisi CI-nya, supaya berkas yang diterima
aplikasi tidak mungkin lebih longgar daripada yang diterima gate konten.

Bentuk bundel:

    {
      "_bundel": { "skema": 1, "dibuat": "2026-02-19", "penerbit": "LPMQ Kemenag" },
      "berkas": { "<nama>.json": { ...dokumen konten lengkap... } }
    }

Setiap dokumen di dalam `berkas` dijalankan melalui aturan yang **sama** dengan
`tools/validate_content.py` (sumber resmi, skema, placeholder, harakat, kategori) — tidak ada
aturan yang disalin ulang di sini.

Pakai:
    python3 tools/validate_bundle.py [--strict] bundel.json [...]
    python3 tools/validate_bundle.py --harus-gagal bundel.json   # fixture: wajib ditolak
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import validate_content as vc  # noqa: E402

SKEMA = 1
PANJANG_NAMA_MAKS = 64


def nama_berkas_sah(nama: str) -> bool:
    """Sama dengan `BundelKonten.namaBerkasSah` di aplikasi."""
    return (
        6 <= len(nama) <= PANJANG_NAMA_MAKS
        and nama.endswith(".json")
        and not nama.startswith(".")
        and not nama.startswith("_")
        and not any(c in nama for c in "/\\:")
    )


def validasi(path: Path, strict: bool) -> list[str]:
    errs: list[str] = []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        return [f"{path}: JSON tidak valid — {e}"]

    if not isinstance(data, dict):
        return [f"{path}: bundel harus berupa objek JSON"]

    meta = data.get("_bundel")
    if not isinstance(meta, dict):
        errs.append(f"{path}: tanpa metadata '_bundel'")
        meta = {}
    if meta.get("skema") != SKEMA:
        errs.append(f"{path}: skema {meta.get('skema')!r} tidak didukung (harus {SKEMA})")

    berkas = data.get("berkas")
    if not isinstance(berkas, dict) or not berkas:
        errs.append(f"{path}: tanpa objek 'berkas' yang berisi")
        return errs

    for nama, doc in sorted(berkas.items()):
        if not nama_berkas_sah(nama):
            errs.append(f"{path}: nama berkas '{nama}' tidak sah")
            continue
        if not isinstance(doc, dict):
            errs.append(f"{path}: '{nama}' bukan objek JSON")
            continue

        sumber = doc.get("sumber_url") or doc.get("sumberUrl") or ""
        if not vc.is_official(sumber):
            errs.append(f"{path}: {nama} sumber_url tidak resmi — {sumber!r}")

        # Aturan yang sama dengan gate konten — dipanggil, bukan disalin.
        errs += vc.check_schema(nama, doc)
        errs += vc.check_placeholders(nama, doc)
        errs += vc.check_harakat(nama, doc)
        errs += vc.check_kategori(nama, doc)

        for node, source in vc.walk_entries(doc):
            label = node.get("judul") or node.get("nama") or node.get("id") or "(entri)"
            if not source:
                if strict:
                    errs.append(f"{path}: {nama} entri '{label}' tanpa sumber_url")
            elif not vc.is_official(source):
                errs.append(f"{path}: {nama} entri '{label}' sumber_url di luar whitelist — {source}")

    return [f"{path}: {e}" if not e.startswith(str(path)) else e for e in errs]


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    strict = "--strict" in sys.argv
    harus_gagal = "--harus-gagal" in sys.argv

    if not args:
        print("[validate_bundle] tidak ada berkas bundel yang diberikan")
        return 1

    errors: list[str] = []
    for arg in args:
        path = Path(arg)
        if not path.is_file():
            errors.append(f"{path}: berkas tidak ditemukan")
            continue
        errors += validasi(path, strict)

    for e in errors:
        print(f"[GAGAL] {e}")

    if harus_gagal:
        if errors:
            print(f"[validate_bundle] {len(errors)} galat seperti diharapkan (fixture wajib ditolak)")
            return 0
        print("[validate_bundle] fixture seharusnya ditolak, tetapi lolos")
        return 1

    print(f"[validate_bundle] {len(args)} bundel diperiksa, {len(errors)} galat")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
