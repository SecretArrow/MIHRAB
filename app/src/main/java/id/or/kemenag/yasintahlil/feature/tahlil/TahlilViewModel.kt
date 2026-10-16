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
import id.or.kemenag.yasintahlil.core.data.local.KegiatanDao
import id.or.kemenag.yasintahlil.core.data.local.KegiatanEntity
import id.or.kemenag.yasintahlil.core.domain.repository.UserDataRepository
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.KunciProgres
import id.or.kemenag.yasintahlil.core.util.UkuranBacaan
import id.or.kemenag.yasintahlil.feature.khataman.RangkaianHari
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
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
    /**
     * Mode Imam aktif: bacaan memakai susunan Lengkap dan tiap kartu diberi lencana siapa yang
     * membaca (Bersama/Imam/Sendiri-sendiri). Terpisah dari [susunanId] — lihat [Store.TAHLIL_IMAM].
     */
    val modeImam: Boolean = false,
) {
    // Nama dan sumber susunan aktif tidak lagi dioper ke layar: keterangan susunan (termasuk
    // sumbernya, hingga sunnah.com) tinggal di layar keterangan yang dibuka lewat ikon Info —
    // dibaca ulang dari bundel yang sama, sehingga memperbarui bundelnya memperbarui keterangan
    // itu tanpa menyentuh ViewModel ini.

    val adaSusunanSaya: Boolean get() = susunanId == SusunanSaya.ID
}

@HiltViewModel
class TahlilViewModel @Inject constructor(
    private val store: Store,
    private val repo: ContentRepo,
    private val userData: UserDataRepository,
    private val kegiatanDao: KegiatanDao,
) : ViewModel() {

    private val _state = MutableStateFlow(TahlilUiState())
    val state: StateFlow<TahlilUiState> = _state.asStateFlow()

    init {
        load()
        // Kegiatan harian "tahlil_buka" untuk Khataman & Streak. Dicatat otomatis begitu layar ini
        // dibuka — rangkaian hari baca tidak boleh putus hanya karena pengguna lupa menekan
        // tombol "catat"; membuka layar bacaan memang sudah berarti membaca. Sekali per hari
        // (`jumlahHariIni`): membuka ulang layar ini di hari yang sama tidak menambah baris baru,
        // sebab rangkaian menghitung HARI, bukan jumlah kali membuka.
        viewModelScope.launch {
            val hariIni = RangkaianHari.iso(LocalDate.now())
            if (kegiatanDao.jumlahHariIni(hariIni, RangkaianHari.TAHIL_BUKA) == 0) {
                kegiatanDao.insert(
                    KegiatanEntity(
                        tanggal = hariIni,
                        jenis = RangkaianHari.TAHIL_BUKA,
                        keterangan = "",
                    ),
                )
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val semua = repo.tahlil(null)
            val pilihan = SusunanSaya.parse(store.get(Store.TAHLIL_CUSTOM, BawaanPreferensi.SUSUNAN_SAYA))
            val susunan = PreferensiSusunan.tersedia(repo.tahlilSusunan(), SusunanSaya.susunan(pilihan))
            val id = PreferensiSusunan.idAktif(
                store.get(Store.TAHLIL_SUSUNAN, BawaanPreferensi.SUSUNAN_TAHLIL_DIPILIH), susunan)
            val modeImam = store.get(Store.TAHLIL_IMAM, BawaanPreferensi.MODE_IMAM)
            // Mode Imam selalu membaca susunan Lengkap; bila nilai tersimpannya bukan Lengkap
            // (mis. pengguna terakhir memilih Ringkas lalu menyalakan mode Imam dari sesi lama),
            // susunan dasarnya mengikuti mode Imam.
            val idEfektif = if (modeImam && susunan.any { it.id == SUSUNAN_LENGKAP }) SUSUNAN_LENGKAP else id
            val items = itemsUntuk(idEfektif, semua, pilihan)
            val idx = userData.progress(KIND, KunciProgres.TAHLIL_POSISI).first()?.value ?: 0
            val count = userData.progress(KIND, KunciProgres.TAHLIL_HITUNGAN).first()?.value ?: 0
            _state.value = TahlilUiState(
                susunan = susunan,
                susunanId = idEfektif,
                items = items,
                semuaBagian = semua,
                pilihan = pilihan,
                idx = idx.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                count = count,
                arabSize = store.get(Store.ARAB_SIZE, UkuranBacaan.ARAB_DEFAULT),
                showLatin = store.get(Store.SHOW_LATIN, BawaanPreferensi.TAMPIL_LATIN),
                showTrans = store.get(Store.SHOW_TRANS, BawaanPreferensi.TAMPIL_TERJEMAHAN),
                showArab = store.get(Store.SHOW_ARAB, BawaanPreferensi.TAMPIL_ARAB),
                modeImam = modeImam,
            )
        }
    }

    /** Bagian yang ditampilkan untuk sebuah susunan: susunan pengguna disusun lokal, lainnya dari berkas. */
    private fun itemsUntuk(id: String, semua: List<TahlilItem>, pilihan: List<String>): List<TahlilItem> =
        if (id == SusunanSaya.ID) SusunanSaya.susun(semua, pilihan) else repo.tahlil(id.ifBlank { null })

    fun setSusunan(id: String) {
        val s = _state.value
        val items = itemsUntuk(id, s.semuaBagian, s.pilihan)
        _state.update { it.copy(susunanId = id, items = items, idx = 0, count = 0, modeImam = false) }
        viewModelScope.launch {
            store.set(Store.TAHLIL_SUSUNAN, id)
            store.set(Store.TAHLIL_IMAM, false)
            userData.setProgress(KIND, KunciProgres.TAHLIL_POSISI, 0)
            userData.setProgress(KIND, KunciProgres.TAHLIL_HITUNGAN, 0)
        }
    }

    /**
     * Nyalakan/matikan Mode Imam. Menyalakannya membawa susunan dasar ke Lengkap — bacaannya
     * memang persis Lengkap; lencananya saja yang ditambahkan. Mematikannya kembali ke susunan
     * dasar yang tercatat tanpa mengubah pilihan pengguna yang lain.
     */
    fun setModeImam(aktif: Boolean) {
        val s = _state.value
        if (aktif) {
            val punyaLengkap = s.susunan.any { it.id == SUSUNAN_LENGKAP }
            val id = if (punyaLengkap) SUSUNAN_LENGKAP else s.susunanId
            val items = itemsUntuk(id, s.semuaBagian, s.pilihan)
            _state.update { it.copy(susunanId = id, items = items, idx = 0, count = 0, modeImam = true) }
            viewModelScope.launch { store.set(Store.TAHLIL_SUSUNAN, id) }
        } else {
            _state.update { it.copy(modeImam = false, idx = 0, count = 0) }
        }
        viewModelScope.launch { store.set(Store.TAHLIL_IMAM, aktif) }
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
                modeImam = false,
            )
        }
        viewModelScope.launch {
            store.set(Store.TAHLIL_CUSTOM, SusunanSaya.serialize(bersih))
            store.set(Store.TAHLIL_SUSUNAN, SusunanSaya.ID)
            store.set(Store.TAHLIL_IMAM, false)
            userData.setProgress(KIND, KunciProgres.TAHLIL_POSISI, 0)
            userData.setProgress(KIND, KunciProgres.TAHLIL_HITUNGAN, 0)
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
            userData.setProgress(KIND, KunciProgres.TAHLIL_POSISI, 0)
            userData.setProgress(KIND, KunciProgres.TAHLIL_HITUNGAN, 0)
        }
    }

    fun jumpTo(i: Int) {
        if (i in _state.value.items.indices) { _state.update { it.copy(idx = i, count = 0) }; persist(); catat() }
    }

    fun setCount(v: Int) {
        _state.update { it.copy(count = v.coerceIn(0, it.items.getOrNull(it.idx)?.ulangi ?: 1)) }
        persist()
    }

    // Pengaturan tampilan bacaan terpusat di tab Pengaturan; layar ini hanya membacanya.

    private fun persist() {
        val s = _state.value
        viewModelScope.launch {
            userData.setProgress(KIND, KunciProgres.TAHLIL_POSISI, s.idx)
            userData.setProgress(KIND, KunciProgres.TAHLIL_HITUNGAN, s.count)
            s.items.getOrNull(s.idx)?.let { store.set(Store.LAST, JenisBacaan.id(JenisBacaan.TAHLIL, it.id)) }
        }
    }

    /**
     * Catat bagian yang sedang ditandai ke riwayat (`plan.txt` §"Pencarian, Favorit, dan Riwayat":
     * *"riwayat terakhir dibaca"*).
     *
     * Layar Tahlil kini bersambung seperti Surat Yasin, jadi posisi baca berpindah lewat
     * [jumpTo] saja: tombol "Ke bagian", tindakan "Tandai terakhir dibaca" pada kartunya,
     * maupun tombol penanda "Terakhir dibaca" — semuanya jalan yang sama menuju fungsi ini.
     * Bukan pada [setCount]: menghitung tasbih juga memanggil [persist], dan mencatat riwayat
     * tiap ketukan penghitung akan membanjiri daftar "Riwayat terbaru" dengan satu bagian
     * yang sama.
     */
    private fun catat() {
        val s = _state.value
        val bagian = s.items.getOrNull(s.idx) ?: return
        viewModelScope.launch {
            userData.pushHistory(KIND, bagian.id, "Tahlil bagian ${s.idx + 1}: ${bagian.judul}")
        }
    }

    // Jenis progres dan riwayat bacaan Tahlil datang dari kosakata bersama `JenisBacaan` — sama
    // dengan yang dipakai pemetaan tujuan tombol "Buka" pada Favorit & Riwayat — dan rujukan baris
    // progresnya dari `KunciProgres`, sama dengan yang dipakai Pengaturan saat mengosongkannya.
    companion object {
        const val KIND = JenisBacaan.TAHLIL

        /** Id susunan Lengkap pada `tahlil.json` — dasar bacaan Mode Imam. */
        const val SUSUNAN_LENGKAP = "nu_online_lengkap"
    }
}
