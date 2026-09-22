import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/puertos/almacen_de_progreso.dart';
import '../dominio/tutoriales/estado_del_motor.dart';
import '../dominio/tutoriales/modelo.dart';
import '../dominio/tutoriales/motor.dart';
import '../dominio/tutoriales/registro.dart';
import '../infraestructura/almacen_de_progreso_seguro.dart';
import '../pantallas/soporte/catalogo/catalogo.dart';
import 'sesion.dart';

/// **El enchufe del motor de tutoriales.** Todo lo que hay que cambiar para apuntar a
/// otro catálogo, a otro almacén o a otro origen de capacidades está en este archivo.

/// El catálogo. Se sobreescribe en pruebas con uno de mentira.
final catalogoDeTutorialesProvider = Provider<List<TutorialDefinicion>>(
  (_) => catalogoDeTutoriales,
);

/// **Qué puede hacer quien está mirando**, que es lo que decide qué tutoriales se le
/// ofrecen.
///
/// `sesion` sale de que haya un token guardado, que es real. `organizador` queda en
/// falso hasta que la app exponga la habilitación del CU-90 fuera de la pantalla de
/// crear grupo (hoy solo se consulta con un `organizadorId` concreto): declararlo acá
/// en vez de inventarlo deja el hueco a la vista y con un solo lugar donde llenarlo.
final capacidadesProvider = FutureProvider<Set<Capacidad>>((ref) async {
  final token = await ref.watch(sesionProvider).tokenDeAcceso();
  return {if (token != null && token.isNotEmpty) Capacidad.sesion};
});

final registroDeTutorialesProvider = Provider<RegistroDeTutoriales>((ref) {
  final capacidades =
      ref.watch(capacidadesProvider).asData?.value ?? const <Capacidad>{};
  return RegistroDeTutoriales(
    ref.watch(catalogoDeTutorialesProvider),
    capacidades,
  );
});

/// El adaptador del progreso. Local hoy; el remoto se enchufa acá el día que exista.
final almacenDeProgresoProvider = Provider<AlmacenDeProgreso>(
  (ref) => AlmacenDeProgresoSeguro(ref.watch(almacenSeguroProvider)),
);

/// **La única copia viva del avance.** El motor escribe por acá y el centro de ayuda
/// lee de acá, así una tarjeta se actualiza sola al terminar un recorrido.
class ProgresoDeTutoriales extends Notifier<Map<String, ProgresoDeTutorial>> {
  @override
  Map<String, ProgresoDeTutorial> build() {
    cargar().ignore();
    return const {};
  }

  /// Trae lo guardado. Si el almacén falla, el avance arranca vacío: se pierde el
  /// historial, no la posibilidad de hacer el tutorial.
  Future<void> cargar() async {
    try {
      final filas = await ref.read(almacenDeProgresoProvider).leer();
      // Lo que se escribió mientras esta lectura viajaba es más nuevo que lo leído: se
      // conserva. Reemplazar el mapa entero borraba el avance de un tutorial que había
      // empezado durante la carga.
      state = {for (final p in filas) p.tutorialId: p, ...state};
    } on Object {
      state = const {};
    }
  }

  Future<void> guardar(ProgresoDeTutorial progreso) async {
    state = {...state, progreso.tutorialId: progreso};
    await ref.read(almacenDeProgresoProvider).guardar(progreso);
  }

  /// Reiniciar borra la fila: el tutorial vuelve a estar como el primer día.
  Future<void> reiniciar(String tutorialId) async {
    state = {...state}..remove(tutorialId);
    await ref.read(almacenDeProgresoProvider).reiniciar(tutorialId);
  }
}

final progresoDeTutorialesProvider =
    NotifierProvider<ProgresoDeTutoriales, Map<String, ProgresoDeTutorial>>(
      ProgresoDeTutoriales.new,
    );

final motorDeTutorialesProvider =
    NotifierProvider<MotorDeTutoriales, EstadoDelMotor>(MotorDeTutoriales.new);
