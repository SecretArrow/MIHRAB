package id.or.kemenag.yasintahlil.core.util

/**
 * Kalimat untuk **berkas konten yang tidak ada di bundel aplikasi**.
 *
 * Saran yang sama — *"Pasang ulang aplikasi bila ini terus terjadi."* — dahulu ditulis di empat
 * layar (Tahlil, Pengaturan, Dzikir, dan Adzan), masing-masing menuliskan kalimat sebabnya sendiri
 * (*"Berkas susunan tahlil…"*, *"Berkas kumpulan dzikir…"*, *"Berkas lafaz adzan…"*). Memasang ulang
 * aplikasi adalah satu-satunya jalan keluar yang dapat diberikan aplikasi untuk bundel bawaan yang
 * rusak; bila salah satu salinannya diperbaiki — mis. ditambahi langkah lain — tiga layar lain tetap
 * menyarankan hal lama untuk kerusakan yang sama, dan pengguna yang berpindah layar mengira
 * keadaannya berbeda.
 *
 * Yang berbeda antarlayar hanyalah **berkas mana** yang tidak ditemukan, dan itu tetap disebut
 * pemanggil. Murni tanpa Android supaya dapat diuji langsung di JVM.
 */
object PesanKonten {

    /**
     * Kalimat lengkap untuk berkas [nama] yang tidak ditemukan, mis.
     * `berkasTidakAda("susunan tahlil")`. [nama] adalah sebutan berkas dalam bahasa Indonesia —
     * pengguna tidak dapat berbuat apa pun dengan nama berkas JSON-nya.
     */
    fun berkasTidakAda(nama: String): String =
        "Berkas $nama tidak ditemukan di aplikasi. Pasang ulang aplikasi bila ini terus terjadi."
}
