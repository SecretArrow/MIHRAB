package id.or.kemenag.yasintahlil.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.or.kemenag.yasintahlil.feature.adzan.AdzanScreen
import id.or.kemenag.yasintahlil.feature.adzan.DoaAdzanScreen
import id.or.kemenag.yasintahlil.feature.adzan.IqomahScreen
import id.or.kemenag.yasintahlil.feature.alat.AlatScreen
import id.or.kemenag.yasintahlil.feature.dzikir.DzikirScreen
import id.or.kemenag.yasintahlil.feature.favorit.FavoritScreen
import id.or.kemenag.yasintahlil.feature.home.HomeScreen
import id.or.kemenag.yasintahlil.feature.ibadah.IbadahScreen
import id.or.kemenag.yasintahlil.feature.jadwal.JadwalScreen
import id.or.kemenag.yasintahlil.feature.kiblat.KiblatScreen
import id.or.kemenag.yasintahlil.feature.search.SearchScreen
import id.or.kemenag.yasintahlil.feature.settings.SettingsScreen
import id.or.kemenag.yasintahlil.feature.sholat.GhaibScreen
import id.or.kemenag.yasintahlil.feature.sholat.JenazahScreen
import id.or.kemenag.yasintahlil.feature.sholat.SholatDetailScreen
import id.or.kemenag.yasintahlil.feature.sholat.SholatScreen
import id.or.kemenag.yasintahlil.feature.tahlil.TahlilScreen
import id.or.kemenag.yasintahlil.feature.yasin.YasinScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val bottomBarVisible = remember { mutableStateOf(true) }
    val tabs = listOf(
        Triple(Routes.HOME, "Beranda", Icons.Filled.Home),
        Triple(Routes.IBADAH, "Ibadah", Icons.Filled.MenuBook),
        Triple(Routes.ALAT, "Alat", Icons.Filled.Explore),
        Triple(Routes.SETTINGS, "Pengaturan", Icons.Filled.Settings),
    )
    CompositionLocalProvider(LocalBottomBarVisible provides bottomBarVisible) {
        Scaffold(bottomBar = {
            if (bottomBarVisible.value) NavigationBar {
                val entry by nav.currentBackStackEntryAsState()
                val cur = entry?.destination?.route
                tabs.forEach { (r, label, icon) ->
                    NavigationBarItem(
                        selected = cur == r,
                        onClick = { nav.navigate(r) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        }) { pad ->
            NavHost(nav, Routes.HOME, Modifier.padding(pad)) {
                composable(Routes.HOME) { HomeScreen { nav.navigate(it) } }
                composable(Routes.IBADAH) { IbadahScreen { nav.navigate(it) } }
                composable(Routes.ALAT) { AlatScreen { nav.navigate(it) } }
                composable(Routes.SETTINGS) { SettingsScreen() }
                composable(Routes.YASIN) { YasinScreen() }
                composable(Routes.TAHLIL) { TahlilScreen() }
                composable(Routes.SHOLAT) { SholatScreen { nav.navigate(it) } }
                composable("${Routes.SHOLAT_DETAIL}/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    SholatDetailScreen(it.arguments?.getString("id").orEmpty())
                }
                composable(Routes.GHAIB) { GhaibScreen() }
                composable(Routes.JENAZAH) { JenazahScreen() }
                composable(Routes.ADZAN) { AdzanScreen() }
                composable(Routes.DOA_ADZAN) { DoaAdzanScreen() }
                composable(Routes.IQOMAH) { IqomahScreen() }
                composable(Routes.KIBLAT) { KiblatScreen() }
                composable(Routes.JADWAL) { JadwalScreen() }
                composable(Routes.DZIKIR) { DzikirScreen() }
                composable(Routes.SEARCH) { SearchScreen(go = { nav.navigate(it) }) }
                composable(Routes.FAVORIT) { FavoritScreen { nav.navigate(it) } }
            }
        }
    }
}
