package id.or.kemenag.yasintahlil.feature.asmaul

import android.content.Context
import org.json.JSONObject

/**
 * Pembaca daftar Asmaul Husna dari aset `assets/asmaul/asmaul_husna.json` — leksikon referensi
 * sembilan puluh sembilan nama Allah.
 *
 * **Mengapa di aset, bukan di Kotlin.** Aturan repo ini tegas: teks Arab tidak boleh
 * tertulis di kode — seluruhnya tinggal di berkas aset supaya sumbernya tercatat dan
 * terkelola terpisah dari logika (dijaga `ArabicTextTest` dan `ContentSourceTest`). Daftar ini
 * karenanya berpindah ke aset; penjelasan asal-usulnya (risalah Al-Qushairi, sebagian riwayat
 * Tirmidzi, variasi urutan antar sumber, penomoran mulai "Allah" hingga "Ash-Shabur" ke-99)
 * tinggal di dalam berkas itu sendiri pada kunci `keterangan`.
 *
 * **Mengapa bukan di bundel konten `assets/content`.** Jalur bundel menuntut setiap teks
 * menyebut sumber resmi daring yang dapat dicocokkan kata-per-kata (whitelist
 * `tools/validate_content.py`). Daftar 99 nama yang masyhur tidak bersumber tunggal pada satu
 * URL resmi — melekatkan satu URL padanya berarti mengarang rujukan. Jalur aset mandiri
 * (seperti dokumen legal di `assets/legal`) lebih jujur: datanya lengkap, batas pengetahuannya
 * dijelaskan di tempat yang sama.
 *
 * Murni pembaca: tanpa keadaan, tanpa cache — 99 baris kecil yang dibaca sekali saat layarnya
 * terbuka (pola `DokumenLegal.baca`).
 */
object AsmaulData {

    /** Lokasi berkas di dalam aset aplikasi. */
    const val BERKAS = "asmaul/asmaul_husna.json"

    /** Satu nama: nomor urutnya, tulisan Arab berharakat, alihaksara latin, dan artinya. */
    data class Nama(
        val no: Int,
        val arab: String,
        val latin: String,
        val arti: String,
    )

    /** Penjelasan asal-usul daftar dari berkas yang sama — ditampilkan layar sebagai pengantar. */
    data class Daftar(val keterangan: String, val nama: List<Nama>)

    /**
     * Baca seluruh daftar. `null` hanya bila berkasnya tak terbaca/tak terurai — layar
     * menangani keadaan itu sebagai galat biasa, bukan daftar kosong: "sepertinya berhasil
     * membaca 0 nama" lebih menyesatkan daripada galat yang jujur.
     */
    fun muat(ctx: Context): Daftar? = runCatching {
        val teks = ctx.assets.open(BERKAS).bufferedReader().use { it.readText() }
        val akar = JSONObject(teks)
        val larik = akar.getJSONArray("nama")
        val nama = buildList {
            for (i in 0 until larik.length()) {
                val n = larik.getJSONObject(i)
                add(
                    Nama(
                        no = n.getInt("no"),
                        arab = n.getString("arab"),
                        latin = n.getString("latin"),
                        arti = n.getString("arti"),
                    ),
                )
            }
        }
        Daftar(keterangan = akar.optString("keterangan"), nama = nama)
    }.getOrNull()
}
