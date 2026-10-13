package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Uji instrumented (`src/androidTest`) harus menunjuk layar lewat [NamaLayar], bukan mengeja
 * namanya sendiri.
 *
 * Pada 2026-10-12 nama layar disatukan ke [NamaLayar] (`ea3c366`): menu Ibadah berhenti menulis
 * *"Yasin"* dan mulai memakai `NamaLayar.YASIN` — *"Surat Yasin"*. `NavSmokeTest` tidak ikut
 * berubah: ia masih menekan `onNodeWithText("Yasin")`, pencocokan **tepat**, sehingga tak ada satu
 * simpul pun yang cocok. Uji itu gagal, tetapi bukan karena aplikasinya salah — dan sejak commit
 * itu **setiap penggabungan ke `main` meninggalkan job `e2e` merah**, sehingga sinyalnya berhenti
 * berarti: kegagalan sungguhan tidak lagi dapat dibedakan dari kegagalan yang sudah biasa.
 *
 * Yang salah bukan teksnya, melainkan **salinannya**. Uji yang mengeja ulang nama layar menguji
 * ejaan, bukan navigasi: ia hanya lulus selama dua salinan itu kebetulan sama. Karena itu uji ini
 * memaku dua hal:
 *
 *  1. **tidak ada salinan** — argumen pencocok simpul (`onNodeWithText`, `hasText`, …) di seluruh
 *     `src/androidTest` tidak boleh memuat nama layar dari [NamaLayar], dan juga tidak boleh menjadi
 *     penggalan salah satunya (itulah bentuk kegagalannya: `"Yasin"` adalah penggalan *"Surat
 *     Yasin"*);
 *  2. **yang dipakai memang nama bersama** — `NavSmokeTest`, satu-satunya uji yang menavigasi antar
 *     layar, benar-benar memakai `NamaLayar.*`, termasuk untuk tombol Yasin yang dahulu basi.
 *
 * **Batasnya.** Yang diperiksa hanya **argumen pencocok simpul**, bukan setiap literal di berkas
 * uji. Pesan kegagalan seperti `assertTrue("Surat Yasin tidak menyebut sumber teks Arab", …)`
 * memang menyebut nama layar — dan itu wajar, sebab ia menjelaskan; yang tidak wajar adalah
 * **mencari** simpul dengan nama yang disalin. Daftar pencocoknya sengaja ditulis lengkap di
 * [POLA_ARGUMEN] supaya batas itu terlihat.
 */
class NamaLayarDiUjiInstrumentedTest {

    private fun root(): File = KodeSumber.root()

    private fun berkasUji(): List<File> =
        File(root(), "src/androidTest/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun navSmoke(): File =
        File(root(), "src/androidTest/java/id/or/kemenag/yasintahlil/NavSmokeTest.kt")

    /** Kode tanpa komentar: KDoc yang menjelaskan pola lama memuat tulisan yang sama persis. */
    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    // ---- Nama layar, dibaca dari sumbernya sendiri ---------------------------------------------

    /**
     * Nilai tiap `const val` di [NamaLayar], **dibaca dari berkasnya** — bukan disalin ke daftar
     * kedua di sini. Daftar salinan akan menjadi persis masalah yang dijaga uji ini: menambah nama
     * layar tidak akan ikut terjaga, tanpa galat apa pun.
     */
    private fun nilaiNamaLayar(): List<String> {
        val teks = KodeSumber.kodeTanpaKomentar(
            KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/core/util/NamaLayar.kt"),
        )
        return Regex("const val [A-Z_]+ = \"([^\"]*)\"")
            .findAll(teks).map { it.groupValues[1] }.toList()
    }

    // ---- Argumen pencocok simpul ---------------------------------------------------------------

    /**
     * Nama fungsi Compose-test yang argumen pertamanya adalah teks yang **dicari** di layar.
     * Ditulis lengkap dan sengaja: yang di luar daftar ini (mis. pesan `assertTrue`) tidak diperiksa,
     * sebab di sana menyebut nama layar justru menjelaskan.
     */
    private val pencocok = listOf(
        "onNodeWithText", "onAllNodesWithText",
        "onNodeWithContentDescription", "onAllNodesWithContentDescription",
        "onNodeWithSubstring", "hasText", "hasContentDescription",
    )

    private val polaArgumen = Regex(
        "(?:${pencocok.joinToString("|")})\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
    )

    /**
     * String mentah (`"""…"""`) disamakan bentuknya dengan string biasa, supaya satu pola cukup
     * untuk keduanya. Isi yang memuat tanda kutip sendiri tidak diurai sempurna — bentuk itu tidak
     * muncul sebagai argumen pencocok, dan pemindainya sengaja tidak dibuat rumit untuknya.
     */
    private fun rapikanStringMentah(teks: String): String =
        Regex("\"\"\"(.*?)\"\"\"", RegexOption.DOT_MATCHES_ALL)
            .replace(teks) { "\"${it.groupValues[1]}\"" }

    /**
     * Argumen pencocok simpul berisi literal, di luar komentar. Rujukan `NamaLayar.*` tak masuk.
     *
     * Komentar dibuang (KDoc yang menjelaskan pola lama memuat pemanggilan yang sama persis),
     * tetapi string **mentah** dipertahankan: argumen pun boleh ditulis `onNodeWithText("""Yasin""")`,
     * dan bentuk itu justru paling mudah luput karena pemindai bawaan membuang isi string mentah.
     */
    private fun argumenPencocok(teks: String): List<String> =
        polaArgumen.findAll(rapikanStringMentah(KodeSumber.kodeTanpaKomentar(teks, mentah = true)))
            .map { it.groupValues[1] }.toList()

    /**
     * [teks] tanpa sisipan templat (`${NamaLayar.IBADAH}` dan `$nama`) — yang tersisa hanyalah
     * bagian yang benar-benar dieja di berkas uji. Rujukan ke [NamaLayar] **bukan** salinan, jadi
     * ia harus hilang sebelum diperiksa; kalau tidak, `"Menu ${NamaLayar.IBADAH}"` akan dituduh
     * menyalin padahal justru memakai nama bersama.
     */
    private fun tanpaTemplate(teks: String): String =
        teks.replace(Regex("""\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_.]*"""), "")

    /**
     * Apakah [argumen] memakai [nama] — sebagai bagian darinya (`"Menu Ibadah"`), sama persis, atau
     * **penggalan** darinya (`"Yasin"` dari *"Surat Yasin"*).
     *
     * Arah penggalan itulah bentuk kegagalan yang sesungguhnya terjadi, sehingga harus ikut
     * diperiksa. Argumen lebih pendek dari empat huruf diabaikan supaya kata sambung pendek tidak
     * dianggap penggalan nama.
     */
    private fun miripNamaLayar(argumen: String, nama: String): Boolean =
        argumen.contains(nama) || (argumen.length >= 4 && nama.contains(argumen))

    // ---- Uji -----------------------------------------------------------------------------------

    /** Prasyarat: kalau daftar namanya kosong, penjaganya hijau tanpa memeriksa apa pun. */
    @Test
    fun daftarNamaLayarTerbacaDariSumbernya() {
        val nama = nilaiNamaLayar()
        assertTrue("tak satu pun nama layar terbaca dari NamaLayar.kt — pola pengambilnya rusak",
            nama.size >= 18)
        assertTrue("ada nama layar kosong", nama.none { it.isBlank() })
        assertEquals("nama layar kembar membuat penjaganya ambigu", nama.size, nama.toSet().size)
    }

    @Test
    fun ujiInstrumentedTidakMenyalinNamaLayar() {
        val nama = nilaiNamaLayar()
        val pelanggar = mutableListOf<String>()
        berkasUji().forEach { f ->
            argumenPencocok(f.readText()).forEach { argumen ->
                val dieja = tanpaTemplate(argumen)
                nama.filter { miripNamaLayar(dieja, it) }
                    .forEach { pelanggar += "${f.name}: \"$dieja\" menyalin nama layar \"$it\"" }
            }
        }
        assertTrue(
            "uji instrumented mengeja nama layar, bukan memakai NamaLayar: $pelanggar",
            pelanggar.isEmpty(),
        )
    }

    /**
     * Perbaikannya harus terlihat: `NavSmokeTest` menavigasi lewat nama bersama, termasuk untuk
     * tombol Yasin yang dahulu basi. Tanpa ini, penjaga di atas dapat dipuaskan dengan menghapus
     * uji navigasinya — padahal yang salah adalah salinannya, bukan ujinya.
     */
    @Test
    fun navSmokeTestMenavigasiLewatNamaBersama() {
        val k = kode(navSmoke())
        listOf("IBADAH", "ALAT", "PENGATURAN", "BERANDA", "YASIN").forEach { kunci ->
            assertTrue("NavSmokeTest tidak memakai NamaLayar.$kunci untuk menavigasi",
                k.contains("NamaLayar.$kunci"))
        }
        // Prasyarat pemindai: argumen pencocoknya benar-benar terbaca.
        assertTrue("tak ada argumen pencocok simpul yang terbaca dari NavSmokeTest",
            argumenPencocok(navSmoke().readText()).isNotEmpty())
    }

    /**
     * Penjaga di atas hanya berarti bila pemindainya benar-benar bekerja: templat harus dibuang
     * (rujukan bukan salinan), sedangkan ejaan biasa harus tertangkap — termasuk bentuk penggalan
     * yang dahulu lolos (`"Yasin"`).
     */
    @Test
    fun pemindaiMembedakanRujukanDariEjaan() {
        val rujukan = "rule.onNodeWithText(\"Menu \${NamaLayar.IBADAH}\").assertIsDisplayed()"
        assertEquals("argumen pencocok tidak terbaca dari templat",
            listOf("Menu \${NamaLayar.IBADAH}"), argumenPencocok(rujukan))
        assertEquals("templat tidak dibuang sebelum diperiksa", "Menu ", tanpaTemplate(argumenPencocok(rujukan).single()))

        val ejaan = "rule.onNodeWithText(\"Yasin\").performClick()"
        assertEquals(listOf("Yasin"), argumenPencocok(ejaan))
        assertTrue("\"Yasin\" (penggalan \"Surat Yasin\") tidak dikenali sebagai salinan nama layar",
            miripNamaLayar(tanpaTemplate(argumenPencocok(ejaan).single()), NamaLayar.YASIN))

        // Komentar yang menjelaskan pola lama tidak boleh dihitung sebagai pelanggaran.
        assertTrue("pemindai menghitung argumen di dalam komentar",
            argumenPencocok("// dahulu: onNodeWithText(\"Yasin\")").isEmpty())

        // Argumen berupa string mentah tetap terbaca — pemindai bawaan membuang isinya.
        assertEquals("argumen string mentah luput dari pemindai",
            listOf("Yasin"), argumenPencocok("rule.onNodeWithText(\"\"\"Yasin\"\"\").performClick()"))
    }
}
