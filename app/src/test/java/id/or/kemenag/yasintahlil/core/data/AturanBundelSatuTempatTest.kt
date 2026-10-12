package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Aturan bundel pembaruan konten harus tetap **satu** — dipakai aplikasi saat memasang bundel yang
 * dipilih pengguna *dan* gate CI yang menilai bundel itu saat dibangun.
 *
 * Dua aturan yang dahulu punya salinan di kedua sisi:
 *
 *  - **Prosa penunjuk** ([BundelKonten.PLACEHOLDER]): daftar kata yang menandakan teks belum
 *    diverifikasi. `tools/validate_content.py` menyimpan `PLACEHOLDER_MARKERS` tersendiri, dan tak
 *    ada apa pun yang membandingkannya. Bila daftar Python lebih longgar, prosa seperti *"lihat
 *    rujukan file"* lolos gate lalu **ditampilkan kepada pengguna sebagai bacaan** — tepat yang
 *    dilarang aturan "jangan karang teks agama". Bila lebih ketat, bundel yang sah ditolak saat
 *    dipasang (pembaruan tampak berhasil tetapi tidak mengubah apa pun).
 *  - **Aturan nama berkas** ([BundelKonten.namaBerkasSah]): `tools/validate_bundle.py` menulis
 *    ulang seluruh aturannya — batas `6`, batas `64`, akhiran `.json`, awalan terlarang, dan
 *    pemisah jalur — sebagai `nama_berkas_sah` tersendiri. Penyimpangannya paling sunyi dari
 *    semuanya, sebab **tak satu pun bundel contoh di `tools/fixtures/bundel/` punya nama yang
 *    cacat**: aturan itu tidak pernah dijalankan gate, sehingga salinan yang salah tetap hijau.
 *  - **Field yang dinilai** ([BundelKonten.FIELD_TEKS]): field tempat prosa penunjuk dan harakat
 *    dicari dahulu dieja di tiga tempat — badan `fieldTeks`, pemeriksa `check_placeholders` di gate
 *    Python, dan baris aturan `docs/CONTENT_UPDATE.md` yang menyebut field yang sama kepada
 *    pembaca. Field yang ditambahkan di satu sisi saja menyempitkan pemeriksaan tanpa galat: prosa
 *    penunjuk di field baru lolos ke layar, atau dokumen menjanjikan pemeriksaan yang tak pernah
 *    dijalankan.
 *
 * Gate kini **membaca** nilainya dari `BundelKonten.kt`. Uji ini menjaga agar keduanya tidak pernah
 * kembali menyimpan salinan, dan agar pola pengambilnya benar-benar menghasilkan nilai aplikasi —
 * sama seperti `SumberResmiSatuTempatTest` menjaga daftar host resmi.
 */
class AturanBundelSatuTempatTest {

    private fun root(): File = KodeSumber.root()

    /**
     * Akar repositori, tempat `app/` dan `tools/` bersebelahan.
     *
     * Uji berjalan dengan direktori kerja modul (`app/`), jadi [root] menunjuk ke `app` — naik satu
     * tingkat dulu sebelum mencari gate.
     */
    private fun repoRoot(): File =
        generateSequence(root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "tools/validate_bundle.py").isFile }
            ?: error("tools/validate_bundle.py tidak ditemukan dari ${root().absolutePath}")

    private fun berkasMain(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private val berkasBundel = "src/main/java/id/or/kemenag/yasintahlil/core/data/BundelKonten.kt"

    private val gerbangKonten: String
        get() = File(repoRoot(), "tools/validate_content.py").readText()

    private val gerbangBundel: String
        get() = File(repoRoot(), "tools/validate_bundle.py").readText()

    /**
     * Potongan pola pengambil yang ditulis **persis sama** di gate Python. Bila salah satu sisi
     * mengubah polanya sendiri, uji ini gagal lebih dulu daripada gate yang diam-diam membaca
     * daftar kosong.
     */
    private val polaDaftar = """\s*=\s*listOf\(([^)]*)\)"""
    private val polaTeks = """((?:[^"\\]|\\.)*)"""

    /** Isi literal string Kotlin → teks sebenarnya (`\\` → `\`, `\"` → `"`). */
    private fun unescape(teks: String): String {
        val keluar = StringBuilder()
        var i = 0
        while (i < teks.length) {
            if (teks[i] == '\\' && i + 1 < teks.length) {
                keluar.append(teks[i + 1])
                i += 2
            } else {
                keluar.append(teks[i])
                i++
            }
        }
        return keluar.toString()
    }

    /** Nilai `<nama> = listOf("a", "b")` pada sumber Kotlin, dengan pola yang sama seperti gate. */
    private fun daftar(sumber: String, nama: String): List<String> {
        val isi = Regex("""\b$nama\s*=\s*listOf\(([^)]*)\)""").find(sumber)
            ?.groupValues?.get(1)
            ?: error("'$nama = listOf(...)' tidak ditemukan di $berkasBundel")
        return Regex("\"$polaTeks\"").findAll(isi)
            .map { unescape(it.groupValues[1]) }.toList()
    }

    private fun angka(sumber: String, nama: String): Int =
        Regex("""\b$nama\s*=\s*(\d+)""").find(sumber)?.groupValues?.get(1)?.toInt()
            ?: error("'$nama = <angka>' tidak ditemukan di $berkasBundel")

    private fun teks(sumber: String, nama: String): String =
        Regex("\\b$nama\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(sumber)
            ?.groupValues?.get(1)?.let { unescape(it) }
            ?: error("'$nama = \"...\"' tidak ditemukan di $berkasBundel")

    // --- Prosa penunjuk -------------------------------------------------------------------------

    /** Hanya berkas yang menyatakan kebijakannya boleh mengeja prosanya. */
    @Test
    fun prosaPenunjukHanyaDinyatakanDiBundelKonten() {
        val pelanggar = mutableListOf<String>()
        berkasMain().filter { it.name != "BundelKonten.kt" }.forEach { f ->
            val kode = KodeSumber.kodeTanpaKomentar(f.readText())
            BundelKonten.PLACEHOLDER.filter { kode.contains("\"$it\"") }
                .forEach { pelanggar += "${f.name}: \"$it\"" }
        }
        assertTrue("prosa penunjuk dieja ulang di berkas yang tidak dijaga: $pelanggar",
            pelanggar.isEmpty())
    }

    /**
     * Setiap prosa pada daftar bersama benar-benar menolak bundel.
     *
     * Daftar yang tidak terpakai sama saja dengan tidak ada: prosa yang ditambahkan ke
     * [BundelKonten.PLACEHOLDER] tetapi tidak pernah dijalankan akan membuat gate Python (yang
     * membaca daftar yang sama) menolak bundel yang justru diterima aplikasi.
     */
    @Test
    fun setiapProsaPenunjukBenarBenarDitolak() {
        assertTrue("daftar prosa penunjuk kosong", BundelKonten.PLACEHOLDER.isNotEmpty())
        BundelKonten.PLACEHOLDER.forEach { prosa ->
            val h = BundelKonten.baca(bundelDenganTeks("teks $prosa teks"))
            assertFalse("prosa penunjuk '$prosa' tidak ditolak", h.sah)
            assertTrue(h.ditolak.toString(), h.ditolak.any { "prosa penunjuk" in it })
        }
    }

    private fun bundelDenganTeks(teks: String): String = """
        {
          "_bundel": { "skema": 1 },
          "berkas": {
            "dzikir.json": {
              "sumber_url": "https://sunnah.com/muslim:591",
              "kategori": "dzikir",
              "arab": "$teks"
            }
          }
        }
    """.trimIndent()

    // --- Gate membaca aturan yang sama ----------------------------------------------------------

    @Test
    fun gerbangKontenMembacaProsaPenunjukDariBundelKonten() {
        assertTrue("gate konten tidak membaca daftar prosa dari BundelKonten.kt",
            gerbangKonten.contains("BundelKonten.kt"))
        assertTrue("gate konten memakai pola pengambil daftar yang berbeda dari penjaganya",
            gerbangKonten.contains(polaDaftar))
        assertTrue("gate konten memakai pola pengambil teks yang berbeda dari penjaganya",
            gerbangKonten.contains(polaTeks))
        // Salinan daftar di gate akan membuat dua kebijakan yang dapat berbeda tanpa galat.
        val salinan = BundelKonten.PLACEHOLDER.filter { gerbangKonten.contains("\"$it\"") }
        assertTrue("gate konten menyimpan salinan prosa penunjuk: $salinan", salinan.isEmpty())
    }

    /**
     * Gate konten membaca **field yang dinilai** dari [BundelKonten], bukan menyimpannya.
     *
     * `check_placeholders` dahulu menyimpan `("arab", "latin")` tersendiri. Bila daftar bersama
     * bertambah field, gate yang menyimpan salinan akan terus memeriksa yang lama — dan prosanya
     * lolos ke layar tanpa satu pun galat.
     */
    @Test
    fun gerbangKontenMembacaFieldTeksDariBundelKonten() {
        assertTrue("gate konten tidak membaca daftar field teks dari BundelKonten.kt",
            gerbangKonten.contains("""baca_daftar_teks(SUMBER_BUNDEL, "FIELD_TEKS")"""))
        assertFalse("gate konten mengeja ulang daftar field teks",
            gerbangKonten.contains("if k in (\"arab\", \"latin\")"))
    }

    /**
     * Gate bundel membaca aturan nama berkas dari [BundelKonten], bukan menyimpannya.
     *
     * Aturan inilah yang paling sunyi penyimpangannya: `tools/fixtures/bundel/cacat.json` ditolak
     * karena isinya, bukan karena nama berkasnya, sehingga salinan yang salah tetap hijau di CI.
     */
    @Test
    fun gerbangBundelMembacaAturanNamaBerkasDariBundelKonten() {
        listOf(
            "vc.PANJANG_NAMA_MIN", "vc.PANJANG_NAMA_MAKS", "vc.AKHIRAN_NAMA",
            "vc.AWALAN_NAMA_DILARANG", "vc.PEMISAH_NAMA_DILARANG",
        ).forEach { assertTrue("gate bundel tidak membaca $it", gerbangBundel.contains(it)) }
        assertTrue("gate bundel tidak membaca versi skema dari BundelKonten",
            gerbangBundel.contains("""baca_angka(vc.SUMBER_BUNDEL, "SKEMA")"""))
        // Nilai yang dahulu disalin ke `nama_berkas_sah` tidak boleh kembali.
        listOf("6 <=", "<= 64", "endswith(\".json\")").forEach {
            assertFalse("gate bundel mengeja ulang '$it'", gerbangBundel.contains(it))
        }
    }

    /** Pola pengambil benar-benar menghasilkan nilai aplikasi — bukan sekadar cocok. */
    @Test
    fun polaPengambilMenghasilkanAturanAplikasi() {
        val sumber = KodeSumber.sumber(berkasBundel)

        assertEquals("prosa penunjuk yang dibaca gate berbeda dari yang dipakai aplikasi",
            BundelKonten.PLACEHOLDER, daftar(sumber, "PLACEHOLDER"))
        assertEquals("field teks yang dibaca gate berbeda dari yang dipakai aplikasi",
            BundelKonten.FIELD_TEKS, daftar(sumber, "FIELD_TEKS"))
        assertEquals("awalan terlarang yang dibaca gate berbeda dari yang dipakai aplikasi",
            BundelKonten.AWALAN_NAMA_DILARANG, daftar(sumber, "AWALAN_NAMA_DILARANG"))
        assertEquals("pemisah terlarang yang dibaca gate berbeda dari yang dipakai aplikasi",
            BundelKonten.PEMISAH_NAMA_DILARANG, daftar(sumber, "PEMISAH_NAMA_DILARANG"))
        assertEquals(BundelKonten.PANJANG_NAMA_MIN, angka(sumber, "PANJANG_NAMA_MIN"))
        assertEquals(BundelKonten.PANJANG_NAMA_MAKS, angka(sumber, "PANJANG_NAMA_MAKS"))
        assertEquals(BundelKonten.AKHIRAN_NAMA, teks(sumber, "AKHIRAN_NAMA"))
        assertEquals(BundelKonten.SKEMA, angka(sumber, "SKEMA"))

        // Daftar kosong akan membuat gate meloloskan segalanya; rentang terbalik akan membuat
        // seluruh nama berkas ditolak.
        assertTrue("daftar prosa penunjuk kosong", BundelKonten.PLACEHOLDER.isNotEmpty())
        assertTrue("daftar field teks kosong", BundelKonten.FIELD_TEKS.isNotEmpty())
        assertTrue("awalan terlarang kosong", BundelKonten.AWALAN_NAMA_DILARANG.isNotEmpty())
        assertTrue("pemisah terlarang kosong", BundelKonten.PEMISAH_NAMA_DILARANG.isNotEmpty())
        assertTrue("rentang panjang nama terbalik",
            BundelKonten.PANJANG_NAMA_MIN < BundelKonten.PANJANG_NAMA_MAKS)
        assertTrue("akhiran nama harus berawalan titik", BundelKonten.AKHIRAN_NAMA.startsWith("."))
        // Pemisah jalur wajib memuat garis miring terbalik — satu-satunya nilai yang memerlukan
        // escape di sumber Kotlin, dan justru itu yang paling mudah terbaca salah oleh gate.
        assertTrue("pemisah terlarang kehilangan garis miring terbalik",
            BundelKonten.PEMISAH_NAMA_DILARANG.any { it == "\\" })
    }

    /** Aturan nama berkas dipakai lewat konstanta bersama, bukan dieja di dalam badan fungsinya. */
    @Test
    fun namaBerkasSahMemakaiKonstantaBersama() {
        val kode = KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(berkasBundel))
        assertTrue("batas panjang tidak dibaca dari konstanta bersama",
            kode.contains("PANJANG_NAMA_MIN..PANJANG_NAMA_MAKS"))
        assertTrue("akhiran tidak dibaca dari konstanta bersama",
            kode.contains("nama.endsWith(AKHIRAN_NAMA)"))
        assertTrue("awalan terlarang tidak dibaca dari konstanta bersama",
            kode.contains("AWALAN_NAMA_DILARANG.none"))
        assertTrue("pemisah terlarang tidak dibaca dari konstanta bersama",
            kode.contains("PEMISAH_NAMA_DILARANG.none"))
        assertFalse("batas panjang dieja ulang di dalam badan fungsi",
            kode.contains("in 6..64"))
    }

    /** Field teks dipakai lewat konstanta bersama, bukan dieja di dalam badan fungsinya. */
    @Test
    fun fieldTeksMemakaiKonstantaBersama() {
        val kode = KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(berkasBundel))
        assertTrue("field teks tidak dibaca dari konstanta bersama", kode.contains("k in FIELD_TEKS"))
        assertFalse("field teks dieja ulang di dalam badan fungsi",
            kode.contains("k == \"arab\" || k == \"latin\""))
    }

    // --- Dokumen yang mengulang aturan ini ------------------------------------------------------

    /**
     * Sel aturan pada baris tabel `docs/CONTENT_UPDATE.md` yang memuat [penanda].
     *
     * Tabel itu punya dua kolom (aturan, alasan); hanya sel pertama yang memuat aturannya, sebab
     * kolom alasan pun memakai penekanan berkurung kutip (`plan.txt`) dan bukan nama field atau
     * prosa. Barisnya karena itu dipecah lebih dulu, bukan dipindai seluruhnya.
     */
    private fun selAturanDokumen(penanda: String): String {
        val berkas = File(repoRoot(), "docs/CONTENT_UPDATE.md")
        val baris = berkas.readLines().singleOrNull { it.startsWith("|") && it.contains(penanda) }
            ?: error("baris aturan '$penanda' tidak ditemukan di docs/CONTENT_UPDATE.md")
        val sel = baris.split("|")
        assertTrue("baris aturan '$penanda' kehilangan kolomnya", sel.size >= 3)
        return sel[1].trim()
    }

    /**
     * Prosa penunjuk yang disebut dokumen harus benar-benar ditolak aplikasi.
     *
     * Baris aturan itu menyingkat daftarnya dengan *"…"*, jadi yang dituntut bukan kesamaan:
     * **tiap prosa yang disebut dokumen wajib ada di [BundelKonten.PLACEHOLDER]**. Bila sebuah
     * prosa berganti nama atau dihapus dari daftar bersama, dokumen akan menjanjikan penjagaan yang
     * tak lagi ada — pembaca menyimpulkan bundelnya aman padahal prosanya lolos ke layar. Sebaliknya,
     * begitu dokumen berhenti menyingkat (tanpa *"…"*), daftarnya justru harus lengkap.
     */
    @Test
    fun prosaPenunjukDiDokumenMasihDitolakAplikasi() {
        val sel = selAturanDokumen("prosa penunjuk")
        assertTrue("baris aturan prosa penunjuk tidak lagi memuat daftar berkurung",
            sel.contains("(") && sel.contains(")"))
        val didalam = sel.substringAfter("(").substringBefore(")")
        val disebut = Regex("`([^`]+)`").findAll(didalam).map { it.groupValues[1] }.toList()
        assertTrue("dokumen tidak lagi menyebut satu pun prosa penunjuk", disebut.isNotEmpty())

        val asing = disebut.filterNot { it in BundelKonten.PLACEHOLDER }
        assertTrue(
            "prosa di docs/CONTENT_UPDATE.md bukan prosa penunjuk yang ditolak aplikasi — dokumen " +
                "menjanjikan penjagaan yang tidak ada: $asing",
            asing.isEmpty(),
        )
        if ("…" !in didalam) {
            assertEquals(
                "dokumen berhenti menyingkat daftar prosa penunjuk, jadi daftarnya harus lengkap",
                BundelKonten.PLACEHOLDER, disebut,
            )
        }
    }

    /**
     * Field yang disebut dokumen harus persis yang diperiksa aplikasi.
     *
     * Baris itu adalah satu-satunya keterangan yang dibaca orang untuk tahu **seberapa luas**
     * pemeriksaan prosanya. Field yang ditambahkan di kode tanpa ikut disunting di sini membuat
     * dokumen mengecilkan cakupannya; field yang disebut dokumen tetapi tidak diperiksa membuatnya
     * melebihkannya.
     */
    @Test
    fun fieldProsaPenunjukDiDokumenSelarasDenganYangDiperiksa() {
        val sel = selAturanDokumen("prosa penunjuk")
        assertTrue("baris aturan prosa penunjuk tidak lagi memuat daftar berkurung", sel.contains("("))
        val disebut = Regex("`([a-z]+)`").findAll(sel.substringBefore("("))
            .map { it.groupValues[1] }.toList()
        assertEquals(
            "field prosa penunjuk di docs/CONTENT_UPDATE.md berbeda dari yang diperiksa aplikasi",
            BundelKonten.FIELD_TEKS, disebut,
        )
    }
}
