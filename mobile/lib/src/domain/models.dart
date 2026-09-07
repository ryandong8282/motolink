enum ConnectionStateStatus { disconnected, connecting, connected, reconnecting }

enum RideStatus { idle, riding, paused, finished }

class Rider {
  const Rider({
    required this.id,
    required this.nickname,
    required this.motorcycle,
    required this.distanceMeters,
    required this.online,
  });

  final String id;
  final String nickname;
  final String motorcycle;
  final int distanceMeters;
  final bool online;
}

class TeamMember {
  const TeamMember({
    required this.id,
    required this.nickname,
    required this.isCaptain,
    required this.distanceFromLeaderMeters,
    required this.online,
  });

  final String id;
  final String nickname;
  final bool isCaptain;
  final int distanceFromLeaderMeters;
  final bool online;
}

class RideTeam {
  const RideTeam({
    required this.id,
    required this.roomCode,
    required this.name,
    required this.routeNote,
    required this.capacity,
    required this.members,
  });

  final String id;
  final String roomCode;
  final String name;
  final String routeNote;
  final int capacity;
  final List<TeamMember> members;
}

class RideSnapshot {
  const RideSnapshot({
    required this.status,
    required this.elapsed,
    required this.distanceMeters,
    required this.currentSpeedKmh,
    required this.averageSpeedKmh,
    required this.maxSpeedKmh,
  });

  const RideSnapshot.idle()
      : status = RideStatus.idle,
        elapsed = Duration.zero,
        distanceMeters = 0,
        currentSpeedKmh = 0,
        averageSpeedKmh = 0,
        maxSpeedKmh = 0;

  final RideStatus status;
  final Duration elapsed;
  final double distanceMeters;
  final double currentSpeedKmh;
  final double averageSpeedKmh;
  final double maxSpeedKmh;

  RideSnapshot copyWith({
    RideStatus? status,
    Duration? elapsed,
    double? distanceMeters,
    double? currentSpeedKmh,
    double? averageSpeedKmh,
    double? maxSpeedKmh,
  }) {
    return RideSnapshot(
      status: status ?? this.status,
      elapsed: elapsed ?? this.elapsed,
      distanceMeters: distanceMeters ?? this.distanceMeters,
      currentSpeedKmh: currentSpeedKmh ?? this.currentSpeedKmh,
      averageSpeedKmh: averageSpeedKmh ?? this.averageSpeedKmh,
      maxSpeedKmh: maxSpeedKmh ?? this.maxSpeedKmh,
    );
  }
}
