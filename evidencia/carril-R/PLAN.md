# Plan carril R — cierre de lo abierto en erp/publicidad (2026-10-11)

- Repos: PasanakuBackend-cierre (rama justin/feature/cierre-endurecimiento-2026-10-08). Predecesor: carril K.
- Resultado observable: un pago parcial a proveedor repetido con la misma Idempotency-Key no duplica nada; el aprobador de una factura es el del token; el contrato no admite QR en el pago a proveedor.
- Kill-test: pagar dos veces con la misma clave y contar filas en erp.pago_a_proveedor (debe ser 1).
- IN: servicios/erp, servicios/publicidad (sin cambios previstos), docs/entidades/13_contabilidad_erp.puml, docs/Restricciones.md, sql/ (generado), apps/backoffice (erp), packages/simulado (erp), docs.
- OUT: grupos/identidad (P), nucleo/aportes (Q), apps/movil (T), origin/test, origin/dev, VPS.

## R1 — Idempotencia del pago a proveedor (91.1.4)
| ID | Microtarea | CA | DoD |
|---|---|---|---|
| R1.M1 | Columna clave_idempotencia (NULL) + R-CTB-11 UNIQUE (factura, clave) en puml/Restricciones/generar_ddl | indice existe en la base | psql \d erp.pago_a_proveedor |
| R1.M2 | CU-103 usa la clave (misma clave+mismo cuerpo = mismo resultado; mismo clave distinto cuerpo = conflicto; carrera = un solo pago) | tests correcto/reintento/concurrencia/rechazo R-CTB-11 | gradle :servicios:erp:test |
| R1.M3 | Esquema aditivo verificado sobre la base anterior | aplicar-esquema.sh agrega la columna | log en evidencia |
## R2 — aprobadaPor desde el token
| R2.M1 | Contrato sin aprobadaPor (+ aprobar:boolean), CU deriva de ctx.usuarioId, regenerar clientes/simulado/backoffice | 400 con aprobadaPor; matriz negativa | webTest, vitest entrada-estricta |
## R3 — QR fuera del pago a proveedor
| R3.M1 | enum formaPago sin QR; backoffice sin opcion QR en pago de factura | 400 con QR | webTest, vitest |
## R4 — Rutas sin contrato
| R4.M1 | Inventario y decisiones ABIERTO con dueno | doc actualizado | verificar_rutas_frontend.py |
