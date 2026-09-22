import '../tutoriales/modelo.dart';

/// **El puerto del progreso de los tutoriales.**
///
/// El motor no sabe si esto termina en el almacén del teléfono o en una tabla del
/// servidor: pide leer, guardar y reiniciar, y alguien cumple.
///
/// Hoy el adaptador es local (`infraestructura/almacen_de_progreso_seguro.dart`)
/// porque el backend todavía no tiene dónde guardar esto. El contrato remoto está
/// documentado en `docs/Frontend/Motor de tutoriales.md` §11; el día que exista, se
/// escribe otro adaptador y se cambia un `override`. Ninguna pantalla se entera.
abstract interface class AlmacenDeProgreso {
  /// Todo el progreso de quien tiene la sesión abierta en este teléfono.
  Future<List<ProgresoDeTutorial>> leer();

  /// Guarda una fila entera. Idempotente: la misma fila dos veces deja lo mismo.
  Future<void> guardar(ProgresoDeTutorial progreso);

  /// Borra el progreso de UN tutorial: reiniciar es empezar de cero.
  Future<void> reiniciar(String tutorialId);
}
