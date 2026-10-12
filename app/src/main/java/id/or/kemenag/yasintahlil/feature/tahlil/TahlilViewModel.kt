package id.or.kemenag.yasintahlil.feature.tahlil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.or.kemenag.yasintahlil.core.data.BawaanPreferensi
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.data.PreferensiSusunan
import id.or.kemenag.yasintahlil.core.data.Store
import id.or.kemenag.yasintahlil.core.data.TahlilItem
import id.or.kemenag.yasintahlil.core.data.TahlilSusunan
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
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
    /** Seluruh bagian yang tersedia, apa pun susunan yang aktif — bahan penyusun "Susunan saya". */
    val semuaBagian: List<TahlilItem> = emptyList(),
    /** Id bagian pilihan pengguna (urut) untuk "Susunan saya"; kosong = belum disusun. */
    val pilihan: List<String> = emptyList(),
    val idx: Int = 0,
    val count: Int = 0,
    val arabSize: Float = UkuranBacaan.ARAB_DEFAULT,
    // Sama dengan bawaan yang dibaca dari preferensi, supaya tampilan tidak sempat berubah saat
    // DataStore selesai dimuat.
    val showLatin: Boolean = BawaanPreferensi.TAMPIL_LATIN,
    val showTrans: Boolean = BawaanPreferensi.TAMPIL_TERJEMAHAN,
    val showArab: Boolean = BawaanPreferensi.TAMPIL_ARAB,
) {
    /** Nama susunan yang sedang aktif — dipakai layar agar tidak menyebut susunan yang keliru. */
    val namaSusunan: String get() = susunan.firstOrNull { it.id == susunanId }?.nama.orEmpty()

    /**
     * Sumber susunan yang sedang aktif (`plan.txt` §"Fitur Tahlil Lengkap": *"berdasarkan sumber
     * atau tradisi yang jelas"*). Kosong untuk "Susunan saya" — susunan buatan pengguna tidak
     * mengklaim rujukan apa pun, dan layar harus mengatakannya alih-alih mengarang sumber.
     */
    val sumberSusunan: String get() = susunan.firstOrNull { it.id == susunanId }?.sumber.orEmpty()

    val adaSusunanSaya: Boolean get() = susunanId == SusunanSaya.ID
}

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
            val semua = repo.tahlil(null)
            val pilihan = SusunanSaya.parse(store.get(Store.TAHLIL_CUSTOM, BawaanPreferensi.SUSUNAN_SAYA))
            val susunan = PreferensiSusunan.tersedia(repo.tahlilSusunan(), SusunanSaya.susunan(pilihan))
            val id = PreferensiSusunan.idAktif(
                store.get(Store.TAHLIL_SUSUNAN, BawaanPreferensi.SUSUNAN_TAHLIL_DIPILIH), susunan)
            val items = itemsUntuk(id, semua, pilihan)
            val idx = userData.progress(KIND, "idx").first()?.value ?: 0
            val count = userData.progress(KIND, "count").first()?.value ?: 0
            _state.value = TahlilUiState(
                susunan = susunan,
                susunanId = id,
                items = items,
                semuaBagian = semua,
                pilihan = pilihan,
                idx = idx.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                count = count,
                arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT),
                showLatin = store.get(Store.SHOW_LATIN, BawaanPreferensi.TAMPIL_LATIN),
                showTrans = store.get(Store.SHOW_TRANS, BawaanPreferensi.TAMPIL_TERJEMAHAN),
                showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB),
            )
        }
    }

    /** Bagian yang ditampilkan untuk sebuah susunan: susunan pengguna disusun lokal, lainnya dari berkas. */
    private fun itemsUntuk(id: String, semua: List<TahlilItem>, pilihan: List<String>): List<TahlilItem> =
        if (id == SusunanSaya.ID) SusunanSaya.susun(semua, pilihan) else repo.tahlil(id.ifBlank { null })

    fun setSusunan(id: String) {
        val s = _state.value
        val items = itemsUntuk(id, s.semuaBagian, s.pilihan)
        _state.update { it.copy(susunanId = id, items = items, idx = 0, count = 0) }
        viewModelScope.launch {
            store.set(Store.TAHLIL_SUSUNAN, id)
            userData.setProgress(KIND, "idx", 0)
            userData.setProgress(KIND, "count", 0)
        }
    }

    /**
     * Simpan "Susunan saya" dari pilihan pengguna, lalu langsung pindah ke susunan itu. Daftar
     * kosong tidak diizinkan (susunan tanpa bagian hanya akan membuat layar buntu), sehingga
     * tombol simpan dimatikan sebelum ada bagian yang dipilih.
     */
    fun simpanSusunanSaya(ids: List<String>) {
        val bersih = SusunanSaya.parse(SusunanSaya.serialize(ids))
        if (bersih.isEmpty()) return
        val s = _state.value
        val items = SusunanSaya.susun(s.semuaBagian, bersih)
        val lain = s.susunan.filterNot { it.id == SusunanSaya.ID }
        _state.update {
            it.copy(
                susunan = lain + listOfNotNull(SusunanSaya.susunan(bersih)),
                susunanId = SusunanSaya.ID,
                pilihan = bersih,
                items = items,
                idx = 0,
                count = 0,
            )
        }
        viewModelScope.launch {
            store.set(Store.TAHLIL_CUSTOM, SusunanSaya.serialize(bersih))
            store.set(Store.TAHLIL_SUSUNAN, SusunanSaya.ID)
            userData.setProgress(KIND, "idx", 0)
            userData.setProgress(KIND, "count", 0)
        }
    }

    /** Buang "Susunan saya" dan kembali ke susunan pertama dari berkas konten. */
    fun hapusSusunanSaya() {
        val s = _state.value
        val lain = s.susunan.filterNot { it.id == SusunanSaya.ID }
        val id = PreferensiSusunan.idAktif("", lain)
        val items = itemsUntuk(id, s.semuaBagian, emptyList())
        _state.update {
            it.copy(susunan = lain, susunanId = id, pilihan = emptyList(), items = items, idx = 0, count = 0)
        }
        viewModelScope.launch {
            store.set(Store.TAHLIL_CUSTOM, "")
            store.set(Store.TAHLIL_SUSUNAN, id)
            userData.setProgress(KIND, "idx", 0)
            userData.setProgress(KIND, "count", 0)
        }
    }

    fun next() {
        val s = _state.value
        if (s.idx < s.items.size - 1) { _state.update { it.copy(idx = it.idx + 1, count = 0) }; persist(); catat() }
    }

    fun prev() {
        if (_state.value.idx > 0) { _state.update { it.copy(idx = it.idx - 1, count = 0) }; persist(); catat() }
    }

    fun jumpTo(i: Int) {
        if (i in _state.value.items.indices) { _state.update { it.copy(idx = i, count = 0) }; persist(); catat() }
    }

    fun setCount(v: Int) {
        _state.update { it.copy(count = v.coerceIn(0, it.items.getOrNull(it.idx)?.ulangi ?: 1)) }
        persist()
    }

    fun setArabSize(v: Float) { _state.update { it.copy(arabSize = v) }; viewModelScope.launch { store.set(Store.ARAB_SIZE, v) } }
    fun setShowLatin(v: Boolean) { _state.update { it.copy(showLatin = v) }; viewModelScope.launch { store.set(Store.SHOW_LATIN, v) } }
    fun setShowTrans(v: Boolean) { _state.update { it.copy(showTrans = v) }; viewModelScope.launch { store.set(Store.SHOW_TRANS, v) } }
    fun setShowArab(v: Boolean) { _state.update { it.copy(showArab = v) }; viewModelScope.launch { store.set(Store.SHOW_ARAB, v) } }

    private fun persist() {
        val s = _state.value
        viewModelScope.launch {
            userData.setProgress(KIND, "idx", s.idx)
            userData.setProgress(KIND, "count", s.count)
            s.items.getOrNull(s.idx)?.let { store.set(Store.LAST, JenisBacaan.id(JenisBacaan.TAHLIL, it.id)) }
        }
    }

    /**
     * Catat bagian yang sedang dibaca ke riwayat (`plan.txt` §"Pencarian, Favorit, dan Riwayat":
     * *"riwayat terakhir dibaca"*).
     *
     * Dipanggil hanya saat **posisi baca berpindah** ([next], [prev], [jumpTo]) — bukan pada
     * [setCount]. Menghitung tasbih juga memanggil [persist], dan mencatat riwayat tiap ketukan
     * penghitung akan membanjiri daftar "Riwayat terbaru" dengan satu bagian yang sama.
     */
    private fun catat() {
        val s = _state.value
        val bagian = s.items.getOrNull(s.idx) ?: return
        viewModelScope.launch {
            userData.pushHistory(KIND, bagian.id, "Tahlil bagian ${s.idx + 1}: ${bagian.judul}")
        }
    }

    // Jenis progres dan riwayat bacaan Tahlil datang dari kosakata bersama `JenisBacaan` — sama
    // dengan yang dipakai pemetaan tujuan tombol "Buka" pada Favorit & Riwayat.
    companion object { const val KIND = JenisBacaan.TAHLIL }
}
