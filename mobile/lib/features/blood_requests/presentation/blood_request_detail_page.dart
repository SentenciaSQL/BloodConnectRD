import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_widgets.dart';
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

  @override
  Widget build(BuildContext context) {
    final request = ref.watch(bloodRequestDetailProvider(widget.requestId));
    return Scaffold(
      appBar: AppBar(title: const Text('Detalle de solicitud')),
      body: request.when(
        loading: () => const LoadingView(),
        error: (error, _) => ErrorView(
          message: friendlyError(error),
          onRetry: () =>
              ref.invalidate(bloodRequestDetailProvider(widget.requestId)),
        ),
        data: (item) => ListView(
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
            _DetailRow(
              icon: Icons.water_drop_outlined,
              label: 'Unidades requeridas',
              value:
                  '${item.unitsRequired} (${item.completedUnits} completadas)',
            ),
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
            const SizedBox(height: 28),
            PrimaryButton(
              label: 'Quiero ayudar',
              icon: Icons.volunteer_activism,
              isLoading: _submitting,
              onPressed: item.status == 'OPEN' || item.status == 'IN_PROGRESS'
                  ? _offerHelp
                  : null,
            ),
            if (item.status != 'OPEN' && item.status != 'IN_PROGRESS')
              const Padding(
                padding: EdgeInsets.only(top: 8),
                child: Text(
                  'Esta solicitud ya no acepta nuevas respuestas.',
                  textAlign: TextAlign.center,
                ),
              ),
          ],
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
