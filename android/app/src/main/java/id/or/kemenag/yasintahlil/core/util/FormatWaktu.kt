package id.or.kemenag.yasintahlil.core.util

/**
 * Ejaan angka waktu untuk seluruh aplikasi.
 *
 * Dua ejaan yang berbeda pernah ditulis di tiga tempat: `compute` di [PrayerCalc] menyusun waktu
 * sholat, [JadwalHariIni.geser] menggesernya, dan layar Iqomah menghitung mundur — ketiganya
 * menulis `"%02d:%02d"` sendiri-sendiri. Yang pertama dan kedua menghasilkan **jam** yang sama,
 * jadi salinan yang berbeda berarti jadwal yang dihitung dan jadwal yang digeser menyebut jam yang
 * sama dengan tulisan berbeda; yang ketiga menghasilkan **durasi**, sehingga menyalin ejaannya
 * begitu saja menyembunyikan bahwa angkanya berarti lain.
 *
 * Karena itu ejaannya satu tempat di sini, sedangkan maknanya diberi nama masing-masing:
 * [jamMenit] untuk jam, [menitDetik] untuk durasi.
 */
object FormatWaktu {

    /**
     * Ejaan satu-satunya: dua angka, masing-masing dua digit, dipisah titik dua.
     *
     * Dipakai kedua nama di bawah sehingga tak ada lagi format string waktu yang ditulis ulang.
     */
    fun pasanganAngka(a: Int, b: Int): String = "%02d:%02d".format(a, b)

    /**
     * Jam:menit dari menit ke hari — kebalikan [PrayerCalc.toMinutes].
     *
     * Lebih dari 1440 menit tidak dilipat di sini: pemanggil yang menentukan, karena
     * [JadwalHariIni.geser] memang melipatnya sendiri.
     */
    fun jamMenit(totalMenit: Int): String = pasanganAngka(totalMenit / 60, totalMenit % 60)

    /**
     * Menit:detik dari detik — **durasi**, bukan jam.
     *
     * Namanya berbeda dengan sengaja: `90` berarti satu menit tiga puluh detik ("01:30"), bukan
     * pukul satu lewat tiga puluh menit. Angka yang sama, arti yang lain.
     */
    fun menitDetik(totalDetik: Int): String = pasanganAngka(totalDetik / 60, totalDetik % 60)

    /**
     * Durasi jam–menit dalam **kata**: `2 jam 15 mnt`, `45 mnt`, `2 jam`.
     *
     * Dipakai hitung mundur "Berikutnya: …" — durasi yang dibaca dalam kalimat, bukan angka
     * bersanding. Sebelumnya dua layar mengejanya sendiri-sendiri dan hasilnya berbeda: beranda
     * menulis `2j 15m lagi`, layar Jadwal `dalam 2 jam 15 mnt` — dua tulisan untuk satu jeda yang
     * sama. Keduanya juga selalu menyebut jamnya, sehingga jeda sepuluh menit terbaca
     * `0 jam 10 mnt`. Satuannya kini dipilih menurut nilainya: jamnya hilang bila nol, dan
     * menitnya hilang bila pas — seperti orang mengucapkannya.
     */
    fun durasiJamMenit(totalMenit: Int): String {
        if (totalMenit < 1) return "kurang dari 1 mnt"
        val jam = totalMenit / 60
        val sisa = totalMenit % 60
        return when {
            jam == 0 -> "$sisa mnt"
            sisa == 0 -> "$jam jam"
            else -> "$jam jam $sisa mnt"
        }
    }
}
