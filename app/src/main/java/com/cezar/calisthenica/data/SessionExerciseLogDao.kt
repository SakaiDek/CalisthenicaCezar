package com.cezar.calisthenica.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cezar.calisthenica.model.SessionExerciseLog
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk tabel anak `session_exercise_logs` -- halaman-halaman isi dari
 * buku catatan riwayat. Kembar peran dengan `SessionLogDao`: cuma MENULIS saat
 * sesi selesai dan MEMBACA saat kartu riwayat dibuka. Tidak ada `@Update`,
 * tidak ada `@Delete` -- sejarah tidak disunting.
 *
 * PELAJARAN HARI INI: kenapa `insertAll` menerima SATU LIST, bukan dipanggil
 * berkali-kali di dalam loop.
 *
 * Satu sesi punya banyak gerakan sekaligus. Kalau kita `insert` satu per satu
 * di dalam `for`, tiap panggilan itu satu perjalanan bolak-balik ke disk --
 * lima gerakan = lima perjalanan. `@Insert` yang menerima `List` menulis
 * semuanya dalam SATU transaksi: satu perjalanan, semua baris masuk atau tidak
 * sama sekali. Lebih cepat, dan lebih aman dari "separuh tersimpan".
 */
@Dao
interface SessionExerciseLogDao {

    /**
     * Menulis SEMUA gerakan satu sesi sekaligus. `suspend` karena menyentuh
     * disk. Dipanggil sekali dari Runner saat menyentuh SELESAI, di dalam satu
     * transaksi bersama penulisan induk `SessionLog` (lihat `withTransaction`
     * di SessionRunnerScreen) -- jadi induk dan anak lahir bersama atau tidak
     * sama sekali; tak akan ada sesi yang punya ringkasan tapi kehilangan
     * daftar gerakannya.
     */
    @Insert
    suspend fun insertAll(logs: List<SessionExerciseLog>)

    /**
     * Baca daftar gerakan MILIK SATU sesi, diurutkan `urutan` (0,1,2,...) supaya
     * tampil sama persis dengan urutan waktu kamu mengerjakannya: pemanasan di
     * atas, pendinginan di bawah.
     *
     * `Flow` supaya panel detail yang terbuka ikut segar sendiri kalau datanya
     * berubah. Dikembalikan sebagai keran, bukan sekali ambil.
     */
    @Query("SELECT * FROM session_exercise_logs WHERE sessionId = :sessionId ORDER BY urutan ASC")
    fun observeBySession(sessionId: Long): Flow<List<SessionExerciseLog>>
}
