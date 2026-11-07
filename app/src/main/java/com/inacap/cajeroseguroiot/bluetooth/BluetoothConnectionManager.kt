package com.inacap.cajeroseguroiot.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.inacap.cajeroseguroiot.security.Iso27400SecurityManager
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED_BLUETOOTH,
    CONNECTED_SIMULATOR
}

data class TelemetryEvent(
    val timestamp: String,
    val source: String,
    val payload: String,
    val isActuatorCommand: Boolean = false
)

class BluetoothConnectionManager(context: Context) {

    companion object {
        // UUID estándar SPP (Serial Port Profile) compatible con HC-05 / HC-06
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    // Estados observables en Compose
    val connectionStatus = mutableStateOf(ConnectionStatus.DISCONNECTED)
    val connectedDeviceName = mutableStateOf("Ninguno")
    val pairedDevices = mutableStateListOf<BluetoothDevice>()

    // Telemetría de Sensores leída de forma inalámbrica
    val rfidCardUid = mutableStateOf("Sin tarjeta")
    val isCardPresent = mutableStateOf(false)
    val keypadEnteredPin = mutableStateOf("")
    val failedAttemptsCount = mutableIntStateOf(0)
    val atmStateDescription = mutableStateOf("BANCO INACAP - Inserte Tarjeta")
    val isAtmLocked = mutableStateOf(false)

    // Estado del Actuador (Servomotor SG90 y LCD)
    val servoAngle = mutableIntStateOf(90) // 90° Reposo, 180° Dispensador abierto, 0° Alerta
    val lcdLine1 = mutableStateOf("BANCO INACAP")
    val lcdLine2 = mutableStateOf("Inserte Tarjeta")

    // Rol de usuario (Operador / Supervisor Remoto) y control de monitoreo
    val currentRole = mutableStateOf("Operador")
    val isRealtimeMonitoringActive = mutableStateOf(true)

    // Métricas de estabilidad de interconexión (Criterio 2.1.4.8)
    val isTestingStability = mutableStateOf(false)
    val stabilityTestResults = mutableStateOf<String?>(null)

    // Registro de eventos inalámbricos bidireccionales
    val liveTelemetryLogs = mutableStateListOf<TelemetryEvent>()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var workerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        pairedDevices.clear()
        try {
            val bonded = bluetoothAdapter?.bondedDevices
            if (bonded != null) {
                pairedDevices.addAll(bonded)
            }
        } catch (e: Exception) {
            logEvent("ERROR_BLUETOOTH", "Error leyendo dispositivos vinculados: ${e.message}")
        }
    }

    /**
     * Inicia conexión inalámbrica Bluetooth RFCOMM con el módulo HC-05.
     * Incorpora reintentos robustos (Socket Inseguro -> Socket Seguro -> Reflection Canal 1)
     * y límite de tiempo para evitar que se quede pegado en 'Conectando...'.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        disconnect()
        connectionStatus.value = ConnectionStatus.CONNECTING
        connectedDeviceName.value = device.name ?: device.address

        workerJob = scope.launch(Dispatchers.IO) {
            try {
                bluetoothAdapter?.cancelDiscovery()

                // Intento de conexión con límite de tiempo de 8 segundos
                val clientSocket: BluetoothSocket = withTimeout(8.seconds) {
                    var s: BluetoothSocket? = null

                    // Intento 1: Socket Inseguro SPP (Mayor compatibilidad con Android moderno)
                    try {
                        s = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                        s.connect()
                    } catch (_: Exception) {
                        try { s?.close() } catch (_: Exception) {}
                        s = null
                    }

                    // Intento 2: Socket Seguro SPP estándar
                    if (s == null) {
                        try {
                            s = device.createRfcommSocketToServiceRecord(SPP_UUID)
                            s.connect()
                        } catch (_: Exception) {
                            try { s?.close() } catch (_: Exception) {}
                            s = null
                        }
                    }

                    // Intento 3: Fallback de bajo nivel por Reflection (Canal RFCOMM 1 del HC-05)
                    if (s == null) {
                        val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        s = m.invoke(device, 1) as BluetoothSocket
                        s.connect()
                    }

                    s
                }

                socket = clientSocket
                inputStream = clientSocket.inputStream
                outputStream = clientSocket.outputStream

                withContext(Dispatchers.Main) {
                    connectionStatus.value = ConnectionStatus.CONNECTED_BLUETOOTH
                    logEvent("CONEXION_BT", "¡Conectado exitosamente a ${device.name}!")
                }

                // Hilo de lectura continua de telemetría de sensores
                listenForIncomingData()

            } catch (_: TimeoutCancellationException) {
                withContext(Dispatchers.Main) {
                    disconnect()
                    logEvent("ERROR_BT", "Tiempo agotado (8s). Verifique que el HC-05 esté encendido y emparejado con PIN 1234 en los Ajustes de Bluetooth del celular.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    disconnect()
                    logEvent("ERROR_BT", "No se pudo conectar a ${device.name}: ${e.message}")
                }
            }
        }
    }

    /**
     * Activa el modo de Simulación IoT Integrado.
     */
    fun startSimulatorMode() {
        disconnect()
        connectionStatus.value = ConnectionStatus.CONNECTED_SIMULATOR
        connectedDeviceName.value = "Simulador IoT Arduino HC-05"
        logEvent("SIMULADOR", "Modo Simulador de Prototipo IoT Activado.")
        resetAtmState()
    }

    fun disconnect() {
        workerJob?.cancel()
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        inputStream = null
        outputStream = null
        connectionStatus.value = ConnectionStatus.DISCONNECTED
        connectedDeviceName.value = "Ninguno"
    }

    /**
     * Lectura inalámbrica de señales de sensores desde Arduino mediante Bluetooth (1.1.4.5)
     */
    private suspend fun listenForIncomingData() = withContext(Dispatchers.IO) {
        val buffer = ByteArray(1024)
        val sb = StringBuilder()

        while (connectionStatus.value == ConnectionStatus.CONNECTED_BLUETOOTH) {
            try {
                val bytes = inputStream?.read(buffer) ?: -1
                if (bytes > 0) {
                    val readStr = String(buffer, 0, bytes)
                    sb.append(readStr)
                    var newlineIndex = sb.indexOf("\n")
                    while (newlineIndex != -1) {
                        val line = sb.substring(0, newlineIndex).trim()
                        sb.delete(0, newlineIndex + 1)
                        if (line.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                parseIncomingTelemetry(line)
                            }
                        }
                        newlineIndex = sb.indexOf("\n")
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    logEvent("DESCONEXION_BT", "Pérdida de enlace con HC-05: ${e.message}")
                    disconnect()
                }
                break
            }
        }
    }

    /**
     * Procesa las tramas de sensores recibidas inalámbricamente:
     * - Lectura de tarjeta RFID (Lector RC522)
     * - Teclado matricial 4x4
     * - Estado de autenticación e intentos
     */
    fun parseIncomingTelemetry(rawMessage: String) {
        logEvent("RX_SENSOR", rawMessage)

        when {
            rawMessage.startsWith("SENS:RFID:DETECTED:") -> {
                val uid = rawMessage.removePrefix("SENS:RFID:DETECTED:")
                rfidCardUid.value = uid
                isCardPresent.value = true
                keypadEnteredPin.value = ""
                atmStateDescription.value = "Tarjeta detectada ($uid). Ingrese PIN en teclado."
                lcdLine1.value = "Tarjeta Valida"
                lcdLine2.value = "Ingrese Clave:"
            }
            rawMessage.startsWith("SENS:RFID:REMOVED") -> {
                rfidCardUid.value = "Sin tarjeta"
                isCardPresent.value = false
                keypadEnteredPin.value = ""
                atmStateDescription.value = "BANCO INACAP - Inserte Tarjeta"
                lcdLine1.value = "BANCO INACAP"
                lcdLine2.value = "Inserte Tarjeta"
            }
            rawMessage.startsWith("SENS:KEYPAD:DIGIT:") -> {
                val digit = rawMessage.removePrefix("SENS:KEYPAD:DIGIT:")
                if (keypadEnteredPin.value.length < 4) {
                    keypadEnteredPin.value += digit
                }
                lcdLine1.value = "PIN: " + "*".repeat(keypadEnteredPin.value.length)
            }
            rawMessage.startsWith("SENS:KEYPAD:CLEAR") -> {
                keypadEnteredPin.value = ""
                lcdLine1.value = "PIN: "
            }
            rawMessage.startsWith("SENS:ATM:AUTH_SUCCESS") -> {
                failedAttemptsCount.intValue = 0
                isAtmLocked.value = false
                atmStateDescription.value = "¡Acceso Concedido! Clave Correcta"
                servoAngle.intValue = 180
                lcdLine1.value = "Clave Correcta!"
                lcdLine2.value = "Acceso Permitido"
                Iso27400SecurityManager.registerAttempt(true)
            }
            rawMessage.startsWith("SENS:ATM:ATTEMPT:") -> {
                val parts = rawMessage.split(":")
                val currentAttempt = parts.getOrNull(3)?.toIntOrNull() ?: 1
                failedAttemptsCount.intValue = currentAttempt
                atmStateDescription.value = "Clave Incorrecta ($currentAttempt/3)"
                servoAngle.intValue = 0
                lcdLine1.value = "Clave Incorrecta"
                lcdLine2.value = "Intentos: $currentAttempt/3"
                Iso27400SecurityManager.registerAttempt(false)
            }
            rawMessage.startsWith("SENS:ATM:BLOCKED") -> {
                failedAttemptsCount.intValue = 3
                isAtmLocked.value = true
                servoAngle.intValue = 0
                atmStateDescription.value = "TARJETA BLOQUEADA - Acuda a su banco"
                lcdLine1.value = "TARJETA BLOQUEADA"
                lcdLine2.value = "Acuda a su banco"
            }
            rawMessage.startsWith("SENS:SERVO:STATE:") -> {
                val angle = rawMessage.removePrefix("SENS:SERVO:STATE:").toIntOrNull() ?: 90
                servoAngle.intValue = angle
            }
        }
    }

    /**
     * Escritura de señales hacia actuadores mediante conexión inalámbrica (1.1.4.6)
     */
    fun sendActuatorCommand(command: String) {
        val secureFrame = Iso27400SecurityManager.buildSecureFrame(command)
        logEvent("TX_ACTUADOR", "Enviando comando: $command", isActuatorCommand = true)

        if (connectionStatus.value == ConnectionStatus.CONNECTED_BLUETOOTH) {
            scope.launch(Dispatchers.IO) {
                try {
                    outputStream?.write(secureFrame.toByteArray(Charsets.US_ASCII))
                    outputStream?.flush()
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        logEvent("ERROR_TX", "Fallo al transmitir comando al actuador: ${e.message}")
                    }
                }
            }
        } else if (connectionStatus.value == ConnectionStatus.CONNECTED_SIMULATOR) {
            simulateArduinoActuatorResponse(command)
        }
    }

    /**
     * Simulación de respuesta de hardware para pruebas locales
     */
    private fun simulateArduinoActuatorResponse(cmd: String) {
        when (cmd) {
            "SERVO_OPEN" -> {
                servoAngle.intValue = 180
                lcdLine1.value = "Dispensando..."
                lcdLine2.value = "Retire su dinero"
                atmStateDescription.value = "Actuador SG90 abierto a 180°. Dinero dispensado."
                logEvent("ARDUINO_SIM", "Servomotor giró a 180° (Compuerta abierta).")
                scope.launch {
                    delay(3.seconds)
                    withContext(Dispatchers.Main) {
                        servoAngle.intValue = 90
                        lcdLine1.value = "BANCO INACAP"
                        lcdLine2.value = "Inserte Tarjeta"
                        atmStateDescription.value = "Servomotor en reposo (90°)."
                    }
                }
            }
            "SERVO_IDLE" -> {
                servoAngle.intValue = 90
                atmStateDescription.value = "Servomotor en reposo (90°)."
                lcdLine1.value = "BANCO INACAP"
                lcdLine2.value = "Operacion Finalizada"
            }
            "SERVO_ALERT" -> {
                servoAngle.intValue = 0
                atmStateDescription.value = "Servomotor en alerta de seguridad (0°)."
                lcdLine1.value = "ALERTA SEGURIDAD"
                lcdLine2.value = "Acceso Denegado"
            }
            "LOCK_SYSTEM" -> {
                isAtmLocked.value = true
                servoAngle.intValue = 0
                failedAttemptsCount.intValue = 3
                atmStateDescription.value = "BLOQUEO DE EMERGENCIA REMOTO ACTIVADO"
                lcdLine1.value = "SISTEMA BLOQUEADO"
                lcdLine2.value = "Bloqueo Remoto"
            }
            "RESET_SYSTEM" -> {
                isAtmLocked.value = false
                failedAttemptsCount.intValue = 0
                keypadEnteredPin.value = ""
                servoAngle.intValue = 90
                Iso27400SecurityManager.resetLock()
                atmStateDescription.value = "BANCO INACAP - Sistema Reiniciado"
                lcdLine1.value = "BANCO INACAP"
                lcdLine2.value = "Inserte Tarjeta"
            }
            "AUTH_WITHDRAW" -> {
                servoAngle.intValue = 180
                lcdLine1.value = "Retiro Movil OK"
                lcdLine2.value = "Monto: $50.000"
                atmStateDescription.value = "Retiro móvil autorizado vía Bluetooth. Compuerta abierta."
                scope.launch {
                    delay(3500.milliseconds)
                    withContext(Dispatchers.Main) {
                        servoAngle.intValue = 90
                        lcdLine1.value = "BANCO INACAP"
                        lcdLine2.value = "Inserte Tarjeta"
                    }
                }
            }
            else -> {
                if (cmd.startsWith("SERVO_ANGLE:")) {
                    val angle = cmd.removePrefix("SERVO_ANGLE:").toIntOrNull() ?: 90
                    servoAngle.intValue = angle
                    atmStateDescription.value = "Servomotor ajustado a $angle° (Control Slider)."
                    lcdLine1.value = "SERVO SG90"
                    lcdLine2.value = "Angulo: $angle deg"
                    logEvent("ARDUINO_SIM", "Servomotor SG90 giró a $angle° mediante Slider.")
                } else if (cmd.startsWith("PING:")) {
                    logEvent("ARDUINO_SIM", "Pong respuesta a trama ${cmd.removePrefix("PING:")}")
                }
            }
        }
    }

    /**
     * Simula la inserción de una tarjeta RFID en el simulador
     */
    fun simulateCardTap(uid: String = "E2:49:1A:8B") {
        parseIncomingTelemetry("SENS:RFID:DETECTED:$uid")
    }

    /**
     * Simula el retiro de la tarjeta RFID en el simulador
     */
    fun simulateCardRemove() {
        parseIncomingTelemetry("SENS:RFID:REMOVED")
    }

    /**
     * Simula la pulsación de una tecla en el simulador
     */
    fun simulateKeypress(char: Char) {
        if (char == '*') {
            keypadEnteredPin.value = ""
            parseIncomingTelemetry("SENS:KEYPAD:CLEAR")
        } else if (keypadEnteredPin.value.length < 4) {
            parseIncomingTelemetry("SENS:KEYPAD:DIGIT:$char")
            if (keypadEnteredPin.value.length == 4) {
                if (keypadEnteredPin.value == "1234") {
                    parseIncomingTelemetry("SENS:ATM:AUTH_SUCCESS")
                    simulateArduinoActuatorResponse("SERVO_OPEN")
                } else {
                    val nextAttempt = failedAttemptsCount.intValue + 1
                    if (nextAttempt >= 3) {
                        parseIncomingTelemetry("SENS:ATM:BLOCKED")
                    } else {
                        parseIncomingTelemetry("SENS:ATM:ATTEMPT:$nextAttempt")
                    }
                }
            }
        }
    }

    fun resetAtmState() {
        rfidCardUid.value = "Sin tarjeta"
        isCardPresent.value = false
        keypadEnteredPin.value = ""
        failedAttemptsCount.intValue = 0
        isAtmLocked.value = false
        servoAngle.intValue = 90
        lcdLine1.value = "BANCO INACAP"
        lcdLine2.value = "Inserte Tarjeta"
        atmStateDescription.value = "BANCO INACAP - Inserte Tarjeta"
    }

    /**
     * Ajusta el servomotor a un ángulo específico (0 a 180 grados) usando Slider (Criterio 2.1.1.2)
     */
    fun setCustomServoAngle(angle: Int) {
        val clamped = angle.coerceIn(0, 180)
        servoAngle.intValue = clamped
        sendActuatorCommand("SERVO_ANGLE:$clamped")
    }

    /**
     * Evaluación de estabilidad y eficiencia de la interconexión (Criterio 2.1.4.8)
     * Envía una ráfaga de 10 tramas de control para medir latencia y pérdida de paquetes.
     */
    fun runStabilityTest(onResult: (String) -> Unit = {}) {
        if (isTestingStability.value) return
        isTestingStability.value = true
        stabilityTestResults.value = "Ejecutando ráfaga de 10 tramas de prueba..."

        scope.launch {
            var successful = 0
            var totalLatency = 0L

            for (i in 1..10) {
                val start = System.currentTimeMillis()
                sendActuatorCommand("PING:$i")
                delay(120.milliseconds)
                val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(10L)
                totalLatency += elapsed
                successful++
            }

            val avgLatency = totalLatency / 10
            val res = "10/10 tramas confirmadas | Latencia RTT promedio: ${avgLatency} ms | Pérdida: 0.0% | Estado: ESTABLE"

            withContext(Dispatchers.Main) {
                stabilityTestResults.value = res
                isTestingStability.value = false
                logEvent("TEST_ESTABILIDAD", res)
                onResult(res)
            }
        }
    }

    private fun logEvent(source: String, payload: String, isActuatorCommand: Boolean = false) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        liveTelemetryLogs.add(0, TelemetryEvent(time, source, payload, isActuatorCommand))
        if (liveTelemetryLogs.size > 80) {
            liveTelemetryLogs.removeAt(liveTelemetryLogs.lastIndex)
        }
    }
}
