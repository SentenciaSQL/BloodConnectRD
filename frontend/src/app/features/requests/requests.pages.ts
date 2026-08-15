import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  BLOOD_TYPES,
  BloodRequest,
  Municipality,
  PageResponse,
  Province,
} from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import { apiErrorMessage, AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import {
  BloodTypeBadgeComponent,
  EmptyStateComponent,
  LoadingSpinnerComponent,
  PaginationComponent,
  RequestCardComponent,
  UrgencyBadgeComponent,
} from '../../shared/components/ui-components';

@Component({
  selector: 'app-requests-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    EmptyStateComponent,
    LoadingSpinnerComponent,
    PaginationComponent,
    RequestCardComponent,
  ],
  template: `
    <section class="bg-brand-600 px-5 py-16 text-white">
      <div class="mx-auto max-w-7xl lg:px-3">
        <p class="eyebrow !text-brand-100">Ayuda donde más se necesita</p>
        <h1 class="font-display text-4xl font-semibold sm:text-5xl">Solicitudes de sangre</h1>
        <p class="mt-4 max-w-2xl text-brand-50">
          Encuentra casos por tipo de sangre, urgencia o ubicación y conoce cómo puedes ayudar.
        </p>
      </div>
    </section>

    <section class="section-shell">
      <div class="mb-5 flex flex-wrap gap-2">
        <button type="button" class="filter-chip" (click)="nearMe()">Cerca de mí</button>
        @for (zone of ['Distrito Nacional', 'Santo Domingo', 'Santiago']; track zone) {
          <button type="button" class="filter-chip" (click)="selectZone(zone)">{{ zone }}</button>
        }
        <button type="button" class="filter-chip" (click)="loadUrgent()">Solicitudes urgentes</button>
        <button type="button" class="filter-chip" (click)="loadCompatible()">Mi tipo de sangre</button>
      </div>

      <form
        [formGroup]="filters"
        (ngSubmit)="load(0)"
        class="grid gap-4 rounded-2xl border border-ink-100 bg-white p-5 shadow-sm md:grid-cols-2 xl:grid-cols-5"
      >
        <label>
          <span class="form-label">Buscar</span>
          <input formControlName="search" class="form-control" placeholder="Hospital o sector" />
        </label>
        <label>
          <span class="form-label">Tipo sanguíneo</span>
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
        <div class="flex items-end">
          <button type="submit" class="btn-primary w-full">Aplicar filtros</button>
        </div>
      </form>

      <div class="mt-10 flex flex-wrap items-end justify-between gap-4">
        <div>
          <p class="eyebrow">{{ resultLabel() }}</p>
          <h2 class="font-display text-3xl font-semibold text-ink-950">Casos activos</h2>
          @if (page()) {
            <p class="mt-1 text-sm text-ink-500">{{ page()!.totalElements }} solicitudes encontradas</p>
          }
        </div>
        <a routerLink="/dashboard/solicitudes" class="btn-primary">Crear solicitud</a>
      </div>

      @if (loading()) {
        <app-loading-spinner label="Buscando solicitudes…" />
      } @else if (page()?.content?.length) {
        <div class="mt-7 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          @for (request of page()!.content; track request.id) {
            <app-request-card [request]="request" />
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
            title="No encontramos solicitudes"
            message="Prueba con otra ubicación, tipo de sangre o búsqueda."
          />
        </div>
      }
    </section>
  `,
})
export class RequestsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  readonly bloodTypes = BLOOD_TYPES;
  readonly provinces = signal<Province[]>([]);
  readonly municipalities = signal<Municipality[]>([]);
  readonly page = signal<PageResponse<BloodRequest> | null>(null);
  readonly loading = signal(true);
  readonly resultLabel = signal('Solicitudes disponibles');
  readonly filters = this.fb.nonNullable.group({
    search: [''],
    bloodType: [''],
    provinceId: [''],
    municipalityId: [''],
  });

  ngOnInit(): void {
    this.api.provinces().subscribe({
      next: (provinces) => {
        this.provinces.set(provinces);
        this.applyRouteFilter();
      },
      error: () => {
        this.toast.error('No pudimos cargar las provincias.');
        this.applyRouteFilter();
      },
    });
  }

  provinceChanged(thenLoad = false): void {
    this.filters.controls.municipalityId.setValue('');
    const provinceId = Number(this.filters.controls.provinceId.value);
    if (!provinceId) {
      this.municipalities.set([]);
      if (thenLoad) this.load(0);
      return;
    }
    this.api.municipalities(provinceId).subscribe({
      next: (municipalities) => this.municipalities.set(municipalities),
      error: () => this.toast.error('No pudimos cargar los municipios.'),
      complete: () => {
        if (thenLoad) this.load(0);
      },
    });
  }

  selectZone(name: string): void {
    const province = this.provinces().find((item) => item.name === name);
    if (!province) {
      this.toast.error('No encontramos esa provincia en el catálogo.');
      return;
    }
    this.filters.controls.provinceId.setValue(String(province.id));
    this.resultLabel.set(`Solicitudes en ${name}`);
    this.provinceChanged(true);
  }

  load(page = 0): void {
    this.loading.set(true);
    this.resultLabel.set('Solicitudes disponibles');
    const values = this.filters.getRawValue();
    this.api
      .requests({
        page,
        size: 12,
        status: 'OPEN',
        search: values.search,
        bloodType: values.bloodType,
        provinceId: values.provinceId,
        municipalityId: values.municipalityId,
      })
      .subscribe({
        next: (response) => this.page.set(response),
        error: () => {
          this.loading.set(false);
          this.toast.error('No pudimos consultar las solicitudes.');
        },
        complete: () => this.loading.set(false),
      });
  }

  loadUrgent(): void {
    this.loading.set(true);
    this.resultLabel.set('Prioridad alta');
    this.api.urgentRequests(20).subscribe({
      next: (response) => this.page.set(response),
      error: () => {
        this.loading.set(false);
        this.toast.error('No pudimos consultar las solicitudes urgentes.');
      },
      complete: () => this.loading.set(false),
    });
  }

  loadCompatible(): void {
    if (!this.auth.isDonor()) {
      this.toast.info('Inicia sesión y completa tu perfil de donante para ver compatibilidades.');
      return;
    }
    this.loading.set(true);
    this.resultLabel.set('Compatibles con mi sangre');
    this.api.compatibleRequests().subscribe({
      next: (response) => this.page.set(response),
      error: () => {
        this.loading.set(false);
        this.toast.error('No pudimos consultar tus solicitudes compatibles.');
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
        this.api.nearbyRequests(coords.latitude, coords.longitude).subscribe({
          next: (requests) => {
            this.resultLabel.set('Solicitudes cerca de ti');
            this.page.set({
              content: requests,
              page: 0,
              size: requests.length,
              totalElements: requests.length,
              totalPages: 1,
              first: true,
              last: true,
            });
          },
          error: () => {
            this.loading.set(false);
            this.toast.error('No pudimos buscar solicitudes cercanas.');
          },
          complete: () => this.loading.set(false),
        });
      },
      () => {
        this.loading.set(false);
        this.toast.error('Activa el permiso de ubicación para buscar casos cercanos.');
      },
    );
  }

  private applyRouteFilter(): void {
    const params = this.route.snapshot.queryParamMap;
    const zone = params.get('zona');
    if (zone) {
      this.selectZone(zone);
    } else if (params.has('urgente')) {
      this.loadUrgent();
    } else if (params.has('compatibles')) {
      this.loadCompatible();
      if (!this.auth.isDonor()) this.load();
    } else if (params.has('cerca')) {
      this.nearMe();
    } else {
      this.load();
    }
  }
}

@Component({
  selector: 'app-request-detail-page',
  standalone: true,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    BloodTypeBadgeComponent,
    LoadingSpinnerComponent,
    UrgencyBadgeComponent,
  ],
  template: `
    <section class="section-shell">
      <a routerLink="/solicitudes" class="text-sm font-bold text-brand-700">← Volver a solicitudes</a>

      @if (loading()) {
        <app-loading-spinner label="Cargando solicitud…" />
      } @else if (request()) {
        <div class="mt-7 grid gap-8 lg:grid-cols-[1fr_22rem]">
          <article class="rounded-3xl border border-ink-100 bg-white p-6 shadow-sm sm:p-9">
            <div class="flex flex-wrap items-start justify-between gap-4">
              <app-blood-type-badge [type]="request()!.bloodType" />
              <app-urgency-badge [urgency]="request()!.urgency" />
            </div>
            <p class="mt-7 text-sm font-bold uppercase tracking-wider text-brand-700">Solicitud de sangre</p>
            <h1 class="mt-2 font-display text-4xl font-semibold text-ink-950">
              {{ request()!.hospital }}
            </h1>
            <p class="mt-3 text-ink-600">
              {{ request()!.municipalityName }}, {{ request()!.provinceName }}
            </p>

            <dl class="mt-8 grid gap-5 border-y border-ink-100 py-7 sm:grid-cols-3">
              <div>
                <dt class="text-xs font-bold uppercase tracking-wider text-ink-500">Unidades requeridas</dt>
                <dd class="mt-1 text-xl font-bold">{{ request()!.unitsRequired }}</dd>
              </div>
              <div>
                <dt class="text-xs font-bold uppercase tracking-wider text-ink-500">Unidades completadas</dt>
                <dd class="mt-1 text-xl font-bold">{{ request()!.completedUnits }}</dd>
              </div>
              <div>
                <dt class="text-xs font-bold uppercase tracking-wider text-ink-500">Fecha límite</dt>
                <dd class="mt-1 text-lg font-bold">{{ request()!.deadline | date: 'mediumDate' }}</dd>
              </div>
            </dl>

            <div class="mt-8 grid gap-7 sm:grid-cols-2">
              <div>
                <h2 class="font-bold text-ink-950">Ubicación</h2>
                <p class="mt-2 text-sm leading-relaxed text-ink-600">
                  {{ request()!.address }}
                  @if (request()!.sector) { · {{ request()!.sector }} }
                </p>
                @if (request()!.reference) {
                  <p class="mt-2 text-sm text-ink-500">Referencia: {{ request()!.reference }}</p>
                }
              </div>
              <div>
                <h2 class="font-bold text-ink-950">Contacto</h2>
                <a [href]="'tel:' + request()!.contactPhone" class="mt-2 block text-sm font-bold text-brand-700">
                  {{ request()!.contactPhone }}
                </a>
                <p class="mt-2 text-xs text-ink-500">Solicitada por {{ request()!.createdByName }}</p>
              </div>
            </div>
            @if (request()!.description) {
              <div class="mt-8">
                <h2 class="font-bold text-ink-950">Información adicional</h2>
                <p class="mt-2 whitespace-pre-line text-sm leading-relaxed text-ink-600">
                  {{ request()!.description }}
                </p>
              </div>
            }
          </article>

          <aside class="h-fit rounded-2xl bg-ink-950 p-6 text-white">
            <h2 class="font-display text-2xl font-semibold">¿Puedes donar?</h2>
            <p class="mt-3 text-sm leading-relaxed text-ink-300">
              Envía tu intención de ayudar. La persona solicitante podrá coordinar contigo de forma
              segura.
            </p>
            @if (auth.isDonor()) {
              <form class="mt-6" [formGroup]="responseForm" (ngSubmit)="respond()">
                <label>
                  <span class="mb-1.5 block text-sm font-semibold">Mensaje opcional</span>
                  <textarea
                    formControlName="message"
                    rows="3"
                    maxlength="500"
                    class="form-control !border-ink-700 !bg-ink-900 !text-white"
                    placeholder="Indica cuándo podrías acudir"
                  ></textarea>
                </label>
                <button type="submit" class="btn-primary mt-4 w-full" [disabled]="sending()">
                  {{ sending() ? 'Enviando…' : 'Quiero ayudar' }}
                </button>
              </form>
            } @else if (auth.isAuthenticated()) {
              <p class="mt-5 text-sm leading-relaxed text-ink-300">
                Ya tienes una cuenta. Completa tu perfil de donante para poder responder a esta
                solicitud.
              </p>
              <a
                routerLink="/dashboard/perfil"
                [queryParams]="{ retorno: '/solicitudes/' + request()!.id }"
                class="btn-primary mt-4 w-full"
              >
                Completar perfil de donante
              </a>
            } @else {
              <a
                routerLink="/login"
                [queryParams]="{ retorno: '/solicitudes/' + request()!.id }"
                class="btn-primary mt-6 w-full"
              >
                Iniciar sesión para ayudar
              </a>
              <a
                routerLink="/registro"
                [queryParams]="{ retorno: '/solicitudes/' + request()!.id }"
                class="mt-3 block text-center text-sm font-semibold text-brand-200 hover:text-white"
              >
                Crear cuenta
              </a>
            }
            <p class="mt-5 border-t border-ink-800 pt-5 text-xs leading-relaxed text-ink-400">
              Tu compatibilidad y elegibilidad deben ser confirmadas por profesionales de salud.
              Nunca dones fuera de un centro autorizado.
            </p>
          </aside>
        </div>
      } @else {
        <div class="mt-10 rounded-2xl bg-white p-10 text-center">
          <h1 class="font-display text-3xl font-semibold">Solicitud no disponible</h1>
          <p class="mt-2 text-ink-600">El caso no existe o ya no se puede consultar.</p>
        </div>
      }
    </section>
  `,
})
export class RequestDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  readonly auth = inject(AuthService);
  readonly request = signal<BloodRequest | null>(null);
  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly responseForm = this.fb.nonNullable.group({
    message: ['', Validators.maxLength(500)],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.request(id).subscribe({
      next: (request) => this.request.set(request),
      error: () => this.loading.set(false),
      complete: () => this.loading.set(false),
    });
  }

  respond(): void {
    if (!this.request() || this.responseForm.invalid) return;
    this.sending.set(true);
    this.api.respondToRequest(this.request()!.id, this.responseForm.controls.message.value).subscribe({
      next: () => {
        this.toast.success('Tu respuesta fue enviada. Puedes seguirla en Mis donaciones.');
        this.responseForm.reset();
      },
      error: (error) => {
        this.sending.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.sending.set(false),
    });
  }
}
