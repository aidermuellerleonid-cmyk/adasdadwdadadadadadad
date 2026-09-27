package de.fitapp.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Schwarz/Blau-Farbschema (immer dunkel, unabhängig vom Systemdesign), mit etwas mehr
// Tiefe/Kontrast zwischen den Oberflächen-Ebenen für einen moderneren, "geschichteten" Look.
private val Blue = Color(0xFF2F9BFF)
private val BlueLight = Color(0xFF7BC4FF)
private val BlueDark = Color(0xFF0D47A1)
private val Teal = Color(0xFF35D6C0)
private val TealDark = Color(0xFF0B3B36)
private val NearBlack = Color(0xFF07090D)
private val SurfaceBlack = Color(0xFF12151C)
private val SurfaceVariantBlack = Color(0xFF1B212B)
private val SurfaceHighBlack = Color(0xFF232A36)
private val ErrorRed = Color(0xFFFF6B6B)

private val DarkColors = darkColorScheme(
    primary = BlueLight,
    onPrimary = Color(0xFF00274D),
    primaryContainer = BlueDark,
    onPrimaryContainer = Color.White,
    secondary = Teal,
    onSecondary = Color(0xFF00332D),
    secondaryContainer = TealDark,
    onSecondaryContainer = Color(0xFFC8FFF4),
    tertiary = Blue,
    onTertiary = Color.White,
    tertiaryContainer = BlueDark,
    onTertiaryContainer = Color.White,
    background = NearBlack,
    onBackground = Color(0xFFE7EAF0),
    surface = SurfaceBlack,
    onSurface = Color(0xFFE7EAF0),
    surfaceVariant = SurfaceVariantBlack,
    onSurfaceVariant = Color(0xFFAEB8C6),
    surfaceTint = BlueLight,
    inverseSurface = Color(0xFFE7EAF0),
    inverseOnSurface = Color(0xFF12151C),
    outline = Color(0xFF454E5D),
    outlineVariant = Color(0xFF2A313D),
    error = ErrorRed,
    onError = Color(0xFF3B0000),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD4)
)

// Etwas großzügiger abgerundete Ecken für Karten, Buttons, Dialoge & Textfelder –
// wirkt weicher/moderner, wirkt sich automatisch auf die ganze App aus.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Etwas kräftigere Überschriften (SemiBold statt Regular) für mehr visuelle Hierarchie.
private val BaseTypography = Typography()
private val AppTypography = BaseTypography.copy(
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
)

@Composable
fun FitAppTheme(content: @Composable () -> Unit) {
    // Bewusst immer dunkel (Schwarz/Blau/Türkis), unabhängig vom Systemdesign.
    MaterialTheme(colorScheme = DarkColors, shapes = AppShapes, typography = AppTypography, content = content)
}

val BrandBlueDark = BlueDark
