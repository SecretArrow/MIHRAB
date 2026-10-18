package id.or.kemenag.yasintahlil.core.util

/**
 * **Satu lafaz, satu bacaan** — lafaz Arab yang sama tidak boleh dibaca (Latin) atau diterjemahkan
 * berbeda di dua tempat di dalam bundel konten.
 *
 * Bundel konten sengaja tidak menyatukan lafaz yang dipakai berulang: tiap modul berdiri sendiri
 * supaya pengguna dapat memasang pembaruan salah satunya saja (lihat [BundelKonten]). Harga dari
 * kemandirian itu adalah **salinan** — dan salinan yang tak dibandingkan pasti menyimpang.
 *
 * Sudah terjadi. Doa untuk mayit
 * ([sholat_mayit.json] `bacaan[0]`, id `doa_mayit`) dan bagian Tahlil yang memuat lafaz yang sama
 * ([tahlil_full.json] `bagian[13]`, id `doa_mayit`) ditulis dari sumber yang sama — `HR Muslim —
 * sunnah.com`, lafaz Arabnya bahkan identik sampai harakatnya — tetapi terjemahan Indonesianya
 * **sudah berbeda**: satu berbunyi *"…, rahmatilah dia, sejahterakanlah dia …"* dan yang lain
 * *"…, berilah dia rahmat, sejahterakanlah dia …"*. Pengguna membaca doa yang sama di dua layar dan
 * mendapat dua kalimat yang tidak persis sama, tanpa satu pun pesan galat dan tanpa cara mengetahui
 * mana yang disengaja.
 *
 * Lebih lebar lagi: `adzan.json` menulis **enam lafaz adzan dua kali** — sekali di `adzan.daftar`
 * dan sekali lagi di `iqomah.daftar` — hanya berbeda di jumlah ulangan. Keduanya tampil di layar
 * Adzan. Sampai hari ini keduanya sejalan, tetapi tak ada apa pun yang menjaganya: satu perbaikan
 * terjemahan di daftar adzan akan meninggalkan daftar iqomah memakai kata lama, dan bacaan yang
 * sama akan terbaca berbeda di dua tab yang bersebelahan.
 *
 * Karena itu aturannya tinggal di sini, dan **penjaganya satu tempat**:
 * `LafazSamaSatuTerjemahanTest` membaca seluruh berkas `assets/content`, menyusun [Pemakaian] tiap
 * lafaz, lalu menuntut [penyimpangan] kosong. Aturannya sengaja **tidak** disalin ke gate Python
 * (`tools/validate_content.py`) yang memeriksa berkas yang sama: gate itu membaca *konstanta* dari
 * Kotlin, bukan logika, sehingga menaruh pemeriksaan ini di sana berarti menulis ulang logikanya —
 * persis kekeliruan yang sedang ditutup.
 *
 * Murni tanpa Android, sehingga dapat diuji langsung di JVM.
 */
object LafazSama {

    /**
     * Satu pemakaian sebuah lafaz: di mana ia berada, lafaz Arabnya, dan bagaimana ia dibaca lalu
     * diterjemahkan. [tempat] hanya untuk pesan galat — penjaga mengisinya dengan nama berkas dan
     * jalur entrinya (mis. `adzan.json.iqomah.daftar[3]`).
     */
    data class Pemakaian(
        val tempat: String,
        val arab: String,
        val latin: String,
        val terjemahan: String,
    )

    /**
     * Pemakaian yang dikelompokkan per lafaz Arab, tanpa spasi tepi. Lafaz kosong dibuang: ia bukan
     * lafaz, dan mengelompokkannya hanya akan menyatukan entri yang tak berhubungan.
     */
    fun kelompok(pemakaian: List<Pemakaian>): Map<String, List<Pemakaian>> =
        pemakaian.filter { it.arab.isNotBlank() }.groupBy { it.arab.trim() }

    /**
     * Penyimpangan antar-salinan: lafaz yang sama, tetapi [Pemakaian.latin] atau
     * [Pemakaian.terjemahan]-nya berbeda. Kosong berarti seluruh salinan sejalan.
     *
     * Yang dibandingkan hanya **bacaan lafaznya** — Latin dan terjemahan — bukan seluruh entri.
     * `judul`, `sumber`, `ulangi`, dan `kategori` memang boleh berbeda antar-modul: adzan mengulang
     * takbir empat kali dan iqomah dua, dan modul mayit menyebut "takbir ke-3" pada judulnya. Yang
     * tidak boleh berbeda adalah bagaimana lafaz yang sama dibaca dan dimengerti.
     */
    fun penyimpangan(pemakaian: List<Pemakaian>): List<String> {
        val hasil = mutableListOf<String>()
        kelompok(pemakaian).forEach { (arab, salinan) ->
            if (salinan.size < 2) return@forEach
            val tempat = salinan.joinToString(", ") { it.tempat }
            val latin = salinan.map { it.latin.trim() }.distinct()
            val terjemahan = salinan.map { it.terjemahan.trim() }.distinct()
            if (latin.size > 1) {
                hasil += "lafaz \"$arab\" dibaca berbeda di $tempat: ${latin.joinToString(" | ")}"
            }
            if (terjemahan.size > 1) {
                hasil += "lafaz \"$arab\" diterjemahkan berbeda di $tempat: " +
                    terjemahan.joinToString(" | ")
            }
        }
        return hasil.sorted()
    }
}
