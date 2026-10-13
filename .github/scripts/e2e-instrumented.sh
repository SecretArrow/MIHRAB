#!/usr/bin/env bash
# Menjalankan uji instrumented di emulator dengan tahan-gagal infrastruktur.
#
# Dipanggil oleh .github/workflows/e2e.yml sebagai SATU baris perintah. Itu penting:
# aksi `reactivecircus/android-emulator-runner` menjalankan setiap baris `script`
# sebagai `sh -c` terpisah, sehingga loop/fungsi multi-baris tidak akan pernah utuh.
# Seluruh logika karena itu diletakkan di berkas ini.
#
# Latar belakang: run E2E `main` 38185305094 merah TANPA menjalankan satu uji pun —
# emulator mendadak tak responsif, adb berhenti menjawab, lalu AGP melewati perangkat
# ("Unknown API Level" → "0 perangkat cocok"). Kegagalan seperti itu masalah
# infrastruktur, bukan uji merah, jadi diulang sekali; uji yang benar-benar gagal
# (assertion) tetap langsung dinyatakan gagal.
#
# Kesiapan perangkat kini diukur dengan **perintah yang sama** dengan yang dipakai
# pembacanya. ddmlib — lewat AGP — membaca SELURUH tabel properti (`adb shell getprop` tanpa
# argumen) dan membatalkannya setelah **2 detik** (`PropertyFetcher.GETPROP_TIMEOUT_SEC`), lalu
# melempar ShellCommandUnresponsiveException; AGP menerjemahkannya menjadi "Unknown API Level" dan
# melewati perangkatnya. Sebelumnya kesiapan hanya diperiksa dengan pembacaan SATU properti
# (`getprop ro.build.version.sdk`), yang jauh lebih ringan — sehingga gerbangnya dapat lulus
# sementara pembacaan penuh masih kehabisan waktu, dan AGP tetap melewati perangkat. Run `main`
# 38366620795 adalah buktinya: gerbang melaporkan `API='29'` dua kali, lalu AGP tetap berkata
# "Unknown API Level" dua kali.

set -u

LOG=/tmp/e2e-connected.log

# Batas waktu ddmlib saat membaca properti perangkat: `PropertyFetcher.GETPROP_TIMEOUT_SEC`, bawaan
# 2 detik (dapat dinaikkan lewat properti sistem `ddmlib.getprop.timeout.sec`, tetapi aplikasi ini
# memakai bawaannya). Gerbang dan pembacanya harus mengukur dengan angka yang **sama**: gerbang yang
# lebih longgar lulus justru untuk perangkat yang, di mata AGP, masih kehabisan waktu.
BATAS_PROPERTI=2

# Lama maksimal menunggu perangkat siap, dan lama maksimal menunggunya tenang sesudah adb
# dibangkitkan ulang — keduanya dalam detik. Batas waktu ini menggantikan jumlah percobaan tetap
# supaya lama tunggu tidak bergantung pada berapa lama satu percobaan memakan waktunya.
BATAS_SIAP=300
BATAS_TENANG=120

tunggu_boot() {
  until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    sleep 2
  done
}

# Pembacaan properti yang **sama** dengan yang dilakukan ddmlib: seluruh tabel properti, dibatasi
# BATAS_PROPERTI detik. Selesai tanpa kehabisan waktu berarti perangkat benar-benar siap bagi
# pembacanya — bukan sekadar "satu properti kebetulan bisa dibaca".
perintah_siap() {
  timeout "$BATAS_PROPERTI" adb shell getprop > /dev/null 2>&1
}

# Tunggu adb benar-benar bisa membaca properti. Tanpa ini AGP melewati perangkat
# dengan "Unknown API Level" dan seluruh uji dilewati tanpa dijalankan.
tunggu_properti() {
  local api=""
  local batas=$((SECONDS + BATAS_SIAP))
  while [ "$SECONDS" -lt "$batas" ]; do
    api=""
    if perintah_siap; then
      api="$(adb shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')"
      # Keluarannya harus berupa angka. adb yang belum siap kadang mengembalikan
      # keluaran sampah (atau hanya spasi); bila nilai itu diteruskan ke AGP, AGP
      # membacanya sebagai "API level=1" dan menolak memasang APK
      # ("Cannot install split APKs with API level < 21") — 0 uji berjalan.
      case "$api" in
        '' | *[!0-9]*) api="" ;;
      esac
    fi
    [ -n "$api" ] && break
    sleep 2
  done
  echo "Perangkat: $(adb devices | tr '\n' ' ') | API='$api'"
  [ -n "$api" ] || { echo "::error::Emulator tidak melaporkan API level"; return 1; }
}

# Penyiapan perangkat adalah SATU urutan: menunggu perangkat, menunggu boot selesai,
# lalu memastikan properti terbaca. Dahulu urutan itu ditulis dua kali — jalur pertama
# memanggil ketiganya, jalur ulangan hanya `tunggu_boot` — dan salinan kedua itulah
# yang menyimpang: emulator yang boot-nya selesai tetapi adb-nya belum bisa membaca
# properti membuat AGP menolak memasang APK, sehingga ulangan gagal dengan 0 uji
# berjalan meski ujinya sendiri tidak salah. Kini keduanya memakai fungsi ini.
siapkan_perangkat() {
  adb wait-for-device
  tunggu_boot
  tunggu_properti
}

# Berapa pembacaan penuh berturut-turut yang membuktikan emulator benar-benar tenang.
SIAP_BERTURUT=3

# Sesudah adb dibangkitkan ulang, emulator butuh waktu sampai benar-benar responsif. Dahulu di
# sini berdiri `sleep 15` — angka tunggu yang tak berhubungan dengan apa pun yang diperiksa, dan
# justru itu yang membuat ulangan tetap gagal meski perangkat "sudah siap" menurut urutan
# penyiapan. Yang ditunggu kini adalah buktinya sendiri: SIAP_BERTURUT pembacaan penuh
# berturut-turut yang semuanya selesai dalam batas waktu ddmlib, sampai BATAS_TENANG detik.
tunggu_tenang() {
  local berturut=0
  local batas=$((SECONDS + BATAS_TENANG))
  while [ "$SECONDS" -lt "$batas" ]; do
    if perintah_siap; then
      berturut=$((berturut + 1))
      [ "$berturut" -ge "$SIAP_BERTURUT" ] && return 0
    else
      berturut=0
    fi
    sleep 2
  done
  return 1
}

jalankan_uji() {
  ./gradlew :app:connectedDebugAndroidTest --build-cache > "$LOG" 2>&1
}

# plan.txt: "Pemeriksaan crash serta error log". Uji yang lulus bisa saja melewati proses
# yang sempat mati, jadi buffer crash/ANR diperiksa terpisah — selagi emulator masih hidup,
# sebelum aksi ini mematikannya.
periksa_crash() {
  local paket="id.or.kemenag.yasintahlil"
  local crash
  crash="$(adb logcat -d -b crash 2>/dev/null | grep -A 25 "FATAL EXCEPTION" || true)"
  if printf '%s' "$crash" | grep -q "$paket"; then
    echo "::error::Crash terdeteksi pada $paket selama uji instrumented"
    printf '%s\n' "$crash"
    return 1
  fi
  local anr
  anr="$(adb logcat -d 2>/dev/null | grep -E "ANR in $paket" || true)"
  if [ -n "$anr" ]; then
    echo "::error::ANR terdeteksi pada $paket selama uji instrumented"
    printf '%s\n' "$anr"
    return 1
  fi
  echo "Pemeriksaan crash: bersih (tidak ada FATAL EXCEPTION/ANR pada $paket)."
}

adb start-server
siapkan_perangkat || exit 1

if jalankan_uji; then
  tail -n 15 "$LOG"
  periksa_crash || exit 1
  exit 0
fi
tail -n 60 "$LOG"

# Hanya kegagalan ber-tanda-tangan infrastruktur yang diulang.
if grep -qE "ShellCommandUnresponsiveException|Unknown API Level|No compatible devices connected|Unable to connect to adb daemon" "$LOG"; then
  echo "::warning::Emulator tak responsif (bukan kegagalan uji) — mengulang sekali"
  adb kill-server || true
  adb start-server
  siapkan_perangkat || exit 1
  tunggu_tenang || { echo "::error::Emulator tidak kunjung tenang untuk pembacaan properti penuh"; exit 1; }
  if jalankan_uji; then
    tail -n 15 "$LOG"
    periksa_crash || exit 1
    exit 0
  fi
  tail -n 60 "$LOG"
fi

echo "::error::Uji instrumented gagal"
exit 1
