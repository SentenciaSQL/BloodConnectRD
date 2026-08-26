import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  Availability,
  BLOOD_TYPES,
  BloodRequest,
  BloodRequestPayload,
  BloodType,
  DonationHistory,
  Donor,
  DonorPayload,
  Municipality,
  Notification,
  PageResponse,
  Province,
  RequestStatus,
  Sex,
  Urgency,
  requestProgressPercent,
} from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import { apiErrorMessage, AuthService } from '../../core/services/auth.service';
import { bloodRequestSlug } from '../../core/seo/request-slug';
import { ToastService } from '../../core/services/toast.service';
import {
  BadgeComponent,
  DonationCardComponent,
  EmptyStateComponent,
  LoadingSpinnerComponent,
  ModalComponent,
  PaginationComponent,
  RequestCardComponent,
} from '../../shared/components/ui-components';

function dominicanPhone(control: AbstractControl): ValidationErrors | null {
  const digits = String(control.value ?? '').replace(/\D/g, '');
  const national = digits.length === 11 && digits.startsWith('1') ? digits.slice(1) : digits;
  return /^(809|829|849)\d{7}$/.test(national) ? null : { dominicanPhone: true };
}

function normalizePhone(value: string): string {
  const digits = value.replace(/\D/g, '');
  return `+${digits.startsWith('1') ? digits : `1${digits}`}`;
}

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [RouterLink, RequestCardComponent, LoadingSpinnerComponent, EmptyStateComponent],
  template: `
    <header>
      <p class="eyebrow">Mi cuenta</p>
      <h1 class="font-display text-4xl font-semibold text-ink-950">
        Hola, {{ auth.user()?.firstName }}
      </h1>
      <p class="mt-2 text-ink-600">Aquí tienes un resumen de tu actividad reciente.</p>
    </header>

    <div class="mt-8 grid gap-4 sm:grid-cols-3">
      <article class="rounded-2xl bg-brand-600 p-6 text-white">
        <p class="text-sm font-semibold text-brand-100">Mis solicitudes</p>
        <p class="mt-3 font-display text-4xl font-semibold">{{ requests()?.totalElements ?? '—' }}</p>
      </article>
      <article class="rounded-2xl bg-white p-6 shadow-sm">
        <p class="text-sm font-semibold text-ink-500">Notificaciones sin leer</p>
        <p class="mt-3 font-display text-4xl font-semibold text-ink-950">{{ unreadCount() }}</p>
      </article>
      <article class="rounded-2xl bg-ink-950 p-6 text-white">
        <p class="text-sm font-semibold text-ink-300">Estado de cuenta</p>
        <p class="mt-3 text-xl font-bold">{{ auth.user()?.enabled ? 'Activa' : 'Inactiva' }}</p>
      </article>
    </div>

    <div class="mt-10 grid gap-4 sm:grid-cols-3">
      <a routerLink="/dashboard/solicitudes" class="rounded-xl border border-ink-200 bg-white p-5 font-bold hover:border-brand-300 hover:text-brand-700">
        + Crear solicitud
      </a>
      <a routerLink="/dashboard/mensajes" class="rounded-xl border border-ink-200 bg-white p-5 font-bold hover:border-brand-300 hover:text-brand-700">
        Mensajes
      </a>
      <a routerLink="/dashboard/notificaciones" class="rounded-xl border border-ink-200 bg-white p-5 font-bold hover:border-brand-300 hover:text-brand-700">
        Ver notificaciones
      </a>
    </div>

    <section class="mt-12">
      <div class="flex items-end justify-between gap-4">
        <div>
          <p class="eyebrow">Actividad</p>
          <h2 class="font-display text-3xl font-semibold">Solicitudes recientes</h2>
        </div>
        <a routerLink="/dashboard/solicitudes" class="text-sm font-bold text-brand-700">Ver todas →</a>
      </div>
      @if (loading()) {
        <app-loading-spinner />
      } @else if (requests()?.content?.length) {
        <div class="mt-6 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          @for (request of requests()!.content.slice(0, 3); track request.id) {
            <app-request-card [request]="request" />
          }
        </div>
      } @else {
        <div class="mt-6">
          <app-empty-state
            title="Aún no tienes solicitudes"
            message="Cuando necesites apoyo, podrás publicar un caso desde tu cuenta."
          >
            <a routerLink="/dashboard/solicitudes" class="btn-primary">Crear solicitud</a>
          </app-empty-state>
        </div>
      }
    </section>
  `,
})
export class DashboardHomePage implements OnInit {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  readonly requests = signal<PageResponse<BloodRequest> | null>(null);
  readonly unreadCount = signal(0);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.myRequests().subscribe({
      next: (requests) => this.requests.set(requests),
      error: () => this.loading.set(false),
      complete: () => this.loading.set(false),
    });
    this.api.notifications(0, true).subscribe({
      next: (notifications) => this.unreadCount.set(notifications.totalElements),
    });
  }
}

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [ReactiveFormsModule, BadgeComponent, LoadingSpinnerComponent, RouterLink],
  template: `
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Datos personales</p>
        <h1 class="font-display text-4xl font-semibold text-ink-950">Mi perfil</h1>
        <p class="mt-2 text-ink-600">Administra tu cuenta y tu información como donante.</p>
      </div>
      @if (profile()) {
        <app-badge [tone]="profile()!.availability === 'AVAILABLE' ? 'green' : 'amber'">
          {{ availabilityLabels[profile()!.availability] }}
        </app-badge>
      }
    </header>

    <section class="mt-8 rounded-2xl border border-ink-100 bg-white p-6 shadow-sm">
      <h2 class="font-display text-2xl font-semibold">Cuenta</h2>
      <dl class="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        <div>
          <dt class="text-xs font-bold uppercase tracking-wider text-ink-500">Nombre</dt>
          <dd class="mt-1 font-semibold">{{ auth.user()?.firstName }} {{ auth.user()?.lastName }}</dd>
        </div>
        <div>
          <dt class="text-xs font-bold uppercase tracking-wider text-ink-500">Correo</dt>
          <dd class="mt-1 font-semibold">{{ auth.user()?.email }}</dd>
        </div>
        <div>
          <dt class="text-xs font-bold uppercase tracking-wider text-ink-500">Tipo de cuenta</dt>
          <dd class="mt-1 font-semibold">{{ auth.user()?.role === 'DONOR' ? 'Donante' : 'Usuario' }}</dd>
        </div>
      </dl>
      <p class="mt-6 text-sm text-ink-500">
        ¿Quieres eliminar tu cuenta?
        <a routerLink="/eliminacion-de-cuenta" class="font-bold text-brand-700">Consulta el proceso</a>.
      </p>
    </section>

    @if (loading()) {
      <app-loading-spinner label="Cargando perfil…" />
    } @else {
      <section class="mt-7 rounded-2xl border border-ink-100 bg-white p-6 shadow-sm">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 class="font-display text-2xl font-semibold">
              {{ profile() ? 'Perfil de donante' : 'Conviértete en donante' }}
            </h2>
            <p class="mt-1 text-sm text-ink-600">
              {{ profile() ? 'Mantén estos datos actualizados.' : 'Completa tus datos para responder a casos compatibles.' }}
            </p>
          </div>
          @if (profile()) {
            <select
              class="form-control !w-auto"
              [value]="profile()!.availability"
              (change)="setAvailability($any($event.target).value)"
              aria-label="Disponibilidad"
            >
              <option value="AVAILABLE">Disponible</option>
              <option value="TEMPORARILY_UNAVAILABLE">No disponible temporalmente</option>
              <option value="INACTIVE">Inactivo</option>
            </select>
          }
        </div>

        <form class="mt-8 grid gap-5 sm:grid-cols-2" [formGroup]="form" (ngSubmit)="save()">
          <label>
            <span class="form-label">Tipo sanguíneo</span>
            <select formControlName="bloodType" class="form-control">
              <option value="">Selecciona</option>
              @for (type of bloodTypes; track type) {
                <option [value]="type">{{ type }}</option>
              }
            </select>
          </label>
          <label>
            <span class="form-label">Fecha de nacimiento</span>
            <input type="date" formControlName="birthDate" class="form-control" />
          </label>
          <label>
            <span class="form-label">Sexo</span>
            <select formControlName="sex" class="form-control">
              <option value="">Selecciona</option>
              <option value="FEMALE">Femenino</option>
              <option value="MALE">Masculino</option>
              <option value="OTHER">Otro</option>
            </select>
          </label>
          <label>
            <span class="form-label">Teléfono dominicano</span>
            <input type="tel" formControlName="phone" class="form-control" placeholder="(809) 555-0123" />
            <span class="mt-1 block text-xs text-ink-500">Prefijos aceptados: 809, 829 y 849.</span>
            @if (form.controls.phone.touched && form.controls.phone.invalid) {
              <span class="form-error">Escribe un número dominicano válido.</span>
            }
          </label>
          <label>
            <span class="form-label">Provincia</span>
            <select formControlName="provinceId" class="form-control" (change)="provinceChanged()">
              <option value="">Selecciona</option>
              @for (province of provinces(); track province.id) {
                <option [value]="province.id">{{ province.name }}</option>
              }
            </select>
          </label>
          <label>
            <span class="form-label">Municipio</span>
            <select formControlName="municipalityId" class="form-control">
              <option value="">Selecciona</option>
              @for (municipality of municipalities(); track municipality.id) {
                <option [value]="municipality.id">{{ municipality.name }}</option>
              }
            </select>
          </label>
          <label>
            <span class="form-label">Sector</span>
            <input formControlName="sector" class="form-control" />
          </label>
          <label>
            <span class="form-label">Dirección aproximada</span>
            <input formControlName="approximateAddress" class="form-control" />
          </label>
          <label>
            <span class="form-label">Última donación</span>
            <input type="date" formControlName="lastDonationDate" class="form-control" />
            <span class="mt-1 block text-xs text-ink-500">Déjalo vacío si nunca has donado.</span>
          </label>
          <div class="flex items-end">
            <button type="button" class="btn-secondary w-full" (click)="useLocation()">
              Usar mi ubicación aproximada
            </button>
          </div>
          <p class="sm:col-span-2 rounded-lg bg-amber-50 p-4 text-xs leading-relaxed text-amber-900">
            Tu tipo sanguíneo debe haber sido confirmado por un centro de salud. La elegibilidad para
            donar se determina en cada visita.
          </p>
          <button type="submit" class="btn-primary sm:col-span-2" [disabled]="saving()">
            {{ saving() ? 'Guardando…' : profile() ? 'Guardar cambios' : 'Crear perfil de donante' }}
          </button>
        </form>
      </section>
    }
  `,
})
export class ProfilePage implements OnInit {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly bloodTypes = BLOOD_TYPES;
  readonly provinces = signal<Province[]>([]);
  readonly municipalities = signal<Municipality[]>([]);
  readonly profile = signal<Donor | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly availabilityLabels: Record<Availability, string> = {
    AVAILABLE: 'Disponible',
    TEMPORARILY_UNAVAILABLE: 'No disponible temporalmente',
    INACTIVE: 'Inactivo',
  };
  private coordinates: { latitude: number; longitude: number } | null = null;
  readonly form = this.fb.nonNullable.group({
    bloodType: ['', Validators.required],
    birthDate: ['', Validators.required],
    sex: ['', Validators.required],
    phone: [this.auth.user()?.phone ?? '', [Validators.required, dominicanPhone]],
    provinceId: ['', Validators.required],
    municipalityId: ['', Validators.required],
    sector: [''],
    approximateAddress: [''],
    lastDonationDate: [''],
  });

  ngOnInit(): void {
    this.api.provinces().subscribe({
      next: (provinces) => this.provinces.set(provinces),
      error: () => this.toast.error('No pudimos cargar las provincias.'),
    });
    if (this.auth.user()?.role === 'DONOR') {
      this.api.myDonorProfile().subscribe({
        next: (profile) => this.populate(profile),
        error: () => this.loading.set(false),
        complete: () => this.loading.set(false),
      });
    } else {
      this.loading.set(false);
    }
  }

  provinceChanged(selectedMunicipalityId?: number): void {
    this.form.controls.municipalityId.setValue('');
    const provinceId = Number(this.form.controls.provinceId.value);
    if (!provinceId) {
      this.municipalities.set([]);
      return;
    }
    this.api.municipalities(provinceId).subscribe({
      next: (municipalities) => {
        this.municipalities.set(municipalities);
        if (selectedMunicipalityId) {
          this.form.controls.municipalityId.setValue(String(selectedMunicipalityId));
        }
      },
      error: () => this.toast.error('No pudimos cargar los municipios.'),
    });
  }

  useLocation(): void {
    navigator.geolocation?.getCurrentPosition(
      ({ coords }) => {
        this.coordinates = { latitude: coords.latitude, longitude: coords.longitude };
        this.toast.success('Guardaremos una ubicación aproximada con tu perfil.');
      },
      () => this.toast.error('Activa el permiso de ubicación para usar esta opción.'),
    );
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.error('Completa los campos obligatorios.');
      return;
    }
    const value = this.form.getRawValue();
    const payload: DonorPayload = {
      bloodType: value.bloodType as BloodType,
      birthDate: value.birthDate,
      sex: value.sex as Sex,
      phone: normalizePhone(value.phone),
      provinceId: Number(value.provinceId),
      municipalityId: Number(value.municipalityId),
      sector: value.sector || undefined,
      approximateAddress: value.approximateAddress || undefined,
      latitude: this.coordinates?.latitude ?? this.profile()?.latitude ?? null,
      longitude: this.coordinates?.longitude ?? this.profile()?.longitude ?? null,
      lastDonationDate: value.lastDonationDate || null,
    };
    this.saving.set(true);
    const wasCreating = !this.profile();
    const operation = this.profile() ? this.api.updateDonor(payload) : this.api.createDonor(payload);
    operation.subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.auth.me().subscribe({
          next: () => {
            this.toast.success(
              wasCreating
                ? 'Tu perfil de donante fue creado. Ya puedes responder solicitudes.'
                : 'Tu perfil de donante fue actualizado.',
            );
            const retorno = this.route.snapshot.queryParamMap.get('retorno');
            if (wasCreating && retorno?.startsWith('/')) {
              void this.router.navigateByUrl(retorno);
            }
          },
          error: () => {
            this.toast.success('Tu perfil de donante fue actualizado.');
          },
        });
      },
      error: (error) => {
        this.saving.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.saving.set(false),
    });
  }

  setAvailability(availability: Availability): void {
    this.api.updateAvailability(availability).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.toast.success('Tu disponibilidad fue actualizada.');
      },
      error: (error) => this.toast.error(apiErrorMessage(error)),
    });
  }

  private populate(profile: Donor): void {
    this.profile.set(profile);
    this.coordinates =
      profile.latitude != null && profile.longitude != null
        ? { latitude: profile.latitude, longitude: profile.longitude }
        : null;
    this.form.patchValue({
      bloodType: profile.bloodType,
      birthDate: profile.birthDate ?? '',
      sex: profile.sex ?? '',
      phone: profile.phone ?? '',
      provinceId: String(profile.provinceId),
      sector: profile.sector ?? '',
      approximateAddress: profile.approximateAddress ?? '',
      lastDonationDate: profile.lastDonationDate ?? '',
    });
    this.provinceChanged(profile.municipalityId);
  }
}

@Component({
  selector: 'app-my-requests-page',
  standalone: true,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    BadgeComponent,
    EmptyStateComponent,
    LoadingSpinnerComponent,
    ModalComponent,
    PaginationComponent,
  ],
  template: `
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Casos publicados</p>
        <h1 class="font-display text-4xl font-semibold text-ink-950">Mis solicitudes</h1>
        <p class="mt-2 text-ink-600">Crea y administra solicitudes de sangre.</p>
      </div>
      <button
        type="button"
        class="btn-primary"
        (click)="openCreate()"
      >
        + Nueva solicitud
      </button>
    </header>

    <div class="mt-6 flex flex-wrap items-end justify-between gap-4">
      <label class="w-full sm:w-72">
        <span class="form-label">Filtrar por estado</span>
        <select
          class="form-control"
          [value]="statusFilter()"
          (change)="changeStatusFilter($any($event.target).value)"
        >
          <option value="ACTIVE">Activas (abiertas y en progreso)</option>
          <option value="OPEN">Abiertas</option>
          <option value="IN_PROGRESS">En progreso</option>
          <option value="FULFILLED">Completadas</option>
          <option value="CANCELLED">Canceladas</option>
          <option value="EXPIRED">Vencidas</option>
          <option value="ALL">Todas</option>
        </select>
      </label>
      <p class="text-sm text-ink-500">
        {{ filteredRequests().length }}
        {{ filteredRequests().length === 1 ? 'solicitud' : 'solicitudes' }}
      </p>
    </div>

    @if (loading()) {
      <app-loading-spinner />
    } @else if (filteredRequests().length) {
      <div class="mt-8 grid gap-4">
        @for (request of filteredRequests(); track request.id) {
          <article class="rounded-2xl border border-ink-100 bg-white p-5 shadow-sm">
            <div class="flex flex-wrap items-start justify-between gap-4">
              <div>
                <div class="flex flex-wrap items-center gap-2">
                  <span class="text-lg font-black text-brand-700">{{ request.bloodType }}</span>
                  <app-badge
                    [tone]="
                      request.status === 'FULFILLED' || request.status === 'OPEN'
                        ? 'green'
                        : request.status === 'IN_PROGRESS'
                          ? 'amber'
                          : 'neutral'
                    "
                  >
                    {{ statusLabels[request.status] }}
                  </app-badge>
                </div>
                <h2 class="mt-2 text-lg font-bold">{{ request.hospital }}</h2>
                <p class="mt-1 text-sm text-ink-500">
                  {{ request.municipalityName }} · vence {{ request.deadline | date: 'mediumDate' }}
                </p>
              </div>
              <div class="flex flex-wrap gap-2">
                <a [routerLink]="['/solicitudes', requestSlug(request)]" class="btn-secondary">
                  Ver
                </a>
                @if (request.status === 'OPEN' || request.status === 'IN_PROGRESS') {
                  <button type="button" class="btn-secondary" (click)="openEdit(request)">
                    Actualizar
                  </button>
                  <button type="button" class="btn-secondary !text-brand-700" (click)="cancel(request)">
                    Cancelar
                  </button>
                }
              </div>
            </div>
            <div class="mt-4 h-3 overflow-hidden rounded-full bg-ink-100">
              <div
                class="h-full rounded-full bg-brand-600"
                [style.width.%]="progressPercent(request)"
              ></div>
            </div>
            <div class="mt-2 flex items-center justify-between gap-3 text-sm font-semibold text-ink-700">
              <span>{{ request.completedUnits }} de {{ request.unitsRequired }} unidades recibidas</span>
              <span class="text-brand-700">{{ progressPercent(request) }}%</span>
            </div>
          </article>
        }
      </div>
      <app-pagination
        [page]="page()!.page"
        [totalPages]="page()!.totalPages"
        (changed)="load($event)"
      />
    } @else {
      <div class="mt-8">
        <app-empty-state
          [title]="statusFilter() === 'ACTIVE' ? 'No tienes solicitudes activas' : 'No hay solicitudes con este estado'"
          [message]="statusFilter() === 'ACTIVE' ? 'No tienes solicitudes abiertas ni en progreso.' : 'Selecciona otro estado para consultar tus solicitudes.'"
        >
          <button type="button" class="btn-primary" (click)="modalOpen.set(true)">Crear solicitud</button>
        </app-empty-state>
      </div>
    }

    <app-modal
      [open]="modalOpen()"
      [title]="editingRequest() ? 'Actualizar solicitud de sangre': 'Nueva solicitud de sangre'" (closed)="closeModal()">
      <form
        class="grid gap-4 sm:grid-cols-2"
        [formGroup]="form"
        (ngSubmit)="save()"
      >
        <label class="sm:col-span-2">
          <span class="form-label">Nombre del paciente</span>
          <input formControlName="patientName" class="form-control" />
        </label>
        <label>
          <span class="form-label">Tipo sanguíneo</span>
          <select formControlName="bloodType" class="form-control">
            <option value="">Selecciona</option>
            @for (type of bloodTypes; track type) {
              <option [value]="type">{{ type }}</option>
            }
          </select>
        </label>
        <label>
          <span class="form-label">Unidades requeridas</span>
          <input type="number" min="1" formControlName="unitsRequired" class="form-control" />
        </label>
        <label class="sm:col-span-2">
          <span class="form-label">Hospital o clínica</span>
          <input formControlName="hospital" class="form-control" />
        </label>
        <label>
          <span class="form-label">Provincia</span>
          <select formControlName="provinceId" class="form-control" (change)="provinceChanged()">
            <option value="">Selecciona</option>
            @for (province of provinces(); track province.id) {
              <option [value]="province.id">{{ province.name }}</option>
            }
          </select>
        </label>
        <label>
          <span class="form-label">Municipio</span>
          <select formControlName="municipalityId" class="form-control">
            <option value="">Selecciona</option>
            @for (municipality of municipalities(); track municipality.id) {
              <option [value]="municipality.id">{{ municipality.name }}</option>
            }
          </select>
        </label>
        <label>
          <span class="form-label">Sector</span>
          <input formControlName="sector" class="form-control" />
        </label>
        <label>
          <span class="form-label">Urgencia</span>
          <select formControlName="urgency" class="form-control">
            <option value="LOW">Baja</option>
            <option value="MEDIUM">Media</option>
            <option value="HIGH">Alta</option>
            <option value="CRITICAL">Crítica</option>
          </select>
        </label>
        <label class="sm:col-span-2">
          <span class="form-label">Dirección</span>
          <input formControlName="address" class="form-control" />
        </label>
        <label class="sm:col-span-2">
          <span class="form-label">Referencia</span>
          <input formControlName="reference" class="form-control" />
        </label>
        <label>
          <span class="form-label">Fecha límite</span>
          <input type="datetime-local" formControlName="deadline" class="form-control" />
        </label>
        <label>
          <span class="form-label">Teléfono de contacto</span>
          <input type="tel" formControlName="contactPhone" class="form-control" placeholder="(809) 555-0123" />
          @if (form.controls.contactPhone.touched && form.controls.contactPhone.invalid) {
            <span class="form-error">Escribe un número dominicano válido.</span>
          }
        </label>
        <label class="sm:col-span-2">
          <span class="form-label">Descripción</span>
          <textarea formControlName="description" rows="3" class="form-control"></textarea>
        </label>
        <button
          type="submit"
          class="btn-primary sm:col-span-2"
          [disabled]="saving()"
        >
          @if (saving()) {
            {{
              editingRequest()
                ? 'Actualizando…'
                : 'Publicando…'
            }}
          } @else {
            {{
              editingRequest()
                ? 'Guardar cambios'
                : 'Publicar solicitud'
            }}
          }
        </button>
      </form>
    </app-modal>
  `,
})
export class MyRequestsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  readonly requestSlug = bloodRequestSlug;
  readonly bloodTypes = BLOOD_TYPES;
  readonly page = signal<PageResponse<BloodRequest> | null>(null);
  readonly statusFilter = signal<'ACTIVE' | 'ALL' | RequestStatus>('ACTIVE');
  readonly filteredRequests = computed(() => {
    const requests = this.page()?.content ?? [];
    const selected = this.statusFilter();

    if (selected === 'ACTIVE') {
      return requests.filter(
        (request) => request.status === 'OPEN' || request.status === 'IN_PROGRESS',
      );
    }

    if (selected === 'ALL') {
      return requests;
    }

    return requests.filter((request) => request.status === selected);
  });
  readonly provinces = signal<Province[]>([]);
  readonly municipalities = signal<Municipality[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly modalOpen = signal(false);
  readonly editingRequest = signal<BloodRequest | null>(null);
  readonly statusLabels: Record<string, string> = {
    OPEN: 'Abierta',
    IN_PROGRESS: 'En progreso',
    FULFILLED: 'Completada',
    CANCELLED: 'Cancelada',
    EXPIRED: 'Vencida',
  };
  readonly form = this.fb.nonNullable.group({
    patientName: ['', Validators.required],
    bloodType: ['', Validators.required],
    unitsRequired: [1, [Validators.required, Validators.min(1)]],
    hospital: ['', Validators.required],
    provinceId: ['', Validators.required],
    municipalityId: ['', Validators.required],
    sector: [''],
    address: ['', Validators.required],
    reference: [''],
    deadline: [
      new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 16),
      Validators.required,
    ],
    description: [''],
    contactPhone: ['', [Validators.required, dominicanPhone]],
    urgency: ['HIGH', Validators.required],
  });

  ngOnInit(): void {
    this.api.provinces().subscribe({
      next: (provinces) => this.provinces.set(provinces),
      error: () => this.toast.error('No pudimos cargar las provincias.'),
    });
    this.load();
  }

  load(page = 0): void {
    this.loading.set(true);
    this.api.myRequests(page).subscribe({
      next: (response) => this.page.set(response),
      error: () => {
        this.loading.set(false);
        this.toast.error('No pudimos cargar tus solicitudes.');
      },
      complete: () => this.loading.set(false),
    });
  }

  changeStatusFilter(value: string): void {
    this.statusFilter.set(value as 'ACTIVE' | 'ALL' | RequestStatus);
  }

  provinceChanged(): void {
    this.form.controls.municipalityId.setValue('');
    const provinceId = Number(this.form.controls.provinceId.value);
    if (!provinceId) {
      this.municipalities.set([]);
      return;
    }
    this.api.municipalities(provinceId).subscribe({
      next: (municipalities) => this.municipalities.set(municipalities),
      error: () => this.toast.error('No pudimos cargar los municipios.'),
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();

      this.toast.error(
        'Completa los campos obligatorios.',
      );

      return;
    }

    const value = this.form.getRawValue();

    const payload: BloodRequestPayload = {
      patientName: value.patientName,
      bloodType: value.bloodType as BloodType,
      unitsRequired: value.unitsRequired,
      hospital: value.hospital,
      provinceId: Number(value.provinceId),
      municipalityId: Number(
        value.municipalityId,
      ),
      sector: value.sector || undefined,
      address: value.address,
      reference: value.reference || undefined,
      deadline: new Date(
        value.deadline,
      ).toISOString(),
      description:
        value.description || undefined,
      contactPhone: normalizePhone(
        value.contactPhone,
      ),
      urgency: value.urgency as Urgency,
    };

    const editing = this.editingRequest();

    const operation = editing
      ? this.api.updateRequest(
        editing.id,
        payload,
      )
      : this.api.createRequest(payload);

    this.saving.set(true);

    operation.subscribe({
      next: () => {
        this.toast.success(
          editing
            ? 'La solicitud fue actualizada.'
            : 'La solicitud fue publicada.',
        );

        this.closeModal();

        this.load(
          this.page()?.page ?? 0,
        );
      },
      error: (error) => {
        this.saving.set(false);

        this.toast.error(
          apiErrorMessage(error),
        );
      },
      complete: () => {
        this.saving.set(false);
      },
    });
  }

  cancel(request: BloodRequest): void {
    if (!window.confirm('¿Quieres cancelar esta solicitud?')) return;
    this.api.cancelRequest(request.id).subscribe({
      next: () => {
        this.toast.success('La solicitud fue cancelada.');
        this.load(this.page()?.page ?? 0);
      },
      error: (error) => this.toast.error(apiErrorMessage(error)),
    });
  }

  progressPercent(request: BloodRequest): number {
    return requestProgressPercent(request);
  }

  openCreate(): void {
    this.editingRequest.set(null);
    this.municipalities.set([]);
    this.resetForm();
    this.modalOpen.set(true);
  }

  openEdit(request: BloodRequest): void {
    this.editingRequest.set(request);
    this.modalOpen.set(true);

    this.form.patchValue({
      patientName: request.patientName,
      bloodType: request.bloodType,
      unitsRequired: request.unitsRequired,
      hospital: request.hospital,
      provinceId: String(request.provinceId),
      municipalityId: '',
      sector: request.sector ?? '',
      address: request.address,
      reference: request.reference ?? '',
      deadline: this.toDateTimeLocal(
        request.deadline,
      ),
      description: request.description ?? '',
      contactPhone: request.contactPhone,
      urgency: request.urgency,
    });

    this.api
      .municipalities(request.provinceId)
      .subscribe({
        next: (municipalities) => {
          this.municipalities.set(municipalities);

          this.form.controls.municipalityId.setValue(
            String(request.municipalityId),
          );
        },
        error: () => {
          this.toast.error(
            'No pudimos cargar los municipios.',
          );
        },
      });
  }

  private resetForm(): void {
    this.form.reset({
      patientName: '',
      bloodType: '',
      unitsRequired: 1,
      hospital: '',
      provinceId: '',
      municipalityId: '',
      sector: '',
      address: '',
      reference: '',
      deadline: new Date(
        Date.now() + 3 * 86400000,
      )
        .toISOString()
        .slice(0, 16),
      description: '',
      contactPhone: '',
      urgency: 'HIGH',
    });
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.editingRequest.set(null);
    this.municipalities.set([]);
    this.resetForm();
  }

  private toDateTimeLocal(value: string): string {
    const date = new Date(value);

    const offset = date.getTimezoneOffset() * 60000;

    return new Date(date.getTime() - offset)
      .toISOString()
      .slice(0, 16);
  }
}

@Component({
  selector: 'app-my-donations-page',
  standalone: true,
  imports: [DatePipe, DonationCardComponent, EmptyStateComponent, LoadingSpinnerComponent, RouterLink],
  template: `
    <header>
      <p class="eyebrow">Historial</p>
      <h1 class="font-display text-4xl font-semibold text-ink-950">Mis donaciones</h1>
      <p class="mt-2 text-ink-600">Consulta las donaciones que has reportado y su estado de confirmación.</p>
    </header>

    @if (loading()) {
      <app-loading-spinner label="Cargando historial…" />
    } @else if (history()) {
      <div class="mt-8 grid gap-4 sm:grid-cols-3">
        <article class="rounded-2xl bg-brand-600 p-6 text-white">
          <p class="text-sm text-brand-100">Donaciones completadas</p>
          <p class="mt-2 font-display text-4xl font-semibold">{{ history()!.totalDonations }}</p>
        </article>
        <article class="rounded-2xl bg-white p-6 shadow-sm">
          <p class="text-sm text-ink-500">Unidades donadas</p>
          <p class="mt-2 font-display text-4xl font-semibold">{{ history()!.totalUnits }}</p>
        </article>
        <article class="rounded-2xl bg-ink-950 p-6 text-white">
          <p class="text-sm text-ink-300">Próxima fecha estimada</p>
          <p class="mt-2 text-xl font-bold">
            {{ history()!.estimatedNextDate ? (history()!.estimatedNextDate | date: 'mediumDate') : 'Consulta en el centro' }}
          </p>
        </article>
      </div>
      <p class="mt-6 rounded-xl border-l-4 border-amber-400 bg-amber-50 p-5 text-sm leading-relaxed text-amber-900">
        {{ history()!.orientationNote }}
      </p>
      @if (history()!.history.length) {
        <div class="mt-8 grid gap-4 md:grid-cols-2">
          @for (donation of history()!.history; track donation.id) {
            <app-donation-card [donation]="donation" />
          }
        </div>
      } @else {
        <div class="mt-8">
          <app-empty-state
            title="No hay donaciones registradas"
            message="Cuando pulses “Ya doné” en una solicitud, tu reporte aparecerá aquí."
          >
            <a routerLink="/centros" class="btn-primary">Buscar un centro</a>
          </app-empty-state>
        </div>
      }
    }
  `,
})
export class MyDonationsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  readonly history = signal<DonationHistory | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.donationHistory().subscribe({
      next: (history) => this.history.set(history),
      error: (error) => {
        this.loading.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.loading.set(false),
    });
  }
}

@Component({
  selector: 'app-notifications-page',
  standalone: true,
  imports: [DatePipe, EmptyStateComponent, LoadingSpinnerComponent, PaginationComponent],
  template: `
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Actualizaciones</p>
        <h1 class="font-display text-4xl font-semibold text-ink-950">Notificaciones</h1>
        <p class="mt-2 text-ink-600">Mantente al día con tus solicitudes y donaciones.</p>
      </div>
      <button type="button" class="btn-secondary" (click)="markAllRead()">Marcar todas como leídas</button>
    </header>

    @if (loading()) {
      <app-loading-spinner />
    } @else if (page()?.content?.length) {
      <div class="mt-8 overflow-hidden rounded-2xl border border-ink-100 bg-white shadow-sm">
        @for (notification of page()!.content; track notification.id) {
          <button
            type="button"
            class="flex w-full items-start gap-4 border-b border-ink-100 p-5 text-left last:border-0 hover:bg-ink-50"
            [class.bg-brand-50]="!notification.read"
            (click)="open(notification)"
          >
            <span
              class="mt-1 h-2.5 w-2.5 shrink-0 rounded-full"
              [class]="notification.read ? 'bg-ink-200' : 'bg-brand-600'"
            ></span>
            <span class="flex-1">
              <strong class="block text-ink-950">{{ notification.title }}</strong>
              <span class="mt-1 block text-sm leading-relaxed text-ink-600">{{ notification.message }}</span>
              <small class="mt-2 block text-ink-400">{{ notification.createdAt | date: 'medium' }}</small>
            </span>
          </button>
        }
      </div>
      <app-pagination
        [page]="page()!.page"
        [totalPages]="page()!.totalPages"
        (changed)="load($event)"
      />
    } @else {
      <div class="mt-8">
        <app-empty-state
          title="No tienes notificaciones"
          message="Aquí aparecerán las novedades relacionadas con tu actividad."
        />
      </div>
    }
  `,
})
export class NotificationsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  readonly page = signal<PageResponse<Notification> | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    this.loading.set(true);
    this.api.notifications(page).subscribe({
      next: (response) => this.page.set(response),
      error: () => {
        this.loading.set(false);
        this.toast.error('No pudimos cargar tus notificaciones.');
      },
      complete: () => this.loading.set(false),
    });
  }

  open(notification: Notification): void {
    this.markRead(notification);
    if (notification.resourceType === 'CONVERSATION' && notification.resourceId) {
      void this.router.navigate(['/dashboard/mensajes', notification.resourceId]);
      return;
    }
    if (notification.resourceType === 'BLOOD_REQUEST' && notification.resourceId) {
      void this.router.navigate(['/solicitudes', notification.resourceId]);
    }
  }

  markRead(notification: Notification): void {
    if (notification.read) return;
    this.api.markNotificationRead(notification.id).subscribe({
      next: (updated) => {
        this.page.update((page) =>
          page
            ? {
              ...page,
              content: page.content.map((item) => (item.id === updated.id ? updated : item)),
            }
            : page,
        );
      },
      error: () => this.toast.error('No pudimos actualizar la notificación.'),
    });
  }

  markAllRead(): void {
    this.api.markAllNotificationsRead().subscribe({
      next: () => {
        this.page.update((page) =>
          page ? { ...page, content: page.content.map((item) => ({ ...item, read: true })) } : page,
        );
        this.toast.success('Todas las notificaciones fueron marcadas como leídas.');
      },
      error: () => this.toast.error('No pudimos actualizar las notificaciones.'),
    });
  }
}
