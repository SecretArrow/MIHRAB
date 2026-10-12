package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.core.data.PrayerTimes
import java.util.TimeZone

/**
 * Aturan bersama untuk menyusun jadwal sholat dari **nilai preferensi mentah**.
 *
 * Sebelumnya setiap pemakai aturan ini menyalinnya sendiri-sendiri — `PrayerScheduler`,
 * `JadwalViewModel`, dan `HomeViewModel` masing-masing punya `parseCorr` yang identik, dan
 * faktor bayangan Asar ("hanafi" → 2) serta pemilihan zona waktu juga ditulis ulang di tiap
 * tempat. Tiga salinan dari satu aturan berarti tiga kesempatan untuk berbeda: satu layar dapat
 * menghitung Asar dengan sudut yang lain daripada yang dipakai pekerja latar, dan pengingatnya
 * pun berbunyi pada menit yang tidak sama dengan yang ditampilkan layar.
 *
 * Berkas ini tidak menyentuh Android sama sekali, sehingga aturannya dapat diuji langsung
 * ([JadwalHariIniTest]) tanpa perangkat maupun emulator.
 */
object JadwalHariIni {

    /**
     * "1,0,1,…" → enam angka koreksi menit, urut `subuh, terbit, zuhur, asar, magrib, isya`.
     *
     * Panjangnya selalu tepat enam: entri yang hilang diisi nol, kelebihan dipotong. Tanpa
     * penjagaan itu satu koma yang salah ketik menggeser seluruh indeks, dan koreksi Asar akan
     * diterapkan pada Zuhur.
     */
    fun parseCorr(csv: String): List<Int> =
        (csv.split(",").map { it.trim().toIntOrNull() ?: 0 } + List(6) { 0 }).take(6)

    /** Apakah mazhab Asar yang dipilih memakai faktor bayangan dua (Hanafi). */
    fun asarHanafi(asarKey: String): Boolean = asarKey == "hanafi"

    /** Faktor bayangan Asar: 2 untuk Hanafi, 1 untuk jumhur (Syafi'i, Maliki, Hambali). */
    fun asarFactor(asarKey: String): Double = if (asarHanafi(asarKey)) 2.0 else 1.0

    /** Zona waktu dari id tersimpan; kosong atau tak dikenal = zona waktu perangkat. */
    fun zona(tzId: String): TimeZone =
        if (tzId.isBlank()) TimeZone.getDefault() else TimeZone.getTimeZone(tzId)

    /**
     * Parameter hisab dari nilai preferensi mentah. Semua pemanggil memakai ini agar jadwal yang
     * ditampilkan layar, yang dipakai penjadwal pengingat, dan yang dihitung beranda selalu sama.
     */
    fun params(
        lat: Double,
        lon: Double,
        methodKey: String,
        asarKey: String,
        corrCsv: String,
        tz: TimeZone,
    ): PrayerCalc.Params = params(
        lat = lat, lon = lon, method = PrayerCalc.Method.from(methodKey),
        asarHanafi = asarHanafi(asarKey), corr = parseCorr(corrCsv), tz = tz,
    )

    /** Varian untuk pemanggil yang sudah memegang nilai terurai (mis. state `ViewModel`). */
    fun params(
        lat: Double,
        lon: Double,
        method: PrayerCalc.Method,
        asarHanafi: Boolean,
        corr: List<Int>,
        tz: TimeZone,
    ): PrayerCalc.Params = PrayerCalc.Params(
        lat = lat, lon = lon, method = method,
        asarFactor = if (asarHanafi) 2.0 else 1.0, corr = corr, tz = tz,
    )

    /**
     * Jam "HH:mm" yang sama, dimajukan/dimundurkan [menit] dengan **memutar melewati tengah
     * malam**: pengingat sebelum Subuh kerap jatuh pada hari sebelumnya, dan jam "-10 menit dari
     * 00:05" harus terbaca 23:55 — bukan angka negatif atau waktu yang tidak ada.
     */
    fun geser(hhmm: String, menit: Int): String {
        val total = ((PrayerCalc.toMinutes(hhmm) + menit) % 1440 + 1440) % 1440
        return "%02d:%02d".format(total / 60, total % 60)
    }

    /** Jam tiap waktu, dikenali dari namanya — sehingga urutan daftar nama yang menentukan. */
    private fun jamPerNama(t: PrayerTimes): Map<String, String> = mapOf(
        "Subuh" to t.subuh, "Terbit" to t.terbit, "Zuhur" to t.zuhur,
        "Asar" to t.asar, "Magrib" to t.magrib, "Isya" to t.isya,
    )

    /** Lima waktu yang beradzan (Terbit tidak; matahari terbit bukan waktu sholat). */
    fun adzan(t: PrayerTimes): List<String> = LIMA_WAKTU.map { jamPerNama(t).getValue(it) }

    /**
     * Nama lima waktu beradzan, **berurutan**. Urutannya bukan hiasan: `Store.ADZAN_ON` adalah CSV
     * lima angka yang indeksnya berarti waktu ke-`i` dari daftar ini, dan [adzan] mengembalikan jam
     * dengan urutan yang sama. Dahulu daftar ini ditulis ulang di `PrayerScheduler` dan di layar
     * Adzan — bila keduanya berbeda urutan, mematikan "Magrib" akan mematikan waktu lain tanpa
     * satu pun pesan galat.
     */
    val LIMA_WAKTU = listOf("Subuh", "Zuhur", "Asar", "Magrib", "Isya")

    /**
     * Nama enam waktu untuk koreksi menit, berurutan. Indeksnya sejajar dengan CSV `Store.CORR`
     * yang diurai [parseCorr] (subuh, terbit, zuhur, asar, magrib, isya): satu koma yang salah
     * ketik menggeser seluruh indeks, dan koreksi Asar akan diterapkan pada Zuhur.
     */
    val ENAM_WAKTU = listOf("Subuh", "Terbit", "Zuhur", "Asar", "Magrib", "Isya")

    /** Jam keenam waktu, sejajar dengan [ENAM_WAKTU] — dasar tampilan koreksi menit. */
    fun semua(t: PrayerTimes): List<String> = ENAM_WAKTU.map { jamPerNama(t).getValue(it) }

    /**
     * Jam untuk satu [nama] waktu, atau `null` bila namanya bukan salah satu dari [ENAM_WAKTU].
     * Pemanggil yang menerima nama dari [PrayerCalc.next] memakainya agar pemetaan nama→jam tidak
     * ditulis ulang sebagai rangkaian `when` yang dapat menyesatkan ke waktu yang salah.
     */
    fun jam(t: PrayerTimes, nama: String): String? = jamPerNama(t)[nama]
}
