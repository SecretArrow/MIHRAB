# SUMBER RESMI — WAJIB DIRUJUK, DILARANG KARANG TEKS AGAMA

Dokumen ini adalah satu-satunya daftar sumber yang sah untuk konten aplikasi.

## 1. Teks Al-Qur'an / Yasin + Terjemahan
- https://quran.kemenag.go.id/ (Mushaf Standar Indonesia, LPMQ Kemenag RI)
- https://quran-api.lpmqkemenag.id/ (Qur'an Kemenag API — daftar + surat ke lajnah@kemenag.go.id)
- https://quranindonesia.kemenag.go.id/ (aplikasi rujukan resmi)
- https://lajnah.kemenag.go.id/ (unduhan mushaf resmi)
- Edisi terjemah: Kemenag 2019. Yasin = 83 ayat. File: `app/src/main/assets/content/yasin_full.json`
- Penyegaran teks: `tools/fetch_quran_kemenag.py` (alat pengembang, dijalankan manual) mengambil
  `arab` (MSI), `latin` (transliterasi resmi Kemenag), dan `terjemahan` dari layanan yang dipakai
  situs resmi `quran.kemenag.go.id`. Terjemahan/transliterasi non-resmi sebelumnya (Tanzil,
  api.quran.com) sudah tidak dipakai lagi.
- Atribusi di About: "Sumber teks: Lajnah Pentashihan Mushaf Al-Qur'an (LPMQ) Kemenag RI"

## 2. Jadwal Sholat
- https://sihat.kemenag.dev/ (SIHAT Ditjen Bimas Islam — metode baku Kemenag per kab/kota)
- https://sihat.kemenag.dev/pengembang (API resmi: Bearer token + IP whitelist, rate-limited, wajib cantum "Kementerian Agama RI")
- https://bimasislam.kemenag.go.id/jadwalshalat (fallback bulanan)
- Fallback offline: algoritma Astronomical Algorithms (Jean Meeus) + Ilmu Falak (Muhyidin Khazin), label "perkiraan lokal — verifikasi SIHAT"

## 3. Kalender Hijriah
- https://sihat.kemenag.dev/kalender-hijriah (Kalender Hijriah Indonesia Bimas Islam)
- PDF Kalender 2026 + Sidang Isbat (1 Ramadan 1447H = 19 Feb 2026, 96 titik rukyat)
- Pendamping: https://hilal.bmkg.go.id/ (hisab + rukyat BMKG)

## 4. Arah Kiblat
- https://sihat.kemenag.dev/arah-kiblat (azimut busur terpendek dari utara sejati)
- https://sihat.kemenag.dev/e-kiblat (sertifikat via Kemenag Kab/Kota, teodolit)
- Koordinat Ka'bah resmi: 21°25'21.0"U 39°49'34.3"T
- Rashdul kiblat 2x setahun (bayangan tongkat) sebagai fallback tanpa sensor

## 5. Panduan Sholat + Sholat Ghaib
- Program Fasolatan / Ngaji Fasolatan Ditjen Bimas Islam (launch 24 Jun 2025): kerangka wudhu, tayamum, sholat, imam-makmum
- Verifikasi hadis: https://sunnah.com/ (Bukhari 756/1335, Muslim 394, Musnad Syafi'i 588)
- Sholat Ghaib: imbauan resmi Kemenag (Cianjur 2022, Turki-Suriah 2023, Maroko-Libya 2023); dalil primer HR Muttafaq Alaih (Abu Hurairah, sholat Najasyi 4 takbir); penjelasan syarat + kritik sanad via NU Online (hanya Najasyi yang sahih)
- Perbedaan mazhab wajib tampil: Syafi'i/Hambali membolehkan; Hanafi/Maliki masyhur membatasi. Default Indonesia = Syafi'i.

## 6. Aturan konten
- Setiap entri JSON wajib: `sumber_url` — domain resmi, dan **gagal** di mode `--strict`.
- Aturan tambahan per jenis entri: entri dzikir wajib `sumber` per-entri; bacaan setelah adzan wajib `waktu`.
- **Tidak diperiksa mesin**: `edisi` dan `catatan_mazhab` — metadata provenans di tingkat berkas/bagian
  yang menjelaskan asal-usul. Keduanya tak pernah digagalkan CI, jadi kelengkapannya harus diperiksa
  manusia saat meninjau; jangan mengandalkan gate untuk menangkap kutipan yang hilang.
  - Karena itu keduanya **wajib dibaca dan ditampilkan** oleh layar yang bersangkutan, bukan
    diringkas ulang di kode: `sholat_daftar.json#catatan_mazhab` oleh layar rincian langkah sholat,
    `wudhu.json#edisi` oleh layar Wudhu, `tahlil.json#edisi` oleh layar Tahlil (sebagai sumber
    cadangan tiap susunan), `adzan.json#iqomah#catatan_mazhab` oleh layar Iqomah. Penjaganya
    `AtribusiModulTest`, `AtribusiSusunanTahlilTest`, dan `AtribusiLafazAdzanTest`; dahulu
    `catatan_mazhab` dan `edisi` tak pernah terbaca sehingga layar menampilkan parafrase yang
    menyimpang — termasuk menyatakan *qunut Subuh sunnah* padahal berkasnya mencatatnya sebagai
    ketentuan yang diperselisihkan.
  - Satu keterangan, satu rumah: `edisi`/`catatan_*` tingkat modul **tidak boleh disalin** ke berkas
    lain, sebab salinan di berkas yang tak dibaca kode tak akan pernah tampil — dan bila ejaannya
    berbeda, justru ejaan yang dibaca yang menentukan apa yang pengguna lihat. Berlaku juga **di
    dalam satu berkas**: catatan yang hanya berlaku untuk satu bagian tinggal di bagian itu
    (`adzan.json#adzan#catatan_subuh`, `adzan.json#iqomah#catatan_mazhab`), bukan dinaikkan ke
    tingkat modul.
- CI gagal jika `sumber_url` kosong atau domain di luar: `*.kemenag.go.id, *.kemenag.dev, sunnah.com, bmkg.go.id, nu.or.id` (penjelas saja).
- Dilarang generate teks Arab dengan AI. Dilarang karang hadis/sanad.
