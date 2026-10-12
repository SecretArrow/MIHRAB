package id.or.kemenag.yasintahlil.feature.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.BuildConfig
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.PreferensiSusunan
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.data.TahlilSusunan
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.domain.model.RingkasanData
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.core.sholat.Mazhab
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.KunciProgres
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.PengingatAdzan
import id.or.kemenag.yasintahlil.core.util.PesanKonten
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.Routes
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SettingsScreen(go: (String) -> Unit = {}) {
    val store = rememberStore()
    val repo = rememberRepo()
    val userData = rememberUserData()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var theme by remember { mutableStateOf(TemaAplikasi.SISTEM) }
    var arab by remember { mutableStateOf(UkuranBacaan.ARAB_DEFAULT) }
    var latin by remember { mutableStateOf(UkuranBacaan.LATIN_DEFAULT) }
    var lineHeight by remember { mutableStateOf(UkuranBacaan.JARAK_DEFAULT) }
    var pageTheme by remember { mutableStateOf(TemaHalaman.IKUT) }
    var showL by remember { mutableStateOf(BawaanPreferensi.TAMPIL_LATIN) }
    var showT by remember { mutableStateOf(BawaanPreferensi.TAMPIL_TERJEMAHAN) }
    var showA by remember { mutableStateOf(BawaanPreferensi.TAMPIL_ARAB) }
    var infoCard by remember { mutableStateOf(BawaanPreferensi.KARTU_INFO) }
    var asar by remember { mutableStateOf(JadwalHariIni.ASAR_DEFAULT) }
    var mazhab by remember { mutableStateOf(Mazhab.DEFAULT) }
    var city by remember { mutableStateOf(JadwalHariIni.KOTA_DEFAULT) }
    var notif by remember { mutableStateOf(BawaanPreferensi.PENGINGAT_WAKTU) }
    var hijriCorr by remember { mutableStateOf(BawaanPreferensi.KOREKSI_HIJRIAH) }
    var compassUnit by remember { mutableStateOf(TampilanKompas.SATUAN_DEFAULT) }
    var compassView by remember { mutableStateOf(TampilanKompas.KOMPAS) }
    // Susunan tahlil yang boleh dipilih dan yang sedang aktif — dibaca dari bundel konten plus
    // "Susunan saya" bila pengguna pernah menyusunnya di modul Tahlil.
    var susunanTersedia by remember { mutableStateOf<List<TahlilSusunan>>(emptyList()) }
    var susunanAktif by remember { mutableStateOf("") }
    var backup by remember { mutableStateOf("") }
    var ringkasan by remember { mutableStateOf<RingkasanData?>(null) }
    var ringkasanUlang by remember { mutableStateOf(0) }
    var hapusTarget by remember { mutableStateOf<BagianData?>(null) }
    LaunchedEffect(Unit) {
        theme = store.get(Store.DARK, TemaAplikasi.SISTEM)
        arab = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT)
        latin = store.get(Store.LATIN_SIZE, UkuranBacaan.LATIN_DEFAULT)
        lineHeight = store.get(Store.LINE_HEIGHT, UkuranBacaan.JARAK_DEFAULT)
        pageTheme = store.get(Store.PAGE_THEME, TemaHalaman.IKUT)
        showL = store.get(Store.SHOW_LATIN, BawaanPreferensi.TAMPIL_LATIN)
        showT = store.get(Store.SHOW_TRANS, BawaanPreferensi.TAMPIL_TERJEMAHAN)
        showA = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB)
        infoCard = store.get(Store.INFO_CARD, BawaanPreferensi.KARTU_INFO)
        asar = store.get(Store.ASAR, JadwalHariIni.ASAR_DEFAULT)
        city = store.get(Store.CITY, JadwalHariIni.KOTA_DEFAULT)
        mazhab = store.get(Store.MADHAB, Mazhab.DEFAULT)
        notif = store.get(Store.NOTIF, BawaanPreferensi.PENGINGAT_WAKTU)
        hijriCorr = store.get(Store.HIJRI_CORR, BawaanPreferensi.KOREKSI_HIJRIAH)
        compassUnit = store.get(Store.COMPASS_UNIT, TampilanKompas.SATUAN_DEFAULT)
        compassView = store.get(Store.COMPASS_VIEW, TampilanKompas.KOMPAS)
        val daftarSusunan = PreferensiSusunan.tersedia(
            repo.tahlilSusunan(),
            PreferensiSusunan.susunanSaya(
                PreferensiSusunan.parse(store.get(Store.TAHLIL_CUSTOM, BawaanPreferensi.SUSUNAN_SAYA))),
        )
        susunanTersedia = daftarSusunan
        susunanAktif = PreferensiSusunan.idAktif(
            store.get(Store.TAHLIL_SUSUNAN, BawaanPreferensi.SUSUNAN_TAHLIL_DIPILIH), daftarSusunan)
    }
    // Dimuat ulang setiap kali satu bagian data dihapus, agar angkanya tidak berbohong.
    LaunchedEffect(ringkasanUlang) { ringkasan = userData.ringkasan() }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Text(NamaLayar.PENGATURAN, style = MaterialTheme.typography.titleLarge)
        Text("Tema:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TemaAplikasi.SEMUA.forEach { t ->
                FilterChip(theme == t, { theme = t; scope.launch { store.set(Store.DARK, t) } }, { Text(t) })
            }
        }
        Text("Bahasa aplikasi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text("Indonesia — satu-satunya bahasa antarmuka. Teks Arab tetap Arab, dan terjemahan yang " +
                "disajikan adalah terjemahan resmi Kemenag 2019 berbahasa Indonesia. Belum ada " +
                "terjemahan antarmuka lain, jadi tidak ada tombol bahasa yang tidak berfungsi.",
            style = MaterialTheme.typography.labelSmall)

        // Rentang dan bawaannya dipakai bersama bottom sheet pengaturan bacaan (satu sumber
        // kebenaran: `UkuranBacaan`).
        Text("Ukuran Arab: ${arab.toInt()}"); Slider(arab, { arab = it; scope.launch { store.set(Store.ARAB_SIZE, arab) } }, valueRange = UkuranBacaan.ARAB)
        Text("Ukuran Latin: ${latin.toInt()}"); Slider(latin, { latin = it; scope.launch { store.set(Store.LATIN_SIZE, latin) } }, valueRange = UkuranBacaan.LATIN)
        // "Jarak antarbaris" dan "tema halaman" juga preferensi bacaan global (`plan.txt`
        // §"Fitur Surat Yasin": *"Pengaturan jarak antarbaris dan tema halaman"*). Keduanya sudah
        // ada di sheet pengaturan bacaan, tetapi sebelumnya tidak ada di layar Pengaturan — padahal
        // "Ukuran Arab" dan "Ukuran Latin" ada di sini. Menaruhnya di sini membuat keempat
        // preferensi bacaan dapat ditemukan di satu tempat, menulis kunci yang sama.
        Text("Jarak antarbaris: ${"%.1f".format(lineHeight)}")
        Slider(lineHeight, { lineHeight = it; scope.launch { store.set(Store.LINE_HEIGHT, lineHeight) } }, valueRange = UkuranBacaan.JARAK)
        Text("Tema halaman bacaan:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TemaHalaman.SEMUA.forEach { t ->
                FilterChip(pageTheme == t,
                    { pageTheme = t; scope.launch { store.set(Store.PAGE_THEME, t) } },
                    { Text(TemaHalaman.label(t)) })
            }
        }
        Text("Tema halaman hanya mengubah warna kertas bacaan (krem/gelap), tanpa menyentuh teks " +
                "atau sumbernya. Berlaku juga di sheet pengaturan bacaan.",
            style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(showA, { showA = !showA; scope.launch { store.set(Store.SHOW_ARAB, showA) } }, { Text("Arab") })
            FilterChip(showL, { showL = !showL; scope.launch { store.set(Store.SHOW_LATIN, showL) } }, { Text("Latin") })
            FilterChip(showT, { showT = !showT; scope.launch { store.set(Store.SHOW_TRANS, showT) } }, { Text("Terjemahan") })
        }
        Text("Berlaku di semua layar bacaan (Yasin, Tahlil, Dzikir, Adzan, panduan sholat). " +
                "Menyembunyikan Arab berguna saat ingin membaca Latin/terjemahan saja.",
            style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Kartu info di beranda")
            Switch(infoCard, {
                infoCard = it
                scope.launch { store.set(Store.INFO_CARD, infoCard) }
            })
        }
        Text("Mazhab Asar:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Nama mazhabnya dari aturan bersama (`JadwalHariIni.ASAR_SEMUA`), sama dengan chip di
            // menu Jadwal: dua nama untuk satu nilai membuat pengguna mengira ada dua pengaturan.
            JadwalHariIni.ASAR_SEMUA.forEach { (kunci, label) ->
                FilterChip(asar == kunci, {
                    asar = kunci
                    scope.launch {
                        store.set(Store.ASAR, kunci)
                        // Mazhab Asar menggeser jam Asar, jadi pengingat yang sudah terjadwal harus
                        // disusun ulang — menu Jadwal melakukannya, layar ini dahulu hanya menyimpan
                        // nilainya, sehingga jam yang diingatkan tetap memakai mazhab lama sampai
                        // ada hal lain yang memicu penjadwalan ulang.
                        PrayerScheduler.scheduleAll(ctx.applicationContext)
                    }
                }, { Text(label) })
            }
        }

        Text("Mazhab panduan ibadah", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Mazhab.SEMUA.forEach { (key, label) ->
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
            // Batas koreksi dari aturan bersama (`BawaanPreferensi`), bukan angka yang diingat
            // masing-masing tombol: tombol dan nilai bawaan tidak dapat berbeda pendapat lagi.
            OutlinedButton(onClick = { hijriCorr = BawaanPreferensi.koreksiHijriah(hijriCorr - 1); scope.launch { store.set(Store.HIJRI_CORR, hijriCorr) } }) { Text("−") }
            OutlinedButton(onClick = { hijriCorr = BawaanPreferensi.koreksiHijriah(hijriCorr + 1); scope.launch { store.set(Store.HIJRI_CORR, hijriCorr) } }) { Text("+") }
        }
        Text("Tanggal Hijriah dihitung tabular (perkiraan) dan dapat berselisih dengan isbat Kemenag; samakan dengan hasil sidang isbat lewat koreksi ini.",
            style = MaterialTheme.typography.labelSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Text(PengingatAdzan.NAMA_PENGINGAT_WAKTU); Switch(notif, {
                notif = it
                scope.launch {
                    store.set(Store.NOTIF, notif)
                    PrayerScheduler.scheduleAll(ctx.applicationContext)
                }
            })
        }
        Text("Manajemen data offline", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp))
        val r = ringkasan
        if (r == null) {
            Text("Menghitung isi data yang tersimpan…", style = MaterialTheme.typography.labelSmall)
        } else {
            Text("Tersimpan di perangkat ini: ${r.favorit} favorit, ${r.bookmark} bookmark, " +
                    "${r.riwayat} riwayat, ${r.progres} progres. Tidak ada data ini yang dikirim ke server.",
                style = MaterialTheme.typography.labelSmall)
            BagianData.entries.forEach { bagian ->
                val n = bagian.jumlah(r)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${bagian.label}: $n", style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(onClick = { hapusTarget = bagian }, enabled = n > 0) { Text("Hapus") }
                }
            }
            Text("Menghapus satu bagian tidak menyentuh bagian lain, jadi riwayat saja dapat dibuang " +
                    "tanpa kehilangan favorit dan bookmark.",
                style = MaterialTheme.typography.labelSmall)
        }
        hapusTarget?.let { bagian ->
            val n = bagian.jumlah(ringkasan ?: RingkasanData())
            AlertDialog(
                onDismissRequest = { hapusTarget = null },
                title = { Text("Hapus ${bagian.label.lowercase()}?") },
                text = {
                    Text("$n entri ${bagian.label.lowercase()} akan dihapus dari perangkat ini dan " +
                            "tidak dapat dikembalikan. Bagian lain tidak tersentuh.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        hapusTarget = null
                        scope.launch {
                            bagian.hapus(userData)
                            ringkasanUlang++
                        }
                    }) { Text("Hapus") }
                },
                dismissButton = { TextButton(onClick = { hapusTarget = null }) { Text("Batal") } },
            )
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
        Text("Kota: $city (ubah di menu Jadwal).", style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 8.dp))

        Text("Susunan Tahlil", style = MaterialTheme.typography.titleMedium)
        if (susunanTersedia.isEmpty()) {
            Text(PesanKonten.berkasTidakAda("susunan tahlil"),
                style = MaterialTheme.typography.labelSmall)
        } else {
            // Pemilih hanya muncul bila memang ada yang bisa dipilih; satu chip tunggal hanyalah hiasan.
            if (susunanTersedia.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    susunanTersedia.forEach { s ->
                        FilterChip(susunanAktif == s.id, {
                            susunanAktif = s.id
                            scope.launch {
                                store.set(Store.TAHLIL_SUSUNAN, s.id)
                                // Urutan bagiannya berbeda, jadi posisi baca yang lama tidak lagi
                                // menunjuk bagian yang sama — dikosongkan supaya penandanya tidak
                                // berbohong. Sama seperti saat mengganti susunan di modul Tahlil.
                                userData.setProgress(JenisBacaan.TAHLIL, KunciProgres.TAHLIL_POSISI, 0)
                                userData.setProgress(JenisBacaan.TAHLIL, KunciProgres.TAHLIL_HITUNGAN, 0)
                            }
                        }, { Text(s.nama) })
                    }
                }
            }
            Text("Susunan aktif: ${PreferensiSusunan.namaAktif(susunanAktif, susunanTersedia)}. " +
                    (if (susunanTersedia.size > 1)
                        "Pilihan ini sama dengan pemilih di modul Tahlil (satu kunci preferensi); " +
                            "menggantinya mengosongkan penanda posisi baca karena urutan bagiannya berbeda."
                     else
                        "Baru satu susunan tersedia di bundel konten; susunan lain menunggu rujukan " +
                            "tradisi yang jelas dan tidak disusun sendiri di sini.") +
                    " Susunan dapat berbeda menurut tradisi dan rujukan — tidak ada susunan yang " +
                    "diklaim paling benar.",
                style = MaterialTheme.typography.labelSmall)
            // `plan.txt` §"Fitur Tahlil Lengkap" meminta susunan *"berdasarkan sumber atau tradisi
            // yang jelas"*; sumbernya harus terlihat di sini juga, bukan hanya di modul Tahlil.
            val sumberAktif = PreferensiSusunan.sumberAktif(susunanAktif, susunanTersedia)
            Text(if (sumberAktif.isNotBlank()) "Sumber susunan aktif: $sumberAktif"
                    else "Sumber susunan aktif: tidak diklaim — susunan ini disusun sendiri oleh Anda.",
                style = MaterialTheme.typography.labelSmall)
        }

        Text("Kompas kiblat", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TampilanKompas.SATUAN.forEach { u ->
                FilterChip(compassUnit == u,
                    { compassUnit = u; scope.launch { store.set(Store.COMPASS_UNIT, compassUnit) } },
                    { Text(TampilanKompas.labelSatuan(u)) })
            }
        }
        Text("Mengubah satuan tampilan azimut di layar Kiblat. Perhitungan tetap memakai derajat.",
            style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            TampilanKompas.SEMUA.forEach { t ->
                FilterChip(compassView == t,
                    { compassView = t; scope.launch { store.set(Store.COMPASS_VIEW, t) } },
                    { Text(TampilanKompas.label(t)) })
            }
        }
        Text("Menentukan apakah jarum kompas digambar di layar Kiblat. Pada perangkat tanpa " +
                "magnetometer jarum tidak pernah digambar, karena tanpa sensor arahnya hanya " +
                "dugaan yang tampak meyakinkan.",
            style = MaterialTheme.typography.labelSmall)

        Text("Pembaruan konten", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp))
        OutlinedButton(onClick = { go(Routes.KONTEN_UPDATE) }) { Text("Buka pembaruan konten") }
        Text("Memasang bundel konten pilihan Anda setelah diperiksa sumbernya (domain resmi, " +
                "kategori, harakat). Tidak ada unduhan otomatis dan tidak ada izin jaringan.",
            style = MaterialTheme.typography.labelSmall)

        Text("Tentang & sumber", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp))
        Text("Yasin dan Tahlil — versi ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.labelMedium)
        // Ringkasannya tetap di sini supaya terlihat tanpa membuka apa pun; dokumen lengkapnya
        // (kebijakan privasi, daftar izin beserta alasannya, dan lisensi) ada di layar Tentang —
        // sebelumnya hanya ada di berkas `docs/` yang tak dapat dibaca pengguna aplikasi.
        OutlinedButton(onClick = { go(Routes.TENTANG) }) { Text("Buka Tentang & privasi") }
        Text("Lisensi kode: Apache License 2.0 (lihat berkas LICENSE). Konten keagamaan tidak tercakup lisensi kode dan tetap tunduk pada sumber resminya.",
            style = MaterialTheme.typography.labelSmall)
        Text("Teks Qur'an: LPMQ Kemenag MSI (quran.kemenag.go.id). Jadwal: metode SIHAT Bimas Islam (sihat.kemenag.dev). Kiblat: SIHAT E-Kiblat. Hadis: sunnah.com. Izin: lokasi (kiblat/jadwal), notifikasi (pengingat), getar (timer iqomah). Data tersimpan lokal; lokasi tidak dikirim ke server mana pun.",
            style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Bagian data lokal yang dapat dibuang sendiri-sendiri. Setiap bagian memetakan dirinya ke jumlah
 * yang tampil dan ke operasi hapusnya, supaya tombol "Hapus" tak mungkin menghapus bagian lain.
 */
private enum class BagianData(val label: String) {
    FAVORIT("Favorit"),
    BOOKMARK("Bookmark"),
    RIWAYAT("Riwayat"),
    PROGRES("Progres"),
    ;

    fun jumlah(r: RingkasanData): Int = when (this) {
        FAVORIT -> r.favorit
        BOOKMARK -> r.bookmark
        RIWAYAT -> r.riwayat
        PROGRES -> r.progres
    }

    suspend fun hapus(repo: UserDataRepository) = when (this) {
        FAVORIT -> repo.clearFavorites()
        BOOKMARK -> repo.clearBookmarks()
        RIWAYAT -> repo.clearHistory()
        PROGRES -> repo.clearProgress()
    }
}
