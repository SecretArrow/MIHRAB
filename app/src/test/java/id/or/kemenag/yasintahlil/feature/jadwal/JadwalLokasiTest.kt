package id.or.kemenag.yasintahlil.feature.jadwal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Jadwal Sholat dan Pengaturan Adzan" meminta *"Pembaruan jadwal saat lokasi
 * berubah"*.
 *
 * Sebelumnya layar Jadwal hanya membaca satu perbaikan terakhir (`LocationProvider.lastKnown`)
 * lalu diam: jadwal tetap memakai koordinat tempat pengguna pertama kali menekan tombol GPS,
 * sampai ia menekannya lagi. Layar Kiblat sudah mengikuti aliran lokasi live; uji ini menjaga
 * agar Jadwal tidak kembali tertinggal.
 *
 * `JadwalViewModel` tidak dapat dijalankan di JVM (butuh `Context`), jadi invariannya diperiksa
 * dari sumbernya — sama seperti `RujukanTest.contentRepMemakaiPenyelesaiBersama`.
 */
class JadwalLokasiTest {

    private fun sumber(relatif: String): String {
        val root = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }
        return File(root, relatif).readText()
    }

    private fun vm(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/jadwal/JadwalViewModel.kt")

    private fun layar(): String =
        sumber("src/main/java/id/or/kemenag/yasintahlil/feature/jadwal/JadwalScreen.kt")

    /** Isi `fun <nama>(...) { ... }` — untuk memastikan perubahan terjadi di fungsi yang benar. */
    private fun badanFungsi(src: String, nama: String): String {
        val i = src.indexOf("fun $nama(")
        if (i < 0) return ""
        val start = src.indexOf('{', i)
        if (start < 0) return ""
        var depth = 0
        for (j in start until src.length) {
            when (src[j]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return src.substring(start, j + 1)
                }
            }
        }
        return src.substring(start)
    }

    @Test
    fun jadwalMengikutiAliranLokasiLive() {
        val s = vm()
        assertTrue("JadwalViewModel tidak mengikuti LocationProvider.updates() — jadwal tak ikut berpindah",
            s.contains("LocationProvider.updates(ctx)"))
    }

    @Test
    fun adaSaklarIkutiLokasiYangDisimpan() {
        val s = vm()
        assertTrue("JadwalViewModel tidak punya setFollowGps()", s.contains("fun setFollowGps("))
        assertTrue("pilihan mengikuti lokasi tidak disimpan", s.contains("store.set(Store.JADWAL_IKUT"))
        assertTrue("pilihan mengikuti lokasi tidak dipulihkan saat layar dibuka",
            s.contains("store.get(Store.JADWAL_IKUT"))
    }

    /**
     * Kunci preferensi harus terpisah dari `USE_GPS` (yang berarti "koordinat berasal dari GPS" dan
     * dipakai bersama layar Kiblat). Satu kunci untuk dua arti membuat satu saklar mengubah dua hal.
     */
    @Test
    fun saklarIkutiTidakMenumpangKunciUseGps() {
        assertFalse(
            "JadwalViewModel membaca Store.USE_GPS untuk arti 'mengikuti perubahan lokasi' — " +
                "kunci itu berarti 'koordinat berasal dari GPS' dan dipakai bersama layar Kiblat",
            vm().contains("store.get(Store.USE_GPS"),
        )
    }

    /** Kunci baru wajib ikut backup/pemulihan, jika tidak pilihannya hilang saat backup dipulihkan. */
    @Test
    fun pilihanIkutLokasiIkutTerBackup() {
        val store = sumber("src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt")
        val impor = store.substringAfter("fun importJson")
        assertTrue("Store.JADWAL_IKUT tidak dideklarasikan", store.contains("JADWAL_IKUT = booleanPreferencesKey"))
        assertTrue("Store.JADWAL_IKUT tidak ikut importJson (hilang saat backup dipulihkan)",
            impor.contains("bool(JADWAL_IKUT)"))
    }

    /** Goyangan GPS beberapa meter saat perangkat diam tidak boleh memicu hitung ulang. */
    @Test
    fun hanyaHitungUlangBilaBenarBenarBerpindah() {
        val s = vm()
        assertTrue("tidak ada ambang jarak sebelum hitung ulang", s.contains("AMBANG_PINDAH_METER"))
        assertTrue("tidak ada perhitungan jarak antar koordinat", s.contains("distanceBetween("))
    }

    /**
     * Daftar kota yang gagal dibaca tidak boleh menjadi daftar kosong tanpa keterangan.
     *
     * Bundel konten dapat diperbarui pengguna (layar "Pembaruan konten"). Bila `repo.kota()` gagal,
     * dahulu kegagalannya ditelan (`getOrDefault(emptyList())`) sehingga tombol "Kota: …" membuka
     * daftar kosong tanpa satu pun penjelasan — pengguna menyimpulkan tombolnya rusak, padahal
     * bundelnya yang bermasalah. Sebabnya kini dibawa ke layar, seperti `gagalMuat` untuk preferensi.
     */
    @Test
    fun daftarKotaKosongDiberiKeterangan() {
        val s = vm()
        assertTrue("sebab daftar kota kosong tidak dibawa ke layar", s.contains("kotaGagal"))
        assertFalse("kegagalan membaca daftar kota masih ditelan tanpa keterangan",
            s.contains("runCatching { repo.kota() }.getOrDefault(emptyList())"))
        assertTrue("layar tidak menampilkan keterangan saat daftar kota kosong",
            layar().contains("vm.kotaGagal"))
    }

    /** Memilih kota manual harus mengalahkan GPS — bukan ditimpa perbaikan berikutnya. */
    @Test
    fun memilihKotaMenghentikanIkutanGps() {
        val isi = badanFungsi(vm(), "selectCity")
        assertTrue("selectCity tidak menghentikan ikutan GPS: $isi", isi.contains("hentikanIkutiLokasi"))
    }

    @Test
    fun pekerjaanLokasiDibatalkanAgarTidakBocor() {
        val isi = badanFungsi(vm(), "hentikanIkutiLokasi")
        assertTrue("hentikanIkutiLokasi tidak membatalkan pekerjaan lokasi", isi.contains(".cancel()"))
    }

    @Test
    fun layarMenawarkanSaklarIkutiPerubahanLokasi() {
        val s = layar()
        assertTrue("layar Jadwal tidak punya saklar mengikuti lokasi", s.contains("setFollowGps("))
        assertTrue("layar Jadwal tidak menjelaskan saklarnya",
            s.contains("Ikuti perubahan lokasi"))
        assertFalse("layar Jadwal memanggil lastKnown langsung alih-alih lewat ViewModel",
            s.contains("LocationProvider.lastKnown"))
    }
}
