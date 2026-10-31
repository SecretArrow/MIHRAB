package id.or.kemenag.yasintahlil.feature.sholat

import id.or.kemenag.yasintahlil.R
import id.or.kemenag.yasintahlil.core.sholat.BacaanSholat

/**
 * Ilustrasi kartun posisi tubuh tiap gerakan sholat.
 *
 * Setiap gerakan digambar dari **empat arah** — depan, kanan, kiri, belakang — sebagai kartun flat
 * **tanpa teks** di dalam gambar: jubah gading, kopiah zamrud, garis luar zamrud gelap, sesuai
 * palet tema aplikasi. Keempat tampak dibangun dari satu kerangka 3D yang sama sehingga tidak
 * pernah saling bertentangan (mis. kepala pada tampak kanan selalu di sisi yang benar); berkasnya
 * adalah `res/drawable/posisi_<gerakan>_<tampak>.xml` dan dijaga utuh oleh `PostureTest`.
 *
 * Awalnya ilustrasinya sengaja berupa skema garis tanpa figuratif. Permintaan pengguna
 * (2026-10-15) mengubah arah itu secara eksplisit: *buat ulang seluruh gambar gerakan sholat
 * yang bagus dan benar, kartun lengkap, tanpa teks, setiap gerakan terlihat dari 4 gambar:
 * depan, kanan, kiri, belakang.* Kartun figuratif kini menjadi keputusan yang disengaja.
 *
 * Beberapa bacaan berbagi posisi yang sama (mis. iftitah, Fatihah, dan surat pendek sama-sama
 * dibaca sambil bersedekap), dan salam memuat **dua varian** posisi — menoleh ke kanan lalu ke
 * kiri — karena keduanya memang dua gerakan yang berurutan, bukan satu.
 */
data class TampakPosisi(
    val depan: Int,
    val kanan: Int,
    val kiri: Int,
    val belakang: Int,
)

/**
 * Satu varian posisi. [label] hanya diisi untuk pose yang punya lebih dari satu varian (salam);
 * varian tunggal memakai label kosong dan cukup memakai label [Posture] utamanya.
 */
data class VarianPosisi(
    val label: String,
    val tampak: TampakPosisi,
)

data class Posture(
    /** Satu atau lebih varian posisi (salam menoleh dua arah). */
    val varian: List<VarianPosisi>,
    /** Nama singkat posisi, mis. "Rukuk". */
    val label: String,
    /** Deskripsi posisi tubuh (prosedural, tanpa lafaz Arab). */
    val deskripsi: String,
)

/**
 * Pemetaan id bacaan ([ContentRepo.sholatBacaan]) ke posisi tubuhnya. Beberapa bacaan
 * berbagi posisi yang sama (mis. iftitah dan Fatihah sama-sama berdiri sedekap); qunut
 * digambar dengan tangan terangkat seperti takbir — beda tinggi telapaknya tidak dibedakan.
 */
object Postures {

    private val KIYAM = Posture(
        listOf(VarianPosisi("", TampakPosisi(
            R.drawable.posisi_kiyam_depan, R.drawable.posisi_kiyam_kanan,
            R.drawable.posisi_kiyam_kiri, R.drawable.posisi_kiyam_belakang))),
        "Berdiri tegak",
        "Berdiri tegak menghadap kiblat, kedua kaki direnggangkan secukupnya, pandangan " +
            "tertuju ke tempat sujud, kedua tangan rileks di sisi badan.",
    )
    private val TAKBIR = Posture(
        listOf(VarianPosisi("", TampakPosisi(
            R.drawable.posisi_takbir_depan, R.drawable.posisi_takbir_kanan,
            R.drawable.posisi_takbir_kiri, R.drawable.posisi_takbir_belakang))),
        "Berdiri, tangan diangkat",
        "Berdiri tegak; kedua tangan diangkat sejajar bahu atau telinga dengan jari santai " +
            "dan telapak menghadap kiblat, lalu diletakkan kembali bersedekap.",
    )
    private val SEDEKAP = Posture(
        listOf(VarianPosisi("", TampakPosisi(
            R.drawable.posisi_sedekap_depan, R.drawable.posisi_sedekap_kanan,
            R.drawable.posisi_sedekap_kiri, R.drawable.posisi_sedekap_belakang))),
        "Bersedekap",
        "Berdiri tegak; tangan kanan diletakkan di atas punggung tangan kiri, di atas dada " +
            "atau antara dada dan pusar, pandangan tetap ke tempat sujud.",
    )
    private val RUKUK = Posture(
        listOf(VarianPosisi("", TampakPosisi(
            R.drawable.posisi_rukuk_depan, R.drawable.posisi_rukuk_kanan,
            R.drawable.posisi_rukuk_kiri, R.drawable.posisi_rukuk_belakang))),
        "Rukuk",
        "Membungkuk dengan punggung rata sejajar lantai, kedua tangan memegang " +
            "lutut, pandangan ke tempat sujud.",
    )
    private val ITIDAL = Posture(
        listOf(VarianPosisi("", TampakPosisi(
            R.drawable.posisi_itidal_depan, R.drawable.posisi_itidal_kanan,
            R.drawable.posisi_itidal_kiri, R.drawable.posisi_itidal_belakang))),
        "I'tidal",
        "Bangkit dari rukuk hingga berdiri tegak kembali; kedua tangan dilepas lurus di " +
            "sisi badan dan tubuh ditenangkan sejenak sebelum sujud.",
    )
    private val SUJUD = Posture(
        listOf(VarianPosisi("", TampakPosisi(
            R.drawable.posisi_sujud_depan, R.drawable.posisi_sujud_kanan,
            R.drawable.posisi_sujud_kiri, R.drawable.posisi_sujud_belakang))),
        "Sujud",
        "Turun hingga dahi dan hidung menyentuh lantai; kedua telapak tangan, kedua " +
            "lutut, dan kedua ujung kaki juga menempel lantai (tujuh anggota sujud).",
    )
    private val DUDUK = Posture(
        listOf(VarianPosisi("", TampakPosisi(
            R.drawable.posisi_duduk_depan, R.drawable.posisi_duduk_kanan,
            R.drawable.posisi_duduk_kiri, R.drawable.posisi_duduk_belakang))),
        "Duduk iftirasy",
        "Duduk di atas kaki kiri yang terlipat, kaki kanan tegak, kedua tangan " +
            "diletakkan di atas paha, pandangan ke arah jari telunjuk.",
    )
    private val TASYAHUD_AWAL = Posture(
        listOf(VarianPosisi("", TampakPosisi(
            R.drawable.posisi_tasyahud_awal_depan, R.drawable.posisi_tasyahud_awal_kanan,
            R.drawable.posisi_tasyahud_awal_kiri, R.drawable.posisi_tasyahud_awal_belakang))),
        "Duduk iftirasy",
        "Duduk iftirasy pada rakaat kedua sholat yang lebih dari dua rakaat; jari telunjuk " +
            "tangan kanan diacungkan menghadap kiblat saat membaca syahadat.",
    )
    /**
     * Duduk tasyahud akhir. Sengaja **tidak** diberi label salah satu bentuk saja: yang dipakai
     * bergantung pada jumlah rakaat, dan aplikasi tidak menanyakan jumlah rakaat sholat sunnah
     * yang sedang dibaca. Kartunnya menggambarkan bentuk tawarruk, sedangkan deskripsinya
     * menyebut kedua bentuk apa adanya.
     */
    private val TASYAHUD_AKHIR = Posture(
        listOf(VarianPosisi("", TampakPosisi(
            R.drawable.posisi_tasyahud_akhir_depan, R.drawable.posisi_tasyahud_akhir_kanan,
            R.drawable.posisi_tasyahud_akhir_kiri, R.drawable.posisi_tasyahud_akhir_belakang))),
        "Duduk akhir",
        "Duduk pada rakaat terakhir. Pada sholat tiga atau empat rakaat: pantat menyentuh " +
            "lantai dengan kaki kiri di bawah kaki kanan (tawarruk). Pada sholat dua rakaat: " +
            "duduk iftirasy seperti duduk di antara dua sujud. Kedua tangan di atas paha, " +
            "pandangan ke arah jari telunjuk.",
    )
    /** Salam menoleh dua arah — dua varian, bukan satu, karena keduanya gerakan berurutan. */
    private val SALAM = Posture(
        listOf(
            VarianPosisi("Menoleh ke kanan", TampakPosisi(
                R.drawable.posisi_salam_kanan_depan, R.drawable.posisi_salam_kanan_kanan,
                R.drawable.posisi_salam_kanan_kiri, R.drawable.posisi_salam_kanan_belakang)),
            VarianPosisi("Menoleh ke kiri", TampakPosisi(
                R.drawable.posisi_salam_kiri_depan, R.drawable.posisi_salam_kiri_kanan,
                R.drawable.posisi_salam_kiri_kiri, R.drawable.posisi_salam_kiri_belakang)),
        ),
        "Duduk, menoleh",
        "Masih duduk tasyahud akhir, wajah menoleh ke kanan lalu ke kiri hingga " +
            "pipi terlihat dari belakang.",
    )

    /**
     * Id bacaan datang dari [BacaanSholat] — kosakata yang sama dengan yang dipakai bundel konten
     * dan daftar langkah di layar, sehingga tidak ada ejaan kedua yang bisa menyimpang. Peta ini
     * sendiri harus mencakup **seluruh** [BacaanSholat.SEMUA]: id yang tak punya posisi membuat
     * ilustrasi gerakannya hilang tanpa keterangan (lihat `PostureTest`).
     */
    private val byId: Map<String, Posture> = mapOf(
        BacaanSholat.NIAT to KIYAM,
        BacaanSholat.TAKBIR to TAKBIR,
        BacaanSholat.IFTITAH to SEDEKAP,
        BacaanSholat.FATIHAH to SEDEKAP,
        BacaanSholat.SURAT_SETELAH_FATIHAH to SEDEKAP,
        BacaanSholat.RUKUK to RUKUK,
        BacaanSholat.ITIDAL to ITIDAL,
        BacaanSholat.SUJUD to SUJUD,
        BacaanSholat.DUDUK to DUDUK,
        BacaanSholat.TASYAHUD_AWAL to TASYAHUD_AWAL,
        BacaanSholat.TASYAHUD_AKHIR to TASYAHUD_AKHIR,
        BacaanSholat.SHOLAWAT_IBRAHIMIYAH to TASYAHUD_AKHIR,
        BacaanSholat.QUNUT to TAKBIR,
        BacaanSholat.SALAM to SALAM,
    )

    /** Posisi untuk id bacaan tertentu, atau null bila tidak ada (tak seharusnya terjadi). */
    fun of(bacaanId: String): Posture? = byId[bacaanId]

    /** Semua id bacaan yang sudah punya posisi — dipakai uji kelengkapan. */
    fun coveredIds(): Set<String> = byId.keys
}
