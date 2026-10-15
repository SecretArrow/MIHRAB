#!/usr/bin/env python3
"""Ambil teks Al-Qur'an resmi dari layanan Qur'an Kemenag (quran.kemenag.go.id).

Alat pengembang — **tidak** dijalankan di CI dan **tidak** dipakai saat build.
Jalankan manual bila hendak menyegarkan teks Arab (Mushaf Standar Indonesia, MSI)
dan transliterasi Latin resmi Kemenag pada berkas konten.

    python3 tools/fetch_quran_kemenag.py            # perbarui berkas konten
    python3 tools/fetch_quran_kemenag.py --dry-run  # tampilkan ringkasan saja

Yang diperbarui: `arab`, `latin`, dan `terjemahan` — semuanya dari layanan resmi
Qur'an Kemenag, sehingga tidak ada lagi teks Al-Qur'an yang bersumber dari cermin
non-resmi. Penanda catatan kaki (mis. "Tuhan1)") dibuang dari terjemahan karena
aplikasi belum menampilkan catatan kaki; teks catatannya sendiri ada di field
`footnotes` API. Field provenans ikut disesuaikan agar jejak sumber terlihat di
berkas konten dan lolos `tools/validate_content.py`.

Sumber: SOURCES.md bagian 1 — quran.kemenag.go.id (MSI, LPMQ Kemenag RI).
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "app" / "src" / "main" / "assets" / "content"

# Layanan yang dipakai situs resmi quran.kemenag.go.id untuk memuat ayat.
API = "https://web-api.qurankemenag.net"
HEADERS = {
    "Origin": "https://quran.kemenag.go.id",
    "Referer": "https://quran.kemenag.go.id/",
    "Accept": "application/json",
    "User-Agent": "yasin-tahlil-content-refresh/1.0 (provenance tool)",
}

SUMBER_URL = "https://quran.kemenag.go.id/"
SUMBER_ARAB = "Mushaf Standar Indonesia (MSI) LPMQ Kemenag RI — quran.kemenag.go.id"
SUMBER_LATIN = "Transliterasi resmi Kemenag RI — quran.kemenag.go.id"
SUMBER_TERJEMAHAN = "Terjemah Kemenag edisi 2019 — quran.kemenag.go.id"
EDISI = ("Kemenag: teks Arab Mushaf Standar Indonesia (MSI), transliterasi Latin resmi, "
         "terjemah edisi 2019")
CATATAN = ("Teks Arab (MSI), transliterasi Latin, dan terjemahan diambil dari layanan resmi "
           "Qur'an Kemenag (quran.kemenag.go.id) lewat tools/fetch_quran_kemenag.py. Penanda "
           "catatan kaki dibuang karena aplikasi belum menampilkan catatan kaki.")

# berkas konten -> surah (seluruh ayat berkas diganti dari API)
TARGETS = {
    "yasin_full.json": 36,
    "surah_001.json": 1,
    "surah_112.json": 112,
    "surah_113.json": 113,
    "surah_114.json": 114,
}

# ayat tunggal: berkas -> (surah, ayah, id bagian bila di dalam "bagian")
# `tahlil_full.json` dahulu memuat `doa_penutup` = QS Al-Baqarah 201 secara berdiri sendiri;
# susunan Tahlil kini mengikuti NU Online dan doa itu hidup di dalam rangkaian doa penutupnya,
# sehingga berkas itu tidak lagi bisa ditimpa ayat tunggal (lihat docs/CONTENT_SOURCES.md).
FLAT_TARGETS = {
    "ayat_kursi.json": (2, 255, None),
}


def _get(path: str, **params) -> dict:
    query = "&".join(f"{k}={v}" for k, v in params.items())
    url = f"{API}/{path}" + (f"?{query}" if query else "")
    req = urllib.request.Request(url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:  # pesan ringkas, bukan traceback
        raise SystemExit(f"[fetch] HTTP {e.code} untuk {url}") from None
    except urllib.error.URLError as e:
        raise SystemExit(f"[fetch] gagal menghubungi {url}: {e.reason}") from None


def ayat(surah: int, start: int, limit: int) -> list[dict]:
    return _get("quran-ayah", surah=surah, start=start, limit=limit)["data"]


def terjemah(teks: str) -> str:
    """Buang penanda catatan kaki Kemenag (mis. 'Tuhan1)' → 'Tuhan') dan rapikan spasi."""
    bersih = re.sub(r"\s*\d+\)", "", teks or "")
    return re.sub(r"\s{2,}", " ", bersih).strip()


def provenans(node: dict) -> None:
    """Samakan field jejak sumber pada satu berkas/entri konten."""
    node["sumber_arab"] = SUMBER_ARAB
    node["sumber_latin"] = SUMBER_LATIN
    node["sumber_terjemahan"] = SUMBER_TERJEMAHAN
    node["sumber_url"] = SUMBER_URL
    node["edisi"] = EDISI
    node["catatan_verifikasi"] = CATATAN


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true", help="jangan tulis berkas")
    args = ap.parse_args()

    def simpan(path: Path, data: dict) -> None:
        if not args.dry_run:
            # indent=1 dan tanpa baris baru di akhir: mengikuti format berkas konten
            # yang ada agar diff hanya memuat baris yang benar-benar berubah.
            path.write_text(json.dumps(data, ensure_ascii=False, indent=1),
                            encoding="utf-8")

    for name, surah in TARGETS.items():
        path = CONTENT / name
        data = json.loads(path.read_text(encoding="utf-8"))
        verses = data["verses"]
        fetched = {a["ayah"]: a for a in ayat(surah, 0, len(verses) + 5)}
        if len(fetched) != len(verses):
            raise SystemExit(f"[fetch] {name}: API mengembalikan {len(fetched)} ayat, "
                             f"berkas memuat {len(verses)}")
        for v in verses:
            a = fetched.get(v["nomor"])
            if a is None:
                raise SystemExit(f"[fetch] {name}: ayat {v['nomor']} tidak ada di API")
            v["arab"] = a["arabic"].strip()
            v["latin"] = a["latin"].strip()
            v["terjemahan"] = terjemah(a["translation"])
        provenans(data)
        print(f"[fetch] {name}: {len(verses)} ayat dari surah {surah}")
        simpan(path, data)

    for name, (surah, ayah, bagian_id) in FLAT_TARGETS.items():
        path = CONTENT / name
        data = json.loads(path.read_text(encoding="utf-8"))
        node = data
        if bagian_id is not None:
            node = next(b for b in data["bagian"] if b["id"] == bagian_id)
        a = next(x for x in ayat(surah, ayah - 1, 1) if x["ayah"] == ayah)
        node["arab"] = a["arabic"].strip()
        node["latin"] = a["latin"].strip()
        node["terjemahan"] = terjemah(a["translation"])
        if bagian_id is None:
            provenans(data)
        print(f"[fetch] {name}: {surah}:{ayah}")
        simpan(path, data)

    print("[fetch] selesai" + (" (dry-run, tidak ada berkas ditulis)" if args.dry_run else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main())
