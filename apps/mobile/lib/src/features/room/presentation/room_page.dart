import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/domain/auth_session.dart';
import '../application/ptt_controller.dart';
import '../domain/ptt_state.dart';
import '../domain/ride_room.dart';

class RoomPage extends ConsumerStatefulWidget {
  const RoomPage({
    required this.session,
    required this.room,
    super.key,
  });

  final AuthSession session;
  final RideRoom room;

  @override
  ConsumerState<RoomPage> createState() => _RoomPageState();
}

class _RoomPageState extends ConsumerState<RoomPage> {
  late final PttControllerArgs _args;

  @override
  void initState() {
    super.initState();
    _args = PttControllerArgs(
      roomId: widget.room.id,
      userId: widget.session.userId,
    );
    Future<void>.microtask(
      () => ref.read(pttControllerProvider(_args).notifier).initialize(),
    );
  }

  @override
  Widget build(BuildContext context) {
    final ptt = ref.watch(pttControllerProvider(_args));
    final controller = ref.read(pttControllerProvider(_args).notifier);
    final isTransmitting = ptt.phase == PttPhase.transmitting;
    final isRequesting = ptt.phase == PttPhase.requesting;

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.room.name),
        actions: [
          Center(child: Text('房间号 ${widget.room.roomCode}')),
          const SizedBox(width: 16),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    const Icon(Icons.location_on_outlined),
                    const SizedBox(width: 12),
                    const Expanded(
                      child: Text('队员实时位置与轨迹同步将在地图 SDK POC 后接入'),
                    ),
                    Chip(label: Text('${widget.room.members.length} 人在线')),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              '车队成员',
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
            ),
            const SizedBox(height: 8),
            ...widget.room.members.map(
              (member) => ListTile(
                contentPadding: EdgeInsets.zero,
                leading: CircleAvatar(
                  child: Text(member.nickname.characters.first),
                ),
                title: Text(member.nickname),
                subtitle: Text(member.role == 'OWNER' ? '队长' : '成员'),
                trailing: Icon(
                  Icons.circle,
                  size: 12,
                  color: member.online ? Colors.green : Colors.grey,
                ),
              ),
            ),
            const SizedBox(height: 24),
            Text(
              ptt.message,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
            ),
            if (ptt.speakerId != null && ptt.speakerId != widget.session.userId)
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text(
                  '讲话者：${ptt.speakerId}',
                  textAlign: TextAlign.center,
                ),
              ),
            const SizedBox(height: 18),
            Center(
              child: Listener(
                onPointerDown: (_) => controller.press(),
                onPointerUp: (_) => controller.release(),
                onPointerCancel: (_) => controller.release(),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 160),
                  width: 190,
                  height: 190,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: isTransmitting
                        ? Theme.of(context).colorScheme.error
                        : Theme.of(context).colorScheme.primary,
                    boxShadow: [
                      BoxShadow(
                        blurRadius: isTransmitting ? 28 : 14,
                        spreadRadius: isTransmitting ? 7 : 2,
                        color: (isTransmitting
                                ? Theme.of(context).colorScheme.error
                                : Theme.of(context).colorScheme.primary)
                            .withValues(alpha: 0.28),
                      ),
                    ],
                  ),
                  alignment: Alignment.center,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (isRequesting)
                        const SizedBox(
                          width: 34,
                          height: 34,
                          child: CircularProgressIndicator(
                            strokeWidth: 3,
                            color: Colors.white,
                          ),
                        )
                      else
                        Icon(
                          isTransmitting ? Icons.mic : Icons.mic_none,
                          size: 54,
                          color: Colors.white,
                        ),
                      const SizedBox(height: 10),
                      Text(
                        isTransmitting ? '松开结束' : '按住说话',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 18,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 24),
            const Card(
              child: Padding(
                padding: EdgeInsets.all(16),
                child: Text(
                  'MVP 当前已实现服务端抢麦租约、客户端按压状态机和原生桥接边界。'
                  '实际音频、锁屏后台、蓝牙耳机与风噪能力需要接入 RTC 后进行实车验证。',
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
