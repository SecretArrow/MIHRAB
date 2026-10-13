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
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.sholat.BacaanSholat
import id.or.kemenag.yasintahlil.core.sholat.TampilanSholatKhusus
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect
import kotlinx.coroutines.launch

/**
 * Judul langkah takbir yang khas layar ini — sholat ghaib membaca ta'awudz sebelum Al-Fatihah dan
 * salamnya menoleh ke dua sisi. Takbir 2–4 menyebut bacaan yang sama dengan layar Jenazah, jadi
 * ejaannya tinggal di [TampilanSholatKhusus.judulTakbirBersama] dan digabung lewat
 * [TampilanSholatKhusus.judulLangkah]; di sini hanya yang benar-benar khas.
 *
 * Kuncinya adalah id bacaan dari [BacaanSholat.TAKBIR_MAYIT], jadi setiap id pada urutan bersama
 * itu pasti punya judul — penjaganya memaku cakupan itu sehingga `getValue` tidak pernah gagal.
 */
private val judulLangkah: Map<String, String> = TampilanSholatKhusus.judulLangkah(
    mapOf(
        BacaanSholat.FATIHAH to "Takbir 1 → ta'awudz + Al-Fatihah (HR Bukhari 756, Muslim 394)",
        BacaanSholat.SALAM to "Salam kanan-kiri",
    ),
)

/**
 * Kalimat lanjutan setelah dalil, **khas Ghaib**: entri ghaib bersandar pada imbauan Kemenag dan
 * kritik sanad NU Online, jadi kedua rujukan itu disebut di sini. Jenazah tidak memakainya.
 */
private const val CATATAN_DALIL =
    " Rujukan tata cara: imbauan Kemenag; syarat & kritik sanad: NU Online. Lihat SOURCES.md."

/**
 * Sholat Ghaib: 4 takbir tanpa ruku/sujud, fardhu kifayah. Seluruh konten (niat & doa)
 * dibaca dari assets ([BerkasKonten.SHOLAT_GHAIB] + bacaan bersama) — tidak ada teks Arab di Kotlin.
 *
 * Perangkaian teksnya — judul, syarat, dalil, catatan mazhab, langkah, dan cadangan teks bacaan —
 * datang dari [TampilanSholatKhusus], sama dengan layar Jenazah. Yang khas layar ini hanya
 * [judulLangkah] dan label chip layar penuhnya.
 */
@Composable
fun GhaibScreen() {
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
    val info = remember { repo.sholatKhusus(BerkasKonten.SHOLAT_GHAIB) }
    val bacaan = remember { repo.sholatBacaan() + repo.sholatMayitBacaan() }
    fun cari(id: String) = bacaan.find { it.id == id }
    val steps: List<Pair<String, Bacaan?>> =
        listOf("Niat dalam hati (varian di bawah) — fardhu kifayah" to null) +
            // Urutan bacaan takbir datang dari `BacaanSholat.TAKBIR_MAYIT`; yang khas layar ini
            // hanya judulnya. Sebelumnya kelima idnya diketik ulang di sini, sehingga satu salah
            // eja cukup untuk menghilangkan satu takbir — kartunya tak muncul, judulnya tetap ada.
            BacaanSholat.TAKBIR_MAYIT.map { judulLangkah.getValue(it) to cari(it) }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text(TampilanSholatKhusus.judul(info.judul.ifBlank { NamaLayar.GHAIB }, info.takbir),
            style = MaterialTheme.typography.titleLarge)
        Text(TampilanSholatKhusus.dalil(info.dalil) + CATATAN_DALIL,
            style = MaterialTheme.typography.labelMedium)
        if (info.syarat.isNotEmpty()) {
            Text(TampilanSholatKhusus.syarat(info.syarat), style = MaterialTheme.typography.labelMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text(NamaAntarmuka.LAYAR_PENUH_IMAM) })
            OutlinedButton(onClick = { settings = true }) { Text(NamaAntarmuka.PENGATURAN_BACAAN) }
        }
        if (info.niat.isNotEmpty()) {
            Text("Varian niat:", style = MaterialTheme.typography.titleMedium)
            info.niat.forEach { n ->
                // Varian niat diberi rujukan `niat:<id varian>` supaya tidak bertabrakan dengan id
                // langkah di bawah (`ghaib:<id bacaan>`) — keduanya id tersimpan yang berbeda.
                BacaanCard(JenisBacaan.id(JenisBacaan.GHAIB, JenisBacaan.id(JenisBacaan.NIAT, n.id)),
                    n.judul, n.arab, n.latin, n.terjemahan,
                    showLatin, showTrans, arabSize, sumber = n.sumber, showArab = showArab, kategori = n.kategori)
            }
        }
        steps.forEachIndexed { i, (judul, b) ->
            Text(TampilanSholatKhusus.langkah(i + 1, judul), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp))
            if (b != null) {
                val (ra, rl, rt) = repo.resolveArabLatinTerjemah(b.rujukanFile)
                val (a, l, t) = TampilanSholatKhusus.teksBacaan(b, ra, rl, rt)
                BacaanCard(JenisBacaan.id(JenisBacaan.GHAIB, b.id), b.judul, a, l, t, showLatin, showTrans,
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
