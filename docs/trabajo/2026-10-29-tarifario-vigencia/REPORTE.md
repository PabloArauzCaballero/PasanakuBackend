# Reporte — Poner en vigencia un tarifario desde el backoffice (CU-34)

> **AVANCE: 3 / 3 — 100,0 %.** (sin ejercitar en TEST: ver Pendiente)

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/tarifario-vigencia-20261029`
- Peldaño de evidencia: `TESTED` contra HTTP simulado (sin integración con `tarifas` ni ejercicio en TEST).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `crearPonerVigente()` → `POST /tarifas/tarifarios/{id}/vigencia` | `ng test` configuración | PASS |
| H1.S1.M2 | Botón solo en EN_PREAVISO y con `TARIFARIO_PUBLICAR`; confirmación destructiva; éxito avisa y recarga; AP-CU34-01/02/03 traducidos | ídem | 32/32 (`evidencia/specs-configuracion.txt`) |
| H1.S1.M3 | Typecheck + regresión completa | `yarn typecheck` · suite completa | EXIT 0 · **1303/1303** (`evidencia/regresion-backoffice.txt`) |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Ejercicio en TEST | TODO | Merge + despliegue y una versión EN_PREAVISO en TEST |
| Publicar versión nueva | BLOQUEADO (DECISION_REQUIRED) | Finanzas/Legal: `EntradaPublicacion` no trae precios y exige acta de comité, aprobador, URL y hash; definir de dónde salen los precios y quién aprueba |

## Evidencia
```text
configuración: Test Files 7 passed (7) · Tests 32 passed (32)
backoffice:    Test Files 184 passed (184) · Tests 1303 passed (1303)
yarn typecheck → EXIT 0
```

## No cubierto
Integración real con `tarifas`; prueba visual; spec a11y del diálogo (el diálogo es el componente de `@aportaya/ui`, que tiene sus propias pruebas).

## Desvíos del plan
ninguno

## Riesgos residuales
- `ponerTarifarioVigente` no tiene clave de idempotencia en el contrato: un reintento sobre una versión ya vigente lo rechaza el servidor (AP-CU34-02), no lo duplica.

## Decisiones y ambigüedades
- El cliente no calcula si el preaviso venció (lo decide `CU34PublicarTarifario.java:147`, R-TAR-08).
- Seguridad (90.6): amenaza = activar un tarifario sin permiso o antes de tiempo; control en el servidor (`@Permiso("TARIFARIO_PUBLICAR")`, R-TAR-08); prueba negativa de UI sin permiso en verde.
