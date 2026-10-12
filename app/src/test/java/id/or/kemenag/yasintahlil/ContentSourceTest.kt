package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Menjaga aturan SOURCES.md: setiap file konten wajib mencantumkan `sumber_url`
 * dengan domain resmi. Dibaca langsung dari disk agar jalan tanpa Robolectric.
 */
class ContentSourceTest {

    private fun contentDir(): File {
        val candidates = listOf(
            File("src/main/assets/content"),
            File("app/src/main/assets/content"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Direktori assets/content tidak ditemukan dari ${File(".").absolutePath}")
    }

    @Test
    fun everyContentFileDeclaresOfficialSource() {
        val files = contentDir().listFiles { f -> f.extension == "json" }.orEmpty()
        assertTrue("Tidak ada file konten ditemukan", files.isNotEmpty())

        val problems = mutableListOf<String>()
        files.forEach { f ->
            val text = f.readText()
            val url = Regex("\"sumber_url\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)
            when {
                url == null -> problems += "${f.name}: tanpa sumber_url"
                !ContentValidator.isOfficialSource(url) -> problems += "${f.name}: domain tidak resmi ($url)"
            }
        }
        assertTrue("Pelanggaran sumber resmi: $problems", problems.isEmpty())
    }

    /** Tidak boleh ada teks instruksi yang menyamar sebagai lafaz Arab. */
    @Test
    fun arabicFieldsAreNotPlaceholders() {
        val badMarkers = listOf(
            "lafaz niat per sholat", "... (sesuai", "TODO", "Lorem",
            "lihat rujukan file", "dibaca penuh dari mushaf",
        )
        val files = contentDir().listFiles { f -> f.extension == "json" }.orEmpty()
        val problems = mutableListOf<String>()
        files.forEach { f ->
            val text = f.readText()
            badMarkers.forEach { m -> if (text.contains(m)) problems += "${f.name}: '$m'" }
        }
        assertTrue("Placeholder terdeteksi: $problems", problems.isEmpty())
    }

    /**
     * Setiap entri dzikir wajib menyebut sumbernya sendiri, bukan hanya mengandalkan
     * `sumber_url` tingkat berkas. Tanpa itu pengguna tak bisa membedakan ayat Qur'an,
     * hadis sahih, dan hadis daif (mis. doa masuk rumah & doa sesudah makan).
     *
     * Dihitung lewat jumlah kunci: `"judul"` ada pada tiap kelompok sekaligus tiap entri,
     * sedangkan `"items"` hanya pada kelompok — jadi judul - kelompok = jumlah entri.
     */
    @Test
    fun everyDzikirItemDeclaresItsOwnSource() {
        val text = File(contentDir(), "dzikir.json").readText()
        fun count(key: String) = Regex("\"$key\"\\s*:").findAll(text).count()
        val entri = count("judul") - count("items")
        val bersumber = count("sumber")
        assertTrue("dzikir.json tanpa entri yang diperiksa", entri > 0)
        assertTrue(
            "Ada $entri entri dzikir tetapi hanya $bersumber yang punya sumber sendiri",
            bersumber >= entri,
        )
    }

    /**
     * wudhu.json sengaja tanpa teks Arab (lafaz menunggu verifikasi sumber resmi). Uji ini
     * menjaga agar tidak ada yang diam-diam menyisipkan teks Arab hasil karangan ke berkas itu.
     */
    @Test
    fun wudhuContentHasNoFabricatedArabic() {
        val f = File(contentDir(), "wudhu.json")
        assertTrue("wudhu.json tidak ditemukan", f.isFile)
        val text = f.readText()
        val arabic = Regex("[\\u0600-\\u06FF]").find(text)
        assertTrue("wudhu.json memuat teks Arab tanpa sumber: '${arabic?.value}'", arabic == null)
        assertTrue("wudhu.json kehilangan bagian tayamum", text.contains("tayamum"))
        assertTrue("wudhu.json kehilangan bagian pembatal", text.contains("pembatal"))
    }

    /** Konten ghaib/jenazah harus berada di assets, bukan di kode Kotlin. */
    @Test
    fun ghaibAndJenazahContentLivesInAssets() {
        val ghaib = File(contentDir(), "sholat_ghaib.json").readText()
        val jenazah = File(contentDir(), "sholat_jenazah.json").readText()
        val mayit = File(contentDir(), "sholat_mayit.json").readText()
        val arabicInField = Regex("\"arab\"\\s*:\\s*\"[^\"]*[\\u0600-\\u06FF]")

        assertTrue("sholat_ghaib.json tanpa varian niat ber-Arab", arabicInField.containsMatchIn(ghaib))
        assertTrue("sholat_ghaib.json tanpa array niat", ghaib.contains("\"niat\""))
        assertTrue("sholat_jenazah.json tanpa varian niat ber-Arab", arabicInField.containsMatchIn(jenazah))
        assertTrue("sholat_mayit.json tanpa doa_mayit", mayit.contains("\"doa_mayit\""))
        assertTrue("sholat_mayit.json tanpa doa_mayit_tambahan", mayit.contains("\"doa_mayit_tambahan\""))
        // Doa mayit tidak boleh tercampur ke langkah sholat umum.
        val umum = File(contentDir(), "sholat_bacaan.json").readText()
        assertTrue("Doa mayit bocor ke sholat_bacaan.json", !umum.contains("doa_mayit"))
    }

    /**
     * Tidak boleh ada teks Arab hardcoded di kode aplikasi mana pun: seluruh teks ibadah wajib
     * berasal dari assets agar tervalidasi gate konten (`tools/validate_content.py`). Uji ini
     * menggantikan pemeriksaan per-layar dan berlaku untuk seluruh `app/src/main/java`.
     */
    @Test
    fun appCodeHasNoHardcodedArabic() {
        val roots = listOf(File("src/main/java"), File("app/src/main/java"))
        val srcRoot = roots.firstOrNull { it.isDirectory }
            ?: error("Direktori sumber tidak ditemukan dari ${File(".").absolutePath}")
        val arabic = Regex("[\\u0600-\\u06FF]")
        val problems = mutableListOf<String>()
        srcRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { f ->
            arabic.find(f.readText())?.let { problems += "${f.relativeTo(srcRoot)}: '${it.value}'" }
        }
        assertTrue("Teks Arab hardcoded di kode (pindahkan ke assets): $problems", problems.isEmpty())
    }

    /**
     * Bacaan yang tadinya kosong (doa keluar masjid/masuk rumah/safar, lafaz sholawat setelah
     * adzan) sudah diisi dari sumber resmi — jangan sampai hilang lagi. Lihat
     * docs/CONTENT_SOURCES.md §"Kekurangan yang masih menunggu sumber resmi".
     */
    @Test
    fun previouslyMissingDoaArePresent() {
        val dzikir = File(contentDir(), "dzikir.json").readText()
        listOf("Doa keluar masjid", "Doa masuk rumah", "Doa bepergian (safar)").forEach { judul ->
            assertTrue("dzikir.json kehilangan '$judul'", dzikir.contains(judul))
        }
        // Doa masuk rumah wajib menyebut derajat hadisnya secara terbuka (daif menurut Al-Albani).
        val i = dzikir.indexOf("Doa masuk rumah")
        val block = dzikir.substring(i, dzikir.indexOf("\"terjemahan\"", i).takeIf { it > i } ?: dzikir.length)
        assertTrue("Doa masuk rumah tanpa keterangan derajat hadis", block.contains("daif"))

        val adzan = File(contentDir(), "adzan.json").readText()
        assertTrue("adzan.json kehilangan sholawat setelah adzan",
            adzan.contains("\"sholawat_setelah_adzan\""))
        assertTrue("adzan.json kehilangan catatan menjawab adzan",
            adzan.contains("\"menjawab_adzan\""))
    }

    /**
     * Pemilihan mazhab mengubah bacaan yang tampil. Uji ini memastikan entri yang memang
     * khusus satu mazhab (mis. qunut = Syafi'i) ditandai dengan benar dan tag-nya dikenal.
     */
    @Test
    fun mazhabTaggedRecitationsAreValid() {
        val text = File(contentDir(), "sholat_bacaan.json").readText()

        val start = text.indexOf("\"id\": \"qunut\"")
        assertTrue("Entri qunut tidak ditemukan", start >= 0)
        val next = text.indexOf("\"id\":", start + 1)
        val qunutBlock = if (next > start) text.substring(start, next) else text.substring(start)
        assertTrue("qunut tidak ditandai mazhab syafii", qunutBlock.contains("\"mazhab\": \"syafii\""))
        // Qunut hanya untuk Subuh — jangan sampai muncul di sholat lain.
        assertTrue("qunut tidak dibatasi ke sholat subuh", qunutBlock.contains("\"sholat\": [\"subuh\"]"))

        val allowed = setOf("syafii", "hanafi", "maliki", "hambali")
        Regex("\"mazhab\"\\s*:\\s*\"([^\"]+)\"").findAll(text).forEach {
            assertTrue("Tag mazhab tak dikenal: ${it.groupValues[1]}", it.groupValues[1] in allowed)
        }

        // Cakupan sholat harus merujuk id sholat yang benar-benar ada di daftar.
        val known = Regex("\"id\": \"([a-z_]+)\"")
            .findAll(File(contentDir(), "sholat_daftar.json").readText())
            .map { it.groupValues[1] }.toSet()
        Regex("\"sholat\"\\s*:\\s*\\[([^\\]]*)\\]").findAll(text).forEach { m ->
            Regex("\"([^\"]+)\"").findAll(m.groupValues[1]).forEach { s ->
                assertTrue("Cakupan sholat tak dikenal: ${s.groupValues[1]}", s.groupValues[1] in known)
            }
        }
    }

    /** Penjelasan Jumat & safar harus prosedural (tanpa teks Arab) dan benar-benar ada. */
    @Test
    fun jumatAndSafarHaveProceduralKetentuan() {        val text = File(contentDir(), "sholat_daftar.json").readText()
        val arabic = Regex("[\\u0600-\\u06FF]")
        listOf("jumat", "safar").forEach { id ->
            val start = text.indexOf("\"id\": \"$id\"")
            assertTrue("Entri $id tidak ditemukan", start >= 0)
            val next = text.indexOf("\"id\":", start + 1)
            val block = if (next > start) text.substring(start, next) else text.substring(start)
            assertTrue("$id tanpa daftar ketentuan", block.contains("\"ketentuan\""))
            val ketStart = block.indexOf("\"ketentuan\"")
            val ketEnd = block.indexOf("]", ketStart).takeIf { it > ketStart } ?: block.length
            val ket = block.substring(ketStart, ketEnd)
            assertTrue("Ketentuan $id memuat teks Arab tanpa sumber", !arabic.containsMatchIn(ket))
        }
    }

    /**
     * Teks Al-Qur'an wajib memakai Mushaf Standar Indonesia (MSI) LPMQ Kemenag beserta
     * transliterasi Latin resmi Kemenag — bukan teks Utsmani dari Tanzil maupun transliterasi
     * Inggris dari api.quran.com. Lihat SOURCES.md §1 dan tools/fetch_quran_kemenag.py.
     */
    @Test
    fun quranTextUsesMsiAndOfficialTransliteration() {
        val quranFiles = listOf(
            "yasin_full.json", "surah_001.json", "surah_112.json",
            "surah_113.json", "surah_114.json", "ayat_kursi.json",
        )
        quranFiles.forEach { name ->
            val text = File(contentDir(), name).readText()
            assertTrue("$name: sumber_arab bukan MSI LPMQ Kemenag",
                text.contains("\"sumber_arab\": \"Mushaf Standar Indonesia (MSI) LPMQ Kemenag RI"))
            assertTrue("$name: sumber_latin bukan transliterasi resmi Kemenag",
                text.contains("\"sumber_latin\": \"Transliterasi resmi Kemenag RI"))
        }
    }

    /** Tidak boleh ada berkas konten yang masih merujuk sumber teks non-resmi. */
    @Test
    fun noContentFileCitesNonOfficialTextSource() {
        val terlarang = listOf("api.quran.com", "api.alquran.cloud", "Tanzil")
        val files = contentDir().listFiles { f -> f.extension == "json" }.orEmpty()
        val problems = mutableListOf<String>()
        files.forEach { f ->
            val text = f.readText()
            terlarang.forEach { m -> if (text.contains(m)) problems += "${f.name}: '$m'" }
        }
        assertTrue("Sumber teks non-resmi masih dirujuk: $problems", problems.isEmpty())
    }
}
