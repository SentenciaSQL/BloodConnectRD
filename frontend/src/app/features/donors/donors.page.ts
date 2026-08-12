import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import {
  BLOOD_TYPES,
  Donor,
  Municipality,
  PageResponse,
  Province,
} from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import {
  BadgeComponent,
  BloodTypeBadgeComponent,
  EmptyStateComponent,
  LoadingSpinnerComponent,
  PaginationComponent,
} from '../../shared/components/ui-components';

@Component({
  selector: 'app-donors-page',
  standalone: true,
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    BadgeComponent,
    BloodTypeBadgeComponent,
    EmptyStateComponent,
    LoadingSpinnerComponent,
    PaginationComponent,
  ],
  template: `
    <section class="bg-ink-950 px-5 py-16 text-white">
      <div class="mx-auto max-w-7xl lg:px-3">
        <p class="eyebrow !text-brand-300">Red de donantes</p>
        <h1 class="font-display text-4xl font-semibold sm:text-5xl">Donantes disponibles</h1>
        <p class="mt-4 max-w-2xl text-ink-300">
          Busca por tipo sanguíneo y ubicación. Protegemos la identidad y los datos de contacto de
          cada donante.
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
          <span class="form-label">Tipo de sangre</span>
          <select formControlName="bloodType" class="form-control">
            <option value="">Todos</option>
            @for (type of bloodTypes; track type) {
              <option [value]="type">{{ type }}</option>
            }
          </select>
        </label>
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
        <div class="flex items-end gap-2">
          <button type="submit" class="btn-primary flex-1">Buscar</button>
          <button type="button" class="btn-secondary" (click)="nearMe()" aria-label="Buscar cerca de mí">
            Cerca de mí
          </button>
        </div>
      </form>

      <div class="mt-10 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 class="font-display text-3xl font-semibold text-ink-950">Resultados</h2>
          @if (page()) {
            <p class="mt-1 text-sm text-ink-500">{{ page()!.totalElements }} donantes encontrados</p>
          }
        </div>
        <a routerLink="/registro" class="btn-primary">Quiero ser donante</a>
      </div>

      @if (loading()) {
        <app-loading-spinner label="Buscando donantes…" />
      } @else if (page()?.content?.length) {
        <div class="mt-7 grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
          @for (donor of page()!.content; track donor.id) {
            <article class="rounded-2xl border border-ink-100 bg-white p-5 shadow-sm">
              <div class="flex items-start justify-between gap-4">
                <app-blood-type-badge [type]="donor.bloodType" />
                <app-badge [tone]="donor.availability === 'AVAILABLE' ? 'green' : 'amber'">
                  {{ donor.availability === 'AVAILABLE' ? 'Disponible' : 'No disponible temporalmente' }}
                </app-badge>
              </div>
              <h3 class="mt-5 font-bold text-ink-950">
                Donante en {{ donor.municipalityName }}
              </h3>
              <p class="mt-1 text-sm text-ink-600">{{ donor.provinceName }}</p>
              @if (donor.approximateDistanceKm !== null && donor.approximateDistanceKm !== undefined) {
                <p class="mt-4 text-sm font-semibold text-brand-700">
                  A {{ donor.approximateDistanceKm | number: '1.0-1' }} km aproximadamente
                </p>
              }
              <p class="mt-4 border-t border-ink-100 pt-4 text-xs leading-relaxed text-ink-500">
                Los datos personales se comparten únicamente mediante el flujo seguro de una solicitud.
              </p>
            </article>
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
            title="No encontramos donantes"
            message="Prueba con otra ubicación o elimina algunos filtros."
          />
        </div>
      }
    </section>
  `,
})
export class DonorsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  readonly bloodTypes = BLOOD_TYPES;
  readonly provinces = signal<Province[]>([]);
  readonly municipalities = signal<Municipality[]>([]);
  readonly page = signal<PageResponse<Donor> | null>(null);
  readonly loading = signal(true);
  private coordinates: { latitude: number; longitude: number } | null = null;
  readonly filters = this.fb.nonNullable.group({
    bloodType: [''],
    provinceId: [''],
    municipalityId: [''],
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
    const value = this.filters.getRawValue();
    this.api
      .donors({
        page,
        size: 12,
        bloodType: value.bloodType,
        provinceId: value.provinceId,
        municipalityId: value.municipalityId,
        availability: 'AVAILABLE',
        latitude: this.coordinates?.latitude,
        longitude: this.coordinates?.longitude,
      })
      .subscribe({
        next: (response) => this.page.set(response),
        error: () => {
          this.loading.set(false);
          this.toast.error('No pudimos consultar los donantes.');
        },
        complete: () => this.loading.set(false),
      });
  }

  nearMe(): void {
    if (!navigator.geolocation) {
      this.toast.error('Tu navegador no permite obtener la ubicación.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        this.coordinates = { latitude: coords.latitude, longitude: coords.longitude };
        this.load(0);
      },
      () => this.toast.error('Activa el permiso de ubicación para buscar donantes cercanos.'),
    );
  }
}
