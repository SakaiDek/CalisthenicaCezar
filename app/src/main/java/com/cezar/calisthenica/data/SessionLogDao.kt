package com.cezar.calisthenica.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cezar.calisthenica.model.SessionLog
import kotlinx.coroutines.flow.Flow

/**
 * DAO untuk tabel `session_logs` -- pintu masuk satu-satunya ke riwayat sesi.
 * Sama seperti `ProgramDao`, tidak ada isi fungsi di sini: cuma tanda tangan +
 * anotasi, dan KSP yang menuliskan SQL-nya saat build.
 *
 * PELAJARAN HARI INI: kenapa riwayat cukup DUA fungsi, tidak ada `@Delete`
 * dan tidak ada `@Update`. Riwayat itu catatan sejarah (lihat komentar panjang
 * di `SessionLog.kt`) -- kita cuma butuh MENULIS baris baru saat sesi selesai,
 * dan MEMBACA daftarnya untuk ditampilkan. Menyunting atau menghapus sejarah
 * belum jadi kebutuhan MVP; kalau nanti perlu "hapus satu entri riwayat",
 * tinggal tambah `@Delete` di sini tanpa mengubah tabel sama sekali.
 */
@Dao
interface SessionLogDao {

    /**
     * `suspend` karena menulis ke disk itu lambat untuk ukuran layar; Room
     * memindahkan kerjanya ke belakang layar sendiri. Dipanggil sekali saja
     * saat Runner menyentuh garis SELESAI (dijaga flag `sudahDicatat` supaya
     * satu sesi = satu baris, tidak dobel walau layar diputar).
     *
     * MENGEMBALIKAN `Long` (mulai DB v8): ini `rowId` yang baru saja dibuat
     * SQLite, dan karena kolom `id` kita `INTEGER PRIMARY KEY AUTOINCREMENT`,
     * angka itu SAMA dengan `id` baris ini. Kenapa kita butuh: baris anak di
     * `session_exercise_logs` harus tahu `sessionId` induknya, dan induknya
     * baru punya id SETELAH ditulis. Jadi urutannya wajib: tulis induk dulu,
     * ambil id-nya dari sini, baru tempel ke anak-anaknya. Dulu fungsi ini
     * mengembalikan Unit karena belum ada yang butuh id-nya; sekarang ada.
     */
    @Insert
    suspend fun insert(log: SessionLog): Long

    /**
     * `Flow` = keran: begitu ada baris riwayat baru masuk, layar Riwayat
     * langsung tahu dan menggambar ulang tanpa refresh manual.
     *
     * Diurutkan `waktuSelesaiMillis DESC` -- terbaru di ATAS. Sengaja pakai jam
     * dinding, BUKAN `id`, walau keduanya biasanya searah. Alasannya: yang
     * dijanjikan ke user adalah "urut dari yang paling baru KAMU KERJAKAN", dan
     * itu maknanya waktu, bukan nomor baris. Untuk sekarang hasilnya sama, tapi
     * mengurutkan berdasarkan makna (kapan) lebih jujur daripada berdasarkan
     * kebetulan teknis (nomor urut insert).
     */
    @Query("SELECT * FROM session_logs ORDER BY waktuSelesaiMillis DESC")
    fun observeAll(): Flow<List<SessionLog>>

    /**
     * KALENDER — daftar tanggal (teks ISO "yyyy-MM-dd") yang PERNAH ada sesinya,
     * tanpa dobel (`DISTINCT`). Satu hari bisa punya banyak sesi, tapi untuk
     * menaruh TITIK di grid kalender kita cuma butuh tahu "hari ini ada sesi
     * atau tidak" — jadi kita minta daftar tanggal unik saja, bukan seluruh
     * baris. Ini yang bikin fitur streak GRATIS: tidak ada tabel baru, cukup
     * membaca ulang kolom `tanggal` yang sudah ditulis Runner tiap sesi selesai.
     *
     * Sengaja TIDAK di-ORDER di SQL: pemakainya (layar Kalender) langsung
     * mengubah list ini jadi `Set<LocalDate>` untuk pencarian "hari X ada?"
     * yang O(1), jadi urutan dari DB tidak berarti apa-apa di sana.
     */
    @Query("SELECT DISTINCT tanggal FROM session_logs")
    fun observeSemuaTanggal(): Flow<List<String>>

    /**
     * KALENDER — semua sesi pada SATU tanggal (dipakai saat user mengetuk hari
     * bertitik). `:tanggal` adalah bind-argument Room (bukan tempel string),
     * jadi aman dari SQL injection walau nilainya berasal dari luar. Diurutkan
     * terbaru di atas, sama seperti `observeAll`, supaya kartu yang muncul di
     * bawah kalender konsisten dengan yang di layar Riwayat.
     */
    @Query("SELECT * FROM session_logs WHERE tanggal = :tanggal ORDER BY waktuSelesaiMillis DESC")
    fun observeByTanggal(tanggal: String): Flow<List<SessionLog>>
}
