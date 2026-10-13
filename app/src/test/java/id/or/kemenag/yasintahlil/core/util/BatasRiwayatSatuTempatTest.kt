package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batas **daftar riwayat terbaru** — berapa entri yang dibaca dan berapa yang ditampilkan — harus
 * satu tempat, dan **jumlah** riwayat tidak boleh disimpulkan dari daftar yang sudah dipotong.
 *
 * Dua hal berbeda pernah tertukar di sini:
 *
 * - Angka batasnya dieja di tiap tempat pemakaian: `recentHistory(50)` di beranda dan di Pencarian,
 *   `recentHistory(60)` di Favorit, lalu jatah tampilnya dipotong `20` di ViewModel Favorit dan `12`
 *   lagi di layarnya. Hubungan "baca longgar → saring → potong" tak terlihat dari mana pun, dan
 *   `recentHistory` sendiri mendeklarasikan bawaan `= 50` yang **tak satu pun** pemanggil pakai.
 * - Beranda menjawab **"Riwayat: N"** dengan `recentHistory(50).size` — ukuran daftar yang sudah
 *   dipotong — sehingga pengguna dengan lebih dari 50 riwayat diberi tahu angka yang salah, sementara
 *   angka yang sama di layar "Manajemen data offline" dibaca dari `ringkasan()` (`COUNT(*)`) dan
 *   menyebut angka yang sebenarnya. Satu fakta, dua jawaban, tanpa satu pun pesan galat.
 *
 * Angka-angkanya kini tinggal di [BatasRiwayat], jumlahnya dihitung
 * [id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository.jumlahRiwayat], dan uji ini
 * menjaga keduanya: hubungan antarangka batas, dan tak ada layar yang mengejanya kembali.
 */
class BatasRiwayatSatuTempatTest {

    private fun layar(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(
            KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    private val homeVm = "feature/home/HomeViewModel.kt"
    private val favoritVm = "feature/favorit/FavoritViewModel.kt"
    private val favoritLayar = "feature/favorit/FavoritScreen.kt"
    private val search = "feature/search/SearchScreen.kt"
    private val repo = "core/domain/repository/UserDataRepository.kt"
    private val dao = "core/data/local/Daos.kt"

    // ---- Angka-angkanya sendiri -----------------------------------------------------------------

    /**
     * Batas baca **harus** lebih longgar daripada jatah tampil. Membalikkannya tidak berbunyi apa
     * pun: penyaringan "satu bacaan satu baris" berjalan sesudah pembacaan, jadi daftar hanya
     * menyisakan lebih sedikit baris daripada jatahnya dan bacaan lain tak pernah muncul.
     */
    @Test
    fun batasBacaLebihLonggarDariJatahTampil() {
        assertTrue("batas baca daftar (${BatasRiwayat.DIBACA_DAFTAR}) tidak lebih longgar daripada " +
            "jatah tampil (${BatasRiwayat.DITAMPILKAN}) — penyaringan akan menyisakan lebih sedikit " +
            "baris daripada jatahnya",
            BatasRiwayat.DIBACA_DAFTAR > BatasRiwayat.DITAMPILKAN)
        assertTrue("batas baca ringkas harus positif", BatasRiwayat.DIBACA_RINGKAS > 0)
        assertTrue("jatah tampil harus positif", BatasRiwayat.DITAMPILKAN > 0)
    }

    // ---- Jumlah riwayat bukan ukuran daftar yang dipotong ---------------------------------------

    @Test
    fun berandaMenghitungRiwayatDariJumlahBukanDariDaftarTerpotong() {
        val s = layar(homeVm)
        assertTrue("beranda tidak memakai jumlahRiwayat() — \"Riwayat: N\" kembali disimpulkan dari " +
            "daftar yang sudah dipotong jatah bacanya", s.contains("jumlahRiwayat()"))
        assertFalse("beranda masih membaca daftar riwayat terbaru untuk menghitung jumlahnya, " +
            "padahal jumlahnya ada sendiri (jumlahRiwayat) dan tak terbatas jatah baca",
            s.contains("recentHistory("))
    }

    @Test
    fun repositoriMenyediakanJumlahRiwayatSebagaiAliran() {
        assertTrue("UserDataRepository tidak menyediakan jumlahRiwayat(): Flow<Int>",
            layar(repo).contains("fun jumlahRiwayat(): Flow<Int>"))
        assertTrue("HistoryDao tidak menghitung riwayat sebagai aliran (observeCount)",
            layar(dao).contains("fun observeCount(): Flow<Int>"))
    }

    /**
     * `recentHistory` **tidak** boleh punya batas bawaan. Bawaan `= 50` yang dahulu tertulis di
     * sana hanya tampak seperti kebijakan padahal setiap pemanggil menulis batasnya sendiri
     * (termasuk `60`), sehingga layar yang berbeda diam-diam membaca jatah yang berbeda.
     */
    @Test
    fun recentHistoryTidakPunyaBatasBawaan() {
        val s = layar(repo)
        assertTrue("recentHistory tidak lagi menerima batas dari pemanggilnya",
            s.contains("fun recentHistory(limit: Int): Flow<List<HistItem>>"))
        assertFalse("recentHistory kembali punya batas bawaan — kebijakan yang tampak berlaku " +
            "padahal tak satu pun pemanggil memakainya",
            s.contains("fun recentHistory(limit: Int ="))
    }

    // ---- Pemakaiannya satu ----------------------------------------------------------------------

    /** Tiap pemanggil menyerahkan batasnya ke [BatasRiwayat], bukan mengetik angkanya sendiri. */
    @Test
    fun setiapPemanggilMemakaiBatasBersama() {
        assertTrue("FavoritViewModel tidak membaca riwayat dengan BatasRiwayat.DIBACA_DAFTAR",
            layar(favoritVm).contains("recentHistory(BatasRiwayat.DIBACA_DAFTAR)"))
        assertTrue("FavoritViewModel tidak memotong daftar ke BatasRiwayat.DITAMPILKAN",
            layar(favoritVm).contains("take(BatasRiwayat.DITAMPILKAN)"))
        assertTrue("SearchScreen tidak membaca riwayat dengan BatasRiwayat.DIBACA_RINGKAS",
            layar(search).contains("recentHistory(BatasRiwayat.DIBACA_RINGKAS)"))
    }

    /**
     * Tak ada lagi angka batas riwayat yang dieja di tempat pemakaian.
     *
     * `MigrasiWarisan` sengaja tidak ikut diperiksa: `recentHistory(1)` di sana bukan batas daftar
     * melainkan pertanyaan "apakah sudah ada riwayat sama sekali?" sebelum data lama dipindahkan.
     */
    @Test
    fun batasRiwayatTidakDiejaDiTempatPemakaian() {
        val pola = Regex("""recentHistory\(\s*\d""")
        val offenders = listOf(homeVm, favoritVm, favoritLayar, search)
            .filter { pola.containsMatchIn(layar(it)) }
        assertEquals(
            "batas riwayat dieja sebagai angka di tempat pemakaian alih-alih memakai BatasRiwayat",
            emptyList<String>(), offenders,
        )
    }

    /**
     * **Jatah tampil** pun tidak boleh dieja di tempat pemakaian.
     *
     * Pemeriksaan sebelumnya hanya menangkap `recentHistory(<angka>)` — batas **baca** — sehingga
     * jatah tampil yang ditulis sendiri lolos. Sudah terjadi: Pencarian memotong daftar "pencarian
     * terbaru" dengan `take(10)` sementara [BatasRiwayat.DITAMPILKAN] menyatakan `12`, jadi layar
     * itu menampilkan jumlah baris yang berbeda dari daftar riwayat lain tanpa satu pun pesan galat.
     * Layar-layar ini adalah seluruh tempat daftar riwayat dipotong untuk ditampilkan.
     */
    @Test
    fun jatahTampilRiwayatTidakDiejaDiTempatPemakaian() {
        val pola = Regex("""take\(\s*\d""")
        val offenders = listOf(homeVm, favoritVm, favoritLayar, search)
            .filter { pola.containsMatchIn(layar(it)) }
        assertEquals(
            "jatah tampil riwayat dieja sebagai angka di tempat pemakaian alih-alih memakai " +
                "BatasRiwayat.DITAMPILKAN",
            emptyList<String>(), offenders,
        )
        assertTrue("FavoritViewModel tidak memotong daftar ke BatasRiwayat.DITAMPILKAN",
            layar(favoritVm).contains("take(BatasRiwayat.DITAMPILKAN)"))
        assertTrue("Pencarian tidak memotong daftar \"pencarian terbaru\" ke " +
            "BatasRiwayat.DITAMPILKAN",
            layar(search).contains("take(BatasRiwayat.DITAMPILKAN)"))
    }
}
