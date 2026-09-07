import 'package:flutter/material.dart';

import '../models.dart';

class NearbyPage extends StatelessWidget {
  const NearbyPage({required this.user, super.key});

  final UserProfile user;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('附近车友'),
        actions: [
          IconButton(
            tooltip: '筛选',
            onPressed: () => _showMvpNotice(context),
            icon: const Icon(Icons.tune),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          Container(
            height: 330,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(24),
              gradient: const LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [Color(0xFFDDE7EA), Color(0xFFB9CED2)],
              ),
            ),
            child: Stack(
              children: [
                Positioned.fill(
                  child: CustomPaint(painter: _RoadGridPainter()),
                ),
                const Positioned(
                  left: 24,
                  top: 24,
                  child: _RangeBadge(),
                ),
                const Positioned(
                  left: 72,
                  top: 106,
                  child: _RiderPin(label: '阿凯', distance: '850m'),
                ),
                const Positioned(
                  right: 48,
                  top: 74,
                  child: _RiderPin(label: '小北', distance: '1.7km'),
                ),
                const Positioned(
                  right: 88,
                  bottom: 52,
                  child: _RiderPin(label: 'V7', distance: '2.4km'),
                ),
                Positioned(
                  left: 0,
                  right: 0,
                  bottom: 20,
                  child: Center(
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                      decoration: BoxDecoration(
                        color: Colors.black87,
                        borderRadius: BorderRadius.circular(99),
                      ),
                      child: const Text(
                        '地图 SDK 待接入 · 当前为交互占位',
                        style: TextStyle(color: Colors.white, fontSize: 12),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '附近在线',
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
              ),
              const Text('演示数据', style: TextStyle(color: Color(0xFF667085))),
            ],
          ),
          const SizedBox(height: 12),
          const _NearbyCard(
            nickname: '阿凯',
            bike: 'Honda CB650R',
            distance: '850 m',
            years: '骑龄 6 年',
          ),
          const SizedBox(height: 10),
          const _NearbyCard(
            nickname: '小北',
            bike: 'Yamaha MT-07',
            distance: '1.7 km',
            years: '骑龄 3 年',
          ),
          const SizedBox(height: 10),
          const _NearbyCard(
            nickname: 'V7',
            bike: 'Moto Guzzi V7',
            distance: '2.4 km',
            years: '骑龄 8 年',
          ),
        ],
      ),
    );
  }

  void _showMvpNotice(BuildContext context) {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (context) => const Padding(
        padding: EdgeInsets.fromLTRB(24, 8, 24, 32),
        child: Text(
          'MVP 暂不公开真实陌生人精确坐标。后续接入地图时，应做位置模糊化、查询限频、隐身和拉黑。',
          style: TextStyle(fontSize: 16, height: 1.6),
        ),
      ),
    );
  }
}

class _RangeBadge extends StatelessWidget {
  const _RangeBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.92),
        borderRadius: BorderRadius.circular(99),
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.my_location, size: 16),
          SizedBox(width: 6),
          Text('3 km', style: TextStyle(fontWeight: FontWeight.w700)),
        ],
      ),
    );
  }
}

class _RiderPin extends StatelessWidget {
  const _RiderPin({required this.label, required this.distance});

  final String label;
  final String distance;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        CircleAvatar(
          radius: 22,
          backgroundColor: Theme.of(context).colorScheme.primary,
          child: Text(label.characters.first),
        ),
        Container(
          margin: const EdgeInsets.only(top: 4),
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(99),
          ),
          child: Text(distance, style: const TextStyle(fontSize: 11)),
        ),
      ],
    );
  }
}

class _NearbyCard extends StatelessWidget {
  const _NearbyCard({
    required this.nickname,
    required this.bike,
    required this.distance,
    required this.years,
  });

  final String nickname;
  final String bike;
  final String distance;
  final String years;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
        leading: CircleAvatar(
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          child: Text(nickname.characters.first),
        ),
        title: Text(nickname, style: const TextStyle(fontWeight: FontWeight.w700)),
        subtitle: Text('$bike · $years'),
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Text(distance, style: const TextStyle(fontWeight: FontWeight.w800)),
            const Text('在线', style: TextStyle(color: Colors.green, fontSize: 12)),
          ],
        ),
      ),
    );
  }
}

class _RoadGridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.white.withValues(alpha: 0.45)
      ..strokeWidth = 3
      ..style = PaintingStyle.stroke;
    for (double y = 45; y < size.height; y += 72) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y + 36), paint);
    }
    for (double x = 30; x < size.width; x += 95) {
      canvas.drawLine(Offset(x, 0), Offset(x - 42, size.height), paint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
