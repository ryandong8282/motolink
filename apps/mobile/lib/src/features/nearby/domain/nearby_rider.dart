class NearbyRider {
  const NearbyRider({
    required this.userId,
    required this.nickname,
    required this.motorcycle,
    required this.distanceMeters,
    required this.online,
  });

  final String userId;
  final String nickname;
  final String motorcycle;
  final double distanceMeters;
  final bool online;

  factory NearbyRider.fromJson(Map<String, dynamic> json) {
    return NearbyRider(
      userId: json['userId'] as String,
      nickname: json['nickname'] as String,
      motorcycle: (json['motorcycle'] as String?) ?? '未设置车型',
      distanceMeters: (json['distanceMeters'] as num).toDouble(),
      online: (json['online'] as bool?) ?? true,
    );
  }
}
