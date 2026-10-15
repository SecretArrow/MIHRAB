package id.or.kemenag.yasintahlil.feature.asmaul

/**
 * Sembilan puluh sembilan nama Allah (Asmaul Husna) sebagai data statis murni — pola yang sama
 * dengan [id.or.kemenag.yasintahlil.core.sholat.BacaanSholat]: leksikon referensi yang dibaca
 * langsung tanpa menumpu berkas aset, sehingga tidak ikut rantai validasi bundel konten.
 *
 * **Penomoran.** Daftar dihitung mulai "Allah" sebagai nama pertama sehingga totalnya tepat
 * sembilan puluh sembilan — mengikuti enumerasi yang masyhur diriwayatkan para ulama (Al-Qushairi
 * menyusun risalahnya, sebagian nama diriwayatkan Tirmidzi); "Ar-Rahman" dengan demikian menjadi
 * nama kedua dan "Ash-Shabur" yang kesembilan puluh sembilan menutup daftar. Variasi urutan dan
 * ejaan latin memang ada antar sumber; aplikasi memakai bentuk yang paling umum dipakai di
 * Indonesia. Tidak ada satu pun tulisan ini mengaku penetapan resmi — itu milik kitab para ulama.
 *
 * Tulisan Arabnya sengaja berharakat penuh supaya dapat dibaca langsung oleh pengguna yang baru
 * belajar membaca Al-Qur'an; dijaga oleh `AsmaulDataTest` yang memeriksa jumlah, urutan, dan
 * kehadiran harakat pada setiap nama.
 */
object AsmaulData {

    /** Satu nama: nomor urutnya, tulisan Arab berharakat, alihaksara latin, dan artinya. */
    data class Nama(
        val no: Int,
        val arab: String,
        val latin: String,
        val arti: String,
    )

    val DAFTAR: List<Nama> = listOf(
        Nama(1, "اللَّهُ", "Allah", "Dzat Yang Maha Suci; nama yang menghimpun seluruh nama-Nya yang indah"),
        Nama(2, "الرَّحْمَنُ", "Ar-Rahman", "Yang Maha Pengasih"),
        Nama(3, "الرَّحِيمُ", "Ar-Rahim", "Yang Maha Penyayang"),
        Nama(4, "الْمَلِكُ", "Al-Malik", "Yang Maha Merajai"),
        Nama(5, "الْقُدُّوسُ", "Al-Quddus", "Yang Maha Suci"),
        Nama(6, "السَّلَامُ", "As-Salam", "Yang Maha Memberi Kesejahteraan"),
        Nama(7, "الْمُؤْمِنُ", "Al-Mu'min", "Yang Maha Memberi Keamanan"),
        Nama(8, "الْمُهَيْمِنُ", "Al-Muhaimin", "Yang Maha Memelihara"),
        Nama(9, "الْعَزِيزُ", "Al-'Aziz", "Yang Maha Perkasa"),
        Nama(10, "الْجَبَّارُ", "Al-Jabbar", "Yang Maha Gagah"),
        Nama(11, "الْمُتَكَبِّرُ", "Al-Mutakabbir", "Yang Maha Megah"),
        Nama(12, "الْخَالِقُ", "Al-Khaliq", "Yang Maha Pencipta"),
        Nama(13, "الْبَارِئُ", "Al-Bari'", "Yang Maha Melepaskan (mewujudkan makhluk)"),
        Nama(14, "الْمُصَوِّرُ", "Al-Mushawwir", "Yang Maha Membentuk Rupa"),
        Nama(15, "الْغَفَّارُ", "Al-Ghaffar", "Yang Maha Pengampun"),
        Nama(16, "الْقَهَّارُ", "Al-Qahhar", "Yang Maha Menundukkan"),
        Nama(17, "الْوَهَّابُ", "Al-Wahhab", "Yang Maha Pemberi Karunia"),
        Nama(18, "الرَّزَّاقُ", "Ar-Razzaq", "Yang Maha Pemberi Rezeki"),
        Nama(19, "الْفَتَّاحُ", "Al-Fattah", "Yang Maha Pembuka Rahmat"),
        Nama(20, "الْعَلِيمُ", "Al-'Alim", "Yang Maha Mengetahui"),
        Nama(21, "الْقَابِضُ", "Al-Qabidh", "Yang Maha Menyempitkan"),
        Nama(22, "الْبَاسِطُ", "Al-Basith", "Yang Maha Melapangkan"),
        Nama(23, "الْخَافِضُ", "Al-Khafidh", "Yang Maha Merendahkan"),
        Nama(24, "الرَّافِعُ", "Ar-Rafi'", "Yang Maha Meninggikan"),
        Nama(25, "الْمُعِزُّ", "Al-Mu'izz", "Yang Maha Memuliakan"),
        Nama(26, "الْمُذِلُّ", "Al-Mudzill", "Yang Maha Menghinakan"),
        Nama(27, "السَّمِيعُ", "As-Sami'", "Yang Maha Mendengar"),
        Nama(28, "الْبَصِيرُ", "Al-Bashir", "Yang Maha Melihat"),
        Nama(29, "الْحَكَمُ", "Al-Hakam", "Yang Maha Menetapkan Hukum"),
        Nama(30, "الْعَدْلُ", "Al-'Adl", "Yang Maha Adil"),
        Nama(31, "اللَّطِيفُ", "Al-Lathif", "Yang Maha Lembut"),
        Nama(32, "الْخَبِيرُ", "Al-Khabir", "Yang Maha Mengetahui Rahasia"),
        Nama(33, "الْحَلِيمُ", "Al-Halim", "Yang Maha Penyantun"),
        Nama(34, "الْعَظِيمُ", "Al-'Azhim", "Yang Maha Agung"),
        Nama(35, "الْغَفُورُ", "Al-Ghafur", "Yang Maha Memberi Pengampunan"),
        Nama(36, "الشَّكُورُ", "Asy-Syakur", "Yang Maha Pembalas Syukur"),
        Nama(37, "الْعَلِيُّ", "Al-'Aliy", "Yang Maha Tinggi"),
        Nama(38, "الْكَبِيرُ", "Al-Kabir", "Yang Maha Besar"),
        Nama(39, "الْحَفِيظُ", "Al-Hafizh", "Yang Maha Menjaga"),
        Nama(40, "الْمُقِيتُ", "Al-Muqit", "Yang Maha Pemberi Kecukupan"),
        Nama(41, "الْحَسِيبُ", "Al-Hasib", "Yang Maha Membuat Perhitungan"),
        Nama(42, "الْكَرِيمُ", "Al-Karim", "Yang Maha Pemurah"),
        Nama(43, "الرَّقِيبُ", "Ar-Raqib", "Yang Maha Mengawasi"),
        Nama(44, "الْمُجِيبُ", "Al-Mujib", "Yang Maha Mengabulkan"),
        Nama(45, "الْوَاسِعُ", "Al-Wasi'", "Yang Maha Luas"),
        Nama(46, "الْحَكِيمُ", "Al-Hakim", "Yang Maha Bijaksana"),
        Nama(47, "الْوَدُودُ", "Al-Wadud", "Yang Maha Mencintai"),
        Nama(48, "الْمَجِيدُ", "Al-Majid", "Yang Maha Mulia"),
        Nama(49, "الْبَاعِثُ", "Al-Ba'its", "Yang Maha Membangkitkan"),
        Nama(50, "الشَّهِيدُ", "Asy-Syahid", "Yang Maha Menyaksikan"),
        Nama(51, "الْحَقُّ", "Al-Haqq", "Yang Maha Benar"),
        Nama(52, "الْوَكِيلُ", "Al-Wakil", "Yang Maha Mengurus Hamba-Nya"),
        Nama(53, "الْقَوِيُّ", "Al-Qawiyy", "Yang Maha Kuat"),
        Nama(54, "الْمَتِينُ", "Al-Matin", "Yang Maha Kokoh"),
        Nama(55, "الْوَلِيُّ", "Al-Waliyy", "Yang Maha Melindungi"),
        Nama(56, "الْحَمِيدُ", "Al-Hamid", "Yang Maha Terpuji"),
        Nama(57, "الْمُحْصِي", "Al-Muhshi", "Yang Maha Menghitung"),
        Nama(58, "الْمُبْدِئُ", "Al-Mubdi'", "Yang Maha Memulai"),
        Nama(59, "الْمُعِيدُ", "Al-Mu'id", "Yang Maha Mengembalikan"),
        Nama(60, "الْمُحْيِي", "Al-Muhyi", "Yang Maha Menghidupkan"),
        Nama(61, "الْمُمِيتُ", "Al-Mumit", "Yang Maha Mematikan"),
        Nama(62, "الْحَيُّ", "Al-Hayy", "Yang Maha Hidup"),
        Nama(63, "الْقَيُّومُ", "Al-Qayyum", "Yang Maha Berdiri Sendiri"),
        Nama(64, "الْوَاجِدُ", "Al-Wajid", "Yang Maha Menemukan"),
        Nama(65, "الْمَاجِدُ", "Al-Majid", "Yang Maha Agung Kemuliaannya"),
        Nama(66, "الْوَاحِدُ", "Al-Wahid", "Yang Maha Tunggal"),
        Nama(67, "الْأَحَدُ", "Al-Ahad", "Yang Maha Esa"),
        Nama(68, "الصَّمَدُ", "Ash-Shamad", "Yang Maha Dibutuhkan"),
        Nama(69, "الْقَادِرُ", "Al-Qadir", "Yang Maha Menentukan"),
        Nama(70, "الْمُقْتَدِرُ", "Al-Muqtadir", "Yang Maha Berkuasa"),
        Nama(71, "الْمُقَدِّمُ", "Al-Muqaddim", "Yang Maha Mendahulukan"),
        Nama(72, "الْمُؤَخِّرُ", "Al-Mu'akhkhir", "Yang Maha Mengakhirkan"),
        Nama(73, "الْأَوَّلُ", "Al-Awwal", "Yang Maha Awal"),
        Nama(74, "الْآخِرُ", "Al-Akhir", "Yang Maha Akhir"),
        Nama(75, "الظَّاهِرُ", "Azh-Zhahir", "Yang Maha Menyatakan"),
        Nama(76, "الْبَاطِنُ", "Al-Bathin", "Yang Maha Tersembunyi"),
        Nama(77, "الْوَالِي", "Al-Wali", "Yang Maha Mengatur"),
        Nama(78, "الْمُتَعَالِي", "Al-Muta'ali", "Yang Maha Luhur"),
        Nama(79, "الْبَرُّ", "Al-Barr", "Yang Maha Penderma"),
        Nama(80, "التَّوَّابُ", "At-Tawwab", "Yang Maha Penerima Taubat"),
        Nama(81, "الْمُنْتَقِمُ", "Al-Muntaqim", "Yang Maha Pemberi Balasan"),
        Nama(82, "الْعَفُوُّ", "Al-'Afuww", "Yang Maha Pemaaf"),
        Nama(83, "الرَّؤُوفُ", "Ar-Ra'uf", "Yang Maha Belas Kasih"),
        Nama(84, "مَالِكُ الْمُلْكِ", "Malikul Mulk", "Yang Maha Pemilik Kerajaan"),
        Nama(85, "ذُو الْجَلَالِ وَالْإِكْرَامِ", "Dzul Jalali wal Ikram", "Yang Memiliki Kebesaran dan Kemuliaan"),
        Nama(86, "الْمُقْسِطُ", "Al-Muqsith", "Yang Maha Pemberi Keadilan"),
        Nama(87, "الْجَامِعُ", "Al-Jami'", "Yang Maha Mengumpulkan"),
        Nama(88, "الْغَنِيُّ", "Al-Ghaniy", "Yang Maha Kaya"),
        Nama(89, "الْمُغْنِي", "Al-Mughni", "Yang Maha Memberi Kekayaan"),
        Nama(90, "الْمَانِعُ", "Al-Mani'", "Yang Maha Mencegah"),
        Nama(91, "الضَّارُّ", "Adh-Dharr", "Yang Maha Memberi Derita"),
        Nama(92, "النَّافِعُ", "An-Nafi'", "Yang Maha Memberi Manfaat"),
        Nama(93, "النُّورُ", "An-Nur", "Yang Maha Bercahaya"),
        Nama(94, "الْهَادِي", "Al-Hadi", "Yang Maha Pemberi Petunjuk"),
        Nama(95, "الْبَدِيعُ", "Al-Badi'", "Yang Maha Pencipta Tiada Banding"),
        Nama(96, "الْبَاقِي", "Al-Baqi", "Yang Maha Kekal"),
        Nama(97, "الْوَارِثُ", "Al-Warits", "Yang Maha Pewaris"),
        Nama(98, "الرَّشِيدُ", "Ar-Rasyid", "Yang Maha Membimbing"),
        Nama(99, "الصَّبُورُ", "Ash-Shabur", "Yang Maha Sabar"),
    )
}
