package id.or.kemenag.yasintahlil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `docs/PERMISSIONS.md` dan `AndroidManifest.xml` sama-sama menjanjikan aturan *"izin hanya diminta
 * bila benar-benar dipakai"*. Aturan itu dulu **tidak pernah diperiksa secara umum**: satu-satunya
 * uji izin, `OfflineManifestTest.izin_alarm_tepat_waktu_sejalan_dengan_pemakaian`, hanya mengawasi
 * pasangan `AlarmManager` ↔ `SCHEDULE_EXACT_ALARM`. Akibatnya `WAKE_LOCK` duduk di manifes tanpa
 * satu pun pemakaian di kode aplikasi, dan tak ada apa pun yang memberitahu — dokumennya bahkan
 * menunjuk uji alarm sebagai penjaganya.
 *
 * Uji ini membuat aturan itu nyata: setiap izin yang **dideklarasikan** harus punya penanda
 * pemakaian di `src/main` (kode aplikasi sendiri). Izin yang memang milik pustaka — dibawa ke
 * manifes gabungan oleh manifes pustaka itu sendiri — sengaja tidak dideklarasikan di sini,
 * sehingga tidak perlu masuk tabel penanda.
 *
 * Tabelnya sengaja **dua arah**: izin tanpa penanda gagal, dan penanda untuk izin yang tak lagi
 * dideklarasikan juga gagal — tabel yang basi menyesatkan sama seperti tabel yang kosong.
 */
class PemakaianIzinTest {

    private fun modul(): File =
        listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun manifest(): String = File(modul(), "src/main/AndroidManifest.xml").readText()

    /** Gabungan seluruh sumber Kotlin `main` (kode produksi, bukan uji). */
    private fun sumberUtama(): String = File(modul(), "src/main")
        .walkTopDown().filter { it.isFile && it.extension == "kt" }
        .joinToString("\n") { it.readText() }

    /**
     * Izin → penanda yang membuktikan pemakaiannya di kode aplikasi.
     *
     * `FOREGROUND_SERVICE` dan `FOREGROUND_SERVICE_MEDIA_PLAYBACK` berbagi satu penanda: keduanya
     * dibutuhkan satu layanan latar yang sama, yang tipe-nya dinyatakan lewat
     * `ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK` di `ReminderWorker`. `RECEIVE_BOOT_COMPLETED`
     * dipakai lewat aksi siarannya (`Intent.ACTION_BOOT_COMPLETED` di `BootReceiver`), bukan lewat
     * nama izinnya.
     */
    private val penanda: Map<String, List<String>> = mapOf(
        "ACCESS_FINE_LOCATION" to listOf("ACCESS_FINE_LOCATION"),
        "ACCESS_COARSE_LOCATION" to listOf("ACCESS_COARSE_LOCATION"),
        "POST_NOTIFICATIONS" to listOf("POST_NOTIFICATIONS"),
        "RECEIVE_BOOT_COMPLETED" to listOf("ACTION_BOOT_COMPLETED"),
        "FOREGROUND_SERVICE" to listOf("FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK"),
        "FOREGROUND_SERVICE_MEDIA_PLAYBACK" to listOf("FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK"),
        "VIBRATE" to listOf("VIBRATOR_SERVICE"),
        "ACCESS_NOTIFICATION_POLICY" to listOf("NOTIFICATION_POLICY_ACCESS_SETTINGS"),
    )

    private fun dideklarasikan(): Set<String> =
        Regex("<uses-permission\\s+android:name=\"android\\.permission\\.([A-Z_]+)\"")
            .findAll(manifest()).map { it.groupValues[1] }.toSet()

    @Test
    fun setiapIzinYangDideklarasikanTerbuktiDipakaiDiKode() {
        val izin = dideklarasikan()
        assertTrue("nama izin tidak terbaca dari manifes", izin.size >= 6)
        val teks = sumberUtama()
        val tanpaPemakaian = izin.filter { nama ->
            val penandaIzin = penanda[nama] ?: return@filter true
            penandaIzin.none { teks.contains(it) }
        }
        assertTrue(
            "izin ini dideklarasikan di manifes tetapi tak ada pemakaiannya di kode aplikasi — " +
                "aplikasi meminta akses yang tak pernah dipakai: $tanpaPemakaian",
            tanpaPemakaian.isEmpty(),
        )
    }

    @Test
    fun tabelPenandaTidakBasi() {
        val basi = penanda.keys - dideklarasikan()
        assertTrue(
            "tabel penanda menyebut izin yang tak lagi dideklarasikan — tabelnya jadi tak bermakna: $basi",
            basi.isEmpty(),
        )
    }

    /**
     * `WAKE_LOCK` dulu dideklarasikan di manifes tanpa satu pun `PowerManager`/`newWakeLock` di kode
     * aplikasi. Pustaka yang membutuhkannya membawanya sendiri ke manifes gabungan — WorkManager
     * mencantumkan `WAKE_LOCK` di manifesnya — jadi aplikasi tak perlu ikut meminta. Uji ini menahan
     * agar izin itu tidak kembali tanpa pemakaian.
     */
    @Test
    fun izinWakeLockTidakDimintaTanpaPemakaian() {
        assertFalse(
            "manifes meminta WAKE_LOCK padahal kode aplikasi tak memakai PowerManager/WakeLock",
            dideklarasikan().contains("WAKE_LOCK"),
        )
        val kode = sumberUtama()
        assertFalse("kode aplikasi mulai memakai PowerManager", kode.contains("PowerManager"))
        assertFalse("kode aplikasi mulai memakai newWakeLock", kode.contains("newWakeLock"))
    }
}
