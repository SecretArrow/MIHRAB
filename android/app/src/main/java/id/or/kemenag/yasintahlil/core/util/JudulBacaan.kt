package id.or.kemenag.yasintahlil.core.util

/**
 * Judul satu bacaan yang **disimpan pengguna** — favorit, bookmark, dan riwayat.
 *
 * Judul ini bukan hiasan: ia yang dibaca pengguna di daftar Favorit & Riwayat untuk mengenali
 * bacaannya, dan ia juga yang dipakai indeks pencarian. Judul satu ayat Yasin dahulu ditulis di
 * **dua** tempat dengan dua ejaan — `Yasin ayat $n` di `YasinViewModel` (saat menandai posisi baca
 * dan menandai favorit) dan `Yasin ayat ${v.nomor}` di indeks pencarian. Keduanya harus sepakat
 * tanpa apa pun yang menjaganya: begitu salah satunya diperbaiki, ayat yang sama muncul dengan dua
 * nama berbeda, dan pengguna yang menandai ayat dari layar Yasin lalu mencarinya lewat menu
 * Pencarian melihat dua bacaan yang seolah berlainan.
 *
 * Murni tanpa Android supaya dapat diuji langsung di JVM.
 */
object JudulBacaan {

    /**
     * Judul satu ayat Yasin menurut [nomor] ayatnya, mis. `Yasin ayat 58`.
     *
     * [nomor] adalah nomor ayat pada surat, sama dengan yang dipakai sebagai `ref` tersimpan dan
     * yang ditampilkan layar Yasin.
     */
    fun yasinAyat(nomor: Int): String = "Yasin ayat $nomor"
}
