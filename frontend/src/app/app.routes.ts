import { Routes } from '@angular/router';

import { adminGuard, authGuard, guestGuard } from './core/guards/auth.guards';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./layout/public-shell.component').then((component) => component.PublicShellComponent),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/landing/landing.page').then((component) => component.LandingPage),
      },
      {
        path: 'login',
        canActivate: [guestGuard],
        loadComponent: () =>
          import('./features/auth/auth.pages').then((component) => component.LoginPage),
      },
      {
        path: 'registro',
        canActivate: [guestGuard],
        loadComponent: () =>
          import('./features/auth/auth.pages').then((component) => component.RegisterPage),
      },
      {
        path: 'donantes',
        loadComponent: () =>
          import('./features/donors/donors.page').then((component) => component.DonorsPage),
      },
      {
        path: 'solicitudes',
        loadComponent: () =>
          import('./features/requests/requests.pages').then((component) => component.RequestsPage),
      },
      {
        path: 'solicitudes/:id',
        loadComponent: () =>
          import('./features/requests/requests.pages').then(
            (component) => component.RequestDetailPage,
          ),
      },
      {
        path: 'centros',
        loadComponent: () =>
          import('./features/centers/centers.page').then((component) => component.CentersPage),
      },
      {
        path: 'como-donar',
        loadComponent: () =>
          import('./features/how-to-donate/how-to-donate.page').then(
            (component) => component.HowToDonatePage,
          ),
      },
      {
        path: 'compatibilidad',
        loadComponent: () =>
          import('./features/compatibility/compatibility.page').then(
            (component) => component.CompatibilityPage,
          ),
      },
      {
        path: 'preguntas-frecuentes',
        loadComponent: () =>
          import('./features/faq/faq.page').then((component) => component.FaqPage),
      },
    ],
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/dashboard-shell.component').then(
        (component) => component.DashboardShellComponent,
      ),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/dashboard/dashboard.pages').then(
            (component) => component.DashboardHomePage,
          ),
      },
      {
        path: 'perfil',
        loadComponent: () =>
          import('./features/dashboard/dashboard.pages').then((component) => component.ProfilePage),
      },
      {
        path: 'solicitudes',
        loadComponent: () =>
          import('./features/dashboard/dashboard.pages').then(
            (component) => component.MyRequestsPage,
          ),
      },
      {
        path: 'donaciones',
        loadComponent: () =>
          import('./features/dashboard/dashboard.pages').then(
            (component) => component.MyDonationsPage,
          ),
      },
      {
        path: 'notificaciones',
        loadComponent: () =>
          import('./features/dashboard/dashboard.pages').then(
            (component) => component.NotificationsPage,
          ),
      },
    ],
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./layout/admin-shell.component').then((component) => component.AdminShellComponent),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/admin/admin.pages').then((component) => component.AdminHomePage),
      },
      {
        path: 'usuarios',
        loadComponent: () =>
          import('./features/admin/admin.pages').then((component) => component.AdminUsersPage),
      },
      {
        path: 'donantes',
        loadComponent: () =>
          import('./features/admin/admin.pages').then((component) => component.AdminDonorsPage),
      },
      {
        path: 'solicitudes',
        loadComponent: () =>
          import('./features/admin/admin.pages').then((component) => component.AdminRequestsPage),
      },
      {
        path: 'donaciones',
        loadComponent: () =>
          import('./features/admin/admin.pages').then((component) => component.AdminDonationsPage),
      },
      {
        path: 'centros',
        loadComponent: () =>
          import('./features/admin/admin.pages').then((component) => component.AdminCentersPage),
      },
      {
        path: 'estadisticas',
        loadComponent: () =>
          import('./features/admin/admin.pages').then(
            (component) => component.AdminStatisticsPage,
          ),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
