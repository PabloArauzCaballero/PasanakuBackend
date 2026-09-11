import 'package:flutter_riverpod/flutter_riverpod.dart';

/// CU-03 — declaración PEP y beneficiario final. Una pregunta binaria y, si es
/// afirmativa, el detalle del cargo; la debida diligencia reforzada la decide el
/// servidor (flujo alternativo 6a de CU-01), esta pantalla solo declara.
class EstadoPep {
  const EstadoPep({this.esPep = false, this.detalle = ''});
  final bool esPep;
  final String detalle;

  EstadoPep copiarCon({bool? esPep, String? detalle}) =>
      EstadoPep(esPep: esPep ?? this.esPep, detalle: detalle ?? this.detalle);
}

class PepNotifier extends Notifier<EstadoPep> {
  @override
  EstadoPep build() => const EstadoPep();

  void declarar({required bool esPep, String detalle = ''}) =>
      state = EstadoPep(esPep: esPep, detalle: detalle);
}

final pepProvider = NotifierProvider<PepNotifier, EstadoPep>(PepNotifier.new);

/// CU-07 — perfil. Campos editables mínimos que declara la maqueta (D-1 no agrega
/// más que nombre de contacto y correo; el resto de la identidad es inmutable tras
/// el KYC, así que no se ofrece acá).
class EstadoPerfil {
  const EstadoPerfil({this.correo = '', this.enviando = false, this.error});
  final String correo;
  final bool enviando;
  final String? error;

  EstadoPerfil copiarCon({String? correo, bool? enviando, String? error}) =>
      EstadoPerfil(
        correo: correo ?? this.correo,
        enviando: enviando ?? false,
        error: error,
      );
}

class PerfilNotifier extends Notifier<EstadoPerfil> {
  @override
  EstadoPerfil build() => const EstadoPerfil();

  void actualizarCorreo(String correo) =>
      state = state.copiarCon(correo: correo);

  Future<void> guardar() {
    throw UnimplementedError(
      'CU-07: falta el cliente Dart de identidad — hueco declarado en '
      'planes/informes/carril-M1.md §3.',
    );
  }
}

final perfilProvider = NotifierProvider<PerfilNotifier, EstadoPerfil>(
  PerfilNotifier.new,
);

/// CU-09 — cambiar credenciales y solicitar la baja.
class EstadoContrasena {
  const EstadoContrasena({
    this.actual = '',
    this.nueva = '',
    this.enviando = false,
    this.error,
  });
  final String actual;
  final String nueva;
  final bool enviando;
  final String? error;

  EstadoContrasena copiarCon({
    String? actual,
    String? nueva,
    bool? enviando,
    String? error,
  }) => EstadoContrasena(
    actual: actual ?? this.actual,
    nueva: nueva ?? this.nueva,
    enviando: enviando ?? false,
    error: error,
  );
}

class ContrasenaNotifier extends Notifier<EstadoContrasena> {
  @override
  EstadoContrasena build() => const EstadoContrasena();

  void actualizar({String? actual, String? nueva}) =>
      state = state.copiarCon(actual: actual, nueva: nueva);

  Future<void> confirmar() {
    throw UnimplementedError(
      'CU-09: falta el cliente Dart de identidad — hueco declarado en '
      'planes/informes/carril-M1.md §3.',
    );
  }
}

final contrasenaProvider =
    NotifierProvider<ContrasenaNotifier, EstadoContrasena>(
      ContrasenaNotifier.new,
    );

/// CU-09 — baja de cuenta: confirmación explícita, sin recolectar motivo (la
/// maqueta no pide uno; declarado como supuesto en el informe).
class BajaNotifier extends Notifier<bool> {
  @override
  bool build() => false; // confirmando

  Future<void> confirmarBaja() {
    throw UnimplementedError(
      'CU-09: falta el cliente Dart de identidad — hueco declarado en '
      'planes/informes/carril-M1.md §3.',
    );
  }
}

final bajaProvider = NotifierProvider<BajaNotifier, bool>(BajaNotifier.new);
