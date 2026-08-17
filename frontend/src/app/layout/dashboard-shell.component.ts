import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-dashboard-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="min-h-screen bg-ink-50">
      <header class="sticky top-0 z-30 flex h-16 items-center border-b border-ink-100 bg-white px-4 lg:hidden">
        <button
          type="button"
          class="mr-3 rounded-lg p-2"
          (click)="sidebarOpen.set(true)"
          aria-label="Abrir navegación"
        >
          ☰
        </button>
        <a routerLink="/" class="font-display text-xl font-semibold">BloodConnect RD</a>
      </header>

      @if (sidebarOpen()) {
        <button
          type="button"
          class="fixed inset-0 z-40 bg-ink-950/50 lg:hidden"
          (click)="sidebarOpen.set(false)"
          aria-label="Cerrar navegación"
        ></button>
      }

      <aside
        class="fixed inset-y-0 left-0 z-50 flex w-72 flex-col bg-ink-950 px-4 py-6 text-white transition-transform lg:translate-x-0"
        [class.-translate-x-full]="!sidebarOpen()"
      >
        <div class="flex items-center justify-between px-3">
          <a routerLink="/" class="flex items-center gap-3" (click)="sidebarOpen.set(false)">
            <span class="grid h-10 w-10 place-items-center rounded-full bg-brand-600 text-sm font-black">BC</span>
            <span class="font-display text-xl font-semibold">BloodConnect RD</span>
          </a>
          <button
            type="button"
            class="rounded-lg p-2 text-ink-300 lg:hidden"
            (click)="sidebarOpen.set(false)"
            aria-label="Cerrar navegación"
          >
            ✕
          </button>
        </div>

        <div class="mt-8 rounded-xl bg-ink-900 p-4">
          <p class="truncate font-bold">{{ auth.user()?.firstName }} {{ auth.user()?.lastName }}</p>
          <p class="mt-1 truncate text-xs text-ink-400">{{ auth.user()?.email }}</p>
        </div>

        <nav class="mt-7 grid gap-1 text-sm font-semibold" aria-label="Mi cuenta">
          @for (link of links; track link.path) {
            <a
              [routerLink]="link.path"
              routerLinkActive="bg-brand-600 text-white"
              [routerLinkActiveOptions]="{ exact: link.exact }"
              (click)="sidebarOpen.set(false)"
              class="flex items-center gap-3 rounded-lg px-3 py-3 text-ink-300 hover:bg-ink-900 hover:text-white"
            >
              <span class="w-5 text-center" aria-hidden="true">{{ link.icon }}</span>
              {{ link.label }}
            </a>
          }
        </nav>

        <div class="mt-auto grid gap-2">
          <a routerLink="/" class="rounded-lg px-3 py-2 text-sm font-semibold text-ink-300 hover:text-white">
            ← Volver al sitio
          </a>
          <button
            type="button"
            (click)="logout()"
            class="rounded-lg border border-ink-700 px-3 py-2 text-left text-sm font-semibold text-ink-200 hover:border-brand-500 hover:text-white"
          >
            Cerrar sesión
          </button>
        </div>
      </aside>

      <main class="min-h-screen px-5 py-8 lg:ml-72 lg:px-10 lg:py-10">
        <div class="mx-auto max-w-6xl">
          <router-outlet />
        </div>
      </main>
    </div>
  `,
})
export class DashboardShellComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly sidebarOpen = signal(false);
  readonly links = [
    { path: '/dashboard', label: 'Resumen', icon: '⌂', exact: true },
    { path: '/dashboard/perfil', label: 'Mi perfil', icon: '○', exact: false },
    { path: '/dashboard/solicitudes', label: 'Mis solicitudes', icon: '♡', exact: false },
    { path: '/dashboard/donaciones', label: 'Mis donaciones', icon: '✓', exact: false },
    { path: '/dashboard/mensajes', label: 'Mensajes', icon: '✉', exact: false },
    { path: '/dashboard/notificaciones', label: 'Notificaciones', icon: '●', exact: false },
  ];

  logout(): void {
    this.auth.logout().subscribe(() => void this.router.navigate(['/']));
  }
}
