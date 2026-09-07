import 'package:flutter/material.dart';

import 'api_client.dart';
import 'models.dart';
import 'screens/home_shell.dart';
import 'screens/login_page.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MotoLinkApp());
}

class MotoLinkApp extends StatefulWidget {
  const MotoLinkApp({super.key});

  @override
  State<MotoLinkApp> createState() => _MotoLinkAppState();
}

class _MotoLinkAppState extends State<MotoLinkApp> {
  late final MotoLinkApi _api = MotoLinkApi();
  AppSession? _session;

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    const seed = Color(0xFFF4BE32);
    return MaterialApp(
      title: 'MotoLink',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: seed,
          brightness: Brightness.light,
        ),
        scaffoldBackgroundColor: const Color(0xFFF5F6F8),
        useMaterial3: true,
        cardTheme: const CardThemeData(
          elevation: 0,
          margin: EdgeInsets.zero,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.all(Radius.circular(20)),
          ),
        ),
      ),
      home: _session == null
          ? LoginPage(
              api: _api,
              onLoggedIn: (session) => setState(() => _session = session),
            )
          : HomeShell(
              api: _api,
              session: _session!,
              onLogout: () => setState(() => _session = null),
            ),
    );
  }
}
