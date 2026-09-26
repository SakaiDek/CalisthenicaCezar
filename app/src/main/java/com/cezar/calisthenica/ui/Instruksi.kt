package com.cezar.calisthenica.ui

/*
 * Format "Cara melakukan": langkah bernomor ATAU paragraf.
 *
 * Yang tersimpan di database tetap SATU kolom String (`Exercise.instruction`),
 * jadi tidak ada tabel baru dan tidak ada migration. Formatnya tidak disimpan
 * sebagai kolom sendiri, tapi DIBACA dari isi teksnya: kalau setiap baris
 * diawali "1." / "2)" maka itu daftar langkah, kalau tidak maka itu paragraf.
 *
 * Tiga fungsi di bawah dipakai bareng oleh form (untuk mengedit) dan layar
 * detail (untuk menampilkan), supaya keduanya tidak pernah beda pendapat soal
 * teks yang sama.
 */

/**
 * Awalan nomor di depan satu baris. Spasi setelah titik hanya dimakan SATU
 * (`" ?"`), bukan `\s*`, supaya `gabungLangkah` lalu `pecahLangkah` selalu
 * mengembalikan teks yang sama persis seperti yang diketik user. Kalau spasinya
 * dimakan rakus, kursor di kolom ketikan bisa melompat saat user menekan spasi.
 */
private val AWALAN_NOMOR = Regex("^\\s*\\d+[.)] ?")

/**
 * True kalau teks ini berbentuk daftar bernomor: ada isinya, DAN setiap baris
 * yang tidak kosong diawali nomor. Satu baris polos di antaranya sudah cukup
 * untuk membuatnya dianggap paragraf, dan itu memang yang diinginkan.
 */
fun instruksiBernomor(teks: String): Boolean {
    val baris = teks.split("\n").filter { it.isNotBlank() }
    return baris.isNotEmpty() && baris.all { AWALAN_NOMOR.containsMatchIn(it) }
}

/**
 * Teks jadi daftar langkah, nomornya dibuang. Baris kosong TIDAK dibuang di
 * sini: saat mengedit, langkah yang baru ditambah memang masih kosong dan dia
 * harus tetap punya kolomnya sendiri.
 */
fun pecahLangkah(teks: String): List<String> =
    if (teks.isEmpty()) emptyList() else teks.split("\n").map { AWALAN_NOMOR.replaceFirst(it, "") }

/** Daftar langkah jadi satu teks bernomor, siap disimpan ke kolom instruction. */
fun gabungLangkah(langkah: List<String>): String =
    langkah.mapIndexed { i, teks -> "${i + 1}. $teks" }.joinToString("\n")
