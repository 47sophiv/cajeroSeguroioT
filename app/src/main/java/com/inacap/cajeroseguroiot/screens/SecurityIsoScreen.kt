package com.inacap.cajeroseguroiot.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inacap.cajeroseguroiot.security.Iso27400SecurityManager
import com.inacap.cajeroseguroiot.ui.theme.*

@Composable
fun SecurityIsoScreen() {
    var testPin by remember { mutableStateOf("1234") }
    val hashedPin = remember(testPin) { Iso27400SecurityManager.hashPin(testPin) }
    val testFrame = remember(testPin) { Iso27400SecurityManager.buildSecureFrame("AUTH_TEST:$testPin") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Título de la sección
        Text(
            text = "Seguridad IoT: Estándar ISO 27400 & Mitigación OT",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Resolución integral de brechas de seguridad (1.1.2.2) y lineamientos de la norma ISO/IEC 27400 (Criterio 2.1.3)",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Demostración Criptográfica en Tiempo Real
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Demostración de Cifrado en Tránsito",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Prueba en vivo de la función Hash SHA-256 + Salt y trama protegida con Checksum XOR:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = testPin,
                    onValueChange = { if (it.length <= 6) testPin = it },
                    label = { Text("Probar PIN Secreto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Digest SHA-256 resultante:",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Text(
                    text = hashedPin,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = BankSuccess,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Trama Inalámbrica Segura (con Checksum XOR):",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Text(
                    text = testFrame.trim(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = BankAccentTeal,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Matriz de Brechas de Seguridad Documentadas y Mitigadas
        Text(
            text = "Matriz de Brechas y Mitigaciones Técnicas (ISO 27400)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Iso27400SecurityManager.documentedBreaches.forEach { breach ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[${breach.code}] ${breach.breachName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val sevColor = when (breach.severity) {
                            "CRÍTICA" -> BankAlert
                            "ALTA" -> Color(0xFFF97316)
                            else -> BankWarning
                        }
                        Surface(
                            color = sevColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = breach.severity,
                                color = sevColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Vector de Ataque: ${breach.attackVector}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Mitigación Implementada: ${breach.technicalMitigation}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BankPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = breach.isoControl,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = BankAccentTeal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Registro de Auditoría de Seguridad (ISO 27400 Cláusula 8.3)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Registro de Auditoría Criptográfica (Audit Logs)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(Iso27400SecurityManager.auditLogs) { log ->
                        Text(
                            text = "[${log.timestamp}] [${log.eventType}] ${log.details} (Hash: #${log.integrityHash})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
