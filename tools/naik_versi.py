#!/usr/bin/env python3
"""Naikkan versi rilis di tempat: `versionName`/`versionCode` di app/build.gradle.kts + CHANGELOG.md.

Dipakai oleh `.github/workflows/auto-release.yml` untuk rilis otomatis. Versi hidup di SATU tempat
— `app/build.gradle.kts` — dan tag rilis wajib menyamainya (`release.yml` membandingkan keduanya
lalu menggagalkan rilis bila berbeda), jadi kenaikan versi pun harus dilakukan pada sumber yang
sama, bukan diketik ulang di workflow. Angka yang disalin ke banyak tempat pasti menyimpang.

Yang dilakukan:
  1. Membaca `versionName` ("X.Y.Z") dan `versionCode` (bilangan bulat) dari `app/build.gradle.kts`
     — masing-masing wajib tepat satu kali, kalau tidak: galat keras. Baris komentar yang menyebut
     kedua nama itu tidak ikut terbaca karena polanya menuntut awal baris berupa penugasan.
  2. Menurunkan versi baru: `--jenis minor` → X.(Y+1).0; `--jenis patch` (bawaan) → X.Y.(Z+1).
     `versionCode` selalu +1 (Google Play menolak yang tidak naik).
  3. Memindahkan isi `## [Unreleased]` di CHANGELOG.md menjadi bagian `## [X.Y.Z] - tanggal`
     (format Keep a Changelog), menyisipkan catatan rilis otomatis di dalamnya, lalu membuka
     kembali bagian `[Unreleased]` yang kosong di atasnya. Bila `[Unreleased]` sedang berisi,
     isinya ikut dipindahkan — itu memang perilaku yang benar: catatan itu ditulis untuk rilis
     berikutnya, dan rilis otomatis tidak boleh membuangnya.

Yang TIDAK dilakukan di sini: memeriksa tabrakan dengan tag yang sudah ada. Itu pekerjaan
pemanggilnya, yang memegang riwayat git lengkap; alat ini sengaja murni suntingan berkas supaya
 perilakunya dapat diuji tanpa repositori.

Keluaran (stdout, dua baris stabil untuk dipanen workflow):
  versi=X.Y.Z
  kode=N

Penggunaan:
  python3 tools/naik_versi.py [--jenis minor|patch] [--commit-sha SHA] [--tanggal YYYY-MM-DD] [--akar DIR]
"""

from __future__ import annotations

import argparse
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

BERKAS_GRADLE = Path("app/build.gradle.kts")
BERKAS_CHANGELOG = Path("CHANGELOG.md")

# Awal baris (bukan komentar) berupa penugasan versionName/versionCode. Backreference menahan
# bentuk indentasi aslinya sehingga penulisan ulang tidak mengubah format berkas.
POLA_NAMA = re.compile(r'(?m)^(\s*versionName\s*=\s*")(\d+\.\d+\.\d+)("\s*)$')
POLA_KODE = re.compile(r"(?m)^(\s*versionCode\s*=\s*)(\d+)(\s*)$")
POLA_UNRELEASED = re.compile(r"(?m)^## \[Unreleased\][ \t]*$")
POLA_BAGIAN = re.compile(r"(?m)^## \[")


def galat(pesan: str) -> None:
    print(f"[naik_versi] GALAT: {pesan}", file=sys.stderr)
    raise SystemExit(1)


def baca_gradle(akar: Path) -> tuple[str, str, int]:
    path = akar / BERKAS_GRADLE
    if not path.is_file():
        galat(f"{BERKAS_GRADLE} tidak ditemukan di {akar} — jalankan dari akar repositori atau isi --akar")
    teks = path.read_text(encoding="utf-8")
    nama = POLA_NAMA.findall(teks)
    kode = POLA_KODE.findall(teks)
    if len(nama) != 1:
        galat(f"{BERKAS_GRADLE} memuat {len(nama)} baris versionName bentuk \"X.Y.Z\" — "
              "harus tepat satu (sumber tunggal versi)")
    if len(kode) != 1:
        galat(f"{BERKAS_GRADLE} memuat {len(kode)} baris versionCode — harus tepat satu")
    return teks, nama[0][1], int(kode[0][1])


def naikkan(versi: str, jenis: str) -> str:
    mayor, minor, patch = (int(bil) for bil in versi.split("."))
    if jenis == "minor":
        return f"{mayor}.{minor + 1}.0"
    return f"{mayor}.{minor}.{patch + 1}"


def catatan_otomatis(versi: str, sha: str | None) -> str:
    asal = f"commit `{sha}`" if sha else "HEAD `main`"
    return (
        f"**Rilis otomatis** dari {asal}: seluruh pipeline push `main` (CI lint+unit+build, "
        "E2E emulator, workflow-lint) telah lulus sebelum versi dinaikkan. Versi dan changelog "
        "ditulis oleh `tools/naik_versi.py` melalui `.github/workflows/auto-release.yml`; "
        f"catatan lengkap perubahan dihasilkan otomatis pada GitHub Release tag `v{versi}`."
    )


def sunting_changelog(akar: Path, versi: str, tanggal: str, sha: str | None) -> None:
    path = akar / BERKAS_CHANGELOG
    if not path.is_file():
        galat(f"{BERKAS_CHANGELOG} tidak ditemukan di {akar}")
    teks = path.read_text(encoding="utf-8")
    m = POLA_UNRELEASED.search(teks)
    if m is None:
        galat(f"{BERKAS_CHANGELOG} tidak lagi memuat bagian '## [Unreleased]'")
    sisa = teks[m.end():]
    berikut = POLA_BAGIAN.search(sisa)
    isi = (sisa[: berikut.start()] if berikut else sisa).strip("\n")
    ekor = sisa[berikut.start():] if berikut else ""

    bagian = [f"## [{versi}] - {tanggal}", ""]
    if isi:
        bagian += [isi, ""]
    bagian.append(catatan_otomatis(versi, sha))

    if ekor:
        teks_baru = teks[: m.end()] + "\n\n" + "\n".join(bagian) + "\n\n" + ekor
    else:
        # Tanpa bagian versi lama setelahnya, berkas harus tetap berakhir tepat satu baris baru.
        teks_baru = teks[: m.end()] + "\n\n" + "\n".join(bagian) + "\n"
    path.write_text(teks_baru, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Naikkan versi rilis di tempat (versionName/versionCode + CHANGELOG.md).",
    )
    parser.add_argument("--jenis", choices=["minor", "patch"], default="patch",
                        help="jenis kenaikan: minor → X.(Y+1).0, patch → X.Y.(Z+1) (bawaan: patch)")
    parser.add_argument("--commit-sha", default=None,
                        help="SHA yang lolos pipeline; ditulis ke catatan changelog (bawaan: tanpa SHA)")
    parser.add_argument("--tanggal", default=None,
                        help="tanggal rilis YYYY-MM-DD (bawaan: hari ini UTC)")
    parser.add_argument("--akar", default=".", help="akar repositori (bawaan: direktori kerja)")
    arg = parser.parse_args()

    akar = Path(arg.akar)
    tanggal = arg.tanggal or datetime.now(timezone.utc).strftime("%Y-%m-%d")
    if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", tanggal):
        galat(f"--tanggal harus YYYY-MM-DD, diterima: {tanggal!r}")

    teks, versi_lama, kode_lama = baca_gradle(akar)
    versi_baru = naikkan(versi_lama, arg.jenis)
    kode_baru = kode_lama + 1

    teks_baru = POLA_NAMA.sub(lambda m: m.group(1) + versi_baru + m.group(3), teks, count=1)
    teks_baru = POLA_KODE.sub(lambda m: m.group(1) + str(kode_baru) + m.group(3), teks_baru, count=1)
    (akar / BERKAS_GRADLE).write_text(teks_baru, encoding="utf-8")

    sunting_changelog(akar, versi_baru, tanggal, arg.commit_sha)

    print(f"versi={versi_baru}")
    print(f"kode={kode_baru}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
