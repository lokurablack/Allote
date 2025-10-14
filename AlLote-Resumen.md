# Al Lote - Resumen del Proyecto

## Vision General
- Proyecto liderado por Marcos Schmid para crear una app Android nativa (Kotlin) que asista a su empresa T&S Agroaplicaciones.
- Objetivo principal: digitalizar la gestion integral de trabajos de aplicacion agricola con drones DJI Agras.
- La app apunta a operar offline-first en campo y cumplir con estandares de Play Store; se contemplan planes futuros para iOS.

## Perfil del Negocio
- Empresa: T&S Agroaplicaciones (contratista), ubicada en Chilibroste, Cordoba, Argentina.
- Servicios: aplicaciones liquidas y solidas con dos drones DJI Agras (ej. T50), incluyendo logistica de baterias, generadores y mixer.
- Flujo operacional: recepcion de pedidos, validacion climatica (viento, direccion, humedad, temperatura, indice KP), mapeo con controles del dron, configuracion de parametros (caudal, tamano de gota, altura, velocidad, interlineado) y limpieza posterior.
- Costos orientativos: aplicacion liquida ~12 USD por hectarea (insumos a cargo del productor).

## Objetivos de la App
- Centralizar gestion de clientes, trabajos, lotes, productos, recetas y administracion financiera.
- Facilitar calculos de recetas y consumo de insumos por hectarea.
- Proveer informacion meteorologica y ayudas operativas durante la planificacion.
- Ofrecer una navegacion inferior con boton flotante central adaptable al contexto (accion principal segun pantalla).

## Modulos y Pantallas Clave
- **DashboardScreen**: tablero inicial con clima (detalle expandible, pronostico horario), accesos rapidos a modulos, resumen de KPIs (trabajos, clientes, saldo) y noticias del agro con scroll infinito.
- **ClientsScreen**: gestor de clientes con busqueda, estados vacios guiados, tarjetas con acciones para editar/eliminar y acceso a detalle completo.
- **JobsScreen**: listado de trabajos con filtros, indicadores de estado, accesos a calculos de recetas, archivos adjuntos y eventos asociados; integra dialogos de ayuda y FAB para altas.
- **ProductsScreen / AdminScreen** (derivada del contexto): control de catalogos y administracion financiera.
- **ChecklistsScreen**: gestion completa de checklists (creacion, renombrado, eliminacion, reordenamiento drag & drop, animaciones y snackbars).
- **LocationDialogs.kt**: dialogos reutilizables para seleccionar y visualizar coordenadas en Google Maps (picker y viewer).
- **MainActivity**: orquestador de Scaffold, barra superior, barra inferior con FAB embebido, AppNavHost y dialogos modales (ej. JobDialogCompose).

## Arquitectura y Tech Stack
- Arquitectura limpia basada en MVVM + UDF con capas separadas (UI Compose, ViewModel, Repositorio, Data Sources).
- Jetpack Compose (Material 3) para toda la UI; estados expuestos via `StateFlow` y patrones UiState inmutables.
- Inyeccion de dependencias con Hilt; modulo principal `AppModule` provee repositorios, DAOs y servicios (singleton).
- Persistencia offline con Room, 16 entidades y 22 migraciones versionadas.
- Corrutinas y Flow para asincronia.
- Navegacion con Navigation Compose; carga de imagenes con Coil.
- Integraciones externas: Retrofit + Gson para APIs (Open-Meteo clima, Bluelytics cotizaciones, NewsData.io noticias); Google Maps via LocationDialogs.

## Flujo de Datos Referencial (Jobs)
1. `JobsScreen` obtiene `JobsViewModel` via `hiltViewModel()`.
2. El ViewModel expone `StateFlow<JobsUiState>` y consulta `JobsRepository`.
3. `JobsRepository` accede a `JobDao` (Room) como fuente de verdad.
4. `JobDao` devuelve `Flow<List<Job>>`; el repositorio lo procesa y lo entrega al ViewModel.
5. La UI observa el flujo y se recompone automaticamente.

## Lineamientos y Buenas Practicas
- Explicaciones y decisiones deben comunicarse en lenguaje simple, pensadas para alguien con conocimientos basicos de programacion.
- Todas las funcionalidades nuevas deben considerar requisitos de Play Store (permisos, seguridad, UX, rendimiento).
- Mantener filosofia offline-first; la sincronizacion con backend puede agregarse en el futuro sin romper repositorios.
- Respetar las migraciones existentes en Room (no perder datos en campo).
- Favorecer pruebas (unitarias para ViewModels/repositorios, integracion de DAOs, pruebas Compose UI).

## Planificacion de Trabajo (Ideas a Implementar)
- **Modulo de plan de trabajo**: digitaliza la planificacion previa y ahora permite ajustar interlineado, velocidad de trabajo, autonomia, capacidad de tanque, tiempo de reabastecimiento y cantidad de drones. Calcula limites por bateria/tanque, reutiliza ubicaciones guardadas, soporta delimitar el lote en un mapa para geometrías irregulares y genera segmentos con comentarios operativos.
- **Generacion de informes PDF**: emitir reporte post-trabajo con datos de produccion, clientes, productos y condiciones, listo para entregar al productor.
- **Mejoras potenciales**: manejo uniforme de errores en UiState, sincronizacion futura con backend multiusuario, exploracion de soporte iOS.

## Datos Operativos y Ejemplo Practico
- Planificar siempre la ubicacion del equipo reabastecedor y el sentido de vuelo para maximizar autonomia.
- Ejemplo documento: tres lotes de trigo con medidas 640x700 m, 580x700/450 m y 600x600 m; se eligieron puntos de aterrizaje segun calles internas/exteriores y se adapto la direccion de vuelo al viento pronosticado (E-O preferido). Se priorizo trabajar lotes por separado para evitar vuelos ineficientes (limite de 19.5 L util cuando el vuelo conjunto excedia autonomia).

## Requisitos y Futuras Colaboraciones
- Mantener trazabilidad de cambios y documentar decisiones relevantes en este archivo.
- Informar oportunidades de mejora al detectar optimizaciones durante el desarrollo.
- Prever eventual refactor para soporte iOS (estructurar modulos y logica compartible cuando sea posible).

