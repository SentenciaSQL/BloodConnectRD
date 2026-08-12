import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';

import '../../../core/constants/api_paths.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_models.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../../auth/domain/auth_controller.dart';
import '../../locations/data/location_repository.dart';
import '../data/donor_repository.dart';

/// Carga el perfil actual (si existe) antes de abrir el formulario de crear/editar.
class DonorProfileFormGate extends ConsumerWidget {
  const DonorProfileFormGate({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profile = ref.watch(donorProfileProvider);
    return profile.when(
      loading: () => Scaffold(
        appBar: AppBar(title: const Text('Perfil de donante')),
        body: const Center(child: CircularProgressIndicator()),
      ),
      error: (_, _) => const DonorProfileFormPage(),
      data: (donor) => DonorProfileFormPage(
        key: ValueKey(donor?.id ?? 'new'),
        initialProfile: donor,
      ),
    );
  }
}

class DonorProfileFormPage extends ConsumerStatefulWidget {
  const DonorProfileFormPage({super.key, this.initialProfile});

  final DonorProfile? initialProfile;

  @override
  ConsumerState<DonorProfileFormPage> createState() =>
      _DonorProfileFormPageState();
}

class _DonorProfileFormPageState extends ConsumerState<DonorProfileFormPage> {
  final _formKey = GlobalKey<FormState>();
  final _phone = TextEditingController();
  final _sector = TextEditingController();
  final _address = TextEditingController();

  String? _bloodType;
  String? _sex;
  int? _provinceId;
  int? _municipalityId;
  DateTime? _birthDate;
  DateTime? _lastDonationDate;
  double? _latitude;
  double? _longitude;
  bool _submitting = false;
  bool _locating = false;

  bool get _isEditing => widget.initialProfile != null;

  @override
  void initState() {
    super.initState();
    final profile = widget.initialProfile;
    final user = ref.read(authControllerProvider).user;
    _phone.text = profile?.phone.isNotEmpty == true
        ? profile!.phone
        : (user?.phone ?? '');
    if (profile != null) {
      _bloodType = profile.bloodType;
      _sex = profile.sex;
      _provinceId = profile.provinceId == 0 ? null : profile.provinceId;
      _municipalityId = profile.municipalityId == 0
          ? null
          : profile.municipalityId;
      _birthDate = profile.birthDate;
      _lastDonationDate = profile.lastDonationDate;
      _sector.text = profile.sector ?? '';
      _address.text = profile.approximateAddress ?? '';
      _latitude = profile.latitude;
      _longitude = profile.longitude;
    }
  }

  @override
  void dispose() {
    _phone.dispose();
    _sector.dispose();
    _address.dispose();
    super.dispose();
  }

  Future<void> _pickBirthDate() async {
    final now = DateTime.now();
    final initial = _birthDate ?? DateTime(now.year - 25);
    final date = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(now.year - 80),
      lastDate: DateTime(now.year - 16),
      helpText: 'Fecha de nacimiento',
      cancelText: 'Cancelar',
      confirmText: 'Aceptar',
    );
    if (date != null) setState(() => _birthDate = date);
  }

  Future<void> _pickLastDonation() async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      initialDate: _lastDonationDate ?? now.subtract(const Duration(days: 90)),
      firstDate: DateTime(now.year - 20),
      lastDate: now,
      helpText: 'Última donación (opcional)',
      cancelText: 'Cancelar',
      confirmText: 'Aceptar',
    );
    if (date != null) setState(() => _lastDonationDate = date);
  }

  Future<void> _useCurrentLocation() async {
    setState(() => _locating = true);
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
      setState(() {
        _latitude = position.latitude;
        _longitude = position.longitude;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Ubicación aproximada guardada. No se mostrará públicamente.',
            ),
          ),
        );
      }
    } on PermissionDeniedException {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Sin GPS puedes continuar con provincia y municipio.',
            ),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'No pudimos obtener tu ubicación. Continúa manualmente.',
            ),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _locating = false);
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_bloodType == null ||
        _sex == null ||
        _provinceId == null ||
        _municipalityId == null ||
        _birthDate == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Completa todos los campos obligatorios.'),
        ),
      );
      return;
    }

    setState(() => _submitting = true);
    final payload = <String, dynamic>{
      'bloodType': _bloodType,
      'birthDate': _formatApiDate(_birthDate!),
      'sex': _sex,
      'phone': _phone.text.trim(),
      'provinceId': _provinceId,
      'municipalityId': _municipalityId,
      'sector': _optional(_sector.text),
      'approximateAddress': _optional(_address.text),
      'latitude': _latitude,
      'longitude': _longitude,
      'lastDonationDate': _lastDonationDate == null
          ? null
          : _formatApiDate(_lastDonationDate!),
    };

    try {
      final repo = ref.read(donorRepositoryProvider);
      if (_isEditing) {
        await repo.update(payload);
      } else {
        await repo.create(payload);
      }
      await ref.read(authControllerProvider.notifier).refreshUser();
      ref.invalidate(donorProfileProvider);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            _isEditing
                ? 'Tu perfil de donante fue actualizado.'
                : 'Tu perfil de donante fue creado. Ya puedes ayudar.',
          ),
        ),
      );
      context.go('/perfil');
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(friendlyError(error))));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  String _formatApiDate(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-'
      '${date.month.toString().padLeft(2, '0')}-'
      '${date.day.toString().padLeft(2, '0')}';

  String? _optional(String value) {
    final text = value.trim();
    return text.isEmpty ? null : text;
  }

  String? _validatePhone(String? value) {
    final text = value?.trim() ?? '';
    if (text.isEmpty) return 'El teléfono es obligatorio';
    final digits = text.replaceAll(RegExp(r'\D'), '');
    final normalized = digits.startsWith('1') && digits.length == 11
        ? digits
        : digits;
    final match = RegExp(r'^(1)?(809|829|849)\d{7}$').hasMatch(normalized);
    if (!match) {
      return 'Usa un teléfono dominicano (809, 829 o 849)';
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final provinces = ref.watch(provincesProvider);
    final municipalities = _provinceId == null
        ? const AsyncValue<List<Municipality>>.data([])
        : ref.watch(municipalitiesProvider(_provinceId!));

    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? 'Editar perfil de donante' : 'Ser donante'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
          children: [
            Text(
              _isEditing
                  ? 'Mantén tus datos actualizados para recibir solicitudes compatibles.'
                  : 'Completa tu perfil para responder solicitudes de sangre en República Dominicana.',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 8),
            Text(
              'La elegibilidad final para donar la determinan profesionales de la salud.',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 20),
            const SectionHeader(title: 'Datos de donante'),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: _bloodType,
              decoration: const InputDecoration(
                labelText: 'Tipo de sangre',
                border: OutlineInputBorder(),
              ),
              items: BloodTypes.values
                  .map(
                    (type) => DropdownMenuItem(value: type, child: Text(type)),
                  )
                  .toList(),
              validator: (value) =>
                  value == null ? 'Selecciona tu tipo de sangre' : null,
              onChanged: (value) => setState(() => _bloodType = value),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: _sex,
              decoration: const InputDecoration(
                labelText: 'Sexo',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'FEMALE', child: Text('Femenino')),
                DropdownMenuItem(value: 'MALE', child: Text('Masculino')),
                DropdownMenuItem(value: 'OTHER', child: Text('Otro')),
              ],
              validator: (value) => value == null ? 'Selecciona una opción' : null,
              onChanged: (value) => setState(() => _sex = value),
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: _pickBirthDate,
              icon: const Icon(Icons.cake_outlined),
              label: Text(
                _birthDate == null
                    ? 'Fecha de nacimiento'
                    : 'Nacimiento: ${formatDate(_birthDate, short: true)}',
              ),
            ),
            const SizedBox(height: 12),
            AppTextField(
              controller: _phone,
              label: 'Teléfono',
              hint: '809-555-1234',
              keyboardType: TextInputType.phone,
              prefixIcon: Icons.phone_outlined,
              validator: _validatePhone,
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: _pickLastDonation,
              icon: const Icon(Icons.event_outlined),
              label: Text(
                _lastDonationDate == null
                    ? 'Última donación (opcional)'
                    : 'Última donación: ${formatDate(_lastDonationDate, short: true)}',
              ),
            ),
            if (_lastDonationDate != null)
              Align(
                alignment: Alignment.centerLeft,
                child: TextButton(
                  onPressed: () => setState(() => _lastDonationDate = null),
                  child: const Text('Quitar fecha de última donación'),
                ),
              ),
            const SizedBox(height: 24),
            const SectionHeader(title: 'Ubicación'),
            const SizedBox(height: 8),
            Text(
              'Tu dirección exacta no se muestra públicamente.',
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 12),
            provinces.when(
              loading: () => const LinearProgressIndicator(),
              error: (_, _) => const Text('No se pudieron cargar provincias'),
              data: (items) => DropdownButtonFormField<int>(
                value: _provinceId,
                decoration: const InputDecoration(
                  labelText: 'Provincia',
                  border: OutlineInputBorder(),
                ),
                items: items
                    .map(
                      (item) => DropdownMenuItem(
                        value: item.id,
                        child: Text(item.name),
                      ),
                    )
                    .toList(),
                validator: (value) =>
                    value == null ? 'Selecciona una provincia' : null,
                onChanged: (value) => setState(() {
                  _provinceId = value;
                  _municipalityId = null;
                }),
              ),
            ),
            const SizedBox(height: 12),
            municipalities.when(
              loading: () => const LinearProgressIndicator(),
              error: (_, _) => const Text('No se pudieron cargar municipios'),
              data: (items) => DropdownButtonFormField<int>(
                key: ValueKey(_provinceId),
                value: _municipalityId,
                decoration: const InputDecoration(
                  labelText: 'Municipio',
                  border: OutlineInputBorder(),
                ),
                items: items
                    .map(
                      (item) => DropdownMenuItem(
                        value: item.id,
                        child: Text(item.name),
                      ),
                    )
                    .toList(),
                validator: (value) =>
                    value == null ? 'Selecciona un municipio' : null,
                onChanged: _provinceId == null
                    ? null
                    : (value) => setState(() => _municipalityId = value),
              ),
            ),
            const SizedBox(height: 12),
            AppTextField(
              controller: _sector,
              label: 'Sector (opcional)',
              textInputAction: TextInputAction.next,
            ),
            const SizedBox(height: 12),
            AppTextField(
              controller: _address,
              label: 'Dirección aproximada (opcional)',
              maxLines: 2,
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: _locating ? null : _useCurrentLocation,
              icon: _locating
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.my_location_outlined),
              label: Text(
                _latitude == null
                    ? 'Usar mi ubicación aproximada'
                    : 'Ubicación lista (${_latitude!.toStringAsFixed(3)}, ${_longitude!.toStringAsFixed(3)})',
              ),
            ),
            const SizedBox(height: 28),
            PrimaryButton(
              label: _submitting
                  ? 'Guardando…'
                  : (_isEditing ? 'Guardar cambios' : 'Crear perfil de donante'),
              onPressed: _submitting ? null : _submit,
            ),
          ],
        ),
      ),
    );
  }
}
