# Plan — Llevar el rediseño bancario de la app móvil al repo que despliega

- Fecha: 2026-10-29 · Repos: PasanakuBackend (destino), PasanakuFrontend `claude/rediseno-bancario` (origen) · Predecesor: `PasanakuFrontend/docs/trabajo/2026-10-21-rediseno-bancario` (corte intermedio, 41/126)
- Resultado observable: el APK y la app web espejo de test muestran el rediseño «Banca Verde Profunda» (ADR-053).
- Kill-test: `git ls-tree -r origin/test | grep ADR-053` vacío (hoy) → no está hecho.

## Alcance
- IN: merge de la rama del rediseño sobre `dev`, conflictos, pruebas, goldens con Flutter 3.44.8, capturas, PR a dev y test.
- OUT: completar el resto del plan del rediseño (85 microtareas pendientes en el plan de origen).
- Ambigüedades: donde dev y el rediseño cambiaron lo mismo, se conservó la función de dev y el diseño del rediseño (detalle en el commit del merge). Confirmar con Pablo.

## H1 — El rediseño llega a test
**CA:** Dado el APK de test, cuando se abre la app, entonces se ve la tarjeta héroe del saldo, la barra flotante y los botones en píldora.
**DoD:** analyze + pruebas + goldens en verde; PR a dev y test; APK compilado.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | Merge sin marcadores | 0 conflictos | `git diff --name-only --diff-filter=U` vacío | HECHO |
| H1.M2 | Análisis limpio | sin issues | `dart analyze --fatal-infos` (movil, diseno) | HECHO |
| H1.M3 | Pruebas | 0 fallos | `flutter test` (3.44.8) | HECHO |
| H1.M4 | Goldens regenerados y mirados | pasan | `flutter test test/goldens` | HECHO |
| H1.M5 | Capturas de la app compilada | en `evidencia/` | Playwright | TODO |
| H1.M6 | PR a dev y test, APK | mergeados, APK publicado | URLs | TODO |
