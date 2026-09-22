import 'modelo.dart';

/// **El progreso como JSON.** Está aparte del modelo a propósito: el modelo es lo que
/// el motor entiende, y esto es cómo viaja al almacén. El día que el backend exponga
/// el contrato de §11, estas dos funciones son lo único que cambia de forma.
Map<String, Object?> progresoAJson(ProgresoDeTutorial p) => {
  'tutorialId': p.tutorialId,
  'version': p.version,
  'estado': p.estado.name,
  'pasoId': p.pasoId,
  'indice': p.indice,
  'iniciadoEn': p.iniciadoEn.toIso8601String(),
  'terminadoEn': p.terminadoEn?.toIso8601String(),
  'ultimaInteraccion': p.ultimaInteraccion.toIso8601String(),
  'repeticiones': p.repeticiones,
};

/// Devuelve `null` si la fila no tiene forma de progreso: se descarta lo ilegible en
/// vez de romper la pantalla. El avance de un tutorial no vale una app trabada.
ProgresoDeTutorial? progresoDesdeJson(Object? crudo) {
  if (crudo is! Map) return null;
  final tutorialId = crudo['tutorialId'];
  final version = crudo['version'];
  final indice = crudo['indice'];
  if (tutorialId is! String || version is! String || indice is! int) {
    return null;
  }
  final iniciadoEn = _fecha(crudo['iniciadoEn']);
  if (iniciadoEn == null) return null;
  return ProgresoDeTutorial(
    tutorialId: tutorialId,
    version: version,
    estado: _estado(crudo['estado']),
    pasoId: crudo['pasoId'] is String ? crudo['pasoId'] as String : null,
    indice: indice,
    iniciadoEn: iniciadoEn,
    terminadoEn: _fecha(crudo['terminadoEn']),
    ultimaInteraccion: _fecha(crudo['ultimaInteraccion']) ?? iniciadoEn,
    repeticiones: crudo['repeticiones'] is int
        ? crudo['repeticiones'] as int
        : 0,
  );
}

EstadoDeProgreso _estado(Object? crudo) => EstadoDeProgreso.values.firstWhere(
  (e) => e.name == crudo,
  orElse: () => EstadoDeProgreso.pendiente,
);

DateTime? _fecha(Object? crudo) =>
    crudo is String ? DateTime.tryParse(crudo) : null;
