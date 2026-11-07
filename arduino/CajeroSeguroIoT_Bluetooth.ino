/*
  Proyecto: Cajero Automático Seguro IoT con Arduino y Bluetooth
  Asignatura: TI3042 - Internet de las Cosas y Smart Devices (INACAP)
  Estudiante: Sophia Caro (Entrega Individual)
  Unidad 2: Interconexión Inalámbrica con App Android
  
  Descripción del proyecto:
  Este programa controla un prototipo de cajero automático usando un lector RFID (RC522),
  un teclado matricial para ingresar el PIN, una pantalla LCD 16x2 y un servomotor SG90
  que funciona como la compuerta dispensadora de dinero.
  
  Para esta entrega corregimos las observaciones de la Unidad 1 agregando la comunicación
  inalámbrica con el módulo Bluetooth HC-05 (usando SoftwareSerial en los pines A0 y A1).
  De esta forma el cajero envía telemetría de sensores a la app de Android (tarjetas e intentos)
  y recibe comandos para abrir la compuerta, bloquearse o autorizar retiros móviles.

  Conexiones en Arduino Uno:
  - Módulo Bluetooth HC-05:
      VCC -> 5V
      GND -> GND
      TXD -> Pin A0 (RX en Arduino)
      RXD -> Pin A1 (TX en Arduino)
  - Lector RFID-RC522:
      3.3V -> Pin 3.3V (Importante: no conectar a 5V)
      GND  -> GND
      RST  -> Pin 9
      SDA (SS) -> Pin 10
      MOSI -> Pin 11
      MISO -> Pin 12
      SCK  -> Pin 13
  - Pantalla LCD 16x2:
      Pines RS, E, D4, D5, D6, D7 -> Pines 7, 6, 5, 4, 3, 2
  - Servomotor SG90:
      Señal (cable naranja) -> Pin 8 (PWM)
      VCC y GND -> 5V y GND
*/

#include <SPI.h>
#include <MFRC522.h>
#include <LiquidCrystal.h>
#include <Servo.h>
#include <SoftwareSerial.h>

// Conexión del Bluetooth HC-05 en pines analógicos como serial digital
#define BT_RX_PIN A0
#define BT_TX_PIN A1
SoftwareSerial BTSerial(BT_RX_PIN, BT_TX_PIN);

// Pines del Lector RFID RC522 (Bus SPI)
#define SS_PIN 10
#define RST_PIN 9
MFRC522 rfid(SS_PIN, RST_PIN);

// Configuración de la pantalla LCD (Pines 7, 6, 5, 4, 3, 2)
LiquidCrystal lcd(7, 6, 5, 4, 3, 2);

// Configuración del Servomotor SG90
#define SERVO_PIN 8
Servo miServo;

// Posiciones del servomotor (en grados)
const int POS_ALERTA   = 0;    // 0°: Posición de alerta / Acceso denegado
const int POS_CENTRO   = 90;   // 90°: Reposo / Compuerta cerrada
const int POS_DERECHA  = 180;  // 180°: Compuerta abierta / Dispensando dinero

// Clave correcta y límite de intentos para seguridad
const String CLAVE_CORRECTA = "1234";
const int MAX_INTENTOS = 3;

// Variables para controlar el estado del cajero
bool tarjetaDetectada = false;
String uidTarjetaActual = "";
String claveIngresada = "";
int intentosFallidos = 0;
bool sistemaBloqueado = false;

// Buffer para leer lo que llega por Bluetooth desde el celular
String comandoBluetooth = "";

void setup() {
  // Iniciamos la comunicación serial para depurar en el PC y con el Bluetooth
  Serial.begin(9600);
  BTSerial.begin(9600); // Velocidad estándar del módulo HC-05
  
  Serial.println(F("Cajero Seguro IoT iniciado correctamente"));
  BTSerial.println(F("SENS:ATM:INITIALIZED"));

  // Iniciamos el bus SPI y el lector RFID
  SPI.begin();
  rfid.PCD_Init();

  // Conectamos el servomotor y lo dejamos cerrado en reposo (90°)
  miServo.attach(SERVO_PIN);
  miServo.write(POS_CENTRO);

  // Iniciamos la pantalla LCD
  lcd.begin(16, 2);
  mostrarInicio();

  Serial.println(F("Esperando tarjeta o comandos por Bluetooth..."));
}

void loop() {
  // 1. Revisar si la app de Android nos mandó alguna orden por Bluetooth
  leerComandosBluetooth();

  // Si el sistema está bloqueado por seguridad no permitimos operar hasta desbloquearlo
  if (sistemaBloqueado) {
    return;
  }

  // 2. Leer si hay una tarjeta puesta en el lector RFID
  if (!tarjetaDetectada) {
    if (rfid.PICC_IsNewCardPresent() && rfid.PICC_ReadCardSerial()) {
      tarjetaDetectada = true;
      claveIngresada = "";
      intentosFallidos = 0;

      // Obtenemos el código UID de la tarjeta
      uidTarjetaActual = "";
      for (byte i = 0; i < rfid.uid.size; i++) {
        uidTarjetaActual += (rfid.uid.uidByte[i] < 0x10 ? "0" : "");
        uidTarjetaActual += String(rfid.uid.uidByte[i], HEX);
        if (i < rfid.uid.size - 1) uidTarjetaActual += ":";
      }
      uidTarjetaActual.toUpperCase();

      Serial.print(F("Tarjeta detectada UID: "));
      Serial.println(uidTarjetaActual);

      // Le avisamos a la app de Android por Bluetooth que hay tarjeta
      BTSerial.print(F("SENS:RFID:DETECTED:"));
      BTSerial.println(uidTarjetaActual);

      // Mostramos en la pantalla LCD física
      lcd.clear();
      lcd.print("Tarjeta Valida");
      lcd.setCursor(0, 1);
      lcd.print("Ingrese Clave:");

      delay(1200);
      lcd.clear();
      lcd.print("PIN: ");

      rfid.PICC_HaltA();
      rfid.PCD_StopCrypto1();
    }
  }

  // 3. Lectura de teclas ingresadas (por monitor serie o teclado físico)
  if (Serial.available()) {
    char c = Serial.read();
    procesarTecla(c);
  }
}

// Lee las órdenes inalámbricas que envía el celular
void leerComandosBluetooth() {
  while (BTSerial.available()) {
    char c = BTSerial.read();
    if (c == '\n' || c == '\r') {
      if (comandoBluetooth.length() > 0) {
        ejecutarOrdenBluetooth(comandoBluetooth);
        comandoBluetooth = "";
      }
    } else {
      comandoBluetooth += c;
    }
  }
}

// Ejecuta la acción física según el comando recibido desde la app
void ejecutarOrdenBluetooth(String trama) {
  trama.trim();
  Serial.print(F("Comando recibido por Bluetooth: "));
  Serial.println(trama);

  // Extraemos el nombre del comando si viene con formato seguro ($CMD:COMANDO:NONCE#CHECKSUM)
  String cmd = trama;
  if (trama.startsWith("$CMD:")) {
    int primerPunto = 5;
    int segundoPunto = trama.indexOf(':', primerPunto);
    if (segundoPunto != -1) {
      cmd = trama.substring(primerPunto, segundoPunto);
    } else {
      int hashIndex = trama.indexOf('#');
      cmd = (hashIndex != -1) ? trama.substring(primerPunto, hashIndex) : trama.substring(primerPunto);
    }
  }

  // Orden: Abrir la compuerta a 180° para entregar dinero
  if (cmd == "SERVO_OPEN") {
    Serial.println(F("Abriendo compuerta del dispensador (180 grados)..."));
    lcd.clear();
    lcd.print("Dispensando...");
    lcd.setCursor(0, 1);
    lcd.print("Retire su dinero");

    miServo.write(POS_DERECHA); // Giro a 180°
    BTSerial.println(F("SENS:SERVO:STATE:180"));
    delay(3000); // 3 segundos abierta

    miServo.write(POS_CENTRO);  // Vuelve a reposo 90°
    BTSerial.println(F("SENS:SERVO:STATE:90"));
    mostrarInicio();
  }
  // Orden: Volver a reposo (90°)
  else if (cmd == "SERVO_IDLE") {
    miServo.write(POS_CENTRO);
    BTSerial.println(F("SENS:SERVO:STATE:90"));
  }
  // Orden: Posición de alerta (0°)
  else if (cmd == "SERVO_ALERT") {
    miServo.write(POS_ALERTA);
    BTSerial.println(F("SENS:SERVO:STATE:0"));
  }
  // Orden: Bloqueo de emergencia remoto desde la app
  else if (cmd == "LOCK_SYSTEM") {
    sistemaBloqueado = true;
    miServo.write(POS_ALERTA);
    lcd.clear();
    lcd.print("SISTEMA BLOQUEADO");
    lcd.setCursor(0, 1);
    lcd.print("Bloqueo Remoto");
    BTSerial.println(F("SENS:ATM:BLOCKED"));
    Serial.println(F("Alerta: Cajero bloqueado remotamente desde la app"));
  }
  // Orden: Desbloqueo y reinicio del cajero
  else if (cmd == "RESET_SYSTEM") {
    sistemaBloqueado = false;
    tarjetaDetectada = false;
    claveIngresada = "";
    intentosFallidos = 0;
    miServo.write(POS_CENTRO);
    mostrarInicio();
    BTSerial.println(F("SENS:ATM:RESET_OK"));
    BTSerial.println(F("SENS:SERVO:STATE:90"));
    Serial.println(F("Cajero desbloqueado y listo"));
  }
  // Orden: Retiro móvil autorizado desde la app sin tarjeta física
  else if (cmd == "AUTH_WITHDRAW") {
    lcd.clear();
    lcd.print("Retiro Movil OK");
    lcd.setCursor(0, 1);
    lcd.print("Monto Autorizado");

    miServo.write(POS_DERECHA);
    BTSerial.println(F("SENS:SERVO:STATE:180"));
    delay(3500);

    miServo.write(POS_CENTRO);
    BTSerial.println(F("SENS:SERVO:STATE:90"));
    mostrarInicio();
  }
}

// Procesa cada dígito del PIN
void procesarTecla(char tecla) {
  if (tecla == '*') {
    claveIngresada = "";
    lcd.clear();
    lcd.print("PIN: ");
    BTSerial.println(F("SENS:KEYPAD:CLEAR"));
    return;
  }

  if (isDigit(tecla) && claveIngresada.length() < 4) {
    claveIngresada += tecla;
    lcd.print("*"); // Ocultamos el PIN con asteriscos en la pantalla

    // Enviamos el evento a la app
    BTSerial.print(F("SENS:KEYPAD:DIGIT:"));
    BTSerial.println(tecla);

    // Cuando completa los 4 dígitos verificamos
    if (claveIngresada.length() == 4) {
      delay(400);
      evaluarPin();
    }
  }
}

// Verifica si la clave ingresada es la correcta
void evaluarPin() {
  lcd.clear();

  if (claveIngresada == CLAVE_CORRECTA) {
    // Clave correcta: abrir compuerta y avisar a la app
    lcd.print("Clave Correcta!");
    lcd.setCursor(0, 1);
    lcd.print("Acceso Permitido");

    Serial.println(F("Clave correcta: Abriendo servomotor a 180 grados"));
    BTSerial.println(F("SENS:ATM:AUTH_SUCCESS"));
    BTSerial.println(F("SENS:SERVO:STATE:180"));

    miServo.write(POS_DERECHA); // Giro a 180°
    delay(3000);

    miServo.write(POS_CENTRO);  // Reposo a 90°
    BTSerial.println(F("SENS:SERVO:STATE:90"));

    tarjetaDetectada = false;
    claveIngresada = "";
    intentosFallidos = 0;
    mostrarInicio();

  } else {
    // Clave incorrecta: sumar intento y mover servo a 0° de alerta
    intentosFallidos++;
    miServo.write(POS_ALERTA);
    BTSerial.println(F("SENS:SERVO:STATE:0"));

    if (intentosFallidos < MAX_INTENTOS) {
      lcd.print("Clave Incorrecta");
      lcd.setCursor(0, 1);
      lcd.print("Intentos: ");
      lcd.print(intentosFallidos);
      lcd.print("/");
      lcd.print(MAX_INTENTOS);

      BTSerial.print(F("SENS:ATM:ATTEMPT:"));
      BTSerial.println(intentosFallidos);

      delay(2000);
      miServo.write(POS_CENTRO);
      BTSerial.println(F("SENS:SERVO:STATE:90"));

      claveIngresada = "";
      lcd.clear();
      lcd.print("PIN: ");
    } else {
      // Si se equivoca 3 veces bloqueamos el cajero
      sistemaBloqueado = true;
      lcd.print("TARJETA BLOQUEADA");
      lcd.setCursor(0, 1);
      lcd.print("Acuda a su banco");

      BTSerial.println(F("SENS:ATM:BLOCKED"));
      Serial.println(F("Alerta: Tarjeta bloqueada por 3 intentos fallidos"));
    }
  }
}

// Pantalla de inicio del cajero
void mostrarInicio() {
  lcd.clear();
  lcd.print("BANCO INACAP");
  lcd.setCursor(0, 1);
  lcd.print("Inserte Tarjeta");
}
