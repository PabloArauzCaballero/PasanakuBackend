import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'cu01_registrar.dart';
import 'datos_del_alta.dart';

export 'datos_del_alta.dart' show DatosLeidosDelDocumento, DatosPersonales;
import 'estado_contrato.dart';
import 'estado_sesion.dart' show mensajeDeError;

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

/// Sabe en qué paso está el alta y lo manda al servidor al cerrarlo. No pinta: eso
/// es de los organismos de cada paso, y la petición la arma [Registro].
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

  /// CU-01 · `POST /usuarios`. Manda el alta completa en una sola petición, que es
  /// como la define el contrato: los ocho pasos juntan datos, y recién al final —con
  /// los contratos aceptados— se crea la persona.
  ///
  /// Devuelve el id de la cuenta de billetera que el backend abre junto al usuario,
  /// o `null` si el alta quedó pendiente de verificación sin billetera todavía.
  Future<String?> enviarAlServidor() async {
    if (state.enviando) return null;
    final d = state.datos;
    final nacimiento = d.fechaNacimiento;
    if (nacimiento == null) {
      state = state.copiarCon(error: 'Falta tu fecha de nacimiento.');
      return null;
    }
    state = state.copiarCon(enviando: true);
    try {
      final r = await ref
          .read(registroProvider)
          .crear(
            telefonoE164: d.telefono,
            nombres: d.nombres,
            apellidos: d.apellidos,
            fechaNacimiento: nacimiento,
            tipoDocumento: d.tipoDocumento,
            numeroDocumento: d.numeroDocumento,
            contratosAceptados: ref.read(contratoProvider).aceptados,
          );
      state = state.copiarCon();
      return r;
    } on Object catch (e) {
      state = state.copiarCon(error: mensajeDeError(e));
      return null;
    }
  }
}

final altaProvider = NotifierProvider<AltaNotifier, EstadoAlta>(
  AltaNotifier.new,
);
