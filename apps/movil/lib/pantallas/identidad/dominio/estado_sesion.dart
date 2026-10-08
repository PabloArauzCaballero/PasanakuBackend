import 'package:aportaya_diseno/errores.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../proveedores/sesion.dart';
import 'cu04_autenticar.dart';

/// CU-04 — autenticar con MFA. El servidor decide cuándo pedir el factor y la
/// app no ofrece una confianza de dispositivo que no puede registrar.
enum PasoSesion { credenciales, mfa, autenticada }

class EstadoSesion {
  const EstadoSesion({
    this.paso = PasoSesion.credenciales,
    this.telefono = '',
    this.contrasena = '',
    this.codigoMfa = '',
    this.enviando = false,
    this.error,
  });

  final PasoSesion paso;
  final String telefono;
  final String contrasena;
  final String codigoMfa;
  final bool enviando;
  final String? error;

  EstadoSesion copiarCon({
    PasoSesion? paso,
    String? telefono,
    String? contrasena,
    String? codigoMfa,
    bool? enviando,
    String? error,
  }) => EstadoSesion(
    paso: paso ?? this.paso,
    telefono: telefono ?? this.telefono,
    contrasena: contrasena ?? this.contrasena,
    codigoMfa: codigoMfa ?? this.codigoMfa,
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
  /// —`mfa` si el servidor lo pidió, `autenticada` si ya hay sesión— o con el error
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
      final autenticada = resultado == ResultadoDeAutenticacion.entro;
      state = state.copiarCon(
        paso: autenticada ? PasoSesion.autenticada : PasoSesion.mfa,
        telefono: autenticada ? '' : state.telefono,
        contrasena: autenticada ? '' : state.contrasena,
        codigoMfa: '',
      );
      return true;
    } on Object catch (e) {
      state = state.copiarCon(codigoMfa: '', error: mensajeDeError(e));
      return false;
    }
  }

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
