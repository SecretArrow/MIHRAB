package id.or.kemenag.yasintahlil.feature.ibadah

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.designsystem.BagianJudul
import id.or.kemenag.yasintahlil.core.designsystem.MenuBaris
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.navigation.Routes

/**
 * Menu Ibadah: pintu ke seluruh bacaan dan panduan ibadah, dikelompokkan menurut maksud
 * kunjungannya — bacaan utama, panduan tata cara, lalu simpanan pribadi. Tiap tujuan adalah
 * baris berikon dari satu komponen bersama ([MenuBaris]) supaya bentuknya seragam dengan menu
 * Alat; labelnya tetap dari [NamaLayar], satu nama untuk satu layar.
 */
@Composable
fun IbadahScreen(go: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Menu ${NamaLayar.IBADAH}", style = MaterialTheme.typography.titleLarge)

        // Judul menu bukan tempat mengarang asal-usul teks. Dahulu tombol ini menulis
        // "(83 ayat, MSI Kemenag)": jumlah ayat dan nama sumbernya dipatok di kode, sehingga tidak
        // dapat ikut berubah bila bundelnya diperbarui — dan layar Yasin sendiri sudah menolak
        // hardcode serupa (`AtribusiTeksQuranTest`). Atribusi teks Arab, transliterasi, dan
        // terjemahan kini tampil di layar Yasin, dibaca dari bundelnya.
        BagianJudul("Bacaan utama")
        listOf(
            Triple(Routes.YASIN, NamaLayar.YASIN, Icons.AutoMirrored.Filled.MenuBook),
            Triple(Routes.TAHLIL, NamaLayar.TAHLIL, Icons.Filled.VolunteerActivism),
            Triple(Routes.DZIKIR, NamaLayar.DZIKIR, Icons.Filled.AutoAwesome),
        ).forEach { (rute, label, ikon) -> MenuBaris(label, ikon, { go(rute) }) }

        BagianJudul("Panduan ibadah")
        listOf(
            Triple(Routes.SHOLAT, NamaLayar.SHOLAT, Icons.Filled.SelfImprovement),
            Triple(Routes.WUDHU, NamaLayar.WUDHU, Icons.Filled.WaterDrop),
            Triple(Routes.GHAIB, NamaLayar.GHAIB, Icons.Filled.PersonOff),
            Triple(Routes.JENAZAH, NamaLayar.JENAZAH, Icons.Filled.Groups),
            Triple(Routes.DOA_ADZAN, NamaLayar.DOA_ADZAN, Icons.Filled.Campaign),
        ).forEach { (rute, label, ikon) -> MenuBaris(label, ikon, { go(rute) }) }

        // Amalan harian dan referensi ringan: tasbih, Asmaul Husna, zakat, khataman, hingga
        // peringatan tahlil untuk almarhum. Sama bentuknya dengan bagian lain — satu keluarga
        // `MenuBaris` — supaya menu tetap satu bentuk, bukan menumpuk gaya baru.
        BagianJudul("Amalan & referensi")
        listOf(
            Triple(Routes.TASBIH, NamaLayar.TASBIH, Icons.Filled.TouchApp),
            Triple(Routes.ASMAUL, NamaLayar.ASMAUL, Icons.Filled.AutoStories),
            Triple(Routes.ZAKAT, NamaLayar.ZAKAT, Icons.Filled.Calculate),
            Triple(Routes.KHATAMAN, NamaLayar.KHATAMAN, Icons.Filled.Flag),
            Triple(Routes.ALMARHUM, NamaLayar.ALMARHUM, Icons.Filled.Event),
        ).forEach { (rute, label, ikon) -> MenuBaris(label, ikon, { go(rute) }) }

        BagianJudul("Simpanan")
        listOf(
            Triple(Routes.SEARCH, NamaLayar.SEARCH, Icons.Filled.Search),
            Triple(Routes.FAVORIT, NamaLayar.FAVORIT, Icons.Filled.Favorite),
        ).forEach { (rute, label, ikon) -> MenuBaris(label, ikon, { go(rute) }) }
    }
}
