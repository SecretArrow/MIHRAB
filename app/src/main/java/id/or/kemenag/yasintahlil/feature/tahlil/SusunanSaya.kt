package id.or.kemenag.yasintahlil.feature.tahlil

import id.or.kemenag.yasintahlil.core.data.PreferensiSusunan
import id.or.kemenag.yasintahlil.core.data.TahlilItem
import id.or.kemenag.yasintahlil.core.data.TahlilSusunan

/**
 * "Susunan saya" — susunan tahlil yang disusun sendiri oleh pengguna (`plan.txt`: *"Sediakan
 * susunan bacaan yang dapat dikonfigurasi"* dan *"Izinkan pengguna memilih susunan bacaan tanpa
 * mengubah teks sumber secara otomatis"*).
 *
 * Yang dapat diubah hanyalah **bagian mana yang ikut dibaca dan urutannya**; teks tiap bagian tetap
 * diambil apa adanya dari berkas sumber beserta atribusinya. Karena itu tidak ada teks keagamaan
 * yang dikarang di sini — susunan ini tidak diklaim sebagai satu-satunya susunan yang benar dan
 * ditampilkan sebagai susunan buatan pengguna, bukan sebagai tradisi tertentu.
 *
 * Sengaja murni (tanpa Android) agar aturan urutannya dapat diuji di JVM.
 *
 * Bentuk yang **disimpan** (id, nama, dan CSV id bagian) tinggal di [PreferensiSusunan] karena
 * nilainya dibaca juga oleh layar Pengaturan; di sini hanya aturan penyusunan yang butuh
 * [TahlilItem].
 */
object SusunanSaya {
    /** Id susunan buatan pengguna; dibedakan dari id susunan di `assets/content/tahlil.json`. */
    const val ID = PreferensiSusunan.ID_SAYA
    const val NAMA = PreferensiSusunan.NAMA_SAYA

    /** Baca daftar id dari preferensi (CSV). Urutan dipertahankan, duplikat dan kosong dibuang. */
    fun parse(csv: String): List<String> = PreferensiSusunan.parse(csv)

    /** Simpan daftar id ke preferensi (CSV). */
    fun serialize(ids: List<String>): String = PreferensiSusunan.serialize(ids)

    /**
     * Terapkan pilihan pengguna pada seluruh bagian tahlil.
     *
     * Bagian yang tidak ada di [pilihan] tidak ikut ditampilkan, dan id yang tidak dikenal (mis.
     * bagian yang dihapus dari bundel konten setelah susunan disimpan) dilewati — bukan membuat
     * layar gagal. Urutan mengikuti [pilihan], bukan urutan berkas.
     */
    fun susun(semua: List<TahlilItem>, pilihan: List<String>): List<TahlilItem> {
        val byId = semua.associateBy { it.id }
        return pilihan.mapNotNull { byId[it] }
    }

    /** Susunan buatan pengguna untuk ditampilkan di pemilih susunan, atau null bila belum ada. */
    fun susunan(pilihan: List<String>): TahlilSusunan? = PreferensiSusunan.susunanSaya(pilihan)
}
