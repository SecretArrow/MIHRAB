package id.or.kemenag.yasintahlil.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Menuliskan satu kartu bacaan (judul, Arab, latin, terjemahan, sumber) menjadi gambar PNG
 * bergaya islami-zamrud — aksi "Bagikan sebagai gambar" pada lembar aksi kartu bacaan.
 *
 * **Kenapa lewat berkas + FileProvider.** Sejak API 24, URI `file://` tidak boleh meninggalkan
 * aplikasi — berkas di `cacheDir` harus diteruskan sebagai `content://` lewat `FileProvider`.
 * Provider-nya sudah terpasang di manifes (`${applicationId}.fileprovider`, lihat komentar
 * di `AndroidManifest.xml`) dan folder yang diizinkannya dideklarasikan di `res/xml/file_paths.xml`
 * (`cache-path` bernama "bagikan"); folder serta nama berkas di sini harus tetap cocok dengannya.
 *
 * **Kenapa warna diteruskan pemanggil.** Kartu bacaan bisa sedang memakai "tema halaman"
 * (kertas krem/gelap) atau tema aplikasi (terang/gelap). Gambar yang dibagikan mengikuti kertas
 * yang sedang dilihat pengguna — warna latar/teks/aksen diambil dari `MaterialTheme.colorScheme`
 * di `BacaanCard` dan dikirim ke sini sebagai `Int` ARGB, sehingga berkas ini tidak menebak-nebak
 * tema dan hasilnya sama di layar maupun di gambar.
 *
 * **Kenapa PNG di cache dibersihkan.** Tiap pembagian menulis PNG baru; tanpa pembersihan,
 * cache perlahan terisi gambar yang tidak akan pernah dibagikan lagi. [bersihkan] membuang yang
 * lama dan hanya menyimpan [SIMPAN_MAKS] berkas terbaru — ia dipanggil tepat **setelah**
 * [buat] selesai, dan berkas yang barusan dibuat adalah yang termuda, jadi tidak ikut terbuang.
 *
 * Bagian murninya ([namaBerkas], [pilihYangDisimpan], [susunFooter], [denganAlpha]) tidak
 * menyentuh Android sehingga diuji langsung di JVM (`BagikanGambarTest`).
 */
object BagikanGambar {

    /** Lebar hasil 1080 px: cukup tajam untuk cerita/kartu, tetapi tidak memberatkan memori. */
    const val LEBAR = 1080

    /** Tebal tepi (padding) semua sisi, dan lebar area teks yang tersisa sesudahnya. */
    private const val TEPI = 84

    /** Radius sudut kartu — selaras dengan bentuk `MaterialTheme.shapes.large` (24 dp) di layar. */
    private const val SUDUT = 44f

    /** Garis aksen vertikal di kiri kartu: lebar dan posisi kirinya. */
    private const val GARIS_LEBAR = 12
    private const val GARIS_KIRI = 40f

    /** Banyaknya PNG terbaru yang dibiarkan tetap hidup oleh [bersihkan]. */
    const val SIMPAN_MAKS = 8

    /**
     * Folder cache untuk PNG yang akan dibagikan. Nama ini harus sama dengan `path` pada
     * `res/xml/file_paths.xml` — bila berbeda, `FileProvider.getUriForFile` melempar
     * `IllegalArgumentException` dan aksi berbagi gagal.
     */
    private const val FOLDER = "bagikan"

    /** Nama aplikasi pada kaki gambar — penanda asal ketika gambarnya disimpan ulang penerima. */
    private const val KAKI = "Yasin dan Tahlil"

    /**
     * Render satu kartu bacaan menjadi PNG di `cache/bagikan` dan kembalikan berkasnya.
     *
     * Tinggi dihitung lebih dulu dari `StaticLayout` tiap blok (ia mengukur sendiri tinggi
     * pelipatannya), sehingga bitmap dibuat sekali dengan tinggi pas — bukan bitmap tinggi
     * maksimum lalu dipotong: sudut bawah kartu ikut membulat rapi tanpa dua kali menggambar.
     *
     * Blok Arab ditata dengan `ALIGN_NORMAL` + heuristik `FIRSTSTRONG_RTL`. `Layout.Alignment`
     * diartikan **relatif arah paragraf**: `ALIGN_NORMAL` menempelkan baris ke pinggir mulai
     * paragraf — untuk paragraf kanan-ke-kiri itu adalah pinggir KANAN (dipastikan pada
     * `Layout.getLineLeft` di AOSP: `ALIGN_NORMAL` + arah RTL menjadi rata kanan, sedangkan
     * `ALIGN_OPPOSITE` justru rata kiri). `FIRSTSTRONG_RTL` menjamin paragraf Arab selalu
     * dihitung RTL — bila teksnya diawali angka/tanda baca lemah, heuristik bawaan (LTR) akan
     * menyeret baris Arab ke pinggir kiri.
     */
    fun buat(
        ctx: Context,
        judul: String,
        arab: String,
        latin: String,
        terjemahan: String,
        sumber: String,
        latar: Int,
        teks: Int,
        aksen: Int,
    ): File {
        val lebarTeks = LEBAR - 2 * TEPI

        // Blok teks: ukurannya sendiri menyimpan "jarak sebelum blok" agar urutan menyusun
        // tinggi dan menggambar tidak boleh berbeda (tinggi pas = jumlah keduanya).
        val blok = mutableListOf<Pair<Int, StaticLayout>>()
        blok += 0 to tata(
            judul, catTeks(teks, 44f, tebal = true), lebarTeks,
        )
        if (arab.isNotBlank()) {
            blok += 44 to tataArab(arab, catTeks(teks, 52f), lebarTeks)
        }
        if (latin.isNotBlank()) {
            blok += 36 to tata(latin, catTeks(denganAlpha(teks, 204), 34f, miring = true), lebarTeks)
        }
        if (terjemahan.isNotBlank()) {
            blok += 24 to tata(terjemahan, catTeks(teks, 34f), lebarTeks)
        }
        if (sumber.isNotBlank()) {
            blok += 28 to tata("Sumber: $sumber", catTeks(denganAlpha(teks, 153), 26f), lebarTeks)
        }
        val tanggal = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date())
        blok += 64 to tata(susunFooter(tanggal), catTeks(denganAlpha(teks, 153), 26f), lebarTeks)

        val tinggi = TEPI + blok.sumOf { (jarak, tata) -> jarak + tata.height } + TEPI
        val gambar = Bitmap.createBitmap(LEBAR, tinggi, Bitmap.Config.ARGB_8888)
        val kanvas = Canvas(gambar)
        val cat = Paint(Paint.ANTI_ALIAS_FLAG)

        // Latar kartu membulat di keempat sudutnya — tinggi bitmap memang tinggi pasnya.
        cat.color = latar
        kanvas.drawRoundRect(0f, 0f, LEBAR.toFloat(), tinggi.toFloat(), SUDUT, SUDUT, cat)
        // Garis aksen vertikal di kiri: penanda kartu bacaan yang sama dengan tepi zamrud
        // kartunya di layar; ujungnya ikut membulat supaya terbaca sebagai garis, bukan bilah.
        cat.color = aksen
        kanvas.drawRoundRect(
            GARIS_KIRI, TEPI.toFloat(), GARIS_KIRI + GARIS_LEBAR, (tinggi - TEPI).toFloat(),
            GARIS_LEBAR / 2f, GARIS_LEBAR / 2f, cat,
        )

        var y = TEPI
        for ((jarak, tata) in blok) {
            y += jarak
            kanvas.save()
            kanvas.translate(TEPI.toFloat(), y.toFloat())
            tata.draw(kanvas)
            kanvas.restore()
            y += tata.height
        }

        val folder = File(ctx.cacheDir, FOLDER)
        folder.mkdirs()
        val berkas = File(folder, namaBerkas(System.currentTimeMillis()))
        FileOutputStream(berkas).use { keluar ->
            gambar.compress(Bitmap.CompressFormat.PNG, 100, keluar)
        }
        return berkas
    }

    /**
     * Buang PNG lama di folder bagikan, kecuali [simpanMaks] yang terbaru (diturutkan dari
     * `lastModified`). Dipanggil pemanggil [buat] **setelah** render: berkas yang barusan dibuat
     * adalah yang termuda, jadi selalu selamat; yang terbuang hanya yang lebih tua.
     */
    fun bersihkan(ctx: Context, simpanMaks: Int = SIMPAN_MAKS) {
        val folder = File(ctx.cacheDir, FOLDER)
        val berkas = folder.listFiles { f -> f.isFile && f.name.endsWith(".png") } ?: return
        val simpan = pilihYangDisimpan(berkas.map { it.name to it.lastModified() }, simpanMaks)
        berkas.filter { it.name !in simpan }.forEach { it.delete() }
    }

    /**
     * Buka pilihan berbagi sistem untuk [berkas]. Izin baca diberikan lewat bendera pada intent
     * (chooser meneruskannya) — tanpa itu aplikasi tujuan tidak bisa membuka `content://` URI.
     * Mengikuti pola `Context.shareText` di `UiKit`: chooser + `startActivity` dari activity.
     */
    fun bagikan(ctx: Context, berkas: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", berkas)
        val kirim = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(kirim, "Bagikan gambar"))
    }

    /** Nama berkas PNG yang unik per waktu pembuatannya — "kartu_<stempel>.png". */
    fun namaBerkas(stempel: Long): String = "kartu_$stempel.png"

    /**
     * Nama-nama berkas yang boleh tetap hidup dari daftar [entri] (nama, waktu ubah terakhir):
     * [simpanMaks] yang termuda. Murni, sehingga aturan "yang mana yang dibuang" diuji di JVM
     * tanpa menyentuh sistem berkas.
     */
    fun pilihYangDisimpan(entri: List<Pair<String, Long>>, simpanMaks: Int): Set<String> =
        entri.sortedByDescending { it.second }.take(simpanMaks.coerceAtLeast(0)).map { it.first }.toSet()

    /** Baris kaki gambar: nama aplikasi beserta tanggal kartu dibagikan. */
    fun susunFooter(tanggal: String): String = "$KAKI · $tanggal"

    /**
     * [warna] ARGB dengan saluran alfa diganti [alpha] (0–255): teks latin/sumber/kaki memakai
     * warna teks yang sama dengan kartunya, hanya lebih pudar — bukan warna baru yang mungkin
     * tidak lagi selaras dengan "kertas" temanya.
     */
    fun denganAlpha(warna: Int, alpha: Int): Int =
        (warna and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    /** Kuas teks blok biasa (kiri-ke-kanan): tebal/miring sesuai parameternya. */
    private fun catTeks(warna: Int, ukuran: Float, tebal: Boolean = false, miring: Boolean = false) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = warna
            textSize = ukuran
            isFakeBoldText = tebal
            if (miring) textSkewX = -0.25f
        }

    /** Lipat [isi] menjadi blok multi-baris rata kiri (paragraf Indonesia). */
    private fun tata(isi: String, cat: TextPaint, lebar: Int): StaticLayout =
        StaticLayout.Builder.obtain(isi, 0, isi.length, cat, lebar)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.3f)
            .setIncludePad(false)
            .build()

    /**
     * Lipat teks Arab menjadi blok multi-baris rata kanan — lihat [buat] mengapa harus
     * `ALIGN_NORMAL` + `FIRSTSTRONG_RTL`, bukan `ALIGN_OPPOSITE`. Jarak baris lebih renggang
     * (1,7) daripada blok Latin supaya harakat di puncak huruf tidak menempel baris di atasnya.
     */
    private fun tataArab(isi: String, cat: TextPaint, lebar: Int): StaticLayout =
        StaticLayout.Builder.obtain(isi, 0, isi.length, cat, lebar)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setLineSpacing(0f, 1.7f)
            .setIncludePad(false)
            .build()
}
