package id.or.kemenag.yasintahlil.feature.tasbih

import java.time.LocalDate
import java.util.Locale

/**
 * Penolong tanggal harian Tasbih Digital: bentuk simpan `yyyy-MM-dd` dan label pendeknya untuk
 * daftar riwayat ("Hari ini", "Kemarin", "15 Okt").
 *
 * Murni tanpa Android supaya aturan labelnya dapat diuji sebagai unit test biasa — pola yang sama
 * dengan `InfoHarian`. Tanggal disimpan sebagai string bentuk tetap (bukan milidetik) karena ia
 * sekaligus kunci tabel `tasbih`: satu baris per hari, ditimpa tiap ketukan.
 */
object TasbihHarian {

    /**
     * Singkatan bulan Indonesia, urutannya mengikuti `LocalDate.monthValue` (1 = Januari).
     * Sengaja tidak memakai `Locale` Indonesia bawaan Java: singkatannya ("Sept", "Okt") tidak
     * seragam panjang dan bentuk label ini dibaca pengguna setiap hari, jadi dikunci di sini
     * supaya tidak berubah mengikuti perangkat.
     */
    val bulanPendek = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
    )

    /**
     * Bentuk simpan tanggal: `yyyy-MM-dd` dengan [Locale.US], sehingga angkanya selalu digit biasa
     * di segala perangkat dan urutannya sebagai teks sama dengan urutannya sebagai tanggal.
     */
    fun iso(tanggal: LocalDate): String =
        "%04d-%02d-%02d".format(Locale.US, tanggal.year, tanggal.monthValue, tanggal.dayOfMonth)

    /**
     * Label pendek satu baris riwayat: "Hari ini" bila [iso] sama dengan [hariIniIso], "Kemarin"
     * bila tepat sehari sebelumnya, selain itu "15 Okt" — cukup untuk membedakan baris riwayat
     * 7 hari tanpa menuliskan tahunnya.
     *
     * Tanggal yang tidak terbaca dikembalikan apa adanya: baris riwayat tetap tampil, hanya tanpa
     * label ramahnya, bukan hilang atau membuat layar gagal.
     */
    fun label(iso: String, hariIniIso: String): String {
        if (iso == hariIniIso) return "Hari ini"
        val tanggal = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
        val hariIni = runCatching { LocalDate.parse(hariIniIso) }.getOrNull() ?: return iso
        if (tanggal == hariIni.minusDays(1)) return "Kemarin"
        return "${tanggal.dayOfMonth} ${bulanPendek[tanggal.monthValue - 1]}"
    }
}
