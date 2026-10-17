# Plan — Corregir los pendientes que dejó el carril A del backoffice

- Fecha: 2026-10-16 · Repos afectados: PasanakuBackend (+ espejo) · Predecesor: `docs/trabajo/2026-10-16-backoffice-carril-a/`
- Resultado observable: el CI de `dev` queda en verde en los jobs que el carril tocó o heredó, y los defectos
  registrados en «Riesgos residuales» del reporte del carril que **no** esperan una decisión quedan corregidos y
  probados, en `dev` y en `test`.
- Kill-test: `gh pr checks` del PR a `dev` con algún job rojo que no esté declarado como preexistente con evidencia.

## Alcance

- IN:
  - CI de `dev` en rojo: formato Java de `identidad`, formato Dart de `packages/diseno_flutter` y `apps/movil`
    (rojos ya antes del carril; el pedido «corrijamos todos los pendientes» los trae al alcance).
  - Defecto propio: en #89 se renombraron los archivos de tres pruebas web pero no sus clases.
  - `MatrizDeAutorizacionContratoTest` (plataforma) en rojo por `actualizarFechaExpiracionDocumento`.
  - Dato personal en logs: `identidad/web/SesionesController.java:96` pone el teléfono en el MDC.
  - Documentación del permiso de `crearCampana` (el YAML dice uno, el código exige otro).
  - CU-47 y CU-48 reciben el aprobador en la entrada en vez de tomarlo de la sesión.
  - Llevar lo mismo a `test` (rama `-test`, cherry-pick).
- OUT (no se tocan, necesitan decisión; siguen en el reporte del carril):
  - 33 permisos S-8 sin catalogar (incluye `GOBIERNO_SESIONAR`), D-15, catálogo de finalidades, modelo del motor
    de decisiones, monedas en `cierre_diario`/`reclamo_cliente`/`alerta_monitoreo_lft`, fórmula de
    `conciliacion_custodia.diferencia`, umbrales de descuadre, A7.
  - `dev` del espejo del frontend (divergencia de ~467 archivos con PRs propios: pisarlo es destructivo).
  - Prueba `VERIFIED` con servicios levantados (el perfil `base` falla por la imagen de MinIO: `ENVIRONMENT`).
- Ambigüedades registradas:
  - `Traza.marcarCasoDeUso(cu, usuarioId)` recibe ids de recurso: renombrar la clave del MDC cambiaría los
    tableros de logs; solo se corrige el nombre del parámetro y su doc (sin cambio de comportamiento).
    Confirmar con: dueño de plataforma.

## H1 — CI de `dev` en verde

**CA:** Dado el PR de este trabajo contra `dev`, cuando corre el CI, entonces «Formato, reglas propias y
compilación», «Frontend — Flutter y Angular» y «La bóveda y el esquema no divergen» pasan.
**DoD:** `gh pr checks <PR>` sin fallas (salida pegada en `evidencia/`).
**Estado:** A MEDIAS (local verde; el CI de dev depende de H4)

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Nombres de clase de `CustodiaControllerWebTest`, `ContabilidadControllerWebTest`, `UifControllerWebTest` = nombre de archivo | clase = archivo en los tres | `grep -c "class <Nombre>"` = 1 en cada uno + `webTest` de nucleo-financiero y cumplimiento en verde | HECHO — `evidencia/H1-01-nombres-de-clase.txt`, `H1-03-identidad-y-web.log` |
| H1.S1.M2 | Formato Java de `identidad` (solo formato) | `spotlessCheck` limpio | `./gradlew :servicios:identidad:spotlessCheck test --no-build-cache` exit 0 | HECHO — `evidencia/H1-03-identidad-y-web.log` (spotlessCheck, test, webTest de identidad en verde) |
| H1.S1.M3 | Formato Dart de `packages/diseno_flutter` (Flutter 3.44.8, la del CI) | sin cambios pendientes | `dart format --output=none --set-exit-if-changed lib test` exit 0 + `flutter test` del paquete | HECHO — `evidencia/H1-02-formato-dart.txt` (format exit 0; flutter test 118/15 idéntico a `dev` limpio: las 15 son goldens macOS≠Linux, ENVIRONMENT) |
| H1.S1.M4 | Formato Dart de `apps/movil` | sin cambios pendientes | ídem en `apps/movil` | HECHO — `evidencia/H1-02-formato-dart.txt` (format exit 0) |
| H1.S1.M5 | (descubierta) `testBarrido` de `identidad` en rojo ya en `dev`: `RevisionRepositorio` 314 líneas y `UsuariosControllerWebTest` 300 | ningún archivo ≥ 300; mismas pruebas | `:servicios:identidad:testBarrido` + conteo de `@Test` antes/después | HECHO — `evidencia/H1-04-limite-de-lineas.txt` (12 = 12), `H3-01-regresion-y-gates.txt` |

## H2 — Defectos registrados sin decisión pendiente

**CA:** Dado cada defecto del reporte del carril que no espera decisión, cuando se ejercita, entonces el
comportamiento correcto queda probado.
**DoD:** prueba dirigida de cada uno + regresión del servicio tocado.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H2.S1.M1 | `actualizarFechaExpiracionDocumento`: declarar en `PoliticaDeAcceso` su control compensatorio (`TitularDelExpediente`) y probar que sin sesión no escribe | matriz verde; sin sesión = rechazo sin llamar al caso de uso | `:plataforma:comun-pruebas:contractTest` + `UsuariosSubirDocumentoWebTest` | HECHO — `evidencia/H2-01-fecha-expiracion.log` (matriz 8/8; prueba nueva 3/3: sin sesión 401, token de alta 401, otra sesión 403) |
| H2.S1.M2 | Ningún teléfono en el MDC del ingreso (CU-04) | el login no pone el teléfono en la traza | prueba web que inspecciona el MDC + `grep` sin `getTelefonoE164` en `marcarCasoDeUso` | HECHO — `evidencia/H2-02-sin-telefono-en-trazas.txt` (también estaba en el alta CU-01: corregido) |
| H2.S1.M3 | YAML de `crearCampana` documenta el permiso que exige el código | YAML = `@Permiso` | `grep` del YAML + `generar_postman.py` | HECHO — `evidencia/H2-03-permiso-crear-campana.txt` |
| H2.S1.M4 | CU-48 `activar` toma el aprobador de la sesión | sin `aprobadaPor` en la entrada; queda el usuario de la sesión | `integrationTest --tests '*CU48*'` | HECHO — `evidencia/H2-04-aprobador-de-sesion.log` (aserción reforzada a `aprobada_por = sesión`) |
| H2.S1.M5 | CU-47 `aprobar` toma el aprobador de la sesión | ídem | `integrationTest --tests '*CU47*'` | HECHO — `evidencia/H2-04-aprobador-de-sesion.log` (ídem) |
| H2.S1.M6 | Parámetro de `Traza.marcarCasoDeUso` nombrado por lo que es (referencia), sin cambiar la clave del MDC | firma y doc honestas | `./gradlew :plataforma:comun-web:test` | HECHO — `evidencia/H2-06-traza.log` (comun-web 11/0) |

## H3 — Integración

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Regresión de los servicios tocados y gates | 0 fallas | gradle por servicio + `verificar_*` + `generar_postman` | HECHO — `evidencia/H3-01-regresion-y-gates.txt` |
| H3.S1.M2 | PR a `dev` con CI verde y merge | merge hecho | `gh pr view --json state` = MERGED | A MEDIAS — #97 mergeado (839c03bc, por el dueño); el CI de `dev` siguió rojo por causas que no eran de #97, que se atacan en H4 |
| H3.S1.M3 | Rama `-test` con los mismos commits, verificada, PR y merge | merge hecho | regresión sobre el árbol de `test` + `gh pr view` | HECHO — `dev` y `test` quedaron con el mismo árbol tras la sincronización del dueño (#105); `git merge-base --is-ancestor 161284f4 origin/test` = sí |
| H3.S1.M4 | `test` del espejo sincronizado sin force | árbol = `origin/test` | `git diff --quiet` contra `origin/test` | HECHO — espejo `test` = `origin/test` (b225a5b0, merge con el árbol de origin, sin force) |
| H3.S1.M5 | REPORTE.md | escrito | `report_gate` | HECHO — `REPORTE.md` |

## H4 — (descubierto) Auditoría del CI de `dev` y `test` tras los merges rápidos

**CA:** Dado `dev` y `test` después de los merges del 2026-10-16, cuando corre el CI, entonces ningún job queda rojo por
una causa conocida y sin PR.
**DoD:** auditoría local de los 15 servicios + plataforma + frontend; un PR por causa; CI del PR revisado.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Regresión backend completa de `dev` | conteo y fallas por servicio | gradle de los 15 servicios + plataforma | HECHO — 3.721 pruebas, 1 falla (`CU04CodigoPorCorreoTest`); inversiones necesita `python` (ENVIRONMENT local, 23/23 con shim) |
| H4.S1.M2 | `CU04CodigoPorCorreoTest` espera AP-CU04-10 + Postman de identidad regenerado | identidad verde | `:servicios:identidad` completo + `verificar_*` | HECHO local — PR #109 |
| H4.S1.M3 | Trivy: espejar en `.trivyignore` las excepciones H17 de Spring 6.2.19 | sin CRITICAL bloqueante de esas CVE | doc de Trivy (`exp:`) + Maven Central sin 6.2.20 | HECHO local — PR #110 |
| H4.S1.M4 | Trivy: openssl de la imagen runtime | `libssl3t64` 3.0.13-0ubuntu3.16 | contenedor del mismo digest: .15 → .16 | HECHO local — PR #110 |
| H4.S1.M5 | `f1` lint frontend (tokens, backoffice 206 líneas) | `yarn lint` exit 0 | `yarn lint` | HECHO local — PR #110 |
| H4.S1.M6 | simulado: ejemplos de identidad sin `telefono` | 307/307 | `vitest run` en simulado | HECHO local — PR #110 |
| H4.S1.M7 | Clientes Dart versionados sin los modelos nuevos | `yarn typecheck` exit 0 | typecheck con clientes generados de a uno | HECHO local — PR #110 |
| H4.S1.M8 | CI de #109 y #110 en verde | jobs sin falla | `gh pr checks` | A MEDIAS — CI de #110: Trivy, imagen, formato, contratos y `f1` en verde; corredores rojo solo por CU-04 (lo arregla #109); `f2` rojo por goldens móviles desactualizados en Linux (anverso-360 31,6 %, capturas_* ~1 %, pautas_publicas 4,7 %, cabecera_fase_2 0,8 %) |

## Riesgos y bloqueos previstos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Otra sesión está cambiando `identidad` en `dev` en paralelo (#91, #93) | conflicto de merge | trabajar sobre el último `origin/dev`, refrescar antes del PR, cambios mínimos |
| El formato Dart depende de la versión: con otra versión el diff no coincide con el CI | CI sigue rojo | Flutter 3.44.8 local = CI; `flutter pub get` antes de formatear |
| Reformatear archivos ajenos | ruido en el historial | solo los archivos que el CI marca; commit propio de formato, sin otro cambio |
