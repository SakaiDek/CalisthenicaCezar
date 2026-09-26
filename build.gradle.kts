plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    // Compose compiler = plugin Kotlin sejak Kotlin 2.0. Versinya HARUS sama
    // dengan versi Kotlin di atas, kalau beda build langsung nolak.
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    // KSP = Kotlin Symbol Processing. Ini "tukang" yang membaca anotasi Room
    // (@Entity, @Dao) lalu MENULIS kode SQL-nya untuk kita saat build.
    // Nomor versinya dua bagian: "2.0.20" harus sama persis dengan versi Kotlin
    // di atas, "-1.0.25" itu nomor rilis KSP sendiri. Kalau bagian depan beda,
    // build langsung nolak.
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
}