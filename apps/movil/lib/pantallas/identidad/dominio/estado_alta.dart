import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'capturas_del_expediente.dart';
import 'datos_del_alta.dart';
import 'envio_del_alta.dart';
import 'modelo_del_alta.dart';
import 'verificacion_correo.dart';

export 'datos_del_alta.dart' show DatosLeidosDelDocumento, DatosPersonales;
export 'modelo_del_alta.dart';
export 'capturas_del_expediente.dart' show CaraDelCarril, CapturasDelExpediente;

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
    state = state.copiarCon(datos: datos, limpiarVerificacion: cambioCorreo);
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
