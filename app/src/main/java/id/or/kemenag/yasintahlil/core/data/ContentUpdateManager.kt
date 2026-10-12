package id.or.kemenag.yasintahlil.core.data

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pemasangan bundel pembaruan konten ke penyimpanan aplikasi — memenuhi `plan.txt`:
 * *"Sediakan mekanisme pembaruan konten dengan validasi sumber."*
 *
 * Konten bawaan tetap berada di `assets/content` (tidak pernah ditimpa). Pembaruan ditulis ke
 * `filesDir/content_update/` dan dibaca lebih dulu oleh [ContentRepo]; berkas bawaan dipakai lagi
 * begitu pembaruan dikembalikan atau berkasnya rusak.
 *
 * Pembaruan **hanya** masuk lewat berkas yang dipilih pengguna sendiri (SAF, tanpa izin apa pun):
 * tidak ada unduhan otomatis, sehingga tidak ada konten baru yang datang tanpa tindakan sadar
 * pengguna. Sebelum ditulis, seluruh bundel diperiksa [BundelKonten] — sumber resmi, kategori,
 * harakat, bebas placeholder, dan rujukan berkas yang benar-benar ada. Satu berkas yang gagal
 * membatalkan seluruh bundel.
 */
class ContentUpdateManager(private val ctx: Context) {

    /** Direktori penimpaan konten, sejajar dengan `assets/content`. */
    val dir: File get() = File(ctx.filesDir, DIR)

    private val staging: File get() = File(ctx.filesDir, "$DIR.tmp")
    private val meta: File get() = File(dir, META)

    /** Hasil pemasangan sebuah bundel. */
    sealed interface Hasil {
        /** Bundel lolos seluruh pemeriksaan dan sudah dipasang. */
        data class Terpasang(val berkas: List<BundelKonten.Berkas>) : Hasil

        /** Bundel ditolak; [alasan] adalah galat per berkas, apa adanya untuk ditampilkan. */
        data class Ditolak(val alasan: List<String>) : Hasil
    }

    /** Keadaan konten yang sedang dipakai. */
    data class Status(
        val aktif: Boolean = false,
        val dipasang: String = "",
        val penerbit: String = "",
        val catatan: String = "",
        val berkas: List<BundelKonten.Berkas> = emptyList(),
    )

    /** Keadaan pembaruan saat ini, dibaca dari berkas metadata di disk. */
    fun status(): Status {
        val j = runCatching { JSONObject(meta.readText()) }.getOrNull() ?: return Status()
        val arr = j.optJSONArray("berkas") ?: JSONArray()
        val berkas = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val nama = o.optString("nama").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val kat = o.optJSONArray("kategori")?.let { k -> (0 until k.length()).map { k.getString(it) } }.orEmpty()
            BundelKonten.Berkas(nama, o.optString("sumber_url"), kat)
        }
        return Status(
            aktif = berkas.isNotEmpty(),
            dipasang = j.optString("dipasang"),
            penerbit = j.optString("penerbit"),
            catatan = j.optString("catatan"),
            berkas = berkas,
        )
    }

    /** Baca bundel dari berkas yang dipilih pengguna, lalu pasang bila lolos pemeriksaan. */
    fun pasangDariUri(uri: Uri): Hasil =
        runCatching { pasang(bacaTeks(uri)) }
            .getOrElse { Hasil.Ditolak(listOf("Berkas bundel tidak dapat dibaca: ${it.message.orEmpty().take(160)}")) }

    /**
     * Periksa lalu pasang [teks] sebagai bundel. Penulisan bersifat atomik: seluruh berkas ditulis
     * ke direktori sementara, baru ditukar setelah semua berhasil, sehingga kegagalan di tengah
     * jalan tidak meninggalkan konten setengah terpasang.
     */
    fun pasang(teks: String): Hasil {
        val hasil = BundelKonten.baca(teks)
        // Rujukan berkas (`berkas.json#id`) hanya dapat diperiksa setelah bentuk bundelnya sah —
        // dan hanya di sini, karena daftar berkas bawaan ada pada `Context`, bukan pada aturan
        // murni `BundelKonten`. Tanpa pemeriksaan ini bundel dengan rujukan mati tetap diterima
        // sebagai "bersumber resmi", lalu menghentikan layar Dzikir/Tahlil/Panduan Sholat saat
        // dibuka — kegagalan yang jauh dari tempat sebabnya.
        val galatRujukan = if (hasil.sah) BundelKonten.periksaRujukan(hasil, berkasAset()) else emptyList()
        if (!hasil.sah || galatRujukan.isNotEmpty()) {
            val alasan = (hasil.ditolak + galatRujukan).ifEmpty { listOf("Bundel tidak memuat berkas konten.") }
            Log.w(TAG, "Bundel pembaruan ditolak: ${alasan.size} alasan")
            return Hasil.Ditolak(alasan)
        }

        staging.deleteRecursively()
        if (!staging.mkdirs()) return Hasil.Ditolak(listOf("Tidak dapat menyiapkan folder pembaruan."))

        val ringkas = BundelKonten.ringkasan(hasil)
        return runCatching {
            hasil.berkas.forEach { (nama, doc) -> File(staging, nama).writeText(doc.toString(1)) }
            File(staging, META).writeText(metaJson(hasil, ringkas).toString(1))
            // Tukar direktori: setelah baris ini, seluruh bundel aktif sekaligus.
            dir.deleteRecursively()
            check(staging.renameTo(dir)) { "Tidak dapat mengaktifkan folder pembaruan." }
            Log.i(TAG, "Bundel pembaruan dipasang: ${ringkas.size} berkas")
            Hasil.Terpasang(ringkas)
        }.getOrElse {
            staging.deleteRecursively()
            Log.e(TAG, "Pemasangan bundel gagal", it)
            Hasil.Ditolak(listOf("Pemasangan gagal: ${it.message.orEmpty().take(160)}"))
        }
    }

    /** Nama berkas konten bawaan aplikasi — sasaran sah bagi rujukan berkas pada bundel. */
    private fun berkasAset(): Set<String> =
        runCatching { ctx.assets.list("content")?.toSet().orEmpty() }.getOrElse {
            Log.w(TAG, "Daftar berkas konten bawaan tidak dapat dibaca", it)
            emptySet()
        }

    /** Hapus seluruh pembaruan; konten kembali sepenuhnya ke bawaan aplikasi. */
    fun kembalikan(): Boolean {
        val ok = !dir.exists() || dir.deleteRecursively()
        staging.deleteRecursively()
        Log.i(TAG, "Pembaruan konten dikembalikan ke bawaan (berhasil=$ok)")
        return ok
    }

    private fun metaJson(hasil: BundelKonten.Hasil, ringkas: List<BundelKonten.Berkas>): JSONObject {
        val berkas = JSONArray()
        ringkas.forEach { b ->
            berkas.put(JSONObject()
                .put("nama", b.nama)
                .put("sumber_url", b.sumberUrl)
                .put("kategori", JSONArray(b.kategori)))
        }
        return JSONObject()
            .put("skema", hasil.skema)
            .put("dipasang", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()))
            .put("penerbit", hasil.penerbit)
            .put("catatan", hasil.catatan)
            .put("berkas", berkas)
    }

    /** Baca berkas terpilih dengan batas ukuran agar bundel raksasa tidak menghabiskan memori. */
    private fun bacaTeks(uri: Uri): String {
        val masuk = ctx.contentResolver.openInputStream(uri)
            ?: error("berkas tidak dapat dibuka")
        return masuk.use { aliran ->
            val keluar = ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            while (true) {
                val n = aliran.read(buf)
                if (n < 0) break
                check(keluar.size() + n <= MAX_BYTE) {
                    "bundel terlalu besar (maks ${MAX_BYTE / (1024 * 1024)} MB)"
                }
                keluar.write(buf, 0, n)
            }
            keluar.toByteArray().toString(Charsets.UTF_8)
        }
    }

    companion object {
        const val TAG = "ContentUpdate"
        const val DIR = "content_update"
        const val META = "_meta.json"

        /** Batas ukuran bundel (4 MB) — cukup untuk seluruh konten, jauh di bawah batas memori. */
        const val MAX_BYTE = 4 * 1024 * 1024
    }
}
