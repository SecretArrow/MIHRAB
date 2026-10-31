package id.or.kemenag.yasintahlil.feature.khataman

import id.or.kemenag.yasintahlil.feature.tasbih.TasbihHarian
import java.time.LocalDate

/**
 * Hitungan rangkaian hari baca (streak) milik layar Khataman & Streak — murni Kotlin tanpa
 * Android, teruji langsung di JVM seperti `ZakatHitung` dan `TasbihHarian`.
 *
 * Bahan hitungannya adalah baris tabel `kegiatan` ([id.or.kemenag.yasintahlil.core.data.local.KegiatanEntity]):
 * membuka layar Yasin/Tahlil masing-masing dicatat sebagai [YASIN_BUKA]/[TAHIL_BUKA] sekali per
 * hari, dan menuntaskan khataman dicatat sebagai [KHATAM_YASIN]. Nilai konstanta di bawah adalah
 * ejaan **persis** kolom `jenis` di tabel itu — baris lama dan baru harus saling mengenali, jadi
 * jangan menyalin ejaannya ke tempat lain.
 */
object RangkaianHari {

    /** Jenis kegiatan: layar Surat Yasin dibuka pada hari itu (dicatat otomatis, sekali per hari). */
    const val YASIN_BUKA = "yasin_buka"

    /**
     * Jenis kegiatan: layar Tahlil dibuka pada hari itu. Nama konstantanya tanpa "i" kedua, tetapi
     * nilainya mengeja "tahlil" lengkap — persis kolom `jenis` yang tersimpan, sebab nilainyalah
     * yang dibandingkan di tabel, bukan nama konstantanya.
     */
    const val TAHIL_BUKA = "tahlil_buka"

    /** Jenis kegiatan: khataman Yasin dituntaskan pada hari itu (dicatat sendiri oleh pengguna). */
    const val KHATAM_YASIN = "khatam_yasin"

    /**
     * Bentuk simpan tanggal `yyyy-MM-dd` — **dirujuk** dari [TasbihHarian.iso], bukan disalin
     * menjadi implementasi kedua: bentuk ini sekaligus isi kolom `tanggal` di tabel `kegiatan`,
     * dan dua implementasi yang berbeda diam-diam akan menyimpan satu hari sebagai dua tanggal
     * yang tak saling mengenali.
     */
    fun iso(tanggal: LocalDate): String = TasbihHarian.iso(tanggal)

    /**
     * Panjang rangkaian hari baca yang sedang berjalan: hari-hari berturut-turut yang berakhir
     * pada [hariIni], **atau** pada kemarin bila hari ini belum ada aktivitas — rangkaian yang
     * belum putus tidak boleh terbaca lenyap hanya karena pengguna membuka layar ini sebelum
     * sempat membaca. Dua hari kosong beruntun (hari ini dan kemarin) mengembalikan 0.
     *
     * Sebuah lubang memutus rangkaian: bila kemarin kosong, hari ini adalah rangkaian baru
     * panjangnya satu, dan rangkaian lama di depannya hanya hidup di [terpanjang]. [hariIni] yang
     * tak terbaca dikembalikan 0 — pengguna melihat "0", bukan layar gagal.
     */
    fun rangkaian(tanggal: List<String>, hariIni: String): Int {
        val ada = tanggal.toSet()
        val hari = runCatching { LocalDate.parse(hariIni) }.getOrNull() ?: return 0
        // Titik akhir rangkaian: hari ini bila ada aktivitasnya; kalau belum, kemarin masih
        // dihitung hidup. Selisih keduanya satu hari saja — dua hari kosong beruntun = putus.
        val akhir = when {
            iso(hari) in ada -> hari
            iso(hari.minusDays(1)) in ada -> hari.minusDays(1)
            else -> return 0
        }
        var jumlah = 0
        var kursor = akhir
        while (iso(kursor) in ada) {
            jumlah++
            kursor = kursor.minusDays(1)
        }
        return jumlah
    }

    /**
     * Rangkaian terpanjang yang pernah tercatat — termasuk yang sudah lama putus, karena yang
     * ditunjukkan kepada pengguna adalah rekor pribadinya, bukan keadaan hari ini. Uraian tanggal
     * yang tak terbaca dan tanggal yang berulang diabaikan: baris lain tetap terhitung, bukan
     * ikut gagal bersamanya.
     */
    fun terpanjang(tanggal: List<String>): Int {
        val urut = tanggal
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .distinct()
            .sorted()
        if (urut.isEmpty()) return 0
        var terpanjang = 1
        var kini = 1
        for (i in 1 until urut.size) {
            kini = if (urut[i] == urut[i - 1].plusDays(1)) kini + 1 else 1
            if (kini > terpanjang) terpanjang = kini
        }
        return terpanjang
    }
}
