import { DatePipe } from '@angular/common';
import {
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { interval } from 'rxjs';

import { ChatMessage, Conversation } from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import { apiErrorMessage } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import {
  EmptyStateComponent,
  LoadingSpinnerComponent,
} from '../../shared/components/ui-components';

@Component({
  selector: 'app-conversations-page',
  standalone: true,
  imports: [DatePipe, RouterLink, EmptyStateComponent, LoadingSpinnerComponent],
  template: `
    <header>
      <p class="eyebrow">Coordinación</p>
      <h1 class="font-display text-4xl font-semibold text-ink-950">Mensajes</h1>
      <p class="mt-2 text-ink-600">
        Conversaciones privadas para coordinar donaciones. El teléfono y el correo no se
        comparten automáticamente.
      </p>
    </header>

    @if (loading()) {
      <app-loading-spinner />
    } @else if (conversations().length) {
      <div class="mt-8 overflow-hidden rounded-2xl border border-ink-100 bg-white shadow-sm">
        @for (conversation of conversations(); track conversation.id) {
          <a
            [routerLink]="['/dashboard/mensajes', conversation.id]"
            class="flex w-full items-start gap-4 border-b border-ink-100 p-5 text-left last:border-0 hover:bg-ink-50"
            [class.bg-brand-50]="conversation.unreadCount > 0"
          >
            <span class="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-brand-100 font-bold text-brand-800">
              {{ initials(conversation.otherUserName) }}
            </span>
            <span class="min-w-0 flex-1">
              <span class="flex flex-wrap items-center justify-between gap-2">
                <strong class="truncate text-ink-950">{{ conversation.otherUserName }}</strong>
                @if (conversation.lastMessageAt) {
                  <small class="shrink-0 text-ink-400">
                    {{ conversation.lastMessageAt | date: 'dd/MM/yyyy HH:mm' }}
                  </small>
                }
              </span>
              <span class="mt-1 block truncate text-sm font-semibold text-ink-500">
                Solicitud de {{ conversation.bloodRequestPatientName }}
                @if (conversation.bloodRequestBloodType) {
                  · {{ conversation.bloodRequestBloodType }}
                }
                · {{ conversation.bloodRequestHospital }}
              </span>
              <span class="mt-1 block truncate text-sm text-ink-600">
                {{ conversation.lastMessage || 'Conversación iniciada. Escribe el primer mensaje.' }}
              </span>
            </span>
            @if (conversation.unreadCount > 0) {
              <span class="mt-1 rounded-full bg-brand-600 px-2.5 py-1 text-xs font-bold text-white">
                {{ conversation.unreadCount }}
              </span>
            }
          </a>
        }
      </div>
    } @else {
      <div class="mt-8">
        <app-empty-state
          title="No tienes conversaciones"
          message="Cuando pulses “Contactar” en una persona interesada, el chat aparecerá aquí."
        />
      </div>
    }
  `,
})
export class ConversationsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  readonly conversations = signal<Conversation[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.load();
    interval(15000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load(false));
  }

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  }

  private load(showSpinner = true): void {
    if (showSpinner) this.loading.set(true);
    this.api.conversations().subscribe({
      next: (conversations) => this.conversations.set(conversations),
      error: () => {
        this.loading.set(false);
        if (showSpinner) this.toast.error('No pudimos cargar tus conversaciones.');
      },
      complete: () => this.loading.set(false),
    });
  }
}

@Component({
  selector: 'app-conversation-chat-page',
  standalone: true,
  imports: [DatePipe, RouterLink, ReactiveFormsModule, LoadingSpinnerComponent],
  template: `
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <a routerLink="/dashboard/mensajes" class="text-sm font-bold text-brand-700">← Conversaciones</a>
        <h1 class="mt-2 font-display text-4xl font-semibold text-ink-950">
          {{ conversation()?.otherUserName || 'Chat' }}
        </h1>
        @if (conversation(); as item) {
          <p class="mt-2 text-ink-600">
            Solicitud de {{ item.bloodRequestPatientName }}
            @if (item.bloodRequestBloodType) {
              · {{ item.bloodRequestBloodType }}
            }
            · {{ item.bloodRequestHospital }}
          </p>
          <a
            [routerLink]="['/solicitudes', item.bloodRequestId]"
            class="mt-1 inline-block text-sm font-bold text-brand-700"
          >
            Ver solicitud
          </a>
        }
      </div>
    </header>

    @if (loading()) {
      <app-loading-spinner />
    } @else {
      <section class="mt-8 flex min-h-[28rem] flex-col overflow-hidden rounded-2xl border border-ink-100 bg-white shadow-sm">
        <div #thread class="flex-1 space-y-3 overflow-y-auto p-5" style="max-height: 28rem">
          @if (messages().length) {
            @for (message of messages(); track message.id) {
              <article
                class="max-w-[80%] rounded-2xl px-4 py-3"
                [class.ml-auto]="message.mine"
                [class.bg-brand-600]="message.mine"
                [class.text-white]="message.mine"
                [class.bg-ink-100]="!message.mine"
                [class.text-ink-950]="!message.mine"
              >
                <p class="text-xs font-semibold opacity-80">{{ message.mine ? 'Tú' : message.senderName }}</p>
                <p class="mt-1 whitespace-pre-line text-sm leading-relaxed">{{ message.body }}</p>
                <p class="mt-2 text-[11px] opacity-70">{{ message.createdAt | date: 'dd/MM/yyyy HH:mm' }}</p>
              </article>
            }
          } @else {
            <p class="text-sm text-ink-500">
              Aún no hay mensajes. Coordina aquí el lugar y la hora de la donación. No se muestra
              teléfono ni correo automáticamente.
            </p>
          }
        </div>
        <form
          class="flex gap-3 border-t border-ink-100 p-4"
          [formGroup]="form"
          (ngSubmit)="send()"
        >
          <label class="sr-only" for="chat-message">Mensaje</label>
          <textarea
            id="chat-message"
            formControlName="body"
            rows="2"
            maxlength="2000"
            class="form-control"
            placeholder="Escribe un mensaje…"
          ></textarea>
          <button type="submit" class="btn-primary self-end" [disabled]="sending() || form.invalid">
            {{ sending() ? 'Enviando…' : 'Enviar' }}
          </button>
        </form>
      </section>
    }
  `,
})
export class ConversationChatPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly thread = viewChild<ElementRef<HTMLElement>>('thread');
  readonly conversation = signal<Conversation | null>(null);
  readonly messages = signal<ChatMessage[]>([]);
  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly form = this.fb.nonNullable.group({
    body: ['', [Validators.required, Validators.maxLength(2000)]],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      void this.router.navigate(['/dashboard/mensajes']);
      return;
    }
    this.refresh(id, true);
    interval(4000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refresh(id, false));
  }

  send(): void {
    if (this.form.invalid || !this.conversation()) return;
    const body = this.form.controls.body.value.trim();
    if (!body) {
      this.form.controls.body.setValue('');
      return;
    }
    this.sending.set(true);
    this.api.sendConversationMessage(this.conversation()!.id, body).subscribe({
      next: (message) => {
        this.form.reset({ body: '' });
        this.messages.update((items) =>
          items.some((item) => item.id === message.id) ? items : [...items, message],
        );
        this.scrollToBottom();
      },
      error: (error) => {
        this.sending.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.sending.set(false),
    });
  }

  private refresh(id: number, showSpinner: boolean): void {
    if (showSpinner) this.loading.set(true);
    this.api.conversation(id).subscribe({
      next: (conversation) => this.conversation.set(conversation),
      error: () => {
        this.loading.set(false);
        if (showSpinner) {
          this.toast.error('No pudimos abrir esta conversación.');
          void this.router.navigate(['/dashboard/mensajes']);
        }
      },
    });
    this.api.conversationMessages(id).subscribe({
      next: (messages) => {
        const previousCount = this.messages().length;
        this.messages.set(messages);
        this.loading.set(false);
        this.api.markConversationRead(id).subscribe();
        if (showSpinner || messages.length !== previousCount) {
          this.scrollToBottom();
        }
      },
      error: () => this.loading.set(false),
    });
  }

  private scrollToBottom(): void {
    queueMicrotask(() => {
      const element = this.thread()?.nativeElement;
      if (element) element.scrollTop = element.scrollHeight;
    });
  }
}
