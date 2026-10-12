package id.or.kemenag.yasintahlil.navigation

/**
 * Pemetaan "jenis konten tersimpan" (favorit & riwayat) ke rute layar tujuannya.
 *
 * Favorit dan riwayat menyimpan `kind`, yaitu bagian sebelum `:` pada id bacaan. Sebelumnya
 * pemetaannya ada di dalam `FavoritScreen` dan ditutup rapat dengan `else -> HOME`, sehingga tombol
 * **"Buka"** membuka layar yang salah tanpa memberi tahu siapa pun:
 *
 * - favorit **dzikir** — `kind`-nya adalah *id kelompok* (`pagi_petang`, `setelah_sholat`,
 *   `harian`, `doa_khusus`), bukan `dzikir` — membuka **Beranda**;
 * - favorit **doa adzan** (`kind = "doa"`) membuka **Dzikir**, bukan layar Doa Adzan;
 * - favorit **iqomah** (`kind = "iqomah"`) membuka layar **Adzan**, bukan Iqomah;
 * - baris **riwayat pencarian** (`kind = "search"`) membuka **Beranda**.
 *
 * Tombol yang membuka layar lain lebih buruk daripada tidak ada tombol: pengguna mengira isinya
 * hilang, lalu menandai ulang hal yang sama. Karena itu fungsi di sini mengembalikan `null` untuk
 * jenis yang memang tidak punya layar sendiri, dan layar menampilkan keterangan — bukan tombol yang
 * diam-diam menyesatkan (`plan.txt`: *"Pastikan setiap tombol memiliki fungsi nyata"*).
 *
 * [grupDzikir] diisi id kelompok dzikir **dari bundel konten**. Daftarnya sengaja tidak ditulis
 * tetap di sini: daftar tetap akan kembali salah begitu bundel menambah kelompok baru — persis
 * kerusakan yang sedang diperbaiki.
 */
object RuteKonten {

    /**
     * Rute layar untuk sebuah [kind], atau `null` bila jenis itu tidak punya layar sendiri.
     *
     * @param grupDzikir id kelompok dzikir pada bundel konten (lihat `ContentRepo.dzikir()`).
     */
    fun rute(kind: String, grupDzikir: Set<String> = emptySet()): String? = when (kind) {
        "yasin" -> Routes.YASIN
        "tahlil" -> Routes.TAHLIL
        "dzikir" -> Routes.DZIKIR
        // Bacaan di layar Doa Adzan memakai kind "doa"; lafaz Iqomah memakai "iqomah".
        "doa" -> Routes.DOA_ADZAN
        "iqomah" -> Routes.IQOMAH
        "adzan" -> Routes.ADZAN
        // Bacaan sholat, lafaz niat, dan langkah panduan hidup di modul sholat; rincian per-sholat
        // (Routes.SHOLAT_DETAIL) butuh argumen id sehingga tidak dapat dituju dari sini.
        "bacaan", "niat", "langkah" -> Routes.SHOLAT
        "ghaib" -> Routes.GHAIB
        "jenazah" -> Routes.JENAZAH
        "search" -> Routes.SEARCH
        else -> if (kind in grupDzikir) Routes.DZIKIR else null
    }

    /**
     * Rute untuk nilai `Store.LAST` — bacaan terakhir yang dibuka, berformat `kind:ref` (mis.
     * `yasin:58` atau `tahlil:taawudz`).
     *
     * Beranda dahulu menentukan tujuannya sendiri dengan `if (last.startsWith("yasin")) YASIN else
     * FAVORIT`. Karena `Store.LAST` hanya pernah berisi `yasin:…` dan `tahlil:…`, setiap bacaan
     * Tahlil terakhir membuka layar **Favorit** — bukan Tahlil. Tombol yang membawa pengguna ke
     * tempat yang salah membuat isinya tampak hilang, jadi aturannya tinggal satu: yang ini, dipakai
     * beranda maupun layar Favorit.
     *
     * `null` bila jenisnya tidak punya layar sendiri (layar harus mengatakannya, bukan menebak).
     */
    fun ruteTerakhir(last: String): String? = rute(last.substringBefore(':'))

    /**
     * Nama layar tujuan, untuk memberi tahu pengguna ke mana tombol "Buka" akan membawanya.
     * `null` bila tidak ada tujuan.
     */
    fun namaTujuan(kind: String, grupDzikir: Set<String> = emptySet()): String? =
        when (rute(kind, grupDzikir)) {
            Routes.YASIN -> "Surat Yasin"
            Routes.TAHLIL -> "Tahlil"
            Routes.DZIKIR -> "Dzikir & Doa"
            Routes.DOA_ADZAN -> "Doa Adzan"
            Routes.IQOMAH -> "Iqomah"
            Routes.ADZAN -> "Adzan & Pengingat"
            Routes.SHOLAT -> "Panduan Sholat"
            Routes.GHAIB -> "Sholat Ghaib"
            Routes.JENAZAH -> "Sholat Jenazah"
            Routes.SEARCH -> "Pencarian"
            else -> null
        }
}
