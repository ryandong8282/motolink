import 'package:flutter/material.dart';

import '../../data/demo_store.dart';
import '../../domain/models.dart';
import '../../platform/riding_core.dart';
import '../team/team_room_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({
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
            titleSpacing: 20,
            title: const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'MotoLink',
                  style: TextStyle(fontWeight: FontWeight.w800),
                ),
                Text(
                  '附近车友 · 骑行组队 · 实时对讲',
                  style: TextStyle(fontSize: 12, color: Colors.white60),
                ),
              ],
            ),
            actions: [
              Padding(
                padding: const EdgeInsets.only(right: 16),
                child: CircleAvatar(
                  backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                  child: const Icon(Icons.two_wheeler_rounded),
                ),
              ),
            ],
          ),
          body: SafeArea(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
              children: [
                _ConnectionBanner(connection: store.connection),
                const SizedBox(height: 14),
                _MapPreview(riders: store.nearbyRiders),
                const SizedBox(height: 18),
                Row(
                  children: [
                    const Expanded(
                      child: Text(
                        '附近车友',
                        style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
                      ),
                    ),
                    TextButton(
                      onPressed: () {},
                      child: const Text('3km 范围'),
                    ),
                  ],
                ),
                ...store.nearbyRiders.map(
                  (rider) => Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: _RiderTile(rider: rider),
                  ),
                ),
                const SizedBox(height: 8),
                _TeamCard(
                  teamName: store.team.name,
                  roomCode: store.team.roomCode,
                  memberCount: store.team.members.length,
                  routeNote: store.team.routeNote,
                  onEnter: () async {
                    await ridingCore.joinTeamRoom(store.team.id);
                    if (!context.mounted) return;
                    await Navigator.of(context).push<void>(
                      MaterialPageRoute<void>(
                        builder: (_) => TeamRoomScreen(
                          store: store,
                          ridingCore: ridingCore,
                        ),
                      ),
                    );
                  },
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _ConnectionBanner extends StatelessWidget {
  const _ConnectionBanner({required this.connection});

  final ConnectionStateStatus connection;

  @override
  Widget build(BuildContext context) {
    final connected = connection == ConnectionStateStatus.connected;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: connected
            ? const Color(0xFF163424)
            : Theme.of(context).colorScheme.errorContainer,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Icon(
            connected ? Icons.check_circle_rounded : Icons.sync_rounded,
            size: 18,
            color: connected ? const Color(0xFF63DB91) : null,
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              connected ? '实时服务已连接' : '网络波动，正在自动重连',
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
          const Text(
            'DEMO',
            style: TextStyle(fontSize: 11, color: Colors.white54),
          ),
        ],
      ),
    );
  }
}

class _MapPreview extends StatelessWidget {
  const _MapPreview({required this.riders});

  final List<Rider> riders;

  @override
  Widget build(BuildContext context) {
    return AspectRatio(
      aspectRatio: 1.25,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: DecoratedBox(
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [Color(0xFF26343A), Color(0xFF10191D)],
            ),
          ),
          child: Stack(
            children: [
              Positioned.fill(
                child: CustomPaint(painter: _MapGridPainter()),
              ),
              const Positioned(
                left: 28,
                top: 42,
                child: _MapMarker(label: '阿泽', active: true),
              ),
              const Positioned(
                right: 44,
                top: 78,
                child: _MapMarker(label: '小雨', active: true),
              ),
              const Positioned(
                right: 88,
                bottom: 32,
                child: _MapMarker(label: '老周', active: false),
              ),
              Align(
                child: Container(
                  width: 56,
                  height: 56,
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.primary,
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.white, width: 3),
                    boxShadow: const [
                      BoxShadow(blurRadius: 18, color: Colors.black54),
                    ],
                  ),
                  child: const Icon(Icons.navigation_rounded, color: Colors.white),
                ),
              ),
              Positioned(
                left: 16,
                right: 16,
                bottom: 14,
                child: Row(
                  children: [
                    _MapPill(icon: Icons.people_alt_rounded, text: '${riders.length} 位车友'),
                    const SizedBox(width: 8),
                    const _MapPill(icon: Icons.radar_rounded, text: '3km'),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _MapGridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.white.withOpacity(0.055)
      ..strokeWidth = 1;
    const step = 38.0;
    for (double x = 0; x < size.width; x += step) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
    }
    for (double y = 0; y < size.height; y += step) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
    final road = Paint()
      ..color = Colors.white.withOpacity(0.11)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 8
      ..strokeCap = StrokeCap.round;
    final path = Path()
      ..moveTo(-20, size.height * .72)
      ..cubicTo(
        size.width * .2,
        size.height * .55,
        size.width * .55,
        size.height * .92,
        size.width + 30,
        size.height * .28,
      );
    canvas.drawPath(path, road);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

class _MapMarker extends StatelessWidget {
  const _MapMarker({required this.label, required this.active});

  final String label;
  final bool active;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          width: 34,
          height: 34,
          decoration: BoxDecoration(
            color: active ? const Color(0xFF4EC77A) : Colors.blueGrey,
            shape: BoxShape.circle,
            border: Border.all(color: Colors.white, width: 2),
          ),
          child: const Icon(Icons.two_wheeler_rounded, size: 18),
        ),
        const SizedBox(height: 3),
        Text(label, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700)),
      ],
    );
  }
}

class _MapPill extends StatelessWidget {
  const _MapPill({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: Colors.black.withOpacity(.55),
        borderRadius: BorderRadius.circular(99),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 15),
          const SizedBox(width: 5),
          Text(text, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

class _RiderTile extends StatelessWidget {
  const _RiderTile({required this.rider});

  final Rider rider;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
        leading: Stack(
          children: [
            CircleAvatar(
              radius: 23,
              backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
              child: const Icon(Icons.person_rounded),
            ),
            Positioned(
              right: 0,
              bottom: 1,
              child: Container(
                width: 11,
                height: 11,
                decoration: BoxDecoration(
                  color: rider.online ? const Color(0xFF55D88A) : Colors.blueGrey,
                  shape: BoxShape.circle,
                  border: Border.all(color: const Color(0xFF17191D), width: 2),
                ),
              ),
            ),
          ],
        ),
        title: Text(rider.nickname, style: const TextStyle(fontWeight: FontWeight.w700)),
        subtitle: Text('${rider.motorcycle} · ${rider.distanceMeters}m'),
        trailing: IconButton.filledTonal(
          onPressed: rider.online ? () {} : null,
          icon: const Icon(Icons.waving_hand_rounded, size: 20),
          tooltip: '打招呼',
        ),
      ),
    );
  }
}

class _TeamCard extends StatelessWidget {
  const _TeamCard({
    required this.teamName,
    required this.roomCode,
    required this.memberCount,
    required this.routeNote,
    required this.onEnter,
  });

  final String teamName;
  final String roomCode;
  final int memberCount;
  final String routeNote;
  final VoidCallback onEnter;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.primaryContainer,
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: const Icon(Icons.groups_rounded),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(teamName, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
                      Text('房间 $roomCode · $memberCount 人在线', style: const TextStyle(color: Colors.white60)),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            Text(routeNote, style: const TextStyle(color: Colors.white70)),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: onEnter,
                icon: const Icon(Icons.headset_mic_rounded),
                label: const Text('进入车队对讲'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
