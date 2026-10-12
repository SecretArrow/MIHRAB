#!/usr/bin/env python3
"""Gate: setiap berkas di `.github/workflows/` harus benar-benar dapat dijalankan.

Kenapa gate ini ada: GitHub **tidak** melaporkan berkas workflow yang salah parse sebagai
kesalahan yang jelas. Ia menjalankan workflow-nya, langsung menggagalkannya, dan tidak membuat
satu pun job — sehingga pemeriksaan yang seharusnya menjaga PR hilang tanpa jejak di daftar
check. Itu pernah terjadi: satu nama langkah yang memuat `": "` tanpa tanda kutip membuat
seluruh `ci.yml` tidak sah, dan PR tidak punya check sama sekali.

Gate ini karena itu dijalankan oleh workflow **terpisah** (`workflow-lint.yml`): kalau `ci.yml`
sendiri yang rusak, `ci.yml` tidak bisa memberi tahu siapa pun.

Yang diperiksa, sengaja minimal agar tidak menyalin aturan GitHub:
  1. Berkas dapat dibaca sebagai YAML (galat ditampilkan beserta baris/kolomnya).
  2. Akar berupa pemetaan dan memuat `jobs` yang tidak kosong.
  3. Setiap job punya `runs-on` dan `steps` yang tidak kosong.

Penggunaan: `python3 tools/check_workflows.py [folder_workflow]`
"""

from __future__ import annotations

import sys
from pathlib import Path

try:
    import yaml
except ImportError:  # pragma: no cover - runner GitHub sudah menyediakannya
    print("[check_workflows] PyYAML tidak tersedia; jalankan `pip install pyyaml`.")
    raise SystemExit(2)


def periksa(path: Path) -> list[str]:
    """Kembalikan daftar galat untuk satu berkas workflow (kosong = lolos)."""
    try:
        isi = path.read_text(encoding="utf-8")
    except OSError as exc:
        return [f"tidak dapat dibaca: {exc}"]

    try:
        dok = yaml.safe_load(isi)
    except yaml.YAMLError as exc:
        # Pesan PyYAML sudah memuat baris/kolom; dipertahankan apa adanya agar mudah diperbaiki.
        return [f"YAML tidak sah — {exc}"]

    if not isinstance(dok, dict):
        return ["akar berkas bukan pemetaan (mapping) YAML"]

    galat: list[str] = []
    # `on:` dibaca YAML 1.1 sebagai boolean True, sehingga kuncinya bisa berupa True.
    if "on" not in dok and True not in dok:
        galat.append("tidak menyatakan pemicu ('on')")

    jobs = dok.get("jobs")
    if not isinstance(jobs, dict) or not jobs:
        galat.append("tidak memuat job apa pun ('jobs')")
        return galat

    for nama, job in jobs.items():
        if not isinstance(job, dict):
            galat.append(f"job '{nama}' bukan pemetaan")
            continue
        if "runs-on" not in job:
            galat.append(f"job '{nama}' tidak punya 'runs-on'")
        langkah = job.get("steps")
        if not isinstance(langkah, list) or not langkah:
            galat.append(f"job '{nama}' tidak punya 'steps'")
    return galat


def main() -> int:
    folder = Path(sys.argv[1] if len(sys.argv) > 1 else ".github/workflows")
    if not folder.is_dir():
        print(f"[check_workflows] folder tidak ditemukan: {folder}")
        return 1

    berkas = sorted(folder.glob("*.y*ml"))
    if not berkas:
        print(f"[check_workflows] tidak ada berkas workflow di {folder}")
        return 1

    total = 0
    for path in berkas:
        galat = periksa(path)
        if galat:
            total += len(galat)
            for g in galat:
                print(f"[GAGAL] {path}: {g}")
        else:
            print(f"[ok] {path}")

    if total:
        print(f"[check_workflows] {len(berkas)} berkas diperiksa, {total} galat")
        return 1
    print(f"[check_workflows] {len(berkas)} berkas diperiksa, 0 galat")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
