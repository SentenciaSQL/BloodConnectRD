import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_models.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../../auth/domain/auth_controller.dart';
import '../../donations/data/donation_repository.dart';
import '../../messages/data/conversation_repository.dart';
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

  PreferredSizeWidget _buildAppBar() {
    return AppBar(
      automaticallyImplyLeading: false,
      leading: IconButton(
        tooltip: 'Volver',
        icon: const Icon(Icons.arrow_back),
        onPressed: () {
          if (context.canPop()) {
            context.pop();
          } else {
            context.go('/mis-solicitudes');
          }
        },
      ),
      title: const Text('Detalle de solicitud'),
    );
  }

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
      ref.invalidate(requestResponsesProvider(widget.requestId));
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Tu ofrecimiento de ayuda fue enviado')),
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

  Future<void> _reportDonation(int maxUnits) async {
    final draft = await showModalBottomSheet<_DonationReportDraft>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (context) => _ReportDonationSheet(maxUnits: maxUnits),
    );
    if (draft == null || !mounted) return;
    setState(() => _submitting = true);
    try {
      await ref
          .read(bloodRequestRepositoryProvider)
          .reportDonation(
            widget.requestId,
            units: draft.units,
            donationDate: draft.date,
            notes: draft.notes,
          );
      invalidateBloodRequestCaches(ref, requestId: widget.requestId);
      ref.invalidate(donationHistoryProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Donación reportada – pendiente de confirmación del receptor.',
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

  Future<void> _contact(DonationResponseModel offer) async {
    setState(() => _submitting = true);
    try {
      final conversation = await ref
          .read(conversationRepositoryProvider)
          .open(requestId: widget.requestId, donorUserId: offer.donorUserId);
      if (!mounted) return;
      context.push('/mensajes/${conversation.id}');
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
    final units = await showModalBottomSheet<int>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (context) => _ConfirmReceptionSheet(
        donorName: donation.donorName,
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
      invalidateBloodRequestCaches(ref, requestId: widget.requestId);
      ref.invalidate(donationHistoryProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Las unidades recibidas fueron confirmadas.'),
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
    final donations = ref.watch(requestDonationsProvider(widget.requestId));
    final offers = ref.watch(requestResponsesProvider(widget.requestId));
    final user = ref.watch(authControllerProvider).user;
    return request.when(
      loading: () =>
          Scaffold(appBar: _buildAppBar(), body: const LoadingView()),
      error: (error, _) => Scaffold(
        appBar: _buildAppBar(),
        body: ErrorView(
          message: friendlyError(error),
          onRetry: () =>
              ref.invalidate(bloodRequestDetailProvider(widget.requestId)),
        ),
      ),
      data: (item) {
        final isOwner = user != null && user.id == item.createdById;
        final isDonor = user?.isDonor == true;
        final reportedDonations =
            donations.valueOrNull ?? const <DonationModel>[];
        final helpOffers =
            offers.valueOrNull ?? const <DonationResponseModel>[];
        final mine = reportedDonations.where(
          (donation) => donation.donorUserId == user?.id,
        );
        final myDonation = mine.isEmpty ? null : mine.first;
        final pendingMine =
            myDonation != null && myDonation.isPendingConfirmation;
        final myOffer = helpOffers.where(
          (offer) => offer.donorUserId == user?.id && offer.isActiveOffer,
        );
        final alreadyOffered = myOffer.isNotEmpty;
        final canOffer =
            isDonor &&
            !isOwner &&
            !alreadyOffered &&
            (item.status == 'OPEN' || item.status == 'IN_PROGRESS');
        final canReport =
            isDonor &&
            !isOwner &&
            (item.status == 'OPEN' || item.status == 'IN_PROGRESS') &&
            item.pendingUnits > 0 &&
            !pendingMine;
        return Scaffold(
          appBar: _buildAppBar(),
          body: ListView(
            padding: EdgeInsets.fromLTRB(
              20,
              20,
              20,
              canReport || pendingMine ? 120 : 28,
            ),
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
                item.hospital,
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 4),
              Text(item.location),
              const SizedBox(height: 16),
              Text(
                'Necesita ${item.unitsRequired} ${item.unitsRequired == 1 ? 'unidad' : 'unidades'}',
                style: Theme.of(
                  context,
                ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 16),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: RequestProgressBar(request: item, height: 12),
                ),
              ),
              const SizedBox(height: 12),
              _DetailRow(
                icon: Icons.person_outline,
                label: 'Paciente',
                value: item.patientName,
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
              if (isOwner) ...[
                const SizedBox(height: 24),
                const SectionHeader(title: 'Personas interesadas en ayudar'),
                const SizedBox(height: 8),
                Text(
                  'Estos mensajes no cambian el progreso. Las unidades se suman cuando confirmas “Ya doné”.',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: 8),
                if (helpOffers.isEmpty)
                  const Text(
                    'Cuando un donante pulse “Quiero ayudar”, su mensaje aparecerá aquí.',
                  )
                else
                  for (final offer in helpOffers)
                    _HelpOfferTile(
                      offer: offer,
                      contacting: _submitting,
                      onContact: () => _contact(offer),
                    ),
                const SizedBox(height: 24),
                const SectionHeader(title: 'Donaciones reportadas'),
                const SizedBox(height: 8),
                if (reportedDonations.isEmpty)
                  const Text(
                    'Cuando un donante pulse “Ya doné”, el reporte aparecerá aquí para que confirmes la recepción.',
                  )
                else
                  for (final donation in reportedDonations)
                    _ReportedDonationTile(
                      donation: donation,
                      submitting: _submitting,
                      onConfirm:
                          donation.isPendingConfirmation &&
                              item.pendingUnits + donation.confirmedUnits > 0
                          ? () => _confirmDonation(
                              donation,
                              (item.pendingUnits + donation.confirmedUnits)
                                  .clamp(0, donation.units)
                                  .toInt(),
                            )
                          : null,
                    ),
              ],
              if (!isOwner && isDonor) ...[
                const SizedBox(height: 24),
                if (alreadyOffered)
                  Card(
                    color: Theme.of(context).colorScheme.secondaryContainer,
                    child: const Padding(
                      padding: EdgeInsets.all(16),
                      child: Text(
                        'Ya ofreciste ayudar. El creador de la solicitud recibió tu mensaje y podrá contactarte por el chat interno de BloodConnect.',
                        style: TextStyle(fontWeight: FontWeight.w700),
                      ),
                    ),
                  )
                else
                  PrimaryButton(
                    label: 'Quiero ayudar',
                    icon: Icons.volunteer_activism,
                    isLoading: _submitting,
                    onPressed: canOffer ? _offerHelp : null,
                  ),
              ],
              if (!isOwner && user != null && !isDonor) ...[
                const SizedBox(height: 24),
                const Text(
                  'Completa tu perfil de donante para ofrecer ayuda en esta solicitud.',
                ),
              ],
              if (pendingMine && !isOwner) ...[
                const SizedBox(height: 16),
                Card(
                  color: Theme.of(context).colorScheme.secondaryContainer,
                  child: const Padding(
                    padding: EdgeInsets.all(16),
                    child: Text(
                      'Donación reportada – pendiente de confirmación del receptor.',
                      style: TextStyle(fontWeight: FontWeight.w700),
                    ),
                  ),
                ),
              ],
            ],
          ),
          bottomNavigationBar: canReport
              ? SafeArea(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
                    child: FilledButton.icon(
                      onPressed: _submitting
                          ? null
                          : () => _reportDonation(item.pendingUnits),
                      icon: const Icon(Icons.water_drop),
                      label: Text(_submitting ? 'Registrando…' : '🩸 Ya doné'),
                      style: FilledButton.styleFrom(
                        minimumSize: const Size.fromHeight(52),
                        textStyle: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                  ),
                )
              : null,
        );
      },
    );
  }
}

class _HelpOfferTile extends StatelessWidget {
  const _HelpOfferTile({
    required this.offer,
    required this.contacting,
    required this.onContact,
  });

  final DonationResponseModel offer;
  final bool contacting;
  final VoidCallback onContact;

  @override
  Widget build(BuildContext context) {
    final bloodType = offer.donorBloodType;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              bloodType == null || bloodType.isEmpty
                  ? offer.donorName
                  : '${offer.donorName} — $bloodType',
              style: Theme.of(
                context,
              ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 6),
            Text('Fecha: ${formatDate(offer.createdAt, short: true)}'),
            Text('Estado: ${offer.statusLabel}'),
            const SizedBox(height: 8),
            Text(
              (offer.message == null || offer.message!.isEmpty)
                  ? 'Sin mensaje adicional.'
                  : '“${offer.message}”',
            ),
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: contacting ? null : onContact,
              icon: const Icon(Icons.chat_outlined),
              label: Text(contacting ? 'Abriendo…' : 'Contactar'),
            ),
          ],
        ),
      ),
    );
  }
}

class _ReportedDonationTile extends StatelessWidget {
  const _ReportedDonationTile({
    required this.donation,
    required this.submitting,
    this.onConfirm,
  });

  final DonationModel donation;
  final bool submitting;
  final VoidCallback? onConfirm;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              donation.donorName,
              style: Theme.of(
                context,
              ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 6),
            Text(
              'Reportó: ${donation.units} ${donation.units == 1 ? 'unidad' : 'unidades'}',
            ),
            Text('Fecha: ${formatDate(donation.date, short: true)}'),
            Text('Estado: ${donation.statusLabel}'),
            if (onConfirm != null) ...[
              const SizedBox(height: 12),
              FilledButton(
                onPressed: submitting ? null : onConfirm,
                child: const Text('Confirmar recepción'),
              ),
            ],
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
          hintText: 'Hola, soy O+ y puedo donar mañana en la mañana.',
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

class _DonationReportDraft {
  const _DonationReportDraft({
    required this.units,
    required this.date,
    this.notes,
  });

  final int units;
  final DateTime date;
  final String? notes;
}

class _ReportDonationSheet extends StatefulWidget {
  const _ReportDonationSheet({required this.maxUnits});

  final int maxUnits;

  @override
  State<_ReportDonationSheet> createState() => _ReportDonationSheetState();
}

class _ReportDonationSheetState extends State<_ReportDonationSheet> {
  late int _units;
  DateTime _date = DateTime.now();
  final _notes = TextEditingController();

  @override
  void initState() {
    super.initState();
    _units = widget.maxUnits < 1 ? 1 : 1;
  }

  @override
  void dispose() {
    _notes.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final selected = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: DateTime.now().subtract(const Duration(days: 30)),
      lastDate: DateTime.now(),
    );
    if (selected != null) setState(() => _date = selected);
  }

  @override
  Widget build(BuildContext context) {
    final max = widget.maxUnits < 1 ? 1 : widget.maxUnits;
    return Padding(
      padding: EdgeInsets.fromLTRB(
        20,
        8,
        20,
        20 + MediaQuery.viewInsetsOf(context).bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Ya doné',
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          Text(
            'Esta solicitud aún necesita ${widget.maxUnits} ${widget.maxUnits == 1 ? 'unidad' : 'unidades'}.',
          ),
          const SizedBox(height: 20),
          const Text('¿Cuántas unidades donaste?'),
          const SizedBox(height: 8),
          UnitsStepper(
            value: _units,
            min: 1,
            max: max,
            onChanged: (value) => setState(() => _units = value),
          ),
          const SizedBox(height: 16),
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Fecha de donación'),
            subtitle: Text(formatDate(_date, short: true)),
            trailing: const Icon(Icons.event),
            onTap: _pickDate,
          ),
          TextField(
            controller: _notes,
            maxLength: 500,
            maxLines: 2,
            decoration: const InputDecoration(
              labelText: 'Nota opcional',
              border: OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: widget.maxUnits < 1
                ? null
                : () => Navigator.pop(
                    context,
                    _DonationReportDraft(
                      units: _units,
                      date: _date,
                      notes: _notes.text.trim().isEmpty
                          ? null
                          : _notes.text.trim(),
                    ),
                  ),
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(48),
            ),
            child: const Text('Confirmar donación'),
          ),
        ],
      ),
    );
  }
}

class _ConfirmReceptionSheet extends StatefulWidget {
  const _ConfirmReceptionSheet({
    required this.donorName,
    required this.reportedUnits,
    required this.alreadyConfirmed,
    required this.maxUnits,
  });

  final String donorName;
  final int reportedUnits;
  final int alreadyConfirmed;
  final int maxUnits;

  @override
  State<_ConfirmReceptionSheet> createState() => _ConfirmReceptionSheetState();
}

class _ConfirmReceptionSheetState extends State<_ConfirmReceptionSheet> {
  late int _units;

  @override
  void initState() {
    super.initState();
    _units = widget.maxUnits < 1 ? 1 : widget.maxUnits;
  }

  @override
  Widget build(BuildContext context) {
    final min = widget.alreadyConfirmed < 1 ? 1 : widget.alreadyConfirmed;
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Confirmar recepción',
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          Text(
            '${widget.donorName} reportó ${widget.reportedUnits} ${widget.reportedUnits == 1 ? 'unidad' : 'unidades'}.',
          ),
          const SizedBox(height: 16),
          Text('Unidades reportadas: ${widget.reportedUnits}'),
          Text('Unidades recibidas: $_units'),
          const SizedBox(height: 12),
          UnitsStepper(
            value: _units,
            min: min,
            max: widget.maxUnits < min ? min : widget.maxUnits,
            onChanged: (value) => setState(() => _units = value),
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: () => Navigator.pop(context, _units),
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(48),
            ),
            child: const Text('Confirmar unidades'),
          ),
        ],
      ),
    );
  }
}
