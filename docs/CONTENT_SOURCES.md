# Provenans Konten — Status Sumber Resmi

Dokumen ini mencatat **asal setiap file konten** dan menandai bagian yang **masih perlu
diverifikasi/dicocokkan dengan sumber resmi**. Aturan lengkap ada di `SOURCES.md`.

Sumber resmi yang diizinkan (gate CI `tools/validate_content.py`):
`*.kemenag.go.id`, `*.kemenag.dev`, `sunnah.com`, `bmkg.go.id`, `nu.or.id`.

## Ringkasan status

| File | Isi | Sumber resmi | Status |
|---|---|---|---|
| `yasin_full.json` | Yasin 83 ayat (Arab, latin, terjemahan) | Terjemahan: **Kemenag 2019** (resmi). Arab: Utsmani. Latin: **non-resmi** | ⚠️ perlu cocokkan MSI & ganti latin |
| `surah_001/112/113/114.json` | Al-Fatihah, Al-Ikhlas, Al-Falaq, An-Nas | idem | ⚠️ perlu cocokkan MSI & ganti latin |
| `ayat_kursi.json` | Ayat Kursi (Al-Baqarah 255) | idem | ⚠️ perlu cocokkan MSI & ganti latin |
| `tahlil_full.json` | 15 bagian susunan tahlil | Qur'an Kemenag + hadis sunnah.com | ⚠️ sebagian Arab perlu cocokkan MSI |
| `tahlil.json` | Deskriptor susunan (id merujuk `tahlil_full`) | — | ✅ metadata |
| `adzan.json` | Lafaz adzan, iqomah, doa setelah adzan, menjawab adzan | **sunnah.com** (hadis sahih) + Fasolatan Kemenag | ✅ resmi |
| `dzikir.json` | Dzikir & doa harian | **sunnah.com** + Qur'an Kemenag | ✅ resmi |
| `sholat_bacaan.json` | Bacaan sholat (takbir..salam, qunut) | **sunnah.com** + Fasolatan Kemenag | ✅ resmi |
| `sholat_daftar.json` | Daftar sholat wajib/sunnah + niat | **sunnah.com** + Fasolatan Kemenag | ✅ resmi |
| `sholat_ghaib.json` | Sholat ghaib | **sunnah.com** (Bukhari 1335, Muslim 394) | ✅ resmi |
| `kota.json` | Koordinat kota | **sihat.kemenag.dev** (metode SIHAT) | ✅ resmi |

## Yang WAJIB dibereskan agar 100% sumber resmi

1. **Teks Arab Al-Qur'an** — saat ini memakai standar Utsmani (via Tanzil/alquran.cloud),
   bukan **Mushaf Standar Indonesia (MSI) LPMQ Kemenag**. Tindakan: unduh MSI resmi dari
   `lajnah.kemenag.go.id` / `quran.kemenag.go.id` lalu ganti `arab` tiap ayat.
2. **Transliterasi Latin** — saat ini dari `api.quran.com` (**berbahasa Inggris**, non-resmi).
   `plan.txt` meminta transliterasi Latin Indonesia. Tindakan: ganti dengan transliterasi
   Indonesia yang tepercaya (mis. terbitan LPMQ/Fasolatan) — **jangan** hasil generate AI.
3. **Terjemahan** — sudah Kemenag 2019 (resmi). Pertahankan.

## Kekurangan yang masih menunggu sumber resmi

`plan.txt` meminta beberapa bacaan yang **belum dapat dicantumkan** karena teks Arabnya belum
tersedia dari sumber di whitelist. Sesuai aturan proyek ("Jangan karang teks agama"), entri ini
**dibiarkan kosong, bukan dikarang** — dan dicatat di sini agar dapat diisi setelah diverifikasi
manusia dari sumber resmi.

| Yang diminta `plan.txt` | Status | Sumber yang diperlukan |
|---|---|---|
| Doa **keluar masjid** | ❌ belum ada di `dzikir.json` (yang ada: doa masuk masjid) | sunnah.com — HR Muslim/Abu Dawud |
| Doa **masuk rumah** | ❌ belum ada (yang ada: doa keluar rumah) | sunnah.com — HR Abu Dawud 5095 |
| Doa **bepergian (safar)** | ❌ belum ada | sunnah.com — HR Muslim 1342 |
| **Lafaz sholawat setelah adzan** | ⚠️ disebutkan di layar doa adzan, lafaznya belum dicantumkan | sunnah.com — HR Muslim 384 |
| **Niat sholat Jumat** | ⚠️ ditandai belum dicantumkan di panduan sholat | Fasolatan Kemenag / MSI LPMQ |
| **Susunan Tahlil kedua** (`plan.txt`: "jika memungkinkan") | ❌ hanya 1 susunan (`lazim_indonesia`); pemilih susunan sudah siap menerima lebih dari satu | rujukan tradisi yang jelas (mis. HPT Muhammadiyah) — jangan disusun sendiri |

> Catatan: teks Arab/hadis **tidak pernah** dibuat dengan AI. Bila sumber resmi belum
> tersedia, entri ditandai `catatan_verifikasi` dan dibiarkan apa adanya, bukan dikarang.
> Doa untuk orang yang meninggal dan doa ampunan sudah tersedia (modul Tahlil, sholat
> jenazah/ghaib, serta Istighfar/Sayyidul Istighfar di `dzikir.json`).

## Cara memeriksa

```bash
python3 tools/validate_content.py          # peringatan (non-blocking) — untuk pengembangan
python3 tools/validate_content.py --strict # gate: gagal bila ada entri tanpa sumber resmi
```

Mode `--strict` dijalankan di **setiap PR** (`ci.yml`) dan di rilis (`release.yml`), sehingga
entri tanpa sumber resmi tidak dapat masuk ke `main`.
