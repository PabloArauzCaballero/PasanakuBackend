> **AVANCE: 13 / 13 — 100 %.**

# Reporte — A6 · listados del ERP para el backoffice (carril A, subagente)

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md) · Rama: `codex/backoffice-a6-erp` (commit `b4ca1dc6`, desde `70747767`), integrada en la rama del carril con el merge `b5831572`.
- Peldaño: backend `erp` en **TESTED** (integración contra PostgreSQL real + web + regresión del módulo en verde), **re-verificado por el agente principal** sobre el árbol integrado (`../evidencia/A6-02-integracion-erp.txt`: test 5/0, webTest 52/0, integrationTest 117/0, testBarrido 2/0). No es VERIFIED: la UI todavía no consume estos endpoints y no hubo ejercicio por el gateway.
- Nota: el subagente no pudo escribir este archivo (el arnés lo bloquea); el agente principal lo transcribió de su entrega.

Endpoints (todos con `estados`, `limite` 1..100 default 25, cursor `antesDe`+`antesDeId`, orden `(columna, id)` DESC, importes `{monto, moneda}`; permisos `CONTABILIDAD_ERP_*` sembrados):

| Método y ruta | Permiso | Alternativos | 422 |
|---|---|---|---|
| `GET /erp/partidas-presupuestarias` (opcional `ejercicioId`) | PRESUPUESTO | REPORTES | AP-CU101-04 |
| `GET /erp/ordenes-de-compra` | COMPRAS | CUENTAS_POR_PAGAR, REPORTES | AP-CU102-04 |
| `GET /erp/facturas-de-proveedor` | CUENTAS_POR_PAGAR | PAGAR, REPORTES | AP-CU103-06 |
| `GET /erp/cuentas-por-cobrar` | COBRAR | REPORTES | AP-CU104-04 |
| `GET /erp/activos` | ACTIVOS_FIJOS | REPORTES | AP-CU105-04 |

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | 5 GET + esquemas + AP-CU101-04/102-04/103-06/104-04/105-04 en erp.yaml | `./gradlew :servicios:erp:compileJava --no-build-cache` | PASS exit 0 |
| H1.S2.M1..M5 | Partidas, órdenes, facturas, cuentas por cobrar y activos | `integrationTest --tests '*ListadoTest'` | PASS 17/17 — evidencia/01 |
| H1.S2.M6 | Pagado/cobrado, saldo y estado derivados de pagos/cobros | idem CU103/CU104 | PASS — evidencia/01 |
| H1.S3.M1 | ListadosDeErpWebTest 200/403 AP-SEG-01/401/400/422 | `webTest` | PASS 7/7 — evidencia/02 |
| H1.S3.M2 | ArranqueTest 19→24 + @MockitoBean en 5 webTest | regresión | PASS — evidencia/04 |
| H1.S3.M3 | Fichas CU-101..105 (códigos, permisos, escenarios) | `python3 scripts/verificar_criterios.py` | PASS — evidencia/03 |
| H1.S3.M4 | Postman de erp | `python3 scripts/generar_postman.py` | PASS — evidencia/05 |
| H1.S3.M5 | Regresión + gates | ver Evidencia | PASS — evidencia/03, 04 |
| H1.S3.M6 | Clase de rutas renombrada a `ListadosDeErpController` | generar_postman | PASS — evidencia/05 |

## A medias

ninguna

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| Listado de asientos | fuera de alcance | Dueño de `nucleo-financiero` (`asiento_contable` es de su esquema, regla 98.1.1) |
| UI paginada | fuera de alcance | Carril B adapta `contrato-erp.ts`: `cargadorDeLista` espera `T[]` y el contrato devuelve página con cursor; campos renombrados (`cuentaContableNombre`→`cuentaContableId`, `periodoNombre`→`anio`/`mes`, `corridoEnElPeriodoVigente`→`depreciadoEnElPeriodoVigente`, `aprobadaPor`→`aprobadaPorLaSesion`, `descripcion` de la orden no viaja) |

## Evidencia

`evidencia/01` (17 pruebas de integración), `02` (webTest), `03` (gates), `04` (regresión: test 5/0, webTest 52/0, integrationTest 117/0, testBarrido 2/0, contractTest NO-SOURCE; BUILD SUCCESSFUL), `05` (postman: solo erp, 19→24 operaciones). Re-verificación del agente principal: `../evidencia/A6-02-integracion-erp.txt`.

## No cubierto

Gateway/UI; `contractTest` sin fuentes; índices del keyset y plan de consulta; `montoEjecutado` solo probado en `0.00`; RLS con otro rol de base.

## Desvíos del plan

1. `RutasDeListadosDeErp` → `ListadosDeErpController` (`generar_postman.py` solo lee `*Controller.java`; `scripts/` fuera de alcance). Las rutas viven en una clase abstracta que `ErpController` extiende (patrón `RutasDeHabilitacion`).
2. Los tests "límite" pasaron a escenarios Gherkin (`verificar_criterios` exige escenario en `CU*Test`).
3. El repositorio se partió en dos (`ListadosDeErpRepositorio`, `ListadosDeTercerosRepositorio`) por el tope de líneas.
4. Se agregó H1.S2.M6: `monto_pagado`, `monto_cobrado` y `estado` quedan congelados por append-only; los listados los derivan de `pago_a_proveedor` y `cobro_cuenta_por_cobrar` con la regla de CU-103/CU-104. `saldo_pendiente` y `valor_en_libros` figuran como GENERATED en el comentario pero son columnas planas en el DDL; el valor en libros se calcula con `Depreciacion.valorEnLibros`.

## Riesgos residuales

Divergencia DDL/comentario GENERATED en `factura_proveedor`, `cuenta_por_cobrar` y `activo_fijo` (dueño de `sql/`). Keyset sin índice dedicado. `ErpController` en 298 líneas.

## Decisiones y ambigüedades

1. Permisos de lectura por listado: Tesorería lee facturas con PAGAR; quien registra facturas lee órdenes con CUENTAS_POR_PAGAR. Confirmar con Contabilidad.
2. Respuesta mínima y campos renombrados: `descripcion` de la orden no viaja; la del activo sí. Confirmar con carril B.
3. Orden DESC por instrucción del carril; la cola de pagos por vencimiento ascendente sería un parámetro de orden nuevo = DECISION_REQUIRED.
4. ANULADA e INCOBRABLE se conservan; el resto del estado se deriva con la regla de CU-103/CU-104.
5. Datos de prueba sintéticos (años 2550–2575).

Procesos en background al cerrar: ninguno.
