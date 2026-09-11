import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Los ocho pasos del alta (CU-01, delta D-1 de la maqueta). Un paso = un
/// organismo; ninguno conoce a los demás, solo al notifier.
enum PasoAlta {
  datos,
  celular,
  anverso,
  reverso,
  pruebaDeVida,
  cotejo,
  perfilTransaccional,
  contrato,
}

class DatosPersonales {
  const DatosPersonales({
    this.nombres = '',
    this.apellidos = '',
    this.fechaNacimiento,
    this.telefono = '',
    this.tipoDocumento = 'CI',
    this.numeroDocumento = '',
  });
  final String nombres;
  final String apellidos;
  final DateTime? fechaNacimiento;
  final String telefono;
  final String tipoDocumento;
  final String numeroDocumento;

  DatosPersonales copiarCon({
    String? nombres,
    String? apellidos,
    DateTime? fechaNacimiento,
    String? telefono,
    String? tipoDocumento,
    String? numeroDocumento,
  }) => DatosPersonales(
    nombres: nombres ?? this.nombres,
    apellidos: apellidos ?? this.apellidos,
    fechaNacimiento: fechaNacimiento ?? this.fechaNacimiento,
    telefono: telefono ?? this.telefono,
    tipoDocumento: tipoDocumento ?? this.tipoDocumento,
    numeroDocumento: numeroDocumento ?? this.numeroDocumento,
  );
}

/// Lo leído del documento por el proveedor de KYC (CU-01 flujo 3). Sin cliente de
/// identidad generado (§ hueco H-CLIENTE del informe), esto queda como el shape que
/// consumirá `FilaDeCotejo`, poblado por el propio usuario mientras no haya OCR.
class DatosLeidosDelDocumento {
  const DatosLeidosDelDocumento({
    this.nombres = '',
    this.apellidos = '',
    this.numeroDocumento = '',
  });
  final String nombres;
  final String apellidos;
  final String numeroDocumento;
}

class EstadoAlta {
  const EstadoAlta({
    this.paso = PasoAlta.datos,
    this.datos = const DatosPersonales(),
    this.codigoConfirmado = false,
    this.rutaAnverso,
    this.rutaReverso,
    this.rutaSelfie,
    this.leido = const DatosLeidosDelDocumento(),
    this.origenDeFondos = '',
    this.actividadEconomica = '',
    this.montoMensualEstimado,
    this.enviando = false,
    this.error,
  });

  final PasoAlta paso;
  final DatosPersonales datos;
  final bool codigoConfirmado;
  final String? rutaAnverso;
  final String? rutaReverso;
  final String? rutaSelfie;
  final DatosLeidosDelDocumento leido;
  final String origenDeFondos;
  final String actividadEconomica;
  final double? montoMensualEstimado;
  final bool enviando;
  final String? error;

  static const _orden = PasoAlta.values;
  int get numeroDePaso => _orden.indexOf(paso) + 1;
  int get totalDePasos => _orden.length;

  EstadoAlta copiarCon({
    PasoAlta? paso,
    DatosPersonales? datos,
    bool? codigoConfirmado,
    String? rutaAnverso,
    String? rutaReverso,
    String? rutaSelfie,
    DatosLeidosDelDocumento? leido,
    String? origenDeFondos,
    String? actividadEconomica,
    double? montoMensualEstimado,
    bool? enviando,
    String? error,
  }) => EstadoAlta(
    paso: paso ?? this.paso,
    datos: datos ?? this.datos,
    codigoConfirmado: codigoConfirmado ?? this.codigoConfirmado,
    rutaAnverso: rutaAnverso ?? this.rutaAnverso,
    rutaReverso: rutaReverso ?? this.rutaReverso,
    rutaSelfie: rutaSelfie ?? this.rutaSelfie,
    leido: leido ?? this.leido,
    origenDeFondos: origenDeFondos ?? this.origenDeFondos,
    actividadEconomica: actividadEconomica ?? this.actividadEconomica,
    montoMensualEstimado: montoMensualEstimado ?? this.montoMensualEstimado,
    enviando: enviando ?? false,
    error: error,
  );
}

/// Sabe en qué paso está el alta, y nada más: ni pinta, ni llama a la red. Enviar el
/// alta completa a `POST /usuarios` queda pendiente del cliente Dart de identidad
/// (hueco declarado en el informe del carril) — `enviarAlServidor` se deja como
/// punto de extensión explícito en vez de simular una llamada.
class AltaNotifier extends Notifier<EstadoAlta> {
  @override
  EstadoAlta build() => const EstadoAlta();

  void actualizarDatos(DatosPersonales datos) =>
      state = state.copiarCon(datos: datos);

  void confirmarCelular() => state = state.copiarCon(codigoConfirmado: true);

  void capturarAnverso(String? ruta) =>
      state = state.copiarCon(rutaAnverso: ruta);

  void capturarReverso(String? ruta) =>
      state = state.copiarCon(rutaReverso: ruta);

  void capturarSelfie(String? ruta) =>
      state = state.copiarCon(rutaSelfie: ruta);

  void actualizarPerfilTransaccional({
    required String origen,
    required String actividad,
    required double? monto,
  }) => state = state.copiarCon(
    origenDeFondos: origen,
    actividadEconomica: actividad,
    montoMensualEstimado: monto,
  );

  void siguiente() {
    const orden = PasoAlta.values;
    final i = orden.indexOf(state.paso);
    if (i < orden.length - 1) state = state.copiarCon(paso: orden[i + 1]);
  }

  void atras() {
    const orden = PasoAlta.values;
    final i = orden.indexOf(state.paso);
    if (i > 0) state = state.copiarCon(paso: orden[i - 1]);
  }

  /// Punto de extensión: lo completa quien conecte el cliente de `identidad`.
  Future<void> enviarAlServidor() {
    throw UnimplementedError(
      'CU-01: falta el cliente Dart de identidad (clientes/dart) — hueco '
      'declarado en planes/informes/carril-M1.md §3.',
    );
  }
}

final altaProvider = NotifierProvider<AltaNotifier, EstadoAlta>(
  AltaNotifier.new,
);
