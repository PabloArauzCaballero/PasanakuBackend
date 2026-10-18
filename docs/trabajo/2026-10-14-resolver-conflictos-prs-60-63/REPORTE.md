# Reporte — Resolver conflictos de los PRs #60 y #63

> **AVANCE: 8 / 11 — 72,7 %.** (H1.S1.M2 y H1.S1.M3 DESCARTADAS, no suman. H4.S1.M1 goldens/tests queda A MEDIAS.)

- Fecha: 2026-10-14 · Plan: [PLAN.md](./PLAN.md) · Rama local: `merge-test-en-dev` (commit a75de033)
- Peldaño de evidencia alcanzado: por área — ramas git: VERIFIED; APK: RUNS; IPA: RUNS (archivo compilado, no subido); nucleo-financiero Java: WRITTEN; TEST Contabo: sin cerrar.

## Completado
Ninguna.

## A medias
### H1.S1.M1 / H1.S1.M4 — Merge de test en dev y push
- Qué anda: `git merge -s ours --no-ff origin/test` sobre `origin/dev` (21cae5a8) produce un merge con 0 conflictos. Árbol del merge == árbol de `origin/dev` (verificado con `git rev-parse ^{tree}`).
- Qué no anda: el push `HEAD:dev` fue denegado por el clasificador de permisos; #60 y #63 siguen `CONFLICTING`.
- Qué falta exactamente: pushear `merge-test-en-dev` a `dev` y re-consultar `gh pr view 60` y `63`.
- Dónde quedó: worktree en el scratchpad (`wt-dev`), rama `merge-test-en-dev`, sin push.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S1.M2 (goldens) | DESCARTADO | El árbol de dev no cambia, no hay goldens que regenerar. Lo decidió el hallazgo de abajo. |
| H1.S1.M3 (tests) | DESCARTADO | Mismo motivo: código idéntico al de dev. |
| H2.S1.M1 (#63) | TODO | #65 ya mergeó `codex/alta-atlas-sesion-premium-2026-10-11` en dev; tras el push, confirmar si #63 queda redundante. Confirmar con Leonardolv21. |

## Evidencia
```text
git diff --stat origin/test b099e7e9      -> (vacío)  test == snapshot b099e7e9
git merge-base --is-ancestor b099e7e9 origin/dev -> sí
git merge -s ours --no-ff origin/test     -> Merge made by the 'ours' strategy.
tree dev == tree merge: SI ; conflictos: 0
```

## No cubierto
Estado de mergeabilidad en GitHub tras el push, y que el CI corra verde en #60.

## Desvíos del plan
Se descartó resolver los 113 archivos a mano: `test` no aporta contenido que `dev` no tenga.

## Riesgos residuales
`-s ours` descarta cualquier cambio exclusivo de `test`. Verificado vacío contra b099e7e9, pero si alguien commiteó en `test` fuera de esos 3 commits después de mi fetch, se perdería.

## Decisiones y ambigüedades
- `paso_celular.dart` y el pacto `nucleo-financiero-entregas.json`: ya no se deciden, el árbol de dev manda. Confirmar con Pablo.
- #63: confirmar con Leonardolv21 si se cierra por redundante.


---
## Actualización 2026-10-14 noche: dev==test, TEST en Contabo, APK/IPA/TestFlight

### Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1/M4 | Merge de test en dev empujado; #60 pasó a MERGED | `gh pr view 60 --json state` | MERGED |
| H2.S1.M1 | #63 quedó CLEAN y entró a test (merge e98b27c) | `git log origin/test` | PASS |
| H3.S1.M1 | `dev` == `test` en origin (f783db9b) | `git rev-parse origin/dev origin/test` | iguales, ver abajo |
| H4.S1.M2 | APK release contra TEST, 110,5 MB | `flutter build apk --release` | exit 0; `~/Downloads/APK-Pasanaku/aportaya-test-6-TEST.apk` ([apk-build.txt](evidencia/apk-build.txt)) |

### A medias
**H3.S1.M2 — TEST desplegado con f783db9b.** Qué anda: el stack de e98b27c sigue arriba (15 servicios + 3 fronts `healthy`, 4 dominios 200). Qué no anda: la tanda de f783db9 FALLÓ en 3 builds: (a) `nucleo-financiero`: `HechosPorHttp.comprometidoEnPozos` definido dos veces (javac, 1 error); (b) `backoffice`: clientes Angular del host viejos (`index.ts` del 16-sep vs `identidad.yaml` del 14-oct; falta `CaraDelExpediente`); (c) `movil-web`: va a fallar por `formatearDia` duplicada. Qué falta: mergear PR #67, correr en el host `bin/generar-clientes.sh` y `bin/construir-fronts.sh`, y `bin/disparar-despliegue.sh`. Dónde quedó: PR #67 abierto, rama `pablo/fix/compilacion-movil-test-2026-10-14` (2 commits), CI liviano en verde; el Java NO compiló localmente (jOOQ pide PostgreSQL).

**H4.S1.M3 — TestFlight.** Qué anda: `Runner.xcarchive` de 245,1 MB compilado desde la rama del PR #67 con build 202610141936. Qué no anda: `exportArchive` falla con `Failed to Use Accounts` (Xcode sin sesión de Apple); el clasificador me impide usar la clave de App Store Connect. Qué falta: que Pablo corra el comando de exportar/subir con `!`. Dónde quedó: archivo en el worktree `scratchpad/wt-release`, plist en `scratchpad/export-testflight.plist`.

**H4.S1.M1 — Goldens y tests en macOS/3.44.8.** `flutter test` apps/movil sin goldens de paquetes: 547 pasan, 39 fallan ([resumen](evidencia/flutter-test-movil-resumen.txt)): 38 por diferencia de píxeles (incluye `test/capturas` contra imágenes de `docs/`) y 1 no golden (`deep_link_test`: «MFA se muestra fuera del shell», no encuentra `mfaIndicacion`; el texto existe en `textos.dart:164`). Sin clasificar, sin regenerar baselines. `packages/diseno_flutter` no se corrió.

### Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H3.S1.M3 imágenes de fronts al día | TODO | host: generar clientes + reconstruir fronts tras el merge de #67 |
| Pendiente RELEVO-CODEX §5/§11 (carriles P, Q, R, T) | TODO | no verificados; ver ese documento |
| H10.S2.M1 deep link en emulador (PROGRESO) | TODO | emulador Android |

### Hallazgos para anotar (fuera de alcance, no arreglados)
- `scripts/testflight_ios.sh` afirma que los clientes Dart «ya vienen versionados»: es falso (`clientes/dart/*/lib/src` no está en git). Hay que correr `./gradlew generateOpenApiClients --no-parallel --no-build-cache` (Java 21) antes. Sin eso el archivo falla con `DefaultApi isn't defined`.
- El log `autodespliegue.log` del host imprime en claro la clave admin de la base (`BD_CLAVE_ADMIN`). Enmascarada en este reporte. **Conviene rotarla y dejar de pasarla como ARG de build.**
- El CI de PRs a `test` no compila Java ni Flutter (jobs `skipping`): por eso los tres duplicados llegaron a `test`.
- versionCode de Android: `AAAAMMDDHHMM` excede 2.100.000.000; el APK usa `AAMMDDHH`.

### Riesgos residuales
Hasta mergear #67, `test` no compila (iOS, nucleo-financiero) y Contabo sigue en e98b27c con fronts viejos.


---
## Cierre 2026-10-14 (23:15)
- **H3.S1.M2 HECHO / H3.S1.M3 HECHO** — Contabo corre `213cb7d3`: `aportaya-api`, `-web`, `-backoffice`, `-movil` en `finished`; 16 contenedores sanos; 4 dominios 200 ([evidencia](evidencia/contabo-despliegue-213cb7d.txt)).
- **H4.S1.M3 HECHO** — TestFlight: `Upload succeeded`, `EXPORT SUCCEEDED`, build 202610141936 ([evidencia](evidencia/testflight-subida.txt)). Apple todavía procesa; no se vio en la app TestFlight.
- **Incidente durante el trabajo:** el despliegue de 213cb7d3 dejó TEST caído unos minutos (API 503) porque faltaba la imagen `aportaya/identidad:test`; se reconstruyó con `bin/construir.sh identidad` y se re-disparó. Además, un `generar-clientes.sh` mío chocó con la tanda automática (bloqueo de caché de Gradle) y borró `clientes/angular`; se regeneró completo después.
- **Causa de fondo sin arreglar:** la tanda automática marca `listo en <sha>` aunque el despliegue de Coolify falle (`failed` en `application_deployment_queues`), y no regenera `clientes/angular`. Debe corregirse en `reconstruir-si-cambio.sh` (host, no versionado).
- **Verificación de operaciones de `generar-clientes.sh`: 16 FALLA** (garantia 6, inversiones 5, nucleo-financiero 3, identidad 2: p. ej. `emitirQr`, `leerQr`, `listarCorridasDeInversiones`). No impidieron compilar los frentes; sin clasificar.
- **No cubierto:** no se probó la app en un teléfono (ni APK ni TestFlight); no se ejercitó el recorrido de usuario contra TEST; no se rompió nada a propósito (regla 98.8).

---
## Pendientes atendidos 2026-10-15
- **testflight_ios.sh** — corregido: genera los clientes Dart con Java 21 si faltan. PR #68 a `test`, sin mergear. Peldaño `WRITTEN` (solo `bash -n`; no se corrió el script completo).
- **deep_link_test (MFA)** — `TEST_BUG`: la pantalla muestra `explicacionMfa`, la prueba buscaba `mfaIndicacion` (ya no se pinta en `lib`). Corregido, `flutter test test/widget/deep_link_test.dart` → 5 pasan. PR #69 a `test`, sin mergear.
- **Goldens (307 rojas en macOS; 268 de la app + 13 de `diseno_flutter` también rojas en Linux/Ubuntu, Flutter 3.44.8, contenedor Docker amd64)** — NO es solo diferencia de plataforma. Evidencia visual: `atomos_botones_claro` del baseline muestra botones con degradado y secundario verde; el código actual pinta botones planos marrones. Los baselines vienen de los commits de Leonardolv21 (13-14 oct) y no coinciden con el código. Tokens regenerados: idénticos, descartado. Clase: `DECISION_REQUIRED` (qué diseño es el correcto). No se regeneraron: hacerlo fijaría uno de los dos diseños sin que nadie lo haya elegido. **Confirmar con Pablo/Leo.**
- **`UnmountedRefException` en `h10_subida_visual_test`** — ocurre en `limpiar` del test tras el fallo de pixeles; secundaria, no investigada aparte.
- **frontend-mirror** — NO resuelto: `test` y `dev` del espejo divergen (13 y 21 commits propios, 6849 archivos de diferencia, conflictos en `yarn.lock` y `verificar_limites_compose.py`). Unirlos es decisión de contenido.
- **wt-release** — worktree eliminado.
- **Sin tocar:** rotación de la clave de la base, script de autodespliegue del host, 16 FALLA de `generar-clientes.sh`, carriles P/Q/R/T y H10.S2.M1.
