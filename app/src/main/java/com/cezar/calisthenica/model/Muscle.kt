package com.cezar.calisthenica.model

/**
 * Daftar otot yang bisa ditandai per gerakan.
 *
 * KENAPA ini enum dan bukan sekadar teks bebas?
 * Kalau otot cuma String, cepat atau lambat kamu akan punya "Latissimus",
 * "latissimus", "Lats", dan "Lat" sebagai empat hal berbeda di database yang
 * sama. Diagram anatomi nanti tidak akan tahu harus menyalakan yang mana.
 * Dengan enum, compiler yang jadi penjaga pintu: salah tulis = build merah,
 * bukan data kotor yang baru ketahuan enam bulan kemudian.
 *
 * Nama ototnya sengaja saya biarkan istilah Inggris. Bukan karena gengsi --
 * karena di lapangan tidak ada yang bilang "lengan bawah", semua bilang
 * "forearm". Judul kelompoknya yang Indonesia, karena itu cuma label rak.
 *
 * PELAJARAN HARI INI, dan ini pelajaran GRATIS yang saya siapkan semalam
 * tanpa memberitahumu kenapa: menambah nilai enum di sini NOL BIAYA. Tidak
 * ada migration, tidak ada versi database naik, tidak ada data lama rusak.
 * Alasannya ada dua, dan dua-duanya keputusan sadar:
 *
 * 1. Yang tersimpan di kolom `muscles` cuma NAMA-nya sebagai teks
 *    ("UPPER_TRAPS,UPPER_BACK"), bukan nomor urutnya. Jadi saya boleh
 *    menyelipkan LOWER_TRAPS di tengah daftar tanpa menggeser arti baris
 *    yang sudah kamu simpan. Kalau dulu saya simpan nomor urut, menyelipkan
 *    satu otot akan mengubah "Upper Back" jadi "Lower Traps" di semua data
 *    lamamu secara diam-diam. Itu jenis bug yang bikin orang berhenti ngoding.
 * 2. Converters.kt membaca pakai `firstOrNull { it.name == name }`, bukan
 *    `valueOf`. Jadi nama yang tidak dikenal cuma dilewati, tidak bikin crash.
 *
 * Dan karena yang disimpan `.name` sedangkan yang tampil `.label`, saya baru
 * saja mengganti tulisan "Chest" jadi "Mid Chest" di layarmu TANPA menyentuh
 * satu baris pun data. Itu gunanya dua kolom dipisah sejak awal.
 */
enum class Muscle(
    val label: String,
    val region: BodyRegion,
    val view: BodyView,
) {
    // ================= Leher & punggung =================
    // Trapezius itu SATU otot besar dengan tiga arah tarikan yang beda kerja,
    // dan di kalistenik bedanya bukan teori. Contoh dari rekamanmu sendiri:
    // "Scapular Pull up" yang baru kamu daftarkan itu latihan LOWER TRAPS --
    // gerakannya menurunkan dan menahan tulang belikat. Kamu menandainya
    // Upper Traps + Upper Back semalam karena Lower Traps belum ada. Upper
    // Traps itu justru yang ANGKAT bahu (shrug), arah kebalikannya. Kamu
    // benar, dan daftar saya yang kurang.
    NECK("Neck", BodyRegion.BACK_NECK, BodyView.BOTH),
    UPPER_TRAPS("Upper Traps", BodyRegion.BACK_NECK, BodyView.BOTH),
    MID_TRAPS("Mid Traps", BodyRegion.BACK_NECK, BodyView.BACK),
    LOWER_TRAPS("Lower Traps", BodyRegion.BACK_NECK, BodyView.BACK),
    RHOMBOIDS("Rhomboids", BodyRegion.BACK_NECK, BodyView.BACK),

    // UPPER_BACK saya PERTAHANKAN walau sekarang tumpang tindih dengan Mid
    // Traps dan Rhomboids. Bukan karena rapi, tapi karena satu gerakan di HP-mu
    // sudah memakainya. Menghapusnya berarti data pertamamu kehilangan penanda.
    // Anggap ini rak "punggung atas, belum dipecah".
    UPPER_BACK("Upper Back", BodyRegion.BACK_NECK, BodyView.BACK),

    // Teres major = "lats kecil". Wajib ada di app kalistenik: dia ikut kerja
    // di semua tarikan lengan dari atas ke bawah.
    TERES_MAJOR("Teres Major", BodyRegion.BACK_NECK, BodyView.BACK),
    LATISSIMUS("Latissimus", BodyRegion.BACK_NECK, BodyView.BACK),
    LOWER_BACK("Lower Back", BodyRegion.BACK_NECK, BodyView.BACK),

    // ================= Bahu =================
    // Dipecah tiga, bukan satu "Shoulder". Untuk kalistenik bedanya nyata:
    // front lever itu kerja rear delt, handstand itu front delt. Kalau kamu
    // mau digabung jadi satu, sekarang gratis; nanti setelah ada 40 gerakan
    // tercatat, menggabungkannya berarti menyunting data.
    FRONT_DELT("Front Delt", BodyRegion.SHOULDER, BodyView.FRONT),
    SIDE_DELT("Side Delt", BodyRegion.SHOULDER, BodyView.BOTH),
    REAR_DELT("Rear Delt", BodyRegion.SHOULDER, BodyView.BACK),

    // Rotator cuff tidak kelihatan di kaca dan tidak bikin bangga, tapi dia
    // yang bikin kamu masih bisa latihan lima tahun lagi. Pantas dapat chip.
    ROTATOR_CUFF("Rotator Cuff", BodyRegion.SHOULDER, BodyView.BACK),

    // ================= Dada & perut =================
    // Pec major punya dua serat dengan arah beda: yang atas (clavicular) kerja
    // saat mendorong ke atas-depan, yang bawah (sternal) saat dip. Makanya
    // dipecah tiga.
    UPPER_CHEST("Upper Chest", BodyRegion.TORSO, BodyView.FRONT),
    CHEST("Mid Chest", BodyRegion.TORSO, BodyView.FRONT),
    LOWER_CHEST("Lower Chest", BodyRegion.TORSO, BodyView.FRONT),

    // Serratus anterior: kamu benar, ini kelalaian yang paling serius dari
    // daftar semalam. Dialah yang mendorong tulang belikat MAJU dan memutarnya
    // ke atas -- artinya dia otot utama di planche, handstand, dan push-up plus.
    // App kalistenik tanpa serratus itu seperti app lari tanpa betis.
    SERRATUS_ANTERIOR("Serratus Anterior", BodyRegion.TORSO, BodyView.FRONT),
    ABS("Abs", BodyRegion.TORSO, BodyView.FRONT),
    OBLIQUES("Obliques", BodyRegion.TORSO, BodyView.FRONT),

    // ================= Lengan =================
    BICEPS("Biceps", BodyRegion.ARM, BodyView.FRONT),

    // Brachialis duduk DI BAWAH biceps dan dialah yang bikin lengan kelihatan
    // tebal dari samping. Kerjanya paling keras di grip netral/hammer.
    BRACHIALIS("Brachialis", BodyRegion.ARM, BodyView.BOTH),
    TRICEPS("Triceps", BodyRegion.ARM, BodyView.BACK),
    FOREARM("Forearm", BodyRegion.ARM, BodyView.BOTH),

    // ================= Kaki =================
    GLUTES("Glutes", BodyRegion.LEG, BodyView.BACK),

    // Hip flexor saya taruh di Kaki walau latihannya terasa seperti perut.
    // Ini yang bekerja saat leg raise dan L-sit, termasuk tahan leg-up-the-wall
    // 160 detik yang pernah kamu sebut. Tanpa chip ini, semua gerakan angkat
    // kaki cuma tertulis "Abs" -- dan itu bohong setengah.
    HIP_FLEXORS("Hip Flexors", BodyRegion.LEG, BodyView.FRONT),

    // Adductor = paha dalam. Wajib untuk straddle, middle split, dan straddle
    // planche.
    ADDUCTORS("Adductors", BodyRegion.LEG, BodyView.FRONT),
    QUADRICEPS("Quadriceps", BodyRegion.LEG, BodyView.FRONT),
    HAMSTRINGS("Hamstrings", BodyRegion.LEG, BodyView.BACK),
    CALVES("Calves", BodyRegion.LEG, BodyView.BOTH),
    TIBIALIS("Tibialis", BodyRegion.LEG, BodyView.FRONT),
}

/**
 * Kelompok otot, dipakai sebagai judul rak saat menampilkan chip pilihan.
 * Tiga puluh chip berjejer tanpa pengelompokan itu dinding, bukan pilihan --
 * dan sekarang jumlahnya memang tiga puluh, jadi rak ini bukan hiasan lagi.
 */
enum class BodyRegion(val label: String) {
    BACK_NECK("Leher & Punggung"),
    SHOULDER("Bahu"),
    TORSO("Dada & Perut"),
    ARM("Lengan"),
    LEG("Kaki"),
}

/**
 * Otot ini kelihatan dari sisi mana.
 *
 * Belum ada satu pun kode yang memakai ini hari ini -- dan itu memang niatnya.
 * Ini titipan untuk diagram anatomi nanti: figur depan hanya menggambar otot
 * FRONT dan BOTH, figur belakang hanya BACK dan BOTH. Saya taruh sekarang
 * karena menambah properti ke enum itu gratis, sedangkan membongkar enum yang
 * sudah tersimpan di ribuan baris database itu tidak.
 */
enum class BodyView { FRONT, BACK, BOTH }
