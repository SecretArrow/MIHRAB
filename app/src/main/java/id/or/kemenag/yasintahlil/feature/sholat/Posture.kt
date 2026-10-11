package id.or.kemenag.yasintahlil.feature.sholat

import id.or.kemenag.yasintahlil.R

/**
 * Ilustrasi skematik posisi tubuh tiap gerakan sholat.
 *
 * Sesuai rencana: setiap gerakan punya visual demonstrasi yang sopan + deskripsi posisi tubuh.
 * Gambar sengaja berupa skema garis (lihat `res/drawable/posisi_*.xml`) — tanpa penggambaran
 * figuratif dan tanpa teks Arab — sedangkan deskripsi di bawah ini murni prosedural.
 */
data class Posture(
    /** Resource skema posisi (`R.drawable.posisi_*`). */
    val gambar: Int,
    /** Nama singkat posisi, mis. "Rukuk". */
    val label: String,
    /** Deskripsi posisi tubuh (prosedural, tanpa lafaz). */
    val deskripsi: String,
)

/**
 * Pemetaan id bacaan ([ContentRepo.sholatBacaan]) ke posisi tubuhnya. Beberapa bacaan
 * berbagi posisi yang sama (mis. iftitah dan Fatihah sama-sama berdiri).
 */
object Postures {

    private val BERDIRI = Posture(
        R.drawable.posisi_berdiri,
        "Berdiri tegak",
        "Berdiri tegak menghadap kiblat, pandangan ke tempat sujud, kedua tangan " +
            "bersedekap di depan dada atau perut.",
    )
    private val TAKBIR = Posture(
        R.drawable.posisi_takbir,
        "Berdiri, tangan diangkat",
        "Berdiri tegak; kedua tangan diangkat sejajar bahu atau telinga, lalu " +
            "diletakkan kembali bersedekap.",
    )
    private val RUKUK = Posture(
        R.drawable.posisi_rukuk,
        "Rukuk",
        "Membungkuk dengan punggung rata sejajar lantai, kedua tangan memegang " +
            "lutut, pandangan ke tempat sujud.",
    )
    private val SUJUD = Posture(
        R.drawable.posisi_sujud,
        "Sujud",
        "Turun hingga dahi dan hidung menyentuh lantai; kedua telapak tangan, kedua " +
            "lutut, dan kedua ujung kaki juga menempel lantai (tujuh anggota sujud).",
    )
    private val DUDUK = Posture(
        R.drawable.posisi_duduk,
        "Duduk iftirasy",
        "Duduk di atas kaki kiri yang terlipat, kaki kanan tegak, kedua tangan " +
            "diletakkan di atas paha, pandangan ke arah jari telunjuk.",
    )
    private val SALAM = Posture(
        R.drawable.posisi_salam,
        "Duduk, menoleh",
        "Masih duduk tasyahud akhir, wajah menoleh ke kanan lalu ke kiri hingga " +
            "pipi terlihat dari belakang.",
    )

    private val byId: Map<String, Posture> = mapOf(
        "niat" to BERDIRI,
        "takbir" to TAKBIR,
        "iftitah" to BERDIRI,
        "fatihah" to BERDIRI,
        "rukuk" to RUKUK,
        "itidal" to BERDIRI,
        "sujud" to SUJUD,
        "duduk" to DUDUK,
        "tasyahud" to DUDUK,
        "sholawat_ibrahimiyah" to DUDUK,
        "qunut" to TAKBIR,
        "salam" to SALAM,
    )

    /** Posisi untuk id bacaan tertentu, atau null bila tidak ada (tak seharusnya terjadi). */
    fun of(bacaanId: String): Posture? = byId[bacaanId]

    /** Semua id bacaan yang sudah punya posisi — dipakai uji kelengkapan. */
    fun coveredIds(): Set<String> = byId.keys
}
