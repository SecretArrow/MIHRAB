package id.or.kemenag.yasintahlil

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.or.kemenag.yasintahlil.core.data.FormatCadangan
import id.or.kemenag.yasintahlil.core.data.local.AppDatabase
import id.or.kemenag.yasintahlil.core.data.local.RoomUserDataRepository
import id.or.kemenag.yasintahlil.core.domain.model.RingkasanData
import id.or.kemenag.yasintahlil.core.util.BatasRiwayat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * plan.txt: "Pengujian penyimpanan bookmark dan progres."
 *
 * Menutup lalu membuka kembali database sama artinya dengan aplikasi ditutup dan dibuka ulang.
 * Data harus tetap ada karena disimpan di Room — bukan hanya di memori (state Compose).
 */
@RunWith(AndroidJUnit4::class)
class UserDataPersistenceTest {

    private lateinit var ctx: Context
    private var db: AppDatabase? = null

    private fun openDb(): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, DB_NAME).build().also { db = it }

    /** Tutup database lalu buka lagi — mensimulasikan aplikasi dibuka ulang. */
    private fun restart(): RoomUserDataRepository {
        db?.close()
        return RoomUserDataRepository(openDb())
    }

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        ctx.deleteDatabase(DB_NAME)
        restart()
    }

    @After
    fun tearDown() {
        db?.close()
        ctx.deleteDatabase(DB_NAME)
    }

    @Test
    fun bookmarkBertahanSetelahAplikasiDibukaUlang() {
        runBlocking {
            restart().toggleBookmark("yasin", "58", "Ayat 58")

            val after = restart()
            assertTrue("bookmark hilang setelah dibuka ulang", after.isBookmarked("yasin", "58").first())
            assertEquals(1, after.bookmarks("yasin").first().size)
        }
    }

    @Test
    fun progresBertahanDanDitimpaBukanDitambah() {
        runBlocking {
            val repo = restart()
            repo.setProgress("yasin", "mark", 12)
            repo.setProgress("tahlil", "lazim_indonesia", 3)

            val after = restart()
            assertEquals(12, after.progress("yasin", "mark").first()?.value)
            assertEquals(3, after.progress("tahlil", "lazim_indonesia").first()?.value)

            // Kunci gabungan (kind, refId): memperbarui, bukan menumpuk baris.
            after.setProgress("yasin", "mark", 40)
            assertEquals(40, restart().progress("yasin", "mark").first()?.value)
            assertEquals(1, restart().progressByKind("yasin").first().size)
        }
    }

    @Test
    fun favoritDanRiwayatBertahan() {
        runBlocking {
            val repo = restart()
            repo.toggleFavorite("dzikir", "pagi_petang", "Dzikir pagi & petang")
            repo.pushHistory("yasin", "1", "Yasin ayat 1")

            val after = restart()
            assertEquals(1, after.favorites().first().size)
            assertTrue(after.isFavorite("dzikir", "pagi_petang").first())
            assertTrue(after.recentHistory(BatasRiwayat.DIBACA_RINGKAS).first().any { it.refId == "1" })
        }
    }

    @Test
    fun toggleMembatalkanPilihan() {
        runBlocking {
            val repo = restart()
            repo.toggleBookmark("yasin", "7", "Ayat 7")
            assertTrue(repo.isBookmarked("yasin", "7").first())

            repo.toggleBookmark("yasin", "7", "Ayat 7")
            assertFalse(repo.isBookmarked("yasin", "7").first())
            assertTrue(repo.bookmarks("yasin").first().isEmpty())
        }
    }

    @Test
    fun backupDanPulihkanMengembalikanSeluruhData() {
        runBlocking {
            val repo = restart()
            repo.toggleBookmark("yasin", "58", "Ayat 58")
            repo.toggleFavorite("dzikir", "harian", "Doa harian")
            repo.setProgress("yasin", "mark", 20)
            val backup = repo.exportJson()

            // Pemulihan ke database kosong (mis. setelah pasang ulang aplikasi).
            restart().restoreJson(backup)

            val after = restart()
            assertTrue(after.isBookmarked("yasin", "58").first())
            assertTrue(after.isFavorite("dzikir", "harian").first())
            assertEquals(20, after.progress("yasin", "mark").first()?.value)
        }
    }

    /** Berkas backup yang rusak tidak boleh menghapus data yang sedang tersimpan. */
    @Test
    fun backupRusakTidakMenghapusData() {
        runBlocking {
            val repo = restart()
            repo.toggleBookmark("yasin", "58", "Ayat 58")

            repo.restoreJson("{ bukan json")
            assertTrue("data lama terhapus oleh backup rusak", repo.isBookmarked("yasin", "58").first())
        }
    }

    /**
     * Berkas yang sah tetapi hanya memuat sebagian bagian tak boleh menghapus bagian lain.
     *
     * Dahulu keempat tabel dikosongkan lebih dahulu, baru diisi ulang dari berkas. Berkas yang tak
     * memuat sebuah larik — versi lain, berkas separuh, atau nama kunci yang meleset — karena itu
     * berakhir dengan bagian itu terhapus, tanpa satu pun pesan galat.
     */
    @Test
    fun berkasSebagianTidakMenghapusBagianLain() {
        runBlocking {
            val repo = restart()
            repo.toggleBookmark("yasin", "58", "Ayat 58")
            repo.setProgress("yasin", "mark", 20)
            repo.pushHistory("yasin", "1", "Yasin 1", 1_700_000_000_000)

            // Berkas sah, tetapi hanya memuat larik favorit yang kosong.
            val sebagian = JSONObject()
                .put(FormatCadangan.FAVORIT, JSONArray())
                .toString()
            repo.restoreJson(sebagian)

            val after = restart()
            assertEquals("favorit tidak dikosongkan sesuai berkas", 0, after.ringkasan().favorit)
            assertTrue("bookmark ikut terhapus oleh berkas yang tak memuatnya",
                after.isBookmarked("yasin", "58").first())
            assertEquals("progres ikut terhapus oleh berkas yang tak memuatnya",
                20, after.progress("yasin", "mark").first()?.value)
            assertEquals("riwayat ikut terhapus oleh berkas yang tak memuatnya",
                1, after.ringkasan().riwayat)
        }
    }

    /**
     * Berkas yang hanya memuat preferensi tak boleh mengosongkan data terstruktur.
     *
     * Bentuk inilah yang sampai ke pemulih ketika berkasnya memuat `prefs` tanpa `userData`;
     * sebelum diperbaiki, pemulihannya mengirim objek kosong dan menghapus segalanya.
     */
    @Test
    fun berkasTanpaBagianDataPenggunaTidakMengubahData() {
        runBlocking {
            val repo = restart()
            repo.toggleFavorite("dzikir", "harian", "Doa harian")
            repo.setProgress("yasin", "mark", 7)

            repo.restoreJson("{}")

            val after = restart()
            assertEquals("favorit terhapus oleh berkas yang tak memuat data pengguna",
                1, after.ringkasan().favorit)
            assertEquals(7, after.progress("yasin", "mark").first()?.value)
        }
    }

    /**
     * plan.txt: "Manajemen data offline." Angka yang ditampilkan sebelum menghapus harus benar —
     * termasuk riwayat, yang jumlahnya tidak dibatasi [recentHistory] (yang hanya menampilkan 50).
     */
    @Test
    fun ringkasanMenghitungIsiTiapBagian() {
        runBlocking {
            assertEquals("database baru seharusnya kosong", RingkasanData(), restart().ringkasan())

            val repo = restart()
            repo.toggleFavorite("dzikir", "harian", "Doa harian")
            repo.toggleFavorite("dzikir", "pagi_petang", "Dzikir pagi & petang")
            repo.toggleBookmark("yasin", "58", "Ayat 58")
            repo.pushHistory("yasin", "1", "Yasin ayat 1")
            repo.pushHistory("tahlil", "lazim_indonesia", "Tahlil")
            repo.setProgress("yasin", "mark", 12)
            repo.setProgress("tahlil", "lazim_indonesia", 3)

            val r = restart().ringkasan()
            assertEquals(2, r.favorit)
            assertEquals(1, r.bookmark)
            assertEquals(2, r.riwayat)
            assertEquals(2, r.progres)
        }
    }

    /**
     * `jumlahRiwayat()` menghitung **seluruh** riwayat, bukan jatah baca `recentHistory`. Beranda
     * memakainya untuk "Riwayat: N"; bila angkanya diambil dari daftar yang sudah dipotong jatah
     * bacanya, pengguna dengan lebih banyak entri diberi tahu angka yang salah — dan berbeda dari
     * angka yang sama di layar "Manajemen data offline" yang memakai `ringkasan()`.
     */
    @Test
    fun jumlahRiwayatTidakTerbatasJatahBaca() {
        runBlocking {
            val repo = restart()
            repeat(BatasRiwayat.DIBACA_RINGKAS + 5) { i ->
                repo.pushHistory("yasin", "$i", "Yasin ayat $i")
            }

            val after = restart()
            assertEquals(BatasRiwayat.DIBACA_RINGKAS + 5, after.jumlahRiwayat().first())
            assertEquals("ringkasan() dan jumlahRiwayat() harus menyebut angka yang sama",
                after.jumlahRiwayat().first(), after.ringkasan().riwayat)
            assertEquals("daftar terbaru tetap dipotong jatah bacanya",
                BatasRiwayat.DIBACA_RINGKAS,
                after.recentHistory(BatasRiwayat.DIBACA_RINGKAS).first().size)
        }
    }

    /** Menghapus satu bagian tidak boleh menyentuh bagian lain — inti "manajemen data offline". */
    @Test
    fun hapusRiwayatTidakMenyentuhBagianLain() {
        runBlocking {
            val repo = restart()
            repo.toggleFavorite("dzikir", "harian", "Doa harian")
            repo.toggleBookmark("yasin", "58", "Ayat 58")
            repo.pushHistory("yasin", "1", "Yasin ayat 1")
            repo.setProgress("yasin", "mark", 12)

            repo.clearHistory()

            val after = restart()
            assertEquals(0, after.ringkasan().riwayat)
            assertTrue("riwayat masih terbaca setelah dihapus", after.recentHistory(BatasRiwayat.DIBACA_RINGKAS).first().isEmpty())
            assertEquals(1, after.ringkasan().favorit)
            assertEquals(1, after.ringkasan().bookmark)
            assertEquals(1, after.ringkasan().progres)
            assertEquals(12, after.progress("yasin", "mark").first()?.value)
        }
    }

    /** Favorit, bookmark, dan progres juga punya tombol hapusnya sendiri; semuanya harus nyata. */
    @Test
    fun hapusFavoritBookmarkDanProgresBekerjaDanBertahan() {
        runBlocking {
            val repo = restart()
            repo.toggleFavorite("dzikir", "harian", "Doa harian")
            repo.toggleBookmark("yasin", "58", "Ayat 58")
            repo.setProgress("yasin", "mark", 12)

            repo.clearFavorites()
            repo.clearBookmarks()
            repo.clearProgress()

            // Diverifikasi setelah dibuka ulang: penghapusan tersimpan, bukan hanya di memori.
            val after = restart()
            assertEquals(RingkasanData(), after.ringkasan())
            assertTrue(after.favorites().first().isEmpty())
            assertTrue(after.bookmarks("yasin").first().isEmpty())
            assertTrue(after.progressByKind("yasin").first().isEmpty())
        }
    }

    private companion object {
        const val DB_NAME = "persistence_test.db"
    }
}
