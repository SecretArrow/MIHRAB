#!/usr/bin/env python3
"""Sinkronkan konten Tahlil dari bundel Android ke web/.

Permintaan pengguna (2026-10-20): versi web di folder `web/`. Sumber tunggal konten tetap
bundel Android (`android/app/src/main/assets/content/`) — `web/content/` hanyalah SALINAN
yang dibuat skrip ini, supaya perbaikan teks (harakat, Latin polos, atribusi) tidak pernah
hanya terjadi di salah satu versi. Dipanggil:

  python3 tools/sinkron_web.py          # salin ulang
  python3 tools/sinkron_web.py --check  # gagal bila salinan basi (dipakai workflow Pages)

Idempoten: salinan identik tidak ditulis ulang (mtime tidak berubah tanpa perubahan isi).
"""
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASAL = ROOT / "android" / "app" / "src" / "main" / "assets" / "content"
TUJUAN = ROOT / "web" / "content"

BERKAS = ["tahlil.json", "tahlil_full.json", "tahlil_imam.json"]


def main():
    cek = "--check" in sys.argv
    if not ASAL.is_dir():
        sys.exit(f"GAGAL: bundel sumber tidak ada di {ASAL}")
    TUJUAN.mkdir(parents=True, exist_ok=True)
    basi = []
    for nama in BERKAS:
        asal = ASAL / nama
        tujuan = TUJUAN / nama
        if not asal.is_file():
            sys.exit(f"GAGAL: {nama} tidak ada di bundel sumber")
        if cek:
            if not tujuan.is_file() or tujuan.read_bytes() != asal.read_bytes():
                basi.append(nama)
            continue
        if tujuan.is_file() and tujuan.read_bytes() == asal.read_bytes():
            print(f"  sama  {nama}")
            continue
        shutil.copyfile(asal, tujuan)
        print(f"  salin {nama}")
    if cek:
        if basi:
            sys.exit(f"GAGAL: salinan web basi — jalankan 'python3 tools/sinkron_web.py': {basi}")
        print("OK: salinan web sinkron dengan bundel Android")
        return
    print("OK: web/content tersinkron")


if __name__ == "__main__":
    main()
