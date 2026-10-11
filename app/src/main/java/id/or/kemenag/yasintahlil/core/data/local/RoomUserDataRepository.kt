package id.or.kemenag.yasintahlil.core.data.local

import id.or.kemenag.yasintahlil.core.domain.model.BookmarkItem
import id.or.kemenag.yasintahlil.core.domain.model.FavItem
import id.or.kemenag.yasintahlil.core.domain.model.HistItem
import id.or.kemenag.yasintahlil.core.domain.model.ProgressItem
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomUserDataRepository @Inject constructor(
    private val db: AppDatabase,
) : UserDataRepository {

    private val fav = db.favoriteDao()
    private val mark = db.bookmarkDao()
    private val hist = db.historyDao()
    private val prog = db.progressDao()

    override fun favorites(): Flow<List<FavItem>> =
        fav.observeAll().map { list -> list.map { FavItem(it.kind, it.refId, it.title, it.createdAt) } }

    override fun isFavorite(kind: String, refId: String): Flow<Boolean> = fav.observeIsFavorite(kind, refId)

    override suspend fun toggleFavorite(kind: String, refId: String, title: String) {
        if (fav.exists(kind, refId)) fav.delete(kind, refId) else fav.upsert(FavoriteEntity(kind, refId, title))
    }

    override fun bookmarks(kind: String): Flow<List<BookmarkItem>> =
        mark.observeByKind(kind).map { list -> list.map { BookmarkItem(it.kind, it.refId, it.label, it.createdAt) } }

    override fun isBookmarked(kind: String, refId: String): Flow<Boolean> = mark.observeIsBookmarked(kind, refId)

    override suspend fun toggleBookmark(kind: String, refId: String, label: String) {
        if (mark.exists(kind, refId)) mark.delete(kind, refId) else mark.upsert(BookmarkEntity(kind, refId, label))
    }

    override fun recentHistory(limit: Int): Flow<List<HistItem>> =
        hist.observeRecent(limit).map { list -> list.map { HistItem(it.kind, it.refId, it.title, it.openedAt) } }

    override suspend fun pushHistory(kind: String, refId: String, title: String) {
        hist.insert(HistoryEntity(kind = kind, refId = refId, title = title))
    }

    override suspend fun clearHistory() = hist.clear()

    override fun progress(kind: String, refId: String): Flow<ProgressItem?> =
        prog.observe(kind, refId).map { list ->
            list.firstOrNull()?.let { p -> ProgressItem(p.kind, p.refId, p.value, p.updatedAt) }
        }

    override fun progressByKind(kind: String): Flow<List<ProgressItem>> =
        prog.observeByKind(kind).map { list -> list.map { ProgressItem(it.kind, it.refId, it.value, it.updatedAt) } }

    override suspend fun setProgress(kind: String, refId: String, value: Int) {
        prog.upsert(ProgressEntity(kind, refId, value))
    }
}
