#!/usr/bin/env python3
"""Sinkronkan konten Tahlil dari bundel Android ke web/.

Permintaan pengguna (2026-10-20): versi web di folder `web/`. Sumber tunggal konten tetap
bundel Android (`android/app/src/main/assets/content/`) — `web/content/` hanyalah SALINAN
yang dibuat skrip ini, supaya perbaikan teks (harakat, Latin polos, atribusi) tidak pernah
hanya terjadi di salah satu versi. Dipanggil:

  python3 tools/sinkron_web.py          # salin ulang
  python3 tools/sinkron_web.py --check  # gagal bila salinan basi (dipakai workflow Pages)

Selain tiga berkas inti (tahlil.json, tahlil_full.json, tahlil_imam.json), skrip ini juga
menyalin **berkas rujukan**: bagian surat pada Ringkas/Lengkap tidak menyimpan teksnya,
melainkan menunjuk `rujukan_file` (surah_001/112/113/114.json, ayat_kursi.json) — di Android
teks itu diselesaikan `ContentRepo.resolveArabLatinTerjemah`. Daftar salinannya dipindai dari
bundel sendiri (kunci `rujukan_file`/`rujukanFile`, di mana pun kedalannya), jadi rujukan baru
ikut tersalin tanpa menyunting daftar di dua tempat. Rujukan mati (berkas sasaran tidak ada
di bundel) menggagalkan sinkronisasi — perilaku yang sama dengan `BundelKonten.periksaRujukan`
di Android; tanpa itu kartu surat justru tampil kosong tanpa galat (ditemukan 2026-10-31).

Idempoten: salinan identik tidak ditulis ulang (mtime tidak berubah tanpa perubahan isi).
"""
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASAL = ROOT / "android" / "app" / "src" / "main" / "assets" / "content"
TUJUAN = ROOT / "web" / "content"

BERKAS_INTI = ["tahlil.json", "tahlil_full.json", "tahlil_imam.json"]
KUNCI_RUJUKAN = ("rujukan_file", "rujukanFile")


def kumpulkan_rujukan(o, hasil: set) -> None:
    """Kumpulkan sasaran `rujukan_file` (tanpa akhiran `#id`) dari struktur JSON mana pun."""
    if isinstance(o, dict):
        for kunci, nilai in o.items():
            if kunci in KUNCI_RUJUKAN and isinstance(nilai, str) and nilai.strip():
                nama = nilai.split("#", 1)[0].strip()
                if nama:
                    hasil.add(nama)
            else:
                kumpulkan_rujukan(nilai, hasil)
    elif isinstance(o, list):
        for x in o:
            kumpulkan_rujukan(x, hasil)


def daftar_salinan() -> list:
    """Berkas inti + seluruh sasaran rujukan; gagal keras bila ada rujukan mati."""
    nama_rujukan: set = set()
    for nama in BERKAS_INTI:
        try:
            dok = json.loads((ASAL / nama).read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as e:
            sys.exit(f"GAGAL: {nama} tidak dapat dibaca dari bundel sumber: {e}")
        kumpulkan_rujukan(dok, nama_rujukan)
    salinan = list(BERKAS_INTI)
    for nama in sorted(nama_rujukan):
        if "/" in nama or nama.startswith("."):
            sys.exit(f"GAGAL: nama rujukan mencurigakan: {nama!r}")
        if not (ASAL / nama).is_file():
            sys.exit(f"GAGAL: rujukan mati — '{nama}' tidak ada di bundel sumber "
                     f"(BundelKonten akan menolaknya juga)")
        salinan.append(nama)
    return salinan


def main():
    cek = "--check" in sys.argv
    if not ASAL.is_dir():
        sys.exit(f"GAGAL: bundel sumber tidak ada di {ASAL}")
    TUJUAN.mkdir(parents=True, exist_ok=True)
    basi = []
    for nama in daftar_salinan():
        asal = ASAL / nama
        tujuan = TUJUAN / nama
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
