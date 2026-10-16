package id.or.kemenag.yasintahlil.core.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.ds by preferencesDataStore("yasin_tahlil")

/** Preferensi aplikasi (DataStore). Data terstruktur (favorit/progres) kini di Room. */
class Store(private val ctx: Context) {
    companion object {
        val DARK = stringPreferencesKey("theme") // terang|gelap|sistem
        val ARAB_SIZE = floatPreferencesKey("arab_size")
        val LATIN_SIZE = floatPreferencesKey("latin_size")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val SHOW_LATIN = booleanPreferencesKey("show_latin")
        val SHOW_TRANS = booleanPreferencesKey("show_trans")
        val SHOW_ARAB = booleanPreferencesKey("show_arab")
        /**
         * Layar tetap menyala saat membaca. Dahulu chip lokal di layar Yasin; kini pengaturan
         * terpusat di tab Pengaturan (baca `BawaanPreferensi.TETAP_MENYALA`) dan tetap diterapkan
         * layar Yasin yang memakainya.
         */
        val KEEP_ON = booleanPreferencesKey("keep_on")
        /**
         * Penghitung ulangan (tombol + dan − hitungan) ditampilkan di layar Tahlil dan Dzikir.
         * Dahulu ia menempel pada setiap layar; kini opt-in dan pengaturannya — seperti segala
         * pengaturan lain — terpusat di tab Pengaturan (baca `BawaanPreferensi.TAMPIL_PENGHITUNG`).
         */
        val SHOW_COUNTER = booleanPreferencesKey("show_counter")
        /** Tema halaman bacaan: `ikut`|`krem`|`gelap` (lihat `TemaHalaman` di paket designsystem). */
        val PAGE_THEME = stringPreferencesKey("page_theme")
        val ASAR = stringPreferencesKey("asar") // syafii|hanafi
        val METHOD = stringPreferencesKey("method") // kemenag|mwl|umm_al_qura|egyptian|makkah
        val CITY = stringPreferencesKey("city")
        val LAT = doublePreferencesKey("lat")
        val LON = doublePreferencesKey("lon")
        val USE_GPS = booleanPreferencesKey("use_gps")
        /**
         * Jadwal sholat mengikuti perubahan lokasi (GPS live), bukan hanya satu perbaikan terakhir.
         *
         * Terpisah dari [USE_GPS]: [USE_GPS] berarti "koordinat yang dipakai berasal dari GPS"
         * (dipakai bersama layar Kiblat), sedangkan kunci ini berarti "hitung ulang saat pengguna
         * berpindah tempat". Menggabungkannya membuat satu saklar mengubah dua hal sekaligus.
         */
        val JADWAL_IKUT = booleanPreferencesKey("jadwal_ikut_lokasi")
        val TZ = stringPreferencesKey("tz") // id zona waktu, kosong = default perangkat
        val CORR = stringPreferencesKey("corr") // koreksi menit csv 6 angka
        val ADZAN_ON = stringPreferencesKey("adzan_on") // csv 5 bool
        /** Berkas audio adzan yang **sedang aktif** — satu-satunya yang diputar penjadwal & pekerja. */
        val ADZAN_URI = stringPreferencesKey("adzan_uri")
        /**
         * Daftar "jenis suara" adzan milik pengguna: nama + URI per baris (lihat [DaftarSuaraAdzan]).
         *
         * Terpisah dari [ADZAN_URI] karena keduanya menjawab hal berbeda: [ADZAN_URI] "berkas mana
         * yang berbunyi", kunci ini "berkas apa saja yang pernah disimpan". Menggabungkannya akan
         * memaksa pekerja latar mengurai daftar hanya untuk tahu satu berkas yang harus diputar.
         */
        val ADZAN_SOUNDS = stringPreferencesKey("adzan_sounds")
        val ADZAN_VOL = floatPreferencesKey("adzan_vol")
        val ADZAN_PRE_MIN = intPreferencesKey("adzan_pre_min")
        /** Pengingat membaca doa setelah adzan, beberapa menit setelah waktu sholat masuk. */
        val DOA_ADZAN_REMINDER = booleanPreferencesKey("doa_adzan_reminder")
        val IQOMAH_MIN = intPreferencesKey("iqomah_min")
        /**
         * Pengingat opsional bahwa perkiraan waktu iqomah telah lewat ([IQOMAH_MIN] menit setelah
         * masuk waktu). Kalimat yang membatasinya satu tempat — `PengingatAdzan.IQOMAH_PERKIRAAN` —
         * karena waktu iqomah tiap masjid memang berbeda.
         */
        val IQOMAH_ON = booleanPreferencesKey("iqomah_on")
        val MADHAB = stringPreferencesKey("madhab") // syafii|hanafi|maliki|hambali
        val TAHLIL_SUSUNAN = stringPreferencesKey("tahlil_susunan")
        /** Susunan tahlil buatan pengguna: CSV id bagian, urut. Kosong = belum disusun. */
        val TAHLIL_CUSTOM = stringPreferencesKey("tahlil_custom")
        /**
         * Mode Imam Tahlil aktif: lencana "siapa membaca" tampil pada tiap bagian dan bacaannya
         * memakai susunan Lengkap. Terpisah dari [TAHLIL_SUSUNAN] karena [TAHLIL_SUSUNAN] tetap
         * menyimpan susunan dasarnya (Lengkap) — Pengaturan menampilkan sumber susunan yang sah,
         * bukan id "imam" yang tidak dikenal daftar susunan bundel.
         */
        val TAHLIL_IMAM = booleanPreferencesKey("tahlil_imam")
        val COMPASS_UNIT = stringPreferencesKey("compass_unit") // derajat|mil
        /** Tampilan kompas kiblat: `kompas`|`angka` (lihat `TampilanKompas` di paket designsystem). */
        val COMPASS_VIEW = stringPreferencesKey("compass_view")
        val INFO_CARD = booleanPreferencesKey("info_card")
        val HIJRI_CORR = intPreferencesKey("hijri_corr") // koreksi hari kalender Hijriah (-2..2)
        val FAV = stringSetPreferencesKey("fav") // warisan (legacy) — migrasi ke Room
        val HIST = stringPreferencesKey("hist") // warisan (legacy) — migrasi ke Room
        val LAST = stringPreferencesKey("last") // mis. yasin:58
        val YASIN_MARK = intPreferencesKey("yasin_mark") // warisan
        val TAHLIL_PROG = stringPreferencesKey("tahlil_prog") // warisan posisi:hitungan — migrasi ke Room
        val NOTIF = booleanPreferencesKey("notif")
        /**
         * Onboarding sekali di awal sudah selesai. `false` berarti pengguna baru (atau yang belum
         * menekan "Mulai") masih disambut layar pengantar sebelum tiba di beranda.
         */
        val ONBOARDING_SELESAI = booleanPreferencesKey("onboarding_selesai")
        /**
         * Pengingat imsak: beberapa menit sebelum Subuh (bawaan [BawaanPreferensi.MENIT_IMSAK]).
         * Opt-in seperti pengingat doa/iqomah, karena tidak semua pengguna sedang berpuasa.
         */
        val IMSAK_ON = booleanPreferencesKey("imsak_on")
        val IMSAK_MIN = intPreferencesKey("imsak_min")
    }

    suspend fun <T> get(key: Preferences.Key<T>, def: T): T =
        ctx.ds.data.map { it[key] ?: def }.first()

    suspend fun <T> set(key: Preferences.Key<T>, v: T) {
        ctx.ds.edit { it[key] = v }
    }

    /** Aliran reaktif untuk ViewModel. */
    fun <T> flow(key: Preferences.Key<T>, def: T): Flow<T> =
        ctx.ds.data.map { it[key] ?: def }

    /** Backup penuh seluruh preferensi sebagai JSON. */
    suspend fun exportJson(): String {
        val d = ctx.ds.data.first()
        val o = JSONObject()
        d.asMap().forEach { (k, v) ->
            when (v) {
                is Set<*> -> o.put(k.name, JSONArray(v.toList()))
                else -> o.put(k.name, v)
            }
        }
        return o.toString()
    }

    /** Pulihkan preferensi dari JSON backup. */
    suspend fun importJson(text: String) {
        val o = runCatching { JSONObject(text) }.getOrNull() ?: return
        ctx.ds.edit { p ->
            fun str(k: Preferences.Key<String>) { if (o.has(k.name)) p[k] = o.optString(k.name) }
            fun bool(k: Preferences.Key<Boolean>) { if (o.has(k.name)) p[k] = o.optBoolean(k.name) }
            fun flt(k: Preferences.Key<Float>) { if (o.has(k.name)) p[k] = o.optDouble(k.name).toFloat() }
            fun dbl(k: Preferences.Key<Double>) { if (o.has(k.name)) p[k] = o.optDouble(k.name) }
            fun int(k: Preferences.Key<Int>) { if (o.has(k.name)) p[k] = o.optInt(k.name) }
            str(DARK); str(ASAR); str(METHOD); str(CITY); str(TZ); str(CORR)
            str(ADZAN_ON); str(ADZAN_URI); str(ADZAN_SOUNDS); str(MADHAB); str(TAHLIL_SUSUNAN)
            str(COMPASS_UNIT)
            str(TAHLIL_CUSTOM); str(PAGE_THEME); str(COMPASS_VIEW)
            str(HIST); str(LAST); str(TAHLIL_PROG)
            flt(ARAB_SIZE); flt(LATIN_SIZE); flt(LINE_HEIGHT); flt(ADZAN_VOL)
            dbl(LAT); dbl(LON)
            bool(SHOW_LATIN); bool(SHOW_TRANS); bool(SHOW_ARAB); bool(USE_GPS); bool(JADWAL_IKUT)
            bool(INFO_CARD); bool(NOTIF); bool(DOA_ADZAN_REMINDER); bool(IQOMAH_ON); bool(KEEP_ON)
            bool(SHOW_COUNTER); bool(ONBOARDING_SELESAI); bool(IMSAK_ON); bool(TAHLIL_IMAM)
            int(ADZAN_PRE_MIN); int(IQOMAH_MIN); int(YASIN_MARK); int(HIJRI_CORR); int(IMSAK_MIN)
            if (o.has(FAV.name)) {
                val arr = o.optJSONArray(FAV.name)
                if (arr != null) {
                    val set = mutableSetOf<String>()
                    for (i in 0 until arr.length()) set += arr.optString(i)
                    p[FAV] = set
                }
            }
        }
    }
}
