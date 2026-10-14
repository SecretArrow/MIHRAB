package id.or.kemenag.yasintahlil.feature.keterangan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.AtribusiTeks
import id.or.kemenag.yasintahlil.core.data.BerkasKonten
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.designsystem.BagianJudul
import id.or.kemenag.yasintahlil.core.designsystem.EmptyState
import id.or.kemenag.yasintahlil.core.designsystem.NamaAntarmuka
import id.or.kemenag.yasintahlil.core.designsystem.rememberRepo
import id.or.kemenag.yasintahlil.core.sholat.TampilanSholatKhusus
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.PengingatAdzan

/**
 * Layar keterangan (activity terpisah) — rumah baru bagi keterangan panjang yang dahulu menumpuk
 * di badan layar konten.
 *
 * Keterangannya **dibaca ulang dari sumber yang sama** dengan layar asalnya (`ContentRepo` beserta
 * aturan bersamanya), bukan disalin atau dikirim dari layar pembukanya: satu fakta tetap satu
 * rumah, dan memperbarui berkas kontennya ikut memperbarui layar ini tanpa perubahan kode.
 * Yang dioper hanya `asal` — id layar yang keterangannya diminta.
 *
 * Layar ini tidak dibuka langsung dari notifikasi (`Routes.KETERANGAN` bukan anggota
 * `Routes.LANGSUNG`): keterangan hanya bermakna bersama layar asalnya, dan ketukan notifikasi
 * tidak pernah dimaksudkan mendarat di sini.
 */
@Composable
fun KeteranganScreen(asal: String, kembali: () -> Unit) {
    val repo = rememberRepo()
    val nama = remember(asal) { namaAsal(repo, asal) }
    val bagian = remember(asal) { bagian(repo, asal) }

    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = kembali) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = NamaAntarmuka.KEMBALI,
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "${NamaAntarmuka.KETERANGAN} — $nama",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (bagian.isEmpty()) {
            EmptyState(
                "Keterangan tidak tersedia",
                "Buka keterangan dari ikon Info pada layarnya masing-masing.",
            )
        } else {
            bagian.forEach { b ->
                BagianJudul(b.judul)
                Text(
                    b.isi,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

/** Satu bagian keterangan: judul bagian beserta isinya yang utuh. */
private data class BagianKeterangan(val judul: String, val isi: String)

/**
 * Nama layar asal untuk judul — dari kosakata bersama `NamaLayar`, sehingga ejaannya tak dapat
 * berbeda dari nama yang disebut tombol pembukanya. Rincian per-sholat dinamai bundelnya sendiri.
 */
private fun namaAsal(repo: ContentRepo, asal: String): String = when (asal) {
    "yasin" -> NamaLayar.YASIN
    "tahlil" -> NamaLayar.TAHLIL
    "dzikir" -> NamaLayar.DZIKIR
    "sholat" -> NamaLayar.SHOLAT
    "wudhu" -> NamaLayar.WUDHU
    "ghaib" -> NamaLayar.GHAIB
    "jenazah" -> NamaLayar.JENAZAH
    "adzan" -> NamaLayar.ADZAN
    "doa_adzan" -> NamaLayar.DOA_ADZAN
    "iqomah" -> NamaLayar.IQOMAH
    "kiblat" -> NamaLayar.KIBLAT
    "jadwal" -> NamaLayar.JADWAL
    else -> sholatById(repo, asal)?.nama ?: asal
}

/** Entri sholat untuk asal berbentuk `sholat:<id>`, atau null bila bukan bentuk itu. */
private fun sholatById(repo: ContentRepo, asal: String) =
    asal.takeIf { it.startsWith(PREFIX_SHOLAT) }
        ?.removePrefix(PREFIX_SHOLAT)
        ?.let { id -> (repo.sholatWajib() + repo.sholatSunnah()).find { it.id == id } }

private const val PREFIX_SHOLAT = "sholat:"

/**
 * Daftar bagian keterangan untuk satu layar asal — semuanya dibaca dari `repo` (bundel konten dan
 * aturan bersama), sehingga layar ini tidak pernah menyatakan lebih dari yang dicatat sumbernya.
 * Asal yang tak dikenal menghasilkan daftar kosong dan ditangani sebagai keadaan kosong.
 */
private fun bagian(repo: ContentRepo, asal: String): List<BagianKeterangan> = when (asal) {
    "yasin" -> {
        val surah = repo.yasin
        val atribusi = AtribusiTeks.surah(surah)
        buildList {
            if (atribusi.isNotBlank()) add(BagianKeterangan("Asal teks", atribusi))
            add(
                BagianKeterangan(
                    "Cara membaca",
                    "Ketuk \"Ke ayat\" untuk melompat ke nomor ayat; tombol berikon di sebelahnya " +
                        "membuka mode layar penuh yang menyembunyikan bilah bawah dan memperbesar " +
                        "teks Arab agar nyaman dibaca dari jauh. Tekan lama kartu ayat untuk salin, " +
                        "bagikan, memfavoritkan, menandai terakhir dibaca, atau mem-bookmarknya. " +
                        "Baris \"Terakhir dibaca\" di atas daftar sekaligus menjadi tombol lompat " +
                        "ke ayat penanda Anda.",
                ),
            )
            add(
                BagianKeterangan(
                    "Mencari ayat",
                    "Mencari ayat atau bacaan lain dilakukan di layar Pencarian (menu Beranda/Ibadah " +
                        "→ Cari), yang menelusuri seluruh konten — nomor ayat, terjemahan, transliterasi, " +
                        "nama doa, hingga panduan ibadah. Pengaturan tampilan bacaan (Arab/Latin/" +
                        "terjemahan, ukuran, tema halaman, tetap menyala) terpusat di tab Pengaturan.",
                ),
            )
        }
    }

    "tahlil" -> {
        buildList {
            val tradisi = repo.tahlilCatatanTradisi()
            if (tradisi.isNotBlank()) add(BagianKeterangan("Catatan tradisi", tradisi))
            val sumberBacaan = repo.tahlilSumberBacaan()
            if (sumberBacaan.isNotBlank()) add(BagianKeterangan("Sumber bacaan", sumberBacaan))
            val susunan = repo.tahlilSusunan()
            if (susunan.isNotEmpty()) add(
                BagianKeterangan(
                    "Susunan yang tersedia",
                    susunan.joinToString("\n") { s ->
                        "• ${s.nama}" + if (s.sumber.isNotBlank()) " — ${s.sumber}" else ""
                    },
                ),
            )
        }
    }

    "dzikir" -> {
        val kelompok = repo.dzikir()
        buildList {
            if (kelompok.isNotEmpty()) add(
                BagianKeterangan(
                    "Kelompok dzikir",
                    kelompok.joinToString("\n") { g ->
                        "• ${g.judul} (${g.items.size} bacaan)"
                    },
                ),
            )
            add(
                BagianKeterangan(
                    "Penghitung",
                    "Tiap bacaan menyimpan hitungannya sendiri di perangkat — tombol tambah/kurang " +
                        "mengisinya dan progresnya tetap ada setelah layar ditutup. Penghitung " +
                        "ditampilkan untuk semua bacaan, bukan hanya yang diulang, sehingga bacaan " +
                        "sekali baca pun dapat ditandai selesai.",
                ),
            )
        }
    }

    "sholat" -> {
        buildList {
            val panduan = repo.sholatPanduan()
            if (panduan.isNotBlank()) add(BagianKeterangan("Sumber panduan", panduan))
            val mazhab = repo.sholatCatatanMazhab()
            if (mazhab.isNotBlank()) add(BagianKeterangan("Catatan mazhab", mazhab))
            add(
                BagianKeterangan(
                    "Isi panduan",
                    "Tab Wajib memuat lima sholat wajib, tab Sunnah mengelompokkan sholat sunnah " +
                        "menurut waktu pelaksanaan, jumlah rakaat, atau tujuannya, dan tab Bacaan " +
                        "memuat lafaz tiap langkah beserta kapan dibacanya. Tiap entri dapat dibuka " +
                        "ke panduan langkah demi langkahnya.",
                ),
            )
        }
    }

    "wudhu" -> {
        val panduan = repo.panduanBersuci()
        buildList {
            if (panduan.edisi.isNotBlank()) add(BagianKeterangan("Edisi panduan", panduan.edisi))
            if (panduan.catatanMazhab.isNotBlank()) {
                add(BagianKeterangan("Catatan mazhab", panduan.catatanMazhab))
            }
            if (panduan.catatanLafaz.isNotBlank()) {
                add(BagianKeterangan("Catatan lafaz", panduan.catatanLafaz))
            }
        }
    }

    "ghaib", "jenazah" -> bagianSholatKhusus(
        repo,
        if (asal == "ghaib") BerkasKonten.SHOLAT_GHAIB else BerkasKonten.SHOLAT_JENAZAH,
    )

    "adzan" -> {
        buildList {
            add(
                BagianKeterangan(
                    "Jenis suara",
                    "Aplikasi tidak menyertakan audio bawaan karena lisensi: setiap jenis suara " +
                        "adalah berkas pilihan Anda sendiri yang diberi nama. Beberapa boleh " +
                        "disimpan sekaligus dan satu dipilih sebagai yang dipakai pengingat. Nama " +
                        "hanya penanda pilihan Anda.",
                ),
            )
            add(
                BagianKeterangan(
                    "Cara kerja pengingat",
                    "Pengingat dijadwalkan lewat WorkManager (tanpa izin alarm tepat waktu): waktu " +
                        "dapat bergeser beberapa menit saat perangkat tidur (Doze) atau dibatasi " +
                        "optimasi baterai — buka \"Optimasi baterai\" pada layar Adzan agar lebih " +
                        "tepat. Menonaktifkan sebuah waktu mematikan adzan sekaligus pengingatnya.",
                ),
            )
            val catatan = repo.adzanCatatan()
            if (catatan.isNotBlank()) add(BagianKeterangan("Catatan", catatan))
        }
    }

    "doa_adzan" -> {
        buildList {
            add(
                BagianKeterangan(
                    "Bacaan sebelum & sesudah adzan",
                    "Tidak ada satu doa khusus yang wajib tepat sebelum adzan. Yang disyariatkan: " +
                        "menjawab adzan, sholawat, lalu doa setelah adzan — tiap kartu menyebut " +
                        "kapan dibacanya.",
                ),
            )
            add(
                BagianKeterangan(
                    "Pengingat doa setelah adzan",
                    "${PengingatAdzan.DOA_SEBAB} ${PengingatAdzan.DOA_IKUT_SAKLAR} " +
                        PengingatAdzan.DOA_BUKAN_SYARIAT,
                ),
            )
        }
    }

    "iqomah" -> {
        buildList {
            add(
                BagianKeterangan(
                    "Perkiraan iqomah",
                    "Timer iqomah bersifat perkiraan: ${PengingatAdzan.IQOMAH_PERKIRAAN} Timer di " +
                        "layar hanya berjalan selama layarnya terbuka dan bergetar sekali saat " +
                        "waktunya habis; saklar \"Ingatkan saat perkiraan iqomah habis\" " +
                        "menambatkannya ke pengingat masuk waktu supaya tetap mengingatkan walau " +
                        "layar tertutup.",
                ),
            )
            val catatan = repo.iqomahCatatan()
            if (catatan.isNotBlank()) add(BagianKeterangan("Catatan", catatan))
        }
    }

    "kiblat" -> {
        buildList {
            add(
                BagianKeterangan(
                    "Azimut kiblat",
                    "Azimut dihitung di perangkat dari koordinat Ka'bah yang diterbitkan SIHAT " +
                        "Bimas Islam (E-Kiblat); aplikasi tidak menghubungi server. Ketelitiannya " +
                        "bergantung pada ketelitian lokasi dan sensor perangkat Anda.",
                ),
            )
            add(
                BagianKeterangan(
                    "Kompas & peta",
                    "Jarum emas pada kompas menunjuk kiblat dan garis abu menunjuk utara; " +
                        "akurasinya tidak dijamin dan dipengaruhi casing magnetik, benda logam, " +
                        "serta medan magnet di sekitar. Peta skematik di bawahnya bekerja tanpa " +
                        "internet dan menjadi acuan bila perangkat tak punya magnetometer atau " +
                        "kompasnya belum terkalibrasi.",
                ),
            )
            add(
                BagianKeterangan(
                    "Verifikasi tanpa alat",
                    "Rashdul kiblat — dua kali setahun matahari tepat di atas Ka'bah; bayangan " +
                        "benda tegak menunjuk kiblat saat itu. Jadwalnya diumumkan situs SIHAT " +
                        "Kemenag.",
                ),
            )
        }
    }

    "jadwal" -> {
        buildList {
            add(
                BagianKeterangan(
                    "Perhitungan",
                    "Jadwal dihitung di perangkat (algoritma Meeus), bukan diambil dari server — " +
                        "jadi tidak ada cache daring yang bisa basi dan jadwal tetap tersedia " +
                        "tanpa internet.",
                ),
            )
            add(
                BagianKeterangan(
                    "Lokasi & metode",
                    "Jadwal mengikuti kota pilihan, lokasi GPS, metode hisab, mazhab Asar, " +
                        "koreksi menit per waktu, dan zona waktu yang dipilih di layar ini. " +
                        "Saklar \"Ikuti perubahan lokasi\" menghitung ulang jadwal otomatis saat " +
                        "Anda berpindah tempat.",
                ),
            )
        }
    }

    else -> sholatById(repo, asal)?.let { info ->
        buildList {
            if (info.deskripsi.isNotBlank()) add(BagianKeterangan("Deskripsi", info.deskripsi))
            if (info.tujuan.isNotBlank()) add(BagianKeterangan("Tujuan", info.tujuan))
            if (info.ketentuan.isNotEmpty()) {
                add(BagianKeterangan("Ketentuan", info.ketentuan.joinToString("\n") { "• $it" }))
            }
            if (info.niatCatatan.isNotBlank()) {
                add(BagianKeterangan("Catatan niat", info.niatCatatan))
            }
        }
    } ?: emptyList()
}

/**
 * Bagian keterangan bersama dua layar sholat mayit — perangkaiannya dari `TampilanSholatKhusus`,
 * sama dengan yang dipakai layarnya, sehingga ejaan hukum/dalil/syarat tidak dapat berbeda
 * antara ringkasan di layar dan keterangan lengkapnya di sini.
 */
private fun bagianSholatKhusus(repo: ContentRepo, berkas: String): List<BagianKeterangan> {
    val info = repo.sholatKhusus(berkas)
    return buildList {
        if (info.hukum.isNotBlank()) {
            add(BagianKeterangan("Hukum", TampilanSholatKhusus.hukum(info.hukum)))
        }
        if (info.dalil.isNotBlank()) {
            add(BagianKeterangan("Dalil", TampilanSholatKhusus.dalil(info.dalil, info.catatanRujukan)))
        }
        if (info.syarat.isNotEmpty()) {
            add(BagianKeterangan("Syarat", TampilanSholatKhusus.syarat(info.syarat)))
        }
        if (info.catatanMazhab.isNotBlank()) {
            add(BagianKeterangan("Catatan mazhab", TampilanSholatKhusus.catatanMazhab(info.catatanMazhab)))
        }
    }
}
