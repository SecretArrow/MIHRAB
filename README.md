# Yasin dan Tahlil — Aplikasi Islami Modern

Aplikasi Android native (Kotlin + Jetpack Compose + Material 3), offline-first.

## Sumber resmi (wajib)
Lihat `SOURCES.md`. Teks Qur'an dari LPMQ Kemenag RI (Mushaf Standar Indonesia),
jadwal dari SIHAT Ditjen Bimas Islam, kiblat dari SIHAT E-Kiblat,
panduan sholat mengacu Fasolatan Kemenag + verifikasi `sunnah.com`.
Jangan commit token/API key. Pakai GitHub Secrets.

## Struktur
- `app/` kode Android (feature: home, yasin, tahlil, sholat incl. ghaib & wudhu, adzan, iqomah, kiblat, jadwal, dzikir, search, settings)
- `app/src/main/assets/content/` JSON konten berlisensi + kolom `sumber_url`
- `.github/workflows/` ci (lint+unit+build, hemat), e2e (main/nightly saja), release (tag v*),
  autofix (higiene teks: `workflow_dispatch` + sapuan mingguan), workflow-lint (memastikan berkas
  workflow sendiri dapat dibaca — lihat `tools/check_workflows.py`)
- `.github/dependabot.yml` pembaruan dependensi Gradle & GitHub Actions
- `tools/` gate konten & gate cepat: `validate_content.py` (konten wajib bersumber resmi),
  `validate_bundle.py` (bundel pembaruan), `check_workflows.py` (workflow dapat dibaca),
  `check_kotlin_lex.py` (higiene komentar Kotlin; `--uji-diri` memeriksa pemindai literalnya
  sendiri), `format_teks.py` (higiene teks: akhir baris LF,
  spasi ekor, tepat satu baris baru di akhir; `--fix` merapikan di tempat). Semuanya Python murni
  dan dapat dijalankan lokal tanpa membangun proyek.

## Build (via GitHub Actions, bukan lokal)
- PR: validasi konten + lint + unit test + build debug/release (R8) (3-6 mnt).
- main: sama seperti PR.
- tag `v*.*.*`: release AAB/APK + GitHub Release otomatis. **Signed** bila Secrets keystore tersedia; bila tidak, APK release bersifat *unsigned* (hanya untuk memvalidasi build R8).
- `workflow_dispatch` pada release: menguji build rilis **tanpa menerbitkan apa pun**; isi masukan `tag` untuk menerbitkan rilis bagi tag yang sudah ada.

Secrets (hanya untuk rilis bertanda tangan): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

Langkah verifikasi di `release.yml` **membuktikan** tanda tangannya, bukan sekadar mencetaknya: ia
membandingkan sidik jari SHA-256 penanda tangan APK dengan keystore yang didekode dari secret, jadi
APK yang bertanda tangan debug akan menggagalkan rilis.

Brankas keystore-nya sendiri disimpan di akar repo sebagai arsip **terenkripsi**
(`key-sign-apk.zip` + lapisan `key-sign-apk.zip.enc`). Repositori ini publik, jadi sandinya **tidak**
ada di sini — ia hanya hidup di Secrets dan di penyimpanan pemilik. Untuk membuka sementara:
`./pakai-keystore.sh buka` lalu `./pakai-keystore.sh bersihkan` (salinan skrip ada di brankasnya).

Aplikasi **100% offline**: tidak meminta izin Internet dan tidak mengirim data ke mana pun.
Sumber jadwal/kalender *online* resmi belum diaktifkan karena SIHAT Bimas Islam tidak
menyediakan API publik untuk aplikasi pihak ketiga; izin Internet akan ditambahkan hanya bila
fitur itu benar-benar ada.

## Batasan yang disengaja

Semuanya adalah pilihan sadar, bukan pekerjaan yang tertinggal — supaya tidak ada yang tampak
"belum jadi" padahal memang sengaja:

- **Antarmuka hanya berbahasa Indonesia.** Tidak ada kontrol "Bahasa aplikasi" karena belum ada
  terjemahan antarmuka kedua; tombol yang tidak mengubah apa pun dilarang oleh `plan.txt` sendiri
  ("Hindari tombol dekoratif yang tidak bekerja").
- **Tidak ada audio adzan bawaan.** Lisensi audio tidak dapat dipastikan, jadi setiap jenis suara
  adalah berkas pilihan pengguna yang ia beri nama — aplikasi tidak menyertakan satu pun.
- **Satu susunan Tahlil.** Susunan lain menunggu sumber resmi; lihat `docs/CONTENT_SOURCES.md`.
  Penjelasan perbedaan susunan Iqomah antar-mazhab juga belum ada karena alasan yang sama.
- **Pengingat memakai WorkManager, tanpa izin alarm tepat waktu.** Waktu dapat bergeser beberapa
  menit saat perangkat tidur (Doze); hal ini dinyatakan di layar Adzan, bukan disembunyikan.
- **Sepenuhnya offline** — tidak ada sumber *online*, dengan alasan yang dijelaskan di bagian Build
  di atas.
- **Toolchain dipatok** (AGP 8.13.x, Gradle 8.14.6, Kotlin 2.3.21, `compileSdk`/`targetSdk` 36).
  Kenaikan toolchain harus dilakukan sebagai satu perubahan teruji — rincian dan alasan tiap plafon
  ada di `.github/dependabot.yml`. `targetSdk` 36 sudah memenuhi syarat minimum Google Play untuk
  pengajuan aplikasi baru (wajib API 36 sejak 31 Agustus 2026).

## Jalankan lokal (opsional)
Butuh Android Studio + JDK 17. `./gradlew :app:assembleDebug`.

## Lisensi
Kode aplikasi: **Apache License 2.0** — lihat `LICENSE`.
Copyright 2026 SecretArrow.

Konten keagamaan (teks Qur'an, hadis, panduan) **tidak** tercakup lisensi ini dan tetap
tunduk pada sumber resmi masing-masing — lihat `SOURCES.md` dan `docs/CONTENT_SOURCES.md`.
Kebijakan privasi: `docs/PRIVACY.md`. Daftar izin: `docs/PERMISSIONS.md`.
