import 'dart:async';

import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:riding_core/riding_core.dart';

import '../api_client.dart';
import '../models.dart';

class RidePage extends StatefulWidget {
  const RidePage({
    required this.api,
    required this.session,
    required this.activeTeam,
    super.key,
  });

  final MotoLinkApi api;
  final AppSession session;
  final TeamRoom? activeTeam;

  @override
  State<RidePage> createState() => _RidePageState();
}

class _RidePageState extends State<RidePage> {
  final RidingCore _ridingCore = RidingCore.instance;
  final Stopwatch _stopwatch = Stopwatch();
  Timer? _ticker;
  RideSession? _ride;
  bool _busy = false;
  String? _error;
  double _distanceMeters = 0;
  double _maxSpeedMps = 0;

  @override
  void dispose() {
    _ticker?.cancel();
    _stopwatch.stop();
    super.dispose();
  }

  Future<void> _start() async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final whenInUse = await Permission.locationWhenInUse.request();
      if (!whenInUse.isGranted) {
        throw StateError('需要定位权限才能记录骑行');
      }
      await Permission.locationAlways.request();

      final ride = await widget.api.startRide(
        userId: widget.session.user.id,
        roomId: widget.activeTeam?.id,
      );
      await _ridingCore.initialize();
      await _ridingCore.startRide(
        rideId: ride.id,
        roomId: widget.activeTeam?.id,
        userId: widget.session.user.id,
      );
      await _ridingCore.setScreenAwake(true);

      _stopwatch
        ..reset()
        ..start();
      _ticker?.cancel();
      _ticker = Timer.periodic(
        const Duration(seconds: 1),
        (_) => mounted ? setState(() {}) : null,
      );
      if (mounted) {
        setState(() => _ride = ride);
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = error.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  Future<void> _finish() async {
    final ride = _ride;
    if (ride == null) {
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      _ticker?.cancel();
      _stopwatch.stop();
      await _ridingCore.stopRide();
      await _ridingCore.setScreenAwake(false);
      final finished = await widget.api.finishRide(
        rideId: ride.id,
        distanceMeters: _distanceMeters,
        maxSpeedMps: _maxSpeedMps,
      );
      if (mounted) {
        setState(() => _ride = null);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('骑行已保存：${finished.id.substring(0, 8)}')),
        );
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = error.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final riding = _ride != null;
    final elapsed = _stopwatch.elapsed;
    final durationText = [
      elapsed.inHours.toString().padLeft(2, '0'),
      (elapsed.inMinutes % 60).toString().padLeft(2, '0'),
      (elapsed.inSeconds % 60).toString().padLeft(2, '0'),
    ].join(':');

    return Scaffold(
      appBar: AppBar(title: const Text('骑行记录')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: riding ? const Color(0xFF172033) : Colors.white,
              borderRadius: BorderRadius.circular(24),
            ),
            child: Column(
              children: [
                Text(
                  riding ? '骑行进行中' : '准备出发',
                  style: TextStyle(
                    color: riding ? Colors.white70 : const Color(0xFF667085),
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  durationText,
                  style: TextStyle(
                    color: riding ? Colors.white : const Color(0xFF172033),
                    fontSize: 48,
                    fontWeight: FontWeight.w900,
                    letterSpacing: -1,
                  ),
                ),
                const SizedBox(height: 24),
                Row(
                  children: [
                    Expanded(
                      child: _Metric(
                        label: '里程',
                        value: '${(_distanceMeters / 1000).toStringAsFixed(1)} km',
                        dark: riding,
                      ),
                    ),
                    Expanded(
                      child: _Metric(
                        label: '最高速度',
                        value: '${(_maxSpeedMps * 3.6).toStringAsFixed(0)} km/h',
                        dark: riding,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          if (widget.activeTeam != null)
            Card(
              child: ListTile(
                leading: const Icon(Icons.groups),
                title: Text(widget.activeTeam!.name),
                subtitle: Text('轨迹将关联房间 ${widget.activeTeam!.roomCode}'),
              ),
            )
          else
            const Card(
              child: ListTile(
                leading: Icon(Icons.person_pin_circle_outlined),
                title: Text('个人骑行'),
                subtitle: Text('先加入车队可同时启用队内位置共享'),
              ),
            ),
          const SizedBox(height: 20),
          SizedBox(
            height: 58,
            child: riding
                ? FilledButton.icon(
                    style: FilledButton.styleFrom(
                      backgroundColor: Theme.of(context).colorScheme.error,
                    ),
                    onPressed: _busy ? null : _finish,
                    icon: const Icon(Icons.stop_circle_outlined),
                    label: const Text('结束并保存骑行'),
                  )
                : FilledButton.icon(
                    onPressed: _busy ? null : _start,
                    icon: const Icon(Icons.play_arrow),
                    label: const Text('开始骑行'),
                  ),
          ),
          if (_busy) ...[
            const SizedBox(height: 18),
            const Center(child: CircularProgressIndicator()),
          ],
          if (_error != null) ...[
            const SizedBox(height: 16),
            Text(
              _error!,
              textAlign: TextAlign.center,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
          const SizedBox(height: 24),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.secondaryContainer,
              borderRadius: BorderRadius.circular(18),
            ),
            child: const Text(
              '当前原生层已经预留后台定位入口，页面暂未计算真实里程。下一步将定位点先写入手机本地队列，再批量上传 `/rides/{id}/points`，避免弱网丢轨迹。',
              style: TextStyle(height: 1.55),
            ),
          ),
        ],
      ),
    );
  }
}

class _Metric extends StatelessWidget {
  const _Metric({required this.label, required this.value, required this.dark});

  final String label;
  final String value;
  final bool dark;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          label,
          style: TextStyle(color: dark ? Colors.white60 : const Color(0xFF667085)),
        ),
        const SizedBox(height: 6),
        Text(
          value,
          style: TextStyle(
            color: dark ? Colors.white : const Color(0xFF172033),
            fontSize: 20,
            fontWeight: FontWeight.w800,
          ),
        ),
      ],
    );
  }
}
