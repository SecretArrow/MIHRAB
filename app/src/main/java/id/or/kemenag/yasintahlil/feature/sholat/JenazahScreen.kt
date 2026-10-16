package id.or.kemenag.yasintahlil.feature.sholat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
import id.or.kemenag.yasintahlil.navigation.Routes

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
 * Sebelum masuk ke bacaannya, pengguna memilih dahulu jenazahnya **Laki-laki atau Wanita**
 * (permintaan pengguna, 2026-10-16): niat dan doa mayit keduanya ber-dhamir gender, jadi
 * pilihannya diletakkan paling atas — pilihannya langsung menyesuaikan seluruh lafaz di
 * bawahnya, bukan dibiarkan tertukar di tengah bacaan.
 *
 * Keterangan panjang — dalil utuh, syarat, catatan mazhab — tinggal di layar keterangan lewat
 * ikon Info; di badan layar yang tersisa hanyalah hukum dan baris dalilnya (keduanya dipaku
 * penjaga atribusi), bukan lagi kartu keterangan yang mengulang isinya.
 *
 * Perangkaian teksnya — judul, syarat, dalil, catatan mazhab, langkah, dan cadangan teks bacaan —
 * datang dari [TampilanSholatKhusus], sama dengan layar Ghaib. Yang khas layar ini hanya
 * [judulLangkah] dan label chip layar penuhnya.
 */
@Composable
fun JenazahScreen(go: (String) -> Unit = {}) {
    val repo = rememberRepo()
    val store = rememberStore()
    // rememberSaveable: mode layar penuh dan pilihan gender tidak hilang saat layar diputar ulang.
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var wanita by rememberSaveable { mutableStateOf(false) }
    var arabSize by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    var showArab by remember { mutableStateOf(BawaanPreferensi.TAMPIL_ARAB) }
    var showLatin by remember { mutableStateOf(BawaanPreferensi.TAMPIL_LATIN) }
    var showTrans by remember { mutableStateOf(BawaanPreferensi.TAMPIL_TERJEMAHAN) }
    LaunchedEffect(Unit) {
        arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
        showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB)
        showLatin = store.get(Store.SHOW_LATIN, BawaanPreferensi.TAMPIL_LATIN)
        showTrans = store.get(Store.SHOW_TRANS, BawaanPreferensi.TAMPIL_TERJEMAHAN)
    }
    FullscreenEffect(fullscreen)
    val info = remember { repo.sholatKhusus(BerkasKonten.SHOLAT_JENAZAH) }
    val bacaan = remember { repo.sholatBacaan() + repo.sholatMayitBacaan() }
    // Lookup gender-sadar: bacaan mayit punya varian dhamir wanita (`BacaanSholat.VARIAN_WANITA`,
    // aturan bersama kedua layar) — pilihan di kepala layar menentukan lafaz mana yang dipakai.
    fun cari(id: String) = bacaan.find { it.id == BacaanSholat.idUntuk(id, wanita) }
    val steps: List<Pair<String, Bacaan?>> =
        // Urutan bacaan takbir datang dari `BacaanSholat.TAKBIR_MAYIT`; yang khas layar ini hanya
        // judulnya. Sebelumnya kelima idnya diketik ulang di sini, sehingga satu salah eja cukup
        // untuk menghilangkan satu takbir — kartunya tak muncul, judulnya tetap ada.
        BacaanSholat.TAKBIR_MAYIT.map { judulLangkah.getValue(it) to cari(it) }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(TampilanSholatKhusus.judul(info.judul.ifBlank { NamaLayar.JENAZAH }, info.takbir, info.tanpaRukuSujud),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("jenazah")) }
        }
        // Hukum dan dalil tetap tampil ringkas di badan layar (keduanya dipaku penjaga atribusi
        // dan hanya sebaris); syarat dan catatan mazhab yang lebih panjang tinggal di layar
        // keterangan lewat ikon Info — tidak lagi mengulang isinya di sini.
        if (info.hukum.isNotBlank()) {
            Text(TampilanSholatKhusus.hukum(info.hukum), style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp))
        }
        Text(TampilanSholatKhusus.dalil(info.dalil, info.catatanRujukan),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2, overflow = TextOverflow.Ellipsis)

        // Pilihan jenazah — Laki-laki / Wanita — diletakkan SEBELUM konten agar jelas lafaz mana
        // yang sedang dibaca: niat dan doa mayit sama-sama menyesuaikan dhamirnya seketika.
        BarisChip(Modifier.padding(top = 6.dp)) {
            FilterChip(!wanita, { wanita = false }, { Text("Laki-laki") })
            FilterChip(wanita, { wanita = true }, { Text("Wanita") })
        }
        BarisChip(Modifier.padding(vertical = 4.dp)) {
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text(NamaAntarmuka.LAYAR_PENUH) })
        }
        // Niat mengikuti pilihan jenazah; bila bundelnya belum memuat varian yang dipilih,
        // seluruh varian tampil — layar tidak boleh buntu karena bundel yang lebih tua.
        val niatPas = info.niat.filter { it.id == if (wanita) "jenazah_perempuan" else "jenazah_laki" }
            .ifEmpty { info.niat }
        niatPas.forEach { n ->
            // Varian niat diberi rujukan `niat:<id varian>` supaya tidak bertabrakan dengan id
            // langkah takbir di bawah (`jenazah:<id bacaan>`).
            BacaanCard(JenisBacaan.id(JenisBacaan.JENAZAH, JenisBacaan.id(JenisBacaan.NIAT, n.id)),
                "Niat (${n.judul})", n.arab, n.latin, n.terjemahan,
                showLatin, showTrans, arabSize, sumber = n.sumber, showArab = showArab, kategori = n.kategori)
            if (n.catatan.isNotBlank()) Text(n.catatan, style = MaterialTheme.typography.labelSmall)
        }
        steps.forEachIndexed { i, (judul, b) ->
            BagianJudul(TampilanSholatKhusus.langkah(i + 1, judul))
            if (b != null) {
                val (ra, rl, rt) = repo.resolveArabLatinTerjemah(b.rujukanFile)
                val (a, l, t) = TeksBacaan.pilih(b, ra, rl, rt)
                BacaanCard(JenisBacaan.id(JenisBacaan.JENAZAH, b.id), b.judul, a, l, t, showLatin, showTrans,
                    UkuranBacaan.arabBacaJauh(arabSize, fullscreen), sumber = b.sumber,
                    showArab = showArab, kategori = b.kategori, ayat = b.ayat)
            }
        }
    }

}
