# Auditoría — aportes-nucleo-financiero (lote L1, solo lectura)

> Escrito en tandas (indicación de Pablo, 2026-11-20): cada hallazgo se catalogó al identificarse y se subió a la rama. Lectura repartida en 4 subagentes de solo lectura; **cada hallazgo de este archivo se re-abrió y verificó en el código en la sesión principal** (regla 70.4.8). Lo que no se re-abrió se dice explícitamente en su ficha.

## 1. Ficha
- Módulo: `aportes-nucleo-financiero` — `servicios/aportes`, `servicios/nucleo-financiero`, sus tablas, pactos y pantallas de cobro/billetera.
- Responsable: Pablo (dueño del producto) · Fecha: 2026-11-20
- Commit auditado: `origin/test` @ `07fc69719287b0dadd152d0293a054948df69ead`. `origin/dev` @ `850e0f51` **no difiere** en `servicios/aportes` ni `servicios/nucleo-financiero` (`git diff --stat origin/test origin/dev -- servicios/aportes servicios/nucleo-financiero` → vacío).
- Historial: completo (`git fetch --unshallow`; `--is-shallow-repository` → `false`).
- Archivos/líneas cubiertos: 418 archivos y 53.610 líneas en la huella de los dos servicios. Main (203 archivos, 21.924 líneas): leídos completos, salvo los dos OpenAPI (parciales). Pruebas (215 archivos): inventariadas por grep, no leídas línea a línea. Más SQL, pactos y pantallas listados en la sección 3.

## 2. Resumen
El módulo **no está en condiciones de operar con dinero de terceros bajo las reglas del 2026-11-20**. El defecto central es de diseño: el cobro de un aporte se acredita en el momento en que el cliente lo declara (`CU21CobrarAporte`), con canal, monto y referencia elegidos por el propio cliente, y el webhook de la pasarela (`CU100`) solo archiva el aviso. Para el canal QR, `nucleo-financiero` no mueve dinero ni asienta en el mayor, pero la cuota queda `PAGADO` y cuenta como «confirmado» para la entrega del turno. A eso se suman: el registro contable de la cobertura empresarial, encendido en el despliegue de TEST (contradice R-NUEVA-2); unos «cuatro ojos» del reverso que se falsifican escribiendo un UUID; un reembolso negativo que la base no rechaza; y un libro de billetera que no se refleja en el mayor. La parte bien hecha es sólida y conviene preservarla (sección 9): el tipo `Dinero`, la verificación HMAC, la idempotencia con índices únicos, el outbox, los consumidores idempotentes y el mayor append-only.

| Clase | S1 | S2 | S3 | S4 | Total |
|---|---:|---:|---:|---:|---:|
| ACOPLE | 0 | 1 | 0 | 0 | 1 |
| CONFIG | 0 | 0 | 0 | 1 | 1 |
| CONTRATO | 0 | 0 | 1 | 0 | 1 |
| DATOS | 0 | 1 | 0 | 0 | 1 |
| DEBIDO-PROCESO | 0 | 1 | 0 | 0 | 1 |
| DEFECTO | 0 | 4 | 1 | 0 | 5 |
| DINERO | 1 | 2 | 3 | 0 | 6 |
| DUP-INT | 0 | 0 | 1 | 0 | 1 |
| FLUJO-QR | 4 | 1 | 0 | 0 | 5 |
| HUECO | 0 | 5 | 1 | 0 | 6 |
| OBSERV | 0 | 0 | 1 | 0 | 1 |
| PRUEBAS | 0 | 1 | 0 | 0 | 1 |
| REESCRITURA | 0 | 0 | 1 | 0 | 1 |
| RESILIENCIA | 0 | 0 | 1 | 0 | 1 |
| RESPALDO | 1 | 2 | 2 | 0 | 5 |
| SEG | 1 | 2 | 3 | 0 | 6 |
| UX-A11Y | 1 | 0 | 1 | 0 | 2 |
| **Total** | 8 | 20 | 16 | 1 | 45 |

**Los diez más graves**
1. `aportes-nucleo-financiero-FLUJO-QR-001` (S1) — El aporte se da por cobrado por declaración del cliente, sin confirmación de la pasarela
2. `aportes-nucleo-financiero-FLUJO-QR-002` (S1) — El webhook de la pasarela no confirma nada: solo archiva
3. `aportes-nucleo-financiero-FLUJO-QR-003` (S1) — El «confirmado» que usa la entrega del pozo sale de `monto_pagado`, no del mayor ni de la pasarela
4. `aportes-nucleo-financiero-FLUJO-QR-004` (S1) — Con canal distinto de billetera, `nucleo-financiero` no mueve dinero pero la cuota ya quedó pagada
5. `aportes-nucleo-financiero-DINERO-001` (S1) — Un reembolso con importe negativo pasa todas las validaciones y, al aprobarse, sube lo pagado de la cuota
6. `aportes-nucleo-financiero-UX-A11Y-001` (S1) — La app ofrece declarar un aporte por QR, transferencia o tarjeta escribiendo la referencia de un comprobante
7. `aportes-nucleo-financiero-SEG-005` (S1) — Los «cuatro ojos» del reverso de billetera se cumplen escribiendo cualquier UUID en el cuerpo
8. `aportes-nucleo-financiero-RESPALDO-003` (S1) — El consumidor de asientos que registra la cobertura empresarial está encendido en el despliegue de TEST
9. `aportes-nucleo-financiero-SEG-001` (S2) — En TEST el secreto del webhook de aportes es un literal público del repositorio
10. `aportes-nucleo-financiero-RESPALDO-001` (S2) — Canales `FONDO_GARANTIA` y `EFECTIVO_AL_ORGANIZADOR` aceptados para cobrar un aporte

**Peldaño de evidencia por área (regla 30).** Backend `aportes`: `DISCOVERED`. Backend `nucleo-financiero`: `DISCOVERED`. SQL: `DISCOVERED`. Pantallas: `DISCOVERED`. Verificadores Python del repo: ejecutados (salidas en la sección 7). Pruebas Java, Flutter y Angular: `BLOCKED` por entorno (sección 11). Ningún hallazgo se ejecutó; las confianzas «confirmado» lo son por lectura de las líneas citadas.

## 3. Huella y mapa de conexiones
**Huella** (418 archivos, 53.610 líneas en `servicios/aportes` y `servicios/nucleo-financiero`; 203 archivos y 21.924 líneas son `src/main`):
- `servicios/aportes`: 77 archivos de main (≈6.200 líneas, OpenAPI de 808 líneas) y 52 de pruebas (≈7.400).
- `servicios/nucleo-financiero`: 126 de main (≈15.750, OpenAPI de 1.547) y 151 de pruebas (≈23.400).
- SQL: `sql/10_tablas/03_aportes_pagos_qr/` (23 tablas; las del mayor `asiento_contable`, `movimiento_contable`, `cuenta_contable` y `cierre_diario` son de `nucleo-financiero`); `sql/10_tablas/10_billetera_custodia/` (27 tablas); sus entradas en `20_claves`, `30_indices`, `35_append_only`, `40_reglas`, `60_semillas/22-cuentas-de-sistema.sql`, `61_dev/09-…`.
- Pactos: `contratos/pactos/aportes-nucleo-financiero-aporte-cobrado.json`, `aportes-nucleo-financiero.json`, `grupos-aportes-grupo-iniciado.json`, `nucleo-financiero-entregas.json`, `nucleo-financiero-garantia-aporte-aplicado.json`, `nucleo-financiero-garantia-transaccion-reversada.json`, `inversiones-nucleo-financiero.json`.
- Catálogo de flujos: `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml`, `recarga-y-retiro.yaml`.
- Clientes: `apps/movil/lib/pantallas/billetera/**`, `apps/movil/lib/pantallas/pasanaku/{pantalla_aportar,aporte_registrado,tarjeta_de_aporte,pantalla_mi_estado,…}.dart`, `apps/backoffice/src/app/rutas/operacion/{retiros,custodia,billetera,desembolsos,cierres,reversos}`, `apps/web/src/app/paginas/inicio/secciones/*` y `verificadores/simulador-de-costos.ts`. `clientes/{angular,dart}` de estos servicios **no están versionados** (`.gitignore:50-51`): se compararon contra el OpenAPI y contra su uso.
- `packages/simulado`: no llega a builds de producción (el móvil rechaza un gateway loopback en release; el backoffice solo lo reemplaza en `development`/`demo`).

**Saltos del dinero (98.8.1).**
1. App → gateway → `aportes` `POST /aportes/obligaciones/{id}/pagos` (CU21): registra `pago` `ACREDITADO`, sube `monto_pagado` y emite `aportes.aporte_cobrado` por outbox.
2. `aportes` → `grupos` `GET /grupos/obligaciones/admisibilidad` (síncrono, con timeout y circuito; antes de la transacción).
3. Kafka `aportaya.aportes.aporte_cobrado` → `nucleo-financiero` `ConsumoDeAportes`: si el canal es `BILLETERA_MOVIL`, debita la billetera del usuario, acredita la del grupo y asienta `SALDO_USUARIOS → SALDO_GRUPOS` (o el fondo, si es `RECARGO_MORA`), y publica `aporte_aplicado`. Con otro canal no hace nada. Si el saldo no alcanza, publica `aporte_no_aplicado`.
4. Kafka `nucleo_financiero.aporte_no_aplicado` → `aportes` `CU21CompensarAporte`: el pago pasa a `REVERSADO` y la cuota vuelve a pendiente.
5. Pasarela → `aportes` `POST /pagos/webhooks/{codigo}` (CU100): verifica la firma y archiva. **Sin efecto sobre el pago.**
6. `entregas` → `aportes` `GET /aportes/periodos/{id}/recaudo` (`ENTREGA_EJECUTAR`): `confirmado` / `cubiertoMutual` / `faltante` desde `monto_pagado`.
7. `garantia` → Kafka `cobertura_empresarial_*` / `respaldo_*` → `nucleo-financiero` `ConsumidorDeAsientosKafka` (encendido en el compose de Coolify).

**Mapa de conexiones**
| Desde → hacia | Mecanismo | Punto exacto | Observación |
|---|---|---|---|
| aportes → grupos | HTTP | `CalendarioDelPeriodoPorHttp`, `HechosDeGruposPorHttp` | timeout y circuito (`ClienteConCircuito`) |
| grupos → aportes | evento `grupo_iniciado` | `ConsumidorDeGrupoIniciadoKafka` | idempotente; pacto `grupos-aportes-grupo-iniciado.json` |
| aportes → nucleo | evento `aporte_cobrado` | outbox CU21 → `ConsumidorDeAportesKafka` | pacto verificado campo a campo (lote C) |
| nucleo → aportes | evento `aporte_no_aplicado` | `ConsumidorDeAportesNoAplicadosKafka` | sin DLQ; NO_COMPENSABLE solo deja `warn` (HUECO-002) |
| nucleo → aportes | HTTP | `RecaudoDelDiaPorHttp` (`/aportes/recaudo-diario`), `HechosPorHttp` (`/aportes/participantes/{p}/estado`) | timeout sí, circuito no; sin pacto |
| entregas → aportes | HTTP | `GET /aportes/periodos/{id}/recaudo` | fuente de «confirmado» (FLUJO-QR-003) |
| grupos (CU-60) → aportes | HTTP | `GET /aportes/grupos/{id}/primera-cuota` | habilita el sorteo |
| nucleo → grupos | HTTP | `HechosPorHttp` (alias, participaciones, actividad) | alias concatenado sin codificar (sospecha, lote C) |
| nucleo → tarifas | HTTP | `CotizadorPorHttp` | `costoDe` con circuito; `cotizar`/`aceptar` sin circuito |
| nucleo → entregas | HTTP | `OfertasPorHttp` | sin circuito |
| entregas/garantia → nucleo | eventos de asiento | `ConsumidorDeAsientosKafka` | RESPALDO-003 |
| nucleo → garantia | eventos `aporte_aplicado`, `transaccion_reversada` | pactos `nucleo-financiero-garantia-*` | — |
| identidad/grupos → nucleo | eventos `usuario_registrado`, `grupo_creado` | `ConsumidorDeUsuariosKafka`, `ConsumidorDeGruposKafka` | sin pacto (lote C) |
| inversiones/entregas → nucleo | HTTP servicio-a-servicio | `/billetera/retenciones/*`, `/billetera/inversiones/*`, `pago-a-tercero` | identidad de servicio (`IdentidadDeServicio`) |
| erp, cumplimiento → tablas de nucleo | SQL entre esquemas | ver ACOPLE-001 | prohibido por 98.1.1 |
| nucleo/aportes → otros esquemas | FK | `sql/20_claves/03_aportes_pagos_qr.sql` | ACOPLE-001 |
| aportes → `catalogo.dia_no_habil`, `comun.bitacora_evento` | SQL | `FeriadosRepositorio.java:29,44`, `AuditoriaRepositorio.java:44` | catálogo y bitácora compartidos y declarados |

## 4. Distancia al flujo nuevo
### a. Inventario de aprobaciones (14; 9 DECISIÓN REQUERIDA)
| ID | Dónde | Qué aprueba y quién | Estado · endpoint · permiso · pantalla · evento | Clasificación | Qué se rompe si se quita |
|---|---|---|---|---|---|
| AP-01 | `AportesController.java:126-169`; `pantalla_aportar.dart:23-161` | La persona **declara** su pago y el servidor lo da por cobrado | `POST /aportes/obligaciones/{id}/pagos` · `BILLETERA_OPERAR` · `pago.es_manual`, `registrado_por` · `aporte_cobrado` | ELIMINABLE (sustituir por QR + webhook) | La app pierde su único camino de pago, que hoy no está enlazado (HUECO-003) |
| AP-02 | `AportesController.java:236-242` | Un administrador genera las cuotas de cada periodo | `POST /aportes/periodos/{id}/obligaciones` · `ADMIN_PLATAFORMA` · `obligaciones_generadas` | ELIMINABLE (automatizar) | Nada, si existe el disparador automático |
| AP-03 | `PagosController.java:76`; `CU19ReembolsarPago.java:118-134` | Segunda persona aprueba un reembolso | `POST /aportes/reembolsos/{id}/aprobacion` · `REVERSO_AUTORIZAR` · `reembolso` `SOLICITADO→EJECUTADO` · `reembolso_ejecutado` | DECISIÓN REQUERIDA (A-1) | Los reembolsos se ejecutan sin segregación de funciones |
| AP-04 | `PagosController.java` (`registrarDisputa`); `CU19:191` | Soporte abre una disputa | `POST /aportes/pagos/{id}/disputas` · `SOPORTE` · `disputa_abierta` | DECISIÓN REQUERIDA (A-1) | Sin canal de reclamo (posible obligación de protección al consumidor: norma a identificar por Cumplimiento) |
| AP-05 | `PagosController.java:113-129`; `CU99EnrutarProveedor` | Un administrador da de alta y activa un proveedor de pago | `POST /aportes/pagos/proveedores` · `ADMIN_PLATAFORMA` · `proveedor_activado` | DECISIÓN REQUERIDA (configuración, fuera del circuito por grupo) | Ninguna pasarela operativa |
| AP-06 | `comprobante_manual.sql:9-15`; `plan_regularizacion.sql:11`; `obligacion_aporte.sql:31` (`REPORTADO_POR_USUARIO`, `EN_VERIFICACION`) | Revisión humana de comprobantes y planes de pago | Solo esquema: sin código Java (`grep`) | ELIMINABLE | Nada en código; limpiar esquema y semillas |
| AP-07 | `ConsumidorDeAportesNoAplicadosKafka` (rama NO_COMPENSABLE) | «Una persona de operaciones» resuelve el aporte no compensable | Sin endpoint, solo `warn` | ELIMINABLE (desaparece con FLUJO-QR-001) | — |
| AP-08 | `SolicitudDeRetiro.java:122`; `ResolucionDeRetiro.java:188-290`; `FondeoDeLaBilletera.java:182`; `application.yml:61`; `restricciones.sql:1529-1535`; backoffice `operacion/retiros/*` | Segunda persona aprueba retiros de 5000 o más (ADR-049) | `orden_retiro` `EN_REVISION` · `POST /billetera/retiros/{id}/aprobacion` · `RETIRO_APROBAR` · `retiro_autorizado`/`retiro_rechazado` · `ck_retiro_doble_aprobacion` | DECISIÓN REQUERIDA (A-1) | El `CHECK` de la base rechaza estados pagables sin `aprobada_por` |
| AP-09 | `CU14ReversarTransaccion.java:70-74`; `BilleteraController.java:230-236` | «Autorizador» del reverso (falsificable, SEG-005) | `POST /billetera/reversos` · `REVERSO_AUTORIZAR` · `transaccion_reversada` | DECISIÓN REQUERIDA (A-1) | `garantia` consume `transaccion_reversada` |
| AP-10 | `BilleteraController.java:142-149` | Tesorería dispara la acreditación de una recarga | `POST /billetera/recargas/{id}/acreditacion` · `TESORERIA` · `recarga_acreditada` | ELIMINABLE (sustituir por webhook) | Sin webhook, ninguna recarga se acredita |
| AP-11 | `CU17BloquearPorAutoridad.java:62,139` | Operador registra y levanta un bloqueo judicial | `bloqueo_saldo` | DECISIÓN REQUERIDA (A-1; norma a identificar por Cumplimiento) | Incumplimiento de orden de autoridad |
| AP-12 | `CU16CerrarBilletera.java:59,135`; `CierreRepositorio.java:60-68` | Cierre de billetera en dos pasos | `solicitud_cierre_billetera` `SOLICITADA→APROBADA` | DECISIÓN REQUERIDA (A-1) | — |
| AP-13 | `CierreDiarioDesdeContabilidad.java:39`; `CU50ConciliarCustodia.java:133`; `OrdenRetiroRepositorio.java:264` | Contabilidad cierra el día; Tesorería concilia la custodia (el encaje frena retiros) | `CONTABILIDAD`, `TESORERIA` | DECISIÓN REQUERIDA (A-1) | El control de encaje queda sin dato (ver DEFECTO-005) |
| AP-14 | `CU11RetirarSaldo.java:107`; `RetirosConProveedor.java:66` | Quién instruye el pago de un retiro autorizado (sin disparador, A9) | — | DECISIÓN REQUERIDA (A-1) | Hoy ningún retiro sale (HUECO-004) |

### b. Inventario de respaldo y cobertura (9 + 1 que no aplica; 5 DECISIÓN REQUERIDA)
| ID | Dónde | Qué pone la empresa y cuándo | Asiento en el mayor | Clasificación | Qué depende de ello |
|---|---|---|---|---|---|
| RS-01 | `RecaudoDelPeriodo.java:14-29,71`; `AportesController.java:220-221`; `aportes.yaml:381-383` | Calcula el `faltante` que «se le pide al respaldo empresarial» | Ninguno en `aportes` | DECISIÓN REQUERIDA (A-2) | `entregas` (L2) |
| RS-02 | `SaldoDeLaObligacion.java:16-19`; `obligacion_aporte.sql:29-31` (`monto_cubierto_garantia`, `CUBIERTO_POR_GARANTIA`, `REPOSICION_COBERTURA`, `APORTE_FONDO_GARANTIA`) | Lo cubierto por garantía cuenta como pagado | Ninguno en `aportes` (nadie del módulo escribe la columna; el escritor sería otro servicio, sin confirmar) | DECISIÓN REQUERIDA (A-3) | Recaudo, estado del participante |
| RS-03 | `aportes.yaml:603-606`; `pago.sql:27` | Canal `FONDO_GARANTIA` para pagar una cuota | Ninguno (canal sin billetera, FLUJO-QR-004) | CONTRADICE R-NUEVA-2 | Contrato público (RESPALDO-001) |
| RS-04 | `ConsumidorDeAsientosKafka.java:44-56`; `ConsumoDeAsientos.java:31-33`; `docker-compose.coolify.yml:603-611` | Asienta `cobertura_empresarial_aplicada/_reversada` y `respaldo_*` cuando `garantia` cubre | `RESERVA_RESPALDO`/`CAJA_EMPRESA` → `POZO_GRUPO` (cuentas 9.x) | CONTRADICE R-NUEVA-2 | `garantia`, `entregas`; RESPALDO-003 |
| RS-05 | `application.yml:88-112`; `sql/60_semillas/22-cuentas-de-sistema.sql:15-33` | Cuentas de caja de la empresa y reserva de respaldo | Plan de cuentas | CONTRADICE R-NUEVA-2 (A-3 para lo ya asentado) | RESPALDO-005 |
| RS-06 | `DestinoDelCobro.java:17-19,43` | El recargo de mora fondea la reserva de respaldo | `SALDO_USUARIOS → RESERVA_RESPALDO` | DECISIÓN REQUERIDA (A-4) | Catálogo `aporte-mora-cobertura.yaml:81-87`; `CUConsumoDeRecargoTest` |
| RS-07 | `ConsumoDeAportes.java:190` | Si la cuenta del participante `permiteNegativo`, el pozo se acredita sin saldo (adelanto) | `SALDO_USUARIOS → SALDO_GRUPOS` con saldo negativo | DECISIÓN REQUERIDA (por omisión `FALSE`, `cuenta_billetera.sql:19`) | — |
| RS-08 | `transaccion_billetera.sql:28,31`; `asiento_contable.sql:19`; `sql/61_dev/09-mora-cobertura-y-cobranza.sql:39,49-59` | Tipos y orígenes `COBERTURA_*`; semilla de desarrollo con el fondo pagando al grupo | Origen `COBERTURA` | DECISIÓN REQUERIDA (A-3) | Semillas de desarrollo |
| RS-09 | `apps/web/src/app/paginas/inicio/secciones/producto.ts:75`; `app.routes.ts:117` (+ `cierre.ts:71`, `transparencia.ts:65`, `hero.ts:31`) | Promesa pública: «el fondo cubre la entrega» | — | CONTRADICE R-NUEVA-2 | Marketing (RESPALDO-002) |
| RS-10 | `CU26EjecutarAval`, `CU23*` | Aval entre participantes / fondo mutual | — | NO APLICA en este módulo (viven en `garantia`, L2) | — |

### c. Ambigüedades
| ID | Qué muestra hoy el código de este módulo | Supuesto de trabajo | A quién confirmar |
|---|---|---|---|
| A-1 | Nueve controles humanos (AP-03, 04, 05, 08, 09, 11, 12, 13, 14), entre ellos la doble aprobación de retiros (ADR-049) y el bloqueo por autoridad | No se asume que se eliminan; se listan como DECISIÓN REQUERIDA | Pablo; Cumplimiento para AP-08 y AP-11 |
| A-2 | `aportes` calcula `faltante` y `cubiertoMutual` (RS-01); `nucleo` asienta la cobertura empresarial si llega (RS-04). La rama «falta plata y nadie cubre» no existe en este módulo | No se inventa: DECISIÓN REQUERIDA | Pablo |
| A-3 | Esquema, semillas y consumidor de respaldo vivos (RS-02, 04, 05, 08); aval fuera del módulo | El aval entre participantes no entra en R-NUEVA-2 | Pablo |
| A-4 | El recargo se calcula automáticamente (DEBIDO-PROCESO-001) y va a la reserva de respaldo (RS-06); la mora no transita por tiempo (DEFECTO-003) | Sin decisión, el destino del recargo queda indefinido | Pablo; Cumplimiento (tasas y recargos son parámetros financieros, regla 97.5.4) |

### d. Flujo del faltante (FLUJO-FALTANTE)
Ramas que el código de este módulo tiene hoy cuando al corte falta plata: (1) `aportes` informa `faltante = pozo − confirmado − cubiertoMutual` a `entregas` — VIGENTE como cálculo, pero «confirmado» no es caja (FLUJO-QR-003); (2) `nucleo` asienta la cobertura empresarial que publique `garantia` — CONTRADICE R-NUEVA-2 (RS-04); (3) entrega parcial, diferida, devolución o reasignación — **no existen en este módulo**: DECISIÓN REQUERIDA (A-2). Lo que decide `entregas` con el faltante se audita en L2.

### e. Qué queda inconsistente si la operación falla a mitad (98.8.6)
- Cobro con canal billetera y saldo insuficiente: la cuota queda `PAGADO` hasta que llega y se procesa `aporte_no_aplicado` (ventana = latencia del outbox + Kafka + consumidor). Si la compensación es `NO_COMPENSABLE`, queda inconsistente **sin plazo** (HUECO-002).
- Cobro con canal QR: inconsistente **para siempre** respecto del mayor (FLUJO-QR-004).
- Consumidor de `nucleo` con rechazo «reintentable» (cuenta inexistente o no operativa): lanza la excepción y, agotados los reintentos de Kafka, el evento se descarta (lote B, `ConsumoDeAportes.java:169-188,247-249`; sospecha, falta ver la configuración de DLT): cuota pagada sin aplicar y sin compensación.

## 5. Hallazgos

### aportes-nucleo-financiero-FLUJO-QR-001 — El aporte se da por cobrado por declaración del cliente, sin confirmación de la pasarela
- Clase / Severidad / Confianza: FLUJO-QR (también DINERO, SEG) / **S1** / confirmado
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/web/AportesController.java:126-169`; `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CobrarAporte.java:58-172`; `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/PagoRepositorio.java:42`; `servicios/aportes/src/main/resources/openapi/aportes.yaml:596-608`
- Evidencia:
  ```java
  // AportesController.java:143-156 — el cuerpo del cliente decide canal, monto, referencia y esManual
  var salida = cu21.acreditar(new CU21CobrarAporte.EntradaCobro(idempotencyKey.toString(), obligacionId,
          MapeoDeAportes.dinero(cuerpo.getMonto()), …, cuerpo.getCanal().getValue(),
          cuerpo.getReferenciaProveedor(), Optional.ofNullable(cuerpo.getProveedorId()),
          Boolean.TRUE.equals(cuerpo.getEsManual()), admisibilidad.periodoAbierto()), ctx);
  // PagoRepositorio.java:42 — el pago nace acreditado
  .set(DSL.field("estado", String.class), "ACREDITADO")
  // CU21CobrarAporte.java:127-128,134-137 — la obligación sube monto_pagado y se emite aporte_cobrado
  obligaciones.acreditar(dsl, obligacion.id(), entrada.monto(), estadoNuevo, …);
  outbox.emitir(dsl, new EventoDominio("aportes.aporte_cobrado", …
  ```
  El contrato acepta `canal` ∈ `[BILLETERA_MOVIL, COMPENSACION_INTERNA, EFECTIVO_AL_ORGANIZADOR, FONDO_GARANTIA, QR_INTEROPERABLE, TARJETA, TRANSFERENCIA_BANCARIA]` (`aportes.yaml:603-606`) del propio cuerpo de la petición (permiso `BILLETERA_OPERAR`, titular verificado vía `grupos`).
- Cómo se comprobó: lectura completa de `CU21CobrarAporte.java`, del método `cobrarAporte` del controlador y de `CU100RecibirWebhookPasarela.java`; `grep -n '"estado"' PagoRepositorio.java`. El propio javadoc de CU100 lo declara: «`CU21CobrarAporte` acredita de forma síncrona» (`CU100RecibirWebhookPasarela.java:43-49`). No ejecutado (Gradle bloqueado, ver Límites).
- Impacto: un participante (o un cliente modificado) que envía `canal=QR_INTEROPERABLE` con una referencia cualquiera deja su cuota `PAGADO` sin que exista dinero. Esa cuota cuenta como «confirmado» para la entrega del turno (ver FLUJO-QR-003). El beneficiario y el grupo reciben un pozo que no existe en caja; la empresa queda con un descalce que, con el respaldo vigente, cubriría ella (R-NUEVA-2).
- Causa de fondo: falta la cadena `orden_cobro → intento_pago → webhook` (tablas existen en `sql/10_tablas/03_aportes_pagos_qr/orden_cobro.sql`, `intento_pago.sql`; sin código que las use, ya señalado en `docs/auditoria-produccion/idempotencia-scope.md` como «NO IMPLEMENTADO» y **sigue vigente** en este commit).
- Solución de fondo: el endpoint de cobro solo crea una `orden_cobro` (estado pendiente, QR emitido); el paso a `ACREDITADO`, el incremento de `monto_pagado` y la emisión de `aporte_cobrado` ocurren **únicamente** en el procesamiento del webhook con firma verificada. El canal lo fija el servidor, nunca el cliente. Para `BILLETERA_MOVIL` el débito lo confirma `nucleo-financiero` antes de marcar pagado.
- Relación con el flujo nuevo: FLUJO-QR (contradice R-NUEVA-1 y regla 91.3.1)
- Conexiones y módulos afectados: `nucleo-financiero` (consumidor `aporte_cobrado`), `entregas`/`garantia` (consultan recaudo), `grupos` (sorteo exige primeras cuotas `ACREDITADO`, ADR-054 D3), app móvil.
- Estándares y normas: ISO/IEC 25010 Seguridad (integridad, no repudio) y Adecuación funcional (corrección); CWE-345 (verificación insuficiente de la autenticidad de los datos); OWASP API Security Top 10 (lógica de negocio). Reglas 91.3.1, 91.1.5, 96.2.
- Esfuerzo estimado: grande

### aportes-nucleo-financiero-FLUJO-QR-002 — El webhook de la pasarela no confirma nada: solo archiva
- Clase / Severidad / Confianza: FLUJO-QR (también HUECO) / **S1** / confirmado
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU100RecibirWebhookPasarela.java:147-166`
- Evidencia:
  ```java
  var pago = pagoOpt.get();
  Dinero montoDelEvento = montoDe(evento);
  if (montoDelEvento == null || !montoDelEvento.equals(pago.monto())) { … "DESCARTADO" … }
  webhooks.marcarProcesado(dsl, webhookId, "PROCESADO", null, pago.id(), ahora);
  return Resultado.procesado(webhookId, pago.id());
  ```
- Cómo se comprobó: lectura completa del caso de uso (224 líneas). No hay transición del `pago`, ni de la `obligacion_aporte`, ni evento: el único efecto es la fila en `webhook_pasarela`. Tampoco se distingue el tipo de evento (pago fallido, reversado, contracargo): `tipoEvento` solo se guarda (`:141`).
- Impacto: el único mecanismo que según R-NUEVA-1 debe mover el flujo no mueve nada; un webhook de pago **rechazado** o **reversado** que coincida en monto se marca `PROCESADO` igual. Un pago que la pasarela nunca confirma queda `ACREDITADO` para siempre.
- Causa de fondo: el CU se construyó como control de seguridad transversal sobre un `pago` ya acreditado (javadoc `:43-49`, «simplificación de alcance, declarada»), no como el disparador del cobro.
- Solución de fondo: el webhook verificado es la transición `orden_cobro PENDIENTE → PAGADA` y dispara (en la misma transacción, por outbox) el registro del `pago` y `aporte_cobrado`; los eventos de fallo/reverso generan su transición inversa con contra-asiento.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: los mismos de FLUJO-QR-001.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (completitud); regla 91.3.1, 98.3.3.
- Esfuerzo estimado: grande

### aportes-nucleo-financiero-FLUJO-QR-003 — El «confirmado» que usa la entrega del pozo sale de `monto_pagado`, no del mayor ni de la pasarela
- Clase / Severidad / Confianza: FLUJO-QR (también DINERO 91.1.5) / **S1** / confirmado
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/web/AportesController.java:207-229`; `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/RecaudoRepositorio.java:16-18,33,58`; `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/RecaudoDelPeriodo.java:47-72`
- Evidencia: `RecaudoRepositorio.java:16-18` (javadoc): «`monto_pagado` solo lo … pago: por eso es la fuente de lo «confirmado». Contrastarlo contra el mayor es trabajo …»; el endpoint `consultarRecaudoDelPeriodo` (permiso `ENTREGA_EJECUTAR`) devuelve `confirmado`, `cubiertoMutual` y `faltante` calculados desde ese campo.
- Cómo se comprobó: `grep -rn "confirmado\|monto_pagado"` en `servicios/aportes/src/main` y lectura de las líneas citadas.
- Impacto: con FLUJO-QR-001, un aporte declarado por QR y nunca pagado se suma a la caja del pozo que `entregas` liquida. Con canal `BILLETERA_MOVIL` que luego falla por saldo (`aporte_no_aplicado`), hay una ventana hasta la compensación en la que el pozo aparece más lleno de lo real.
- Causa de fondo: dos fuentes de verdad (obligación de `aportes` vs. mayor de `nucleo-financiero`) sin conciliación en el camino de la entrega.
- Solución de fondo: el importe con el que se decide una entrega se lee del dueño del dinero (saldo de la cuenta del grupo en el mayor de `nucleo-financiero`, regla 98.4.2), o `monto_pagado` solo sube tras la aplicación confirmada en el mayor.
- Relación con el flujo nuevo: FLUJO-QR; alimenta FLUJO-FALTANTE (el `faltante` que dispara el respaldo).
- Conexiones y módulos afectados: `entregas` (lote L2), `garantia`.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (corrección); reglas 91.1.5, 98.4.2.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-FLUJO-QR-004 — Con canal distinto de billetera, `nucleo-financiero` no mueve dinero pero la cuota ya quedó pagada
- Clase / Severidad / Confianza: FLUJO-QR (también HUECO) / **S1** / confirmado
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ConsumoDeAportes.java:61-63,78,139-140`
- Evidencia:
  ```java
  // :61 «Alcance declarado: solo el canal BILLETERA_MOVIL sale de una billetera. Los demas canales (QR, …»
  if (!CANAL_BILLETERA.equals(aporte.canal())) {
      return Salida.sinEfecto(Resultado.CANAL_SIN_BILLETERA);
  }
  ```
- Cómo se comprobó: `grep -n` sobre el consumidor y lectura de las líneas.
- Impacto: un aporte por QR (el único canal que R-NUEVA-1 admite) no deja asiento en el mayor ni acredita la cuenta del grupo; `aportes` lo da por pagado. La caja del grupo en el mayor y el «confirmado» de `aportes` divergen por construcción.
- Causa de fondo: la contrapartida contable del QR (cuenta puente de custodia/pasarela → cuenta del grupo) no está implementada; el javadoc la remite a «conciliación y su propio asiento», que no existe en este flujo.
- Solución de fondo: el webhook verificado del QR genera el asiento `DEBE custodia/pasarela · HABER SALDO_GRUPOS` con clave de idempotencia derivada del evento de la pasarela.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: `aportes`, `entregas`.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (completitud); reglas 91.1.5, 91.6.1.
- Esfuerzo estimado: grande

### aportes-nucleo-financiero-SEG-001 — En TEST el secreto del webhook de aportes es un literal público del repositorio
- Clase / Severidad / Confianza: SEG (también CONFIG) / **S2** / probable (falta confirmar el valor de `PERFIL_SPRING` en Coolify; el repo dice `local`)
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/SecretoWebhookDeDesarrollo.java:18-26`; `docker-compose.coolify.yml:83-112` (servicio `aportes` sin variable del secreto); `despliegue/TEST.md:84-90,455-458`
- Evidencia: `@Profile({"local", "test"})` y `@Value("${aportaya.aportes.webhook.secreto-de-prueba:<literal por omisión>}")` (valor omitido aquí); `TEST.md:458`: «Se queda en `PERFIL_SPRING=local`».
- Cómo se comprobó: lectura del bean; `grep -rn "WEBHOOK_SECRETO"` (solo `notificaciones` lo recibe, `docker-compose.coolify.yml:555`); `auditar_backend.py` lo marca: «`aportaya.aportes.webhook.secreto-de-prueba` es un secreto y solo tiene valor por omision».
- Impacto: en el servidor de TEST cualquiera que lea el repo puede firmar webhooks válidos. Hoy el efecto es acotado porque el webhook no acredita (FLUJO-QR-002), pero en cuanto el webhook pase a ser el disparador del cobro, esto es acreditación sin pago. Regla 91.3.2: «La firma del webhook se valida siempre, incluso en desarrollo».
- Causa de fondo: el doble de la pasarela se confina por perfil, y TEST corre con el perfil de desarrollo.
- Solución de fondo: secreto obligatorio por entorno, inyectado por variable y sin valor por omisión (falla al arrancar si falta, regla 90.3.4); el doble con secreto fijo solo en pruebas automáticas.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: despliegue TEST (transversal, L5).
- Estándares y normas: ISO/IEC 25010 Seguridad (autenticidad); CWE-798 (credenciales embebidas); reglas 90.3.4, 91.3.2.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-RESPALDO-001 — Canales `FONDO_GARANTIA` y `EFECTIVO_AL_ORGANIZADOR` aceptados para cobrar un aporte
- Clase / Severidad / Confianza: RESPALDO (también DINERO RN-18) / **S2** / probable (falta ver si algún cliente los envía; lo revisa el lote de frontends)
- Ubicaciones: `servicios/aportes/src/main/resources/openapi/aportes.yaml:603-606`; `sql/10_tablas/03_aportes_pagos_qr/pago.sql:27`
- Evidencia: `CONSTRAINT ck_pago_canal CHECK (canal IN ('BILLETERA_MOVIL', 'COMPENSACION_INTERNA', 'EFECTIVO_AL_ORGANIZADOR', 'FONDO_GARANTIA', 'QR_INTEROPERABLE', 'TARJETA', 'TRANSFERENCIA_BANCARIA'))`
- Cómo se comprobó: lectura del contrato y del DDL.
- Impacto: un participante con `BILLETERA_OPERAR` puede declarar su cuota pagada «por el fondo de garantía» o «en efectivo al organizador». Lo primero contradice R-NUEVA-2; lo segundo, la regla 91.5 (el dinero del grupo no pasa por el organizador).
- Causa de fondo: el enum del contrato público replica el catálogo interno de canales del esquema.
- Solución de fondo: el contrato público solo admite el canal que el servidor emite (QR); los canales internos, si se conservan, solo por operaciones de sistema. La continuidad de `FONDO_GARANTIA` es DECISIÓN REQUERIDA (A-3).
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: `garantia` (L2), `organizador` (L3).
- Estándares y normas: ISO/IEC 25010 Seguridad (integridad); reglas 91.5, 90.1.5.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-CONTRATO-001 — `referenciaProveedor` admite 100 caracteres en el contrato y 80 en la base
- Clase / Severidad / Confianza: CONTRATO (también DATOS) / **S3** / confirmado por lectura (sin ejecución)
- Ubicaciones: `servicios/aportes/src/main/resources/openapi/aportes.yaml:607`; `sql/10_tablas/03_aportes_pagos_qr/pago.sql:18`
- Evidencia: `referenciaProveedor: { type: string, maxLength: 100 }` frente a `referencia_proveedor VARCHAR(80) NOT NULL`.
- Cómo se comprobó: lectura de ambas líneas.
- Impacto: una referencia de 81 a 100 caracteres pasa la validación y falla en el `INSERT` (error de base, probable 500 en lugar del error de contrato, regla 96.2.4).
- Causa de fondo: contrato y esquema mantenidos a mano por separado.
- Solución de fondo: alinear el límite desde la fuente de verdad del esquema y verificarlo en `verificar_contratos_limites.py`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: clientes generados.
- Estándares y normas: ISO/IEC 25010 Fiabilidad (tolerancia a fallos); RFC 9457; regla 96.2.4.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-DEFECTO-001 — El `recargo` del cuerpo se registra como comisión del proveedor y reduce el neto
- Clase / Severidad / Confianza: DEFECTO (también DDD) / **S2** / confirmado por lectura
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/web/AportesController.java:147-150`; `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CobrarAporte.java:107-109`; `sql/10_tablas/03_aportes_pagos_qr/pago.sql:12-13`
- Evidencia: el controlador pasa `cuerpo.getRecargo()` como `comision` de `EntradaCobro`; CU21 lo persiste en `monto_comision_proveedor` y calcula `entrada.monto().menos(entrada.comision())` como `monto_neto_acreditado`.
- Cómo se comprobó: lectura de ambas ubicaciones.
- Impacto: un cliente que manda un recargo reduce el neto acreditado del pago en ese importe, y el recargo de mora queda contabilizado como comisión de la pasarela; `monto_comision_proveedor` es `NUMERIC(10,2)` y el recargo viene sin tope de contrato. El campo lo controla el cliente.
- Causa de fondo: concepto del lenguaje ubicuo («recargo» vs. «comisión del proveedor») mapeado al campo equivocado.
- Solución de fondo: la comisión del proveedor la informa la pasarela en el webhook; el recargo es su propia obligación `RECARGO_MORA` y no viaja en el cobro de la cuota.
- Relación con el flujo nuevo: ninguna directa (A-4)
- Conexiones y módulos afectados: conciliación, reportes.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (corrección); regla 91.3.4.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-OBSERV-001 — Importes atribuibles a una persona en logs de nivel INFO
- Clase / Severidad / Confianza: OBSERV (también SEG) / **S3** / confirmado por lectura
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CobrarAporte.java:170`
- Evidencia: `BITACORA.info("CU-21 · Aporte cobrado registrado: pago={}, obligacion={}, monto={}, estado={}", pagoId, obligacion.id(), entrada.monto(), estadoNuevo)`
- Cómo se comprobó: lectura. No se comprobó si el enmascarador de PII de la configuración de logs cubre el campo `monto` (pendiente).
- Impacto: la regla 90.2.1 pide loguear identificadores, no importes atribuibles a una persona.
- Causa de fondo: log de depuración convertido en log de negocio.
- Solución de fondo: quitar el importe del log; si hace falta, que lo registre la auditoría con acceso controlado.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 Seguridad (confidencialidad); regla 90.2.1.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-DINERO-001 — Un reembolso con importe negativo pasa todas las validaciones y, al aprobarse, sube lo pagado de la cuota
- Clase / Severidad / Confianza: DINERO (también DATOS) / **S1** / probable (lectura completa del camino; falta ejecutarlo contra PostgreSQL, bloqueado por entorno)
- Ubicaciones: `servicios/aportes/src/main/resources/openapi/aportes.yaml:788-790`; `servicios/aportes/src/main/java/bo/aportaya/aportes/web/MapeoDeAportes.java:20-24`; `plataforma/comun-dominio/.../Dinero.java:36-45`; `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU19ReembolsarPago.java:88-99,141-148`; `sql/10_tablas/03_aportes_pagos_qr/reembolso.sql:8,17-18`; `sql/10_tablas/03_aportes_pagos_qr/obligacion_aporte.sql:17,30`
- Evidencia: `pattern: '^-?\d+\.\d{2}$'` (el esquema `Dinero` admite signo); `Dinero.de` solo valida escala, no signo; CU19: `if (entrada.monto().esMayorQue(disponible))` es falso para un negativo; `reembolso.monto NUMERIC(14,2) NOT NULL` sin `CHECK`; al aprobar, `obligacion.saldo().pagado().menos(reembolso.monto())` y `revertirPago` hace `monto_pagado - (monto)`; `monto_pagado` no tiene `CHECK` (solo `monto_esperado > 0`).
- Cómo se comprobó: lectura de cada línea citada; `grep -rn "signum\|negativ"` en `servicios/aportes/src/main/java` (solo `CU21AbrirPrimeraCuota.java:109`); `grep` de `reembolso` y `monto_pagado` en `sql/20_claves` y `sql/40_reglas` sin `CHECK` aplicable.
- Impacto: con una solicitud negativa (cualquier `BILLETERA_OPERAR`, ver SEG-002) y una aprobación de un segundo usuario con `REVERSO_AUTORIZAR`, la cuota queda con más `monto_pagado` del que entró y puede pasar a `PAGADO` sin dinero; `reembolso_ejecutado` viaja con importe negativo a sus consumidores. Viola 91.2.2 («nunca es negativo; el signo lo da la cuenta»).
- Causa de fondo: el tipo `Dinero` del contrato y del dominio no distingue importe (≥0) de saldo con signo, y la base no tiene la invariante.
- Solución de fondo: esquema de contrato sin signo para importes, `Dinero` (o un `Importe`) que rechace negativos en el borde, y `CHECK (monto > 0)` en `reembolso`, `CHECK (monto_pagado >= 0)` y equivalentes en `monto_neto_acreditado`, `monto_comision_proveedor`, `disputa_pago.monto_disputado`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: consumidores de `aportes.reembolso_ejecutado` (nucleo-financiero), `entregas` vía recaudo.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (corrección), Seguridad (integridad); CWE-20 (validación de entrada), CWE-839 (comparación numérica sin mínimo); reglas 91.2.2, 96.2.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-SEG-002 — Endpoints de aportes sin verificación de propiedad del recurso (BOLA)
- Clase / Severidad / Confianza: SEG / **S2** / confirmado por lectura (sin ejecución)
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/web/AportesController.java:80-93` (`consultarEstadoDelParticipante`, `BILLETERA_VER`), `:95-103` (`contarMorosos`), `:108-124` (`consultarPrimeraCuota`), `:207-229` (`consultarRecaudoDelPeriodo`, `ENTREGA_EJECUTAR`); `servicios/aportes/src/main/java/bo/aportaya/aportes/web/PagosController.java:56-72` → `CU19ReembolsarPago.java:76-99` (`solicitarReembolso`, `BILLETERA_OPERAR`); `sql/40_reglas/restricciones.sql:1410-1418` (RLS solo para tablas con `usuario_id`/`cuenta_billetera_id`)
- Evidencia: `@Permiso("BILLETERA_VER") public ResponseEntity<EstadoDelParticipante> consultarEstadoDelParticipante(UUID participanteId) { … estados.ejecutar(participanteId, sesion.actual()) …`; `ConsultarEstadoDelParticipante.java` no contiene comparación con el usuario de la sesión (`grep -n "usuarioId\|titular"` vacío); CU19 `solicitar` solo verifica que el pago exista (`:78`) antes de insertar.
- Cómo se comprobó: lectura de los métodos y de los casos de uso; `grep` de verificaciones de titularidad. El único endpoint que verifica propiedad es el cobro (`AportesController.java:133-141`, vía `grupos`).
- Impacto: cualquier participante con `BILLETERA_VER` lee deuda, total aportado y estado de mora de otro participante cambiando el id; cualquiera con `BILLETERA_OPERAR` solicita el reembolso de un pago ajeno (lo frena la aprobación de cuatro ojos, pero queda la solicitud y el evento `reembolso_solicitado`).
- Causa de fondo: autorización por rol sin chequeo a nivel de objeto; las tablas de `aportes` no tienen RLS por titular.
- Solución de fondo: verificación de pertenencia en cada endpoint (o RLS por `participante_id` resuelto desde la sesión) y matriz de autorización negativa en los tests web.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `grupos` (resolución participante→usuario), app móvil.
- Estándares y normas: ISO/IEC 25010 Seguridad (confidencialidad, integridad); OWASP API Security Top 10 API1 (BOLA); CWE-639; reglas 90.1.3, 96.5.2, 80.2.3.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-DEFECTO-002 — La política de mora `DIARIO_COMPUESTO` que admite la base no existe en Java y el fallo se traga
- Clase / Severidad / Confianza: DEFECTO (también CONTRATO, RESILIENCIA) / **S2** / confirmado por lectura
- Ubicaciones: `sql/10_tablas/03_aportes_pagos_qr/politica_mora.sql:17`; `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/RecargoDeMora.java:21-25`; `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CobrarAporte.java:217-219`; `servicios/aportes/src/main/java/bo/aportaya/aportes/trabajos/RecargosDeMoraDiarios.java:54-56`
- Evidencia: base `CHECK (tipo_recargo IN ('DIARIO_COMPUESTO', 'FIJO', 'PORCENTUAL'))`; Java `enum Tipo { FIJO, PORCENTUAL, PORCENTUAL_DIARIO }`; `RecargoDeMora.Tipo.valueOf(p.tipoRecargo())`; el trabajo `catch (RuntimeException fallo) { LOG.warn(…`.
- Cómo se comprobó: lectura de las cuatro ubicaciones.
- Impacto: una sola política `DIARIO_COMPUESTO` hace fallar `valueOf` dentro de la transacción del trabajo diario: se revierte la corrida entera (ningún grupo recibe recargo) y solo queda un `warn`. `PORCENTUAL_DIARIO` es inalcanzable desde la base. Además `CU21CobrarAporte.java:218` pasa `0` como días de gracia de la política (ignora `politica_mora.dias_gracia`).
- Causa de fondo: catálogo cerrado modelado como enum del lenguaje y como `CHECK` por separado (regla 97.4.7).
- Solución de fondo: un único catálogo codificado; el trabajo aísla la falla por obligación y alerta.
- Relación con el flujo nuevo: ninguna directa (A-4)
- Conexiones y módulos afectados: `nucleo-financiero` (el recargo va al fondo de garantía, ver RESPALDO-002).
- Estándares y normas: ISO/IEC 25010 Fiabilidad (tolerancia a fallos), Adecuación funcional; reglas 97.4.7, 96.7.4.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-HUECO-001 — Las cuotas de los periodos 2..N solo nacen si un administrador las dispara a mano
- Clase / Severidad / Confianza: HUECO (también APROB) / **S2** / confirmado
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/web/AportesController.java:236-242`; `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml:15-17`
- Evidencia: `@Permiso("ADMIN_PLATAFORMA") … generarObligacionesDelPeriodo(…) { … calendario.generar(periodoId, sesion.actual())`; catálogo `id: generar-obligaciones · actor: ADMIN_PLATAFORMA`.
- Cómo se comprobó: `grep -rn "calendario.generar\|\.generar("` en `servicios/aportes` → único llamador `AportesController.java:242`. La primera cuota sí es automática (`ConsumidorDeGrupoIniciadoKafka` → `CU21AbrirPrimeraCuota`).
- Impacto: sin un humano que llame el endpoint cada periodo, no hay obligaciones, no hay cobro y no hay entrega. Es una habilitación manual en el circuito (R-NUEVA-1).
- Causa de fondo: falta un disparador por evento de apertura de periodo o por trabajo programado.
- Solución de fondo: consumidor de apertura de periodo (o trabajo con ShedLock) idempotente por `(periodo, cupo)`, que ya garantiza `uq_obligacion_periodo_cupo`.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: `grupos` (L3).
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (completitud); regla 96.7.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-HUECO-002 — Si `nucleo-financiero` no pudo aplicar el aporte y la compensación no procede, la cuota queda pagada sin dinero y sin alerta
- Clase / Severidad / Confianza: HUECO (también RESILIENCIA) / **S2** / confirmado por lectura
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CompensarAporte.java:95-124`; `servicios/aportes/src/main/java/bo/aportaya/aportes/consumo/ConsumidorDeAportesNoAplicadosKafka.java` (rama `NO_COMPENSABLE`)
- Evidencia: `return sinEfecto(Resultado.NO_COMPENSABLE);` y el consumidor solo registra un `warn`; el evento queda consumido.
- Cómo se comprobó: lectura del caso de uso y del consumidor.
- Impacto: el estado «pagado» de la obligación y el mayor quedan divergentes para siempre, sin tarea, expediente ni alerta; el «confirmado» de la entrega (FLUJO-QR-003) lo cuenta.
- Causa de fondo: la saga de cobro no tiene estado persistido ni paso de escalamiento.
- Solución de fondo: con el cobro disparado solo por confirmación (FLUJO-QR-001/002) desaparece la necesidad de compensar; mientras tanto, persistir la excepción y alertar.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: `nucleo-financiero`.
- Estándares y normas: ISO/IEC 25010 Fiabilidad (recuperabilidad); reglas 98.3.2, 96.7.4.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-DATOS-001 — Ninguna tabla de `aportes` es append-only y el rol de la aplicación puede modificarlas
- Clase / Severidad / Confianza: DATOS (también TRAZA) / **S2** / confirmado por lectura
- Ubicaciones: `sql/35_append_only/` (sin entradas de `aportes.*`); `sql/00_base/02_esquemas.sql:79-81`; `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/WebhookRepositorio.java:64-75` (UPDATE de estado del webhook, cuyo comentario `:12-20` lo llama append-only); `PagoRepositorio.java:107-116` (UPDATE a `REVERSADO`)
- Evidencia: `grep -n "aportes\." sql/35_append_only/*.sql` → vacío; `GRANT USAGE ON SCHEMA aportes TO svc_aportes; ALTER DEFAULT PRIVILEGES IN SCHEMA aportes …`.
- Cómo se comprobó: grep y lectura.
- Impacto: pagos, webhooks y reembolsos son registros de dinero que la aplicación (o un script con su rol) puede reescribir sin rastro; la regla 91.1.3/91.2.5 exige `UPDATE`/`DELETE` revocados en el rol.
- Causa de fondo: el modelo de estados de `pago` y `webhook_pasarela` se implementa con `UPDATE` en lugar de eventos de estado append-only.
- Solución de fondo: separar el hecho inmutable (pago recibido, webhook recibido) de su estado (tabla de transiciones append-only) y revocar `UPDATE/DELETE` sobre los hechos.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: auditoría.
- Estándares y normas: ISO/IEC 25010 Seguridad (no repudio, responsabilidad); reglas 91.1.3, 91.2.5.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-SEG-003 — La firma del webhook no cubre el timestamp
- Clase / Severidad / Confianza: SEG / **S3** / confirmado por lectura
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/VerificadorDeFirmaWebhook.java:30-36`; `CU100RecibirWebhookPasarela.java:98-114`
- Evidencia: `byte[] firma = mac.doFinal(cuerpoCrudo.getBytes(StandardCharsets.UTF_8));` — la ventana de ±5 min se evalúa sobre `X-Timestamp`, que no está firmado.
- Cómo se comprobó: lectura.
- Impacto: un cuerpo capturado se puede reenviar con un timestamp nuevo; hoy lo mitiga la deduplicación por `idEvento`, pero la ventana no aporta protección.
- Causa de fondo: esquema de firma propio, sin cubrir la cabecera temporal.
- Solución de fondo: firmar `timestamp + "." + cuerpo` (o el esquema que publique la pasarela contratada).
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 Seguridad (autenticidad); CWE-294 (replay).
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-DINERO-002 — Reembolsos sin moneda: se leen siempre como BOB
- Clase / Severidad / Confianza: DINERO / **S3** / confirmado por lectura
- Ubicaciones: `sql/10_tablas/03_aportes_pagos_qr/reembolso.sql` (sin columna `moneda`); `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/PagoRepositorio.java:198`
- Evidencia: `Dinero.de(f.get("monto", BigDecimal.class), Moneda.BOB)`.
- Cómo se comprobó: grep de `moneda` en el DDL (vacío) y lectura.
- Impacto: el reembolso de un pago en USD se trata como BOB; la aprobación falla al operar monedas distintas o, peor, devuelve en otra moneda. Regla 91.1.2.
- Causa de fondo: columna de importe sin moneda al lado.
- Solución de fondo: `moneda CHAR(3)` en `reembolso` (y `disputa_pago`, `conciliacion`, `excepcion_conciliacion`, `plan_regularizacion`, `extracto_bancario`, según el lote A — pendiente de verificar cada una).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional; ISO 4217; regla 91.1.2.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-UX-A11Y-001 — La app ofrece declarar un aporte por QR, transferencia o tarjeta escribiendo la referencia de un comprobante
- Clase / Severidad / Confianza: UX-A11Y (causa: FLUJO-QR, APROB) / **S1** / confirmado (servidor: FLUJO-QR-001)
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/pantalla_aportar.dart:23-36,110-161`; `apps/movil/lib/pantallas/pasanaku/textos_de_conversion.dart:41-43`
- Evidencia: canales `BILLETERA_MOVIL`, `QR_INTEROPERABLE`, `TRANSFERENCIA_BANCARIA`, `TARJETA` elegibles por la persona; campo «referencia del pago» con ayuda «El número de operación de tu comprobante.».
- Cómo se comprobó: lectura de la pantalla; el servidor acredita sin confirmación (FLUJO-QR-001).
- Impacto: el cliente oficial del producto implementa la autodeclaración del pago que R-NUEVA-1 prohíbe.
- Causa de fondo: CU-21 diseñado como «aplicar pago informado».
- Solución de fondo: la pantalla de aporte genera la orden y muestra el QR dinámico, y espera la confirmación del servidor; sin campo de referencia.
- Relación con el flujo nuevo: FLUJO-QR / APROB
- Conexiones y módulos afectados: `aportes`.
- Estándares y normas: ISO/IEC 25010 Seguridad (integridad); regla 91.3.1.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-HUECO-003 — No hay en la app un camino navegable para pagar un aporte
- Clase / Severidad / Confianza: HUECO / **S2** / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart:94-100` (única definición de `pasanaku.aportar`); `apps/movil/lib/pantallas/billetera/acciones_de_saldo.dart:22-26`; `apps/movil/lib/navegacion/enlaces_profundos.dart:16`
- Evidencia: `grep -rn "pasanaku.aportar\|'aportar'" apps/movil/lib` → solo `rutas_operacion.dart:96`; la acción «Aportar» navega a `pasanaku.miEstado`; el enlace profundo `aporte` apunta a `/billetera/aportes`.
- Cómo se comprobó: grep (subagente D) y re-ejecución propia del grep.
- Impacto: en producción la persona no tiene cómo aportar desde la app; el flujo cobrado-por-QR no tiene superficie.
- Causa de fondo: pantalla construida y nunca enlazada.
- Solución de fondo: con la pantalla de cobro por QR (UX-A11Y-001), enlazarla desde «Mi estado» y desde el aviso.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: navegación de la app (L6).
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (completitud).
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-SEG-004 — El importe a cobrar viaja en la URL de la pantalla de aporte
- Clase / Severidad / Confianza: SEG (también DEFECTO) / **S3** / confirmado por lectura
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart:98-99`; `apps/movil/lib/pantallas/pasanaku/pantalla_aportar.dart:137-142`
- Evidencia: `montoDelPeriodo: state.uri.queryParameters['monto'] ?? '0.00'`; moneda fija `'BOB'`.
- Cómo se comprobó: lectura.
- Impacto: importe atribuible en la URL (regla 90.2.2); el monto lo decide el cliente; sin parámetro se cobra `0.00`.
- Causa de fondo: la pantalla no consulta la obligación al servidor.
- Solución de fondo: leer monto y moneda de la obligación por id.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `aportes`.
- Estándares y normas: ISO/IEC 25010 Seguridad (confidencialidad); CWE-598; regla 90.2.2.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-RESPALDO-002 — La web pública promete que «el fondo cubre la entrega» si alguien no aporta
- Clase / Severidad / Confianza: RESPALDO (también DOC) / **S3** / confirmado
- Ubicaciones: `apps/web/src/app/paginas/inicio/secciones/producto.ts:75`; `apps/web/src/app/app.routes.ts:117`; además `cierre.ts:71`, `transparencia.ts:65`, `hero.ts:31` (citadas por el subagente D, no re-abiertas una por una)
- Evidencia: «Si alguien no aporta, el fondo cubre la entrega de quien tenía el turno.»; «Un fondo de garantía cubre esa falta para que el resto del grupo no se vea afectado».
- Cómo se comprobó: `grep -n "cubre"` sobre los dos primeros archivos.
- Impacto: promesa comercial contraria a R-NUEVA-2; riesgo de reclamo del consumidor si se mantiene tras el cambio.
- Causa de fondo: textos escritos para ADR-051.
- Solución de fondo: reescribir según la decisión A-2 (qué pasa con la entrega cuando falta plata).
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: marketing, L6.
- Estándares y normas: ISO/IEC 25010 Usabilidad (reconocibilidad adecuada).
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-UX-A11Y-002 — Comprobantes con título fijo que no refleja el estado devuelto
- Clase / Severidad / Confianza: UX-A11Y (también COHERENCIA) / **S3** / confirmado por lectura
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/aporte_registrado.dart:19-29`; `apps/movil/lib/pantallas/billetera/comprobante_de_retiro.dart:22-26`
- Evidencia: el aporte muestra siempre `TextosPasanaku.aporteConfirmado` con `Tono.ok` (el chip sí muestra el estado de la obligación); el retiro muestra siempre `retiroInstruido`, aunque el estado sea `EN_REVISION` o `RECHAZADA`.
- Cómo se comprobó: lectura.
- Impacto: la persona lee «confirmado»/«instruido» cuando la cuota sigue pendiente o el retiro espera revisión.
- Causa de fondo: el título no se deriva del estado del servidor.
- Solución de fondo: título y tono según el estado devuelto; mostrar el pendiente.
- Relación con el flujo nuevo: APROB (retiro en revisión)
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 Usabilidad; regla 95.2.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-SEG-005 — Los «cuatro ojos» del reverso de billetera se cumplen escribiendo cualquier UUID en el cuerpo
- Clase / Severidad / Confianza: SEG (también APROB) / **S1** / confirmado por lectura
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/MapeoDeBilletera.java:148-155`; `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU14ReversarTransaccion.java:70-74`; `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/BilleteraController.java:230-236` (`POST /billetera/reversos`, `REVERSO_AUTORIZAR`); `servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml:1035-1040`
- Evidencia: `cuerpo.getAutorizadaPor()` se pasa tal cual al caso de uso, que solo comprueba `if (entrada.autorizadaPor().equals(ctx.usuarioId()))`.
- Cómo se comprobó: lectura de las tres ubicaciones; el autorizador nunca se autentica ni tiene sesión propia.
- Impacto: una sola persona con `REVERSO_AUTORIZAR` reversa transacciones de billetera de cualquier cuenta declarando un «autorizador» inventado; el rastro de auditoría atribuye la autorización a un tercero que no participó. Es la segregación de funciones de R-SEG-04 anulada.
- Causa de fondo: la segunda firma se modela como un dato de entrada y no como una acción autenticada.
- Solución de fondo: flujo en dos pasos (solicitar · autorizar) donde cada paso toma el usuario del token; o, si el reverso manual deja de existir en el circuito (R-NUEVA-1), retirarlo. Lo segundo es DECISIÓN REQUERIDA (A-1).
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: `garantia` (consume `transaccion_reversada`, pacto `nucleo-financiero-garantia-transaccion-reversada.json`).
- Estándares y normas: ISO/IEC 25010 Seguridad (responsabilidad, no repudio); OWASP API Security Top 10 API5 (BFLA); CWE-639, CWE-284; reglas 90.1.2, 91.4.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-RESPALDO-003 — El consumidor de asientos que registra la cobertura empresarial está encendido en el despliegue de TEST
- Clase / Severidad / Confianza: RESPALDO / **S1** / probable (el lado productor es de `garantia`, lote L2; falta confirmar que publique en TEST)
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/consumo/ConsumidorDeAsientosKafka.java:44-56`; `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ConsumoDeAsientos.java:31-33`; `docker-compose.coolify.yml:603-611`; `servicios/nucleo-financiero/src/main/resources/application.yml:88-112`
- Evidencia: el compose de Coolify fija `CONSUMO_DE_ASIENTOS_HABILITADO: ${CONSUMO_DE_ASIENTOS_HABILITADO:-true}` y mapea `CUENTA_LOGICA_CAJA_EMPRESA_BOB …:-9.1.01`, `CUENTA_LOGICA_RESERVA_RESPALDO_BOB …:-9.1.02`, `CUENTA_LOGICA_POZO_GRUPO_BOB …:-9.1.03`. El consumidor escucha `garantia.cobertura_empresarial_aplicada/_reversada` y `garantia.respaldo_reservado/_ampliado/_liberado/_recuperado`.
- Cómo se comprobó: lectura del compose (`sed -n 600,612p`) y del consumidor (subagente C; líneas re-abiertas del compose).
- Impacto: en el entorno desplegado, cada vez que `garantia` cubre un faltante, el mayor de `nucleo-financiero` asienta dinero de la empresa (reserva de respaldo) hacia el pozo del grupo. Es exactamente lo que R-NUEVA-2 prohíbe, en un camino vivo.
- Causa de fondo: implementación de ADR-051 (regla 7, «la empresa aporta el faltante»).
- Solución de fondo: dejar de consumir los tópicos de respaldo/cobertura y retirar las cuentas lógicas `CAJA_EMPRESA`/`RESERVA_RESPALDO`; qué se hace con lo ya asentado y con el faltante es DECISIÓN REQUERIDA (A-2, A-3).
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: `garantia`, `entregas` (L2).
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (corrección respecto del requisito vigente); reglas 91.1.5, 98.3.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-DINERO-003 — Recargas, transferencias y reversos de billetera no asientan en el mayor: dos contabilidades que divergen
- Clase / Severidad / Confianza: DINERO (también COHERENCIA) / **S2** / confirmado
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU10RecargarSaldo.java`, `CU12TransferirSaldo.java`, `CU14ReversarTransaccion.java` (0 referencias a `CU24RegistrarAsiento`); `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/CuentaBilleteraRepositorio.java:94-110` (saldo desde `movimiento_billetera`); `servicios/nucleo-financiero/README.md:114`
- Evidencia: `grep -c "cu24\|CU24"` → 0 en los tres casos de uso; en `ResolucionDeRetiro` y `CU13RetenerSaldo` las coincidencias son solo comentarios. El README: «recargas y retiros no asientan en el mayor, asi que 2.1.01 queda negativo».
- Cómo se comprobó: grep y lectura.
- Impacto: el saldo de billetera (lo que autoriza débitos) sale del libro de billetera y el mayor no lo refleja; el reverso de un `APORTE_A_GRUPO` (que sí tiene asiento) deja el mayor sin contra-asiento. El panel y el cierre no pueden cuadrar contra el mayor (regla 91.1.5).
- Causa de fondo: el mayor de doble partida se agregó después del libro de billetera y solo se conectó a dos consumidores.
- Solución de fondo: toda operación de billetera genera su asiento en la misma transacción; el reverso usa el contra-asiento del asiento vinculado.
- Relación con el flujo nuevo: FLUJO-QR (la recarga por QR es la otra entrada de dinero)
- Conexiones y módulos afectados: `erp`, transparencia, cierre diario.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (corrección); reglas 91.1.5, 91.6.3.
- Esfuerzo estimado: grande

### aportes-nucleo-financiero-DINERO-004 — El cuadre del asiento y el signo de cada línea no están garantizados en la base
- Clase / Severidad / Confianza: DINERO (también DATOS) / **S2** / confirmado por lectura
- Ubicaciones: `sql/40_reglas/restricciones.sql:112-135` (`fn_aud_asiento_cuadrado`, `tg_asiento_cuadrado` solo `ON asiento_contable`); `sql/10_tablas/03_aportes_pagos_qr/movimiento_contable.sql:10-11`
- Evidencia: `CREATE CONSTRAINT TRIGGER tg_asiento_cuadrado AFTER INSERT OR UPDATE ON asiento_contable … WHEN (NEW.estado IN ('CONFIRMADO','REVERSADO'))`; la función compara `COALESCE(SUM(debe),0)` con `COALESCE(SUM(haber),0)` (un asiento sin líneas pasa como 0 = 0); `debe NUMERIC(16,2) NOT NULL, haber NUMERIC(16,2) NOT NULL` sin `CHECK` (`grep "debe *>=\|ck_movimiento_contable"` → vacío).
- Cómo se comprobó: lectura y grep.
- Impacto: un `INSERT` de `movimiento_contable` en una transacción posterior sobre un asiento ya confirmado no vuelve a disparar el cuadre (append-only impide `UPDATE`/`DELETE`, no `INSERT`); líneas con importe negativo o con debe y haber a la vez pasan. Regla 91.2.1 y 91.2.2.
- Causa de fondo: la invariante se ata al encabezado y no a las líneas.
- Solución de fondo: constraint trigger diferido también `AFTER INSERT ON movimiento_contable` (por `asiento_id`), mínimo dos líneas, y `CHECK (debe >= 0 AND haber >= 0 AND (debe = 0) <> (haber = 0))`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los que proponen asientos (`garantia`, `entregas`).
- Estándares y normas: ISO/IEC 25010 Seguridad (integridad); reglas 91.2.1, 91.2.2, 97.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-HUECO-004 — Los retiros autorizados nunca se instruyen al proveedor
- Clase / Severidad / Confianza: HUECO / **S2** / confirmado
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU11RetirarSaldo.java:107` (`instruirPago`); `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/RetirosConProveedor.java:66` (`despachar`)
- Evidencia: `grep -rn "instruirPago\|despachar(" servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero` → solo las definiciones y comentarios; `scripts/validar_flujos.py`: «AVISO · recarga-y-retiro.yaml: paso `instruir-pago` SIN CABLEAR — H8-01 · Ningún trabajo, endpoint ni consumidor llama a RetirosConProveedor.despachar ni a CU11RetirarSaldo.instruirPago en producción».
- Cómo se comprobó: grep propio y salida del verificador.
- Impacto: una orden `AUTORIZADA` deja el saldo retenido indefinidamente; la persona no recibe su dinero.
- Causa de fondo: falta el disparador; la decisión (A9 del plan del carril) quedó abierta.
- Solución de fondo: un único despachador (ver DUP-INT-001) con disparador real; si es automático o de operador es DECISIÓN REQUERIDA (A-1).
- Relación con el flujo nuevo: APROB (si el disparador es humano)
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (completitud).
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-HUECO-005 — `nucleo-financiero` no arranca fuera de `local`/`test`, y la prueba de arranque lo tapa con un mock
- Clase / Severidad / Confianza: HUECO (también PRUEBAS) / **S2** / confirmado por lectura
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/ProveedorDeRetiroLocal.java:38`; `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/` `ArranqueProduccionTest` `:56-57`; `despliegue/TEST.md:455-458`
- Evidencia: único `implements ProveedorDeRetiro` con `@Profile({"local", "test"})`; en la prueba, `@MockitoBean private ProveedorDeRetiro proveedorDeRetiro;`; TEST.md: «nucleo-financiero no arranca (no existe un ProveedorDeRetiro fuera de local/test) … Se queda en PERFIL_SPRING=local».
- Cómo se comprobó: grep de implementaciones y lectura.
- Impacto: el servicio que es el libro contable no puede desplegarse con perfil de producción; por eso TEST corre con `local`, que arrastra los dobles de desarrollo (ver SEG-001).
- Causa de fondo: adaptador real de retiros sin construir; la prueba de arranque no lo detecta porque lo reemplaza.
- Solución de fondo: adaptador real o fail-closed explícito que arranque; prueba de arranque sin mocks del puerto.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: despliegue (L5).
- Estándares y normas: ISO/IEC 25010 Portabilidad (instalabilidad); regla 80.5.4.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-ACOPLE-001 — El mayor tiene claves foráneas hacia tablas de `grupos`, `identidad` y `erp`, y otros servicios leen sus tablas
- Clase / Severidad / Confianza: ACOPLE / **S2** / confirmado por lectura
- Ubicaciones: `sql/20_claves/03_aportes_pagos_qr.sql:15-36` (`REFERENCES grupos.grupo`, `identidad.usuario`, `erp.periodo_contable`, con `ON DELETE SET NULL`); entrantes: `servicios/cumplimiento/.../LimiteRepositorio.java:67,99`, `servicios/erp/.../PeriodoRepositorio.java:149-150`, `servicios/erp/.../PresupuestoRepositorio.java:113-115,172` (marcadas por `scripts/auditar_backend.py` como «divergencias declaradas, invariante 11»)
- Evidencia: `FOREIGN KEY (grupo_id) REFERENCES grupos.grupo (id) ON DELETE SET NULL`; `FOREIGN KEY (registrado_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL`.
- Cómo se comprobó: lectura del DDL de claves; salida de `auditar_backend.py`.
- Impacto: un `DELETE` en `grupos` intenta un `UPDATE` sobre `asiento_contable` (append-only) y falla: el libro bloquea a otro servicio. Los lectores entrantes acoplan `erp` y `cumplimiento` al esquema interno del mayor. Regla 98.1.1/98.1.2.
- Causa de fondo: el esquema se modeló como una base única antes de partir en servicios.
- Solución de fondo: quitar las FK entre esquemas y validar por evento/API; exponer a `erp`/`cumplimiento` lo que necesitan por contrato.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `grupos`, `identidad`, `erp`, `cumplimiento`.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad (modularidad); ADR-017; reglas 98.1.1, 98.1.2.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-RESPALDO-004 — El recargo por mora fondea la reserva de respaldo de la empresa
- Clase / Severidad / Confianza: RESPALDO / **S2** / confirmado por lectura
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/DestinoDelCobro.java:17-19,43`; `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml:81-87`
- Evidencia: `FONDO_GARANTIA( // La misma cuenta lógica que usa garantia para sus coberturas. "RESERVA_RESPALDO", …`; catálogo: «DEBE usuarios / HABER reserva de respaldo; el pozo del grupo no se mueve».
- Cómo se comprobó: lectura.
- Impacto: el recargo que paga el moroso termina en la cuenta con la que la empresa cubre faltantes; si el respaldo desaparece (R-NUEVA-2), el destino del recargo queda sin definir.
- Causa de fondo: ADR-051.
- Solución de fondo: DECISIÓN REQUERIDA (A-4): a quién pertenece el recargo cuando nadie cubre el faltante.
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: `garantia`, `aportes`.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional; regla 91.5.
- Esfuerzo estimado: chico (una vez decidido)

### aportes-nucleo-financiero-RESPALDO-005 — Las semillas base (las que se aplican en todos los entornos) siembran cuentas «sintéticas» de caja de la empresa y reserva de respaldo
- Clase / Severidad / Confianza: RESPALDO (también DATOS) / **S3** / confirmado por lectura
- Ubicaciones: `sql/60_semillas/22-cuentas-de-sistema.sql:15-33`
- Evidencia: `('9.1.01', 'Caja de la empresa BOB (sintetica)', …)`, `('9.1.02', 'Reserva de respaldo BOB (sintetica)', …)`, ídem USD.
- Cómo se comprobó: lectura.
- Impacto: datos sintéticos en semillas base (regla 97.6.3: los datos de demostración viven separados) y cuentas cuya razón de ser contradice R-NUEVA-2.
- Causa de fondo: el mapeo sintético se usó para poder encender el consumidor de asientos en TEST.
- Solución de fondo: mover a `61_dev` lo sintético y, según A-3, retirar las cuentas de respaldo.
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: `garantia`.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional; reglas 97.4.1, 97.6.3.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-FLUJO-QR-005 — La recarga de billetera se acredita cuando un humano de Tesorería la dispara, no por webhook
- Clase / Severidad / Confianza: FLUJO-QR (también APROB) / **S2** / confirmado por lectura
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/BilleteraController.java:142-149`; `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/RecargasConProveedor.java`; `servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml:193-215`
- Evidencia: «Hasta que exista el webhook de la pasarela para recargas, lo confirma tesoreria.» · `@Permiso("TESORERIA") … acreditarRecarga(UUID ordenId, …)`.
- Cómo se comprobó: lectura. La acreditación consulta al proveedor (no acredita «lo que vino»), pero depende de que una persona la dispare; el lote B señala además que el caso de uso exige titularidad (`CU10RecargarSaldo.java:156,189-190`), de modo que el disparo de Tesorería probablemente falla (sospecha, no ejecutada).
- Impacto: dinero pagado por la persona que no se acredita hasta que alguien lo confirma; es un paso humano en la entrada de dinero (R-NUEVA-1).
- Causa de fondo: no hay webhook de recargas.
- Solución de fondo: webhook firmado de la pasarela (mismo patrón que CU100, corregido según SEG-003) que acredite con identidad de sistema.
- Relación con el flujo nuevo: FLUJO-QR / APROB
- Conexiones y módulos afectados: app móvil (`pantalla_recargar.dart`).
- Estándares y normas: ISO/IEC 25010 Adecuación funcional; regla 91.3.1.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-DUP-INT-001 — Dos puertos y dos caminos para instruir un retiro
- Clase / Severidad / Confianza: DUP-INT (también REESCRITURA) / **S3** / confirmado por lectura
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/ProveedorDeRetiro.java`, `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/ProveedorDeRetiros.java`; `CU11RetirarSaldo.java:107-136` frente a `CU11InstruirRetiro.java:39-75` + `RetirosConProveedor.java`
- Evidencia: dos interfaces para el mismo proveedor; ninguno de los caminos tiene llamador en `main` (HUECO-004).
- Cómo se comprobó: lectura del lote B y grep propio de llamadores.
- Impacto: reglas que divergen (según el lote B, `instruirPago` no revalida titularidad, doble aprobación ni enfriamiento); cualquiera que se cablee primero fija el comportamiento.
- Causa de fondo: dos carriles construyeron la misma pieza.
- Solución de fondo: un solo puerto y un solo despachador.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-DINERO-005 — El extracto y el certificado de saldo rotulan en BOB las cuentas en USD
- Clase / Severidad / Confianza: DINERO / **S3** / confirmado por lectura
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/ExtractoRepositorio.java:31-32,64`
- Evidencia: `Dinero.de(f.get("saldo_disponible", BigDecimal.class), Moneda.BOB)`.
- Cómo se comprobó: grep y lectura.
- Impacto: documento con valor probatorio emitido en la moneda equivocada. Regla 91.1.2.
- Causa de fondo: moneda fija en el mapeo.
- Solución de fondo: leer la moneda de la cuenta.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional; ISO 4217.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-DINERO-006 — El «sello» del cierre diario no incluye los saldos que dice proteger
- Clase / Severidad / Confianza: DINERO (también DOC) / **S3** / confirmado por lectura
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU51EjecutarCierreDiario.java:172-176`
- Evidencia: `/** El sello del saldo del dia: lo que permite detectar despues una alteracion. */ … String material = cuentaId + "|" + fecha;`
- Cómo se comprobó: lectura.
- Impacto: alterar el saldo sellado no cambia el sello; el control antialteración no existe y el comentario lo afirma.
- Causa de fondo: el material del hash omite el contenido.
- Solución de fondo: incluir saldos y moneda (y el hash del día anterior) en el material.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: auditoría.
- Estándares y normas: ISO/IEC 25010 Seguridad (integridad); CWE-354.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-HUECO-006 — La transferencia P2P publica `obligacion_pagada` y nadie la consume
- Clase / Severidad / Confianza: HUECO / **S3** / confirmado (se retira la sospecha S1 del lote B)
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU12TransferirSaldo.java:160-171`
- Evidencia: `grep -rln "obligacion_pagada" servicios plataforma --include=*.yml --include=*.java` → solo el productor y su test.
- Cómo se comprobó: grep propio. La hipótesis de que una transferencia a un grupo marcara un aporte pagado sin QR **no se sostiene**: `aportes` solo consume `aporte_no_aplicado` y `grupo_iniciado`.
- Impacto: dinero que entra a la billetera de un grupo por fuera de toda obligación (se puede transferir a un grupo, `ResolverDestino`) sin reflejo en `aportes`; evento sin consumidor.
- Causa de fondo: integración a medio construir.
- Solución de fondo: decidir si P2P hacia grupos existe; si no, prohibirlo; si sí, que `aportes` lo reconozca por contrato.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: `aportes`.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (completitud).
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-PRUEBAS-001 — Las pruebas fijan el flujo viejo y no hay evidencia de ejecución
- Clase / Severidad / Confianza: PRUEBAS / **S2** / confirmado
- Ubicaciones: `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CUConsumoDeAportesRechazosTest.java:219` (espera `CANAL_SIN_BILLETERA` para QR); `ArranqueProduccionTest:56-57` (mock del puerto de retiro); tests web de `aportes` sin matriz de propiedad (SEG-002); `CU21Test` nombres que no prueban lo que dicen (`:189` «concurrencia» secuencial, `:207` «cuadre» que prueba solo `SaldoDeLaObligacion`, según el lote A); `scripts/auditar_backend.py`: «0/393 clases de prueba corrieron y pasaron».
- Evidencia: `assertThat(salida.resultado()).isEqualTo(Resultado.CANAL_SIN_BILLETERA);`
- Cómo se comprobó: grep propio de las dos primeras; salida del verificador.
- Impacto: arreglar FLUJO-QR-004 pone en rojo una prueba que hoy protege el defecto; ninguna prueba ejercita monto negativo, canal prohibido, propiedad, ni el perfil productivo del secreto. No hay `@Disabled` (bien).
- Causa de fondo: las pruebas describen la implementación vigente, no el requisito.
- Solución de fondo: reescribir los casos según R-NUEVA-1/2 y agregar los negativos citados.
- Relación con el flujo nuevo: FLUJO-QR / RESPALDO
- Conexiones y módulos afectados: CI.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad (capacidad de prueba); reglas 80.2.3, 80.2.5, 80.5.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-CONFIG-001 — Pool de conexiones declarado distinto en el descriptor y en la configuración
- Clase / Severidad / Confianza: CONFIG / **S4** / confirmado por los lotes A y C (no re-abierto)
- Ubicaciones: `servicios/aportes/descriptor.yml:17` y `servicios/aportes/src/main/resources/application.yml:14`; ídem en `servicios/nucleo-financiero` (`descriptor.yml:16`, `application.yml:14`)
- Evidencia: `hikari_por_replica: 10` frente a `maximum-pool-size: 20`.
- Impacto: el cálculo de capacidad de PgBouncer (ADR-037) subestima la mitad de las conexiones.
- Causa de fondo: dos fuentes para el mismo número.
- Solución de fondo: derivar uno del otro.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: despliegue.
- Estándares y normas: ISO/IEC 25010 Eficiencia (capacidad).
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-DEFECTO-003 — Ninguna obligación pasa a mora por el paso del tiempo
- Clase / Severidad / Confianza: DEFECTO / **S2** / confirmado por lectura
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/SaldoDeLaObligacion.java:41-49`; `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/EstadoDelParticipanteRepositorio.java:28,31`
- Evidencia: `estadoSegunSaldo(…, vencida)` solo se invoca al cobrar, compensar o reembolsar; `grep -rn "EN_MORA\|VENCIDO"` en `servicios/aportes/src/main/java` no muestra ninguna transición programada. El estado del participante cuenta deuda con `WHERE o.estado IN ('EN_MORA', 'VENCIDO')`.
- Cómo se comprobó: grep y lectura. El trabajo diario de recargos crea otra fila (`RECARGO_MORA`) y no cambia el estado de la original.
- Impacto: un participante que no pagó aparece «al día» con deuda 0 para `grupos`, `nucleo-financiero` (`HechosPorHttp` consulta este estado) y la app.
- Causa de fondo: el estado es un campo escrito en eventos de pago, no una función de la fecha.
- Solución de fondo: transición diaria idempotente (`PENDIENTE → EN_MORA` por fecha de fin de gracia en `America/La_Paz`) o calcular la mora al leer.
- Relación con el flujo nuevo: ninguna directa (A-4)
- Conexiones y módulos afectados: `grupos`, `garantia`, `nucleo-financiero`.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (corrección).
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-DEBIDO-PROCESO-001 — El recargo por mora se aplica automáticamente aunque la política no lo autorice
- Clase / Severidad / Confianza: DEBIDO-PROCESO (también DEFECTO) / **S2** / confirmado por lectura
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CobrarAporte.java:198-247`; `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/ObligacionRepositorio.java:151-172`; `sql/10_tablas/03_aportes_pagos_qr/politica_mora.sql` (`aplica_automatico`, `vigente_desde`)
- Evidencia: el repositorio de políticas lee `dias_gracia` (`:158,172`) pero no `aplica_automatico` ni la vigencia (`grep -n "aplica_automatico\|vigente"` vacío); el trabajo crea el recargo sin aviso previo ni plazo de descargo.
- Cómo se comprobó: grep y lectura.
- Impacto: se cobra una penalidad a una persona sin expediente ni descargo (regla 91.4.1 y 91.4.4), incluso donde el grupo pidió que no fuera automático.
- Causa de fondo: la política está modelada en la base y no se respeta en el código.
- Solución de fondo: respetar `aplica_automatico` y la vigencia; cuando aplique, aviso y plazo de descargo antes de devengar. El tratamiento de la mora sin respaldo es DECISIÓN REQUERIDA (A-4).
- Relación con el flujo nuevo: ninguna directa (A-4)
- Conexiones y módulos afectados: `nucleo-financiero` (aplica el recargo), `garantia`.
- Estándares y normas: ISO/IEC 25010 Adecuación funcional; regla 91.4.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-DEFECTO-004 — La recarga de billetera evalúa los límites después de que el dinero llegó
- Clase / Severidad / Confianza: DEFECTO (también DINERO) / **S2** / confirmado por lectura
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU10RecargarSaldo.java:226-232`
- Evidencia: tras acreditar la orden, `limites.exigirDentroDe(dsl, new EntradaLimites(cuenta.id(), CONCEPTO, orden.acreditado()), ctx);`
- Cómo se comprobó: lectura.
- Impacto: si el límite se supera, la excepción revierte la acreditación de un dinero que la pasarela ya cobró; no existe el camino de devolución. Dinero de la persona sin acreditar ni devolver.
- Causa de fondo: el control preventivo se ubicó en el paso de confirmación.
- Solución de fondo: evaluar el límite al solicitar la orden; si al confirmar se excede, acreditar y abrir una devolución con su asiento.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: `cumplimiento` (límites).
- Estándares y normas: ISO/IEC 25010 Fiabilidad; regla 91.3.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-RESILIENCIA-001 — La `Idempotency-Key` del reembolso, su aprobación y el alta de proveedor se recibe y se descarta
- Clase / Severidad / Confianza: RESILIENCIA (también DINERO 91.1.4) / **S3** / confirmado por lectura
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/web/PagosController.java:56-72,77,114`
- Evidencia: `solicitarReembolso(UUID idempotencyKey, UUID pagoId, EntradaReembolso cuerpo)` → `cu19.solicitar(new CU19ReembolsarPago.EntradaReembolso(pagoId, …, cuerpo.getMotivo().getValue()), …)` sin la clave.
- Cómo se comprobó: lectura.
- Impacto: un doble envío crea dos solicitudes de reembolso del mismo pago (el tope por disponible lo acota, pero dos parciales pasan). Regla 91.1.4.
- Causa de fondo: el contrato pide la cabecera y el caso de uso no la modela.
- Solución de fondo: clave en la entidad con `UNIQUE` y respuesta idempotente.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguno.
- Estándares y normas: ISO/IEC 25010 Fiabilidad; reglas 91.1.4, 96.3.3.
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-REESCRITURA-001 — `ConciliarLoteTripartito` existe y está probado, pero nada lo invoca
- Clase / Severidad / Confianza: REESCRITURA (código muerto) / **S3** / confirmado
- Ubicaciones: `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/ConciliarLoteTripartito.java` (142 líneas)
- Evidencia: `grep -rln "ConciliarLoteTripartito" servicios/aportes/src` → la propia clase y dos tests (`CU21CalendarioConciliacionTest`, `CU132ConciliacionTest`); sin endpoint, trabajo ni consumidor.
- Cómo se comprobó: grep propio.
- Impacto: la conciliación tripartita (libro · proveedor · banco) que justificaría el «confirmado» no corre; ningún pago pasa a conciliado. Según el lote A, además corta los días en UTC (`ConciliacionRepositorio.java:36-38`; no re-abierto).
- Causa de fondo: pieza construida sin disparador.
- Solución de fondo: cablearla (trabajo diario) o retirarla; con el flujo QR por webhook, la conciliación es el control posterior.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: `nucleo-financiero` (cierre diario).
- Estándares y normas: ISO/IEC 25010 Mantenibilidad.
- Esfuerzo estimado: medio

### aportes-nucleo-financiero-DEFECTO-005 — Sin ninguna conciliación de custodia, el encaje se da por cumplido
- Clase / Severidad / Confianza: DEFECTO / **S3** / confirmado por lectura
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/OrdenRetiroRepositorio.java:264-277`
- Evidencia: `SELECT count(*) … WHERE NOT cumple_encaje` sobre las últimas conciliaciones; `return incumplen == null || incumplen == 0;` — con cero filas, devuelve verdadero.
- Cómo se comprobó: lectura.
- Impacto: el control que frena retiros cuando la custodia no cubre los saldos abre por omisión (contra el principio de denegar por omisión del repo).
- Causa de fondo: ausencia de dato tratada como cumplimiento.
- Solución de fondo: sin conciliación vigente para la moneda, el encaje es «desconocido» y los retiros se frenan o se acotan.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: Tesorería.
- Estándares y normas: ISO/IEC 25010 Seguridad; CWE-636 (no fallar de forma segura).
- Esfuerzo estimado: chico

### aportes-nucleo-financiero-SEG-006 — El bloqueo judicial de saldo se autoriza con un permiso de lectura
- Clase / Severidad / Confianza: SEG / **S3** / confirmado por lectura
- Ubicaciones: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/` (método `bloquearPorAutoridad`, `@Permiso("DATOS_SENSIBLES_LEER")`); `POST /billetera/bloqueos`
- Evidencia: extracción de anotaciones de los controladores: `nucleo-financiero bloquearPorAutoridad Permiso("DATOS_SENSIBLES_LEER")`.
- Cómo se comprobó: script de lectura de anotaciones `@Permiso` sobre `web/*.java` (sección 6).
- Impacto: quien puede **leer** datos sensibles puede **inmovilizar** el saldo de cualquier persona; además el levantamiento no libera la retención (lote B, `CU17BloquearPorAutoridad.java:143`, no re-abierto).
- Causa de fondo: permiso reutilizado por afinidad temática, no por acción.
- Solución de fondo: permiso propio de escritura para bloqueos judiciales, con segregación respecto de quien los levanta.
- Relación con el flujo nuevo: APROB (AP-11)
- Conexiones y módulos afectados: `cumplimiento` (L4).
- Estándares y normas: ISO/IEC 25010 Seguridad; OWASP API Security Top 10 API5 (BFLA); regla 90.1.4.
- Esfuerzo estimado: chico

## 6. Matriz de contratos
Fuente de la especificación: `servicios/aportes/src/main/resources/openapi/aportes.yaml` y `servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml`. Permiso: extraído de las anotaciones `@Permiso` de `web/*.java`. `scripts/inventario_endpoints.py --check` → «endpoints relevados: 350 · hallazgos: 0 · todo lo implementado tiene contrato y viceversa». Dinero en el contrato: `{monto: string ^-?\d+\.\d{2}$, moneda: BOB|USD}` en los dos servicios (admite negativos: DINERO-001). Cliente generado: no versionado (`.gitignore:50-51`); se comparó contra el uso en `apps/*`.

| Operación | Especificación | Implementación (permiso · propiedad) | Pacto | Modelo del front | Esquema | Discrepancias |
|---|---|---|---|---|---|---|
| `POST /aportes/obligaciones/{id}/pagos` cobrarAporte | `EntradaCobro` (canal enum de 7, `esManual`, `referenciaProveedor` ≤100) | `BILLETERA_OPERAR` · sí (vía `grupos`) | — | `pantalla_aportar.dart` | `pago`, `obligacion_aporte` | canal y esManual del cliente (FLUJO-QR-001, RESPALDO-001); 100 vs `VARCHAR(80)` (CONTRATO-001); `recargo`→comisión (DEFECTO-001) |
| `POST /pagos/{id}/reembolsos` | `EntradaReembolso` | `BILLETERA_OPERAR` · **no** | — | — | `reembolso` (sin moneda, sin CHECK) | idempotencia ignorada (RESILIENCIA-001); negativo (DINERO-001) |
| `POST /pagos/reembolsos/{id}/aprobacion` | — | `REVERSO_AUTORIZAR` · cuatro ojos por sesión | — | sin pantalla | `reembolso.aprobado_por` | — |
| `POST /pagos/{id}/disputas` | — | `SOPORTE` | — | — | `disputa_pago` (sin moneda) | — |
| `GET/POST /pagos/proveedores` | — | `TABLERO_SISTEMAS_VER` / `ADMIN_PLATAFORMA` | — | backoffice sistemas | `proveedor_pago` | banderas autoafirmadas (lote A, no re-abierto) |
| `GET /pagos/webhooks-recibidos` | límite 1-100 | `TABLERO_SISTEMAS_VER` | — | backoffice sistemas | `webhook_pasarela` | — |
| `POST /pagos/enrutamiento` | — | `BILLETERA_OPERAR` | — | — | `proveedor_pago` | salud observada la envía el cliente (lote A, no re-abierto) |
| `POST /pagos/webhooks/{codigo}` | cuerpo crudo + `X-Firma` + `X-Timestamp` | `@Publico` + HMAC | — (sin pacto con la pasarela) | — | `webhook_pasarela`, `uq_webhook_idem` | sin efecto (FLUJO-QR-002); timestamp no firmado (SEG-003) |
| `GET /aportes/participantes/{id}/estado` | `EstadoDelParticipante` | `BILLETERA_VER` · **no** | — (consumido por `nucleo` `HechosPorHttp` sin pacto) | `pantalla_mi_estado.dart` | `obligacion_aporte` | BOLA (SEG-002); suma monedas distintas y devuelve `MAX(moneda)` (lote A, `EstadoDelParticipanteRepositorio.java:26-35`, no re-abierto) |
| `GET /aportes/grupos/{id}/morosos` | `Morosos` | `BILLETERA_VER` · **no** | — | — | `obligacion_aporte` | mora no transita (DEFECTO-003) |
| `GET /aportes/grupos/{id}/primera-cuota` | `PrimeraCuota` | `BILLETERA_VER` · **no** | — (lo consume `grupos` CU-60) | — | `obligacion_aporte` | habilita el sorteo con cuotas «pagadas» sin QR (FLUJO-QR-001) |
| `GET /aportes/periodos/{id}/recaudo` | `RecaudoDelPeriodo` (`cubiertoMutual`) | `ENTREGA_EJECUTAR` · no | **sin pacto** con `entregas` | — | `obligacion_aporte.monto_pagado` | «confirmado» no es caja (FLUJO-QR-003); RS-01 |
| `GET /aportes/recaudo-diario` | `RecaudoDiario` (importes sin moneda por fila: la moneda es la clave) | `CONTABILIDAD` / `AUDITORIA_LEER` | **sin pacto** con `nucleo` (`RecaudoDelDiaPorHttp`) | backoffice cierres | `pago` | `cierre_diario` sin moneda (lote C, no re-abierto) |
| `POST /aportes/periodos/{id}/obligaciones` | `CalendarioDeObligaciones` | `ADMIN_PLATAFORMA` | — | — | `obligacion_aporte`, `uq_obligacion_periodo_cupo` | disparo manual (HUECO-001) |
| `GET /billetera/cuentas/propia`, `/{c}/saldo`, `/{c}/movimientos` | — | `BILLETERA_VER` · propiedad en el caso de uso (lote C) | inversiones (saldo) | móvil (Dio directo, sin cliente generado) | `cuenta_billetera`, `movimiento_billetera` | saldo desde el libro de billetera, no desde el mayor (DINERO-003) |
| `POST /billetera/cotizaciones` | — | `BILLETERA_OPERAR` | — (`CotizadorPorHttp` → tarifas sin pacto) | — | — | moneda de tarifas ignorada (lote C, no re-abierto) |
| `POST /billetera/recargas` | `EntradaRecarga` (`costoProveedor` del cliente, lote D) | `BILLETERA_OPERAR` | — | `pantalla_recargar.dart` | `orden_recarga`, `uq_recarga_idem` | medios no-QR sin camino de pago (lote D) |
| `POST /billetera/recargas/{id}/acreditacion` | «la confirmación del proveedor» | `TESORERIA` | — | — | `orden_recarga` | descrito como del proveedor, lo dispara un humano (FLUJO-QR-005) |
| `POST /recargas-de-prueba/{id}/pago` | — | `BILLETERA_OPERAR` · 404 sin bean de simulador | — | `cobro_de_recarga.dart` («Simular pago») | — | activo en TEST (perfil `local`) |
| `POST /billetera/retenciones`, `/{id}/cierre` | — | `BILLETERA_OPERAR` | inversiones | — | `retencion_saldo` (sin moneda) | — |
| `POST /billetera/retenciones/{id}/pago-a-tercero`, `/liberacion-de-mercado` | — | `SERVICIO_ENTREGAS` | **sin pacto** (el de entregas cubre solo ofertas) | — | `retencion_saldo` | `liberacion-de-mercado` sin prueba web (`matriz_de_pruebas.py`) |
| `POST /billetera/inversiones/debitos`, `/acreditaciones` | — | `SERVICIO_INVERSIONES` | inversiones | — | `transaccion_billetera` | — |
| `GET /billetera/retiros`, `POST /billetera/retiros`, `POST /billetera/retiros/{id}/aprobacion` | `SalidaRetiro.estado` incluye `EN_REVISION` | `RETIRO_APROBAR` / `BILLETERA_OPERAR` | — | `pantalla_retirar.dart` (destino como UUID a mano), `comprobante_de_retiro.dart`; backoffice retiros | `orden_retiro`, `ck_retiro_doble_aprobacion` | AP-08; nunca se instruye (HUECO-004); el móvil oculta `EN_REVISION` (UX-A11Y-002) |
| `POST /billetera/transferencias` | destino `GRUPO` admitido | `BILLETERA_OPERAR` | — | `pantalla_transferir.dart` | `transferencia_p2p` | `obligacion_pagada` sin consumidor (HUECO-006) |
| `POST /billetera/reversos` | `EntradaReverso.autorizadaPor` | `REVERSO_AUTORIZAR` · **no** | garantía (evento resultante) | — | `reverso_transaccion` | autorizador falsificable (SEG-005); sin asiento (DINERO-003) |
| `GET /billetera/{c}/extracto` | — | `BILLETERA_VER` · sí | — | `pantalla_extracto.dart` | `certificado_saldo` | BOB fijo (DINERO-005) |
| `POST /billetera/cierres` | — | `BILLETERA_OPERAR` | — | — | `solicitud_cierre_billetera` | idempotencia ignorada (lote C, no re-abierto) |
| `POST /billetera/bloqueos` | — | `DATOS_SENSIBLES_LEER` | — | — | `bloqueo_saldo` | permiso de lectura para escribir (SEG-006) |
| `POST /billetera/qr`, `/qr/lecturas`, `/qr/pagos` | QR interno firmado con HMAC | `BILLETERA_OPERAR` | — | — | `qr_transferencia` | — |
| `GET/POST /custodia/conciliaciones` | — | `AUDITORIA_LEER` / `TESORERIA` | — | backoffice custodia | `conciliacion_custodia` | encaje abre por omisión (DEFECTO-005) |
| `GET/POST /contabilidad/cierres-diarios`, `GET /contabilidad/reversos` | montos «sin moneda (A-7)» | `AUDITORIA_LEER` / `CONTABILIDAD` | — | backoffice cierres/reversos | `cierre_diario` (sin moneda) | sello sin saldos (DINERO-006) |

**Eventos**
| Evento | Publica (outbox) | Consume (idempotente) | Pacto | Discrepancias |
|---|---|---|---|---|
| `aportes.aporte_cobrado` | `aportes` CU21 (sí) | `nucleo` `ConsumoDeAportes` (sí, `evento_consumido` + clave `aporte-cobrado:<pagoId>`) | `aportes-nucleo-financiero-aporte-cobrado.json` (coincide campo a campo) | se emite sin pago confirmado (FLUJO-QR-001); el pacto no dice qué hace el consumidor con canal QR (nada) |
| `nucleo_financiero.aporte_no_aplicado` | `nucleo` (sí) | `aportes` (sí) | `aportes-nucleo-financiero.json` | sin DLQ (lote A) |
| `grupos.grupo_iniciado` | `grupos` | `aportes` (sí) | `grupos-aportes-grupo-iniciado.json` + `PactoGrupoIniciadoTest` | — |
| `aportes.recargo_generado`, `reembolso_solicitado`, `reembolso_ejecutado`, `disputa_abierta`, `proveedor_activado`, `obligaciones_generadas`, `aporte_compensado` | `aportes` (sí) | no verificado en este lote | **sin pacto** | `reembolso_ejecutado` puede llevar importe negativo (DINERO-001) |
| `aportes.conciliacion_ejecutada` | sin productor vivo (REESCRITURA-001) | — | — | — |
| `nucleo_financiero.aporte_aplicado`, `transaccion_reversada` | `nucleo` (sí) | `garantia` | `nucleo-financiero-garantia-*.json` | — |
| `nucleo_financiero.obligacion_pagada` | `nucleo` CU12 | **nadie** | — | HUECO-006 |
| `nucleo_financiero.retiro_autorizado` | `nucleo` | **nadie** (`validar_flujos.py`) | — | HUECO-004 |
| `garantia.cobertura_empresarial_*`, `garantia.respaldo_*`, `entregas.desembolso_acreditado` | `garantia`, `entregas` | `nucleo` `ConsumidorDeAsientosKafka` (on en Coolify) | **sin pacto** | RESPALDO-003; el pozo vive en dos cuentas lógicas (`SALDO_GRUPOS` y `POZO_GRUPO`, lote B, no re-abierto) |
| `identidad.usuario_registrado`, `grupos.grupo_creado` | otros | `nucleo` `AperturaDeBilleteras` | **sin pacto** | — |

Prueba de contrato del lado productor: existe para `aporte_cobrado` (`CU21EventoDeAporteTest#cumpleElPactoConNucleo`) y `grupo_iniciado`. Las llamadas HTTP de `entregas` → `aportes` (recaudo) y `entregas` → `nucleo` (pago a tercero, liberación) **no tienen pacto** (regla 98.1.4). Según el lote C, varias descripciones de pacto nombran tests que no existen (`PactoConInversionesTest`, `EntregasPublicaLaOfertaTest`); no re-abierto.

## 7. Evidencia obligatoria del módulo
| Exigencia (91.6 · 98.8 · 90.6) | Estado | Detalle |
|---|---|---|
| Asientos que genera cada operación de dinero y su suma | NO COMPROBADO | Por lectura: aporte por billetera `DEBE SALDO_USUARIOS / HABER SALDO_GRUPOS` (o `RESERVA_RESPALDO` si es recargo); aporte por QR, recarga, transferencia, reverso y retiro: **sin asiento** (FLUJO-QR-004, DINERO-003). No ejecutado. |
| Idempotencia ejercitada (misma operación dos veces) | NO COMPROBADO | Existen `CU21Test`, `CU100WebhookTest.eventoRepetidoEsIdempotente`, 47 archivos de `nucleo` que tratan idempotencia; no corrieron (Gradle bloqueado). |
| Cuadre del mayor contra el total esperado | NO COMPROBADO | Sin base. La invariante en base tiene huecos (DINERO-004). |
| Caso de reversa | NO COMPROBADO | `CU14` no genera contra-asiento en el mayor (DINERO-003). |
| Redondeo declarado | PASS (por lectura) | `Dinero.dividir`/`por` exigen `RoundingMode` explícito y rechazan `UNNECESSARY`; la construcción falla si hay más de 2 decimales (`Dinero.java:36-44,72-115`). El reparto de residuo no se ejercitó. |
| Prueba de contrato | PARCIAL (existe, no ejecutada) | Ver sección 6. |
| Prueba de duplicado de mensaje | NO COMPROBADO | Consumidores con `evento_consumido` en la misma transacción (lectura). |
| Servicio dependido apagado | NO COMPROBADO | `CalendarioDelPeriodoCaidaTest` existe; no ejecutado; sin Docker. |
| Traza con `correlationId` | NO COMPROBADO | El outbox propaga `traza().id()` (`CU21CobrarAporte.java:168`); sin ejecución. |
| Matriz de autorización negativa | FAIL (por lectura) | Faltan los casos de propiedad (SEG-002); `scripts/verificar_pruebas_web.py`: «42 controladores con matriz propia» mide existencia, no propiedad. |

**Salidas de los verificadores del repo** (literal, recortado; el resto en el scratchpad de la sesión, no versionado):
```text
$ python3 scripts/validar_flujos.py                         # exit 0
  OK    · aporte-mora-cobertura.yaml: 12 pasos, 6 validos, 7 limite, 7 invalidos
  OK    · recarga-y-retiro.yaml: 6 pasos, 3 validos, 5 limite, 6 invalidos
  AVISO · recarga-y-retiro.yaml: paso `instruir-pago` SIN CABLEAR — H8-01 · Ningún trabajo, endpoint ni consumidor llama a RetirosConProveedor.despachar ni a CU11RetirarSaldo.instruirPago en producción; el evento nucleo_financiero.retiro_autorizado se publica y nadie lo consume. …
$ python3 scripts/verificar_criterios.py                    # exit 0
casos de uso implementados y verificados: 108
pendientes de implementar (3): CU-05 (identidad), CU-51 (aportes), CU-57 (nucleo-financiero)
$ python3 scripts/inventario_endpoints.py --check           # exit 0
endpoints relevados: 350 · hallazgos: 0
$ python3 scripts/matriz_de_pruebas.py --servicio aportes   # exit 0
| aportes | 15 | 15 | 12 | 10 | 2 |
$ python3 scripts/matriz_de_pruebas.py --servicio nucleo-financiero   # exit 0
| nucleo-financiero | 29 | 28 | 25 | 24 | 4 |
Sin prueba web (1):  POST /billetera/retenciones/{retencionId}/liberacion-de-mercado  (liberarRetencionDelMercado)
$ python3 scripts/verificar_seguridad.py                    # exit 0
  AVISO · servicios/nucleo-financiero/…/ReversoRepositorio.java:73 — DSL.field(variable) — nombre de columna dinámico
  AVISO · servicios/nucleo-financiero/…/DestinoRepositorio.java:48 — DSL.field(variable) …
  AVISO · servicios/nucleo-financiero/…/DestinoRepositorio.java:62 — DSL.field(variable) …
TODO OK · 7 aviso(s)
$ python3 scripts/verificar_contratos_limites.py            # exit 0
  - nucleo-financiero · openapi/*.yaml#/components/schemas/EntradaBloqueo/documentoUrl: sin limite (`maxLength`/`maximum`/`maxItems`)
$ python3 scripts/auditar_backend.py                        # exit 0
    - servicios/aportes/…/SecretoWebhookDeDesarrollo.java: `aportaya.aportes.webhook.secreto-de-prueba` es un secreto y solo tiene valor por omision
    - aportes: CU100 sin prueba de caso de uso
EVIDENCIA DE EJECUCION  ·  0/393 clases de prueba corrieron y pasaron
NOTA GLOBAL: 9.64/10   ·   con 0/393 pruebas verificadas
$ python3 scripts/verificar_boveda.py                       # exit 0 · TODO OK
$ python3 scripts/verificar_carriles.py                     # exit 0 · TODO OK (con avisos ajenos al módulo)
$ python3 scripts/verificar_frontend.py                     # exit 0 · TODO OK
$ python3 scripts/verificar_pruebas_web.py                  # exit 0
operaciones con el estado HTTP fuera de su contrato: 25
$ python3 scripts/verificar_rutas_frontend.py               # exit 1 — falla por `GET /grupos/solicitudes-escaladas` (no es de este módulo)
$ python3 scripts/cobertura.py                              # exit 0 — «Sin informes. Corré primero: ./gradlew test webTest integrationTest»
$ python3 scripts/listar_topicos.py                         # exit 0 (269 líneas; usado para el mapa de eventos)
```
Los avisos de `DSL.field(variable)` (`ReversoRepositorio.java:73`, `DestinoRepositorio.java:48,62`) no se re-abrieron: quedan como sospecha de inyección por nombre de columna, a revisar por el dueño del servicio. El verificador `verificar_pruebas_web.py` informa 25 operaciones con estado HTTP fuera de contrato en todo el repo; no se desglosó cuántas son de este módulo.

## 8. Cobertura por clase
| Clase | Qué se revisó y cómo | Hallazgos |
|---|---|---|
| APROB | grep `aprobar/aprobaci/APROBAD/PENDIENTE_/comprobante/habilit/confirmar/requiere_doble_aprobacion` en main, SQL y pantallas; lectura de cada coincidencia | 0 como clase propia; 14 puntos en el inventario 4a, catalogados bajo SEG, HUECO, FLUJO-QR y UX-A11Y |
| RESPALDO | grep `respaldo/cobertura/cubrir/faltante/garantia` en main, SQL, compose, semillas, textos | 5 |
| FLUJO-QR | lectura completa de CU21, CU100, `ConsumoDeAportes`, recarga; recorrido de los saltos | 5 |
| FLUJO-FALTANTE | lectura de `RecaudoDelPeriodo`, consumidor de asientos; sección 4d | 0 como clase propia (ramas descritas en 4d y catalogadas en FLUJO-QR-003, RESPALDO-003) |
| COHERENCIA | contraste ADR-051, ADR-054, ADR-049, catálogo `aporte-mora-cobertura.yaml`, javadocs y textos de pantalla | 0 como clase propia (las contradicciones quedaron en RESPALDO-002/004, UX-A11Y-002, DINERO-006, FLUJO-QR-005) |
| DUP-INT | lectura completa y comparación de caminos de retiro y de clave de idempotencia | 1 |
| DUP-X | comparación de `Dinero`/formateo con `plataforma` y `packages` (lote D: tres implementaciones de «cadena a centavos», riesgo bajo; no catalogado) | 0 catalogados |
| REESCRITURA | grep de llamadores de cada caso de uso | 1 (+ HUECO-004, HUECO-006, DUP-INT-001) |
| PISADA | lectura de comentarios de carriles y del historial (`git log` completo disponible) | 0 catalogados; señales: comentarios que se contradicen con el código (DINERO-006, `CuentasLogicasConfiguradas`, `LibroDeBilletera` según lote C) |
| PARCHE | lectura | 0 como clase propia (SEG-001: doble confinado por perfil y TEST en perfil de desarrollo) |
| FRAGIL | cadenas mágicas de estado/tipo entre capas (`RECARGO_MORA`, canales, estados) | 0 como clase propia (DEFECTO-002: enum vs CHECK) |
| DEFECTO | lectura completa | 5 |
| HUECO | lectura completa + grep de llamadores | 6 |
| RESILIENCIA | timeouts, circuitos, catch, DLQ, outbox | 1 (+ HUECO-002) |
| DINERO | las cinco prohibiciones 91.1 y las invariantes 91.2 en SQL y código | 6 |
| DEBIDO-PROCESO | recargo y mora | 1 |
| DDD | lenguaje ubicuo (`recargo`/`comisión`, `SALDO_GRUPOS`/`POZO_GRUPO`) | 0 como clase propia (DEFECTO-001) |
| ARQ | ArchUnit existente; lectura de dependencias de capa (lote B: aplicación importa repositorios concretos e instancia con `new`, no re-abierto) | 0 catalogados |
| CLEAN | `TODO/FIXME`, código muerto, comentarios falsos | 0 como clase propia (comentarios falsos en DINERO-006) |
| ACOPLE | grep de `DSL.name(` y SQL por esquema; FK entre esquemas | 1 |
| CONTRATO | sección 6 | 1 |
| DATOS | DDL, claves, append-only, semillas | 1 (+ DINERO-004, RESPALDO-005) |
| RENDIMIENTO | lote B/C: N+1 en cierre diario y folio `count(*)+1`; no re-abiertos | 0 catalogados |
| PRUEBAS | inventario de tests y verificadores | 1 |
| UX-A11Y | pantallas de cobro y billetera (lote D) | 2 |
| OBSERV | logs de los casos de uso de dinero | 1 |
| CONFIG | descriptores, `application*.yml`, compose | 1 (+ SEG-001) |
| DOC | READMEs, javadocs, catálogos | 0 como clase propia (incluido en RESPALDO-002 y DINERO-006) |
| SEG | permisos y propiedad por endpoint, webhook, secretos | 6 |
| TRAZA | append-only y bitácoras | 0 como clase propia (DATOS-001) |
| REG | ver sección 11 | 0 catalogados: los riesgos regulatorios se anotan en AP-08, AP-11 y A-1 («norma a identificar por Cumplimiento»); habilitación de dinero real sigue condicionada al dossier H15 |
| OTRO | — | 0 |

## 9. Lo que está bien
- `CU100RecibirWebhookPasarela` valida la firma HMAC sobre el cuerpo crudo antes de leerlo, aplica una ventana de ±5 min, deduplica por `(proveedor_id, clave)` con índice único, y nunca acredita «lo que vino»: compara monto y moneda contra el pago registrado (`:90-163`).
- `CU21CobrarAporte` bloquea la obligación, revalida la idempotencia tras el candado y usa versión optimista (`:65-131`); el evento sale por outbox en la misma transacción (`:134`).
- `Dinero` (plataforma) usa `BigDecimal` con escala fija, exige regla de redondeo explícita y compara por `compareTo` (`Dinero.java:36-44,72-115,129-131`).
- No se encontró `float`/`double` para importes en ninguna capa del módulo (Java, Dart, TypeScript de cobro y billetera); los importes viajan como cadena de punta a punta (lotes B, C, D).
- El mayor (`asiento_contable`, `movimiento_contable`) y el libro de billetera son append-only con trigger y `REVOKE` (`sql/35_append_only`, `03_permisos`), con cuadre diferido por asiento y monomoneda (con los huecos de DINERO-004).
- Índices únicos con nombre para la idempotencia: `uq_webhook_idem`, `uq_pago_idem`, `uq_obligacion_periodo_cupo`, `uq_tx_idem`, `uq_recarga_idem`, `uq_retiro_idem`; advisory lock en el libro.
- Todos los eventos salen por outbox en la misma transacción; los consumidores registran `evento_consumido` en la misma transacción.
- Clientes HTTP de `aportes` con timeout, circuito y denegación por omisión; la red queda fuera de las transacciones.
- La auto-aprobación de retiros y de reembolsos está bloqueada por sesión (`CU19ReembolsarPago.java:128`, `ck_retiro_doble_aprobacion`).
- El simulado (`packages/simulado`) no llega a builds de producción; la pantalla con saldo se protege de capturas; los botones de pago, retiro y aprobación se bloquean durante el envío.
- Ninguna prueba está deshabilitada; las pruebas de integración usan PostgreSQL real (Testcontainers).

## 10. Observado fuera de alcance
- `despliegue/TEST.md:458` — `nucleo-financiero` no arranca fuera de `local`/`test` porque no existe `ProveedorDeRetiro` productivo (se audita en este lote en la tanda siguiente).
- `servicios/cumplimiento/.../LimiteRepositorio.java:67,99` y `servicios/erp/.../PeriodoRepositorio.java:149-150`, `PresupuestoRepositorio.java:113-115,172` leen tablas del esquema `nucleo_financiero` (catalogado del lado de este módulo en ACOPLE-001; el arreglo es de L4/L5).
- `servicios/garantia` y `servicios/entregas`: proponen asientos con la cuenta lógica `POZO_GRUPO` (`entregas/.../AsientoDeEntrega.java:40`, `garantia/.../AsientoPropuesto.java:48`), distinta de la `SALDO_GRUPOS` que usa `nucleo` (lote B; L2).
- `apps/movil/lib/navegacion/vigilante_de_aprobacion.dart:41` — aprobación de KYC (L4).
- `apps/web/src/app/paginas/inicio/secciones/transparencia.ts:65` — ejemplo con un nombre de persona presentado como caso real (lote D; L6).
- `apps/backoffice/.../contabilidad/dominio/cu101-presupuestos.ts:56-60` y `contrato-erp.ts:42` operan importes con `Number` (ERP de la empresa, no dinero de participantes; L5).
- `scripts/verificar_rutas_frontend.py` sale con exit 1 por la lista de conocidas desactualizada (`GET /grupos/solicitudes-escaladas`, L3).

## 11. Límites de esta auditoría
- **Nada se ejecutó del código del módulo.** `./gradlew --no-daemon :servicios:aportes:test :servicios:aportes:webTest :servicios:nucleo-financiero:test :servicios:nucleo-financiero:webTest --continue` → `Could not resolve org.postgresql:postgresql:42.7.12 … Received status code 429 from server: Too Many Requests` · `BUILD FAILED in 38s` (un intento, según el metaprompt). Sin daemon de Docker (`failed to connect to the docker API at unix:///var/run/docker.sock`): `integrationTest` imposible. No se corrieron los barridos de Gradle (tamaño de archivos, umbrales, secretos) por la misma causa.
- **Front sin ejecutar.** `yarn typecheck` → `This project's package.json defines "packageManager": "yarn@4.18.0". However the current global version of Yarn is 1.22.22.`; sin `node_modules`. `yarn lint` no se intentó por la misma causa. Flutter no está instalado.
- `scripts/idempotencia.py` no es un verificador: es una biblioteca del generador de DDL.
- **Cobertura de lectura.** Main de los dos servicios: completo, salvo los OpenAPI (parciales: rutas, esquemas de dinero y las operaciones de cobro, recarga, retiro y reverso). Pruebas: inventariadas por grep, no leídas línea a línea (215 archivos, ≈30.800 líneas). Tablas de `10_billetera_custodia`: por grep de importes, moneda, CHECK y append-only, no línea a línea. Pantallas de backoffice de custodia, cierres y reversos: vistas y recibos no leídos (lote D). `clientes/{angular,dart}` no versionados.
- **Lectura repartida.** Cuatro subagentes leyeron los lotes; cada hallazgo del informe se re-abrió en la sesión principal. Las fichas dicen «no re-abierto» donde una afirmación secundaria viene solo del subagente.
- **Supuestos.** (1) TEST corre con `PERFIL_SPRING=local` según `despliegue/TEST.md:458`; sin acceso a Coolify. (2) `garantia` publica en TEST los eventos de cobertura (RESPALDO-003); lo confirma L2. (3) Las tablas de `03_aportes_pagos_qr` del mayor (`asiento_contable`, `movimiento_contable`, `cuenta_contable`, `cierre_diario`) son de `nucleo-financiero`, aunque estén en la carpeta de aportes. (4) La lista de «textos de la web» de RESPALDO-002 incluye tres archivos citados por el subagente sin re-abrir.
- **No cubierto.** Interior de `grupos`, `garantia`, `entregas`, `tarifas`, `cumplimiento`, `inversiones`; los pagos de `TARJETA`/`TRANSFERENCIA_BANCARIA` como integraciones (no existen); la configuración del enmascarador de PII de logs (OBSERV-001 queda sin comprobar contra él); lectores de pantalla y capturas (regla 95.7: ningún hallazgo visual llega a `VERIFIED`).
- **Proceso.** El control `blocker_gate.py` (regla 65) señala microtareas `BLOQUEADO`/`EN CURSO` de **otros** planes (`docs/trabajo/2026-10-03-…`, `2026-10-14-…`, `2026-10-16-…`, `2026-10-27-…`, `2026-10-28-…`, `2026-10-30-…`). No se tocaron: el metaprompt prohíbe escribir fuera de esta carpeta.
- No se declara conformidad ni no conformidad con ninguna norma. La habilitación de dinero real sigue condicionada al dossier H15 (`docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-F/H15-dossier-habilitacion-real.md`, no leído en esta auditoría).
- Las severidades se fijaron por daño potencial y no se ejercitaron; el rango de confianza está en cada ficha.

### Refutación (Fase 7)
Cada S1 y S2 se re-abrió en la ubicación citada. Resultado:
- **Retirados o rebajados.** (a) «La transferencia P2P marca aportes pagados sin QR» (lote B, S1 sospecha): ningún servicio consume `obligacion_pagada` → baja a HUECO-006, S3. (b) «El comprobante del aporte muestra siempre confirmado» (lote D, S2): el chip sí muestra el estado de la obligación → S3 (UX-A11Y-002). (c) «Reverso y retiro nunca llaman a cu24» (lote B): `ResolucionDeRetiro` y `CU13RetenerSaldo` lo mencionan, pero solo en comentarios; se mantiene para recarga, transferencia y reverso (DINERO-003). (d) `EFECTIVO_AL_ORGANIZADOR`/`FONDO_GARANTIA` (lote A, S1) → S2 probable, porque falta confirmar que algún cliente los envíe (la app no los ofrece: `pantalla_aportar.dart:23-36`).
- **Falsos positivos típicos descartados.** Autorización en RLS: las tablas de `aportes` no tienen política de fila (`restricciones.sql:1405-1420` solo cubre tablas con `usuario_id`/`cuenta_billetera_id`). Append-only en la base: ninguna tabla de `aportes` aparece en `sql/35_append_only`. CHECK en `40_reglas`: no hay CHECK de signo para `reembolso.monto`, `monto_pagado` ni `debe`/`haber`. Código muerto referenciado por Kafka, trabajo o plantilla: se buscó en `consumo/`, `trabajos/` y `web/` antes de afirmar «sin llamador» (HUECO-001, HUECO-004, REESCRITURA-001). Cálculos con `double` de presentación: no se encontró ninguno usado para dinero.
- **Pasada inversa.** Las clases sin hallazgo propio (FLUJO-FALTANTE, COHERENCIA, DUP-X, PISADA, PARCHE, FRAGIL, DDD, ARQ, CLEAN, RENDIMIENTO, DOC, TRAZA, REG, OTRO) se revisaron: sus casos quedaron catalogados bajo la clase de la causa o se anotaron como no re-abiertos (sección 8).

## 12. Anexo de cobertura de archivos
Una fila por archivo de la huella de los dos servicios (`find servicios/aportes servicios/nucleo-financiero -type f -not -path '*/build/*'`). El SQL, los pactos y las pantallas se listan en la sección 3, con su grado de lectura en la sección 11.

| Ruta | Líneas | Leído completo | Qué hace |
|---|---:|---|---|
| `servicios/aportes/.jqwik-database` | 0 | sí | configuración / descriptor / documentación del servicio |
| `servicios/aportes/README.md` | 97 | sí | configuración / descriptor / documentación del servicio |
| `servicios/aportes/build.gradle.kts` | 81 | sí | configuración / descriptor / documentación del servicio |
| `servicios/aportes/descriptor.yml` | 30 | sí | configuración / descriptor / documentación del servicio |
| `servicios/aportes/gradle.lockfile` | 220 | sí | configuración / descriptor / documentación del servicio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/Aplicacion.java` | 21 | sí | configuración / descriptor / documentación del servicio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/.gitkeep` | 0 | sí (vacío) | marcador de carpeta |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU100RecibirWebhookPasarela.java` | 224 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU19ReembolsarPago.java` | 235 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21AbrirPrimeraCuota.java` | 114 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CobrarAporte.java` | 275 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU21CompensarAporte.java` | 171 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CU99EnrutarProveedor.java` | 205 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/CalendarioDelPeriodo.java` | 40 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/ConciliarLoteTripartito.java` | 142 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/ConsultarEstadoDelParticipante.java` | 42 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/ConsultarPlataformaDePagos.java` | 33 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/ConsultarPrimeraCuota.java` | 34 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/ConsultarRecaudoDelPeriodo.java` | 57 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/ConsultarRecaudoDiario.java` | 53 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/GenerarObligacionesDelPeriodo.java` | 109 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/HechosDeGrupos.java` | 17 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/IndicadoresComercialesDeAportes.java` | 35 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/IndicadoresDeMora.java` | 67 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/aplicacion/RegistroDelCalendario.java` | 142 | sí | caso de uso / servicio de aplicación |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/consumo/ConsumidorDeAportesNoAplicadosKafka.java` | 94 | sí | consumidor Kafka |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/consumo/ConsumidorDeGrupoIniciadoKafka.java` | 125 | sí | consumidor Kafka |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/.gitkeep` | 0 | sí (vacío) | marcador de carpeta |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/ConciliacionTripartita.java` | 226 | sí | modelo o puerto de dominio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/EstadoDePagos.java` | 31 | sí | modelo o puerto de dominio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/MotivoDeReembolso.java` | 38 | sí | modelo o puerto de dominio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/PlataformaDePagos.java` | 33 | sí | modelo o puerto de dominio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/PrimeraCuotaDelGrupo.java` | 15 | sí | modelo o puerto de dominio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/ProveedorDeSecretoWebhook.java` | 24 | sí | modelo o puerto de dominio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/RecargoDeMora.java` | 67 | sí | modelo o puerto de dominio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/RecaudoDelPeriodo.java` | 74 | sí | modelo o puerto de dominio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/SaldoDeLaObligacion.java` | 50 | sí | modelo o puerto de dominio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/TipoDeDisputa.java` | 29 | sí | modelo o puerto de dominio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/VencimientoDeObligacion.java` | 85 | sí | modelo o puerto de dominio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/dominio/VerificadorDeFirmaWebhook.java` | 66 | sí | modelo o puerto de dominio |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/.gitkeep` | 0 | sí (vacío) | marcador de carpeta |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/AuditoriaRepositorio.java` | 66 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/CalendarioDeObligacionesRepositorio.java` | 92 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/ConciliacionRepositorio.java` | 105 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/EstadoDelParticipanteRepositorio.java` | 66 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/FeriadosRepositorio.java` | 49 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/IndicadoresDeAportesRepositorio.java` | 90 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/ObligacionRepositorio.java` | 217 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/PagoRepositorio.java` | 247 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/PlataformaDePagosRepositorio.java` | 55 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/PrimeraCuotaRepositorio.java` | 53 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/ProveedorPagoRepositorio.java` | 96 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/RecaudoDiarioRepositorio.java` | 68 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/RecaudoRepositorio.java` | 64 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/SecretoWebhookDeDesarrollo.java` | 33 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/SecretoWebhookSinConfigurar.java` | 29 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/WebhookRepositorio.java` | 79 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/clientes/CalendarioDelPeriodoPorHttp.java` | 103 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/clientes/ClienteConCircuito.java` | 128 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/clientes/ConfiguracionDeCircuito.java` | 28 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/infraestructura/clientes/HechosDeGruposPorHttp.java` | 48 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/trabajos/RecargosDeMoraDiarios.java` | 61 | sí | trabajo programado |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/web/.gitkeep` | 0 | sí (vacío) | marcador de carpeta |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/web/AportesController.java` | 278 | sí | controlador o mapeo HTTP |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/web/MapeoDeAportes.java` | 37 | sí | controlador o mapeo HTTP |
| `servicios/aportes/src/main/java/bo/aportaya/aportes/web/PagosController.java` | 226 | sí | controlador o mapeo HTTP |
| `servicios/aportes/src/main/resources/application.yml` | 72 | sí | configuración |
| `servicios/aportes/src/main/resources/openapi/aportes.yaml` | 808 | parcial (cabecera, rutas, esquemas de dinero y cobro/recarga/retiro/reverso) | contrato OpenAPI |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/AislamientoEsquemaTest.java` | 165 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/ArquitecturaTest.java` | 60 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/ArranqueTest.java` | 75 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/AuditoriaCriticaTest.java` | 104 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/BarridoTest.java` | 28 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/BaseDeAportes.java` | 122 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/BaseDeCalendario.java` | 76 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/BaseDeCompensacion.java` | 145 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU100WebhookTest.java` | 227 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU132ConciliacionTest.java` | 208 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU132RecaudoTest.java` | 192 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU19RechazosTest.java` | 244 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU19Test.java` | 227 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU21CalendarioConciliacionTest.java` | 169 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU21CalendarioRechazosTest.java` | 139 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU21CalendarioTest.java` | 148 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU21CompensaRechazosTest.java` | 125 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU21CompensaTest.java` | 108 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU21EventoDeAporteTest.java` | 166 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU21PrimeraCuotaTest.java` | 208 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU21RecargosDiariosTest.java` | 65 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU21RechazosTest.java` | 217 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU21ReintentoConcurrenteTest.java` | 89 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU21Test.java` | 283 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU21ZonaHorariaTest.java` | 135 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU51RecaudoDiarioTest.java` | 183 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU99RechazosTest.java` | 233 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CU99Test.java` | 235 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CUIndicadoresComercialesDeAportesTest.java` | 97 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/CUIndicadoresDeMoraTest.java` | 119 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/FixturaDeAportes.java` | 241 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/PlataformaDePagosRepositorioTest.java` | 85 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/consumo/ConsumidorDeAportesNoAplicadosKafkaTest.java` | 115 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/consumo/ConsumidorDeGrupoIniciadoKafkaTest.java` | 84 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/consumo/PactoGrupoIniciadoTest.java` | 79 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/dominio/ConciliacionTripartitaTest.java` | 167 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/dominio/RecaudoDelPeriodoTest.java` | 109 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/dominio/TipoDeDisputaTest.java` | 21 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/dominio/VencimientoDeObligacionTest.java` | 89 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/dominio/VerificadorDeFirmaWebhookTest.java` | 80 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/infraestructura/clientes/CalendarioDelPeriodoCaidaTest.java` | 138 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/infraestructura/clientes/CalendarioDelPeriodoPorHttpTest.java` | 178 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/web/AportesConsultasWebTest.java` | 162 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/web/AportesControllerWebTest.java` | 261 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/web/CalendarioWebTest.java` | 161 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/web/PagosControllerWebTest.java` | 210 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/web/PagosEnrutamientoYWebhookWebTest.java` | 130 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/web/PlataformaDePagosWebTest.java` | 105 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/web/PrimeraCuotaWebTest.java` | 115 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/web/RecaudoDiarioWebTest.java` | 119 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/web/RecaudoWebTest.java` | 113 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/java/bo/aportaya/aportes/web/SeguridadWebTest.java` | 63 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/aportes/src/test/resources/fixtures/.gitkeep` | 0 | no línea a línea (inventario de métodos por grep; cuerpos de `BarridoTest`, `CU21Test:185-283`, `CU99Test:160-235` leídos) | prueba de aportes |
| `servicios/nucleo-financiero/.jqwik-database` | 0 | sí | configuración / descriptor / documentación del servicio |
| `servicios/nucleo-financiero/README.md` | 165 | sí | configuración / descriptor / documentación del servicio |
| `servicios/nucleo-financiero/build.gradle.kts` | 53 | sí | configuración / descriptor / documentación del servicio |
| `servicios/nucleo-financiero/descriptor.yml` | 30 | sí | configuración / descriptor / documentación del servicio |
| `servicios/nucleo-financiero/gradle.lockfile` | 208 | sí | configuración / descriptor / documentación del servicio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/Aplicacion.java` | 27 | sí | configuración / descriptor / documentación del servicio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/AperturaDeBilleteras.java` | 197 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/AporteCobrado.java` | 21 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/BandejaDeRetiros.java` | 75 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU10RecargarSaldo.java` | 290 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU10RechazarRecarga.java` | 61 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU11InstruirRetiro.java` | 96 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU11RetirarSaldo.java` | 261 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU12AcreditarRescate.java` | 171 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU12TransferirSaldo.java` | 227 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU13EjecutarConDestino.java` | 294 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU13LiberarRetencionDelMercado.java` | 53 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU13RetenerSaldo.java` | 284 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU14ReversarTransaccion.java` | 217 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU15EmitirExtracto.java` | 223 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU16CerrarBilletera.java` | 178 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU17BloquearPorAutoridad.java` | 171 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU24RegistrarAsiento.java` | 204 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU40EvaluarLimites.java` | 165 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU50ConciliarCustodia.java` | 188 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU51EjecutarCierreDiario.java` | 204 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CierreDiarioDesdeContabilidad.java` | 59 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CodigosDeRetiro.java` | 20 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ConsultarMovimientosBilletera.java` | 74 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ConsultarSaldo.java` | 70 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ConsultarTesoreria.java` | 179 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ConsumoDeAportes.java` | 296 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ConsumoDeAsientos.java` | 203 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ConsumoDeEvidenciaMfa.java` | 40 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CotizarOperacion.java` | 177 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CuentaPropia.java` | 55 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CuentasDelDestino.java` | 40 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CuerpoDeRetencion.java` | 45 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/DestinoDelCobro.java` | 53 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/IndicadoresFinancieros.java` | 51 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/LecturaDeSaldo.java` | 48 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/OrdenesExistentes.java` | 59 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/PagoDeCompraEnMercado.java` | 54 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/PagoDeRecargaDePrueba.java` | 56 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/PoliticaDeTitular.java` | 29 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/QrDeLaRecarga.java` | 43 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/QrInterno.java` | 280 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/RecargasConProveedor.java` | 205 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/RegistrarDiscrepancia.java` | 82 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ReglasDeEjecucion.java` | 89 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ReglasDeRecarga.java` | 49 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ReglasDeRetiro.java` | 58 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/RepartoDeCompraEnMercado.java` | 35 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ResolucionDeRetiro.java` | 291 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/ResolverDestino.java` | 51 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/RetirosConProveedor.java` | 226 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/SolicitudDeRetiro.java` | 176 | sí | caso de uso / servicio de aplicación |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/consumo/ConsumidorDeAportesKafka.java` | 120 | sí | consumidor Kafka |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/consumo/ConsumidorDeAsientosKafka.java` | 83 | sí | consumidor Kafka |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/consumo/ConsumidorDeGruposKafka.java` | 45 | sí | consumidor Kafka |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/consumo/ConsumidorDeUsuariosKafka.java` | 45 | sí | consumidor Kafka |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/consumo/SobreDeEvento.java` | 62 | sí | consumidor Kafka |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/ClasificadorDeQr.java` | 47 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/CondicionesDeRetiro.java` | 90 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/ContenidoDeQr.java` | 91 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/CostoDeOperacion.java` | 46 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/CuadrarPartidas.java` | 61 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/DiscrepanciaDelProveedor.java` | 45 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/EstadoDeRetiro.java` | 44 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/EvaluacionDeTope.java` | 64 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/HuellaDePeticion.java` | 33 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/OrigenAsiento.java` | 14 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/Partida.java` | 48 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/RatioDeCobertura.java` | 46 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/VentanaDeLimite.java` | 53 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/VigenciaDeRetencion.java` | 61 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/CotizadorDeComision.java` | 48 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/CuentasLogicas.java` | 18 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/HechosDeOtrosServicios.java` | 43 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/OfertasDelMercado.java` | 24 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/ProveedorDeRecargas.java` | 39 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/ProveedorDeRetiro.java` | 50 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/ProveedorDeRetiros.java` | 25 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/RecaudoDelDia.java` | 26 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/SegundoFactor.java` | 28 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/SimuladorDePagoDeRecarga.java` | 15 | sí | modelo o puerto de dominio |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/AprobacionDeRetiroRepositorio.java` | 44 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/AsientoRepositorio.java` | 118 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/BandejaDeRetirosRepositorio.java` | 74 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/BloqueoDeIdempotencia.java` | 21 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/BloqueoRepositorio.java` | 89 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/CierreDiarioRepositorio.java` | 79 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/CierreRepositorio.java` | 93 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/ClienteProveedorSimulado.java` | 246 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/ConciliacionRepositorio.java` | 91 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/CotizadorPorHttp.java` | 222 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/CuentaBilleteraRepositorio.java` | 212 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/CuentaContableRepositorio.java` | 31 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/CuentasDeSistemaRepositorio.java` | 70 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/CuentasLogicasConfiguradas.java` | 33 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/DestinoRepositorio.java` | 67 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/DiscrepanciaRepositorio.java` | 56 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/EvidenciaMfaConsumidaRepositorio.java` | 36 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/ExtractoRepositorio.java` | 127 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/GuardiaDeCuentasLogicas.java` | 33 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/IndicadoresDelMayorRepositorio.java` | 58 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/LibroDeBilletera.java` | 291 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/LimiteRepositorio.java` | 153 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/MovimientoBilleteraRepositorio.java` | 49 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/OrdenRecargaRepositorio.java` | 240 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/OrdenRetiroRepositorio.java` | 299 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/ProveedorDeRetiroLocal.java` | 83 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/QrRepositorio.java` | 130 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/RecargasProveedorLocal.java` | 82 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/RecargasProveedorSimulado.java` | 52 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/RegistroDeIdempotencia.java` | 90 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/RetencionRepositorio.java` | 157 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/RetirosProveedorSimulado.java` | 39 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/ReversoRepositorio.java` | 173 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/SegundoFactorLocal.java` | 47 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/SegundoFactorStepUp.java` | 124 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/TesoreriaRepositorio.java` | 221 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/TransferenciaRepositorio.java` | 93 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/clientes/HechosPorHttp.java` | 143 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/clientes/OfertasPorHttp.java` | 56 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/clientes/RecaudoDelDiaPorHttp.java` | 79 | sí | adaptador (repositorio jOOQ, cliente HTTP, doble) |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/trabajos/.gitkeep` | 0 | sí (vacío) | marcador de carpeta |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/trabajos/ReconciliacionDeRetiros.java` | 94 | sí | trabajo programado |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/.gitkeep` | 0 | sí (vacío) | marcador de carpeta |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/BilleteraController.java` | 296 | sí | controlador o mapeo HTTP |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/ContabilidadController.java` | 125 | sí | controlador o mapeo HTTP |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/CustodiaController.java` | 94 | sí | controlador o mapeo HTTP |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/FondeoDeLaBilletera.java` | 248 | sí | controlador o mapeo HTTP |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/IdentidadDeServicio.java` | 23 | sí | controlador o mapeo HTTP |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/MapeoDeBilletera.java` | 176 | sí | controlador o mapeo HTTP |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/MovimientosDeLaBilletera.java` | 166 | sí | controlador o mapeo HTTP |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/QrDeLaBilletera.java` | 82 | sí | controlador o mapeo HTTP |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/RecargasDePruebaController.java` | 50 | sí | controlador o mapeo HTTP |
| `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/RutasDelMercadoDeLaBilletera.java` | 36 | sí | controlador o mapeo HTTP |
| `servicios/nucleo-financiero/src/main/resources/application-local.yml` | 21 | sí | configuración |
| `servicios/nucleo-financiero/src/main/resources/application-production.yml` | 7 | sí | configuración |
| `servicios/nucleo-financiero/src/main/resources/application-staging.yml` | 10 | sí | configuración |
| `servicios/nucleo-financiero/src/main/resources/application-test.yml` | 12 | sí | configuración |
| `servicios/nucleo-financiero/src/main/resources/application.yml` | 151 | sí | configuración |
| `servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml` | 1547 | parcial (cabecera, rutas, esquemas de dinero y cobro/recarga/retiro/reverso) | contrato OpenAPI |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/AppendOnlyTest.java` | 103 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/ArquitecturaTest.java` | 60 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/ArranqueProduccionTest.java` | 92 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/ArranqueRecargaQrTest.java` | 115 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/ArranqueTest.java` | 78 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/AutorizacionNegativaTest.java` | 299 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/BarridoTest.java` | 28 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/BaseDeApertura.java` | 86 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/BaseDeAportes.java` | 194 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/BaseDeBilletera.java` | 263 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/BaseDeCU24.java` | 119 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/BaseDeEventosDeAsientos.java` | 87 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/BaseDeInversiones.java` | 126 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/BaseDeLibroInvariantes.java` | 57 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/BaseDeMercado.java` | 49 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/BaseDeQr.java` | 107 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/BaseDeRecargo.java` | 60 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10ConcurrenciaTest.java` | 87 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10CotizacionTest.java` | 272 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10DiscrepanciaTest.java` | 251 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10IdempotenciaTest.java` | 158 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10ProveedorTest.java` | 191 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10QrDinamicoTest.java` | 205 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10RechazosTest.java` | 241 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10ServidorRealTest.java` | 247 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10Test.java` | 297 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU11AprobacionConcurrenciaTest.java` | 177 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU11AprobacionIdempotenciaTest.java` | 209 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU11AprobacionTest.java` | 175 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU11BandejaTest.java` | 159 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU11CondicionesTest.java` | 104 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU11IdempotenciaTest.java` | 129 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU11MetricasTest.java` | 110 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU11ProveedorSimuladoTest.java` | 250 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU11ProveedorTest.java` | 63 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU11RechazosTest.java` | 273 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU11ServidorRealTest.java` | 163 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU11Test.java` | 284 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU12ConcurrenciaTest.java` | 94 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU12DobleGastoTest.java` | 240 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU12IdempotenciaTest.java` | 152 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU12ProyeccionTest.java` | 143 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU12QrRechazosTest.java` | 162 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU12QrTest.java` | 152 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU12RechazosTest.java` | 197 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU12RescateTest.java` | 221 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU12Test.java` | 244 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU13CuentaPropiaTest.java` | 75 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU13IdempotenciaTest.java` | 129 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU13InversionBaseTest.java` | 78 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU13InversionTest.java` | 261 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU13LiberacionDelMercadoTest.java` | 93 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU13MercadoRechazosTest.java` | 149 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU13MercadoTest.java` | 187 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU13PactoInversionesTest.java` | 254 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU13RechazosTest.java` | 134 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU13Test.java` | 222 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU14CargosTest.java` | 174 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU14RechazosTest.java` | 127 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU14ReversosTest.java` | 154 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU14Test.java` | 237 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU15RechazosTest.java` | 100 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU15Test.java` | 204 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU16CierreConDeudaTest.java` | 145 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU16RechazosTest.java` | 127 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU16Test.java` | 231 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU17RechazosTest.java` | 122 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU17Test.java` | 214 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU24EntregasEventosTest.java` | 75 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU24EventosReinicioTest.java` | 70 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU24EventosTest.java` | 188 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU24MonedaTest.java` | 203 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU24RechazosTest.java` | 226 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU24SaldoDerivadoTest.java` | 73 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU24SemillasTest.java` | 209 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU24Test.java` | 165 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU40Test.java` | 286 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU50ConciliacionesTest.java` | 221 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU50RechazosTest.java` | 101 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU50Test.java` | 201 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU51CierreDesdeContabilidadTest.java` | 226 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU51CierresTest.java` | 85 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU51RechazosTest.java` | 149 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU51Test.java` | 227 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CUAperturaDeBilleterasTest.java` | 276 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CUAperturaYAporteTest.java` | 119 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CUAporteNoAplicadoTest.java` | 160 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CUConsumoDeAportesConcurrenciaTest.java` | 86 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CUConsumoDeAportesRechazosTest.java` | 264 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CUConsumoDeAportesTest.java` | 269 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CUConsumoDeRecargoTest.java` | 187 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CUFlujoRecargaYRetiroTest.java` | 132 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CUIndicadoresFinancierosTest.java` | 162 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CUPactosDelRecargoTest.java` | 98 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/ConfirmacionesDePrueba.java` | 24 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/EscenarioDeCierreDiario.java` | 86 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/EscenarioDeRetiro.java` | 53 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/FixturaDeBilletera.java` | 255 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/FixturaDeCustodia.java` | 121 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/FixturaDeNucleoFinanciero.java` | 69 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/FixturaDeObligacion.java` | 101 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/FixturaDeSistema.java` | 179 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/FlujosDePrueba.java` | 115 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/IdentidadesDelPacto.java` | 126 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/LibroBenchmarkTest.java` | 239 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/LibroConcurrenciaTest.java` | 254 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/LibroInvariantesTest.java` | 108 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/LibroRollbackTest.java` | 97 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/ProveedorDeRecargasFalso.java` | 40 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/ProveedorDeRetirosFalso.java` | 78 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/ProveedorPythonLocal.java` | 190 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/RecargasProveedorLocalTest.java` | 114 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/ReconciliacionDeRetirosTest.java` | 169 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/SegundoFactorStepUpTest.java` | 119 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/TarifasFalso.java` | 64 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/TransaccionalDePrueba.java` | 25 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/aplicacion/CuerpoDeRetencionTest.java` | 53 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/consumo/ConsumidorDeAperturasKafkaTest.java` | 108 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/consumo/ConsumidorDeAportesKafkaTest.java` | 97 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/consumo/ConsumidorDeAsientosKafkaTest.java` | 61 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/dominio/CuadrarPartidasPropiedadTest.java` | 58 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/dominio/CuadrarPartidasTest.java` | 70 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/dominio/EstadoDeRetiroTest.java` | 102 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/dominio/QrDeDominioTest.java` | 83 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/infraestructura/ContratosConOtrosServiciosTest.java` | 174 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/infraestructura/CotizadorPorHttpTest.java` | 197 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/infraestructura/CotizadorResilienciaTest.java` | 153 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/infraestructura/EmisorDeEvidenciaDePrueba.java` | 222 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/infraestructura/EmisorDeEvidenciaDePruebaTest.java` | 116 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/infraestructura/GuardiaDeCuentasLogicasTest.java` | 65 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/infraestructura/HechosPorHttpPorUsuarioTest.java` | 199 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/infraestructura/PactoConEntregasContratoTest.java` | 203 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/infraestructura/ProveedorSimuladoHostilTest.java` | 243 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/infraestructura/RecargasProveedorSimuladoTest.java` | 158 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/infraestructura/RecaudoDelDiaContratoTest.java` | 149 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/BaseDeOperacionesWeb.java` | 107 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/BilleteraAprobacionWebTest.java` | 232 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/BilleteraBandejaWebTest.java` | 229 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/BilleteraControllerWebTest.java` | 271 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/BilleteraFondeoWebTest.java` | 154 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/BilleteraMovimientosWebTest.java` | 157 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/BilleteraSaldoWebTest.java` | 138 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/ContabilidadControllerWebTest.java` | 120 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/CotizacionesWebTest.java` | 197 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/CuentaPropiaWebTest.java` | 159 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/CustodiaControllerWebTest.java` | 259 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/InversionesWebTest.java` | 299 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/QrControllerWebTest.java` | 206 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/QrInteroperabilidadContratoTest.java` | 106 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/RecargasDePruebaControllerWebTest.java` | 72 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/SeguridadWebTest.java` | 94 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
| `servicios/nucleo-financiero/src/test/resources/fixtures/.gitkeep` | 0 | no (inventario por grep: @Disabled, asserts, Testcontainers, temas) | prueba de nucleo-financiero |
