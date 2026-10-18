package id.or.kemenag.yasintahlil.core.legal

import android.content.Context
import id.or.kemenag.yasintahlil.core.util.ContentValidator

/**
 * Dokumen legal yang dibundel di `assets/legal/`.
 *
 * Berkas di sini adalah **salinan apa adanya** dari `docs/PRIVACY.md`, `docs/PERMISSIONS.md`, dan
 * `LICENSE` di repositori; uji memastikan ketiganya identik byte per byte. Dengan begitu kebijakan
 * privasi dan daftar izin yang dibaca pengguna di dalam aplikasi tidak dapat menyimpang dari yang
 * didokumentasikan — versi aplikasi dan versi repositori selalu sama.
 */
object DokumenLegal {
    const val PRIVASI = "legal/PRIVACY.md"
    const val IZIN = "legal/PERMISSIONS.md"
    const val LISENSI = "legal/LICENSE.txt"

    /** Isi dokumen, atau null bila asetnya tak terbaca (mis. berkas hilang saat pemaketan). */
    fun baca(ctx: Context, nama: String): String? =
        runCatching { ctx.assets.open(nama).bufferedReader().readText() }.getOrNull()
}

/**
 * Nama lembaga di balik setiap domain resmi yang boleh dipakai konten.
 *
 * Daftar domainnya sendiri **tidak ditulis ulang** di sini: sumbernya [ContentValidator.allowedHosts],
 * yang juga dipakai gerbang CI untuk menolak konten dari domain luar. Kalau daftar itu bertambah,
 * uji menuntut domain barunya diberi nama di sini — sehingga layar Tentang tak pernah menampilkan
 * domain resmi tanpa keterangan siapa pemiliknya.
 */
object SumberKonten {
    private val nama = mapOf(
        "kemenag.go.id" to "Kementerian Agama RI — LPMQ (teks Al-Qur'an dan terjemahannya)",
        "kemenag.dev" to "SIHAT Bimas Islam Kemenag (jadwal sholat dan arah kiblat)",
        "sunnah.com" to "sunnah.com (teks hadis beserta penomoran rujukannya)",
        "bmkg.go.id" to "BMKG (rujukan hilal dan kalender)",
        "nu.or.id" to "NU Online (rujukan amalan dan doa)",
    )

    /** Domain induk resmi untuk [host] (yang boleh jadi subdomain), atau null bila di luar daftar. */
    fun induk(host: String): String? {
        val h = host.trim().lowercase()
        return ContentValidator.allowedHosts.firstOrNull { h == it || h.endsWith(".$it") }
    }

    /** Keterangan pemilik domain, atau null bila domainnya tidak dikenal. */
    fun label(host: String): String? = induk(host)?.let { nama[it] }
}
