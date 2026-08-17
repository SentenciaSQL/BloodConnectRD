import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FAQ_QUESTIONS } from '../../core/seo/content';

@Component({
  selector: 'app-faq-page',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="bg-hero-mesh px-5 py-20 text-center">
      <div class="mx-auto max-w-3xl">
        <p class="eyebrow">Respuestas claras</p>
        <h1 class="font-display text-5xl font-semibold text-ink-950">Preguntas frecuentes</h1>
        <p class="mt-4 text-ink-600">
          Información sobre BloodConnect RD y el proceso general de donación.
        </p>
      </div>
    </section>

    <section class="section-shell !max-w-4xl">
      <div class="grid gap-3">
        @for (item of questions; track item.question; let index = $index) {
          <article class="overflow-hidden rounded-xl border border-ink-200 bg-white">
            <button
              type="button"
              class="flex w-full items-center justify-between gap-5 p-5 text-left font-bold text-ink-950"
              (click)="toggle(index)"
              [attr.aria-expanded]="open() === index"
            >
              {{ item.question }}
              <span class="text-brand-600">{{ open() === index ? '−' : '+' }}</span>
            </button>
            @if (open() === index) {
              <p class="border-t border-ink-100 px-5 py-5 text-sm leading-relaxed text-ink-600">
                {{ item.answer }}
              </p>
            }
          </article>
        }
      </div>

      <p class="mt-6 text-sm text-ink-600">
        Si quieres borrar tu cuenta,
        <a routerLink="/eliminacion-de-cuenta" class="font-bold text-brand-700">consulta cómo solicitar la eliminación</a>.
      </p>

      <div class="mt-12 rounded-3xl bg-ink-950 p-8 text-white sm:flex sm:items-center sm:justify-between sm:gap-8">
        <div>
          <h2 class="font-display text-2xl font-semibold">¿Listo para ayudar?</h2>
          <p class="mt-2 text-sm text-ink-300">Crea tu perfil y mantén tu disponibilidad actualizada.</p>
        </div>
        <a routerLink="/registro" class="btn-primary mt-6 sm:mt-0">Crear cuenta</a>
      </div>
    </section>
  `,
})
export class FaqPage {
  readonly open = signal<number | null>(0);
  readonly questions = FAQ_QUESTIONS;

  toggle(index: number): void {
    this.open.update((current) => (current === index ? null : index));
  }
}
