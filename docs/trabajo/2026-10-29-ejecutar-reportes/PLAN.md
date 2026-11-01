# Plan — Ejecutar un reporte desde el backoffice (CU-58)

- Fecha: 2026-10-29 · Repos: monorepo (servicios/auditoria, apps/backoffice) · Predecesor: backlog ítem 5
- Rama: `claude/ejecutar-reportes-20261029` (desde `origin/test`)
- Resultado observable: en Configuración › Reportes, el operador elige un reporte, completa sus parámetros (uno por cada
  `parametros_esperados`), la justificación si el reporte tiene datos sensibles y, si quiere, el formato; lo ejecuta y ve
  estado, filas y huella (`hashResultado`); la ejecución aparece en el historial.
- Kill-test: un reporte del catálogo sin botón «Ejecutar», o un parámetro que la pantalla inventa o no pide → no está hecho.

## Hallazgos
- `ejecutarReporte` (`auditoria.yaml:321`, `ReportesController.java:90`, `@Permiso("AUDITORIA_LEER")`, Idempotency-Key): cuerpo
  `{parametros: {string: string}, formato?, justificacion? (10..300, obligatoria con datos sensibles)}` → `SalidaReporte`.
- La pantalla (`configuracion/reportes/pantalla-de-reportes.ts:24`) excluye ejecutar a propósito: **el catálogo no expone
  `parametros_esperados`** (`DefinicionDeReporteEnLista` no lo tiene), así que ninguna UI podía saber qué pedir.
- Base: `auditoria.definicion_reporte.parametros_esperados JSONB NOT NULL`; TEST: 10/10 definiciones con mapa nombre→tipo
  (`date`, `uuid`, `uuid?` = opcional), p. ej. `CONCILIACION_DIARIA {"fecha":"date"}`.
- `descargarExportacion` solo autoriza y cuenta (204, no devuelve el archivo): **fuera de alcance**; se muestra el `exportacionId`.

## Hallazgo bloqueante (2026-10-29, reproducido)
- El motor lee `parametros_esperados` como **arreglo** `[{nombre, tipo, obligatorio}]` con tipos `UUID|FECHA|ENTERO|DECIMAL|BOOLEANO|TEXTO`
  (`infraestructura/ReporteRepositorio.java:85`, `dominio/ParametrosDeReporte.java:54`; tests: `FixturaDeReportes`, arreglo).
- La semilla oficial (`seeders/minimos/19-reportes-y-retencion.json` → `sql/60_semillas/19-reportes-y-retencion.sql`) los guarda como
  **objeto** `{"usuario_id":"uuid","desde":"date"}` con tipos en minúscula (`uuid`, `date`, `uuid?`).
- TEST, solo lectura: `jsonb_typeof = object` en 10/10 y la consulta del motor da
  `ERROR: cannot call jsonb_to_recordset on a non-array` → **ningún reporte sembrado se puede ejecutar** (clase DATA).
- Corregir el JSON fuente arregla entornos nuevos, pero la semilla es `ON CONFLICT (nombre) DO NOTHING` y no hay mecanismo de
  migración de datos en `sql/`: las 10 filas de TEST no se actualizarían. Corregir la base a mano está prohibido (regla 97.1.5).
- **DECISION_REQUIRED** (Pablo): cómo llevar la corrección a TEST. Recomendado: que `generar_semillas.py` acepte un bloque
  declarado `"actualizar": ["parametros_esperados"]` → `ON CONFLICT (nombre) DO UPDATE SET parametros_esperados = EXCLUDED…`
  (idempotente, segunda corrida no cambia nada), y corregir el JSON a la forma del motor.

## Alcance
- IN: campo aditivo `parametrosEsperados` (mapa nombre→tipo, tal cual la base) en `DefinicionDeReporteEnLista`; botón
  «Ejecutar» por definición; formulario por tipo; resultado; recarga del historial.
- OUT: descarga del archivo; crear o editar definiciones; tipos no vistos en la base (se muestran como texto libre y el
  servidor valida).

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Contrato + mapeo de `parametrosEsperados` | el listado lo trae tal como la base | `webTest` auditoria | TODO |
| H1.S2.M1 | Dominio `crearEjecutarReporte()` con clave | cuerpo exacto del contrato | spec | TODO |
| H1.S2.M2 | Formulario por tipo + justificación + formato + resultado | correcto / límite (opcional `uuid?`, cero filas) / inválido (422 traducido, justificación < 10) | spec | TODO |
| H1.S3.M1 | Lint + typecheck + regresión completa + spotless/barrido/web de auditoria | sin rojos nuevos | suites | TODO |
