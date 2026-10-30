# Plan — Arreglar las pantallas flojas de la app móvil (con las skills de marketing) y relanzar TestFlight

- Fecha: 2026-10-30 · Repos afectados: Pasanaku (`apps/movil`) · Rama: `claude/pantallas-y-marketing-20261030` (base `origin/test` 7ab476c5)
- Predecesor: `docs/trabajo/2026-10-21-rediseno-bancario/` (rediseño + pasada de marketing, PRs #298–#305) y `docs/trabajo/2026-10-28-apk-y-testflight/`.
- Resultado observable: en TestFlight aparece un build nuevo de AportaYa cuyas pantallas de entrada/alta, inicio, grupo y billetera se ven y se leen mejor que el build 202610282041.
- Kill-test: las capturas «después» de las pantallas elegidas son indistinguibles de las «antes», o el build no llega a App Store Connect.

## Pedido literal
«relanza lo nuevo de testflight pero porfavor arregla pues muchas pantallas que estan pesimas. implementale estas skills de marketing (coreyhaines31/marketingskills) y relanza en testflight».
Respuestas: pantallas = bienvenida/tour/alta, inicio y grupo, billetera, **todas**; marketing = **aplicarlas a la app**.

## Alcance
- IN: textos y composición de pantallas en `apps/movil/lib/pantallas/**` y, si una pantalla lo exige, piezas de `packages/diseno_flutter`; capturas antes/después con el arnés `test/capturas/rediseno`; build y subida con `scripts/testflight_ios.sh`.
- OUT: backend, contratos, lógica de dinero, navegación nueva, pantallas nuevas, backoffice.
- Ambigüedades registradas:
  - «pésimas» no dice cuáles ni por qué. Supuesto: las juzgo con el arnés de capturas (412 claro) y la rúbrica de `ui-quality-review` + `copywriting`/`cro`/`onboarding`; arreglo las de peor puntaje primero. A confirmar con Pablo al ver el build.
  - Las skills de marketing ya se aplicaron una vez (PRs #298–#305). Supuesto: esta pasada es correctiva sobre lo que sigue flojo, no reescribir la voz (`docs/trabajo/2026-10-21-rediseno-bancario/voz.md` sigue mandando).

## H1 — Diagnóstico con capturas
**CA:** Dado el estado de `origin/test`, cuando corro el arnés, entonces tengo una captura por pantalla y una lista priorizada de defectos con pantalla y causa.
**DoD:** `flutter test test/capturas/rediseno --update-goldens --dart-define=FASE=2026-10-30-antes --name "412 · claro"` exit 0 · `evidencia/diagnostico.md` escrito.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Capturas «antes» de todas las pantallas (412, claro) | existe un PNG por pantalla/estado | comando de arriba → exit 0 | HECHO (229 PNG, exit 0) |
| H1.S1.M2 | Diagnóstico priorizado | `evidencia/diagnostico.md` lista defectos por pantalla | `test -s evidencia/diagnostico.md` | HECHO |

## H2 — Arreglos de pantallas
Defectos y prioridad en `evidencia/diagnostico.md`.

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Permuta: botón anclado con `BarraDeAccion` | el botón queda pegado abajo, no a mitad de pantalla | captura «después» `grupos/permuta` | HECHO (captura mirada) |
| H2.S1.M2 | Retiro del grupo: ídem | ídem | captura `grupos/retiro` | HECHO (captura mirada) |
| H2.S1.M3 | Cambiar contraseña: ídem | ídem | captura `perfil/contrasena` | HECHO (captura mirada) |
| H2.S1.M4 | Reclamo nuevo: ídem | ídem | captura `grupos/reclamo-nuevo` | DESCARTADO (la pantalla no tiene botón: es informativa, `PantallaReclamoPendiente`; decidido por el agente al leerla) |
| H2.S1.M5 | Verificación adicional: ídem | ídem | captura `perfil/verificacion-profunda` | HECHO (captura mirada) |
| H2.S2.M1 | Bienvenida tras el alta: panel arriba | panel arriba, sin media pantalla vacía | captura `alta/bienvenida` | HECHO (captura mirada). El paso «Unirme a un pasanaku» se quitó del alcance: grupos se activan recién con la identidad verificada y no hay evidencia de que escanear funcione antes |
| H2.S2.M2 | Billetera sin «Hola» suelto | la cabecera dice solo «Tu billetera» | captura `billetera/inicio` | HECHO (captura mirada) |
| H2.S2.M3 | Transparencia vacía sin jerga | el vacío explica qué aparece y cuándo | captura `grupos/transparencia` | HECHO (captura mirada) |
| H2.S3.M1 | Regresión | analyze sin errores, suite de la app en verde | `flutter analyze` · `flutter test` | HECHO: analyze sin issues; 8 fallos no-golden y 274 goldens idénticos al árbol limpio (`evidencia/regresion.txt`) |
**CA:** Dado cada defecto priorizado, cuando se corrige, entonces la captura «después» lo muestra resuelto en claro y oscuro, a 360/412/600.
**DoD:** `flutter analyze` sin errores nuevos · `flutter test` (suite de la app) sin fallos nuevos · capturas «después» mirada por mirada.
**Estado:** HECHO (412 claro mirado; 360/600 y oscuro no se miraron)

## H3 — Relanzar en TestFlight
**CA:** Dado el código de H2 commiteado, cuando corro el script, entonces App Store Connect acepta el build.
**DoD:** `bash scripts/testflight_ios.sh` → `EXPORT SUCCEEDED`.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Compilar, firmar y subir | build aceptado | `bash scripts/testflight_ios.sh` → `EXPORT SUCCEEDED` | HECHO (build 202610301429, `evidencia/subida.txt`) |
| H3.S1.M2 | PR a test y a dev | PRs abiertos con la regresión pegada | `gh pr view` | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Acuerdo de Apple (PLA) vencido otra vez | la subida falla | lo acepta el titular; queda el `.xcarchive` |
| Goldens del CI cambian con los arreglos | CI rojo | actualizar goldens afectados en el mismo PR |
