package id.or.kemenag.yasintahlil.core.util

import java.util.Calendar

/**
 * Kalender Hijriah tabular (aproksimasi aritmetik). DAPAT BERSELISIH 1-2 HARI
 * dengan penetapan resmi sidang isbat Kemenag — selalu rujuk
 * https://sihat.kemenag.dev/kalender-hijriah untuk Ramadan/Syawal/Zulhijah.
 */
object HijriCalc {
    val bulan = listOf(
        "Muharram", "Safar", "Rabiulawal", "Rabiulakhir", "Jumadilawal",
        "Jumadilakhir", "Rajab", "Syakban", "Ramadan", "Syawal",
        "Zulkaidah", "Zulhijah"
    )

    fun from(date: Calendar = Calendar.getInstance()): Triple<Int, Int, Int> {
        var y = date.get(Calendar.YEAR); var m = date.get(Calendar.MONTH) + 1
        val d = date.get(Calendar.DAY_OF_MONTH)
        var jd = (1461 * (y + 4800 + (m - 14) / 12)) / 4 +
            (367 * (m - 2 - 12 * ((m - 14) / 12))) / 12 -
            (3 * ((y + 4900 + (m - 14) / 12) / 100)) / 4 + d - 32075
        var l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        l = l - 10631 * n + 354
        val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val hm = (24 * l) / 709
        val hd = l - (709 * hm) / 24
        val hy = (30 * n + j - 30).toInt()
        return Triple(hd, hm, hy)
    }

    fun format(date: Calendar = Calendar.getInstance()): String {
        val (d, m, y) = from(date)
        return "$d ${bulan[m - 1]} $y H (perkiraan — ikuti Kemenag)"
    }
}
