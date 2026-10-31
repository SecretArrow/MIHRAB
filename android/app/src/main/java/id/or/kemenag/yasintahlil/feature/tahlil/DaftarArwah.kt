package id.or.kemenag.yasintahlil.feature.tahlil

/**
 * "Daftar Arwah" — nama-nama orang yang telah wafat yang dituju dalam tahlil (permintaan
 * pengguna, 2026-10-20: *"saya lihat imam disini membaca daftar nama nama orang yang sudah
 * wafat di tahlil dan juga doa"*).
 *
 * Buku "Tahlil dan doa IMAM" sengaja menulis **titik-titik** pada Bagian 4 — "hadhrati ruhi…,
 * wa hadhrati ruhi…, wa hadhrati ruhi…" — tempat imam menyebut nama arwah yang dituju, dan
 * pada doa Bagian IV frasa *"man ijtama'na hahuna bisababihi"* menunjuk orang tertentu tanpa
 * menulis namanya. Nama-namanya memang **bukan lafaz baku**: tiap majelis/keluarga membawa
 * daftarnya sendiri dan adatnya berbeda antar daerah. Karena itu aplikasi tidak mengarang
 * nama (dan tidak menyisipkan apa pun ke lafaz buku) — ia hanya menyediakan tempat yang layak:
 * daftar milik pengguna, disimpan lokal di perangkat (`Store.ARWAH`), tampil jelas terpisah
 * dari teks bacaan.
 *
 * Sengaja murni (tanpa Android) agar aturan pembersihannya dapat diuji di JVM — pola yang
 * sama dengan `SusunanSaya`. Satu nama per baris saat tersimpan; pemisah baris di dalam nama
 * dibuang karena akan merusak format simpanan.
 */
object DaftarArwah {

    /** Batas jumlah nama — majelis membaca belasan nama; lebih dari ini tak lagi terbaca nyaman. */
    const val MAKS_ENTRI = 30

    /** Batas panjang satu nama (karakter) — cukup untuk "Hj. Fatimah binti H. Ahmad". */
    const val MAKS_PANJANG = 60

    /** Satu nama siap simpan/tampil: baris baru dan spasi ganda dirapikan, ujungnya dipangkas. */
    fun bersih(nama: String): String =
        nama.replace(Regex("\\s+"), " ").trim().take(MAKS_PANJANG)

    /** Baca daftar dari preferensi (satu nama per baris). Urutan dipertahankan. */
    fun parse(teks: String): List<String> =
        teks.split('\n').map(::bersih).filter { it.isNotEmpty() }.beres()

    /** Simpan daftar ke preferensi (satu nama per baris). */
    fun serialize(nama: List<String>): String = nama.beres().joinToString("\n")

    /** Tambah satu nama di urutan akhir; duplikat (tanpa pandang besar-kecil huruf) tidak ditambah dua kali. */
    fun tambah(lama: List<String>, nama: String): List<String> =
        (lama + bersih(nama)).beres()

    /** Buang nama pada indeks [i]; indeks di luar daftar mengembalikan daftar apa adanya. */
    fun hapus(lama: List<String>, i: Int): List<String> =
        if (i in lama.indices) lama.toMutableList().also { it.removeAt(i) } else lama

    /** Baris ringkas untuk panel pengingat di kartu doa: "Nama1 • Nama2 • Nama3". */
    fun ringkas(nama: List<String>, maksTampil: Int = 6): String =
        nama.beres().let { daftar ->
            val potong = daftar.take(maksTampil)
            val sisa = daftar.size - potong.size
            potong.joinToString(" • ") + if (sisa > 0) " • … (+$sisa)" else ""
        }

    private fun List<String>.beres(): List<String> =
        filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.take(MAKS_ENTRI)
}
