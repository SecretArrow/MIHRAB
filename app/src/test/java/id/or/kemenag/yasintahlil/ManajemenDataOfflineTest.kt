package id.or.kemenag.yasintahlil

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` meminta pengaturan "Manajemen data offline" — bukan hanya "hapus semua data".
 *
 * Uji ini menjaga tiga hal yang mudah rusak diam-diam: (1) setiap bagian data punya tombol hapusnya
 * sendiri, sehingga pengguna dapat membuang riwayat saja tanpa kehilangan favorit; (2) penghapusan
 * selalu lewat dialog konfirmasi yang menyebut jumlah entri, bukan menghapus dalam keadaan buta;
 * (3) implementasi Room benar-benar mengisi setiap anggota antarmuka [UserDataRepository], sehingga
 * layar pengaturan tak pernah memanggil fungsi yang belum ada.
 */
class ManajemenDataOfflineTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private fun pengaturan(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt")

    private fun antarmuka(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/core/domain/repository/UserDataRepository.kt")

    private fun implementasi(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/local/RoomUserDataRepository.kt")

    @Test
    fun setiapBagianDataPunyaTombolHapusSendiri() {
        val layar = pengaturan()
        listOf("clearFavorites", "clearBookmarks", "clearHistory", "clearProgress").forEach { nama ->
            assertTrue("layar Pengaturan tidak memakai $nama — bagian itu tak dapat dihapus sendiri",
                layar.contains(nama))
        }
        assertTrue("jumlah entri tidak ditampilkan sebelum menghapus", layar.contains("ringkasan"))
    }

    @Test
    fun jumlahEntriDitampilkanSebelumMenghapus() {
        val layar = pengaturan()
        listOf("favorit", "bookmark", "riwayat", "progres").forEach { bagian ->
            assertTrue("ringkasan tidak menyebut jumlah $bagian", layar.contains(bagian))
        }
    }

    @Test
    fun penghapusanSelaluLewatDialogKonfirmasi() {
        val layar = pengaturan()
        assertTrue("penghapusan tanpa dialog konfirmasi", layar.contains("AlertDialog("))
        assertTrue("dialog tidak menyediakan pilihan batal", layar.contains("\"Batal\""))
        assertTrue("dialog tidak menyebut jumlah entri yang akan hilang", layar.contains("entri "))
    }

    @Test
    fun implementasiRoomMengisiSeluruhAnggotaAntarmuka() {
        val nama = Regex("""^\s*(?:suspend )?fun (\w+)\(""", RegexOption.MULTILINE)
            .findAll(antarmuka()).map { it.groupValues[1] }.toSet()
        assertTrue("antarmuka tak terbaca", nama.size >= 15)

        val impl = implementasi()
        val hilang = nama.filterNot { impl.contains("fun $it(") }
        assertTrue("anggota UserDataRepository belum diimplementasikan: $hilang", hilang.isEmpty())
    }

    @Test
    fun setiapTabelPunyaQueryHitung() {
        val daos = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/local/Daos.kt")
        listOf("favorites", "bookmarks", "history", "progress").forEach { tabel ->
            assertTrue("tabel $tabel tidak punya query COUNT(*) untuk ringkasan",
                daos.contains("SELECT COUNT(*) FROM $tabel"))
        }
    }
}
