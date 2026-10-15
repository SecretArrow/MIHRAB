package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.core.audio.TtsBacakan
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Patokan bagian murni dua berkas baru aksi lembar kartu bacaan — "Bagikan sebagai gambar"
 * ([BagikanGambar]) dan Mode Bacakan ([TtsBacakan]): penamaan berkas PNG, aturan pembersihan
 * cache, kaki gambar, pengubahan saluran alfa warna, serta susunan teks yang dibacakan TTS.
 *
 * Merender bitmap dan menjalankan mesin TTS hanya mungkin di Android; aturan-aturan di atas
 * karena itu dipisah sebagai fungsi murni agar dipakukan di sini, gaya `ZakatHitungTest` —
 * salah satu angka atau ejaannya bergeser pun langsung ketahuan sebelum terbit. Susunan teks
 * TTS diuji dari berkas ini juga karena ia bagian murni tanpa Android (mesinnya sendiri tidak
 * bisa diuji di JVM).
 */
class BagikanGambarTest {

    @Test
    fun namaBerkasMenempelStempelDanAkhiranPng() {
        assertEquals("kartu_1728999000123.png", BagikanGambar.namaBerkas(1728999000123L))
        assertEquals("kartu_0.png", BagikanGambar.namaBerkas(0L))
    }

    @Test
    fun pembersihanMenyimpanYangTermuda() {
        val entri = listOf(
            "kartu_1.png" to 10L,
            "kartu_2.png" to 30L,
            "kartu_3.png" to 20L,
            "kartu_4.png" to 40L,
        )
        // Dua yang termuda: stempel 40 lalu 30 — sisanya boleh dibuang.
        assertEquals(
            setOf("kartu_4.png", "kartu_2.png"),
            BagikanGambar.pilihYangDisimpan(entri, 2),
        )
    }

    @Test
    fun pembersihanMenyimpanSemuaSaatJumlahnyaKurangDariBatas() {
        val entri = listOf("kartu_1.png" to 10L, "kartu_2.png" to 20L)
        assertEquals(
            setOf("kartu_1.png", "kartu_2.png"),
            BagikanGambar.pilihYangDisimpan(entri, 8),
        )
    }

    @Test
    fun pembersihanBatasNolMembuangSemua() {
        val entri = listOf("kartu_1.png" to 10L, "kartu_2.png" to 20L)
        assertEquals(emptySet<String>(), BagikanGambar.pilihYangDisimpan(entri, 0))
    }

    @Test
    fun pembersihanSeriStempelDitetapkanUrutanDaftar() {
        // Stempel sama (milidetik yang sama) harus tetap deterministik: urutan stabil
        // pengurutan menempatkan yang lebih dulu di daftar sebagai "termuda".
        val seri = listOf("kartu_a.png" to 5L, "kartu_b.png" to 5L)
        assertEquals(setOf("kartu_a.png"), BagikanGambar.pilihYangDisimpan(seri, 1))
    }

    @Test
    fun footerMemuatNamaAplikasiDanTanggal() {
        assertEquals(
            "Yasin dan Tahlil · 15 Oktober 2026",
            BagikanGambar.susunFooter("15 Oktober 2026"),
        )
    }

    @Test
    fun alfaMenggantiSaluranAlfaSaja() {
        val tinta = 0xFF1D1B15.toInt()
        // Saluran RGB tak berubah — teks pudar tetap sekeluarga warna kertasnya.
        assertEquals(0xCC1D1B15.toInt(), BagikanGambar.denganAlpha(tinta, 0xCC))
        assertEquals(0x991D1B15.toInt(), BagikanGambar.denganAlpha(tinta, 0x99))
    }

    @Test
    fun alfaDijepitKeRentangByte() {
        val tinta = 0xFF1D1B15.toInt()
        assertEquals(0x001D1B15, BagikanGambar.denganAlpha(tinta, 0))
        assertEquals(0x001D1B15, BagikanGambar.denganAlpha(tinta, -7))
        assertEquals(0xFF1D1B15.toInt(), BagikanGambar.denganAlpha(tinta, 300))
    }

    @Test
    fun teksBacakanMenyantumkanJudulLatinLaluTerjemahan() {
        assertEquals(
            "Surah Al-Fatihah: 1. Bismillahirrahmanirrahim. Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.",
            TtsBacakan.susunTeksBacaan(
                "Surah Al-Fatihah: 1",
                "Bismillahirrahmanirrahim",
                "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.",
            ),
        )
    }

    @Test
    fun teksBacakanMelompatiBagianKosong() {
        // Kartu tanpa latin: judul langsung disambung terjemahan, tanpa titik menggantung.
        assertEquals(
            "Doa Selepas Sholat. Allahumma antas salam",
            TtsBacakan.susunTeksBacaan("Doa Selepas Sholat", "  ", "Allahumma antas salam"),
        )
        assertEquals(
            "Basmalah",
            TtsBacakan.susunTeksBacaan("Basmalah", "", ""),
        )
    }
}
