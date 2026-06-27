package com.deckmaster.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Deep mystic color palette
val DeepPurple      = Color(0xFF1A0A2E)
val MidnightBlue    = Color(0xFF0D1B2A)
val CardGold        = Color(0xFFD4AF37)
val CardGoldLight   = Color(0xFFF5D97A)
val HeartsRed       = Color(0xFFE53935)
val ClubsGreen      = Color(0xFF2E7D32)
val DiamondsBlue    = Color(0xFF1565C0)
val SpadesBlack     = Color(0xFF212121)
val NullCard        = Color(0xFF444444)
val CardBack        = Color(0xFF1A2A5E)   // deep navy — face-down card back
val CardBackPattern = Color(0xFF3A4A7E)   // subtle hex pattern on card back
val SurfaceDark     = Color(0xFF1E1E2E)
val SurfaceMid      = Color(0xFF2A2A3E)
val OnSurface       = Color(0xFFE0E0E0)
val PlanetGlow      = Color(0xFFFFD700)
val RulerGlow       = Color(0xFFFF8C00)

private val DarkColorScheme = darkColorScheme(
    primary          = CardGold,
    onPrimary        = Color(0xFF1A0A2E),
    secondary        = Color(0xFF9C27B0),
    onSecondary      = Color.White,
    tertiary         = Color(0xFF00BCD4),
    background       = MidnightBlue,
    onBackground     = OnSurface,
    surface          = SurfaceDark,
    onSurface        = OnSurface,
    surfaceVariant   = SurfaceMid,
    onSurfaceVariant = Color(0xFFB0B0C0)
)

@Composable
fun DeckMasterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
