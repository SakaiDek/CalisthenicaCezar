package com.cezar.calisthenica.data

import androidx.room.TypeConverter
import com.cezar.calisthenica.model.ExerciseCategory
import com.cezar.calisthenica.model.ExerciseType
import com.cezar.calisthenica.model.Muscle

/**
 * PELAJARAN HARI INI: SQLite cuma kenal lima jenis isi -- teks, angka bulat,
 * angka desimal, gumpalan biner, dan kosong. Titik. Dia tidak tahu apa itu
 * `List<Muscle>`, dan dia tidak akan pernah tahu.
 *
 * Jadi kita jadi penerjemahnya. Room akan memanggil fungsi-fungsi di bawah ini
 * OTOMATIS, dua arah: saat menyimpan (Kotlin -> teks) dan saat membaca
 * (teks -> Kotlin). Kamu tidak akan pernah memanggilnya sendiri. Di kode kamu
 * tetap menulis `exercise.muscles.forEach { ... }` seperti list biasa, seolah
 * database memang paham enum.
 *
 * Ini yang bikin keputusan "satu kolom teks, bukan tabel penghubung" jadi
 * murah: harganya cuma file ini, sekali tulis, lalu tidak pernah disentuh lagi.
 */
class Converters {

    // ---------- List<Muscle> <-> "LATISSIMUS,UPPER_BACK" ----------

    @TypeConverter
    fun fromMuscles(value: List<Muscle>): String =
        // Simpan .name (LATISSIMUS), JANGAN .label ("Latissimus"). Kalau suatu
        // hari kamu mengganti label supaya lebih enak dibaca di layar, data
        // lamamu tidak ikut rusak. Yang tampil di mata user dan yang tersimpan
        // di disk sengaja dipisah.
        value.joinToString(",") { it.name }

    @TypeConverter
    fun toMuscles(value: String): List<Muscle> {
        // Jebakan yang wajib dijaga: "".split(",") di Kotlin TIDAK menghasilkan
        // list kosong, tapi list berisi satu string kosong. Tanpa penjaga ini,
        // setiap gerakan yang belum dipilih ototnya akan bikin app crash saat
        // dibaca.
        if (value.isBlank()) return emptyList()

        return value.split(",").mapNotNull { raw -> muscleOrNull(raw.trim()) }
    }

    /**
     * Sengaja TIDAK memakai `Muscle.valueOf(name)`.
     *
     * `valueOf` melempar exception kalau namanya tidak dikenal. Bayangkan suatu
     * hari kamu menggabung FRONT_DELT dan REAR_DELT jadi satu SHOULDER: semua
     * baris lama masih menyimpan "FRONT_DELT", dan app-mu akan mati saat
     * membuka katalog -- crash-nya pun terjadi di dalam perut Room, susah
     * dilacak. Dengan cara ini, baris itu cuma kehilangan satu penanda otot.
     *
     * Data user tidak boleh mati gara-gara kamu berubah pikiran.
     */
    private fun muscleOrNull(name: String): Muscle? =
        Muscle.entries.firstOrNull { it.name == name }

    // ---------- List<Long> <-> "6,11" (ID alat) ----------

    /**
     * Bentuknya persis pola Muscle di atas, ISINYA yang naik kelas: dulu di sini
     * tersimpan NAMA enum ("FLOOR_MAT,TOWEL"), sekarang ID baris tabel ("6,11").
     *
     * KENAPA ID DAN BUKAN NAMA -- ini inti perombakan hari ini, jadi pelan-pelan.
     * Nama alat sekarang boleh kamu ubah kapan saja. Kalau yang tersimpan di
     * kolom ini teks "Matras", lalu kamu ganti namanya jadi "Yoga Mat", setiap
     * gerakan yang memakai matras mendadak menunjuk ke sesuatu yang tidak ada
     * lagi -- tanpa error, tanpa crash, cuma penanda alat yang hilang diam-diam.
     * Bug paling jahat memang yang tidak bersuara.
     *
     * ID tidak ikut berubah saat nama berubah. Itu SATU-SATUNYA alasan kita
     * pakai angka di sini, dan alasan itu cukup.
     *
     * Perhatikan tiga hal kecil yang sebenarnya besar:
     *
     * 1. Penjaga `isBlank()` IKUT DISALIN dan wajib bertahan. Jebakannya persis
     *    sama seperti pada Muscle: gerakan tanpa alat menyimpan teks kosong, dan
     *    `"".split(",")` di Kotlin menghasilkan list berisi SATU string kosong,
     *    bukan list kosong. Tanpa penjaga ini hampir semua gerakan bodyweight-mu
     *    bikin app crash saat katalog dibuka.
     *
     * 2. `toLongOrNull()` menggantikan peran `muscleOrNull` di atas, dan
     *    alasannya sama: teks yang tidak masuk akal cuma DIABAIKAN, tidak
     *    melempar exception. Bonusnya nyata -- kalau suatu hari ada satu baris
     *    yang entah bagaimana masih menyimpan "FLOOR_MAT", `toLongOrNull` cuma
     *    menjatuhkannya. `toLong()` biasa akan melempar NumberFormatException
     *    dari dalam perut Room, dan crash dari dalam kode yang dibangkitkan KSP
     *    itu salah satu yang paling susah dilacak di Android.
     *
     * 3. File ini TIDAK TAHU alat mana yang masih ada di tabel, dan memang tidak
     *    boleh tahu -- converter dipanggil Room di tengah proses membaca baris,
     *    dia tidak boleh balik bertanya ke database. Jadi ID yang alatnya sudah
     *    kamu hapus akan tetap keluar dari sini sebagai angka. Yang membuang ID
     *    yatim itu tugas LAYAR, dengan `mapNotNull { peta[it] }`. Aturan ini
     *    berlaku di seluruh app dan jangan dilanggar: satu tempat menerjemahkan
     *    teks jadi angka, tempat lain yang memutuskan angka itu masih berarti
     *    atau tidak.
     */
    @TypeConverter
    fun fromEquipmentIds(value: List<Long>): String =
        value.joinToString(",")

    @TypeConverter
    fun toEquipmentIds(value: String): List<Long> {
        if (value.isBlank()) return emptyList()

        return value.split(",").mapNotNull { raw -> raw.trim().toLongOrNull() }
    }

    // ---------- Enum tunggal <-> teks ----------

    @TypeConverter
    fun fromCategory(value: ExerciseCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): ExerciseCategory =
        // Alasan yang sama dengan di atas: kategori tak dikenal jangan bikin
        // crash. Jatuhkan ke SKILL supaya gerakannya tetap kelihatan dan bisa
        // kamu perbaiki sendiri dari layar edit.
        ExerciseCategory.entries.firstOrNull { it.name == value } ?: ExerciseCategory.SKILL

    @TypeConverter
    fun fromType(value: ExerciseType): String = value.name

    @TypeConverter
    fun toType(value: String): ExerciseType =
        ExerciseType.entries.firstOrNull { it.name == value } ?: ExerciseType.REPS
}
