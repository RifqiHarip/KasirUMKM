plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Hapus baris kapt, gunakan KSP (Sesuaikan versi Kotlin Anda, misal 2.0.0 atau 2.0.21)
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}