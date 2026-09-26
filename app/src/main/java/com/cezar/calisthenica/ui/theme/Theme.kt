package com.cezar.calisthenica.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    outlineVariant = LightOutlineVariant,
    scrim = LightScrim,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,

    /*
     * ATURAN YANG SAYA MINTA KAMU PEGANG SETELAH INI, dan ini pelajaran paling
     * mahal dari sesi hari ini:
     *
     *   Slot warna M3 yang TIDAK kamu isi tidak jadi kosong -- dia jadi MILIK
     *   ORANG LAIN.
     *
     * Sebelum baris-baris di bawah ada, `darkColorScheme()` mengisi sendiri
     * semua slot yang kita lewatkan dengan palet baku Material 3. Palet baku itu
     * ungu. Jadi identitas oranye-hitam yang kita ukur dari blueprint hanya
     * berlaku di tempat yang kita sebut namanya; sisanya diam-diam ungu.
     *
     * `Card` M3 tidak memakai `surface`, dia memakai `surfaceContainerLow`.
     * Snackbar memakai `inverseSurface`. Tombol hapus memakai `error`. Divider
     * memakai `outlineVariant`. Empat slot itu tidak pernah kita isi -- dan
     * empat-empatnya kelihatan di layar HP-mu.
     *
     * Cara memeriksanya nanti tanpa alat apa pun: cari nama slot di dokumentasi
     * komponen yang mau kamu pakai, lalu pastikan namanya ada di daftar ini.
     * Kalau tidak ada, kamu sedang memakai warna orang lain.
     */
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    outlineVariant = DarkOutlineVariant,
    scrim = DarkScrim,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
)

/**
 * Tema tunggal untuk seluruh app.
 *
 * KEPUTUSAN DIBALIK, 5 September 2026 -- baca ini sebelum kamu bertanya-tanya
 * kenapa warna app-mu tidak ikut wallpaper lagi.
 *
 * Sampai kemarin `dynamicColor` di sini bernilai `true`, artinya di Android 12+
 * warna app diambil dari wallpaper HP ("Material You"). Itu keputusan yang kita
 * setujui bersama 4 September 2026 dan tercatat di CLAUDE.md. Hari ini kamu
 * memberi perintah yang mengalahkannya: "Ikuti persis standar alur visual
 * blueprint yang sudah gue buat."
 *
 * Dua hal itu tidak bisa jalan bersamaan, dan ini alasannya -- bukan selera,
 * tapi aritmatika: Material You MENGHITUNG palet dari wallpaper. Wallpaper-mu
 * kebiruan, jadi app-mu keluar teal di atas biru-abu. Blueprint-mu hitam dengan
 * oranye #FB8D05. Selama dynamicColor menyala, blueprint-mu MUSTAHIL muncul di
 * layar, karena wallpaper selalu menang. Jadi salah satu harus mengalah, dan
 * yang mengalah adalah yang bukan permintaanmu.
 *
 * Kalau suatu hari kamu berubah pikiran, ongkosnya satu kata: panggil
 * `CalisthenicaTheme(dynamicColor = true)` di MainActivity. Tidak ada yang perlu
 * dihapus. Saya sengaja tidak membuang jalannya -- keputusan desain yang
 * dibalik itu wajar, keputusan desain yang jalan keluarnya ditutup itu ceroboh.
 */
@Composable
fun CalisthenicaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
    ) {
        /*
         * OBAT PERTAMA UNTUK "KILATAN ABU-ABU", dan pelajaran penting soal
         * cara kerja Compose.
         *
         * `MaterialTheme` TIDAK MENGGAMBAR APA PUN. Namanya menipu: dia cuma
         * kotak arsip berisi nilai -- warna, huruf, bentuk -- yang bisa dibaca
         * komponen di bawahnya. Nol piksel. Kalau isi app-mu tidak menutupi
         * seluruh layar, yang terlihat di sisa layar itu adalah jendela Android
         * di belakangnya, bukan warna tema.
         *
         * Kapan isi app tidak menutupi layar? Tepat satu frame saat kamu ganti
         * layar. Katalog dibongkar, layar detail belum selesai diukur, dan di
         * celah itu tidak ada siapa-siapa yang menggambar. Yang muncul adalah
         * warna jendela -- yang di tema Android bawaan bernilai #303030, alias
         * ABU-ABU. Itu kilatan yang kamu lihat, dan itu bukan halusinasi.
         *
         * `Surface` ini menambal celahnya: dia satu lapis warna seukuran layar
         * penuh yang hidup di LUAR pergantian layar, jadi dia tidak pernah ikut
         * dibongkar. Mau layar mana pun sedang bertukar, di belakangnya selalu
         * ada hitam -- bukan lubang.
         *
         * Obat KEDUA ada di res/values-night/themes.xml, karena celah yang sama
         * juga menganga sebelum Compose menggambar frame pertamanya. Satu
         * penyakit, dua pintu masuk; dua-duanya harus ditutup.
         */
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.background,
            content = content,
        )
    }
}
