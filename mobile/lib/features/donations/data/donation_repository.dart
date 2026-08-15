import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/api_paths.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_client.dart';
import '../../../core/networking/api_models.dart';
import '../../auth/domain/auth_controller.dart';

class DonationRepository {
  const DonationRepository(this._api);

  final ApiClient _api;

  Future<DonationHistory> mine() async {
    final response = await _api.get('${ApiPaths.donations}/me');
    return DonationHistory.fromJson(asJson(response));
  }

  Future<DonationHistory?> mineOrNull() async {
    try {
      return await mine();
    } on NotFoundException {
      return null;
    }
  }

  Future<List<DonationResponseModel>> myResponses() async {
    final response = await _api.get('${ApiPaths.donationResponses}/me');
    return asJsonList(response).map(DonationResponseModel.fromJson).toList();
  }

  Future<DonationResponseModel> complete(int id) async {
    final response = await _api.patch('${ApiPaths.donationResponses}/$id/complete');
    return DonationResponseModel.fromJson(asJson(response));
  }

  Future<DonationResponseModel> cancel(int id) async {
    final response = await _api.patch('${ApiPaths.donationResponses}/$id/cancel');
    return DonationResponseModel.fromJson(asJson(response));
  }
}

final donationRepositoryProvider = Provider<DonationRepository>(
  (ref) => DonationRepository(ref.watch(apiClientProvider)),
);

final donationHistoryProvider = FutureProvider<DonationHistory?>((ref) async {
  final auth = ref.watch(authControllerProvider);
  if (auth.isInitializing || !auth.isAuthenticated || auth.user == null) {
    return null;
  }
  ref.watch(authControllerProvider.select((state) => state.user?.id));
  return ref.read(donationRepositoryProvider).mineOrNull();
});

final myDonationResponsesProvider =
    FutureProvider<List<DonationResponseModel>>((ref) async {
      final auth = ref.watch(authControllerProvider);
      if (auth.isInitializing || !auth.isAuthenticated || auth.user == null) {
        return const [];
      }
      ref.watch(authControllerProvider.select((state) => state.user?.id));
      return ref.read(donationRepositoryProvider).myResponses();
    });
