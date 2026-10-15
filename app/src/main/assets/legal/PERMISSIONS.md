# Izin Aplikasi (Permissions)

Semua izin dideklarasikan di `app/src/main/AndroidManifest.xml`. Aplikasi bersifat
**offline-first**: seluruh perhitungan dan konten berada di perangkat — satu-satunya akses
jaringan adalah murottal yang Anda putar sendiri (baris `INTERNET` di bawah), dan izin yang
tidak diperlukan tidak diminta saat pemasangan.

| Izin | Kapan diminta | Alasan | Bila ditolak |
|------|----------------|--------|--------------|
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Saat pengguna menekan "Pakai GPS" (kiblat/jadwal) | Menentukan koordinat untuk arah kiblat dan jadwal sholat | Pengguna dapat memilih lokasi manual dari daftar kota — fitur tetap berjalan |
| `POST_NOTIFICATIONS` (Android 13+) | Saat aplikasi pertama dibuka | Menampilkan pengingat waktu sholat/adzan | Pengingat tidak muncul; fitur lain tetap berjalan |
| `RECEIVE_BOOT_COMPLETED` | Otomatis | Menyusun ulang pengingat setelah perangkat menyala ulang | Pengingat berhenti sampai aplikasi dibuka lagi |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Saat memutar audio adzan pilihan pengguna | Memutar adzan di latar belakang | Adzan hanya berupa notifikasi |
| `VIBRATE` | Saat timer iqomah berakhir | Getar singkat sebagai penanda | Tidak ada getar |
| `ACCESS_NOTIFICATION_POLICY` | Saat pengguna membuka "Atur akses DND" di menu Adzan | Memungkinkan pengingat dikecualikan dari mode Jangan Ganggu | Pengingat mengikuti aturan mode senyap/DND |
| `INTERNET` | Tanpa dialog (izin biasa) — aktif hanya saat pengguna menekan putar murottal di layar Yasin | Mengalirkan audio murottal Surat Yasin per ayat dari penyedia audio publik (everyayah.com) — satu-satunya lalu lintas jaringan aplikasi | Murottal tidak dapat diputar; seluruh fitur lain tetap berjalan offline |

## Catatan

- **Internet hanya untuk murottal Yasin.** Audio murottal Surat Yasin dialirkan per ayat
  dari `everyayah.com` ketika pengguna sendiri menekan tombol putar — itulah satu-satunya
  lalu lintas jaringan aplikasi. Aplikasi tidak pernah memeriksa status jaringan
  (`ACCESS_NETWORK_STATE` tidak dideklarasikan); seluruh hisab, jadwal, dan konten tetap
  dihitung di perangkat. Pemakaiannya terkunci di paket `core/audio` — dijaga
  `OfflineManifestTest.internet_hanya_untuk_murottal_di_paket_audio`.
- **Tidak ada izin** untuk kamera, mikrofon, kontak, SMS, atau penyimpanan eksternal.
- **Tidak ada `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`**: pengingat memakai WorkManager,
  bukan `AlarmManager`. Izin itu hanya muncul bila kode benar-benar memakai `AlarmManager`
  (dijaga `OfflineManifestTest.izin_alarm_tepat_waktu_sejalan_dengan_pemakaian`).
  Konsekuensinya waktu pengingat dapat bergeser beberapa menit saat perangkat masuk Doze;
  aplikasi menyarankan menonaktifkan optimasi baterai di menu Adzan.
- **Setiap izin pada tabel di atas benar-benar dipakai kode aplikasi sendiri** — dijaga
  `PemakaianIzinTest`, yang menolak izin yang dideklarasikan tanpa pemakaian di `src/main`.
  Pustaka boleh membawa izinnya sendiri ke manifes gabungan (WorkManager, misalnya,
  mencantumkan `WAKE_LOCK`); izin semacam itu sengaja **tidak** masuk tabel ini karena aplikasi
  tidak pernah memintanya sendiri.
- Lokasi hanya dipakai di perangkat untuk menghitung kiblat/jadwal; **tidak pernah dikirim**.
- Aplikasi tidak menyertakan audio adzan bawaan (alasan lisensi); berkas audio dipilih
  pengguna dan izin baca berkas disimpan agar dapat diputar pekerja latar.
