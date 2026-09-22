import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../proveedores/tutoriales.dart';
import 'avance.dart';
import 'escribano.dart';
import 'estado_del_motor.dart';
import 'modelo.dart';

/// **El motor: dónde estoy, cómo sigo y qué hago si el paso no se puede dar.**
///
/// Es una máquina de estados; no dibuja nada. El velo, el globo y los gestos viven en
/// `pantallas/soporte/capa_de_tutorial.dart`, y el catálogo en `pantallas/soporte/
/// catalogo/`. Entre los tres no hay un solo `if` con el id de un tutorial concreto.
///
/// Tres reglas que no se negocian:
/// 1. **No se traba.** Un objetivo que no aparece es un problema declarado con
///    reintento y salida, nunca una pantalla congelada.
/// 2. **No opera.** No toca botones, no envía formularios, no confirma nada.
/// 3. **No inventa permisos.** Los tutoriales ya vienen filtrados por capacidad.
class MotorDeTutoriales extends Notifier<EstadoDelMotor> {
  late final _escribano = EscribanoDeProgreso(ref);
  AnclasDeTutorial? _anclas;
  void Function(String ruta)? _navegar;
  String Function()? _ubicacion;

  @override
  EstadoDelMotor build() => EstadoDelMotor.apagado;

  /// La capa visual le presta al motor lo que solo existe con un `BuildContext`: el
  /// registro de anclas y la navegación. Se llama una vez, al montarse la capa.
  void conectar({
    required AnclasDeTutorial anclas,
    required void Function(String ruta) navegar,
    required String Function() ubicacion,
  }) {
    _anclas = anclas;
    _navegar = navegar;
    _ubicacion = ubicacion;
  }

  /// Empieza de cero. Si ya se había hecho, las repeticiones viajan.
  Future<void> iniciar(String tutorialId, {int repeticiones = 0}) async {
    final tutorial = ref.read(registroDeTutorialesProvider).buscar(tutorialId);
    if (tutorial == null || tutorial.pasos.isEmpty) return;
    state = EstadoDelMotor(tutorial: tutorial);
    await _escribano.guardar(
      progresoInicial(tutorial, DateTime.now(), repeticiones: repeticiones),
    );
    await _ir(0);
  }

  /// Retoma donde quedó. Un índice fuera de rango vuelve al último paso posible.
  Future<void> continuar(String tutorialId, ProgresoDeTutorial progreso) async {
    final tutorial = ref.read(registroDeTutorialesProvider).buscar(tutorialId);
    if (tutorial == null || tutorial.pasos.isEmpty) return;
    state = EstadoDelMotor(tutorial: tutorial);
    await _ir(progreso.indice.clamp(0, tutorial.pasos.length - 1));
  }

  Future<void> avanzar() async {
    if (!state.puedeAvanzar) {
      state = state.copiaCon(
        problema:
            state.paso?.ayudaSiFalla ?? 'Hacé lo que pide el paso para seguir.',
      );
      return;
    }
    if (state.esUltimo) return _terminar();
    await _ir(state.indice + 1);
  }

  Future<void> retroceder() async {
    if (state.indice == 0) return;
    await _ir(state.indice - 1);
  }

  /// Reintenta el paso actual: vuelve a buscar el objetivo desde cero.
  Future<void> reintentar() => _ir(state.indice);

  /// La acción que el paso pedía ya está hecha.
  void marcarAccionCumplida() {
    if (state.activo) state = state.copiaCon(accionCumplida: true);
  }

  /// Pide confirmación si va por la mitad; si recién empezó, cierra sin preguntar.
  void pedirSalida() {
    if (state.indice == 0 || state.esUltimo) {
      omitir();
      return;
    }
    state = state.copiaCon(confirmandoSalida: true);
  }

  void seguirEnElTutorial() {
    state = state.copiaCon(confirmandoSalida: false);
  }

  /// Abandona: queda omitido, con el paso donde se fue. Se puede repetir después.
  void omitir() {
    final tutorial = state.tutorial;
    if (tutorial != null) {
      final ahora = DateTime.now();
      _escribano.sinEsperar(
        cerrado(
          enPaso(
            _escribano.de(tutorial, ahora),
            state.indice,
            state.paso?.id,
            ahora,
          ),
          EstadoDeProgreso.omitido,
          ahora,
        ),
      );
    }
    state = EstadoDelMotor.apagado;
  }

  Future<void> _terminar() async {
    final tutorial = state.tutorial;
    state = EstadoDelMotor.apagado;
    if (tutorial == null) return;
    final ahora = DateTime.now();
    final hecho = _escribano
        .de(tutorial, ahora)
        .copiaCon(indice: tutorial.pasos.length);
    await _escribano.guardar(
      cerrado(hecho, EstadoDeProgreso.completado, ahora),
    );
  }

  /// El corazón: navegar si el paso vive en otra pantalla, esperar al elemento y dejar
  /// todo listo. Cada regreso temprano deja un problema declarado, nunca silencio.
  Future<void> _ir(int indice) async {
    final tutorial = state.tutorial;
    if (tutorial == null || indice >= tutorial.pasos.length) return;
    final paso = tutorial.pasos[indice];
    state = state.copiaCon(
      indice: indice,
      accionCumplida: false,
      limpiarProblema: true,
      limpiarRecuadro: true,
    );

    _escribano.sinEsperar(
      enPaso(
        _escribano.de(tutorial, DateTime.now()),
        indice,
        paso.id,
        DateTime.now(),
      ),
    );

    // El primer paso hereda la ruta del tutorial: `ruta` de la definición es «dónde
    // empieza». Sin esto, abrir desde Ayuda un tutorial de otra pantalla dejaba a la
    // persona en Ayuda, con el primer ancla imposible de encontrar.
    if (!_llegarA(paso.ruta ?? (indice == 0 ? tutorial.ruta : null))) return;
    if (paso.ancla == null) return;
    final recuadro = await _anclas?.esperar(paso.ancla!, paso.espera);
    if (!state.activo || state.indice != indice) return;
    if (recuadro == null) {
      state = state.copiaCon(
        problema: 'No encontramos «${paso.ancla}» en esta pantalla.',
      );
      return;
    }
    state = state.copiaCon(recuadro: recuadro);
  }

  /// Lleva a esa ruta si hace falta. `false` si no se pudo.
  bool _llegarA(String? ruta) {
    if (ruta == null) return true;
    final actual = _ubicacion?.call() ?? '';
    if (actual == ruta || actual.startsWith('$ruta/')) return true;
    if (_navegar == null) {
      state = state.copiaCon(problema: 'No pudimos abrir «$ruta» desde acá.');
      return false;
    }
    _navegar!(ruta);
    return true;
  }

  /// Vuelve a medir el elemento del paso actual: la pantalla se desplazó o cambió de
  /// tamaño y el agujero tiene que seguirla.
  void medirDeNuevo() {
    final ancla = state.paso?.ancla;
    if (!state.activo || ancla == null) return;
    final recuadro = _anclas?.recuadroDe(ancla);
    if (recuadro != null && recuadro != state.recuadro) {
      state = state.copiaCon(recuadro: recuadro);
    }
  }
}
