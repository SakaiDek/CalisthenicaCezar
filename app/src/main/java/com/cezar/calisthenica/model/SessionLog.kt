package com.cezar.calisthenica.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Satu baris riwayat: bukti bahwa sebuah sesi latihan BENAR-BENAR selesai.
 *
 * Tabelnya `session_logs`, lahir di DB versi 7. Ini tabel keempat di app
 * (`programs`, `exercises`, `equipment`, `program_exercises`, lalu ini), dan
 * jenis tabel yang paling aman ditambah -- lihat komentar MIGRATION_6_7 di
 * `AppDatabase.kt`. Sampai baris ini ada, Session Runner ibarat ikan mas koki:
 * cantik menjalankan sesi, lalu lupa semuanya begitu kamu tutup. Mulai sekarang
 * dia ingat.
 *
 * ===========================================================================
 * PELAJARAN HARI INI, dan ini yang paling gampang salah kalau tidak diniatkan:
 * SATU SESI PUNYA DUA JAM, dan tiap kolom waktu di sini WAJIB tahu dia jam yang
 * mana.
 * ===========================================================================
 *
 *   `durasiDetik`  = LAMANYA sesi. Diukur di Runner pakai `elapsedRealtime()`
 *                    (jam monoton -- cuma bisa maju, tidak peduli tanggal, dan
 *                    di-reset saat HP reboot). Sempurna untuk MENGUKUR selisih,
 *                    tapi angkanya tidak punya arti kalender sama sekali. Makanya
 *                    yang disimpan di sini cuma HASIL kurangnya (berapa detik),
 *                    bukan tanda waktunya.
 *
 *   `waktuSelesaiMillis` = KAPAN sesi berakhir, jam dinding (`currentTimeMillis`).
 *                    Ini yang dipakai untuk mengurutkan riwayat "terbaru di atas".
 *                    Jam dinding bisa saja melompat (user ganti jam, zona waktu),
 *                    tapi untuk "kapan" itu satu-satunya jam yang punya makna.
 *
 *   `tanggal`      = tanggal LOKAL yang kamu ALAMI, teks ISO "2026-09-26".
 *                    Kenapa disimpan terpisah padahal sudah ada millis di atas:
 *                    streak & kalender (Ronde 2) menghitung "berapa hari beruntun",
 *                    dan itu harus pakai tanggal versi HP-mu, bukan tanggal UTC.
 *                    Sesi jam 1 pagi WIB itu masih "hari ini" bagimu, tapi kalau
 *                    kalender menghitung dari UTC dia bisa jatuh ke tanggal
 *                    kemarin. Menyimpan tanggalnya sebagai teks, dihitung sekali
 *                    di HP saat sesi selesai, bikin streak-nya stabil selamanya --
 *                    tidak akan berubah walau nanti dibaca di zona waktu lain.
 *
 * ===========================================================================
 * KENAPA `programNama` DISALIN KE SINI, bukan disambung ke tabel `programs`.
 * ===========================================================================
 * Ini namanya DENORMALISASI, dan di sini dia keputusan yang BENAR. Riwayat itu
 * catatan sejarah: apa yang terjadi, terjadi. Kalau besok kamu ganti nama
 * "Pull Day A" jadi "Pull Day (revisi)", atau malah HAPUS programnya, baris
 * riwayat ini harus tetap terbaca apa adanya -- "kamu menyelesaikan Pull Day A
 * pada 26 September". Menyambung ke tabel `programs` saat menampilkan berarti
 * riwayat ikut berubah/hilang setiap kali programnya disentuh. Sejarah tidak
 * boleh bisa disunting dari masa depan. Jadi kita FOTO namanya saat itu juga.
 *
 * Konsekuensinya yang sengaja kita terima: TIDAK ADA `@ForeignKey` ke `programs`.
 * Kalau ada, menghapus program akan ikut menyeret riwayatnya (cascade) atau malah
 * menolak penghapusan -- dua-duanya salah untuk catatan sejarah. `programId`
 * tetap disimpan (siapa tahu Ronde 2 mau "buka lagi program ini"), tapi dia cuma
 * angka biasa, bukan tali yang mengikat. Pola yang sama persis dengan keputusan
 * tanpa-FK di `ProgramExerciseRef.kt`.
 *
 * `catatan` sengaja dimasukkan SEKARANG walau UI penulisnya belum ada. Kolomnya
 * gratis dibuat saat migrasi v7 ini; menambahkannya nanti berarti migrasi v8
 * lagi cuma demi satu kolom teks. `String?` (boleh null) karena sesi tanpa
 * catatan itu keadaan yang paling normal, bukan kekurangan.
 */
@Entity(tableName = "session_logs")
data class SessionLog(
    // autoGenerate = true: id diisi database (1, 2, 3, ...). 0 = "baris baru,
    // belum bernomor". AUTOINCREMENT di migrationnya menjamin nomor cuma maju.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Angka biasa, BUKAN foreign key. Lihat komentar di atas.
    val programId: Long,

    // Foto nama program SAAT sesi selesai. Tidak ikut berubah kalau program
    // di-rename/hapus. Inilah yang ditampilkan di daftar riwayat.
    val programNama: String,

    // Jam dinding (currentTimeMillis) saat menekan garis finish. Kunci urut DESC.
    val waktuSelesaiMillis: Long,

    // Tanggal lokal yang dialami, teks ISO "yyyy-MM-dd". Fondasi streak/kalender.
    val tanggal: String,

    // Hasil kurang dua tanda waktu elapsedRealtime, sudah jadi detik. Ditampilkan
    // lewat labelDurasi() yang sama dengan kartu dasbor -- satu cara menulis durasi.
    val durasiDetik: Int,

    // Jumlah set yang benar-benar tuntas (dari `setSelesai` di Runner).
    val totalSetSelesai: Int,

    // Jumlah gerakan di program saat dijalankan (rakitan.size).
    val totalGerakan: Int,

    // Catatan bebas. Null = tidak ada. UI penulisnya menyusul; kolomnya disiapkan
    // dari sekarang supaya tidak perlu migrasi v8 cuma untuk ini.
    val catatan: String? = null,
)
