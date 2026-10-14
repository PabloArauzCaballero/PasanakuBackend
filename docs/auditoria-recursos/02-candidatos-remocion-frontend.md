# Candidatos para retirar del repositorio Frontend

Fecha de evaluación: 2026-10-07. La condición de retiro es que las referencias de build/CI estén reemplazadas y que los archivos Frontend exclusivos de Backend hayan sido preservados. El estado actual es **no retirar todavía**: la CI de Frontend ejecuta varias de estas rutas directamente.

| Path o grupo | Origen/dueño objetivo | Duplicado | Uso/referencia actual en Frontend | Puede retirarse ahora | Riesgo / trabajo previo |
|---|---|---|---|---|---|
| `build.gradle.kts`, `settings.gradle.kts`, `settings-gradle.lockfile`, `gradle.properties`, `gradlew*`, `gradle/`, `buildSrc/`, `bd/` | Backend | Sí; 26 proyectos Gradle también están en Backend | `.github/workflows/ci.yml` ejecuta tareas Gradle; wrappers y plugins dan servicio a generación, compilación y pruebas backend | No | Trasladar gates de backend a Backend; aislar generación de OpenAPI; mantener capacidad de CI para validar contrato consumido |
| `servicios/`, `plataforma/` | Backend | Sí, con 14 servicios y módulos de plataforma | CI ejecuta pruebas/compilación; `scripts/` backend lee OpenAPI, configuración y módulos | No | Reemplazar los gates y obtener contrato/clientes desde artifact por SHA. Migrar los cambios backend exclusivos de Backend según corresponda |
| `sql/`, `seeders/` | Backend | Sí | Workflows aplican SQL, semillas y verificaciones en PostgreSQL; scripts de boveda/DDL dependen de SQL | No | Mover validaciones a Backend y versionar/publicar esquema solo si Frontend necesita una fixture concreta |
| `despliegue/`, `docker-compose.coolify.yml`, Dockerfiles de servicios | Backend (los Dockerfiles web/móvil son de Frontend) | Sí | Infraestructura/compose y scripts están en CI; compose contiene frontend y backend | Parcial, tras dividir archivos | Separar compose por repo; no retirar Dockerfiles de `apps/web`, `apps/backoffice` o `apps/movil` |
| Scripts backend en `scripts/` | Backend | Sí para los presentes en ambos | `ci.yml` ejecuta generadores y verificadores de SQL, descriptores, seguridad, contratos y despliegue | No | Clasificar uno por uno; mover cada gate con su fuente o sustituirlo por artifact/API reproducible |
| `.github/workflows/ci.yml`, `boveda-y-esquema.yml` | Cada repo mantiene CI propia | Mismo nombre; `ci.yml` modificado | El CI Frontend combina jobs backend, generación de clientes, apps Angular/Flutter, E2E y base PostgreSQL | No | Descomponer por responsabilidad y mantener una comprobación de compatibilidad contract-first |
| `clientes/` | Artefacto producido por Backend; consumido por Frontend | Solo Backend en los SHAs comparados | Workflow Frontend ejecuta `generateOpenApiClients`; las apps/paquetes referencian APIs y modelos | No copiar manualmente | Publicación inmutable por SHA/version; importar y compilar contra ese artifact |
| `docs/`, `planes/`, `.claude/` | Clasificar por contenido | En su mayoría idénticos | CI/scripts y agentes leen algunos documentos; los planes incluyen contratos y políticas de ambos dominios | No retirar en bloque | Reasignar documento por dueño y mantener enlaces a la fuente canónica |
| `apps/`, `packages/`, `package.json`, `yarn.lock`, `turbo.json` | Frontend | Sí; están en ambos | Aplicación Angular/Flutter, paquetes, workflow y build | Retirar del Backend después de migrar los 144 archivos backend-exclusivos y desacoplar CI | Backend no debe perder las validaciones actuales ni tests web; moverlos al repo Frontend o a un job por artifact |

## Revisión de referencias

La búsqueda en `.github`, `scripts`, `apps`, `package.json` y `turbo.json` confirmó invocaciones explícitas de `./gradlew`, validaciones de `sql/`, lectura de `servicios/*/openapi`, y scripts que generan clientes desde fuentes backend. Las menciones en comentarios/documentación también deben convertirse en enlaces entre repos al mover documentos, aunque no bloqueen ejecución.

## Criterio de cierre de PR-02

Para cada fila: workflow consumidor actualizado, archivo mantenido en un solo origen, historial/cambios exclusivos preservados, checkout limpio del repo separado y build pertinente reproducido. Hasta entonces, esta tabla es un inventario de candidatos, no una autorización técnica para borrado masivo.
