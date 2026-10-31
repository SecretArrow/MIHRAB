package id.or.kemenag.yasintahlil

import java.io.File

/**
 * Pemindai kode sumber untuk uji yang memeriksa **isi berkas**, bukan perilaku kelasnya.
 *
 * Beberapa penjaga di repositori ini bekerja dengan mencari potongan kode di dalam berkas lain —
 * misalnya "layar ini tidak boleh lagi menghitung tujuannya sendiri". Pencarian seperti itu mudah
 * tertipu oleh **komentar**: KDoc yang menjelaskan pola lama memuat tulisan yang sama persis dengan
 * yang dilarang, sehingga penjaganya gagal justru karena penjelasannya bagus. Karena itu komentar
 * dibuang lebih dahulu.
 *
 * Pemindainya sengaja tinggal di satu tempat: aturannya sama untuk semua penjaga, bukan salinan
 * yang berbeda-beda di tiap berkas uji — persis kekeliruan yang sedang dihindari oleh penjaga yang
 * memakainya.
 */
object KodeSumber {

    /** Direktori `src/main`, ditemukan dari akar repositori maupun dari dalam `app/`. */
    fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    /** Isi berkas sumber relatif terhadap [root], mis. `src/main/java/…/HomeScreen.kt`. */
    fun sumber(relatif: String): String = File(root(), relatif).readText()

    /**
     * [teks] tanpa komentar baris (`//`) maupun blok (`/* … */`).
     *
     * @param buangString bila `true`, isi literal string ikut dibuang: string kosong menjadi `""`
     *   (tetap dapat dicari sebagai bawaan kosong) dan string berisi menjadi `<str>`. Dipakai penjaga
     *   yang mencari **nilai bawaan literal**, supaya penjelasan seperti
     *   `store.get(Store.SHOW_ARAB, true)` di dalam KDoc tidak dihitung sebagai pelanggaran. Bila
     *   `false`, string dipertahankan apa adanya agar pencarian potongan berkutip
     *   (mis. `startsWith("yasin")`) tetap bekerja.
     * @param mentah bila `true`, isi string mentah (`"""…"""`) ikut dipertahankan, bukan dibuang.
     *   Bawaannya `false` karena string mentah hampir selalu prosa penjelas, bukan kode. Penjaga
     *   yang memeriksa **argumen pemanggilan fungsi** menyalakannya: argumen pun boleh ditulis
     *   sebagai string mentah, dan bila isinya dibuang, pemanggilan itu luput tanpa jejak.
     */
    fun kodeTanpaKomentar(teks: String, buangString: Boolean = false, mentah: Boolean = false): String {
        val sb = StringBuilder()
        var i = 0
        var blok = 0
        while (i < teks.length) {
            when {
                blok > 0 -> if (teks.startsWith("*/", i)) { blok--; i += 2 } else i++
                teks.startsWith("//", i) -> {
                    val akhir = teks.indexOf('\n', i)
                    i = if (akhir < 0) teks.length else akhir
                }
                teks.startsWith("/*", i) -> { blok++; i += 2 }
                // String mentah (`"""…"""`) tidak berisi kode, jadi dibuang seluruhnya — kecuali
                // bila [mentah] memintanya dipertahankan.
                teks.startsWith("\"\"\"", i) -> {
                    val mulai = i
                    val akhir = teks.indexOf("\"\"\"", i + 3)
                    i = if (akhir < 0) teks.length else akhir + 3
                    if (mentah) sb.append(teks, mulai, i)
                }
                teks[i] == '"' -> {
                    val mulai = i
                    i++
                    var kosong = true
                    while (i < teks.length && teks[i] != '"' && teks[i] != '\n') {
                        kosong = false
                        i += if (teks[i] == '\\') 2 else 1
                    }
                    i++
                    if (!buangString) sb.append(teks, mulai, minOf(i, teks.length))
                    else sb.append(if (kosong) "\"\"" else "<str>")
                }
                else -> { sb.append(teks[i]); i++ }
            }
        }
        return sb.toString()
    }

    /**
     * Isi setiap literal string di dalam [teks] — baik `"…"` maupun `"""…"""` — di luar komentar.
     *
     * Dipakai penjaga yang memeriksa **nilai yang benar-benar tampil**: teks yang disisipkan ke dalam
     * kalimat panjang tak lagi terbaca oleh pencarian potongan berkutip utuh (`"sunnah.com"`), sebab
     * bunyinya kini `"… — sunnah.com"`. Isi bentuk mentah pun ikut dibaca, sebab [kodeTanpaKomentar]
     * membuangnya seluruhnya dan host di dalamnya karena itu luput.
     *
     * Urutan escape `\` dihormati supaya tanda kutip yang di-escape tidak memotong literalnya.
     */
    fun literalString(teks: String): List<String> {
        val hasil = mutableListOf<String>()
        var i = 0
        var blok = 0
        while (i < teks.length) {
            when {
                blok > 0 -> if (teks.startsWith("*/", i)) { blok--; i += 2 } else i++
                teks.startsWith("//", i) -> {
                    val akhir = teks.indexOf('\n', i)
                    i = if (akhir < 0) teks.length else akhir
                }
                teks.startsWith("/*", i) -> { blok++; i += 2 }
                teks.startsWith("\"\"\"", i) -> {
                    val akhir = teks.indexOf("\"\"\"", i + 3)
                    hasil += teks.substring(i + 3, if (akhir < 0) teks.length else akhir)
                    i = if (akhir < 0) teks.length else akhir + 3
                }
                teks[i] == '"' -> {
                    val isi = StringBuilder()
                    i++
                    while (i < teks.length && teks[i] != '"' && teks[i] != '\n') {
                        if (teks[i] == '\\' && i + 1 < teks.length) {
                            isi.append(teks[i + 1]); i += 2
                        } else {
                            isi.append(teks[i]); i++
                        }
                    }
                    i++
                    hasil += isi.toString()
                }
                else -> i++
            }
        }
        return hasil
    }
}
