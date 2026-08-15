import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_models.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../../auth/domain/auth_controller.dart';
import '../data/donation_repository.dart';

class DonationHistoryPage extends ConsumerStatefulWidget {
  const DonationHistoryPage({super.key});

  @override
  ConsumerState<DonationHistoryPage> createState() =>
      _DonationHistoryPageState();
}

class _DonationHistoryPageState extends ConsumerState<DonationHistoryPage> {
  int? _busyId;

  Future<void> _refresh() async {
    ref.invalidate(donationHistoryProvider);
    ref.invalidate(myDonationResponsesProvider);
    await Future.wait([
      ref.read(donationHistoryProvider.future),
      ref.read(myDonationResponsesProvider.future),
    ]);
  }

  Future<void> _complete(DonationResponseModel response) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Registrar donación'),
        content: const Text(
          '¿Confirmas que esta donación ya se realizó en un centro autorizado?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Confirmar'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    setState(() => _busyId = response.id);
    try {
      await ref.read(donationRepositoryProvider).complete(response.id);
      ref.invalidate(donationHistoryProvider);
      ref.invalidate(myDonationResponsesProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('La donación fue registrada en tu historial.')),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(friendlyError(error))));
    } finally {
      if (mounted) setState(() => _busyId = null);
    }
  }

  Future<void> _cancel(DonationResponseModel response) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Cancelar ofrecimiento'),
        content: const Text('¿Quieres cancelar este ofrecimiento?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('No'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Sí, cancelar'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    setState(() => _busyId = response.id);
    try {
      await ref.read(donationRepositoryProvider).cancel(response.id);
      ref.invalidate(myDonationResponsesProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('El ofrecimiento fue cancelado.')),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(friendlyError(error))));
    } finally {
      if (mounted) setState(() => _busyId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authControllerProvider).user;
    final history = ref.watch(donationHistoryProvider);
    final responses = ref.watch(myDonationResponsesProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Mis donaciones')),
      body: RefreshIndicator(
        onRefresh: _refresh,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
          children: [
            if (user?.isDonor != true)
              EmptyState(
                title: 'Completa tu perfil de donante',
                message:
                    'Para ofrecer ayuda y consultar tu historial, primero crea tu perfil de donante.',
                icon: Icons.person_add_alt_1,
                action: FilledButton(
                  onPressed: () => context.push('/perfil/donante'),
                  child: const Text('Crear perfil de donante'),
                ),
              )
            else ...[
              history.when(
                loading: () => const Padding(
                  padding: EdgeInsets.symmetric(vertical: 24),
                  child: Center(child: CircularProgressIndicator()),
                ),
                error: (error, _) => ErrorView(
                  message: friendlyError(error),
                  onRetry: () => ref.invalidate(donationHistoryProvider),
                ),
                data: (summary) => _HistorySummary(summary: summary),
              ),
              const SizedBox(height: 22),
              SectionHeader(
                title: 'Mis ofrecimientos',
                actionLabel: 'Solicitudes',
                onAction: () => context.go('/solicitudes'),
              ),
              const SizedBox(height: 8),
              responses.when(
                loading: () => const Padding(
                  padding: EdgeInsets.symmetric(vertical: 20),
                  child: Center(child: CircularProgressIndicator()),
                ),
                error: (error, _) => ErrorView(
                  message: friendlyError(error),
                  onRetry: () => ref.invalidate(myDonationResponsesProvider),
                ),
                data: (items) {
                  if (items.isEmpty) {
                    return const EmptyState(
                      title: 'Aún no has ofrecido ayuda',
                      message:
                          'Cuando respondas una solicitud con “Quiero ayudar”, aparecerá aquí.',
                      icon: Icons.volunteer_activism_outlined,
                    );
                  }
                  return Column(
                    children: items
                        .map(
                          (item) => _ResponseCard(
                            response: item,
                            busy: _busyId == item.id,
                            onOpen: () =>
                                context.push('/solicitudes/${item.bloodRequestId}'),
                            onComplete: item.canComplete
                                ? () => _complete(item)
                                : null,
                            onCancel: item.canCancel ? () => _cancel(item) : null,
                          ),
                        )
                        .toList(),
                  );
                },
              ),
              const SizedBox(height: 22),
              const SectionHeader(title: 'Historial de donaciones'),
              const SizedBox(height: 8),
              history.when(
                loading: () => const SizedBox.shrink(),
                error: (_, _) => const SizedBox.shrink(),
                data: (summary) {
                  if (summary == null || summary.history.isEmpty) {
                    return const EmptyState(
                      title: 'Aún no hay donaciones',
                      message:
                          'Cuando completes una donación aparecerá en este historial.',
                      icon: Icons.history,
                    );
                  }
                  return Column(
                    children: summary.history
                        .map((donation) => DonationCard(donation: donation))
                        .toList(),
                  );
                },
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _HistorySummary extends StatelessWidget {
  const _HistorySummary({required this.summary});

  final DonationHistory? summary;

  @override
  Widget build(BuildContext context) {
    final totalDonations = summary?.totalDonations ?? 0;
    final totalUnits = summary?.totalUnits ?? 0;
    return Column(
      children: [
        Card(
          color: Theme.of(context).colorScheme.primaryContainer,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _Metric(value: '$totalDonations', label: 'Donaciones'),
                _Metric(value: '$totalUnits', label: 'Unidades'),
                _Metric(
                  value: formatDate(summary?.lastDonation, short: true),
                  label: 'Última',
                ),
              ],
            ),
          ),
        ),
        if (summary?.orientationNote.isNotEmpty == true) ...[
          const SizedBox(height: 12),
          Card(
            color: Theme.of(context).colorScheme.secondaryContainer,
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Text(summary!.orientationNote),
            ),
          ),
        ],
      ],
    );
  }
}

class _ResponseCard extends StatelessWidget {
  const _ResponseCard({
    required this.response,
    required this.busy,
    required this.onOpen,
    this.onComplete,
    this.onCancel,
  });

  final DonationResponseModel response;
  final bool busy;
  final VoidCallback onOpen;
  final VoidCallback? onComplete;
  final VoidCallback? onCancel;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 14, 16, 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                if (response.requestBloodType?.isNotEmpty == true) ...[
                  BloodTypeBadge(response.requestBloodType!),
                  const SizedBox(width: 8),
                ],
                Expanded(
                  child: Text(
                    response.hospital?.isNotEmpty == true
                        ? response.hospital!
                        : 'Solicitud de sangre',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                Text(
                  responseStatusLabel(response.status),
                  style: Theme.of(context).textTheme.labelLarge,
                ),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              '${response.location} · ${formatDate(response.createdAt, short: true)}',
            ),
            if (response.message.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text('“${response.message}”'),
            ],
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                OutlinedButton(
                  onPressed: onOpen,
                  child: const Text('Ver solicitud'),
                ),
                if (onComplete != null)
                  FilledButton(
                    onPressed: busy ? null : onComplete,
                    child: Text(busy ? 'Registrando…' : 'Marcar donada'),
                  ),
                if (onCancel != null)
                  TextButton(
                    onPressed: busy ? null : onCancel,
                    child: const Text('Cancelar'),
                  ),
              ],
            ),
          ],
        ),
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
