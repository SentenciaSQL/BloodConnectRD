import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/api_paths.dart';
import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_client.dart';
import '../../../core/networking/api_models.dart';
import '../../auth/domain/auth_controller.dart';

class DonorRepository {
  const DonorRepository(this._api);

  final ApiClient _api;

  Future<DonorProfile> mine() async {
    final response = await _api.get('${ApiPaths.donors}/me');
    return DonorProfile.fromJson(asJson(response));
  }

  /// Devuelve null cuando el usuario autenticado aún no tiene perfil de donante.
  Future<DonorProfile?> mineOrNull() async {
    try {
      return await mine();
    } on NotFoundException {
      return null;
    }
  }

  Future<DonorProfile> create(Map<String, dynamic> payload) async {
    final response = await _api.post(ApiPaths.donors, data: payload);
    return DonorProfile.fromJson(asJson(response));
  }

  Future<DonorProfile> update(Map<String, dynamic> payload) async {
    final response = await _api.put('${ApiPaths.donors}/me', data: payload);
    return DonorProfile.fromJson(asJson(response));
  }

  Future<DonorProfile> updateAvailability(bool isAvailable) async {
    final response = await _api.patch(
      '${ApiPaths.donors}/me/availability',
      data: {
        'availability': isAvailable ? 'AVAILABLE' : 'TEMPORARILY_UNAVAILABLE',
      },
    );
    return DonorProfile.fromJson(asJson(response));
  }
}

final donorRepositoryProvider = Provider<DonorRepository>(
  (ref) => DonorRepository(ref.watch(apiClientProvider)),
);

/// Perfil del donante de la sesión actual.
/// - `null` = autenticado pero sin perfil (pendiente)
/// - error = fallo real de red/servidor (mostrar reintento)
final donorProfileProvider = FutureProvider<DonorProfile?>((ref) async {
  final auth = ref.watch(authControllerProvider);
  if (auth.isInitializing || !auth.isAuthenticated || auth.user == null) {
    return null;
  }

  // Fuerza un nuevo fetch al cambiar de usuario/sesión.
  ref.watch(authControllerProvider.select((state) => state.user?.id));
  return ref.read(donorRepositoryProvider).mineOrNull();
});
