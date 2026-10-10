package id.or.kemenag.yasintahlil.core.data

data class Verse(val nomor: Int, val arab: String, val latin: String, val terjemahan: String)
data class Surah(val nama: String, val ayatCount: Int, val verses: List<Verse>, val sumber: String)

data class TahlilItem(
    val id: String, val judul: String,
    val arab: String = "", val latin: String = "", val terjemahan: String = "",
    val ulangi: Int = 1, val rujukanFile: String? = null, val sumber: String = ""
)

data class Niat(val arab: String, val latin: String, val terjemahan: String)
data class SholatInfo(
    val id: String, val nama: String, val rakaat: String, val waktu: String,
    val deskripsi: String, val niat: Niat? = null, val rujukanFile: String? = null
)
data class Bacaan(
    val id: String, val judul: String, val kapan: String = "",
    val arab: String = "", val latin: String = "", val terjemahan: String = "",
    val sumber: String = "", val rujukanFile: String? = null
)

data class DzikirItem(
    val judul: String, val arab: String = "", val latin: String = "",
    val terjemahan: String = "", val ulangi: Int = 1,
    val sumber: String = "", val rujukanFile: String? = null,
    val rujukan: List<String> = emptyList()
)
data class DzikirGroup(val id: String, val judul: String, val items: List<DzikirItem>)

data class AdzanLafaz(val arab: String, val latin: String, val terjemahan: String = "", val ulangi: Int = 1)

data class Kota(val nama: String, val lat: Double, val lon: Double)

data class PrayerTimes(
    val subuh: String, val terbit: String, val zuhur: String,
    val asar: String, val magrib: String, val isya: String
)
