# Reporte — Recarga de saldo simulada de punta a punta, con puertos y adaptadores

> **AVANCE: 0 / 50 — 0,0 %.** Plan escrito y entregado. La ejecución la lleva la sesión de Pasanaku (session_01Nyoe6askYwdYBBAMWbzb3v).

- Fecha: 2026-10-24 · Plan: [PLAN.md](./PLAN.md) · Inventario: [PENDIENTES-HEREDADOS.md](./PENDIENTES-HEREDADOS.md) · Rama: `claude/plan-recarga-simulada-hexagonal` (desde `dev` 46d285dd)
- Peldaño de evidencia alcanzado: `DISCOVERED`. Hay hallazgos citados con ruta, que vienen de cuatro agentes de planificación de solo lectura. No se escribió código ni se corrió ninguna prueba.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| (plan) | Plan consolidado con 9 hitos y 50 microtareas, cada una con CA y DoD | `ls docs/trabajo/2026-10-24-recarga-simulada-hexagonal/PLAN.md` | El archivo existe (commit 2a581f22) |
| (inventario) | Inventario de unos 87 pendientes heredados, clasificados en las clases a–e, con lo ya resuelto y su evidencia | `ls docs/trabajo/2026-10-24-recarga-simulada-hexagonal/PENDIENTES-HEREDADOS.md` | El archivo existe (commit acd8dea3) |

Estos dos ítems son artefactos de planificación: no cuentan como microtareas del plan.

## A medias

Ninguna.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| H0.M1 a H8.M4 (50 microtareas) | TODO | Ejecución por la sesión de Pasanaku, que ya recibió el plan el 2026-10-24 |
| H7.S2.M4 | TODO, con riesgo de quedar BLOQUEADO | Red hacia `*.161.97.85.216.sslip.io` desde el entorno de la sesión. Si no se habilita, se cierra contra el doble según la regla 65 |

## Evidencia

- Los hallazgos de partida, con `ruta:línea`, están en la sección «Hallazgos de partida» de [PLAN.md](./PLAN.md).
- La evidencia de cada pendiente heredado y de cada ítem ya resuelto está en [PENDIENTES-HEREDADOS.md](./PENDIENTES-HEREDADOS.md).

## No cubierto

- No se ejecutó ninguno de los comandos de DoD del plan. Algunos nombres de tarea (por ejemplo `yarn verificar:compose` y `test:e2e visual-recargas`) siguen el patrón del repo, pero no se verificó que existan. La sesión que ejecute los ajusta y los anota como desvío.
- No se reprodujo el defecto «solo el titular puede confirmar una recarga» (`CU10RecargarSaldo.java:187-188`). Se tomó de la lectura del código.

## Desvíos del plan

- La regla 70.1.2 limita a un subagente. Se lanzaron cuatro en paralelo porque el usuario lo pidió explícitamente. Los cuatro fueron de solo lectura y no tocaron archivos.

## Riesgos residuales

- Los del plan, sección «Riesgos y bloqueos previstos».
- `plan_status.py` subcuenta el avance real del repo, porque no reconoce los IDs `H1.M1`. Ver [PENDIENTES-HEREDADOS.md](./PENDIENTES-HEREDADOS.md).

## Decisiones y ambigüedades

- Decisiones U1–U3 del usuario (Justin), registradas en el plan: el QR lo genera el banco con el monto exacto; la confirmación es por consulta automática al banco; lo que no cuadra se trata como problema y no se acredita.
- Supuestos derivados de esas decisiones (quién acredita, pagos vencidos o que exceden el saldo máximo, nada de pagar desde la app, simulación habilitada en TEST, enmienda a ADR-033, cuentas del espejo) y las ambigüedades A1–A3: ver la sección «Decisiones del usuario» del plan. Se confirman con Justin y, en el caso de las cuentas contables, con contabilidad.
