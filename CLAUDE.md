# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

**Al Lote** - App Android para gestión de aplicaciones agrícolas con drones (pulverizaciones líquidas y sólidas). Desarrollada en Kotlin 100%.

## Contexto del Negocio

- Empresa de aplicaciones agrícolas con drones DJI Agras (T&S Agroaplicaciones)
- Gestión de trabajos para productores agrícolas (clientes)
- Cálculo preciso de recetas de productos (herbicidas, insecticidas, fertilizantes, semillas)
- Factores críticos: condiciones climáticas, caudal de aplicación, autonomía de baterías, ubicación del equipo reabastecedor
- La app funciona offline-first (uso en campo sin conexión)

## Arquitectura

**Patrón:** MVVM con Clean Architecture
- **UI:** Jetpack Compose + Material 3
- **ViewModel:** StateFlow + UDF (Unidirectional Data Flow)
- **Repository:** Single Source of Truth
- **Data:** Room (local) + Retrofit (APIs remotas: clima, cotizaciones, noticias)
- **DI:** Hilt
- **Asincronía:** Coroutines + Flow
- **Navegación:** Navigation Compose

Estructura por features: `ui.clients`, `ui.jobs`, `ui.recetas`, etc.

## Base de Datos (Room)

- **AppDatabase.kt**: 16 entidades, 22 versiones con migraciones
- Entidades principales: `Job`, `Client`, `Product`, `Receta`, `WorkPlan`, `Movimiento`, `Lote`, etc.
- Migraciones manejadas manualmente para no perder datos del usuario

## Comandos de Desarrollo

### Build y Run
```bash
# Desde Android Studio o:
./gradlew assembleDebug
./gradlew installDebug
```

### Tests (si existen)
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Módulos Principales

### 1. **Dashboard** (`DashboardScreen`)
- Hub central con resumen de negocio
- Clima actual y pronóstico
- Noticias del agro
- Acceso rápido a módulos

### 2. **Trabajos** (`JobsScreen`, `JobDetailScreen`)
- CRUD de trabajos con filtros (estado, cliente, fechas, tipo aplicación, facturación)
- Superficie en hectáreas, tipo de aplicación (líquida/sólida/mixta)
- Estados: Pendiente → Finalizado
- Facturación: No Facturado → Facturado → Pagado

### 3. **Recetas** (`RecetasScreen`)
- Cálculo de mezclas de productos
- Parámetros: hectáreas, caudal (L/ha o Kg/ha), capacidad tanque
- Productos ordenados por formulación (orden de mezcla crítico)
- División en lotes para gestión detallada (`GestionLotesScreen`)

### 4. **Clientes** (`ClientsScreen`, `ClientAdminScreen`)
- CRUD de clientes
- Vista de trabajos por cliente
- Contabilidad individual (cuenta corriente)

### 5. **Administración** (`AdministracionScreen`, `AdministracionGeneralScreen`)
- Cálculo de facturación por trabajo (precio/ha × superficie, con IVA opcional 10.5%)
- Contabilidad general con movimientos debe/haber
- Aprobación de pagos pendientes
- Filtros por rango de fechas

### 6. **Productos** (`ProductsScreen`, `ProductDetailScreen`)
- Catálogo de insumos (pulverización/esparcido)
- Atributos: nombre comercial, principio activo, tipo, formulación, banda toxicológica
- Los productos aquí definidos se usan en las recetas

### 7. **Parámetros** (`ParametrosScreen`)
- Configuración técnica por trabajo: dosis, interlineado, velocidad, altura
- Diferenciado por tipo: disco/RPM (sólido) vs tamaño gota (líquido)

### 8. **Configuración** (`SettingsScreen`)
- Moneda (USD/ARS) con tasa de cambio manual o automática (API Bluelytics)
- Precios por tipo de aplicación
- Gestión de formulaciones (orden de mezcla)

## Decisiones de Diseño Importantes

1. **Offline-first**: Room es la fuente principal, APIs son complementarias
2. **Formulaciones**: El orden de mezcla de productos es crítico para aplicaciones seguras
3. **Lotes**: Permiten dividir trabajos grandes y calcular sobrantes de productos
4. **Ubicación GPS**: Cada trabajo/lote puede tener coordenadas (Google Maps integration)
5. **Imágenes**: Adjuntar fotos a trabajos (`ImagesJobScreen`)
6. **Checklists**: Listas de verificación genéricas (`ChecklistsScreen`)

## Consideraciones del Usuario

- Usuario tiene conocimientos básicos de programación
- Explicar cambios de forma simple, sin tecnicismos excesivos
- La app debe cumplir requisitos para publicación en Play Store
- Futuro: posible refactorización para iOS

## Flujo de Trabajo Típico

1. Crear cliente
2. Crear trabajo asociado a cliente (superficie, tipo aplicación, ubicación)
3. Configurar parámetros de aplicación
4. Crear receta (elegir productos, dosis, calcular cantidades)
5. Opcionalmente: dividir en lotes
6. Marcar trabajo como finalizado
7. Calcular facturación en Administración
8. Registrar pago en Contabilidad

## APIs Externas

- **Open-Meteo**: pronóstico climático (temperatura, viento, precipitación)
- **Bluelytics**: cotización dólar blue (ARS/USD)
- **NewsData.io**: noticias del sector agropecuario

## Plan de Trabajo (Feature Futura - Alta Complejidad)

Planificación pre-trabajo considerando:
- Extensiones del lote (este-oeste, norte-sur)
- Autonomía batería (8-9 min)
- Capacidad tanque (40L en T50)
- Caudal aplicación (8-10 L/ha)
- Ubicación del equipo reabastecedor
- Dirección del viento
- Optimización de pasadas para maximizar eficiencia

**Nota**: Solo implementar si existe forma sencilla e intuitiva.

## Generación de PDF (Feature Futura)

Informe completo del trabajo para entregar al cliente post-aplicación.
