package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Bacaan
import id.or.kemenag.yasintahlil.core.data.BerkasKonten
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.data.TeksBacaan
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.sholat.CaraKelompok
import id.or.kemenag.yasintahlil.core.sholat.KelompokSholat
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.Routes

/**
 * Menu Panduan Sholat — bentuknya sama dengan menu Ibadah: deretan baris berikon yang
 * dikelompokkan menurut maksud kunjungannya (sholat wajib, sholat sunnah, bacaan).
 *
 * Dahulu layar ini memilah isinya lewat tiga tab ("Wajib"/"Sunnah"/"Bacaan") berikut kotak
 * penyaringnya. Permintaan pengguna (2026-10-15): tampilan setelah menu dibuka harus sama
 * seperti menu Ibadah, bukan tabs — dan berlaku ke semua yang serupa. Maka ketiga tab itu
 * kini menjadi bagian pada satu halaman yang dapat digulir; rincian tiap sholat dan tiap
 * bacaan tetap tinggal di layarnya masing-masing.
 *
 * Mencari entri tertentu bukan lagi tugas layar ini: layar Pencarian menelusuri seluruh
 * daftar sholat dan bacaannya sekaligus, lintas kelompok.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SholatScreen(go: (String) -> Unit) {
    val repo = rememberRepo()
    val store = rememberStore()
    val wajib = remember { repo.sholatWajib() }
    val sunnah = remember { repo.sholatSunnah() }
    val bacaan = remember { repo.sholatBacaan() }
    // rememberSaveable: cara pengelompokan pilihan pengguna tidak hilang saat diputar ulang.
    var cara by rememberSaveable { mutableStateOf(CaraKelompok.WAKTU) }
    // Preferensi bacaan dibaca untuk kartu bacaan yang dibuka dari lembar bawah; pengaturannya
    // sendiri terpusat di tab Pengaturan — layar ini hanya membacanya.
    var showArab by remember { mutableStateOf(BawaanPreferensi.TAMPIL_ARAB) }
    var showLatin by remember { mutableStateOf(BawaanPreferensi.TAMPIL_LATIN) }
    var showTrans by remember { mutableStateOf(BawaanPreferensi.TAMPIL_TERJEMAHAN) }
    var arabSize by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    LaunchedEffect(Unit) {
        showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB)
        showLatin = store.get(Store.SHOW_LATIN, BawaanPreferensi.TAMPIL_LATIN)
        showTrans = store.get(Store.SHOW_TRANS, BawaanPreferensi.TAMPIL_TERJEMAHAN)
        arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
    }
    // Sumber panduan dipadatkan ke kartu keterangan: barisnya tetap dibaca dari bundel
    // (`sholat_daftar.json` → `panduan`) dan tetap tampil sekilas, sementara catatan mazhab
    // modul dan penjelas isi panduan dibuka lewat ikon Info.
    val panduan = remember { repo.sholatPanduan() }
    val kelompokSunnah = remember(sunnah, cara) { KelompokSholat.kelompok(sunnah, cara) }
    // Bacaan yang dibuka pengguna dari lembar bawah. `remember` biasa cukup: panel memang wajar
    // tertutup saat perangkat diputar — tidak ada yang hilang.
    var bukaBacaan by remember { mutableStateOf<Bacaan?>(null) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(NamaLayar.SHOLAT, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("sholat")) }
        }
        if (panduan.isNotBlank()) {
            KartuKeterangan(
                onInfo = { go(Routes.keterangan("sholat")) },
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text("Sumber panduan: $panduan", style = MaterialTheme.typography.labelMedium)
            }
        }

        BagianJudul("Sholat wajib")
        wajib.forEach { s ->
            // Entri yang punya modul sendiri (Ghaib, Jenazah) dikenali dari daftar bersama
            // `BerkasKonten.SHOLAT_KHUSUS` — daftar itu memuat id entrinya di `sholat_daftar.json`,
            // rutenya, dan nama layarnya. Sebelumnya id-nya dieja di sini (`s.id == "ghaib"`), dan
            // tak ada apa pun yang membandingkannya dengan bundel: mengganti id di sana menghapus
            // pintu ini diam-diam, dan entri itu jatuh ke halaman rincian tanpa isi.
            val modul = BerkasKonten.SHOLAT_KHUSUS.firstOrNull { it.idDaftar == s.id }
            MenuBaris(
                s.nama,
                // Ikonya tetap dari rute modulnya, bukan dari id entrinya: rute sudah dibaca dari
                // daftar bersama, jadi ikon tidak menambah ejaan id yang dapat basi.
                when (modul?.rute) {
                    Routes.GHAIB -> Icons.Filled.PersonOff
                    Routes.JENAZAH -> Icons.Filled.Groups
                    else -> Icons.Filled.Schedule
                },
                onClick = { go(if (modul != null) modul.rute else "${Routes.SHOLAT_DETAIL}/${s.id}") },
                keterangan = daftarKeterangan(s.rakaat, s.waktu),
            )
        }

        BagianJudul("Sholat sunnah")
        // `plan.txt` §"Sholat Sunnah": "Kelompokkan sholat berdasarkan waktu pelaksanaan, jumlah
        // rakaat, dan tujuan." Ketiganya ditawarkan karena satu cara pengelompokan tidak menjawab
        // semua pertanyaan: "mana yang dikerjakan malam ini" berbeda dari "mana yang tujuannya
        // memohon ampunan".
        BarisChip {
            Text("Kelompok:", style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterVertically))
            CaraKelompok.entries.forEach { c ->
                FilterChip(cara == c, { cara = c }, { Text(c.label) })
            }
        }
        kelompokSunnah.forEach { (nama, isi) ->
            Text("$nama (${isi.size})", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp))
            isi.forEach { s ->
                MenuBaris(
                    s.nama,
                    Icons.Filled.SelfImprovement,
                    onClick = { go("${Routes.SHOLAT_DETAIL}/${s.id}") },
                    keterangan = daftarKeterangan(s.rakaat, s.waktu),
                )
            }
        }

        BagianJudul("Bacaan sholat")
        bacaan.forEach { b ->
            MenuBaris(
                b.judul,
                Icons.AutoMirrored.Filled.MenuBook,
                onClick = { bukaBacaan = b },
                keterangan = b.kapan.takeIf { it.isNotBlank() },
            )
        }
    }

    // Bacaan dibuka sebagai lembar bawah, bukan tab: isinya utuh di satu tempat (Arab,
    // transliterasi, terjemahan, sumber) dan tetap dapat ditekan-lama untuk salin/bagikan/favorit
    // seperti kartu bacaan di layar lain.
    bukaBacaan?.let { b ->
        ModalBottomSheet(onDismissRequest = { bukaBacaan = null },
            sheetState = rememberModalBottomSheetState()) {
            Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 28.dp)) {
                // Aturan cadangan teks dari rujukan bersama, dengan cadangan medan entri sendiri —
                // aturannya satu tempat (`TeksBacaan`), sama dengan layar sholat mayit, rincian
                // langkah, dan Tahlil.
                val (ra, rl, rt) = repo.resolveArabLatinTerjemah(b.rujukanFile)
                val (a, l, t) = TeksBacaan.pilih(b, ra, rl, rt)
                BacaanCard(JenisBacaan.id(JenisBacaan.BACAAN, b.id), b.judul, a, l, t,
                    showLatin, showTrans, arabSize,
                    sumber = b.sumber.ifBlank { b.kapan },
                    showArab = showArab, kategori = b.kategori)
                if (b.catatan.isNotBlank()) {
                    Text(b.catatan, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** Baris keterangan singkat untuk menu: "2 rakaat • Subuh" — kosong bila keduanya tak terisi. */
private fun daftarKeterangan(rakaat: String, waktu: String): String? =
    listOf(rakaat.takeIf { it.isNotBlank() }, waktu.takeIf { it.isNotBlank() })
        .filterNotNull().joinToString(" • ").ifBlank { null }
