package id.or.kemenag.yasintahlil.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Komponen arsitektur menu modern (`plan.txt` §"UI/UX dan Navigasi": *"Beranda dengan kartu
 * informasi yang rapi"*, *"Tombol aksi utama mudah dijangkau"*).
 *
 * Sebelum ini, menu Ibadah/Alat dan aksi cepat beranda adalah tumpukan `Button` polos tanpa ikon.
 * Ketiganya kini punya bentuk yang sama di seluruh aplikasi: ubin ikon-atas-label-bawah untuk
 * kisi aksi cepat, baris ikon-kiri-label-panah untuk menu daftar, dan judul bagian bergaris
 * aksen. Labelnya tetap datang dari `NamaLayar` — komponen ini tidak menamai layar apa pun.
 */

/**
 * Judul satu bagian layar dengan garis aksen zamrud di kirinya — pemisah visual yang tenang
 * antarbagian, menggantikan deretan `Text` judul yang tampil tanpa penanda.
 */
@Composable
fun BagianJudul(teks: String, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 18.dp)
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
        )
        Spacer(Modifier.width(10.dp))
        Text(teks, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Ubin menu untuk kisi aksi cepat: ikon di dalam wadah bulat zamrud lembut, label di bawahnya.
 * Lebar ubin mengikuti bobot barisnya (`Modifier.weight(1f)` dari pemanggil), sehingga kisi
 * tetap rata di segala lebar layar.
 */
@Composable
fun MenuTile(
    label: String,
    ikon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    ikon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Baris menu navigasi: ikon dalam wadah bulat di kiri, label, panah di kanan. Dipakai menu
 * Ibadah, Alat, kelompok dzikir, dan daftar sholat supaya daftar panjang itu terbaca sebagai
 * deretan tujuan yang sama bentuknya — bukan tombol terisi yang berebut perhatian satu sama
 * lain. Barisnya ringkas: lima belas tujuan di kedua menu itu harus muat digulir tanpa terasa
 * panjang.
 *
 * [keterangan] adalah baris kecil di bawah labelnya — mis. "2 rakaat • Subuh" pada sholat atau
 * "12 bacaan" pada kelompok dzikir. Sifatnya opsional: menu Ibadah dan Alat tidak
 * memakainya, dan bentuknya tetap sama seperti sebelum parameter ini ada.
 */
@Composable
fun MenuBaris(
    label: String,
    ikon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    keterangan: String? = null,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(34.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    ikon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (keterangan != null) Text(
                    keterangan,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * Tombol ikon **Info** kecil — pintu ke layar keterangan (activity terpisah).
 *
 * Keterangan panjang yang dahulu menumpuk di badan layar konten kini dipadatkan atau dipindahkan
 * ke layar keterangan; ikon kecil ini adalah satu-satunya jalan ke sana, sehingga badan layar
 * tetap bersih tanpa menyembunyikan informasi. Ukurannya sengaja kecil (32 dp) supaya tidak
 * berebut perhatian dengan judul layar di sebelahnya.
 */
@Composable
fun TombolInfo(modifier: Modifier = Modifier, onInfo: () -> Unit) {
    Surface(
        onClick = onInfo,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.size(32.dp),
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = NamaAntarmuka.KETERANGAN,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(7.dp),
        )
    }
}

/**
 * Kartu keterangan ringkas: baris-baris penjelas yang tetap perlu terbaca sekilas, dengan ikon
 * Info kecil yang membuka keterangannya yang lengkap di layar terpisah.
 *
 * [ringkas] adalah bagian yang dipadatkan — biasanya baris hukum, dalil, atau sumber yang memang
 * wajib tampil; penjelasan yang lebih panjang tinggal di layar keterangan dan tidak menumpuk di
 * sini. Wadahnya `surfaceVariant` (bukan kartu berbingkai) supaya terbaca sebagai catatan kaki
 * layar, bukan konten utama yang berebut perhatian dengan bacaan di bawahnya.
 */
@Composable
fun KartuKeterangan(
    onInfo: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    ringkas: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f).padding(top = 4.dp), content = ringkas)
            if (onInfo != null) TombolInfo(onInfo = onInfo)
        }
    }
}

/**
 * Baris chip yang dapat digulir mendatar: deretan chip/tombol pendamping tidak pernah meluber
 * keluar layar sempit — ia digulir, bukan dipaksa mempersempat tiap chipnya.
 *
 * Sebelum ini, tiap layar menata deretan chipnya sendiri; yang tidak sempat diberi
 * `horizontalScroll` meluber diam-diam di ponsel sempit karena `Row` tidak pernah memberi tahu
 * ketika isinya tak muat lagi.
 */
@Composable
fun BarisChip(
    modifier: Modifier = Modifier,
    konten: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = konten,
    )
}

/**
 * Lencana kecil bertonal — penanda ringkas seperti jumlah rakaat, waktu pelaksanaan, atau jumlah
 * ulangan bacaan. Satu bentuk di seluruh aplikasi, menggantikan teks polos yang menumpuk di
 * bawah judul tanpa penanda.
 */
@Composable
fun Lencana(teks: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier,
    ) {
        Text(
            teks,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

/**
 * Kartu satu entri daftar (sholat wajib/sunnah, langkah panduan, hasil pencarian): judul, deret
 * lencana pendamping, isi, dan baris aksi — semua dengan bentuk yang sama di seluruh aplikasi.
 *
 * Sebelum ini tiap layar menata kartunya sendiri: `Card` polos tanpa bingkai, teks judul dan
 * keterangannya sama polosnya, dan tombolnya penuh lebar. Entri yang berderet panjang kini
 * terbaca sebagai satu keluarga bentuk.
 */
@Composable
fun KartuEntri(
    judul: String,
    lencana: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    isi: @Composable ColumnScope.() -> Unit = {},
    aksi: @Composable RowScope.() -> Unit = {},
) {
    Card(
        modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(judul, style = MaterialTheme.typography.titleMedium)
            if (lencana.isNotEmpty()) {
                // Digulir mendatar, bukan dipaksa mempersempit: tiga lencana bertemu teks tujuan
                // yang panjang harus tetap terbaca utuh di ponsel sempit (pola yang sama dengan
                // [BarisChip] — `Row` tak pernah memberi tahu ketika isinya tak muat lagi).
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) { lencana.forEach { Lencana(it) } }
            }
            Column(
                Modifier.padding(top = if (lencana.isEmpty()) 6.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = isi,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = aksi,
            )
        }
    }
}

/**
 * Lingkaran nomor langkah — penanda urut pada panduan bersuci. Nomornya tinggal di dalam
 * lingkaran tonal zamrud, sehingga deretan langkah terbaca sebagai urutan, bukan daftar teks
 * "1. …" yang menempel pada judulnya.
 */
@Composable
fun LangkahNomor(no: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.size(30.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$no",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
