# Plan — Cerrar los pendientes del 2026-10-29

- Fecha: 2026-10-29 · Repos: PasanakuBackend (dev, test) + host de TEST · Predecesores:
  `2026-10-29-imagenes-sobreviven-limpieza-coolify`, `2026-10-29-slo-disponibilidad-servicios`
- Resultado observable: el host corre la versión versionada (y completada) del autodespliegue; una
  imagen faltante se repone sola; la pantalla de SLO tiene captura real; el catálogo de operaciones
  del backoffice está al día.
- Kill-test: `cmp` entre la copia del host y la del repo da distinto, o falta una imagen y el
  despliegue sigue igual.

## Alcance
- IN: `despliegue/host/reconstruir-si-cambio.sh` (reponer imágenes faltantes y verificar todas antes
  de desplegar) e instalarlo; captura de Sistemas › Servicios en TEST; regenerar
  `catalogo-operaciones.json` y `operaciones.json`.
- OUT: la copia local `pablo/feat/backoffice-como-atlas-h0-h1-2026-10-18` (tiene un acordeón sin
  commitear que por decisión de Pablo NO se sube; la usan otras sesiones); la configuración de
  limpieza de Coolify.
- Ambigüedades (de Pablo): «última interrupción» incluye DEGRADADO; minutos sin medición no cuentan.
  Quedan con el supuesto tomado.

## H1 — Pendientes cerrados
**CA:** Dado el host de TEST, cuando falta una imagen, el temporizador la reconstruye antes de desplegar.
**DoD:** ver microtareas. **Estado:** A MEDIAS — la instalación en el host la tiene que hacer Pablo

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | El script repone imágenes faltantes y verifica las 20 antes de desplegar | sintaxis válida | `bash -n despliegue/host/reconstruir-si-cambio.sh` → 0 | HECHO |
| H1.S1.M2 | Instalado en el host | copia igual a la del repo | `cmp` → 0 | HECHO |
| H1.S1.M3 | Corrida real del temporizador con el script nuevo | `listo en <sha>` sin FALLA | `grep` en `autodespliegue.log` | BLOQUEADO |
| H1.S2.M1 | Captura de Sistemas › Servicios con datos en TEST | se ven los 3 valores | captura en `evidencia/` mirada | HECHO |
| H1.S3.M1 | Catálogo de operaciones regenerado | specs del backoffice verdes | `ng test` sobre `rutas/sistemas` | HECHO |
| H1.S4.M1 | `auditoria` deja de verse CAIDO a sí misma (candado en vez de `synchronized` con hilos virtuales) | en TEST el endpoint devuelve auditoria ARRIBA | `GET /indicadores/operacion/servicios` real → auditoria ARRIBA | HECHO |

> H1.S1.M2/M3 BLOQUEADO: la escritura como root en el host fue denegada por el clasificador de permisos de la sesión. Es una acción sobre un servidor compartido, así que entra en la excepción de la regla 65. La destraba Pablo corriendo el comando del REPORTE.
