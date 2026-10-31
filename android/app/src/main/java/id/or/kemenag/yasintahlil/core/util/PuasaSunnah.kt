package id.or.kemenag.yasintahlil.core.util

import java.time.DayOfWeek

/**
 * Puasa sunnah dan hari besar Islam dari hasil konversi [HijriCalc] — murni, tanpa Android,
 * agar aturan tanggalnya dapat diuji langsung di JVM (pola [InfoHarian]).
 *
 * Yang diminta masuk daftar [puasaSunnah] hanyalah puasa **sunnah**; Ramadan sengaja tidak
 * diberi label puasa karena puasanya wajib, bukan sunnah — awalnya cukup ditandai sebagai hari
 * besar oleh [hariBesar]. Satu hari boleh memuat lebih dari satu label (mis. Senin yang jatuh
 * tanggal 13 Syawal): semuanya ditampilkan, urut dari yang mingguan (Senin/Kamis) lalu yang
 * tanggalan, sehingga layar selalu menyebut yang paling dekat dengan niat harian pengguna
 * lebih dahulu.
 *
 * **Batas akurasi.** Seluruh label ini mengikuti kalender Hijriah **tabular** [HijriCalc]:
 * tanggal 13/14/15, 9/10 Muharram, dsb. bisa berselisih 1–2 hari dengan penetapan sidang isbat
 * Kemenag — selaras disclaimer HijriCalc, layar memakai kata "perkiraan" dan tidak pernah
 * mengaku taraf resmi.
 */
object PuasaSunnah {

    /**
     * Daftar puasa sunnah pada satu hari.
     *
     * @param hariH tanggal Hijriah (1..30) dari [HijriCalc.from].
     * @param bulanH bulan Hijriah (1..12) dari [HijriCalc.from].
     * @param hariMinggu nomor hari **dalam minggu** mengikuti [DayOfWeek.getValue]: 1 = Senin,
     *   …, 7 = Minggu. Namanya memang "hari minggu" (hari dalam pekan), bukan hari Minggu —
     *   nomornya tetap menuntut [DayOfWeek] agar Senin/Kamis tak tertukar antarlayar.
     * @return label puasa sunnah hari itu, bisa kosong; urut: Senin/Kamis, Ayyamul Bidh
     *   (13–15 tiap bulan), Tasu'a (9 Muharram), Asyura (10 Muharram), Arafah (9 Zulhijah),
     *   lalu rangkaian enam hari Syawal (2..30 Syawal — tanggal 1 adalah Idul Fitri, hari
     *   yang haram berpuasa).
     */
    fun puasaSunnah(hariH: Int, bulanH: Int, hariMinggu: Int): List<String> {
        val hasil = mutableListOf<String>()
        if (hariMinggu == DayOfWeek.MONDAY.value) hasil += "Puasa Senin"
        if (hariMinggu == DayOfWeek.THURSDAY.value) hasil += "Puasa Kamis"
        if (hariH in 13..15) hasil += "Puasa Ayyamul Bidh"
        if (hariH == 9 && bulanH == 1) hasil += "Puasa Tasu'a"
        if (hariH == 10 && bulanH == 1) hasil += "Puasa Asyura"
        if (hariH == 9 && bulanH == 12) hasil += "Puasa Arafah"
        if (bulanH == 10 && hariH in 2..30) hasil += "Rangkaian puasa Syawal"
        return hasil
    }

    /**
     * Nama hari besar Islam pada tanggal Hijriah itu, atau `null` bila bukan hari besar:
     * 1 Muharram Tahun Baru Hijriah, 10 Muharram Hari Asyura, 12 Rabiulawal Maulid Nabi,
     * 27 Rajab Isra Mikraj, 15 Syakban Nisfu Syakban, 1 Ramadan Awal Ramadan, 1 Syawal
     * Idul Fitri, 10 Zulhijah Idul Adha.
     *
     * Mengikuti Hijriah tabular (lihat KDoc objek ini): penetapan resminya tetap sidang isbat
     * Kemenag.
     */
    fun hariBesar(hariH: Int, bulanH: Int): String? = when {
        hariH == 1 && bulanH == 1 -> "Tahun Baru Hijriah"
        hariH == 10 && bulanH == 1 -> "Hari Asyura"
        hariH == 12 && bulanH == 3 -> "Maulid Nabi"
        hariH == 27 && bulanH == 7 -> "Isra Mikraj"
        hariH == 15 && bulanH == 8 -> "Nisfu Syakban"
        hariH == 1 && bulanH == 9 -> "Awal Ramadan"
        hariH == 1 && bulanH == 10 -> "Idul Fitri"
        hariH == 10 && bulanH == 12 -> "Idul Adha"
        else -> null
    }
}
