import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_models.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../../auth/domain/auth_controller.dart';
import '../data/blood_request_repository.dart';

class BloodRequestDetailPage extends ConsumerStatefulWidget {
  const BloodRequestDetailPage({super.key, required this.requestId});

  final int requestId;

  @override
  ConsumerState<BloodRequestDetailPage> createState() =>
      _BloodRequestDetailPageState();
}

class _BloodRequestDetailPageState
    extends ConsumerState<BloodRequestDetailPage> {
  bool _submitting = false;

  Future<void> _offerHelp() async {
    final message = await showDialog<String>(
      context: context,
      builder: (context) => const _HelpDialog(),
    );
    if (message == null || !mounted) return;
    setState(() => _submitting = true);
    try {
      await ref
          .read(bloodRequestRepositoryProvider)
          .offerHelp(widget.requestId, message: message);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Tu ofrecimiento fue enviado. Te contactarán para coordinar.',
          ),
        ),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(friendlyError(error))));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _reportDonation() async {
    final units = await showDialog<int>(
      context: context,
      builder: (context) => const _ReportDonationDialog(),
    );
    if (units == null || !mounted) return;
    setState(() => _submitting = true);
    try {
      await ref
          .read(bloodRequestRepositoryProvider)
          .reportDonation(widget.requestId, units: units);
      ref.invalidate(bloodRequestDetailProvider(widget.requestId));
      ref.invalidate(requestDonationsProvider(widget.requestId));
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Tu donación fue reportada. El receptor confirmará las unidades recibidas.',
          ),
        ),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(friendlyError(error))));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _confirmDonation(DonationModel donation, int maxUnits) async {
    final units = await showDialog<int>(
      context: context,
      builder: (context) => _ConfirmDonationDialog(
        reportedUnits: donation.units,
        alreadyConfirmed: donation.confirmedUnits,
        maxUnits: maxUnits,
      ),
    );
    if (units == null || !mounted) return;
    setState(() => _submitting = true);
    try {
      await ref
          .read(bloodRequestRepositoryProvider)
          .confirmDonation(donation.id, confirmedUnits: units);
      ref.invalidate(bloodRequestDetailProvider(widget.requestId));
      ref.invalidate(requestDonationsProvider(widget.requestId));
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Las unidades recibidas fueron confirmadas.')),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(friendlyError(error))));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final request = ref.watch(bloodRequestDetailProvider(widget.requestId));
    final donations = ref.watch(requestDonationsProvider(widget.requestId));
    final user = ref.watch(authControllerProvider).user;
    return Scaffold(
      appBar: AppBar(title: const Text('Detalle de solicitud')),
      body: request.when(
        loading: () => const LoadingView(),
        error: (error, _) => ErrorView(
          message: friendlyError(error),
          onRetry: () =>
              ref.invalidate(bloodRequestDetailProvider(widget.requestId)),
        ),
        data: (item) {
          final isOwner = user != null && user.id == item.createdById;
          final isDonor = user?.isDonor == true;
          final reportedDonations = donations.valueOrNull ?? const <DonationModel>[];
          final mine = reportedDonations.where(
            (donation) => donation.donorUserId == user?.id,
          );
          final myDonation = mine.isEmpty ? null : mine.first;
          final canReport =
              isDonor &&
              !isOwner &&
              (item.status == 'OPEN' || item.status == 'IN_PROGRESS') &&
              (myDonation == null || !myDonation.isPendingConfirmation);
          return ListView(
            padding: const EdgeInsets.all(20),
            children: [
              Row(
                children: [
                  BloodTypeBadge(item.bloodType, large: true),
                  const SizedBox(width: 10),
                  UrgencyBadge(item.urgency),
                ],
              ),
              const SizedBox(height: 20),
              Text(
                item.patientName,
                style: Theme.of(
                  context,
                ).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 4),
              Text(item.hospital, style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 20),
              _ProgressCard(request: item),
              const SizedBox(height: 12),
              _DetailRow(
                icon: Icons.location_on_outlined,
                label: 'Ubicación',
                value: '${item.address}\n${item.location}',
              ),
              _DetailRow(
                icon: Icons.event_outlined,
                label: 'Fecha límite',
                value: formatDate(item.deadline),
              ),
              _DetailRow(
                icon: Icons.phone_outlined,
                label: 'Teléfono de contacto',
                value: item.contactPhone,
              ),
              if (item.description.isNotEmpty) ...[
                const SizedBox(height: 12),
                const SectionHeader(title: 'Información adicional'),
                const SizedBox(height: 6),
                Text(item.description),
              ],
              if (isOwner && reportedDonations.isNotEmpty) ...[
                const SizedBox(height: 24),
                const SectionHeader(title: 'Donaciones reportadas'),
                const SizedBox(height: 8),
                Text(
                  'Confirma cuántas unidades recibiste realmente de cada donante.',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: 12),
                for (final donation in reportedDonations)
                  _ReportedDonationTile(
                    donation: donation,
                    remainingCapacity:
                        item.pendingUnits + donation.confirmedUnits,
                    submitting: _submitting,
                    onConfirm: donation.isPendingConfirmation
                        ? () => _confirmDonation(
                            donation,
                            (item.pendingUnits + donation.confirmedUnits)
                                .clamp(0, donation.units)
                                .toInt(),
                          )
                        : null,
                  ),
              ],
              const SizedBox(height: 28),
              if (!isOwner)
                PrimaryButton(
                  label: 'Quiero ayudar',
                  icon: Icons.volunteer_activism,
                  isLoading: _submitting,
                  onPressed: item.status == 'OPEN' || item.status == 'IN_PROGRESS'
                      ? _offerHelp
                      : null,
                ),
              if (canReport) ...[
                const SizedBox(height: 12),
                PrimaryButton(
                  label: 'Marcar como donación realizada',
                  icon: Icons.bloodtype,
                  isLoading: _submitting,
                  onPressed: _reportDonation,
                ),
              ],
              if (myDonation != null && !isOwner) ...[
                const SizedBox(height: 12),
                Text(
                  'Tu reporte: ${myDonation.units} reportadas, ${myDonation.confirmedUnits} confirmadas · ${myDonation.statusLabel}',
                  textAlign: TextAlign.center,
                ),
              ],
              if (item.status != 'OPEN' && item.status != 'IN_PROGRESS')
                const Padding(
                  padding: EdgeInsets.only(top: 8),
                  child: Text(
                    'Esta solicitud ya no acepta nuevas respuestas.',
                    textAlign: TextAlign.center,
                  ),
                ),
            ],
          );
        },
      ),
    );
  }
}

class _ProgressCard extends StatelessWidget {
  const _ProgressCard({required this.request});

  final BloodRequestModel request;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              request.progressLabel,
              style: Theme.of(
                context,
              ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 12),
            ClipRRect(
              borderRadius: BorderRadius.circular(999),
              child: LinearProgressIndicator(
                value: request.unitsRequired == 0
                    ? 0
                    : request.progressPercent / 100,
                minHeight: 10,
              ),
            ),
            const SizedBox(height: 12),
            Text('Unidades requeridas: ${request.unitsRequired}'),
            Text('Unidades recibidas: ${request.completedUnits}'),
            Text('Unidades pendientes: ${request.pendingUnits}'),
            Text('Progreso: ${request.progressPercent}%'),
          ],
        ),
      ),
    );
  }
}

class _ReportedDonationTile extends StatelessWidget {
  const _ReportedDonationTile({
    required this.donation,
    required this.remainingCapacity,
    required this.submitting,
    this.onConfirm,
  });

  final DonationModel donation;
  final int remainingCapacity;
  final bool submitting;
  final VoidCallback? onConfirm;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        title: Text(donation.donorName),
        subtitle: Text(
          '${formatDate(donation.date, short: true)} · ${donation.units} reportadas · ${donation.confirmedUnits} confirmadas\n${donation.statusLabel}',
        ),
        isThreeLine: true,
        trailing: onConfirm == null
            ? null
            : TextButton(
                onPressed: submitting || remainingCapacity <= 0 ? null : onConfirm,
                child: const Text('Confirmar'),
              ),
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: CircleAvatar(child: Icon(icon)),
      title: Text(label),
      subtitle: Text(value),
    );
  }
}

class _HelpDialog extends StatefulWidget {
  const _HelpDialog();

  @override
  State<_HelpDialog> createState() => _HelpDialogState();
}

class _HelpDialogState extends State<_HelpDialog> {
  final _message = TextEditingController();

  @override
  void dispose() {
    _message.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Ofrecer ayuda'),
      content: TextField(
        controller: _message,
        maxLength: 500,
        maxLines: 3,
        decoration: const InputDecoration(
          labelText: 'Mensaje opcional',
          hintText: 'Indica cuándo podrían contactarte',
          border: OutlineInputBorder(),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(context, _message.text.trim()),
          child: const Text('Enviar'),
        ),
      ],
    );
  }
}

class _ReportDonationDialog extends StatefulWidget {
  const _ReportDonationDialog();

  @override
  State<_ReportDonationDialog> createState() => _ReportDonationDialogState();
}

class _ReportDonationDialogState extends State<_ReportDonationDialog> {
  final _units = TextEditingController(text: '1');

  @override
  void dispose() {
    _units.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Marcar como donación realizada'),
      content: TextField(
        controller: _units,
        keyboardType: TextInputType.number,
        decoration: const InputDecoration(
          labelText: 'Unidades donadas',
          border: OutlineInputBorder(),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: () {
            final value = int.tryParse(_units.text.trim()) ?? 0;
            if (value <= 0) return;
            Navigator.pop(context, value);
          },
          child: const Text('Reportar'),
        ),
      ],
    );
  }
}

class _ConfirmDonationDialog extends StatefulWidget {
  const _ConfirmDonationDialog({
    required this.reportedUnits,
    required this.alreadyConfirmed,
    required this.maxUnits,
  });

  final int reportedUnits;
  final int alreadyConfirmed;
  final int maxUnits;

  @override
  State<_ConfirmDonationDialog> createState() => _ConfirmDonationDialogState();
}

class _ConfirmDonationDialogState extends State<_ConfirmDonationDialog> {
  late final TextEditingController _units;

  @override
  void initState() {
    super.initState();
    _units = TextEditingController(text: '${widget.maxUnits}');
  }

  @override
  void dispose() {
    _units.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Confirmar unidades recibidas'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'El donante reportó ${widget.reportedUnits} ${widget.reportedUnits == 1 ? 'unidad' : 'unidades'}. '
            'Ya confirmadas: ${widget.alreadyConfirmed}. Máximo: ${widget.maxUnits}.',
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _units,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: 'Unidades recibidas',
              border: OutlineInputBorder(),
            ),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: () {
            final value = int.tryParse(_units.text.trim()) ?? 0;
            if (value <= 0 || value > widget.maxUnits) return;
            Navigator.pop(context, value);
          },
          child: const Text('Confirmar'),
        ),
      ],
    );
  }
}
