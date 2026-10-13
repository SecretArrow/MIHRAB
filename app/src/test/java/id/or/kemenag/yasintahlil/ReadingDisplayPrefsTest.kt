package id.or.kemenag.yasintahlil.core.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Preferensi tampilan bacaan ("Arab / Latin / Terjemahan") harus benar-benar berpengaruh di
 * **setiap** layar bacaan.
 *
 * Awalnya kunci `SHOW_ARAB` hanya tersimpan tanpa mengubah layar mana pun; penjaga `showArab`
 * ditulis untuk itu. Ternyata dua kuncinya yang lain — `SHOW_LATIN` dan `SHOW_TRANS` — mengulang
 * riwayat yang sama, dan lebih sunyi: tujuh layar menuliskan `true` langsung di setiap pemanggilan
 * `BacaanCard`, sementara Yasin, Tahlil, dan Dzikir membacanya dari preferensi. Akibatnya pengguna
 * yang mematikan "Tampil terjemahan" di Pengaturan masih membaca terjemahan di layar Sholat, Ghaib,
 * Jenazah, Adzan, dan Iqomah — dan di layar itu tombolnya pun tak pernah muncul di "Pengaturan
 * bacaan", sebab `BacaanSettingsSheet` menerima keduanya sebagai parameter **opsional** dengan
 * alasan "layar yang tak punya bacaan Latin/terjemahan". Alasan itu keliru: ketujuh layar itu
 * memang menampilkan Latin dan terjemahan.
 *
 * Jadi uji ini memaku aturan yang sama untuk ketiga kunci: setiap `BacaanCard` menyebut ketiganya,
 * dan setiap `BacaanSettingsSheet` menyediakan ketiganya — bentuk opsionalnya sudah dihapus, jadi
 * ketiadaan salah satunya adalah galat kompilasi di layar, bukan chip yang menghilang diam-diam.
 */
class ReadingDisplayPrefsTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun featureSources(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/feature")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun storeSource(): String =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt").readText()

    private fun uiKitSource(): String =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/designsystem/UiKit.kt").readText()

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

    /** Sheet pengaturan bacaan harus menyediakan **ketiga** sakelar, bukan hanya Arab. */
    @Test
    fun setiap_BacaanSettingsSheet_menyediakan_ketiga_sakelar() {
        val offenders = semuaPemanggilan("BacaanSettingsSheet")
            .filterNot { (_, t) ->
                t.contains("showLatin") && t.contains("onShowLatin") &&
                    t.contains("showTrans") && t.contains("onShowTrans")
            }
            .map { (n, t) -> "$n: ${rapikan(t)}" }
        assertTrue(
            "Ada BacaanSettingsSheet yang tidak menyediakan sakelar Latin/terjemahan, sehingga " +
                "pengguna tak dapat mengubahnya dari layar itu:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    /**
     * Penjaga yang berhenti cocok akan hijau tanpa memeriksa apa pun. Jumlah pemanggilan yang
     * diharapkan dipaku supaya perubahan bentuk pemanggilan tidak membuatnya buta diam-diam.
     */
    @Test
    fun penjagaMenemukanPemanggilanYangDiharapkan() {
        val kartu = semuaPemanggilan("BacaanCard").size
        val sheet = semuaPemanggilan("BacaanSettingsSheet").size
        assertTrue("penjaga tidak lagi menemukan pemanggilan BacaanCard — polanya salah", kartu >= 13)
        assertTrue("penjaga tidak lagi menemukan BacaanSettingsSheet — polanya salah", sheet >= 10)
    }

    @Test
    fun ArabText_langsung_juga_dihormati_showArab() {
        val offenders = featureSources().flatMap { f ->
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
     * Bentuk **opsional** itulah yang dahulu membuat tujuh layar dapat menghilangkan sakelar
     * Latin/terjemahan tanpa jejak apa pun: tidak ada galat, tidak ada chip, dan nilainya tetap
     * `true` di dalam kartunya. Karena itu parameternya dipaku sebagai **wajib** — layar yang
     * memakai sheet ini tak punya cara lagi untuk diam-diam melewatinya.
     */
    @Test
    fun sheetTidakLagiMenjadikanSakelarLatinTerjemahanOpsional() {
        val deklarasi = uiKitSource()
            .substringAfter("fun BacaanSettingsSheet(").substringBefore(") {")
        listOf(
            "showLatin: Boolean",
            "onShowLatin: (Boolean) -> Unit",
            "showTrans: Boolean",
            "onShowTrans: (Boolean) -> Unit",
        ).forEach { tanda ->
            assertTrue(
                "BacaanSettingsSheet kehilangan parameter wajib `$tanda` — sakelar Latin/terjemahan " +
                    "dapat dihilangkan layar tanpa jejak lagi",
                deklarasi.contains(tanda),
            )
        }
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
