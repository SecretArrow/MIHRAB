# Changelog

Format mengikuti [Keep a Changelog](https://keepachangelog.com/id/1.1.0/).
Versi mengikuti [Semantic Versioning](https://semver.org/lang/id/).

## [Unreleased]

### Ditambahkan
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

### Diubah
- **Perhitungan jadwal** memakai algoritma Meeus (deklinasi matahari + *equation of time*)
  dan terverifikasi terhadap acuan SIHAT Bimas Islam.
- Beranda menampilkan hitung mundur sholat berikutnya secara *live*.
- Favorit, bookmark, riwayat, dan progres dipindahkan dari DataStore ke Room; DataStore
  tetap dipakai untuk preferensi.

### Dihapus
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
