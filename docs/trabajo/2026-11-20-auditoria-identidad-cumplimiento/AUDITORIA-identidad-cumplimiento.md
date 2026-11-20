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
| APR-16 | `apps/backoffice/src/app/rutas/cumplimiento/organizadores/pantalla-de-habilitacion.ts:80-95`; `dominio/cu90-habilitacion.ts:33-39` | Operador de plataforma habilita a un organizador tras la capacitación | `POST /organizadores/{id}/habilitacion` (servicio organizador, `ADMIN_PLATAFORMA`); ruta sin guardia (tanda 6) | DECISIÓN REQUERIDA (A-1; ADR-054 ya quitó al organizador humano del alta y del inicio; la decisión sobre la habilitación es del lote L3) | creación de grupos con organizador |
| APR-17 | `apps/web/src/app/paginas/invitacion/invitacion.ts:15,52-53,97` | Texto público al invitado: la admisión «la resuelve una persona de AportaYa» | texto; la admisión vive en grupos (lote L3) | ELIMINABLE el texto si R-NUEVA-1 quita la admisión manual; DECISIÓN REQUERIDA en L3 | el texto queda falso |

Pantallas del backoffice que ejecutan las filas anteriores (tanda 6): APR-01 → `apps/backoffice/src/app/rutas/cumplimiento/verificaciones/vista-decision-del-expediente.ts:68-82`; APR-07 → `operacion/retiros/pantalla-de-aprobacion-de-retiro.ts:150-171` y `bandeja-de-retiros.ts:87-102`; APR-09 → `cumplimiento/uif/aprobar-y-enviar.ts:65-90`; APR-10 → `cumplimiento/alertas/abrir-caso.ts:56-77`, `cumplimiento/casos/decidir-caso.ts:78-102`; APR-11 → `cumplimiento/uif/registrar-requerimiento.ts:57-78`, `responder-requerimiento.ts:41-59`; APR-04 → `sistemas/usuarios-internos/modal-asignar-roles.ts:73-88`.

Total: **17 filas** (≈ 30 puntos de código). DECISIÓN REQUERIDA: APR-01, APR-03, APR-07, APR-12, APR-15, APR-16 (6). CONSERVAR / CONSERVAR POR NORMA: 9 (APR-04, -06, -08, -09, -10, -11, -13, -14; APR-05 NO APLICA). ELIMINABLE: APR-02 como vía de activación y APR-17 como texto (2).

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA**. Evidencia: `pantalla_subida_del_expediente.dart:101-110` dispose→`liberarCapturas()`; `seguimiento_del_alta.dart:62-71` vacía las capturas y llama `eliminarArchivosLocales()`; `:157-175` «Continuar» solo se deshabilita con `subiendoAlguna`; `kyc_del_titular.dart:28-39` `pendientesDeKyc` solo pide los perfiles y la fecha; `pantalla_completar_kyc.dart:127-153` solo ofrece los perfiles. Búsqueda de un camino alternativo: `/registro/subida` solo se alcanza desde el alta (`pantalla_registro.dart:113`) o desde `PantallaVerificarIdentidad` (`:87`), que es para quien todavía no tiene verificación (`aviso_de_verificacion.dart:70-73`, con `sinVerificar`). `pantalla_estado_de_verificacion.dart` no tiene ninguna acción para volver a subir. Corrección: Ninguna. S2/probable es razonable.

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA**. Evidencia: `sql/40_reglas/restricciones.sql:528-529` dice «Las órdenes ya autorizadas siguen su curso», pero `:532-534` solo exceptúa `BORRADOR`/`RECHAZADA` y `:552-554` `BEFORE INSERT OR UPDATE OF estado`. La subconsulta de `:535-539` no filtra por moneda. Java filtra `WHERE cc.moneda = ?` (`OrdenRetiroRepositorio.java:264-278`). No hay otra definición de la función (grep en sql/ y servicios/). Corrección: Además quedan bloqueadas `EN_PROCESO→REVERSADA` y la transición a `PAGADA`. `CU11InstruirRetiro.java:63` también chequea por moneda, lo que confirma la incoherencia.

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA**. Evidencia: `ResolucionDeRetiro.java:101-114` tiene dos patas por `orden.solicitado()`. Al proveedor se le instruye `envio.neto()` (`RetirosConProveedor.java:77`). Ningún código de `servicios/*/src/main` inserta en `cargo_comision` (grep: solo DDL y `sql/62_volumen`). El evento `retiro_pagado` no lleva el costo y no tiene consumidores fuera del emisor. CU-11 (`:47-53`) exige `cargo_comision`. Corrección: Ninguna. Se puede subir la confianza a «confirmado (lectura)».

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA**. Evidencia: `sql/15_infra/borradores.sql:6` `correo VARCHAR(150) PRIMARY KEY`, `:12` `numero_documento VARCHAR(30)`, `:19` `monto_mensual_estimado TEXT`. El repositorio, en `:44,103,113,177`, enlaza `numeroDocumento()` sin transformarlo (`:131`). Corrección: Matiz: `scripts/generar_ddl.py:1195` declara a propósito «No es una entidad del modelo». La parte de la «segunda fuente de verdad» (97.1.1) es una decisión documentada, no una omisión. Hay que reformularla como divergencia declarada. Se solapa con DATOS-082.

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA**. Evidencia: `sql/61_dev/15-usuarios-dev.sql:7-9` tiene nombre, apellidos, fecha de nacimiento, teléfono y correo con aspecto real (valores no reproducidos). `:52,54` repiten los correos en `canal_vinculado`. El JSON fuente solo trae `descripcion/nota/advertencia` («SOLO DEV»), sin declarar que los datos son sintéticos ni que hubo consentimiento. Corrección: Las cuentas parecen ser del propio responsable del proyecto. Eso podría bajar el impacto, pero no exime de 97.6.1. Mantener S2/probable.

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA CON CORRECCIÓN**. Evidencia: `06-umbrales-uif.sql:5-28` usa `base_normativa 'Instructivo EIF …'`, `current_date` y `TRUE`. El JSON dice `"estado": "confirmar vigencia con legal"` (`:4`) y el de 07, `"confirmar plazos con legal"`. `19-reportes-y-retencion.sql:67` contiene `PEP … >= 5000 … TRUE`. `seeders/README.md:48` marca 08 como «listo». Corrección: Las líneas de `08-gobierno-y-licencia.sql` están mal: el archivo tiene 43 líneas; la matriz está en `:27-35` y las tipologías en `:37-41`. `matriz_riesgo_lft` **no tiene columna `activa`** (`10_tablas/12_cumplimiento_asfi/matriz_riesgo_lft.sql:5-17`): usa `vigente_desde current_date` y `aprobada_por` NULL implícito. Las tipologías (`regla_monitoreo_lft`) sí usan `activa TRUE`, pero con `vigente_desde now()`, no `current_date`. La severidad no cambia.

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA CON CORRECCIÓN**. Evidencia: `sql/00_base/03_permisos.sql:17,42` dan `GRANT SELECT, INSERT, UPDATE` sobre todo el esquema. Los únicos REVOKE en esos esquemas son para `acta_comite`, `evento_riesgo_operativo`, `registro_operacion_relevante`, `bitacora_evento` y `registro_acceso_datos` (`:108-174`). No hay ningún trigger sobre las 9 tablas citadas (grep de TRIGGER en sql/). Corrección: Hay que acotar la lista. `alerta_monitoreo_lft` y `caso_investigacion_lft` tienen una columna `estado` con flujo de trabajo, y `consentimiento` tiene `revocado_en` (`consentimiento.sql:15`), así que el UPDATE es legítimo por diseño. Ahí la solución es sellar columnas o estados terminales con un trigger, no el REVOKE de UPDATE. Para `intento_*`, `historial_credencial`, `aceptacion_contrato` y `envio_regulatorio` el hallazgo queda en pie.

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA**. Evidencia: `VerificacionCorreoRepositorio.java:183-186`: `if ("CONSUMIDO"…) { if (coincide) return borradores.resolverResultado(…); throw …}`, sin `intento(...)` ni incremento. En esa rama tampoco se controla `expira_en`. La ruta es `@Publico` (`UsuariosController.java:261-262`). El límite está solo en el gateway: `rutas.yml:38-41`, 5/s con ráfaga de 10. Corrección: Para encontrar el token hace falta el `verificacionId` y que la huella del correo coincida (`:176-181`). Mencionarlo como precondición.

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA**. Evidencia: `BorradorOnboardingRepositorio.java:100-141` inserta `numero_documento`, `origen_de_fondos` y `monto_mensual_estimado` tal como llegan. El javadoc de `DocumentoDeIdentidad.java:13` dice «El numero nunca viaja ni se guarda en claro». Corrección: Es un duplicado sustancial de SEG-044 (misma tabla, misma causa). Conviene fusionarlos o enlazarlos para no contar dos veces.

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA**. Evidencia: `MapeoDelAlta.java:59-62` devuelve `"cifrado:" + documento.hashNumero()`. `RegistroRepositorio.java:158-171` hace `.set(NUMERO_CIFRADO, numeroCifrado)` y `VERSION_LLAVE (short) 1`. `DocumentoDeIdentidad.java:63,75-90` aplica SHA-256 a `numero + pimienta`. Todos los usos de `numeroCifradoDe` (`MapeoDelAlta:32`, `ExpedienteController:163`) pasan el mismo valor; no hay ningún adaptador de cifrado.

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA**. Evidencia: `RegistroRepositorio.java:198-224` usa `DSL.val("1")` y `DSL.val("0".repeat(64))`. `aceptaContratos` solo se usa en `CU01RegistrarUsuario.java:150` (`isEmpty`). `MapeoDelAlta.java:37` pasa `true` literal para `licenciaHabilitaBilletera`. En la app, `panel_de_aceptacion.dart:57-61` dice: «Cuando exista el cliente de cumplimiento… llama a POST /contratos/{id}/aceptaciones». `CU05AceptarContrato` existe en cumplimiento, pero no se invoca en el alta. Corrección: El `true` literal está en `:37`, no en `:38` (diferencia menor).

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA**. Evidencia: `RevisionRepositorio.java:174-235` no tiene outbox ni EventoDominio. Los `new EventoDominio` de identidad están solo en CU01, CU04, CU08, CU09 y GestionarUsuariosInternos (grep); no existen `kyc_*` ni `verificacion_*`. No hay trigger en `sql/40_reglas` sobre `verificacion_kyc` que emita eventos. El único consumidor de `usuario_registrado` es `ConsumidorDeUsuariosKafka.java:33` (@KafkaListener).

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA CON CORRECCIÓN**. Evidencia: `ExpedienteRepositorio.java:47-75` hace `update(...).where(USUARIO_ID.eq(usuarioId))`, sin condición de estado. `TitularDelExpediente.java:32-37` acepta el token de alta o la sesión propia, sin mirar el estado. `CU02GuardarFotoDelExpediente.java:42-48` tampoco valida el estado. No hay trigger ni restricción en sql/ sobre `verificacion_kyc` o `documento_identidad` que lo impida. Corrección: La ubicación `AnotarFotoEnExpediente.java:91-106` no existe: el archivo tiene 48 líneas y lo relevante está en `:32-47`. La ruta del controlador está bien (`ExpedienteController.java:87-110`). La severidad no cambia.

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA CON CORRECCIÓN**. Evidencia: `RevisionRepositorio.java:216-235` inserta `'GLOBAL', NULL` con `otorgada_por = revisor`. `FirmaDeOrganizadorRepositorio.java:37-53` usa como otorgante del ORGANIZADOR a quien aprobó el KYC, y `:104-126` asigna `'GLOBAL', NULL`. La semilla `10-roles-y-permisos.sql:18-19` declara ORGANIZADOR y PARTICIPANTE con ámbito `GRUPO`. `ck_asignacion_ambito_completo` (`restricciones.sql:1621-1623`) no cruza `rol.ambito` con `asignacion.ambito`, así que la base lo acepta. Corrección: Para PARTICIPANTE, que el revisor figure como otorgante es una decisión documentada (javadoc `:216-217` «El revisor queda como otorgante») y además lo fuerza `ck_asignacion_no_autoasignada` (`:1620`). La parte «a nombre de quien no lo decidió» vale de lleno solo para ORGANIZADOR. El núcleo sólido del hallazgo es el ámbito GLOBAL.

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA CON CORRECCIÓN**. Evidencia: Las clases `CU08AsignarRol`, `CU08RevocarRol`, `CU09CambiarCredencial` y `CU09SolicitarBaja` solo se referencian a sí mismas (grep en src/main). En los openapi de todos los servicios no hay rutas de credencial, baja, recuperación ni enrolamiento MFA. `README.md:21-24` marca CU-08 y CU-09 como «Implementado». Los mensajes «Podés recuperar el acceso» están en `CU01RegistrarUsuario.java:128,139,147`. Corrección: CU-08 para operadores **sí tiene endpoint**: `PUT /usuarios-internos/{id}/roles` (`identidad.yaml:914-934`, «CU-08»), que llega a `GestionarUsuariosInternos.asignarRoles` (`:103`). Lo que falta son las clases CU08 (código muerto) y la revocación como operación propia. Hay que reformularlo así: «CU-09, recuperación, baja y MFA sin endpoint; CU-08 expuesto por otra vía; clases CU08/CU09 sin uso».

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
- Refutación (fase 7, verificador independiente A): **CONFIRMA**. Evidencia: `UsuariosInternosRepositorio.java:257-268` solo limpia `usuario_interno.bloqueado_hasta` y `motivo_bloqueo`. El login consulta `bloqueo_cuenta` (`UsuarioRepositorio.java:78-83`, que usa `CU04Autenticar.java:103`) y ese bloqueo lo crea `AccesoRepositorio.java:158-164`. En `:181-193`, `provisional` se hashea y no se devuelve, ni se envía, ni se vuelve a usar (grep de `provisional`). Corrección: Precisión: el bloqueo por intentos tiene `desbloquea_en` y vence solo. El impacto real es que el operador no puede levantarlo antes de tiempo, no un bloqueo eterno.

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA**. Evidencia: `I/web/VerificacionesController.java:59-95` (`verFotoDelExpediente`/`verContenidoDeFoto`) solo `@Permiso` + `revision.foto/contenido`; `I/aplicacion/CU02RevisarExpediente.java:68-80` lee del almacén sin registrar nada; `identidad.yaml:278,299,331` promete "queda registrada"; `I/infraestructura/ExpedienteRepositorio.java:17` repite la promesa en javadoc. `grep -rni "acceso_datos\ Corrección: registrarLectura" I/ plataforma/` → 0 en main (solo tests). No hay filtro/interceptor en `plataforma/comun-web` que lo haga.

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA**. Evidencia: `I/aplicacion/SolicitarVerificacionCorreo.java:40-48` → `ErrorDeNegocio(1,3,…, campo=correo)`; `I/aplicacion/CU01RegistrarUsuario.java:125-149` → tres rechazos con `campo` telefono/documento/correo; `I/aplicacion/CU04Autenticar.java:96-101` → `usuarioId.isEmpty()` retorna antes de cualquier hash (sin hash señuelo). Corrección: Ninguna (confianza "lectura; tiempo no medido" es correcta).

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA**. Evidencia: `I/aplicacion/VerificarTitularidad.java:62-74` solo exige `d.hash_numero` no nulo; `I/infraestructura/RegistroRepositorio.java:158-170` inserta el documento con `estado='EN_REVISION'` ya en el alta (CU01:226) → el cotejo da `true` sin KYC aprobado. Sin UNIQUE en `documento_identidad(usuario_id)` (`sql/30_indices/01_identidad_usuarios.sql:85-89` solo índice simple) y hay dos caminos de inserción (CU01:226, CU02IniciarVerificacion:69) → `fetchOne()` puede lanzar `TooManyRowsException`; el manejador (`ManejadorGlobalDeErrores.java:134-160`) solo traduce si hay SQLSTATE, si no cae al genérico 500. CU18 de entregas no comprueba KYC por su cuenta (`CU18RegistrarCuentaDestino.java:76-81` confía en el cotejo). Corrección: Ninguna; confianza "probable" adecuada para el 500.

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA CON CORRECCIÓN**. Evidencia: `I/aplicacion/BuscarPorTelefono.java:35-38` usa `ContextoSesion.deSistema`; `I/web/UsuariosController.java:151-170` sin `LimiteDeConsultas` (solo titularidad lo tiene, :218); `I/aplicacion/EmitirTokenDeInvitacion.java:44-147` no consulta a grupos ni verifica propiedad de `grupoId`; además `revocarVivas` (:150-160) revoca tokens vivos del grupo ajeno + teléfono. ORGANIZADOR tiene `GRUPO_ADMINISTRAR` (`sql/60_semillas/10-roles-y-permisos.sql:152`) y se autoconcede GLOBAL (`FirmaDeOrganizadorRepositorio.java:104-126`). Corrección: Rango erróneo: `EmitirTokenDeInvitacion.java` tiene 175 líneas → citar `44-167`. El tope del gateway (`rutas.yml:32-42`) es 5/s con clave **IP+usuario** (`LimitadorDeTasa.java:28`), no "por IP". Agregar impacto: puede revocar invitaciones vigentes de un grupo ajeno. Severidad S2 se mantiene.

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA**. Evidencia: `I/infraestructura/DesafioLocal.java:39-40` `ConcurrentHashMap`; `I/aplicacion/CU04StepUp.java:42` `abiertos` en memoria; `I/web/LimiteDeConsultas.java:32` `porActor` en memoria; `servicios/identidad/descriptor.yml:12-14` `min: 4`. Única alternativa `DesafioDeDesarrollo` es `@Profile("local")`. Sin afinidad de sesión en gateway (grep `sticky Corrección: affinity` → 0).

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
- Clase / Severidad / Confianza: FLUJO-FALTANTE / S2 / confirmado (por lectura y grep)
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
- Clase / Severidad / Confianza: FLUJO-FALTANTE / S2 / confirmado
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
- Refutación (fase 7, verificador independiente B): **CONFIRMA**. Evidencia: `C/infraestructura/DeclaracionPepRepositorio.java:46-49` `deleteFrom(beneficiario_final)`; `sql/00_base/03_permisos.sql:17` y `02_esquemas.sql:134-135` solo `SELECT, INSERT, UPDATE` (DELETE nunca concedido; en Postgres el DELETE falla aunque no haya filas). `beneficiario_final`/`declaracion_pep` fuera de `visibles_por_titular` (`restricciones.sql:1370-1392`) → rama "denegar por omisión" (`:1437-1438`). Endpoint `@Permiso("PARTICIPANTE")` (`C/web/CumplimientoController.java:167-170`), CU03 usa `conContexto(ctx)` sin `comoSistema` (:104-113). Pruebas conectan como dueño del contenedor (`BaseDeCumplimiento.java:131-135`).

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA CON CORRECCIÓN**. Evidencia: `C/web/ReclamosController.java:93-95` `@Permiso("RECLAMO_VER")`; web test `:165-191` da 403 a ORGANIZADOR y SERVICIO_*. Garantía llama desde `IncumplimientosController.java:169-194` con `@Permiso("GRUPO_ADMINISTRAR")` y propaga el token del usuario (`ClienteDeServicio.propagarElToken`); ORGANIZADOR no tiene `RECLAMO_VER` (semillas :174-216 solo soporte/cumplimiento/collections/admin) → 403 → `orElse(FRENA)` (`EfectoDeReclamosPorHttp.java:49-57`). Organizador idéntico (:47-55). Corrección: El **análogo de grupos no resiste**: `FirmaDeOrganizadorRepositorio.java:46` exige rol PARTICIPANTE antes de conceder ORGANIZADOR, así que el organizador siempre trae PARTICIPANTE y `LicenciaController.java:39` no le da 403. Quitar ese análogo y la frase de impacto "grupos no podría crear grupos". Matiz: un usuario con GRUPO_ADMINISTRAR **y** RECLAMO_VER (p. ej. ADMIN_PLATAFORMA) sí pasa. S2 se mantiene.

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA CON CORRECCIÓN**. Evidencia: 35 tipos `cumplimiento.*` emitidos; `grep` de esos tipos y de `aportaya.cumplimiento` fuera de `servicios/cumplimiento/src/main` → 0; único `topicPattern` es `indicador_calculado` (auditoría). `README.md:80-84` "Eventos que consume" vacío. `alConfirmarseCoincidencia` (`CU03DeclararPep.java:172`) solo se invoca desde `CU03Test`. `revision_vencida` lleva `agregado_id = traza` y solo `cantidad` (`CU06RevisarConocimiento.java:190-197`). Corrección: Líneas erróneas: CU06 tiene 212 líneas (citar `184-200`, no 403-421/415-416); CU02ElevarDiligencia tiene 194 (citar `110`, `156`, no 289-299); CU03DeclararPep tiene 219 (citar `146`, `192`, no 492-529). Impacto exagerado en consentimientos: identidad **sí** guarda `TERMINOS`, `PRIVACIDAD`, `TRATAMIENTO_DATOS` en el alta (`I/aplicacion/CU01RegistrarUsuario.java:68,229` → `RegistroRepositorio.java:200-222`, aunque con `hash_documento` de 64 ceros); lo que se pierde son las finalidades emitidas por CU-05. Además `vencerYPedirLimitacion` no tiene llamador en main, así que `revision_vencida` ni se emite. S2 se mantiene.

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA**. Evidencia: `C/aplicacion/CU43RemitirReportes.java:97-102` arma el contenido solo para `hashDe`; `:242-248` URL = `baseUrlDeArchivos + "/reportes/"…`; `application.yml:34` `base-url: ${BASE_URL_PUBLICA:https://aportaya.bo}`; `:168-179` `ACEPTADO`/`numeroConstancia` salen de la entrada del operador (`AccionesDeCumplimientoWeb.java:47-57`). Sin almacén en CU43 (grep `almacen` → 0).

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA**. Evidencia: `AccionesDeCumplimientoWeb.java:37-45` pasa `catalogoCodigo` y `formulario` independientes; `AccionesDeCumplimiento.java:58-64` no los relaciona; `CU43RemitirReportes.java:74-84` devuelve el existente; `OperacionRelevanteRepositorio.java:115-129` `formulario LIKE ?`; `cumplimiento.yaml:1659` enum libre.

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA CON CORRECCIÓN**. Evidencia: `GobiernoRepositorio.java:216-246` precheck solo `ABIERTO/EN_REMEDIACION`; `sql/30_indices/12_cumplimiento_asfi.sql:262-263` UNIQUE global; `codigo VARCHAR(20)`; semillas `RECLAMOS-M`/`RECLAMOS-A` colisionan al truncar (`CU43:236-240`); `BCP-`+`proceso_critico VARCHAR(80)` sin truncar (`CU56:97-106`); `REC-`+8 (`CU52:213-215`). Corrección: (1) Línea errónea: el hallazgo de CU45 está en `CU45AtenderRequerimiento.java:149-165` (el archivo tiene 196 líneas). (2) **No es 500**: `ManejadorGlobalDeErrores.java:134` + `FallaDeLaBase.java:24` traducen 23505 a **409 AP-BD-01**, y el desborde de VARCHAR a 422 AP-BD-03; el rollback de la operación sí ocurre. Severidad S2 se mantiene (el envío tardío sigue siendo imposible).

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA CON CORRECCIÓN**. Evidencia: `ConfiguracionDeCumplimiento.java:47-101` literales (6/12/24, 5/15/30/45 d, 24/48/72/120 h, Escala(15,8), Set roles); javadoc :24 "con su fuente escrita al lado" sin fuente; `CU54EscalarPlanesVencidos.java:117,135` `{7,30,90}` con comentario "viven en configuración"; `application.yml:40-58,84-87` sin procedencia; `CU43:196`, `CU52:221` `plusDays(30)`; `CU54RegistrarRiesgoOperativo.java:200` `DIAS_POR_OMISION=30`. Corrección: Línea errónea: `CU45AtenderRequerimiento.java:430` no existe (196 líneas) → el literal `plusDays(15)` está en `:165`. Nota: `reclamos.dias-habiles-de-respuesta` sí cita fuente (ASFI Libro 4), no está en la lista y está bien excluido.

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA CON CORRECCIÓN**. Evidencia: Sin llamador en main: `vencidosSinEnviar` (`ReporteRegulatorioRepositorio.java:155`), `vencidosSinResponder` (Requerimiento y Reclamo), `abiertasSinAnalista` (`MonitoreoLftRepositorio.java:188`), `conPruebaVencida` (Continuidad), `vencerYPedirLimitacion` (solo `CU06Test`). `trabajos/` solo `ControlDeIncidentesVencidos` y `ControlDePlanesVencidos`. Corrección: Línea errónea: CU06 citar `184-200`, no 403-421.

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA CON CORRECCIÓN**. Evidencia: `C/web/ReclamosController.java:66-72,93-98`, `UifController.java:73-80`, `CumplimientoController.java:116-147`: solo `@Permiso` + `Traza.marcarCasoDeUso`. `grep registro_acceso_datos C/` → **0** coincidencias. Corrección: Evidencia imprecisa: el "comentario" está en `CU45AtenderRequerimiento.java:116-118` y no nombra la tabla; dice que la escribe auditoría a partir del evento `requerimiento_recibido`, evento que nadie consume (ver 123). Agregar esa cadena rota como evidencia.

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
- Refutación (fase 7, verificador independiente B): **CONFIRMA**. Evidencia: `grep -rl` de "cuadre: la suma de debitos…" → 18 archivos; de "la misma clave de idempotencia" → 18. `CU44Test.java:129-150` no usa clave; `:184-200` "cuadre" cuenta alertas. `CU06Test.java:89-111` solo cuenta `evento_dominio`; `CU43Test.java:135-142` "control diario" ejercita `generar`; `CU41Test.java:103-118` el test pasa acumulado 0; `CU02Test.java:89-100` solo prueba la función de base; `BaseDeCumplimiento.java:131-135` conecta como dueño.

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

#### Tanda 6 — backoffice (cumplimiento, accesos, retiros) y web (lote 5)

Rutas sin prefijo: relativas a `apps/backoffice/src/app/rutas/`. Peldaño: DISCOVERED (yarn no corrió). El hallazgo del lote sobre los tipos escritos a mano de la aprobación de retiro se fusionó en IDCU-DOC-006.

### IDCU-DATOS-144 — Ficha de caso LFT enrutada con datos de ejemplo, catálogo de causales inventado y "debido proceso" con notificación al titular
- Clase / Severidad / Confianza: DATOS / S2 / confirmado
- Ubicaciones: `cumplimiento/casos/pantalla-de-caso.ts:37,79-91,93,125-129`; `cumplimiento/dominio/cu44-caso.ts:10-15,17-33,35-46`; `cumplimiento/cumplimiento.routes.ts:64-68`; `cumplimiento/textos.ts:163`
- Evidencia: `this.caso() ?? { id: 'demo', causal: null, ...}`; `CATALOGO_DE_CAUSALES ... 'PATRON_FRACCIONAMIENTO' ...`; etapa `'Notificación' ... 'Probada con el acuse del canal'`, `'Descargo'`, `'Apelación'`
- Cómo se comprobó: lectura; `casos/:casoId` carga `PantallaDeCaso` (routes:64-68); el contrato no tiene GET de caso, así que `caso` siempre es `undefined` y se usa el objeto `demo`. El catálogo de causales no existe en `cumplimiento.yaml` (EntradaDecisionCasoLft solo tiene decision/conclusion/reporteSospechosoId). La conclusión enviada al servidor se pre-rellena con `Causal: <texto inventado>` (pantalla-de-caso.ts:126-129).
- Nota del orquestador: re-verificado `cumplimiento/casos/pantalla-de-caso.ts:79-91` (caso por omisión `id: 'demo'`) y `cumplimiento/cumplimiento.routes.ts:64-68` (ruta `casos/:casoId` sin `canMatch`, mientras la bandeja tiene `canMatch: [guardiaDe('casos')]`).
- Impacto: pantalla real del build final que muestra una escalera de etapas ficticia; en un caso LFT/ROS la "notificación", "descargo" y "apelación" al titular contradicen el deber de reserva que la misma sección declara (`textos-lft.ts:111,148`, `decidir-caso.ts:25-26`). Causales sin procedencia (regla 97.4.1/00.2.1) terminan dentro de la conclusión registrada.
- Causa de fondo: se mezcló el patrón `debido-proceso` (sanciones) con el caso LFT, y se dejó un tipo de dominio "listo para conectarse" a un contrato que ya cambió.
- Solución de fondo: eliminar `CasoDeCumplimiento`/`CATALOGO_DE_CAUSALES`/`etapasDelCaso` o reemplazarlos por un `GET /cumplimiento/casos/{id}` real (sin persona) en el contrato; la ficha sin contrato debe ser `ap-indisponible` como alertas/actas; si las causales son necesarias, catálogo codificado en el servidor con procedencia.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: `decidirCasoLft`
- Regla: 97.4.1, 00.2.1, 95 (datos simulados), aml deber de reserva
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-SEG-145 — Narrativa del caso LFT persistida en IndexedDB del navegador, por caso y no por usuario, sin purga al salir, en ruta sin guardia
- Clase / Severidad / Confianza: SEG / S2 / probable
- Ubicaciones: `cumplimiento/casos/pantalla-de-caso.ts:101-120,136-138`; `cumplimiento/cumplimiento.routes.ts:64-68`; (soporte fuera de lote) `nucleo/borrador.ts:3-16,28-36`
- Evidencia: `` `cumplimiento:caso:${this.casoId()}:narrativa` ``; `servicioBorrador.guardar(...)` en cada tecla; `grep ServicioBorrador` no encuentra purga en cierre de sesión
- Cómo se comprobó: lectura + grep de usos de `ServicioBorrador` (solo pantalla-de-caso y pantalla-de-reclamo); la ruta `casos/:casoId` no tiene `canMatch`.
- Nota del orquestador: re-verificado `cumplimiento/casos/pantalla-de-caso.ts:101-120` (`servicioBorrador.guardar(this.claveBorrador(), valor)`) y la ruta sin guardia (`cumplimiento.routes.ts:64-68`). La guardia de ruta no es seguridad (95.6.1), pero el borrador local sí es un dato que queda en el equipo.
- Impacto: información sujeta a reserva UIF (narrativa de sospecha) queda en claro en el perfil del navegador; otro operador del mismo equipo que abra `/cumplimiento/casos/<id>` (sin `CUMPLIMIENTO_CASOS`) ve el borrador recuperado. IndexedDB es tan legible por script/extensión como localStorage (el comentario de `borrador.ts` afirma lo contrario).
- Causa de fondo: borrador genérico del shell reutilizado para datos reservados sin clave por sesión ni cifrado.
- Solución de fondo: no persistir narrativas LFT en el cliente (o en memoria de sesión), o clave por usuario + purga en logout + guardia de ruta; si se exige borrador, guardarlo en el servidor con control de acceso.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna remota
- Regla: 90.2.1, 95.6.2/95.6.4
- Esfuerzo estimado: chico
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DINERO-146 — Aprobación de retiro "a ciegas" por enlace profundo: se autoriza sin ver monto, destino ni titular
- Clase / Severidad / Confianza: DINERO / S2 / confirmado
- Ubicaciones: `operacion/retiros/pantalla-de-aprobacion-de-retiro.ts:39-47,118-119`; `operacion/retiros/dominio/cu11-aprobacion-de-retiro.ts:10-13`; `nucleo-financiero.yaml:462-466`
- Evidencia: `resumen = (this.router.currentNavigation()?.extras.state?.['retiro'] ?? null)`; `@if (!resumen) { <p class="aviso">{{ t.sinListado }}</p> }` y el formulario sigue habilitado
- Cómo se comprobó: lectura; el contrato dice que destino y nombre "se ven en la ficha de la orden" pero no existe operación de consulta de orden. Al recargar o abrir por enlace, `resumen` es null y el botón "Registrar mi decisión" queda operativo.
- Nota del orquestador: re-verificado `operacion/retiros/pantalla-de-aprobacion-de-retiro.ts:39-47`: sin `resumen` se muestra el aviso `t.sinListado` y el formulario de decisión sigue disponible.
- Impacto: la segunda firma (control antifraude ADR-049) se puede dar sin ver importe ni cuenta destino ni ventana de enfriamiento; la doble aprobación pierde su valor de control.
- Causa de fondo: el contrato no publica `GET /billetera/retiros/{id}`; la UI depende del estado de navegación.
- Solución de fondo: agregar consulta de la orden (importe, moneda, destino enmascarado, enfriamiento, MFA del solicitante) y bloquear la decisión hasta tenerla; o retirar la doble aprobación si R-NUEVA-1 la elimina (A-1).
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: `aprobarRetiro`, `listarRetirosEnRevision`
- Regla: 91.3.3, 90.5
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DEFECTO-147 — "Aprobar y enviar" un reporte UIF identifica el reporte por un código tecleado, no por la fila elegida; y arrastra datos del reporte anterior
- Clase / Severidad / Confianza: DEFECTO / S2 / confirmado
- Ubicaciones: `cumplimiento/uif/aprobar-y-enviar.ts:27,47-51,56-62,68-75`; `cumplimiento/dominio/cu43-reportes.ts:89-98`; `cumplimiento.yaml:1512-1518,1660-1670`
- Evidencia: `catalogoCodigo: this.catalogoCodigo()` (tecleado); la fila trae `catalogoReporteId` (uuid), no el código; el `effect` dice "lo cargado para un período no se arrastra a otro" pero solo limpia `errores`/`errorGeneral`
- Cómo se comprobó: lectura de componente y esquemas `ReporteRegulatorioEnLista` / `EntradaEnvioReporte`.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: el aprobador hace clic en la fila del reporte A y puede aprobar/registrar el envío del reporte B del mismo período (otro catálogo) si escribe otro código; canal, constancia, "aceptado por el organismo" y reintentos de un reporte previo quedan precargados al abrir otro.
- Causa de fondo: contrato identifica por (periodo, catalogoCodigo) en vez de `reporteId`; el componente no resetea su estado.
- Solución de fondo: cambiar el contrato a `POST /uif/reportes/{reporteId}/envios` o incluir `catalogoCodigo` en la fila y no editable; resetear todos los signals al cambiar `reporte()`.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: `aprobarYEnviarReporte`
- Regla: 95.3.3 (inverso: datos de otro registro), 96.4
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DATOS-148 — La ficha KYC descarga la cola completa (todos los estados y personas) para mostrar un expediente; la cola no pagina
- Clase / Severidad / Confianza: DATOS / S2 / confirmado
- Ubicaciones: `cumplimiento/dominio/cu02-expedientes.ts:21-32,34-44`; `cumplimiento/verificaciones/ficha-de-expediente.ts:74`; `identidad.yaml:271-291`
- Evidencia: `httpResource<ExpedienteEnRevision[]>(() => (verificacionId() ? \`${gateway}/identidad/verificaciones\` : undefined))` y `.find(...)`
- Cómo se comprobó: lectura; `listarVerificaciones` devuelve array sin `limite` ni cursor.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: cada apertura de ficha trae nombre, teléfono, correo y estado de TODAS las verificaciones históricas (incluidas aprobadas/rechazadas) al navegador; crece sin límite (96.6.1) y viola respuesta mínima (90.1.8). Además "Por decidir" también pide la colección completa.
- Causa de fondo: contrato sin `GET /identidad/verificaciones/{id}` ni paginación.
- Solución de fondo: agregar consulta por id y cursor/limite al listado; filtro multi-estado en servidor.
- Relación con el flujo nuevo: APROB (pantalla de aprobación KYC)
- Conexiones y módulos afectados: `listarVerificaciones`
- Regla: 96.6.1, 90.1.8, 96.5.3
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-HUECO-149 — Alta de usuario interno sin forma de entregar la contraseña provisional ni de enrolar el segundo factor
- Clase / Severidad / Confianza: HUECO / S2 / confirmado
- Ubicaciones: `sistemas/usuarios-internos/modal-alta-usuario-interno.ts:32`; `servicios/identidad/.../infraestructura/UsuariosInternosRepositorio.java:181-183`; `servicios/identidad/.../aplicacion/CU04Autenticar.java:141-149`; `identidad.yaml:830-990` (sin endpoint de cambio de contraseña ni de enrolamiento TOTP)
- Evidencia: UI: "Se le asigna una contraseña provisional que tiene que cambiar en su primer ingreso."; backend: `String provisional = generarContrasenaProvisional(); String hash = hasheador.hashear(...)` y la variable no se devuelve ni se envía; operador sin factor → `AP-CU04-06 FACTOR_NO_ENROLADO`
- Cómo se comprobó: lectura del backend y grep de `enrol|totp|cambiar-contrasena` en backoffice (sin pantallas) y en `identidad.yaml`.
- Nota del orquestador: re-verificado `servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/UsuariosInternosRepositorio.java:181-183`: la contraseña provisional se genera y se hashea; `grep -n provisional` no encuentra otro uso (no se devuelve ni se envía). Relacionado con IDCU-HUECO-077 (sin enrolamiento MFA).
- Impacto: un operador nuevo nunca puede ingresar; la pantalla afirma algo que no ocurre. Empuja a soluciones manuales en base (prohibidas 97.1.5).
- Causa de fondo: CU-08 implementado a medias (alta sin canal de entrega ni primer ingreso).
- Solución de fondo: invitación con token de un solo uso (hash con pepper) para fijar contraseña + enrolar TOTP; endpoint y pantalla de primer ingreso.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `crearUsuarioInterno`, `autenticar`
- Regla: 90.2.9, 00.6.2
- Esfuerzo estimado: grande
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-DINERO-150 — Importes sin moneda en contratos consumidos por el lote
- Clase / Severidad / Confianza: DINERO / S2 / confirmado
- Ubicaciones: `cumplimiento.yaml:1525` (`montoTotal` string sin moneda) → `cumplimiento/uif/bandeja-de-reportes.ts:29,40,65`; `organizador.yaml:697` (`limiteDeMonto`) → `cumplimiento/organizadores/pantalla-de-habilitacion.ts:75`; `cumplimiento.yaml:1720` (`perdidaCuantificada`) → `cu56-continuidad.ts:23-24`
- Evidencia: `montoTotal: { type: string, pattern: '^-?\d+\.\d{2}$' }`; `{ nombre: this.t.limiteDeMonto, valor: h.limiteDeMonto }`
- Cómo se comprobó: contraste UI ↔ OpenAPI. La bandeja UIF lo declara ("moneda sin especificar"); la habilitación del organizador muestra el número pelado.
- Impacto: límites y totales de dinero sin moneda (91.1.2); el límite de monto del organizador se lee como si fuera en una moneda implícita.
- Causa de fondo: tablas/contratos sin `moneda CHAR(3)`.
- Solución de fondo: agregar moneda al esquema y al contrato; mostrar con `ap-monto`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `listarReportesRegulatorios`, `consultarHabilitacion`, `registrarPruebaDeContinuidad`
- Regla: 91.1.2
- Esfuerzo estimado: medio
- Estándares y normas: ver evidencia y ubicaciones.
- Refutación (fase 7, verificador independiente B): **CONFIRMA CON CORRECCIÓN**. Evidencia: `cumplimiento.yaml:1525` `montoTotal` string sin moneda; `:1720` `perdidaCuantificada` sin moneda; `organizador.yaml:697` `limiteDeMonto` sin moneda; `bandeja-de-reportes.ts:29,40,65` y `pantalla-de-habilitacion.ts:75` lo pintan sin moneda. Corrección: `cu56-continuidad.ts:23-24` **no consume** `perdidaCuantificada`: lo excluye a propósito y lo registra como ambigüedad. Ese consumidor no aplica; el defecto queda en el contrato. Matiz: `bandeja-de-reportes.ts:40` ya rotula la columna exportada como "sin moneda" (regla 91.1.2). S2→S3 si el único impacto es de presentación.

### IDCU-SEG-151 — Revisión de accesos exportable sin permiso de exportación (lista de quién no tiene segundo factor)
- Clase / Severidad / Confianza: SEG / S3 / confirmado
- Ubicaciones: `sistemas/accesos/pantalla-accesos.ts:27`; `packages/ui/src/listado/listado.ts:50,98`; también `sistemas/roles-internos/pantalla-roles-internos.ts:34`, `sistemas/permisos-internos/pantalla-permisos-internos.ts:35`
- Evidencia: `<ap-listado ...>` sin `[exportable]`; `readonly exportable = input(true)`
- Cómo se comprobó: lectura; usuarios internos sí lo condiciona (`pantalla-usuarios-internos.ts:45`).
- Impacto: cualquiera que entre a Sistemas descarga persona×rol×"Segundo factor: No" — un mapa de cuentas débiles con privilegios.
- Solución de fondo: `[exportable]="exportador.puedeExportar('accesos')"` y default `false` en el kit.
- Relación con el flujo nuevo: ninguna · Conexiones: ninguna · Regla: 90.2.1 · Esfuerzo: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-FRAGIL-152 — Fotos de cédula: suscripciones sin cancelar crean `blob:` después de soltar/destruir
- Clase / Severidad / Confianza: FRAGIL / S3 / confirmado (por lectura)
- Ubicaciones: `cumplimiento/verificaciones/tira-de-fotos.ts:88-99,105-125`
- Evidencia: `this.pedirContenido(...).subscribe({ next: (bytes) => this.url.update(... URL.createObjectURL(bytes) ...) })` sin `takeUntilDestroyed`
- Cómo se comprobó: lectura: `soltar()` revoca lo que hay en ese momento; una respuesta que llega luego de cerrar la tira o de salir de la ficha crea un objeto URL nuevo que nadie revoca.
- Impacto: exactamente la fuga que el comentario dice evitar ("cada apertura deja una cédula viva en la memoria"); además abrir/cerrar rápido dispara lecturas duplicadas auditadas.
- Solución de fondo: `takeUntilDestroyed` + cancelar en `soltar()`; descartar respuestas si `abierto()` es falso.
- Relación: APROB (pantalla KYC) · Regla: 90.2 · Esfuerzo: chico
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-UX-A11Y-153 — Decisión KYC sin confirmación, motivo sin tope del contrato y error del servidor no asociado al campo
- Clase / Severidad / Confianza: UX-A11Y / S3 / confirmado
- Ubicaciones: `cumplimiento/verificaciones/decision-de-expediente.ts:32,37-54`; `vista-decision-del-expediente.ts:68-82`; `identidad.yaml:1344`
- Evidencia: `<ap-campo [etiqueta]="t.motivo" ... [(valor)]="motivo" />` sin máximo (contrato `maxLength: 160`); `(pulsado)="decidir.emit(...)"` directo
- Impacto: un clic aprueba o rechaza de forma irreversible (AP-CU01-08); un motivo >160 vuelve como error general (`ap-error-de-envio`), no en el campo (95.3.2).
- Solución de fondo: diálogo de confirmación con consecuencia; `maximo=160`; mapear 422 al campo.
- Relación: APROB · Regla: 95.3.1/95.3.2 · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-SEG-154 — La bandeja KYC muestra teléfono y correo de cada persona en la fila
- Clase / Severidad / Confianza: PII / S3 / confirmado
- Ubicaciones: `cumplimiento/verificaciones/pantalla-de-expedientes.ts:70-74,139`; `identidad.yaml:1306-1312`
- Evidencia: `<span class="tenue">{{ e.telefono }} · {{ correo(e) }}</span>`
- Impacto: para decidir en la ficha no hace falta el contacto en la lista (respuesta mínima 90.1.8/96.5.3); expone PII en pantalla compartida y en capturas.
- Solución de fondo: enmascarar en la lista; contacto solo en la ficha.
- Relación: APROB · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-DEFECTO-155 — "Responder requerimiento" conserva el enlace escrito para otro oficio
- Clase / Severidad / Confianza: DEFECTO / S3 / confirmado
- Ubicaciones: `cumplimiento/uif/responder-requerimiento.ts:36-58`
- Evidencia: `respuestaUrl` solo se limpia tras éxito; al cancelar y abrir otro oficio el valor anterior sigue.
- Impacto: respuesta de un oficio adjuntada a otro (dato de autoridad mal registrado, no editable luego).
- Solución de fondo: resetear al cambiar `requerimiento()` y al cancelar.
- Relación: APROB · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-HUECO-156 — Abrir caso: "marcá alertas de un mismo titular" pero la fila no trae titular; el analista se pega como UUID
- Clase / Severidad / Confianza: HUECO / S3 / confirmado
- Ubicaciones: `cumplimiento/alertas/bandeja-de-alertas.ts:61`; `textos-lft.ts:100,112-113`; `alertas/abrir-caso.ts:29`; `cumplimiento.yaml:1393-1406`
- Evidencia: `elegirAlertas: 'Marcá las alertas de un mismo titular...'`; `AlertaEnBandeja` no tiene titular; `analistaId` campo de texto UUID
- Impacto: el operador agrupa a ciegas y se entera por AP-CU44-08; pegar UUIDs a mano es frágil y no muestra quién es el analista.
- Solución de fondo: identificador opaco de titular (seudónimo) en la fila o agrupación asistida por servidor; selector de analistas con `CUMPLIMIENTO_ALERTAS`.
- Relación: APROB · Esfuerzo: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-REG-157 — Asignación de roles privilegiados sin doble control
- Clase / Severidad / Confianza: REG / S3 / sospecha
- Ubicaciones: `sistemas/usuarios-internos/modal-asignar-roles.ts:73-88`; `UsuariosInternosRepositorio.java:249`
- Evidencia: solo se bloquea la auto-ampliación (`"Nadie se amplia sus propios permisos."`); un único administrador otorga `OFICIAL_CUMPLIMIENTO` o `RETIRO_APROBAR` a otra persona.
- Impacto: la segregación R-SEG-04 de retiros y casos se puede sortear con dos cuentas coludidas y un único administrador.
- Solución de fondo: maker-checker para roles marcados `CRITICO`/`requiereMfa`.
- Relación: APROB (decisión A-1 condiciona) · Esfuerzo: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-CONTRATO-158 — Mutaciones de usuarios internos sin Idempotency-Key; listado sin paginación; 403 mal tipado
- Clase / Severidad / Confianza: CONTRATO / S3 / confirmado
- Ubicaciones: `sistemas/dominio/usuarios-internos.ts:62-88`; `identidad.yaml:858,891-956`; `sistemas/usuarios-internos/pantalla-usuarios-internos.ts:116-118`
- Evidencia: `http.patch<...>(\`${gateway}/usuarios-internos/${id}\`, entrada)` sin `context`; `'403': { $ref: '#/components/responses/ReglaDeNegocio' }`
- Impacto: reintentos duplican eventos de auditoría (suspender/desbloquear); colección completa sin límite (96.6.1).
- Solución de fondo: Idempotency-Key en contrato y cliente; cursor/limite.
- Relación: ninguna · Regla: 96.3.3, 96.6.1 · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-APROB-159 — Habilitar organizador: botón para cualquiera, ruta sin guardia, sin bloqueo de doble envío, contrato sin idempotencia
- Clase / Severidad / Confianza: APROB / S3 / confirmado
- Ubicaciones: `cumplimiento/organizadores/pantalla-de-habilitacion.ts:43-45,80-95`; `cumplimiento/cumplimiento.routes.ts:117-121`; `cu90-habilitacion.ts:33-39`; `organizador.yaml:313-326`
- Evidencia: `habilitar()` no verifica `habilitando()` antes de enviar; el contrato no declara `Idempotency-Key` aunque el cliente la manda
- Impacto: punto de aprobación humana del circuito (organizador) con UX débil; el servidor sí exige `ADMIN_PLATAFORMA` (`OrganizadoresController.java:128`).
- Solución: guardia de ruta/botón por permiso, `if (habilitando()) return`, Idempotency-Key en contrato. Decidir A-1.
- Relación: APROB · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-FRAGIL-160 — Rutas de detalle sin `canMatch` mientras las bandejas sí lo tienen
- Clase / Severidad / Confianza: FRAGIL / S3 / confirmado
- Ubicaciones: `cumplimiento/cumplimiento.routes.ts:51-56,64-68,95-99,112-121`
- Evidencia: `{ path: 'casos/:casoId', loadComponent: ... }` sin guardia
- Impacto: no es seguridad (95.6.1, el servidor decide), pero `casos/:casoId` monta datos locales reservados (ver hallazgo de IndexedDB) y `organizadores/.../habilitacion` ofrece el POST a cualquiera.
- Solución: `canMatch: [guardiaDe('casos')]`, etc. · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-CLEAN-161 — Código muerto (componentes y funciones sin uso) cubierto por tests que inflan la cobertura
- Clase / Severidad / Confianza: CLEAN / S4 / confirmado (grep de usos)
- Ubicaciones: `alertas/pantalla-de-alertas.ts` (todo), `actas/pantalla-de-acta.ts` (todo; `cerrar()` solo pone `this.cerrada = true`, 67-70), `reclamos/pantalla-de-reclamo.ts` (todo; `enviar()` solo borra el borrador, 56-58), `verificaciones/vencimiento-del-documento.ts`, `compartido/navegacion.ts:19-42` (componente), `dominio/cu02-expedientes.ts:96,99-111,127-132` (`colaVacia`, `coincideConBusqueda`, `crearPedirFoto`), `dominio/cu97-alertas.ts:29-32`, `textos.ts:124-125`
- Impacto: botones que "no hacen nada" listos para volver a enrutarse; tests (`pantalla-de-acta.spec.ts`, `pantalla-de-reclamo.spec.ts`, `pantalla-de-alertas.spec.ts`) verifican componentes que el producto no usa (PRUEBAS).
- Solución: borrar o mover a un paquete de prototipos excluido del build. · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-DOC-162 — Comentarios que contradicen el contrato actual
- Clase / Severidad / Confianza: DOC / S4 / confirmado
- Ubicaciones: `dominio/cu44-caso.ts:10-15` ("no declara una ruta HTTP para el caso" — existe `/cumplimiento/casos`, `cumplimiento.yaml:618-715`); `dominio/cu52-reclamo.ts:9-12` (existe `/reclamos`, yaml:468-569); `dominio/cu94-acta.ts:9-11` (existe la lista, yaml:897); `cumplimiento.routes.ts:18-25` (párrafo partido); `dominio/cu90-habilitacion.ts:9-12`; `retiros/dominio/cu11-aprobacion-de-retiro.ts:10-13`; `nucleo/borrador.ts:7-9` (IndexedDB "no lo lee cualquier extensión")
- Solución: actualizar. · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-CONTRATO-163 — Mensajes para códigos que el contrato no declara o con otra semántica
- Clase / Severidad / Confianza: CONTRATO / S4 / probable
- Ubicaciones: `compartido/errores-de-cumplimiento.ts:15,19,20`; `cumplimiento.yaml:708-709,775-776,948-950`
- Evidencia: `'AP-CU43-04'`, `'AP-CU44-04'` no figuran en las respuestas; contrato `AP-CU44-03 REPORTE_SIN_NARRATIVA` vs texto UI "el ROS tiene que estar radicado"
- Solución: generar el catálogo de errores desde el contrato; un solo CATALOGO en `nucleo/errores.ts`. · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-DUP-INT-164 — Diccionarios de estados/enums copiados a mano en varios lugares
- Clase / Severidad / Confianza: DUP-INT / S4 / confirmado
- Ubicaciones: `textos-uif.ts:16-26,65-66` vs enums generados usados en `cu43-reportes.ts:52-53` y `cu45-requerimientos.ts:44`; `textos-lft.ts:105-106,131-138`; `dominio/cu02-presentacion.ts:13-30` vs `textos.ts:124-125`; `sistemas/dominio/accesos.ts:10-19` vs `pantalla-usuarios-internos.ts:24`; `errores-de-cumplimiento.ts` vs `nucleo/errores.ts`
- Impacto: un valor nuevo del contrato aparece en el filtro sólo si alguien edita el texto.
- Solución: derivar opciones de los enums generados y mapear solo etiquetas. · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-UX-A11Y-165 — Menú «Más» de usuarios internos hecho a mano
- Clase / Severidad / Confianza: UX-A11Y / S3 / probable
- Ubicaciones: `sistemas/usuarios-internos/pantalla-usuarios-internos.ts:60-70,84,165-183`
- Evidencia: `<details class="mas">` + `position: fixed` calculado con `window.innerHeight`; cierre por `document:click`/`window:scroll`; `separacion = 4` (px literal)
- Impacto: sin `role="menu"`/flechas, el foco no vuelve al `summary` tras acción; cierra al hacer scroll dentro de la tabla. Duplica un overlay que el kit debería proveer (95.1.1).
- Solución: componente de menú del kit (CDK overlay) con foco gestionado. · Esfuerzo: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-UX-A11Y-166 — Ficha de usuario interno muestra códigos y fechas crudos; id por snapshot
- Clase / Severidad / Confianza: UX-A11Y / S4 / confirmado
- Ubicaciones: `sistemas/usuarios-internos/pantalla-usuario-interno-detalle.ts:39,46,85,103-105`
- Evidencia: `{{ u.estado }}` (ACTIVO), `hasta {{ u.bloqueadoHasta }}` (ISO), `valor: u.debeCambiarContrasena` (boolean), `this.route.snapshot.paramMap.get('id')`
- Solución: formatear como en el listado; `input()` con binding de ruta. · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-UX-A11Y-167 — Literales de tipografía, tiempos y medidas en lugar de tokens
- Clase / Severidad / Confianza: UX-A11Y / S4 / confirmado
- Ubicaciones: `verificaciones/tira-de-fotos.ts:61,65,66`; `verificaciones/decision-de-expediente.ts:59`; `verificaciones/vencimiento-del-documento.ts:28`; `casos/formulario-de-caso.ts:45-46`; `actas/pantalla-de-acta.ts:43`; `retiros/pantalla-de-aprobacion-de-retiro.ts:103,106`; `ingreso/pantalla-de-ingreso.ts:100,113`; `ingreso/panel-de-marca.ts:69,75,78`; `alertas/pantalla-de-alertas.ts:34`
- Evidencia: `font-size: .875rem`, `font-size: 2.1rem`, `animation: aparecer .45s cubic-bezier(...)`
- Regla: 95.1.5 · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-CLEAN-168 — `$any` y `as` en el borde del contrato
- Clase / Severidad / Confianza: CLEAN / S4 / confirmado
- Ubicaciones: `compartido/campos.ts:29`; `casos/decidir-caso.ts:36`; `casos/formulario-de-caso.ts:33`; `casos/pantalla-de-caso.ts:125` (`({ casoId, codigo: casoId }) as CasoEnBandeja`); `dominio/cu43-reportes.ts:44`; `dominio/cu45-requerimientos.ts:35`; `dominio/cu56-continuidad.ts:36` (`r.body as ...`); `dominio/paginas.ts:51`; `retiros/pantalla-de-aprobacion-de-retiro.ts:119,153`
- Impacto: un 201 sin cuerpo o un cuerpo distinto pasa como tipo correcto. · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-SEG-169 — Parámetros de ruta y query sin codificar; enlaces sin validar esquema
- Clase / Severidad / Confianza: SEG / S4 / confirmado
- Ubicaciones: `dominio/cu02-expedientes.ts:30,131,149,163`; `dominio/cu90-habilitacion.ts:16,38`; `sistemas/dominio/usuarios-internos.ts:38,68,77,87`; `dominio/cu45-requerimientos.ts:66,85-86`; `dominio/cu56-continuidad.ts:66`
- Evidencia: `` `${gateway}/identidad/verificaciones?estado=${e}` ``; `documentoUrl` solo valida longitud (acepta `javascript:`)
- Impacto: bajo (ids son UUID del servidor), pero los enlaces guardados se renderizan en otras pantallas. · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-ACOPLE-170 — Importaciones cruzadas entre dominios del backoffice
- Clase / Severidad / Confianza: ACOPLE / S4 / confirmado
- Ubicaciones: `verificaciones/pantalla-de-expedientes.ts:16` (`../../sistemas/compartido/carga`); `uif/bandeja-de-reportes.ts:20` (`../../operacion/comun/importe-sin-moneda`)
- Solución: mover a `nucleo/` o al kit. · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-SEG-171 — Exportación con un único permiso para cuatro colas distintas
- Clase / Severidad / Confianza: SEG / S4 / confirmado
- Ubicaciones: `colas/cola-de-lectura.ts:42` (`recursoDeExportacion="cumplimiento-colas"`)
- Impacto: quien puede exportar incidentes puede exportar eventos con pérdidas y continuidad. · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-OBSERV-172 — Huella de dispositivo aleatoria por pestaña en el ingreso
- Clase / Severidad / Confianza: OBSERV / S4 / confirmado
- Ubicaciones: `ingreso/dominio/cu04-autenticar.ts:26-37`; `ingreso/pantalla-de-ingreso.ts:121`
- Impacto: cada ingreso es "dispositivo nuevo" para `dispositivoPorHuella` (`CU04Autenticar.java:128`): la señal de dispositivo confiable/nuevo queda inútil para operadores y la tabla crece por pestaña.
- Solución: decisión explícita (ADR) o huella estable no identificante. · Esfuerzo: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-UX-A11Y-173 — Ingreso: `AP-CU04-06` (sin factor enrolado) se muestra como texto del servidor, sin camino
- Clase / Severidad / Confianza: UX-A11Y / S4 / confirmado
- Ubicaciones: `ingreso/pantalla-de-ingreso.ts:163-172`
- Impacto: ligado al HUECO de alta: el operador recibe "Tenés que enrolar tu segundo factor" sin dónde hacerlo. · Esfuerzo: chico (tras el hallazgo de alta)
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Esfuerzo estimado: ver evidencia y ubicaciones.

### IDCU-FLUJO-FALTANTE-174 — Web: el secreto de invitación se retira de la URL y se guarda en memoria, pero nunca se usa ni se entrega a la app
- Clase / Severidad / Confianza: FLUJO-FALTANTE / S2 / probable
- Ubicaciones: `apps/web/src/app/paginas/invitacion/token-de-invitacion.ts:44-60`; `invitacion.ts:101-109`; `apps/web/src/app/layout/enlace-a-la-app.ts:27-32`
- Evidencia: `grep SecretoDeInvitacionEnMemoria|.tomar()` → solo declaración e inyección; el botón lleva a la URL de tienda (`meta` http/https) sin token
- Impacto: quien abre el enlace en el navegador pierde el token (ya no está en la barra) y la app no lo recibe; queda sin forma de canjear salvo pedir otro enlace.
- Causa de fondo: la web no tiene sesión de participante y no se diseñó el traspaso (deep link con token).
- Solución de fondo: deep link/universal link `aportaya://invitacion#t=...` construido desde memoria, o no retirar el fragmento hasta el traspaso.
- Relación con el flujo nuevo: FLUJO-QR indirecto (alta de integrantes) / APROB (texto de admisión humana)
- Esfuerzo estimado: medio
- Cómo se comprobó: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.
- Refutación (fase 7, verificador independiente B): **CONFIRMA**. Evidencia: `apps/web/src/app/paginas/invitacion/token-de-invitacion.ts:44-60` guarda/`tomar()`; `invitacion.ts:101-109` retira de la URL y `guardar()`; `grep "\.tomar()\ Corrección: \.hay()"` en `apps/web/src` sin spec → 0 llamadores; `layout/enlace-a-la-app.ts:27-32` arma el enlace solo desde `<meta>`, sin token.

### IDCU-PRUEBAS-175 — Espera fija y dobles declarados
- Clase / Severidad / Confianza: PRUEBAS / S4 / confirmado
- Ubicaciones: `cumplimiento/casos/pantalla-de-caso.spec.ts:66` (`setTimeout(r, 50)`); todas las specs del lote usan `HttpTestingController` (declarado en `compartido/pruebas.apoyo.ts:14-18`, `retiros/*.spec.ts` "[UI contra HTTP simulado]")
- Impacto: 80.3.4; ningún test del lote prueba integración real con identidad/cumplimiento/nucleo-financiero.
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

### IDCU-RENDIMIENTO-176 — Funciones en plantilla recalculadas por celda
- Clase / Severidad / Confianza: RENDIMIENTO / S4 / confirmado
- Ubicaciones: `verificaciones/pantalla-de-expedientes.ts:87,129` (`new Date()` por fila y ciclo); `colas/cola-de-lectura.ts:43-46,70-78` (celda calculada dos veces); `retiros/bandeja-de-retiros.ts:58`
- Esfuerzo estimado: chico
- Cómo se comprobó: ver evidencia y ubicaciones.
- Impacto: ver evidencia y ubicaciones.
- Causa de fondo: ver evidencia y ubicaciones.
- Solución de fondo: ver evidencia y ubicaciones.
- Relación con el flujo nuevo: ver evidencia y ubicaciones.
- Conexiones y módulos afectados: ver evidencia y ubicaciones.
- Estándares y normas: ver evidencia y ubicaciones.

## 6. Matriz de contratos

Las columnas «pacto», «cliente generado», «modelo del front» y «esquema» se resumen en 6.3, porque para la gran mayoría de filas el valor es el mismo (sin pacto; cliente Dart generado desde el mismo OpenAPI; tabla del propio esquema). Las referencias «F0x» dentro de las tablas son números de trabajo del lote; su ficha está en §5 (tanda 4 para identidad, tanda 5 para cumplimiento).

### 6.1 Operaciones HTTP de `identidad` (39)
Permiso = anotación en el controlador, aplicada por `plataforma/comun-web/.../GuardiaDePermiso.java:52-82` (rol del token == código, o código ∈ `permisos` del token). `@Publico` = sin guardia.

| # | Método y ruta | controlador:línea | permiso exigido | DTO entrada → salida | códigos (contrato) | Idempotencia | Observaciones / divergencias |
|---|---|---|---|---|---|---|---|
| 1 | POST /usuarios/verificaciones/correo | UsuariosController:251 | @Publico | SolicitudVerificacionCorreo → VerificacionCorreoSolicitada | 202, 422 | Key obligatoria, **usada** | Enumera correos (F17) |
| 2 | POST /usuarios/verificaciones/correo/{id}/confirmacion | UsuariosController:262 | @Publico | ConfirmacionVerificacionCorreo → VerificacionCorreoConfirmada | 200, 422 | n/a | Reconsulta ilimitada sobre token CONSUMIDO devuelve borrador con PII (F07) |
| 3 | PUT /usuarios/verificaciones/correo/{id}/borrador | UsuariosController:277 | @Publico | BorradorOnboarding ↔ | 200, 422 | n/a | Sin vencimiento del token para escribir; CI en claro (F08) |
| 4 | POST /usuarios | UsuariosController:236 | @Publico | EntradaRegistro → SalidaRegistro | 202, 422 (+409 descrito) | Key obligatoria, **ignorada** (F21) | Contrato describe "una transacción con billetera" (F34) |
| 5 | POST /sesiones | SesionesController:94 | @Publico | EntradaAutenticacion → SalidaAutenticacion | 200, 422 (+400) | sin Key (declarado) | No verifica estado de cuenta (F01); rate limit en gateway |
| 6 | POST /sesion/refrescar | SesionController:61 | @Publico | EntradaRenovacion → SalidaRenovacion | 200, 401, 422 | n/a | Sin rate limit en gateway; no verifica estado (F01) |
| 7 | DELETE /sesion | SesionController:104 | PARTICIPANTE \| BACKOFFICE | – → 204 | 204, 401 | idempotente | JWT sigue válido 15 min (F24) |
| 8 | POST /sesiones/desafios | SesionesController:129 | PARTICIPANTE | EntradaDesafioDeStepUp → SalidaDesafioDeStepUp | 201, 422 | Key obligatoria, **ignorada** | Estado en memoria (F04). Un operador sin rol PARTICIPANTE no puede |
| 9 | POST /sesiones/desafios/{id}/verificacion | SesionesController:142 | PARTICIPANTE | EntradaVerificacionDeStepUp → SalidaEvidenciaDeStepUp | 200, 422 | un solo uso | `factor.tipo` ignorado |
| 10 | GET /identidad/verificaciones | VerificacionesController:48 | VERIFICACION_RESOLVER | ?estado → [ExpedienteEnRevision] | 200 | n/a | Sin paginación; sin bitácora de lectura (F16, F29) |
| 11 | GET /identidad/verificaciones/{id}/fotos/{cara} | VerificacionesController:61 | VERIFICACION_RESOLVER | → EnlaceDeFoto | 200, 422 | n/a | Contrato promete registro de lectura: no existe (F16) |
| 12 | GET /identidad/verificaciones/{id}/fotos/{cara}/contenido | VerificacionesController:80 | VERIFICACION_RESOLVER | → binario | 200, 422 | n/a | Ídem |
| 13 | POST /identidad/verificaciones/{id}/decision | VerificacionesController:97 | VERIFICACION_RESOLVER | DecisionDeVerificacion → ExpedienteEnRevision | 200, 422 | Key obligatoria, **ignorada** | APROBACIÓN HUMANA; relee toda la cola (F29); sin evento (F11) |
| 14 | POST /usuarios/{id}/documentos | ExpedienteController:89 | @Publico + Token-De-Alta o sesión propia | multipart → ArchivoDelExpediente | 201, 403, 422 | Key obligatoria, **ignorada** | Sobrescribe tras APROBADA (F12) |
| 15 | PUT /usuarios/{id}/documento/fecha-expiracion | ExpedienteController:143 | @Publico (pero exige sesión propia en código) | FechaExpiracionDocumento → 204 | 204, 401, 403, 422 | n/a | Contrato dice bearerAuth; código @Publico (F38) |
| 16 | GET /usuarios/{id}/verificacion | ExpedienteController:125 | @Publico + Token-De-Alta o sesión propia | → EstadoDeVerificacion | 200, 403, 422 | n/a | OK (BOLA controlado) |
| 17 | POST /usuarios/{id}/verificacion | ExpedienteController:157 | PARTICIPANTE + sesión propia | Documento → EstadoDeVerificacion | 202, 401, 403, 422 | sin Key | No emite evento (F11); tras RECHAZADA no hay reintento (F14) |
| 18 | POST /usuarios/{id}/titularidad/verificacion | UsuariosController:216 | DATOS_SENSIBLES_LEER \| SERVICIO_ENTREGAS | EntradaTitularidad → SalidaTitularidad | 200, 422, 429 | n/a | No exige KYC aprobado (F18); tope por réplica |
| 19 | POST /usuarios/tokens/invitacion | UsuariosController:173 | GRUPO_ADMINISTRAR | EntradaTokenDeInvitacion → SalidaTokenDeInvitacion | 201, 422 | Key usada (huella) | Sin propiedad sobre `grupoId` (F19) |
| 20 | POST /usuarios/tokens/invitacion/validar | UsuariosController:196 | PARTICIPANTE | EntradaValidacionInvitacion → SalidaValidacionInvitacion | 200 | n/a | Contexto sistema con usuario de sesión (F27) |
| 21 | POST /usuarios/tokens/invitacion/{id}/consumos | InvitacionesController:29 | PARTICIPANTE | CanjeInvitacion → ConsumoInvitacion | 200, 422 | Key usada (`clave_consumo`) | `ConsumoInvitacion` sin `additionalProperties:false` |
| 22 | POST /usuarios/tokens/invitacion/{id}/revocacion | InvitacionesController:47 | GRUPO_ADMINISTRAR | – → 204 | 204, 422 | idempotente | Propiedad por `emisor_id` OK |
| 23 | POST /usuarios/busquedas/por-telefono | UsuariosController:167 | GRUPO_ADMINISTRAR | EntradaBusquedaPorTelefono → UsuarioEncontrado | 200 | n/a | Sin tope; GRUPO_ADMINISTRAR autoconcedible (F19) |
| 24 | GET /usuarios/por-telefono (deprecado) | UsuariosController:155 | GRUPO_ADMINISTRAR | ?telefono → UsuarioEncontrado | 200 | n/a | Teléfono en URL; grupos ya migró (F36) |
| 25 | POST /usuarios/{id}/roles/organizador | UsuariosController:135 | PARTICIPANTE + id==sesión | EntradaRolOrganizador → RolOrganizadorActivado | 200, 422 | idempotente por contrato | Sin Key; HTTP sin compensación (F33) |
| 26 | GET /usuarios/yo/perfil | ExpedienteController:173 | PARTICIPANTE | → MiPerfil | 200 | n/a | OK |
| 27 | PUT /usuarios/yo/foto-de-perfil | ExpedienteController:198 | PARTICIPANTE | multipart → 204 | 204, 401, 422 | sin Key (declarado) | OK |
| 28 | GET /usuarios/yo/foto-de-perfil | ExpedienteController:213 | PARTICIPANTE | → binario | 200, 401, 404 | n/a | OK |
| 29 | GET /identidad/contactos/grupos/{g}/integrantes/{u} | RutasDeContactoEnGrupo:29 | PARTICIPANTE + grupos confirma | → ContactoEnGrupo | 200, 403 | n/a | Lectura de PII sin bitácora (F16) |
| 30 | GET /usuarios/{id}/kyc | UsuariosController:228 | PARTICIPANTE + (propio o rol BACKOFFICE/ADMIN_PLATAFORMA/sistema) | → NivelKyc | 200, 403 | n/a | Autoriza por string de rol (F06, F39) |
| 31 | GET /usuarios-internos | UsuariosInternosController:35 | USUARIOS_INTERNOS_LEER \| ACCESOS_ADMINISTRAR | ?estado,rol,busqueda → [UsuarioInternoResumen] | 200, 401, 403 | n/a | Sin paginación; N+1 (F29); 403 referencia `ReglaDeNegocio` |
| 32 | POST /usuarios-internos | UsuariosInternosController:44 | USUARIOS_INTERNOS_GESTIONAR \| ACCESOS_ADMINISTRAR | EntradaAltaUsuarioInterno → UsuarioInternoDetalle | 201, 422 | Key obligatoria, **ignorada** | Cualquier `rolCodigo` sin SoD; clave provisional nunca entregada (F02, F15) |
| 33 | GET /usuarios-internos/{id} | UsuariosInternosController:54 | USUARIOS_INTERNOS_LEER \| ACCESOS_ADMINISTRAR | → UsuarioInternoDetalle | 200, 404 | n/a | `actualizadoEn` ficticio (F31) |
| 34 | PATCH /usuarios-internos/{id} | UsuariosInternosController:63 | USUARIOS_INTERNOS_GESTIONAR \| ACCESOS_ADMINISTRAR | EntradaActualizarUsuarioInterno → UsuarioInternoDetalle | 200, 422 | sin Key | Aplica a **cualquier** usuario; ACTIVO salta KYC (F02) |
| 35 | PUT /usuarios-internos/{id}/roles | UsuariosInternosController:72 | ídem | EntradaAsignarRolesUsuarioInterno → UsuarioInternoDetalle | 200, 422 | sin Key | SoD inerte (F05); cualquier rol a cualquier usuario (F02) |
| 36 | POST /usuarios-internos/{id}/desbloquear | UsuariosInternosController:81 | ídem | EntradaDesbloqueoUsuarioInterno → UsuarioInternoDetalle | 200, 422 | sin Key | Sin efecto (F15); motivo no persistido |
| 37 | GET /roles | RolesController:28 | ROLES_INTERNOS_LEER \| ACCESOS_ADMINISTRAR | → [RolCatalogo] | 200 | n/a | N+1 |
| 38 | GET /permisos | PermisosController:28 | PERMISOS_INTERNOS_LEER \| ACCESOS_ADMINISTRAR | → [PermisoCatalogo] | 200 | n/a | OK |
| 39 | GET /.well-known/jwks.json | JwksController:34 | público (permitAll) | → JWKS | 200 | n/a | Fuera del OpenAPI a propósito (declarado) |
| — | CU-08 asignar/revocar, CU-09 cambio de credencial, baja, recuperación, enrolamiento MFA | — | — | — | — | — | **No existen en el contrato ni en controladores** (F14) |

Todas las operaciones del OpenAPI tienen implementación; ninguna implementación (salvo JWKS) está fuera del contrato.


### 6.2 Operaciones HTTP de `cumplimiento` (27)
| método ruta | operationId (yaml:línea) | controlador:línea | permiso código | DTO entrada | errores contrato/código | Observación |
|---|---|---|---|---|---|---|
| POST /cumplimiento/usuarios/{usuarioId}/diligencia | elevarDiligencia (128) | CumplimientoController:173 → AdhesionController:155 | ANALISTA_CUMPLIMIENTO | EntradaDiligencia (`aprobadaPor`, `segundaRevisionPor` del cuerpo, yaml:1220-1234) | AP-CU02-01..04 | Aprobadores no salen de la sesión; Idempotency-Key ignorada |
| POST /cumplimiento/usuarios/{usuarioId}/pep | declararPep (158) | CumplimientoController:166 → AdhesionController:127 | PARTICIPANTE + soloElPropio | EntradaPep | AP-CU03-01/02 | DELETE sin GRANT; RLS de `declaracion_pep` no admite titular |
| GET /cumplimiento/contratos/vigentes | listarContratosVigentes (186) | CumplimientoController:149 | @Publico | — | — | OK |
| POST /cumplimiento/contratos/{contratoId}/aceptaciones | aceptarContrato (207) | CumplimientoController:157 → AdhesionController:103 | PARTICIPANTE + soloElPropio(cuerpo.usuarioId) | EntradaAceptacion | AP-CU05-01..03 | IP = `getRemoteAddr` (gateway); sin idempotencia (duplica aceptación) |
| GET /cumplimiento/riesgos/eventos | listarEventosDeRiesgo (238) | ColasDeCumplimientoWeb:46 | RESPONSABLE_RIESGOS / AUDITORIA_LEER | query | AP-CU54-04 | OK |
| POST /cumplimiento/riesgos/eventos | registrarRiesgoOperativo (268) | CumplimientoController:187 | RESPONSABLE_RIESGOS | EntradaRiesgoOperativo | AP-CU54-01..03 | Reintento choca UNIQUE sin captura |
| GET/POST /cumplimiento/seguridad/incidentes | listarIncidentes (313) / registrarIncidente (343) | ColasDeCumplimientoWeb:54 / CumplimientoController:220 | RESPONSABLE_SEGURIDAD (+AUDITORIA_LEER en GET) | EntradaIncidente | AP-CU55-01..04 | — |
| POST .../incidentes/{id}/reportes | reportarIncidente (389) | CumplimientoController:253 | RESPONSABLE_SEGURIDAD | — | AP-CU55-02 | Repetible: reescribe fecha y re-emite |
| POST .../incidentes/{id}/notificaciones | notificarTitulares (411) | CumplimientoController:261 | RESPONSABLE_SEGURIDAD | — | AP-CU55-03 | Idem |
| GET /reclamos/usuarios/{usuarioId}/efecto | consultarEfectoDeReclamos (436) | ReclamosController:93 | RECLAMO_VER | query producto | 401/403 | Consumidores reales probablemente sin ese permiso |
| GET /reclamos | listarBandejaDeReclamos (469) | ReclamosController:53 | RECLAMO_VER | query | AP-CU52-07 | — |
| GET /reclamos/{reclamoId} | consultarReclamo (513) | ReclamosController:67 | RECLAMO_ATENDER | — | AP-CU52-05 | Lectura de texto personal sin bitácora de lectura |
| POST /reclamos/{reclamoId}/respuesta | responderReclamo (537) | ReclamosController:74 | RECLAMO_ATENDER | EntradaRespuestaReclamo | AP-CU52-03/05/06 | — |
| GET /cumplimiento/alertas | listarBandejaDeAlertas (571) | CumplimientoController:116 | CUMPLIMIENTO_ALERTAS | query | AP-CU44-05 | — |
| GET /cumplimiento/casos | listarBandejaDeCasos (619) | CumplimientoController:135 | CUMPLIMIENTO_CASOS | query | AP-CU44-05 | — |
| POST /cumplimiento/casos | abrirCasoLft (654) | CumplimientoController:77 | CUMPLIMIENTO_CASOS | EntradaCasoLft (`analistaId` del cuerpo, yaml:1344-1351) | AP-CU44-08 | analista arbitrario |
| POST /cumplimiento/casos/{casoId}/decision | decidirCasoLft (683) | CumplimientoController:93 | CUMPLIMIENTO_CASOS | EntradaDecisionCasoLft | AP-CU44-01/02/03/06/07 | mismo permiso que abrir |
| GET/POST /uif/reportes | listarReportes (717) / generarReporte (748) | UifController:65 / 36 | CUMPLIMIENTO_REPORTAR (+AUDITORIA_LEER GET) | EntradaGeneracionReporte (`catalogoCodigo` y `formulario` independientes, yaml:1659) | AP-CU43-01/05 | catálogo y formulario pueden no coincidir |
| GET/POST /uif/requerimientos | listar (780) / registrar (811) | UifController:74 / 51 | OFICIAL_CUMPLIMIENTO (+AUDITORIA_LEER GET) | EntradaRequerimientoDeAutoridad | AP-CU45-01..03/05 | — |
| GET/POST /cumplimiento/continuidad/pruebas | listar (843) / registrar (866) | ColasDeCumplimientoWeb:62 / 38 | RESPONSABLE_RIESGOS | EntradaPruebaDeContinuidad | AP-CU56-01..04 | `ejecutadaPor` del cuerpo (declarado) |
| GET /cumplimiento/comites/actas | listarActasDeComite (898) | ColasDeCumplimientoWeb:70 | OFICIAL_CUMPLIMIENTO / AUDITORIA_LEER | query | AP-CU94-07 | — |
| POST /uif/reportes/envios | aprobarYEnviarReporte (922) | UifController:43 | CUMPLIMIENTO_REPORTAR | EntradaEnvioReporte (`aceptadoPorElOrganismo`, `numeroConstancia` del operador) | AP-CU43-01..04 | "Envío" sin integración |
| POST /uif/requerimientos/{id}/respuesta | responderRequerimiento (955) | UifController:58 | OFICIAL_CUMPLIMIENTO | EntradaRespuestaRequerimiento | AP-CU45-01/04 | — |
| GET /licencia/alcance | verificarAlcance (992) | LicenciaController:38 | PARTICIPANTE | query servicio, usuarioId | AP-CU46-* | GET con escritura; `usuarioId` arbitrario |

Cobertura contrato↔código: las 27 operaciones del yaml tienen implementación y `@Permiso`/`@Publico` (verificado por `SabanaDeSeguridadWeb` heredada en `web/SeguridadWebTest.java:29-30`). El yaml no declara el permiso en un campo máquina-legible (`x-permiso`): solo en `description`. No hay ruta para CU-06, 40, 41, 42, 47, 48, 49, 53, 94 (declarado en el yaml:9-10 y :1020-1023).


Nota del orquestador: `scripts/auditar_backend.py` informa cinco operaciones de cumplimiento «declaradas sin implementar» (`listarEventosDeRiesgo`, `listarIncidentesDeSeguridad`, `listarPruebasDeContinuidad`, `registrarPruebaDeContinuidad`, `listarActasDeComite`; `evidencia/auditar_backend.txt`). Es un **falso positivo** del verificador: están implementadas en la clase abstracta `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/ColasDeCumplimientoWeb.java:28,49,73` (`abstract class ColasDeCumplimientoWeb implements CumplimientoApi`), que hereda el controlador. Se registra en §10.

### 6.3 Capas cruzadas y eventos

| Operación / evento | Pacto (`contratos/pactos/`) | Cliente generado | Modelo app / front | Esquema `sql/` | Discrepancias |
|---|---|---|---|---|---|
| identidad → grupos `GET /grupos/{g}/integrantes/{u}/comparte` | `identidad-grupos-pertenencia.json:14` (consumidor) | n/a | n/a | n/a | sin prueba del lado productor ni casos 4xx/5xx/timeout (98.1.4) — ficha en tanda 3 |
| `POST /usuarios` (alta) | no | `clientes/dart/identidad` (`entrada_registro`) | la app usa Dio crudo con comentario obsoleto (IDCU-CONTRATO-017) | `usuario`, `documento_identidad`, `verificacion_kyc` | `Idempotency-Key` obligatoria e ignorada; perfil transaccional ausente (IDCU-HUECO-009) |
| `Documento.tipo` | — | `[CI, CEX, PASAPORTE]` | coerción silenciosa a `CI` (IDCU-COHERENCIA-018) | catálogo distinto (tanda 3) | tres vocabularios para el mismo concepto |
| `POST /cumplimiento/usuarios/{id}/pep` | no | `clientes/dart/cumplimiento` | **la app nunca lo llama** (IDCU-HUECO-010) | `declaracion_pep`, `beneficiario_final` | DELETE sin privilegio y RLS del titular (tanda 5) |
| `GET /cumplimiento/contratos/vigentes` | no | sí | texto del contrato fijo en la app (IDCU-COHERENCIA-013) | `contrato_adhesion` | versión/hash no comparados |
| `GET /reclamos/usuarios/{id}/efecto` | no | n/a | n/a (consumido por garantía/organizador) | `reclamo_cliente` | permiso exigido que el llamador no tiene (tanda 5) |
| `POST /billetera/retiros/{id}/aprobacion` | no | `clientes/angular/nucleo-financiero` | tipos escritos a mano en el backoffice (IDCU-DOC-006) | `orden_retiro` | el contrato declara 404/409 y el código devuelve 422 (tanda 3) |
| `GET /usuarios/por-telefono` (deprecado) | no | sí | grupos ya migró | — | teléfono en la URL (90.2.2), sigue publicado |
| OpenAPI identidad y cumplimiento | — | — | — | — | sin `components.securitySchemes` aunque identidad referencia `bearerAuth` (tanda 3) |
| Eventos `identidad.*` (10) | no | — | — | outbox | la decisión KYC no publica evento; cumplimiento no consume `usuario_registrado` |
| Eventos `cumplimiento.*` (~30) | no | — | — | outbox | **ninguno con consumidor** (tanda 5); `limites_evaluados` lleva `monto` |
| Evento `nucleo_financiero.retiro_autorizado` | no | — | — | outbox | sin consumidor (IDCU-FLUJO-FALTANTE-036); `retiro_rechazado` con dos formas (tanda 3) |

Operaciones que alguien llama y no existen: progreso de tutoriales `/identidad/tutoriales/progreso` (GET/PUT/DELETE) desde el frontend, sin operación en `identidad.yaml` (declarada «conocida», `evidencia/verificar_rutas_frontend.txt`); cambio de contraseña, baja, recuperación de acceso y enrolamiento MFA: la app ofrece las pantallas y no hay operación (IDCU-HUECO-011, -012, -025). Operaciones que existen y nadie llama: `declararPep` desde la app; CU-40 de cumplimiento (sin ruta, IDCU-REESCRITURA-001).

## 7. Evidencia obligatoria del módulo

| Evidencia (91.6 / 98.8 / 90.6) | Estado | Detalle |
|---|---|---|
| Asientos por operación de dinero | NO APLICA / NO COMPROBADO | identidad y cumplimiento no escriben el mayor (GRANTs `sql/00_base/03_permisos.sql:17-21`, tanda 5). El asiento del pago del retiro no tiene línea de costo (tanda 3): leído, no ejecutado |
| Idempotencia ejercitada | NO COMPROBADO | no se ejecutó nada contra una base; `python3 scripts/idempotencia.py` → exit 0 sin salida (`evidencia/idempotencia.txt`) |
| Cuadre del mayor | NO APLICA | — |
| Caso de reversa | NO COMPROBADO | — |
| Redondeo declarado | NO COMPROBADO | importes de cumplimiento sin moneda (tanda 5) |
| Prueba de contrato | NO COMPROBADO | pacto identidad→grupos sin verificación de productor; Gradle no corrió |
| Prueba de duplicado de mensaje | NO COMPROBADO | ni identidad ni cumplimiento consumen eventos |
| Servicio dependido apagado | NO COMPROBADO | por lectura: identidad→grupos falla cerrado; garantía/organizador→cumplimiento traducen el error a «FRENA» |
| Traza con `correlationId` | NO COMPROBADO | — |
| Matriz de autorización negativa | PASS de existencia, NO COMPROBADO de ejecución | `python3 scripts/verificar_pruebas_web.py` → «controladores: 42 · con matriz propia: 42 · en deuda: 0» (`evidencia/verificar_pruebas_web.txt`); `scripts/auditar_backend.py` → «0/393 clases de prueba corrieron y pasaron» |
| Verificadores estáticos | ver detalle | `verificar_seguridad.py` exit 0; `verificar_boveda.py` exit 0 «Sin divergencias»; `inventario_endpoints.py --check` exit 0 «alineado»; `verificar_rutas_frontend.py` **exit 1** (falla ajena: `GET /grupos/solicitudes-escaladas` ya tiene contrato); `verificar_criterios.py` → «pendientes de implementar (3): CU-05 (identidad), …»; `validar_flujos.py` → AVISO H8-01 retiro sin cablear; `verificar_contratos_limites.py` → 9 campos de cumplimiento y 3 de identidad sin `maxLength`/`maximum`/`maxItems` |

## 8. Cobertura por clase

| Clase | Qué se revisó y con qué método | Hallazgos |
|---|---|---|
| APROB | grep `aprob|confirm|habilit|revis|PENDIENTE_|doble` en servicios, SQL, app y backoffice; lectura de cada endpoint (lotes 1–5) | 4 |
| RESPALDO | grep `respaldo|cobertura|garant|fondo|subsid|anticip` en todo el módulo; lectura del tour de la app; ADR-051 | 1 |
| FLUJO-QR | el módulo no participa del cobro QR; se buscó si identidad/cumplimiento avanzan el flujo sin QR (gating de KYC, límites). Sin hallazgos: no hay camino de este módulo que acredite aportes | 0 |
| FLUJO-FALTANTE | búsqueda de consumidores y disparadores (`@KafkaListener`, `@Scheduled`, controladores) para cada caso de uso; retiro autorizado; detección UIF; oficios | 6 |
| COHERENCIA | contraste ADR-049/051/054, `docs/Cumplimiento.md`, catálogo de flujos, OpenAPI, textos de la app | 6 |
| DUP-INT | comparación de clases y componentes homónimos dentro de cada servicio/app | 5 |
| DUP-X | búsqueda por concepto entre servicios (CU-40 duplicado entre cumplimiento y nucleo-financiero quedó como REESCRITURA). Sin otra fila propia | 0 |
| REESCRITURA | búsqueda de casos de uso duplicados y sin referencia | 1 |
| PISADA | comentarios contradictorios y convenciones mezcladas; historial completo disponible pero no se recorrió commit por commit (§11). Se registró como DOC/COHERENCIA cuando apareció | 0 |
| PARCHE | búsqueda de valores fijos, `catch` que tragan, banderas; registrados como CLEAN/FRAGIL/DATOS | 0 |
| FRAGIL | lectura de cadenas mágicas y estados compartidos | 8 |
| DEFECTO | lectura completa de main de ambos servicios | 14 |
| HUECO | endpoints/pantallas sin implementación; CU sin entrada | 16 |
| RESILIENCIA | timeouts de clientes, estado en memoria, efectos dentro de transacciones | 4 |
| DINERO | 91.1/91.2: tipos de importe, moneda, append-only, idempotencia en retiros y límites | 7 |
| DEBIDO-PROCESO | bloqueos y acciones automáticas sin expediente; deber de reserva en casos LFT | 2 |
| DDD | lenguaje ubicuo y objetos de valor; los desvíos aparecen dentro de COHERENCIA/DINERO/ARQ. Sin fila propia | 0 |
| ARQ | capas, SQL crudo en aplicación, ADR-032 | 2 |
| CLEAN | código muerto, `$any`/`as`, literales | 5 |
| ACOPLE | lecturas de esquemas ajenos (`auditar_backend.py` invariante 11) e importaciones cruzadas | 2 |
| CONTRATO | matriz §6: OpenAPI vs controlador vs cliente vs app | 10 |
| DATOS | esquema, semillas, procedencia de catálogos (97.4/97.5.4) | 13 |
| RENDIMIENTO | listados sin paginar, N+1 | 4 |
| PRUEBAS | lectura de pruebas de autorización y plantillas; ninguna ejecutada | 7 |
| UX-A11Y | lectura de pantallas de la app y del backoffice; sin capturas | 8 |
| OBSERV | logs con PII, huellas y métricas | 2 |
| CONFIG | valores por omisión de secretos (`auditar_backend.py` «Cableado»), backup Android | 2 |
| DOC | comentarios y documentación contra el código | 5 |
| SEG | autorización por endpoint (matriz §6), tokens, MFA, RLS, PII, enumeración | 33 |
| TRAZA | bitácora de lectura, rastro de aprobaciones | 4 |
| REG | contraste con `docs/Cumplimiento.md`; parámetros normativos sin fuente | 4 |
| OTRO | lo que no encaja en otra clase | 1 |

Total de fichas en §5: 176. Un hallazgo con dos clases cuenta en la primera (la que describe la causa).

## 9. Lo que está bien

- No hay confianza en cabeceras de identidad sin firmar: `grep -rniE 'X-User|X-Usuario|X-Actor|X-Rol'` sobre identidad, cumplimiento y `comun-web` → sin resultados; la identidad se resuelve del JWT validado (`SesionDeLaPeticion.java:66-84`).
- El emisor de tokens falla al arrancar sin clave en `staging`/`production` (`EmisorDeAcceso.java:75-79`).

### servicio identidad

- Identidad del actor desde **JWT RS256 verificado en cada servicio**, nunca cabecera (`SesionDeLaPeticion.java:155-180`; `ConfiguracionDeSeguridad` sin confianza en el gateway). Guardia de permisos real (`GuardiaDePermiso`) y arranque que exige `@Permiso`/`@Publico` en cada endpoint.
- Control de objeto del expediente con token de alta HMAC atado a usuario+propósito+vencimiento, 403 idéntico (`TokenDeAlta`, `TitularDelExpediente`), con pruebas negativas.
- Argon2id con pimienta, `char[]` borrado, comparación constante; OTP de correo e invitaciones con HMAC, intentos y bloqueo por token; refresh rotatorio con detección de reuso por trigger y ventana de gracia; cookie HttpOnly/Secure/SameSite=Strict.
- Outbox en la misma transacción; intento de login registrado aunque falle (no lanza dentro de la tx).
- Respuesta mínima en titularidad (sí/no) y en contacto de grupo; teléfono por cuerpo en la búsqueda nueva; rotación de claves JWKS con `kid`.
- Precondición en la escritura en `resolver` KYC y en `pasarARevisionSiFaltabaDocumento`; `FOR UPDATE` en decisión y fecha.
- Clientes HTTP con timeout global y fallo cerrado.

### servicio cumplimiento

- Actor desde la sesión en CU-43 aprobar, CU-45 responder, CU-44 decidir, CU-47/48 aprobar/activar; el contrato rechaza `aprobadoPor`/plazo en el cuerpo (`AccionesDeCumplimiento.java:23-27`, `web/AccionesDeCumplimientoWebTest.java:129-176`).
- CU-44 decisión: transición condicional `estado <> 'CERRADO'` + reintento idéntico sin segundo evento + prueba de concurrencia real (`CasoLftRepositorio.java:71-92`, `CU44DecisionTest.java:218-244`).
- Deber de reserva en bandejas: las consultas LFT no leen `usuario_id`, montos ni resúmenes (`BandejasLftRepositorio.java:14-17`); evento `caso_abierto` sin usuario; `notificarAlTitular=false`; RLS por omisión deniega las tablas LFT al titular (`restricciones.sql:1437-1438`).
- Segregación R-SEG-04 del reporte garantizada en base (`ck_reporte_segregacion`).
- Outbox transaccional en todas las escrituras; sin publicación directa a Kafka; sin clientes HTTP salientes.
- `fail-on-unknown-properties: true` + `additionalProperties: false` (mass assignment) con pruebas (`AmbitoDeLaPersonaWebTest.java:141-183`).
- `soloElPropio` en PEP y aceptación con negativa indistinguible (AP-SEG-01).
- Umbrales UIF como dato con base normativa y cita (catálogo `umbral_reporte_uif`), coincidentes con docs/Cumplimiento.md §2.1-2.2; reporte en cero; plazos guardados y no recalculados.
- Pérdida neta leída de la columna GENERATED; `DECIMAL` y `BigDecimal` en todo importe (sin double/float).
- Controles diarios CU-54/55 idempotentes por código derivado y `SKIP LOCKED`, con tope por corrida.
- `EfectoDelReclamo` deniega por omisión y está probado en tres niveles (`dominio/EfectoDelReclamoTest.java`).
- Sin TODO/FIXME; ningún `catch` que trague errores (solo traducción a `ErrorDeNegocio`); sin PII en logs (solo conteos).

### esquema, contratos y retiros

- Doble capa real contra autoaprobación: `if` en Java + `UPDATE ... AND solicitada_por <> aprobada_por` + CHECK (`AprobacionDeRetiroRepositorio.java:22-30`, `restricciones.sql:1529-1535`); concurrencia resuelta por UPDATE condicionado.
- Idempotencia del retiro por `(cuenta, clave)` con lock y verificación de "misma solicitud" (`SolicitudDeRetiro.java:82-96`, `ReglasDeRetiro.java:135-143`, `uq_retiro_idem`).
- Retención antes de la orden; BOLA del titular antes de tomar el lock (`SolicitudDeRetiro.java:77-82`).
- Cuenta verificada + titular + enfriamiento en Java y en trigger de base (`CondicionesDeRetiro.java:64-78`, `fn_bil_validar_instrumento_retiro`).
- `SegundoFactorLocal` confinado a `local/test`; `SegundoFactorStepUp` deniega por omisión y no loguea el JWT.
- Registro UIF por umbral en la misma transacción, idempotente (`uq_operelev_tx_umbral`), con tipo de cambio reproducible y umbrales versionados con EXCLUDE.
- RLS: lo que cumplimiento escribe sobre una persona no es visible para ella (anti-soplo, `restricciones.sql:1353-1360`).
- Documento cifrado con versión de llave; tokens y contraseñas sólo como hash; R-SEG-09 (reuso de refresh revoca familia).
- Seeds dev/volumen con guarda `app.entorno='dev'`.
- Simulado alineado 1:1 con los contratos (38/28 operaciones con escenarios ok/vacío/adverso/rechazo).

### app móvil

- Tokens de acceso/refresco solo en almacén seguro; iOS `unlocked_this_device`; nada en SharedPreferences (`apps/movil/lib/proveedores/sesion.dart:28-49`). Logout revoca en servidor (`cierre_de_sesion.dart:45-57`).
- La contraseña del alta solo vive en memoria y nunca se persiste (`dominio/modelo_alta.dart:85-89`); el borrador no la incluye.
- MFA decidido por el servidor (`dominio/cu04_autenticar.dart:14-20,94-104`); sin refresh no se usa `sesionId` como credencial (`:85-90`).
- Cero `print`/`debugPrint`/logs en el lote; las rutas de archivos de documentos nunca se loguean (`dominio/capturas_del_expediente.dart:28-45`).
- FLAG_SECURE en las fases cuenta/identidad del alta (`dominio/comportamiento_de_pantalla_del_alta.dart:37-48`).
- `Token-De-Alta` de un solo propósito para operar sin sesión, nunca se envía el de otro usuario (`dominio/estado_de_verificacion.dart:43-52`).
- Idempotencia de alta, código de correo y subida (con rotación al repetir) bien pensada (`dominio/subida_del_expediente.dart:173-191`, `dominio/verificacion_correo.dart:32-58`).
- `usuarioId` para completar KYC se toma del token, no de la navegación (`pantalla_completar_kyc_de_sesion.dart:7`).
- Movimiento reducido respetado en tour, candado, transiciones y cuenta atrás; semántica cuidada en riel, miniaturas, filas de perfil; estados con texto + tono, no solo color (`seccion_verificacion_kyc.dart:129-135`).
- Consentimientos separados y botón habilitado solo tras leer hasta el final y con los tres ids vigentes (`dominio/estado_contrato.dart:49-56`).

### backoffice y web

- Deber de reserva en bandejas LFT: ni la fila ni el código piden persona/monto (`cu44-bandejas.ts:21`, contrato `cumplimiento.yaml:575-580`), "nunca avisar al titular" explícito (`decidir-caso.ts:24-26`). En `apps/web` no hay ninguna pantalla que exponga alertas, casos o ROS (grep sin aciertos).
- Idempotencia por contenido en todas las escrituras de cumplimiento, KYC y retiros; 200 vs 201 distingue reintento (`cu43-reportes.ts:42-45`).
- Estado "sin confirmar" en retiro (no confunde "sin respuesta" con "no pasó") y reintento con la misma clave (`pantalla-de-aprobacion-de-retiro.ts:173-182`).
- Fotos de cédula pedidas de a una, como binario autenticado, revocadas al salir (salvo la carrera señalada) (`tira-de-fotos.ts`); el número de documento no viaja (`cu02-presentacion.ts:64-68`).
- Cuatro estados + "denegado" en bandejas de cursor; errores traducidos con traza (`bandeja-por-cursor.ts`, `listado-de-bandeja.ts`); export de datos de identidad condicionado a permiso (`pantalla-de-expedientes.ts:67,110`).
- Datos de ejemplo de Sistemas solo en `development`/`demo` por `fileReplacements`; producción usa el proveedor real (`apps/backoffice/angular.json:65-96`).
- Ingreso: credencial solo en memoria, `volverA` restringido a rutas internas (`pantalla-de-ingreso.ts:189-193`), sin distinguir cuenta inexistente.
- Invitación web: secreto en fragmento, retirado de historial, `Referrer-Policy: no-referrer`, `noindex`, `RenderMode.Client` (`app.routes.server.ts:34-38`).
- Autoampliación de permisos bloqueada en servidor (`UsuariosInternosRepositorio.java:249`).

## 10. Observado fuera de alcance

- `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU40EvaluarLimites.java:80,132` — `reloj.ahora().atOffset(ZoneOffset.UTC)` para la vigencia de límites (mismo patrón que IDCU-DEFECTO-003; lote L1).

- **nucleo-financiero / recarga (lote L1)** — La recarga la acredita TESORERIA a mano; el catálogo dice que la acredita el proveedor con firma: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/BilleteraController.java:141-148`. Acredita saldo por acción de TESORERÍA y no por webhook firmado; choca con 91.3.1 y R-NUEVA-1. Propuesto S1 por el subagente; no es de este módulo.

- `scripts/auditar_backend.py` — falso positivo: cuenta como «declaradas sin implementar» operaciones heredadas de una clase abstracta (`servicios/cumplimiento/.../web/ColasDeCumplimientoWeb.java:28`).
- `scripts/verificar_rutas_frontend.py` — exit 1 por una ruta de grupos que ya tiene contrato (`GET /grupos/solicitudes-escaladas`), lote L3.

### servicio identidad

- `plataforma/gateway/.../LimitadorDeTasa.java`: clave de rate limit = IP + `Authorization.hashCode()`; `/sesion/**` y `/usuarios-internos/**` sin rate limit (`rutas.yml:107-112`).
- `plataforma/comun-web/.../ClienteDeServicio.java:302-310`: registra en log y en el mensaje de error el cuerpo de respuestas 4xx/5xx de otros servicios (posible PII).
- `nucleo-financiero/.../ConsumidorDeUsuariosKafka.java:33`: abre billetera al registrarse aun sin KYC.
- `grupos/.../PersonasPorHttp.java:25`: pide `/usuarios/{id}/kyc` de terceros con token de participante (403→vacío).
- `sql/40_reglas/restricciones.sql:1326-1328`: un solo rol privilegiado de fila para todo el personal.

### servicio cumplimiento

- `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/ClienteConCircuito.java:35,72` — propaga el token del usuario a llamadas entre servicios (98.6.3).
- `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/HechosPorHttp.java:160` — servicio interpolado en la URL sin codificar.
- `sql/60_semillas/06-umbrales-uif.sql:5-28` — `vigente_desde = current_date`.
- `sql/40_reglas/restricciones.sql:1326-1329` — `fn_seg_rol_privilegiado` solo reconoce `BACKOFFICE|CUMPLIMIENTO|AUDITOR`; si el `rol` del token de un analista es otro (p. ej. `OFICIAL_CUMPLIMIENTO`), la RLS dejaría vacías sus bandejas (sospecha, no verificado).
- `plataforma/comun-web/.../idempotencia/Idempotencia.java` — existe y no la usa cumplimiento.

### esquema, contratos y retiros

- `servicios/grupos/.../grupos.yaml:1031` publica `consultarSiCompartenGrupo` (productor del pacto).
- `sql/40_reglas/restricciones.sql:1501-1516` `ck_entrega_segregacion` (entregas) y `ck_reverso_segregacion` exigen aprobación humana en entregas/reversos (lote de entregas).
- `CU13RetenerSaldo.ejecutar` vía `cerrarRetencion` deja al titular "ejecutar" su propia retención sin movimiento (consume saldo sin asiento).
- `ADR-017`: FK cruzadas entre esquemas como desvío declarado de 98.1.

### app móvil

- `servicios/identidad/src/main/resources/openapi/identidad.yaml:1038-1048` — `BorradorOnboarding` con enum y patrón de teléfono distintos del resto del contrato (ver H12).
- `servicios/identidad/src/main/resources/openapi/identidad.yaml:350-352` — `resolverVerificacion` (decisión humana del KYC), punto APROB del servidor.
- `.gitignore:51` — `clientes/dart/` ignorado: el cliente generado no está versionado; el contraste por campos/enums se hizo contra el spec, no contra el código generado.
- `packages/diseno_flutter` — `ArteFondoDeGarantia` (ilustración de cobertura, H01).

### backoffice y web

- `apps/backoffice/src/app/nucleo/borrador.ts:7-16` — IndexedDB sin cifrado ni purga; abre la base en cada operación sin cerrarla.
- `servicios/identidad/.../UsuariosInternosRepositorio.java:181-183` — contraseña provisional generada y descartada.
- `servicios/identidad/.../openapi/identidad.yaml:271-291,830-859` — listados sin paginación.
- `packages/ui/src/listado/listado.ts:98` — `exportable` por defecto `true`.
- `apps/backoffice/src/app/rutas/postulaciones/pantalla-de-postulacion.ts:54` — enlace a habilitación (aprobación de postulación, otro lote).

## 11. Límites de esta auditoría

- Gradle no corrió: `./gradlew :servicios:identidad:testBarrido` → `BUILD FAILED`, «Received status code 429 from server: Too Many Requests» al resolver dependencias de `buildSrc` (`evidencia/gradle-identidad-testBarrido.txt`). Además la generación jOOQ exige PostgreSQL con el esquema aplicado (`buildSrc/src/main/kotlin/aportaya.jooq.gradle.kts:22`) y el metaprompt prohíbe aplicar migraciones. Todas las pruebas Java del módulo y el barrido de tamaño/umbrales/secretos quedan **NO COMPROBADO**.
- `yarn typecheck` (backoffice) no corrió: «packageManager yarn@4.18.0 … current global version of Yarn is 1.22.22» (Corepack deshabilitado; no se instaló nada). Flutter/Dart no están instalados. **NO COMPROBADO**.
- No se ejecutaron `scripts/inventario_endpoints.py` ni `scripts/matriz_de_pruebas.py` sin `--check`/`--servicio` porque escriben archivos del repo; `inventario_endpoints.py --check` sí corrió.

- Supuesto de frontera: el resto de `nucleo-financiero` (recarga, mayor, QR) es del lote L1; pantallas de grupos/admisiones/postulaciones del lote L3; `plataforma/*` del lote L5. De ellos se leyó solo el punto de contacto.
- Historial: completo, pero no se recorrió commit por commit buscando PISADA; los rastros de pisada se registraron cuando aparecieron en el código (comentarios contradictorios, convenciones mezcladas).
- Ejecución: nada se ejecutó contra una base ni se levantó ningún servicio, ni la app. Todo el informe está en peldaño DISCOVERED salvo las salidas de los verificadores Python de `scripts/` (en `evidencia/`).
- Clientes generados: `clientes/dart/*/lib/src` y `clientes/angular/*` no están versionados (`.gitignore`); la comparación se hizo contra los OpenAPI fuente.
- Lectura de pruebas: se leyeron completas las de autorización, step-up, UIF y PEP; el resto se inventarió por nombre (ver cada lote).
- Severidad: el orquestador ajustó la severidad de 8 fichas propuestas por los subagentes (5 S1→S2, 2 S1 mantenidos tras re-verificar, 1 S1 de recarga movido a §10); cada ajuste lleva «Nota del orquestador» en su ficha.
- Datos personales: el informe no contiene datos de participantes; los importes citados (por ejemplo, el umbral 5000.00 de `application.yml`) son parámetros de configuración del repo, no importes de personas.

### servicio identidad

- No se ejecutó nada (regla del contrato): ninguna afirmación pasa de DISCOVERED.
- Tests: leídos completos 6 (ArquitecturaTest, SeguridadWebTest, UsuariosAutorizacionNegativaWebTest, StepUpTest, CableadoDelSegundoFactorTest, CU04RechazosTest) y parcial BaseDeCU04; del resto se inventariaron nombres `@DisplayName` y cabeceras. No se leyeron completos CU04Test, CU04RenovacionTest, CU02RevisionTest, CU02DecisionDelServidorTest, CU69*, CU08*/CU09*, VerificacionesControllerWebTest, UsuariosSubirDocumentoWebTest, SesionControllerWebTest, SesionesStepUpWebTest, UsuariosInvitacionesKycWebTest (por tiempo; su cobertura se infirió de los nombres).
- No se verificaron políticas RLS concretas de `identidad.usuario` (UPDATE por BACKOFFICE) ni del resto de tablas: condiciona la severidad de F02 y F06.
- `PlantillaCorreoDeVerificacion.java` leído hasta la línea 60 (resto es HTML).
- OpenAPI: líneas 830–980 (usuarios-internos/roles/permisos paths) y 1500–1631 (schemas de internos) leídas por extracción de índices y schemas puntuales, no línea a línea.
- `gradle.lockfile` (221) y `.jqwik-database` no revisados.

### servicio cumplimiento

- `gradle.lockfile` no auditado línea a línea (dependencias; fuera del foco regulatorio).
- `openapi/cumplimiento.yaml` (1761 líneas): leídos cabecera de errores, todas las rutas/operationId, permisos en `description`, `Idempotency-Key` y los esquemas con campos sensibles (`aprobadaPor`, `analistaId`, `formulario`, `aceptadoPorElOrganismo`); no se cotejó cada esquema de respuesta campo por campo.
- Pruebas: leídas completas las de autorización (`AutorizacionNegativaWebTest`, `AmbitoDeLaPersonaWebTest`, `SeguridadWebTest`), `CU44DecisionTest`, `CU44Test`; de UIF/PEP/KYC (CU-41/42/43/06/03/02) se leyeron nombres y las aserciones clave citadas; el resto inventariado con una frase.
- `TableroDeCumplimientoWeb.java` y `TableroDeCumplimientoRepositorio.java` leídos en su primera mitad y el resto por estructura (mapeos y SQL análogos).
- Plataforma: solo `GuardiaDePermiso`, `SesionDeLaPeticion`, `PermisoAlternativo`, `Datos` (set_config) e `Idempotencia` (existencia).
- Nada ejecutado (contrato del lote): las fallas de GRANT/RLS son por lectura del SQL, no reproducidas.

### esquema, contratos y retiros

- No se leyó `servicios/identidad` ni `servicios/cumplimiento` salvo greps de cotejo (`DocumentoDeIdentidad`, `MapeoDelAlta`, `BorradorOnboardingRepositorio`, `ExpedienteController @Publico`), por contrato del lote.
- `sql/30_indices/12_cumplimiento_asfi.sql`, `restricciones.sql` fuera de los rangos citados, y `prueba_humo.sql` fuera de UIF: no leídos completos.
- `CU-01/02/03/06/08/18/41/43/44` sólo por grep de aprobaciones/umbrales; no se contrastaron línea a línea.
- Postman: inventario de requests y headers, no los scripts de prueba.
- Nada ejecutado: los hallazgos "probable" (doble gasto por liberar retención, costo sin línea, step-up no ligado) requieren una prueba de integración para confirmarse.

### app móvil

- No se ejecutó nada (ni `flutter test` ni la app): todos los hallazgos son peldaño DISCOVERED; ninguno está verificado en runtime ni con capturas.
- Tests: de 112 archivos de `apps/movil/test/**` que importan identidad (14 999 líneas) solo se leyeron por grep dirigido y los rangos citados; no se auditó cada aserción.
- `lib/dominio/cliente.dart`, `cierre_de_sesion.dart`, `biometria_ios.dart` y el lado Android de la biometría: solo rangos/grep (punto de contacto), no lectura completa.
- No se pudo comparar contra el código Dart generado (`clientes/dart/identidad/lib/src/` ausente); se usó el OpenAPI fuente.
- Si el «fondo de garantía» se financia con dinero de la empresa o de los participantes no se puede determinar desde este lote (H01 queda «probable»).

### backoffice y web

- Specs (`*.spec.ts`, ~3.5k líneas): se leyeron todos los títulos y por muestreo `pantalla-de-caso.spec.ts:55-75`; no se leyó cada aserción.
- `clientes/angular/*` está en `.gitignore` y no existe en el checkout (solo `publicidad`): no se pudo contrastar los tipos generados con los manuales; el contraste se hizo contra los YAML.
- No se leyeron `nucleo/` (interceptores de errores, idempotencia, sesión, timeout HTTP): el timeout de clientes y la traducción de errores se asumen ahí y quedan sin verificar.
- `sistemas/dominio/accesos.ts` leído hasta la línea 60; resto de `sistemas/` (fuera de lote) no leído.
- No se ejecutó nada: ningún hallazgo está verificado en runtime (peldaño `DISCOVERED`).

## 12. Anexo de cobertura de archivos

Una tabla por lote, tal como la registró cada lectura. «Leído completo: no» lleva su motivo.

### servicio identidad


### main (todos leídos completos)
| ruta (relativa a `servicios/identidad/`) | líneas | leído completo | qué hace |
|---|---|---|---|
| README.md | 68 | sí | Ficha del servicio; declara CU-01/02/04/07/08/09 "Implementado"; tablas de eventos vacías |
| build.gradle.kts | 50 | sí | Plugins servicio/jooq/openapi, deps (argon2, oauth2, kafka, shedlock, resilience4j) |
| descriptor.yml | 30 | sí | N1, réplicas min 4 / max 10, hikari_por_replica 10 |
| src/main/resources/application.yml | 82 | sí | Config; pimienta obligatoria; claves JWT/invitaciones opcionales; hikari 20 |
| src/main/resources/openapi/identidad.yaml | 1631 | sí (paths completos; schemas leídos 980–1631) | Contrato OpenAPI 3.1 |
| Aplicacion.java / ConfiguracionDeIdentidad.java | 21/29 | sí | Arranque; beans Reloj/Ids/Outbox |
| aplicacion/ActivarRolOrganizador.java | 59 | sí | CU-90: firma contrato + llama a organizador + concede ORGANIZADOR |
| aplicacion/AnotarFotoEnExpediente.java | 48 | sí | Anota clave de foto en documento/verificación (tx corta, contexto sistema) |
| aplicacion/BuscarPorTelefono.java | 40 | sí | Teléfono→UUID como contexto de sistema |
| aplicacion/CU01RegistrarUsuario.java | 291 | sí | Alta: usuario, credencial, documento, verificación, consentimientos, outbox |
| aplicacion/CU02ActualizarFechaExpiracion.java | 35 | sí | Titular corrige fecha de vencimiento |
| aplicacion/CU02ConsultarEstadoDeVerificacion.java | 42 | sí | Estado del expediente (contexto sistema) |
| aplicacion/CU02GuardarFotoDelExpediente.java | 72 | sí | Sube foto al almacén y anota |
| aplicacion/CU02IniciarVerificacion.java | 73 | sí | KYC diferido: INCOMPLETO_KYC→PENDIENTE_VERIFICACION |
| aplicacion/CU02RevisarExpediente.java | 133 | sí | Cola KYC, fotos y **decisión humana** |
| aplicacion/CU04Autenticar.java | 224 | sí | Login, lockout, factor, sesión |
| aplicacion/CU04StepUp.java | 117 | sí | Step-up: desafío en memoria + evidencia JWT |
| aplicacion/CU07FotoDePerfil.java | 51 | sí | Foto de perfil propia |
| aplicacion/CU08AsignarRol.java / CU08RevocarRol.java | 118/76 | sí | Asignar/revocar rol con SoD (sin endpoint) |
| aplicacion/CU09CambiarCredencial.java / CU09SolicitarBaja.java | 136/69 | sí | Cambio de clave / baja (sin endpoint) |
| aplicacion/CerrarSesion.java | 55 | sí | Logout: revoca sesión + refresh |
| aplicacion/ConfirmarVerificacionCorreo.java | 31 | sí | Confirma OTP de correo |
| aplicacion/ConsultarContactoEnGrupo.java | 32 | sí | Teléfono de un integrante tras preguntar a grupos |
| aplicacion/ConsultarMiPerfil.java | 83 | sí | Perfil propio |
| aplicacion/ConsultarNivelKyc.java | 43 | sí | Nivel KYC (propio o por rol) |
| aplicacion/ConsumirInvitacion.java | 175 | sí | Canje/revocación de token de invitación (SQL crudo) |
| aplicacion/EmitirAcceso.java | 89 | sí | Calcula permisos y firma JWT de acceso |
| aplicacion/EmitirTokenDeInvitacion.java | 175 | sí | Emite token de invitación idempotente con límite diario |
| aplicacion/EntradaAutenticacion.java | 31 | sí | Record de entrada login |
| aplicacion/FotoDePerfilEnLaCuenta.java | 55 | sí | url_avatar |
| aplicacion/GestionarUsuariosInternos.java | 153 | sí | CRUD personal interno + outbox |
| aplicacion/GuardarBorradorOnboarding.java | 29 | sí | Guarda borrador del alta |
| aplicacion/IndicadoresDeIdentidad.java | 43 | sí | Conteos para tableros |
| aplicacion/RenovarSesion.java | 253 | sí | Refresh rotatorio con detección de reuso |
| aplicacion/SolicitarVerificacionCorreo.java | 73 | sí | OTP de correo previo al alta |
| aplicacion/ValidarTokenDeInvitacion.java | 61 | sí | Valida token + teléfono + KYC mínimo |
| aplicacion/VerificarTitularidad.java | 88 | sí | Sí/no nombre+documento para entregas |
| dominio/*.java (17 + 3 puertos) | ~860 | sí | Átomos: ámbitos, documento, SoD, políticas, perfiles, resultado |
| infraestructura/AccesoRepositorio.java | 179 | sí | Intentos, dispositivos, sesiones, bloqueos |
| infraestructura/AccesosRepositorio.java | 148 | sí | Roles/permisos/asignaciones |
| infraestructura/Argon2Hasheador.java | 67 | sí | Argon2id + pimienta |
| infraestructura/BorradorOnboardingRepositorio.java | 208 | sí | Borrador del alta (texto plano) |
| infraestructura/ContactoEnGrupoRepositorio.java | 31 | sí | Lee teléfono como sistema |
| infraestructura/CorreoDeAccesoRepositorio.java | 47 | sí | Correo verificado para OTP de acceso |
| infraestructura/CorreoDeVerificacionLocal.java | 26 | sí | Adaptador de correo nulo |
| infraestructura/CredencialRepositorio.java | 85 | sí | Historial, reemplazo, cierre de sesiones, baja |
| infraestructura/DesafioDeDesarrollo.java | 81 | sí | Factor fijo `000000` con perfil `local` |
| infraestructura/DesafioLocal.java | 104 | sí | Único DesafioDeFactor "real": códigos en memoria |
| infraestructura/EmisorDeAcceso.java | 232 | sí | Firma RS256, JWKS, evidencia step-up |
| infraestructura/ExpedienteRepositorio.java | 76 | sí | Anota fotos (sin precondición de estado) |
| infraestructura/FechaDeExpiracionRepositorio.java | 45 | sí | Actualiza fecha con FOR UPDATE |
| infraestructura/FilaDeExpediente.java | 75 | sí | Mapeo fila→expediente |
| infraestructura/FirmaDeOrganizadorRepositorio.java | 136 | sí | Token FIRMA_CONTRATO + rol ORGANIZADOR como sistema |
| infraestructura/GmailApiCorreoDeVerificacion.java | 97 | sí | Envío Gmail API |
| infraestructura/IndicadoresDeIdentidadRepositorio.java | 52 | sí | Conteos |
| infraestructura/PlantillaCorreoDeVerificacion.java | 123 | sí (texto/HTML) | Plantilla HTML del OTP |
| infraestructura/ProteccionDeVerificacionCorreo.java | 57 | sí | HMAC de código y destino con la pimienta |
| infraestructura/RegistroRepositorio.java | 269 | sí | Escrituras del alta |
| infraestructura/RevisionRepositorio.java | 236 | sí | Cola KYC, decisión, activación y rol base |
| infraestructura/RolesYPermisosRepositorio.java | 83 | sí | Catálogos |
| infraestructura/RotacionDeRolesInternos.java | 83 | sí | Reemplazo de roles de internos |
| infraestructura/SecretoDeInvitacion.java | 40 | sí | HMAC de invitaciones |
| infraestructura/TextoDeVerificacion.java | 42 | sí | Enmascarado + intento |
| infraestructura/UsuarioInternoMapeador.java | 59 | sí | Mapeo DTO |
| infraestructura/UsuarioRepositorio.java | 84 | sí | Lecturas del login |
| infraestructura/UsuariosInternosRepositorio.java | 287 | sí | Alta/edición/roles/desbloqueo internos |
| infraestructura/VerificacionCorreoRepositorio.java | 257 | sí | OTP de correo |
| infraestructura/clientes/GruposPorHttp.java / OrganizadorPorHttp.java | 41/50 | sí | Clientes HTTP salientes |
| web/*.java (17 archivos) | ~1600 | sí | Controladores, token de alta, cookie, límite |

### test (inventario; ver Límites)
| ruta (src/test/java/bo/aportaya/identidad/) | líneas | leído completo | qué hace |
|---|---|---|---|
| ArquitecturaTest | 60 | sí | ArchUnit: dominio puro, @Transactional solo en aplicación, sin import cruzado, sin JPA, web↛infra |
| web/SeguridadWebTest | 123 | sí | Barrido 401/403 de todas las rutas (casos de uso mockeados) |
| web/UsuariosAutorizacionNegativaWebTest | 280 | sí | Matriz negativa por rol: búsqueda, titularidad, invitación, CU-90 (mocks) |
| StepUpTest | 132 | sí | Evidencia step-up con DesafioDeFactor MOCK |
| CableadoDelSegundoFactorTest | 58 | sí | Confirma que TEST corre perfil `local` (factor 000000) + Gmail |
| CU04RechazosTest | 194 | sí | Reglas R-xxx en base; escribe en `comun` y `nucleo_financiero` |
| web/UsuariosInternosControllerWebTest | 231 | no (nombres de prueba) | 9 casos felices + 401/403; sin SoD ni escalada |
| UsuariosInternosRepositorioTest | 122 | no (nombres) | Alta de operador contra Testcontainers |
| web/VerificacionesControllerWebTest | 299 | no (nombres) | Cola/fotos/decisión: permiso, cara inválida |
| web/UsuariosSubirDocumentoWebTest | 299 | no (nombres) | Token de alta: ajeno/vencido/forjado/403 idéntico |
| web/SesionControllerWebTest | 177 | no (nombres) | Refresh web/móvil, logout |
| CU04RenovacionTest | 241 | no (nombres) | Rotación/reuso/gracia de refresh contra BD real |
| CU04Test / CU04CodigoPorCorreoTest / CU04IngresoConCorreoTest / CU04CuentaSinDocumentoTest | 278/149/111/58 | no (nombres) | Login, lockout, factor por correo |
| web/SesionesStepUpWebTest / SesionesControllerWebTest / SesionesIngresoConCorreoWebTest | 115/279/116 | no (nombres) | Contrato HTTP de sesiones (mocks) |
| CU08Test / CU08RechazosTest / CU08CodigosDeRolVigentesTest / BaseDeCU08 | 199/129/58/106 | no (nombres) | CU-08 a nivel caso de uso (sin endpoint en producción) |
| CU09Test / CU09RechazosTest / BaseDeCU09 | 224/152/194 | no (nombres) | CU-09 a nivel caso de uso (sin endpoint) |
| CU69* (5) / BaseDeEmisionDeInvitacion | ~600 | no (nombres) | Token de invitación: emisión, política, auditoría, validación |
| web/UsuariosInvitacionesKycWebTest | 199 | no (nombres) | Canje/revocación/KYC 401/403/422 |
| CU02RevisionTest / CU02DecisionDelServidorTest / CU02NivelKycTest | 249/134/72 | no (cabecera) | Reglas de aprobación KYC |
| CU01*Test (5) / BaseDeCU01 / DoblesDeLaCoreografia | ~1200 | no (cabecera) | Alta |
| CU07*Test, CU90RolOrganizadorTest, CUVerificarTitularidadTest, CUIndicadoresDeIdentidadTest | ~640 | no (cabecera) | Foto perfil, organizador, titularidad (BD real), indicadores |
| ContactoEnGrupoRepositorioTest, infraestructura/clientes/GruposPorHttpPactoContratoTest, web/ContactoEnGrupoWebTest | 105/164/69 | no (cabecera) | Teléfono de integrante + pacto consumidor con grupos |
| Borrador*Test (2), web/MapeoDelBorradorTest | 159/198/155 | no (cabecera) | Borrador del alta |
| DesafioDeDesarrolloTest, DesafioPorCorreoTest, GmailApiCorreoDeVerificacionTest | 54/84/105 | no (nombres) | Factores y correo |
| RotacionDeClavesDeFirmaTest, web/JwksControllerWebTest | 169/88 | no (cabecera) | Rotación JWKS |
| EsquemaAlDiaRepositorioTest, FechaDeExpiracionRepositorioTest, Arranque*Test (3), BarridoTest | ~550 | no (cabecera) | Deriva jOOQ, fecha, arranque, barrido |
| web/Usuarios*WebTest restantes (Consultas, Controller, FechaExpiracion, FotoDePerfil, IniciarVerificacion, TitularidadLimite, VerificacionCorreo), web/EstadoDeVerificacionWebTest(+Complemento), web/Permisos/RolesControllerWebTest | ~1900 | no (cabecera) | Contrato HTTP con casos de uso mockeados |
| FixturaDeIdentidad, ExpedientesDePrueba, CorreoEnMemoria, BaseDeCU04 | ~520 | parcial (BaseDeCU04 40–60) | Fixtures; BaseDeCU04 usa Testcontainers o `APORTAYA_TEST_DB_URL` |

### servicio cumplimiento

| ruta | líneas | leído completo (sí/no + motivo) | qué hace (una frase) |
|---|---|---|---|
| servicios/cumplimiento/.jqwik-database | 1 | sí | Vacío |
| servicios/cumplimiento/README.md | 116 | sí | Ficha del servicio; declara CU 'Implementado' y eventos (incompleto) |
| servicios/cumplimiento/build.gradle.kts | 45 | sí | Plugins de convención, pisos de cobertura, dependencias |
| servicios/cumplimiento/descriptor.yml | 30 | sí | Nivel N3, réplicas, sondas; nivel_porque inexacto |
| servicios/cumplimiento/gradle.lockfile | 206 | no (lockfile) | Lockfile de dependencias (no auditado línea a línea) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/Aplicacion.java | 26 | sí | Arranque Spring + @EnableScheduling |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/ConfiguracionDeCumplimiento.java | 103 | sí | Beans de política: requisitos, plazos LFT/incidente, escalas, roles incompatibles (sin fuente) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/AccionesDeCumplimiento.java | 133 | sí | Fachada HTTP CU-43/45/56 con reintento por estado y actor de sesión |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU02ElevarDiligencia.java | 194 | sí | Eleva nivel DD; cuatro ojos PEP con ids del llamador |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU03DeclararPep.java | 219 | sí | Declaración PEP + BF (DELETE/INSERT) + recalificación; alConfirmarseCoincidencia sin llamador |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU05AceptarContrato.java | 176 | sí | Aceptación append-only con hash de evidencia; consentimientos por evento |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU05ConsultarContratosVigentes.java | 44 | sí | Lista contratos vigentes (ruta pública) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU06RevisarConocimiento.java | 212 | sí | Revisión KYC y desvío de perfil; sin disparador |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU40EvaluarLimites.java | 98 | sí | Evalúa límites leyendo nucleo_financiero; sin disparador |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU41RegistrarPcc01.java | 272 | sí | Detección PCC-01; tipo de cambio y acumulado del llamador; sin disparador |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU42RegistrarRog.java | 138 | sí | Detección ROG; sin disparador |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU43RemitirReportes.java | 273 | sí | Genera/aprueba/registra envío de reportes UIF (archivo no persistido) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU44DecidirComoRevisor.java | 158 | sí | Decisión de caso LFT con revisor = sesión |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU44InvestigarYReportar.java | 265 | sí | Abre caso (analista del cuerpo) y decide (versión insegura, solo tests) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU45AtenderRequerimiento.java | 196 | sí | Registra/responde oficio; bloqueo sin ejecución |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU46VerificarAlcance.java | 156 | sí | Decide habilitación por licencia/sandbox; emite rechazo |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU47EvaluarRiesgoDeProducto.java | 212 | sí | Evaluación de producto; aprobación con booleanos autodeclarados; sin entrada |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU48CalibrarReglas.java | 213 | sí | Reglas LFT: crear/simular/activar/triar con autodeclaraciones; sin entrada |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU49DesignarOficial.java | 218 | sí | Designación/baja de oficial y capacitación; sin entrada |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU52AtenderReclamo.java | 275 | sí | Ingreso/prórroga/respuesta de reclamos; solo responder expuesto |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU53ElevarReclamo.java | 171 | sí | Segunda instancia; sin entrada |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU54EscalarPlanesVencidos.java | 140 | sí | Control diario planes vencidos -> hallazgo |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU54RegistrarRiesgoOperativo.java | 225 | sí | Registra evento de riesgo operativo + plan |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU55EscalarIncidentesVencidos.java | 106 | sí | Control diario incidentes sin reportar -> hallazgo |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU55GestionarIncidente.java | 262 | sí | Registra incidente, reporta, notifica |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU56EjecutarPruebaDeContinuidad.java | 162 | sí | Registra prueba BCP, resultado calculado, plan de acción |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU94ElevarAlComite.java | 212 | sí | Sesión de comité con quórum; aprueba evaluaciones; sin entrada |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/ConsultarBandejaDeReclamos.java | 121 | sí | Bandeja y ficha de reclamos paginadas |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/ConsultarBandejasLft.java | 159 | sí | Bandejas de alertas y casos LFT |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/ConsultarEfectoDelReclamo.java | 111 | sí | Si un reclamo frena deuda/sanción (consumido por garantia/organizador) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/ConsultarTableroDeCumplimiento.java | 281 | sí | Colas paginadas de reportes, oficios, riesgos, incidentes, pruebas, actas |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/ArchivoRegulatorio.java | 54 | sí | Arma contenido determinista y SHA-256 (contenido no se guarda) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/ClasificacionDeEvento.java | 106 | sí | Taxonomía riesgo operativo y validaciones |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/ClasificacionPep.java | 79 | sí | Clasifica PEP/BF a nivel de riesgo |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/CoberturaDeCapacitacion.java | 59 | sí | Cobertura de capacitación por periodo |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/ConceptoRog.java | 34 | sí | Mapeo tipo de transacción -> concepto ROG |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/ContratoPublicado.java | 32 | sí | Record de contrato publicado |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/DesvioDePerfil.java | 73 | sí | Cálculo de desvío % y severidad |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/EfectoDelReclamo.java | 100 | sí | Regla pura: frena/no frena deuda o sanción |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/EstadoDeLicencia.java | 29 | sí | Enum de estados de licencia |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/EvaluacionDeLimites.java | 73 | sí | Veredicto de límites (sin moneda) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/EvidenciaDeAceptacion.java | 65 | sí | Hash de evidencia de aceptación |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/ExpresionDeRegla.java | 117 | sí | Valida expresión de regla LFT y proporcionalidad |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/HabilitacionDeServicio.java | 111 | sí | Decide vía de habilitación licencia/sandbox |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/NivelDeDiligencia.java | 27 | sí | Enum ordenado de niveles DD |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/ObjetivosDeContinuidad.java | 60 | sí | Compara RTO/RPO |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/PeriodicidadDeRevision.java | 26 | sí | Meses de revisión por riesgo |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/PlazoDelReclamo.java | 74 | sí | Plazo hábil, conservación 10 años, prórroga |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/PlazosDelIncidente.java | 56 | sí | Plazos de reporte/notificación por severidad |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/QuorumDeComite.java | 78 | sí | Quórum, composición, votos viciados |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/RequisitosDeNivel.java | 38 | sí | Documentos faltantes por nivel |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/RiesgoDelProducto.java | 120 | sí | Matriz de riesgo de producto |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/UmbralAlcanzado.java | 66 | sí | Medición y conversión a USD (CU-41 no la usa) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/VersionAceptable.java | 32 | sí | Versión de contrato aceptable |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/AccionesDeCumplimientoRepositorio.java | 82 | sí | Lecturas de oficio/prueba para reintentos |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/AceptacionRepositorio.java | 84 | sí | INSERT aceptacion_contrato |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/BandejaDeReclamosRepositorio.java | 104 | sí | SQL de bandeja/ficha de reclamos |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/BandejasLftRepositorio.java | 121 | sí | SQL de bandejas LFT sin PII |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/CalificacionRiesgoRepositorio.java | 85 | sí | Cierra/abre calificación vigente |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/CasoLftRepositorio.java | 131 | sí | Casos LFT: abrir, decidir condicional, consulta |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/ContinuidadRepositorio.java | 129 | sí | Planes y pruebas BCP (2 métodos sin uso) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/ContratoRepositorio.java | 131 | sí | Contratos y tarifario (lee catalogo.tarifario) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/DeclaracionPepRepositorio.java | 77 | sí | PEP y BF con DELETE; LocalDate.now() |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/DiligenciaRepositorio.java | 95 | sí | DD: abrir, elevarTipo sin id, completar |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/EvaluacionProductoRepositorio.java | 104 | sí | Evaluaciones de producto versionadas |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/GobiernoRepositorio.java | 267 | sí | Comités, actas, oficial, capacitación, hallazgos (check-then-insert) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/IncidenteSeguridadRepositorio.java | 198 | sí | Incidentes, activos, designación; SKIP LOCKED |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/LicenciaRepositorio.java | 76 | sí | Lee catalogo.licencia_regulatoria |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/LimiteRepositorio.java | 132 | sí | Lee catalogo y nucleo_financiero (sin GRANT, sin FOR UPDATE) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/MonitoreoLftRepositorio.java | 212 | sí | Reglas y alertas LFT |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/OperacionRelevanteRepositorio.java | 191 | sí | Umbrales UIF, registros, declaración de origen |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/PerfilTransaccionalRepositorio.java | 34 | sí | Perfil declarado más reciente |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/ReclamoRepositorio.java | 285 | sí | Reclamos, instancias, efecto por usuario |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/ReporteRegulatorioRepositorio.java | 197 | sí | Catálogo, reportes, envíos |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/RequerimientoRepositorio.java | 102 | sí | Oficios de autoridad |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/RevisionKycRepositorio.java | 156 | sí | Revisiones KYC, desvíos (upsert), alertas |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/RiesgoOperativoRepositorio.java | 207 | sí | Eventos, planes, hallazgos automáticos |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/SandboxRepositorio.java | 64 | sí | Entorno de prueba regulado |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/TableroDeCumplimientoRepositorio.java | 275 | sí | SQL de colas del backoffice |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/trabajos/ControlDeIncidentesVencidos.java | 43 | sí | @Scheduled 06:45 CU-55 |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/trabajos/ControlDePlanesVencidos.java | 58 | sí | @Scheduled 06:15 CU-54 |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/AccionesDeCumplimientoWeb.java | 128 | sí | Mapeo HTTP de CU-43/45/56 (201/200) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/AdhesionController.java | 183 | sí | CU-02/03/05: soloElPropio; aprobadores del cuerpo |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/ColasDeCumplimientoWeb.java | 76 | sí | Colas /cumplimiento y registro de prueba |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/CumplimientoController.java | 268 | sí | /cumplimiento: CU-44, 54, 55, adhesión |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/LicenciaController.java | 58 | sí | GET /licencia/alcance (PARTICIPANTE) |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/MapeoDeLft.java | 62 | sí | Mapeo de bandejas LFT |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/MapeoDeReclamos.java | 51 | sí | Mapeo de reclamos |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/ReclamosController.java | 126 | sí | /reclamos: bandeja, ficha, respuesta, efecto |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/TableroDeCumplimientoWeb.java | 212 | sí | Mapeo de colas a DTO |
| servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/web/UifController.java | 81 | sí | /uif: reportes y requerimientos |
| servicios/cumplimiento/src/main/resources/application.yml | 90 | sí | Config: jackson fail-on-unknown, plazos y umbrales internos sin fuente |
| servicios/cumplimiento/src/main/resources/openapi/cumplimiento.yaml | 1761 | parcial: cabecera, rutas, permisos, campos sensibles | Contrato 3.1: 27 operaciones + catálogo de errores de CU sin ruta |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/ArmadoDeOla3.java | 117 | sí | Cableado manual de CU de ola 3 para pruebas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/ArquitecturaTest.java | 60 | parcial: nombres de pruebas y aserciones clave | ArchUnit: dirección de dependencias |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/ArranqueTest.java | 65 | parcial: nombres de pruebas y aserciones clave | Contexto levanta y rutas en prefijos |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/BarridoTest.java | 28 | parcial: nombres de pruebas y aserciones clave | Reglas de barrido sobre fuentes |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/BaseDeCumplimiento.java | 242 | sí | Base de integración: conecta como dueño del contenedor (sin SET ROLE) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU02RechazosTest.java | 150 | sí | Rechazos CU-02 (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU02Test.java | 185 | sí | CU-02 + plantillas transversales (leído completo; criterio3 prueba función SQL, no LIMITADA) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU03Test.java | 290 | sí | CU-03 PEP/BF + plantillas (leído completo; corre como dueño, no detecta DELETE/RLS) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU05Test.java | 270 | parcial: nombres de pruebas y aserciones clave | CU-05 aceptación + plantillas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU05VigentesTest.java | 80 | parcial: nombres de pruebas y aserciones clave | Contratos vigentes |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU06Test.java | 267 | sí | CU-06 (leído completo; 'LIMITADA' solo verifica el evento) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU40RechazosTest.java | 107 | parcial: nombres de pruebas y aserciones clave | Rechazos CU-40 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU40Test.java | 204 | parcial: nombres de pruebas y aserciones clave | CU-40 contra nucleo_financiero como dueño |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU41CuadreTest.java | 182 | sí | Plantillas cuadre/compensación CU-41 (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU41RechazosTest.java | 238 | sí | Rechazos R-UIF-01..05 (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU41Test.java | 225 | sí | CU-41 (leído completo; reinicio de ventana lo provee el test con acumulado 0) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU42RechazosTest.java | 145 | sí | Rechazos ROG (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU42Test.java | 280 | sí | CU-42 ROG (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU43AccionesTest.java | 122 | sí | Acciones CU-43 vía fachada (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU43ColaTest.java | 78 | sí | Cola de reportes (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU43RechazosTest.java | 141 | sí | Rechazos CU-43 (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU43Test.java | 284 | sí | CU-43 (leído completo; 'control diario' es el envío tardío, no un job) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU44BandejaTest.java | 168 | sí | Bandejas LFT (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU44DecisionTest.java | 245 | sí | Decisión revisor=sesión, concurrencia (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU44RechazosTest.java | 153 | sí | Rechazos CU-44 (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU44Test.java | 270 | sí | CU-44 vía versión insegura + plantillas (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU45AccionesTest.java | 125 | parcial: nombres de pruebas y aserciones clave | Acciones CU-45 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU45ColaTest.java | 75 | parcial: nombres de pruebas y aserciones clave | Cola de oficios |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU45RechazosTest.java | 150 | parcial: nombres de pruebas y aserciones clave | Rechazos CU-45 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU45Test.java | 226 | parcial: nombres de pruebas y aserciones clave | CU-45 + plantillas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU46Test.java | 218 | parcial: nombres de pruebas y aserciones clave | CU-46 licencia/sandbox |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU47RechazosTest.java | 185 | parcial: nombres de pruebas y aserciones clave | Rechazos CU-47 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU47Test.java | 286 | parcial: nombres de pruebas y aserciones clave | CU-47 + plantillas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU48RechazosTest.java | 185 | parcial: nombres de pruebas y aserciones clave | Rechazos CU-48 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU48Test.java | 273 | parcial: nombres de pruebas y aserciones clave | CU-48 + plantillas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU49RechazosTest.java | 144 | parcial: nombres de pruebas y aserciones clave | Rechazos CU-49 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU49Test.java | 268 | parcial: nombres de pruebas y aserciones clave | CU-49 + plantillas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU52BandejaTest.java | 202 | parcial: nombres de pruebas y aserciones clave | Bandeja/ficha reclamos contra PG |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU52EfectoDelReclamoTest.java | 183 | parcial: nombres de pruebas y aserciones clave | Efecto del reclamo integración |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU52RechazosTest.java | 186 | parcial: nombres de pruebas y aserciones clave | Rechazos CU-52 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU52RespuestaTest.java | 157 | parcial: nombres de pruebas y aserciones clave | Reintento de respuesta CU-52 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU52Test.java | 298 | parcial: nombres de pruebas y aserciones clave | CU-52 + plantillas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU53RechazosTest.java | 116 | parcial: nombres de pruebas y aserciones clave | Rechazos CU-53 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU53Test.java | 275 | parcial: nombres de pruebas y aserciones clave | CU-53 + plantillas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU54ColaTest.java | 74 | parcial: nombres de pruebas y aserciones clave | Cola de eventos de riesgo |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU54RechazosTest.java | 131 | parcial: nombres de pruebas y aserciones clave | Rechazos CU-54 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU54Test.java | 182 | parcial: nombres de pruebas y aserciones clave | CU-54 registro y escalado |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU55ColaTest.java | 69 | parcial: nombres de pruebas y aserciones clave | Cola de incidentes |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU55RechazosTest.java | 95 | parcial: nombres de pruebas y aserciones clave | Rechazos CU-55 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU55Test.java | 216 | parcial: nombres de pruebas y aserciones clave | CU-55 incidente y escalado |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU56AccionesTest.java | 63 | parcial: nombres de pruebas y aserciones clave | Acciones CU-56 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU56ColaTest.java | 66 | parcial: nombres de pruebas y aserciones clave | Cola de pruebas BCP |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU56RechazosTest.java | 96 | parcial: nombres de pruebas y aserciones clave | Rechazos CU-56 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU56Test.java | 239 | parcial: nombres de pruebas y aserciones clave | CU-56 + plantillas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU94ColaTest.java | 45 | parcial: nombres de pruebas y aserciones clave | Cola de actas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU94CuadreTest.java | 132 | parcial: nombres de pruebas y aserciones clave | Plantillas cuadre CU-94 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU94RechazosTest.java | 209 | parcial: nombres de pruebas y aserciones clave | Rechazos CU-94 |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/CU94Test.java | 262 | parcial: nombres de pruebas y aserciones clave | CU-94 comité |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/EscenarioDeAcciones.java | 32 | sí | Base de pruebas de acciones |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/EscenarioDeColas.java | 90 | sí | Base de pruebas de colas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/EscenarioDeProducto.java | 48 | sí | Fixture de evaluación en borrador |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/FixturaDeCumplimiento.java | 174 | sí | Inserta filas de prueba sintéticas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/FixturaDeGobierno.java | 212 | sí | Filas de gobierno/LFT sintéticas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/FixturaDeIncidentes.java | 114 | sí | Filas de incidentes |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/FixturaDeRiesgos.java | 66 | sí | Filas de riesgo operativo |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/FixturaDeUif.java | 264 | sí | Filas UIF/umbral sintéticas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/PoliticaDeCarril.java | 52 | sí | Política de armado en pruebas |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/dominio/EfectoDelReclamoTest.java | 145 | parcial: nombres de pruebas y aserciones clave | Regla pura EfectoDelReclamo en 3 niveles |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/web/AccionesDeCumplimientoWebTest.java | 213 | parcial: nombres | Web acciones CU-43/45/56 con dobles (leído por nombres) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/web/AmbitoDeLaPersonaWebTest.java | 196 | sí | Ámbito titular PEP/aceptación (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/web/AutorizacionNegativaWebTest.java | 166 | sí | Matriz 401/403 por rol (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/web/CumplimientoControllerWebTest.java | 287 | parcial: nombres de pruebas y aserciones clave | Web CU-02/03/05/54/55 con dobles |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/web/LftAccionesWebTest.java | 199 | parcial: nombres | Web abrir/decidir caso (leído por nombres; no cubre que quien abre no decida) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/web/LftBandejaWebTest.java | 194 | parcial: nombres | Web bandejas LFT (leído por nombres) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/web/LicenciaControllerWebTest.java | 120 | parcial: nombres de pruebas y aserciones clave | Web licencia |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/web/ReclamosBandejaWebTest.java | 237 | parcial: nombres de pruebas y aserciones clave | Web bandeja reclamos |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/web/ReclamosControllerWebTest.java | 222 | parcial: nombres de pruebas y aserciones clave | Web efecto: 403 a ORGANIZADOR y tokens de servicio |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/web/SeguridadWebTest.java | 79 | sí | Sábana de seguridad heredada (leído completo) |
| servicios/cumplimiento/src/test/java/bo/aportaya/cumplimiento/web/UifControllerWebTest.java | 221 | parcial: nombres | Web colas UIF (leído por nombres) |

### esquema, contratos y retiros


| ruta | líneas | leído completo | qué hace |
|---|---|---|---|
| `sql/10_tablas/01_identidad_usuarios/*.sql` (27 archivos) | ~720 | sí (DDL completo; comentarios de columna leídos en las tablas clave) | Tablas del esquema `identidad` |
| `sql/10_tablas/12_cumplimiento_asfi/*.sql` (47 archivos) | ~1460 | sí (DDL completo; comentarios clave) | Tablas de `cumplimiento` y `catalogo` (umbral UIF, licencia) |
| `sql/20_claves/01_identidad_usuarios.sql` | 180 | sí | FK de identidad |
| `sql/20_claves/12_cumplimiento_asfi.sql` | 520 | sí (todas las REFERENCES) | FK de cumplimiento (cruzan a identidad, nucleo_financiero, auditoria, tarifas) |
| `sql/30_indices/01_identidad_usuarios.sql` | 125 | sí | Índices identidad |
| `sql/30_indices/12_cumplimiento_asfi.sql` | 407 | no, por grep | Índices cumplimiento |
| `sql/35_append_only/append_only.sql` | 244 | sólo líneas de mis tablas | Triggers append-only |
| `sql/00_base/03_permisos.sql` | 284 | grants/revokes de mis esquemas + bloque search_path | GRANT/REVOKE por rol |
| `sql/00_base/02_esquemas.sql` | 595 | sólo search_path por rol | |
| `sql/15_infra/borradores.sql` | 51 | sí | `identidad.borrador_onboarding` (fuera del .puml) |
| `sql/15_infra/nulabilidad.sql` | 828 | sólo mis tablas | DROP NOT NULL |
| `sql/40_reglas/restricciones.sql` | ~3200 | rangos 255-280, 430-560, 870-1300, 1330-1720 | R-BIL retiro, R-UIF, R-CON, R-SEG, RLS, segregación |
| `sql/50_verificacion/{verificaciones,prueba_humo}.sql` | 145 / 824 | verificaciones completo por encabezados; humo por grep + función `debe_fallar` | Controles y prueba de humo |
| `sql/60_semillas/06,07,08,10,11,13,19(parcial),20(encabezado)` | ~530 | sí salvo 19/20 parciales | Catálogos mínimos de mi alcance |
| `sql/61_dev/15,14,17,18,02,05` | — | por grep (enmascarado) | Seeds dev de identidad |
| `sql/62_volumen/*` | — | por grep | Volumen sintético (guarda dev) |
| `seeders/README.md`, `seeders/minimos/{06,07,08,10,11,13,20}.json` (metadatos) | — | metadatos | Procedencia de seeds |
| `contratos/pactos/identidad-grupos-pertenencia.json` | 46 | sí | Pacto identidad→grupos |
| `contratos/pactos/*` (resto) | — | grep identidad/cumplimiento | Sin otros pactos de identidad/cumplimiento |
| `clientes/dart/{identidad,cumplimiento}` | 65/67 | sí (barrel) + comparación automática con OpenAPI | Clientes Dart generados |
| `servicios/{identidad,cumplimiento}/src/main/resources/openapi/*.yaml` | 1631 / 1761 | inventario de operaciones + rangos puntuales | Contratos |
| `packages/simulado/ejemplos/{identidad,cumplimiento}` | 38 + 28 json | inventario de escenarios + 1 completo | Mock por contrato |
| `postman/coleccion|humo/{identidad,cumplimiento}` + entornos | — | inventario de requests/headers | Colecciones |
| `docs/Arquitectura/ADR-049 Doble aprobación de retiros.md` | 187 | sí | Decisión doble aprobación |
| `docs/Arquitectura/ADR-032`, `ADR-017` | — | ADR-032 completo; ADR-017 por grep | Fuente de verdad del esquema; FK cruzadas |
| `nucleo-financiero/.../AprobacionDeRetiroRepositorio.java` | 44 | sí | UPDATE condicionado de aprobación |
| `.../BandejaDeRetiros.java`, `BandejaDeRetirosRepositorio.java` | 75 / 74 | sí | Bandeja EN_REVISION |
| `.../ResolucionDeRetiro.java` | 291 | sí | confirmar/rechazar/aprobar |
| `.../ReglasDeRetiro.java`, `CondicionesDeRetiro.java`, `EstadoDeRetiro.java`, `CodigosDeRetiro.java` | 58/90/44/20 | sí | Reglas puras |
| `.../CU11RetirarSaldo.java`, `SolicitudDeRetiro.java` | 261 / 176 | sí | Orquestación CU-11 |
| `.../SegundoFactor.java`, `SegundoFactorLocal.java`, `SegundoFactorStepUp.java` | 28/47/124 | sí | MFA de retiro |
| `.../OrdenRetiroRepositorio.java` | 299 | sí | Persistencia orden_retiro |
| `.../web/BilleteraController.java` | 296 | sólo endpoints de retiro/aprobación/retención/recarga | |
| `.../web/FondeoDeLaBilletera.java` | — | rangos 60-90, 140-248 | Cálculo del flag de doble aprobación |
| `.../aplicacion/CU13RetenerSaldo.java`, `RetencionRepositorio.java`, `VigenciaDeRetencion.java` | — | rangos de liberar/vencer | Cierre de retención (cotejo) |
| `sql/10_tablas/10_billetera_custodia/orden_retiro.sql` + FK/índices | — | sí | Tabla de la orden (no existe tabla de aprobación separada) |
| `BilleteraAprobacionWebTest.java`, `BilleteraBandejaWebTest.java` | 232 / 229 | casos (DisplayName/expect) | Pruebas web |
| `docs/catalogo-de-flujos/recarga-y-retiro.yaml`, `matriz.md` | 155 / — | yaml completo; matriz por grep | Catálogo de flujos |
| `docs/CasosDeUso/CU-11` | 312 | sí | Retirar saldo |
| `docs/CasosDeUso/CU-01,02,03,06,08,18,41,43,44` | — | grep de puntos de aprobación/umbral | |

### app móvil

| ruta | líneas | leído completo (sí/no + motivo) | qué hace (una frase) |
|---|---|---|---|
| apps/movil/lib/pantallas/identidad/acciones_de_verificacion.dart | 92 | sí | Botón principal según estado KYC/sesión (activar, salir, reintentar, entrar) |
| apps/movil/lib/pantallas/identidad/activar_cuenta.dart | 20 | sí | Renueva token tras KYC aprobado y navega a billetera |
| apps/movil/lib/pantallas/identidad/avatar_con_aro.dart | 69 | sí | Avatar del perfil con insignia de cámara (excluido de semántica) |
| apps/movil/lib/pantallas/identidad/avisos_de_sesion.dart | 56 | sí | Alertas del login (cuenta creada, sesión caducada 8 h, error) y nota de custodia |
| apps/movil/lib/pantallas/identidad/banda_de_ingreso.dart | 78 | sí | Banda de marca del login con saludo |
| apps/movil/lib/pantallas/identidad/banda_de_marca.dart | 69 | sí | Banda de marca de la portada |
| apps/movil/lib/pantallas/identidad/barra_de_lectura.dart | 48 | sí | Barra de progreso de lectura del contrato |
| apps/movil/lib/pantallas/identidad/boton_consultar_estado.dart | 75 | sí | «Consultar estado»: pregunta si ya aprobaron el KYC y renueva acceso |
| apps/movil/lib/pantallas/identidad/campos_de_sesion.dart | 97 | sí | Campos celular/correo + contraseña del ingreso |
| apps/movil/lib/pantallas/identidad/capa_de_bloqueo.dart | 168 | sí | Candado local (overlay) con desbloqueo biométrico |
| apps/movil/lib/pantallas/identidad/capa_de_bloqueo_animacion.dart | 47 | sí | Animación del candado con movimiento reducido |
| apps/movil/lib/pantallas/identidad/capa_de_bloqueo_biometria.dart | 87 | sí | Pedido biométrico nativo por episodio |
| apps/movil/lib/pantallas/identidad/capa_de_bloqueo_vista.dart | 157 | sí | Vista del candado (textos fijos, fallo biométrico crudo) |
| apps/movil/lib/pantallas/identidad/continuar_alta.dart | 67 | sí | Enlace y aviso para retomar alta a medias |
| apps/movil/lib/pantallas/identidad/controlador_prueba_de_vida.dart | 197 | sí | Máquina de la prueba de vida (cámara, juez, captura) |
| apps/movil/lib/pantallas/identidad/cuerpo_del_contrato.dart | 170 | sí | Pinta el contrato local por bloques |
| apps/movil/lib/pantallas/identidad/dominio/bloques_del_contrato.dart | 120 | sí | Parser del texto del contrato en bloques |
| apps/movil/lib/pantallas/identidad/dominio/borrador_alta.dart | 116 | sí | Mapeo estado del alta <-> BorradorOnboarding (monto como double) |
| apps/movil/lib/pantallas/identidad/dominio/calidad_de_captura.dart | 64 | sí | Chequeo local de tamaño/proporción de la foto |
| apps/movil/lib/pantallas/identidad/dominio/camara_identidad.dart | 13 | sí | Provider del puerto Cámara |
| apps/movil/lib/pantallas/identidad/dominio/canales_de_verificacion.dart | 4 | sí | Canal único CORREO |
| apps/movil/lib/pantallas/identidad/dominio/captura_desde_archivo.dart | 61 | sí | Lee foto, evalúa calidad, arma Captura con sha corto |
| apps/movil/lib/pantallas/identidad/dominio/capturas_del_expediente.dart | 135 | sí | Modelo inmutable de las 5 capturas y borrado local |
| apps/movil/lib/pantallas/identidad/dominio/cara_del_carril.dart | 13 | sí | Enum de caras con valor API |
| apps/movil/lib/pantallas/identidad/dominio/carnet_de_prueba.dart | 113 | sí | Carnet sintético solo !kReleaseMode (no usado en lib) |
| apps/movil/lib/pantallas/identidad/dominio/ciclo_de_pose.dart | 143 | sí | Reloj de pose: respiro, cuenta atrás, tomar ahora |
| apps/movil/lib/pantallas/identidad/dominio/comportamiento_de_pantalla_del_alta.dart | 74 | sí | Mixin: FLAG_SECURE por fase y scroll al error |
| apps/movil/lib/pantallas/identidad/dominio/consultar_estado_de_cuenta.dart | 50 | sí | Consulta estado de cuenta y renueva acceso si verificada |
| apps/movil/lib/pantallas/identidad/dominio/contratos_vigentes.dart | 68 | sí | GET /cumplimiento/contratos/vigentes y tipos del alta |
| apps/movil/lib/pantallas/identidad/dominio/cu01_registrar.dart | 139 | sí | POST /usuarios por Dio crudo con Idempotency-Key |
| apps/movil/lib/pantallas/identidad/dominio/cu02_actualizar_fecha_expiracion.dart | 36 | sí | PUT fecha de vencimiento por Dio crudo, sin manejo de error |
| apps/movil/lib/pantallas/identidad/dominio/cu02_consultar_verificacion.dart | 32 | sí | GET estado de verificación con Token-De-Alta |
| apps/movil/lib/pantallas/identidad/dominio/cu02_subir_foto.dart | 110 | sí | POST multipart de foto con Idempotency-Key y Token-De-Alta |
| apps/movil/lib/pantallas/identidad/dominio/cu04_autenticar.dart | 115 | sí | POST /sesiones con MFA decidido por servidor; guarda tokens |
| apps/movil/lib/pantallas/identidad/dominio/datos_del_alta.dart | 104 | sí | DatosPersonales y DatosLeidosDelDocumento |
| apps/movil/lib/pantallas/identidad/dominio/envio_del_alta.dart | 98 | sí | Arma y envía el alta, mapea error por campo |
| apps/movil/lib/pantallas/identidad/dominio/estado_alta.dart | 2 | sí | Barrel del estado del alta |
| apps/movil/lib/pantallas/identidad/dominio/estado_contrato.dart | 101 | sí | Estado de los tres consentimientos y ids vigentes |
| apps/movil/lib/pantallas/identidad/dominio/estado_cuenta.dart | 156 | sí | Correo de perfil, PEP local, contraseña/baja que lanzan UnimplementedError |
| apps/movil/lib/pantallas/identidad/dominio/estado_de_verificacion.dart | 114 | sí | Sondeo 2 s × 20 del estado KYC |
| apps/movil/lib/pantallas/identidad/dominio/estado_sesion.dart | 183 | sí | Notifier del ingreso (credenciales, MFA, confianza local) |
| apps/movil/lib/pantallas/identidad/dominio/foto_de_perfil.dart | 81 | sí | GET/PUT foto de perfil y nombre |
| apps/movil/lib/pantallas/identidad/dominio/juez_de_pose.dart | 111 | sí | Umbrales de pose (no medidos) |
| apps/movil/lib/pantallas/identidad/dominio/kyc_del_titular.dart | 52 | sí | Providers KYC de sesión, faltantes y etiquetas («En revisión humana») |
| apps/movil/lib/pantallas/identidad/dominio/lectura_de_rostro.dart | 20 | sí | DTO normalizado del detector |
| apps/movil/lib/pantallas/identidad/dominio/modelo_alta.dart | 193 | sí | EstadoAlta vigente (monto double) |
| apps/movil/lib/pantallas/identidad/dominio/modelo_del_alta.dart | 126 | sí | Copia muerta y divergente de EstadoAlta/PasoAlta |
| apps/movil/lib/pantallas/identidad/dominio/notifier_alta.dart | 190 | sí | AltaNotifier: pasos, código de correo, borrador, envío |
| apps/movil/lib/pantallas/identidad/dominio/pautas_de_fotos.dart | 98 | sí | Catálogo de consejos de fotos |
| apps/movil/lib/pantallas/identidad/dominio/primer_bloqueo.dart | 8 | sí | Primer requisito pendiente |
| apps/movil/lib/pantallas/identidad/dominio/seguimiento_del_alta.dart | 84 | sí | usuarioId+tokenDeAlta+capturas tras el alta (memoria) |
| apps/movil/lib/pantallas/identidad/dominio/subida_del_expediente.dart | 196 | sí | Subida en lote de 5 fotos con estados y cancelación |
| apps/movil/lib/pantallas/identidad/dominio/validaciones.dart | 179 | sí | Validaciones de ayuda (edad, CI, contraseña, correo) |
| apps/movil/lib/pantallas/identidad/dominio/verificacion_correo.dart | 200 | sí | Código de correo, rescate en almacén seguro, borrador (catch mudo) |
| apps/movil/lib/pantallas/identidad/dominio/verificacion_correo_guardada.dart | 78 | sí | Serialización de la verificación guardada |
| apps/movil/lib/pantallas/identidad/dominio/verificacion_diferida.dart | 87 | sí | EstadoDeCuenta desde /usuarios/yo/perfil e iniciar verificación |
| apps/movil/lib/pantallas/identidad/fila_de_accion_de_perfil.dart | 117 | sí | Fila de ajustes con semántica de botón |
| apps/movil/lib/pantallas/identidad/fila_de_subida.dart | 108 | sí | Fila de estado de subida por cara |
| apps/movil/lib/pantallas/identidad/foto_de_perfil_editable.dart | 172 | sí | Cabecera del perfil y cambio de foto |
| apps/movil/lib/pantallas/identidad/foto_pendiente_kyc.dart | 96 | sí | Tarjeta de perfiles faltantes del KYC |
| apps/movil/lib/pantallas/identidad/garantias_de_portada.dart | 86 | sí | Tres garantías de la portada |
| apps/movil/lib/pantallas/identidad/ilustraciones/accesorios_de_persona.dart | 64 | sí | Dibujo: lentes y gorra |
| apps/movil/lib/pantallas/identidad/ilustraciones/carnet_de_ejemplo.dart | 161 | sí | Dibujo del carnet genérico |
| apps/movil/lib/pantallas/identidad/ilustraciones/detalles_del_carnet.dart | 156 | sí | Dibujo de detalles del carnet |
| apps/movil/lib/pantallas/identidad/ilustraciones/escena_de_rostro.dart | 101 | sí | Dibujo de escenas de rostro |
| apps/movil/lib/pantallas/identidad/ilustraciones/ilustracion_de_pauta.dart | 154 | sí | CustomPainter de pautas (excluido de semántica) |
| apps/movil/lib/pantallas/identidad/ilustraciones/persona_de_ejemplo.dart | 133 | sí | Dibujo de la persona de ejemplo |
| apps/movil/lib/pantallas/identidad/ilustraciones/rasgos_de_persona.dart | 95 | sí | Dibujo de rasgos y pelo |
| apps/movil/lib/pantallas/identidad/lamina_del_tour.dart | 101 | sí | Lámina del tour con paralaje (respeta movimiento reducido) |
| apps/movil/lib/pantallas/identidad/laminas_del_tour.dart | 51 | sí | Las 4 láminas; la 4.ª promete cobertura del fondo de garantía |
| apps/movil/lib/pantallas/identidad/lo_que_necesitas.dart | 75 | sí | Qué pide el alta |
| apps/movil/lib/pantallas/identidad/mostrar_celebracion_de_alta.dart | 14 | sí | Diálogo de celebración |
| apps/movil/lib/pantallas/identidad/pagina_con_transicion_de_marca.dart | 12 | sí | Transición de página de marca |
| apps/movil/lib/pantallas/identidad/panel_de_aceptacion.dart | 68 | sí | Casillas de consentimientos y botón aceptar |
| apps/movil/lib/pantallas/identidad/pantalla_baja.dart | 76 | sí | Baja: «Confirmar baja» solo cierra la hoja |
| apps/movil/lib/pantallas/identidad/pantalla_bienvenida.dart | 76 | sí | Bienvenida post-alta con moneda 'BOB' literal |
| apps/movil/lib/pantallas/identidad/pantalla_camara.dart | 200 | sí | Cámara en vivo del carnet (sin try en tomar foto) |
| apps/movil/lib/pantallas/identidad/pantalla_completar_kyc.dart | 200 | sí | Completar perfiles y fecha de vencimiento desde perfil |
| apps/movil/lib/pantallas/identidad/pantalla_completar_kyc_de_sesion.dart | 37 | sí | Resuelve usuarioId desde token y abre completar KYC |
| apps/movil/lib/pantallas/identidad/pantalla_contrasena.dart | 111 | sí | Cambio de contraseña que no llama al servidor |
| apps/movil/lib/pantallas/identidad/pantalla_contrato.dart | 151 | sí | Contrato local + ids vigentes del servidor |
| apps/movil/lib/pantallas/identidad/pantalla_dispositivos.dart | 66 | sí | Casilla «confiar en dispositivo» no enviada |
| apps/movil/lib/pantallas/identidad/pantalla_estado_de_verificacion.dart | 150 | sí | Estado KYC (pendiente/revisión humana/aprobada/rechazada) |
| apps/movil/lib/pantallas/identidad/pantalla_identidad_requerida.dart | 159 | sí | Compuerta antes de dinero/pasanaku según estado KYC |
| apps/movil/lib/pantallas/identidad/pantalla_mfa.dart | 137 | sí | Ingreso del segundo factor |
| apps/movil/lib/pantallas/identidad/pantalla_pautas_de_fotos.dart | 185 | sí | Pautas con ejemplos correcto/incorrecto |
| apps/movil/lib/pantallas/identidad/pantalla_perfil.dart | 153 | sí | Perfil: KYC, correo, seguridad, cerrar sesión, baja |
| apps/movil/lib/pantallas/identidad/pantalla_portada.dart | 149 | sí | Portada de bienvenida |
| apps/movil/lib/pantallas/identidad/pantalla_prueba_de_vida.dart | 196 | sí | Pantalla de prueba de vida |
| apps/movil/lib/pantallas/identidad/pantalla_registro.dart | 200 | sí | Asistente del alta y cierre (celebración, subida) |
| apps/movil/lib/pantallas/identidad/pantalla_sesion.dart | 189 | sí | Pantalla de ingreso |
| apps/movil/lib/pantallas/identidad/pantalla_subida_del_expediente.dart | 186 | sí | Subida del expediente; borra capturas al salir |
| apps/movil/lib/pantallas/identidad/pantalla_tour.dart | 194 | sí | Tour de 4 láminas |
| apps/movil/lib/pantallas/identidad/pantalla_verificacion_profunda.dart | 123 | sí | Declaración PEP que no se envía |
| apps/movil/lib/pantallas/identidad/pantalla_verificar_identidad.dart | 174 | sí | Verificación diferida: envía solo el documento |
| apps/movil/lib/pantallas/identidad/paso_contrato_de_alta.dart | 42 | sí | Paso que lleva al contrato |
| apps/movil/lib/pantallas/identidad/pasos_alta/aviso_de_permiso_camara.dart | 97 | sí | Aviso de permiso de cámara |
| apps/movil/lib/pantallas/identidad/pasos_alta/barra_superior_de_vida.dart | 61 | sí | Barra de la prueba de vida |
| apps/movil/lib/pantallas/identidad/pasos_alta/bloque_clave_de_cuenta.dart | 86 | sí | Bloque de contraseña del alta |
| apps/movil/lib/pantallas/identidad/pasos_alta/bloque_contacto_de_cuenta.dart | 75 | sí | Celular +591 y correo |
| apps/movil/lib/pantallas/identidad/pasos_alta/bloque_de_formulario.dart | 51 | sí | Encabezado de bloque (ícono 14 literal) |
| apps/movil/lib/pantallas/identidad/pasos_alta/cabecera_de_camara.dart | 51 | sí | Cabecera de cámara; IconButton sin tooltip |
| apps/movil/lib/pantallas/identidad/pasos_alta/cabecera_de_fase.dart | 54 | sí | Progreso de fase |
| apps/movil/lib/pantallas/identidad/pasos_alta/campo_de_actividad.dart | 60 | sí | Selector CIIU + «¿Cuál?» |
| apps/movil/lib/pantallas/identidad/pasos_alta/campo_de_detalle.dart | 39 | sí | Campo de detalle de «Otro» |
| apps/movil/lib/pantallas/identidad/pasos_alta/campos_de_nombre.dart | 107 | sí | Cuatro campos de nombre |
| apps/movil/lib/pantallas/identidad/pasos_alta/campos_del_carnet.dart | 38 | sí | Layout número/expedición |
| apps/movil/lib/pantallas/identidad/pasos_alta/canal_de_verificacion.dart | 59 | sí | Muestra canal único (textos fijos) |
| apps/movil/lib/pantallas/identidad/pasos_alta/catalogo_del_perfil.dart | 83 | sí | Catálogos origen de fondos y actividad |
| apps/movil/lib/pantallas/identidad/pasos_alta/consejos_para_la_foto.dart | 81 | sí | Acordeón de consejos |
| apps/movil/lib/pantallas/identidad/pasos_alta/cuenta_atras_grande.dart | 34 | sí | Cuenta atrás con movimiento reducido |
| apps/movil/lib/pantallas/identidad/pasos_alta/departamentos.dart | 13 | sí | Nueve departamentos |
| apps/movil/lib/pantallas/identidad/pasos_alta/elegir_captura_de_galeria.dart | 22 | sí | Elegir foto de galería y evaluarla |
| apps/movil/lib/pantallas/identidad/pasos_alta/encabezado_y_consejos_de_camara.dart | 36 | sí | Cabecera + consejos |
| apps/movil/lib/pantallas/identidad/pasos_alta/errores_paso_datos.dart | 44 | sí | Mapa de errores del paso datos (no usado por PasoDatos) |
| apps/movil/lib/pantallas/identidad/pasos_alta/formateadores_del_alta.dart | 6 | sí | Formateador de CI |
| apps/movil/lib/pantallas/identidad/pasos_alta/guia_de_encuadre.dart | 193 | sí | Marco de encuadre carnet/óvalo |
| apps/movil/lib/pantallas/identidad/pasos_alta/hoja_antes_de_escanear.dart | 62 | sí | Hoja «Antes de escanear» (textos fijos) |
| apps/movil/lib/pantallas/identidad/pasos_alta/miniaturas_de_vida.dart | 124 | sí | Miniaturas de las 3 poses |
| apps/movil/lib/pantallas/identidad/pasos_alta/opciones_de_captura_rechazada.dart | 58 | sí | Repetir / usar igual |
| apps/movil/lib/pantallas/identidad/pasos_alta/paso_capturas.dart | 193 | sí | Carrusel de 5 capturas |
| apps/movil/lib/pantallas/identidad/pasos_alta/paso_correo.dart | 106 | sí | Confirmar código de correo |
| apps/movil/lib/pantallas/identidad/pasos_alta/paso_cotejo.dart | 69 | sí | Cotejo declarado vs «leído» (mismo dato) |
| apps/movil/lib/pantallas/identidad/pasos_alta/paso_cuenta.dart | 197 | sí | Contacto + contraseña; reglas de clave reimplementadas |
| apps/movil/lib/pantallas/identidad/pasos_alta/paso_datos.dart | 162 | sí | Nombres y fecha de nacimiento |
| apps/movil/lib/pantallas/identidad/pasos_alta/paso_documento.dart | 191 | sí | Capturas + datos del carnet (solo CI) |
| apps/movil/lib/pantallas/identidad/pasos_alta/paso_perfil_transaccional.dart | 195 | sí | Origen, actividad y monto (double, 'Bs') |
| apps/movil/lib/pantallas/identidad/pasos_alta/pie_de_capturas.dart | 58 | sí | Botón enviar y motivo de bloqueo |
| apps/movil/lib/pantallas/identidad/pasos_alta/pie_de_prueba_de_vida.dart | 124 | sí | Pie con «Tomar ahora» siempre disponible |
| apps/movil/lib/pantallas/identidad/pasos_alta/pie_del_paso_datos.dart | 35 | sí | Pie del paso datos |
| apps/movil/lib/pantallas/identidad/pasos_alta/riel_de_capturas.dart | 137 | sí | Riel de 5 caras accesible |
| apps/movil/lib/pantallas/identidad/pasos_alta/sacar_captura.dart | 101 | sí | Escáner/cámara/prueba de vida según cara |
| apps/movil/lib/pantallas/identidad/pasos_alta/saltar_verificacion.dart | 46 | sí | «Saltar por ahora» |
| apps/movil/lib/pantallas/identidad/pasos_alta/silueta_de_cara.dart | 143 | sí | Silueta de la prueba de vida |
| apps/movil/lib/pantallas/identidad/pasos_alta/tarjeta_de_captura.dart | 166 | sí | Tarjeta de captura del carrusel |
| apps/movil/lib/pantallas/identidad/pasos_alta/tarjeta_de_confianza.dart | 36 | sí | Mensaje de privacidad (ícono 18 literal) |
| apps/movil/lib/pantallas/identidad/pasos_alta/transicion_de_paso_alta.dart | 36 | sí | Transición entre pasos con movimiento reducido |
| apps/movil/lib/pantallas/identidad/pasos_alta/trazos_de_cara.dart | 25 | sí | Geometría del óvalo |
| apps/movil/lib/pantallas/identidad/pasos_alta/visor_ampliado_de_captura.dart | 86 | sí | Visor ampliado con tooltip |
| apps/movil/lib/pantallas/identidad/rutas.dart | 73 | sí | Rutas dentro del shell |
| apps/movil/lib/pantallas/identidad/rutas_de_entrada.dart | 139 | sí | Rutas de entrada con extra!/query! sin validar |
| apps/movil/lib/pantallas/identidad/sacudida_de_credenciales.dart | 49 | sí | Sacudida con movimiento reducido |
| apps/movil/lib/pantallas/identidad/seccion_verificacion_kyc.dart | 136 | sí | Tarjeta KYC del perfil (4 estados) |
| apps/movil/lib/pantallas/identidad/sesion_pie.dart | 57 | sí | «Olvidé contraseña» sin flujo (snackbar) |
| apps/movil/lib/pantallas/identidad/texto_completar_kyc.dart | 34 | sí | Textos de completar KYC («una persona revisa») |
| apps/movil/lib/pantallas/identidad/texto_del_contrato.dart | 261 | sí | Contrato y tarifario hardcodeados en el cliente |
| apps/movil/lib/pantallas/identidad/textos.dart | 197 | sí | Textos de identidad (tour 4: cobertura del fondo) |
| apps/movil/lib/pantallas/identidad/textos_de_captura.dart | 99 | sí | Textos de captura |
| apps/movil/lib/pantallas/identidad/textos_de_portada.dart | 17 | sí | Textos de portada |
| apps/movil/lib/pantallas/identidad/textos_de_subida.dart | 36 | sí | Textos de subida |
| apps/movil/lib/pantallas/identidad/textos_de_tramites.dart | 37 | sí | Textos de trámites (contraseña, baja, PEP) |
| apps/movil/lib/pantallas/identidad/textos_de_verificacion.dart | 101 | sí | Textos de estados KYC y motivos de rechazo |
| apps/movil/lib/pantallas/identidad/textos_del_alta.dart | 81 | sí | Textos del alta |
| apps/movil/lib/pantallas/identidad/textos_del_perfil.dart | 49 | sí | Textos del perfil |
| apps/movil/lib/proveedores/sesion.dart | 196 | sí | Tokens acceso/refresco en almacén seguro, tope 8 h, reclamos del token |
| apps/movil/lib/infraestructura/android/almacen_seguro_android.dart | 22 | sí | flutter_secure_storage con AndroidOptions() por defecto |
| apps/movil/lib/infraestructura/ios/almacen_seguro_ios.dart | 28 | sí | Keychain `unlocked_this_device` |
| apps/movil/lib/dominio/renovar_acceso.dart | 18 | sí | Refresco «activar cuenta» con Dio nuevo sin timeouts |
| apps/movil/lib/dominio/cliente.dart | ~100 | no (solo 66-100) | Dio global (connect 8 s / receive 12 s) e interceptores |
| apps/movil/lib/dominio/cierre_de_sesion.dart | ~72 | no (grep) | Logout local + revocación `DELETE /sesion` |
| apps/movil/android/app/src/main/AndroidManifest.xml | — | no (grep allowBackup) | Sin `allowBackup`/`dataExtractionRules` |
| servicios/identidad/.../openapi/identidad.yaml | 1631 | no (paths + 8 schemas) | Contrato de identidad usado para contrastar |
| apps/movil/test/** (112 archivos importan identidad, 14 999 líneas) | — | no (grep dirigido + `pantalla_baja_test.dart` y `estado_alta_test.dart:135-175` leídos) | Tests de identidad |

### backoffice y web


Archivos de implementación: leídos completos (sí). Specs: leídos sus títulos (`describe`/`it`) en todos y el cuerpo por
muestreo (ver Límites).

| ruta (relativa a `apps/backoffice/src/app/rutas/` salvo indicación) | líneas | leído completo | qué hace |
|---|---|---|---|
| cumplimiento/cumplimiento.routes.ts | 122 | sí | rutas de la sección; `canMatch` solo en bandejas, no en detalles |
| cumplimiento/compartido/acceso.ts | 23 | sí | `tieneAlguno`/`requiereAlguno`: rol o permiso, espejo de GuardiaDePermiso |
| cumplimiento/compartido/bandeja-por-cursor.ts | 135 | sí | estado de bandeja paginada por cursor (4 estados + denegado) |
| cumplimiento/compartido/campos.ts | 42 | sí | `ap-fecha-y-hora` + reexportes |
| cumplimiento/compartido/errores-de-cumplimiento.ts | 45 | sí | catálogo local de mensajes por código AP-CU4x |
| cumplimiento/compartido/indisponible.ts | 46 | sí | pantalla de "consulta no disponible" |
| cumplimiento/compartido/listado-de-bandeja.ts | 123 | sí | listado por cursor con export gated por permiso |
| cumplimiento/compartido/marco.ts | 39 | sí | marco visual de bandejas |
| cumplimiento/compartido/navegacion.ts | 42 | sí | `ENTRADAS_DE_CUMPLIMIENTO` (usada) + componente de navegación (no usado) |
| cumplimiento/compartido/pruebas.apoyo.ts | 73 | sí | montaje de tests con HttpTestingController |
| cumplimiento/dominio/validacion.ts | 20 | sí | UUID, período, fecha La Paz |
| cumplimiento/dominio/paginas.ts | 57 | sí | `pedirPagina` genérico por cursor |
| cumplimiento/dominio/colas-de-lectura.ts | 33 | sí | cargadores CU-54/55/94 |
| cumplimiento/dominio/cu02-expedientes.ts | 167 | sí | HTTP de identidad: cola KYC, fotos, decisión |
| cumplimiento/dominio/cu02-presentacion.ts | 126 | sí | presentación de expediente KYC |
| cumplimiento/dominio/cu43-reportes.ts | 102 | sí | UIF reportes: generar / aprobar-y-enviar |
| cumplimiento/dominio/cu44-bandejas.ts | 50 | sí | alertas/casos LFT: listar, abrir, decidir |
| cumplimiento/dominio/cu44-caso.ts | 51 | sí | tipo de caso y catálogo de causales escritos a mano |
| cumplimiento/dominio/cu44-validacion.ts | 56 | sí | validación de apertura/decisión de caso |
| cumplimiento/dominio/cu45-requerimientos.ts | 89 | sí | requerimientos de autoridad |
| cumplimiento/dominio/cu52-reclamo.ts | 22 | sí | tipo de reclamo (sin uso real) |
| cumplimiento/dominio/cu56-continuidad.ts | 86 | sí | pruebas de continuidad |
| cumplimiento/dominio/cu90-habilitacion.ts | 40 | sí | consulta/habilitación de organizador |
| cumplimiento/dominio/cu94-acta.ts | 46 | sí | reglas de quórum (sin contrato) |
| cumplimiento/dominio/cu97-alertas.ts | 40 | sí | alerta temprana (sin contrato) |
| cumplimiento/verificaciones/pantalla-de-expedientes.ts | 170 | sí | bandeja KYC |
| cumplimiento/verificaciones/ficha-de-expediente.ts | 112 | sí | ficha KYC con pestañas |
| cumplimiento/verificaciones/decision-de-expediente.ts | 89 | sí | botones aprobar/rechazar KYC |
| cumplimiento/verificaciones/vista-decision-del-expediente.ts | 84 | sí | pestaña decisión KYC (POST) |
| cumplimiento/verificaciones/vista-resumen-del-expediente.ts | 79 | sí | pestaña resumen |
| cumplimiento/verificaciones/vista-fotos-del-expediente.ts | 57 | sí | pestaña fotos |
| cumplimiento/verificaciones/tira-de-fotos.ts | 126 | sí | descarga fotos como blob |
| cumplimiento/verificaciones/vencimiento-del-documento.ts | 38 | sí | componente no usado |
| cumplimiento/verificaciones/vista-como-funciona.ts | 59 | sí | explicación |
| cumplimiento/verificaciones/textos-verificaciones.ts | 103 | sí | textos |
| cumplimiento/verificaciones/textos-como-funciona.ts | 67 | sí | textos (declaran aprobación humana KYC) |
| cumplimiento/alertas/bandeja-de-alertas.ts | 124 | sí | bandeja LFT + abrir caso |
| cumplimiento/alertas/abrir-caso.ts | 78 | sí | diálogo `abrirCasoLft` |
| cumplimiento/alertas/contenedor-de-alerta.ts | 21 | sí | indisponible CU-97 |
| cumplimiento/alertas/pantalla-de-alertas.ts | 45 | sí | componente no enrutado |
| cumplimiento/casos/bandeja-de-casos.ts | 118 | sí | bandeja de casos + decidir |
| cumplimiento/casos/decidir-caso.ts | 103 | sí | diálogo `decidirCasoLft` |
| cumplimiento/casos/pantalla-de-caso.ts | 142 | sí | ficha de caso con datos demo + borrador |
| cumplimiento/casos/formulario-de-caso.ts | 70 | sí | presentación causal/narrativa |
| cumplimiento/uif/bandeja-de-reportes.ts | 128 | sí | bandeja CU-43 |
| cumplimiento/uif/preparar-reporte.ts | 63 | sí | generar reporte |
| cumplimiento/uif/aprobar-y-enviar.ts | 91 | sí | aprobar + registrar envío (2.ª persona) |
| cumplimiento/uif/bandeja-de-requerimientos.ts | 119 | sí | bandeja CU-45 |
| cumplimiento/uif/registrar-requerimiento.ts | 79 | sí | alta de oficio |
| cumplimiento/uif/responder-requerimiento.ts | 60 | sí | respuesta de oficio |
| cumplimiento/colas/cola-de-lectura.ts | 79 | sí | cola genérica de lectura |
| cumplimiento/colas/pantalla-de-continuidad.ts | 68 | sí | CU-56 |
| cumplimiento/colas/registrar-prueba.ts | 79 | sí | alta prueba CU-56 |
| cumplimiento/colas/pantalla-de-eventos.ts | 54 | sí | CU-54 |
| cumplimiento/colas/pantalla-de-incidentes.ts | 51 | sí | CU-55 |
| cumplimiento/colas/pantalla-de-actas.ts | 45 | sí | CU-94 lista |
| cumplimiento/actas/contenedor-de-acta.ts | 20 | sí | indisponible |
| cumplimiento/actas/pantalla-de-acta.ts | 71 | sí | componente no enrutado; "cerrar" sin HTTP |
| cumplimiento/reclamos/contenedor-de-reclamo.ts | 21 | sí | redirige a operación |
| cumplimiento/reclamos/pantalla-de-reclamo.ts | 59 | sí | componente no enrutado; "enviar" sin HTTP |
| cumplimiento/organizadores/pantalla-de-habilitacion.ts | 96 | sí | CU-90 habilitar organizador |
| cumplimiento/textos.ts, textos-uif.ts, textos-lft.ts, textos-colas.ts | 100/88/64/63 | sí | textos |
| cumplimiento/**/*.spec.ts (36 archivos) | 2882 | títulos sí; cuerpo por muestreo | unitarias/a11y con HttpTestingController |
| sistemas/accesos/pantalla-accesos.ts | 69 | sí | revisión de accesos (persona×rol) |
| sistemas/usuarios-internos/pantalla-usuarios-internos.ts | 198 | sí | listado + menú de acciones |
| sistemas/usuarios-internos/gestor-de-usuario-interno.ts | 134 | sí | editar/suspender/desbloquear |
| sistemas/usuarios-internos/modal-asignar-roles.ts | 89 | sí | PUT roles |
| sistemas/usuarios-internos/modal-alta-usuario-interno.ts | 99 | sí | POST alta |
| sistemas/usuarios-internos/pantalla-usuario-interno-detalle.ts | 113 | sí | ficha |
| sistemas/roles-internos/pantalla-roles-internos.ts | 123 | sí | catálogo roles |
| sistemas/permisos-internos/pantalla-permisos-internos.ts | 108 | sí | catálogo permisos |
| sistemas/dominio/usuarios-internos.ts (soporte, fuera de lote) | 122 | sí | HTTP de usuarios internos |
| sistemas/dominio/accesos.ts (soporte) | 60 primeras | parcial | puerto real de accesos |
| sistemas/** specs del lote (11) | 809 | títulos sí; cuerpo por muestreo | |
| ingreso/pantalla-de-ingreso.ts | 194 | sí | login 2 pasos (CU-04) |
| ingreso/dominio/cu04-autenticar.ts | 37 | sí | POST /sesiones, huella por pestaña |
| ingreso/dominio/identificador.ts | 30 | sí | correo o teléfono |
| ingreso/panel-de-marca.ts | 88 | sí | panel decorativo |
| ingreso/*.spec.ts (2) | 144 | títulos sí | |
| operacion/retiros/pantalla-de-aprobacion-de-retiro.ts | 198 | sí | segunda firma de retiro |
| operacion/retiros/bandeja-de-retiros.ts | 103 | sí | bandeja de retiros en revisión |
| operacion/retiros/dominio/cu11-aprobacion-de-retiro.ts | 32 | sí | POST aprobación (tipos a mano) |
| operacion/retiros/dominio/cu11-retiros-en-revision.ts | 33 | sí | GET bandeja |
| operacion/retiros/textos.ts | 51 | sí | textos |
| operacion/retiros/*.spec.ts (3) | 365 | títulos sí | |
| `apps/web/src/app/paginas/invitacion/invitacion.ts` | 111 | sí | puerta del enlace de invitación |
| `apps/web/src/app/paginas/invitacion/token-de-invitacion.ts` | 61 | sí | lee/retira secreto; lo guarda en memoria |
| `apps/web/src/app/app.routes.server.ts` | 40 | sí | modo de render por ruta |
| `apps/web` resto (grep identidad/registro/kyc/invitación/uif) | — | grep | sin otras pantallas de identidad/KYC; no hay registro web |

Contratos contrastados: `servicios/identidad/.../identidad.yaml` (146-177, 271-382, 830-990, 1303-1360, 1522-1595),
`servicios/cumplimiento/.../cumplimiento.yaml` (570-1000, 1341-1720), `servicios/organizador/.../organizador.yaml`
(266-332, 689-707), `servicios/nucleo-financiero/.../nucleo-financiero.yaml` (457-560).
