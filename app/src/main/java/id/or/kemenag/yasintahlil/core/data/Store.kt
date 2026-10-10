package id.or.kemenag.yasintahlil.core.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore("yasin_tahlil")

/** Penyimpanan lokal: pengaturan, favorit, riwayat, bookmark, progres. */
class Store(private val ctx: Context) {
    companion object {
        val DARK = stringPreferencesKey("theme") // terang|gelap|sistem
        val ARAB_SIZE = floatPreferencesKey("arab_size")
        val LATIN_SIZE = floatPreferencesKey("latin_size")
        val SHOW_LATIN = booleanPreferencesKey("show_latin")
        val SHOW_TRANS = booleanPreferencesKey("show_trans")
        val ASAR = stringPreferencesKey("asar") // syafii|hanafi
        val CITY = stringPreferencesKey("city")
        val LAT = doublePreferencesKey("lat")
        val LON = doublePreferencesKey("lon")
        val USE_GPS = booleanPreferencesKey("use_gps")
        val CORR = stringPreferencesKey("corr") // koreksi menit csv 6 angka
        val ADZAN_ON = stringPreferencesKey("adzan_on") // csv 5 bool
        val ADZAN_URI = stringPreferencesKey("adzan_uri")
        val ADZAN_VOL = floatPreferencesKey("adzan_vol")
        val IQOMAH_MIN = intPreferencesKey("iqomah_min")
        val FAV = stringSetPreferencesKey("fav")
        val HIST = stringPreferencesKey("hist") // csv id|waktu
        val LAST = stringPreferencesKey("last") // mis. yasin:58
        val YASIN_MARK = intPreferencesKey("yasin_mark")
        val TAHLIL_PROG = stringPreferencesKey("tahlil_prog") // idx:count
        val NOTIF = booleanPreferencesKey("notif")
    }

    suspend fun <T> get(key: Preferences.Key<T>, def: T): T =
        ctx.ds.data.map { it[key] ?: def }.first()

    suspend fun <T> set(key: Preferences.Key<T>, v: T) {
        ctx.ds.edit { it[key] = v }
    }

    suspend fun toggleFav(id: String) {
        val cur = get(FAV, emptySet())
        set(FAV, if (cur.contains(id)) cur - id else cur + id)
    }

    suspend fun pushHist(id: String) {
        val cur = get(HIST, "")
        val items = (listOf("$id|${System.currentTimeMillis()}") + cur.split(";").filter { it.isNotBlank() }).take(50)
        set(HIST, items.joinToString(";"))
    }

    suspend fun exportAll(): String {
        val d = ctx.ds.data.first()
        return d.asMap().entries.joinToString("\n") { (k, v) -> "${k.name}=$v" }
    }

    suspend fun importAll(text: String) {
        ctx.ds.edit { p ->
            text.lines().forEach { line ->
                val i = line.indexOf('=')
                if (i > 0) {
                    val k = line.substring(0, i); val v = line.substring(i + 1)
                    when (k) {
                        "theme" -> p[DARK] = v
                        "city" -> p[CITY] = v
                        "last" -> p[LAST] = v
                        "hist" -> p[HIST] = v
                    }
                }
            }
        }
    }
}
