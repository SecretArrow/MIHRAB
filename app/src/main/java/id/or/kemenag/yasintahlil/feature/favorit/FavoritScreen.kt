package id.or.kemenag.yasintahlil.feature.favorit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.navigation.Routes
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import kotlinx.coroutines.launch

@Composable
fun FavoritScreen(go: (String) -> Unit) {
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    var fav by remember { mutableStateOf(setOf<String>()) }
    var hist by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        fav = store.get(Store.FAV, emptySet())
        hist = store.get(Store.HIST, "")
        last = store.get(Store.LAST, "")
    }
    fun rute(id: String) = when {
        id.startsWith("yasin") -> Routes.YASIN
        id.startsWith("tahlil") -> Routes.TAHLIL
        id.startsWith("dzikir") || id.startsWith("doa") || id.startsWith("jawab") -> Routes.DZIKIR
        id.startsWith("bacaan") || id.startsWith("niat") || id.startsWith("langkah") || id.startsWith("ghaib") || id.startsWith("jenazah") -> Routes.SHOLAT
        id.startsWith("adzan") || id.startsWith("iqomah") -> Routes.ADZAN
        else -> Routes.HOME
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Favorit, Riwayat & Terakhir dibaca", style = MaterialTheme.typography.titleLarge)
        if (last.isNotBlank()) {
            Text("Terakhir dibaca: $last")
            Button(onClick = { go(rute(last)) }) { Text("Lanjutkan") }
        }
        Text("Favorit (${fav.size}):", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(fav.toList()) { id ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(id, modifier = Modifier.weight(1f))
                    TextButton(onClick = { go(rute(id)) }) { Text("Buka") }
                    TextButton(onClick = { scope.launch { store.toggleFav(id); fav = store.get(Store.FAV, emptySet()) } }) { Text("Hapus") }
                }
            }
        }
        Text("Riwayat terbaru:", style = MaterialTheme.typography.titleMedium)
        hist.split(";").filter { it.isNotBlank() }.take(10).forEach { Text(it) }
        if (hist.isBlank()) Text("Belum ada riwayat.", style = MaterialTheme.typography.labelMedium)
    }
}
