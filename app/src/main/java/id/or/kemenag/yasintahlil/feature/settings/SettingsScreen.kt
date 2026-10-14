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
import id.or.kemenag.yasintahlil.core.data.FormatCadangan
import id.or.kemenag.yasintahlil.core.data.PreferensiSusunan
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.data.TahlilSusunan
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.domain.model.RingkasanData
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import id.or.kemenag.yasintahlil.core.scheduling.PrayerScheduler
import id.or.kemenag.yasintahlil.core.sholat.Mazhab
import id.or.kemenag.yasintahlil.core.util.ContentValidator
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.KunciProgres
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.core.util.PengingatAdzan
import id.or.kemenag.yasintahlil.core.util.PesanKonten
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.navigation.Routes
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Tab Pengaturan — **satu-satunya** tempat mengubah pengaturan aplikasi.
 *
 * Dahulu pengaturan bercerai-berai: tiap layar bacaan membawa tombol "Pengaturan bacaan" yang
 * membuka bottom sheet-nya sendiri, chip "Tetap menyala" hidup hanya di layar Yasin, dan layar
 * ini sendiri berupa daftar panjang tanpa kelompok. Kini semuanya tinggal di sini, dikelompokkan
 * menurut maksudnya: Tampilan, [NamaAntarmuka.PENGATURAN_BACAAN], Ibadah, Jadwal & lokasi,
 * Kompas kiblat, Data lokal, lalu Tentang.
 *
 * Kendali yang memang milik layar lainnya tetap di layarnya (mis. pemilih metode hisab di menu
 * Jadwal, saklar per-waktu adzan di menu Adzan): keduanya bagian dari cara kerja layar itu,
 * bukan preferensi aplikasi.
 */
@Composable
fun SettingsScreen(go: (String) -> Unit = {}) {
    val store = rememberStore()
    val repo = rememberRepo()
    val userData = rememberUserData()
    val migrasi = rememberMigrasiWarisan()
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
    // "Tetap menyala" dahulu chip lokal di layar Yasin; kini preferensi global yang dibaca layar
    // itu, sehingga nilainya juga berlaku setelah aplikasi ditutup.
    var tetapMenyala by remember { mutableStateOf(BawaanPreferensi.TETAP_MENYALA) }
    var infoCard by remember { mutableStateOf(BawaanPreferensi.KARTU_INFO) }
    var asar by remember { mutableStateOf(JadwalHariIni.ASAR_DEFAULT) }
    var mazhab by remember { mutableStateOf(Mazhab.DEFAULT) }
    var city by remember { mutableStateOf(JadwalHariIni.KOTA_DEFAULT) }
    // Metode hisab yang sedang berlaku. Layar ini **tidak** punya pemilihnya (pemilihnya di menu
    // Jadwal), tetapi keterangan "Sumber jadwal sholat" di bawah menyebut metode yang dipakai —
    // jadi namanya harus dibaca dari `Store.METHOD`, bukan diketik tetap "Kemenag/SIHAT".
    var metode by remember { mutableStateOf(PrayerCalc.Method.from(JadwalHariIni.METODE_DEFAULT)) }
    var notif by remember { mutableStateOf(BawaanPreferensi.PENGINGAT_WAKTU) }
    var hijriCorr by remember { mutableStateOf(BawaanPreferensi.KOREKSI_HIJRIAH) }
    var compassUnit by remember { mutableStateOf(TampilanKompas.SATUAN_DEFAULT) }
    var compassView by remember { mutableStateOf(TampilanKompas.KOMPAS) }
    // Susunan tahlil yang boleh dipilih dan yang sedang aktif — dibaca dari bundel konten plus
    // "Susunan saya" bila pengguna pernah menyusunnya di modul Tahlil.
    var susunanTersedia by remember { mutableStateOf<List<TahlilSusunan>>(emptyList()) }
    var susunanAktif by remember { mutableStateOf(BawaanPreferensi.SUSUNAN_TAHLIL_DIPILIH) }
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
        tetapMenyala = store.get(Store.KEEP_ON, BawaanPreferensi.TETAP_MENYALA)
        infoCard = store.get(Store.INFO_CARD, BawaanPreferensi.KARTU_INFO)
        asar = store.get(Store.ASAR, JadwalHariIni.ASAR_DEFAULT)
        city = store.get(Store.CITY, JadwalHariIni.KOTA_DEFAULT)
        metode = PrayerCalc.Method.from(store.get(Store.METHOD, JadwalHariIni.METODE_DEFAULT))
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

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(NamaLayar.PENGATURAN, style = MaterialTheme.typography.titleLarge)

        // ---- Tampilan ------------------------------------------------------------------------
        BagianJudul("Tampilan")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TemaAplikasi.SEMUA.forEach { t ->
                FilterChip(theme == t, { theme = t; scope.launch { store.set(Store.DARK, t) } }, { Text(t) })
            }
        }
        Text("Tema halaman: ${TemaHalaman.label(pageTheme)} — warna kertas bacaan, tanpa menyentuh teksnya.",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TemaHalaman.SEMUA.forEach { t ->
                FilterChip(pageTheme == t,
                    { pageTheme = t; scope.launch { store.set(Store.PAGE_THEME, t) } },
                    { Text(TemaHalaman.label(t)) })
            }
        }
        BarisSaklar(
            "Kartu info di beranda",
            "Kartu \"Info Islami harian\" di beranda dapat dimatikan.",
            infoCard,
        ) { infoCard = it; scope.launch { store.set(Store.INFO_CARD, infoCard) } }

        // ---- Pengaturan bacaan ---------------------------------------------------------------
        // Judul bagian memakai nama bersama yang sama dengan yang dijanjikan seluruh layar bacaan:
        // pengaturan ini berlaku untuk semua bacaan, dan namanya satu.
        BagianJudul(NamaAntarmuka.PENGATURAN_BACAAN)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(showA, { showA = !showA; scope.launch { store.set(Store.SHOW_ARAB, showA) } }, { Text(NamaAntarmuka.TAMPIL_ARAB) })
            FilterChip(showL, { showL = !showL; scope.launch { store.set(Store.SHOW_LATIN, showL) } }, { Text(NamaAntarmuka.TAMPIL_LATIN) })
            FilterChip(showT, { showT = !showT; scope.launch { store.set(Store.SHOW_TRANS, showT) } }, { Text(NamaAntarmuka.TAMPIL_TERJEMAHAN) })
        }
        Text("Berlaku di semua layar bacaan (Yasin, Tahlil, Dzikir, Adzan, panduan sholat).",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
        // Rentang dan bawaannya dari aturan bersama (`UkuranBacaan`), dan **namanya** pun dari
        // kosakata bersama `NamaAntarmuka` — keduanya mengatur kunci `Store` yang sama, jadi dua
        // ejaan berarti dua pengaturan di mata pengguna.
        Text("${NamaAntarmuka.UKURAN_ARAB}: ${arab.toInt()}", style = MaterialTheme.typography.labelLarge)
        Slider(arab, { arab = it; scope.launch { store.set(Store.ARAB_SIZE, arab) } }, valueRange = UkuranBacaan.ARAB)
        Text("${NamaAntarmuka.UKURAN_LATIN}: ${latin.toInt()}", style = MaterialTheme.typography.labelLarge)
        Slider(latin, { latin = it; scope.launch { store.set(Store.LATIN_SIZE, latin) } }, valueRange = UkuranBacaan.LATIN)
        // "Jarak antarbaris" juga preferensi bacaan global (`plan.txt` §"Fitur Surat Yasin":
        // *"Pengaturan jarak antarbaris dan tema halaman"*); labelnya satu angka di belakang koma.
        Text("${NamaAntarmuka.JARAK_ANTARBARIS}: ${"%.1f".format(lineHeight)}", style = MaterialTheme.typography.labelLarge)
        Slider(lineHeight, { lineHeight = it; scope.launch { store.set(Store.LINE_HEIGHT, lineHeight) } }, valueRange = UkuranBacaan.JARAK)
        BarisSaklar(
            "Tetap menyala saat membaca",
            "Layar tidak padam selama membaca di layar Yasin.",
            tetapMenyala,
        ) { tetapMenyala = it; scope.launch { store.set(Store.KEEP_ON, tetapMenyala) } }

        // ---- Ibadah & mazhab -------------------------------------------------------------------
        // Nama bagian ini bukan "Ibadah" polos: itu nama tab/layar bersama (`NamaLayar.IBADAH`),
        // dan mengejanya sendiri di sini membuat dua nama untuk satu hal.
        BagianJudul("Ibadah & mazhab")
        Text("Mazhab Asar", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Nama mazhabnya dari aturan bersama (`JadwalHariIni.ASAR_SEMUA`), sama dengan chip di
            // menu Jadwal: dua nama untuk satu nilai membuat pengguna mengira ada dua pengaturan.
            JadwalHariIni.ASAR_SEMUA.forEach { (kunci, label) ->
                FilterChip(asar == kunci, {
                    asar = kunci
                    scope.launch {
                        store.set(Store.ASAR, kunci)
                        // Mazhab Asar menggeser jam Asar, jadi pengingat yang sudah terjadwal
                        // harus disusun ulang — menyimpan nilainya saja meninggalkan jam yang
                        // diingatkan memakai mazhab lama.
                        PrayerScheduler.scheduleAll(ctx.applicationContext)
                    }
                }, { Text(label) })
            }
        }
        Text("Mazhab panduan ibadah", style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Mazhab.SEMUA.forEach { (key, label) ->
                FilterChip(mazhab == key, { mazhab = key; scope.launch { store.set(Store.MADHAB, key) } }, { Text(label) })
            }
        }
        Text("Menentukan bacaan di panduan sholat — mis. doa qunut hanya untuk mazhab Syafi'i.",
            style = MaterialTheme.typography.labelSmall)
        Text("Susunan Tahlil", style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 8.dp))
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
                    // Catatan tradisi dibaca dari bundel, sama seperti di modul Tahlil — bukan
                    // diketik ulang di sini.
                    repo.tahlilCatatanTradisi().ifBlank { "Susunan dapat berbeda menurut tradisi." },
                style = MaterialTheme.typography.labelSmall)
            // `plan.txt` §"Fitur Tahlil Lengkap" meminta susunan *"berdasarkan sumber atau tradisi
            // yang jelas"*; sumbernya harus terlihat di sini juga, bukan hanya di modul Tahlil.
            val sumberAktif = PreferensiSusunan.sumberAktif(susunanAktif, susunanTersedia)
            Text(if (sumberAktif.isNotBlank()) "Sumber susunan aktif: $sumberAktif"
                    else "Sumber susunan aktif: tidak diklaim — susunan ini disusun sendiri oleh Anda.",
                style = MaterialTheme.typography.labelSmall)
        }

        // ---- Jadwal & lokasi ------------------------------------------------------------------
        BagianJudul("Jadwal & lokasi")
        BarisSaklar(
            PengingatAdzan.NAMA_PENGINGAT_WAKTU,
            "Pengingat memakai lokasi & metode hisab dari menu Jadwal.",
            notif,
        ) { notif = it; scope.launch { store.set(Store.NOTIF, notif); PrayerScheduler.scheduleAll(ctx.applicationContext) } }
        Text("Sumber jadwal sholat: hitung lokal (offline, algoritma Meeus, metode ${metode.label} — " +
                "${JadwalHariIni.metodeRingkas(metode)}). Metode, kota, dan zona waktunya diubah di " +
                "menu Jadwal; sumber online resmi belum tersedia karena SIHAT Bimas Islam tidak " +
                "menyediakan API publik.",
            style = MaterialTheme.typography.labelSmall)
        Text("Kota saat ini: $city (ubah di menu Jadwal).", style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp))
        Text("Koreksi kalender Hijriah: ${if (hijriCorr > 0) "+" else ""}$hijriCorr hari",
            style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Batas koreksi dari aturan bersama (`BawaanPreferensi`), bukan angka yang diingat
            // masing-masing tombol: tombol dan nilai bawaan tidak dapat berbeda pendapat lagi.
            OutlinedButton(onClick = { hijriCorr = BawaanPreferensi.koreksiHijriah(hijriCorr - 1); scope.launch { store.set(Store.HIJRI_CORR, hijriCorr) } }) { Text("−") }
            OutlinedButton(onClick = { hijriCorr = BawaanPreferensi.koreksiHijriah(hijriCorr + 1); scope.launch { store.set(Store.HIJRI_CORR, hijriCorr) } }) { Text("+") }
        }
        Text("Tanggal Hijriah dihitung tabular (perkiraan); samakan dengan hasil sidang isbat lewat koreksi ini.",
            style = MaterialTheme.typography.labelSmall)

        // ---- Kompas kiblat --------------------------------------------------------------------
        BagianJudul("Kompas kiblat")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TampilanKompas.SATUAN.forEach { u ->
                FilterChip(compassUnit == u,
                    { compassUnit = u; scope.launch { store.set(Store.COMPASS_UNIT, compassUnit) } },
                    { Text(TampilanKompas.labelSatuan(u)) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            TampilanKompas.SEMUA.forEach { t ->
                FilterChip(compassView == t,
                    { compassView = t; scope.launch { store.set(Store.COMPASS_VIEW, compassView) } },
                    { Text(TampilanKompas.label(t)) })
            }
        }
        Text("Mengubah satuan azimut dan gambar jarum kompas di layar Kiblat. Perhitungan tetap memakai derajat.",
            style = MaterialTheme.typography.labelSmall)

        // ---- Data lokal -----------------------------------------------------------------------
        BagianJudul("Data lokal")
        val r = ringkasan
        if (r == null) {
            Text("Menghitung isi data yang tersimpan…", style = MaterialTheme.typography.labelSmall)
        } else {
            Text("Tersimpan di perangkat ini: ${r.favorit} favorit, ${r.bookmark} bookmark, " +
                    "${r.riwayat} riwayat, ${r.progres} progres. Tidak ada yang dikirim ke server.",
                style = MaterialTheme.typography.labelSmall)
            BagianData.entries.forEach { bagian ->
                val n = bagian.jumlah(r)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${bagian.label}: $n", style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(onClick = { hapusTarget = bagian }, enabled = n > 0) { Text(NamaAntarmuka.HAPUS) }
                }
            }
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
                    }) { Text(NamaAntarmuka.HAPUS) }
                },
                dismissButton = { TextButton(onClick = { hapusTarget = null }) { Text(NamaAntarmuka.BATAL) } },
            )
        }
        Text("Backup / pulihkan data lokal", style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    // Gabungkan preferensi (DataStore) + data terstruktur (Room) dalam satu berkas.
                    // Bentuk berkasnya tinggal di `FormatCadangan` supaya penulis dan pembacanya
                    // tak dapat berbeda ejaan — lihat KDoc-nya.
                    backup = JSONObject()
                        .put(FormatCadangan.KUNCI_VERSI, FormatCadangan.VERSI)
                        .put(FormatCadangan.KUNCI_PREFERENSI, JSONObject(store.exportJson()))
                        .put(FormatCadangan.KUNCI_DATA_PENGGUNA, JSONObject(userData.exportJson()))
                        .toString()
                    ctx.copyText("backup", backup)
                }
            }) { Text("Salin backup") }
            Button(onClick = {
                scope.launch {
                    val root = runCatching { JSONObject(backup) }.getOrNull()
                    if (root != null && root.has(FormatCadangan.KUNCI_PREFERENSI)) {
                        store.importJson(
                            root.getJSONObject(FormatCadangan.KUNCI_PREFERENSI).toString())
                        // Bagian data pengguna hanya dipulihkan bila berkasnya memang memuatnya:
                        // berkas yang memuat preferensi tanpa data terstruktur tak boleh
                        // mengosongkan favorit/bookmark/riwayat/progres yang sedang tersimpan.
                        root.optJSONObject(FormatCadangan.KUNCI_DATA_PENGGUNA)
                            ?.let { userData.restoreJson(it.toString()) }
                    } else {
                        // Kompatibel dengan backup lama (hanya preferensi). Berkas cadangan v1.0.0
                        // masih memuat kunci warisan, jadi pemindahnya dijalankan lagi.
                        store.importJson(backup)
                    }
                    migrasi.jalankan()
                    ringkasanUlang++
                }
            }) { Text("Pulihkan") }
        }
        Text("Backup memuat pengaturan, favorit, bookmark, riwayat, dan progres. Simpan berkasnya di tempat aman.",
            style = MaterialTheme.typography.labelSmall)
        OutlinedTextField(backup, { backup = it }, label = { Text("Data backup") }, modifier = Modifier.fillMaxWidth())

        // ---- Pembaruan konten & tentang ----------------------------------------------------------
        // Judul bagian dan label tombolnya menyebut layar tujuan dengan nama bersama, dan judulnya
        // ditulis `Text(NamaLayar.…)` — bukan `BagianJudul` — karena NavSmokeTest mencari teks
        // judul ini persis namanya saat menggulir layar Pengaturan.
        Text(NamaLayar.PEMBARUAN_KONTEN, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
        OutlinedButton(onClick = { go(Routes.KONTEN_UPDATE) }) { Text("Buka ${NamaLayar.PEMBARUAN_KONTEN}") }
        Text("Memasang bundel konten pilihan Anda setelah diperiksa sumbernya. Tanpa unduhan otomatis dan tanpa izin jaringan.",
            style = MaterialTheme.typography.labelSmall)

        Text(NamaLayar.TENTANG, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
        Text("Yasin dan Tahlil — versi ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { go(Routes.TENTANG) }) { Text("Buka ${NamaLayar.TENTANG}") }
        Text("Lisensi kode: Apache License 2.0. Konten keagamaan tetap tunduk pada sumber resminya " +
                "(${ContentValidator.allowedHosts.joinToString()}); keterangan pemilik tiap domain " +
                "ada di ${NamaLayar.TENTANG}. Izin: lokasi (kiblat/jadwal), notifikasi (pengingat), " +
                "getar (timer iqomah). Data tersimpan lokal; tidak ada yang dikirim ke server.",
            style = MaterialTheme.typography.labelSmall)
    }
}

/** Baris pengaturan bersaklar: nama, satu baris catatan, dan saklarnya di kanan. */
@Composable
private fun BarisSaklar(
    judul: String,
    catatan: String,
    nilai: Boolean,
    ubah: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(judul, style = MaterialTheme.typography.titleSmall)
            Text(catatan, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(nilai, ubah)
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
