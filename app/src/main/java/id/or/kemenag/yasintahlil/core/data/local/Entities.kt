package id.or.kemenag.yasintahlil.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data terstruktur pengguna (favorit, bookmark, riwayat, progres) disimpan di Room
 * agar aman terhadap rotasi/process-death dan bisa dikueri.
 * Kunci gabungan (kind + refId), mis. kind="yasin", refId="58".
 */

@Entity(tableName = "favorites", primaryKeys = ["kind", "refId"])
data class FavoriteEntity(
    val kind: String,
    val refId: String,
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "bookmarks", primaryKeys = ["kind", "refId"])
data class BookmarkEntity(
    val kind: String,
    val refId: String,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val refId: String,
    val title: String = "",
    val openedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "progress", primaryKeys = ["kind", "refId"])
data class ProgressEntity(
    val kind: String,
    val refId: String,
    val value: Int,
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Hitungan tasbih satu hari: satu baris per tanggal, ditimpa (REPLACE) tiap ketukan —
 * sehingga hitungan tidak pernah hilang oleh rotasi/process-death.
 */
@Entity(tableName = "tasbih")
data class TasbihEntity(
    @PrimaryKey val tanggal: String, // "yyyy-MM-dd" (Locale.US) — satu baris per hari
    val hitung: Int,
    val target: Int,
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Daftar almarhum untuk peringatan tahlil: nama + tanggal wafat; hari ke-7/40/100/1000
 * dan haul dihitung dari tanggalnya, tidak disimpan ulang.
 */
@Entity(tableName = "almarhum")
data class AlmarhumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val tanggalWafat: String, // "yyyy-MM-dd"
    val catatan: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Kegiatan harian untuk khataman & rangkaian hari (streak): membuka layar bacaan
 * ("yasin_buka"/"tahlil_buka") dan menuntaskan khataman ("khatam_yasin").
 */
@Entity(tableName = "kegiatan")
data class KegiatanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tanggal: String, // "yyyy-MM-dd"
    val jenis: String, // mis. "yasin_buka", "tahlil_buka", "khatam_yasin"
    val keterangan: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
