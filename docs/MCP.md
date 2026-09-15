# MCP de BloodConnect RD (Fase 1)

Servidor **Model Context Protocol** de solo lectura embebido en el backend Spring Boot. Permite que un asistente de IA consulte solicitudes activas de sangre, centros de donación cercanos y compatibilidad ABO/Rh **sin** acceder a PostgreSQL ni a datos personales sensibles.

Esta fase **no** incluye chatbot, proveedor de LLM ni herramientas de escritura.

## Problema que resuelve

Angular y Flutter ya consumen la API REST. Los clientes MCP (Cursor, Claude Desktop, inspectores MCP) necesitan un contrato estable, autenticado y minimizado para:

- listar necesidades de sangre vigentes;
- ver el detalle público de una solicitud;
- localizar centros de donación;
- contrastar tipos sanguíneos con las **mismas reglas** que ya usa BloodConnect RD.

Sin esta capa, un modelo tendría que llamar endpoints REST heterogéneos, vería teléfonos y nombres de paciente, o inventaría SQL y reglas médicas.

## Diagnóstico de la arquitectura actual

El backend es un monolito servlet (Spring MVC + JPA bloqueante + JWT). Los servicios reutilizados son:

| Capacidad | Servicio existente |
|-----------|-------------------|
| Solicitudes | `BloodRequestService.listActive()` / `get()` |
| Centros | `DonationCenterService.nearby()` |
| Compatibilidad | `BloodCompatibilityService.canDonate()` y `DISCLAIMER` |

`BloodRequestService.list()` de la API REST no filtra vencidas por fecha (el job `BloodRequestExpiryService` marca `EXPIRED` de forma asíncrona). Para MCP se añadió `listActive()`, que reutiliza la misma `Specification` con estados `OPEN`/`IN_PROGRESS` y `deadline` futura, **sin** buscar por nombre de paciente.

La API REST de Angular/Flutter no cambia de contrato.

## Dependencia MCP elegida

| Opción | Resultado |
|--------|-----------|
| **Spring AI 1.1.4** + `spring-ai-starter-mcp-server-webmvc` | **Seleccionada.** GA en Maven Central, Java 17, Spring Boot 3.4/3.5, transporte Streamable HTTP (`spring.ai.mcp.server.protocol=STREAMABLE`), modo `SYNC` (JPA bloqueante). Anotaciones oficiales `@McpTool` / `@McpToolParam` (`org.springaicommunity.mcp.annotation`). |
| Spring AI 2.0.x | **Incompatible.** La documentación oficial indica Spring Boot 4.0/4.1. |
| SDK Java `io.modelcontextprotocol.sdk` a pelo | Válido, pero se perdería el auto-config WebMVC que ya empaqueta Spring AI 1.1.4 (usa internamente `mcp-spring-webmvc` 0.17.0). |

Transporte remoto recomendado por la especificación MCP (2025-03-26): **Streamable HTTP**. SSE queda como legado. No se usa WebFlux.

## Arquitectura

```text
Cliente MCP  --Streamable HTTP /mcp-->  Spring Security (JWT)
                                         |
                                         v
                               BloodConnectMcpTools (@McpTool)
                                         |
                                         v
                                  McpQueryService
                                         |
          +------------------------------+------------------------------+
          v                              v                              v
 BloodRequestService          DonationCenterService          BloodCompatibilityService
          |                              |                              |
          v                              v                              v
        JPA / PostgreSQL (solo a través de services; nunca desde las tools)
```

Paquete: `com.bloodconnect.mcp` (`config`, `tools`, `dto`, `mapper`, `service`, `audit`, `security`).

Las tools no tocan repositorios de dominio. El único repositorio nuevo es `McpAuditRepository` (auditoría).

## Herramientas

### 1. `list_active_blood_requests`

Lista solicitudes `OPEN`/`IN_PROGRESS` no vencidas.

| Parámetro | Obligatorio | Notas |
|-----------|-------------|--------|
| `bloodType` | no | `A+`, `O-`, etc. |
| `provinceId` | no | |
| `municipalityId` | no | |
| `urgency` | no | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `search` | no | Hospital o sector (no nombre de paciente) |
| `page` | no | Default 0 |
| `size` | no | Default 20, máximo `MCP_MAX_PAGE_SIZE` (50) |

### 2. `get_blood_request`

| Parámetro | Obligatorio |
|-----------|-------------|
| `requestId` | sí |

Responde  «no encontrada» si no existe, está cancelada, completada, expirada o vencida. No distingue esos casos a propósito.

### 3. `find_nearby_donation_centers`

| Parámetro | Obligatorio | Notas |
|-----------|-------------|--------|
| `latitude` | sí | WGS84 |
| `longitude` | sí | WGS84 |
| `radiusKm` | no | Default 25, máximo `MCP_MAX_RADIUS_KM` (100) |

Delega en `DonationCenterService.nearby()`.

### 4. `check_blood_compatibility`

| Parámetro | Obligatorio |
|-----------|-------------|
| `donorBloodType` | sí |
| `recipientBloodType` | sí |

Respuesta: `compatible`, `explanation` (español, basada en la tabla existente), `disclaimer`.

No se inventaron reglas médicas. Se reutiliza el mapa ABO/Rh de `BloodCompatibilityService`.

## Minimización de datos

Los DTO MCP **no** incluyen:

- email, teléfono personal, documentos, tokens, contraseñas;
- nombre del paciente, teléfono de contacto, dirección residencial, referencia, descripción clínica;
- identidad de quien creó la solicitud.

Los centros sí pueden incluir dirección y teléfono **institucionales** (datos públicos del establecimiento).

## Autenticación (fase 1)

El endpoint Streamable HTTP (`POST/GET /mcp`) exige un **JWT Bearer** del mismo emisor que Angular/Flutter (`Authorization: Bearer <access-token>`).

Esto **no** cumple OAuth 2.1 / Protected Resource Metadata de la especificación MCP de autorización. No hay token passthrough a terceros: el servidor MCP es el propio backend y valida el JWT localmente.

Una API key estática **no** se ofrece como solución de producción.

Hasta implementar un Authorization Server OAuth 2.1 (o un gateway que lo haga), el servidor permanece **`MCP_ENABLED=false`** por defecto, también en el perfil `prod` y en Docker/Railway.

## Cómo ejecutar en local

1. PostgreSQL y backend habituales (`docs` del README raíz).
2. Login REST para obtener un access token.
3. Arrancar con MCP activo:

```bash
cd backend
export DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/bloodconnect_db
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export MCP_ENABLED=true
export JWT_SECRET=CHANGE_ME_WITH_A_SECURE_SECRET_AT_LEAST_256_BITS_LONG_FOR_HS256
mvn spring-boot:run
```

El endpoint Streamable HTTP queda en `http://localhost:8080/mcp`.

### Perfil STDIO (solo desarrollo)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mcp-stdio
```

Los logs van a **stderr** y a `logs/mcp-stdio.log`. No se escribe logging de aplicación en stdout.

## Cómo probarlo con un cliente MCP

### MCP Inspector

```bash
npx @modelcontextprotocol/inspector
```

Conexión:

- Transport: **Streamable HTTP**
- URL: `http://localhost:8080/mcp`
- Header: `Authorization: Bearer <ACCESS_TOKEN>`

Debe listar las cuatro tools e invocarlas.

### Postman e IntelliJ HTTP Client

1. `POST /api/auth/login` y copiar `accessToken`.
2. `POST http://localhost:8080/mcp` con:
   - `Authorization: Bearer <accessToken>`
   - `Content-Type: application/json`
   - `Accept: application/json, text/event-stream` (si falta, el backend lo completa)
3. Cuerpo `initialize`, luego `tools/list` y `tools/call`.
4. Si `initialize` responde el header `mcp-session-id`, reutilizarlo en las siguientes llamadas.

Un 401 con `"Se requiere autenticación..."` indica que falta el Bearer. Un 500 genérico de la API REST no debe aparecer en `/mcp`; si ocurre, revise el log del backend.

### Ejemplo JSON-RPC (initialize + tools/list)

Tras autenticarse, un cliente envía (simplificado):

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-03-26",
    "capabilities": {},
    "clientInfo": { "name": "inspector", "version": "dev" }
  }
}
```

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list"
}
```

### Ejemplo de llamada

`check_blood_compatibility`:

Entrada:

```json
{
  "donorBloodType": "O-",
  "recipientBloodType": "A+"
}
```

Salida (forma lógica):

```json
{
  "donorBloodType": "O-",
  "recipientBloodType": "A+",
  "compatible": true,
  "explanation": "Según las reglas ABO y Rh que ya usa BloodConnect RD, el tipo O- puede donar al tipo A+.",
  "disclaimer": "La compatibilidad sanguínea mostrada es informativa. La elegibilidad para donar debe ser determinada por profesionales de la salud. Esta información es orientativa; la compatibilidad definitiva debe confirmarla un centro médico."
}
```

`list_active_blood_requests` no incluye `patientName`, `contactPhone` ni `address`.

## Configuración del cliente (sin secretos)

Claude Desktop / Cursor (plantilla):

```json
{
  "mcpServers": {
    "bloodconnectrd": {
      "url": "https://TU_HOST/mcp",
      "headers": {
        "Authorization": "Bearer <ACCESS_TOKEN_NO_SUBIR_A_GIT>"
      }
    }
  }
}
```

No commitear tokens. El access JWT de esta fase caduca (`JWT_EXPIRATION`).

## Variables de entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `MCP_ENABLED` | `false` | Activa el servidor MCP y las tools |
| `MCP_SERVER_NAME` | `bloodconnectrd-mcp` | Nombre anunciado al cliente |
| `MCP_SERVER_VERSION` | `1.0.0` | Versión anunciada |
| `MCP_ENDPOINT` | `/mcp` | Path Streamable HTTP |
| `MCP_MAX_PAGE_SIZE` | `50` | Máximo `size` |
| `MCP_DEFAULT_PAGE_SIZE` | `20` | `size` por defecto |
| `MCP_DEFAULT_RADIUS_KM` | `25` | Radio por defecto |
| `MCP_MAX_RADIUS_KM` | `100` | Radio máximo MCP |
| `MCP_RATE_LIMIT_ENABLED` | `true` | Rate limit in-memory |
| `MCP_RATE_LIMIT_PER_MINUTE` | `60` | Tope por usuario/IP |

No hay secretos MCP nuevos. Se reutiliza `JWT_SECRET` (nunca en el repositorio).

## Auditoría y rate limiting

Cada invocación guarda `tool_name`, `invoked_at`, `outcome`, `principal_id`, `principal_role` y `error_code` en `mcp_audit_events`. **No** se persisten argumentos, JWT, email ni teléfonos.

El rate limit es un filtro in-memory por usuario autenticado (o IP). No sustituye un WAF ni un API gateway.

## Riesgos de privacidad

- Un JWT de usuario válido puede enumerar solicitudes públicas minimizadas y centros. Eso es intencional y acotado, pero sigue siendo información de salud agregada.
- El campo REST `search` original coincide con `patientName`; en MCP **no**.
- Coordenadas de solicitud son las que ya expone la API pública de hospitales; no se añaden direcciones residenciales.
- Logs no deben incluir tokens. El filtro JWT existente no los registra.
- No hay SQL generado por el modelo: solo parámetros tipados hacia services.

## Limitaciones de la primera versión

- Solo lectura. No crear/editar/cancelar solicitudes.
- Sin OAuth 2.1, PKCE, resource indicators ni discovery `/.well-known/oauth-protected-resource`.
- JWT de usuario de la app no es un access token de cliente MCP de larga duración.
- Rate limit no distribuido (una instancia JVM).
- STDIO no es el transporte de Railway.
- Sin recursos MCP ni prompts.
- Compatibilidad ABO/Rh informativa, no aptitud médica.

## Pasos antes de habilitarlo en producción

1. Implementar autorización MCP OAuth 2.1 (AS + RS, metadatos, audiencias, sin passthrough).
2. Definir un cliente confidencial o público dedicado, no reutilizar el JWT de Angular como solución definitiva.
3. Revisar el modelo de consentimiento y el registro de auditoría con retención.
4. Sustituir el rate limit in-memory por uno compartido (Redis/gateway).
5. Pruebas de fuga de PII sobre respuestas reales de staging.
6. Activar `MCP_ENABLED=true` solo cuando los puntos anteriores estén cerrados.
7. No presentar una API key compartida como cierre de seguridad.

## Archivos principales

- `backend/src/main/java/com/bloodconnect/mcp/`
- `backend/src/main/resources/db/migration/V19__mcp_audit_events.sql`
- `backend/src/main/resources/application-mcp-stdio.properties`
- `backend/src/test/java/com/bloodconnect/mcp/`
