# Provenans Konten — Status Sumber Resmi

Dokumen ini mencatat **asal setiap file konten** dan menandai bagian yang **masih perlu
diverifikasi/dicocokkan dengan sumber resmi**. Aturan lengkap ada di `SOURCES.md`.

Sumber resmi yang diizinkan (gate CI `tools/validate_content.py`):
`*.kemenag.go.id`, `*.kemenag.dev`, `sunnah.com`, `bmkg.go.id`, `nu.or.id`.

## Ringkasan status

| File | Isi | Sumber resmi | Status |
|---|---|---|---|
| `yasin_full.json` | Yasin 83 ayat (Arab, latin, terjemahan) | **MSI LPMQ Kemenag** + transliterasi & terjemah Kemenag | ✅ resmi |
| `surah_001/112/113/114.json` | Al-Fatihah, Al-Ikhlas, Al-Falaq, An-Nas | idem | ✅ resmi |
| `ayat_kursi.json` | Ayat Kursi (Al-Baqarah 255) | idem | ✅ resmi |
| `tahlil_full.json` | 15 bagian susunan tahlil | Qur'an Kemenag (MSI) + hadis sunnah.com | ✅ resmi |
| `tahlil.json` | Deskriptor susunan (id merujuk `tahlil_full`) | — | ✅ metadata |
| `adzan.json` | Lafaz adzan, iqomah, doa setelah adzan, menjawab adzan | **sunnah.com** (hadis sahih) + Fasolatan Kemenag | ✅ resmi |
| `dzikir.json` | Dzikir & doa harian — 5 kelompok: *setelah sholat*, *pagi & petang*, *harian*, *doa khusus*, *doa memohon ampunan* | **sunnah.com** + Qur'an Kemenag (sumber per-entri) | ✅ resmi |
| `sholat_bacaan.json` | Bacaan sholat (takbir..salam, qunut) | **sunnah.com** + Fasolatan Kemenag | ✅ resmi |
| `sholat_daftar.json` | Daftar sholat wajib/sunnah + niat | **sunnah.com** + Fasolatan Kemenag | ✅ resmi |
| `sholat_ghaib.json` | Sholat ghaib | **sunnah.com** (Bukhari 1335, Muslim 394) | ✅ resmi |
| `sholat_jenazah.json` | Sholat jenazah (syarat, langkah, niat) | **sunnah.com** (Bukhari 1335) | ✅ resmi |
| `sholat_mayit.json` | Doa untuk mayit | **sunnah.com** (Muslim 963) | ✅ resmi |
| `wudhu.json` | Wudhu, tayamum, rukun, pembatal | **Fasolatan Kemenag** (kerangka) | ✅ resmi (tanpa lafaz) |
| `sholat_syarat.json` | Syarat wajib/sah, persiapan, penutup aurat, kebersihan tempat | **Fasolatan Kemenag** (kerangka) | ⚠️ kerangka, menunggu verifikasi kata-per-kata |
| `kota.json` | Koordinat kota | **sihat.kemenag.dev** (metode SIHAT) | ✅ resmi |

## Kategori sumber bacaan (wajib)

`plan.txt` meminta bacaan **dibedakan menurut sumbernya**. Setiap berkas yang memuat teks Arab
karena itu wajib menyatakan `kategori`:

| Kunci | Arti | Label di layar |
|---|---|---|
| `quran` | Ayat Al-Qur'an (MSI LPMQ Kemenag) | Al-Qur'an |
| `hadis` | Hadis/atsar dari kitab hadis (sunnah.com) | Hadis |
| `dzikir` | Dzikir & doa umum tanpa nisbah ayat/hadis tertentu | Dzikir umum |
| `tradisi` | Rumusan yang lazim di Indonesia (mis. lafaz niat, susunan tahlil) | Tradisi lokal |

Kategori boleh dinyatakan **sekali di tingkat berkas**, dan **ditimpa per entri** bila berkasnya
bercampur. Contohnya `sholat_bacaan.json` bertingkat berkas `hadis`, tetapi entri `niat`-nya
`tradisi` dan `fatihah`/`surat_setelah_fatihah` `quran`; `tahlil_full.json` bertingkat `dzikir`
dengan `doa_mayit` = `hadis` dan `doa_penutup` = `quran`.

Kunci di luar daftar di atas **gagal di gate CI**. Di aplikasi, `SumberKategori.dikenal()`
menyaringnya sehingga kunci tak dikenal **tidak ditampilkan sama sekali** — lebih baik tanpa label
daripada label yang belum diverifikasi. Dua sisi ini (gate Python dan `SumberKategori.kt`) harus
tetap sepakat; `SumberKategoriTest` membandingkan daftarnya dan gagal bila salah satu berubah
sendiri.

## Teks Al-Qur'an: MSI + transliterasi resmi (selesai)

Seluruh teks Al-Qur'an kini berasal dari layanan resmi **Qur'an Kemenag**
(`quran.kemenag.go.id`), bukan lagi dari cermin non-resmi:

- **Teks Arab** memakai **Mushaf Standar Indonesia (MSI) LPMQ Kemenag** (bukan standar
  Utsmani/Tanzil). Ciri MSI antara lain `الْقُرْاٰنِ` (alif khanjariyah), bukan `ٱلْقُرْءَانِ`.
- **Transliterasi Latin** memakai transliterasi resmi Kemenag berbahasa Indonesia
  (mis. `Bismillāhir-raḥmānir-raḥīm(i).`), menggantikan transliterasi Inggris dari
  `api.quran.com`.
- **Terjemahan** memakai terjemah Kemenag edisi 2019 dari sumber yang sama (penanda
  catatan kaki dibuang karena aplikasi belum menampilkan catatan kaki).

Penyegaran dilakukan lewat `tools/fetch_quran_kemenag.py` (alat pengembang, **tidak**
dijalankan di CI) sehingga jejak pengambilan data dapat diaudit dan diulang. Invarian
"sumber teks harus MSI Kemenag dan tidak boleh merujuk cermin non-resmi" dijaga oleh
uji `ContentSourceTest.quranTextUsesMsiAndOfficialTransliteration` dan
`ContentSourceTest.noContentFileCitesNonOfficialTextSource`.

## Kekurangan yang masih menunggu sumber resmi

`plan.txt` meminta beberapa bacaan yang **belum dapat dicantumkan** karena teks Arabnya belum
tersedia dari sumber di whitelist. Sesuai aturan proyek ("Jangan karang teks agama"), entri ini
**dibiarkan kosong, bukan dikarang** — dan dicatat di sini agar dapat diisi setelah diverifikasi
manusia dari sumber resmi.

| Yang diminta `plan.txt` | Status | Sumber yang diperlukan |
|---|---|---|
| Doa **keluar masjid** | ✅ ada di `dzikir.json` | HR Abu Dawud 465 (sahih, Al-Albani) — sunnah.com |
| Doa **masuk rumah** | ✅ ada di `dzikir.json` — **diberi catatan derajat hadis** | HR Abu Dawud 5096 (**daif** menurut Al-Albani) — sunnah.com |
| Doa **bepergian (safar)** | ✅ ada di `dzikir.json` | HR Muslim 1342 — sunnah.com |
| **Lafaz sholawat setelah adzan** | ✅ ada di `adzan.json` (`sholawat_setelah_adzan`) & tampil di layar Doa Adzan | HR Bukhari 3370 — sunnah.com |
| **Penjelasan waktu membaca tiap bacaan adzan** | ✅ setiap entri di `adzan.json` (`menjawab_adzan`, `sholawat_setelah_adzan`, `doa_setelah_adzan`) punya field `waktu`, dan gate konten menolak entri yang kehilangannya | mengikuti urutan yang diperintahkan pada hadis yang **sudah dikutip entri itu sendiri** (menjawab → sholawat → doa; HR Bukhari & Muslim — sunnah.com). Field `waktu` hanya keterangan berbahasa Indonesia, **bukan lafaz atau hadis baru** |
| **Niat sholat Jumat** | ⚠️ ditandai belum dicantumkan di panduan sholat | Fasolatan Kemenag / MSI LPMQ |
| **Susunan Tahlil kedua** (`plan.txt`: "jika memungkinkan") | ❌ hanya 1 susunan (`lazim_indonesia`); pemilih susunan sudah siap menerima lebih dari satu, di modul Tahlil **dan** di Pengaturan (`PreferensiSusunan`) | rujukan tradisi yang jelas (mis. HPT Muhammadiyah) — jangan disusun sendiri |
| **Rincian kata syarat/persiapan sholat** (`sholat_syarat.json`) | ⚠️ kerangka berbahasa Indonesia sudah ada dan tampil di layar detail sholat; kalimatnya **belum dicocokkan kata-per-kata** dengan modul Fasolatan resmi | modul Fasolatan Ditjen Bimas Islam (unduhan resmi) |

> Catatan: teks Arab/hadis **tidak pernah** dibuat dengan AI. Bila sumber resmi belum
> tersedia, entri ditandai `catatan_verifikasi` dan dibiarkan apa adanya, bukan dikarang.
> Doa untuk orang yang meninggal dan doa ampunan sudah tersedia (modul Tahlil, sholat
> jenazah/ghaib, serta Istighfar/Sayyidul Istighfar di `dzikir.json`). Sejak kelompok
> **"Doa khusus"** ditambahkan, doa jenazah juga tampil sebagai entri tersendiri di pustaka
> dzikir — tetapi **tidak disalin**: entri itu menunjuk `sholat_mayit.json#doa_mayit`, sehingga
> hanya ada satu salinan teks yang terverifikasi di seluruh aplikasi.
>
> **"Doa memohon ampunan"** (`plan.txt` §"Pustaka Doa dan Dzikir") kini menjadi kelompok
> tersendiri di `dzikir.json`. Isinya juga **tidak ada yang baru**: ketiga entrinya menunjuk
> lafaz yang sudah terverifikasi — Istighfar 3x (`tahlil_full.json#istighfar`, HR Muslim 591),
> Sayyidul Istighfar, dan Subhanallahi wa bihamdih 100x (HR Bukhari-Muslim) — yang dua terakhir
> ada di kelompok *Dzikir pagi & petang* di berkas yang sama. Agar rujukan seperti itu mungkin,
> setiap entri `dzikir.json` diberi `id` stabil dan `ContentRepo` mencari `#id` juga di dalam
> `kelompok[].items[]` (sebelumnya hanya `bagian`/`bacaan`); `RujukanTest` menguji penyelesai itu
> dan `DzikirRujukanTest` menjaga agar **tidak satu pun** entri penunjuk menyalin teksnya sendiri.
>
> **Doa masuk rumah** disertakan beserta derajat hadisnya (daif menurut Al-Albani) secara
> terbuka pada field `sumber`, agar pengguna dapat menilai sendiri — bukan disembunyikan.
> Lafaz menjawab adzan dipindahkan dari kode Kotlin ke `adzan.json` supaya tervalidasi gate
> konten dan tidak ada teks Arab hardcoded di layar.
>
> **`sholat_syarat.json` sengaja tanpa satu pun huruf Arab.** Isinya daftar periksa berbahasa
> Indonesia (syarat wajib, syarat sah, persiapan, batas aurat, kebersihan tempat) yang dibaca
> pembaca langkah demi langkah sebelum takbiratul ihram. Aturan proyek melarang menyusun lafaz
> atau hadis tanpa sumber resmi, jadi berkas ini tidak memuat keduanya — dan `SyaratSholatTest`
> menolak masuknya karakter Arab maupun kunci `arab`/`latin` ke dalamnya. Statusnya dinyatakan
> terbuka di layar lewat field `catatan_verifikasi`: kerangkanya mengikuti Fasolatan, rincian
> katanya masih menunggu pencocokan dengan modul resmi. Menggabungkannya ke sini bukan menambah
> klaim keagamaan baru, melainkan menaruh bagian panduan yang selama ini tidak ada di alur.

## Cara memeriksa

```bash
python3 tools/validate_content.py          # peringatan (non-blocking) — untuk pengembangan
python3 tools/validate_content.py --strict # gate: gagal bila ada entri tanpa sumber resmi
```

Selain `sumber_url` tingkat berkas, gate ini juga menuntut **sumber per-entri** pada
`dzikir.json` (agar ayat Qur'an, hadis sahih, dan hadis **daif** dapat dibedakan pengguna —
lihat doa masuk rumah dan doa sesudah makan) serta menolak **placeholder** di field `arab`/`latin`
(mis. `"— lihat rujukan file —"`). Bila teks asli diambil dari berkas rujukan (`rujukan`), field
inline-nya dibiarkan kosong alih-alih diisi prosa penunjuk.

Mode `--strict` dijalankan di **setiap PR** (`ci.yml`) dan di rilis (`release.yml`), sehingga
entri tanpa sumber resmi tidak dapat masuk ke `main`.

Gate yang sama juga **menolak teks Arab tanpa harakat** (kata Arab ≥ 3 huruf yang sama sekali
tak punya tanda baris). Teks berharakat wajib agar pemula dapat membaca; teks mentah yang belum
divokalkan hampir pasti salah tempel. Invarian ini dijaga pula oleh uji `ArabicTextTest`, yang
sekaligus memastikan tidak ada teks Arab yang ditulis langsung di kode Kotlin — seluruh lafaz
harus berasal dari `assets/content` agar sumbernya dapat ditelusuri.

Terakhir, gate **menuntut `kategori`** pada setiap berkas ber-Arab (nilai tak dikenal = galat),
dan **menolak nilai `kategori` di luar** `quran`/`hadis`/`dzikir`/`tradisi`. Lihat bagian
"Kategori sumber bacaan (wajib)" di atas; pasangan ujinya adalah `SumberKategoriTest`.
