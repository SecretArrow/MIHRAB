package id.or.kemenag.yasintahlil.core.data

/**
 * Bentuk **berkas cadangan** (backup) pengguna: nama kunci di dalam JSON-nya, sekali untuk selamanya.
 *
 * Berkas cadangan punya dua sisi yang tak pernah bertemu — **penulis** dan **pembaca** — dan
 * keduanya dahulu mengeja kunci yang sama secara terpisah: `SettingsScreen` menulis
 * `{"version":1,"prefs":…,"userData":…}`, `RoomUserDataRepository.exportJson` menulis larik
 * `favorites`/`bookmarks`/`progress`/`history` beserta medannya, lalu `restoreJson` dan layar
 * Pengaturan **mengeja ulang setiap satu** nama itu untuk membacanya kembali. Enam belas nama, dua
 * salinan, tak satu pun diperiksa.
 *
 * Bedanya sunyi, dan kerusakannya jauh lebih besar daripada pada preferensi. `restoreJson` dahulu
 * **mengosongkan keempat tabel lebih dahulu**, baru mengisi ulang dari berkas. Jadi satu nama kunci
 * yang meleset di sisi penulis — `"userData"` menjadi `"user_data"`, `"favorites"` menjadi `"favs"`
 * — membuat pembaca tak menemukan apa pun, mengosongkan seluruh favorit, bookmark, riwayat, dan
 * progres, lalu memasukkan nol baris. Pengguna menekan "Pulihkan", tak melihat satu pun pesan galat,
 * dan **kehilangan seluruh datanya** — termasuk data yang sedang tersimpan sebelum pemulihan, yang
 * sebenarnya tidak diminta diganti.
 *
 * Karena itu aturannya tinggal di sini, dan satu hal lagi ikut diperbaiki di tempat pemakaiannya:
 * pemulihan hanya boleh menyentuh bagian yang **benar-benar ada di berkas**. Dua aturan itu saling
 * melengkapi — tanpa yang kedua, satu kunci yang berganti nama tetap menghapus data; tanpa yang
 * pertama, tak ada yang memberi tahu bahwa kuncinya berganti nama.
 *
 * **Nilainya dibekukan.** Kunci-kunci ini sudah tertulis di berkas cadangan milik pengguna; mengganti
 * salah satunya membuat berkas lama tak terbaca. Mengubahnya harus disengaja dan disertai pembacaan
 * bentuk lama.
 *
 * Nama keempat larik **kebetulan sama** dengan nama tabel Room-nya (`@Entity(tableName = "favorites")`
 * di `Entities.kt`). Keduanya sengaja tidak disatukan: nama tabel adalah urusan skema penyimpanan,
 * nama kunci adalah urusan berkas yang keluar dari aplikasi, dan menyatukannya berarti satu perubahan
 * skema diam-diam mengubah bentuk berkas cadangan.
 */
object FormatCadangan {

    /** Versi bentuk berkas. Berkas v1 memuat `prefs` dan `userData`; berkas lama memuat prefs saja. */
    const val VERSI = 1

    // ---- Kunci tingkat atas ---------------------------------------------------------------------

    /** Versi bentuk berkas ([VERSI]). */
    const val KUNCI_VERSI = "version"

    /** Objek preferensi ([Store.exportJson]). */
    const val KUNCI_PREFERENSI = "prefs"

    /** Objek data terstruktur ([RoomUserDataRepository.exportJson]). */
    const val KUNCI_DATA_PENGGUNA = "userData"

    // ---- Larik di dalam `userData` ---------------------------------------------------------------

    const val FAVORIT = "favorites"
    const val BOOKMARK = "bookmarks"
    const val PROGRES = "progress"
    const val RIWAYAT = "history"

    // ---- Medan di dalam tiap entri ---------------------------------------------------------------

    /** Jenis bacaan, mis. `yasin` atau `dzikir`. */
    const val KIND = "kind"

    /** Rujukan di dalam jenis itu, mis. nomor ayat. */
    const val REF_ID = "refId"

    /** Judul entri (favorit, riwayat). */
    const val JUDUL = "title"

    /** Label entri (bookmark). */
    const val LABEL = "label"

    /** Nilai progres bacaan. */
    const val NILAI = "value"

    /** Waktu entri dibuat — favorit dan bookmark. */
    const val DIBUAT = "createdAt"

    /** Waktu progres terakhir diperbarui. */
    const val DIPERBARUI = "updatedAt"

    /** Waktu riwayat dibuka. */
    const val DIBUKA = "openedAt"

    /** Nomor baris riwayat; dibutuhkan agar urutan dan waktu aslinya ikut pulih. */
    const val ID = "id"
}
