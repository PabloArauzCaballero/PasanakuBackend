/// **El catálogo se valida, no se confía.** Un tutorial mal escrito se descubre acá y
/// en la prueba del catálogo, no cuando alguien lo abre. Texto puro, sin Flutter.
library;

import 'modelo.dart';

enum CodigoDeProblema {
  idDuplicado,
  tutorialVacio,
  pasoDuplicado,
  pasoSinAncla,
  ordenIncorrecto,
  rutaInexistente,
  requisitoInexistente,
  siguienteInexistente,
  ciclo,
}

class ProblemaDeCatalogo {
  const ProblemaDeCatalogo(this.codigo, this.tutorialId, this.detalle);
  final CodigoDeProblema codigo;
  final String tutorialId;
  final String detalle;

  @override
  String toString() => '$tutorialId: $detalle';
}

/// Revisa el catálogo entero y devuelve TODO lo que está mal, no lo primero.
List<ProblemaDeCatalogo> validarCatalogo(
  List<TutorialDefinicion> tutoriales, {
  List<String> rutas = const [],
}) {
  final problemas = <ProblemaDeCatalogo>[];
  final vistos = <String>{};
  for (final t in tutoriales) {
    if (!vistos.add(t.id)) {
      problemas.add(
        ProblemaDeCatalogo(
          CodigoDeProblema.idDuplicado,
          t.id,
          'el id "${t.id}" está declarado dos veces',
        ),
      );
    }
    problemas.addAll(_validarUno(t, rutas));
  }
  problemas.addAll(_validarReferencias(tutoriales, vistos));
  problemas.addAll(_validarCiclos(tutoriales));
  return problemas;
}

List<ProblemaDeCatalogo> _validarUno(TutorialDefinicion t, List<String> rutas) {
  final problemas = <ProblemaDeCatalogo>[];
  void anotar(CodigoDeProblema c, String detalle) =>
      problemas.add(ProblemaDeCatalogo(c, t.id, detalle));

  if (t.pasos.isEmpty) {
    anotar(
      CodigoDeProblema.tutorialVacio,
      'un tutorial sin pasos no enseña nada',
    );
  }
  if (t.ruta != null && !_conoce(rutas, t.ruta!)) {
    anotar(
      CodigoDeProblema.rutaInexistente,
      'la ruta "${t.ruta}" no existe en la app',
    );
  }

  final ids = <String>{};
  var ordenAnterior = -1 << 30;
  for (final p in t.pasos) {
    if (!ids.add(p.id)) {
      anotar(
        CodigoDeProblema.pasoDuplicado,
        'el paso "${p.id}" está dos veces',
      );
    }
    if (p.ancla == null && p.accion.tipo != TipoDeAccion.navegar) {
      anotar(
        CodigoDeProblema.pasoSinAncla,
        'el paso "${p.id}" no resalta nada ni pide navegar',
      );
    }
    if (p.ruta != null && !_conoce(rutas, p.ruta!)) {
      anotar(
        CodigoDeProblema.rutaInexistente,
        'el paso "${p.id}" apunta a "${p.ruta}", que no existe',
      );
    }
    if (p.orden != null) {
      if (p.orden! <= ordenAnterior) {
        anotar(
          CodigoDeProblema.ordenIncorrecto,
          'el paso "${p.id}" declara orden ${p.orden} después de $ordenAnterior',
        );
      }
      ordenAnterior = p.orden!;
    }
  }
  return problemas;
}

List<ProblemaDeCatalogo> _validarReferencias(
  List<TutorialDefinicion> tutoriales,
  Set<String> ids,
) {
  final problemas = <ProblemaDeCatalogo>[];
  for (final t in tutoriales) {
    for (final r in t.requisitos) {
      if (!ids.contains(r)) {
        problemas.add(
          ProblemaDeCatalogo(
            CodigoDeProblema.requisitoInexistente,
            t.id,
            'el requisito "$r" no existe',
          ),
        );
      }
    }
    if (t.siguiente != null && !ids.contains(t.siguiente)) {
      problemas.add(
        ProblemaDeCatalogo(
          CodigoDeProblema.siguienteInexistente,
          t.id,
          'el siguiente "${t.siguiente}" no existe',
        ),
      );
    }
  }
  return problemas;
}

/// Ciclos por requisitos: `A` exige `B` y `B` exige `A` deja a las dos tarjetas
/// diciendo «antes hacé la otra» para siempre.
List<ProblemaDeCatalogo> _validarCiclos(List<TutorialDefinicion> tutoriales) {
  final porId = {for (final t in tutoriales) t.id: t};
  final problemas = <ProblemaDeCatalogo>[];
  final resueltos = <String>{};
  final camino = <String>{};

  void recorrer(String id) {
    if (resueltos.contains(id)) return;
    if (camino.contains(id)) {
      problemas.add(
        ProblemaDeCatalogo(
          CodigoDeProblema.ciclo,
          id,
          '"$id" es requisito de sí mismo vía ${camino.join(' → ')}',
        ),
      );
      return;
    }
    camino.add(id);
    for (final r in porId[id]?.requisitos ?? const <String>[]) {
      if (porId.containsKey(r)) recorrer(r);
    }
    camino.remove(id);
    resueltos.add(id);
  }

  for (final t in tutoriales) {
    recorrer(t.id);
  }
  return problemas;
}

/// Una ruta conocida vale como prefijo: `/pasanaku/mi-estado` la cubre `/pasanaku`.
bool _conoce(List<String> rutas, String ruta) {
  if (rutas.isEmpty) return true;
  final limpia = ruta.split('?').first;
  return rutas.any((r) => limpia == r || limpia.startsWith('$r/'));
}
