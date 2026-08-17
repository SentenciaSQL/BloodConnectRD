import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/api_paths.dart';
import '../../../core/networking/api_client.dart';
import '../../../core/networking/api_models.dart';
import '../../auth/domain/auth_controller.dart';

class ConversationRepository {
  const ConversationRepository(this._api);

  final ApiClient _api;

  Future<ConversationModel> open({
    required int requestId,
    required int donorUserId,
  }) async {
    final response = await _api.post(
      '${ApiPaths.bloodRequests}/$requestId/conversations',
      data: {'donorUserId': donorUserId},
    );
    return ConversationModel.fromJson(asJson(response));
  }

  Future<List<ConversationModel>> list() async {
    final response = await _api.get(ApiPaths.conversations);
    return asJsonList(response).map(ConversationModel.fromJson).toList();
  }

  Future<ConversationModel> get(int id) async {
    final response = await _api.get('${ApiPaths.conversations}/$id');
    return ConversationModel.fromJson(asJson(response));
  }

  Future<List<ChatMessageModel>> messages(int id) async {
    final response = await _api.get('${ApiPaths.conversations}/$id/messages');
    return asJsonList(response).map(ChatMessageModel.fromJson).toList();
  }

  Future<ChatMessageModel> send(int id, String body) async {
    final response = await _api.post(
      '${ApiPaths.conversations}/$id/messages',
      data: {'body': body},
    );
    return ChatMessageModel.fromJson(asJson(response));
  }

  Future<ConversationModel> markRead(int id) async {
    final response = await _api.put('${ApiPaths.conversations}/$id/read');
    return ConversationModel.fromJson(asJson(response));
  }

  Future<int> unreadCount() async {
    final response = await _api.get('${ApiPaths.messages}/unread-count');
    return (asJson(response)['unreadCount'] as num?)?.toInt() ?? 0;
  }
}

final conversationRepositoryProvider = Provider<ConversationRepository>(
  (ref) => ConversationRepository(ref.watch(apiClientProvider)),
);

final conversationsProvider = FutureProvider<List<ConversationModel>>((
  ref,
) async {
  final auth = ref.watch(authControllerProvider);
  if (auth.isInitializing || !auth.isAuthenticated) return const [];
  final items = await ref.watch(conversationRepositoryProvider).list();
  final timer = Timer(const Duration(seconds: 12), ref.invalidateSelf);
  ref.onDispose(timer.cancel);
  return items;
});

final conversationProvider = FutureProvider.family<ConversationModel, int>(
  (ref, id) => ref.watch(conversationRepositoryProvider).get(id),
);

final conversationMessagesProvider =
    FutureProvider.family<List<ChatMessageModel>, int>(
      (ref, id) => ref.watch(conversationRepositoryProvider).messages(id),
    );

final unreadMessageCountProvider = FutureProvider<int>((ref) async {
  final auth = ref.watch(authControllerProvider);
  if (auth.isInitializing || !auth.isAuthenticated) return 0;
  final count = await ref.watch(conversationRepositoryProvider).unreadCount();
  final timer = Timer(const Duration(seconds: 12), ref.invalidateSelf);
  ref.onDispose(timer.cancel);
  return count;
});
