package id.or.kemenag.yasintahlil.navigation

import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.NamaLayar

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
 *
 * Rute dan namanya diputuskan bersama lewat [tujuan] / [tujuanTerakhir], bukan dengan memanggil
 * [rute] dan [namaTujuan] terpisah. Dahulu beranda memanggil keduanya **tanpa** [grupDzikir],
 * sementara layar Favorit menyertakannya: jenis yang sama dapat menghasilkan jawaban berbeda di
 * dua layar, dan nama tujuan yang kosong membuat baris tersimpan tampak seperti tautan rusak.
 */
object RuteKonten {

    /**
     * Keterangan yang ditampilkan layar saat sebuah bacaan tersimpan tidak punya layar tujuan.
     *
     * Satu tempat, bukan disalin ke tiap layar: dua salinan akan berbeda begitu salah satunya
     * diperbaiki, dan pengguna melihat dua kalimat berbeda untuk keadaan yang sama.
     */
    const val TIDAK_DIKENAL = "Layar tujuan tidak dikenal"

    /**
     * Tujuan sebuah bacaan tersimpan: [rute] layarnya beserta [nama] layar itu.
     *
     * Keduanya selalu ada bersama — [tujuan] tidak pernah mengembalikan nama kosong, karena tombol
     * "Buka" tanpa keterangan tujuan sama menyesatkannya dengan tombol yang membuka layar salah.
     */
    data class TujuanKonten(val rute: String, val nama: String)

    /**
     * Rute layar untuk sebuah [kind], atau `null` bila jenis itu tidak punya layar sendiri.
     *
     * @param grupDzikir id kelompok dzikir pada bundel konten (lihat `ContentRepo.dzikir()`).
     */
    fun rute(kind: String, grupDzikir: Set<String> = emptySet()): String? = when (kind) {
        // Ejaan setiap jenis datang dari `JenisBacaan` — kosakata yang sama dengan yang dipakai
        // layar saat menulis id kartunya. Sebelumnya kedua sisi mengejanya sendiri-sendiri, jadi
        // satu huruf yang berubah di salah satu sisi sudah cukup membuat favorit terbuka ke layar
        // yang salah tanpa satu pun pesan galat.
        JenisBacaan.YASIN -> Routes.YASIN
        JenisBacaan.TAHLIL -> Routes.TAHLIL
        JenisBacaan.DZIKIR -> Routes.DZIKIR
        // Bacaan di layar Doa Adzan memakai jenis "doa"; lafaz Iqomah memakai "iqomah".
        JenisBacaan.DOA -> Routes.DOA_ADZAN
        JenisBacaan.IQOMAH -> Routes.IQOMAH
        JenisBacaan.ADZAN -> Routes.ADZAN
        // Bacaan sholat, lafaz niat, dan langkah panduan hidup di modul sholat; rincian per-sholat
        // (Routes.SHOLAT_DETAIL) butuh argumen id sehingga tidak dapat dituju dari sini.
        JenisBacaan.BACAAN, JenisBacaan.NIAT, JenisBacaan.LANGKAH -> Routes.SHOLAT
        JenisBacaan.GHAIB -> Routes.GHAIB
        JenisBacaan.JENAZAH -> Routes.JENAZAH
        JenisBacaan.SEARCH -> Routes.SEARCH
        // Jenis progres ([JenisBacaan.HANYA_PROGRES]) dan id rusak ([JenisBacaan.LAIN]) sengaja
        // tidak punya layar: keduanya bukan alamat yang dapat dituju.
        else -> if (kind in grupDzikir) Routes.dzikirGrup(kind) else null
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
     *
     * @param grupDzikir id kelompok dzikir pada bundel konten — wajib sama dengan yang dipakai
     *   [rute]; tanpa itu bacaan dzikir terbaca seolah tidak punya tujuan.
     */
    fun ruteTerakhir(last: String, grupDzikir: Set<String> = emptySet()): String? =
        rute(JenisBacaan.jenisDari(last), grupDzikir)

    /**
     * Tujuan lengkap untuk nilai `Store.LAST` — rute **dan** nama layarnya sekaligus, sehingga
     * pemanggil tidak dapat memakai [ruteTerakhir] dengan satu [grupDzikir] lalu [namaTujuan]
     * dengan yang lain (atau tanpa sama sekali).
     */
    fun tujuanTerakhir(last: String, grupDzikir: Set<String> = emptySet()): TujuanKonten? =
        tujuan(JenisBacaan.jenisDari(last), grupDzikir)

    /**
     * Nama layar tujuan, untuk memberi tahu pengguna ke mana tombol "Buka" akan membawanya.
     * `null` bila tidak ada tujuan.
     */
    fun namaTujuan(kind: String, grupDzikir: Set<String> = emptySet()): String? =
        when (rute(kind, grupDzikir)) {
            Routes.YASIN -> NamaLayar.YASIN
            Routes.TAHLIL -> NamaLayar.TAHLIL
            Routes.DZIKIR -> NamaLayar.DZIKIR
            Routes.DOA_ADZAN -> NamaLayar.DOA_ADZAN
            Routes.IQOMAH -> NamaLayar.IQOMAH
            Routes.ADZAN -> NamaLayar.ADZAN
            Routes.SHOLAT -> NamaLayar.SHOLAT
            Routes.GHAIB -> NamaLayar.GHAIB
            Routes.JENAZAH -> NamaLayar.JENAZAH
            Routes.SEARCH -> NamaLayar.SEARCH
            else -> null
        }

    /**
     * Rute dan nama tujuan sebuah [kind] sekaligus, atau `null` bila jenis itu tidak punya layar.
     *
     * Inilah yang dipakai layar: satu pemanggilan, satu [grupDzikir], sehingga keterangan tujuan
     * dan tombol "Buka" tidak mungkin menunjuk hal yang berbeda.
     */
    fun tujuan(kind: String, grupDzikir: Set<String> = emptySet()): TujuanKonten? {
        val r = rute(kind, grupDzikir) ?: return null
        val n = namaTujuan(kind, grupDzikir) ?: return null
        return TujuanKonten(r, n)
    }
}
