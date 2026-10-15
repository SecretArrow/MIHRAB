package id.or.kemenag.yasintahlil.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.navigation.FullscreenEffect

/**
 * Layar pengantar sekali di awal (onboarding): tiga halaman singkat sebelum beranda — sambutan,
 * amalan harian, dan waktu sholat & pengingat. Ditampilkan hanya selama penanda
 * `Store.ONBOARDING_SELESAI` belum tersimpan; "Mulai" menuliskan penandanya lalu kembali ke
 * beranda dengan tumpukan dibersihkan (lihat pemanggilnya di `AppNav`), sehingga pengguna lama
 * tidak pernah melihatnya lagi dan tombol kembali dari beranda keluar aplikasi, bukan kembali
 * ke sini.
 *
 * Perpindahan halamannya sengaja bukan pager: pengantar tiga halaman tidak butuh gulir bebas —
 * satu urutan maju-mundur sudah cukup, dan kolom tunggal ber-[AnimatedContent] tetap sederhana
 * tanpa pustaka pager. Posisi halaman disimpan `rememberSaveable` supaya memutar perangkat di
 * tengah pengantar tidak melempar pengguna kembali ke halaman pertama.
 */
@Composable
fun OnboardingScreen(selesai: () -> Unit) {
    // Bilah navigasi bawah disembunyikan selama pengantar — pengguna baru belum boleh
    // berkeliaran sebelum menekan "Mulai"; pulih otomatis saat layar ditinggalkan.
    FullscreenEffect(true)
    var halaman by rememberSaveable { mutableStateOf(0) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedContent(
            targetState = halaman,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 8 })
                    .togetherWith(fadeOut(tween(160)))
            },
            label = "halaman-pengantar",
        ) { target ->
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (target) {
                    0 -> {
                        Text(
                            "Assalamu'alaikum",
                            style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Selamat datang di Yasin dan Tahlil. Bacaan Surat Yasin dan tahlil " +
                                "lengkap dengan terjemahannya — semuanya tersimpan di perangkat " +
                                "dan tetap dapat dibaca tanpa sambungan internet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    1 -> {
                        Text(
                            "Amalan harian",
                            style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Dzikir pagi & petang, doa harian, tasbih digital, Asmaul Husna, " +
                                "kalkulator zakat, kalender Hijriah & Jawa, hingga catatan tahlil " +
                                "almarhum dan khataman Yasin — satu aplikasi untuk kebiasaan " +
                                "harian Anda.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> {
                        Text(
                            "Waktu sholat & pengingat",
                            style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Jadwal sholat dihitung di perangkat sesuai lokasi dan metode " +
                                "pilihan Anda, dilengkapi pengingat adzan dan imsak, arah kiblat, " +
                                "serta widget jadwal di layar utama. Seluruh pengaturannya " +
                                "berkumpul di satu tempat: tab Pengaturan.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Titik penanda posisi halaman: halaman aktif menyala warna pokok, sisanya redup.
        // Bentuknya murni penanda (bukan tombol) — perpindahan halaman lewat tombol di bawah.
        Spacer(Modifier.height(28.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(HALAMAN) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .background(
                            if (i == halaman) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // "Kembali" hanya ada setelah halaman pertama; tempatnya tetap dijaga agar tombol
            // "Lanjut"/"Mulai" tidak berpindah posisi antar-halaman.
            if (halaman > 0) {
                TextButton(onClick = { halaman-- }) { Text("Kembali") }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Button(onClick = {
                if (halaman >= HALAMAN - 1) selesai() else halaman++
            }) {
                Text(if (halaman >= HALAMAN - 1) "Mulai" else "Lanjut")
            }
        }
    }
}

private const val HALAMAN = 3
