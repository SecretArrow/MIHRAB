package id.or.kemenag.yasintahlil.feature.sholat

import id.or.kemenag.yasintahlil.KodeSumber
import id.or.kemenag.yasintahlil.core.sholat.BacaanSholat
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Tata Cara Sholat Langkah demi Langkah" mendaftar *"Tasyahud awal **jika relevan**"*
 * dan *"Tasyahud akhir"* sebagai **dua langkah terpisah**.
 *
 * Dulu keduanya satu entri ("Tasyahud (awal & akhir)"), sehingga pengguna yang mengikuti langkah
 * demi langkah tak pernah diberi tahu bahwa tasyahud awal hanya ada pada sholat lebih dari dua
 * rakaat — dan ilustrasi posisinya pun salah untuk duduk akhir, karena satu-satunya posisi yang
 * dipetakan adalah duduk iftirasy.
 *
 * Uji ini juga menjaga aturan sumber: lafaz tasyahud **tidak ditulis dua kali**. Entri
 * `tasyahud_awal` hanya merujuk lafaz yang sudah ada (`sholat_bacaan.json#tasyahud_akhir`) lewat
 * mekanisme `rujukan_file` yang sudah dipakai `niat` dan `fatihah`, sehingga pemisahan langkah ini
 * tidak menambah satu pun teks Arab baru.
 *
 * Urutan langkah pun diperiksa: daftar langkah dibaca berurutan, jadi bacaan yang diletakkan di
 * tempat yang bertentangan dengan keterangan waktunya sendiri akan menyesatkan pembaca yang
 * mengikuti urutannya tanpa membaca keterangannya.
 */
class LangkahSholatTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun bundel(): JSONObject =
        JSONObject(File(root(), "src/main/assets/content/sholat_bacaan.json").readText())

    private fun entri(): List<JSONObject> {
        val arr = bundel().getJSONArray("bacaan")
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    private fun entri(id: String): JSONObject =
        entri().firstOrNull { it.optString("id") == id }
            ?: error("entri '$id' tidak ada di bundel")

    @Test
    fun keduaTasyahudAdaSebagaiLangkahTerpisahDanBerurutan() {
        val id = entri().map { it.getString("id") }
        assertTrue("tasyahud awal tidak ada", id.contains("tasyahud_awal"))
        assertTrue("tasyahud akhir tidak ada", id.contains("tasyahud_akhir"))
        assertTrue("tasyahud awal harus sebelum tasyahud akhir",
            id.indexOf("tasyahud_awal") < id.indexOf("tasyahud_akhir"))
        // Entri gabungan lama tidak boleh tertinggal dan tampil sebagai langkah ketiga.
        assertTrue("entri gabungan 'tasyahud' masih ada", !id.contains("tasyahud"))
    }

    /** `plan.txt`: tasyahud awal "jika relevan" — kapan ia relevan harus terbaca pengguna. */
    @Test
    fun masingMasingMenjelaskanKapanDibaca() {
        val awal = entri("tasyahud_awal").getString("kapan")
        val akhir = entri("tasyahud_akhir").getString("kapan")
        // Dibandingkan tanpa membedakan besar-kecil huruf: kalimatnya dimulai dengan huruf kapital.
        assertTrue("kapan tasyahud awal tidak menyebut syarat rakaat: $awal",
            awal.lowercase().contains("dua rakaat"))
        assertTrue("kapan tasyahud awal tidak menyebut duduknya: $awal",
            awal.lowercase().contains("iftirasy"))
        assertTrue("kapan tasyahud akhir tidak menyebut rakaat terakhir: $akhir",
            akhir.lowercase().contains("rakaat terakhir"))
        assertTrue("kapan tasyahud akhir tidak menyebut lanjutannya: $akhir",
            akhir.lowercase().contains("sholawat"))
        assertNotEquals("kedua langkah menjelaskan waktu yang sama", awal, akhir)
    }

    /**
     * Lafaz tidak boleh disalin dua kali — teks Arab hanya dari sumber resmi, dan menyalinnya
     * berarti menambah satu salinan lagi yang bisa menyimpang tanpa ketahuan.
     */
    @Test
    fun lafazTidakDisalinTetapiDirujuk() {
        val awal = entri("tasyahud_awal")
        assertTrue("tasyahud awal menulis lafaznya sendiri", !awal.has("arab"))
        assertEquals("rujukan tasyahud awal tidak menunjuk lafaz tasyahud akhir",
            "sholat_bacaan.json#tasyahud_akhir", awal.getString("rujukan_file"))
        assertTrue("tasyahud akhir tidak memuat lafaznya", entri("tasyahud_akhir").has("arab"))
    }

    /** Rujukan itu harus benar-benar menyelesaikan lafaz lengkap berharakat, bukan kosong. */
    @Test
    fun rujukanTasyahudAwalTeruraiLengkap() {
        val rujukan = entri("tasyahud_awal").getString("rujukan_file")
        val pisah = rujukan.split("#", limit = 2)
        val id = pisah[1]
        val sumber = entri(id)
        listOf("arab", "latin", "terjemahan").forEach { kunci ->
            assertTrue("lafaz tasyahud awal kosong pada '$kunci'",
                sumber.optString(kunci).isNotBlank())
        }
        // Harakat wajib ada (permintaan pengguna: huruf Arab selalu berharakat).
        val arab = sumber.getString("arab")
        assertTrue("lafaz tasyahud tanpa harakat", arab.any { it.code in 0x064B..0x0652 })
    }

    /** Ilustrasi posisi harus membedakan duduk awal dari duduk akhir. */
    @Test
    fun posisiDudukAwalDanAkhirBerbeda() {
        val awal = Postures.of("tasyahud_awal") ?: error("tasyahud awal tanpa posisi")
        val akhir = Postures.of("tasyahud_akhir") ?: error("tasyahud akhir tanpa posisi")
        assertTrue("kedua tasyahud memakai ilustrasi posisi yang sama",
            awal.varian.single().tampak != akhir.varian.single().tampak)
        assertTrue("duduk awal bukan iftirasy: ${awal.label}", awal.label.contains("iftirasy"))
        assertTrue("duduk akhir mengklaim satu bentuk saja: ${akhir.label}",
            akhir.label == "Duduk akhir")
        assertTrue("deskripsi duduk akhir tidak menyebut kedua bentuk rakaat",
            akhir.deskripsi.contains("tawarruk") && akhir.deskripsi.contains("iftirasy"))
        // Sholawat Ibrahimiyah dibaca pada duduk akhir, bukan duduk iftirasy.
        assertTrue("sholawat tidak dipetakan ke duduk akhir",
            Postures.of("sholawat_ibrahimiyah")?.varian?.single()?.tampak == akhir.varian.single().tampak)
    }

    /** Mode ringkas harus tetap sah untuk sholat dua rakaat: tanpa langkah yang bersyarat rakaat. */
    @Test
    fun modeRingkasMelewatiLangkahBersyaratRakaat() {
        val daftar = BacaanSholat.RINGKAS
        assertTrue("mode ringkas tidak memuat tasyahud akhir", BacaanSholat.TASYAHUD_AKHIR in daftar)
        assertTrue("mode ringkas memuat langkah bersyarat rakaat", BacaanSholat.TASYAHUD_AWAL !in daftar)
        // Qunut pun bersyarat — mazhab Syafi'i dan sholat Subuh — jadi tidak boleh ikut meringkas.
        assertTrue("mode ringkas memuat langkah bersyarat mazhab", BacaanSholat.QUNUT !in daftar)
        // Layar harus membaca daftar bersama itu, bukan menyalin isinya: salinan akan berbeda
        // begitu salah satunya diubah, dan satu langkah pokok diam-diam hilang dari ringkasan.
        val layar = KodeSumber.kodeTanpaKomentar(File(root(),
            "src/main/java/id/or/kemenag/yasintahlil/feature/sholat/SholatDetailScreen.kt").readText())
        assertTrue("layar tidak memakai daftar ringkas bersama", layar.contains("BacaanSholat.RINGKAS"))
    }

    /**
     * Qunut dibaca setelah i'tidal pada rakaat kedua, jadi ia harus berdiri sesudah i'tidal pada
     * daftar langkah. Sebelumnya entri ini terletak di antara sholawat Ibrahimiyah dan salam —
     * bertentangan dengan keterangan waktunya sendiri, sehingga pembaca yang membaca urutannya
     * sampai ke qunut sesudah tasyahud.
     */
    @Test
    fun qunutTerletakSetelahItidal() {
        val id = entri().map { it.getString("id") }
        val itidal = id.indexOf("itidal")
        assertTrue("i'tidal tidak ada di bundel", itidal >= 0)
        assertEquals("qunut tidak tepat sesudah i'tidal", itidal + 1, id.indexOf("qunut"))
        assertTrue("qunut terletak sesudah tasyahud", id.indexOf("qunut") < id.indexOf("tasyahud_awal"))
    }

    /** Seluruh langkah wajib tetap ada setelah pemisahan dan pengurutan ulang. */
    @Test
    fun tidakAdaLangkahYangHilang() {
        // Dibandingkan dengan kosakata bersama, bukan daftar yang diketik di sini: daftar kedua di
        // dalam uji akan tetap "lulus" meski bundel dan layar sama-sama bergeser meninggalkannya.
        assertEquals("id bacaan pada bundel berbeda dari kosakata bersama",
            BacaanSholat.SEMUA, entri().map { it.getString("id") })
    }

    /**
     * Daftar langkah yang kosong tidak boleh membuat aplikasi berhenti.
     *
     * Bundel konten dapat diperbarui pengguna (layar "Pembaruan konten"), sehingga id sholat yang
     * dahulu ada bisa hilang dari `sholat_bacaan.json` dan daftar langkahnya menjadi kosong. Dahulu
     * baris `steps[step.coerceIn(0, steps.size - 1)]` menghitung `coerceIn(0, -1)` — rentang kosong —
     * dan aplikasi berhenti begitu layar ini dibuka. Langkah kini digambar lewat
     * `steps.forEachIndexed` — badan perulangan tak pernah jalan pada daftar kosong, jadi tak ada
     * indeks yang bisa salah — dan kekosongannya tetap diberitahukan sebelum perulangan itu.
     */
    @Test
    fun daftarLangkahKosongTidakMembuatAplikasiBerhenti() {
        // Tanpa komentar: penjelasan di layar menyebut pola lama itu, dan penjelasan bukan pelanggaran.
        val layar = KodeSumber.kodeTanpaKomentar(File(root(),
            "src/main/java/id/or/kemenag/yasintahlil/feature/sholat/SholatDetailScreen.kt").readText())
        assertTrue("layar tidak lagi menggambar langkahnya", layar.contains("steps.forEachIndexed {"))
        assertTrue("layar tidak menangani daftar langkah yang kosong",
            layar.contains("if (steps.isEmpty())"))
        // Pemberitahuan kekosongan harus berada **sebelum** perulangan menggambarnya: urutan lain
        // berarti pengguna sempat melihat sesuatu yang tak semestinya digambar sebelum diberi tahu.
        assertTrue("langkah digambar sebelum daftar kosong ditangani",
            layar.substringBefore("steps.forEachIndexed {").contains("if (steps.isEmpty())"))
    }
}
