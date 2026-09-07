import 'dart:convert';

import 'package:http/http.dart' as http;

import 'models.dart';

class MotoLinkApi {
  MotoLinkApi({http.Client? client, String? baseUrl})
      : _client = client ?? http.Client(),
        baseUrl = (baseUrl ??
                const String.fromEnvironment(
                  'API_BASE_URL',
                  defaultValue: 'http://localhost:8080',
                ))
            .replaceFirst(RegExp(r'/$'), '');

  final http.Client _client;
  final String baseUrl;

  Uri _uri(String path, [Map<String, String>? query]) {
    final uri = Uri.parse('$baseUrl$path');
    return query == null ? uri : uri.replace(queryParameters: query);
  }

  Uri teamSocketUri({required String roomId, required String userId}) {
    final httpUri = _uri('/ws/teams', {'roomId': roomId, 'userId': userId});
    return httpUri.replace(scheme: httpUri.scheme == 'https' ? 'wss' : 'ws');
  }

  Future<AppSession> devLogin({
    required String phone,
    required String nickname,
  }) async {
    final json = await _post('/api/v1/auth/dev-login', {
      'phone': phone,
      'nickname': nickname,
    });
    return AppSession(
      user: UserProfile.fromJson(json['user'] as Map<String, dynamic>),
      token: json['token'] as String,
    );
  }

  Future<TeamRoom> createTeam({
    required String leaderId,
    required String name,
    int maxMembers = 12,
  }) async {
    final json = await _post('/api/v1/teams', {
      'leaderId': leaderId,
      'name': name,
      'maxMembers': maxMembers,
    });
    return TeamRoom.fromJson(json);
  }

  Future<TeamRoom> joinTeam({
    required String userId,
    required String roomCode,
  }) async {
    final json = await _post('/api/v1/teams/join', {
      'userId': userId,
      'roomCode': roomCode,
    });
    return TeamRoom.fromJson(json);
  }

  Future<TeamRoom> getTeam(String roomId) async {
    final response = await _client.get(_uri('/api/v1/teams/$roomId'));
    return TeamRoom.fromJson(_decode(response));
  }

  Future<FloorLease> requestFloor({
    required String roomId,
    required String userId,
  }) async {
    final json = await _post('/api/v1/teams/$roomId/ptt/request', {
      'userId': userId,
    });
    return FloorLease.fromJson(json);
  }

  Future<FloorLease> heartbeatFloor({
    required String roomId,
    required String userId,
  }) async {
    final json = await _post('/api/v1/teams/$roomId/ptt/heartbeat', {
      'userId': userId,
    });
    return FloorLease.fromJson(json);
  }

  Future<FloorLease> releaseFloor({
    required String roomId,
    required String userId,
  }) async {
    final json = await _post('/api/v1/teams/$roomId/ptt/release', {
      'userId': userId,
    });
    return FloorLease.fromJson(json);
  }

  Future<RideSession> startRide({
    required String userId,
    String? roomId,
  }) async {
    final json = await _post('/api/v1/rides/start', {
      'userId': userId,
      'roomId': roomId,
    });
    return RideSession.fromJson(json);
  }

  Future<RideSession> finishRide({
    required String rideId,
    required double distanceMeters,
    required double maxSpeedMps,
  }) async {
    final json = await _post('/api/v1/rides/$rideId/finish', {
      'distanceMeters': distanceMeters,
      'maxSpeedMps': maxSpeedMps,
    });
    return RideSession.fromJson(json);
  }

  Future<Map<String, dynamic>> _post(
    String path,
    Map<String, dynamic> body,
  ) async {
    final response = await _client.post(
      _uri(path),
      headers: const {'content-type': 'application/json'},
      body: jsonEncode(body),
    );
    return _decode(response);
  }

  Map<String, dynamic> _decode(http.Response response) {
    final Object? decoded = response.body.isEmpty ? null : jsonDecode(response.body);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = decoded is Map<String, dynamic>
          ? decoded['message']?.toString() ?? 'Request failed'
          : 'Request failed';
      throw ApiException(response.statusCode, message);
    }
    if (decoded is! Map<String, dynamic>) {
      throw ApiException(response.statusCode, 'Unexpected server response');
    }
    return decoded;
  }

  void close() => _client.close();
}

class ApiException implements Exception {
  const ApiException(this.statusCode, this.message);

  final int statusCode;
  final String message;

  @override
  String toString() => message;
}
