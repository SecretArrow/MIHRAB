package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Aturan *"bagaimana metode hisab yang sedang berlaku diterangkan ke pengguna"* dahulu ditulis
 * ulang di tiap layar yang menyebutnya, dengan tiga akibat yang berbeda-beda:
 *
 * - kepala layar Jadwal menuliskan `Magrib+…mnt`,
 * - penjelasan di menu Adzan menuliskan `Isya Magrib+… menit` — **dua ejaan untuk satu aturan**,
 * - dan layar Pengaturan menuliskan metode **Kemenag/SIHAT** secara tetap, padahal pengguna bebas
 *   memilih metode lain (MWL, Umm al-Qura, Egyptian, Makkah). Siapa pun yang memilih MWL membaca di
 *   Pengaturan bahwa jadwalnya dihitung dengan Kemenag/SIHAT — keterangan yang berbohong tanpa satu
 *   pun pesan galat.
 *
 * Uji ini menahan agar aturan itu tinggal di satu tempat: [JadwalHariIni.metodeRingkas], dibaca dari
 * [PrayerCalc.Method] yang sedang aktif. Isinya diperiksa **terhadap definisi setiap metode**, bukan
 * terhadap tulisan yang diharapkan — jadi mengubah sudut sebuah metode tidak membuat uji ini hijau
 * secara palsu, dan menambah metode baru pun ikut terjaga.
 */
class AturanMetodeSatuTempatTest {

    private val layarPenyebutMetode = listOf(
        "src/main/java/id/or/kemenag/yasintahlil/feature/jadwal/JadwalScreen.kt",
        "src/main/java/id/or/kemenag/yasintahlil/feature/adzan/AdzanScreen.kt",
        "src/main/java/id/or/kemenag/yasintahlil/feature/settings/SettingsScreen.kt",
    )

    /**
     * Ringkasan harus **mengikuti** definisi metodenya: sudut Fajr selalu sudut, sedangkan Isya
     * dinyatakan sebagai sudut derajat atau sebagai selang menit sesudah Magrib — tidak keduanya.
     */
    @Test
    fun ringkasanMetodeMengikutiDefinisiMetodenya() {
        assertTrue("tidak ada metode untuk diperiksa", PrayerCalc.Method.entries.size >= 5)
        PrayerCalc.Method.entries.forEach { m ->
            val teks = JadwalHariIni.metodeRingkas(m)
            assertTrue("$m: sudut Fajr tidak muncul pada ringkasan", teks.contains("Fajr ${m.fajr}°"))
            if (m.ishaIntervalMin != null) {
                assertTrue("$m: selang Isya sesudah Magrib tidak muncul",
                    teks.contains("Isya Magrib+${m.ishaIntervalMin} mnt"))
                assertFalse("$m: sudut Isya tidak boleh muncul bila metode memakai selang menit",
                    teks.contains("Isya ${m.isha}°"))
            } else {
                assertTrue("$m: sudut Isya tidak muncul pada ringkasan", teks.contains("Isya ${m.isha}°"))
            }
        }
    }

    /** Setiap layar yang menerangkan metode harus membaca aturan bersama, bukan menyalinnya. */
    @Test
    fun setiapLayarYangMenyebutMetodeMemakaiAturanBersama() {
        layarPenyebutMetode.forEach { rel ->
            val kode = KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(rel))
            assertTrue("$rel menerangkan metode tetapi tidak memakai `JadwalHariIni.metodeRingkas`",
                kode.contains("JadwalHariIni.metodeRingkas("))
        }
    }

    /**
     * Dan tidak boleh ada layar yang mengetik sendiri bagian aturan itu — sudut metode, selang
     * `Magrib+…`, atau nama metode tetap. Yang pertama mengubah keterangan menjadi salinan kedua,
     * yang terakhir mengubahnya menjadi kebohongan begitu pengguna memilih metode lain.
     */
    @Test
    fun tidakAdaLayarYangMengetikSudutMetodeSendiri() {
        val dilarang = listOf(
            "metode.fajr", "metode.isha", "s.method.fajr", "s.method.isha",
            "Magrib+", "Kemenag/SIHAT",
        )
        layarPenyebutMetode.forEach { rel ->
            val kode = KodeSumber.kodeTanpaKomentar(KodeSumber.sumber(rel))
            dilarang.forEach { pola ->
                assertFalse("$rel mengetik sendiri bagian aturan metode ('$pola') alih-alih " +
                    "memakai `JadwalHariIni.metodeRingkas`", kode.contains(pola))
            }
        }
    }

    /**
     * Layar yang menyebut nama metode harus benar-benar **tahu** metode mana yang aktif. Layar
     * Pengaturan dahulu menyebut "Kemenag/SIHAT" tanpa pernah membaca `Store.METHOD`; memakai
     * `metode.label` sambil membiarkannya selalu bernilai bawaan adalah kebohongan yang sama,
     * hanya berpakaian seolah membaca preferensi. Karena itu tiap layar diperiksa bersama berkas
     * satu paketnya — menu Jadwal mendapat metodenya dari `JadwalViewModel` di paket yang sama.
     */
    @Test
    fun metodeDiLayarBerasalDariPreferensi() {
        layarPenyebutMetode.forEach { rel ->
            val berkas = File(KodeSumber.root(), rel)
            // `parentFile` bertipe `File?` menurut anotasi JDK. Direktori induknya selalu ada karena
            // [rel] berupa jalur relatif berisi pemisah — `?: error` menjaga invarian itu terbaca,
            // bukan menyamarkannya sebagai "tidak ada berkas di paket ini".
            val dirPaket = berkas.parentFile ?: error("$rel tidak punya direktori induk")
            val sePaket = dirPaket.listFiles { f -> f.isFile && f.extension == "kt" }.orEmpty()
            assertTrue("$rel menyebut metode tetapi tak ada berkas di paketnya yang membaca " +
                "`Store.METHOD` — nama metodenya pasti tetap",
                sePaket.any { KodeSumber.kodeTanpaKomentar(it.readText()).contains("store.get(Store.METHOD") })
        }
    }

    /**
     * Bukti bahwa aturannya benar-benar tinggal di satu tempat: format `Magrib+…mnt` hanya muncul di
     * satu berkas kode produksi. Bila salinan kedua lahir kembali, uji ini menyebut berkasnya.
     */
    @Test
    fun aturanMetodeHanyaHidupDiSatuBerkas() {
        val berkas = File(KodeSumber.root(), "src/main/java")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }
            .filter { KodeSumber.kodeTanpaKomentar(it.readText()).contains("Magrib+") }
            .map { it.name }.sorted().toList()
        assertEquals("format 'Magrib+…mnt' harus hidup di satu berkas saja", listOf("JadwalHariIni.kt"), berkas)
    }
}
