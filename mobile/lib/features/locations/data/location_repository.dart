import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/api_paths.dart';
import '../../../core/networking/api_client.dart';
import '../../../core/networking/api_models.dart';

class LocationRepository {
  const LocationRepository(this._api);

  final ApiClient _api;

  Future<List<Province>> provinces() async {
    final response = await _api.get('${ApiPaths.locations}/provinces');
    return asJsonList(response).map(Province.fromJson).toList();
  }

  Future<List<Municipality>> municipalities(int provinceId) async {
    final response = await _api.get(
      '${ApiPaths.locations}/provinces/$provinceId/municipalities',
    );
    return asJsonList(response).map(Municipality.fromJson).toList();
  }
}

final locationRepositoryProvider = Provider<LocationRepository>(
  (ref) => LocationRepository(ref.watch(apiClientProvider)),
);

final provincesProvider = FutureProvider<List<Province>>(
  (ref) => ref.watch(locationRepositoryProvider).provinces(),
);

final municipalitiesProvider = FutureProvider.family<List<Municipality>, int>(
  (ref, provinceId) =>
      ref.watch(locationRepositoryProvider).municipalities(provinceId),
);
