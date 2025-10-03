import 'package:flutter/services.dart';
import 'package:go_fun_band/toolkit/enums.dart';
import 'package:go_fun_band/toolkit/models.dart';

const methodChannel = MethodChannel('toolkit_flutter');

typedef TagReadCallback = void Function(GoBandUser);
typedef TagReadErrorCallback = void Function(String);

/// An implementation of [GoFunBandPlatform] that uses method channels.
class GoFunBand {

  static GoFunBand? _instance;
  static GoFunBand get instance => _instance ??= GoFunBand._();

  TagReadCallback? _tagReadCallback;
  TagReadErrorCallback? _tagReadErrorCallback;

  GoFunBand._() {
    methodChannel.setMethodCallHandler(_handleMethodCall);
  }

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case "onTagRead":
      // Handle tag read event
        if (_tagReadCallback != null) {
          final userMap = call.arguments as Map<dynamic, dynamic>;
          final user = GoBandUser.fromJson(Map<String, dynamic>.from(userMap));
          _tagReadCallback!(user);
        }
        break;
      case "onTagReadError":
      // Handle tag read error event
        if (_tagReadErrorCallback != null) {
          final errorMessage = call.arguments as String;
          _tagReadErrorCallback!(errorMessage);
        }
        break;
      default:
        throw MissingPluginException(
            'Not implemented: ${call.method} in GoFunBand');
    }
  }

  Future<bool> initializeToolkit(GoBandEnvironment environment) async {
    return await methodChannel
        .invokeMethod('initializeToolkit', {'environment': environment.name});
  }

  Future<void> configureDevice(String apiKey) async {
    await methodChannel.invokeMethod('configureDevice', {'apiKey': apiKey});
  }

  Future<bool> checkAvailableReader() async {
    return await methodChannel.invokeMethod('checkAvailableReader');
  }

  Future<void> syncToolkitData() async {
    await methodChannel.invokeMethod('syncToolkitData');
  }

  Future<void> removeHandlers() async {
    await methodChannel.invokeMethod('removeHandlers');
  }

  Future<void> startReader({
    required TagReadCallback tagReadCallback,
    required TagReadErrorCallback tagReadErrorCallback,
  }) async {
    _tagReadCallback = tagReadCallback;
    _tagReadErrorCallback = tagReadErrorCallback;

    await methodChannel.invokeMethod('startReader');
  }
}
