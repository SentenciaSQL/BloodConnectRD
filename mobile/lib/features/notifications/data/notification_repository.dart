import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/api_paths.dart';
import '../../../core/networking/api_client.dart';
import '../../../core/networking/api_models.dart';

class NotificationRepository {
  const NotificationRepository(this._api);

  final ApiClient _api;

  Future<List<NotificationModel>> list() async {
    final response = await _api.get(
      ApiPaths.notifications,
      queryParameters: {'size': 50, 'sort': 'createdAt', 'direction': 'desc'},
    );
    return pageContent(response).map(NotificationModel.fromJson).toList();
  }

  Future<void> markRead(int id) async {
    await _api.patch('${ApiPaths.notifications}/$id/read');
  }

  Future<void> markAllRead() async {
    await _api.patch('${ApiPaths.notifications}/read-all');
  }
}

final notificationRepositoryProvider = Provider<NotificationRepository>(
  (ref) => NotificationRepository(ref.watch(apiClientProvider)),
);

final notificationsProvider = FutureProvider<List<NotificationModel>>(
  (ref) => ref.watch(notificationRepositoryProvider).list(),
);
