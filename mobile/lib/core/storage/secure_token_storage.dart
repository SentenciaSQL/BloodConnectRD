import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SecureTokenStorage {
  SecureTokenStorage({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  static const _accessTokenKey = 'bloodconnect_access_token';
  static const _refreshTokenKey = 'bloodconnect_refresh_token';
  static const _fingerprintEnabledKey = 'bloodconnect_fingerprint_enabled';

  final FlutterSecureStorage _storage;

  Future<String?> readAccessToken() => _storage.read(key: _accessTokenKey);

  Future<String?> readRefreshToken() => _storage.read(key: _refreshTokenKey);

  Future<bool> isFingerprintEnabled() async {
    return await _storage.read(key: _fingerprintEnabledKey) == 'true';
  }

  Future<void> setFingerprintEnabled(bool enabled) {
    return _storage.write(
      key: _fingerprintEnabledKey,
      value: enabled.toString(),
    );
  }

  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    await Future.wait([
      _storage.write(key: _accessTokenKey, value: accessToken),
      _storage.write(key: _refreshTokenKey, value: refreshToken),
    ]);
  }

  Future<void> clear() async {
    await Future.wait([
      _storage.delete(key: _accessTokenKey),
      _storage.delete(key: _refreshTokenKey),
      _storage.delete(key: _fingerprintEnabledKey),
    ]);
  }
}
