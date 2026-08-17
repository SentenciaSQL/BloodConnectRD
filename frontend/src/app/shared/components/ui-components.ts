import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  BloodRequest,
  BloodType,
  Donation,
  DonationCenter,
  Urgency,
  donationStatusLabel,
  donationStatusTone,
  requestPendingUnits,
  requestProgressPercent,
} from '../../core/models/api.models';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-button',
  standalone: true,
  template: `
    <button
      [type]="type()"
      [disabled]="disabled()"
      (click)="pressed.emit()"
      class="inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-5 py-2.5 text-sm font-bold transition focus:outline-none focus:ring-2 focus:ring-brand-400 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
      [class]="
        variant() === 'primary'
          ? 'bg-brand-600 text-white shadow-sm hover:bg-brand-700'
          : variant() === 'danger'
            ? 'bg-brand-50 text-brand-800 hover:bg-brand-100'
            : 'border border-ink-300 bg-white text-ink-800 hover:border-brand-300 hover:text-brand-700'
      "
    >
      <ng-content />
    </button>
  `,
})
export class ButtonComponent {
  readonly variant = input<'primary' | 'secondary' | 'danger'>('primary');
  readonly type = input<'button' | 'submit'>('button');
  readonly disabled = input(false);
  readonly pressed = output<void>();
}

@Component({
  selector: 'app-input',
  standalone: true,
  template: `
    <label class="block">
      <span class="mb-1.5 block text-sm font-semibold text-ink-800">{{ label() }}</span>
      <input
        [type]="type()"
        [value]="value()"
        [placeholder]="placeholder()"
        [disabled]="disabled()"
        (input)="valueChange.emit($any($event.target).value)"
        class="form-control"
      />
      @if (hint()) {
        <span class="mt-1 block text-xs text-ink-500">{{ hint() }}</span>
      }
    </label>
  `,
})
export class InputComponent {
  readonly label = input.required<string>();
  readonly type = input('text');
  readonly value = input('');
  readonly placeholder = input('');
  readonly hint = input('');
  readonly disabled = input(false);
  readonly valueChange = output<string>();
}

@Component({
  selector: 'app-modal',
  standalone: true,
  template: `
    @if (open()) {
      <div
        class="fixed inset-0 z-50 grid place-items-center bg-ink-950/65 p-4"
        role="dialog"
        aria-modal="true"
        (click)="closed.emit()"
      >
        <section
          class="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl"
          (click)="$event.stopPropagation()"
        >
          <div class="mb-5 flex items-start justify-between gap-4">
            <h2 class="font-display text-2xl font-semibold text-ink-950">{{ title() }}</h2>
            <button
              type="button"
              (click)="closed.emit()"
              class="rounded-lg p-2 text-ink-500 hover:bg-ink-100"
              aria-label="Cerrar ventana"
            >
              ✕
            </button>
          </div>
          <ng-content />
        </section>
      </div>
    }
  `,
})
export class ModalComponent {
  readonly open = input(false);
  readonly title = input('');
  readonly closed = output<void>();
}

@Component({
  selector: 'app-card',
  standalone: true,
  template: `
    <article class="rounded-2xl border border-ink-100 bg-white p-5 shadow-sm">
      <ng-content />
    </article>
  `,
})
export class CardComponent {}

@Component({
  selector: 'app-badge',
  standalone: true,
  template: `
    <span
      class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-bold"
      [class]="
        tone() === 'red'
          ? 'bg-brand-100 text-brand-800'
          : tone() === 'green'
            ? 'bg-emerald-100 text-emerald-800'
            : tone() === 'amber'
              ? 'bg-amber-100 text-amber-800'
              : 'bg-ink-100 text-ink-700'
      "
    >
      <ng-content />
    </span>
  `,
})
export class BadgeComponent {
  readonly tone = input<'red' | 'green' | 'amber' | 'neutral'>('neutral');
}

@Component({
  selector: 'app-blood-type-badge',
  standalone: true,
  template: `
    <span
      class="inline-grid h-12 min-w-12 place-items-center rounded-full bg-brand-600 px-2 text-base font-black text-white shadow-sm"
      [attr.aria-label]="'Tipo de sangre ' + type()"
    >
      {{ type() }}
    </span>
  `,
})
export class BloodTypeBadgeComponent {
  readonly type = input.required<BloodType>();
}

@Component({
  selector: 'app-urgency-badge',
  standalone: true,
  template: `
    <span
      class="inline-flex rounded-full px-2.5 py-1 text-xs font-bold"
      [class]="
        urgency() === 'CRITICAL'
          ? 'bg-brand-600 text-white'
          : urgency() === 'HIGH'
            ? 'bg-orange-100 text-orange-800'
            : urgency() === 'MEDIUM'
              ? 'bg-amber-100 text-amber-800'
              : 'bg-ink-100 text-ink-700'
      "
    >
      {{ labels[urgency()] }}
    </span>
  `,
})
export class UrgencyBadgeComponent {
  readonly urgency = input.required<Urgency>();
  readonly labels: Record<Urgency, string> = {
    LOW: 'Baja',
    MEDIUM: 'Media',
    HIGH: 'Alta',
    CRITICAL: 'Crítica',
  };
}

@Component({
  selector: 'app-request-card',
  standalone: true,
  imports: [DatePipe, RouterLink, BloodTypeBadgeComponent, UrgencyBadgeComponent],
  template: `
    <article
      class="group flex h-full flex-col rounded-2xl border border-ink-100 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-md"
    >
      <div class="flex items-start justify-between gap-4">
        <app-blood-type-badge [type]="request().bloodType" />
        <app-urgency-badge [urgency]="request().urgency" />
      </div>
      <h3 class="mt-5 text-lg font-bold text-ink-950">{{ request().hospital }}</h3>
      <p class="mt-1 text-sm text-ink-600">
        {{ request().municipalityName }}, {{ request().provinceName }}
      </p>
      <dl class="mt-4 grid grid-cols-2 gap-3 border-y border-ink-100 py-4 text-sm">
        <div>
          <dt class="text-ink-500">Unidades</dt>
          <dd class="font-bold text-ink-900">
            {{ request().completedUnits }}/{{ request().unitsRequired }}
          </dd>
        </div>
        <div>
          <dt class="text-ink-500">Fecha límite</dt>
          <dd class="font-bold text-ink-900">{{ request().deadline | date: 'd MMM' }}</dd>
        </div>
      </dl>
      <div class="mt-4 h-2 overflow-hidden rounded-full bg-ink-100">
        <div
          class="h-full rounded-full bg-brand-600"
          [style.width.%]="progressPercent()"
        ></div>
      </div>
      <p class="mt-2 text-xs text-ink-500">
        {{ request().completedUnits }} de {{ request().unitsRequired }} unidades
        ({{ progressPercent() }}%)
      </p>
      <a
        [routerLink]="['/solicitudes', request().id]"
        class="mt-auto pt-4 text-sm font-bold text-brand-700 hover:text-brand-900"
      >
        Ver solicitud <span aria-hidden="true">→</span>
      </a>
    </article>
  `,
})
export class RequestCardComponent {
  readonly request = input.required<BloodRequest>();

  progressPercent(): number {
    return requestProgressPercent(this.request());
  }
}

@Component({
  selector: 'app-donation-card',
  standalone: true,
  imports: [DatePipe, BadgeComponent],
  template: `
    <article class="rounded-2xl border border-ink-100 bg-white p-5 shadow-sm">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p class="text-sm text-ink-500">{{ donation().donationDate | date: 'longDate' }}</p>
          <h3 class="mt-1 font-bold text-ink-950">
            {{ donation().donationCenterName || 'Donación vinculada a solicitud' }}
          </h3>
        </div>
        <app-badge [tone]="donationStatusTone(donation().status)">
          {{ donationStatusLabel(donation().status) }}
        </app-badge>
      </div>
      <p class="mt-3 text-sm text-ink-600">
        {{ donation().confirmedUnits }} de {{ donation().units }}
        {{ donation().units === 1 ? 'unidad confirmada' : 'unidades confirmadas' }}
      </p>
    </article>
  `,
})
export class DonationCardComponent {
  readonly donation = input.required<Donation>();
  readonly donationStatusLabel = donationStatusLabel;
  readonly donationStatusTone = donationStatusTone;
}

@Component({
  selector: 'app-donation-center-card',
  standalone: true,
  imports: [DecimalPipe, BadgeComponent],
  template: `
    <article class="flex h-full flex-col rounded-2xl border border-ink-100 bg-white p-5 shadow-sm">
      <div class="flex items-start justify-between gap-3">
        <div>
          <p class="text-xs font-bold uppercase tracking-wider text-brand-700">
            {{ typeLabels[center().type] }}
          </p>
          <h3 class="mt-2 text-lg font-bold text-ink-950">{{ center().name }}</h3>
        </div>
        @if (center().approximateDistanceKm !== null && center().approximateDistanceKm !== undefined) {
          <app-badge>{{ center().approximateDistanceKm | number: '1.0-1' }} km</app-badge>
        }
      </div>
      <p class="mt-3 text-sm leading-relaxed text-ink-600">
        {{ center().address }}, {{ center().municipalityName }}
      </p>
      @if (center().schedule) {
        <p class="mt-3 text-sm font-medium text-ink-800">Horario: {{ center().schedule }}</p>
      }
      @if (center().phone) {
        <a class="mt-auto pt-4 text-sm font-bold text-brand-700" [href]="'tel:' + center().phone">
          {{ center().phone }}
        </a>
      }
    </article>
  `,
})
export class DonationCenterCardComponent {
  readonly center = input.required<DonationCenter>();
  readonly typeLabels: Record<string, string> = {
    HOSPITAL: 'Hospital',
    CLINIC: 'Clínica',
    BLOOD_BANK: 'Banco de sangre',
    MEDICAL_CENTER: 'Centro médico',
    OTHER: 'Otro centro',
  };
}

@Component({
  selector: 'app-request-progress',
  standalone: true,
  template: `
    <section [class]="compact() ? '' : 'rounded-2xl border border-ink-100 bg-ink-50 p-5'">
      <dl class="grid gap-4 sm:grid-cols-4">
        <div>
          <dt class="text-xs font-bold uppercase tracking-wider text-ink-500">Unidades requeridas</dt>
          <dd class="mt-1 text-xl font-bold text-ink-950">{{ request().unitsRequired }}</dd>
        </div>
        <div>
          <dt class="text-xs font-bold uppercase tracking-wider text-ink-500">Unidades recibidas</dt>
          <dd class="mt-1 text-xl font-bold text-ink-950">{{ request().completedUnits }}</dd>
        </div>
        <div>
          <dt class="text-xs font-bold uppercase tracking-wider text-ink-500">Unidades pendientes</dt>
          <dd class="mt-1 text-xl font-bold text-ink-950">{{ pendingUnits() }}</dd>
        </div>
        <div>
          <dt class="text-xs font-bold uppercase tracking-wider text-ink-500">Progreso</dt>
          <dd class="mt-1 text-xl font-bold text-brand-700">{{ progressPercent() }}%</dd>
        </div>
      </dl>
      <div class="mt-4 h-2.5 overflow-hidden rounded-full bg-ink-100">
        <div
          class="h-full rounded-full bg-brand-600 transition-[width]"
          [style.width.%]="progressPercent()"
        ></div>
      </div>
      <p class="mt-2 text-sm font-medium text-ink-600">
        {{ request().completedUnits }} de {{ request().unitsRequired }} unidades ({{ progressPercent() }}%)
      </p>
    </section>
  `,
})
export class RequestProgressComponent {
  readonly request = input.required<BloodRequest>();
  readonly compact = input(false);

  pendingUnits(): number {
    return requestPendingUnits(this.request());
  }

  progressPercent(): number {
    return requestProgressPercent(this.request());
  }
}

@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `
    <div class="rounded-2xl border border-dashed border-ink-300 bg-white/70 px-6 py-12 text-center">
      <span class="mx-auto grid h-12 w-12 place-items-center rounded-full bg-brand-50 text-xl">♡</span>
      <h3 class="mt-4 font-display text-xl font-semibold text-ink-950">{{ title() }}</h3>
      <p class="mx-auto mt-2 max-w-md text-sm text-ink-600">{{ message() }}</p>
      <div class="mt-5"><ng-content /></div>
    </div>
  `,
})
export class EmptyStateComponent {
  readonly title = input('Todavía no hay información');
  readonly message = input('Vuelve a intentarlo en unos minutos.');
}

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  template: `
    <div class="flex items-center justify-center gap-3 py-12 text-sm font-semibold text-ink-600">
      <span class="h-6 w-6 animate-spin rounded-full border-2 border-brand-200 border-t-brand-600"></span>
      {{ label() }}
    </div>
  `,
})
export class LoadingSpinnerComponent {
  readonly label = input('Cargando información…');
}

@Component({
  selector: 'app-pagination',
  standalone: true,
  template: `
    @if (totalPages() > 1) {
      <nav class="mt-8 flex items-center justify-center gap-3" aria-label="Paginación">
        <button
          type="button"
          class="btn-secondary"
          [disabled]="page() === 0"
          (click)="changed.emit(page() - 1)"
        >
          Anterior
        </button>
        <span class="text-sm font-semibold text-ink-600">
          Página {{ page() + 1 }} de {{ totalPages() }}
        </span>
        <button
          type="button"
          class="btn-secondary"
          [disabled]="page() + 1 >= totalPages()"
          (click)="changed.emit(page() + 1)"
        >
          Siguiente
        </button>
      </nav>
    }
  `,
})
export class PaginationComponent {
  readonly page = input(0);
  readonly totalPages = input(0);
  readonly changed = output<number>();
}

@Component({
  selector: 'app-toast',
  standalone: true,
  template: `
    <div class="pointer-events-none fixed right-4 top-4 z-[70] flex w-[min(24rem,calc(100%-2rem))] flex-col gap-3">
      @for (message of toast.messages(); track message.id) {
        <div
          class="pointer-events-auto flex items-start gap-3 rounded-xl border bg-white p-4 shadow-xl"
          [class]="
            message.type === 'success'
              ? 'border-emerald-200'
              : message.type === 'error'
                ? 'border-brand-200'
                : 'border-ink-200'
          "
          role="status"
        >
          <span
            class="mt-0.5 grid h-6 w-6 shrink-0 place-items-center rounded-full text-xs font-black text-white"
            [class]="message.type === 'success' ? 'bg-emerald-600' : message.type === 'error' ? 'bg-brand-600' : 'bg-ink-600'"
          >
            {{ message.type === 'success' ? '✓' : message.type === 'error' ? '!' : 'i' }}
          </span>
          <p class="flex-1 text-sm font-medium text-ink-800">{{ message.text }}</p>
          <button
            type="button"
            class="text-ink-400 hover:text-ink-800"
            (click)="toast.dismiss(message.id)"
            aria-label="Cerrar mensaje"
          >
            ✕
          </button>
        </div>
      }
    </div>
  `,
})
export class ToastComponent {
  readonly toast = inject(ToastService);
}
