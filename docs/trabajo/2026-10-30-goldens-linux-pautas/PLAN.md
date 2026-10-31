# Plan — CI en verde: goldens de las pautas regenerados en Linux

- Fecha: 2026-10-30 · Repos afectados: Pasanaku (`apps/movil`) · Rama: `claude/goldens-linux-pautas-20261030` desde `origin/test` `cd134d0d`
- Predecesor: `2026-10-30-test-sano-e-ilustraciones` (#360/#361 se mergearon con el job «Frontend — Flutter y Angular» en rojo sin revisar los checks)
- Pedido de Pablo: «continua» (paso 4: tests en rojo).
- Resultado observable: el job «Frontend — Flutter y Angular» del CI pasa en un PR que toque la app móvil.
- Kill-test: el CI del PR sigue con `ilustracion_de_pauta_escenas.png` o `pautas_publicas_360.png` en rojo.

## Hechos (descubrimiento)
- Los goldens de la app se generan y comparan en **Linux** (`.github/workflows/ci.yml:425-458`, «H17.S9.M5: las líneas base de macOS no pasaban en Linux»). Los «336 rojos» locales son diferencias de render de macOS, no fallas: en el CI de #360 pasaron **872 y fallaron 2** — los dos goldens que generé en Mac (1,01 % y 4,32 %).
- #358 ya regeneró en Linux los de #351 (perfil y escáner).
- Imagen local `pasanaku-flutter-linux:3.44.8` (linux/amd64, Flutter 3.44.8, la misma versión del CI), usada por otra sesión para lo mismo (`docs/trabajo/2026-10-30-continuar-alta-desde-login/REPORTE.md:28,47`).

## Alcance
- IN: regenerar en el contenedor Linux `apps/movil/test/widget/goldens/ilustracion_de_pauta_escenas.png` y `pautas_publicas_360.png`; PRs a test y dev **esperando el CI en verde antes de mergear**.
- OUT: cualquier otro golden; el código de las ilustraciones.

## H1 — CI en verde
**CA:** Dado un PR con los goldens nuevos, cuando corre el CI, entonces «Frontend — Flutter y Angular» pasa. **DoD:** checks del PR. **Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Regenerar los 2 goldens en el contenedor Linux | dos corridas sin `--update-goldens` pasan (determinista) | `docker run --platform linux/amd64 pasanaku-flutter-linux:3.44.8 flutter test <2 tests>` ×2 | HECHO |
| H1.S1.M2 | Inspeccionar los 2 PNG nuevos | se ven el carnet y la persona detallados | lectura de los PNG | HECHO |
| H1.S1.M3 | PR a test con el CI esperado en verde y mergeado | «Frontend — Flutter y Angular» = pass; `MERGED` | `gh pr checks <n> --watch` · `gh pr view` | HECHO |
| H1.S1.M4 | Gemelo a dev con el CI en verde y mergeado | ídem; `git diff origin/dev origin/test` vacío | ídem | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Emulación amd64 en Apple Silicon lenta | minutos de espera | solo 2 archivos de test |
| Otro check del CI en rojo por causas ajenas | no se puede afirmar «verde» | clasificar contra el CI de otro PR reciente |
