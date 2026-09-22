import 'package:flutter/widgets.dart';

/// **El registro de lo que un tutorial puede señalar.**
///
/// En la web se marca con `data-tutorial-id`; en Flutter no hay DOM, así que se marca
/// con una `GlobalKey` estable por id. Quien envuelve un widget en [MarcaDeTutorial] le
/// presta esa clave, y el motor pregunta acá dónde quedó en pantalla.
///
/// Guardar la clave en un mapa y no en cada pantalla es lo que permite que una pantalla
/// **no sepa** que los tutoriales existen: solo declara que ese botón se llama
/// `billetera.recargar`.
class AnclasDeTutorial {
  final Map<String, GlobalKey> _claves = {};

  /// La clave de un id. Siempre la misma para el mismo id, aunque la pantalla se
  /// reconstruya: una clave nueva por reconstrucción dejaría el foco huérfano.
  GlobalKey clave(String id) =>
      _claves.putIfAbsent(id, () => GlobalKey(debugLabel: 'tutorial:$id'));

  /// Dónde está el elemento en la pantalla. `null` si todavía no se dibujó o si ya no
  /// está: eso es un dato normal, no un error.
  Rect? recuadroDe(String id) {
    final contexto = _claves[id]?.currentContext;
    if (contexto == null) return null;
    final render = contexto.findRenderObject();
    if (render is! RenderBox || !render.hasSize || !render.attached) {
      return null;
    }
    return render.localToGlobal(Offset.zero) & render.size;
  }

  /// Espera a que el elemento aparezca, para lo que llega después de una petición.
  /// Devuelve `null` al vencerse el plazo — **nunca lanza**: quién decide qué hacer con
  /// un objetivo que no está es el motor.
  ///
  /// Cuenta **intentos**, no reloj de pared. En `flutter_test` los temporizadores corren
  /// en un reloj falso que solo avanza al bombear cuadros, mientras que `DateTime.now()`
  /// sigue siendo el reloj de verdad: mezclarlos hacía que la espera nunca terminara en
  /// una prueba y que el problema del paso jamás se mostrara.
  Future<Rect?> esperar(
    String id,
    Duration plazo, {
    Duration paso = const Duration(milliseconds: 50),
  }) async {
    final intentos = (plazo.inMilliseconds / paso.inMilliseconds).ceil().clamp(
      1,
      600,
    );
    for (var i = 0; i < intentos; i++) {
      final recuadro = recuadroDe(id);
      if (recuadro != null) {
        return recuadro;
      }
      await Future<void>.delayed(paso);
    }
    return recuadroDe(id);
  }
}

/// Pone un registro de anclas al alcance de todo el árbol. Va una sola vez, arriba del
/// enrutador.
class ProveedorDeAnclas extends StatefulWidget {
  const ProveedorDeAnclas({super.key, required this.hijo, this.anclas});

  final Widget hijo;

  /// Se puede inyectar uno propio en pruebas.
  final AnclasDeTutorial? anclas;

  @override
  State<ProveedorDeAnclas> createState() => _ProveedorDeAnclasState();
}

class _ProveedorDeAnclasState extends State<ProveedorDeAnclas> {
  late final AnclasDeTutorial _anclas = widget.anclas ?? AnclasDeTutorial();

  @override
  Widget build(BuildContext context) =>
      _AnclasHeredadas(anclas: _anclas, child: widget.hijo);
}

class _AnclasHeredadas extends InheritedWidget {
  const _AnclasHeredadas({required this.anclas, required super.child});

  final AnclasDeTutorial anclas;

  @override
  bool updateShouldNotify(_AnclasHeredadas anterior) =>
      anclas != anterior.anclas;
}

/// El registro que hay arriba, o `null` si nadie lo puso.
AnclasDeTutorial? anclasDe(BuildContext context) =>
    context.dependOnInheritedWidgetOfExactType<_AnclasHeredadas>()?.anclas;

/// **Marca un widget como algo que un tutorial puede señalar.**
///
/// ```dart
/// MarcaDeTutorial(id: 'billetera.recargar', hijo: Boton(...))
/// ```
///
/// Sin registro arriba —una prueba de una pantalla suelta, un golden— devuelve el hijo
/// tal cual. Marcar nunca puede cambiar cómo se ve una pantalla.
class MarcaDeTutorial extends StatelessWidget {
  const MarcaDeTutorial({super.key, required this.id, required this.hijo});

  final String id;
  final Widget hijo;

  @override
  Widget build(BuildContext context) {
    final anclas = anclasDe(context);
    if (anclas == null) return hijo;
    return KeyedSubtree(key: anclas.clave(id), child: hijo);
  }
}
