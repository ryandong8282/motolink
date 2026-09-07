import 'package:flutter/material.dart';

import '../api_client.dart';
import '../models.dart';
import 'nearby_page.dart';
import 'profile_page.dart';
import 'ride_page.dart';
import 'team_page.dart';
import 'team_room_page.dart';

class HomeShell extends StatefulWidget {
  const HomeShell({
    required this.api,
    required this.session,
    required this.onLogout,
    super.key,
  });

  final MotoLinkApi api;
  final AppSession session;
  final VoidCallback onLogout;

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;
  TeamRoom? _activeTeam;

  Future<void> _openTeam(TeamRoom team) async {
    setState(() => _activeTeam = team);
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => TeamRoomPage(
          api: widget.api,
          session: widget.session,
          initialTeam: team,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final pages = <Widget>[
      NearbyPage(user: widget.session.user),
      TeamPage(
        api: widget.api,
        session: widget.session,
        activeTeam: _activeTeam,
        onOpenTeam: _openTeam,
      ),
      RidePage(
        api: widget.api,
        session: widget.session,
        activeTeam: _activeTeam,
      ),
      ProfilePage(session: widget.session, onLogout: widget.onLogout),
    ];

    return Scaffold(
      body: IndexedStack(index: _index, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (value) => setState(() => _index = value),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.radar_outlined),
            selectedIcon: Icon(Icons.radar),
            label: '附近',
          ),
          NavigationDestination(
            icon: Icon(Icons.groups_outlined),
            selectedIcon: Icon(Icons.groups),
            label: '车队',
          ),
          NavigationDestination(
            icon: Icon(Icons.route_outlined),
            selectedIcon: Icon(Icons.route),
            label: '骑行',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: '我的',
          ),
        ],
      ),
    );
  }
}
