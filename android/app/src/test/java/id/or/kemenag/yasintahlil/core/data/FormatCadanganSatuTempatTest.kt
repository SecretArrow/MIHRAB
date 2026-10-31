package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Bentuk **berkas cadangan** — nama kunci di dalam JSON-nya — harus satu tempat, dan **pemulihan
 * tidak boleh menghapus bagian yang tak ada di berkas**.
 *
 * Bentuk ini dahulu dieja dua kali oleh dua sisi yang tak pernah bertemu: penulisnya
 * (`SettingsScreen` dan `RoomUserDataRepository.exportJson`) dan pembacanya (`restoreJson` dan layar
 * Pengaturan lagi). Enam belas nama, dua salinan, tak satu pun diperiksa — sementara `Store` punya
 * penjaga sepadan untuk sisi preferensinya (`StoreBackupLengkapTest`) dan sisi data terstruktur ini
 * tak punya apa-apa.
 *
 * Bedanya sunyi dan akibatnya lebih besar daripada kehilangan satu pengaturan. `restoreJson` dahulu
 * mengosongkan keempat tabel **lebih dahulu**, baru mengisi ulang dari berkas. Satu nama kunci yang
 * meleset karena itu berakhir dengan seluruh favorit, bookmark, riwayat, dan progres terhapus, nol
 * baris dimasukkan, dan tak satu pun pesan galat — pemulihan tampak berhasil. Karena itu uji ini
 * menjaga dua hal sekaligus: bentuknya tinggal di [FormatCadangan], dan pemulihan membaca lariknya
 * sebelum mengosongkan tabelnya.
 *
 * Dibaca dari sumber, bukan dijalankan: `RoomUserDataRepository` butuh Room dan `SettingsScreen`
 * butuh Compose, sedangkan yang diperiksa di sini murni soal dua salinan yang harus sepadan.
 */
class FormatCadanganSatuTempatTest {

    private fun sumber(relatif: String): String =
        KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif")

    private val repo = "core/data/local/RoomUserDataRepository.kt"
    private val layar = "feature/settings/SettingsScreen.kt"
    private val entities = "core/data/local/Entities.kt"

    /**
     * Seluruh nama kunci bentuk berkas, diambil dari [FormatCadangan] — bukan diketik ulang di sini.
     * Mengganti nama konstantanya membuat uji ini gagal dikompilasi; mengubah **nilainya** ditangkap
     * [bentukBerkasDibekukan].
     */
    private val namaKunci = listOf(
        FormatCadangan.KUNCI_VERSI, FormatCadangan.KUNCI_PREFERENSI, FormatCadangan.KUNCI_DATA_PENGGUNA,
        FormatCadangan.FAVORIT, FormatCadangan.BOOKMARK, FormatCadangan.PROGRES, FormatCadangan.RIWAYAT,
        FormatCadangan.KIND, FormatCadangan.REF_ID, FormatCadangan.JUDUL, FormatCadangan.LABEL,
        FormatCadangan.NILAI, FormatCadangan.DIBUAT, FormatCadangan.DIPERBARUI, FormatCadangan.DIBUKA,
        FormatCadangan.ID,
    )

    /**
     * Nama kunci yang **khas** milik berkas cadangan: tak ada bentuk lain di aplikasi ini yang
     * memakainya, sehingga kemunculannya sebagai literal di luar rumahnya pasti pelanggaran.
     *
     * Nama medan seperti `"id"`, `"title"`, atau `"label"` tidak ikut dipindai ke seluruh aplikasi:
     * `"id"` misalnya juga kunci bentuk lain (konten, rute navigasi), jadi mencarinya di mana-mana
     * hanya menghasilkan salah tuduh. Yang khas tetap diperiksa di kedua berkas pemilik bentuk ini.
     */
    private val namaKhas = listOf(
        FormatCadangan.KUNCI_VERSI, FormatCadangan.KUNCI_PREFERENSI, FormatCadangan.KUNCI_DATA_PENGGUNA,
        FormatCadangan.FAVORIT, FormatCadangan.BOOKMARK, FormatCadangan.PROGRES, FormatCadangan.RIWAYAT,
    )

    /** Badan sebuah fungsi (di dalam `{ … }`), dicari dengan kurung kurawal yang seimbang. */
    private fun badan(src: String, nama: String): String {
        val mulai = src.indexOf("fun $nama(")
        if (mulai < 0) return ""
        var i = mulai + "fun $nama".length
        var depth = 0
        while (i < src.length) {
            when (src[i]) {
                '(' -> depth++
                ')' -> { depth--; if (depth == 0) { i++; break } }
            }
            i++
        }
        while (i < src.length && src[i] != '{') i++
        if (i >= src.length) return ""
        val dari = i
        depth = 0
        while (i < src.length) {
            when (src[i]) {
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) { i++; break } }
            }
            i++
        }
        return src.substring(dari, minOf(i, src.length))
    }

    /** Kemunculan tiap `FormatCadangan.<NAMA>` di dalam [teks], beserta jumlahnya. */
    private fun token(teks: String): Map<String, Int> =
        Regex("""FormatCadangan\.([A-Z][A-Z0-9_]*)""").findAll(teks)
            .groupingBy { it.groupValues[1] }.eachCount()

    // ---- Bentuknya sendiri ----------------------------------------------------------------------

    /**
     * Nilai tiap kunci **dibekukan**: kunci-kunci ini sudah tertulis di berkas cadangan milik
     * pengguna, jadi menggantinya membuat berkas lama tak terbaca — dan itu terjadi tanpa galat,
     * sebagai data yang kembali kosong.
     */
    @Test
    fun bentukBerkasDibekukan() {
        assertEquals("versi berkas berubah — berkas lama tak lagi dikenali", 1, FormatCadangan.VERSI)
        assertEquals("version", FormatCadangan.KUNCI_VERSI)
        assertEquals("prefs", FormatCadangan.KUNCI_PREFERENSI)
        assertEquals("userData", FormatCadangan.KUNCI_DATA_PENGGUNA)
        assertEquals("favorites", FormatCadangan.FAVORIT)
        assertEquals("bookmarks", FormatCadangan.BOOKMARK)
        assertEquals("progress", FormatCadangan.PROGRES)
        assertEquals("history", FormatCadangan.RIWAYAT)
        assertEquals("kind", FormatCadangan.KIND)
        assertEquals("refId", FormatCadangan.REF_ID)
        assertEquals("title", FormatCadangan.JUDUL)
        assertEquals("label", FormatCadangan.LABEL)
        assertEquals("value", FormatCadangan.NILAI)
        assertEquals("createdAt", FormatCadangan.DIBUAT)
        assertEquals("updatedAt", FormatCadangan.DIPERBARUI)
        assertEquals("openedAt", FormatCadangan.DIBUKA)
        assertEquals("id", FormatCadangan.ID)
        assertEquals("ada kunci yang bernilai sama — salah satunya akan salah dibaca",
            namaKunci.size, namaKunci.toSet().size)
    }

    // ---- Penulis dan pembaca memakai kunci yang sama --------------------------------------------

    /**
     * Jumlah kemunculan tiap kunci di penulis harus sama dengan di pembaca.
     *
     * Dibandingkan **beserta jumlahnya**, bukan sekadar himpunannya: menukar `createdAt` dengan
     * `openedAt` di satu pembaca saja tetap menyisakan himpunan yang sama, tetapi membuat favorit
     * dan bookmark kehilangan waktu dibuatnya sementara riwayat memakai waktu yang salah.
     */
    @Test
    fun penulisDanPembacaMemakaiKunciYangSama() {
        val tulis = token(badan(KodeSumber.kodeTanpaKomentar(sumber(repo)), "exportJson"))
        val baca = token(badan(KodeSumber.kodeTanpaKomentar(sumber(repo)), "restoreJson"))
        assertTrue("exportJson tidak memakai FormatCadangan sama sekali — penandanya salah",
            tulis.size >= 10)
        assertTrue("restoreJson tidak memakai FormatCadangan sama sekali — penandanya salah",
            baca.size >= 10)
        assertEquals("penulis dan pembaca berkas cadangan memakai kunci yang berbeda — " +
            "data pengguna akan hilang saat dipulihkan, tanpa pesan galat", tulis, baca)
    }

    // ---- Pemulihan tidak menghapus apa yang tak ada di berkas -----------------------------------

    /**
     * Keempat larik dibaca **sebelum** tabelnya dikosongkan.
     *
     * Inilah yang membuat satu kunci yang meleset berhenti menjadi bencana: selama lariknya dibaca
     * lebih dahulu dan pengosongan hanya terjadi bila lariknya ada, berkas yang tak menyebut sebuah
     * bagian meninggalkan bagian itu apa adanya. Membalik urutannya mengembalikan kehilangan data
     * yang sunyi itu.
     */
    @Test
    fun pemulihanMembacaLarikSebelumMengosongkanTabel() {
        val baca = badan(KodeSumber.kodeTanpaKomentar(sumber(repo)), "restoreJson")
        assertTrue("restoreJson tidak terbaca", baca.contains("toEntities"))
        // Pasangan "tabel mana dikosongkan dari larik mana", dibaca dari sumbernya sendiri:
        // `val favArr = root.optJSONArray(FormatCadangan.FAVORIT)`. Nama konstantanya diambil dari
        // sana, bukan diketik ulang di sini, supaya penggantian nama tak membuat uji ini mencari
        // teks yang tak pernah ada lalu lolos tanpa memeriksa apa pun.
        val pasangan = Regex("""val (\w+)Arr = root\.optJSONArray\(FormatCadangan\.(\w+)\)""")
            .findAll(baca).map { it.groupValues[1] to it.groupValues[2] }.toList()
        assertEquals("tidak semua bagian dibaca dari lariknya sendiri: $pasangan", 4, pasangan.size)
        pasangan.forEach { (dao, kunci) ->
            val dibaca = baca.indexOf("root.optJSONArray(FormatCadangan.$kunci)")
            val dikosongkan = baca.indexOf("$dao.clear()")
            assertTrue("larik $kunci tidak dibaca dari berkas", dibaca >= 0)
            assertTrue("restoreJson tidak mengosongkan tabel $dao", dikosongkan > 0)
            assertTrue("tabel $dao dikosongkan sebelum larik $kunci dibaca — berkas yang tak " +
                "memuat bagian itu akan menghapusnya", dibaca < dikosongkan)
            // Pengosongan harus **bergantung** pada keberadaan lariknya, bukan sekadar berdiri di
            // sebelahnya: `if (true)` atau `if (false)` sama-sama lolos pemeriksaan urutan di atas.
            val penjaga = Regex("""if \(${dao}Arr != null\) \{ $dao\.clear\(\)""")
            assertTrue("pengosongan tabel $dao tidak dijaga keberadaan larik $kunci di berkas",
                penjaga.containsMatchIn(baca))
        }
    }

    // ---- Tak ada yang mengejanya kembali --------------------------------------------------------

    /** Kedua berkas yang dahulu mengeja bentuk ini sendiri kini harus lewat [FormatCadangan]. */
    @Test
    fun bentukBerkasTidakDiejaDiTempatPemakaian() {
        listOf(repo, layar).forEach { berkas ->
            val literals = KodeSumber.literalString(sumber(berkas))
            val pelanggar = namaKunci.filter { it in literals }
            assertEquals("$berkas masih mengeja nama kunci berkas cadangan sendiri",
                emptyList<String>(), pelanggar)
        }
    }

    /**
     * Nama yang khas tak muncul sebagai literal di berkas lain mana pun.
     *
     * `Entities.kt` dikecualikan karena nama tabel Room-nya kebetulan sama (`tableName = "favorites"`).
     * Keduanya sengaja tidak disatukan: nama tabel adalah urusan skema penyimpanan, nama kunci adalah
     * urusan berkas yang keluar dari aplikasi. Menyatukannya berarti satu perubahan skema diam-diam
     * mengubah bentuk berkas cadangan.
     */
    @Test
    fun namaKhasHanyaDiejaDiRumahnya() {
        val akar = File(KodeSumber.root(), "src/main/java/id/or/kemenag/yasintahlil")
        val pelanggar = akar.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.name != "FormatCadangan.kt" && it.name != "Entities.kt" }
            .flatMap { f ->
                val literals = KodeSumber.literalString(f.readText())
                namaKhas.filter { it in literals }.map { "${f.name}: $it" }
            }
            .toList()
        assertEquals("nama kunci berkas cadangan dieja di luar rumahnya",
            emptyList<String>(), pelanggar)
    }

    /** Layar Pengaturan memakai kunci bersama untuk kedua sisinya, dan menulis versinya. */
    @Test
    fun layarPengaturanMemakaiKunciBersama() {
        val kode = KodeSumber.kodeTanpaKomentar(sumber(layar))
        assertTrue("Pengaturan tidak menulis versi berkas dari FormatCadangan",
            kode.contains("FormatCadangan.KUNCI_VERSI"))
        assertTrue("Pengaturan tidak memakai FormatCadangan untuk kunci preferensi",
            kode.contains("FormatCadangan.KUNCI_PREFERENSI"))
        assertTrue("Pengaturan tidak memakai FormatCadangan untuk kunci data pengguna",
            kode.contains("FormatCadangan.KUNCI_DATA_PENGGUNA"))
        // Pembaca hanya memulihkan data pengguna bila berkasnya memang memuatnya; tanpa penjaga
        // itu, berkas yang memuat preferensi saja akan mengosongkan seluruh data terstruktur.
        assertTrue("Pengaturan memulihkan data pengguna tanpa memeriksa keberadaannya di berkas",
            kode.contains("optJSONObject(FormatCadangan.KUNCI_DATA_PENGGUNA)"))
    }
}
