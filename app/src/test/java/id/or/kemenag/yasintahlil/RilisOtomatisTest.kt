package id.or.kemenag.yasintahlil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Keputusan-keputusan alur **rilis otomatis** (`auto-release.yml`) dikunci di sini supaya tidak
 * "diperbaiki" keliru oleh orang berikutnya — persis peran `AlurKerjaRilisDanAutofixTest` bagi
 * `release.yml` dan `autofix.yml`.
 *
 * Empat keputusan yang dijaga:
 *
 * 1. **Pemicu `workflow_run` atas alur E2E yang selesai.** Rilis hanya boleh dikerjakan SETELAH
 *    semua verifikasi selesai, dan tidak ada `needs` lintas workflow. Mendengarkan selesainya E2E
 *    (yang lazim paling lambat) memberi titik mulai paling akhir; sisanya ditunggu lewat polling.
 *    Nama alur yang didengarkan harus byte-sama dengan `name:` di `e2e.yml` — bila E2E berganti
 *    nama tanpa ikut memperbarui pemicu ini, `workflow_run` diam-diam mati dan rilis otomatis
 *    berhenti terbit tanpa satu pun galat. Uji pertama menjaga keduanya tetap sejalan.
 * 2. **Orkestrator tipis, bukan salinan build.** Seluruh logika build rilis, dekode keystore,
 *    verifikasi tanda tangan, pembanding versionName-vs-tag, dan penerbitan hidup di
 *    `release.yml` dan sudah dijaga `AlurKerjaRilisDanAutofixTest`. Alur ini hanya memutuskan
 *    KAPAN dan VERSI BERAPA, lalu mem-dispatch `release.yml` pada tag yang barusan dibuatnya.
 * 3. **Dorongan memakai GITHUB_TOKEN.** Peristiwa `push` dari GITHUB_TOKEN tidak memicu workflow
 *    baru (aturan anti-rekursi GitHub) — commit kenaikan versi memang tidak menjalankan CI
 *    sendiri. Itu diterima karena ia hanya mengganti dua angka + changelog, dan pohon hasilnya
 *    tetap dibuktikan oleh build rilis yang dilaunchkan seketika. Konsekuensinya tag yang
 *    didorong GITHUB_TOKEN juga TIDAK memicu `release.yml` — karena itu penerbitan dilaunchkan
 *    lewat `workflow_dispatch`, yang justru TIDAK termasuk peristiwa yang ditekan.
 * 4. **Yang dirilis = yang teruji.** Versi hanya dinaikkan setelah tiga gerbang lolos: SHA belum
 *    pernah dirilis (idempoten), SELURUH run `event=push` untuk SHA itu hijau (bukan hanya E2E
 *    yang memicu), dan main masih tepat di SHA itu (bukan SHA yang lebih baru yang punya
 *    rangkaian pipeline-nya sendiri).
 */
class RilisOtomatisTest {

    private fun berkasRepo(path: String): File {
        val kandidat = listOf(File("."), File("app"), File("..")).map { File(it, path) }
        return kandidat.firstOrNull { it.isFile }
            ?: error("$path tidak ditemukan dari ${File(".").absolutePath}")
    }

    private fun alur(): String = berkasRepo(".github/workflows/auto-release.yml").readText()
    private fun e2e(): String = berkasRepo(".github/workflows/e2e.yml").readText()

    /**
     * Teks **satu langkah** saja, dari `- name: <nama>` sampai `- name:` berikutnya — pola yang
     * sama dengan `AlurKerjaRilisDanAutofixTest`: memeriksa seluruh berkas terlalu longgar karena
     * penanda yang sama bisa terpuaskan oleh langkah lain.
     */
    private fun langkah(teks: String, nama: String): String {
        val mulai = teks.indexOf("- name: $nama")
        require(mulai >= 0) { "langkah '$nama' tidak ditemukan di auto-release.yml" }
        val sisa = teks.substring(mulai)
        val berikut = Regex("(?m)^      - name: ").find(sisa, 1)?.range?.first ?: sisa.length
        return sisa.substring(0, berikut)
    }

    /** Nama alur E2E persis seperti yang didengarkan pemicu `workflow_run`. */
    private fun namaAlurE2e(): String =
        Regex("(?m)^name:\\s*(.+)$").find(e2e())?.groupValues?.get(1)?.trim()
            ?: error("e2e.yml tidak menyatakan nama alur (baris 'name:')")

    @Test
    fun pemicuMendengarkanNamaAlurE2eYangSebenarnya() {
        val nama = namaAlurE2e()
        assertTrue(
            "auto-release.yml tidak lagi mendengarkan nama alur E2E yang sebenarnya ('$nama') — " +
                "pemicu workflow_run akan diam-diam mati dan rilis otomatis berhenti terbit tanpa " +
                "satu pun galat; perbarui daftar 'workflows:' saat E2E berganti nama",
            alur().contains("workflows: ['$nama']"),
        )
    }

    @Test
    fun hanyaSaatE2eSuksesDiMainKarenaPush() {
        val teks = alur()
        listOf(
            "workflow_run.conclusion == 'success'" to "rilis dikerjakan walau E2E gagal",
            "workflow_run.head_branch == 'main'" to "rilis dikerjakan untuk cabang bukan main",
            "workflow_run.event == 'push'" to
                "rilis dikerjakan oleh E2E malam/dispatch — malam tanpa commit baru tak ada yang " +
                "bisa dirilis, dan dispatch E2E adalah uji, bukan persetujuan merilis",
        ).forEach { (penanda, akibat) ->
            assertTrue("hilang: '$penanda' — $akibat", teks.contains(penanda))
        }
    }

    @Test
    fun menungguSeluruhPipelinePushBukanHanyaPemicunya() {
        val blok = langkah(alur(), "Tunggu seluruh pipeline push SHA ini hijau")
        assertTrue(
            "langkah ini tidak memeriksa daftar run untuk SHA terkait (actions/runs?event=push) — " +
                "E2E bisa selesai lebih dulu daripada CI lint+unit+build",
            blok.contains("actions/runs") && blok.contains("event=push"),
        )
        assertTrue(
            "run yang tidak sukses tidak ditolak — rilis boleh terbit di atas pipeline yang merah",
            blok.contains("conclusion != \"success\""),
        )
        assertTrue(
            "kehabisan waktu menunggu tidak dinyatakan merah — SHA yang sebenarnya hijau diam-diam " +
                "tak pernah dirilis tanpa jejak yang bisa ditindak",
            blok.contains("::error::"),
        )
    }

    @Test
    fun shaYangSudahDirilisTidakDirilisLagi() {
        val blok = langkah(alur(), "Gerbang idempoten — SHA ini belum dirilis")
        assertTrue(
            "pemeriksaan 'tag menunjuk persis SHA ini' tidak ada — dispatch ulang akan menerbitkan " +
                "rilis ganda untuk SHA yang sama",
            blok.contains("git tag --points-at"),
        )
        assertTrue(
            "pemeriksaan 'SHA sudah tercakup rilis yang lebih baru' tidak ada — SHA lama yang " +
                "sudah tercakup tag terbaru akan naik versi dua kali",
            blok.contains("merge-base --is-ancestor"),
        )
    }

    @Test
    fun yangDirilisAdalahYangTerujiDanTermutakhir() {
        val blok = langkah(alur(), "Gerbang main — masih di SHA yang teruji")
        assertTrue(
            "posisi main tidak diperiksa ulang setelah menunggu — main yang sudah maju berarti SHA " +
                "teruji itu tertinggal, dan SHA terbaru-lah yang pantas dirilis oleh run-nya sendiri",
            blok.contains("rev-parse origin/main"),
        )
    }

    @Test
    fun urutanGerbangLaluVersiLaluTagLaluDispatchTerjaga() {
        val teks = alur()
        val urutan = listOf(
            "Gerbang idempoten — SHA ini belum dirilis",
            "Tunggu seluruh pipeline push SHA ini hijau",
            "Gerbang main — masih di SHA yang teruji",
            "Naikkan versi (semver + versionCode) dan tulis changelog",
            "Commit dan dorong kenaikan versi ke main",
            "Tandai commit kenaikan versi dengan tag rilis",
            "Launchkan release.yml pada tag — build, tanda tangan, terbit",
            "Tunggu rilis benar-benar terbit — status run disalin",
        )
        urutan.forEach { nama ->
            assertTrue(
                "langkah '$nama' tidak ditemukan di auto-release.yml — alur rilis berubah bentuk; " +
                    "perbarui uji ini bersama alurnya agar keduanya tidak saling menipu",
                teks.indexOf("- name: $nama") >= 0,
            )
        }
        val posisi = urutan.map { teks.indexOf("- name: $it") }
        assertTrue(
            "urutan langkah rilis berubah: $posisi — gerbang wajib mendahului kenaikan versi, " +
                "kenaikan versi mendahului tag, dan tag mendahului dispatch",
            posisi.zipWithNext().all { (awal, akhir) -> awal < akhir },
        )
    }

    @Test
    fun versiDinaikkanOlehAlatSumberTunggal() {
        val blok = langkah(alur(), "Naikkan versi (semver + versionCode) dan tulis changelog")
        assertTrue(
            "versi tidak dinaikkan lewat tools/naik_versi.py — angka yang diketik ulang di " +
                "workflow menjadi salinan kedua yang pasti menyimpang dari sumber tunggalnya",
            blok.contains("tools/naik_versi.py"),
        )
        val alat = berkasRepo("tools/naik_versi.py")
        val teks = alat.readText()
        assertTrue(
            "naik_versi.py tidak lagi membaca versionName/versionCode dari app/build.gradle.kts",
            teks.contains("versionName") && teks.contains("versionCode"),
        )
        assertTrue(
            "naik_versi.py tidak lagi memindahkan bagian [Unreleased] CHANGELOG.md — catatan yang " +
                "sengaja ditulis untuk rilis berikutnya akan hilang diam-diam",
            teks.contains("Unreleased"),
        )
        assertTrue(
            "naik_versi.py tidak lagi menolak sumber ganda ('tepat satu') — baris versi kedua di " +
                "build.gradle.kts akan dinaikkan mana-mana tanpa diperiksa",
            teks.contains("tepat satu"),
        )
    }

    @Test
    fun tabrakanTagDitolakKeras() {
        val blok = langkah(alur(), "Naikkan versi (semver + versionCode) dan tulis changelog")
        assertTrue(
            "pemeriksaan tabrakan tag tidak ada — dua rangkaian rilis yang berdekatan bisa " +
                "menurunkan versi yang sama dan saling menimpa",
            blok.contains("refs/tags/v\$VERSI") && blok.contains("::error::tag"),
        )
    }

    @Test
    fun tidakMenggandakanBuildRilis() {
        val teks = alur()
        listOf("assembleRelease", "bundleRelease", "KEYSTORE_BASE64", "keystore.properties").forEach { penanda ->
            assertFalse(
                "auto-release.yml memuat '$penanda' — build, tanda tangan, dan penerbitan hidup " +
                    "di release.yml dan dijaga AlurKerjaRilisDanAutofixTest; menggandakannya di sini " +
                    "membuat dua salinan yang bisa diam-diam menyimpang",
                teks.contains(penanda),
            )
        }
    }

    @Test
    fun penerbitanTetapHidupDiReleaseYml() {
        val blok = langkah(alur(), "Launchkan release.yml pada tag — build, tanda tangan, terbit")
        assertTrue(
            "rilis tidak dilaunchkan lewat dispatch release.yml — CARA membangun dan menerbitkan " +
                "harus tetap satu tempat yang sudah teruji",
            blok.contains("release.yml/dispatches"),
        )
        assertTrue(
            "dispatch tidak memakai tag sebagai ref — tanpa ref tag, release.yml checkout main " +
                "(yang bisa sudah maju) dan pembatas ref_type-nya tak berlaku",
            blok.contains("ref=v\$VERSI"),
        )
    }

    @Test
    fun hanyaTokenBawaanBukanSecretPribadi() {
        val rahasia = Regex("secrets\\.[A-Za-z_]+").findAll(alur()).map { it.value }.toList()
        assertTrue(
            "alur ini memang membutuhkan token bawaan (dispatch API dan dorongan git) — bila " +
                "pemakaiannya hilang, periksa ulang langkah mana yang kehilangan autentikasinya",
            rahasia.isNotEmpty(),
        )
        val asing = rahasia.filter { it != "secrets.GITHUB_TOKEN" }
        assertTrue(
            "auto-release.yml memakai secret selain token bawaan: $asing — PAT tidak disimpan di " +
                "repo ini (lihat komentar autofix.yml: aktor bot bukan kolaborator, run-nya berhenti " +
                "di action_required)",
            asing.isEmpty(),
        )
    }

    @Test
    fun antreTanpaMembatalkan() {
        val teks = alur()
        assertTrue(
            "rilis otomatis harus mengantre (cancel-in-progress: false) — membatalkan yang sedang " +
                "berjalan meninggalkan kenaikan versi setengah jadi di main",
            teks.contains("group: rilis-otomatis") && teks.contains("cancel-in-progress: false"),
        )
    }

    @Test
    fun statusRilisDisalinBukanDiasumsikan() {
        val blok = langkah(alur(), "Tunggu rilis benar-benar terbit — status run disalin")
        assertTrue(
            "status run rilis yang dilaunchkan tidak diikuti — tanda hijau alur ini menjadi " +
                "bohong bila build rilis sebenarnya gagal",
            blok.contains("event=workflow_dispatch") && blok.contains("::error::release.yml berakhir"),
        )
    }

    /** Alasan yang tidak tertulis akan dianggap berlebihan lalu dihapus; setelah itu jebakannya terulang. */
    @Test
    fun alasanKeputusanTerdokumentasi() {
        val teks = alur()
        assertTrue(
            "auto-release.yml tak lagi menjelaskan peran GITHUB_TOKEN (penekanan peristiwa push " +
                "dan pengecualiannya) — orang berikutnya akan 'memperbaiki' alur ini keliru",
            teks.contains("GITHUB_TOKEN"),
        )
        assertTrue(
            "auto-release.yml tak lagi menjelaskan pengecualian workflow_dispatch — tanpa itu, " +
                "alasan dispatch (bukan memicu release.yml lewat push tag) tak terlihat",
            teks.contains("workflow_dispatch"),
        )
        assertTrue(
            "auto-release.yml tak lagi menyebut AlurKerjaRilisDanAutofixTest — alasan memakai " +
                "release.yml alih-alih membangun sendiri hilang",
            teks.contains("AlurKerjaRilisDanAutofixTest"),
        )
    }
}
