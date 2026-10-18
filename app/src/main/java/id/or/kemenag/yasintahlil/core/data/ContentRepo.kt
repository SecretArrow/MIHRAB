package id.or.kemenag.yasintahlil.core.data

import android.content.Context
import android.util.Log
import id.or.kemenag.yasintahlil.core.util.ContentValidator
import org.json.JSONObject
import java.io.File

/**
 * Repository offline-first: konten dibaca dari assets/content (sumber resmi), dengan lapisan
 * pembaruan opsional di `filesDir/content_update/` (lihat [ContentUpdateManager]).
 *
 * Lapisan pembaruan hanya dipakai bila berkasnya ada **dan** masih lolos pemeriksaan sumber:
 * berkas yang rusak, hilang, atau kehilangan `sumber_url` resmi otomatis diabaikan sehingga
 * aplikasi kembali membaca konten bawaan yang sudah diverifikasi.
 *
 * Nama wadah entri dan kunci `id`-nya dibaca dari [BentukKonten], satu tempat bersama penelusuran
 * rujukan ([entriRujukan]) — lihat catatan di sana tentang apa yang terjadi bila keduanya berbeda.
 *
 * Nama **berkasnya** pun demikian: setiap nama datang dari [BerkasKonten], sebab nama itu adalah
 * kunci ke aset sekaligus ke direktori pembaruan. Sebelas nama dahulu dieja di dalam kelas ini dan
 * tak satu pun dibandingkan dengan berkas yang benar-benar dikirim — berkas yang diganti namanya
 * tetap lolos seluruh uji dan baru ketahuan saat layarnya dibuka pengguna.
 */
class ContentRepo(private val ctx: Context) {
    private val cache = mutableMapOf<String, JSONObject>()
    private val updateDir = File(ctx.filesDir, ContentUpdateManager.DIR)
    private val metaPembaruan = File(updateDir, ContentUpdateManager.META)

    /**
     * Koma Arab (U+060C) — pemisah ayat pada rangkaian Arab yang mengalir (rujukan satu surat
     * utuh). Ditulis sebagai escape agar **teks Arab tetap tinggal di bundel**: kode memuat
     * pemisahnya saja, dan penjaga "tidak ada teks Arab hardcoded di kode" tetap hijau.
     */
    private val KOMA_ARAB = "\u060C"

    /** Cap terakhir pembaruan; berubahnya cap membatalkan cache agar tak menyajikan teks lama. */
    private var cap = metaPembaruan.lastModified()

    private fun doc(file: String): JSONObject {
        val capKini = metaPembaruan.lastModified()
        if (capKini != cap) {
            cache.clear()
            cap = capKini
        }
        return cache.getOrPut(file) {
            dariPembaruan(file) ?: dariAset(file)
        }
    }

    /** Dokumen bila dapat dibaca, `null` bila tidak — untuk rujukan yang belum tentu ada. */
    private fun docAtauNull(file: String): JSONObject? =
        runCatching { doc(file) }.getOrElse {
            Log.w("ContentRepo", "Rujukan konten '$file' tidak dapat dibaca", it)
            null
        }

    /** Dokumen dari lapisan pembaruan, atau null bila tak ada / tak lagi layak dipakai. */
    private fun dariPembaruan(file: String): JSONObject? {
        val f = File(updateDir, file)
        if (!f.isFile) return null
        val json = runCatching { JSONObject(f.readText()) }.getOrElse {
            Log.w("ContentRepo", "Konten pembaruan $file rusak — memakai bawaan", it)
            return null
        }
        val src = ContentValidator.sumberBerkas(json)
        if (src.isBlank() || !ContentValidator.isOfficialSource(src)) {
            Log.w("ContentRepo", "Konten pembaruan $file tanpa sumber_url resmi — memakai bawaan")
            return null
        }
        return json
    }

    private fun dariAset(file: String): JSONObject {
        val json = JSONObject(ctx.assets.open("content/$file").bufferedReader().readText())
        // Penjaga runtime: ingatkan bila konten kehilangan atribusi sumber resmi.
        val src = ContentValidator.sumberBerkas(json)
        if (src.isBlank() || !ContentValidator.isOfficialSource(src)) {
            Log.w("ContentRepo", "Konten $file tanpa sumber_url resmi — periksa SOURCES.md")
        }
        return json
    }

    private fun verse(o: JSONObject) = Verse(
        o.getInt("nomor"), o.getString("arab"),
        o.optString("latin"), o.optString("terjemahan")
    )

    fun surah(file: String): Surah {
        val d = doc(file)
        val arr = d.getJSONArray("verses")
        return Surah(
            d.optString("surah", file), d.optInt("ayat_count", arr.length()),
            (0 until arr.length()).map { verse(arr.getJSONObject(it)) },
            d.optString("sumber_terjemahan", ""),
            d.optString("kategori", ""),
            // Asal teks Arab & transliterasi dicatat terpisah dari terjemahan di berkasnya; tanpa
            // keduanya, hanya asal terjemahan yang dapat ditampilkan.
            d.optString("sumber_arab", ""),
            d.optString("sumber_latin", ""),
        )
    }

    val yasin: Surah get() = surah(BerkasKonten.YASIN_FULL)

    /**
     * Teks Arab/Latin/terjemahan dari sebuah berkas rujukan.
     *
     * Nama berkas boleh diikuti `#id` untuk menunjuk **satu entri** di dalam berkas yang memuat
     * daftar (`bagian` pada tahlil, `bacaan` pada bacaan sholat), mis.
     * `sholat_mayit.json#doa_mayit`. Dengan begitu teks yang sama dapat dipakai di beberapa modul
     * tanpa disalin ulang — salinan yang menyimpang adalah cara paling halus untuk membuat dua
     * bacaan berbeda dari satu sumber yang sama.
     *
     * Rujukan yang tidak dapat ditelusuri mengembalikan teks kosong, **bukan** melempar: berkasnya
     * dibaca lewat [docAtauNull], dan pemanggil sudah punya jalan mundur ke teks milik entrinya
     * sendiri (`o.optString("arab", a)`). Sebelumnya `doc` dipanggil langsung, sehingga satu
     * rujukan mati menaikkan `FileNotFoundException` sampai ke `remember { repo.dzikir() }` pada
     * layar dan menghentikan seluruh layar. Bundel yang memuat rujukan mati sudah ditolak saat
     * dipasang ([BundelKonten.periksaRujukan]); jalan mundur ini menjaga berkas bawaan yang
     * kehilangan sasarannya karena satu berkas dihapus, tanpa menjatuhkan layar.
     */
    fun resolveArabLatinTerjemah(rujukanFile: String?): Triple<String, String, String> {
        if (rujukanFile.isNullOrBlank()) return Triple("", "", "")
        val pisah = rujukanFile.split("#", limit = 2)
        val nama = pisah[0].trim()
        if (nama.isBlank()) return Triple("", "", "")
        val d = docAtauNull(nama) ?: return Triple("", "", "")
        val id = pisah.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        if (id != null) {
            val o = entriRujukan(d, id) ?: return Triple("", "", "")
            return Triple(o.optString("arab"), o.optString("latin"), o.optString("terjemahan"))
        }
        return if (d.has("verses")) {
            // Arab utuh mengalir — ayat-ayatnya dirangkai dengan koma Arab (lihat [KOMA_ARAB])
            // seperti mushaf dibaca bersambung, bukan daftar bernomor.
            val arr = d.getJSONArray("verses")
            Triple(
                (0 until arr.length()).joinToString("$KOMA_ARAB ") { i ->
                    arr.getJSONObject(i).getString("arab") },
                (0 until arr.length()).joinToString(" ") { i ->
                    arr.getJSONObject(i).optString("latin") },
                (0 until arr.length()).joinToString(" ") { i ->
                    arr.getJSONObject(i).optString("terjemahan") }
            )
        } else Triple(d.optString("arab"), d.optString("latin"), d.optString("terjemahan"))
    }

    /**
     * Seluruh bagian Tahlil dari berkas konten, urut sesuai berkas; [susunanId] memilahnya
     * mengikuti susunan di `tahlil.json`. Rincian "ayat demi ayat" dahulu ikut dimuat untuk
     * blok kartu bernama sama — dihapus karena mengulang bacaan yang sama (permintaan
     * pengguna, 2026-10-17), dan bersama itu jalur rinciannya dipensunkan seluruhnya.
     */
    fun tahlil(susunanId: String? = null): List<TahlilItem> {
        val d = doc(BerkasKonten.TAHLIL_FULL)
        val arr = d.getJSONArray(BentukKonten.BAGIAN)
        val def = d.optString("kategori", "")
        val all = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val rujukan = rujukanTeks(o)
            val (a, l, t) = resolveArabLatinTerjemah(rujukan)
            TahlilItem(
                o.getString(BentukKonten.ID), o.getString("judul"),
                o.optString("arab", a), o.optString("latin", l), o.optString("terjemahan", t),
                o.optInt("ulangi", 1),
                rujukan,
                o.optString("sumber", ""),
                o.optString("kategori", def),
            )
        }
        if (susunanId == null) return all
        val order = tahlilSusunan().firstOrNull { it.id == susunanId }?.bagian ?: return all
        val byId = all.associateBy { it.id }
        return order.mapNotNull { byId[it] }.ifEmpty { all }
    }

    /**
     * Peta id bagian Tahlil → pembaginya pada Mode Imam (`tahlil_imam.json`): siapa yang membaca
     * (Bersama/Imam/Sendiri-sendiri) beserta aba singkat dan keterangannya. Mode Imam menampilkan
     * susunan Lengkap dengan lencana pembagi ini — berkasnya tidak memuat lafaz apa pun, sehingga
     * peta kosong (berkas tak terbaca) hanya berarti lencana tidak tampil, bukan bacaan hilang.
     */
    fun tahlilImam(): Map<String, TahlilImamAba> {
        val d = docAtauNull(BerkasKonten.TAHLIL_IMAM) ?: return emptyMap()
        val aba = d.optJSONObject("aba") ?: return emptyMap()
        val out = mutableMapOf<String, TahlilImamAba>()
        aba.keys().forEach { id ->
            val o = aba.optJSONObject(id) ?: return@forEach
            out[id] = TahlilImamAba(
                o.optString("aba"), o.optString("siapa"), o.optString("ket"),
            )
        }
        return out
    }

    /**
     * Daftar susunan tahlil yang tersedia (mis. "lazim Indonesia").
     *
     * Setiap susunan menyebut sumbernya sendiri bila berkasnya mencantumkan `sumber`; bila tidak,
     * dipakai keterangan `edisi` tingkat modul — supaya susunan yang tersedia tidak tampil sebagai
     * rangkaian tanpa asal-usul (`plan.txt` §"Fitur Tahlil Lengkap").
     *
     * `edisi` dibaca dari `tahlil.json` saja — **satu** rumah untuk seluruh modul Tahlil. Berkas
     * `tahlil_full.json` dahulu menyalinnya juga dengan ejaan yang berbeda (*"… memakai MSI LPMQ
     * Kemenag + terjemah Kemenag edisi 2019 …"* versus *"… bacaan Qur'an Kemenag 2019 …"*), padahal
     * yang dibaca kode hanya salinan di `tahlil.json` — dan salinan itu justru yang **lebih
     * pendek**, sehingga layar menyebut asal bacaan Qur'annya lebih longgar daripada yang dicatat
     * sumbernya. Salinan mati di `tahlil_full.json` sudah dihapus; penjaganya
     * `AtribusiSusunanTahlilTest.edisiModulTinggalDiSatuBerkasTahlil`.
     */
    fun tahlilSusunan(): List<TahlilSusunan> {
        val d = doc(BerkasKonten.TAHLIL_SUSUNAN)
        val arr = d.optJSONArray("susunan") ?: return emptyList()
        val edisiModul = d.optString("edisi", "")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val b = o.optJSONArray(BentukKonten.BAGIAN) ?: org.json.JSONArray()
            TahlilSusunan(
                o.getString(BentukKonten.ID), o.optString("nama", o.getString(BentukKonten.ID)),
                (0 until b.length()).map { b.getString(it) },
                o.optString("sumber", edisiModul),
            )
        }
    }

    fun sholatWajib(): List<SholatInfo> = sholatList("wajib")
    fun sholatSunnah(): List<SholatInfo> = sholatList("sunnah")

    /**
     * Sumber panduan tingkat modul dari `sholat_daftar.json` (`panduan`).
     *
     * Layar Panduan Sholat mencetak "(Fasolatan Kemenag)" sebagai teks hardcode, sementara
     * berkasnya sudah mencatat keterangan yang lebih tepat — *"Fasolatan Ditjen Bimas Islam Kemenag;
     * verifikasi hadis via sunnah.com"*. Keterangan itu ditulis, divalidasi gerbang konten, lalu
     * tak pernah sampai ke layar. Kosong = tidak diklaim.
     */
    fun sholatPanduan(): String = doc(BerkasKonten.SHOLAT_DAFTAR).optString("panduan", "")

    /**
     * Catatan mazhab tingkat modul dari `sholat_daftar.json` (`catatan_mazhab`).
     *
     * Layar rincian langkah sholat mencetak catatannya sendiri: *"Indonesia umumnya Syafi'i (qunut
     * Subuh sunnah)"*. Bundelnya **tidak** mengambil pendirian itu — berkasnya menulis bahwa qunut
     * Subuh termasuk ketentuan yang *"memiliki perbedaan pendapat"*. Jadi kalimat di layar bukan
     * sekadar salinan yang lebih pendek: ia menyatakan sebagai mapan hal yang sumbernya sendiri
     * catat sebagai diperselisihkan, dan tak ada apa pun yang membandingkannya dengan berkas.
     *
     * `catatan_mazhab` dan `edisi` adalah dua metadata provenans yang — seperti dicatat
     * `SOURCES.md` §6 — tidak diperiksa gerbang konten, sehingga kelengkapannya hanya terjaga bila
     * benar-benar **dibaca dan ditampilkan** seperti di sini. Kosong = tidak diklaim.
     */
    fun sholatCatatanMazhab(): String =
        doc(BerkasKonten.SHOLAT_DAFTAR).optString("catatan_mazhab", "")

    /**
     * Catatan tradisi tingkat modul dari `tahlil.json` (`catatan_tradisi`).
     *
     * Layar Tahlil mencetak *"susunan dapat berbeda menurut tradisi"* sebagai teks hardcode —
     * kalimat yang sudah ada di bundel dengan bunyi yang lebih lengkap (*"…; bukan satu-satunya
     * versi yang benar"*). Membacanya dari berkas menjaga agar peringatan itu ikut berubah bila
     * bundelnya diperbarui, bukan tertinggal sebagai salinan di kode.
     */
    fun tahlilCatatanTradisi(): String = doc(BerkasKonten.TAHLIL_SUSUNAN).optString("catatan_tradisi", "")

    /**
     * Sumber bacaan tingkat modul dari `tahlil_full.json` (`sumber_umum`).
     *
     * Sebagian besar bagian tahlil tidak menyebut sumbernya sendiri; keterangan tingkat modul
     * inilah yang berlaku untuk mereka (*"Bacaan Qur'an dari Mushaf Standar Indonesia LPMQ Kemenag;
     * doa dari khazanah sunnah"*). Tanpa dibaca, kartu bagian itu tampil tanpa asal-usul apa pun.
     */
    fun tahlilSumberBacaan(): String = doc(BerkasKonten.TAHLIL_FULL).optString("sumber_umum", "")

    private fun sholatList(key: String): List<SholatInfo> {
        val d = doc(BerkasKonten.SHOLAT_DAFTAR)
        val defKategori = d.optString("kategori", "")
        val arr = d.getJSONArray(key)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val n = o.optJSONObject("niat")
            val ket = o.optJSONArray("ketentuan")?.let { k -> (0 until k.length()).map { k.getString(it) } }.orEmpty()
            SholatInfo(
                o.getString(BentukKonten.ID), o.getString("nama"), o.getString("rakaat"),
                o.getString("waktu"), o.getString("deskripsi"),
                n?.let {
                    Niat(it.getString("arab"), it.getString("latin"), it.getString("terjemahan"),
                        it.optString("kategori", defKategori))
                },
                rujukanTeks(o),
                ketentuan = ket,
                niatCatatan = o.optString("niat_catatan", ""),
                tujuan = o.optString("tujuan"),
            )
        }
    }

    fun sholatBacaan(): List<Bacaan> = parseBacaan(BerkasKonten.SHOLAT_BACAAN)

    /** Bacaan doa mayit (dipakai sholat jenazah & ghaib) — berkas terpisah agar tidak
     *  tercampur ke daftar langkah sholat umum. */
    fun sholatMayitBacaan(): List<Bacaan> = parseBacaan(BerkasKonten.SHOLAT_MAYIT)

    private fun parseBacaan(file: String): List<Bacaan> {
        val d = doc(file)
        val defKategori = d.optString("kategori", "")
        val arr = d.getJSONArray(BentukKonten.BACAAN)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val rujukan = rujukanTeks(o)
            val (a, l, t) = resolveArabLatinTerjemah(rujukan)
            val scope = o.optJSONArray("sholat")?.let { s -> (0 until s.length()).map { s.getString(it) } }.orEmpty()
            Bacaan(o.getString(BentukKonten.ID), o.getString("judul"), o.optString("kapan"),
                o.optString("arab", a), o.optString("latin", l), o.optString("terjemahan", t),
                o.optString("sumber"),
                rujukan,
                catatan = o.optString("catatan", ""),
                mazhab = o.optString("mazhab", "").takeIf { it.isNotBlank() },
                sholatScope = scope,
                kategori = o.optString("kategori", defKategori))
        }
    }

    fun dzikir(): List<DzikirGroup> {
        val d = doc(BerkasKonten.DZIKIR)
        val defKategori = d.optString("kategori", "")
        val arr = d.getJSONArray(BentukKonten.KELOMPOK)
        return (0 until arr.length()).map { gi ->
            val g = arr.getJSONObject(gi)
            val items = g.getJSONArray(BentukKonten.ITEMS)
            DzikirGroup(g.getString(BentukKonten.ID), g.getString("judul"),
                (0 until items.length()).map { i ->
                    val o = items.getJSONObject(i)
                    val ruj = mutableListOf<String>()
                    if (o.has(KUNCI_RUJUKAN_DAFTAR)) {
                        val ra = o.getJSONArray(KUNCI_RUJUKAN_DAFTAR)
                        repeat(ra.length()) { ruj += ra.getString(it) }
                    }
                    val all = rujukanTeksSemua(o) + ruj
                    val joined = all.map { resolveArabLatinTerjemah(it) }
                    val ja = joined.joinToString("\n\n") { it.first }.ifBlank { o.optString("arab") }
                    val jl = joined.joinToString("\n\n") { it.second }.ifBlank { o.optString("latin") }
                    val jt = joined.joinToString("\n\n") { it.third }.ifBlank { o.optString("terjemahan") }
                    DzikirItem(o.getString("judul"), o.optString("arab", ja),
                        o.optString("latin", jl), o.optString("terjemahan", jt),
                        o.optInt("ulangi", 1), o.optString("sumber"),
                        all.firstOrNull(), all.drop(1),
                        o.optString("kategori", defKategori))
                })
        }
    }

    fun adzan(): List<AdzanLafaz> {
        val d = doc(BerkasKonten.ADZAN)
        val def = d.optString("kategori", "")
        val bagian = d.getJSONObject("adzan")
        val defSumber = bagian.optString("sumber", "")
        val arr = bagian.getJSONArray("daftar")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AdzanLafaz(o.getString("arab"), o.getString("latin"), o.optString("terjemahan"),
                o.optInt("ulangi", 1), o.optString("sumber", defSumber),
                o.optString("kategori", def))
        }
    }

    fun adzanCatatan(): String = doc(BerkasKonten.ADZAN).getJSONObject("adzan").optString("catatan_subuh")

    /**
     * Catatan perbedaan susunan iqomah antar mazhab/tradisi — ditampilkan apa adanya.
     *
     * Dibaca dari **bagiannya sendiri** (`adzan.json` → `iqomah.catatan_mazhab`), mengikuti
     * konvensi berkasnya: catatan yang berlaku untuk satu bagian tinggal di dalam bagian itu,
     * seperti `adzan.catatan_subuh` yang dibaca [adzanCatatan]. Berkasnya dahulu memuat keterangan
     * ini **dua kali** — salinan di tingkat modul (dibaca fungsi ini, tanpa contoh perbedaannya)
     * dan salinan di dalam `iqomah` (tak pernah dibaca, tetapi menyebut *"(mis. jumlah lafaz)"*).
     * Jadi yang tampil di layar Iqomah bukan sekadar salinan yang lebih pendek: contoh yang
     * menjelaskan **apa** yang berbeda antar mazhab justru hilang. Salinan tingkat modul dihapus dan
     * ejaan yang lebih lengkap tinggal di `iqomah` — satu keterangan, satu rumah. Kosong = tidak
     * diklaim.
     */
    fun iqomahCatatan(): String =
        doc(BerkasKonten.ADZAN).getJSONObject("iqomah").optString("catatan_mazhab")

    fun iqomah(): List<AdzanLafaz> {
        val d = doc(BerkasKonten.ADZAN)
        val def = d.optString("kategori", "")
        val bagian = d.getJSONObject("iqomah")
        val defSumber = bagian.optString("sumber", "")
        val arr = bagian.getJSONArray("daftar")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AdzanLafaz(o.getString("arab"), o.getString("latin"), o.optString("terjemahan"),
                o.optInt("ulangi", 1), o.optString("sumber", defSumber),
                o.optString("kategori", def))
        }
    }

    fun doaAdzan(): AdzanDoa = adzanDoa("doa_setelah_adzan")

    /** Entri adzan.json sebagai [AdzanDoa] — kategori disimpulkan dari tingkat berkas bila kosong. */
    private fun adzanDoa(key: String): AdzanDoa {
        val d = doc(BerkasKonten.ADZAN)
        val o = d.getJSONObject(key)
        return AdzanDoa(o.getString("judul"), o.getString("arab"), o.getString("latin"),
            o.optString("terjemahan"), o.optString("catatan"), o.optString("waktu"),
            o.optString("sumber"), o.optString("kategori", d.optString("kategori", "")))
    }

    /** Lafaz menjawab adzan beserta catatan kapan lafaz ini dibaca. */
    fun menjawabAdzan(): AdzanDoa = adzanDoa("menjawab_adzan")

    /** Sholawat yang dibaca setelah menjawab adzan (sebelum doa setelah adzan). */
    fun sholawatAdzan(): AdzanDoa = adzanDoa("sholawat_setelah_adzan")

    fun kota(): List<Kota> {
        val arr = doc(BerkasKonten.KOTA).getJSONArray("kota")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i); Kota(o.getString("nama"), o.getDouble("lat"), o.getDouble("lon"))
        }
    }

    /** Panduan bersuci (wudhu, tayamum, pembatal) — langkah prosedural tanpa teks Arab. */
    fun panduanBersuci(): PanduanBersuci {
        val d = doc(BerkasKonten.WUDHU)
        fun langkah(key: String): List<Langkah> {
            val arr = d.optJSONObject(key)?.optJSONArray("langkah") ?: return emptyList()
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Langkah(o.optInt("no", i + 1), o.getString("judul"), o.optString("keterangan"))
            }
        }
        fun stringList(key: String): List<String> {
            val arr = d.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { arr.getString(it) }
        }
        // Judul bagian dibaca dari objeknya sendiri (`wudhu.judul`, `tayamum.judul`); tanpa ini
        // layar mengetik headingnya sendiri dan keterangan di dalam judul berkasnya tak pernah tampil.
        fun judulBagian(key: String): String = d.optJSONObject(key)?.optString("judul").orEmpty()
        return PanduanBersuci(
            rukun = stringList("rukun"),
            wudhu = langkah("wudhu"),
            tayamum = langkah("tayamum"),
            pembatal = stringList("pembatal"),
            catatanMazhab = d.optString("catatan_mazhab"),
            catatanLafaz = d.optString("catatan_lafaz"),
            judulWudhu = judulBagian("wudhu"),
            judulTayamum = judulBagian("tayamum"),
            edisi = d.optString("edisi"),
        )
    }

    /**
     * Syarat dan persiapan sholat dari `sholat_syarat.json`.
     *
     * Berkasnya sengaja **tanpa teks Arab**: yang dibutuhkan pengguna di sini adalah daftar periksa
     * berbahasa Indonesia (sudah wudhu? sudah menutup aurat? sudah masuk waktu?), bukan lafaz.
     * Lafaznya ada di `sholat_bacaan.json` dan `wudhu.json` dengan sumbernya masing-masing.
     */
    fun sholatSyarat(): SholatSyarat {
        val d = doc(BerkasKonten.SHOLAT_SYARAT)
        fun stringList(key: String): List<String> {
            val arr = d.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { arr.getString(it) }
        }
        val aurat = d.optJSONObject("penutup_aurat")
        return SholatSyarat(
            syaratWajib = stringList("syarat_wajib"),
            syaratSah = stringList("syarat_sah"),
            persiapan = stringList("persiapan"),
            penutupAurat = Aurat(
                judul = aurat?.optString("judul").orEmpty(),
                lakiLaki = aurat?.optString("laki_laki").orEmpty(),
                perempuan = aurat?.optString("perempuan").orEmpty(),
                catatan = aurat?.optString("catatan").orEmpty(),
            ),
            kebersihanTempat = stringList("kebersihan_tempat"),
            catatanMazhab = d.optString("catatan_mazhab"),
            catatanVerifikasi = d.optString("catatan_verifikasi"),
        )
    }

    /** Konten sholat khusus (ghaib/jenazah) dari assets — termasuk varian niat. */
    fun sholatKhusus(file: String): SholatKhusus {
        val d = doc(file)
        fun stringList(key: String): List<String> {
            val arr = d.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { arr.getString(it) }
        }
        val niatArr = d.optJSONArray("niat") ?: org.json.JSONArray()
        val defKategori = d.optString("kategori", "")
        val niat = (0 until niatArr.length()).map { i ->
            val o = niatArr.getJSONObject(i)
            Bacaan(
                o.optString(BentukKonten.ID, "niat_$i"), o.optString("judul"),
                o.optString("kapan"), o.optString("arab"), o.optString("latin"),
                o.optString("terjemahan"), o.optString("sumber"),
                catatan = o.optString("catatan"),
                kategori = o.optString("kategori", defKategori),
            )
        }
        return SholatKhusus(
            judul = d.optString("judul"),
            hukum = d.optString("hukum"),
            takbir = d.optInt("takbir", 4),
            // Dibaca supaya judul layar tidak lagi mengetik keterangan "tanpa ruku/sujud" sendiri:
            // dahulu `TampilanSholatKhusus.judul` selalu menambahkannya, apa pun isi bundelnya.
            tanpaRukuSujud = d.optBoolean("tanpa_ruku_sujud", false),
            dalil = d.optString("dalil"),
            // Dibaca supaya layar Ghaib tidak lagi mengetik kalimat rujukannya sendiri: salinan di
            // kode menyebut NU Online padahal `sumber_tambahan` berkasnya tak mencantumkannya.
            catatanRujukan = d.optString("catatan_rujukan"),
            catatanMazhab = d.optString("catatan_mazhab"),
            syarat = stringList("syarat"),
            niat = niat,
            langkah = stringList("langkah"),
        )
    }

    /**
     * Domain resmi yang **benar-benar dipakai** bundel konten, untuk layar Tentang.
     *
     * Dibaca dari seluruh nilai bersumber pada tiap berkas konten — bukan dari daftar tetap di kode,
     * dan bukan hanya `sumber_url` tingkat berkas. Berkas yang menambahkan `sumber_tambahan`
     * (daftar URL per modul, mis. `sholat_ghaib.json`) karena itu ikut tampil: daftar ini harus
     * memuat setiap domain yang benar-benar dirujuk, sebab layar Tentang menjanjikan bahwa daftar
     * itu "tidak dapat bertambah diam-diam".
     *
     * Berkas yang rusak dahulu dilewati tanpa jejak (`mapNotNull { runCatching { doc(f) } … }`),
     * sehingga daftar yang tampak lengkap bisa kehilangan domainnya. Kini berkas itu dicatat pada
     * [SumberBundel.gagal] supaya layar Tentang dapat mengatakan bahwa daftarnya belum tentu utuh —
     * janji "tidak bertambah diam-diam" hanya bermakna bila daftar itu juga tidak dapat *menyusut*
     * diam-diam.
     */
    fun sumberDipakai(): SumberBundel {
        val berkas = runCatching { ctx.assets.list("content")?.toList() }.getOrNull()
            ?: return SumberBundel(emptyList(), listOf("direktori content pada aset tidak dapat dibaca"))
        val host = mutableListOf<String>()
        val gagal = mutableListOf<String>()
        berkas.filter { it.endsWith(".json") }.forEach { f ->
            val d = runCatching { doc(f) }.getOrElse { e ->
                gagal += "$f (${e.message ?: e::class.java.simpleName})"
                return@forEach
            }
            ContentValidator.sumberUrl(d).forEach { url ->
                ContentValidator.hostOf(url)?.let { host += it }
            }
        }
        return SumberBundel(host.distinct().sorted(), gagal.sorted())
    }
}
