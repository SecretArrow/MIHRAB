package id.or.kemenag.yasintahlil.core.data

/**
 * Aturan **cadangan teks bacaan**: hasil rujukan bersama bila ada isinya, medan entri itu sendiri
 * bila rujukannya kosong atau mati.
 *
 * Bacaan pada bundel konten boleh menunjuk teksnya ke entri lain (`rujukanFile`, mis.
 * `surah_001.json#1`), dan rujukan itu boleh gagal — berkasnya hilang, id-nya berganti, atau
 * bundelnya lebih tua daripada kodenya. [ContentRepo.resolveArabLatinTerjemah] mengembalikan teks
 * kosong untuk rujukan yang tak dapat ditelusuri (**bukan** melempar), sehingga tiap layar harus
 * tahu apa yang ditampilkan bila rujukannya kosong: **teks milik entri itu sendiri**.
 *
 * Aturan itu dulu hidup di **lima** tempat: dua layar sholat mayit memanggil salinannya lewat
 * `TampilanSholatKhusus.teksBacaan`, sementara layar Panduan Sholat, rincian langkah sholat, dan
 * layar Tahlil masing-masing mengeja `Triple(it.first.ifBlank { b.arab }, …)` sendiri. Kelimanya
 * hari ini menghasilkan hal yang sama — dan justru itulah yang membuatnya berbahaya: satu
 * perubahan aturan (mis. ikut memangkas spasi, atau memakai judul entri sebagai cadangan) yang
 * dikenakan pada satu salinan saja tidak berbunyi apa pun, dan pengguna membaca teks yang berbeda
 * dari layar yang menampilkan entri yang sama.
 *
 * Murni tanpa Android, sehingga aturannya dapat diuji langsung di JVM — dan penjaganya
 * (`TeksBacaanSatuTempatTest`) memastikan tak ada layar yang mengejanya kembali.
 */
object TeksBacaan {

    /** Aturan untuk entri sholat ([Bacaan]) — dipakai Panduan Sholat dan kedua layar sholat mayit. */
    fun pilih(
        b: Bacaan,
        arab: String,
        latin: String,
        terjemahan: String,
    ): Triple<String, String, String> = pilih(arab, latin, terjemahan, b.arab, b.latin, b.terjemahan)

    /** Aturan untuk bagian Tahlil ([TahlilItem]) — bentuk rujukannya sama, medan cadangannya pun. */
    fun pilih(
        t: TahlilItem,
        arab: String,
        latin: String,
        terjemahan: String,
    ): Triple<String, String, String> = pilih(arab, latin, terjemahan, t.arab, t.latin, t.terjemahan)

    /**
     * Aturan itu sendiri, dipilih **per medan**: rujukan bila ada isinya, [cadanganArab] dan
     * kawan-kawannya bila tidak.
     *
     * Ketiganya dipilih terpisah dengan sengaja — satu rujukan boleh memuat Arabnya tetapi tidak
     * terjemahannya (mis. berkas yang hanya menyimpan teks Arab), dan medan yang kosong jatuh ke
     * cadangannya sendiri, bukan menjatuhkan ketiga medan sekaligus.
     */
    private fun pilih(
        arab: String, latin: String, terjemahan: String,
        cadanganArab: String, cadanganLatin: String, cadanganTerjemahan: String,
    ): Triple<String, String, String> = Triple(
        arab.ifBlank { cadanganArab },
        latin.ifBlank { cadanganLatin },
        terjemahan.ifBlank { cadanganTerjemahan },
    )
}
