import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_models.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../data/notification_repository.dart';

class NotificationsPage extends ConsumerWidget {
  const NotificationsPage({super.key});

  Future<void> _markAll(BuildContext context, WidgetRef ref) async {
    try {
      await ref.read(notificationRepositoryProvider).markAllRead();
      ref.invalidate(notificationsProvider);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Todas fueron marcadas como leídas.')),
        );
      }
    } catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(friendlyError(error))));
      }
    }
  }

  Future<void> _open(
    BuildContext context,
    WidgetRef ref,
    NotificationModel item,
  ) async {
    if (!item.isRead) {
      try {
        await ref.read(notificationRepositoryProvider).markRead(item.id);
        ref.invalidate(notificationsProvider);
      } catch (_) {
        // El contenido sigue disponible aunque no se pueda actualizar el estado.
      }
    }
    if (!context.mounted) return;
    if (item.resourceType == 'CONVERSATION' && item.resourceId != null) {
      context.push('/mensajes/${item.resourceId}');
      return;
    }
    if (item.resourceType == 'BLOOD_REQUEST' && item.resourceId != null) {
      context.push('/solicitudes/${item.resourceId}');
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notifications = ref.watch(notificationsProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notificaciones'),
        actions: [
          TextButton(
            onPressed: () => _markAll(context, ref),
            child: const Text('Leer todas'),
          ),
        ],
      ),
      body: notifications.when(
        loading: () => const LoadingView(),
        error: (error, _) => ErrorView(
          message: friendlyError(error),
          onRetry: () => ref.invalidate(notificationsProvider),
        ),
        data: (items) {
          if (items.isEmpty) {
            return const EmptyState(
              title: 'Sin notificaciones',
              message: 'Aquí verás novedades sobre solicitudes y donaciones.',
              icon: Icons.notifications_none,
            );
          }
          return RefreshIndicator(
            onRefresh: () => ref.refresh(notificationsProvider.future),
            child: ListView.separated(
              padding: const EdgeInsets.all(12),
              itemCount: items.length,
              separatorBuilder: (_, _) => const Divider(height: 1),
              itemBuilder: (context, index) {
                final item = items[index];
                return ListTile(
                  onTap: () => _open(context, ref, item),
                  leading: CircleAvatar(
                    backgroundColor: item.isRead
                        ? Theme.of(context).colorScheme.surfaceContainerHighest
                        : Theme.of(context).colorScheme.primaryContainer,
                    child: Icon(
                      item.isRead
                          ? Icons.notifications_outlined
                          : Icons.notifications_active,
                    ),
                  ),
                  title: Text(
                    item.title,
                    style: TextStyle(
                      fontWeight: item.isRead
                          ? FontWeight.normal
                          : FontWeight.w700,
                    ),
                  ),
                  subtitle: Text(
                    '${item.message}\n${formatDate(item.createdAt, short: true)}',
                  ),
                  isThreeLine: true,
                  trailing: item.resourceId == null
                      ? null
                      : const Icon(Icons.chevron_right),
                );
              },
            ),
          );
        },
      ),
    );
  }
}
