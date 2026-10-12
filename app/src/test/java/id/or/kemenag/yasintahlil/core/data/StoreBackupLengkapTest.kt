package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Setiap kunci preferensi ([Store]) harus ikut **dipulihkan** dari backup.
 *
 * `exportJson` menulis seluruh isi DataStore apa adanya (`d.asMap()`), jadi ia lengkap dengan
 * sendirinya. `importJson` tidak: ia menyebut satu per satu kunci yang dipulihkan, dikelompokkan
 * menurut tipe. Dua daftar itu — deklarasi kunci di `Store` dan daftar di `importJson` — ditulis
 * terpisah, dan **tak ada apa pun yang membandingkannya**.
 *
 * Bedanya sunyi, dan datang tepat pada saat paling buruk. Pengguna mencadangkan pengaturannya,
 * memasang ulang aplikasi, lalu memulihkan backup — dan pemulihannya **tampak berhasil**: berkasnya
 * terbaca, tak ada galat, layar menampilkan pesan sukses. Hanya satu pengaturan yang diam-diam
 * kembali ke bawaan, dan tak ada cara mengetahuinya kecuali mencoba satu per satu.
 *
 * Kerusakan ini sudah beberapa kali terjadi di repositori ini: pilihan tema halaman, tampilan
 * kompas, susunan tahlil, jenis suara adzan, dan pengingat iqomah/doa adzan masing-masing pernah
 * tertinggal dari `importJson`, dan tiap perbaikannya menambahkan pemeriksaannya sendiri di berkas
 * uji fitur itu. Yang belum pernah ada adalah pemeriksaan yang berlaku untuk **semua** kunci
 * sekaligus — sehingga kunci berikutnya akan tertinggal lagi dengan cara yang sama.
 *
 * Uji ini menutup kelasnya: daftar kunci diambil dari deklarasi `Store`, daftar pemulihan diambil
 * dari badan `importJson`, dan keduanya harus memuat nama yang **sama persis** — nama yang hilang
 * maupun nama yang tidak pernah dideklarasikan sama-sama gagal.
 *
 * Sengaja membaca berkas sumber, bukan menjalankan `Store`: DataStore butuh Android, sedangkan
 * invariannya murni soal dua daftar yang harus sepadan.
 */
class StoreBackupLengkapTest {

    private fun root(): File =
        listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    /** `Store.kt` tanpa komentar — KDoc-nya menyebut pola lama sebagai penjelasan, bukan pelanggaran. */
    private val store: String
        get() = KodeSumber.kodeTanpaKomentar(
            File(root(), "src/main/java/id/or/kemenag/yasintahlil/core/data/Store.kt").readText())

    /**
     * Pemulih `importJson` untuk tiap tipe kunci DataStore, sejajar satu-satu.
     *
     * `stringSet` sengaja tidak ada di sini: nilainya bukan skalar, jadi ia dipulihkan lewat cabang
     * tersendiri yang membaca larik JSON (`p[FAV] = set`).
     */
    private val pemulih = mapOf(
        "string" to "str",
        "boolean" to "bool",
        "float" to "flt",
        "double" to "dbl",
        "int" to "int",
    )

    /** Seluruh kunci yang dideklarasikan `Store`: nama → tipe DataStore-nya. */
    private fun kunciTerdeklarasi(): Map<String, String> =
        Regex("""\bval\s+([A-Z][A-Z0-9_]*)\s*=\s*(\w+?)PreferencesKey\(""")
            .findAll(store)
            .associate { it.groupValues[1] to it.groupValues[2] }

    /**
     * Badan `importJson` — satu-satunya tempat kunci dituliskan ulang untuk dipulihkan.
     *
     * `importJson` adalah anggota terakhir `Store`, jadi badannya berakhir pada `}` ber-indentasi
     * empat spasi yang menutup fungsinya. Penanda fungsinya diperiksa lebih dahulu: `substringAfter`
     * mengembalikan seluruh teks bila penandanya tidak ada, sehingga fungsi yang berganti nama akan
     * membuat uji ini membaca seluruh berkas dan **lolos tanpa memeriksa apa pun**.
     */
    private fun badanImpor(): String {
        assertTrue("Store.kt tidak lagi memuat 'fun importJson'", store.contains("fun importJson"))
        val impor = store.substringAfter("fun importJson").substringBefore("\n    }\n")
        assertTrue("badan importJson tidak terbaca", impor.contains("o.optJSONArray"))
        return impor
    }

    @Test
    fun setiapKunciPreferensiIkutDipulihkanDariBackup() {
        val kunci = kunciTerdeklarasi()
        // Pola yang berhenti cocok akan membuat uji ini hijau tanpa memeriksa apa pun.
        assertTrue("tidak ada kunci Store yang terbaca — pola pengambilnya salah", kunci.size >= 20)

        val impor = badanImpor()
        assertTrue("badan importJson mencakup seluruh berkas — penandanya tidak terbaca dengan benar",
            impor.length < store.length)

        val lewatPemulih = Regex("""\b(str|bool|flt|dbl|int)\(\s*([A-Z][A-Z0-9_]*)\s*\)""")
            .findAll(impor).associate { it.groupValues[2] to it.groupValues[1] }
        val lewatCabang = Regex("""\bp\[([A-Z][A-Z0-9_]*)\]\s*=""")
            .findAll(impor).map { it.groupValues[1] }.toSet()

        val dipulihkan = lewatPemulih.keys + lewatCabang
        assertTrue("importJson tidak memulihkan kunci apa pun", dipulihkan.size >= 20)

        val hilang = kunci.keys - dipulihkan
        assertTrue(
            "kunci preferensi tidak ikut dipulihkan dari backup — pengaturan itu diam-diam " +
                "kembali ke bawaan saat backup dipulihkan: $hilang",
            hilang.isEmpty(),
        )

        val asing = dipulihkan - kunci.keys
        assertTrue(
            "importJson memulihkan nama yang tidak dideklarasikan Store (salah ketik atau kunci " +
                "yang sudah dihapus, sehingga pemulihannya tak berefek): $asing",
            asing.isEmpty(),
        )

        // Tipe pemulih tidak diperiksa di sini: tipe kunci dan tipe pemulihnya dijaga compiler, jadi
        // pemeriksaan teks atas keduanya tak akan pernah dapat gagal. Yang benar-benar dapat luput
        // adalah **tipe kunci yang belum dikenal penjaga ini** — tipe baru berarti pemulih baru, dan
        // pemulih yang tak dikenal membuat pembandingan nama di atas tidak lagi lengkap.
        val tipeBaru = kunci.values.toSet() - pemulih.keys - setOf("stringSet")
        assertTrue("tipe kunci baru belum punya pemulih yang dikenal penjaga ini: $tipeBaru",
            tipeBaru.isEmpty())

        // Kunci non-skalar punya tipe yang tidak punya pemulih — pastikan ia lewat cabangnya sendiri,
        // bukan sekadar tidak muncul di mana pun.
        val nonSkalar = kunci.filterValues { pemulih[it] == null }.keys
        assertTrue("kunci non-skalar tidak dipulihkan lewat cabangnya sendiri: ${nonSkalar - lewatCabang}",
            nonSkalar.all { it in lewatCabang })

        assertEquals("jumlah kunci yang dipulihkan berbeda dari yang dideklarasikan",
            kunci.size, dipulihkan.size)
    }
}
