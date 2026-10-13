package id.or.kemenag.yasintahlil.core.data

data class Verse(val nomor: Int, val arab: String, val latin: String, val terjemahan: String)
data class Surah(
    val nama: String, val ayatCount: Int, val verses: List<Verse>, val sumber: String,
    /** Kategori sumber isi surat (mis. [SumberKategori.QURAN]) — lihat `assets/content`. */
    val kategori: String = "",
    /**
     * Asal teks Arab dan transliterasi Latin — terpisah dari [sumber] yang menyebut terjemahan.
     *
     * Berkas Qur'an di `assets/content` mencatat ketiganya sendiri-sendiri (`sumber_arab`,
     * `sumber_latin`, `sumber_terjemahan`): teks Arab dari Mushaf Standar Indonesia (MSI) LPMQ
     * Kemenag, transliterasi Latin resmi Kemenag, terjemahan edisi 2019. Hanya [sumber] yang
     * dahulu dibaca kode, dan itu pun tidak pernah ditampilkan — jadi pengguna tidak dapat
     * memeriksa dari mana teks Arab dan transliterasi yang ia baca berasal.
     */
    val sumberArab: String = "",
    val sumberLatin: String = "",
)

data class TahlilItem(
    val id: String, val judul: String,
    val arab: String = "", val latin: String = "", val terjemahan: String = "",
    val ulangi: Int = 1, val rujukanFile: String? = null, val sumber: String = "",
    /** Kategori sumber bacaan ([SumberKategori]); kosong = tidak diklaim. */
    val kategori: String = ""
)

/**
 * Susunan rangkaian tahlil (urut) — merujuk id pada daftar bagian tahlil.
 *
 * [sumber] menyebut asal susunan ini (`plan.txt` §"Fitur Tahlil Lengkap": *"Sediakan beberapa
 * susunan Tahlil berdasarkan sumber atau tradisi yang jelas"*). Susunan buatan pengguna
 * ("Susunan saya") tidak mengklaim sumber apa pun, jadi nilainya kosong.
 */
data class TahlilSusunan(
    val id: String, val nama: String, val bagian: List<String>, val sumber: String = "",
)

data class Niat(
    val arab: String, val latin: String, val terjemahan: String,
    /** Kategori sumber lafaz niat ([SumberKategori]) — lafaz niat Indonesia umumnya `tradisi`. */
    val kategori: String = "",
)
data class SholatInfo(
    val id: String, val nama: String, val rakaat: String, val waktu: String,
    val deskripsi: String, val niat: Niat? = null, val rujukanFile: String? = null,
    /** Ketentuan khusus (mis. Jumat, qashar/jamak safar) — prosedural, tanpa teks Arab. */
    val ketentuan: List<String> = emptyList(),
    /** Catatan bila niat sengaja belum disertakan (menunggu verifikasi sumber resmi). */
    val niatCatatan: String = "",
    /**
     * Maksud sholat sunnah (mis. "Ampunan dosa", "Penghormatan masjid").
     *
     * `plan.txt` §"Sholat Sunnah": *"Kelompokkan sholat berdasarkan waktu pelaksanaan, jumlah
     * rakaat, dan tujuan."* Sebelumnya kolom ini ada di bundel tetapi tak pernah dibaca kode —
     * data yang ditulis, divalidasi gerbang konten, lalu tak sampai ke pengguna sama sekali.
     */
    val tujuan: String = "",
)
data class Bacaan(
    val id: String, val judul: String, val kapan: String = "",
    val arab: String = "", val latin: String = "", val terjemahan: String = "",
    val sumber: String = "", val rujukanFile: String? = null, val catatan: String = "",
    /** Bila terisi (mis. "syafii"), bacaan hanya tampil untuk mazhab itu. null = semua mazhab. */
    val mazhab: String? = null,
    /** Bila terisi (mis. ["subuh"]), bacaan hanya tampil untuk sholat itu. kosong = semua sholat. */
    val sholatScope: List<String> = emptyList(),
    /** Kategori sumber bacaan ([SumberKategori]); kosong = tidak diklaim. */
    val kategori: String = ""
)

data class DzikirItem(
    val judul: String, val arab: String = "", val latin: String = "",
    val terjemahan: String = "", val ulangi: Int = 1,
    val sumber: String = "", val rujukanFile: String? = null,
    val rujukan: List<String> = emptyList(),
    /** Kategori sumber bacaan ([SumberKategori]); kosong = tidak diklaim. */
    val kategori: String = ""
)
data class DzikirGroup(val id: String, val judul: String, val items: List<DzikirItem>)

data class AdzanLafaz(
    val arab: String, val latin: String, val terjemahan: String = "", val ulangi: Int = 1,
    /**
     * Rujukan lafaz ini (mis. *"HR Bukhari-Muslim — sunnah.com"*).
     *
     * `adzan.json` sudah mencantumkan `sumber` pada tingkat bagian (`adzan`/`iqomah`) maupun
     * per-entri, tetapi nilainya dahulu tidak pernah dibaca kode — jadi layar Adzan dan Iqomah
     * menampilkan lafaz telanjang tanpa asal-usulnya. Kolom ini meneruskannya ke [BacaanCard],
     * yang sudah punya baris "Sumber: …". Kosong = tidak diklaim.
     */
    val sumber: String = "",
    /** Kategori sumber lafaz ([SumberKategori]); kosong = tidak diklaim. */
    val kategori: String = "",
)

/** Doa/lafaz seputar adzan yang tampil di layar Doa Adzan (mis. menjawab adzan, sholawat). */
data class AdzanDoa(
    val judul: String,
    val arab: String,
    val latin: String,
    val terjemahan: String = "",
    /** Keterangan tambahan yang menyertai bacaan (mis. pengecualian dua seruan hayya). */
    val catatan: String = "",
    /** Kapan bacaan ini dibaca — `plan.txt` §"Bacaan Setelah Adzan": *"Penjelasan waktu membacanya."* */
    val waktu: String = "",
    val sumber: String = "",
    /** Kategori sumber bacaan ([SumberKategori]); kosong = tidak diklaim. */
    val kategori: String = "",
)

data class Kota(val nama: String, val lat: Double, val lon: Double)

/** Satu langkah panduan (wudhu/tayamum) — keterangan prosedural, bukan teks ibadah. */
data class Langkah(val no: Int, val judul: String, val keterangan: String = "")

/** Panduan bersuci: wudhu, tayamum, dan hal-hal yang membatalkan wudhu. */
data class PanduanBersuci(
    val rukun: List<String> = emptyList(),
    val wudhu: List<Langkah> = emptyList(),
    val tayamum: List<Langkah> = emptyList(),
    val pembatal: List<String> = emptyList(),
    val catatanMazhab: String = "",
    val catatanLafaz: String = "",
    /**
     * Judul bagian dari bundel (`wudhu.json` → `wudhu.judul`) — heading yang berdiri di atas
     * langkah-langkahnya.
     *
     * Layar Wudhu dahulu mengetik kedua judulnya sendiri (*"Tata cara wudhu"* dan *"Tata cara
     * tayamum"*), padahal berkasnya mencantumkan keduanya. Salinan di layar bukan sekadar ringkas:
     * judul tayamum di berkas berbunyi *"Tata cara tayamum (pengganti wudhu saat tidak ada air /
     * sakit)"*, sehingga pengguna tidak pernah tahu bahwa bagian itu memang pengganti wudhu ketika
     * air tidak ada — keterangan yang sumbernya sendiri tulis. Judul bagian karena itu dibaca dari
     * berkasnya, seperti [edisi].
     */
    val judulWudhu: String = "",
    /** Judul bagian tayamum (`wudhu.json` → `tayamum.judul`). Lihat [judulWudhu]. */
    val judulTayamum: String = "",
    /**
     * Provenans tingkat modul (`wudhu.json` → `edisi`) — kerangka mana yang diikuti.
     *
     * Layar Wudhu dahulu mencetak parafrasenya sendiri (*"Kerangka tata cara mengikuti program
     * Fasolatan Ditjen Bimas Islam Kemenag; urutan rukun mazhab Syafi'i (umum di Indonesia)"*),
     * padahal berkasnya memuat kalimat yang lebih lengkap — termasuk bahwa kerangkanya juga
     * mencakup **tayamum** dan datang dari *Ngaji Fasolatan*. Salinan yang lebih pendek itu bukan
     * sekadar ringkas: ia menghilangkan asal-usul yang sumbernya sendiri cantumkan.
     */
    val edisi: String = "",
)

/** Batas aurat menurut mazhab yang dipilih panduan. */
data class Aurat(
    val judul: String = "",
    val lakiLaki: String = "",
    val perempuan: String = "",
    val catatan: String = "",
)

/**
 * Syarat dan persiapan sholat — kerangka berbahasa Indonesia tanpa teks Arab.
 *
 * `plan.txt` §"Tata Cara Sholat Langkah demi Langkah" menempatkan syarat wajib/sah, persiapan,
 * wudhu, penutup aurat, dan penentuan kiblat **di dalam** panduan langkah demi langkah. Model ini
 * membawa bagian yang berbentuk daftar; wudhu dan kiblat punya layarnya sendiri dan ditautkan dari
 * layar detail.
 */
data class SholatSyarat(
    val syaratWajib: List<String> = emptyList(),
    val syaratSah: List<String> = emptyList(),
    val persiapan: List<String> = emptyList(),
    val penutupAurat: Aurat = Aurat(),
    val kebersihanTempat: List<String> = emptyList(),
    val catatanMazhab: String = "",
    val catatanVerifikasi: String = "",
) {
    /** Jumlah butir yang ditampilkan, untuk label ringkasan pada tombol pembuka. */
    val jumlah: Int
        get() = syaratWajib.size + syaratSah.size + persiapan.size + kebersihanTempat.size +
            listOf(penutupAurat.lakiLaki, penutupAurat.perempuan).count { it.isNotBlank() }
}

/** Konten sholat khusus (ghaib/jenazah): metadata + varian niat, dibaca dari assets. */
data class SholatKhusus(
    val judul: String = "",
    val hukum: String = "",
    val takbir: Int = 4,
    /**
     * `tanpa_ruku_sujud` — sholat ini dikerjakan berdiri dengan takbir saja, tanpa ruku dan sujud.
     *
     * Dibaca dari berkasnya supaya judul layar tidak lagi **mengetik** keterangan itu sendiri:
     * dahulu `TampilanSholatKhusus.judul` selalu menambahkan ", tanpa ruku/sujud" apa pun isi
     * bundelnya, sehingga modul sholat khusus yang dengan ruku/sujud akan tetap dijuduli begitu.
     */
    val tanpaRukuSujud: Boolean = false,
    val dalil: String = "",
    val catatanMazhab: String = "",
    val syarat: List<String> = emptyList(),
    val niat: List<Bacaan> = emptyList(),
    /**
     * Urutan langkah menurut berkas konten (`langkah`), mis. `["niat", "takbir1_fatihah", …]`.
     *
     * Bukan teks untuk ditampilkan: urutan langkah yang benar-benar dibaca pengguna datang dari
     * [id.or.kemenag.yasintahlil.core.sholat.BacaanSholat.TAKBIR_MAYIT], yang memakai kosakata id
     * bacaan — sedangkan daftar ini memakai nama langkah tingkat konten. Kegunaannya adalah
     * menyatakan urutan itu **di bundel** sehingga penjaganya dapat membandingkan keduanya: satu
     * takbir yang hilang dari salah satunya akan terbaca sebagai perbedaan panjang, bukan sebagai
     * langkah yang diam-diam tak pernah tampil.
     */
    val langkah: List<String> = emptyList(),
)

data class PrayerTimes(
    val subuh: String, val terbit: String, val zuhur: String,
    val asar: String, val magrib: String, val isya: String
)
