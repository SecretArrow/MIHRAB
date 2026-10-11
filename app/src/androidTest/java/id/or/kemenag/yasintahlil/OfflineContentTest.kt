package id.or.kemenag.yasintahlil

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * plan.txt: "Pengujian mode offline."
 *
 * Seluruh konten inti harus terbaca dari assets perangkat — tanpa jaringan sama sekali —
 * dan aplikasi tidak boleh mendeklarasikan izin INTERNET (janji 100% offline).
 */
@RunWith(AndroidJUnit4::class)
class OfflineContentTest {

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val repo: ContentRepo get() = ContentRepo(ctx)

    @Test
    fun yasinUtuhDibacaDariAset() {
        val yasin = repo.yasin
        assertEquals("jumlah ayat Yasin", 83, yasin.ayatCount)
        assertEquals(83, yasin.verses.size)
        assertEquals("nomor ayat harus 1..83 berurutan", (1..83).toList(), yasin.verses.map { it.nomor })
        assertTrue("ada ayat tanpa teks Arab", yasin.verses.all { it.arab.isNotBlank() })
        assertTrue("ada ayat tanpa terjemahan", yasin.verses.all { it.terjemahan.isNotBlank() })
    }

    @Test
    fun seluruhModulKontenTerbaca() {
        assertTrue("sholat wajib kosong", repo.sholatWajib().isNotEmpty())
        assertTrue("sholat sunnah kosong", repo.sholatSunnah().isNotEmpty())
        assertTrue("bacaan sholat kosong", repo.sholatBacaan().isNotEmpty())
        assertTrue("dzikir kosong", repo.dzikir().isNotEmpty())
        assertTrue("tahlil kosong", repo.tahlil().isNotEmpty())
        assertTrue("susunan tahlil kosong", repo.tahlilSusunan().isNotEmpty())
        assertTrue("lafaz adzan kosong", repo.adzan().isNotEmpty())
        assertTrue("lafaz iqomah kosong", repo.iqomah().isNotEmpty())
        assertTrue("daftar kota kosong", repo.kota().isNotEmpty())

        val (arab, latin, terjemahan) = repo.doaAdzan()
        assertTrue(arab.isNotBlank() && latin.isNotBlank() && terjemahan.isNotBlank())
    }

    /** Setiap susunan tahlil hanya boleh merujuk bagian yang benar-benar ada. */
    @Test
    fun susunanTahlilHanyaMerujukBagianYangAda() {
        val ids = repo.tahlil().map { it.id }.toSet()
        repo.tahlilSusunan().forEach { susunan ->
            assertTrue("susunan ${susunan.id} kosong", susunan.bagian.isNotEmpty())
            assertTrue(
                "susunan ${susunan.id} merujuk bagian tak dikenal: ${susunan.bagian - ids}",
                ids.containsAll(susunan.bagian),
            )
        }
    }

    @Test
    fun aplikasiTidakMemintaIzinInternet() {
        @Suppress("DEPRECATION")
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PERMISSIONS)
        val perms = info.requestedPermissions?.toList().orEmpty()
        assertFalse("INTERNET diminta padahal aplikasi offline", perms.contains(Manifest.permission.INTERNET))
        assertFalse("ACCESS_NETWORK_STATE diminta", perms.contains(Manifest.permission.ACCESS_NETWORK_STATE))
    }
}
