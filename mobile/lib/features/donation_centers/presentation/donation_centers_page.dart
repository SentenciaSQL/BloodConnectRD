import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_client.dart';
import '../../../core/networking/api_models.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../../locations/data/location_repository.dart';
import '../data/donation_center_repository.dart';

class DonationCentersPage extends ConsumerStatefulWidget {
  const DonationCentersPage({super.key});

  @override
  ConsumerState<DonationCentersPage> createState() =>
      _DonationCentersPageState();
}

class _DonationCentersPageState extends ConsumerState<DonationCentersPage> {
  late Future<List<DonationCenterModel>> _centers;

  int? _provinceId;
  int? _municipalityId;

  bool _mapMode = false;
  bool _locating = false;

  LatLng? _currentLocation;

  @override
  void initState() {
    super.initState();

    _centers = ref.read(donationCenterRepositoryProvider).list();
  }

  void _loadList() {
    final future = ref
        .read(donationCenterRepositoryProvider)
        .list(provinceId: _provinceId, municipalityId: _municipalityId);

    setState(() {
      _centers = future;
    });
  }

  Future<void> _nearby() async {
    setState(() {
      _locating = true;
    });

    try {
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();

      if (!serviceEnabled) {
        throw const PermissionDeniedException(
          'Los servicios de ubicación están desactivados.',
        );
      }

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

      final position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
        ),
      );

      final currentLocation = LatLng(position.latitude, position.longitude);

      final future = ref
          .read(donationCenterRepositoryProvider)
          .nearby(latitude: position.latitude, longitude: position.longitude);

      if (!mounted) return;

      setState(() {
        _currentLocation = currentLocation;
        _centers = future;
      });
    } on PermissionDeniedException {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'No se pudo obtener tu ubicación. Puedes seleccionar una provincia y municipio para buscar centros.',
          ),
        ),
      );
    } catch (error) {
      if (!mounted) return;

      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(friendlyError(error))));
    } finally {
      if (mounted) {
        setState(() {
          _locating = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final provinces = ref.watch(provincesProvider);

    final municipalities = _provinceId == null
        ? const AsyncValue<List<Municipality>>.data([])
        : ref.watch(municipalitiesProvider(_provinceId!));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Centros de donación'),
        actions: [
          IconButton(
            tooltip: _mapMode ? 'Ver lista' : 'Ver mapa',
            onPressed: () {
              setState(() {
                _mapMode = !_mapMode;
              });
            },
            icon: Icon(_mapMode ? Icons.list : Icons.map_outlined),
          ),
        ],
      ),
      body: Column(
        children: [
          Card(
            margin: const EdgeInsets.fromLTRB(16, 4, 16, 10),
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                children: [
                  provinces.when(
                    loading: () => const LinearProgressIndicator(),
                    error: (_, _) =>
                        const Text('No se pudieron cargar provincias'),
                    data: (items) => DropdownButtonFormField<int?>(
                      value: _provinceId,
                      decoration: const InputDecoration(
                        labelText: 'Provincia',
                        border: OutlineInputBorder(),
                      ),
                      items: [
                        const DropdownMenuItem<int?>(
                          value: null,
                          child: Text('Todas'),
                        ),
                        ...items.map(
                          (item) => DropdownMenuItem<int?>(
                            value: item.id,
                            child: Text(item.name),
                          ),
                        ),
                      ],
                      onChanged: (value) {
                        setState(() {
                          _provinceId = value;
                          _municipalityId = null;
                          _currentLocation = null;
                        });

                        _loadList();
                      },
                    ),
                  ),
                  if (_provinceId != null) ...[
                    const SizedBox(height: 8),
                    municipalities.when(
                      loading: () => const LinearProgressIndicator(),
                      error: (_, _) =>
                          const Text('No se pudieron cargar municipios'),
                      data: (items) => DropdownButtonFormField<int?>(
                        key: ValueKey(_provinceId),
                        value: _municipalityId,
                        decoration: const InputDecoration(
                          labelText: 'Municipio',
                          border: OutlineInputBorder(),
                        ),
                        items: [
                          const DropdownMenuItem<int?>(
                            value: null,
                            child: Text('Todos'),
                          ),
                          ...items.map(
                            (item) => DropdownMenuItem<int?>(
                              value: item.id,
                              child: Text(item.name),
                            ),
                          ),
                        ],
                        onChanged: (value) {
                          setState(() {
                            _municipalityId = value;
                            _currentLocation = null;
                          });

                          _loadList();
                        },
                      ),
                    ),
                  ],
                  const SizedBox(height: 4),
                  TextButton.icon(
                    onPressed: _locating ? null : _nearby,
                    icon: _locating
                        ? const SizedBox.square(
                            dimension: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.near_me_outlined),
                    label: const Text('Buscar cerca de mí'),
                  ),
                ],
              ),
            ),
          ),
          Expanded(
            child: FutureBuilder<List<DonationCenterModel>>(
              future: _centers,
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const LoadingView(message: 'Buscando centros…');
                }

                if (snapshot.hasError) {
                  return ErrorView(
                    message: friendlyError(snapshot.error!),
                    onRetry: _loadList,
                  );
                }

                final items = snapshot.data ?? const [];

                if (_mapMode) {
                  return _CentersMap(
                    centers: items,
                    currentLocation: _currentLocation,
                    onShowList: () {
                      setState(() {
                        _mapMode = false;
                      });
                    },
                  );
                }

                if (items.isEmpty) {
                  return const EmptyState(
                    title: 'No hay centros',
                    message:
                        'No encontramos centros en la ubicación seleccionada.',
                    icon: Icons.local_hospital_outlined,
                  );
                }

                return RefreshIndicator(
                  onRefresh: () async {
                    _loadList();
                    await _centers;
                  },
                  child: ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 4, 16, 96),
                    itemCount: items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (context, index) =>
                        DonationCenterCard(center: items[index]),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _CentersMap extends StatelessWidget {
  const _CentersMap({
    required this.centers,
    required this.onShowList,
    this.currentLocation,
  });

  final List<DonationCenterModel> centers;
  final VoidCallback onShowList;
  final LatLng? currentLocation;

  @override
  Widget build(BuildContext context) {
    final located = centers
        .where((center) => center.latitude != null && center.longitude != null)
        .toList();

    final LatLng? initialPosition =
        currentLocation ??
        (located.isNotEmpty
            ? LatLng(located.first.latitude!, located.first.longitude!)
            : null);

    if (initialPosition == null) {
      return EmptyState(
        title: 'Mapa no disponible',
        message: 'No hay una ubicación disponible para mostrar en el mapa.',
        icon: Icons.map_outlined,
        action: OutlinedButton.icon(
          onPressed: onShowList,
          icon: const Icon(Icons.list),
          label: const Text('Ver lista'),
        ),
      );
    }

    return GoogleMap(
      initialCameraPosition: CameraPosition(
        target: initialPosition,
        zoom: currentLocation != null ? 13 : 11,
      ),
      markers: located
          .map(
            (center) => Marker(
              markerId: MarkerId(center.id.toString()),
              position: LatLng(center.latitude!, center.longitude!),
              infoWindow: InfoWindow(
                title: center.name,
                snippet: center.address,
              ),
            ),
          )
          .toSet(),
      myLocationEnabled: currentLocation != null,
      myLocationButtonEnabled: currentLocation != null,
      mapToolbarEnabled: false,
    );
  }
}
