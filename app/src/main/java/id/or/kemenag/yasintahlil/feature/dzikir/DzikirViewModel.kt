package id.or.kemenag.yasintahlil.feature.dzikir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.DzikirGroup
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DzikirUiState(
    val arabSize: Float = UkuranBacaan.ARAB_DEFAULT,
    val showLatin: Boolean = true,
    val showTrans: Boolean = true,
    val showArab: Boolean = true,
    val counts: Map<String, Int> = emptyMap(),
)

/** Counter dzikir persisten (Room) — tetap tersimpan setelah keluar-masuk layar atau aplikasi ditutup. */
@HiltViewModel
class DzikirViewModel @Inject constructor(
    private val store: Store,
    private val repo: ContentRepo,
    private val userData: UserDataRepository,
) : ViewModel() {

    val groups: List<DzikirGroup> = repo.dzikir()

    private val _state = MutableStateFlow(DzikirUiState())
    val state: StateFlow<DzikirUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT),
                    showLatin = store.get(Store.SHOW_LATIN, true),
                    showTrans = store.get(Store.SHOW_TRANS, true),
                    showArab = store.get(Store.SHOW_ARAB, true),
                )
            }
        }
        viewModelScope.launch {
            userData.progressByKind(KIND).collect { list ->
                _state.update { s -> s.copy(counts = list.associate { it.refId to it.value }) }
            }
        }
    }

    fun setArabSize(v: Float) { _state.update { it.copy(arabSize = v) }; save { store.set(Store.ARAB_SIZE, v) } }

    fun setShowLatin(v: Boolean) { _state.update { it.copy(showLatin = v) }; save { store.set(Store.SHOW_LATIN, v) } }

    fun setShowTrans(v: Boolean) { _state.update { it.copy(showTrans = v) }; save { store.set(Store.SHOW_TRANS, v) } }

    fun setShowArab(v: Boolean) { _state.update { it.copy(showArab = v) }; save { store.set(Store.SHOW_ARAB, v) } }

    fun setCount(key: String, value: Int) {
        val v = value.coerceAtLeast(0)
        _state.update { it.copy(counts = it.counts + (key to v)) }
        viewModelScope.launch { userData.setProgress(KIND, key, v) }
    }

    fun reset(key: String) = setCount(key, 0)

    private fun save(block: suspend () -> Unit) { viewModelScope.launch { block() } }

    companion object { const val KIND = "dzikir" }
}
