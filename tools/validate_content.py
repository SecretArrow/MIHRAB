#!/usr/bin/env python3
"""Validasi konten keagamaan sesuai SOURCES.md.

Aturan:
- Setiap JSON di app/src/main/assets/content/ harus JSON valid.
- Tidak boleh ada kunci objek yang ditulis dua kali (nilainya hilang tanpa jejak).
- Setiap "entri" (objek yang memuat teks keagamaan) sebaiknya punya `sumber_url`
  yang domainnya termasuk whitelist resmi.
- Nilai yang tampak URL (kunci apa pun berisi "sumber") harus domain resmi.

Mode:
  (default)  -> galat hanya untuk JSON rusak / domain tidak resmi. Entri tanpa
                sumber_url dilaporkan sebagai peringatan (tidak menggagalkan).
  --strict   -> entri tanpa sumber_url juga menggagalkan (dipakai setelah
                seluruh konten dilengkapi).

Sumber resmi: daftar tunggalnya ada di `ContentValidator.allowedHosts` (Kotlin) dan dibaca dari
sana — lihat `load_allowed_hosts()`.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
CONTENT_DIR = ROOT / "app" / "src" / "main" / "assets" / "content"
CONTENT_VALIDATOR = (ROOT / "app" / "src" / "main" / "java" / "id" / "or" / "kemenag"
                     / "yasintahlil" / "core" / "util" / "ContentValidator.kt")

# Pola pengambil daftar host dari sumber Kotlin. Ditulis persis sama di penjaga Kotlin
# (`SumberResmiSatuTempatTest`) supaya kedua sisi tidak dapat memakai pola yang berbeda.
ALLOWED_HOSTS_RE = r'allowedHosts\s*=\s*listOf\(([^)]*)\)'


def load_allowed_hosts() -> list[str]:
    """Daftar host resmi, dibaca dari satu-satunya tempat yang menyatakannya.

    Aplikasi menolak berkas konten yang `sumber_url`-nya di luar daftar ini saat runtime
    (`ContentValidator.isOfficialSource` dipakai `ContentRepo` dan `BundelKonten`); gate ini
    harus menilai dengan daftar yang **sama**. Dua salinan daftar berarti berkas dapat lolos
    gate tetapi ditolak aplikasi — pembaruan tampak terpasang, setiap berkasnya dilewati, dan
    aplikasi diam-diam kembali memakai konten bawaan tanpa satu pun pesan yang menyebut sebabnya.
    Sebaliknya, host yang ditambahkan di Kotlin tanpa ikut di sini membuat gate gagal pada berkas
    yang justru sah.

    Sengaja **gagal keras** bila daftarnya tidak terbaca: gate yang diam-diam memakai daftar
    kosong akan meloloskan segalanya.
    """
    try:
        sumber = CONTENT_VALIDATOR.read_text(encoding="utf-8")
    except OSError as e:
        raise SystemExit(f"[validate_content] tidak dapat membaca {CONTENT_VALIDATOR}: {e}")
    m = re.search(ALLOWED_HOSTS_RE, sumber)
    if not m:
        raise SystemExit(f"[validate_content] 'allowedHosts = listOf(...)' tidak ditemukan di "
                         f"{CONTENT_VALIDATOR}")
    hosts = re.findall(r'"([^"]+)"', m.group(1))
    if not hosts:
        raise SystemExit(f"[validate_content] daftar host resmi kosong di {CONTENT_VALIDATOR}")
    return hosts


ALLOWED_HOSTS = load_allowed_hosts()
ENTRY_KEYS = {"arab", "latin", "terjemahan", "judul", "nama", "dalil"}


# --- Aturan bundel yang dibaca dari sumber Kotlin -------------------------------------------
#
# Sebagian aturan bundel dipakai dua kali: oleh aplikasi saat memasang bundel yang dipilih pengguna
# sendiri, dan oleh gate CI. Dahulu keduanya menyimpan salinannya masing-masing — satu di Kotlin,
# satu di Python — dan tak ada apa pun yang membandingkannya. Dua salinan berarti dua kebijakan,
# dan bedanya sunyi: bundel yang lolos gate dapat ditolak aplikasi saat dipasang (pembaruan tampak
# berhasil tetapi tidak mengubah apa pun), atau sebaliknya prosa penunjuk yang seharusnya ditolak
# lolos sampai **ditampilkan kepada pengguna sebagai bacaan** — tepat yang dilarang aturan
# "jangan karang teks agama".
#
# Karena itu gate **membaca** nilainya dari `BundelKonten.kt` alih-alih menyimpannya. Pola
# pengambilnya ditulis persis sama di penjaga Kotlin (`AturanBundelSatuTempatTest`), sehingga pola
# yang berbeda antar-keduanya tidak dapat lolos tanpa ketahuan.

BUNDEL_KONTEN = (ROOT / "app" / "src" / "main" / "java" / "id" / "or" / "kemenag"
                 / "yasintahlil" / "core" / "data" / "BundelKonten.kt")

# Aturan teks Arab (rentang blok, rentang huruf, tanda harakat, panjang kata minimum) dipakai tiga
# kali dengan nilai yang sama: `BundelKonten` saat memasang bundel, `ArabicTextTest` saat memeriksa
# assets, dan gate ini. Rentang Unicode tidak dapat dibaca sekilas, sehingga salinan yang tertinggal
# tidak pernah tampak salah bagi peninjau — karena itu nilainya dibaca dari satu tempat.
TEKS_ARAB = (ROOT / "app" / "src" / "main" / "java" / "id" / "or" / "kemenag"
             / "yasintahlil" / "core" / "util" / "TeksArab.kt")

# Literal string Kotlin, termasuk escape di dalamnya (`"\\"`), supaya `PEMISAH_NAMA_DILARANG`
# yang memuat garis miring terbalik pun terbaca utuh.
_TEKS_KOTLIN = r'"((?:[^"\\]|\\.)*)"'
_ESCAPE_KOTLIN = {"\\": "\\", '"': '"', "n": "\n", "t": "\t", "r": "\r"}

# Rentang dan kode Unicode Kotlin: `0x0600 to 0x06FF` dan `0x0670`.
_RENTANG_KOTLIN = r"0x([0-9A-Fa-f]+)\s+to\s+0x([0-9A-Fa-f]+)"
_KODE_KOTLIN = r"0x([0-9A-Fa-f]+)"


def _unescape_kotlin(teks: str) -> str:
    """Isi literal string Kotlin → teks sebenarnya (`\\\\` → `\\`, `\\"` → `"`)."""
    keluar: list[str] = []
    i = 0
    while i < len(teks):
        if teks[i] == "\\" and i + 1 < len(teks):
            keluar.append(_ESCAPE_KOTLIN.get(teks[i + 1], teks[i + 1]))
            i += 2
        else:
            keluar.append(teks[i])
            i += 1
    return "".join(keluar)


def _cari(sumber: str, pola: str, nama: str, berkas: Path = BUNDEL_KONTEN) -> str:
    m = re.search(pola, sumber)
    if not m:
        raise SystemExit(f"[validate_content] '{nama}' tidak ditemukan di {berkas}")
    return m.group(1)


def baca_teks(sumber: str, nama: str, berkas: Path = BUNDEL_KONTEN) -> str:
    """Nilai `const val <nama> = "…"` pada sumber Kotlin."""
    return _unescape_kotlin(_cari(sumber, rf"\b{nama}\s*=\s*{_TEKS_KOTLIN}", nama, berkas))


def baca_angka(sumber: str, nama: str, berkas: Path = BUNDEL_KONTEN) -> int:
    """Nilai `const val <nama> = 1` pada sumber Kotlin."""
    return int(_cari(sumber, rf"\b{nama}\s*=\s*(\d+)", nama, berkas))


def baca_daftar_teks(sumber: str, nama: str, berkas: Path = BUNDEL_KONTEN) -> list[str]:
    """Nilai `val <nama> = listOf("a", "b")` pada sumber Kotlin."""
    isi = _cari(sumber, rf"\b{nama}\s*=\s*listOf\(([^)]*)\)", nama, berkas)
    return [_unescape_kotlin(t) for t in re.findall(_TEKS_KOTLIN, isi)]


def baca_rentang(sumber: str, nama: str, berkas: Path = BUNDEL_KONTEN) -> list[tuple[int, int]]:
    """Nilai `val <nama> = listOf(0x0600 to 0x06FF, …)` pada sumber Kotlin."""
    isi = _cari(sumber, rf"\b{nama}\s*=\s*listOf\(([^)]*)\)", nama, berkas)
    rentang = [(int(a, 16), int(b, 16)) for a, b in re.findall(_RENTANG_KOTLIN, isi)]
    if not rentang:
        raise SystemExit(
            f"[validate_content] '{nama}' tidak memuat rentang '0x… to 0x…' di {berkas}")
    return rentang


def baca_kode(sumber: str, nama: str, berkas: Path = BUNDEL_KONTEN) -> list[int]:
    """Nilai `val <nama> = listOf(0x0670, 0x0653, …)` pada sumber Kotlin."""
    isi = _cari(sumber, rf"\b{nama}\s*=\s*listOf\(([^)]*)\)", nama, berkas)
    kode = [int(x, 16) for x in re.findall(_KODE_KOTLIN, isi)]
    if not kode:
        raise SystemExit(f"[validate_content] '{nama}' tidak memuat kode '0x…' di {berkas}")
    return kode


def _sumber(berkas: Path) -> str:
    """Isi sebuah berkas sumber Kotlin. Sengaja **gagal keras** bila tidak terbaca: gate yang
    diam-diam memakai nilai bawaan akan meloloskan berkas yang seharusnya ditolak."""
    try:
        return berkas.read_text(encoding="utf-8")
    except OSError as e:
        raise SystemExit(f"[validate_content] tidak dapat membaca {berkas}: {e}")


SUMBER_BUNDEL = _sumber(BUNDEL_KONTEN)

# Aturan nama berkas konten — dipakai `tools/validate_bundle.py` lewat modul ini.
PANJANG_NAMA_MIN = baca_angka(SUMBER_BUNDEL, "PANJANG_NAMA_MIN")
PANJANG_NAMA_MAKS = baca_angka(SUMBER_BUNDEL, "PANJANG_NAMA_MAKS")
AKHIRAN_NAMA = baca_teks(SUMBER_BUNDEL, "AKHIRAN_NAMA")
AWALAN_NAMA_DILARANG = baca_daftar_teks(SUMBER_BUNDEL, "AWALAN_NAMA_DILARANG")
PEMISAH_NAMA_DILARANG = baca_daftar_teks(SUMBER_BUNDEL, "PEMISAH_NAMA_DILARANG")
PLACEHOLDER_MARKERS = tuple(baca_daftar_teks(SUMBER_BUNDEL, "PLACEHOLDER"))
# Field yang dinilai prosa penunjuk dan harakat — dibaca dari `BundelKonten.FIELD_TEKS`, sebab
# `docs/CONTENT_UPDATE.md` menyebut field yang sama kepada pembaca dan dahulu tak ada yang
# membandingkannya. Lihat catatan di atas berkas ini.
FIELD_TEKS = tuple(baca_daftar_teks(SUMBER_BUNDEL, "FIELD_TEKS"))

# Aturan teks Arab — dipakai `unvocalized_words` di bawah, dan dibaca dari `TeksArab.kt`.
SUMBER_TEKS_ARAB = _sumber(TEKS_ARAB)
RENTANG_ARAB = baca_rentang(SUMBER_TEKS_ARAB, "RENTANG_ARAB", TEKS_ARAB)
RENTANG_HURUF = baca_rentang(SUMBER_TEKS_ARAB, "RENTANG_HURUF", TEKS_ARAB)
RENTANG_HARAKAT = baca_rentang(SUMBER_TEKS_ARAB, "RENTANG_HARAKAT", TEKS_ARAB)
HARAKAT_TUNGGAL = baca_kode(SUMBER_TEKS_ARAB, "HARAKAT_TUNGGAL", TEKS_ARAB)
MIN_HURUF = baca_angka(SUMBER_TEKS_ARAB, "MIN_HURUF", TEKS_ARAB)


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

    # Bacaan setelah adzan: setiap entri wajib menyatakan **kapan** dibaca — plan.txt §"Bacaan
    # Setelah Adzan": "Penjelasan waktu membacanya". Tanpa `waktu`, pengguna hanya tahu urutannya,
    # bukan kapan tiap bacaan dikerjakan — dan urutan saja tidak menjawab "sekarang baca yang mana".
    for k in ("menjawab_adzan", "sholawat_setelah_adzan", "doa_setelah_adzan"):
        entri = data.get(k)
        if isinstance(entri, dict) and not str(entri.get("waktu", "")).strip():
            errs.append(f"{name}: '{k}' tanpa 'waktu' (penjelasan kapan bacaan ini dibaca)")

    return errs


def check_placeholders(name: str, node, where="") -> list[str]:
    """Teks Arab/Latin tidak boleh berisi prosa penunjuk (placeholder) — lihat plan.txt:
    'Jangan menghasilkan teks Arab secara acak atau merekonstruksi bacaan yang belum
    diverifikasi'. Bila teks asli diambil dari berkas rujukan, kosongkan field inline-nya.

    Daftar prosanya ada di `PLACEHOLDER_MARKERS`, dibaca dari `BundelKonten.PLACEHOLDER` — lihat
    catatan di bagian atas berkas ini. Field yang dinilai (`FIELD_TEKS`) pun dibaca dari sana, agar
    field yang ditambahkan di satu sisi saja tidak membuat pemeriksaan ini diam-diam menyempit."""
    errs: list[str] = []
    if isinstance(node, dict):
        for k, v in node.items():
            if k in FIELD_TEKS and isinstance(v, str):
                low = v.lower()
                for m in PLACEHOLDER_MARKERS:
                    if m in low:
                        errs.append(f"{name}: {where}.{k} berisi placeholder '{m}'")
            errs.extend(check_placeholders(name, v, f"{where}.{k}"))
    elif isinstance(node, list):
        for i, v in enumerate(node):
            errs.extend(check_placeholders(name, v, f"{where}[{i}]"))
    return errs


# Harakat/baris dan blok Arab \u2014 nilainya dibaca dari `TeksArab.kt` (lihat catatan di atas),
# bukan disimpan di sini.
HARAKAT = set(HARAKAT_TUNGGAL)
for _awal, _akhir in RENTANG_HARAKAT:
    HARAKAT |= set(range(_awal, _akhir + 1))

ARABIC_RUN = re.compile("[" + "".join(
    f"\\u{awal:04x}-\\u{akhir:04x}" for awal, akhir in RENTANG_ARAB) + "]+")


def unvocalized_words(text: str) -> list[str]:
    """Kata Arab (>= `MIN_HURUF` huruf) yang sama sekali tanpa harakat.

    Teks Arab pada aplikasi harus berharakat agar dapat dibaca pemula; kata tanpa satu pun tanda
    baris hampir pasti salah ketik atau teks mentah yang belum divokalkan. Huruf mad tidak dihitung
    sebagai penanda \u2014 kata yang hanya memuat huruf mad tetap dianggap tanpa harakat.

    Aturannya dipakai bersama `BundelKonten` dan `ArabicTextTest`; lihat catatan di atas berkas ini.
    """
    out: list[str] = []
    for run in ARABIC_RUN.findall(text):
        letters = [c for c in run if any(a <= ord(c) <= b for a, b in RENTANG_HURUF)]
        if len(letters) >= MIN_HURUF and not any(ord(c) in HARAKAT for c in run):
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


class _KunciGanda(Exception):
    def __init__(self, kunci: str, jumlah: int):
        super().__init__(kunci)
        self.kunci = kunci
        self.jumlah = jumlah


def _cek_pasangan(pairs):
    hitung: dict[str, int] = {}
    for k, _ in pairs:
        hitung[k] = hitung.get(k, 0) + 1
    for k, n in hitung.items():
        if n > 1:
            raise _KunciGanda(k, n)
    return dict(pairs)


def check_duplicate_keys(name: str, text: str) -> list[str]:
    """Kunci objek JSON yang ditulis dua kali dalam satu objek.

    JSON tidak melarangnya, tetapi parser mana pun (termasuk `org.json` di aplikasi)
    hanya menyimpan **yang terakhir** — sehingga nilai pertama hilang tanpa galat,
    tanpa peringatan, dan tanpa jejak. Persis itu yang pernah terjadi pada
    `adzan.json`: penjelasan lengkap cara menjawab adzan tertimpa versi pendek yang
    tertinggal, dan yang dibaca pengguna adalah versi yang lebih sedikit menjelaskan.
    Karena kerusakannya sunyi, ia harus ditolak di sini.
    """
    try:
        json.loads(text, object_pairs_hook=_cek_pasangan)
    except _KunciGanda as e:
        return [f"{name}: kunci '{e.kunci}' ditulis {e.jumlah}x dalam satu objek "
                f"(hanya yang terakhir terbaca)"]
    return []


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
        teks = path.read_text(encoding="utf-8")
        errors.extend(check_duplicate_keys(rel.name, teks))
        try:
            data = json.loads(teks)
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

        # Nilai apa pun yang tampak URL di kunci berisi 'sumber'.
        #
        # Termasuk URL yang berada **di dalam daftar**: `sumber_tambahan` di
        # `sholat_ghaib.json`/`sholat_jenazah.json` berbentuk list, dan dahulu elemen
        # list tidak pernah diperiksa karena penelusuran hanya menangani dict. Akibatnya
        # domain non-resmi di sana lolos gate — padahal layar Tentang menyatakan kepada
        # pengguna bahwa "berkas konten yang menyebut domain lain ditolak saat dibangun".
        # Kunci induk dibawa turun ke elemen list supaya `sumber_tambahan[i]` tetap
        # dikenali sebagai nilai bersumber.
        def scan_urls(n, where, kunci_induk=""):
            if isinstance(n, dict):
                for k, v in n.items():
                    scan_urls(v, f"{where}.{k}", k)
            elif isinstance(n, list):
                for i, v in enumerate(n):
                    scan_urls(v, f"{where}[{i}]", kunci_induk)
            elif isinstance(n, str) and n.strip().startswith("http") and "sumber" in kunci_induk.lower():
                if not is_official(n.strip()):
                    errors.append(f"{rel}: {where} domain tidak resmi — {n}")

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
