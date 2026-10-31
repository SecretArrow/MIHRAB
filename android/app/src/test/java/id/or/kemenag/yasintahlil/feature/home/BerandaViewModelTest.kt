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
 * Beranda kini juga ringkas: kartu saklar pengingat dan ringkasan favorit/riwayat sudah pulang ke
 * rumahnya masing-masing (saklar ke tab Pengaturan — dijaga `PengaturanTerpusatTest`; angka
 * simpanan ke layar Favorit & Riwayat), sehingga ViewModel-nya pun tak lagi membawa keadaan yang
 * tak dipakai layarnya sendiri.
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
    }

    /** Hitung mundur tetap hidup dari detak di ViewModel, bukan `remember` di layar. */
    @Test
    fun hitungMundurDihidupkanDariViewModel() {
        val s = vm()
        assertTrue("ViewModel tidak mendetak jam", s.contains("delay(1_000)"))
        assertTrue("ViewModel tidak menyimpan waktu berjalan", s.contains("nowMillis"))
    }

    /**
     * Saklar pengingat sudah pulang ke tab Pengaturan: beranda tak lagi membawanya, dan
     * ViewModel-nya tak lagi menyimpan pengaturan yang tak bisa diubah dari layarnya. Penjadwalan
     * ulang saat saklar itu ditekan dijaga di rumah barunya (`PengaturanTerpusatTest` +
     * `PengingatAdzanTest`).
     */
    @Test
    fun berandaTidakLagiMembawaPengaturanPengingat() {
        val s = vm()
        assertFalse("ViewModel masih menulis Store.NOTIF — kendalinya milik Pengaturan",
            s.contains("store.set(Store.NOTIF"))
        assertFalse("ViewModel masih membawa penjadwalan pengingat",
            s.contains("PrayerScheduler"))
    }

    @Test
    fun layarBerandaMemakaiViewModel() {
        val s = layar()
        assertTrue("beranda tidak mengambil ViewModel", s.contains("hiltViewModel()"))
        assertTrue("beranda tidak mengumpulkan keadaan ViewModel",
            s.contains("vm.state.collectAsStateWithLifecycle()"))
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

    /**
     * `HomeScreen` punya dua parameter, dan yang **terakhir** adalah `vm` — bukan `go`. Lambda
     * penutup (`HomeScreen { … }`) karena itu mengikat ke `vm` dan gagal dikompilasi. Panggilannya
     * harus memakai argumen bernama, seperti `FavoritScreen`/`SearchScreen` yang sudah begitu.
     */
    @Test
    fun navigasiMemanggilBerandaDenganArgumenBernama() {
        val nav = sumber("src/main/java/id/or/kemenag/yasintahlil/navigation/AppNav.kt")
        assertTrue("panggilan HomeScreen tidak memakai go = (lambda penutup akan mengikat ke vm)",
            nav.contains("HomeScreen(go ="))
    }
}
