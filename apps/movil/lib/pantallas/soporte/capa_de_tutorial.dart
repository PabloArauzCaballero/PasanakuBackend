import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:aportaya_diseno/moviles/foco_de_tutorial.dart';
import 'package:aportaya_diseno/moviles/globo_de_tutorial.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../dominio/tutoriales/modelo.dart';
import '../../proveedores/tutoriales.dart';
import 'textos.dart';

/// **La capa del tutorial**: lo único que la app monta para que los tutoriales existan.
///
/// Junta lo que el motor no toca porque es vista: el velo, el globo, el toque que
/// cumple un paso y la confirmación de salida. Con el tutorial apagado no dibuja nada
/// y no escucha nada.
class CapaDeTutorial extends ConsumerStatefulWidget {
  const CapaDeTutorial({
    super.key,
    required this.hijo,
    required this.enrutador,
  });

  final Widget hijo;
  final GoRouter enrutador;

  @override
  ConsumerState<CapaDeTutorial> createState() => _CapaDeTutorialState();
}

class _CapaDeTutorialState extends ConsumerState<CapaDeTutorial>
    with WidgetsBindingObserver {
  bool _conectado = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  /// Subió el teclado o giró el teléfono: el elemento se movió y el agujero lo sigue.
  @override
  void didChangeMetrics() =>
      ref.read(motorDeTutorialesProvider.notifier).medirDeNuevo();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_conectado) return;
    final anclas = anclasDe(context);
    if (anclas == null) return;
    _conectado = true;
    ref
        .read(motorDeTutorialesProvider.notifier)
        .conectar(
          anclas: anclas,
          navegar: widget.enrutador.go,
          ubicacion: () => widget.enrutador.state.uri.path,
        );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  /// Se remide **al desplazar**, que es como se mueve un elemento en un teléfono. Se
  /// escucha la notificación de scroll en vez de dejar un temporizador latiendo: un
  /// reloj que late siempre gasta batería y, en las pruebas, queda pendiente para
  /// siempre.
  bool _alDesplazar(ScrollNotification _) {
    ref.read(motorDeTutorialesProvider.notifier).medirDeNuevo();
    return false;
  }

  /// Un toque dentro del elemento resaltado cumple el paso que pedía tocarlo. Se mira
  /// el puntero desde arriba en vez de envolver cada botón: así ninguna pantalla tiene
  /// que enterarse de que los tutoriales existen.
  void _mirarToque(PointerDownEvent evento) {
    final estado = ref.read(motorDeTutorialesProvider);
    final paso = estado.paso;
    if (paso == null || paso.accion.tipo != TipoDeAccion.toque) return;
    if (estado.recuadro?.contains(evento.position) ?? false) {
      ref.read(motorDeTutorialesProvider.notifier).marcarAccionCumplida();
    }
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(motorDeTutorialesProvider);
    final motor = ref.read(motorDeTutorialesProvider.notifier);
    _vigilarNavegacion(estado.paso);
    // Después de pintar, una medición: el elemento acaba de aparecer o de moverse por
    // un cambio de la propia pantalla.
    if (estado.activo) {
      WidgetsBinding.instance.addPostFrameCallback((_) => motor.medirDeNuevo());
    }

    final paso = estado.paso;
    return Stack(
      children: [
        NotificationListener<ScrollNotification>(
          onNotification: _alDesplazar,
          child: Listener(onPointerDown: _mirarToque, child: widget.hijo),
        ),
        if (paso != null) ...[
          Positioned.fill(
            child: FocoDeTutorial(
              recuadro: estado.recuadro,
              bloquea: paso.bloquea,
              alTocarFuera: motor.pedirSalida,
            ),
          ),
          // **El globo se da vuelta si lo que señala está abajo.** Apoyado siempre
          // abajo tapaba justo la barra de pestañas, que es lo que más se señala en
          // una app. Con el elemento en la mitad inferior, el globo sube.
          Positioned(
            left: 0,
            right: 0,
            top: _arriba(context, estado.recuadro, paso.posicion) ? 0 : null,
            bottom: _arriba(context, estado.recuadro, paso.posicion) ? null : 0,
            child: GloboDeTutorial(
              arriba: _arriba(context, estado.recuadro, paso.posicion),
              titulo: paso.titulo,
              descripcion: paso.descripcion,
              indice: estado.indice,
              total: estado.total,
              problema: estado.problema,
              textoAvanzar: estado.esUltimo
                  ? TextosSoporte.terminar
                  : TextosSoporte.siguiente,
              textoSalir: TextosSoporte.omitir,
              alAvanzar: motor.avanzar,
              alRetroceder: motor.retroceder,
              alSalir: motor.pedirSalida,
              alReintentar: motor.reintentar,
            ),
          ),
        ],
        if (estado.confirmandoSalida)
          Positioned.fill(
            child: _ConfirmarSalida(
              alSalir: motor.omitir,
              alSeguir: motor.seguirEnElTutorial,
            ),
          ),
      ],
    );
  }

  /// ¿El globo va arriba? Sí cuando el paso lo pide, o cuando lo que resalta está en
  /// la mitad de abajo de la pantalla —ahí es donde el globo lo taparía—.
  bool _arriba(BuildContext context, Rect? recuadro, PosicionDeGlobo posicion) {
    if (posicion == PosicionDeGlobo.arriba) return true;
    if (recuadro == null) return false;
    return recuadro.center.dy > MediaQuery.sizeOf(context).height * 0.55;
  }

  /// Un paso que pide llegar a una ruta se cumple solo: se compara con la ubicación.
  void _vigilarNavegacion(PasoDeTutorial? paso) {
    if (paso == null || paso.accion.tipo != TipoDeAccion.navegar) return;
    final ubicacion = widget.enrutador.state.uri.path;
    final destino = paso.accion.ruta!;
    if (ubicacion == destino || ubicacion.startsWith('$destino/')) {
      WidgetsBinding.instance.addPostFrameCallback(
        (_) =>
            ref.read(motorDeTutorialesProvider.notifier).marcarAccionCumplida(),
      );
    }
  }
}

/// Confirmación antes de abandonar a la mitad. No es un `showDialog` porque el globo ya
/// está encima de todo: se dibuja en la misma capa y no pelea por el foco.
class _ConfirmarSalida extends StatelessWidget {
  const _ConfirmarSalida({required this.alSalir, required this.alSeguir});

  final VoidCallback alSalir;
  final VoidCallback alSeguir;

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: const Text(TextosSoporte.salirTitulo),
    content: const Text(TextosSoporte.salirCuerpo),
    actions: [
      TextButton(
        onPressed: alSalir,
        child: const Text(TextosSoporte.salirConfirmar),
      ),
      TextButton(
        onPressed: alSeguir,
        child: const Text(TextosSoporte.salirSeguir),
      ),
    ],
  );
}
