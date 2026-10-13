package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.Bacaan
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.BerkasKonten
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.data.TeksBacaan
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.sholat.BacaanSholat
import id.or.kemenag.yasintahlil.core.sholat.TampilanSholatKhusus
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import kotlinx.coroutines.launch

/**
 * Judul langkah takbir yang khas layar ini — sholat jenazah hadir tidak membaca ta'awudz dan
 * salamnya tanpa keterangan arah. Takbir 2–4 menyebut bacaan yang sama dengan layar Ghaib, jadi
 * ejaannya tinggal di [TampilanSholatKhusus.judulTakbirBersama] dan digabung lewat
 * [TampilanSholatKhusus.judulLangkah]; di sini hanya yang benar-benar khas.
 *
 * Kuncinya adalah id bacaan dari [BacaanSholat.TAKBIR_MAYIT], jadi setiap id pada urutan bersama
 * itu pasti punya judul — penjaganya memaku cakupan itu sehingga `getValue` tidak pernah gagal.
 */
private val judulLangkah: Map<String, String> = TampilanSholatKhusus.judulLangkah(
    mapOf(
        BacaanSholat.FATIHAH to "Takbir 1 → Al-Fatihah",
        BacaanSholat.SALAM to "Salam",
    ),
)

/**
 * Sholat jenazah hadir: 4 takbir tanpa ruku/sujud, fardhu kifayah. Konten (niat & doa)
 * dibaca dari assets ([BerkasKonten.SHOLAT_JENAZAH] + bacaan bersama) — tanpa teks Arab di Kotlin.
 *
 * Perangkaian teksnya — judul, syarat, dalil, catatan mazhab, langkah, dan cadangan teks bacaan —
 * datang dari [TampilanSholatKhusus], sama dengan layar Ghaib. Yang khas layar ini hanya
 * [judulLangkah] dan label chip layar penuhnya.
 */
@Composable
fun JenazahScreen() {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    // rememberSaveable: mode layar penuh pilihan pengguna tidak hilang saat layar diputar ulang.
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    var showArab by remember { mutableStateOf(BawaanPreferensi.TAMPIL_ARAB) }
    var showLatin by remember { mutableStateOf(BawaanPreferensi.TAMPIL_LATIN) }
    var showTrans by remember { mutableStateOf(BawaanPreferensi.TAMPIL_TERJEMAHAN) }
    var settings by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
        showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB)
        showLatin = store.get(Store.SHOW_LATIN, BawaanPreferensi.TAMPIL_LATIN)
        showTrans = store.get(Store.SHOW_TRANS, BawaanPreferensi.TAMPIL_TERJEMAHAN)
    }
    FullscreenEffect(fullscreen)
    val info = remember { repo.sholatKhusus(BerkasKonten.SHOLAT_JENAZAH) }
    val bacaan = remember { repo.sholatBacaan() + repo.sholatMayitBacaan() }
    fun cari(id: String) = bacaan.find { it.id == id }
    val steps: List<Pair<String, Bacaan?>> =
        // Urutan bacaan takbir datang dari `BacaanSholat.TAKBIR_MAYIT`; yang khas layar ini hanya
        // judulnya. Sebelumnya kelima idnya diketik ulang di sini, sehingga satu salah eja cukup
        // untuk menghilangkan satu takbir — kartunya tak muncul, judulnya tetap ada.
        BacaanSholat.TAKBIR_MAYIT.map { judulLangkah.getValue(it) to cari(it) }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text(TampilanSholatKhusus.judul(info.judul.ifBlank { NamaLayar.JENAZAH }, info.takbir, info.tanpaRukuSujud),
            style = MaterialTheme.typography.titleLarge)
        // Hukum dibaca dari bundel (`hukum`), sama seperti di layar Ghaib — bukan diketik di sini.
        if (info.hukum.isNotBlank()) {
            Text(TampilanSholatKhusus.hukum(info.hukum), style = MaterialTheme.typography.labelMedium)
        }
        // Catatan rujukan diteruskan walau berkas Jenazah belum memuatnya: aturannya satu tempat,
        // jadi modul yang menambahkannya kelak tak perlu mengubah layar ini.
        Text(TampilanSholatKhusus.dalil(info.dalil, info.catatanRujukan),
            style = MaterialTheme.typography.labelMedium)
        if (info.syarat.isNotEmpty()) {
            Text(TampilanSholatKhusus.syarat(info.syarat), style = MaterialTheme.typography.labelMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text(NamaAntarmuka.LAYAR_PENUH) })
            OutlinedButton(onClick = { settings = true }) { Text(NamaAntarmuka.PENGATURAN_BACAAN) }
        }
        info.niat.forEach { n ->
            // Varian niat diberi rujukan `niat:<id varian>` supaya tidak bertabrakan dengan id
            // langkah takbir di bawah (`jenazah:<id bacaan>`).
            BacaanCard(JenisBacaan.id(JenisBacaan.JENAZAH, JenisBacaan.id(JenisBacaan.NIAT, n.id)),
                "Niat (${n.judul})", n.arab, n.latin, n.terjemahan,
                showLatin, showTrans, arabSize, sumber = n.sumber, showArab = showArab, kategori = n.kategori)
            if (n.catatan.isNotBlank()) Text(n.catatan, style = MaterialTheme.typography.labelSmall)
        }
        steps.forEachIndexed { i, (judul, b) ->
            Text(TampilanSholatKhusus.langkah(i + 1, judul), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
            if (b != null) {
                val (ra, rl, rt) = repo.resolveArabLatinTerjemah(b.rujukanFile)
                val (a, l, t) = TeksBacaan.pilih(b, ra, rl, rt)
                BacaanCard(JenisBacaan.id(JenisBacaan.JENAZAH, b.id), b.judul, a, l, t, showLatin, showTrans,
                    UkuranBacaan.arabBacaJauh(arabSize, fullscreen), sumber = b.sumber,
                    showArab = showArab, kategori = b.kategori)
            }
        }
        if (info.catatanMazhab.isNotBlank()) {
            Text(TampilanSholatKhusus.catatanMazhab(info.catatanMazhab),
                style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
        }
    }

    if (settings) {
        BacaanSettingsSheet(
            onDismiss = { settings = false },
            showArab = showArab, onShowArab = { showArab = it; scope.launch { store.set(Store.SHOW_ARAB, it) } },
            showLatin = showLatin, onShowLatin = { showLatin = it; scope.launch { store.set(Store.SHOW_LATIN, it) } },
            showTrans = showTrans, onShowTrans = { showTrans = it; scope.launch { store.set(Store.SHOW_TRANS, it) } },
            arabSize = arabSize, onArabSize = { arabSize = it; scope.launch { store.set(Store.ARAB_SIZE, it) } },
        )
    }
}
