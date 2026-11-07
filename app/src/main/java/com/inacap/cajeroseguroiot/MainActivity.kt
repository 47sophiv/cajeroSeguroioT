package com.inacap.cajeroseguroiot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.inacap.cajeroseguroiot.bluetooth.BluetoothConnectionManager
import com.inacap.cajeroseguroiot.screens.*
import com.inacap.cajeroseguroiot.ui.theme.CajeroSeguroTheme

class MainActivity : ComponentActivity() {

    private lateinit var btManager: BluetoothConnectionManager

    // Registro de permisos dinámicos para Bluetooth en Android 12+ y anteriores
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Permisos de Bluetooth concedidos.", Toast.LENGTH_SHORT).show()
            btManager.refreshPairedDevices()
        } else {
            Toast.makeText(this, "Se requieren permisos para usar Bluetooth.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btManager = BluetoothConnectionManager(this)

        checkAndRequestPermissions()

        setContent {
            CajeroSeguroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLoggedIn by remember { mutableStateOf(false) }
                    var loggedUser by remember { mutableStateOf("Sophia Caro") }

                    if (!isLoggedIn) {
                        // Pantalla de Login obligatoria (Criterio 2.1.1.1 - 10 puntos)
                        LoginScreen(
                            btManager = btManager,
                            onLoginSuccess = { user, role ->
                                loggedUser = user
                                isLoggedIn = true
                            }
                        )
                    } else {
                        // Aplicación principal con pestañas (Criterios 2.1.1.2, 2.1.2, 2.1.3, 2.1.4)
                        AppMainLayout(
                            btManager = btManager,
                            currentUser = loggedUser,
                            onRequestPermissions = { checkAndRequestPermissions() },
                            onLogout = {
                                isLoggedIn = false
                                btManager.disconnect()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            btManager.refreshPairedDevices()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        btManager.disconnect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainLayout(
    btManager: BluetoothConnectionManager,
    currentUser: String,
    onRequestPermissions: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Conexión", "Monitoreo", "Activador", "Interconexión", "Seguridad")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cajero Seguro IoT",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Usuario: $currentUser (${btManager.currentRole.value})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text(
                            text = "Salir",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Pestañas de navegación con scroll horizontal para soportar 5 pestañas completas
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 8.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> ConnectionScreen(btManager, onRequestPermissions)
                    1 -> SensorReadingScreen(btManager)
                    2 -> ActuatorWritingScreen(btManager)
                    3 -> InterconnectionScreen(btManager)
                    4 -> SecurityIsoScreen()
                }
            }
        }
    }
}
