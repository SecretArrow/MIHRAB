package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.KodeSumber
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Nilai awal state preferensi di layar harus **sama** dengan bawaannya.
 *
 * Setiap layar memegang pengaturan dalam `remember` dan mengisinya dari
 * `store.get(Store.K, BAKU)` di dalam `LaunchedEffect`. Ada dua nilai yang terlibat: [BAKU] — nilai
 * yang dipakai bila pengguna belum pernah mengubah apa pun — dan **nilai awal** `mutableStateOf(...)`
 * yang tampil selama pemuatan belum selesai. Keduanya menjawab pertanyaan yang sama ("apa isi
 * pengaturan ini bagi pengguna yang belum menyentuhnya?"), jadi keduanya harus hal yang sama.
 *
 * Yang terjadi bila berbeda bukan galat, melainkan **kedipan**: layar sempat menampilkan pengaturan
 * lain, lalu berubah. Karena sunyi, salinan itu tidak pernah ketahuan sampai seseorang mengubah
 * bawaannya dan menemukan satu layar yang tidak ikut berubah.
 *
 * Dua di antaranya sudah pernah menjadi kenyataan di repositori ini:
 *
 * - `AdzanScreen` menulis `PrayerCalc.Method.KEMENAG` sebagai nilai awal `metode`, sementara
 *   pemuatannya membaca `JadwalHariIni.METODE_DEFAULT` — tepat pola "selalu Kemenag/SIHAT" yang
 *   komentar di atasnya sendiri menyebut sudah diperbaiki. Layar itu masih sempat berbohong kepada
 *   pengguna yang memilih metode lain.
 * - `showArab` ditulis `true` di tujuh layar, padahal `SettingsScreen` memakai
 *   [BawaanPreferensi.TAMPIL_ARAB]. Mengubah konstanta itu akan menyembunyikan tulisan Arab di
 *   Pengaturan sementara tujuh layar lain masih sempat menampilkannya.
 *
 * Karena itu uji ini memaku aturannya: untuk setiap `var N by remember… { mutableStateOf(AWAL) }`
 * yang dimuat dari `store.get(Store.K, BAKU)`, [AWAL] harus berupa **BAKU itu sendiri** atau
 * **seluruh bentuk pemuatannya dengan `store.get(...)` diganti BAKU** — bentuk kedua diperlukan
 * karena pemuatan sering membungkusnya dengan konversi (`parseOn`, `preMenit`, `Method.from`), dan
 * nilai awal harus melalui konversi yang sama.
 *
 * Yang **tidak** diperiksa di sini: nilai awal state yang bukan berasal dari `store.get`, misalnya
 * state murni tampilan (`fullscreen`, `settings`) atau state yang dibaca lewat `Store.flow` di
 * ViewModel. Aturan ini hanya berlaku bagi pasangan "nilai awal ↔ nilai bawaan" yang ditulis di
 * berkas yang sama; di luar itu tidak ada sepasang pun untuk dibandingkan.
 */
class NilaiAwalPreferensiTest {

    private fun berkasMain(): List<File> =
        File(KodeSumber.root(), "src/main/java/id/or/kemenag/yasintahlil")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun rapat(teks: String): String = teks.filterNot { it.isWhitespace() }

    /** Indeks `)` yang menutup `(` pada [buka], atau `-1` bila tak seimbang. */
    private fun seimbang(teks: String, buka: Int): Int {
        var dalam = 0
        var i = buka
        while (i < teks.length) {
            when (teks[i]) {
                '(' -> dalam++
                ')' -> { dalam--; if (dalam == 0) return i }
                '"' -> {
                    i++
                    while (i < teks.length && teks[i] != '"') { if (teks[i] == '\\') i++; i++ }
                }
            }
            i++
        }
        return -1
    }

    /** Daftar argumen, dipotong pada koma tingkat atas saja. */
    private fun argumen(isi: String): List<String> {
        val hasil = mutableListOf<String>()
        var dalam = 0
        val kini = StringBuilder()
        isi.forEach { c ->
            when {
                c == '(' || c == '[' || c == '{' -> { dalam++; kini.append(c) }
                c == ')' || c == ']' || c == '}' -> { dalam--; kini.append(c) }
                c == ',' && dalam == 0 -> { hasil += kini.toString(); kini.clear() }
                else -> kini.append(c)
            }
        }
        hasil += kini.toString()
        return hasil
    }

    /** Akhir pernyataan mulai dari [mulai]: baris baru (atau penutup) pada kedalaman kurung nol. */
    private fun akhirPernyataan(teks: String, mulai: Int): Int {
        var dalam = 0
        var i = mulai
        while (i < teks.length) {
            when (teks[i]) {
                '(', '[', '{' -> dalam++
                ')', ']', '}' -> if (dalam == 0) return i else dalam--
                '\n' -> if (dalam == 0) return i
                '"' -> {
                    i++
                    while (i < teks.length && teks[i] != '"') { if (teks[i] == '\\') i++; i++ }
                }
            }
            i++
        }
        return teks.length
    }

    private val AWAL = Regex("""\bvar\s+(\w+)\s+by\s+remember(?:Saveable)?\s*\{\s*mutableStateOf\s*(?:<.*?>)?\s*\(""")

    /** Nilai awal tiap `var N by remember… { mutableStateOf(…) }`: nama → bentuk nilai awalnya. */
    private fun nilaiAwal(teks: String): Map<String, List<String>> {
        val hasil = mutableMapOf<String, MutableList<String>>()
        AWAL.findAll(teks).forEach { m ->
            val buka = m.range.last
            val tutup = seimbang(teks, buka)
            if (tutup > buka) {
                hasil.getOrPut(m.groupValues[1]) { mutableListOf() } += teks.substring(buka + 1, tutup)
            }
        }
        return hasil
    }

    private val TETAPAN = Regex("""\b(\w+)\s*=\s*""")

    /**
     * Tiap `N = …store.get(Store.K, BAKU)…`: nama, baku, dan bentuk pemuatan dengan `store.get(...)`
     * diganti BAKU.
     */
    private fun pemuatan(teks: String): List<Triple<String, String, String>> {
        val hasil = mutableListOf<Triple<String, String, String>>()
        for (m in TETAPAN.findAll(teks)) {
            val mulai = m.range.last + 1
            val rhs = teks.substring(mulai, akhirPernyataan(teks, mulai))
            val gi = rhs.indexOf("store.get(")
            if (gi < 0) continue
            val buka = gi + "store.get".length
            val tutup = seimbang(rhs, buka)
            if (tutup < 0) continue
            val args = argumen(rhs.substring(buka + 1, tutup))
            if (args.size < 2 || !args[0].trim().startsWith("Store.")) continue
            val baku = args[1].trim()
            hasil += Triple(
                m.groupValues[1], baku,
                rhs.substring(0, gi) + baku + rhs.substring(tutup + 1),
            )
        }
        return hasil
    }

    @Test
    fun nilaiAwalStatePreferensiSamaDenganNilaiBawaannya() {
        var diperiksa = 0
        val pelanggar = mutableListOf<String>()
        berkasMain().forEach { f ->
            val teks = KodeSumber.kodeTanpaKomentar(f.readText())
            val awal = nilaiAwal(teks)
            pemuatan(teks).groupBy { it.first }.forEach { (nama, daftar) ->
                val nilai = awal[nama] ?: return@forEach
                diperiksa++
                val sah = daftar.flatMap { listOf(it.second, it.third) }.map(::rapat).toSet()
                val beda = nilai.map(::rapat).filter { it !in sah }.distinct()
                if (beda.isNotEmpty()) {
                    pelanggar += "${f.name}: $nama — nilai awal ${beda} bukan bawaannya " +
                        "${sah.toList()}"
                }
            }
        }
        // Pola yang berhenti cocok akan membuat uji ini hijau tanpa memeriksa apa pun.
        assertTrue("penjaga tidak lagi menemukan state preferensi mana pun — polanya salah",
            diperiksa >= 40)
        assertTrue(
            "nilai awal state preferensi berbeda dari bawaannya, sehingga layar sempat " +
                "menampilkan pengaturan lain sebelum pemuatan selesai: $pelanggar",
            pelanggar.isEmpty(),
        )
    }
}
