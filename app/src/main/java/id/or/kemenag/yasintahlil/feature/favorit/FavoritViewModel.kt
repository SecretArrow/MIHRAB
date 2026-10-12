package id.or.kemenag.yasintahlil.feature.favorit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.domain.model.BookmarkItem
import id.or.kemenag.yasintahlil.core.domain.model.FavItem
import id.or.kemenag.yasintahlil.core.domain.model.HistItem
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritUiState(
    val favorites: List<FavItem> = emptyList(),
    val bookmarks: List<BookmarkItem> = emptyList(),
    val history: List<HistItem> = emptyList(),
    val last: String = "",
)

@HiltViewModel
class FavoritViewModel @Inject constructor(
    private val store: Store,
    private val userData: UserDataRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritUiState())
    val state: StateFlow<FavoritUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { userData.favorites().collect { f -> _state.update { it.copy(favorites = f) } } }
        viewModelScope.launch { userData.allBookmarks().collect { b -> _state.update { it.copy(bookmarks = b) } } }
        viewModelScope.launch { userData.recentHistory(20).collect { h -> _state.update { it.copy(history = h) } } }
        viewModelScope.launch { _state.update { it.copy(last = store.get(Store.LAST, "")) } }
    }

    fun remove(item: FavItem) {
        viewModelScope.launch { userData.toggleFavorite(item.kind, item.refId, item.title) }
    }

    fun removeBookmark(item: BookmarkItem) {
        viewModelScope.launch { userData.toggleBookmark(item.kind, item.refId, item.label) }
    }

    fun clearHistory() {
        viewModelScope.launch { userData.clearHistory() }
    }
}
