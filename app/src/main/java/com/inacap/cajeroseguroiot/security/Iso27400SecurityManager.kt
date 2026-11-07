package com.inacap.cajeroseguroiot.security

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Módulo de seguridad basado en la norma ISO 27400 (Seguridad en IoT).
 * Aquí gestionamos el cifrado del PIN, la validación de tramas por Bluetooth
 * y el control de los intentos fallidos para evitar ataques de fuerza bruta.
 */
object Iso27400SecurityManager {

    // Clave secreta fija para robustecer el hash del PIN
    private const val SALT = "INACAP_IOT_ATM_SECURE_SALT_2026"
    private var failedAttempts = 0
    private const val MAX_ATTEMPTS = 3
    private var isSystemLocked = false

    // Registro de eventos para auditoría de seguridad (Control ISO 27400 8.3)
    val auditLogs = mutableListOf<AuditLogEntry>()

    data class AuditLogEntry(
        val timestamp: String,
        val eventType: String,
        val details: String,
        val integrityHash: String
    )

    data class SecurityBreachMitigation(
        val code: String,
        val breachName: String,
        val severity: String,
        val attackVector: String,
        val technicalMitigation: String,
        val isoControl: String
    )

    // Brechas de seguridad identificadas en el cajero y su solución (Resolución criterio 1.1.2.2)
    val documentedBreaches = listOf(
        SecurityBreachMitigation(
            code = "BR-01",
            breachName = "Intercepción de datos por Bluetooth",
            severity = "Alta",
            attackVector = "Alguien cercano podría intentar capturar los datos inalámbricos que viajan entre el celular y el Arduino.",
            technicalMitigation = "No mandamos el PIN en texto plano por el aire. Usamos un hash SHA-256 con Salt y sumas de verificación para asegurar que nadie altere los datos.",
            isoControl = "ISO 27400 Cláusula 7.1: Protección de datos en tránsito."
        ),
        SecurityBreachMitigation(
            code = "BR-02",
            breachName = "Ataque de repetición (Replay)",
            severity = "Crítica",
            attackVector = "Un atacante podría capturar el comando de abrir compuerta y volver a mandarlo más tarde.",
            technicalMitigation = "A cada orden le agregamos un número identificador único (Nonce) que cambia en cada envío para que no se pueda reutilizar una orden anterior.",
            isoControl = "ISO 27400 Cláusula 6.2: Autenticación de entidades."
        ),
        SecurityBreachMitigation(
            code = "BR-03",
            breachName = "Fuerza bruta en el teclado",
            severity = "Media-Alta",
            attackVector = "Intentar adivinar el PIN de 4 dígitos probando muchas combinaciones seguidas.",
            technicalMitigation = "Pusimos un límite de 3 intentos máximos. Al tercer fallo el sistema se bloquea por completo y solo se desbloquea desde la app.",
            isoControl = "ISO 27400 Cláusula 6.1: Control de acceso y bloqueo por umbral."
        ),
        SecurityBreachMitigation(
            code = "BR-04",
            breachName = "Clonación de tarjeta RFID (Skimming)",
            severity = "Alta",
            attackVector = "Copiar el código UID de la tarjeta física con un lector cercano no autorizado.",
            technicalMitigation = "Usamos autenticación de doble factor (MFA). Tener la tarjeta física no sirve de nada si no se ingresa además el PIN secreto correcto.",
            isoControl = "ISO 27400 Cláusula 6.3: Autenticación multifactor obligatoria."
        )
    )

    // Calcula el hash SHA-256 del PIN agregándole el Salt
    fun hashPin(pin: String): String {
        val input = pin + SALT
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Arma la trama de comando agregando el Nonce y el Checksum XOR de verificación
    fun buildSecureFrame(command: String): String {
        val nonce = (System.currentTimeMillis() % 100000).toString()
        val payload = "CMD:$command:$nonce"
        var checksum = 0
        for (b in payload.toByteArray(Charsets.US_ASCII)) {
            checksum = checksum xor b.toInt()
        }
        val hexChecksum = "%02X".format(checksum)
        val frame = "$$payload#$hexChecksum\n"

        logSecurityEvent("SECURE_TX", "Comando enviado: $payload (Checksum: $hexChecksum)")
        return frame
    }

    // Control de los intentos de PIN para evitar fuerza bruta
    fun registerAttempt(isSuccess: Boolean): Pair<Boolean, String> {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        return if (isSuccess) {
            failedAttempts = 0
            isSystemLocked = false
            logSecurityEvent("AUTH_SUCCESS", "Acceso correcto a las $timestamp")
            Pair(true, "Acceso Autorizado")
        } else {
            failedAttempts++
            if (failedAttempts >= MAX_ATTEMPTS) {
                isSystemLocked = true
                logSecurityEvent("LOCKDOWN", "Alerta: Se superaron los $MAX_ATTEMPTS intentos permitidos. Cajero bloqueado.")
                Pair(false, "Cajero bloqueado por seguridad (3 intentos)")
            } else {
                val remaining = MAX_ATTEMPTS - failedAttempts
                logSecurityEvent("AUTH_FAIL", "Intento fallido $failedAttempts de $MAX_ATTEMPTS")
                Pair(false, "Clave errónea. Le quedan $remaining intento(s)")
            }
        }
    }

    fun isLocked(): Boolean = isSystemLocked

    fun resetLock() {
        failedAttempts = 0
        isSystemLocked = false
        logSecurityEvent("ADMIN_RESET", "Desbloqueo administrativo realizado desde la app.")
    }

    private fun logSecurityEvent(type: String, details: String) {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val hash = hashPin("$time:$type:$details").take(8)
        auditLogs.add(0, AuditLogEntry(time, type, details, hash))
        if (auditLogs.size > 50) {
            auditLogs.removeAt(auditLogs.lastIndex)
        }
    }
}
