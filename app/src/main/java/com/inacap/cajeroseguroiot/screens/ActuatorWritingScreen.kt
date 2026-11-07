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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inacap.cajeroseguroiot.bluetooth.BluetoothConnectionManager
import com.inacap.cajeroseguroiot.ui.theme.*

/**
 * Pantalla de Activador (Criterio 2.1.1.2 - 10 puntos)
 * Aplica principios de Material Design e integra elementos interactivos:
 * - Slider: Control manual analógico del servomotor SG90 (0° a 180°).
 * - RadioButtons: Selección de posiciones predefinidas de la compuerta.
 * - Switch: Interruptor de Bloqueo de Emergencia en tiempo real.
 * - EditTexts (OutlinedTextField): Monto a retirar y envío de comandos seriales personalizados.
 */
@Composable
fun ActuatorWritingScreen(btManager: BluetoothConnectionManager) {
    val servoAngle = btManager.servoAngle.intValue
    val isLocked = btManager.isAtmLocked.value

    var withdrawAmount by remember { mutableStateOf("50000") }
    var customCommandText by remember { mutableStateOf("") }
    var sliderValue by remember { mutableFloatStateOf(servoAngle.toFloat()) }
    var selectedPresetRadio by remember { mutableStateOf("90") }

    // Sincronizar el slider si el ángulo cambia externamente
    LaunchedEffect(servoAngle) {
        sliderValue = servoAngle.toFloat()
        selectedPresetRadio = when (servoAngle) {
            0 -> "0"
            180 -> "180"
            else -> "90"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Título de la pantalla
        Text(
            text = "Pantalla de Activador (Control Remoto)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Envío de datos hacia actuadores IoT mediante elementos interactivos (Criterio 2.1.1.2)",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Interruptor Switch de Bloqueo de Emergencia (Criterio 2.1.1.2)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isLocked) BankAlert.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Interruptor de Bloqueo de Emergencia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) BankAlert else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isLocked) "SISTEMA BLOQUEADO (Acceso denegado)" else "Sistema operativo normal",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Switch interactivo
                Switch(
                    checked = isLocked,
                    onCheckedChange = { activateLock ->
                        if (activateLock) {
                            btManager.sendActuatorCommand("LOCK_SYSTEM")
                        } else {
                            btManager.sendActuatorCommand("RESET_SYSTEM")
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = BankAlert)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Control del Servomotor SG90 con Slider y RadioButtons (Criterio 2.1.1.2)
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
                            text = "Actuador de compuerta en Pin 8 de Arduino",
                            style = MaterialTheme.typography.bodySmall,
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
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Deslizador interactivo Slider (Criterio 2.1.1.2)
                Text(
                    text = "Control Analógico mediante Slider: ${sliderValue.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        sliderValue = newValue
                        btManager.setCustomServoAngle(newValue.toInt())
                    },
                    valueRange = 0f..180f,
                    steps = 17, // Pasos cada 10 grados
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = BankPrimary,
                        activeTrackColor = BankPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Selección mediante RadioButtons (Criterio 2.1.1.2)
                Text(
                    text = "Posiciones Predefinidas (RadioButtons):",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPresetRadio == "0",
                            onClick = {
                                selectedPresetRadio = "0"
                                btManager.sendActuatorCommand("SERVO_ALERT")
                            }
                        )
                        Text("0° Alerta", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPresetRadio == "90",
                            onClick = {
                                selectedPresetRadio = "90"
                                btManager.sendActuatorCommand("SERVO_IDLE")
                            }
                        )
                        Text("90° Reposo", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPresetRadio == "180",
                            onClick = {
                                selectedPresetRadio = "180"
                                btManager.sendActuatorCommand("SERVO_OPEN")
                            }
                        )
                        Text("180° Dispensar", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botones directos de activación
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

        // 3. Retiro Móvil con EditText interactivo (Criterio 2.1.1.2)
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
                    text = "Autoriza la apertura de la compuerta tras validación móvil.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // EditText interactivo para monto
                OutlinedTextField(
                    value = withdrawAmount,
                    onValueChange = { withdrawAmount = it },
                    label = { Text("Monto a Retirar ($ CLP)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
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

        // 4. Envío de Comandos Seriales Personalizados (EditText adicional)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Consola de Comandos hacia Arduino",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Permite enviar órdenes personalizadas al microcontrolador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customCommandText,
                    onValueChange = { customCommandText = it },
                    label = { Text("Comando (Ej: CMD:SERVO_OPEN)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (customCommandText.isNotBlank()) {
                            btManager.sendActuatorCommand(customCommandText.trim())
                            customCommandText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BankPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Transmitir Comando Inalámbrico")
                }
            }
        }
    }
}
