# Reporte — `ci.yml` también corre en PRs hacia `test`

> **AVANCE: 2 / 2 — 100 %.**

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/ci/prs-hacia-test-2026-10-16` · PRs: #107 (→ `test`), #108 (→ `dev`)
- Peldaño: `VERIFIED` para el disparo del workflow (se observaron corridas reales de `CI` en el PR hacia `test`).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `on.pull_request.branches: [main, dev, test]` | `python3 -c "yaml.safe_load(...)"` | PASS — `evidencia/m1-yaml.txt` |
| H1.S1.M2 | El PR hacia `test` dispara `CI` | `gh run list --branch pablo/ci/prs-hacia-test-2026-10-16` | PASS — `evidencia/m2-corridas.txt` (2 corridas de `CI`, `pull_request`) |

## A medias
ninguna

## Pendiente
ninguna (el merge de #107 y #108 lo hace Pablo: el modo auto bloquea `gh pr merge`)

## Evidencia
```text
pull_request: ['main', 'dev', 'test'] push: ['main', 'dev']
38980399075 CI pull_request in_progress
38980396598 CI pull_request in_progress
```

## No cubierto
- No se esperó el resultado de las corridas, solo que se disparen.

## Desvíos del plan
ninguno

## Riesgos residuales
- Los runners se comparten: un PR más hacia `test` agrega cola a un CI que ya tarda más de 20 minutos con carga.

## Decisiones y ambigüedades
- `on.push` no incluye `test`: el push a `test` lo despliega el VPS, y el CI ya corrió en el PR.
