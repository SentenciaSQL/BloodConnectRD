import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/constants/api_paths.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_models.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../../locations/data/location_repository.dart';
import '../data/blood_request_repository.dart';

class BloodRequestsPage extends ConsumerStatefulWidget {
  const BloodRequestsPage({super.key});

  @override
  ConsumerState<BloodRequestsPage> createState() => _BloodRequestsPageState();
}

class _BloodRequestsPageState extends ConsumerState<BloodRequestsPage> {
  BloodRequestFilters _filters = const BloodRequestFilters();
  bool _showFilters = false;

  @override
  Widget build(BuildContext context) {
    final requests = ref.watch(bloodRequestsProvider(_filters));
    return Scaffold(
      appBar: AppBar(
        title: const Text('Solicitudes de sangre'),
        actions: [
          IconButton(
            tooltip: 'Mis solicitudes',
            onPressed: () => context.push('/mis-solicitudes'),
            icon: const Icon(Icons.assignment_outlined),
          ),
          IconButton(
            tooltip: 'Crear solicitud',
            onPressed: () => context.push('/crear-solicitud'),
            icon: const Icon(Icons.add_circle_outline),
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
            child: Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () =>
                        setState(() => _showFilters = !_showFilters),
                    icon: const Icon(Icons.tune),
                    label: const Text('Filtros'),
                  ),
                ),
                const SizedBox(width: 8),
                PopupMenuButton<String>(
                  tooltip: 'Ordenar solicitudes',
                  onSelected: _setSort,
                  itemBuilder: (context) => const [
                    PopupMenuItem(
                      value: 'recent',
                      child: Text('Más recientes'),
                    ),
                    PopupMenuItem(
                      value: 'deadline',
                      child: Text('Fecha límite más cercana'),
                    ),
                    PopupMenuItem(
                      value: 'urgency',
                      child: Text('Mayor urgencia'),
                    ),
                  ],
                  child: const Padding(
                    padding: EdgeInsets.all(12),
                    child: Row(
                      children: [
                        Icon(Icons.sort),
                        SizedBox(width: 6),
                        Text('Ordenar'),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          if (_showFilters) _Filters(filters: _filters, onChanged: _setFilters),
          Expanded(
            child: requests.when(
              loading: () =>
                  const LoadingView(message: 'Buscando solicitudes…'),
              error: (error, _) => ErrorView(
                message: friendlyError(error),
                onRetry: () => ref.invalidate(bloodRequestsProvider(_filters)),
              ),
              data: (items) {
                if (items.isEmpty) {
                  return const EmptyState(
                    title: 'No hay solicitudes',
                    message:
                        'No encontramos solicitudes con los filtros seleccionados.',
                    icon: Icons.bloodtype_outlined,
                  );
                }
                return RefreshIndicator(
                  onRefresh: () =>
                      ref.refresh(bloodRequestsProvider(_filters).future),
                  child: ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 4, 16, 96),
                    itemCount: items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (context, index) => RequestCard(
                      request: items[index],
                      onTap: () =>
                          context.push('/detalle-solicitud/${items[index].id}'),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  void _setFilters(BloodRequestFilters filters) {
    setState(() => _filters = filters);
  }

  void _setSort(String value) {
    setState(() {
      _filters = switch (value) {
        'deadline' => _filters.copyWith(sort: 'deadline', direction: 'asc'),
        'urgency' => _filters.copyWith(sort: 'urgency', direction: 'desc'),
        _ => _filters.copyWith(sort: 'createdAt', direction: 'desc'),
      };
    });
  }
}

class _Filters extends ConsumerWidget {
  const _Filters({required this.filters, required this.onChanged});

  final BloodRequestFilters filters;
  final ValueChanged<BloodRequestFilters> onChanged;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final provinces = ref.watch(provincesProvider);
    final municipalities = filters.provinceId == null
        ? const AsyncValue<List<Municipality>>.data([])
        : ref.watch(municipalitiesProvider(filters.provinceId!));
    return Card(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 12),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: DropdownButtonFormField<String?>(
                    value: filters.bloodType,
                    decoration: const InputDecoration(
                      labelText: 'Tipo de sangre',
                      border: OutlineInputBorder(),
                    ),
                    items: [
                      const DropdownMenuItem(value: null, child: Text('Todos')),
                      ...BloodTypes.values.map(
                        (type) =>
                            DropdownMenuItem(value: type, child: Text(type)),
                      ),
                    ],
                    onChanged: (value) => onChanged(
                      filters.copyWith(
                        bloodType: value,
                        clearBloodType: value == null,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: DropdownButtonFormField<String?>(
                    value: filters.urgency,
                    decoration: const InputDecoration(
                      labelText: 'Urgencia',
                      border: OutlineInputBorder(),
                    ),
                    items: const [
                      DropdownMenuItem(value: null, child: Text('Todas')),
                      DropdownMenuItem(value: 'LOW', child: Text('Baja')),
                      DropdownMenuItem(value: 'MEDIUM', child: Text('Media')),
                      DropdownMenuItem(value: 'HIGH', child: Text('Alta')),
                      DropdownMenuItem(
                        value: 'CRITICAL',
                        child: Text('Crítica'),
                      ),
                    ],
                    onChanged: (value) => onChanged(
                      filters.copyWith(
                        urgency: value,
                        clearUrgency: value == null,
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            provinces.when(
              loading: () => const LinearProgressIndicator(),
              error: (_, _) => const Text('No se pudieron cargar provincias'),
              data: (items) => DropdownButtonFormField<int?>(
                value: filters.provinceId,
                decoration: const InputDecoration(
                  labelText: 'Provincia',
                  border: OutlineInputBorder(),
                ),
                items: [
                  const DropdownMenuItem(value: null, child: Text('Todas')),
                  ...items.map(
                    (item) => DropdownMenuItem(
                      value: item.id,
                      child: Text(item.name),
                    ),
                  ),
                ],
                onChanged: (value) => onChanged(
                  filters.copyWith(
                    provinceId: value,
                    clearProvince: value == null,
                    clearMunicipality: true,
                  ),
                ),
              ),
            ),
            if (filters.provinceId != null) ...[
              const SizedBox(height: 8),
              municipalities.when(
                loading: () => const LinearProgressIndicator(),
                error: (_, _) => const Text('No se pudieron cargar municipios'),
                data: (items) => DropdownButtonFormField<int?>(
                  value: filters.municipalityId,
                  decoration: const InputDecoration(
                    labelText: 'Municipio',
                    border: OutlineInputBorder(),
                  ),
                  items: [
                    const DropdownMenuItem(value: null, child: Text('Todos')),
                    ...items.map(
                      (item) => DropdownMenuItem(
                        value: item.id,
                        child: Text(item.name),
                      ),
                    ),
                  ],
                  onChanged: (value) => onChanged(
                    filters.copyWith(
                      municipalityId: value,
                      clearMunicipality: value == null,
                    ),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
