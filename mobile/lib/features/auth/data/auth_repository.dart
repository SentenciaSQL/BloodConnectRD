import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/api_paths.dart';
import '../../../core/networking/api_client.dart';
import '../../../core/networking/api_models.dart';
import '../../../core/storage/secure_token_storage.dart';

class AuthRepository {
  const AuthRepository(this._api, this._storage);

  final ApiClient _api;
  final SecureTokenStorage _storage;

  Future<AuthTokens> login({
    required String email,
    required String password,
  }) async {
    final response = await _api.post(
      '${ApiPaths.auth}/login',
      data: {'email': email.trim(), 'password': password},
    );
    return _persistAuth(response);
  }

  Future<String> register({
    required String firstName,
    required String lastName,
    required String email,
    required String phone,
    required String password,
    required String confirmPassword,
  }) async {
    final response = await _api.post(
      '${ApiPaths.auth}/register',
      data: {
        'firstName': firstName.trim(),
        'lastName': lastName.trim(),
        'email': email.trim(),
        'phone': phone.trim(),
        'password': password,
        'confirmPassword': confirmPassword,
      },
    );

    return asJson(response)['message']?.toString() ??
        'Cuenta creada correctamente. Revisa tu correo electrónico.';
  }

  Future<String> resendVerification(String email) async {
    final response = await _api.post(
      '${ApiPaths.auth}/resend-verification',
      data: {'email': email.trim()},
    );

    return asJson(response)['message']?.toString() ??
        'Si tu cuenta está pendiente, recibirás un nuevo correo.';
  }

  Future<AppUser> restoreSession() async {
    final token = await _storage.readAccessToken();
    if (token == null || token.isEmpty) {
      throw const NoStoredSession();
    }
    return me();
  }

  Future<AppUser> me() async {
    final response = await _api.get('${ApiPaths.auth}/me');
    return AppUser.fromJson(asJson(response));
  }

  Future<void> logout() async {
    final refreshToken = await _storage.readRefreshToken();
    try {
      await _api.post(
        '${ApiPaths.auth}/logout',
        data: refreshToken == null ? null : {'refreshToken': refreshToken},
      );
    } finally {
      await _storage.clear();
    }
  }

  Future<void> forgotPassword({required String email}) async {
    await _api.post(
      '${ApiPaths.auth}/forgot-password',
      data: {'email': email.trim()},
    );
  }

  Future<void> resetPassword({
    required String token,
    required String password,
    required String confirmPassword,
  }) async {
    await _api.post(
      '${ApiPaths.auth}/reset-password',
      data: {
        'token': token.trim(),
        'password': password,
        'confirmPassword': confirmPassword,
      },
    );
  }

  Future<AuthTokens> _persistAuth(Object? response) async {
    final auth = AuthTokens.fromJson(asJson(response));
    await _storage.saveTokens(
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
    );
    return auth;
  }

  Future<void> clearSession() => _storage.clear();
}

class NoStoredSession implements Exception {
  const NoStoredSession();
}

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(
    ref.watch(apiClientProvider),
    ref.watch(secureTokenStorageProvider),
  );
});
