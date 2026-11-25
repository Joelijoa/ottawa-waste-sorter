package com.ottawa.wastesorter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ottawa.wastesorter.R

/** Jetons de couleur du design « Ottawa Waste Sorter — Android app design ». */
object Ows {
    val Green = Color(0xFF23845A)          // Primary green : actions principales, bac vert
    val GreenDark = Color(0xFF1B6B49)
    val GreenSoft = Color(0xFFDDF2E5)      // Secondary green : sélection, notes d'info
    val GreenInk = Color(0xFF1F3B2C)
    val Blue = Color(0xFF3478D4)           // Bac bleu uniquement
    val Charcoal = Color(0xFF25282B)       // Texte, bac noir
    val Background = Color(0xFFF7F8F6)
    val Surface = Color(0xFFFFFFFF)
    val Muted = Color(0xFF777E79)          // Méta, placeholders
    val Secondary = Color(0xFF5C635F)      // Texte secondaire
    val Border = Color(0xFFE3E6E1)
    val Divider = Color(0xFFECEEEA)
    val Outline = Color(0xFFC9CEC8)        // Contour des boutons secondaires
    val Warning = Color(0xFFD99A35)
    val WarningSoft = Color(0xFFFBF1E0)
    val WarningIcon = Color(0xFFB07A1F)
    val WarningInk = Color(0xFF5A3E0E)
    val WarningInk2 = Color(0xFF6B4A12)
    val Danger = Color(0xFFB4442F)
    val GarbageGrey = Color(0xFFE4E6E2)
    val BarOff = Color(0xFFDDE1DC)
    val Mint = Color(0xFF8FD3AE)           // Accents du snackbar
    val Disabled = Color(0xFF9AA09B)
    val Camera = Color(0xFF111314)
}

val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
    Font(R.font.figtree_extrabold, FontWeight.ExtraBold),
)

/** Échelle typographique du design (Figtree). */
object OwsType {
    val Display = TextStyle(fontFamily = Figtree, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em, lineHeight = 33.sp)
    val ScreenTitle = TextStyle(fontFamily = Figtree, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em, lineHeight = 32.sp)
    val BinLabel = TextStyle(fontFamily = Figtree, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.01.em, lineHeight = 30.sp)
    val CardTitle = TextStyle(fontFamily = Figtree, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em, lineHeight = 27.sp)
    val Section = TextStyle(fontFamily = Figtree, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp)
    val Body = TextStyle(fontFamily = Figtree, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 23.sp)
    val BodySmall = TextStyle(fontFamily = Figtree, fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp)
    val Meta = TextStyle(fontFamily = Figtree, fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    val Label = TextStyle(fontFamily = Figtree, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em, lineHeight = 17.sp)
    val Button = TextStyle(fontFamily = Figtree, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
}

private val Colors = lightColorScheme(
    primary = Ows.Green,
    onPrimary = Color.White,
    primaryContainer = Ows.GreenSoft,
    onPrimaryContainer = Ows.GreenDark,
    secondary = Ows.Secondary,
    secondaryContainer = Ows.GreenSoft,
    onSecondaryContainer = Ows.GreenDark,
    background = Ows.Background,
    onBackground = Ows.Charcoal,
    surface = Ows.Surface,
    onSurface = Ows.Charcoal,
    surfaceVariant = Ows.Divider,
    onSurfaceVariant = Ows.Secondary,
    surfaceContainer = Ows.Surface,
    surfaceContainerLow = Ows.Surface,
    surfaceContainerHigh = Ows.Surface,
    outline = Ows.Outline,
    outlineVariant = Ows.Border,
    error = Ows.Danger,
)

private val AppTypography = Typography().let { base ->
    Typography(
        displaySmall = base.displaySmall.copy(fontFamily = Figtree),
        headlineLarge = OwsType.Display,
        headlineMedium = OwsType.ScreenTitle,
        headlineSmall = OwsType.CardTitle,
        titleLarge = OwsType.CardTitle,
        titleMedium = OwsType.Section,
        titleSmall = OwsType.Section.copy(fontSize = 16.sp),
        bodyLarge = OwsType.Body,
        bodyMedium = OwsType.BodySmall,
        bodySmall = OwsType.Meta,
        labelLarge = OwsType.Button.copy(fontSize = 15.sp),
        labelMedium = OwsType.Label,
        labelSmall = OwsType.Meta.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Le design est défini en clair uniquement (fond #F7F8F6) : l'application garde ce thème
 * même lorsque le téléphone est en mode sombre, pour que les couleurs des bacs restent identiques.
 */
@Composable
fun WasteSorterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, shapes = AppShapes, content = content)
}
