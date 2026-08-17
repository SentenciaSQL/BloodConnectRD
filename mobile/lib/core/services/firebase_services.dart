import 'dart:async';
import 'dart:io';

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../constants/api_paths.dart';
import '../networking/api_client.dart';

class OptionalFirebase {
  OptionalFirebase._();

  static bool isAvailable = false;

  static Future<void> initialize() async {
    try {
      // Sin GoogleService-Info.plist / google-services.json Firebase no está listo.
      // En iOS una inicialización nativa mal configurada puede tumbar el proceso;
      // capturamos el error y seguimos sin push/Crashlytics.
      await Firebase.initializeApp();
      isAvailable = true;
      await FirebaseCrashlytics.instance.setCrashlyticsCollectionEnabled(
        !kDebugMode,
      );
      if (!kDebugMode) {
        FlutterError.onError =
            FirebaseCrashlytics.instance.recordFlutterFatalError;
        PlatformDispatcher.instance.onError = (error, stack) {
          FirebaseCrashlytics.instance.recordError(error, stack, fatal: true);
          return true;
        };
      }
    } catch (error, stack) {
      isAvailable = false;
      if (kDebugMode) {
        debugPrint(
          'Firebase no está configurado; la app continuará sin notificaciones. ($error)',
        );
        debugPrintStack(stackTrace: stack);
      }
    }
  }
}

class FirebaseMessagingService {
  FirebaseMessagingService(this._api);

  final ApiClient _api;
  StreamSubscription<String>? _tokenSubscription;
  StreamSubscription<RemoteMessage>? _tapSubscription;
  StreamSubscription<RemoteMessage>? _foregroundSubscription;
  String? _registeredToken;
  void Function(String location)? onNotificationRoute;
  void Function()? onUnreadRefresh;

  Future<void> start({
    void Function(String location)? onNotificationRoute,
    void Function()? onUnreadRefresh,
  }) async {
    this.onNotificationRoute = onNotificationRoute ?? this.onNotificationRoute;
    this.onUnreadRefresh = onUnreadRefresh ?? this.onUnreadRefresh;
    if (!OptionalFirebase.isAvailable) return;
    try {
      final messaging = FirebaseMessaging.instance;
      await messaging.requestPermission(provisional: true);
      final token = await messaging.getToken();
      if (token != null) await _register(token);
      _tokenSubscription ??= messaging.onTokenRefresh.listen(_registerSafely);
      _tapSubscription ??= FirebaseMessaging.onMessageOpenedApp.listen(
        (message) => _routeMessage(message, this.onNotificationRoute),
      );
      _foregroundSubscription ??= FirebaseMessaging.onMessage.listen((_) {
        this.onUnreadRefresh?.call();
      });
      final initialMessage = await messaging.getInitialMessage();
      if (initialMessage != null) {
        _routeMessage(initialMessage, this.onNotificationRoute);
      }
    } catch (_) {
      if (kDebugMode) {
        debugPrint('No se pudo activar la mensajería en este dispositivo.');
      }
    }
  }

  Future<void> unregister() async {
    final token = _registeredToken;
    if (token == null || token.isEmpty) return;
    try {
      await _api.delete('${ApiPaths.devices}/${Uri.encodeComponent(token)}');
    } catch (_) {
      // El cierre de sesión no debe quedar bloqueado por FCM.
    } finally {
      _registeredToken = null;
    }
  }

  Future<void> _register(String token) async {
    final platform = Platform.isIOS ? 'IOS' : 'ANDROID';
    await _api.post(
      ApiPaths.devices,
      data: {'token': token, 'platform': platform},
    );
    _registeredToken = token;
  }

  void _registerSafely(String token) {
    _register(token).catchError((_) {
      if (kDebugMode) {
        debugPrint('No se pudo actualizar el token de notificaciones.');
      }
    });
  }

  void _routeMessage(
    RemoteMessage message,
    void Function(String location)? onNotificationRoute,
  ) {
    final resourceType = message.data['resourceType']?.toString();
    final resourceId = message.data['resourceId']?.toString();
    if (resourceType == 'CONVERSATION' && resourceId != null) {
      onNotificationRoute?.call('/mensajes/$resourceId');
      return;
    }
    if (resourceType == 'BLOOD_REQUEST' && resourceId != null) {
      onNotificationRoute?.call('/solicitudes/$resourceId');
    }
  }

  void dispose() {
    _tokenSubscription?.cancel();
    _tapSubscription?.cancel();
    _foregroundSubscription?.cancel();
  }
}

final firebaseMessagingServiceProvider = Provider<FirebaseMessagingService>((
  ref,
) {
  final service = FirebaseMessagingService(ref.watch(apiClientProvider));
  ref.onDispose(service.dispose);
  return service;
});
