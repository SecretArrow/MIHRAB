package id.or.kemenag.yasintahlil.core.util

/**
 * Kosakata **jenis bacaan tersimpan** — bagian sebelum `:` pada id bacaan (mis. `yasin:58`,
 * `adzan:3`, `doa:adzan`).
 *
 * Jenis ini adalah kunci sambungan antara **penulis** dan **pembaca**. Layar menulisnya sebagai
 * awalan id kartu ([id]), dan `BacaanCard` menyimpannya apa adanya ke favorit, riwayat, dan progres;
 * [RuteKonten][id.or.kemenag.yasintahlil.navigation.RuteKonten] membacanya kembali untuk menentukan
 * layar tujuan tombol "Buka". Dahulu kedua sisi mengejanya sendiri-sendiri — `"yasin"` di
 * `YasinScreen` dan di `RuteKonten`, `"adzan"` di layar Adzan dan di `RuteKonten` — sehingga satu
 * huruf yang berubah di salah satu sisi membuat favorit terbuka ke layar yang salah, atau berakhir
 * "Layar tujuan tidak dikenal", tanpa satu pun pesan galat.
 *
 * Jenis sengaja **tidak** disamakan dengan rute meski beberapa kebetulan sama bunyinya ([YASIN] =
 * `Routes.YASIN` = `"yasin"`, tetapi [GHAIB] = `"ghaib"` sementara `Routes.GHAIB` =
 * `"sholat/ghaib"`). Menyamakan keduanya berarti salah satunya harus berubah begitu salah satu
 * kebutuhan bergeser — persis kekeliruan yang dihindari di sini.
 *
 * Murni tanpa Android supaya dapat diuji langsung di JVM.
 */
object JenisBacaan {

    /** Pemisah antara jenis dan rujukan pada id bacaan tersimpan. */
    const val PEMISAH = ':'

    // ---- Jenis yang punya layar sendiri (dipakai favorit & riwayat) --------------------------

    const val YASIN = "yasin"
    const val TAHLIL = "tahlil"
    const val DZIKIR = "dzikir"

    /** Bacaan pada layar Doa Adzan — bukan layar Dzikir. */
    const val DOA = "doa"
    const val IQOMAH = "iqomah"
    const val ADZAN = "adzan"

    /** Bacaan, lafaz niat, dan langkah panduan hidup di layar Panduan Sholat. */
    const val BACAAN = "bacaan"
    const val NIAT = "niat"
    const val LANGKAH = "langkah"

    const val GHAIB = "ghaib"
    const val JENAZAH = "jenazah"
    const val SEARCH = "search"

    /**
     * Jenis yang **wajib** punya layar tujuan: seluruhnya ditulis sebagai awalan id kartu bacaan
     * atau sebagai jenis riwayat, sehingga setiap entri tersimpan harus dapat dibuka kembali.
     * Sebagian layarnya sama ([BACAAN], [NIAT], dan [LANGKAH] menuju Panduan Sholat).
     *
     * Daftar ini adalah sisi "jenis" dari peta di `RuteKonten.rute`; uji `RuteKontenTest`
     * memastikan setiap anggotanya benar-benar mengarah ke layar.
     */
    val BERPETA: List<String> = listOf(
        YASIN, TAHLIL, DZIKIR, DOA, IQOMAH, ADZAN,
        BACAAN, NIAT, LANGKAH, GHAIB, JENAZAH, SEARCH,
    )

    // ---- Jenis yang hanya menyimpan progres --------------------------------------------------

    /**
     * Jenis yang dipakai **hanya** untuk menyimpan progres ("sudah dibaca/selesai"), bukan favorit
     * atau riwayat: panduan sholat per-sholat dan panduan wudhu. Keduanya tidak punya layar yang
     * dapat dituju tanpa argumen — rincian sholat butuh id, dan langkah wudhu tinggal di satu layar
     * bersama tayamum — sehingga sengaja **tidak** masuk [BERPETA].
     */
    const val SHOLAT = "sholat"
    const val WUDHU = "wudhu"

    /** Jenis progres, urut untuk ditampilkan; tidak ada yang punya layar tujuan sendiri. */
    val HANYA_PROGRES: List<String> = listOf(SHOLAT, WUDHU)

    /**
     * Jenis bawaan untuk id yang tidak berbentuk `jenis:rujukan` — lihat [pisah].
     * Sengaja tidak ada di [BERPETA]: id yang rusak tidak boleh menawarkan tombol "Buka" ke layar
     * yang tidak tentu.
     */
    const val LAIN = "lain"

    // ---- Menyusun dan membaca id tersimpan ---------------------------------------------------

    /**
     * Susun id bacaan tersimpan dari [jenis] dan [rujukan] — mis. `id(YASIN, "58")` = `"yasin:58"`.
     *
     * Seluruh penulis id memakai fungsi ini supaya pemisahnya tidak pernah berbeda antara penulis
     * dan pembaca ([pisah]). Pemisah yang tertukar membuat jenis id terbaca sebagai bagian rujukan,
     * dan bacaan tersimpan itu kehilangan layar tujuannya.
     */
    fun id(jenis: String, rujukan: String): String = "$jenis$PEMISAH$rujukan"

    /**
     * Id kartu lafaz iqomah ke-[nomor], **bernomor urut mulai 1** — `idLafazIqomah(1)` = `"iqomah:1"`.
     *
     * Nomor urutnya adalah aturan, bukan pilihan pemanggil, dan itulah sebabnya ia tinggal di sini.
     * Layar Iqomah dahulu menyusunnya dari `i + 1` sementara indeks pencarian menyusunnya dari `i`
     * mentah, sehingga lafaz yang **sama** tersimpan dengan dua rujukan berbeda: kartunya
     * `iqomah:1` (lafaz pertama) sedangkan dokumen pencariannya `iqomah:0`. Pengguna yang menandai
     * lafaz pertama sebagai favorit lalu menemukannya lewat pencarian melihat dua bacaan yang
     * berbeda — favoritnya tidak menempel pada hasil yang sama — tanpa satu pun pesan galat.
     *
     * Id kartu diambil dari nomor urut, bukan dari teks Arabnya, karena beberapa lafaz iqomah
     * memang berulang; id yang diambil dari teks membuat favorit satu lafaz menandai lafaz lain
     * juga.
     */
    fun idLafazIqomah(nomor: Int): String = id(IQOMAH, nomor.toString())

    /**
     * Pisahkan id tersimpan menjadi jenis dan rujukannya; id yang tidak berbentuk `jenis:rujukan`
     * dibaca sebagai [LAIN] beserta seluruh isinya.
     */
    fun pisah(id: String): Pair<String, String> {
        val i = id.indexOf(PEMISAH)
        return if (i <= 0) LAIN to id else id.substring(0, i) to id.substring(i + 1)
    }

    /**
     * Jenis sebuah id tersimpan, atau seluruh id bila tidak ada pemisah.
     *
     * Berbeda dari [pisah] yang menandai id rusak sebagai [LAIN], di sini `"yasin"` tetap terbaca
     * sebagai [YASIN]: nilai `Store.LAST` dari versi aplikasi yang lebih dahulu hanya berisi jenisnya
     * saja, dan menandainya "lain" akan membuat bacaan terakhir kehilangan tombol lanjutannya.
     */
    fun jenisDari(id: String): String = id.substringBefore(PEMISAH)
}
