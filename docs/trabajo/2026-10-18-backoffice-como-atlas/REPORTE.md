# Reporte — Backoffice como Atlas

> **AVANCE: 46 / 61 — 75,4 %.** (cifra del hook `report_gate.py`: microtareas HECHO / total; no verificada por esta redacción)

- Fecha: 2026-10-21 · Plan: [PLAN.md](./PLAN.md) · Rama: `dev` (commit base `7752e1b`)
- Peldaño de evidencia alcanzado: **WRITTEN** para todo lo de este reporte. Ninguna afirmación de HECHO se re-ejecutó en esta sesión.
- Autor de este reporte: sesión de Claude sobre `claude/rediseno-bancario`. No es el responsable del backoffice; el reporte resume el `PLAN.md` y no reemplaza la verificación de su dueño.

## COMPLETADO
Ninguna microtarea entra acá. No hay salida literal de DoD ejecutada en esta sesión para ninguna de ellas.

## A MEDIAS
Las filas que el hook cuenta como EN CURSO y que el `PLAN.md` declara HECHO en una fila de rango. Discrepancia de conteo, sin verificar:

### H2.S1.M4 — Campana de avisos internos
- Qué anda: el resto de H2 (shell, menú, buscador, migas, explicador) según el plan.
- Qué no anda: la campana no existe; el plan la difiere hasta H5.S0 (backend real).
- Qué falta exactamente: `H5.S0` (`aviso_interno` en `notificaciones`) y luego la campana con e2e.
- Dónde quedó: `PLAN.md` estado TODO.

### H4.S2.M1–M10, H4.S3.M1–M14, H4.S5.M1–M10, H4.S6.M1–M6, H4.S7.M1–M6, H4.S8.M1–M4
- Qué anda (declarado): migración a `ap-cabecera-de-pagina` y `ExplicadorDeVista` por pantalla, con specs según el plan.
- Qué no anda: el hook las cuenta como EN CURSO; el `PLAN.md` las marca HECHO por rango. No sé cuál es el estado real por pantalla.
- Qué falta exactamente: contar pantalla por pantalla (10 + 14 + 10 + 6 + 6 + 4 filas) y ejecutar sus specs.
- Dónde quedó: `PLAN.md` secciones H4.S2–H4.S8.

## PENDIENTE
| ID | Estado | Qué lo destraba |
|---|---|---|
| H5.S0 … H5.S14 | TODO | Contratos de backend por módulo (ver tabla de H5 en `PLAN.md`); requiere dueños de cada servicio |
| H7.S1.M1 | TODO | Regresión completa y gates |
| H7.S1.M2 | TODO | Capturas 41 × 3 × 2 |
| H7.S1.M3 | TODO | PRs #test y #dev mergeados; TEST reconstruido; verificación en vivo con el admin |

## Evidencia
Índice de `evidencia/` tal como existe hoy (no re-verificado):
- `H0-contraste-pii.txt`, `H3-S1-auditoria.txt`, `atlas-inventario.md`, `atlas-roles-y-permisos.md`, `sistema-visual.md`
- `H1/`, `H2/`
No se corrió ningún comando en esta sesión.

## No cubierto
- Ninguna microtarea declarada HECHO fue re-verificada aquí: tests, goldens, e2e, capturas y contraste quedan sin ejercitar.
- La discrepancia de conteo entre el hook y el `PLAN.md` por filas de rango (H4.*) no está resuelta.
- La verificación en vivo con credenciales reales (H7.S1.M3) no fue posible.

## Desvíos del plan
Ninguno aplicado por esta redacción. Este reporte se escribió a pedido de quien opera la sesión, a partir del plan existente.

## Riesgos residuales
- Un reporte escrito por un tercero que no ejecutó la verificación puede leerse como aval. Este no lo es.
- Las filas de rango ocultan estado por pantalla; puede haber microtareas abiertas sin registrar.

## Decisiones y ambigüedades
- Se usó la cifra 46/61 del hook sin recalcularla. Confirmar con el dueño del plan.
- Confirmar al dueño del backoffice si este reporte se conserva o lo reemplaza el suyo.
