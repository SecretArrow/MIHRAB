package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.navigation.Routes

/**
 * Nama berkas konten yang dibaca **langsung** oleh sebuah layar, bukan lewat daftar modul.
 *
 * Kebanyakan konten dibaca lewat `ContentRepo` dengan nama modul yang dikenal isinya (mis.
 * `repo.dzikir()`), tetapi dua modul sholat khusus dibaca per berkas: layar Sholat Ghaib meminta
 * `sholat_ghaib.json` dan layar Sholat Jenazah meminta `sholat_jenazah.json`. Nama itu dahulu
 * dituliskan sendiri-sendiri di **tiga** tempat — dua layar dan indeks pencarian — sehingga salah
 * ketik pada salah satunya tidak berbunyi apa pun: layar menampilkan keadaan kosong ("konten tidak
 * tersedia") sementara pencarian tetap mendaftar modulnya, atau sebaliknya. Nama berkas adalah kunci
 * ke bundel; ia tidak boleh punya lebih dari satu ejaan.
 *
 * [SHOLAT_KHUSUS] juga menyimpan **layar** tiap modul, sebab pencarian harus menunjuk ke mana
 * hasilnya membuka dan layar itu sendiri yang tahu berkas mana yang dibacanya. Dengan keduanya di
 * satu tempat, menambah modul sholat khusus berarti menambah satu baris — bukan mengejar nama
 * berkas di tiga berkas berbeda.
 *
 * Murni tanpa Android, sehingga dapat diuji langsung di JVM; [Routes] hanya kumpulan string.
 */
object BerkasKonten {

    /** Satu modul sholat khusus: [berkas] di bundel konten dan [rute] layar yang menampilkannya. */
    data class Modul(val berkas: String, val rute: String)

    const val SHOLAT_GHAIB = "sholat_ghaib.json"
    const val SHOLAT_JENAZAH = "sholat_jenazah.json"

    /** Modul sholat khusus beserta layarnya, urut seperti di daftar Panduan Sholat. */
    val SHOLAT_KHUSUS: List<Modul> = listOf(
        Modul(SHOLAT_GHAIB, Routes.GHAIB),
        Modul(SHOLAT_JENAZAH, Routes.JENAZAH),
    )
}
