# Kebijakan Privasi (Privacy Policy)

Terakhir diperbarui: 2026-10-15

## Ringkasan

Aplikasi **Yasin dan Tahlil** dirancang *offline-first*. Seluruh konten (ayat, doa, dzikir,
panduan sholat, jadwal) disimpan di dalam aplikasi. Kami **tidak mengumpulkan, tidak
mengirim, dan tidak menjual** data pribadi Anda.

## Data yang disimpan di perangkat

- **Preferensi**: tema, ukuran huruf, metode hisab, mazhab Asar, koreksi menit, zona waktu,
  pengaturan adzan/iqomah, dan pilihan lokasi manual.
- **Data terstruktur**: favorit, penanda bacaan (bookmark), riwayat, dan progres bacaan
  (tersimpan di basis data Room lokal).

Data tersebut hanya berada di perangkat Anda dan dihapus bila aplikasi di-uninstall.
Fitur *Backup* mengekspor preferensi ke JSON yang Anda salin sendiri — isinya tidak
pernah dikirim ke pihak ketiga oleh aplikasi.

## Izin

Rincian lengkap ada di [`PERMISSIONS.md`](PERMISSIONS.md). Intinya:

- **Lokasi** (opsional) hanya dipakai di perangkat untuk menghitung arah kiblat dan jadwal
  sholat. Koordinat **tidak pernah** dikirim ke server mana pun.
- **Notifikasi** dipakai untuk pengingat waktu sholat.
- **Internet** hanya untuk murottal Surat Yasin: audio dialirkan per ayat dari penyedia
  audio publik (everyayah.com) saat Anda sendiri menekan tombol putar. Penyedia itu dapat
  melihat alamat IP dan permintaan berkasnya — itulah satu-satunya lalu lintas jaringan
  aplikasi. Selain itu, seluruh perhitungan dan konten berada di perangkat.

## Pihak ketiga

Aplikasi tidak menyertakan SDK iklan, analitik, atau pelacak apa pun. Saat murottal
Surat Yasin diputar, satu-satunya pihak ketiga yang dihubungi adalah penyedia audionya
(everyayah.com) — tanpa identitas, cookie, atau data pribadi lain yang dikirim.

## Konten keagamaan

Teks Arab, terjemahan, dan hadis mengikuti sumber resmi yang didokumentasikan di
[`CONTENT_SOURCES.md`](CONTENT_SOURCES.md) dan [`../SOURCES.md`](../SOURCES.md).

## Kontak

Untuk pertanyaan privasi, buka *issue* pada repositori proyek.
