# Credenciales con identidad de servicio (inversiones, entregas) — carril C3

Pregunta: los roles `SERVICIO_INVERSIONES` y `SERVICIO_ENTREGAS` existen en `seeders/minimos/10-roles-y-permisos.json`
y nucleo-financiero los exige, pero nadie los usaba. ¿Cómo obtiene cada servicio su credencial?

## Qué ya tenía el repo (hechos, con ruta)

| Pieza | Dónde | Qué dice |
|---|---|---|
| Un solo emisor de tokens | `servicios/identidad/.../EmisorDeAcceso.java` | RS256, 15 min, reclamos `sub`, `rol`, `permisos`; los otros servicios solo verifican contra el JWKS |
| Cómo se autoriza | `plataforma/comun-web/.../GuardiaDePermiso.java:46` | `@Permiso("X")` pasa si el reclamo `rol == X` **o** `permisos` contiene `X` |
| Qué trae un token real | `servicios/identidad/.../SesionesController.java:41-55` y `EmitirAcceso.java` | `rol` = `PARTICIPANTE` o `BACKOFFICE` (el piso de la sesión); los códigos de rol vigentes viajan en `permisos` |
| Mecanismo declarado para servicios | `docs/Arquitectura/ADR-028 Mecánica de saga.md` §5 | «token de cliente emitido por identidad (client credentials, vigencia corta, alcance por servicio)» — **no está implementado** (identidad.yaml no tiene tal endpoint) |

## Hallazgo (defecto real, corregido en nucleo-financiero)

`ReglasDeEjecucion.exigirServicio` compara `ctx.rol()` con `SERVICIO_INVERSIONES`. Con un token real de identidad `rol` es
`BACKOFFICE`, así que **la operación habría rechazado a inversiones aunque `@Permiso` lo dejara pasar**. Las pruebas no lo
mostraban porque `Sesiones.como("SERVICIO_INVERSIONES")` pone el rol de servicio en `rol`. Corrección:
`servicios/nucleo-financiero/.../web/IdentidadDeServicio.java` (el contexto toma el rol de servicio que el permiso del token acredita; un
token sin el permiso no se disfraza). Pruebas: `InversionesWebTest.tokenRealDeIdentidad` y el pacto completo
`CU13PactoInversionesTest` (token firmado con forma de identidad, verificado contra un JWKS real).

## Decisión (lado inversiones, implementada)

La credencial de servicio es **el mismo token de acceso de identidad**, de una cuenta de servicio con el rol `SERVICIO_INVERSIONES`:

* `CredencialPorLogin` (`servicios/inversiones/.../infraestructura/clientes/`): entra por `POST /sesiones` (CU-04) con teléfono, clave y
  segundo factor TOTP (RFC 6238, `CodigoTotp`), cachea el token hasta 90 s antes de su `exp` y lo renueva sola, con un solo hilo
  renovando. Ante un fallo espera 30 s antes de reintentar (cada intento fallido cuenta para el bloqueo de la cuenta).
  Falla cerrado: sin credencial no sale ninguna petición que mueva plata.
* `ConfiguracionDeCredencialDeServicio`: elige cuenta de servicio → token emitido (`INVERSIONES_LIBRO_CREDENCIAL_SERVICIO`) → nada.
  Una cuenta configurada a medias **no arranca** (90.3.4).
* `LibroPorHttp` ya no recibe un `String`: recibe el puerto `CredencialDeServicio`.
* Variables nuevas (vacías en el repo; `scripts/generar_compose.py`, `DE_DESARROLLO` y `DE_ENTORNO`; compose de Coolify regenerado):
  `INVERSIONES_SERVICIO_TELEFONO`, `INVERSIONES_SERVICIO_CLAVE`, `INVERSIONES_SERVICIO_TOTP_SEMILLA`, `INVERSIONES_SERVICIO_FACTOR_FIJO`.
  La clave entra en la deuda declarada de `SecretosSinDefaultBarridoTest` (opcional por diseño, falla cerrado).

`entregas` (SERVICIO_ENTREGAS): **no se implementó** (es de otro carril). Su credencial se obtiene igual (cuenta de servicio +
`CredencialPorLogin`, reutilizable porque no depende de inversiones); hoy nucleo→entregas propaga el token de quien llama
(`ClienteDeServicio`), y entregas→nucleo (`pagarAlTercero`) necesita ese mismo mecanismo en `servicios/entregas`.

## Pruebas (resultado en `conteos-carril-C3.txt`)

* `CredencialPorLoginContratoTest` (9): identidad como doble HTTP declarado — correcto (entra, factor TOTP del instante), límite
  (reutiliza, renueva una vez antes de vencer, `exp` ausente), inválido (422, sin `tokenAcceso`, 5xx, basura, timeout, configuración a medias).
* `CodigoTotpTest` (3): vectores del apéndice B de RFC 6238.
* `LibroPorHttpPactoContratoTest` (3) y `CU13PactoInversionesTest` (2): ver `integracion-entre-servicios.md`.

## Qué NO está cubierto y quién lo cierra (ABIERTO-solo-persona / otro carril)

1. **identidad** no valida un segundo factor real fuera del perfil `local` (`DesafioLocal` nunca entrega el código; `DesafioDeDesarrollo` acepta
   `000000`), y no hay endpoint de credenciales de cliente (ADR-028 §5). Hasta que exista uno de los dos, la cuenta de servicio solo
   puede entrar en TEST (perfil `local`, `INVERSIONES_SERVICIO_FACTOR_FIJO`). Lo cierra el dueño de `identidad`.
2. **Aprovisionar** la cuenta (usuario, rol `SERVICIO_INVERSIONES`, factor, clave) y cargar las variables en Coolify: acción de quien
   opera el entorno; los usuarios no entran a `seeders/minimos` (datos de personas) y las claves reales no van al repo.
3. Que `esOperador()` (rol de ámbito GLOBAL → segundo factor obligatorio) sea el comportamiento deseado para cuentas de servicio:
   decisión de seguridad (a quien consulte identidad/seguridad).
4. Rotación de la clave de la cuenta de servicio: no ensayada.
