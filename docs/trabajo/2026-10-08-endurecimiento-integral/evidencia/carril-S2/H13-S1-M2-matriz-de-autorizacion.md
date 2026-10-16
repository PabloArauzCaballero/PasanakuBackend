# H13.S1.M2 · Matriz de autorización negativa transversal

CA del plan: «Dado usuario de otro grupo/ámbito, cuando intenta leer o actuar, entonces se rechaza sin
revelar datos» (E-H13-02, incluido el acceso de servicios).

## Qué se construyó

| Pieza | Archivo | Qué prueba |
|---|---|---|
| Lector de la matriz | `plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/seguridad/{MapaDeSeguridad,FuenteJava,Operacion,Decision}.java` | Cruza los 15 OpenAPI con los controladores: por cada `operationId`, su `@Permiso`/`@Publico`, el archivo y la línea |
| Decisiones humanas, a la vista | `.../src/test/java/.../seguridad/PoliticaDeAcceso.java` | Rutas sin sesión (con su control), ámbito por persona (con el archivo que lo sostiene) |
| Matriz (contrato) | `MatrizDeAutorizacionContratoTest` (7 pruebas) | 15 servicios y > 200 operaciones recorridas; **toda operación tiene decisión de acceso**; todo permiso existe en el catálogo sembrado; la lista de rutas sin sesión es exactamente la declarada; cada una con razón y control que existe en el código; toda operación de participante que nombra a una persona declara su ámbito, y el control declarado está de verdad en el código |
| Inyección al lector | `MapaDeSeguridadDeteccionTest` (6 pruebas) | Un servicio sintético con un endpoint sin decisión, una justificación con `;` y paréntesis, un Javadoc que nombra la anotación, y una operación sin controlador: el lector los ve |
| Pruebas web negativas (cumplimiento) | `servicios/cumplimiento/src/test/java/.../web/AmbitoDeLaPersonaWebTest.java` (4) | Participante no declara PEP ni acepta contrato en nombre de otro: 403 `AP-SEG-01`, el caso de uso ni se entera, el cuerpo no contiene el id ajeno y es **idéntico** al 403 por falta de permiso |
| Prueba de IDOR (auditoría) | `servicios/auditoria/src/test/java/.../AutorizacionNegativaTest.java` (3, integración) | Otro auditor con `AUDITORIA_LEER` no baja la exportación ajena, no consume su tope, y recibe el mismo mensaje que para una exportación inexistente |

La sabana dinámica por servicio (`SabanaDeSeguridadWeb`: 401 sin sesión, 403 sin permiso, 403 con permisos de
la operación de al lado, 400 sin `Idempotency-Key`) ya existía en los 15; esto agrega lo que ella no ve.

## Lo que la matriz encontró

Operaciones: 213 (aportes 10, auditoria 4, cumplimiento 9, entregas 14, erp 15, garantia 20, grupos 26,
identidad 18, inversiones 12, notificaciones 3, nucleo-financiero 16, organizador 22, publicidad 14,
tarifas 12, transparencia 18). Decisión: 200 con `@Permiso`, 13 con `@Publico`, **0 sin decisión**.

### Hallazgos (archivo:línea; no corregidos si el servicio es ajeno)

| # | Sev. | Dónde | Qué | Dueño / arreglo propuesto |
|---|---|---|---|---|
| 1 | **Alta** | `servicios/identidad/src/main/java/bo/aportaya/identidad/web/UsuariosController.java:184` (`subirDocumento`, `POST /usuarios/{usuarioId}/documentos`) + `aplicacion/CU02GuardarFotoDelExpediente.java` | Es `@Publico` y el `usuarioId` viaja en la ruta: quien conozca (o adivine) el UUID de otra persona escribe fotos en **su** expediente KYC, sin sesión ni token. El caso de uso no verifica que el usuario esté en alta | identidad. Exigir un token de propósito único emitido en el alta (CU-01) y atado al `usuarioId`, o limitar a usuarios en `PRE_REGISTRO`/`PENDIENTE_VERIFICACION` con un desafío de alta |
| 2 | Media | `servicios/identidad/.../infraestructura/EmisorDeAcceso.java:143` | El JWKS publica **una sola clave**: rotar `JWT_CLAVE_FIRMA` desloguea a todos | identidad: JWKS con dos claves durante la ventana (ensayado en `rotacion-ensayo.log`) |
| 3 | Media | `servicios/aportes/.../AportesController.java:62` (`consultarEstadoDelParticipante`) | `participanteId` en la ruta bajo `BILLETERA_VER`; el caso de uso no compara con la sesión. Hoy la base lo niega por omisión (`obligacion_aporte` no lleva `usuario_id`, `fn_seg_aplicar_rls`), lo que devuelve «al día» en vez de 403: la protección es accidental | aportes: decidir si es endpoint de servicio (entonces permiso de servicio, no `BILLETERA_VER`) o de persona (entonces control explícito) |
| 4 | Media | `servicios/notificaciones/.../NotificacionesController.java:52` (`consultarSupresion`, `GET /notificaciones/supresion?identificador=…`) | El identificador (teléfono o correo) viaja en la **query** (90.2.2) y cualquier `GRUPO_ADMINISTRAR` de **cualquier** grupo puede preguntar por cualquier contacto: es un oráculo de «este contacto pidió no recibir» | notificaciones + grupos. `POST /notificaciones/supresion/consultas` con el identificador en el cuerpo y límite de consultas. Cambia el contrato y a su consumidor (grupos): no se hizo en este carril |
| 5 | Baja | `servicios/auditoria/.../DerechosController.java:36` (`ejercerDerechos`, `DATOS_SENSIBLES_LEER`, `usuarioId` en el cuerpo) | El permiso es de personal, no del titular: el titular no puede ejercer sus derechos (CU-07) por esta ruta | **Decisión de negocio**: ¿quién ejerce el derecho, el titular o un operador en su nombre? A confirmar con Cumplimiento/Legal |
| 6 | Info | Roles `SERVICIO_ENTREGAS`, `SERVICIO_INVERSIONES` (`seeders/minimos/10-roles-y-permisos.json`) | Existen identidades de servicio como **rol**, pero las llamadas entre servicios reenvían el token del **usuario** (`ClienteDeServicio.propagarElToken`) y los trabajos programados salen **sin token** (el propio Javadoc de `ClienteDeServicio` lo dice) y toda ruta no pública exige sesión → el destino respondería 401 (inferido de la guardia, no ejercitado) | plataforma: definir el token de servicio con alcance acotado (98.6.3); hoy un trabajo no puede consultar a otro servicio |
| 7 | Media | `buildSrc/src/main/kotlin/aportaya.openapi.gradle.kts:92` (configuración del generador) y `servicios/*/build/generated/openapi/.../modelo/*.java` | Spring Boot **no rechaza campos desconocidos** (`fail-on-unknown-properties=false` por omisión) y, además, **31 modelos de petición/respuesta de 11 servicios** salen del generador con `@JsonAnySetter` (p. ej. cumplimiento `EntradaPep.java:208`, `EntradaDiligencia`; auditoría, entregas, erp, garantia, nucleo-financiero, organizador, publicidad, tarifas, transparencia, notificaciones): el contrato dice `additionalProperties: false` y el servidor lo acepta igual (mass assignment, regla 96.2.2) | plataforma/plantilla de servicios: configurar el generador para que no emita el mapa de propiedades adicionales, y activar `fail-on-unknown-properties` en `comun-web` |
| 8 | Baja | `servicios/notificaciones/.../NotificacionesController.java` (`emitirNotificacion`) | `datos` está tipado `Object` por el generador: una cadena o una lista pasaban la validación y se publicaban al outbox | **Cerrado** (ver abajo) |

### Cerrados en este carril

- `fail-on-unknown-properties: true` en `application.yml` de **cumplimiento, auditoría y notificaciones**: un campo que el contrato no
  declara devuelve 400 `AP-VAL-02` en los modelos que no lo absorben (prueba `AmbitoDeLaPersonaWebTest.rechazaElCampoDeMas`,
  `CuerpoEstrictoWebTest`, `AlarmasOperativasWebTest.rechazaLoInvalido`). `EntradaPep` y `EntradaDiligencia` lo siguen absorbiendo (hallazgo 7).
- `emitirNotificacion` (notificaciones) rechaza con 422 un `datos` que no es un objeto (`AlarmasOperativasWebTest`).

- `declararPep` y `aceptarContrato` (cumplimiento): antes cualquier `PARTICIPANTE` podía actuar sobre otra persona
  con solo poner su UUID; lo único que lo frenaba era la política de fila de la base (`usuario_id = fn_seg_usuario_actual()`), que no se ejercitó acá y cuyo código HTTP de rechazo no es el 403 genérico.
  Ahora `AdhesionController.soloElPropio` lo corta antes de tocar el caso de uso.
- `descargarExportacion` (auditoría): cualquier titular de `AUDITORIA_LEER` podía autorizar la descarga de la
  exportación que otro pidió (la tabla `exportacion_reporte` no lleva `usuario_id`, así que no tenía política de fila).
  Ahora solo el solicitante, y para los demás la respuesta es la de «no existe».

## Matriz de acceso entre servicios (lo que se pudo demostrar)

Servicios que llaman a otros por HTTP (30 sitios de llamada en 8 clientes): `HechosPorHttp` (grupos y
nucleo-financiero), `HechosDeGruposPorHttp` (aportes), `RecaudoPorHttp` (entregas, garantia), `RespaldoPorHttp`,
`TitularPorHttp` (entregas), `CotizadorPorHttp` (nucleo-financiero), `LibroPorHttp` (inversiones), `PaquetePorHttp`
(transparencia). Todos propagan el **token del usuario** que originó la petición; el destino aplica su
`@Permiso` y su política de fila con esa identidad. Consecuencias verificadas en el código: no hay confianza
implícita en la red interna (bien, 98.6.1), pero tampoco hay identidad de servicio para trabajos sin usuario
(hallazgo 6).

## No cubierto

- Las 9 operaciones de «persona + permiso de participante» sin control de código (`POLITICA_DE_FILA`) se apoyan
  en las políticas de fila de la base; **no se ejercitó cada una contra la base real** con dos usuarios (eso es
  la `AutorizacionNegativaTest` de cada servicio dueño; sólo nucleo-financiero y auditoría la tienen).
- La matriz mira operaciones con identificador de persona (`usuarioId`, `participanteId`, `titularId`); el
  aislamiento por **grupo** (`grupoId`) entre administradores de grupos distintos no se verificó acá: lo
  decide `Consultas.esAdministradorDelGrupo` en grupos y no está en la matriz.
- Nada de lo anterior corre contra los servicios levantados ni contra el gateway.


## Cierre en el carril A3 (2026-10-10)

Los hallazgos abiertos de este documento se cerraron en `evidencia/carril-A3/RESULTADO.md` (control archivo:linea, pruebas negativas con
dos personas y conteos). Resumen: 1 (subirDocumento, token de alta), 2 (JWKS de dos claves), 3 (supresion por cuerpo + tope),
4/7 (plantilla del generador sin `@JsonAnySetter` + entrada estricta), 8 (`/reclamos/usuarios/{id}/efecto`, `RECLAMO_VER`).
Quedan abiertos con duenio: 3 (aportes `consultarEstadoDelParticipante`), 5 (auditoria `ejercerDerechos`, decision de negocio) y 6 (identidad de servicio).
