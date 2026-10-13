package id.or.kemenag.yasintahlil.core.data

import org.json.JSONObject

/**
 * Kunci **rujukan berkas** bernilai satu teks, berurutan menurut prioritas.
 *
 * Satu tempat, karena kunci yang sama dibaca dua peran: pemeriksa bundel (`BundelKonten`, yang
 * menolak rujukan mati sebelum bundel dipasang) dan pembaca konten (`ContentRepo`, yang benar-benar
 * mengikuti rujukannya). Dahulu keduanya menuliskan daftar ini sendiri-sendiri, dan hanya
 * `ContentRepo.dzikir` yang mengenal ejaan kedua — sehingga bundel ber-`rujukanFile` pada Tahlil
 * atau bacaan sholat lolos pemasangan, lalu entri yang mengandalkan rujukan itu tampil **kosong**
 * tanpa galat: rujukannya sudah "diperiksa" sebagai sah, tetapi tidak pernah dibaca.
 */
internal val KUNCI_RUJUKAN = listOf("rujukan_file", "rujukanFile")

/** Kunci rujukan berkas bernilai larik teks (beberapa lafaz dari satu entri dzikir). */
internal const val KUNCI_RUJUKAN_DAFTAR = "rujukan"

/**
 * Nilai rujukan berkas pada [o], satu per ejaan pada [KUNCI_RUJUKAN] yang benar-benar ada.
 *
 * Mengembalikan **semua** ejaan yang ada, bukan hanya yang pertama: pemeriksa menelusuri setiap
 * kemunculan kunci rujukan di seluruh dokumen, jadi pembaca pun harus mengikuti semuanya — kalau
 * tidak, rujukan yang diperiksa bisa berbeda dari rujukan yang dibaca, persis kekeliruan yang
 * membuat teks tampil kosong tanpa galat.
 */
internal fun rujukanTeksSemua(o: JSONObject): List<String> =
    KUNCI_RUJUKAN.filter { o.opt(it) is String }.map { o.optString(it) }

/** Rujukan berkas tunggal pada [o]: ejaan pertama yang ada, atau `null` bila tidak merujuk. */
internal fun rujukanTeks(o: JSONObject): String? = rujukanTeksSemua(o).firstOrNull()

/**
 * Cari entri ber-`id` di dalam sebuah dokumen konten.
 *
 * Berkas konten menyimpan entri yang dapat dirujuk dalam tiga bentuk: `bagian` (Tahlil),
 * `bacaan` (sholat jenazah & langkah sholat), dan `kelompok[].items[]` (dzikir). Dua bentuk
 * pertama sudah lama dipakai lewat `rujukan_file` = `berkas.json#id`; bentuk ketiga ditambahkan
 * di sini agar **pustaka dzikir dapat menunjuk entrinya sendiri**. Tanpa itu, kelompok baru
 * seperti "Doa memohon ampunan" hanya bisa dibuat dengan menyalin lafaz yang sudah terverifikasi
 * ke tempat kedua — dan setiap salinan adalah kesempatan baru untuk menyimpang dari sumbernya.
 *
 * Nama wadah dan kunci `id`-nya datang dari [BentukKonten], satu tempat bersama [ContentRepo] yang
 * benar-benar membaca dokumen itu — lihat catatan di sana.
 *
 * @return objek entri, atau `null` bila [id] tidak ditemukan pada bentuk mana pun. `null` berarti
 *   rujukan mati; pemanggil harus memperlakukannya sebagai teks kosong, bukan mengarang isi.
 */
internal fun entriRujukan(dokumen: JSONObject, id: String): JSONObject? {
    if (id.isBlank()) return null
    val langsung = BentukKonten.DAFTAR_ENTRI.firstNotNullOfOrNull { dokumen.optJSONArray(it) }
    if (langsung != null) {
        for (i in 0 until langsung.length()) {
            val o = langsung.optJSONObject(i) ?: continue
            if (o.optString(BentukKonten.ID) == id) return o
        }
    }
    val kelompok = dokumen.optJSONArray(BentukKonten.KELOMPOK) ?: return null
    for (g in 0 until kelompok.length()) {
        val items = kelompok.optJSONObject(g)?.optJSONArray(BentukKonten.ITEMS) ?: continue
        for (i in 0 until items.length()) {
            val o = items.optJSONObject(i) ?: continue
            if (o.optString(BentukKonten.ID) == id) return o
        }
    }
    return null
}
