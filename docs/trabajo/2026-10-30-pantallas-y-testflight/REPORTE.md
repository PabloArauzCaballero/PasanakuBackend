# Reporte — Pantallas flojas de la app móvil (skills de marketing) y relanzamiento en TestFlight

> **AVANCE: 11 / 13 — 84,6 %.** Build 202610301429 subido a App Store Connect. 1 microtarea DESCARTADO (no suma) y 1 A MEDIAS (PRs abiertos, sin mergear al escribir esto).

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Ramas: `claude/pantallas-y-marketing-20261030` (PR #335 → test), `claude/pantallas-y-marketing-dev-20261030` (PR #336 → dev)
- Peldaño de evidencia: **TESTED** (analyze limpio, suite sin fallos nuevos) + prueba visual a 412 en claro. No `VERIFIED`: no se abrió el build en un teléfono contra TEST.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Capturas «antes» de las 48 pantallas del inventario (229 PNG) | `flutter test test/capturas/rediseno/capturas_test.dart --update-goldens --dart-define=FASE=2026-10-30-antes --name "412 · claro"` | exit 0, All tests passed |
| H1.S1.M2 | Diagnóstico priorizado | `test -s evidencia/diagnostico.md` | PASS · [diagnostico.md](./evidencia/diagnostico.md) |
| H2.S1.M1 | Permuta: botón anclado abajo | captura `grupos/permuta` | PASS · `evidencia/capturas/{antes,despues}-grupos-permuta__datos.png` |
| H2.S1.M2 | Retiro del grupo: ídem | captura `grupos/retiro` | PASS |
| H2.S1.M3 | Cambiar contraseña: ídem | captura `perfil/contrasena` | PASS |
| H2.S1.M5 | Verificación adicional: ídem | captura `perfil/verificacion-profunda` | PASS |
| H2.S2.M1 | Bienvenida tras el alta: panel arriba | captura `alta/bienvenida` | PASS |
| H2.S2.M2 | Billetera sin «Hola» suelto | captura `billetera/inicio` | PASS |
| H2.S2.M3 | Transparencia vacía sin jerga | captura `grupos/transparencia` (vacío) | PASS |
| H2.S3.M1 | Regresión sin fallos nuevos | `flutter analyze` · `flutter test …` árbol limpio vs cambios | PASS · [regresion.txt](./evidencia/regresion.txt) |
| H3.S1.M1 | Build 202610301429 subido | `xcodebuild -exportArchive … -authenticationKey*` | `Upload succeeded` · [subida.txt](./evidencia/subida.txt) |

## A medias
### H3.S1.M2 — PRs a test y dev
- Qué anda: rama pusheada; PR #335 (test) y PR #336 (dev, mismos dos commits cherry-pickeados sobre `origin/dev`).
- Qué no anda: al escribir esto el CI estaba `pending`; no están mergeados.
- Qué falta exactamente: CI en verde y merge de ambos. Si el job de goldens del CI falla en bienvenida/billetera, regenerar esos goldens en el CI de macOS.
- Dónde quedó: worktrees `../Pasanaku-pantallas` y `../Pasanaku-pantallas-dev`; ambos compilan.

## Pendiente
ninguna

## Evidencia
```text
$ flutter analyze
No issues found!
$ flutter test test/unidad test/widget test/contrato test/identidad test/pasanaku test/billetera test/navegacion test/a11y
fallos línea base: 8 · con cambios: 8 · diff: vacío
$ flutter test test/goldens
fallos línea base: 274 · con cambios: 274 · diff: vacío
$ xcodebuild -exportArchive … (build 202610301429)
Progress 100%: Upload succeeded.
** EXPORT SUCCEEDED **
```
Capturas antes/después: [evidencia/capturas/](./evidencia/capturas/).

## No cubierto
- Solo se miró 412 dp en claro; 360/600 y tema oscuro no se revisaron a mano.
- El build no se instaló ni se abrió en un teléfono.
- El teclado subiendo con la barra se dedujo del patrón de `pantalla_pedir_cupo.dart` (barra dentro del `body`); no se probó en dispositivo.
- Los 8 fallos no-golden y los 274 goldens locales vienen de `origin/test` y no se investigaron.

## Desvíos del plan
- `scripts/testflight_ios.sh` compiló pero la firma falló (`exportArchive Failed to Use Accounts`). Se firmó y subió con la clave de API de App Store Connect (`~/.appstoreconnect/env`), igual que el 2026-10-28.
- H2.S1.M4 (reclamo) DESCARTADO: la pantalla no tiene botón, es informativa.
- H2.S2.M1: se quitó el paso «Unirme a un pasanaku» porque los grupos se activan recién con la identidad verificada.
- La primera versión usaba `bottomNavigationBar`, que deja el botón bajo el teclado; se pasó a la barra dentro del cuerpo antes de commitear.

## Riesgos residuales
- `yarn lint` falla por `apps/movil/lib/pantallas/identidad/pasos_alta/paso_cuenta.dart` sin formatear (ya venía así en `origin/test`, último cambio ebcabc36). Fuera de alcance, anotado.
- Al regenerar, los barriles de `clientes/dart/{aportes,notificaciones,tarifas}` cambian respecto de lo commiteado; no se subieron.
- Los textos raros de las capturas («nombre», «puntaje», «Niveldeconfianza», «hashEsperado») son datos de ejemplo del contrato. Con datos reales no aparecen, pero no se comprobó contra TEST.

## Decisiones y ambigüedades
- «Muchas pantallas pésimas» no decía cuáles. Supuesto: las juzgué con capturas de todas las pantallas y arreglé los defectos de composición y texto que se repetían. **A confirmar con Pablo** al abrir el build: qué pantallas siguen sin gustarle.
- Las skills de marketing ya se habían aplicado (PRs #298–#305). Esta pasada fue correctiva (`onboarding`, `copywriting`, `cro`) y respetó `docs/trabajo/2026-10-21-rediseno-bancario/voz.md`.
- El estilo del botón deshabilitado (contorno pálido) es deliberado (`packages/diseno_flutter/lib/atomos/boton.dart:66`); no se tocó.
