package id.or.kemenag.yasintahlil.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import id.or.kemenag.yasintahlil.core.data.BerkasKonten
import id.or.kemenag.yasintahlil.core.data.ContentRepo
import id.or.kemenag.yasintahlil.core.designsystem.*
import id.or.kemenag.yasintahlil.core.util.BatasRiwayat
import id.or.kemenag.yasintahlil.core.util.JenisBacaan
import id.or.kemenag.yasintahlil.core.util.JudulBacaan
import id.or.kemenag.yasintahlil.core.util.NamaLayar
import id.or.kemenag.yasintahlil.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pencarian terpadu atas seluruh konten (`plan.txt`: nama surat, nomor ayat, nama doa, bacaan
 * sholat, kata dalam terjemahan, bagian Tahlil, dan panduan ibadah).
 *
 * Indeks dibangun **sekali** dari `ContentRepo` lalu disaring di memori oleh [Pencarian], sehingga
 * mengetik tidak membaca berkas berulang kali. Pembacaan dan penguraian JSON dijalankan di luar
 * thread UI; kegagalannya tidak lagi ditelan diam-diam melainkan tampil sebagai [ErrorState]
 * dengan tombol mencoba kembali.
 */
@Composable
fun SearchScreen(go: (String) -> Unit, q0: String = "") {
    val repo = rememberRepo()
    val userData = rememberUserData()
    val scope = rememberCoroutineScope()
    // rememberSaveable: kata kunci yang sedang diketik tidak hilang saat layar diputar ulang.
    // `recent`, `indeks`, `galat`, dan `ulang` sengaja tetap `remember` — ketiganya dibangun ulang
    // oleh efek di bawah, bukan pilihan pengguna yang perlu dipertahankan.
    var q by rememberSaveable { mutableStateOf(q0) }
    var recent by remember { mutableStateOf(listOf<String>()) }
    var indeks by remember { mutableStateOf<List<SearchDoc>?>(null) }
    var galat by remember { mutableStateOf<String?>(null) }
    var ulang by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        userData.recentHistory(BatasRiwayat.DIBACA_RINGKAS).collect { list ->
            recent = list.filter { it.kind == JenisBacaan.SEARCH }.map { it.title }.distinct()
        }
    }

    // `ulang` dinaikkan oleh tombol "Coba lagi": indeks disusun ulang dari nol.
    LaunchedEffect(ulang) {
        galat = null
        indeks = null
        runCatching { withContext(Dispatchers.IO) { bangunIndeks(repo) } }
            .onSuccess { indeks = it }
            .onFailure { galat = "Indeks pencarian gagal dimuat: ${sebab(it)}" }
    }

    val hits = remember(q, indeks) { indeks?.let { Pencarian.cari(it, q) }.orEmpty() }

    /** Catat kata kunci ke riwayat supaya "pencarian terbaru" terisi meski hasilnya tak dibuka. */
    fun catat() {
        val kata = q.trim()
        if (kata.isNotBlank()) scope.launch { userData.pushHistory(JenisBacaan.SEARCH, kata, kata) }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text(NamaLayar.SEARCH, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            label = { Text("surat, ayat, doa, bacaan, panduan") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { catat() }),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )

        when {
            galat != null -> ErrorState(galat!!, onRetry = { ulang++ })
            indeks == null -> LoadingState("Menyiapkan indeks pencarian…")
            else -> {
                if (q.isNotBlank()) {
                    Text("${hits.size} hasil", style = MaterialTheme.typography.labelMedium)
                }
                if (q.isBlank()) {
                    if (recent.isNotEmpty()) {
                        Text("Pencarian terbaru:", style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp))
                        // Jatah tampil "pencarian terbaru" memakai batas bersama yang sama dengan
                        // daftar riwayat lain; sebelumnya layar ini memotongnya sendiri di angka 10
                        // sehingga jumlah yang benar-benar tampil berbeda dari yang dijanjikan
                        // `BatasRiwayat.DITAMPILKAN`.
                        recent.take(BatasRiwayat.DITAMPILKAN).forEach { r -> TextButton(onClick = { q = r }) { Text(r) } }
                    } else {
                        EmptyState(
                            judul = "Cari di seluruh konten",
                            pesan = "Ketik nama surat (mis. Yasin), nomor ayat, nama doa, lafaz bacaan " +
                                "sholat, kata dalam terjemahan, bagian Tahlil, atau panduan ibadah " +
                                "seperti wudhu dan tayamum.",
                        )
                    }
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(hits) { h ->
                        KartuHasil(h) {
                            scope.launch { userData.pushHistory(JenisBacaan.SEARCH, h.id, q.trim()) }
                            go(h.rute)
                        }
                    }
                    if (q.isNotBlank() && hits.isEmpty()) {
                        item {
                            EmptyState(
                                judul = "Tidak ada hasil untuk \"${q.trim()}\"",
                                pesan = "Coba kata lain, periksa ejaan, atau cari dengan nomor ayat.",
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Kartu satu hasil: judul, cuplikan yang memuat kata kunci, dan tombol menuju layarnya. */
@Composable
private fun KartuHasil(h: Hit, buka: () -> Unit) {
    KartuEntri(
        judul = h.judul,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Text(h.cuplikan, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    } aksi {
        FilledTonalButton(onClick = buka) { Text(NamaAntarmuka.BUKA) }
    }
}

private fun sebab(t: Throwable): String =
    t.message?.takeIf { it.isNotBlank() } ?: t::class.simpleName.orEmpty().ifBlank { "sebab tak dikenal" }

/**
 * Susun indeks dari seluruh konten yang dapat dicari. Setiap entri menyebut rutenya sendiri
 * sehingga hasil pencarian selalu bisa dibuka — tidak ada hasil yang berujung ke jalan buntu.
 *
 * Sumbernya persis sama dengan yang dibaca layar-layarnya (`ContentRepo`), termasuk lafaz Arab,
 * sehingga pengguna yang menempelkan potongan ayat pun menemukannya.
 */
private fun bangunIndeks(repo: ContentRepo): List<SearchDoc> = buildList {
    // Surat Yasin: nama surat plus seluruh ayatnya (nomor ayat dicocokkan persis).
    val yasin = repo.yasin
    add(SearchDoc("surat:yasin", "Surat ${yasin.nama}", listOf(yasin.nama), Routes.YASIN))
    yasin.verses.forEach { v ->
        add(
            SearchDoc(
                id = JenisBacaan.id(JenisBacaan.YASIN, v.nomor.toString()),
                judul = JudulBacaan.yasinAyat(v.nomor),
                teks = listOf(v.terjemahan, v.latin, v.arab),
                rute = Routes.YASIN,
                nomor = v.nomor,
            )
        )
    }

    // Bagian Tahlil — termasuk surat Al-Fatihah/Al-Ikhlas/Al-Falaq/An-Nas yang dibaca di dalamnya.
    repo.tahlil().forEach { t ->
        add(SearchDoc(JenisBacaan.id(JenisBacaan.TAHLIL, t.id), "Tahlil: ${t.judul}", listOf(t.judul, t.terjemahan, t.latin, t.arab), Routes.TAHLIL))
    }

    // Pustaka doa & dzikir.
    repo.dzikir().forEach { grup ->
        grup.items.forEach { d ->
            add(SearchDoc(JenisBacaan.id(JenisBacaan.DZIKIR, d.judul), "Doa: ${d.judul}", listOf(d.judul, d.terjemahan, d.latin, d.arab), Routes.DZIKIR))
        }
    }

    // Panduan sholat wajib & sunnah beserta ketentuannya.
    (repo.sholatWajib() + repo.sholatSunnah()).forEach { s ->
        add(SearchDoc("sholat:${s.id}", "Sholat: ${s.nama}", listOf(s.nama, s.deskripsi, s.waktu, s.rakaat) + s.ketentuan, Routes.SHOLAT))
    }

    // Bacaan sholat (lafaz, kapan dibaca, dan terjemahannya).
    repo.sholatBacaan().forEach { b ->
        add(SearchDoc(JenisBacaan.id(JenisBacaan.BACAAN, b.id), "Bacaan: ${b.judul}", listOf(b.judul, b.kapan, b.terjemahan, b.latin, b.arab), Routes.SHOLAT))
    }

    // Panduan bersuci: wudhu, tayamum, rukun, dan pembatal wudhu.
    val bersuci = repo.panduanBersuci()
    add(SearchDoc("wudhu:rukun", "Wudhu: rukun", bersuci.rukun, Routes.WUDHU))
    bersuci.wudhu.forEach { l ->
        add(SearchDoc("wudhu:${l.no}", "Wudhu langkah ${l.no}: ${l.judul}", listOf(l.judul, l.keterangan), Routes.WUDHU))
    }
    bersuci.tayamum.forEach { l ->
        add(SearchDoc("tayamum:${l.no}", "Tayamum langkah ${l.no}: ${l.judul}", listOf(l.judul, l.keterangan), Routes.WUDHU))
    }
    add(SearchDoc("wudhu:pembatal", "Pembatal wudhu", bersuci.pembatal, Routes.WUDHU))

    // Syarat & persiapan sholat — kerangka berbahasa Indonesia tanpa lafaz (lihat
    // `sholat_syarat.json`). Tanpa ini, kata seperti "aurat" atau "najis" tidak dapat dicari.
    val syarat = repo.sholatSyarat()
    add(SearchDoc("syarat:wajib", "Sholat: syarat wajib", syarat.syaratWajib, Routes.SHOLAT))
    add(SearchDoc("syarat:sah", "Sholat: syarat sah", syarat.syaratSah, Routes.SHOLAT))
    add(SearchDoc("syarat:persiapan", "Sholat: persiapan sebelum sholat", syarat.persiapan, Routes.SHOLAT))
    add(SearchDoc("syarat:aurat", "Sholat: penutup aurat",
        listOf(syarat.penutupAurat.lakiLaki, syarat.penutupAurat.perempuan, syarat.penutupAurat.catatan),
        Routes.SHOLAT))
    add(SearchDoc("syarat:tempat", "Sholat: kebersihan tempat sholat", syarat.kebersihanTempat, Routes.SHOLAT))

    // Sholat khusus (ghaib & jenazah): ketentuan, langkah, dan varian niatnya. Nama berkas dan
    // layar tujuannya datang dari `BerkasKonten` — daftar yang sama dengan yang dibaca kedua layar
    // itu, sehingga indeks pencarian tidak dapat mendaftar modul yang layarnya membaca berkas lain.
    BerkasKonten.SHOLAT_KHUSUS.forEach { m ->
        val k = repo.sholatKhusus(m.berkas)
        add(
            SearchDoc(
                id = "khusus:${m.berkas}",
                judul = "Sholat: ${k.judul}",
                teks = listOf(k.judul, k.hukum, k.dalil) + k.syarat + k.langkah,
                rute = m.rute,
            )
        )
        k.niat.forEach { n ->
            add(SearchDoc("khusus:${m.berkas}:${n.id}", "${k.judul} — ${n.judul}", listOf(n.judul, n.terjemahan, n.latin, n.arab), m.rute))
        }
    }

    // Doa seputar adzan dan lafaz iqomah.
    listOf(repo.doaAdzan(), repo.menjawabAdzan(), repo.sholawatAdzan()).forEach { doa ->
        add(SearchDoc(JenisBacaan.id(JenisBacaan.ADZAN, doa.judul), doa.judul, listOf(doa.judul, doa.terjemahan, doa.latin, doa.arab), Routes.DOA_ADZAN))
    }
    repo.iqomah().forEachIndexed { i, lafaz ->
        // Id lafaz memakai aturan bernomor urut 1 yang sama dengan kartunya di layar Iqomah —
        // sebelumnya indeks mentah (`i`) membuat dokumen ini menyimpan `iqomah:0` untuk lafaz yang
        // kartunya `iqomah:1`, sehingga favorit lafaz itu tidak menempel pada hasil pencariannya.
        add(SearchDoc(JenisBacaan.idLafazIqomah(i + 1), "Iqomah — lafaz ${i + 1}", listOf(lafaz.terjemahan, lafaz.latin, lafaz.arab), Routes.IQOMAH))
    }
}
