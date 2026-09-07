import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/auth_repository.dart';
import '../domain/auth_session.dart';

final authControllerProvider =
    StateNotifierProvider<AuthController, AsyncValue<AuthSession?>>((ref) {
  return AuthController(ref.watch(authRepositoryProvider));
});

class AuthController extends StateNotifier<AsyncValue<AuthSession?>> {
  AuthController(this._repository) : super(const AsyncData(null));

  final AuthRepository _repository;

  Future<void> login({required String phone, required String nickname}) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => _repository.devLogin(phone: phone, nickname: nickname),
    );
  }

  void logout() => state = const AsyncData(null);
}
