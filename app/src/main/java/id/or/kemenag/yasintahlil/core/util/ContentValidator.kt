package id.or.kemenag.yasintahlil.core.util

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
}
