package com.cezar.calisthenica.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * JEMBATAN antara program dan gerakan. Satu baris di sini = SATU gerakan yang
 * terpasang di SATU program, lengkap dengan angka-angkanya.
 *
 * Ini tabel yang selama ini tidak ada, dan ketidakhadirannya yang membuat
 * "Struktur sesi" di kartu dasbor cuma teks palsu. Sekarang teks itu punya
 * sumber.
 *
 * ---------------------------------------------------------------------------
 * PELAJARAN HARI INI: kenapa angka repetisi TIDAK boleh nempel di Exercise.
 *
 * Buka `Exercise.kt` dan baca komentar paling atasnya; saya sudah menuliskan
 * janji ini berbulan-bulan lalu: "Wall Angle bisa 10 repetisi di program
 * pemanasanmu dan 30 detik tahan di program mobilitasmu; gerakannya sama,
 * angkanya beda." Hari ini janji itu dibayar.
 *
 * Kalau `reps` disimpan di Exercise, satu gerakan cuma boleh punya satu angka
 * di seluruh app. Mengubah repetisi push-up di program hari Senin akan ikut
 * mengubah program hari Kamis. Data yang berubah sendiri tanpa kamu sentuh --
 * pola bug yang sama yang bikin kita memilih ID alat, bukan nama alat.
 *
 * Jadi aturannya: sifat GERAKAN tinggal di `exercises` (nama, instruksi, foto,
 * otot). Sifat gerakan DI DALAM SEBUAH PROGRAM tinggal di sini.
 * ---------------------------------------------------------------------------
 *
 * KENAPA ADA `id` SENDIRI, bukan kunci gabungan (programId + exerciseId).
 *
 * Ini keputusan yang gampang dianggap remeh, jadi saya jelaskan sekalian.
 * Kunci gabungan berarti "satu gerakan cuma boleh muncul sekali per program".
 * Kedengarannya wajar sampai kamu menyusun sesi betulan: push-up ringan di
 * pemanasan, lalu push-up berat lagi di grup inti. Gerakan yang sama, dua baris,
 * dua angka berbeda. Atau superset: Pull Up, Ring Row, Pull Up lagi.
 *
 * Dengan `id` sendiri, semua itu sah. Kunci gabungan akan menolaknya di tingkat
 * database, dan penolakan di tingkat database itu tembok yang tidak bisa
 * ditembus dari layar mana pun.
 *
 * KENAPA TIDAK ADA FOREIGN KEY (dan ini penawaran MVP yang saya buka
 * terang-terangan): Room bisa memasang `@ForeignKey ... onDelete = CASCADE`,
 * yang artinya menghapus program otomatis menghapus baris-baris ini. Itu memang
 * lebih rapi. Harganya: tulisan `CREATE TABLE` di migration harus SAMA PERSIS
 * dengan yang Room bayangkan, termasuk klausa foreign key dan dua indeks dengan
 * nama yang dibuat Room sendiri. Satu kata salah = app menolak terbuka, dan kamu
 * kehilangan satu siklus build 3 menit untuk mencari tahu kata mana.
 *
 * Gantinya, penghapusannya kita kerjakan sendiri dengan satu baris SQL eksplisit
 * (`ProgramExerciseDao.hapusSemuaDiProgram`). Lebih banyak kode manusia, jauh
 * lebih sedikit kode yang bisa salah tanpa suara. Kalau nanti tabel ini sudah
 * stabil dan kamu mau menaikkannya ke foreign key, itu satu migration sendiri.
 */
@Entity(tableName = "program_exercises")
data class ProgramExerciseRef(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Menunjuk ke `programs.id`. */
    val programId: Long,

    /** Menunjuk ke `exercises.id`. */
    val exerciseId: Long,

    /**
     * Nomor grup, bukan enum-nya langsung. Pola yang sama dengan
     * `Exercise.equipmentHeight` yang menyimpan `Ketinggian.tingkat`.
     *
     * Kenapa angka dan bukan teks "PEMANASAN": karena database yang harus
     * MENGURUTKANNYA. `ORDER BY grup` dengan angka menghasilkan pemanasan, inti,
     * pendinginan. Dengan teks, SQLite mengurutkan alfabet: INTI, PEMANASAN,
     * PENDINGINAN -- pendinginan sebelum inti, dan sesi latihanmu jadi ngawur.
     *
     * Ini beda dengan `tipe` di bawah yang boleh tetap enum: tidak ada satu pun
     * query yang mengurutkan berdasarkan tipe.
     */
    val grup: Int = Grup.INTI.nomor,

    /**
     * Urutan di DALAM grup, mulai dari 1. Bukan urutan global.
     *
     * Kenapa tidak mengandalkan `id` saja sebagai urutan: id itu urutan
     * PEMBUATAN, dan kamu pasti akan menyelipkan gerakan di tengah nanti.
     * Angka ini yang bisa ditukar tanpa menyentuh baris lain.
     *
     * Angkanya sengaja TIDAK dijamin rapat (1,2,3 tanpa bolong). Setiap kali
     * kamu menaik-turunkan gerakan, layar perakit menomori ulang seluruh grup
     * dari 1 -- jadi bolong apa pun langsung sembuh sendiri di simpanan
     * berikutnya. Menyembuhkan diri lebih murah daripada mencegah dengan
     * sempurna.
     */
    val urutan: Int = 0,

    /**
     * Dihitung atau ditahan, UNTUK BARIS INI. Nilai awalnya disalin dari
     * `Exercise.defaultType`, lalu bebas ditimpa di sini.
     *
     * Inilah arti kata "SARAN" yang sudah tertulis di `Exercise.defaultType`
     * sejak awal: sarannya di katalog, keputusannya di sini.
     */
    val tipe: ExerciseType = ExerciseType.REPS,

    /**
     * Angka sasaran per set. Artinya ditentukan `tipe`: kalau REPS ini jumlah
     * repetisi, kalau HOLD ini jumlah DETIK.
     *
     * Satu kolom untuk dua arti, bukan dua kolom (`reps` dan `detik`) yang salah
     * satunya selalu kosong. Dua kolom yang saling mengecualikan itu undangan
     * untuk data mustahil: baris yang punya 12 repetisi SEKALIGUS 90 detik.
     * Bentuk data yang tidak bisa salah lebih baik daripada bentuk data yang
     * dijaga supaya tidak salah.
     *
     * Contoh dari catatan latihanmu sendiri: leg up the wall tercatat
     * `tipe = HOLD, target = 160`. Butterfly yang lututnya naik-turun aktif
     * tercatat `tipe = HOLD, target = 81` -- ditahan waktunya, walau anggotanya
     * bergerak. Yang membedakan REPS dan HOLD itu SATUAN YANG KAMU HITUNG, bukan
     * diam atau bergeraknya badanmu.
     */
    val target: Int = 10,

    /** Berapa set. Nama `setCount`, bukan `set`, karena `set` itu kata kunci. */
    val setCount: Int = 3,

    /**
     * Istirahat SETELAH tiap set, dalam detik. 0 artinya lanjut tanpa henti;
     * itu jawaban yang benar untuk gerakan pemanasan yang dirangkai mengalir.
     */
    val istirahatDetik: Int = 60,

    /**
     * Catatan bebas untuk baris ini. Di sinilah "penanda beban tambahan" yang
     * kamu tulis di CLAUDE.md hidup: "rompi 5kg", "tempo turun 3 detik", "pakai
     * band merah".
     *
     * Sengaja teks bebas, BUKAN kolom `bebanKg: Int`. Beban tambahan di
     * kalistenik jarang berupa angka bersih -- kadang rompi, kadang satu kaki
     * diangkat, kadang cuma tempo yang diperlambat. Kolom angka akan memaksamu
     * membohongi datamu sendiri. Kalau nanti kamu mau grafik "beban naik dari
     * bulan ke bulan", baru kolom angka itu pantas dibuat, dan itu tabel log
     * latihan, bukan tabel ini.
     */
    val catatan: String = "",
)

/**
 * Tiga bagian satu sesi latihan, persis pembagian yang kamu tulis di CLAUDE.md.
 *
 * `nomor` ditulis tangan dan BUKAN ordinal enum, alasannya sama seperti di
 * `Ketinggian`: ordinal itu nomor kursi yang bergeser sendiri kalau ada anggota
 * baru diselipkan di tengah. Nomor tulis tangan tidak pernah bergeser, dan
 * angka inilah yang sudah tertulis di database HP-mu.
 *
 * Labelnya di Kotlin, bukan di strings.xml. Gaya rumah yang sama dengan
 * `ExerciseCategory`, `Muscle`, dan `Ketinggian`: menambah satu grup baru nanti
 * (misalnya "Mobilitas" di antara pemanasan dan inti) cukup satu baris di sini,
 * tidak menyeret perubahan ke file XML yang terpisah.
 *
 * Kalau suatu hari grup keempat ditambahkan, beri dia nomor 4 walau posisinya di
 * tengah, lalu biarkan `urut()` yang mengurutkan tampilannya. Nomor itu
 * identitas yang tersimpan; urutan tampil cuma selera hari ini.
 */
enum class Grup(val nomor: Int, val label: String) {
    PEMANASAN(1, "Pemanasan"),
    INTI(2, "Inti"),
    PENDINGINAN(3, "Pendinginan"),
    ;

    companion object {
        /**
         * Angka dari database jadi enum. Angka tak dikenal jatuh ke INTI, TIDAK
         * melempar exception -- alasannya sama seperti `muscleOrNull` di
         * Converters: data user tidak boleh mati gara-gara kita berubah pikiran.
         * Kalau suatu hari grup nomor 4 dihapus dari kode, baris latihan yang
         * memakainya cuma pindah ke Inti, bukan bikin app crash saat dibuka.
         */
        fun dari(nomor: Int): Grup = entries.firstOrNull { it.nomor == nomor } ?: INTI

        /** Urutan tampil di layar: pemanasan dulu, pendinginan terakhir. */
        fun urut(): List<Grup> = entries.sortedBy { it.nomor }
    }
}

/**
 * Perkiraan waktu satu baris latihan, dalam detik.
 *
 * ---------------------------------------------------------------------------
 * PELAJARAN HARI INI: rumus yang hidup di dua tempat, dan kenapa saya memilih
 * MENGAKUINYA daripada menyembunyikannya.
 *
 * Rumus ini ada DUA KALI di app: sekali di sini (Kotlin), sekali sebagai SQL di
 * `ProgramExerciseDao.observeRingkasan`. Itu pelanggaran aturan "satu benda satu
 * pemilik" yang saya khotbahkan di layar perakit, dan saya tidak mau kamu
 * menemukannya sendiri lalu curiga saya ceroboh.
 *
 * Alasannya begini. Layar perakit sudah memegang SELURUH daftar gerakan satu
 * program di memori, jadi menjumlahkannya di Kotlin itu gratis. Kartu dasbor
 * TIDAK: dia harus tahu total dari semua program sekaligus, dan kalau angkanya
 * dihitung di Kotlin, app harus menarik setiap baris dari setiap program ke
 * memori cuma untuk menjumlahkan lalu membuangnya. Database bisa menjumlahkan
 * tanpa mengirim satu baris pun. Untuk 5 program itu tidak terasa; untuk 60
 * program di tahun depan, itu bedanya dasbor yang mulus dan dasbor yang
 * tersendat setiap kali dibuka.
 *
 * Jadi pilihannya: satu rumus yang lambat di satu tempat, atau dua rumus yang
 * cepat dengan satu kewajiban. Saya ambil yang kedua, DAN saya bayar
 * kewajibannya dengan komentar keras di kedua sisi: UBAH SATU, UBAH DUANYA.
 * Utang yang ditulis di buku itu utang; utang yang tidak dicatat itu bom.
 * ---------------------------------------------------------------------------
 *
 * ISI RUMUSNYA, dan mana yang pasti mana yang tebakan:
 *
 * - HOLD itu PASTI. `setCount * target` -- 3 set tahan 30 detik ya 90 detik.
 *   Tidak ada yang bisa salah di sini.
 *
 * - REPS itu TAKSIRAN, dan angkanya 3 detik per repetisi. Ini murni tebakan
 *   tempo santai: satu detik turun, satu detik tahan, satu detik naik. Push-up
 *   cepatmu mungkin 1,5 detik; negative pull-up-mu mungkin 6 detik.
 *
 * - Istirahat DIHITUNG penuh, `setCount * istirahatDetik`, termasuk istirahat
 *   setelah set terakhir. Sengaja. Perkiraan waktu latihan lebih baik sedikit
 *   berlebih daripada sedikit kurang, karena kamu memakainya untuk memutuskan
 *   "sempat nggak nih sebelum magrib".
 *
 * CARA MEMPERBAIKI AKURASINYA nanti BUKAN dengan mengganti 3 jadi 4. Itu cuma
 * menukar satu tebakan dengan tebakan lain. Yang benar: satu kolom baru
 * `tempoDetik` di tabel ini, supaya setiap gerakan menyimpan temponya sendiri --
 * dan itu berarti satu migration baru (v6 ke v7). Jangan tergoda menambal angka
 * ajaib di dalam rumus.
 */
fun ProgramExerciseRef.perkiraanDetik(): Int {
    val kerja = if (tipe == ExerciseType.HOLD) {
        setCount * target
    } else {
        setCount * target * DETIK_PER_REPETISI
    }
    return kerja + setCount * istirahatDetik
}

/**
 * Tebakan tempo satu repetisi. Diberi nama supaya angka 3 di rumus di atas tidak
 * jadi "angka ajaib" yang tidak ada yang tahu asalnya enam bulan dari sekarang.
 *
 * Kembarannya ada di SQL: cari `target * 3` di `ProgramExerciseDao`.
 */
private const val DETIK_PER_REPETISI = 3
