package id.or.kemenag.yasintahlil.core.data

/**
 * Bentuk dokumen konten yang dapat dirujuk: nama wadahnya dan kunci `id` entrinya.
 *
 * Berkas konten menyimpan entri yang dapat dirujuk dalam tiga bentuk — `bagian` (Tahlil dan susunan
 * tahlil), `bacaan` (sholat jenazah, langkah sholat, bacaan setelah adzan), dan
 * `kelompok[].items[]` (dzikir) — dan setiap entri dikenali oleh kuncinya, [ID].
 *
 * Bentuk itu dibaca **dua peran**: [ContentRepo], yang benar-benar membangun daftar bacaan dari
 * dokumennya, dan [entriRujukan], yang mengikuti rujukan `berkas.json#id` dari dokumen lain. Dahulu
 * keduanya menuliskan nama-nama wadah dan kunci `id` sendiri-sendiri, dan tak ada apa pun yang
 * membandingkannya. Salinan yang menyimpang gagal dengan cara yang menyesatkan ke arah mana pun ia
 * bergeser: bila **pembacanya** lebih dulu memakai wadah baru, pemeriksa bundel masih menelusuri
 * yang lama sehingga bundel yang sah ditolak dengan alasan "menunjuk entri yang tidak ada" — seolah
 * isinya yang salah, bukan kodenya; bila **pemeriksanya** yang lebih dulu, rujukan dinyatakan sah
 * padahal pembaca mencarinya di tempat lain, dan entri tampil **kosong tanpa galat** — persis yang
 * dilarang aturan "jangan karang teks agama": teks yang seharusnya dari sumber terverifikasi hilang
 * tanpa jejak.
 *
 * Karena itu bentuknya tinggal di sini sebagai data, dan [DAFTAR_ENTRI] dipakai bersama oleh
 * penelusuran rujukan sehingga wadah baru cukup ditambahkan sekali.
 */
object BentukKonten {

    /** Kunci `id` entri yang dapat dirujuk, pada setiap bentuk di [DAFTAR_ENTRI] maupun di [ITEMS]. */
    const val ID = "id"

    /** Larik entri ber-`id` tingkat dokumen pada Tahlil dan susunan tahlil. */
    const val BAGIAN = "bagian"

    /** Larik entri ber-`id` tingkat dokumen pada bacaan sholat dan bacaan setelah adzan. */
    const val BACAAN = "bacaan"

    /** Larik kelompok dzikir; entri ber-`id`-nya berada di dalam [ITEMS]. */
    const val KELOMPOK = "kelompok"

    /** Larik entri ber-`id` di dalam satu [KELOMPOK]. */
    const val ITEMS = "items"

    /**
     * Bentuk tingkat dokumen yang ditelusuri mencari [ID], berurutan menurut prioritas.
     *
     * Berupa data, bukan tiga baris `?:` di badan penelusurannya, supaya wadah baru cukup
     * ditambahkan di sini — dan supaya [BAGIAN]/[BACAAN] tidak dapat diam-diam terlepas dari
     * daftar yang benar-benar ditelusuri.
     */
    val DAFTAR_ENTRI = listOf(BAGIAN, BACAAN)
}
