# Carril A3 · cierre de los hallazgos abiertos de H13.S1 (M2, M3, M6)

Fecha 2026-10-10. Rama `justin/feature/cierre-endurecimiento-2026-10-08`. Salidas literales en esta carpeta
(`*.log`, `conteos-test-results.txt`). Postgres desechable en 5441 (se bajo al terminar). Sin datos reales: los
contactos y documentos de las pruebas son sinteticos.

## Por hallazgo

| # | Hallazgo | Estado | Control (archivo) | Prueba negativa |
|---|---|---|---|---|
| 1 | `subirDocumento` publico con `usuarioId` en la ruta (IDOR sobre el KYC) | **CERRADO** | `identidad/.../web/TitularDelExpediente.java` + `TokenDeAlta.java` (HMAC de proposito unico, atado al usuario, 30 min); el alta (`UsuariosController.registrarUsuario`) devuelve `tokenDeAlta`; cabecera `Token-De-Alta` | `UsuariosSubirDocumentoWebTest` (16): token de otra persona, sesion de otra, sin nada, vencido, forjado, mal formado x7, 403 identico exista o no el usuario, el token no es una sesion |
| 2 | JWKS con una sola clave | **CERRADO** | `EmisorDeAcceso.java` (`aportaya.jwt.clave-firma-anterior` / `JWT_CLAVE_FIRMA_ANTERIOR`, solo publica, nunca firma) | `RotacionDeClavesDeFirmaTest` (8): la sesion vieja sobrevive a la rotacion, la anterior no firma, retirar la anterior invalida lo viejo, ninguna clave privada se publica, anterior sin vigente / mismo kid / JWK ilegible no arrancan |
| 3 | `consultarSupresion` con el contacto en la query (oraculo) | **CERRADO** (contrato nuevo; el GET queda deprecado hasta que `grupos` migre) | `POST /notificaciones/supresion/consultas` (cuerpo) + `LimiteDeConsultas.java` (30/min por actor, 429 con `Retry-After`) | `AutorizacionNegativaWebTest` (notificaciones, 9) |
| 4 | 31 modelos con `@JsonAnySetter` | **CERRADO** | `buildSrc/plantillas-openapi/spring/{pojo,model}.mustache` + `aportaya.openapi.gradle.kts` (`templateDir`) + `plataforma/comun-web/.../CuerposEstrictos.java` (FAIL_ON_UNKNOWN_PROPERTIES en TODA la entrada MVC) | `ModelosSinCamposAbiertosTest` (3, lee plantillas y modelos generados), `AmbitoDeLaPersonaWebTest` (+2 nuevas, 7) (EntradaPep, beneficiario) |
| 5 | Log crudo del manejador y «Using generated security password» | **CERRADO** | `ManejadorGlobalDeErrores.java` + `RastroSeguro.java` (clase, SQLSTATE, restriccion, `trazaId`; nunca el mensaje ni el throwable); gateway `application.yml` excluye las dos autoconfiguraciones de usuario | `BitacoraDeErroresSinContenidoWebTest` (3); log del gateway: 0 ocurrencias (antes 1) en `ArranqueRateLimitGatewayTest.xml` |
| 6 | `HechosPorHttp:224`, `DesafioDeDesarrollo:50`, documento en outbox erp/publicidad | ver abajo | | |
| 7 | H13.S1.M2 a HECHO real | **CERRADO** (en los 3 servicios; el resto sigue con la sabana de cada servicio) | `PoliticaDeAcceso.PUBLICAS_SIN_CONTROL` eliminada y reemplazada por una regla sin lista de excepciones (`ningunaRutaPublicaEscribeSinControl`) | `UsuariosAutorizacionNegativaWebTest` (6), `UsuariosSubirDocumentoWebTest` (16), `AutorizacionNegativaWebTest` (notificaciones 9, cumplimiento 11), `ReclamosControllerWebTest` (10); `MatrizDeAutorizacionContratoTest` (8) en verde |
| 8 | H13.S1.M6 a HECHO: ruta HTTP del efecto de reclamo | **CERRADO** del lado productor; consumidores **A MEDIAS contra el doble** (otro agente) | `GET /reclamos/usuarios/{usuarioId}/efecto`, `RECLAMO_VER`, `ReclamosController.java`; contrato y doble en `contrato-efecto-reclamo.md` y `EfectoDeReclamosDeEnsayo.java` | `ReclamosControllerWebTest` (10) + `EfectoDeReclamosDeEnsayoTest` (22, tres niveles) |
| 9 | Seed `value too long for varchar(60)` y 0 roles | **CERRADO** en la FUENTE | `seeders/minimos/10-roles-y-permisos.json` (2 nombres de rol de 74 y 71 caracteres) + `scripts/generar_semillas.py` ahora valida el largo contra el modelo (antes solo existencia de columna) | `sembrar-corrida1.log` / `sembrar-corrida2.log` RC 0 y `semillas-conteo-*.log`: **17 roles, 37 permisos, 65 rol_permiso**, identicos en la 2a corrida |
| E | Permisos que el backoffice pedia y no existian | **CERRADO** | `RECLAMO_VER`, `SOLICITUD_INGRESO_VER`, `SOLICITUD_INGRESO_RESOLVER` sembrados (ver abajo) | `ReclamosControllerWebTest`; matriz de permisos de `verificar_seguridad.py` |

### 6 · lo que sigue abierto o registrado

- `HechosPorHttp` (grupos) ya usa `POST /usuarios/busquedas/por-telefono` (lo adopto el carril B): las dos lineas de deuda del barrido
  se borraron. **Pendiente de grupos**: `HechosPorHttp.java:170` sigue llamando `GET /notificaciones/supresion?identificador=...` (el
  barrido no lo ve porque `identificador` no esta en su lista de nombres): debe pasar a `POST /notificaciones/supresion/consultas`.
- `DesafioDeDesarrollo`: el codigo ya no se escribe en el log (solo el id del desafio). Linea de deuda borrada.
- `numeroDocumento` en el outbox de erp (`CU102AltaDeTercero.java:73`) y publicidad (`CU110AltaDeAnunciante.java:68`) y el eco del
  documento en el error de erp (`:52`): **NO tocado** (archivos ajenos; no es trivial: cambia el payload que consumen otros). Siguen en
  `salidas-sensibles-conocidas.txt` con duenio. Arreglo propuesto: publicar el `terceroId`/`socioId`.
- Falso positivo declarado: `entregas/.../RespaldoPorHttp.java:54` (`clave` es una clave de idempotencia). Renombrar la variable en entregas.

### Permisos agregados (tarea extra del frontend F2)

| Permiso | Recurso/accion | MFA | Roles |
|---|---|---|---|
| `RECLAMO_VER` | reclamo_cliente / LEER | no | `SOPORTE`, `PUNTO_RECLAMO`, `OFICIAL_CUMPLIMIENTO`, `ADMIN_PLATAFORMA` |
| `SOLICITUD_INGRESO_VER` | solicitud_ingreso / LEER | no | `ADMIN_PLATAFORMA`, `SOPORTE` |
| `SOLICITUD_INGRESO_RESOLVER` | solicitud_ingreso / RESOLVER | si | `ADMIN_PLATAFORMA` |

- `RECLAMO_VER` lo exige `consultarEfectoDeReclamos` (cumplimiento). El backoffice pide ese nombre.
- **Alineacion pendiente, fuera de mi propiedad**: en grupos, `proponerAdmision` exige `GRUPO_ADMINISTRAR`, `resolverAdmision`
  `ADMIN_PLATAFORMA` e `historialAdmision` `PARTICIPANTE` (`RutasDeAdmision.java`). Los dos permisos `SOLICITUD_INGRESO_*` existen en el
  catalogo pero ningun `@Permiso` de grupos los usa todavia: con un token real el backoffice los tiene y las rutas de grupos aceptan
  `ADMIN_PLATAFORMA`, asi que la pantalla es alcanzable; pero la segregacion «quien propone no resuelve» la hace `AdmisionDelGrupo`
  (rol/ id), no el permiso. Para que el `@Permiso` use los nombres nuevos hay que tocar `RutasDeAdmision.java` y `GruposController`
  (carril B). `ADMIN_PLATAFORMA` es la unica identidad con `SOLICITUD_INGRESO_RESOLVER` y nadie se autoresuelve (lo valida grupos).

## Verificacion

Ver `conteos-test-results.txt` (conteos reales de `build/test-results/*.xml`) y los logs por etapa:
`c2-compilar.log` (compila los tres servicios + plataforma), `e*-*.log` (spotless, compilacion de los 15 servicios, test/testBarrido,
webTest, integrationTest de identidad, notificaciones, cumplimiento y gateway), `f*-*.log` (re-corrida tras ajustar las pruebas
de otros servicios), `g*-*.log` (integrationTest de los servicios que el generador toco).

### Pruebas de otros servicios ajustadas (sin debilitar aserciones)

Con la entrada estricta, cuatro pruebas web que mandaban a proposito un campo no declarado y esperaban que el caso de uso
«no lo recibiera» ahora reciben `400` (la garantia es mas fuerte: el caso de uso ni se invoca). Se cambio el estado esperado a 400
con `verifyNoInteractions`, y la parte que verificaba que el valor del servidor manda se conserva sobre una peticion VALIDA:

- `garantia/.../RespaldoCoberturaWebTest.elClienteNoDictaImportes`
- `grupos/.../AcuerdosControllerWebTest.elQuorumNoLoDeclaraElProponente`
- `grupos/.../TurnosControllerWebTest.losHechosNoLosDeclaraQuienPide`
- `inversiones/.../InversionesAnalisisWebTest.simulacion`

## Conteos (de `build/test-results/*.xml`, ver `conteos-test-results.txt`)

18 modulos, **3215 tests, 0 fallas, 0 errores, 0 omitidas**. Por tarea, los modulos de este carril:

| Modulo | test | webTest | integrationTest | testBarrido |
|---|---|---|---|---|
| identidad | 20 | 81 | 120 | 2 |
| notificaciones | 5 | 30 | 60 | 2 |
| cumplimiento | 14 | 48 | 280 | 2 |
| comun-web | 3 | 32 | 10 | 3 |
| comun-pruebas | 52 (+ contractTest 20, integrationTest 15) | - | - | 5 |
| gateway | - | - | 2 | - |

Servicios que el generador toco, re-corridos en esta vuelta (test + webTest + integrationTest + testBarrido, 0 fallas):
auditoria (16/28/43/2), erp (5/19/82/2), publicidad (5/16/52/2), tarifas (5/28/96/2), transparencia, organizador.
`garantia`, `grupos`, `inversiones`, `entregas`, `aportes` y `nucleo-financiero`: `test`, `webTest` y `testBarrido` se re-corrieron
(el generador y la entrada estricta los afectan); su `integrationTest` **no** se re-corrio en esta vuelta (cuentan lo de la corrida anterior).

Gates: `python scripts/verificar_pruebas_web.py` -> 34 controladores, 34 con matriz propia, 0 en deuda, 15 sabanas.
`python scripts/verificar_seguridad.py` -> 1 FALLA ajena (ver No cubierto); rutas abiertas: «las 5 rutas abiertas son las declaradas»;
«los 37 permisos sembrados respetan R-SEG-12». `scripts/inventario_endpoints.py` regenerado (231 endpoints relevados; incluye los 3 nuevos de este carril y los de otros carriles). Sigue listando 20 operaciones
declaradas en contrato sin controlador (de grupos, identidad -invitaciones- y organizador): preexistentes y ajenas.

## No cubierto

- Nada corrio contra los servicios levantados ni contra el gateway real con identidad real (la subida con token de alta se ejercita
  en la capa MVC con el caso de uso doblado; el almacen y la base reales de `subirDocumento` no se tocaron en esta vuelta).
- El **cliente movil** (`apps/movil`) ya manda `Token-De-Alta` (`cu01_registrar.dart`, `cu02_subir_foto.dart`,
  `subida_del_expediente.dart`) pero **no se compilo ni se probo** (Flutter/Dart fuera de este carril y del guardian de RAM):
  hay que regenerar el cliente Dart (`./gradlew generarClientes`) porque `SalidaRegistro.tokenDeAlta` es un campo nuevo.
  El flujo web de alta no sube fotos (grep en `apps/web`, `packages`: ninguna llamada a `/documentos`).
- `FAIL_ON_UNKNOWN_PROPERTIES` rechaza ahora cualquier campo de mas en TODOS los servicios: un cliente (Angular/Dart/postman) que
  mande un campo que el contrato no declara recibira 400. Las pruebas web del repo pasan; **no se corrio contra los frontends**.
- Los clientes TS/Dart (`clientes/`, ignorados por git) y `packages/simulado/{generado,ejemplos}` se regeneran desde los contratos:
  agregan `buscarUsuarioPorTelefono`, `verificarSupresion`, `consultarEfectoDeReclamos` y el header/campos de `subirDocumento` y
  `SalidaRegistro`. No los regenere (otro dueño, y ya hay ejemplos del efecto creados por otro agente).
- `verificar_seguridad.py`: queda 1 FALLA que no es de este carril: `apps/backoffice/e2e/aceptacion/apoyo/doble-con-estado.ts:47,51`
  (aleatoriedad no criptografica). `verificar_criterios.py`: 14 pruebas sin criterio en la boveda, ninguna de este carril
  (CU-08 identidad: `CU08CodigosDeRolVigentesTest`, ya en HEAD; CU-62/63/64/68 de grupos).
- La rotacion real en Coolify no se ensayo: sigue siendo el ensayo sintetico + la prueba del emisor.

## Decisiones y supuestos

- Token de alta sin estado (HMAC con la pimienta del servicio, 30 min configurable `identidad.alta.vigencia-token`) en vez de una tabla:
  no cambia el esquema ni hay nada que limpiar. Un token se puede reutilizar dentro de la ventana para subir las tres caras (es lo
  que hace falta); la idempotencia de cada subida sigue siendo la del `Idempotency-Key`. Si negocio quiere un solo uso, hace falta tabla.
- `FAIL_ON_UNKNOWN_PROPERTIES` solo en el convertidor MVC de entrada (no en el `ObjectMapper` global): los clientes HTTP entre
  servicios siguen tolerando campos nuevos del productor (98.1.3).
- El tope de consultas de supresion es por replica (en memoria): con N replicas el tope efectivo es N veces el configurado.
