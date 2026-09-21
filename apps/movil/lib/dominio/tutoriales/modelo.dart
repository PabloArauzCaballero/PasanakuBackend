/// **El contrato del motor de tutoriales de la app.** Solo datos: ni un `import` de
/// Flutter, ni una línea de red. Un tutorial se declara en
/// `pantallas/soporte/catalogo/` y el motor no conoce a ninguno en particular.
///
/// Es el mismo modelo que el del backoffice (`apps/backoffice/src/app/nucleo/
/// tutoriales/tipos.ts`), en Dart: dos productos, un solo concepto de «tutorial».
library;

/// Lo que alguien puede hacer en la app, y que decide qué tutoriales le sirven.
///
/// **No es un permiso**: los permisos los aplica el servidor en cada llamada. Esto solo
/// decide qué se OFRECE. Un tutorial jamás abre nada que la app no abriera igual.
enum Capacidad {
  /// Hay sesión abierta en este teléfono.
  sesion,

  /// Esta persona está habilitada para organizar un grupo (CU-90).
  organizador,
}

/// Dónde se planta el globo respecto del elemento resaltado.
enum PosicionDeGlobo { arriba, abajo, centro }

/// Cuánto pide el tutorial de quien lo hace. Ordena el catálogo, no bloquea nada.
enum Dificultad { inicial, intermedio, avanzado }

/// Qué se espera que haga la persona antes de poder seguir.
///
/// **Ninguna acción escribe en el servidor.** El motor mira lo que ya pasa en la
/// pantalla; nunca toca por su cuenta. Un tutorial no aporta, no paga y no retira.
enum TipoDeAccion {
  /// Se lee y se sigue con «Siguiente». Es lo normal.
  ninguna,

  /// Hay que tocar el elemento resaltado.
  toque,

  /// Hay que llegar a una ruta.
  navegar,

  /// Hay que hacer que algo aparezca (una hoja, un panel, una fila).
  aparezca,
}

/// La acción esperada, con el dato que necesite según su tipo.
class AccionEsperada {
  const AccionEsperada.ninguna()
    : tipo = TipoDeAccion.ninguna,
      ruta = null,
      ancla = null;
  const AccionEsperada.toque({this.ancla})
    : tipo = TipoDeAccion.toque,
      ruta = null;
  const AccionEsperada.navegar(String this.ruta)
    : tipo = TipoDeAccion.navegar,
      ancla = null;
  const AccionEsperada.aparezca(String this.ancla)
    : tipo = TipoDeAccion.aparezca,
      ruta = null;

  final TipoDeAccion tipo;
  final String? ruta;
  final String? ancla;
}

/// Un paso: una sola idea, un solo elemento, una sola acción.
class PasoDeTutorial {
  const PasoDeTutorial({
    required this.id,
    required this.titulo,
    required this.descripcion,
    this.ancla,
    this.posicion = PosicionDeGlobo.abajo,
    this.ruta,
    this.orden,
    this.accion = const AccionEsperada.ninguna(),
    this.ayudaSiFalla,
    this.espera = const Duration(seconds: 4),
    this.bloquea = false,
  });

  final String id;
  final String titulo;
  final String descripcion;

  /// El id de la `MarcaDeTutorial` que envuelve al elemento. Sin ancla, el globo va
  /// centrado y habla de la pantalla entera.
  final String? ancla;
  final PosicionDeGlobo posicion;

  /// La ruta donde este paso tiene sentido. El motor navega si hace falta.
  final String? ruta;

  /// Solo para validar el orden declarado; el orden real es el de la lista.
  final int? orden;
  final AccionEsperada accion;

  /// Qué decirle a quien se quedó trabado.
  final String? ayudaSiFalla;

  /// Cuánto esperar a un elemento que llega después de una petición.
  final Duration espera;

  /// Con `true` no se puede tocar la app detrás del velo. Por omisión, sí se puede:
  /// un paso que dice «tocá este botón» tiene que dejar tocarlo.
  final bool bloquea;
}

/// Un tutorial completo. Es una constante, no una clase con lógica.
class TutorialDefinicion {
  const TutorialDefinicion({
    required this.id,
    required this.version,
    required this.titulo,
    required this.descripcion,
    required this.categoria,
    required this.dificultad,
    required this.pasos,
    this.ruta,
    this.minutos,
    this.obligatorio = false,
    this.requisitos = const [],
    this.siguiente,
    this.requiere = const {},
  });

  final String id;

  /// Cambiarla es decir «esto ya no es el mismo tutorial» (ver `avance.dart`).
  final String version;
  final String titulo;
  final String descripcion;
  final String categoria;
  final Dificultad dificultad;
  final List<PasoDeTutorial> pasos;

  /// Dónde empieza; también enciende el acceso contextual de esa pantalla.
  final String? ruta;
  final int? minutos;
  final bool obligatorio;

  /// Ids de tutoriales que conviene haber hecho antes. Avisan, no bloquean.
  final List<String> requisitos;

  /// El que se ofrece al terminar este.
  final String? siguiente;

  /// Qué tiene que poder hacer alguien para que este tutorial le sirva. Un tutorial
  /// que enseña a crear un grupo no se le ofrece a quien no puede organizar: sería
  /// enseñarle a usar un botón que no va a ver.
  final Set<Capacidad> requiere;
}

/// Qué le pasó a una persona con un tutorial.
enum EstadoDeProgreso { pendiente, enProgreso, completado, omitido }

/// La fila de progreso. Es lo que viaja al almacén, local hoy y remoto mañana.
class ProgresoDeTutorial {
  const ProgresoDeTutorial({
    required this.tutorialId,
    required this.version,
    required this.estado,
    required this.indice,
    required this.iniciadoEn,
    required this.ultimaInteraccion,
    this.pasoId,
    this.terminadoEn,
    this.repeticiones = 0,
  });

  final String tutorialId;
  final String version;
  final EstadoDeProgreso estado;
  final String? pasoId;
  final int indice;
  final DateTime iniciadoEn;
  final DateTime? terminadoEn;
  final DateTime ultimaInteraccion;
  final int repeticiones;

  ProgresoDeTutorial copiaCon({
    EstadoDeProgreso? estado,
    String? pasoId,
    int? indice,
    DateTime? terminadoEn,
    DateTime? ultimaInteraccion,
    int? repeticiones,
  }) => ProgresoDeTutorial(
    tutorialId: tutorialId,
    version: version,
    estado: estado ?? this.estado,
    pasoId: pasoId ?? this.pasoId,
    indice: indice ?? this.indice,
    iniciadoEn: iniciadoEn,
    terminadoEn: terminadoEn ?? this.terminadoEn,
    ultimaInteraccion: ultimaInteraccion ?? this.ultimaInteraccion,
    repeticiones: repeticiones ?? this.repeticiones,
  );
}
