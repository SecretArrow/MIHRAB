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
     * Ejaan kunci **sumber tingkat berkas** yang diterima skema, berurutan menurut prioritas.
     *
     * Satu tempat, karena kunci yang sama dibaca di tiga peran: pemeriksa bundel
     * (`BundelKonten.periksaBerkas`), ringkasan berkas yang ditampilkan layar
     * (`BundelKonten.ringkasan`), dan pembaca konten yang benar-benar memakainya
     * (`ContentRepo.dariPembaruan`/`dariAset`). Gerbang konten di `tools/` juga mengenal keduanya.
     *
     * Dahulu ketiga peran Kotlin itu menuliskan daftar ini sendiri-sendiri, dan justru pembaca
     * kontenlah yang tidak mengenal ejaan kedua — sehingga bundel ber-`sumberUrl` **lolos**
     * pemasangan dan dilaporkan *"terpasang"*, tetapi setiap berkasnya ditolak pembaca saat dibuka
     * (tanpa `sumber_url`, berkas pembaruan dianggap tidak bersumber) dan aplikasi diam-diam kembali
     * memakai konten bawaan. Pembaruan yang tampak berhasil tetapi tidak mengubah apa pun — dan tak
     * ada satu pun pesan yang menyebut sebabnya.
     */
    val KUNCI_SUMBER = listOf("sumber_url", "sumberUrl")

    /**
     * Ejaan yang **ditulis** aplikasi sendiri — yang pertama pada [KUNCI_SUMBER].
     *
     * Dipakai `MetaPembaruan.tulis` saat mencatat sumber tiap berkas yang dipasang.
     * Menulis lewat konstanta ini menjaga agar tak ada ejaan kunci yang ditulis di luar berkas ini,
     * sekaligus aman: pembacanya ([sumberBerkas]) menerima kedua ejaan, jadi mengubah urutan
     * prioritas tidak membuat metadata lama tak terbaca.
     */
    val KUNCI_SUMBER_UTAMA: String get() = KUNCI_SUMBER.first()

    /**
     * Sumber tingkat berkas pada [dokumen]: ejaan pertama pada [KUNCI_SUMBER] yang terisi, atau
     * `""` bila tidak ada yang menyatakan sumber.
     *
     * Dipakai pemeriksa **dan** pembaca konten, supaya ejaan yang lolos pemeriksaan adalah ejaan
     * yang benar-benar dibaca.
     */
    fun sumberBerkas(dokumen: JSONObject): String =
        KUNCI_SUMBER.firstNotNullOfOrNull { k ->
            dokumen.optString(k).takeIf { it.isNotBlank() }
        }.orEmpty()

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
