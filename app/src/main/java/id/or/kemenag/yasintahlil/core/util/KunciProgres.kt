package id.or.kemenag.yasintahlil.core.util

/**
 * Kunci baris **progres** bacaan — pasangan `(jenis, rujukan)` yang menyebut satu baris di tabel
 * progres (`UserDataRepository.progress`/`setProgress`).
 *
 * Jenisnya datang dari [JenisBacaan]; rujukannya menyebut baris mana di dalam jenis itu. Sebagian
 * bacaan memakai id isinya sebagai rujukan (sholat memakai id sholatnya, dzikir memakai
 * `<id kelompok>:<judul>`), dan itu wajar karena rujukannya memang sebuah id konten. Tetapi ada dua
 * bacaan yang **tidak** menyimpan progres per entri konten:
 *
 * - **Tahlil** menyimpan dua angka untuk satu susunan: posisi bagian yang sedang dibaca dan
 *   hitungan tasbih bagian itu. Keduanya bukan id konten, jadi butuh nama sendiri.
 * - **Panduan Wudhu** menyimpan **satu** tanda untuk seluruh layar ("sudah dipelajari"), bukan per
 *   langkah.
 *
 * Nama-nama itu dahulu berupa literal yang diketik ulang di tiap tempat pemakaian: `"idx"` dan
 * `"count"` muncul sepuluh kali di `TahlilViewModel`, lalu diketik ulang lagi di `SettingsScreen`
 * yang mengosongkan progres Tahlil saat susunan diganti; `"wudhu"` diketik di tiga tempat di
 * `WudhuScreen` padahal jenisnya sudah datang dari `JenisBacaan`. Penulis dan pembaca baris yang
 * sama karena itu dapat berbeda ejaan tanpa galat apa pun — pengguna menekan "mulai dari awal"
 * dan penghitungnya tidak bergerak, atau tanda "sudah dipelajari" hilang sendiri. Semuanya kini
 * membaca satu tempat ini.
 *
 * **Nilainya dibekukan**: rujukan ini tersimpan di perangkat dan bertahan melintasi pembaruan
 * aplikasi, jadi mengubahnya membuat baris lama tak terbaca — posisi baca Tahlil kembali ke nol dan
 * tanda wudhu hilang. Mengubahnya harus disengaja dan disertai migrasi.
 */
object KunciProgres {

    /** Posisi bagian Tahlil yang sedang dibaca (indeks bagian pada susunan aktif). */
    const val TAHLIL_POSISI = "idx"

    /** Hitungan tasbih bagian Tahlil yang sedang dibaca. */
    const val TAHLIL_HITUNGAN = "count"

    /**
     * Tanda "sudah dipelajari" untuk seluruh panduan wudhu.
     *
     * Berbeda dari [TAHLIL_POSISI] dan [TAHLIL_HITUNGAN] yang menyebut dua angka, rujukan ini hanya
     * perlu satu baris. Bunyinya kebetulan sama dengan jenisnya ([JenisBacaan.WUDHU]) karena begitulah
     * baris ini dahulu ditulis; nilainya dibekukan agar tanda dari versi sebelumnya tetap terbaca.
     */
    const val WUDHU = "wudhu"
}
