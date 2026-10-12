package id.or.kemenag.yasintahlil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Teknologi dan Arsitektur" meminta *"arsitektur modular yang memisahkan UI, domain,
 * data, penjadwalan, audio, lokasi, dan sensor"*.
 *
 * Sebelumnya `AdzanPlayer` tinggal di paket `core.scheduling` dan layar Adzan membuat `ExoPlayer`
 * sendiri. Akibatnya pemutaran audio tersebar: perubahan lisensi berkas adzan menyentuh kode
 * penjadwalan, dan lapisan UI memegang pemutar media. Uji ini menjaga agar lapisan `core.audio`
 * tetap menjadi satu-satunya tempat Media3 dipakai.
 */
class LapisanAudioTest {

    private fun mainDir(): File =
        listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }
            .let { File(it, "src/main/java/id/or/kemenag/yasintahlil") }

    private fun berkasKotlin(dir: File): List<File> =
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun isi(f: File) = f.readText()

    @Test
    fun lapisanAudioAdaDanBerisiPemutar() {
        val dir = File(mainDir(), "core/audio")
        assertTrue("paket core/audio tidak ada", dir.isDirectory)
        val nama = berkasKotlin(dir).map { it.name }.toSet()
        assertTrue("AdzanPlayer tidak ada di core/audio: $nama", "AdzanPlayer.kt" in nama)
        assertTrue("AdzanPreview tidak ada di core/audio: $nama", "AdzanPreview.kt" in nama)
        berkasKotlin(dir).forEach {
            assertTrue("${it.name} tidak menyatakan paket core.audio",
                isi(it).contains("package id.or.kemenag.yasintahlil.core.audio"))
        }
    }

    /** Pemutar tidak boleh tinggal lagi di paket penjadwalan. */
    @Test
    fun penjadwalanTidakLagiMemuatPemutarAudio() {
        val scheduling = File(mainDir(), "core/scheduling")
        val nama = berkasKotlin(scheduling).map { it.name }
        assertFalse("AdzanPlayer masih ada di core/scheduling: $nama", "AdzanPlayer.kt" in nama)
    }

    /** Lapisan UI tidak boleh memakai Media3 langsung — cukup lewat `core.audio`. */
    @Test
    fun lapisanUiTidakMemakaiMedia3Langsung() {
        val pelanggar = berkasKotlin(File(mainDir(), "feature"))
            .filter { isi(it).contains("androidx.media3") }
            .map { it.name }
        assertTrue("layar memakai Media3 langsung, bukan lewat core.audio: $pelanggar", pelanggar.isEmpty())
    }

    @Test
    fun pekerjaPengingatMemakaiPemutarDariLapisanAudio() {
        val worker = File(mainDir(), "core/data/ReminderWorker.kt")
        assertTrue("ReminderWorker tidak memakai core.audio.AdzanPlayer",
            isi(worker).contains("id.or.kemenag.yasintahlil.core.audio.AdzanPlayer"))
    }
}
