package id.or.kemenag.yasintahlil.core.designsystem

import kotlin.math.roundToInt

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

    // ---- Satuan azimut ---------------------------------------------------------------------
    // Kunci `"derajat"`/`"mil"` dahulu ditulis di tiga berkas: layar Pengaturan, layar Kiblat, dan
    // `KiblatViewModel` (bawaan state-nya). Menambah satu satuan berarti menyunting ketiganya, dan
    // satuan yang lolos dari percabangan tampilan akan tampil sebagai derajat meski pilihannya
    // tersimpan — pengguna melihat angka yang sama untuk satuan yang berbeda.

    /** Derajat busur, 0–360 — bawaan, dan satu-satunya satuan yang dipakai perhitungan. */
    const val DERAJAT = "derajat"

    /** Mil artileri (1/6400 lingkaran). Hanya satuan **tampilan**; azimutnya tetap derajat. */
    const val MIL = "mil"

    /** Seluruh satuan yang sah, urut untuk ditampilkan. */
    val SATUAN: List<String> = listOf(DERAJAT, MIL)

    /** Satuan bawaan bila pengguna belum pernah memilihnya. */
    const val SATUAN_DEFAULT = DERAJAT

    fun satuanSah(v: String): Boolean = v in SATUAN

    /**
     * Nama satuan untuk ditampilkan; nilai tak dikenal diperlakukan sebagai [DERAJAT].
     *
     * Mil diberi keterangan "(6400)" karena istilahnya mudah tertukar dengan mil jarak: yang
     * dimaksud di sini adalah mil artileri, 1/6400 lingkaran. Sebelumnya hanya layar Pengaturan
     * memberi keterangan itu sementara chip di layar Kiblat berbunyi "Mil" saja — dua nama untuk
     * satu satuan yang sama.
     */
    fun labelSatuan(v: String): String = when (v) {
        MIL -> "Mil (6400)"
        else -> "Derajat"
    }

    /** Jarum hanya boleh digambar bila sensornya ada **dan** pengguna memilih tampilan kompas. */
    fun gambarJarum(pilihan: String, adaSensor: Boolean): Boolean =
        adaSensor && (if (sah(pilihan)) pilihan else KOMPAS) == KOMPAS

    // ---- Angka azimut ----------------------------------------------------------------------

    /**
     * 1 derajat = 17,7778 mil artileri (lingkaran penuh 6400 mil).
     *
     * Konversinya tinggal di sini, bukan di layar Kiblat. Satuan adalah urusan aturan ini, dan layar
     * yang menuliskan konversinya sendiri berarti satuan baru harus tahu cara mengonversi —
     * persis kerusakan yang pernah terjadi pada kunci satuannya (dahulu ditulis di tiga berkas).
     */
    const val MIL_PER_DEG = 17.7778

    /**
     * Azimut [derajat] menurut satuan tampilan yang dipilih — angka yang benar-benar dibaca pengguna.
     *
     * Satuan tak dikenal diperlakukan sebagai [DERAJAT], sama seperti [labelSatuan]: pilihan yang
     * rusak tidak boleh mengubah angkanya menjadi satuan lain.
     */
    fun formatAzimut(derajat: Double, satuan: String): String =
        if ((if (satuanSah(satuan)) satuan else SATUAN_DEFAULT) == MIL) {
            "${(derajat * MIL_PER_DEG).roundToInt()} mil"
        } else {
            "%.1f°".format(derajat)
        }
}
