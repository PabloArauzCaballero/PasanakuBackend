/// El código de correo pedido antes del alta, tal como se guarda en el teléfono para
/// retomar el alta si la persona cierra la app. Separado de `verificacion_correo.dart`
/// para que ningún archivo pase de 200 líneas.
library;

class VerificacionSolicitada {
  const VerificacionSolicitada({
    required this.id,
    required this.destino,
    this.confirmada = false,
  });
  final String id;
  final String destino;

  /// `true` cuando la persona ya había confirmado el código antes de cerrar la app.
  final bool confirmada;
}

class VerificacionGuardada {
  const VerificacionGuardada({
    required this.correo,
    required this.clave,
    required this.id,
    required this.destino,
    required this.expiraEn,
    required this.confirmada,
  });

  final String correo;
  final String clave;
  final String id;
  final String destino;
  final DateTime expiraEn;
  final bool confirmada;

  VerificacionSolicitada comoSolicitada() =>
      VerificacionSolicitada(id: id, destino: destino, confirmada: confirmada);

  VerificacionGuardada confirmar() => VerificacionGuardada(
    correo: correo,
    clave: clave,
    id: id,
    destino: destino,
    expiraEn: expiraEn,
    confirmada: true,
  );

  Map<String, Object> aJson() => {
    'correo': correo,
    'clave': clave,
    'id': id,
    'destino': destino,
    'expiraEn': expiraEn.toUtc().toIso8601String(),
    'confirmada': confirmada,
  };

  static VerificacionGuardada? desdeJson(Object? json) {
    if (json is! Map) return null;
    final expira = DateTime.tryParse('${json['expiraEn']}');
    final correo = json['correo'], clave = json['clave'];
    final id = json['id'], destino = json['destino'];
    if (expira == null ||
        correo is! String ||
        clave is! String ||
        id is! String ||
        destino is! String) {
      return null;
    }
    return VerificacionGuardada(
      correo: correo,
      clave: clave,
      id: id,
      destino: destino,
      expiraEn: expira,
      confirmada: json['confirmada'] == true,
    );
  }
}
