# Reporte — Ejecutar reportes (CU-58): pantalla (segunda entrega)

> **AVANCE: 4 / 4 de esta entrega — 100,0 %.** Depende de #287 (semilla) para funcionar en TEST.

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Primera entrega: [REPORTE.md](./REPORTE.md) (#287/#288) · Rama: `claude/ejecutar-reportes-ui-20261029`
- Peldaño: backend `TESTED` (web + integración) · frontend `TESTED` contra HTTP simulado · sin ejercitar en TEST.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `parametrosEsperados` (opcional) en el catálogo, leído con `jsonb_to_recordset` como el motor; una fila con forma vieja se lista sin parámetros en vez de tumbar el listado | `./gradlew :servicios:auditoria:webTest :servicios:auditoria:integrationTest --rerun` | web 82/82 · integración 75/75 (2 casos nuevos en `CU58HistorialTest`) |
| H1.S2.M1 | `crearEjecutarReporte()` con clave por contenido | spec | PASS |
| H1.S2.M2 | Formulario: un campo por parámetro (FECHA → fecha), opcional/obligatorio, justificación 10..300 si es sensible, formato opcional, resultado con filas (cero es respuesta) y huella; «Ejecutar» solo con el `permisoRequerido` de la sesión, si no «Requiere X» | `ng test` configuración | 5/5 nuevos; configuración 37/37 |
| H1.S3.M1 | Lint + typecheck + regresión completa | `yarn lint` · `yarn typecheck` · suite completa | EXIT 0 · EXIT 0 · **1320/1320** |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Verificación en TEST | TODO | Merge de #287 (semilla) y de este PR + despliegue: ejecutar «Conciliación diaria de custodia» con una fecha |

## Evidencia
```text
auditoria: webTest 82/82 · integrationTest 75/75 · spotlessCheck EXIT 0
backoffice: yarn lint EXIT 0 · typecheck EXIT 0 · Test Files 186 passed (186) · Tests 1320 passed (1320)
```

## No cubierto
Descarga del archivo exportado (`descargarExportacion` solo autoriza y cuenta); prueba visual; ejecución real de cada `consulta_base`.

## Desvíos del plan
ninguno

## Riesgos residuales
- Con `AUDITORIA_LEER`, el admin ejecuta 5 de los 10 reportes; los demás piden BILLETERA_VER, GRUPO_ADMINISTRAR o CUMPLIMIENTO_REPORTAR (segregación decidida por Pablo). La pantalla lo dice por fila.

## Decisiones y ambigüedades
- Seguridad (90.6): amenaza = sacar datos sin permiso o sin huella; control en el servidor (`AUDITORIA_LEER` + `permisoRequerido` de la definición + justificación + huella); la UI no ofrece lo que el permiso no alcanza.
