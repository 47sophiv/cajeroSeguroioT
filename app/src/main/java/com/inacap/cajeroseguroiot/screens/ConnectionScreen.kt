package com.inacap.cajeroseguroiot.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inacap.cajeroseguroiot.bluetooth.BluetoothConnectionManager
import com.inacap.cajeroseguroiot.bluetooth.ConnectionStatus
import com.inacap.cajeroseguroiot.ui.theme.*

@SuppressLint("MissingPermission")
@Composable
fun ConnectionScreen(
    btManager: BluetoothConnectionManager,
    onRequestPermissions: () -> Unit
) {
    val status = btManager.connectionStatus.value
    val connectedDevice = btManager.connectedDeviceName.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tarjeta de Estado de Conexión
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Estado de Enlace IoT",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Indicador luminoso de estado
                    val (statusColor, statusText) = when (status) {
                        ConnectionStatus.CONNECTED_BLUETOOTH -> Pair(BankSuccess, "BLUETOOTH ACTIVO")
                        ConnectionStatus.CONNECTED_SIMULATOR -> Pair(BankWarning, "MODO SIMULADOR")
                        ConnectionStatus.CONNECTING -> Pair(BankWarning, "CONECTANDO...")
                        ConnectionStatus.DISCONNECTED -> Pair(BankAlert, "DESCONECTADO")
                    }
                    Surface(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelLarge,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Dispositivo actual: $connectedDevice",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    if (status != ConnectionStatus.DISCONNECTED) {
                        Button(
                            onClick = { btManager.disconnect() },
                            colors = ButtonDefaults.buttonColors(containerColor = BankAlert),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Desconectar")
                        }
                    } else {
                        Button(
                            onClick = {
                                onRequestPermissions()
                                btManager.refreshPairedDevices()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Buscar Dispositivos")
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { btManager.startSimulatorMode() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Modo Simulación")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sección de dispositivos vinculados (HC-05 / HC-06)
        Text(
            text = "Dispositivos Bluetooth Vinculados",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Seleccione su módulo HC-05 / HC-06 para sincronizar el cajero",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (btManager.pairedDevices.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No se detectan dispositivos Bluetooth emparejados.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vincule su módulo HC-05 en los Ajustes de Bluetooth de su teléfono (PIN: 1234 o 0000) o utilice el 'Modo Simulación' para probar el prototipo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(btManager.pairedDevices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { btManager.connectToDevice(device) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = device.name ?: "Dispositivo Desconocido",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = device.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Button(
                                onClick = { btManager.connectToDevice(device) },
                                colors = ButtonDefaults.buttonColors(containerColor = BankPrimary)
                            ) {
                                Text("Conectar")
                            }
                        }
                    }
                }
            }
        }
    }
}
