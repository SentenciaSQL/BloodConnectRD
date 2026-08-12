import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../data/donation_repository.dart';

class DonationHistoryPage extends ConsumerWidget {
  const DonationHistoryPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final history = ref.watch(donationHistoryProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Historial de donaciones')),
      body: history.when(
        loading: () => const LoadingView(),
        error: (error, _) => ErrorView(
          message: friendlyError(error),
          onRetry: () => ref.invalidate(donationHistoryProvider),
        ),
        data: (summary) {
          if (summary == null) {
            return const EmptyState(
              title: 'Perfil de donante requerido',
              message:
                  'Completa tu perfil de donante para consultar el historial de donaciones.',
              icon: Icons.person_add_alt_1,
            );
          }
          if (summary.history.isEmpty) {
            return const EmptyState(
              title: 'Aún no hay donaciones',
              message:
                  'Cuando completes una donación aparecerá en este historial.',
              icon: Icons.history,
            );
          }
          return RefreshIndicator(
            onRefresh: () => ref.refresh(donationHistoryProvider.future),
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Card(
                  color: Theme.of(context).colorScheme.primaryContainer,
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        _Metric(
                          value: '${summary.totalDonations}',
                          label: 'Donaciones',
                        ),
                        _Metric(
                          value: '${summary.totalUnits}',
                          label: 'Unidades',
                        ),
                        _Metric(
                          value: formatDate(summary.lastDonation, short: true),
                          label: 'Última',
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 14),
                ...summary.history.map(
                  (donation) => DonationCard(donation: donation),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _Metric extends StatelessWidget {
  const _Metric({required this.value, required this.label});

  final String value;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          value,
          style: Theme.of(
            context,
          ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
        ),
        Text(label, style: Theme.of(context).textTheme.bodySmall),
      ],
    );
  }
}
