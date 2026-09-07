import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../../../core/config/app_config.dart';
import '../../../core/network/api_client.dart';
import '../domain/auth_session.dart';

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(ref.watch(dioProvider));
});

class AuthRepository {
  AuthRepository(this._dio);

  final Dio _dio;
  static const _uuid = Uuid();

  Future<AuthSession> devLogin({
    required String phone,
    required String nickname,
  }) async {
    if (AppConfig.demoMode) {
      return AuthSession(
        userId: _uuid.v4(),
        nickname: nickname,
        phone: phone,
      );
    }

    final response = await _dio.post<Map<String, dynamic>>(
      '/api/v1/auth/dev-login',
      data: {'phone': phone, 'nickname': nickname},
    );
    final data = response.data!;
    return AuthSession(
      userId: data['userId'] as String,
      nickname: data['nickname'] as String,
      phone: data['phone'] as String,
    );
  }
}
