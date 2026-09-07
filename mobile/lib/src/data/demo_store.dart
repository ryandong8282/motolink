import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';

import '../domain/models.dart';

class DemoStore extends ChangeNotifier {
  DemoStore()
      : nearbyRiders = const [
          Rider(
            id: 'rider-1',
            nickname: '北城阿泽',
            motorcycle: 'Honda CB650R',
            distanceMeters: 380,
            online: true,
          ),
          Rider(
            id: 'rider-2',
            nickname: '小雨骑行',
            motorcycle: 'Yamaha MT-07',
            distanceMeters: 920,
            online: true,
          ),
          Rider(
            id: 'rider-3',
            nickname: '老周',
            motorcycle: 'BMW R 1250 GS',
            distanceMeters: 1860,
            online: false,
          ),
        ],
        team = const RideTeam(
          id: 'demo-team',
          roomCode: '520131',
          name: '周末环湖小队',
          routeNote: '城北集合，沿湖骑行约 45km',
          capacity: 12,
          members: [
            TeamMember(
              id: 'captain',
              nickname: '队长·阿泽',
              isCaptain: true,
              distanceFromLeaderMeters: 0,
              online: true,
            ),
            TeamMember(
              id: 'me',
              nickname: '我',
              isCaptain: false,
              distanceFromLeaderMeters: 42,
              online: true,
            ),
            TeamMember(
              id: 'rider-2',
              nickname: '小雨',
              isCaptain: false,
              distanceFromLeaderMeters: 138,
              online: true,
            ),
          ],
        );

  final List<Rider> nearbyRiders;
  final RideTeam team;

  ConnectionStateStatus connection = ConnectionStateStatus.connected;
  RideSnapshot ride = const RideSnapshot.idle();
  bool isTalking = false;
  String? currentSpeaker = '队长·阿泽';
  Timer? _rideTimer;
  int _tick = 0;

  bool get isRiding => ride.status == RideStatus.riding;

  void startRide() {
    if (isRiding) return;
    _tick = 0;
    ride = const RideSnapshot.idle().copyWith(status: RideStatus.riding);
    _rideTimer?.cancel();
    _rideTimer = Timer.periodic(const Duration(seconds: 1), (_) => _advanceRide());
    notifyListeners();
  }

  void finishRide() {
    _rideTimer?.cancel();
    _rideTimer = null;
    ride = ride.copyWith(
      status: RideStatus.finished,
      currentSpeedKmh: 0,
    );
    isTalking = false;
    notifyListeners();
  }

  void beginTalking() {
    if (connection != ConnectionStateStatus.connected || !isRiding) return;
    isTalking = true;
    currentSpeaker = '我';
    notifyListeners();
  }

  void endTalking() {
    if (!isTalking) return;
    isTalking = false;
    currentSpeaker = null;
    notifyListeners();
  }

  void simulateReconnect() {
    connection = ConnectionStateStatus.reconnecting;
    notifyListeners();
    Future<void>.delayed(const Duration(seconds: 2), () {
      connection = ConnectionStateStatus.connected;
      notifyListeners();
    });
  }

  void _advanceRide() {
    _tick += 1;
    final speed = 42 + math.sin(_tick / 5) * 13;
    final distanceDelta = speed * 1000 / 3600;
    final elapsed = ride.elapsed + const Duration(seconds: 1);
    final distance = ride.distanceMeters + distanceDelta;
    ride = ride.copyWith(
      elapsed: elapsed,
      distanceMeters: distance,
      currentSpeedKmh: speed,
      averageSpeedKmh: distance / 1000 / (elapsed.inSeconds / 3600),
      maxSpeedKmh: math.max(ride.maxSpeedKmh, speed),
    );
    notifyListeners();
  }

  @override
  void dispose() {
    _rideTimer?.cancel();
    super.dispose();
  }
}
