package com.cezar.calisthenica.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Satu program latihan.
 *
 * Class ini sekarang punya DUA pekerjaan sekaligus:
 *   1. Jadi bentuk data yang dibaca UI (seperti sebelumnya).
 *   2. Jadi cetak biru satu BARIS di tabel database, gara-gara @Entity.
 *
 * Sengaja saya gabung. Di app besar, model UI dan model database biasanya
 * dipisah supaya perubahan skema database tidak merembet ke layar. Tapi di
 * tahap ini isinya identik, jadi memisahkannya cuma bikin dua file yang harus
 * kamu ubah dua kali setiap ada kolom baru. Kita pisah nanti, kalau memang
 * sudah beda -- misalnya waktu kartu perlu menampilkan "streak minggu ini"
 * yang tidak ada di tabel mana pun.
 */
@Entity(tableName = "programs")
data class WorkoutProgram(
    // autoGenerate = true artinya: JANGAN kasih id, biar database yang
    // menomori sendiri (1, 2, 3, ...). Nilai default 0 adalah kode rahasia
    // untuk bilang "ini baris baru, nomornya belum ada".
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,

    /*
     * ==================== DUA KOLOM MATI ====================
     * Status per 7 September 2026: TIDAK ADA SATU PUN kode yang membaca dua
     * kolom ini lagi. Jangan pakai. Jangan isi.
     *
     * Janji yang tertulis di komentar lama ("nanti dihitung sendiri dari daftar
     * gerakan") sudah ditepati, tapi bukan di sini -- jawabannya ada di
     * `ProgramExerciseDao.observeRingkasan()`, yang menghitungnya langsung dari
     * tabel `program_exercises` setiap kali datanya berubah.
     *
     * PELAJARAN HARI INI: kenapa kolom mati tidak langsung saya buang.
     *
     * Karena SQLite di HP-mu tidak bisa. `ALTER TABLE ... DROP COLUMN` baru ada
     * di SQLite 3.35, dan Android baru membawanya sekitar API 34. `minSdk` app
     * ini 26 -- di HP Android 8 kolomnya tetap tidak bisa dibuang.
     *
     * Jalan yang benar-benar tersedia: bikin tabel baru tanpa dua kolom ini,
     * `INSERT INTO ... SELECT` seluruh isi tabel lama ke tabel baru, `DROP` yang
     * lama, lalu `RENAME`. Empat perintah yang MEMINDAHKAN SETIAP BARIS DATA
     * ASLIMU. Kelas migration yang paling berisiko yang ada, dan bayarannya cuma
     * dua kolom berisi 0 yang tidak mengganggu siapa pun.
     *
     * Jadi keputusannya: biarkan. Nol di dua kolom itu tidak menghabiskan apa-apa
     * selain ruang yang tidak terasa. Yang berbahaya bukan kolom matinya, tapi
     * kolom mati yang TIDAK DITANDAI -- suatu hari kamu membacanya lagi, dapat
     * angka 0, dan menghabiskan satu malam mencari kenapa. Komentar ini vaksinnya.
     *
     * Kalau suatu hari tabel `programs` HARUS dibongkar untuk alasan lain
     * (misalnya menambah kolom `terakhirDikerjakan`), sekalian saja tinggalkan
     * dua kolom ini di tabel barunya. Satu perjalanan data, dua pekerjaan.
     * ========================================================
     */
    val exerciseCount: Int = 0,
    val estimatedMinutes: Int = 0,
)
