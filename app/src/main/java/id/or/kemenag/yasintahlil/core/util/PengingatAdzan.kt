package id.or.kemenag.yasintahlil.core.util

/**
 * Aturan murni untuk **preferensi pengingat adzan**: saklar per waktu dan panjang timer iqomah.
 *
 * Sama seperti [JadwalHariIni] menyatukan aturan hisab, berkas ini menyatukan aturan preferensi
 * yang sebelumnya disalin ke beberapa tempat:
 *
 *  - `Store.ADZAN_ON` adalah CSV lima angka yang diurai ulang di `PrayerScheduler` (pembaca) dan di
 *    layar Adzan (penulis). Bila keduanya tidak sepakat soal panjang atau padding, menyalakan satu
 *    waktu di layar dapat mematikan waktu lain di penjadwal.
 *  - Batas panjang timer iqomah (1–60 menit) dahulu ditulis di tiga tempat: dua tombol di layar
 *    Iqomah, penjadwal pengingat, dan pekerja latar. Bila salah satunya berbeda, layar menampilkan
 *    angka yang tidak pernah dipakai notifikasi.
 *
 * Tanpa Android, sehingga dapat diuji langsung di JVM.
 */
object PengingatAdzan {

    /** Banyak waktu beradzan — panjang CSV `Store.ADZAN_ON`. Sejajar dengan [JadwalHariIni.LIMA_WAKTU]. */
    val JUMLAH_WAKTU = JadwalHariIni.LIMA_WAKTU.size

    /**
     * "1,0,1,…" → lima saklar per waktu. Panjangnya selalu tepat [JUMLAH_WAKTU]: entri yang hilang
     * dianggap **menyala** (perilaku bawaan aplikasi) dan kelebihan dipotong, agar indeks tidak
     * bergeser bila CSV-nya cacat.
     */
    fun parseOn(csv: String): List<Boolean> =
        (csv.split(",").map { it.trim() == "1" } + List(JUMLAH_WAKTU) { true }).take(JUMLAH_WAKTU)

    /** Kebalikan [parseOn]: saklar → CSV yang dibaca penjadwal dan pekerja latar. */
    fun serializeOn(on: List<Boolean>): String = on.map { if (it) "1" else "0" }.joinToString(",")

    /** Batas panjang timer iqomah, dalam menit. Nilai tersimpan selalu dijepit ke rentang ini. */
    const val IQOMAH_MIN_MENIT = 1
    const val IQOMAH_MAKS_MENIT = 60

    /** Panjang timer iqomah bawaan bila pengguna belum pernah mengaturnya. */
    const val IQOMAH_DEFAULT_MENIT = 10

    /** Jepit nilai tersimpan ke rentang yang sah, sehingga layar dan notifikasi memakai angka sama. */
    fun iqomahMenit(mentah: Int): Int = mentah.coerceIn(IQOMAH_MIN_MENIT, IQOMAH_MAKS_MENIT)
}
