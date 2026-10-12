#!/usr/bin/env python3
"""Perapian higiene teks — aturan yang aman dijalankan otomatis, tanpa mengubah makna.

Kenapa ada: `plan.txt` baris 1 meminta "auto fix" sebagai bagian dari alur kerja. Yang dahulu ada
(`autofix.yml`) hanya menjalankan `./gradlew tasks` — tanpa pemasang apa pun — sehingga dihapus
karena tak pernah memperbaiki apa-apa. Alat ini mengisi kekosongan itu dengan aturan yang
**tidak dapat mengubah perilaku**: hanya spasi dan baris baru.

Aturan yang dirapikan (semuanya mekanis, jadi aman untuk berkas Kotlin, JSON, YAML, maupun
Markdown sekaligus):

1. Berkas teks berakhir dengan **tepat satu** baris baru. Berkas tanpa baris baru di akhir membuat
   `git diff` menampilkan "\\ No newline at end of file" dan menyulitkan penggabungan; berkas
   dengan beberapa baris kosong di akhir menambah derau pada tiap suntingan berikutnya.
2. Tidak ada spasi atau tab di **akhir** baris. Tak terlihat di editor, tetapi ikut terbawa ke
   setiap `git diff` dan bisa berubah tanpa sengaja oleh editor lain.
3. Tidak ada baris yang **hanya** berisi spasi atau tab — itu baris kosong yang menyamar.
4. Akhir baris **LF saja**. Berkas campuran CRLF/LF membuat diff menandai seluruh berkas berubah.
5. Tidak lebih dari **dua** baris kosong beruntun.

Yang **tidak** disentuh, karena tidak dapat dipastikan tanpa membaca maksud penulisnya: indentasi,
panjang baris, urutan impor, penamaan, dan seluruh isi teks keagamaan. Alat ini bukan pengganti
`./gradlew :app:lintFix` (yang menangani aturan Android Lint) maupun peninjauan manusia.

Penggunaan:
  python3 tools/format_teks.py [folder]            # periksa saja; keluar 1 bila ada temuan
  python3 tools/format_teks.py --fix [folder]      # rapikan di tempat
"""

from __future__ import annotations

import sys
from pathlib import Path

# Ekstensi berkas teks yang dirapikan. Sengaja berupa daftar, bukan "semua berkas": berkas biner
# atau tak dikenal tidak boleh dibuka lalu ditulis ulang.
EKSTENSI = {
    ".kt", ".kts", ".json", ".yml", ".yaml", ".md", ".xml", ".toml",
    ".py", ".sh", ".properties", ".pro", ".txt", ".cfg",
}

# Folder yang dilewati: hasil build dan metadata perkakas, bukan sumber yang disunting manusia.
LEWATI_FOLDER = {".git", "build", ".gradle", "__pycache__", ".idea", "node_modules", ".kotlin"}

# Batas ukuran: berkas raksasa (mis. aset) tidak perlu diperiksa dan memperlambat gate.
BATAS_BYTE = 2 * 1024 * 1024

MAKS_BARIS_KOSONG = 2


def rapikan(teks: str) -> str:
    """Kembalikan [teks] setelah seluruh aturan diterapkan. Idempoten."""
    # Berkas kosong dibiarkan kosong: tidak ada baris akhir yang hilang, dan mengubahnya menjadi
    # "\n" berarti menambah isi — padahal alat ini hanya boleh membuang kelebihan spasi/baris baru,
    # bukan menciptakannya.
    if teks == "":
        return ""

    # Akhir baris diseragamkan lebih dulu supaya aturan berikutnya hanya berurusan dengan "\n".
    teks = teks.replace("\r\n", "\n").replace("\r", "\n")

    baris = [b.rstrip() for b in teks.split("\n")]

    # Batas dua baris kosong beruntun.
    hasil: list[str] = []
    kosong = 0
    for b in baris:
        if b == "":
            kosong += 1
            if kosong > MAKS_BARIS_KOSONG:
                continue
        else:
            kosong = 0
        hasil.append(b)

    # Buang seluruh baris kosong di ujung, lalu tambahkan tepat satu baris baru.
    while hasil and hasil[-1] == "":
        hasil.pop()
    return "\n".join(hasil) + "\n"


def periksa(path: Path) -> list[str]:
    """Kembalikan daftar temuan untuk satu berkas (kosong = bersih)."""
    try:
        asli = path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return []  # berkas biner atau tak terbaca: bukan urusan alat ini

    if asli == "":
        return []  # berkas kosong: tidak ada yang bisa dirapikan

    if rapikan(asli) == asli:
        return []

    temuan: list[str] = []
    if "\r\n" in asli or "\r" in asli:
        temuan.append("akhir baris bukan LF")
    if asli and not asli.endswith("\n"):
        temuan.append("tidak berakhir dengan baris baru")
    if asli.endswith("\n\n"):
        temuan.append("ada baris kosong berlebih di akhir berkas")
    if any(b != b.rstrip() for b in asli.split("\n")):
        temuan.append("ada spasi/tab di akhir baris")
    if any(b and b.strip() == "" for b in asli.split("\n")):
        temuan.append("ada baris berisi spasi/tab saja")
    if "\n\n\n\n" in asli.replace("\r\n", "\n"):
        temuan.append("lebih dari dua baris kosong beruntun")
    return temuan or ["higiene teks"]


def berkas_teks(akar: Path) -> list[Path]:
    hasil = []
    for p in sorted(akar.rglob("*")):
        if not p.is_file() or p.suffix not in EKSTENSI:
            continue
        if any(bagian in LEWATI_FOLDER for bagian in p.parts):
            continue
        try:
            if p.stat().st_size > BATAS_BYTE:
                continue
            if b"\x00" in p.read_bytes()[:8192]:
                continue  # berkas biner
        except OSError:
            continue
        hasil.append(p)
    return hasil


def main() -> int:
    argv = sys.argv[1:]
    fix = "--fix" in argv
    sisa = [a for a in argv if not a.startswith("--")]
    akar = Path(sisa[0]) if sisa else Path(".")

    if not akar.is_dir():
        print(f"[format_teks] folder tidak ditemukan: {akar}")
        return 1

    berkas = berkas_teks(akar)
    if not berkas:
        print(f"[format_teks] tidak ada berkas teks di {akar}")
        return 1

    dirapikan = 0
    temuan_total = 0
    for p in berkas:
        temuan = periksa(p)
        if not temuan:
            continue
        if fix:
            p.write_text(rapikan(p.read_text(encoding="utf-8")), encoding="utf-8")
            dirapikan += 1
            print(f"[dirapikan] {p}: {', '.join(temuan)}")
        else:
            temuan_total += len(temuan)
            for t in temuan:
                print(f"[GAGAL] {p}: {t}")

    if fix:
        print(f"[format_teks] {len(berkas)} berkas diperiksa, {dirapikan} dirapikan")
        return 0

    if temuan_total:
        print(f"[format_teks] {len(berkas)} berkas diperiksa, {temuan_total} temuan "
              f"— jalankan `python3 tools/format_teks.py --fix`")
        return 1
    print(f"[format_teks] {len(berkas)} berkas diperiksa, 0 temuan")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
