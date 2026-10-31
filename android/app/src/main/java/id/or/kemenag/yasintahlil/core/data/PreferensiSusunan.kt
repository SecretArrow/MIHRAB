package id.or.kemenag.yasintahlil.core.data

/**
 * Aturan "Preferensi susunan Tahlil" (`plan.txt` §"Pengaturan Aplikasi").
 *
 * Pilihan susunan disimpan pada satu kunci preferensi ([Store.TAHLIL_SUSUNAN]) dan dibaca oleh dua
 * layar — modul Tahlil dan Pengaturan. Aturannya diletakkan di sini supaya keduanya tidak berbeda
 * pendapat tentang susunan mana yang sedang aktif, terutama ketika nilai tersimpan sudah tidak ada
 * lagi di bundel konten (bundel diganti, atau "Susunan saya" dihapus).
 *
 * Sengaja murni (tanpa Android) agar dapat diuji di JVM.
 */
object PreferensiSusunan {

    /** Id susunan buatan pengguna; dibedakan dari id susunan di `assets/content/tahlil.json`. */
    const val ID_SAYA = "saya"
    const val NAMA_SAYA = "Susunan saya"

    /** Baca daftar id dari preferensi (CSV). Urutan dipertahankan, duplikat dan kosong dibuang. */
    fun parse(csv: String): List<String> =
        csv.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()

    /** Simpan daftar id ke preferensi (CSV). */
    fun serialize(ids: List<String>): String =
        ids.map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(",")

    /**
     * Susunan buatan pengguna untuk ditampilkan di pemilih, atau null bila belum disusun.
     *
     * Sumbernya sengaja kosong: susunan ini disusun sendiri oleh pengguna dari bagian yang sudah
     * ada, jadi ia tidak boleh tampil seolah berasal dari rujukan atau tradisi tertentu.
     */
    fun susunanSaya(pilihan: List<String>): TahlilSusunan? =
        if (pilihan.isEmpty()) null else TahlilSusunan(ID_SAYA, NAMA_SAYA, pilihan, sumber = "")

    /** Susunan yang boleh dipilih: dari bundel konten, ditambah susunan buatan pengguna bila ada. */
    fun tersedia(dariBerkas: List<TahlilSusunan>, susunanSaya: TahlilSusunan?): List<TahlilSusunan> =
        dariBerkas + listOfNotNull(susunanSaya)

    /**
     * Id susunan yang benar-benar aktif. Nilai tersimpan yang tak lagi ada di daftar jatuh ke
     * susunan pertama — bukan ke daftar kosong yang akan membuat layar Tahlil buntu.
     */
    fun idAktif(disimpan: String, tersedia: List<TahlilSusunan>): String {
        val cocok = tersedia.firstOrNull { it.id == disimpan }
        return (cocok ?: tersedia.firstOrNull())?.id ?: ""
    }

    /** Nama susunan yang aktif, untuk ditampilkan; kosong bila tak ada susunan sama sekali. */
    fun namaAktif(disimpan: String, tersedia: List<TahlilSusunan>): String {
        val id = idAktif(disimpan, tersedia)
        return tersedia.firstOrNull { it.id == id }?.nama ?: ""
    }

    /**
     * Sumber susunan yang aktif (`plan.txt` §"Fitur Tahlil Lengkap": *"berdasarkan sumber atau
     * tradisi yang jelas"*). Kosong berarti susunan itu tidak mengklaim sumber apa pun — mis.
     * "Susunan saya" — dan layar harus mengatakannya, bukan mengarang rujukan.
     */
    fun sumberAktif(disimpan: String, tersedia: List<TahlilSusunan>): String {
        val id = idAktif(disimpan, tersedia)
        return tersedia.firstOrNull { it.id == id }?.sumber.orEmpty()
    }
}
