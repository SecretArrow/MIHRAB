package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.core.util.ContentValidator
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Isi berkas metadata pembaruan (`_meta.json`) — ditulis saat sebuah bundel dipasang, dibaca
 * kembali untuk menampilkan keadaan konten yang sedang dipakai.
 *
 * Berkas ini **bukan** bundelnya sendiri: `_meta.json` ditulis `ContentUpdateManager` ke dalam
 * direktori pembaruan, dan hanya kelas itu yang membacanya. Kunci-kuncinya karena itu tinggal di
 * sini sebagai data, bukan dieja di badan penulis dan pembacanya.
 *
 * Dahulu `metaJson` dan `status()` menuliskan tiap kunci sendiri-sendiri — tujuh kunci, dua kali.
 * Tak ada apa pun yang membandingkannya, dan akibatnya sunyi persis seperti kekeliruan bundel yang
 * lain: mengganti nama kunci di satu sisi saja membuat layar **"Pembaruan konten"** melaporkan
 * *tidak ada pembaruan terpasang* padahal berkasnya sudah ada dan sedang dipakai — dan karena
 * tombol **"Kembalikan ke konten bawaan"** hanya muncul saat ada yang terpasang, pengguna
 * kehilangan satu-satunya cara kembali ke konten bawaan, tanpa satu pun pesan galat.
 *
 * Isi berkasnya murni JSON, jadi penulis dan pembacanya dipisahkan ke sini sebagai fungsi murni
 * yang dapat diuji langsung di JVM — `ContentUpdateManager` sendiri butuh `Context` (direktori
 * aplikasi dan assets), sehingga uji perilaku biasa tak dapat menjangkaunya. Pola yang sama dipakai
 * [entriRujukan] untuk rujukan berkas.
 */
object MetaPembaruan {

    /** Versi skema metadata yang ditulis aplikasi. */
    const val SKEMA = "skema"

    /** Waktu pemasangan, sudah diformat ([FORMAT_WAKTU]). */
    const val DIPASANG = "dipasang"

    /** Penerbit bundel, apa adanya dari metadata bundel. */
    const val PENERBIT = "penerbit"

    /** Catatan bundel, apa adanya. */
    const val CATATAN = "catatan"

    /** Larik ringkasan berkas yang dipasang. */
    const val BERKAS = "berkas"

    /** Nama satu berkas konten di dalam [BERKAS]. */
    const val NAMA = "nama"

    /** Kategori sumber satu berkas di dalam [BERKAS]. */
    const val KATEGORI = "kategori"

    /**
     * Bentuk waktu pada [DIPASANG]. Dinyatakan di sini karena hanya penulisnya yang memformat;
     * pembaca menampilkan teksnya apa adanya, sehingga format yang berubah tak membuat metadata
     * lama tak terbaca.
     *
     * Waktunya ditulis pada zona perangkat ([TimeZone.getDefault]) — itulah yang dibaca pengguna di
     * layar "Pembaruan konten". [tulis] menerima zonanya sebagai parameter agar ujinya sama di
     * mesin mana pun, bukan bergantung pada zona mesin yang menjalankannya.
     */
    const val FORMAT_WAKTU = "yyyy-MM-dd HH:mm"

    /**
     * Keadaan konten pembaruan yang sedang dipakai — hasil pembacaan `_meta.json`.
     *
     * [aktif] **diturunkan** dari [berkas], bukan disimpan sendiri: dua fakta tentang hal yang sama
     * adalah cara paling halus untuk membuat layar menampilkan keadaan yang tak pernah ada (mis.
     * "ada pembaruan" tanpa satu berkas pun untuk ditampilkan, atau sebaliknya).
     */
    data class Status(
        val skema: Int = 0,
        val dipasang: String = "",
        val penerbit: String = "",
        val catatan: String = "",
        val berkas: List<BundelKonten.Berkas> = emptyList(),
    ) {
        val aktif: Boolean get() = berkas.isNotEmpty()
    }

    /**
     * Bangun isi `_meta.json` dari hasil pemasangan [hasil] dan ringkasannya [ringkas].
     *
     * [waktu] dan [zona] dapat diberikan agar ujinya tetap sama setiap kali dijalankan; produksi
     * memakai waktu sekarang pada zona perangkat.
     */
    fun tulis(
        hasil: BundelKonten.Hasil,
        ringkas: List<BundelKonten.Berkas>,
        waktu: Date = Date(),
        zona: TimeZone = TimeZone.getDefault(),
    ): JSONObject {
        val berkas = JSONArray()
        ringkas.forEach { b ->
            berkas.put(
                JSONObject()
                    .put(NAMA, b.nama)
                    // Sumber ditulis lewat ejaan utama bersama, sama seperti pembacanya
                    // ([ContentValidator.sumberBerkas] menerima kedua ejaan).
                    .put(ContentValidator.KUNCI_SUMBER_UTAMA, b.sumberUrl)
                    .put(KATEGORI, JSONArray(b.kategori)),
            )
        }
        val format = SimpleDateFormat(FORMAT_WAKTU, Locale.US).apply { timeZone = zona }
        return JSONObject()
            .put(SKEMA, hasil.skema)
            .put(DIPASANG, format.format(waktu))
            .put(PENERBIT, hasil.penerbit)
            .put(CATATAN, hasil.catatan)
            .put(BERKAS, berkas)
    }

    /**
     * Baca `_meta.json` dari [teks]. JSON yang rusak atau kosong tidak melempar: hasilnya
     * [Status] bawaan (tidak aktif), sebab berkas metadata yang rusak hanya berarti aplikasi
     * kembali membaca konten bawaan — bukan alasan menjatuhkan layar Pengaturan.
     */
    fun baca(teks: String): Status {
        val j = runCatching { JSONObject(teks) }.getOrNull() ?: return Status()
        val arr = j.optJSONArray(BERKAS) ?: JSONArray()
        val berkas = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            // Berkas tanpa nama tak dapat ditampilkan maupun dijelaskan asalnya; dilewati, bukan
            // ditampilkan sebagai baris kosong.
            val nama = o.optString(NAMA).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val kat = o.optJSONArray(KATEGORI)
                ?.let { k -> (0 until k.length()).map { k.getString(it) } }.orEmpty()
            BundelKonten.Berkas(nama, ContentValidator.sumberBerkas(o), kat)
        }
        return Status(
            skema = j.optInt(SKEMA),
            dipasang = j.optString(DIPASANG),
            penerbit = j.optString(PENERBIT),
            catatan = j.optString(CATATAN),
            berkas = berkas,
        )
    }
}
