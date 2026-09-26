package com.cezar.calisthenica.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TABEL ANAK dari `session_logs`. Satu baris di sini = SATU gerakan yang
 * tercatat di dalam SATU sesi riwayat. Kalau `SessionLog` itu sampul buku
 * catatan ("Sabtu, 3 set, 5 gerakan"), tabel ini isi halamannya: gerakan apa
 * saja yang ada di sesi itu, satu per satu.
 *
 * ---------------------------------------------------------------------------
 * PELAJARAN HARI INI: kenapa tabel ini MENYALIN nama gerakan, bukan menunjuk
 * ke `exercises` lewat foreign key.
 *
 * Kamu sudah membuktikannya sendiri di Poco F5: kamu hapus total program
 * 'fhfh', dan barisnya di Riwayat tetap utuh. Itu bekerja karena `SessionLog`
 * MENYALIN `programNama`, bukan mengikatnya. Prinsip yang sama persis kita
 * pakai di sini, satu tingkat lebih dalam: baris ini menyalin `namaGerakan`.
 *
 * Bayangkan kalau tidak. Kamu latihan "Push Up" hari ini, tercatat di riwayat.
 * Bulan depan kamu hapus gerakan "Push Up" dari katalog karena mau ganti nama.
 * Kalau tabel ini menunjuk ke `exercises.id`, halaman riwayat hari ini
 * mendadak kosong atau -- lebih buruk -- id itu didaur ulang dan riwayatmu
 * berubah jadi "Squat". Sejarah yang bisa berubah sendiri itu bukan sejarah.
 *
 * Jadi aturannya sama seperti seluruh keluarga riwayat: yang masuk ke sini
 * adalah FOTO keadaan saat sesi selesai, bukan tali ke data induk yang masih
 * hidup dan bisa berubah.
 * ---------------------------------------------------------------------------
 *
 * KENAPA `sessionId` cuma `Long` biasa, BUKAN `@ForeignKey` ke `session_logs`.
 * Alasannya sama dengan `ProgramExerciseRef` dan `SessionLog`: foreign key
 * memaksa tulisan `CREATE TABLE` di migration cocok kata-per-kata dengan yang
 * Room bayangkan (termasuk indeks bernama otomatis), dan satu kata salah =
 * app menolak terbuka. Untuk MVP harganya tidak sepadan. `sessionId` cukup
 * jadi angka penanda; membaca detail satu sesi tinggal `WHERE sessionId = ?`.
 *
 * KENAPA yang disimpan STRUKTUR TERENCANA (target + setCount dari resep), BUKAN
 * jumlah set yang BENAR-BENAR kamu selesaikan per gerakan. Ini penawaran MVP
 * yang saya buka terang-terangan: Runner sekarang cuma menghitung `setSelesai`
 * sebagai TOTAL satu sesi, tidak per gerakan, dan garis SELESAI bisa dicapai
 * lewat "Lewati". Melacak set-aktual per gerakan butuh state baru di Runner --
 * pekerjaan sendiri. Untuk sekarang kita foto rencananya ("Push Up: 3 set x 10
 * rep"), yang sudah menjawab pertanyaanmu "gerakan apa aja di sesi itu". Kalau
 * nanti kamu mau "berapa set yang benar-benar kelar", itu kolom `setSelesai`
 * baru di sini + penghitung per-gerakan di Runner, satu ronde tersendiri.
 */
@Entity(tableName = "session_exercise_logs")
data class SessionExerciseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Menunjuk ke `session_logs.id`. Angka biasa, bukan foreign key. */
    val sessionId: Long,

    /**
     * Urutan tampil di dalam sesi, mulai 0. Diisi dari posisi gerakan di
     * `rakitan` yang SUDAH diurutkan query (grup lalu urutan), jadi menyimpan
     * indeksnya saja sudah cukup -- pemanasan tetap di atas, pendinginan di
     * bawah, tanpa perlu mengurutkan ulang saat dibaca.
     */
    val urutan: Int,

    /** FOTO nama gerakan saat sesi selesai. Lihat komentar panjang di atas. */
    val namaGerakan: String,

    /**
     * FOTO nomor grup (1 pemanasan, 2 inti, 3 pendinginan). Angka, bukan teks,
     * alasan sama seperti `ProgramExerciseRef.grup`. Dibaca balik lewat
     * `Grup.dari(grup)` untuk dapat labelnya saat digambar.
     */
    val grup: Int,

    /**
     * FOTO tipe gerakan: REPS atau HOLD. Disimpan sebagai enum -- Converters
     * yang sudah terpasang di pintu database (fromType/toType) menerjemahkannya
     * jadi teks tanpa saya menulis kode baru. Dipakai untuk memilih kata saat
     * menggambar: "10 rep" (REPS) atau "tahan 30 dtk" (HOLD).
     */
    val tipe: ExerciseType = ExerciseType.REPS,

    /** FOTO angka sasaran per set. REPS = jumlah repetisi, HOLD = jumlah detik. */
    val target: Int,

    /** FOTO berapa set yang DIRENCANAKAN untuk gerakan ini. */
    val setCount: Int,

    /**
     * FOTO nama file foto gerakan saat sesi selesai (thumbnail). Sama filosofinya
     * dengan `namaGerakan`: kita SALIN, bukan mengikat ke katalog. Bulan depan
     * kamu boleh hapus gerakan "Push Up" dari katalog -- riwayat hari ini tetap
     * memegang foto yang benar karena namanya sudah tersimpan di sini.
     *
     * KENAPA `String?` (boleh null / boleh kosong), dan ini yang menjawab
     * permintaanmu "kalau master gerakannya sudah dihapus, thumbnail-nya jadi
     * placeholder kosong":
     *   - Baris riwayat LAMA (dari sebelum kolom ini ada) otomatis bernilai null,
     *     karena migration menambah kolom tanpa nilai bawaan. Itu sah.
     *   - `FotoAlat` di layar Riwayat sudah menangani teks kosong/null dengan
     *     anggun: file tak ada -> kotak placeholder, bukan crash.
     * Jadi kolom yang boleh kosong ini bukan kelalaian, tapi memang cara membuat
     * "foto tidak tersedia" jadi keadaan yang WAJAR, bukan kesalahan.
     *
     * Nilai bawaan `= null` PENTING supaya konstruktor lama (dan migration) tak
     * wajib mengisinya, dan supaya Room tak menuntut kolom NOT NULL.
     */
    val fotoUri: String? = null,
)
