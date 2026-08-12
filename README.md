# BloodConnect RD

Plataforma profesional para conectar personas que necesitan sangre con donantes disponibles, hospitales, clínicas y bancos de sangre en **República Dominicana**.

> BloodConnect RD es una plataforma de conexión, no un sistema médico. La elegibilidad para donar debe ser determinada por profesionales de la salud.

## Objetivo

- Registrar usuarios y donantes
- Publicar y gestionar solicitudes de sangre
- Buscar donantes compatibles y solicitudes cercanas
- Gestionar centros de donación
- Notificaciones internas y push
- Panel administrativo con estadísticas

## Arquitectura

```text
Angular Web ─────────┐
                     │
                     ▼
               Spring Boot API
                     ▲
                     │
Flutter Mobile ──────┘
                     │
                     ▼
                 PostgreSQL
```

## Stack

| Capa | Tecnología |
|------|------------|
| Backend | Java 17, Spring Boot 3, Security, JWT, JPA, Flyway, PostgreSQL, Swagger |
| Web | Angular 20+, TypeScript, Tailwind CSS, Signals |
| Móvil | Flutter, Riverpod, Dio, GoRouter, Firebase, Google Maps |
| DevOps | Docker, Docker Compose, GitHub Actions |

## Estructura del monorepo

```text
bloodconnect-rd/
├── backend/                 # Spring Boot API
├── frontend/                # Angular Web
├── mobile/                  # Flutter Android/iOS
├── .github/workflows/ci.yml
├── docker-compose.yml
├── .env.example
└── README.md
```

## País e internacionalización

- País: República Dominicana (`country = DO`)
- Idioma: `es-DO`
- Zona horaria: `America/Santo_Domingo`
- Fecha: `dd/MM/yyyy`
- Fecha/hora: `dd/MM/yyyy hh:mm a`

No hay selector de país en esta versión.

## Requisitos

- Java 17+
- Maven 3.9+
- Node.js 20+ / npm
- Flutter 3.32+
- Docker / Docker Compose (recomendado)
- PostgreSQL 16

## Inicio rápido con Docker

```bash
cp .env.example .env
docker compose up -d
```

Servicios:

| Servicio | URL |
|----------|-----|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| Health | http://localhost:8080/actuator/health |
| Web | http://localhost:4200 |
| PostgreSQL | `localhost:5432` / `bloodconnect_db` |

> En algunos entornos cloud el daemon Docker puede no montar overlays; en ese caso use PostgreSQL local y ejecute backend/frontend por separado.

## Desarrollo local

### PostgreSQL

```bash
# Ejemplo local
createdb bloodconnect_db
# Credenciales por defecto de desarrollo: postgres / postgres
```

### Backend

```bash
cd backend
export DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/bloodconnect_db
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
mvn spring-boot:run
```

Flyway aplica automáticamente V1–V12 (`ddl-auto=validate`).

### Frontend

```bash
cd frontend
npm ci
npm start
```

### Mobile

```bash
cd mobile
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080 --dart-define=ENV=development
```

- Android emulator: `http://10.0.2.2:8080`
- iOS simulator: `http://localhost:8080`

## Cuentas de demostración (seed)

| Rol | Correo | Contraseña |
|-----|--------|------------|
| ADMIN | `admin@bloodconnect.do` | `Admin123!` |
| DONOR | `ana.donor@bloodconnect.do` | `Donor123!` |
| USER | `laura.user@bloodconnect.do` | `User123!` |

Datos de demostración en Distrito Nacional, Santo Domingo, Santiago, La Vega, San Cristóbal, Puerto Plata, La Romana y San Pedro de Macorís.

## Endpoints principales

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/auth/me

GET  /api/locations/provinces
GET  /api/locations/provinces/{id}/municipalities

GET  /api/donors
GET  /api/donors/compatible
GET  /api/blood-compatibility/{bloodType}

GET  /api/blood-requests
GET  /api/blood-requests/urgent
GET  /api/blood-requests/nearby
POST /api/blood-requests/{id}/responses

GET  /api/donation-centers
GET  /api/donation-centers/nearby

GET  /api/notifications
POST /api/devices

GET  /api/admin/statistics/dashboard
```

Documentación completa: Swagger UI.

## Provincias y municipios

El catálogo geográfico vive solo en el backend (Flyway V3). Angular y Flutter cargan provincia → municipio vía API. No hardcodear listas independientes.

## Variables de entorno

Ver [`.env.example`](.env.example). Nunca subir `.env` real.

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`):

- Backend: `mvn clean package -DskipTests`
- Angular: `npm ci && npm run build`
- Flutter: `flutter pub get && flutter analyze && flutter build apk --release`

**No se ejecutan tests automatizados** (decisión del proyecto).

## Firebase

La app móvil integra FCM/Crashlytics de forma opcional: sin `google-services.json` / `GoogleService-Info.plist` la app sigue funcionando; el registro push se omite de forma segura.

## Aviso médico

```text
La compatibilidad sanguínea mostrada es informativa.
La elegibilidad para donar debe ser determinada por profesionales de la salud.
```

## Privacidad

- Dirección exacta y coordenadas del donante no se exponen públicamente
- No se almacenan diagnósticos ni expedientes médicos
- No se solicita cédula en el MVP
- No se vende ni se pone precio a la sangre

## Screenshots

Agregar capturas en `docs/screenshots/` (landing, dashboard, solicitudes, app móvil).

## Documentación por módulo

- [backend/README.md](backend/README.md)
- [frontend/README.md](frontend/README.md)
- [mobile/README.md](mobile/README.md)

## Roadmap

Fases 1–15 implementadas en este monorepo (inicialización → auth → dominio → web → móvil → geo/push → admin → Docker/CI → pulido).

## Licencia

Proyecto de portafolio / educativo.
