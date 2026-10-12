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
 *
 * **Nama rentang ditulis sekali.** Sebelumnya setiap nama hidup dua kali — sekali sebagai literal
 * pada cabang [waktu], sekali lagi pada [URUTAN_WAKTU]. Keduanya adalah daftar teks yang sama, jadi
 * tak ada yang menjaga keduanya tetap sejalan: mengubah nama di salah satu sisi sudah cukup membuat
 * kelompok itu **hilang dari urutan** tanpa satu pun pesan galat, karena `indexOf` yang gagal
 * mengembalikan −1 dan kelompok itu diam-diam terlempar ke urutan paling akhir. Kini [waktu]
 * mengembalikan konstanta di bawah, dan [URUTAN_WAKTU] menyusun dirinya dari konstanta yang sama.
 */
object KelompokSholat {

    /** Rentang waktu yang tidak terbaca — kehadirannya berarti aturan di bawah perlu ditambah. */
    const val LAIN = "Waktu lain"

    /** Setelah matahari terbit hingga sebelum Zuhur. */
    const val PAGI = "Pagi hingga sebelum Zuhur"

    /** Sepertiga malam terakhir, rawatib malam, dan tarawih. */
    const val MALAM = "Malam"

    /** Rawatib: sebelum/sesudah sholat wajib. */
    const val MENGIRINGI = "Mengiringi sholat wajib"

    /** Sholat sunnah yang tak terikat waktu tertentu. */
    const val KAPAN_SAJA = "Kapan saja selain waktu terlarang"

    /** Terikat peristiwa: masuk masjid, gerhana, dan sejenisnya. */
    const val PERISTIWA = "Peristiwa tertentu"

    /**
     * Urutan tampil rentang waktu, dari pagi ke malam lalu yang tak terikat waktu.
     *
     * Disusun dari konstanta di atas — bukan salinan teksnya — sehingga setiap nama yang keluar dari
     * [waktu] pasti punya tempat di urutan ini.
     */
    val URUTAN_WAKTU: List<String> = listOf(PAGI, MALAM, MENGIRINGI, KAPAN_SAJA, PERISTIWA, LAIN)

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
            w.contains("kapan saja") -> KAPAN_SAJA
            w.contains("sholat wajib") -> MENGIRINGI
            w.contains("malam") || w.contains("isya") || w.contains("subuh") -> MALAM
            w.contains("terbit") || w.contains("pagi") || w.contains("zuhur") -> PAGI
            w.contains("masjid") || w.contains("gerhana") -> PERISTIWA
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
