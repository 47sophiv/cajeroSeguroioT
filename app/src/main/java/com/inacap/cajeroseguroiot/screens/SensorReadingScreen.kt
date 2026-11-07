package com.inacap.cajeroseguroiot.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inacap.cajeroseguroiot.bluetooth.BluetoothConnectionManager
import com.inacap.cajeroseguroiot.bluetooth.ConnectionStatus
import com.inacap.cajeroseguroiot.ui.theme.*

/**
 * Pantalla de Monitoreo (Criterio 2.1.1.2 - 10 puntos)
 * Visualiza datos recibidos inalámbricamente desde el circuito IoT
 * e incorpora elementos interactivos de Material Design:
 * - Switch: Pausa/Reanudación de recepción en tiempo real.
 * - RadioButtons: Filtro de categorías de eventos (Todos / Sensores / Actuadores).
 * - EditText (OutlinedTextField): Búsqueda y filtrado de tramas recibidas.
 */
@Composable
fun SensorReadingScreen(btManager: BluetoothConnectionManager) {
    val rfidUid = btManager.rfidCardUid.value
    val isCardIn = btManager.isCardPresent.value
    val pinInput = btManager.keypadEnteredPin.value
    val attempts = btManager.failedAttemptsCount.intValue
    val isLocked = btManager.isAtmLocked.value
    val status = btManager.connectionStatus.value

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterRadio by remember { mutableStateOf("TODOS") }
    val isRealtime = btManager.isRealtimeMonitoringActive.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Encabezado
        Text(
            text = "Pantalla de Monitoreo (Telemetría IoT)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Visualización en tiempo real de datos de sensores Arduino (Criterio 2.1.1.2 y 2.1.4)",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Control de Monitoreo mediante Switch interactivo (Criterio 2.1.1.2)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monitoreo en Tiempo Real",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRealtime) "Recepción continua activa vía Bluetooth" else "Monitoreo en pausa",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRealtime) BankSuccess else TextSecondary
                    )
                }

                // Switch interactivo
                Switch(
                    checked = isRealtime,
                    onCheckedChange = { btManager.isRealtimeMonitoringActive.value = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Simulación de Pantalla LCD 16x2 del Prototipo Físico
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color(0xFF22C55E), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DISPLAY LCD 16x2 (ARDUINO)",
                        color = Color(0xFF86EFAC),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "VÍA HC-05",
                        color = Color(0xFF86EFAC),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Fila 1 LCD
                Text(
                    text = btManager.lcdLine1.value.padEnd(16, ' '),
                    color = Color(0xFFF0FDF4),
                    fontSize = 19.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                // Fila 2 LCD
                Text(
                    text = btManager.lcdLine2.value.padEnd(16, ' '),
                    color = Color(0xFFBBF7D0),
                    fontSize = 19.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Tarjeta de Sensor RFID (RC522)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Lector RFID-RC522",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bus SPI en Pines 9-13 Arduino",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        color = if (isCardIn) BankSuccess.copy(alpha = 0.2f) else BankAlert.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isCardIn) "TARJETA PRESENTE" else "SIN TARJETA",
                            color = if (isCardIn) BankSuccess else BankAlert,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "UID Leído Inalámbricamente:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = rfidUid,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = BankPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Tarjeta de Teclado Matricial 4x4
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Teclado Matricial 4x4",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ingreso de Clave PIN Secreta",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        color = when (attempts) {
                            0 -> BankSuccess.copy(alpha = 0.15f)
                            1 -> Color(0xFFFBBF24).copy(alpha = 0.2f)
                            2 -> Color(0xFFF97316).copy(alpha = 0.2f)
                            else -> BankAlert.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Fallos: $attempts / 3",
                            color = if (attempts >= 3) BankAlert else Color(0xFFD97706),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Dígitos Recibidos (Ocultos con Asteriscos):",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (i in 0 until 4) {
                        val digitFilled = i < pinInput.length
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .background(
                                    color = if (digitFilled) BankPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (digitFilled) BankPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (digitFilled) "●" else "-",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (digitFilled) BankPrimary else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Panel de simulación interactiva si está en Modo Simulación
        if (status == ConnectionStatus.CONNECTED_SIMULATOR) {
            Spacer(modifier = Modifier.height(16.dp))
            SimulatorHardwareCard(btManager)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Filtro de Registro con EditText y RadioButtons (Criterio 2.1.1.2)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Filtrar Eventos de Telemetría",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // EditText para búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar evento o UID...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // RadioButtons para selección de categoría
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedFilterRadio == "TODOS",
                            onClick = { selectedFilterRadio = "TODOS" }
                        )
                        Text("Todos", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedFilterRadio == "SENSORES",
                            onClick = { selectedFilterRadio = "SENSORES" }
                        )
                        Text("Sensores", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedFilterRadio == "ACTUADORES",
                            onClick = { selectedFilterRadio = "ACTUADORES" }
                        )
                        Text("Actuadores", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lista de eventos filtrados
                val filteredLogs = btManager.liveTelemetryLogs.filter { event ->
                    val matchesSearch = searchQuery.isBlank() || event.payload.contains(searchQuery, ignoreCase = true)
                    val matchesType = when (selectedFilterRadio) {
                        "SENSORES" -> !event.isActuatorCommand
                        "ACTUADORES" -> event.isActuatorCommand
                        else -> true
                    }
                    matchesSearch && matchesType
                }

                Text(
                    text = "Mostrando ${filteredLogs.size} eventos:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (filteredLogs.isEmpty()) {
                        Text(
                            text = "No hay eventos que coincidan con el filtro.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        filteredLogs.forEach { log ->
                            Surface(
                                color = if (log.isActuatorCommand) BankPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${log.timestamp} [${log.source}]",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BankPrimary
                                    )
                                    Text(
                                        text = log.payload,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
