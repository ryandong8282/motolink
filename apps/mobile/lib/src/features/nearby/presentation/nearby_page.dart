import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/domain/auth_session.dart';
import '../../room/data/room_repository.dart';
import '../../room/domain/ride_room.dart';
import '../../room/presentation/room_page.dart';
import '../data/nearby_repository.dart';
import '../domain/nearby_rider.dart';

class NearbyPage extends ConsumerStatefulWidget {
  const NearbyPage({required this.session, super.key});

  final AuthSession session;

  @override
  ConsumerState<NearbyPage> createState() => _NearbyPageState();
}

class _NearbyPageState extends ConsumerState<NearbyPage> {
  late Future<List<NearbyRider>> _future;

  @override
  void initState() {
    super.initState();
    _future = _load();
  }

  Future<List<NearbyRider>> _load() {
    return ref.read(nearbyRepositoryProvider).list(
          userId: widget.session.userId,
        );
  }

  Future<void> _openRoom(RideRoomLoader loader, String failurePrefix) async {
    try {
      final room = await loader();
      if (!mounted) return;
      await Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => RoomPage(session: widget.session, room: room),
        ),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$failurePrefix：$error')),
      );
    }
  }

  Future<void> _createRoom() {
    return _openRoom(
      () => ref.read(roomRepositoryProvider).createRoom(
            userId: widget.session.userId,
            roomName: '周末轻骑队',
          ),
      '创建车队失败',
    );
  }

  Future<void> _joinRoom() async {
    final codeController = TextEditingController(text: '952701');
    final roomCode = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('加入车队'),
        content: TextField(
          controller: codeController,
          keyboardType: TextInputType.number,
          maxLength: 6,
          decoration: const InputDecoration(labelText: '6 位房间号'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(
              dialogContext,
              codeController.text.trim(),
            ),
            child: const Text('加入'),
          ),
        ],
      ),
    );
    codeController.dispose();
    if (roomCode == null || roomCode.length != 6 || !mounted) return;

    await _openRoom(
      () => ref.read(roomRepositoryProvider).joinRoom(
            userId: widget.session.userId,
            roomCode: roomCode,
          ),
      '加入车队失败',
    );
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: RefreshIndicator(
        onRefresh: () async {
          setState(() => _future = _load());
          await _future;
        },
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    '附近车友',
                    style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                          fontWeight: FontWeight.w800,
                        ),
                  ),
                ),
                IconButton(
                  tooltip: '输入房间号加入',
                  onPressed: _joinRoom,
                  icon: const Icon(Icons.login),
                ),
                const SizedBox(width: 4),
                FilledButton.icon(
                  onPressed: _createRoom,
                  icon: const Icon(Icons.groups),
                  label: const Text('创建车队'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            const _MapPlaceholder(),
            const SizedBox(height: 16),
            FutureBuilder<List<NearbyRider>>(
              future: _future,
              builder: (context, snapshot) {
                if (snapshot.connectionState != ConnectionState.done) {
                  return const Center(child: CircularProgressIndicator());
                }
                if (snapshot.hasError) {
                  return Text('加载失败：${snapshot.error}');
                }
                final riders = snapshot.data ?? const [];
                return Column(
                  children: riders
                      .map(
                        (rider) => Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: _RiderCard(rider: rider),
                        ),
                      )
                      .toList(),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

typedef RideRoomLoader = Future<RideRoom> Function();

class _MapPlaceholder extends StatelessWidget {
  const _MapPlaceholder();

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 210,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: const LinearGradient(
          colors: [Color(0xFFE9F1ED), Color(0xFFD7E4DE)],
        ),
      ),
      child: Stack(
        children: [
          const Center(
            child: Icon(Icons.my_location, size: 48, color: Color(0xFF316A57)),
          ),
          ...const [
            Positioned(left: 44, top: 54, child: _MapPin()),
            Positioned(right: 48, top: 82, child: _MapPin()),
            Positioned(left: 108, bottom: 34, child: _MapPin()),
          ],
          Positioned(
            left: 12,
            bottom: 12,
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.9),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Padding(
                padding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                child: Text('地图 SDK 待 POC 后接入'),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MapPin extends StatelessWidget {
  const _MapPin();

  @override
  Widget build(BuildContext context) {
    return const CircleAvatar(
      radius: 16,
      child: Icon(Icons.two_wheeler, size: 18),
    );
  }
}

class _RiderCard extends StatelessWidget {
  const _RiderCard({required this.rider});

  final NearbyRider rider;

  @override
  Widget build(BuildContext context) {
    final distance = rider.distanceMeters < 1000
        ? '${rider.distanceMeters.toStringAsFixed(0)} 米'
        : '${(rider.distanceMeters / 1000).toStringAsFixed(1)} 公里';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            CircleAvatar(
              radius: 25,
              child: Text(rider.nickname.characters.first),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    rider.nickname,
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 4),
                  Text('${rider.motorcycle} · $distance'),
                ],
              ),
            ),
            OutlinedButton(
              onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('已向 ${rider.nickname} 发出组队邀请（演示）')),
              ),
              child: const Text('邀请'),
            ),
          ],
        ),
      ),
    );
  }
}
