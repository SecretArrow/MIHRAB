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
}
