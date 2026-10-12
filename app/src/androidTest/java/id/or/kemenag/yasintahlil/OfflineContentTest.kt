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

    /**
     * Setiap entri dzikir harus benar-benar menghasilkan teks — termasuk entri yang hanya
     * menunjuk berkas rujukan (`berkas#id`). Rujukan yang salah ketik tidak melempar galat; ia
     * hanya tampil sebagai kartu kosong di layar, jadi jalur ini diuji pada konten sungguhan.
     */
    @Test
    fun setiapEntriDzikirMenghasilkanTeksLengkap() {
        val kosong = repo.dzikir().flatMap { g -> g.items.map { "${g.id}:${it.judul}" to it } }
            .filter { (_, it) -> it.arab.isBlank() || it.latin.isBlank() || it.terjemahan.isBlank() }
            .map { it.first }
        assertTrue("entri dzikir tanpa teks lengkap: $kosong", kosong.isEmpty())
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

    /**
     * `plan.txt` §"Fitur Tahlil Lengkap": susunan harus *"berdasarkan sumber atau tradisi yang
     * jelas"*. Susunan dari bundel konten wajib menyebut sumbernya (per-susunan atau keterangan
     * `edisi` tingkat modul) — susunan tanpa asal-usul tidak boleh tampil sebagai rangkaian telanjang.
     */
    @Test
    fun susunanDariBundelMenyebutSumbernya() {
        repo.tahlilSusunan().forEach { susunan ->
            assertTrue(
                "susunan ${susunan.id} tidak menyebut sumber apa pun",
                susunan.sumber.isNotBlank(),
            )
        }
    }

    @Test
    fun aplikasiTidakMemintaIzinInternet() {
        @Suppress("DEPRECATION")
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PERMISSIONS)
        val perms = info.requestedPermissions?.toList().orEmpty()
        assertFalse("INTERNET diminta padahal aplikasi offline", perms.contains(Manifest.permission.INTERNET))
        // ACCESS_NETWORK_STATE sengaja tidak diuji di sini: izin itu disuntikkan manifes
        // pustaka androidx.work (bukan dideklarasikan aplikasi), sehingga manifes gabungan
        // selalu memuatnya walau aplikasi sendiri tak pernah menyentuh jaringan. Yang penting
        // adalah tidak adanya INTERNET — dan itu diperiksa di atas serta di OfflineManifestTest.
    }
}
