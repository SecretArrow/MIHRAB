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

set -u

LOG=/tmp/e2e-connected.log

tunggu_boot() {
  until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    sleep 2
  done
}

# Tunggu adb benar-benar bisa membaca properti. Tanpa ini AGP melewati perangkat
# dengan "Unknown API Level" dan seluruh uji dilewati tanpa dijalankan.
tunggu_properti() {
  local api=""
  local i
  for i in $(seq 1 90); do
    api="$(adb shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')"
    [ -n "$api" ] && break
    sleep 2
  done
  echo "Perangkat: $(adb devices | tr '\n' ' ') | API='$api'"
  [ -n "$api" ] || { echo "::error::Emulator tidak melaporkan API level"; return 1; }
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
adb wait-for-device
tunggu_boot
tunggu_properti || exit 1

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
  adb wait-for-device
  tunggu_boot
  sleep 15
  if jalankan_uji; then
    tail -n 15 "$LOG"
    periksa_crash || exit 1
    exit 0
  fi
  tail -n 60 "$LOG"
fi

echo "::error::Uji instrumented gagal"
exit 1
