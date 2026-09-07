import 'package:flutter/material.dart';

import '../../data/demo_store.dart';
import '../../domain/models.dart';
import '../../platform/riding_core.dart';

class TeamRoomScreen extends StatelessWidget {
  const TeamRoomScreen({
    required this.store,
    required this.ridingCore,
    super.key,
  });

  final DemoStore store;
  final RidingCore ridingCore;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: store,
      builder: (context, _) {
        return Scaffold(
          appBar: AppBar(
            title: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(store.team.name, style: const TextStyle(fontWeight: FontWeight.w800)),
                Text(
                  '房间 ${store.team.roomCode} · ${store.team.members.length}/${store.team.capacity}',
                  style: const TextStyle(fontSize: 12, color: Colors.white60),
                ),
              ],
            ),
            actions: [
              IconButton(
                onPressed: store.simulateReconnect,
                tooltip: '模拟弱网重连',
                icon: const Icon(Icons.network_check_rounded),
              ),
              const SizedBox(width: 4),
            ],
          ),
          body: SafeArea(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
              children: [
                _NetworkStatus(status: store.connection),
                const SizedBox(height: 12),
                _RideSummary(snapshot: store.ride),
                const SizedBox(height: 12),
                _SafetyNotice(members: store.team.members),
                const SizedBox(height: 18),
                const Text(
                  '车队成员',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
                ),
                const SizedBox(height: 10),
                Card(
                  margin: EdgeInsets.zero,
                  child: Column(
                    children: [
                      for (var index = 0; index < store.team.members.length; index++) ...[
                        _MemberTile(
                          member: store.team.members[index],
                          speaking: store.currentSpeaker == store.team.members[index].nickname,
                        ),
                        if (index < store.team.members.length - 1)
                          const Divider(height: 1, indent: 72),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 24),
                Center(
                  child: _PushToTalkButton(
                    enabled: store.isRiding &&
                        store.connection == ConnectionStateStatus.connected,
                    talking: store.isTalking,
                    busyBy: store.isTalking ? null : store.currentSpeaker,
                    onPressed: () async {
                      await ridingCore.requestFloor();
                      store.beginTalking();
                    },
                    onReleased: () async {
                      store.endTalking();
                      await ridingCore.releaseFloor();
                    },
                  ),
                ),
                const SizedBox(height: 14),
                Text(
                  store.isRiding
                      ? '按住实时讲话，松开立即释放麦权'
                      : '开始骑行后启用对讲和队内位置共享',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: Colors.white60),
                ),
                const SizedBox(height: 20),
                SizedBox(
                  width: double.infinity,
                  child: store.isRiding
                      ? OutlinedButton.icon(
                          onPressed: () async {
                            store.endTalking();
                            await ridingCore.finishRide();
                            store.finishRide();
                          },
                          icon: const Icon(Icons.stop_circle_outlined),
                          label: const Text('结束本次骑行'),
                        )
                      : FilledButton.icon(
                          onPressed: () async {
                            await ridingCore.startRide();
                            store.startRide();
                          },
                          icon: const Icon(Icons.play_arrow_rounded),
                          label: const Text('开始骑行'),
                        ),
                ),
                const SizedBox(height: 10),
                TextButton.icon(
                  onPressed: () => _showEmergencySheet(context),
                  icon: const Icon(Icons.sos_rounded),
                  label: const Text('紧急求助（演示）'),
                  style: TextButton.styleFrom(
                    foregroundColor: Theme.of(context).colorScheme.error,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _showEmergencySheet(BuildContext context) {
    return showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 30),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              '发送紧急求助',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 10),
            const Text(
              'MVP 仅向当前车队发送位置和求助消息，不代表专业救援已经受理。',
              style: TextStyle(color: Colors.white70),
            ),
            const SizedBox(height: 18),
            FilledButton.icon(
              onPressed: () {
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('演示求助已发送给 3 名车队成员')),
                );
              },
              icon: const Icon(Icons.send_rounded),
              label: const Text('确认发送'),
            ),
          ],
        ),
      ),
    );
  }
}

class _NetworkStatus extends StatelessWidget {
  const _NetworkStatus({required this.status});

  final ConnectionStateStatus status;

  @override
  Widget build(BuildContext context) {
    final connected = status == ConnectionStateStatus.connected;
    return AnimatedContainer(
      duration: const Duration(milliseconds: 250),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: connected ? const Color(0xFF173526) : const Color(0xFF4A3217),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Icon(
            connected ? Icons.graphic_eq_rounded : Icons.sync_rounded,
            color: connected ? const Color(0xFF65DE94) : const Color(0xFFFFBA66),
          ),
          const SizedBox(width: 9),
          Expanded(
            child: Text(
              connected ? '对讲通道已连接 · 42ms' : '弱网重连中，麦克风暂不可用',
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
  }
}

class _RideSummary extends StatelessWidget {
  const _RideSummary({required this.snapshot});

  final RideSnapshot snapshot;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              children: [
                _Metric(
                  label: '当前速度',
                  value: snapshot.currentSpeedKmh.toStringAsFixed(0),
                  unit: 'km/h',
                ),
                _Metric(
                  label: '里程',
                  value: (snapshot.distanceMeters / 1000).toStringAsFixed(2),
                  unit: 'km',
                ),
                _Metric(
                  label: '时长',
                  value: _formatDuration(snapshot.elapsed),
                  unit: '',
                ),
              ],
            ),
            if (snapshot.status == RideStatus.finished) ...[
              const Divider(height: 28),
              Text(
                '平均 ${snapshot.averageSpeedKmh.toStringAsFixed(1)} km/h · '
                '最高 ${snapshot.maxSpeedKmh.toStringAsFixed(1)} km/h',
                style: const TextStyle(color: Colors.white70),
              ),
            ],
          ],
        ),
      ),
    );
  }

  static String _formatDuration(Duration duration) {
    final hours = duration.inHours.toString().padLeft(2, '0');
    final minutes = (duration.inMinutes % 60).toString().padLeft(2, '0');
    final seconds = (duration.inSeconds % 60).toString().padLeft(2, '0');
    return '$hours:$minutes:$seconds';
  }
}

class _Metric extends StatelessWidget {
  const _Metric({required this.label, required this.value, required this.unit});

  final String label;
  final String value;
  final String unit;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        children: [
          Text(label, style: const TextStyle(fontSize: 12, color: Colors.white54)),
          const SizedBox(height: 5),
          Text(value, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
          if (unit.isNotEmpty)
            Text(unit, style: const TextStyle(fontSize: 11, color: Colors.white54)),
        ],
      ),
    );
  }
}

class _SafetyNotice extends StatelessWidget {
  const _SafetyNotice({required this.members});

  final List<TeamMember> members;

  @override
  Widget build(BuildContext context) {
    final furthest = members.fold<int>(
      0,
      (distance, member) => member.distanceFromLeaderMeters > distance
          ? member.distanceFromLeaderMeters
          : distance,
    );
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFF17242D),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFF294252)),
      ),
      child: Row(
        children: [
          const Icon(Icons.shield_outlined, color: Color(0xFF74C9FF)),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              '车队队形正常，最远成员距队长 ${furthest}m',
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}

class _MemberTile extends StatelessWidget {
  const _MemberTile({required this.member, required this.speaking});

  final TeamMember member;
  final bool speaking;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: CircleAvatar(
        backgroundColor: speaking
            ? Theme.of(context).colorScheme.primary
            : Theme.of(context).colorScheme.surfaceContainerHighest,
        child: Icon(speaking ? Icons.mic_rounded : Icons.person_rounded),
      ),
      title: Row(
        children: [
          Text(member.nickname, style: const TextStyle(fontWeight: FontWeight.w700)),
          if (member.isCaptain) ...[
            const SizedBox(width: 6),
            const Icon(Icons.workspace_premium_rounded, size: 16, color: Color(0xFFFFC05A)),
          ],
        ],
      ),
      subtitle: Text(
        member.isCaptain
            ? '队长 · 正在领队'
            : '距队长 ${member.distanceFromLeaderMeters}m',
      ),
      trailing: speaking
          ? const _SpeakingBars()
          : Icon(
              member.online ? Icons.circle : Icons.circle_outlined,
              size: 12,
              color: member.online ? const Color(0xFF58D98A) : Colors.white38,
            ),
    );
  }
}

class _SpeakingBars extends StatelessWidget {
  const _SpeakingBars();

  @override
  Widget build(BuildContext context) {
    return const Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(Icons.graphic_eq_rounded, color: Color(0xFF67E399)),
        SizedBox(width: 4),
        Text('讲话中', style: TextStyle(color: Color(0xFF67E399), fontSize: 12)),
      ],
    );
  }
}

class _PushToTalkButton extends StatelessWidget {
  const _PushToTalkButton({
    required this.enabled,
    required this.talking,
    required this.busyBy,
    required this.onPressed,
    required this.onReleased,
  });

  final bool enabled;
  final bool talking;
  final String? busyBy;
  final VoidCallback onPressed;
  final VoidCallback onReleased;

  @override
  Widget build(BuildContext context) {
    final primary = Theme.of(context).colorScheme.primary;
    final background = talking
        ? const Color(0xFFEF4D4D)
        : enabled
            ? primary
            : const Color(0xFF34373D);

    return GestureDetector(
      onTapDown: enabled ? (_) => onPressed() : null,
      onTapUp: enabled ? (_) => onReleased() : null,
      onTapCancel: enabled ? onReleased : null,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 120),
        width: talking ? 178 : 168,
        height: talking ? 178 : 168,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: background,
          border: Border.all(color: Colors.white.withOpacity(.22), width: 8),
          boxShadow: [
            BoxShadow(
              color: background.withOpacity(.35),
              blurRadius: talking ? 36 : 22,
              spreadRadius: talking ? 8 : 2,
            ),
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(talking ? Icons.mic_rounded : Icons.mic_none_rounded, size: 52),
            const SizedBox(height: 8),
            Text(
              talking
                  ? '正在讲话'
                  : !enabled
                      ? '暂不可用'
                      : busyBy == null
                          ? '按住说话'
                          : '$busyBy 讲话中',
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900),
            ),
          ],
        ),
      ),
    );
  }
}
