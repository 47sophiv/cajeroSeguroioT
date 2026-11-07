package com.inacap.cajeroseguroiot.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inacap.cajeroseguroiot.bluetooth.BluetoothConnectionManager
import com.inacap.cajeroseguroiot.ui.theme.*

@Composable
fun ActuatorWritingScreen(btManager: BluetoothConnectionManager) {
    val servoAngle = btManager.servoAngle.value
    val isLocked = btManager.isAtmLocked.value
    var withdrawAmount by remember { mutableStateOf("50000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Título de la pantalla
        Text(
            text = "Control de Actuadores (Escritura Inalámbrica)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Transmisión de comandos inalámbricos hacia servomotor SG90 y periféricos (Criterio 1.1.4.6 y 2.1.2)",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta de Estado del Servomotor SG90
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
                            text = "Servomotor TowerPro SG90",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Actuador PWM en Pin 8 de Arduino",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    val (posLabel, posColor) = when (servoAngle) {
                        180 -> Pair("ABIERTO (180°)", BankSuccess)
                        0 -> Pair("ALERTA (0°)", BankAlert)
                        else -> Pair("REPOSO (90°)", BankPrimary)
                    }

                    Surface(
                        color = posColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = posLabel,
                            color = posColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Barra de visualización de ángulo de giro
                Text(
                    text = "Ángulo actual de rotación: $servoAngle°",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { servoAngle.toFloat() / 180f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = when (servoAngle) {
                        180 -> BankSuccess
                        0 -> BankAlert
                        else -> BankPrimary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de control manual del actuador
                Text(
                    text = "Comandos Inalámbricos de Servo:",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { btManager.sendActuatorCommand("SERVO_OPEN") },
                        colors = ButtonDefaults.buttonColors(containerColor = BankSuccess),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Abrir (180°)")
                    }

                    Button(
                        onClick = { btManager.sendActuatorCommand("SERVO_IDLE") },
                        colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reposo (90°)")
                    }

                    Button(
                        onClick = { btManager.sendActuatorCommand("SERVO_ALERT") },
                        colors = ButtonDefaults.buttonColors(containerColor = BankAlert),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Alerta (0°)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta de Retiro Móvil sin Tarjeta Física (Funcionalidad Avanzada de la App)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Retiro Móvil de Dinero (Sin Tarjeta)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Autoriza la apertura remota de la compuerta dispensadora tras validación de credenciales bancarias móviles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = withdrawAmount,
                    onValueChange = { withdrawAmount = it },
                    label = { Text("Monto a Retirar ($ CLP)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { btManager.sendActuatorCommand("AUTH_WITHDRAW") },
                    colors = ButtonDefaults.buttonColors(containerColor = BankAccentTeal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLocked,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Autorizar Retiro Vía Bluetooth",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta de Controles de Emergencia y Mantenimiento
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Control Remoto de Seguridad y Bloqueo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Envía tramas prioritarias de bloqueo ante intrusiones o reinicio administrativo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { btManager.sendActuatorCommand("LOCK_SYSTEM") },
                        colors = ButtonDefaults.buttonColors(containerColor = BankAlert),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Bloqueo Inmediato")
                    }

                    Button(
                        onClick = { btManager.sendActuatorCommand("RESET_SYSTEM") },
                        colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reinicio Seguro")
                    }
                }
            }
        }
    }
}
