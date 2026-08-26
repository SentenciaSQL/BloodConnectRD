import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_client.dart';
import '../../../core/networking/api_models.dart';
import '../../../core/services/firebase_services.dart';
import '../data/auth_repository.dart';

class AuthState {
  const AuthState({
    this.user,
    this.isInitializing = false,
    this.isSubmitting = false,
    this.errorMessage,
  });

  final AppUser? user;
  final bool isInitializing;
  final bool isSubmitting;
  final String? errorMessage;

  bool get isAuthenticated => user != null;

  AuthState copyWith({
    AppUser? user,
    bool clearUser = false,
    bool? isInitializing,
    bool? isSubmitting,
    String? errorMessage,
    bool clearError = false,
  }) {
    return AuthState(
      user: clearUser ? null : (user ?? this.user),
      isInitializing: isInitializing ?? this.isInitializing,
      isSubmitting: isSubmitting ?? this.isSubmitting,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }
}

class AuthController extends StateNotifier<AuthState> {
  AuthController(
    this._repository,
    this._messaging,
    Stream<void> sessionExpirations,
  ) : super(const AuthState(isInitializing: true)) {
    _expirationSubscription = sessionExpirations.listen((_) {
      sessionExpired();
    });
    _restore();
  }

  final AuthRepository _repository;
  final FirebaseMessagingService _messaging;
  late final StreamSubscription<void> _expirationSubscription;

  Future<void> _restore() async {
    try {
      final user = await _repository.restoreSession();
      state = AuthState(user: user);
      await _messaging.start();
    } on NoStoredSession {
      state = const AuthState();
    } catch (_) {
      await _repository.clearSession();
      state = const AuthState();
    }
  }

  Future<bool> login({required String email, required String password}) async {
    state = state.copyWith(isSubmitting: true, clearError: true);
    try {
      final auth = await _repository.login(email: email, password: password);
      state = AuthState(user: auth.user);
      await _messaging.start();
      return true;
    } catch (error) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: friendlyError(error),
      );
      return false;
    }
  }

  Future<String?> register({
    required String firstName,
    required String lastName,
    required String email,
    required String phone,
    required String password,
    required String confirmPassword,
  }) async {
    state = state.copyWith(isSubmitting: true, clearError: true);

    try {
      final message = await _repository.register(
        firstName: firstName,
        lastName: lastName,
        email: email,
        phone: phone,
        password: password,
        confirmPassword: confirmPassword,
      );

      state = const AuthState();
      return message;
    } catch (error) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: friendlyError(error),
      );
      return null;
    }
  }

  Future<String?> resendVerification(String email) async {
    state = state.copyWith(isSubmitting: true, clearError: true);

    try {
      final message = await _repository.resendVerification(email);
      state = state.copyWith(isSubmitting: false, clearError: true);
      return message;
    } catch (error) {
      state = state.copyWith(
        isSubmitting: false,
        errorMessage: friendlyError(error),
      );
      return null;
    }
  }

  Future<void> logout() async {
    state = state.copyWith(isSubmitting: true, clearError: true);
    await _messaging.unregister();
    try {
      await _repository.logout();
    } catch (_) {
      await _repository.clearSession();
    }
    state = const AuthState();
  }

  Future<void> sessionExpired() async {
    await _repository.clearSession();
    state = const AuthState(
      errorMessage: 'Tu sesión venció. Inicia sesión nuevamente.',
    );
  }

  Future<void> refreshUser() async {
    try {
      final user = await _repository.me();
      state = state.copyWith(user: user, clearError: true);
    } catch (_) {
      // Mantener sesión actual si falla el refresh de perfil.
    }
  }

  void clearError() => state = state.copyWith(clearError: true);

  @override
  void dispose() {
    _expirationSubscription.cancel();
    super.dispose();
  }
}

final authControllerProvider = StateNotifierProvider<AuthController, AuthState>(
  (ref) {
    return AuthController(
      ref.watch(authRepositoryProvider),
      ref.watch(firebaseMessagingServiceProvider),
      ref.watch(sessionExpirationBusProvider).stream,
    );
  },
);
