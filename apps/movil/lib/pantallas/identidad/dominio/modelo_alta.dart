import '../../../dominio/errores.dart';

import 'capturas_del_expediente.dart';
import 'datos_del_alta.dart';

export 'datos_del_alta.dart' show DatosLeidosDelDocumento, DatosPersonales;
export 'capturas_del_expediente.dart' show CaraDelCarril, CapturasDelExpediente;

/// Los pasos del alta (CU-01). `anverso`, `reverso` y `pruebaDeVida` se juntan en
/// un único `capturas` — el mismo carrusel de cinco fotos de Atlas, no tres pasos
/// separados — y por eso ahora son siete, no ocho (la maqueta D-1 sigue nombrando
/// ocho porque contaba las tres fotos como pasos propios).
enum PasoAlta { cuenta, correo, documento, datos, actividad, contrato }

/// Qué recorre el asistente. La verificación de identidad (carnet, fotos y
/// actividad) es opcional al crear la cuenta: quien la salta la completa después,
/// cuando quiere mover dinero o participar en un pasanaku (2026-10-17).
enum ModoDelAlta {
  /// El registro con la verificación incluida.
  completo,

  /// El registro sin carnet ni actividad: la cuenta se crea sin verificar.
  sinVerificacion,

  /// Solo la verificación, desde adentro de la app, para una cuenta ya creada.
  /// El paso «contrato» es ahí el envío.
  soloVerificacion,
}

/// Las cuatro fases de orientación del alta; varios pasos internos pueden
/// pertenecer a una misma fase sin cambiar el contrato enviado al servidor.
enum FaseAlta {
  cuenta('Tu cuenta'),
  identidad('Tu identidad'),
  datos('Tus datos'),
  contrato('Contrato');

  const FaseAlta(this.titulo);
  final String titulo;
}

extension FaseDePasoAlta on PasoAlta {
  FaseAlta get fase => switch (this) {
    PasoAlta.cuenta || PasoAlta.correo => FaseAlta.cuenta,
    PasoAlta.documento => FaseAlta.identidad,
    PasoAlta.datos || PasoAlta.actividad => FaseAlta.datos,
    PasoAlta.contrato => FaseAlta.contrato,
  };
}

class EstadoAlta {
  const EstadoAlta({
    this.paso = PasoAlta.cuenta,
    this.modo = ModoDelAlta.completo,
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
    this.campoError,
    this.trazaId,
  });

  final PasoAlta paso;
  final ModoDelAlta modo;
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

  /// El detalle ingresado cuando el origen es `OTRO`. Vacío en cualquier otro
  /// caso: una lista cerrada con detalle pegado atrás deja de ser una lista cerrada.
  final String detalleDelOrigen;

  /// La sección CIIU elegida, también de la lista cerrada.
  final String actividadEconomica;

  /// Lo escrito en «¿Cuál?» cuando la actividad es `OTRA`; vacío en cualquier otra.
  final String detalleDeLaActividad;
  final double? montoMensualEstimado;
  final bool enviando;
  final String? error;
  final CampoDeErrorApi? campoError;
  final String? trazaId;

  /// Los pasos que recorre este modo, en orden.
  List<PasoAlta> get orden => switch (modo) {
    ModoDelAlta.completo => PasoAlta.values,
    ModoDelAlta.sinVerificacion => const [
      PasoAlta.cuenta,
      PasoAlta.correo,
      PasoAlta.datos,
      PasoAlta.contrato,
    ],
    ModoDelAlta.soloVerificacion => const [
      PasoAlta.documento,
      PasoAlta.actividad,
      PasoAlta.contrato,
    ],
  };
  bool get sinVerificacion => modo == ModoDelAlta.sinVerificacion;
  int get numeroDePaso => orden.indexOf(paso) + 1;
  int get totalDePasos => orden.length;
  int get numeroDeFase => FaseAlta.values.indexOf(paso.fase) + 1;
  int get totalDeFases => FaseAlta.values.length;

  EstadoAlta copiarCon({
    PasoAlta? paso,
    ModoDelAlta? modo,
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
    CampoDeErrorApi? campoError,
    String? trazaId,
  }) => EstadoAlta(
    paso: paso ?? this.paso,
    modo: modo ?? this.modo,
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
    campoError: campoError,
    trazaId: trazaId,
  );
}
