package com.momentummm.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Momentum palette.
 *
 * Organizada en rampas para que cualquier pantalla pueda pedir el tono exacto que
 * necesita en vez de inventar hex sueltos. La identidad es indigo -> violeta, con
 * una neutral fría que permite superficies muy oscuras sin que se vean "sucias".
 */

// ---------------------------------------------------------------- Brand: Indigo
val Indigo50 = Color(0xFFEEF0FF)
val Indigo100 = Color(0xFFE0E3FF)
val Indigo200 = Color(0xFFC4C9FF)
val Indigo300 = Color(0xFFA5ABFF)
val Indigo400 = Color(0xFF8388FA)
val Indigo500 = Color(0xFF6366F1)
val Indigo600 = Color(0xFF4F46E5)
val Indigo700 = Color(0xFF4338CA)
val Indigo800 = Color(0xFF3730A3)
val Indigo900 = Color(0xFF272377)

// --------------------------------------------------------------- Accent: Violet
val Violet200 = Color(0xFFDDD6FE)
val Violet300 = Color(0xFFC4B5FD)
val Violet400 = Color(0xFFA78BFA)
val Violet500 = Color(0xFF8B5CF6)
val Violet600 = Color(0xFF7C3AED)
val Violet900 = Color(0xFF3B1E7E)

// ------------------------------------------------------------- Semantic accents
val Mint300 = Color(0xFF6EE7B7)
val Mint400 = Color(0xFF34D399)
val Mint500 = Color(0xFF10B981)
val Mint600 = Color(0xFF059669)

val Amber300 = Color(0xFFFCD34D)
val Amber400 = Color(0xFFFBBF24)
val Amber500 = Color(0xFFF59E0B)

val Rose300 = Color(0xFFFDA4AF)
val Rose400 = Color(0xFFFB7185)
val Rose500 = Color(0xFFF43F5E)
val Rose600 = Color(0xFFE11D48)

val Sky300 = Color(0xFF7DD3FC)
val Sky400 = Color(0xFF38BDF8)
val Sky500 = Color(0xFF0EA5E9)

val Coral400 = Color(0xFFFB923C)
val Coral500 = Color(0xFFF97316)

// -------------------------------------------------------------- Neutral (slate)
val Neutral0 = Color(0xFFFFFFFF)
val Neutral50 = Color(0xFFF7F8FC)
val Neutral100 = Color(0xFFF0F2F8)
val Neutral200 = Color(0xFFE4E7F0)
val Neutral300 = Color(0xFFCDD2E0)
val Neutral400 = Color(0xFF9AA1B5)
val Neutral500 = Color(0xFF6C7489)
val Neutral600 = Color(0xFF4B5266)
val Neutral700 = Color(0xFF343A4B)
val Neutral800 = Color(0xFF232936)
val Neutral850 = Color(0xFF1A1F2B)
val Neutral900 = Color(0xFF12151F)
val Neutral950 = Color(0xFF0B0D14)

// ------------------------------------------------------- Roles: tema claro
val Primary = Indigo600
val PrimaryVariant = Indigo700
val Secondary = Mint500
val SecondaryVariant = Mint600
val Success = Mint500
val Warning = Amber500
val Error = Rose500

val BackgroundWhite = Neutral50
val BackgroundGray = Neutral100
val SurfaceLight = Neutral0
val SurfaceElevatedLight = Neutral0
val SurfaceSunkenLight = Neutral100
val OutlineLight = Neutral200

val TextPrimary = Neutral900
val TextSecondary = Neutral500
val TextTertiary = Neutral400

val WeeksLived = Indigo500
val WeeksFuture = Neutral200

// -------------------------------------------------------- Roles: tema oscuro
val PrimaryDark = Indigo400
val PrimaryVariantDark = Violet400
val SecondaryDark = Mint400
val SecondaryVariantDark = Mint500
val SuccessDark = Mint400
val WarningDark = Amber400
val ErrorDark = Rose400

val BackgroundDark = Neutral950
val BackgroundGrayDark = Neutral900
val SurfaceDark = Neutral900
val SurfaceElevatedDark = Neutral850
val SurfaceSunkenDark = Neutral950
val OutlineDark = Neutral800

val TextPrimaryDark = Color(0xFFF6F7FB)
val TextSecondaryDark = Neutral400
val TextTertiaryDark = Neutral500

val WeeksLivedDark = Indigo400
val WeeksFutureDark = Neutral800

// ------------------------------------------------------------------- Gradientes
val GradientStart = Indigo500
val GradientEnd = Violet500
val GradientStartDark = Indigo600
val GradientEndDark = Violet600

/** Colores por categoría de uso, para gráficas y anillos segmentados. */
val CategorySocial = Violet500
val CategoryEntertainment = Rose500
val CategoryProductivity = Mint500
val CategoryGames = Amber500
val CategoryCommunication = Sky500
val CategoryOther = Neutral400

/** Paleta ordenada para series de datos (charts, leaderboards, anillos). */
val DataSeries = listOf(
    Indigo500,
    Violet500,
    Sky500,
    Mint500,
    Amber500,
    Rose500,
    Coral500,
)

// ------------------------------------------------- Aliases heredados (M3 base)
val Purple80 = Violet300
val PurpleGrey80 = Neutral300
val Pink80 = Rose300
val Purple40 = Violet600
val PurpleGrey40 = Neutral600
val Pink40 = Rose600

// -----------------------------------------------------------------------------
// Tokens de apoyo añadidos al migrar colores sueltos de las pantallas.
// -----------------------------------------------------------------------------

/** Ámbar muy claro, para contenedores de aviso en modo claro. */
val Amber50 = Color(0xFFFFF7E6)
/** Ámbar oscuro, legible sobre Amber50. */
val Amber600 = Color(0xFFD97706)

/** Rosas oscuros: sólo para los gradientes de las pantallas de fricción. */
val Rose800 = Color(0xFF9F1239)
val Rose900 = Color(0xFF6B0F26)

/**
 * Velos de peligro. Se usan como fondo en el desbloqueo de emergencia y en el
 * onboarding de choque: son casi negros con matiz rojo, para que el aviso se
 * perciba sin que el texto pierda contraste.
 */
val DangerVeilDeep = Color(0xFF1A0508)
val DangerVeilMid = Color(0xFF2D0A12)

/**
 * Metales del podio. El oro ya existe como Amber400; plata y bronce se definen
 * aquí porque son referencias literales a medallas, no colores de marca.
 */
val Silver = Color(0xFFC3C8D4)
val Bronze = Color(0xFFB87333)
