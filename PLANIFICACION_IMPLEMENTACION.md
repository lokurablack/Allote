# Relevamiento Digital de Lotes

## Resumen general

- Se reemplazó por completo la antigua funcionalidad de planificación automática de vuelos.
- La aplicación ahora ofrece un **módulo de relevamiento visual** que replica el flujo manual de croquis y anotaciones en papel.
- El operario puede documentar accesos, zonas de reabastecimiento, áreas restringidas, obstáculos, estado de los lotes vecinos, peligros y observaciones libres directamente desde el dispositivo.
- El relevamiento queda ligado al trabajo (`Job`) y, si corresponde, al lote (`Lote`), de modo que la información queda disponible para cualquier integrante del equipo antes de ejecutar la tarea.

### Actualizaciones recientes (2025-10-15)

- Se agrego un boton para abrir el mapa satelital a pantalla completa, manteniendo las mismas herramientas de anotacion.
- Cada anotacion puntual ahora muestra un pin con color y glifo segun la categoria, e incluye el titulo visible para identificarla sin tocar el marcador.
- Ahora se pueden dibujar trazos, lineas, rectangulos, circulos y flechas directamente sobre el mapa satelital con la herramienta 'Dibujo', reutilizando las mismas herramientas del croquis.

### Mejoras de UX en pantalla completa (2025-10-18)

#### Panel de controles colapsable

**Problema**: El panel de controles en modo pantalla completa ocupaba demasiado espacio visual, dificultando la visualización completa del mapa.

**Solución implementada** (`FieldSurveyScreen.kt:1240-1344`):

1. **Estado de expansión persistente**:
   - Se agregó `isControlsExpanded` con `rememberSaveable` para mantener el estado entre recomposiciones (línea 1240)
   - Por defecto el panel se inicia expandido (`true`)

2. **Header con toggle**:
   - Nuevo Row con texto "Controles" y botón IconButton (líneas 1270-1294)
   - Iconos dinámicos según estado:
     - `Icons.AutoMirrored.Filled.KeyboardArrowLeft` cuando está expandido (colapsar)
     - `Icons.AutoMirrored.Filled.KeyboardArrowRight` cuando está colapsado (expandir)

3. **Renderizado condicional**:
   - Los controles (LayerAndToolSelector, CategorySelector, SketchToolSelector) solo se muestran cuando `isControlsExpanded == true` (líneas 1296-1328)
   - El header con el botón de toggle permanece siempre visible

4. **Dimensionamiento del panel**:
   - Ancho fijo de 280dp para evitar superposición con el botón de cerrar pantalla completa (línea 1261)
   - Posicionado en esquina superior izquierda con padding de 16dp
   - Background semi-transparente (alpha 0.95f) con elevación para contraste sobre el mapa

#### Etiquetas descriptivas en geometrías

**Problema**: Las líneas, flechas, rectángulos y círculos dibujados sobre el mapa no mostraban texto descriptivo, solo los puntos tenían marcadores con título.

**Solución implementada** (`FieldSurveyScreen.kt:769-801`):

1. **Marcadores en polilíneas** (líneas 769-781):
   - Cuando una polilínea (LINE o ARROW) tiene título o descripción, se agrega un marcador en el punto medio
   - Función helper `calculatePolylineCenter()` obtiene el punto medio de la línea (líneas 1068-1075)

2. **Marcadores en polígonos** (líneas 789-801):
   - Cuando un polígono (RECTANGLE o CIRCLE) tiene título o descripción, se agrega un marcador en el centroide
   - Función helper `calculatePolygonCenter()` calcula el promedio de coordenadas (líneas 1077-1085)

3. **Características del marcador**:
   - Reutiliza la función `rememberMarkerDescriptor()` para generar el bitmap personalizado
   - zIndex = 2f para que aparezca sobre la geometría (zIndex = 1f para las líneas/polígonos)
   - Mismo diseño visual que los marcadores de punto (pin con color de categoría, glifo y etiqueta con título)

#### Capitalización automática en campos de texto

**Problema**: Los campos de texto no capitalizaban automáticamente la primera letra, requiriendo intervención manual del usuario.

**Solución implementada**:

1. **Imports necesarios** (`FieldSurveyScreen.kt`):
   - `androidx.compose.foundation.text.KeyboardOptions` (línea 39)
   - `androidx.compose.ui.text.input.KeyboardCapitalization` (línea 103)

2. **Campos configurados**:
   - **AnnotationDialog** - Campo "Título" (línea 1728):
     ```kotlin
     keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
     ```
   - **AnnotationDialog** - Campo "Detalle" (línea 1736):
     ```kotlin
     keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
     ```
   - **CustomCategoryDialog** - Campo "Nombre" (línea 1780):
     ```kotlin
     keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
     ```

3. **Comportamiento**:
   - Al abrir cualquier campo de texto, el teclado automáticamente pone la primera letra de cada oración en mayúscula
   - Mejora la consistencia y profesionalismo de los datos ingresados
   - Reduce pasos manuales para el usuario

---

## Objetivos del nuevo módulo

- Capturar el perímetro real del lote y mantener un repositorio digital de anotaciones operativas.
- Facilitar la transferencia de conocimiento entre quien visita el lote para planificar y el operario que realiza el trabajo.
- Permitir alternar entre un mapa satelital y un croquis libre para plasmar recorridos, zonas y referencias visuales.
- Centralizar fotografías, notas detalladas y banderas de “dato crítico” que ameritan especial atención.
- Compartir o exportar la información sin depender de impresiones o fotos de pizarras.

---

## Arquitectura de datos

### Entidades nuevas (`AppDatabase` v29)

| Entidad | Propósito | Campos clave |
| --- | --- | --- |
| `FieldSurvey` | Cabecera del relevamiento asociado a un `Job` (y opcionalmente a un `Lote`). | `jobId`, `loteId`, `createdAt`, `updatedAt`, `baseLayer`, `boundaryGeoJson`, `customCategoriesJson` |
| `SurveyAnnotation` | Anotación individual (marcador, polilínea, polígono o trazo). | `surveyId`, `category`, `geometryType`, `geometryPayload`, `colorHex`, `isCritical`, `metadataJson` |
| `AnnotationMedia` | Archivos multimedia asociados a una anotación (fotos, documentos). | `annotationId`, `uri`, `type`, `isUploaded` |

#### Migración 28 → 29

1. Se renombran las tablas viejas `work_plans` y `flight_segments` a `legacy_*` para preservar el histórico.
2. Se crean las nuevas tablas con índices sobre `jobId`, `loteId` y `surveyId`.
3. Se conserva la versión previa para referencia pero ya no se usan las entidades ni DAOs antiguos.

---

## Repositorio y capa de dominio

### `FieldSurveyRepository`

- `ensureSurvey(jobId, loteId)` crea o reutiliza el relevamiento activo para el trabajo/lote.
- `observeSurveyWithAnnotations(surveyId)` expone un flow reactivo con la cabecera y las anotaciones + adjuntos.
- Operaciones de mantenimiento: actualizar capa base, perímetro, categorías personalizadas, insertar/editar/eliminar anotaciones y adjuntos.
- Accesos de conveniencia para obtener metadatos del trabajo (`Job`) y del lote (`Lote`).
- Internamente se actualiza el `updatedAt` del relevamiento ante cualquier modificación.

### Geometrías (`SurveyGeometry`)

Se definió un `sealed class` para representar diferentes tipos de geometría y serializarlas a JSON:

- `MapPoint`, `MapPolyline`, `MapPolygon` para trabajar con coordenadas geográficas.
- `SketchPath` y `SketchShape` para croquis manuales (almacena puntos normalizados respecto al lienzo).

---

## UI y flujo de usuario

### Pantalla `FieldSurveyScreen`

1. **Capa de trabajo**: conmutador entre Mapa (Google Maps híbrido) y Croquis o lienzo libre.
2. **Selector de categorías**: chips configurables con color e icono; incluye conjunto base (Accesos, Reabastecimiento, Zonas prohibidas, Obstáculos, Vecinos, Peligros, Observaciones) y permite crear categorías personalizadas.
3. **Mapa**: tapping crea anotaciones puntuales; se renderizada el perímetro capturado y se listan los elementos guardados.
4. **Croquis**: herramientas de trazo libre, línea, flecha, rectángulo, círculo y modo mover/zoom; soporte de deshacer.
5. **Panel de anotaciones**: listado con edición in-place, adjuntos fotográficos y banderas de criticidad.
6. **Snacks y validaciones**: feedback inmediato ante guardados, eliminaciones y errores.

### ViewModel (`FieldSurveyViewModel`)

- Inicializa contexto (`Job`, `Lote`) y garantiza la existencia del relevamiento.
- Convierte el snapshot de base de datos a `FieldSurveyUiState`, separando anotaciones de mapa y croquis.
- Gestiona borradores de anotación, apertura/cierre de diálogos y sincroniza la capa base seleccionada.
- Serializa perímetros y categorías personalizadas (JSON) y dispara eventos de exportación PDF y compartir.

---

## Datos persistidos vs. elementos UI

| Elemento UI | Persistencia |
| --- | --- |
| Perímetro interactivo | `FieldSurvey.boundaryGeoJson` (array de `{lat, lng}`) |
| Categorías personalizadas | `FieldSurvey.customCategoriesJson` |
| Trazo libre croquis | `SurveyAnnotation` con `SketchPath` |
| Markers en mapa | `SurveyAnnotation` con `MapPoint` |
| Fotos adjuntas | `AnnotationMedia` |

---

## Impacto en navegación y DI

- Nueva ruta: `AppDestinations.FIELD_SURVEY_ROUTE = "field_survey/{jobId}?loteId={loteId}"`.
- `AppModule` inyecta `FieldSurveyDao`, `SurveyAnnotationDao`, `AnnotationMediaDao` y `FieldSurveyRepository`.
- `JobDetailScreen` abre la experiencia cuando el usuario elige “Planificar relevamiento”.
- Se eliminan `WorkPlanScreen`, `WorkPlanViewModel`, `WorkPlanRepository`, `FlightPlanningService` y tablas relacionadas.

---

## Funcionalidades clave entregadas

1. **Exportación en PDF**: genera un informe con cabecera del trabajo, vista simplificada del mapa/croquis y listado con datos críticos. El archivo se comparte vía `FileProvider` y se abre con el intent del sistema.
2. **Adjuntos fotográficos**: cámara y galería con persistencia de URIs; miniaturas en la lista y limpieza controlada.
3. **Edición de anotaciones**: el diálogo se reutiliza tanto para crear como para modificar título, detalle y criticidad.
4. **Croquis enriquecido**: herramientas de línea, flecha, rectángulo, círculo, freehand, zoom/pan y undo, con normalización para persistir y exportar.

---

## Consideraciones de migración y soporte

- Los registros previos de planificación se mantienen en tablas `legacy_*` para consulta histórica.
- La base de datos incrementa a versión 29; se incluye migración completa en `AppDatabase`.
- El build `assembleDebug` compila correctamente tras los cambios (ver logs del 2025-10-14).

---

## Referencias de código

- `app/src/main/java/com/example/allote/data/FieldSurvey.kt` – Entidades y relaciones Room.
- `app/src/main/java/com/example/allote/data/FieldSurveyRepository.kt` – API de acceso a datos.
- `app/src/main/java/com/example/allote/ui/survey/FieldSurveyViewModel.kt` – Orquestación del estado UI.
- `app/src/main/java/com/example/allote/ui/survey/FieldSurveyScreen.kt` – Composable principal con herramientas de relevamiento.
- `app/src/main/java/com/example/allote/ui/survey/export/FieldSurveyExporter.kt` – Generación y compartición del PDF.
- `app/src/main/java/com/example/allote/ui/AppNavigation.kt` – Nueva ruta y navegación actualizada.

---

## Optimizaciones y ajustes posteriores (2025-10-15)

### Corrección de errores críticos

1. **Error de exportación PDF**: Se corrigió el parsing incorrecto de `boundaryGeoJson`. El campo almacena un array JSON de objetos `{lat, lng}` pero se intentaba parsear como `SurveyGeometry`. Se agregó la función helper `parseBoundaryPoints()` en `FieldSurveyExporter.kt:430-443` para manejar correctamente el formato.

2. **Error de overload en compilación**: Se eliminó el archivo `FieldSurveyScreen_backup.kt` que causaba conflicto al tener dos funciones `FieldSurveyScreen` con la misma firma en el mismo paquete.

### Optimización del layout y distribución de espacio

**Problema identificado**: El mapa/croquis se volvía demasiado pequeño cuando se agregaban anotaciones, haciendo imposible seguir trabajando sobre la imagen.

**Solución implementada** (`FieldSurveyScreen.kt`):

1. **Reducción de densidad visual**:
   - Altura de la lista de anotaciones: 200dp → 120dp (línea 850)
   - Padding del Card contenedor: 12dp → 8dp (línea 832)
   - Padding de cada anotación: 10dp → 8dp (línea 879)
   - Espaciadores internos: 6dp → 4dp (líneas 924, 932)
   - Spacing entre anotaciones: 8dp → 6dp (línea 849)

2. **Compactación de elementos**:
   - Miniaturas de fotos: 80×60dp → 60×45dp (línea 950)
   - Botón "Foto": altura 28dp → 24dp, icono 14dp → 12dp, texto 11sp → 10sp (líneas 935-939)
   - Botón eliminar en miniaturas: 20dp → 18dp, icono 12dp → 10dp (líneas 963-965)

3. **Ajustes tipográficos previos**:
   - Todos los tamaños de fuente reducidos en 20-30%
   - Títulos: 18sp, labels: 12-13sp, body: 11-13sp
   - Uso de símbolos (✏, —, →, ▢, ○, ✋) en lugar de texto en herramientas de croquis
   - Iconos: 14-18dp en lugar de 24dp

**Resultado**: Se liberaron aproximadamente 80-100dp de espacio vertical para el mapa/croquis, manteniendo toda la funcionalidad de la lista pero priorizando el área de trabajo principal.
