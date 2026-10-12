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
  workflow-lint (memastikan berkas workflow sendiri dapat dibaca — lihat `tools/check_workflows.py`)
- `.github/dependabot.yml` pembaruan dependensi Gradle & GitHub Actions
- `tools/` gate konten & gate cepat: `validate_content.py` (konten wajib bersumber resmi),
  `validate_bundle.py` (bundel pembaruan), `check_workflows.py` (workflow dapat dibaca),
  `check_kotlin_lex.py` (higiene komentar Kotlin). Ketiganya di luar `validate_content.py`
  dapat dijalankan lokal tanpa membangun proyek.

## Build (via GitHub Actions, bukan lokal)
- PR: validasi konten + lint + unit test + build debug/release (R8) (3-6 mnt).
- main: sama seperti PR.
- tag `v*.*.*`: release AAB/APK + GitHub Release otomatis. **Signed** bila Secrets keystore tersedia; bila tidak, APK release bersifat *unsigned* (hanya untuk memvalidasi build R8).

Secrets (hanya untuk rilis bertanda tangan): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

Aplikasi **100% offline**: tidak meminta izin Internet dan tidak mengirim data ke mana pun.
Sumber jadwal/kalender *online* resmi belum diaktifkan karena SIHAT Bimas Islam tidak
menyediakan API publik untuk aplikasi pihak ketiga; izin Internet akan ditambahkan hanya bila
fitur itu benar-benar ada.

## Jalankan lokal (opsional)
Butuh Android Studio + JDK 17. `./gradlew :app:assembleDebug`.

## Lisensi
Kode aplikasi: **Apache License 2.0** — lihat `LICENSE`.
Copyright 2026 SecretArrow.

Konten keagamaan (teks Qur'an, hadis, panduan) **tidak** tercakup lisensi ini dan tetap
tunduk pada sumber resmi masing-masing — lihat `SOURCES.md` dan `docs/CONTENT_SOURCES.md`.
Kebijakan privasi: `docs/PRIVACY.md`. Daftar izin: `docs/PERMISSIONS.md`.
