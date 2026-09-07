import 'package:flutter/material.dart';
import 'package:riding_core/riding_core.dart';

import '../models.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({
    required this.session,
    required this.onLogout,
    super.key,
  });

  final AppSession session;
  final VoidCallback onLogout;

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  String _coreStatus = '尚未检测';
  bool _checking = false;

  Future<void> _checkCore() async {
    setState(() => _checking = true);
    try {
      final status = await RidingCore.instance.getStatus();
      if (mounted) {
        setState(() => _coreStatus =
            '${status.platform} · ${status.provider} · ${status.state}');
      }
    } catch (error) {
      if (mounted) {
        setState(() => _coreStatus = error.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _checking = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = widget.session.user;
    return Scaffold(
      appBar: AppBar(title: const Text('我的')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 32,
                    backgroundColor: Theme.of(context).colorScheme.primary,
                    child: Text(
                      user.nickname.characters.first,
                      style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w900),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          user.nickname,
                          style: Theme.of(context).textTheme.titleLarge?.copyWith(
                                fontWeight: FontWeight.w900,
                              ),
                        ),
                        const SizedBox(height: 4),
                        Text(user.phone),
                        const SizedBox(height: 4),
                        Text(
                          'ID ${user.id.substring(0, 8)}',
                          style: const TextStyle(color: Color(0xFF667085)),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 18),
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.memory),
                  title: const Text('RidingCore 状态'),
                  subtitle: Text(_coreStatus),
                  trailing: _checking
                      ? const SizedBox.square(
                          dimension: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.chevron_right),
                  onTap: _checking ? null : _checkCore,
                ),
                const Divider(height: 1),
                const ListTile(
                  leading: Icon(Icons.shield_outlined),
                  title: Text('隐私与权限'),
                  subtitle: Text('MVP 仅展示入口，正式文本由运营方确认'),
                ),
                const Divider(height: 1),
                const ListTile(
                  leading: Icon(Icons.info_outline),
                  title: Text('版本'),
                  subtitle: Text('0.1.0 MVP scaffold'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          OutlinedButton.icon(
            onPressed: widget.onLogout,
            icon: const Icon(Icons.logout),
            label: const Text('退出开发态账号'),
          ),
        ],
      ),
    );
  }
}
