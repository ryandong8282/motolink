class RoomMember {
  const RoomMember({
    required this.userId,
    required this.nickname,
    required this.role,
    required this.online,
  });

  final String userId;
  final String nickname;
  final String role;
  final bool online;

  factory RoomMember.fromJson(Map<String, dynamic> json) {
    return RoomMember(
      userId: json['userId'] as String,
      nickname: json['nickname'] as String,
      role: json['role'] as String,
      online: (json['online'] as bool?) ?? true,
    );
  }
}

class RideRoom {
  const RideRoom({
    required this.id,
    required this.roomCode,
    required this.name,
    required this.members,
  });

  final String id;
  final String roomCode;
  final String name;
  final List<RoomMember> members;

  factory RideRoom.fromJson(Map<String, dynamic> json) {
    return RideRoom(
      id: json['id'] as String,
      roomCode: json['roomCode'] as String,
      name: json['name'] as String,
      members: (json['members'] as List<dynamic>? ?? const [])
          .cast<Map<String, dynamic>>()
          .map(RoomMember.fromJson)
          .toList(),
    );
  }
}
