package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.KodeSumber
import id.or.kemenag.yasintahlil.core.data.PrayerTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Aritmetika "menit ke hari" hidup di satu tempat — bukan `1440` telanjang yang diketik ulang.
 *
 * Angka `1440` dahulu muncul di tiga berkas sebagai bilangan sihir: `PrayerCalc.compute` melipat
 * hasil hisab dengannya, `JadwalHariIni.geser`/`berikutnya` memutar jam melewati tengah malam
 * dengannya, dan `PrayerScheduler.enqueueNext` menghitung jeda pengingat dengannya
 * (`if (d <= 0) d += 1440`, ditulis **dua kali** dalam satu fungsi). Yang dipertaruhkan bukan
 * kerapian: jeda itulah yang menentukan **kapan pengingat berbunyi**, sementara jam yang sama
 * menentukan **apa yang terbaca pengguna**. Satu salah ketik — `1439`, atau lipatan yang lupa
 * menangani sisa bagi negatif — membuat pengingat berbunyi pada menit yang tidak sama dengan jam
 * di layar, tanpa satu pun pesan galat.
 *
 * Uji ini menjaga tiga hal: (1) angkanya satu, (2) lipatannya satu, dan (3) aturan "jeda ke
 * kemunculan berikutnya" satu — termasuk kasus tepi yang mudah salah: jam yang **tepat** sekarang
 * harus dibulatkan ke depan, bukan menjadi jeda nol.
 */
class MenitHariSatuTempatTest {

    private fun root(): File = KodeSumber.root()

    private fun berkasMain(): List<File> =
        File(root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun kode(f: File): String = KodeSumber.kodeTanpaKomentar(f.readText())

    private fun sumber(relatif: String): String =
        kode(File(root(), "src/main/java/id/or/kemenag/yasintahlil/$relatif"))

    // ---- Angkanya satu --------------------------------------------------------------------------

    /** Menit sehari dipaku, dan tak ada berkas lain yang mengejanya sendiri. */
    @Test
    fun menitSehariHanyaDitulisDiSatuBerkas() {
        assertEquals("menit sehari bukan 1440", 1440, PrayerCalc.MENIT_SEHARI)
        val pemakai = berkasMain()
            .filter { kode(it).contains("1440") }
            .map { it.name }.sorted()
        assertEquals("angka 1440 diketik ulang di luar PrayerCalc.MENIT_SEHARI",
            listOf("PrayerCalc.kt"), pemakai)
    }

    /** Ketiga pemakai lama memakai aturan bersama, bukan mengejanya lagi. */
    @Test
    fun pemakaiLamaMemakaiAturanBersama() {
        assertTrue("PrayerScheduler tidak memakai PrayerCalc.menitHari untuk menit dinding",
            sumber("core/scheduling/PrayerScheduler.kt").contains("PrayerCalc.menitHari("))
        // Jeda waktu masuk harus memakai aturan bersama untuk **jam itu**, bukan hanya salah satu
        // pemakaian di berkas yang sama: `sisaMenit(jam, nowMin)` dapat diganti `+ MENIT_SEHARI`
        // yang sama sekali tak melipat, dan penjaga yang longgar tetap hijau karena baris pra-adzan
        // di bawahnya masih memanggil aturan itu.
        assertTrue("PrayerScheduler tidak memakai JadwalHariIni.sisaMenit(jam, nowMin) untuk jeda adzan",
            sumber("core/scheduling/PrayerScheduler.kt").contains("JadwalHariIni.sisaMenit(jam, nowMin)"))
        assertTrue("PrayerScheduler tidak memakai JadwalHariIni.geser untuk jam pra-adzan",
            sumber("core/scheduling/PrayerScheduler.kt").contains("JadwalHariIni.geser("))
        assertTrue("JadwalHariIni.geser tidak memakai PrayerCalc.putaranMenit",
            sumber("core/util/JadwalHariIni.kt").contains("PrayerCalc.putaranMenit("))
        // `berikutnya` dan `geser` memakai aturan yang sama; mengejanya kembali sebagai
        // `+ MENIT_SEHARI` menghasilkan angka yang sama persis, jadi hanya pemakaian aturan
        // bersamanya yang dapat dijaga — dan itulah yang membuat keduanya tak dapat menyimpang.
        assertTrue("JadwalHariIni.berikutnya tidak memakai sisaMenit untuk putaran tengah malam",
            sumber("core/util/JadwalHariIni.kt").contains("sisaMenit(hhmm, nowMin)"))
    }

    /**
     * Layar pun membaca menit dinding lewat aturan yang sama. Dua layar yang menampilkan hitung
     * mundur "Berikutnya: …" dahulu mengeja `h * 60 + m` sendiri — aritmetika yang sama dengan
     * [PrayerCalc.menitHari], tetapi salinan yang dapat berbeda tanpa satu pun pesan galat, sebab
     * jam yang dipakai menghitung mundur tak lagi dijamin sama dengan jam yang dijadwalkan.
     */
    @Test
    fun layarMemakaiMenitHariBersama() {
        listOf(
            "feature/home/HomeScreen.kt",
            "feature/jadwal/JadwalViewModel.kt",
        ).forEach { rel ->
            assertTrue("$rel tidak memakai PrayerCalc.menitHari untuk menit dinding",
                sumber(rel).contains("PrayerCalc.menitHari("))
        }
        val pengeja = berkasMain()
            .filter { Regex("""HOUR_OF_DAY\)\s*\*\s*60""").containsMatchIn(kode(it)) }
            .map { it.name }.sorted()
        assertEquals("menit dinding dieja sendiri (h * 60 + m) di luar PrayerCalc.menitHari",
            emptyList<String>(), pengeja)
    }

    // ---- Lipatannya satu ------------------------------------------------------------------------

    /** Lipatan selalu mendarat di `0..1439`, termasuk untuk nilai negatif dan lebih dari sehari. */
    @Test
    fun putaranMenitMelipatKeRentangHari() {
        assertEquals(0, PrayerCalc.putaranMenit(0))
        assertEquals(1439, PrayerCalc.putaranMenit(1439))
        assertEquals(0, PrayerCalc.putaranMenit(1440))
        assertEquals(1, PrayerCalc.putaranMenit(1441))
        // Sisa bagi Kotlin bernilai negatif: tanpa penambahan sehari lebih dulu, "-10 menit dari
        // 00:05" menjadi angka negatif alih-alih 23:55.
        assertEquals(1439, PrayerCalc.putaranMenit(-1))
        assertEquals(0, PrayerCalc.putaranMenit(-1440))
        assertEquals(1439, PrayerCalc.putaranMenit(-1441))
        assertEquals(5, PrayerCalc.putaranMenit(2 * 1440 + 5))
    }

    @Test
    fun putaranMenitSelaluDalamSatuHari() {
        (-2 * 1440..2 * 1440).forEach { m ->
            val p = PrayerCalc.putaranMenit(m)
            assertTrue("putaranMenit($m) = $p di luar 0..1439", p in 0 until 1440)
            assertEquals("putaranMenit tidak idempoten pada $m", p, PrayerCalc.putaranMenit(p))
        }
    }

    // ---- Menit dinding satu aturan --------------------------------------------------------------

    /**
     * Membaca jam dari kalender dan dari teks "HH:mm" harus memberi angka yang sama.
     *
     * Nilai mutlaknya dipaku lebih dulu: membandingkan [PrayerCalc.menitHari] dengan
     * [PrayerCalc.toMinutes] saja **hampa**, karena `toMinutes` memang didefinisikan lewat
     * `menitHari` — keduanya salah bersama-sama tanpa ketahuan. Oracle yang bebas adalah
     * [FormatWaktu.pasanganAngka], yang hanya mengeja dan tak tahu apa pun tentang menit.
     */
    @Test
    fun menitHariSepakatDenganToMinutes() {
        assertEquals(0, PrayerCalc.menitHari(0, 0))
        assertEquals(305, PrayerCalc.menitHari(5, 5))
        assertEquals(600, PrayerCalc.menitHari(10, 0))
        assertEquals(1439, PrayerCalc.menitHari(23, 59))
        (0 until 24).forEach { h ->
            (0 until 60).forEach { m ->
                assertEquals("menitHari($h, $m) tidak sejalan dengan toMinutes",
                    FormatWaktu.pasanganAngka(h, m), FormatWaktu.jamMenit(PrayerCalc.menitHari(h, m)))
                assertEquals(PrayerCalc.menitHari(h, m),
                    PrayerCalc.toMinutes(FormatWaktu.pasanganAngka(h, m)))
            }
        }
    }

    /** `geser` adalah lipatan itu sendiri — jam yang digeser tak dapat berbeda lipatan. */
    @Test
    fun geserMemakaiPutaranMenit() {
        assertEquals("23:55", JadwalHariIni.geser("00:05", -10))
        assertEquals("12:00", JadwalHariIni.geser("12:00", 1440))
        listOf("00:05" to -10, "23:55" to 10, "04:35" to 0, "12:00" to -1440).forEach { (jam, d) ->
            assertEquals("geser(\"$jam\", $d) tidak memakai PrayerCalc.putaranMenit",
                FormatWaktu.jamMenit(PrayerCalc.putaranMenit(PrayerCalc.toMinutes(jam) + d)),
                JadwalHariIni.geser(jam, d))
        }
    }

    // ---- Jeda ke kemunculan berikutnya ----------------------------------------------------------

    /**
     * Jeda selalu `1..1440`. Jam yang **tepat** sekarang dijawab dengan kemunculan **besok**, bukan
     * nol: pekerja pengingat menjadwalkan ulang dirinya sesudah berbunyi, sehingga jeda nol berarti
     * mengantre ulang pada menit yang sama — pengingat yang berbunyi tanpa henti.
     */
    @Test
    fun sisaMenitSelaluKeDepan() {
        assertEquals("tepat sekarang harus dibulatkan ke depan", 1440, JadwalHariIni.sisaMenit("05:00", 300))
        assertEquals(1, JadwalHariIni.sisaMenit("05:01", 300))
        assertEquals(1, JadwalHariIni.sisaMenit("05:00", 299))
        assertEquals(1439, JadwalHariIni.sisaMenit("04:59", 300))
        (0 until 1440).forEach { nowMin ->
            listOf("00:00", "05:00", "12:00", "23:59").forEach { jam ->
                val s = JadwalHariIni.sisaMenit(jam, nowMin)
                assertTrue("sisaMenit(\"$jam\", $nowMin) = $s di luar 1..1440", s in 1..1440)
            }
        }
    }

    /** Jeda dan jam tujuan harus konsisten: menjadwalkan [sisa] menit benar-benar tiba di jamnya. */
    @Test
    fun sisaMenitMendaratDiJamnya() {
        listOf("04:35", "11:55", "19:10").forEach { jam ->
            listOf(0, 1, 300, 700, 1380, 1439).forEach { nowMin ->
                val s = JadwalHariIni.sisaMenit(jam, nowMin)
                val tiba = FormatWaktu.jamMenit(PrayerCalc.putaranMenit(nowMin + s))
                assertEquals("sisaMenit(\"$jam\", $nowMin) mendarat di jam lain", jam, tiba)
            }
        }
    }

    /** `berikutnya` memakai aturan putaran yang sama, bukan `+ 1440` sendiri. */
    @Test
    fun berikutnyaMemakaiAturanYangSama() {
        val t = PrayerTimes("04:35", "05:50", "11:55", "15:15", "17:58", "19:10")
        assertEquals("Subuh", JadwalHariIni.berikutnya(PrayerCalc.toMinutes("23:00"), t).nama)
        assertEquals(JadwalHariIni.sisaMenit("04:35", PrayerCalc.toMinutes("23:00")),
            JadwalHariIni.berikutnya(PrayerCalc.toMinutes("23:00"), t).sisaMenit)
        assertEquals("Zuhur", JadwalHariIni.berikutnya(PrayerCalc.toMinutes("11:50"), t).nama)
        assertEquals(5, JadwalHariIni.berikutnya(PrayerCalc.toMinutes("11:50"), t).sisaMenit)
        // Tepat sekarang sudah terhitung lewat: yang dijawab waktu berikutnya.
        assertEquals("Asar", JadwalHariIni.berikutnya(PrayerCalc.toMinutes("11:55"), t).nama)
        assertEquals(200, JadwalHariIni.berikutnya(PrayerCalc.toMinutes("11:55"), t).sisaMenit)
    }

    /** Jam pra-adzan dijadwalkan dari aturan geser yang sama dengan yang ditampilkan layar. */
    @Test
    fun jedaPraAdzanDariJamYangDitampilkan() {
        val t = PrayerTimes("04:35", "05:50", "11:55", "15:15", "17:58", "19:10")
        listOf("04:35", "19:10").forEach { jam ->
            listOf(0, 250, 700, 1439).forEach { nowMin ->
                listOf(5, 10, 30).forEach { preMin ->
                    val pra = JadwalHariIni.geser(jam, -preMin)
                    val jeda = JadwalHariIni.sisaMenit(pra, nowMin)
                    val tiba = FormatWaktu.jamMenit(PrayerCalc.putaranMenit(nowMin + jeda))
                    assertEquals("jeda pra-adzan $jam ($preMin mnt) dari $nowMin tidak tiba di $pra",
                        pra, tiba)
                }
            }
        }
    }
}
