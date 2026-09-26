package com.cezar.calisthenica.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Satu alat latihan MILIK KAMU. Bukan daftar bawaan app -- daftar isi gudangmu.
 *
 * PELAJARAN PERTAMA HARI INI, dan ini pelajaran arsitektur yang paling penting
 * sejauh proyek ini jalan: KAPAN sesuatu berhenti jadi enum dan mulai jadi tabel.
 *
 * Sampai kemarin file ini berisi `enum class Equipment` dengan 13 nilai yang
 * SAYA yang menentukan. Itu sah selama daftarnya milik programmer. Enum artinya
 * "kemungkinannya sudah habis dan sudah diketahui saat kode ditulis" -- seperti
 * `ExerciseType` cuma bisa REPS atau HOLD, selamanya, karena memang cuma ada
 * dua cara mengukur satu gerakan.
 *
 * Begitu KAMU yang boleh menambah alat, tiga hal langsung mustahil dilakukan
 * enum, dan ketiganya bukan soal kerapian:
 *
 *   1. Enum ditulis di dalam kode. Menambah nilai enum artinya mengubah kode,
 *      lalu compile, lalu install ulang. User tidak bisa compile app. Titik.
 *   2. Enum TIDAK PUNYA IDENTITAS. `FLOOR_MAT` itu namanya sekaligus dirinya.
 *      Jadi kalau besok "Matras" mau kamu ganti jadi "Yoga Mat", enum-nya harus
 *      ganti nama, dan SEMUA gerakan yang tercatat memakai teks "FLOOR_MAT"
 *      mendadak menunjuk ke sesuatu yang tidak ada lagi. Datamu jadi yatim
 *      tanpa satu pun error muncul.
 *   3. Enum tidak bisa menyimpan foto. Foto punya nama file, nama file bisa
 *      ganti, dan sesuatu yang isinya bisa berubah itu DATA, bukan konstanta.
 *
 * Tabel menyelesaikan ketiganya sekaligus, dan kuncinya di poin 2: sebuah baris
 * punya `id` yang TIDAK IKUT BERUBAH saat namanya berubah. Itu sebabnya kolom
 * `equipment` di tabel exercises sekarang menyimpan "3,7", bukan
 * "FLOOR_MAT,TOWEL". Ganti nama sepuasnya; id 3 tetap id 3, dan setiap gerakan
 * yang menunjuk ke sana tetap benar. Ini bukan gaya-gayaan basis data, ini
 * satu-satunya cara supaya fitur "ubah nama alat" tidak merusak data lamamu.
 *
 * ------------------------------------------------------------------
 * ATURAN YANG SELAMAT DARI ENUM LAMA, dan tolong dipegang terus:
 *
 * TIDAK ADA baris "Tanpa alat" di tabel ini. Jangan pernah menambahkannya.
 * LIST KOSONG BERARTI BADAN SENDIRI.
 *
 * Alasannya sama seperti dulu: kalau ada baris "Tanpa alat", kalimat "gerakan
 * ini tidak butuh alat" punya DUA wujud di database -- list kosong, atau list
 * berisi satu id "Tanpa alat". Begitu satu keadaan punya dua wujud, kamu wajib
 * memeriksa dua-duanya di SETIAP layar. Cepat atau lambat ada satu tempat yang
 * lupa, dan kamu dapat gerakan bertuliskan "Tanpa alat, Pull Up Bar" sekaligus.
 * Layar yang menampilkannya yang bertugas menerjemahkan list kosong jadi
 * kalimat manusia, bukan database yang menyimpan kalimatnya.
 * ------------------------------------------------------------------
 *
 * @param id nol berarti "belum pernah disimpan". Room yang mengisi angka
 *   sebenarnya saat `insert`, dan `insert` MENGEMBALIKAN angka itu -- ingat ini,
 *   karena itulah satu-satunya cara dialog "+ Tambah alat" bisa langsung
 *   mencentang alat yang baru saja kamu buat.
 * @param name yang dilihat manusia. Bebas, termasuk "Pull Up Bar depan rumah"
 *   kalau kamu punya dua palang dan mau membedakannya.
 * @param photoFile NAMA FILE-nya saja, bukan path, bukan Uri galeri. Alasan
 *   lengkapnya ada di `data/MediaFiles.kt`: Uri galeri itu tiket masuk yang
 *   hangus, nama file di folder app itu milik kita selamanya. Kosong = belum
 *   ada foto, dan itu keadaan yang sah -- UI menggantinya dengan huruf pertama
 *   nama alatnya.
 * @param adjustableHeight sakelar yang KAMU nyalakan sendiri di dialog alat.
 *   Artinya: "alat ini bisa aku naik-turunkan". Ring, palang portabel, TRX.
 *   Palang pintu yang dibaut permanen: matikan.
 *
 *   Kenapa ini sebuah SAKELAR di data, bukan `if (name == "Gymnastic Rings")`
 *   di dalam kode -- dan ini pelajaran hari ini:
 *
 *   Kalau saya menuliskan nama alatnya di kode, app cuma mengenali ring yang
 *   NAMANYA persis seperti tebakan saya. Kamu menamainya "Ring kayu bikinan
 *   sendiri", fiturnya hilang tanpa pesan apa pun. Kamu menamainya "rings"
 *   huruf kecil, hilang juga. Dan hari kamu beli TRX, saya harus mengubah kode
 *   lagi lalu kamu install ulang. Sakelar memindahkan keputusan itu dari
 *   tanganku ke tanganmu, dan harganya cuma satu kolom INTEGER.
 *
 *   Room menyimpan Boolean sebagai angka: 0 mati, 1 hidup. Itu sebabnya
 *   migrasi 4 ke 5 menulis `INTEGER NOT NULL DEFAULT 0` -- semua alat yang
 *   sudah ada di gudangmu otomatis "tidak bisa diatur", yang memang jawaban
 *   yang benar untuk sebagian besar alat.
 */
@Entity(tableName = "equipment")
data class Equipment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val photoFile: String = "",
    val adjustableHeight: Boolean = false,
)
