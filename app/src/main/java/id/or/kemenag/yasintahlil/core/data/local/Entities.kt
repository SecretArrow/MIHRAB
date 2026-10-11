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
