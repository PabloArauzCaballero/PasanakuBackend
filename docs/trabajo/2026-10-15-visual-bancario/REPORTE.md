# Reporte — Guías de captura sobrias e identidad visual institucional (app móvil)

> **AVANCE: 14 / 14 — 100,0 %.** Hitos H1 y H2 quedan A MEDIAS: falta verlo en un teléfono real (cámara + ML Kit) y que Pablo confirme las ambigüedades A1–A3.

- Fecha: 2026-10-15 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/feat/visual-bancario-2026-10-15` (desde `origin/test` 51982f75), worktree `.claude/worktrees/visual-bancario`. Subida a `origin` y `frontend-mirror`; PR #71 contra `test` (https://github.com/PabloArauzCaballero/PasanakuBackend/pull/71), sin mergear.
- Peldaño de evidencia alcanzado: `TESTED` + prueba visual por goldens renderizados (claro/oscuro, 360/412/600 dp, texto 130 %). **No** `VERIFIED`: no se corrió en dispositivo.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Rostro: un óvalo único (proporción 0,73) para las tres poses; perfiles con tres chevrones de giro hacia el lado pedido. Sin orejas, nariz ni cruz | `flutter test test/widget/silueta_de_cara_test.dart --update-goldens` | PASS 5/5 · `evidencia/despues-guias-silueta-y-carnet.png` |
| H1.S1.M2 | La prueba de vida ya no apila las cuatro esquinas sobre el óvalo | `grep GuiaDeEncuadre pantalla_prueba_de_vida.dart` → vacío | PASS · `despues-2-capturas-y-perfil.png` (2.ª) |
| H1.S2.M1 | Cámara del carnet: contorno ID-1 completo (85,6×54, radio 3,18 mm) con velo exterior; cara en la cámara de respaldo con el mismo óvalo | tests de captura | PASS 20/20 · `evidencia/h1-capturas-tests.txt` |
| H1.S2.M2 | Tarjeta del carrusel: superficie plana `surface` + `Elevacion.tarjeta`, esquema dibujado del carnet (foto + renglones / líneas de lectura del reverso / flechas de perfil) en lugar de ícono en círculo y degradé | ídem | PASS |
| H1.S2.M3 | Riel de cinco capturas en casillas redondeadas sin sombras | ídem | PASS |
| H2.S1.M1 | Bóveda + `tokens.json`: fondo `#F4F6F5`, acento de acción = `g700` (oscuro `g300`), títulos y cifras en Instrument 600, sombras 2/3 más bajas | `yarn workspace @aportaya/tokens test:front` + contraste | PASS 15/15 + 3/3 · `h2-s1-tokens.txt` |
| H2.S1.M2 | Regenerados `tokens.css` / `tokens.dart` | ídem (`generados-coinciden`) | PASS |
| H2.S2.M1 | Botón plano: sin degradé, sin halo del color del botón, sin brillo en reposo, sin «elevarse» al pasar el mouse; presión 0,98 | `flutter test` en `packages/diseno_flutter` | PASS 133/133 · `h3-suite-diseno-final.txt` |
| H2.S2.M2 | `peligro` en rojo sólido («Dar de baja tu cuenta») | ídem + golden perfil | PASS |
| H2.S2.M3 | Ilustraciones del tour sin el distintivo naranja (`o500` → `g700`) | goldens del tour | PASS · `despues-3-tour-y-oscuro.png` |
| H2.S2.M4 | Anillo del foco de tutorial en `g300` fijo (con el verde profundo se perdía en el velo) | `flutter test test/a11y/tutoriales_a11y_test.dart` | PASS 2/2 |
| H3.S1.M1 | Análisis estático | `dart analyze --fatal-infos` (movil y diseno) | No issues found |
| H3.S1.M2 | Regresión completa de la app | `flutter test` en `apps/movil` | 921 pass, 7 rojos **todos preexistentes** (ver Evidencia) |
| H3.S1.M3 | Hojas antes/después miradas | — | `evidencia/antes-*.png`, `evidencia/despues-*.png` |

## A medias
### H1 / H2 — hitos
- Qué anda: todo lo de la tabla, demostrado en goldens renderizados por Flutter.
- Qué no anda: nada observado en rojo atribuible a este trabajo.
- Qué falta exactamente: (1) correr el alta en un iPhone/Android real y mirar el óvalo y el contorno del carnet sobre la imagen de la cámara viva, con ML Kit midiendo; (2) confirmar A1–A3 con Pablo; (3) revisar CI y mergear el PR #71.
- Dónde quedó: rama `pablo/feat/visual-bancario-2026-10-15`, compila (`dart analyze` limpio), subida a los dos remotos, PR #71 abierto.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Verificación en dispositivo | TODO | Un build a TestFlight/dispositivo desde esta rama |
| Portales Angular (web/backoffice) | TODO | Heredan los tokens nuevos; nadie los miró. Decidir A1 |

## Evidencia
```text
$ flutter test   (apps/movil, final)
01:53 +921 ~1 -9: Some tests failed.
# 2 de los 9 eran goldens movidos por el retoque de ilustraciones; regenerados después:
$ flutter test test/goldens/h8_tour_oscuro_test.dart test/widget/pautas_publicas_golden_test.dart --update-goldens
00:01 +2: All tests passed!
# 7 restantes — los 7 fallan igual en origin/test (evidencia/fallos-base.lst, 306 rojos en la base):
h10_mfa_visual_test: MFA · cargando ×6  → no encuentra la etiqueta "Comprobando código..., enviando"
h8_break_ui_test: billetera · saldo cero y vacíos · 200 % · oscuro → Bad state: No element
$ comm -13 fallos-base.lst fallos-final.lst   → (vacío: ningún rojo nuevo)

$ flutter test  (packages/diseno_flutter)   → 00:12 +133: All tests passed!
$ yarn workspace @aportaya/tokens test:front → Test Files 3 passed · Tests 15 passed
$ python3 scripts/verificar_frontend.py movil → 3 FALLAS, todas en archivos no tocados:
  main_h6_s3_m8_profile.dart (dinero/print), cu04_autenticar.dart:83 (print), dominio/tutoriales/motor.dart (256 líneas)
```
Índice: `h0-*` preparación (clientes generados), `h1-capturas-tests.txt`, `h2-*` tokens y diseño, `h3-suite-movil-BASE-origin-test.txt` (corrida de la base para comparar), `h3-suite-movil-final.txt`, `h3-gate-frontend.txt`, hojas `antes-*`/`despues-*`.

## No cubierto
- Cámara real y ML Kit: los goldens usan una cámara sintética (fondo negro).
- Portales Angular: consumen `tokens.css`; cambian de color y tipografía sin verificación visual.
- Gate de seguridad: no aplica (cambio visual, sin datos ni dinero).

## Desvíos del plan
- `BarraDePuntos` NO se quitó del paso de capturas (el plan decía quitarla): tiene contrato a11y testeado; se rediseñó sólo el riel.
- La tarjeta conserva `Elevacion.tarjeta` (el plan decía plana sin sombra): es el contrato de `Tarjeta`; la sobriedad sale de bajar las sombras en tokens.
- Se agregaron H2.S2.M3 (ilustraciones) y H2.S2.M4 (anillo del tutorial, regresión real detectada por la suite).
- Un `dart format lib test` reformateó 8 archivos fuera de alcance; se revirtieron a `origin/test` antes del commit.
- Se regeneraron goldens de pantallas que viven en la evidencia del trabajo predecesor (`2026-10-11-alta-atlas-sesion-premium/evidencia/h10-visual/`), porque los tests h10 comparan contra esas rutas.
- AGENTS.md pide leer completas ~80 skills; se leyeron completas `pasanaku-diseno`, `improve-ui` y `color-systems`. Las demás, no.

## Riesgos residuales
- Tests reescritos al nuevo diseño (con la misma fuerza, no debilitados): `silueta_de_cara_test` (ahora exige óvalo único y espejo exacto de las flechas), `roles_color_acento_test` (superficie sin degradé), `movimiento_test` (sin brillo en reposo; cargando = indicador), `roles_de_color_accion_test` (peligro rojo).
- El logotipo pasó de naranja a verde por el rol `accent`: es un cambio de identidad de marca.

## Decisiones y ambigüedades
- A1 «transversalmente»: se aplicó vía tokens compartidos → también cambian web y backoffice. **Confirmar con Pablo** si los portales deben cambiar.
- A2 Identidad: se conserva el verde de marca; el naranja deja de ser color de acción (queda en datos de turnos). **Confirmar con Pablo.**
- A3 Contradice una decisión previa de la casa («las acciones no usan verde/rojo», `roles_de_color_accion_test`): ahora la acción es verde y lo irreversible rojo. **Confirmar con Pablo.**
- No se descargaron skills nuevas: las de diseño pedidas (`apple-design`, `emil-design-eng`, `improve-ui`, `color-systems`, `ui-ux-pro-max`, etc.) ya estaban instaladas.
