import '../../dominio/tutoriales/avance.dart';
import '../../dominio/tutoriales/modelo.dart';

/// Los filtros del centro de ayuda.
enum FiltroDeEstado { todos, pendiente, enProgreso, completado }

/// Una tarjeta ya resuelta: el tutorial más lo que le pasó a esta persona.
class TutorialEnLista {
  const TutorialEnLista({
    required this.tutorial,
    required this.estado,
    required this.fraccion,
    required this.continuable,
    required this.progreso,
    required this.requisitosPendientes,
  });

  final TutorialDefinicion tutorial;
  final EstadoDeProgreso estado;
  final double fraccion;
  final bool continuable;
  final ProgresoDeTutorial? progreso;
  final List<TutorialDefinicion> requisitosPendientes;
}

/// **Lo que el centro de ayuda muestra, calculado sin pintar nada.**
///
/// Función pura sobre tres entradas —catálogo, avance y filtro— así que el orden de la
/// lista y el texto de cada botón se prueban con listas literales, sin levantar la app.
List<TutorialEnLista> componerLista(
  List<TutorialDefinicion> tutoriales,
  Map<String, ProgresoDeTutorial> progresos, {
  String texto = '',
  FiltroDeEstado filtro = FiltroDeEstado.todos,
  String? categoria,
}) {
  final porId = {for (final t in tutoriales) t.id: t};
  final lista = tutoriales
      .map(
        (t) => TutorialEnLista(
          tutorial: t,
          estado: estadoDe(progresos[t.id], t),
          fraccion: fraccionDe(progresos[t.id], t),
          continuable: sePuedeContinuar(progresos[t.id], t),
          progreso: progresos[t.id],
          requisitosPendientes: requisitosPendientes(t, porId, progresos),
        ),
      )
      .where((f) => _coincide(f, texto, filtro, categoria))
      .toList();
  lista.sort((a, b) => _peso(a).compareTo(_peso(b)));
  return lista;
}

/// El que conviene hacer ahora: el primero sin hacer y sin requisitos pendientes.
/// `null` cuando no queda nada, que también es una respuesta.
TutorialEnLista? recomendado(List<TutorialEnLista> lista) {
  for (final f in lista) {
    if (f.estado != EstadoDeProgreso.completado &&
        f.requisitosPendientes.isEmpty) {
      return f;
    }
  }
  return null;
}

bool _coincide(
  TutorialEnLista fila,
  String texto,
  FiltroDeEstado filtro,
  String? categoria,
) {
  if (categoria != null && fila.tutorial.categoria != categoria) return false;
  if (!_coincideEstado(fila, filtro)) return false;
  final busqueda = texto.trim().toLowerCase();
  if (busqueda.isEmpty) return true;
  final heno =
      '${fila.tutorial.titulo} ${fila.tutorial.descripcion} ${fila.tutorial.categoria}'
          .toLowerCase();
  return busqueda.split(RegExp(r'\s+')).every(heno.contains);
}

bool _coincideEstado(TutorialEnLista fila, FiltroDeEstado filtro) =>
    switch (filtro) {
      FiltroDeEstado.todos => true,
      // Omitido cuenta como pendiente: se dejó a medias y sigue sin hacerse.
      FiltroDeEstado.pendiente =>
        fila.estado == EstadoDeProgreso.pendiente ||
            fila.estado == EstadoDeProgreso.omitido,
      FiltroDeEstado.enProgreso => fila.estado == EstadoDeProgreso.enProgreso,
      FiltroDeEstado.completado => fila.estado == EstadoDeProgreso.completado,
    };

/// Primero lo importante, después lo que está a medias, después lo pendiente, y al
/// final lo ya hecho: el orden en el que a uno le sirve encontrarlo.
int _peso(TutorialEnLista fila) {
  if (fila.estado == EstadoDeProgreso.completado) return 4;
  if (fila.tutorial.obligatorio) return 0;
  if (fila.estado == EstadoDeProgreso.enProgreso) return 1;
  return fila.estado == EstadoDeProgreso.omitido ? 3 : 2;
}
