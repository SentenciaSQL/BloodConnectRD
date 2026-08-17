import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  BLOOD_TYPES,
  BloodRequest,
  Donation,
  DonationResponse,
  Municipality,
  PageResponse,
  Province,
  donationStatusLabel,
  donationStatusTone,
  requestPendingUnits,
  responseStatusLabel,
  responseStatusTone,
} from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import { apiErrorMessage, AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import {
  BadgeComponent,
  BloodTypeBadgeComponent,
  EmptyStateComponent,
  LoadingSpinnerComponent,
  ModalComponent,
  PaginationComponent,
  RequestCardComponent,
  RequestProgressComponent,
  UnitsStepperComponent,
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
        status: ['OPEN', 'IN_PROGRESS'],
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

function localIsoDate(date = new Date()): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
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
    ModalComponent,
    RequestProgressComponent,
    UnitsStepperComponent,
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
            <p class="mt-7 text-sm font-bold uppercase tracking-wider text-brand-700">Detalle de solicitud</p>
            <h1 class="mt-2 font-display text-4xl font-semibold text-ink-950">
              {{ request()!.hospital }}
            </h1>
            <p class="mt-3 text-ink-600">
              {{ request()!.bloodType }} · {{ request()!.municipalityName }}, {{ request()!.provinceName }}
            </p>
            <p class="mt-5 font-display text-2xl font-semibold text-ink-950">
              Necesita {{ request()!.unitsRequired }}
              {{ request()!.unitsRequired === 1 ? 'unidad' : 'unidades' }}
            </p>
            @if (canReportDonation()) {
              <button type="button" class="btn-primary mt-5 min-h-12 px-8 text-base" (click)="openReportModal()">
                Ya doné
              </button>
            } @else if (myPendingDonation()) {
              <p class="mt-5 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-900">
                Donación reportada – pendiente de confirmación del receptor.
              </p>
            }

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

            @if (isOwner()) {
              <section class="mt-10 border-t border-ink-100 pt-8">
                <h2 class="font-display text-2xl font-semibold text-ink-950">Personas interesadas en ayudar</h2>
                <p class="mt-1 text-sm text-ink-600">
                  Estos mensajes son ofrecimientos de ayuda. No cambian el progreso de unidades hasta
                  que el donante pulse “Ya doné” y tú confirmes la recepción.
                </p>
                @if (offers().length) {
                  <div class="mt-5 grid gap-4">
                    @for (offer of offers(); track offer.id) {
                      <article class="rounded-2xl border border-ink-100 p-5">
                        <div class="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <h3 class="font-bold text-ink-950">
                              {{ offer.donorName }}
                              @if (offer.donorBloodType) {
                                <span class="text-brand-700">— {{ offer.donorBloodType }}</span>
                              }
                            </h3>
                            <p class="mt-1 text-sm text-ink-500">
                              Ofreció ayudar el {{ offer.createdAt | date: 'dd/MM/yyyy HH:mm' }}
                            </p>
                          </div>
                          <app-badge [tone]="offerTone(offer.status)">
                            {{ offerLabel(offer.status) }}
                          </app-badge>
                        </div>
                        @if (offer.message) {
                          <p class="mt-3 text-sm leading-relaxed text-ink-700">“{{ offer.message }}”</p>
                        } @else {
                          <p class="mt-3 text-sm text-ink-500">Sin mensaje adicional.</p>
                        }
                        <button
                          type="button"
                          class="btn-primary mt-4"
                          [disabled]="contactingId() === offer.id"
                          (click)="contact(offer)"
                        >
                          {{ contactingId() === offer.id ? 'Abriendo…' : 'Contactar' }}
                        </button>
                      </article>
                    }
                  </div>
                } @else {
                  <p class="mt-5 rounded-xl border border-dashed border-ink-200 bg-ink-50 px-4 py-4 text-sm text-ink-600">
                    Cuando un donante pulse “Quiero ayudar”, su mensaje aparecerá aquí.
                  </p>
                }
              </section>
            }

            @if (isOwner()) {
              <section class="mt-10 border-t border-ink-100 pt-8">
                <h2 class="font-display text-2xl font-semibold text-ink-950">Donaciones reportadas</h2>
                <p class="mt-1 text-sm text-ink-600">
                  Confirma cuántas unidades recibiste realmente de cada donante. El progreso solo
                  se actualiza con unidades confirmadas.
                </p>
                @if (donations().length) {
                  <div class="mt-5 grid gap-4">
                    @for (donation of donations(); track donation.id) {
                      <article class="rounded-2xl border border-ink-100 p-5">
                        <div class="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <h3 class="font-bold text-ink-950">{{ donation.donorName }}</h3>
                            <p class="mt-1 text-sm text-ink-500">
                              Reportada el {{ donation.donationDate | date: 'dd/MM/yyyy' }}
                            </p>
                          </div>
                          <app-badge [tone]="statusTone(donation.status)">
                            {{ statusLabel(donation.status) }}
                          </app-badge>
                        </div>
                        <p class="mt-3 text-sm text-ink-600">
                          Reportó: <strong>{{ donation.units }} {{ donation.units === 1 ? 'unidad' : 'unidades' }}</strong>
                        </p>
                        @if (canConfirm(donation)) {
                          <button type="button" class="btn-primary mt-4" (click)="openConfirmModal(donation)">
                            Confirmar recepción
                          </button>
                        }
                      </article>
                    }
                  </div>
                } @else {
                  <p class="mt-5 rounded-xl border border-dashed border-ink-200 bg-ink-50 px-4 py-4 text-sm text-ink-600">
                    Cuando un donante pulse “Ya doné”, el reporte aparecerá aquí para que confirmes la recepción.
                  </p>
                }
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
              @if (auth.isDonor() && !isOwner() && myActiveOffer()) {
                <p class="mt-6 rounded-xl bg-ink-900 px-4 py-3 text-sm font-semibold text-emerald-200">
                  Ya ofreciste ayudar. El creador de la solicitud recibió tu mensaje y podrá
                contactarte por el chat interno de BloodConnect.
                </p>
              } @else if (auth.isDonor() && !isOwner()) {
                <form class="mt-6" [formGroup]="responseForm" (ngSubmit)="respond()">
                  <label>
                    <span class="mb-1.5 block text-sm font-semibold">Mensaje para quien publicó la solicitud</span>
                    <textarea
                      formControlName="message"
                      rows="3"
                      maxlength="500"
                      class="form-control !border-ink-700 !bg-ink-900 !text-white"
                      placeholder="Hola, soy O+ y puedo donar mañana en la mañana."
                    ></textarea>
                  </label>
                  <button type="submit" class="btn-primary mt-4 w-full" [disabled]="sending() || !canOfferHelp()">
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
              <button type="button" class="btn-primary mt-6 w-full min-h-12" (click)="openReportModal()">
                Ya doné
              </button>
            } @else if (myPendingDonation()) {
              <p class="mt-5 rounded-lg bg-ink-900 px-4 py-3 text-sm font-semibold text-amber-200">
                Donación reportada – pendiente de confirmación del receptor.
              </p>
            }
          </aside>
        </div>
      } @else {
        <div class="mt-10 rounded-2xl bg-white p-10 text-center">
          <h1 class="font-display text-3xl font-semibold">Solicitud no disponible</h1>
          <p class="mt-2 text-ink-600">El caso no existe o ya no se puede consultar.</p>
        </div>
      }

      <app-modal [open]="reportModalOpen()" title="Ya doné" (closed)="reportModalOpen.set(false)">
        <p class="text-sm text-ink-600">
          Esta solicitud aún necesita {{ maxReportable() }}
          {{ maxReportable() === 1 ? 'unidad' : 'unidades' }}. El progreso no cambia hasta que el
          receptor confirme la recepción.
        </p>
        <form class="mt-6 grid gap-5" [formGroup]="reportForm" (ngSubmit)="reportDonation()">
          <div>
            <span class="form-label">¿Cuántas unidades donaste?</span>
            <app-units-stepper
              [value]="reportForm.controls.units.value"
              [min]="1"
              [max]="maxReportable()"
              (valueChange)="reportForm.controls.units.setValue($event)"
            />
          </div>
          <label>
            <span class="form-label">Fecha de donación</span>
            <input type="date" formControlName="donationDate" class="form-control" [max]="today" />
          </label>
          <label>
            <span class="form-label">Nota opcional</span>
            <textarea formControlName="notes" rows="2" maxlength="500" class="form-control"></textarea>
          </label>
          <button type="submit" class="btn-primary w-full" [disabled]="reporting() || maxReportable() < 1">
            {{ reporting() ? 'Registrando…' : 'Confirmar donación' }}
          </button>
        </form>
      </app-modal>

      <app-modal
        [open]="confirmModalOpen()"
        title="Confirmar recepción"
        (closed)="closeConfirmModal()"
      >
        @if (donationToConfirm(); as donation) {
          <p class="text-sm text-ink-600">
            <strong>{{ donation.donorName }}</strong> reportó {{ donation.units }}
            {{ donation.units === 1 ? 'unidad' : 'unidades' }}.
          </p>
          <dl class="mt-5 grid gap-2 text-sm">
            <div class="flex justify-between">
              <dt class="text-ink-500">Unidades reportadas</dt>
              <dd class="font-bold">{{ donation.units }}</dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-ink-500">Unidades recibidas</dt>
              <dd class="font-bold">{{ receivedUnits() }}</dd>
            </div>
          </dl>
          <div class="mt-5">
            <span class="form-label">Unidades recibidas</span>
            <app-units-stepper
              [value]="receivedUnits()"
              [min]="Math.max(1, donation.confirmedUnits)"
              [max]="maxConfirmable(donation)"
              (valueChange)="receivedUnits.set($event)"
            />
          </div>
          <button
            type="button"
            class="btn-primary mt-6 w-full"
            [disabled]="confirmingId() === donation.id"
            (click)="confirm(donation)"
          >
            {{ confirmingId() === donation.id ? 'Confirmando…' : 'Confirmar unidades' }}
          </button>
        }
      </app-modal>
    </section>
  `,
})
export class RequestDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  readonly auth = inject(AuthService);
  readonly Math = Math;
  readonly today = localIsoDate();
  readonly request = signal<BloodRequest | null>(null);
  readonly donations = signal<Donation[]>([]);
  readonly offers = signal<DonationResponse[]>([]);
  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly reporting = signal(false);
  readonly confirmingId = signal<number | null>(null);
  readonly contactingId = signal<number | null>(null);
  readonly reportModalOpen = signal(false);
  readonly confirmModalOpen = signal(false);
  readonly donationToConfirm = signal<Donation | null>(null);
  readonly receivedUnits = signal(1);
  readonly responseForm = this.fb.nonNullable.group({
    message: ['', Validators.maxLength(500)],
  });
  readonly reportForm = this.fb.nonNullable.group({
    units: [1, [Validators.required, Validators.min(1)]],
    donationDate: [this.today, Validators.required],
    notes: ['', Validators.maxLength(500)],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.request(id).subscribe({
      next: (request) => {
        this.request.set(request);
        this.loadDonations(request.id);
        this.loadOffers(request.id);
      },
      error: () => this.loading.set(false),
      complete: () => this.loading.set(false),
    });
  }

  isOwner(): boolean {
    const userId = this.auth.user()?.id;
    return Boolean(userId && userId === this.request()?.createdById);
  }

  maxReportable(): number {
    const request = this.request();
    return request ? Math.max(0, requestPendingUnits(request)) : 0;
  }

  canOfferHelp(): boolean {
    const request = this.request();
    if (!request || !this.auth.isDonor() || this.isOwner()) return false;
    if (request.status !== 'OPEN' && request.status !== 'IN_PROGRESS') return false;
    return !this.myActiveOffer();
  }

  myActiveOffer(): DonationResponse | undefined {
    const userId = this.auth.user()?.id;
    return this.offers().find(
      (offer) =>
        offer.donorUserId === userId &&
        (offer.status === 'PENDING' || offer.status === 'ACCEPTED'),
    );
  }

  offerLabel(status: string): string {
    return responseStatusLabel(status);
  }

  offerTone(status: string): 'red' | 'green' | 'amber' | 'neutral' {
    return responseStatusTone(status);
  }

  contact(offer: DonationResponse): void {
    const request = this.request();
    if (!request) return;
    this.contactingId.set(offer.id);
    this.api.openConversation(request.id, offer.donorUserId).subscribe({
      next: (conversation) => {
        void this.router.navigate(['/dashboard/mensajes', conversation.id]);
      },
      error: (error) => {
        this.contactingId.set(null);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.contactingId.set(null),
    });
  }

  canReportDonation(): boolean {
    const request = this.request();
    if (!request || !this.auth.isDonor() || this.isOwner()) return false;
    if (request.status !== 'OPEN' && request.status !== 'IN_PROGRESS') return false;
    if (this.maxReportable() < 1) return false;
    return !this.myPendingDonation();
  }

  myDonation(): Donation | undefined {
    const userId = this.auth.user()?.id;
    return this.donations().find((donation) => donation.donorUserId === userId);
  }

  myPendingDonation(): Donation | undefined {
    const donation = this.myDonation();
    if (!donation) return undefined;
    return donation.status === 'REPORTED' || donation.status === 'PARTIALLY_CONFIRMED'
      ? donation
      : undefined;
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

  openReportModal(): void {
    this.reportForm.reset({
      units: 1,
      donationDate: this.today,
      notes: '',
    });
    this.reportModalOpen.set(true);
  }

  openConfirmModal(donation: Donation): void {
    this.donationToConfirm.set(donation);
    this.receivedUnits.set(this.maxConfirmable(donation));
    this.confirmModalOpen.set(true);
  }

  closeConfirmModal(): void {
    this.confirmModalOpen.set(false);
    this.donationToConfirm.set(null);
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
        this.toast.success('Tu ofrecimiento de ayuda fue enviado.');
        this.responseForm.reset();
        this.loadOffers(this.request()!.id);
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
    const value = this.reportForm.getRawValue();
    if (value.units > this.maxReportable()) {
      this.toast.error('No puedes reportar más unidades de las que aún necesita la solicitud.');
      return;
    }
    this.reporting.set(true);
    this.api
      .reportDonation(this.request()!.id, {
        units: value.units,
        donationDate: value.donationDate,
        notes: value.notes || undefined,
      })
      .subscribe({
        next: () => {
          this.toast.success('Donación reportada – pendiente de confirmación del receptor.');
          this.reportModalOpen.set(false);
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
    const units = this.receivedUnits();
    this.confirmingId.set(donation.id);
    this.api.confirmDonation(donation.id, units).subscribe({
      next: () => {
        this.toast.success('Las unidades recibidas fueron confirmadas.');
        this.closeConfirmModal();
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
    this.loadOffers(id);
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

  private loadOffers(id: number): void {
    if (!this.auth.isAuthenticated()) {
      this.offers.set([]);
      return;
    }
    this.api.requestResponses(id).subscribe({
      next: (offers) => this.offers.set(offers),
      error: () => this.offers.set([]),
    });
  }
}

