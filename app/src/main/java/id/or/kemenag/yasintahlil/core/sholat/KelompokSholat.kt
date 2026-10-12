package id.or.kemenag.yasintahlil.core.sholat

import id.or.kemenag.yasintahlil.core.data.SholatInfo

/** Cara mengelompokkan daftar sholat sunnah. */
enum class CaraKelompok(val label: String) {
    WAKTU("Waktu"),
    RAKAAT("Rakaat"),
    TUJUAN("Tujuan"),
}

/**
 * Pengelompokan sholat sunnah — `plan.txt` §"Sholat Sunnah": *"Kelompokkan sholat berdasarkan waktu
 * pelaksanaan, jumlah rakaat, dan tujuan."*
 *
 * Pengelompokan **tidak mengarang keterangan baru**. Untuk rakaat dan tujuan, kuncinya adalah nilai
 * yang sudah tertulis di entri itu sendiri. Untuk waktu, nilai mentahnya terlalu khusus
 * ("Sepertiga malam terakhir", "Saat masuk masjid") sehingga satu kelompok hanya berisi satu
 * entri — tidak berguna sebagai kelompok — jadi [waktu] memetakannya ke rentang yang lebih kasar
 * yang **dibaca dari kata-kata pada `waktu` itu sendiri**, bukan dari penilaian penulis kode.
 *
 * Setiap entri harus jatuh ke salah satu rentang yang bernama; [LAIN] menandai entri yang tidak
 * terbaca dan ujinya menolak kehadirannya, sehingga entri baru tak bisa diam-diam tak terkelompok.
 */
object KelompokSholat {

    /** Rentang waktu yang tidak terbaca — kehadirannya berarti aturan di bawah perlu ditambah. */
    const val LAIN = "Waktu lain"

    /** Urutan tampil rentang waktu, dari pagi ke malam lalu yang tak terikat waktu. */
    private val URUTAN_WAKTU = listOf(
        "Pagi hingga sebelum Zuhur",
        "Malam",
        "Mengiringi sholat wajib",
        "Kapan saja selain waktu terlarang",
        "Peristiwa tertentu",
        LAIN,
    )

    /**
     * Rentang waktu dari teks `waktu` entri.
     *
     * Aturannya diperiksa berurutan dan hanya melihat kata yang memang ada di teks itu: "Kapan
     * saja" lebih dulu karena teks itu juga menyebut waktu terlarang, dan "sholat wajib" sebelum
     * "Malam"/"Pagi" karena rawatib bisa jatuh pada keduanya.
     */
    fun waktu(waktu: String): String {
        val w = waktu.lowercase()
        return when {
            w.contains("kapan saja") -> "Kapan saja selain waktu terlarang"
            w.contains("sholat wajib") -> "Mengiringi sholat wajib"
            w.contains("malam") || w.contains("isya") || w.contains("subuh") -> "Malam"
            w.contains("terbit") || w.contains("pagi") || w.contains("zuhur") ->
                "Pagi hingga sebelum Zuhur"
            w.contains("masjid") || w.contains("gerhana") -> "Peristiwa tertentu"
            else -> LAIN
        }
    }

    /**
     * Kelompokkan [entri] menurut [cara]. Urutan kelompok untuk [CaraKelompok.WAKTU] mengikuti
     * [URUTAN_WAKTU] (pagi → malam → tak terikat waktu); untuk cara lain diurutkan menurut nama
     * kelompok, karena nilai mentahnya tidak punya urutan alami.
     */
    fun kelompok(entri: List<SholatInfo>, cara: CaraKelompok): List<Pair<String, List<SholatInfo>>> {
        val hasil = entri.groupBy { s ->
            when (cara) {
                CaraKelompok.WAKTU -> waktu(s.waktu)
                CaraKelompok.RAKAAT -> s.rakaat
                CaraKelompok.TUJUAN -> s.tujuan.ifBlank { "Tujuan belum dicatat" }
            }
        }
        val urut = when (cara) {
            CaraKelompok.WAKTU -> hasil.keys.sortedBy { k ->
                URUTAN_WAKTU.indexOf(k).let { if (it < 0) URUTAN_WAKTU.size else it }
            }
            else -> hasil.keys.sorted()
        }
        return urut.map { k -> k to hasil.getValue(k) }
    }
}
