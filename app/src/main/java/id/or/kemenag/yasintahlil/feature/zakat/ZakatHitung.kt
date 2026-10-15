package id.or.kemenag.yasintahlil.feature.zakat

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Hitungan zakat murni lokal — tanpa Android, agar teruji langsung di JVM seperti aturan bersama
 * lain di `core/util`.
 *
 * Patokannya yang umum dipakai di Indonesia: takaran fitrah 2,5 kg beras per jiwa, nisab mal
 * senilai 85 gram emas, kadar 2,5%. Semuanya perkiraan patokan umum — rincian fiqihnya (haul,
 * utang, jenis harta niaga, harga emas yang mana) diserahkan pada amil atau ulama setempat.
 */
object ZakatHitung {

    /** Takaran zakat fitrah per jiwa dalam kg beras. */
    const val KG_PER_JIWA = 2.5

    /** Berat emas yang menjadi batas nisab zakat mal. */
    const val NISAB_EMAS_GRAM = 85.0

    /** Kadar zakat atas harta dan penghasilan. */
    const val RATE = 0.025

    /** Nominal zakat fitrah dalam rupiah: jiwa × takaran beras × harga beras per kg. */
    fun fitrah(jiwa: Int, hargaBerasPerKg: Double): Double = jiwa * KG_PER_JIWA * hargaBerasPerKg

    /** Beras yang wajib dikeluarkan zakat fitrah (kg), berapa pun harganya. */
    fun fitrahKg(jiwa: Int): Double = jiwa * KG_PER_JIWA

    /** Zakat mal: 2,5% dari harta yang telah mencapai nisab. */
    fun mal(hartaZakat: Double): Double = hartaZakat * RATE

    /** Batas nisab zakat mal dalam rupiah: senilai [NISAB_EMAS_GRAM] gram emas. */
    fun nisabEmas(hargaEmasPerGram: Double): Double = NISAB_EMAS_GRAM * hargaEmasPerGram

    /**
     * Zakat penghasilan: 2,5% dari penghasilan bulanan bersih (setelah kebutuhan pokok).
     * Patokan umum; rincian fiqihnya (haul, nisab setahun) diserahkan pada amil/ulama.
     */
    fun penghasilan(penghasilanBulanan: Double): Double = penghasilanBulanan * RATE

    /**
     * Rupiah utuh dengan pemisah ribuan titik — "Rp 1.250.000". Nilai nol atau negatif menjadi
     * "Rp 0": isian yang belum diisi jangan ditampilkan seolah berhutang.
     */
    fun rupiah(nilai: Double): String =
        "Rp " + DecimalFormat("#,##0", simbolTitik()).format(maxOf(0.0, nilai))

    /** Berat (kg) dengan satu desimal koma — "6,3 kg", "10 kg"; nol dan negatif menjadi "0 kg". */
    fun kg(nilai: Double): String =
        DecimalFormat("0.#", simbolKoma()).format(maxOf(0.0, nilai)) + " kg"

    /** Simbol angka format Indonesia: pemisah ribuan titik — apa pun lokal JVM-nya. */
    private fun simbolTitik(): DecimalFormatSymbols =
        DecimalFormatSymbols().apply { groupingSeparator = '.' }

    /** Simbol angka format Indonesia: desimal koma — apa pun lokal JVM-nya. */
    private fun simbolKoma(): DecimalFormatSymbols =
        DecimalFormatSymbols().apply { decimalSeparator = ',' }
}
