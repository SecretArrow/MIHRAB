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
- `.github/workflows/` ci (lint+unit+build, hemat), e2e (main/nightly saja), release (tag v*)
- `.github/dependabot.yml` pembaruan dependensi Gradle & GitHub Actions

## Build (via GitHub Actions, bukan lokal)
- PR: validasi konten + lint + unit test + build debug/release (R8) (3-6 mnt).
- main: sama seperti PR.
- tag `v*.*.*`: release AAB/APK + GitHub Release otomatis. **Signed** bila Secrets keystore tersedia; bila tidak, APK release bersifat *unsigned* (hanya untuk memvalidasi build R8).

Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Opsional (bila fitur sumber online dipakai): `SIHAT_TOKEN`.

## Jalankan lokal (opsional)
Butuh Android Studio + JDK 17. `./gradlew :app:assembleDebug`.
