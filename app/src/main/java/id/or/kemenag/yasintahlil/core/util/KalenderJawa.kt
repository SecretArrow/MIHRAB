package id.or.kemenag.yasintahlil.core.util

import java.time.LocalDate

/**
 * Penanggalan Jawa — pasaran (siklus 5 hari), weton, neptu, nama bulan, dan tahun Aji Saka —
 * dihitung murni dari tanggal Masehi ([LocalDate]), tanpa Android agar dapat diuji langsung di
 * JVM (pola [InfoHarian] dan [NamaLayar]).
 *
 * **Pasaran & weton.** Pasaran berputar dalam siklus 5 hari (Legi–Pahing–Pon–Wage–Kliwon) dan
 * berpasangan dengan nama hari menjadi **weton** (mis. "Jumat Legi"); karena 7 × 5 = 35, satu
 * weton hanya berulang tiap 35 hari. Offset siklusnya diturunkan dari jangkar sejarah yang
 * masyhur: proklamasi kemerdekaan jatuh pada **Jumat Legi, 17 Agustus 1945** — [jdn] hari itu
 * (2431685) habis dibagi 5, maka `jdn(t) mod 5 == 0` berarti Legi tanpa offset tambahan. Jangkar
 * kedua, **Senin Pahing, 1 Januari 1900**, sejalan: jdn 2415021 bersisa 1 → Pahing. Keduanya
 * terpisah 16664 hari (2380 minggu bersisa 4 hari), sehingga nama hari dan pasaran yang
 * diturunkan dari dua jangkar itu tidak mungkin saling meniadakan — keduanya dijaga di
 * `KalenderJawaTest`.
 *
 * **Neptu** adalah angka weton menurut primbon Jawa: Legi 5, Pahing 9, Pon 7, Wage 4, Kliwon 8;
 * Minggu 5, Senin 4, Selasa 3, Rabu 7, Kamis 8, Jumat 6, Sabtu 9. Ia khazanah budaya yang
 * dipakai luas dalam tradisi Jawa (memilih hari, memilih wetan lahir, dan sebagainya) — **bukan**
 * bagian syariat Islam; aplikasi menampilkannya sebagai pengetahuan budaya, bukan anjuran ibadah.
 *
 * **Bulan & tahun Jawa.** Sejak Sultan Agung (abad ke-17), nama bulan Jawa berjalan selaras
 * urutan bulan Hijriah: Sura = Muharram, …, Besar = Zulhijah ([bulanJawa]). Penomoran tahunnya
 * memakai konvensi `Hijriah − 78` ([tahunJawa]) — batas akurasinya dijelaskan di masing-masing
 * fungsi; sebagaimana [HijriCalc], seluruh hasil yang menumpu Hijriah tabular tetap perkiraan
 * yang bisa berselisih dengan isbat Kemenag.
 */
object KalenderJawa {

    /** Urutan pasaran dalam satu siklus 5 hari — tetap, tidak bergantung tanggal mana pun. */
    val PASARAN = listOf("Legi", "Pahing", "Pon", "Wage", "Kliwon")

    /** Nama hari Minggu..Sabtu — diindeks `dayOfWeek.value % 7` (Senin=1 … Minggu=7 → 0). */
    val HARI = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")

    /** Neptu pasaran, sejajar [PASARAN]: Legi 5, Pahing 9, Pon 7, Wage 4, Kliwon 8. */
    private val NEPTU_PASARAN = listOf(5, 9, 7, 4, 8)

    /** Neptu hari, sejajar [HARI] (Minggu..Sabtu): 5, 4, 3, 7, 8, 6, 9. */
    private val NEPTU_HARI = listOf(5, 4, 3, 7, 8, 6, 9)

    /** Nama bulan Masehi Indonesia, Januari..Desember. */
    val BULAN_MASEHI = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember",
    )

    /** Nama bulan Jawa, sejajar urutan bulan Hijriah 1..12: Sura..Besar. */
    val BULAN_JAWA = listOf(
        "Sura", "Sapar", "Mulud", "Ba'da Mulud", "Jumadilawal", "Jumadilakhir",
        "Rejeb", "Ruwah", "Pasa", "Sawal", "Dulkaidah", "Besar",
    )

    /**
     * Julian Day Number (JDN) tanggal Masehi — rumus integer klasik dengan **pembagian lantai**
     * ([Math.floorDiv]), seluruhnya bulat sehingga tidak menumpuk galat pecahan.
     *
     * Bulan digeser dahulu (`a = (14 − bulan) / 12`: Januari/Februari dihitung milik tahun
     * sebelumnya) sehingga **semua operand pembagian tak-negatif** untuk tahun masehi; karena itu
     * pembagian lantai dan terpotong berhimpit, dan hasilnya identik dengan [HijriCalc] yang
     * memakai ragam Fliegel–Van Flandern. Justru di situlah jebakannya: menukar `/` dengan
     * `floorDiv` pada bentuk asli `(m − 14) / 12` **salah** — rumus itu lahir untuk pembagian
     * Fortran yang terpotong ke nol, dan untuk Januari `(1 − 14) / 12` harus `−1`, bukan `−2`
     * hasil lantai; bentuk lantai menggeser semua tanggal Januari dua hari dan langsung tertangkap
     * jangkar uji di bawah.
     *
     * Jangkar yang dipaku uji (semua tanggal nyata, bukan angka karangan): 1970-01-01 → 2440588
     * (jangkar epoch Unix), 2000-01-01 → 2451545 (J2000.0), 1900-01-01 → 2415021, dan
     * 17-08-1945 → **2431685**. Nilai 17-08-1945 itu diverifikasi tiga jalur: (1) mundur 19860
     * hari dari 2451545; (2) mundur 8903 hari dari 2440588; (3) jangkar weton Jumat Legi —
     * 2431685 mod 5 = 0 (Legi) dan mod 7 = 4 (Jumat). Angka 2431771 yang sempat beredar di draf
     * spesifikasi tidak lolos ketiga jalur itu dan karenanya tidak dipakai.
     */
    fun jdn(t: LocalDate): Long {
        val a = Math.floorDiv(14L - t.monthValue, 12L)      // 1 untuk Jan/Feb, 0 selainnya
        val y = t.year.toLong() + 4800L - a                  // selalu positif untuk tahun masehi
        val m = t.monthValue.toLong() + 12L * a - 3L         // Maret = 0 … Januari = 10, Februari = 11
        return t.dayOfMonth.toLong() + Math.floorDiv(153L * m + 2L, 5L) +
            365L * y + Math.floorDiv(y, 4L) - Math.floorDiv(y, 100L) +
            Math.floorDiv(y, 400L) - 32045L
    }

    /**
     * Pasaran hari itu: `jdn(t) mod 5` membaca urutan [PASARAN] mulai Legi — offset 0 inilah yang
     * diturunkan dari jangkar 17 Agustus 1945 = Legi dan diukuhkan jangkar 1 Januari 1900 =
     * Pahing (lihat KDoc objek ini). [Math.floorMod] menjaga hasil tetap benar walau JDN suatu
     * saat negatif (tanggal pra-Masehi).
     */
    fun pasaran(t: LocalDate): String = PASARAN[Math.floorMod(jdn(t), 5L).toInt()]

    /** Nama hari Indonesia (Minggu..Sabtu) — konversi `dayOfWeek` milik java.time. */
    fun hari(t: LocalDate): String = HARI[t.dayOfWeek.value % 7]

    /** Weton hari itu: nama hari + pasaran, mis. "Jumat Legi" (weton proklamasi 17-8-1945). */
    fun weton(t: LocalDate): String = "${hari(t)} ${pasaran(t)}"

    /**
     * Neptu weton hari itu: neptu hari ditambah neptu pasaran menurut primbon Jawa — mis. Jumat
     * Legi = 6 + 5 = 11, Senin Pahing = 4 + 9 = 13. Pengetahuan budaya, bukan syariat (lihat
     * KDoc objek ini).
     */
    fun neptu(t: LocalDate): Int =
        NEPTU_HARI[t.dayOfWeek.value % 7] + NEPTU_PASARAN[Math.floorMod(jdn(t), 5L).toInt()]

    /**
     * Nama bulan Jawa dari urutan bulan Hijriah 1..12 (Sura..Besar) — selaras sejak Sultan Agung
     * menjadikan bulan Jawa mengikuti bulan Hijriah. Nomor di luar 1..12 dipatok ke batas
     * terdekat, bukan dilempar: angka itu datang dari [HijriCalc] yang tabular, dan melempar
     * galat ke pengguna lebih buruk daripada menampilkan batas yang terdekat.
     */
    fun bulanJawa(hijriBulanKe: Int): String = BULAN_JAWA[(hijriBulanKe - 1).coerceIn(0, 11)]

    /**
     * Tahun Aji Saka dari tahun Hijriah — konvensi `Hijriah − 78` yang dipakai layar Kalender
     * (contoh: 1448 H → 1370).
     *
     * **Batas akurasi.** Era Aji Saka berakar tahun 78 Masehi, dan penanggalan Jawa yang berjalan
     * menomori tahunnya dengan angka era itu (tahun 2020-an: 19xx) memakai sistem windu–kurup
     * tersendiri — tahun 1448 H menurut kalender kraton karenanya **bukan** 1370. Angka fungsi
     * ini adalah konvensi penanda sederhana, bukan taraf kalender resmi mana pun; yang kokoh
     * diuji jangkarnya adalah [pasaran], [weton], dan [neptu] yang berdiri sendiri di atas hari
     * Masehi.
     */
    fun tahunJawa(hijriTahun: Int): Int = hijriTahun - 78

    /** Tanggal Masehi gaya Indonesia, mis. "17 Agustus 1945". */
    fun tanggalIndo(t: LocalDate): String =
        "${t.dayOfMonth} ${BULAN_MASEHI[t.monthValue - 1]} ${t.year}"
}
