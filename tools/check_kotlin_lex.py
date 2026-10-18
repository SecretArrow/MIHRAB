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

Agar tanda kutip di dalam literal tidak ikut dihitung, pemindaian melewati literal string dan
karakter — termasuk string mentah, yang isinya boleh berakhir dengan kutip sehingga penutupnya
adalah tiga kutipan **terakhir** dari rentetan kutipan di ujungnya, bukan tiga yang pertama.
Salah membaca bentuk itu membuat apostrof di dalam kode sesudahnya membuka literal karakter
palsu yang menelan `/*` berikutnya, dan gate melaporkan `*/` tanpa pembuka pada berkas yang
sebenarnya bersih.

Ini **bukan** pengganti kompilasi; ia hanya memeriksa hal yang bisa dipastikan secara leksikal
tanpa membangun proyek, supaya galat sejenis tidak perlu menunggu satu siklus CI.

Penggunaan: `python3 tools/check_kotlin_lex.py [folder_sumber]`
            `python3 tools/check_kotlin_lex.py --uji-diri` (memeriksa pemindainya sendiri)
"""

from __future__ import annotations

import sys
from pathlib import Path


def periksa_teks(teks: str) -> list[str]:
    """Kembalikan daftar galat higiene komentar untuk isi satu berkas Kotlin."""
    galat: list[str] = []

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
            # Kutipan yang menempel di belakang penutup masih isi string mentah: pada
            # `Regex("""…""""")` isinya berakhir dengan satu kutip, dan penutupnya adalah tiga
            # kutipan **terakhir** dari rentetan itu. Tanpa aturan ini, sisa kutipan terbaca
            # sebagai awal literal biasa, lalu apostrof di dalamnya (mis. `ta'awudz`) membuka
            # literal karakter palsu yang menelan `/*` sesudahnya — dan `*/` di ujung komentar itu
            # dilaporkan sebagai penutup tanpa pembuka, seolah berkasnya cacat.
            while akhir >= 0 and akhir + 3 < n and teks[akhir + 3] == '"':
                akhir += 1
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


def periksa(path: Path) -> list[str]:
    """Kembalikan daftar galat higiene komentar untuk satu berkas Kotlin."""
    return periksa_teks(path.read_text(encoding="utf-8"))


# Potongan Kotlin yang harus dibaca benar (atau salah) oleh pemindai di atas. Berkas nyata tak
# dapat memuat keduanya sekaligus — yang satu harus bersih dan yang lain harus gagal — jadi
# kasusnya ditulis di sini dan diperiksa `--uji-diri`.
UJI_DIRI: list[tuple[str, str, bool]] = [
    (
        "string mentah berakhir dengan kutip, diikuti apostrof di dalam kode",
        'val r = Regex("""[^"]+"""").findAll(it).map { m -> m.value.trim(\'"\') }\n'
        "/**\n"
        " * Ghaib membaca ta'awudz sebelum Al-Fatihah.\n"
        " */\n"
        "fun f() {}\n",
        False,
    ),
    (
        "string mentah biasa",
        'val r = Regex("""a"b""")\nfun f() {}\n',
        False,
    ),
    (
        "apostrof di dalam komentar bukan awal literal karakter",
        "/**\n * Mazhab Syafi'i dan nama Ka'bah.\n */\nfun f() {}\n",
        False,
    ),
    (
        "penutup komentar di dalam teks komentar",
        "/**\n * Pola MIME `` `*/*` `` ditulis apa adanya.\n */\nfun f() {}\n",
        True,
    ),
    (
        "komentar blok tidak ditutup",
        "/* belum ditutup\nfun f() {}\n",
        True,
    ),
]


def uji_diri() -> int:
    """Periksa pemindai terhadap kasus di [UJI_DIRI]; kembalikan 0 bila semuanya sesuai dugaan."""
    gagal = 0
    for nama, teks, harus_gagal in UJI_DIRI:
        galat = periksa_teks(teks)
        if bool(galat) != harus_gagal:
            gagal += 1
            harap = "galat" if harus_gagal else "bersih"
            print(f"[GAGAL] uji-diri: {nama} — seharusnya {harap}, hasilnya {galat!r}")
    if gagal:
        print(f"[check_kotlin_lex] uji-diri: {len(UJI_DIRI)} kasus, {gagal} tidak sesuai")
        return 1
    print(f"[check_kotlin_lex] uji-diri: {len(UJI_DIRI)} kasus, semuanya sesuai")
    return 0


def main() -> int:
    if len(sys.argv) > 1 and sys.argv[1] == "--uji-diri":
        return uji_diri()

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
