# Proyecto Cajero Automatico Seguro IoT - Unidad 2

Estudiante: Sophia Caro  
Asignatura: TI3042 - Internet of Things y Smart Devices  
Evaluacion: Evaluacion Formativa 2  
Modalidad: Individual  
Sede: INACAP  

---

## Descripcion del proyecto

En este proyecto desarrolle una aplicacion movil en Android Studio (Kotlin con Jetpack Compose) que se comunica por Bluetooth con una placa Arduino Uno para controlar el prototipo de cajero automatico seguro.

La aplicacion permite recibir en tiempo real los datos de los sensores (tarjetas RFID y pulsaciones del teclado matricial) y tambien enviar comandos de forma inalambrica para controlar los actuadores (servomotor de dispensacion de dinero, pantalla LCD y bloqueo de emergencia), aplicando medidas de ciberseguridad basadas en la norma ISO/IEC 27400:2022.

---

## Modulos y funcionalidades de la app

1. Conexion Bluetooth:
   - Permite escanear y conectarse al modulo Bluetooth HC-05 del Arduino mediante el perfil SPP / socket RFCOMM.
   - Cuenta con un "Modo Simulacion" para poder probar y demostrar todas las pantallas, sensores y actuadores directamente en la app o emulador sin necesidad de tener el circuito conectado.

2. Pestaña de Sensores (Lectura inalambrica):
   - Muestra el identificador UID de la tarjeta RFID leida por el sensor RC522.
   - Muestra las teclas presionadas en el teclado matricial 4x4 (ocultando los digitos con asteriscos por seguridad).
   - Replica de manera virtual el estado y los mensajes de la pantalla LCD 16x2.
   - Registro de telemetria en vivo con la hora y detalle de cada evento recibido.

3. Pestaña de Actuadores (Escritura inalambrica):
   - Control del servomotor SG90 que simula la compuerta de entrega de dinero: apertura a 180 grados, reposo a 90 grados y posicion de alerta a 0 grados.
   - Boton para autorizar retiro de dinero directamente desde la aplicacion movil.
   - Boton de bloqueo de emergencia que bloquea el cajero ante cualquier situacion sospechosa, con opcion de desbloqueo mediante PIN de administrador.

4. Pestaña de Seguridad (ISO/IEC 27400:2022):
   - Proteccion contra robo de contraseñas: hashing de clave PIN mediante SHA-256 agregando un Salt aleatorio.
   - Proteccion contra ataques de repeticion (Replay Attacks): envio de tramas de control con identificador unico (Nonce) y suma de verificacion Checksum XOR.
   - Proteccion contra fuerza bruta: bloqueo del sistema al registrarse 3 intentos incorrectos consecutivos.
   - Doble factor de autenticacion (MFA): requiere lectura de tarjeta RFID valida mas la clave PIN en el teclado.

---

## Conexiones del circuito Arduino

El codigo del microcontrolador se encuentra en la carpeta `arduino/CajeroSeguroIoT_Bluetooth.ino`.

- Modulo Bluetooth HC-05: pines A0 (TX) y A1 (RX) utilizando SoftwareSerial a 9600 baudios.
- Lector RFID RC522: conectado a los pines del bus SPI (pin 10 SDA, pin 13 SCK, pin 11 MOSI, pin 12 MISO, pin 9 RST).
- Servomotor SG90: conectado al pin digital 8 (PWM).
- Pantalla LCD 16x2: pines digitales 2 al 7.
- Teclado matricial 4x4: pines digitales y analogicos de entrada.

---

## Instrucciones para ejecutar el proyecto

1. Parte Arduino:
   - Abrir el archivo `arduino/CajeroSeguroIoT_Bluetooth.ino` en Arduino IDE.
   - Seleccionar la placa Arduino Uno y el puerto COM correspondiente.
   - Cargar el programa a la placa (desconectar los cables RX/TX del HC-05 al momento de cargar si comparten pines de programacion).

2. Parte Android:
   - Abrir la carpeta `CajeroSeguroIoT` en Android Studio.
   - Esperar a que sincronice Gradle y presionar "Run app" (Shift + F10) en un emulador o celular Android.
   - O bien instalar directamente el archivo ejecutable `CajeroSeguroIoT.apk`.

3. Prueba con hardware:
   - Encender el circuito del Arduino.
   - Ir a los Ajustes de Bluetooth del celular y vincular el HC-05 (clave por defecto 1234).
   - En la app, presionar "Buscar Dispositivos", seleccionar HC-05 y conectarse.

4. Prueba sin hardware (Modo Simulacion):
   - Abrir la aplicacion y en la pantalla de conexion presionar el boton "Modo Simulacion".
   - Esto habilita controles interactivos para simular el deslizamiento de tarjetas RFID, el ingreso de claves en el teclado y observar la respuesta del servomotor en tiempo real.
