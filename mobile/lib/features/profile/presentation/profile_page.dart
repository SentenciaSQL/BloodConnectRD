import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../../auth/domain/auth_controller.dart';
import '../../donations/data/donation_repository.dart';
import '../../donors/data/donor_repository.dart';

class ProfilePage extends ConsumerStatefulWidget {
  const ProfilePage({super.key});

  @override
  ConsumerState<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends ConsumerState<ProfilePage> {
  bool _updatingAvailability = false;

  Future<void> _toggleAvailability(bool value) async {
    setState(() => _updatingAvailability = true);
    try {
      await ref.read(donorRepositoryProvider).updateAvailability(value);
      ref.invalidate(donorProfileProvider);
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(friendlyError(error))));
      }
    } finally {
      if (mounted) setState(() => _updatingAvailability = false);
    }
  }

  Future<void> _logout() async {
    await ref.read(authControllerProvider.notifier).logout();
    if (mounted) context.go('/login');
  }

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authControllerProvider);
    final donor = ref.watch(donorProfileProvider);
    final donations = ref.watch(donationHistoryProvider);
    final user = auth.user;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mi perfil'),
        actions: [
          IconButton(
            tooltip: 'Mensajes',
            onPressed: () => context.push('/mensajes'),
            icon: const Icon(Icons.chat_outlined),
          ),
          IconButton(
            tooltip: 'Notificaciones',
            onPressed: () => context.push('/notificaciones'),
            icon: const Icon(Icons.notifications_outlined),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 32,
                    child: Text(
                      (user?.firstName.isNotEmpty ?? false)
                          ? user!.firstName[0].toUpperCase()
                          : 'B',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          user?.fullName ?? '',
                          style: Theme.of(context).textTheme.titleLarge
                              ?.copyWith(fontWeight: FontWeight.w800),
                        ),
                        Text(user?.email ?? ''),
                        Text(user?.phone ?? ''),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          const SectionHeader(title: 'Perfil de donante'),
          const SizedBox(height: 8),
          donor.when(
            loading: () => const Card(
              child: Padding(
                padding: EdgeInsets.all(20),
                child: LinearProgressIndicator(),
              ),
            ),
            error: (error, _) => ErrorView(
              message: friendlyError(error),
              onRetry: () => ref.invalidate(donorProfileProvider),
            ),
            data: (profile) {
              if (profile == null) {
                return const EmptyState(
                  title: 'Perfil de donante pendiente',
                  message:
                      'Todavía no tienes información de donante registrada. Complétala desde la web o crea tu perfil aquí más adelante.',
                  icon: Icons.person_add_alt_1,
                );
              }
              return Card(
                child: Column(
                  children: [
                    ListTile(
                      leading: BloodTypeBadge(profile.bloodType),
                      title: const Text('Tipo de sangre'),
                      subtitle: Text(profile.bloodType),
                    ),
                    ListTile(
                      leading: const Icon(Icons.location_on_outlined),
                      title: const Text('Ubicación'),
                      subtitle: Text(
                        profile.location.isEmpty
                            ? 'No indicada'
                            : profile.location,
                      ),
                    ),
                    SwitchListTile(
                      secondary: const Icon(Icons.volunteer_activism_outlined),
                      title: const Text('Disponible para donar'),
                      subtitle: Text(availabilityLabel(profile.availability)),
                      value: profile.isAvailable,
                      onChanged: _updatingAvailability
                          ? null
                          : _toggleAvailability,
                    ),
                  ],
                ),
              );
            },
          ),
          const SizedBox(height: 16),
          const SectionHeader(title: 'Resumen de donaciones'),
          const SizedBox(height: 8),
          donations.when(
            loading: () => const LinearProgressIndicator(),
            error: (error, _) => ErrorView(
              message: friendlyError(error),
              onRetry: () => ref.invalidate(donationHistoryProvider),
            ),
            data: (summary) {
              if (summary == null) {
                return const Text(
                  'El historial de donaciones estará disponible cuando completes tu perfil de donante.',
                );
              }
              return Card(
                child: Column(
                  children: [
                    ListTile(
                      leading: const Icon(Icons.favorite_outline),
                      title: Text('${summary.totalDonations} donaciones'),
                      subtitle: Text('${summary.totalUnits} unidades donadas'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.push('/donaciones'),
                    ),
                    if (summary.lastDonation != null)
                      ListTile(
                        leading: const Icon(Icons.event_outlined),
                        title: const Text('Última donación'),
                        subtitle: Text(formatDate(summary.lastDonation)),
                      ),
                  ],
                ),
              );
            },
          ),
          const SizedBox(height: 12),
          ListTile(
            leading: const Icon(Icons.chat_outlined),
            title: const Text('Mensajes'),
            subtitle: const Text('Conversaciones privadas sobre solicitudes'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => context.push('/mensajes'),
          ),
          ListTile(
            leading: const Icon(Icons.bloodtype_outlined),
            title: const Text('Mis solicitudes'),
            subtitle: const Text('Consulta el progreso de unidades recibidas'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => context.push('/mis-solicitudes'),
          ),
          ListTile(
            leading: const Icon(Icons.volunteer_activism_outlined),
            title: const Text('Mis donaciones'),
            subtitle: const Text('Reportes y estado de confirmación'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => context.push('/donaciones'),
          ),
          ListTile(
            leading: const Icon(Icons.notifications_outlined),
            title: const Text('Notificaciones'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => context.push('/notificaciones'),
          ),
          ListTile(
            leading: Icon(
              Icons.logout,
              color: Theme.of(context).colorScheme.error,
            ),
            title: Text(
              'Cerrar sesión',
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
            onTap: auth.isSubmitting ? null : _logout,
          ),
        ],
      ),
    );
  }
}
