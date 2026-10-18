# Reporte — Arreglar el backoffice de TEST y los rojos de CI en `test` y `dev`

> **AVANCE: 10 / 10 — 100 %.** CI de `dev` y `test` arreglado y mergeado (#98, #103, #99). Backoffice nuevo desplegado y verificado en TEST (imagen `a65f64d4c64b`, contenedor `healthy`, el código publicado incluye `correo`). Guiones del VPS corregidos.

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md)
- PRs: #98 y #103 → `dev`, #99 → `test`, todos MERGED. #103 y #99 los armó la sesión `pasanaku-b6` partiendo de #98.
- Peldaño de evidencia: identidad `TESTED` (regresión completa) · frontend `TESTED` (lint y tests sin goldens) · backoffice en TEST `VERIFIED` (desplegado, responde 200, el código publicado trae el campo).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `VerificacionesControllerWebTest` compila. En `test` llegó por #95; mi commit `d397ac2b` quedó sin publicar | `webTest` 120/120 | PASS · evidencia/01 |
| H1.S1.M2 | spotless en `ExpedienteDeIdentidad` | `spotlessJavaCheck` | PASS · evidencia/01 |
| H1.S1.M3 | Barrido: `FilaDeExpediente` sale de `RevisionRepositorio`. La división de `UsuariosControllerWebTest` quedó la de #97 | `check` exit 0 · `testBarrido` 2/2 | PASS · evidencia/01, 02 |
| H1.S2.M2 | `reconstruir-si-cambio.sh` del VPS: si cambia un OpenAPI, corre `generar-clientes.sh` y reconstruye backoffice y web | `bash -n` + diff; copia `.bak-20261016-contratos` | PASS (ver Evidencia) |
| H2.S1.M1 | `dev`: spotless y barrido de identidad | `check` + `integrationTest` (151/35/120) | PASS · evidencia/02 |
| H2.S1.M2 | `dev`: `dart format` en `diseno_flutter` y `movil` | `yarn lint` | TODO OK · evidencia/02 |
| H2.S1.M3 | Reglas de frontend: `Espacio.s4`; separación de `cliente.dart`, `rutas.dart` y `hoja_de_seleccion.dart` | `yarn lint` + `flutter test` (115 sin goldens) | PASS · evidencia/02 |
| H3.S1.M1 | PRs mergeados en `dev` y `test` | `gh pr view` | MERGED |

## A medias
ninguna

### Cierre de H1.S2.M1 / H3.S1.M2 — Backoffice nuevo en TEST (HECHO)
- **Causa 1:** el cliente Angular generado en el VPS era del 15/10 y no tenía `correo`, así que la build daba `TS2339`.
- **Causa 2, encontrada en el camino:** el chequeo de `/opt/aportaya/bin/generar-clientes.sh` buscaba cada operación solo
  en `default.service.ts`. Las operaciones con `tags:` se generan en `<tag>.service.ts`: por ejemplo `garantia`
  `[respaldo]` va a `respaldo.service.ts`, y ahí estaba `reservarRespaldo`. El chequeo daba FALLA falsa, y así se cayeron
  el rescate de las 19:45 y la corrida de las 20:59. Se corrigió para que busque en `*.service.ts`
  (copia en `generar-clientes.sh.bak-20261016-tags`).
- **Resultado** (corrida del temporizador 21:24 → 21:55, sha `867b9cc`):
```text
[21:26:09] construyendo /opt/aportaya/bin/generar-clientes.sh   (sin FALLA)
[21:49:57] construyendo docker   (backoffice)
[21:51:44] construyendo docker   (web)
[21:55:12] listo en 867b9cc
Coolify: 13 · 18 · 19 · 20  finished (02:01–02:03 UTC)
backoffice-kd3vj8sxq5nw2ycm7htbra9f-20261017T020221  aportaya/backoffice:test  Up (healthy)
imagen backoffice:test = a65f64d4c64b · el contenedor usa sha256:a65f64d4c64
https://backoffice.aportaya.161.97.85.216.sslip.io → HTTP 200 · "correo" en chunk-BfE-FtQv.js y chunk-CEUEKSI4.js
```

## Pendiente
ninguna

## Evidencia
- `evidencia/01-identidad-test.txt` y `evidencia/02-dev.txt`.
- Diff del guion del VPS:
```text
> CONTRATOS='^servicios/[^/]+/src/main/resources/openapi/'
> echo "$CAMBIOS" | grep -qE "$CONTRATOS" && construir /opt/aportaya/bin/generar-clientes.sh
> FRONT_ANGULAR='^(…|clientes/angular/|servicios/[^/]+/src/main/resources/openapi/)'
```

## No cubierto
- `flutter analyze` y `flutter test` de `apps/movil` en local: requieren los clientes Dart generados. Los cubre el CI.
- Goldens de `diseno_flutter`: 15 fallan en mi máquina con y sin estos cambios (dependen de la plataforma).
- Los dos guiones corregidos del VPS se ejercitaron en la corrida 21:24 (regeneró clientes, construyó y desplegó).

## Desvíos del plan
- `VerificacionesControllerWebTest` se arregló **antes** de escribir el plan (incumple la regla 20). Quedó registrado en el plan.
- Reparto con otras sesiones: `pasanaku-b6` promovió `dev`→`test` y reemplazó #98 por #103 y #99, quedándose con la división de tests de #97. El rescate del backoffice lo lanzó otra sesión, así que no lo dupliqué.
- Se agregó H2.S1.M3 (reglas de `verificar_frontend`), que apareció al correr el lint completo.

## Riesgos residuales
- Una generación de clientes Dart **huérfana**, que quedó del servicio de autodespliegue que terminó en `failed`, corre sobre el mismo `/opt/aportaya/repo` que el rescate. Si chocan, el rescate puede registrar `FALLA generar-clientes`.
- Otros rojos de CI que encontró `pasanaku-7b`, fuera de este trabajo: `CU04CodigoPorCorreoTest` en `dev` desde `278dc8ca` (lo arregla #109), y Trivy por CVE-2026-47884/47890 (spring-webmvc) y CVE-2026-84782 (openssl).
- La contraseña del operador de TEST quedó visible en la conversación, porque la salida del `!` se muestra ahí. Conviene rotarla.

## Decisiones y ambigüedades
- "Corregilo" se interpretó como "los problemas ajenos listados en el reporte anterior". Se le avisó a Pablo en el chat.
- No existe un rol "super admin": el más alto es `ADMIN_PLATAFORMA` (GLOBAL), y lo tiene `USR000091`. Confirmar con Pablo si quiere que esa cuenta tenga además roles de cumplimiento.
- **Códigos de error de CU-04 sin definir en la bóveda** (hallazgo de `pasanaku-7b` en #109). La ficha de CU-04
  documenta hasta `AP-CU04-07`. `-08` se usa con más de un sentido (`POLITICA_FALTANTE` y `DESAFIO_CONSUMIDO_O_INEXISTENTE`
  en `identidad.yaml`), y `-10` existe solo en el YAML; desde `278dc8ca` es el que devuelve el pedido de código por correo.
  `verificar_boveda` exige códigos correlativos, así que agregar `-10` obliga a definir `-08` y `-09`. Es una decisión de
  contrato: **confirmar con Pablo** qué significa cada uno antes de tocar la ficha.
