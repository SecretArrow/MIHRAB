package id.or.kemenag.yasintahlil.core.legal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pengurai Markdown dipakai untuk menampilkan dokumen legal di layar Tentang. Ujinya memakai
 * **dokumen sungguhan** dari `docs/`, bukan contoh yang ditulis khusus: yang penting bukan apakah
 * pengurainya benar secara umum, melainkan apakah berkas yang benar-benar dibundel terbaca utuh —
 * judul, butir, paragraf, dan tabel izinnya.
 */
class MarkdownRingkasTest {

    private fun modul(): File = listOf(File("."), File("app")).first { File(it, "src/main").isDirectory }

    private fun repo(): File = listOf(File(".."), File(".")).first { File(it, "docs/PRIVACY.md").isFile }

    private fun berkas(relatif: String): String = File(repo(), relatif).readText()

    private fun teks(b: Blok): String = when (b) {
        is Blok.Judul -> b.teks
        is Blok.Butir -> b.teks
        is Blok.Paragraf -> b.teks
        is Blok.Tabel -> (b.judul + b.baris.flatten()).joinToString(" ")
    }

    @Test
    fun judulDikenali() {
        val b = MarkdownRingkas.blok("# Judul\n\n## Bagian\n\n### Anak")
        assertEquals(
            listOf(Blok.Judul(1, "Judul"), Blok.Judul(2, "Bagian"), Blok.Judul(3, "Anak")),
            b,
        )
    }

    /** Dokumen dibungkus per baris: baris lanjutan harus ikut bloknya, bukan jadi blok sendiri. */
    @Test
    fun butirDanParagrafDisatukanDenganBarisLanjutannya() {
        val b = MarkdownRingkas.blok("- satu\n  lanjutan\n\nteks\n  masih teks")
        assertEquals(
            listOf(Blok.Butir("satu lanjutan"), Blok.Paragraf("teks masih teks")),
            b,
        )
    }

    /** Butir berurutan tanpa baris kosong tetap butir terpisah — kalau tidak, isinya menyatu. */
    @Test
    fun butirBerurutanTetapTerpisah() {
        val b = MarkdownRingkas.blok("- satu\n  lanjutan\n- dua\n- tiga\n  lanjutan tiga")
        assertEquals(
            listOf(Blok.Butir("satu lanjutan"), Blok.Butir("dua"), Blok.Butir("tiga lanjutan tiga")),
            b,
        )
    }

    @Test
    fun penandaMarkdownDibuang() {
        assertEquals(
            "tebal dan miring dan PERMISSIONS.md",
            MarkdownRingkas.rapi("**tebal** dan *miring* dan [`PERMISSIONS.md`](PERMISSIONS.md)"),
        )
    }

    @Test
    fun tabelDikenaliDanBarisPemisahDibuang() {
        val b = MarkdownRingkas.blok("| A | B |\n|---|---|\n| 1 | 2 |\n| 3 | 4 |")
        val t = b.single() as Blok.Tabel
        assertEquals(listOf("A", "B"), t.judul)
        assertEquals(listOf(listOf("1", "2"), listOf("3", "4")), t.baris)
    }

    @Test
    fun kebijakanPrivasiSungguhanTeruraiUtuh() {
        val b = MarkdownRingkas.blok(berkas("docs/PRIVACY.md"))
        assertEquals(Blok.Judul(1, "Kebijakan Privasi (Privacy Policy)"), b.first())
        assertEquals(
            listOf(
                "Kebijakan Privasi (Privacy Policy)", "Ringkasan", "Data yang disimpan di perangkat",
                "Izin", "Pihak ketiga", "Konten keagamaan", "Kontak",
            ),
            b.filterIsInstance<Blok.Judul>().map { it.teks },
        )
        // Dua butir data tersimpan + tiga butir izin.
        assertEquals(5, b.filterIsInstance<Blok.Butir>().size)
        assertTrue("tidak ada paragraf yang terbaca", b.filterIsInstance<Blok.Paragraf>().size >= 5)
    }

    @Test
    fun tidakAdaPenandaMarkdownYangLolosKeLayar() {
        listOf("docs/PRIVACY.md", "docs/PERMISSIONS.md").forEach { nama ->
            val gabung = MarkdownRingkas.blok(berkas(nama)).joinToString(" ") { teks(it) }
            listOf("**", "`", "](", "##", "|").forEach { penanda ->
                assertTrue("penanda '$penanda' lolos dari $nama", !gabung.contains(penanda))
            }
        }
    }

    /** Tabel izin harus terbaca per baris — di layar ponsel empat kolom tidak muat. */
    @Test
    fun tabelIzinSungguhanTerbaca() {
        val b = MarkdownRingkas.blok(berkas("docs/PERMISSIONS.md"))
        val t = b.filterIsInstance<Blok.Tabel>().single()
        assertEquals(listOf("Izin", "Kapan diminta", "Alasan", "Bila ditolak"), t.judul)
        assertEquals(7, t.baris.size)
        assertTrue("ada baris tabel yang tidak sepanjang kepalanya",
            t.baris.all { it.size == t.judul.size })
        assertTrue("izin lokasi tidak menyebut kiblat pada alasannya",
            t.baris.first()[2].contains("kiblat"))
        assertTrue("ada sel izin yang kosong", t.baris.all { it[0].isNotBlank() })
    }

    /**
     * Setiap izin yang didokumentasikan harus benar-benar dideklarasikan. Dokumentasi izin yang
     * menyebut izin yang tidak ada — atau sebaliknya — membuat penjelasan di layar Tentang
     * menyesatkan pembaca dan peninjau toko aplikasi.
     */
    @Test
    fun setiapIzinDiDokumenDideklarasikanDiManifest() {
        val manifest = File(modul(), "src/main/AndroidManifest.xml").readText()
        val b = MarkdownRingkas.blok(berkas("docs/PERMISSIONS.md"))
        val t = b.filterIsInstance<Blok.Tabel>().single()
        val nama = t.baris.flatMap { Regex("""[A-Z][A-Z_]{3,}""").findAll(it[0]).map { m -> m.value } }
        assertTrue("nama izin tidak terbaca dari tabel", nama.size >= 7)
        val tidakAda = nama.filterNot { manifest.contains(it) }
        assertTrue("izin didokumentasikan tetapi tidak ada di manifest: $tidakAda", tidakAda.isEmpty())
    }
}
