import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="min-h-screen bg-ink-50">
      <header class="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-ink-100 bg-white px-4 xl:hidden">
        <button type="button" class="rounded-lg p-2" (click)="open.set(true)" aria-label="Abrir menú">
          ☰
        </button>
        <span class="font-display text-lg font-semibold">Administración</span>
        <a routerLink="/" class="text-sm font-bold text-brand-700">Sitio</a>
      </header>

      @if (open()) {
        <button
          type="button"
          class="fixed inset-0 z-40 bg-ink-950/50 xl:hidden"
          (click)="open.set(false)"
          aria-label="Cerrar menú"
        ></button>
      }

      <aside
        class="fixed inset-y-0 left-0 z-50 flex w-72 flex-col bg-white px-4 py-6 shadow-xl transition-transform xl:translate-x-0"
        [class.-translate-x-full]="!open()"
      >
        <div class="flex items-center justify-between px-3">
          <a routerLink="/" class="flex items-center gap-3" (click)="open.set(false)">
            <span class="grid h-10 w-10 place-items-center rounded-full bg-brand-600 text-sm font-black text-white">
              BC
            </span>
            <span>
              <strong class="block font-display text-lg leading-none">BloodConnect RD</strong>
              <small class="text-xs text-ink-500">Panel administrativo</small>
            </span>
          </a>
          <button type="button" class="p-2 xl:hidden" (click)="open.set(false)" aria-label="Cerrar menú">✕</button>
        </div>

        <nav class="mt-8 grid gap-1 text-sm font-semibold" aria-label="Administración">
          @for (link of links; track link.path) {
            <a
              [routerLink]="link.path"
              routerLinkActive="bg-brand-50 text-brand-800"
              [routerLinkActiveOptions]="{ exact: link.exact }"
              (click)="open.set(false)"
              class="rounded-lg px-3 py-3 text-ink-600 hover:bg-ink-50 hover:text-ink-950"
            >
              {{ link.label }}
            </a>
          }
        </nav>

        <div class="mt-auto border-t border-ink-100 pt-5">
          <p class="px-3 text-sm font-bold">{{ auth.user()?.firstName }} {{ auth.user()?.lastName }}</p>
          <p class="px-3 text-xs text-ink-500">Cuenta administradora</p>
          <button
            type="button"
            (click)="logout()"
            class="mt-4 w-full rounded-lg border border-ink-200 px-3 py-2 text-left text-sm font-semibold hover:border-brand-300 hover:text-brand-700"
          >
            Cerrar sesión
          </button>
        </div>
      </aside>

      <main class="min-h-screen px-5 py-8 xl:ml-72 xl:px-10 xl:py-10">
        <div class="mx-auto max-w-7xl">
          <router-outlet />
        </div>
      </main>
    </div>
  `,
})
export class AdminShellComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly open = signal(false);
  readonly links = [
    { path: '/admin', label: 'Resumen', exact: true },
    { path: '/admin/usuarios', label: 'Usuarios', exact: false },
    { path: '/admin/donantes', label: 'Donantes', exact: false },
    { path: '/admin/solicitudes', label: 'Solicitudes', exact: false },
    { path: '/admin/donaciones', label: 'Donaciones', exact: false },
    { path: '/admin/centros', label: 'Centros de donación', exact: false },
    { path: '/admin/estadisticas', label: 'Estadísticas', exact: false },
  ];

  logout(): void {
    this.auth.logout().subscribe(() => void this.router.navigate(['/']));
  }
}
