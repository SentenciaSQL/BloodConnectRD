import { Component, RESPONSE_INIT, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { SeoService } from '../../core/seo/seo.service';

@Component({
  selector: 'app-not-found-page',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="section-shell !max-w-3xl">
      <div class="mt-10 rounded-2xl bg-white p-10 text-center">
        <p class="eyebrow">Error 404</p>
        <h1 class="font-display text-3xl font-semibold text-ink-950">Página no encontrada</h1>
        <p class="mt-2 text-ink-600">
          El enlace que seguiste no existe o fue movido. Revisa la dirección o vuelve al inicio.
        </p>
        <a routerLink="/" class="btn-primary mt-6 inline-block">Volver al inicio</a>
      </div>
    </section>
  `,
})
export class NotFoundPage {
  private readonly seo = inject(SeoService);
  private readonly router = inject(Router);
  private readonly responseInit = inject(RESPONSE_INIT, { optional: true });

  constructor() {
    this.seo.applyNotFound(this.router.url.split('?')[0]);
    if (this.responseInit) {
      this.responseInit.status = 404;
    }
  }
}
