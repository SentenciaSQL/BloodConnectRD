import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/errors/app_exception.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../data/blood_request_repository.dart';

class MyRequestsPage extends ConsumerWidget {
  const MyRequestsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final requests = ref.watch(myRequestsProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Mis solicitudes')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/solicitudes/crear'),
        icon: const Icon(Icons.add),
        label: const Text('Nueva'),
      ),
      body: requests.when(
        loading: () => const LoadingView(),
        error: (error, _) => ErrorView(
          message: friendlyError(error),
          onRetry: () => ref.invalidate(myRequestsProvider),
        ),
        data: (items) {
          if (items.isEmpty) {
            return const EmptyState(
              title: 'Aún no tienes solicitudes',
              message: 'Publica un caso cuando necesites apoyo de donantes.',
              icon: Icons.bloodtype_outlined,
            );
          }
          return RefreshIndicator(
            onRefresh: () => ref.refresh(myRequestsProvider.future),
            child: ListView.separated(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 100),
              itemCount: items.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final request = items[index];
                return BloodRequestCard(
                  request: request,
                  onTap: () => context.push('/solicitudes/${request.id}'),
                );
              },
            ),
          );
        },
      ),
    );
  }
}
