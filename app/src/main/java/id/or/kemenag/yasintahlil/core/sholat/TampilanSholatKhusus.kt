package id.or.kemenag.yasintahlil.core.sholat

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
 * Yang **khas tiap layar** tetap tinggal di layarnya: judul takbir 1 dan salam (Ghaib membaca
 * ta'awudz sebelum Al-Fatihah dan salamnya menoleh dua sisi, Jenazah tidak) dan label chip layar
 * penuh (`LAYAR_PENUH_IMAM` vs `LAYAR_PENUH`). Keduanya perbedaan yang disengaja, dan penjaganya
 * memakunya supaya tidak menyamar sebagai salinan.
 *
 * Judul takbir 2–4 tidak khas siapa pun — keduanya menyebut bacaan yang sama dengan nama yang
 * sama — jadi ejaannya tinggal di [judulTakbirBersama]. Sebelumnya ketiganya diketik di kedua
 * layar; dua salinan berarti satu perbaikan ejaan harus dikenakan dua kali, dan bila hanya satu
 * yang dikenai, kedua layar menyebut langkah yang sama dengan dua nama berbeda.
 *
 * Aturan **cadangan teks bacaan** ("pakai hasil rujukan bila ada, pakai medan entri sendiri bila
 * tidak") pernah tinggal di sini sebagai `teksBacaan` — tetapi aturan itu tidak khas sholat mayit:
 * layar Panduan Sholat, rincian langkah sholat, dan layar Tahlil memakainya juga, masing-masing
 * dengan salinan sendiri. Ia kini satu tempat di
 * [id.or.kemenag.yasintahlil.core.data.TeksBacaan], dan kelima layar itu memanggilnya.
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

    /**
     * Judul langkah takbir yang **sama persis** di kedua layar mayit.
     *
     * Takbir 2–4 menyebut bacaan yang sama dengan nama yang sama di Ghaib maupun Jenazah. Yang
     * benar-benar khas tiap layar hanya takbir 1 (ta'awudz di Ghaib) dan salam (arah toleh di
     * Ghaib); keduanya tetap tinggal di layarnya. Kuncinya adalah id bacaan dari
     * [BacaanSholat.TAKBIR_MAYIT], sehingga `judulLangkah.getValue(id)` tak pernah gagal selama
     * gabungan peta bersama dan entri khas menutupi seluruh urutan itu — penjaganya memaku
     * cakupan itu, bukan sekadar kehadiran tiap berkas.
     */
    val judulTakbirBersama: Map<String, String> = mapOf(
        BacaanSholat.SHOLAWAT_IBRAHIMIYAH to "Takbir 2 → Sholawat Ibrahimiyah",
        BacaanSholat.DOA_MAYIT to "Takbir 3 → Doa untuk mayit",
        BacaanSholat.DOA_MAYIT_TAMBAHAN to "Takbir 4 → Doa tambahan",
    )

    /**
     * Peta judul langkah takbir satu layar: entri bersama ([judulTakbirBersama]) digabung dengan
     * [khas] — judul yang hanya berlaku di layar itu.
     *
     * Entri [khas] yang menimpa entri bersama **ditolak**, bukan diizinkan menang diam-diam:
     * perbedaan yang disengaja harus disengaja, bukan akibat salinan yang tertinggal. Penjaga di
     * uji memaku kedua peta tetap lepas, jadi `require` ini tidak pernah menyala di jalur normal.
     */
    fun judulLangkah(khas: Map<String, String>): Map<String, String> {
        val tumpangTindih = khas.keys intersect judulTakbirBersama.keys
        require(tumpangTindih.isEmpty()) {
            "judul langkah khas menimpa entri bersama: ${tumpangTindih.sorted()}"
        }
        return judulTakbirBersama + khas
    }

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
}
