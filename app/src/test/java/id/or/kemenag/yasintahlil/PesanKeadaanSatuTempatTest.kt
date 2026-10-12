package id.or.kemenag.yasintahlil

import id.or.kemenag.yasintahlil.core.util.PengingatAdzan
import id.or.kemenag.yasintahlil.core.util.PesanKonten
import id.or.kemenag.yasintahlil.core.util.PesanLokasi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Kalimat yang menjelaskan **keadaan yang sama** harus punya satu tempat.
 *
 * Bukan kerapian: tiga kalimat pengguna di aplikasi ini dahulu ditulis berulang, dan salinan yang
 * berbeda membuat satu keadaan terbaca berbeda-beda antarlayar.
 *
 *  - *"Belum ada perbaikan lokasi GPS…"* ada di `JadwalViewModel` **dan** `KiblatViewModel`.
 *  - *"Berkas … tidak ditemukan di aplikasi. Pasang ulang aplikasi…"* ada di empat layar.
 *  - Penjelasan pengingat "baca doa setelah adzan" ada di layar Adzan **dan** layar Doa Adzan,
 *    masing-masing menyebut jeda 5 menitnya sendiri.
 *
 * Uji ini menjaga dua hal: aturannya benar-benar dibaca dari satu tempat, dan tidak ada layar yang
 * menuliskan kalimatnya lagi sendiri-sendiri.
 */
class PesanKeadaanSatuTempatTest {

    private val akar = File(KodeSumber.root(), "src/main/java/id/or/kemenag/yasintahlil")

    /** Seluruh berkas `.kt` di bawah `src/main`, kecuali berkas yang memang pemilik kalimatnya. */
    private fun berkasSelain(vararg dikecualikan: String): List<File> =
        akar.walkTopDown().filter { it.isFile && it.extension == "kt" && it.name !in dikecualikan }.toList()

    /** Kode [f] tanpa komentar; string dipertahankan supaya literal yang dilarang tetap terbaca. */
    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    private fun layar(relatif: String): String =
        KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif")

    // ---- Pesan lokasi ----------------------------------------------------------------------

    @Test
    fun sebabGpsSamaDanSaranBerbedaAntarLayar() {
        assertTrue("sebab Jadwal tidak memuat sebab bersama",
            PesanLokasi.TANPA_FIX_JADWAL.startsWith(PesanLokasi.TANPA_FIX_SEBAB))
        assertTrue("sebab Kiblat tidak memuat sebab bersama",
            PesanLokasi.TANPA_FIX_KIBLAT.startsWith(PesanLokasi.TANPA_FIX_SEBAB))
        // Sarannya memang berbeda: Jadwal punya pemilih kotanya sendiri, Kiblat tidak.
        assertFalse("saran kedua layar tidak boleh sama",
            PesanLokasi.TANPA_FIX_JADWAL == PesanLokasi.TANPA_FIX_KIBLAT)
    }

    @Test
    fun tidakAdaLayarYangMenulisUlangSebabGps() {
        val pelanggar = berkasSelain("PesanLokasi.kt")
            .filter { kode(it).contains(PesanLokasi.TANPA_FIX_SEBAB.substringBefore(".")) }
            .map { it.name }
        assertTrue("sebab GPS ditulis ulang di luar PesanLokasi: $pelanggar", pelanggar.isEmpty())
    }

    @Test
    fun keduaViewModelMembacaPesanLokasi() {
        assertTrue("layar Jadwal tidak membaca saran untuk pemilih kotanya",
            layar("feature/jadwal/JadwalViewModel.kt").contains("PesanLokasi.TANPA_FIX_JADWAL"))
        assertTrue("layar Kiblat tidak membaca saran untuk layar tanpa pemilih kota",
            layar("feature/kiblat/KiblatViewModel.kt").contains("PesanLokasi.TANPA_FIX_KIBLAT"))
    }

    // ---- Berkas konten yang tidak ditemukan ------------------------------------------------

    @Test
    fun berkasTidakAdaMenyusunKalimatLengkap() {
        assertEquals(
            "Berkas susunan tahlil tidak ditemukan di aplikasi. Pasang ulang aplikasi bila ini terus terjadi.",
            PesanKonten.berkasTidakAda("susunan tahlil"))
    }

    @Test
    fun empatLayarMemakaiPesanKontenUntukBerkasnya() {
        val diharapkan = mapOf(
            "feature/adzan/AdzanScreen.kt" to "lafaz adzan",
            "feature/tahlil/TahlilScreen.kt" to "susunan tahlil",
            "feature/dzikir/DzikirScreen.kt" to "kumpulan dzikir",
            "feature/settings/SettingsScreen.kt" to "susunan tahlil",
        )
        diharapkan.forEach { (berkas, nama) ->
            assertTrue("$berkas tidak memakai PesanKonten.berkasTidakAda(\"$nama\")",
                layar(berkas).contains("PesanKonten.berkasTidakAda(\"$nama\")"))
        }
    }

    @Test
    fun tidakAdaYangMenulisUlangSaranPasangUlang() {
        val pelanggar = berkasSelain("PesanKonten.kt")
            .filter { kode(it).contains("Pasang ulang aplikasi") }
            .map { it.name }
        assertTrue("saran \"Pasang ulang aplikasi\" ditulis ulang di luar PesanKonten: $pelanggar",
            pelanggar.isEmpty())
    }

    // ---- Penjelasan pengingat doa setelah adzan --------------------------------------------

    @Test
    fun penjelasanPengingatDoaDibacaDariAturanBersama() {
        listOf("feature/adzan/AdzanScreen.kt", "feature/adzan/IqomahDoaScreen.kt").forEach { berkas ->
            val teks = layar(berkas)
            listOf("DOA_SEBAB", "DOA_IKUT_SAKLAR", "DOA_BUKAN_SYARIAT").forEach { nama ->
                assertTrue("$berkas tidak memakai PengingatAdzan.$nama",
                    teks.contains("PengingatAdzan.$nama"))
            }
        }
    }

    @Test
    fun jedaPengingatDoaSebutAngkaYangSamaDenganPenjadwal() {
        // Angka 5-nya hidup di satu tempat; penjelasan di layar membacanya, penjadwal memakainya.
        assertEquals("jeda pengingat doa berubah tanpa penjelasannya", 5, PengingatAdzan.DOA_DELAY_MIN)
        assertTrue("sebab pengingat doa tidak menyebut jedanya",
            PengingatAdzan.DOA_SEBAB.contains("${PengingatAdzan.DOA_DELAY_MIN} menit"))
    }

    @Test
    fun tidakAdaYangMenulisUlangPenjelasanPengingatDoa() {
        val pelanggar = berkasSelain("PengingatAdzan.kt")
            .filter { kode(it).contains("pengingat kedua") }
            .map { it.name }
        assertTrue("penjelasan pengingat doa ditulis ulang di luar PengingatAdzan: $pelanggar",
            pelanggar.isEmpty())
    }
}
