import { Component, OnInit, inject, signal } from '@angular/core';

import { BLOOD_TYPES, BloodType, Compatibility } from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { LoadingSpinnerComponent } from '../../shared/components/ui-components';

@Component({
  selector: 'app-compatibility-page',
  standalone: true,
  imports: [LoadingSpinnerComponent],
  template: `
    <section class="bg-hero-mesh px-5 py-16 sm:py-20">
      <div class="mx-auto max-w-5xl text-center">
        <p class="eyebrow">Información educativa</p>
        <h1 class="font-display text-4xl font-semibold text-ink-950 sm:text-5xl">
          Compatibilidad sanguínea
        </h1>
        <p class="mx-auto mt-4 max-w-2xl text-ink-600">
          Selecciona un tipo de sangre para conocer a quién puede donar glóbulos rojos y de quién
          puede recibirlos.
        </p>
        <div class="mt-8 flex flex-wrap justify-center gap-3">
          @for (type of bloodTypes; track type) {
            <button
              type="button"
              (click)="select(type)"
              class="grid h-14 min-w-14 place-items-center rounded-full border-2 px-2 font-black transition"
              [class]="
                selected() === type
                  ? 'border-brand-600 bg-brand-600 text-white shadow-lg'
                  : 'border-ink-200 bg-white text-ink-800 hover:border-brand-300'
              "
            >
              {{ type }}
            </button>
          }
        </div>
      </div>
    </section>

    <section class="section-shell !max-w-5xl">
      @if (loading()) {
        <app-loading-spinner label="Consultando compatibilidad…" />
      } @else if (result()) {
        <div class="grid gap-6 md:grid-cols-2">
          <article class="rounded-3xl bg-brand-600 p-7 text-white sm:p-9">
            <p class="text-sm font-bold uppercase tracking-wider text-brand-100">Puede donar a</p>
            <div class="mt-6 flex flex-wrap gap-3">
              @for (type of result()!.canDonateTo; track type) {
                <span class="grid h-14 min-w-14 place-items-center rounded-full bg-white px-2 font-black text-brand-700">
                  {{ type }}
                </span>
              }
            </div>
            <p class="mt-7 text-sm leading-relaxed text-brand-100">
              Una persona {{ result()!.bloodType }} puede donar glóbulos rojos a los grupos mostrados.
            </p>
          </article>
          <article class="rounded-3xl bg-ink-950 p-7 text-white sm:p-9">
            <p class="text-sm font-bold uppercase tracking-wider text-ink-300">Puede recibir de</p>
            <div class="mt-6 flex flex-wrap gap-3">
              @for (type of result()!.canReceiveFrom; track type) {
                <span class="grid h-14 min-w-14 place-items-center rounded-full bg-white px-2 font-black text-ink-900">
                  {{ type }}
                </span>
              }
            </div>
            <p class="mt-7 text-sm leading-relaxed text-ink-300">
              Una persona {{ result()!.bloodType }} puede recibir glóbulos rojos de los grupos mostrados.
            </p>
          </article>
        </div>

        <div class="mt-8 rounded-2xl border-l-4 border-amber-400 bg-amber-50 p-6">
          <h2 class="font-bold text-amber-950">Aviso médico importante</h2>
          <p class="mt-2 text-sm leading-relaxed text-amber-900">
            {{ result()!.disclaimer || defaultDisclaimer }}
          </p>
          <p class="mt-2 text-sm leading-relaxed text-amber-900">
            Esta guía no sustituye pruebas de compatibilidad, tipificación ni la valoración de un
            profesional de salud. Plasma y plaquetas tienen reglas diferentes.
          </p>
        </div>
      }

      <div class="mt-14 border-t border-ink-200 pt-10">
        <h2 class="font-display text-3xl font-semibold text-ink-950">Conceptos básicos</h2>
        <div class="mt-7 grid gap-8 md:grid-cols-3">
          <div>
            <p class="font-bold text-brand-700">Sistema ABO</p>
            <p class="mt-2 text-sm leading-relaxed text-ink-600">
              Clasifica la sangre en A, B, AB u O según los antígenos presentes.
            </p>
          </div>
          <div>
            <p class="font-bold text-brand-700">Factor Rh</p>
            <p class="mt-2 text-sm leading-relaxed text-ink-600">
              El signo positivo o negativo indica la presencia o ausencia del factor Rh.
            </p>
          </div>
          <div>
            <p class="font-bold text-brand-700">Prueba cruzada</p>
            <p class="mt-2 text-sm leading-relaxed text-ink-600">
              El centro de salud confirma la compatibilidad antes de cualquier transfusión.
            </p>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class CompatibilityPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  readonly bloodTypes = BLOOD_TYPES;
  readonly selected = signal<BloodType>('O+');
  readonly result = signal<Compatibility | null>(null);
  readonly loading = signal(true);
  readonly defaultDisclaimer =
    'La compatibilidad mostrada es orientativa y debe ser confirmada por personal médico.';

  ngOnInit(): void {
    this.select('O+');
  }

  select(type: BloodType): void {
    this.selected.set(type);
    this.loading.set(true);
    this.api.compatibility(type).subscribe({
      next: (result) => this.result.set(result),
      error: () => {
        this.loading.set(false);
        this.toast.error('No pudimos consultar la compatibilidad.');
      },
      complete: () => this.loading.set(false),
    });
  }
}
