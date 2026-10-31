package id.or.kemenag.yasintahlil.core.util

/**
 * Kalimat saat **perbaikan lokasi GPS tidak kunjung ada**, dipakai layar Jadwal dan layar Kiblat.
 *
 * Sebab kegagalannya sama di kedua layar, dan dahulu kalimatnya disalin:
 * `JadwalViewModel.PESAN_TANPA_FIX` dan `KiblatViewModel.PESAN_TANPA_FIX` masing-masing menuliskan
 * *"Belum ada perbaikan lokasi GPS. Pastikan GPS aktif dan berada di luar ruangan, lalu coba lagi"*
 * sendiri-sendiri. Dua salinan kalimat yang menjelaskan **sebab** yang sama akan berbeda begitu
 * salah satunya diperbaiki — dan pengguna yang berpindah layar membaca dua penjelasan berbeda untuk
 * satu kegagalan yang sama.
 *
 * Yang memang berbeda hanyalah **saran**nya, sebab kedua layar punya jalan keluar yang berbeda:
 * Jadwal punya pemilih kotanya sendiri, sedangkan Kiblat tidak — kotanya dipilih di menu Jadwal.
 * Karena itu sebabnya satu, sedangkan saran tetap milik masing-masing layar.
 *
 * Murni tanpa Android supaya dapat diuji langsung di JVM.
 */
object PesanLokasi {

    /** Sebab kegagalannya — sama di kedua layar. */
    const val TANPA_FIX_SEBAB =
        "Belum ada perbaikan lokasi GPS. Pastikan GPS aktif dan berada di luar ruangan, " +
            "lalu coba lagi"

    /** Saran untuk layar yang punya pemilih kota sendiri (Jadwal). */
    const val TANPA_FIX_JADWAL = "$TANPA_FIX_SEBAB — atau pilih kota secara manual."

    /** Saran untuk layar tanpa pemilih kota (Kiblat): kotanya dipilih di menu Jadwal. */
    const val TANPA_FIX_KIBLAT = "$TANPA_FIX_SEBAB — atau matikan GPS dan pilih kota di menu Jadwal."
}
