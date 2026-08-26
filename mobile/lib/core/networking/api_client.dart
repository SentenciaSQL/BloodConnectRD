import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../config/app_config.dart';
import '../constants/api_paths.dart';
import '../errors/app_exception.dart';
import '../storage/secure_token_storage.dart';

class ApiClient {
  ApiClient({
    required String baseUrl,
    required SecureTokenStorage tokenStorage,
    required void Function() onSessionExpired,
  }) : _tokenStorage = tokenStorage,
       _onSessionExpired = onSessionExpired {
    _dio = Dio(
      BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: const Duration(seconds: 15),
        receiveTimeout: const Duration(seconds: 20),
        headers: const {'Accept': 'application/json'},
        listFormat: ListFormat.multi,
      ),
    );
    _dio.interceptors.add(
      AuthInterceptor(
        dio: _dio,
        baseUrl: baseUrl,
        tokenStorage: _tokenStorage,
        onSessionExpired: _expireSession,
      ),
    );
  }

  final SecureTokenStorage _tokenStorage;
  final void Function() _onSessionExpired;
  late final Dio _dio;

  Future<dynamic> get(String path, {Map<String, dynamic>? queryParameters}) =>
      _request(() => _dio.get<dynamic>(path, queryParameters: queryParameters));

  Future<dynamic> post(String path, {Object? data}) =>
      _request(() => _dio.post<dynamic>(path, data: data));

  Future<dynamic> put(String path, {Object? data}) =>
      _request(() => _dio.put<dynamic>(path, data: data));

  Future<dynamic> patch(String path, {Object? data}) =>
      _request(() => _dio.patch<dynamic>(path, data: data));

  Future<dynamic> delete(String path, {Object? data}) =>
      _request(() => _dio.delete<dynamic>(path, data: data));

  Future<dynamic> _request(Future<Response<dynamic>> Function() request) async {
    try {
      return (await request()).data;
    } on DioException catch (error) {
      if (error.error is AppException) throw error.error! as AppException;
      throw mapDioException(error);
    }
  }

  void _expireSession() {
    _onSessionExpired();
  }
}

class AuthInterceptor extends QueuedInterceptor {
  AuthInterceptor({
    required Dio dio,
    required String baseUrl,
    required SecureTokenStorage tokenStorage,
    required void Function() onSessionExpired,
  }) : _dio = dio,
       _refreshDio = Dio(BaseOptions(baseUrl: baseUrl)),
       _tokenStorage = tokenStorage,
       _onSessionExpired = onSessionExpired;

  final Dio _dio;
  final Dio _refreshDio;
  final SecureTokenStorage _tokenStorage;
  final void Function() _onSessionExpired;
  Future<String>? _refreshing;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final token = await _tokenStorage.readAccessToken();
    if (token != null && token.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    final options = err.requestOptions;
    final isUnauthorized = err.response?.statusCode == 401;
    final alreadyRetried = options.extra['authRetried'] == true;
    final isAuthEntryPoint =
        options.path == '${ApiPaths.auth}/login' ||
        options.path == '${ApiPaths.auth}/register' ||
        options.path == '${ApiPaths.auth}/refresh' ||
        options.path == '${ApiPaths.auth}/forgot-password' ||
        options.path == '${ApiPaths.auth}/reset-password';

    if (!isUnauthorized || alreadyRetried || isAuthEntryPoint) {
      handler.next(err);
      return;
    }

    try {
      final token = await _refreshAccessToken();
      final response = await _dio.fetch<dynamic>(
        options.copyWith(
          headers: {...options.headers, 'Authorization': 'Bearer $token'},
          extra: {...options.extra, 'authRetried': true},
        ),
      );
      handler.resolve(response);
    } catch (_) {
      await _tokenStorage.clear();
      _onSessionExpired();
      handler.reject(
        DioException(
          requestOptions: options,
          response: err.response,
          error: const UnauthorizedException(),
          type: DioExceptionType.badResponse,
        ),
      );
    }
  }

  Future<String> _refreshAccessToken() {
    return _refreshing ??= _performRefresh().whenComplete(() {
      _refreshing = null;
    });
  }

  Future<String> _performRefresh() async {
    final refreshToken = await _tokenStorage.readRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) {
      throw const UnauthorizedException();
    }

    final response = await _refreshDio.post<dynamic>(
      '${ApiPaths.auth}/refresh',
      data: {'refreshToken': refreshToken},
    );
    final data = Map<String, dynamic>.from(response.data as Map);
    final access = data['accessToken']?.toString();
    final nextRefresh = data['refreshToken']?.toString();
    if (access == null || nextRefresh == null) {
      throw const UnauthorizedException();
    }
    await _tokenStorage.saveTokens(
      accessToken: access,
      refreshToken: nextRefresh,
    );
    return access;
  }
}

class SessionExpirationBus {
  final _controller = StreamController<void>.broadcast();

  Stream<void> get stream => _controller.stream;

  void notify() => _controller.add(null);

  void dispose() => _controller.close();
}

final appConfigProvider = Provider<AppConfig>(
  (ref) => AppConfig.fromEnvironment(),
);

final secureTokenStorageProvider = Provider<SecureTokenStorage>(
  (ref) => SecureTokenStorage(),
);

final sessionExpirationBusProvider = Provider<SessionExpirationBus>((ref) {
  final bus = SessionExpirationBus();
  ref.onDispose(bus.dispose);
  return bus;
});

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(
    baseUrl: ref.watch(appConfigProvider).apiBaseUrl,
    tokenStorage: ref.watch(secureTokenStorageProvider),
    onSessionExpired: ref.read(sessionExpirationBusProvider).notify,
  );
});
