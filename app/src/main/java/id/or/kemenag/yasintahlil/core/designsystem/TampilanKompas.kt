package id.or.kemenag.yasintahlil.core.designsystem

/**
 * `plan.txt` §"Arah Kiblat": *"Pengaturan satuan dan tampilan kompas."* Satuan mengubah angka
 * azimut; **tampilan** menentukan apakah jarum kompas digambar sama sekali.
 *
 * Pilihan ini bukan hiasan. Pada perangkat tanpa magnetometer, sudut hadap perangkat tidak
 * diketahui, sehingga jarum yang digambar akan tampak seolah perangkat menghadap utara — sebuah
 * arah yang salah tetapi terlihat meyakinkan. Karena itu perangkat tanpa sensor **selalu**
 * memakai tampilan angka + peta (lihat `KiblatScreen`), dan pengguna yang sensornya tersedia
 * tetapi tidak dapat diandalkan boleh memilih tampilan yang sama.
 */
object TampilanKompas {

    /** Jarum kompas digambar, di samping angka azimut dan peta. */
    const val KOMPAS = "kompas"

    /** Hanya angka azimut dan peta — tanpa jarum. */
    const val ANGKA = "angka"

    val SEMUA: List<String> = listOf(KOMPAS, ANGKA)

    fun sah(v: String): Boolean = v in SEMUA

    fun label(v: String): String = when (v) {
        ANGKA -> "Angka & peta"
        else -> "Kompas & peta"
    }

    /** Jarum hanya boleh digambar bila sensornya ada **dan** pengguna memilih tampilan kompas. */
    fun gambarJarum(pilihan: String, adaSensor: Boolean): Boolean =
        adaSensor && (if (sah(pilihan)) pilihan else KOMPAS) == KOMPAS
}
