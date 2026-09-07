import 'package:flutter/services.dart';

class RidingCore {
  RidingCore._();

  static final RidingCore instance = RidingCore._();
  static const MethodChannel _channel = MethodChannel('com.motolink/riding_core');

  Future<void> initialize() => _channel.invokeMethod<void>('initialize');

  Future<void> joinChannel({
    required String roomId,
    required String userId,
    required String provider,
    required String token,
  }) =>
      _channel.invokeMethod<void>('joinChannel', {
        'roomId': roomId,
        'userId': userId,
        'provider': provider,
        'token': token,
      });

  Future<void> leaveChannel() => _channel.invokeMethod<void>('leaveChannel');

  Future<void> startTransmitting() =>
      _channel.invokeMethod<void>('startTransmitting');

  Future<void> stopTransmitting() =>
      _channel.invokeMethod<void>('stopTransmitting');

  Future<void> startRide({
    required String rideId,
    required String userId,
    String? roomId,
  }) =>
      _channel.invokeMethod<void>('startRide', {
        'rideId': rideId,
        'roomId': roomId,
        'userId': userId,
      });

  Future<void> stopRide() => _channel.invokeMethod<void>('stopRide');

  Future<void> setScreenAwake(bool awake) =>
      _channel.invokeMethod<void>('setScreenAwake', {'awake': awake});

  Future<RidingCoreStatus> getStatus() async {
    final response = await _channel.invokeMapMethod<String, dynamic>('getStatus');
    return RidingCoreStatus.fromMap(response ?? const <String, dynamic>{});
  }
}

class RidingCoreStatus {
  const RidingCoreStatus({
    required this.platform,
    required this.provider,
    required this.state,
    required this.channelJoined,
    required this.transmitting,
    required this.rideActive,
  });

  final String platform;
  final String provider;
  final String state;
  final bool channelJoined;
  final bool transmitting;
  final bool rideActive;

  factory RidingCoreStatus.fromMap(Map<String, dynamic> map) => RidingCoreStatus(
        platform: map['platform']?.toString() ?? 'unknown',
        provider: map['provider']?.toString() ?? 'mock',
        state: map['state']?.toString() ?? 'unknown',
        channelJoined: map['channelJoined'] as bool? ?? false,
        transmitting: map['transmitting'] as bool? ?? false,
        rideActive: map['rideActive'] as bool? ?? false,
      );
}
