import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/theme/app_theme.dart';
import 'features/auth/application/auth_controller.dart';
import 'features/auth/presentation/login_page.dart';
import 'features/home/presentation/home_shell.dart';

class MotoLinkApp extends ConsumerWidget {
  const MotoLinkApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);

    return MaterialApp(
      title: 'MotoLink',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      home: auth.when(
        data: (session) => session == null
            ? const LoginPage()
            : HomeShell(session: session),
        loading: () => const Scaffold(
          body: Center(child: CircularProgressIndicator()),
        ),
        error: (error, _) => Scaffold(
          body: Center(child: Text('启动失败：$error')),
        ),
      ),
    );
  }
}
