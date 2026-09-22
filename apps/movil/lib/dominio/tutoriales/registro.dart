import 'modelo.dart';

/// **El registro: qué tutoriales existen y cuáles le sirven a quien está mirando.**
///
/// Filtra por capacidad, nunca al revés: el tutorial no abre nada, solo señala lo que
/// la app ya muestra. Lo peor que puede pasar si el catálogo estuviera mal es que le
/// señalen a alguien un botón que el servidor no le deja usar.
class RegistroDeTutoriales {
  const RegistroDeTutoriales(this.todos, this.capacidades);

  final List<TutorialDefinicion> todos;
  final Set<Capacidad> capacidades;

  /// Los que esta persona puede ver, en el orden del catálogo.
  List<TutorialDefinicion> get disponibles => todos
      .where((t) => t.requiere.every(capacidades.contains))
      .toList(growable: false);

  Map<String, TutorialDefinicion> get porId => {
    for (final t in disponibles) t.id: t,
  };

  /// Las categorías presentes, en el orden en que aparecen.
  List<String> get categorias =>
      {for (final t in disponibles) t.categoria}.toList(growable: false);

  TutorialDefinicion? buscar(String id) => porId[id];

  /// El tutorial de una pantalla: el de la ruta más específica con la que la ubicación
  /// empieza. Es lo que enciende el acceso «¿Cómo funciona esta pantalla?».
  TutorialDefinicion? paraLaRuta(String ubicacion) {
    final limpia = ubicacion.split('?').first;
    final candidatos = disponibles
        .where(
          (t) =>
              t.ruta != null &&
              (limpia == t.ruta || limpia.startsWith('${t.ruta}/')),
        )
        .toList();
    if (candidatos.isEmpty) return null;
    candidatos.sort((a, b) => b.ruta!.length.compareTo(a.ruta!.length));
    return candidatos.first;
  }
}
