import 'package:flutter_riverpod/flutter_riverpod.dart';

/// CU-04 — autenticar con MFA y registrar dispositivo. Tres pasos: credenciales,
/// código MFA, y la oferta de confiar en el dispositivo. Igual que `AltaNotifier`,
/// no llama a la red por sí mismo: eso es del cliente de identidad (hueco).
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

  /// Valida solo el formato (ayuda); el servidor decide si la contraseña es
  /// correcta y si hace falta MFA (invariante 7).
  void enviarCredenciales() => state = state.copiarCon(paso: PasoSesion.mfa);

  void completarMfa(String codigo) =>
      state = state.copiarCon(codigoMfa: codigo, paso: PasoSesion.dispositivo);

  void elegirConfianza(bool confiar) =>
      state = state.copiarCon(confiarEnDispositivo: confiar);

  /// Punto de extensión — ver `AltaNotifier.enviarAlServidor`.
  Future<void> iniciarSesion() {
    throw UnimplementedError(
      'CU-04: falta el cliente Dart de identidad — hueco declarado en '
      'planes/informes/carril-M1.md §3.',
    );
  }
}

final sesionIdentidadProvider =
    NotifierProvider<SesionIdentidadNotifier, EstadoSesion>(
      SesionIdentidadNotifier.new,
    );
