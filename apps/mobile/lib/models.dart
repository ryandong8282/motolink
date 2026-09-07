class UserProfile {
  const UserProfile({
    required this.id,
    required this.phone,
    required this.nickname,
  });

  final String id;
  final String phone;
  final String nickname;

  factory UserProfile.fromJson(Map<String, dynamic> json) => UserProfile(
        id: json['id'] as String,
        phone: json['phone'] as String,
        nickname: json['nickname'] as String,
      );
}

class AppSession {
  const AppSession({required this.user, required this.token});

  final UserProfile user;
  final String token;
}

class TeamMember {
  const TeamMember({
    required this.userId,
    required this.nickname,
    required this.role,
  });

  final String userId;
  final String nickname;
  final String role;

  factory TeamMember.fromJson(Map<String, dynamic> json) => TeamMember(
        userId: json['userId'] as String,
        nickname: json['nickname'] as String,
        role: json['role'] as String,
      );
}

class TeamRoom {
  const TeamRoom({
    required this.id,
    required this.roomCode,
    required this.name,
    required this.leaderId,
    required this.maxMembers,
    required this.status,
    required this.members,
  });

  final String id;
  final String roomCode;
  final String name;
  final String leaderId;
  final int maxMembers;
  final String status;
  final List<TeamMember> members;

  factory TeamRoom.fromJson(Map<String, dynamic> json) => TeamRoom(
        id: json['id'] as String,
        roomCode: json['roomCode'] as String,
        name: json['name'] as String,
        leaderId: json['leaderId'] as String,
        maxMembers: json['maxMembers'] as int,
        status: json['status'] as String,
        members: (json['members'] as List<dynamic>? ?? const [])
            .map((item) => TeamMember.fromJson(item as Map<String, dynamic>))
            .toList(growable: false),
      );
}

class FloorLease {
  const FloorLease({
    required this.granted,
    required this.released,
    this.holderUserId,
    this.leaseExpiresAt,
  });

  final bool granted;
  final bool released;
  final String? holderUserId;
  final DateTime? leaseExpiresAt;

  factory FloorLease.fromJson(Map<String, dynamic> json) => FloorLease(
        granted: json['granted'] as bool? ?? false,
        released: json['released'] as bool? ?? false,
        holderUserId: json['holderUserId'] as String?,
        leaseExpiresAt: json['leaseExpiresAt'] == null
            ? null
            : DateTime.parse(json['leaseExpiresAt'] as String),
      );
}

class RideSession {
  const RideSession({
    required this.id,
    required this.status,
    required this.startedAt,
    required this.distanceMeters,
  });

  final String id;
  final String status;
  final DateTime startedAt;
  final double distanceMeters;

  factory RideSession.fromJson(Map<String, dynamic> json) => RideSession(
        id: json['id'] as String,
        status: json['status'] as String,
        startedAt: DateTime.parse(json['startedAt'] as String),
        distanceMeters: (json['distanceMeters'] as num).toDouble(),
      );
}
