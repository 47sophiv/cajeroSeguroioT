# Cajero Automático Seguro IoT con Android y Bluetooth HC-05

**Asignatura:** TI3042 - Internet of Things y Smart Devices  
**Evaluación:** Evaluación Formativa Unidad 2  
**Estudiante:** Sophia Caro  
**Modalidad:** Individual  
**Institución:** INACAP  

---

## 📌 Descripción del Proyecto

Este proyecto consiste en un sistema de Cajero Automático IoT que integra un microcontrolador **Arduino Uno** con una aplicación móvil desarrollada en **Android Studio** (Kotlin + Jetpack Compose) a través de comunicación inalámbrica **Bluetooth (HC-05)**, aplicando los principios de seguridad de la norma **ISO/IEC 27400:2022**.

---

## 📱 Características de la Aplicación Android

- **Lenguaje y UI:** Kotlin con Jetpack Compose y Material 3.
- **Conectividad:** Socket RFCOMM / perfil Bluetooth SPP hacia el módulo HC-05.
- **Pestaña Sensores (Lectura):**
  - Lectura en tiempo real del UID de tarjetas RFID (RC522).
  - Lectura de pulsaciones del teclado matricial 4x4 (ocultamiento seguro con asteriscos).
  - Réplica virtual del estado de la pantalla LCD 16x2.
  - Consola de telemetría en vivo con logs de eventos.
- **Pestaña Actuadores (Escritura):**
  - Control de apertura de la compuerta con el servomotor SG90 (180° dispensar dinero, 90° reposo, 0° alerta).
  - Autorización remota de retiros de dinero.
  - Bloqueo y desbloqueo de emergencia del cajero.
- **Modo Simulación:**
  - Permite probar el funcionamiento completo (deslizar tarjetas virtuales, teclear claves y simular el servomotor) directamente desde la app sin tener el circuito físico conectado.
- **Seguridad ISO/IEC 27400:**
  - Hashing seguro SHA-256 + Salt para contraseñas.
  - Tramas protegidas con Nonce y Checksum XOR contra ataques de repetición.
  - Bloqueo por fuerza bruta al tercer intento fallido.
  - Doble factor de autenticación (Tarjeta RFID + PIN).

---

## 🔌 Hardware y Conexiones Arduino

El código de Arduino se encuentra en la carpeta `arduino/CajeroSeguroIoT_Bluetooth.ino`.

| Componente | Pines Arduino | Función |
| :--- | :--- | :--- |
| **Módulo Bluetooth HC-05** | RX -> Pin A1, TX -> Pin A0 | Comunicación serial inalámbrica a 9600 baudios |
| **Lector RFID-RC522** | SDA: Pin 10, SCK: Pin 13, MOSI: Pin 11, MISO: Pin 12, RST: Pin 9 | Lectura de tarjeta del cliente |
| **Teclado Matricial 4x4** | Filas: Pines analógicos/digitales según esquemático | Ingreso de clave secreta (PIN) |
| **Servomotor SG90** | Pin 8 (PWM) | Apertura de compuerta para entrega de dinero |
| **Pantalla LCD 16x2 (I2C/Paralelo)** | Pines 2 al 7 | Mensajes de estado al usuario |

---

## 🚀 Cómo Ejecutar el Proyecto

### 1. En Arduino:
1. Abrir `arduino/CajeroSeguroIoT_Bluetooth.ino` en Arduino IDE.
2. Cargar el sketch en el microcontrolador Arduino Uno.
3. Conectar el módulo HC-05 a los pines A0 y A1 (desconectando RX/TX durante la carga si comparte pines con el puerto serie).

### 2. En Android Studio:
1. Abrir la carpeta `CajeroSeguroIoT` en Android Studio.
2. Compilar y ejecutar en un dispositivo Android físico o en el emulador.
3. Para probar con hardware físico: emparejar el HC-05 en los ajustes de Bluetooth del teléfono y pulsar **"Buscar Dispositivos"** en la app.
4. Para probar sin hardware: pulsar el botón **"Modo Simulación"** para interactuar con todos los sensores y actuadores de forma virtual.
