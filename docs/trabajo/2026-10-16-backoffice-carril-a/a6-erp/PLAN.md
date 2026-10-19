# Plan — A6 · listados del ERP para el backoffice

- Fecha: 2026-10-16 · Repos afectados: este monorepo, solo `servicios/erp/**`, fichas `docs/CasosDeUso/CU-101..105`, `postman/**/erp*` · Rama: `codex/backoffice-a6-erp` (desde `origin/codex/backoffice-dominio-20261015`, `70747767`)
- Predecesor: carril A H4/H7 (listados de ejercicios y períodos en `ErpController`).
- Resultado observable: la sección Contabilidad del backoffice puede pedir al gateway, paginado por cursor y filtrado por estado, las partidas presupuestarias, las órdenes de compra, las facturas de proveedor (cuentas por pagar), las cuentas por cobrar y el inventario de activos fijos, con permisos `CONTABILIDAD_ERP_*` sembrados.
- Kill-test: `GET /erp/ordenes-de-compra` con sesión `CONTABILIDAD_ERP_COMPRAS` devuelve 404/405 (hoy no existe).

## Descubrimiento (hechos)

- `erp.yaml` tiene 19 rutas; las únicas lecturas son ejercicios, período vigente, períodos del ejercicio y documento de estado financiero (`servicios/erp/src/main/resources/openapi/erp.yaml:83-469`).
- La UI llama a listados que no existen: `GET /erp/partidas-presupuestarias` (`apps/backoffice/src/app/rutas/contabilidad/dominio/cu101-presupuestos.ts:68`), `/erp/ordenes-de-compra` (`cu102-compras.ts:59`), `/erp/facturas-de-proveedor` (`cu103-facturas.ts`), `/erp/cuentas-por-cobrar` (`cu104-cobros.ts:62`), `/erp/activos` (`cu105-activos.ts`). Hueco declarado en `contrato-erp.ts:10-23`.
- Tablas: `sql/10_tablas/13_contabilidad_erp/{partida_presupuestaria,presupuesto,centro_costo,orden_compra,tercero_comercial,factura_proveedor,cuenta_por_cobrar,activo_fijo,categoria_activo_fijo,depreciacion_activo,periodo_contable}.sql`. Todas con `moneda CHAR(3)` al lado del importe.
- Asientos: `asiento_contable` vive en `nucleo_financiero` (invariante 12, `FixturaDeErp.java:65`). No es de erp: no se lista acá (regla 98.1.1).
- Generador agrupa por primer tramo: todo `/erp` cae en `ErpApi`. Precedente para partir el controlador: `abstract class RutasDeHabilitacion implements OrganizadoresApi` + `OrganizadoresController extends` (`servicios/organizador/.../web/RutasDeHabilitacion.java:25`).
- Patrón de cursor: `servicios/nucleo-financiero/.../aplicacion/ConsultarTesoreria.java` (Pagina, exigirCursor, código del CU).
- Códigos existentes: CU-101 01..03, CU-102 01..03, CU-103 01..05, CU-104 01..03, CU-105 01..03.

## Alcance

- IN: 5 operaciones GET nuevas en `erp.yaml`; repositorio(s) de lectura; servicio de aplicación con records propios; rutas en clase abstracta `ListadosDeErpController` (renombrada desde `RutasDeListadosDeErp`, ver Desvíos en REPORTE); pruebas de integración y web; `ArranqueTest` 19→24; fichas CU-101..105 (códigos + escenarios); postman de erp.
- OUT: listado de asientos (nucleo-financiero); cambios a `sql/` (índices); adaptar la UI al formato paginado (carril B); nombre de la cuenta contable (está en `nucleo_financiero`, no se cruza esquema).
- Ambigüedades registradas:
  1. ¿Qué permiso lee cada listado? Supuesto: el permiso del módulo, más `CONTABILIDAD_ERP_REPORTES` (lectura), más el que opera sobre el listado (`PAGAR` ve facturas; `CUENTAS_POR_PAGAR` ve órdenes). Confirmar con el dueño de producto / Contabilidad.
  2. La UI declara campos de texto libre (`descripcion` de la orden y del activo) y `aprobadaPor`. Supuesto: no se devuelven texto libre de orden (respuesta mínima); el activo sí devuelve `descripcion` porque es la identidad del bien (no datos de persona). Se devuelve `aprobadaPorLaSesion` y no el id del aprobador. Confirmar con carril B.
  3. Orden del cursor: siempre `(columna, id) DESC` (instrucción del carril). Facturas por `fecha_emision`, cuentas por cobrar por `fecha_vencimiento`, activos por `fecha_adquisicion`, órdenes por `creada_en`, partidas por `fecha_inicio` del período.

## H1 — Listados del ERP
**CA:** Dado un operador con el permiso del módulo, cuando pide cada listado, entonces recibe una página ordenada con cursor estable, importes como cadena con moneda, y 403/401/422/400 en los casos de error.
**DoD:** regresión `:servicios:erp:{spotlessCheck,test,webTest,integrationTest,testBarrido,contractTest}` en verde + gates `verificar_criterios`, `verificar_boveda`, `verificar_seguridad`, `generar_postman`.
**Estado:** HECHO

### H1.S1 — Contrato
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | 5 GET + esquemas + códigos AP-CU101-04, AP-CU102-04, AP-CU103-06, AP-CU104-04, AP-CU105-04 en `erp.yaml` | El generador produce los 5 métodos en `ErpApi` | `./gradlew :servicios:erp:compileJava --no-build-cache` → exit 0 tras implementar | HECHO |

### H1.S2 — Lectura contra base real
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Partidas presupuestarias (CU-101) | Recorre por cursor sin repetir; filtra por ejercicio y estado; cursor a medias → AP-CU101-04 | `integrationTest --tests '*CU101ListadoTest'` verde | HECHO |
| H1.S2.M2 | Órdenes de compra (CU-102) | idem, AP-CU102-04 | `--tests '*CU102ListadoTest'` verde | HECHO |
| H1.S2.M3 | Facturas de proveedor (CU-103), con `aprobadaPorLaSesion` del servidor | idem, AP-CU103-06 | `--tests '*CU103ListadoTest'` verde | HECHO |
| H1.S2.M4 | Cuentas por cobrar (CU-104) | idem, AP-CU104-04 | `--tests '*CU104ListadoTest'` verde | HECHO |
| H1.S2.M5 | Activos fijos (CU-105), con `depreciadoEnElPeriodoVigente` | idem, AP-CU105-04 | `--tests '*CU105ListadoTest'` verde | HECHO |

### H1.S3 — Web, documentación y cierre
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | `ListadosDeErpWebTest`: 200, 403 AP-SEG-01, 401, 422, 400 | Todos los casos | `webTest --tests '*ListadosDeErpWebTest'` verde | HECHO |
| H1.S3.M2 | `ArranqueTest` 19→24 y `@MockitoBean` del servicio nuevo en los webTest | Arranca con 24 rutas | `integrationTest --tests '*ArranqueTest'` / `webTest` verde | HECHO |
| H1.S3.M3 | Fichas CU-101..105: códigos y escenarios del backoffice | Gate sin divergencias | `python3 scripts/verificar_criterios.py` exit 0 | HECHO |
| H1.S3.M4 | Postman de erp regenerado | Diff solo en erp | `python3 scripts/generar_postman.py` exit 0 | HECHO |
| H1.S3.M5 | Regresión del servicio + gates | Todo verde | comando de regresión del DoD de H1 | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Otros agentes con Testcontainers en paralelo | fallos de entorno espurios | re-correr en serie antes de clasificar |
| Pruebas de listado ven filas de otras pruebas | aserciones frágiles | fechas en rango 27xx y cursor inicial explícito; partidas filtradas por ejercicio propio |
| Sin índice compuesto para el keyset | rendimiento con volumen | fuera de alcance (`sql/`): registrado como deuda |

## Microtareas agregadas en el camino
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M6 | Derivar pagado/cobrado, saldo y estado de `pago_a_proveedor` / `cobro_cuenta_por_cobrar` (las tablas son append-only y sus columnas quedan congeladas; `saldo_pendiente` y `valor_en_libros` no son GENERATED en el DDL) | El listado coincide con lo que devuelven CU-103/CU-104 al pagar/cobrar | `CU103ListadoTest`, `CU104ListadoTest` verdes (evidencia/01) | HECHO |
| H1.S3.M6 | Renombrar la clase de rutas a `*Controller` para que `generar_postman.py` lea sus `@Permiso` | Sin aviso de erp en el generador | `python3 scripts/generar_postman.py` sin erp en el AVISO (evidencia/05) | HECHO |

Evidencia: `evidencia/01..05`. Estado actualizado 2026-10-16.
