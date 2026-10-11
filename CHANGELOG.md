# Changelog

Format mengikuti [Keep a Changelog](https://keepachangelog.com/id/1.1.0/).
Versi mengikuti [Semantic Versioning](https://semver.org/lang/id/).

## [Unreleased]

### Ditambahkan
- **Doa & lafaz adzan yang tadinya kosong**: doa keluar masjid (HR Abu Dawud 465), doa masuk
  rumah (HR Abu Dawud 5096 — derajat **daif** menurut Al-Albani, dicantumkan terbuka pada field
  `sumber`), doa bepergian/safar (HR Muslim 1342), serta **lafaz sholawat setelah adzan**
  (HR Bukhari 3370) yang kini tampil di layar Doa Adzan. Semuanya dari sunnah.com, tanpa teks
  yang dikarang. Lafaz menjawab adzan dipindahkan dari kode Kotlin ke `adzan.json`, dan uji baru
  menjaga agar **tidak ada teks Arab hardcoded di seluruh kode aplikasi**.
- **Peta arah kiblat (offline)**: peta skematik tanpa internet — menampilkan posisi pengguna,
  Ka'bah, dan rute lingkaran besar di antaranya (`QiblaCalc.greatCirclePath`/`distanceKm` +
  `MapProjection`), menjadi acuan arah bila perangkat tak punya magnetometer atau kompas belum
  terkalibrasi. Bujur di-*unwrap* agar rute yang melewati garis tanggal tetap benar.
- **Ilustrasi gerakan sholat**: setiap langkah panduan sholat kini menampilkan skema posisi
  tubuh (`res/drawable/posisi_*.xml` — garis geometris, tanpa penggambaran figuratif dan tanpa
  teks Arab) beserta deskripsi posisi yang prosedural; pemetaan bacaan→posisi dijaga oleh uji
  (`PostureTest`).
- **Jadwal sholat**: pemilihan metode hisab (Kemenag/SIHAT, MWL, Umm al-Qura, Egyptian,
  Makkah), mazhab Asar (Syafi'i/Hanafi), zona waktu, dan koreksi menit per waktu
  (`JadwalViewModel` + `JadwalScreen`).
- **Adzan & pengingat**: pemutaran audio adzan pilihan pengguna di latar melalui
  *foreground service* media; pengingat pra-adzan; opsi tembus mode senyap/DND; pintasan
  izin sistem (notifikasi, optimasi baterai, alarm tepat waktu).
- **Timer iqomah**: hitung mundur nyata dengan getar saat berakhir.
- **Kalender Hijriah**: koreksi hari manual agar selaras dengan isbat Kemenag.
- **Penyusun pengingat** (`PrayerScheduler`) yang membaca lokasi/metode/koreksi tersimpan,
  berjalan harian, dan disusun ulang otomatis setelah reboot (`BootReceiver`).
- Dokumentasi izin (`docs/PERMISSIONS.md`) dan kebijakan privasi (`docs/PRIVACY.md`).
- **Kiblat**: permintaan izin lokasi saat runtime, lokasi GPS live (bukan hanya
  `lastKnownLocation`), sensor rotasi bila tersedia (cadangan akselerometer + magnetometer),
  mode layar penuh, dan tombol "Perbarui lokasi".
- **Jadwal sholat**: tombol "Pakai lokasi GPS" dengan permintaan izin runtime.
- **Aksesibilitas**: teks Arab memakai arah tata letak RTL; ikon bilah bawah punya
  `contentDescription`.
- **Yasin/Tahlil/Dzikir**: ukuran font & jarak antarbaris, penanda ayat terakhir, bookmark,
  pencarian internal, counter persisten (Room), pemilih susunan tahlil, dan mode layar penuh
  yang benar-benar menyembunyikan bilah navigasi.
- **Favorit & Riwayat**: disimpan di Room (tahan rotasi & penutupan aplikasi), dengan
  migrasi sekali dari penyimpanan lama.
- Dependabot untuk dependensi Gradle & GitHub Actions (`.github/dependabot.yml`).
- Berkas `LICENSE` (Apache-2.0) untuk kode aplikasi, dengan catatan bahwa konten keagamaan
  tetap tunduk pada sumber resminya masing-masing; serta `.editorconfig`.
- **Wudhu & Tayamum**: panduan langkah demi langkah (kerangka Fasolatan/Bimas Islam, mazhab
  Syafi'i), daftar pembatal wudhu, dan penanda "sudah dipelajari" yang tersimpan di Room.
  Lafaz Arab sengaja belum dicantumkan — menunggu verifikasi sumber resmi.
- **Backup/pemulihan penuh**: satu berkas memuat preferensi (DataStore) sekaligus favorit,
  bookmark, riwayat, dan progres (Room); tetap kompatibel dengan backup lama.
- **Pemilihan mazhab panduan** (Syafi'i/Hanafi/Maliki/Hambali) yang benar-benar mengubah
  bacaan: entri bertanda mazhab (mis. doa qunut — Syafi'i) hanya tampil bila mazhab cocok;
  bacaan juga dapat dibatasi per sholat (qunut hanya muncul di Subuh). Dapat diubah di
  Pengaturan maupun langsung di layar panduan sholat.
- **Penjelasan khusus sholat**: daftar ketentuan untuk Jumat (siapa yang wajib, khutbah)
  dan keringanan safar (qashar & jamak) kini tampil di panduan sholat. Lafaz niat Jumat
  ditandai belum dicantumkan — menunggu verifikasi sumber resmi.
- **Identitas visual**: ikon peluncur adaptif (bulan sabit & bintang, latar gradasi zamrud),
  termasuk lapisan *monochrome* untuk ikon bertema Android 13+. Nama aplikasi dipindahkan
  ke `strings.xml`.
- **Tema jendela terang/gelap** (`values`/`values-night`): latar jendela disamakan dengan
  palet aplikasi sehingga tidak ada kedipan putih saat dibuka dalam mode gelap; warna bilah
  status/navigasi mengikuti tema. Palet XML dijaga selaras dengan palet Compose lewat uji.

### Diubah
- **Teks Al-Qur'an kini 100% dari sumber resmi**: seluruh ayat (`yasin_full.json`,
  `surah_001/112/113/114.json`, `ayat_kursi.json`, serta ayat Qur'an di `tahlil_full.json`)
  memakai **Mushaf Standar Indonesia (MSI) LPMQ Kemenag** beserta **transliterasi Latin resmi
  Kemenag** dan terjemah Kemenag edisi 2019 — menggantikan teks Utsmani (Tanzil) dan
  transliterasi Inggris dari `api.quran.com` yang non-resmi. Pengambilan data dapat diaudit dan
  diulang lewat `tools/fetch_quran_kemenag.py` (alat pengembang, tidak dijalankan di CI);
  invarian sumber dijaga `ContentSourceTest`. Perbaikan sampingan: BOM tersembunyi di ayat
  pertama Al-Fatihah ikut hilang.
- **Kejujuran izin**: izin `SCHEDULE_EXACT_ALARM` dihapus dari manifes karena pengingat memakai
  WorkManager dan tidak ada pemakaian `AlarmManager` sama sekali; tombol "Alarm tepat waktu"
  (yang membuka pengaturan izin tak terpakai) ikut dihapus, dan teks di menu Adzan kini
  menjelaskan bahwa waktu pengingat dapat bergeser saat perangkat masuk Doze. Invarian
  "izin alarm tepat waktu hanya bila `AlarmManager` dipakai" dijaga uji.
- **Pengaturan bahasa**: baris "Bahasa aplikasi" kini menjelaskan bahwa antarmuka hanya
  tersedia dalam bahasa Indonesia (sesuai `plan.txt`), alih-alih menyimpan kunci `lang`
  yang tidak pernah dibaca kode mana pun.
- **Perhitungan jadwal** memakai algoritma Meeus (deklinasi matahari + *equation of time*)
  dan terverifikasi terhadap acuan SIHAT Bimas Islam.
- Beranda menampilkan hitung mundur sholat berikutnya secara *live*.
- Favorit, bookmark, riwayat, dan progres dipindahkan dari DataStore ke Room; DataStore
  tetap dipakai untuk preferensi.
- **Aksesibilitas warna**: tema gelap memakai varian terang untuk `primary`/`secondary`
  dengan teks gelap di atas tombol, dan warna teks Latin (emas) digelapkan — semuanya agar
  lolos ambang kontras WCAG AA. Ditambah uji kontras otomatis (`ContrastTest`).
- **Sholat Ghaib & Jenazah**: niat dan doa mayit dipindahkan dari kode Kotlin ke assets
  (`sholat_ghaib.json`, `sholat_jenazah.json`, `sholat_mayit.json`) agar tervalidasi gate
  konten; tidak ada lagi teks Arab hardcoded di layar.

### Dihapus
- Berkas `assets/content/yasin.json` yang yatim (metadata Yasin tak dibaca kode mana pun —
  aplikasi memakai `yasin_full.json`, yang juga sudah memuat metadata surat). Rujukan di
  `SOURCES.md` dan `docs/CONTENT_SOURCES.md` disesuaikan.
- Alur kerja `autofix.yml` yang hanya menjalankan `./gradlew tasks` (tanpa formatter nyata);
  pemeriksaan tetap dilakukan oleh lint & unit test di `ci.yml`.
- Izin `INTERNET` dan `ACCESS_NETWORK_STATE` dari manifes: tidak ada kode jaringan sama sekali,
  sehingga deklarasi izin kini jujur sejalan dengan sifat aplikasi yang 100% offline.

### Catatan
- Sumber jadwal/kalender **online resmi** belum diaktifkan karena SIHAT Bimas Islam tidak
  menyediakan API publik untuk aplikasi pihak ketiga; perhitungan lokal tetap akurat dan
  bekerja tanpa internet.

## [1.0.0] - 2026-10-10
- Rilis awal: Yasin, Tahlil, panduan sholat, dzikir & doa, kiblat, jadwal sholat, adzan,
  pencarian, favorit & riwayat.
