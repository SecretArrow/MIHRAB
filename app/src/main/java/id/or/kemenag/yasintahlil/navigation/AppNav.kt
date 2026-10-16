package id.or.kemenag.yasintahlil.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.KolomKonten
import id.or.kemenag.yasintahlil.core.designsystem.rememberStore
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.feature.adzan.AdzanScreen
import id.or.kemenag.yasintahlil.feature.adzan.DoaAdzanScreen
import id.or.kemenag.yasintahlil.feature.adzan.IqomahScreen
import id.or.kemenag.yasintahlil.feature.alat.AlatScreen
import id.or.kemenag.yasintahlil.feature.almarhum.AlmarhumScreen
import id.or.kemenag.yasintahlil.feature.asmaul.AsmaulHusnaScreen
import id.or.kemenag.yasintahlil.feature.dzikir.DzikirGroupScreen
import id.or.kemenag.yasintahlil.feature.dzikir.DzikirScreen
import id.or.kemenag.yasintahlil.feature.favorit.FavoritScreen
import id.or.kemenag.yasintahlil.feature.home.HomeScreen
import id.or.kemenag.yasintahlil.feature.ibadah.IbadahScreen
import id.or.kemenag.yasintahlil.feature.jadwal.JadwalScreen
import id.or.kemenag.yasintahlil.feature.kalender.KalenderScreen
import id.or.kemenag.yasintahlil.feature.keterangan.KeteranganScreen
import id.or.kemenag.yasintahlil.feature.khataman.KhatamanScreen
import id.or.kemenag.yasintahlil.feature.kiblat.KiblatScreen
import id.or.kemenag.yasintahlil.feature.onboarding.OnboardingScreen
import id.or.kemenag.yasintahlil.feature.search.SearchScreen
import id.or.kemenag.yasintahlil.feature.settings.ContentUpdateScreen
import id.or.kemenag.yasintahlil.feature.settings.SettingsScreen
import id.or.kemenag.yasintahlil.feature.sholat.GhaibScreen
import id.or.kemenag.yasintahlil.feature.sholat.JenazahScreen
import id.or.kemenag.yasintahlil.feature.sholat.SholatDetailScreen
import id.or.kemenag.yasintahlil.feature.sholat.SholatScreen
import id.or.kemenag.yasintahlil.feature.sholat.WudhuScreen
import id.or.kemenag.yasintahlil.feature.tahlil.TahlilScreen
import id.or.kemenag.yasintahlil.feature.tasbih.TasbihInfoScreen
import id.or.kemenag.yasintahlil.feature.tasbih.TasbihRiwayatScreen
import id.or.kemenag.yasintahlil.feature.tasbih.TasbihScreen
import id.or.kemenag.yasintahlil.feature.tentang.TentangScreen
import id.or.kemenag.yasintahlil.feature.sumber.SumberScreen
import id.or.kemenag.yasintahlil.feature.yasin.YasinScreen
import id.or.kemenag.yasintahlil.feature.zakat.ZakatScreen
import kotlinx.coroutines.launch

@Composable
fun AppNav(pendingRoute: String? = null, onRouteConsumed: () -> Unit = {}) {
    val nav = rememberNavController()
    val bottomBarVisible = remember { mutableStateOf(true) }
    // Onboarding sekali di awal: selama penandanya belum tersimpan, pengguna baru disambut
    // layar pengantar sebelum dapat berkeliling. "Mulai" menuliskan penandanya lalu kembali
    // ke beranda; pengguna lama (penanda sudah `true`) tidak pernah melihatnya lagi.
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    var onboardingSelesai by remember { mutableStateOf(BawaanPreferensi.ONBOARDING_SELESAI) }
    LaunchedEffect(Unit) {
        onboardingSelesai = store.get(Store.ONBOARDING_SELESAI, BawaanPreferensi.ONBOARDING_SELESAI)
        if (!onboardingSelesai) nav.navigate(Routes.ONBOARDING)
    }
    // Ketukan notifikasi membawa rute; hanya rute yang dikenal yang dibuka, sisanya diabaikan
    // supaya permintaan dari luar tak pernah membuat aplikasi berhenti.
    LaunchedEffect(pendingRoute) {
        val r = pendingRoute
        if (r != null) {
            if (r in Routes.LANGSUNG) nav.navigate(r)
            onRouteConsumed()
        }
    }
    // `Icons.AutoMirrored.Filled.MenuBook`, bukan `Icons.Filled.MenuBook` yang usang: ikon buku
    // dibalik mendatar pada tata letak kanan-ke-kiri. Ikon lain di sini tidak punya arah baca,
    // jadi tetap versi biasa.
    val tabs = listOf(
        Triple(Routes.HOME, NamaLayar.BERANDA, Icons.Filled.Home),
        Triple(Routes.IBADAH, NamaLayar.IBADAH, Icons.AutoMirrored.Filled.MenuBook),
        Triple(Routes.ALAT, NamaLayar.ALAT, Icons.Filled.Explore),
        Triple(Routes.SETTINGS, NamaLayar.PENGATURAN, Icons.Filled.Settings),
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
            // Tata letak responsif: seluruh isi dipusatkan pada kolom selebar maksimum, sehingga di
            // tablet maupun ponsel lanskap baris bacaan tidak membentang sepanjang layar. Batas ini
            // dipasang sekali di sini agar setiap layar ikut, tanpa pengkabelan per layar.
            KolomKonten(Modifier.padding(pad)) {
            NavHost(nav, Routes.HOME, Modifier.fillMaxSize()) {
                composable(Routes.HOME) { HomeScreen(go = { nav.navigate(it) }) }
                composable(Routes.IBADAH) { IbadahScreen { nav.navigate(it) } }
                composable(Routes.ALAT) { AlatScreen { nav.navigate(it) } }
                composable(Routes.SETTINGS) { SettingsScreen { nav.navigate(it) } }
                composable(Routes.YASIN) { YasinScreen(go = { nav.navigate(it) }) }
                composable(Routes.TAHLIL) { TahlilScreen(go = { nav.navigate(it) }) }
                composable(Routes.SHOLAT) { SholatScreen { nav.navigate(it) } }
                composable("${Routes.SHOLAT_DETAIL}/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    SholatDetailScreen(it.arguments?.getString("id").orEmpty()) { r -> nav.navigate(r) }
                }
                composable(Routes.GHAIB) { GhaibScreen(go = { nav.navigate(it) }) }
                composable(Routes.JENAZAH) { JenazahScreen(go = { nav.navigate(it) }) }
                composable(Routes.WUDHU) { WudhuScreen(go = { nav.navigate(it) }) }
                composable(Routes.ADZAN) { AdzanScreen(go = { nav.navigate(it) }) }
                composable(Routes.DOA_ADZAN) { DoaAdzanScreen(go = { nav.navigate(it) }) }
                composable(Routes.IQOMAH) { IqomahScreen(go = { nav.navigate(it) }) }
                composable(Routes.KIBLAT) { KiblatScreen(go = { nav.navigate(it) }) }
                composable(Routes.JADWAL) { JadwalScreen(go = { nav.navigate(it) }) }
                composable(Routes.DZIKIR) { DzikirScreen(go = { nav.navigate(it) }) }
                // Satu kelompok dzikir: seluruh bacaannya mengalir bersambung di layar ini.
                // Id-nya datang dari menu Dzikir, indeks Pencarian, atau tombol "Buka" favorit —
                // selalu dari bundel, tidak pernah dieja di sini.
                composable(
                    "${Routes.DZIKIR}/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) {
                    // `go` diberi nama: parameternya didahului `vm` bawaan, sehingga lambda ekor
                    // tanpa nama justru menempel pada `vm` — berbeda dari SholatDetailScreen yang
                    // memang berakhir pada `go`.
                    DzikirGroupScreen(
                        id = it.arguments?.getString("id").orEmpty(),
                        go = { r -> nav.navigate(r) },
                    )
                }
                composable(Routes.SEARCH) { SearchScreen(go = { nav.navigate(it) }) }
                composable(Routes.FAVORIT) { FavoritScreen(go = { nav.navigate(it) }) }
                composable(Routes.KONTEN_UPDATE) { ContentUpdateScreen() }
                composable(Routes.TENTANG) { TentangScreen(go = { nav.navigate(it) }) }
                composable(Routes.SUMBER) { SumberScreen() }
                // ---- Amalan & referensi --------------------------------------------------------------
                composable(Routes.TASBIH) { TasbihScreen(go = { nav.navigate(it) }) }
                // Dua layar pendamping Tasbih — riwayat 7 hari dan keterangannya — dibuka dari
                // layar Tasbih sendiri, bukan dari menu: keduanya bermakna bersama alat hitungnya.
                composable(Routes.TASBIH_RIWAYAT) { TasbihRiwayatScreen() }
                composable(Routes.TASBIH_INFO) { TasbihInfoScreen() }
                composable(Routes.ASMAUL) { AsmaulHusnaScreen(go = { nav.navigate(it) }) }
                composable(Routes.ZAKAT) { ZakatScreen(go = { nav.navigate(it) }) }
                composable(Routes.KALENDER) { KalenderScreen(go = { nav.navigate(it) }) }
                composable(Routes.KHATAMAN) { KhatamanScreen(go = { nav.navigate(it) }) }
                composable(Routes.ALMARHUM) { AlmarhumScreen(go = { nav.navigate(it) }) }
                // Layar pengantar sekali di awal: "Mulai" menyimpan penandanya lalu kembali ke
                // beranda dengan tumpukan dibersihkan — tombol kembali dari beranda keluar aplikasi,
                // bukan kembali ke pengantar yang sudah selesai.
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(selesai = {
                        scope.launch { store.set(Store.ONBOARDING_SELESAI, true) }
                        nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    })
                }
                // Layar keterangan: keterangan panjang tinggal di sini, dibuka lewat ikon Info
                // kecil pada layar konten. Asalnya menjadi argumen supaya keterangannya dibaca
                // ulang dari sumber yang sama — bukan disalin dari layar pembukanya.
                composable(
                    "${Routes.KETERANGAN}/{asal}",
                    arguments = listOf(navArgument("asal") { type = NavType.StringType }),
                ) {
                    KeteranganScreen(
                        it.arguments?.getString("asal").orEmpty(),
                        kembali = { nav.popBackStack() },
                    )
                }
            }
            }
        }
    }
}
