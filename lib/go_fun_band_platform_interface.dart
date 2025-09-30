import 'package:go_fun_band/toolkit/enums.dart';
import 'package:go_fun_band/toolkit/models.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'gofunband.dart';

typedef TagReadCallback = void Function(GoBandUser);
typedef TagReadErrorCallback = void Function(String);

abstract class GoFunBandPlatform extends PlatformInterface {
  /// Constructs a GoFunBandPlatform.
  GoFunBandPlatform() : super(token: _token);

  static final Object _token = Object();

  static GoFunBandPlatform _instance = GoFunBand();

  /// The default instance of [GoFunBandPlatform] to use.
  ///
  /// Defaults to [MethodChannelGoFunBand].
  static GoFunBandPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [GoFunBandPlatform] when
  /// they register themselves.
  static set instance(GoFunBandPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<bool> initializeToolkit(GoBandEnvironment environment) async {
    throw UnimplementedError('initializeToolkit() has not been implemented.');
  }

  Future<void> configureDevice(String apiKey) async {
    throw UnimplementedError('configureDevice() has not been implemented.');
  }

  Future<bool> checkAvailableReader() async {
    throw UnimplementedError(
        'checkAvailableReader() has not been implemented.');
  }

  Future<void> syncToolkitData() async {
    throw UnimplementedError('syncToolkitData() has not been implemented.');
  }

  Future<void> removeHandlers() async {
    throw UnimplementedError('removeHandlers() has not been implemented.');
  }

  Future<void> startReader({
    required TagReadCallback tagReadCallback,
    required TagReadErrorCallback tagReadErrorCallback,
  }) async {
    throw UnimplementedError('startReader() has not been implemented.');
  }
}
