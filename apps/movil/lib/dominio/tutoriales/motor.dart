import 'dart:async';

import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../proveedores/sesion.dart';
import '../../proveedores/tutoriales.dart';
import 'avance.dart';
import 'escribano.dart';
import 'estado_del_motor.dart';
import 'modelo.dart';
import 'registro.dart';
import 'textos_del_motor.dart';

export 'textos_del_motor.dart';
part 'motor_acciones.dart';

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
  EstadoDelMotor get _estado => state;
  set _estado(EstadoDelMotor valor) => state = valor;

  RegistroDeTutoriales get _registro => ref.read(registroDeTutorialesProvider);
  Sesion get _sesion => ref.read(sesionProvider);

  AnclasDeTutorial? _anclas;
  void Function(String ruta)? _navegar;
  String Function()? _ubicacion;
  Timer? _temporizadorDeEspera;
  Completer<void>? _esperaEnCurso;
  int _generacion = 0;

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

  Future<void> _pausarCancelable(Duration duracion) {
    _temporizadorDeEspera?.cancel();
    final espera = Completer<void>();
    _esperaEnCurso = espera;
    _temporizadorDeEspera = Timer(duracion, () {
      if (!espera.isCompleted) espera.complete();
      if (identical(_esperaEnCurso, espera)) {
        _esperaEnCurso = null;
        _temporizadorDeEspera = null;
      }
    });
    return espera.future;
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
  Future<void> _ir(int indice, {required int generacion}) async {
    if (generacion != _generacion) return;
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
    final recuadro = await _anclas?.esperar(
      paso.ancla!,
      paso.espera,
      pausa: _pausarCancelable,
      cancelada: () => generacion != _generacion,
    );
    if (generacion != _generacion || !state.activo || state.indice != indice) {
      return;
    }
    if (recuadro == null) {
      state = state.copiaCon(
        // Sin el id interno del ancla («billetera.saldo»): no le dice nada a quien mira. Si esta
        // parte de la pantalla no está (por ejemplo, la billetera todavía no está activa), se
        // dice eso y se puede reintentar o salir de la guía.
        problema: textoAnclaAusente,
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
      state = state.copiaCon(problema: textoNoSePudoAbrir);
      return false;
    }
    _navegar!(ruta);
    return true;
  }
}
