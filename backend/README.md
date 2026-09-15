# BloodConnect RD — Backend

API REST con Spring Boot 3 para la plataforma BloodConnect RD (República Dominicana).

## Stack

- Java 17 / Spring Boot 3.5
- Spring Security + JWT (access + refresh)
- Spring Data JPA / Hibernate (`ddl-auto=validate`)
- PostgreSQL + Flyway
- MapStruct + Lombok
- springdoc-openapi (Swagger)
- Actuator

## Ejecutar

```bash
export DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/bloodconnect_db
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
mvn spring-boot:run
```

## Build

```bash
mvn clean package -DskipTests
```

## URLs

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html
- Health: http://localhost:8080/actuator/health

## Migraciones Flyway

| Archivo | Contenido |
|---------|-----------|
| V1 | users |
| V2 | refresh_tokens |
| V3 | provinces / municipalities (RD) |
| V4 | donors |
| V5 | blood_requests |
| V6 | donation_responses |
| V7 | donation_centers |
| V8 | donations |
| V9 | notifications |
| V10 | device_tokens |
| V11 | indexes |
| V12 | seed desarrollo |
| V13 | confirmación de donaciones (unidades reportadas/confirmadas) |
| V14 | conversaciones y mensajes privados asociados a solicitudes |
| V15 | estados SENT/DELIVERED/READ de mensajes del chat |
| V16 | coordenadas de centros de donación |
| V17 | password reset tokens |
| V18 | verificación de correo |
| V19 | auditoría de invocaciones MCP (`mcp_audit_events`) |

No modificar migraciones ya aplicadas; crear una nueva si el esquema cambia.

## Módulos

`auth`, `user`, `donor`, `bloodrequest`, `donationresponse`, `donation`, `donationcenter`, `conversation`, `location`, `notification`, `device`, `statistics`, `admin`, `security`, `config`, `common`, `exception`, `mcp`

## MCP

Servidor Model Context Protocol de solo lectura (solicitudes activas, centros cercanos, compatibilidad ABO/Rh). Desactivado por defecto (`MCP_ENABLED=false`). No habilitar en producción hasta completar OAuth 2.1.

- Estado (público): `GET http://localhost:8080/api/mcp/status`
- Handshake: **POST** `http://localhost:8080/mcp` con JSON-RPC `initialize` y JWT Bearer. `GET /mcp` es SSE, no el handshake.
- `tools/list` y `tools/call` responden SSE: en Windows PowerShell 5.1 use `curl.exe`, no `Invoke-WebRequest`.
- En Windows PowerShell use `$env:MCP_ENABLED = "true"` **antes** de `mvn spring-boot:run` (o en la Run Configuration de IntelliJ) y reinicie el proceso. `set MCP_ENABLED=true` no aplica.

Detalle: [docs/MCP.md](../docs/MCP.md).

## Seed

- `admin@bloodconnect.do` / `Admin123!`
- `*.donor@bloodconnect.do` / `Donor123!`
- `*.user@bloodconnect.do` / `User123!`

## Notas

- Zona horaria: `America/Santo_Domingo`
- Teléfonos DO normalizados a `+1809/829/849...`
- Compatibilidad sanguínea informativa (no aptitud médica)
