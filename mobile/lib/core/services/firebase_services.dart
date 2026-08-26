import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../constants/api_paths.dart';
import '../networking/api_client.dart';

@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
}

class OptionalFirebase {
  OptionalFirebase._();

  static bool isAvailable = false;

  static Future<void> initialize() async {
    try {
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
          'Firebase no está configurado; '
          'la app continuará sin notificaciones. ($error)',
        );
        debugPrintStack(stackTrace: stack);
      }
    }
  }
}

class FirebaseMessagingService {
  FirebaseMessagingService(this._api);

  static const _channel = AndroidNotificationChannel(
    'bloodconnect_alerts',
    'Alertas de BloodConnect',
    description: 'Mensajes y novedades de solicitudes de sangre.',
    importance: Importance.high,
  );

  final ApiClient _api;

  final FlutterLocalNotificationsPlugin _localNotifications =
      FlutterLocalNotificationsPlugin();

  StreamSubscription<String>? _tokenSubscription;
  StreamSubscription<RemoteMessage>? _tapSubscription;
  StreamSubscription<RemoteMessage>? _foregroundSubscription;

  String? _registeredToken;

  void Function(String location)? onNotificationRoute;

  void Function(RemoteMessage message)? onMessageReceived;

  Future<void> start({
    void Function(String location)? onNotificationRoute,
    void Function(RemoteMessage message)? onMessageReceived,
  }) async {
    this.onNotificationRoute = onNotificationRoute ?? this.onNotificationRoute;

    this.onMessageReceived = onMessageReceived ?? this.onMessageReceived;

    if (!OptionalFirebase.isAvailable) {
      return;
    }

    try {
      final messaging = FirebaseMessaging.instance;

      await _initializeLocalNotifications();

      final permission = await messaging.requestPermission(
        alert: true,
        badge: true,
        sound: true,
        provisional: false,
      );

      if (kDebugMode) {
        debugPrint('PERMISO FCM: ${permission.authorizationStatus}');
      }

      await messaging.setForegroundNotificationPresentationOptions(
        alert: true,
        badge: true,
        sound: true,
      );

      final token = await messaging.getToken();

      if (kDebugMode) {
        debugPrint('FCM TOKEN: $token');
      }

      if (token != null && token.isNotEmpty) {
        await _register(token);
      }

      _tokenSubscription ??= messaging.onTokenRefresh.listen(_registerSafely);

      _tapSubscription ??= FirebaseMessaging.onMessageOpenedApp.listen((
        message,
      ) {
        _routeMessage(message, this.onNotificationRoute);
      });

      _foregroundSubscription ??= FirebaseMessaging.onMessage.listen((message) {
        if (kDebugMode) {
          debugPrint(
            'NOTIFICACIÓN FCM RECIBIDA: '
            '${message.messageId}',
          );
          debugPrint('DATOS FCM: ${message.data}');
        }

        this.onMessageReceived?.call(message);

        unawaited(_showForegroundNotification(message));
      });

      final initialMessage = await messaging.getInitialMessage();

      if (initialMessage != null) {
        _routeMessage(initialMessage, this.onNotificationRoute);
      }
    } catch (error, stack) {
      if (kDebugMode) {
        debugPrint('No se pudo activar la mensajería: $error');
        debugPrintStack(stackTrace: stack);
      }
    }
  }

  Future<void> unregister() async {
    final token = _registeredToken;

    if (token == null || token.isEmpty) {
      return;
    }

    try {
      await _api.delete(
        '${ApiPaths.devices}/'
        '${Uri.encodeComponent(token)}',
      );
    } catch (_) {
      // Un error de FCM no debe impedir cerrar sesión.
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

    if (kDebugMode) {
      debugPrint('Token FCM registrado correctamente.');
    }
  }

  void _registerSafely(String token) {
    if (kDebugMode) {
      debugPrint('NUEVO FCM TOKEN: $token');
    }

    _register(token).catchError((error) {
      if (kDebugMode) {
        debugPrint(
          'No se pudo actualizar el token '
          'de notificaciones: $error',
        );
      }
    });
  }

  void _routeMessage(
    RemoteMessage message,
    void Function(String location)? notificationRoute,
  ) {
    _routeData(message.data, notificationRoute);
  }

  Future<void> _initializeLocalNotifications() async {
    if (!Platform.isAndroid) {
      return;
    }

    const settings = InitializationSettings(
      android: AndroidInitializationSettings('@mipmap/ic_launcher'),
    );

    await _localNotifications.initialize(
      settings: settings,
      onDidReceiveNotificationResponse: (response) {
        final payload = response.payload;

        if (payload == null || payload.isEmpty) {
          onNotificationRoute?.call('/notificaciones');
          return;
        }

        try {
          final decoded = jsonDecode(payload);

          final data = Map<String, dynamic>.from(decoded as Map);

          _routeData(data, onNotificationRoute);
        } catch (_) {
          onNotificationRoute?.call('/notificaciones');
        }
      },
    );

    await _localNotifications
        .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin
        >()
        ?.createNotificationChannel(_channel);
  }

  Future<void> _showForegroundNotification(RemoteMessage message) async {
    if (!Platform.isAndroid) {
      return;
    }

    final notification = message.notification;

    final title = notification?.title ?? message.data['title']?.toString();

    final body = notification?.body ?? message.data['body']?.toString();

    if (title == null && body == null) {
      return;
    }

    final notificationId =
        message.messageId?.hashCode ??
        ((DateTime.now().millisecondsSinceEpoch ~/ 1000) & 0x7fffffff);

    await _localNotifications.show(
      id: notificationId,
      title: title ?? 'BloodConnect RD',
      body: body ?? '',
      notificationDetails: const NotificationDetails(
        android: AndroidNotificationDetails(
          'bloodconnect_alerts',
          'Alertas de BloodConnect',
          channelDescription:
              'Mensajes y novedades de '
              'solicitudes de sangre.',
          importance: Importance.high,
          priority: Priority.high,
        ),
      ),
      payload: jsonEncode(message.data),
    );
  }

  void _routeData(
    Map<String, dynamic> data,
    void Function(String location)? notificationRoute,
  ) {
    final resourceType = data['resourceType']?.toString();

    final resourceId = data['resourceId']?.toString();

    if (resourceType == 'CONVERSATION' &&
        resourceId != null &&
        resourceId.isNotEmpty) {
      notificationRoute?.call('/mensajes/$resourceId');
      return;
    }

    if (resourceType == 'BLOOD_REQUEST' &&
        resourceId != null &&
        resourceId.isNotEmpty) {
      notificationRoute?.call('/detalle-solicitud/$resourceId');
      return;
    }

    notificationRoute?.call('/notificaciones');
  }

  void dispose() {
    _tokenSubscription?.cancel();
    _tapSubscription?.cancel();
    _foregroundSubscription?.cancel();

    _tokenSubscription = null;
    _tapSubscription = null;
    _foregroundSubscription = null;
  }
}

final firebaseMessagingServiceProvider = Provider<FirebaseMessagingService>((
  ref,
) {
  final service = FirebaseMessagingService(ref.watch(apiClientProvider));

  ref.onDispose(service.dispose);

  return service;
});
