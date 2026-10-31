package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Aturan *"kunci preferensi hanya ada bila benar-benar dipakai"*.
 *
 * `docs/PERMISSIONS.md` dan manifes sama-sama menjanjikan aturan serupa untuk **izin** — dan dulu
 * `WAKE_LOCK` duduk di manifes tanpa satu pun pemakaian, sampai `PemakaianIzinTest` menjadikan
 * aturan itu nyata. Kunci preferensi punya lubang yang sama, hanya lebih sunyi lagi: izin yang tak
 * dipakai masih terlihat di peninjau toko aplikasi, sedangkan kunci yang tak dipakai **tak terlihat
 * siapa pun**. Ia ikut dideklarasikan, ikut disebut `importJson`, ikut dijanjikan `BawaanPreferensi`
 * — dan tak satu layar pun membacanya.
 *
 * Tiga kunci seperti itu benar-benar ada: `SOURCE` (sisa fitur "sumber online" yang tak pernah
 * diaktifkan — dua chip di Pengaturan memang sengaja mati), `HIST` (riwayat), dan `TAHLIL_PROG`
 * (posisi Tahlil). Dua yang terakhir lebih buruk daripada sekadar kode mati: keduanya menyimpan
 * **data pengguna sungguhan** dari v1.0.0, dan karena tak ada yang membacanya lagi, pemindahan ke
 * Room hanya mencakup favorit — riwayat dan posisi Tahlil pengguna hilang tanpa galat apa pun.
 *
 * Dua arah yang dijaga di sini: setiap kunci yang **dideklarasikan** harus dipakai kode aplikasi,
 * dan setiap kunci yang **ditandai warisan** harus benar-benar dipindahkan dari DataStore.
 */
class PemakaianKunciTest {

    private val storeRel = "src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt"
    private val appRel = "src/main/java/id/or/kemenag/yasintahlil/YasinApp.kt"
    private val migrasiRel = "src/main/java/id/or/kemenag/yasintahlil/core/data/local/MigrasiWarisan.kt"
    private val settingsRel = "src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt"

    /** `Store.kt` tanpa komentar — KDoc-nya menyebut kunci yang sudah dihapus sebagai penjelasan. */
    private val store: String get() = KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(storeRel))

    /** `Store.kt` apa adanya: penanda "migrasi ke Room" justru **ada di dalam komentar**. */
    private val storeMentah: String get() = KodeSumber.sumber(storeRel)

    /** Nama kunci yang dideklarasikan `Store`. */
    private fun kunciDideklarasikan(): Set<String> =
        Regex("""\bval\s+([A-Z][A-Z0-9_]*)\s*=\s*\w+?PreferencesKey\(""")
            .findAll(store).map { it.groupValues[1] }.toSet()

    /** Seluruh berkas kode aplikasi, kecuali `Store.kt` sendiri (tempat kuncinya dideklarasikan). */
    private fun berkasAplikasi(): List<File> =
        File(KodeSumber.root(), "src/main/java")
            .walkTopDown().filter { it.isFile && it.extension == "kt" && it.name != "Store.kt" }.toList()

    /**
     * Kunci yang ditandai warisan **dan** punya tujuan pemindahan: komentarnya menyebut
     * "migrasi ke Room". `YASIN_MARK` juga bertanda warisan, tetapi ia masih dibaca langsung oleh
     * layar Yasin, jadi ia bukan kunci yang harus dipindahkan.
     */
    private val kunciWarisan = listOf("FAV", "HIST", "TAHLIL_PROG")

    @Test
    fun setiapKunciPreferensiDipakaiKodeAplikasi() {
        val kunci = kunciDideklarasikan()
        assertTrue("tidak ada kunci Store yang terbaca — pola pengambilnya salah", kunci.size >= 30)

        val dipakai = mutableSetOf<String>()
        berkasAplikasi().forEach { f ->
            val kode = KodeSumber.kodeTanpaKomentar(f.readText())
            Regex("""\.(?:get|flow|set)\(\s*Store\.([A-Z][A-Z0-9_]*)""")
                .findAll(kode).forEach { dipakai += it.groupValues[1] }
        }
        // Pemindaian yang berhenti menemukan apa pun akan membuat uji ini hijau tanpa memeriksa
        // apa-apa; jumlah kunci yang terbaca memastikan pemindaiannya benar-benar bekerja.
        assertTrue("tidak ada pemakaian kunci yang terbaca dari kode aplikasi", dipakai.size >= 30)

        val takDipakai = kunci - dipakai
        assertTrue(
            "kunci preferensi dideklarasikan (dan ikut dipulihkan dari backup) tetapi tak satu pun " +
                "kode aplikasi membacanya — data pengguna yang tersimpan di situ tak akan pernah " +
                "terlihat lagi: $takDipakai",
            takDipakai.isEmpty(),
        )
    }

    /**
     * Setiap kunci warisan harus **dibaca** pemindahnya (untuk diambil isinya) dan **dikosongkan**
     * setelahnya (agar tidak dipindahkan dua kali). Kunci yang hanya salah satunya terpenuhi berarti
     * pemindahannya setengah jalan — persis kekeliruan yang membuat riwayat dan posisi Tahlil
     * tertinggal ketika hanya favorit yang dipindahkan.
     *
     * Pemindah yang **didefinisikan tetapi tak pernah dipanggil** sama saja dengan tidak ada, dan itu
     * juga pernah terjadi di repositori ini; karena itu rantai pemanggilannya ikut diperiksa, dari
     * `onCreate` sampai ketiga fungsi pemindahnya.
     */
    @Test
    fun setiapKunciWarisanDipindahkanDariDataStore() {
        val app = KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(appRel))
        assertTrue("YasinApp tidak menjalankan pemindah data warisan dari onCreate",
            app.substringAfter("override fun onCreate").contains("migrasiWarisan.jalankan()"))

        val migrasi = KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(migrasiRel))
        val fungsi = Regex("""private suspend fun (\w+)\(""")
            .findAll(migrasi).map { it.groupValues[1] }.toList()
        assertEquals("jumlah fungsi pemindah berbeda dari jumlah kunci warisan",
            kunciWarisan.size, fungsi.size)
        // Badan `jalankan` saja — dipotong sebelum definisi pemindah pertama. Tanpa potongan ini,
        // tanda tangan definisinya sendiri (`private suspend fun favorit() {`) sudah memuat
        // `favorit()`, sehingga "didefinisikan tetapi tak dipanggil" lolos begitu saja — persis
        // lubang yang membuat pemindah riwayat sempat luput.
        val badan = migrasi.substringAfter("suspend fun jalankan()")
            .substringBefore("private suspend fun")
        fungsi.forEach { f ->
            assertTrue("pemindah $f didefinisikan tetapi tak pernah dipanggil — data warisannya " +
                "tidak akan pernah dipindahkan", badan.contains("$f()"))
        }

        kunciWarisan.forEach { k ->
            assertTrue("$k tidak dibaca pemindah data warisan", migrasi.contains("store.get(Store.$k"))
            assertTrue("$k tidak dikosongkan setelah dipindahkan", migrasi.contains("store.set(Store.$k"))
        }
    }

    /**
     * Jalan **kedua** data warisan masuk ke DataStore: berkas cadangan v1.0.0 hanya memuat
     * preferensi, sehingga `Store.importJson` menuliskan favorit, riwayat, dan posisi Tahlil kembali
     * ke DataStore — dan tak ada yang membacanya lagi. Tanpa pemindah yang dijalankan sesudah
     * pemulihan, tombol "Pulihkan" tampak berhasil sementara ketiga data itu tak pernah muncul.
     */
    @Test
    fun pemulihanBackupLamaJugaMenjalankanPemindahWarisan() {
        val layar = KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(settingsRel))
        val pulih = layar.substringAfter("val root = runCatching { JSONObject(backup) }")
            .substringBefore("Text(\"Pulihkan\")")
        assertTrue("pemulihan backup lama tidak menjalankan pemindah data warisan",
            pulih.contains("migrasi.jalankan()"))
        assertTrue("pemindah dijalankan sebelum preferensi backup dipulihkan",
            pulih.indexOf("store.importJson(backup)") < pulih.indexOf("migrasi.jalankan()"))
    }

    /** Tabel warisan tidak boleh basi: kunci bertanda "migrasi ke Room" harus tepat sama dengan daftarnya. */
    @Test
    fun daftarKunciWarisanTidakBasi() {
        val ditandai = Regex("""\bval\s+([A-Z][A-Z0-9_]*)\s*=[^\n]*//[^\n]*migrasi ke Room""")
            .findAll(storeMentah).map { it.groupValues[1] }.toSet()
        assertEquals(
            "kunci bertanda 'migrasi ke Room' berbeda dari daftar yang benar-benar dipindahkan — " +
                "kunci warisan yang luput tak akan pernah dipindahkan, dan data penggunanya hilang",
            kunciWarisan.toSet(), ditandai,
        )
    }
}
