package id.or.kemenag.yasintahlil.core.util

/**
 * Batas daftar **riwayat terbaru** — berapa entri yang dibaca dari penyimpanan dan berapa yang
 * ditampilkan.
 *
 * Riwayat bacaan dapat memuat bacaan yang sama berkali-kali: satu ayat Yasin dan satu bagian Tahlil
 * mencatat dirinya sendiri setiap kali dibuka. Karena itu daftar riwayat harus membaca **lebih
 * banyak** entri daripada yang ditampilkan — dibaca longgar, disaring menjadi satu baris per bacaan,
 * baru dipotong ke jatah tampil. Bila ketiga angka itu ditulis di tempat pemakaiannya, hubungannya
 * tidak terlihat dan mudah terbalik; membalikkannya pun tidak berbunyi apa pun, daftarnya hanya
 * menjadi lebih pendek dan bacaan lain yang sebenarnya ada tak pernah muncul.
 *
 * Angka-angka ini menjawab satu pertanyaan lagi yang berbeda: **berapa banyak riwayat yang dimiliki
 * pengguna**. Pertanyaan itu tidak boleh dijawab dengan ukuran daftar yang sudah dipotong — lihat
 * [id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository.jumlahRiwayat].
 *
 * Murni tanpa Android supaya dapat diuji langsung di JVM.
 */
object BatasRiwayat {

    /**
     * Entri yang dibaca untuk **daftar** riwayat (layar Favorit). Sengaja lebih longgar daripada
     * [DITAMPILKAN]: penyaringan "satu bacaan satu baris" berjalan sesudah pembacaan, sehingga
     * pengulangan tidak menghabiskan jatah tampil dan menyembunyikan bacaan lain yang ada.
     */
    const val DIBACA_DAFTAR = 60

    /** Entri yang dibaca untuk daftar pendek di tempat lain (mis. pencarian terbaru di Pencarian). */
    const val DIBACA_RINGKAS = 50

    /** Baris riwayat yang benar-benar ditampilkan. */
    const val DITAMPILKAN = 12
}
