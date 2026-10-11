# Integración de origin/dev (7b467d8e) en la rama del cierre — servicios

Base común: 59370bbd (origin/test). Lado "ours" = rama del cierre (334f2e44); lado "theirs" = origin/dev (31 commits: escáner de identidad de cinco capturas, renovación de sesión del backoffice, vencimiento del carnet, límites de recursos de compose).

## identidad: `web/UsuariosController.java` (2 conflictos)
- Importes: se conservan `NivelKyc` (nuestro, `ConsultarNivelKyc`) y `EstadoDeVerificacion` (dev, `consultarEstadoDeVerificacion`). El constructor ya traía las dos dependencias nuevas sin conflicto.
- Método privado `mapear(EntradaRegistro)`: nuestra rama lo había extraído a `MapeoDelAlta`; dev lo había tocado para pasar `fechaExpiracion` al documento. Se conserva la extracción (se borra el método del controlador) y el cambio de dev se aplica en `MapeoDelAlta.mapear` (`cuerpo.getDocumento().getFechaExpiracion()` como último argumento de `DocumentoDeIdentidad.de`). Se pierde cero comportamiento: el vencimiento llega al caso de uso, que es quien lo juzga (prueba de dev `elVencimientoLlegaAlCasoDeUso`).

## identidad: `web/UsuariosControllerWebTest.java` (1 conflicto)
- Solo importes: se conservan `LocalDate` y `OffsetDateTime`. Se conservan las pruebas de dev (vencimiento, estado de verificación) y las nuestras (nivel KYC, consumo de invitación).

## Auto-fusionados (sin conflicto textual) que se verifican por compilación y pruebas
`CU01RegistrarUsuario`, `DocumentoDeIdentidad`, `ExpedienteDeIdentidad`, `EmitirAcceso`, `RenovarSesion`, `SesionController`, `SesionesController`, `VerificacionesController`, `openapi/identidad.yaml`, `gateway/application.yml`, repositorios de identidad. Ver el resultado de la verificación en `verificacion.md`.

## Ajustes que exigió la compilación y las pruebas (no eran conflictos de texto)
- `web/ExpedienteController.java` (nuevo): `UsuariosController` quedó en 315 líneas (el barrido corta en 300). La subida de fotos
  (`subirDocumento`, con el control de titularidad) y `consultarEstadoDeVerificacion` pasan a una clase base abstracta, igual que
  `InvitacionesController`. Comportamiento y anotaciones (`@Publico`) intactos; la prueba web de ambas operaciones sigue contra el mismo
  controlador.
- Pruebas web que levantan `UsuariosController` (`UsuariosAutorizacionNegativaWebTest`, `UsuariosInvitacionesKycWebTest`,
  `UsuariosSubirDocumentoWebTest`, `UsuariosVerificacionCorreoWebTest`) y la de dev `EstadoDeVerificacionWebTest`: cada lado agregó una
  dependencia al controlador y el doble del otro lado no existía. Se agregan los dobles (`CU02ConsultarEstadoDeVerificacion` en las de
  nuestro lado; `ConsumirInvitacion`, `ConsultarNivelKyc`, `SolicitarVerificacionCorreo`, `ConfirmarVerificacionCorreo` en la de dev). No se
  tocó ninguna aserción.
- `BaseDeCU01.registrarConVencimiento` (dev): el record `EntradaRegistro` de nuestro lado tiene `verificacionCorreoId` (7.º argumento); se
  pasa `base.verificacionCorreoId()`.
- `CU02RevisionTest.aprobarElExpedienteActivaAlUsuarioPendienteYRechazarNo` (nuestro): aprobaba un expediente sin fotos y sin documento; la
  regla de dev (AP-CU01-09/10/11, probada en `CU02DecisionDelServidorTest`) lo rechaza con razón. El expediente que se aprueba pasa a
  `expedienteCompleto(usuario, "2030-01-01")`; el que se rechaza y el flujo siguen igual. La aserción de estados no cambia.
- `SesionesControllerWebTest` (dev, 308 líneas): se parte; los cuatro casos de step-up (B37) pasan sin cambios a `SesionesStepUpWebTest`.
- `UsuariosControllerWebTest`: 302 líneas; se factoriza la respuesta aceptada (`ALTA_ACEPTADA`) que se repetía en cuatro pruebas.
- `buildSrc/.../aportaya.base.gradle.kts`: `VerificarTitularidadTest` (carril I, nuestro) extiende `BaseDeCU08` (contenedor) pero corría en
  `test` con 5 s por método: el `@BeforeAll` vencía. Se excluye de `test` y se incluye en `integrationTest`. Es un defecto de nuestra
  rama, no del merge; se arregla acá porque deja el corredor `test` en rojo.
