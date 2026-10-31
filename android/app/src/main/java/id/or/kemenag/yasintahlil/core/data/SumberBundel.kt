package id.or.kemenag.yasintahlil.core.data

/**
 * Hasil pembacaan domain sumber dari bundel konten: yang **berhasil** dibaca, dan berkas yang
 * **gagal** dibaca beserta sebabnya.
 *
 * Dua hal ini dahulu tidak dapat dibedakan. [ContentRepo.sumberDipakai] melewati berkas yang gagal
 * diurai (`mapNotNull { runCatching { doc(f) }.getOrNull() }`) dan tetap mengembalikan daftar yang
 * tampak utuh, sehingga layar Tentang — yang menjanjikan bahwa daftar sumbernya *"tidak dapat
 * bertambah diam-diam"* — justru dapat **kehilangan** domain tanpa satu pun tanda. Bagi pengguna,
 * daftar yang menyusut itu sama saja dengan daftar yang lengkap: ia tak punya cara mengetahuinya.
 *
 * Kegagalan karena itu dibawa keluar, bukan ditelan: [gagal] menyebut berkas yang tak terbaca,
 * dan [lengkap] bernilai `false` selama daftar [host] belum tentu memuat seluruh domain yang
 * dirujuk bundel.
 */
data class SumberBundel(
    /** Domain resmi yang benar-benar dirujuk bundel, unik dan terurut. */
    val host: List<String>,
    /** Berkas konten yang gagal dibaca, dalam bentuk `nama (sebab)`. */
    val gagal: List<String>,
) {
    /** Bundel terbaca utuh: [host] memang seluruh domain yang dirujuk, bukan sebagian. */
    val lengkap: Boolean get() = gagal.isEmpty()
}
