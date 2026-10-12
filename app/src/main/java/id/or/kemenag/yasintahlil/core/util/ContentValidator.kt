package id.or.kemenag.yasintahlil.core.util

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

// Validasi konten keagamaan: setiap entri wajib punya sumber resmi (lihat SOURCES.md).
object ContentValidator {
    val allowedHosts = listOf("kemenag.go.id", "kemenag.dev", "sunnah.com", "bmkg.go.id", "nu.or.id")

    /** Host dari URL, atau null bila URL tidak valid. */
    fun hostOf(url: String): String? =
        runCatching { URI(url.trim()).host?.lowercase() }.getOrNull()

    /** True hanya bila host PERSIS sama atau subdomain dari domain resmi (anti-spoofing). */
    fun isOfficialSource(url: String): Boolean {
        val host = hostOf(url) ?: return false
        return allowedHosts.any { host == it || host.endsWith(".$it") }
    }

    fun requireOfficial(url: String) {
        require(url.isNotBlank()) { "sumber_url wajib diisi" }
        require(isOfficialSource(url)) { "sumber harus resmi: $url" }
    }

    /**
     * Seluruh URL yang dinyatakan sebagai **sumber** di dalam [node], di kedalaman mana pun.
     *
     * Bukan hanya `sumber_url` tingkat berkas: berkas konten juga mencantumkan `sumber_tambahan`
     * berbentuk **daftar** URL (mis. `sholat_ghaib.json`), dan nilai di dalam daftar itulah yang
     * dahulu luput dari pemeriksaan — baik di gate `tools/validate_content.py` (yang hanya
     * menelusuri objek) maupun di daftar sumber layar Tentang (yang hanya membaca `sumber_url`).
     * Akibatnya domain non-resmi dapat bersembunyi di sana tanpa satu pun pemeriksaan gagal,
     * padahal layar Tentang menyatakan kepada pengguna bahwa berkas yang menyebut domain lain
     * ditolak saat dibangun.
     */
    fun sumberUrl(node: JSONObject): List<String> {
        val hasil = mutableListOf<String>()
        telusuri(node, "", hasil)
        return hasil
    }

    private fun telusuri(nilai: Any?, kunci: String, hasil: MutableList<String>) {
        when (nilai) {
            is JSONObject -> nilai.keys().forEach { k -> telusuri(nilai.opt(k), k, hasil) }
            // Kunci induk dibawa turun ke elemen daftar supaya `sumber_tambahan[i]` tetap dikenali
            // sebagai nilai bersumber.
            is JSONArray -> for (i in 0 until nilai.length()) telusuri(nilai.opt(i), kunci, hasil)
            is String -> {
                val url = nilai.trim()
                if (kunci.contains("sumber", ignoreCase = true) && url.startsWith("http")) hasil += url
            }
        }
    }
}
