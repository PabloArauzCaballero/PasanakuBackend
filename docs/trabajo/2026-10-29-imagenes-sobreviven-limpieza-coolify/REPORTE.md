# Reporte — Las imágenes de TEST sobreviven a la limpieza diaria de Coolify

> **AVANCE: 4 / 4 — 100 %.** (El hito H1 queda A MEDIAS: falta ver una limpieza nocturna real.)

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/imagenes-sobreviven-limpieza-20261029`
- Peldaño de evidencia alcanzado: `TESTED`. El build real en el host lleva la etiqueta y una réplica
  de la regla de borrado de Coolify la respeta. La limpieza nocturna real sobre la imagen
  desplegada no se observó.

## Incidente y rescate (antes del cambio de código)
- Síntoma: el despliegue de `aportaya-backoffice` en 5dafb0f falló a las 00:25 UTC del 29-10 con
  `No such image: aportaya/backoffice:test`, y el backoffice quedó caído. También faltaba `aportaya/web:test`.
- Causa: el build salió OK a las 19:55 (hora del host). La limpieza forzada de Coolify corrió a
  las 00:00 UTC (20:00 local, 67 % → 47 % de disco) y borró las imágenes sin contenedor que no
  tenían `coolify.managed=true`. El despliegue de las 20:18 ya no las encontró.
- Rescate: se reconstruyeron backoffice (65 s) y web (80 s) en 5dafb0f con
  `disparar-despliegue.sh`. Las cuatro apps terminaron en `finished`. Backoffice y web responden 200,
  app 200 y `GET /actuator/health` del gateway da `UP`. `POST /api/v1/sesiones` por el
  backoffice devuelve el 400 de validación esperado (AP-VAL-01) con el cuerpo vacío.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `LABEL coolify.managed=true` en la etapa final de los 5 Dockerfile | `git grep -c "coolify.managed=true" -- '*Dockerfile*'` | PASS, 5 archivos · `evidencia/M1-grep.txt` |
| H1.S1.M2 | La imagen real del backoffice, construida en el host, lleva la etiqueta | `docker build … && docker image inspect -f '{{index .Config.Labels "coolify.managed"}}'` | PASS, `true` · `evidencia/M2-build-inspect.txt` |
| H1.S1.M3 | La regla de borrado de Coolify conserva la etiquetada y borra la otra | réplica del paso de `buildImagePruneCommand` acotada a dos imágenes de prueba | PASS: `Untagged: …:sin-etiqueta`, `:con-etiqueta` queda · `evidencia/M3-regla-de-borrado.txt` |
| H1.S1.M4 | Documentado en `despliegue/TEST.md` | `grep -n coolify.managed despliegue/TEST.md` | PASS · `evidencia/M4-doc.txt` |

## A medias
### H1 — Sobrevivir a la limpieza nocturna real
- Qué anda: la etiqueta está en las imágenes y la regla de borrado de Coolify la respeta (M2, M3).
- Qué no anda: nada observado.
- Qué falta exactamente: después de que la próxima limpieza corra a las 00:00 UTC con imágenes
  ya etiquetadas, comprobar `docker images --filter reference='aportaya/*:test'` (las 19
  presentes) y `docker_cleanup_executions` (`success`).
- Dónde quedó: rama `claude/imagenes-sobreviven-limpieza-20261029`, PRs a `dev` y `test`.

## Pendiente
Ninguna.

## Evidencia
```text
$ despliegue de ec4e9ad (autodespliegue.log): … OK movil-web en 633s · [21:30:34] desplegando · [21:30:35] listo en ec4e9ad
$ por cada aportaya/*:test → docker image inspect -f '{{index .Config.Labels "coolify.managed"}}'
movil-web=true web=true esquema=true transparencia=true tarifas=true publicidad=true organizador=true
nucleo-financiero=true notificaciones=true inversiones=true identidad=true grupos=true garantia=true erp=true
entregas=true cumplimiento=true auditoria=true aportes=true gateway=true backoffice=true   (20/20)
backoffice 200 · web 200 · app 200
```
```text
$ docker exec coolify-db psql … "select … from server_settings …"
localhost|0 0 * * *|80|t
$ … docker_cleanup_executions (última)
Forced Docker cleanup job executed successfully. Disk usage before: 67%, Disk usage after: 47%.
$ réplica de la regla (evidencia/M3-regla-de-borrado.txt)
antes: aportaya/prueba-limpieza:sin-etiqueta · aportaya/prueba-limpieza:con-etiqueta
Untagged: aportaya/prueba-limpieza:sin-etiqueta
despues: aportaya/prueba-limpieza:con-etiqueta
```

## No cubierto
- La limpieza programada real de Coolify no se ejecutó: se replicó su comando de borrado sobre dos
  imágenes de prueba.
- Las imágenes Java, la del esquema y la de la app móvil con la etiqueta no se construyeron en
  esta sesión. Se construyó solo la del backoffice; las otras cuatro usan el mismo `LABEL`.

## Desvíos del plan
Ninguno.

## Riesgos residuales
- (Resuelto 2026-10-28 21:30 hora del host: el despliegue de ec4e9ad reconstruyó las 20 imágenes y todas llevan la etiqueta.)
- La versión versionada de `despliegue/host/reconstruir-si-cambio.sh`, que verifica cada imagen
  antes de desplegar, **no está instalada** en el host: el host corre la del 16-10. Su
  instalación es manual por decisión de la revisión del 28-10.

## Decisiones y ambigüedades
- Se eligió la etiqueta en los Dockerfile y no tocar la limpieza de Coolify, porque el servidor se
  comparte con Atlas y otros recursos. Confirmar con Pablo.
- Supuesto: marcar `coolify.managed=true` no tiene otro efecto en Coolify. Según `CleanupDocker.php`,
  solo excluye del `rmi`. Confirmar con Pablo.
