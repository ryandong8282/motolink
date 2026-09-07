import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../../../core/config/app_config.dart';
import '../../../core/network/api_client.dart';

final rideRepositoryProvider = Provider<RideRepository>((ref) {
  return RideRepository(ref.watch(dioProvider));
});

class RideRepository {
  RideRepository(this._dio);

  final Dio _dio;
  static const _uuid = Uuid();

  Future<String> start({required String userId}) async {
    if (AppConfig.demoMode) return _uuid.v4();
    final response = await _dio.post<Map<String, dynamic>>(
      '/api/v1/rides',
      options: userOptions(userId),
    );
    return response.data!['id'] as String;
  }

  Future<void> appendPoint({
    required String userId,
    required String rideId,
    required double latitude,
    required double longitude,
    required double speedKmh,
  }) async {
    if (AppConfig.demoMode) return;
    await _dio.post<void>(
      '/api/v1/rides/$rideId/points',
      data: {
        'latitude': latitude,
        'longitude': longitude,
        'speedKmh': speedKmh,
        'recordedAt': DateTime.now().toUtc().toIso8601String(),
      },
      options: userOptions(userId),
    );
  }

  Future<void> finish({
    required String userId,
    required String rideId,
  }) async {
    if (AppConfig.demoMode) return;
    await _dio.post<void>(
      '/api/v1/rides/$rideId/finish',
      options: userOptions(userId),
    );
  }
}
