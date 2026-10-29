# Reporte — Ejecutar reportes (CU-58): corrección de la semilla (primera entrega)

> **AVANCE: 1 / 5 — 20,0 %.** La corrección de datos está hecha y probada; la pantalla de ejecución queda para la entrega siguiente.

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/ejecutar-reportes-20261029`
- Peldaño: semilla `TESTED` (Postgres real descartable + integración) · en TEST se aplica al desplegar (`despliegue/aplicar-esquema.sh:109`).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H0 (agregada) | `parametros_esperados` de las 10 definiciones sembradas en la forma que lee el motor (`[{nombre,tipo,obligatorio}]`, tipos del validador) + `generar_semillas.py` con `"actualizar"` declarado (`ON CONFLICT … DO UPDATE … WHERE … IS DISTINCT FROM`) | Postgres 16 descartable: semilla vieja → nueva ×2; `CU58SemillasTest` | ver Evidencia |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S1.M1 | TODO | Exponer `parametrosEsperados` en `DefinicionDeReporteEnLista` |
| H1.S2.M1–M2 | TODO | Formulario de ejecución en el backoffice |
| H1.S3.M1 | TODO | Regresión del frontend (cuando haya frontend) |
| Verificación en TEST | TODO | Merge + despliegue: `jsonb_typeof = array` en 10/10 y la consulta del motor sin error |

## Evidencia
```text
== 1) semilla VIEJA (forma de TEST hoy)        object|10 · motor: ERROR: cannot call jsonb_to_recordset on a non-array
== 2) semilla NUEVA, primera corrida            INSERT 0 10 → array|10
== 3) semilla NUEVA, segunda corrida            INSERT 0 0 (idempotente)
== 4) motor                                     CARTERA_EN_MORA|grupo_id|UUID|f · CONCILIACION_DIARIA|fecha|FECHA|t
== 5) conteo                                    10 (sin duplicados)
CU58SemillasTest: semilla vieja → 2/3 fallan («non-array») · semilla nueva → 3/3
auditoria: webTest 82/82 · integrationTest 76/76 · spotlessCheck EXIT 0
```
Archivos: `evidencia/semilla-que-actualiza.txt`, `evidencia/guardia-con-semilla-vieja.txt`.

## No cubierto
- La ejecución de cada `consulta_base` contra datos reales: esta entrega solo prueba que el motor **lee** los parámetros.
- `testBarrido` de `auditoria` sigue en rojo por motivos previos y ajenos (`SaludDeServicios`, `IndicadoresController` 379 líneas).

## Desvíos del plan
- Se encontró que ningún reporte sembrado se podía ejecutar (clase DATA, reproducido en TEST, solo lectura). Pablo eligió
  «semilla que actualiza» (2026-10-29). La pantalla se posterga.
- La primera conversión del JSON lo reformateaba entero (810 líneas): se revirtió y se aplicó un cambio puntual (23 líneas).
- Trampa medida: sin `--rerun`, Gradle reutiliza el resultado de una prueba que lee un SQL cambiado.

## Riesgos residuales
- `"actualizar"` reescribe en cada entorno las columnas declaradas con lo que diga la semilla: es la intención (la semilla manda
  sobre el catálogo), pero un ajuste manual en un entorno se perdería (y eso ya está prohibido por la regla 97.1.5).

## Decisiones y ambigüedades
- Mapeo de tipos: `uuid→UUID`, `date→FECHA`, `string→TEXTO`, `uuid?→UUID` no obligatorio (vocabulario de `ParametrosDeReporte.java:54`).
