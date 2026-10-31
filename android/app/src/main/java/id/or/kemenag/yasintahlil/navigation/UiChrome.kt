package id.or.kemenag.yasintahlil.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf

/**
 * Visibilitas bilah navigasi bawah. Layar yang menyediakan mode layar penuh (Yasin, Tahlil,
 * Ghaib, Dzikir, Iqomah) menyetel ini ke `false` agar bilah benar-benar tersembunyi, lalu
 * mengembalikannya ke `true` saat keluar/di-dispose.
 */
val LocalBottomBarVisible = compositionLocalOf<MutableState<Boolean>> {
    error("LocalBottomBarVisible belum disediakan oleh AppNav")
}

/** Sembunyikan bilah navigasi selama [enabled] benar; pulihkan otomatis saat layar ditinggalkan. */
@Composable
fun FullscreenEffect(enabled: Boolean) {
    val bar = LocalBottomBarVisible.current
    DisposableEffect(enabled) {
        bar.value = !enabled
        onDispose { bar.value = true }
    }
}
