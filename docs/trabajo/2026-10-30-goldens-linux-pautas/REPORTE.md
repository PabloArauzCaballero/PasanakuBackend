# Reporte — CI en verde: goldens de las pautas regenerados en Linux

> **AVANCE: 4 / 4 — 100,0 %.** «Frontend — Flutter y Angular» y «App — goldens (Linux)» en verde en #367 (test); #371 (dev) mergeado con los goldens verificados en Linux a mano porque el CI los saltó.

- Fecha: 2026-10-30/31 · Plan: [PLAN.md](./PLAN.md) · Ramas: `claude/goldens-linux-pautas-20261030` (→ test), `…-dev-20261030` (→ dev)
- Peldaño de evidencia alcanzado: `TESTED` en CI (test) y en contenedor Linux (dev).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | 2 goldens regenerados en Linux amd64, Flutter 3.44.8 | `docker run --platform linux/amd64 pasanaku-flutter-linux:3.44.8 flutter test --update-goldens …` + 2 corridas sin regenerar | `+2: All tests passed!` ×2 ([evidencia/linux-goldens.txt](./evidencia/linux-goldens.txt)) |
| H1.S1.M2 | PNG inspeccionados: carnet y persona detallados; la pantalla de pautas con su cabecera actual | lectura de los PNG | correctos |
| H1.S1.M3 | #367 a test con CI en verde, mergeado | `gh pr checks 367` | Frontend `pass` · App — goldens (Linux) `pass` · `MERGED` |
| H1.S1.M4 | #371 a dev mergeado; `dev` y `test` con el mismo contenido | contenedor Linux sobre el árbol de dev · `git diff --stat origin/dev origin/test` | `+2: All tests passed!` ([evidencia/linux-goldens-dev.txt](./evidencia/linux-goldens-dev.txt)) · diff vacío |

## A medias
ninguna

## Pendiente
ninguna

## Evidencia
```text
$ gh pr checks 367
pass · App — goldens (Linux)
pass · Frontend — Flutter y Angular (ADR-044)
pending · App — navegación en simulador iOS (macOS)
pending · Release iOS (macOS)
$ gh pr checks 371
skipping · App — goldens (Linux)
skipping · Frontend — Flutter y Angular (ADR-044)
```

## No cubierto
- Los jobs de iOS en macOS de #367 seguían `pending` al mergear (no corren goldens).
- En #371 el CI **saltó** Frontend y goldens; la verificación de dev es la del contenedor local, no la del CI.

## Desvíos del plan
ninguno

## Riesgos residuales
- **El detector de cambios del CI salta la app en PRs a `dev` que solo traen PNG cherry-pickeados** (#371): un golden roto puede entrar a `dev` sin que el CI lo vea. No se investigó (fuera de alcance).
- Correr la suite de la app en macOS da cientos de «fallas» de goldens que no existen en Linux; las cifras «336/338/353» de los reportes del 30/10 son eso (se agregó una corrección en cada uno).

## Decisiones y ambigüedades
- Los goldens de la app se generan **siempre en Linux** (`pasanaku-flutter-linux:3.44.8`), nunca en Mac: es lo que ya fija `.github/workflows/ci.yml:425`.
- Al mergear, se exigen en verde los checks que ejercitan el cambio (Frontend y goldens); los de iOS en macOS no se esperan.
