package id.or.kemenag.yasintahlil.core.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Arah Kiblat": *"Indikator status kalibrasi sensor"*, *"Petunjuk menggerakkan
 * perangkat membentuk pola angka delapan jika sensor memerlukan kalibrasi"*, dan *"Jangan
 * mengklaim arah kompas selalu akurat"*.
 *
 * Yang mudah salah: memperlakukan "belum ada pembacaan" sama dengan "terkalibrasi". Sebelum sensor
 * mengirim satu pun nilai, aplikasi tidak tahu apa-apa soal kalibrasi — dan jarum yang digambar
 * saat itu tampak meyakinkan padahal arahnya belum tentu benar.
 */
class KalibrasiKompasTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    @Test
    fun tanpaSensorSelaluTanpaSensor() {
        listOf(null, true, false).forEach { akurasi ->
            assertEquals("akurasi $akurasi tidak mengubah status tanpa sensor",
                StatusKompas.TANPA_SENSOR, KalibrasiKompas.status(false, akurasi))
        }
    }

    /** Sebelum ada pembacaan, statusnya belum diketahui — bukan "terkalibrasi". */
    @Test
    fun belumAdaPembacaanBerartiMenunggu() {
        assertEquals(StatusKompas.MENUNGGU, KalibrasiKompas.status(true, null))
        assertFalse("status tanpa pembacaan tidak boleh tampil sebagai peringatan",
            KalibrasiKompas.perluPerhatian(KalibrasiKompas.status(true, null)))
    }

    @Test
    fun akurasiSensorMenentukanStatus() {
        assertEquals(StatusKompas.TERKALIBRASI, KalibrasiKompas.status(true, true))
        assertEquals(StatusKompas.PERLU_KALIBRASI, KalibrasiKompas.status(true, false))
    }

    /** Pola angka delapan hanya diminta saat sensornya memang perlu dikalibrasi. */
    @Test
    fun hanyaPerluKalibrasiYangMemintaPolaDelapan() {
        assertEquals(StatusKompas.entries.filter { KalibrasiKompas.perluPolaDelapan(it) },
            listOf(StatusKompas.PERLU_KALIBRASI))
    }

    @Test
    fun peringatanHanyaUntukYangMemangBermasalah() {
        assertEquals(StatusKompas.entries.filter { KalibrasiKompas.perluPerhatian(it) },
            listOf(StatusKompas.TANPA_SENSOR, StatusKompas.PERLU_KALIBRASI))
    }

    /** Status tidak boleh disampaikan lewat warna saja — setiap status punya tanda dan label sendiri. */
    @Test
    fun setiapStatusPunyaLabelDanTandaSendiri() {
        val label = StatusKompas.entries.map { KalibrasiKompas.label(it) }
        val tanda = StatusKompas.entries.map { KalibrasiKompas.tanda(it) }
        assertTrue("ada label status yang kosong", label.none { it.isBlank() })
        assertTrue("ada tanda status yang kosong", tanda.none { it.isBlank() })
        assertEquals("label status tidak boleh sama satu dengan lain", label.size, label.distinct().size)
        assertEquals("tanda status tidak boleh sama satu dengan lain", tanda.size, tanda.distinct().size)
    }

    /** Layar Kiblat menampilkan indikatornya, dan status awal tidak mengklaim "terkalibrasi". */
    @Test
    fun layarKiblatMenampilkanStatusKalibrasi() {
        val layar = File(root(),
            "src/main/java/id/or/kemenag/yasintahlil/feature/kiblat/KiblatScreen.kt").readText()
        assertTrue("KiblatScreen tidak menghitung status kalibrasi",
            layar.contains("KalibrasiKompas.status("))
        assertTrue("KiblatScreen tidak memberi judul indikator status",
            layar.contains("Status kalibrasi sensor"))
        assertTrue("KiblatScreen tidak memakai petunjuk pola angka delapan",
            layar.contains("KalibrasiKompas.perluPolaDelapan("))

        val vm = File(root(),
            "src/main/java/id/or/kemenag/yasintahlil/feature/kiblat/KiblatViewModel.kt").readText()
        assertTrue("status kalibrasi awal harus 'belum diketahui' (null), bukan 'terkalibrasi'",
            vm.contains("val calibrated: Boolean? = null"))
    }
}
