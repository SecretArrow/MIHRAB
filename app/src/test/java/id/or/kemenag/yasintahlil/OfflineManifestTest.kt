package id.or.kemenag.yasintahlil

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/**
 * Peta arah kiblat sengaja **offline** (gratikula + rute lingkaran besar, tanpa ubin jaringan
 * dan tanpa API key). Uji ini menjaga janji "100% offline": manifes tidak boleh mendeklarasikan
 * izin jaringan. Bila kelak sumber jadwal online benar-benar diaktifkan, uji ini harus
 * diperbarui bersama manifes — bukan dihapus diam-diam.
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

    @Test fun tanpa_izin_jaringan() {
        val m = manifest()
        assertFalse("Manifes mendeklarasikan izin INTERNET", m.contains("android.permission.INTERNET"))
        assertFalse("Manifes mendeklarasikan ACCESS_NETWORK_STATE",
            m.contains("android.permission.ACCESS_NETWORK_STATE"))
    }
}
