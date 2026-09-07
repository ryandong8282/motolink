import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/config/app_config.dart';
import '../../../core/network/api_client.dart';
import '../domain/nearby_rider.dart';

final nearbyRepositoryProvider = Provider<NearbyRepository>((ref) {
  return NearbyRepository(ref.watch(dioProvider));
});

class NearbyRepository {
  NearbyRepository(this._dio);

  final Dio _dio;

  Future<List<NearbyRider>> list({required String userId}) async {
    if (AppConfig.demoMode) {
      await Future<void>.delayed(const Duration(milliseconds: 250));
      return const [
        NearbyRider(
          userId: 'demo-1',
          nickname: '北四环阿凯',
          motorcycle: 'Honda CB650R',
          distanceMeters: 380,
          online: true,
        ),
        NearbyRider(
          userId: 'demo-2',
          nickname: '小满同学',
          motorcycle: 'Yamaha MT-07',
          distanceMeters: 920,
          online: true,
        ),
        NearbyRider(
          userId: 'demo-3',
          nickname: '老周慢骑',
          motorcycle: 'BMW R 1250 GS',
          distanceMeters: 1800,
          online: true,
        ),
      ];
    }

    await _dio.post<void>(
      '/api/v1/nearby/location',
      data: const {
        'latitude': 39.9042,
        'longitude': 116.4074,
        'speedKmh': 0,
        'heading': 0,
      },
      options: userOptions(userId),
    );

    final response = await _dio.get<List<dynamic>>(
      '/api/v1/nearby',
      queryParameters: const {
        'latitude': 39.9042,
        'longitude': 116.4074,
        'radiusKm': 10,
      },
      options: userOptions(userId),
    );
    return response.data!
        .cast<Map<String, dynamic>>()
        .map(NearbyRider.fromJson)
        .toList();
  }
}
