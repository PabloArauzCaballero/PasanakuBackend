import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/errores.dart';
import '../../../proveedores/sesion.dart';
import 'capturas_del_expediente.dart';
import 'envio_del_alta.dart';
import 'modelo_alta.dart';
import 'verificacion_correo.dart';

/// Sabe en qué paso está el alta y lo manda al servidor al cerrarlo. No pinta: eso
/// es de los organismos de cada paso, y la petición la arma [Registro].
class AltaNotifier extends Notifier<EstadoAlta> {
  @override
  EstadoAlta build() {
    ref.watch(generacionDeSesionProvider);
    return const EstadoAlta();
  }

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
        codigoConfirmado: solicitud.confirmada,
      );
      return true;
    } on VerificacionCorreoException catch (e) {
      state = state.copiarCon(errorCodigo: e.mensaje, trazaId: e.trazaId);
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
      state = state.copiarCon(errorCodigo: e.mensaje, trazaId: e.trazaId);
    } on Object {
      state = state.copiarCon(errorCodigo: 'No pudimos comprobar el codigo.');
    }
  }

  Future<void> registrarCaptura(CaraDelCarril cara, Captura captura) async {
    final capturas = await state.capturas.reemplazarCaptura(cara, captura);
    state = state.copiarCon(capturas: capturas);
  }

  Future<void> quitarCaptura(CaraDelCarril cara) async {
    final anterior = state.capturas.porCara[cara];
    await anterior?.eliminarArchivoLocal();
    state = state.copiarCon(capturas: state.capturas.sinCaptura(cara));
  }

  /// Se llama cuando se abandona la pantalla de alta, no al transferir las fotos
  /// al seguimiento después de crear la cuenta.
  Future<void> abandonar() async {
    final capturas = state.capturas;
    state = const EstadoAlta();
    await capturas.eliminarArchivosLocales();
  }

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

  /// Vacía el asistente. Se llama al cerrar el alta: un formulario de seis pasos
  /// que sobrevive hace que el siguiente empiece con los datos del anterior.
  void reiniciar() => state = const EstadoAlta();

  void siguiente() {
    final orden = state.orden;
    final i = orden.indexOf(state.paso);
    if (i < orden.length - 1) state = state.copiarCon(paso: orden[i + 1]);
  }

  void atras() {
    // Quien saltó la verificación y vuelve desde sus datos reencuentra el paso del
    // carnet: puede cambiar de idea y verificarse ahora.
    if (state.sinVerificacion && state.paso == PasoAlta.datos) {
      state = state.copiarCon(
        paso: PasoAlta.documento,
        modo: ModoDelAlta.completo,
      );
      return;
    }
    final orden = state.orden;
    final i = orden.indexOf(state.paso);
    if (i > 0) state = state.copiarCon(paso: orden[i - 1]);
  }

  /// «Saltar por ahora» en el paso del carnet: la cuenta se crea sin verificar y la
  /// verificación se pide cuando la persona quiera operar con dinero. Las fotos que
  /// ya hubiera sacado se borran: no se van a subir.
  Future<void> saltarVerificacion() async {
    final capturas = state.capturas;
    state = state.copiarCon(
      modo: ModoDelAlta.sinVerificacion,
      paso: PasoAlta.datos,
      capturas: const CapturasDelExpediente(),
    );
    await capturas.eliminarArchivosLocales();
  }

  /// La verificación desde adentro de la app, para una cuenta ya creada.
  void empezarSoloVerificacion() => state = const EstadoAlta(
    paso: PasoAlta.documento,
    modo: ModoDelAlta.soloVerificacion,
  );

  /// CU-01 · `POST /usuarios`. La petición la arma [enviarElAlta]; acá solo vive el
  /// estado de envío, que es lo único que mira la pantalla.
  Future<ResultadoDelAlta> enviarAlServidor() async {
    if (state.enviando) return const ResultadoDelAlta();
    state = state.copiarCon(enviando: true);
    final r = await enviarElAlta(ref, state);
    state = state.copiarCon(
      paso: switch (r.campoError) {
        CampoDeErrorApi.contrasena => PasoAlta.cuenta,
        CampoDeErrorApi.fechaExpiracion => PasoAlta.documento,
        null => state.paso,
      },
      error: r.error,
      campoError: r.campoError,
      trazaId: r.trazaId,
    );
    return r;
  }
}

final altaProvider = NotifierProvider<AltaNotifier, EstadoAlta>(
  AltaNotifier.new,
);
