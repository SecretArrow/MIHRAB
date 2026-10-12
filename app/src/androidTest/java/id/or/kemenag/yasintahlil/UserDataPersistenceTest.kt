package id.or.kemenag.yasintahlil

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import id.or.kemenag.yasintahlil.core.data.local.AppDatabase
import id.or.kemenag.yasintahlil.core.data.local.RoomUserDataRepository
import id.or.kemenag.yasintahlil.core.domain.model.RingkasanData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
            assertTrue(after.recentHistory().first().any { it.refId == "1" })
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
            assertTrue("riwayat masih terbaca setelah dihapus", after.recentHistory().first().isEmpty())
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
