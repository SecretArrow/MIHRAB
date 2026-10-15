package id.or.kemenag.yasintahlil

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * plan.txt: "Pengujian mode offline."
 *
 * Seluruh konten inti harus terbaca dari assets perangkat — tanpa jaringan sama sekali. Izin
 * INTERNET kini SAH dideklarasikan untuk satu hal: murottal Surat Yasin yang dialirkan per ayat
 * saat pengguna sendiri menekan putar — janji "jaringan hanya untuk murottal, pemakaiannya
 * terkunci di paket core/audio" dijaga di sisi sumber oleh `OfflineManifestTest`.
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

    /**
     * Lafaz adzan dan iqomah berasal dari hadis, jadi tiap lafaz dari bundel wajib menyebut
     * rujukannya. `adzan.json` mencatatnya di tingkat bagian (`adzan`/`iqomah`), dan pembacaan
     * berkas menjatuhkan diri ke keterangan itu bila entri tidak menyebut sumbernya sendiri —
     * tanpa cadangan tersebut, seluruh lafaz tampil tanpa asal-usulnya.
     */
    @Test
    fun lafazAdzanDanIqomahMenyebutSumbernya() {
        (repo.adzan().map { "adzan" to it } + repo.iqomah().map { "iqomah" to it }).forEach { (bagian, l) ->
            assertTrue(
                "lafaz $bagian \"${l.arab.take(12)}\" tidak menyebut sumber apa pun",
                l.sumber.isNotBlank(),
            )
        }
    }

    /**
     * Teks Qur'an wajib menyebut asal teks Arab, transliterasi, dan terjemahannya — ketiganya
     * dicatat terpisah di berkas dan berasal dari bagian berbeda layanan resmi Kemenag. Bila salah
     * satu kosong, layar tidak dapat menyebutkannya, dan pengguna kehilangan asal-usul teks yang
     * ia baca.
     */
    @Test
    fun surahYasinMenyebutSumberKetigaTeksnya() {
        val yasin = repo.yasin
        assertTrue("Surat Yasin tidak menyebut sumber teks Arab", yasin.sumberArab.isNotBlank())
        assertTrue("Surat Yasin tidak menyebut sumber transliterasi", yasin.sumberLatin.isNotBlank())
        assertTrue("Surat Yasin tidak menyebut sumber terjemahan", yasin.sumber.isNotBlank())
    }

    /**
     * Keterangan sumber tingkat modul wajib ada di bundel — sebagian besar bagian tahlil dan
     * seluruh daftar sholat tidak menyebut sumbernya sendiri, jadi keterangan modul inilah satu-
     * satunya yang dapat ditampilkan untuk mereka.
     */
    @Test
    fun keteranganSumberTingkatModulTersedia() {
        assertTrue("tahlil_full.json tidak menyediakan sumber bacaan tingkat modul",
            repo.tahlilSumberBacaan().isNotBlank())
        assertTrue("tahlil.json tidak menyediakan catatan tradisi tingkat modul",
            repo.tahlilCatatanTradisi().isNotBlank())
        assertTrue("sholat_daftar.json tidak menyediakan sumber panduan tingkat modul",
            repo.sholatPanduan().isNotBlank())
    }

    @Test
    fun internetDideklarasikanUntukMurottal() {
        @Suppress("DEPRECATION")
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PERMISSIONS)
        val perms = info.requestedPermissions?.toList().orEmpty()
        // INTERNET memang harus ada di manifes gabungan yang terpasang: tanpanya streaming
        // murottal Yasin gagal senyap di perangkat sungguhan. Batasnya — jaringan HANYA untuk
        // itu, pemakaiannya terkunci di paket core/audio — tidak dapat dibuktikan dari manifes
        // gabungan; itu dijaga uji sumber `OfflineManifestTest.internet_hanya_untuk_murottal_di_paket_audio`.
        assertTrue(
            "INTERNET tidak dideklarasikan — murottal Yasin tidak akan dapat dialirkan",
            perms.contains(Manifest.permission.INTERNET),
        )
        // ACCESS_NETWORK_STATE sengaja tidak diuji di sini: izin itu disuntikkan manifes
        // pustaka androidx.work (bukan dideklarasikan aplikasi), sehingga manifes gabungan
        // selalu memuatnya walau aplikasi sendiri tak pernah memeriksa status jaringan.
    }
}
