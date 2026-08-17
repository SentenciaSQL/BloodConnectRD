import 'package:equatable/equatable.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/api_paths.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_client.dart';
import '../../../core/networking/api_models.dart';
import '../../auth/domain/auth_controller.dart';

class BloodRequestFilters extends Equatable {
  const BloodRequestFilters({
    this.bloodType,
    this.provinceId,
    this.municipalityId,
    this.urgency,
    this.sort = 'createdAt',
    this.direction = 'desc',
  });

  final String? bloodType;
  final int? provinceId;
  final int? municipalityId;
  final String? urgency;
  final String sort;
  final String direction;

  Map<String, dynamic> toQuery() => {
    if (bloodType != null) 'bloodType': bloodType,
    if (provinceId != null) 'provinceId': provinceId,
    if (municipalityId != null) 'municipalityId': municipalityId,
    if (urgency != null) 'urgency': urgency,
    'status': ['OPEN', 'IN_PROGRESS'],
    'size': 50,
    'sort': sort,
    'direction': direction,
  };

  BloodRequestFilters copyWith({
    String? bloodType,
    bool clearBloodType = false,
    int? provinceId,
    bool clearProvince = false,
    int? municipalityId,
    bool clearMunicipality = false,
    String? urgency,
    bool clearUrgency = false,
    String? sort,
    String? direction,
  }) {
    return BloodRequestFilters(
      bloodType: clearBloodType ? null : (bloodType ?? this.bloodType),
      provinceId: clearProvince ? null : (provinceId ?? this.provinceId),
      municipalityId: clearMunicipality
          ? null
          : (municipalityId ?? this.municipalityId),
      urgency: clearUrgency ? null : (urgency ?? this.urgency),
      sort: sort ?? this.sort,
      direction: direction ?? this.direction,
    );
  }

  @override
  List<Object?> get props => [
    bloodType,
    provinceId,
    municipalityId,
    urgency,
    sort,
    direction,
  ];
}

class BloodRequestRepository {
  const BloodRequestRepository(this._api);

  final ApiClient _api;

  Future<List<BloodRequestModel>> list(BloodRequestFilters filters) async {
    final response = await _api.get(
      ApiPaths.bloodRequests,
      queryParameters: filters.toQuery(),
    );
    return pageContent(response).map(BloodRequestModel.fromJson).toList();
  }

  Future<List<BloodRequestModel>> urgent({int size = 5}) async {
    final response = await _api.get(
      '${ApiPaths.bloodRequests}/urgent',
      queryParameters: {'size': size, 'sort': 'urgency', 'direction': 'desc'},
    );
    return pageContent(response).map(BloodRequestModel.fromJson).toList();
  }

  Future<List<BloodRequestModel>> compatible({int size = 5}) async {
    final response = await _api.get(
      '${ApiPaths.bloodRequests}/compatible',
      queryParameters: {'size': size, 'sort': 'urgency', 'direction': 'desc'},
    );
    return pageContent(response).map(BloodRequestModel.fromJson).toList();
  }

  Future<List<BloodRequestModel>> nearby({
    required double latitude,
    required double longitude,
    double radius = 25,
  }) async {
    final response = await _api.get(
      '${ApiPaths.bloodRequests}/nearby',
      queryParameters: {
        'latitude': latitude,
        'longitude': longitude,
        'radius': radius,
      },
    );
    return asJsonList(response).map(BloodRequestModel.fromJson).toList();
  }

  Future<BloodRequestModel> get(int id) async {
    final response = await _api.get('${ApiPaths.bloodRequests}/$id');
    return BloodRequestModel.fromJson(asJson(response));
  }

  Future<BloodRequestModel> create(Map<String, dynamic> payload) async {
    final response = await _api.post(ApiPaths.bloodRequests, data: payload);
    return BloodRequestModel.fromJson(asJson(response));
  }

  Future<void> offerHelp(int requestId, {String? message}) async {
    await _api.post(
      '${ApiPaths.bloodRequests}/$requestId/responses',
      data: {'message': message},
    );
  }

  Future<List<DonationModel>> donations(int requestId) async {
    final response = await _api.get('${ApiPaths.bloodRequests}/$requestId/donations');
    return asJsonList(response).map(DonationModel.fromJson).toList();
  }

  Future<List<BloodRequestModel>> mine({int size = 50}) async {
    final response = await _api.get(
      '${ApiPaths.bloodRequests}/my',
      queryParameters: {'page': 0, 'size': size, 'sort': 'createdAt', 'direction': 'desc'},
    );
    return pageContent(response).map(BloodRequestModel.fromJson).toList();
  }

  Future<DonationModel> reportDonation(
    int requestId, {
    required int units,
    DateTime? donationDate,
    String? notes,
  }) async {
    final date = donationDate ?? DateTime.now();
    final isoDate =
        '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
    final response = await _api.post(
      '${ApiPaths.bloodRequests}/$requestId/donations',
      data: {
        'units': units,
        'donationDate': isoDate,
        if (notes != null && notes.isNotEmpty) 'notes': notes,
      },
    );
    return DonationModel.fromJson(asJson(response));
  }

  Future<DonationModel> confirmDonation(
    int donationId, {
    required int confirmedUnits,
  }) async {
    final response = await _api.patch(
      '${ApiPaths.donations}/$donationId/confirm',
      data: {'confirmedUnits': confirmedUnits},
    );
    return DonationModel.fromJson(asJson(response));
  }
}

final bloodRequestRepositoryProvider = Provider<BloodRequestRepository>(
  (ref) => BloodRequestRepository(ref.watch(apiClientProvider)),
);

final bloodRequestsProvider =
    FutureProvider.family<List<BloodRequestModel>, BloodRequestFilters>(
      (ref, filters) => ref.watch(bloodRequestRepositoryProvider).list(filters),
    );

final urgentRequestsProvider = FutureProvider<List<BloodRequestModel>>(
  (ref) => ref.watch(bloodRequestRepositoryProvider).urgent(),
);

final compatibleRequestsProvider = FutureProvider<List<BloodRequestModel>>((
  ref,
) async {
  final auth = ref.watch(authControllerProvider);
  if (auth.isInitializing || !auth.isAuthenticated || auth.user == null) {
    return const [];
  }
  try {
    return await ref.watch(bloodRequestRepositoryProvider).compatible();
  } on NotFoundException {
    return const [];
  } on ForbiddenException {
    return const [];
  }
});


final bloodRequestDetailProvider =
    FutureProvider.family<BloodRequestModel, int>(
      (ref, id) => ref.watch(bloodRequestRepositoryProvider).get(id),
    );

final myRequestsProvider = FutureProvider<List<BloodRequestModel>>((ref) async {
  final auth = ref.watch(authControllerProvider);
  if (auth.isInitializing || !auth.isAuthenticated) return const [];
  return ref.watch(bloodRequestRepositoryProvider).mine();
});

final requestDonationsProvider = FutureProvider.family<List<DonationModel>, int>(
  (ref, id) async {
    final auth = ref.watch(authControllerProvider);
    if (!auth.isAuthenticated) return const [];
    try {
      return await ref.watch(bloodRequestRepositoryProvider).donations(id);
    } on ForbiddenException {
      return const [];
    } on NotFoundException {
      return const [];
    }
  },
);

void invalidateBloodRequestCaches(Ref ref, {int? requestId}) {
  ref.invalidate(bloodRequestsProvider);
  ref.invalidate(urgentRequestsProvider);
  ref.invalidate(compatibleRequestsProvider);
  ref.invalidate(myRequestsProvider);
  if (requestId != null) {
    ref.invalidate(bloodRequestDetailProvider(requestId));
    ref.invalidate(requestDonationsProvider(requestId));
  }
}
