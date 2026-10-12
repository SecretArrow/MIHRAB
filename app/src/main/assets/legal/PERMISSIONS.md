# Izin Aplikasi (Permissions)

Semua izin dideklarasikan di `app/src/main/AndroidManifest.xml`. Aplikasi bersifat
**offline-first**: tidak ada data yang dikirim ke server mana pun, dan izin yang tidak
diperlukan tidak diminta saat pemasangan.

| Izin | Kapan diminta | Alasan | Bila ditolak |
|------|----------------|--------|--------------|
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Saat pengguna menekan "Pakai GPS" (kiblat/jadwal) | Menentukan koordinat untuk arah kiblat dan jadwal sholat | Pengguna dapat memilih lokasi manual dari daftar kota — fitur tetap berjalan |
| `POST_NOTIFICATIONS` (Android 13+) | Saat aplikasi pertama dibuka | Menampilkan pengingat waktu sholat/adzan | Pengingat tidak muncul; fitur lain tetap berjalan |
| `RECEIVE_BOOT_COMPLETED` | Otomatis | Menyusun ulang pengingat setelah perangkat menyala ulang | Pengingat berhenti sampai aplikasi dibuka lagi |
| `WAKE_LOCK` | Otomatis | Memastikan pekerjaan pengingat selesai saat perangkat tidur | — |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Saat memutar audio adzan pilihan pengguna | Memutar adzan di latar belakang | Adzan hanya berupa notifikasi |
| `VIBRATE` | Saat timer iqomah berakhir | Getar singkat sebagai penanda | Tidak ada getar |
| `ACCESS_NOTIFICATION_POLICY` | Saat pengguna membuka "Atur akses DND" di menu Adzan | Memungkinkan pengingat dikecualikan dari mode Jangan Ganggu | Pengingat mengikuti aturan mode senyap/DND |

## Catatan

- **Tidak ada izin** untuk kamera, mikrofon, kontak, SMS, penyimpanan eksternal, atau
  **Internet** — aplikasi sepenuhnya offline. Izin Internet hanya akan ditambahkan bila
  sumber jadwal/kalender resmi *online* benar-benar diaktifkan.
- **Tidak ada `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`**: pengingat memakai WorkManager,
  bukan `AlarmManager`. Izin hanya dideklarasikan bila benar-benar dipakai — dijaga oleh
  uji `OfflineManifestTest.izin_alarm_tepat_waktu_sejalan_dengan_pemakaian`. Konsekuensinya
  waktu pengingat dapat bergeser beberapa menit saat perangkat masuk Doze; aplikasi
  menyarankan menonaktifkan optimasi baterai di menu Adzan.
- Lokasi hanya dipakai di perangkat untuk menghitung kiblat/jadwal; **tidak pernah dikirim**.
- Aplikasi tidak menyertakan audio adzan bawaan (alasan lisensi); berkas audio dipilih
  pengguna dan izin baca berkas disimpan agar dapat diputar pekerja latar.
