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
import androidx.compose.runtime.Composable
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

@Composable
fun SensorReadingScreen(btManager: BluetoothConnectionManager) {
    val rfidUid = btManager.rfidCardUid.value
    val isCardIn = btManager.isCardPresent.value
    val pinInput = btManager.keypadEnteredPin.value
    val attempts = btManager.failedAttemptsCount.value
    val isLocked = btManager.isAtmLocked.value
    val status = btManager.connectionStatus.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Encabezado
        Text(
            text = "Telemetría de Sensores (Lectura Inalámbrica)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Recepción de señales de sensores en tiempo real vía Bluetooth (Criterio 1.1.4.5 y 2.1.4)",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Simulación de Pantalla LCD 16x2 del Prototipo Físico
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
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "VÍA HC-05",
                        color = Color(0xFF86EFAC),
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Fila 1 LCD
                Text(
                    text = btManager.lcdLine1.value.padEnd(16, ' '),
                    color = Color(0xFFF0FDF4),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                // Fila 2 LCD
                Text(
                    text = btManager.lcdLine2.value.padEnd(16, ' '),
                    color = Color(0xFFBBF7D0),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta Sensor 1: Lector RFID-RC522
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
                    Column {
                        Text(
                            text = "Sensor RFID (MFRC522)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Frecuencia 13.56 MHz (Bus SPI)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    val badgeColor = if (isCardIn) BankSuccess else TextSecondary
                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isCardIn) "TARJETA DETECTADA" else "SIN TARJETA",
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "UID Leído: $rfidUid",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCardIn) BankPrimary else TextSecondary
                )

                if (status == ConnectionStatus.CONNECTED_SIMULATOR) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { btManager.simulateCardTap("A4:7B:3F:92") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BankAccentTeal)
                    ) {
                        Text("Simular Inserción de Tarjeta RFID")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta Sensor 2: Teclado Matricial 4x4
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sensor Teclado Matricial 4x4",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Entrada de PIN Bancario (Barrido matricial)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PIN Ingresado:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (pinInput.isEmpty()) "[ Vacío ]" else "* ".repeat(pinInput.length),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BankPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Barra de Intentos Fallidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contador de Intentos:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    val attemptColor = when (attempts) {
                        0 -> BankSuccess
                        1 -> BankWarning
                        2 -> BankAlert
                        else -> BankAlert
                    }
                    Text(
                        text = if (isLocked) "3/3 (BLOQUEADO)" else "$attempts / 3 intentos",
                        fontWeight = FontWeight.Bold,
                        color = attemptColor
                    )
                }

                // Si está en modo simulador, teclado interactivo
                if (status == ConnectionStatus.CONNECTED_SIMULATOR) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Teclado Virtual de Pruebas (Simulador):",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val keys = listOf(
                        listOf('1', '2', '3'),
                        listOf('4', '5', '6'),
                        listOf('7', '8', '9'),
                        listOf('*', '0', '#')
                    )

                    keys.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { char ->
                                OutlinedButton(
                                    onClick = { btManager.simulateKeypress(char) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = char.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Registro de telemetría de sensores en vivo
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Historial de Tramas Inalámbricas (Logcat)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(btManager.liveTelemetryLogs) { log ->
                        Text(
                            text = "[${log.timestamp}] [${log.source}]: ${log.payload}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (log.isActuatorCommand) BankAccentTeal else TextPrimary
                        )
                        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
