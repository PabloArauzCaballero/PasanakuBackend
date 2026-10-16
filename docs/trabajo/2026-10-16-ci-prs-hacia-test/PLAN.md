# Plan — `ci.yml` también corre en PRs hacia `test`

- Fecha: 2026-10-16 · Repos: PasanakuBackend · Predecesor: `2026-10-16-ci-formato-test`
- Resultado observable: un PR con base `test` dispara el workflow `CI` (formato, barrido, frontend, tests).
- Kill-test: el PR de este mismo cambio hacia `test` no muestra checks de `CI`.
- Pedido: "de una vez porfa" (Pablo), sobre la propuesta de agregar `test` a `pull_request.branches`.

## Alcance
- IN: `.github/workflows/ci.yml`, solo la lista `on.pull_request.branches`.
- OUT: `on.push` (un push a `test` lo despliega el VPS; no se agrega CI de push), los `if` por `github.ref` (líneas 898 y 952, solo aplican a push).

## H1 — CI en PRs hacia `test`
**CA:** Dado un PR hacia `test`, cuando se abre, entonces corre el workflow `CI`.
**DoD:** YAML válido + checks de `CI` visibles en el PR hacia `test`.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `pull_request.branches: [main, dev, test]` | El YAML parsea y la lista tiene `test` | `python3 -c "yaml.safe_load(...)"` → `['main','dev','test']` | HECHO (`evidencia/m1-yaml.txt`) |
| H1.S1.M2 | PR hacia `test` y hacia `dev`, misma cabeza | El PR hacia `test` muestra corridas de `CI` | `gh run list --branch <rama> --workflow ci.yml` | TODO |
