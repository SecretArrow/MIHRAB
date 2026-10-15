package id.or.kemenag.yasintahlil.feature

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Kualitas Kode dan Pengujian" meminta pengujian untuk *"berbagai ukuran layar"*, dan
 * §"UI/UX dan Navigasi" meminta tata letak yang menyesuaikan ukuran layar. Memutar perangkat adalah
 * cara paling umum pengguna berpindah ukuran layar — dan karena `MainActivity` **tidak** mengunci
 * orientasi (`android:screenOrientation` tidak ada di manifest), rotasi membangun ulang Activity
 * beserta seluruh komposisinya.
 *
 * Apa pun yang disimpan di `remember` biasa lenyap saat itu. Untuk sebagian keadaan itu tidak
 * apa-apa: `settings` adalah panel modal yang memang wajar tertutup, dan `indeks`/`galat`/`ulang`
 * dibangun ulang oleh efek. Yang **tidak** boleh lenyap adalah keadaan yang dipilih pengguna dan
 * tak dapat dipulihkan sendiri: mode layar penuh dan mode baca, cara pengelompokan yang dipilih,
 * kata kunci yang sedang diketik. (Tab dan langkah panduan tidak lagi punya keadaan tersimpan —
 * layar Tahlil, kelompok dzikir, dan langkah sholat kini bersambung, tanpa tombol next/prev.)
 * Kehilangan `q` berarti pengguna mengetik ulang kata kuncinya; kehilangan `ringkas` berarti
 * ia terlempar kembali ke tampilan pemula hanya karena memutar perangkat.
 *
 * Uji ini menjaga daftar itu tetap di `rememberSaveable`. Isinya sengaja **disebut satu per satu**,
 * bukan "semua `remember` harus `rememberSaveable`" — aturan yang lebih luas itu akan memaksa
 * keadaan turunan dan modal ikut disimpan, dan itu bukan yang diinginkan.
 */
class KeadaanLayarSaatDiputarTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    /** Keadaan yang harus bertahan: berkas → nama variabel → alasan singkat. */
    private val wajibBertahan: List<Triple<String, String, String>> = listOf(
        Triple("feature/dzikir/DzikirGroupScreen.kt", "fullscreen", "mode layar penuh"),
        Triple("feature/sholat/SholatScreen.kt", "cara", "cara pengelompokan yang dipilih"),
        Triple("feature/sholat/SholatDetailScreen.kt", "ringkas", "mode ringkas/pemula"),
        Triple("feature/sholat/SholatDetailScreen.kt", "selesai", "tanda sudah selesai"),
        Triple("feature/sholat/SholatDetailScreen.kt", "syaratTerbuka", "daftar syarat yang dibuka"),
        Triple("feature/sholat/GhaibScreen.kt", "fullscreen", "mode layar penuh"),
        Triple("feature/sholat/JenazahScreen.kt", "fullscreen", "mode layar penuh"),
        Triple("feature/adzan/IqomahDoaScreen.kt", "fullscreen", "mode layar penuh"),
        Triple("feature/tahlil/TahlilScreen.kt", "jamaah", "mode bacaan berjamaah"),
        Triple("feature/tahlil/TahlilScreen.kt", "fullscreen", "mode layar penuh"),
        Triple("feature/tahlil/TahlilScreen.kt", "susun", "panel susunan tahlil"),
        Triple("feature/tahlil/TahlilScreen.kt", "jump", "nomor bagian yang sedang diketik"),
        Triple("feature/kiblat/KiblatScreen.kt", "fullscreen", "mode layar penuh"),
        Triple("feature/yasin/YasinScreen.kt", "focus", "mode baca fokus"),
        Triple("feature/yasin/YasinScreen.kt", "jump", "nomor ayat yang sedang diketik"),
        Triple("feature/search/SearchScreen.kt", "q", "kata kunci yang sedang diketik"),
    )

    /**
         * Keadaan Yasin yang dahulu dijaga di sini dan kini hidup di tempat lain — lebih awet
         * daripada `rememberSaveable`:
         *
         * - `keepOn` ("tetap menyala") kini preferensi global `Store.KEEP_ON` yang diatur di tab
         *   Pengaturan dan dibaca reaktif oleh layar Yasin — ia bertahan bahkan saat proses mati;
         * - `query` (pencarian ayat di layar Yasin) sudah dipindahkan ke layar Pencarian terpadu
         *   (kata kuncinya dijaga sebagai `q` di berkas itu); dan
         * - `showBookmarks` (panel daftar bookmark) sudah tidak ada — daftarnya tinggal di layar
         *   Favorit, dan aksi bookmarknya ada pada lembar aksi tekan-lama kartu ayat.
         */

    @Test
    fun keadaanPilihanPenggunaBertahanSaatDiputar() {
        val hilang = mutableListOf<String>()
        wajibBertahan.forEach { (berkas, nama, alasan) ->
            val src = sumber("src/main/java/id/or/kemenag/yasintahlil/$berkas")
            // Deklarasinya harus `var <nama> by rememberSaveable { … }` — bukan `remember` biasa.
            val pola = Regex("""var\s+$nama\s+by\s+rememberSaveable\s*\{""")
            if (!pola.containsMatchIn(src)) {
                val apa = if (Regex("""var\s+$nama\s+by\s+remember\s*\{""").containsMatchIn(src)) {
                    "masih `remember` biasa"
                } else {
                    "tidak ditemukan"
                }
                hilang += "$berkas: `$nama` ($alasan) $apa"
            }
        }
        assertTrue(
            "Keadaan ini lenyap saat perangkat diputar karena tak disimpan di rememberSaveable:\n" +
                hilang.joinToString("\n"),
            hilang.isEmpty(),
        )
    }

    /** Keadaan yang sengaja tetap `remember` — didokumentasikan supaya tak "diperbaiki" keliru. */
    @Test
    fun keadaanSementaraSengajaTidakDisimpan() {
        val salah = mutableListOf<String>()
        listOf(
            Triple("feature/search/SearchScreen.kt", "indeks", "dibangun ulang oleh LaunchedEffect"),
            Triple("feature/search/SearchScreen.kt", "galat", "dibangun ulang oleh LaunchedEffect"),
            Triple("feature/search/SearchScreen.kt", "ulang", "penghitung percobaan ulang, bukan pilihan"),
            Triple("feature/adzan/AdzanScreen.kt", "putarGagal", "pesan galat pemutaran sementara"),
        ).forEach { (berkas, nama, alasan) ->
            val src = sumber("src/main/java/id/or/kemenag/yasintahlil/$berkas")
            if (Regex("""var\s+$nama\s+by\s+rememberSaveable\s*\{""").containsMatchIn(src)) {
                salah += "$berkas: `$nama` ($alasan) — sengaja bukan keadaan pilihan pengguna"
            }
        }
        assertTrue(salah.joinToString("\n"), salah.isEmpty())
    }

    /**
     * `rememberSaveable` berasal dari paket `runtime.saveable`, **bukan** `runtime`. Layar yang
     * mengimpor `androidx.compose.runtime.*` saja tidak mendapatkannya, sehingga penambahan ini
     * mudah terlewat dan baru ketahuan saat kompilasi.
     */
    @Test
    fun berkasYangMemakainyaMengimporPaketYangBenar() {
        val kurang = mutableListOf<String>()
        wajibBertahan.map { it.first }.distinct().forEach { berkas ->
            val src = sumber("src/main/java/id/or/kemenag/yasintahlil/$berkas")
            if (!src.contains("import androidx.compose.runtime.saveable.rememberSaveable")) {
                kurang += berkas
            }
        }
        assertTrue("Berkas memakai rememberSaveable tanpa mengimpor paketnya: $kurang", kurang.isEmpty())
    }
}
