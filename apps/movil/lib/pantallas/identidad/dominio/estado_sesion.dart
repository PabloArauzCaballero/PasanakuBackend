import 'package:aportaya_diseno/errores.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../dominio/cierre_de_sesion.dart';
import '../../../dominio/errores.dart' as errores;
import '../../../proveedores/sesion.dart';
import 'cu04_autenticar.dart';

/// CU-04 — autenticar con MFA y registrar dispositivo. Tres pasos: credenciales,
/// código MFA, y la oferta de confiar en el dispositivo. Habla con el servidor a
/// través de [Autenticacion]; nunca arma una petición por su cuenta.
enum PasoSesion { credenciales, mfa, dispositivo }

class EstadoSesion {
  const EstadoSesion({
    this.paso = PasoSesion.credenciales,
    this.identificador = '',
    this.contrasena = '',
    this.codigoMfa = '',
    this.confiarEnDispositivo = false,
    this.enviando = false,
    this.error,
    this.trazaId,
    this.credencialesRechazadas = false,
    this.correoMfaEnviado = false,
  });

  final PasoSesion paso;
  final String identificador;
  final String contrasena;
  final String codigoMfa;
  final bool confiarEnDispositivo;
  final bool enviando;
  final String? error;
  final String? trazaId;
  final bool credencialesRechazadas;
  final bool correoMfaEnviado;

  EstadoSesion copiarCon({
    PasoSesion? paso,
    String? identificador,
    String? contrasena,
    String? codigoMfa,
    bool? confiarEnDispositivo,
    bool? enviando,
    String? error,
    String? trazaId,
    bool? credencialesRechazadas,
    bool? correoMfaEnviado,
  }) => EstadoSesion(
    paso: paso ?? this.paso,
    identificador: identificador ?? this.identificador,
    contrasena: contrasena ?? this.contrasena,
    codigoMfa: codigoMfa ?? this.codigoMfa,
    confiarEnDispositivo: confiarEnDispositivo ?? this.confiarEnDispositivo,
    enviando: enviando ?? false,
    error: error,
    trazaId: trazaId,
    credencialesRechazadas:
        credencialesRechazadas ?? this.credencialesRechazadas,
    correoMfaEnviado: correoMfaEnviado ?? this.correoMfaEnviado,
  );
}

class SesionIdentidadNotifier extends Notifier<EstadoSesion> {
  @override
  EstadoSesion build() {
    ref.watch(generacionDeSesionProvider);
    return const EstadoSesion();
  }

  void actualizarCredenciales({String? identificador, String? contrasena}) =>
      state = state.copiarCon(
        identificador: identificador,
        contrasena: contrasena,
        credencialesRechazadas: false,
      );

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
          identificador: state.identificador,
          credencial: state.contrasena,
        ),
    credenciales: true,
  );

  /// El segundo paso: el código que llegó por SMS o app de autenticación.
  Future<bool> completarMfa(String codigo) {
    state = state.copiarCon(codigoMfa: codigo);
    return _intentar(
      () => ref
          .read(autenticacionProvider)
          .conFactor(
            identificador: state.identificador,
            credencial: state.contrasena,
            codigo: codigo,
          ),
    );
  }

  /// Revalida las credenciales y solicita una emisión nueva al correo confirmado;
  /// el destino nunca se acepta desde la app.
  Future<bool> reenviarCodigoMfa() {
    state = state.copiarCon(correoMfaEnviado: false);
    return _intentar(
      () => ref
          .read(autenticacionProvider)
          .conCredenciales(
            identificador: state.identificador,
            credencial: state.contrasena,
          ),
      credenciales: true,
    );
  }

  Future<bool> _intentar(
    Future<ResultadoDeAutenticacion> Function() llamada, {
    bool credenciales = false,
  }) async {
    if (state.enviando) return false;
    state = state.copiarCon(enviando: true, credencialesRechazadas: false);
    try {
      final resultado = await llamada();
      state = state.copiarCon(
        paso:
            resultado == ResultadoDeAutenticacion.faltaSegundoFactor ||
                resultado == ResultadoDeAutenticacion.factorEnviadoPorCorreo
            ? PasoSesion.mfa
            : PasoSesion.dispositivo,
        credencialesRechazadas: false,
        correoMfaEnviado:
            resultado == ResultadoDeAutenticacion.factorEnviadoPorCorreo,
      );
      return true;
    } on Object catch (e) {
      state = state.copiarCon(
        error: mensajeDeError(e),
        trazaId: e is ErrorPresentable ? e.trazaId : null,
        credencialesRechazadas:
            credenciales && e is errores.ErrorDeApi && e.codigo == 'AP-CU04-01',
      );
      return false;
    }
  }

  void elegirConfianza(bool confiar) =>
      state = state.copiarCon(confiarEnDispositivo: confiar);

  /// Borra los tokens y deja el estado como recién abierto: quien cierra sesión no
  /// puede quedarse con su teléfono escrito en el formulario del siguiente.
  Future<void> cerrarSesion() async {
    await ref.read(cierreDeSesionProvider).ejecutar();
    state = const EstadoSesion();
  }
}

/// El texto que ve una persona. Los errores de dominio ya traen su mensaje en voz de
/// marca; cualquier otra cosa cae en una frase que no culpa a quien está mirando.
String mensajeDeError(Object e, {String? mensajeDesconocido}) {
  final problema = errorDeDominio(e);
  if (problema is ErrorPresentable) return problema.mensaje;
  return mensajeDesconocido ??
      'No pudimos completar el ingreso. Probá de nuevo en un momento.';
}

String? trazaDeError(Object e) {
  final problema = errorDeDominio(e);
  return problema is ErrorPresentable ? problema.trazaId : null;
}

final sesionIdentidadProvider =
    NotifierProvider<SesionIdentidadNotifier, EstadoSesion>(
      SesionIdentidadNotifier.new,
    );
