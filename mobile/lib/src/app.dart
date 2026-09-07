import 'package:flutter/material.dart';

import 'data/demo_store.dart';
import 'features/home/home_screen.dart';
import 'platform/riding_core.dart';

class MotoLinkApp extends StatefulWidget {
  const MotoLinkApp({super.key});

  @override
  State<MotoLinkApp> createState() => _MotoLinkAppState();
}

class _MotoLinkAppState extends State<MotoLinkApp> {
  late final DemoStore _store;
  late final RidingCore _ridingCore;

  @override
  void initState() {
    super.initState();
    _store = DemoStore();
    _ridingCore = const MockRidingCore();
    _ridingCore.initialize();
  }

  @override
  void dispose() {
    _store.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    const accent = Color(0xFFFF6B35);
    final colorScheme = ColorScheme.fromSeed(
      seedColor: accent,
      brightness: Brightness.dark,
      surface: const Color(0xFF17191D),
    );

    return MaterialApp(
      title: 'MotoLink',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: colorScheme,
        scaffoldBackgroundColor: const Color(0xFF0D0F12),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF0D0F12),
          elevation: 0,
        ),
        cardTheme: CardThemeData(
          color: const Color(0xFF17191D),
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
            side: const BorderSide(color: Color(0xFF292C32)),
          ),
        ),
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            minimumSize: const Size(0, 52),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
        ),
      ),
      home: HomeScreen(store: _store, ridingCore: _ridingCore),
    );
  }
}
