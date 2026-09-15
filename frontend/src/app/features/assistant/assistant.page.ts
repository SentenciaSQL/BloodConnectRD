import { isPlatformBrowser } from '@angular/common';
import { Component, ElementRef, OnInit, PLATFORM_ID, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AssistantAskResponse, AssistantLink } from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import { apiErrorMessage } from '../../core/services/auth.service';

interface ChatTurn {
  role: 'user' | 'assistant';
  text: string;
  links: AssistantLink[];
}

@Component({
  selector: 'app-assistant-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <header>
      <p class="eyebrow">Asistente</p>
      <h1 class="font-display text-4xl font-semibold text-ink-950">Preguntas sobre sangre</h1>
      <p class="mt-2 max-w-2xl text-ink-600">
        Habla en español. El asistente consulta solicitudes activas, centros cercanos y
        compatibilidad ABO/Rh con las mismas reglas de BloodConnect RD. No crea solicitudes ni
        sustituye una evaluación médica.
      </p>
    </header>

    @if (statusLoading()) {
      <p class="mt-8 text-sm text-ink-500">Comprobando el asistente…</p>
    } @else if (!enabled()) {
      <div class="mt-8 rounded-2xl border border-amber-200 bg-amber-50 p-6 text-amber-950">
        <p class="font-bold">El asistente no está activo en este servidor.</p>
        <p class="mt-2 text-sm leading-relaxed">
          Quien opera el backend debe definir <code>ASSISTANT_ENABLED=true</code> y reiniciar Spring
          Boot. El navegador nunca habla el protocolo MCP.
        </p>
      </div>
    } @else {
      <section class="mt-8 overflow-hidden rounded-2xl border border-ink-100 bg-white shadow-sm">
        <div #thread class="max-h-[28rem] space-y-4 overflow-y-auto p-5">
          @for (turn of turns(); track $index) {
            <article
              class="max-w-[85%] whitespace-pre-wrap rounded-2xl px-4 py-3 text-sm leading-relaxed"
              [class.ml-auto]="turn.role === 'user'"
              [class.bg-brand-600]="turn.role === 'user'"
              [class.text-white]="turn.role === 'user'"
              [class.bg-ink-50]="turn.role === 'assistant'"
              [class.text-ink-900]="turn.role === 'assistant'"
            >
              <p>{{ turn.text }}</p>
              @if (turn.links.length) {
                <div class="mt-3 flex flex-wrap gap-2">
                  @for (link of turn.links; track link.path) {
                    <a
                      [routerLink]="link.path"
                      class="rounded-full bg-white px-3 py-1 text-xs font-bold text-brand-700 ring-1 ring-brand-200"
                    >
                      {{ link.label }} →
                    </a>
                  }
                </div>
              }
            </article>
          }
        </div>

        <div class="flex flex-wrap gap-2 border-t border-ink-100 px-4 py-3">
          @for (prompt of prompts; track prompt) {
            <button
              type="button"
              class="rounded-full border border-ink-200 px-3 py-1.5 text-xs font-semibold text-ink-700 hover:border-brand-300 hover:text-brand-700"
              [disabled]="sending()"
              (click)="ask(prompt)"
            >
              {{ prompt }}
            </button>
          }
        </div>

        <form class="grid gap-3 border-t border-ink-100 p-4" [formGroup]="form" (ngSubmit)="submit()">
          <label class="flex items-center gap-2 text-sm font-semibold text-ink-700">
            <input type="checkbox" formControlName="includeLocation" class="h-4 w-4 rounded border-ink-300" />
            Incluir mi ubicación (para centros cercanos)
          </label>
          <div class="flex gap-3">
            <label class="sr-only" for="assistant-message">Pregunta</label>
            <textarea
              id="assistant-message"
              formControlName="message"
              rows="2"
              maxlength="500"
              class="form-control"
              placeholder="Ej. ¿O- puede donar a A+?"
            ></textarea>
            <button type="submit" class="btn-primary self-end" [disabled]="sending() || form.invalid">
              {{ sending() ? 'Consultando…' : 'Preguntar' }}
            </button>
          </div>
          @if (error()) {
            <p class="form-error">{{ error() }}</p>
          }
        </form>
      </section>
    }
  `,
})
export class AssistantPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly thread = viewChild<ElementRef<HTMLElement>>('thread');

  readonly enabled = signal(false);
  readonly statusLoading = signal(true);
  readonly sending = signal(false);
  readonly error = signal<string | null>(null);
  readonly turns = signal<ChatTurn[]>([
    {
      role: 'assistant',
      text:
        'Hola. Puedo ayudarte con solicitudes activas, centros de donación cercanos y compatibilidad ABO/Rh. La información es orientativa.',
      links: [],
    },
  ]);
  readonly prompts = [
    '¿O- puede donar a A+?',
    'Solicitudes de O+ urgentes',
    'Centros cerca de mí',
    'Ayuda',
  ];
  readonly form = this.fb.nonNullable.group({
    message: ['', [Validators.required, Validators.maxLength(500)]],
    includeLocation: [false],
  });

  ngOnInit(): void {
    this.api.assistantStatus().subscribe({
      next: (status) => {
        this.enabled.set(status.enabled);
        this.statusLoading.set(false);
      },
      error: () => {
        this.enabled.set(false);
        this.statusLoading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    const message = this.form.controls.message.value.trim();
    if (!message) {
      this.form.controls.message.setValue('');
      return;
    }
    this.ask(message);
  }

  ask(message: string): void {
    if (this.sending() || !this.enabled()) return;
    this.error.set(null);
    this.sending.set(true);
    this.turns.update((turns) => [...turns, { role: 'user', text: message, links: [] }]);
    this.form.patchValue({ message: '' });
    this.scrollToBottom();

    const send = (coordinates: { latitude: number; longitude: number } | null) => {
      this.api.askAssistant(message, coordinates).subscribe({
        next: (response) => this.appendAssistant(response),
        error: (err) => {
          this.sending.set(false);
          this.error.set(apiErrorMessage(err));
        },
      });
    };

    if (this.form.controls.includeLocation.value && this.isBrowser && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) =>
          send({ latitude: position.coords.latitude, longitude: position.coords.longitude }),
        () => send(null),
        { enableHighAccuracy: false, timeout: 8000 },
      );
      return;
    }
    send(null);
  }

  private appendAssistant(response: AssistantAskResponse): void {
    this.turns.update((turns) => [
      ...turns,
      { role: 'assistant', text: response.reply, links: response.links ?? [] },
    ]);
    this.sending.set(false);
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    queueMicrotask(() => {
      const element = this.thread()?.nativeElement;
      if (element) element.scrollTop = element.scrollHeight;
    });
  }
}
