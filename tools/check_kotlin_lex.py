#!/usr/bin/env python3
"""Gate higiene komentar Kotlin — menangkap galat kompilasi yang paling mudah lolos.

Blok komentar Kotlin (`/* … */`) berakhir pada `*/` **pertama**, jadi menuliskan `*/` di dalam
komentar — misalnya saat menyebut pola MIME atau potongan kode — menutup komentarnya lebih awal
dan sisa teksnya berubah menjadi kode. Itu pernah terjadi: satu KDoc memuat `` `*/*` `` dan
seluruh modul gagal dikompilasi dengan deretan "Expecting a top level declaration".

Aturan yang diperiksa (sengaja sempit agar tidak ada positif palsu):

1. Satu baris tidak boleh memuat lebih dari satu `*/`. Komentar satu baris hanya punya satu
   penutup; penutup kedua berarti salah satunya berada di dalam teks komentar.
2. Satu baris tidak boleh memuat lebih dari satu `/*`. Pembuka kedua di dalam komentar membuka
   komentar bersarang (Kotlin mengizinkannya), sehingga jumlah penutupnya jadi tak seimbang.
3. Kedalaman komentar harus kembali nol di akhir berkas — komentar yang tidak tertutup.

Ini **bukan** pengganti kompilasi; ia hanya memeriksa hal yang bisa dipastikan secara leksikal
tanpa membangun proyek, supaya galat sejenis tidak perlu menunggu satu siklus CI.

Penggunaan: `python3 tools/check_kotlin_lex.py [folder_sumber]`
"""

from __future__ import annotations

import sys
from pathlib import Path


def periksa(path: Path) -> list[str]:
    """Kembalikan daftar galat higiene komentar untuk satu berkas Kotlin."""
    galat: list[str] = []
    teks = path.read_text(encoding="utf-8")

    for no, baris in enumerate(teks.splitlines(), start=1):
        if baris.count("*/") > 1:
            galat.append(f"baris {no}: ada lebih dari satu '*/' — komentar ditutup lebih awal")
        if baris.count("/*") > 1:
            galat.append(f"baris {no}: ada lebih dari satu '/*' — komentar bersarang")

    # Kedalaman komentar, dengan literal string/karakter dilewati agar '/*' di dalamnya tidak
    # dihitung. String mentah (\"\"\"…\"\"\") berhenti pada tiga kutip, bukan pada escape.
    # Di dalam komentar hanya pembuka/penutup komentar yang berarti — tanda kutip di sana (mis.
    # "Ka'bah" atau "Syafi'i") bukan awal literal, jadi tidak boleh memulai pemindaian literal.
    depth = 0
    i = 0
    n = len(teks)
    while i < n:
        c = teks[i]

        if depth > 0:
            if teks.startswith("*/", i):
                depth -= 1
                i += 2
            elif teks.startswith("/*", i):
                depth += 1
                i += 2
            else:
                i += 1
            continue

        if teks.startswith("//", i):
            akhir = teks.find("\n", i)
            i = n if akhir < 0 else akhir + 1
            continue
        if teks.startswith("/*", i):
            depth += 1
            i += 2
            continue
        if teks.startswith("*/", i):
            galat.append(f"posisi {i}: '*/' tanpa '/*' pembuka")
            i += 2
            continue
        if c == '"' and teks.startswith('"""', i):
            akhir = teks.find('"""', i + 3)
            i = n if akhir < 0 else akhir + 3
            continue
        if c == '"':
            i += 1
            while i < n:
                if teks[i] == "\\":
                    i += 2
                    continue
                if teks[i] == '"' or teks[i] == "\n":
                    break
                i += 1
            i += 1
            continue
        if c == "'":
            i += 1
            while i < n and teks[i] != "'":
                i += 2 if teks[i] == "\\" else 1
            i += 1
            continue
        i += 1

    if depth > 0:
        galat.append(f"ada {depth} komentar blok yang tidak ditutup")
    return galat


def main() -> int:
    akar = Path(sys.argv[1] if len(sys.argv) > 1 else "app/src")
    if not akar.is_dir():
        print(f"[check_kotlin_lex] folder tidak ditemukan: {akar}")
        return 1

    berkas = sorted(akar.rglob("*.kt"))
    if not berkas:
        print(f"[check_kotlin_lex] tidak ada berkas .kt di {akar}")
        return 1

    total = 0
    for path in berkas:
        galat = periksa(path)
        if galat:
            total += len(galat)
            for g in galat:
                print(f"[GAGAL] {path}: {g}")

    if total:
        print(f"[check_kotlin_lex] {len(berkas)} berkas diperiksa, {total} galat")
        return 1
    print(f"[check_kotlin_lex] {len(berkas)} berkas diperiksa, 0 galat")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
