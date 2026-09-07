class AuthSession {
  const AuthSession({
    required this.userId,
    required this.nickname,
    required this.phone,
  });

  final String userId;
  final String nickname;
  final String phone;
}
