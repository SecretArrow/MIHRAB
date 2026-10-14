package id.or.kemenag.yasintahlil.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
        modifier.padding(top = 20.dp, bottom = 10.dp),
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
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    ikon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
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
 * Ibadah dan Alat supaya kedua daftar panjang itu terbaca sebagai deretan tujuan yang sama
 * bentuknya — bukan tombol terisi yang berebut perhatian satu sama lain.
 */
@Composable
fun MenuBaris(
    label: String,
    ikon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    ikon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
