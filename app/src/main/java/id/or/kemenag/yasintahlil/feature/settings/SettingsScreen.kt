package id.or.kemenag.yasintahlil.feature.settings

import androidx.compose.foundation.BorderStroke
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
 * Tab Pengaturan — **satu-satunya** tempat mengubah preferensi aplikasi, dengan satu aturan
 * pemusatan: *satu pengaturan, satu rumah*. Kendali yang sama tidak boleh hidup di dua layar
 * sekaligus — pengguna yang menemukannya di dua tempat mengira ada dua pengaturan, dan salah
 * satunya diam-diam tidak berlaku.
 *
 * Susunannya delapan kelompok dalam kartu: Tampilan, [NamaAntarmuka.PENGATURAN_BACAAN],
 * Ibadah & mazhab, Jadwal & pengingat, Kompas kiblat, Data lokal, [NamaLayar.PEMBARUAN_KONTEN],
 * lalu [NamaLayar.TENTANG].
 *
 * Yang tetap di luar layar ini hanyalah kendali yang merupakan **cara kerja layar itu sendiri**,
 * bukan preferensi: pemilih metode hisab, kota, GPS, dan zona waktu di menu Jadwal (masukan
 * perhitungan jadwalnya), serta saklar per waktu, jenis suara, volume, dan izin di menu Adzan
 * (hub pengingat adzan). Mazhab Asar, satuan & tampilan kompas, durasi & pengingat iqomah, dan
 * pengingat doa setelah adzan dahulu hidup ganda di layar lain — kini masing-masing hanya ada di
 * satu tempat, dan layar asalnya cukup menampilkan hasilnya.
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
    // Penghitung ulangan (+ / − hitungan) di layar Tahlil dan Dzikir: opt-in dari sini.
    var tampilPenghitung by remember { mutableStateOf(BawaanPreferensi.TAMPIL_PENGHITUNG) }
    // Panjang timer iqomah: dahulu tombol − / + di layar Iqomah, kini di sini bersama kendali lain.
    var durasiIqomah by remember { mutableStateOf(PengingatAdzan.IQOMAH_DEFAULT_MENIT) }
    // Saklar pengingat iqomah juga pindah ke sini dari layar Iqomah: ia preferensi notifikasi,
    // bukan cara kerja timer — layar itu kini cukup membaca dan menjalankan hitung mundurnya.
    var ingatkanIqomah by remember { mutableStateOf(BawaanPreferensi.PENGINGAT_IQOMAH) }
    // Saklar pengingat imsak: satu keluarga dengan pengingat iqomah — sama-sama pengingat tambahan
    // yang bermakna hanya bagi sebagian pengguna (imsak: yang berpuasa), maka opt-in dan
    // rumahnya di sini bersama kendali pengingat lainnya.
    var ingatkanImsak by remember { mutableStateOf(BawaanPreferensi.PENGINGAT_IMSAK) }
    // Menit imsak sebelum Subuh; nilai awalnya bawaan bersama, dan pemuatannya lewat penjepit yang
    // sama dengan penjadwal serta pekerja latar agar angkanya tak dapat berbeda.
    var menitImsak by remember { mutableStateOf(BawaanPreferensi.MENIT_IMSAK) }
    // Koreksi menit per waktu sholat: dahulu deretan − / + di layar Jadwal, kini di sini.
    var corr by remember { mutableStateOf(JadwalHariIni.parseCorr(JadwalHariIni.CORR_DEFAULT)) }
    var infoCard by remember { mutableStateOf(BawaanPreferensi.KARTU_INFO) }
    var asar by remember { mutableStateOf(JadwalHariIni.ASAR_DEFAULT) }
    var mazhab by remember { mutableStateOf(Mazhab.DEFAULT) }
    var city by remember { mutableStateOf(JadwalHariIni.KOTA_DEFAULT) }
    // Metode hisab yang sedang berlaku. Layar ini **tidak** punya pemilihnya (pemilihnya di menu
    // Jadwal), tetapi keterangan "Sumber jadwal & lokasi" di bawah menyebut metode yang dipakai —
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
        tampilPenghitung = store.get(Store.SHOW_COUNTER, BawaanPreferensi.TAMPIL_PENGHITUNG)
        durasiIqomah = PengingatAdzan.iqomahMenit(
            store.get(Store.IQOMAH_MIN, PengingatAdzan.IQOMAH_DEFAULT_MENIT))
        ingatkanIqomah = store.get(Store.IQOMAH_ON, BawaanPreferensi.PENGINGAT_IQOMAH)
        ingatkanImsak = store.get(Store.IMSAK_ON, BawaanPreferensi.PENGINGAT_IMSAK)
        menitImsak = BawaanPreferensi.menitImsak(
            store.get(Store.IMSAK_MIN, BawaanPreferensi.MENIT_IMSAK))
        corr = JadwalHariIni.parseCorr(store.get(Store.CORR, JadwalHariIni.CORR_DEFAULT))
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

    /** Ubah koreksi satu waktu sholat, simpan, lalu susun ulang pengingatnya. */
    fun ubahKoreksi(i: Int, delta: Int) {
        // Rentangnya dari aturan bersama, sama dengan yang dipakai `parseCorr` saat membaca —
        // batas tombol dan batas jalur baca tidak boleh berbeda.
        corr = corr.toMutableList().also {
            it[i] = (it[i] + delta).coerceIn(
                JadwalHariIni.KOREKSI_MIN_MENIT, JadwalHariIni.KOREKSI_MAKS_MENIT)
        }
        scope.launch {
            store.set(Store.CORR, corr.joinToString(","))
            PrayerScheduler.scheduleAll(ctx.applicationContext)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(NamaLayar.PENGATURAN, style = MaterialTheme.typography.titleLarge)
        Text("Seluruh pengaturan aplikasi tinggal di sini — tiap kendali hanya punya satu rumah.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))

        // ---- Tampilan ------------------------------------------------------------------------
        BagianJudul("Tampilan")
        KartuGrup {
            KolomPreferensi("Tema aplikasi") {
                BarisChip {
                    TemaAplikasi.SEMUA.forEach { t ->
                        FilterChip(theme == t, { theme = t; scope.launch { store.set(Store.DARK, t) } }, { Text(t) })
                    }
                }
            }
            KolomPreferensi(
                "Tema halaman",
                "Warna kertas bacaan — tidak menyentuh teks dan ukurannya.",
            ) {
                BarisChip {
                    TemaHalaman.SEMUA.forEach { t ->
                        FilterChip(pageTheme == t,
                            { pageTheme = t; scope.launch { store.set(Store.PAGE_THEME, t) } },
                            { Text(TemaHalaman.label(t)) })
                    }
                }
            }
            BarisSaklar(
                "Kartu info di beranda",
                "Kartu \"Info Islami harian\" di beranda dapat dimatikan.",
                infoCard,
            ) { infoCard = it; scope.launch { store.set(Store.INFO_CARD, infoCard) } }
        }

        // ---- Pengaturan bacaan ---------------------------------------------------------------
        // Judul bagian memakai nama bersama yang sama dengan yang dijanjikan seluruh layar bacaan:
        // pengaturan ini berlaku untuk semua bacaan, dan namanya satu.
        BagianJudul(NamaAntarmuka.PENGATURAN_BACAAN)
        KartuGrup {
            KolomPreferensi(
                "Bagian yang tampil",
                "Berlaku di semua layar bacaan (Yasin, Tahlil, Dzikir, Adzan, panduan sholat).",
            ) {
                BarisChip {
                    FilterChip(showA, { showA = !showA; scope.launch { store.set(Store.SHOW_ARAB, showA) } }, { Text(NamaAntarmuka.TAMPIL_ARAB) })
                    FilterChip(showL, { showL = !showL; scope.launch { store.set(Store.SHOW_LATIN, showL) } }, { Text(NamaAntarmuka.TAMPIL_LATIN) })
                    FilterChip(showT, { showT = !showT; scope.launch { store.set(Store.SHOW_TRANS, showT) } }, { Text(NamaAntarmuka.TAMPIL_TERJEMAHAN) })
                }
            }
            // Rentang dan bawaannya dari aturan bersama (`UkuranBacaan`), dan **namanya** pun dari
            // kosakata bersama `NamaAntarmuka` — keduanya mengatur kunci `Store` yang sama, jadi dua
            // ejaan berarti dua pengaturan di mata pengguna.
            BarisUkur(NamaAntarmuka.UKURAN_ARAB, arab.toInt().toString(),
                UkuranBacaan.ARAB, arab) { arab = it; scope.launch { store.set(Store.ARAB_SIZE, arab) } }
            BarisUkur(NamaAntarmuka.UKURAN_LATIN, latin.toInt().toString(),
                UkuranBacaan.LATIN, latin) { latin = it; scope.launch { store.set(Store.LATIN_SIZE, latin) } }
            // "Jarak antarbaris" juga preferensi bacaan global (`plan.txt` §"Fitur Surat Yasin":
            // *"Pengaturan jarak antarbaris dan tema halaman"*); labelnya satu angka di belakang koma.
            BarisUkur(NamaAntarmuka.JARAK_ANTARBARIS, "%.1f".format(lineHeight),
                UkuranBacaan.JARAK, lineHeight) { lineHeight = it; scope.launch { store.set(Store.LINE_HEIGHT, lineHeight) } }
            BarisSaklar(
                "Tetap menyala saat membaca",
                "Layar tidak padam selama membaca di layar Yasin.",
                tetapMenyala,
            ) { tetapMenyala = it; scope.launch { store.set(Store.KEEP_ON, tetapMenyala) } }
            BarisSaklar(
                "Penghitung ulangan",
                "Tombol + dan − hitungan di layar Tahlil dan Dzikir; mati secara bawaan agar " +
                    "layar bacaan tetap bersih — hitungan yang pernah tersimpan tidak hilang.",
                tampilPenghitung,
            ) { tampilPenghitung = it; scope.launch { store.set(Store.SHOW_COUNTER, tampilPenghitung) } }
        }

        // ---- Ibadah & mazhab -------------------------------------------------------------------
        // Nama bagian ini bukan "Ibadah" polos: itu nama tab/layar bersama (`NamaLayar.IBADAH`),
        // dan mengejanya sendiri di sini membuat dua nama untuk satu hal. Mazhab Asar dahulu
        // ditawarkan sekali lagi sebagai chip di menu Jadwal — kini rumahnya hanya di sini,
        // berdampingan dengan mazhab panduan supaya kedua pengaturan mazhab mudah ditemukan bersama.
        BagianJudul("Ibadah & mazhab")
        KartuGrup {
            KolomPreferensi(
                "Mazhab Asar",
                "Menentukan cara menghitung jam Asar — jamnya tercetak di menu Jadwal beserta " +
                    "pengingatnya yang ikut dijadwalkan ulang.",
            ) {
                BarisChip {
                    // Nama mazhabnya dari aturan bersama (`JadwalHariIni.ASAR_SEMUA`), sama dengan
                    // yang dipakai perhitungan jadwal: dua nama untuk satu nilai membuat pengguna
                    // mengira ada dua pengaturan.
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
            }
            KolomPreferensi(
                "Mazhab panduan ibadah",
                "Menentukan bacaan di panduan sholat — mis. doa qunut hanya untuk mazhab Syafi'i.",
            ) {
                BarisChip {
                    Mazhab.SEMUA.forEach { (key, label) ->
                        FilterChip(mazhab == key, { mazhab = key; scope.launch { store.set(Store.MADHAB, key) } }, { Text(label) })
                    }
                }
            }
            KolomPreferensi("Susunan Tahlil") {
                if (susunanTersedia.isEmpty()) {
                    Text(PesanKonten.berkasTidakAda("susunan tahlil"),
                        style = MaterialTheme.typography.labelSmall)
                } else {
                    // Pemilih hanya muncul bila memang ada yang bisa dipilih; satu chip tunggal hanyalah hiasan.
                    if (susunanTersedia.size > 1) {
                        BarisChip {
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
            }
        }
        // ---- Jadwal & pengingat ------------------------------------------------------------------
        // Dahulu bernama "Jadwal & lokasi", tetapi kotanya justru dipilih di menu Jadwal; yang
        // tinggal di sini adalah kendali pengingat beserta koreksinya — namanya disesuaikan agar
        // pengguna mencari pengingat di tempat yang namanya mengatakan pengingat.
        BagianJudul("Jadwal & pengingat")
        KartuGrup {
            BarisSaklar(
                PengingatAdzan.NAMA_PENGINGAT_WAKTU,
                "Saklar utama seluruh pengingat waktu sholat. Tiap waktunya punya saklar sendiri " +
                    "di menu Adzan; lokasi & metode hisabnya dari menu Jadwal.",
                notif,
            ) { notif = it; scope.launch { store.set(Store.NOTIF, notif); PrayerScheduler.scheduleAll(ctx.applicationContext) } }
            // Durasi iqomah: − / + -nya dipindah dari layar Iqomah ke sini — satu rumah bagi seluruh
            // pengaturan. Layar itu tetap membaca dan menampilkan angkanya, sama seperti notifikasi latar.
            KolomPreferensi(
                "Durasi iqomah",
                "Panjang hitung mundur di layar Iqomah dan jeda pengingatnya. ${PengingatAdzan.IQOMAH_PERKIRAAN}",
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Batasnya dari aturan bersama, sama dengan yang menjepit nilai tersimpan di penjadwal
                    // dan pekerja latar — kalau tidak, angka di sini bisa tak pernah dipakai pengingatnya.
                    OutlinedButton(onClick = {
                        durasiIqomah = PengingatAdzan.iqomahMenit(durasiIqomah - 1)
                        scope.launch {
                            store.set(Store.IQOMAH_MIN, durasiIqomah)
                            PrayerScheduler.scheduleAll(ctx.applicationContext)
                        }
                    }) { Text("−") }
                    Text("$durasiIqomah mnt", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = {
                        durasiIqomah = PengingatAdzan.iqomahMenit(durasiIqomah + 1)
                        scope.launch {
                            store.set(Store.IQOMAH_MIN, durasiIqomah)
                            PrayerScheduler.scheduleAll(ctx.applicationContext)
                        }
                    }) { Text("+") }
                }
            }
            // Saklar pengingat iqomah pindah dari layar Iqomah: ia preferensi notifikasi, satu
            // keluarga dengan "Durasi iqomah" di atas — layar itu kini murni timer dan bacaannya.
            BarisSaklar(
                "Ingatkan saat perkiraan iqomah habis",
                "Bila aktif: notifikasi $durasiIqomah menit setelah pengingat masuk waktu, tetap " +
                    "berjalan walau aplikasi ditutup, dan mengikuti saklar per waktu di menu Adzan. " +
                    "Ketukannya membuka layar Iqomah.",
                ingatkanIqomah,
            ) { ingatkanIqomah = it; scope.launch { store.set(Store.IQOMAH_ON, ingatkanIqomah); PrayerScheduler.scheduleAll(ctx.applicationContext) } }
            // Pengingat imsak: satu keluarga dengan pengingat iqomah di atas — sama-sama opt-in
            // karena hanya bermakna bagi yang berpuasa (nada yang sama dengan BawaanPreferensi),
            // dan mengikuti saklar per waktu Subuh di menu Adzan seperti pengingat lainnya.
            BarisSaklar(
                "Ingatkan imsak sebelum Subuh",
                "Bila aktif: notifikasi $menitImsak menit sebelum jadwal Subuh, tetap berjalan " +
                    "walau aplikasi ditutup, dan mengikuti saklar Subuh di menu Adzan. Ketukannya " +
                    "membuka menu Jadwal.",
                ingatkanImsak,
            ) { ingatkanImsak = it; scope.launch { store.set(Store.IMSAK_ON, ingatkanImsak); PrayerScheduler.scheduleAll(ctx.applicationContext) } }
            KolomPreferensi(
                "Menit imsak sebelum Subuh",
                "Berapa menit sebelum jadwal Subuh pengingat imsak berbunyi. " +
                    PengingatAdzan.IMSAK_PERKIRAAN,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Batasnya dari aturan bersama, sama dengan yang menjepit nilai tersimpan di
                    // penjadwal dan pekerja latar — kalau tidak, angka di sini bisa tak pernah
                    // dipakai pengingatnya.
                    OutlinedButton(onClick = {
                        menitImsak = BawaanPreferensi.menitImsak(menitImsak - 1)
                        scope.launch {
                            store.set(Store.IMSAK_MIN, menitImsak)
                            PrayerScheduler.scheduleAll(ctx.applicationContext)
                        }
                    }) { Text("−") }
                    Text("$menitImsak mnt", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = {
                        menitImsak = BawaanPreferensi.menitImsak(menitImsak + 1)
                        scope.launch {
                            store.set(Store.IMSAK_MIN, menitImsak)
                            PrayerScheduler.scheduleAll(ctx.applicationContext)
                        }
                    }) { Text("+") }
                }
            }
            // Koreksi menit per waktu: deretan − / + -nya dipindah dari layar Jadwal ke sini. Jadwal di
            // layar itu dan pengingatnya sama-sama membaca `Store.CORR`, jadi keduanya tetap ikut.
            KolomPreferensi(
                "Koreksi menit per waktu",
                "Menambah atau mengurangi menit pada satu waktu sholat — berlaku untuk jadwal di " +
                    "menu Jadwal sekaligus pengingatnya. Metode hisab, kota, dan zona waktunya " +
                    "tetap diubah di menu Jadwal.",
            ) {
                // Nama waktunya dari aturan bersama — indeksnya adalah arti CSV `Store.CORR` yang diurai
                // `JadwalHariIni.parseCorr`, jadi nama dan angkanya tak dapat tertukar.
                JadwalHariIni.ENAM_WAKTU.forEachIndexed { i, n ->
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(n, style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { ubahKoreksi(i, -1) }) { Text("−") }
                            Text("${if (corr[i] > 0) "+" else ""}${corr[i]}",
                                style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = { ubahKoreksi(i, +1) }) { Text("+") }
                        }
                    }
                }
            }
            KolomPreferensi(
                "Koreksi kalender Hijriah",
                "Tanggal Hijriah dihitung tabular (perkiraan); samakan dengan hasil sidang isbat " +
                    "lewat koreksi ini.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    // Batas koreksi dari aturan bersama (`BawaanPreferensi`), bukan angka yang diingat
                    // masing-masing tombol: tombol dan nilai bawaan tidak dapat berbeda pendapat lagi.
                    OutlinedButton(onClick = { hijriCorr = BawaanPreferensi.koreksiHijriah(hijriCorr - 1); scope.launch { store.set(Store.HIJRI_CORR, hijriCorr) } }) { Text("−") }
                    Text("${if (hijriCorr > 0) "+" else ""}$hijriCorr hari",
                        style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { hijriCorr = BawaanPreferensi.koreksiHijriah(hijriCorr + 1); scope.launch { store.set(Store.HIJRI_CORR, hijriCorr) } }) { Text("+") }
                }
            }
            KolomPreferensi("Sumber jadwal & lokasi") {
                Text("Hitung lokal (offline, algoritma Meeus, metode ${metode.label} — " +
                        "${JadwalHariIni.metodeRingkas(metode)}). Metode, kota, dan zona waktunya " +
                        "diubah di menu Jadwal; sumber online resmi belum tersedia karena SIHAT " +
                        "Bimas Islam tidak menyediakan API publik.",
                    style = MaterialTheme.typography.labelSmall)
                Text("Kota saat ini: $city (ubah di menu Jadwal).",
                    style = MaterialTheme.typography.labelSmall)
            }
        }

        // ---- Kompas kiblat --------------------------------------------------------------------
        // Satuan dan tampilan kompas dahulu ditawarkan sekali lagi sebagai chip di layar Kiblat;
        // kini rumahnya hanya di sini — layar itu menampilkan hasilnya dan cukup mengarahkan ke sini.
        BagianJudul("Kompas kiblat")
        KartuGrup {
            KolomPreferensi("Satuan azimut") {
                BarisChip {
                    TampilanKompas.SATUAN.forEach { u ->
                        FilterChip(compassUnit == u,
                            { compassUnit = u; scope.launch { store.set(Store.COMPASS_UNIT, compassUnit) } },
                            { Text(TampilanKompas.labelSatuan(u)) })
                    }
                }
            }
            KolomPreferensi(
                "Tampilan kompas",
                "Mengubah gambar jarum kompas di layar Kiblat. Perhitungan tetap memakai derajat.",
            ) {
                BarisChip {
                    TampilanKompas.SEMUA.forEach { t ->
                        FilterChip(compassView == t,
                            { compassView = t; scope.launch { store.set(Store.COMPASS_VIEW, compassView) } },
                            { Text(TampilanKompas.label(t)) })
                    }
                }
            }
        }

        // ---- Data lokal -----------------------------------------------------------------------
        BagianJudul("Data lokal")
        KartuGrup {
            val r = ringkasan
            if (r == null) {
                Text("Menghitung isi data yang tersimpan…", style = MaterialTheme.typography.labelSmall)
            } else {
                Text("Tersimpan di perangkat ini: ${r.favorit} favorit, ${r.bookmark} bookmark, " +
                        "${r.riwayat} riwayat, ${r.progres} progres. Tidak ada yang dikirim ke server.",
                    style = MaterialTheme.typography.labelSmall)
                BagianData.entries.forEach { bagian ->
                    val n = bagian.jumlah(r)
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${bagian.label}: $n", style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = { hapusTarget = bagian }, enabled = n > 0) { Text(NamaAntarmuka.HAPUS) }
                    }
                }
            }
            KolomPreferensi(
                "Backup / pulihkan data lokal",
                "Backup memuat pengaturan, favorit, bookmark, riwayat, dan progres. Simpan " +
                    "berkasnya di tempat aman.",
            ) {
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
                OutlinedTextField(backup, { backup = it }, label = { Text("Data backup") },
                    modifier = Modifier.fillMaxWidth())
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

        // ---- Pembaruan konten -------------------------------------------------------------------
        // Judul bagian dan label tombolnya menyebut layar tujuan dengan nama bersama — satu nama
        // untuk satu layar, sama seperti seluruh judul bagian lain di layar ini.
        BagianJudul(NamaLayar.PEMBARUAN_KONTEN)
        KartuGrup {
            OutlinedButton(onClick = { go(Routes.KONTEN_UPDATE) }, modifier = Modifier.fillMaxWidth()) {
                Text("Buka ${NamaLayar.PEMBARUAN_KONTEN}")
            }
            Text("Memasang bundel konten pilihan Anda setelah diperiksa sumbernya. Tanpa unduhan otomatis dan tanpa izin jaringan.",
                style = MaterialTheme.typography.labelSmall)
        }

        // ---- Tentang ----------------------------------------------------------------------------
        // NavSmokeTest mencari teks judul ini (nama bersama layar Tentang) saat menggulir layar
        // Pengaturan — judul bagian memakai nama yang sama dengan layar yang dibukanya.
        BagianJudul(NamaLayar.TENTANG)
        KartuGrup {
            Text("MIHRAB — versi ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { go(Routes.TENTANG) }, modifier = Modifier.fillMaxWidth()) {
                Text("Buka ${NamaLayar.TENTANG}")
            }
            Text("Lisensi kode: Apache License 2.0. Konten keagamaan tetap tunduk pada sumber resminya " +
                    "(${ContentValidator.allowedHosts.joinToString()}); keterangan pemilik tiap domain " +
                    "ada di ${NamaLayar.TENTANG}. Izin: lokasi (kiblat/jadwal), notifikasi (pengingat), " +
                    "getar (timer iqomah). Data tersimpan lokal; tidak ada yang dikirim ke server.",
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Kartu satu kelompok pengaturan — batas visual yang jelas antarkelompok pada layar yang panjang. */
@Composable
private fun KartuGrup(isi: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = isi,
        )
    }
}

/**
 * Satu pengaturan di dalam kartu kelompok: namanya di atas, kendalinya di bawahnya, dan catatan
 * kecil penjelas di ujung. Catatan ditulis sebagai parameter agar pasangan nama–catatan tidak
 * dapat tertukar antarpengaturan.
 */
@Composable
private fun KolomPreferensi(
    judul: String,
    catatan: String? = null,
    isi: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(judul, style = MaterialTheme.typography.titleSmall)
        Column(
            Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = isi,
        )
        if (catatan != null) Text(
            catatan,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Baris pengukur: nama di kiri, angkanya di kanan, penggesernya di bawahnya. */
@Composable
private fun BarisUkur(
    judul: String,
    nilai: String,
    rentang: ClosedFloatingPointRange<Float>,
    posisi: Float,
    ubah: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(judul, style = MaterialTheme.typography.titleSmall)
            Text(nilai, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
        }
        Slider(posisi, ubah, valueRange = rentang)
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
        Modifier.fillMaxWidth(),
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
