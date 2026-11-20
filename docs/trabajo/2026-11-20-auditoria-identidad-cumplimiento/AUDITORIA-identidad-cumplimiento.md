# AUDITORÍA — identidad-cumplimiento (lote L4)

> Estado del documento: **EN CONSTRUCCIÓN**. Se escribe por tandas y cada tanda se sube a la rama
> `auditoria/identidad-cumplimiento-2026-11-20`. Las secciones marcadas «pendiente» todavía no se cerraron.

## 1. Ficha

| Campo | Valor |
|---|---|
| Módulo | identidad-cumplimiento (L4): `servicios/identidad`, `servicios/cumplimiento`, KYC, UIF/ASFI, retiros con doble aprobación |
| Responsable | Pablo (dueño del producto) |
| Fecha | 2026-11-20 |
| Commit auditado `origin/test` | `07fc69719287b0dadd152d0293a054948df69ead` |
| Commit `origin/dev` | `850e0f51192d03094304630a61e44e5bd2a97efe` — `git diff --stat origin/test origin/dev` vacío: **mismo árbol** |
| Historial | completo (`git fetch --unshallow`; `git rev-parse --is-shallow-repository` → `false`; 1878 commits en `origin/test`) |
| Archivos / líneas cubiertos | pendiente (anexo §12) |

## 2. Resumen

Pendiente de cierre. Tabla de severidades y diez más graves se completan al terminar la lectura.

## 3. Huella y mapa de conexiones

### 3.1 Huella

| Capa | Ubicación | Volumen | Cómo se leyó |
|---|---|---|---|
| Servicio Java `identidad` | `servicios/identidad/` (main 108 archivos, test 81) | ~21 000 líneas Java | lote 1 (main completo; 6 pruebas completas, resto inventariado) |
| Servicio Java `cumplimiento` | `servicios/cumplimiento/` (main 98, test 80) | ~25 300 líneas Java | lote 2 (main completo; pruebas de autorización/UIF/PEP completas, resto inventariado) |
| OpenAPI | `servicios/identidad/src/main/resources/openapi/identidad.yaml`, `servicios/cumplimiento/src/main/resources/openapi/*.yaml` | 39 rutas identidad; ~50 operaciones cumplimiento | lotes 1–3 |
| Esquema | `sql/10_tablas/01_identidad_usuarios/` (27 tablas), `sql/10_tablas/12_cumplimiento_asfi/` (47 tablas); sentencias de esas tablas en `sql/20_claves`, `30_indices`, `35_append_only`, `40_reglas/restricciones.sql`, `60_semillas`, `61_dev`, `62_volumen` | — | lote 3 |
| Retiros con doble aprobación (ADR-049) | `servicios/nucleo-financiero/.../{ResolucionDeRetiro,BandejaDeRetiros,ReglasDeRetiro,CU11RetirarSaldo,SolicitudDeRetiro,AprobacionDeRetiroRepositorio,OrdenRetiroRepositorio,SegundoFactorStepUp}.java`, `BilleteraController` (endpoints de retiro), `sql/10_tablas/10_billetera_custodia/orden_retiro.sql` | — | lote 3 (solo el circuito de retiro; el resto de nucleo-financiero es del lote L1) |
| Pactos y clientes | `contratos/pactos/identidad-grupos-pertenencia.json`; `clientes/dart/{identidad,cumplimiento}` (barrel versionado, `src/` ignorado por `.gitignore:51`); `clientes/angular/*` | — | lote 3 (inventario, comparación por operación con el OpenAPI) |
| App móvil | `apps/movil/lib/pantallas/identidad/**` | 154 archivos, 15 288 líneas Dart | lote 4 (completo) |
| Backoffice / web | `apps/backoffice/src/app/rutas/{cumplimiento,sistemas/{accesos,usuarios-internos,roles-internos,permisos-internos},ingreso,operacion/retiros}`, pantallas de identidad de `apps/web` | ~9 700 líneas TS | lote 5 |
| Documentación de contraste | `docs/Arquitectura/ADR-049`, `ADR-051`, `ADR-054`, `docs/Cumplimiento.md`, `docs/catalogo-de-flujos/recarga-y-retiro.yaml` | — | orquestador y lote 3 |

Supuesto de frontera (registrado en §11): las pantallas de grupos/admisiones/postulaciones del backoffice son del lote L3; el resto de `nucleo-financiero` (recarga, mayor, QR) es del lote L1.

### 3.2 Conexiones

| Desde | Hacia | Mecanismo | Punto exacto | Observaciones |
|---|---|---|---|---|
| los 13 servicios | identidad | JWKS `GET /.well-known/jwks.json` | `servicios/identidad/.../web/JwksController.java:32-35`; `plataforma/comun-web/.../ConfiguracionDelDecodificador.java:27-28` | sin `iss`/`aud` (IDCU-SEG-005) |
| gateway | identidad | rutas `/api/v1/{identidad,permisos,roles,sesion,sesiones,usuarios,usuarios-internos}/**` | `plataforma/gateway/src/main/resources/rutas.yml:21-42,107-112` | rate limit solo en `/sesiones/**` y `/usuarios/**` |
| identidad | grupos | HTTP `GET /grupos/{g}/integrantes/{u}/comparte` + pacto | `servicios/identidad/.../infraestructura/clientes/GruposPorHttp.java:28-37`; `contratos/pactos/identidad-grupos-pertenencia.json:14` | timeout global 1 s/3 s; falla cerrado; pacto sin prueba de productor |
| identidad | organizador | HTTP habilitación básica | `servicios/identidad/.../infraestructura/clientes/OrganizadorPorHttp.java:69-84` | resultado de `firmar` descartado |
| identidad | Gmail API | HTTPS | `servicios/identidad/.../infraestructura/GmailApiCorreoDeVerificacion.java:57-88` | envío dentro de transacción en CU-04 |
| identidad | almacén de archivos | `comun-archivos` | `CU02GuardarFotoDelExpediente.java:121`, `CU02RevisarExpediente.java:64,70` | objeto antes que fila |
| identidad | Kafka | outbox `identidad.*` (10 tópicos) | `CU01RegistrarUsuario.java:240-247`, `CU04Autenticar.java:116,195` | la decisión KYC no emite evento |
| grupos | identidad | HTTP tokens de invitación, `/usuarios/{id}/kyc`, `/usuarios/busquedas/por-telefono` | `servicios/grupos/.../TokensDeInvitacionPorHttp.java:46,68,86,103`; `PersonasPorHttp.java:25,32,44` | identidad no valida que el emisor administre el grupo |
| entregas | identidad | HTTP `/usuarios/{id}/titularidad/verificacion` (`SERVICIO_ENTREGAS`) | `servicios/entregas/.../TitularPorHttp.java:68` | tope por réplica |
| nucleo-financiero | identidad | JWT de evidencia step-up | `servicios/nucleo-financiero/.../SegundoFactorStepUp.java:118-169` | no liga monto ni destino |
| garantia, organizador | cumplimiento | HTTP `GET /reclamos/usuarios/{id}/efecto` | `servicios/garantia/.../EfectoDeReclamosPorHttp.java:52`; `servicios/organizador/.../EfectoDeReclamosPorHttp.java:51` | token del usuario vs permiso `RECLAMO_VER` |
| grupos | cumplimiento | HTTP `GET /licencia/alcance` | `servicios/grupos/.../HechosPorHttp.java:157-162` | cada rechazo escribe outbox |
| cumplimiento | `nucleo_financiero.consumo_limite`, `cuenta_billetera` | SELECT cruzado | `servicios/cumplimiento/.../LimiteRepositorio.java:83-84,113-118` | IDCU-ACOPLE-002 |
| cumplimiento | `catalogo.*` (tarifario, licencia, límites, umbral UIF) | SELECT (ADR-029) | `ContratoRepositorio.java:113-123`, `LicenciaRepositorio.java:32-53` | admitido por ADR |
| nucleo-financiero | `cumplimiento.registro_operacion_relevante` | trigger diferido SECURITY DEFINER | `sql/40_reglas/restricciones.sql:1052-1056` | ADR-017 lo admite; idempotente por `uq_operelev_tx_umbral` |
| cumplimiento | Kafka | outbox, ~30 tópicos `cumplimiento.*` | `evidencia/listar_topicos.txt` | ninguno con consumidor |
| cumplimiento | identidad / nucleo-financiero | eventos `consentimiento_registrado`, limitar cuenta, bloqueo por oficio | `CU05AceptarContrato.java:120-131`, `CU06RevisarConocimiento.java:403-421`, `CU45AtenderRequerimiento.java:370-385` | sin consumidor |
| app móvil | identidad / cumplimiento | Dio + cliente generado | ver tabla de conexiones del lote 4 en IDCU-CONTRATO-017 | seis operaciones por Dio crudo |
| backoffice | identidad / cumplimiento / nucleo-financiero | HttpClient | ver tanda 6 | — |

### 3.3 Saltos del dinero que toca este módulo (98.8.1)

Retiro: app → `nucleo-financiero POST /billetera/retiros` → `cumplimiento`? **no**: los límites los evalúa la copia de nucleo-financiero (IDCU-REESCRITURA-001) → `identidad` (evidencia step-up, JWT) → `orden_retiro` EN_REVISION/AUTORIZADA → (si ≥ umbral) backoffice `POST /billetera/retiros/{id}/aprobacion` → evento `retiro_autorizado` → **sin consumidor**: nadie instruye el pago (IDCU-FLUJO-FALTANTE-036).

## 4. Distancia al flujo nuevo

### 4.a Inventario de aprobaciones

Clasificación según §9.4.a del metaprompt. «Norma» = fuente del repo citada; la determinación legal la hace Cumplimiento.

| ID | Dónde (`ruta:línea`) | Qué aprueba y quién | Estado · endpoint · permiso · pantalla · evento | Clasificación | Qué se rompe aguas abajo si se quita |
|---|---|---|---|---|---|
| APR-01 | `servicios/identidad/.../web/VerificacionesController.java:97` → `aplicacion/CU02RevisarExpediente.java:83-128`; `identidad.yaml:350-352,458` («la aprobacion sigue siendo manual») | Revisión KYC manual: operador con `VERIFICACION_RESOLVER` (ADMIN_PLATAFORMA, OFICIAL/ANALISTA_CUMPLIMIENTO) aprueba o rechaza el expediente | `verificacion_kyc` PENDIENTE/EN_REVISION→APROBADA/RECHAZADA; `POST /identidad/verificaciones/{id}/decision`; backoffice `rutas/cumplimiento/verificaciones`; **sin evento** | DECISIÓN REQUERIDA (A-1) | Único camino a ACTIVO y rol PARTICIPANTE; gating de la app (APR-12); cumplimiento no se entera |
| APR-02 | `servicios/identidad/.../infraestructura/UsuariosInternosRepositorio.java:221-233` | Operador cambia `usuario.estado` (ACTIVO/SUSPENDIDO) de cualquier usuario | `PATCH /usuarios-internos/{id}`; `USUARIOS_INTERNOS_GESTIONAR`; evento `usuario_interno_actualizado` | ELIMINABLE como camino de activación (debería quedar solo para internos); la suspensión es control de seguridad | Segundo camino paralelo a APR-01 (IDCU-SEG-076) |
| APR-03 | `servicios/identidad/.../aplicacion/ActivarRolOrganizador.java:40-58`; `OrganizadorPorHttp.java:69-84` | CU-90: rol ORGANIZADOR exige KYC aprobado + habilitación confirmada por el servicio organizador | `POST /usuarios/{id}/roles/organizador`; `asignacion_rol` | DECISIÓN REQUERIDA (A-1; además ADR-054 quitó al organizador humano del alta y el inicio) | Creación de grupos con organizador |
| APR-04 | `servicios/identidad/.../aplicacion/CU08AsignarRol.java:248-300`; `UsuariosInternosController.java:44,72` | Asignación humana de roles internos (otorgante ≠ titular) | `POST/PUT /usuarios-internos`, `/roles`; `ck_asignacion_no_autoasignada` | CONSERVAR (control de acceso del backoffice; no es circuito del pasanaku) | Sin operadores |
| APR-05 | `servicios/identidad/.../aplicacion/CU09CambiarCredencial.java:85-92` | Restablecimiento asistido de credencial con `aprobadoPor` | sin endpoint | NO APLICA hoy (código muerto) | nada |
| APR-06 | `servicios/identidad/.../aplicacion/CU04StepUp.java:33-117` | Evidencia de segundo factor del propio titular para RETIRO / CAMBIO_CUENTA / ADMIN | `POST /sesiones/desafios`, `/verificacion` | CONSERVAR (no es aprobación de terceros: es autenticación del titular) | retiros sin MFA |
| APR-07 | `servicios/nucleo-financiero/.../web/BilleteraController.java:158-174`; `ResolucionDeRetiro.java:188-290`; `ck_retiro_doble_aprobacion`; `application.yml:58-61` (umbral 5000.00) | Doble aprobación de retiro ≥ umbral: TESORERIA u OPERATIONS_MANAGER ≠ solicitante | `orden_retiro` EN_REVISION→AUTORIZADA/RECHAZADA; `GET /billetera/retiros`, `POST /billetera/retiros/{id}/aprobacion`; `RETIRO_APROBAR`; backoffice `rutas/operacion/retiros`; evento `retiro_autorizado` | DECISIÓN REQUERIDA (A-1). Insumo: `docs/Cumplimiento.md:170-176` sostiene que la norma «no exige aprobación humana operación por operación», sino niveles y montos de autorización definidos y aprobados por el Directorio (candidata a ELIMINABLE si se reemplaza por política de umbrales) | Bandeja, endpoint, permiso, pruebas `BilleteraAprobacionWebTest`/`BilleteraBandejaWebTest`, paso del catálogo `recarga-y-retiro.yaml`; el CHECK no estorba si `requiere_doble_aprobacion=false` |
| APR-08 | `servicios/cumplimiento/.../aplicacion/CU02ElevarDiligencia.java:125-133`; `web/AdhesionController.java:158-165`; trigger `restricciones.sql:1103-1124` | Elevar debida diligencia; PEP con cuatro ojos | `POST /cumplimiento/usuarios/{id}/diligencia`; `ANALISTA_CUMPLIMIENTO` | CONSERVAR POR NORMA (`docs/Cumplimiento.md` §2.3: «Doble revisión en DDD reforzada»); confirmar con Cumplimiento (A-1) | topes por nivel |
| APR-09 | `servicios/cumplimiento/.../aplicacion/CU43RemitirReportes.java:147-166`; `ck_reporte_segregacion` | Aprobar y enviar reporte mensual UIF/ASFI (aprobador ≠ generador) | `POST /uif/reportes/envios`; `CUMPLIMIENTO_REPORTAR` | CONSERVAR POR NORMA (obligación de reporte UIF, `docs/Cumplimiento.md` §2.1–2.2; la segregación es R-SEG-04 interna) | remisión regulatoria |
| APR-10 | `servicios/cumplimiento/.../web/CumplimientoController.java:78-114`; `CU44DecidirComoRevisor.java:327-389` | Caso LFT: analista investiga, oficial distinto decide | `POST /cumplimiento/casos`, `/casos/{id}/decision`; `CUMPLIMIENTO_CASOS` | CONSERVAR POR NORMA (`docs/Cumplimiento.md:183-185`: «la UIF obliga a que la alerta la analice un analista y a que el reporte lo revise otra identidad») | ROS |
| APR-11 | `servicios/cumplimiento/.../aplicacion/CU45AtenderRequerimiento.java:329-444` | Oficial registra y responde oficios de autoridad | `POST /uif/requerimientos`, `/respuesta` | CONSERVAR POR NORMA (atención de requerimientos, `docs/Cumplimiento.md` §2.3 «Congelamiento y atención de requerimientos») | — |
| APR-12 | `apps/movil/lib/pantallas/identidad/pantalla_identidad_requerida.dart:15-18,91-111`; `textos_de_verificacion.dart:6-21,42-81`; `dominio/estado_de_verificacion.dart:10-23`; `apps/movil/lib/proveedores/sesion.dart:149-159` | Compuerta de la app: sin KYC aprobado por una persona no hay dinero ni pasanaku; textos «una persona los revise» | pantallas de estado/sondeo; claim `billeteraPermitida` | sigue a APR-01 (DECISIÓN REQUERIDA A-1) | rediseño de compuerta y textos |
| APR-13 | `servicios/cumplimiento/.../aplicacion/CU52AtenderReclamo.java:173-238` | Punto de reclamo responde; alimenta `EfectoDelReclamo` | `POST /reclamos/{id}/respuesta`; `RECLAMO_ATENDER` | CONSERVAR POR NORMA (protección del consumidor, `docs/Cumplimiento.md` §1.3) | debido proceso (91.4) |
| APR-14 | `CU47EvaluarRiesgoDeProducto.java:149-190`, `CU48CalibrarReglas.java:131-164`, `CU49DesignarOficial.java:68-162`, `CU53ElevarReclamo.java:116-157`, `CU55GestionarIncidente.java:145-204`, `CU56EjecutarPruebaDeContinuidad.java:73-78` | Gobierno: comité, reglas LFT, oficial, segunda instancia, incidentes, continuidad | varios sin endpoint | CONSERVAR (gobierno interno; fuera del circuito del pasanaku) | — |
| APR-15 | `sql/60_semillas/10-roles-y-permisos.sql` | Permisos de aprobación de otros módulos sembrados acá: `ENTREGA_AUTORIZAR`, `SOLICITUD_INGRESO_RESOLVER`, `REVERSO_AUTORIZAR` | — | DECISIÓN REQUERIDA en los lotes L2/L3 (tocan el circuito) | `SegregacionDeFunciones` ya no los reconoce (IDCU-DEFECTO-079) |

Total: **15 filas** (≈ 30 puntos de código). DECISIÓN REQUERIDA: APR-01, APR-03, APR-07, APR-12, APR-15 (5). CONSERVAR / CONSERVAR POR NORMA: 9. ELIMINABLE: APR-02 como vía de activación (1). Las filas de backoffice se completan en la tanda 6.

### 4.b Inventario de respaldo y cobertura

| ID | Dónde | Qué pone la empresa y cuándo | Asiento | Clasificación | Qué depende de ello |
|---|---|---|---|---|---|
| RSP-01 | `apps/movil/lib/pantallas/identidad/textos.dart:42-46`; `laminas_del_tour.dart:44-50`; arte `ArteFondoDeGarantia` (`packages/diseno_flutter`) | Promesa de pantalla: «un fondo de garantía cubre la cuota para que el turno se entregue igual» (no mueve dinero) | ninguno | CONTRADICE R-NUEVA-2 en su promesa; el origen del fondo es DECISIÓN REQUERIDA (A-2/A-3) | primera pantalla del tour; IDCU-RESPALDO-007 |
| RSP-02 | `sql/60_semillas/10-roles-y-permisos.sql:94,241-242` | Permiso `FONDO_ADMINISTRAR` (fondo de garantía, CU-29) para ADMIN_PLATAFORMA y TESORERIA | — | DECISIÓN REQUERIDA (A-3; semántica en el lote L2) | lote L2 |
| RSP-03 | `sql/10_tablas/01_identidad_usuarios/referencia_personal.sql:13` (`acepta_ser_avalista`) | Aval de una persona, no de la empresa | — | NO APLICA (aval entre participantes, A-3) | — |

Búsquedas que sustentan «nada más»: `grep -rniE "respaldo|cobertura|garant|fondo|subsid|anticip"` sobre `servicios/identidad/src/main`, `servicios/cumplimiento/src/main`, tablas y semillas del módulo y `apps/movil/lib/pantallas/identidad` (lotes 1–4). Identidad y cumplimiento no escriben el mayor.

### 4.c Ambigüedades A-1 a A-4

| ID | Qué muestra el código hoy | Supuesto de trabajo | A quién confirmar |
|---|---|---|---|
| A-1 | KYC con revisión humana obligatoria (APR-01, APR-12); diligencia PEP con cuatro ojos (APR-08); caso LFT analista/revisor (APR-10); doble aprobación de retiros ≥ 5000.00 sin moneda ni fuente (APR-07). `docs/Cumplimiento.md:170-185` distingue: la norma no exige aprobación por operación de desembolsos, sí analista + revisor en la vía del sospechoso | No se elimina ninguna; se listan como DECISIÓN REQUERIDA o CONSERVAR POR NORMA | Pablo (producto) y Cumplimiento (legal) |
| A-2 | En este módulo no hay rama de entrega con faltante. Sí hay una promesa de pantalla (RSP-01) de que «el turno se entrega igual» | No se inventa la regla | Pablo |
| A-3 | `FONDO_ADMINISTRAR` sembrado (RSP-02); aval entre personas en `referencia_personal` (RSP-03) | El aval entre participantes no entra en R-NUEVA-2 | Pablo |
| A-4 | Cumplimiento no calcula mora ni recargos. `EfectoDelReclamo` (`servicios/cumplimiento/.../dominio/EfectoDelReclamo.java:65-99`) frena deuda o sanción firme mientras haya reclamo abierto; consumido por garantía y organizador con un permiso que el llamador no tiene (ver tanda 5) | Sin supuesto: no hay regla en este módulo | Pablo |

## 5. Hallazgos

### IDCU-REESCRITURA-001 — CU-40 «Evaluar límites» existe dos veces; la copia de cumplimiento está muerta y diverge de la que mueve dinero
- Clase / Severidad / Confianza: REESCRITURA (también ACOPLE y DEFECTO) / S2 / confirmado
- Ubicaciones: `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU40EvaluarLimites.java:34-98`;
  `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/LimiteRepositorio.java:46-110,113-118`;
  `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU40EvaluarLimites.java:41-120` (la viva)
- Evidencia:
  - Javadoc de cumplimiento, `LimiteRepositorio.java:49-52`: «El consumo se lee con {@code FOR UPDATE} … leerlo sin bloquear la fila permite que dos operaciones simultaneas … pasen el tope diario».
  - La consulta (`:72-110`) es un `leftJoin(DSL.table(DSL.name("nucleo_financiero", "consumo_limite")))` **sin** `forUpdate()`: `grep -n 'forUpdate\|FOR UPDATE' LimiteRepositorio.java` → solo la línea 49 (el comentario).
  - La versión de nucleo-financiero sí bloquea: `var consumo = bloqueando ? limites.acumuladoBloqueado(...)` y ofrece `exigirDentroDe(dsl, …)` dentro de la transacción del dinero.
- Cómo se comprobó: `grep -rn 'CU40EvaluarLimites\b' servicios/cumplimiento/src` → solo pruebas (`ArmadoDeOla3.java:70`, `BaseDeCumplimiento.java:82`, `CU40Test`, `CU40RechazosTest`); ningún controlador ni consumidor de `servicios/cumplimiento/src/main/**/web` lo invoca; `grep` de una ruta `/limites` en el OpenAPI de cumplimiento → sin operación. Los consumidores reales (`CU11RetirarSaldo`, `SolicitudDeRetiro`, `CU12TransferirSaldo`, `CU10RecargarSaldo`) importan el de nucleo-financiero.
- Impacto: hoy ninguno en dinero (la copia no se ejecuta en producción), pero hay dos respuestas para la misma regla R-LIM-01: la copia muerta no bloquea, resuelve la ventana distinto (`ventana_inicio/ventana_fin` de la fila vs `VentanaDeLimite.resolver`) y emite `cumplimiento.limites_evaluados` con el `monto` en el payload. Si alguien la cablea creyendo el Javadoc, dos operaciones concurrentes pasan el tope. Las pruebas `CU40Test`/`CU40RechazosTest` dan cobertura a código que no corre y no a la regla viva.
- Causa de fondo: CU-40 se implementó en dos carriles; la decisión de en qué servicio vive quedó escrita como «divergencia declarada» (`LimiteRepositorio.java:59-71`) en lugar de resolverse.
- Solución de fondo: un único CU-40 en `nucleo-financiero` (dueño de `consumo_limite` y de la transacción); retirar la copia de cumplimiento con sus pruebas; si cumplimiento necesita el rastro, consumirlo por evento (`nucleo_financiero.limite_rechazo`) en vez de leer el esquema ajeno.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: nucleo-financiero (lote L1), esquema `nucleo_financiero` (`consumo_limite`, `cuenta_billetera`), `catalogo.limite_operativo_billetera`
- Estándares y normas: ISO/IEC 25010 Mantenibilidad (modularidad) y Fiabilidad; reglas 98.1.1 (lectura de base ajena), 96.3.1, 00 §1.1 (pieza duplicada)
- Esfuerzo estimado: chico

### IDCU-ACOPLE-002 — Cumplimiento lee tablas del esquema `nucleo_financiero`
- Clase / Severidad / Confianza: ACOPLE / S3 / confirmado
- Ubicaciones: `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/LimiteRepositorio.java:83-84` (`nucleo_financiero.consumo_limite`), `:113-118` (`nucleo_financiero.cuenta_billetera`)
- Evidencia: `.from(DSL.table(DSL.name("nucleo_financiero", "cuenta_billetera")))`; el propio archivo lo declara: «INVARIANTE-11 DECLARADO · `consumo_limite` vive en `nucleo_financiero`» (`:60`).
- Cómo se comprobó: lectura del archivo; `python3 scripts/auditar_backend.py` lo lista en «DIVERGENCIAS DECLARADAS» (`evidencia/auditar_backend.txt`).
- Impacto: un cambio de columna en `nucleo_financiero` rompe cumplimiento sin que ninguna prueba de contrato lo avise; el rol de base de cumplimiento necesita permisos sobre el esquema ajeno. Severidad S3 porque el único camino que lo usa es la copia muerta de IDCU-REESCRITURA-001.
- Causa de fondo: la misma que IDCU-REESCRITURA-001.
- Solución de fondo: se resuelve al retirar la copia; si cumplimiento necesitara el nivel de diligencia de la cuenta, pedirlo por API o recibirlo por evento.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: nucleo-financiero
- Estándares y normas: ISO/IEC 25010 Mantenibilidad (modularidad); regla 98.1.1 y 98.1.2
- Esfuerzo estimado: chico

### IDCU-DEFECTO-003 — La fecha de vigencia de los límites se calcula en UTC, no en `America/La_Paz`
- Clase / Severidad / Confianza: DEFECTO / S3 / confirmado
- Ubicaciones: `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU40EvaluarLimites.java:51`
- Evidencia: `var hoy = reloj.ahora().atOffset(ZoneOffset.UTC).toLocalDate();` — mientras `plataforma/comun-dominio/.../Reloj.java:26-30` ofrece `ZONA = ZoneId.of("America/La_Paz")` y `hoy()`.
- Cómo se comprobó: lectura de ambos archivos.
- Impacto: entre las 20:00 y las 24:00 de La Paz (UTC-4) el código ya considera «mañana» para `vigente_desde`/`vigente_hasta`: un tope que vence hoy deja de aplicarse cuatro horas antes y uno nuevo entra cuatro horas antes. Hoy acotado porque la copia no se ejecuta (IDCU-REESCRITURA-001).
- Causa de fondo: no se usa `Reloj.hoy()`.
- Solución de fondo: usar `reloj.hoy()` en todo cálculo de fecha calendario; se resuelve junto con la unificación de CU-40.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: la copia viva de nucleo-financiero tiene el mismo patrón (ver §10).
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (corrección); ISO 8601
- Esfuerzo estimado: chico

### IDCU-SEG-004 — TEST corre con perfil `local`: segundo factor «000000» para cualquiera y clave JWT en memoria permitida
- Clase / Severidad / Confianza: SEG (también CONFIG y REG) / S2 / confirmado (por documentación y código; el entorno TEST no se tocó)
- Ubicaciones: `servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/EmisorDeAcceso.java:73-80,82-91`; `despliegue/TEST.md:86-88,458`; `docker-compose.coolify.yml:448`
- Evidencia:
  - `EmisorDeAcceso.java:74-75`: `boolean produccion = entorno.acceptsProfiles(Profiles.of("staging", "production")); if (generada && produccion) { throw … }` → con cualquier otro perfil, una clave vacía genera una **en memoria** (`:80`) y solo deja un `WARN` (`:89`).
  - `despliegue/TEST.md:86-87`: «TEST corre con el perfil `local`, que trae el segundo factor de desarrollo: **el código es `000000` para cualquiera**».
  - `docker-compose.coolify.yml:448`: `JWT_CLAVE_FIRMA: ${JWT_CLAVE_FIRMA}` sin `:?` que obligue a definirla.
- Cómo se comprobó: lectura; no se accedió al servidor TEST (prohibido por el metaprompt §5).
- Impacto: en un servidor expuesto, el segundo factor del backoffice no protege nada (cualquier credencial de operador filtrada basta); con dos réplicas y la clave vacía, cada réplica firma con la suya. Hoy TEST no maneja dinero real; pero el perfil es la única barrera entre «desarrollo» y «compartido», y el fail-closed no cubre el perfil que efectivamente usa el entorno compartido.
- Causa de fondo: el fail-closed se ata a nombres de perfil (`staging`, `production`) y no a «entorno compartido»; TEST quedó en `local` como decisión documentada.
- Solución de fondo: un perfil propio para TEST que exija `JWT_CLAVE_FIRMA` y deshabilite el segundo factor fijo, o invertir la lógica (fallar salvo perfil explícitamente descartable); `${JWT_CLAVE_FIRMA:?}` en el compose.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los servicios (validan contra el JWKS de identidad); backoffice
- Estándares y normas: ISO/IEC 25010 Seguridad (autenticidad); OWASP ASVS (autenticación multifactor); regla 90.3.4 (config que no falla al arrancar); habilitación de dinero real condicionada al dossier H15
- Esfuerzo estimado: chico

### IDCU-SEG-005 — Los servidores de recurso no validan emisor ni audiencia; el token de acceso no los lleva
- Clase / Severidad / Confianza: SEG / S3 / confirmado
- Ubicaciones: `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/ConfiguracionDelDecodificador.java:27-28` (punto de contacto, transversal); `servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/EmisorDeAcceso.java:112-121` (acceso sin `iss`/`aud`), `:155-157` (evidencia step-up con `iss`/`aud`, misma clave)
- Evidencia: `return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();` (sin `setJwtValidator`); el constructor del token de acceso no llama `.issuer(...)` ni `.audience(...)`.
- Cómo se comprobó: lectura. Refutación: `SesionDeLaPeticion.java:71-77` rechaza todo token sin claim `rol`, y la evidencia step-up no lo trae → hoy un token de evidencia no sirve como token de acceso. Por eso queda S3 (defensa en profundidad), no S1.
- Impacto: la separación entre tipos de token depende de que ningún otro token firmado con la misma clave lleve `rol`; cualquier token futuro firmado por identidad con ese claim sería aceptado por los catorce servicios.
- Causa de fondo: hallazgo previo `docs/auditoria-produccion/security-matrix.md:52` («JWT sin `iss`/`aud`») — **sigue vigente** en el commit auditado (la parte de clave efímera fue cerrada para `staging`/`production`, ver IDCU-SEG-004).
- Solución de fondo: emitir `iss` y `aud` en el token de acceso y validar ambos en el decodificador compartido.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los servicios; `plataforma/comun-web` (lote L5)
- Estándares y normas: ISO/IEC 25010 Seguridad; OWASP API Security Top 10 (API2 Broken Authentication); regla 98.6.1
- Esfuerzo estimado: chico

### IDCU-DOC-006 — El cliente de aprobación de retiros dice que no existe la bandeja que el archivo vecino consume
- Clase / Severidad / Confianza: DOC (también PISADA) / S4 / confirmado
- Ubicaciones: `apps/backoffice/src/app/rutas/operacion/retiros/dominio/cu11-aprobacion-de-retiro.ts:10-12,15-18`; `apps/backoffice/src/app/rutas/operacion/retiros/dominio/cu11-retiros-en-revision.ts:3-4,12`; `servicios/nucleo-financiero/src/main/resources/openapi/*.yaml:457-460`
- Evidencia: «El contrato NO publica un listado ni una consulta de órdenes en revisión: la pantalla trabaja con el identificador que llega en el aviso» vs `GET /billetera/retiros` («Bandeja de retiros que esperan la segunda aprobacion») usado en `cu11-retiros-en-revision.ts:12`. Además `cu11-aprobacion-de-retiro.ts` declara los tipos a mano (`:15-18`) mientras el vecino importa el cliente generado (`clientes/angular/nucleo-financiero/model/...`).
- Cómo se comprobó: lectura de ambos archivos y `grep -n retiros` del OpenAPI de nucleo-financiero.
- Impacto: comentario que miente y dos convenciones para el mismo contrato en la misma carpeta: si el OpenAPI cambia `EntradaAprobacionRetiro`, el tipo escrito a mano no se entera.
- Causa de fondo: dos agentes en momentos distintos (el listado se agregó después).
- Solución de fondo: usar los tipos del cliente generado y borrar el comentario obsoleto.
- Relación con el flujo nuevo: APROB (pantalla de la doble aprobación de retiros, ver §4.a)
- Conexiones y módulos afectados: nucleo-financiero (contrato)
- Estándares y normas: ISO/IEC 25010 Mantenibilidad; regla 96.4.1
- Esfuerzo estimado: chico

#### Tanda 2 — app móvil, pantallas de identidad (lote 4)

Rutas sin prefijo: relativas a `apps/movil/lib/pantallas/identidad/`. Peldaño: DISCOVERED (nada se ejecutó: Flutter no está instalado).

### IDCU-RESPALDO-007 — El tour promete que «un fondo de garantía cubre la cuota» de quien no paga
- Clase / Severidad / Confianza: RESPALDO / S2 / confirmado (el texto); la clasificación del fondo es DECISIÓN REQUERIDA (A-2/A-3)
- Ubicaciones: `apps/movil/lib/pantallas/identidad/textos.dart:42-46`; `apps/movil/lib/pantallas/identidad/laminas_del_tour.dart:44-50`
- Evidencia: `'Hay un fondo de garantía que cubre la cuota para que el turno se entregue igual.'`; etiqueta `'Un paraguas cubriendo la moneda de la cuota'`
- Cómo se comprobó: lectura completa de ambos archivos; búsqueda de «garant/cubr/respald» en el lote; el contrato local (`texto_del_contrato.dart`) no menciona fondo de garantía ni cobertura.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: promesa comercial de cobertura en la primera pantalla que ve cualquier persona. Choca con R-NUEVA-2 (la empresa no pone plata por quien no paga) si el fondo es de la empresa, y aun si es mutual promete «el turno se entrega igual», que la regla nueva ya no garantiza. Además contradice el contrato, que es silencioso: promesa sin respaldo contractual.
- Nota del orquestador: el subagente lo propuso S1. Se baja a S2 porque es un texto de pantalla, no un camino que mueva dinero (criterio S1 de la §10 del metaprompt). Además el texto describe el «fondo de garantía» mutual, que `docs/Arquitectura/ADR-051 Garantía del pozo, comisión y autogestión.md:57` declara **legado** («la reserva empresarial es el único respaldo; el fondo mutual … no cubre faltantes de pozo»): contradice a la vez R-NUEVA-2 y la decisión vigente.
- Causa de fondo: copy de producto escrito para el modelo anterior (fondo que cubre) sin trazabilidad a la regla de negocio vigente.
- Solución de fondo: retirar la lámina 4 y su arte, o reescribirla según la decisión de R-NUEVA-2 (qué pasa si alguien no aporta: mora + debido proceso, sin cobertura); alinear con el contrato de participación.
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: ninguna técnica; dominio garantía (ilustración `ArteFondoDeGarantia` en `packages/diseno_flutter`).
- Estándares y normas: 91.5 / R-NUEVA-2; 00 §1.6 (no cambiar la semántica de un requisito).
- Esfuerzo estimado: chico

### IDCU-APROB-008 — Todo el circuito de identidad del cliente asume aprobación humana de KYC como compuerta de dinero y pasanaku
- Clase / Severidad / Confianza: APROB / S2 / confirmado (en el cliente)
- Ubicaciones: `pantalla_identidad_requerida.dart:15-18,91-111,133-136`; `dominio/verificacion_diferida.dart:13-31`; `dominio/kyc_del_titular.dart:42-51`; `pantalla_estado_de_verificacion.dart:78-112`; `dominio/consultar_estado_de_cuenta.dart:8-49`; `boton_consultar_estado.dart:11-56`; `acciones_de_verificacion.dart:33-39`; `textos_de_verificacion.dart:6-21,42-44,56-58,72-81`; `texto_completar_kyc.dart:7-8,32-33`; `textos_del_perfil.dart:29-30`; `apps/movil/lib/proveedores/sesion.dart:149-159`; spec `identidad.yaml:350-352,458`
- Evidencia: `EstadoDeVerificacionEstadoEnum.EN_REVISION => 'En revisión humana'`; `'Apenas una persona los confirme vas a poder hacer esta operación.'`
- Cómo se comprobó: lectura de todo el lote + paths del spec (`resolverVerificacion`, «la aprobacion sigue siendo manual»).
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: si A-1 se resuelve como «el KYC entra en R-NUEVA-1», hay que rediseñar la compuerta, los estados mostrados, el sondeo y la activación por renovación de token; si queda fuera, el copy debe decirlo sin prometer plazos.
- Causa de fondo: el KYC está modelado como revisión humana del backoffice (decisión `resolverVerificacion`), el cliente solo refleja.
- Solución de fondo: DECISIÓN REQUERIDA A-1 (negocio/cumplimiento). Implementar la rama que se decida en el servidor; el cliente solo debe mapear `estadoDeCuenta` y no codificar «una persona».
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: `GET /usuarios/yo/perfil`, `GET /usuarios/{id}/verificacion`, `POST /sesion/refrescar`, backoffice.
- Estándares y normas: 00 §1.7 (ambigüedad registrada, no resuelta).
- Esfuerzo estimado: grande

### IDCU-HUECO-009 — El perfil transaccional (origen de fondos, actividad, monto) se pide y nunca se registra
- Clase / Severidad / Confianza: HUECO / FLUJO-FALTANTE / S2 / confirmado
- Ubicaciones: `dominio/cu01_registrar.dart:61-78` (cuerpo sin perfil); `dominio/envio_del_alta.dart:38-55`; `pantalla_verificar_identidad.dart:71-79` (verificación diferida: solo documento); `pasos_alta/catalogo_del_perfil.dart:16-18` («el día que el alta mande el perfil al servidor»); `pasos_alta/paso_perfil_transaccional.dart:97-110`; spec `identidad.yaml:1415-1418` (`EntradaRegistro additionalProperties: false`, sin perfil); test `apps/movil/test/identidad/estado_alta_test.dart:135-160` (lista blanca que excluye el perfil)
- Evidencia: `'aceptaContratos': contratosAceptados,` es el último campo; ningún `origenDeFondos/actividadEconomica/montoMensualEstimado` fuera del borrador
- Cómo se comprobó: lectura; `grep origenDeFondos` en `servicios/` solo aparece en `BorradorOnboarding*` de identidad, no en el alta ni en cumplimiento.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: la persona declara origen de fondos y actividad (exigencia UIF/ASFI para comparar contra lo operado) y el dato solo vive en un borrador temporal de onboarding; `cumplimiento.perfil_transaccional` / `declaracion_origen_fondos` no lo reciben desde la app. Declaración regulatoria perdida y un paso de UI que miente sobre su efecto.
- Causa de fondo: el contrato `EntradaRegistro` nunca incorporó el perfil y el test fija esa ausencia como correcta.
- Solución de fondo: decidir el dueño (cumplimiento) y su contrato; publicar evento o endpoint con el perfil (con moneda e importe decimal); o retirar el paso si no se va a usar.
- Relación con el flujo nuevo: ninguna directa (cumplimiento UIF, A-1)
- Conexiones y módulos afectados: identidad → cumplimiento (inexistente hoy).
- Estándares y normas: 98.1 (contrato), 90.2, 97.4.5.
- Esfuerzo estimado: medio

### IDCU-HUECO-010 — La declaración PEP se guarda solo en memoria y se pierde
- Clase / Severidad / Confianza: HUECO / S2 / confirmado
- Ubicaciones: `pantalla_verificacion_profunda.dart:104-117`; `dominio/estado_cuenta.dart:23-46`
- Evidencia: `onPressed: ... ref.read(pepProvider.notifier).declarar(esPep: _esPep!, detalle: _detalle.text)` — `PepNotifier` solo hace `state = EstadoPep(...)`, sin llamada de red ni navegación
- Cómo se comprobó: lectura; `grep pepProvider` en `lib` solo lo usa esta pantalla.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: la persona responde una pregunta de cumplimiento, toca «Confirmar respuesta» y no pasa nada; ni el servidor ni cumplimiento se enteran. Riesgo regulatorio (debida diligencia reforzada no disparada) y UX engañosa.
- Causa de fondo: pantalla construida antes de que existiera el endpoint; sin estado de error/éxito.
- Solución de fondo: endpoint de declaración PEP en cumplimiento con idempotencia y acuse; mientras no exista, ocultar la ruta `/identidad/verificacion-profunda`.
- Relación con el flujo nuevo: ninguna (A-1)
- Conexiones y módulos afectados: ninguna hoy.
- Estándares y normas: 95.2 (estados), 90.1.
- Esfuerzo estimado: medio

### IDCU-HUECO-011 — «Cambiar contraseña» no cambia nada y no avisa
- Clase / Severidad / Confianza: HUECO / SEG / S2 / confirmado
- Ubicaciones: `pantalla_contrasena.dart:92-104`; `dominio/estado_cuenta.dart:114-136` (`confirmar()` lanza `UnimplementedError` y nadie lo llama)
- Evidencia: `onPressed: valido ? () => ref.read(contrasenaProvider.notifier).actualizar(actual: _actual.text, nueva: _nueva.text) : null`
- Cómo se comprobó: lectura; `grep contrasenaProvider` solo aparece en esa pantalla.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: quien sospecha que su clave fue expuesta cree haberla cambiado. Además la contraseña actual y la nueva quedan en el estado de un `Notifier` global sin limpiarse.
- Causa de fondo: falta el endpoint en identidad (el comentario lo dice) y la UI se publicó igual.
- Solución de fondo: endpoint con verificación de la clave actual, rate limit y revocación de sesiones; hasta entonces ocultar la fila del perfil (`pantalla_perfil.dart:87-93`).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: identidad (inexistente).
- Estándares y normas: 90.1, 95.3.4, 00 §5.4.
- Esfuerzo estimado: medio

### IDCU-HUECO-012 — «Confirmar baja» solo cierra la hoja; el contrato promete cerrar la cuenta «desde la app»
- Clase / Severidad / Confianza: HUECO / S2 / confirmado
- Ubicaciones: `pantalla_baja.dart:50-67`; `dominio/estado_cuenta.dart:138-156` (`confirmarBaja` lanza `UnimplementedError`, no usado); `texto_del_contrato.dart:186` («puede cerrar su cuenta cuando quiera, desde la app»); test `apps/movil/test/widget/pantalla_baja_test.dart:8-27` (solo prueba cancelar)
- Evidencia: `onConfirmar: () => Navigator.of(context).pop(),`
- Cómo se comprobó: lectura.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: derecho contractual (cláusula 17.ª y Ley 453 citada) incumplido; la persona cree haberse dado de baja.
- Causa de fondo: endpoint inexistente; pantalla publicada como si existiera.
- Solución de fondo: endpoint de baja con precondiciones (saldo cero, sin cupos activos) validadas en servidor; o retirar la fila y la cláusula hasta tenerlo.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: identidad, núcleo financiero, grupos (precondiciones).
- Estándares y normas: 95.2, 00 §6.
- Esfuerzo estimado: medio

### IDCU-COHERENCIA-013 — Se acepta por id un contrato del servidor pero se muestra un texto y un tarifario fijos del cliente
- Clase / Severidad / Confianza: COHERENCIA / DUP-X / S2 / probable
- Ubicaciones: `texto_del_contrato.dart:24-28,226-260`; `cuerpo_del_contrato.dart:20`; `dominio/estado_contrato.dart:42-47`; `pantalla_contrato.dart:27-35`; `panel_de_aceptacion.dart:57-61` (comentario obsoleto); `avisos_de_sesion.dart:28`, `capa_de_bloqueo_vista.dart:155-156`
- Evidencia: `const String versionDelContrato = 'CTO-BILLETERA v1';` y `Comisión por cobro de turno: 0,30 % ... mínimo de Bs 10,00 y un máximo de Bs 50,00.` escritos en el cliente, mientras `aceptados` envía `vigentes.deTipo(...).id` del servidor
- Cómo se comprobó: lectura; la pantalla nunca compara versión/hash del vigente con el texto pintado.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: la aceptación auditada (id del servidor) puede corresponder a un texto distinto del que la persona leyó; un cambio de tarifario en servidor no actualiza lo mostrado. Parámetros de negocio (3 intentos, 5 min, 8 h, 30 días, 0,30 %) duplicados en el cliente.
- Causa de fondo: el endpoint de vigentes no devuelve (o no se usa) el cuerpo del contrato.
- Solución de fondo: servir el texto (o su URL firmada + hash) desde cumplimiento y pintar exactamente eso; bloquear aceptar si hash ≠ versión pintada.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: cumplimiento `GET /cumplimiento/contratos/vigentes`; tarifas.
- Estándares y normas: 91.1.5 (una sola fuente), 97.4.6.
- Esfuerzo estimado: medio

### IDCU-HUECO-014 — Fotos del expediente que fallaron se borran al salir y no hay camino para re-subir anverso, reverso ni selfie
- Clase / Severidad / Confianza: HUECO / S2 / probable
- Ubicaciones: `pantalla_subida_del_expediente.dart:101-110,157-175`; `dominio/seguimiento_del_alta.dart:62-71`; `dominio/kyc_del_titular.dart:28-40`; `pantalla_completar_kyc.dart:127-153`
- Evidencia: `dispose(){ ... addPostFrameCallback((_) { unawaited(_seguimientoDelAlta.liberarCapturas()); });` y «Continuar» habilitado con fotos `fallida`; `pendientesDeKyc` solo lista perfiles y fecha
- Cómo se comprobó: lectura del flujo completo (subida → continuar → perfil/completar).
- Impacto: expediente incompleto sin recuperación en la app; con revisión humana termina en rechazo o en soporte.
- Causa de fondo: `pendientesDeKyc` modela solo los faltantes «nuevos» (perfiles, fecha), no las cinco caras.
- Solución de fondo: derivar faltantes de `EstadoDeVerificacion.fotos` para las cinco caras y ofrecer recaptura; no borrar copias locales de caras fallidas.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `POST /usuarios/{id}/documentos`.
- Estándares y normas: 95.2.3.
- Esfuerzo estimado: chico

### IDCU-SEG-015 — Prueba de vida y cotejo son escenografía en el cliente
- Clase / Severidad / Confianza: SEG / DATOS / S2 / confirmado (en cliente); lo que hace el servidor no se verificó
- Ubicaciones: `dominio/ciclo_de_pose.dart:115-117`; `pasos_alta/pie_de_prueba_de_vida.dart:105-112`; `controlador_prueba_de_vida.dart:96-99`; `pasos_alta/paso_cotejo.dart:12-55`; `dominio/datos_del_alta.dart:92-104`; `texto_del_contrato.dart:76`
- Evidencia: `/// «Tomar ahora»: siempre disponible ...`; `FilaDeCotejo(campo: ..., declarado: datos.nombres, leido: datos.nombres)`
- Cómo se comprobó: lectura.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: el detector de pose se saltea con un toque; el «cotejo» compara el dato consigo mismo y siempre «coincide». El contrato le dice a la persona que hay «prueba de vida y cotejo». Datos simulados en producción presentados como verificación.
- Causa de fondo: OCR/liveness no contratados (hueco H-CLIENTE declarado en comentarios).
- Solución de fondo: quitar el cotejo hasta tener OCR real o rotularlo como «revisá tus datos»; la decisión de vida debe tomarla el servidor/proveedor; ajustar el contrato.
- Relación con el flujo nuevo: APROB (hoy lo compensa la revisión humana)
- Conexiones y módulos afectados: proveedor KYC (inexistente).
- Estándares y normas: 00 §2.1, 97.4.1.
- Esfuerzo estimado: medio

### IDCU-DINERO-016 — Monto mensual estimado como `double`, sin moneda en el contrato, 'Bs' literal
- Clase / Severidad / Confianza: DINERO / S3 / confirmado
- Ubicaciones: `pasos_alta/paso_perfil_transaccional.dart:81-88,107,163,170-173`; `dominio/modelo_alta.dart:111,156`; `dominio/notifier_alta.dart:104`; `dominio/borrador_alta.dart:45,111-114`; `dominio/modelo_del_alta.dart:72`
- Evidencia: `final double? montoMensualEstimado;`; `montoMensualEstimado: state.montoMensualEstimado?.toString()` (envía `"1500.0"`); formateador `RegExp(r'[0-9.]')` admite `1.2.3`
- Cómo se comprobó: lectura; spec `BorradorOnboarding.montoMensualEstimado: { type: string }` sin moneda.
- Impacto: importe declarado sin moneda y con representación binaria; `round()` al restaurar pierde decimales.
- Causa de fondo: falta tipo `Dinero` en el modelo del alta.
- Solución de fondo: usar el `Dinero` del cliente generado (existe `dinero.dart`) con `moneda`, `Decimal`/string validado.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: borrador de onboarding.
- Estándares y normas: 91.1.1, 91.1.2.
- Esfuerzo estimado: chico

### IDCU-CONTRATO-017 — Seis operaciones del spec se llaman por Dio crudo con comentarios obsoletos y casteos sin validar
- Clase / Severidad / Confianza: CONTRATO / DUP-INT / S3 / confirmado
- Ubicaciones: `dominio/cu01_registrar.dart:27-28,48-87`; `dominio/estado_cuenta.dart:6-17`; `dominio/verificacion_diferida.dart:9-12,34-76`; `dominio/cu02_actualizar_fecha_expiracion.dart:5-30`; `dominio/cu02_subir_foto.dart:61-83`; `dominio/foto_de_perfil.dart:20-75`; `dominio/datos_del_alta.dart:92-94`; spec `identidad.yaml:99,435-437,502-503,734-736,747-773`; barrel `clientes/dart/identidad/lib/aportaya_cliente_identidad.dart` (exporta `mi_perfil`, `fecha_expiracion_documento`, `entrada_registro`, `salida_registro`)
- Evidencia: `/// Va por Dio directo y no por el cliente generado: ese cliente todavía exige el documento y no conoce INCOMPLETO_KYC` — el spec actual ya declara `documento` opcional y `INCOMPLETO_KYC`.
- Cómo se comprobó: comparación de paths/schemas del spec y exports del barrel (el `src/` generado no está en el repo: `.gitignore:51`).
- Impacto: el cliente no se rompe en compilación cuando cambia el contrato; `MiPerfil` se pide tres veces por providers distintos; `r.data!` / `as String` revientan con TypeError ante una respuesta inesperada.
- Causa de fondo: deriva entre spec y cliente nunca reconciliada.
- Solución de fondo: regenerar cliente, usar `registrarUsuario`, `consultarMiPerfil`, `actualizarFechaExpiracionDocumento`, `iniciarVerificacion`, `subirDocumento`, `cambiarMiFotoDePerfil`; un solo provider de `MiPerfil`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todas las de identidad listadas arriba.
- Estándares y normas: 96.1.2, 98.1.3.
- Esfuerzo estimado: medio

### IDCU-COHERENCIA-018 — Vocabularios de tipo de documento y teléfono divergentes; coerción silenciosa a CI
- Clase / Severidad / Confianza: COHERENCIA / S3 / probable (latente: la UI solo ofrece CI)
- Ubicaciones: `dominio/cu01_registrar.dart:105-109`; `dominio/borrador_alta.dart:7-12,72-76`; spec `identidad.yaml:1387-1398` (`Documento.tipo: [CI, CEX, PASAPORTE]`) vs `:1038-1048` (`BorradorOnboarding.tipoDocumento: [CI, PASAPORTE, CARNET_EXTRANJERO]`, `telefonoE164: '^\+591[67]\d{7}$'`); `dominio/validaciones.dart:62-68` (`^\+591\d{8}$`)
- Evidencia: `'tipo': switch (tipo.toUpperCase()) { 'CEX' => 'CEX', 'PASAPORTE' => 'PASAPORTE', _ => 'CI' }` — un borrador restaurado trae `CARNET_EXTRANJERO` y se envía como `CI`.
- Cómo se comprobó: lectura + spec.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: un extranjero restaurado desde borrador quedaría registrado como CI; un teléfono `+5912…` pasa el alta y rompe el guardado del borrador (tragado por `catch (_)`).
- Causa de fondo: dos enums para el mismo concepto en el mismo contrato.
- Solución de fondo: unificar el enum en el spec; el cliente debe fallar, no adivinar, ante un valor desconocido.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: alta, borrador.
- Estándares y normas: 97.4.7, 98.1.3.
- Esfuerzo estimado: chico

### IDCU-HUECO-019 — El alta solo admite CI boliviano
- Clase / Severidad / Confianza: HUECO / REG / S3 / confirmado
- Ubicaciones: `pasos_alta/paso_documento.dart:93-166` (sin selector de tipo); `dominio/datos_del_alta.dart:13`; `dominio/validaciones.dart:89-95`; `pasos_alta/formateadores_del_alta.dart:3-6`; contra `texto_del_contrato.dart:76` y spec `Documento.tipo`
- Evidencia: `'Escribí un número de CI de entre 5 y 10 dígitos.'` y `this.tipoDocumento = 'CI'`
- Cómo se comprobó: lectura.
- Impacto: residentes con CEX o pasaporte no pueden verificarse; el contrato y el spec lo prevén.
- Causa de fondo: alcance recortado sin decisión registrada.
- Solución de fondo: selector de tipo con validaciones por tipo, o decisión de negocio que lo excluya.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: alta, verificación diferida.
- Estándares y normas: 00 §1.6.
- Esfuerzo estimado: medio

### IDCU-FRAGIL-020 — Catch que tragan y estados que quedan colgados
- Clase / Severidad / Confianza: FRAGIL / S3 / confirmado (por lectura)
- Ubicaciones: `dominio/verificacion_correo.dart:186-188` (`catch (_) {}`); `dominio/subida_del_expediente.dart:122-156` (solo `on DioException`: un `FileSystemException` de `readAsBytes` o un `TypeError` de `as String` deja la cara en «subiendo» y el botón Continuar bloqueado); `pantalla_camara.dart:70-97` (`_tomarFoto` sin try: `_tomando` queda `true`); `pantalla_identidad_requerida.dart:51-63` (`onActivar` sin try); `pantalla_perfil.dart:123-129` (sin finally); `dominio/notifier_alta.dart:117-124` (`unawaited(guardarBorrador())`)
- Evidencia: `} on DioException catch (e) {` como única rama en `_subirUna`
- Cómo se comprobó: lectura.
- Impacto: pantallas trabadas sin salida, y fallos de borrador invisibles (la persona pierde el rescate sin saberlo).
- Causa de fondo: manejo de errores por tipo de excepción esperado, no por resultado.
- Solución de fondo: `on Object` con estado `fallida` accionable; registrar (sin PII) el fallo de borrador.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: subida de fotos, borrador.
- Estándares y normas: 95.2.1, 96.3.
- Esfuerzo estimado: chico

### IDCU-DEFECTO-021 — Clave de idempotencia de «completar KYC» nunca rota
- Clase / Severidad / Confianza: DEFECTO / S3 / probable
- Ubicaciones: `pantalla_completar_kyc.dart:80-88`; contrastar con la rotación correcta en `dominio/subida_del_expediente.dart:187-191`
- Evidencia: `formularioId: 'kyc-completar-${cara.name}',` sin `cerrar` previo
- Cómo se comprobó: lectura de `idempotenciaProvider.claveDe` usado en `cu02_subir_foto.dart:70-72`.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: tras un fallo con respuesta (p. ej. 422 por imagen inválida), la foto nueva viaja con la misma clave; el servidor puede reproducir el resultado anterior o rechazar por cuerpo distinto.
- Causa de fondo: patrón de rotación no aplicado en esta pantalla.
- Solución de fondo: rotar la clave al repetir la foto (igual que `rotarClaveParaRepetir`).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `POST /usuarios/{id}/documentos`.
- Estándares y normas: 96.3.3.
- Esfuerzo estimado: chico

### IDCU-SEG-022 — Credenciales: backup Android por defecto y candado biométrico solo visual
- Clase / Severidad / Confianza: SEG / CONFIG / S3 / probable
- Ubicaciones: `apps/movil/android/app/src/main/AndroidManifest.xml:10` (sin `android:allowBackup="false"` ni `dataExtractionRules`); `apps/movil/lib/infraestructura/android/almacen_seguro_android.dart:9`; `capa_de_bloqueo_biometria.dart:63-67`; `apps/movil/lib/proveedores/sesion.dart:48-49`
- Evidencia: `const FlutterSecureStorage(aOptions: AndroidOptions())`; comentario «Confirma identidad local para quitar el candado; no autentica»
- Cómo se comprobó: lectura y grep del manifiesto.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: (a) el contenedor cifrado de tokens entra en el backup automático (las claves del Keystore no viajan: al restaurar falla la lectura y la sesión se pierde; MASVS-STORAGE recomienda excluirlo); (b) los tokens se leen sin biometría: el candado protege la UI, no la credencial (MASVS-AUTH-2). Lo positivo: iOS usa `unlocked_this_device` y nada va a SharedPreferences.
- Causa de fondo: configuración por defecto.
- Solución de fondo: excluir el almacén del backup; evaluar ligar el refresh token a una clave del Keystore con autenticación de usuario.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: almacén seguro.
- Estándares y normas: 95.6.2, OWASP MASVS-STORAGE-1 / AUTH-2.
- Esfuerzo estimado: chico

### IDCU-RESILIENCIA-023 — Refresco de sesión sin timeouts y con dos refrescadores no coordinados
- Clase / Severidad / Confianza: RESILIENCIA / S3 / probable
- Ubicaciones: `apps/movil/lib/dominio/cliente.dart:88-90`; `apps/movil/lib/dominio/renovar_acceso.dart:12-16`
- Evidencia: `Dio(BaseOptions(baseUrl: dio.options.baseUrl))` sin `connectTimeout/receiveTimeout`, y `Refrescador(...)` nuevo en cada «Activar mi cuenta»
- Cómo se comprobó: lectura (Dio no aplica timeout si no se configura).
- Impacto: «Activar mi cuenta» / «Consultar estado» pueden quedar cargando indefinidamente; un refresco del interceptor y otro del botón en paralelo pueden presentar el mismo refresh token rotado (detección de reúso → sesión cerrada).
- Causa de fondo: se copió `BaseOptions` sin los timeouts del Dio principal.
- Solución de fondo: compartir el `Refrescador` single-flight y heredar timeouts.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `POST /sesion/refrescar`.
- Estándares y normas: 98.2.1.
- Esfuerzo estimado: chico

### IDCU-UX-A11Y-024 — Errores crudos y estados vacíos mudos
- Clase / Severidad / Confianza: UX-A11Y / S3 / confirmado
- Ubicaciones: `textos_de_verificacion.dart:94-101` (`_ => codigo`: el `motivoRechazo` libre del servidor, `maxLength 160`, se muestra tal cual; el mapeo de 4 códigos «igual que Atlas» no tiene fuente en el spec); `capa_de_bloqueo_vista.dart:122-126` (fallo biométrico del sistema en el texto); `pantalla_estado_de_verificacion.dart:50` y `pantalla_identidad_requerida.dart:41` (`mensajeVacio: ''`); `pantalla_completar_kyc.dart:110` (vacío = «La revisión ya terminó»)
- Evidencia: `'No se pudo abrir la huella o el PIN del teléfono ($falloBiometrico).'`
- Cómo se comprobó: lectura; spec `EstadoDeVerificacion.motivoRechazo` es string libre.
- Impacto: mensajes técnicos al usuario; vacío sin orientación.
- Causa de fondo: catálogo de motivos no codificado en el contrato.
- Solución de fondo: enum de motivos en el spec y texto genérico accionable para valores desconocidos.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `GET /usuarios/{id}/verificacion`.
- Estándares y normas: 95.2.2, 95.2.3, 97.5.2.
- Esfuerzo estimado: chico

### IDCU-HUECO-025 — «Confiar en este dispositivo por 30 días» no se envía; recuperación de contraseña inexistente
- Clase / Severidad / Confianza: HUECO / S3 / confirmado
- Ubicaciones: `pantalla_dispositivos.dart:45-58`; `dominio/estado_sesion.dart:155-156`; `sesion_pie.dart:10-28`; `textos.dart:145-147`; contrato `texto_del_contrato.dart:105`
- Evidencia: `onChanged: notifier.elegirConfianza` solo cambia estado local; `'Todavía no se puede recuperar desde la app.'`
- Cómo se comprobó: lectura.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: casilla con efecto falso; quien olvida la clave queda fuera de su dinero sin flujo (el contrato dice que se desbloquea «por el procedimiento de recuperación que la app indique»).
- Causa de fondo: endpoints ausentes.
- Solución de fondo: implementar o retirar; recuperación con token de un solo uso y sin enumeración.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: identidad.
- Estándares y normas: 90.1.11.
- Esfuerzo estimado: medio

### IDCU-FRAGIL-026 — Rutas que revientan con enlace profundo o sin `extra`
- Clase / Severidad / Confianza: FRAGIL / S3 / confirmado (por lectura, no ejecutado)
- Ubicaciones: `rutas_de_entrada.dart:96,104,113,120`
- Evidencia: `PantallaDeCamara(cara: state.extra! as CaraDelCarril)`; `usuarioId: state.uri.queryParameters['usuario']!`
- Cómo se comprobó: lectura.
- Impacto: `/registro/subida` o `/registro/estado` sin `usuario`, o reapertura de la app en `/registro/camara`, lanzan excepción en build.
- Causa de fondo: sin validación de parámetros de ruta.
- Solución de fondo: validar y redirigir a una pantalla de estado.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna.
- Estándares y normas: 95.2.
- Esfuerzo estimado: chico

### IDCU-UX-A11Y-027 — Botón de solo icono sin nombre accesible
- Clase / Severidad / Confianza: UX-A11Y / S3 / confirmado
- Ubicaciones: `pasos_alta/cabecera_de_camara.dart:24-27` (usado por `encabezado_y_consejos_de_camara.dart:24`)
- Evidencia: `IconButton(icon: const Icon(Icons.arrow_back, color: Paleta.white), onPressed: onVolver)` sin `tooltip`
- Cómo se comprobó: lectura (comparar con `barra_superior_de_vida.dart:28-29` y `visor_ampliado_de_captura.dart:76-77`, que sí lo tienen).
- Impacto: el lector de pantalla anuncia «botón» sin nombre en la cámara del carnet.
- Causa de fondo: ver solución.
- Solución de fondo: `tooltip: 'Volver'`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna.
- Estándares y normas: 95.4.1.
- Esfuerzo estimado: chico

### IDCU-PRUEBAS-028 — Los tests fijan los huecos como comportamiento correcto
- Clase / Severidad / Confianza: PRUEBAS / S3 / confirmado
- Ubicaciones: `apps/movil/test/identidad/estado_alta_test.dart:135-160` (lista blanca del alta sin perfil transaccional); `apps/movil/test/widget/pantalla_baja_test.dart:8-27` (solo cancelar); ausencia de tests para `pepProvider`, `contrasenaProvider`, `confiarEnDispositivo` (grep sin resultados); goldens con `skip: reutilizaH8 && _omitirH8EnActualizacion` (`test/goldens/h10_alta_*_visual_test.dart`, p. ej. `h10_alta_actividad_visual_test.dart:20,130`)
- Evidencia: `expect(body.keys.toSet().difference(permitidas), isEmpty);`
- Cómo se comprobó: grep dirigido y lectura de los rangos citados (no se leyeron completos los 112 archivos de test).
- Impacto: suites verdes que blindan la pérdida de la declaración y las acciones falsas.
- Causa de fondo: ver solución.
- Solución de fondo: tests de contrato que exijan el envío o la ausencia deliberada con decisión registrada.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: —
- Estándares y normas: 80.2, 80.5.4.
- Esfuerzo estimado: chico

### IDCU-DEFECTO-029 — El sondeo del estado KYC se rinde a los 40 s frente a una revisión humana
- Clase / Severidad / Confianza: DEFECTO / S4 / confirmado
- Ubicaciones: `dominio/estado_de_verificacion.dart:21-22,79-81,91-93`
- Evidencia: `_intervalo = Duration(seconds: 2); _maximoDeIntentos = 20;`
- Impacto: la pantalla deja de actualizar sin decirlo; mitigado por «Actualizar estado».
- Causa de fondo: ver solución.
- Solución de fondo: backoff largo o push; avisar cuando se detiene.
- Relación con el flujo nuevo: APROB
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad.
- Esfuerzo estimado: chico

### IDCU-DUP-INT-030 — Código muerto y duplicado
- Clase / Severidad / Confianza: DUP-INT / CLEAN / S4 / confirmado
- Ubicaciones: `dominio/modelo_del_alta.dart:1-126` (copia divergente de `EstadoAlta`/`PasoAlta`, sin importadores); `dominio/carnet_de_prueba.dart:55-82` (`generarCapturasDePrueba` sin uso en `lib`; viaja en el binario aunque bloqueado por `kReleaseMode`); `pasos_alta/errores_paso_datos.dart:3-44` (sin uso; `paso_datos.dart:67-89` reimplementa); `dominio/estado_cuenta.dart:48-86,138-156` (`perfilProvider`, `bajaProvider` sin uso); `textos_de_verificacion.dart:6-7`, `textos_de_captura.dart:21` (textos sin uso); `pantalla_bienvenida.dart:9-12` y `panel_de_aceptacion.dart:57-61` (comentarios obsoletos)
- Impacto: confusión sobre cuál `EstadoAlta` rige; superficie de dev en release.
- Causa de fondo: ver solución.
- Solución de fondo: borrar.
- Relación con el flujo nuevo: ninguna
- Evidencia: ver ubicaciones.
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad.
- Esfuerzo estimado: chico

### IDCU-CLEAN-031 — Literales de tamaño y textos fuera de los archivos de textos
- Clase / Severidad / Confianza: CLEAN / S4 / confirmado
- Ubicaciones: tamaños: `pasos_alta/bloque_de_formulario.dart:26` (14), `avisos_de_sesion.dart:46` (16), `pasos_alta/tarjeta_de_confianza.dart:24` (18), `pantalla_pautas_de_fotos.dart:158` (20), `pasos_alta/miniaturas_de_vida.dart:80-81,88` (76/34), `capa_de_bloqueo_vista.dart:83,92` (420/88), `banda_de_marca.dart:62` (148), `pantalla_subida_del_expediente.dart:122` (112), `lamina_del_tour.dart:86`, `pantalla_portada.dart:67` (320); textos: `capa_de_bloqueo_vista.dart:99,113,134,150-156`, `avisos_de_sesion.dart:28-30`, `pantalla_pautas_de_fotos.dart:29,49,56,63-64`, `pasos_alta/opciones_de_captura_rechazada.dart:30,43`, `pasos_alta/hoja_antes_de_escanear.dart:34-57`, `pantalla_completar_kyc_de_sesion.dart:22,31`, `pantalla_subida_del_expediente.dart:126-127`, `paso_contrato_de_alta.dart:25-28`, `pasos_alta/canal_de_verificacion.dart:30-32`, `pantalla_verificacion_profunda.dart:52`, `pantalla_bienvenida.dart:53` (`'BOB'`), `pasos_alta/paso_perfil_transaccional.dart:163` (`'Bs'`)
- Impacto: tokens y textos se desincronizan; parámetros de negocio (8 h, 5 min) escritos en la UI.
- Causa de fondo: ver solución.
- Solución de fondo: tokens de `aportaya_diseno` y archivos de textos.
- Relación con el flujo nuevo: ninguna
- Estándares y normas: 95.1.5.
- Evidencia: ver ubicaciones.
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Esfuerzo estimado: chico

### IDCU-UX-A11Y-032 — Movimiento no condicionado a «reducir movimiento» en dos puntos
- Clase / Severidad / Confianza: UX-A11Y / S4 / confirmado
- Ubicaciones: `dominio/comportamiento_de_pantalla_del_alta.dart:54-59` (`Scrollable.ensureVisible(duration: Movimiento.entrada)` sin `duracion(context, …)`); `fila_de_subida.dart:95-98` (`LinearProgressIndicator` indeterminado)
- Impacto: animación con movimiento reducido activo.
- Causa de fondo: ver solución.
- Solución de fondo: usar `duracion(context, …)`.
- Relación con el flujo nuevo: ninguna
- Estándares y normas: 95.5.1.
- Evidencia: ver ubicaciones.
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Esfuerzo estimado: chico

### IDCU-FRAGIL-033 — La cámara devuelve dos formas distintas
- Clase / Severidad / Confianza: FRAGIL / CONTRATO interno / S4 / confirmado
- Ubicaciones: `pantalla_camara.dart:23-25,96,110`; `pasos_alta/sacar_captura.dart:85`; `rutas_de_entrada.dart:89-91` (documenta un `'prueba'` que ya no existe)
- Evidencia: `Navigator.of(context).pop(evaluacion.captura)` vs `pop({widget.cara: captura})`
- Impacto: contrato `Object?` difuso entre pantallas; cualquier consumidor nuevo se equivoca.
- Causa de fondo: ver solución.
- Solución de fondo: un único tipo de resultado.
- Relación con el flujo nuevo: ninguna
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad.
- Esfuerzo estimado: chico

### IDCU-TRAZA-034 — `tokenDeAltaVenceEn` ignorado y provider KYC sin invalidación por sesión
- Clase / Severidad / Confianza: TRAZA / SEG / S4 / confirmado
- Ubicaciones: `dominio/cu01_registrar.dart:84-87` (no lee `tokenDeAltaVenceEn`, spec `identidad.yaml:1497`); `dominio/seguimiento_del_alta.dart:19-20`; `dominio/kyc_del_titular.dart:23-26` (`FutureProvider.family` sin `autoDispose` ni `generacionDeSesionProvider`)
- Impacto: tras vencer el token de alta, subida/consulta dan 403 genérico; caché de KYC que sobrevive al cambio de sesión del mismo id.
- Causa de fondo: ver solución.
- Solución de fondo: usar el vencimiento para ofrecer «ingresá para continuar»; `autoDispose` + generación.
- Relación con el flujo nuevo: ninguna
- Evidencia: ver ubicaciones.
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad.
- Esfuerzo estimado: chico

### IDCU-REG-035 — Validaciones de negocio duplicadas en el cliente
- Clase / Severidad / Confianza: REG / S4 / confirmado
- Ubicaciones: `pasos_alta/paso_cuenta.dart:80-98` (clave no contiene celular/documento, «las mismas dos comprobaciones que hace el servidor»); `dominio/validaciones.dart:70-75` (edad por `inDays / 365.25`, falla en el cumpleaños 18); `pasos_alta/paso_datos.dart:141` (otra regla de 18 años por fecha)
- Impacto: dos implementaciones de `PoliticaDeClave` y de mayoría de edad que pueden divergir; error de borde en el día del cumpleaños.
- Causa de fondo: ver solución.
- Solución de fondo: comparar por fecha civil; considerar exponer la política desde el servidor.
- Relación con el flujo nuevo: ninguna
- Evidencia: ver ubicaciones.
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad.
- Esfuerzo estimado: chico

#### Tanda 3 — esquema, contratos, catálogos y retiros con doble aprobación (lote 3)

Peldaño: DISCOVERED (nada se ejecutó contra una base). Fichas del subagente; S1/S2 re-abiertos por el orquestador donde se indica.

### IDCU-FLUJO-FALTANTE-036 — Un retiro AUTORIZADO nunca se instruye al proveedor en producción
- Clase / Severidad / Confianza: FLUJO-FALTANTE / S2 / confirmado (grep sin llamadores en main)
- Ubicaciones: `servicios/nucleo-financiero/.../aplicacion/CU11RetirarSaldo.java:107-136`; `.../aplicacion/RetirosConProveedor.java:66,92`; `.../aplicacion/CU11InstruirRetiro.java:38-67`; `docs/catalogo-de-flujos/recarga-y-retiro.yaml:64-73`
- Evidencia: catálogo: "H8-01 · Ningún trabajo, endpoint ni consumidor llama a RetirosConProveedor.despachar ni a CU11RetirarSaldo.instruirPago en producción".
- Cómo se comprobó: `grep -rn "\.despachar(|instruirPago|retiro_autorizado"` en `src/main`: sólo definiciones y tests.
- Nota del orquestador: propuesto S1; se baja a S2 porque no crea, pierde ni duplica dinero: lo deja retenido sin salida (fallo funcional en un flujo en uso). Ya registrado por el propio repo como H8-01 con decisión pendiente A9 (`python3 scripts/validar_flujos.py` → «AVISO · recarga-y-retiro.yaml: paso `instruir-pago` SIN CABLEAR», `evidencia/validar_flujos.txt`). Re-verificado: `grep -rn "instruirPago\|\.despachar(" servicios --include=*.java | grep -v /test/` → solo la definición `CU11RetirarSaldo.java:107` y comentarios.
- Impacto: la orden queda `AUTORIZADA` con el saldo retenido indefinidamente; la persona no recibe su plata. Con la regla R-NUEVA-1 el despacho tampoco puede depender de un operador.
- Causa de fondo: dos implementaciones del despacho, ninguna cableada a un disparador.
- Solución de fondo: un único despacho (`CU11InstruirRetiro` + `RetirosConProveedor`) disparado por el consumidor de `retiro_autorizado` (outbox) o un job con ShedLock; borrar `instruirPago`.
- Relación con el flujo nuevo: FLUJO-QR (salida de dinero sin humano)
- Conexiones y módulos afectados: outbox nucleo_financiero, proveedor de retiros
- Regla: 91.3, 98.3.2
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DINERO-037 — El titular puede liberar la retención de su propio retiro mientras la orden sigue viva
- Clase / Severidad / Confianza: DINERO / S2 / probable (falta una prueba de integración que libere la retención de una orden AUTORIZADA y luego confirme el pago)
- Ubicaciones: `.../web/BilleteraController.java:186-200` (`cerrarRetencion`, `@Permiso("BILLETERA_OPERAR")`); `.../aplicacion/CU13RetenerSaldo.java:180-245`; `.../aplicacion/SolicitudDeRetiro.java:126-136` (retención con `referenciaTipo ORDEN_RETIRO`)
- Evidencia: `cerrar(...)` sólo hace `exigirTitular(...)` y `cerrarDentroDe(...)`; no mira `referencia_tipo`.
- Cómo se comprobó: lectura del camino `cerrarRetencion → cu13.liberar → cerrar`.
- Nota del orquestador: re-verificado `CU13RetenerSaldo.java:179-214`: `liberar` → `cerrar` solo exige titularidad (`exigirTitular`) y estado abierto; no mira a qué orden pertenece la retención. Se baja de S1 a S2 porque hoy el pago al proveedor no está cableado (IDCU-FLUJO-FALTANTE-036): el doble gasto requiere ese camino. Pasa a S1 el día que se cablee.
- Impacto: con la orden `EN_REVISION`/`AUTORIZADA`/`EN_PROCESO`, liberar devuelve el importe al disponible; si el proveedor luego paga, `confirmarPago` falla en `ejecutarDentroDe` (retención ya cerrada) y el libro no registra la salida: dinero fuera de custodia y saldo gastable dos veces.
- Causa de fondo: el endpoint genérico de retenciones no distingue retenciones gobernadas por otro agregado.
- Solución de fondo: prohibir cerrar por API una retención con `referencia_tipo` (sólo el dueño del agregado la cierra); CHECK/trigger en `retencion_saldo` que impida LIBERADA mientras la orden referida no esté `RECHAZADA`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: retencion_saldo, orden_retiro
- Regla: 91.1, 96.3.2
- Esfuerzo estimado: chico
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-038 — Las políticas de fila dan ESCRITURA al titular sobre sus roles, su KYC, su reputación, su PEP y su usuario
- Clase / Severidad / Confianza: SEG / S2 / confirmado (lectura del generador de políticas)
- Ubicaciones: `sql/40_reglas/restricciones.sql:1362-1395` (`visibles_por_titular` incluye `asignacion_rol`, `verificacion_kyc`, `reputacion_usuario`, `perfil_financiero`, `credencial_acceso`, `factor_mfa`, `consentimiento`, `token_verificacion`, `declaracion_origen_fondos`, `aceptacion_contrato`), `:1446-1453` (`cond_escritura := cond` salvo `lectura_por_titular`), `:1468-1474` (`pol_usuario_titular` WITH CHECK `id = fn_seg_usuario_actual()`)
- Evidencia: comentario `'asignacion_rol', -- ... Se leen, no se escriben`, pero sólo `organizador, participante, movimiento_billetera, solicitud_ingreso` quedan de sólo lectura.
- Cómo se comprobó: lectura de `fn_seg_aplicar_rls`.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura de `sql/40_reglas/restricciones.sql` / `application.yml` / grep).
- Impacto: la base, que ADR-021 declara última barrera, permite a una sesión PARTICIPANTE insertarse una `asignacion_rol` propia (sólo la frena `ck_asignacion_no_autoasignada` si `otorgada_por` = él mismo; con otro `otorgada_por` pasa), aprobarse su `verificacion_kyc`, subir su `reputacion_usuario`, cambiar `es_pep`, o editar `usuario.nivel_kyc/estado`. Hoy lo impide sólo el código Java.
- Causa de fondo: el régimen se decide por lista de visibilidad, no por operación.
- Solución de fondo: separar políticas `FOR SELECT` y `FOR INSERT/UPDATE`; mover a `lectura_por_titular` todo lo que el titular no debe escribir; columnas sensibles de `usuario` con `GRANT UPDATE (col)` restringido.
- Relación con el flujo nuevo: APROB (KYC autoaprobado sería el atajo si se elimina la revisión)
- Regla: 90.1.2, 90.1.7, 96.5
- Esfuerzo estimado: medio
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DEFECTO-039 — `ck_reporte_segregacion` se salta con `generado_por` NULL y no exige `revisado_por`
- Clase / Severidad / Confianza: DEFECTO / S2 / confirmado (semántica SQL: CHECK que evalúa NULL se acepta)
- Ubicaciones: `sql/40_reglas/restricciones.sql:1512-1516`; `sql/20_claves/12_cumplimiento_asfi.sql:485` (`generado_por ... ON DELETE SET NULL`); `docs/CasosDeUso/CU-43 ...md:40-41`
- Evidencia: `estado <> 'ENVIADO' OR (aprobado_por IS NOT NULL AND aprobado_por <> generado_por)`; el mismo archivo documenta el problema en R-SEG-02 ("un CHECK que evalúa a NULL se acepta").
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura de `sql/40_reglas/restricciones.sql` / `application.yml` / grep).
- Impacto: un reporte UIF generado por proceso (sin `generado_por`) se envía con un solo par de ojos; CU-43 exige revisado y aprobado distintos del generador.
- Solución de fondo: `generado_por IS NOT NULL` o `aprobado_por IS DISTINCT FROM generado_por`, y la misma regla para `revisado_por`.
- Relación con el flujo nuevo: APROB (A-1)
- Regla: 91.2, 90.5
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-HUECO-040 — R-UIF-09 (diligencia vigente) y R-CON-06 (contrato aceptado) no los invoca ninguna operación
- Clase / Severidad / Confianza: HUECO / REG / S2 / confirmado (grep repo-wide `.sql/.java/.kt`)
- Ubicaciones: `sql/40_reglas/restricciones.sql:1089-1100` (`fn_uif_exigir_ddd`), `:1197-1210` (`fn_con_exigir_contrato`); sólo llamadas en `servicios/cumplimiento/src/test/.../CU02Test.java:97`, `CU05Test.java:104`, `CU06Test.java:118`
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura de `sql/40_reglas/restricciones.sql` / `application.yml` / grep).
- Impacto: se opera sin debida diligencia ni contrato de adhesión vigentes aunque el modelo diga "denegar"; además los contratos sembrados están en `BORRADOR` (`11-contratos-de-adhesion.sql:62-67`), así que si se cableara hoy nadie podría operar.
- Solución de fondo: invocarlas desde los triggers de las operaciones de dinero (o desde `fn_lim_evaluar`) y sembrar contratos reales.
- Relación con el flujo nuevo: ninguna
- Regla: 91.2 (invariante en la base)
- Esfuerzo estimado: medio
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-DEFECTO-041 — El trigger de encaje bloquea también el pago de órdenes ya en curso y no filtra por moneda
- Clase / Severidad / Confianza: DEFECTO / DINERO / S2 / confirmado (lectura)
- Ubicaciones: `sql/40_reglas/restricciones.sql:530-555`; `.../infraestructura/OrdenRetiroRepositorio.java:264-278`
- Evidencia: comentario "Las órdenes ya autorizadas siguen su curso", pero `tg_retiro_encaje BEFORE INSERT OR UPDATE OF estado` sólo exceptúa `BORRADOR`/`RECHAZADA`; la consulta mira todas las custodias, mientras Java filtra `WHERE cc.moneda = ?`.
- Impacto: con encaje roto en cualquier moneda, `EN_PROCESO→PAGADA` falla aunque el proveedor ya pagó: el libro no refleja la salida (descuadre custodia vs mayor) y la retención queda abierta; Java y base dan veredictos distintos.
- Solución de fondo: aplicar sólo a `INSERT` y a la transición hacia `AUTORIZADA`, y filtrar por la moneda de la orden.
- Relación con el flujo nuevo: ninguna
- Regla: 91.2, COHERENCIA
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-DINERO-042 — El pago del retiro no registra la línea del costo
- Clase / Severidad / Confianza: DINERO / S2 / probable
- Ubicaciones: `.../aplicacion/ResolucionDeRetiro.java:103-115`; `.../aplicacion/SolicitudDeRetiro.java:116`; `docs/CasosDeUso/CU-11 Retirar saldo.md:47-53`
- Evidencia: dos patas `Pata.debito(cuenta, orden.solicitado())` / `Pata.credito(cuentaPuente, orden.solicitado())`; neto = monto − costo, pero el costo no va a ninguna cuenta de ingreso; CU-11 exige `cargo_comision` y asiento.
- Impacto: el puente de custodia recibe el bruto y paga el neto; la diferencia no tiene línea (91.3.4), la conciliación de custodia acumula un residuo inexplicado.
- Solución de fondo: tres patas (usuario→puente por neto, usuario→ingreso por costo) o asiento de comisión en la misma transacción.
- Relación con el flujo nuevo: ninguna
- Regla: 91.3.4, 91.1.5
- Esfuerzo estimado: medio
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-APROB-043 — Umbral de doble aprobación sin moneda, sin procedencia, por operación y decidido por el controlador
- Clase / Severidad / Confianza: APROB / S2 / confirmado
- Ubicaciones: `servicios/nucleo-financiero/src/main/resources/application.yml:58-61`; `.../web/FondeoDeLaBilletera.java:71,182`; `sql/40_reglas/restricciones.sql:1529-1535`; `ADR-049:56`
- Evidencia: `doble-aprobacion-desde: 5000.00` ("el número se mueve sin desplegar" — falso: es propiedad Spring); `monto.monto().compareTo(desdeCuandoSonDosFirmas) >= 0` sin mirar la moneda.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura de `sql/40_reglas/restricciones.sql` / `application.yml` / grep).
- Impacto: 5000 BOB y 5000 USD son lo mismo; fraccionar en retiros < 5000 evita la segunda firma; el CHECK de la base sólo actúa si la app puso `requiere_doble_aprobacion=TRUE`, así que no es una "segunda capa independiente" para el umbral. Parámetro financiero sin fuente (97.5.4).
- Solución de fondo: umbral en catálogo versionado con moneda y procedencia; regla acumulada por ventana; que la base derive el flag (trigger) del catálogo. Si A-1 elimina la aprobación, reemplazar por límites por ventana + enfriamiento + step-up ligado a la operación.
- Relación con el flujo nuevo: APROB (A-1 decisión requerida)
- Regla: 91.1.2, 97.5.4
- Esfuerzo estimado: medio
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-SEG-044 — `borrador_onboarding` guarda el número de documento en claro y el monto como texto
- Clase / Severidad / Confianza: SEG / DATOS / S2 / confirmado
- Ubicaciones: `sql/15_infra/borradores.sql:5-24`; `servicios/identidad/.../BorradorOnboardingRepositorio.java:44,103,113,177`; `scripts/generar_ddl.py:1195-1236`
- Evidencia: `numero_documento VARCHAR(30)`, `correo VARCHAR(150) PRIMARY KEY`, `monto_mensual_estimado TEXT` sin moneda.
- Impacto: el número que `documento_identidad` cifra y hashea con pimienta queda en claro por hasta 7 días en otra tabla (R-SEG-01); PII como PK; importe sin tipo ni moneda. Además la tabla no está en el `.puml` (segunda fuente de verdad, 97.1.1).
- Solución de fondo: cifrar con la misma llave versionada o no persistir el número en el borrador; PK UUID; monto DECIMAL + moneda; modelar en el `.puml`.
- Relación con el flujo nuevo: ninguna
- Regla: 90.2, 91.1.1-2, 97.1.1
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-DATOS-045 — Seeds de desarrollo con datos personales de apariencia real
- Clase / Severidad / Confianza: DATOS / S2 / probable (no se puede verificar identidad; no se reproducen los valores)
- Ubicaciones: `sql/61_dev/15-usuarios-dev.sql:7-9,52,54` (y su fuente `seeders/dev/15-usuarios-dev.json`)
- Evidencia: nombre y apellido, fecha de nacimiento, teléfono y dos correos (uno institucional de estudiante, uno personal) que no son sintéticos ni `.invalid`. Valores omitidos a propósito.
- Impacto: viola 97.6.1 / 00.2.3; queda en el historial de git y en cualquier base dev.
- Solución de fondo: reemplazar por datos sintéticos (`*.invalid`, `+5917000xxxx`) y purgar del historial si se decide.
- Relación con el flujo nuevo: ninguna
- Regla: 97.6.1, 90.2.3
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-REG-046 — Parámetros regulatorios y de riesgo activos sin confirmación ni procedencia completa
- Clase / Severidad / Confianza: REG / DATOS / S2 / confirmado
- Ubicaciones: `sql/60_semillas/06-umbrales-uif.sql:5-28` (+ `seeders/minimos/06-umbrales-uif.json`: `"estado": "confirmar vigencia con legal"`); `07-reportes-regulatorios.sql:4-15` (`"confirmar plazos con legal"`); `08-gobierno-y-licencia.sql:42-58` (matriz y tipologías `activa TRUE`, `aprobada_por` NULL, `vigente_desde current_date`); `19-reportes-y-retencion.sql:56-68` (`PEP ... >= 5000`)
- Evidencia: `base_normativa 'Instructivo EIF ...'` (instructivo para Entidades de Intermediación Financiera; aplicabilidad a una ETF no confirmada); `vigente_desde = current_date` (fecha de siembra presentada como vigencia de la norma); ponderaciones de matriz y umbrales de tipologías sin fuente.
- Impacto: 97.5.4 prohíbe inferir parámetros financieros; el sistema ya registra PCC-01/ROG con estos números (`fn_uif_registrar_operacion`). README de seeders marca 08 como "listo".
- Solución de fondo: procedencia (URL, fecha de obtención, responsable) por fila, confirmación de cumplimiento, fechas de vigencia reales, `activa=FALSE` hasta aprobación.
- Relación con el flujo nuevo: ninguna
- Regla: 97.4.2, 97.5.4, 00.2.1
- Esfuerzo estimado: medio
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-DATOS-047 — Bitácoras y evidencias de identidad/cumplimiento no son append-only
- Clase / Severidad / Confianza: DATOS / S2 / confirmado
- Ubicaciones: `sql/00_base/03_permisos.sql:17,42` (`GRANT SELECT, INSERT, UPDATE` a `svc_cumplimiento`/`svc_identidad`); `sql/35_append_only/append_only.sql` (sólo `acta_comite`, `evento_riesgo_operativo`, `registro_operacion_relevante` de mis esquemas)
- Evidencia: sin REVOKE ni trigger para `intento_autenticacion`, `intento_validacion_token`, `consentimiento`, `historial_credencial`, `aceptacion_contrato`, `envio_regulatorio`, `reporte_regulatorio` (post-ENVIADO), `alerta_monitoreo_lft`, `caso_investigacion_lft`.
- Impacto: la evidencia de consentimiento, de aceptación de contrato y del envío regulatorio se puede reescribir con el rol del servicio (91.1.3, 91.2.5). Los triggers son `FOR EACH ROW` y no frenan `TRUNCATE` (el rol no lo tiene, pero `rol_migracion` sí).
- Solución de fondo: agregarlas a la lista APPEND_ONLY del modelo; estados terminales sellados por trigger.
- Relación con el flujo nuevo: ninguna
- Regla: 91.1.3, 91.2.5, 90.2.7
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-TRAZA-048 — La aprobación de retiro no deja registro propio y el motivo de rechazo se pierde
- Clase / Severidad / Confianza: TRAZA / HUECO / S3 / confirmado
- Ubicaciones: `.../infraestructura/AprobacionDeRetiroRepositorio.java:34-43`; `.../aplicacion/ResolucionDeRetiro.java:204-219,271-281`; `sql/10_tablas/10_billetera_custodia/orden_retiro.sql` (sin `aprobada_en`, sin `motivo_rechazo`)
- Evidencia: el rechazo escribe al rechazador en `aprobada_por`; `motivo` sólo entra en `HuellaDePeticion`, no se persiste ni viaja en el evento.
- Impacto: no hay "quién, cuándo y por qué" auditable de la decisión; el cliente no puede explicar el rechazo. No existe "tabla de aprobación de retiro" (el pedido la suponía).
- Solución de fondo: tabla append-only `decision_retiro(orden_id, decisor, desenlace, motivo, decidida_en)` o columnas `decidida_en`, `rechazada_por`, `motivo_rechazo`.
- Relación con el flujo nuevo: APROB
- Regla: 90.2.7, 96.8.3
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-CONTRATO-049 — `nucleo_financiero.retiro_rechazado` sale con dos formas distintas
- Clase / Severidad / Confianza: CONTRATO / S3 / confirmado
- Ubicaciones: `.../ResolucionDeRetiro.java:162-169` (`cuentaBilleteraId`, `motivo`) vs `:271-281` (`cuentaBilleteraId`, `aprobadaPor`, `solicitadaPor`)
- Impacto: consumidores (notificaciones, auditoría) reciben payloads incompatibles bajo el mismo tipo.
- Solución de fondo: un esquema único versionado del evento.
- Regla: 98.1.3-5
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-PRUEBAS-050 — `debe_fallar` acepta cualquier excepción; dos pruebas de R-UIF fallan por otra causa
- Clase / Severidad / Confianza: PRUEBAS / S3 / confirmado (R-UIF-07) / probable (R-UIF-06)
- Ubicaciones: `sql/50_verificacion/prueba_humo.sql:25-33`, `:260-270` (R-UIF-06 inserta `codigo 'PCC-01'`, ya sembrado con UNIQUE), `:273-277` (R-UIF-07 con `regla_monitoreo_id NULL, usuario_id NULL` NOT NULL)
- Evidencia: `EXCEPTION WHEN others THEN RETURN 'OK ...'`.
- Impacto: la prueba pasa aunque el CHECK que dice probar no exista.
- Solución de fondo: `debe_fallar` con SQLSTATE/constraint esperado; datos válidos salvo la condición bajo prueba.
- Regla: 80.5.4
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-COHERENCIA-051 — Tres vocabularios para el carnet de extranjería y dos para el nivel KYC
- Clase / Severidad / Confianza: COHERENCIA / S3 / confirmado
- Ubicaciones: `identidad.yaml:1397` (`CEX`), `identidad.yaml:1047` y `sql/15_infra/borradores.sql:23` (`CARNET_EXTRANJERO`), `identidad/documento_identidad.sql:22` (`CARNET_EXTRANJERIA`), `servicios/identidad/.../web/MapeoDelAlta.java:64-66`; `identidad/usuario.sql:26` (`COMPLETO`) vs `identidad/verificacion_kyc.sql:23` (`AVANZADO`); `identidad.yaml:1314` (4 estados) vs `verificacion_kyc.sql:24` (6)
- Impacto: un borrador retomado devuelve `CARNET_EXTRANJERO` que `registrarUsuario` no acepta; filas `VENCIDA`/`NO_INICIADA` no caben en el enum del contrato.
- Solución de fondo: un único catálogo codificado (97.4.7) usado por contrato, borrador y tabla.
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-DATOS-052 — Importes sin moneda en identidad y cumplimiento; tipo de cambio sin fecha
- Clase / Severidad / Confianza: DATOS / S3 / confirmado
- Ubicaciones: `identidad/perfil_financiero.sql:9-10`; `identidad/restriccion_usuario.sql:11`; `cumplimiento/alerta_monitoreo_lft.sql:13`; `desvio_perfil.sql:11-12`; `reporte_regulatorio.sql:16`; `reclamo_cliente.sql:15`; `instancia_reclamo.sql:14`; `observacion_regulatoria.sql:13`; `licencia_regulatoria.sql:16`; `entorno_prueba_regulado.sql:11-12`; `regla_monitoreo_lft.sql:12`; `registro_operacion_relevante.sql:22` (sin fecha del tipo de cambio)
- Regla: 91.1.2
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-DATOS-053 — Idempotencia y catálogos cerrados faltantes en `token_verificacion`; FK e índices
- Clase / Severidad / Confianza: DATOS / S3 / confirmado
- Ubicaciones: `identidad/token_verificacion.sql:11,14,16,29` (sin CHECK de `estado`, `proposito`, `canal_entrega`; `clave_idempotencia NOT NULL` sin UNIQUE en `30_indices/01`); `30_indices/01_identidad_usuarios.sql` (`uq_usuario_telefono_e164` + `ix_usuario_telefono_e164` duplicado); `20_claves/01` (sin FK para `alcance_invitacion.emisor_id/consumidor_id/grupo_destino_id`, `sesion.refresco_familia_id`)
- Impacto: emisión de OTP duplicable ante reintento; estados libres; índice redundante.
- Regla: 91.2.4, 97.4.7, 97.5.1
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-SEG-054 — El hash de búsqueda del documento no usa el único camino que la regla declara
- Clase / Severidad / Confianza: SEG / DOC / S3 / confirmado
- Ubicaciones: `servicios/identidad/.../dominio/DocumentoDeIdentidad.java:63,76-81`; `sql/40_reglas/restricciones.sql:1222-1250`
- Evidencia: `MessageDigest.getInstance("SHA-256").digest(numero + pimienta)` vs R-SEG-01 "el hash de búsqueda es SIEMPRE un HMAC ... `fn_seg_hash_busqueda` es el único camino permitido".
- Impacto: dos implementaciones de la misma protección; la base no puede verificar cuál se usó (sólo `length = 64`).
- Solución de fondo: HMAC-SHA256 en un único componente compartido.
- Regla: 90.2.9
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-CONTRATO-055 — OpenAPI de identidad y cumplimiento sin esquema de seguridad
- Clase / Severidad / Confianza: CONTRATO / S3 / confirmado
- Ubicaciones: `servicios/identidad/src/main/resources/openapi/identidad.yaml:216,445,516` (`bearerAuth` referenciado; no hay `components.securitySchemes`); `cumplimiento.yaml` (ninguna operación declara seguridad)
- Impacto: spec inválido según OpenAPI; el contrato no documenta qué operaciones exigen token, ni el "token de alta" de `@Publico subirDocumento` (`ExpedienteController.java:88`).
- Regla: 96.4.2, 98.1.3
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-CONTRATO-056 — `buscarPorTelefono` deprecado sigue publicado con el teléfono en la URL
- Clase / Severidad / Confianza: CONTRATO / S3 / confirmado
- Ubicaciones: `identidad.yaml:682-701`; `postman/coleccion/identidad...json` (GET por-telefono); `packages/simulado/ejemplos/identidad/buscarPorTelefono.json`
- Impacto: PII en query (90.2.2); sin fecha de retiro (98.1.3).
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-CONTRATO-057 — `aprobarRetiro` declara 404 y 409 pero el código devuelve 422
- Clase / Severidad / Confianza: CONTRATO / S3 / confirmado
- Ubicaciones: `servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml:519-560`; `.../ResolucionDeRetiro.java:247-263`; `recarga-y-retiro.yaml:57-59`
- Evidencia: el propio yaml anota "hoy sale 422, no el 409 de abajo ... ambigüedad A-5".
- Regla: 96.4.4
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-PRUEBAS-058 — Pacto identidad→grupos sin verificación de productor ni casos de error
- Clase / Severidad / Confianza: PRUEBAS / CONTRATO / S3 / confirmado
- Ubicaciones: `contratos/pactos/identidad-grupos-pertenencia.json:5,9-44`
- Evidencia: "NO existe todavia una prueba contra el proceso real de grupos"; sólo dos 200.
- Impacto: 98.1.4; tres niveles (65) sin límite ni inválido. No hay pactos para quienes consumen identidad (nivel KYC, step-up) ni cumplimiento (alcance de licencia).
- Esfuerzo estimado: medio
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-SEG-059 — La evidencia step-up no está ligada al retiro concreto
- Clase / Severidad / Confianza: SEG / S3 / probable
- Ubicaciones: `.../infraestructura/SegundoFactorStepUp.java:139-169`; `.../dominio/CondicionesDeRetiro.java:55-56` (mensaje "o no es para este retiro")
- Evidencia: valida `iss/aud/sub/proposito=RETIRO/acr/jti`; no compara monto, destino ni clave de idempotencia.
- Impacto: un JWT step-up obtenido para un retiro chico autoriza otro de otro monto o destino dentro de su vigencia.
- Solución de fondo: claim con hash de (cuenta, destino, monto, clave) verificado al solicitar.
- Esfuerzo estimado: medio
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-APROB-060 — La bandeja no da información para decidir
- Clase / Severidad / Confianza: APROB / S3 / probable
- Ubicaciones: `.../infraestructura/BandejaDeRetirosRepositorio.java:138-148`; `nucleo-financiero.yaml:464-466` ("se ven en la ficha de la orden" — no hay operación de ficha entre las de retiro)
- Impacto: el aprobador ve importes y fechas, no destino, KYC ni historial: la segunda firma es ceremonial; tampoco hay bitácora de lectura (90.2.7).
- Relación con el flujo nuevo: APROB (argumento para A-1)
- Esfuerzo estimado: medio
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-DEFECTO-061 — Rechazar una orden le pone fecha de pago
- Clase / Severidad / Confianza: DEFECTO / S3 / confirmado
- Ubicaciones: `.../OrdenRetiroRepositorio.java:171-185`; `.../ResolucionDeRetiro.java:154-155`
- Evidencia: `pasarA` siempre hace `.set(pagada_en, pagadaEn)`; `rechazar` la llama con `ahora`. El propio repo (`:208-210`) dice que una fecha de pago sin pago "seria mentir".
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-RESILIENCIA-062 — Dos despachos al proveedor; `instruirPago` no puede correr bien
- Clase / Severidad / Confianza: RESILIENCIA / DUP-INT / S3 / confirmado
- Ubicaciones: `.../CU11RetirarSaldo.java:107-136`; `plataforma/comun-datos/.../Datos.java:43-48`; `.../CU11InstruirRetiro.java`, `RetirosConProveedor.java`; tests `CU11Test.java:95`, `CU11ProveedorTest.java:49`
- Evidencia: `instruirPago` sin `@Transactional` llama `datos.conContexto` (lanza `SinTransaccion` sin transacción) y se auto-invoca `rechazar` (el proxy `@Transactional` no actúa). Los tests lo envuelven en `transaccion.execute`, metiendo la llamada al proveedor dentro de la transacción que el comentario dice evitar.
- Solución de fondo: borrar `instruirPago` y dejar un solo despacho.
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.

### IDCU-DUP-INT-063 — Catálogos paralelos de umbrales y reglas de monitoreo
- Clase / Severidad / Confianza: DUP-INT / S3 / confirmado
- Ubicaciones: `08-gobierno-y-licencia.sql:52-58` (`regla_monitoreo_lft`, `ENTRADA_SALIDA_INMEDIATA` 500 / 120 min); `19-reportes-y-retencion.sql:56-68` (`regla_cumplimiento`, misma tipología con otros parámetros, umbral 10000.0 literal en vez de referir `umbral_reporte_uif`); `19:70-80` (`umbral_operativo` vs `limite_operativo_billetera`, el propio comentario admite que pueden divergir)
- Regla: 97.4.6
- Esfuerzo estimado: medio
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-FRAGIL-064 — Retención del retiro con motivo y vencimiento contrarios a CU-11
- Clase / Severidad / Confianza: FRAGIL / S3 / probable
- Ubicaciones: `.../SolicitudDeRetiro.java:34,126-136` (`COMISION_PENDIENTE`, vence a los días de política); `.../CU13RetenerSaldo.java:258-263` (`vencerCaducadas`, hoy sin llamador); `docs/CasosDeUso/CU-11 Retirar saldo.md:42,64`
- Impacto: CU-11 dice `ENTREGA_EN_CURSO` y "la retención vence y se libera" en timeout; si se cablea el vencimiento, una orden `EN_PROCESO` pierde su retención y el saldo se puede gastar dos veces.
- Solución de fondo: retención de retiro indefinida mientras la orden esté viva; el cierre lo decide sólo la orden.
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.

### IDCU-COHERENCIA-065 — CU-11 promete cosas que el código no hace
- Clase / Severidad / Confianza: COHERENCIA / DOC / S3 / confirmado
- Ubicaciones: `CU-11 ...md:30` (disponible ≥ monto + costo) vs `CondicionesDeRetiro.java:60` (monto ≤ disponible; costo se descuenta); `CU-11:36-37,61-62` (antifraude `REVISAR/RECHAZAR`) sin llamada en `SolicitudDeRetiro`; `CU-11:81` (`factorMfa` min 6 max 8) vs JWT step-up.
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-DEBIDO-PROCESO-066 — Bloqueos y retenciones automáticas sin expediente
- Clase / Severidad / Confianza: DEBIDO-PROCESO / S3 / confirmado (en datos); decisión regulatoria pendiente
- Ubicaciones: `cumplimiento/regla_monitoreo_lft.sql:22`; `08-gobierno-y-licencia.sql:54`; `19-reportes-y-retencion.sql:61-64`; `CU-44 ...md:38,57`
- Impacto: `RETENER_OPERACION`/`BLOQUEAR_USUARIO`/`BLOQUEAR_CUENTA` sin plazo de descargo (91.4); puede ser exigencia LGI/FT, pero la excepción no está registrada como decisión.
- Esfuerzo estimado: chico (registro) / medio (implementación)
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-SEG-067 — ADMIN_PLATAFORMA concentra funciones incompatibles
- Clase / Severidad / Confianza: SEG / S3 / confirmado
- Ubicaciones: `sql/60_semillas/10-roles-y-permisos.sql:36-40,59-60,92,100,145-195,241`
- Evidencia: mismo rol con `ACCESOS_ADMINISTRAR`, `VERIFICACION_RESOLVER`, `SEGURIDAD_ACCESO_RESTABLECER`, `SEGURIDAD_FACTOR_REINSCRIBIR`, `FONDO_ADMINISTRAR`, `TARIFARIO_PUBLICAR`, `CATALOGO_EDITAR`.
- Impacto: contradice la segregación que CU-08 3a dice exigir (`INCOMPATIBILIDAD_DE_FUNCIONES`).
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-HUECO-068 — R-UIF-10 deja pasar PEP COMPLETA sin `aprobada_por`
- Clase / Severidad / Confianza: HUECO / S3 / confirmado (semántica SQL)
- Ubicaciones: `sql/40_reglas/restricciones.sql:1110-1116`
- Evidencia: `NEW.segunda_revision_por IS NULL OR NEW.segunda_revision_por = NEW.aprobada_por` es NULL si `aprobada_por` es NULL → no lanza.
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-DATOS-069 — PII en claro en cumplimiento e identidad
- Clase / Severidad / Confianza: DATOS / S3 / confirmado
- Ubicaciones: `cumplimiento/beneficiario_final.sql:9` (`documento VARCHAR(30)`); `identidad/intento_autenticacion.sql:8` (`identificador_usado` teléfono/correo, indexado `30_indices/01`); `identidad/alcance_invitacion.sql:9`, `referencia_personal.sql:9`
- Regla: 90.2.1
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-ARQ-070 — ADR-032 incompleto: no hay `sql/migraciones/` ni verificación de deriva
- Clase / Severidad / Confianza: ARQ / CONFIG / S3 / confirmado
- Ubicaciones: `docs/Arquitectura/ADR-032 Aplicación del esquema.md` (punto 2); `ls sql/migraciones` → no existe; `sql/50_verificacion/verificaciones.sql` (sin chequeo modelo↔base↔jOOQ); `sql/20_claves/*.sql:6-10` (DROP/ADD de cada FK en cada aplicación); `30_indices` sin `CONCURRENTLY`; `15_infra/nulabilidad.sql:13-17` (advierte ACCESS EXCLUSIVE)
- Impacto: no hay camino de cambio de esquema a producción ni gate de deriva (97.2); reaplicar valida todas las FK con bloqueo (97.3.3).
- Esfuerzo estimado: grande
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-DOC-071 — Documentación generada desactualizada
- Clase / Severidad / Confianza: DOC / S4 / confirmado
- Ubicaciones: `seeders/README.md` ("15 roles, 29 permisos"; el seed tiene 31 roles); `docs/catalogo-de-flujos/matriz.md:17,187-222` (36 endpoints de identidad; el contrato tiene 38, faltan los de foto de perfil); `cumplimiento/catalogo_reporte_regulatorio.sql` y `reporte_regulatorio.sql` (mismo COMMENT ON TABLE); `identidad/verificacion_kyc.sql:42` (comentario de versionado aplicado a un vencimiento de KYC); matriz: ningún flujo de identidad/cumplimiento en el catálogo (KYC, UIF sin flujo).
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-DATOS-072 — Contratos de adhesión y políticas de token con valores de relleno o ambiguos
- Clase / Severidad / Confianza: DATOS / S4 / confirmado
- Ubicaciones: `11-contratos-de-adhesion.sql:62-67` (`repeat('0', 64)`, URL de dominio propio sin documento); `13-politicas-de-token.sql:72-74,89-90` (dos políticas `VERIFICACION_CORREO`, OTP y ENLACE, para el mismo propósito)
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-PRUEBAS-073 — Matriz negativa incompleta en la aprobación de retiro
- Clase / Severidad / Confianza: PRUEBAS / S4 / confirmado
- Ubicaciones: `BilleteraAprobacionWebTest.java:136-229` (sin 401, sin falta de `Idempotency-Key`; `cu11` mockeado); la bandeja sí tiene 401 (`BilleteraBandejaWebTest.java:190`)
- Regla: 80.2.3
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

### IDCU-CONFIG-074 — Barrel Dart versionado con `src/` ignorado
- Clase / Severidad / Confianza: CONFIG / S4 / confirmado
- Ubicaciones: `.gitignore:51` (`clientes/dart/`); `git ls-files clientes/dart/identidad` (barrel + pubspec trackeados); `apps/movil/pubspec.yaml:28,50`
- Impacto: el checkout trae un barrel que exporta archivos inexistentes hasta que se genera. Comparación automática barrel↔OpenAPI: identidad 50/50 esquemas; cumplimiento 49 + 4 `*_inner` del generador (sin deriva).
- Esfuerzo estimado: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Cómo se comprobó: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.

#### Tanda 4 — servicio `identidad` (lote 1)

Rutas sin prefijo: relativas a `servicios/identidad/src/main/java/bo/aportaya/identidad/`. Peldaño: DISCOVERED (Gradle no corrió).

### IDCU-SEG-075 — Suspender, bloquear o dar de baja una cuenta no impide volver a entrar
- Clase / Severidad / Confianza: SEG / S1 / confirmado (lectura)
- Ubicaciones: `src/main/java/bo/aportaya/identidad/aplicacion/CU04Autenticar.java:88-195`; `infraestructura/UsuarioRepositorio.java:25-48,78-83`; `infraestructura/UsuariosInternosRepositorio.java:221-235`; `aplicacion/RenovarSesion.java:94-151`
- Evidencia: el login solo filtra `USUARIO.ELIMINADO_EN.isNull()` y `tieneBloqueoVigente` (tabla `bloqueo_cuenta`); nunca lee `usuario.estado`. `actualizar` hace `.set(USUARIO.ESTADO, nuevoEstado)` + `cerrarSesionesDe`.
- Cómo se comprobó: se recorrió `autenticar()` completo y `renovar()`; ninguna consulta mira `estado` (SUSPENDIDO, BAJA_VOLUNTARIA, BLOQUEADO_SEGURIDAD del enum de `MiPerfil`).
- Nota del orquestador: re-verificado: `UsuarioRepositorio.estadoDe` (:50-55) existe y ningún archivo lo llama (`grep -rn "estadoDe(" servicios/identidad` → solo la definición); `CU04Autenticar.java:103` solo mira `tieneBloqueoVigente` (tabla `bloqueo_cuenta`), y `UsuariosInternosRepositorio.actualizar` (:221-233) cambia `usuario.estado` y cierra sesiones sin insertar bloqueo. Se sostiene S1 (saltar la autorización).
- Impacto: suspender a un operador (único efecto: cerrar sesiones) no lo deja afuera: vuelve a autenticarse y recibe token con sus permisos. Lo mismo con cuentas BLOQUEADO_SEGURIDAD o en baja.
- Causa de fondo: el estado de cuenta y el bloqueo de acceso son dos modelos (`usuario.estado`, `bloqueo_cuenta`, `usuario_interno.bloqueado_hasta`) sin una regla única de "puede autenticarse".
- Solución de fondo: átomo de dominio `PuedeAutenticarse(estado, bloqueos)` aplicado en `CU04Autenticar` y `RenovarSesion`; prueba de integración "suspendido no entra".
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los servicios (el token sigue emitiéndose)
- Regla de la casa: 90.1.2, 96.3.2
- Esfuerzo estimado: chico
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-076 — `/usuarios-internos` permite activar a cualquier participante saltando el KYC y darle cualquier rol
- Clase / Severidad / Confianza: SEG/APROB / S2 / probable (no se verificó la política RLS de UPDATE sobre `usuario` para app.rol=BACKOFFICE)
- Ubicaciones: `infraestructura/UsuariosInternosRepositorio.java:201-255,270-278`; `infraestructura/RotacionDeRolesInternos.java:36-82`; `web/UsuariosInternosController.java:63-85`; OpenAPI `identidad.yaml` `EntradaActualizarUsuarioInterno` (`estado: enum [ACTIVO, SUSPENDIDO]`) y `EntradaAltaUsuarioInterno.rolCodigo` libre
- Evidencia: `resolverUsuarioId` hace `.from(USUARIO).leftJoin(USUARIO_INTERNO)...where(USUARIO.ID.eq(id).or(...))` → acepta el id de un participante; `actualizar` escribe `USUARIO.ESTADO` sin precondición; `rotar` asigna cualquier código de rol con ámbito "GLOBAL".
- Cómo se comprobó: lectura de las tres rutas y del schema OpenAPI.
- Nota del orquestador: re-verificado `UsuariosInternosRepositorio.java:200-233,269-277` (leftJoin acepta cualquier `usuario.id`). Se baja a S2 porque exige `USUARIOS_INTERNOS_GESTIONAR` o `ACCESOS_ADMINISTRAR` (`UsuariosInternosController.java:61-62,70-71`): abuso de un privilegiado, no salto anónimo.
- Impacto: un operador con `USUARIOS_INTERNOS_GESTIONAR` puede (a) poner `ACTIVO` a un participante con KYC pendiente/rechazado (sin `nivel_kyc`, sin revisión, sin `revisada_por`); (b) otorgar `ADMIN_PLATAFORMA`/`SERVICIO_ENTREGAS`/`ENTREGA_AUTORIZAR` a una cuenta de participante que él controle (la única guarda es `uId != operadorId`). `crear` también acepta cualquier `rolCodigo`.
- Causa de fondo: el módulo de personal interno no está acotado a `usuario_interno` ni a roles de ámbito GLOBAL interno; no hay máquina de estados de `usuario`.
- Solución de fondo: `resolverUsuarioId` con JOIN interno obligatorio; lista blanca de roles asignables por quien asigna; transiciones de estado solo por casos de uso dueños (KYC activa; suspensión con expediente); SoD real (F05); test de autorización negativa.
- Relación con el flujo nuevo: APROB (camino de habilitación manual paralelo)
- Conexiones y módulos afectados: todos los servicios que confían en permisos del token
- Regla de la casa: 90.1.2-90.1.4, 90.1.9, 91.4
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-HUECO-077 — El segundo factor del operador no existe: no hay TOTP ni enrolamiento; en TEST cualquier factor es `000000`
- Clase / Severidad / Confianza: HUECO/SEG / S1 / confirmado
- Ubicaciones: `infraestructura/DesafioLocal.java:47-101`; `infraestructura/DesafioDeDesarrollo.java:36-81` (`CODIGO = "000000"`, línea 76 `if (CODIGO.equals(valorPresentado)) return true`); `aplicacion/CU04Autenticar.java:142-190`; `src/test/.../CableadoDelSegundoFactorTest.java` ("TEST (perfil local + gmail_api): el desafio primario es el de desarrollo"); OpenAPI sin endpoint de enrolamiento
- Evidencia: `DesafioLocal.emitir` genera `"%06d".formatted(azar.nextInt(1_000_000))` y solo lo guarda en un mapa; para TOTP no se envía a nadie; `validar` compara contra ese código. No hay lectura de `factor_mfa.secreto_cifrado` en todo el servicio.
- Cómo se comprobó: `grep implements DesafioDeFactor` → solo `DesafioLocal` y `DesafioDeDesarrollo`; `grep -i "recuper|restablec|factor|totp"` en el OpenAPI: no hay alta de factor.
- Nota del orquestador: re-verificado: solo existen `DesafioLocal` (código aleatorio en memoria, `DesafioLocal.java:39-56`) y `DesafioDeDesarrollo` (`@Profile("local")`, `CODIGO = "000000"`, `DesafioDeDesarrollo.java:36-40`); `grep -rli totp` no encuentra implementación. Relacionado con IDCU-SEG-004.
- Impacto: fuera del perfil `local`, un operador (R-SEG-10 exige factor siempre) no puede ingresar nunca: el código válido es un número aleatorio que no recibe. En el entorno TEST (perfil `local`, con Gmail real) cualquier operador entra con `000000`, y la evidencia step-up para RETIRO se obtiene igual. La única forma de operar el backoffice es con el bypass.
- Causa de fondo: adaptador de desarrollo promovido a implementación por omisión; enrolamiento de factores nunca construido.
- Solución de fondo: adaptador TOTP (RFC 6238) que lea el secreto cifrado; endpoints de enrolamiento/confirmación/códigos de respaldo; perfil `local` prohibido en entornos con datos (falla de arranque).
- Relación con el flujo nuevo: ninguna (bloquea backoffice)
- Conexiones y módulos afectados: nucleo-financiero (evidencia step-up), backoffice
- Regla de la casa: 90.1.1, 90.5, 00.2 (dato fijo)
- Esfuerzo estimado: grande
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-HUECO-078 — Step-up para retiros inutilizable en producción
- Clase / Severidad / Confianza: HUECO/RESILIENCIA / S2 / probable (consumo real en nucleo-financiero fuera de lote)
- Ubicaciones: `aplicacion/CU04StepUp.java:42` (`Map<UUID, Abierto> abiertos = new ConcurrentHashMap<>()`), `:68` (`desafio.emitir(ctx.usuarioId(), tipo)` — sobrecarga de 2 args que NO envía correo), `:60-71`; `infraestructura/DesafioLocal.java:39,47-56`; `descriptor.yml` (`replicas.min: 4`); `CU04StepUp.java:69,79,91` (`Instant.now()` en lugar de `Reloj`)
- Evidencia: el participante solo tiene factor CORREO (`CU04Autenticar.java:131-140`), pero `abrir` usa `factorActivo` (sin fallback a correo) y luego `emitir` sin destino → el código nunca llega; si no enroló factor → AP-CU04-06. Desafío y código viven en memoria de UNA réplica, con 4 réplicas mínimas.
- Cómo se comprobó: lectura; la propia clase declara "con mas de una replica... hay que moverlos a token_verificacion (riesgo residual)".
- Nota del orquestador: re-verificado `CU04StepUp.java:42,68` (mapa `abiertos` en memoria y `desafio.emitir(usuarioId, tipo)` de dos argumentos, que no entrega el código: `DesafioLocal.java:47-56`); `descriptor.yml:13` `min: 4`. Se baja a S2: impide retirar (fallo funcional), no mueve dinero.
- Impacto: los retiros/cambios de cuenta que exijan evidencia step-up no se pueden completar (o fallan al azar según el balanceador). El login de 2 pasos con correo tiene el mismo problema de réplica.
- Causa de fondo: estado de seguridad efímero en memoria; dos caminos de factor (login vs step-up) que divergen.
- Solución de fondo: persistir desafíos en `token_verificacion` (hash con pepper, intentos, vencimiento); unificar resolución de factor y entrega; usar `Reloj`.
- Relación con el flujo nuevo: FLUJO-FALTANTE (retiro del dinero recibido)
- Conexiones y módulos afectados: nucleo-financiero
- Regla de la casa: 98.2.4, 90.2.9
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DEFECTO-079 — La segregación de funciones compara códigos de permiso que no existen
- Clase / Severidad / Confianza: DEFECTO / S2 / confirmado
- Ubicaciones: `dominio/SegregacionDeFunciones.java:16-22`; usada en `infraestructura/RotacionDeRolesInternos.java:50` y `aplicacion/CU08AsignarRol.java:69`
- Evidencia: pares `"ENTREGAS_AUTORIZAR", "ENTREGAS_EJECUTAR"`, `"REVERSOS_AUTORIZAR"`, `"RETIROS_*"`, `"PAGOS_*"`; el catálogo `sql/60_semillas/10-roles-y-permisos.sql` define `ENTREGA_AUTORIZAR`, `ENTREGA_EJECUTAR`, `REVERSO_AUTORIZAR` (singular) y ningún RETIROS_/PAGOS_.
- Cómo se comprobó: `grep -rhoE` de ambos conjuntos en `sql/` y `servicios/*/src/main`.
- Nota del orquestador: re-verificado: `SegregacionDeFunciones.java:17-21` usa `ENTREGAS_AUTORIZAR`/`REVERSOS_AUTORIZAR`; `sql/60_semillas/10-roles-y-permisos.sql` siembra `ENTREGA_AUTORIZAR`/`REVERSO_AUTORIZAR` (0 coincidencias de los plurales).
- Impacto: nunca dispara AP-CU08-03; un mismo operador puede recibir autorizar+ejecutar entregas. Queda solo el control por registro en BD (`ck_entrega_segregacion`, `restricciones.sql:1504-1512`).
- Causa de fondo: pares escritos a mano sin referencia al catálogo ni prueba contra la semilla real (CU08Test usa permisos sintéticos).
- Solución de fondo: pares como dato de catálogo (`par_incompatible`) con FK a `permiso`; prueba de integración contra la semilla.
- Relación con el flujo nuevo: APROB (soporte de autorización humana de entregas/retiros)
- Conexiones y módulos afectados: entregas, nucleo-financiero, contabilidad
- Regla de la casa: 91.4, 97.4.6
- Esfuerzo estimado: chico
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-080 — Cualquier rol GLOBAL (anunciante, comercial, soporte, servicios) recibe `rol=BACKOFFICE`, que las políticas de fila tratan como privilegiado; la vigencia se ignora
- Clase / Severidad / Confianza: SEG / S2 / probable (impacto real depende de endpoints de otros servicios que confían en RLS)
- Ubicaciones: `web/SesionesController.java:55,201`; `web/SesionController.java:78-79`; `dominio/PerfilDeAcceso.java:33`; `infraestructura/UsuarioRepositorio.java:67-76` (no filtra `VIGENTE_HASTA` ni `OTORGADA_EN`); `aplicacion/ConsultarNivelKyc.java:22`; `sql/40_reglas/restricciones.sql:1326-1328` (`fn_seg_rol_privilegiado()` = `IN ('BACKOFFICE','CUMPLIMIENTO','AUDITOR')`); semilla de roles: `ANUNCIANTE`, `COMERCIAL`, `OPERADOR_PUBLICIDAD`, `SERVICIO_*` con ámbito GLOBAL
- Evidencia: `resultado.esOperador() ? ROL_DE_OPERADOR : ROL_DE_PARTICIPANTE` con `ROL_DE_OPERADOR = "BACKOFFICE"`.
- Cómo se comprobó: lectura + semilla + función SQL.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: un anunciante externo obtiene `app.rol=BACKOFFICE` → políticas `usuario_id = fn_seg_usuario_actual() OR fn_seg_rol_privilegiado()` le muestran filas de todas las personas en cualquier endpoint que filtre por RLS; además una asignación GLOBAL vencida sigue haciendo "operador".
- Causa de fondo: un único rol de sesión para todo personal (comentario del propio controlador, líneas 42-53: "refinamiento con su propio ADR").
- Solución de fondo: rol de sesión derivado del departamento/ámbito real (o `app.rol` = rol específico) y política de fila por permiso; `perfilDe` con la misma vigencia que `PermisosEfectivos`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: los 13 servicios (RLS)
- Regla de la casa: 90.1.2, 96.5.4, 90.2.8
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-081 — Fuerza bruta ilimitada sobre un OTP ya consumido devuelve el borrador con PII
- Clase / Severidad / Confianza: SEG / S2 / confirmado (lectura)
- Ubicaciones: `infraestructura/VerificacionCorreoRepositorio.java:183-187`; `infraestructura/BorradorOnboardingRepositorio.java:40-70`; ruta pública `POST /usuarios/verificaciones/correo/{id}/confirmacion` (`UsuariosController.java:262`)
- Evidencia: `if ("CONSUMIDO".equals(...)) { if (coincide) return borradores.resolverResultado(...); throw ... "ya fue utilizado" }` — sin incrementar `intentos_fallidos` ni registrar intento.
- Cómo se comprobó: lectura de `confirmarEnDsl`.
- Impacto: con `verificacionId` + correo, 10^6 intentos (gateway 5/s por IP; distribuible) devuelven nombres, teléfono, fecha de nacimiento, **número de documento en claro**, actividad y origen de fondos. Además permite releer el borrador indefinidamente.
- Causa de fondo: la rama de "reintento idempotente" no comparte el contador de intentos.
- Solución de fondo: contar intentos también sobre CONSUMIDO (o no devolver borrador tras consumo; entregarlo con un token de un solo uso).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna
- Regla de la casa: 90.1.14, 90.2.9
- Esfuerzo estimado: chico
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DATOS-082 — El número de documento y el origen de fondos del borrador se guardan en texto plano 7 días
- Clase / Severidad / Confianza: DATOS/SEG / S2 / confirmado
- Ubicaciones: `infraestructura/BorradorOnboardingRepositorio.java:98-141`; `dominio/BorradorOnboardingDatos.java`; contraste con `dominio/DocumentoDeIdentidad.java` (javadoc: "El numero nunca viaja ni se guarda en claro")
- Evidencia: `INSERT INTO identidad.borrador_onboarding (... numero_documento ... origen_de_fondos ... monto_mensual_estimado ...)`.
- Cómo se comprobó: lectura.
- Impacto: padrón de cédulas en claro legible por quien acceda a la base; contradice el diseño del documento.
- Causa de fondo: el borrador se diseñó como espejo del formulario.
- Solución de fondo: no persistir el número (o cifrarlo con la misma llave que `documento_identidad`); retención mínima.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna
- Regla de la casa: 90.2, 00.2.3
- Esfuerzo estimado: chico
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DATOS-083 — `numero_cifrado` no está cifrado: guarda el hash con un prefijo, el número es irrecuperable
- Clase / Severidad / Confianza: DATOS/REG / S2 / confirmado
- Ubicaciones: `web/MapeoDelAlta.java:59-62` (`return "cifrado:" + documento.hashNumero();`); `infraestructura/RegistroRepositorio.java:158-171` (`.set(DOCUMENTO_IDENTIDAD.NUMERO_CIFRADO, numeroCifrado)`, `VERSION_LLAVE = 1`); `dominio/DocumentoDeIdentidad.java:63,76,79-90` (SHA-256 de `numero + pimienta`)
- Evidencia: comentario "El cifrado real lo hace el adaptador de archivos; aca la frontera" — no hay tal adaptador.
- Cómo se comprobó: búsqueda de usos de `numeroCifrado`.
- Impacto: la columna promete un dato que no existe; reportes UIF/ASFI, cotejo con listas y debida diligencia no pueden obtener el número; `version_llave=1` sugiere una llave inexistente. Además el hash es SHA-256 simple sobre un CI de 7-8 dígitos: con la pimienta filtrada se revierte por fuerza bruta.
- Causa de fondo: placeholder que quedó en producción.
- Solución de fondo: cifrado real (KMS/llave versionada) + HMAC para búsqueda.
- Relación con el flujo nuevo: APROB (A-1: cumplimiento UIF depende de este dato)
- Conexiones y módulos afectados: cumplimiento, auditoría
- Regla de la casa: 97.4.1, 90.2
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-REG-084 — Consentimientos con versión y hash de documento inventados; los contratos aceptados no se registran
- Clase / Severidad / Confianza: REG/DATOS / S2 / confirmado
- Ubicaciones: `infraestructura/RegistroRepositorio.java:200-224` (`DSL.val("1")`, `DSL.val("0".repeat(64))`); `aplicacion/CU01RegistrarUsuario.java:150-152` (solo `aceptaContratos().isEmpty()`); `web/MapeoDelAlta.java:38` (`licenciaHabilitaBilletera` = `true` fijo)
- Evidencia: el arreglo `aceptaContratos` (UUIDs) se valida no vacío y se descarta.
- Cómo se comprobó: lectura completa del alta.
- Impacto: la prueba de consentimiento no identifica qué texto se aceptó (hash de ceros), ni qué contrato; la verificación de licencia R-LIC-01 es un literal.
- Causa de fondo: modelo de consentimiento sin catálogo de documentos versionados conectado.
- Solución de fondo: resolver versión/hash reales del documento vigente; persistir contrato aceptado; consultar licencia real.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: auditoría (CU-07 derechos), cumplimiento
- Regla de la casa: 97.4.1, 00.2.1
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-FLUJO-FALTANTE-085 — La decisión KYC no emite evento; la coreografía del alta está incompleta
- Clase / Severidad / Confianza: FLUJO-FALTANTE/CONTRATO / S2 / confirmado (búsqueda de consumidores en `servicios/`)
- Ubicaciones: `infraestructura/RevisionRepositorio.java:174-235` (sin outbox); `aplicacion/CU02IniciarVerificacion.java:46-72` (sin outbox); `aplicacion/CU01RegistrarUsuario.java:30-57,235-247` (javadoc: "cumplimiento y nucleo-financiero lo consumen")
- Evidencia: lista completa de `"identidad.*"` emitidos (ver Conexiones): no hay `kyc_aprobado/rechazado/verificacion_iniciada`. Único consumidor de `usuario_registrado`: `nucleo-financiero/.../ConsumidorDeUsuariosKafka.java:33`.
- Cómo se comprobó: `grep` de los temas en todos los servicios.
- Impacto: cumplimiento no se entera del alta ni del KYC aprobado; nucleo abre billetera al registrarse incluso con `nivelSolicitado=NINGUNO` (antes de cualquier KYC); otros servicios deben preguntar `/kyc` sincrónicamente.
- Causa de fondo: el evento de cambio de estado de identidad nunca se modeló.
- Solución de fondo: eventos `identidad.verificacion_iniciada/resuelta` por outbox con nivel resultante; consumidor en cumplimiento.
- Relación con el flujo nuevo: APROB (si el KYC se automatiza, el evento es el disparador)
- Conexiones y módulos afectados: cumplimiento, nucleo-financiero, grupos
- Regla de la casa: 98.3.3, 96.7.2
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-086 — Las fotos del expediente se pueden reemplazar después de aprobado (evidencia legal sobrescrita)
- Clase / Severidad / Confianza: SEG / S2 / confirmado
- Ubicaciones: `infraestructura/ExpedienteRepositorio.java:47-75`; `aplicacion/AnotarFotoEnExpediente.java:91-106`; `web/ExpedienteController.java:89-111`
- Evidencia: `dsl.update(VERIFICACION_KYC).set(columna, ...).where(VERIFICACION_KYC.USUARIO_ID.eq(usuarioId))` — sin condición de estado ni de verificación vigente; idem `DOCUMENTO_IDENTIDAD`.
- Cómo se comprobó: lectura.
- Impacto: el titular (con su sesión) cambia anverso/selfie de un expediente APROBADO; la clave anterior se pierde (no hay historial) y el expediente aprobado ya no muestra lo que se aprobó. Actualiza además todas las verificaciones del usuario.
- Causa de fondo: escritura sin precondición (96.3.2) y columnas mutables para evidencia.
- Solución de fondo: aceptar fotos solo en PENDIENTE/EN_REVISION de la verificación vigente; evidencia append-only.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: cumplimiento/auditoría
- Regla de la casa: 96.3.2, 91.1.3 (espíritu append-only)
- Esfuerzo estimado: chico
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-TRAZA-087 — Roles otorgados a nombre del revisor KYC que no los decidió, y con ámbito GLOBAL para roles de GRUPO
- Clase / Severidad / Confianza: TRAZA / S2 / confirmado
- Ubicaciones: `infraestructura/RevisionRepositorio.java:220-235` (PARTICIPANTE, `otorgada_por = revisor`, `'GLOBAL'`); `infraestructura/FirmaDeOrganizadorRepositorio.java:37-53,104-126` (ORGANIZADOR, `otorgante` = quien aprobó el KYC, `'GLOBAL', NULL`); semilla: PARTICIPANTE y ORGANIZADOR tienen ámbito `GRUPO`; `dominio/AmbitoDeRol.java` ("Un rol de grupo sin grupo seria un rol de plataforma disfrazado")
- Evidencia: `INSERT INTO identidad.asignacion_rol ... SELECT ?, r.id, 'GLOBAL', NULL, ?, ...`.
- Cómo se comprobó: lectura + semilla.
- Impacto: la bitácora de accesos atribuye a un analista de cumplimiento la concesión de ORGANIZADOR meses después; `GRUPO_ADMINISTRAR` se concede para toda la plataforma. Si el revisor aprueba su propia cuenta, `ck_asignacion_no_autoasignada` revienta con 500 en lugar de 422.
- Causa de fondo: necesidad de un `otorgada_por` no nulo ≠ titular sin un actor de sistema modelado.
- Solución de fondo: actor de sistema explícito para asignaciones automáticas; asignación con `ambito` del rol; regla de "no aprobar el propio expediente" en el caso de uso.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: auditoría, grupos
- Regla de la casa: 90.2.7, 96.8.3
- Esfuerzo estimado: chico
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-HUECO-088 — CU-08 y CU-09 sin endpoint; no existe recuperación, cambio de clave, baja ni enrolamiento MFA
- Clase / Severidad / Confianza: HUECO / S2 / confirmado
- Ubicaciones: `aplicacion/CU08AsignarRol.java`, `CU08RevocarRol.java`, `CU09CambiarCredencial.java`, `CU09SolicitarBaja.java` (sin referencias desde `web/`); `README.md:18-25` ("Implementado"); mensajes "Podés recuperar el acceso" `CU01RegistrarUsuario.java:128,139,147`; plantilla de correo "cambia tu contrasena" (`PlantillaCorreoDeVerificacion.java`)
- Evidencia: `grep CU08AsignarRol|CU09CambiarCredencial` en `src/main` solo encuentra las clases.
- Cómo se comprobó: búsqueda + OpenAPI.
- Impacto: un usuario que olvida la clave pierde la cuenta (y el acceso a su dinero); nadie puede cambiar su clave ni pedir la baja (derecho ARCO); tras un KYC RECHAZADO no hay forma de reenviar (`pasarARevisionSiFaltabaDocumento` exige INCOMPLETO_KYC, `RegistroRepositorio.java:189-197`). Tests de CU-08/CU-09 prueban código inalcanzable.
- Causa de fondo: casos de uso implementados sin su página.
- Solución de fondo: exponer con contrato, autorización negativa e idempotencia; flujo de recuperación con token de un solo uso + enfriamiento (ya modelado en `VentanaDeEnfriamiento`).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: nucleo (enfriamiento), grupos (obligaciones de baja)
- Regla de la casa: 00.6.2, 30
- Esfuerzo estimado: grande
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-089 — Desbloquear un interno no desbloquea; la clave provisional del alta nunca se entrega
- Clase / Severidad / Confianza: SEG / S2 / confirmado
- Ubicaciones: `infraestructura/UsuariosInternosRepositorio.java:257-268` (limpia `usuario_interno.bloqueado_hasta`), `:180-193` (`generarContrasenaProvisional()` se hashea y se descarta); login usa `bloqueo_cuenta` (`UsuarioRepositorio.java:78-83`)
- Evidencia: `bloqueado_hasta` no se escribe en ningún lugar de identidad (grep).
- Cómo se comprobó: búsqueda de escrituras.
- Impacto: el botón de desbloqueo es un placebo; un operador recién creado no tiene forma de conocer su clave (y no hay recuperación, F14) ni de enrolar factor (F03).
- Causa de fondo: modelo de bloqueo duplicado; flujo de invitación de operador inexistente.
- Solución de fondo: un solo modelo de bloqueo; alta de operador con enlace de activación de un solo uso.
- Relación con el flujo nuevo: APROB (desbloqueo manual)
- Conexiones y módulos afectados: backoffice
- Regla de la casa: 90.1.11
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-090 — Lecturas de datos sensibles sin bitácora de lectura, aunque el contrato lo promete
- Clase / Severidad / Confianza: SEG / S2 / confirmado (no hay escritura a `registro_acceso_datos` ni evento)
- Ubicaciones: `web/VerificacionesController.java:48-104`; `aplicacion/CU02RevisarExpediente.java:56-75`; `aplicacion/ConsultarContactoEnGrupo.java:26-31`; `aplicacion/VerificarTitularidad.java:38-75`; OpenAPI `verFotoDelExpediente`/`verContenidoDeFoto` ("Cada llamada es una lectura de dato personal y queda registrada")
- Evidencia: tabla `comun.registro_acceso_datos` existe (`sql/10_tablas/09_auditoria_reportes/registro_acceso_datos.sql`, constraint `ck_acceso_justificacion` probado en `CU04RechazosTest.java:92-106`) y nadie la escribe desde identidad.
- Cómo se comprobó: grep.
- Impacto: ver cédulas/selfies, teléfonos de integrantes y cotejos de titularidad no deja rastro auditable.
- Causa de fondo: bitácora de lectura sin componente de plataforma.
- Solución de fondo: interceptor/evento de lectura con justificación para endpoints marcados sensibles.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: auditoría
- Regla de la casa: 90.2.7
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-091 — Enumeración de cuentas en rutas públicas y por tiempo de respuesta del login
- Clase / Severidad / Confianza: SEG / S2 / confirmado (lectura; tiempo no medido)
- Ubicaciones: `aplicacion/SolicitarVerificacionCorreo.java:40-48` (AP-CU01-03 `campo=correo`); `aplicacion/CU01RegistrarUsuario.java:125-149` (`campo` telefono/documento/correo); `aplicacion/CU04Autenticar.java:96-101` (usuario inexistente responde sin ejecutar Argon2; existente ejecuta Argon2 64 MiB/3 iteraciones)
- Evidencia: `throw new ErrorDeNegocio(CodigoError.de(1, 3), "Ya hay una cuenta con ese telefono...", Map.of("campo", "telefono"))`.
- Cómo se comprobó: lectura.
- Impacto: cualquiera sin cuenta confirma si un teléfono, correo o CI tiene cuenta (el CI con un alta de prueba); el login también lo filtra por tiempo, anulando el mensaje genérico de `CU04Autenticar.java:97-100`.
- Causa de fondo: decisión de UX (2026-10-15) sin contramedida.
- Solución de fondo: respuesta uniforme + aviso al dueño por canal verificado; hash señuelo en login.
- Relación con el flujo nuevo: ninguna
- Regla de la casa: 90.1.14, skill authn-identity
- Esfuerzo estimado: chico
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-092 — `VerificarTitularidad` responde "coincide" sin KYC aprobado y puede devolver 500
- Clase / Severidad / Confianza: SEG / S2 / probable
- Ubicaciones: `aplicacion/VerificarTitularidad.java:45-75`
- Evidencia: solo exige que exista `d.hash_numero` (comentario: "Sin documento verificado..."), no `verificacion_kyc.estado='APROBADA'` ni `documento_identidad.estado`; `leftJoin(documento_identidad)` + `fetchOne()` falla con más de un documento.
- Cómo se comprobó: lectura.
- Impacto: entregas puede registrar una cuenta bancaria "de titular verificado" para alguien cuyo KYC está pendiente o fue rechazado (el documento se guarda en el alta, antes de revisar).
- Causa de fondo: confundir "declaró un documento" con "documento verificado".
- Solución de fondo: cotejar contra documento de verificación APROBADA vigente.
- Relación con el flujo nuevo: DINERO/desembolso (91.3.3)
- Conexiones y módulos afectados: entregas
- Regla de la casa: 91.3.3
- Esfuerzo estimado: chico
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-093 — `GRUPO_ADMINISTRAR` (autoconcedido vía CU-90) permite enumerar teléfonos sin tope e invitar a grupos ajenos
- Clase / Severidad / Confianza: SEG/ACOPLE / S2 / probable
- Ubicaciones: `aplicacion/BuscarPorTelefono.java:35-38` (contexto `deSistema`); `web/UsuariosController.java:155-169` (sin límite); `aplicacion/EmitirTokenDeInvitacion.java:44-236` (no verifica que `ctx` administre `grupoId`); `infraestructura/FirmaDeOrganizadorRepositorio.java:104-126` (ORGANIZADOR GLOBAL)
- Evidencia: rol ORGANIZADOR se obtiene en autoservicio tras KYC (A3); la búsqueda devuelve `existe` + UUID por teléfono.
- Cómo se comprobó: lectura; la propiedad del grupo vive en `grupos` (que llama con el token del usuario), pero la ruta de identidad está publicada en el gateway (`rutas.yml:32-42`).
- Impacto: cualquier organizador puede barrer +591XXXXXXXX y mapear clientes (5/s por IP); puede emitir tokens de invitación para `grupoId` ajenos directamente contra identidad.
- Causa de fondo: autorización por rol sin propiedad del recurso.
- Solución de fondo: tope por actor persistente; verificar propiedad del grupo (pregunta a grupos o claim firmado); exponer estas rutas solo servicio-a-servicio.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: grupos
- Regla de la casa: 90.1.2-90.1.3, 90.1.14
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-RESILIENCIA-094 — Estado de seguridad en memoria con 4 réplicas mínimas
- Clase / Severidad / Confianza: RESILIENCIA / S2 / confirmado (lectura + descriptor)
- Ubicaciones: `infraestructura/DesafioLocal.java:39-40`; `aplicacion/CU04StepUp.java:42`; `web/LimiteDeConsultas.java:32`; `descriptor.yml` `replicas.min: 4`
- Evidencia: `ConcurrentHashMap` para desafíos y topes.
- Cómo se comprobó: lectura.
- Impacto: segundo factor por correo (login) y step-up fallan si la segunda petición cae en otra réplica; reinicios pierden desafíos; tope de titularidad efectivo ×4..×10.
- Causa de fondo: adaptador "local" usado en producción.
- Solución de fondo: persistir en `token_verificacion`/Redis.
- Relación con el flujo nuevo: FLUJO-FALTANTE (retiros)
- Regla de la casa: 98.2.4
- Esfuerzo estimado: medio
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-CONTRATO-095 — Códigos de error reutilizados con significados distintos y fuera del patrón declarado
- Clase / Severidad / Confianza: CONTRATO / S3 / confirmado
- Ubicaciones: AP-CU01-07 = "documento vencido" (`CU01RegistrarUsuario.java:115`, `CU02IniciarVerificacion.java:53`) y "no pudimos enviar el correo"/"solicitud de otro correo"/"sin política" (`SolicitarVerificacionCorreo.java:64`, `VerificacionCorreoRepositorio.java:62,67,83`, `BorradorOnboardingRepositorio.java:159`); AP-CU01-08 = "ya resuelto" (`RevisionRepositorio.java:193`) y "límite diario de códigos" (`VerificacionCorreoRepositorio.java:120,129,138`); AP-CU01-09/10/11 idem (`CU02RevisarExpediente.java:99-113` vs `VerificacionCorreoRepositorio.java:174-213`, `CU01RegistrarUsuario.java:204`, `CU02ActualizarFechaExpiracion.java:28`); AP-CU04-08 = propósito inválido, desafío usado y "sin política de refresh" (`CU04StepUp.java:62,80`, `RenovarSesion.java:168`); OpenAPI `Error.codigo` pattern `^AP-CU\d+-\d{2}$` excluye AP-VAL-02, AP-SES-01, AP-BD-01 que el propio contrato documenta
- Evidencia: ver citas.
- Impacto: la app no puede distinguir errores (96.4.4); validadores de contrato rechazarían respuestas reales.
- Solución de fondo: catálogo de códigos por CU, prueba que impida duplicados.
- Relación con el flujo nuevo: ninguna
- Regla de la casa: 96.4.4
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-CONTRATO-096 — `Idempotency-Key` obligatoria en el contrato pero ignorada en el código
- Clase / Severidad / Confianza: CONTRATO/DINERO / S3 / confirmado
- Ubicaciones: `UsuariosController.java:236-238` (alta), `ExpedienteController.java:89-91` (subida), `VerificacionesController.java:97-99` (decisión KYC), `UsuariosInternosController.java:44-47` (alta interno), `SesionesController.java:129-131` (desafío)
- Evidencia: el parámetro `idempotencyKey` no se pasa a ningún caso de uso; el componente `plataforma/comun-web/.../idempotencia/Idempotencia.java` existe y no se usa en identidad.
- Impacto: un reintento del alta responde 422 CUENTA_YA_EXISTE en vez de la misma respuesta; reintentar el desafío reemplaza el código anterior; reintentar alta de interno duplica error/estado.
- Solución de fondo: usar el componente de idempotencia de plataforma en todas las mutaciones con Key.
- Relación con el flujo nuevo: ninguna
- Regla de la casa: 91.1.4 (espíritu), 96.3.3, 96.4.1
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DINERO-097 — `montoMensualEstimado` es texto libre sin formato ni moneda
- Clase / Severidad / Confianza: DINERO / S3 / confirmado
- Ubicaciones: OpenAPI `BorradorOnboarding.montoMensualEstimado: { type: string }`; `dominio/BorradorOnboardingDatos.java`; `BorradorOnboardingRepositorio.java:138`
- Evidencia: el contrato ya define `Dinero{monto, moneda}` y no lo usa.
- Impacto: dato de debida diligencia (perfil transaccional UIF) sin moneda ni validación.
- Solución de fondo: usar `Dinero`.
- Relación con el flujo nuevo: ninguna
- Regla de la casa: 91.1.1-91.1.2
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-098 — Una sola pimienta para todo; hash de documento sin HMAC
- Clase / Severidad / Confianza: SEG / S3 / confirmado
- Ubicaciones: `infraestructura/Argon2Hasheador.java:27,60-66`; `dominio/DocumentoDeIdentidad.java:63,76`; `infraestructura/ProteccionDeVerificacionCorreo.java:19`; `web/ExpedienteController.java:72` (`new TokenDeAlta(pimienta, ...)`)
- Impacto: filtrar un secreto compromete hash de claves, padrón de CI, OTP de correo y tokens de alta a la vez; rotarlo invalida todo.
- Solución de fondo: claves derivadas por propósito (HKDF) y HMAC para el documento.
- Regla de la casa: 90.3, 90.2.9
- Esfuerzo estimado: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-099 — Refresh con SHA-256 sin pimienta; JWT de acceso sin `iss`/`aud`, no revocable al cerrar sesión o suspender
- Clase / Severidad / Confianza: SEG / S3 / confirmado
- Ubicaciones: `aplicacion/RenovarSesion.java:211-219`; `infraestructura/EmisorDeAcceso.java:112-122`; `plataforma/comun-web/.../ConfiguracionDelDecodificador.java` (solo `withJwkSetUri`)
- Impacto: incumple 90.2.9 literal; el token de acceso sigue 15 min después de logout/revocación/suspensión; sin `aud`, cualquier servicio acepta tokens destinados a otro.
- Solución de fondo: HMAC con pepper; `iss`/`aud` validados; lista de sesiones revocadas o `sesion_id` verificado en operaciones de dinero.
- Regla de la casa: 90.2.9, 98.6.1
- Esfuerzo estimado: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-CONFIG-100 — Configuración que no falla al arrancar y tamaños incoherentes
- Clase / Severidad / Confianza: CONFIG / S3 / confirmado
- Ubicaciones: `infraestructura/SecretoDeInvitacion.java:24-30` (valida la clave en el primer uso → 500); `application.yml` `invitaciones.clave: ${INVITACIONES_CLAVE:}`; `EmisorDeAcceso.java:73-81` (fail-closed solo perfiles `staging`/`production`; TEST usa `local`); `application.yml` `hikari.maximum-pool-size: 20` vs `descriptor.yml` `hikari_por_replica: 10`
- Impacto: arranca "sano" y falla en la primera invitación; con otro nombre de perfil, clave JWT en memoria con 4 réplicas; presupuesto de conexiones ×2.
- Solución de fondo: `@ConfigurationProperties @Validated`.
- Regla de la casa: 90.3.4
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-FRAGIL-101 — Carreras en controles de seguridad
- Clase / Severidad / Confianza: FRAGIL / S3 / probable
- Ubicaciones: `AccesoRepositorio.java:75-91` + `CU04Autenticar.java:112-125` (conteo de fallidos sin bloqueo: N intentos paralelos superan el tope); `FirmaDeOrganizadorRepositorio.java:110-120` (`NOT EXISTS` + INSERT sin unicidad); `CU08RevocarRol.java:56`/`RotacionDeRolesInternos.java:58-72` (último administrador: lectura-luego-escritura); `CU01RegistrarUsuario.java:263-265` (código público de 8 hex: colisión probable a ~65k usuarios → 409/500)
- Solución de fondo: bloqueo por usuario (`FOR UPDATE`/advisory) e índices únicos.
- Regla de la casa: 96.3.1-96.3.2
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-ARQ-102 — Capas cruzadas, SQL crudo en aplicación y "contexto de sistema" con el usuario de la sesión
- Clase / Severidad / Confianza: ARQ / S3 / confirmado
- Ubicaciones: `aplicacion/GestionarUsuariosInternos.java:5-11` e `infraestructura/UsuariosInternosRepositorio.java`, `RolesYPermisosRepositorio.java`, `UsuarioInternoMapeador.java` importan `web.generado.modelo`; SQL crudo en `aplicacion/ConsumirInvitacion.java`, `EmitirTokenDeInvitacion.java`, `CerrarSesion.java:29`, `ConsultarMiPerfil.java:31`, `ConsultarNivelKyc.java`, `ValidarTokenDeInvitacion.java`; `ContextoSesion.deSistema(ctx.usuarioId(), ...)` en `BuscarPorTelefono.java:37`, `ValidarTokenDeInvitacion.java:35`, `ContactoEnGrupoRepositorio.java:28`; `FirmaDeOrganizadorRepositorio` abre transacciones en infraestructura (elude `ArquitecturaTest.laTransaccionVivEnElOrganismo`)
- Impacto: RLS se apaga con identidad del usuario (auditoría ambigua); la ArchUnit no lo detecta.
- Solución de fondo: puertos de lectura acotados; ArchUnit aplicacion↛web.
- Regla de la casa: 96.1.3, skill native-code-patterns
- Esfuerzo estimado: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DUP-INT-103 — Dos implementaciones de asignación de roles divergentes
- Clase / Severidad / Confianza: DUP-INT / S3 / confirmado
- Ubicaciones: `aplicacion/CU08AsignarRol.java:248-300` (valida ámbito, permisos resultantes con los vigentes, MFA) vs `infraestructura/RotacionDeRolesInternos.java:36-82` (sin validar ámbito, SoD solo sobre los nuevos, siempre GLOBAL, revoca todo)
- Impacto: la ruta expuesta es la débil; la probada (CU08Test) no está expuesta.
- Solución de fondo: una sola.
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-RENDIMIENTO-104 — Listados sin paginación, N+1 y relectura total tras decidir
- Clase / Severidad / Confianza: RENDIMIENTO / S3 / confirmado
- Ubicaciones: `RevisionRepositorio.java:23-54`; `UsuariosInternosRepositorio.java:48-97` (2 consultas por fila); `AccesosRepositorio.java:39-56`; `RolesYPermisosRepositorio.java:177-201`; `VerificacionesController.java:100-104` (`revision.cola(null, ...)...orElseThrow()` → `NoSuchElementException` 500 si no aparece)
- Regla de la casa: 96.6.1
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-COHERENCIA-105 — Tres nombres para el carné de extranjería y conversiones que tragan errores
- Clase / Severidad / Confianza: COHERENCIA / S3 / confirmado
- Ubicaciones: OpenAPI `Documento.tipo: [CI, CEX, PASAPORTE]`, `BorradorOnboarding.tipoDocumento: [CI, PASAPORTE, CARNET_EXTRANJERO]`; `DocumentoDeIdentidad.Tipo.CARNET_EXTRANJERIA`; `web/MapeoDelBorrador.java:24,39` (`catch (IllegalArgumentException ignored) {}`)
- Impacto: borrador con CARNET_EXTRANJERO no mapea a dominio; datos que se pierden en silencio.
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DATOS-106 — Valores inventados presentados como reales
- Clase / Severidad / Confianza: DATOS / S3 / confirmado
- Ubicaciones: `UsuariosInternosRepositorio.java:164` (`FECHA_NACIMIENTO, LocalDate.of(1990, 1, 1)`), `:167` (`"ES_BO"` vs `"es"` en `RegistroRepositorio.java:114`), `:133` (`setActualizadoEn(ahora)`); `SesionesController.java:58` (`nivel_diligencia = "SIMPLIFICADA"` fijo en todo token); `AccesoRepositorio.java:115-116` (`modelo/version_app = "desconocido"`)
- Impacto: datos de personas y de diligencia falsos que otros servicios leen como ciertos (nivel de diligencia en el token).
- Regla de la casa: 00.2.1, 97.4.1
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-TRAZA-107 — La IP registrada es la del gateway
- Clase / Severidad / Confianza: TRAZA / S3 / probable (sin `server.forward-headers-strategy` en identidad ni comun-web)
- Ubicaciones: `getRemoteAddr()` en `UsuariosController.java:142`, `SesionesController.java` (mapear), `InvitacionesController.java:32,53`, `SesionController.java:68`
- Impacto: `intento_autenticacion.ip_origen`, consentimientos, tokens e intentos de canje sin valor forense; cualquier detección por IP es inútil.
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-RESILIENCIA-108 — Efectos externos dentro de transacción y saga sin compensación
- Clase / Severidad / Confianza: RESILIENCIA / S3 / confirmado
- Ubicaciones: `CU04Autenticar.java:165` (envío de correo dentro de `@Transactional`); `ActivarRolOrganizador.java:45-57` (tx1 token → HTTP organizador → tx2 rol; 5xx de `OrganizadorPorHttp` sale como `ErrorDeDominio` sin capturar); `ClienteDeServicio.blindar` registra cuerpos 4xx en log (plataforma)
- Regla de la casa: 96.3.4, 98.3.2
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-109 — Evidencia de firma de contrato de organizador débil
- Clase / Severidad / Confianza: SEG / S3 / probable
- Ubicaciones: `FirmaDeOrganizadorRepositorio.java:55-102`
- Evidencia: token `FIRMA_CONTRATO` nace `CONSUMIDO`; `contenidoHash` lo manda el cliente y no se coteja con el texto vigente; `clave_idempotencia = "firma-org:" + contratoId` (si la unicidad es global y `contratoId` es compartido, la segunda persona cae en `IllegalStateException` "No hay politica").
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DOC-110 — Documentación que contradice el código
- Clase / Severidad / Confianza: DOC/CONTRATO / S3 / confirmado
- Ubicaciones: OpenAPI `registrarUsuario` ("Una transaccion: usuario, KYC, debida diligencia, calificacion de riesgo, expediente y cuenta de billetera"); cabecera OpenAPI ("Toda ruta exige sesion"); `README.md` (CU-08/09 "Implementado", tablas de eventos vacías aunque emite 8 temas); `CU01RegistrarUsuario.java:47-51` (cumplimiento consume); `SalidaRegistro.tokenDeAlta` ("solo subir fotos" — también autoriza consultar estado)
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-PRUEBAS-111 — Las pruebas no cubren los controles que fallan
- Clase / Severidad / Confianza: PRUEBAS / S3 / confirmado
- Ubicaciones: `src/test/.../web/*WebTest` (casos de uso `@MockitoBean`); `StepUpTest.java` (DesafioDeFactor mock); `UsuariosInternosControllerWebTest` (solo felices + 401/403); `CU04RechazosTest.java:22-106` (escribe en `comun` y `nucleo_financiero`; R-SEG-09 solo verifica que el constraint existe)
- Faltan: suspendido no entra; SoD con semilla real; PATCH a participante; foto tras APROBADA; fuerza bruta en CONSUMIDO; dos réplicas; TOTP real; bitácora de lectura.
- Regla de la casa: 80.2.3-80.2.4
- Esfuerzo estimado: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-112 — Ruta deprecada con teléfono en la URL sigue publicada
- Clase / Severidad / Confianza: SEG / S3 / confirmado
- Ubicaciones: `UsuariosController.java:151-162`; OpenAPI `/usuarios/por-telefono`; `grupos/.../PersonasPorHttp.java:44` ya usa el POST
- Regla de la casa: 90.2.2
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-OTRO-113 — `actualizarFechaExpiracionDocumento` es `@Publico` pero exige sesión; actualiza todos los documentos
- Clase / Severidad / Confianza: OTRO / S4 / confirmado
- Ubicaciones: `ExpedienteController.java:141-148` (`sesion.actual()` antes de `titular.exigir`); `FechaDeExpiracionRepositorio.java:37-40`
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-CLEAN-114 — `ConsultarNivelKyc` autoriza por string de rol que nunca viene en el token
- Clase / Severidad / Confianza: CLEAN / S4 / confirmado
- Ubicaciones: `ConsultarNivelKyc.java:22` (`"ADMIN_PLATAFORMA"` nunca es `rol` del token: solo PARTICIPANTE/BACKOFFICE)
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-RENDIMIENTO-115 — Gmail pide token OAuth en cada envío; cabecera `To:` sin sanear
- Clase / Severidad / Confianza: RENDIMIENTO / S4 / confirmado
- Ubicaciones: `GmailApiCorreoDeVerificacion.java:49-88`
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-OBSERV-116 — Tope de titularidad por réplica y mapa `entregadosPorCorreo` que crece
- Clase / Severidad / Confianza: OBSERV / S4 / confirmado
- Ubicaciones: `web/LimiteDeConsultas.java` (declarado); `DesafioLocal.java:40,72`
- Esfuerzo estimado: chico

**Conteo: S1 = 4 · S2 = 15 · S3 = 19 · S4 = 4 · total 42.**
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

#### Tanda 5 — servicio `cumplimiento` (lote 2)

Rutas sin prefijo: relativas a `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/`. Peldaño: DISCOVERED. El hallazgo del subagente sobre CU-40 se fusionó en IDCU-REESCRITURA-001 / IDCU-ACOPLE-002 (agrega: `svc_cumplimiento` sin GRANT sobre `nucleo_financiero`).

### IDCU-FLUJO-FALTANTE-117 — La detección UIF (PCC-01 y ROG, CU-41/CU-42) no tiene ningún disparador: los reportes mensuales a la UIF salen siempre "en cero"
- Clase / Severidad / Confianza: FLUJO-FALTANTE / **S1** / confirmado (por lectura y grep)
- Ubicaciones: `aplicacion/CU41RegistrarPcc01.java:56-172`, `aplicacion/CU42RegistrarRog.java:322-407`, `aplicacion/CU43RemitirReportes.java:86-89`, `README.md:28-30,83-87`, `src/main/resources/openapi/cumplimiento.yaml:9-10`
- Evidencia: grep de referencias a `CU41RegistrarPcc01` en main → solo `CU42RegistrarRog.java` (tipos de record); `CU42RegistrarRog` → ninguna; no hay `@KafkaListener` ni consumidor en el servicio; README "Eventos que consume | | |". CU-43 arma el reporte con `operaciones.pendientesDelPeriodo(...)` sobre `registro_operacion_relevante`, que en producción nunca recibe filas.
- Cómo se comprobó: grep cruzado de cada CU contra `web/`, `trabajos/` y consumidores; lectura de CU-43.
- Nota del orquestador: re-verificado: `grep -rln CU42RegistrarRog servicios/cumplimiento/src/main` → solo su propio archivo; `CU41RegistrarPcc01` solo lo usa `CU42RegistrarRog`; ningún controlador, consumidor ni trabajo los invoca. Propuesto S1; se clasifica S2 (REG alto) porque no mueve ni expone dinero: el criterio S1 del metaprompt no cubre el reporte regulatorio, pero el riesgo ante la UIF es grave y lo determina Cumplimiento.
- Impacto: el oficial "genera" y "aprueba" reportes PCC-01/ROG con `reporte_en_cero=true` cada mes aunque haya cargas/retiros de billetera ≥ USD 1.000 acumulados (inciso i, docs/Cumplimiento.md:261,289). Es un reporte falso a la autoridad firmado con segregación de funciones; el sistema además declara el CU "Implementado".
- Causa de fondo: el CU recibe el acumulado y el tipo de cambio "resueltos por quien posee las transacciones" (`CU41RegistrarPcc01.java:245-250`) pero ese productor (nucleo_financiero/aportes) no publica nada que cumplimiento consuma; nadie cableó el consumidor.
- Solución de fondo: consumidor idempotente (deduplicación por `message_id`) de los eventos de movimiento confirmado del núcleo, con el acumulado calculado por el dueño o por una proyección local; tipo de cambio desde catálogo con procedencia; bloquear `generar` de CU-43 si el consumidor tiene desfase (98.4.3).
- Relación con el flujo nuevo: FLUJO-QR (cada aporte/retiro por QR confirmado debería alimentar la detección)
- Conexiones y módulos afectados: nucleo_financiero/aportes → cumplimiento; UIF.
- Estándares y normas: 98.3.4, 98.4.3, 30 (afirmación "Implementado" sin evidencia), docs/Cumplimiento.md §2.1-2.2.
- Esfuerzo estimado: grande

### IDCU-FLUJO-FALTANTE-118 — Un oficio de autoridad con orden de bloqueo nunca bloquea nada y el acceso por oficio no queda en bitácora
- Clase / Severidad / Confianza: FLUJO-FALTANTE / **S1** / confirmado
- Ubicaciones: `aplicacion/CU45AtenderRequerimiento.java:370-385,391-400`, `web/AccionesDeCumplimientoWeb.java:60-74`
- Evidencia: el evento `cumplimiento.requerimiento_recibido` lleva `exigeBloqueo` y `justificacionDeAcceso` pero **no** `usuarioAfectadoId`; `anotarBloqueo` no tiene llamador en main; ningún servicio consume el evento (grep).
- Cómo se comprobó: lectura del payload y grep de consumidores y de `anotarBloqueo`.
- Nota del orquestador: re-verificado: `anotarBloqueo` (`CU45AtenderRequerimiento.java:128`) no tiene llamador fuera de su repositorio. Propuesto S1; S2 por el mismo criterio que el anterior.
- Impacto: una orden de congelamiento (R-BIL-14; docs/Cumplimiento.md:313 "Congelamiento y atención de requerimientos de autoridad ✅") queda registrada y respondida sin que el saldo se bloquee; el registro de acceso con justificación (R-SEG-02, `Restricciones.md:1303,1368-1376`) que "escribe auditoria" nunca ocurre.
- Causa de fondo: el diseño delega en eventos sin consumidor y sin los datos mínimos para actuar.
- Solución de fondo: comando idempotente hacia nucleo_financiero (saga con clave = id del requerimiento) con el usuario afectado en un canal autenticado entre servicios; escribir `comun.registro_acceso_datos` (cumplimiento tiene GRANT INSERT) en cada lectura bajo oficio.
- Relación con el flujo nuevo: ninguna (regulatorio) — afecta retiros del pasanaku de una persona embargada.
- Conexiones y módulos afectados: cumplimiento → nucleo_financiero, auditoria.
- Estándares y normas: 90.2.7, 98.3.2, 96.7.2.
- Esfuerzo estimado: medio

### IDCU-SEG-119 — Los "cuatro ojos" de la diligencia PEP (CU-02) los declara el cliente HTTP: un solo analista eleva el nivel con dos UUID cualesquiera
- Clase / Severidad / Confianza: SEG/APROB / **S1** / confirmado
- Ubicaciones: `web/AdhesionController.java:155-166`, `aplicacion/CU02ElevarDiligencia.java:258-266,278`, `infraestructura/DiligenciaRepositorio.java:79-92`, `sql/40_reglas/restricciones.sql:1111-1116`, `openapi/cumplimiento.yaml:1220-1234`
- Evidencia: `cuerpo.getAprobadaPor(), Optional.ofNullable(cuerpo.getSegundaRevisionPor())`; el trigger solo exige `segunda_revision_por <> aprobada_por`. Ninguna comprobación de que el llamador sea uno de los dos, de que existan, de que tengan rol, ni de que el segundo haya actuado.
- Cómo se comprobó: lectura del controlador, del CU y de `fn_uif_validar_pep`.
- Nota del orquestador: re-verificado `web/AdhesionController.java:158-165` (`cuerpo.getAprobadaPor()`, `cuerpo.getSegundaRevisionPor()`) y `aplicacion/CU02ElevarDiligencia.java:125-133` (solo exige que la segunda exista si es PEP; no la compara con la sesión ni con `aprobadaPor`). Se sostiene S1: salta el control de cuatro ojos y sube topes de operación.
- Impacto: un ANALISTA_CUMPLIMIENTO (o un token robado con ese permiso) eleva a REFORZADA/AMPLIADA a un PEP sin segunda revisión real; el nivel nuevo desbloquea topes mayores (`CU02ElevarDiligencia.java:282-287`). Firma por otro (90.1.2). El comentario de `AccionesDeCumplimiento.java:23-27` afirma que "el actor sale de la sesión" — no se aplicó aquí.
- Causa de fondo: aprobación modelada como dato de entrada y no como dos actos de dos sesiones.
- Solución de fondo: dos operaciones (aprobar / segunda revisión) cuyo actor es `ctx.usuarioId()`, con estado intermedio `PENDIENTE_SEGUNDA_REVISION` y CHECK de distinto actor; rechazar `aprobadaPor` en el cuerpo (como ya se hace en CU-43, test `web/AccionesDeCumplimientoWebTest.java:129-139`).
- Relación con el flujo nuevo: APROB (A-1: ¿KYC queda bajo R-NUEVA-1? DECISIÓN REQUERIDA; la norma citada en docs exige la doble revisión)
- Conexiones y módulos afectados: catalogo.limite_operativo_billetera (topes).
- Estándares y normas: 90.1.2, 96.5.2, R-UIF-10.
- Esfuerzo estimado: medio

### IDCU-SEG-120 — La separación "quien investiga no decide" es nominal: el analista es un UUID libre del cuerpo y abrir y decidir usan el mismo permiso
- Clase / Severidad / Confianza: SEG/DEBIDO-PROCESO / **S2** / confirmado
- Ubicaciones: `web/CumplimientoController.java:78-85,94-105`, `aplicacion/CU44InvestigarYReportar.java:108-116`, `aplicacion/CU44DecidirComoRevisor.java:347-350`, `openapi/cumplimiento.yaml:1344-1351,692`
- Evidencia: `cuerpo.getAnalistaId()` sin validar existencia, rol ni que difiera del llamador; decidir solo exige `ctx.usuarioId() != caso.analistaId()`; ambos `@Permiso("CUMPLIMIENTO_CASOS")` ("solo OFICIAL_CUMPLIMIENTO lo tiene sembrado", yaml:692). No existe paso donde el analista registre su análisis: la conclusión la escribe el revisor.
- Cómo se comprobó: lectura; `web/LftAccionesWebTest.java:88-145` usa el mismo `OFICIAL_CUMPLIMIENTO` para abrir y decidir y ninguna prueba exige que quien abre no decida.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: un oficial abre un caso nombrando a un tercero (o un UUID inexistente) como analista y lo DESCARTA él mismo: cierre de alertas LFT sin segunda identidad, que la UIF exige (docs/Cumplimiento.md:183-185). `reporteSospechosoId` tampoco se verifica (vive en auditoria).
- Causa de fondo: el control se apoya en `ck_caso_revision` (revisado_por ≠ analista_id) sin atar `analista_id` a un acto real del analista.
- Solución de fondo: el analista se asigna a sí mismo (o se valida contra identidad con rol ANALISTA y ≠ oficial), registra su informe en un estado `EN_REVISION`; solo entonces otro con permiso distinto decide; verificar el ROS por contrato con auditoria.
- Relación con el flujo nuevo: APROB (A-1)
- Conexiones y módulos afectados: auditoria (ROS), identidad (roles).
- Estándares y normas: 90.1.4, R-SEG-04, regla 91.4 (debido proceso).
- Esfuerzo estimado: medio

### IDCU-DEFECTO-121 — Declarar PEP (CU-03, endpoint expuesto) borra beneficiarios con DELETE sin privilegio y escribe tablas que la RLS no deja escribir al titular
- Clase / Severidad / Confianza: DEFECTO/DATOS / **S2** / confirmado (GRANT) · probable (RLS, no ejecutado)
- Ubicaciones: `infraestructura/DeclaracionPepRepositorio.java:46-63`, `sql/00_base/03_permisos.sql:17`, `sql/00_base/02_esquemas.sql:134-135`, `sql/40_reglas/restricciones.sql:1437-1438,1370-1392`, `src/test/.../BaseDeCumplimiento.java:131-135`
- Evidencia: `dsl.deleteFrom(... "beneficiario_final")`; `GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA cumplimiento TO svc_cumplimiento` (sin DELETE). `declaracion_pep`, `beneficiario_final`, `calificacion_riesgo_cliente`, `debida_diligencia` no están en `visibles_por_titular`, por lo que la política es "solo privilegiado o sistema" también para escribir; el endpoint corre con la sesión PARTICIPANTE. Las pruebas conectan con el usuario dueño del contenedor (bypassa GRANT/RLS), igual que el caso documentado en `restricciones.sql:1379-1388` para nucleo-financiero.
- Cómo se comprobó: lectura de GRANTs, del bucle RLS y de la base de pruebas.
- Impacto: `POST /cumplimiento/usuarios/{id}/pep` responde 500 en producción para todo participante; además el DELETE destruye el histórico de beneficiarios finales (conservación ≥10 años, docs/Cumplimiento.md:227) y la alta queda sin la recalificación que el CU promete.
- Causa de fondo: modelo "foto completa" con borrado + pruebas que no usan el rol de producción.
- Solución de fondo: beneficiarios versionados (vigente_desde/hasta, append-only) en lugar de DELETE; política RLS de escritura para el titular sobre su declaración, o ejecutar la recalificación como sistema; pruebas con `SET ROLE svc_cumplimiento` + `app.rol`.
- Relación con el flujo nuevo: ninguna directa (KYC/alta)
- Conexiones y módulos afectados: app participante (alta).
- Estándares y normas: 91.1.3 (append-only en registros probatorios), 97.1.5, 80.2.4.
- Esfuerzo estimado: medio

### IDCU-COHERENCIA-122 — `garantia` y `organizador` consultan el efecto de reclamos con el token del usuario, pero la ruta exige `RECLAMO_VER`: la respuesta es 403 y el consumidor lo traduce a "FRENA" siempre
- Clase / Severidad / Confianza: COHERENCIA/CONTRATO / **S2** / probable (depende de qué token llega en cada invocación; no ejecutado)
- Ubicaciones: `web/ReclamosController.java:93-98`, `web/ReclamosControllerWebTest.java:165-191`, `servicios/garantia/.../EfectoDeReclamosPorHttp.java:49-57`, `servicios/garantia/.../ClienteConCircuito.java:35,72`, `servicios/organizador/.../EfectoDeReclamosPorHttp.java:51`; análogo `servicios/grupos/.../HechosPorHttp.java:157-162` contra `web/LicenciaController.java:39` (`PARTICIPANTE`)
- Evidencia: "El token de quien pregunta viaja en la llamada"; la prueba web confirma 403 para `ORGANIZADOR` y para tokens `SERVICIO_*`.
- Cómo se comprobó: lectura cruzada productor/consumidor.
- Impacto: ninguna deuda se vuelve ejecutable ni sanción firme (o, si el consumidor cambiara a "PROCEDE" ante error, al revés); grupos no podría crear grupos si el organizador no trae PARTICIPANTE. El pacto entre servicios no tiene prueba de contrato del lado productor con el token real.
- Causa de fondo: autorización inter-servicio basada en el token del usuario final (98.6.3 pide token de servicio con alcance).
- Solución de fondo: token de servicio con permiso dedicado (`RECLAMO_EFECTO_LEER`) para garantia/organizador; prueba de contrato consumer-driven.
- Relación con el flujo nuevo: DEBIDO-PROCESO (bloquea cobranza/sanciones) — relevante para R-NUEVA-2 (si no hay respaldo, la cobranza es la única vía).
- Conexiones y módulos afectados: garantia, organizador, grupos.
- Estándares y normas: 98.6.3, 98.1.4.
- Esfuerzo estimado: chico

### IDCU-FLUJO-FALTANTE-123 — Ninguno de los ~30 eventos que publica cumplimiento tiene consumidor: consentimientos, limitación de cuentas, nivel de diligencia y casos LFT se pierden
- Clase / Severidad / Confianza: FLUJO-FALTANTE / **S2** / confirmado (grep)
- Ubicaciones: `aplicacion/CU05AceptarContrato.java:120-131`, `CU06RevisarConocimiento.java:403-421`, `CU02ElevarDiligencia.java:289-299`, `CU03DeclararPep.java:492-529`, `README.md:72-87`
- Evidencia: grep de cada tipo en `servicios/` y `plataforma/` fuera de cumplimiento → "(nadie)". `revision_vencida` lleva solo `cantidad` y usa la traza como `agregado_id` (`CU06RevisarConocimiento.java:415-416`): aun con consumidor no se puede saber qué cuenta limitar. `alConfirmarseCoincidencia` no se invoca (coincidencias de listas nunca abren caso).
- Cómo se comprobó: grep.
- Impacto: los consentimientos por finalidad aceptados al alta nunca se guardan en `identidad.consentimiento` (prueba de consentimiento inexistente); revisiones KYC vencidas no limitan cuentas (R-UIF-09); coincidencias de listas restrictivas no generan investigación.
- Causa de fondo: integración "por evento" declarada sin consumidor ni prueba de contrato.
- Solución de fondo: por evento, consumidor idempotente en el dueño + prueba de contrato; payload con el agregado real (usuario/cuenta), no la traza.
- Relación con el flujo nuevo: ninguna directa
- Conexiones y módulos afectados: identidad, nucleo_financiero, auditoria.
- Estándares y normas: 98.1.4, 98.8.2, 96.7.
- Esfuerzo estimado: grande

### IDCU-HUECO-124 — El "archivo" del reporte regulatorio no existe y el "envío" es una declaración manual del operador
- Clase / Severidad / Confianza: HUECO/REG / **S2** / confirmado
- Ubicaciones: `aplicacion/CU43RemitirReportes.java:97-115,168-179,242-248`, `dominio/ArchivoRegulatorio.java:21-31`, `web/AccionesDeCumplimientoWeb.java:47-57`, `application.yml:34`
- Evidencia: el contenido se arma solo para hashearlo y se descarta; `url_archivo` = `base-url + "/reportes/" + codigo + "-" + periodo` (URL del sitio público por omisión). `aceptadoPorElOrganismo` y `numeroConstancia` los teclea quien aprueba, en el mismo acto.
- Cómo se comprobó: lectura.
- Impacto: no hay archivo que remitir ni conservar (10 años); el hash no es verificable contra nada; un operador puede marcar ACEPTADO sin envío real; si alguna vez se publicara en esa URL, el reporte UIF quedaría en el sitio público.
- Causa de fondo: integración con UIF (brecha B-4, docs/Cumplimiento.md:483) no implementada; se simula con datos del operador.
- Solución de fondo: generar y guardar el archivo en almacenamiento privado con hash; registrar el envío con la constancia del canal real o un estado `PENDIENTE_CONSTANCIA` separado; nunca derivar la URL del dominio público.
- Relación con el flujo nuevo: APROB (A-1)
- Conexiones y módulos afectados: UIF.
- Estándares y normas: 90.2.4, 97.4.1 (dato presentado como real), regla 65 (declarar doble).
- Esfuerzo estimado: grande

### IDCU-DEFECTO-125 — Generar un reporte acepta `catalogoCodigo` y `formulario` independientes: se puede generar (y bloquear para siempre) el PCC-01 de un mes con registros ROG
- Clase / Severidad / Confianza: DEFECTO / **S2** / confirmado
- Ubicaciones: `web/AccionesDeCumplimientoWeb.java:37-45`, `aplicacion/CU43RemitirReportes.java:74-115,250-254`, `openapi/cumplimiento.yaml:1659`, `infraestructura/OperacionRelevanteRepositorio.java:115-129`
- Evidencia: `acciones.generarReporte(cuerpo.getPeriodo(), cuerpo.getCatalogoCodigo(), cuerpo.getFormulario().getValue(), ...)`; el filtro es `formulario LIKE ?` y nada lo ata al catálogo. Un segundo `generar` devuelve el existente (`CU43RemitirReportes.java:79-84`), y `uq_reporte_catalogo_periodo` impide regenerar.
- Cómo se comprobó: lectura.
- Impacto: reporte con contenido equivocado, no corregible sin tocar la base.
- Causa de fondo: el filtro debería ser atributo del catálogo (el propio comentario dice "Es catalogo del reporte", línea 251-252).
- Solución de fondo: derivar el filtro de `catalogo_reporte_regulatorio`; quitar `formulario` del contrato (versionado).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: UIF.
- Estándares y normas: 96.2, 96.4.
- Esfuerzo estimado: chico

### IDCU-DEFECTO-126 — `abrirHallazgo` compara solo hallazgos abiertos pero el índice UNIQUE es global, con códigos truncados que colisionan: el envío tardío o la respuesta tardía revientan con 500 y se revierten
- Clase / Severidad / Confianza: DEFECTO / **S2** / confirmado (lectura; no ejecutado)
- Ubicaciones: `infraestructura/GobiernoRepositorio.java:216-246`, `sql/30_indices/12_cumplimiento_asfi.sql:262-263`, `aplicacion/CU43RemitirReportes.java:236-240`, `CU45AtenderRequerimiento.java:416-423`, `CU56EjecutarPruebaDeContinuidad.java:97-106`, `CU52AtenderReclamo.java:213-215`
- Evidencia: `.in("ABIERTO", "EN_REMEDIACION")` antes del INSERT vs `CREATE UNIQUE INDEX uq_hallazgo_auditoria_codigo ON ... (codigo)`. Códigos: `"REP-"+periodo+"-"+8 chars` (`RECLAMOS-M` y `RECLAMOS-A` → `RECLAMOS`), `"OFI-"+16 chars del oficio`, `"BCP-"+procesoCritico` (sin truncar a 20; un proceso de >16 chars excede VARCHAR(20)), uno por proceso para siempre.
- Cómo se comprobó: lectura del índice y de los generadores de código.
- Impacto: el segundo vencimiento de un mismo código (o un hallazgo anterior ya cerrado) produce violación de unicidad → 500 → rollback del envío del reporte / de la respuesta al oficio / del registro de la prueba. El CU dice "fuera de plazo se envía igual": no se puede.
- Causa de fondo: SELECT-then-INSERT (91.2.4) + códigos no únicos por construcción.
- Solución de fondo: código derivado único (id del reporte/oficio/prueba), `INSERT ... ON CONFLICT DO NOTHING RETURNING`, y un solo repositorio de hallazgos (hoy hay dos: también `RiesgoOperativoRepositorio.java:148-193`).
- Relación con el flujo nuevo: ninguna
- Estándares y normas: 91.2.4, 96.3.2.
- Esfuerzo estimado: chico
- Conexiones y módulos afectados: ver evidencia y ubicaciones.

### IDCU-DATOS-127 — Parámetros normativos sin procedencia en código y configuración (plazos LFT, plazos de incidente, periodicidades, desvío, requisitos por nivel, roles incompatibles)
- Clase / Severidad / Confianza: DATOS/REG / **S2** / confirmado
- Ubicaciones: `ConfiguracionDeCumplimiento.java:38-107`, `application.yml` (claves `aportaya.desvio.*`, `aportaya.oficial.*`, `aportaya.reglas.techo-de-trafico`, `cumplimiento.revision.meses-riesgo-alto`, `cumplimiento.caso-lft.dias-plazo`), `aplicacion/CU54EscalarPlanesVencidos.java:116-135`, `CU43RemitirReportes.java:196`, `CU45AtenderRequerimiento.java:430`, `CU52AtenderReclamo.java:221`, `CU54RegistrarRiesgoOperativo.java:199-200`, `README.md:67-70`
- Evidencia: `"CRITICA", Duration.ofDays(5) ... "BAJA", Duration.ofDays(45)` (R-UIF-08), incidentes 24/48/72/120 h y 72 h a titulares, `PeriodicidadDeRevision(6, 12, 24)`, umbrales 100/200/500 %, `Escala(15, 8)`, `Set.of("TESORERIA", "OPERACIONES", "COMERCIAL")`; el javadoc dice "con su fuente escrita al lado" y no hay fuente. `cortesDeSeveridad = {7, 30, 90}` con comentario "viven en configuración" (falso). Plazos de hallazgo `plusDays(30)`/`plusDays(15)` literales. README admite que los plazos de incidente son "deliberadamente cortos" sin política declarada.
- Cómo se comprobó: lectura; búsqueda de procedencia en docs/Cumplimiento.md (solo los umbrales UIF tienen fuente: §2.1-2.2 y :507).
- Impacto: plazos regulatorios inventados; un incidente "en plazo" o un caso "vencido" se decide con números sin respaldo.
- Causa de fondo: falta de catálogo con procedencia para estos parámetros.
- Solución de fondo: catálogo con `base_normativa`, fuente, fecha y aprobación (como `umbral_reporte_uif`) o confirmación escrita de Cumplimiento/Legal; registrar como ambigüedad mientras tanto.
- Relación con el flujo nuevo: ninguna
- Estándares y normas: 97.5.4, 00 §2.1.
- Esfuerzo estimado: medio
- Conexiones y módulos afectados: ver evidencia y ubicaciones.

### IDCU-DATOS-128 — Semilla de umbrales UIF con `vigente_desde = current_date`: la vigencia depende del día en que se sembró, no de la norma
- Clase / Severidad / Confianza: DATOS / **S3** / confirmado
- Ubicaciones: `sql/60_semillas/06-umbrales-uif.sql:5-28` (fuera de lote, solo leído como fuente), consumido en `infraestructura/OperacionRelevanteRepositorio.java:25-44`
- Evidencia: `'Instructivo EIF art. 52 ... R.A. UIF/050/2026', current_date, TRUE`. Los valores y la cita coinciden con docs/Cumplimiento.md:249-289 (bien), pero la vigencia real es la de la R.A. (10-jun-2026 + plazo de adecuación).
- Impacto: operaciones con fecha anterior a la siembra no encuentran umbral (no se registran); reproducibilidad R-UIF-01 rota entre entornos.
- Solución de fondo: fecha de vigencia de la norma en el JSON fuente (`seeders/minimos/06-umbrales-uif.json`), con URL y fecha de obtención (97.4.2).
- Relación con el flujo nuevo: FLUJO-QR (ver hallazgo S1 de detección)
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-HUECO-129 — Controles de vencimiento escritos como consultas y nunca disparados
- Clase / Severidad / Confianza: HUECO / **S2** / confirmado
- Ubicaciones: `ReporteRegulatorioRepositorio.java:154-175` (`vencidosSinEnviar`), `RequerimientoRepositorio.java:82-99`, `ReclamoRepositorio.java:195-224`, `MonitoreoLftRepositorio.java:187-208` (`abiertasSinAnalista`), `ContinuidadRepositorio.java:80-119`, `CU06RevisarConocimiento.java:403-421`, `trabajos/` (solo 2 trabajos)
- Evidencia: grep: ninguno de estos métodos tiene llamador en main.
- Impacto: un reporte UIF no enviado, un oficio sin responder, un reclamo vencido o una alerta sin analista no generan hallazgo ni aviso; solo se detecta un envío tardío cuando alguien lo envía. `CU43Test.java:135-159` se titula "Cuando corre el control diario" pero ejercita el envío tardío.
- Solución de fondo: trabajos programados idempotentes (como CU-54/55) con visibilidad de última ejecución (96.7.4).
- Relación con el flujo nuevo: ninguna
- Esfuerzo estimado: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-130 — No hay bitácora de lectura de datos personales/financieros
- Clase / Severidad / Confianza: SEG/TRAZA / **S2** / confirmado
- Ubicaciones: `web/ReclamosController.java:66-72,93-98`, `web/UifController.java:73-80`, `web/CumplimientoController.java:116-147`; grep `registro_acceso_datos` en main → solo un comentario (`CU45AtenderRequerimiento.java:118`)
- Evidencia: el servicio tiene `GRANT INSERT` sobre `comun` pero ningún endpoint de lectura (ficha con texto del reclamo, efecto por usuario, requerimientos con `usuarioAfectadoId`) registra acceso; solo `Traza.marcarCasoDeUso`.
- Impacto: incumple 90.2.7 y la promesa de docs/Cumplimiento.md:270 ("Uso exclusivo de la información ... registro_acceso_datos ✅").
- Solución de fondo: interceptor de lectura que inserte en `comun.registro_acceso_datos` con justificación (≥10 chars, `Restricciones.md:1368-1376`).
- Esfuerzo estimado: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DEFECTO-131 — `DiligenciaRepositorio.elevarTipo` actualiza TODAS las diligencias del usuario, incluidas las históricas
- Clase / Severidad / Confianza: DEFECTO / **S3** / confirmado
- Ubicaciones: `infraestructura/DiligenciaRepositorio.java:65-70`, llamado en `CU02ElevarDiligencia.java:309`, `CU03DeclararPep.java:459-460`
- Evidencia: `.where(DSL.field("usuario_id", UUID.class).eq(usuarioId))` sin `id`.
- Impacto: reescribe el tipo de diligencias pasadas (pérdida de historia probatoria); y `vigenteDe` toma la más reciente por `iniciada_en` sin mirar estado.
- Solución: filtrar por id; nunca editar diligencias cerradas.
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-CONTRATO-132 — `Idempotency-Key` es obligatoria en el contrato y ningún endpoint la usa
- Clase / Severidad / Confianza: CONTRATO/DEFECTO / **S3** / confirmado
- Ubicaciones: `openapi/cumplimiento.yaml:1026-1033`, todos los métodos con `UUID idempotencyKey` en `web/*Controller.java`; `CU02ElevarDiligencia.java:315-321` recibe `ClaveIdempotencia` y no la usa; `CU54RegistrarRiesgoOperativo.java:134-147`; `AceptacionRepositorio.java:22-58`; `CU52AtenderReclamo.java:76-126`
- Evidencia: el parámetro se recibe y se descarta; `plataforma/comun-web/.../idempotencia/Idempotencia.java` existe y no se invoca.
- Impacto: reintento de CU-02 → 422 AP-CU02-04 (ya no es "superior"); de CU-54 → violación de `uq_evento_riesgo_operativo_codigo` sin captura (500 probable); de aceptación de contrato → dos filas append-only; `reportarIncidente`/`notificarTitulares` repetidos reescriben la fecha y re-emiten el evento (`IncidenteSeguridadRepositorio.java:124-136` sin guarda).
- Solución: usar `Idempotencia` de plataforma con `respuesta_idempotente` o guardas de estado en la escritura.
- Estándares y normas: 96.3.3, 96.4.1.
- Esfuerzo estimado: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.

### IDCU-CLEAN-133 — La versión insegura de la decisión LFT (`CU44InvestigarYReportar.decidir`) sigue viva
- Clase / Severidad / Confianza: CLEAN/SEG / **S3** / confirmado
- Ubicaciones: `aplicacion/CU44InvestigarYReportar.java:145-225`
- Evidencia: revisor, analista y alertas del llamador; `if (entrada.revisadoPor() != null && ...)` deja pasar `null`; cierra alertas por id sin comprobar que sean del caso. Solo la usan pruebas (`CU44Test.java:97-268`).
- Impacto: código muerto con el agujero que `CU44DecidirComoRevisor` corrige; las pruebas "transversales" de CU-44 validan esta versión y no la expuesta.
- Solución: borrar el método y migrar las pruebas a `CU44DecidirComoRevisor`.
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DUP-INT-134 — Duplicaciones internas con semántica divergente
- Clase / Severidad / Confianza: DUP-INT / **S3** / confirmado
- Ubicaciones: `CU41RegistrarPcc01.java:73-89` vs `dominio/UmbralAlcanzado.java:10-45` ("una sola implementación para el PCC-01 y para los ROG" — CU-41 no la usa); `UmbralAlcanzado.ventanaDesde` sin uso; `RevisionKycRepositorio.abrirAlerta` vs `MonitoreoLftRepositorio.levantarAlerta` (sin uso); `GobiernoRepositorio.abrirHallazgo` vs `RiesgoOperativoRepositorio.abrirHallazgo`/`hallazgoConCodigo` (uno filtra estados, el otro no); `CU94ElevarAlComite.java:128-131` aprueba evaluaciones sin la verificación de no objeción de `CU47EvaluarRiesgoDeProducto.java:163-170` e ignora el booleano de `evaluaciones.aprobar`.
- Impacto: dos verdades para el mismo cálculo regulatorio; CU-94 puede aprobar un producto que exige no objeción sin tenerla.
- Esfuerzo estimado: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DINERO-135 — Importes sin moneda y tipo de cambio sin fuente
- Clase / Severidad / Confianza: DINERO / **S3** / confirmado
- Ubicaciones: `CU40EvaluarLimites.java:529` (`EntradaLimite` sin moneda; evento `monto` sin moneda :511), `dominio/EvaluacionDeLimites.java:24-49`, `CU52AtenderReclamo.java:251-258` (monto reclamado; declarado A-7 en `ConsultarBandejaDeReclamos.java:95`), `CU53ElevarReclamo.java:162-163` (`montoResarcido`), `CU56EjecutarPruebaDeContinuidad.java:133` (`perdidaBruta` en el evento sin moneda), `ConsultarTableroDeCumplimiento.java:55` (`montoTotal`), `CU41RegistrarPcc01.java:64-74,257`
- Evidencia: `tipoDeCambio` llega en la entrada del llamador y se multiplica (`monto.multiply(tipoDeCambio)`) sin catálogo ni fecha; la semántica (USD por unidad vs unidades por USD) no está declarada.
- Impacto: un tipo de cambio invertido (6,96 vs 0,1437) desplaza el umbral ×48; reproducibilidad R-UIF-04 solo aparente.
- Estándares y normas: 91.1.2, 97.5.4.
- Esfuerzo estimado: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.

### IDCU-SEG-136 — Controles regulatorios que aceptan autodeclaraciones booleanas (hoy sin entrada HTTP)
- Clase / Severidad / Confianza: SEG / **S3** / confirmado
- Ubicaciones: `CU47EvaluarRiesgoDeProducto.java:158-170,202-203` (`comiteConQuorum`, `hayNoObjecion`), `CU48CalibrarReglas.java:118-143,206-207` (`hubosimulacion`, `superaTecho`, conteos), `CU49DesignarOficial.java:76-83,196-201` (`rolesDelDesignado`), `CU53ElevarReclamo.java:80-84` (`hayEvidenciaTecnica`), `CU52AtenderReclamo.java:136-143` (`comunicadaAlCliente`), `CU41RegistrarPcc01.java:263-266` (`exento`, `motivoDeExencion`)
- Impacto: si se exponen tal cual, cada control se salta enviando `true`. Exponerlos exige derivar esos datos del servidor.
- Esfuerzo estimado: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-137 — `GET /licencia/alcance` escribe en el outbox por cada rechazo, con un `usuarioId` arbitrario del query
- Clase / Severidad / Confianza: SEG/OBSERV / **S3** / confirmado
- Ubicaciones: `web/LicenciaController.java:38-45`, `aplicacion/CU46VerificarAlcance.java:87-105`
- Evidencia: `Optional.ofNullable(usuarioId)` del query se usa como `agregado_id` del evento; cualquier PARTICIPANTE puede generar eventos atribuidos a otra persona y amplificar escrituras con GETs. `usuariosRegistrados` siempre 0: el cupo de sandbox nunca se agota (`LicenciaController.java:22-25`).
- Además: `exigirHabilitado` lanza tras emitir en la misma transacción → el evento se revierte, contra el comentario (:88-92). `codigoDe` decide el código de error por substring del mensaje (:131-143) — FRAGIL.
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-FRAGIL-138 — Evidencia y parseos frágiles
- Clase / Severidad / Confianza: FRAGIL / **S3** / confirmado
- Ubicaciones: `web/AdhesionController.java:115-116` (IP = `peticion.getRemoteAddr()`; sin `server.forward-headers-strategy` en `application.yml`, detrás del gateway es la IP del gateway; `dispositivoId` siempre vacío); `GobiernoRepositorio.java:47-55` y `LicenciaRepositorio.java:61-73` (JSON parseado con regex/split); `DeclaracionPepRepositorio.java:73-74` (`LocalDate.now()` de la JVM, no `Reloj` ni La Paz); `CU52AtenderReclamo.java:46` (`new BandejaDeReclamosRepositorio()` fuera de DI); `CU41RegistrarPcc01.java:97` (NPE si `ventanaDias` es null en umbral acumulado); `CU41RegistrarPcc01.java:188-241` (`declarar` usa un ternario sobre `null` siempre verdadero).
- Impacto: el hash de evidencia de aceptación del contrato (CU-05) sella una IP que no es la del firmante.
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DEFECTO-139 — Escalado de incidentes vencidos puede quedar en inanición
- Clase / Severidad / Confianza: DEFECTO / **S3** / probable
- Ubicaciones: `CU55EscalarIncidentesVencidos.java:67-74`, `IncidenteSeguridadRepositorio.java:178-197`
- Evidencia: a diferencia de CU-54 (que marca el plan VENCIDO, `CU54EscalarPlanesVencidos.java:81`), los incidentes no se marcan; con más de `tope-por-corrida` (200) vencidos que ya tienen hallazgo, la misma ventana ordenada por `plazo_reporte` se reselecciona cada día y los nuevos nunca se escalan.
- Solución: excluir los que ya tienen hallazgo en la consulta o marcar.
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DEBIDO-PROCESO-140 — Pequeñas fugas hacia el titular o fuera del equipo LFT
- Clase / Severidad / Confianza: DEBIDO-PROCESO/UX / **S3** / confirmado
- Ubicaciones: `web/AdhesionController.java:150-151` (devuelve `nivelRiesgo` ALTO/MEDIO/BAJO al participante), `CU02ElevarDiligencia.java:233-237` (mensaje "Hay una coincidencia de lista sin resolver" al analista de KYC), `CU03DeclararPep.java:509-526` (resumen del caso con `nombreCoincidente` y evento `caso_lft_abierto` con `usuarioId`, en contra del criterio de `CU44InvestigarYReportar.java:136-137`)
- Impacto: riesgo de revelar a la persona su calificación y la existencia de investigación (deber de reserva).
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-PRUEBAS-141 — Pruebas plantilla con nombres que no corresponden y afirmaciones que no se verifican; integración sin el rol de producción
- Clase / Severidad / Confianza: PRUEBAS / **S2** / confirmado
- Ubicaciones: 18 archivos con "cuadre: la suma de debitos iguala la de creditos" y "compensacion ..." (p. ej. `CU44Test.java:184-269`); 18 con "la misma clave de idempotencia" sin clave (`CU44Test.java:129-150`); `CU06Test.java:89-111` ("la cuenta_billetera queda LIMITADA" — solo cuenta el evento), `CU02Test.java:89-100`, `CU43Test.java:135-159` ("control diario" inexistente), `CU41Test.java:103-118` (el reinicio de ventana lo provee el test con acumulado 0); `BaseDeCumplimiento.java:131-135` (conexión como dueño: sin GRANT ni RLS); pruebas web con casos de uso doblados.
- Impacto: la suite está verde con funciones rotas en producción (CU-03 DELETE/RLS, CU-40 GRANT) y títulos que inducen a creer cubierto lo que no lo está (regla 30, "equivalencias falsas").
- Solución: renombrar/eliminar plantillas, pruebas de autorización y persistencia con `SET ROLE svc_cumplimiento` + `app.rol` real (patrón ya usado en `AislamientoEsquemaTest`).
- Estándares y normas: 80.2.4, 80.5.4, 30.
- Esfuerzo estimado: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.

### IDCU-DOC-142 — README y descriptor afirman más de lo que existe
- Clase / Severidad / Confianza: DOC / **S3** / confirmado
- Ubicaciones: `README.md:20-39` ("Implementado" para CU-06/40/41/42/47/48/49/53/94 sin entrada), `README.md:72-81` (lista 6 eventos de ~30), `README.md:83-87` (consume: vacío), `descriptor.yml:10` ("monitorear y alertar llega por evento": no hay consumidor), `ConfiguracionDeCumplimiento.java:31` ("con su fuente escrita al lado").
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-RENDIMIENTO-143 — Listados sin límite y alertas no idempotentes
- Clase / Severidad / Confianza: RENDIMIENTO/DATOS / **S4** / confirmado
- Ubicaciones: `ReclamoRepositorio.java:231-258` (`casosDelUsuario` sin paginación, 96.6.1); `CU06RevisarConocimiento.java:336-346` (cada corrida abre una alerta nueva aunque el desvío del mes sea upsert); `dominio/DesvioDePerfil.java:44-46` y `RiesgoDelProducto.java:102-105` (`IllegalArgumentException` → 500 en vez de error de contrato); `CU54RegistrarRiesgoOperativo.java:168-177` y `CU55GestionarIncidente.java:226-238` (huella de 24 bits como clave de unicidad: colisión rechaza un hecho legítimo).
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

## 6. Matriz de contratos

Pendiente.

## 7. Evidencia obligatoria del módulo

Pendiente.

## 8. Cobertura por clase

Pendiente.

## 9. Lo que está bien

- No hay confianza en cabeceras de identidad sin firmar: `grep -rniE 'X-User|X-Usuario|X-Actor|X-Rol'` sobre identidad, cumplimiento y `comun-web` → sin resultados; la identidad se resuelve del JWT validado (`SesionDeLaPeticion.java:66-84`).
- El emisor de tokens falla al arrancar sin clave en `staging`/`production` (`EmisorDeAcceso.java:75-79`).

## 10. Observado fuera de alcance

- `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU40EvaluarLimites.java:80,132` — `reloj.ahora().atOffset(ZoneOffset.UTC)` para la vigencia de límites (mismo patrón que IDCU-DEFECTO-003; lote L1).

- **nucleo-financiero / recarga (lote L1)** — La recarga la acredita TESORERIA a mano; el catálogo dice que la acredita el proveedor con firma: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/BilleteraController.java:141-148`. Acredita saldo por acción de TESORERÍA y no por webhook firmado; choca con 91.3.1 y R-NUEVA-1. Propuesto S1 por el subagente; no es de este módulo.

## 11. Límites de esta auditoría

- Gradle no corrió: `./gradlew :servicios:identidad:testBarrido` → `BUILD FAILED`, «Received status code 429 from server: Too Many Requests» al resolver dependencias de `buildSrc` (`evidencia/gradle-identidad-testBarrido.txt`). Además la generación jOOQ exige PostgreSQL con el esquema aplicado (`buildSrc/src/main/kotlin/aportaya.jooq.gradle.kts:22`) y el metaprompt prohíbe aplicar migraciones. Todas las pruebas Java del módulo y el barrido de tamaño/umbrales/secretos quedan **NO COMPROBADO**.
- `yarn typecheck` (backoffice) no corrió: «packageManager yarn@4.18.0 … current global version of Yarn is 1.22.22» (Corepack deshabilitado; no se instaló nada). Flutter/Dart no están instalados. **NO COMPROBADO**.
- No se ejecutaron `scripts/inventario_endpoints.py` ni `scripts/matriz_de_pruebas.py` sin `--check`/`--servicio` porque escriben archivos del repo; `inventario_endpoints.py --check` sí corrió.

## 12. Anexo de cobertura de archivos

Pendiente.
