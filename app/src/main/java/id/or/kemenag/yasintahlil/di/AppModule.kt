package id.or.kemenag.yasintahlil.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.data.local.AlmarhumDao
import id.or.kemenag.yasintahlil.core.data.local.AppDatabase
import id.or.kemenag.yasintahlil.core.data.local.BookmarkDao
import id.or.kemenag.yasintahlil.core.data.local.FavoriteDao
import id.or.kemenag.yasintahlil.core.data.local.HistoryDao
import id.or.kemenag.yasintahlil.core.data.local.KegiatanDao
import id.or.kemenag.yasintahlil.core.data.local.ProgressDao
import id.or.kemenag.yasintahlil.core.data.local.TasbihDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentRepo(@ApplicationContext ctx: Context): ContentRepo = ContentRepo(ctx)

    @Provides
    @Singleton
    fun provideStore(@ApplicationContext ctx: Context): Store = Store(ctx)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRASI_1_2)
            .build()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideBookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideProgressDao(db: AppDatabase): ProgressDao = db.progressDao()

    @Provides
    fun provideTasbihDao(db: AppDatabase): TasbihDao = db.tasbihDao()

    @Provides
    fun provideAlmarhumDao(db: AppDatabase): AlmarhumDao = db.almarhumDao()

    @Provides
    fun provideKegiatanDao(db: AppDatabase): KegiatanDao = db.kegiatanDao()
}
