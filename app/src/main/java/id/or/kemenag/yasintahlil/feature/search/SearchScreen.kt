package id.or.kemenag.yasintahlil.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
 * Pencarian terpadu atas **seluruh** konten (`plan.txt`: nama surat, nomor ayat, nama doa, bacaan
 * sholat, kata dalam terjemahan, bagian Tahlil, dan panduan ibadah) — bukan hanya Surat Yasin.
 *
 * Indeks dibangun **sekali** dari `ContentRepo` lalu disaring di memori oleh [Pencarian], sehingga
 * mengetik tidak membaca berkas berulang kali. Pembacaan dan penguraian JSON dijalankan di luar
 * thread UI; kegagalannya tidak ditelan diam-diam melainkan tampil sebagai [ErrorState] dengan
 * tombol mencoba kembali.
 *
 * Kotak carinya lebar dengan ikon kaca pembesar di kiri dan tombol kosongkan di kanan; saat kosong
 * tampil saran cepat dan pencarian terbaru sebagai chip — satu ketukan mengisi kotaknya, bukan
 * mengetik dari nol.
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(NamaLayar.SEARCH, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            singleLine = true,
            placeholder = { Text("Surat, ayat, doa, bacaan, panduan…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (q.isNotEmpty()) {
                    IconButton(onClick = { q = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = NamaAntarmuka.KOSONGKAN)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { catat() }),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        when {
            galat != null -> ErrorState(galat!!, onRetry = { ulang++ })
            indeks == null -> LoadingState("Menyiapkan indeks pencarian…")
            else -> {
                if (q.isBlank()) {
                    Text("Coba cari", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp))
                    // Saran katanya bukan id bacaan mana pun (mis. "qunut" adalah id pada
                    // `BacaanSholat` — kata yang diketik ulang di luar kosakatanya akan
                    // menyimpang dari id yang benar-benar dipakai bundelnya).
                    BarisChip {
                        listOf("Yasin", "tahlil", "ayat kursi", "wudhu", "rakaat").forEach { saran ->
                            AssistChip(onClick = { q = saran; catat() }, label = { Text(saran) })
                        }
                    }
                    if (recent.isNotEmpty()) {
                        Text("Pencarian terbaru", style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 16.dp))
                        // Jatah tampil "pencarian terbaru" memakai batas bersama yang sama dengan
                        // daftar riwayat lain (`BatasRiwayat.DITAMPILKAN`), bukan potongan sendiri.
                        BarisChip {
                            recent.take(BatasRiwayat.DITAMPILKAN).forEach { r ->
                                AssistChip(onClick = { q = r }, label = { Text(r) })
                            }
                        }
                    } else {
                        EmptyState(
                            judul = "Cari di seluruh konten",
                            pesan = "Ketik nama surat (mis. Yasin), nomor ayat, nama doa, lafaz " +
                                "bacaan sholat, kata dalam terjemahan, bagian Tahlil, atau panduan " +
                                "ibadah seperti wudhu dan tayamum.",
                        )
                    }
                } else {
                    Text(
                        if (hits.isEmpty()) "Tidak ada hasil untuk \"${q.trim()}\""
                        else "${hits.size} hasil untuk \"${q.trim()}\"",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    if (hits.isEmpty()) {
                        EmptyState(
                            judul = "Coba kata lain",
                            pesan = "Periksa ejaan, atau cari dengan nomor ayat (mis. 1).",
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
        aksi = {
            FilledTonalButton(onClick = buka) { Text(NamaAntarmuka.BUKA) }
        },
        // Dua lambda tak mungkin sekaligus menempati posisi ekor — keduanya diberi nama.
        isi = {
            Text(h.cuplikan, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
    )
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

    // Pustaka doa & dzikir — hasilnya membuka kelompoknya langsung, bukan sekadar menu induknya.
    repo.dzikir().forEach { grup ->
        grup.items.forEach { d ->
            add(SearchDoc(JenisBacaan.id(JenisBacaan.DZIKIR, d.judul), "Doa: ${d.judul}", listOf(d.judul, d.terjemahan, d.latin, d.arab), Routes.dzikirGrup(grup.id)))
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
