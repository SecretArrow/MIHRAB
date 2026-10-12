# Pembaruan konten (dengan validasi sumber)

`plan.txt`: *"Sediakan mekanisme pembaruan konten dengan validasi sumber."*

Aplikasi ini **offline-first**: seluruh konten keagamaan dikirim di dalam APK
(`app/src/main/assets/content/*.json`) dan tidak pernah diunduh dari jaringan — manifes bahkan
tidak meminta izin `INTERNET` (dijaga `OfflineManifestTest`). Pembaruan konten karena itu
berbentuk **berkas bundel yang dipilih pengguna sendiri**, bukan unduhan otomatis.

## Alur di aplikasi

1. Pengguna menyimpan berkas bundel (`.json`) dari sumber resmi di perangkat.
2. **Pengaturan → Pembaruan konten → Pilih berkas bundel** — berkas dibuka lewat pemilih berkas
   sistem (Storage Access Framework), jadi tidak perlu izin penyimpanan apa pun.
3. Seluruh isi bundel diperiksa ([`BundelKonten`](../app/src/main/java/id/or/kemenag/yasintahlil/core/data/BundelKonten.kt)).
   Bila ada satu berkas yang gagal, **seluruh bundel ditolak** — tidak ada pemasangan sebagian.
4. Bundel yang lolos ditulis ke `filesDir/content_update/` dan dibaca lebih dulu oleh
   `ContentRepo`; berkas bawaan tetap utuh di `assets` dan dipakai lagi setelah
   **Kembalikan ke konten bawaan**.

Lapisan pembaruan diperiksa ulang setiap kali dibaca: berkas yang rusak, hilang, atau kehilangan
`sumber_url` resmi otomatis diabaikan, sehingga aplikasi kembali ke konten bawaan yang sudah
diverifikasi — bukan menampilkan teks yang tak terverifikasi.

## Bentuk berkas bundel

```json
{
  "_bundel": {
    "skema": 1,
    "dibuat": "2026-02-19",
    "penerbit": "LPMQ Kemenag",
    "catatan": "Pembaruan teks Qur'an edisi 2026"
  },
  "berkas": {
    "yasin_full.json": {
      "sumber_url": "https://quran.kemenag.go.id/…",
      "kategori": "quran",
      "verses": [ { "nomor": 1, "arab": "…", "latin": "…", "terjemahan": "…" } ]
    }
  }
}
```

`berkas` memetakan nama berkas konten (persis seperti di `assets/content/`) ke dokumen lengkap
penggantinya. Nama harus nama polos berakhiran `.json`: tanpa pemisah direktori, tanpa awalan
titik, dan tanpa awalan `_` (awalan itu disisihkan untuk berkas internal, mis. metadata
pembaruan).

## Aturan yang diperiksa

| Aturan | Alasan |
| --- | --- |
| `sumber_url` ada dan domainnya resmi (host persis atau subdomain) | `plan.txt`: "pastikan semuanya berasal dari sumber resmi"; pencocokan suffix mencegah `kemenag.go.id.penyerang.example` lolos |
| Berkas ber-Arab menyatakan `kategori` yang dikenal (`quran`, `hadis`, `dzikir`, `tradisi`) | `plan.txt`: "Bedakan bacaan yang bersumber dari Al-Qur'an, hadis, dzikir umum, dan tradisi lokal" |
| Teks Arab wajib berharakat | `plan.txt`: "Verifikasi harakat, ejaan, nomor ayat, transliterasi, dan terjemahan" |
| Tidak ada prosa penunjuk di `arab`/`latin` (`lihat rujukan file`, `dibaca penuh dari mushaf`, `todo`, …) | `plan.txt`: "Jangan menghasilkan teks Arab secara acak atau merekonstruksi bacaan yang belum diverifikasi" |
| Nama berkas tidak menunjuk ke luar folder konten | mencegah path traversal menimpa berkas lain di penyimpanan aplikasi |
| Skema bundel sama dengan yang dipahami aplikasi | bundel format lama tidak dipasang setengah jalan |

Aturan ini **sama** dengan gate konten CI: `tools/validate_content.py` untuk konten bawaan, dan
`tools/validate_bundle.py` untuk isi bundel — skrip kedua memanggil fungsi pemeriksa dari skrip
pertama, jadi tidak ada aturan yang disalin ulang dan bisa berbeda.

## Sisi CI

`.github/workflows/ci.yml` (job `lint`) menjalankan dua fixture:

```bash
python3 tools/validate_bundle.py --strict tools/fixtures/bundel/sah.json
python3 tools/validate_bundle.py --strict --harus-gagal tools/fixtures/bundel/cacat.json
```

Fixture `sah.json` harus lolos; `cacat.json` (sumber tak resmi, kategori asing, teks Arab tanpa
harakat) harus ditolak — bila gate-nya longgar, `--harus-gagal` justru gagal dan CI merah.

## Yang tidak dilakukan aplikasi

- **Tidak mengunduh** bundel dari jaringan, dan tidak meminta izin `INTERNET`.
- **Tidak mengganti** konten yang sudah diverifikasi dengan teks buatan AI: validator menolak
  berkas tanpa sumber resmi, tetapi keputusan menyetujui isi tetap milik manusia
  (lihat [`SOURCES.md`](../SOURCES.md) dan [`docs/CONTENT_SOURCES.md`](CONTENT_SOURCES.md)).
- **Tidak menghapus** konten bawaan; `assets` selalu bisa dipakai lagi.
