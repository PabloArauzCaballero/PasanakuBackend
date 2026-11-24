# Plan — Publicar en el contrato la alerta de origen y el titular del caso (CU-44) y quién propuso/aprobó cada versión de parámetro (CU-132)

- Fecha: 2026-11-24 · Repos afectados: PasanakuBackend (`servicios/tarifas`, `servicios/cumplimiento`, `apps/backoffice`, `packages/simulado`, `postman`) · Predecesor: `2026-11-24-rediseno-caso-y-parametros`
- Pedido: Pablo pidió explícitamente agregar al contrato lo que las pantallas declaraban faltante. Cambios ADITIVOS.
- Resultado observable: el oficial que abre `/cumplimiento/casos/:id` ve en el resumen la alerta de origen (regla, tipología, severidad) y el titular enmascarado; en parámetros financieros, cada versión dice quién la propuso y quién la aprobó.
- Kill-test: abrir la captura del resumen del caso; si sigue diciendo «El contrato del caso todavía no los publica», no está hecho.

## Hallazgos (fase 1)
- `VersionDelParametro` (tarifas/dominio) ya guarda `propuestoPor: UUID` y `aprobadoPor: Optional<UUID>`; `MapeoDeParametros.version` no los publica.
- Patrón del repo para actores de backoffice: UUID (`aprobadoPor`, `registradoPor: [string,'null'] uuid`, `analistaId`), y el backoffice los pinta en monoespaciada (`cajon-de-revision.ts` `mono: true`). No hay nombre visible de operador en ningún contrato.
- CU-44: NO existe ruta que alimente la pantalla del caso (`pantalla-de-caso.ts` usa un caso de demostración). La bandeja `GET /cumplimiento/casos` declara deber de reserva («no viaja la persona») y la descripción de alertas dice que la persona «se resuelve en la ficha».
- Los datos están en la base propia de cumplimiento: `caso_investigacion_lft.usuario_id` y `alerta_monitoreo_lft.caso_id` + `regla_monitoreo_lft` (codigo, tipologia). No hace falta migración ni leer otra base.
- Cumplimiento no guarda nombre ni documento (viven en identidad). Patrón de enmascarado existente: `CuentaEnmascarada` (entregas) — asteriscos + últimos 4.

## Alcance
- IN: `tarifas.yaml` (`VersionDeParametro` + `propuestoPor`, `aprobadoPor`), `MapeoDeParametros`, `ParametrosWebTest`; `cumplimiento.yaml` (ruta nueva `GET /cumplimiento/casos/{casoId}` `obtenerCasoLft` → `FichaDeCasoLft`), `BandejasLftRepositorio`, `ConsultarBandejasLft`, `MapeoDeLft`, `CumplimientoController`, `LftBandejaWebTest`, `CU44BandejaTest`; backoffice `rutas/cumplimiento/{casos,dominio,textos}`, `rutas/configuracion/{parametros,textos}`, specs y los dos E2E; ejemplos del simulado de las operaciones tocadas; Postman.
- OUT: móvil, otras pantallas, esquema SQL, autorización (mismo permiso `CUMPLIMIENTO_CASOS`), la bandeja de casos (sigue sin persona).
- Ambigüedades:
  - A1: no hay nombre de operador en ningún contrato → se publica el UUID (patrón vigente). Confirmar con Pablo si quiere nombre visible (requeriría consultar identidad).
  - A2: cumplimiento no tiene nombre/documento del titular → se publica `titularEnmascarado` = `****` + últimos 4 del seudónimo `usuario_id`. Confirmar con Pablo/cumplimiento si basta.
  - A3: la ficha publica la alerta MÁS ANTIGUA enlazada como «de origen» (y el total de enlazadas). Supuesto; confirmar.

## H1 — Contratos y backend
**CA:** Dado un operador con permiso, cuando consulta las versiones de un parámetro o la ficha de un caso, entonces recibe quién propuso/aprobó y la alerta de origen + titular enmascarado, sin romper a los clientes existentes.
**DoD:** `openapi-generator validate` sin errores · verificadores de contrato · tests Java escritos (verificación Java en CI: Gradle local bloqueado por 429).
**Estado:** A MEDIAS — contrato y verificadores en verde; Java sin compilar ni testear localmente (BLOQUEADO por entorno: Gradle 429; lo verifica CI)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `VersionDeParametro` + `propuestoPor`/`aprobadoPor` (yaml + mapeo + test web) | campos opcionales; aprobador null si pendiente | `validate -i tarifas.yaml` sin errores | HECHO (contrato; Java en CI) |
| H1.S2.M1 | `GET /cumplimiento/casos/{casoId}` → `FichaDeCasoLft` (yaml) | ruta nueva, permiso `CUMPLIMIENTO_CASOS`, 404/422 caso inexistente | `validate -i cumplimiento.yaml` | HECHO |
| H1.S2.M2 | Repositorio + caso de uso + controlador + tests (web y BD) | titular nunca en claro; alerta null si no hay | palantir-java-format 2.50.0 `--dry-run` limpio; compilación y tests Java en CI | A MEDIAS (escrito y formateado; Gradle local 429) |
| H1.S3.M1 | Postman, ejemplos del simulado, verificadores | sin diff tras regenerar | `generar_postman.py`, `verificar_pruebas_web.py`, `verificar_contratos_limites.py`, `inventario_endpoints.py --check`, vitest simulado | HECHO |

## H2 — Backoffice muestra los datos reales
**CA:** Dado el oficial en el caso, cuando la ficha llega, entonces el resumen muestra la alerta y el titular enmascarado (y un estado de error/sin alerta legible); en parámetros, el historial y las tarjetas dicen quién propuso y aprobó.
**DoD:** clientes regenerados, `yarn typecheck`, `yarn lint`, specs tocadas, E2E caso + parámetros, capturas revisadas en `2026-11-23-continuar-traspaso/evidencia/contrato/`.
**Estado:** HECHO (contra el simulado; sin backend real)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Regenerar clientes Angular tarifas/cumplimiento | modelos con campos nuevos | `ls clientes/angular/cumplimiento/model/fichaDeCasoLft.ts` | HECHO |
| H2.S2.M1 | Resumen del caso con alerta y titular | sin texto «todavía no los publica» | spec caso + E2E caso | HECHO |
| H2.S3.M1 | Parámetros: propuso/aprobó | sin «todavía no publica quién» | spec parámetros + E2E parámetros | HECHO |
| H2.S4.M1 | Gates y capturas | typecheck/lint verdes; capturas miradas | `yarn typecheck && yarn lint` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Gradle local no resuelve dependencias (429) | Java sin compilar acá | copiar patrones vecinos; declarar en el reporte; CI verifica |
| Límite de líneas por archivo del backoffice | gate rojo | componentes chicos |
