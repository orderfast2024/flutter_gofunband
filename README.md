# GoFun Band Plugin

Un plugin de Flutter para integrar la funcionalidad de lectura de bandas GoFun en aplicaciones Android. Este plugin permite leer información de usuarios desde bandas NFC y gestionar transacciones de recarga.

## 📋 Tabla de Contenidos

- [Características](#características)
- [Instalación](#instalación)
- [Configuración](#configuración)
- [Uso](#uso)
- [API Reference](#api-reference)
- [Ejemplos](#ejemplos)
- [Requisitos](#requisitos)
- [Soporte](#soporte)

## ✨ Características

- 🔧 **Inicialización del Toolkit**: Configuración del entorno (Sandbox/Producción/Local)
- 📱 **Lectura de Bandas NFC**: Lectura de información de usuarios desde bandas MIFARE
- 💰 **Gestión de Balance**: Acceso al balance del usuario desde la banda
- 🔄 **Sincronización de Datos**: Sincronización automática de transacciones
- ⚡ **Manejo de Errores**: Callbacks para manejo de errores de lectura
- 🛡️ **Configuración Segura**: Configuración del dispositivo con API Key

## 🚀 Instalación

### 1. Agregar dependencia

Agrega el plugin a tu `pubspec.yaml`:

```yaml
dependencies:
  go_fun_band: ^0.0.1
```

### 2. Instalar dependencias

```bash
flutter pub get
```

### 3. Configuración Android

Asegúrate de que tu aplicación tenga los permisos necesarios en `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="true" />
```

## ⚙️ Configuración

### Inicialización

```dart
import 'package:go_fun_band/gofunband.dart';

final goFunBand = GoFunBand();
```

### Configurar el entorno

```dart
// Inicializar el toolkit
bool initialized = await goFunBand.initializeToolkit(GoBandEnvironment.SANDBOX);

if (initialized) {
  print('Toolkit inicializado correctamente');
} else {
  print('Error al inicializar el toolkit');
}
```

### Configurar el dispositivo

```dart
// Configurar con tu API Key
await goFunBand.configureDevice('tu-api-key-aqui');
```

## 📖 Uso

### Lectura de Bandas

```dart
// Verificar si hay un lector disponible
bool readerAvailable = await goFunBand.checkAvailableReader();

if (readerAvailable) {
  // Iniciar la lectura de bandas
  await goFunBand.startReader(
    tagReadCallback: (GoBandUser user) {
      print('Usuario leído: ${user.id}');
      print('Balance: \$${user.balance}');
      // Manejar la información del usuario
    },
    tagReadErrorCallback: (String error) {
      print('Error al leer la banda: $error');
      // Manejar el error
    },
  );
}
```

### Sincronización de Datos

```dart
// Sincronizar datos del toolkit
await goFunBand.syncToolkitData();
```

### Limpiar Handlers

```dart
// Remover handlers cuando ya no se necesiten
await goFunBand.removeHandlers();
```

## 📚 API Reference

### Clases Principales

#### `GoFunBand`

La clase principal para interactuar con el plugin.

#### `GoBandUser`

Modelo que representa un usuario leído desde la banda.

```dart
class GoBandUser {
  final String id;        // ID del usuario
  final double balance;   // Balance del usuario
}
```

#### `GoBandEnvironment`

Enum para especificar el entorno de trabajo.

```dart
enum GoBandEnvironment {
  SANDBOX,     // Entorno de pruebas
  PRODUCTION,  // Entorno de producción
  LOCAL        // Entorno local
}
```

### Métodos

#### `initializeToolkit(GoBandEnvironment environment)`

Inicializa el toolkit con el entorno especificado.

**Parámetros:**
- `environment`: Entorno de trabajo (SANDBOX, PRODUCTION, LOCAL)

**Retorna:** `Future<bool>` - `true` si la inicialización fue exitosa

#### `configureDevice(String apiKey)`

Configura el dispositivo con la API Key proporcionada.

**Parámetros:**
- `apiKey`: Clave API para la configuración

#### `checkAvailableReader()`

Verifica si hay un lector NFC disponible.

**Retorna:** `Future<bool>` - `true` si hay un lector disponible

#### `startReader({required TagReadCallback tagReadCallback, required TagReadErrorCallback tagReadErrorCallback})`

Inicia la lectura de bandas NFC.

**Parámetros:**
- `tagReadCallback`: Callback que se ejecuta cuando se lee una banda exitosamente
- `tagReadErrorCallback`: Callback que se ejecuta cuando ocurre un error

#### `syncToolkitData()`

Sincroniza los datos del toolkit.

#### `removeHandlers()`

Remueve todos los handlers configurados.

### Callbacks

#### `TagReadCallback`

```dart
typedef TagReadCallback = void Function(GoBandUser user);
```

#### `TagReadErrorCallback`

```dart
typedef TagReadErrorCallback = void Function(String error);
```

## 💡 Ejemplos

### Ejemplo Completo

```dart
import 'package:flutter/material.dart';
import 'package:go_fun_band/gofunband.dart';

class BandReaderScreen extends StatefulWidget {
  @override
  _BandReaderScreenState createState() => _BandReaderScreenState();
}

class _BandReaderScreenState extends State<BandReaderScreen> {
  final GoFunBand _goFunBand = GoFunBand();
  GoBandUser? _currentUser;
  String _status = 'Inicializando...';

  @override
  void initState() {
    super.initState();
    _initializeBandReader();
  }

  Future<void> _initializeBandReader() async {
    try {
      // Inicializar toolkit
      bool initialized = await _goFunBand.initializeToolkit(GoBandEnvironment.SANDBOX);

      if (!initialized) {
        setState(() => _status = 'Error al inicializar toolkit');
        return;
      }

      // Configurar dispositivo
      await _goFunBand.configureDevice('tu-api-key');

      // Verificar lector
      bool readerAvailable = await _goFunBand.checkAvailableReader();

      if (!readerAvailable) {
        setState(() => _status = 'No hay lector NFC disponible');
        return;
      }

      setState(() => _status = 'Listo para leer bandas');

      // Iniciar lectura
      await _goFunBand.startReader(
        tagReadCallback: _onTagRead,
        tagReadErrorCallback: _onTagReadError,
      );

    } catch (e) {
      setState(() => _status = 'Error: $e');
    }
  }

  void _onTagRead(GoBandUser user) {
    setState(() {
      _currentUser = user;
      _status = 'Banda leída exitosamente';
    });
  }

  void _onTagReadError(String error) {
    setState(() {
      _status = 'Error: $error';
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Lector de Bandas GoFun')),
      body: Padding(
        padding: EdgeInsets.all(16.0),
        child: Column(
          children: [
            Text('Estado: $_status'),
            SizedBox(height: 20),
            if (_currentUser != null) ...[
              Card(
                child: Padding(
                  padding: EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      Text('ID Usuario: ${_currentUser!.id}'),
                      Text('Balance: \$${_currentUser!.balance}'),
                    ],
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    _goFunBand.removeHandlers();
    super.dispose();
  }
}
```

## 📋 Requisitos

### Flutter
- Flutter SDK >= 3.3.0
- Dart SDK >= 3.5.4

### Android
- Android API Level 21+ (Android 5.0+)
- Dispositivo con soporte NFC
- Permisos NFC configurados

### Dependencias Nativas
- GoFun Band SDK para Android
- Soporte para MIFARE Ultralight C

## 🔧 Desarrollo

### Estructura del Proyecto

```
lib/
├── gofunband.dart                           # Clase principal del plugin
├── go_fun_band_platform_interface.dart     # Interfaz de plataforma
└── toolkit/
    ├── enums.dart                          # Enumeraciones
    └── models.dart                         # Modelos de datos

android/
└── src/main/kotlin/com/orderfast/go_fun_band/
    └── GoFunBandPlugin.kt                  # Implementación Android
```

### Ejecutar el Ejemplo

```bash
cd example
flutter run
```

## 🐛 Solución de Problemas

### Error: "Reader not available"
- Verifica que el dispositivo tenga NFC habilitado
- Asegúrate de que la aplicación tenga permisos NFC
- Verifica que el dispositivo soporte MIFARE

### Error: "Failed to initialize toolkit"
- Verifica que la API Key sea válida
- Confirma que el entorno esté configurado correctamente
- Revisa los logs de Android para más detalles

### Error: "Tag read error"
- Asegúrate de que la banda esté cerca del dispositivo
- Verifica que la banda sea compatible con MIFARE Ultralight C
- Intenta mover la banda lentamente sobre el área NFC

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles.

**Desarrollado por OrderFast** 🚀