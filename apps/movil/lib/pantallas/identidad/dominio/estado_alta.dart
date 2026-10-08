import 'datos_del_alta.dart';

export 'datos_del_alta.dart' show DatosLeidosDelDocumento, DatosPersonales;

/// Los pasos del alta de cuenta. Cada pantalla representa un paso.
enum PasoAlta {
  datos,
  contrasena,
  celular,
  anverso,
  reverso,
  pruebaDeVida,
  cotejo,
  perfilTransaccional,
  contrato,
}

/// Estado visible y serializable en memoria del asistente de registro.
class EstadoAlta {
  const EstadoAlta({
    this.paso = PasoAlta.datos,
    this.datos = const DatosPersonales(),
    this.contrasena = '',
    this.codigoConfirmado = false,
    this.rutaAnverso,
    this.rutaReverso,
    this.rutaSelfie,
    this.leido = const DatosLeidosDelDocumento(),
    this.origenDeFondos = '',
    this.detalleDelOrigen = '',
    this.actividadEconomica = '',
    this.montoMensualEstimado = '',
    this.enviando = false,
    this.error,
    this.restaurando = false,
    this.resumido = false,
    this.errorBorrador,
    this.fotosPendientes = false,
  });

  final PasoAlta paso;
  final DatosPersonales datos;

  /// Vive solo en memoria y en la petición de registro; nunca en el borrador.
  final String contrasena;
  final bool codigoConfirmado;
  final String? rutaAnverso;
  final String? rutaReverso;
  final String? rutaSelfie;
  final DatosLeidosDelDocumento leido;
  final String origenDeFondos;
  final String detalleDelOrigen;
  final String actividadEconomica;
  final String montoMensualEstimado;
  final bool enviando;
  final String? error;
  final bool restaurando;
  final bool resumido;
  final String? errorBorrador;
  final bool fotosPendientes;

  static const _orden = PasoAlta.values;
  int get numeroDePaso => _orden.indexOf(paso) + 1;
  int get totalDePasos => _orden.length;

  EstadoAlta copiarCon({
    PasoAlta? paso,
    DatosPersonales? datos,
    String? contrasena,
    bool? codigoConfirmado,
    String? rutaAnverso,
    String? rutaReverso,
    String? rutaSelfie,
    DatosLeidosDelDocumento? leido,
    String? origenDeFondos,
    String? detalleDelOrigen,
    String? actividadEconomica,
    String? montoMensualEstimado,
    bool? enviando,
    String? error,
    bool? restaurando,
    bool? resumido,
    String? errorBorrador,
    bool? fotosPendientes,
  }) => EstadoAlta(
    paso: paso ?? this.paso,
    datos: datos ?? this.datos,
    contrasena: contrasena ?? this.contrasena,
    codigoConfirmado: codigoConfirmado ?? this.codigoConfirmado,
    rutaAnverso: rutaAnverso ?? this.rutaAnverso,
    rutaReverso: rutaReverso ?? this.rutaReverso,
    rutaSelfie: rutaSelfie ?? this.rutaSelfie,
    leido: leido ?? this.leido,
    origenDeFondos: origenDeFondos ?? this.origenDeFondos,
    detalleDelOrigen: detalleDelOrigen ?? this.detalleDelOrigen,
    actividadEconomica: actividadEconomica ?? this.actividadEconomica,
    montoMensualEstimado: montoMensualEstimado ?? this.montoMensualEstimado,
    enviando: enviando ?? false,
    error: error,
    restaurando: restaurando ?? false,
    resumido: resumido ?? this.resumido,
    errorBorrador: errorBorrador,
    fotosPendientes: fotosPendientes ?? this.fotosPendientes,
  );
}
