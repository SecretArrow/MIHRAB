package id.or.kemenag.yasintahlil

import id.or.kemenag.yasintahlil.navigation.RuteKonten
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Pencarian, Favorit, dan Riwayat": *"Sediakan favorit, riwayat terakhir dibaca,
 * bookmark, dan pencarian terbaru."*
 *
 * "Riwayat terakhir dibaca" tidak berjalan: satu-satunya penulis riwayat adalah tombol **"Salin"**
 * pada kartu bacaan (dan riwayat pencarian), sedangkan modul **Yasin** dan **Tahlil** — dua layar
 * baca utama — tidak pernah menulisnya sama sekali. Akibatnya daftar "Riwayat terbaru" memuat
 * bacaan yang *disalin*, bukan bacaan yang *dibaca*, dan membaca Yasin dari awal sampai akhir tidak
 * meninggalkan jejak apa pun.
 *
 * Uji ini menjaga tiga hal sekaligus:
 *
 * 1. kedua modul itu benar-benar mencatat posisi bacaannya;
 * 2. pencatatannya tidak membanjiri riwayat — menghitung tasbih juga menyimpan progres, dan
 *    mencatat tiap ketukan penghitung akan mengubur bacaan lain;
 * 3. satu bacaan hanya menempati satu baris, karena pengulangan kini jauh lebih mungkin terjadi.
 */
class RiwayatBacaanTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private val yasinVm = "src/main/java/id/or/kemenag/yasintahlil/feature/yasin/YasinViewModel.kt"
    private val tahlilVm = "src/main/java/id/or/kemenag/yasintahlil/feature/tahlil/TahlilViewModel.kt"
    private val favoritVm = "src/main/java/id/or/kemenag/yasintahlil/feature/favorit/FavoritViewModel.kt"
    private val favoritLayar = "src/main/java/id/or/kemenag/yasintahlil/feature/favorit/FavoritScreen.kt"

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

    @Test
    fun yasinMencatatAyatYangDibaca() {
        val b = badan(sumber(yasinVm), "markRead")
        assertTrue("markRead tidak ditemukan di YasinViewModel", b.isNotBlank())
        assertTrue("Yasin tidak menulis riwayat saat menandai bacaan",
            b.contains("pushHistory(JenisBacaan.YASIN"))
        assertTrue("riwayat Yasin tidak menyebut nomor ayatnya", b.contains("n.toString()"))
    }

    @Test
    fun tahlilMencatatBagianYangDibaca() {
        val s = sumber(tahlilVm)
        assertTrue("Tahlil tidak punya pencatat riwayat", s.contains("private fun catat()"))
        listOf("next", "prev", "jumpTo").forEach { f ->
            val b = badan(s, f)
            assertTrue("$f tidak ditemukan di TahlilViewModel", b.isNotBlank())
            assertTrue("$f tidak mencatat riwayat", b.contains("catat()"))
        }
        assertTrue("pencatat riwayat Tahlil tidak menulis apa pun",
            badan(s, "catat").contains("pushHistory("))
    }

    /**
     * Menghitung tasbih juga memanggil `persist()`. Bila pencatatan riwayat ditempelkan di sana,
     * setiap ketukan penghitung menambah satu baris riwayat dan daftar "Riwayat terbaru" penuh
     * dengan satu bagian yang sama.
     */
    @Test
    fun penghitungTidakMembanjiriRiwayat() {
        val s = sumber(tahlilVm)
        val hitung = badan(s, "setCount")
        assertTrue("setCount tidak ditemukan", hitung.isNotBlank())
        assertTrue("setCount ikut mencatat riwayat", !hitung.contains("catat()"))
        assertTrue("persist() sendiri menulis riwayat — semua pemanggilnya ikut terbebani",
            !badan(s, "persist").contains("pushHistory("))
    }

    @Test
    fun satuBacaanHanyaSatuBarisRiwayat() {
        val s = sumber(favoritVm)
        assertTrue("riwayat tidak disaring sehingga bacaan yang sama berulang",
            s.contains("distinctBy"))
        assertTrue("penyaringan riwayat tidak memakai jenis + id bacaan",
            s.contains("i.kind to i.refId"))
        // Penyaringan harus setelah pengambilan yang lebih longgar, bukan sesudah jatah tampil:
        // bila semua baris terbaru adalah bacaan yang sama, penyaringan setelah jatah menyisakan
        // satu baris dan menyembunyikan bacaan lain yang sebenarnya ada. Kedua angka itu kini
        // tinggal di `BatasRiwayat`, dan hubungannya dipaku di `BatasRiwayatSatuTempatTest`.
        assertTrue("riwayat tidak diambil dengan batas baca bersama (BatasRiwayat.DIBACA_DAFTAR)",
            s.contains("recentHistory(BatasRiwayat.DIBACA_DAFTAR)"))
        assertTrue("jatah tampil riwayat tidak memakai batas bersama (BatasRiwayat.DITAMPILKAN)",
            s.contains("take(BatasRiwayat.DITAMPILKAN)"))
    }

    @Test
    fun barisRiwayatBerkunciUnik() {
        val s = sumber(favoritLayar)
        assertTrue("baris riwayat tidak diberi kunci unik",
            s.contains("key = { \"h:\${it.kind}:\${it.refId}\" }"))
    }

    /** Riwayat hanya berguna bila tombol "Buka"-nya membawa ke layar yang benar. */
    @Test
    fun jenisRiwayatYangDicatatPunyaLayarTujuan() {
        listOf("yasin", "tahlil").forEach { kind ->
            val rute = RuteKonten.rute(kind)
            assertTrue("riwayat '$kind' tidak punya layar tujuan", rute != null)
            assertTrue("riwayat '$kind' tidak punya nama tujuan yang terbaca",
                !RuteKonten.namaTujuan(kind).isNullOrBlank())
        }
        // "search" sengaja tetap punya tujuan sendiri (layar Pencarian), bukan null.
        assertEquals("search", RuteKonten.rute("search"))
    }
}
