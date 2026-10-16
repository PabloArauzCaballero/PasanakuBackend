# Plan — `test` vuelve a pasar formato, barrido y `dart format` después del merge de #95

- Fecha: 2026-10-16 · Repos: PasanakuBackend · Predecesor: PR #95 (promoción dev → test), plan `2026-10-16-arreglos-ci-y-backoffice` (otra sesión, rama `pablo/fix/arreglos-dev-ci-2026-10-16`)
- Resultado observable: en `test`, `./gradlew spotlessCheck testBarrido` y `dart format --set-exit-if-changed` en `packages/diseno_flutter` y `apps/movil` salen con código 0.
- Kill-test: cualquiera de esos tres comandos sale distinto de 0 en la rama de este trabajo.
- Pedido: "arreglalo porfa" (Pablo), sobre los rojos de CI heredados por `test` al mergear #95.

## Hallazgos (medidos sobre `origin/test` = `9ce01feb`)
1. `./gradlew spotlessCheck` → rc=1: `servicios/identidad/.../dominio/ExpedienteDeIdentidad.java`.
2. `./gradlew testBarrido` → rc=1: `RevisionRepositorio.java` 317 líneas, `UsuariosControllerWebTest.java` 300 (límite 300).
3. `dart format` → rc=1: 6 archivos en `packages/diseno_flutter`, 7 en `apps/movil`.
4. `ci.yml` solo corre en PRs hacia `main`/`dev`; por eso #95 no mostró estos rojos.
5. `dev ⊂ test`. Otra sesión tiene sin commitear, sobre `dev`, el formato Dart y la separación de
   `RevisionRepositorio` (→ `FilaDeExpediente`) y `UsuariosControllerWebTest`.

## Alcance
- IN: los tres rojos de arriba en `test`, con cambio mínimo y sin cambiar comportamiento.
- OUT: `dev` (lo arregla la otra sesión); cualquier otro rojo de CI.
- Ambigüedad: cómo dividir los archivos de 300+ líneas. Supuesto: se reutiliza la división de la otra
  sesión (vía `dev` → `test`) para que la próxima promoción no choque. Confirmado con la otra sesión por mensaje; si no responde, se consulta a Pablo.

## H1 — `test` en verde en formato y barrido
**CA:** Dado `test` tras este PR, cuando corre CI de formato/barrido/dart format, entonces los tres pasan y identidad sigue compilando y pasando sus tests.
**DoD:** los tres comandos del resultado observable con rc=0 + `./gradlew :servicios:identidad:test :servicios:identidad:webTest` en verde.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Traer el arreglo de `dev` de la otra sesión a la rama de `test` (merge) | merge sin conflictos sin resolver | `git diff --check` y sin `<<<<<<<` | HECHO — merge de #98 (`88a3538d`); conflicto en `RevisionRepositorio` resuelto con la versión de dev y `USUARIO.CORREO` movido a `FilaDeExpediente` (`evidencia/m1-merge.txt`) |
| H1.S1.M2 | `spotlessApply` en identidad | `spotlessCheck` pasa | `./gradlew spotlessCheck` → rc 0 | HECHO (`evidencia/m2-spotless.txt`, commit 6caf30e9) |
| H1.S1.M3 | `dart format` en `diseno_flutter` y `movil` | sin cambios pendientes | `dart format --output=none --set-exit-if-changed lib test` → rc 0 | HECHO (`evidencia/m3-dart-format.txt`, commit 6caf30e9) |
| H1.S1.M4 | Barrido por debajo de 300 líneas | `testBarrido` pasa | `./gradlew testBarrido` → rc 0 | HECHO (`evidencia/m4-barrido.txt`, rc=0) |
| H1.S1.M5 | Regresión de identidad | tests de identidad en verde | `./gradlew :servicios:identidad:test :servicios:identidad:webTest` | HECHO — test 35/35, webTest 120/120 con `--rerun` (`evidencia/m5-identidad.txt`) |
| H1.S2.M1 | PR → `test` (#99) y → `dev` con la misma cabeza | MERGEABLE, checks verdes | `gh pr view --json mergeable,statusCheckRollup` | EN CURSO |
| H1.S2.M2 | (agregada) Rama para `dev`: resolver la doble división de `UsuariosControllerWebTest` (#97 vs #98) | sin tests duplicados ni perdidos; checks de dev en verde | spotless/barrido/dart/identidad en `wt-dev` → rc 0 | HECHO — `evidencia/dev-*.txt`, test 35/35, webTest 123/123 |

Simulación del bloqueo (regla 65) para H1.S1.M1: el contrato es "dev sin rojos de formato/barrido". Si la
otra sesión no entrega, M2–M4 se ejecutan directamente sobre `test` (cambio deterministico para M2/M3;
para M4 se replica la misma división que ya tiene en disco la otra sesión).

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Dos divisiones distintas del mismo archivo | Conflictos en la próxima promoción | Una sola división, la de `dev` |
| Dos builds a la vez con la otra sesión | Timeouts espurios | Chequear `pgrep` de Gradle antes de cada corrida |
