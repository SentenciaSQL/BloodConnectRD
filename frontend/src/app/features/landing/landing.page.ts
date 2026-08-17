import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { BloodRequest, DonationCenter } from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import {
  DonationCenterCardComponent,
  EmptyStateComponent,
  LoadingSpinnerComponent,
  RequestCardComponent,
} from '../../shared/components/ui-components';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    RouterLink,
    DonationCenterCardComponent,
    EmptyStateComponent,
    LoadingSpinnerComponent,
    RequestCardComponent,
  ],
  template: `
    <section class="overflow-hidden bg-ink-950 text-white">
      <div class="grid min-h-[660px] lg:grid-cols-2">
        <div class="flex items-center px-5 py-20 sm:px-10 lg:px-[max(3rem,calc((100vw-80rem)/2))] lg:pr-14">
          <div class="max-w-xl">
            <p class="mb-5 inline-flex items-center gap-2 text-sm font-bold uppercase tracking-[0.18em] text-brand-300">
              <span class="h-2 w-2 rounded-full bg-brand-500"></span>
              Red solidaria dominicana
            </p>
            <h1 class="font-display text-5xl font-semibold leading-[1.02] tracking-tight sm:text-6xl xl:text-7xl">
              Donar sangre <span class="text-brand-400">salva vidas</span>
            </h1>
            <p class="mt-7 max-w-lg text-lg leading-relaxed text-ink-200">
              Conectamos a personas que necesitan sangre con donantes disponibles y centros de
              donación en toda la República Dominicana.
            </p>
            <div class="mt-9 flex flex-wrap gap-3">
              <a routerLink="/registro" class="btn-primary !px-7 !py-3.5">Quiero donar</a>
              <a
                routerLink="/dashboard/solicitudes"
                class="inline-flex min-h-11 items-center justify-center rounded-lg border border-white/30 px-7 py-3.5 text-sm font-bold text-white transition hover:bg-white hover:text-ink-950"
              >
                Necesito sangre
              </a>
            </div>
            <p class="mt-7 flex items-start gap-2 text-xs leading-relaxed text-ink-400">
              <span aria-hidden="true">ⓘ</span>
              La elegibilidad para donar siempre debe ser confirmada por profesionales de la salud.
            </p>
          </div>
        </div>
        <div class="relative min-h-[440px] lg:min-h-full">
          <img
            src="/images/donation-hero.jpg"
            alt="Profesional de salud preparando una donación de sangre"
            width="1600"
            height="944"
            fetchpriority="high"
            decoding="async"
            class="absolute inset-0 h-full w-full object-cover"
          />
          <div class="absolute inset-0 bg-gradient-to-r from-ink-950/45 via-transparent to-transparent"></div>
          <div class="absolute inset-x-0 bottom-0 bg-gradient-to-t from-ink-950/80 to-transparent px-7 pb-8 pt-24 lg:px-10">
            <p class="font-display text-2xl font-semibold">Un gesto. Tres vidas.</p>
            <p class="mt-1 text-sm text-white/75">Tu donación puede marcar la diferencia hoy.</p>
          </div>
        </div>
      </div>
    </section>

    <section class="border-b border-ink-100 bg-white py-8">
      <div class="mx-auto max-w-7xl px-5 lg:px-8">
        <p class="mb-4 text-sm font-bold text-ink-700">Encuentra lo que necesitas rápidamente</p>
        <div class="flex flex-wrap gap-2">
          @for (filter of quickFilters; track filter.label) {
            <a
              [routerLink]="filter.path"
              [queryParams]="filter.query"
              class="rounded-full border border-ink-200 bg-white px-4 py-2 text-sm font-semibold text-ink-700 transition hover:border-brand-300 hover:bg-brand-50 hover:text-brand-800"
            >
              {{ filter.label }}
            </a>
          }
        </div>
      </div>
    </section>

    <section class="section-shell">
      <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p class="eyebrow">Se necesita tu ayuda</p>
          <h2 class="section-title">Solicitudes urgentes</h2>
          <p class="section-copy">Casos activos que requieren respuesta prioritaria.</p>
        </div>
        <a routerLink="/solicitudes" [queryParams]="{ urgente: 1 }" class="text-sm font-bold text-brand-700">
          Ver todas las solicitudes →
        </a>
      </div>
      @if (loadingRequests()) {
        <app-loading-spinner label="Buscando solicitudes urgentes…" />
      } @else if (requests().length) {
        <div class="mt-9 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          @for (request of requests(); track request.id) {
            <app-request-card [request]="request" />
          }
        </div>
      } @else {
        <div class="mt-9">
          <app-empty-state
            title="No hay solicitudes urgentes"
            message="En este momento no encontramos casos urgentes activos."
          />
        </div>
      }
    </section>

    <section class="bg-ink-950 py-20 text-white">
      <div class="mx-auto max-w-7xl px-5 lg:px-8">
        <p class="eyebrow !text-brand-300">Un proceso sencillo</p>
        <h2 class="section-title !text-white">Cómo funciona</h2>
        <div class="mt-12 grid gap-10 md:grid-cols-3">
          @for (step of steps; track step.number) {
            <div class="border-t border-ink-700 pt-6">
              <span class="font-display text-4xl font-semibold text-brand-400">{{ step.number }}</span>
              <h3 class="mt-5 text-xl font-bold">{{ step.title }}</h3>
              <p class="mt-3 text-sm leading-relaxed text-ink-300">{{ step.copy }}</p>
            </div>
          }
        </div>
        <a routerLink="/como-donar" class="mt-10 inline-flex text-sm font-bold text-brand-300 hover:text-white">
          Conoce la guía completa →
        </a>
      </div>
    </section>

    <section class="section-shell">
      <div class="grid items-center gap-12 lg:grid-cols-2">
        <div>
          <p class="eyebrow">Compatibilidad sanguínea</p>
          <h2 class="section-title">Conoce quién puede recibir tu tipo de sangre</h2>
          <p class="section-copy">
            Consulta información de compatibilidad de glóbulos rojos antes de responder a una
            solicitud.
          </p>
          <div class="mt-7 flex flex-wrap gap-2" aria-label="Tipos de sangre">
            @for (type of bloodTypes; track type) {
              <span class="grid h-12 min-w-12 place-items-center rounded-full bg-brand-50 px-2 font-black text-brand-700">
                {{ type }}
              </span>
            }
          </div>
          <p class="mt-6 rounded-lg border-l-4 border-amber-400 bg-amber-50 p-4 text-xs leading-relaxed text-amber-900">
            Información educativa. La compatibilidad y elegibilidad final deben ser verificadas por
            personal médico mediante las pruebas correspondientes.
          </p>
          <a routerLink="/compatibilidad" class="btn-primary mt-7">Consultar compatibilidad</a>
        </div>
        <div class="overflow-hidden rounded-3xl bg-brand-600 p-8 text-white sm:p-12">
          <p class="font-display text-4xl font-semibold">O−</p>
          <p class="mt-5 text-xl font-bold">Donante universal de glóbulos rojos</p>
          <p class="mt-3 text-sm leading-relaxed text-brand-100">
            Cada tipo sanguíneo tiene un papel importante. Descubre a cuáles grupos puedes donar y
            de cuáles puedes recibir.
          </p>
        </div>
      </div>
    </section>

    <section class="bg-white py-20">
      <div class="mx-auto max-w-7xl px-5 lg:px-8">
        <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <p class="eyebrow">Donación segura</p>
            <h2 class="section-title">Centros de donación</h2>
            <p class="section-copy">Encuentra centros registrados cerca de tu comunidad.</p>
          </div>
          <a routerLink="/centros" class="text-sm font-bold text-brand-700">Explorar centros →</a>
        </div>
        @if (loadingCenters()) {
          <app-loading-spinner label="Cargando centros…" />
        } @else if (centers().length) {
          <div class="mt-9 grid gap-5 md:grid-cols-2 xl:grid-cols-3">
            @for (center of centers(); track center.id) {
              <app-donation-center-card [center]="center" />
            }
          </div>
        } @else {
          <div class="mt-9">
            <app-empty-state
              title="No hay centros disponibles"
              message="No pudimos mostrar centros en este momento."
            />
          </div>
        }
      </div>
    </section>

    <section class="bg-brand-600 px-5 py-20 text-center text-white">
      <div class="mx-auto max-w-3xl">
        <h2 class="font-display text-4xl font-semibold sm:text-5xl">Hoy puedes cambiar una historia</h2>
        <p class="mx-auto mt-5 max-w-2xl text-brand-100">
          Regístrate, indica tu disponibilidad y conecta con solicitudes compatibles en tu comunidad.
        </p>
        <a routerLink="/registro" class="mt-8 inline-flex rounded-lg bg-white px-7 py-3.5 text-sm font-bold text-brand-700 hover:bg-brand-50">
          Unirme como donante
        </a>
      </div>
    </section>
  `,
})
export class LandingPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly requests = signal<BloodRequest[]>([]);
  readonly centers = signal<DonationCenter[]>([]);
  readonly loadingRequests = signal(true);
  readonly loadingCenters = signal(true);
  readonly bloodTypes = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  readonly quickFilters = [
    { label: 'Cerca de mí', path: '/solicitudes', query: { cerca: 1 } },
    { label: 'Distrito Nacional', path: '/solicitudes', query: { zona: 'Distrito Nacional' } },
    { label: 'Santo Domingo', path: '/solicitudes', query: { zona: 'Santo Domingo' } },
    { label: 'Santiago', path: '/solicitudes', query: { zona: 'Santiago' } },
    { label: 'Solicitudes urgentes', path: '/solicitudes', query: { urgente: 1 } },
    { label: 'Mi tipo de sangre', path: '/solicitudes', query: { compatibles: 1 } },
  ];
  readonly steps = [
    {
      number: '01',
      title: 'Crea tu cuenta',
      copy: 'Regístrate con tus datos de contacto y, si quieres donar, completa tu perfil sanguíneo.',
    },
    {
      number: '02',
      title: 'Encuentra una conexión',
      copy: 'Explora solicitudes por urgencia, ubicación o compatibilidad con tu tipo de sangre.',
    },
    {
      number: '03',
      title: 'Dona con seguridad',
      copy: 'Coordina tu respuesta y acude a un centro donde el personal médico hará la evaluación.',
    },
  ];

  ngOnInit(): void {
    this.api.urgentRequests(6).subscribe({
      next: (page) => this.requests.set(page.content),
      error: () => this.loadingRequests.set(false),
      complete: () => this.loadingRequests.set(false),
    });
    this.api.centers({ size: 3 }).subscribe({
      next: (page) => this.centers.set(page.content),
      error: () => this.loadingCenters.set(false),
      complete: () => this.loadingCenters.set(false),
    });
  }
}
