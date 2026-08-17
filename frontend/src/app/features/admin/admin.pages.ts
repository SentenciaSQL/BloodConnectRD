import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import {
  BLOOD_TYPES,
  BloodRequest,
  CenterPayload,
  CenterType,
  DashboardStatistics,
  Donation,
  DonationCenter,
  Donor,
  Municipality,
  PageResponse,
  Province,
  Role,
  User,
  donationStatusLabel,
  donationStatusTone,
  requestProgressPercent,
} from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import { apiErrorMessage } from '../../core/services/auth.service';
import { bloodRequestSlug } from '../../core/seo/request-slug';
import { ToastService } from '../../core/services/toast.service';
import {
  BadgeComponent,
  EmptyStateComponent,
  LoadingSpinnerComponent,
  ModalComponent,
  PaginationComponent,
} from '../../shared/components/ui-components';

@Component({
  selector: 'app-admin-home-page',
  standalone: true,
  imports: [RouterLink, LoadingSpinnerComponent],
  template: `
    <header>
      <p class="eyebrow">Panel administrativo</p>
      <h1 class="font-display text-4xl font-semibold text-ink-950">Resumen general</h1>
      <p class="mt-2 text-ink-600">Indicadores actuales de BloodConnect RD.</p>
    </header>
    @if (loading()) {
      <app-loading-spinner label="Cargando indicadores…" />
    } @else if (stats()) {
      <div class="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        @for (card of cards(); track card.label) {
          <article class="rounded-2xl border border-ink-100 bg-white p-6 shadow-sm">
            <p class="text-sm font-semibold text-ink-500">{{ card.label }}</p>
            <p class="mt-3 font-display text-4xl font-semibold text-ink-950">{{ card.value }}</p>
            <p class="mt-2 text-xs font-bold text-brand-700">{{ card.note }}</p>
          </article>
        }
      </div>

      <div class="mt-8 grid gap-6 xl:grid-cols-[1.4fr_1fr]">
        <section class="rounded-2xl border border-ink-100 bg-white p-6 shadow-sm">
          <div class="flex items-center justify-between">
            <div>
              <p class="eyebrow">Distribución</p>
              <h2 class="font-display text-2xl font-semibold">Donantes por tipo sanguíneo</h2>
            </div>
            <a routerLink="/admin/estadisticas" class="text-sm font-bold text-brand-700">Ver detalle</a>
          </div>
          <div class="mt-7 grid gap-4">
            @for (entry of entries(stats()!.bloodTypeDistribution); track entry[0]) {
              <div class="grid grid-cols-[2.5rem_1fr_2.5rem] items-center gap-3">
                <span class="font-black text-brand-700">{{ entry[0] }}</span>
                <div class="h-3 overflow-hidden rounded-full bg-ink-100">
                  <div class="h-full rounded-full bg-brand-600" [style.width.%]="barWidth(entry[1])"></div>
                </div>
                <span class="text-right text-sm font-bold">{{ entry[1] }}</span>
              </div>
            }
          </div>
        </section>
        <section class="rounded-2xl bg-ink-950 p-6 text-white">
          <p class="eyebrow !text-brand-300">Acciones rápidas</p>
          <h2 class="font-display text-2xl font-semibold">Administrar plataforma</h2>
          <nav class="mt-6 grid gap-2">
            <a routerLink="/admin/usuarios" class="rounded-lg bg-ink-900 px-4 py-3 text-sm font-semibold hover:bg-ink-800">
              Revisar usuarios →
            </a>
            <a routerLink="/admin/solicitudes" class="rounded-lg bg-ink-900 px-4 py-3 text-sm font-semibold hover:bg-ink-800">
              Revisar solicitudes →
            </a>
            <a routerLink="/admin/centros" class="rounded-lg bg-ink-900 px-4 py-3 text-sm font-semibold hover:bg-ink-800">
              Gestionar centros →
            </a>
          </nav>
        </section>
      </div>
    }
  `,
})
export class AdminHomePage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  readonly stats = signal<DashboardStatistics | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.dashboardStatistics().subscribe({
      next: (stats) => this.stats.set(stats),
      error: (error) => {
        this.loading.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.loading.set(false),
    });
  }

  cards() {
    const stats = this.stats();
    return stats
      ? [
          { label: 'Usuarios', value: stats.users, note: 'Cuentas registradas' },
          { label: 'Donantes', value: stats.donors, note: `${stats.availableDonors} disponibles` },
          { label: 'Solicitudes abiertas', value: stats.openRequests, note: 'Requieren seguimiento' },
          { label: 'Solicitudes completadas', value: stats.fulfilledRequests, note: 'Casos atendidos' },
          { label: 'Donaciones', value: stats.donations, note: 'Registros totales' },
          {
            label: 'Disponibilidad',
            value: stats.donors ? `${Math.round((stats.availableDonors / stats.donors) * 100)} %` : '0 %',
            note: 'Donantes activos',
          },
        ]
      : [];
  }

  entries(record: Record<string, number>): [string, number][] {
    return Object.entries(record);
  }

  barWidth(value: number): number {
    const max = Math.max(...Object.values(this.stats()?.bloodTypeDistribution ?? {}), 1);
    return (value / max) * 100;
  }
}

@Component({
  selector: 'app-admin-users-page',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, BadgeComponent, LoadingSpinnerComponent, PaginationComponent],
  template: `
    <header>
      <p class="eyebrow">Administración</p>
      <h1 class="font-display text-4xl font-semibold">Usuarios</h1>
      <p class="mt-2 text-ink-600">Consulta cuentas y controla su estado de acceso.</p>
    </header>

    <form [formGroup]="filters" (ngSubmit)="load(0)" class="mt-8 grid gap-4 rounded-2xl bg-white p-5 shadow-sm md:grid-cols-4">
      <label class="md:col-span-2">
        <span class="form-label">Buscar</span>
        <input formControlName="search" class="form-control" placeholder="Nombre o correo" />
      </label>
      <label>
        <span class="form-label">Rol</span>
        <select formControlName="role" class="form-control">
          <option value="">Todos</option>
          <option value="USER">Usuario</option>
          <option value="DONOR">Donante</option>
          <option value="ADMIN">Administrador</option>
        </select>
      </label>
      <div class="flex items-end"><button type="submit" class="btn-primary w-full">Buscar</button></div>
    </form>

    @if (loading()) {
      <app-loading-spinner />
    } @else {
      <div class="mt-7 overflow-x-auto rounded-2xl border border-ink-100 bg-white shadow-sm">
        <table class="w-full min-w-[780px] text-left text-sm">
          <thead class="bg-ink-50 text-xs uppercase tracking-wider text-ink-500">
            <tr>
              <th class="px-5 py-4">Usuario</th>
              <th class="px-5 py-4">Teléfono</th>
              <th class="px-5 py-4">Rol</th>
              <th class="px-5 py-4">Registro</th>
              <th class="px-5 py-4">Estado</th>
              <th class="px-5 py-4 text-right">Acción</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-ink-100">
            @for (user of page()?.content ?? []; track user.id) {
              <tr>
                <td class="px-5 py-4">
                  <strong class="block text-ink-950">{{ user.firstName }} {{ user.lastName }}</strong>
                  <span class="text-xs text-ink-500">{{ user.email }}</span>
                </td>
                <td class="px-5 py-4">{{ user.phone }}</td>
                <td class="px-5 py-4">{{ roleLabels[user.role] }}</td>
                <td class="px-5 py-4">{{ user.createdAt | date: 'mediumDate' }}</td>
                <td class="px-5 py-4">
                  <app-badge [tone]="user.enabled ? 'green' : 'red'">
                    {{ user.enabled ? 'Activa' : 'Inactiva' }}
                  </app-badge>
                </td>
                <td class="px-5 py-4 text-right">
                  <button type="button" class="btn-secondary" (click)="toggle(user)">
                    {{ user.enabled ? 'Desactivar' : 'Activar' }}
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <app-pagination
        [page]="page()?.page ?? 0"
        [totalPages]="page()?.totalPages ?? 0"
        (changed)="load($event)"
      />
    }
  `,
})
export class AdminUsersPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  readonly page = signal<PageResponse<User> | null>(null);
  readonly loading = signal(true);
  readonly roleLabels: Record<Role, string> = {
    USER: 'Usuario',
    DONOR: 'Donante',
    ADMIN: 'Administrador',
  };
  readonly filters = this.fb.nonNullable.group({ search: [''], role: [''] });

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    this.loading.set(true);
    const value = this.filters.getRawValue();
    this.api.adminUsers({ search: value.search, role: value.role as Role | '', page }).subscribe({
      next: (response) => this.page.set(response),
      error: (error) => {
        this.loading.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.loading.set(false),
    });
  }

  toggle(user: User): void {
    this.api.setUserEnabled(user.id, !user.enabled).subscribe({
      next: (updated) => {
        this.page.update((page) =>
          page
            ? {
                ...page,
                content: page.content.map((item) => (item.id === updated.id ? updated : item)),
              }
            : page,
        );
        this.toast.success(`La cuenta fue ${updated.enabled ? 'activada' : 'desactivada'}.`);
      },
      error: (error) => this.toast.error(apiErrorMessage(error)),
    });
  }
}

@Component({
  selector: 'app-admin-donors-page',
  standalone: true,
  imports: [ReactiveFormsModule, BadgeComponent, LoadingSpinnerComponent, PaginationComponent],
  template: `
    <header>
      <p class="eyebrow">Administración</p>
      <h1 class="font-display text-4xl font-semibold">Donantes</h1>
      <p class="mt-2 text-ink-600">Supervisa disponibilidad, tipo sanguíneo y ubicación.</p>
    </header>
    <form [formGroup]="filters" (ngSubmit)="load(0)" class="mt-8 grid gap-4 rounded-2xl bg-white p-5 shadow-sm md:grid-cols-3">
      <label>
        <span class="form-label">Tipo sanguíneo</span>
        <select formControlName="bloodType" class="form-control">
          <option value="">Todos</option>
          @for (type of bloodTypes; track type) { <option [value]="type">{{ type }}</option> }
        </select>
      </label>
      <label>
        <span class="form-label">Disponibilidad</span>
        <select formControlName="availability" class="form-control">
          <option value="">Todos</option>
          <option value="AVAILABLE">Disponible</option>
          <option value="TEMPORARILY_UNAVAILABLE">No disponible temporalmente</option>
          <option value="INACTIVE">Inactivo</option>
        </select>
      </label>
      <div class="flex items-end"><button type="submit" class="btn-primary w-full">Aplicar filtros</button></div>
    </form>
    @if (loading()) {
      <app-loading-spinner />
    } @else {
      <div class="mt-7 overflow-x-auto rounded-2xl border border-ink-100 bg-white shadow-sm">
        <table class="w-full min-w-[680px] text-left text-sm">
          <thead class="bg-ink-50 text-xs uppercase tracking-wider text-ink-500">
            <tr>
              <th class="px-5 py-4">Identificador</th>
              <th class="px-5 py-4">Tipo</th>
              <th class="px-5 py-4">Ubicación</th>
              <th class="px-5 py-4">Última donación</th>
              <th class="px-5 py-4">Disponibilidad</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-ink-100">
            @for (donor of page()?.content ?? []; track donor.id) {
              <tr>
                <td class="px-5 py-4 font-bold">Donante #{{ donor.id }}</td>
                <td class="px-5 py-4 text-lg font-black text-brand-700">{{ donor.bloodType }}</td>
                <td class="px-5 py-4">{{ donor.municipalityName }}, {{ donor.provinceName }}</td>
                <td class="px-5 py-4">{{ donor.lastDonationDate || 'Sin registro' }}</td>
                <td class="px-5 py-4">
                  <app-badge [tone]="donor.availability === 'AVAILABLE' ? 'green' : 'amber'">
                    {{ availabilityLabels[donor.availability] }}
                  </app-badge>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <app-pagination
        [page]="page()?.page ?? 0"
        [totalPages]="page()?.totalPages ?? 0"
        (changed)="load($event)"
      />
    }
  `,
})
export class AdminDonorsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  readonly bloodTypes = BLOOD_TYPES;
  readonly page = signal<PageResponse<Donor> | null>(null);
  readonly loading = signal(true);
  readonly availabilityLabels: Record<string, string> = {
    AVAILABLE: 'Disponible',
    TEMPORARILY_UNAVAILABLE: 'No disponible temporalmente',
    INACTIVE: 'Inactivo',
  };
  readonly filters = this.fb.nonNullable.group({ bloodType: [''], availability: [''] });

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    this.loading.set(true);
    this.api.donors({ ...this.filters.getRawValue(), page, size: 20 }).subscribe({
      next: (response) => this.page.set(response),
      error: (error) => {
        this.loading.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.loading.set(false),
    });
  }
}

@Component({
  selector: 'app-admin-requests-page',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, RouterLink, BadgeComponent, LoadingSpinnerComponent, PaginationComponent],
  template: `
    <header>
      <p class="eyebrow">Administración</p>
      <h1 class="font-display text-4xl font-semibold">Solicitudes</h1>
      <p class="mt-2 text-ink-600">Consulta el estado y prioridad de los casos publicados.</p>
    </header>
    <form [formGroup]="filters" (ngSubmit)="load(0)" class="mt-8 grid gap-4 rounded-2xl bg-white p-5 shadow-sm md:grid-cols-4">
      <label>
        <span class="form-label">Buscar</span>
        <input formControlName="search" class="form-control" placeholder="Hospital o paciente" />
      </label>
      <label>
        <span class="form-label">Estado</span>
        <select formControlName="status" class="form-control">
          <option value="">Todos</option>
          <option value="OPEN">Abierta</option>
          <option value="IN_PROGRESS">En progreso</option>
          <option value="FULFILLED">Completada</option>
          <option value="CANCELLED">Cancelada</option>
          <option value="EXPIRED">Vencida</option>
        </select>
      </label>
      <label>
        <span class="form-label">Urgencia</span>
        <select formControlName="urgency" class="form-control">
          <option value="">Todas</option>
          <option value="CRITICAL">Crítica</option>
          <option value="HIGH">Alta</option>
          <option value="MEDIUM">Media</option>
          <option value="LOW">Baja</option>
        </select>
      </label>
      <div class="flex items-end"><button type="submit" class="btn-primary w-full">Buscar</button></div>
    </form>
    @if (loading()) {
      <app-loading-spinner />
    } @else {
      <div class="mt-7 overflow-x-auto rounded-2xl border border-ink-100 bg-white shadow-sm">
        <table class="w-full min-w-[860px] text-left text-sm">
          <thead class="bg-ink-50 text-xs uppercase tracking-wider text-ink-500">
            <tr>
              <th class="px-5 py-4">Caso</th>
              <th class="px-5 py-4">Tipo</th>
              <th class="px-5 py-4">Ubicación</th>
              <th class="px-5 py-4">Progreso</th>
              <th class="px-5 py-4">Fecha límite</th>
              <th class="px-5 py-4">Estado</th>
              <th class="px-5 py-4"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-ink-100">
            @for (request of page()?.content ?? []; track request.id) {
              <tr>
                <td class="px-5 py-4">
                  <strong class="block">{{ request.hospital }}</strong>
                  <span class="text-xs text-ink-500">{{ request.patientName }}</span>
                </td>
                <td class="px-5 py-4 text-lg font-black text-brand-700">{{ request.bloodType }}</td>
                <td class="px-5 py-4">{{ request.municipalityName }}, {{ request.provinceName }}</td>
                <td class="px-5 py-4">
                  {{ request.completedUnits }} de {{ request.unitsRequired }}
                  ({{ progressPercent(request) }}%)
                </td>
                <td class="px-5 py-4">{{ request.deadline | date: 'mediumDate' }}</td>
                <td class="px-5 py-4"><app-badge>{{ statusLabels[request.status] }}</app-badge></td>
                <td class="px-5 py-4">
                  <a [routerLink]="['/solicitudes', requestSlug(request)]" class="font-bold text-brand-700">Ver</a>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <app-pagination
        [page]="page()?.page ?? 0"
        [totalPages]="page()?.totalPages ?? 0"
        (changed)="load($event)"
      />
    }
  `,
})
export class AdminRequestsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  readonly requestSlug = bloodRequestSlug;
  readonly page = signal<PageResponse<BloodRequest> | null>(null);
  readonly loading = signal(true);
  readonly statusLabels: Record<string, string> = {
    OPEN: 'Abierta',
    IN_PROGRESS: 'En progreso',
    FULFILLED: 'Completada',
    CANCELLED: 'Cancelada',
    EXPIRED: 'Vencida',
  };
  readonly filters = this.fb.nonNullable.group({ search: [''], status: [''], urgency: [''] });

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    this.loading.set(true);
    this.api.requests({ ...this.filters.getRawValue(), page, size: 20 }).subscribe({
      next: (response) => this.page.set(response),
      error: (error) => {
        this.loading.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.loading.set(false),
    });
  }

  progressPercent(request: BloodRequest): number {
    return requestProgressPercent(request);
  }
}

@Component({
  selector: 'app-admin-donations-page',
  standalone: true,
  imports: [DatePipe, BadgeComponent, LoadingSpinnerComponent, PaginationComponent],
  template: `
    <header>
      <p class="eyebrow">Administración</p>
      <h1 class="font-display text-4xl font-semibold">Donaciones</h1>
      <p class="mt-2 text-ink-600">Historial consolidado de donaciones registradas.</p>
    </header>
    @if (loading()) {
      <app-loading-spinner />
    } @else {
      <div class="mt-8 overflow-x-auto rounded-2xl border border-ink-100 bg-white shadow-sm">
        <table class="w-full min-w-[760px] text-left text-sm">
          <thead class="bg-ink-50 text-xs uppercase tracking-wider text-ink-500">
            <tr>
              <th class="px-5 py-4">Donante</th>
              <th class="px-5 py-4">Centro</th>
              <th class="px-5 py-4">Fecha</th>
              <th class="px-5 py-4">Unidades</th>
              <th class="px-5 py-4">Confirmadas</th>
              <th class="px-5 py-4">Estado</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-ink-100">
            @for (donation of page()?.content ?? []; track donation.id) {
              <tr>
                <td class="px-5 py-4 font-semibold">{{ donation.donorName }}</td>
                <td class="px-5 py-4">{{ donation.donationCenterName || 'Vinculada a solicitud' }}</td>
                <td class="px-5 py-4">{{ donation.donationDate | date: 'mediumDate' }}</td>
                <td class="px-5 py-4">{{ donation.units }}</td>
                <td class="px-5 py-4">{{ donation.confirmedUnits }}</td>
                <td class="px-5 py-4">
                  <app-badge [tone]="statusTone(donation.status)">
                    {{ statusLabel(donation.status) }}
                  </app-badge>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <app-pagination
        [page]="page()?.page ?? 0"
        [totalPages]="page()?.totalPages ?? 0"
        (changed)="load($event)"
      />
    }
  `,
})
export class AdminDonationsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  readonly page = signal<PageResponse<Donation> | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    this.loading.set(true);
    this.api.adminDonations(page).subscribe({
      next: (response) => this.page.set(response),
      error: (error) => {
        this.loading.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.loading.set(false),
    });
  }

  statusLabel(status: string): string {
    return donationStatusLabel(status);
  }

  statusTone(status: string): 'red' | 'green' | 'amber' | 'neutral' {
    return donationStatusTone(status);
  }
}

@Component({
  selector: 'app-admin-centers-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    BadgeComponent,
    EmptyStateComponent,
    LoadingSpinnerComponent,
    ModalComponent,
    PaginationComponent,
  ],
  template: `
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Administración</p>
        <h1 class="font-display text-4xl font-semibold">Centros de donación</h1>
        <p class="mt-2 text-ink-600">Crea y actualiza los centros publicados en el directorio.</p>
      </div>
      <button type="button" class="btn-primary" (click)="newCenter()">+ Agregar centro</button>
    </header>
    @if (loading()) {
      <app-loading-spinner />
    } @else if (page()?.content?.length) {
      <div class="mt-8 overflow-x-auto rounded-2xl border border-ink-100 bg-white shadow-sm">
        <table class="w-full min-w-[800px] text-left text-sm">
          <thead class="bg-ink-50 text-xs uppercase tracking-wider text-ink-500">
            <tr>
              <th class="px-5 py-4">Centro</th>
              <th class="px-5 py-4">Tipo</th>
              <th class="px-5 py-4">Ubicación</th>
              <th class="px-5 py-4">Estado</th>
              <th class="px-5 py-4 text-right">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-ink-100">
            @for (center of page()!.content; track center.id) {
              <tr>
                <td class="px-5 py-4">
                  <strong class="block">{{ center.name }}</strong>
                  <span class="text-xs text-ink-500">{{ center.phone || 'Sin teléfono' }}</span>
                </td>
                <td class="px-5 py-4">{{ typeLabels[center.type] }}</td>
                <td class="px-5 py-4">{{ center.municipalityName }}, {{ center.provinceName }}</td>
                <td class="px-5 py-4">
                  <app-badge [tone]="center.active ? 'green' : 'red'">
                    {{ center.active ? 'Activo' : 'Inactivo' }}
                  </app-badge>
                </td>
                <td class="px-5 py-4 text-right">
                  <button type="button" class="font-bold text-brand-700" (click)="edit(center)">Editar</button>
                  <button type="button" class="ml-4 font-bold text-ink-500 hover:text-brand-700" (click)="remove(center)">
                    Eliminar
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <app-pagination
        [page]="page()!.page"
        [totalPages]="page()!.totalPages"
        (changed)="load($event)"
      />
    } @else {
      <div class="mt-8">
        <app-empty-state title="No hay centros" message="Agrega el primer centro al directorio.">
          <button type="button" class="btn-primary" (click)="newCenter()">Agregar centro</button>
        </app-empty-state>
      </div>
    }

    <app-modal
      [open]="modalOpen()"
      [title]="editing() ? 'Editar centro de donación' : 'Agregar centro de donación'"
      (closed)="modalOpen.set(false)"
    >
      <form class="grid gap-4 sm:grid-cols-2" [formGroup]="form" (ngSubmit)="save()">
        <label class="sm:col-span-2">
          <span class="form-label">Nombre</span>
          <input formControlName="name" class="form-control" />
        </label>
        <label>
          <span class="form-label">Tipo</span>
          <select formControlName="type" class="form-control">
            <option value="HOSPITAL">Hospital</option>
            <option value="CLINIC">Clínica</option>
            <option value="BLOOD_BANK">Banco de sangre</option>
            <option value="MEDICAL_CENTER">Centro médico</option>
            <option value="OTHER">Otro</option>
          </select>
        </label>
        <label>
          <span class="form-label">Teléfono</span>
          <input formControlName="phone" class="form-control" />
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
          <span class="form-label">Horario</span>
          <input formControlName="schedule" class="form-control" placeholder="Lun–Vie 8:00 AM – 5:00 PM" />
        </label>
        <label class="sm:col-span-2">
          <span class="form-label">Dirección</span>
          <input formControlName="address" class="form-control" />
        </label>
        <label class="sm:col-span-2">
          <span class="form-label">Referencia</span>
          <input formControlName="reference" class="form-control" />
        </label>
        <label class="flex items-center gap-3 text-sm font-semibold">
          <input type="checkbox" formControlName="active" class="h-4 w-4 accent-brand-600" />
          Mostrar como centro activo
        </label>
        <div class="flex justify-end">
          <button type="submit" class="btn-primary" [disabled]="saving()">
            {{ saving() ? 'Guardando…' : 'Guardar centro' }}
          </button>
        </div>
      </form>
    </app-modal>
  `,
})
export class AdminCentersPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  readonly page = signal<PageResponse<DonationCenter> | null>(null);
  readonly provinces = signal<Province[]>([]);
  readonly municipalities = signal<Municipality[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly modalOpen = signal(false);
  readonly editing = signal<DonationCenter | null>(null);
  readonly typeLabels: Record<CenterType, string> = {
    HOSPITAL: 'Hospital',
    CLINIC: 'Clínica',
    BLOOD_BANK: 'Banco de sangre',
    MEDICAL_CENTER: 'Centro médico',
    OTHER: 'Otro',
  };
  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    type: ['HOSPITAL', Validators.required],
    provinceId: ['', Validators.required],
    municipalityId: ['', Validators.required],
    sector: [''],
    address: ['', Validators.required],
    reference: [''],
    phone: [''],
    schedule: [''],
    active: [true],
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
    this.api.centers({ page, size: 20 }).subscribe({
      next: (response) => this.page.set(response),
      error: (error) => {
        this.loading.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.loading.set(false),
    });
  }

  provinceChanged(municipalityId?: number): void {
    this.form.controls.municipalityId.setValue('');
    const provinceId = Number(this.form.controls.provinceId.value);
    if (!provinceId) {
      this.municipalities.set([]);
      return;
    }
    this.api.municipalities(provinceId).subscribe({
      next: (municipalities) => {
        this.municipalities.set(municipalities);
        if (municipalityId) this.form.controls.municipalityId.setValue(String(municipalityId));
      },
      error: () => this.toast.error('No pudimos cargar los municipios.'),
    });
  }

  newCenter(): void {
    this.editing.set(null);
    this.municipalities.set([]);
    this.form.reset({
      name: '',
      type: 'HOSPITAL',
      provinceId: '',
      municipalityId: '',
      sector: '',
      address: '',
      reference: '',
      phone: '',
      schedule: '',
      active: true,
    });
    this.modalOpen.set(true);
  }

  edit(center: DonationCenter): void {
    this.editing.set(center);
    this.form.patchValue({
      name: center.name,
      type: center.type,
      provinceId: String(center.provinceId),
      sector: center.sector ?? '',
      address: center.address,
      reference: center.reference ?? '',
      phone: center.phone ?? '',
      schedule: center.schedule ?? '',
      active: center.active,
    });
    this.provinceChanged(center.municipalityId);
    this.modalOpen.set(true);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.error('Completa los campos obligatorios.');
      return;
    }
    const value = this.form.getRawValue();
    const payload: CenterPayload = {
      name: value.name,
      type: value.type as CenterType,
      provinceId: Number(value.provinceId),
      municipalityId: Number(value.municipalityId),
      sector: value.sector || undefined,
      address: value.address,
      reference: value.reference || undefined,
      phone: value.phone || undefined,
      schedule: value.schedule || undefined,
      latitude: this.editing()?.latitude,
      longitude: this.editing()?.longitude,
      active: value.active,
    };
    this.saving.set(true);
    const operation = this.editing()
      ? this.api.updateCenter(this.editing()!.id, payload)
      : this.api.createCenter(payload);
    operation.subscribe({
      next: () => {
        this.toast.success('El centro fue guardado.');
        this.modalOpen.set(false);
        this.load(this.page()?.page ?? 0);
      },
      error: (error) => {
        this.saving.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.saving.set(false),
    });
  }

  remove(center: DonationCenter): void {
    if (!window.confirm(`¿Quieres eliminar “${center.name}”?`)) return;
    this.api.deleteCenter(center.id).subscribe({
      next: () => {
        this.toast.success('El centro fue eliminado.');
        this.load(this.page()?.page ?? 0);
      },
      error: (error) => this.toast.error(apiErrorMessage(error)),
    });
  }
}

@Component({
  selector: 'app-admin-statistics-page',
  standalone: true,
  imports: [LoadingSpinnerComponent],
  template: `
    <header>
      <p class="eyebrow">Datos de plataforma</p>
      <h1 class="font-display text-4xl font-semibold">Estadísticas</h1>
      <p class="mt-2 text-ink-600">Distribuciones y tendencias basadas en los registros actuales.</p>
    </header>
    @if (loading()) {
      <app-loading-spinner label="Preparando estadísticas…" />
    } @else if (stats()) {
      <div class="mt-8 grid gap-6 xl:grid-cols-2">
        <section class="rounded-2xl bg-white p-6 shadow-sm">
          <p class="eyebrow">Donantes</p>
          <h2 class="font-display text-2xl font-semibold">Tipos sanguíneos</h2>
          <div class="mt-7 grid grid-cols-2 gap-4 sm:grid-cols-4">
            @for (entry of entries(stats()!.bloodTypeDistribution); track entry[0]) {
              <div class="rounded-xl bg-brand-50 p-4 text-center">
                <p class="text-xl font-black text-brand-700">{{ entry[0] }}</p>
                <p class="mt-1 font-display text-3xl font-semibold">{{ entry[1] }}</p>
              </div>
            }
          </div>
        </section>

        <section class="rounded-2xl bg-white p-6 shadow-sm">
          <p class="eyebrow">Solicitudes</p>
          <h2 class="font-display text-2xl font-semibold">Por provincia</h2>
          <div class="mt-7 grid gap-4">
            @for (entry of entries(stats()!.requestsByProvince); track entry[0]) {
              <div>
                <div class="mb-1.5 flex justify-between gap-3 text-sm">
                  <span class="font-semibold">{{ entry[0] }}</span><strong>{{ entry[1] }}</strong>
                </div>
                <div class="h-3 overflow-hidden rounded-full bg-ink-100">
                  <div class="h-full rounded-full bg-brand-600" [style.width.%]="width(entry[1], stats()!.requestsByProvince)"></div>
                </div>
              </div>
            }
          </div>
        </section>

        <section class="rounded-2xl bg-white p-6 shadow-sm xl:col-span-2">
          <p class="eyebrow">Tendencia</p>
          <h2 class="font-display text-2xl font-semibold">Donaciones por mes</h2>
          <div class="mt-8 flex min-h-64 items-end gap-3 overflow-x-auto border-b border-ink-200 pb-1">
            @for (entry of entries(stats()!.donationsByMonth); track entry[0]) {
              <div class="flex min-w-16 flex-1 flex-col items-center justify-end gap-2">
                <strong class="text-sm">{{ entry[1] }}</strong>
                <div
                  class="w-full max-w-16 rounded-t-lg bg-brand-600"
                  [style.height.px]="Math.max(12, width(entry[1], stats()!.donationsByMonth) * 1.8)"
                ></div>
                <span class="pb-2 text-center text-xs text-ink-500">{{ entry[0] }}</span>
              </div>
            }
          </div>
        </section>
      </div>
    }
  `,
})
export class AdminStatisticsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  readonly Math = Math;
  readonly stats = signal<DashboardStatistics | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.dashboardStatistics().subscribe({
      next: (stats) => this.stats.set(stats),
      error: (error) => {
        this.loading.set(false);
        this.toast.error(apiErrorMessage(error));
      },
      complete: () => this.loading.set(false),
    });
  }

  entries(record: Record<string, number>): [string, number][] {
    return Object.entries(record);
  }

  width(value: number, record: Record<string, number>): number {
    const max = Math.max(...Object.values(record), 1);
    return (value / max) * 100;
  }
}
