package id.or.kemenag.yasintahlil

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.scheduling.BootReceiver
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * plan.txt: "Pengujian penjadwalan pengingat dan perilaku aplikasi di latar belakang."
 *
 * Memeriksa jumlah pekerjaan yang benar-benar diantrekan WorkManager untuk tiap kombinasi
 * saklar pengingat — bukan sekadar memanggil fungsi lalu berasumsi ia bekerja.
 */
@RunWith(AndroidJUnit4::class)
class ReminderSchedulingTest {

    private lateinit var ctx: Context
    private val store: Store get() = Store(ctx)

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx)
        reset()
    }

    @After
    fun tearDown() = reset()

    private fun reset() = runBlocking {
        store.set(Store.NOTIF, true)
        store.set(Store.ADZAN_ON, "1,1,1,1,1")
        store.set(Store.ADZAN_PRE_MIN, 0)
        store.set(Store.DOA_ADZAN_REMINDER, false)
        store.set(Store.IQOMAH_ON, false)
    }

    /** Pekerjaan yang belum selesai/dibatalkan — pekerjaan CANCELLED tetap tersimpan di database. */
    private fun pending(): Int =
        WorkManager.getInstance(ctx).getWorkInfosByTag(PrayerScheduler.TAG).get()
            .count { !it.state.isFinished }

    @Test
    fun semuaWaktuAktifMenjadwalkanLimaPengingat() {
        runBlocking {
            store.set(Store.ADZAN_ON, "1,1,1,1,1")
            PrayerScheduler.scheduleAll(ctx)
        }
        assertEquals(5, pending())
    }

    @Test
    fun waktuYangDimatikanTidakDijadwalkan() {
        runBlocking {
            store.set(Store.ADZAN_ON, "1,0,1,0,1")
            PrayerScheduler.scheduleAll(ctx)
        }
        assertEquals("hanya Subuh, Asar, Isya yang aktif", 3, pending())
    }

    @Test
    fun pengingatSebelumAdzanMenambahSatuJadwalPerWaktu() {
        runBlocking {
            store.set(Store.ADZAN_PRE_MIN, 10)
            PrayerScheduler.scheduleAll(ctx)
        }
        assertEquals("5 adzan + 5 pra-adzan", 10, pending())
    }

    @Test
    fun mematikanPengingatMembatalkanSeluruhJadwal() {
        runBlocking {
            PrayerScheduler.scheduleAll(ctx)
            assertTrue("pengingat aktif seharusnya punya jadwal", pending() > 0)

            store.set(Store.NOTIF, false)
            PrayerScheduler.scheduleAll(ctx)
        }
        assertEquals("pengingat nonaktif harus membatalkan seluruh pekerjaan", 0, pending())
    }

    @Test
    fun pengingatDoaSetelahAdzanMenambahSatuJadwalPerWaktu() {
        runBlocking {
            store.set(Store.DOA_ADZAN_REMINDER, true)
            PrayerScheduler.scheduleAll(ctx)
        }
        assertEquals("5 adzan + 5 pengingat doa", 10, pending())
    }

    @Test
    fun mematikanPengingatDoaMenghapusJadwalnya() {
        runBlocking {
            store.set(Store.DOA_ADZAN_REMINDER, true)
            PrayerScheduler.scheduleAll(ctx)
            assertEquals(10, pending())

            store.set(Store.DOA_ADZAN_REMINDER, false)
            PrayerScheduler.scheduleAll(ctx)
        }
        assertEquals("kembali ke 5 jadwal adzan saja", 5, pending())
    }

    /** plan.txt: "Tambahkan pengatur waktu Iqomah opsional setelah pengingat masuk waktu sholat." */
    @Test
    fun pengingatIqomahMenambahSatuJadwalPerWaktu() {
        runBlocking {
            store.set(Store.IQOMAH_ON, true)
            PrayerScheduler.scheduleAll(ctx)
        }
        assertEquals("5 adzan + 5 pengingat iqomah", 10, pending())
    }

    @Test
    fun mematikanPengingatIqomahMenghapusJadwalnya() {
        runBlocking {
            store.set(Store.IQOMAH_ON, true)
            PrayerScheduler.scheduleAll(ctx)
            assertEquals(10, pending())

            store.set(Store.IQOMAH_ON, false)
            PrayerScheduler.scheduleAll(ctx)
        }
        assertEquals("kembali ke 5 jadwal adzan saja", 5, pending())
    }

    /** Ketiga saklar bersamaan harus menumpuk, bukan saling menggantikan. */
    @Test
    fun doaDanIqomahBersamaTetapSepasangPerWaktu() {
        runBlocking {
            store.set(Store.DOA_ADZAN_REMINDER, true)
            store.set(Store.IQOMAH_ON, true)
            store.set(Store.ADZAN_PRE_MIN, 10)
            PrayerScheduler.scheduleAll(ctx)
        }
        assertEquals("5 adzan + 5 pra + 5 doa + 5 iqomah", 20, pending())
    }

    /** Waktu yang dimatikan tidak boleh ikut mengingatkan iqomah. */
    @Test
    fun iqomahMengikutiSaklarPerWaktu() {
        runBlocking {
            store.set(Store.ADZAN_ON, "1,0,1,0,1")
            store.set(Store.IQOMAH_ON, true)
            PrayerScheduler.scheduleAll(ctx)
        }
        assertEquals("3 waktu aktif × (adzan + iqomah)", 6, pending())
    }

    @Test
    fun bootReceiverTerdaftarDiManifes() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED).setPackage(ctx.packageName)
        val receivers = ctx.packageManager.queryBroadcastReceivers(intent, 0)
        assertTrue(
            "BootReceiver tidak terdaftar — pengingat hilang setelah reboot",
            receivers.any { it.activityInfo?.name == BootReceiver::class.java.name },
        )
    }
}
