package com.cezar.calisthenica.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.cezar.calisthenica.model.WorkoutProgram
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object. Anggap ini DAFTAR PESANAN yang kamu serahkan ke
 * dapur. Kamu tidak masuk dapur dan tidak menyentuh SQLite langsung; kamu cuma
 * nulis "mau apa", lalu KSP yang menuliskan kode SQL-nya saat build.
 *
 * Perhatikan: di file ini TIDAK ADA satu pun isi fungsi. Semuanya cuma tanda
 * tangan fungsi + anotasi. Itu bukan kode setengah jadi -- memang begitu
 * bentuknya. Isinya dibuatkan otomatis.
 */
@Dao
interface ProgramDao {

    /**
     * `Flow` = keran, bukan gelas.
     *
     * Kalau ini `List<WorkoutProgram>` biasa, dia sekali ambil lalu selesai:
     * kamu simpan program baru, layar tidak tahu, dan kamu harus refresh
     * manual. Karena tipenya Flow, Room akan MENGABARI layar setiap kali isi
     * tabel `programs` berubah, dan Compose langsung menggambar ulang.
     *
     * Jadi nanti waktu kamu tekan Simpan di dialog, kita tidak menyuruh daftar
     * untuk memperbarui diri. Daftarnya tahu sendiri.
     */
    @Query("SELECT * FROM programs ORDER BY id DESC")
    fun observeAll(): Flow<List<WorkoutProgram>>

    /**
     * `suspend` = fungsi ini boleh berhenti sebentar tanpa membekukan layar.
     * Nulis ke disk itu lambat (untuk ukuran komputer), dan Android akan
     * melempar error kalau kamu melakukannya di thread yang menggambar UI.
     * Room mengurus pemindahan kerjanya ke belakang layar untukmu.
     */
    @Insert
    suspend fun insert(program: WorkoutProgram)

    @Delete
    suspend fun delete(program: WorkoutProgram)
}
