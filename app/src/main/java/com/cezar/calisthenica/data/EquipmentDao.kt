package com.cezar.calisthenica.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cezar.calisthenica.model.Equipment
import kotlinx.coroutines.flow.Flow

/**
 * Pintu masuk ke tabel `equipment`. Sengaja meniru `ExerciseDao` sedekat
 * mungkin, karena pola yang sudah TERBUKTI jalan di Poco F5 itu lebih berharga
 * daripada pola yang lebih pintar tapi belum pernah dijalankan.
 */
@Dao
interface EquipmentDao {

    /**
     * Semua alat, urut nama, buta huruf besar-kecil.
     *
     * `COLLATE NOCASE` itu yang bikin "ab wheel" duduk di sebelah "Ab Wheel"
     * dan bukan terbuang ke bawah semua nama berhuruf besar. Tanpa ini, SQLite
     * mengurutkan pakai kode ASCII, dan di ASCII semua huruf BESAR datang
     * sebelum huruf kecil -- jadi "Zebra" duduk di atas "ab wheel". Kelihatan
     * seperti bug pengurutan padahal cuma kita yang lupa bilang "abaikan besar
     * kecilnya".
     *
     * Kenapa `Flow` dan bukan `List` biasa: Flow itu langganan, bukan sekali
     * ambil. Begitu kamu menambah alat dari dalam dialog, SEMUA layar yang
     * sedang mendengarkan Flow ini ikut berubah sendiri -- form, katalog, layar
     * detail. Tidak ada satu pun baris "tolong refresh" yang perlu kamu tulis,
     * dan itu sebabnya alat baru bisa langsung muncul tercentang.
     */
    @Query("SELECT * FROM equipment ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Equipment>>

    /**
     * Mengembalikan `Long`: ID yang baru saja dibuatkan Room.
     *
     * INI BAGIAN YANG PALING PENTING DI SELURUH FILE INI, jadi jangan
     * dilewat. Tanpa nilai kembalian ini, dialog "+ Tambah alat" cuma bisa
     * menyimpan lalu berharap. Dengan nilai kembalian ini, dialog bisa langsung
     * berkata "alat nomor 14 sudah jadi, sekalian centang dia" -- alat baru
     * muncul dalam keadaan TERPILIH, tanpa kamu perlu mencarinya lagi di
     * daftar. Satu tipe kembalian, satu langkah kerja hilang dari hidupmu.
     *
     * Ini juga alasan `insert` alat dilakukan DI DALAM form, bukan dilaporkan
     * ke atas seperti `Exercise`. Cuma yang memanggil `insert` yang menerima
     * ID-nya.
     */
    @Insert
    suspend fun insert(equipment: Equipment): Long

    /** Ubah nama atau foto. Room mencocokkan barisnya lewat @PrimaryKey. */
    @Update
    suspend fun update(equipment: Equipment)

    @Delete
    suspend fun delete(equipment: Equipment)

    /**
     * Berapa gerakan yang memakai alat ini?
     *
     * PELAJARAN HARI INI, dan ini jebakan yang bikin banyak orang kehilangan
     * data tanpa sadar. Kolom `exercises.equipment` isinya daftar ID dipisah
     * koma: "6,11,3". Godaan pertama untuk mencari ID 1 di dalamnya:
     *
     *     WHERE equipment LIKE '%1%'
     *
     * Itu SALAH, dan salahnya diam-diam. Pola itu juga cocok dengan "11", "21",
     * "13", "100" -- semua angka yang kebetulan mengandung digit 1. Kamu akan
     * diberi tahu "alat ini dipakai di 9 gerakan" padahal cuma 1, lalu kamu
     * membatalkan penghapusan karena takut. Atau lebih parah, sebaliknya.
     *
     * Trik di bawah menutup lubang itu dengan cara yang murah: tempelkan koma
     * di kedua ujung daftarnya DAN di kedua ujung angka yang dicari.
     *
     *     ',' || '6,11,3' || ','   ->   ",6,11,3,"
     *     '%,' || 1 || ',%'        ->   "%,1,%"
     *
     * Sekarang "11" tidak mungkin lolos, karena yang dicari harus punya koma
     * PERSIS di kiri dan kanannya. Setiap ID di dalam daftar punya itu, termasuk
     * yang pertama dan yang terakhir, karena kita sendiri yang menambahkan koma
     * pembungkusnya. `||` di SQLite artinya sambung teks, bukan "atau".
     *
     * Angkanya dipakai untuk memperingatkanmu SEBELUM menghapus alat. Dan ya,
     * ini pengakuan jujur soal harga skema pilihan kita: kalau alat disimpan di
     * tabel penghubung, pertanyaan ini cuma satu `COUNT(*)` biasa yang dibantu
     * indeks. Dengan daftar koma, SQLite harus membaca dan memeriksa SETIAP
     * baris gerakan satu per satu. Untuk ratusan gerakan itu tidak terasa. Untuk
     * puluhan ribu, ini yang pertama akan terasa lambat, dan saat itu terjadi
     * kita pindah ke tabel penghubung. Kamu sudah tahu ongkosnya dari sekarang.
     */
    @Query(
        "SELECT COUNT(*) FROM exercises " +
            "WHERE (',' || equipment || ',') LIKE ('%,' || :id || ',%')",
    )
    suspend fun jumlahPemakai(id: Long): Int
}
