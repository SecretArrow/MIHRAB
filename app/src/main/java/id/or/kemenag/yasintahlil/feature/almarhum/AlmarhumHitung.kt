package id.or.kemenag.yasintahlil.feature.almarhum

import java.time.LocalDate
import java.util.Locale

/**
 * Hitungan peringatan tahlil almarhum — murni Kotlin tanpa Android, agar teruji langsung di JVM
 * seperti `ZakatHitung` milik Kalkulator Zakat dan `TasbihHarian` milik Tasbih Digital.
 *
 * Hari ke-7, ke-40, ke-100, dan ke-1000 adalah tradisi masyarakat Indonesia untuk menghimpun doa
 * bagi yang telah wafat (tahlil/arwahan) — bukan ketentuan syariat: jumlah dan harinya berbeda
 * antarkeluarga dan antardaerah, dan pengguna dari kalangan mana pun bebas menggunakannya atau
 * tidak. [KETERANGAN_TRADISI] menyimpan batas tanggung jawab itu sebagai satu kalimat yang sama
 * untuk semua tempat yang menyebutnya — nada yang sama dengan `PengingatAdzan.DOA_BUKAN_SYARIAT`
 * pada pengingat iqomah.
 *
 * Haul dihitung per tahun kalender (`plusYears`), bukan per 354 hari: yang diperingati keluarga di
 * Indonesia adalah tanggal wafatnya menurut kalender yang dipakai sehari-hari, sehingga pengingat
 * ini dan catatan keluarga menyebut hari yang sama.
 */
object AlmarhumHitung {

    /**
     * Batas tanggung jawab aplikasi atas daftar peringatan ini — satu kalimat untuk semua tempat
     * yang menyebutnya (layar ini dan kelak layar/keterangan lain), supaya tidak ada salinan yang
     * dapat menyimpang diam-diam.
     */
    const val KETERANGAN_TRADISI =
        "Hari ke-7, 40, 100, dan 1000 beserta haul adalah tradisi masyarakat Indonesia untuk " +
            "menghimpun doa bagi yang telah wafat — tradisi turun-temurun, bukan ketetapan agama; " +
            "siapa pun bebas menggunakannya atau tidak."

    /** Hari-hari pengingat menurut tradisi tahlil di masyarakat Indonesia, dihitung sejak hari wafat. */
    private val HARI_TRADISI = longArrayOf(7, 40, 100, 1000)

    /** Banyak haul tahunan pertama yang disiapkan untuk satu almarhum. */
    const val JUMLAH_HAUL = 3

    /** Panjang bentuk simpan "yyyy-MM-dd" (tahun empat digit); isian dialog dibatasi selewat ini. */
    const val ISO_PANJANG = 10

    /**
     * Nama bulan Indonesia, urutannya mengikuti `LocalDate.monthValue` (1 = Januari). Sengaja
     * ditulis manual, bukan memakai `Locale` Indonesia bawaan Java: bentuk ini dibaca pengguna
     * pada hari yang menyakitkan, jadi tidak boleh berubah mengikuti pengaturan perangkat — pola
     * yang sama dengan `TasbihHarian.bulanPendek`.
     */
    val bulan = arrayOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember",
    )

    /**
     * Satu peringatan yang dihitung dari tanggal wafat: hari ke-N menurut tradisi, atau haul
     * tahunan ke-N. [hariKe] `null` hanya untuk haul — ia dihitung per tahun, bukan per hari.
     */
    data class Peringatan(
        /** Nama yang tampil kepada pengguna, mis. "Hari ke-40" atau "Haul ke-2". */
        val label: String,
        /** Hari pelaksanaannya menurut kalender Masehi. */
        val tanggal: LocalDate,
        /** Hari ke-sejak wafat; `null` untuk haul. */
        val hariKe: Long?,
    )

    /**
     * Tanggal [hari] hari setelah [tanggal]. Aturan "hari ke-N = hari wafat ditambah N hari"
     * (hari wafat sendiri adalah hari ke-0) hidup di satu tempat ini saja: layar dan pekerja
     * pengingat memanggil fungsi yang sama, dan bila ada keluarga yang kebetulan menghitungnya
     * berbeda, hanya fungsi inilah yang perlu berubah.
     */
    fun tambahHari(tanggal: LocalDate, hari: Long): LocalDate = tanggal.plusDays(hari)

    /**
     * Seluruh peringatan satu almarhum: hari ke-7/40/100/1000 menurut tradisi tahlil Indonesia,
     * plus [JUMLAH_HAUL] haul tahunan pertama. Hasilnya **terurut naik menurut tanggalnya** —
     * hari ke-1000 (sekitar dua tahun sembilan bulan) jatuh di antara haul ke-2 dan ke-3, jadi
     * urutan lain (mis. hari dulu baru haul) akan membuat [berikutnya] melompati haul yang lebih
     * dekat.
     */
    fun peringatan(wafat: LocalDate): List<Peringatan> {
        val hari = HARI_TRADISI.map { n -> Peringatan("Hari ke-$n", tambahHari(wafat, n), n) }
        val haul = (1..JUMLAH_HAUL).map { n ->
            Peringatan("Haul ke-$n", wafat.plusYears(n.toLong()), null)
        }
        return (hari + haul).sortedBy { it.tanggal }
    }

    /**
     * Peringatan pertama yang belum lewat pada [hariIni]. Bila [hariIni] jatuh tepat pada tanggal
     * sebuah peringatan, peringatan itulah yang dikembalikan — hari-H adalah hari peringatannya,
     * bukan keesokannya. `null` bila seluruh jadwal telah terlewat (wafatnya lebih dari tiga tahun
     * lalu): pemanggil menampilkan daftarnya tanpa menonjolkan apa pun, bukan mengarang peringatan
     * yang memang tidak dihitung.
     */
    fun berikutnya(wafat: LocalDate, hariIni: LocalDate): Peringatan? =
        peringatan(wafat).firstOrNull { it.tanggal >= hariIni }

    /**
     * Bentuk simpan tanggal `yyyy-MM-dd` dengan [Locale.US] — angkanya selalu digit biasa di
     * segala perangkat, dan urutannya sebagai teks sama dengan urutannya sebagai tanggal. Bentuk
     * yang sama dengan `TasbihHarian.iso` karena keduanya menjadi isi kolom tanggal di Room.
     */
    fun iso(tanggal: LocalDate): String =
        "%04d-%02d-%02d".format(Locale.US, tanggal.year, tanggal.monthValue, tanggal.dayOfMonth)

    /**
     * Kebalikan [iso]; `null` bila [s] bukan bentuk `yyyy-MM-dd` yang sah. Pemeriksaannya ketat
     * ("2026-1-1" ditolak) karena teks ini sekaligus kunci tabel: bentuk longgar akan menyimpan
     * satu tanggal sebagai dua baris yang berbeda. Mengembalikan `null` — bukan melempar — karena
     * pemanggilnya (dialog isian, pekerja pengingat) memperlakukan yang tak terbaca sebagai
     * "bukan tanggal", bukan keadaan darurat.
     */
    fun dariIso(s: String): LocalDate? = runCatching { LocalDate.parse(s) }.getOrNull()

    /**
     * Tanggal dalam bentuk Indonesia yang utuh — "17 Agustus 2025". Dipakai kartu almarhum dan
     * teks notifikasi pengingatnya agar keduanya menyebut tanggal yang sama dengan cara yang sama.
     */
    fun tanggalIndo(tanggal: LocalDate): String =
        "${tanggal.dayOfMonth} ${bulan[tanggal.monthValue - 1]} ${tanggal.year}"
}
