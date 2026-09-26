package com.cezar.calisthenica.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cezar.calisthenica.model.Equipment
import com.cezar.calisthenica.model.Exercise
import com.cezar.calisthenica.model.ProgramExerciseRef
import com.cezar.calisthenica.model.WorkoutProgram

/**
 * Pintu masuk ke database. Satu file SQLite betulan, tersimpan di penyimpanan
 * privat app di HP-mu (`/data/data/com.cezar.calisthenica/databases/`), tidak
 * bisa dibaca app lain.
 *
 * `version = 6` -- naik dari 5 karena ada TABEL BARU: `program_exercises`, si
 * jembatan yang menghubungkan program dengan gerakan. Lihat MIGRATION_5_6 di
 * bawah, dan baca komentar panjang di `ProgramExerciseRef.kt` untuk tahu kenapa
 * tabel ini harus ada.
 *
 * Ini tabel yang paling lama ditunggu di proyek ini. Sampai kemarin, `programs`
 * dan `exercises` hidup sendiri-sendiri tanpa saling mengenal -- itu sebabnya
 * tulisan "Struktur sesi" di kartu dasbor selama ini cuma teks yang saya
 * hardcode di `WorkoutCard.kt`. Mulai versi 6, teks itu punya sumber data.
 *
 * `version = 5` dulu lahir dua kolom (`equipment.adjustableHeight` dan
 * `exercises.equipmentHeight`). Naik ke 4 lahir tabel `equipment`.
 *
 * Perhatikan pola yang mulai kelihatan setelah empat migration: menambah TABEL
 * BARU itu jenis perubahan yang PALING AMAN dari semuanya. Tidak ada satu pun
 * baris data lama yang dibaca, ditulis, atau dipindah -- tabel lamamu tidak
 * tahu-menahu ada tetangga baru. Bandingkan dengan MIGRATION_3_4 yang harus
 * menerjemahkan isi kolom yang sudah terisi; itu operasi yang bisa merusak data
 * dalam diam. Yang hari ini tidak bisa.
 *
 * Setiap kali BENTUK database berubah, angka ini WAJIB naik. Kalau tidak, Room
 * mendapati file di HP tidak sesuai cetak biru dan app langsung mati saat
 * dibuka.
 *
 * Dan naiknya angka itu selalu datang bersama TANGGUNG JAWAB: harus ada resep
 * yang memberitahu Room cara mengubah file lama jadi bentuk baru. Lihat
 * MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, dan MIGRATION_5_6 di bawah.
 *
 * `exportSchema = false` cuma mematikan peringatan Room soal menyimpan salinan
 * skema ke folder JSON. Berguna di tim besar untuk mengecek migration, tidak
 * perlu untuk kita sekarang.
 *
 * `@TypeConverters` di sini artinya: penerjemah di kelas Converters berlaku
 * untuk SELURUH database ini. Dipasang sekali di pintu masuk, bukan ditempel
 * satu-satu di setiap kolom. Perhatikan buahnya hari ini: `ProgramExerciseRef`
 * menyimpan `tipe: ExerciseType` dan saya tidak perlu menulis satu baris pun
 * penerjemah baru -- `fromType`/`toType` yang sudah ada sejak dulu langsung
 * dipakai. Itu untungnya memasang converter di pintu masuk.
 */
@Database(
    entities = [
        WorkoutProgram::class,
        Exercise::class,
        Equipment::class,
        ProgramExerciseRef::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun programDao(): ProgramDao

    abstract fun exerciseDao(): ExerciseDao

    abstract fun equipmentDao(): EquipmentDao

    abstract fun programExerciseDao(): ProgramExerciseDao

    companion object {
        // @Volatile + synchronized = supaya kalau dua bagian app minta
        // database di saat yang hampir sama, yang dibuat tetap SATU, bukan dua.
        // Membuka dua koneksi ke file yang sama itu sumber bug yang bikin
        // pusing: data tersimpan di satu koneksi, tidak kelihatan di koneksi
        // lain.
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * RESEP MIGRATION 2 -> 3. Inilah pelajaran yang saya janjikan sesi
         * lalu, dan hari ini kamu memang sudah harus memakainya -- karena di
         * Poco F5 sekarang ADA DUA GERAKAN ASLI yang kamu ketik sendiri
         * lengkap dengan fotonya, dan itu tidak boleh hilang cuma gara-gara
         * kita menambah satu kolom.
         *
         * Bacanya begini: "kalau kamu menemukan file database versi 2,
         * jalankan SQL ini, dan setelah itu dia sah disebut versi 3."
         *
         * `ALTER TABLE ... ADD COLUMN` itu SATU-SATUNYA perubahan bentuk yang
         * SQLite lakukan dengan murah dan aman. Dia tidak menyalin tabel, tidak
         * menyentuh baris yang sudah ada, cuma menambah satu kolom kosong di
         * kanan. Bandingkan dengan mengganti nama kolom atau mengubah tipenya:
         * di SQLite itu berarti bikin tabel baru, pindahkan semua baris, hapus
         * yang lama, ganti nama -- empat perintah yang tiap satunya bisa gagal
         * di tengah. Kalau suatu hari kamu berpikir "ganti tipe kolom saja",
         * ingat kalimat ini dulu.
         *
         * `DEFAULT ''` bukan hiasan, dia WAJIB. Kolomnya NOT NULL (di Kotlin
         * tipenya `List<Equipment>`, bukan `List<Equipment>?`), sementara dua
         * baris lamamu jelas tidak punya isi untuk kolom yang baru lahir.
         * SQLite akan menolak perintah ini tanpa DEFAULT. Dan `''` -- teks
         * kosong -- persis nilai yang dibaca Converters sebagai "list kosong",
         * alias "gerakan ini tanpa alat". Jadi kedua gerakanmu langsung punya
         * isi yang benar tanpa kita sentuh satu baris pun.
         *
         * `TEXT` karena Converters menyimpan `List<Equipment>` sebagai satu
         * teks "PULL_UP_BAR,RINGS". Tipe di SQL harus cocok dengan yang
         * dihasilkan Converters, bukan dengan tipe Kotlin-nya.
         *
         * Kalau resep ini SALAH, hukumannya keras: Room membandingkan hasil
         * akhirnya dengan cetak biru kolom per kolom, dan app menolak dibuka.
         * Bukan crash diam-diam, tapi crash saat dibuka. Jadi kalau nanti app
         * langsung tertutup begitu dibuka, jangan cari-cari di layar -- bilang
         * ke saya "baca logcat", dan jawabannya akan ada di situ.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE exercises ADD COLUMN equipment TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        /**
         * Tiga belas nama alat bawaan yang DULU hidup sebagai `enum Equipment`,
         * dalam URUTAN PERSIS seperti enum-nya. Urutan itu bukan selera: posisi
         * ke-1 jadi id 1, ke-6 jadi id 6, dan seterusnya. Kalau urutan daftar ini
         * digeser satu saja, gerakan "Wall Angle"-mu bisa berubah jadi butuh
         * "Handuk". Jangan pernah menyentuh daftar ini lagi setelah build ini
         * jalan -- dia sudah selesai bertugas.
         *
         * INI TEMPAT TERAKHIR ENUM ITU ADA DI SELURUH PROYEK. Setelah baris-baris
         * ini dieksekusi sekali di HP-mu, "PULL_UP_BAR" tidak akan pernah muncul
         * lagi di kode mana pun. Migration memang begitu sifatnya: dia jembatan
         * sekali pakai antara dunia lama dan dunia baru, dan dia harus tetap ada
         * di kode selamanya justru karena dia sekali pakai -- HP mana pun yang
         * lama tidak di-update masih butuh jembatan ini untuk pulang.
         */
        private val ALAT_LAMA = listOf(
            "PULL_UP_BAR" to "Pull Up Bar",
            "DIP_BAR" to "Dip Bar",
            "RINGS" to "Ring",
            "PARALLETTES" to "Parallettes",
            "WALL" to "Dinding",
            "FLOOR_MAT" to "Matras",
            "BOX" to "Box / Bangku",
            "CHAIR_TABLE" to "Kursi / Meja",
            "RESISTANCE_BAND" to "Resistance Band",
            "ADDED_WEIGHT" to "Beban Tambahan",
            "TOWEL" to "Handuk",
            "AB_WHEEL" to "Ab Wheel",
            "JUMP_ROPE" to "Skipping",
        )

        /**
         * RESEP MIGRATION 3 -> 4: alat berhenti jadi konstanta di kode dan mulai
         * jadi barang milikmu di database.
         *
         * Ini migration paling panjang yang pernah kita tulis, dan panjangnya
         * ada alasannya: dia harus MENYELAMATKAN dua gerakan asli di Poco F5-mu
         * yang tercatat memakai "Matras" dan "Handuk". Kalau saya malas, saya
         * cukup menghapus `fallbackToDestructiveMigration` dan membiarkan Room
         * membuang seluruh database -- app langsung bersih, nol kode, dan foto
         * serta instruksi yang kamu ketik sendiri hilang. Saya sengaja tidak
         * menawarkan pilihan itu.
         *
         * EMPAT LANGKAH, dan urutannya tidak boleh ditukar:
         *
         * LANGKAH 1 -- bikin tabelnya.
         *   Tulisan SQL-nya harus SAMA PERSIS dengan yang Room bayangkan dari
         *   `@Entity` di Equipment.kt: nama kolom, tipe, NOT NULL, semuanya.
         *   Room memeriksanya kolom per kolom setiap app dibuka. Salah satu
         *   spasi tidak masalah, salah satu kata (misal INTEGER jadi BIGINT)
         *   langsung bikin app menolak terbuka.
         *
         *   `AUTOINCREMENT` di situ bukan sinonim "nomor otomatis" -- dia punya
         *   satu sifat yang HARI INI jadi jaminan keamanan data kita: id yang
         *   sudah pernah dipakai TIDAK PERNAH didaur ulang, walau barisnya
         *   dihapus. Kenapa itu penting? Karena kalau kamu menghapus alat id 6,
         *   angka 6 masih tertulis di kolom gerakan lamamu. Tanpa AUTOINCREMENT,
         *   alat berikutnya yang kamu buat bisa kebagian id 6 lagi -- dan
         *   gerakan yang dulu butuh matras mendadak "butuh Ring". Data yang
         *   berubah sendiri tanpa ada yang menyentuhnya. Dengan AUTOINCREMENT,
         *   angka cuma maju, jadi id yatim tetap yatim dan diabaikan diam-diam.
         *
         * LANGKAH 2 -- tanam ketiga belas alat lama, dengan id yang KITA sebut
         *   sendiri (1..13). Biasanya id dibiarkan diisi SQLite, tapi di sini
         *   kita butuh kepastian: langkah 3 menerjemahkan nama jadi angka, dan
         *   dia harus tahu angkanya sebelum bertanya.
         *
         * LANGKAH 3 -- terjemahkan isi kolomnya. Ini inti pekerjaannya.
         *   `REPLACE(kolom, dari, ke)` di SQLite mengganti SEMUA kemunculan.
         *   Jadi "FLOOR_MAT,TOWEL" lewat 13 putaran REPLACE keluar jadi "6,11".
         *
         *   JEBAKAN yang saya periksa dulu sebelum menulis ini, dan kamu wajib
         *   tahu cara memeriksanya: REPLACE tidak peduli batas kata. Kalau ada
         *   satu nama enum yang jadi POTONGAN dari nama enum lain -- misalnya
         *   "BAR" dan "DIP_BAR" -- maka mengganti "BAR" lebih dulu akan merusak
         *   "DIP_BAR" jadi "DIP_2". Saya sudah membandingkan ketiga belasnya satu
         *   per satu: tidak ada satu pun yang jadi potongan yang lain, jadi
         *   REPLACE polos aman di sini. Kalau suatu hari daftarnya tidak seaman
         *   ini, obatnya sudah kamu lihat di `EquipmentDao.jumlahPemakai`:
         *   bungkus dulu daftarnya dengan koma di kedua ujung, lalu cari
         *   ",NAMA," bukan "NAMA".
         *
         * LANGKAH 4 -- buang alat yang tidak pernah kamu pakai.
         *   Kamu minta "tidak ada alat bawaan sama sekali", dan saya setuju. Tapi
         *   membuang ketiga belasnya buta-buta akan mencabut Matras dan Handuk
         *   dari gerakanmu. Jadi kita tanam semua dulu, terjemahkan, lalu hapus
         *   yang ternyata tidak ada yang menunjuk. Hasil akhirnya di HP-mu:
         *   tabel `equipment` berisi TEPAT DUA baris, Matras dan Handuk, dua-duanya
         *   belum berfoto -- siap kamu fotoi sendiri. Nol alat bawaan yang tidak
         *   kamu pakai, nol data hilang. Itu penawaran yang kamu setujui.
         *
         *   SQL-nya memakai trik koma yang sama, plus satu hal yang pantas kamu
         *   perhatikan: `FROM equipment AS e2, exercises AS x` itu penggabungan
         *   silang -- setiap alat dipasangkan dengan setiap gerakan. Terdengar
         *   mahal, dan untuk tabel besar memang mahal. Di sini 13 x 2 = 26
         *   perbandingan. Kadang jawaban yang "boros" itu jawaban yang benar,
         *   selama kamu tahu angkanya.
         *
         * Kalau ada satu saja yang salah di antara empat langkah ini, app akan
         * MENOLAK TERBUKA. Itu bukan bencana, itu justru bagusnya Room: dia lebih
         * memilih mati di depan matamu daripada jalan dengan data yang rusak.
         * Kalau itu terjadi: jangan uninstall, jangan panik. Bilang "baca logcat".
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // LANGKAH 1
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `equipment` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`photoFile` TEXT NOT NULL)",
                )

                // LANGKAH 2 & 3, sekali jalan per alat supaya urutan id dan
                // urutan penerjemahan tidak mungkin melenceng satu dari yang lain.
                ALAT_LAMA.forEachIndexed { index, (namaEnum, label) ->
                    val id = index + 1L

                    // Nama alat dikirim sebagai ARGUMEN (tanda ?), bukan
                    // ditempel ke dalam teks SQL. Di sini labelnya memang
                    // tulisan saya sendiri dan tidak berbahaya, tapi kebiasaan
                    // ini yang menyelamatkanmu nanti saat yang masuk adalah
                    // nama yang DIKETIK USER: satu tanda petik di "Bapak's Bar"
                    // sudah cukup untuk mematahkan perintah SQL yang ditempel.
                    db.execSQL(
                        "INSERT INTO equipment (id, name, photoFile) VALUES (?, ?, '')",
                        arrayOf<Any?>(id, label),
                    )

                    db.execSQL(
                        "UPDATE exercises SET equipment = REPLACE(equipment, ?, ?)",
                        arrayOf<Any?>(namaEnum, id.toString()),
                    )
                }

                // LANGKAH 4
                db.execSQL(
                    "DELETE FROM equipment WHERE id NOT IN (" +
                        "SELECT DISTINCT e2.id FROM equipment AS e2, exercises AS x " +
                        "WHERE (',' || x.equipment || ',') LIKE ('%,' || e2.id || ',%'))",
                )
            }
        }

        /**
         * 4 -> 5: dua kolom baru, nol data disentuh.
         *
         * Bandingkan panjangnya dengan MIGRATION_3_4 di atas. Yang itu 60 baris
         * penuh kehati-hatian karena dia MEMINDAHKAN ARTI data lamamu. Yang ini
         * dua baris, karena `ADD COLUMN ... NOT NULL DEFAULT 0` adalah satu-satunya
         * perubahan bentuk yang SQLite lakukan tanpa menyalin tabel: dia cuma
         * mencatat di kepala tabel "mulai sekarang ada kolom ini, dan yang belum
         * mengisinya dianggap 0". Baris-baris lamamu tidak dibaca, tidak ditulis,
         * tidak dipindah. Di HP-mu perintah ini selesai dalam hitungan milidetik
         * walau tabelnya berisi ribuan gerakan.
         *
         * Kenapa DEFAULT-nya harus ada, bukan boleh kosong: kolomnya `NOT NULL`
         * di cetak biru Room (karena `Boolean` dan `Int` di Kotlin tidak bisa
         * null). Menambah kolom NOT NULL ke tabel yang sudah berisi data tanpa
         * memberi nilai bawaan itu perintah yang mustahil dipenuhi -- SQLite
         * menolak, dan penolakannya muncul sebagai app yang tidak mau terbuka.
         *
         * Dan angka 0 kebetulan berarti hal yang benar di dua-duanya sekaligus:
         * `adjustableHeight` 0 = "alat ini tidak bisa diatur" (jawaban yang tepat
         * untuk semua alat yang sudah ada di gudangmu), `equipmentHeight` 0 =
         * `Ketinggian.BELUM`. Jadi setelah update ini app-mu tampak persis sama
         * sampai kamu sendiri menyalakan sakelarnya. Itu memang tujuannya.
         *
         * Perhatikan juga apa yang TIDAK ada di sini: tidak ada `arrayOf<Any?>`.
         * Argumen tanda tanya dipakai untuk NILAI yang bisa berasal dari user.
         * Nama kolom dan angka 0 di sini tulisan saya sendiri, tetap di setiap
         * HP, jadi tidak ada yang perlu diamankan.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE equipment ADD COLUMN adjustableHeight INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE exercises ADD COLUMN equipmentHeight INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        /**
         * 5 -> 6: satu tabel baru, nol baris lama disentuh.
         *
         * PELAJARAN HARI INI, dan ini yang paling gampang bikin app menolak
         * terbuka kalau kamu tidak tahu: KENAPA TIDAK ADA SATU PUN `DEFAULT` DI
         * SQL DI BAWAH, padahal di `ProgramExerciseRef.kt` hampir semua
         * propertinya punya nilai bawaan (`grup = INTI`, `target = 10`,
         * `setCount = 3`, `istirahatDetik = 60`)?
         *
         * Karena nilai bawaan Kotlin dan nilai bawaan SQLite itu DUA BENDA YANG
         * BERBEDA, di dua dunia yang berbeda.
         *
         *   `val target: Int = 10` -> berlaku saat KODE KOTLIN membuat objek
         *   tanpa menyebut target. Yang mengisinya compiler Kotlin, sebelum
         *   datanya sampai ke database.
         *
         *   `target INTEGER NOT NULL DEFAULT 10` -> berlaku saat ada perintah
         *   `INSERT` yang tidak menyebut kolom target sama sekali. Yang
         *   mengisinya SQLite.
         *
         * Kita tidak pernah butuh yang kedua, karena Room selalu mengirim SEMUA
         * kolom di setiap INSERT. Dan yang bikin ini bukan cuma soal selera:
         * Room MEMBANDINGKAN nilai bawaan SQL di HP dengan yang ada di cetak
         * biru `@Entity`. Cetak birunya tidak punya `@ColumnInfo(defaultValue = ...)`
         * satu pun, jadi Room mengharapkan kolom TANPA default. Kalau saya
         * baik hati menambahkan `DEFAULT 10` di sini, Room akan mendapati file
         * di HP-mu berbeda dari cetak biru, dan app MENOLAK TERBUKA. Kebaikan
         * yang tidak diminta itu bug.
         *
         * Perhatikan `equipment` di MIGRATION_3_4: `Equipment` juga punya nilai
         * bawaan Kotlin, dan SQL-nya juga tidak punya DEFAULT satu pun. Pola yang
         * sama, dan pola itu memang aturan.
         *
         * `AUTOINCREMENT` dipasang dengan alasan yang sama seperti di tabel
         * `equipment` dan sudah saya jelaskan panjang di sana: id yang pernah
         * dipakai tidak pernah didaur ulang.
         *
         * `IF NOT EXISTS` itu jaring pengaman untuk keadaan yang tidak terduga
         * (misalnya migration setengah jalan lalu app dimatikan paksa). Tanpa itu,
         * upaya kedua akan gagal karena tabelnya sudah ada, dan gagalnya muncul
         * sebagai app yang tidak mau dibuka.
         *
         * Yang TIDAK ada di sini dan itu keputusan sadar: FOREIGN KEY dan INDEX.
         * Alasan lengkapnya ada di komentar paling atas `ProgramExerciseRef.kt`;
         * ringkasnya, Room ikut memverifikasi keduanya kata per kata, dan
         * harganya tidak sepadan untuk MVP. Penghapusan sampahnya kita kerjakan
         * eksplisit lewat `hapusSemuaDiProgram` dan `hapusYatim`.
         *
         * Dan tidak ada `arrayOf<Any?>` di sini, alasannya sama seperti
         * MIGRATION_4_5: seluruh teks di bawah tulisan saya sendiri, tidak ada
         * sepotong pun yang berasal dari yang kamu ketik di HP.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `program_exercises` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`programId` INTEGER NOT NULL, " +
                        "`exerciseId` INTEGER NOT NULL, " +
                        "`grup` INTEGER NOT NULL, " +
                        "`urutan` INTEGER NOT NULL, " +
                        "`tipe` TEXT NOT NULL, " +
                        "`target` INTEGER NOT NULL, " +
                        "`setCount` INTEGER NOT NULL, " +
                        "`istirahatDetik` INTEGER NOT NULL, " +
                        "`catatan` TEXT NOT NULL)",
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    // applicationContext, BUKAN context Activity. Kalau pakai
                    // context Activity, database ikut memegang Activity itu
                    // dan Activity-nya tidak bisa dibuang dari memori
                    // (memory leak) tiap kali HP diputar.
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calisthenica.db",
                )
                    // ==========================================================
                    //  DUA BARIS INI URUTANNYA PENTING, DAN BUKAN CADANGAN
                    //  SATU SAMA LAIN. Bacanya:
                    //
                    //  addMigrations = "kalau ada jalan yang sudah saya
                    //  tuliskan, PAKAI itu." Ini yang dijalankan di HP-mu
                    //  nanti: file versi 2 -> resep ALTER TABLE -> versi 3,
                    //  dua gerakan berfotomu tetap utuh di tempatnya.
                    //
                    //  fallbackToDestructiveMigration = PARASUT untuk jalan
                    //  yang TIDAK saya tuliskan. Saya cuma menulis 2 -> 3.
                    //  Kalau di suatu HP masih ada file versi 1 (misal HP tes
                    //  lain yang tertinggal di zaman itu), Room tidak punya
                    //  jalan 1 -> 3 dan tanpa baris ini app-nya langsung mati.
                    //  Dengan baris ini, database itu dibuang dan dibuat ulang.
                    //
                    //  Jadi baris kedua BUKAN lagi "males nulis migration".
                    //  Dia sekarang cuma menangkap versi purba yang tidak ada
                    //  di HP-mu. Sekali kamu melihat gerakanmu masih ada
                    //  setelah update ini, kamu sudah punya bukti bahwa jalan
                    //  yang dipakai adalah jalan yang pertama.
                    //
                    //  DAN INI YANG WAJIB KAMU CATAT SEUMUR PROYEK: dari
                    //  sekarang, setiap kali kamu menambah/mengubah KOLOM,
                    //  kerjanya SELALU dua langkah -- naikkan `version`, lalu
                    //  tulis satu Migration lagi (3 -> 4, lalu 4 -> 5, dan
                    //  seterusnya). Room menyambung rantainya sendiri, jadi
                    //  jangan pernah menghapus resep yang lama walau kelihatan
                    //  tidak terpakai. Resep lama itu satu-satunya jalan pulang
                    //  bagi HP yang lama tidak di-update.
                    //
                    //  Hari ini rantainya jadi empat: 2->3, 3->4, 4->5, 5->6.
                    //  Kalau suatu HP masih memegang file versi 2, Room
                    //  menjalankan keempatnya berurutan sampai sampai ke 6. Itu
                    //  sebabnya MIGRATION_2_3 yang tampak "sudah tidak relevan"
                    //  tetap harus hidup di file ini.
                    // ==========================================================
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
