import 'dart:io';

enum AppEnvironment { development, staging, production }

class AppConfig {
  const AppConfig({
    required this.environment,
    required this.apiBaseUrl,
    required this.googleMapsApiKey,
  });

  final AppEnvironment environment;
  final String apiBaseUrl;
  final String googleMapsApiKey;

  bool get hasGoogleMapsKey => googleMapsApiKey.trim().isNotEmpty;

  static const country = 'DO';
  static const locale = 'es-DO';
  static const timezone = 'America/Santo_Domingo';
  static const appName = 'BloodConnect RD';

  /// Default local API host when `API_BASE_URL` is not passed.
  /// - Android emulator: `http://10.0.2.2:8080`
  /// - iOS simulator / desktop: `http://localhost:8080`
  /// - Physical device: pass your Mac/PC LAN IP, e.g. `http://192.168.1.20:8080`
  static String get defaultLocalApiBaseUrl {
    if (Platform.isAndroid) {
      return 'http://10.0.2.2:8080';
    }
    return 'http://localhost:8080';
  }

  static AppConfig fromEnvironment() {
    const envName = String.fromEnvironment('ENV', defaultValue: 'development');
    const apiUrlDefine = String.fromEnvironment('API_BASE_URL');
    const googleMapsApiKey = String.fromEnvironment('GOOGLE_MAPS_API_KEY');

    final environment = switch (envName) {
      'staging' => AppEnvironment.staging,
      'production' => AppEnvironment.production,
      _ => AppEnvironment.development,
    };

    final apiUrl = apiUrlDefine.trim().isEmpty
        ? defaultLocalApiBaseUrl
        : apiUrlDefine.trim();

    return AppConfig(
      environment: environment,
      apiBaseUrl: apiUrl,
      googleMapsApiKey: googleMapsApiKey,
    );
  }
}
