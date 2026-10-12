package id.or.kemenag.yasintahlil.feature.favorit

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Pencarian": *"Sediakan favorit, riwayat terakhir dibaca, bookmark, dan pencarian
 * terbaru."* — dan §"Pengaturan Aplikasi" menghitung bookmark di antara data yang tersimpan.
 *
 * Bookmark memang sudah disimpan di Room, tetapi sebelumnya **tidak pernah terlihat** di satu pun
 * layar daftar: pengguna hanya melihat jumlahnya di Pengaturan, tanpa cara membukanya. Data yang
 * tersimpan tetapi tak dapat dijangkau sama saja dengan hilang bagi pengguna.
 */
class BookmarkTerlihatTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private val layar = "src/main/java/id/or/kemenag/yasintahlil/feature/favorit/FavoritScreen.kt"
    private val vm = "src/main/java/id/or/kemenag/yasintahlil/feature/favorit/FavoritViewModel.kt"

    @Test
    fun layarFavoritMenampilkanBagianBookmark() {
        val s = sumber(layar)
        assertTrue("layar Favorit tidak menampilkan bagian bookmark", s.contains("Bookmark ("))
        assertTrue("layar Favorit tidak membaca daftar bookmark dari state", s.contains("s.bookmarks"))
        assertTrue("tidak ada tombol hapus bookmark", s.contains("vm.removeBookmark("))
        assertTrue("keadaan kosong bookmark tidak dijelaskan",
            s.contains("Belum ada bookmark"))
    }

    @Test
    fun viewModelMengamatiSeluruhBookmarkLintasJenis() {
        val s = sumber(vm)
        assertTrue("viewModel tidak mengamati bookmark", s.contains("userData.allBookmarks()"))
        assertTrue("state tidak menyimpan bookmark", s.contains("val bookmarks: List<BookmarkItem>"))
        assertTrue("menghapus bookmark tidak diteruskan ke penyimpanan",
            s.contains("userData.toggleBookmark(item.kind, item.refId, item.label)"))
    }

    /** Bookmark ditulis di layar bacaan, jadi jenisnya beragam — bukan hanya "yasin". */
    @Test
    fun repositoryMenyediakanBookmarkLintasJenis() {
        val repo = sumber("src/main/java/id/or/kemenag/yasintahlil/core/domain/repository/UserDataRepository.kt")
        assertTrue("repository tidak menyediakan bookmark lintas jenis", repo.contains("fun allBookmarks()"))
        val room = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/local/RoomUserDataRepository.kt")
        assertTrue("implementasi Room tidak membaca seluruh bookmark",
            room.contains("override fun allBookmarks()"))
    }
}
