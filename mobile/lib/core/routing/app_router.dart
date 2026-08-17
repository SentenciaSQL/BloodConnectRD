import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/domain/auth_controller.dart';
import '../../features/auth/presentation/login_page.dart';
import '../../features/auth/presentation/register_page.dart';
import '../../features/blood_requests/presentation/blood_request_detail_page.dart';
import '../../features/blood_requests/presentation/blood_requests_page.dart';
import '../../features/blood_requests/presentation/create_blood_request_page.dart';
import '../../features/blood_requests/presentation/my_requests_page.dart';
import '../../features/donation_centers/presentation/donation_centers_page.dart';
import '../../features/donations/presentation/donate_page.dart';
import '../../features/donations/presentation/donation_history_page.dart';
import '../../features/donors/presentation/donor_profile_form_page.dart';
import '../../features/home/presentation/home_page.dart';
import '../../features/home/presentation/home_shell_page.dart';
import '../../features/notifications/presentation/notifications_page.dart';
import '../../features/profile/presentation/profile_page.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final refresh = _RouterRefresh(ref);
  final router = GoRouter(
    initialLocation: '/cargando',
    refreshListenable: refresh,
    redirect: (context, state) {
      final auth = ref.read(authControllerProvider);
      final location = state.matchedLocation;
      final onAuthPage = location == '/login' || location == '/registro';
      if (auth.isInitializing) {
        return location == '/cargando' ? null : '/cargando';
      }
      if (!auth.isAuthenticated) {
        return onAuthPage ? null : '/login';
      }
      if (onAuthPage || location == '/cargando') return '/';
      return null;
    },
    routes: [
      GoRoute(
        path: '/cargando',
        builder: (context, state) => const _SplashPage(),
      ),
      GoRoute(path: '/login', builder: (context, state) => const LoginPage()),
      GoRoute(
        path: '/registro',
        builder: (context, state) => const RegisterPage(),
      ),
      GoRoute(
        path: '/notificaciones',
        builder: (context, state) => const NotificationsPage(),
      ),
      GoRoute(
        path: '/donaciones',
        builder: (context, state) => const DonationHistoryPage(),
      ),
      GoRoute(
        path: '/mis-solicitudes',
        builder: (context, state) => const MyRequestsPage(),
      ),
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) {
          return HomeShellPage(navigationShell: navigationShell);
        },
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(path: '/', builder: (context, state) => const HomePage()),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/solicitudes',
                builder: (context, state) => const BloodRequestsPage(),
                routes: [
                  GoRoute(
                    path: 'crear',
                    builder: (context, state) => const CreateBloodRequestPage(),
                  ),
                  GoRoute(
                    path: ':id',
                    builder: (context, state) => BloodRequestDetailPage(
                      requestId: int.parse(state.pathParameters['id']!),
                    ),
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/donar',
                builder: (context, state) => const DonatePage(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/centros',
                builder: (context, state) => const DonationCentersPage(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/perfil',
                builder: (context, state) => const ProfilePage(),
                routes: [
                  GoRoute(
                    path: 'donante',
                    builder: (context, state) => const DonorProfileFormGate(),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    ],
  );
  ref.onDispose(() {
    refresh.dispose();
    router.dispose();
  });
  return router;
});

class _RouterRefresh extends ChangeNotifier {
  _RouterRefresh(Ref ref) {
    ref.listen(authControllerProvider, (_, _) => notifyListeners());
  }
}

class _SplashPage extends StatelessWidget {
  const _SplashPage();

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      backgroundColor: const Color(0xFFFFF8F6),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Image.asset(
              'assets/branding/splash_logo.png',
              width: 148,
              height: 148,
              filterQuality: FilterQuality.high,
            ),
            const SizedBox(height: 20),
            Text(
              'BloodConnect RD',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.w800,
                    color: const Color(0xFF1D2528),
                  ),
            ),
            const SizedBox(height: 6),
            Text(
              'República Dominicana',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: const Color(0xFF547478),
                  ),
            ),
            const SizedBox(height: 28),
            SizedBox(
              width: 28,
              height: 28,
              child: CircularProgressIndicator(
                strokeWidth: 3,
                color: colors.primary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
