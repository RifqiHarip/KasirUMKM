package com.rifqi.kasirumkm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- PALET WARNA CASH LOGGER (BLACK, DARK GREEN & GOLD) ---
private val EmeraldPrimary = Color(0xFF2E7D32)      // Hijau Tua Klasik / Emerald
private val EmeraldPrimaryContainer = Color(0xFF1B5E20) // Hijau Tua Lebih Gelap
private val GoldAccent = Color(0xFFFFD700)          // Emas (Gold) untuk Aksen Penting
private val DarkBackground = Color(0xFF121212)      // Hitam Pekat Android
private val DarkSurface = Color(0xFF1E1E1E)         // Hitam Permukaan Card / Dialog
private val DarkSurfaceVariant = Color(0xFF2A2A2A)  // Hitam Varian untuk TopBar / BottomBar
private val OnDarkPrimary = Color(0xFFFFFFFF)       // Teks putih bersih di atas hijau
private val OnDarkSurface = Color(0xFFE0E0E0)       // Teks abu-abu terang yang nyaman di mata

private val CashLoggerColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnDarkPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = GoldAccent,
    onSecondary = Color(0xFF000000), // Teks hitam di atas tombol gold
    background = DarkBackground,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB0B0B0)
)

@Composable
fun KasirUMKMTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CashLoggerColorScheme,
        typography = Typography,
        content = content
    )
}