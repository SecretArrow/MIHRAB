package id.or.kemenag.yasintahlil.feature.search

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

data class Hit(val id: String, val judul: String, val cuplikan: String, val rute: String)

@Composable
fun SearchScreen(go: (String) -> Unit, q0: String = "") {
    val repo = rememberRepo()
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf(q0) }
    val hits = remember(q) {
        if (q.isBlank()) emptyList()
        else buildList {
            repo.yasin.verses.forEach { v ->
                if (v.terjemahan.contains(q, true) || v.latin.contains(q, true) || q.trim() == v.nomor.toString())
                    add(Hit("yasin:${v.nomor}", "Yasin ayat ${v.nomor}", v.terjemahan.take(120),
                        Routes.YASIN))
            }
            repo.tahlil().forEach { t ->
                if (t.judul.contains(q, true) || t.terjemahan.contains(q, true))
                    add(Hit("tahlil:${t.id}", "Tahlil: ${t.judul}", t.terjemahan.take(120),
                        Routes.TAHLIL))
            }
            repo.dzikir().flatMap { it.items }.forEach { d ->
                if (d.judul.contains(q, true) || d.terjemahan.contains(q, true))
                    add(Hit("dzikir:${d.judul}", "Doa: ${d.judul}", d.terjemahan.take(120),
                        Routes.DZIKIR))
            }
            (repo.sholatWajib() + repo.sholatSunnah()).forEach { s ->
                if (s.nama.contains(q, true) || s.deskripsi.contains(q, true))
                    add(Hit("sholat:${s.id}", "Sholat: ${s.nama}", s.deskripsi.take(120),
                        Routes.SHOLAT))
            }
            repo.sholatBacaan().forEach { b ->
                if (b.judul.contains(q, true))
                    add(Hit("bacaan:${b.id}", "Bacaan: ${b.judul}", b.kapan,
                        Routes.SHOLAT))
            }
        }.take(50)
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Pencarian", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(q, { q = it }, label = { Text("surat, ayat, doa, bacaan...") },
            modifier = Modifier.fillMaxWidth())
        Text("${hits.size} hasil", style = MaterialTheme.typography.labelMedium)
        LazyColumn(Modifier.fillMaxSize()) {
            items(hits) { h ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(h.judul, style = MaterialTheme.typography.titleMedium)
                        Text(h.cuplikan)
                        Button(onClick = {
                            scope.launch { store.pushHist("search:${h.id}") }
                            go(h.rute)
                        }) { Text("Buka") }
                    }
                }
            }
        }
    }
}
