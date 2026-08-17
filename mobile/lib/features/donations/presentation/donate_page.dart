import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../../donors/data/donor_repository.dart';
import '../data/donation_repository.dart';

class DonatePage extends ConsumerStatefulWidget {
  const DonatePage({super.key});

  @override
  ConsumerState<DonatePage> createState() => _DonatePageState();
}

class _DonatePageState extends ConsumerState<DonatePage> {
  bool _updating = false;

  Future<void> _toggleAvailability(bool value) async {
    setState(() => _updating = true);
    try {
      await ref.read(donorRepositoryProvider).updateAvailability(value);
      ref.invalidate(donorProfileProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              value
                  ? 'Ahora apareces como disponible para donar.'
                  : 'Tu disponibilidad fue actualizada.',
            ),
          ),
        );
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(friendlyError(error))));
      }
    } finally {
      if (mounted) setState(() => _updating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final profile = ref.watch(donorProfileProvider);
    final history = ref.watch(donationHistoryProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Donar sangre')),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(donorProfileProvider);
          ref.invalidate(donationHistoryProvider);
          await ref.read(donationHistoryProvider.future);
        },
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
          children: [
            profile.when(
              loading: () => const Card(
                child: Padding(
                  padding: EdgeInsets.all(24),
                  child: LinearProgressIndicator(),
                ),
              ),
              error: (error, _) => ErrorView(
                message: friendlyError(error),
                onRetry: () => ref.invalidate(donorProfileProvider),
              ),
              data: (donor) {
                if (donor == null) {
                  return const EmptyState(
                    title: 'Perfil de donante pendiente',
                    message:
                        'Completa tu perfil de donante para indicar disponibilidad.',
                    icon: Icons.person_add_alt_1,
                  );
                }
                return Card(
                  child: SwitchListTile(
                    value: donor.isAvailable,
                    onChanged: _updating ? null : _toggleAvailability,
                    secondary: BloodTypeBadge(donor.bloodType),
                    title: Text(
                      donor.isAvailable
                          ? 'Disponible para donar'
                          : 'No disponible temporalmente',
                    ),
                    subtitle: Text(
                      _updating
                          ? 'Actualizando…'
                          : availabilityLabel(donor.availability),
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 18),
            const SectionHeader(title: 'Tu impacto'),
            const SizedBox(height: 8),
            history.when(
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => ErrorView(
                message: friendlyError(error),
                onRetry: () => ref.invalidate(donationHistoryProvider),
              ),
              data: (summary) {
                if (summary == null) {
                  return const Text(
                    'El resumen de donaciones aparecerá cuando tengas un perfil de donante.',
                  );
                }
                return Column(
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: _SummaryCard(
                            value: '${summary.totalDonations}',
                            label: 'Donaciones',
                            icon: Icons.favorite_outline,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: _SummaryCard(
                            value: '${summary.totalUnits}',
                            label: 'Unidades',
                            icon: Icons.water_drop_outlined,
                          ),
                        ),
                      ],
                    ),
                    if (summary.estimatedNextDate != null)
                      ListTile(
                        contentPadding: EdgeInsets.zero,
                        leading: const Icon(Icons.event_available_outlined),
                        title: const Text('Próxima fecha estimada'),
                        subtitle: Text(formatDate(summary.estimatedNextDate)),
                      ),
                    if (summary.orientationNote.isNotEmpty)
                      Card(
                        color: Theme.of(context).colorScheme.secondaryContainer,
                        child: Padding(
                          padding: const EdgeInsets.all(14),
                          child: Text(summary.orientationNote),
                        ),
                      ),
                  ],
                );
              },
            ),
            const SizedBox(height: 22),
            PrimaryButton(
              label: 'Ver solicitudes compatibles',
              icon: Icons.bloodtype_outlined,
              onPressed: () => context.go('/solicitudes'),
            ),
            const SizedBox(height: 8),
            OutlinedButton.icon(
              onPressed: () => context.push('/donaciones'),
              icon: const Icon(Icons.history),
              label: const Text('Mis donaciones'),
            ),
          ],
        ),
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  const _SummaryCard({
    required this.value,
    required this.label,
    required this.icon,
  });

  final String value;
  final String label;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Icon(icon, color: Theme.of(context).colorScheme.primary),
            const SizedBox(height: 8),
            Text(
              value,
              style: Theme.of(
                context,
              ).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w800),
            ),
            Text(label),
          ],
        ),
      ),
    );
  }
}
