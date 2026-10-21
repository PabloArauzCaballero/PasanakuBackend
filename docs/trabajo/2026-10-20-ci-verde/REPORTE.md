# Reporte — CI en verde en dev y test sin regresiones

> **AVANCE: 8 / 14 — 57,1 %.** P1 y P3 quedaron resueltos y P2 resultó no ser una falla del CI (2026-10-21). Falta el resultado del CI real (H1.S3.M2) y `diseno_flutter` (de Pablo). Ver «Cierre de P1–P3».

- Fecha: 2026-10-20 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/ci-verde-20261020` · PRs #168 (test) y #169 (dev)
- Peldaño de evidencia: `TESTED` en lo arreglado (reproducción local de cada job).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Criterios gherkin de los 3 casos de CU-69 | `python scripts/verificar_criterios.py` | PASS — «Sin divergencias» |
| H1.S1.M2 | spotless de `comun-mensajeria` y `comun-web` | `gradlew spotlessCheck` | PASS (0) |
| H1.S1.M3 | `CU69FirmaYSorteoTest` vuelve a su versión; casos nuevos en `CU69FirmaSinCupoTest` | `gradlew testBarrido` | PASS (0) |
| H1.S1.M4 | `UsuariosInternosRepositorio` 285 líneas (rotación de roles movida tal cual) | `gradlew testBarrido` | PASS (0) |
| H1.S1.M5 | identidad: fixtures con `nivel_riesgo`, prefijos de `ArranqueTest`, bind de `BorradorOnboarding` | `gradlew :servicios:identidad:integrationTest` | PASS 183/183 |

Corrida local completa del job «Los cinco corredores» (en serie):

```text
test => 0
webTest => 0
integrationTest => 0
contractTest => 1
sagaTest => 0
jacocoTestCoverageVerification => 1
```

## A medias
### H1.S3.M1 — Regresión backend completa
- Qué anda: `test`, `webTest`, `integrationTest` y `sagaTest` en 0 fallos en todo el repo.
- Qué no anda: `contractTest` (3 fallas) y `jacocoTestCoverageVerification` (pisos de cobertura). Nunca corrían en CI porque el job se saltaba («Formato» estaba rojo).
- Qué falta exactamente: ver Pendiente P1–P3.
- Dónde quedó: rama `claude/ci-verde-20261020`; compila.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| P1 | RESUELTO 2026-10-21 → H1.S4.M1 | Ver «Cierre de P1–P3». |
| P2 | DESCARTADO 2026-10-21 (solo Windows, CRLF) → H1.S4.M2 | Ver «Cierre de P1–P3». |
| P3 | RESUELTO 2026-10-21 → H1.S4.M3 | Ver «Cierre de P1–P3». |
| H1.S2 | DESCARTADO en esta PR (lo decidió el agente para no pisar trabajo en curso) | Los 3 arreglos de `diseno_flutter` (dart format, 25 literales → tokens, 6 archivos > 200 líneas) se hicieron y verificaron (`verificar_frontend.py diseno` → TODO OK, `flutter analyze` limpio), pero `origin/dev` recibió `c46d277a` (Pablo) que reescribe esos mismos 6 archivos: el merge chocaba en 5. Se revirtieron de esta rama para no pisar su trabajo. Queda para Pablo: correr `dart format` con Dart 3.12.2, aplicar el mapeo de colores de abajo (decisión del dueño: colores de marca) y partir los 6 archivos (con `part`/`part of` funciona sin cambiar la API). La versión hecha está en el commit `e66103f5` de esta rama como referencia. |
| H1.S3.M2 | EN ESPERA | Resultado del CI real de #168/#169 (pasos de Frontend que nunca corrían: typecheck, `flutter test`, a11y, app, Angular, e2e). |

## No cubierto
- Pasos del job de Frontend posteriores al lint (no se pudieron reproducir fielmente en Windows con Flutter 3.47.5; los mide el CI).
- Capturas visuales de los componentes con colores de marca (grilla de métricas, insignias, tour, tarjetas de puntos y riesgo): no hay goldens de esos componentes; falta mirarlos en el teléfono.

## Desvíos del plan
- Un helper propio para el porcentaje se descartó: difería de `toStringAsFixed(1)` en 400 de 100.001 valores. Se mantuvo `toStringAsFixed(1)` con `// permitido: porcentaje, no es dinero` (convención existente en `apps/movil/lib/dominio/validacion.dart`).
- La primera recomendación (pasar los colores Tailwind a tokens nuevos) se corrigió: viola `pruebas/tokens-contra-boveda.spec.ts`. El dueño eligió colores de marca.

## Riesgos residuales
- Cambio visible de colores en los componentes nuevos del detalle de miembro (decisión del dueño).
- `cifraGrande` sin sobrescribir: los números grandes pasan de 44/48 a 40.

## Decisiones y ambigüedades
- Mapeo de colores: verde→`ok`, azul→`info`, ámbar→`warn`, violeta de la grilla→`brand`, rojo→`err`, bronce→`o600/o100`, plata→`muted/cloud`, oro→`warn/warnbg`, diamante→`info/infobg`, esmeralda→`ok/okbg`, `#6B3FA0`→`Paleta.pii` (exacto), blanco→`Paleta.white` (exacto), negro→`Paleta.ink`. A confirmar con diseño (Pablo).

## Agregado después del merge con dev (2026-10-21)
| Qué | Por qué | Verificación |
|---|---|---|
| `identidad.usuario_interno_creado` sin `correo` en el payload | `SalidasSensiblesBarridoTest` (regla 90.2: ids, no contenidos). Ningún servicio consume ese evento. En Windows el barrido no lo detecta (CRLF); en el CI (LF) sí. | `identidad` compila; `webTest` 144/144 |
| `verificar_pruebas_web.py` reconoce `@PruebaWeb(<Controlador>.class)` | `AuditoriaController` (unificado el 19-oct) reparte su matriz en `Bitacora`, `CatalogosOperativos` y `DerechosControllerWebTest`, todos con `@PruebaWeb(AuditoriaController.class)` | «41 · con matriz propia: 41 · en deuda: 0» |
| `generar_compose.py`: `MAXIMO_DE_PARTICIPACIONES` (desarrollo `4`; desplegado `${...:-4}`) | El generador se negaba a escribir el compose por esa variable nueva de grupos | `verificar_limites_compose.py` OK; una línea nueva en `docker-compose.coolify.yml` |

Las PRs #168/#169 quedan **abiertas sin mergear**: el CI no puede quedar verde hasta resolver P1–P3 y el frontend de `diseno_flutter`. La última espera del CI la cortó el sistema por falta de memoria; el resultado de los últimos commits se ve en la PR.

## Cierre de P1–P3 (2026-10-21)
Decisión: el dueño del trabajo pidió «lo más recomendable y seguro posible» y delegó así la decisión de P1. Se tomó la opción que cierra el hueco sin cambiar el flujo de la app.

| ID | Qué se hizo | Verificación (local, Windows + Docker) | Resultado |
|---|---|---|---|
| H1.S4.M1 (P1) | `guardarBorradorOnboarding` (sin sesión) solo escribe con el token **CONSUMIDO** (código ya confirmado) y la huella del correo. Antes también aceptaba un token solo `ENVIADO`. El control quedó declarado en `PoliticaDeAcceso`. | `:plataforma:comun-pruebas:contractTest` · `:servicios:identidad:integrationTest --tests *BorradorOnboarding*` | PASS: matriz 8/8, `BorradorOnboardingGuardadoRepositorioTest` 4/4, `BorradorOnboardingRepositorioTest` 5/5. Con la condición vieja la prueba de `ENVIADO` falla ([evidencia](./evidencia/p1-p2-2026-10-21.txt)) |
| H1.S4.M2 (P2) | DESCARTADO: no es una falla. `entregas.yaml:406` sí publica `GET /entregas/mercado/ofertas/{ofertaId}`. La prueba busca `
` y la copia de Windows tiene CRLF (`core.autocrlf=true`). | `:servicios:nucleo-financiero:contractTest --tests *PactoConEntregas*` | Con CRLF: FAIL · con LF (como en git y en el CI): PASS 6/6. Clase `ENVIRONMENT`, sin cambio de código |
| H1.S4.M3 (P3) | Pruebas nuevas contra PostgreSQL real, sin bajar ningún piso: avisos internos (notificaciones), catálogos operativos y bitácora (auditoria), fecha de vencimiento (identidad) | `jacocoTestCoverageVerification` después de correr las suites completas de cada módulo | notificaciones líneas 0.84→0.91 / ramas 0.75→0.83 · auditoria 0.85→0.94 / 0.64→0.76 · identidad 0.787→0.793 / 0.70 — PASS los tres |
| H1.S4.M4 | **Bug encontrado por P3:** activar o desactivar un elemento de catálogo operativo siempre fallaba. La acción `ACTIVAR_/DESACTIVAR_ELEMENTO_CATALOGO` no está en `ck_bitacora_evento_accion` y el cambio nunca se guardaba (las pruebas web usaban dobles). Ahora se registra como `CAMBIO_ESTADO` (acción existente; el sentido queda en `activo`). Ningún consumidor usaba los nombres viejos. | `:servicios:auditoria:integrationTest --tests *CatalogosOperativosRepositorioTest*` + todas las suites de auditoria | PASS 6/6. Con el nombre viejo, 2 fallan por la restricción |

### Seguridad (regla 90.6) — P1
- Amenaza: con solo el id de verificación (UUID) y el correo se podía escribir el borrador del alta de otra persona sin haber probado el código.
- Control: `BorradorOnboardingRepositorio.validarEstadoToken` (`SOLO_CORREO_CONFIRMADO`) + `validarCorreoDelToken` (huella HMAC del correo).
- Pruebas: `BorradorOnboardingGuardadoRepositorioTest`, casos correcto, límite (ENVIADO) e inválidos (correo ajeno, id inexistente).
- Riesgo residual: el token CONSUMIDO sigue sirviendo para guardar el borrador hasta que se cree la cuenta, porque así se reanuda el alta. No se le agregó un vencimiento propio para no cortar altas largas.

### No cubierto
- El CI real de estos commits: ver H1.S3.M2.
- La app no se probó en un teléfono. No hizo falta cambiarla: solo guarda el borrador después de confirmar el código e ignora los errores del guardado (`notifier_alta.dart:122`, `verificacion_correo.dart:177`).

### Deuda anotada (fuera de alcance, sin tocar)
- `CatalogosOperativosRepositorio.actualizarEstadoElemento` arma el JSON de la bitácora pegando texto, y el `motivo` va sin escapar: un motivo con comillas rompe el JSONB y hace fallar el cambio. Hay que escaparlo o armar el JSON con ObjectMapper.
- `GestionarAvisosInternos.mapear` tiene un respaldo a `INFO` que nunca se ejecuta: la base solo admite los 6 tipos del enum.
