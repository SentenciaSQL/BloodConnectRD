import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  BLOOD_TYPES,
  BloodRequest,
  Donation,
  Municipality,
  PageResponse,
  Province,
  donationStatusLabel,
  donationStatusTone,
  requestPendingUnits,
} from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import { apiErrorMessage, AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import {
  BadgeComponent,
  BloodTypeBadgeComponent,
  EmptyStateComponent,
  LoadingSpinnerComponent,
  PaginationComponent,
  RequestCardComponent,
  RequestProgressComponent,
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
    BadgeComponent,
    BloodTypeBadgeComponent,
    LoadingSpinnerComponent,
    RequestProgressComponent,
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

            <div class="mt-8">
              <app-request-progress [request]="request()!" />
            </div>
            <p class="mt-4 text-sm text-ink-500">
              Fecha límite: <strong class="text-ink-800">{{ request()!.deadline | date: 'mediumDate' }}</strong>
            </p>

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

            @if (isOwner() && donations().length) {
              <section class="mt-10 border-t border-ink-100 pt-8">
                <h2 class="font-display text-2xl font-semibold text-ink-950">Donaciones reportadas</h2>
                <p class="mt-1 text-sm text-ink-600">
                  Confirma cuántas unidades recibiste realmente de cada donante. El progreso solo
                  se actualiza con unidades confirmadas.
                </p>
                <div class="mt-5 grid gap-4">
                  @for (donation of donations(); track donation.id) {
                    <article class="rounded-2xl border border-ink-100 p-5">
                      <div class="flex flex-wrap items-start justify-between gap-3">
                        <div>
                          <h3 class="font-bold text-ink-950">{{ donation.donorName }}</h3>
                          <p class="mt-1 text-sm text-ink-500">
                            Reportada el {{ donation.donationDate | date: 'mediumDate' }}
                          </p>
                        </div>
                        <app-badge [tone]="statusTone(donation.status)">
                          {{ statusLabel(donation.status) }}
                        </app-badge>
                      </div>
                      <p class="mt-3 text-sm text-ink-600">
                        Reportadas: {{ donation.units }} · Confirmadas: {{ donation.confirmedUnits }}
                      </p>
                      @if (canConfirm(donation)) {
                        <form class="mt-4 flex flex-wrap items-end gap-3" (ngSubmit)="confirm(donation)">
                          <label class="min-w-[10rem] flex-1">
                            <span class="form-label">Unidades recibidas</span>
                            <input
                              type="number"
                              min="1"
                              class="form-control"
                              [max]="maxConfirmable(donation)"
                              [value]="confirmValue(donation)"
                              (input)="setConfirmUnits(donation.id, $any($event.target).value)"
                            />
                          </label>
                          <button type="submit" class="btn-primary" [disabled]="confirmingId() === donation.id">
                            {{ confirmingId() === donation.id ? 'Confirmando…' : 'Confirmar unidades recibidas' }}
                          </button>
                        </form>
                      }
                    </article>
                  }
                </div>
              </section>
            }
          </article>

          <aside class="h-fit space-y-4">
            <div class="rounded-2xl bg-ink-950 p-6 text-white">
              <h2 class="font-display text-2xl font-semibold">¿Puedes donar?</h2>
              <p class="mt-3 text-sm leading-relaxed text-ink-300">
                Envía tu intención de ayudar. La persona solicitante podrá coordinar contigo de forma
                segura.
              </p>
              @if (auth.isDonor() && !isOwner()) {
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
              } @else if (auth.isAuthenticated() && !auth.isDonor()) {
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
              } @else if (!auth.isAuthenticated()) {
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
              } @else {
                <p class="mt-5 text-sm leading-relaxed text-ink-300">
                  Esta es tu solicitud. Confirma las unidades cuando las recibas.
                </p>
              }
              <p class="mt-5 border-t border-ink-800 pt-5 text-xs leading-relaxed text-ink-400">
                Tu compatibilidad y elegibilidad deben ser confirmadas por profesionales de salud.
                Nunca dones fuera de un centro autorizado.
              </p>
            </div>

            @if (canReportDonation()) {
              <div class="rounded-2xl border border-brand-200 bg-white p-6">
                <h2 class="font-display text-xl font-semibold text-ink-950">Marcar como donación realizada</h2>
                <p class="mt-2 text-sm text-ink-600">
                  Indica cuántas unidades donaste. El progreso de la solicitud no cambia hasta que
                  el receptor confirme la recepción.
                </p>
                <form class="mt-5 grid gap-4" [formGroup]="reportForm" (ngSubmit)="reportDonation()">
                  <label>
                    <span class="form-label">Unidades donadas</span>
                    <input type="number" min="1" formControlName="units" class="form-control" />
                  </label>
                  <label>
                    <span class="form-label">Notas opcionales</span>
                    <textarea formControlName="notes" rows="2" maxlength="500" class="form-control"></textarea>
                  </label>
                  <button type="submit" class="btn-primary w-full" [disabled]="reporting()">
                    {{ reporting() ? 'Registrando…' : 'Marcar como donación realizada' }}
                  </button>
                </form>
              </div>
            } @else if (myDonation()) {
              <div class="rounded-2xl border border-ink-100 bg-white p-6">
                <h2 class="font-display text-xl font-semibold text-ink-950">Tu reporte</h2>
                <p class="mt-2 text-sm text-ink-600">
                  Reportaste {{ myDonation()!.units }}
                  {{ myDonation()!.units === 1 ? 'unidad' : 'unidades' }}.
                  Confirmadas: {{ myDonation()!.confirmedUnits }}.
                </p>
                <p class="mt-3">
                  <app-badge [tone]="statusTone(myDonation()!.status)">
                    {{ statusLabel(myDonation()!.status) }}
                  </app-badge>
                </p>
              </div>
            }
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
  readonly donations = signal<Donation[]>([]);
  readonly confirmUnits = signal<Record<number, number>>({});
  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly reporting = signal(false);
  readonly confirmingId = signal<number | null>(null);
  readonly responseForm = this.fb.nonNullable.group({
    message: ['', Validators.maxLength(500)],
  });
  readonly reportForm = this.fb.nonNullable.group({
    units: [1, [Validators.required, Validators.min(1)]],
    notes: ['', Validators.maxLength(500)],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.request(id).subscribe({
      next: (request) => {
        this.request.set(request);
        this.loadDonations(request.id);
      },
      error: () => this.loading.set(false),
      complete: () => this.loading.set(false),
    });
  }

  isOwner(): boolean {
    const userId = this.auth.user()?.id;
    return Boolean(userId && userId === this.request()?.createdById);
  }

  canReportDonation(): boolean {
    const request = this.request();
    if (!request || !this.auth.isDonor() || this.isOwner()) return false;
    if (request.status !== 'OPEN' && request.status !== 'IN_PROGRESS') return false;
    return !this.donations().some(
      (donation) =>
        donation.donorUserId === this.auth.user()?.id &&
        (donation.status === 'REPORTED' || donation.status === 'PARTIALLY_CONFIRMED'),
    );
  }

  myDonation(): Donation | undefined {
    const userId = this.auth.user()?.id;
    return this.donations().find((donation) => donation.donorUserId === userId);
  }

  canConfirm(donation: Donation): boolean {
    return (
      this.isOwner() &&
      (donation.status === 'REPORTED' || donation.status === 'PARTIALLY_CONFIRMED') &&
      this.maxConfirmable(donation) > donation.confirmedUnits
    );
  }

  maxConfirmable(donation: Donation): number {
    const pending = requestPendingUnits(this.request()!);
    return Math.min(donation.units, donation.confirmedUnits + pending);
  }

  defaultConfirmUnits(donation: Donation): number {
    return Math.max(donation.confirmedUnits || 1, Math.min(donation.units, this.maxConfirmable(donation)));
  }

  confirmValue(donation: Donation): number {
    const selected = this.confirmUnits()[donation.id];
    return selected === undefined ? this.defaultConfirmUnits(donation) : selected;
  }

  setConfirmUnits(id: number, value: string): void {
    const units = Number(value);
    this.confirmUnits.update((current) => ({ ...current, [id]: units }));
  }

  statusLabel(status: string): string {
    return donationStatusLabel(status);
  }

  statusTone(status: string): 'red' | 'green' | 'amber' | 'neutral' {
    return donationStatusTone(status);
  }

  respond(): void {
    if (!this.request() || this.responseForm.invalid) return;
    this.sending.set(true);
    this.api.respondToRequest(this.request()!.id, this.responseForm.controls.message.value).subscribe({
      next: () => {
        this.toast.success('Tu respuesta fue enviada. Revisa tus notificaciones para dar seguimiento.');
        this.responseForm.reset();
      },
      error: (error) => {
        this.sending.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.sending.set(false),
    });
  }

  reportDonation(): void {
    if (!this.request() || this.reportForm.invalid) {
      this.reportForm.markAllAsTouched();
      return;
    }
    this.reporting.set(true);
    const value = this.reportForm.getRawValue();
    this.api.reportDonation(this.request()!.id, { units: value.units, notes: value.notes || undefined }).subscribe({
      next: () => {
        this.toast.success('Tu donación fue reportada. El receptor confirmará las unidades recibidas.');
        this.refresh();
      },
      error: (error) => {
        this.reporting.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.reporting.set(false),
    });
  }

  confirm(donation: Donation): void {
    const units = this.confirmValue(donation);
    this.confirmingId.set(donation.id);
    this.api.confirmDonation(donation.id, units).subscribe({
      next: () => {
        this.toast.success('Las unidades recibidas fueron confirmadas.');
        this.refresh();
      },
      error: (error) => {
        this.confirmingId.set(null);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.confirmingId.set(null),
    });
  }

  private refresh(): void {
    const id = this.request()?.id;
    if (!id) return;
    this.api.request(id).subscribe({
      next: (request) => this.request.set(request),
    });
    this.loadDonations(id);
  }

  private loadDonations(id: number): void {
    if (!this.auth.isAuthenticated()) {
      this.donations.set([]);
      return;
    }
    this.api.requestDonations(id).subscribe({
      next: (donations) => this.donations.set(donations),
      error: () => this.donations.set([]),
    });
  }
}
