package id.or.kemenag.yasintahlil.core.util

/**
 * Penyaring cepat di dalam satu layar berisi banyak entri.
 *
 * `plan.txt` §"UI/UX dan Navigasi" meminta *"pencarian dan akses cepat pada halaman yang memiliki
 * banyak konten"*. Pencarian terpadu (`feature/search`) menjawab pertanyaan "di mana bacaan X di
 * seluruh aplikasi"; penyaring ini menjawab pertanyaan yang berbeda — "di mana X di antara puluhan
 * entri yang sedang saya lihat" — jadi ia menyaring daftar di layar, bukan berpindah layar.
 *
 * Berbeda dari `Pencarian.cari`, kata kunci kosong berarti **semua** entri ditampilkan: daftar yang
 * sedang dibaca tidak boleh mendadak kosong hanya karena kotak pencariannya masih kosong.
 *
 * Murni tanpa Android supaya aturan pencocokannya dapat diuji sebagai unit test biasa.
 */
object Saring {

    /** True bila [kata] kosong, atau salah satu [teks] memuatnya (tanpa beda huruf besar/kecil). */
    fun cocok(kata: String, teks: List<String>): Boolean {
        val kunci = kata.trim()
        if (kunci.isBlank()) return true
        return teks.any { it.contains(kunci, ignoreCase = true) }
    }

    /**
     * Saring [item]; [teks] mengembalikan kumpulan teks yang boleh dicocokkan untuk satu item
     * (judul, teks Arab, transliterasi, terjemahan). Urutan hasil mengikuti urutan [item].
     */
    fun <T> tapis(kata: String, item: List<T>, teks: (T) -> List<String>): List<T> {
        if (kata.isBlank()) return item
        return item.filter { cocok(kata, teks(it)) }
    }
}
