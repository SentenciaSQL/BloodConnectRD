import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-public-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="min-h-screen bg-[#fffaf8] text-ink-950">
      <a
        href="#contenido-principal"
        class="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:font-bold focus:text-brand-700"
      >
        Saltar al contenido
      </a>
      <header class="sticky top-0 z-40 border-b border-ink-100 bg-white/95 backdrop-blur">
        <div class="mx-auto flex h-20 max-w-7xl items-center justify-between px-5 lg:px-8">
          <a routerLink="/" class="flex items-center gap-3" aria-label="BloodConnect RD, inicio">
            <span
              class="grid h-11 w-11 place-items-center rounded-full bg-brand-600 font-black text-white"
            >
              BC
            </span>
            <span>
              <strong class="block font-display text-xl leading-none">BloodConnect RD</strong>
              <small class="text-xs font-medium text-ink-500">República Dominicana</small>
            </span>
          </a>

          <nav
            class="hidden items-center gap-6 text-sm font-semibold lg:flex"
            aria-label="Principal"
          >
            <a routerLink="/solicitudes" routerLinkActive="text-brand-700">Solicitudes</a>
            <a routerLink="/donantes" routerLinkActive="text-brand-700">Donantes</a>
            <a routerLink="/centros" routerLinkActive="text-brand-700">Centros</a>
            <a routerLink="/compatibilidad" routerLinkActive="text-brand-700">Compatibilidad</a>
            <a routerLink="/como-donar" routerLinkActive="text-brand-700">Cómo donar</a>
          </nav>

          <div class="hidden items-center gap-3 lg:flex">
            @if (auth.isAuthenticated()) {
              <a [routerLink]="auth.isAdmin() ? '/admin' : '/dashboard'" class="btn-primary">
                Mi cuenta
              </a>
            } @else {
              <a routerLink="/login" class="btn-secondary">Iniciar sesión</a>
              <a routerLink="/registro" class="btn-primary">Crear cuenta</a>
            }
          </div>

          <button
            type="button"
            class="rounded-lg p-2 text-ink-700 lg:hidden"
            (click)="toggleMenu()"
            [attr.aria-expanded]="menuOpen()"
            aria-label="Abrir menú"
          >
            <span class="text-2xl">☰</span>
          </button>
        </div>

        @if (menuOpen()) {
          <nav class="border-t border-ink-100 bg-white px-5 py-5 lg:hidden" aria-label="Móvil">
            <div class="mx-auto grid max-w-7xl gap-1">
              @for (link of mobileLinks; track link.path) {
                <a
                  [routerLink]="link.path"
                  (click)="menuOpen.set(false)"
                  class="rounded-lg px-3 py-2.5 text-sm font-semibold hover:bg-brand-50 hover:text-brand-700"
                >
                  {{ link.label }}
                </a>
              }
              <div class="mt-3 flex gap-3 border-t border-ink-100 pt-4">
                @if (auth.isAuthenticated()) {
                  <a
                    [routerLink]="auth.isAdmin() ? '/admin' : '/dashboard'"
                    (click)="menuOpen.set(false)"
                    class="btn-primary"
                  >
                    Mi cuenta
                  </a>
                } @else {
                  <a routerLink="/login" (click)="menuOpen.set(false)" class="btn-secondary">
                    Iniciar sesión
                  </a>
                  <a routerLink="/registro" (click)="menuOpen.set(false)" class="btn-primary">
                    Crear cuenta
                  </a>
                }
              </div>
            </div>
          </nav>
        }
      </header>

      <main id="contenido-principal">
        <router-outlet />
      </main>

      <section class="border-t border-ink-200 bg-gradient-to-br from-brand-700 via-brand-600 to-ink-900 px-5 py-14 text-white">
        <div class="mx-auto flex max-w-7xl flex-col items-start justify-between gap-8 lg:flex-row lg:items-center lg:px-3">
          <div class="max-w-xl">
            <p class="text-sm font-semibold uppercase tracking-wide text-brand-100">App móvil</p>
            <h2 class="mt-2 font-display text-3xl font-semibold tracking-tight sm:text-4xl">
              Lleva BloodConnect RD contigo
            </h2>
            <p class="mt-3 text-sm leading-relaxed text-brand-50/90 sm:text-base">
              Descarga la aplicación para Android o iOS y responde a solicitudes de sangre desde cualquier lugar de República Dominicana.
            </p>
          </div>
          <div class="flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
            <a
              href=""
              (click)="$event.preventDefault()"
              aria-label="Descargar en Google Play (próximamente)"
              class="inline-flex min-w-[220px] items-center gap-3 rounded-xl bg-ink-950 px-5 py-3.5 text-white shadow-lg ring-1 ring-white/10 transition hover:bg-black"
            >
              <svg class="h-8 w-8 shrink-0" viewBox="0 0 24 24" aria-hidden="true" fill="currentColor">
                <path d="M3.6 2.8c-.4.2-.6.6-.6 1.1v16.2c0 .5.2.9.6 1.1l9.7-9.2L3.6 2.8zm11.2 6.6 2.3-1.3-2.8-1.6-2.2 2.1 2.7.8zm3.5-2 2.2 1.3c.7.4 1.1 1 .9 1.7-.1.3-.3.6-.6.8l-2.7 1.6-2.5-1.5 2.7-1.6v-.1l-.1-.1.1-.1zm-3.5 7.4-2.7.8 2.2 2.1 2.8-1.6-2.3-1.3zm-4.1-2.5L3.9 21.1c.2 0 .3.1.5.1h.1l10.2-5.9-2.5-1.5z"/>
              </svg>
              <span class="text-left leading-tight">
                <span class="block text-[10px] uppercase tracking-wide text-ink-300">Disponible en</span>
                <span class="block text-base font-semibold">Google Play</span>
              </span>
            </a>
            <a
              href=""
              (click)="$event.preventDefault()"
              aria-label="Descargar en App Store (próximamente)"
              class="inline-flex min-w-[220px] items-center gap-3 rounded-xl bg-ink-950 px-5 py-3.5 text-white shadow-lg ring-1 ring-white/10 transition hover:bg-black"
            >
              <svg class="h-8 w-8 shrink-0" viewBox="0 0 24 24" aria-hidden="true" fill="currentColor">
                <path d="M18.7 12.6c0-2.3 1.9-3.4 2-3.5-1.1-1.6-2.8-1.8-3.4-1.8-1.4-.2-2.8.9-3.5.9-.7 0-1.9-.8-3.1-.8-1.6 0-3.1 1-3.9 2.4-1.7 2.9-.4 7.2 1.2 9.6.8 1.1 1.7 2.4 3 2.4 1.2 0 1.6-.8 3.1-.8s1.8.8 3.1.8c1.3 0 2.1-1.1 2.9-2.2.9-1.3 1.3-2.6 1.3-2.6s-2.5-1-2.7-3.8zM15.4 5.5c.7-.8 1.1-1.9 1-3-.9 0-2.1.6-2.7 1.4-.6.7-1.2 1.9-1 3 1 .1 2-.5 2.7-1.4z"/>
              </svg>
              <span class="text-left leading-tight">
                <span class="block text-[10px] uppercase tracking-wide text-ink-300">Descargar en</span>
                <span class="block text-base font-semibold">App Store</span>
              </span>
            </a>
          </div>
        </div>
      </section>

      <footer class="bg-ink-950 px-5 py-12 text-ink-200">
        <div class="mx-auto grid max-w-7xl gap-10 md:grid-cols-3 lg:px-3">
          <div>
            <p class="font-display text-2xl font-semibold text-white">BloodConnect RD</p>
            <p class="mt-3 max-w-sm text-sm leading-relaxed text-ink-300">
              Conectamos donantes y personas que necesitan sangre en República Dominicana.
            </p>
          </div>
          <div>
            <h2 class="font-bold text-white">Información</h2>
            <div class="mt-3 grid gap-2 text-sm">
              <a routerLink="/preguntas-frecuentes" class="hover:text-white"
                >Preguntas frecuentes</a
              >
              <a routerLink="/como-donar" class="hover:text-white">Guía para donar</a>
              <a routerLink="/compatibilidad" class="hover:text-white">Compatibilidad sanguínea</a>
              <a routerLink="/eliminacion-de-cuenta" class="hover:text-white"
                >Eliminación de cuenta</a
              >
            </div>
          </div>
          <div>
            <h2 class="font-bold text-white">Importante</h2>
            <p class="mt-3 text-sm leading-relaxed text-ink-300">
              Esta plataforma facilita conexiones. La evaluación clínica y la elegibilidad para
              donar corresponden exclusivamente al personal de salud.
            </p>
          </div>
        </div>
        <p
          class="mx-auto mt-10 max-w-7xl border-t border-ink-800 pt-6 text-xs text-ink-400 lg:px-3"
        >
          © 2026 BloodConnect RD · Servicio para República Dominicana · Desarrollado por
          <a
            href="https://andresfrias.dev/"
            target="_blank"
            rel="noopener noreferrer"
            class="font-semibold text-ink-200 underline-offset-2 transition hover:text-white hover:underline"
          >
            afriasdev
          </a>
        </p>
      </footer>
    </div>
  `,
})
export class PublicShellComponent {
  readonly auth = inject(AuthService);
  readonly menuOpen = signal(false);
  readonly mobileLinks = [
    { path: '/solicitudes', label: 'Solicitudes de sangre' },
    { path: '/donantes', label: 'Donantes' },
    { path: '/centros', label: 'Centros de donación' },
    { path: '/compatibilidad', label: 'Compatibilidad' },
    { path: '/como-donar', label: 'Cómo donar' },
    { path: '/preguntas-frecuentes', label: 'Preguntas frecuentes' },
    { path: '/eliminacion-de-cuenta', label: 'Eliminación de cuenta' },
  ];

  toggleMenu(): void {
    this.menuOpen.update((value) => !value);
  }
}
