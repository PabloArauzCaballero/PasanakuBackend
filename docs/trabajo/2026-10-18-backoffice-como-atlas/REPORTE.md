# Reporte — Backoffice como Atlas

> **AVANCE: 46 / 61 — 75,4 %.** Cifra de `plan_status.py`, marcada por esa herramienta como **NO CONFIABLE** (15 filas sin contar). No se recalculó ni se verificó en esta redacción.

- Fecha: 2026-10-21 · Plan: [PLAN.md](./PLAN.md) · Rama del reporte: `claude/rediseno-bancario` (el backoffice vive en `dev`, base `7752e1b`)
- Peldaño de evidencia alcanzado: **DISCOVERED** para todo lo que sigue. Se leyó el `PLAN.md` y la salida de `plan_status.py`; ningún comando de verificación del backoffice se ejecutó en esta sesión.
- Autor de este reporte: sesión de Claude que no es responsable del backoffice. Resume el plan; no reemplaza la verificación de su dueño.

## COMPLETADO
Ninguna microtarea. Ninguna tiene salida literal de su DoD ejecutada en esta sesión.

## A MEDIAS
Declarado en el `PLAN.md` y confirmado por `plan_status.py` como A MEDIAS.

### H2.S1.M5 — Explicador de la vista en el shell
- Qué anda (según el plan): el shell ya muestra la nota de la vista.
- Qué no anda: las bandas de propósito todavía existen en 48 pantallas; el plan dice que se retiran pantalla por pantalla dentro de H4.
- Qué falta exactamente: retirar las bandas en H4 y correr `grep -rc banda-de-proposito rutas` → 0.
- Dónde quedó: `PLAN.md`, fila H2.S1.M5.

### H4 — filas de rango (H4.S2.M1–M10, H4.S3.M1–M14, H4.S5.M1–M10, H4.S6.M1–M6, H4.S7.M1–M6, H4.S8.M1–M4)
- Qué anda (declarado): migración a `ap-cabecera-de-pagina` y `ExplicadorDeVista`, con specs según el plan.
- Qué no anda: el parser no cuenta estas filas de rango de forma individual; no hay estado verificado por pantalla.
- Qué falta exactamente: listar cada pantalla con su estado propio y ejecutar sus specs.
- Dónde quedó: `PLAN.md` secciones H4.S2–H4.S8.

## PENDIENTE
| ID | Estado | Qué lo destraba |
|---|---|---|
| H2.S1.M4 | TODO | Campana de avisos: depende de H5.S0 (backend real) |
| H4.S1.M5 | Sin estado reconocible | El plan dice «NO APLICA»; no es uno de los seis estados de la regla 20 §5. Hay que pasarlo a DESCARTADO con motivo |
| H5.S0 … H5.S14 | TODO | Contratos de backend por módulo; son subtareas, no microtareas: el parser las rechaza por formato (regla 20 pide H<n>.S<n>.M<n>) |
| H7.S1.M1 | TODO | Regresión completa y gates |
| H7.S1.M2 | TODO | Capturas 41 × 3 × 2 |
| H7.S1.M3 | TODO | PRs #test y #dev mergeados; TEST reconstruido; verificación en vivo con el admin |

## Evidencia
Índice de `evidencia/` tal como existe hoy, sin re-verificar: `H0-contraste-pii.txt`, `H3-S1-auditoria.txt`, `atlas-inventario.md`, `atlas-roles-y-permisos.md`, `sistema-visual.md`, carpetas `H1/` y `H2/`. No se corrió ningún comando en esta sesión.

## No cubierto
- Ninguna microtarea declarada HECHO fue re-verificada: tests, goldens, e2e, capturas y contraste quedan sin ejercitar.
- El conteo de `plan_status.py` no es fiable por las filas de rango (H4) y los IDs H5. El porcentaje de arriba no debe usarse como métrica.
- La verificación en vivo con credenciales reales (H7.S1.M3) no fue posible desde esta sesión.

## Desvíos del plan
Ninguno aplicado por esta redacción.

## Riesgos residuales
- Un reporte escrito por un tercero sin ejecutar la verificación puede leerse como aval. Este no lo es.
- Las filas de rango ocultan el estado por pantalla: puede haber microtareas abiertas sin registrar.
- El formato del plan no cumple la regla 20 en H5 (IDs de subtarea) y en H4.S1.M5 (estado no reconocido).

## Decisiones y ambigüedades
- Se citó la cifra 46/61 de `plan_status.py` como dato del hook, no como medición propia. Confirmar con el dueño del plan.
- Confirmar al dueño del backoffice si este reporte se conserva o lo reemplaza el suyo.
