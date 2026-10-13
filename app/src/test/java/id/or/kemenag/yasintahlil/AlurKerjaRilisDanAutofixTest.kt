package id.or.kemenag.yasintahlil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Keputusan **pemicu** alur kerja rilis & auto-fix dikunci di sini supaya tidak "diperbaiki" keliru.
 *
 * Dua pelajaran mahal yang dijaga:
 *
 * 1. `autofix.yml` **tidak boleh** dipicu `pull_request`. Dibuktikan empiris (PR #37): bila bot
 *    meng-commit perbaikan ke cabang PR, GitHub membuat rangkaian run untuk commit itu dengan aktor
 *    `github-actions[bot]`; karena bot bukan kolaborator, run itu berhenti di `action_required`
 *    (menunggu persetujuan manusia) — sehingga commit perbaikannya justru **tidak pernah**
 *    divalidasi lint/unit/build. Itu melanggar prinsip "setiap commit yang digabung sudah lulus
 *    CI". Karena itu pembagiannya: `ci.yml` yang **mencegah** (gate `format_teks`), `autofix.yml`
 *    yang **memperbaiki** lewat `workflow_dispatch` + sapuan mingguan.
 * 2. Langkah `softprops/action-gh-release` di `release.yml` **wajib** punya pembatas ref. Action itu
 *    menormalkan `github.ref_name` menjadi nama tag, jadi tanpa pembatas, menjalankan alur rilis
 *    dari sebuah cabang (mis. untuk menguji build rilis) akan mencoba menerbitkan rilis bernama
 *    cabang itu.
 */
class AlurKerjaRilisDanAutofixTest {

    private fun berkasRepo(path: String): File {
        val kandidat = listOf(File("."), File("app"), File("..")).map { File(it, path) }
        return kandidat.firstOrNull { it.isFile }
            ?: error("$path tidak ditemukan dari ${File(".").absolutePath}")
    }

    /**
     * Isi berkas tanpa baris komentar. Komentar di `autofix.yml` sengaja menjelaskan mengapa
     * `pull_request` **tidak** dipakai — kata itu ada di sana, jadi memeriksa teks mentah akan
     * salah membaca alasan sebagai pelanggaran.
     */
    private fun tanpaKomentar(teks: String): String =
        teks.lines().filterNot { it.trimStart().startsWith("#") }.joinToString("\n")

    private fun autofix(): String = berkasRepo(".github/workflows/autofix.yml").readText()
    private fun release(): String = berkasRepo(".github/workflows/release.yml").readText()
    private fun ci(): String = berkasRepo(".github/workflows/ci.yml").readText()

    /** Blok `on:` sebuah alur kerja, dari `on:` sampai `permissions:`/`jobs:` pertama. */
    private fun pemicu(teks: String): String {
        val mulai = Regex("(?m)^on:\\s*$").find(teks)?.range?.last
            ?: error("blok 'on:' tidak ditemukan")
        val sisa = teks.substring(mulai + 1)
        val akhir = listOf("permissions:", "jobs:")
            .mapNotNull { Regex("(?m)^$it").find(sisa)?.range?.first }
            .minOrNull() ?: sisa.length
        return sisa.substring(0, akhir)
    }

    @Test
    fun autofixTidakDipicuPullRequest() {
        val p = pemicu(tanpaKomentar(autofix()))
        assertTrue("autofix.yml kehilangan pemicu workflow_dispatch", p.contains("workflow_dispatch:"))
        assertFalse(
            "autofix.yml dipicu pull_request: commit bot ke cabang PR berhenti di action_required " +
                "sehingga perbaikannya tak tervalidasi CI — lihat KDoc kelas ini",
            p.contains("pull_request"),
        )
    }

    @Test
    fun autofixPunyaJalanPerbaikanYangDisengaja() {
        val p = pemicu(tanpaKomentar(autofix()))
        assertTrue("autofix.yml kehilangan pemicu workflow_dispatch", p.contains("workflow_dispatch:"))
        assertTrue("autofix.yml kehilangan sapuan terjadwal sebagai jaring pengaman main",
            p.contains("schedule:") && p.contains("cron:"))
    }

    @Test
    fun autofixHanyaCommitBilaAdaPerubahan() {
        val src = autofix()
        assertTrue(
            "autofix.yml tidak menjaga diri dari commit kosong — periksa `git diff --quiet` lebih dulu",
            src.contains("git diff --quiet"),
        )
        assertTrue("autofix.yml tidak diberi izin menulis", src.contains("contents: write"))
    }

    @Test
    fun autofixMerapikanLaluMemeriksaUlang() {
        val src = autofix()
        val rapikan = src.indexOf("tools/format_teks.py --fix")
        val periksa = src.indexOf("python3 tools/format_teks.py .")
        assertTrue("autofix.yml tidak menjalankan pemasang format_teks --fix", rapikan >= 0)
        assertTrue(
            "autofix.yml tidak memeriksa ulang hasil perapian (periksa harus setelah rapikan)",
            periksa > rapikan,
        )
    }

    /** Alasan keputusan harus ikut bertahan; kalau komentarnya hilang, jebakannya mudah terulang. */
    @Test
    fun alasanKeputusanTerdokumentasi() {
        val komentar = autofix()
        assertTrue("autofix.yml tak lagi menjelaskan jebakan action_required",
            komentar.contains("action_required"))
        assertTrue("autofix.yml tak lagi menjelaskan peran GITHUB_TOKEN",
            komentar.contains("GITHUB_TOKEN"))
    }

    @Test
    fun langkahRilisDibatasiRef() {
        val src = tanpaKomentar(release())
        assertTrue(
            "langkah action-gh-release di release.yml tidak dibatasi ref — dispatch dari cabang " +
                "akan mencoba menerbitkan rilis bernama cabang itu",
            src.contains("github.ref_type == 'tag'"),
        )
    }

    @Test
    fun dispatchRilisPunyaMasukanTag() {
        val p = pemicu(tanpaKomentar(release()))
        assertTrue("release.yml kehilangan pemicu workflow_dispatch", p.contains("workflow_dispatch:"))
        assertTrue("dispatch release.yml tidak punya masukan 'tag' untuk memilih tag rilis",
            Regex("(?m)^\\s+tag:\\s*$").containsMatchIn(p))
    }

    @Test
    fun ciMencegahHigieneKotorMasuk() {
        assertTrue(
            "ci.yml tidak menjalankan gate format_teks — tanpa ini, alur auto-fix kehilangan " +
                "pasangan pencegahnya",
            ci().contains("python3 tools/format_teks.py ."),
        )
    }

    /**
     * Langkah verifikasi tanda tangan **tidak pernah benar-benar berjalan** sampai keystore tersedia:
     * ia terlewat selama `KEYSTORE_BASE64` kosong. Begitu terpasang, ia gagal dengan
     * `Unsupported command: .../build-tools/35.0.0/apksigner` — sebab glob
     * `$ANDROID_HOME/build-tools/<versi>/apksigner` cocok ke **lebih dari satu** versi build-tools di
     * runner, dan glob yang meleset ke banyak berkas dipecah shell menjadi perintah + argumen.
     * Jadi pilih satu berkas secara pasti; jangan pernah kembali ke bentuk glob telanjang itu.
     */
    @Test
    fun apksignerDipilihTunggalBukanLewatGlob() {
        val src = tanpaKomentar(release())
        assertFalse(
            "release.yml memanggil apksigner lewat glob `build-tools/*/apksigner` yang cocok ke " +
                "beberapa versi build-tools — shell memecahnya jadi perintah + argumen dan " +
                "apksigner mengeluh \"Unsupported command\"",
            src.contains("build-tools/*/apksigner verify"),
        )
        assertTrue(
            "release.yml tidak memilih satu apksigner secara pasti (harap `sort -V | tail -1`)",
            src.contains("sort -V") && src.contains("tail -1"),
        )
    }

    /**
     * Langkah itu bernama "bukan debug", tetapi dahulu hanya **mencetak** sertifikat tanpa
     * menyatakan apa pun — APK debug-signing akan lolos dengan tenang. Sekarang ia membandingkan
     * sidik jari penanda tangan APK dengan keystore yang barusan didekode dari secret, sehingga
     * rantai secret -> keystore -> APK dibuktikan, bukan diasumsikan.
     */
    @Test
    fun verifikasiTandaTanganMembandingkanSidikJariKeystore() {
        val src = tanpaKomentar(release())
        assertTrue(
            "sidik jari keystore tidak dihitung dari keystore yang didekode secret (keytool)",
            src.contains("keytool") && src.contains("sidik_jks="),
        )
        assertTrue(
            "sidik jari penanda tangan APK tidak dibaca dari keluaran apksigner — " +
                "memastikan `sidik_apk` benar-benar diisi perintah, bukan dikosongkan",
            src.contains("sidik_apk=\$(grep"),
        )
        assertTrue(
            "perbandingan kedua sidik jari tidak ditegakkan — APK yang bertanda tangan kunci lain " +
                "(termasuk debug) akan lolos dengan tenang",
            src.contains("\"\$sidik_jks\" != \"\$sidik_apk\"") &&
                src.contains("::error::APK tidak bertanda tangan"),
        )
    }
}
