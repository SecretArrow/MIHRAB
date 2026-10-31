package id.or.kemenag.yasintahlil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Izin jaringan dinyatakan **jujur**: INTERNET dipakai hanya oleh murottal Surat Yasin —
 * streaming per ayat yang dimulai pengguna sendiri — dan pemakaiannya dikunci di paket
 * `core/audio` (layar, penjadwal, dan konten tetap offline). `ACCESS_NETWORK_STATE` tetap
 * tidak boleh: aplikasi tidak pernah memeriksa status jaringan; kegagalan streaming cukup
 * ditangani sebagai galat biasa di tempat yang sama.
 *
 * Selain itu, izin hanya boleh dideklarasikan bila benar-benar dipakai:
 * `SCHEDULE_EXACT_ALARM` harus sejalan dengan adanya pemakaian `AlarmManager` di kode.
 */
class OfflineManifestTest {

    private fun manifest(): String {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        )
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("AndroidManifest.xml tidak ditemukan dari ${File(".").absolutePath}")
    }

    /** Gabungan seluruh sumber Kotlin `main` (kode produksi, bukan uji). */
    private fun mainSources(): String {
        val root = listOf(File("src/main"), File("app/src/main")).firstOrNull { it.isDirectory }
            ?: error("src/main tidak ditemukan dari ${File(".").absolutePath}")
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
    }

    /** Sumber Kotlin `main` **di luar** paket `core/audio` — tempat yang tak boleh menyentuh jaringan. */
    private fun sumberDiLuarAudio(): String {
        val root = listOf(File("src/main"), File("app/src/main")).firstOrNull { it.isDirectory }
            ?: error("src/main tidak ditemukan dari ${File(".").absolutePath}")
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && !it.absolutePath.replace('\\', '/').contains("/core/audio/") }
            .joinToString("\n") { it.readText() }
    }

    /**
     * INTERNET dideklarasikan untuk murottal — dan hanya untuk itu: penanda pemakaiannya
     * (domain sumber audio) tidak boleh muncul di luar paket `core/audio`, sehingga janji
     * "jaringan hanya untuk murottal" dijaga oleh kode, bukan oleh komentar.
     */
    @Test fun internet_hanya_untuk_murottal_di_paket_audio() {
        val m = manifest()
        assertTrue("Manifes belum mendeklarasikan INTERNET untuk murottal Yasin",
            m.contains("android.permission.INTERNET"))
        assertFalse("Manifes mendeklarasikan ACCESS_NETWORK_STATE — aplikasi tidak memeriksa status jaringan",
            m.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertFalse("Penanda pemakaian jaringan (domain murottal) muncul di luar core/audio: " +
            "pemakaian jaringan harus terkunci di paket audio",
            sumberDiLuarAudio().contains("everyayah.com"))
        assertTrue("Penanda pemakaian murottal tidak ditemukan di core/audio",
            mainSources().contains("everyayah.com"))
    }

    /** Pengingat memakai WorkManager; tanpa AlarmManager, izin alarm tepat waktu tidak boleh ada. */
    @Test fun izin_alarm_tepat_waktu_sejalan_dengan_pemakaian() {
        val pakaiAlarmManager = mainSources().contains("AlarmManager")
        val deklarasiIzin = manifest().let {
            it.contains("android.permission.SCHEDULE_EXACT_ALARM") ||
                it.contains("android.permission.USE_EXACT_ALARM")
        }
        assertEquals(
            "SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM harus dideklarasikan tepat saat AlarmManager dipakai",
            pakaiAlarmManager,
            deklarasiIzin,
        )
    }
}
