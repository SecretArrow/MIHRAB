package id.or.kemenag.yasintahlil.core.util

/**
 * Nama **layar tujuan** — satu nama untuk satu layar, dipakai oleh setiap tempat yang menyebut
 * layar itu kepada pengguna.
 *
 * Sebelum berkas ini ada, nama sebuah layar ditulis ulang di tiap tempat yang menunjuknya, dan
 * salinan-salinannya sudah menyimpang. Layar yang sama disebut dengan nama berbeda tergantung dari
 * mana pengguna datang:
 *
 * - **Surat Yasin** disebut *"Yasin"* di kartu beranda dan menu Ibadah, tetapi *"Surat Yasin"* di
 *   label tujuan tombol "Buka" (`RuteKonten.namaTujuan`).
 * - **Adzan & Pengingat** disebut *"Adzan"* di kartu beranda, tetapi *"Adzan & Pengingat"* di menu
 *   Alat dan di judul layarnya sendiri.
 * - **Arah Kiblat** disebut *"Kiblat"* di kartu beranda dan *"Kompas Kiblat"* di menu Alat, padahal
 *   judul layarnya *"Arah Kiblat"* — tiga nama untuk satu layar.
 * - **Pencarian** disebut *"Cari"* di kartu beranda.
 * - **Panduan Sholat** disebut *"Sholat Wajib & Sunnah"* di menu Ibadah.
 * - **Iqomah** disebut *"Iqomah + timer"* dan **Tahlil** disebut *"Tahlil + counter"* di menu
 *   Ibadah, padahal timer dan counter itu memang ada **di dalam** layarnya: imbuhan itu tidak
 *   menjanjikan hal lain, hanya menyebut isi layar yang sama dengan kata yang berbeda.
 * - **Favorit & Riwayat** berjudul *"Favorit, Bookmark, Riwayat & Terakhir dibaca"*.
 *
 * Akibatnya nyata tetapi sunyi: pengguna menekan *"Kompas Kiblat"* lalu tiba di layar berjudul
 * *"Arah Kiblat"*, atau menekan *"Yasin"* lalu tiba di *"Surat Yasin"* — dan menyimpulkan ia
 * tersesat. Tidak ada satu pun pesan galat, karena aplikasinya memang berjalan benar; yang berbeda
 * hanyalah **nama**. Karena itu `namaTujuan` (label tujuan tombol "Buka") dan judul layar yang
 * dibukanya kini membaca nama yang sama dari sini, dan ujinya menuntut keduanya sejajar.
 *
 * **Batasnya.** Nama yang hidup di sini hanyalah nama layar yang **dapat dibuka pengguna** — dari
 * menu, kartu, tab bawah, tombol "Buka" bacaan tersimpan, atau tombol di Pengaturan. Layar yang
 * namanya benar-benar hanya muncul di satu tempat tidak dimasukkan, sebab tidak ada salinan yang
 * dapat menyimpang: rincian per-sholat (`Routes.SHOLAT_DETAIL`) dinamai oleh bundelnya sendiri.
 *
 * Pengaturan pernah disebut pengecualian dengan alasan bahwa tombolnya menyebut **tindakannya**
 * ("Buka pembaruan konten"), bukan nama layarnya. Alasan itu tidak bertahan begitu diperiksa: tiap
 * bagian di sana diawali **judul bagian** yang menyebut layar tujuannya, sehingga namanya memang
 * muncul lebih dari sekali — dan sudah menyimpang. Layar Tentang ditulis **tiga** cara sekaligus
 * (judul layarnya *"Tentang aplikasi"*, judul bagiannya *"Tentang & sumber"*, tombolnya *"Buka
 * Tentang & privasi"*), sedangkan Pembaruan konten memakai huruf besar-kecil yang berbeda antara
 * judul bagian dan tombolnya. Dua uji bahkan memaku kedua ejaan itu — yang satu menuntut layarnya
 * memuat *"Tentang aplikasi"*, yang lain menuntut Pengaturan memuat *"Tentang & sumber"* — dan
 * bersama-sama menjaga agar satu layar tetap punya dua nama. Keduanya kini tinggal di sini.
 *
 * Tanpa Android, sehingga dapat diuji langsung di JVM — sama seperti [JenisBacaan] dan
 * [JudulBacaan].
 */
object NamaLayar {

    // ---- Bagian tab bawah ---------------------------------------------------------------------
    //
    // Keempat nama bagian ini dipakai tab bawah; [IBADAH] dan [ALAT] juga menjadi judul menunya
    // ("Menu Ibadah" / "Menu Alat"), sehingga tab dan judul layar yang dibukanya tidak lagi dapat
    // menyebut bagian yang sama dengan kata berbeda.

    /** Beranda — layar pembuka. Judulnya sendiri berupa sapaan, bukan nama layar. */
    const val BERANDA = "Beranda"

    const val IBADAH = "Ibadah"
    const val ALAT = "Alat"
    const val PENGATURAN = "Pengaturan"

    // ---- Layar bacaan --------------------------------------------------------------------------

    /**
     * "Surat Yasin", bukan "Yasin": kata "Yasin" sendirian juga nama aplikasinya, sehingga tombol
     * yang hanya menyebut "Yasin" tidak memberi tahu apa yang akan terbuka.
     */
    const val YASIN = "Surat Yasin"

    /**
     * "Tahlil" — tanpa imbuhan "+ counter". Counter itu ada di dalam layarnya; menambahkannya di
     * menu membuat satu layar punya dua nama tanpa ada hal berbeda yang dijanjikan.
     */
    const val TAHLIL = "Tahlil"

    const val DZIKIR = "Dzikir & Doa"

    // ---- Panduan ibadah ------------------------------------------------------------------------

    /** Layar panduan sholat wajib & sunnah. Menu Ibadah dahulu menyebutnya "Sholat Wajib & Sunnah". */
    const val SHOLAT = "Panduan Sholat"

    const val WUDHU = "Wudhu & Tayamum"
    const val GHAIB = "Sholat Ghaib"
    const val JENAZAH = "Sholat Jenazah"

    // ---- Adzan & waktu -------------------------------------------------------------------------

    /** "Adzan & Pengingat", bukan "Adzan": layar ini juga mengatur pengingatnya. */
    const val ADZAN = "Adzan & Pengingat"

    /**
     * Layar doa seputar adzan. Judulnya menambahkan keterangan *"sebelum & sesudah adzan"* di
     * belakang nama ini — keterangan itu boleh, selama namanya tetap yang bersama.
     */
    const val DOA_ADZAN = "Doa Adzan"

    const val IQOMAH = "Iqomah"
    const val JADWAL = "Jadwal Sholat"

    /**
     * "Arah Kiblat" — nama yang dipakai judul layarnya, bukan "Kompas Kiblat" (menyebut alatnya)
     * atau "Kiblat" (terlalu pendek untuk membedakannya dari kata biasa).
     */
    const val KIBLAT = "Arah Kiblat"

    // ---- Pencarian & simpanan ------------------------------------------------------------------

    /** "Pencarian", bukan "Cari": kartu beranda dahulu menulis "Cari" untuk layar yang sama. */
    const val SEARCH = "Pencarian"

    const val FAVORIT = "Favorit & Riwayat"

    // ---- Layar yang dibuka dari Pengaturan -----------------------------------------------------

    /**
     * Layar Tentang: versi, kebijakan privasi, daftar izin, lisensi, dan sumber konten. Namanya
     * dahulu ditulis tiga cara sekaligus — judul bagian di Pengaturan, label tombolnya, dan judul
     * layarnya sendiri. Lihat catatan batas di atas.
     */
    const val TENTANG = "Tentang aplikasi"

    /** Layar memasang bundel konten pilihan pengguna, dari berkas yang ia pilih sendiri. */
    const val PEMBARUAN_KONTEN = "Pembaruan konten"

    // ---- Amalan & referensi ---------------------------------------------------------------------

    /** Layar penghitung tasbih digital: target harian, getar tiap ketukan, riwayat 7 hari. */
    const val TASBIH = "Tasbih Digital"

    /** Layar 99 nama Allah beserta artinya, dapat dicari. */
    const val ASMAUL = "Asmaul Husna"

    /** Kalkulator zakat: fitrah, mal/penghasilan, dan tabungan dengan hitungan nisab. */
    const val ZAKAT = "Kalkulator Zakat"

    /** Kalender Masehi beranotasi Hijriah & pasaran: weton, puasa sunnah, hari besar Islam. */
    const val KALENDER = "Kalender"

    /** Catatan khataman Yasin beserta rangkaian hari baca (streak) yang sedang berjalan. */
    const val KHATAMAN = "Khataman & Streak"

    /** Layar peringatan tahlil untuk almarhum: hari ke-7, 40, 100, 1000, dan haul tahunan. */
    const val ALMARHUM = "Tahlil Almarhum"
}
