import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'core/routing/app_router.dart';
import 'core/services/firebase_services.dart';
import 'core/theme/app_theme.dart';
import 'features/messages/data/conversation_repository.dart';
import 'features/notifications/data/notification_repository.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('es_DO');
  await OptionalFirebase.initialize();
  if (OptionalFirebase.isAvailable) {
    FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
  }
  runApp(const ProviderScope(child: BloodConnectApp()));
}

class BloodConnectApp extends ConsumerWidget {
  const BloodConnectApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(appRouterProvider);
    final messaging = ref.read(firebaseMessagingServiceProvider);
    messaging.onNotificationRoute = (location) => router.go(location);
    messaging.onMessageReceived = (message) {
      ref.invalidate(notificationsProvider);
      ref.invalidate(unreadMessageCountProvider);
      ref.invalidate(conversationsProvider);

      final resourceType = message.data['resourceType']?.toString();

      final resourceId = int.tryParse(
        message.data['resourceId']?.toString() ?? '',
      );

      if (resourceType == 'CONVERSATION' && resourceId != null) {
        ref.invalidate(conversationProvider(resourceId));

        ref.invalidate(conversationMessagesProvider(resourceId));
      }
    };
    return MaterialApp.router(
      title: 'BloodConnect RD',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: ThemeMode.system,
      routerConfig: router,
      locale: const Locale('es', 'DO'),
      supportedLocales: const [Locale('es', 'DO')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
    );
  }
}
