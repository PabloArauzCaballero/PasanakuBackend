# Integración de origin/dev (7b467d8e) en la rama del cierre — servicios

Base común: 59370bbd (origin/test). Lado "ours" = rama del cierre (334f2e44); lado "theirs" = origin/dev (31 commits: escáner de identidad de cinco capturas, renovación de sesión del backoffice, vencimiento del carnet, límites de recursos de compose).

## identidad: `web/UsuariosController.java` (2 conflictos)
- Importes: se conservan `NivelKyc` (nuestro, `ConsultarNivelKyc`) y `EstadoDeVerificacion` (dev, `consultarEstadoDeVerificacion`). El constructor ya traía las dos dependencias nuevas sin conflicto.
- Método privado `mapear(EntradaRegistro)`: nuestra rama lo había extraído a `MapeoDelAlta`; dev lo había tocado para pasar `fechaExpiracion` al documento. Se conserva la extracción (se borra el método del controlador) y el cambio de dev se aplica en `MapeoDelAlta.mapear` (`cuerpo.getDocumento().getFechaExpiracion()` como último argumento de `DocumentoDeIdentidad.de`). Se pierde cero comportamiento: el vencimiento llega al caso de uso, que es quien lo juzga (prueba de dev `elVencimientoLlegaAlCasoDeUso`).

## identidad: `web/UsuariosControllerWebTest.java` (1 conflicto)
- Solo importes: se conservan `LocalDate` y `OffsetDateTime`. Se conservan las pruebas de dev (vencimiento, estado de verificación) y las nuestras (nivel KYC, consumo de invitación).

## Auto-fusionados (sin conflicto textual) que se verifican por compilación y pruebas
`CU01RegistrarUsuario`, `DocumentoDeIdentidad`, `ExpedienteDeIdentidad`, `EmitirAcceso`, `RenovarSesion`, `SesionController`, `SesionesController`, `VerificacionesController`, `openapi/identidad.yaml`, `gateway/application.yml`, repositorios de identidad. Ver el resultado de la verificación en `verificacion.md`.
