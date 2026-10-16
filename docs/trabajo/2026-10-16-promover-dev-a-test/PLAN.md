# Plan — Resolver conflictos de la promoción dev → test (PR #94)

- Fecha: 2026-10-16 · Repos afectados: PasanakuBackend · Predecesor: PR #94
- Resultado observable: existe una rama que contiene `dev` + `test` sin conflictos y que GitHub puede mergear en `test`.
- Kill-test: `git merge-tree --write-tree origin/test <rama>` informa conflictos, o el build/tests de identidad y backoffice fallan.

## Alcance
- IN: los 10 archivos en conflicto entre `origin/dev` y `origin/test`.
- OUT: cualquier otro cambio; no se toca `dev` ni `test` directamente.
- Ambigüedades registradas: las tres goldens binarias no se pueden fusionar. Supuesto: se toma la versión de `dev` (la más reciente) y se regeneran o verifican con el test de widget. Confirmar con Pablo.

## H1 — Rama de promoción sin conflictos
**CA:** Dado `dev` y `test`, cuando se mergea la rama en `test`, entonces no hay conflictos y conserva el comportamiento de ambos lados.
**DoD:** merge-tree sin conflictos · tests de identidad en verde · typecheck/tests del backoffice en verde.
**Estado:** EN CURSO

### H1.S1 — Resolución
**CA:** Ningún marcador de conflicto queda en el árbol.
**DoD:** `git diff --check` y `grep -rn '^<<<<<<<'` vacíos.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Resolver PROGRESO.md | sin marcadores, ambos lados presentes | `grep -c '^<<<<<<<' PROGRESO.md` → 0 | HECHO |
| H1.S1.M2 | Resolver backoffice cumplimiento (2 archivos) | compila y pasan sus tests | test/typecheck del backoffice → exit 0 | HECHO |
| H1.S1.M3 | Resolver identidad (4 archivos) | compila y pasan los tests | `./gradlew :servicios:identidad:test` → BUILD SUCCESSFUL | HECHO |
| H1.S1.M4 | Resolver goldens móvil (3 png) | test de widget de capturas pasa | `flutter test` de capturas → All tests passed | HECHO |
| H1.S1.M6 | `VerificacionesControllerWebTest` (roto ya en `test`: llama al constructor sin `telefono`) pasa `null` como teléfono | compila y pasa | `./gradlew :servicios:identidad:test` → BUILD SUCCESSFUL | HECHO |
| H1.S1.M5 | Commit, push y PR hacia test | PR mergeable | `gh pr view --json mergeable` → MERGEABLE | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Ambos lados implementan lo mismo distinto (A6 copiado aparte en #90) | Código duplicado | Comparar contra los commits de origen y quedarse con la unión coherente |
