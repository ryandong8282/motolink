import 'package:flutter/material.dart';

import '../api_client.dart';
import '../models.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({
    required this.api,
    required this.onLoggedIn,
    super.key,
  });

  final MotoLinkApi api;
  final ValueChanged<AppSession> onLoggedIn;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _phoneController = TextEditingController(text: '13800000000');
  final _nicknameController = TextEditingController(text: 'RoadRider');
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _phoneController.dispose();
    _nicknameController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final session = await widget.api.devLogin(
        phone: _phoneController.text.trim(),
        nickname: _nicknameController.text.trim(),
      );
      if (!mounted) {
        return;
      }
      widget.onLoggedIn(session);
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
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 430),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Container(
                      width: 64,
                      height: 64,
                      decoration: BoxDecoration(
                        color: Theme.of(context).colorScheme.primary,
                        borderRadius: BorderRadius.circular(22),
                      ),
                      child: const Icon(Icons.two_wheeler, size: 34),
                    ),
                  ),
                  const SizedBox(height: 32),
                  Text(
                    'MotoLink',
                    style: Theme.of(context).textTheme.displaySmall?.copyWith(
                          fontWeight: FontWeight.w900,
                          letterSpacing: -1.5,
                        ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '先把组队、麦权与骑行链路跑通。',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                  ),
                  const SizedBox(height: 36),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(22),
                      child: Form(
                        key: _formKey,
                        child: Column(
                          children: [
                            TextFormField(
                              controller: _phoneController,
                              keyboardType: TextInputType.phone,
                              decoration: const InputDecoration(
                                labelText: '手机号（开发态）',
                                prefixIcon: Icon(Icons.phone_android),
                                border: OutlineInputBorder(),
                              ),
                              validator: (value) {
                                final text = value?.trim() ?? '';
                                if (text.length < 6) {
                                  return '请输入有效测试手机号';
                                }
                                return null;
                              },
                            ),
                            const SizedBox(height: 16),
                            TextFormField(
                              controller: _nicknameController,
                              decoration: const InputDecoration(
                                labelText: '骑行昵称',
                                prefixIcon: Icon(Icons.person_outline),
                                border: OutlineInputBorder(),
                              ),
                              validator: (value) =>
                                  (value?.trim().isEmpty ?? true) ? '请输入昵称' : null,
                            ),
                            if (_error != null) ...[
                              const SizedBox(height: 14),
                              Align(
                                alignment: Alignment.centerLeft,
                                child: Text(
                                  _error!,
                                  style: TextStyle(
                                    color: Theme.of(context).colorScheme.error,
                                  ),
                                ),
                              ),
                            ],
                            const SizedBox(height: 22),
                            SizedBox(
                              width: double.infinity,
                              height: 52,
                              child: FilledButton.icon(
                                onPressed: _loading ? null : _submit,
                                icon: _loading
                                    ? const SizedBox.square(
                                        dimension: 18,
                                        child: CircularProgressIndicator(strokeWidth: 2),
                                      )
                                    : const Icon(Icons.login),
                                label: Text(_loading ? '正在连接…' : '进入 MVP'),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 18),
                  const Text(
                    '当前登录仅用于开发验证。正式版本必须接入短信验证码、Token 刷新和风控。',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: Color(0xFF667085), height: 1.5),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
