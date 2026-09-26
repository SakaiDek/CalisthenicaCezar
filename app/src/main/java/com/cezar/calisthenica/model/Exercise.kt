package com.cezar.calisthenica.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Satu GERAKAN di katalog. Ini master data: "Scapular Chin Up" ada satu kali
 * di sini, lalu boleh dipakai di sepuluh program berbeda tanpa disalin.
 *
 * Perhatikan yang TIDAK ada di sini: jumlah repetisi, lama tahan, dan waktu
 * istirahat. Itu bukan sifat gerakannya, itu sifat gerakan DI DALAM SEBUAH
 * PROGRAM. "Wall Angle" bisa 10 repetisi di program pemanasanmu dan 30 detik
 * tahan di program mobilitasmu -- gerakannya sama, angkanya beda. Angka-angka
 * itu tempatnya nanti di tabel relasi, bukan di sini.
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val category: ExerciseCategory,

    /**
     * Instruksi pelaksanaan. Satu String, antar langkah dipisah baris baru.
     * Nanti di layar detail kita pecah dengan `split("\n")` jadi daftar butir,
     * persis gaya "Execution" di app yang kamu tunjukkan. Tidak perlu tabel
     * sendiri hanya untuk menyimpan beberapa kalimat berurutan.
     */
    val instruction: String = "",

    /**
     * Saran satuan default: dihitung (REPS) atau ditahan (HOLD).
     * Statusnya SARAN, bukan hukum -- nanti tiap baris gerakan di dalam
     * program boleh menimpanya, seperti tombol `Repetitions | Seconds` yang
     * kamu lihat di rekamanmu.
     */
    val defaultType: ExerciseType = ExerciseType.REPS,

    /**
     * Otot yang dilatih. Disimpan sebagai SATU kolom teks (lihat Converters).
     * Ini keputusan sadar, bukan jalan pintas: kita tidak akan pernah bertanya
     * "cari semua gerakan yang melatih Latissimus", karena filter di katalog
     * memakai kategori, bukan otot. Yang kita butuh hanya "otot milik gerakan
     * INI" -- dan untuk itu satu kolom teks sudah cukup.
     */
    val muscles: List<Muscle> = emptyList(),

    /**
     * ID alat yang dibutuhkan, menunjuk ke tabel `equipment`. LIST KOSONG =
     * cukup badan sendiri; sengaja tidak ada baris "NONE" (alasannya panjang
     * dan penting, ada di Equipment.kt).
     *
     * PERHATIKAN `@ColumnInfo`, karena ini pelajaran hari ini dan bukan hiasan:
     * nama propertinya di Kotlin `equipmentIds`, tapi nama KOLOMNYA di SQLite
     * tetap `equipment` seperti sejak versi 3.
     *
     * Kenapa tidak sekalian nama kolomnya diganti biar seragam? Karena
     * `ALTER TABLE ... RENAME COLUMN` baru ada di SQLite 3.25, dan SQLite
     * 3.25 baru masuk Android sejak API 30. `minSdk` kita 26. Di HP Android 8
     * perintah itu langsung gagal, dan app-nya menolak dibuka. Jalan
     * satu-satunya kalau ngotot ganti nama kolom: buat tabel baru, salin semua
     * isinya, hapus tabel lama, ganti nama tabel barunya. Empat langkah
     * berisiko, demi kosmetik.
     *
     * Jadi kita bayar semurah-murahnya: satu anotasi. `@ColumnInfo` memang ada
     * untuk memisahkan "nama yang enak dibaca programmer" dari "nama yang sudah
     * tertulis di HP user". Ingat trik ini; nanti pasti kepakai lagi.
     *
     * ISINYA yang berubah hari ini, bukan bentuknya: dulu "FLOOR_MAT,TOWEL",
     * sekarang "6,11". Tipe kolomnya tetap TEXT, jadi Room tidak melihat
     * perbedaan bentuk tabel sama sekali. Yang harus dikerjakan migration 3 -> 4
     * cuma MENERJEMAHKAN isinya. Bandingkan dengan migration 2 -> 3 yang harus
     * menambah kolom betulan -- itu ubah bentuk, ini ubah isi. Dua pekerjaan
     * yang beda kelas, dan yang kedua jauh lebih aman.
     */
    @ColumnInfo(name = "equipment")
    val equipmentIds: List<Long> = emptyList(),

    /**
     * Ketinggian alat untuk gerakan INI, sebagai angka dari `Ketinggian.tingkat`.
     * 0 = belum diatur, dan 0 itu jawaban yang benar untuk hampir semua gerakan.
     *
     * Kenapa nempel di gerakan, bukan di alat: ring yang sama kamu pakai
     * setinggi pinggul untuk Ring Row dan setinggi kepala untuk Muscle Up. Kalau
     * angkanya disimpan di alat, mengubah satu gerakan akan ikut mengubah semua
     * gerakan lain yang memakai ring itu. Yang di alat cuma IZINnya
     * (`Equipment.adjustableHeight`), angkanya di sini.
     *
     * KETERUSTERANGAN MVP, biar kamu tidak menemukannya sendiri lalu kaget:
     * satu gerakan menyimpan SATU angka ketinggian. Kalau di satu gerakan kamu
     * mencentang dua alat yang dua-duanya bisa diatur (ring DAN palang), angka
     * ini berlaku untuk keduanya. Untuk menyimpan tinggi per-alat-per-gerakan,
     * kolom ini harus jadi tabel penghubung sendiri -- itu pekerjaan besar untuk
     * kasus yang belum pernah kamu alami. Kita tunda sampai kamu benar-benar
     * butuh, dan kamu sudah tahu batasnya dari sekarang.
     */
    val equipmentHeight: Int = 0,

    /**
     * Tiga kolom media. Isinya NAMA FILE, bukan path lengkap, dan bukan URI
     * galeri. Path folder privat app bisa berubah, nama file tidak. URI galeri
     * malah lebih parah: izinnya mati saat proses app mati, jadi videonya
     * "hilang" besok pagi.
     *
     * thumbnailFile & videoFile = file yang sudah kita salin ke folder privat
     * app, jalan tanpa internet dan tanpa satu pun permission.
     * youtubeUrl = jenis media yang BEDA sifatnya: harus keluar app atau
     * WebView, dan butuh sinyal. Sengaja dipisah supaya app tahu bedanya.
     */
    val thumbnailFile: String = "",
    val videoFile: String = "",
    val youtubeUrl: String = "",
)

/**
 * Kategori gerakan, sesuai filter chip di mockup Katalog Gerakan buatanmu.
 *
 * LUBANG ITU SAYA TUTUP HARI INI, 5 September 2026 -- dan saya menutupnya
 * dengan RIBUT, bukan diam-diam.
 *
 * Dulu di sini tertulis: "di lima ini tidak ada tempat untuk push-up atau dip.
 * Saya sengaja TIDAK menambahkannya diam-diam, karena mockup itu spesifikasimu."
 * Alasan itu masih saya pegang. Yang berubah: kamu bertanya "ada yang kurang?",
 * dan ini jawaban paling jujur yang saya punya. Aplikasi kalistenik tanpa
 * kategori PUSH itu seperti buku catatan latihan yang halaman push-up-nya
 * dicabut. Kamu tidak akan bisa mendaftarkan push-up, dip, pike push-up,
 * handstand push-up -- separuh isi kalistenik.
 *
 * Jadi VERTICAL_PUSH dan HORIZONTAL_PUSH saya tambahkan, dan saya sebutkan
 * terang-terangan di chat supaya kamu bisa membatalkannya kalau memang tidak
 * mau. Ongkosnya nol: yang tersimpan di database cuma NAMA nilai enum sebagai
 * teks, jadi tabelnya tidak berubah bentuk sama sekali dan tidak ada migration
 * yang perlu ditulis. Data gerakan yang sudah kamu isi selamat semuanya.
 *
 * Bandingkan dengan menambah KOLOM (lihat migration 2 -> 3 untuk `equipment`
 * di AppDatabase.kt): itu mengubah bentuk tabel, dan harganya satu migration
 * yang harus ditulis tangan. Menambah nilai enum: gratis. Menambah kolom:
 * bayar. Ingat bedanya, karena itu yang menentukan seberapa berani kamu
 * mengubah skema nanti.
 *
 * URUTAN di bawah ini bukan alfabet, tapi urutan pola gerak: tarik dulu, dorong
 * kemudian, lalu kaki, inti, dan terakhir skill. Urutan enum inilah yang jadi
 * urutan chip di katalog dan di form, jadi menyusunnya sekali di sini merapikan
 * dua layar sekaligus.
 */
enum class ExerciseCategory(val label: String) {
    VERTICAL_PULL("Vertical Pull"),
    HORIZONTAL_PULL("Horizontal Pull"),
    VERTICAL_PUSH("Vertical Push"),
    HORIZONTAL_PUSH("Horizontal Push"),
    LEGS("Legs"),
    CORE("Core"),
    SKILL("Skill"),
}

/** Dihitung, atau ditahan. Dua-duanya latihan, satuannya saja beda. */
enum class ExerciseType(val label: String) {
    REPS("Repetisi"),
    HOLD("Detik"),
}
