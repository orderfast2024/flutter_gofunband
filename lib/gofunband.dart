import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:go_fun_band/toolkit/enums.dart';
import 'package:go_fun_band/toolkit/models.dart';

import 'go_fun_band_platform_interface.dart';

/// An implementation of [GoFunBandPlatform] that uses method channels.
class GoFunBand extends GoFunBandPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('go_fun_band');

  TagReadCallback? _tagReadCallback;
  TagReadErrorCallback? _tagReadErrorCallback;

  MethodChannelGoFunBand() {
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

  @override
  Future<bool> initializeToolkit(GoBandEnvironment environment) async {
    return await methodChannel
        .invokeMethod('initializeToolkit', {'environment': environment.name});
  }

  @override
  Future<void> configureDevice(String apiKey) async {
    await methodChannel.invokeMethod('configureDevice', {'apiKey': apiKey});
  }

  @override
  Future<bool> checkAvailableReader() async {
    return await methodChannel.invokeMethod('checkAvailableReader');
  }

  @override
  Future<void> syncToolkitData() async {
    await methodChannel.invokeMethod('syncToolkitData');
  }

  @override
  Future<void> removeHandlers() async {
    await methodChannel.invokeMethod('removeHandlers');
  }

  @override
  Future<void> startReader({
    required TagReadCallback tagReadCallback,
    required TagReadErrorCallback tagReadErrorCallback,
  }) async {
    _tagReadCallback = tagReadCallback;
    _tagReadErrorCallback = tagReadErrorCallback;

    await methodChannel.invokeMethod('startReader');
  }
}
