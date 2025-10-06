import 'dart:async';
import 'package:flutter/services.dart';
import 'package:go_fun_band/toolkit/models.dart';

class GoFunBandException implements Exception {
  final String code;
  final String message;

  GoFunBandException(this.code, this.message);

  @override
  String toString() => 'GoFunBandException($code): $message';
}

/// Plugin principal de GoFunBand
class GoFunBandPlugin {
  static const MethodChannel _channel = MethodChannel('gofunband');

  static GoFunBandPlugin? _instance;

  static GoFunBandPlugin get instance => _instance ??= GoFunBandPlugin._();

  GoFunBandPlugin._() {
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  // Streams para callbacks
  final _tagReadController = StreamController<TagUser>.broadcast();
  final _tagErrorController = StreamController<String>.broadcast();
  final _rechargeSuccessController =
      StreamController<RechargeResult>.broadcast();
  final _rechargeErrorController = StreamController<String>.broadcast();

  /// Stream para tags leídos exitosamente
  Stream<TagUser> get onTagRead => _tagReadController.stream;

  /// Stream para errores de lectura de tags
  Stream<String> get onTagError => _tagErrorController.stream;

  /// Stream para recargas exitosas
  Stream<RechargeResult> get onRechargeSuccess =>
      _rechargeSuccessController.stream;

  /// Stream para errores de recarga
  Stream<String> get onRechargeError => _rechargeErrorController.stream;

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onTagRead':
        final user = TagUser.fromMap(call.arguments as Map);
        _tagReadController.add(user);
        break;

      case 'onTagReadError':
        final error = call.arguments is Map
            ? call.arguments['error'] as String
            : call.arguments.toString();
        _tagErrorController.add(error);
        break;

      case 'onRechargeSuccess':
        final result = RechargeResult.fromMap(call.arguments as Map);
        _rechargeSuccessController.add(result);
        break;

      case 'onRechargeError':
        final error = call.arguments is Map
            ? call.arguments['error'] as String
            : call.arguments.toString();
        _rechargeErrorController.add(error);
        break;
    }
  }

  Future<void> startAutoSync() async {
    try {
      await _channel.invokeMethod('startAutoSync');
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Detiene la sincronización automática
  Future<void> stopAutoSync() async {
    try {
      await _channel.invokeMethod('stopAutoSync');
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Inicializa el toolkit
  ///
  /// [environment] puede ser 'PRODUCTION' o 'SANDBOX' (default)
  /// [readerType] puede ser 'ACS' (default)
  /// [autoSync] indica si la sincronización automática debe estar habilitada (default: true)
  Future<void> initializeToolkit({
    String environment = 'SANDBOX',
    String readerType = 'ACS',
    bool autoSync = true,
  }) async {
    try {
      await _channel.invokeMethod('initializeToolkit', {
        'environment': environment,
        'readerType': readerType,
        'autoSync': autoSync,
      });
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Configura el dispositivo con la API key
  Future<void> configureDevice(String apiKey) async {
    try {
      await _channel.invokeMethod('configureDevice', {
        'apiKey': apiKey,
      });
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Verifica si hay un lector disponible
  Future<bool> checkAvailableReader() async {
    try {
      final result = await _channel.invokeMethod('checkAvailableReader');
      return result as bool;
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Verifica si el dispositivo está configurado
  Future<bool> isDeviceConfigured() async {
    try {
      final result = await _channel.invokeMethod('isDeviceConfigured');
      return result as bool;
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Sincroniza los datos con el servidor
  Future<void> syncToolkitData() async {
    try {
      await _channel.invokeMethod('syncToolkitData');
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Inicia el lector de tags
  Future<void> startReader() async {
    try {
      await _channel.invokeMethod('startReader');
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Detiene el lector de tags
  Future<void> stopReader() async {
    try {
      await _channel.invokeMethod('stopReader');
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Remueve todos los handlers activos
  Future<void> removeHandlers() async {
    try {
      await _channel.invokeMethod('removeHandlers');
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Añade una recarga a un tag
  ///
  /// [amount] debe estar en céntimos
  /// [concept] puede ser 'CASH', 'CARD', o 'CLAIM'
  /// [origin] identifica el origen de la recarga
  /// [reference] referencia opcional para la transacción
  Future<void> addRecharge({
    required int amount,
    String concept = 'CASH',
    String origin = 'Flutter',
    String? reference,
  }) async {
    try {
      await _channel.invokeMethod('addRecharge', {
        'amount': amount,
        'concept': concept,
        'origin': origin,
        if (reference != null) 'reference': reference,
      });
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Libera los recursos del plugin
  void dispose() {
    _tagReadController.close();
    _tagErrorController.close();
    _rechargeSuccessController.close();
    _rechargeErrorController.close();
    _channel.invokeMethod("shutdownToolkit");
  }
}
