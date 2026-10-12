package id.or.kemenag.yasintahlil.core.data

import org.json.JSONObject

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
 * @return objek entri, atau `null` bila [id] tidak ditemukan pada bentuk mana pun. `null` berarti
 *   rujukan mati; pemanggil harus memperlakukannya sebagai teks kosong, bukan mengarang isi.
 */
internal fun entriRujukan(dokumen: JSONObject, id: String): JSONObject? {
    if (id.isBlank()) return null
    val langsung = dokumen.optJSONArray("bagian") ?: dokumen.optJSONArray("bacaan")
    if (langsung != null) {
        for (i in 0 until langsung.length()) {
            val o = langsung.optJSONObject(i) ?: continue
            if (o.optString("id") == id) return o
        }
    }
    val kelompok = dokumen.optJSONArray("kelompok") ?: return null
    for (g in 0 until kelompok.length()) {
        val items = kelompok.optJSONObject(g)?.optJSONArray("items") ?: continue
        for (i in 0 until items.length()) {
            val o = items.optJSONObject(i) ?: continue
            if (o.optString("id") == id) return o
        }
    }
    return null
}
