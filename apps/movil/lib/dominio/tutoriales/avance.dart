/// **Las cuentas del avance, sin estado y sin IO.** Todo lo que el centro de ayuda
/// muestra sale de acá, así se prueba con listas literales y sin levantar la app.
library;

import 'modelo.dart';

/// El progreso recién nacido de un tutorial que alguien acaba de abrir.
ProgresoDeTutorial progresoInicial(
  TutorialDefinicion t,
  DateTime ahora, {
  int repeticiones = 0,
}) => ProgresoDeTutorial(
  tutorialId: t.id,
  version: t.version,
  estado: EstadoDeProgreso.enProgreso,
  pasoId: t.pasos.isEmpty ? null : t.pasos.first.id,
  indice: 0,
  iniciadoEn: ahora,
  ultimaInteraccion: ahora,
  repeticiones: repeticiones,
);

/// Mueve el progreso a un paso. No decide nada: eso es del motor.
ProgresoDeTutorial enPaso(
  ProgresoDeTutorial previo,
  int indice,
  String? pasoId,
  DateTime ahora,
) => previo.copiaCon(
  estado: EstadoDeProgreso.enProgreso,
  indice: indice,
  pasoId: pasoId,
  ultimaInteraccion: ahora,
);

/// Cierra el progreso con un desenlace. Completar suma una repetición; omitir no.
ProgresoDeTutorial cerrado(
  ProgresoDeTutorial previo,
  EstadoDeProgreso estado,
  DateTime ahora,
) => previo.copiaCon(
  estado: estado,
  terminadoEn: ahora,
  ultimaInteraccion: ahora,
  repeticiones: estado == EstadoDeProgreso.completado
      ? previo.repeticiones + 1
      : previo.repeticiones,
);

/// **Cambió la versión del tutorial: lo que había ya no vale.**
///
/// El progreso viejo no se borra —se sigue sabiendo que esa persona lo hizo, y las
/// repeticiones viajan— pero deja de contar como completado y el tutorial vuelve a
/// ofrecerse. Es la estrategia conservadora a propósito: mostrar de más molesta;
/// esconder un cambio que importa deja a alguien usando instrucciones viejas.
bool vigente(ProgresoDeTutorial? p, TutorialDefinicion t) =>
    p != null && p.version == t.version;

/// El estado con el que se pinta la tarjeta, ya con la versión tenida en cuenta.
EstadoDeProgreso estadoDe(ProgresoDeTutorial? p, TutorialDefinicion t) =>
    p == null || !vigente(p, t) ? EstadoDeProgreso.pendiente : p.estado;

/// Se continúa lo que quedó a medias en la versión que está corriendo, **lo haya
/// dejado a medias o lo haya abandonado a propósito**.
///
/// Omitido también cuenta: al salir se promete «guardamos por qué paso ibas». Si
/// después la tarjeta solo ofreciera empezar de cero, esa promesa sería mentira.
bool sePuedeContinuar(ProgresoDeTutorial? p, TutorialDefinicion t) {
  final estado = estadoDe(p, t);
  return (estado == EstadoDeProgreso.enProgreso ||
          estado == EstadoDeProgreso.omitido) &&
      (p?.indice ?? 0) > 0;
}

/// Cuánto recorrió esta persona, de 0 a 1.
double fraccionDe(ProgresoDeTutorial? p, TutorialDefinicion t) {
  final estado = estadoDe(p, t);
  if (estado == EstadoDeProgreso.completado) return 1;
  if (t.pasos.isEmpty || p == null || estado == EstadoDeProgreso.pendiente) {
    return 0;
  }
  return (p.indice / t.pasos.length).clamp(0, 1).toDouble();
}

/// Tutoriales completados sobre tutoriales disponibles. Lo omitido NO cuenta como
/// hecho —omitir es «esto no lo necesito», no «ya lo sé»— pero tampoco se esconde.
double avanceGeneral(
  List<TutorialDefinicion> tutoriales,
  Map<String, ProgresoDeTutorial> progresos,
) {
  if (tutoriales.isEmpty) return 0;
  final hechos = tutoriales
      .where((t) => estadoDe(progresos[t.id], t) == EstadoDeProgreso.completado)
      .length;
  return hechos / tutoriales.length;
}

/// Los requisitos que todavía le faltan a quien mira esta tarjeta.
List<TutorialDefinicion> requisitosPendientes(
  TutorialDefinicion t,
  Map<String, TutorialDefinicion> porId,
  Map<String, ProgresoDeTutorial> progresos,
) => t.requisitos
    .map((id) => porId[id])
    .whereType<TutorialDefinicion>()
    .where((r) => estadoDe(progresos[r.id], r) != EstadoDeProgreso.completado)
    .toList();
