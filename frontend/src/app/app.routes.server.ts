import { RenderMode, ServerRoute } from '@angular/ssr';

const noIndex = { 'X-Robots-Tag': 'noindex, nofollow' };

export const serverRoutes: ServerRoute[] = [
  { path: '', renderMode: RenderMode.Server },
  { path: 'donantes', renderMode: RenderMode.Server },
  { path: 'solicitudes', renderMode: RenderMode.Server },
  { path: 'solicitudes/:id', renderMode: RenderMode.Server },
  { path: 'centros', renderMode: RenderMode.Server },
  { path: 'como-donar', renderMode: RenderMode.Server },
  { path: 'compatibilidad', renderMode: RenderMode.Server },
  { path: 'preguntas-frecuentes', renderMode: RenderMode.Server },
  { path: 'eliminacion-de-cuenta', renderMode: RenderMode.Server },
  { path: 'login', renderMode: RenderMode.Prerender, headers: noIndex },
  { path: 'registro', renderMode: RenderMode.Prerender, headers: noIndex },
  { path: 'dashboard', renderMode: RenderMode.Client, headers: noIndex },
  { path: 'dashboard/**', renderMode: RenderMode.Client, headers: noIndex },
  { path: 'admin', renderMode: RenderMode.Client, headers: noIndex },
  { path: 'admin/**', renderMode: RenderMode.Client, headers: noIndex },
  { path: '**', renderMode: RenderMode.Server },
];
