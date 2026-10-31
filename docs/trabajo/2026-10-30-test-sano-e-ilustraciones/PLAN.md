# Plan — TEST sano tras la sincronización, ilustraciones HD en test/dev y nuevo TestFlight

- Fecha: 2026-10-30 · Repos afectados: Pasanaku (`apps/movil`, solo lectura sobre el servidor TEST) · Rama: `claude/ilustraciones-pautas-hd-20261030` desde `origin/test` `265fb807`, worktree `../Pasanaku-test-ilustraciones`
- Predecesores: `2026-10-30-escaneo-qr-y-perfil` (cerrado), `2026-10-30-ilustraciones-hd-pautas` (código escrito y verificado, sin commit, en la rama vieja `pablo/feat/backoffice-como-atlas-h0-h1-2026-10-18`)
- Pedido de Pablo: «ok hagamoslo» sobre: 1) verificar TEST tras el merge #356, 2) llevar las ilustraciones a test/dev y subir un TestFlight con todo.
- Resultado observable: (1) evidencia de que TEST corre el commit de `test` y responde; (2) las pautas de foto con las ilustraciones nuevas en `test` y `dev`, y un build nuevo en TestFlight que las incluye.
- Kill-test: (1) la API de TEST no responde o corre un commit viejo; (2) el golden `ilustracion_de_pauta_escenas.png` en `origin/test` sigue siendo el plano.

## Alcance
- IN: lectura del estado de TEST (HTTP y, si hay acceso, `git`/logs del host en solo lectura); copiar los 3 archivos de `ilustraciones/` y los 2 goldens desde el árbol viejo; PRs a test y dev; build y subida a TestFlight.
- OUT: reconstruir o reiniciar algo en el servidor (acción sobre algo compartido: se informa, no se hace); arreglar los tests ya rojos; revisar el código de los 35 commits de otras sesiones.
- Ambigüedades: las del reporte de ilustraciones («Ultra HD» = vector detallado; rasgos simples en el muñeco) siguen sin confirmar por Pablo.

## H1 — TEST sano tras #356
**CA:** Dado el merge #356 en `test`, cuando se consulta TEST, entonces la API responde y el despliegue corre el commit de `test` (o se informa con evidencia que no).
**DoD:** salidas pegadas en `evidencia/`. **Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | La API y los frentes de TEST responden | HTTP 2xx/4xx esperado, no 5xx ni timeout | `curl` a API, web, app | TODO |
| H1.S1.M2 | El commit desplegado coincide con `origin/test` | sha del host = `origin/test` o diferencia explicada | lectura en el host (`git -C /opt/aportaya/repo rev-parse HEAD`, log de reconstrucción) | TODO |

## H2 — Ilustraciones HD en test y dev
**CA:** Dado `origin/test`, cuando se abren las pautas de foto, entonces se ven el carnet y el muñeco detallados. **DoD:** tests en verde, PRs mergeados. **Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Copiar ilustraciones y goldens al worktree de test | analyze sin issues; goldens en verde | `flutter analyze lib/pantallas/identidad/ilustraciones` · `flutter test test/widget/ilustracion_de_pauta_golden_test.dart test/widget/pautas_publicas_test.dart test/widget/pautas_publicas_golden_test.dart` | HECHO |
| H2.S1.M1b | Partir `ilustracion_de_pauta.dart` (452) y `persona_de_ejemplo.dart` (277) en archivos ≤ 200 líneas sin cambiar el dibujo (descubierto: el trabajo anterior no corrió `yarn lint`) | golden idéntico, sin regenerarlo | `flutter test test/widget/ilustracion_de_pauta_golden_test.dart` contra el golden ya inspeccionado · `yarn workspace @aportaya/movil lint` sin fallas nuevas | HECHO |
| H2.S1.M2 | Regresión completa vs `origin/test` limpio | 0 fallas nuevas | `flutter test` con y sin el cambio | HECHO |
| H2.S1.M3 | PR a test y gemelo a dev, mergeados | ambos `MERGED`; `git diff origin/dev origin/test` vacío | `gh pr view` · `git diff --stat` | TODO |

## H3 — TestFlight con todo
| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Build desde `origin/test` (con ilustraciones, QR, perfil y la sincronización) subido | `EXPORT SUCCEEDED` | `scripts/testflight_ios.sh --solo-compilar` + `xcodebuild -exportArchive … -authenticationKey*` | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| TEST corre un commit viejo (el push solo reinicia imágenes) | la app nueva contra un backend viejo | se informa con evidencia; reconstruir es decisión de Pablo |
| Sin acceso SSH al host | H1.S1.M2 sin verificar | se verifica lo observable por HTTP y se declara |
