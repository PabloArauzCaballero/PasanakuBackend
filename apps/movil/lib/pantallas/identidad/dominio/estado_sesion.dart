import 'package:aportaya_diseno/errores.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../proveedores/sesion.dart';
import 'cu04_autenticar.dart';

/// CU-04 — autenticar con MFA y registrar dispositivo. Tres pasos: credenciales,
/// código MFA, y la oferta de confiar en el dispositivo. Habla con el servidor a
/// través de [Autenticacion]; nunca arma una petición por su cuenta.
enum PasoSesion { credenciales, mfa, dispositivo }

class EstadoSesion {
  const EstadoSesion({
    this.paso = PasoSesion.credenciales,
    this.telefono = '',
    this.contrasena = '',
    this.codigoMfa = '',
    this.confiarEnDispositivo = false,
    this.enviando = false,
    this.error,
  });

  final PasoSesion paso;
  final String telefono;
  final String contrasena;
  final String codigoMfa;
  final bool confiarEnDispositivo;
  final bool enviando;
  final String? error;

  EstadoSesion copiarCon({
    PasoSesion? paso,
    String? telefono,
    String? contrasena,
    String? codigoMfa,
    bool? confiarEnDispositivo,
    bool? enviando,
    String? error,
  }) => EstadoSesion(
    paso: paso ?? this.paso,
    telefono: telefono ?? this.telefono,
    contrasena: contrasena ?? this.contrasena,
    codigoMfa: codigoMfa ?? this.codigoMfa,
    confiarEnDispositivo: confiarEnDispositivo ?? this.confiarEnDispositivo,
    enviando: enviando ?? false,
    error: error,
  );
}

class SesionIdentidadNotifier extends Notifier<EstadoSesion> {
  @override
  EstadoSesion build() => const EstadoSesion();

  void actualizarCredenciales({String? telefono, String? contrasena}) =>
      state = state.copiarCon(telefono: telefono, contrasena: contrasena);

  /// Manda las credenciales al servidor. La pantalla **no decide** si hace falta un
  /// segundo factor: eso lo dice la respuesta (invariante 7).
  ///
  /// Devuelve `true` si el flujo avanzó. El estado queda con el paso que corresponde
  /// —`mfa` si el servidor lo pidió, `dispositivo` si ya hay sesión— o con el error
  /// traducido a voz de marca si algo falló.
  Future<bool> enviarCredenciales() => _intentar(
    () => ref
        .read(autenticacionProvider)
        .conCredenciales(
          telefonoE164: state.telefono,
          credencial: state.contrasena,
        ),
  );

  /// El segundo paso: el código que llegó por SMS o app de autenticación.
  Future<bool> completarMfa(String codigo) {
    state = state.copiarCon(codigoMfa: codigo);
    return _intentar(
      () => ref
          .read(autenticacionProvider)
          .conFactor(
            telefonoE164: state.telefono,
            credencial: state.contrasena,
            codigo: codigo,
          ),
    );
  }

  Future<bool> _intentar(
    Future<ResultadoDeAutenticacion> Function() llamada,
  ) async {
    if (state.enviando) return false;
    state = state.copiarCon(enviando: true);
    try {
      final resultado = await llamada();
      state = state.copiarCon(
        paso: resultado == ResultadoDeAutenticacion.faltaSegundoFactor
            ? PasoSesion.mfa
            : PasoSesion.dispositivo,
      );
      return true;
    } on Object catch (e) {
      state = state.copiarCon(error: mensajeDeError(e));
      return false;
    }
  }

  void elegirConfianza(bool confiar) =>
      state = state.copiarCon(confiarEnDispositivo: confiar);

  /// Borra los tokens y deja el estado como recién abierto: quien cierra sesión no
  /// puede quedarse con su teléfono escrito en el formulario del siguiente.
  Future<void> cerrarSesion() async {
    await ref.read(sesionProvider).cerrar();
    state = const EstadoSesion();
  }
}

/// El texto que ve una persona. Los errores de dominio ya traen su mensaje en voz de
/// marca; cualquier otra cosa cae en una frase que no culpa a quien está mirando.
String mensajeDeError(Object e) => e is ErrorPresentable
    ? e.mensaje
    : 'No pudimos completar el ingreso. Probá de nuevo en un momento.';

final sesionIdentidadProvider =
    NotifierProvider<SesionIdentidadNotifier, EstadoSesion>(
      SesionIdentidadNotifier.new,
    );
