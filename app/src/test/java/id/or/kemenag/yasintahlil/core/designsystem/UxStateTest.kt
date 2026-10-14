package id.or.kemenag.yasintahlil.core.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` meminta: *"Berikan skeleton loading atau indikator proses saat memuat data"*,
 * *"Sediakan tampilan kosong, pesan kesalahan, dan tombol mencoba kembali"*, serta *"Hindari
 * tombol dekoratif yang tidak bekerja"*.
 *
 * Uji ini menjaga agar komponen keadaan tetap ada **dan** benar-benar dipakai, agar pengaturan
 * tampilan bacaan terpusat di tab Pengaturan (bukan kembali bertebaran sebagai sheet/chip di tiap
 * layar bacaan), dan agar tak ada tombol yang tampak bisa ditekan tetapi tak melakukan apa pun.
 */
class UxStateTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun featureSources(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/feature")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun uiKit(): String =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/designsystem/UiKit.kt").readText()

    private fun semuaSumberUi(): List<File> = featureSources() +
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/designsystem")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun komponen_keadaan_dideklarasikan() {
        val ui = uiKit()
        listOf("EmptyState", "ErrorState", "LoadingState", "SkeletonCard")
            .forEach { nama ->
                assertTrue("UiKit.kt tidak mendeklarasikan $nama",
                    Regex("""fun\s+$nama\s*\(""").containsMatchIn(ui))
            }
    }

    @Test
    fun komponen_keadaan_dipakai_layar() {
        val sumber = featureSources().map { it.readText() }
        listOf("EmptyState", "ErrorState", "LoadingState", "SkeletonCard").forEach { nama ->
            assertTrue("Tidak ada layar fitur yang memakai $nama — komponen mati",
                sumber.any { it.contains("$nama(") })
        }
    }

    /**
     * Layar yang menampilkan bacaan tidak lagi membawa pengaturannya sendiri — semuanya terpusat
     * di layar Pengaturan. Penjaga ini memastikan pengaturan bacaan memang tersedia di sana
     * (sakelar + pengukur + kuncinya), supaya pemusatan tidak berubah menjadi penghilangan.
     */
    @Test
    fun pengaturan_bacaan_terpusat_di_pengaturan() {
        val layar = File(
            root(), "src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt",
        ).readText()
        listOf(
            "Store.SHOW_ARAB", "Store.SHOW_LATIN", "Store.SHOW_TRANS",
            "Store.ARAB_SIZE", "Store.LATIN_SIZE", "Store.LINE_HEIGHT",
        ).forEach { kunci ->
            assertTrue("layar Pengaturan tidak mengatur $kunci", layar.contains(kunci))
        }
        assertTrue("judul bagian pengaturan bacaan bukan nama bersama",
            layar.contains("BagianJudul(NamaAntarmuka.PENGATURAN_BACAAN)"))
    }

    /** Rentang ukuran yang sah hanya ditulis sekali, di `UkuranBacaan` — bukan angka lepas per layar. */
    @Test
    fun rentang_ukuran_ditulis_sekali() {
        val aturan = File(
            root(), "src/main/java/id/or/kemenag/yasintahlil/core/util/UkuranBacaan.kt",
        ).readText()
        listOf("16f..40f", "12f..24f", "1.4f..2.6f").forEach { r ->
            assertTrue("Rentang $r tidak ada di aturan bersama UkuranBacaan", aturan.contains(r))
        }
        val pengaturan = File(
            root(), "src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt",
        ).readText()
        listOf("UkuranBacaan.ARAB", "UkuranBacaan.LATIN", "UkuranBacaan.JARAK").forEach { r ->
            assertTrue("$r tidak dipakai layar Pengaturan — pengaturannya bisa berbeda dari aturannya", r in pengaturan)
        }
        val lepas = featureSources().filter { f ->
            val t = f.readText()
            listOf("16f..40f", "12f..24f", "1.4f..2.6f").any { t.contains(it) }
        }.map { it.name }
        assertTrue("Angka rentang ditulis ulang di layar (bukan dari aturan bersama): $lepas",
            lepas.isEmpty())
    }

    /**
     * Tidak ada tombol dekoratif: `onClick = {}` hanya boleh dipakai pada kontrol yang memang
     * dimatikan (`enabled = false`) sebagai penanda status, atau pada kontrol yang bekerja lewat
     * gestur lain — kartu bacaan membuka lembar aksinya dengan **menekan lama**
     * (`onLongClick`), jadi ketukan biasanya memang tak melakukan apa pun dan itu bukan tombol
     * mati: pengguna tetap punya jalan mengoperasikannya.
     */
    @Test
    fun tidak_ada_tombol_dekoratif() {
        val pelanggar = mutableListOf<String>()
        semuaSumberUi().forEach { f ->
            val t = f.readText()
            Regex("""onClick\s*=\s*\{\s*\}""").findAll(t).forEach { m ->
                val jendela = t.substring(maxOf(0, m.range.first - 240), minOf(t.length, m.range.last + 240))
                val dimatikan = jendela.contains("enabled = false")
                val bekerjaLewatGesturLain = jendela.contains("onLongClick")
                if (!dimatikan && !bekerjaLewatGesturLain) {
                    val baris = t.take(m.range.first).count { it == '\n' } + 1
                    pelanggar += "${f.name}:$baris"
                }
            }
        }
        assertTrue("Tombol yang tampak aktif tetapi tak melakukan apa pun: $pelanggar",
            pelanggar.isEmpty())
    }
}
