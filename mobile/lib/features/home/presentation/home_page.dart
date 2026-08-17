import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_models.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../../auth/domain/auth_controller.dart';
import '../../blood_requests/data/blood_request_repository.dart';
import '../../donors/data/donor_repository.dart';

class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  bool _loadingNearby = false;
  String? _nearbyError;
  List<BloodRequestModel>? _nearby;

  Future<void> _loadNearby() async {
    setState(() {
      _loadingNearby = true;
      _nearbyError = null;
    });
    try {
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        throw const PermissionDeniedException(
          'El permiso de ubicación fue rechazado.',
        );
      }
      final position = await Geolocator.getCurrentPosition();
      final items = await ref
          .read(bloodRequestRepositoryProvider)
          .nearby(latitude: position.latitude, longitude: position.longitude);
      if (mounted) setState(() => _nearby = items);
    } on PermissionDeniedException {
      if (mounted) {
        setState(() {
          _nearbyError =
              'Activa la ubicación o busca por provincia y municipio.';
        });
      }
    } catch (error) {
      if (mounted) setState(() => _nearbyError = friendlyError(error));
    } finally {
      if (mounted) setState(() => _loadingNearby = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authControllerProvider).user;
    final profile = ref.watch(donorProfileProvider);
    final urgent = ref.watch(urgentRequestsProvider);
    final compatible = ref.watch(compatibleRequestsProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('BloodConnect RD'),
        actions: [
          IconButton(
            tooltip: 'Notificaciones',
            onPressed: () => context.push('/notificaciones'),
            icon: const Icon(Icons.notifications_outlined),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(donorProfileProvider);
          ref.invalidate(urgentRequestsProvider);
          ref.invalidate(compatibleRequestsProvider);
          await Future.wait([
            ref.read(urgentRequestsProvider.future),
            ref.read(compatibleRequestsProvider.future),
          ]);
        },
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
          children: [
            Text(
              'Hola, ${user?.firstName ?? ''}',
              style: Theme.of(
                context,
              ).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800),
            ),
            const Text('Tu ayuda puede marcar la diferencia hoy.'),
            const SizedBox(height: 16),
            profile.when(
              loading: () => const Card(
                child: Padding(
                  padding: EdgeInsets.all(20),
                  child: LinearProgressIndicator(),
                ),
              ),
              error: (error, _) => Card(
                child: ListTile(
                  leading: const Icon(Icons.error_outline),
                  title: const Text('No pudimos cargar tu perfil de donante'),
                  subtitle: Text(friendlyError(error)),
                  trailing: IconButton(
                    tooltip: 'Reintentar',
                    onPressed: () => ref.invalidate(donorProfileProvider),
                    icon: const Icon(Icons.refresh),
                  ),
                ),
              ),
              data: (donor) {
                if (donor == null) {
                  return Card(
                    child: ListTile(
                      leading: const Icon(Icons.person_add_alt_1),
                      title: const Text('Completa tu perfil de donante'),
                      subtitle: const Text(
                        'Agrega tu tipo de sangre y ubicación para ver compatibilidad.',
                      ),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.go('/perfil'),
                    ),
                  );
                }
                return Card(
                  color: Theme.of(context).colorScheme.primaryContainer,
                  child: Padding(
                    padding: const EdgeInsets.all(18),
                    child: Row(
                      children: [
                        BloodTypeBadge(donor.bloodType, large: true),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                availabilityLabel(donor.availability),
                                style: Theme.of(context).textTheme.titleMedium
                                    ?.copyWith(fontWeight: FontWeight.w700),
                              ),
                              Text(
                                donor.location.isEmpty
                                    ? 'Ubicación no indicada'
                                    : donor.location,
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 20),
            const SectionHeader(title: 'Acciones rápidas'),
            const SizedBox(height: 8),
            GridView.count(
              crossAxisCount: 2,
              childAspectRatio: 2.2,
              crossAxisSpacing: 8,
              mainAxisSpacing: 8,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              children: [
                _QuickAction(
                  icon: Icons.add_circle_outline,
                  label: 'Crear solicitud',
                  onTap: () => context.push('/solicitudes/crear'),
                ),
                _QuickAction(
                  icon: Icons.assignment_outlined,
                  label: 'Mis solicitudes',
                  onTap: () => context.push('/mis-solicitudes'),
                ),
                _QuickAction(
                  icon: Icons.local_hospital_outlined,
                  label: 'Ver centros',
                  onTap: () => context.go('/centros'),
                ),
                _QuickAction(
                  icon: Icons.history,
                  label: 'Mis donaciones',
                  onTap: () => context.push('/donaciones'),
                ),
              ],
            ),
            const SizedBox(height: 22),
            SectionHeader(
              title: 'Solicitudes urgentes',
              actionLabel: 'Ver todas',
              onAction: () => context.go('/solicitudes'),
            ),
            _RequestSection(
              value: urgent,
              emptyMessage: 'No hay solicitudes urgentes en este momento.',
            ),
            const SizedBox(height: 18),
            const SectionHeader(title: 'Compatibles contigo'),
            _RequestSection(
              value: compatible,
              emptyMessage: 'No hay solicitudes compatibles en este momento.',
              compactError:
                  'Completa tu perfil de donante para ver compatibilidad.',
            ),
            const SizedBox(height: 18),
            SectionHeader(
              title: 'Cerca de ti',
              actionLabel: _nearby == null ? 'Buscar' : 'Actualizar',
              onAction: _loadingNearby ? null : _loadNearby,
            ),
            if (_loadingNearby)
              const Padding(
                padding: EdgeInsets.all(20),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (_nearbyError != null)
              EmptyState(
                title: 'Ubicación no disponible',
                message: _nearbyError!,
                icon: Icons.location_off_outlined,
                action: OutlinedButton(
                  onPressed: () => context.go('/solicitudes'),
                  child: const Text('Buscar por ubicación'),
                ),
              )
            else if (_nearby == null)
              const EmptyState(
                title: 'Solicitudes cercanas',
                message:
                    'Usa tu ubicación cuando quieras ver solicitudes cercanas.',
                icon: Icons.near_me_outlined,
              )
            else if (_nearby!.isEmpty)
              const EmptyState(
                title: 'Todo tranquilo cerca de ti',
                message: 'No encontramos solicitudes en un radio de 25 km.',
                icon: Icons.location_searching,
              )
            else
              ..._nearby!
                  .take(3)
                  .map(
                    (item) => Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: RequestCard(
                        request: item,
                        onTap: () => context.push('/solicitudes/${item.id}'),
                      ),
                    ),
                  ),
          ],
        ),
      ),
    );
  }
}

class _RequestSection extends StatelessWidget {
  const _RequestSection({
    required this.value,
    required this.emptyMessage,
    this.compactError,
  });

  final AsyncValue<List<BloodRequestModel>> value;
  final String emptyMessage;
  final String? compactError;

  @override
  Widget build(BuildContext context) {
    return value.when(
      loading: () => const Padding(
        padding: EdgeInsets.all(20),
        child: Center(child: CircularProgressIndicator()),
      ),
      error: (_, _) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 12),
        child: Text(compactError ?? 'No pudimos cargar esta sección.'),
      ),
      data: (items) {
        if (items.isEmpty) {
          return Padding(
            padding: const EdgeInsets.symmetric(vertical: 12),
            child: Text(emptyMessage),
          );
        }
        return Column(
          children: items
              .take(3)
              .map(
                (item) => Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: RequestCard(
                    request: item,
                    onTap: () => context.push('/solicitudes/${item.id}'),
                  ),
                ),
              )
              .toList(),
        );
      },
    );
  }
}

class _QuickAction extends StatelessWidget {
  const _QuickAction({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Row(
            children: [
              Icon(icon, color: Theme.of(context).colorScheme.primary),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  label,
                  style: const TextStyle(fontWeight: FontWeight.w600),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
