import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/networking/api_models.dart';
import '../../../core/utils/formatters.dart';
import '../../../shared/widgets/app_widgets.dart';
import '../data/conversation_repository.dart';

class ConversationsPage extends ConsumerWidget {
  const ConversationsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final conversations = ref.watch(conversationsProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Mensajes')),
      body: conversations.when(
        loading: () => const LoadingView(),
        error: (error, _) => ErrorView(
          message: friendlyError(error),
          onRetry: () => ref.invalidate(conversationsProvider),
        ),
        data: (items) {
          if (items.isEmpty) {
            return const EmptyState(
              title: 'Sin conversaciones',
              message:
                  'Cuando pulses “Contactar” en una persona interesada, el chat aparecerá aquí.',
              icon: Icons.chat_outlined,
            );
          }
          return RefreshIndicator(
            onRefresh: () => ref.refresh(conversationsProvider.future),
            child: ListView.separated(
              padding: const EdgeInsets.all(12),
              itemCount: items.length,
              separatorBuilder: (_, _) => const Divider(height: 1),
              itemBuilder: (context, index) {
                final item = items[index];
                return ListTile(
                  onTap: () => context.push('/mensajes/${item.id}'),
                  tileColor: item.unreadCount > 0
                      ? Theme.of(context).colorScheme.primaryContainer
                          .withValues(alpha: 0.35)
                      : null,
                  leading: Stack(
                    children: [
                      CircleAvatar(
                        child: Text(
                          item.otherUserName.isEmpty
                              ? '?'
                              : item.otherUserName[0].toUpperCase(),
                        ),
                      ),
                      if (item.unreadCount > 0)
                        Positioned(
                          right: 0,
                          top: 0,
                          child: Container(
                            width: 10,
                            height: 10,
                            decoration: BoxDecoration(
                              color: Theme.of(context).colorScheme.primary,
                              shape: BoxShape.circle,
                            ),
                          ),
                        ),
                    ],
                  ),
                  title: Text(
                    item.otherUserName,
                    style: TextStyle(
                      fontWeight: item.unreadCount > 0
                          ? FontWeight.w800
                          : FontWeight.w600,
                    ),
                  ),
                  subtitle: Text(
                    '${item.requestLabel}\n${item.preview}',
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  isThreeLine: true,
                  trailing: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        formatDateTime(item.lastMessageAt),
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                      if (item.unreadCount > 1) ...[
                        const SizedBox(height: 6),
                        CircleAvatar(
                          radius: 11,
                          backgroundColor: Theme.of(
                            context,
                          ).colorScheme.primary,
                          child: Text(
                            '${item.unreadCount}',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 11,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ),
                      ] else if (item.unreadCount == 1) ...[
                        const SizedBox(height: 8),
                        Icon(
                          Icons.circle,
                          size: 10,
                          color: Theme.of(context).colorScheme.primary,
                        ),
                      ],
                    ],
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}

class ConversationChatPage extends ConsumerStatefulWidget {
  const ConversationChatPage({super.key, required this.conversationId});

  final int conversationId;

  @override
  ConsumerState<ConversationChatPage> createState() =>
      _ConversationChatPageState();
}

class _ConversationChatPageState extends ConsumerState<ConversationChatPage> {
  final _controller = TextEditingController();
  final _scrollController = ScrollController();
  Timer? _poll;
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _markRead());
    _poll = Timer.periodic(const Duration(seconds: 4), (_) {
      ref.invalidate(conversationMessagesProvider(widget.conversationId));
    });
  }

  @override
  void dispose() {
    _poll?.cancel();
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _markRead() async {
    try {
      await ref
          .read(conversationRepositoryProvider)
          .markRead(widget.conversationId);
      ref.invalidate(conversationsProvider);
      ref.invalidate(conversationProvider(widget.conversationId));
      ref.invalidate(unreadMessageCountProvider);
    } catch (_) {
      // El historial sigue disponible aunque no se actualice el estado de lectura.
    }
  }

  Future<void> _send() async {
    final body = _controller.text.trim();
    if (body.isEmpty || _sending) return;
    setState(() => _sending = true);
    try {
      await ref
          .read(conversationRepositoryProvider)
          .send(widget.conversationId, body);
      _controller.clear();
      ref.invalidate(conversationMessagesProvider(widget.conversationId));
      ref.invalidate(conversationProvider(widget.conversationId));
      ref.invalidate(conversationsProvider);
      await _markRead();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(friendlyError(error))));
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final conversation = ref.watch(conversationProvider(widget.conversationId));
    final messages = ref.watch(
      conversationMessagesProvider(widget.conversationId),
    );
    return Scaffold(
      appBar: AppBar(
        title: conversation.when(
          data: (item) => Text(item.otherUserName),
          loading: () => const Text('Chat'),
          error: (_, _) => const Text('Chat'),
        ),
      ),
      body: Column(
        children: [
          conversation.maybeWhen(
            data: (item) => Material(
              color: Theme.of(context).colorScheme.surfaceContainerHighest,
              child: ListTile(
                dense: true,
                title: Text(item.requestLabel),
                trailing: const Icon(Icons.open_in_new, size: 18),
                onTap: () => context.push('/solicitudes/${item.bloodRequestId}'),
              ),
            ),
            orElse: () => const SizedBox.shrink(),
          ),
          Expanded(
            child: messages.when(
              loading: () => const LoadingView(),
              error: (error, _) => ErrorView(
                message: friendlyError(error),
                onRetry: () => ref.invalidate(
                  conversationMessagesProvider(widget.conversationId),
                ),
              ),
              data: (items) {
                if (items.isEmpty) {
                  return const EmptyState(
                    title: 'Sin mensajes',
                    message:
                        'Coordina aquí el lugar y la hora de la donación. No se muestra teléfono ni correo automáticamente.',
                    icon: Icons.forum_outlined,
                  );
                }
                return ListView.builder(
                  controller: _scrollController,
                  padding: const EdgeInsets.fromLTRB(12, 12, 12, 16),
                  itemCount: items.length,
                  itemBuilder: (context, index) {
                    final message = items[index];
                    return _ChatBubble(message: message);
                  },
                );
              },
            ),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _controller,
                      minLines: 1,
                      maxLines: 4,
                      maxLength: 2000,
                      decoration: const InputDecoration(
                        hintText: 'Escribe un mensaje…',
                        counterText: '',
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton.filled(
                    onPressed: _sending ? null : _send,
                    icon: _sending
                        ? const SizedBox.square(
                            dimension: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.send),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ChatBubble extends StatelessWidget {
  const _ChatBubble({required this.message});

  final ChatMessageModel message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final mine = message.mine;
    return Align(
      alignment: mine ? Alignment.centerRight : Alignment.centerLeft,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 320),
        child: Container(
          margin: const EdgeInsets.only(bottom: 10),
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(
            color: mine ? colors.primary : colors.surfaceContainerHighest,
            borderRadius: BorderRadius.circular(16),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                mine ? 'Tú' : message.senderName,
                style: TextStyle(
                  color: mine ? colors.onPrimary : colors.onSurface,
                  fontWeight: FontWeight.w800,
                  fontSize: 12,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                message.body,
                style: TextStyle(
                  color: mine ? colors.onPrimary : colors.onSurface,
                ),
              ),
              if (message.createdAt != null) ...[
                const SizedBox(height: 6),
                Text(
                  mine
                      ? '${formatDateTime(message.createdAt)}  ${message.statusMark}'
                      : formatDateTime(message.createdAt),
                  style: TextStyle(
                    color: (mine ? colors.onPrimary : colors.onSurface)
                        .withValues(alpha: message.status == 'READ' ? 1 : 0.72),
                    fontSize: 11,
                    fontWeight: message.status == 'READ'
                        ? FontWeight.w700
                        : FontWeight.w400,
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
