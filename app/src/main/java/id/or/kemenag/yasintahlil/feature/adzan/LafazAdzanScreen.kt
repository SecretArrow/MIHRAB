package id.or.kemenag.yasintahlil.feature.adzan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.BacaanCard
import id.or.kemenag.yasintahlil.core.designsystem.BagianJudul
import id.or.kemenag.yasintahlil.core.designsystem.EmptyState
import id.or.kemenag.yasintahlil.core.designsystem.TombolInfo
import id.or.kemenag.yasintahlil.core.designsystem.rememberRepo
import id.or.kemenag.yasintahlil.core.designsystem.rememberStore
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.PesanKonten
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.Routes

/**
 * Layar lafaz adzan — bacaannya sendiri, terpisah dari pengaturnya: dahulu lafaz ini menempel di
 * bagian bawah layar "Adzan & Pengingat" yang isinya saklar, suara, dan izin; kini ia dibuka dari
 * bagian "Adzan" di menu Ibadah, sejajar dengan doa sebelum dan sesudah adzan.
 *
 * Urutan lafaz diberi nomor, dan tiap kartu menyebut sumbernya. Sebelumnya kartu hanya berjudul
 * "Adzan" dan tanpa sumber, sehingga lafaz yang berbunyi sama berulang kali (takbir, syahadat,
 * dan seruan hayya) tak dapat dibedakan satu sama lain. Karena judulnya sama, id kartunya pun
 * bertabrakan (`l.arab.take(10)`), dan kartu dengan id kembar berbagi satu entri favorit —
 * menandai satu lafaz membuat lafaz lain ikut bertanda. Nomor urut menjadikan id-nya unik; id itu
 * ikut pindah tanpa berubah, sehingga favorit lama tetap menempel pada kartunya.
 */
@Composable
fun LafazAdzanScreen(go: (String) -> Unit = {}) {
    val repo = rememberRepo()
    val store = rememberStore()
    val lafaz = remember { repo.adzan() }
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
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(NamaLayar.LAFAZ_ADZAN, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            TombolInfo { go(Routes.keterangan("lafaz_adzan")) }
        }
        Text("Suara adzan, pengingatnya, dan volumenya diatur di layar ${NamaLayar.ADZAN} " +
                "(menu Alat).", style = MaterialTheme.typography.labelMedium)
        BagianJudul("Lafaz adzan")
        if (lafaz.isEmpty()) {
            EmptyState("Lafaz adzan tidak tersedia",
                PesanKonten.berkasTidakAda("lafaz adzan"))
        }
        lafaz.forEachIndexed { i, l ->
            BacaanCard(JenisBacaan.id(JenisBacaan.ADZAN, (i + 1).toString()),
                "Lafaz adzan ${i + 1} dari ${lafaz.size}",
                l.arab, l.latin, l.terjemahan,
                showLatin, showTrans, arabSize, ulangi = l.ulangi, sumber = l.sumber,
                showArab = showArab, kategori = l.kategori)
        }
        Text(repo.adzanCatatan(), style = MaterialTheme.typography.labelMedium)
    }
}
