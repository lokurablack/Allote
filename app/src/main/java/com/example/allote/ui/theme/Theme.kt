package com.example.allote.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Aquí es donde ocurre la magia. Asignamos nuestros colores de marca
// a los roles de Material 3 (primary, secondary, background, etc.).
private val LightColorScheme = lightColorScheme(
    // Colores de Marca
    primary = BrandGreenPrimary,          // Color principal para botones, FABs, links.
    onPrimary = Color.White,              // Color del texto/iconos SOBRE el color primario.
    primaryContainer = StatusJobFinished, // Un contenedor relacionado con el color primario (verde claro).
    onPrimaryContainer = BrandGreenPrimary, // Texto/iconos sobre 'primaryContainer'.

    secondary = BrandBrownSecondary,      // Color para elementos menos prominentes (ej. chips de filtros).
    onSecondary = Color.White,            // Texto/iconos sobre el color secundario.
    secondaryContainer = Color(0xFFE6E0B9), // Contenedor relacionado con el secundario.
    onSecondaryContainer = BrandBrownSecondary,

    tertiary = BrandGreenTertiary,        // Color de acento.
    onTertiary = Color.White,             // Texto/iconos sobre el color terciario.
    tertiaryContainer = Color(0xFFBBECEB),
    onTertiaryContainer = BrandGreenTertiary,

    // Colores de Error
    error = SemanticError,                // Color para errores.
    onError = Color.White,                // Texto/iconos sobre el color de error.

    // Colores Neutrales
    background = NeutralBackground,       // Color de fondo de las pantallas.
    onBackground = NeutralTextPrimary,    // Color del texto sobre el fondo.

    surface = NeutralSurface,             // Color de las superficies (Cards, Menús, etc.).
    onSurface = NeutralTextPrimary,       // Color del texto sobre las superficies.

    surfaceVariant = NeutralBackground,   // Para distinguir superficies (ej. fondo de un TextField).
    onSurfaceVariant = NeutralTextSecondary, // Texto secundario sobre 'surfaceVariant'.

    outline = Color.LightGray             // Para bordes y divisores.
)

@Composable
fun ClientJobAppTheme(
    // Por ahora no usaremos tema oscuro ni colores dinámicos para mantenerlo simple.
    // darkTheme: Boolean = isSystemInDarkTheme(),
    // dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Forzamos el uso de nuestro LightColorScheme personalizado.
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Hacemos que la barra de estado superior sea de nuestro color primario.
            window.statusBarColor = colorScheme.primary.toArgb()
            // Ajusta el color de los iconos de la barra de estado (hora, batería)
            // 'false' significa que los iconos serán claros (para fondos oscuros).
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Tu archivo de tipografía
        content = content
    )
}