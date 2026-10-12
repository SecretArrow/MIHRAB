package id.or.kemenag.yasintahlil.core.sensor

/**
 * `plan.txt` §"Arah Kiblat" meminta **"Indikator status kalibrasi sensor"** dan **"Petunjuk
 * menggerakkan perangkat membentuk pola angka delapan jika sensor memerlukan kalibrasi"**.
 *
 * Statusnya sengaja berlapis, bukan satu boolean. "Belum terbaca" berbeda dari "terkalibrasi":
 * sebelum sensor mengirim satu pun nilai, aplikasi **belum tahu** apa pun soal kalibrasi, dan
 * menampilkan tanda centang saat itu adalah klaim yang belum terbukti — kebalikannya, layar yang
 * diam saja membuat pengguna mengira kompasnya sudah dipercaya. Status ini juga memisahkan "tidak
 * ada magnetometer" (perangkat memang tidak dapat memakai kompas langsung) dari "perlu kalibrasi"
 * (sensor ada, tetapi arahnya belum layak dipercaya).
 */
enum class StatusKompas {
    /** Perangkat tidak punya magnetometer/sensor rotasi — kompas langsung tidak tersedia. */
    TANPA_SENSOR,

    /** Sensor ada, tetapi belum ada satu pun pembacaan — status kalibrasi belum diketahui. */
    MENUNGGU,

    /** Sensor aktif dan akurasinya dilaporkan baik. */
    TERKALIBRASI,

    /** Sensor ada, tetapi akurasinya dilaporkan tidak andal — arahnya belum layak dipercaya. */
    PERLU_KALIBRASI,
}

object KalibrasiKompas {

    /**
     * Status dari ketersediaan sensor dan akurasi terakhir.
     *
     * @param adaSensor sensor rotasi/magnetometer terpasang.
     * @param terkalibrasi akurasi terakhir; `null` bila belum ada pembacaan sama sekali.
     */
    fun status(adaSensor: Boolean, terkalibrasi: Boolean?): StatusKompas = when {
        !adaSensor -> StatusKompas.TANPA_SENSOR
        terkalibrasi == null -> StatusKompas.MENUNGGU
        terkalibrasi -> StatusKompas.TERKALIBRASI
        else -> StatusKompas.PERLU_KALIBRASI
    }

    fun label(s: StatusKompas): String = when (s) {
        StatusKompas.TANPA_SENSOR -> "Sensor kompas tidak tersedia"
        StatusKompas.MENUNGGU -> "Menunggu pembacaan sensor…"
        StatusKompas.TERKALIBRASI -> "Sensor kompas terbaca & terkalibrasi"
        StatusKompas.PERLU_KALIBRASI -> "Sensor perlu kalibrasi"
    }

    /** Tanda status — status tidak boleh disampaikan lewat warna saja (aksesibilitas). */
    fun tanda(s: StatusKompas): String = when (s) {
        StatusKompas.TANPA_SENSOR -> "✕"
        StatusKompas.MENUNGGU -> "…"
        StatusKompas.TERKALIBRASI -> "✓"
        StatusKompas.PERLU_KALIBRASI -> "⚠"
    }

    /** Hanya kalibrasi yang gagal yang memerlukan gerakan pola angka delapan. */
    fun perluPolaDelapan(s: StatusKompas): Boolean = s == StatusKompas.PERLU_KALIBRASI

    /** Status yang harus tampil sebagai peringatan, bukan sekadar keterangan. */
    fun perluPerhatian(s: StatusKompas): Boolean =
        s == StatusKompas.TANPA_SENSOR || s == StatusKompas.PERLU_KALIBRASI
}
