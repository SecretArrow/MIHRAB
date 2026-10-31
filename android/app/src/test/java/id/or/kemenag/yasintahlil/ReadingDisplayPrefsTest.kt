package id.or.kemenag.yasintahlil.core.designsystem

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Preferensi tampilan bacaan ("Arab / Latin / Terjemahan") harus benar-benar berpengaruh di
 * **setiap** layar bacaan, dan pengaturannya kini terpusat di **satu** tempat: tab Pengaturan.
 *
 * Awalnya kunci `SHOW_ARAB` hanya tersimpan tanpa mengubah layar mana pun; penjaga `showArab`
 * ditulis untuk itu. Ternyata dua kuncinya yang lain — `SHOW_LATIN` dan `SHOW_TRANS` — mengulang
 * riwayat yang sama: tujuh layar menuliskan `true` langsung di setiap pemanggilan `BacaanCard`.
 * Setelahnya pengaturan pernah pula dijual per layar lewat bottom sheet `BacaanSettingsSheet`
 * di sepuluh layar — pengaturan yang sama, pintunya bertebaran. Kini semuanya tinggal di layar
 * Pengaturan, dan uji ini memaku dua sisinya sekaligus:
 *
 * - setiap `BacaanCard` tetap menyebut ketiga sakelarnya (bentuk opsionalnya pernah membuat chip
 *   di Pengaturan mati di sebagian layar), dan
 * - pengaturan bacaan tidak boleh punya pintu kedua di layar mana pun — komponen sheet-nya
 *   memang sudah dihapus dari `UiKit`, dan menghidupkannya kembali tanpa sengaja akan tertangkap
 *   [sheetPengaturanBacaanSudahTidakAda].
 *
 * Satu jalur pelanggaran yang tak tertangkap pencarian pemanggilan: **fungsi pembantu**. Layar
 * Doa Setelah Adzan merender kartunya lewat `KartuDoa`, dan `KartuDoa` dahulu hanya menerima
 * `showArab` — sehingga `showLatin`/`showTrans` di badannya menunjuk ke luar jangkauan. Karena itu
 * parameter fungsi pembantu ikut dipaku
 * ([fungsiPembantuYangMeneruskanShowArabIkutMenerimaKeduanya]).
 */
class ReadingDisplayPrefsTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun featureSources(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/feature")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun storeSource(): String =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt").readText()

    /**
     * Potongan sumber tiap pemanggilan `nama(` dengan tanda kurung yang seimbang, diawali
     * teks pada baris yang sama sebelum pemanggilan — agar penjaga di luar tanda kurung
     * (mis. `if (s.showArab && arab.isNotBlank()) ArabText(...)`) ikut terbaca.
     */
    private fun callSites(src: String, nama: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (true) {
            val at = src.indexOf("$nama(", i)
            if (at < 0) break
            var depth = 0
            var j = at + nama.length
            while (j < src.length) {
                when (src[j]) {
                    '(' -> depth++
                    ')' -> {
                        depth--
                        if (depth == 0) { j++; break }
                    }
                }
                j++
            }
            val barisMulai = src.lastIndexOf('\n', at) + 1
            out += src.substring(barisMulai, minOf(j, src.length))
            i = maxOf(j, at + nama.length + 1)
        }
        return out
    }

    /** Setiap pemanggilan `nama(` di layar fitur, berpasangan dengan nama berkasnya. */
    private fun semuaPemanggilan(nama: String): List<Pair<String, String>> =
        featureSources().flatMap { f -> callSites(f.readText(), nama).map { f.name to it } }

    private fun rapikan(teks: String) = teks.replace(Regex("\\s+"), " ").take(80)

    /**
     * Tiap deklarasi `fun nama(...)` beserta daftar parameternya, dengan tanda kurung seimbang.
     * Komentar dibuang lebih dahulu supaya KDoc yang menyebut bentuk lama tidak ikut terbaca, dan
     * hanya `fun` di awal baris yang diambil agar prosa di dalam kalimat tidak menghitung.
     */
    private fun deklarasiFungsi(src: String): List<String> {
        val kode = KodeSumber.kodeTanpaKomentar(src)
        val out = mutableListOf<String>()
        Regex("""(?m)^\s*(?:private |internal |public |protected )?fun\s+(\w+)\s*\(""")
            .findAll(kode).forEach { m ->
                var depth = 0
                var j = m.range.last
                while (j < kode.length) {
                    when (kode[j]) {
                        '(' -> depth++
                        ')' -> {
                            depth--
                            if (depth == 0) { j++; break }
                        }
                    }
                    j++
                }
                out += kode.substring(m.range.first, minOf(j, kode.length))
            }
        return out
    }

    @Test
    fun setiap_pemanggilan_BacaanCard_menghormati_showArab() {
        val offenders = semuaPemanggilan("BacaanCard")
            .filterNot { (_, t) -> t.contains("showArab") }
            .map { (n, t) -> "$n: ${rapikan(t)}" }
        assertTrue(
            "Ada pemanggilan BacaanCard tanpa showArab sehingga chip \"Arab\" di Pengaturan " +
                "tidak berpengaruh:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    /**
     * Sama untuk Latin dan terjemahan. Nilai yang ditulis sendiri (`true`) adalah pelanggaran yang
     * dicari: di situlah layar berhenti menghormati pilihan pengguna.
     */
    @Test
    fun setiap_pemanggilan_BacaanCard_menghormati_showLatin_dan_showTrans() {
        val offenders = semuaPemanggilan("BacaanCard")
            .filterNot { (_, t) -> t.contains("showLatin") && t.contains("showTrans") }
            .map { (n, t) -> "$n: ${rapikan(t)}" }
        assertTrue(
            "Ada pemanggilan BacaanCard yang menuliskan tampil-tidaknya Latin/terjemahan " +
                "sendiri (mis. `true`) sehingga chip \"Latin\"/\"Terjemahan\" di Pengaturan " +
                "tidak berpengaruh di layar itu:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    /**
     * Pengaturan bacaan terpusat: layar Pengaturan menyediakan ketiga sakelarnya dan menulis kunci
     * yang sama. Tanpa sisi ini, penjaga pemanggilan di atas bisa dipuaskan dengan menghapus
     * pintu pengaturannya di mana pun — pengguna kehilangan kendali tanpa satu pun galat.
     */
    @Test
    fun pengaturanBacaanTerpusatDiLayarPengaturan() {
        val layar = File(
            root(), "src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt",
        ).readText()
        listOf(
            "Store.SHOW_ARAB" to "showA",
            "Store.SHOW_LATIN" to "showL",
            "Store.SHOW_TRANS" to "showT",
        ).forEach { (kunci, saklar) ->
            assertTrue("layar Pengaturan tidak menyediakan sakelar $kunci", layar.contains(kunci))
            assertTrue("sakelar $kunci di Pengaturan tidak terhubung ke nilainya ($saklar)",
                layar.contains(saklar))
        }
        assertTrue("Pengaturan tidak memakai nama bersama untuk sakelar tampilannya",
            layar.contains("NamaAntarmuka.TAMPIL_ARAB"))
    }

    /**
     * Penjaga yang berhenti cocok akan hijau tanpa memeriksa apa pun. Jumlah pemanggilan yang
     * diharapkan dipaku supaya perubahan bentuk pemanggilan tidak membuatnya buta diam-diam.
     */
    @Test
    fun penjagaMenemukanPemanggilanYangDiharapkan() {
        val kartu = semuaPemanggilan("BacaanCard").size
        assertTrue("penjaga tidak lagi menemukan pemanggilan BacaanCard — polanya salah", kartu >= 13)
        // Pintu pengaturan kedua tidak boleh muncul lagi di layar mana pun: nama komponennya
        // sendiri sudah tidak boleh tersisa di seluruh kode utama.
        val sisa = featureSources().filter { it.readText().contains("BacaanSettingsSheet(") }
        assertTrue("sheet pengaturan bacaan masih dibuka di layar: ${sisa.map { it.name }}",
            sisa.isEmpty())
    }

    @Test
    fun ArabText_langsung_juga_dihormati_showArab() {
        // AsmaulHusnaScreen dikecualikan dengan sengaja: sakelar "tampilkan Arab" mengatur
        // BACAAN (Yasin, tahlil, dzikir — baris Arab panjang yang sebagian pengguna ingin
        // sembunyikan), sedangkan Asmaul Husna adalah daftar nama — justru tulisan Arabnya-lah
        // isi utamanya; menyembunyikannya menyisakan daftar arti tanpa yang didefinisikan.
        // Alasan itu didokumentasikan pula di KDoc layarnya.
        val offenders = featureSources()
            .filterNot { it.name == "AsmaulHusnaScreen.kt" }
            .flatMap { f ->
                callSites(f.readText(), "ArabText")
                    .filterNot { it.contains("showArab") }
                    .map { "${f.name}: ${rapikan(it)}" }
            }
        assertTrue(
            "Ada ArabText yang tampil tanpa memeriksa showArab:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    /**
     * Komponen `BacaanSettingsSheet` dihapus saat pengaturan dipusatkan ke tab Pengaturan.
     * Identifier ini tidak boleh kembali muncul di mana pun — kehadirannya berarti ada lagi
     * pintu pengaturan kedua di luar tab Pengaturan. Yang diperiksa kode tanpa komentar:
     * KDoc yang menjelaskan sejarahnya memang menyebut nama itu sebagai penjelasan, bukan kode.
     */
    @Test
    fun sheetPengaturanBacaanSudahTidakAda() {
        val pelanggar = File(root(), "src/main/java")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }
            .filter { KodeSumber.kodeTanpaKomentar(it.readText()).contains("BacaanSettingsSheet") }
            .map { it.name }
            .toList()
        assertTrue("BacaanSettingsSheet masih ada di kode utama: $pelanggar", pelanggar.isEmpty())
    }

    /**
     * Fungsi pembantu yang meneruskan `showArab` ke kartunya harus menerima `showLatin` dan
     * `showTrans` juga. Tanpa itu, badan fungsinya menyebut dua nama yang tidak ada di
     * jangkauannya: pencarian pemanggilan di atas tetap hijau (namanya ada di sana), tetapi
     * layarnya tidak dapat dikompilasi — kegagalannya baru muncul di runner, bukan di sini.
     *
     * Yang diperiksa hanya layar fitur; `BacaanCard` sendiri tinggal di `core/designsystem` dan
     * memang menerima ketiganya.
     */
    @Test
    fun fungsiPembantuYangMeneruskanShowArabIkutMenerimaKeduanya() {
        val offenders = featureSources().flatMap { f ->
            deklarasiFungsi(f.readText())
                .filter { it.contains("showArab: Boolean") }
                .filterNot { it.contains("showLatin: Boolean") && it.contains("showTrans: Boolean") }
                .map { "${f.name}: ${rapikan(it)}" }
        }
        assertTrue(
            "Ada fungsi pembantu yang menerima showArab tetapi tidak showLatin/showTrans, " +
                "sehingga badan fungsinya menyebut nama di luar jangkauan dan layarnya gagal " +
                "dikompilasi:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun kunci_SHOW_ARAB_dideklarasikan_dan_dibaca() {
        val store = storeSource()
        assertTrue("Store.SHOW_ARAB tidak dideklarasikan",
            store.contains("val SHOW_ARAB = booleanPreferencesKey"))
        // Harus dibaca di luar Store.kt, jika tidak chip "Arab" kembali menjadi kontrol mati.
        val pembaca = featureSources().filter { it.readText().contains("Store.SHOW_ARAB") }
        assertTrue("Tidak ada layar fitur yang membaca Store.SHOW_ARAB", pembaca.isNotEmpty())
    }

    @Test
    fun kunci_SHOW_LATIN_dan_SHOW_TRANS_dideklarasikan_dan_dibaca() {
        val store = storeSource()
        listOf("SHOW_LATIN", "SHOW_TRANS").forEach { kunci ->
            assertTrue("Store.$kunci tidak dideklarasikan",
                store.contains("val $kunci = booleanPreferencesKey"))
            // Harus dibaca di luar Store.kt, jika tidak chip-nya kembali menjadi kontrol mati.
            val pembaca = featureSources().filter { it.readText().contains("Store.$kunci") }
            assertTrue("Tidak ada layar fitur yang membaca Store.$kunci", pembaca.isNotEmpty())
        }
    }
}
