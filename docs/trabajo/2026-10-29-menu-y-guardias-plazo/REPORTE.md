# Reporte — `menu-y-guardias.spec.ts` sin agotar el plazo

> **AVANCE: 2 / 2 — 100 %.**

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/loving-tesla-bpulmt` (PasanakuBackend)
- Peldaño: `TESTED` — es un cambio de una prueba; los tests dirigidos y la suite completa corren en verde (dos veces).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Un `beforeAll` carga una vez todos los `loadComponent`/`loadChildren` (recursivo), con plazo propio de 60 s; el primer caso deja de pagar la carga en frío | `ng test --include=src/app/nucleo/menu-y-guardias.spec.ts --reporters=verbose` | PASS 87/87 · «todos · PLATAFORMA» 4270 ms → 394 ms (`evidencia/antes.txt`, `evidencia/despues.txt`) |
| H1.S1.M2 | Regresión del backoffice | `yarn typecheck` · `yarn lint` · `yarn test:front` ×2 | EXIT 0 · TODO OK · 186/1320 y 186/1320; primer caso 398 ms y 393 ms dentro de la suite |

## A medias
Ninguna.

## Pendiente
Ninguna.

## Evidencia
```text
antes (origin/dev aa4b658b, solo):  'todos · PLATAFORMA' 4270ms · 'todos · oficial' 349ms · 87 passed
después (solo):                     'todos · PLATAFORMA' 394ms  · 'todos · oficial' 342ms · 87 passed
después (suite completa, 2 corridas): 398ms / 393ms · Test Files 186 passed · Tests 1320 passed
sabotaje (RECLAMO_VER → CODIGO_INVENTADO en menu.ts, revertido): 2 failed — "/operacion/reclamos: menú oculta, la ruta abre"
```
`evidencia/`: `antes.txt`, `despues.txt`, `sabotaje.txt`, `typecheck.txt`, `lint.txt`, `test-front-1.txt`, `test-front-2.txt`.

## No cubierto
- No se midió en la máquina de CI: GitHub Actions no arranca jobs en la cuenta (facturación).

## Desvíos del plan
Ninguno.

## Riesgos residuales
- La precarga crece con cada pantalla nueva; hoy tarda bastante menos que su plazo de 60 s (el archivo entero corre en segundos), pero ese plazo es el que hay que mirar si un día la carga de rutas se vuelve lenta.

## Decisiones y ambigüedades
- No se subió el plazo del caso ni el global de Vitest: la causa era la carga en frío de las rutas, no la matriz. La carga se hace una vez antes de los casos, con un plazo propio declarado.
- La prueba sigue detectando una divergencia menú ⇔ guardia (sabotaje arriba): no se debilitó.
