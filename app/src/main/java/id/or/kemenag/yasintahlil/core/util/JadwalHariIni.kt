package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.core.data.PrayerTimes
import id.or.kemenag.yasintahlil.core.sholat.Mazhab
import java.time.ZoneId
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
     *
     * Tiap angka dijepit ke [KOREKSI_MIN_MENIT]..[KOREKSI_MAKS_MENIT], sama seperti penjepitan
     * `PengingatAdzan.preMenit`/`iqomahMenit` pada pengingat. Rentang ini dahulu hanya hidup
     * sebagai batas slider di layar Jadwal: nilai tersimpan di luar rentang itu (mis. dari backup
     * yang disunting) dihormati apa adanya oleh seluruh layar dan penjadwal, sehingga jadwal dapat
     * bergeser satu jam lebih jauh daripada yang dapat dipilih pengguna.
     */
    fun parseCorr(csv: String): List<Int> =
        (csv.split(",").map { (it.trim().toIntOrNull() ?: 0).coerceIn(KOREKSI_MIN_MENIT, KOREKSI_MAKS_MENIT) } +
            List(ENAM_WAKTU.size) { 0 }).take(ENAM_WAKTU.size)

    /** Apakah mazhab Asar yang dipilih memakai faktor bayangan dua (Hanafi). */
    fun asarHanafi(asarKey: String): Boolean = asarKey == ASAR_HANAFI

    /** Faktor bayangan Asar: 2 untuk Hanafi, 1 untuk jumhur (Syafi'i, Maliki, Hambali). */
    fun asarFactor(asarKey: String): Double = if (asarHanafi(asarKey)) 2.0 else 1.0

    /**
     * Zona waktu dari id tersimpan; kosong atau tak dikenal = zona waktu perangkat.
     *
     * Aturannya sendiri ada di [zonaId], dan itu disengaja: zona waktu dipakai dalam dua bentuk —
     * `TimeZone` untuk jam beranda, penjadwal pengingat, menu Adzan, dan layar Jadwal; `ZoneId`
     * untuk tanggal butir info harian ([InfoHarian.zona]). `TimeZone.getTimeZone` mengembalikan
     * **GMT** untuk id yang tak dikenal sementara `ZoneId.of` melempar, sehingga id yang rusak
     * (mis. dari pemulihan cadangan) dulu membuat jam beranda dan penjadwal memakai GMT sementara
     * kartu info harian memakai zona perangkat — dua waktu berbeda untuk hari yang sama.
     */
    fun zona(tzId: String): TimeZone = TimeZone.getTimeZone(zonaId(tzId))

    /**
     * Aturan tunggal pemilihan zona waktu tampilan: id kosong **atau tak dikenal** = zona perangkat.
     *
     * Id tak dikenal bukan kasus teoretis: `Store.TZ` ikut backup/pemulihan, jadi nilai dari versi
     * lain dapat masuk tanpa melewati daftar pilihan layar Pengaturan. Zona perangkat adalah
     * tebakan yang jujur; GMT bukan — ia zona yang tak seorang pun pilih.
     */
    fun zonaId(tzId: String): ZoneId =
        if (tzId.isBlank()) ZoneId.systemDefault()
        else runCatching { ZoneId.of(tzId) }.getOrDefault(ZoneId.systemDefault())

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
     *
     * Ejaan jamnya dari [FormatWaktu.jamMenit] — sama dengan yang dipakai `PrayerCalc` saat
     * menghitungnya, supaya jam yang dihitung dan jam yang digeser tak dapat berbeda tulisan.
     */
    fun geser(hhmm: String, menit: Int): String {
        val total = ((PrayerCalc.toMinutes(hhmm) + menit) % 1440 + 1440) % 1440
        return FormatWaktu.jamMenit(total)
    }

    /**
     * Waktu sholat berikutnya pada [t] setelah menit ke-[nowMin], **beserta jamnya**.
     *
     * Nama dan jam sengaja dibawa bersama dalam satu nilai, bukan sebagai nama saja yang pemanggilnya
     * harus mencarikan jamnya sendiri. Dahulu `PrayerCalc.next` mengembalikan nama saja dan menuliskan
     * pasangan nama→jamnya sendiri (`"Subuh" to t.subuh`, …), lalu pemanggilnya mencari jamnya lewat
     * [jam] dengan cabang cadangan `?: times.isya`. Dua daftar enam nama harus sepakat tanpa apa pun
     * yang menjaganya — dan ketika tidak sepakat, hitung mundur beranda menunjuk satu waktu sementara
     * baris jam di bawahnya menampilkan jam waktu lain, tanpa satu pun pesan galat.
     *
     * Urutannya mengikuti [ENAM_WAKTU], daftar yang sama dengan [jam], [semua], [adzan], dan koreksi
     * menit. Terbit ikut karena yang dihitung adalah waktu berikutnya, bukan hanya waktu beradzan.
     *
     * Setelah Isya tak ada lagi waktu pada hari itu, jadi jawabannya Subuh **besok** — dengan putaran
     * melewati tengah malam yang sama seperti [geser].
     */
    fun berikutnya(nowMin: Int, t: PrayerTimes): Berikutnya {
        val berurut = ENAM_WAKTU.zip(semua(t))
        for ((nama, hhmm) in berurut) {
            val sisa = PrayerCalc.toMinutes(hhmm) - nowMin
            if (sisa > 0) return Berikutnya(nama, hhmm, sisa)
        }
        val (nama, hhmm) = berurut.first()
        return Berikutnya(nama, hhmm, PrayerCalc.toMinutes(hhmm) + 1440 - nowMin)
    }

    /** Satu waktu berikutnya: [nama], [jam] tampilannya, dan [sisaMenit] menuju waktunya. */
    data class Berikutnya(val nama: String, val jam: String, val sisaMenit: Int)

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
     * Pemanggil yang sudah memegang **nama** waktu memakainya agar pemetaan nama→jam tidak ditulis
     * ulang sebagai rangkaian `when` yang dapat menyesatkan ke waktu lain.
     */
    fun jam(t: PrayerTimes, nama: String): String? = jamPerNama(t)[nama]

    // ---- Bawaan konfigurasi jadwal ---------------------------------------------------------
    //
    // Nilai-nilai di bawah dahulu ditulis ulang di **sembilan** tempat: `PrayerScheduler`,
    // `HomeViewModel`, `JadwalViewModel`, `KiblatViewModel`, dan layar Adzan masing-masing menuliskan
    // koordinat Jakarta sendiri (`-6.2`/`106.8` sembilan kali), ditambah `"KEMENAG"` (lima kali),
    // `"syafii"` (lima kali), `"Jakarta"` (tiga kali), `"0,0,0,0,0,0"` (empat kali), dan `""` untuk
    // zona waktu (empat kali). Kuncinya memang satu, tetapi bawaannya banyak — dan tak ada satu pun
    // tempat yang tahu berapa bawaannya seharusnya. Bila salah satunya berbeda, layar Jadwal, arah
    // Kiblat, layar Adzan, dan pengingat latar menghitung untuk tempat yang berlainan tanpa satu pun
    // pesan galat; pengguna hanya melihat dua jam yang tidak sama untuk sholat yang sama.
    //
    // Yang paling berbahaya adalah [CORR_DEFAULT]: panjangnya harus tetap sepanjang [ENAM_WAKTU],
    // karena indeksnya berarti waktu ke-`i` — satu koma yang hilang menggeser seluruh koreksi.

    /** Koordinat bawaan bila pengguna belum pernah memilih lokasi: Jakarta. */
    const val LAT_DEFAULT = -6.2
    const val LON_DEFAULT = 106.8

    /** Nama kota bawaan, ditampilkan di Jadwal dan Pengaturan. */
    const val KOTA_DEFAULT = "Jakarta"

    /**
     * Metode hisab bawaan. Sama dengan jatuhan [PrayerCalc.Method.from] untuk kunci tak dikenal,
     * supaya "belum diatur" dan "kunci rusak" menghasilkan jadwal yang sama.
     */
    val METODE_DEFAULT: String = PrayerCalc.Method.KEMENAG.name

    /**
     * Kunci mazhab Asar yang dikenali — **dipinjam** dari kosakata mazhab panduan ([Mazhab]),
     * bukan dieja ulang di sini. Jumhur (Syafi'i, Maliki, Hambali) memakai faktor 1, jadi hanya dua
     * mazhab yang menghasilkan jam Asar berbeda.
     */
    const val ASAR_SYAFII = Mazhab.SYAFII
    const val ASAR_HANAFI = Mazhab.HANAFI
    const val ASAR_DEFAULT = ASAR_SYAFII

    /**
     * Pilihan mazhab Asar sebagai pasangan kunci → nama tampilan, urut untuk ditampilkan.
     *
     * Namanya dahulu dituliskan sendiri-sendiri di **tiga** tempat. Dua di antaranya layar yang
     * keduanya mengatur nilai yang sama (`Store.ASAR`): menu Jadwal menamainya *"Asar Syafi'i"* /
     * *"Asar Hanafi"*, menu Pengaturan *"Syafi'i"* / *"Hanafi"*. Yang ketiga adalah daftar mazhab
     * **panduan** ([Mazhab.SEMUA]) — dan layar Pengaturan menampilkan kedua daftar itu berdampingan
     * di halaman yang sama. Salinan ketiga itulah yang paling mudah lepas: mengubah nama di [Mazhab]
     * tak menyentuh apa pun di sini, sehingga satu layar menawarkan *"Syafi'i"* untuk mazhab panduan
     * dan nama lain untuk mazhab Asar — dua sebutan untuk satu mazhab, tepat bersebelahan. Karena
     * itu daftar ini kini **diturunkan** dari [Mazhab.SEMUA]: hanya nama yang sudah dikenal panduan
     * yang dapat muncul, dan ejaannya dijamin sama. Awalan *"Asar"* tetap milik layar Jadwal — chip
     * di sana berdiri tanpa judul bagian — jadi tiap layar menempelkan awalannya sendiri bila perlu.
     */
    val ASAR_SEMUA: List<Pair<String, String>> =
        Mazhab.SEMUA.filter { it.first == ASAR_SYAFII || it.first == ASAR_HANAFI }

    /** Kunci mazhab Asar untuk faktor bayangan [hanafi] — kebalikan dari [asarHanafi]. */
    fun asarKey(hanafi: Boolean): String = if (hanafi) ASAR_HANAFI else ASAR_SYAFII

    /** Nama mazhab Asar untuk faktor bayangan [hanafi], tanpa awalan *"Asar"*. */
    fun asarLabel(hanafi: Boolean): String =
        ASAR_SEMUA.first { it.first == asarKey(hanafi) }.second

    /**
     * Nama mazhab Asar beserta **faktor bayangannya**, mis. `Syafi'i (1)` — keterangan di kepala
     * layar Jadwal. Angka faktornya dibaca dari [asarFactor], bukan diketik di layar: dahulu kepala
     * layar menuliskan `(1)`/`(2)` langsung, sehingga mengubah faktor bayangan akan membuat
     * keterangan itu menyebut angka yang tidak lagi dipakai perhitungan.
     */
    fun asarLabelFaktor(hanafi: Boolean): String =
        "${asarLabel(hanafi)} (${asarFactor(asarKey(hanafi)).toInt()})"

    /**
     * Sudut satu metode hisab dalam bentuk ringkas, mis. `Fajr 20.0° / Isya 18.0°` — atau
     * `Fajr 18.5° / Isya Magrib+90 mnt` untuk metode yang menetapkan Isya sebagai **selang menit**
     * sesudah Magrib ([PrayerCalc.Method.ishaIntervalMin] terisi, seperti Umm al-Qura dan Makkah).
     *
     * Aturan ini dahulu diketik ulang di tiap layar yang menyebut metode: kepala layar Jadwal
     * menuliskan `Magrib+…mnt`, penjelasan di menu Adzan menuliskan `Isya Magrib+… menit` — dua
     * ejaan untuk satu aturan, dan layar Pengaturan menuliskan metode **Kemenag/SIHAT** secara tetap
     * padahal pengguna dapat memilih metode lain. Karena itu aturannya kini tinggal di sini, dibaca
     * dari [PrayerCalc.Method] yang sedang aktif, dan tidak ada layar yang mengarang angkanya.
     */
    fun metodeRingkas(m: PrayerCalc.Method): String =
        "Fajr ${m.fajr}° / Isya " +
            (m.ishaIntervalMin?.let { "Magrib+$it mnt" } ?: "${m.isha}°")

    /**
     * Koreksi menit bawaan: tanpa koreksi. Panjangnya wajib sepanjang [ENAM_WAKTU].
     *
     * Rentang [KOREKSI_MIN_MENIT]..[KOREKSI_MAKS_MENIT] adalah satu-satunya rentang yang sah; ia
     * dipakai slider layar Jadwal **dan** [parseCorr], sehingga nilai tersimpan yang berada di luar
     * rentang tidak lagi diam-diam dipakai penjadwal.
     */
    const val CORR_DEFAULT = "0,0,0,0,0,0"

    /** Batas koreksi menit per waktu. Cukup untuk membetulkan beda hisab, tak sampai menggeser sholat. */
    const val KOREKSI_MIN_MENIT = -30
    const val KOREKSI_MAKS_MENIT = 30

    /** Zona waktu bawaan: kosong berarti mengikuti zona waktu perangkat (lihat [zona]). */
    const val TZ_DEFAULT = ""
}
