# Límites entre repositorios

Estado objetivo acordado en la auditoría del 2026-10-07.

| Backend | Frontend | Artefactos generados/compartidos |
|---|---|---|
| `servicios/`, `plataforma/`, `bd/`, `sql/`, `seeders/` y descriptores | `apps/web`, `apps/backoffice`, `apps/movil`, `packages/`, Yarn y Turborepo | OpenAPI fuente pertenece a Backend; clientes Angular/Dart se generan allí y se publican de forma inmutable por SHA de commit |
| Gradle, scripts backend, Dockerfiles de servicios y despliegue de API | Scripts frontend, Dockerfiles web/móvil y despliegue frontend | Frontend descarga el artifact de clientes que corresponda a la versión de Backend validada |
| Validaciones de dominio, esquema, build y pruebas de servicios | Lint, typecheck, pruebas, accesibilidad, build y E2E de apps | Un workflow de integración valida explícitamente la combinación de ambos SHAs |

Reglas:

1. Un archivo fuente tiene un repo propietario; no se sincronizan copias a mano.
2. Cada artifact generado registra SHA de Backend, versión de generador y checksum.
3. Frontend valida contra un SHA de Backend fijado en configuración versionada, no contra una rama flotante.
4. La CI de cada repo ejecuta sus gates propios; el workflow de integración valida los dos SHAs y descarga artifacts reproducibles.
5. La migración conserva cambios y pruebas presentes en una sola rama antes de borrar la copia duplicada.

El checkout actual contradice estos límites: Backend y Frontend contienen casi el mismo árbol; ver [auditoría de duplicación](../auditoria-recursos/01-duplicacion-repos.md) y [candidatos de retiro](../auditoria-recursos/02-candidatos-remocion-frontend.md).
