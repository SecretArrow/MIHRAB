package id.or.kemenag.yasintahlil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Label menu adalah **penunjuk arah**, bukan kutipan sumber.
 *
 * Dua tombol menu dahulu menempelkan nama lembaga pada hal yang tidak berasal darinya: "Jadwal
 * Sholat (SIHAT)" — padahal jadwalnya dihitung **di perangkat** (algoritma Meeus, sudut Kemenag) dan
 * aplikasi tidak pernah menghubungi SIHAT, sesuatu yang layar Pengaturan sendiri nyatakan ("sumber
 * online resmi belum diaktifkan"); dan "Yasin (83 ayat, MSI Kemenag)" — jumlah ayat serta nama
 * sumbernya dipatok di kode, sehingga tak dapat ikut berubah bila bundelnya diperbarui, persis
 * hardcode yang sudah ditolak `AtribusiTeksQuranTest` pada layar Yasin.
 *
 * Klaim sumber yang tak dapat ditepati lebih buruk daripada tanpa klaim: pengguna mengira angkanya
 * berasal dari lembaga resmi, padahal hasil hitungan aplikasi. Asal-usul yang benar disebut di layar
 * masing-masing, dibaca dari bundelnya. Uji ini menjaga agar klaim itu tidak merayap kembali ke
 * label menu.
 */
class KlaimSumberMenuTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    /**
     * Hanya baris yang benar-benar menjadi label UI — yang memuat pemanggilan `Text(`.
     * Komentar penjelas justru boleh menyebut sebabnya, jadi ia tidak ikut diperiksa.
     */
    private fun label(berkas: String): List<String> =
        sumber(berkas).lines().filter { it.contains("Text(") }

    private val menu = listOf(
        "src/main/java/id/or/kemenag/yasintahlil/feature/alat/AlatScreen.kt",
        "src/main/java/id/or/kemenag/yasintahlil/feature/ibadah/IbadahScreen.kt",
    )

    @Test
    fun labelMenuTidakMenempelkanKlaimSumber() {
        val terlarang = listOf("SIHAT", "MSI", "Kemenag", "ayat")
        menu.forEach { rel ->
            val baris = label(rel)
            assertTrue("$rel tidak memuat satu pun label untuk diperiksa", baris.isNotEmpty())
            baris.forEach { l ->
                terlarang.forEach { token ->
                    assertFalse(
                        "$rel menempelkan klaim \"$token\" pada label menu — " +
                            "sebut asal-usulnya di layar tujuan, bukan di tombol: $l",
                        l.contains(token),
                    )
                }
            }
        }
    }

    @Test
    fun judulKiblatTidakMengatasnamakanLembaga() {
        val kiblat = sumber("src/main/java/id/or/kemenag/yasintahlil/feature/kiblat/KiblatScreen.kt")
        assertFalse("judul layar Kiblat menempelkan nama lembaga pada angka yang dihitung sendiri",
            kiblat.contains("Text(\"Arah Kiblat (SIHAT"))
        assertTrue("layar Kiblat tidak menyebut asal koordinat Ka'bah yang benar-benar dipakainya",
            kiblat.contains("koordinat Ka'bah yang diterbitkan SIHAT Bimas Islam"))
    }
}
