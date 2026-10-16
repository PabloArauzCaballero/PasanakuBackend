# Plan — Backoffice financiero · Carril A (dominio, contratos y controles)

- Fecha: 2026-10-16 · Repos afectados: PasanakuBackend (`servicios/nucleo-financiero`, `servicios/cumplimiento`, `servicios/erp`, `docs/trabajo/`) · Rama: `codex/backoffice-dominio-20261015` (worktree `.claude/worktrees/backoffice-dominio`)
- Predecesor: `docs/trabajo/2026-10-15-backoffice-financiero/PLAN.md` (plan estratégico A0–A7 / B0–B6) y `COORDINACION.md`. Este documento es su descomposición ejecutable para el carril A.
- Resultado observable: un operador del backoffice con el permiso correcto puede **listar** (paginado, filtrado, con orden estable) las colas que hoy le obligan a tipear un UUID o que la UI simula —retiros en revisión, reclamos, ejercicios/períodos contables, alertas y casos LFT— y **actuar** sobre ellas (aprobar retiro con recibo idempotente, responder un reclamo), con el servidor revalidando permiso, segregación y estado; y el carril B tiene la ficha de contrato OpenAPI de cada uno.
- Kill-test: `curl` con token de operador a `GET /api/v1/billetera/retiros?estado=EN_REVISION` → 404/405 = no está hecho; reintentar `POST /billetera/retiros/{id}/aprobacion` con la misma `Idempotency-Key` → 409 = no está hecho.

## Alcance

- IN:
  - A0: matriz de trazabilidad pantalla → CU → entidad → servicio → contrato → permiso → estado (`existe/parcial/ausente/bloqueado`), matriz de autorización y ficha de contrato por módulo P0, como entregables en `docs/trabajo/2026-10-16-backoffice-carril-a/entregables/`.
  - A4: bandeja de retiros en revisión + idempotencia real de la respuesta de aprobación (`nucleo-financiero`).
  - A5: bandeja y respuesta de reclamos (CU-52), bandejas de alertas y casos LFT (CU-44) en `cumplimiento`.
  - A6: listado de ejercicios y períodos contables / período vigente (`erp`).
  - A7: traza (`Traza.marcarCasoDeUso`), permiso declarado y paginación acotada en cada endpoint nuevo (transversal, dentro de cada microtarea).
  - A2/A3: diseño (ADR propuesto) del núcleo de decisión automática y del gobierno de políticas; **la implementación de esquema queda condicionada** (ver Ambigüedades A-2).
- OUT (no se toca aunque se vea roto):
  - Angular / `apps/backoffice` (carril B). Sólo se lee para saber qué consume.
  - `sql/`, `docs/entidades/*.puml`, `seeders/` — cambio de modelo troncal (skill `arrancar-carril` §7: "Cambiar el modelo de datos → PARÁS"). Ninguna microtarea de este plan altera tablas.
  - Los 33 permisos que los CU exigen y el catálogo no tiene (S-8): **no se inventan** (Flujo de pantallas §9.6). Los endpoints nuevos usan permisos ya sembrados.
  - Avisos de SQL dinámico de `verificar_seguridad.py` §7 en `ReversoRepositorio`/`DestinoRepositorio`/`inversiones` (anotados, no arreglados).
  - Firma digital certificada, límites normativos, matriz SoD definitiva, retención de evidencia: decisiones de negocio/Legal (`DECISION_REQUIRED`).
- Ambigüedades registradas:
  - **A-1 · Dónde viven los entregables de A0.** La skill `arrancar-carril` declara `docs/` solo lectura durante carriles, pero el plan estratégico pide "ADR" y "matriz versionada". Supuesto: se escriben en `docs/trabajo/2026-10-16-backoffice-carril-a/entregables/` con estado *Propuesto*; promoverlos a `docs/Arquitectura/` lo decide el dueño de la bóveda. Confirmar con: Pablo Arauz (dueño del repo).
  - **A-2 · El plan estratégico asigna "esquema" al carril A; la skill de carril lo prohíbe.** Supuesto: el carril A **no** edita `sql/` ni `.puml` en esta sesión; el motor de decisión y el gobierno de políticas salen como ADR + modelo propuesto, y su implementación espera visto bueno de modelo (`boveda-modelo`). Confirmar con: Pablo Arauz + responsable de Seguridad/Riesgos.
  - **A-3 · Permiso de las bandejas.** No hay `RETIRO_VER` ni `CUMPLIMIENTO_ALERTA_VER`. Supuesto: listar retiros en revisión exige `RETIRO_APROBAR` (sólo quien puede decidir ve la cola); bandeja de reclamos exige `RECLAMO_VER` (ya sembrado); responder exige el permiso que el CU-52 declare y que esté sembrado — si no lo está, la acción queda cerrada (denegar por omisión) y se registra. Confirmar con: Seguridad de la información (S-8).
  - **A-5 · Códigos y estados HTTP de `aprobarRetiro` no coinciden con su contrato.** El YAML promete `409` cuando la orden ya no está en revisión y `404` cuando no existe; la implementación (convención `errores-api`, `ManejadorGlobalDeErrores.java:30-35`: 422 para negocio, 409 sólo para la base) responde `422 AP-CU11-11` y `422 AP-CU11-01` — y `AP-CU11-01` es también `SALDO_INSUFICIENTE`, así que el cliente no puede distinguir «orden inexistente» de «saldo insuficiente». Supuesto: no se cambia la plataforma ni el código público en este trabajo; B debe tratar `422 AP-CU11-11` como «ya resuelta». Confirmar con: dueño de `errores-api` / Pablo Arauz (asignar `AP-CU11-15 ORDEN_INEXISTENTE` exige actualizar CU-11).
  - **A-6 · Códigos de CU-52 reutilizados.** `CU52AtenderReclamo` lanza `AP-CU52-01` (`PUNTO_RECLAMO_INACTIVO`) también para «reclamo inexistente» y «reclamo ya cerrado» (`CU52AtenderReclamo.java:130,175,195`). Como `responder`/`prorrogar` no tenían endpoint, se asignan `AP-CU52-05 RECLAMO_INEXISTENTE` y `AP-CU52-06 RECLAMO_YA_CERRADO` antes de publicarlos. Confirmar con: dueño de CU-52.
  - **A-7 · `reclamo_cliente.monto_reclamado` sin moneda.** La regla 91.1.2 prohíbe un importe sin moneda; la tabla no tiene `moneda`. El contrato publica el monto como cadena decimal y declara que la moneda no está modelada, sin suponer BOB. Cerrarlo es cambio de modelo (troncal). Confirmar con: Pablo Arauz / dueño del modelo M12.
  - **A-8 · Lectura de datos financieros de una persona en Cumplimiento.** La regla 90.2.7 exige rastro auditable de toda lectura de datos financieros de una persona; `comun.registro_acceso_datos` existe (lo usa `auditoria/CU58`) pero exige finalidad (`R-SEG-02`). Supuesto: las bandejas LFT salen **sin** persona, monto ni detalle; la ficha con esos datos se construye cuando se defina el catálogo de finalidades de consulta (A1). Confirmar con: Oficial de Cumplimiento / Seguridad.
  - **A-4 · Datos personales en la bandeja de retiros.** La bandeja muestra ids, importe, moneda, estado, fechas y quién la solicitó (id), **no** cuenta destino ni nombre. Supuesto conservador por regla 90.2/96.5.3. Confirmar con: Cumplimiento.

## H1 — A0 · Inventario, trazabilidad y contratos P0

**CA:** Dado el plan estratégico y el código de la rama, cuando el carril B o un revisor abre los entregables, entonces encuentra para cada pantalla P0 su CU, entidad, servicio, operación OpenAPI (o `ausente`), permiso (o `no catalogado`) y estado, con ruta de archivo como evidencia; y ninguna historia P0 queda sin fuente de verdad o marcada como decisión pendiente con dueño.
**DoD:** archivos en `entregables/` + `python3 scripts/verificar_seguridad.py` pegado (lista S-8 vigente) + cada fila `existe` verificable con `grep` de su `operationId`.
**Estado:** HECHO (evidencia H1-01…H1-03; matrices regenerables con `entregables/matriz.py`).

### H1.S1 — Matrices
**CA:** Dado un módulo P0 del backoffice, cuando se busca en la matriz, entonces se ve su estado real con evidencia.
**DoD:** script de verificación de la matriz contra los OpenAPI en verde.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Matriz de trazabilidad pantalla→CU→entidad→servicio→operación→permiso→estado (`entregables/matriz-trazabilidad.md`) | Toda ruta de `Flujo de pantallas §3–§6` tiene fila | `python3 docs/trabajo/2026-10-16-backoffice-carril-a/entregables/matriz.py --verificar` → cada `operationId` marcado `existe` aparece en su YAML; 0 discrepancias | HECHO |
| H1.S1.M2 | Matriz de autorización (recurso/acción/MFA/SoD) con los 12 roles y los permisos sembrados; S-8 listado como `DECISION_REQUIRED` | Cada permiso citado existe en `sql/60_semillas/10-roles-y-permisos.sql` o está marcado no catalogado | `grep` de cada código en la semilla (salida en `evidencia/`) | HECHO |
| H1.S1.M3 | Vocabulario congelado de estados/errores/correlación/paginación para contratos nuevos (`entregables/convenciones-contrato.md`) basado en patrones existentes (`ProblemDetail`, `X-Correlation-Id`, `Idempotency-Key`) | Cada convención cita archivo:línea existente | revisión `grep` de las citas → todas resuelven | HECHO |

### H1.S2 — Diseño del núcleo de decisión y gobierno de políticas (A2/A3)
**CA:** Dado el patrón de gobierno de Atlas y las tablas `regla_*`/`politica_*` existentes, cuando se lee el ADR propuesto, entonces dice qué se reutiliza, qué entidades nuevas haría falta declarar en la bóveda, cómo se garantiza idempotencia/concurrencia y qué parámetros quedan cerrados hasta aprobación.
**DoD:** `entregables/ADR-propuesto-motor-de-decisiones.md` con inventario de tablas existentes citadas (`ls sql/10_tablas/*/{regla,politica}_*`).
**Estado:** HECHO (estado del ADR: *Propuesto*, pendiente de decisión del dueño de la bóveda — A-2).

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Inventario de motor/políticas existentes (`regla_*`, `politica_*`, skill `motor-de-reglas`, CU-48) con ruta | Toda tabla `regla_*`/`politica_*` del árbol aparece | `ls sql/10_tablas/*/ \| grep -E '^(regla\|politica)_'` vs. lista del ADR → mismo conjunto | HECHO |
| H1.S2.M2 | ADR propuesto: contrato `evaluar/obtener/historial/reprocesar`, estados, idempotencia, outbox, gobierno con SoD, rollback, adaptador documental OCR | Cubre las 7 decisiones de §2 del plan estratégico | checklist §2.1–§2.7 marcado con sección del ADR | HECHO |

## H2 — A4 · Bandeja de retiros en revisión y respuesta idempotente (`nucleo-financiero`)

> Decisión D-1 (2026-10-16): los criterios nuevos de CU-11 se escribieron en `docs/CasosDeUso/CU-11 Retirar saldo.md` porque `verificar_criterios.py` rechaza toda prueba sin su criterio en la bóveda; la skill de carril marca `docs/` como solo lectura, pero dejar la bóveda vieja crea dos verdades.

**CA:** Dado un operador con `RETIRO_APROBAR`, cuando abre la cola, entonces ve las órdenes `EN_REVISION` paginadas por `solicitada_en, id` con importe+moneda como cadena decimal; un operador sin el permiso recibe 403; y si reintenta la aprobación con la misma `Idempotency-Key` recibe el mismo recibo (200) y no una segunda transición, mientras que otra clave sobre una orden ya resuelta recibe `AP-CU11-11` (hoy 422; ver A-5).
**DoD:** `./gradlew :servicios:nucleo-financiero:spotlessCheck compileJava` + `integrationTest --tests '*CU11*'` en verde (salida pegada) + prueba HTTP real con servidor (`CU11ServidorRealTest` o equivalente) + `AutorizacionNegativaTest` en verde.
**Estado:** A MEDIAS — todo verde a nivel `TESTED` (evidencia H2-01…H2-08); falta la prueba HTTP contra el servicio levantado (`bootRun` + token real), ver REPORTE «No cubierto».

### H2.S1 — Contrato
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H2.S1.M1 | `GET /billetera/retiros` (`listarRetirosEnRevision`) en `nucleo-financiero.yaml`: filtros `estado` (sólo `EN_REVISION` en v1), `limite` 1–100, `cursor` opaco; respuesta `PaginaRetirosEnRevision` | El generador produce la interfaz y el modelo | `./gradlew :servicios:nucleo-financiero:openApiGenerate compileJava` exit 0 | HECHO |

### H2.S2 — Implementación de la bandeja
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H2.S2.M1 | Repositorio: consulta paginada por keyset `(solicitada_en, id)` sobre `ix_orden_retiro_estado` | Devuelve sólo `EN_REVISION`, orden estable, sin N+1 | `integrationTest --tests '*CU11BandejaTest*'` (caso correcto: 3 órdenes en 2 páginas sin repetir ni saltar) | HECHO |
| H2.S2.M2 | Caso de uso de lectura + controlador con `@Permiso("RETIRO_APROBAR")` y `Traza.marcarCasoDeUso("CU-11")` | 200 con permiso; 403 sin permiso; 401 sin token; `limite` fuera de rango → 400/422 del contrato | `integrationTest --tests '*CU11BandejaTest*' --tests '*AutorizacionNegativaTest*'` | HECHO |
| H2.S2.M4 | **(descubierta)** `ArquitecturaTest.laWebNoTieneReglas` ya estaba en rojo en `e3e11457` y en `origin/dev`: `MapeoDeBilletera.movimientos` (web) lee `MovimientoBilleteraRepositorio.Movimiento` (infraestructura). `ConsultarMovimientosBilletera` pasa a devolver un registro propio de aplicación | 0 violaciones; contrato de `GET /billetera/{id}/movimientos` sin cambios | `./gradlew :servicios:nucleo-financiero:test` → 0 fallas (incl. `ArquitecturaTest`, `BilleteraControllerWebTest`) | HECHO |
| H2.S2.M5 | **(descubierta)** las 44 pruebas `webTest` de `nucleo-financiero` fallan en `e3e11457` (y `dev`): `BilleteraController` exige `ConsultarMovimientosBilletera` y ninguna `@PruebaWeb` declara su doble (`TEST_BUG`). Agregar el `@MockitoBean` faltante en las 7 clases, sin tocar aserciones | 0 fallas por contexto; ninguna aserción modificada | `./gradlew :servicios:nucleo-financiero:webTest` → 0 fallas | HECHO |
| H2.S2.M6 | **(descubierta)** `testBarrido` rojo: preexistente (`BilleteraController` 312 líneas y `LIMITE_MAXIMO=100` en base) + mío (`LIMITE_DE_BANDEJA`, `CU11RetirarSaldo` 315, `OrdenRetiroRepositorio` 357, pruebas web >300). Separar la bandeja en piezas propias y llevar topes a configuración | `testBarrido` 0 fallas; regresión del servicio verde | `./gradlew :servicios:nucleo-financiero:testBarrido` + regresión | HECHO |
| H2.S2.M3 | Límite: cola vacía → página vacía con `siguiente` nulo; cursor inválido → error de contrato, nunca 500 | ambos casos verdes | `integrationTest --tests '*CU11BandejaTest*'` | HECHO |

### H2.S3 — Idempotencia real de la respuesta de aprobación
| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H2.S3.M1 | Reintento del **mismo aprobador** con el **mismo desenlace** sobre una orden que él ya resolvió → 200 con el mismo recibo (sin evento ni métrica duplicados); misma clave con otro desenlace → `AP-CU11-13`; otra clave sobre orden ya resuelta → `AP-CU11-11` como hoy (A-5) | test rojo primero, luego verde; contador de outbox = 1 | `integrationTest --tests '*CU11AprobacionIdempotenciaTest*'` | HECHO |
| H2.S3.M2 | Regresión del módulo de retiros | `CU11*`, `ReconciliacionDeRetirosTest`, `AutorizacionNegativaTest` verdes | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*CU11*' --tests '*Retiro*' --tests '*Autorizacion*'` | HECHO |

## H3 — A5 · Reclamos: bandeja y respuesta (`cumplimiento`, CU-52)

**CA:** Dado un operador con `RECLAMO_VER`, cuando abre la bandeja, entonces ve los reclamos paginados con estado, categoría, plazo persistido (nunca recalculado) y vencimiento; y dado un operador con el permiso de atención, cuando responde, entonces la respuesta se registra una sola vez por clave de idempotencia, con evento de outbox, y el reclamo cambia de estado según CU-52.
**DoD:** `./gradlew :servicios:cumplimiento:spotlessCheck compileJava integrationTest --tests '*CU52*'` verde + autorización negativa.
**Estado:** HECHO a nivel `TESTED` (evidencia H3-00…H3-04); sin prueba contra el servicio levantado (igual que H2).

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Descubrir qué expone `CU52AtenderReclamo`, permisos y estados | Lista con archivo:línea | `evidencia/H3-00-linea-base-cumplimiento.txt` + hallazgos en este plan (A-6, A-7) | HECHO |
| H3.S1.M2 | Contrato en `cumplimiento.yaml`: `GET /reclamos` (`listarBandejaDeReclamos`, vencimientos primero, cursor `(limiteVigente, id)`, `estados` opcional con default abiertos), `GET /reclamos/{reclamoId}` (`consultarReclamo`, ficha con descripción), `POST /reclamos/{reclamoId}/respuesta` (`responderReclamo`, `Idempotency-Key`) y códigos `AP-CU52-05/06/07` | genera y compila | `./gradlew :servicios:cumplimiento:compileJava` exit 0 | HECHO |
| H3.S1.M3 | Bandeja: repositorio keyset + `ConsultarBandejaDeReclamos` + controlador con `RECLAMO_VER` | correcto (orden por vencimiento, 2 páginas sin repetir) / límite (vacía; prorrogado ordena por la prórroga) / inválido (cursor a medias o estado desconocido `AP-CU52-07`) | `integrationTest --tests '*CU52BandejaTest*'` | HECHO |
| H3.S1.M4 | Respuesta: controlador → `CU52AtenderReclamo.responder` con idempotencia **por estado** (no hay `respuesta_idempotente` en el esquema `cumplimiento`): mismo responsable + mismo resultado/texto/devolución sobre reclamo ya cerrado ⇒ mismo recibo, sin segundo evento; cualquier otra cosa `AP-CU52-06`; inexistente `AP-CU52-05` | doble envío = 1 evento | `integrationTest --tests '*CU52RespuestaTest*'` | HECHO |
| H3.S1.M5 | Ficha `GET /reclamos/{id}` con `RECLAMO_ATENDER` (el texto del reclamo solo lo ve quien lo atiende) | 200 / 404→`AP-CU52-05` / 403 | `webTest --tests '*ReclamosBandejaWebTest*'` | HECHO |
| H3.S1.M6 | Matriz HTTP: 401/403 por ruta, 400 por enum/limite, forma JSON; bóveda CU-52 y Postman al día; regresión de `cumplimiento` | 0 fallas | `./gradlew :servicios:cumplimiento:spotlessCheck test webTest testBarrido integrationTest` + `verificar_criterios.py` + `verificar_boveda.py` | HECHO |

## H4 — A6 · Ejercicios y período vigente (`erp`)

**CA:** Dado un operador de contabilidad, cuando entra a Contabilidad, entonces el servidor le devuelve los ejercicios con sus períodos y cuál está vigente (abierto que contiene hoy en zona Bolivia), en lugar del UUID de demostración; si no hay período abierto, la respuesta lo dice (no inventa uno).
**DoD:** `./gradlew :servicios:erp:spotlessCheck compileJava integrationTest --tests '*Periodo*'` verde.
**Estado:** HECHO a nivel `TESTED` (evidencia H4-00…H4-03).

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Contrato `GET /erp/ejercicios` (`listarEjerciciosFiscales`, año descendente, `limite` 1–20, cursor `antesDeAnio`, con sus períodos) y `GET /erp/periodos/vigente` (`consultarPeriodoVigente`) + `AP-CU100-04 SIN_PERIODO_VIGENTE` | genera y compila | `./gradlew :servicios:erp:compileJava` exit 0 | HECHO |
| H4.S1.M2 | Lecturas en `CU100AbrirCerrarPeriodo` (`periodoDeLaFecha` existente para el vigente) + `ErpController` con `CONTABILIDAD_ERP_REPORTES` y `@PermisoAlternativo` de los otros 7 permisos ERP (los mismos que abren la sección en `secciones.ts:38-47`) | correcto (vigente = el que contiene hoy en America/La_Paz; ejercicios con 12 períodos) / límite (sin período para hoy → `AP-CU100-04`, nunca uno inventado; paginado por año) / inválido (sin permiso → 403) | `integrationTest --tests '*CU100PeriodosTest*'` + `webTest --tests '*PeriodosWebTest*'` | HECHO |
| H4.S1.M3 | Bóveda CU-100, Postman y regresión de `erp` (incl. `testBarrido`: `ErpController` está en 298 líneas, se mueve mapeo a `MapeoDeErp`) | 0 fallas | `./gradlew :servicios:erp:spotlessCheck test webTest testBarrido integrationTest` + gates | HECHO |

## H5 — A5 · Bandejas de alertas y casos LFT (`cumplimiento`, CU-44)

**CA:** Dado un analista u oficial de cumplimiento con el permiso sembrado, cuando abre Alertas o Casos, entonces ve la cola paginada desde `alerta_monitoreo_lft` / `caso_investigacion_lft` con estado, severidad y plazo, sin datos personales en la lista.
**DoD:** `./gradlew :servicios:cumplimiento:integrationTest --tests '*CU44Bandeja*'` verde.
**Estado:** HECHO a nivel `TESTED` (evidencia H5-01…H5-06).

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H5.S1.M1 | Contrato `GET /cumplimiento/alertas` (`listarBandejaDeAlertas`, `CUMPLIMIENTO_ALERTAS`) y `GET /cumplimiento/casos` (`listarBandejaDeCasos`, `CUMPLIMIENTO_CASOS`): sin persona, sin monto, sin detalle ni hallazgos (deber de reserva + lectura de datos financieros sin rastro auditable, regla 90.2.7) | compila | `./gradlew :servicios:cumplimiento:compileJava` exit 0 | HECHO |
| H5.S1.M2 | Bandejas: alertas por severidad y antigüedad (cursor `(peso, detectadaEn, id)` simplificado a `(detectadaEn, id)` con filtro de severidad/estado), casos por `plazo_limite` (vencidos primero) | correcto (orden y 2 páginas) / límite (vacía; caso vencido marcado) / inválido (filtro desconocido → código de contrato) / 403 | `integrationTest --tests '*CU44BandejaTest*'` + `webTest --tests '*LftBandejaWebTest*'` | HECHO |

### H5.S2 — Acciones de CU-44 por HTTP, corregidas
**CA:** Dado un caso abierto, cuando el oficial (distinto del analista guardado en el caso) registra la decisión, entonces el revisor es la identidad de la sesión, el analista y las alertas salen del caso guardado, un caso ya decidido no se vuelve a decidir (sin segundo evento) y "caso inexistente" tiene su propio código.
**DoD:** `integrationTest --tests '*CU44DecisionTest*'` + webTest de la ruta.
**Estado:** HECHO a nivel `TESTED` (H5-04, H5-05, H5-06).

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H5.S2.M1 | **(descubierta)** `CU44.decidir` toma `analistaId`/`revisadoPor`/`alertaIds` del cuerpo y **ignora** el `boolean` de `CasoLftRepositorio.decidir` (doble efecto); "caso no existe" usa `AP-CU44-04` (= `CASO_VENCIDO`). Nuevo `decidirComoRevisor(casoId, decision, conclusion, rosId, ctx)`: analista y alertas desde la base, revisor = sesión, transición condicionada y verificada, `AP-CU44-06 CASO_INEXISTENTE`, `AP-CU44-07 CASO_YA_DECIDIDO` | revisor = analista → `AP-CU44-02`; segunda decisión → `AP-CU44-07` y 1 evento | `integrationTest --tests '*CU44DecisionTest*'` | HECHO |
| H5.S2.M2 | Contrato + controlador `POST /cumplimiento/casos/{casoId}/decision` (`decidirCaso`, `CUMPLIMIENTO_CASOS` + rol `OFICIAL_CUMPLIMIENTO` según CU-44 «Exige») y `POST /cumplimiento/casos` (`abrirCasoLft`, `CUMPLIMIENTO_CASOS`) | 200/201, 403 para analista sin rol de oficial en la decisión, 400 cuerpo | `webTest --tests '*LftAccionesWebTest*'` | HECHO |
| H5.S2.M3 | Bóveda CU-44, Postman, regresión `cumplimiento` | 0 fallas | regresión + gates | HECHO |

## H6 — Cierre del carril en esta sesión

**CA:** Dado el trabajo hecho, cuando alguien que no vio la sesión abre el reporte, entonces sabe qué quedó verificado, qué contrato puede consumir B, qué decisiones esperan dueño y qué sigue.
**DoD:** `REPORTE.md` con las tres secciones + `python .claude/hooks/plan_status.py` pegado + commit y push de la rama.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H6.S1.M1 | Regresión de los tres servicios tocados (`test integrationTest` por servicio, en serie) | verde o fallo clasificado | salidas en `evidencia/` | HECHO |
| H6.S1.M3 | **(descubierta)** Fichas de contrato para el carril B (COORDINACION.md «Interfaz de coordinación obligatoria»): por operación nueva, permiso, estados, paginación, errores, idempotencia, correlación y ejemplo **sintético** marcado | una ficha por cada operación nueva (11) | `grep -c '^### ' entregables/fichas-de-contrato.md` → 11 | HECHO |
| H6.S1.M2 | `REPORTE.md` + commit + push a `origin/codex/backoffice-dominio-20261015` | push aceptado | `git log origin/codex/backoffice-dominio-20261015 -1` | HECHO |

## H7 — Ola 2 · Las llamadas que el backoffice ya hace y ningún contrato publica

**Origen:** cruce de las 33 llamadas HTTP de `apps/backoffice` contra todos los OpenAPI (script en
`evidencia/H7-00-llamadas-sin-contrato.txt`): 7 no tienen operación y en ejecución darían 404.
**CA:** Dado el backoffice actual, cuando cualquiera de sus pantallas hace una de esas llamadas,
entonces existe la operación (o queda declarada como `DECISION_REQUIRED` con motivo), con permiso,
paginación y respuesta mínima.
**DoD:** el cruce vuelve a correr y lista solo las declaradas como decisión; regresión de cada servicio tocado.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H7.S1.M1 | `GET /publicidad/anunciantes` (`PUBLICIDAD_ANUNCIANTES`), `GET /publicidad/campanas` (`PUBLICIDAD_APROBAR_CAMPANA`, filtro `estados`), `GET /publicidad/piezas-creativas` (`PUBLICIDAD_MODERAR`, filtro `estadoModeracion`) paginados por cursor | correcto/límite/inválido + 403 | `:servicios:publicidad:integrationTest --tests '*BandejasPublicidadTest*'` + `webTest` | HECHO |
| H7.S1.M2 | **(descubierta)** `PublicidadController.postularSocioComercial` y `ErpController.darDeAltaTercero` ponen `numeroDocumento` en el MDC de los logs (`Traza.marcarCasoDeUso`), regla 90.2.1. Se reemplaza por una etiqueta sin dato. (El mismo defecto con el **teléfono** en `identidad/SesionesController.java:96` queda reportado: no es de este carril) | ningún `marcarCasoDeUso` con documento/teléfono/correo en `servicios/` | `grep -rn 'marcarCasoDeUso(.*getNumeroDocumento' servicios` → vacío | HECHO |
| H7.S2.M1 | `GET /erp/ejercicios/{ejercicioId}/periodos`: períodos con estado, `cerradoEn` y totales debe/haber en una consulta | correcto (12 períodos, cerrado con totales) / inexistente → código / 403 | `:servicios:erp:integrationTest --tests '*CU100PeriodosTest*'` | TODO |
| H7.S3.M1 | `GET /erp/estados-financieros/{id}/documento` con `X-Hash-Contenido` **si** el contenido sellado está guardado; si no está modelado, `DECISION_REQUIRED` | evidencia del modelo | lectura de `estado_financiero_generado.sql` en evidencia | TODO |
| H7.S4.M1 | `GET /grupos/solicitudes-escaladas` **si** el escalamiento D-15 existe en el modelo/código de `grupos`; si no, `DECISION_REQUIRED` | evidencia | idem | TODO |
| H7.S5.M1 | `POST /extraccion/accesos`: no se implementa como endpoint para el front (el rastro de lectura es del servidor, R-SEG-02, A-8) | declarado | `entregables/fichas-de-contrato.md` § decisiones | TODO |
| H7.S6.M1 | Cruce final + regresión de servicios tocados + reporte + push | 0 llamadas sin contrato salvo declaradas | script de cruce + regresión | TODO |

## Pendiente fuera de esta sesión (registrado, no ejecutable sin decisión)

| Bloque del plan estratégico | Estado | Qué lo destraba |
|---|---|---|
| A1 · catalogar los 33 permisos S-8, recertificación, revocación al cambiar privilegios | `DECISION_REQUIRED` | Seguridad de la información decide recurso/acción/MFA/rol de cada código |
| A1 · columnas `aprobado_por` de restablecimiento y política de token por perfil (§9.4–9.5) | `DECISION_REQUIRED` | cambio de modelo con ADR (`boveda-modelo`) |
| A2/A3 · tablas del motor de decisión y versiones de política | `DECISION_REQUIRED` | aprobación del ADR propuesto (H1.S2) + cambio de modelo |
| A4 · conciliación, cierre diario, desembolsos por excepción (CU-50/51/28) | TODO | siguiente ola del carril, mismo patrón que H2 |
| A5 · UIF, requerimientos, riesgo, incidentes, continuidad (CU-43/45/54/55/56), denuncia (§9.1) | TODO / `DECISION_REQUIRED` (denuncia) | siguiente ola; denuncia exige modelo |
| A6 · resto de ERP, tarifario, mensajería, reportería, publicidad | TODO | siguiente ola |
| A7 · paneles, alertas con runbook, backups/restauración | TODO | siguiente ola |

## Riesgos y bloqueos previstos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| `bd:levantar` falla: quay.io devuelve 401 para la imagen de MinIO | No se levanta el perfil `base` completo | Se levanta sólo `postgres` (`docker compose ... up -d --wait postgres`); las pruebas de integración usan Testcontainers. Registrado como `ENVIRONMENT`. |
| La rama está 5 commits detrás de `origin/dev` | Conflicto al integrar | Integrar `dev` al final tras revisar diferencias (COORDINACION.md), no a ciegas. |
| B consume `GET /reclamos` con forma no paginada | Ruptura del cliente B | Se publica la forma paginada en la ficha de contrato y se avisa en el reporte; B ya declara el supuesto en `cu52-reclamos.ts:54`. |
