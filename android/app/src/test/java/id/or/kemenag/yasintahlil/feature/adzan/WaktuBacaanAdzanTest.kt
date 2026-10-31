package id.or.kemenag.yasintahlil.feature.adzan

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * `plan.txt` §"Bacaan Setelah Adzan" meminta *"Penjelasan waktu membacanya"* untuk setiap bacaan:
 * menjawab adzan, sholawat, dan doa setelah adzan.
 *
 * Urutan saja tidak cukup — pengguna yang membuka layar saat adzan sedang berkumandang perlu tahu
 * bacaan mana yang berlaku **sekarang**, bukan hanya urutannya. Karena itu tiap entri menyatakan
 * waktunya sendiri, dan layar menampilkannya lewat satu jalur render yang sama supaya tidak ada
 * entri yang keterangannya diam-diam terlewat.
 */
class WaktuBacaanAdzanTest {

    private fun root(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun sumber(relatif: String): String = File(root(), relatif).readText()

    private val layarRel = "src/main/java/id/or/kemenag/yasintahlil/feature/adzan/IqomahDoaScreen.kt"
    private val repoRel = "src/main/java/id/or/kemenag/yasintahlil/core/data/ContentRepo.kt"

    /** Kunci entri di `adzan.json` beserta pembaca yang seharusnya mengambilnya dari repo. */
    private val entri = listOf(
        "menjawab_adzan" to "repo.menjawabAdzan()",
        "sholawat_setelah_adzan" to "repo.sholawatAdzan()",
        "doa_setelah_adzan" to "repo.doaAdzan()",
    )

    @Test
    fun setiapBacaanAdzanMenyatakanKapanDibaca() {
        val d = JSONObject(sumber("src/main/assets/content/adzan.json"))
        entri.forEach { (k, _) ->
            val waktu = d.getJSONObject(k).optString("waktu")
            assertTrue("'$k' tidak menyatakan kapan dibacanya", waktu.isNotBlank())
            // Waktunya harus menjelaskan, bukan sekadar mengulang judul.
            assertTrue("'$k' punya waktu yang terlalu pendek untuk menjelaskan: '$waktu'",
                waktu.length >= 25)
        }
    }

    /**
     * Setiap kunci JSON punya pembaca di repo, dan tak ada pembaca yang tak ada kuncinya.
     *
     * Arah pertama menjaga entri yang ditambahkan ke bundel tetapi tak terbaca layar mana pun —
     * waktunya hilang tanpa jejak. Arah kedua menjaga sebaliknya: pembaca yang ditambahkan ke repo
     * tanpa entri di bundel maupun gate. Tanpa arah kedua, uji lain tak akan pernah menyentuhnya —
     * semuanya berjalan dari daftar yang kebetulan sudah terdaftar — sehingga layar dapat memanggil
     * bacaan yang bundelnya tak punya, dan kartunya kosong tanpa satu pun galat.
     */
    @Test
    fun setiapEntriDibacaRepo() {
        val repo = sumber(repoRel)
        entri.forEach { (k, pembaca) ->
            assertTrue("repo tidak membaca entri '$k' (diharapkan $pembaca)",
                repo.contains("adzanDoa(\"$k\")"))
        }
        val dariRepo = Regex("""adzanDoa\("([^"]+)"\)""").findAll(repo)
            .map { it.groupValues[1] }.toSet()
        assertEquals(
            "pembaca adzan di ContentRepo berbeda dari kunci yang dituntut gate konten — " +
                "pembaca tanpa entri di bundel akan menampilkan bacaan kosong tanpa galat",
            kunciGateAdzan(), dariRepo,
        )
    }

    /**
     * Layar menampilkan waktu tiap bacaan, bukan hanya urutannya.
     *
     * Kedua field dicocokkan dengan batas kata, bukan pencarian biasa: `entri.catatan` juga termuat
     * di dalam `entri.catatanKaki`, sehingga penggantian nama field yang ceroboh tetap lolos dan
     * catatan pada entri diam-diam tak pernah tampil.
     */
    @Test
    fun layarMenampilkanWaktuTiapBacaan() {
        val layar = sumber(layarRel)
        assertTrue("layar tidak menampilkan waktu bacaan", layar.contains("Kapan dibaca:"))
        assertTrue("layar tidak membaca field waktu",
            Regex("""entri\.waktu\b""").containsMatchIn(layar))
        // Ketiga bacaan harus lewat jalur render yang sama; sebelumnya dua entri dirender langsung
        // dan catatan pada entri lain akan terlewat tanpa jejak.
        entri.forEach { (_, pembaca) ->
            assertTrue("layar tidak memuat bacaan lewat $pembaca", layar.contains(pembaca))
        }
        // Hitung **panggilan** jalur render bersama, bukan sekadar penyebutan namanya: definisi
        // `private fun KartuDoa(...)` pun memuat "KartuDoa(", sehingga pencarian biasa tetap hijau
        // walau sebuah entri diam-diam dirender lewat helper lain dan catatannya ikut hilang.
        val jalur = Regex("""KartuDoa\(JenisBacaan\.id\(""").findAll(layar).count()
        assertTrue(
            "bacaan tidak dirender lewat satu jalur bersama (ditemukan $jalur panggilan KartuDoa, " +
                "diharapkan ${entri.size})",
            jalur >= entri.size,
        )
    }

    /** Catatan yang menyertai bacaan tidak boleh hilang hanya karena entrinya berbeda. */
    @Test
    fun catatanDirenderUntukSemuaEntri() {
        val layar = sumber(layarRel)
        assertTrue("catatan tidak dirender dari jalur bersama",
            Regex("""entri\.catatan\b""").containsMatchIn(layar))
    }

    // ---- Daftar entri itu sendiri harus sepakat di keempat tempat ----

    /**
     * Akar repositori, tempat `app/` dan `tools/` bersebelahan — uji berjalan dengan direktori kerja
     * modul (`app/`), jadi [root] menunjuk ke `app`.
     */
    private fun repoRoot(): File =
        generateSequence(root().absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "tools/validate_content.py").isFile }
            ?: error("tools/validate_content.py tidak ditemukan dari ${root().absolutePath}")

    /**
     * Kunci entri di `adzan.json` yang menyatakan **kapan** dibaca — dibaca dari bundelnya sendiri,
     * bukan dari daftar yang ditulis ulang di berkas ini.
     */
    private fun kunciBersWaktu(): Set<String> {
        val d = JSONObject(sumber("src/main/assets/content/adzan.json"))
        return d.keys().asSequence()
            .filter { d.optJSONObject(it)?.optString("waktu")?.isNotBlank() == true }
            .toSet()
    }

    /**
     * Daftar [entri] di berkas ini harus sama dengan entri ber-`waktu` di bundel.
     *
     * Tanpa pemeriksaan ini, daftar itu hanyalah salinan yang tak dijaga: entri ber-`waktu` yang
     * ditambahkan ke bundel tak akan pernah dituntut punya pembaca di repo maupun ditampilkan layar,
     * karena seluruh pemeriksaan di atas berhenti pada entri yang kebetulan sudah terdaftar di sini.
     */
    @Test
    fun daftarEntriUjiSelarasDenganEntriBerWaktuDiBundel() {
        assertEquals(
            "entri ber-'waktu' di adzan.json berbeda dari daftar di uji ini — entri baru tak akan " +
                "diperiksa, entri yang hilang masih dituntut ada",
            kunciBersWaktu(), entri.map { it.first }.toSet(),
        )
    }

    /**
     * Daftar kunci yang dituntut ber-`waktu` oleh gate konten (`tools/validate_content.py`).
     *
     * Gate menyimpan daftarnya sendiri — salinan ketiga dari kosakata yang sama. Helper ini
     * satu-satunya tempat yang membacanya, supaya kedua uji yang memakainya tak memelihara pengurai
     * yang berbeda-beda untuk daftar yang sama.
     */
    private fun kunciGateAdzan(): Set<String> {
        val gerbang = File(repoRoot(), "tools/validate_content.py").readText()
        val baris = gerbang.lines().singleOrNull { it.contains("for k in (") && it.contains("adzan") }
        assertTrue(
            "gate konten tidak lagi menuntut 'waktu' pada entri adzan — pemeriksaan 'kapan dibaca' " +
                "telah hilang dari gate, atau penandanya berganti nama",
            baris != null,
        )
        return Regex("\"([a-z_]+)\"").findAll(baris!!).map { it.groupValues[1] }.toSet()
    }

    /**
     * Gate konten menuntut `waktu` pada entri yang **sama** dengan yang dibaca aplikasi.
     *
     * `tools/validate_content.py` menyimpan daftar kuncinya sendiri — salinan ketiga dari kosakata
     * yang sama. Bila sebuah kunci berganti nama di bundel dan di pembaca repo tanpa ikut disunting
     * di gate, gate berhenti menuntut `waktu` untuk entri itu **tanpa satu pun galat**: `data.get(k)`
     * mengembalikan `None`, syarat `isinstance(entri, dict)` tidak terpenuhi, dan pemeriksaannya
     * dilewati begitu saja. Keterangan "kapan dibaca" karena itu dapat hilang dari bundel tanpa ada
     * yang menahannya — padahal justru itu yang diminta `plan.txt`.
     */
    @Test
    fun gateKontenMenuntutWaktuPadaEntriYangSama() {
        assertEquals("daftar entri ber-'waktu' di gate konten berbeda dari bundel",
            kunciBersWaktu(), kunciGateAdzan())
    }
}
