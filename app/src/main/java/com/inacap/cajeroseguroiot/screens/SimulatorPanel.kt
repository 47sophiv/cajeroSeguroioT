package com.inacap.cajeroseguroiot.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inacap.cajeroseguroiot.bluetooth.BluetoothConnectionManager
import com.inacap.cajeroseguroiot.ui.theme.*

/**
 * Panel de Simulación Interactiva de Hardware (Criterio 1.1.4.7 y 2.1.2)
 * Permite simular la inserción de la tarjeta RFID-RC522 y el teclado matricial 4x4
 * de forma táctil sin requerir el circuito físico conectado.
 */
@Composable
fun SimulatorHardwareCard(btManager: BluetoothConnectionManager) {
    val isCardIn = btManager.isCardPresent.value
    val rfidUid = btManager.rfidCardUid.value
    val isLocked = btManager.isAtmLocked.value

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Simulador de Sensores Físicos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = Color(0xFFFBBF24).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "MODO INTERACTIVO",
                        color = Color(0xFFD97706),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Prueba la lectura de tarjeta RFID y el ingreso de clave PIN directamente en la app:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Simulación de Tarjeta RFID (RC522)
            Text(
                text = "1. Sensor RFID-RC522 (Tarjeta del Cliente):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (!isCardIn) {
                Button(
                    onClick = { btManager.simulateCardTap("E2:49:1A:8B") },
                    colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Insertar / Deslizar Tarjeta RFID (E2:49:1A:8B)")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = BankSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Tarjeta Insertada: $rfidUid",
                            color = BankSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                        )
                    }

                    Button(
                        onClick = { btManager.simulateCardRemove() },
                        colors = ButtonDefaults.buttonColors(containerColor = BankAlert),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Retirar Tarjeta")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Simulación de Teclado Matricial 4x4
            Text(
                text = "2. Teclado Matricial 4x4 (Ingreso de PIN):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (!isCardIn) "Primero inserte una tarjeta arriba para habilitar el teclado." else "Clave correcta: 1234 (o prueba una incorrecta para probar el bloqueo)",
                style = MaterialTheme.typography.bodySmall,
                color = if (!isCardIn) BankAlert else TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            val keys = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
                listOf('*', '0', '#')
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { key ->
                            Button(
                                onClick = { btManager.simulateKeypress(key) },
                                enabled = isCardIn && !isLocked,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (key == '*' || key == '#') MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Text(
                                    text = if (key == '*') "Borrar (*)" else if (key == '#') "Enter (#)" else key.toString(),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (key == '*' || key == '#') 11.sp else 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { btManager.resetAtmState() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Reiniciar Estado del Prototipo")
            }
        }
    }
}
