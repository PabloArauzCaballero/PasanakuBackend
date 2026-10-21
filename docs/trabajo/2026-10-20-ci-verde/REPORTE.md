# Reporte — CI en verde en dev y test sin regresiones

> **AVANCE: 5 / 10 — 50 %.** El CI todavía NO queda en verde: arreglarlo destapó fallas de fondo que antes no corrían (ver Pendiente).

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
| P1 | BLOQUEADO — `DECISION_REQUIRED` (seguridad) | `MatrizDeAutorizacionContratoTest`: la ruta **sin sesión** `PUT /usuarios/verificaciones/correo/{verificacionId}/borrador` (`guardarBorradorOnboarding`) **escribe sin control compensatorio** (posible IDOR por UUID), y la lista de rutas públicas cambió sin su decisión escrita en `PoliticaDeAcceso`. Requiere decidir el control (token de propósito único, sesión parcial, etc.). Dueño de identidad. |
| P2 | BLOQUEADO — `DECISION_REQUIRED` (contrato entre servicios) | `PactoConEntregasContratoTest` (nucleo-financiero): `entregas.yaml` no publica la ruta GET de la oferta que el consumidor espera. Decidir si se publica en `entregas` o se ajusta el pacto. |
| P3 | TODO | Pisos de cobertura: auditoria (ramas 0.60/0.70, líneas 0.85/0.89), identidad (líneas 0.77/0.79), notificaciones (ramas 0.75/0.82, líneas 0.84/0.86). Hacen falta pruebas nuevas; no se bajan los pisos. |
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
