package id.or.kemenag.yasintahlil.core.data

/**
 * Baris atribusi sumber untuk teks Qur'an, murni tanpa Android sehingga dapat diuji langsung.
 *
 * Teks Arab, transliterasi Latin, dan terjemahan di berkas `assets/content` berasal dari sumber
 * yang **dicatat terpisah** (`sumber_arab`, `sumber_latin`, `sumber_terjemahan`) karena ketiganya
 * memang dari bagian berbeda layanan resmi Qur'an Kemenag. Menampilkan hanya salah satunya, atau
 * menampilkan semuanya sebagai satu sumber, sama-sama menyembunyikan asal-usul yang sebenarnya.
 *
 * Fungsi ini menyusunnya menjadi satu baris yang jujur: hanya bagian yang benar-benar diklaim yang
 * ikut tampil, dan bila tidak ada satu pun keterangan, hasilnya kosong — bukan kalimat karangan.
 */
object AtribusiTeks {

    /** Label tetap agar bentuk barisnya sama di semua layar. */
    private const val ARAB = "Teks Arab"
    private const val LATIN = "Transliterasi"
    private const val TERJEMAHAN = "Terjemahan"

    /**
     * "Teks Arab: … · Transliterasi: … · Terjemahan: …", atau "" bila tak ada sumber yang diklaim.
     */
    fun surah(s: Surah): String = baris(
        ARAB to s.sumberArab,
        LATIN to s.sumberLatin,
        TERJEMAHAN to s.sumber,
    )

    private fun baris(vararg bagian: Pair<String, String>): String =
        bagian.mapNotNull { (label, nilai) ->
            nilai.trim().takeIf { it.isNotBlank() }?.let { "$label: $it" }
        }.joinToString(" · ")
}
