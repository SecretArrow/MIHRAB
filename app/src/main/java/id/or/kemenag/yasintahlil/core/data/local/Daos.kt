package id.or.kemenag.yasintahlil.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE kind = :kind AND refId = :refId)")
    fun observeIsFavorite(kind: String, refId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE kind = :kind AND refId = :refId)")
    suspend fun exists(kind: String, refId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE kind = :kind AND refId = :refId")
    suspend fun delete(kind: String, refId: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE kind = :kind ORDER BY createdAt DESC")
    fun observeByKind(kind: String): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE kind = :kind AND refId = :refId)")
    fun observeIsBookmarked(kind: String, refId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE kind = :kind AND refId = :refId)")
    suspend fun exists(kind: String, refId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE kind = :kind AND refId = :refId")
    suspend fun delete(kind: String, refId: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY openedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(item: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE kind = :kind AND refId = :refId LIMIT 1")
    fun observe(kind: String, refId: String): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE kind = :kind ORDER BY updatedAt DESC")
    fun observeByKind(kind: String): Flow<List<ProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProgressEntity)

    @Query("DELETE FROM progress WHERE kind = :kind AND refId = :refId")
    suspend fun delete(kind: String, refId: String)
}
