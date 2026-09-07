import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/domain/auth_session.dart';
import '../application/ride_controller.dart';

class RidePage extends ConsumerWidget {
  const RidePage({required this.session, super.key});

  final AuthSession session;

  String _duration(int seconds) {
    final hours = seconds ~/ 3600;
    final minutes = (seconds % 3600) ~/ 60;
    final remain = seconds % 60;
    return [hours, minutes, remain]
        .map((value) => value.toString().padLeft(2, '0'))
        .join(':');
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final args = RideControllerArgs(session.userId);
    final ride = ref.watch(rideControllerProvider(args));
    final controller = ref.read(rideControllerProvider(args).notifier);

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            '骑行记录',
            style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
          ),
          const SizedBox(height: 16),
          Container(
            height: 260,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(24),
              color: const Color(0xFF172126),
            ),
            alignment: Alignment.center,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  ride.speedKmh.toStringAsFixed(0),
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 76,
                    height: 1,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const Text(
                  'km/h',
                  style: TextStyle(color: Colors.white70, fontSize: 18),
                ),
                const SizedBox(height: 20),
                Text(
                  ride.active ? '正在记录轨迹' : '等待开始',
                  style: const TextStyle(color: Colors.white),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _MetricCard(
                  label: '时长',
                  value: _duration(ride.elapsedSeconds),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _MetricCard(
                  label: '里程',
                  value: '${ride.distanceKm.toStringAsFixed(2)} km',
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _MetricCard(
                  label: '最高速度',
                  value: '${ride.maxSpeedKmh.toStringAsFixed(0)} km/h',
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _MetricCard(
                  label: '已同步点',
                  value: '${ride.pointCount}',
                ),
              ),
            ],
          ),
          if (ride.error != null) ...[
            const SizedBox(height: 12),
            Text(
              ride.error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
          const SizedBox(height: 20),
          FilledButton.icon(
            onPressed: ride.starting
                ? null
                : ride.active
                    ? controller.finish
                    : controller.start,
            icon: Icon(ride.active ? Icons.stop : Icons.play_arrow),
            label: Padding(
              padding: const EdgeInsets.symmetric(vertical: 14),
              child: Text(
                ride.starting
                    ? '正在开始…'
                    : ride.active
                        ? '结束骑行'
                        : '开始骑行',
              ),
            ),
          ),
          const SizedBox(height: 12),
          const Text(
            '当前用模拟 GPS 点验证前后端轨迹链路。正式版本必须改为本地持久化优先、弱网批量补传。',
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: Theme.of(context).textTheme.bodySmall),
            const SizedBox(height: 6),
            Text(
              value,
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
            ),
          ],
        ),
      ),
    );
  }
}
