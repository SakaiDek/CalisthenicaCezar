package com.cezar.calisthenica.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cezar.calisthenica.model.Exercise
import kotlinx.coroutines.flow.Flow

/**
 * Daftar pesanan untuk tabel `exercises`. Bentuknya sama dengan ProgramDao:
 * tanda tangan fungsi saja, isinya ditulis KSP saat build.
 */
@Dao
interface ExerciseDao {

    /**
     * Perhatikan urutannya BEDA dari daftar program.
     *
     * Program diurutkan `id DESC` (terbaru di atas) karena itu daftar kerja --
     * yang baru kamu buat itu yang mau kamu pakai sekarang. Katalog gerakan
     * kebalikannya: itu KAMUS. Di kamus kamu mencari nama yang sudah kamu tahu,
     * jadi abjad yang benar, bukan waktu pembuatan.
     *
     * `COLLATE NOCASE` = "muscle up" dan "Muscle Up" diurutkan setara. Tanpa
     * ini SQLite menaruh SEMUA huruf besar sebelum huruf kecil, jadi "Zombie
     * Squat" muncul di atas "archer pull up". Itu bukan abjad, itu kode ASCII.
     */
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Exercise>>

    /**
     * Untuk layar detail gerakan nanti. Tipenya `Exercise?` (boleh null) karena
     * gerakannya bisa saja dihapus sementara layar detailnya masih terbuka --
     * kalau tipenya tidak boleh null, itu jadi crash. Begini, layar cuma
     * menerima null dan bisa menutup dirinya dengan sopan.
     */
    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: Long): Flow<Exercise?>

    /**
     * Mengembalikan `Long`, bukan Unit. Itu nomor id yang baru saja diberikan
     * database.
     *
     * Kenapa perlu? Ingat fitur "+ Add New Exercise" di tengah-tengah merakit
     * program: kamu bikin gerakan baru, dan gerakan itu harus LANGSUNG nempel
     * ke grup yang sedang kamu isi. Untuk menempelkannya kita butuh id-nya, dan
     * satu-satunya yang tahu id barunya adalah database. Kalau tidak diminta
     * sekarang, nanti kita harus mencari ulang gerakan itu berdasarkan nama --
     * dan itu rusak begitu ada dua gerakan bernama sama.
     */
    @Insert
    suspend fun insert(exercise: Exercise): Long

    /**
     * @Update mencocokkan baris lewat @PrimaryKey. Jadi ini aman HANYA kalau
     * object yang kamu kirim membawa id aslinya. Kalau id-nya 0, tidak ada
     * yang berubah dan tidak ada error -- bug paling sunyi di Room.
     */
    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    /**
     * Hapus banyak gerakan sekaligus berdasarkan daftar id.
     *
     * PELAJARAN KEDUA HARI INI: kenapa ini SATU query, bukan pengulangan
     * `delete()` di dalam for-loop.
     *
     * Kalau kamu memilih 12 gerakan lalu saya panggil `delete()` dua belas kali,
     * SQLite membuka dan menutup transaksi dua belas kali. Tiap transaksi
     * berakhir dengan menunggu disk benar-benar selesai menulis (fsync) --
     * bagian paling lambat dari seluruh proses. Lebih buruk lagi untuk kita:
     * tabel ini diawasi `Flow`, jadi dua belas transaksi berarti dua belas kali
     * layarmu diberi tahu "datanya berubah!", dan daftar itu berkedip menyusut
     * satu per satu di depan matamu.
     *
     * Satu `DELETE ... WHERE id IN (...)` = satu transaksi, satu fsync, satu
     * kabar ke Flow. Dua belas kartu hilang serentak dalam satu gambar ulang.
     * Itu bukan cuma lebih cepat, itu yang bikin gerakannya terasa "mantap"
     * bukan "rusak".
     *
     * Perhatikan `:ids` diberi List, dan Room yang mengembangkannya sendiri jadi
     * `IN (?, ?, ?, ...)` sebanyak isinya. Ini SATU-SATUNYA cara yang benar --
     * jangan pernah menyusun teks SQL sendiri dengan joinToString, karena itu
     * pintu masuk SQL injection. Di sini isinya cuma angka id dari layarmu
     * sendiri, tapi kebiasaan buruk begini yang suatu hari kamu tulis di tempat
     * datanya datang dari user.
     *
     * Batas yang perlu kamu tahu: SQLite punya langit-langit ~999 parameter per
     * query. Kalau suatu hari kamu bisa memilih ribuan baris sekaligus, ini
     * harus dipecah per 900-an. Untuk katalog gerakan, itu tidak akan terjadi.
     */
    @Query("DELETE FROM exercises WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
