import 'dart:convert';

import '../dominio/puertos/almacen_de_progreso.dart';
import '../dominio/puertos/almacen_seguro.dart';
import '../dominio/tutoriales/modelo.dart';
import '../dominio/tutoriales/serializacion.dart';

/// Una sola clave con todas las filas: son pocas y se leen juntas.
const String claveDeProgreso = 'tutoriales.progreso';

/// **El progreso en el teléfono**, sobre el mismo almacén seguro donde ya viven los
/// tokens ([AlmacenSeguro]).
///
/// Se reusa ese puerto y no se agrega `shared_preferences` por dos razones: una
/// dependencia menos, y que el almacén seguro se limpia junto con la sesión — el avance
/// de los tutoriales de una persona no queda en el teléfono después de que se fue.
///
/// Su límite es real y está declarado: el avance **no viaja a otro teléfono** hasta que
/// exista el adaptador remoto.
class AlmacenDeProgresoSeguro implements AlmacenDeProgreso {
  AlmacenDeProgresoSeguro(this._almacen);

  Future<void> _cola = Future<void>.value();

  final AlmacenSeguro _almacen;

  @override
  Future<List<ProgresoDeTutorial>> leer() async {
    final crudo = await _almacen.leer(claveDeProgreso);
    if (crudo == null || crudo.isEmpty) return const [];
    final Object? leido;
    try {
      leido = jsonDecode(crudo);
    } on FormatException {
      // Alguien dejó la clave a medias. Se descarta lo ilegible y se empieza de nuevo:
      // el avance de un tutorial no vale una pantalla rota.
      return const [];
    }
    if (leido is! List) return const [];
    return leido
        .map(progresoDesdeJson)
        .whereType<ProgresoDeTutorial>()
        .toList();
  }

  @override
  Future<void> guardar(ProgresoDeTutorial progreso) => _enOrden(() async {
    final filas = await _sin(progreso.tutorialId);
    await _escribir([...filas, progreso]);
  });

  @override
  Future<void> reiniciar(String tutorialId) =>
      _enOrden(() => _sin(tutorialId).then(_escribir));

  /// **Una escritura por vez.** Guardar es leer, quitar la fila vieja y escribir todo:
  /// dos de esas a la vez se pisan, y el último paso de un tutorial terminaba guardado
  /// como «en progreso» porque una escritura anterior llegaba tarde. La cola las
  /// encadena; el motor no se entera y sigue sin esperar a ninguna.
  Future<void> _enOrden(Future<void> Function() escritura) {
    final siguiente = _cola.then((_) => escritura());
    _cola = siguiente.catchError((Object _) {});
    return siguiente;
  }

  /// Lo guardado menos ese tutorial. Copia en vez de mutar: `leer()` devuelve una lista
  /// constante cuando no hay nada, y esa no se toca.
  Future<List<ProgresoDeTutorial>> _sin(String tutorialId) async =>
      (await leer()).where((p) => p.tutorialId != tutorialId).toList();

  Future<void> _escribir(List<ProgresoDeTutorial> filas) => _almacen.guardar(
    claveDeProgreso,
    jsonEncode(filas.map(progresoAJson).toList()),
  );
}
