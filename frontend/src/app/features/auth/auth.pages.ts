import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { apiErrorMessage, AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';

function dominicanPhone(control: AbstractControl): ValidationErrors | null {
  const digits = String(control.value ?? '').replace(/\D/g, '');
  const national = digits.length === 11 && digits.startsWith('1') ? digits.slice(1) : digits;
  return /^(809|829|849)\d{7}$/.test(national) ? null : { dominicanPhone: true };
}

const matchingPasswords: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  control.get('password')?.value === control.get('confirmPassword')?.value
    ? null
    : { passwordsMismatch: true };

function normalizedPhone(value: string): string {
  const digits = value.replace(/\D/g, '');
  return `+${digits.startsWith('1') ? digits : `1${digits}`}`;
}

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="grid min-h-[calc(100vh-5rem)] bg-white lg:grid-cols-2">
      <div class="flex items-center justify-center px-5 py-16 sm:px-10">
        <div class="w-full max-w-md">
          <p class="eyebrow">Bienvenido de vuelta</p>
          <h1 class="font-display text-4xl font-semibold text-ink-950">Inicia sesión</h1>
          <p class="mt-3 text-sm text-ink-600">
            Accede a tus solicitudes, donaciones y notificaciones.
          </p>

          @if (expired()) {
            <p class="mt-6 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
              Tu sesión venció. Inicia sesión nuevamente para continuar.
            </p>
          }

          <form class="mt-8 grid gap-5" [formGroup]="form" (ngSubmit)="submit()">
            <label>
              <span class="form-label">Correo electrónico</span>
              <input
                type="email"
                formControlName="email"
                class="form-control"
                autocomplete="email"
                placeholder="nombre@correo.com"
              />
              @if (form.controls.email.touched && form.controls.email.invalid) {
                <span class="form-error">Escribe un correo electrónico válido.</span>
              }
            </label>
            <label>
              <span class="form-label">Contraseña</span>
              <input
                type="password"
                formControlName="password"
                class="form-control"
                autocomplete="current-password"
                placeholder="Tu contraseña"
              />
              @if (form.controls.password.touched && form.controls.password.invalid) {
                <span class="form-error">La contraseña es obligatoria.</span>
              }
            </label>
            @if (error()) {
              <p class="rounded-lg border border-brand-200 bg-brand-50 p-3 text-sm text-brand-800">
                {{ error() }}
              </p>
            }
            <button type="submit" class="btn-primary w-full" [disabled]="loading()">
              {{ loading() ? 'Iniciando sesión…' : 'Iniciar sesión' }}
            </button>
          </form>
          <p class="mt-7 text-center text-sm text-ink-600">
            ¿Todavía no tienes cuenta?
            <a routerLink="/registro" class="font-bold text-brand-700 hover:text-brand-900">Regístrate</a>
          </p>
        </div>
      </div>
      <div
        class="relative hidden bg-[url('/images/login-side.jpg')] bg-cover bg-center lg:block"
      >
        <div class="absolute inset-0 bg-gradient-to-t from-ink-950/85 via-ink-950/20 to-transparent"></div>
        <div class="absolute bottom-0 max-w-lg p-12 text-white">
          <p class="font-display text-4xl font-semibold">Tu comunidad cuenta contigo</p>
          <p class="mt-4 text-white/80">
            Mantén tu disponibilidad al día y responde cuando alguien compatible necesite ayuda.
          </p>
        </div>
      </div>
    </section>
  `,
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly expired = signal(this.route.snapshot.queryParamMap.get('sesion') === 'expirada');
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.form.getRawValue()).subscribe({
      next: (session) => {
        const requested = this.route.snapshot.queryParamMap.get('retorno');
        const target = requested || (session.user.role === 'ADMIN' ? '/admin' : '/dashboard');
        void this.router.navigateByUrl(target);
      },
      error: (error) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(error));
      },
      complete: () => this.loading.set(false),
    });
  }
}

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="bg-ink-50 px-5 py-14 sm:py-20">
      <div class="mx-auto max-w-2xl rounded-3xl border border-ink-100 bg-white p-6 shadow-sm sm:p-10">
        <p class="eyebrow">Forma parte de la red</p>
        <h1 class="font-display text-4xl font-semibold text-ink-950">Crea tu cuenta</h1>
        <p class="mt-3 text-sm leading-relaxed text-ink-600">
          Empieza con tus datos básicos. Luego podrás completar tu perfil de donante desde tu cuenta.
        </p>

        <form class="mt-9 grid gap-5 sm:grid-cols-2" [formGroup]="form" (ngSubmit)="submit()">
          <label>
            <span class="form-label">Nombre</span>
            <input formControlName="firstName" class="form-control" autocomplete="given-name" />
            @if (invalid('firstName')) {
              <span class="form-error">El nombre es obligatorio.</span>
            }
          </label>
          <label>
            <span class="form-label">Apellido</span>
            <input formControlName="lastName" class="form-control" autocomplete="family-name" />
            @if (invalid('lastName')) {
              <span class="form-error">El apellido es obligatorio.</span>
            }
          </label>
          <label class="sm:col-span-2">
            <span class="form-label">Correo electrónico</span>
            <input
              type="email"
              formControlName="email"
              class="form-control"
              autocomplete="email"
              placeholder="nombre@correo.com"
            />
            @if (invalid('email')) {
              <span class="form-error">Escribe un correo electrónico válido.</span>
            }
          </label>
          <label class="sm:col-span-2">
            <span class="form-label">Teléfono dominicano</span>
            <input
              type="tel"
              formControlName="phone"
              class="form-control"
              autocomplete="tel"
              placeholder="(809) 555-0123"
            />
            <span class="mt-1 block text-xs text-ink-500">
              Aceptamos números 809, 829 y 849, con o sin +1.
            </span>
            @if (invalid('phone')) {
              <span class="form-error">Escribe un número dominicano válido de 10 dígitos.</span>
            }
          </label>
          <label>
            <span class="form-label">Contraseña</span>
            <input
              type="password"
              formControlName="password"
              class="form-control"
              autocomplete="new-password"
              placeholder="Mínimo 8 caracteres"
            />
            @if (invalid('password')) {
              <span class="form-error">Debe tener entre 8 y 72 caracteres.</span>
            }
          </label>
          <label>
            <span class="form-label">Confirmar contraseña</span>
            <input
              type="password"
              formControlName="confirmPassword"
              class="form-control"
              autocomplete="new-password"
            />
            @if (
              form.controls.confirmPassword.touched &&
              (form.controls.confirmPassword.invalid || form.hasError('passwordsMismatch'))
            ) {
              <span class="form-error">Las contraseñas deben coincidir.</span>
            }
          </label>
          @if (error()) {
            <p class="sm:col-span-2 rounded-lg border border-brand-200 bg-brand-50 p-3 text-sm text-brand-800">
              {{ error() }}
            </p>
          }
          <label class="sm:col-span-2 flex items-start gap-3 text-xs leading-relaxed text-ink-600">
            <input type="checkbox" formControlName="terms" class="mt-0.5 h-4 w-4 accent-brand-600" />
            Confirmo que los datos son correctos y entiendo que BloodConnect RD facilita conexiones,
            pero no sustituye la evaluación de un centro de salud.
          </label>
          <button type="submit" class="btn-primary sm:col-span-2 w-full" [disabled]="loading()">
            {{ loading() ? 'Creando cuenta…' : 'Crear mi cuenta' }}
          </button>
        </form>
        <p class="mt-7 text-center text-sm text-ink-600">
          ¿Ya tienes cuenta?
          <a routerLink="/login" class="font-bold text-brand-700">Inicia sesión</a>
        </p>
      </div>
    </section>
  `,
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group(
    {
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      phone: ['', [Validators.required, dominicanPhone]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirmPassword: ['', Validators.required],
      terms: [false, Validators.requiredTrue],
    },
    { validators: matchingPasswords },
  );

  invalid(name: 'firstName' | 'lastName' | 'email' | 'phone' | 'password'): boolean {
    const control = this.form.controls[name];
    return control.touched && control.invalid;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { terms: _terms, ...value } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set('');
    this.auth.register({ ...value, phone: normalizedPhone(value.phone) }).subscribe({
      next: () => {
        this.toast.success('Tu cuenta fue creada. Completa tu perfil para comenzar.');
        void this.router.navigate(['/dashboard/perfil']);
      },
      error: (error) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(error));
      },
      complete: () => this.loading.set(false),
    });
  }
}
