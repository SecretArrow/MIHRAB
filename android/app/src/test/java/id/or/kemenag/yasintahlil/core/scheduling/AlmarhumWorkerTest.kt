package id.or.kemenag.yasintahlil.core.scheduling

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Penjaga gaya parser untuk pengingat tahlil almarhum — pola `LapisanAudioTest`: beberapa
 * keputusan di sini tetap sah bagi kompilator meski dicerabut, dan kegagalannya senyap
 * (notifikasi yang tak pernah tampil, jadwal yang tertukar, database yang tiba-tiba jadi dua),
 * sehingga ia dijaga dengan memeriksa isi berkasnya langsung.
 */
class AlmarhumWorkerTest {

    private fun kode(relatif: String): String = KodeSumber.kodeTanpaKomentar(
        KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    /**
     * DAO diambil lewat EntryPoint Hilt dari grafik singleton. WorkManager membangun pekerjanya
     * sendiri; jalur pintas yang paling menggoda — membangun Room sendiri di dalam pekerja —
     * menjadikan instance database dua dan salinan catatan yang tidak terlihat satu sama lain.
     */
    @Test
    fun daoDiambilLewatEntryPointBukanMembangunRoomSendiri() {
        val k = kode("core/scheduling/AlmarhumWorker.kt")
        assertTrue("AlmarhumWorker tidak lagi memakai EntryPointAccessors",
            k.contains("EntryPointAccessors.fromApplication("))
        assertFalse("AlmarhumWorker membangun Room sendiri — instance database bisa menjadi dua",
            k.contains("Room.databaseBuilder"))
    }

    /** Ketukan notifikasi membuka layar Tahlil Almarhum lewat extra rute — pola `ReminderWorker`. */
    @Test
    fun notifikasiMembukaLayarAlmarhumLewatExtraRute() {
        val k = kode("core/scheduling/AlmarhumWorker.kt")
        assertTrue("notifikasi tidak lagi menyasar Routes.ALMARHUM", k.contains("Routes.ALMARHUM"))
        assertTrue("notifikasi tidak lagi memakai MainActivity.EXTRA_ROUTE",
            k.contains("MainActivity.EXTRA_ROUTE"))
    }

    /**
     * Kanal ALMARHUM harus dibuat di `NotifChannels.ensure()`: di Android 8+ notifikasi ke kanal
     * yang belum terdaftar tidak pernah tampil — dan kegagalannya pun tanpa satu pun galat.
     */
    @Test
    fun kanalAlmarhumTerdaftarDiEnsure() {
        val k = kode("core/scheduling/NotifChannels.kt")
        assertTrue("kanal ALMARHUM tidak lagi dibuat di ensure()",
            k.contains("NotificationChannel(ALMARHUM,"))
        assertTrue("kanal ALMARHUM tidak terdaftar di createNotificationChannels()",
            Regex("""listOf\([^)]*almarhum""").containsMatchIn(k))
    }

    /**
     * Penjadwalan periodik dari ViewModel memakai nama unik + KEEP — idempoten terhadap layar
     * yang dibuka berulang. Berganti kebijakan lain (REPLACE/CANCEL) hanya menggeser jadwalnya
     * setiap kali layarnya dibuka.
     */
    @Test
    fun penjadwalanPeriodikIdempotenDariViewModel() {
        val k = kode("feature/almarhum/AlmarhumViewModel.kt")
        assertTrue("penjadwalan tidak lagi memakai ExistingPeriodicWorkPolicy.KEEP",
            k.contains("ExistingPeriodicWorkPolicy.KEEP"))
        assertTrue("penjadwalan tidak lagi menunjuk AlmarhumWorker",
            k.contains("PeriodicWorkRequestBuilder<AlmarhumWorker>"))
    }
}
