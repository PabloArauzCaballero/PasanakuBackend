import '../proveedores/sesion.dart';

typedef EjecutarCierreDeSesion =
    Future<void> Function({required MotivoDeSalida motivo});

/// Revisa el tope absoluto al hidratar, volver al frente y periódicamente.
/// El reloj y el cierre son puertos para mantener la regla determinista.
class TopeDeSesion {
  TopeDeSesion({
    required this._sesion,
    required this._ahora,
    required this._cerrar,
  });

  final Sesion _sesion;
  final DateTime Function() _ahora;
  final EjecutarCierreDeSesion _cerrar;
  Future<void>? _revisionEnCurso;

  Future<void> revisar() {
    final actual = _revisionEnCurso;
    if (actual != null) return actual;
    return _revisionEnCurso = _revisar().whenComplete(() {
      _revisionEnCurso = null;
    });
  }

  Future<void> _revisar() async {
    if (!_sesion.activa) return;
    final inicio = await _sesion.inicioConCredencial();
    if (sesionVencida(inicio, _ahora().toUtc())) {
      await _cerrar(motivo: MotivoDeSalida.caducada);
    }
  }
}
