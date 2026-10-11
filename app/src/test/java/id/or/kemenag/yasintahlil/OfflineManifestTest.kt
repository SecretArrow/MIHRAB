package id.or.kemenag.yasintahlil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/**
 * Peta arah kiblat sengaja **offline** (gratikula + rute lingkaran besar, tanpa ubin jaringan
 * dan tanpa API key). Uji ini menjaga janji "100% offline": manifes tidak boleh mendeklarasikan
 * izin jaringan. Bila kelak sumber jadwal online benar-benar diaktifkan, uji ini harus
 * diperbarui bersama manifes — bukan dihapus diam-diam.
 *
 * Selain itu, izin hanya boleh dideklarasikan bila benar-benar dipakai: `SCHEDULE_EXACT_ALARM`
 * harus sejalan dengan adanya pemakaian `AlarmManager` di kode.
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

    @Test fun tanpa_izin_jaringan() {
        val m = manifest()
        assertFalse("Manifes mendeklarasikan izin INTERNET", m.contains("android.permission.INTERNET"))
        assertFalse("Manifes mendeklarasikan ACCESS_NETWORK_STATE",
            m.contains("android.permission.ACCESS_NETWORK_STATE"))
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
