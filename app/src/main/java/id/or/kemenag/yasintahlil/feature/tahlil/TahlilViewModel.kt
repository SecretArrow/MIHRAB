package id.or.kemenag.yasintahlil.feature.tahlil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.data.TahlilItem
import id.or.kemenag.yasintahlil.core.data.TahlilSusunan
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TahlilUiState(
    val susunan: List<TahlilSusunan> = emptyList(),
    val susunanId: String = "",
    val items: List<TahlilItem> = emptyList(),
    val idx: Int = 0,
    val count: Int = 0,
    val arabSize: Float = 24f,
    val showLatin: Boolean = true,
    val showTrans: Boolean = true,
)

@HiltViewModel
class TahlilViewModel @Inject constructor(
    private val store: Store,
    private val repo: ContentRepo,
    private val userData: UserDataRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TahlilUiState())
    val state: StateFlow<TahlilUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val susunan = repo.tahlilSusunan()
            val saved = store.get(Store.TAHLIL_SUSUNAN, "")
            val id = susunan.firstOrNull { it.id == saved }?.id ?: susunan.firstOrNull()?.id ?: ""
            val items = repo.tahlil(id.ifBlank { null })
            val idx = userData.progress(KIND, "idx").first()?.value ?: 0
            val count = userData.progress(KIND, "count").first()?.value ?: 0
            _state.value = TahlilUiState(
                susunan = susunan,
                susunanId = id,
                items = items,
                idx = idx.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                count = count,
                arabSize = store.get(Store.ARAB_SIZE, 24f),
                showLatin = store.get(Store.SHOW_LATIN, true),
                showTrans = store.get(Store.SHOW_TRANS, true),
            )
        }
    }

    fun setSusunan(id: String) {
        val items = repo.tahlil(id.ifBlank { null })
        _state.update { it.copy(susunanId = id, items = items, idx = 0, count = 0) }
        viewModelScope.launch {
            store.set(Store.TAHLIL_SUSUNAN, id)
            userData.setProgress(KIND, "idx", 0)
            userData.setProgress(KIND, "count", 0)
        }
    }

    fun next() {
        val s = _state.value
        if (s.idx < s.items.size - 1) { _state.update { it.copy(idx = it.idx + 1, count = 0) }; persist() }
    }

    fun prev() {
        if (_state.value.idx > 0) { _state.update { it.copy(idx = it.idx - 1, count = 0) }; persist() }
    }

    fun jumpTo(i: Int) {
        if (i in _state.value.items.indices) { _state.update { it.copy(idx = i, count = 0) }; persist() }
    }

    fun setCount(v: Int) {
        _state.update { it.copy(count = v.coerceIn(0, it.items.getOrNull(it.idx)?.ulangi ?: 1)) }
        persist()
    }

    fun setArabSize(v: Float) { _state.update { it.copy(arabSize = v) }; viewModelScope.launch { store.set(Store.ARAB_SIZE, v) } }
    fun toggleLatin() { val v = !_state.value.showLatin; _state.update { it.copy(showLatin = v) }; viewModelScope.launch { store.set(Store.SHOW_LATIN, v) } }
    fun toggleTrans() { val v = !_state.value.showTrans; _state.update { it.copy(showTrans = v) }; viewModelScope.launch { store.set(Store.SHOW_TRANS, v) } }

    private fun persist() {
        val s = _state.value
        viewModelScope.launch {
            userData.setProgress(KIND, "idx", s.idx)
            userData.setProgress(KIND, "count", s.count)
            s.items.getOrNull(s.idx)?.let { store.set(Store.LAST, "tahlil:${it.id}") }
        }
    }

    companion object { const val KIND = "tahlil" }
}
