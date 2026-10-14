import 'capturas_del_expediente.dart';
import 'datos_del_alta.dart';

/// Los pasos del alta (CU-01). `anverso`, `reverso` y `pruebaDeVida` se juntan en
/// un único `capturas` — el mismo carrusel de cinco fotos de Atlas, no tres pasos
/// separados — y por eso ahora son siete, no ocho (la maqueta D-1 sigue nombrando
/// ocho porque contaba las tres fotos como pasos propios).
enum PasoAlta {
  datos,
  // Va segundo, antes de las fotos. Quien abandona el alta en el paso del documento
  // —que es donde más gente la abandona— ya eligió con qué va a entrar cuando vuelva;
  // al final del formulario nadie lee una regla de contraseña.
  contrasena,
  celular,
  capturas,
  cotejo,
  perfilTransaccional,
  contrato,
}

class EstadoAlta {
  const EstadoAlta({
    this.paso = PasoAlta.datos,
    this.datos = const DatosPersonales(),
    this.contrasena = '',
    this.codigoConfirmado = false,
    this.verificacionCorreoId,
    this.destinoVerificacion,
    this.enviandoCodigo = false,
    this.verificandoCodigo = false,
    this.errorCodigo,
    this.capturas = const CapturasDelExpediente(),
    this.leido = const DatosLeidosDelDocumento(),
    this.origenDeFondos = '',
    this.detalleDelOrigen = '',
    this.actividadEconomica = '',
    this.detalleDeLaActividad = '',
    this.montoMensualEstimado,
    this.enviando = false,
    this.error,
  });

  final PasoAlta paso;
  final DatosPersonales datos;

  /// La clave elegida, en memoria y solo mientras dura el alta. No se persiste en
  /// ningún lado del teléfono: viaja en la petición de CU-01 y el estado se vacía al
  /// terminar. Guardarla en el almacén seguro sería guardar una credencial que el
  /// servidor ya tiene hasheada.
  final String contrasena;
  final bool codigoConfirmado;
  final String? verificacionCorreoId;
  final String? destinoVerificacion;
  final bool enviandoCodigo;
  final bool verificandoCodigo;
  final String? errorCodigo;
  final CapturasDelExpediente capturas;
  final DatosLeidosDelDocumento leido;

  /// El código de la lista cerrada (`catalogo_del_perfil.dart`), no texto libre.
  final String origenDeFondos;

  /// Lo que se escribió a mano cuando el origen es `OTRO`. Vacío en cualquier otro
  /// caso: una lista cerrada con detalle pegado atrás deja de ser una lista cerrada.
  final String detalleDelOrigen;

  /// La sección CIIU elegida, también de la lista cerrada.
  final String actividadEconomica;

  /// Lo escrito en «¿Cuál?» cuando la actividad es `OTRA`; vacío en cualquier otra.
  final String detalleDeLaActividad;
  final double? montoMensualEstimado;
  final bool enviando;
  final String? error;

  static const _orden = PasoAlta.values;
  int get numeroDePaso => _orden.indexOf(paso) + 1;
  int get totalDePasos => _orden.length;

  EstadoAlta copiarCon({
    PasoAlta? paso,
    DatosPersonales? datos,
    String? contrasena,
    bool? codigoConfirmado,
    String? verificacionCorreoId,
    String? destinoVerificacion,
    bool? enviandoCodigo,
    bool? verificandoCodigo,
    String? errorCodigo,
    bool limpiarVerificacion = false,
    CapturasDelExpediente? capturas,
    DatosLeidosDelDocumento? leido,
    String? origenDeFondos,
    String? detalleDelOrigen,
    String? actividadEconomica,
    String? detalleDeLaActividad,
    double? montoMensualEstimado,
    bool? enviando,
    String? error,
  }) => EstadoAlta(
    paso: paso ?? this.paso,
    datos: datos ?? this.datos,
    contrasena: contrasena ?? this.contrasena,
    codigoConfirmado: limpiarVerificacion
        ? false
        : codigoConfirmado ?? this.codigoConfirmado,
    verificacionCorreoId: limpiarVerificacion
        ? null
        : verificacionCorreoId ?? this.verificacionCorreoId,
    destinoVerificacion: limpiarVerificacion
        ? null
        : destinoVerificacion ?? this.destinoVerificacion,
    enviandoCodigo: enviandoCodigo ?? false,
    verificandoCodigo: verificandoCodigo ?? false,
    errorCodigo: errorCodigo,
    capturas: capturas ?? this.capturas,
    leido: leido ?? this.leido,
    origenDeFondos: origenDeFondos ?? this.origenDeFondos,
    detalleDelOrigen: detalleDelOrigen ?? this.detalleDelOrigen,
    actividadEconomica: actividadEconomica ?? this.actividadEconomica,
    detalleDeLaActividad: detalleDeLaActividad ?? this.detalleDeLaActividad,
    montoMensualEstimado: montoMensualEstimado ?? this.montoMensualEstimado,
    enviando: enviando ?? false,
    error: error,
  );
}
