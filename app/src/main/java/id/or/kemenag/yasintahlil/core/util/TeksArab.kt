package id.or.kemenag.yasintahlil.core.util

/**
 * Aturan teks Arab yang dipakai pemeriksaan konten: blok Unicode-nya, hurufnya, tanda harakatnya,
 * dan panjang kata minimum yang dinilai.
 *
 * Aturan ini pernah ditulis **tiga kali** dengan nilai yang sama, di tiga tempat yang berbeda:
 * `BundelKonten` (saat bundel pembaruan dipasang), `ArabicTextTest` (saat assets diperiksa di CI),
 * dan `tools/validate_content.py` (gate konten di CI). Ketiganya mengeja rentang yang sama —
 * `0600–06FF` dan `0750–077F` untuk teks Arab, `0621–064A` dan `0671–06D3` untuk hurufnya,
 * `064B–0652` ditambah lima kode tunggal untuk harakat — dan tak satu pun membandingkan dirinya
 * dengan yang lain.
 *
 * Bahayanya berbeda dari salinan biasa karena rentang Unicode tidak dapat dibaca sekilas: menambah
 * satu tanda baris baru di Kotlin, misalnya, tidak membuat berkas mana pun tampak berubah bagi
 * peninjau. Bila gate Python tertinggal, teks yang belum divokalkan **lolos CI** lalu baru ditolak
 * saat bundel dipasang — atau, lebih buruk, diterima aplikasi dan **ditampilkan kepada pengguna
 * sebagai bacaan**. Bila salinan Kotlin yang tertinggal, uji assets gagal pada berkas yang justru
 * sah, dan yang paling mudah diperbaiki adalah menambal salah satu salinan — persis cara salinan
 * ketiga lahir.
 *
 * Karena itu nilainya tinggal **di sini saja**, dinyatakan sebagai rentang yang dapat dibaca mesin
 * (bukan tersebar di dalam badan fungsi), dan gate Python **membacanya dari berkas ini** dengan pola
 * pengambil yang sama seperti `ContentValidator.allowedHosts`. Dijaga `AturanTeksArabSatuTempatTest`.
 *
 * Yang **tidak** ikut disatukan adalah bagian mana yang diperiksa: `BundelKonten` hanya menilai
 * field `arab`/`latin`, gate konten menilai kunci mana pun (termasuk `catatan`), dan uji assets
 * menilai setiap string JSON. Perbedaan cakupan itu memang disengaja; yang tidak boleh berbeda
 * adalah aturannya.
 *
 * Tanpa Android, sehingga dapat diuji langsung di JVM.
 */
object TeksArab {

    /** Blok Unicode teks Arab: blok utama dan blok tambahan. */
    val RENTANG_ARAB = listOf(0x0600 to 0x06FF, 0x0750 to 0x077F)

    /** Rentang kode **huruf** Arab — dipakai menghitung panjang sebuah kata. */
    val RENTANG_HURUF = listOf(0x0621 to 0x064A, 0x0671 to 0x06D3)

    /** Rentang kode tanda harakat: fathatan sampai sukun. */
    val RENTANG_HARAKAT = listOf(0x064B to 0x0652)

    /**
     * Tanda harakat di luar [RENTANG_HARAKAT]: alef khanjariyah dan tanda maddah.
     *
     * Ditulis terpisah karena kodenya tidak berdampingan dengan rentang utama — kesalahan yang
     * paling mudah terjadi saat menyalin aturan ini adalah kehilangan daftar kecil ini.
     */
    val HARAKAT_TUNGGAL = listOf(0x0670, 0x0653, 0x0654, 0x0655, 0x0656)

    /**
     * Panjang minimum kata Arab sebelum dinilai, dalam huruf.
     *
     * Kata yang lebih pendek sering memang tanpa harakat (mis. kata sambung satu huruf), sehingga
     * menilainya hanya menghasilkan galat palsu.
     */
    const val MIN_HURUF = 3

    /** Pola [RENTANG_ARAB] sebagai kelas karakter, dipakai mencari satu kata Arab utuh. */
    val POLA_ARAB: String = "[" + RENTANG_ARAB.joinToString("") {
        "\\u%04X-\\u%04X".format(it.first, it.second)
    } + "]+"

    /** [RENTANG_ARAB] sebagai pola siap pakai. */
    val regexArab: Regex = Regex(POLA_ARAB)

    /** Seluruh kode tanda harakat: rentang ditambah kode tunggal. */
    val HARAKAT: Set<Char> =
        RENTANG_HARAKAT.flatMap { (awal, akhir) -> awal..akhir }.map { it.toChar() }.toSet() +
            HARAKAT_TUNGGAL.map { it.toChar() }

    /**
     * Kata Arab ber-[MIN_HURUF] huruf atau lebih yang **sama sekali** tanpa tanda harakat.
     *
     * Teks Arab pada aplikasi harus berharakat agar dapat dibaca pemula; kata seperti itu hampir
     * pasti salah ketik atau teks mentah yang belum divokalkan. Huruf mad tidak dihitung sebagai
     * penanda — sebuah kata yang hanya memuat huruf mad tetap dianggap tanpa harakat.
     */
    fun tanpaHarakat(teks: String): List<String> =
        regexArab.findAll(teks).map { it.value }.filter { run ->
            val huruf = run.count { c -> RENTANG_HURUF.any { c.code in it.first..it.second } }
            huruf >= MIN_HURUF && run.none { it in HARAKAT }
        }.toList()
}
