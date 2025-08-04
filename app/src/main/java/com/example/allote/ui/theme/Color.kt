package com.example.allote.ui.theme

import androidx.compose.ui.graphics.Color

// --- PALETA DE MARCA ---
// Estos son los colores que definen la identidad de tu aplicación.
// Los usaremos para crear el 'colorScheme' en Theme.kt.
val BrandGreenPrimary = Color(0xFF006D37)    // Verde oscuro y profesional para elementos principales.
val BrandBrownSecondary = Color(0xFF605C3A)   // Un tono tierra/marrón para acentos.
val BrandGreenTertiary = Color(0xFF386666)   // Un verde más apagado para acentos secundarios.

// --- COLORES NEUTRALES ---
// Para fondos, superficies de tarjetas y el color del texto.
val NeutralBackground = Color(0xFFFCFCFC)    // Un blanco ligeramente cálido para los fondos de pantalla.
val NeutralSurface = Color(0xFFFFFFFF)     // Blanco puro para las superficies de los Cards.
val NeutralTextPrimary = Color(0xFF1F1F1F)    // Negro/gris muy oscuro para el texto principal.
val NeutralTextSecondary = Color(0xFF757575)  // Gris medio para texto secundario o descriptivo.

// --- COLORES SEMÁNTICOS Y DE ESTADO ---
// Colores que comunican un significado específico (error, éxito, estados).
val SemanticError = Color(0xFFB00020)        // Rojo estándar para errores.
val SemanticSuccess = Color(0xFF2E7D32)      // Verde para acciones exitosas.
val SemanticWarning = Color(0xFFE5C200)      // Amarillo/ámbar para advertencias.

// Colores para el estado de facturación
val StatusBillingRed = Color(0xFFD32F2F)      // Rojo para "No Facturado".
val StatusBillingYellow = Color(0xFFE5C200)   // Amarillo para "Facturado".
val StatusBillingGreen = Color(0xFF4CAF50)     // Verde para "Pagado".

// Colores para el estado de los trabajos
val StatusJobPending = Color(0xFFFFFDE7)      // Amarillo muy claro para "Pendiente".
val StatusJobFinished = Color(0xFFE8F5E9)      // Verde muy claro para "Finalizado".

// --- COLORES ESPECÍFICOS DE UI ---
// Colores para componentes muy concretos. Nómbrenlos claramente por su uso.
val JobTypeLiquidBackground = Color(0xFFB3E5FC)   // Aplicación líquida - azul muy claro.
val JobTypeSolidBackground = Color(0xFFFFF9C4)    // Aplicación sólida - amarillo muy claro.
val JobTypeMixedBackground = Color(0xFFD7CCC8)    // Aplicación mixta - marrón muy claro.
val JobTypeMiscBackground = Color(0xFFC8E6C9)     // Aplicaciones varias - verde muy claro.
val FilterCardBackground = Color(0xFFF1F8E9)      // Fondo para la tarjeta de filtros.