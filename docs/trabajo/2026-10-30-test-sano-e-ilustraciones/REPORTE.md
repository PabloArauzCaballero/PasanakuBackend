# Reporte — TEST sano tras la sincronización, ilustraciones HD en test/dev y nuevo TestFlight

> **AVANCE: 7 / 7 — 100,0 %.** TEST desplegó la sincronización y responde; ilustraciones en `test` (#360) y `dev` (#361); build **202610302027** subido a TestFlight.

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Ramas: `claude/ilustraciones-pautas-hd-20261030` (→ test), `claude/ilustraciones-pautas-hd-dev-20261030` (→ dev)
- Peldaño de evidencia alcanzado: `TESTED` + golden inspeccionado (app); TEST verificado solo por HTTP y lectura del host. Sin corrida en dispositivo físico.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | La API y los frentes de TEST responden | `curl` a health, perfil, web, backoffice, app | 200 · 401 sin token · 302 · 302 · 302 ([evidencia/h1-http.txt](./evidencia/h1-http.txt)) |
| H1.S1.M2 | El autodespliegue tomó solo la sincronización: `listo en 265fb80` (20:26 host) y ya reconstruye `e945c72` = `origin/test`, el mismo commit del build de TestFlight | `ssh … git -C /opt/aportaya/repo rev-parse HEAD` + `autodespliegue.log` (solo lectura) | host en `e945c723` = `origin/test` ([evidencia/h1-despliegue.txt](./evidencia/h1-despliegue.txt)) |
| H2.S1.M1 | Ilustraciones copiadas al árbol de test; goldens regenerados ahí (el fondo y la pantalla de pautas cambiaron en test) e inspeccionados | `flutter test test/widget/ilustracion_de_pauta_golden_test.dart test/widget/pautas_publicas_test.dart test/widget/pautas_publicas_golden_test.dart` | `+4: All tests passed!` |
| H2.S1.M1b | Archivos partidos a ≤ 200 líneas (7 archivos, máx. 161) sin cambiar el dibujo | mismo test **sin** `--update-goldens` · `yarn workspace @aportaya/movil lint` | `+4: All tests passed!` · `TODO OK` |
| H2.S1.M2 | Regresión completa en test | `flutter test` con y sin el cambio | 336 vs 338 → **0 nuevas**, 2 menos ([evidencia/fallas-nuevas.txt](./evidencia/fallas-nuevas.txt) vacío) |
| H2.S1.M3 | #360 a `test` y #361 a `dev` (cherry-pick; en dev 336 fallas, todas ya presentes en test limpio) mergeados; `git diff origin/dev origin/test` vacío | `gh pr view` · `git diff --stat` | `MERGED` / `MERGED` · diff vacío |
| H3.S1.M1 | Build **202610302027** de `origin/test` `e945c723` (ilustraciones + QR + perfil + sincronización) subido | `bash scripts/testflight_ios.sh --solo-compilar` · `xcodebuild -exportArchive … -authenticationKey*` | `Upload succeeded` · `EXPORT SUCCEEDED` ([evidencia/testflight-subida.txt](./evidencia/testflight-subida.txt)) |

## A medias
ninguna

## Pendiente
ninguna

## Evidencia
```text
$ curl …/actuator/health → 200 · …/usuarios/yo/perfil → 401 (sin token, esperado)
$ autodespliegue.log (host, UTC-4)
[20:26:31] listo en 265fb80
[20:26:31] test avanzo: 265fb80 -> e945c72
$ flutter test (con cambio) → 336 fallas · (origin/test 265fb807 limpio) → 338 · nuevas: 0
$ yarn workspace @aportaya/movil lint → TODO OK
$ xcodebuild -exportArchive … → Upload succeeded · ** EXPORT SUCCEEDED **
```

## No cubierto
- TEST se verificó por HTTP (salud y un 401 esperado) y por el log del host; **no** se ejercitó un flujo con sesión (alta, ingreso, perfil, unirse por QR) contra el backend nuevo.
- La reconstrucción de `e945c72` en el host estaba en curso al cerrar; no se vio su `listo`.
- El build 202610302027 no se instaló ni se abrió en un teléfono; el procesado lo hace Apple.
- No se revisó el código de los 35 commits de otras sesiones que entraron con la sincronización.

## Desvíos del plan
- **Pérdida y recuperación de cambios en el worktree.** La primera regresión «con y sin el cambio» apartaba los archivos con `tar` usando rutas de `git status` (relativas a la raíz) desde `apps/movil`: `tar` falló, la corrida «con cambio» no ocurrió, y el paso de limpieza encadenado con `;` descartó los cambios igual. Se recuperó desde los originales intactos del árbol viejo, rehaciendo la partición con el mismo guion y verificando el golden **sin regenerar** (idéntico). La corrida de línea base de esa vez sí fue válida (árbol limpio) y se usó. La segunda vez no se movió ningún archivo.
- Se agregó H2.S1.M1b: el trabajo anterior de ilustraciones no había corrido `yarn lint` (452 y 277 líneas).
- No se usó `git stash`: la lista de stashes es compartida con worktrees de otras sesiones.

## Riesgos residuales
- `test` sigue con 336 tests en rojo (capturas de otras pantallas), ajenos a este trabajo.
- El build de TestFlight usa los barriles de `clientes/dart` regenerados desde los contratos (los versionados están atrasados).

## Decisiones y ambigüedades
- Siguen sin confirmar por Pablo: «Ultra HD» = dibujo vectorial detallado; la persona con rasgos simples (antes era un busto sin rasgos).
- No se reconstruyó ni reinició nada en el servidor: el autodespliegue lo hizo solo.


## Corrección (2026-10-31)
Las cifras de «fallas en `test`» de este reporte (300+) salieron de correr en **macOS** goldens que el proyecto genera y compara en **Linux** (`.github/workflows/ci.yml:425`): son diferencias de render, no fallas. El CI real de los PRs de este trabajo terminó con el job «Frontend — Flutter y Angular» **en rojo** y se mergeó sin revisar los checks. Se corrigió en [2026-10-30-goldens-linux-pautas](../2026-10-30-goldens-linux-pautas/REPORTE.md) (#367/#371).
