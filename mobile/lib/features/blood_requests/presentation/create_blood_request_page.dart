import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter/services.dart';

import '../../../core/constants/api_paths.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_models.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../../locations/data/location_repository.dart';
import '../data/blood_request_repository.dart';

class CreateBloodRequestPage extends ConsumerStatefulWidget {
  const CreateBloodRequestPage({super.key});

  @override
  ConsumerState<CreateBloodRequestPage> createState() =>
      _CreateBloodRequestPageState();
}

class _CreateBloodRequestPageState
    extends ConsumerState<CreateBloodRequestPage> {
  final _formKey = GlobalKey<FormState>();
  final _patient = TextEditingController();
  final _units = TextEditingController(text: '1');
  final _hospital = TextEditingController();
  final _sector = TextEditingController();
  final _address = TextEditingController();
  final _reference = TextEditingController();
  final _description = TextEditingController();
  final _phone = TextEditingController();

  String? _bloodType;
  String _urgency = 'MEDIUM';
  int? _provinceId;
  int? _municipalityId;
  DateTime? _deadline;
  double? _latitude;
  double? _longitude;
  bool _submitting = false;
  bool _locating = false;

  @override
  void dispose() {
    for (final controller in [
      _patient,
      _units,
      _hospital,
      _sector,
      _address,
      _reference,
      _description,
      _phone,
    ]) {
      controller.dispose();
    }
    super.dispose();
  }

  Future<void> _pickDeadline() async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      initialDate: now.add(const Duration(days: 1)),
      firstDate: now.add(const Duration(days: 1)),
      lastDate: now.add(const Duration(days: 365)),
      helpText: 'Selecciona la fecha límite',
      cancelText: 'Cancelar',
      confirmText: 'Aceptar',
    );
    if (date != null) {
      setState(() {
        _deadline = DateTime(date.year, date.month, date.day, 23, 59);
      });
    }
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
          const SnackBar(content: Text('Ubicación agregada a la solicitud.')),
        );
      }
    } on PermissionDeniedException {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'No se concedió acceso. Selecciona provincia y municipio.',
            ),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'No pudimos obtener tu ubicación. Puedes continuar manualmente.',
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
        _provinceId == null ||
        _municipalityId == null ||
        _deadline == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Completa todos los campos obligatorios.'),
        ),
      );
      return;
    }
    setState(() => _submitting = true);
    try {
      final request = await ref.read(bloodRequestRepositoryProvider).create({
        'patientName': _patient.text.trim(),
        'bloodType': _bloodType,
        'unitsRequired': int.parse(_units.text),
        'hospital': _hospital.text.trim(),
        'provinceId': _provinceId,
        'municipalityId': _municipalityId,
        'sector': _optional(_sector.text),
        'address': _address.text.trim(),
        'reference': _optional(_reference.text),
        'latitude': _latitude,
        'longitude': _longitude,
        'deadline': _deadline!.toUtc().toIso8601String(),
        'description': _optional(_description.text),
        'contactPhone': _phone.text.trim(),
        'urgency': _urgency,
      });
      ref.invalidate(bloodRequestsProvider);
      if (mounted) context.go('/solicitudes/${request.id}');
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

  String? _optional(String value) {
    final text = value.trim();
    return text.isEmpty ? null : text;
  }

  @override
  Widget build(BuildContext context) {
    final provinces = ref.watch(provincesProvider);
    final municipalities = _provinceId == null
        ? const AsyncValue<List<Municipality>>.data([])
        : ref.watch(municipalitiesProvider(_provinceId!));
    return Scaffold(
      appBar: AppBar(title: const Text('Crear solicitud')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            const SectionHeader(title: 'Datos de la solicitud'),
            const SizedBox(height: 12),
            AppTextField(
              controller: _patient,
              label: 'Nombre del paciente',
              validator: _required,
            ),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: DropdownButtonFormField<String>(
                    value: _bloodType,
                    decoration: const InputDecoration(
                      labelText: 'Tipo de sangre',
                      border: OutlineInputBorder(),
                    ),
                    items: BloodTypes.values
                        .map(
                          (type) =>
                              DropdownMenuItem(value: type, child: Text(type)),
                        )
                        .toList(),
                    validator: (value) =>
                        value == null ? 'Selecciona el tipo' : null,
                    onChanged: (value) => setState(() => _bloodType = value),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: AppTextField(
                    controller: _units,
                    label: 'Unidades',
                    keyboardType: TextInputType.number,
                    inputFormatters: [
                      FilteringTextInputFormatter.digitsOnly,
                      LengthLimitingTextInputFormatter(2),
                    ],
                    validator: (value) {
                      final units = int.tryParse(value ?? '');

                      if (units == null || units < 1 || units > 99) {
                        return 'Ingresa una cantidad entre 1 y 99';
                      }

                      return null;
                    },
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            AppTextField(
              controller: _hospital,
              label: 'Hospital o clínica',
              validator: _required,
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: _urgency,
              decoration: const InputDecoration(
                labelText: 'Urgencia',
                border: OutlineInputBorder(),
              ),
              items: const [
                DropdownMenuItem(value: 'LOW', child: Text('Baja')),
                DropdownMenuItem(value: 'MEDIUM', child: Text('Media')),
                DropdownMenuItem(value: 'HIGH', child: Text('Alta')),
                DropdownMenuItem(value: 'CRITICAL', child: Text('Crítica')),
              ],
              onChanged: (value) {
                if (value != null) setState(() => _urgency = value);
              },
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: _pickDeadline,
              icon: const Icon(Icons.event_outlined),
              label: Text(
                _deadline == null
                    ? 'Seleccionar fecha límite'
                    : 'Fecha límite: ${formatDate(_deadline, short: true)}',
              ),
            ),
            const SizedBox(height: 24),
            const SectionHeader(title: 'Ubicación'),
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
            AppTextField(controller: _sector, label: 'Sector (opcional)'),
            const SizedBox(height: 12),
            AppTextField(
              controller: _address,
              label: 'Dirección',
              validator: _required,
            ),
            const SizedBox(height: 12),
            AppTextField(
              controller: _reference,
              label: 'Referencia (opcional)',
            ),
            const SizedBox(height: 8),
            TextButton.icon(
              onPressed: _locating ? null : _useCurrentLocation,
              icon: _locating
                  ? const SizedBox.square(
                      dimension: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.my_location),
              label: Text(
                _latitude == null
                    ? 'Usar mi ubicación actual'
                    : 'Ubicación actual agregada',
              ),
            ),
            const SizedBox(height: 24),
            const SectionHeader(title: 'Contacto'),
            const SizedBox(height: 12),
            AppTextField(
              controller: _phone,
              label: 'Teléfono de contacto',
              keyboardType: TextInputType.phone,
              validator: _required,
            ),
            const SizedBox(height: 12),
            AppTextField(
              controller: _description,
              label: 'Información adicional (opcional)',
              maxLines: 4,
            ),
            const SizedBox(height: 24),
            PrimaryButton(
              label: 'Publicar solicitud',
              isLoading: _submitting,
              onPressed: _submit,
            ),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  String? _required(String? value) =>
      (value?.trim().isEmpty ?? true) ? 'Este campo es obligatorio' : null;
}
