import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable, catchError, finalize, shareReplay, tap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ApiError, AuthResponse, User } from '../models/api.models';

const ACCESS_TOKEN_KEY = 'bloodconnect_access_token';
const REFRESH_TOKEN_KEY = 'bloodconnect_refresh_token';
const USER_KEY = 'bloodconnect_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private refreshInFlight?: Observable<AuthResponse>;

  readonly user = signal<User | null>(this.readUser());
  readonly isAuthenticated = computed(() => Boolean(this.user() && this.accessToken));
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  readonly isDonor = computed(() => this.user()?.role === 'DONOR');

  get accessToken(): string | null {
    return this.readStorage(ACCESS_TOKEN_KEY);
  }

  get refreshToken(): string | null {
    return this.readStorage(REFRESH_TOKEN_KEY);
  }

  login(credentials: { email: string; password: string }): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/auth/login`, credentials)
      .pipe(tap((response) => this.persistSession(response)));
  }

  register(data: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    confirmPassword: string;
    phone: string;
  }): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/auth/register`, data)
      .pipe(tap((response) => this.persistSession(response)));
  }

  me(): Observable<User> {
    return this.http
      .get<User>(`${environment.apiBaseUrl}/auth/me`)
      .pipe(tap((user) => this.persistUser(user)));
  }

  refresh(): Observable<AuthResponse> {
    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }

    const refreshToken = this.refreshToken;
    if (!refreshToken) {
      this.clearSession();
      return throwError(() => new Error('No hay una sesión para renovar.'));
    }

    this.refreshInFlight = this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/auth/refresh`, { refreshToken })
      .pipe(
        tap((response) => this.persistSession(response)),
        catchError((error) => {
          this.clearSession();
          return throwError(() => error);
        }),
        finalize(() => (this.refreshInFlight = undefined)),
        shareReplay(1),
      );

    return this.refreshInFlight;
  }

  logout(): Observable<unknown> {
    return this.http
      .post(`${environment.apiBaseUrl}/auth/logout`, {
        refreshToken: this.refreshToken,
      })
      .pipe(
        catchError(() => [null]),
        finalize(() => this.clearSession()),
      );
  }

  forgotPassword(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${environment.apiBaseUrl}/auth/forgot-password`,
      { email },
    );
  }

  resetPassword(data: {
    token: string;
    password: string;
    confirmPassword: string;
  }): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${environment.apiBaseUrl}/auth/reset-password`,
      data,
    );
  }

  clearSession(): void {
    this.removeStorage(ACCESS_TOKEN_KEY);
    this.removeStorage(REFRESH_TOKEN_KEY);
    this.removeStorage(USER_KEY);
    this.user.set(null);
  }

  private persistSession(response: AuthResponse): void {
    this.writeStorage(ACCESS_TOKEN_KEY, response.accessToken);
    this.writeStorage(REFRESH_TOKEN_KEY, response.refreshToken);
    this.persistUser(response.user);
  }

  private persistUser(user: User): void {
    this.writeStorage(USER_KEY, JSON.stringify(user));
    this.user.set(user);
  }

  private readUser(): User | null {
    try {
      const value = this.readStorage(USER_KEY);
      return value ? (JSON.parse(value) as User) : null;
    } catch {
      this.removeStorage(USER_KEY);
      return null;
    }
  }

  private readStorage(key: string): string | null {
    if (!this.isBrowser) return null;
    return localStorage.getItem(key);
  }

  private writeStorage(key: string, value: string): void {
    if (!this.isBrowser) return;
    localStorage.setItem(key, value);
  }

  private removeStorage(key: string): void {
    if (!this.isBrowser) return;
    localStorage.removeItem(key);
  }
}

export function apiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as ApiError | string | undefined;
    if (typeof body === 'string' && body.trim()) {
      return body;
    }
    if (body && typeof body === 'object') {
      if (body.message) return body.message;
      if (body.detail) return body.detail;
      if (body.errors) return Object.values(body.errors)[0] ?? 'Revisa los datos ingresados.';
    }
    if (error.status === 0) return 'No pudimos conectar con el servicio. Intenta de nuevo.';
    if (error.status === 403) return 'No tienes permiso para realizar esta acción.';
    if (error.status === 404) return 'No encontramos la información solicitada.';
  }
  return 'Ocurrió un error inesperado. Intenta de nuevo.';
}
