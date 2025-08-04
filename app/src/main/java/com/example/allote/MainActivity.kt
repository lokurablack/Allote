package com.example.allote

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // <-- 1. IMPORT NECESARIO
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.allote.data.Client
import com.example.allote.data.Job
import com.example.allote.ui.AppDestinations
import com.example.allote.ui.AppNavHost
import com.example.allote.ui.addjob.AddJobViewModel
import com.example.allote.ui.components.JobDialogCompose
import com.example.allote.ui.main.MainUiState
import com.example.allote.ui.main.MainViewModel
import com.example.allote.ui.theme.ClientJobAppTheme
import dagger.hilt.android.AndroidEntryPoint

// El objeto sellado NavScreen se mantiene igual, es una buena práctica.
sealed class NavScreen(val route: String, val label: String, val icon: ImageVector) {
    object Trabajos : NavScreen(AppDestinations.JOBS_ROUTE, "Trabajos", Icons.Default.WorkOutline)
    object Clientes : NavScreen(AppDestinations.CLIENTS_ROUTE, "Clientes", Icons.Default.PeopleOutline)
    object Admin : NavScreen(AppDestinations.ADMIN_ROUTE, "Admin", Icons.Default.AdminPanelSettings)
    object Productos : NavScreen(AppDestinations.PRODUCTS_ROUTE, "Productos", Icons.Default.Inventory2)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val addJobViewModel: AddJobViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // <-- 2. INSTALACIÓN DE LA SPLASH SCREEN
        // Llama a installSplashScreen ANTES de super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // <-- 3. (OPCIONAL) MANTENER SPLASH MIENTRAS CARGAN DATOS
        // Esto evita mostrar una pantalla de carga vacía justo después del splash.
        // La splash se ocultará cuando uiState.isLoading sea `false`.
        splashScreen.setKeepOnScreenCondition {
            mainViewModel.uiState.value.isLoading
        }

        setContent {
            ClientJobAppTheme {
                val uiState by mainViewModel.uiState.collectAsState()
                val clientsForDialog by addJobViewModel.clients.collectAsState()
                val navController = rememberNavController()

                // La llamada al Scaffold principal no cambia.
                MainAppScaffold(
                    uiState = uiState,
                    clientsForDialog = clientsForDialog,
                    onSaveJob = { addJobViewModel.saveJob(it) },
                    navController = navController,
                    onSettingsClick = { navController.navigate(AppDestinations.SETTINGS_ROUTE) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    uiState: MainUiState,
    clientsForDialog: List<Client>,
    onSaveJob: (Job) -> Unit,
    navController: NavHostController,
    onSettingsClick: () -> Unit
) {
    var fabAction by remember { mutableStateOf<() -> Unit>({}) }
    var showAddJobDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // <-- 4. SIMPLIFICACIÓN: YA NO NECESITAMOS LA LÓGICA 'showBars'
    // Como la Splash Screen ahora es manejada por el sistema, el Scaffold
    // siempre será visible.

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Al Lote") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    // El icono de "Home" solo aparece si no estamos en el dashboard.
                    if (currentRoute != AppDestinations.DASHBOARD_ROUTE) {
                        IconButton(onClick = {
                            navController.navigate(AppDestinations.DASHBOARD_ROUTE) {
                                popUpTo(0) // Limpia el back stack para no acumular pantallas.
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = "Volver al Inicio"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "Configuración")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFFE8F5E9)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavigationBarItem(
                        selected = currentRoute == NavScreen.Trabajos.route,
                        onClick = { navController.navigate(NavScreen.Trabajos.route) },
                        icon = { Icon(NavScreen.Trabajos.icon, NavScreen.Trabajos.label) },
                        label = { Text(NavScreen.Trabajos.label) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == NavScreen.Clientes.route,
                        onClick = { navController.navigate(NavScreen.Clientes.route) },
                        icon = { Icon(NavScreen.Clientes.icon, NavScreen.Clientes.label) },
                        label = { Text(NavScreen.Clientes.label) }
                    )
                    FloatingActionButton(
                        onClick = { fabAction() },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Filled.Add, "Agregar")
                    }
                    NavigationBarItem(
                        selected = currentRoute == NavScreen.Admin.route,
                        onClick = { navController.navigate(NavScreen.Admin.route) },
                        icon = { Icon(NavScreen.Admin.icon, NavScreen.Admin.label) },
                        label = { Text(NavScreen.Admin.label) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == NavScreen.Productos.route,
                        onClick = { navController.navigate(NavScreen.Productos.route) },
                        icon = { Icon(NavScreen.Productos.icon, NavScreen.Productos.label) },
                        label = { Text(NavScreen.Productos.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // <-- 5. RECORDATORIO IMPORTANTE
        // Asegúrate de que en tu archivo `AppNavHost.kt`, el `startDestination`
        // ya no sea SPLASH_ROUTE, sino la pantalla principal, por ej. `AppDestinations.DASHBOARD_ROUTE`.
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            setFabAction = { newAction -> fabAction = newAction },
            showAddJobDialog = { showAddJobDialog = true }
        )
    }

    if (showAddJobDialog) {
        if (clientsForDialog.isEmpty()) {
            AlertDialog(
                onDismissRequest = { showAddJobDialog = false },
                title = { Text("No hay clientes") },
                text = { Text("Debe agregar al menos un cliente antes de poder crear un trabajo.") },
                confirmButton = {
                    TextButton(onClick = { showAddJobDialog = false; navController.navigate(AppDestinations.CLIENTS_ROUTE) }) {
                        Text("Ir a Clientes")
                    }
                }
            )
        } else {
            JobDialogCompose(
                clients = clientsForDialog,
                onDismiss = { showAddJobDialog = false },
                onJobSaved = { newJob ->
                    onSaveJob(newJob)
                    Toast.makeText(context, "Trabajo agregado", Toast.LENGTH_SHORT).show()
                    showAddJobDialog = false
                }
            )
        }
    }
}