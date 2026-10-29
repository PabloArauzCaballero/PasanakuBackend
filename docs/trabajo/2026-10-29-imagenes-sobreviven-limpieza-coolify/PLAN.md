# Plan — Las imágenes de TEST sobreviven a la limpieza diaria de Coolify

- Fecha: 2026-10-29 · Repos afectados: PasanakuBackend (ramas dev y test) · Predecesor: ninguno
- Resultado observable: un despliegue de TEST que llega después de las 00:00 UTC encuentra
  `aportaya/backoffice:test` (y las demás imágenes construidas en el host) y no falla con
  «No such image».
- Kill-test: construir una imagen con el Dockerfile y ver que `docker inspect` NO muestra
  `coolify.managed=true` → no está hecho.

## Hallazgos (fase 1)
- Despliegue 00:25 UTC del 29-10 (sha 5dafb0f) falló: `No such image: aportaya/backoffice:test`.
- `logs/autodespliegue.log`: la imagen se construyó OK a las 19:55 (hora del host, UTC-4).
- `docker_cleanup_executions`: limpieza forzada a las 00:00 UTC (20:00 local), 67 % → 47 % disco.
  `server_settings`: `docker_cleanup_frequency = 0 0 * * *`, `force_docker_cleanup = t`.
- `app/Actions/Server/CleanupDocker.php` (Coolify instalado en el host), `buildImagePruneCommand`:
  borra con `docker rmi` toda imagen etiquetada que no sea de una app con build de Coolify y que
  **no tenga la etiqueta `coolify.managed=true`**; las que usa un contenedor fallan en silencio.
  Coolify no construye las nuestras (`pull_policy: never`), así que ninguna está protegida
  mientras no la use un contenedor. También se había perdido `aportaya/web:test`.
- Rescate ya hecho en el host: reconstruidas backoffice y web en 5dafb0f y redesplegado
  (evidencia en el REPORTE).

## Alcance
- IN: `LABEL coolify.managed=true` en la etapa final de los cinco Dockerfile que se construyen
  en el host de TEST; nota en `despliegue/TEST.md`.
- OUT: configuración de limpieza de Coolify (servidor compartido con Atlas y otros 38 recursos);
  instalar en el host la versión versionada de `reconstruir-si-cambio.sh` (decisión de revisión
  de seguridad del 2026-10-28: se instala a mano).
- Ambigüedades: ¿marcar imágenes propias como `coolify.managed` tiene otro efecto en Coolify?
  Revisado `CleanupDocker.php`: la etiqueta solo se usa para excluir de `rmi` y, en contenedores
  detenidos, para podarlos; los contenedores de compose ya llevan esa etiqueta puesta por Coolify.
  Supuesto: sin otro efecto. Confirmar con Pablo.

## H1 — Las imágenes construidas en el host no se borran en la limpieza
**CA:** Dado una imagen `aportaya/*:test` construida en el host y sin contenedor, cuando corre la
limpieza de Coolify, entonces la imagen sigue existiendo.
**DoD:** build real con la etiqueta + simulación del comando de limpieza de Coolify sobre una
imagen sin uso, salida pegada.
**Estado:** A MEDIAS — falta observar una limpieza nocturna real con la etiqueta desplegada

### H1.S1 — Etiqueta en los Dockerfile
**CA:** Las cinco imágenes llevan `coolify.managed=true`.
**DoD:** ver microtareas.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `LABEL` en la etapa final de los 5 Dockerfile | `git grep` encuentra 5 | `git grep -c "coolify.managed=true" -- '*Dockerfile*'` → 5 archivos | HECHO |
| H1.S1.M2 | La imagen del backoffice construida en el host lleva la etiqueta | inspect = true | `docker image inspect -f '{{index .Config.Labels "coolify.managed"}}'` → `true` | HECHO |
| H1.S1.M3 | La regla de borrado de Coolify no la toca | sigue presente | réplica del filtro de `buildImagePruneCommand` sobre una imagen etiquetada y otra sin etiqueta, sin contenedor → la etiquetada queda, la otra se borra | HECHO |
| H1.S1.M4 | Documentado en `despliegue/TEST.md` | sección presente | `grep -n "coolify.managed" despliegue/TEST.md` | HECHO |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Las imágenes viejas etiquetadas no se podan nunca | disco | se reemplazan por tag `:test`; las huérfanas quedan sin tag y `docker image prune -f` (dangling) sí las borra |
