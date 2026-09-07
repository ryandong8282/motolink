import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/room_repository.dart';
import '../domain/ptt_state.dart';
import '../infrastructure/ptt_gateway.dart';

class PttControllerArgs {
  const PttControllerArgs({required this.roomId, required this.userId});

  final String roomId;
  final String userId;

  @override
  bool operator ==(Object other) {
    return other is PttControllerArgs &&
        other.roomId == roomId &&
        other.userId == userId;
  }

  @override
  int get hashCode => Object.hash(roomId, userId);
}

final pttControllerProvider = StateNotifierProvider.autoDispose
    .family<PttController, PttUiState, PttControllerArgs>((ref, args) {
  return PttController(
    roomId: args.roomId,
    userId: args.userId,
    floorRepository: ref.watch(roomRepositoryProvider),
    gateway: ref.watch(pttGatewayProvider),
  );
});

class PttController extends StateNotifier<PttUiState> {
  PttController({
    required this.roomId,
    required this.userId,
    required FloorControlRepository floorRepository,
    required PttGateway gateway,
  })  : _floorRepository = floorRepository,
        _gateway = gateway,
        super(const PttUiState());

  final String roomId;
  final String userId;
  final FloorControlRepository _floorRepository;
  final PttGateway _gateway;

  Timer? _heartbeat;
  bool _joined = false;
  bool _pressed = false;
  int _operation = 0;

  Future<void> initialize() async {
    if (_joined) return;
    try {
      await _gateway.joinRoom(roomId);
      _joined = true;
    } catch (error) {
      state = PttUiState(
        phase: PttPhase.error,
        message: '进入语音房间失败：$error',
      );
    }
  }

  Future<void> press() async {
    if (_pressed || state.phase == PttPhase.transmitting) return;
    _pressed = true;
    final operation = ++_operation;
    state = const PttUiState(
      phase: PttPhase.requesting,
      message: '正在申请麦权…',
    );

    try {
      await initialize();
      if (!_joined || !_pressed || operation != _operation) return;

      final lease = await _floorRepository.requestFloor(
        roomId: roomId,
        userId: userId,
      );

      if (!_pressed || operation != _operation) {
        if (lease.granted) {
          await _floorRepository.releaseFloor(
            roomId: roomId,
            userId: userId,
          );
        }
        return;
      }

      if (!lease.granted) {
        _pressed = false;
        state = PttUiState(
          phase: PttPhase.denied,
          speakerId: lease.speakerId,
          message: '当前有人正在讲话',
        );
        return;
      }

      await _gateway.startTransmit();
      if (!_pressed || operation != _operation) {
        await _gateway.stopTransmit();
        await _floorRepository.releaseFloor(
          roomId: roomId,
          userId: userId,
        );
        return;
      }

      state = PttUiState(
        phase: PttPhase.transmitting,
        speakerId: userId,
        message: '正在讲话，松开结束',
      );
      _startHeartbeat();
    } catch (error) {
      _pressed = false;
      _heartbeat?.cancel();
      state = PttUiState(
        phase: PttPhase.error,
        message: '对讲失败：$error',
      );
    }
  }

  Future<void> release() async {
    final wasActive = _pressed ||
        state.phase == PttPhase.requesting ||
        state.phase == PttPhase.transmitting;
    _pressed = false;
    ++_operation;
    _heartbeat?.cancel();

    if (!wasActive) {
      state = const PttUiState();
      return;
    }

    try {
      if (state.phase == PttPhase.transmitting) {
        await _gateway.stopTransmit();
      }
      await _floorRepository.releaseFloor(
        roomId: roomId,
        userId: userId,
      );
      state = const PttUiState();
    } catch (error) {
      state = PttUiState(
        phase: PttPhase.error,
        message: '释放麦权失败：$error',
      );
    }
  }

  void _startHeartbeat() {
    _heartbeat?.cancel();
    _heartbeat = Timer.periodic(const Duration(seconds: 5), (_) async {
      try {
        final renewed = await _floorRepository.heartbeat(
          roomId: roomId,
          userId: userId,
        );
        if (!renewed && mounted) {
          _pressed = false;
          _heartbeat?.cancel();
          await _gateway.stopTransmit();
          state = const PttUiState(
            phase: PttPhase.denied,
            message: '麦权已失效，请重新按住讲话',
          );
        }
      } catch (_) {
        if (mounted) {
          state = const PttUiState(
            phase: PttPhase.error,
            message: '网络波动，麦权续租失败',
          );
        }
      }
    });
  }

  Future<void> shutdown() async {
    _pressed = false;
    ++_operation;
    _heartbeat?.cancel();
    try {
      await _gateway.stopTransmit();
      await _floorRepository.releaseFloor(roomId: roomId, userId: userId);
      if (_joined) await _gateway.leaveRoom();
    } catch (_) {
      // 页面退出时采用 best-effort 清理；服务端 TTL 会回收遗留麦权。
    }
    _joined = false;
  }

  @override
  void dispose() {
    unawaited(shutdown());
    super.dispose();
  }
}
