package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.navigation.Routes

/**
 * Nama berkas konten — satu ejaan untuk setiap berkas di `assets/content`.
 *
 * Nama berkas bukan sekadar label: ia adalah **kunci** ke tiga tempat sekaligus. `assets/content`
 * menyimpannya sebagai berkas bawaan, `filesDir/content_update/` menyimpannya sebagai penimpaan yang
 * dipasang pengguna (lihat [ContentUpdateManager]), dan peta `berkas` pada sebuah bundel memakainya
 * sebagai kunci entri ([BundelKonten]). Satu nama yang tertulis berbeda di salah satu tempat tidak
 * berbunyi apa pun saat dibangun maupun diuji: `ContentRepo` mencarinya di aset, tidak menemukannya,
 * dan barulah saat layar itu dibuka pengguna melihat kegagalan — `FileNotFoundException` dari
 * `assets.open`, atau, pada lapisan pembaruan, **diam-diam kembali ke konten bawaan** sehingga
 * pembaruan yang dilaporkan "terpasang" tidak mengubah apa pun.
 *
 * Karena itu namanya tinggal di sini saja. Dua nama sudah lebih dulu pindah ke sini
 * ([SHOLAT_GHAIB], [SHOLAT_JENAZAH]) beserta penjaganya; sisanya masih dieja di dalam `ContentRepo`
 * pada sebelas tempat, tanpa apa pun yang membandingkannya dengan berkas yang benar-benar dikirim.
 * Kini seluruhnya di sini, dan [SEMUA] menjadi daftar yang dapat dibandingkan dengan direktori aset.
 *
 * [SHOLAT_KHUSUS] juga menyimpan **layar** tiap modul sholat khusus, **id entri daftar** yang
 * membukanya, dan **nama layarnya** bagi pengguna — sebab pencarian harus menunjuk ke mana hasilnya
 * membuka, Panduan Sholat harus tahu entri mana yang punya layar sendiri, dan layar itu sendiri yang
 * tahu berkas mana yang dibacanya. Dengan semuanya di satu tempat, menambah modul sholat khusus
 * berarti menambah satu baris — bukan mengejar nama berkas, id daftar, dan rute di tiga berkas
 * berbeda.
 *
 * Murni tanpa Android, sehingga dapat diuji langsung di JVM; [Routes] hanya kumpulan string.
 */
object BerkasKonten {

    /**
     * Satu modul sholat khusus: berkasnya di bundel konten, layar yang menampilkannya, entri daftar
     * yang membukanya, dan nama layar itu bagi pengguna.
     *
     * Keempatnya sengaja tinggal bersama. [idDaftar] adalah id entri di `sholat_daftar.json`;
     * `SholatScreen` dahulu membandingkan id itu dengan literalnya sendiri (`s.id == "ghaib"`), dan
     * tak ada apa pun yang menyandingkannya dengan berkas yang benar-benar dikirim. Mengganti id di
     * bundel karena itu **diam-diam** menghapus tombol "Buka" bagi entri itu dan menyisakan tombol
     * "Tata cara" menuju halaman rincian yang tak punya isi — tanpa satu pun pesan galat.
     *
     * [nama] datang dari [NamaLayar] supaya tombolnya menyebut layar tujuan dengan nama yang sama
     * dengan judul layar itu sendiri; dahulu tombolnya menulis *"Buka panduan Ghaib"*, ejaan ketiga
     * bagi layar yang sama.
     */
    data class Modul(val berkas: String, val rute: String, val idDaftar: String, val nama: String)

    // ---- Bacaan & panduan ----------------------------------------------------------------------

    /** Seluruh surah Yasin beserta terjemahannya. */
    const val YASIN_FULL = "yasin_full.json"

    /** Seluruh bagian Tahlil beserta lafaznya. */
    const val TAHLIL_FULL = "tahlil_full.json"

    /** Daftar susunan Tahlil (urutan bagian) beserta catatan tradisi tingkat modul. */
    const val TAHLIL_SUSUNAN = "tahlil.json"

    /** Daftar sholat wajib & sunnah, lengkap dengan niat dan ketentuannya. */
    const val SHOLAT_DAFTAR = "sholat_daftar.json"

    /** Lafaz bacaan sholat dari takbir sampai salam. */
    const val SHOLAT_BACAAN = "sholat_bacaan.json"

    /** Bacaan doa mayit, terpisah agar tidak tercampur ke langkah sholat umum. */
    const val SHOLAT_MAYIT = "sholat_mayit.json"

    /** Syarat, persiapan, dan penutup aurat — daftar periksa tanpa lafaz. */
    const val SHOLAT_SYARAT = "sholat_syarat.json"

    /** Panduan bersuci: wudhu, tayamum, dan pembatalnya. */
    const val WUDHU = "wudhu.json"

    /** Kelompok dzikir beserta hitungan dan rujukan teksnya. */
    const val DZIKIR = "dzikir.json"

    /** Lafaz adzan, iqomah, jawaban adzan, dan doa setelahnya. */
    const val ADZAN = "adzan.json"

    /** Daftar kota beserta koordinatnya untuk pemilihan lokasi. */
    const val KOTA = "kota.json"

    // ---- Modul sholat khusus -------------------------------------------------------------------

    const val SHOLAT_GHAIB = "sholat_ghaib.json"
    const val SHOLAT_JENAZAH = "sholat_jenazah.json"

    /**
     * Seluruh berkas konten yang dibaca aplikasi, urut seperti di atas.
     *
     * Daftar ini ada supaya "setiap nama yang dibaca benar-benar dikirim" dapat **diperiksa** —
     * bukan sekadar dijanjikan oleh ejaan yang kebetulan sama di sebelas tempat.
     */
    val SEMUA: List<String> = listOf(
        YASIN_FULL, TAHLIL_FULL, TAHLIL_SUSUNAN,
        SHOLAT_DAFTAR, SHOLAT_BACAAN, SHOLAT_MAYIT, SHOLAT_SYARAT,
        WUDHU, DZIKIR, ADZAN, KOTA,
        SHOLAT_GHAIB, SHOLAT_JENAZAH,
    )

    /** Modul sholat khusus beserta layarnya, urut seperti di daftar Panduan Sholat. */
    val SHOLAT_KHUSUS: List<Modul> = listOf(
        Modul(SHOLAT_GHAIB, Routes.GHAIB, "ghaib", NamaLayar.GHAIB),
        Modul(SHOLAT_JENAZAH, Routes.JENAZAH, "jenazah", NamaLayar.JENAZAH),
    )
}
