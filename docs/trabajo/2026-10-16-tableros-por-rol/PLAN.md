# Plan — Tableros por rol (financiero, comercial, sistemas), catálogo de datos y de flujos, pruebas con Faker y OpenTelemetry

- Fecha: 2026-10-16 · Repos afectados: PasanakuBackend (+ espejo del frontend para `apps/backoffice`) ·
  Rama: `pablo/feat/tableros-por-rol-2026-10-16` (worktree `../Pasanaku-tableros`, desde `origin/dev` `d63e4fef`) ·
  Predecesor: ninguno
- Resultado observable: una persona con rol `TESORERIA`/`CONTABILIDAD` entra al backoffice y ve el tablero
  financiero; una con rol `COMERCIAL`, el comercial; una con `ADMIN_PLATAFORMA`, el de sistemas, el catálogo de
  datos y el catálogo de flujos. Cada número sale del servicio dueño del dato (el dinero, del mayor), declara su
  fórmula y su fuente, y con la base sembrada por el generador Faker muestra valores no nulos y coherentes.
- Kill-test: con la base sembrada, `GET` del tablero financiero devuelve un KPI cuyo valor no coincide con la
  consulta de cuadre sobre `asiento_contable`/`movimiento_contable`; o una persona sin el permiso obtiene 200.

## Patrón de referencia (Atlas) y adaptación

Se sigue lo ya resuelto en los repos de Atlas (clones en `~/Documents/GitHub/atlas`), con una adaptación forzada
por la regla 98:

| Pieza | En Atlas | En Pasanaku |
|---|---|---|
| Catálogo de KPI | En código, con `code, panel, unit, numerator, denominator, sources, freshness, minSample, emptySample, owner, thresholds`. Lo define `AtlasDashboardsBackend/src/modules/kpi-catalog/kpi-definition.types.ts` | Mismo contrato, en `plataforma` (módulo compartido); cada servicio declara los KPI de los que es dueño |
| Honestidad del número | `null` + `reason` (`SIN_DATOS`, `MUESTRA_INSUFICIENTE`, `FUENTE_NO_CONFIGURADA`, `NO_IMPLEMENTADO`…); toda tasa viaja con numerador y denominador; `isSeed` (`AtlasDashboardsBackend/README.md`) | Igual. La semilla de Faker se marca, y el tablero lo muestra |
| Historia | Foto diaria por KPI (`src/modules/snapshots`) | **Ya existe:** `auditoria.indicador_kpi` (CU-98) guarda cada indicador por período; es la foto |
| Origen de los datos | Un servicio de reporting que **lee las bases hermanas** con un rol de solo lectura | **No se puede**: la regla 98.1.1 prohíbe leer la base de otro servicio, «ni solo para un reporte». **Rediseñado tras H3 (ver «Reencuadre CU-98»):** cada servicio dueño calcula sus indicadores y los publica por outbox (`<servicio>.indicador_calculado`); `auditoria` los guarda en `indicador_kpi` y `GET /indicadores` (CU-98) los publica |
| Catálogo de entidades | Vivo en la base (`system_data_entity_catalog`), con la narrativa exigida por el gate `check:entity-narratives`, más `GET /systems/data-entities` | La narrativa vive en los PlantUML de `docs/Modelos` (fuente de verdad del esquema, `scripts/generar_ddl.py`). Se genera `COMMENT ON`, un `.md` y un JSON, y un gate exige narrativa en toda tabla y columna |
| Catálogo de flujos | Declarado en código (`*.process.fixtures.ts`) y sincronizado de forma idempotente; etapas, pasos, dependencias y transiciones (`AtlasBackend/docs/endpoints/workflow-catalog.md`) | Declarado en archivos versionados, uno por flujo, con el mismo modelo; un generador cruza los pasos con los OpenAPI y los tests |
| Observabilidad | Dominio → `TracingService` → OTel API → SDK → OTLP → Collector (memory_limiter, batch, redaction) (`AtlasBackend/docs/observability/01-architecture-design.md`) | Igual hasta el Collector. Métricas a Prometheus (decisión del 2026-10-16). Trazas: Jaeger efímero solo en dev, como en Atlas |

## Decisiones tomadas con Pablo (2026-10-16)

| # | Pregunta | Decisión |
|---|---|---|
| D1 | ¿De quién es la plata del recargo por mora? | **Del fondo de garantía.** La plataforma no gana con la mora |
| D2 | «Turno de ciclo cerrado» | **El último turno del grupo** (mayor `orden_asignado`, `grupos.turno`) |
| D3 | Roles | **Tres permisos** (`TABLERO_FINANCIERO_VER`, `TABLERO_COMERCIAL_VER`, `TABLERO_SISTEMAS_VER`): el financiero para `TESORERIA` y `CONTABILIDAD`, el de sistemas para `ADMIN_PLATAFORMA`, más el rol nuevo `COMERCIAL` |
| D4 | Telemetría | **OTel + Collector + Prometheus.** Brave se cambia por el bridge de OTel |
| D5 | «Usuarios activos» | **Distinto por tablero.** Comercial: persona con un cupo en un grupo en curso. Sistemas: persona distinta con una sesión usada hoy (desde `identidad`, nunca desde la telemetría) |
| D6 | Período | Decidido «selector de rango». **Desvío (2026-10-16):** CU-98 (fuente de mayor prioridad, regla 00 §8) define períodos `AAAA-MM-DD` / `AAAA-MM` / `AAAA` con serie de hasta 24 períodos; no hay rango libre. El selector ofrece día, mes y año más la serie. A informar a Pablo |
| D7 | KPI extra | Todos los recomendados (ver H4 y H5), más los de sistemas de H6 |
| D8 | Alcance de flujos y pruebas | **Generador más cobertura por fases.** Matriz completa ahora; árboles de decisión de los flujos de dinero ahora; el resto en trabajos siguientes |
| D9 | Catálogo de datos | **Narrativa en el PlantUML**, de donde salen `COMMENT ON`, el `.md`, el JSON y la pantalla |
| D10 | Faker | **En dos lugares, con semilla fija**: un generador Python para `seeders/dev/volumen-*.json` y Datafaker en los fixtures Java |
| D11 | «Pendientes de pago cobrados» | **Los dos casos**: lo que el mismo moroso pagó tarde, y lo recuperado después de que cubrió el fondo |
| D12 | Fondo de inversión | Pablo lo dejó pendiente porque se le dijo que no existía. **Corrección (2026-10-16):** existe en `dev` como servicio `inversiones` (CU-120 a CU-129, `servicios/inversiones/README.md`): inversión voluntaria con el saldo libre del titular, sin dinero de grupos. «Saldo invertido» = suma de las posiciones vigentes (`inversiones.posicion_inversion`), calculada por `inversiones`. Sus parámetros son sintéticos y el aliado es un simulador (`README`, R-INV-01), así que el KPI sale con `isSeed`/«dato sintético» hasta tener un aliado real |

## Alcance

- IN:
  - Penalización por mora completa: cableado del disparo diario de `CU21CobrarAporte.generarRecargos`, que según
    `servicios/aportes/README.md:50` existe pero no lo llama nadie, y asiento del recargo cobrado al fondo de
    garantía (D1).
  - Permisos, rol `COMERCIAL` y su asignación en `seeders/minimos`.
  - Generador Faker de volumen para dev, y Datafaker en los fixtures de prueba.
  - Endpoints `GET /<prefijo>/indicadores` en los servicios dueños, con foto diaria, catálogo de KPI y
    autorización.
  - Tres pantallas de tablero en `apps/backoffice`, cableadas a los clientes generados.
  - OpenTelemetry en todos los servicios y el gateway, el Collector y Prometheus en el compose, y el tablero de
    sistemas.
  - Catálogo de datos (PlantUML → `COMMENT ON`, `.md`, JSON y pantalla) con su gate.
  - Catálogo de flujos: modelo, generador de la matriz endpoint → test, y árboles de los flujos de dinero.
  - Pruebas de integración de los flujos de dinero.
- OUT:
  - Activar inversiones contra un núcleo o un aliado real (bloqueo propio de `inversiones`, fuera de alcance).
  - Árboles de decisión y pruebas de integración de los flujos que no son de dinero, más allá de la matriz
    (fases siguientes, D8).
  - App móvil: los tableros son del backoffice.
  - Tempo y Grafana en TEST.
  - Cualquier cambio que no pida una microtarea: lo roto que se encuentre se anota en el reporte.
- Ambigüedades registradas:
  - A1 · RESUELTA (Pablo, 2026-10-16): «saldo cargado» = lo que la gente sube a su billetera para jugar;
    «saldo retirado» = lo que saca de su billetera. H0.S1.M3 solo localiza las tablas.
  - A2 · Cuenta del recargo en el fondo. Supuesto: el recargo cobrado acredita `2.1.03 Fondo de garantía —
    recursos de los grupos` (`sql/60_semillas/01-plan-de-cuentas.sql:103`). Si contabilidad quiere ver el
    recargo separado, se crea una subcuenta `2.1.03.x`. Confirmar con: responsable contable.
  - A3 · Comisiones cobradas. Supuesto: comisiones de la plataforma devengadas y cobradas por `tarifas`
    (CU-31), cuenta `4.1`. No existe comisión del organizador (regla 91.5). Confirmar con: Pablo.
  - A4 · Muestra mínima por tasa: 20 casos en el denominador (mora, recuperación, retención). Es un parámetro de
    presentación, no financiero. Confirmar con: Pablo.
  - A5 · RESUELTA en H0.S1.M4: endpoints por servicio dueño (`transparencia` no proyecta dinero). Texto original: Composición de los KPI: llamadas desde el backoffice a cada servicio, o un modelo de lectura en
    `transparencia` alimentado por eventos. Supuesto: llamadas directas (un salto, cumple 98.2.5). Se valida en
    H0.S1.M4; si `transparencia` ya proyecta esos hechos, se usa esa vía y se registra el desvío.

## H0 — Descubrimiento factual

**CA:** Dado este plan, cuando se cierra H0, entonces cada KPI tiene identificados su servicio dueño, su tabla o
cuenta de origen y su fórmula, citados con ruta y línea, y A1 y A5 quedan resueltas con evidencia.
**DoD:** `evidencia/H0-01-asientos.md` y `evidencia/H0-02-a-06.md` (dueño y fuente de cada KPI con su ruta).
**Estado:** HECHO

### H0.S1 — Fuentes y patrones

**CA:** Toda fuente de dato de los tableros está localizada; ninguna se supone.
**DoD:** El mapa de H0 completo, sin celdas vacías.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H0.S1.M1 | Qué operaciones generan asientos en `nucleo-financiero`: aporte, recarga, retiro, cobertura, recuperación, entrega, comisión, recargo | Cada operación tiene asiento localizado o «sin asiento» declarado | `grep -rn "asiento" servicios/*/src/main` pegado en `evidencia/H0-01-asientos.txt` | HECHO — `evidencia/H0-01-asientos.md` |
| H0.S1.M2 | Patrón de un endpoint de lectura con autorización por permiso (controlador, `ContextoSesion`, OpenAPI, cliente generado) | Dos ejemplos vecinos citados | `evidencia/H0-02-patron-lectura.md` con rutas | HECHO — `evidencia/H0-02-a-06.md` |
| H0.S1.M3 | Resolver A1: billetera, recarga y retiro en el modelo | Tabla y estado de «recarga acreditada» y «retiro pagado» citados | `grep` sobre `docs/Modelos` y `sql/10_tablas` pegado | HECHO — `evidencia/H0-02-a-06.md` |
| H0.S1.M4 | Resolver A5: qué proyecta hoy `transparencia` | Decisión registrada en «Decisiones» con evidencia | `evidencia/H0-04-transparencia.md` | HECHO — `evidencia/H0-02-a-06.md` |
| H0.S1.M5 | Cómo el backoffice obtiene la sesión y los permisos, y cómo protege rutas (`guardia-rol-sistemas.ts`, `matriz-de-roles.spec.ts`) | Patrón citado | `evidencia/H0-05-guardias.md` | HECHO — `evidencia/H0-02-a-06.md` |
| H0.S1.M6 | Versiones instaladas: Spring Boot, Micrometer, el bridge de OTel y Datafaker compatibles (catálogo `gradle/libs.versions.toml`), y Python Faker | Versiones verificadas en la documentación oficial, no supuestas | Enlaces y versiones en `evidencia/H0-06-versiones.md` | HECHO — `evidencia/H0-02-a-06.md` |

## H1 — La penalización por mora se cobra y queda en el fondo de garantía

**CA:** Dado un aporte vencido con política de mora del grupo, cuando corre el trabajo diario, entonces se genera
un único `RECARGO_MORA`. Cuando el participante lo paga, el mayor registra débito en caja o banco y crédito en el
fondo de garantía, y una segunda corrida no duplica nada.
**DoD:** Tests de integración contra Postgres real en verde, con los cinco puntos de la regla 91.6 pegados en el
reporte (asientos, idempotencia, cuadre, reversa y redondeo).
**Estado:** TODO

### H1.S1 — Disparo diario

**CA:** El método existente se ejecuta una vez por día por instancia del clúster.
**DoD:** Test con dos ejecuciones del mismo día → un recargo por obligación.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `@Scheduled` + ShedLock para `generarRecargos`, siguiendo el patrón de otro trabajo programado del repo (localizarlo antes) | El bean se registra con su bloqueo `aportes-recargos-mora` | `./gradlew :servicios:aportes:test --tests '*Recargo*'` en verde | HECHO — `evidencia/H1-S1-recargos-diarios.txt` (test renombrado a `CU21RecargosDiariosTest` por la convención de corredores; bean verificado en `ArranqueTest`) |
| H1.S1.M2 | Test de la segunda corrida del mismo día | 0 recargos nuevos | Mismo comando, el test nombrado en verde | HECHO — `CU21RecargosDiariosTest.dosCorridasElMismoDia`, mismo archivo de evidencia |

### H1.S2 — Contabilización del recargo cobrado

**CA:** El pago de un `RECARGO_MORA` genera un asiento balanceado con crédito al fondo de garantía (D1, A2).
**DoD:** Consulta de cuadre pegada: `SUM(debe)=SUM(haber)` por asiento, y saldo de `2.1.03` igual a la suma de
recargos cobrados del escenario.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S2.M0a | (Agregada por el hallazgo H0-01) El evento `aportes.aporte_cobrado` publica `tipo` de la obligación (campo aditivo; versión del contrato documentada) | El payload trae `tipo` | Test de contrato del productor en `aportes` en verde | HECHO — `evidencia/H1-S2-M0a-tipo-en-evento.txt` (consumidor ignora campos extra: compatible) |
| H1.S2.M0b | (H0-01) `ConsumoDeAportes` lee `tipo` opcional; un evento sin `tipo` sigue el camino actual (compatibilidad) | Consumidor viejo y nuevo conviven | Test del lector Kafka con y sin `tipo` en verde | HECHO — `evidencia/H1-S2-M0b-consumidor-tipo.txt` |
| H1.S2.M0c | (H1-02, rediseñada) Billetera de sistema `FONDO_GARANTIA` única por moneda (`SYS-FONDO-GARANTIA`, `SYS-FONDO-GARANTIA-USD`) sembrada en `seeders/minimos/22-cuentas-de-sistema.json`, más `CuentasDeSistemaRepositorio.fondoDeGarantia` | Una activa por moneda; segunda siembra = 0 filas | `python3 scripts/generar_semillas.py` → exit 0 + test del repositorio en verde | HECHO — `evidencia/H1-S2-M0c-semilla-fondo.txt` (855 → 0 filas; la búsqueda `fondoDeGarantia` se ejercita en M1) |
| H1.S2.M0d | (H1-02) Esquema: valor `RECARGO_MORA` en `transaccion_billetera.tipo` y en `garantia.movimiento_fondo.tipo` (PlantUML → `generar_ddl.py`) | DDL regenerado, deriva en verde | `python3 scripts/generar_ddl.py` → exit 0 + verificación de deriva pegada | HECHO — `evidencia/H1-S2-M0d-esquema-recargo.txt` (CK_AMPLIADOS; base existente actualizada; gates del CI en verde) |
| H1.S2.M1 | `RECARGO_MORA` cobrado: débito a la billetera del participante y crédito a la `FONDO_GARANTIA` del grupo; asiento DEBE `SALDO_USUARIOS` / HABER `RESERVA_RESPALDO`; el pozo del grupo no cambia | Asiento cuadrado con moneda; saldo del pozo sin cambio | Test de integración `*RecargoContabilizado*` en verde | HECHO — `evidencia/H1-S2-M1-recargo-al-fondo.txt` (6/6; regresión nucleo 84+482 en verde) |
| H1.S2.M1b | Sin billetera de fondo: `aporte_no_aplicado` con motivo `FONDO_SIN_BILLETERA` (aportes compensa); nunca se acredita al pozo | 0 movimientos y evento de compensación | Test nombrado en verde | HECHO con desvío — se sigue el patrón existente de la billetera del grupo: AP-CU12-02 SIN marcar (el reproceso lo aplica), no compensación. Test `sinBilleteraDelFondo` |
| H1.S2.M1c | `aporte_aplicado` lleva `tipo`; `garantia` lo consume y, si es `RECARGO_MORA`, registra `movimiento_fondo` `RECARGO_MORA` y suma a `saldo_disponible` del fondo del grupo (idempotente por evento) | Fondo +monto una sola vez | Test del consumidor en `garantia` en verde | HECHO — `evidencia/H1-S2-M1c-garantia-anota.txt` (primer consumidor Kafka de garantia; regresión 38+159 en verde) |
| H1.S2.M2 | Idempotencia: el mismo pago confirmado dos veces | Un solo asiento (clave de idempotencia `UNIQUE`) | Test nombrado en verde, salida pegada | HECHO — cubierta por `CUConsumoDeRecargoTest.duplicado`/`reintento` (nucleo) y `CUIngresoAlFondoTest.duplicado` (garantia), en las evidencias de M1 y M1c |
| H1.S2.M3 | Reversa: si se anula el pago, se genera un contra-asiento | Contra-asiento presente, sin `UPDATE` sobre el mayor | Test nombrado en verde | A MEDIAS — anda: CU-14 sobre el recargo devuelve Bs 25 al participante y se los quita al fondo (`CUConsumoDeRecargoTest.reversaDelRecargo`, 7/7). No anda: (a) CU-14 no escribe contra-asiento en el mayor para NINGUNA transacción (preexistente, fuera de alcance); (b) garantia no se entera de la reversa (→ H1.S2.M4) |
| H1.S2.M4 | (Agregada) `garantia` consume `nucleo_financiero.transaccion_reversada`: si la original es un `RECARGO_MORA` anotado, resta del `saldo_disponible` y deja un `movimiento_fondo` de ajuste que referencia el reverso | Tras reversar, fondo = saldo previo; idempotente | Test del consumidor en garantia en verde | HECHO — `evidencia/H1-S2-M4-reversa-en-garantia.txt` |

## H2 — Semillas: permisos, rol y volumen sintético

**CA:** Dado `python3 scripts/generar_semillas.py` y la base aplicada, cuando se siembra dos veces, entonces
existen los tres permisos, el rol `COMERCIAL` y un volumen sintético marcado como tal, y la segunda corrida no
inserta nada.
**DoD:** Salida de las dos corridas pegada, conteos por tabla y verificación de deriva (regla 97.7).
**Estado:** HECHO

### H2.S1 — Permisos y rol (`seeders/minimos`)

**CA:** La matriz rol × permiso coincide con D3.
**DoD:** Consulta de la matriz pegada.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Los tres permisos y el rol `COMERCIAL` en el JSON de mínimos (el mismo origen que `10-roles-y-permisos.sql`) | El generador no reporta error | `python3 scripts/generar_semillas.py` → exit 0 | HECHO — `evidencia/H2-S1-permisos-y-rol.txt` |
| H2.S1.M2 | Asignaciones según D3 | La consulta devuelve las 4 filas esperadas | `psql -c "select r.codigo,p.codigo …"` pegado | HECHO — `evidencia/H2-S1-permisos-y-rol.txt` |
| H2.S1.M3 | Idempotencia | 0 filas en la segunda corrida | Las dos corridas pegadas | HECHO — `evidencia/H2-S1-permisos-y-rol.txt` |

### H2.S2 — Generador Faker de volumen (`seeders/dev`)

**CA:** Con una semilla fija, el generador produce siempre el mismo JSON: ~300 personas, ~40 grupos en estados
variados, 12 meses de aportes, moras, coberturas, recuperaciones, recargos, entregas, recargas y retiros. Todo
importe es decimal con moneda, todo asiento cuadra, y cada registro lleva la marca de sintético.
**DoD:** Dos corridas → `sha256` idéntico; el script de verificación de cuadre en verde.
**Estado:** HECHO — con desvío: en vez de inventar 60 tablas con Faker se CLONA el escenario demo coherente (12 clones, uno por mes) con claves renombradas y personas de Faker; conjunto propio `seeders/volumen/` → `sql/62_volumen` con marca propia. Se anclaron al grupo 3 subconsultas del seed de dev que asumían un único grupo.

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H2.S2.M1 | `scripts/generar_volumen_dev.py` con Faker (versión de H0.S1.M6), semilla fija y `Decimal` | Escribe `seeders/dev/volumen-*.json` con `entorno: dev` | `python3 -I scripts/generar_volumen_dev.py && sha256sum seeders/dev/volumen-*` dos veces, mismo hash | HECHO — `evidencia/H2-S2-volumen.txt` |
| H2.S2.M2 | Coherencia de dominio: estados de turnos y obligaciones válidos contra los `CHECK` | La guarda de `generar_semillas.py` acepta los archivos | `python3 scripts/generar_semillas.py` → exit 0 | HECHO — `evidencia/H2-S2-volumen.txt` |
| H2.S2.M3 | Siembra real en la base dev y cuadre del mayor | `SUM(debe)=SUM(haber)` por asiento, 0 huérfanos | Consultas de verificación pegadas | HECHO — `evidencia/H2-S2-volumen.txt` |
| H2.S2.M4 | Idempotencia de la siembra | 0 inserciones en la segunda corrida | Salida pegada | HECHO — `evidencia/H2-S2-volumen.txt` |

### H2.S3 — Datafaker en los fixtures Java

**CA:** Los fixtures de los servicios tocados por este trabajo generan personas y montos con Datafaker y una
semilla fija; el mismo test produce los mismos datos en cada corrida.
**DoD:** Test de determinismo en verde.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H2.S3.M1 | Dependencia `datafaker` de test en el catálogo de versiones (justificada según 90.4.2) | Compila | `./gradlew :plataforma:<modulo-test>:compileTestJava` → exit 0 | HECHO — `evidencia/H2-S3-datafaker.txt` |
| H2.S3.M2 | Fábrica compartida `DatosSinteticos` con semilla fija | Dos instancias con la misma semilla dan los mismos valores | Test unitario nombrado en verde | HECHO — `evidencia/H2-S3-datafaker.txt` |

## H3 — Contrato común de indicadores y catálogo de KPI

**CA:** Todo endpoint de indicadores responde el mismo contrato:
`{ code, value|null, reason?, numerator?, denominator?, unit, moneda?, periodo, comparacion, isSeed }`,
con su ficha en el catálogo, y un KPI sin ficha no se publica.
**DoD:** Tests unitarios del contrato (`null` ≠ 0, muestra mínima, tasa con numerador y denominador) en verde,
y el contrato en el OpenAPI de cada servicio.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Tipos `DefinicionKpi` / `ResultadoKpi` / `MotivoSinValor` en `plataforma`, espejo de `kpi-definition.types.ts` de Atlas | Compila | `./gradlew :plataforma:<modulo>:compileJava` → exit 0 | HECHO — `IndicadorTest` 8/8; `:plataforma:comun-dominio:check` (cobertura 97/96) en verde |
| H3.S1.M2 | `Guardas` de publicación: muestra mínima, `emptySample`, moneda obligatoria en dinero | Tres casos (correcto, límite, inválido) | Test unitario nombrado en verde | HECHO — `IndicadorTest` 8/8; `:plataforma:comun-dominio:check` (cobertura 97/96) en verde |
| H3.S1.M3 | Esquema `ResultadoKpi` reutilizable en los OpenAPI | El lint de contratos pasa | Comando de lint de OpenAPI del repo → exit 0 | DESCARTADO — por el reencuadre CU-98 el contrato HTTP ya existe (`SalidaTablero`/`Indicador` en `auditoria.yaml`); decidido por el agente con evidencia, a confirmar con Pablo |
| H3.S1.M4 | Foto diaria: tabla `foto_kpi` por servicio dueño (PlantUML → DDL) y trabajo programado | Deriva en verde | `python3 scripts/generar_ddl.py` + verificación de deriva pegada | DESCARTADO — redundante: la foto es `auditoria.indicador_kpi` (CU-98) |

## Reencuadre CU-98 (2026-10-16, tras H3)

Al abrir H4 se encontró `docs/CasosDeUso/CU-98 Publicar el tablero de indicadores.md`, **implementado en
`auditoria` solo del lado de la publicación**: `GET /indicadores` (`IndicadoresController`, `CU98PublicarTablero`) lee
`auditoria.indicador_kpi` unido a `definicion_indicador`, y aplica meta, supresión por mínimo de casos, variación y
serie. El README dice: «el valor lo deja el trabajo de cálculo en `indicador_kpi`», y **ese trabajo no existe**.
Regla 96.1: se completa lo existente en lugar de crear endpoints paralelos por servicio.

- Cada servicio dueño calcula sus indicadores con `Indicador` (H3) y los emite por outbox:
  `<servicio>.indicador_calculado` `{codigo, nombre, valor, unidad, periodo, casos, provisorio}`. Si `Indicador` da
  `SIN_DATOS`, no se emite: la tabla no admite `valor` nulo y CU-98 muestra el indicador ausente, no un cero.
- `auditoria` consume esos temas y guarda el valor en `indicador_kpi`. **Corregido al abrir H4.S0.M3:** el modelo
  (`docs/entidades/09_auditoria_reportes.puml:702-738`) define UN valor por período y versión de definición, en una
  tabla append-only (una corrección = versión nueva de la definición). Por eso **solo se emiten períodos CERRADOS**
  (ayer; el mes y el año al terminar), con `ON CONFLICT DO NOTHING`, y el tablero abre en el último período cerrado.
- **Ambigüedad A7:** CU-98 (flujo 1a) habla de indicadores «provisorios» de un período abierto, pero la unicidad
  (código, dimensión, período, definición) no permite actualizarlos. Supuesto: no se guardan períodos abiertos.
  Confirmar con: Pablo (dueño de CU-98).
- `definicion_indicador` se siembra en `minimos/` con el catálogo de tableros (fórmula, fuente, familia, dueño,
  mínimo de casos). **Sin metas:** son una decisión de negocio (CU-98 «metas fijadas antes del período») y sin meta no
  hay semáforo, que es lo correcto.
- Autorización por familia en el servidor: `AUDITORIA_LEER` ve todo; `TABLERO_FINANCIERO_VER` ve `FINANZAS`+`RIESGO`;
  `TABLERO_COMERCIAL_VER` ve `NEGOCIO`; `TABLERO_SISTEMAS_VER` ve `OPERACION`.
- `provisorio`: siempre `false`, porque solo se emiten períodos cerrados. No refleja si cuadraron los cierres contables, que cada servicio no conoce (riesgo anotado).
- Los de saldo (stock: mora vigente, fondo, saldo invertido) solo se calculan para el período en curso; sin historia
  previa, CU-98 los muestra sin variación («indicador nuevo sin historia»). H3.S1.M4 (foto propia) queda **DESCARTADA
  por redundante**: la foto es `indicador_kpi`.

## H4 — Tablero financiero

**CA:** Dado un usuario con `TABLERO_FINANCIERO_VER` y un período (día, mes o año), cuando abre Tablero → Financiero,
entonces ve los indicadores de `FINANZAS` y `RIESGO`: **saldo cargado**, **pendientes cobrados** (pago tarde propio y
recuperación), **saldo retirado**, **saldo invertido**, **dinero en mora**, **impagos cubiertos**, **intereses por
mora cobrados**, **comisiones cobradas**, **turnos normales** y **turnos que cierran ciclo**, **tasa de mora**,
**cobertura del fondo**, **recuperación de lo cubierto**, **entregas pendientes** y **tiempo medio turno → desembolso**.
Cada valor cuadra con su consulta de origen. Un usuario sin el permiso no recibe esas familias (servidor).
**DoD:** Cuadre por KPI contra la base sembrada; matriz de autorización por familia; E2E y capturas en 3 viewports × 2
temas × 4 estados.
**Estado:** TODO

### H4.S0 — El sumidero en `auditoria` (común a H4, H5 y H6)

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H4.S0.M1 | Catálogo `definicion_indicador` de los tableros en `seeders/minimos` (código, v1, familia, dueño, fórmula, fuente, mínimo de casos) | Siembra idempotente | Dos siembras (N → 0) pegadas | HECHO — `evidencia/H4-S0-M1-definiciones.txt` (36 → 0) |
| H4.S0.M2 | `PublicadorDeIndicadores` en `plataforma`: `Indicador` → evento `<servicio>.indicador_calculado` por outbox; `SIN_DATOS` no emite | Correcto, límite (sin datos) e inválido (sin código) | Test unitario en verde | HECHO — `PublicadorDeIndicadoresTest` 5/5 |
| H4.S0.M3 | `auditoria` consume los temas `*.indicador_calculado` y guarda en `indicador_kpi` con la definición vigente al inicio del período; idempotente; sin definición → se descarta con aviso (AP-CU98-02) | Mismo evento dos veces → una fila; mismo período otra vez → no pisa | Test de integración en verde | HECHO — `CURegistroDeIndicadoresTest` 4/4, `ConsumidorDeIndicadoresKafkaTest` 3/3, `ArranqueTest` 1/1 |
| H4.S0.M4 | CU-98 autoriza por familia: `@PermisoAlternativo` con los tres permisos y filtro en el servidor | Sin permiso de la familia → no aparece; sin ninguno → 403 | Test de autorización negativa en verde | HECHO — `evidencia/H4-S0-M4-familias.txt` |

### H4.S1 — Calculadores por servicio dueño (dinero desde el mayor)

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H4.S1.M1 | `nucleo-financiero`: cargado, retirado, intereses por mora y comisiones cobradas, desde `asiento_contable`+`movimiento_contable` por tipo de transacción | Cada valor = consulta de cuadre | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*Indicadores*'` en verde | HECHO — `evidencia/H4-S1-M1-nucleo.txt` |
| H4.S1.M2 | `garantia`: impagos cubiertos, recuperado, recuperación y cobertura del fondo (redefinida dentro de garantia; la mora pasa a `aportes`, H4.S1.M3) | Tasas con numerador y denominador | `./gradlew :servicios:garantia:integrationTest --tests '*Indicadores*'` en verde | HECHO — `evidencia/H4-S1-M2-garantia.txt` |
| H4.S1.M3 | `aportes`: dinero en mora, tasa de mora y pendientes cobrados (pago tarde propio) | Dos componentes separados | `./gradlew :servicios:aportes:integrationTest --tests '*Indicadores*'` en verde | HECHO — `evidencia/H4-S1-M3-aportes.txt` |
| H4.S1.M4 | `grupos`: turnos normales y turnos de cierre (D2) | El de cierre se cuenta una vez | `./gradlew :servicios:grupos:integrationTest --tests '*Indicadores*'` en verde | HECHO — `evidencia/H4-S1-M4-grupos.txt` |
| H4.S1.M5 | `entregas`: entregas pendientes y tiempo medio | `SIN_DATOS` sin entregas | `./gradlew :servicios:entregas:integrationTest --tests '*Indicadores*'` en verde | HECHO — `evidencia/H4-S1-M5-entregas.txt` |
| H4.S1.M6 | `inversiones`: saldo invertido (posiciones vigentes), marcado sintético (D12) | Valor = consulta | `./gradlew :servicios:inversiones:integrationTest --tests '*Indicadores*'` en verde | HECHO — `evidencia/H4-S1-M6-inversiones.txt` |
| H4.S1.M7 | Disparo: trabajo diario por servicio (ayer; mes y año anteriores al cambiar) y relleno de los últimos 13 meses cerrados para los de flujo | Corre una vez por día; relleno idempotente | Test del trabajo en verde | HECHO — `evidencia/H4-S1-M7-disparo.txt` |
| H4.S1.M8 | Punta a punta con la base sembrada: calculadores → Kafka → `auditoria` → `GET /indicadores` | Valores = consultas de cuadre | Ensayo con el stack local pegado | TODO — se ejecuta en H9 con el stack levantado (regla 70: un stack a la vez) |

### H4.S2 — Pantalla

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H4.S2.M1 | Tablero financiero: contenedor + presentacionales reusando los de `tablero/`; selector día/mes/año y serie; cuatro estados | Specs en verde | `yarn --cwd apps/backoffice test:front` (specs nombrados) | HECHO — `evidencia/H4-S2-frontend.txt` |
| H4.S2.M2 | Ruta con `requierePermiso('TABLERO_FINANCIERO_VER')` | Spec de la guardia en verde | Spec en verde | HECHO — ruta con `requierePermiso` + 403 del servidor probado en `tablero-por-rol.spec` |
| H4.S2.M3 | E2E contra el stack real sembrado | Pantalla = cuadre | `yarn --cwd apps/backoffice test:e2e tablero-financiero` con trace | TODO |
| H4.S2.M4 | Prueba visual | 3 viewports × 2 temas × 4 estados | Capturas en `evidencia/H4/` | TODO |

## H5 — Tablero comercial

**CA:** Con `TABLERO_COMERCIAL_VER`, la familia `NEGOCIO`: usuarios nuevos, activos (D5), embudo de alta, grupos
nuevos y completos, días hasta llenarse, retención a 60 días y volumen comprometido. Misma honestidad y autorización
que H4.
**DoD:** Igual que H4.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H5.S1.M1 | `identidad`: usuarios nuevos (paso 1 del embudo) y KYC aprobados (paso 2) | Valores = cuadre | `./gradlew :servicios:identidad:integrationTest --tests '*Indicadores*'` en verde | HECHO — `evidencia/H5-S1-M1-identidad.txt` (1 falla PREEXISTENTE en dev, ajena, documentada) |
| H5.S1.M2 | `grupos`: activos comerciales, grupos nuevos y completos, días de llenado, retención | Retención con num/den | `./gradlew :servicios:grupos:integrationTest --tests '*IndicadoresComerciales*'` en verde | HECHO — `evidencia/H5-S1-M2-grupos.txt` |
| H5.S1.M3 | `aportes`: primer aporte (paso 4 del embudo); el volumen comprometido pasó a `grupos` (dueño de cupos, cuota y períodos) | Decimal con moneda | `./gradlew :servicios:aportes:integrationTest --tests '*IndicadoresComerciales*'` en verde | HECHO — `evidencia/H5-S1-M3-aportes.txt` |
| H5.S2.M1 | Pantalla comercial con embudo y fichas | Specs en verde | `yarn --cwd apps/backoffice test:front` (specs nombrados) | HECHO — misma pantalla parametrizada (`tipo: comercial`), embudo probado; `evidencia/H4-S2-frontend.txt` |
| H5.S2.M2 | E2E real y prueba visual | Pantalla = cuadre; capturas | `test:e2e tablero-comercial` + `evidencia/H5/` | TODO |

## H6 — OpenTelemetry y tablero de sistemas

**CA:** Dado el stack levantado, cuando se hacen solicitudes, entonces cada servicio exporta métricas y trazas
OTLP al Collector, Prometheus las guarda, y el tablero de sistemas (permiso `TABLERO_SISTEMAS_VER`) muestra
solicitudes, exitosas, fallidas (4xx y 5xx separados), tasa de error, latencia p50/p95/p99, usuarios activos de
hoy (D5) y el desglose de los endpoints tocados hoy (ruta plantilla, conteo, error y p95), sin ningún dato
personal en etiquetas ni atributos.
**DoD:** Una traza con el mismo `traceId` cruzando el gateway y un servicio. Consulta a Prometheus pegada,
igual al tablero. Escaneo de etiquetas sin PII pegado. Prueba de caída: con el Collector apagado, los servicios
siguen respondiendo.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Cambiar `micrometer-tracing-bridge-brave` por el bridge de OTel más el exportador OTLP (versiones de H0.S1.M6) | Todos los módulos compilan | `./gradlew compileJava` → exit 0 | HECHO — `evidencia/H6-S1-M1-otel.txt` |
| H6.S1.M2 | Configuración común en `plataforma`: recurso (`service.name`, `version`), muestreo y redacción de atributos | Test de redacción: un atributo con documento o cuenta sale enmascarado | Test unitario nombrado en verde | A MEDIAS — defaults comunes y redacción en el Collector escritos y validados; falta verlos con spans reales (H9) |
| H6.S1.M3 | Etiqueta de ruta plantilla (`/grupos/{id}`), nunca la URL cruda | Ninguna serie con un UUID en la etiqueta | `curl prometheus/api/v1/series` filtrado, pegado | TODO |
| H6.S2.M1 | Collector (`memory_limiter`, `batch`, `attributes/redaction`) y Prometheus en `docker-compose` dev y en Coolify, solo en la red interna | Contenedores sanos y puertos sin exponer | `docker compose ps` + `curl` desde afuera rechazado, pegados | HECHO — `evidencia/H6-S2-M1-collector-prometheus.txt` (configs validadas; perfil `observabilidad`; TEST apagado hasta operar el host) |
| H6.S2.M2 | Jaeger efímero solo en dev (como Atlas) | Traza visible | Captura de la traza en `evidencia/H6/` | TODO |
| H6.S2.M3 | Prueba de caída del Collector | El servicio sigue respondiendo 200 | Salida pegada | TODO |
| H6.S3.M1 | Endpoint de indicadores de sistemas que consulta Prometheus por HTTP con timeout, consultas en lista blanca y degradación declarada (`FUENTE_NO_CONFIGURADA`). El servicio que lo hospeda se decide en H0 | Correcto, límite (sin tráfico) e inválido (Prometheus caído) | Test de integración con Prometheus real más un doble para «caído» | HECHO contra doble — `evidencia/H6-S3-M1-sistemas.txt` (Prometheus real en H9) |
| H6.S3.M2 | Usuarios activos hoy desde las sesiones de `identidad` | Valor = consulta | Test nombrado en verde | HECHO — `evidencia/H6-S3-M2-activos.txt` |
| H6.S3.M3 | Pantalla de sistemas (Sistemas → Tráfico) | Specs de los cuatro estados en verde | `yarn --cwd apps/backoffice test:front` (specs nombrados) | HECHO — `evidencia/H6-S3-M3-pantalla-sistemas.txt` |
| H6.S3.M4 | E2E real y prueba visual | Tablero = consulta a Prometheus | `yarn --cwd apps/backoffice test:e2e tablero-sistemas` + `evidencia/H6/` | TODO |

## H7 — Catálogo de datos

**CA:** Toda tabla y columna de todos los esquemas tiene en el PlantUML su narrativa de negocio («por qué
existe») y de sistema («qué la escribe y quién la lee»). De ahí se generan `COMMENT ON`,
`docs/catalogo-de-datos.md` y `catalogo-datos.json`, y la pantalla Sistemas → Catálogo de datos permite buscar
por esquema, tabla y columna y ver las relaciones. Un gate falla si falta una narrativa.
**DoD:** `python3 scripts/generar_ddl.py` → exit 0, el gate de narrativa en verde, verificación de deriva y
prueba visual.
**Estado:** TODO

> **Corrección del plan (H7, 2026-10-16, D13).** El descubrimiento encontró que la narrativa de negocio por tabla
> YA existe, escrita a mano, en `docs/entidades/*.md` (secciones «Qué es», «Para qué sirve (negocio)», «Por qué
> debe existir», «A nivel de sistema»; ver `docs/entidades/README.md:17-26`): 271 de 337 tablas tienen ficha. Poner
> la narrativa además en el PlantUML crearía dos fuentes de verdad (regla 97.1.1). Se lee de las fichas. La
> narrativa de SISTEMA se deriva del modelo (servicio dueño, append-only, FK salientes y entrantes, CHECK, índices).
> La clasificación de sensibilidad reusa el vocabulario de `SalidasSensibles.java:74-80`. Ambigüedad A8 (columnas):
> 490 de 3680 columnas tienen nota en su ficha; escribir a mano las otras ~3200 no cabe en este trabajo. Se cubren
> con la ficha cuando existe, un diccionario de convenciones del proyecto para los nombres repetidos (`id`,
> `estado`, `moneda`, `clave_idempotencia`, `vigente_desde`…) y la descripción estructural; el gate exige ficha en
> el 100 % de las tablas y trinquete (no baja) en la cobertura de columnas.
>
> **D14 (2026-10-17).** El catálogo en JSON describe el esquema completo de la base. Servido como archivo estático
> del backoffice quedaría accesible sin sesión (regla 90.5, superficie pública no autenticada). Se sirve desde
> `auditoria` con `TABLERO_SISTEMAS_VER`. Los módulos 13 y 14 ya tenían su narrativa, escrita por grupo de
> tablas: se extendió el lector en lugar de reescribirla, y el gate acepta «Para qué sirve» o «Por qué debe existir».

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H7.S1.M1 | `scripts/catalogo_de_datos.py`: lee las fichas `docs/entidades/*.md` por tabla y por columna (sin sintaxis nueva en el PlantUML, D13) | Devuelve la ficha de `obligacion_aporte` y la nota de `monto_esperado` | `python3 -I scripts/catalogo_de_datos.py --tabla obligacion_aporte` pegado | HECHO — `evidencia/H7-S1-M1-lector-de-fichas.txt` |
| H7.S1.M2 | El generador emite `COMMENT ON TABLE/COLUMN` con la narrativa (en lugar de la anotación cruda) | DDL regenerado; `obj_description` devuelve el texto en una base aplicada | `psql` pegado | HECHO — `evidencia/H7-S1-M2-comment-on.txt` |
| H7.S1.M3 | Gate `scripts/verificar_narrativas.py`: 100 % de tablas con ficha y trinquete de cobertura de columnas | Falla con una tabla sin ficha y pasa sin faltantes | Dos corridas pegadas | HECHO — `evidencia/H7-S1-M3-gate-narrativas.txt` (entra al CI cuando el modelo completo pase, H7.S1.M5) |
| H7.S1.M4a | Fichas faltantes de los módulos 01, 02, 04, 07, 10 y 11 (14 tablas) | Gate filtrado por esos módulos → exit 0 | `verificar_narrativas.py --modulo …` pegado | HECHO — `evidencia/H7-S1-M4a-fichas.txt` |
| H7.S1.M4b | Fichas faltantes del módulo 08 (6 tablas) | Ídem | Ídem | HECHO — `evidencia/H7-S1-M4b-fichas-respaldo.txt` |
| H7.S1.M4c | Fichas faltantes del módulo 13 (18 tablas) | Ídem | Ídem | HECHO — `evidencia/H7-S1-M4cd-fichas-13-14.txt` (narrativa existente, lector extendido) |
| H7.S1.M4d | Fichas faltantes del módulo 14 (14 tablas) | Ídem | Ídem | HECHO — `evidencia/H7-S1-M4cd-fichas-13-14.txt` (narrativa existente, lector extendido) |
| H7.S1.M4e | Fichas del módulo 15 (inversiones, sin `.md`; 14 tablas) | Ídem | Ídem | HECHO — `evidencia/H7-S1-M4e-fichas-inversiones.txt` |
| H7.S1.M5 | Generar `docs/catalogo-de-datos.md` y `servicios/auditoria/src/main/resources/catalogo/catalogo-datos.json` (esquemas, tablas, columnas, tipos, nulabilidad, PK, FK, relaciones, sensibilidad) — D14: NO en `public/` del backoffice, que se sirve sin sesión | Dos corridas idénticas | `shasum -a 256` dos veces, pegado | HECHO — `evidencia/H7-S1-M5-catalogo-generado.txt` |
| H7.S1.M6 | `GET /indicadores/catalogo-de-datos` en `auditoria` (contrato OpenAPI + controlador que sirve el recurso), con `TABLERO_SISTEMAS_VER` | 200 con permiso; 403 sin permiso; 401 sin token | Test web nombrado en verde | HECHO — `evidencia/H7-S1-M6-endpoint-catalogo.txt` |
| H7.S1.M7 | Gate `verificar_narrativas.py` en el CI (paso 6i de `ci.yml`) | El paso existe y el gate pasa en el modelo completo | `grep` del paso + corrida local exit 0 | HECHO — `evidencia/H7-S1-M7-gate-en-ci.txt` |
| H7.S2.M1 | Pantalla de catálogo (lista, búsqueda normalizada y ficha de tabla con relaciones) consumiendo el JSON publicado | Specs de los cuatro estados en verde | `yarn --cwd apps/backoffice test:front` (specs nombrados) | TODO |
| H7.S2.M2 | Prueba visual | Capturas inspeccionadas | `evidencia/H7/` | TODO |

## H8 — Catálogo de flujos y orquestación de pruebas

**CA:** Existe un catálogo de flujos declarado en archivos versionados (etapas, pasos con endpoint, actor,
precondición, transiciones de éxito, error y condición, y casos válidos, límite e inválidos) y un generador que
cruza los 162 endpoints de los OpenAPI con sus tests y con los flujos. Los flujos de dinero (aporte → mora →
cobertura → recuperación → entrega, más recarga y retiro) tienen árbol completo y prueba de integración de punta a
punta.
**DoD:** `docs/catalogo-de-flujos/matriz.md` generado con conteos reales (endpoints con y sin test unitario o web
y con y sin flujo), más las pruebas de integración de los flujos de dinero en verde.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H8.S1.M1 | Formato del flujo (YAML por flujo, modelo de `workflow-catalog.md` de Atlas: etapas, pasos, dependencias y transiciones) con su esquema de validación | Un flujo inválido se rechaza | `python3 -I scripts/validar_flujos.py` con un caso bueno y uno malo, pegado | TODO |
| H8.S1.M2 | `scripts/matriz_de_pruebas.py`: OpenAPI × tests × flujos | Lista los 162 endpoints con su estado | Salida con totales pegada | TODO |
| H8.S1.M3 | Gate: todo paso de un flujo apunta a un `operationId` existente | Falla con un paso huérfano | Dos corridas pegadas | TODO |
| H8.S2.M1 | Árbol del flujo aporte → mora → recargo → cobertura → recuperación | Casos válidos, límite e inválidos con su código de error | Validador → exit 0 | TODO |
| H8.S2.M2 | Árbol del flujo entrega del turno (normal y de cierre) | Ídem | Validador → exit 0 | TODO |
| H8.S2.M3 | Árbol del flujo recarga y retiro de billetera | Ídem | Validador → exit 0 | TODO |
| H8.S3.M1 | Prueba de integración del flujo de H8.S2.M1, contra Postgres y con Datafaker | Saldos finales = esperado, asientos cuadrados | Test nombrado en verde | TODO |
| H8.S3.M2 | Prueba de integración del flujo de H8.S2.M2 | Ídem | Test nombrado en verde | TODO |
| H8.S3.M3 | Prueba de integración del flujo de H8.S2.M3 | Ídem | Test nombrado en verde | TODO |
| H8.S3.M4 | Pruebas unitarias o web de los endpoints sin test en la matriz, limitadas a los servicios de dinero (`aportes`, `garantia`, `entregas`, `nucleo-financiero`); el resto pasa a un trabajo siguiente (D8) | Matriz sin huecos en esos cuatro | `scripts/matriz_de_pruebas.py --servicio …` pegado | TODO |

## H9 — Regresión, gates y entrega

**CA:** Todo lo anterior en verde junto, y PRs a `dev` y a `test` mergeados (memoria `feedback_prs_dev_y_test`).
**DoD:** Suite del backend y del backoffice en serie, `gh pr checks` sin rojos, gate de seguridad (sección 90.6)
y `REPORTE.md`.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H9.S1.M1 | Regresión del backend | Exit 0 | `./gradlew test` pegado | TODO |
| H9.S1.M2 | Regresión del backoffice (unit, a11y y lint) | Exit 0, o rojos preexistentes demostrados (memoria `a11y-backoffice-vence-plazo`) | Salidas pegadas | TODO |
| H9.S1.M3 | Gate de seguridad y privacidad: amenaza, control, test negativo y riesgo residual | Sección completa en el reporte | Revisión contra 90.6 | TODO |
| H9.S1.M4 | PR a `dev` y PR a `test` | CI en verde y mergeados | `gh pr checks` pegado | TODO |

## Hallazgos durante la ejecución

- **H1-02 (dinero):** el tipo de billetera `FONDO_GARANTIA` existe (`sql/10_tablas/10_billetera_custodia/cuenta_billetera.sql:24`),
  pero ningún seeder ni ningún código la crea. Los asientos que propone `garantia` entran al mayor solo con
  `consumo-de-asientos.habilitado=true` (apagado por omisión, `nucleo-financiero/application.yml`). Hoy no existe
  ningún camino de dinero hacia el fondo. Para el recargo se agregan H1.S2.M0c, M0d, M1b y M1c.
  `APORTE_FONDO_GARANTIA` tiene el mismo problema y queda fuera de alcance (riesgo en el reporte).
- **Desvío (2026-10-16):** el primer diseño de M0c (una billetera `FONDO_GARANTIA` por grupo) violó R-BIL-05
  (`sql/40_reglas/restricciones.sql:251-257`: una billetera que no es `USUARIO` ni `GRUPO` no lleva `grupo_id`).
  Rompió 7 tests existentes de apertura (FAIL observado), se revirtió y volvieron a verde (9/9 y 2/2). El modelo
  define el fondo como **una billetera de sistema por moneda**; el desglose por grupo vive en
  `garantia.fondo_garantia` y `garantia.movimiento_fondo` (M1c).
- **A6 (supuesto):** el recargo se atribuye al fondo **del grupo** (`ambito POR_GRUPO`, el único sembrado:
  `seeders/dev/04-fondo-y-cuenta-del-grupo.json`). El fondo `MUTUAL_PLATAFORMA` no tiene datos ni billetera; si un
  grupo usa el mutual, el recargo se rechaza y se compensa (M1b) hasta que se defina. Confirmar con: Pablo.

## Riesgos y bloqueos previstos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| El trabajo es muy grande para una sola sesión | Cierre parcial | Se cierra por hito con reporte; el orden H0 → H1 → H2 → H3 → H4 → H5 → H6 → H7 → H8 → H9 deja valor en cada corte |
| Alguna operación de dinero no genera asiento hoy (H0.S1.M1) | El KPI no puede salir del mayor | Microtarea nueva en el plan; mientras tanto el KPI responde `null` + `NO_IMPLEMENTADO`, nunca un `SUM` sobre pagos |
| RAM del VPS de Contabo con el Collector y Prometheus | Inestabilidad en TEST | Límites de memoria en el compose, retención de Prometheus a 15 días, `memory_limiter` |
| Cardinalidad de métricas por ruta cruda | Prometheus explota | Solo ruta plantilla (H6.S1.M3) |
| PII en atributos de trazas | Violación de la regla 90.2.1 | Redacción en la app y en el Collector, más un escaneo en la evidencia |
| Narrativas de ~300 tablas | Mucho volumen | Una microtarea por módulo; el gate impide declarar avance sin texto |
| Inversiones con aliado simulado | El saldo invertido no es real | Se muestra como «dato sintético» (D12) |
| «Intereses cobrados» se confunde con los intereses de inversión (`inversiones.distribucion_interes`, CU-126) | Dos números con el mismo nombre | El KPI se llama «Intereses por mora cobrados»; los intereses de inversión, si se piden, son otro KPI |
