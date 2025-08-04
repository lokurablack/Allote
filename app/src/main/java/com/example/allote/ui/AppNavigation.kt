package com.example.allote.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.allote.ui.administracion.AdministracionScreen
import com.example.allote.ui.administracion.AdministracionViewModel
import com.example.allote.ui.administraciongeneral.AdministracionGeneralScreen
import com.example.allote.ui.administraciongeneral.AdministracionGeneralViewModel
import com.example.allote.ui.clientadmin.ClientAdministracionScreen
import com.example.allote.ui.clientadmin.ClientAdministracionViewModel
import com.example.allote.ui.clientcontabilidad.ClientContabilidadScreen
import com.example.allote.ui.clientcontabilidad.ClientContabilidadViewModel
import com.example.allote.ui.clientjobs.ClientJobsScreen
import com.example.allote.ui.clientjobs.ClientJobsViewModel
import com.example.allote.ui.clients.ClientsScreen
import com.example.allote.ui.clients.ClientsViewModel
import com.example.allote.ui.dashboard.DashboardScreen
import com.example.allote.ui.docviewer.DocumentViewerScreen
import com.example.allote.ui.formulaciones.FormulacionesScreen
import com.example.allote.ui.formulaciones.FormulacionesViewModel
import com.example.allote.ui.imagesjob.ImagesJobScreen
import com.example.allote.ui.imagesjob.ImagesJobViewModel
import com.example.allote.ui.jobdetail.JobDetailScreen
import com.example.allote.ui.jobdetail.JobDetailViewModel
import com.example.allote.ui.jobs.JobsScreen
import com.example.allote.ui.jobs.JobsViewModel
import com.example.allote.ui.lotes.GestionLotesScreen
import com.example.allote.ui.lotes.GestionLotesViewModel
import com.example.allote.ui.main.MainViewModel
import com.example.allote.ui.parametros.ParametrosScreen
import com.example.allote.ui.parametros.ParametrosViewModel
import com.example.allote.ui.pdfviewer.PdfViewerScreen
import com.example.allote.ui.pdfviewer.PdfViewerViewModel
import com.example.allote.ui.productdetail.ProductDetailScreen
import com.example.allote.ui.productdetail.ProductDetailViewModel
import com.example.allote.ui.products.ProductosScreen
import com.example.allote.ui.products.ProductsViewModel
import com.example.allote.ui.recetas.RecetasScreen
import com.example.allote.ui.recetas.RecetasViewModel
import com.example.allote.ui.settings.SettingsScreen
import com.example.allote.ui.settings.SettingsViewModel
import com.example.allote.ui.splash.SplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

object AppDestinations {
    const val SPLASH_ROUTE = "splash"
    const val DASHBOARD_ROUTE = "dashboard"
    const val JOBS_ROUTE = "jobs"
    const val CLIENTS_ROUTE = "clients"
    const val ADMIN_ROUTE = "admin"
    const val PRODUCTS_ROUTE = "products"
    const val FORMULACIONES_ROUTE = "formulaciones"
    const val SETTINGS_ROUTE = "settings"
    const val JOB_ID_ARG = "jobId"
    const val CLIENT_ID_ARG = "clientId"
    const val PRODUCT_ID_ARG = "productId"
    const val JOB_DETAIL_ROUTE = "job_detail/{$JOB_ID_ARG}"
    const val CLIENT_ADMIN_ROUTE = "client_admin/{$CLIENT_ID_ARG}"
    const val CLIENT_JOBS_ROUTE = "client_jobs/{$CLIENT_ID_ARG}"
    const val CLIENT_CONTABILIDAD_ROUTE = "client_contabilidad/{$CLIENT_ID_ARG}"
    const val RECETAS_ROUTE = "recetas/{$JOB_ID_ARG}"
    const val ADMINISTRACION_ROUTE = "administracion/{$JOB_ID_ARG}"
    const val ADMINISTRACION_RESUMEN_ROUTE = "administracion_resumen/{$JOB_ID_ARG}"
    const val PDF_VIEWER_ROUTE = "pdf_viewer/{$JOB_ID_ARG}"
    const val PARAMETROS_ROUTE = "parametros/{$JOB_ID_ARG}"
    const val PRODUCT_DETAIL_ROUTE = "product_detail/{$PRODUCT_ID_ARG}"
    const val IMAGES_JOB_ROUTE = "images_job/{$JOB_ID_ARG}"
    const val GESTION_LOTES_ROUTE = "gestion_lotes/{$JOB_ID_ARG}"
    const val MOVIMIENTO_ID_ARG = "movimientoId"
    const val DOCUMENT_VIEWER_ROUTE = "document_viewer/{$MOVIMIENTO_ID_ARG}"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    setFabAction: (() -> Unit) -> Unit,
    showAddJobDialog: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.SPLASH_ROUTE,
        modifier = modifier
    ) {
        composable(AppDestinations.SPLASH_ROUTE) {
            // 1. OBTENEMOS EL VIEWMODEL AQUÍ, EN EL ENTORNO @Composable.
            // Este es el lugar correcto para llamar a hiltViewModel().
            val mainViewModel: MainViewModel = hiltViewModel()

            // 2. AHORA, USAMOS LA INSTANCIA DEL VIEWMODEL DENTRO DEL LaunchedEffect.
            LaunchedEffect(Unit) {
                // Inicia un temporizador de duración mínima.
                val minDurationJob = launch { delay(2500L) }

                // Espera a que la carga de datos del ViewModel termine.
                val dataLoadingJob = launch {
                    mainViewModel.uiState // <-- Usamos la instancia de arriba
                        .first { state -> !state.isLoading }
                }

                // Espera a que AMBOS trabajos (delay y carga de datos) terminen.
                joinAll(minDurationJob, dataLoadingJob)

                // Una vez que ambos han terminado, navega.
                navController.navigate(AppDestinations.DASHBOARD_ROUTE) {
                    popUpTo(AppDestinations.SPLASH_ROUTE) { inclusive = true }
                }
            }

            // Muestra tu diseño de splash, como ya lo tienes.
            SplashScreen()
        }

        composable(AppDestinations.DASHBOARD_ROUTE) {
            // Aquí no hay cambios, ya estaba correcto.
            val mainViewModel: MainViewModel = hiltViewModel()
            val uiState by mainViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                setFabAction { showAddJobDialog() }
            }

            DashboardScreen(
                uiState = uiState,
                onNavigate = { route -> navController.navigate(route) },
                onLocationPermissionGranted = { mainViewModel.onLocationPermissionGranted() },
                onFetchNextPage = { mainViewModel.onFetchNextPage() },
                onRefresh = { mainViewModel.onRefresh() }
            )
        }

        composable(AppDestinations.DASHBOARD_ROUTE) {
            // ... (Esta parte no necesita cambios)
            val mainViewModel: MainViewModel = hiltViewModel()
            val uiState by mainViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                setFabAction { showAddJobDialog() }
            }

            DashboardScreen(
                uiState = uiState,
                onNavigate = { route -> navController.navigate(route) },
                onLocationPermissionGranted = { mainViewModel.onLocationPermissionGranted() },
                onFetchNextPage = { mainViewModel.onFetchNextPage() },
                onRefresh = { mainViewModel.onRefresh() }
            )
        }
        composable(AppDestinations.JOBS_ROUTE) {
            LaunchedEffect(Unit) {
                setFabAction(showAddJobDialog)
            }

            val viewModel: JobsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            JobsScreen(
                uiState = uiState,
                onFilterChange = viewModel::onFilterChange,
                onDateChange = viewModel::onDateChange,
                onSaveJob = viewModel::saveJob,
                onUpdateJob = viewModel::updateJob,
                onDeleteJob = viewModel::deleteJob,
                onJobClick = { jobId -> navController.navigate("job_detail/$jobId") },
                onViewRecipes = { jobId -> navController.navigate("recetas/$jobId") }
            )
        }
        composable(AppDestinations.CLIENTS_ROUTE) {
            val viewModel: ClientsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            ClientsScreen(
                uiState = uiState,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onClearSearch = viewModel::onClearSearch,
                onSaveClient = viewModel::onSaveClient,
                onDeleteClient = viewModel::onDeleteClient,
                onClientClick = { clientId -> navController.navigate("client_admin/$clientId") },
                setFabAction = setFabAction
            )
        }
        composable(AppDestinations.ADMIN_ROUTE) {
            val viewModel: AdministracionGeneralViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            AdministracionGeneralScreen(
                uiState = uiState,
                viewModel = viewModel,
                currencySettings = uiState.currencySettings,
                setFabAction = setFabAction,
                // --- AÑADE LA NUEVA ACCIÓN DE NAVEGACIÓN ---
                onNavigateToDocumentViewer = { movimientoId ->
                    navController.navigate("document_viewer/$movimientoId")
                }
            )
        }
        composable(AppDestinations.PRODUCTS_ROUTE) {
            val viewModel: ProductsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            ProductosScreen(
                uiState = uiState,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onClearSearchQuery = viewModel::onClearSearchQuery,
                onTabSelected = viewModel::onTabSelected,
                onSaveProduct = viewModel::saveProduct,
                setFabAction = setFabAction,
                onDeleteProduct = viewModel::deleteProduct,
                onProductClick = { productId -> navController.navigate("product_detail/$productId") }
            )
        }
        composable(
            route = AppDestinations.PRODUCT_DETAIL_ROUTE,
            arguments = listOf(navArgument(AppDestinations.PRODUCT_ID_ARG) { type = NavType.IntType })
        ) {
            val viewModel: ProductDetailViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            ProductDetailScreen(
                uiState = uiState,
                onUpdateProduct = viewModel::updateProduct
            )
        }
        composable(
            route = AppDestinations.CLIENT_ADMIN_ROUTE,
            arguments = listOf(navArgument(AppDestinations.CLIENT_ID_ARG) { type = NavType.IntType })
        ) {
            val viewModel: ClientAdministracionViewModel = hiltViewModel()
            val client by viewModel.clientState.collectAsState()
            ClientAdministracionScreen(
                client = client,
                onNavigateToJobs = { clientId -> navController.navigate("client_jobs/$clientId") },
                onNavigateToContabilidad = { clientId -> navController.navigate("client_contabilidad/$clientId") }
            )
        }
        composable(
            route = AppDestinations.CLIENT_JOBS_ROUTE,
            arguments = listOf(navArgument(AppDestinations.CLIENT_ID_ARG) { type = NavType.IntType })
        ) {
            val viewModel: ClientJobsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            ClientJobsScreen(
                uiState = uiState,
                onStatusChange = viewModel::onStatusChange,
                onTypeChange = viewModel::onTypeChange,
                onBillingChange = viewModel::onBillingChange,
                onDateChange = viewModel::onDateChange,
                onUpdateJob = viewModel::updateJob,
                onDeleteJob = viewModel::deleteJob,
                onSaveJob = viewModel::saveJob,
                onJobClick = { jobId ->
                    navController.navigate(AppDestinations.JOB_DETAIL_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString()))
                },
                onViewRecipes = { jobId ->
                    navController.navigate(AppDestinations.RECETAS_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString()))
                },
                onNavigateToContabilidad = { clientId ->
                    navController.navigate(AppDestinations.CLIENT_CONTABILIDAD_ROUTE.replace("{${AppDestinations.CLIENT_ID_ARG}}", clientId.toString()))
                },
                setFabAction = setFabAction
            )
        }
        composable(route = AppDestinations.FORMULACIONES_ROUTE) {
            val viewModel: FormulacionesViewModel = hiltViewModel()
            val formulaciones by viewModel.formulaciones.collectAsState()

            FormulacionesScreen(
                formulaciones = formulaciones,
                onMove = viewModel::onMove,
                onAddFormulacion = viewModel::addFormulacion,
                onUpdateFormulacion = viewModel::updateFormulacion,
                onDeleteFormulacion = viewModel::deleteFormulacion,
                isFormulacionInUse = viewModel::isFormulacionInUse,
                setFabAction = setFabAction,
                onAutoSave = { onComplete ->
                    viewModel.autoSaveIfNeeded(onComplete)
                }
            )
        }
        composable(AppDestinations.SETTINGS_ROUTE) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            SettingsScreen(
                uiState = uiState,
                onNavigateToFormulaciones = { navController.navigate(AppDestinations.FORMULACIONES_ROUTE) },
                onSavePrice = viewModel::savePrice,
                onSaveCurrency = viewModel::saveDisplayCurrency,
                onSaveExchangeRate = viewModel::saveExchangeRate,
                // --- AÑADE LA NUEVA CONEXIÓN ---
                onUpdateRateFromApi = viewModel::actualizarTasaDeCambioDesdeApi
            )
        }
        composable(
            route = AppDestinations.CLIENT_CONTABILIDAD_ROUTE,
            arguments = listOf(navArgument(AppDestinations.CLIENT_ID_ARG) { type = NavType.IntType })
        ) {
            val viewModel: ClientContabilidadViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            ClientContabilidadScreen(
                uiState = uiState,
                onAddMovimiento = viewModel::addMovimiento,
                onUpdateMovimiento = viewModel::updateMovimiento,
                onDeleteMovimiento = viewModel::deleteMovimiento,
                onDateChange = viewModel::onDateChange,
                onNavigateToDocumentViewer = { movimientoId ->
                    navController.navigate("document_viewer/$movimientoId")
                },
                onConfirmarEliminacionGeneral = viewModel::confirmarEliminacionDeContabilidadGeneral,
                onCancelarEliminacion = viewModel::cancelarEliminacion,
                setFabAction = setFabAction
            )
        }
        composable(
            route = AppDestinations.RECETAS_ROUTE,
            arguments = listOf(navArgument(AppDestinations.JOB_ID_ARG) { type = NavType.IntType })
        ) {
            val viewModel: RecetasViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val isSolidApplication by viewModel.isSolidApplication.collectAsState()
            val filteredProducts by viewModel.filteredAvailableProducts.collectAsState()

            RecetasScreen(
                uiState = uiState,
                isSolidApplication = isSolidApplication,
                filteredProducts = filteredProducts,
                isHectareasValid = viewModel::isHectareasValid,
                isCaudalValid = viewModel::isCaudalValid,
                isCaldoPorTachadaValid = viewModel::isCaldoPorTachadaValid,
                onHectareasChange = viewModel::onHectareasChange,
                onCaudalChange = viewModel::onCaudalChange,
                onCaldoPorTachadaChange = viewModel::onCaldoPorTachadaChange,
                onCalcularClick = viewModel::calcularYGuardarReceta,
                onEliminarClick = viewModel::eliminarReceta,
                onAgregarProducto = viewModel::agregarProducto,
                onEliminarProductoDeReceta = viewModel::eliminarProductoDeReceta,
                onProductSearchQueryChanged = viewModel::onProductSearchQueryChanged,
                onClearProductSearch = viewModel::clearProductSearch,
                onNavigateToLotes = { jobId ->
                    navController.navigate(
                        AppDestinations.GESTION_LOTES_ROUTE.replace("{${AppDestinations.JOB_ID_ARG}}", jobId.toString())
                    )
                }
            )
        }
        composable(
            route = AppDestinations.ADMINISTRACION_ROUTE,
            arguments = listOf(navArgument(AppDestinations.JOB_ID_ARG) { type = NavType.IntType })
        ) {
            val viewModel: AdministracionViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            AdministracionScreen(
                uiState = uiState,
                onCostoChange = viewModel::onCostoChange,
                onHectareasChange = viewModel::onHectareasChange,
                onIvaChange = viewModel::onIvaChange,
                onCalcularClick = viewModel::calcularTotales,
                onGuardarClick = viewModel::saveChanges
            )
        }
        composable(
            route = AppDestinations.ADMINISTRACION_RESUMEN_ROUTE,
            arguments = listOf(navArgument(AppDestinations.JOB_ID_ARG) { type = NavType.IntType })
        ) {
            // This screen is no longer used, but we keep the route in case of old references.
            // You can optionally add a message here.
        }
        composable(
            route = AppDestinations.PDF_VIEWER_ROUTE,
            arguments = listOf(navArgument(AppDestinations.JOB_ID_ARG) { type = NavType.IntType })
        ) {
            val viewModel: PdfViewerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            PdfViewerScreen(
                uiState = uiState,
                onAddDocuments = viewModel::addDocuments,
                onDeleteDocument = viewModel::deleteDocument,
                onLoadThumbnail = { docItem -> viewModel.loadThumbnailFor(docItem) }
            )
        }
        composable(
            route = AppDestinations.PARAMETROS_ROUTE,
            arguments = listOf(navArgument(AppDestinations.JOB_ID_ARG) { type = NavType.IntType })
        ) {
            val viewModel: ParametrosViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            ParametrosScreen(
                uiState = uiState,
                onSave = viewModel::saveParametros,
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable(
            route = AppDestinations.IMAGES_JOB_ROUTE,
            arguments = listOf(navArgument(AppDestinations.JOB_ID_ARG) { type = NavType.IntType })
        ) {
            val viewModel: ImagesJobViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            ImagesJobScreen(
                uiState = uiState,
                onSelectImages = viewModel::addImages,
                onDeleteImage = viewModel::deleteImage
            )
        }
        composable(
            route = AppDestinations.GESTION_LOTES_ROUTE,
            arguments = listOf(navArgument(AppDestinations.JOB_ID_ARG) { type = NavType.IntType })
        ) {
            val viewModel: GestionLotesViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val recipeSummary by viewModel.recipeSummary.collectAsState()
            val isLoadingRecipe by viewModel.isLoadingRecipe.collectAsState()

            GestionLotesScreen(
                uiState = uiState,
                recipeSummary = recipeSummary,
                isLoadingRecipe = isLoadingRecipe,
                onAddLote = viewModel::addLote,
                onUpdateLote = viewModel::updateLote,
                onDeleteLote = viewModel::deleteLote,
                onUpdateLoteLocation = viewModel::updateLoteLocation,
                onLoadRecipe = viewModel::loadRecipeForLote,
                onDismissRecipe = viewModel::clearRecipeSummary,
                setFabAction = setFabAction,
                onRegistrarTrabajoRealizado = viewModel::registrarTrabajoRealizado,
                onGenerateSurplusSummary = viewModel::generateSurplusSummary,
                onClearSurplusSummary = viewModel::clearSurplusSummary
            )
        }
        composable(
            route = AppDestinations.JOB_DETAIL_ROUTE,
            arguments = listOf(navArgument(AppDestinations.JOB_ID_ARG) { type = NavType.IntType })
        ) {
            val viewModel: JobDetailViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            JobDetailScreen(
                uiState = uiState,
                onUpdateLocation = viewModel::updateJobLocation,
                onNavigate = { route -> navController.navigate(route) },
                onDaySelected = viewModel::onDaySelected,
                onDismissHourlyDialog = viewModel::onDismissHourlyDialog
            )
        }
        composable(
            route = AppDestinations.DOCUMENT_VIEWER_ROUTE,
            arguments = listOf(navArgument(AppDestinations.MOVIMIENTO_ID_ARG) { type = NavType.IntType })
        ) {
            DocumentViewerScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}
