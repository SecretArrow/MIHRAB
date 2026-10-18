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
 * Judul langkah takbir yang khas layar ini — sholat ghaib membaca ta'awudz sebelum Al-Fatihah dan
 * salamnya menoleh ke dua sisi. Takbir 2–4 menyebut bacaan yang sama dengan layar Jenazah, jadi
 * ejaannya tinggal di [TampilanSholatKhusus.judulTakbirBersama] dan digabung lewat
 * [TampilanSholatKhusus.judulLangkah]; di sini hanya yang benar-benar khas.
 *
 * Takbir 1 **tidak** lagi menyebut rujukan hadisnya. Rujukan itu sudah ditampilkan kartu bacaan di
 * bawahnya — dibaca dari `sumber` entri `fatihah` di bundel ("HR Bukhari 756, Muslim 394 (la sholata
 * liman lam yaqra bi fatihatil kitab) — sunnah.com"). Salinan yang diketik di judul ini lebih pendek
 * dan menghilangkan matan serta alamat resminya, sehingga satu layar menampilkan **dua** kutipan
 * berbeda untuk bacaan yang sama.
 *
 * Kuncinya adalah id bacaan dari [BacaanSholat.TAKBIR_MAYIT], jadi setiap id pada urutan bersama
 * itu pasti punya judul — penjaganya memaku cakupan itu sehingga `getValue` tidak pernah gagal.
 */
private val judulLangkah: Map<String, String> = TampilanSholatKhusus.judulLangkah(
    mapOf(
        BacaanSholat.FATIHAH to "Takbir 1 → ta'awudz + Al-Fatihah",
        BacaanSholat.SALAM to "Salam kanan-kiri",
    ),
)

/**
 * Sholat Ghaib: 4 takbir tanpa ruku/sujud, fardhu kifayah. Seluruh konten (niat & doa)
 * dibaca dari assets ([BerkasKonten.SHOLAT_GHAIB] + bacaan bersama) — tidak ada teks Arab di Kotlin.
 *
 * Sebelum masuk ke bacaannya, pengguna memilih dahulu jenazahnya **Laki-laki, Wanita, atau
 * Jamak/tidak diketahui** (permintaan pengguna, 2026-10-16): niatnya ber-tiga varian di bundel dan
 * doa mayitnya ber-dhamir gender, jadi pilihannya diletakkan paling atas — seluruh lafaz di
 * bawahnya menyesuaikan seketika. Varian jamak memakai doa umum (dhamirnya tidak berubah).
 *
 * Keterangan panjang — dalil utuh, syarat, catatan mazhab — tinggal di layar keterangan lewat
 * ikon Info; di badan layar yang tersisa hanyalah hukum dan baris dalilnya (keduanya dipaku
 * penjaga atribusi), bukan lagi kartu keterangan yang mengulang isinya.
 *
 * Perangkaian teksnya — judul, syarat, dalil beserta catatan rujukannya, catatan mazhab, langkah,
 * dan cadangan teks bacaan — datang dari [TampilanSholatKhusus], sama dengan layar Jenazah. Yang
 * khas layar ini hanya [judulLangkah] dan label chip layar penuhnya.
 *
 * Kalimat rujukan tambahan itu dahulu diketik di sini sebagai `CATATAN_DALIL`, padahal isinya
 * menyebut NU Online — domain yang **tidak** tercantum di `sumber_tambahan` berkasnya, sehingga
 * daftar sumber di layar Tentang tak pernah menyebutnya. Kini `sholat_ghaib.json#catatan_rujukan`
 * yang memuatnya dan `sumber_tambahan` mencantumkan ketiga domainnya.
 */
@Composable
fun GhaibScreen(go: (String) -> Unit = {}) {
    val repo = rememberRepo()
    val store = rememberStore()
    // rememberSaveable: mode layar penuh dan pilihan gender tidak hilang saat layar diputar ulang.
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    // Pilihan jenazah: 0 = laki-laki, 1 = wanita, 2 = jamak/tidak diketahui — tiga varian niat
    // yang memang tersedia di bundelnya. Variannya satu Int, bukan tiga Boolean, supaya tepat
    // satu yang terpilih.
    var gender by rememberSaveable { mutableStateOf(0) }
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
    val info = remember { repo.sholatKhusus(BerkasKonten.SHOLAT_GHAIB) }
    val bacaan = remember { repo.sholatBacaan() + repo.sholatMayitBacaan() }
    // Lookup gender-sadar: doa mayit punya varian dhamir wanita (`BacaanSholat.VARIAN_WANITA`,
    // aturan bersama kedua layar); varian jamak tetap memakai doa umumnya.
    fun cari(id: String) = bacaan.find { it.id == BacaanSholat.idUntuk(id, gender == 1) }
    val steps: List<Pair<String, Bacaan?>> =
        listOf("Niat dalam hati (varian di bawah)" to null) +
            // Urutan bacaan takbir datang dari `BacaanSholat.TAKBIR_MAYIT`; yang khas layar ini
            // hanya judulnya. Sebelumnya kelima idnya diketik ulang di sini, sehingga satu salah
            // eja cukup untuk menghilangkan satu takbir — kartunya tak muncul, judulnya tetap ada.
            BacaanSholat.TAKBIR_MAYIT.map { judulLangkah.getValue(it) to cari(it) }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(TampilanSholatKhusus.judul(info.judul.ifBlank { NamaLayar.GHAIB }, info.takbir, info.tanpaRukuSujud),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("ghaib")) }
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

        // Pilihan jenazah — Laki-laki / Wanita / Jamak — SEBELUM konten, agar niat dan doa yang
        // dibaca jelas sasarannya. Bila bundelnya belum memuat varian yang dipilih, seluruh
        // varian tampil — layar tidak boleh buntu karena bundel yang lebih tua.
        BarisChip(Modifier.padding(top = 6.dp)) {
            FilterChip(gender == 0, { gender = 0 }, { Text("Laki-laki") })
            FilterChip(gender == 1, { gender = 1 }, { Text("Wanita") })
            FilterChip(gender == 2, { gender = 2 }, { Text("Jamak") })
        }
        BarisChip(Modifier.padding(vertical = 4.dp)) {
            FilterChip(fullscreen, { fullscreen = !fullscreen }, { Text(NamaAntarmuka.LAYAR_PENUH_IMAM) })
        }
        if (info.niat.isNotEmpty()) {
            BagianJudul("Niat")
            val idNiat = if (gender == 2) "ghaib_jamak" else if (gender == 1) "ghaib_perempuan" else "ghaib_laki"
            val niatPas = info.niat.filter { it.id == idNiat }.ifEmpty { info.niat }
            niatPas.forEach { n ->
                // Varian niat diberi rujukan `niat:<id varian>` supaya tidak bertabrakan dengan id
                // langkah di bawah (`ghaib:<id bacaan>`) — keduanya id tersimpan yang berbeda.
                BacaanCard(JenisBacaan.id(JenisBacaan.GHAIB, JenisBacaan.id(JenisBacaan.NIAT, n.id)),
                    n.judul, n.arab, n.latin, n.terjemahan,
                    showLatin, showTrans, arabSize, sumber = n.sumber, showArab = showArab, kategori = n.kategori)
            }
        }
        steps.forEachIndexed { i, (judul, b) ->
            BagianJudul(TampilanSholatKhusus.langkah(i + 1, judul))
            if (b != null) {
                val (ra, rl, rt) = repo.resolveArabLatinTerjemah(b.rujukanFile)
                val (a, l, t) = TeksBacaan.pilih(b, ra, rl, rt)
                BacaanCard(JenisBacaan.id(JenisBacaan.GHAIB, b.id), b.judul, a, l, t, showLatin, showTrans,
                    UkuranBacaan.arabBacaJauh(arabSize, fullscreen), sumber = b.sumber,
                    showArab = showArab, kategori = b.kategori)
            }
        }
    }

}
