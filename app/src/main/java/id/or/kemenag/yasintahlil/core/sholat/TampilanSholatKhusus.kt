package id.or.kemenag.yasintahlil.core.sholat

import id.or.kemenag.yasintahlil.core.data.Bacaan

/**
 * Aturan teks bersama untuk dua layar sholat mayit — Ghaib dan Jenazah.
 *
 * Keduanya menampilkan **medan yang sama** dari berkas kontennya (`sholat_ghaib.json` /
 * `sholat_jenazah.json`): judul, jumlah takbir, syarat, dalil, dan catatan mazhab. Sebelum ini tiap
 * layar merangkainya sendiri, dan salinannya sudah menyimpang tanpa ketahuan — tak satu pun
 * perbedaan di bawah ini tercatat sebagai pilihan:
 *
 *  - **syarat** yang sama dirangkai `"; "` di Ghaib tetapi `", "` lalu diberi titik di Jenazah,
 *    sehingga satu daftar tampak seperti dua aturan yang berbeda;
 *  - **dalil** yang dimuat berkas konten Jenazah tidak pernah ditampilkan di layarnya, padahal
 *    Ghaib menampilkan medan yang sama — konten yang ada tetapi tak pernah terbaca;
 *  - **judul layar** menyebut *"tanpa ruku/sujud"* di Ghaib saja, padahal dalil kedua entri
 *    menyatakan hal yang sama;
 *  - **niat** ditampilkan tanpa terjemahan di Ghaib (`showTrans = false`) tetapi dengan terjemahan
 *    di Jenazah, padahal kedua berkas memuat terjemahannya.
 *
 * Yang **khas tiap layar** tetap tinggal di layarnya: peta judul langkah takbir (`judulLangkah` —
 * Ghaib membaca ta'awudz sebelum Al-Fatihah dan salamnya menoleh dua sisi, Jenazah tidak) dan
 * label chip layar penuh (`LAYAR_PENUH_IMAM` vs `LAYAR_PENUH`). Keduanya perbedaan yang disengaja,
 * dan penjaganya memakunya supaya tidak menyamar sebagai salinan.
 *
 * Batas yang belum ditutup, dan **tidak** diklaim sebagai perbedaan yang disengaja: cara menyajikan
 * niat masih berbeda — Ghaib memberi judul *"Varian niat:"* lalu kartunya tanpa label, Jenazah
 * melabeli tiap kartu *"Niat (…)"*; Ghaib juga menyisipkan satu langkah *"Niat dalam hati…"* yang
 * tak ada di Jenazah. Merapikannya berarti memilih satu penyajian untuk pengguna, bukan sekadar
 * merapikan kode, jadi keputusan itu ditunda — bukan dilupakan.
 */
object TampilanSholatKhusus {

    /** Judul layar: nama entri diikuti jumlah takbirnya. */
    fun judul(nama: String, takbir: Int): String = "$nama — $takbir takbir, tanpa ruku/sujud"

    /** Judul satu langkah takbir: nomor urutnya diikuti judul langkah. */
    fun langkah(nomor: Int, judul: String): String = "$nomor. $judul"

    /**
     * Baris syarat. Perangkaiannya satu untuk kedua layar: dipisah `"; "` supaya tiap syarat tetap
     * terbaca terpisah walau jumlahnya empat (Jenazah), dan diakhiri titik karena isinya kalimat.
     */
    fun syarat(daftar: List<String>): String = "Syarat: " + daftar.joinToString("; ") + "."

    /** Baris dalil — medan yang dimuat kedua berkas konten, jadi keduanya menampilkannya. */
    fun dalil(teks: String): String = "Dalil: $teks"

    /** Baris catatan mazhab. */
    fun catatanMazhab(teks: String): String = "Catatan mazhab: $teks"

    /**
     * Teks bacaan yang ditampilkan: hasil rujukan bersama, atau medan entri itu sendiri bila
     * rujukannya kosong. Aturan cadangan ini dulu disalin utuh di kedua layar.
     */
    fun teksBacaan(
        b: Bacaan,
        arab: String,
        latin: String,
        terjemahan: String,
    ): Triple<String, String, String> =
        Triple(arab.ifBlank { b.arab }, latin.ifBlank { b.latin }, terjemahan.ifBlank { b.terjemahan })
}
