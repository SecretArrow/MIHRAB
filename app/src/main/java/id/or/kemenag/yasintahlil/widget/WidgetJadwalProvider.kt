package id.or.kemenag.yasintahlil.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import id.or.kemenag.yasintahlil.MainActivity
import id.or.kemenag.yasintahlil.R
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.util.HijriCalc
import id.or.kemenag.yasintahlil.core.util.JadwalHariIni
import id.or.kemenag.yasintahlil.core.util.PrayerCalc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Widget homescreen 4x3: jadwal sholat lima waktu untuk hari ini.
 *
 * **RemoteViews klasik, bukan Glance.** Repo ini menahan dependensi baru: pipeline-nya tidak
 * membangun APK secara lokal, sehingga pustaka baru tidak dapat dicoba lebih dulu — dan widget
 * adalah komponen yang paling lama terpasang di layar pengguna, terlalu berisiko dijadikan tempat
 * pertama mencoba pustaka baru. Kerangka `android.appwidget` + [RemoteViews] murni bagian dari
 * platform sejak jauh sebelum minSdk aplikasi (API 26), stabil di seluruh versi yang didukung,
 * dan memadai untuk tata letak baris nama–jam seperti ini.
 *
 * **Dihitung di perangkat, tanpa izin dan tanpa jaringan baru.** Seluruh hisab memakai aturan
 * bersama yang sama dengan layar Jadwal, beranda, dan penjadwal pengingat — preferensi mentah
 * disusun lewat [JadwalHariIni.params], dihitung [PrayerCalc.compute], lalu pasangan nama→jam
 * diambil dari [JadwalHariIni.LIMA_WAKTU] zip [JadwalHariIni.adzan] — sehingga jam di widget,
 * jam di layar, dan pengingat yang berbunyi tidak dapat berbeda.
 *
 * **Pembaruan tiap 30 menit.** `updatePeriodMillis` pada `widget_jadwal_info.xml` adalah 30
 * menit — nilai terkecil yang diizinkan sistem; jam sholat hanya berganti beberapa kali sehari
 * dan tanggalnya sekali, sehingga memaksa lebih sering hanya menghabiskan baterai. Pada tiap
 * pembaruan jadwal dihitung **ulang** untuk tanggal hari itu (zona waktu pengguna), jadi
 * pergantian hari tertangkap tanpa penjadwalan tambahan.
 */
class WidgetJadwalProvider : AppWidgetProvider() {

    /**
     * Id pasangan nama–jam tiap baris waktu, **urutannya sejajar [JadwalHariIni.LIMA_WAKTU]**:
     * indeks ke-`i` milik waktu ke-`i` dari daftar itu — hubungan yang sama dengan indeks CSV
     * `Store.ADZAN_ON`. Menulis ulang pasangan nama→id secara terpisah berarti satu urutan salah
     * ketik menampilkan jam Asar pada baris Zuhur, tanpa satu pun pesan galat; `check` pada blok
     * `init` menjaga panjangnya tetap sejajar.
     */
    private val barisId = listOf(
        R.id.widget_nama_subuh to R.id.widget_jam_subuh,
        R.id.widget_nama_zuhur to R.id.widget_jam_zuhur,
        R.id.widget_nama_asar to R.id.widget_jam_asar,
        R.id.widget_nama_magrib to R.id.widget_jam_magrib,
        R.id.widget_nama_isya to R.id.widget_jam_isya,
    )

    init {
        check(barisId.size == JadwalHariIni.LIMA_WAKTU.size) {
            "barisId harus sejajar JadwalHariIni.LIMA_WAKTU — satu baris widget untuk satu waktu"
        }
    }

    /**
     * Pasang tampilan untuk tiap id widget.
     *
     * Membaca [Store] adalah pemanggilan `suspend`, sementara `onUpdate` berjalan pada utas siaran
     * yang singkat. Polanya sama dengan `BootReceiver`: [goAsync] memberi waktu kepada siaran ini
     * menyelesaikan pekerjaannya, pekerjaannya dijalankan pada `Dispatchers.IO`, dan `finish()`
     * dipanggil di `finally` agar siaran tidak pernah menggantung — berhasil maupun gagal.
     * Kegagalan memasang satu widget ditelan per-widget lewat `runCatching` supaya id lain tetap
     * terpasang; hisabnya sendiri matematika lokal yang deterministik, jadi sisa kegagalannya
     * hanyalah baca-tulis preferensi yang bermasalah — widget lama tetap menampilkan jadwal
     * terakhir yang berhasil dihitung.
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val tugas = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ctx = context.applicationContext
                appWidgetIds.forEach { id ->
                    runCatching { pasang(ctx, appWidgetManager, id) }
                }
            } finally {
                tugas.finish()
            }
        }
    }

    /** Hitung jadwal hari ini lalu isi seluruh teks widget untuk satu [id]. */
    private suspend fun pasang(ctx: Context, mgr: AppWidgetManager, id: Int) {
        // Preferensi dibaca dengan kunci dan bawaan yang sama dengan penjadwal pengingat
        // (pola `PrayerScheduler.loadCfg`): widget yang menghitung dengan aturan lain membuat
        // pengguna membaca dua jam berbeda untuk sholat yang sama — persis kekeliruan yang
        // hendak dicegah aturan bersama `JadwalHariIni`.
        val store = Store(ctx)
        val tz = JadwalHariIni.zona(store.get(Store.TZ, JadwalHariIni.TZ_DEFAULT))
        val params = JadwalHariIni.params(
            lat = store.get(Store.LAT, JadwalHariIni.LAT_DEFAULT),
            lon = store.get(Store.LON, JadwalHariIni.LON_DEFAULT),
            methodKey = store.get(Store.METHOD, JadwalHariIni.METODE_DEFAULT),
            asarKey = store.get(Store.ASAR, JadwalHariIni.ASAR_DEFAULT),
            corrCsv = store.get(Store.CORR, JadwalHariIni.CORR_DEFAULT),
            tz = tz,
        )
        // Kalender zona pengguna — tanggal Masehi/Hijriah di kepala widget harus hari yang sama
        // dengan hari yang dihitung jam sholatnya, bukan hari menurut zona tempat lain.
        val now = Calendar.getInstance(tz)
        val times = PrayerCalc.compute(now, params)

        val rv = RemoteViews(ctx.packageName, R.layout.widget_jadwal)

        // Kepala: nama aplikasi dari satu sumber yang sama dengan label peluncur, bukan diketik
        // ulang di sini; ejaannya dijaga `IconAndThemeResourcesTest`.
        rv.setTextViewText(R.id.widget_judul, ctx.getString(R.string.app_name))

        // Tanggal singkat: Masehi ringkas + Hijriah dari kalender yang sama dengan beranda.
        // Nama bulan Hijriah diambil dari `HijriCalc.bulan` apa adanya — meringkasnya di sini
        // berarti menumbuhkan kosakata kedua yang dapat berbeda ejaan dari beranda.
        val masehi = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id")).format(now.time)
        val (h, b, y) = HijriCalc.from(now, store.get(Store.HIJRI_CORR, BawaanPreferensi.KOREKSI_HIJRIAH))
        val hijriah = "$h ${HijriCalc.bulan[b - 1]} $y H"
        rv.setTextViewText(R.id.widget_tanggal, "$masehi • $hijriah")

        // Lima baris waktu. Jam dari `JadwalHariIni.adzan` sudah berbentuk "HH:mm" — ejaan satu
        // satunya milik `FormatWaktu.jamMenit` yang dipakai layar Jadwal dan beranda; widget tidak
        // mengejanya ulang (apalagi dengan pemisah lain), supaya jam yang sama tak pernah tertulis
        // berbeda antara widget, layar, dan pengingat.
        val jam = JadwalHariIni.adzan(times)
        JadwalHariIni.LIMA_WAKTU.forEachIndexed { i, nama ->
            rv.setTextViewText(barisId[i].first, nama)
            rv.setTextViewText(barisId[i].second, jam[i])
        }

        rv.setTextViewText(R.id.widget_kota, store.get(Store.CITY, JadwalHariIni.KOTA_DEFAULT))

        // Ketukan pada judul membuka aplikasi — cukup `MainActivity` tanpa extra rute: jadwal
        // lengkap tinggal satu ketukan lagi di dalamnya. Pola PendingIntent sama dengan
        // `ReminderWorker.contentIntent`; FLAG_IMMUTABLE wajib di API 31+ dan peluncur memang
        // tak perlu diubah pihak lain.
        val buka = Intent(ctx, MainActivity::class.java)
        val jangka = PendingIntent.getActivity(
            ctx, 0, buka,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        rv.setOnClickPendingIntent(R.id.widget_judul, jangka)

        mgr.updateAppWidget(id, rv)
    }

    /**
     * Tak ada pekerjaan saat widget pertama dipasang: jadwal dihitung ulang pada setiap pembaruan
     * ([onUpdate]), tidak ada sumber daya yang perlu disiapkan sekali untuk seluruh instance.
     */
    override fun onEnabled(context: Context) {
    }

    /**
     * Tak ada pekerjaan saat widget terakhir dilepas: tidak ada yang perlu dilepas karena tidak
     * ada yang pernah disiapkan di [onEnabled].
     */
    override fun onDisabled(context: Context) {
    }
}
