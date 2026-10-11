package id.or.kemenag.yasintahlil.feature.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.BuildConfig
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SettingsScreen() {
    val store = rememberStore()
    val userData = rememberUserData()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var theme by remember { mutableStateOf("sistem") }
    var arab by remember { mutableStateOf(24f) }
    var latin by remember { mutableStateOf(16f) }
    var showL by remember { mutableStateOf(true) }
    var showT by remember { mutableStateOf(true) }
    var asar by remember { mutableStateOf("syafii") }
    var mazhab by remember { mutableStateOf("syafii") }
    var city by remember { mutableStateOf("Jakarta") }
    var notif by remember { mutableStateOf(true) }
    var hijriCorr by remember { mutableStateOf(0) }
    var compassUnit by remember { mutableStateOf("derajat") }
    var backup by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        theme = store.get(Store.DARK, "sistem")
        arab = store.get(Store.ARAB_SIZE, 24f); latin = store.get(Store.LATIN_SIZE, 16f)
        showL = store.get(Store.SHOW_LATIN, true); showT = store.get(Store.SHOW_TRANS, true)
        asar = store.get(Store.ASAR, "syafii"); city = store.get(Store.CITY, "Jakarta")
        mazhab = store.get(Store.MADHAB, "syafii")
        notif = store.get(Store.NOTIF, true)
        hijriCorr = store.get(Store.HIJRI_CORR, 0)
        compassUnit = store.get(Store.COMPASS_UNIT, "derajat")
    }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text("Pengaturan", style = MaterialTheme.typography.titleLarge)
        Text("Tema:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("terang", "gelap", "sistem").forEach { t ->
                FilterChip(theme == t, { theme = t; scope.launch { store.set(Store.DARK, t) } }, { Text(t) })
            }
        }
        Text("Bahasa aplikasi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text("Indonesia — satu-satunya bahasa antarmuka. Teks Arab tetap Arab, dan terjemahan yang " +
                "disajikan adalah terjemahan resmi Kemenag 2019 berbahasa Indonesia. Belum ada " +
                "terjemahan antarmuka lain, jadi tidak ada tombol bahasa yang tidak berfungsi.",
            style = MaterialTheme.typography.labelSmall)

        Text("Ukuran Arab: ${arab.toInt()}"); Slider(arab, { arab = it; scope.launch { store.set(Store.ARAB_SIZE, arab) } }, valueRange = 16f..40f)
        Text("Ukuran Latin: ${latin.toInt()}"); Slider(latin, { latin = it; scope.launch { store.set(Store.LATIN_SIZE, latin) } }, valueRange = 12f..24f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(showL, { showL = !showL; scope.launch { store.set(Store.SHOW_LATIN, showL) } }, { Text("Latin") })
            FilterChip(showT, { showT = !showT; scope.launch { store.set(Store.SHOW_TRANS, showT) } }, { Text("Terjemahan") })
        }
        Text("Mazhab Asar:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(asar == "syafii", { asar = "syafii"; scope.launch { store.set(Store.ASAR, asar) } }, { Text("Syafi'i") })
            FilterChip(asar == "hanafi", { asar = "hanafi"; scope.launch { store.set(Store.ASAR, asar) } }, { Text("Hanafi") })
        }

        Text("Mazhab panduan ibadah", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("syafii" to "Syafi'i", "hanafi" to "Hanafi", "maliki" to "Maliki", "hambali" to "Hambali").forEach { (key, label) ->
                FilterChip(mazhab == key, { mazhab = key; scope.launch { store.set(Store.MADHAB, key) } }, { Text(label) })
            }
        }
        Text("Menentukan bacaan yang ditampilkan di panduan sholat — mis. doa qunut hanya muncul untuk mazhab Syafi'i. Perbedaan mazhab dicatat, tidak dicampuradukkan.",
            style = MaterialTheme.typography.labelSmall)

        Text("Sumber jadwal sholat", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = {}, enabled = false, label = { Text("Hitung lokal (offline)") })
            FilterChip(selected = false, onClick = {}, enabled = false, label = { Text("SIHAT online (belum tersedia)") })
        }
        Text("Perhitungan dilakukan di perangkat (algoritma Meeus, metode Kemenag/SIHAT) sehingga tetap akurat tanpa internet. Sumber online resmi belum diaktifkan karena SIHAT Bimas Islam tidak menyediakan API publik untuk aplikasi pihak ketiga.",
            style = MaterialTheme.typography.labelSmall)

        Text("Koreksi kalender Hijriah: ${if (hijriCorr > 0) "+" else ""}$hijriCorr hari",
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { hijriCorr = (hijriCorr - 1).coerceAtLeast(-2); scope.launch { store.set(Store.HIJRI_CORR, hijriCorr) } }) { Text("−") }
            OutlinedButton(onClick = { hijriCorr = (hijriCorr + 1).coerceAtMost(2); scope.launch { store.set(Store.HIJRI_CORR, hijriCorr) } }) { Text("+") }
        }
        Text("Tanggal Hijriah dihitung tabular (perkiraan) dan dapat berselisih dengan isbat Kemenag; samakan dengan hasil sidang isbat lewat koreksi ini.",
            style = MaterialTheme.typography.labelSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Text("Pengingat ibadah"); Switch(notif, {
                notif = it
                scope.launch {
                    store.set(Store.NOTIF, notif)
                    PrayerScheduler.scheduleAll(ctx.applicationContext)
                }
            })
        }
        Text("Backup / pulihkan data lokal:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    // Gabungkan preferensi (DataStore) + data terstruktur (Room) dalam satu berkas.
                    backup = JSONObject()
                        .put("version", 1)
                        .put("prefs", JSONObject(store.exportJson()))
                        .put("userData", JSONObject(userData.exportJson()))
                        .toString()
                    ctx.copyText("backup", backup)
                }
            }) { Text("Salin backup") }
            Button(onClick = {
                scope.launch {
                    val root = runCatching { JSONObject(backup) }.getOrNull()
                    if (root != null && root.has("prefs")) {
                        store.importJson(root.getJSONObject("prefs").toString())
                        userData.restoreJson(root.optJSONObject("userData")?.toString() ?: "{}")
                    } else {
                        // Kompatibel dengan backup lama (hanya preferensi).
                        store.importJson(backup)
                    }
                }
            }) { Text("Pulihkan") }
        }
        Text("Backup memuat pengaturan, favorit, bookmark, riwayat, dan progres. Simpan berkasnya di tempat aman.",
            style = MaterialTheme.typography.labelSmall)
        OutlinedTextField(backup, { backup = it }, label = { Text("Data backup") }, modifier = Modifier.fillMaxWidth())
        Text("Kota: $city (ubah di menu Jadwal). Susunan Tahlil: lazim Indonesia (lihat modul Tahlil).",
            style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))

        Text("Kompas kiblat", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(compassUnit == "derajat",
                { compassUnit = "derajat"; scope.launch { store.set(Store.COMPASS_UNIT, compassUnit) } },
                { Text("Derajat") })
            FilterChip(compassUnit == "mil",
                { compassUnit = "mil"; scope.launch { store.set(Store.COMPASS_UNIT, compassUnit) } },
                { Text("Mil (6400)") })
        }
        Text("Mengubah satuan tampilan azimut di layar Kiblat. Perhitungan tetap memakai derajat.",
            style = MaterialTheme.typography.labelSmall)

        Text("Tentang & sumber", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp))
        Text("Yasin dan Tahlil — versi ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.labelMedium)
        Text("Lisensi kode: Apache License 2.0 (lihat berkas LICENSE). Konten keagamaan tidak tercakup lisensi kode dan tetap tunduk pada sumber resminya.",
            style = MaterialTheme.typography.labelSmall)
        Text("Teks Qur'an: LPMQ Kemenag MSI (quran.kemenag.go.id). Jadwal: metode SIHAT Bimas Islam (sihat.kemenag.dev). Kiblat: SIHAT E-Kiblat. Hadis: sunnah.com. Izin: lokasi (kiblat/jadwal), notifikasi (pengingat), getar (timer iqomah). Data tersimpan lokal; lokasi tidak dikirim ke server mana pun.",
            style = MaterialTheme.typography.labelSmall)
    }
}
