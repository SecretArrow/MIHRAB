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
import re
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


def check_schema(name: str, data) -> list[str]:
    """Validasi struktur per jenis file konten. Kembalikan daftar galat."""
    errs: list[str] = []
    if not isinstance(data, dict):
        return errs

    if "verses" in data:  # surat/ayat Al-Qur'an
        verses = data["verses"]
        if not isinstance(verses, list) or not verses:
            errs.append(f"{name}: 'verses' kosong")
            return errs
        nums = []
        for i, v in enumerate(verses):
            for k in ("nomor", "arab", "terjemahan"):
                if not v.get(k) and v.get(k) != 0:
                    errs.append(f"{name}: ayat[{i}] tanpa '{k}'")
            if v.get("arab") is not None:
                nums.append(v.get("nomor"))
        if data.get("ayat_count") and data["ayat_count"] != len(verses):
            errs.append(f"{name}: ayat_count={data['ayat_count']} != jumlah verses={len(verses)}")
        if nums and len(set(nums)) != len(nums):
            errs.append(f"{name}: nomor ayat duplikat")

    if "bagian" in data:  # tahlil
        bagian = data["bagian"]
        if not isinstance(bagian, list) or not bagian:
            errs.append(f"{name}: 'bagian' kosong")
        for i, b in enumerate(bagian):
            for k in ("id", "judul"):
                if not b.get(k):
                    errs.append(f"{name}: bagian[{i}] tanpa '{k}'")

    if "susunan" in data:  # deskriptor susunan tahlil
        for i, s in enumerate(data["susunan"]):
            if not s.get("id") or not isinstance(s.get("bagian"), list):
                errs.append(f"{name}: susunan[{i}] butuh 'id' dan 'bagian' (list)")

    if "kota" in data:  # koordinat
        for i, k in enumerate(data["kota"]):
            if not k.get("nama") or k.get("lat") is None or k.get("lon") is None:
                errs.append(f"{name}: kota[{i}] butuh nama/lat/lon")

    if "kelompok" in data:  # dzikir: setiap entri wajib menyebut sumbernya sendiri
        # sumber_url tingkat berkas terlalu umum (mis. "https://sunnah.com/"); tanpa sumber
        # per-entri, hadis daif dan ayat Qur'an tidak dapat dibedakan asalnya oleh pengguna.
        for gi, g in enumerate(data.get("kelompok") or []):
            for ii, it in enumerate((g or {}).get("items") or []):
                label = it.get("judul") or f"{gi}.{ii}"
                if not str(it.get("sumber", "")).strip():
                    errs.append(f"{name}: kelompok[{gi}] entri '{label}' tanpa 'sumber' per-entri")

    return errs


PLACEHOLDER_MARKERS = (
    "lihat rujukan file", "lihat rujukan berkas", "dibaca penuh dari mushaf",
    "todo", "lorem", "... (sesuai", "lafaz niat per sholat",
)


def check_placeholders(name: str, node, where="") -> list[str]:
    """Teks Arab/Latin tidak boleh berisi prosa penunjuk (placeholder) — lihat plan.txt:
    'Jangan menghasilkan teks Arab secara acak atau merekonstruksi bacaan yang belum
    diverifikasi'. Bila teks asli diambil dari berkas rujukan, kosongkan field inline-nya."""
    errs: list[str] = []
    if isinstance(node, dict):
        for k, v in node.items():
            if k in ("arab", "latin") and isinstance(v, str):
                low = v.lower()
                for m in PLACEHOLDER_MARKERS:
                    if m in low:
                        errs.append(f"{name}: {where}.{k} berisi placeholder '{m}'")
            errs.extend(check_placeholders(name, v, f"{where}.{k}"))
    elif isinstance(node, list):
        for i, v in enumerate(node):
            errs.extend(check_placeholders(name, v, f"{where}[{i}]"))
    return errs


# Harakat/baris: fathatan..sukun + alef khanjariyah dan tanda maddah.
HARAKAT = set(range(0x064B, 0x0653)) | {0x0670, 0x0653, 0x0654, 0x0655, 0x0656}
ARABIC_RUN = re.compile("[\u0600-\u06FF\u0750-\u077F]+")


def unvocalized_words(text: str) -> list[str]:
    """Kata Arab (>= 3 huruf) yang sama sekali tanpa harakat.

    Teks Arab pada aplikasi harus berharakat agar dapat dibaca pemula; kata
    tanpa satu pun tanda baris hampir pasti salah ketik atau teks mentah yang
    belum divokalkan. Huruf mad (ا و ي ى) tidak dihitung sebagai penanda.
    """
    out: list[str] = []
    for run in ARABIC_RUN.findall(text):
        letters = [c for c in run if 0x0621 <= ord(c) <= 0x064A or 0x0671 <= ord(c) <= 0x06D3]
        if len(letters) >= 3 and not any(ord(c) in HARAKAT for c in run):
            out.append(run)
    return out


def check_harakat(name: str, node, where="") -> list[str]:
    """Tolak teks Arab tanpa harakat (di kunci mana pun, termasuk catatan)."""
    errs: list[str] = []
    if isinstance(node, dict):
        for k, v in node.items():
            errs.extend(check_harakat(name, v, f"{where}.{k}"))
    elif isinstance(node, list):
        for i, v in enumerate(node):
            errs.extend(check_harakat(name, v, f"{where}[{i}]"))
    elif isinstance(node, str):
        bad = unvocalized_words(node)
        if bad:
            errs.append(f"{name}: {where.lstrip('.')} memuat teks Arab tanpa harakat — {bad}")
    return errs


# Kategori sumber bacaan — plan.txt: "Bedakan bacaan yang bersumber dari Al-Qur'an, hadis,
# dzikir umum, dan tradisi lokal." Dinyatakan sekali di tingkat berkas dan boleh ditimpa
# per entri (mis. `sholat_bacaan.json` mayoritas hadis, tetapi lafaz niatnya tradisi).
KATEGORI = ("quran", "hadis", "dzikir", "tradisi")


def kategori_dari(node) -> str:
    """Nilai `kategori` sebuah objek, atau "" bila tidak ada."""
    k = node.get("kategori") if isinstance(node, dict) else None
    return k.strip() if isinstance(k, str) else ""


def entri_ber_arab(node, path=""):
    """(path, objek) untuk setiap objek yang memuat teks Arab non-kosong."""
    if isinstance(node, dict):
        if str(node.get("arab", "")).strip():
            yield path, node
        for k, v in node.items():
            yield from entri_ber_arab(v, f"{path}.{k}")
    elif isinstance(node, list):
        for i, v in enumerate(node):
            yield from entri_ber_arab(v, f"{path}[{i}]")


def check_kategori(name: str, data) -> list[str]:
    """Berkas yang memuat bacaan ber-Arab wajib menyatakan kategori sumbernya.

    Tanpa kategori, pengguna tak dapat membedakan ayat Al-Qur'an, hadis, dzikir umum, dan
    tradisi lokal — pembedaan yang diminta eksplisit oleh plan.txt. Kategori diambil dari
    tingkat berkas, atau dari tiap entri bila berkasnya bercampur.
    """
    errs: list[str] = []
    if not isinstance(data, dict):
        return errs

    # Seluruh nilai `kategori` di berkas ini (di kedalaman mana pun), sekali per lokasi.
    salah: dict[str, str] = {}

    def scan_nilai(node, path=""):
        if isinstance(node, dict):
            k = kategori_dari(node)
            if k and k not in KATEGORI:
                salah.setdefault(path.lstrip("."), k)
            for key, v in node.items():
                scan_nilai(v, f"{path}.{key}")
        elif isinstance(node, list):
            for i, v in enumerate(node):
                scan_nilai(v, f"{path}[{i}]")

    scan_nilai(data)
    for path, k in salah.items():
        errs.append(f"{name}: {path} kategori '{k}' tidak dikenal "
                    f"(pilihan: {', '.join(KATEGORI)})")

    berarab = list(entri_ber_arab(data))
    if berarab and not kategori_dari(data) and not any(kategori_dari(n) for _, n in berarab):
        errs.append(f"{name}: memuat bacaan ber-Arab tetapi tanpa 'kategori' sumber "
                    f"(pilihan: {', '.join(KATEGORI)})")
    return errs


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

        # --- Pemeriksaan skema struktur per jenis file ---
        errors.extend(check_schema(rel.name, data))
        errors.extend(check_placeholders(rel.name, data))

        # --- Semua teks Arab wajib berharakat ---
        errors.extend(check_harakat(rel.name, data))

        # --- Kategori sumber bacaan wajib dinyatakan ---
        errors.extend(check_kategori(rel.name, data))

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
