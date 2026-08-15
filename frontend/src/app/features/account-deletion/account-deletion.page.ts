import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

export const ACCOUNT_DELETION_EMAIL = 'inf512.andres.frias@gmail.com';
export const ACCOUNT_DELETION_SUBJECT =
  'Solicitud de eliminación de cuenta - BloodConnect RD';

@Component({
  selector: 'app-account-deletion-page',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="bg-hero-mesh px-5 py-20 text-center">
      <div class="mx-auto max-w-3xl">
        <p class="eyebrow">Privacidad</p>
        <h1 class="font-display text-5xl font-semibold text-ink-950">Eliminación de cuenta</h1>
        <p class="mt-4 text-ink-600">
          Los usuarios de BloodConnect RD pueden solicitar la eliminación permanente de su cuenta y
          de los datos asociados.
        </p>
      </div>
    </section>

    <section class="section-shell !max-w-4xl">
      <article class="rounded-3xl border border-ink-100 bg-white p-6 shadow-sm sm:p-9">
        <h2 class="font-display text-3xl font-semibold text-ink-950">Cómo solicitar la eliminación</h2>
        <ol class="mt-6 grid gap-4 text-sm leading-relaxed text-ink-700">
          <li class="rounded-2xl bg-ink-50 p-4">
            <strong class="block text-ink-950">1. Envía un correo electrónico</strong>
            <a class="mt-1 inline-block font-bold text-brand-700" [href]="mailtoHref">
              {{ email }}
            </a>
          </li>
          <li class="rounded-2xl bg-ink-50 p-4">
            <strong class="block text-ink-950">2. Usa este asunto</strong>
            <p class="mt-1">{{ subject }}</p>
          </li>
          <li class="rounded-2xl bg-ink-50 p-4">
            <strong class="block text-ink-950">3. Indica el correo de tu cuenta</strong>
            <p class="mt-1">
              Incluye el correo electrónico asociado a tu cuenta de BloodConnect RD para que
              podamos verificar la solicitud.
            </p>
          </li>
          <li class="rounded-2xl bg-ink-50 p-4">
            <strong class="block text-ink-950">4. Confirmación y eliminación</strong>
            <p class="mt-1">
              Una vez verificada la solicitud, procederemos con la eliminación de la cuenta.
            </p>
          </li>
        </ol>
        <a class="btn-primary mt-8" [href]="mailtoHref">Enviar solicitud por correo</a>
      </article>

      <div class="mt-8 grid gap-6 md:grid-cols-2">
        <article class="rounded-3xl border border-ink-100 bg-white p-6 shadow-sm sm:p-8">
          <h2 class="font-display text-2xl font-semibold text-ink-950">Datos que serán eliminados</h2>
          <ul class="mt-5 grid gap-3 text-sm leading-relaxed text-ink-700">
            <li>Información del perfil.</li>
            <li>Nombre y correo electrónico.</li>
            <li>Información asociada a la cuenta.</li>
            <li>Datos y actividad vinculados al usuario.</li>
          </ul>
        </article>
        <article class="rounded-3xl border border-ink-100 bg-white p-6 shadow-sm sm:p-8">
          <h2 class="font-display text-2xl font-semibold text-ink-950">Datos que pueden conservarse</h2>
          <p class="mt-5 text-sm leading-relaxed text-ink-700">
            Determinados datos podrán conservarse cuando sea necesario por motivos legales, de
            seguridad o cumplimiento normativo. Una vez finalizado el período de retención
            aplicable, estos datos serán eliminados.
          </p>
        </article>
      </div>

      <article class="mt-8 rounded-3xl bg-ink-950 p-6 text-white sm:p-8">
        <h2 class="font-display text-2xl font-semibold">Contacto</h2>
        <p class="mt-3 text-sm leading-relaxed text-ink-300">
          Si tienes dudas sobre esta política o sobre el estado de tu solicitud, escríbenos a
          <a class="font-bold text-white underline-offset-2 hover:underline" [href]="mailtoHref">
            {{ email }}
          </a>
          .
        </p>
        <a routerLink="/preguntas-frecuentes" class="mt-5 inline-block text-sm font-bold text-brand-200">
          Ver preguntas frecuentes →
        </a>
      </article>
    </section>
  `,
})
export class AccountDeletionPage {
  readonly email = ACCOUNT_DELETION_EMAIL;
  readonly subject = ACCOUNT_DELETION_SUBJECT;
  readonly mailtoHref = `mailto:${ACCOUNT_DELETION_EMAIL}?subject=${encodeURIComponent(ACCOUNT_DELETION_SUBJECT)}`;
}
