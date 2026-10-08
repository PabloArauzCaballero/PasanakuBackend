import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'capturas_del_expediente.dart';
import 'datos_del_alta.dart';
import 'envio_del_alta.dart';
import 'verificacion_correo.dart';

export 'datos_del_alta.dart' show DatosLeidosDelDocumento, DatosPersonales;
export 'capturas_del_expediente.dart' show CaraDelCarril, CapturasDelExpediente;

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

/// Sabe en qué paso está el alta y lo manda al servidor al cerrarlo. No pinta: eso
/// es de los organismos de cada paso, y la petición la arma [Registro].
class AltaNotifier extends Notifier<EstadoAlta> {
  @override
  EstadoAlta build() => const EstadoAlta();

  void actualizarDatos(DatosPersonales datos) {
    final cambioCorreo =
        state.datos.correo.trim().toLowerCase() !=
        datos.correo.trim().toLowerCase();
    if (cambioCorreo) ref.read(verificacionCorreoProvider).reiniciar();
    state = state.copiarCon(
      datos: datos,
      limpiarVerificacion: cambioCorreo,
    );
  }

  void elegirContrasena(String clave) =>
      state = state.copiarCon(contrasena: clave);

  void confirmarCelular() => state = state.copiarCon(codigoConfirmado: true);

  Future<bool> solicitarCodigoCorreo({bool nuevo = false}) async {
    if (state.enviandoCodigo) return false;
    if (!nuevo && state.verificacionCorreoId != null) return true;
    state = state.copiarCon(enviandoCodigo: true);
    try {
      final solicitud = await ref
          .read(verificacionCorreoProvider)
          .solicitar(state.datos.correo, nueva: nuevo);
      state = state.copiarCon(
        verificacionCorreoId: solicitud.id,
        destinoVerificacion: solicitud.destino,
        codigoConfirmado: false,
      );
      return true;
    } on VerificacionCorreoException catch (e) {
      state = state.copiarCon(errorCodigo: e.mensaje);
      return false;
    } on Object {
      state = state.copiarCon(
        errorCodigo: 'No pudimos enviar el codigo. Intenta nuevamente.',
      );
      return false;
    }
  }

  Future<void> confirmarCodigoCorreo(String codigo) async {
    final id = state.verificacionCorreoId;
    if (id == null || state.verificandoCodigo) return;
    state = state.copiarCon(verificandoCodigo: true);
    try {
      await ref
          .read(verificacionCorreoProvider)
          .confirmar(id: id, correo: state.datos.correo, codigo: codigo);
      state = state.copiarCon(codigoConfirmado: true);
    } on VerificacionCorreoException catch (e) {
      state = state.copiarCon(errorCodigo: e.mensaje);
    } on Object {
      state = state.copiarCon(errorCodigo: 'No pudimos comprobar el codigo.');
    }
  }

  void registrarCaptura(CaraDelCarril cara, Captura captura) => state = state
      .copiarCon(capturas: state.capturas.conCaptura(cara, captura));

  void quitarCaptura(CaraDelCarril cara) =>
      state = state.copiarCon(capturas: state.capturas.sinCaptura(cara));

  void actualizarPerfilTransaccional({
    required String origen,
    required String detalleDelOrigen,
    required String actividad,
    required String detalleDeLaActividad,
    required double? monto,
  }) => state = state.copiarCon(
    origenDeFondos: origen,
    detalleDelOrigen: detalleDelOrigen,
    actividadEconomica: actividad,
    detalleDeLaActividad: detalleDeLaActividad,
    montoMensualEstimado: monto,
  );

  /// Vacía el asistente. Se llama al cerrar el alta: el estado de un formulario de
  /// siete pasos que sobrevive al final del formulario hace que el siguiente empiece
  /// con los datos del anterior.
  void reiniciar() => state = const EstadoAlta();

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

  /// CU-01 · `POST /usuarios`. La petición la arma [enviarElAlta]; acá solo vive el
  /// estado —«enviando», «falló y por qué»—, que es lo único que mira la pantalla.
  /// Ya no sube las fotos: eso pasa en la pantalla de subida, con el `usuarioId`
  /// que devuelve este llamado (ver `seguimiento_del_alta.dart`).
  Future<ResultadoDelAlta> enviarAlServidor() async {
    if (state.enviando) return const ResultadoDelAlta();
    state = state.copiarCon(enviando: true);
    final r = await enviarElAlta(ref, state);
    state = state.copiarCon(error: r.error);
    return r;
  }
}

final altaProvider = NotifierProvider<AltaNotifier, EstadoAlta>(
  AltaNotifier.new,
);
