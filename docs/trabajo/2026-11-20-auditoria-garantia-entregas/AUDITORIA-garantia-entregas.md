# AUDITORÍA — módulo garantia-entregas (solo lectura)

## 1. Ficha

| Campo | Valor |
|---|---|
| Módulo | `garantia-entregas` (lote L2): `servicios/garantia`, `servicios/entregas`, `sql/10_tablas/08_garantia_incumplimiento`, pantallas y clientes que los consumen |
| Responsable | Pablo (dueño del producto) |
| Fecha | 2026-11-20 |
| Commit auditado (`origin/test`) | `07fc69719287b0dadd152d0293a054948df69ead` |
| `origin/dev` | `850e0f51192d03094304630a61e44e5bd2a97efe` — `git diff --stat origin/test origin/dev` sobre el módulo, el SQL del módulo y `contratos/pactos`: **sin diferencias** |
| Historial | superficial al clonar; profundizado con `git fetch --deepen=300` → `--is-shallow-repository = false` (1 878 commits en `origin/test`) |
| Huella núcleo | 275 archivos · 34 003 líneas (`servicios/garantia`, `servicios/entregas`, `sql/10_tablas/08_garantia_incumplimiento`) |
| Cobertura núcleo | 184 archivos leídos completos (18 414 líneas) · 8 parciales (1 470) · 83 no leídos con motivo (14 119; pruebas por volumen y lockfiles) — ver anexo §12 |
| Cobertura de conexiones | `sql/20_claves`, `30_indices`, `35_append_only`, `40_reglas`, `50_verificacion`, `60_semillas`, `61_dev`, `62_volumen` (por rangos y búsqueda), `sql/10_tablas/04_entregas_fondo` (CREATE), 10 pactos, ADR-049/051/054, catálogo de flujos, ~30 archivos de `apps/movil`, `apps/backoffice`, `apps/web`, 41 mocks de `packages/simulado`, Postman, `clientes/dart/{garantia,entregas}` |
| Método | 4 subagentes de solo lectura con lotes disjuntos (garantía Java · entregas Java · SQL/pactos/docs · frontends/clientes); el agente principal reabrió cada ubicación S1/S2 y corrió los verificadores |

## 2. Resumen

**Estado del módulo.** El módulo no puede operar con dinero de terceros en su forma actual, y contradice las dos reglas
nuevas en su camino central. (1) **R-NUEVA-2:** el respaldo empresarial está vivo de punta a punta: entregas llama
a garantía en cada fondeo con faltante, garantía aplica la reserva de la empresa y nucleo-financiero asienta
`POZO_GRUPO` / `RESERVA_RESPALDO`; el esquema, las reglas de la base, los contratos, las pruebas, los textos de la web
pública y el tour de la app lo sostienen. (2) **R-NUEVA-1:** la entrega del turno no se mueve por el cobro QR: entregas
no consume ningún evento y avanza solo por cinco actos humanos (fondear, autorizar con cuatro ojos, emitir, ejecutar
y anotar la respuesta del proveedor). (3) Con independencia del flujo nuevo, hay defectos graves de seguridad y dinero:
la acreditación del desembolso la tipea una persona y llega al mayor; el desembolso no comprueba que la cuenta sea del
beneficiario; el cliente puede mandar su propio número cifrado; los endpoints del organizador en garantía no verifican
que el grupo sea suyo (BOLA); varias decisiones de dinero (días de mora, aprobador, «grupo cerrado», importe recuperado)
las afirma el cliente. Lo bien resuelto (outbox, idempotencia del fondeo y de la orden, enfriamiento de cuenta,
`NUMERIC` en todo el esquema, cuadre de deducciones en la base) está en §9. Ningún hallazgo se ejecutó: el peldaño es
`DISCOVERED` (Gradle no resolvió dependencias en la nube, ver §11).

**Hallazgos por clase y severidad** (68 en total):

| Clase | S1 | S2 | S3 | S4 | Total |
|---|---:|---:|---:|---:|---:|
| APROB | 0 | 3 | 1 | 0 | 4 |
| RESPALDO | 3 | 1 | 0 | 0 | 4 |
| FLUJO-QR | 0 | 1 | 0 | 0 | 1 |
| FLUJO-FALTANTE | 0 | 1 | 0 | 0 | 1 |
| COHERENCIA | 0 | 0 | 1 | 0 | 1 |
| DUP-X | 0 | 0 | 1 | 0 | 1 |
| REESCRITURA | 0 | 0 | 1 | 0 | 1 |
| PISADA | 0 | 0 | 0 | 1 | 1 |
| PARCHE | 0 | 0 | 1 | 0 | 1 |
| FRAGIL | 0 | 0 | 1 | 0 | 1 |
| DEFECTO | 0 | 1 | 1 | 0 | 2 |
| HUECO | 0 | 1 | 0 | 0 | 1 |
| RESILIENCIA | 0 | 2 | 0 | 0 | 2 |
| DINERO | 2 | 7 | 4 | 0 | 13 |
| DEBIDO-PROCESO | 1 | 2 | 0 | 0 | 3 |
| CLEAN | 0 | 0 | 0 | 1 | 1 |
| ACOPLE | 0 | 1 | 0 | 0 | 1 |
| CONTRATO | 0 | 1 | 2 | 1 | 4 |
| DATOS | 0 | 1 | 2 | 0 | 3 |
| PRUEBAS | 0 | 0 | 2 | 0 | 2 |
| UX-A11Y | 0 | 0 | 3 | 1 | 4 |
| OBSERV | 0 | 0 | 1 | 0 | 1 |
| DOC | 0 | 0 | 2 | 1 | 3 |
| SEG | 7 | 2 | 0 | 1 | 10 |
| TRAZA | 0 | 1 | 0 | 0 | 1 |
| REG | 0 | 0 | 1 | 0 | 1 |
| **Total** | 13 | 25 | 24 | 6 | 68 |

**Los diez más graves**

1. `garantia-entregas-RESPALDO-001` — cada fondeo con faltante dispara la cobertura con dinero de la empresa y la asienta en el mayor (S1).
2. `garantia-entregas-SEG-005` — la acreditación del desembolso la anota una persona sin firma del proveedor y genera el asiento (S1).
3. `garantia-entregas-SEG-006` — el desembolso no verifica que la cuenta destino sea del beneficiario (S1).
4. `garantia-entregas-SEG-007` — el cliente puede guardar su propio `numeroCifrado`, distinto del número que se muestra (S1).
5. `garantia-entregas-DINERO-001` — `ejecutarEntrega` marca ENTREGADA con monto libre y puede convivir con una orden en vuelo (S1).
6. `garantia-entregas-SEG-001` — `recuperarRespaldo` mueve dinero del pozo a la caja de la empresa con un pago afirmado por el cliente (S1).
7. `garantia-entregas-SEG-002` — BOLA: el organizador opera sobre cualquier grupo en garantía (S1).
8. `garantia-entregas-DINERO-003` — `POST /entregas` legacy vivo, con bruto/recaudado del cliente, clave ignorada y `EFECTIVO_ORGANIZADOR` (S1).
9. `garantia-entregas-SEG-003` — cobertura del fondo con días de mora y aprobador del cuerpo, sin expediente firme (S1).
10. `garantia-entregas-DEBIDO-PROCESO-001` — aval ejecutado y subrogado sin firmeza y antes del pago (S1).

**Peldaño de evidencia por área (regla 30):** servicio garantía `DISCOVERED` · servicio entregas `DISCOVERED` ·
esquema `sql/` `DISCOVERED` · frontends `DISCOVERED` · verificadores Python del repo `RUNS` (salida en
`evidencia/05-verificadores.txt`) · suites Gradle `BLOCKED` (entorno, `evidencia/05c-gradle-garantia-test.txt`).

## 3. Huella y mapa de conexiones

**Huella.**
- `servicios/garantia` — 20 operaciones (`garantia.yaml`), casos de uso CU-23 (respaldo y cobertura mutual), CU-25, CU-26, CU-27, CU-29, CU-66, CU-67; consumidores Kafka de `aporte_aplicado` y `transaccion_reversada`; sin trabajos programados.
- `servicios/entregas` — 21 operaciones (`entregas.yaml`), CU-18 (cuenta destino), CU-22 (liquidación legacy y fondeo «pozo completo»), CU-28 (desembolso), CU-130 (mercado de ofertas), CU-136/64 (subasta); un `@Scheduled` (`CierreDeSubastas`); no consume eventos.
- Esquema: 39 tablas en `sql/10_tablas/08_garantia_incumplimiento` (dueño garantía) y 12 en `sql/10_tablas/04_entregas_fondo` (dueño entregas). Fuente de verdad: `docs/entidades/*.puml` + `docs/Restricciones.md` (`sql/README.md:3-6`); el SQL es generado.
- Pantallas: app móvil (`pasanaku/pantalla_mi_estado.dart`, subasta, traspaso, tour), backoffice (bandeja de desembolsos CU-28, explicaciones, tableros), web pública (secciones de inicio y JSON-LD).
- Catálogos: `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml`, `entrega-del-turno.yaml`; CU-22..29 en `docs/CasosDeUso`.

**Conexiones.**

| Desde → hacia | Mecanismo | Punto exacto | Timeout / breaker / idempotencia / token |
|---|---|---|---|
| entregas → aportes | HTTP GET `/aportes/periodos/{id}/recaudo` | `entregas/.../RecaudoPorHttp.java` | timeout global de plataforma, breaker sí, GET · token del usuario |
| entregas → garantía | HTTP POST `/garantia/respaldo/coberturas` | `entregas/.../RespaldoPorHttp.java:56` | breaker sí, `Idempotency-Key` por turno · token del usuario (SEG-009) · **sin pacto** |
| entregas → identidad | HTTP POST `/usuarios/{id}/titularidad/verificacion` | `TitularPorHttp.java` | token de servicio |
| entregas → nucleo-financiero | HTTP retenciones, pago a tercero, cierre, liberación | `FondosPorHttp.java` | clave sí en mutaciones; mezcla token usuario/servicio |
| entregas → grupos | HTTP subastabilidad, elegibilidad, traspaso, participaciones | `GruposParaSubastaPorHttp.java`, `HechosDeGruposPorHttp.java` (`turno()` vacío) | dos breakers separados |
| garantía → aportes | HTTP GET recaudo | `garantia/.../RecaudoPorHttp.java` | breaker sí · **sin pacto** |
| garantía → cumplimiento | HTTP GET `/reclamos/usuarios/{id}/efecto` | `EfectoDeReclamosPorHttp.java` | falla cerrado · token del usuario · sin pacto |
| nucleo-financiero → garantía | Kafka `aporte_aplicado`, `transaccion_reversada` | `garantia/consumo/*` | pactos con verificación de productor; consumidor idempotente (`Consumidos`); sin DLT |
| garantía → nucleo-financiero | Kafka `respaldo_reservado/ampliado/liberado/recuperado`, `cobertura_empresarial_aplicada/reversada` | outbox → `ConsumidorDeAsientosKafka` | outbox sí; sin pacto |
| entregas → nucleo-financiero | Kafka `desembolso_acreditado` (con asiento) | outbox → `ConsumidorDeAsientosKafka` | outbox sí |
| garantía → (nadie) | Kafka `incumplimiento_cubierto`, `aval_ejecutado`, `fondo_devuelto`, `cobertura_requiere_aprobacion`, y demás eventos de expediente | outbox | **sin consumidor** (HUECO-001) |
| entregas → (nadie verificado) | Kafka `pozo_fondeado`, `pozo_con_faltante`, `entrega_*`, `subasta_*`, `oferta_*` | outbox | consumidores no relevados (fuera de alcance) |
| tablas | FK entre esquemas | `sql/20_claves/08_garantia_incumplimiento.sql` (76), `04_entregas_fondo.sql` (25) | ACOPLE-001 |
| front | app móvil → `consultarRestriccion`, subasta; backoffice → `listarDesembolsos` | ver §6 | — |

**Saltos de la operación de dinero «entrega del turno» (98.8.1):** (1) operador → `POST /entregas/pozo-completo`;
(2) entregas → aportes (recaudo afirmado); (3) entregas → garantía (cobertura de la empresa) → outbox → nucleo-financiero
(asiento de cobertura); (4) escritura local del fondeo + `pozo_fondeado`; (5) humano autoriza; (6) humano emite la orden;
(7) fuera de código, alguien transfiere; (8) humano anota la respuesta; (9) `desembolso_acreditado` → nucleo-financiero
asienta. No hay saga persistida que ate 5-9. **Si falla a mitad:** tras (3) y antes de (4) la cobertura queda aplicada en
garantía y asentada sin fondeo registrado (prueba `caidaTrasCubrir` lo contempla con reintento idempotente por turno);
entre (6) y (8) la orden queda ENVIADA_A_PROVEEDOR sin conciliación automática; tras un `ejecutar` manual la acreditación
posterior asienta igual (DINERO-001).

## 4. Distancia al flujo nuevo

### 4.a Inventario de aprobaciones

Búsqueda: `aprobar|aprobaci|APROBAD|PENDIENTE_|comprobante|habilit|confirmar|firme|descargo|autoriz` sobre la huella
y conexiones directas. `comprobante`: sin resultados en `servicios/garantia/src/main` ni `servicios/entregas/src/main`.

| ID | Dónde | Qué aprueba y quién | Estado · endpoint · permiso · pantalla · evento | Clasificación | Qué se rompe aguas abajo si se quita |
|---|---|---|---|---|---|
| AP-01 | `entregas/.../CU22LiquidarEntrega.java:119-149`; `EntregasController.java:99-100` | Autorizar la entrega — RESPONSABLE_RIESGOS | AUTORIZADA · `POST /entregas/{id}/autorizacion` · ENTREGA_AUTORIZAR · sin pantalla · `entrega_autorizada` | DECISIÓN REQUERIDA (A-1: R-SEG-04 puede ser control exigido) | `CU28EmitirDesembolso.java:86` exige AUTORIZADA; `ck_entrega_segregacion`; pruebas CU22/CU28 |
| AP-02 | `CU22LiquidarEntrega.java:161-190`; `EntregasController.java:113-114` | Ejecutar la entrega a mano — TESORERIA (≠ autorizador) | ENTREGADA · `POST /entregas/{id}/ejecucion` · ENTREGA_EJECUTAR · sin pantalla · `entrega_ejecutada` | ELIMINABLE (además es defecto: DINERO-001) | ninguno productivo; pruebas CU22 |
| AP-03 | `EntregasController.java:128-129` | Fondear el pozo — TESORERIA | FONDEADO/CON_PENDIENTE · `POST /entregas/pozo-completo` · ENTREGA_EJECUTAR · sin pantalla · `pozo_fondeado` | ELIMINABLE (debe dispararlo el evento de cobro, FLUJO-QR-001) | pruebas CU133; catálogo `entrega-del-turno.yaml` |
| AP-04 | `DesembolsosController.java:78-79`; `CU28EmitirDesembolso.java:84-123` | Emitir la orden de desembolso — TESORERIA | ENVIADA_A_PROVEEDOR · `POST /desembolsos` · ENTREGA_EJECUTAR · bandeja backoffice (solo lectura) · `desembolso_ordenado` | ELIMINABLE como paso humano (A-1 si Cumplimiento exige control) | integración con proveedor inexistente |
| AP-05 | `DesembolsosController.java:105-106`; `CU28EmitirDesembolso.java:150-196` | Confirmar la acreditación — TESORERIA | ACREDITADA · `POST /desembolsos/{id}/respuesta` · ENTREGA_EJECUTAR · sin pantalla · `desembolso_acreditado` (asiento) | ELIMINABLE (reemplazar por webhook firmado, SEG-005) | consumidor de asientos en nucleo-financiero |
| AP-06 | `CU18RegistrarCuentaDestino.java:147-176` | «Verificar» cuenta destino — el propio dueño | VERIFICADA · `POST /cuentas-bancarias/{id}/verificacion` · BILLETERA_OPERAR · sin pantalla | DECISIÓN REQUERIDA (A-1: verificación de cuenta puede ser obligación) | `tg_orden_desembolso_cuenta_verificada` |
| AP-07 | `garantia/.../CU23CubrirIncumplimiento.java:126-140`; `politica_cobertura.sql:15` | Aprobación manual de cobertura mutual sobre umbral — «aprobadaPor» del cuerpo | SOLICITADA · `POST /incumplimientos/{id}/cobertura` · GRUPO_ADMINISTRAR · sin pantalla · `cobertura_requiere_aprobacion` (sin consumidor) | ELIMINABLE (y A-3 sobre si la cobertura mutual existe) | ninguno (estado muerto) |
| AP-08 | `CU25DeclararIncumplimiento.java` (resolver descargo) | Resolver descargo — organizador | `POST /incumplimientos/{id}/descargo/resolucion` · GRUPO_ADMINISTRAR | CONSERVAR POR NORMA de la casa (regla 91.4: debido proceso); quién resuelve con R-NUEVA-1: DECISIÓN REQUERIDA | expediente FIRME, sanciones |
| AP-09 | `CU25DeclararIncumplimiento.java` (declarar) | Declarar incumplimiento a mano — organizador | `POST /incumplimientos` · GRUPO_ADMINISTRAR · `incumplimiento_declarado` | DECISIÓN REQUERIDA (podría derivarse del vencimiento sin pago) | toda la cadena de mora |
| AP-10 | `CU26EjecutarAval.java` | Ejecutar aval — organizador | `POST /incumplimientos/{id}/aval/ejecucion` · GRUPO_ADMINISTRAR · `aval_ejecutado` (sin consumidor) | CONSERVAR POR NORMA 91.4 con precondición de firmeza; disparo manual DECISIÓN REQUERIDA | subrogación |
| AP-11 | `CU27RestringirDeudor.java`; `CobranzaController.java` | Restringir / levantar restricción — organizador | `POST /incumplimientos/{id}/restriccion`, `POST /cobranza/restricciones/{id}/levantamiento` · GRUPO_ADMINISTRAR · pantalla «Se levanta pagando» | levantar: ELIMINABLE (debe levantarse por pago QR); restringir: CONSERVAR POR NORMA 91.4 | `consultarRestriccion`, app móvil |
| AP-12 | `CU66ReemplazarParticipante.java` | Aprobar y ejecutar reemplazo — organizador | `POST /garantia/reemplazos/{id}/aprobacion` y `/ejecucion` · GRUPO_ADMINISTRAR · `reemplazo_*` | DECISIÓN REQUERIDA | grupos (cupos) |
| AP-13 | `CU67DisolverGrupo.java`; `CU29DevolverFondo.java` | Disolver / cerrar disolución / devolver fondo — organizador | `/garantia/grupos/{id}/disolucion`, `/garantia/disoluciones/{id}/cierre`, `/garantia/fondos/{id}/devolucion` · GRUPO_ADMINISTRAR | DECISIÓN REQUERIDA (cierre del grupo sin organizador, ADR-054) | grupos, nucleo-financiero |
| AP-14 | `GarantiaController.java:153-188` | Reservar/ampliar/liberar/reversar/recuperar respaldo — ADMIN_PLATAFORMA | `/garantia/respaldo/*` | ELIMINABLE (es respaldo, R-NUEVA-2) | RESPALDO-001/002 |
| AP-15 | `sql/40_reglas/restricciones.sql:2771-2778` (R-ORG-06) | Confirmación humana obligatoria para PROPONER_ENTREGA/COBRO/COBERTURA/SANCION | CHECK en `regla_automatizacion` | entrega/cobro/cobertura: ELIMINABLE · sanción: CONSERVAR POR NORMA 91.4 — ADR-054 «Pendiente de ratificación de Cumplimiento» | organizador (L3) |
| AP-16 | `apps/movil/.../rutas_operacion.dart:14-22`; `textos_del_traspaso.dart:66-77` | Votación del grupo para traspasar el cupo | ruta `pasanaku.traspaso` | ELIMINABLE (la subasta lo reemplaza «sin votación») — dueño L3 | grupos (acuerdos) |
| AP-17 | `apps/backoffice/.../explicaciones-operacion.ts:36-38` (texto); ADR-049 | Doble aprobación de retiros de billetera | fuera de este módulo (L1/L4) | DECISIÓN REQUERIDA (A-1) | — |
| AP-18 | `garantia/.../PlanDeContingenciaRepositorio.java` | Plan de contingencia escrito con estado `APROBADO` por código | sin humano | ELIMINABLE junto con el respaldo | — |

Total: **18** puntos. **8** con DECISIÓN REQUERIDA total o parcial (AP-01, AP-06, AP-08, AP-09, AP-10, AP-12, AP-13, AP-17); **4** con una parte CONSERVAR POR NORMA (AP-08, AP-10, AP-11, AP-15 — norma de la casa 91.4; la norma externa queda a identificar por Cumplimiento); ELIMINABLE: AP-02, AP-03, AP-04, AP-05, AP-07, AP-14, AP-16, AP-18 y la parte «levantar» de AP-11 y «entrega/cobro/cobertura» de AP-15.

### 4.b Inventario de respaldo y cobertura

| ID | Dónde | Qué pone la empresa y cuándo | Asiento que deja | Clasificación | Qué depende |
|---|---|---|---|---|---|
| RS-01 | `entregas/.../CU22EntregarPozoCompleto.java:67-85` → `RespaldoPorHttp.java:56` → `garantia/.../CU23CubrirConRespaldo.java:215` | Cubre el faltante del pozo con su reserva, automático dentro del fondeo | DEBE `POZO_GRUPO` / HABER `RESERVA_RESPALDO` (`AsientoPropuesto.java:47-48`) vía `cobertura_empresarial_aplicada` | CONTRADICE R-NUEVA-2 | pruebas CU131, CU133 (4 archivos), `FondeoDelPozoWebTest`, `RespaldoCaidaTest`; mock `fondearPozo.json`; Postman |
| RS-02 | `CU23ReservarRespaldo.java:143,217,268` | Aparta, amplía y libera caja propia por grupo/ciclo (manual, ADMIN) | DEBE `RESERVA_RESPALDO` / HABER `CAJA_EMPRESA` y su inverso | CONTRADICE R-NUEVA-2 | `CU131RespaldoTest`, `RespaldoControllerWebTest` |
| RS-03 | `CU23RecuperarRespaldo.java:142` | Imputa un pago tardío a lo que adelantó la empresa | DEBE `CAJA_EMPRESA` / HABER `POZO_GRUPO` | CONTRADICE R-NUEVA-2 (y SEG-001) | `CU131RecuperacionTest` |
| RS-04 | `CU23ReversarCobertura.java:102` | Contra-asiento de cobertura | DEBE `RESERVA_RESPALDO` / HABER `POZO_GRUPO` | CONTRADICE R-NUEVA-2 (útil solo para desmontar) | — |
| RS-05 | `ContingenciaDeCaja.java`, `PlanDeContingenciaRepositorio.java` | Escala a Tesorería para «reponer el respaldo» | solo eventos | CONTRADICE R-NUEVA-2 | `CU133ContingenciaTest` |
| RS-06 | `sql/10_tablas/08_garantia_incumplimiento/{capacidad,reserva,movimiento,cobertura,cobertura_linea,recuperacion}_respaldo*.sql`; `fondeo_entrega.monto_cubierto_empresa`; R-GAR-08..13; R-DES-03 | Modelo de datos del respaldo | — | CONTRADICE R-NUEVA-2 / qué hacer con el histórico: DECISIÓN REQUERIDA (A-3) | sin semillas de capacidad/reserva |
| RS-07 | `sql/60_semillas/22-cuentas-de-sistema.sql:16-20` | Cuentas `9.1.01 Caja de la empresa`, `9.1.02 Reserva de respaldo` (sintéticas) | — | DECISIÓN REQUERIDA (A-3) | nucleo-financiero |
| RS-08 | `deuda_participante.sql:25` (`acreedor PLATAFORMA`) | La empresa como acreedora del deudor | — | DECISIÓN REQUERIDA (A-3/A-4) | — |
| RS-09 | `garantia/.../CU23CubrirIncumplimiento.java`, `CoberturaAplicable.java`, `politica_cobertura` | **Fondo mutual** (dinero de participantes) cubre al que no pagó | ningún asiento (HUECO-001) | DECISIÓN REQUERIDA (A-3: ¿R-NUEVA-2 alcanza al fondo comunitario?) | `CU23Test`, `CoberturaWebTest`, semillas `02-politicas.sql` |
| RS-10 | `garantia/.../CU26EjecutarAval.java` | Aval entre participantes | ningún asiento | NO APLICA (aval entre participantes, A-3) — con defectos de debido proceso | — |
| RS-11 | Textos: `apps/web/.../cierre.ts:71`, `producto.ts:75`, `transparencia.ts:65`, `app.routes.ts:117`, `apps/movil/.../identidad/textos.dart:42-46`, backoffice `catalogo-de-tableros.ts:31-33`, `explicaciones-tablero.ts:25`, `packages/ui/.../muestras.ts:30`, `packages/diseno_flutter/.../muestras.dart:606` | Prometen que «el fondo cubre» | — | CONTRADICE R-NUEVA-2 (si se refiere al respaldo) / DECISIÓN REQUERIDA (A-3, si es el fondo mutual) | indicadores `IMPAGOS_CUBIERTOS`, `COBERTURA_FONDO`, `RECUPERACION_CUBIERTO` (`sql/60_semillas/23-indicadores-tableros.sql:16-19`) |
| RS-12 | `docs/Arquitectura/ADR-051…:34`, catálogo `aporte-mora-cobertura.yaml` (`cubrir-faltante`, `recuperar-respaldo`), `entrega-del-turno.yaml`, CU-22/23, contratos `garantia.yaml:431-560`, `entregas.yaml:324-353` | Documentación y contratos del respaldo | — | CONTRADICE R-NUEVA-2 | `scripts/validar_flujos.py` lo valida como OK |

Total: **12** puntos. **8** CONTRADICE R-NUEVA-2 (RS-01..06, RS-11, RS-12; RS-06 y RS-11 con una parte DECISIÓN REQUERIDA), **3** DECISIÓN REQUERIDA (RS-07, RS-08, RS-09), **1** NO APLICA (RS-10).

### 4.c Ambigüedades A-1 a A-4

| ID | Pregunta | Lo que el código muestra hoy | Supuesto de trabajo | Confirmar con |
|---|---|---|---|---|
| A-1 | ¿«Nada de aprobación» incluye KYC, UIF/ASFI y doble aprobación de retiros? | En este módulo: cuatro ojos en la entrega (R-SEG-04, AP-01), verificación de cuenta (AP-06), R-ORG-06 (AP-15). Sin norma citada en el repo para R-SEG-04 (REG-001). | No se asume que se eliminan; se listan como DECISIÓN REQUERIDA | Pablo + Cumplimiento |
| A-2 | ¿Qué pasa con la entrega cuando falta plata? | Ramas en §4.d: hoy, sin respaldo, la entrega queda **BLOQUEADA** con el pozo completo como principal y el pendiente conservado; no existe entrega parcial (ADR-051:113 lo prohíbe). | No se inventa la regla | Pablo |
| A-3 | ¿Qué pasa con lo construido sobre respaldo y con el aval? ¿El fondo mutual cuenta como «el sistema paga»? | Respaldo vivo (RS-01..07); fondo mutual vivo pero sin asiento (RS-09); aval vivo con defectos (RS-10). | Aval y fondo mutual no son «la empresa» salvo que Pablo diga otra cosa | Pablo |
| A-4 | ¿Mora y recargo cuando nadie cubre? | El recargo por mora se anota en el fondo de garantía (`IngresoAlFondo`); el catálogo dice que se asienta contra la «reserva de respaldo» (`aporte-mora-cobertura.yaml:87`) — mezcla fondo mutual y reserva de la empresa (no verificado en nucleo-financiero, lote L1). `matriz_sancion` afecta reputación automáticamente (DEBIDO-PROCESO-003). | No se decide | Pablo; Cumplimiento para sanciones |

### 4.d Ramas del faltante al corte (FLUJO-FALTANTE)

| Rama | Dónde | Qué hace | ¿Llama a garantía? | Clasificación |
|---|---|---|---|---|
| A1 faltante 0 | `CU22EntregarPozoCompleto` → `RegistroDelFondeo` | FONDEADO, entrega PROGRAMADA, `pozo_fondeado` | No | VIGENTE (luego exige aprobación humana, APROB-001) |
| A2 faltante > 0, respaldo aplicado | `CU22EntregarPozoCompleto.java:67-85` | `cubiertoEmpresa = faltante`; pozo completo | Sí | CONTRADICE R-NUEVA-2 |
| A3 respaldo insuficiente / sin reserva | `RegistroDelFondeo` | Entrega BLOQUEADA_POR_FONDO_INCOMPLETO con bruto = pozo completo; fondeo CON_PENDIENTE; incidencia CRITICA con SLA sintético 24 h | Sí (consultó) | DECISIÓN REQUERIDA (A-2) |
| A4a reintento con faltante ya pagado | `RegistroDelFondeo` | Recalcula contra aportes; si completo, destraba | No | VIGENTE en mecánica; política A-2 |
| A4b reintento tras ampliar reserva | ídem; prueba `contingenciaYReintento` | Garantía cubre y destraba | Sí | CONTRADICE R-NUEVA-2 |
| A5 garantía no responde | `CU22EntregarPozoCompleto.java` | AP-CU22-08, no escribe | Sí (falla) | Desaparece con el respaldo |
| A6 fondo mutual como caja | `RecaudoDelPozo` (`pozo − confirmado − cubiertoMutual`) | `cubiertoMutual` lo afirma aportes | No | DECISIÓN REQUERIDA (A-3) |
| B legacy `bruto > recaudado` | `CU22LiquidarEntrega` | Rechaza, pero `recaudado` lo manda el cliente | No | intención VIGENTE, control roto (DINERO-003) |
| C legacy con deducciones | `LiquidacionDeEntrega` | Neto con líneas; nunca negativo (`ck_entrega_neto_no_negativo`) | No | DECISIÓN REQUERIDA |
| D derecho cedido con pozo incompleto | `RegistroDelFondeo` | Bloquea a nombre del comprador | — | DECISIÓN REQUERIDA |

Las ramas A3, A4b y D vienen de la lectura del subagente de entregas; el agente principal reabrió A2 y el SLA de A3.

## 5. Hallazgos

Convención: `garantia-entregas-<CLASE>-<NNN>`. Severidad según §10 del metaprompt. Orden: por clase y, dentro de cada
clase, de mayor a menor severidad. Peldaño de todos: `DISCOVERED` (lectura); ninguno se ejecutó. Las fichas compactas
(«Impacto/Solución») agrupan campos en hallazgos S3/S4 cuyo contenido es breve; todas tienen ubicación y evidencia.

### garantia-entregas-APROB-001 — La entrega del turno avanza por cinco actos humanos: fondear, autorizar (cuatro ojos), emitir, ejecutar y anotar respuesta
- Clase / Severidad / Confianza: APROB / S2 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/web/EntregasController.java:99-100,113-114,128-129`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22LiquidarEntrega.java:119-149,169`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java:84-89`; `servicios/entregas/src/main/java/bo/aportaya/entregas/web/DesembolsosController.java:78-79,105-106`; `sql/40_reglas/restricciones.sql` (`ck_entrega_segregacion`, R-SEG-04); `docs/catalogo-de-flujos/entrega-del-turno.yaml:41-55`
- Evidencia: `@Permiso("ENTREGA_AUTORIZAR") autorizarEntrega`; `if (!"AUTORIZADA".equals(entrega.estado())) throw … "todavia no se desembolsa."`; comentario «La autorizacion es la firma de que alguien reviso que corresponde pagarle a esa persona».
- Cómo se comprobó: lectura.
- Impacto: aunque todos paguen por QR, la entrega no avanza sin RESPONSABLE_RIESGOS y TESORERIA. Bloquea R-NUEVA-1.
- Causa de fondo: segregación de funciones R-SEG-04 aplicada al pago del pozo.
- Solución de fondo: transición automática fondeado → autorizado → orden emitida por eventos; decidir con Cumplimiento si R-SEG-04 es obligación legal (A-1) antes de quitarla.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: aportes (evento de cobro), nucleo-financiero.
- Estándares y normas: ISO/IEC 25010 adecuación funcional; R-NUEVA-1; norma a identificar por Cumplimiento para R-SEG-04.
- Esfuerzo estimado: grande

### garantia-entregas-APROB-002 — Aprobación manual por umbral en la cobertura del fondo mutual, con estado SOLICITADA sin salida
- Clase / Severidad / Confianza: APROB (y HUECO) / S2 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirIncumplimiento.java:126-140`; `sql/10_tablas/08_garantia_incumplimiento/politica_cobertura.sql:15`; `sql/60_semillas/02-politicas.sql:27-28`
- Evidencia: `String estado = quedaPendiente ? "SOLICITADA" : "APLICADA";` y evento `garantia.cobertura_requiere_aprobacion`; `grep` del tópico fuera de garantía: sin consumidor; ningún endpoint aplica una cobertura SOLICITADA.
- Impacto: un estado muerto y un paso humano (contra R-NUEVA-1). Combinado con SEG-003 el umbral es eludible.
- Solución de fondo: decidir A-3 (¿existe la cobertura del fondo mutual?) y retirar el paso manual.
- Relación con el flujo nuevo: APROB · Conexiones: ninguna · Estándares y normas: ISO/IEC 25010 completitud; R-NUEVA-1 · Esfuerzo: chico
- Causa de fondo: Política de cobertura con umbral de aprobación pensada para un fondo administrado por personas (ADR-051, catálogo de mora).
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.

### garantia-entregas-APROB-003 — R-ORG-06 obliga en la base a confirmación humana para PROPONER_ENTREGA / PROPONER_COBERTURA / PROPONER_COBRO / PROPONER_SANCION
- Clase / Severidad / Confianza: APROB (y COHERENCIA) / S2 / confirmado
- Ubicaciones: `sql/40_reglas/restricciones.sql:2771-2778`; `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md:25-26,92-97`
- Evidencia: `CHECK (accion NOT IN ('PROPONER_COBRO','PROPONER_ENTREGA','PROPONER_SANCION','PROPONER_COBERTURA') OR requiere_confirmacion_humana)`; ADR-054: «R-ORG-06 … **Pendiente de ratificación de Cumplimiento.**»
- Impacto: ninguna regla de automatización puede proponer una entrega sin humano; contradice R-NUEVA-1. Para sanciones, la confirmación humana puede ser exigencia del debido proceso (91.4).
- Solución de fondo: decisión de Pablo + Cumplimiento por acción (A-1): entrega/cobro sin humano; sanción con expediente. Luego cambio en `docs/Restricciones.md`.
- Relación con el flujo nuevo: APROB · Conexiones: organizador (L3) · Estándares y normas: regla 91.4, R-NUEVA-1 · Esfuerzo: chico (tras decisión)
- Causa de fondo: R-ORG-06 se escribió como restricción general de automatización antes de R-NUEVA-1; ADR-054 intentó acotarla y quedó sin ratificar.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.

### garantia-entregas-APROB-010 — Traspaso del cupo por votación del grupo sigue con ruta viva en la app
- Clase / Severidad / Confianza: APROB (y REESCRITURA con la subasta) / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart:14-22`, `apps/movil/lib/pantallas/pasanaku/textos_del_traspaso.dart:66-77`, `apps/movil/lib/pantallas/pasanaku/dominio/subasta_del_cupo.dart:9-10`
- Evidencia: ruta `'/pasanaku/grupos/:grupoId/cupos/:cupoId/traspaso'` → `PantallaTraspaso`; textos «El grupo aprobó el traspaso en una votación», «Esperando la votación», «Cuando se apruebe, volvés acá a traspasar». La subasta (entregas, CU-136) se declara su reemplazo «sin votación».
- Cómo se comprobó: lectura de las líneas citadas.
- Impacto: dos caminos para ceder el turno; uno exige aprobación del grupo (contra R-NUEVA-1). La persona puede quedar esperando una votación que el flujo nuevo no prevé.
- Causa de fondo: la subasta se agregó sin dar de baja la pantalla vieja.
- Solución de fondo: decidir cuál camino queda (Pablo) y retirar el otro con su ruta, textos y endpoint.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: grupos (votación/acuerdo, lote L3), entregas (subasta).
- Estándares y normas: ISO/IEC 25010 mantenibilidad; regla 00 §1.1, 60 (reescritura).
- Esfuerzo estimado: medio

### garantia-entregas-RESPALDO-001 — Cada fondeo con faltante dispara automáticamente la cobertura con dinero de la empresa
- Clase / Severidad / Confianza: RESPALDO (y DINERO) / S1 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22EntregarPozoCompleto.java:67-85,100`; `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/RespaldoPorHttp.java:56`; `servicios/entregas/src/main/java/bo/aportaya/entregas/web/EntregasController.java:128-129`; `servicios/garantia/src/main/java/bo/aportaya/garantia/web/GarantiaController.java:172-175`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirConRespaldo.java:215`; `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/AsientoPropuesto.java:47-48`; consumidor `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/consumo/ConsumidorDeAsientosKafka.java` (tópico `aportaya.garantia.cobertura_empresarial_aplicada`)
- Evidencia (refutación hecha: no hay bandera que lo desactive, `servicios/entregas/src/main/resources/application.yml:46` «cubre el faltante con la reserva empresarial»; ninguna pantalla llama `fondearPozo`, lo invoca un operador con `ENTREGA_EJECUTAR` por API): `var cobertura = respaldo.cubrir(e.grupoId(), e.periodoId(), e.turnoId(), faltante)` … `cubiertoEmpresa = cobertura.cubierto();` → `RespaldoPorHttp` hace POST a `"/garantia/respaldo/coberturas"` → `AsientoPropuesto.aplicacion(...)` = `par("COBERTURA", coberturaId, Cuenta.POZO_GRUPO, Cuenta.RESERVA_RESPALDO, monto)`; nucleo-financiero escucha ese tópico y registra el asiento.
- Cómo se comprobó: lectura de los cinco archivos; `grep -rhoE 'aportaya\.garantia\.[a-z_]+'` muestra que el tópico tiene consumidor en nucleo-financiero.
- Impacto: la empresa paga al beneficiario lo que no pagaron otros participantes. Es exactamente lo que R-NUEVA-2 prohíbe, en el camino vivo `POST /entregas/pozo-completo`.
- Causa de fondo: ADR-051 (D-02/D-03/D-15, «pozo completo con respaldo empresarial») implementado en entregas, garantía y nucleo-financiero; nadie lo retiró al cambiar la regla.
- Solución de fondo: ADR nueva que deje sin efecto las decisiones de respaldo de ADR-051; retirar el puerto `RespaldoEmpresarial` y `RespaldoPorHttp`, deprecar `/garantia/respaldo/coberturas` con versión y fecha (98.1.3) y conservar el histórico append-only. Qué se hace con el faltante depende de A-2 (no se decide acá).
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: entregas → garantía (HTTP) → nucleo-financiero (Kafka, mayor); aportes (recaudo); lote L1 por el consumidor del asiento.
- Estándares y normas: ISO/IEC 25010 adecuación funcional (corrección); reglas 91, 98.1.3; R-NUEVA-2.
- Esfuerzo estimado: grande

### garantia-entregas-RESPALDO-002 — Reserva, ampliación, liberación y reversa de respaldo empresarial siguen expuestas y mueven caja de la empresa
- Clase / Severidad / Confianza: RESPALDO / S1 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/web/GarantiaController.java:153-180`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23ReservarRespaldo.java:143,217,268`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23ReversarCobertura.java:102`; `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/AsientoPropuesto.java:42-63`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/ContingenciaDeCaja.java`
- Evidencia: `@Permiso("ADMIN_PLATAFORMA") … reservarRespaldo`, `ampliarRespaldo`, `liberarRespaldo`, `reversarCoberturaRespaldo`; `reserva(...) = par("AJUSTE", reservaId, Cuenta.RESERVA_RESPALDO, Cuenta.CAJA_EMPRESA, monto)`. Los cuatro tópicos `respaldo_*` los consume nucleo-financiero.
- Cómo se comprobó: lectura del controlador y de `AsientoPropuesto`; búsqueda de tópicos.
- Impacto: el mecanismo que alimenta RESPALDO-001 sigue operable a mano y escribe en el mayor. Además la contingencia escala a Tesorería para «reponer el respaldo».
- Causa de fondo: la misma que RESPALDO-001.
- Solución de fondo: retirar junto con RESPALDO-001; contra-asiento de liberación para reservas vigentes; tablas `*_respaldo` a solo lectura histórica (A-3).
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: garantía → nucleo-financiero (6 tópicos).
- Estándares y normas: ISO/IEC 25010 corrección; regla 91; R-NUEVA-2.
- Esfuerzo estimado: grande

### garantia-entregas-RESPALDO-003 — El esquema modela el respaldo empresarial como fuente del pozo (seis tablas, reglas R-GAR-08..13 y `fondeo_entrega.monto_cubierto_empresa`)
- Clase / Severidad / Confianza: RESPALDO (y DATOS) / S1 / confirmado
- Ubicaciones: `sql/10_tablas/08_garantia_incumplimiento/{capacidad_respaldo,reserva_respaldo,movimiento_reserva,cobertura_respaldo,cobertura_respaldo_linea,recuperacion_respaldo}.sql`; `sql/10_tablas/04_entregas_fondo/fondeo_entrega.sql:13,15,26`; `sql/40_reglas/restricciones.sql:2585-2589` (R-DES-03) y `:2410-2530` (R-GAR-08..13); `sql/60_semillas/22-cuentas-de-sistema.sql:16-20` (cuentas «Caja de la empresa» y «Reserva de respaldo», declaradas sintéticas)
- Evidencia: `monto_cubierto_empresa NUMERIC(14,2) DEFAULT 0 NOT NULL`; `ck_fondeo_entrega_pendiente CHECK (monto_cubierto_empresa <= monto_faltante AND monto_pendiente = monto_faltante - monto_cubierto_empresa)`; `cobertura_respaldo_id UUID` sin FK.
- Cómo se comprobó: lectura de los DDL y de la regla.
- Impacto: la base exige que la ecuación del pozo tenga un término «empresa»; mientras exista, el código seguirá teniendo dónde poner el dinero de la empresa.
- Causa de fondo: ADR-051. La fuente de verdad del esquema es `docs/entidades/*.puml` + `docs/Restricciones.md` (ver `sql/README.md:3-6`): el SQL es generado.
- Solución de fondo: cambio en el modelo y en `docs/Restricciones.md`, expandir/contraer (97.3.5); conservar el histórico append-only (A-3). Ninguna semilla siembra `capacidad_respaldo`/`reserva_respaldo`: retirarlo no exige limpiar datos sembrados.
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: entregas, garantía, nucleo-financiero (cuentas 9.1.01/9.1.02).
- Estándares y normas: ISO/IEC 25010 corrección; reglas 97.1, 91; R-NUEVA-2.
- Esfuerzo estimado: grande

### garantia-entregas-RESPALDO-010 — La web pública y el tour de la app prometen que el fondo cubre al que no paga
- Clase / Severidad / Confianza: RESPALDO (y DOC) / S2 / confirmado
- Ubicaciones: `apps/web/src/app/paginas/inicio/secciones/cierre.ts:71`, `apps/web/src/app/paginas/inicio/secciones/producto.ts:75`, `apps/web/src/app/paginas/inicio/secciones/transparencia.ts:65`, `apps/web/src/app/app.routes.ts:117` (JSON-LD de preguntas frecuentes, indexable), `apps/web/src/app/paginas/inicio/secciones/hero.ts:31`, `apps/movil/lib/pantallas/identidad/textos.dart:42-46`, `apps/movil/lib/pantallas/identidad/laminas_del_tour.dart:45-46`
- Evidencia: `cierre.ts:71` «…el fondo de garantía cubre la entrega para que quien tenía el turno cobre igual…»; `producto.ts:75` «Si alguien no aporta, el fondo cubre la entrega de quien tenía el turno»; `app.routes.ts:117` «Un fondo de garantía cubre esa falta para que el resto del grupo no se vea afectado»; `textos.dart:45` «Hay un fondo de garantía que cubre la cuota para que el turno se entregue igual».
- Cómo se comprobó: `grep -n 'cubre\|cubri'` sobre los archivos citados; líneas reabiertas.
- Impacto: promesa comercial pública de pozo completo que R-NUEVA-2 prohíbe; riesgo de reclamo del consumidor cuando el turno se entregue incompleto. La de `app.routes.ts` además queda indexada por buscadores.
- Causa de fondo: los textos de marketing se escribieron contra ADR-051 (regla 7, «la empresa aporta el faltante») y no tienen dueño que los re-valide cuando cambia la regla.
- Solución de fondo: reescribir los textos para que digan qué pasa de verdad (se entrega lo cobrado; la deuda queda a nombre de quien no pagó), una vez que Pablo decida A-2 y si existe o no un fondo comunitario (A-3). No inventar la regla en el texto.
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: apps/web (L6 para el shell), apps/movil tour (L4/L6); garantía como concepto.
- Estándares y normas: ISO/IEC 25010 adecuación funcional (corrección); reglas 91.5/91 (promesa de fondos), 95.
- Esfuerzo estimado: chico (textos) tras la decisión de negocio

### garantia-entregas-FLUJO-QR-001 — El cobro QR confirmado no mueve nada en entregas: el servicio no consume ningún evento y el fondeo lo dispara un operador
- Clase / Severidad / Confianza: FLUJO-QR (y APROB) / S2 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java` (sin `@KafkaListener`); `servicios/entregas/src/main/java/bo/aportaya/entregas/web/EntregasController.java:128-129`; `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/RecaudoPorHttp.java`; garantía solo consume `aporte_aplicado` y `transaccion_reversada` (`servicios/garantia/src/main/java/bo/aportaya/garantia/consumo/*.java`) para recargos, no para levantar restricciones ni cerrar expedientes
- Evidencia: `grep -rln KafkaListener servicios/entregas/src/main` → sin resultados. El fondeo pregunta a aportes por HTTP (lo confirmado es lo que afirma aportes) cuando un humano llama `POST /entregas/pozo-completo`.
- Cómo se comprobó: búsqueda de consumidores; lectura del controlador.
- Impacto: con R-NUEVA-1 lo único que debe mover el flujo es el pago confirmado por webhook. Hoy, aunque el último aporte del turno se confirme, la entrega no avanza hasta que alguien la fondee, autorice, emita y anote (APROB-001). La restricción del deudor tampoco se levanta al pagar (UX-A11Y-010).
- Causa de fondo: entregas diseñado como orquestación invocada por Tesorería.
- Solución de fondo: consumir el evento de periodo/turno cobrado (de aportes o nucleo-financiero, con pacto) y avanzar la entrega con estado persistido (saga), idempotente por `message_id`.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: aportes (`CU100RecibirWebhookPasarela`, lote L1), nucleo-financiero.
- Estándares y normas: ISO/IEC 25010 adecuación funcional; reglas 98.3, 96.7.3; R-NUEVA-1.
- Esfuerzo estimado: grande

### garantia-entregas-FLUJO-FALTANTE-001 — Con faltante y sin respaldo la entrega queda BLOQUEADA con el pozo completo como principal y la deuda pendiente sin dueño
- Clase / Severidad / Confianza: FLUJO-FALTANTE (y HUECO) / S2 / probable
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/RegistroDelFondeo.java` (ramas CON_PENDIENTE / BLOQUEADA_POR_FONDO_INCOMPLETO / incidencia FONDO_INCOMPLETO); `servicios/entregas/src/main/resources/application.yml:60-63` (SLA sintético PT24H); `docs/catalogo-de-flujos/entrega-del-turno.yaml:38-39`; ADR-051:113
- Evidencia: tabla de ramas en la sección 4.d; `monto_pendiente` se guarda en el fondeo sin participante deudor asociado; `reportada_por` de la incidencia es el operador.
- Cómo se comprobó: lectura del subagente de `RegistroDelFondeo` y reapertura del `application.yml` por el agente principal; las ramas no se ejecutaron.
- Impacto: con R-NUEVA-2 la rama A3 pasa a ser la regla de todos los faltantes: el beneficiario espera sin plazo definido por negocio, y la deuda «conservada» no se imputa a quien no pagó dentro de entregas.
- Causa de fondo: A-2 sin decidir.
- Solución de fondo: DECISIÓN REQUERIDA (A-2) — no se propone una regla. Lo técnico, cualquiera sea la decisión: imputar el pendiente a la obligación del participante (aportes/garantía) y que el estado se resuelva por evento de pago.
- Relación con el flujo nuevo: RESPALDO / FLUJO-QR
- Conexiones y módulos afectados: aportes, garantía.
- Estándares y normas: ISO/IEC 25010 completitud; regla 91.3.4.
- Esfuerzo estimado: medio (tras la decisión)

### garantia-entregas-COHERENCIA-001 — Estados de `ejecucion_aval` distintos entre DDL, caso de uso y regla R-GAR-04
- Clase / Severidad / Confianza: COHERENCIA (y DEFECTO) / S3 / confirmado
- Ubicaciones: `sql/10_tablas/08_garantia_incumplimiento/ejecucion_aval.sql:17`; `sql/40_reglas/restricciones.sql:2351`; `docs/CasosDeUso` CU-26
- Evidencia: CHECK `('ACEPTADA','EN_DISPUTA','NOTIFICADA','PAGADA','RECHAZADA')`; R-GAR-04 filtra `estado <> 'ANULADA'` (valor imposible); CU-26 usa `PARCIAL`, `ANULADA`, `INNECESARIA`, `EN_COBRANZA`.
- Impacto: el aval parcial y la anulación del caso de uso no se pueden representar.
- Solución de fondo: un catálogo de estados único en el modelo. · Estándares y normas: 97.4.7 · Esfuerzo: chico
- Relación con el flujo nuevo: ninguna (aval entre participantes, A-3)
- Causa de fondo: El catálogo de estados se escribió por separado en el modelo, el caso de uso y la regla.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: garantía; docs/CasosDeUso

### garantia-entregas-DUP-X-001 — `ClienteConCircuito` y `ConfiguracionDeCircuito` copiados en garantía y entregas
- Clase / Severidad / Confianza: DUP-X / S3 / confirmado (mismo nombre y tamaño: 128 y 28 líneas en ambos)
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/ClienteConCircuito.java`, `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/ClienteConCircuito.java`, y las dos `ConfiguracionDeCircuito.java`
- Evidencia: `wc -l` 128/128 y 28/28.
- Impacto: un arreglo de resiliencia o del token (SEG-009) hay que hacerlo dos veces; el código transversal está en `plataforma/comun-web`.
- Solución de fondo: una sola versión en `plataforma/comun-web`. · Relación: ninguna · Estándares y normas: ISO/IEC 25010 mantenibilidad · Esfuerzo: chico
- Causa de fondo: Cada servicio copió el adaptador en lugar de usar el de `plataforma/comun-web`.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: garantía, entregas, plataforma/comun-web

### garantia-entregas-REESCRITURA-001 — Tres mecanismos para ceder el turno: mercado de ofertas (muerto en la práctica), subasta y traspaso por votación
- Clase / Severidad / Confianza: REESCRITURA (y HUECO) / S3 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/HechosDeGruposPorHttp.java:45-46`; `GestionDeOfertas.java`/`RegistroDeOfertas.java`/`ComprarOfertaDeTurno.java` (mercado, CU-130); `SubastaDelCupo.java` (CU-136/64); `apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart:14-22` (traspaso)
- Evidencia: `public Optional<Turno> turno(UUID turnoId) { return Optional.empty(); }` → publicar oferta siempre se deniega; ninguna pantalla llama las operaciones del mercado (sección 6).
- Cómo se comprobó: lectura del adaptador; matriz de contratos del lote de frontends.
- Impacto: código, pruebas, mocks y contratos mantenidos para una función que no puede operar; tres reglas distintas para la misma intención.
- Solución de fondo: decidir cuál queda (Pablo) y retirar las otras con versión.
- Relación con el flujo nuevo: APROB (el traspaso por votación) · Conexiones: grupos, nucleo-financiero · Estándares y normas: ISO/IEC 25010 mantenibilidad · Esfuerzo: medio
- Causa de fondo: Cada carril agregó su mecanismo de cesión sin retirar el anterior.

### garantia-entregas-PISADA-001 — Historial de CU-22 con revert y reaplicación sobre `test`
- Clase / Severidad / Confianza: PISADA / S4 / sospecha
- Ubicaciones: `git log origin/test -- servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22*.java`
- Evidencia: `0c2e1488 revert: volver test a 59370bbd mientras se diagnostica el despliegue` seguido de `60f9fd1e Subida a TEST desde justin/feature/cierre-endurecimiento-2026-10-08 … endurecimiento integral` y `d8e103ec feat: endurecimiento integral …`.
- Cómo se comprobó: `git log --oneline` (historial completo tras `--deepen=300`).
- Impacto: indica que el módulo se revirtió y reaplicó en bloque; junto con DINERO-003 (legacy vivo) y DATOS-003 (dos diseños del expediente) sugiere soluciones conviviendo. No se diffearon los commits.
- Solución de fondo: revisar con el dueño qué quedó de cada lado. · Estándares y normas: — · Esfuerzo: chico
- Relación con el flujo nuevo: ninguna
- Causa de fondo: Integración en bloque de ramas largas sobre `test` (revert y reaplicación).
- Conexiones y módulos afectados: entregas

### garantia-entregas-PARCHE-001 — Parámetros «SINTETICOS» y un factor fijo «SOLO TEST» en la configuración principal del servicio
- Clase / Severidad / Confianza: PARCHE (y CONFIG) / S3 / confirmado
- Ubicaciones: `servicios/entregas/src/main/resources/application.yml:28,60-63,73-82`; `servicios/garantia/src/main/resources/application.yml:28`
- Evidencia: `# SINTETICO (ambiguedad del carril C): plazo para resolver un faltante extraordinario de caja … sla: PT24H`; `factor-fijo: ${ENTREGAS_SERVICIO_FACTOR_FIJO:}   # SOLO TEST`; `credencial-servicio: ${ENTREGAS_LIBRO_CREDENCIAL_SERVICIO:}` (valor por defecto vacío); circuit breaker con «Valores SINTETICOS».
- Cómo se comprobó: lectura (no se leyó la clase de validación de arranque; el comentario afirma «Config a medias = no arranca», no verificado).
- Impacto: decisiones de negocio y de resiliencia pendientes viajan como valores por defecto en producción; el servicio se autentica con una cuenta de usuario (teléfono, clave, TOTP) en lugar de una identidad de servicio.
- Solución de fondo: parámetros con procedencia (97.5.4); identidad de servicio emitida por plataforma (98.6.3); valores de prueba solo en perfil de prueba.
- Relación con el flujo nuevo: FLUJO-FALTANTE (SLA) · Estándares y normas: reglas 90.3.4, 97.5.4, 98.6.3 · Esfuerzo: medio
- Causa de fondo: Ambigüedades del carril C resueltas con valores provisorios en la configuración principal.
- Conexiones y módulos afectados: entregas, garantía, identidad (cuenta de servicio)

### garantia-entregas-FRAGIL-001 — Estados de la entrega y del expediente como cadenas literales repetidas en Java, SQL y front
- Clase / Severidad / Confianza: FRAGIL (y DDD) / S3 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java` (`"AUTORIZADA"` 5 veces; `List.of("AUTORIZADA", "EN_PROCESO_DESEMBOLSO")` en `CU28EmitirDesembolso.java:176` y `EntregaRepositorio.java:139`); CHECKs de `sql/10_tablas/04_entregas_fondo/entrega_fondo.sql`; `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/EstadoDelExpediente.java` (constantes `String`)
- Evidencia: `grep -rn '"AUTORIZADA"' servicios/entregas/src/main --include=*.java | wc -l` → 5.
- Impacto: si se renombra o agrega un estado (p. ej. al quitar AUTORIZADA por R-NUEVA-1), se rompe en el repositorio, el caso de uso, el CHECK y el front sin que el compilador avise; ninguna prueba cruza las tres capas.
- Solución de fondo: enum de dominio con mapeo único a la base y al contrato. · Estándares y normas: ISO/IEC 25010 mantenibilidad (modificabilidad) · Esfuerzo: medio
- Relación con el flujo nuevo: APROB (quitar AUTORIZADA es el primer cambio que R-NUEVA-1 exige)
- Causa de fondo: Obsesión por primitivos: los estados no son un tipo de dominio.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: entregas, garantía, sql, backoffice

### garantia-entregas-DEFECTO-002 — RLS de `registro_incumplimiento`/`deuda_participante` deja al imputado y al organizador sin ver el expediente; las pruebas corren con el rol dueño
- Clase / Severidad / Confianza: DEFECTO (y SEG, DEBIDO-PROCESO) / S2 / probable
- Ubicaciones: `sql/40_reglas/restricciones.sql:1437-1438` (`cond := 'fn_seg_rol_privilegiado() OR fn_seg_es_sistema()'` para tablas con `usuario_id` fuera de `visibles_por_titular`); `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU25DeclararIncumplimiento.java:157-163`; `servicios/garantia/src/test/java/bo/aportaya/garantia/BaseDeGarantia.java:73-75`
- Evidencia: rama «denegar por omisión» del recorrido RLS; el descargo busca el expediente con el contexto del imputado.
- Cómo se comprobó: lectura; falta la comprobación `SET ROLE svc_garantia` + contexto PARTICIPANTE contra una base con `sql/aplicar.sql` (no ejecutada: sin base en la nube).
- Impacto: si la hipótesis se confirma, el participante no puede presentar descargo («Ese expediente no existe») y `consultarRestriccion` devuelve `vigente=false` (falla abierta).
- Causa de fondo: lista `visibles_por_titular` no incluye las tablas de garantía; pruebas de integración con rol que no pasa por RLS.
- Solución de fondo: decidir visibilidad por titular en `docs/Restricciones.md`; pruebas de integración con el rol real de la aplicación.
- Relación con el flujo nuevo: ninguna · Conexiones: plataforma/comun-datos · Estándares y normas: ISO/IEC 25010 corrección; regla 80.2.4, 91.4 · Esfuerzo: medio

### garantia-entregas-DEFECTO-001 — `ejecutarReemplazo` responde 500 por `MapeoDeGarantia.dinero(null)`
- Clase / Severidad / Confianza: DEFECTO / S3 / probable
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU66ReemplazarParticipante.java` (`new SalidaReemplazo(reemplazoId, "EJECUTADO", null, null, false)`), `servicios/garantia/src/main/java/bo/aportaya/garantia/web/MapeoDeReemplazo.java`, `MapeoDeGarantia.java`
- Evidencia/Comprobación pendiente: reporte del subagente; el test web mockea montos no nulos. Falta ejecutar el endpoint.
- Impacto/Solución: 500 en una operación de grupo / mapear nulos. · Relación: APROB · Esfuerzo: chico
- Causa de fondo: El caso de uso devuelve montos nulos y el mapeo web asume no nulos; la prueba web mockea el caso de uso.
- Solución de fondo: Salida sin nulos o mapeo que los tolere, y prueba de integración del endpoint.
- Estándares y normas: ISO/IEC 25010 fiabilidad; regla 80.2.2
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: garantía, grupos (cupos)

### garantia-entregas-HUECO-001 — Cobertura del fondo, ejecución de aval y devolución del fondo no mueven dinero: sus eventos no tienen consumidor
- Clase / Severidad / Confianza: HUECO (y DINERO, TRAZA) / S2 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirIncumplimiento.java` (evento `incumplimiento_cubierto`), `CU26EjecutarAval.java` (`aval_ejecutado`), `CU29DevolverFondo.java` (`fondo_devuelto`); `FondoRepositorio.java` (`moverSaldo`)
- Evidencia: `grep -rnE 'garantia\.(incumplimiento_cubierto|aval_ejecutado|fondo_devuelto)'` fuera de `servicios/garantia`: sin resultados; el `@KafkaListener` de nucleo-financiero solo escucha los seis tópicos de respaldo y `desembolso_acreditado`.
- Impacto: el saldo cacheado de `fondo_garantia` baja pero no hay asiento ni movimiento de billetera: el fondo se desincroniza del mayor y quedan deudas por coberturas que nunca se pagaron.
- Causa de fondo: el saldo del fondo vive en garantía (caché) en vez de en el mayor (91.1.5).
- Solución de fondo: cada movimiento del fondo propone su asiento y lo registra nucleo-financiero; los saldos se leen del mayor.
- Relación con el flujo nuevo: RESPALDO (fondo mutual, A-3)
- Conexiones y módulos afectados: nucleo-financiero (lote L1).
- Estándares y normas: ISO/IEC 25010 corrección; reglas 91.1.5, 98.3.
- Esfuerzo estimado: grande
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.

### garantia-entregas-RESILIENCIA-001 — Recargo por mora que no encuentra fondo se reintenta 10 s y se descarta sin DLT ni reproceso
- Clase / Severidad / Confianza: RESILIENCIA / S2 / probable
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/IngresoAlFondo.java` (lanza si el grupo no tiene fondo y comenta «el reproceso lo aplica»); `servicios/garantia/src/main/java/bo/aportaya/garantia/trabajos/` (solo `.gitkeep`); manejador de errores en plataforma (`ConfiguracionMensajeria.java`)
- Evidencia: `ls servicios/garantia/src/main/java/bo/aportaya/garantia/trabajos` → solo `.gitkeep`; no hay reproceso.
- Impacto: el recargo queda en la billetera y el fondo no lo ve; mensaje perdido en silencio (98.3.4, 96.7.4).
- Solución de fondo: DLT con alerta y reproceso idempotente. · Conexiones: nucleo-financiero, plataforma/comun-mensajeria · Estándares y normas: 96.7, 98.3 · Esfuerzo: medio
- Relación con el flujo nuevo: ninguna (A-4: recargo de mora)
- Causa de fondo: Se delegó en un «reproceso» que no existe; el manejador de errores no tiene DLT.
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.

### garantia-entregas-RESILIENCIA-002 — `CierreDeSubastas` programado sin ShedLock y sagas de mercado sin recuperación de pasos colgados
- Clase / Severidad / Confianza: RESILIENCIA / S2 / probable
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/trabajos/CierreDeSubastas.java:34`; `SubastaDelCupo.java` (retener → fijar puja); `ComprarOfertaDeTurno.java` (`vencer`, `abortarColgadas` sin programar)
- Evidencia: `@Scheduled(` sin `@SchedulerLock` (la dependencia shedlock está en `build.gradle.kts`).
- Impacto: con varias réplicas, cierres concurrentes; plata de postores retenida si el proceso cae entre retener y fijar.
- Solución de fondo: ShedLock y job de recuperación de PENDIENTES con token de servicio. · Estándares y normas: 96.7.4 · Esfuerzo: medio
- Relación con el flujo nuevo: ninguna
- Causa de fondo: Pasos de saga sin estado de recuperación programado; ShedLock declarado como dependencia y no aplicado.
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: entregas, nucleo-financiero (retenciones), grupos

### garantia-entregas-DINERO-001 — `ejecutarEntrega` marca ENTREGADA con un monto libre y puede coexistir con una orden de desembolso en vuelo
- Clase / Severidad / Confianza: DINERO (y APROB) / S1 / confirmado (lectura; no ejecutado)
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/web/EntregasController.java:113-114`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22LiquidarEntrega.java:161-190`; `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/EntregaRepositorio.java:129-143`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java:123,173-178`
- Evidencia: `marcarEntregada(...)` hace `.set("monto_efectivamente_entregado", entregado.monto())` con `estado IN ('AUTORIZADA','EN_PROCESO_DESEMBOLSO')`, sin comparar con el neto; `emitir` solo cambia la orden (`cambiarEstado(dsl, ordenId, List.of("CREADA"), "ENVIADA_A_PROVEEDOR")`), la entrega sigue AUTORIZADA; al acreditar, `entregas.cambiarEstado(...)` se llama sin mirar su resultado y el asiento se emite igual.
- Cómo se comprobó: lectura de las cuatro ubicaciones.
- Impacto: una entrega puede quedar ENTREGADA a mano (incluso por un monto distinto, o con método `EFECTIVO_ORGANIZADOR`) y, además, acreditarse la orden y asentarse en el mayor: doble entrega o entrega sin dinero real.
- Causa de fondo: dos caminos de cierre de la entrega (legacy manual y CU-28) sin máquina de estados única.
- Solución de fondo: eliminar `ejecutar`; `emitir` lleva la entrega a EN_PROCESO_DESEMBOLSO con precondición; fallar si `cambiarEstado` devuelve false.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: nucleo-financiero.
- Estándares y normas: ISO/IEC 25010 corrección e integridad; reglas 91.1, 96.3.1-2.
- Esfuerzo estimado: medio

### garantia-entregas-DINERO-003 — `POST /entregas` (liquidación legacy) sigue vivo, confía en bruto/recaudado del cliente, ignora la clave de idempotencia y admite EFECTIVO_ORGANIZADOR
- Clase / Severidad / Confianza: DINERO (y REESCRITURA, HUECO) / S1 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/web/EntregasController.java:64-80`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22LiquidarEntrega.java:51-108`; `servicios/entregas/src/main/resources/openapi/entregas.yaml:897`; `sql/10_tablas/04_entregas_fondo/entrega_fondo.sql:31`
- Evidencia: `liquidarEntrega(UUID idempotencyKey, EntradaLiquidacion cuerpo)` → `cu22.liquidar(... MapeoDeEntregas.dinero(cuerpo.getBruto()), MapeoDeEntregas.dinero(cuerpo.getRecaudado()), … cuerpo.getMetodoDesembolso().getValue() …)` sin pasar `idempotencyKey`; enum `[BILLETERA_MOVIL, COMPENSACION_INTERNA, EFECTIVO_ORGANIZADOR, …]` y el CHECK de la base lo admite, mientras `entregas.yaml:787` dice «sin EFECTIVO_ORGANIZADOR a proposito, el organizador no es caja (RN-18)».
- Cómo se comprobó: lectura del controlador, contrato y DDL.
- Impacto: Tesorería puede crear una entrega con un pozo afirmado por ella, sin pasar por aportes; método en efectivo vía organizador contradice RN-18/91.5. Clon divergente del fondeo (`/entregas/pozo-completo`): dos reglas para lo mismo.
- Causa de fondo: el fondeo nuevo se agregó sin retirar la liquidación vieja (REESCRITURA).
- Solución de fondo: retirar `POST /entregas` o hacerlo delegar en el fondeo; sacar `EFECTIVO_ORGANIZADOR` del contrato y del modelo (`docs/entidades`, no del SQL generado).
- Relación con el flujo nuevo: ninguna directa (riesgo de dinero)
- Conexiones y módulos afectados: aportes (debería afirmar el recaudado).
- Estándares y normas: ISO/IEC 25010 corrección y mantenibilidad; reglas 91.1.4, 91.5, 90.1.7.
- Esfuerzo estimado: medio

### garantia-entregas-DINERO-002 — `saldoRetenido` lo afirma quien pide el desembolso
- Clase / Severidad / Confianza: DINERO / S2 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java:106-111,263`
- Evidencia: `if (!entrada.saldoRetenido()) { throw … "El saldo de la entrega no esta retenido" }` — el booleano viene de la entrada; no hay retención consultada en nucleo-financiero.
- Impacto: el control AP-CU28-04 no controla nada; se puede ordenar pago sin saldo retenido.
- Solución de fondo: crear o consultar la retención en nucleo-financiero dentro del caso de uso.
- Relación con el flujo nuevo: ninguna · Conexiones: nucleo-financiero · Estándares y normas: regla 90.1.7, 91 · Esfuerzo: medio
- Causa de fondo: La retención en nucleo-financiero no se integró; se dejó un booleano de entrada.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.

### garantia-entregas-DINERO-004 — Importes sin moneda o con moneda fija BOB en garantía
- Clase / Severidad / Confianza: DINERO (y DATOS) / S2 / confirmado (esquema) · probable (código)
- Ubicaciones: tablas sin columna `moneda` junto al importe: `movimiento_fondo`, `abono_recuperacion`, `cobertura_incumplimiento`, `politica_cobertura` (topes absolutos), `acuerdo_quita`, `castigo_deuda`, `devolucion_fondo`, `disolucion_anticipada`, `ejecucion_aval`, `aval_participante`, `gestion_cobranza`, `promesa_pago`, `accion_cobranza`, `sancion`, `lista_restriccion_interna`, `reemplazo_participante`, `liquidacion_participante`, `historial_*`, `registro_incumplimiento` (todas en `sql/10_tablas/08_garantia_incumplimiento/`) y `entregas.deduccion_entrega`; código: `ExpedienteRepositorio.java`, `GestionRepositorio.java`, `IndicadoresDeGarantia.java` (moneda fija BOB, según el subagente); contrato `garantia.yaml:689-696` (ver DINERO-010)
- Evidencia: matriz por tabla de la sección 6-bis; `grep -L moneda` sobre los archivos de la carpeta.
- Impacto: viola 91.1.2; un grupo en USD se registra y relee como BOB; las sumas mezclan monedas.
- Causa de fondo: modelo de datos anterior a la regla 91.
- Solución de fondo: agregar moneda en el modelo (`docs/entidades/*.puml` → DDL generado), expandir y contraer.
- Relación con el flujo nuevo: ninguna · Conexiones: nucleo-financiero · Estándares y normas: ISO 4217; regla 91.1.2, 97.1 · Esfuerzo: grande
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.

### garantia-entregas-DINERO-005 — Saldos del fondo cacheados y sumas ad-hoc en lugar del mayor
- Clase / Severidad / Confianza: DINERO / S2 / probable
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/FondoRepositorio.java` (`moverSaldo`, `consumido`), `GestionRepositorio.java` (`deudaViva`), `IndicadoresDeGarantiaRepositorio.java`; `sql/10_tablas/08_garantia_incumplimiento/fondo_garantia.sql`; sin control de cuadre en `sql/50_verificacion/verificaciones.sql` (sí lo hay para la reserva, `:118-134`)
- Evidencia: UPDATE de `saldo_disponible`/`saldo_comprometido`; SUM sobre tablas operativas para indicadores.
- Impacto: dos fuentes de verdad para el mismo saldo (91.1.5); el tablero de transparencia puede mostrar un saldo que el mayor no respalda (agravado por HUECO-001).
- Solución de fondo: saldo leído del mayor; verificación de cuadre en `50_verificacion`.
- Relación con el flujo nuevo: ninguna · Conexiones: nucleo-financiero, transparencia · Estándares y normas: regla 91.1.5 · Esfuerzo: grande
- Causa de fondo: El fondo de garantía se modeló con saldos propios en garantía en lugar de cuentas del mayor.
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.

### garantia-entregas-DINERO-006 — El mismo faltante se puede cubrir dos veces con `turnoId` distintos del mismo periodo
- Clase / Severidad / Confianza: DINERO / S2 / sospecha (moot si se retira RESPALDO-001)
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirFaltanteDelCorte.java` (usa `pedido.turnoId()` sin validarlo contra el recaudo); índice `uq_cobertura_respaldo_turno_viva` (`sql/30_indices/08_garantia_incumplimiento.sql`)
- Evidencia: la unicidad es por turno y por clave, no por periodo (reporte del subagente; el agente principal confirmó la validación de periodo-grupo en `:50` pero no la ausencia de validación del turno).
- Comprobación pendiente: prueba de integración con dos turnos para el mismo periodo.
- Impacto/Solución: doble aplicación de reserva / validar turno contra grupos y unicidad por periodo. · Relación: RESPALDO · Estándares y normas: 91.1.4 · Esfuerzo: chico
- Causa de fondo: El turno se acepta del pedido sin validarlo contra el recaudo ni contra grupos.
- Solución de fondo: Validar turno↔periodo↔grupo y unicidad por periodo (o retirar el respaldo, RESPALDO-001).
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: garantía, entregas, grupos

### garantia-entregas-DINERO-007 — `Dinero` del contrato admite negativos y la disolución/reemplazo usan cifras del cliente
- Clase / Severidad / Confianza: DINERO / S2 / probable
- Ubicaciones: `garantia.yaml` y `entregas.yaml` (`pattern: '^-?\d+\.\d{2}$'`); `CU66ReemplazarParticipante.java` (`deudaTotal.menos(asumida)`); `CU67DisolverGrupo.java` (masa, totales y posiciones del cuerpo)
- Impacto: un `deudaQueAsumeElEntrante` negativo infla la deuda retenida; la disolución reparte con totales no cotejados contra aportes/entregas.
- Solución de fondo: `pattern` sin signo para importes de entrada; leer totales de sus dueños. · Estándares y normas: 91.2.2, 90.1.7 · Esfuerzo: medio
- Evidencia: `Dinero.monto: { type: string, pattern: '^-?\d+\.\d{2}$' }` en ambos OpenAPI (reabierto en `garantia.yaml:695`); en CU66/CU67 según lectura del subagente.
- Relación con el flujo nuevo: ninguna
- Causa de fondo: Esquema `Dinero` común para entrada y salida, y cifras de negocio aceptadas del cliente.
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: garantía, aportes, entregas

### garantia-entregas-DINERO-009 — Sin `CHECK >= 0` y sin `clave_idempotencia UNIQUE` en la mayoría de los importes de garantía
- Clase / Severidad / Confianza: DINERO (y DATOS) / S2 / confirmado (muestreo del agente principal) · matriz completa del subagente en sección 6-bis
- Ubicaciones: `sql/10_tablas/08_garantia_incumplimiento/` — sin `>= 0`: `cobertura_incumplimiento`, `acuerdo_quita`, `castigo_deuda`, `deuda_participante` (salvo saldo), `aval_participante`, `promesa_pago`, `gestion_cobranza`, `abono_recuperacion.aplicado_*`, `subrogacion`, `movimiento_fondo` (sin indicar si el signo va en el número o en el tipo); sin clave de idempotencia: `movimiento_fondo`, `abono_recuperacion`, `castigo_deuda`, `acuerdo_quita`, `devolucion_fondo`, `ejecucion_aval`; `accion_cobranza.costo NUMERIC(10,2)`
- Impacto: viola 91.2.2 y 91.1.4/91.2.4; un reintento puede duplicar un movimiento del fondo.
- Solución de fondo: en el modelo + `docs/Restricciones.md`. · Estándares y normas: regla 91.1.4, 91.2 · Esfuerzo: medio
- Evidencia: Matriz por tabla del lote SQL (sección 12.b, `sql/10_tablas/08_garantia_incumplimiento/*.sql`); muestreo del agente principal en `deuda_participante.sql:25`, `ejecucion_aval.sql:17`.
- Relación con el flujo nuevo: ninguna
- Causa de fondo: Modelo de datos anterior a la regla 91; las restricciones se agregaron solo a las tablas nuevas (respaldo).
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: garantía (esquema)

### garantia-entregas-DINERO-010 — `RestriccionVigente.montoQueLaLevanta` es un importe sin moneda y la app inventa BOB
- Clase / Severidad / Confianza: DINERO (y CONTRATO) / S2 / confirmado
- Ubicaciones: `servicios/garantia/src/main/resources/openapi/garantia.yaml:689-696`, `apps/movil/lib/pantallas/pasanaku/pantalla_mi_estado.dart:91-93`
- Evidencia: `montoQueLaLevanta: { type: string, pattern: '^-?\d+\.\d{2}$' }` (sin `Dinero`); en la app: `// La restricción no trae moneda y la app opera solo en bolivianos` → `_importe(r.montoQueLaLevanta, 'BOB')`.
- Cómo se comprobó: lectura del contrato y de la pantalla.
- Impacto: viola 91.1.2 («prohibido un importe sin moneda»); el patrón admite negativos; si un grupo opera en USD (el contrato `Dinero` admite `[BOB, USD]`) la persona ve un monto en la moneda equivocada.
- Causa de fondo: el esquema se escribió con un string suelto en lugar de reusar `Dinero`; el front parcheó la presentación.
- Solución de fondo: `montoQueLaLevanta: $ref Dinero` (cambio de contrato versionado) y quitar el literal `'BOB'` del front.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: garantia → apps/movil; cliente Dart generado.
- Estándares y normas: ISO/IEC 25010 corrección funcional; ISO 4217; regla 91.1.2, 98.1.3.
- Esfuerzo estimado: chico

### garantia-entregas-DINERO-008 — Residuo de la devolución del fondo sin asignar
- Clase / Severidad / Confianza: DINERO / S3 / probable
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/DevolucionDelFondo.java`, `CU29DevolverFondo.java`
- Evidencia: el reparto topa al último aportante en lo aportado; si el saldo supera lo aportado (recargos) el excedente no se asigna ni se registra.
- Impacto/Solución: viola 91.6.5 («nunca se pierde: se asigna») / declarar regla de residuo (decisión de negocio). · Esfuerzo: chico
- Relación con el flujo nuevo: ninguna
- Causa de fondo: El reparto no define a quién va el residuo.
- Solución de fondo: Regla de residuo declarada (decisión de negocio) y registrada en el reparto.
- Estándares y normas: regla 91.6.5; ISO/IEC 25010 corrección
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: garantía, nucleo-financiero

### garantia-entregas-DINERO-011 — Subasta y puja envían siempre moneda BOB aunque la subasta esté en otra moneda
- Clase / Severidad / Confianza: DINERO / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/dominio/subasta_del_cupo.dart:82`, `:125`; `apps/movil/lib/pantallas/pasanaku/formulario_de_subasta.dart:116`; `apps/movil/lib/pantallas/pasanaku/formulario_de_puja.dart:72`
- Evidencia: `Dinero(monto: …, moneda: DineroMonedaEnum.BOB)`; la puja muestra `subasta.montoMinimo.moneda` pero envía BOB.
- Cómo se comprobó: lectura (subagente) y reapertura de `subasta_del_cupo.dart`.
- Impacto: en un grupo USD la persona ve USD y se envía BOB; el servicio debería rechazar por moneda distinta (si no lo hace es S1 — no comprobado).
- Causa de fondo: moneda constante en el cliente en lugar de tomarla del agregado.
- Solución de fondo: tomar la moneda de `montoMinimo`/`montoAporte`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: entregas (subasta CU-136).
- Estándares y normas: ISO/IEC 25010 corrección; regla 91.1.2.
- Esfuerzo estimado: chico

### garantia-entregas-DINERO-012 — `deuda_participante.acreedor` admite PLATAFORMA y no AVALISTA
- Clase / Severidad / Confianza: DINERO (y RESPALDO) / S3 / confirmado
- Ubicaciones: `sql/10_tablas/08_garantia_incumplimiento/deuda_participante.sql:25`; contraste con `CU26EjecutarAval.java:105` (subroga a `"AVALISTA"`)
- Evidencia: `CHECK (acreedor IN ('FONDO_GARANTIA','GRUPO','PLATAFORMA'))`.
- Impacto: la empresa como acreedora es el reflejo contable del respaldo (R-NUEVA-2); el avalista subrogado no tiene valor válido en la deuda.
- Solución de fondo: decidir A-3/A-4; agregar AVALISTA. · Estándares y normas: 91 · Esfuerzo: chico
- Relación con el flujo nuevo: RESPALDO (A-3/A-4)
- Causa de fondo: El acreedor refleja el diseño con respaldo empresarial.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: garantía (aval, subrogación)

### garantia-entregas-DINERO-013 — `fondo_garantia` sin control de cuadre contra `movimiento_fondo`
- Clase / Severidad / Confianza: DINERO / S3 / probable
- Ubicaciones: `sql/50_verificacion/verificaciones.sql:118-134` (cuadre solo para la reserva); `sql/10_tablas/08_garantia_incumplimiento/fondo_garantia.sql`
- Impacto/Solución: el saldo cacheado del fondo puede divergir sin alerta / verificación de cuadre (y ver DINERO-005). · Esfuerzo: chico
- Evidencia: `sql/50_verificacion/verificaciones.sql:118-134` verifica solo la reserva (lote SQL).
- Relación con el flujo nuevo: RESPALDO (fondo mutual, A-3)
- Causa de fondo: Saldo cacheado sin verificación (ver DINERO-005).
- Solución de fondo: Verificación de cuadre del fondo contra su libro y contra el mayor.
- Estándares y normas: regla 91.1.5
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: garantía, nucleo-financiero

### garantia-entregas-DEBIDO-PROCESO-001 — El aval se ejecuta y la deuda se subroga sin expediente firme y antes de que el avalista pague
- Clase / Severidad / Confianza: DEBIDO-PROCESO (y DINERO) / S1 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU26EjecutarAval.java:98-106`; tópico `garantia.aval_ejecutado` sin consumidor
- Evidencia: `gestion.subrogar(dsl, coberturaId, deuda.id(), "FONDO_GARANTIA", "AVALISTA", aEjecutar, ahora); gestion.marcarDeudaSubrogada(dsl, deuda.id(), deuda.version());` en el mismo paso que notifica (`ahora.plus(plazoDeRespuesta)`), sin consultar estado del expediente; el retorno de `marcarDeudaSubrogada` se ignora. `grep` de `aval_ejecutado` fuera de garantía: sin resultados.
- Cómo se comprobó: lectura; búsqueda de consumidores.
- Impacto: el patrimonio del avalista queda comprometido sin firmeza ni plazo de descargo cumplido (91.4.1/91.4.4), y la deuda cambia de acreedor sin que nadie haya pagado; nadie cobra al avalista.
- Causa de fondo: subrogación modelada como efecto de la notificación, no del pago.
- Solución de fondo: precondición de estado firme en la escritura; subrogar solo al consumir el pago confirmado del avalista (webhook firmado).
- Relación con el flujo nuevo: FLUJO-QR (el pago del avalista) — el aval no es R-NUEVA-2 (A-3)
- Conexiones y módulos afectados: aportes/nucleo-financiero (pago del avalista), reputación.
- Estándares y normas: ISO/IEC 25010 corrección; reglas 91.4, 98.3.
- Esfuerzo estimado: medio

### garantia-entregas-DEBIDO-PROCESO-002 — Sin ruta de apelación en garantía; restricción del deudor sin exigir firmeza
- Clase / Severidad / Confianza: DEBIDO-PROCESO / S2 / probable
- Ubicaciones: `servicios/garantia/src/main/resources/openapi/garantia.yaml` (no hay operación de apelación; la tabla `apelacion_sancion` existe); `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU27RestringirDeudor.java`
- Evidencia: `grep -n apelaci` sobre el openapi de garantía sin operaciones.
- Impacto: 91.4.3 exige que la ruta de apelación exista en el código.
- Solución de fondo: implementar apelación (o ubicar el servicio dueño) y precondición de firmeza en restricción. · Relación: R-NUEVA-1 no elimina el debido proceso · Esfuerzo: medio
- Causa de fondo: La apelación quedó en el modelo (`apelacion_sancion`) sin caso de uso ni endpoint.
- Estándares y normas: regla 91.4.3; ISO/IEC 25010 completitud
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: garantía, reputación (transparencia)

### garantia-entregas-DEBIDO-PROCESO-003 — Semilla de `matriz_sancion` con afectación de reputación automática y sin revisión humana
- Clase / Severidad / Confianza: DEBIDO-PROCESO (y DATOS) / S2 / confirmado
- Ubicaciones: `sql/60_semillas/18-sanciones-y-cobranza.sql:16`
- Evidencia: `('APORTE_ATRASADO', 'LEVE', 2, 'AFECTACION_REPUTACION', 25.0, NULL, TRUE, FALSE)` (`es_automatica=TRUE`, `requiere_revision_humana=FALSE`).
- Impacto: reputación afectada sin expediente FIRME (91.4.2, 91.4.4). Tensión con R-NUEVA-1 que no se resuelve acá: DECISIÓN REQUERIDA.
- Solución de fondo: que la reputación sea consecuencia del expediente firme; decidir con Pablo qué automatiza R-NUEVA-1 en sanciones. · Estándares y normas: regla 91.4 · Esfuerzo: chico
- Relación con el flujo nuevo: APROB (tensión R-NUEVA-1 vs 91.4)
- Causa de fondo: Matriz sembrada con parámetros sin revisión de Cumplimiento.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: garantía, transparencia (reputación)

### garantia-entregas-CLEAN-001 — Código y textos muertos
- Clase / Severidad / Confianza: CLEAN / S4 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/EstadoDelExpediente.java:58` (`admiteTransicion`, sin referencias); `apps/movil/lib/pantallas/pasanaku/textos.dart:113-116` (`tituloEntrega`, `solicitarEntrega`, `entregaEnCurso`, sin uso)
- Evidencia: `grep -rn admiteTransicion servicios/garantia/src` → solo la declaración; `grep` de los textos → solo su definición.
- Impacto: una regla de transición escrita y no aplicada; la persona participante no tiene pantalla de entrega ni de deducciones (91.3.4 sin superficie para quien cobra).
- Solución de fondo: usar `admiteTransicion` en la escritura o borrarlo; decidir la pantalla de entrega. · Estándares y normas: ISO/IEC 25010 mantenibilidad · Esfuerzo: chico
- Relación con el flujo nuevo: ninguna
- Causa de fondo: Funcionalidad empezada y no conectada.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: garantía, apps/movil

### garantia-entregas-ACOPLE-001 — Claves foráneas entre esquemas desde garantía (76) y entregas (25) contra grupos, identidad, aportes y nucleo_financiero
- Clase / Severidad / Confianza: ACOPLE (y COHERENCIA) / S2 / confirmado
- Ubicaciones: `sql/20_claves/08_garantia_incumplimiento.sql`, `sql/20_claves/04_entregas_fondo.sql`; justificación en `sql/00_base/02_esquemas.sql:4-5`; trigger que lee `cuenta_billetera` desde garantía (`sql/40_reglas/restricciones.sql:1955-1970`, `fn_grp_validar_disolucion`)
- Evidencia: `grep -c 'REFERENCES grupos\.\|REFERENCES identidad\.\|REFERENCES aportes\.\|REFERENCES nucleo_financiero\.'` → 76 y 25. Cabecera: «ADR-017: se parte el DESPLIEGUE, no el modelo. Las claves foraneas entre esquemas se conservan».
- Cómo se comprobó: conteo; lectura de la cabecera. El código Java de producción no lee esquemas ajenos (los `DSL.name("garantia", …)`/`("entregas", …)` son propios); las fixturas de prueba de entregas sí escriben en `identidad`, `grupos` y `aportes`.
- Impacto: contradicción directa entre ADR-017 y la regla 98.1.1-2; no se puede separar la base de un servicio sin romper el resto. El trigger de disolución (sin esquema calificado) podría fallar con el rol `svc_garantia` (sospecha, kill-test: `SET ROLE svc_garantia; UPDATE … SET estado='CERRADA'`).
- Solución de fondo: decisión de arquitectura registrada (ADR) sobre cuál manda; si manda 98.1, reemplazar FK cruzadas por referencias lógicas y validación por API/evento.
- Relación con el flujo nuevo: ninguna · Estándares y normas: regla 98.1.1-2; ISO/IEC 25010 modularidad · Esfuerzo: grande
- Causa de fondo: ADR-017 («se parte el despliegue, no el modelo») choca con la regla 98.1.
- Conexiones y módulos afectados: grupos, identidad, aportes, nucleo_financiero, notificaciones, tarifas

### garantia-entregas-CONTRATO-002 — Sin pacto para las llamadas HTTP que mueven dinero (entregas→garantía, garantía→aportes, entregas→aportes, garantía→cumplimiento)
- Clase / Severidad / Confianza: CONTRATO / S2 / confirmado
- Ubicaciones: `contratos/pactos/` (10 pactos; ninguno entregas↔garantía ni garantía→aportes); `servicios/entregas/.../RespaldoPorHttp.java:56`, `servicios/entregas/.../RecaudoPorHttp.java`, `servicios/garantia/.../RecaudoPorHttp.java`, `servicios/garantia/.../EfectoDeReclamosPorHttp.java`; afirmación contraria en `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml:4`
- Evidencia: `ls contratos/pactos/` lista solo `nucleo-financiero-entregas.json`, `nucleo-financiero-garantia-aporte-aplicado.json`, `nucleo-financiero-garantia-transaccion-reversada.json` como relacionados.
- Impacto: un cambio en `/aportes/periodos/{id}/recaudo` o en `/garantia/respaldo/coberturas` rompe el fondeo sin que CI lo avise (98.1.4).
- Solución de fondo: pactos consumer-driven por cada salto con verificación del lado productor.
- Relación con el flujo nuevo: FLUJO-QR (el recaudo es lo que afirma el cobro) · Estándares y normas: regla 98.1.4 · Esfuerzo: medio
- Causa de fondo: Los pactos se escribieron solo para eventos de nucleo-financiero.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: entregas, garantía, aportes, cumplimiento

### garantia-entregas-CONTRATO-001 — Clave `Idempotency-Key` exigida y descartada en `declararIncumplimiento` y `liquidarEntrega`
- Clase / Severidad / Confianza: CONTRATO (y DINERO) / S3 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/web/IncumplimientosController.java:70`; `servicios/entregas/src/main/java/bo/aportaya/entregas/web/EntregasController.java:65`
- Evidencia: el parámetro `idempotencyKey` no se pasa al caso de uso.
- Impacto: el contrato promete idempotencia por clave y deduplica por otra cosa (obligación o turno); `cubrirIncumplimiento`, `ejecutarAval`, `devolverFondo`, `disolucion` no llevan clave (91.1.4).
- Solución de fondo: propagar y persistir la clave con `UNIQUE`. · Estándares y normas: 91.1.4 · Esfuerzo: chico
- Relación con el flujo nuevo: ninguna
- Causa de fondo: El controlador generado recibe la clave y el caso de uso no la modela.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: garantía, entregas

### garantia-entregas-CONTRATO-003 — Pacto `nucleo-financiero-entregas.json` sin verificación del lado productor (entregas)
- Clase / Severidad / Confianza: CONTRATO / S3 / probable
- Ubicaciones: `contratos/pactos/nucleo-financiero-entregas.json`; consumidor `PactoConEntregasContratoTest.java:31` («Lo que NO está cubierto: el proceso real de entregas respondiendo»)
- Evidencia: búsqueda del nombre del pacto en `servicios/entregas/src/test`: sin resultados (subagente; no re-ejecutada por el agente principal).
- Impacto/Solución: contrato de oferta reservada sin garantía del productor / test de verificación en entregas. · Estándares y normas: 98.1.4 · Esfuerzo: chico
- Relación con el flujo nuevo: ninguna
- Causa de fondo: Falta la verificación del lado productor (98.1.4).
- Solución de fondo: Prueba de verificación del pacto en `servicios/entregas/src/test`.
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: entregas, nucleo-financiero

### garantia-entregas-CONTRATO-010 — La app lee solo la primera página de `listarSubastasDeCupo`
- Clase / Severidad / Confianza: CONTRATO / S4 / probable
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/dominio/subasta_del_cupo.dart:13-24`; `entregas.yaml:515` («Paginado, con orden estable»)
- Evidencia: el provider no usa el cursor.
- Cómo se comprobó: lectura del subagente.
- Impacto: subastas fuera de la primera página invisibles.
- Causa de fondo / Solución de fondo: falta paginación en el provider / consumir el cursor.
- Relación con el flujo nuevo: ninguna · Conexiones: entregas · Estándares y normas: ISO/IEC 25010 completitud; 96.6 · Esfuerzo: chico

### garantia-entregas-DATOS-002 — Parámetros financieros sembrados sin procedencia ni moneda
- Clase / Severidad / Confianza: DATOS / S2 / confirmado
- Ubicaciones: `sql/60_semillas/02-politicas.sql:24-30` (desde `seeders/minimos/02-politicas.json:4`); `sql/60_semillas/18-sanciones-y-cobranza.sql:49-56` (quitas 20/40/70 %)
- Evidencia: `(5.0, 5, 100.0, 3000.0, 10000.0, 3, 2000.0, 90, 1.5, now(), FALSE)`; procedencia: `"revisar_con": "área de riesgos"`.
- Impacto: topes, umbral de aprobación, recargo y plazos inferidos (97.5.4) y sin moneda; las semillas sí son idempotentes (`IF NOT EXISTS`).
- Solución de fondo: fuente con fecha y responsable de negocio, o declararlos explícitamente sintéticos. · Estándares y normas: regla 97.4.2, 97.5.4 · Esfuerzo: chico
- Relación con el flujo nuevo: RESPALDO (política de cobertura, A-3)
- Causa de fondo: Parámetros puestos para poder arrancar, sin dueño de negocio.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: garantía, organizador/riesgos

### garantia-entregas-DATOS-003 — `registro_incumplimiento` es append-only pero declara columnas que la lógica necesita mutar
- Clase / Severidad / Confianza: DATOS (y PISADA) / S3 / confirmado
- Ubicaciones: `sql/10_tablas/08_garantia_incumplimiento/registro_incumplimiento.sql:19,40` (`estado`, `version`, `cerrado_en`, `monto_recuperado`); `sql/00_base/03_permisos.sql:173`; `sql/35_append_only/append_only.sql:201-203`; `servicios/garantia/.../dominio/EstadoDelExpediente.java`
- Evidencia: `REVOKE UPDATE, DELETE ON garantia.registro_incumplimiento FROM svc_garantia;` + trigger; el código deriva el estado del historial.
- Impacto: columnas muertas o engañosas (el `estado` de la fila nunca cambia); la regla de cierre motivado no se puede cumplir tras el INSERT. Dos diseños conviviendo.
- Solución de fondo: quitar del modelo las columnas mutables o exponerlas como vista calculada. · Estándares y normas: regla 97.2 · Esfuerzo: chico
- Relación con el flujo nuevo: ninguna
- Causa de fondo: Dos diseños del expediente conviviendo (fila mutable vs historial append-only).
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: garantía

### garantia-entregas-DATOS-004 — `deduccion_entrega` y `fondeo_entrega` mutables (no append-only)
- Clase / Severidad / Confianza: DATOS (y TRAZA) / S3 / probable
- Ubicaciones: `sql/35_append_only/append_only.sql` (ninguna tabla de entregas); `servicios/entregas/.../infraestructura/FondeoRepositorio.java` (UPDATE del fondeo)
- Impacto: la explicación línea a línea de una entrega (91.3.4) puede reescribirse sin historial.
- Solución de fondo: append-only o historial versionado. · Estándares y normas: 91.1.3, 90.2.7 · Esfuerzo: medio
- Evidencia: `grep -n entregas sql/35_append_only/append_only.sql` sin tablas de entregas (lote SQL); UPDATE en `FondeoRepositorio.java` (lote entregas).
- Relación con el flujo nuevo: ninguna
- Causa de fondo: Las tablas de entregas no se incluyeron en el sellado append-only.
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: entregas

### garantia-entregas-PRUEBAS-001 — Pruebas que validan el flujo viejo (respaldo y aprobaciones) y faltan matrices negativas
- Clase / Severidad / Confianza: PRUEBAS / S3 / confirmado (inventario de `@DisplayName`)
- Ubicaciones: garantía: `CU131*`, `CU133ContingenciaTest`, `RespaldoCoberturaWebTest`, `RespaldoControllerWebTest`, `CU23Test` (criterio1 cubre en NOTIFICADO), `CoberturaWebTest`, `CU66Test`; entregas: `CU133PozoCompletoTest`, `CU133PozoCompletoRechazosTest`, `CU133PozoCompletoConcurrenciaTest`, `CU133EntregaConsistenteTest`, `AdaptadoresDelPozoTest`, `RespaldoCaidaTest`, `FondeoDelPozoWebTest`; `SeguridadWebTest` en ambos (solo 401/403 por rol)
- Evidencia: `python3 scripts/matriz_de_pruebas.py --servicio entregas` → «Sin prueba web (5): … /entregas/subastas …» (ver `evidencia/05-verificadores.txt`).
- Impacto: al retirar el respaldo las suites fallarán por diseño; la matriz negativa de BOLA (otro organizador, otro grupo, otro usuario) no existe, por eso SEG-002 y SEG-006 no los detecta ninguna prueba.
- Solución de fondo: reescribir las pruebas contra R-NUEVA-1/2 cuando se decida A-2/A-3; agregar matriz negativa por endpoint (80.2.3).
- Relación con el flujo nuevo: APROB / RESPALDO · Estándares y normas: regla 80.2.3, 80.5 · Esfuerzo: grande
- Causa de fondo: Las pruebas se escribieron contra ADR-051 y solo con matriz de rol.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: garantía, entregas

### garantia-entregas-PRUEBAS-010 — Mocks de `packages/simulado` sin escenario adverso real y con códigos de error ajenos
- Clase / Severidad / Confianza: PRUEBAS (y CONTRATO) / S3 / probable
- Ubicaciones: `packages/simulado/ejemplos/garantia/*.json`, `packages/simulado/ejemplos/entregas/*.json` (41 archivos); ejemplos: `listarSubastasDeCupo` usa AP-CU64-01; `listarDesembolsos` usa AP-CU28-01 (el contrato declara AP-CU28-07); `consultarRestriccion.json` sin `rechazo` y `nivel: "nivel"`.
- Evidencia: comparación por script del subagente (claves de primer nivel y códigos de estado): escenario `adverso` idéntico a `ok` en las 41; 34 mocks emiten 202/503 no declarados.
- Cómo se comprobó: script del subagente; no re-ejecutado por el agente principal.
- Impacto: las pantallas desarrolladas contra el simulador nunca ejercitan error real (regla 65 exige tres niveles).
- Causa de fondo: mocks generados por plantilla.
- Solución de fondo: generar los mocks desde el OpenAPI con los códigos de error de cada operación.
- Relación con el flujo nuevo: ninguna · Conexiones: packages/simulado (L6) · Estándares y normas: regla 65, 80 · Esfuerzo: medio

### garantia-entregas-UX-A11Y-010 — «Se levanta pagando $monto» sin forma de pagar desde la pantalla
- Clase / Severidad / Confianza: UX-A11Y (y APROB) / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/textos.dart:146-148`, `apps/movil/lib/pantallas/pasanaku/pantalla_mi_estado.dart` (bloque de restricción), `servicios/garantia/.../web/CobranzaController.java` (`levantarRestriccion`, permiso `GRUPO_ADMINISTRAR`)
- Evidencia: `'Se levanta pagando $monto.'`; la pantalla no ofrece orden de cobro QR; en el backend levantar la restricción es un acto humano con motivo escrito.
- Cómo se comprobó: lectura de textos y de la pantalla; endpoint en `garantia.yaml` (`levantarRestriccion`).
- Impacto: la pantalla promete un efecto automático que hoy depende de una persona; aviso no accionable (95.2.3).
- Causa de fondo: el levantamiento no está atado al evento de pago confirmado.
- Solución de fondo: levantar la restricción por consumo del evento de aporte cobrado (webhook firmado) y ofrecer el botón de pago QR.
- Relación con el flujo nuevo: APROB / FLUJO-QR
- Conexiones y módulos afectados: aportes (orden QR), garantia.
- Estándares y normas: ISO/IEC 25010 usabilidad; reglas 95.2, R-NUEVA-1.
- Esfuerzo estimado: medio

### garantia-entregas-UX-A11Y-011 — Estados de puja: «ya volvió a tu billetera» mostrado para SUPERADA; GANADORA/RECHAZADA/PENDIENTE sin pintar
- Clase / Severidad / Confianza: UX-A11Y (y DINERO) / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/pantalla_subasta.dart:64-68`, `apps/movil/lib/pantallas/pasanaku/textos_de_la_subasta.dart:65`, enum en `servicios/entregas/src/main/resources/openapi/entregas.yaml:691`
- Evidencia: SUPERADA y LIBERADA comparten «Te superaron: tu saldo ya volvió a tu billetera».
- Cómo se comprobó: lectura (subagente), ubicación reabierta.
- Impacto: afirma un movimiento de dinero no confirmado; estados del contrato sin representación.
- Causa de fondo: mapeo de estados incompleto en la vista.
- Solución de fondo: un texto por estado; «ya volvió» solo con LIBERADA.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: entregas (subasta).
- Estándares y normas: ISO/IEC 25010 corrección y usabilidad; regla 95.2, 98.4.1.
- Esfuerzo estimado: chico

### garantia-entregas-UX-A11Y-012 — Errores del servidor de monto en subasta/puja no se asocian al campo
- Clase / Severidad / Confianza: UX-A11Y / S3 / probable
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/formulario_de_puja.dart:88-95`, `apps/movil/lib/pantallas/pasanaku/formulario_de_subasta.dart:180-189`
- Evidencia: rechazos AP-CU136-03/04/07 (del monto) se pintan como `Alerta` sobre el botón; `CampoMonto` tiene parámetro `error` sin usar.
- Cómo se comprobó: lectura del subagente; no reabierto línea a línea por el agente principal (por eso «probable»).
- Impacto: incumple 95.3.2; lectores de pantalla no vinculan el error al campo. Lo escrito sí se preserva y el doble envío sí está bloqueado.
- Causa de fondo: mapeo de error genérico.
- Solución de fondo: mapear códigos de monto a `CampoMonto.error`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: entregas.
- Estándares y normas: WCAG 2.2 (3.3.1); regla 95.3.
- Esfuerzo estimado: chico

### garantia-entregas-UX-A11Y-013 — Valores literales de tipografía en la bandeja de desembolsos
- Clase / Severidad / Confianza: UX-A11Y / S4 / probable
- Ubicaciones: `apps/backoffice/src/app/rutas/operacion/desembolsos/bandeja-de-desembolsos.ts:67`, `apps/backoffice/src/app/rutas/operacion/comun/importe-sin-moneda.ts:16`
- Evidencia: `max-width: 10ch`, `font-size: .92em`, `font-size: .875rem`.
- Impacto/Solución: incumple 95.1.5; usar tokens de `packages/tokens`. · Relación: ninguna · Esfuerzo: chico
- Causa de fondo: Estilos locales en el componente.
- Solución de fondo: Tokens de `packages/tokens`.
- Estándares y normas: regla 95.1.5
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: backoffice, packages/tokens

### garantia-entregas-OBSERV-001 — Logs con beneficiario y montos atribuibles
- Clase / Severidad / Confianza: OBSERV (y SEG) / S3 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22LiquidarEntrega.java:105,148,189`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java:138`
- Evidencia: `BITACORA.info("CU-22 · Entrega liquidada: entrega={}, grupo={}, beneficiario={}, neto={}", …)`; `"… orden={}, entrega={}, monto={}"`.
- Impacto: importe atribuible a una persona en logs (90.2.1). No se comprobó si el enmascarador de PII de la configuración de logs lo cubre (no corre en la nube).
- Solución de fondo: loguear solo identificadores. · Estándares y normas: regla 90.2.1 · Esfuerzo: chico
- Relación con el flujo nuevo: ninguna
- Causa de fondo: Bitácora de casos de uso críticos agregada con datos de negocio (commit `d2d8c26b`).
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.
- Conexiones y módulos afectados: entregas, plataforma (enmascarador de logs)

### garantia-entregas-DOC-002 — ADR-051, CU-22/23/24, catálogo `aporte-mora-cobertura.yaml` y `entrega-del-turno.yaml` siguen afirmando que la empresa aporta el faltante
- Clase / Severidad / Confianza: DOC (y COHERENCIA, RESPALDO) / S3 / confirmado
- Ubicaciones: `docs/Arquitectura/ADR-051 Garantía del pozo, comisión y autogestión.md:34`; `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml:3,9,40,108-128,159`; `docs/catalogo-de-flujos/entrega-del-turno.yaml:8,41-55,104-110`; `docs/CasosDeUso` CU-22/CU-23; `servicios/entregas/src/main/resources/openapi/entregas.yaml:324-353` (`fondearPozo`: «lo que falte lo cubre `garantia`»); `servicios/garantia/src/main/resources/openapi/garantia.yaml:64,434-560`
- Evidencia: ADR-051:34 «Recibir mesa es recibir el pozo completo del turno; la empresa aporta el faltante (regla 7)»; catálogo: pasos `cubrir-faltante` → `recuperar-respaldo`, «La empresa cubre el faltante del corte».
- Cómo se comprobó: `grep` y lectura de las líneas. Además `python3 scripts/validar_flujos.py` da `OK · aporte-mora-cobertura.yaml: 12 pasos` (`evidencia/05b`): el verificador valida el flujo viejo como correcto.
- Impacto: según la prioridad de evidencia (regla 00 §8) manda la regla de negocio explícita de Pablo (R-NUEVA-2), pero toda la documentación y el verificador sostienen lo contrario; un agente que lea el repo reimplementará el respaldo.
- Solución de fondo: ADR que supersede ADR-051 (regla 7, D-02/D-03/D-15), catálogo y CU actualizados, verificador de flujos alineado.
- Relación con el flujo nuevo: RESPALDO · Conexiones: L1 (aportes, mayor) · Estándares y normas: regla 00 §8 · Esfuerzo: medio
- Causa de fondo: R-NUEVA-2 (2026-11-20) no tiene todavía ADR que supersede ADR-051.

### garantia-entregas-DOC-010 — Backoffice y catálogos de diseño todavía hablan de cobertura del fondo y «fondo completo»
- Clase / Severidad / Confianza: DOC (y RESPALDO) / S3 / confirmado por el subagente, muestreado
- Ubicaciones: `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-tablero.ts:25`, `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-operacion.ts:36-38,73`, `apps/backoffice/src/app/rutas/tablero/dominio/catalogo-de-tableros.ts:31-33`, `packages/ui/src/catalogo/muestras.ts:30`, `packages/diseno_flutter/lib/catalogo/muestras.dart:606`
- Evidencia: «Impagos cubiertos», «Recuperación de lo cubierto», «Cobertura del fondo»; «el fondo cubre la diferencia».
- Impacto: indicadores y ayudas que presuponen R-NUEVA-2 incumplida.
- Solución de fondo: retirar/renombrar tras decidir A-3.
- Relación con el flujo nuevo: RESPALDO · Conexiones: backoffice (L6), transparencia · Estándares y normas: regla 00 §6 · Esfuerzo: chico
- Causa de fondo: Textos escritos contra ADR-051.
- Cómo se comprobó: lectura de las ubicaciones citadas, reabiertas por el agente principal, más la búsqueda indicada en Evidencia.

### garantia-entregas-DOC-001 — README y comentarios dicen que nadie consume el asiento, y sí se consume
- Clase / Severidad / Confianza: DOC / S4 / probable
- Ubicaciones: `servicios/garantia/README.md:70`; `servicios/garantia/src/test/java/bo/aportaya/garantia/MayorDeDoble.java`; `servicios/entregas/src/test/java/bo/aportaya/entregas/CU133EntregaConsistenteTest.java`
- Impacto/Solución: lector engañado sobre el efecto en el mayor / corregir. · Esfuerzo: chico
- Evidencia: Lectura del subagente; el agente principal confirmó que el consumidor existe (`ConsumidorDeAsientosKafka`, §3).
- Relación con el flujo nuevo: RESPALDO
- Causa de fondo: El consumidor se agregó después y la documentación no se actualizó.
- Solución de fondo: Corregir README y comentarios.
- Estándares y normas: ISO/IEC 25010 mantenibilidad (analizabilidad)
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: garantía, entregas, nucleo-financiero

### garantia-entregas-SEG-001 — `recuperarRespaldo` traslada dinero del pozo a la caja de la empresa con un pago que afirma el cliente
- Clase / Severidad / Confianza: SEG (y DINERO, RESPALDO) / S1 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/web/RespaldoWeb.java:152-157`; `servicios/garantia/src/main/java/bo/aportaya/garantia/web/GarantiaController.java:185-188`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23RecuperarRespaldo.java:68-142`; `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/AsientoPropuesto.java:57-58`
- Evidencia: `new CU23RecuperarRespaldo.EntradaRecuperacion(cuerpo.getPagoId(), cuerpo.getObligacionId(), MapeoDeGarantia.dinero(cuerpo.getPago()))`; el caso de uso no consulta aportes ni ningún webhook: usa `e.pago()` y emite `recuperacion(...) = par("COBERTURA", id, Cuenta.CAJA_EMPRESA, Cuenta.POZO_GRUPO, monto)`.
- Cómo se comprobó: lectura completa de `CU23RecuperarRespaldo.recuperar`; el tope es lo cubierto en la línea, no un pago verificado.
- Impacto: un `ADMIN_PLATAFORMA` puede declarar un `pagoId` y un importe inventados y mover saldo del pozo del grupo a la caja de la empresa en el mayor. Viola 91.3.1 (no es pago hasta que la pasarela confirme) y R-NUEVA-1.
- Causa de fondo: la recuperación se diseñó como acto administrativo, no como consumo del evento de pago confirmado.
- Solución de fondo: eliminar el endpoint junto con el respaldo. Si sobrevive alguna imputación, que sea solo por consumo del evento de aporte cobrado, con el importe afirmado por aportes.
- Relación con el flujo nuevo: RESPALDO / FLUJO-QR
- Conexiones y módulos afectados: garantía → nucleo-financiero; aportes (debería ser la fuente).
- Estándares y normas: ISO/IEC 25010 seguridad (integridad); CWE-345; OWASP API Security Top 10 (API6/2023 flujos sensibles); reglas 91.3.1, 90.1.7.
- Esfuerzo estimado: chico (retirar) / medio (rehacer por evento)

### garantia-entregas-SEG-002 — Los endpoints `GRUPO_ADMINISTRAR` de garantía no comprueban que el organizador sea de ese grupo (BOLA)
- Clase / Severidad / Confianza: SEG / S1 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/web/GarantiaController.java:63-137` (devolución, reemplazo, disolución); `servicios/garantia/src/main/java/bo/aportaya/garantia/web/IncumplimientosController.java` (declarar, cubrir `:143-153`, aval, restricción, reemplazo, resolver descargo); `servicios/garantia/src/main/java/bo/aportaya/garantia/web/CobranzaController.java` (`levantarRestriccion`); guardia `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/GuardiaDePermiso.java` (`codigo.equals(contexto.rol()) || sesion.permisos().contains(codigo)`); RLS `sql/40_reglas/restricciones.sql:1437-1462`
- Evidencia: la guardia solo mira el permiso; `grep -rniE 'organizador|esMiembro|pertenece'` en `servicios/garantia/src/main` no encuentra ninguna comprobación de relación con el grupo (la única comprobación de propiedad es `CobranzaController.java:49` y `CU25DeclararIncumplimiento.java:162`). Las tablas sin `usuario_id` (fondo_garantia, disolucion_anticipada, reemplazo_participante, cobertura_*) no reciben política RLS en `fn_seg_aplicar_rls`.
- Cómo se comprobó: lectura de la guardia, del recorrido RLS y búsqueda de chequeos de relación.
- Impacto: cualquier persona con rol ORGANIZADOR (la semilla le da `GRUPO_ADMINISTRAR`, `sql/60_semillas/10-roles-y-permisos.sql:152`) puede disolver, devolver el fondo, reemplazar participantes, ejecutar avales o cubrir incumplimientos de un grupo ajeno.
- Causa de fondo: autorización solo por rol; falta la dimensión «propiedad/relación» (90.1.2).
- Solución de fondo: política de autorización por relación (organizador del grupo resuelto contra grupos o por claim firmado con alcance), aplicada en la escritura, y matriz negativa de pruebas.
- Relación con el flujo nuevo: APROB (son pasos humanos)
- Conexiones y módulos afectados: grupos (fuente de la relación), plataforma/comun-web.
- Estándares y normas: ISO/IEC 25010 seguridad; OWASP API1:2023 (BOLA); CWE-639; reglas 90.1.2-3, 96.5.2.
- Esfuerzo estimado: medio

### garantia-entregas-SEG-003 — El organizador cubre un incumplimiento con días de mora y aprobador que manda en el cuerpo
- Clase / Severidad / Confianza: SEG (y APROB, DEBIDO-PROCESO, DINERO) / S1 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/web/IncumplimientosController.java:143-153`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirIncumplimiento.java:71-127`
- Evidencia: `MapeoDeGarantia.dinero(cuerpo.getMontoSolicitado()), cuerpo.getDiasMora(), cuerpo.getAprobadaPor()`; `boolean quedaPendiente = calculo.exigeAprobacionManual() && entrada.aprobadaPor() == null;` Ninguna línea de 71-125 comprueba el estado del expediente (firme) ni el vencimiento del plazo de descargo.
- Cómo se comprobó: lectura del controlador y del caso de uso.
- Impacto: cualquier UUID en `aprobadaPor` salta el umbral de aprobación manual; `diasMora` lo afirma quien pide la cobertura; se cubre con el fondo de los participantes un expediente recién notificado. Viola 90.1.7, 91.4.1 y RN-18 (el organizador dispone del fondo).
- Causa de fondo: datos que deciden dinero aceptados del cliente.
- Solución de fondo: leer mora y deuda de aportes; exigir expediente firme en la escritura; decidir con Pablo si la cobertura con fondo mutual existe con R-NUEVA-2 (A-3).
- Relación con el flujo nuevo: APROB / RESPALDO (fondo mutual, A-3)
- Conexiones y módulos afectados: aportes (mora), grupos.
- Estándares y normas: ISO/IEC 25010 seguridad (integridad); CWE-807; reglas 90.1.7, 91.4, 91.5.
- Esfuerzo estimado: medio

### garantia-entregas-SEG-004 — La devolución del fondo se decide con `grupoCerrado` y `deudasVivas` del cuerpo
- Clase / Severidad / Confianza: SEG (y DINERO) / S1 / confirmado
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/web/GarantiaController.java:63-69`; `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU29DevolverFondo.java:60-70,150`
- Evidencia: `new CU29DevolverFondo.EntradaDevolucion(grupoId, Boolean.TRUE.equals(cuerpo.getGrupoCerrado()), cuerpo.getDeudasVivas())`; `if (!entrada.grupoCerrado())` / `if (entrada.deudasVivas() > 0)`.
- Cómo se comprobó: lectura.
- Impacto: combinado con SEG-002, un organizador reparte el fondo de garantía de un grupo en curso mandando `grupoCerrado=true, deudasVivas=0`.
- Causa de fondo: precondiciones de negocio afirmadas por el cliente en lugar de leídas de su dueño.
- Solución de fondo: preguntar a grupos el estado del grupo y contar deudas vivas en el propio esquema, dentro de la transacción.
- Relación con el flujo nuevo: APROB (paso manual del organizador)
- Conexiones y módulos afectados: grupos.
- Estándares y normas: ISO/IEC 25010 seguridad; CWE-807; regla 90.1.7, 96.3.2.
- Esfuerzo estimado: chico

### garantia-entregas-SEG-005 — La acreditación del desembolso la tipea una persona: `exitoso=true` marca ACREDITADA y manda el asiento al mayor
- Clase / Severidad / Confianza: SEG (y DINERO, APROB) / S1 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/web/DesembolsosController.java:105-106`; `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java:150-196`; consumidor `ConsumidorDeAsientosKafka.java` (tópico `aportaya.entregas.desembolso_acreditado`)
- Evidencia: `@Permiso("ENTREGA_EJECUTAR") … anotarRespuestaDesembolso`; `if (entrada.exitoso()) { … desembolsos.acreditar(dsl, orden.id(), entrada.referenciaProveedor(), ahora); … "asiento", new AsientoDeEntrega(orden.entregaId(), orden.monto()).comoCarga()`. No hay firma del proveedor ni adaptador al proveedor en el servicio.
- Cómo se comprobó: lectura; búsqueda del consumidor del tópico.
- Impacto: una persona de Tesorería puede declarar pagada una transferencia que no salió; el mayor registra la salida del pozo. El comentario del consumidor dice «El desembolso que el proveedor CONFIRMO», que no es lo que pasa.
- Causa de fondo: falta la integración con el proveedor de desembolso; se reemplazó por un formulario humano.
- Solución de fondo: webhook del proveedor con firma verificada e idempotente por `referenciaProveedor`; retirar el endpoint humano o dejarlo solo para conciliación con doble control y sin asiento.
- Relación con el flujo nuevo: APROB / FLUJO-QR (simétrico del lado de salida)
- Conexiones y módulos afectados: nucleo-financiero (mayor).
- Estándares y normas: ISO/IEC 25010 seguridad (integridad, no repudio); CWE-345; reglas 91.3, 91.1.4.
- Esfuerzo estimado: grande

### garantia-entregas-SEG-006 — El desembolso no comprueba que la cuenta destino sea del beneficiario
- Clase / Severidad / Confianza: SEG (y DINERO) / S1 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java:91-118`
- Evidencia: se valida `cuenta.estaVerificada()` y `bloqueadaHasta`, y se emite `desembolsos.emitir(dsl, entrada.entregaId(), entrada.proveedorId(), entrada.cuentaDestinoId(), entrega.neto(), …)`; no hay comparación entre el titular de la cuenta y el beneficiario de la entrega (ni con el titular de una cesión), ni de moneda.
- Cómo se comprobó: lectura del bloque completo.
- Impacto: Tesorería puede dirigir el pozo de un turno a cualquier cuenta verificada de cualquier usuario. La RLS no ayuda porque Tesorería es rol privilegiado.
- Causa de fondo: la entrega guarda `beneficiario_participante_id` y la cuenta `usuario_id`, sin vínculo verificable en este servicio.
- Solución de fondo: resolver el usuario del beneficiario (o del cesionario) y exigir coincidencia en la escritura y por trigger.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: grupos (participante→usuario), identidad.
- Estándares y normas: ISO/IEC 25010 seguridad; OWASP API1:2023; CWE-639; reglas 91.3.3, 90.1.3.
- Esfuerzo estimado: medio

### garantia-entregas-SEG-007 — El cliente puede enviar su propio `numeroCifrado`, distinto del número que se enmascara y deduplica
- Clase / Severidad / Confianza: SEG / S1 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU18RegistrarCuentaDestino.java:72,101-108`
- Evidencia: `if (entrada.numeroCifrado() != null && … (entrada.numeroCifrado().startsWith("cifrado:") || entrada.numeroCifrado().startsWith(CifradorSimetrico.PREFIJO))) { numeroCifradoReal = entrada.numeroCifrado(); } else { numeroCifradoReal = CifradorSimetrico.cifrar(entrada.numeroEnClaro(), pimienta); }` — y el hash/máscara salen de `numeroEnClaro` (`:72`).
- Cómo se comprobó: lectura.
- Impacto: la cuenta que se muestra y se verifica no es necesariamente la que se paga (el cifrado guardado puede ser de otra cuenta). Además la misma `pimienta` se usa como clave de cifrado y como pepper de hash (reutilización de clave).
- Causa de fondo: atajo para pruebas («cifrado:») que quedó en el código de producción.
- Solución de fondo: cifrar siempre en servidor con una clave dedicada y rotada; rechazar `numeroCifrado` del cliente.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: plataforma (`CifradorSimetrico`).
- Estándares y normas: ISO/IEC 25010 seguridad; CWE-807, CWE-323; reglas 90.2.9, 91.3.3.
- Esfuerzo estimado: chico

### garantia-entregas-SEG-008 — «Cuenta verificada» es autodeclarada por su dueño con un método de texto libre
- Clase / Severidad / Confianza: SEG / S2 / confirmado
- Ubicaciones: `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU18RegistrarCuentaDestino.java:147-176`
- Evidencia: `verificar(UUID cuentaId, String metodo, ContextoSesion ctx)` solo exige `cuenta.usuarioId().equals(ctx.usuarioId())` y llama `cuentas.verificar(dsl, cuentaId, metodo, ahora, …)`; no hay contraste con el banco.
- Cómo se comprobó: lectura.
- Impacto: la condición 91.3.3 («desembolso contra cuenta verificada») queda vacía; el enfriamiento sí corre.
- Causa de fondo: la verificación real (microdepósito/banco) no está implementada; se dejó un paso declarativo.
- Solución de fondo: verificación por proveedor (microdepósito o consulta de titularidad) con resultado firmado.
- Relación con el flujo nuevo: ninguna · Conexiones: identidad (titularidad) · Estándares y normas: ISO/IEC 25010 seguridad; regla 91.3.3 · Esfuerzo: grande

### garantia-entregas-SEG-009 — Las llamadas entre servicios propagan el token del usuario final (cobertura empresarial ejecutada con la identidad de Tesorería)
- Clase / Severidad / Confianza: SEG / S2 / probable
- Ubicaciones: `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/ClienteConCircuito.java:72,81`; `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/ClienteConCircuito.java:81` (clon, ver DUP-X-001)
- Evidencia: propagación del `Authorization` entrante; solo algunos adaptadores de entregas usan la credencial `SERVICIO_ENTREGAS`.
- Impacto: viola 98.6.3; el permiso que exige el servicio llamado es el del humano (`ENTREGA_EJECUTAR`), no uno de servicio con alcance; con cumplimiento, el ORGANIZADOR recibe 403 y el aval/restricción se frena.
- Solución de fondo: token de servicio con alcance acotado por operación. · Conexiones: aportes, cumplimiento, garantía · Estándares y normas: OWASP API2:2023; regla 98.6 · Esfuerzo: medio
- Relación con el flujo nuevo: RESPALDO (la cobertura se pide con identidad de Tesorería)
- Causa de fondo: No hay identidad de servicio emitida por plataforma para todos los adaptadores.
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.

### garantia-entregas-SEG-010 — `usuarioId` de la restricción tomado de la query de la ruta móvil
- Clase / Severidad / Confianza: SEG / S4 / probable
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart:64-72`; control en servidor `servicios/garantia/.../web/CobranzaController.java:47-50`
- Evidencia: `/pasanaku/mi-estado?usuario=…&participante=…`; el servidor exige propio usuario o `GRUPO_ADMINISTRAR`, así que no hay BOLA.
- Impacto: bajo (servidor protege); identificadores en URL innecesarios.
- Solución de fondo: resolver el usuario por sesión. · Estándares y normas: OWASP API1 (mitigado), 90.2.2 · Esfuerzo: chico
- Relación con el flujo nuevo: ninguna
- Causa de fondo: La ruta se armó pasando identificadores en la query.
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: apps/movil, garantía

### garantia-entregas-TRAZA-001 — Sin bitácora de lectura de datos financieros en garantía y entregas
- Clase / Severidad / Confianza: TRAZA / S2 / probable
- Ubicaciones: `servicios/garantia/src/main/java`, `servicios/entregas/src/main/java` (sin referencias a `registro_acceso_datos` ni equivalente); endpoints de lectura `GET /cobranza/restricciones/vigentes/{usuarioId}`, `GET /desembolsos`, `GET /cuentas-bancarias/{id}/disponibilidad`
- Evidencia: `grep -rlnE 'registro_acceso|RegistroDeLectura|AccesoADatos'` sobre ambos servicios → vacío; la tabla `registro_acceso_datos` existe y la usa `servicios/auditoria`.
- Comprobación pendiente: confirmar que ningún filtro transversal de plataforma registra estas lecturas (no se encontró por búsqueda).
- Impacto: la regla 90.2.7 exige rastro auditable de toda lectura de datos financieros de una persona (deuda, restricción, cuentas de cobro, desembolsos).
- Solución de fondo: registro de lectura transversal (plataforma) por endpoint marcado como sensible. · Estándares y normas: regla 90.2.7; OWASP ASVS (registro de seguridad) · Esfuerzo: medio
- Relación con el flujo nuevo: ninguna
- Causa de fondo: La bitácora de lectura existe en auditoría pero no hay mecanismo transversal que la alimente.
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: garantía, entregas, auditoría, plataforma

### garantia-entregas-REG-001 — Segregación de entrega (R-SEG-04), verificación de cuenta y KYC del beneficiario: posibles obligaciones sin fuente normativa en el repo
- Clase / Severidad / Confianza: REG / S3 / sospecha
- Ubicaciones: `sql/40_reglas/restricciones.sql` (`ck_entrega_segregacion`, «R-SEG-04 · cuatro ojos donde importa»); `servicios/entregas/src/main/resources/openapi/entregas.yaml:211`; SEG-008; dossier `docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-F/H15-dossier-habilitacion-real.md` (habilitación de dinero real condicionada, según el metaprompt)
- Evidencia: el código y la base imponen cuatro ojos en la entrega; no se encontró en `servicios/garantia`/`servicios/entregas` cita a una norma ASFI/UIF que lo exija.
- Impacto: quitar la aprobación (R-NUEVA-1) puede eliminar un control que Cumplimiento considere obligatorio; dejarla contradice R-NUEVA-1. Norma a identificar por Cumplimiento.
- Solución de fondo: que Cumplimiento identifique la norma y se registre en `docs/Cumplimiento.md`; decidir A-1. · Esfuerzo: chico (documental)
- Relación con el flujo nuevo: APROB
- Causa de fondo: Controles sin trazabilidad normativa registrada.
- Estándares y normas: norma a identificar por Cumplimiento; `docs/Cumplimiento.md`
- Cómo se comprobó: lectura del subagente de su lote; el agente principal no reabrió cada línea (de ahí la confianza declarada). Lo que falta para confirmarlo es reabrir las líneas citadas o ejecutar el caso.
- Conexiones y módulos afectados: entregas, cumplimiento

## 6. Matriz de contratos

Leyenda de columnas: **OpenAPI** (operationId en `servicios/<svc>/src/main/resources/openapi/<svc>.yaml`) ·
**Servicio** (controlador) · **Pacto** (`contratos/pactos/`) · **Cliente** (los clientes Angular/Dart se generan en build
por `buildSrc/.../aportaya.openapi.gradle.kts:73-74` y no están versionados; hay barrels Dart versionados en
`clientes/dart/{garantia,entregas}/lib/*.dart` que coinciden con los esquemas actuales) · **Front** · **Esquema** · **Discrepancias**.
Las 41 operaciones tienen mock en `packages/simulado` y entrada en Postman (colección y humo).
`python3 scripts/inventario_endpoints.py --check` → «endpoints relevados: 350 · hallazgos: 0 — todo lo implementado tiene
contrato y viceversa» (implementación ↔ OpenAPI alineadas a nivel de ruta).

**Garantía (20 operaciones)**

| Operación | Servicio · permiso | Pacto | Front | Esquema | Discrepancias / hallazgo |
|---|---|---|---|---|---|
| `consultarRestriccion` GET `/cobranza/restricciones/vigentes/{usuarioId}` | `CobranzaController` · PARTICIPANTE + propio o GRUPO_ADMINISTRAR | no | móvil `estado_de_mora.dart` → `pantalla_mi_estado.dart` | `lista_restriccion_interna` | importe sin moneda (DINERO-010); 401/403 no declarados; descripción «nadie mas la puede leer» falsa; mock sin rechazo, `nivel:"nivel"` |
| `levantarRestriccion` POST `/cobranza/restricciones/{id}/levantamiento` | GRUPO_ADMINISTRAR | no | — | idem | APROB (AP-11); BOLA (SEG-002) |
| `declararIncumplimiento` POST `/incumplimientos` | GRUPO_ADMINISTRAR | no | — | `registro_incumplimiento` | `Idempotency-Key` ignorada (CONTRATO-001) |
| `presentarDescargo` · `resolverDescargo` | PARTICIPANTE+imputado · GRUPO_ADMINISTRAR | no | sin pantalla de descargo ni apelación | `descargo_participante` | RLS probable rota (DEFECTO-002); sin apelación (DEBIDO-PROCESO-002) |
| `cubrirIncumplimiento` POST `/incumplimientos/{id}/cobertura` | GRUPO_ADMINISTRAR | no | — | `cobertura_incumplimiento`, `fondo_garantia` | datos de decisión del cliente (SEG-003); 201/200 cruzados; evento sin consumidor |
| `ejecutarAval` · `restringirDeudor` · `proponerReemplazo` | GRUPO_ADMINISTRAR | no | — | `ejecucion_aval`, `subrogacion`, `reemplazo_participante` | estados de aval incoherentes (COHERENCIA-001); sin clave de idempotencia |
| `devolverFondo` · `aprobarReemplazo` · `ejecutarReemplazo` · `iniciarDisolucion` · `cerrarDisolucion` | GRUPO_ADMINISTRAR | no | — | `devolucion_fondo`, `disolucion_anticipada` | banderas del cliente (SEG-004); 500 en ejecutar reemplazo (DEFECTO-001); `Dinero` admite negativos (DINERO-007) |
| `reservarRespaldo` · `ampliarRespaldo` · `liberarRespaldo` · `reversarCoberturaRespaldo` · `recuperarRespaldo` | ADMIN_PLATAFORMA | no | — | tablas `*_respaldo` | RESPALDO-002, SEG-001; mocks con AP-CU23-01 para todo |
| `cubrirConRespaldo` POST `/garantia/respaldo/coberturas` | ENTREGA_EJECUTAR (token humano propagado) | **no** (llamada entregas→garantía) | — | `cobertura_respaldo` | RESPALDO-001; CONTRATO-002 |

**Entregas (21 operaciones)**

| Operación | Servicio · permiso | Pacto | Front | Esquema | Discrepancias / hallazgo |
|---|---|---|---|---|---|
| `registrarCuentaDestino` · `verificarCuentaDestino` · `consultarDisponibilidad` · `designarCuentaPrincipal` | BILLETERA_OPERAR | no | **sin pantalla** de cuenta de cobro | `cuenta_bancaria_beneficiario` | SEG-007, SEG-008; `numeroCifrado` sin límite (`verificar_contratos_limites.py`) |
| `liquidarEntrega` POST `/entregas` | ENTREGA_EJECUTAR | no | — | `entrega_fondo`, `deduccion_entrega` | DINERO-003 (`EFECTIVO_ORGANIZADOR` en `entregas.yaml:897` vs «sin EFECTIVO_ORGANIZADOR» en `:787`); `deducciones` sin `maxItems` |
| `autorizarEntrega` · `ejecutarEntrega` | ENTREGA_AUTORIZAR · ENTREGA_EJECUTAR | no | — | `entrega_fondo` | APROB-001, DINERO-001 |
| `fondearPozo` POST `/entregas/pozo-completo` | ENTREGA_EJECUTAR | no | — (solo catálogo `apps/backoffice/.../operaciones.json:9917-9921`) | `fondeo_entrega` | RESPALDO-001; respuesta trae `cubiertoEmpresa` |
| `listarDesembolsos` GET `/desembolsos` | ENTREGA_EJECUTAR o AUDITORIA_LEER | no | backoffice `cu28-desembolsos.ts` | `orden_desembolso` | campos/enums/cursor coinciden; mock usa AP-CU28-01 (el contrato declara AP-CU28-07) y su escenario vacío trae cursor |
| `emitirOrdenDesembolso` · `anotarRespuestaDesembolso` | ENTREGA_EJECUTAR | no | — | `orden_desembolso`, `intento_desembolso` | SEG-005, SEG-006, DINERO-002; `anotarRespuesta` responde 201 y el YAML declara 200 |
| `publicarOferta` · `listarOfertas` · `cancelarOferta` · `comprarOferta` | PARTICIPANTE | no | — | `oferta_turno`, `cesion_derecho` | REESCRITURA-001 (publicar siempre denegado); mocks con AP-CU18-01 |
| `consultarOfertaParaPago` GET `/entregas/mercado/ofertas/{id}` | SERVICIO_ENTREGAS | `nucleo-financiero-entregas.json` (consumidor nucleo-financiero) | — | `oferta_turno` | sin verificación del lado productor (CONTRATO-003) |
| `abrirSubastaDeCupo` · `listarSubastasDeCupo` · `consultarSubastaDeCupo` · `pujarEnSubasta` · `cancelarSubastaDeCupo` | PARTICIPANTE | no | móvil `subasta_del_cupo.dart`, `retirar_subasta.dart` | `subasta_cupo`, `puja_subasta` | **sin prueba web** las 5 (`matriz_de_pruebas.py --servicio entregas`); moneda BOB fija (DINERO-011); paginación ignorada (CONTRATO-010); `SubastaRepositorio.java:45`/`PujaRepositorio.java:34` `DSL.field(variable)` (aviso de `verificar_seguridad.py`, no verificado si la variable es de lista blanca) |

**Eventos**

| Evento | Publica | Consume | ¿Consumidor idempotente? | Pacto |
|---|---|---|---|---|
| `aportaya.nucleo_financiero.aporte_aplicado` | nucleo-financiero | garantía (`ConsumidorDeAportesAplicadosKafka`) | sí (`Consumidos`) | sí, verificado por productor (`CUPactosDelRecargoTest`) |
| `aportaya.nucleo_financiero.transaccion_reversada` | nucleo-financiero | garantía | dedupe por `eventoId`, no por transacción original (sospecha de doble reverso) | sí |
| `aportaya.garantia.respaldo_*` (4), `cobertura_empresarial_aplicada/reversada` | garantía (outbox) | nucleo-financiero `ConsumidorDeAsientosKafka` | no verificado (lote L1) | **no** |
| `aportaya.entregas.desembolso_acreditado` | entregas (outbox) | nucleo-financiero | no verificado (L1) | **no** |
| `garantia.incumplimiento_cubierto`, `aval_ejecutado`, `fondo_devuelto`, `cobertura_requiere_aprobacion`, eventos de expediente/reemplazo/disolución | garantía | **nadie** | — | — |
| `entregas.pozo_fondeado`, `pozo_con_faltante`, `entrega_*`, `subasta_*`, `oferta_*`, `cuenta_destino_*` | entregas | consumidores no relevados en este lote | — | no |
| Evento de cobro QR (`aporte_cobrado`) | aportes | **entregas no lo consume** (FLUJO-QR-001) | — | — |

## 7. Evidencia obligatoria del módulo (91.6, 98.8, 90.6)

| Evidencia | Estado | Detalle |
|---|---|---|
| Asientos por operación y su suma | NO COMPROBADO | Por lectura: cobertura DEBE POZO_GRUPO / HABER RESERVA_RESPALDO; reserva DEBE RESERVA / HABER CAJA_EMPRESA; recuperación DEBE CAJA_EMPRESA / HABER POZO; desembolso `AsientoDeEntrega` DEBE POZO_GRUPO / HABER DESEMBOLSO_CONFIRMADO. Cobertura mutual, aval y devolución: **sin asiento**. No se ejecutó nada contra una base. |
| Idempotencia ejercitada | NO COMPROBADO | Existen pruebas (fondeo con 8 hilos, orden con la misma clave, compra, puja); no corrieron (Gradle bloqueado). |
| Cuadre del mayor contra el total esperado | NO COMPROBADO | Hay verificación de cuadre de la reserva (`sql/50_verificacion/verificaciones.sql:118-134`), no del fondo. |
| Caso de reversa | NO COMPROBADO | `CU23ReversarCobertura` (contra-asiento) existe; reverso de recargo en `IngresoAlFondo`. |
| Redondeo declarado | NO COMPROBADO / defecto | Residuo de devolución del fondo sin asignar (DINERO-008). |
| Prueba de contrato | FAIL (por ausencia) | Sin pactos para entregas→garantía, garantía→aportes, entregas→aportes, garantía→cumplimiento (CONTRATO-002). |
| Prueba de duplicado de mensaje | NO COMPROBADO | Consumidores de garantía usan `Consumidos`; entregas no consume. |
| Servicio dependido apagado | NO COMPROBADO | Pruebas `RespaldoCaidaTest`, `RecaudoCaidaTest` existen (con dobles); no se apagó ningún contenedor. |
| Traza con `correlationId` | NO COMPROBADO | `Traza.marcarCasoDeUso` en controladores; propagación no ejercitada. |
| Matriz de autorización negativa | FAIL (por ausencia) | `SeguridadWebTest` solo 401/403 por rol; sin otro organizador / otro grupo / otro usuario (PRUEBAS-001). |

## 8. Cobertura por clase

| Clase | Qué se revisó y cómo | Hallazgos |
|---|---|---|
| APROB | Búsqueda de `aprobar|aprobaci|APROBAD|PENDIENTE_|comprobante|habilit|confirmar|autoriz` en huella, SQL y front; lectura de controladores y casos de uso; inventario §4.a | 4 |
| RESPALDO | Búsqueda de los nombres del metaprompt (`cubrirConRespaldo`, `CU23*`, `RespaldoEmpresarial`, `RespaldoPorHttp`, `*_respaldo*`, `politica_cobertura`, cuentas de reserva) y de textos «cubre/cubrimos/pozo completo»; seguimiento hasta el consumidor del asiento; §4.b | 4 |
| FLUJO-QR | Búsqueda de `@KafkaListener` en ambos servicios; lectura del fondeo y de los consumidores de garantía; contraste con el evento de cobro de aportes | 1 |
| FLUJO-FALTANTE | Lectura de `CU22EntregarPozoCompleto`/`RegistroDelFondeo`/`RecaudoDelPozo` (subagente) y reapertura de A2/A3; tabla §4.d | 1 |
| COHERENCIA | Lectura de ADR-049/051/054 (por rangos), catálogo de flujos, CU-22..26, contratos y DDL; `validar_flujos.py` | 1 |
| DUP-INT | Comparación de casos de uso hermanos (liquidar vs fondear, mercado vs subasta); los clones relevantes se reportan como REESCRITURA o DUP-X; ningún clon intra-servicio adicional confirmado | 0 |
| DUP-X | Comparación por nombre y tamaño de adaptadores y utilitarios entre garantía y entregas (`ClienteConCircuito`, `ConfiguracionDeCircuito`, `RecaudoPorHttp`, `MayorDeDoble`) | 1 |
| REESCRITURA | Búsqueda de dos caminos para la misma intención; referencias de rutas y adaptadores (`turno()` vacío) | 1 |
| PISADA | `git log` del módulo (historial completo); columnas de diseño viejo en DDL append-only | 1 |
| PARCHE | Lectura de `application.yml` de ambos servicios (valores «SINTETICOS», «SOLO TEST»), atajos `cifrado:` y monedas fijas | 1 |
| FRAGIL | Conteo de estados literales repetidos en Java/SQL/front | 1 |
| DEFECTO | Lectura de mapeos y de la política RLS aplicada a las tablas del módulo | 2 |
| HUECO | Búsqueda de consumidores de cada evento publicado por garantía | 1 |
| RESILIENCIA | Lectura de adaptadores HTTP, manejo de errores de consumidores, trabajos programados y sagas | 2 |
| DINERO | Las cinco prohibiciones de 91.1 y los invariantes de 91.2 sobre DDL, reglas, casos de uso y front; matriz por tabla | 13 |
| DEBIDO-PROCESO | Precondiciones de estado en aval, restricción, cobertura; semillas de `matriz_sancion`; existencia de apelación | 3 |
| DDD | Revisión de objetos de valor (`Dinero` existe y se usa), estados como `String` (reportado en FRAGIL-001); lenguaje: sin hallazgo propio adicional | 0 |
| ARQ | `ArquitecturaTest` (ArchUnit) presente en ambos servicios; tamaño de archivos: ningún `src/main/*.java` > 300 líneas (`wc -l`); controladores delgados; sin hallazgo propio (el acoplamiento por FK está en ACOPLE) | 0 |
| CLEAN | Búsqueda de referencias de métodos y textos sin uso; `TODO|FIXME` en `src/main` de ambos servicios: sin resultados en el muestreo del subagente | 1 |
| ACOPLE | Conteo de FK entre esquemas; búsqueda de `DSL.name("<otro esquema>"`; fixturas de prueba | 1 |
| CONTRATO | Matriz §6; `inventario_endpoints.py --check`; lectura de `contratos/pactos/`; mocks de `packages/simulado` contra OpenAPI (script del subagente) | 4 |
| DATOS | DDL de las 39 tablas, `20_claves`, `30_indices`, `35_append_only`, `40_reglas`, semillas; matriz por tabla | 3 |
| RENDIMIENTO | Paginación de listados (`listarDesembolsos`, subastas, ofertas paginados con orden estable); consultas en bucles no observadas en los casos de uso leídos; sin hallazgo propio (la paginación ignorada del front está en CONTRATO-010) | 0 |
| PRUEBAS | `matriz_de_pruebas.py`; inventario de `@DisplayName`; lectura completa de bases y dobles; mocks de simulado | 2 |
| UX-A11Y | Lectura de pantallas de subasta, mi estado, bandeja de desembolsos; búsqueda de colores literales (`Color(0x`, `Colors.`): sin resultados en el móvil revisado. Modo oscuro y movimiento reducido no ejercitados | 4 |
| OBSERV | Búsqueda de `BITACORA.info|log.` con importes o identificadores de persona | 1 |
| CONFIG | Lectura de `application.yml`; `verificar_seguridad.py` (sin secretos reportados en el módulo); lockfiles no revisados contra CVE; hallazgos de config reportados en PARCHE-001 | 0 |
| DOC | README de ambos servicios, comentarios de pruebas, ADR y catálogos (DOC) y textos de backoffice/catálogos de diseño | 3 |
| SEG | Guardia de permisos, RLS, controladores y casos de uso por endpoint (rol y propiedad), entrada del cliente en decisiones de dinero, cifrado de cuentas, token entre servicios | 10 |
| TRAZA | Búsqueda de registro de lecturas en ambos servicios y en plataforma | 1 |
| REG | Búsqueda de citas normativas para los controles del módulo; supeditado a Cumplimiento y al dossier H15 | 1 |
| OTRO | Ningún hallazgo fuera de las clases anteriores | 0 |

## 9. Lo que está bien

- Outbox transaccional en todas las publicaciones de ambos servicios (`Outbox.emitir` dentro de la transacción).
- Idempotencia con `UNIQUE` en `cobertura_respaldo`, `movimiento_reserva`, `reserva_respaldo`, `fondeo_entrega` y orden de desembolso, con pruebas de concurrencia (fondeo con 8 hilos).
- Enfriamiento de cuenta destino tras alta (`PT24H`) y trigger `tg_orden_desembolso_cuenta_verificada` en la base (`sql/40_reglas/restricciones.sql:2560-2580`).
- Dinero en `NUMERIC` en todo el esquema; `Dinero.monto` como cadena con dos decimales en los contratos; ni la app ni el backoffice usan `double`/`number` para importes (`packages/diseno_flutter/lib/atomos/campo_monto.dart`).
- Deducciones nunca superan la bolsa: `ck_entrega_neto_no_negativo` + `fn_grp_recalcular_deducciones` (`restricciones.sql:1837-1863`).
- Índices con nombre explícito (`uq_*`, `ix_*`) en todo `sql/30_indices/08_garantia_incumplimiento.sql`.
- El código de producción no lee esquemas ajenos: los cruces van por HTTP o Kafka (el acoplamiento está en las FK, ACOPLE-001).
- Libros append-only con `REVOKE UPDATE, DELETE` y trigger en `movimiento_fondo`, `movimiento_reserva`, `recuperacion_respaldo`, `abono_recuperacion`, `historial_estado_incumplimiento`, `registro_incumplimiento`.
- Circuit breaker en todos los adaptadores HTTP; `EfectoDeReclamosPorHttp` falla cerrado.
- `presentarDescargo` y `consultarRestriccion` sí verifican propiedad del recurso.
- Formularios de subasta/puja: preservan lo escrito y bloquean doble envío.
- Línea base: `GET /desembolsos` figuraba `SIN_ANOTACION` en `docs/auditoria-produccion/endpoints.md:73`; hoy tiene `@Permiso("ENTREGA_EJECUTAR")` + `@PermisoAlternativo({"AUDITORIA_LEER"})` (`DesembolsosController.java:45-46`): corregido.
- Ningún archivo Java de `src/main` supera 300 líneas (el mayor, `CU23CubrirConRespaldo.java`, 288).

## 10. Observado fuera de alcance

- `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/consumo/ConsumidorDeAsientosKafka.java` — registra asientos de respaldo empresarial; su comentario dice «El desembolso que el proveedor CONFIRMO» cuando lo confirma un humano (L1).
- `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml:87` — recargo de mora asentado contra «reserva de respaldo» mientras `:9` dice que va al fondo de garantía (L1, verificar en nucleo-financiero).
- `scripts/verificar_rutas_frontend.py` — FALLA: «la lista de conocidas se pudrió: estas rutas YA tienen contrato: GET /grupos/solicitudes-escaladas» (L3/L6).
- `scripts/auditar_backend.py` — 48 operaciones declaradas sin implementar en cumplimiento y grupos (L3/L4).
- `apps/backoffice/src/app/rutas/operacion/retiros/` — pantalla de aprobación de retiros (ADR-049, A-1; L1/L4).
- `apps/movil/lib/pantallas/identidad/textos_de_verificacion.dart:76` — «Te avisamos apenas la aprueben» (KYC, A-1; L4).
- `apps/movil/lib/pantallas/pasanaku/dominio/enlace_del_grupo.dart:12,140` — «el organizador aprueba cada pedido» (L3).
- `sql/40_reglas/restricciones.sql:1955-1970` — `fn_grp_validar_disolucion` lee `cuenta_billetera` sin esquema calificado (sospecha; L3/L5).
- `.gitignore:51` ignora `clientes/dart/` pero hay barrels versionados en `clientes/dart/{garantia,entregas}` (L6).
- Candado `blocker_gate.py` (regla 65) bloquea el cierre de cualquier sesión por microtareas `BLOQUEADO`/`EN CURSO` de planes de octubre ajenos a esta auditoría (`docs/trabajo/2026-10-03-alta-fecha-actividad-autocaptura`, `2026-10-14-resolver-conflictos-prs-60-63`, `2026-10-16-ci-formato-test`, `2026-10-27-rediseno-backoffice-al-repo-desplegado`, `2026-10-28-admision-directa-backoffice`, `2026-10-30-arreglos-test-y-espejo`).

## 11. Límites de esta auditoría

- **Nada se ejecutó contra el servicio ni la base.** `./gradlew :servicios:garantia:test` falló por entorno: Maven Central respondió `429 Too Many Requests` al resolver plugins de `buildSrc` (`evidencia/05c-gradle-garantia-test.txt`, `exit=1`). Por la regla del metaprompt no se reintentó ni se arregló el entorno. Por eso no corrieron: suites de garantía y entregas, `webTest`, `integrationTest`, barridos Gradle de tamaño, umbrales y secretos, `scripts/cobertura.py` (pide informes de Jacoco). `yarn lint`/`yarn typecheck` y `flutter analyze` no se intentaron (prioridad al backend; supuesto: el lote L6 los cubre).
- **Verificadores que sí corrieron** (salida en `evidencia/05-verificadores.txt` y filtrado en `05b`): `verificar_contratos_limites`, `validar_flujos`, `verificar_criterios`, `verificar_seguridad`, `idempotencia` (no aplica al módulo: es librería), `verificar_boveda`, `verificar_carriles`, `verificar_frontend`, `verificar_pruebas_web`, `auditar_backend`, `listar_topicos`, `inventario_endpoints --check`, `matriz_de_pruebas --servicio {garantia,entregas}` (todos `exit=0` salvo `verificar_rutas_frontend` `exit=1`, fuera de alcance). `inventario_endpoints.py` y `matriz_de_pruebas.py` escriben archivos sin flag: se corrieron solo en modo `--check`/`--servicio`, que no escriben (`git status` limpio salvo esta carpeta).
- **Lectura**: el código `src/main` de ambos servicios lo leyeron completo los subagentes; el agente principal reabrió todas las ubicaciones de los S1 y la mayoría de los S2 (los marcados «probable» o «sospecha» son los no reabiertos o no demostrables sin ejecutar). Unas 83 clases de prueba (~14 k líneas) se relevaron por `@DisplayName`, no línea a línea.
- **Números de línea**: los subagentes reportaron algunas líneas desplazadas (p. ej. `RespaldoWeb.java:385` en un archivo de 174 líneas). Toda línea citada en S1/S2 confirmados fue reubicada por el agente principal; en los S3/S4 «probable» puede quedar algún desplazamiento: la búsqueda por el fragmento citado los ubica.
- **Fuente del esquema**: los hallazgos sobre `sql/` se corrigen en `docs/entidades/*.puml` y `docs/Restricciones.md`, que no se leyeron (el SQL generado es fiel a ellos según `sql/README.md`).
- **No se leyó** `docs/entidades/*.puml`, los ADR completos (ADR-054 y ADR-049 por rangos), CU-24/CU-29 en profundidad, `sql/62_volumen/volumen-02..12`, ni el interior de nucleo-financiero, aportes, grupos o cumplimiento más allá del punto de contacto.
- **Supuestos**: (1) el «fondo de garantía» mutual no es «la empresa» para R-NUEVA-2 hasta que Pablo diga otra cosa (A-3); (2) la pantalla de traspaso por votación pertenece a grupos (L3) y acá solo se registra; (3) los textos de la web pública cuentan como pantallas que consumen el módulo; (4) severidad de textos públicos que prometen cobertura: S2 (no mueven dinero, pero son una promesa al consumidor).
- **Candado de la regla 65**: el hook de cierre bloquea por planes ajenos (ver §10). No se tocaron esos planes (solo lectura, fuera de alcance).
- **Procesos en background**: los cuatro subagentes terminaron; el daemon de Gradle se detuvo con `./gradlew --stop`. No queda nada corriendo.

## 12. Anexo de cobertura de archivos

### 12.a Huella núcleo (generado con `find … -exec wc -l`)

| Archivo | Líneas | Leído completo | Qué hace |
|---|---|---|---|
| `servicios/entregas/.jqwik-database` | 0 | sí (vacío) | Marcador vacío |
| `servicios/entregas/README.md` | 99 | sí (subagente; S1/S2 reabiertos por el agente principal) | Documenta CU-18/22/28, pozo completo con respaldo, mercado |
| `servicios/entregas/build.gradle.kts` | 45 | sí (subagente; S1/S2 reabiertos por el agente principal) | Plugins y dependencias (resilience4j, shedlock, kafka) |
| `servicios/entregas/descriptor.yml` | 30 | sí (subagente; S1/S2 reabiertos por el agente principal) | Nivel, réplicas y sondas |
| `servicios/entregas/gradle.lockfile` | 206 | no — lockfile (se inventaría) | Lock de dependencias |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/Aplicacion.java` | 25 | sí (subagente; S1/S2 reabiertos por el agente principal) | Arranque Spring |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU18RegistrarCuentaDestino.java` | 232 | sí (subagente; S1/S2 reabiertos por el agente principal) | Alta, verificación, disponibilidad y principal de la cuenta destino |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22EntregarPozoCompleto.java` | 105 | sí (subagente; S1/S2 reabiertos por el agente principal) | Orquesta aportes → garantía (respaldo) → registro del fondeo |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU22LiquidarEntrega.java` | 212 | sí (subagente; S1/S2 reabiertos por el agente principal) | Liquidación legacy, autorizar y ejecutar a mano |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/CU28EmitirDesembolso.java` | 277 | sí (subagente; S1/S2 reabiertos por el agente principal) | Orden de desembolso y respuesta del proveedor anotada a mano |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/ComprarOfertaDeTurno.java` | 169 | sí (subagente; S1/S2 reabiertos por el agente principal) | Saga de compra del derecho de cobro |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/ConsultarDesembolsos.java` | 93 | sí (subagente; S1/S2 reabiertos por el agente principal) | Bandeja de Tesorería |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/ConsultasDeSubasta.java` | 118 | sí (subagente; S1/S2 reabiertos por el agente principal) | Vistas de subasta |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/EventosDeSubasta.java` | 29 | sí (subagente; S1/S2 reabiertos por el agente principal) | Helper del outbox de subasta |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/GestionDeOfertas.java` | 95 | sí (subagente; S1/S2 reabiertos por el agente principal) | Publicar, listar y cancelar ofertas |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/IndicadoresDeEntregas.java` | 46 | sí (subagente; S1/S2 reabiertos por el agente principal) | KPIs |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/RegistroDeOfertas.java` | 179 | sí (subagente; S1/S2 reabiertos por el agente principal) | Escrituras de ofertas |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/RegistroDelFondeo.java` | 193 | sí (subagente; S1/S2 reabiertos por el agente principal) | Transacción local del fondeo, bloqueo e incidencia |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/SubastaDelCupo.java` | 246 | sí (subagente; S1/S2 reabiertos por el agente principal) | Saga de subasta |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/TransicionesDeCesion.java` | 253 | sí (subagente; S1/S2 reabiertos por el agente principal) | Pasos locales de la cesión |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/TransicionesDeSubasta.java` | 263 | sí (subagente; S1/S2 reabiertos por el agente principal) | Pasos locales de la subasta |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/aplicacion/TransicionesDelCierre.java` | 150 | sí (subagente; S1/S2 reabiertos por el agente principal) | Cierre/adjudicación de subasta |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/AsientoDeEntrega.java` | 63 | sí (subagente; S1/S2 reabiertos por el agente principal) | Asiento propuesto POZO_GRUPO / DESEMBOLSO_CONFIRMADO |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/CuentaEnmascarada.java` | 57 | sí (subagente; S1/S2 reabiertos por el agente principal) | Hash con pimienta y máscara del número |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/FondeoDelPozo.java` | 41 | sí (subagente; S1/S2 reabiertos por el agente principal) | Cálculo confirmado + mutual + empresa + pendiente |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/LiquidacionDeEntrega.java` | 42 | sí (subagente; S1/S2 reabiertos por el agente principal) | Neto = bruto − deducciones |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/OfertaParaElPago.java` | 14 | sí (subagente; S1/S2 reabiertos por el agente principal) | DTO de dominio |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/OfertaVisible.java` | 15 | sí (subagente; S1/S2 reabiertos por el agente principal) | DTO de dominio |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/ReglasDeOferta.java` | 83 | sí (subagente; S1/S2 reabiertos por el agente principal) | Validación de la oferta |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/ReintentoDeDesembolso.java` | 54 | sí (subagente; S1/S2 reabiertos por el agente principal) | Backoff y errores definitivos |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/TitularidadDeCuenta.java` | 43 | sí (subagente; S1/S2 reabiertos por el agente principal) | Compara nombre y documento declarados |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/VentanaDeEnfriamiento.java` | 31 | sí (subagente; S1/S2 reabiertos por el agente principal) | Fin del enfriamiento de la cuenta |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/puertos/FondosDelComprador.java` | 45 | sí (subagente; S1/S2 reabiertos por el agente principal) | Puerto hacia nucleo-financiero |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/puertos/GruposParaSubasta.java` | 44 | sí (subagente; S1/S2 reabiertos por el agente principal) | Puerto hacia grupos (subasta) |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/puertos/HechosDeGrupos.java` | 44 | sí (subagente; S1/S2 reabiertos por el agente principal) | Puerto hacia grupos (mercado) |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/puertos/RecaudoDelPozo.java` | 31 | sí (subagente; S1/S2 reabiertos por el agente principal) | Puerto hacia aportes |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/puertos/RespaldoEmpresarial.java` | 26 | sí (subagente; S1/S2 reabiertos por el agente principal) | Cálculo de disponible/exposición/recuperación del respaldo (garantía) o puerto hacia garantía (entregas) |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/dominio/puertos/TitularDeLaBilletera.java` | 19 | sí (subagente; S1/S2 reabiertos por el agente principal) | Puerto hacia identidad |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/BandejaDeDesembolsosRepositorio.java` | 79 | sí (subagente; S1/S2 reabiertos por el agente principal) | SQL de la bandeja |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/CesionRepositorio.java` | 180 | sí (subagente; S1/S2 reabiertos por el agente principal) | cesion_derecho |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/ClienteConCircuito.java` | 128 | sí (subagente; S1/S2 reabiertos por el agente principal) | RestClient con circuit breaker y token propagado (clon en ambos servicios) |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/ConfiguracionDeCircuito.java` | 28 | sí (subagente; S1/S2 reabiertos por el agente principal) | Parámetros sintéticos del circuito (clon) |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/ConfiguracionDeCredencialDeServicio.java` | 31 | sí (subagente; S1/S2 reabiertos por el agente principal) | Credencial SERVICIO_ENTREGAS |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/CuentaDestinoRepositorio.java` | 161 | sí (subagente; S1/S2 reabiertos por el agente principal) | cuenta_bancaria_beneficiario |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/DesembolsoRepositorio.java` | 198 | sí (subagente; S1/S2 reabiertos por el agente principal) | Orden e intentos de desembolso |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/EntregaRepositorio.java` | 224 | sí (subagente; S1/S2 reabiertos por el agente principal) | entrega_fondo, deducciones y validaciones |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/FondeoRepositorio.java` | 201 | sí (subagente; S1/S2 reabiertos por el agente principal) | fondeo_entrega (UPDATE), incidencia y advisory lock |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/FondosPorHttp.java` | 253 | sí (subagente; S1/S2 reabiertos por el agente principal) | Cliente de nucleo-financiero |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/GruposParaSubastaPorHttp.java` | 176 | sí (subagente; S1/S2 reabiertos por el agente principal) | Cliente de grupos (subasta) |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/HechosDeGruposPorHttp.java` | 80 | sí (subagente; S1/S2 reabiertos por el agente principal) | Cliente de grupos; turno() siempre vacío |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/IndicadoresDeEntregasRepositorio.java` | 50 | sí (subagente; S1/S2 reabiertos por el agente principal) | SQL de KPIs |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/OfertaRepositorio.java` | 206 | sí (subagente; S1/S2 reabiertos por el agente principal) | oferta_turno |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/PujaRepositorio.java` | 150 | sí (subagente; S1/S2 reabiertos por el agente principal) | puja_subasta |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/RecaudoPorHttp.java` | 76 | sí (subagente; S1/S2 reabiertos por el agente principal) | GET a aportes del recaudo del periodo |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/RespaldoPorHttp.java` | 87 | sí (subagente; S1/S2 reabiertos por el agente principal) | POST a /garantia/respaldo/coberturas |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/SubastaRepositorio.java` | 244 | sí (subagente; S1/S2 reabiertos por el agente principal) | subasta_cupo |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/TitularPorHttp.java` | 80 | sí (subagente; S1/S2 reabiertos por el agente principal) | Titularidad contra identidad |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/trabajos/.gitkeep` | 0 | sí (vacío) | Marcador vacío |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/trabajos/CierreDeSubastas.java` | 45 | sí (subagente; S1/S2 reabiertos por el agente principal) | @Scheduled sin ShedLock |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/web/.gitkeep` | 0 | sí (vacío) | Marcador vacío |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/web/CuentasBancariasController.java` | 126 | sí (subagente; S1/S2 reabiertos por el agente principal) | /cuentas-bancarias |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/web/DesembolsosController.java` | 128 | sí (subagente; S1/S2 reabiertos por el agente principal) | /desembolsos |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/web/EntregasController.java` | 228 | sí (subagente; S1/S2 reabiertos por el agente principal) | /entregas, mercado y subastas |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/web/MapeoDeEntregas.java` | 25 | sí (subagente; S1/S2 reabiertos por el agente principal) | Mapeo de Dinero |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/web/MercadoWeb.java` | 122 | sí (subagente; S1/S2 reabiertos por el agente principal) | Traducción web del mercado |
| `servicios/entregas/src/main/java/bo/aportaya/entregas/web/SubastasWeb.java` | 138 | sí (subagente; S1/S2 reabiertos por el agente principal) | Traducción web de subastas |
| `servicios/entregas/src/main/resources/application.yml` | 91 | sí (subagente; S1/S2 reabiertos por el agente principal) | Configuración del servicio (URLs, circuito, plazos; valores sintéticos) |
| `servicios/entregas/src/main/resources/openapi/entregas.yaml` | 1035 | sí (subagente; S1/S2 reabiertos por el agente principal) | Contrato OpenAPI de entregas |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/ArquitecturaTest.java` | 60 | sí | Prueba/fixture: ArquitecturaTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/ArranqueTest.java` | 65 | sí | Prueba/fixture: ArranqueTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/BarridoTest.java` | 28 | sí | Prueba/fixture: BarridoTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/BaseDeCompraDeTurno.java` | 42 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: BaseDeCompraDeTurno |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/BaseDeEntregas.java` | 188 | sí | Prueba/fixture: BaseDeEntregas |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/BaseDeMercado.java` | 83 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: BaseDeMercado |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/BaseDePozoCompleto.java` | 73 | sí | Prueba/fixture: BaseDePozoCompleto |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/BaseDeSubasta.java` | 140 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: BaseDeSubasta |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU130CarrerasTest.java` | 199 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU130CarrerasTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU130CompraBordesTest.java` | 158 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU130CompraBordesTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU130CompraTest.java` | 203 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU130CompraTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU130LiquidacionCarrerasTest.java` | 211 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU130LiquidacionCarrerasTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU130LiquidacionTrasReinicioTest.java` | 123 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU130LiquidacionTrasReinicioTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU130OfertaParaPagoTest.java` | 66 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU130OfertaParaPagoTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU130OfertasTest.java` | 298 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU130OfertasTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU133EntregaConsistenteTest.java` | 156 | sí | Prueba/fixture: CU133EntregaConsistenteTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU133PozoCompletoConcurrenciaTest.java` | 92 | sí | Prueba/fixture: CU133PozoCompletoConcurrenciaTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU133PozoCompletoRechazosTest.java` | 186 | sí | Prueba/fixture: CU133PozoCompletoRechazosTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU133PozoCompletoTest.java` | 197 | sí | Prueba/fixture: CU133PozoCompletoTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU18RechazosTest.java` | 193 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU18RechazosTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU18Test.java` | 233 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU18Test |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU22PozoYMercadoRechazosTest.java` | 99 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU22PozoYMercadoRechazosTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU22RechazosTest.java` | 239 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU22RechazosTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU22Test.java` | 259 | parcial (DisplayName + rangos) — volumen | Prueba/fixture: CU22Test |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU28DesembolsosTest.java` | 132 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU28DesembolsosTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU28RechazosTest.java` | 271 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU28RechazosTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU28Test.java` | 218 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU28Test |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU64CierreDeSubastaTest.java` | 132 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU64CierreDeSubastaTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CU64SubastaDeCupoTest.java` | 290 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU64SubastaDeCupoTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/CUIndicadoresDeEntregasTest.java` | 116 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CUIndicadoresDeEntregasTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/ContratoDeFondosDelComprador.java` | 182 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: ContratoDeFondosDelComprador |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/DobleDeFondosCumpleElContratoTest.java` | 50 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: DobleDeFondosCumpleElContratoTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/DobleDeGruposParaSubasta.java` | 106 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: DobleDeGruposParaSubasta |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/DoblesDelMercado.java` | 223 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: DoblesDelMercado |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/DoblesDelPozo.java` | 112 | sí | Prueba/fixture: DoblesDelPozo |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/EscenarioDeDesembolso.java` | 97 | parcial (DisplayName + rangos) — volumen | Prueba/fixture: EscenarioDeDesembolso |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/FixturaDeEntregas.java` | 185 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: FixturaDeEntregas |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/MayorDeDoble.java` | 93 | parcial (DisplayName + rangos) — volumen | Prueba/fixture: MayorDeDoble |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/dominio/ReglasDeOfertaCierreTest.java` | 96 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: ReglasDeOfertaCierreTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/infraestructura/AdaptadoresDelPozoTest.java` | 210 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: AdaptadoresDelPozoTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/infraestructura/BaseDeFondosPorHttp.java` | 141 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: BaseDeFondosPorHttp |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/infraestructura/FondosPorHttpInvalidoTest.java` | 90 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: FondosPorHttpInvalidoTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/infraestructura/FondosPorHttpTest.java` | 141 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: FondosPorHttpTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/infraestructura/HechosDeGruposPorHttpTest.java` | 128 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: HechosDeGruposPorHttpTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/infraestructura/RecaudoCaidaTest.java` | 143 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: RecaudoCaidaTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/infraestructura/RespaldoCaidaTest.java` | 147 | parcial (DisplayName + rangos) — volumen | Prueba/fixture: RespaldoCaidaTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/infraestructura/TitularPorHttpTest.java` | 152 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: TitularPorHttpTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/web/CuentasBancariasControllerWebTest.java` | 237 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CuentasBancariasControllerWebTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/web/DesembolsosBandejaWebTest.java` | 143 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: DesembolsosBandejaWebTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/web/DesembolsosControllerWebTest.java` | 217 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: DesembolsosControllerWebTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/web/EntregasControllerWebTest.java` | 238 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: EntregasControllerWebTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/web/FondeoDelPozoWebTest.java` | 164 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: FondeoDelPozoWebTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/web/MercadoWebTest.java` | 183 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: MercadoWebTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/web/OfertaParaPagoWebTest.java` | 169 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: OfertaParaPagoWebTest |
| `servicios/entregas/src/test/java/bo/aportaya/entregas/web/SeguridadWebTest.java` | 50 | sí | Prueba/fixture: SeguridadWebTest |
| `servicios/entregas/src/test/resources/fixtures/.gitkeep` | 0 | sí (vacío) | Prueba/fixture: .gitkeep |
| `servicios/garantia/.jqwik-database` | 0 | sí (vacío) | Marcador vacío |
| `servicios/garantia/README.md` | 108 | sí (subagente; S1/S2 reabiertos por el agente principal) | Documentación del servicio (afirma que no hay consumidor del asiento: falso) |
| `servicios/garantia/build.gradle.kts` | 45 | sí (subagente; S1/S2 reabiertos por el agente principal) | Plugins, pisos de cobertura, dependencias |
| `servicios/garantia/descriptor.yml` | 30 | sí (subagente; S1/S2 reabiertos por el agente principal) | Nivel, réplicas y sondas del servicio |
| `servicios/garantia/gradle.lockfile` | 206 | no — lockfile (se inventaría) | Lock de dependencias |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/Aplicacion.java` | 21 | sí (subagente; S1/S2 reabiertos por el agente principal) | Arranque Spring |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirConRespaldo.java` | 288 | sí (subagente; S1/S2 reabiertos por el agente principal) | La empresa cubre el faltante del pozo con su reserva |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirFaltanteDelCorte.java` | 81 | sí (subagente; S1/S2 reabiertos por el agente principal) | Puerta HTTP: pide el recaudo a aportes y cubre con respaldo |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23CubrirIncumplimiento.java` | 232 | sí (subagente; S1/S2 reabiertos por el agente principal) | Cobertura del fondo mutual con aprobación manual por umbral |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23RecuperarRespaldo.java` | 150 | sí (subagente; S1/S2 reabiertos por el agente principal) | Pago tardío devuelve dinero del pozo a la caja de la empresa |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23ReservarRespaldo.java` | 286 | sí (subagente; S1/S2 reabiertos por el agente principal) | Reservar, ampliar y liberar capacidad empresarial |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU23ReversarCobertura.java` | 108 | sí (subagente; S1/S2 reabiertos por el agente principal) | Contra-asiento de una cobertura empresarial |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU25DeclararIncumplimiento.java` | 290 | sí (subagente; S1/S2 reabiertos por el agente principal) | Declarar incumplimiento, presentar y resolver descargo |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU26EjecutarAval.java` | 131 | sí (subagente; S1/S2 reabiertos por el agente principal) | Ejecutar el aval y subrogar la deuda |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU27RestringirDeudor.java` | 178 | sí (subagente; S1/S2 reabiertos por el agente principal) | Restringir y levantar restricción del deudor |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU29DevolverFondo.java` | 158 | sí (subagente; S1/S2 reabiertos por el agente principal) | Repartir el fondo al cerrar el grupo |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU66ReemplazarParticipante.java` | 167 | sí (subagente; S1/S2 reabiertos por el agente principal) | Proponer, aprobar y ejecutar reemplazo |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/CU67DisolverGrupo.java` | 160 | sí (subagente; S1/S2 reabiertos por el agente principal) | Iniciar y cerrar disolución anticipada |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/ConsultarRestriccion.java` | 45 | sí (subagente; S1/S2 reabiertos por el agente principal) | Consulta si un usuario está restringido y cuánto debe |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/ContingenciaDeCaja.java` | 94 | sí (subagente; S1/S2 reabiertos por el agente principal) | Plan de contingencia y escalamiento por respaldo insuficiente |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/IndicadoresDeGarantia.java` | 75 | sí (subagente; S1/S2 reabiertos por el agente principal) | Indicadores para tableros |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/IngresoAlFondo.java` | 152 | sí (subagente; S1/S2 reabiertos por el agente principal) | Anota en el fondo el recargo por mora y su reverso |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/ReclamosEnCurso.java` | 52 | sí (subagente; S1/S2 reabiertos por el agente principal) | Frena aval/restricción si hay reclamo en curso |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/aplicacion/UsuarioDelExpediente.java` | 31 | sí (subagente; S1/S2 reabiertos por el agente principal) | Dueño de un expediente leído fuera de la transacción |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/consumo/ConsumidorDeAportesAplicadosKafka.java` | 102 | sí (subagente; S1/S2 reabiertos por el agente principal) | Listener de aporte_aplicado |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/consumo/ConsumidorDeReversosKafka.java` | 80 | sí (subagente; S1/S2 reabiertos por el agente principal) | Listener de transaccion_reversada |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/AsientoPropuesto.java` | 92 | sí (subagente; S1/S2 reabiertos por el agente principal) | Partidas CAJA_EMPRESA / RESERVA_RESPALDO / POZO_GRUPO |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/CoberturaAplicable.java` | 109 | sí (subagente; S1/S2 reabiertos por el agente principal) | Topes y umbral de aprobación manual de la cobertura mutual |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/CuadreDeDisolucion.java` | 84 | sí (subagente; S1/S2 reabiertos por el agente principal) | Liquidación por posición en disolución |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/DevolucionDelFondo.java` | 74 | sí (subagente; S1/S2 reabiertos por el agente principal) | Reparto proporcional del fondo |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/EstadoDelExpediente.java` | 61 | sí (subagente; S1/S2 reabiertos por el agente principal) | Constantes de estado; admiteTransicion sin uso |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/PlanDeCaja.java` | 94 | sí (subagente; S1/S2 reabiertos por el agente principal) | Carga de la contingencia |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/PlazoDeDescargo.java` | 31 | sí (subagente; S1/S2 reabiertos por el agente principal) | Límite del plazo de descargo |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/RespaldoEmpresarial.java` | 63 | sí (subagente; S1/S2 reabiertos por el agente principal) | Cálculo de disponible/exposición/recuperación del respaldo (garantía) o puerto hacia garantía (entregas) |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/RestriccionInterna.java` | 53 | sí (subagente; S1/S2 reabiertos por el agente principal) | Tipos de restricción |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/TopeDelAval.java` | 46 | sí (subagente; S1/S2 reabiertos por el agente principal) | Tope ejecutable del aval |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/puertos/EfectoDeReclamos.java` | 32 | sí (subagente; S1/S2 reabiertos por el agente principal) | Puerto hacia cumplimiento |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/dominio/puertos/RecaudoDelPeriodo.java` | 34 | sí (subagente; S1/S2 reabiertos por el agente principal) | Puerto hacia aportes |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/ClienteConCircuito.java` | 128 | sí (subagente; S1/S2 reabiertos por el agente principal) | RestClient con circuit breaker y token propagado (clon en ambos servicios) |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/CoberturaRespaldoRepositorio.java` | 239 | sí (subagente; S1/S2 reabiertos por el agente principal) | Coberturas, líneas y recuperaciones de respaldo |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/ConfiguracionDeCircuito.java` | 28 | sí (subagente; S1/S2 reabiertos por el agente principal) | Parámetros sintéticos del circuito (clon) |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/DeudaRepositorio.java` | 87 | sí (subagente; S1/S2 reabiertos por el agente principal) | deuda_participante |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/DisolucionRepositorio.java` | 70 | sí (subagente; S1/S2 reabiertos por el agente principal) | disolucion_anticipada |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/EfectoDeReclamosPorHttp.java` | 82 | sí (subagente; S1/S2 reabiertos por el agente principal) | GET a cumplimiento, falla cerrado |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/ExpedienteRepositorio.java` | 290 | sí (subagente; S1/S2 reabiertos por el agente principal) | Expediente, historial, evidencia y descargo |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/FondoRepositorio.java` | 270 | sí (subagente; S1/S2 reabiertos por el agente principal) | Fondo, política, coberturas y movimientos (saldo cacheado) |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/GestionRepositorio.java` | 265 | sí (subagente; S1/S2 reabiertos por el agente principal) | Aval, subrogación, restricción, reemplazo |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/IndicadoresDeGarantiaRepositorio.java` | 57 | sí (subagente; S1/S2 reabiertos por el agente principal) | SUM ad-hoc para tableros |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/PlanDeContingenciaRepositorio.java` | 65 | sí (subagente; S1/S2 reabiertos por el agente principal) | plan_contingencia con SQL crudo |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/RecaudoPorHttp.java` | 88 | sí (subagente; S1/S2 reabiertos por el agente principal) | GET a aportes del recaudo del periodo |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/RespaldoRepositorio.java` | 179 | sí (subagente; S1/S2 reabiertos por el agente principal) | Capacidad, reserva y libro movimiento_reserva |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/trabajos/.gitkeep` | 0 | sí (vacío) | Marcador vacío |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/web/.gitkeep` | 0 | sí (vacío) | Marcador vacío |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/web/CobranzaController.java` | 75 | sí (subagente; S1/S2 reabiertos por el agente principal) | Endpoints /cobranza/* |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/web/GarantiaController.java` | 190 | sí (subagente; S1/S2 reabiertos por el agente principal) | Endpoints /garantia/* (incluye respaldo) |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/web/IncumplimientosController.java` | 230 | sí (subagente; S1/S2 reabiertos por el agente principal) | Endpoints /incumplimientos/* |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/web/MapeoDeGarantia.java` | 47 | sí (subagente; S1/S2 reabiertos por el agente principal) | Conversión de Dinero |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/web/MapeoDeReemplazo.java` | 26 | sí (subagente; S1/S2 reabiertos por el agente principal) | Salida del reemplazo |
| `servicios/garantia/src/main/java/bo/aportaya/garantia/web/RespaldoWeb.java` | 174 | sí (subagente; S1/S2 reabiertos por el agente principal) | Traducción web de /garantia/respaldo |
| `servicios/garantia/src/main/resources/application.yml` | 61 | sí (subagente; S1/S2 reabiertos por el agente principal) | Configuración del servicio (URLs, circuito, plazos; valores sintéticos) |
| `servicios/garantia/src/main/resources/openapi/garantia.yaml` | 997 | sí (subagente; S1/S2 reabiertos por el agente principal) | Contrato OpenAPI de garantía |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/ArquitecturaTest.java` | 60 | sí | Prueba/fixture: ArquitecturaTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/ArranqueTest.java` | 65 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: ArranqueTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/BarridoTest.java` | 28 | sí | Prueba/fixture: BarridoTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/BaseDeGarantia.java` | 148 | sí | Prueba/fixture: BaseDeGarantia |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/BaseDeRespaldo.java` | 79 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: BaseDeRespaldo |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU131CoberturaTest.java` | 292 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU131CoberturaTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU131ConcurrenciaTest.java` | 225 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU131ConcurrenciaTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU131FaltanteDelCorteTest.java` | 221 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU131FaltanteDelCorteTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU131RecuperacionTest.java` | 179 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU131RecuperacionTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU131RespaldoTest.java` | 248 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU131RespaldoTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU133ContingenciaTest.java` | 251 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU133ContingenciaTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU23ContingenciaRechazosTest.java` | 67 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU23ContingenciaRechazosTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU23RechazosTest.java` | 166 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU23RechazosTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU23RespaldoRechazosTest.java` | 116 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU23RespaldoRechazosTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU23Test.java` | 280 | parcial (DisplayName + rangos) — volumen | Prueba/fixture: CU23Test |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU25RechazosTest.java` | 204 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU25RechazosTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU25Test.java` | 255 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU25Test |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU26RechazosTest.java` | 190 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU26RechazosTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU26Test.java` | 271 | parcial (DisplayName + rangos) — volumen | Prueba/fixture: CU26Test |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU27RechazosTest.java` | 200 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU27RechazosTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU27ReclamoEnCursoTest.java` | 206 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU27ReclamoEnCursoTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU27Test.java` | 295 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU27Test |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU29RechazosTest.java` | 160 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU29RechazosTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU29Test.java` | 230 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU29Test |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU66RechazosTest.java` | 141 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU66RechazosTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU66Test.java` | 256 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU66Test |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU67RechazosTest.java` | 158 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU67RechazosTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CU67Test.java` | 247 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CU67Test |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CUIndicadoresDeGarantiaTest.java` | 140 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CUIndicadoresDeGarantiaTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/CUIngresoAlFondoTest.java` | 214 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CUIngresoAlFondoTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/FixturaDeGarantia.java` | 256 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: FixturaDeGarantia |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/FixturaDeRespaldo.java` | 155 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: FixturaDeRespaldo |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/MayorDeDoble.java` | 88 | sí | Prueba/fixture: MayorDeDoble |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/consumo/ConsumidorDeAportesAplicadosKafkaTest.java` | 95 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: ConsumidorDeAportesAplicadosKafkaTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/consumo/ConsumidorDeReversosKafkaTest.java` | 87 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: ConsumidorDeReversosKafkaTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/dominio/AsientoPropuestoTest.java` | 78 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: AsientoPropuestoTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/dominio/RespaldoEmpresarialTest.java` | 78 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: RespaldoEmpresarialTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/infraestructura/EfectoDeReclamosPorHttpTest.java` | 186 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: EfectoDeReclamosPorHttpTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/infraestructura/RecaudoCaidaTest.java` | 143 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: RecaudoCaidaTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/infraestructura/RecaudoPorHttpTest.java` | 133 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: RecaudoPorHttpTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/web/BaseDeRespaldoWeb.java` | 62 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: BaseDeRespaldoWeb |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/web/CoberturaWebTest.java` | 139 | sí | Prueba/fixture: CoberturaWebTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/web/CobranzaControllerWebTest.java` | 136 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: CobranzaControllerWebTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/web/GarantiaControllerWebTest.java` | 262 | parcial (DisplayName + rangos) — volumen | Prueba/fixture: GarantiaControllerWebTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/web/IncumplimientosControllerWebTest.java` | 284 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: IncumplimientosControllerWebTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/web/IncumplimientosReemplazoWebTest.java` | 108 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: IncumplimientosReemplazoWebTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/web/RespaldoAmpliacionWebTest.java` | 78 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: RespaldoAmpliacionWebTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/web/RespaldoCoberturaWebTest.java` | 203 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: RespaldoCoberturaWebTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/web/RespaldoControllerWebTest.java` | 176 | no — inventario de @DisplayName; motivo: volumen (≈5 k líneas de prueba por servicio) | Prueba/fixture: RespaldoControllerWebTest |
| `servicios/garantia/src/test/java/bo/aportaya/garantia/web/SeguridadWebTest.java` | 61 | parcial (DisplayName + rangos) — volumen | Prueba/fixture: SeguridadWebTest |
| `servicios/garantia/src/test/resources/fixtures/.gitkeep` | 0 | sí (vacío) | Prueba/fixture: .gitkeep |
| `sql/10_tablas/08_garantia_incumplimiento/abono_recuperacion.sql` | 35 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `abono_recuperacion` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/accion_cobranza.sql` | 32 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `accion_cobranza` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/acuerdo_quita.sql` | 25 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `acuerdo_quita` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/alerta_temprana.sql` | 26 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `alerta_temprana` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/apelacion_sancion.sql` | 31 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `apelacion_sancion` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/aval_participante.sql` | 32 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `aval_participante` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/candidato_reemplazo.sql` | 23 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `candidato_reemplazo` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/capacidad_respaldo.sql` | 25 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `capacidad_respaldo` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/castigo_deuda.sql` | 24 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `castigo_deuda` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/cobertura_incumplimiento.sql` | 37 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `cobertura_incumplimiento` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/cobertura_respaldo.sql` | 47 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `cobertura_respaldo` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/cobertura_respaldo_linea.sql` | 20 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `cobertura_respaldo_linea` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/descargo_participante.sql` | 27 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `descargo_participante` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/deuda_participante.sql` | 41 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `deuda_participante` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/devolucion_fondo.sql` | 24 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `devolucion_fondo` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/disolucion_anticipada.sql` | 28 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `disolucion_anticipada` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/ejecucion_aval.sql` | 26 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `ejecucion_aval` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/estrategia_cobranza.sql` | 26 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `estrategia_cobranza` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/evidencia_incumplimiento.sql` | 27 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `evidencia_incumplimiento` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/fondo_garantia.sql` | 34 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `fondo_garantia` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/gestion_cobranza.sql` | 32 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `gestion_cobranza` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/historial_estado_incumplimiento.sql` | 25 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `historial_estado_incumplimiento` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/historial_incumplimiento_usuario.sql` | 26 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `historial_incumplimiento_usuario` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/liquidacion_participante.sql` | 22 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `liquidacion_participante` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/lista_restriccion_interna.sql` | 29 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `lista_restriccion_interna` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/matriz_sancion.sql` | 27 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `matriz_sancion` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/movimiento_fondo.sql` | 33 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `movimiento_fondo` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/movimiento_reserva.sql` | 38 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `movimiento_reserva` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/plan_contingencia.sql` | 30 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `plan_contingencia` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/politica_cobertura.sql` | 34 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `politica_cobertura` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/politica_sancion.sql` | 20 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `politica_sancion` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/promesa_pago.sql` | 26 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `promesa_pago` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/recuperacion_respaldo.sql` | 23 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `recuperacion_respaldo` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/reemplazo_participante.sql` | 33 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `reemplazo_participante` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/registro_incumplimiento.sql` | 65 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `registro_incumplimiento` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/reserva_respaldo.sql` | 40 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `reserva_respaldo` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/sancion.sql` | 42 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `sancion` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/score_riesgo_incumplimiento.sql` | 22 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `score_riesgo_incumplimiento` de garantía/incumplimiento |
| `sql/10_tablas/08_garantia_incumplimiento/subrogacion.sql` | 21 | sí (bloque CREATE y COMMENT; subagente SQL) | Tabla `subrogacion` de garantía/incumplimiento |


### 12.b Conexiones y capas que consumen el módulo

| Archivo | Líneas | Leído completo | Qué hace |
|---|---|---|---|
| `sql/10_tablas/04_entregas_fondo/{entrega_fondo,fondeo_entrega,deduccion_entrega}.sql` | 55 / 45 / 26 | sí (CREATE) | Tablas de entregas |
| `sql/20_claves/08_garantia_incumplimiento.sql` | 595 | búsqueda completa (FK) | Claves foráneas de garantía |
| `sql/20_claves/04_entregas_fondo.sql` | — | búsqueda (FK) | Claves foráneas de entregas |
| `sql/30_indices/08_garantia_incumplimiento.sql` | 275 | sí | Índices y unicidad |
| `sql/35_append_only/append_only.sql` | 244 | búsqueda (garantía) | Triggers append-only |
| `sql/40_reglas/restricciones.sql` | 3 526 | parcial (1-40, 1340-1510, 1830-1866, 1955-1990, 2295-2640, 2765-2785) | Reglas R-GAR, R-DES, R-GRP, R-ORG, RLS |
| `sql/00_base/02_esquemas.sql`, `03_permisos.sql` | 595 / 284 | parcial | Esquemas, roles, grants y REVOKE |
| `sql/50_verificacion/verificaciones.sql` | — | parcial (115-136) | Cuadre de la reserva |
| `sql/60_semillas/02-politicas.sql`, `18-sanciones-y-cobranza.sql`, `22-cuentas-de-sistema.sql`, `15-eventos-y-plantillas.sql`, `23-indicadores-tableros.sql`; `seeders/minimos/02-politicas.json` | — | bloques relevantes | Semillas del módulo |
| `sql/61_dev/04,07,09`; `sql/62_volumen/volumen-01` | — | inserts de garantía | Datos de desarrollo y volumen sintéticos |
| `contratos/pactos/*.json` (10) | — | claves y descripción | Pactos |
| `docs/Arquitectura/ADR-051…` · `ADR-054…` · `ADR-049…` | 214 / 239 / 187 | completo · rangos · rangos | Decisiones a contrastar |
| `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml`, `entrega-del-turno.yaml`, `recarga-y-retiro.yaml`, `matriz.md` | — | búsqueda y rangos | Catálogo de flujos |
| `docs/CasosDeUso` CU-22..CU-26 | — | búsqueda y rangos | Casos de uso |
| `apps/movil/lib/pantallas/pasanaku/{dominio/estado_de_mora,pantalla_mi_estado,tarjeta_de_aporte,tarjeta_de_mi_grupo,dominio/subasta_del_cupo,dominio/retirar_subasta,errores_de_la_subasta,pantalla_subasta,pantalla_subastas,pantalla_subastar_cupo,formulario_de_puja,formulario_de_subasta,tarjeta_de_subasta,textos_de_la_subasta}.dart` | 37–150 c/u | sí | Mora, restricción y subasta |
| `apps/movil/lib/pantallas/identidad/laminas_del_tour.dart` | 51 | sí | Tour (fondo de garantía) |
| `apps/movil/lib/pantallas/identidad/textos.dart`, `pasanaku/textos.dart`, `pasanaku/rutas_operacion.dart`, `pasanaku/textos_del_traspaso.dart` | 197 / 187 / — / 103 | parcial (rangos relevantes) | Textos y rutas |
| `apps/backoffice/src/app/rutas/operacion/desembolsos/{dominio/cu28-desembolsos,bandeja-de-desembolsos,textos}.ts`, `operacion/comun/importe-sin-moneda.ts` | 34 / 87 / 14 / 27 | sí | Bandeja CU-28 |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-{operacion,tablero}.ts`, `rutas/tablero/dominio/catalogo-de-tableros.ts` | 95 / 58 / — | parcial | Textos de ayuda y tableros |
| `apps/web/src/app/paginas/inicio/secciones/*.ts`, `app.routes.ts` | — | búsqueda | Web pública |
| `packages/simulado/ejemplos/{garantia,entregas}/*.json` (41) | — | comparados por script contra OpenAPI; 2 leídos completos | Mocks |
| `postman/` (colección y humo de garantía y entregas, 4 archivos) | — | inventario por script | Colecciones |
| `clientes/dart/{garantia,entregas}/lib/*.dart` | 53 / 49 | sí | Barrels Dart versionados |
| `plataforma/comun-web/.../seguridad/GuardiaDePermiso.java` | — | rango 55-80 | Guardia de permisos |
| `servicios/nucleo-financiero/.../consumo/ConsumidorDeAsientosKafka.java` | — | rango 40-60 | Consumidor de asientos (punto de contacto) |
