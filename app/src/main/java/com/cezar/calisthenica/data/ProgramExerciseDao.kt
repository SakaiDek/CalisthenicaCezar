package com.cezar.calisthenica.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cezar.calisthenica.model.ExerciseCategory
import com.cezar.calisthenica.model.ProgramExerciseRef
import kotlinx.coroutines.flow.Flow

/**
 * Satu baris siap tampil di layar perakit: angka-angka dari tabel jembatan,
 * DITAMBAH nama dan foto yang diambil dari tabel gerakan.
 *
 * PELAJARAN HARI INI: `@Embedded`.
 *
 * Tanpa anotasi ini, Room akan bingung -- dia melihat sebuah kelas berisi satu
 * objek `ProgramExerciseRef` dan satu `String`, lalu mencari kolom bernama
 * "ref" di hasil query. Kolom itu tidak ada, dan build-nya gagal.
 *
 * `@Embedded` artinya: "jangan cari kolom `ref`; bongkar isi ProgramExerciseRef
 * dan cari kolom untuk SETIAP propertinya." Jadi `pe.*` di query di bawah
 * mengisi seluruh isi `ref` sekaligus, dan tiga baris `AS` mengisi sisanya.
 *
 * KENAPA TIDAK MENULIS `e.id` DI QUERY, dan ini jebakan yang nyata: kalau
 * saya `SELECT pe.*, e.*`, hasilnya punya DUA kolom bernama `id`. Room akan
 * mengambil yang mana? Salah satu, tanpa memberi tahu, dan kamu akan
 * menghapus baris yang salah suatu hari nanti. Jadi dari tabel gerakan saya
 * ambil HANYA tiga kolom yang saya butuh, masing-masing diberi nama baru.
 * Id gerakannya sudah ada di `ref.exerciseId`, tidak perlu diambil dua kali.
 */
data class BarisRakitan(
    @Embedded val ref: ProgramExerciseRef,
    val namaGerakan: String,
    val thumbnail: String,
    val kategori: ExerciseCategory,
)

/**
 * Hitungan per grup untuk SATU program, langsung dari database.
 *
 * Inilah pengganti `WorkoutProgram.exerciseCount` dan `estimatedMinutes` yang
 * selama ini cuma kolom mati berisi 0. Aturannya sudah tertulis di komentar
 * `WorkoutProgram.kt` sejak hari pertama: "Jangan pernah suruh user mengetik
 * angka yang app-nya sanggup menghitung sendiri." Hari ini janji itu ditepati.
 */
data class RingkasanGrup(
    val programId: Long,
    val grup: Int,
    val jumlahGerakan: Int,
    val totalDetik: Int,
)

@Dao
interface ProgramExerciseDao {

    /**
     * Isi satu program, sudah lengkap dengan nama dan foto gerakannya.
     *
     * KENAPA `INNER JOIN` DAN BUKAN `LEFT JOIN` -- ini penerapan langsung hukum
     * rumah yang sudah kita pakai untuk ID alat yatim di `Converters.kt`:
     * "converter menerjemahkan, LAYAR yang memutuskan angka itu masih berarti."
     *
     * `ExerciseDao.deleteByIds` bikin gerakan bisa hilang dari katalog kapan
     * saja. Baris jembatan yang menunjuk ke gerakan yang sudah tidak ada itu
     * baris yatim. `INNER JOIN` membuatnya TIDAK TAMPIL sama sekali -- lebih
     * baik daripada `LEFT JOIN` yang akan memaksa saya menggambar baris kosong
     * tanpa nama, yang cuma bikin kamu bingung.
     *
     * Tapi tidak tampil bukan berarti tidak ada; barisnya masih menumpuk di
     * disk dan masih ikut dihitung `observeRingkasan` di bawah. Itu sebabnya
     * `hapusYatim()` ada.
     *
     * `ORDER BY pe.grup, pe.urutan` -- dua tingkat, dan urutannya penting.
     * Grup dulu (pemanasan sebelum inti), baru urutan di dalam grup. Inilah
     * satu-satunya alasan `grup` disimpan sebagai ANGKA; kalau teks, SQLite
     * mengurutkannya alfabet dan pendinginan naik ke atas inti.
     */
    @Query(
        "SELECT pe.*, " +
            "e.name AS namaGerakan, " +
            "e.thumbnailFile AS thumbnail, " +
            "e.category AS kategori " +
            "FROM program_exercises AS pe " +
            "INNER JOIN exercises AS e ON e.id = pe.exerciseId " +
            "WHERE pe.programId = :programId " +
            "ORDER BY pe.grup, pe.urutan",
    )
    fun observeRakitan(programId: Long): Flow<List<BarisRakitan>>

    /**
     * Hitungan semua program sekaligus, satu baris per (program, grup).
     *
     * KENAPA SATU QUERY UNTUK SEMUA PROGRAM, bukan satu query per kartu:
     * dasbor menggambar sepuluh kartu, dan sepuluh query terpisah berarti
     * sepuluh Flow yang masing-masing membangunkan database. Satu query yang
     * dikelompokkan lalu dibagi-bagi di Kotlin jauh lebih murah, dan di HP
     * bedanya kelihatan saat scroll.
     *
     * ---------------------------------------------------------------------
     * RUMUS PERKIRAAN WAKTU, dan saya buka apa adanya karena ini TAKSIRAN,
     * bukan kebenaran:
     *
     *   HOLD -> setCount * target detik. Ini bukan taksiran, ini eksak.
     *           Leg up the wall 1 set x 160 detik = 160 detik. Titik.
     *
     *   REPS -> setCount * target * 3 detik. INI yang taksiran: saya anggap
     *           satu repetisi rata-rata 3 detik (turun-naik dengan tempo
     *           wajar). Pull-up eksplosifmu mungkin 1,5 detik; negative
     *           chin-up-mu bisa 8 detik.
     *
     *   Lalu ditambah setCount * istirahatDetik.
     * ---------------------------------------------------------------------
     *
     * =====================================================================
     * AWAS, RUMUS INI PUNYA KEMBARAN: `ProgramExerciseRef.perkiraanDetik()`
     * di `model/ProgramExerciseRef.kt`. UBAH SATU, UBAH DUA-DUANYA.
     *
     * Kenapa sengaja dikembarkan (dan bukan kelalaian): layar perakit sudah
     * memegang seluruh baris satu program di memori, jadi versi Kotlin gratis
     * di sana. Dasbor butuh total SEMUA program sekaligus; kalau dihitung di
     * Kotlin, app harus menarik setiap baris dari setiap program ke memori cuma
     * untuk dijumlahkan lalu dibuang. SQLite menjumlahkan tanpa mengirim satu
     * baris pun. Penjelasan panjangnya ada di KDoc `perkiraanDetik()`.
     * =====================================================================
     *
     * Kalau nanti kamu ingin lebih akurat, jalan yang benar bukan mengubah
     * angka 3 jadi 4, tapi menambah kolom `tempoDetik` di tabel ini -- dan itu
     * satu migration baru. Untuk MVP, taksiran yang jujur lebih berguna
     * daripada kolom yang belum pernah kamu isi.
     *
     * `tipe = 'HOLD'` bisa ditulis begitu karena `Converters.fromType`
     * menyimpan `value.name`, bukan `value.label`. Kalau dulu kita menyimpan
     * label ("Detik"), perbandingan ini akan rusak begitu labelnya diperhalus.
     * Keputusan kecil di Converters, buahnya dipetik di sini.
     *
     * `COALESCE(SUM(...), 0)` bukan basa-basi: Room memverifikasi kolom hasil
     * harus non-null karena `totalDetik: Int`. `SUM` atas grup kosong
     * menghasilkan NULL, dan NULL masuk ke `Int` itu crash dari dalam kode
     * bikinan KSP -- jenis crash yang paling susah dilacak.
     */
    @Query(
        "SELECT programId, grup, " +
            "COUNT(*) AS jumlahGerakan, " +
            "COALESCE(SUM(" +
            "  setCount * (CASE WHEN tipe = 'HOLD' THEN target ELSE target * 3 END)" +
            "  + setCount * istirahatDetik" +
            "), 0) AS totalDetik " +
            "FROM program_exercises " +
            "GROUP BY programId, grup " +
            "ORDER BY programId, grup",
    )
    fun observeRingkasan(): Flow<List<RingkasanGrup>>

    /**
     * Nomor urut terakhir yang terpakai di satu grup. Dipakai saat menambah
     * gerakan baru: nomornya = hasil ini + 1, jadi gerakan baru selalu masuk
     * ke EKOR grup, tidak menyalip yang sudah ada.
     *
     * `COALESCE(MAX(urutan), 0)` -- `MAX` atas nol baris menghasilkan NULL,
     * dan grup yang masih kosong itu keadaan yang PALING sering terjadi (setiap
     * program baru). Tanpa COALESCE, gerakan pertama di setiap grup akan bikin
     * app crash. Alasannya sama persis dengan penjaga `isBlank()` di
     * Converters: keadaan kosong itu bukan kasus langka, itu keadaan awal.
     */
    @Query(
        "SELECT COALESCE(MAX(urutan), 0) FROM program_exercises " +
            "WHERE programId = :programId AND grup = :grup",
    )
    suspend fun urutanTerakhir(programId: Long, grup: Int): Int

    @Insert
    suspend fun insert(ref: ProgramExerciseRef): Long

    @Update
    suspend fun update(ref: ProgramExerciseRef)

    /**
     * Simpan beberapa baris sekaligus. Dipakai saat menaik-turunkan gerakan:
     * satu grup dinomori ulang dari 1, lalu seluruh grup disimpan dalam SATU
     * panggilan.
     *
     * Kenapa tidak memanggil `update` di dalam loop: setiap panggilan suspend
     * ke Room itu satu transaksi sendiri. Sepuluh panggilan = sepuluh kali
     * tulis ke disk, dan kalau app mati di tengah, kamu punya grup dengan dua
     * gerakan bernomor 3. Satu panggilan dengan list = satu transaksi: semua
     * berhasil, atau semua tidak terjadi.
     */
    @Update
    suspend fun updateSemua(refs: List<ProgramExerciseRef>)

    @Delete
    suspend fun delete(ref: ProgramExerciseRef)

    /**
     * Kosongkan satu program. WAJIB dipanggil SEBELUM `ProgramDao.delete`.
     *
     * Inilah harga dari keputusan "tanpa foreign key" yang saya tulis di
     * `ProgramExerciseRef.kt`. Dengan `onDelete = CASCADE`, SQLite mengerjakan
     * ini sendiri. Tanpa itu, urusannya jadi tanggung jawab kita -- dan kalau
     * lupa, baris-baris ini menetap di disk selamanya, tak terlihat oleh layar
     * mana pun, tapi tetap ikut dihitung `observeRingkasan` untuk program yang
     * sudah tidak ada.
     *
     * Urutannya tidak boleh dibalik. Hapus program dulu, lalu app mati sebelum
     * baris ini jalan, dan sampahnya tertinggal tanpa jalan pulang: id
     * programnya sudah tidak ada di layar mana pun, jadi tidak ada yang bisa
     * memerintahkan penghapusannya lagi.
     */
    @Query("DELETE FROM program_exercises WHERE programId = :programId")
    suspend fun hapusSemuaDiProgram(programId: Long)

    /**
     * Buang baris yang gerakannya sudah dihapus dari katalog.
     *
     * Dipanggil sekali saat dasbor dibuka. TIDAK perlu penjaga "jangan jalan di
     * frame pertama" seperti pembersih ketinggian alat dulu, dan bedanya patut
     * dipahami: pembersih itu membaca sebuah `Flow` yang frame pertamanya
     * selalu `emptyList()`, jadi tanpa penjaga dia menyimpulkan "tidak ada alat
     * yang bisa diatur" lalu menghapus data yang sehat. Baris ini tidak membaca
     * apa pun dari memori -- keputusannya diambil SQLite sendiri di dalam satu
     * perintah, atas data yang sudah pasti ada di disk. Tidak ada frame pertama
     * untuk salah dibaca.
     *
     * `NOT IN (SELECT id FROM exercises)` aman di sini karena `exercises.id`
     * tidak pernah NULL (dia primary key). Kalau suatu hari subquery-nya bisa
     * menghasilkan NULL, `NOT IN` akan sunyi-sunyi tidak menghapus apa pun --
     * jebakan SQL klasik yang layak kamu ingat.
     */
    @Query("DELETE FROM program_exercises WHERE exerciseId NOT IN (SELECT id FROM exercises)")
    suspend fun hapusYatim()
}
