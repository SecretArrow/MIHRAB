package id.or.kemenag.yasintahlil.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteEntity::class, BookmarkEntity::class, HistoryEntity::class, ProgressEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun progressDao(): ProgressDao

    companion object {
        const val NAME = "yasin_tahlil.db"
    }
}
