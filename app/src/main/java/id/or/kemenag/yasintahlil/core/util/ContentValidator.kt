package id.or.kemenag.yasintahlil.core.util

// Validasi konten keagamaan: setiap entri wajib punya sumber resmi.
object ContentValidator {
    val allowedHosts = listOf("kemenag.go.id", "kemenag.dev", "sunnah.com", "bmkg.go.id", "nu.or.id")

    fun isOfficialSource(url: String): Boolean =
        allowedHosts.any { url.contains(it) }

    fun requireOfficial(url: String) {
        require(url.isNotBlank()) { "sumber_url wajib diisi" }
        require(isOfficialSource(url)) { "sumber harus resmi: $url" }
    }
}
