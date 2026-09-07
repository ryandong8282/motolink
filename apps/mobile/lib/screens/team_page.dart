import 'package:flutter/material.dart';

import '../api_client.dart';
import '../models.dart';

class TeamPage extends StatefulWidget {
  const TeamPage({
    required this.api,
    required this.session,
    required this.activeTeam,
    required this.onOpenTeam,
    super.key,
  });

  final MotoLinkApi api;
  final AppSession session;
  final TeamRoom? activeTeam;
  final ValueChanged<TeamRoom> onOpenTeam;

  @override
  State<TeamPage> createState() => _TeamPageState();
}

class _TeamPageState extends State<TeamPage> {
  final _teamNameController = TextEditingController(text: '周末骑行队');
  final _roomCodeController = TextEditingController();
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _teamNameController.dispose();
    _roomCodeController.dispose();
    super.dispose();
  }

  Future<void> _createTeam() async {
    final name = _teamNameController.text.trim();
    if (name.isEmpty) {
      setState(() => _error = '请输入车队名称');
      return;
    }
    await _run(() => widget.api.createTeam(
          leaderId: widget.session.user.id,
          name: name,
        ));
  }

  Future<void> _joinTeam() async {
    final code = _roomCodeController.text.trim();
    if (!RegExp(r'^\d{6}$').hasMatch(code)) {
      setState(() => _error = '房间号应为 6 位数字');
      return;
    }
    await _run(() => widget.api.joinTeam(
          userId: widget.session.user.id,
          roomCode: code,
        ));
  }

  Future<void> _run(Future<TeamRoom> Function() operation) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final team = await operation();
      if (mounted) {
        widget.onOpenTeam(team);
      }
    } on ApiException catch (exception) {
      if (mounted) {
        setState(() => _error = exception.message);
      }
    } catch (exception) {
      if (mounted) {
        setState(() => _error = exception.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('我的车队')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          if (widget.activeTeam != null) ...[
            _ActiveTeamCard(
              team: widget.activeTeam!,
              onTap: () => widget.onOpenTeam(widget.activeTeam!),
            ),
            const SizedBox(height: 20),
          ],
          Text(
            '创建车队',
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
          ),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                children: [
                  TextField(
                    controller: _teamNameController,
                    decoration: const InputDecoration(
                      labelText: '车队名称',
                      prefixIcon: Icon(Icons.flag_outlined),
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 14),
                  SizedBox(
                    width: double.infinity,
                    height: 48,
                    child: FilledButton.icon(
                      onPressed: _loading ? null : _createTeam,
                      icon: const Icon(Icons.add),
                      label: const Text('创建并进入'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          Text(
            '加入已有车队',
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
          ),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                children: [
                  TextField(
                    controller: _roomCodeController,
                    keyboardType: TextInputType.number,
                    maxLength: 6,
                    decoration: const InputDecoration(
                      labelText: '6 位房间号',
                      prefixIcon: Icon(Icons.dialpad),
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 6),
                  SizedBox(
                    width: double.infinity,
                    height: 48,
                    child: OutlinedButton.icon(
                      onPressed: _loading ? null : _joinTeam,
                      icon: const Icon(Icons.login),
                      label: const Text('加入车队'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          if (_loading) ...[
            const SizedBox(height: 20),
            const Center(child: CircularProgressIndicator()),
          ],
          if (_error != null) ...[
            const SizedBox(height: 18),
            Text(
              _error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
              textAlign: TextAlign.center,
            ),
          ],
          const SizedBox(height: 28),
          const _MvpTip(),
        ],
      ),
    );
  }
}

class _ActiveTeamCard extends StatelessWidget {
  const _ActiveTeamCard({required this.team, required this.onTap});

  final TeamRoom team;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: const Color(0xFF172033),
      child: InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Row(
            children: [
              Container(
                width: 52,
                height: 52,
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.primary,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: const Icon(Icons.graphic_eq, size: 30),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '当前车队',
                      style: TextStyle(color: Colors.white60, fontSize: 12),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      team.name,
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w800,
                        fontSize: 18,
                      ),
                    ),
                    Text(
                      '房间 ${team.roomCode} · ${team.members.length}/${team.maxMembers} 人',
                      style: const TextStyle(color: Colors.white70),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: Colors.white),
            ],
          ),
        ),
      ),
    );
  }
}

class _MvpTip extends StatelessWidget {
  const _MvpTip();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.secondaryContainer,
        borderRadius: BorderRadius.circular(18),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.science_outlined),
          SizedBox(width: 12),
          Expanded(
            child: Text(
              '当前先验证房间与抢麦闭环。扫码入队、附近车队和队长管理放到下一小步。',
              style: TextStyle(height: 1.5),
            ),
          ),
        ],
      ),
    );
  }
}
