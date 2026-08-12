import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import {
  DonationCenter,
  Municipality,
  PageResponse,
  Province,
} from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import {
  DonationCenterCardComponent,
  EmptyStateComponent,
  LoadingSpinnerComponent,
  PaginationComponent,
} from '../../shared/components/ui-components';

@Component({
  selector: 'app-centers-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DonationCenterCardComponent,
    EmptyStateComponent,
    LoadingSpinnerComponent,
    PaginationComponent,
  ],
  template: `
    <section class="relative overflow-hidden bg-ink-950 px-5 py-20 text-white">
      <div class="absolute inset-y-0 right-0 hidden w-1/2 bg-brand-600/15 lg:block"></div>
      <div class="relative mx-auto max-w-7xl lg:px-3">
        <p class="eyebrow !text-brand-300">Donación segura</p>
        <h1 class="max-w-3xl font-display text-4xl font-semibold sm:text-5xl">Centros de donación</h1>
        <p class="mt-4 max-w-2xl text-ink-300">
          Consulta centros registrados, sus horarios y datos de contacto antes de acudir.
        </p>
      </div>
    </section>

    <section class="section-shell">
      <form
        [formGroup]="filters"
        (ngSubmit)="load(0)"
        class="grid gap-4 rounded-2xl border border-ink-100 bg-white p-5 shadow-sm md:grid-cols-4"
      >
        <label>
          <span class="form-label">Provincia</span>
          <select formControlName="provinceId" class="form-control" (change)="provinceChanged()">
            <option value="">Todas</option>
            @for (province of provinces(); track province.id) {
              <option [value]="province.id">{{ province.name }}</option>
            }
          </select>
        </label>
        <label>
          <span class="form-label">Municipio</span>
          <select formControlName="municipalityId" class="form-control">
            <option value="">Todos</option>
            @for (municipality of municipalities(); track municipality.id) {
              <option [value]="municipality.id">{{ municipality.name }}</option>
            }
          </select>
        </label>
        <label>
          <span class="form-label">Tipo de centro</span>
          <select formControlName="type" class="form-control">
            <option value="">Todos</option>
            <option value="HOSPITAL">Hospital</option>
            <option value="CLINIC">Clínica</option>
            <option value="BLOOD_BANK">Banco de sangre</option>
            <option value="MEDICAL_CENTER">Centro médico</option>
            <option value="OTHER">Otro</option>
          </select>
        </label>
        <div class="flex items-end gap-2">
          <button type="submit" class="btn-primary flex-1">Buscar</button>
          <button type="button" class="btn-secondary" (click)="nearMe()">Cerca de mí</button>
        </div>
      </form>

      <div class="mt-10">
        <p class="eyebrow">{{ heading() }}</p>
        <h2 class="section-title">Lugares disponibles</h2>
        @if (page()) {
          <p class="mt-1 text-sm text-ink-500">{{ page()!.totalElements }} centros encontrados</p>
        }
      </div>

      @if (loading()) {
        <app-loading-spinner label="Buscando centros de donación…" />
      } @else if (page()?.content?.length) {
        <div class="mt-7 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          @for (center of page()!.content; track center.id) {
            <app-donation-center-card [center]="center" />
          }
        </div>
        <app-pagination
          [page]="page()!.page"
          [totalPages]="page()!.totalPages"
          (changed)="load($event)"
        />
      } @else {
        <div class="mt-7">
          <app-empty-state
            title="No encontramos centros"
            message="Cambia la ubicación o el tipo de centro para ampliar la búsqueda."
          />
        </div>
      }

      <div class="mt-14 rounded-2xl border-l-4 border-amber-400 bg-amber-50 p-6">
        <h3 class="font-bold text-amber-950">Antes de acudir</h3>
        <p class="mt-2 text-sm leading-relaxed text-amber-900">
          Confirma el horario por teléfono y lleva un documento de identidad. El personal del centro
          evaluará si cumples los requisitos médicos para donar.
        </p>
      </div>
    </section>
  `,
})
export class CentersPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  readonly provinces = signal<Province[]>([]);
  readonly municipalities = signal<Municipality[]>([]);
  readonly page = signal<PageResponse<DonationCenter> | null>(null);
  readonly loading = signal(true);
  readonly heading = signal('Directorio nacional');
  readonly filters = this.fb.nonNullable.group({
    provinceId: [''],
    municipalityId: [''],
    type: [''],
  });

  ngOnInit(): void {
    this.api.provinces().subscribe({
      next: (provinces) => this.provinces.set(provinces),
      error: () => this.toast.error('No pudimos cargar las provincias.'),
    });
    this.load();
  }

  provinceChanged(): void {
    this.filters.controls.municipalityId.setValue('');
    const provinceId = Number(this.filters.controls.provinceId.value);
    if (!provinceId) {
      this.municipalities.set([]);
      return;
    }
    this.api.municipalities(provinceId).subscribe({
      next: (municipalities) => this.municipalities.set(municipalities),
      error: () => this.toast.error('No pudimos cargar los municipios.'),
    });
  }

  load(page = 0): void {
    this.loading.set(true);
    this.heading.set('Directorio nacional');
    this.api.centers({ ...this.filters.getRawValue(), page, size: 12 }).subscribe({
      next: (response) => this.page.set(response),
      error: () => {
        this.loading.set(false);
        this.toast.error('No pudimos consultar los centros.');
      },
      complete: () => this.loading.set(false),
    });
  }

  nearMe(): void {
    if (!navigator.geolocation) {
      this.toast.error('Tu navegador no permite obtener la ubicación.');
      return;
    }
    this.loading.set(true);
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        this.api.nearbyCenters(coords.latitude, coords.longitude).subscribe({
          next: (centers) => {
            this.heading.set('Centros cerca de ti');
            this.page.set({
              content: centers,
              page: 0,
              size: centers.length,
              totalElements: centers.length,
              totalPages: 1,
              first: true,
              last: true,
            });
          },
          error: () => {
            this.loading.set(false);
            this.toast.error('No pudimos buscar centros cercanos.');
          },
          complete: () => this.loading.set(false),
        });
      },
      () => {
        this.loading.set(false);
        this.toast.error('Activa el permiso de ubicación para buscar centros cercanos.');
      },
    );
  }
}
