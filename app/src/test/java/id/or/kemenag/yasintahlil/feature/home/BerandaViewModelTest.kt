package id.or.kemenag.yasintahlil.feature.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Beranda Utama" meminta dasbor yang informatif, dan §"Teknologi dan Arsitektur"
 * meminta *"ViewModel dan StateFlow"* serta *"state management yang terstruktur"*.
 *
 * Sebelumnya beranda adalah satu-satunya layar inti tanpa ViewModel: belasan
 * `remember { mutableStateOf(...) }` diisi sekali lewat satu `LaunchedEffect(Unit)`, sehingga nilai
 * Store tidak reaktif (mengubah lokasi/metode di layar Jadwal baru terlihat setelah beranda dibuka
 * ulang) dan seluruh keadaan hilang saat rotasi. Uji ini menjaga agar keadaan beranda tetap berada
 * di [HomeViewModel] sebagai aliran, bukan kembali ke `remember` di layar.
 *
 * `HomeViewModel` tidak dapat dijalankan di JVM (butuh `Context`/DataStore), jadi invariannya
 * diperiksa dari sumbernya — sama seperti `JadwalLokasiTest`.
 */
class BerandaViewModelTest {

    private fun sumber(relatif: String): String {
        val root = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }
        return File(root, relatif).readText()
    }

    private fun vm(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/home/HomeViewModel.kt")

    private fun layar(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/home/HomeScreen.kt")

    @Test
    fun berandaPunyaViewModelBerbasisAliran() {
        val s = vm()
        assertTrue("HomeViewModel tidak di-inject Hilt", s.contains("@HiltViewModel"))
        assertTrue("keadaan beranda bukan StateFlow", s.contains("StateFlow<HomeUiState>"))
        assertTrue("preferensi Store tidak dibaca sebagai aliran",
            s.contains("store.flow("))
        // Ringkasan favorit/riwayat harus ikut aliran Room, bukan dibaca sekali.
        assertTrue("favorit tidak dibaca sebagai aliran", s.contains("favorites()"))
        assertTrue("riwayat tidak dibaca sebagai aliran", s.contains("recentHistory("))
    }

    /** Hitung mundur tetap hidup dari detak di ViewModel, bukan `remember` di layar. */
    @Test
    fun hitungMundurDihidupkanDariViewModel() {
        val s = vm()
        assertTrue("ViewModel tidak mendetak jam", s.contains("delay(1_000)"))
        assertTrue("ViewModel tidak menyimpan waktu berjalan", s.contains("nowMillis"))
    }

    /** Saklar pengingat harus menjadwalkan ulang pekerja latar — dulu dilakukan di layar. */
    @Test
    fun pengingatDijadwalkanUlangDariViewModel() {
        val s = vm()
        assertTrue("ViewModel tidak menulis Store.NOTIF", s.contains("store.set(Store.NOTIF"))
        assertTrue("ViewModel tidak menjadwalkan ulang pengingat",
            s.contains("PrayerScheduler.scheduleAll"))
    }

    @Test
    fun layarBerandaMemakaiViewModel() {
        val s = layar()
        assertTrue("beranda tidak mengambil ViewModel", s.contains("hiltViewModel()"))
        assertTrue("beranda tidak mengumpulkan keadaan ViewModel",
            s.contains("vm.state.collectAsStateWithLifecycle()"))
        assertTrue("saklar pengingat tidak memanggil ViewModel", s.contains("vm.setNotif("))
    }

    /** Tak ada lagi keadaan Store yang disimpan di layar dengan `remember`. */
    @Test
    fun layarTidakLagiMenyimpanKeadaanStoreDenganRemember() {
        val s = layar()
        assertFalse("beranda masih menyimpan keadaan dengan mutableStateOf",
            s.contains("mutableStateOf("))
        assertFalse("beranda masih membaca Store langsung", s.contains("rememberStore()"))
        assertFalse("beranda masih membaca data pengguna langsung", s.contains("rememberUserData()"))
    }
}
