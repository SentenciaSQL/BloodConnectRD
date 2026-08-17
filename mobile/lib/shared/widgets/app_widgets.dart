import 'package:flutter/material.dart';

import '../../core/networking/api_models.dart';
import '../../core/utils/formatters.dart';

class BloodTypeBadge extends StatelessWidget {
  const BloodTypeBadge(this.bloodType, {super.key, this.large = false});

  final String bloodType;
  final bool large;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: large ? 14 : 10,
        vertical: large ? 8 : 5,
      ),
      decoration: BoxDecoration(
        color: colors.errorContainer,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        bloodType,
        style: TextStyle(
          color: colors.onErrorContainer,
          fontWeight: FontWeight.w800,
          fontSize: large ? 18 : 13,
        ),
      ),
    );
  }
}

class UrgencyBadge extends StatelessWidget {
  const UrgencyBadge(this.urgency, {super.key});

  final String urgency;

  @override
  Widget build(BuildContext context) {
    final color = switch (urgency) {
      'CRITICAL' => Theme.of(context).colorScheme.error,
      'HIGH' => Colors.deepOrange,
      'MEDIUM' => Colors.amber.shade800,
      _ => Colors.blueGrey,
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Text(
        'Urgencia ${urgencyLabel(urgency).toLowerCase()}',
        style: TextStyle(
          color: color,
          fontSize: 12,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class RequestCard extends StatelessWidget {
  const RequestCard({super.key, required this.request, this.onTap});

  final BloodRequestModel request;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  BloodTypeBadge(request.bloodType),
                  const SizedBox(width: 8),
                  UrgencyBadge(request.urgency),
                  const Spacer(),
                  const Icon(Icons.chevron_right),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                request.patientName,
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 4),
              Text(request.hospital),
              const SizedBox(height: 8),
              _IconText(Icons.location_on_outlined, request.location),
              const SizedBox(height: 4),
              _IconText(Icons.water_drop_outlined, request.progressLabel),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(999),
                      child: LinearProgressIndicator(
                        value: request.unitsRequired == 0
                            ? 0
                            : request.progressPercent / 100,
                        minHeight: 10,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    '${request.progressPercent}%',
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ],
              ),
              if (request.distanceKm != null) ...[
                const SizedBox(height: 4),
                _IconText(
                  Icons.near_me_outlined,
                  '${request.distanceKm!.toStringAsFixed(1)} km de distancia',
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class DonationCard extends StatelessWidget {
  const DonationCard({super.key, required this.donation, this.onTap});

  final DonationModel donation;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final child = Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            donation.statusLabel,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 8),
          if (donation.patientName != null && donation.patientName!.isNotEmpty) ...[
            _IconText(Icons.assignment_outlined, 'Solicitud: ${donation.patientName}'),
            const SizedBox(height: 4),
          ],
          if (donation.receiverName != null && donation.receiverName!.isNotEmpty) ...[
            _IconText(Icons.favorite_outline, 'Receptor: ${donation.receiverName}'),
            const SizedBox(height: 4),
          ],
          if ((donation.hospital != null && donation.hospital!.isNotEmpty) ||
              donation.centerName.isNotEmpty) ...[
            _IconText(
              Icons.local_hospital_outlined,
              'Hospital: ${donation.hospital?.isNotEmpty == true ? donation.hospital! : donation.centerName}',
            ),
            const SizedBox(height: 4),
          ],
          _IconText(
            Icons.event_outlined,
            'Fecha: ${formatDate(donation.date, short: true)}',
          ),
          const SizedBox(height: 4),
          _IconText(
            Icons.water_drop_outlined,
            'Unidades donadas: ${donation.units}',
          ),
        ],
      ),
    );
    return Card(
      child: onTap == null
          ? child
          : InkWell(onTap: onTap, child: child),
    );
  }
}

class DonationCenterCard extends StatelessWidget {
  const DonationCenterCard({super.key, required this.center, this.onTap});

  final DonationCenterModel center;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                center.name,
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
              ),
              Text(centerTypeLabel(center.type)),
              const SizedBox(height: 10),
              _IconText(Icons.location_on_outlined, center.address),
              if (center.phone.isNotEmpty) ...[
                const SizedBox(height: 4),
                _IconText(Icons.phone_outlined, center.phone),
              ],
              if (center.schedule.isNotEmpty) ...[
                const SizedBox(height: 4),
                _IconText(Icons.schedule_outlined, center.schedule),
              ],
              if (center.distanceKm != null) ...[
                const SizedBox(height: 4),
                _IconText(
                  Icons.near_me_outlined,
                  '${center.distanceKm!.toStringAsFixed(1)} km',
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class UnitsStepper extends StatelessWidget {
  const UnitsStepper({
    super.key,
    required this.value,
    required this.onChanged,
    this.min = 1,
    this.max = 1,
  });

  final int value;
  final int min;
  final int max;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        IconButton.filledTonal(
          onPressed: value <= min ? null : () => onChanged(value - 1),
          icon: const Icon(Icons.remove),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20),
          child: Text(
            '$value',
            style: Theme.of(
              context,
            ).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w800),
          ),
        ),
        IconButton.filledTonal(
          onPressed: value >= max ? null : () => onChanged(value + 1),
          icon: const Icon(Icons.add),
        ),
      ],
    );
  }
}

class PrimaryButton extends StatelessWidget {
  const PrimaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.isLoading = false,
    this.icon,
  });

  final String label;
  final VoidCallback? onPressed;
  final bool isLoading;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final child = isLoading
        ? const SizedBox.square(
            dimension: 20,
            child: CircularProgressIndicator(strokeWidth: 2),
          )
        : Text(label);
    return SizedBox(
      width: double.infinity,
      child: icon == null
          ? FilledButton(onPressed: isLoading ? null : onPressed, child: child)
          : FilledButton.icon(
              onPressed: isLoading ? null : onPressed,
              icon: Icon(icon),
              label: child,
            ),
    );
  }
}

class AppTextField extends StatelessWidget {
  const AppTextField({
    super.key,
    required this.controller,
    required this.label,
    this.hint,
    this.keyboardType,
    this.textInputAction,
    this.validator,
    this.obscureText = false,
    this.maxLines = 1,
    this.prefixIcon,
    this.enabled = true,
  });

  final TextEditingController controller;
  final String label;
  final String? hint;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final FormFieldValidator<String>? validator;
  final bool obscureText;
  final int maxLines;
  final IconData? prefixIcon;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      enabled: enabled,
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        prefixIcon: prefixIcon == null ? null : Icon(prefixIcon),
        border: const OutlineInputBorder(),
      ),
      keyboardType: keyboardType,
      textInputAction: textInputAction,
      validator: validator,
      obscureText: obscureText,
      maxLines: obscureText ? 1 : maxLines,
    );
  }
}

class SectionHeader extends StatelessWidget {
  const SectionHeader({
    super.key,
    required this.title,
    this.actionLabel,
    this.onAction,
  });

  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Text(
            title,
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
          ),
        ),
        if (actionLabel != null)
          TextButton(onPressed: onAction, child: Text(actionLabel!)),
      ],
    );
  }
}

class LoadingView extends StatelessWidget {
  const LoadingView({super.key, this.message = 'Cargando…'});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const CircularProgressIndicator(),
          const SizedBox(height: 12),
          Text(message),
        ],
      ),
    );
  }
}

class ErrorView extends StatelessWidget {
  const ErrorView({super.key, required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.error_outline,
              size: 48,
              color: Theme.of(context).colorScheme.error,
            ),
            const SizedBox(height: 12),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            RetryButton(onPressed: onRetry),
          ],
        ),
      ),
    );
  }
}

class EmptyState extends StatelessWidget {
  const EmptyState({
    super.key,
    required this.title,
    required this.message,
    this.icon = Icons.inbox_outlined,
    this.action,
  });

  final String title;
  final String message;
  final IconData icon;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 52, color: Theme.of(context).colorScheme.outline),
            const SizedBox(height: 12),
            Text(
              title,
              style: Theme.of(
                context,
              ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 6),
            Text(message, textAlign: TextAlign.center),
            if (action != null) ...[const SizedBox(height: 16), action!],
          ],
        ),
      ),
    );
  }
}

class OfflineBanner extends StatelessWidget {
  const OfflineBanner({super.key, required this.isOffline});

  final bool isOffline;

  @override
  Widget build(BuildContext context) {
    if (!isOffline) return const SizedBox.shrink();
    return Material(
      color: Theme.of(context).colorScheme.errorContainer,
      child: const SafeArea(
        bottom: false,
        child: Padding(
          padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.cloud_off_outlined, size: 18),
              SizedBox(width: 8),
              Flexible(child: Text('Sin conexión a internet')),
            ],
          ),
        ),
      ),
    );
  }
}

class RetryButton extends StatelessWidget {
  const RetryButton({super.key, required this.onPressed});

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onPressed,
      icon: const Icon(Icons.refresh),
      label: const Text('Reintentar'),
    );
  }
}

class _IconText extends StatelessWidget {
  const _IconText(this.icon, this.text);

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 17, color: Theme.of(context).colorScheme.outline),
        const SizedBox(width: 6),
        Expanded(
          child: Text(text, style: Theme.of(context).textTheme.bodySmall),
        ),
      ],
    );
  }
}
