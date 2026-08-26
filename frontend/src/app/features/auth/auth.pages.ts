import { Component, inject, OnInit, signal } from '@angular/core';
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

          @if (registrationPending()) {
            <div class="mt-6 rounded-lg border border-green-200 bg-green-50 p-4 text-sm text-green-900">
              <p class="font-bold">Cuenta creada correctamente</p>
              <p class="mt-1">
                Revisa tu correo electrónico y confirma tu cuenta antes de iniciar sesión.
              </p>
              <button
                type="button"
                class="mt-3 font-bold text-brand-700 hover:text-brand-900"
                [disabled]="resending()"
                (click)="resendVerification()"
              >
                {{ resending() ? 'Reenviando…' : 'Reenviar correo de verificación' }}
              </button>
            </div>
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
          <p class="mt-3 text-center text-sm">
            <a
              routerLink="/recuperar-contrasena"
              class="font-bold text-brand-700 hover:text-brand-900"
            >
              ¿Olvidaste tu contraseña?
            </a>
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
  private readonly toast = inject(ToastService);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly expired = signal(this.route.snapshot.queryParamMap.get('sesion') === 'expirada');
  readonly registrationPending = signal(this.route.snapshot.queryParamMap.get('registro') === 'pendiente');
  readonly registeredEmail = this.route.snapshot.queryParamMap.get('email') ?? '';
  readonly resending = signal(false);
  readonly form = this.fb.nonNullable.group({
    email: [this.registeredEmail, [Validators.required, Validators.email]],
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

  resendVerification(): void {
    const email = this.form.controls.email.value.trim();
    if (!email) {
      this.error.set('Escribe el correo electrónico de tu cuenta.');
      return;
    }

    this.resending.set(true);
    this.error.set('');

    this.auth.resendVerification(email).subscribe({
      next: (response) => this.toast.success(response.message),
      error: (error) => {
        this.resending.set(false);
        this.error.set(apiErrorMessage(error));
      },
      complete: () => this.resending.set(false),
    });
  }
}

@Component({
  selector: 'app-forgot-password-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="bg-ink-50 px-5 py-14 sm:py-20">
      <div
        class="mx-auto max-w-lg rounded-3xl border border-ink-100
               bg-white p-6 shadow-sm sm:p-10"
      >
        <p class="eyebrow">Recuperación de acceso</p>

        <h1 class="font-display text-4xl font-semibold text-ink-950">
          Recupera tu contraseña
        </h1>

        <p class="mt-3 text-sm leading-relaxed text-ink-600">
          Escribe el correo asociado a tu cuenta. Te enviaremos
          un enlace para crear una nueva contraseña.
        </p>

        @if (sent()) {
          <div
            class="mt-8 rounded-xl border border-emerald-200
                   bg-emerald-50 p-4 text-sm text-emerald-900"
          >
            Si existe una cuenta con ese correo, recibirás las
            instrucciones en unos minutos.
          </div>
        } @else {
          <form
            class="mt-8 grid gap-5"
            [formGroup]="form"
            (ngSubmit)="submit()"
          >
            <label>
              <span class="form-label">Correo electrónico</span>

              <input
                type="email"
                formControlName="email"
                class="form-control"
                autocomplete="email"
                placeholder="nombre@correo.com"
              />

              @if (
                form.controls.email.touched &&
                form.controls.email.invalid
              ) {
                <span class="form-error">
                  Escribe un correo electrónico válido.
                </span>
              }
            </label>

            @if (error()) {
              <p
                class="rounded-lg border border-brand-200
                       bg-brand-50 p-3 text-sm text-brand-800"
              >
                {{ error() }}
              </p>
            }

            <button
              type="submit"
              class="btn-primary w-full"
              [disabled]="loading()"
            >
              {{
                loading()
                  ? 'Enviando…'
                  : 'Enviar enlace de recuperación'
              }}
            </button>
          </form>
        }

        <p class="mt-7 text-center text-sm text-ink-600">
          <a
            routerLink="/login"
            class="font-bold text-brand-700 hover:text-brand-900"
          >
            Volver a iniciar sesión
          </a>
        </p>
      </div>
    </section>
  `,
})
export class ForgotPasswordPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly loading = signal(false);
  readonly sent = signal(false);
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    email: [
      '',
      [
        Validators.required,
        Validators.email,
        Validators.maxLength(255),
      ],
    ],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.auth
      .forgotPassword(this.form.controls.email.value)
      .subscribe({
        next: () => this.sent.set(true),
        error: (error) => {
          this.loading.set(false);
          this.error.set(apiErrorMessage(error));
        },
        complete: () => this.loading.set(false),
      });
  }
}

@Component({
  selector: 'app-reset-password-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="bg-ink-50 px-5 py-14 sm:py-20">
      <div
        class="mx-auto max-w-lg rounded-3xl border border-ink-100
               bg-white p-6 shadow-sm sm:p-10"
      >
        <p class="eyebrow">Nueva contraseña</p>

        <h1 class="font-display text-4xl font-semibold text-ink-950">
          Restablece tu contraseña
        </h1>

        @if (!token) {
          <div
            class="mt-7 rounded-xl border border-amber-200
                   bg-amber-50 p-4 text-sm text-amber-900"
          >
            El enlace de recuperación no contiene un token válido.
            Solicita uno nuevo.
          </div>

          <a
            routerLink="/recuperar-contrasena"
            class="btn-primary mt-6 w-full"
          >
            Solicitar otro enlace
          </a>
        } @else if (completed()) {
          <div
            class="mt-7 rounded-xl border border-emerald-200
                   bg-emerald-50 p-4 text-sm text-emerald-900"
          >
            Tu contraseña fue restablecida correctamente.
            Ya puedes iniciar sesión.
          </div>

          <a routerLink="/login" class="btn-primary mt-6 w-full">
            Iniciar sesión
          </a>
        } @else {
          <p class="mt-3 text-sm leading-relaxed text-ink-600">
            Crea una contraseña segura de al menos 8 caracteres.
          </p>

          <form
            class="mt-8 grid gap-5"
            [formGroup]="form"
            (ngSubmit)="submit()"
          >
            <label>
              <span class="form-label">Nueva contraseña</span>

              <input
                type="password"
                formControlName="password"
                class="form-control"
                autocomplete="new-password"
                placeholder="Mínimo 8 caracteres"
              />

              @if (
                form.controls.password.touched &&
                form.controls.password.invalid
              ) {
                <span class="form-error">
                  Debe tener entre 8 y 72 caracteres.
                </span>
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
                (
                  form.controls.confirmPassword.invalid ||
                  form.hasError('passwordsMismatch')
                )
              ) {
                <span class="form-error">
                  Las contraseñas deben coincidir.
                </span>
              }
            </label>

            @if (error()) {
              <p
                class="rounded-lg border border-brand-200
                       bg-brand-50 p-3 text-sm text-brand-800"
              >
                {{ error() }}
              </p>
            }

            <button
              type="submit"
              class="btn-primary w-full"
              [disabled]="loading()"
            >
              {{
                loading()
                  ? 'Guardando…'
                  : 'Cambiar contraseña'
              }}
            </button>
          </form>
        }
      </div>
    </section>
  `,
})
export class ResetPasswordPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  readonly token =
    this.route.snapshot.queryParamMap.get('token')?.trim() ?? '';

  readonly loading = signal(false);
  readonly completed = signal(false);
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group(
    {
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(72),
        ],
      ],
      confirmPassword: ['', Validators.required],
    },
    {
      validators: matchingPasswords,
    },
  );

  submit(): void {
    if (!this.token || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.auth
      .resetPassword({
        token: this.token,
        ...this.form.getRawValue(),
      })
      .subscribe({
        next: () => this.completed.set(true),
        error: (error) => {
          this.loading.set(false);
          this.error.set(apiErrorMessage(error));
        },
        complete: () => this.loading.set(false),
      });
  }
}

@Component({
  selector: 'app-verify-email-page',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="bg-ink-50 px-5 py-14 sm:py-20">
      <div class="mx-auto max-w-lg rounded-3xl border border-ink-100 bg-white p-6 text-center shadow-sm sm:p-10">
        @if (loading()) {
          <p class="eyebrow">Verificación de correo</p>
          <h1 class="font-display text-4xl font-semibold text-ink-950">Confirmando tu correo</h1>
          <p class="mt-4 text-ink-600">Espera un momento mientras verificamos tu cuenta.</p>
        } @else if (completed()) {
          <p class="eyebrow">Correo confirmado</p>
          <h1 class="font-display text-4xl font-semibold text-ink-950">Tu cuenta está lista</h1>
          <p class="mt-4 text-ink-600">
            Tu correo electrónico fue confirmado correctamente. Ya puedes iniciar sesión.
          </p>
          <a routerLink="/login" class="btn-primary mt-7 inline-flex">
            Iniciar sesión
          </a>
        } @else {
          <p class="eyebrow">No pudimos verificar el correo</p>
          <h1 class="font-display text-4xl font-semibold text-ink-950">Enlace no válido</h1>
          <p class="mt-4 rounded-lg border border-brand-200 bg-brand-50 p-3 text-sm text-brand-800">
            {{ error() }}
          </p>
          <a routerLink="/login" class="btn-secondary mt-7 inline-flex">
            Volver al inicio de sesión
          </a>
        }
      </div>
    </section>
  `,
})
export class VerifyEmailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  readonly loading = signal(true);
  readonly completed = signal(false);
  readonly error = signal('');

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.loading.set(false);
      this.error.set('El enlace de verificación no contiene un token.');
      return;
    }

    this.auth.verifyEmail(token).subscribe({
      next: () => this.completed.set(true),
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
      next: (response) => {
        this.toast.success(response.message);
        void this.router.navigate(['/login'], {
          queryParams: {
            registro: 'pendiente',
            email: value.email,
          },
        });
      },
      error: (error) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(error));
      },
      complete: () => this.loading.set(false),
    });
  }
}
