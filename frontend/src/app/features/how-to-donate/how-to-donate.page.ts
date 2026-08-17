import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { HOW_TO_STEPS } from '../../core/seo/content';

@Component({
  selector: 'app-how-to-donate-page',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="bg-ink-950 px-5 py-20 text-white">
      <div class="mx-auto grid max-w-7xl items-center gap-12 lg:grid-cols-2 lg:px-3">
        <div>
          <p class="eyebrow !text-brand-300">Prepárate con confianza</p>
          <h1 class="font-display text-5xl font-semibold">Cómo donar sangre</h1>
          <p class="mt-5 max-w-xl text-lg leading-relaxed text-ink-300">
            Una guía práctica para antes, durante y después de tu visita a un centro de donación.
          </p>
          <a routerLink="/centros" class="btn-primary mt-8">Encontrar un centro</a>
        </div>
        <img
          src="/images/how-to-donate.jpg"
          alt="Proceso seguro de donación de sangre en un centro de salud"
          width="1200"
          height="677"
          loading="lazy"
          decoding="async"
          class="min-h-80 w-full rounded-3xl object-cover"
        />
      </div>
    </section>

    <section class="section-shell">
      <h2 class="section-title">Antes de donar</h2>
      <div class="mt-9 grid gap-8 md:grid-cols-3">
        @for (item of before; track item.title) {
          <div class="border-t-2 border-brand-500 pt-5">
            <h3 class="text-lg font-bold">{{ item.title }}</h3>
            <p class="mt-2 text-sm leading-relaxed text-ink-600">{{ item.copy }}</p>
          </div>
        }
      </div>
    </section>

    <section class="bg-white py-20">
      <div class="mx-auto max-w-5xl px-5 lg:px-8">
        <p class="eyebrow">Tu visita</p>
        <h2 class="section-title">Qué puedes esperar</h2>
        <ol class="mt-10 grid gap-0">
          @for (step of process; track step.number) {
            <li class="grid gap-4 border-l-2 border-brand-200 pb-10 pl-7 sm:grid-cols-[4rem_1fr] sm:pl-10">
              <span class="font-display text-3xl font-semibold text-brand-600">{{ step.number }}</span>
              <div>
                <h3 class="text-lg font-bold">{{ step.title }}</h3>
                <p class="mt-2 text-sm leading-relaxed text-ink-600">{{ step.copy }}</p>
              </div>
            </li>
          }
        </ol>
      </div>
    </section>

    <section class="section-shell">
      <div class="rounded-3xl bg-brand-50 p-7 sm:p-10">
        <p class="eyebrow">Después de donar</p>
        <h2 class="section-title">Cuida tu recuperación</h2>
        <ul class="mt-6 grid gap-3 text-sm text-ink-700 sm:grid-cols-2">
          <li>✓ Descansa unos minutos antes de salir.</li>
          <li>✓ Bebe líquidos durante el resto del día.</li>
          <li>✓ Evita ejercicio intenso y cargas pesadas.</li>
          <li>✓ Sigue las indicaciones del personal del centro.</li>
        </ul>
      </div>
      <p class="mt-8 rounded-xl border-l-4 border-amber-400 bg-amber-50 p-5 text-sm leading-relaxed text-amber-900">
        Esta guía es general. Los requisitos, intervalos de donación y la elegibilidad dependen de tu
        estado de salud y de la evaluación realizada por profesionales médicos.
      </p>
    </section>
  `,
})
export class HowToDonatePage {
  readonly before = [
    {
      title: 'Descansa bien',
      copy: 'Procura dormir al menos seis horas la noche anterior y evita llegar con fatiga.',
    },
    {
      title: 'Come e hidrátate',
      copy: 'Consume una comida ligera y toma suficiente agua. Evita alimentos muy grasosos.',
    },
    {
      title: 'Lleva identificación',
      copy: 'Presenta una cédula o documento válido y comparte tu información de salud con honestidad.',
    },
  ];
  readonly process = HOW_TO_STEPS;
}
