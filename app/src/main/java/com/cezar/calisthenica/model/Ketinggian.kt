package com.cezar.calisthenica.model

/**
 * Tangga ketinggian alat: seberapa tinggi ring/palangmu dipasang untuk SATU
 * gerakan tertentu.
 *
 * Kenapa ini perlu ada sama sekali: "Ring Row" dengan ring sepinggul dan "Ring
 * Row" dengan ring selantai itu dua tingkat kesulitan yang jauh berbeda, padahal
 * gerakannya sama dan alatnya sama. Yang berubah cuma satu angka. Jadi angka itu
 * layak dicatat, dan dicatat di GERAKAN -- bukan di alat, karena satu ring yang
 * sama kamu pakai di ketinggian berbeda untuk gerakan berbeda.
 *
 * ------------------------------------------------------------------
 * PELAJARAN HARI INI: kenapa ada `tingkat` padahal enum sudah punya `ordinal`.
 *
 * Setiap enum Kotlin otomatis punya `ordinal` -- nomor urut posisinya. Godaannya
 * besar: simpan `ordinal` ke database, hemat satu baris kode. JANGAN.
 *
 * `ordinal` itu NOMOR KURSI, bukan NOMOR IDENTITAS. Dia dihitung dari posisi
 * tulisan di file ini. Suatu hari kamu ingin menambahkan "Sebetis" di antara
 * LANTAI dan LUTUT -- keputusan yang wajar dan tidak berbahaya. Detik itu semua
 * yang di bawahnya bergeser satu: LUTUT dari 2 jadi 3, PINGGUL dari 3 jadi 4.
 * Dan setiap gerakan yang sudah kamu simpan berbulan-bulan mendadak berpindah
 * arti sendiri. Tidak ada error, tidak ada peringatan, cuma datamu yang salah.
 *
 * `tingkat` ditulis tangan, jadi angkanya menempel ke ARTI, bukan ke posisi.
 * Menambah nilai baru nanti = beri dia angka BARU (9, 10, ...), tulis di posisi
 * mana pun yang enak dibaca, dan data lama tetap benar. Urutan tampilan diurus
 * `urutTampil` di bawah, bukan oleh urutan di file.
 * ------------------------------------------------------------------
 *
 * Kenapa disimpan sebagai INTEGER, bukan teks seperti `ExerciseCategory`:
 *   1. Ini POSISI DI TANGGA, dan tangga itu memang berurutan. Angka bisa
 *      dibandingkan ("mana yang lebih tinggi"), teks tidak.
 *   2. `DEFAULT 0` di migrasi langsung berarti "belum diatur" tanpa perlu
 *      menyentuh satu baris pun data lamamu.
 *   3. Tidak butuh Converter baru. Nol pekerjaan tambahan di `Converters.kt`.
 *
 * Angka 0 sengaja BUKAN sebuah ketinggian. Dia keadaan "belum dijawab", sama
 * seperti list alat kosong berarti "badan sendiri". Satu keadaan, satu wujud.
 *
 * Label ditulis di Kotlin (bukan `R.string`) mengikuti gaya rumah yang sama
 * dengan `ExerciseCategory`, `ExerciseType`, dan `Muscle`: satu tempat, dan
 * menambah nilai tidak menyeret perubahan ke file XML.
 */
enum class Ketinggian(val tingkat: Int, val label: String) {
    BELUM(0, "Belum diatur"),
    LANTAI(1, "Selantai"),
    LUTUT(2, "Selutut"),
    PINGGUL(3, "Sepinggul"),
    PERUT(4, "Seperut"),
    DADA(5, "Sedada"),
    BAHU(6, "Sebahu"),
    KEPALA(7, "Sekepala"),
    OVERHEAD(8, "Di atas kepala"),
    ;

    companion object {

        /** Tingkat paling tinggi yang ada. Slider memakainya sebagai batas atas. */
        const val TERTINGGI: Int = 8

        /**
         * Terjemahkan angka dari database jadi tangga.
         *
         * Angka asing (misal file database dari versi app yang lebih baru, atau
         * nilai yang sudah kamu hapus dari enum ini) jatuh ke BELUM, bukan
         * bikin app tutup sendiri. Ini kebiasaan yang sama dengan alat yatim di
         * layar detail: yang tidak dikenali cukup TIDAK DITAMPILKAN, jangan
         * dipakai sebagai alasan untuk crash.
         */
        fun dari(tingkat: Int): Ketinggian = entries.firstOrNull { it.tingkat == tingkat } ?: BELUM

        /** Dari tinggi ke rendah -- urutan yang dipakai slider, karena atas = tinggi. */
        fun urutTampil(): List<Ketinggian> = entries.sortedByDescending { it.tingkat }
    }
}
