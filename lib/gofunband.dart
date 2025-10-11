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

typedef TagReadCallback = void Function(TagUser user);
typedef TagErrorCallback = void Function(String error);
typedef RechargeSuccessCallback = void Function(RechargeResult result);
typedef RechargeErrorCallback = void Function(String error);

/// Plugin principal de GoFunBand
class GoFunBandPlugin {
  static const MethodChannel _channel = MethodChannel('gofunband');

  static GoFunBandPlugin? _instance;

  static GoFunBandPlugin get instance => _instance ??= GoFunBandPlugin._();

  GoFunBandPlugin._() {
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  TagReadCallback? _onTagRead;
  TagErrorCallback? _onTagReadError;
  RechargeSuccessCallback? _onRechargeSuccess;
  RechargeErrorCallback? _onRechargeError;

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onTagRead':
        final user = TagUser.fromMap(call.arguments as Map);
        _onTagRead?.call(user);
        break;

      case 'onTagReadError':
        final error = call.arguments is Map
            ? call.arguments['error'] as String
            : call.arguments.toString();
        _onTagReadError?.call(error);
        break;

      case 'onRechargeSuccess':
        final result = RechargeResult.fromMap(call.arguments as Map);
        _onRechargeSuccess?.call(result);
        break;

      case 'onRechargeError':
        final error = call.arguments is Map
            ? call.arguments['error'] as String
            : call.arguments.toString();
        _onRechargeError?.call(error);
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
  Future<void> startReader({
    TagReadCallback? onTagRead,
    TagErrorCallback? onTagReadError,
  }) async {
    try {
      _onTagRead = onTagRead;
      _onTagReadError = onTagReadError;
      await _channel.invokeMethod('startReader');
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Detiene el lector de tags
  Future<void> stopReader() async {
    try {
      _onTagRead = null;
      _onTagReadError = null;
      await _channel.invokeMethod('stopReader');
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Remueve todos los handlers activos
  Future<void> removeHandlers() async {
    try {
      _onTagRead = null;
      _onTagReadError = null;
      _onRechargeSuccess = null;
      _onRechargeError = null;
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
    String concept = 'kiosk',
    String origin = 'OrderFast',
    String? reference,
    RechargeSuccessCallback? onRechargeSuccess,
    RechargeErrorCallback? onRechargeError,
  }) async {
    try {
      _onRechargeSuccess = onRechargeSuccess;
      _onRechargeError = onRechargeError;
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

  Future<bool> isToolkitInitialized() async {
    try {
      final result = await _channel.invokeMethod('isToolkitInitialized');
      return result as bool;
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  Future<bool> addRechargeToUserId({
    required String userId,
    required int amount,
    String concept = 'kiosk',
    String origin = 'OrderFast',
    String? reference,
  }) async {
    try {
      final result = await _channel.invokeMethod('addRechargeToUserId', {
        'userId': userId,
        'amount': amount,
        'concept': concept,
        'origin': origin,
        if (reference != null) 'reference': reference,
      });
      return result as bool;
    } on PlatformException catch (e) {
      throw GoFunBandException(e.code, e.message ?? 'Unknown error');
    }
  }

  /// Libera los recursos del plugin
  void dispose() {
    _onTagRead = null;
    _onTagReadError = null;
    _onRechargeSuccess = null;
    _onRechargeError = null;
    _channel.invokeMethod("shutdownToolkit");
  }
}
