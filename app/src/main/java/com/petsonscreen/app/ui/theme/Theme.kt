package com.petsonscreen.app.ui.theme

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
import androidx.compose.ui.unit.sp
import com.example.R

object PetColors {
    val CanvasBackground = Color(0xFFF5F6F8)
    val CardSurface = Color(0xFFFFFFFF)
    val HairlineBorder = Color(0xFFE4E7EC)
    val StageBackground = Color(0xFFECEEF2)
    val ObsidianInk = Color(0xFF111318)
    val SecondaryMuted = Color(0xFF5B6475)
    val SubtleNumbers = Color(0xFF9CA3AF)
    val ChampagneGold = Color(0xFFB68D40)
    val ChampagneTint = Color(0xFFFBF7EE)
    val ChampagneBorder = Color(0xFFE6D6B5)
    val DestructiveRed = Color(0xFFC93B43)
    val DestructiveTint = Color(0xFFFDF2F2)
}

val EditorialSerifFamily = FontFamily(
    Font(R.font.fraunces, FontWeight.Normal),
    Font(R.font.fraunces, FontWeight.SemiBold),
    Font(R.font.fraunces, FontWeight.Bold)
)

val CleanSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val TabularNumberStyle = TextStyle(
    fontFamily = CleanSansFamily,
    fontFeatureSettings = "tnum"
)

private val EditorialColorScheme = lightColorScheme(
    primary = PetColors.ObsidianInk,
    onPrimary = Color.White,
    primaryContainer = PetColors.ChampagneTint,
    onPrimaryContainer = PetColors.ObsidianInk,
    secondary = PetColors.ChampagneGold,
    onSecondary = Color.White,
    secondaryContainer = PetColors.ChampagneTint,
    onSecondaryContainer = PetColors.ObsidianInk,
    tertiary = PetColors.ChampagneGold,
    background = PetColors.CanvasBackground,
    onBackground = PetColors.ObsidianInk,
    surface = PetColors.CardSurface,
    onSurface = PetColors.ObsidianInk,
    surfaceVariant = PetColors.StageBackground,
    onSurfaceVariant = PetColors.SecondaryMuted,
    outline = PetColors.HairlineBorder,
    outlineVariant = PetColors.ChampagneBorder,
    error = PetColors.DestructiveRed,
    onError = Color.White
)

val EditorialTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = EditorialSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
        color = PetColors.ObsidianInk
    ),
    headlineMedium = TextStyle(
        fontFamily = EditorialSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
        color = PetColors.ObsidianInk
    ),
    headlineSmall = TextStyle(
        fontFamily = EditorialSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
        color = PetColors.ObsidianInk
    ),
    titleLarge = TextStyle(
        fontFamily = EditorialSerifFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = PetColors.ObsidianInk
    ),
    titleMedium = TextStyle(
        fontFamily = CleanSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = PetColors.ObsidianInk
    ),
    titleSmall = TextStyle(
        fontFamily = CleanSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = PetColors.ObsidianInk
    ),
    bodyLarge = TextStyle(
        fontFamily = CleanSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = PetColors.ObsidianInk
    ),
    bodyMedium = TextStyle(
        fontFamily = CleanSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = PetColors.SecondaryMuted
    ),
    bodySmall = TextStyle(
        fontFamily = CleanSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = PetColors.SecondaryMuted
    ),
    labelLarge = TextStyle(
        fontFamily = CleanSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = CleanSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = CleanSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)

val EditorialShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun PetsOnScreenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EditorialColorScheme,
        typography = EditorialTypography,
        shapes = EditorialShapes,
        content = content
    )
}
