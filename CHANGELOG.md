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

### Diubah
- **Perhitungan jadwal** memakai algoritma Meeus (deklinasi matahari + *equation of time*)
  dan terverifikasi terhadap acuan SIHAT Bimas Islam.
- Beranda menampilkan hitung mundur sholat berikutnya secara *live*.

### Catatan
- Sumber jadwal/kalender **online resmi** belum diaktifkan karena SIHAT Bimas Islam tidak
  menyediakan API publik untuk aplikasi pihak ketiga; perhitungan lokal tetap akurat dan
  bekerja tanpa internet.

## [1.0.0] - 2026-10-10
- Rilis awal: Yasin, Tahlil, panduan sholat, dzikir & doa, kiblat, jadwal sholat, adzan,
  pencarian, favorit & riwayat.
