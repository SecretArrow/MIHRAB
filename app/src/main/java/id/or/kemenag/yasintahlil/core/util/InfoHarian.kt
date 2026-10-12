package id.or.kemenag.yasintahlil.core.util

import id.or.kemenag.yasintahlil.core.data.DzikirGroup
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Isi kartu **"Info Islami harian"** di beranda.
 *
 * `plan.txt` §"Beranda Utama" meminta *"kartu informasi Islami harian yang dapat dinonaktifkan"*.
 * Kartu itu dahulu memuat satu kalimat **tetap** — *"Perbanyak sholawat di hari Jumat (HR Abu
 * Dawud)"* — yang ditampilkan setiap hari. Judulnya "harian", isinya hari Jumat: pada hari Senin
 * pengguna membaca anjuran yang menyebut hari Jumat, dan sejak hari kedua kartu itu berhenti memberi
 * apa pun yang baru.
 *
 * Isinya kini berganti mengikuti tanggal, dan **tidak satu pun teks baru dikarang**: butir harian
 * diambil dari pustaka dzikir yang sudah terverifikasi di bundel konten — lafaz berharakat,
 * terjemahan, dan sumbernya ikut ditampilkan — sedangkan hari Jumat memakai anjuran sholawat yang
 * memang khusus hari itu.
 *
 * Murni tanpa Android supaya pemilihan hariannya dapat diuji sebagai unit test biasa.
 */
object InfoHarian {

    /** Anjuran yang memang khusus hari Jumat — dipakai **hanya** pada hari Jumat. */
    const val PESAN_JUMAT = "Perbanyak sholawat di hari Jumat."
    const val SUMBER_JUMAT = "HR Abu Dawud — sunnah.com"

    /** Satu butir ringkas yang ditampilkan kartu beranda. */
    data class Butir(
        val judul: String,
        /** Lafaz berharakat, bila butir ini memuatnya (butir khusus hari Jumat tidak). */
        val arab: String = "",
        /** Teks yang dibaca pengguna: terjemahan lafaz, atau pesan ringkas butir khusus. */
        val teks: String = "",
        val sumber: String = "",
    )

    /** Tanggal menurut zona waktu tampilan — hari yang dipakai memilih butir. */
    fun tanggal(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    /**
     * Zona [id] bila sah, zona perangkat bila id-nya kosong atau tak dikenal.
     *
     * Aturannya bukan milik berkas ini melainkan [JadwalHariIni.zonaId]: jam beranda, penjadwal
     * pengingat, dan layar Jadwal memakai zona yang sama, hanya dalam bentuk `TimeZone`. Kalau
     * keduanya berbeda, "hari ini" pada butir harian tidak sejalan dengan tanggal yang tertera di
     * atasnya — dan butir yang tidak cocok dengan tanggalnya membingungkan.
     */
    fun zona(id: String): ZoneId = JadwalHariIni.zonaId(id)

    fun jumat(tanggal: LocalDate): Boolean = tanggal.dayOfWeek == DayOfWeek.FRIDAY

    /**
     * Butir pustaka dzikir yang layak tampil sebagai info harian: yang memuat **lafaz berharakat
     * dan terjemahannya**.
     *
     * Entri penunjuk (`rujukan_file`) tidak memuat keduanya di sini, dan entri tanpa terjemahan tidak
     * memberi apa pun kepada pengguna yang tidak membaca Arab — keduanya dibuang, bukan ditampilkan
     * sebagai kartu kosong. Urutannya tetap seperti di bundel, sehingga butir yang tampil tidak
     * berubah-ubah di antara pembukaan beranda pada hari yang sama.
     */
    fun dariDzikir(kelompok: List<DzikirGroup>): List<Butir> =
        kelompok.flatMap { it.items }
            .filter { it.arab.isNotBlank() && it.terjemahan.isNotBlank() }
            .map { Butir(it.judul, it.arab, it.terjemahan, it.sumber) }

    /**
     * Butir untuk [tanggal]: anjuran sholawat pada hari Jumat, selain itu satu butir [kandidat] yang
     * bergilir menurut hari ke berapa dalam setahun.
     *
     * `null` hanya bila hari itu bukan Jumat **dan** tidak ada kandidat sama sekali (bundel konten
     * yang tidak terbaca) — kartu tanpa butir lebih baik daripada kartu yang menampilkan bacaan milik
     * hari lain.
     */
    fun pilih(tanggal: LocalDate, kandidat: List<Butir>): Butir? {
        if (jumat(tanggal)) {
            return Butir("Anjuran hari Jumat", teks = PESAN_JUMAT, sumber = SUMBER_JUMAT)
        }
        if (kandidat.isEmpty()) return null
        return kandidat[Math.floorMod(tanggal.dayOfYear, kandidat.size)]
    }
}
