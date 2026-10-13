package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Aturan **"bacaan dibaca dari jauh"** — mode layar penuh, mode fokus, atau mode jamaah — harus
 * dinaikkan ukurannya lewat [UkuranBacaan.arabBacaJauh] di **setiap** layar baca yang punya mode itu.
 *
 * Angka `+ 8` dan batas `44f` memang sudah tinggal di satu tempat, dan `UkuranBacaanTest` menjaga agar
 * tak ada layar menuliskannya kembali. Yang tidak dijaga adalah **pemakaiannya**: sebuah layar dapat
 * memiliki mode baca jarak jauh lalu melewatkan ukuran Arabnya apa adanya, dan tak ada pemeriksaan
 * yang gagal — aturannya benar, ia hanya tidak dipakai. Itulah yang terjadi pada mode fokus Yasin,
 * layar penuh Dzikir, dan layar penuh Tahlil: ketiganya menyembunyikan bilah bawah dan meminta
 * bacaan dibaca dari jauh, tetapi teks Arabnya tetap seukuran bacaan biasa, sedangkan di layar Ghaib,
 * Jenazah, Iqomah, dan mode jamaah Tahlil teksnya membesar. Pengguna yang menekan "Layar penuh" di
 * dua layar itu karena itu tidak mendapat apa yang ia dapat di layar lain, tanpa satu pun pesan galat.
 *
 * Layarnya sendiri **ditemukan dari sumber**, bukan didaftar di sini: setiap berkas yang memanggil
 * `FullscreenEffect(...)` dianggap bermode baca jarak jauh, dan yang benar-benar menampilkan teks
 * Arab (`BacaanCard(`/`ArabText(`) wajib menyalurkan ukurannya lewat aturan bersama. Berkas layar
 * baru karena itu ikut terjaga tanpa perlu menyunting uji ini — dan layar tanpa teks Arab (mis.
 * kompas Kiblat, yang juga punya mode layar penuh) sengaja tidak dituntut.
 */
class UkuranBacaanModeBacaTest {

    private fun berkasSumber(): List<File> =
        File(KodeSumber.root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Nama variabel mode yang mengaktifkan layar penuh lewat `FullscreenEffect(<nama>)`. */
    private fun modeLayarPenuh(kode: String): Set<String> =
        Regex("""FullscreenEffect\(\s*(\w+)\s*\)""").findAll(kode)
            .map { it.groupValues[1] }.toSet()

    /** Layar yang benar-benar menampilkan teks Arab — layar tanpa teks Arab tidak dituntut. */
    private fun menampilkanArab(kode: String): Boolean =
        kode.contains("BacaanCard(") || kode.contains("ArabText(")

    /**
     * Nama variabel mode yang **benar-benar sampai** ke ukuran Arab.
     *
     * Diambil dari argumen kedua setiap panggilan `arabBacaJauh(...)`, ditambah nama-nama yang muncul
     * di dalam definisi alias yang dipakai sebagai argumen itu — sebab layar boleh menggabungkan dua
     * mode lebih dahulu (`val bacaJauh = jamaah || fullscreen`) alih-alih menulis syaratnya dua kali.
     */
    private fun modeYangSampaiKeUkuran(kode: String): Set<String> {
        val kedua = Regex("""arabBacaJauh\(([^()]*)\)""").findAll(kode)
            .map { it.groupValues[1].substringAfter(",").trim() }
            .filter { it.isNotBlank() }
            .toList()
        val langsung = kedua.filter { it.matches(Regex("""\w+""")) }.toMutableSet()
        langsung.toList().forEach { nama ->
            val definisi = Regex("""val\s+$nama\s*=\s*([^\n]+)""").find(kode)
                ?.groupValues?.get(1) ?: return@forEach
            langsung += Regex("""\w+""").findAll(definisi).map { it.value }
        }
        return langsung
    }

    /** Layar bermode baca jarak jauh yang menampilkan teks Arab, beserta mode yang harus tersalurkan. */
    private fun layarBermode(): List<Triple<String, String, String>> =
        berkasSumber().flatMap { f ->
            val kode = KodeSumber.kodeTanpaKomentar(f.readText())
            val mode = modeLayarPenuh(kode)
            if (mode.isEmpty() || !menampilkanArab(kode)) emptyList()
            else mode.map { Triple(f.name, it, kode) }
        }

    /**
     * Setiap layar baca bermode jarak jauh harus menyalurkan ukuran Arabnya lewat aturan bersama.
     *
     * Dua kegagalan yang berbeda sama-sama tertangkap: layar yang tak pernah memanggil
     * `arabBacaJauh` sama sekali (Yasin, Dzikir), dan layar yang memanggilnya untuk mode **lain**
     * tetapi bukan untuk layar penuhnya sendiri (Tahlil, yang hanya menaikkan di mode jamaah).
     */
    @Test
    fun setiapLayarBermodeBacaJauhMenaikkanUkuranLewatAturanBersama() {
        val layar = layarBermode()
        assertTrue("tidak ada layar bermode baca jarak jauh yang terbaca — penandanya salah", layar.size >= 6)
        val pelanggar = layar.filterNot { (_, mode, kode) -> mode in modeYangSampaiKeUkuran(kode) }
            .map { (nama, mode, _) -> "$nama: mode '$mode' tidak sampai ke UkuranBacaan.arabBacaJauh" }
        assertTrue("mode baca jarak jauh tidak menaikkan ukuran Arab lewat aturan bersama: $pelanggar",
            pelanggar.isEmpty())
    }

    /**
     * Penandanya harus benar-benar menemukan layar yang kita maksud, bukan sekadar berkas mana pun.
     *
     * Tanpa ini, uji di atas dapat lolos hanya karena [modeLayarPenuh] berhenti mencocokkan apa pun —
     * mis. bila `FullscreenEffect` diganti nama — dan tak satu layar pun tersisa untuk diperiksa.
     */
    @Test
    fun penandaMenemukanLayarYangMemangBermode() {
        val nama = layarBermode().map { it.first }.toSet()
        listOf(
            "YasinScreen.kt", "DzikirScreen.kt", "TahlilScreen.kt",
            "GhaibScreen.kt", "JenazahScreen.kt", "IqomahDoaScreen.kt",
        ).forEach { assertTrue("$it tidak terbaca sebagai layar bermode baca jarak jauh", it in nama) }
        // Kompas Kiblat memang punya mode layar penuh, tetapi tidak menampilkan teks Arab: ia tidak
        // boleh ikut dituntut menaikkan ukuran bacaan.
        assertTrue("KiblatScreen dituntut menaikkan ukuran bacaan padahal tak menampilkan teks Arab",
            "KiblatScreen.kt" !in nama)
    }

    /** Aturan bersama harus tetap murni: tanpa Android, agar dapat diuji langsung di JVM. */
    @Test
    fun aturanBacaJauhTidakMenyentuhAndroid() {
        val aturan = KodeSumber.sumber("src/main/java/id/or/kemenag/yasintahlil/core/util/UkuranBacaan.kt")
        assertTrue("UkuranBacaan mengimpor Android sehingga tak dapat diuji di JVM",
            !aturan.contains("import android."))
    }
}
