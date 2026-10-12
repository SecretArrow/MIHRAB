package id.or.kemenag.yasintahlil.core.data

import id.or.kemenag.yasintahlil.core.util.ContentValidator
import id.or.kemenag.yasintahlil.core.util.TeksArab
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bundel pembaruan konten — memenuhi `plan.txt`: *"Sediakan mekanisme pembaruan konten dengan
 * validasi sumber."*
 *
 * Bundel adalah satu berkas JSON yang memuat metadata dan peta nama-berkas → dokumen konten:
 *
 * ```json
 * {
 *   "_bundel": { "skema": 1, "dibuat": "2026-02-19", "penerbit": "LPMQ Kemenag",
 *                "catatan": "Pembaruan teks Qur'an edisi 2026" },
 *   "berkas": { "yasin_full.json": { "sumber_url": "https://quran.kemenag.go.id/…", "verses": [ … ] } }
 * }
 * ```
 *
 * Bundel **tidak pernah diunduh otomatis**: pengguna memilih berkasnya sendiri (lihat
 * `ContentUpdateManager`), sehingga tidak ada konten yang masuk tanpa tindakan sadar pengguna dan
 * tanpa izin internet. Sebelum diterima, seluruh isinya melewati aturan yang sama dengan gate CI
 * `tools/validate_content.py`:
 *
 * - setiap berkas harus punya `sumber_url` yang domainnya resmi (whitelist di [ContentValidator],
 *   dengan pencocokan host persis/subdomain agar tidak bisa dipalsukan);
 * - berkas ber-Arab wajib menyatakan `kategori` sumber ([SumberKategori]) yang dikenal;
 * - teks Arab wajib berharakat — teks mentah yang belum divokalkan ditolak;
 * - tidak boleh ada prosa penunjuk (placeholder) di field `arab`/`latin`;
 * - nama berkas tidak boleh menunjuk ke luar direktori konten;
 * - setiap rujukan berkas (`rujukan_file`, `rujukanFile`, larik `rujukan`) harus menunjuk berkas
 *   yang benar-benar ada — lihat [periksaRujukan].
 *
 * Sebuah bundel diterima **utuh atau tidak sama sekali**: satu berkas yang gagal membatalkan
 * seluruh pemasangan, sehingga konten yang sudah diverifikasi tidak pernah tercampur setengah
 * pembaruan.
 */
object BundelKonten {

    /**
     * Versi skema bundel yang dipahami aplikasi ini. Gate CI (`tools/validate_bundle.py`)
     * **membacanya dari sini** alih-alih menyimpan salinannya — lihat catatan di
     * [PANJANG_NAMA_MIN].
     */
    const val SKEMA = 1

    /** Kunci metadata tingkat bundel. */
    const val KUNCI_META = "_bundel"

    /** Kunci peta nama-berkas → dokumen konten. */
    const val KUNCI_BERKAS = "berkas"

    /**
     * Prosa penunjuk yang tidak boleh muncul di field `arab`/`latin`.
     *
     * Daftarnya tinggal **di sini saja**. Gate CI (`tools/validate_content.py`) membacanya dari
     * berkas ini, persis seperti [ContentValidator.allowedHosts] — karena keduanya menilai bundel
     * yang sama dan tidak boleh punya dua kebijakan. Bila salinannya berbeda, bundel yang lolos CI
     * dapat ditolak aplikasi saat dipasang (pembaruan tampak berhasil tetapi tidak mengubah apa pun),
     * atau sebaliknya: prosa penunjuk seperti *"lihat rujukan file"* lolos pemasangan dan
     * **ditampilkan kepada pengguna sebagai bacaan** — tepat yang dilarang aturan "jangan karang
     * teks agama". Dijaga `AturanBundelSatuTempatTest`.
     */
    val PLACEHOLDER = listOf(
        "lihat rujukan file", "lihat rujukan berkas", "dibaca penuh dari mushaf",
        "todo", "lorem", "... (sesuai", "lafaz niat per sholat",
    )

    // ---- Aturan nama berkas --------------------------------------------------------------------
    //
    // Nilai-nilainya dinyatakan sebagai data di sini, bukan disebar di dalam badan
    // [namaBerkasSah], karena gate CI **membacanya dari berkas ini**. `tools/validate_bundle.py`
    // dahulu menuliskan ulang seluruh aturannya sebagai fungsi `nama_berkas_sah` tersendiri —
    // batas `6`, batas `64`, akhiran `.json`, awalan terlarang, dan pemisah jalur — dan tak ada
    // apa pun yang membandingkan keduanya. Dua salinan berarti dua kebijakan: nama berkas yang lolos
    // gate dapat ditolak aplikasi saat dipasang (bundel tampak "terpasang" tetapi ditolak **utuh**,
    // karena satu nama yang ditolak membatalkan seluruh bundel), atau sebaliknya nama yang
    // sebenarnya berbahaya lolos gate lalu baru ditolak di perangkat pengguna.
    //
    // Karena itu skrip Python membaca nama-nama konstanta di bawah, persis seperti
    // `ContentValidator.allowedHosts` dibaca gate konten. Dijaga `AturanBundelSatuTempatTest`.

    /** Panjang nama berkas konten yang sah, dalam karakter. */
    const val PANJANG_NAMA_MIN = 6
    const val PANJANG_NAMA_MAKS = 64

    /** Akhiran wajib nama berkas konten. */
    const val AKHIRAN_NAMA = ".json"

    /**
     * Awalan yang tidak boleh dipakai nama berkas konten.
     *
     * Titik menyembunyikan berkas, dan garis bawah disisihkan untuk berkas internal (mis. metadata
     * pembaruan) agar tidak bisa ditimpa oleh isi bundel.
     */
    val AWALAN_NAMA_DILARANG = listOf(".", "_")

    /** Pemisah jalur, supaya nama berkas tidak dapat menunjuk ke luar direktori konten. */
    val PEMISAH_NAMA_DILARANG = listOf("/", "\\", ":")


    private val ARAB = TeksArab.regexArab

    /** Ringkasan satu dokumen yang lolos pemeriksaan. */
    data class Berkas(val nama: String, val sumberUrl: String, val kategori: List<String>)

    /** Hasil pembacaan sebuah bundel. */
    data class Hasil(
        val skema: Int = 0,
        val dibuat: String = "",
        val penerbit: String = "",
        val catatan: String = "",
        val berkas: Map<String, JSONObject> = emptyMap(),
        val ditolak: List<String> = emptyList(),
    ) {
        /** Bundel hanya boleh dipasang bila tidak ada satu pun berkas yang ditolak. */
        val sah: Boolean get() = ditolak.isEmpty() && berkas.isNotEmpty()
    }

    /**
     * Nama berkas yang boleh masuk ke direktori konten — aturannya ada pada konstanta di atas,
     * supaya gate CI membaca nilai yang sama alih-alih menyimpan salinannya.
     */
    fun namaBerkasSah(nama: String): Boolean =
        nama.length in PANJANG_NAMA_MIN..PANJANG_NAMA_MAKS &&
            nama.endsWith(AKHIRAN_NAMA) &&
            AWALAN_NAMA_DILARANG.none { nama.startsWith(it) } &&
            PEMISAH_NAMA_DILARANG.none { nama.contains(it) }

    /**
     * Baca dan periksa sebuah bundel. JSON yang tidak sah tidak melempar keluar: sebabnya
     * dikembalikan di [Hasil.ditolak] agar layar dapat menampilkannya apa adanya.
     */
    fun baca(teks: String): Hasil {
        val root = runCatching { JSONObject(teks) }.getOrElse {
            return Hasil(ditolak = listOf("Bundel bukan JSON yang sah: ${it.message.orEmpty().take(120)}"))
        }
        val meta = root.optJSONObject(KUNCI_META)
        val skema = meta?.optInt("skema", 0) ?: 0
        val tolak = mutableListOf<String>()
        if (skema != SKEMA) {
            tolak += "Skema bundel $skema tidak didukung — aplikasi ini membaca skema $SKEMA."
        }
        val peta = root.optJSONObject(KUNCI_BERKAS)
        if (peta == null) {
            tolak += "Bundel tidak memuat objek '$KUNCI_BERKAS'."
            return Hasil(skema, "", "", "", emptyMap(), tolak)
        }

        val diterima = mutableMapOf<String, JSONObject>()
        val nama = peta.keys().asSequence().toList().sorted()
        if (nama.isEmpty()) tolak += "Bundel tidak memuat berkas apa pun."
        nama.forEach { k ->
            val galat = mutableListOf<String>()
            if (!namaBerkasSah(k)) {
                galat += "Nama berkas '$k' tidak sah (harus nama polos berakhiran .json)."
            } else {
                val doc = peta.optJSONObject(k)
                if (doc == null) {
                    galat += "Isi '$k' bukan objek JSON."
                } else {
                    galat += periksaBerkas(doc)
                }
            }
            if (galat.isEmpty()) diterima[k] = peta.getJSONObject(k)
            else tolak += galat.map { "$k: $it" }
        }
        return Hasil(
            skema = skema,
            dibuat = meta?.optString("dibuat").orEmpty(),
            penerbit = meta?.optString("penerbit").orEmpty(),
            catatan = meta?.optString("catatan").orEmpty(),
            // Satu berkas gagal = tidak ada yang diterima, sehingga pemanggil tak mungkin
            // memasang sebagian bundel karena keliru memakai [Hasil.berkas] tanpa [Hasil.sah].
            berkas = if (tolak.isEmpty()) diterima else emptyMap(),
            ditolak = tolak,
        )
    }

    /** Aturan per-dokumen. Kosong berarti dokumen lolos. */
    fun periksaBerkas(doc: JSONObject): List<String> {
        val galat = mutableListOf<String>()

        val sumber = ContentValidator.sumberBerkas(doc)
        when {
            sumber.isBlank() -> galat += "tidak menyatakan 'sumber_url'"
            !ContentValidator.isOfficialSource(sumber) ->
                galat += "'sumber_url' bukan domain resmi: $sumber"
        }

        val kategori = kumpulkan(doc, "kategori")
        if (adaTeksArab(doc)) {
            if (kategori.isEmpty()) {
                galat += "memuat teks Arab tanpa 'kategori' sumber " +
                    "(pilihan: ${SumberKategori.SEMUA.joinToString()})"
            }
            galat += tanpaHarakat(doc).map { "teks Arab tanpa harakat: '$it'" }
        }
        kategori.filterNot { SumberKategori.dikenal(it) }.forEach {
            galat += "kategori tak dikenal: '$it' (pilihan: ${SumberKategori.SEMUA.joinToString()})"
        }
        galat += placeholder(doc).map { "field $it berisi prosa penunjuk, bukan bacaan" }
        return galat
    }

    /**
     * Periksa setiap **rujukan berkas** pada bundel yang sudah lolos [baca] — `rujukan_file`,
     * `rujukanFile`, dan larik `rujukan`, sama persis dengan kunci yang dibaca `ContentRepo`.
     *
     * Rujukan itu menunjuk berkas lain (`berkas.json` atau `berkas.json#id`) alih-alih menyalin
     * teksnya, sehingga satu lafaz terverifikasi dapat dipakai beberapa modul tanpa salinan yang
     * bisa menyimpang. Harganya: rujukan yang mati tidak menghasilkan teks kosong, melainkan
     * `FileNotFoundException` dari `ContentRepo.doc` — dan karena pembacaannya ada di dalam
     * `remember { repo.dzikir() }` pada layar, satu rujukan mati **menghentikan seluruh layar**
     * (Dzikir, Tahlil, Panduan Sholat) tanpa satu pun petunjuk sebabnya. Pemeriksaan di sini
     * memindahkan kegagalan itu ke tempat yang benar: bundelnya ditolak saat dipasang, dengan
     * alasan yang dapat dibaca pengguna, bukan saat layarnya dibuka.
     *
     * [berkasAset] adalah nama berkas konten bawaan aplikasi (`assets/content`). Rujukan yang
     * menunjuk berkas di dalam bundel ini **atau** di antara berkas bawaan itu dianggap sah;
     * rujukan yang tidak ada di keduanya ditolak. Isi berkas bawaan tidak diperiksa di sini
     * (aplikasi tidak membacanya saat memasang), melainkan oleh gerbang konten di CI.
     */
    fun periksaRujukan(hasil: Hasil, berkasAset: Set<String>): List<String> {
        val galat = mutableListOf<String>()
        hasil.berkas.forEach { (nama, doc) ->
            telusuriRujukan(doc) { kunci, nilai ->
                val daftar = when {
                    // Larik `rujukan` dibaca `ContentRepo` dengan `getJSONArray`, jadi teks di sini
                    // bukan sekadar tidak berguna — ia melempar saat layar membacanya.
                    kunci == KUNCI_RUJUKAN_DAFTAR && nilai !is JSONArray -> {
                        galat += "$nama: '$kunci' harus berupa larik teks"
                        emptyList()
                    }
                    nilai is String -> listOf(nilai)
                    nilai is JSONArray -> (0 until nilai.length()).map { i ->
                        val satu = nilai.opt(i)
                        if (satu is String) satu
                        else null.also { galat += "$nama: '$kunci[$i]' bukan teks" }
                    }
                    else -> {
                        galat += "$nama: '$kunci' harus berupa teks"
                        emptyList()
                    }
                }
                daftar.filterNotNull().forEach { periksaSatuRujukan(nama, kunci, it, hasil, berkasAset, galat) }
            }
        }
        return galat
    }

    /** Satu nilai rujukan: berkasnya harus ada, dan id setelah `#` harus ada di berkas itu. */
    private fun periksaSatuRujukan(
        nama: String,
        kunci: String,
        nilai: String,
        hasil: Hasil,
        berkasAset: Set<String>,
        galat: MutableList<String>,
    ) {
        val rujukan = nilai.trim()
        if (rujukan.isEmpty()) {
            galat += "$nama: '$kunci' kosong"
            return
        }
        val berkas = rujukan.substringBefore('#').trim()
        val id = rujukan.substringAfter('#', "").trim()
        when {
            berkas.isEmpty() -> galat += "$nama: rujukan '$rujukan' tidak menyebut berkas"
            berkas in hasil.berkas -> {
                // Target di dalam bundel yang sama: id-nya dapat diperiksa langsung.
                val target = hasil.berkas.getValue(berkas)
                if (id.isNotEmpty() && entriRujukan(target, id) == null) {
                    galat += "$nama: rujukan '$rujukan' menunjuk entri yang tidak ada di $berkas"
                }
            }
            berkas in berkasAset -> Unit
            else -> galat += "$nama: rujukan '$rujukan' menunjuk berkas yang tidak ada"
        }
    }

    /**
     * Panggil [terima] untuk setiap kemunculan kunci rujukan berkas pada [node], rekursif.
     *
     * Nilainya diteruskan apa adanya (bukan hanya yang berupa teks) supaya bentuk yang salah pun
     * dapat ditolak di sini — `ContentRepo` membaca larik `rujukan` dengan `getJSONArray`, jadi
     * `"rujukan": "berkas.json"` bukan sekadar tidak berguna melainkan melempar saat dibaca.
     */
    private fun telusuriRujukan(node: Any?, terima: (kunci: String, nilai: Any?) -> Unit) {
        when (node) {
            is JSONObject -> node.keys().asSequence().forEach { k ->
                val v = node.get(k)
                // Ejaan yang diterima datang dari satu tempat bersama pembacanya (`Rujukan`), bukan
                // dari daftar lokal di sini — dua daftar yang seharusnya sama adalah cara paling
                // halus untuk membuat pemeriksa dan pembaca berbeda pendapat.
                if (k in KUNCI_RUJUKAN || k == KUNCI_RUJUKAN_DAFTAR) terima(k, v)
                else telusuriRujukan(v, terima)
            }
            is JSONArray -> (0 until node.length()).forEach { telusuriRujukan(node.get(it), terima) }
        }
    }

    /** Semua nilai string (rekursif) di bawah [kunci] — mis. seluruh `kategori` di berkas. */
    private fun kumpulkan(node: Any?, kunci: String, keluar: MutableList<String> = mutableListOf()): List<String> {
        when (node) {
            is JSONObject -> node.keys().asSequence().forEach { k ->
                val v = node.get(k)
                if (k == kunci && v is String && v.isNotBlank()) keluar += v.trim()
                kumpulkan(v, kunci, keluar)
            }
            is JSONArray -> (0 until node.length()).forEach { kumpulkan(node.get(it), kunci, keluar) }
        }
        return keluar.distinct()
    }

    /** Field `arab`/`latin` (rekursif) beserta nilainya. */
    private fun fieldTeks(node: Any?, keluar: MutableList<Pair<String, String>> = mutableListOf()): List<Pair<String, String>> {
        when (node) {
            is JSONObject -> node.keys().asSequence().forEach { k ->
                val v = node.get(k)
                if ((k == "arab" || k == "latin") && v is String) keluar += k to v
                fieldTeks(v, keluar)
            }
            is JSONArray -> (0 until node.length()).forEach { fieldTeks(node.get(it), keluar) }
        }
        return keluar
    }

    private fun adaTeksArab(node: Any?): Boolean = when (node) {
        is String -> ARAB.containsMatchIn(node)
        is JSONObject -> node.keys().asSequence().any { k -> adaTeksArab(node.get(k)) }
        is JSONArray -> (0 until node.length()).any { adaTeksArab(node.get(it)) }
        else -> false
    }

    /**
     * Kata Arab tanpa tanda harakat pada field `arab`/`latin` (rekursif).
     *
     * Aturannya — rentang Arab, rentang huruf, tanda harakat, dan panjang kata minimum — ada di
     * [TeksArab], dipakai bersama gate konten dan uji assets. Yang tinggal di sini hanyalah **field
     * mana** yang dinilai: `arab`/`latin` saja, karena field lain memang memuat prosa Indonesia.
     */
    private fun tanpaHarakat(node: Any?, keluar: MutableList<String> = mutableListOf()): List<String> {
        fieldTeks(node).forEach { (_, teks) -> keluar += TeksArab.tanpaHarakat(teks) }
        return keluar.distinct()
    }

    private fun placeholder(node: Any?): List<String> =
        fieldTeks(node).filter { (_, teks) ->
            val low = teks.lowercase()
            PLACEHOLDER.any { it in low }
        }.map { it.first }.distinct()

    /** Ringkasan berkas yang lolos — dipakai layar untuk menampilkan sumber tiap berkas. */
    fun ringkasan(hasil: Hasil): List<Berkas> = hasil.berkas.map { (nama, doc) ->
        Berkas(nama, ContentValidator.sumberBerkas(doc), kumpulkan(doc, "kategori"))
    }.sortedBy { it.nama }
}
