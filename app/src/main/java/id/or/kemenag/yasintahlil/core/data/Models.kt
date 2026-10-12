package id.or.kemenag.yasintahlil.core.data

data class Verse(val nomor: Int, val arab: String, val latin: String, val terjemahan: String)
data class Surah(
    val nama: String, val ayatCount: Int, val verses: List<Verse>, val sumber: String,
    /** Kategori sumber isi surat (mis. [SumberKategori.QURAN]) — lihat `assets/content`. */
    val kategori: String = "",
)

data class TahlilItem(
    val id: String, val judul: String,
    val arab: String = "", val latin: String = "", val terjemahan: String = "",
    val ulangi: Int = 1, val rujukanFile: String? = null, val sumber: String = "",
    /** Kategori sumber bacaan ([SumberKategori]); kosong = tidak diklaim. */
    val kategori: String = ""
)

/** Susunan rangkaian tahlil (urut) — merujuk id pada daftar bagian tahlil. */
data class TahlilSusunan(val id: String, val nama: String, val bagian: List<String>)

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
    val dalil: String = "",
    val catatanMazhab: String = "",
    val syarat: List<String> = emptyList(),
    val niat: List<Bacaan> = emptyList(),
    val langkah: List<String> = emptyList(),
)

data class PrayerTimes(
    val subuh: String, val terbit: String, val zuhur: String,
    val asar: String, val magrib: String, val isya: String
)
