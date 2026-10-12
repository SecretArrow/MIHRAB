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
    val showLatin: Boolean = true,
    val showTrans: Boolean = true,
    val showArab: Boolean = true,
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
                    showLatin = store.get(Store.SHOW_LATIN, true),
                    showTrans = store.get(Store.SHOW_TRANS, true),
                    showArab = store.get(Store.SHOW_ARAB, true),
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

    fun setShowLatin(v: Boolean) { _state.update { it.copy(showLatin = v) }; save { store.set(Store.SHOW_LATIN, v) } }

    fun setShowTrans(v: Boolean) { _state.update { it.copy(showTrans = v) }; save { store.set(Store.SHOW_TRANS, v) } }

    fun setShowArab(v: Boolean) { _state.update { it.copy(showArab = v) }; save { store.set(Store.SHOW_ARAB, v) } }

    fun markRead(n: Int) {
        _state.update { it.copy(mark = n) }
        save { store.set(Store.YASIN_MARK, n); store.set(Store.LAST, "yasin:$n") }
        // Riwayat "terakhir dibaca" (`plan.txt` §"Pencarian, Favorit, dan Riwayat"). Sebelumnya
        // modul Yasin tidak pernah menulis riwayat sama sekali: satu-satunya penulis riwayat adalah
        // tombol "Salin" pada kartu bacaan, sehingga daftar "Riwayat terbaru" tidak memuat bacaan
        // yang benar-benar dibuka. Ditulis di sini — bukan pada tiap gulir — karena `markRead`
        // hanya dipanggil saat pengguna memang menandai posisi bacaannya.
        save { userData.pushHistory("yasin", n.toString(), "Yasin ayat $n") }
    }

    fun toggleBookmark(n: Int) {
        viewModelScope.launch { userData.toggleBookmark("yasin", n.toString(), "Yasin ayat $n") }
    }

    private fun save(block: suspend () -> Unit) { viewModelScope.launch { block() } }
}
