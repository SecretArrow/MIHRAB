package id.or.kemenag.yasintahlil.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.or.kemenag.yasintahlil.feature.alat.AlatScreen
import id.or.kemenag.yasintahlil.feature.home.HomeScreen
import id.or.kemenag.yasintahlil.feature.ibadah.IbadahScreen
import id.or.kemenag.yasintahlil.feature.settings.SettingsScreen
import id.or.kemenag.yasintahlil.feature.yasin.YasinScreen
import id.or.kemenag.yasintahlil.feature.tahlil.TahlilScreen
import id.or.kemenag.yasintahlil.feature.sholat.SholatScreen
import id.or.kemenag.yasintahlil.feature.sholat.GhaibScreen
import id.or.kemenag.yasintahlil.feature.kiblat.KiblatScreen
import id.or.kemenag.yasintahlil.feature.jadwal.JadwalScreen
import id.or.kemenag.yasintahlil.feature.dzikir.DzikirScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val tabs = listOf(Routes.HOME to "Beranda", Routes.IBADAH to "Ibadah", Routes.ALAT to "Alat", Routes.SETTINGS to "Pengaturan")
    Scaffold(bottomBar = {
        NavigationBar {
            val entry by nav.currentBackStackEntryAsState()
            val cur = entry?.destination?.route
            tabs.forEach { (r, label) ->
                NavigationBarItem(selected = cur == r, onClick = { nav.navigate(r) }, icon = {}, label = { Text(label) })
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
            composable(Routes.GHAIB) { GhaibScreen() }
            composable(Routes.JENAZAH) { SholatScreen { } }
            composable(Routes.ADZAN) { JadwalScreen() }
            composable(Routes.IQOMAH) { DzikirScreen() }
            composable(Routes.KIBLAT) { KiblatScreen() }
            composable(Routes.JADWAL) { JadwalScreen() }
            composable(Routes.DZIKIR) { DzikirScreen() }
        }
    }
}
