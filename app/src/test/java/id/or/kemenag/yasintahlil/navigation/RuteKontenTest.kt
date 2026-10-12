package id.or.kemenag.yasintahlil.navigation

import id.or.kemenag.yasintahlil.KodeSumber
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tombol "Buka" pada Favorit & Riwayat harus membuka layar yang **memuat** isinya.
 *
 * Sebelumnya pemetaannya ada di dalam `FavoritScreen` dan ditutup `else -> HOME`, sehingga
 * favorit dzikir (yang `kind`-nya adalah id kelompok, bukan `"dzikir"`) membuka Beranda, doa adzan
 * (`kind = "doa"`) membuka Dzikir, iqomah membuka layar Adzan, dan baris riwayat pencarian
 * (`kind = "search"`) juga membuka Beranda. Pengguna menyimpulkan isinya hilang.
 *
 * Jenis yang diperiksa di sini diambil dari **id yang benar-benar ditulis** `BacaanCard` pada
 * bundel konten, bukan dari daftar karangan.
 */
class RuteKontenTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }


    private fun berkasDzikir(): File =
        File(root(), "src/main/assets/content/dzikir.json")

    /** Id kelompok dzikir pada bundel konten — sumber kebenaran pemetaan, bukan daftar tetap. */
    private fun kelompokDzikir(): List<String> {
        val arr = JSONObject(berkasDzikir().readText()).getJSONArray("kelompok")
        return (0 until arr.length()).map { arr.getJSONObject(it).getString("id") }
    }

    /**
     * Setiap `kind` yang benar-benar ditulis aplikasi harus mengarah ke layar yang memuatnya.
     * Daftar ini mengikuti `BacaanCard(...)` dan `pushHistory("search", …)`.
     */
    @Test
    fun setiapJenisKontenTersimpanMengarahKeLayarYangMemuatnya() {
        val harapan = mapOf(
            "yasin" to Routes.YASIN,
            "tahlil" to Routes.TAHLIL,
            "bacaan" to Routes.SHOLAT,
            "niat" to Routes.SHOLAT,
            "langkah" to Routes.SHOLAT,
            "ghaib" to Routes.GHAIB,
            "jenazah" to Routes.JENAZAH,
            "adzan" to Routes.ADZAN,
            "iqomah" to Routes.IQOMAH,
            // Lafaz pada layar Doa Adzan memakai kind "doa" — bukan layar Dzikir.
            "doa" to Routes.DOA_ADZAN,
            "search" to Routes.SEARCH,
        )
        val salah = harapan.filterKeys { RuteKonten.rute(it) != harapan[it] }
        assertTrue("jenis konten tersimpan menuju layar yang salah: $salah", salah.isEmpty())
    }

    /**
     * Kelompok dzikir memakai id-nya sendiri sebagai `kind` (mis. `pagi_petang`). Setiap id pada
     * bundel konten harus dikenali — dan daftarnya harus berasal dari bundel, bukan ditulis tetap.
     */
    @Test
    fun kelompokDzikirPadaBundelKontenDikenali() {
        val ids = kelompokDzikir()
        assertTrue("bundel dzikir tidak punya kelompok", ids.size >= 4)
        ids.forEach { id ->
            assertEquals("kelompok dzikir '$id' tidak menuju layar Dzikir",
                Routes.DZIKIR, RuteKonten.rute(id, ids.toSet()))
            assertEquals("nama tujuan '$id' tidak menyebut layar Dzikir",
                "Dzikir & Doa", RuteKonten.namaTujuan(id, ids.toSet()))
        }
    }

    /**
     * Pemetaan tidak boleh menulis tetap id kelompok dzikir: kelompok yang ditambahkan bundel
     * setelah rilis harus langsung dikenali tanpa mengubah kode.
     */
    @Test
    fun idKelompokDzikirTidakDitulisTetap() {
        assertEquals("id kelompok baru tidak dikenali",
            Routes.DZIKIR, RuteKonten.rute("kelompok_uji_9f3", setOf("kelompok_uji_9f3")))
        assertNull("id kelompok dzikir tampaknya ditulis tetap di pemetaan",
            RuteKonten.rute(kelompokDzikir().first(), emptySet()))
    }

    /** Jenis tak dikenal tidak boleh diam-diam menuju layar lain. */
    @Test
    fun jenisTakDikenalTidakMenyesatkanKeLayarLain() {
        listOf("", ":", "jawab", "tawasul", "entah", "yasin_full").forEach { kind ->
            assertNull("kind '$kind' seharusnya tidak punya tujuan", RuteKonten.rute(kind))
            assertNull("nama tujuan '$kind' seharusnya kosong", RuteKonten.namaTujuan(kind))
        }
    }

    /** Setiap rute yang dikembalikan harus punya nama layar — tombol tanpa keterangan tujuan itu menyesatkan. */
    @Test
    fun setiapRutePunyaNamaTujuan() {
        val kinds = listOf(
            "yasin", "tahlil", "bacaan", "niat", "langkah", "ghaib", "jenazah",
            "adzan", "iqomah", "doa", "search",
        ) + kelompokDzikir()
        val tanpaNama = kinds.filter { RuteKonten.namaTujuan(it, kinds.toSet()) == null }
        assertTrue("rute tanpa nama tujuan: $tanpaNama", tanpaNama.isEmpty())
    }

    /**
     * Nilai `Store.LAST` berformat `kind:ref`. Beranda dahulu memutuskan tujuannya sendiri dengan
     * `if (last.startsWith("yasin")) YASIN else FAVORIT`, sehingga setiap bacaan **Tahlil** terakhir
     * membuka layar Favorit. Karena `Store.LAST` hanya pernah berisi `yasin:…` dan `tahlil:…`,
     * pemeriksaan di sini memakai kedua bentuk yang benar-benar ditulis aplikasi itu.
     */
    @Test
    fun ruteTerakhirMengikutiJenisnyaBukanMenebak() {
        assertEquals(Routes.YASIN, RuteKonten.ruteTerakhir("yasin:58"))
        assertEquals(Routes.TAHLIL, RuteKonten.ruteTerakhir("tahlil:taawudz"))
        // Tanpa `:` pun harus tetap terbaca sebagai jenisnya.
        assertEquals(Routes.YASIN, RuteKonten.ruteTerakhir("yasin"))
        assertNull("nilai kosong seharusnya tidak menuju layar mana pun", RuteKonten.ruteTerakhir(""))
        assertNull("jenis tak dikenal seharusnya tidak menebak layar",
            RuteKonten.ruteTerakhir("entah:1"))
    }

    /**
     * Beranda memakai pemetaan bersama yang sama dengan Favorit — bukan cabang `startsWith` sendiri
     * yang mengirim bacaan Tahlil ke layar Favorit.
     */
    @Test
    fun layarBerandaMemakaiPemetaanBersama() {
        val layar = KodeSumber.kodeTanpaKomentar(File(root(),
            "src/main/java/id/or/kemenag/yasintahlil/feature/home/HomeScreen.kt").readText())
        assertTrue("HomeScreen tidak memakai RuteKonten untuk 'Terakhir dibaca'",
            layar.contains("RuteKonten.ruteTerakhir("))
        assertFalse("HomeScreen masih menebak tujuan dari awalan id bacaan",
            layar.contains("startsWith(\"yasin\")"))
    }

    /** Layar Favorit memakai pemetaan bersama, bukan daftar sendiri dengan fallback Beranda. */
    @Test
    fun layarFavoritMemakaiPemetaanBersama() {
        val layar = KodeSumber.kodeTanpaKomentar(File(root(),
            "src/main/java/id/or/kemenag/yasintahlil/feature/favorit/FavoritScreen.kt").readText())
        assertTrue("FavoritScreen tidak memakai RuteKonten", layar.contains("RuteKonten.rute("))
        assertTrue("FavoritScreen masih menyimpan fallback rute sendiri (Routes.HOME)",
            !layar.contains("Routes.HOME"))
        // Id kelompok dzikir tetap harus berasal dari bundel konten — hanya tempat membacanya yang
        // pindah ke ViewModel. Membacanya di layar dengan `getOrDefault(emptySet())` membuat
        // kegagalan bundel menyamar sebagai "tujuan tidak dikenal"; lihat
        // `kegagalanMembacaKelompokDzikirTidakDisamarkan`.
        assertTrue("FavoritScreen tidak memakai kelompok dzikir dari keadaan layar",
            layar.contains("s.grupDzikir"))
    }

    /**
     * Kegagalan membaca kelompok dzikir dari bundel konten tidak boleh disamarkan.
     *
     * `FavoritScreen` dahulu membaca `repo.dzikir()` sendiri dan menelan kegagalannya menjadi
     * himpunan kosong. Akibatnya bundel konten yang bermasalah membuat setiap baris favorit/riwayat
     * **dzikir** menampilkan *"Layar tujuan tidak dikenal"* — padahal tujuannya memang dikenal
     * (layar Dzikir & Doa); yang gagal adalah mengenali id kelompoknya. Pengguna menyimpulkan
     * isinya hilang. Id kelompok kini dibaca `FavoritViewModel` dan sebab kegagalannya dibawa ke
     * layar lewat `dzikirGagal`, sama seperti `kotaGagal` pada layar Jadwal.
     */
    @Test
    fun kegagalanMembacaKelompokDzikirTidakDisamarkan() {
        val vm = KodeSumber.kodeTanpaKomentar(File(root(),
            "src/main/java/id/or/kemenag/yasintahlil/feature/favorit/FavoritViewModel.kt").readText())
        assertTrue("FavoritViewModel tidak membaca kelompok dzikir dari bundel konten",
            vm.contains("repo.dzikir()"))
        assertTrue("sebab kegagalan membaca kelompok dzikir tidak dibawa ke keadaan layar",
            vm.contains("dzikirGagal"))

        val layar = KodeSumber.kodeTanpaKomentar(File(root(),
            "src/main/java/id/or/kemenag/yasintahlil/feature/favorit/FavoritScreen.kt").readText())
        assertTrue("layar tidak menampilkan sebab kegagalan membaca kelompok dzikir",
            layar.contains("s.dzikirGagal"))
        assertFalse("layar masih membaca sendiri bundel konten dan menelan kegagalannya",
            layar.contains("repo.dzikir()"))
    }
}
