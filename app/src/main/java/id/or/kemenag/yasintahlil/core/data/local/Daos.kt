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

    @Query("SELECT * FROM favorites")
    suspend fun getAll(): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE kind = :kind AND refId = :refId)")
    fun observeIsFavorite(kind: String, refId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE kind = :kind AND refId = :refId)")
    suspend fun exists(kind: String, refId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE kind = :kind AND refId = :refId")
    suspend fun delete(kind: String, refId: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE kind = :kind ORDER BY createdAt DESC")
    fun observeByKind(kind: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<BookmarkEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE kind = :kind AND refId = :refId)")
    fun observeIsBookmarked(kind: String, refId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE kind = :kind AND refId = :refId)")
    suspend fun exists(kind: String, refId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BookmarkEntity>)

    @Query("DELETE FROM bookmarks WHERE kind = :kind AND refId = :refId")
    suspend fun delete(kind: String, refId: String)

    @Query("DELETE FROM bookmarks")
    suspend fun clear()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY openedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY openedAt DESC LIMIT :limit")
    suspend fun getAll(limit: Int): List<HistoryEntity>

    @Insert
    suspend fun insert(item: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HistoryEntity>)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE kind = :kind AND refId = :refId LIMIT 1")
    fun observe(kind: String, refId: String): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE kind = :kind ORDER BY updatedAt DESC")
    fun observeByKind(kind: String): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress")
    suspend fun getAll(): List<ProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProgressEntity>)

    @Query("DELETE FROM progress WHERE kind = :kind AND refId = :refId")
    suspend fun delete(kind: String, refId: String)

    @Query("DELETE FROM progress")
    suspend fun clear()
}
