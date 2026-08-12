import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/api_paths.dart';
import '../../../core/networking/api_client.dart';
import '../../../core/networking/api_models.dart';

class DonationCenterRepository {
  const DonationCenterRepository(this._api);

  final ApiClient _api;

  Future<List<DonationCenterModel>> list({
    int? provinceId,
    int? municipalityId,
  }) async {
    final response = await _api.get(
      ApiPaths.donationCenters,
      queryParameters: {
        if (provinceId != null) 'provinceId': provinceId,
        if (municipalityId != null) 'municipalityId': municipalityId,
        'size': 100,
        'sort': 'name',
        'direction': 'asc',
      },
    );
    return pageContent(response).map(DonationCenterModel.fromJson).toList();
  }

  Future<List<DonationCenterModel>> nearby({
    required double latitude,
    required double longitude,
  }) async {
    final response = await _api.get(
      '${ApiPaths.donationCenters}/nearby',
      queryParameters: {
        'latitude': latitude,
        'longitude': longitude,
        'radius': 25,
      },
    );
    return asJsonList(response).map(DonationCenterModel.fromJson).toList();
  }
}

final donationCenterRepositoryProvider = Provider<DonationCenterRepository>(
  (ref) => DonationCenterRepository(ref.watch(apiClientProvider)),
);

final donationCentersProvider = FutureProvider<List<DonationCenterModel>>(
  (ref) => ref.watch(donationCenterRepositoryProvider).list(),
);
