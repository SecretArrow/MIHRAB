package id.or.kemenag.yasintahlil.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavoriteEntity::class, BookmarkEntity::class, HistoryEntity::class, ProgressEntity::class,
        TasbihEntity::class, AlmarhumEntity::class, KegiatanEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun progressDao(): ProgressDao
    abstract fun tasbihDao(): TasbihDao
    abstract fun almarhumDao(): AlmarhumDao
    abstract fun kegiatanDao(): KegiatanDao

    companion object {
        const val NAME = "yasin_tahlil.db"

        /**
         * Skema v2 menambah tiga tabel — `tasbih` (hitungan per hari), `almarhum` (daftar
         * peringatan tahlil), dan `kegiatan` (khataman & rangkaian hari). Tabel lama tidak
         * berubah; karena itu migrasinya murni `CREATE TABLE` tanpa menyalin data.
         *
         * Perintahnya harus sejajar persis dengan skema yang diharapkan Room (afinitas kolom,
         * `NOT NULL`, dan `PRIMARY KEY`) — selisih sekecil apa pun membuat `Room` menolak
         * membuka basis data yang sudah terpasang.
         */
        val MIGRASI_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tasbih` (`tanggal` TEXT NOT NULL, " +
                        "`hitung` INTEGER NOT NULL, `target` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`tanggal`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `almarhum` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nama` TEXT NOT NULL, `tanggalWafat` TEXT NOT NULL, `catatan` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `kegiatan` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`tanggal` TEXT NOT NULL, `jenis` TEXT NOT NULL, `keterangan` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)",
                )
            }
        }
    }
}
