import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_models.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../data/blood_request_repository.dart';

class MyRequestsPage extends ConsumerStatefulWidget {
  const MyRequestsPage({super.key});

  @override
  ConsumerState<MyRequestsPage> createState() => _MyRequestsPageState();
}

class _MyRequestsPageState extends ConsumerState<MyRequestsPage> {
  Future<void> _confirmDelete(BloodRequestModel request) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('Eliminar solicitud'),
          content: Text(
            '¿Deseas eliminar la solicitud de '
            '${request.patientName}?',
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(dialogContext).pop(false);
              },
              child: const Text('Cancelar'),
            ),
            FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.error,
              ),
              onPressed: () {
                Navigator.of(dialogContext).pop(true);
              },
              child: const Text('Eliminar'),
            ),
          ],
        );
      },
    );

    if (confirmed != true || !mounted) {
      return;
    }

    try {
      await ref.read(bloodRequestRepositoryProvider).delete(request.id);

      invalidateBloodRequestCaches(ref, requestId: request.id);

      if (!mounted) {
        return;
      }

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Solicitud eliminada correctamente')),
      );
    } catch (error) {
      if (!mounted) {
        return;
      }

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(friendlyError(error)),
          backgroundColor: Theme.of(context).colorScheme.error,
        ),
      );
    }
  }

  bool _canModify(BloodRequestModel request) {
    return request.status == 'OPEN' || request.status == 'IN_PROGRESS';
  }

  @override
  Widget build(BuildContext context) {
    final requests = ref.watch(myRequestsProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          tooltip: 'Volver',
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            if (context.canPop()) {
              context.pop();
            } else {
              context.go('/');
            }
          },
        ),
        title: const Text('Mis solicitudes'),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {
          context.push('/crear-solicitud');
        },
        icon: const Icon(Icons.add),
        label: const Text('Nueva'),
      ),
      body: requests.when(
        loading: () => const LoadingView(),
        error: (error, _) => ErrorView(
          message: friendlyError(error),
          onRetry: () {
            ref.invalidate(myRequestsProvider);
          },
        ),
        data: (items) {
          final activeItems = items.where((request) {
            final hasActiveStatus =
                request.status == 'OPEN' || request.status == 'IN_PROGRESS';

            return !request.isExpired && hasActiveStatus;
          }).toList();

          if (activeItems.isEmpty) {
            return const EmptyState(
              title: 'Aún no tienes solicitudes activas',
              message:
                  'Publica un caso cuando necesites apoyo '
                  'de donantes.',
              icon: Icons.bloodtype_outlined,
            );
          }

          return RefreshIndicator(
            onRefresh: () async {
              ref.invalidate(myRequestsProvider);
              await ref.read(myRequestsProvider.future);
            },
            child: ListView.separated(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 100),
              itemCount: activeItems.length,
              separatorBuilder: (_, _) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                final request = activeItems[index];

                return Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    RequestCard(
                      request: request,
                      onTap: () {
                        context.push('/detalle-solicitud/${request.id}');
                      },
                    ),
                    if (_canModify(request)) ...[
                      const SizedBox(height: 4),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          TextButton.icon(
                            onPressed: () {
                              context.push(
                                '/editar-solicitud/'
                                '${request.id}',
                              );
                            },
                            icon: const Icon(Icons.edit_outlined),
                            label: const Text('Actualizar'),
                          ),
                          const SizedBox(width: 8),
                          TextButton.icon(
                            onPressed: () {
                              _confirmDelete(request);
                            },
                            style: TextButton.styleFrom(
                              foregroundColor: Theme.of(
                                context,
                              ).colorScheme.error,
                            ),
                            icon: const Icon(Icons.delete_outline),
                            label: const Text('Eliminar'),
                          ),
                        ],
                      ),
                    ],
                  ],
                );
              },
            ),
          );
        },
      ),
    );
  }
}
