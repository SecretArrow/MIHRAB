package id.or.kemenag.yasintahlil.core.designsystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.Store
import kotlinx.coroutines.launch

@Composable
fun rememberRepo(): ContentRepo {
    val ctx = LocalContext.current
    return remember { ContentRepo(ctx.applicationContext) }
}

@Composable
fun rememberStore(): Store {
    val ctx = LocalContext.current
    return remember { Store(ctx.applicationContext) }
}

@Composable
fun ArabText(arab: String, sizeSp: Float, modifier: Modifier = Modifier) {
    Text(arab, fontSize = sizeSp.sp, lineHeight = (sizeSp * 1.9f).sp,
        textAlign = TextAlign.Right, modifier = modifier.fillMaxWidth())
}

fun Context.copyText(label: String, text: String) {
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText(label, text))
}

fun Context.shareText(text: String) {
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
    }, "Bagikan"))
}

@Composable
fun BacaanCard(
    id: String, judul: String,
    arab: String, latin: String, terjemahan: String,
    showLatin: Boolean, showTrans: Boolean,
    arabSize: Float, latinSize: Float,
    ulangi: Int = 1, sumber: String = "",
    onOpen: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val store = rememberStore()
    val scope = rememberCoroutineScope()
    var fav by remember { mutableStateOf(false) }
    LaunchedEffect(id) { fav = store.get(Store.FAV, emptySet()).contains(id) }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(judul, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (ulangi > 1) AssistChip(onClick = {}, label = { Text("${ulangi}x") })
            }
            if (arab.isNotBlank()) ArabText(arab, arabSize)
            if (showLatin && latin.isNotBlank()) Text(latin, fontSize = latinSize.sp,
                color = MaterialTheme.colorScheme.secondary)
            if (showTrans && terjemahan.isNotBlank()) Text(terjemahan, fontSize = latinSize.sp)
            if (sumber.isNotBlank()) Text("Sumber: $sumber", style = MaterialTheme.typography.labelSmall)
            Row {
                TextButton(onClick = {
                    ctx.copyText(judul, "$judul\n$arab\n$latin\n$terjemahan")
                    scope.launch { store.pushHist("copy:$id") }
                }) { Text("Salin") }
                TextButton(onClick = { ctx.shareText("$judul\n$arab\n$latin\n$terjemahan") }) { Text("Bagikan") }
                TextButton(onClick = {
                    scope.launch { store.toggleFav(id); fav = store.get(Store.FAV, emptySet()).contains(id) }
                }) { Text(if (fav) "★ Favorit" else "☆ Favorit") }
                if (onOpen != null) TextButton(onClick = onOpen) { Text("Buka") }
            }
        }
    }
}

@Composable
fun CounterRow(count: Int, target: Int, onPlus: () -> Unit, onMinus: () -> Unit, onReset: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Button(onClick = onMinus) { Text("−") }
        Text("$count / $target", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onPlus) { Text("+") }
        OutlinedButton(onClick = onReset) { Text("Reset") }
    }
    LinearProgressIndicator(progress = { (count.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
}
