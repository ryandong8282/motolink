import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../../../core/config/app_config.dart';
import '../../../core/network/api_client.dart';
import '../domain/ptt_state.dart';
import '../domain/ride_room.dart';

final roomRepositoryProvider = Provider<RoomRepository>((ref) {
  return RoomRepository(ref.watch(dioProvider));
});

abstract interface class FloorControlRepository {
  Future<FloorLease> requestFloor({
    required String roomId,
    required String userId,
  });

  Future<bool> heartbeat({
    required String roomId,
    required String userId,
  });

  Future<void> releaseFloor({
    required String roomId,
    required String userId,
  });
}

class RoomRepository implements FloorControlRepository {
  RoomRepository(this._dio);

  final Dio _dio;
  static const _uuid = Uuid();
  String? _demoSpeaker;

  Future<RideRoom> createRoom({
    required String userId,
    required String roomName,
  }) async {
    if (AppConfig.demoMode) {
      return RideRoom(
        id: _uuid.v4(),
        roomCode: '952701',
        name: roomName,
        members: [
          RoomMember(
            userId: userId,
            nickname: '我',
            role: 'OWNER',
            online: true,
          ),
          const RoomMember(
            userId: 'demo-1',
            nickname: '北四环阿凯',
            role: 'MEMBER',
            online: true,
          ),
          const RoomMember(
            userId: 'demo-2',
            nickname: '小满同学',
            role: 'MEMBER',
            online: true,
          ),
        ],
      );
    }

    final response = await _dio.post<Map<String, dynamic>>(
      '/api/v1/rooms',
      data: {'name': roomName, 'maxMembers': 20, 'publicRoom': true},
      options: userOptions(userId),
    );
    return RideRoom.fromJson(response.data!);
  }

  Future<RideRoom> joinRoom({
    required String userId,
    required String roomCode,
  }) async {
    if (AppConfig.demoMode) {
      return RideRoom(
        id: _uuid.v4(),
        roomCode: roomCode,
        name: '加入的骑行队',
        members: [
          RoomMember(
            userId: userId,
            nickname: '我',
            role: 'MEMBER',
            online: true,
          ),
          const RoomMember(
            userId: 'demo-owner',
            nickname: '领队老陈',
            role: 'OWNER',
            online: true,
          ),
        ],
      );
    }

    final response = await _dio.post<Map<String, dynamic>>(
      '/api/v1/rooms/join',
      data: {'roomCode': roomCode},
      options: userOptions(userId),
    );
    return RideRoom.fromJson(response.data!);
  }

  @override
  Future<FloorLease> requestFloor({
    required String roomId,
    required String userId,
  }) async {
    if (AppConfig.demoMode) {
      if (_demoSpeaker == null || _demoSpeaker == userId) {
        _demoSpeaker = userId;
        return FloorLease(
          granted: true,
          speakerId: userId,
          leaseMillis: 15000,
        );
      }
      return FloorLease(
        granted: false,
        speakerId: _demoSpeaker,
        leaseMillis: 15000,
      );
    }

    final response = await _dio.post<Map<String, dynamic>>(
      '/api/v1/rooms/$roomId/floor/request',
      options: userOptions(userId),
    );
    final data = response.data!;
    return FloorLease(
      granted: data['granted'] as bool,
      speakerId: data['speakerId'] as String?,
      leaseMillis: data['leaseMillis'] as int,
    );
  }

  @override
  Future<bool> heartbeat({
    required String roomId,
    required String userId,
  }) async {
    if (AppConfig.demoMode) return _demoSpeaker == userId;

    final response = await _dio.post<Map<String, dynamic>>(
      '/api/v1/rooms/$roomId/floor/heartbeat',
      options: userOptions(userId),
    );
    return response.data!['granted'] as bool;
  }

  @override
  Future<void> releaseFloor({
    required String roomId,
    required String userId,
  }) async {
    if (AppConfig.demoMode) {
      if (_demoSpeaker == userId) _demoSpeaker = null;
      return;
    }
    await _dio.post<void>(
      '/api/v1/rooms/$roomId/floor/release',
      options: userOptions(userId),
    );
  }
}
