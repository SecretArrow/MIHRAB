package id.or.kemenag.yasintahlil.feature.sumber

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.util.ContentValidator
import id.or.kemenag.yasintahlil.core.util.NamaLayar

/**
 * Sumber Konten — satu-satunya tempat atribusi sumber dikumpulkan.
 *
 * Dahulu baris "Sumber: …" menempel di setiap kartu bacaan: informasi yang sama berulang di
 * belasan kartu, memakan ruang baca, dan tetap tidak pernah utuh karena tiap kartu hanya tahu
 * sumber entriannya sendiri. Kini kartu bersih dari baris itu dan seluruh asal-usul — teks
 * Qur'an, hadis, susunan tahlil, hingga algoritma perhitungan — terkumpul di sini, dikelompokkan
 * per modul, sehingga satu tempat menjawab "ini dari mana" untuk seluruh aplikasi.
 *
 * Keterangan per-domain (pemilik tiap situs resmi) tetap ada di layar Tentang; layar ini
 * melengkapi dengan penjelasan **per modul**. Daftar domain tidak disalin ke sini — Tentang
 * membacanya langsung dari bundel dan janji "tidak dapat bertambah diam-diam" tinggal di sana.
 */
@Composable
fun SumberScreen() {
    val kelompok = listOf(
        "Al-Qur'an & terjemahan" to listOf(
            "Teks Arab: mushaf standar MSI LPMQ Kemenag — Surat Yasin, Al-Fatihah, Al-Ikhlas, " +
                "Al-Falaq, An-Nas, dan Ayat Kursi.",
            "Terjemahan resmi Kemenag edisi 2019; Al-Baqarah 284–286 diambil melalui layanan " +
                "web-api.qurankemenag.net.",
            "Transliterasi Latin mengikuti pedoman transliterasi resmi yang sama dengan bundel.",
        ),
        "Tahlil & doa arwah" to listOf(
            "Susunan Tahlil Ringkas (28 bagian) & Lengkap (41 bagian): " +
                "${ContentValidator.SITUS_NU}/tahlil (NU Online) — lafaz berharakat, latin, dan " +
                "terjemahan diekstraksi dari halaman " +
                "resmi dan diverifikasi lintas bundel.",
            "Susunan Buku Imam (22 bagian): rangkaian buku panduan tahlil imam milik jamaah " +
                "(dokumen \"Tahlil dan doa IMAM\", 2026-10-18) — harakat Arab dilengkapi dan " +
                "transliterasi Latin disederhanakan polos dari gaya ilmiah dokumen " +
                "(metadata dokumen: NU Online, ${ContentValidator.SITUS_NU}); " +
                "dokumen tidak menyertakan terjemahan, maka kartunya tampil tanpa terjemahan.",
            "Doa jenazah (takbir 3): HR Muslim 591 — ${ContentValidator.SITUS_HADIS}.",
            "Mode Imam: label siapa membaca adalah panduan umum tradisi tahlil; bacaannya sama " +
                "dengan susunan Lengkap.",
        ),
        "Panduan ibadah" to listOf(
            "Panduan sholat wajib & sunnah, bacaan sholat, wudhu & tayamum: Fasolatan Kemenag.",
            "Sholat jenazah & ghaib beserta doa mayit: bacaan resmi dengan riwayat.",
            "Kalimat adzan, jawaban adzan, iqomah, doa & sholawat setelah adzan: HR " +
                "Bukhari–Muslim.",
            "Dzikir & doa harian: himpunan lafaz berharakat terverifikasi.",
        ),
        "Referensi & perhitungan" to listOf(
            "Asmaul Husna: daftar masyhur (Al-Qushairi; sebagian Tirmidzi).",
            "Jadwal sholat: algoritma posisi matahari Meeus dengan sudut Kemenag/SIHAT " +
                "(Subuh −20°, Isya −18°) — dihitung di perangkat, bukan diambil dari server.",
            "Kalender Hijriah: perhitungan tabular + koreksi pengguna; rujukan resmi " +
                "${ContentValidator.SITUS_SIHAT}/kalender-hijriah.",
            "Pasaran Jawa (Legi–Kliwon): siklus 5 hari, jangkar proklamasi 17 Agustus 1945 = " +
                "Jumat Legi.",
            "Koordinat kota & Ka'bah: SIHAT Kemenag (${ContentValidator.SITUS_SIHAT}).",
        ),
    )
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(NamaLayar.SUMBER, style = MaterialTheme.typography.titleLarge)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Seluruh bacaan disusun dari sumber resmi berikut dan terbawa utuh dalam bundel " +
                    "aplikasi — tidak ada halaman web yang dimuat saat membaca. Pemilik tiap " +
                    "domain resmi tercantum di layar Tentang; berkas konten yang menyebut domain " +
                    "di luar daftar ditolak saat dibangun maupun diperbarui.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }
        kelompok.forEach { (judul, butir) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(judul, style = MaterialTheme.typography.titleSmall)
                    butir.forEach { b ->
                        Row(Modifier.fillMaxWidth()) {
                            Text("• ", style = MaterialTheme.typography.bodySmall)
                            Text(
                                b,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
