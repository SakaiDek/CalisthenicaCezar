plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Wajib sejak Kotlin 2.0: compiler Compose sekarang plugin Kotlin terpisah,
    // bukan lagi "composeOptions { kotlinCompilerExtensionVersion = ... }".
    id("org.jetbrains.kotlin.plugin.compose")
    // Dipakai Room untuk generate kode. Kita pakai KSP, BUKAN KAPT.
    // Bedanya penting buat laptopmu: KAPT harus bikin stub Java dulu dari
    // semua file Kotlin (lambat, rakus RAM), KSP baca Kotlin langsung.
    // Di mesin 8GB, ini bukan sekadar "lebih rapi" -- ini yang bikin build
    // tetap belasan detik, bukan semenit.
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.cezar.calisthenica"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cezar.calisthenica"
        // minSdk 26 dipilih sengaja: java.time (LocalDate) jalan native tanpa
        // "core library desugaring". Nanti fitur kalender & streak jadi jauh
        // lebih gampang dan build lebih ringan.
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false

            /*
             * DITAMBAHKAN 5 Sept 2026, dan ini satu-satunya perubahan kode
             * hari ini. Tujuannya bukan "menyiapkan app untuk Play Store" --
             * masih jauh. Tujuannya MENGUKUR.
             *
             * Masalahnya: kamu bilang scroll patah-patah. Saya ukur rekamanmu
             * dan yang paling parah justru DASBOR -- layar tanpa satu pun
             * gambar. Kalau layar tanpa gambar juga tersendat, tersangkanya
             * bukan gambar dan bukan daftar, tapi sesuatu yang berlaku di
             * semua layar. Dan tersangka nomor satu itu: APK debug.
             *
             * Android tidak bisa memasang APK yang tidak ditandatangani.
             * Tanpa baris ini, `assembleRelease` memang jalan tapi hasilnya
             * app-release-UNSIGNED.apk yang ditolak HP-mu, dan task
             * `installRelease` bahkan TIDAK DIBUAT oleh Gradle.
             *
             * Kenapa memakai kunci DEBUG untuk build rilis -- padahal itu
             * terdengar salah:
             *   1. Kunci debug dibuat otomatis oleh Android SDK dan ada di
             *      ~/.android/debug.keystore. Tidak ada file rahasia baru yang
             *      harus kamu simpan atau takut hilang, dan tidak ada password
             *      yang tertulis di dalam repo ini.
             *   2. Karena kuncinya SAMA dengan APK debug, Android menganggap
             *      keduanya app yang sama. Jadi APK rilis bisa dipasang
             *      menimpa yang debug (`install -r`) tanpa menghapus database
             *      Room-mu. Program dan gerakan yang sudah kamu isi selamat.
             *      Kalau saya bikin kunci baru, Android menolak install dengan
             *      INSTALL_FAILED_UPDATE_INCOMPATIBLE dan kamu harus uninstall
             *      dulu -- artinya semua datamu hilang cuma untuk satu tes.
             *
             * WAJIB DIGANTI SEBELUM RILIS SUNGGUHAN. Kunci debug dipegang
             * siapa pun yang punya Android SDK, jadi APK bertanda kunci ini
             * tidak boleh keluar dari HP-mu. Play Store juga menolaknya
             * mentah-mentah. Nanti kita bikin keystore sendiri + variabel
             * lingkungan untuk passwordnya, bukan ditulis di file ini.
             */
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        // Matikan yang tidak dipakai -> build lebih cepat, RAM lebih lega.
        buildConfig = false
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Versi Compose dipin eksplisit dulu (bukan BOM) supaya tidak ada
    // kejutan resolusi versi di build pertama. Nanti kita rapikan pakai BOM.
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.ui:ui-graphics:1.7.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.0")
    implementation("androidx.compose.material3:material3:1.3.0")
    // Ikon bawaan Material (kita cuma butuh Icons.Default.Add untuk tombol +).
    // Sebenarnya ikut terbawa lewat material3, tapi kita pin eksplisit supaya
    // build tidak bergantung pada dependency turunan orang lain.
    // CATATAN: pakai "-core", JANGAN "-extended". Yang extended isinya ribuan
    // ikon dan bikin ukuran APK melar tanpa alasan.
    implementation("androidx.compose.material:material-icons-core:1.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling:1.7.0")

    // ============ Room: database SQLite lokal ============
    // room-runtime  : mesinnya, yang dipakai saat app jalan.
    // room-ktx      : dukungan coroutine + Flow (biar daftar program otomatis
    //                 ikut berubah di layar begitu ada data baru masuk).
    // room-compiler : cuma hidup saat BUILD. Dia yang baca @Dao milikmu lalu
    //                 menulis kode SQL-nya. Pakai `ksp(...)`, bukan
    //                 `implementation(...)`, karena tidak perlu ikut masuk APK.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Jembatan ViewModel <-> Compose. Kita butuh ini supaya data dari Room
    // tidak hilang saat HP diputar atau layar sempat ditutup.
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // ============ Coil: penampil gambar untuk Compose ============
    // INI SATU-SATUNYA TEBAKAN BUTA di build hari ini. Semua kode lain di
    // perubahan ini memakai API yang sudah terbukti jalan di HP-mu semalam.
    // Jadi kalau build merah, tersangkanya cuma satu baris: yang ini.
    //
    // Kenapa butuh library sama sekali untuk "menampilkan gambar"? Karena
    // membaca file JPG 4000x3000 dari galeri lalu menaruhnya di kartu selebar
    // 80dp itu bukan sekadar "buka file". Yang harus terjadi: baca ukurannya
    // dulu tanpa memuat isinya, hitung faktor pengecilan, decode versi kecilnya
    // saja, simpan di cache memori supaya tidak di-decode ulang tiap kali kamu
    // scroll, dan batalkan pekerjaannya kalau kartunya sudah lewat dari layar.
    // Kalau ditulis sendiri, itu bukan satu sore -- dan kalau salah satu langkah
    // terlewat, hadiahnya OutOfMemoryError di HP.
    //
    // Coil, bukan Glide: Coil ditulis Kotlin + coroutine dan punya AsyncImage
    // untuk Compose secara asli. Glide lebih tua, lebih besar, dan
    // integrasi Compose-nya masih tempelan.
    implementation("io.coil-kt:coil-compose:2.7.0")
}
