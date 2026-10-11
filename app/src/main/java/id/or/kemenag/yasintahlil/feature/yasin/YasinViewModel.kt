package id.or.kemenag.yasintahlil.feature.yasin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.data.Surah
import id.or.kemenag.yasintahlil.core.domain.model.BookmarkItem
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YasinUiState(
    val arabSize: Float = 24f,
    val latinSize: Float = 16f,
    val lineHeight: Float = 1.9f,
    val showLatin: Boolean = true,
    val showTrans: Boolean = true,
    val mark: Int = 0,
    val bookmarks: List<BookmarkItem> = emptyList(),
)

@HiltViewModel
class YasinViewModel @Inject constructor(
    private val store: Store,
    private val repo: ContentRepo,
    private val userData: UserDataRepository,
) : ViewModel() {

    val surah: Surah = repo.yasin

    private val _state = MutableStateFlow(YasinUiState())
    val state: StateFlow<YasinUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    arabSize = store.get(Store.ARAB_SIZE, 24f),
                    latinSize = store.get(Store.LATIN_SIZE, 16f),
                    lineHeight = store.get(Store.LINE_HEIGHT, 1.9f),
                    showLatin = store.get(Store.SHOW_LATIN, true),
                    showTrans = store.get(Store.SHOW_TRANS, true),
                    mark = store.get(Store.YASIN_MARK, 0),
                )
            }
        }
        // Bookmark tersimpan di Room (tahan rotasi & process-death).
        viewModelScope.launch {
            userData.bookmarks("yasin").collect { list -> _state.update { s -> s.copy(bookmarks = list) } }
        }
    }

    fun setArabSize(v: Float) { _state.update { it.copy(arabSize = v) }; save { store.set(Store.ARAB_SIZE, v) } }

    fun setLatinSize(v: Float) { _state.update { it.copy(latinSize = v) }; save { store.set(Store.LATIN_SIZE, v) } }

    fun setLineHeight(v: Float) { _state.update { it.copy(lineHeight = v) }; save { store.set(Store.LINE_HEIGHT, v) } }

    fun toggleLatin() {
        val v = !_state.value.showLatin
        _state.update { it.copy(showLatin = v) }; save { store.set(Store.SHOW_LATIN, v) }
    }

    fun toggleTrans() {
        val v = !_state.value.showTrans
        _state.update { it.copy(showTrans = v) }; save { store.set(Store.SHOW_TRANS, v) }
    }

    fun markRead(n: Int) {
        _state.update { it.copy(mark = n) }
        save { store.set(Store.YASIN_MARK, n); store.set(Store.LAST, "yasin:$n") }
    }

    fun toggleBookmark(n: Int) {
        viewModelScope.launch { userData.toggleBookmark("yasin", n.toString(), "Yasin ayat $n") }
    }

    private fun save(block: suspend () -> Unit) { viewModelScope.launch { block() } }
}
