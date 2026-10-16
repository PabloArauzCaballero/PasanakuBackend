> **SUPERADO por H17 (2026-10-12).** Lo que este archivo daba por «escrito sin verificar» se compiló, se formateó y se probó después: ver «Retomada 2026-10-12 (H17)» en [REPORTE.md](../../docs/trabajo/2026-10-08-endurecimiento-integral/REPORTE.md) y `docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-H17/`. Se conserva el texto original de abajo como estado del corte del 2026-10-11.

# Carril R · ESTADO (2026-10-11, cortado por el coordinador)

Peldaño: WRITTEN para todo el código Java/TS (NADA compiló ni corrió: la RAM libre quedó en ~0,6-1,3 GB, el guardián exige 3500 y nunca se liberó; dos esperas de 3000 s). Cancelé mi corrida pendiente.

## Verificado (solo Python, sin Gradle)
- `python scripts/generar_ddl.py`: OK (337 tablas, 173 restricciones). `verificar_criterios.py --servicio erp`: sin divergencias (7 CU). `verificar_boveda.py`: TODO OK. `verificar_rutas_frontend.py`: TODO OK, 19 sin contrato (sin cambio).

## Escrito SIN verificar
1. Idempotencia del pago (R-CTB-11, 91.1.4): puml `clave_idempotencia UUID <<NULL>>` en pago_a_proveedor; Restricciones.md R-CTB-11 (UNIQUE (factura_proveedor_id, clave_idempotencia), nullable para pagos legados); sql/ regenerado (pago_a_proveedor.sql, nulabilidad.sql, restricciones.sql); `CU103FacturaDeProveedor.pagar` (busca por clave bajo el FOR UPDATE; mismo pago => misma respuesta sin fila ni evento; otro pago con la clave => AP-CU103-05) + `ReintentoDePago.java` nuevo + `ComprasRepositorio.pagoPorClave/pagadoHasta/pagar`; `EntradaPago` ahora lleva `UUID claveIdempotencia` (obligatoria); `ErpController`/`MapeoDeErp` la pasan desde la cabecera.
2. aprobadaPor: `EntradaFactura.aprobadaPor` -> `boolean aprobar`; aprobador = `ctx.usuarioId()`. erp.yaml: sin `aprobadaPor` (400 por additionalProperties:false), campo `aprobar` (default false).
3. QR: enum `formaPago` del pago sin QR (alineado con ck_pago_a_proveedor_forma_pago y puml línea ~214); backoffice sin opción QR (`ficha-de-factura.ts`, `cu103-facturas.ts`).
4. Rutas sin contrato: `rutas-erp-publicidad-abierto.md` (ABIERTO con dueño; no existe ningún listado, no se inventó nada).
- Pruebas nuevas: `CU103IdempotenciaTest.java` (criterio4 + reintento x3 + concurrencia 4 hilos + rechaza R-CTB-11 x2), `web/RegistroDeFacturaWebTest.java` (6), 5 casos nuevos en `web/PagoDeFacturaWebTest.java`, `entrada-estricta.spec.ts` (+2), `ficha-de-factura.spec.ts` (+1). Tests existentes de CU103/CU101 adaptados a los nuevos constructores (concurrencia/eventoDuplicado ahora usan claves distintas).
- Docs: CU-103 (flujo, error AP-CU103-05, R-CTB-11, 4.º criterio gherkin), erp.yaml (comentario de errores), docs/Modelos (pago_a_proveedor, _Entidades), cifra 172->173 restricciones en planes/00, 05, 06 y docs/Seguridad.md.
- `packages/simulado/generado` y `clientes/` NO se regeneraron (generarClientes/yarn build no corrieron).

## Falta (en orden)
a) `servicios/erp/build/generated/jooq` ya se borró; regenerar. b) spotlessApply (CU103 quedó en 287 líneas; el barrido exige <300). c) Resolver errores de compilación que aparezcan (nunca compiló). d) tests erp (test/webTest/integrationTest/testBarrido) y publicidad (sin cambios, solo regresión). e) `yarn workspace @aportaya/simulado build` y tsc/lint/vitest de simulado y backoffice. f) Prueba de esquema aditivo sobre base poblada: `scratchpad/r_upgrade.sh` (Postgres 5455, aplica sql de HEAD, siembra pago legado, corre despliegue/aplicar-esquema.sh con el sql nuevo, 2 corridas, prueba del UNIQUE). No ejecutado. g) Conteos desde XML.

## Comandos
`MAX_WAIT_S=3000 bash C:/Users/Usuario/.claude/ram-guard/heavy.sh bash <scratchpad>/r_jj.sh :servicios:erp:spotlessApply :servicios:erp:generarClientes :servicios:erp:test :servicios:erp:webTest :servicios:erp:integrationTest :servicios:erp:testBarrido --continue` (r_jj.sh: Postgres 5455, JDK 21.0.12, logs a esta carpeta). Luego lo mismo con `:servicios:publicidad:...`.

## Supuestos / decisiones a confirmar
- `aprobar` (boolean) conserva REGISTRADA vs APROBADA sin dejar que el cliente nombre al aprobador. Alternativa: aprobar siempre.
- Código nuevo AP-CU103-05 (la bóveda cerraba 4); declarado en CU-103 y erp.yaml.
- Clave amparada en la factura, nullable en la base (TEST poblado); la obligatoriedad la impone la cabecera. El pago no genera asiento propio: lo pide el evento `erp.pago_a_proveedor_registrado` (un solo evento por pago).
- El reintento reconstruye lo pagado "hasta ese pago" por `fecha_pago` (empate exacto de microsegundos no distinguible).
- `registrarFactura` sigue sin replay idempotente (duplicado => 422 por uq_factura_proveedor_numero).
