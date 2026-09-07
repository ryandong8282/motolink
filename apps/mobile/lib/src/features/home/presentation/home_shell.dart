import 'package:flutter/material.dart';

import '../../auth/domain/auth_session.dart';
import '../../nearby/presentation/nearby_page.dart';
import '../../profile/presentation/profile_page.dart';
import '../../rides/presentation/ride_page.dart';

class HomeShell extends StatefulWidget {
  const HomeShell({required this.session, super.key});

  final AuthSession session;

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    final pages = [
      NearbyPage(session: widget.session),
      RidePage(session: widget.session),
      ProfilePage(session: widget.session),
    ];

    return Scaffold(
      body: IndexedStack(index: _index, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (value) => setState(() => _index = value),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.radar), label: '附近'),
          NavigationDestination(icon: Icon(Icons.route), label: '骑行'),
          NavigationDestination(icon: Icon(Icons.person), label: '我的'),
        ],
      ),
    );
  }
}
