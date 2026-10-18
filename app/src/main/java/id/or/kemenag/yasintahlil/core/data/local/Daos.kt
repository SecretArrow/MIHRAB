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

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun count(): Int

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

    /** Seluruh bookmark, lintas jenis — dipakai layar Favorit & Riwayat (`plan.txt`: "bookmark"). */
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<BookmarkEntity>

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun count(): Int

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

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int

    /** Jumlah baris sebagai aliran — beranda memakainya agar "Riwayat: N" ikut berubah. */
    @Query("SELECT COUNT(*) FROM history")
    fun observeCount(): Flow<Int>

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

    @Query("SELECT COUNT(*) FROM progress")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProgressEntity>)

    @Query("DELETE FROM progress WHERE kind = :kind AND refId = :refId")
    suspend fun delete(kind: String, refId: String)

    @Query("DELETE FROM progress")
    suspend fun clear()
}

@Dao
interface TasbihDao {
    /** Baris hari ini — layar tasbih berlangganan aliran ini agar hitungan hidup reaktif. */
    @Query("SELECT * FROM tasbih WHERE tanggal = :tanggal LIMIT 1")
    fun observe(tanggal: String): Flow<List<TasbihEntity>>

    /** Riwayat N hari terakhir, termasuk hari ini — dasar daftar riwayat 7 hari. */
    @Query("SELECT * FROM tasbih ORDER BY tanggal DESC LIMIT :batas")
    fun observeTerbaru(batas: Int): Flow<List<TasbihEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TasbihEntity)

    @Query("DELETE FROM tasbih")
    suspend fun clear()
}

@Dao
interface AlmarhumDao {
    @Query("SELECT * FROM almarhum ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AlmarhumEntity>>

    @Query("SELECT * FROM almarhum ORDER BY createdAt DESC")
    suspend fun getAll(): List<AlmarhumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AlmarhumEntity)

    @Query("DELETE FROM almarhum WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM almarhum")
    suspend fun clear()
}

@Dao
interface KegiatanDao {
    @Query("SELECT * FROM kegiatan ORDER BY tanggal DESC")
    fun observeAll(): Flow<List<KegiatanEntity>>

    @Query("SELECT * FROM kegiatan WHERE jenis = :jenis ORDER BY tanggal DESC")
    fun observeByJenis(jenis: String): Flow<List<KegiatanEntity>>

    /** Jumlah kegiatan satu jenis sebagai aliran — total khataman ikut bertambah saat tercatat. */
    @Query("SELECT COUNT(*) FROM kegiatan WHERE jenis = :jenis")
    fun observeJumlah(jenis: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM kegiatan WHERE jenis = :jenis")
    suspend fun jumlah(jenis: String): Int

    /**
     * Jumlah satu jenis pada satu tanggal — penjaga "sekali per hari": membuka layar Yasin/Tahlil
     * atau menekan tombol khataman tidak menambah baris kedua untuk hari yang sama.
     */
    @Query("SELECT COUNT(*) FROM kegiatan WHERE tanggal = :tanggal AND jenis = :jenis")
    suspend fun jumlahHariIni(tanggal: String, jenis: String): Int

    @Insert
    suspend fun insert(item: KegiatanEntity)

    @Query("DELETE FROM kegiatan")
    suspend fun clear()
}
