#!/usr/bin/env bash
# pakai-keystore.sh — buka brankas keystore rilis "Yasin dan Tahlil" sesaat, lalu bersihkan.
#
#   ./pakai-keystore.sh buka [dir]        # dekripsi + unzip ke <dir> (bawaan: ./.keystore-terbuka)
#   ./pakai-keystore.sh bersihkan [dir]   # hapus total hasil buka
#
# Sandi dibaca dari env KS_PASS; bila kosong, ditanyakan tanpa echo.
# Rahasia hanya hidup selama dipakai. Setelah selesai WAJIB dijalankan `bersihkan`.
#
# Repo aplikasinya PUBLIK: jangan pernah menaruh hasil buka di dalam repo, dan jangan
# pernah commit berkas dari sini.
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
ENC="$HERE/key-sign-apk.zip.enc"
ZIP="$HERE/key-sign-apk.zip"

baca_sandi() {
  if [ -n "${KS_PASS:-}" ]; then return; fi
  printf 'Sandi keystore: ' >&2
  read -rs KS_PASS
  printf '\n' >&2
  export KS_PASS
}

aksi=${1:-}
dir=${2:-./.keystore-terbuka}
case "$aksi" in
  buka)
    baca_sandi
    mkdir -p "$dir"
    chmod 700 "$dir"
    tmp=$(mktemp -d)
    trap 'rm -rf "$tmp"' EXIT
    if [ -f "$ENC" ]; then
      openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 \
        -in "$ENC" -out "$tmp/keystore.zip" -pass env:KS_PASS
    else
      cp "$ZIP" "$tmp/keystore.zip"
    fi
    ( cd "$dir" && unzip -q -o -P "$KS_PASS" "$tmp/keystore.zip" )
    chmod 600 "$dir"/release.jks
    cat > "$dir/keystore.properties" <<EOF
storeFile=$(cd "$dir" && pwd)/release.jks
storePassword=$KS_PASS
keyAlias=yasintahlil
keyPassword=$KS_PASS
EOF
    chmod 600 "$dir/keystore.properties"
    cat >&2 <<EOF
Keystore terbuka di : $dir
Properti Gradle     : $dir/keystore.properties

Untuk membangun rilis bertanda tangan, taruh keystore.properties di akar repo:
  cp "$dir/keystore.properties" <repo>/keystore.properties

JANGAN LUPA, setelah selesai:
  $0 bersihkan $dir
EOF
    ;;
  bersihkan)
    if [ -d "$dir" ]; then
      find "$dir" -type f -exec shred -u {} + 2>/dev/null || true
      rm -rf "$dir"
      echo "Dibersihkan: $dir"
    else
      echo "Tidak ada apa-apa di: $dir"
    fi
    ;;
  *)
    sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
    ;;
esac
