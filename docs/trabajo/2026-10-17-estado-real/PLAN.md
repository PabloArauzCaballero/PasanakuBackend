# Plan — «Arquitectura y estado» con la salud REAL de cada servicio

- Fecha: 2026-10-17 · Repos afectados: PasanakuBackend (+ espejo PasanakuFrontend) · Predecesor: tableros por rol (2026-10-16)
- Resultado observable: en `backoffice/operacion/estado`, la columna «Salud» dice lo que contesta cada servicio AHORA
  (arriba / con problemas / no responde), con la hora de la consulta; si no se puede consultar, lo dice, nunca «arriba».
- Kill-test: apagar un servicio en la pila local y que la pantalla siga diciendo «arriba».
- Por qué: Pablo, 2026-10-17: «FIJATE ESTA PESIMO ESTO … sigue lo mismo». La pantalla era una constante
  (`dominio/estado-del-sistema.ts`): «arriba» fijo para los 14 y «4 réplicas» cuando TEST corre una. La banda promete
  «en qué estado está de verdad»: mostraba un dato inventado (regla 00 §2.1).
- Trabajo en pausa mientras tanto: tableros por rol, H9.S2 (pila aislada D3) queda EN CURSO en su plan, sin procesos vivos.

## Alcance
- IN: endpoint `GET /indicadores/operacion/servicios` en `auditoria` (contrato + sonda + controlador + tests);
  la pantalla `operacion/estado` lo consume; «Réplicas» pasa a «Réplicas exigidas (producción)».
- OUT: contar réplicas en marcha (no hay orquestador que consultar; queda como hallazgo); `sistemas/servicios` (SLO).
- Ambigüedades: A1 — ¿quién puede ver la salud? Supuesto: TABLERO_SISTEMAS_VER o AUDITORIA_LEER (como el resto del
  tablero de sistemas); la pantalla sigue abierta y sin permiso dice «sin permiso para ver la salud». Confirmar: Pablo.

## H1 — Salud real en la pantalla de estado
**CA:** Dado el stack con un servicio caído, cuando un operador con TABLERO_SISTEMAS_VER abre `operacion/estado`,
entonces ese servicio sale «no responde» y los demás «arriba», con la hora de la consulta.
**DoD:** tests web y unitarios de auditoria, specs del backoffice, contrato regenerado, prueba visual y CI del PR.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Contrato `consultarSaludDeServicios` en `auditoria.yaml` | Genera `SalidaSaludDeServicios` | `./gradlew :servicios:auditoria:compileJava` exit 0 | HECHO — exit 0; modelos `SalidaSaludDeServicios` y `SaludDeServicio` generados |
| H1.S1.M2 | `SondaDeSalud`: readiness de cada servicio en paralelo, timeout 2 s, ARRIBA/DEGRADADO/CAIDO, 10 s de memoria | 200→ARRIBA, 503→DEGRADADO, sin conexión→CAIDO | `SondaDeSaludTest` con servidor HTTP local, verde | HECHO — 4/4 + `SaludDeServiciosTest` 1/1, `evidencia/H1-S1-backend.txt` |
| H1.S1.M3 | Controlador con permiso y matriz negativa | 200 con permiso; 403 sin él; 401 sin token | `SaludDeServiciosWebTest` verde | HECHO — 3/3, `SeguridadWebTest` 6/6 (401 de todas las rutas), `ArquitecturaTest` 5/5 |
| H1.S2.M1 | La pantalla consume el endpoint; constante solo con nivel/réplicas exigidas/porqué | Salud desde la API; cargando, error, sin permiso | `pantalla-de-estado.spec.ts` verde | HECHO — 7/7, `evidencia/H1-S2-front.txt` (a11y.spec vence por plazo, preexistente demostrado) |
| H1.S2.M2 | Prueba visual de la pantalla | 3 viewports, 2 temas | capturas miradas en `evidencia/` | HECHO — E2E 8/8 con contraste y desborde por captura; 12 capturas miradas; 3 defectos hallados y corregidos (`H1-S2-front.txt`) |
| H1.S3.M1 | PRs a dev y test, CI verde, espejo | mergeados | `gh pr checks` pegado | TODO |
