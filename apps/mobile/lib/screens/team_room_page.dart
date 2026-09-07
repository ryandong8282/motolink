import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:riding_core/riding_core.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

import '../api_client.dart';
import '../models.dart';

class TeamRoomPage extends StatefulWidget {
  const TeamRoomPage({
    required this.api,
    required this.session,
    required this.initialTeam,
    super.key,
  });

  final MotoLinkApi api;
  final AppSession session;
  final TeamRoom initialTeam;

  @override
  State<TeamRoomPage> createState() => _TeamRoomPageState();
}

class _TeamRoomPageState extends State<TeamRoomPage> {
  final RidingCore _ridingCore = RidingCore.instance;
  final List<String> _events = <String>[];

  late TeamRoom _team = widget.initialTeam;
  WebSocketChannel? _socket;
  StreamSubscription<dynamic>? _socketSubscription;
  Timer? _heartbeatTimer;
  bool _connecting = true;
  bool _requestingFloor = false;
  bool _transmitting = false;
  String _status = '正在初始化骑行核心…';

  @override
  void initState() {
    super.initState();
    unawaited(_initialize());
  }

  Future<void> _initialize() async {
    try {
      final permissions = await <Permission>[
        Permission.microphone,
        Permission.locationWhenInUse,
      ].request();
      final microphoneGranted = permissions[Permission.microphone]?.isGranted ?? false;
      if (!microphoneGranted) {
        throw StateError('需要麦克风权限才能使用对讲');
      }

      await _ridingCore.initialize();
      await _ridingCore.joinChannel(
        roomId: _team.id,
        userId: widget.session.user.id,
        provider: 'mock',
        token: 'local-dev-token',
      );
      await _ridingCore.setScreenAwake(true);

      final socket = WebSocketChannel.connect(
        widget.api.teamSocketUri(
          roomId: _team.id,
          userId: widget.session.user.id,
        ),
      );
      _socket = socket;
      _socketSubscription = socket.stream.listen(
        _handleSocketMessage,
        onError: (Object error, StackTrace stackTrace) {
          if (mounted) {
            setState(() => _status = '实时通道异常：$error');
          }
        },
        onDone: () {
          if (mounted && !_transmitting) {
            setState(() => _status = '实时通道已断开');
          }
        },
      );
      _sendEvent('presence', '${widget.session.user.nickname} 已进入车队');
      if (mounted) {
        setState(() {
          _connecting = false;
          _status = '房间已连接 · RTC 当前为 Mock';
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() {
          _connecting = false;
          _status = error.toString();
        });
      }
    }
  }

  void _handleSocketMessage(dynamic payload) {
    try {
      final event = jsonDecode(payload as String) as Map<String, dynamic>;
      if (event['userId'] == widget.session.user.id) {
        return;
      }
      final text = event['message']?.toString();
      if (text == null || text.isEmpty || !mounted) {
        return;
      }
      setState(() {
        _events.insert(0, text);
        if (_events.length > 5) {
          _events.removeLast();
        }
      });
    } catch (_) {
      // MVP relay accepts opaque messages; malformed peer payloads are ignored.
    }
  }

  void _sendEvent(String type, String message) {
    _socket?.sink.add(jsonEncode({
      'version': 1,
      'type': type,
      'roomId': _team.id,
      'userId': widget.session.user.id,
      'nickname': widget.session.user.nickname,
      'message': message,
      'sentAt': DateTime.now().toUtc().toIso8601String(),
    }));
  }

  Future<void> _pressToTalk() async {
    if (_connecting || _requestingFloor || _transmitting) {
      return;
    }
    setState(() {
      _requestingFloor = true;
      _status = '正在申请麦权…';
    });
    await HapticFeedback.mediumImpact();

    try {
      final lease = await widget.api.requestFloor(
        roomId: _team.id,
        userId: widget.session.user.id,
      );
      if (!lease.granted) {
        if (mounted) {
          setState(() {
            _status = lease.holderUserId == null
                ? '麦权暂不可用，请重试'
                : '其他队员正在讲话';
          });
        }
        return;
      }

      await _ridingCore.startTransmitting();
      _heartbeatTimer?.cancel();
      _heartbeatTimer = Timer.periodic(
        const Duration(seconds: 10),
        (_) => unawaited(_renewFloor()),
      );
      _sendEvent('ptt.started', '${widget.session.user.nickname} 正在讲话');
      if (mounted) {
        setState(() {
          _transmitting = true;
          _status = '正在讲话 · 松手结束';
        });
      }
    } on ApiException catch (error) {
      if (mounted) {
        setState(() => _status = error.message);
      }
    } catch (error) {
      if (mounted) {
        setState(() => _status = error.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _requestingFloor = false);
      }
    }
  }

  Future<void> _renewFloor() async {
    if (!_transmitting) {
      return;
    }
    try {
      final lease = await widget.api.heartbeatFloor(
        roomId: _team.id,
        userId: widget.session.user.id,
      );
      if (!lease.granted) {
        await _releaseTalk(reason: '麦权已失效');
      }
    } catch (_) {
      // One missed heartbeat is tolerated; Redis TTL remains the final guard.
    }
  }

  Future<void> _releaseTalk({String? reason}) async {
    if (!_transmitting && !_requestingFloor) {
      return;
    }
    _heartbeatTimer?.cancel();
    _heartbeatTimer = null;

    try {
      await _ridingCore.stopTransmitting();
    } finally {
      try {
        await widget.api.releaseFloor(
          roomId: _team.id,
          userId: widget.session.user.id,
        );
      } catch (_) {
        // The server-side TTL releases an orphaned floor automatically.
      }
    }

    _sendEvent('ptt.stopped', '${widget.session.user.nickname} 结束讲话');
    if (mounted) {
      setState(() {
        _transmitting = false;
        _requestingFloor = false;
        _status = reason ?? '按住按钮讲话';
      });
    }
  }

  Future<void> _refreshTeam() async {
    try {
      final team = await widget.api.getTeam(_team.id);
      if (mounted) {
        setState(() => _team = team);
      }
    } on ApiException catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(error.message)),
        );
      }
    }
  }

  @override
  void dispose() {
    _heartbeatTimer?.cancel();
    if (_transmitting) {
      unawaited(_ridingCore.stopTransmitting());
      unawaited(widget.api.releaseFloor(
        roomId: _team.id,
        userId: widget.session.user.id,
      ));
    }
    _sendEvent('presence', '${widget.session.user.nickname} 已离开车队');
    unawaited(_socketSubscription?.cancel());
    unawaited(_socket?.sink.close());
    unawaited(_ridingCore.leaveChannel());
    unawaited(_ridingCore.setScreenAwake(false));
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: Text(_team.name),
        actions: [
          IconButton(
            onPressed: _refreshTeam,
            tooltip: '刷新成员',
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 28),
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: const Color(0xFF172033),
                borderRadius: BorderRadius.circular(24),
              ),
              child: Row(
                children: [
                  Container(
                    width: 56,
                    height: 56,
                    decoration: BoxDecoration(
                      color: colorScheme.primary,
                      borderRadius: BorderRadius.circular(18),
                    ),
                    child: const Icon(Icons.two_wheeler, size: 30),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '房间号 ${_team.roomCode}',
                          style: const TextStyle(color: Colors.white60),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${_team.members.length}/${_team.maxMembers} 人在线房间',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 19,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: Colors.green.withValues(alpha: 0.18),
                      borderRadius: BorderRadius.circular(99),
                    ),
                    child: const Text(
                      '已连接',
                      style: TextStyle(color: Color(0xFF84E1A5), fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 18),
            Text(
              '队员',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
            ),
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: _team.members
                  .map((member) => Chip(
                        avatar: CircleAvatar(
                          child: Text(member.nickname.characters.first),
                        ),
                        label: Text(
                          member.role == 'LEADER'
                              ? '${member.nickname} · 队长'
                              : member.nickname,
                        ),
                      ))
                  .toList(growable: false),
            ),
            const SizedBox(height: 32),
            Center(
              child: Text(
                _status,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: _transmitting ? colorScheme.error : null,
                    ),
              ),
            ),
            const SizedBox(height: 18),
            Center(
              child: Listener(
                onPointerDown: (_) => unawaited(_pressToTalk()),
                onPointerUp: (_) => unawaited(_releaseTalk()),
                onPointerCancel: (_) => unawaited(_releaseTalk()),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 140),
                  width: 196,
                  height: 196,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: _transmitting ? colorScheme.error : colorScheme.primary,
                    boxShadow: [
                      BoxShadow(
                        color: (_transmitting ? colorScheme.error : colorScheme.primary)
                            .withValues(alpha: 0.28),
                        blurRadius: _transmitting ? 36 : 22,
                        spreadRadius: _transmitting ? 12 : 4,
                      ),
                    ],
                  ),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        _transmitting ? Icons.mic : Icons.mic_none,
                        size: 58,
                        color: _transmitting ? Colors.white : Colors.black87,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        _transmitting ? '松手结束' : '按住说话',
                        style: TextStyle(
                          color: _transmitting ? Colors.white : Colors.black87,
                          fontSize: 18,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 34),
            if (_events.isNotEmpty) ...[
              Text(
                '房间事件',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
              ),
              const SizedBox(height: 8),
              ..._events.map((event) => ListTile(
                    dense: true,
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.graphic_eq, size: 20),
                    title: Text(event),
                  )),
              const SizedBox(height: 14),
            ],
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: colorScheme.secondaryContainer,
                borderRadius: BorderRadius.circular(18),
              ),
              child: const Text(
                '现在已实现服务端抢麦和客户端按住交互，但音频媒体流仍是 Mock。接入真实 RTC 后，只有获得麦权的客户端才能发布本地音频。',
                style: TextStyle(height: 1.55),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
