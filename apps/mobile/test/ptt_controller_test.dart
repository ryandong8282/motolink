import 'package:flutter_test/flutter_test.dart';
import 'package:motolink/src/features/room/application/ptt_controller.dart';
import 'package:motolink/src/features/room/data/room_repository.dart';
import 'package:motolink/src/features/room/domain/ptt_state.dart';
import 'package:motolink/src/features/room/infrastructure/ptt_gateway.dart';

void main() {
  test('press obtains floor and release stops transmission', () async {
    final floor = _FakeFloorRepository();
    final gateway = _FakeGateway();
    final controller = PttController(
      roomId: 'room-1',
      userId: 'user-1',
      floorRepository: floor,
      gateway: gateway,
    );

    await controller.press();
    expect(controller.state.phase, PttPhase.transmitting);
    expect(gateway.started, isTrue);

    await controller.release();
    expect(controller.state.phase, PttPhase.idle);
    expect(gateway.stopped, isTrue);
    expect(floor.released, isTrue);

    controller.dispose();
  });
}

class _FakeFloorRepository implements FloorControlRepository {
  bool released = false;

  @override
  Future<bool> heartbeat({required String roomId, required String userId}) async {
    return true;
  }

  @override
  Future<void> releaseFloor({
    required String roomId,
    required String userId,
  }) async {
    released = true;
  }

  @override
  Future<FloorLease> requestFloor({
    required String roomId,
    required String userId,
  }) async {
    return FloorLease(granted: true, speakerId: userId, leaseMillis: 15000);
  }
}

class _FakeGateway implements PttGateway {
  bool started = false;
  bool stopped = false;

  @override
  Future<void> joinRoom(String roomId) async {}

  @override
  Future<void> leaveRoom() async {}

  @override
  Future<void> startTransmit() async => started = true;

  @override
  Future<void> stopTransmit() async => stopped = true;
}
