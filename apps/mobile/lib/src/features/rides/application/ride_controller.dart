import 'dart:async';
import 'dart:math' as math;

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/ride_repository.dart';

class RideControllerArgs {
  const RideControllerArgs(this.userId);

  final String userId;

  @override
  bool operator ==(Object other) =>
      other is RideControllerArgs && other.userId == userId;

  @override
  int get hashCode => userId.hashCode;
}

class RideState {
  const RideState({
    this.active = false,
    this.starting = false,
    this.elapsedSeconds = 0,
    this.distanceKm = 0,
    this.speedKmh = 0,
    this.maxSpeedKmh = 0,
    this.pointCount = 0,
    this.rideId,
    this.error,
  });

  final bool active;
  final bool starting;
  final int elapsedSeconds;
  final double distanceKm;
  final double speedKmh;
  final double maxSpeedKmh;
  final int pointCount;
  final String? rideId;
  final String? error;

  RideState copyWith({
    bool? active,
    bool? starting,
    int? elapsedSeconds,
    double? distanceKm,
    double? speedKmh,
    double? maxSpeedKmh,
    int? pointCount,
    String? rideId,
    String? error,
    bool clearError = false,
  }) {
    return RideState(
      active: active ?? this.active,
      starting: starting ?? this.starting,
      elapsedSeconds: elapsedSeconds ?? this.elapsedSeconds,
      distanceKm: distanceKm ?? this.distanceKm,
      speedKmh: speedKmh ?? this.speedKmh,
      maxSpeedKmh: maxSpeedKmh ?? this.maxSpeedKmh,
      pointCount: pointCount ?? this.pointCount,
      rideId: rideId ?? this.rideId,
      error: clearError ? null : (error ?? this.error),
    );
  }
}

final rideControllerProvider = StateNotifierProvider.autoDispose
    .family<RideController, RideState, RideControllerArgs>((ref, args) {
  return RideController(
    userId: args.userId,
    repository: ref.watch(rideRepositoryProvider),
  );
});

class RideController extends StateNotifier<RideState> {
  RideController({
    required this.userId,
    required RideRepository repository,
  })  : _repository = repository,
        super(const RideState());

  final String userId;
  final RideRepository _repository;
  Timer? _timer;

  Future<void> start() async {
    if (state.active || state.starting) return;
    state = const RideState(starting: true);
    try {
      final rideId = await _repository.start(userId: userId);
      state = RideState(active: true, rideId: rideId);
      _timer = Timer.periodic(const Duration(seconds: 1), (_) => _tick());
    } catch (error) {
      state = RideState(error: '开始骑行失败：$error');
    }
  }

  void _tick() {
    if (!state.active || state.rideId == null) return;
    final elapsed = state.elapsedSeconds + 1;
    final speed = 38 + 16 * math.sin(elapsed / 7);
    final distance = state.distanceKm + speed / 3600;
    final shouldUpload = elapsed % 5 == 0;

    state = state.copyWith(
      elapsedSeconds: elapsed,
      speedKmh: speed,
      maxSpeedKmh: math.max(state.maxSpeedKmh, speed),
      distanceKm: distance,
      pointCount: state.pointCount + (shouldUpload ? 1 : 0),
      clearError: true,
    );

    if (shouldUpload) {
      unawaited(
        _repository.appendPoint(
          userId: userId,
          rideId: state.rideId!,
          latitude: 39.9042 + elapsed / 100000,
          longitude: 116.4074 + elapsed / 120000,
          speedKmh: speed,
        ),
      );
    }
  }

  Future<void> finish() async {
    final rideId = state.rideId;
    if (!state.active || rideId == null) return;
    _timer?.cancel();
    state = state.copyWith(active: false, speedKmh: 0);
    try {
      await _repository.finish(userId: userId, rideId: rideId);
    } catch (error) {
      state = state.copyWith(error: '结束骑行同步失败：$error');
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}
