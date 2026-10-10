# Yasin dan Tahlil — Aplikasi Islami Modern

Aplikasi Android native (Kotlin + Jetpack Compose + Material 3), offline-first.

## Sumber resmi (wajib)
Lihat `SOURCES.md`. Teks Qur'an dari LPMQ Kemenag RI (Mushaf Standar Indonesia),
jadwal dari SIHAT Ditjen Bimas Islam, kiblat dari SIHAT E-Kiblat,
panduan sholat mengacu Fasolatan Kemenag + verifikasi `sunnah.com`.
Jangan commit token/API key. Pakai GitHub Secrets.

## Struktur
- `app/` kode Android (feature: home, yasin, tahlil, sholat incl. ghaib, adzan, iqomah, kiblat, jadwal, dzikir, search, settings)
- `app/src/main/assets/content/` JSON konten berlisensi + kolom `sumber_url`
- `.github/workflows/` ci (lint+unit, hemat), e2e (main/nightly saja), release (tag v*), autofix

## Build (via GitHub Actions, bukan lokal)
- PR: lint + unit test saja (3-5 mnt).
- main: + assembleDebug APK.
- tag `v*.*.*`: signed release AAB/APK + GitHub Release otomatis.

Secrets: `SIHAT_TOKEN`, `QURAN_API_KEY` (opsional), `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Jalankan lokal (opsional)
Butuh Android Studio + JDK 17. `./gradlew :app:assembleDebug`.
