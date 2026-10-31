package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Hitung mundur *"Berikutnya: …"* — satu aturan, satu tulisan, dan satu perilaku.
 *
 * Waktu berikutnya sendiri sudah lama datang dari satu tempat ([JadwalHariIni.berikutnya]), tetapi
 * **jeda**-nya masih dikerjakan tiap layar sendiri-sendiri:
 *
 * - Beranda menulis `$sisaMenit / 60` lalu `j`, dan `$sisaMenit % 60` lalu `m lagi`;
 * - Layar Jadwal menulis pembagian yang sama dengan satuan `jam` dan `mnt`.
 *
 * Dua pembagian yang sama, dua tulisan yang berbeda — dan keduanya selalu menyebut jamnya, sehingga
 * jeda sepuluh menit terbaca *"0 jam 10 mnt"*. Yang lebih sunyi lagi: beranda menghitung ulang tiap
 * detik (detak `nowMillis` di `HomeViewModel`), sementara layar Jadwal mengisi `nowMin` **hanya saat
 * layar dimuat**, sehingga pengguna yang membiarkan layar itu terbuka membaca jeda yang membeku
 * pada menit ia membukanya — dua perilaku untuk satu baris yang sama.
 *
 * Uji ini menjaga ketiganya: ejaannya ([FormatWaktu.durasiJamMenit]), pemakaiannya oleh kedua layar,
 * dan detak jam yang membuat baris itu hidup.
 */
class HitungMundurBerikutnyaSatuTempatTest {

    private fun root(): File = KodeSumber.root()

    private fun layar(relatif: String): String =
        KodeSumber.kodeTanpaKomentar(
            File(root(), "src/main/java/id/or/kemenag/yasintahlil/$relatif").readText())

    private fun fitur(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/feature")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private val home get() = layar("feature/home/HomeScreen.kt")
    private val jadwal get() = layar("feature/jadwal/JadwalScreen.kt")
    private val vmJadwal get() = layar("feature/jadwal/JadwalViewModel.kt")

    /** Isi blok `{ … }` yang dibuka [indeksBuka], dengan kurung yang seimbang. */
    private fun blok(teks: String, indeksBuka: Int): String? {
        var depth = 0
        for (j in indeksBuka until teks.length) {
            when (teks[j]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return teks.substring(indeksBuka, j + 1)
                }
            }
        }
        return null
    }

    // ---- Ejaannya satu --------------------------------------------------------------------------

    /** Ejaannya dipaku: satuannya dipilih menurut nilainya, bukan selalu dua-duanya. */
    @Test
    fun durasiJamMenitDiejaMenurutNilainya() {
        assertEquals("2 jam 15 mnt", FormatWaktu.durasiJamMenit(135))
        assertEquals("45 mnt", FormatWaktu.durasiJamMenit(45))
        assertEquals("2 jam", FormatWaktu.durasiJamMenit(120))
        assertEquals("1 mnt", FormatWaktu.durasiJamMenit(1))
        assertEquals("24 jam", FormatWaktu.durasiJamMenit(PrayerCalc.MENIT_SEHARI))
    }

    /**
     * Jeda di bawah satu jam tidak boleh menyebut jamnya sama sekali. Inilah cacat yang lahir dari
     * dua pembagian yang ditulis sendiri-sendiri: `0 jam 10 mnt` — jam yang tidak ada, disebut.
     */
    @Test
    fun jedaDiBawahSatuJamTanpaJam() {
        (1 until 60).forEach { m ->
            val teks = FormatWaktu.durasiJamMenit(m)
            assertTrue("durasiJamMenit($m) = \"$teks\" masih menyebut jam", !teks.contains("jam"))
            assertEquals("$m mnt", teks)
        }
    }

    /** Jam yang pas tidak menyebut menitnya; jeda nol dijawab dengan kata, bukan "0 mnt". */
    @Test
    fun jamPasDanJedaNol() {
        listOf(1, 2, 5, 11).forEach { jam ->
            assertEquals("$jam jam", FormatWaktu.durasiJamMenit(jam * 60))
        }
        assertEquals("kurang dari 1 mnt", FormatWaktu.durasiJamMenit(0))
        assertEquals("kurang dari 1 mnt", FormatWaktu.durasiJamMenit(-5))
    }

    /** Setiap nilai yang mungkin keluar dari [JadwalHariIni.berikutnya] dapat dieja tanpa galat. */
    @Test
    fun setiapJedaYangMungkinDapatDieja() {
        (1..PrayerCalc.MENIT_SEHARI).forEach { m ->
            val teks = FormatWaktu.durasiJamMenit(m)
            assertTrue("durasiJamMenit($m) kosong", teks.isNotBlank())
            assertTrue("durasiJamMenit($m) = \"$teks\" tidak berakhiran satuan",
                teks.endsWith(" mnt") || teks.endsWith(" jam"))
        }
    }

    // ---- Pemakaiannya satu ----------------------------------------------------------------------

    /**
     * Tak ada layar yang membagi sendiri jedanya. Yang dicari adalah pembagiannya — bentuk salinan
     * yang menyimpang tetap memuat `/ 60` dan `% 60` yang sama.
     */
    @Test
    fun layarTidakMengejaJedaSendiri() {
        val offenders = fitur().flatMap { f ->
            val kode = KodeSumber.kodeTanpaKomentar(f.readText())
            listOf("sisaMenit / 60", "sisaMenit % 60", "sisaMenit/60", "sisaMenit%60")
                .filter { kode.contains(it) }
                .map { "${f.name}: $it" }
        }
        assertTrue(
            "Ada layar yang membagi jeda hitung mundur sendiri alih-alih memakai " +
                "FormatWaktu.durasiJamMenit:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    /** Kedua layar yang menampilkan *"Berikutnya: …"* memakai ejaan bersama. */
    @Test
    fun keduaLayarMemakaiAturanBersama() {
        mapOf("Beranda" to home, "Jadwal" to jadwal).forEach { (nama, teks) ->
            assertTrue("$nama tidak mengeja jedanya lewat FormatWaktu.durasiJamMenit",
                teks.contains("FormatWaktu.durasiJamMenit("))
        }
    }

    // ---- Perilakunya satu -----------------------------------------------------------------------

    /**
     * Baris *"Berikutnya: …"* di layar Jadwal harus hidup, sama seperti hitung mundur di beranda.
     *
     * `JadwalViewModel` tak dapat dijalankan di JVM (butuh `Context`), jadi detaknya diperiksa dari
     * sumbernya: harus ada satu putaran yang menunggu (`delay`) dan menyegarkan `nowMin` — bukan
     * hanya diisi di `recompute()`, yang dahulu berarti nilai itu membeku sejak layar dibuka.
     */
    @Test
    fun hitungMundurLayarJadwalIkutHidup() {
        val kode = vmJadwal
        val mulai = kode.indexOf("while (true)")
        assertTrue("JadwalViewModel tidak punya detak jam — hitung mundurnya membeku sejak layar dibuka",
            mulai >= 0)
        val buka = kode.indexOf('{', mulai)
        assertTrue("blok detak jam JadwalViewModel tidak terbaca", buka >= 0)
        val isi = blok(kode, buka)
        assertTrue("detak jam JadwalViewModel tidak terbaca", isi != null)
        assertTrue("detak jam JadwalViewModel tidak menunggu (delay) — putaran tanpa jeda",
            isi!!.contains("delay("))
        assertTrue("detak jam JadwalViewModel tidak menyegarkan nowMin — baris \"Berikutnya: …\" " +
            "tetap membeku", isi.contains("nowMin"))
    }
}
