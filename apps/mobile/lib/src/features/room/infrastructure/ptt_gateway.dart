import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/config/app_config.dart';

final pttGatewayProvider = Provider<PttGateway>((ref) {
  return AppConfig.demoMode ? DemoPttGateway() : MethodChannelPttGateway();
});

abstract interface class PttGateway {
  Future<void> joinRoom(String roomId);
  Future<void> startTransmit();
  Future<void> stopTransmit();
  Future<void> leaveRoom();
}

class DemoPttGateway implements PttGateway {
  @override
  Future<void> joinRoom(String roomId) async {}

  @override
  Future<void> leaveRoom() async {}

  @override
  Future<void> startTransmit() async {}

  @override
  Future<void> stopTransmit() async {}
}

class MethodChannelPttGateway implements PttGateway {
  static const _channel = MethodChannel('motolink/riding_core');

  @override
  Future<void> joinRoom(String roomId) {
    return _channel.invokeMethod<void>('joinRoom', {'roomId': roomId});
  }

  @override
  Future<void> startTransmit() {
    return _channel.invokeMethod<void>('startTransmit');
  }

  @override
  Future<void> stopTransmit() {
    return _channel.invokeMethod<void>('stopTransmit');
  }

  @override
  Future<void> leaveRoom() {
    return _channel.invokeMethod<void>('leaveRoom');
  }
}
