# BloodConnect RD — Frontend Web

Aplicación Angular 20+ orientada a República Dominicana.

## Stack

- Angular 20+ / TypeScript
- Tailwind CSS
- Angular Signals
- Reactive Forms / RxJS
- HTTP Interceptors + Route Guards

## Ejecutar

```bash
npm ci
npm start
```

http://localhost:4200

## Build

```bash
npm run build
```

## Rutas

**Públicas:** `/`, `/login`, `/registro`, `/donantes`, `/solicitudes`, `/solicitudes/:id`, `/centros`, `/como-donar`, `/compatibilidad`, `/preguntas-frecuentes`, `/eliminacion-de-cuenta`

**Privadas:** `/dashboard`, `/dashboard/perfil`, `/dashboard/solicitudes`, `/dashboard/donaciones`, `/dashboard/notificaciones`

**Admin:** `/admin`, `/admin/usuarios`, `/admin/donantes`, `/admin/solicitudes`, `/admin/donaciones`, `/admin/centros`, `/admin/estadisticas`

## Auth

- `AuthService` (signals)
- `AuthInterceptor` + refresh token
- Guards: `authGuard`, `guestGuard`, `adminGuard`, `donorGuard`

## Notas

- Catálogo provincia/municipio solo desde API
- Textos en español (`es-DO`)
- Sin archivos `*.spec.ts`
