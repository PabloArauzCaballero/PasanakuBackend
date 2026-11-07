# Plan — Formato de Java en `dev` (rojo latente de Spotless, 14 archivos)

- Fecha: 2026-11-07 · Repos afectados: PasanakuBackend · Predecesor: [2026-10-30-rojos-de-dev](../2026-10-30-rojos-de-dev/PLAN.md)
- Resultado observable: el paso «2 · formato» (`spotlessCheck`) del job «Formato, reglas propias y compilacion» pasa sobre `dev` y `test`.
- Kill-test: `./gradlew spotlessCheck --continue` sobre el árbol de `dev` falla; si después del cambio sigue fallando en cualquier módulo, esto no está hecho.

## Alcance
- IN: el formato de los archivos que `spotlessCheck` marca. Se pensó que era uno (`ComoSistemaRepositorioTest.java`); al correr el check en todo el repo resultaron **14 archivos en 7 módulos** (el paso de la CI corta en el primer módulo que falla). Solo lo que produce `spotlessApply`.
- OUT: cualquier cambio de comportamiento o de aserciones; los otros rojos que destapó la corrida manual 42262700023 (Gitleaks sobre toda la historia, OSV-Scanner) y la prueba intermitente `deep_link_test 02` de iOS.
- Origen del hallazgo: corrida manual de la CI sobre `dev` (run 42262700023, job 128046662076, paso «2 · formato»). El job solo corre cuando un cambio toca Java, por eso el rojo estaba latente.
- Ambigüedades registradas: ninguna.

## H1 — El formato de Java pasa en `dev` y `test`
**CA:** Dado `dev` con violaciones de Spotless, cuando se formatean con la herramienta del proyecto, entonces `spotlessCheck` de todo el repo sale con 0, el repo compila, las pruebas unitarias de los módulos tocados pasan y el contenido sin espacios en blanco no cambia (salvo el orden de un import).
**DoD:** `spotlessCheck` del repo en 0 · compilación y pruebas unitarias de los 7 módulos · CI de los dos PR con «Formato, reglas propias y compilacion» en success.
**Estado:** A MEDIAS (verificado en local; falta la CI de los PR)

### H1.S1 — Formatear y entregar
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Reproducir la violación en local, en todo el repo | `spotlessCheck --continue` falla y lista los archivos | 14 archivos en 7 módulos · `evidencia/spotless.txt` | HECHO |
| H1.S1.M2 | Aplicar `spotlessApply` y comprobar que es solo formato | `spotlessCheck` del repo en 0; compila; las pruebas unitarias de los 7 módulos pasan; el contenido sin espacios no cambia salvo un reorden de import | `spotlessCheck` exit 0 · `compileJava`/`compileTestJava` exit 0 · `test` de los 7 módulos BUILD SUCCESSFUL | HECHO |
| H1.S1.M3 | PR a `dev` y gemelo a `test` (rama propia desde `test`), no draft, mergeables, con la CI real en verde | `mergeable_state` clean y «Formato, reglas propias y compilacion» en success | `pull_request_read` | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin Docker; Maven Central da 429 desde el proxy de la sesión | `spotlessApply` no resolvía dependencias | espejo de Gradle de solo esta sesión (ya borrado); `integrationTest` no se corre en local |
| Otros módulos con violaciones, escondidas por el corte en el primer módulo que falla | el paso seguiría rojo | **se materializó**: eran 14 archivos, no 1; se corrió `spotlessCheck` de todo el repo |
| El reformateo de 14 archivos choca con trabajo en curso de otras sesiones sobre los mismos archivos (varios son de dinero: `aportes`, `entregas`, `nucleo-financiero`) | conflictos de merge para ellos | solo formato, verificado sin espacios; se avisa en el PR; el merge lo decide una persona |
