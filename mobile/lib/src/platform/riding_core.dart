import 'package:flutter/services.dart';

/// Narrow bridge between Flutter business UI and the native riding runtime.
///
/// Production implementations own RTC, audio-session, Bluetooth, background
/// location and service lifecycle in Swift/Kotlin. Flutter only sends commands
/// and observes high-level state.
abstract interface class RidingCore {
  Future<void> initialize();

  Future<void> joinTeamRoom(String teamId);

  Future<void> leaveTeamRoom();

  Future<void> startRide();

  Future<void> finishRide();

  Future<void> requestFloor();

  Future<void> releaseFloor();
}

class MethodChannelRidingCore implements RidingCore {
  const MethodChannelRidingCore();

  static const MethodChannel _channel = MethodChannel('com.motolink/riding_core');

  @override
  Future<void> initialize() => _invoke('initialize');

  @override
  Future<void> joinTeamRoom(String teamId) =>
      _invoke('joinTeamRoom', <String, Object?>{'teamId': teamId});

  @override
  Future<void> leaveTeamRoom() => _invoke('leaveTeamRoom');

  @override
  Future<void> startRide() => _invoke('startRide');

  @override
  Future<void> finishRide() => _invoke('finishRide');

  @override
  Future<void> requestFloor() => _invoke('requestFloor');

  @override
  Future<void> releaseFloor() => _invoke('releaseFloor');

  Future<void> _invoke(String method, [Map<String, Object?>? arguments]) async {
    await _channel.invokeMethod<void>(method, arguments);
  }
}

/// Used by widget tests and the initial clickable prototype.
class MockRidingCore implements RidingCore {
  const MockRidingCore();

  @override
  Future<void> initialize() async {}

  @override
  Future<void> joinTeamRoom(String teamId) async {}

  @override
  Future<void> leaveTeamRoom() async {}

  @override
  Future<void> startRide() async {}

  @override
  Future<void> finishRide() async {}

  @override
  Future<void> requestFloor() async {}

  @override
  Future<void> releaseFloor() async {}
}
