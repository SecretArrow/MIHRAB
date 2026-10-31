package id.or.kemenag.yasintahlil.core.data

import java.net.URLDecoder

/** Satu jenis suara adzan: berkas pilihan pengguna beserta nama yang ia berikan. */
data class SuaraAdzan(val nama: String, val uri: String)

/**
 * Daftar "jenis suara" adzan (`plan.txt` §"Fitur Adzan" meminta *"Pilihan suara adzan dari sumber
 * audio yang legal digunakan"* dan *"Pengaturan volume dan jenis suara"*).
 *
 * Aplikasi tidak menyertakan audio bawaan (lisensi), jadi "jenis suara" di sini bukan katalog
 * bawaan melainkan **berkas milik pengguna sendiri yang diberi nama**: ia dapat menyimpan beberapa
 * berkas sekaligus (mis. "adzan Masjid Nabawi", "adzan masjid lokal") lalu memilih satu yang aktif.
 *
 * Hanya **satu** berkas yang aktif, dan berkas itu tetap ditulis ke [Store.ADZAN_URI] — kunci yang
 * sudah dibaca penjadwal dan pekerja latar. Dengan begitu memilih jenis suara tidak menuntut jalur
 * pemutaran kedua, dan berkas yang berbunyi saat pengingat tiba dijamin sama dengan yang terlihat
 * aktif di layar.
 *
 * Sengaja murni (tanpa Android) agar dapat diuji di JVM.
 */
object DaftarSuaraAdzan {

    /**
     * Pemisah nama dan URI. Karakter kendali (tab) dipilih karena tidak mungkin muncul di nama
     * berkas yang diketik pengguna, sehingga nama boleh memuat spasi dan tanda baca apa pun.
     */
    private const val PEMISAH = '\t'

    /** Baca daftar dari teks tersimpan; baris cacat dilewati, URI ganda dibuang. */
    fun parse(text: String): List<SuaraAdzan> =
        text.split('\n').mapNotNull { baris ->
            val i = baris.indexOf(PEMISAH)
            if (i < 0) return@mapNotNull null
            val nama = baris.substring(0, i).trim()
            val uri = baris.substring(i + 1).trim()
            if (uri.isBlank()) null else SuaraAdzan(nama.ifBlank { namaDariUri(uri) }, uri)
        }.distinctBy { it.uri }

    /** Simpan daftar ke teks; baris kosong dan URI ganda dibuang agar parse/serialize stabil. */
    fun serialize(daftar: List<SuaraAdzan>): String =
        daftar.filter { it.uri.isNotBlank() }
            .distinctBy { it.uri }
            .joinToString("\n") { "${bersih(it.nama.ifBlank { namaDariUri(it.uri) })}$PEMISAH${bersih(it.uri)}" }

    /**
     * Nama bawaan dari nama berkas pada URI, tanpa ekstensi — dipakai saat pengguna menambah berkas
     * tanpa mengetik nama, dan saat berkas aktif dari versi lama belum punya entri daftar.
     */
    fun namaDariUri(uri: String): String {
        val dasar = uri.substringBefore('?').substringAfterLast('/')
        val tanpaEkstensi = dasar.substringBeforeLast('.', dasar)
        val nama = runCatching { URLDecoder.decode(tanpaEkstensi, "UTF-8") }.getOrDefault(tanpaEkstensi)
        return nama.trim().ifBlank { "Suara" }
    }

    /**
     * Tambah berkas baru, atau perbarui namanya bila URI-nya sudah tersimpan. URI kosong diabaikan
     * supaya daftar tidak pernah memuat entri yang tak dapat diputar.
     */
    fun tambah(daftar: List<SuaraAdzan>, nama: String, uri: String): List<SuaraAdzan> {
        if (uri.isBlank()) return daftar
        val namaBersih = nama.trim().ifBlank { namaDariUri(uri) }
        val i = daftar.indexOfFirst { it.uri == uri }
        return if (i < 0) daftar + SuaraAdzan(namaBersih, uri)
        else daftar.toMutableList().also { it[i] = SuaraAdzan(namaBersih, uri) }
    }

    /** Ubah nama jenis suara; URI yang tidak ada di daftar tidak menambah entri apa pun. */
    fun ubahNama(daftar: List<SuaraAdzan>, uri: String, nama: String): List<SuaraAdzan> =
        if (daftar.none { it.uri == uri }) daftar else tambah(daftar, nama, uri)

    /** Buang satu jenis suara dari daftar. */
    fun hapus(daftar: List<SuaraAdzan>, uri: String): List<SuaraAdzan> =
        daftar.filterNot { it.uri == uri }

    /**
     * Daftar yang ditampilkan: berkas tersimpan, ditambah berkas aktif yang belum tercatat.
     *
     * Pengguna dari versi sebelumnya hanya punya [Store.ADZAN_URI] tanpa daftar; berkas itu harus
     * tetap terlihat dan terpilih — bukan menghilang dari layar seolah tidak pernah dipilih.
     */
    fun tampil(daftar: List<SuaraAdzan>, aktif: String): List<SuaraAdzan> =
        if (aktif.isBlank() || daftar.any { it.uri == aktif }) daftar
        else listOf(SuaraAdzan(namaDariUri(aktif), aktif)) + daftar

    /** Nama jenis suara yang sedang aktif, untuk ditampilkan; kosong bila belum ada berkas. */
    fun namaAktif(daftar: List<SuaraAdzan>, aktif: String): String =
        daftar.firstOrNull { it.uri == aktif }?.nama ?: if (aktif.isBlank()) "" else namaDariUri(aktif)

    /** URI yang dipakai setelah [uri] dibuang: berkas tersisa pertama, atau kosong bila habis. */
    fun aktifSetelahHapus(daftar: List<SuaraAdzan>, uri: String, aktif: String): String =
        if (uri != aktif) aktif else daftar.firstOrNull { it.uri != uri }?.uri.orEmpty()

    /** Buang pemisah/baris baru dari sebuah ruas agar format satu-baris-per-entri tetap utuh. */
    private fun bersih(s: String) =
        s.replace('\n', ' ').replace('\r', ' ').replace(PEMISAH, ' ').trim()
}
