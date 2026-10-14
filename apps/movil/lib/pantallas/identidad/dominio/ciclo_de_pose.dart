import 'juez_de_pose.dart';

/// Tiempo para colocarse en la pose antes de empezar a mirar.
const respiroDePose = Duration(milliseconds: 1600);

/// La cuenta atrás visible: 3 s de pose sostenida.
const cuentaAtrasDePose = Duration(seconds: 3);

/// Cuánto se ve «¡Foto tomada!» antes de la pose siguiente.
const avisoDeFotoTomada = Duration(milliseconds: 1100);

/// Si no llega un veredicto tras el respiro, la cámara se trata como no disponible.
const silencioDelDetector = Duration(seconds: 2);

enum FaseDePose { respiro, buscando, cuentaAtras, tomada, terminada }

enum OrigenDeFoto { automatica, manual }

/// Estado que consume la pantalla sin depender de Flutter.
class EstadoDePose {
  const EstadoDePose({
    required this.fase,
    this.segundos,
    this.progreso = 0,
    this.pista,
    this.disparar = false,
    this.origen,
  });

  final FaseDePose fase;
  final int? segundos;
  final double progreso;
  final String? pista;
  final bool disparar;
  final OrigenDeFoto? origen;
}

/// Ciclo de una pose. Avanza con reloj inyectado y veredictos del juez.
/// Con detector, captura solo tras 3 s de pose válida sostenida. Sin detector,
/// captura tras el respiro y la cuenta atrás. La captura manual está siempre disponible.
class CicloDePose {
  CicloDePose({required DateTime inicio, required bool hayDetector})
    : this._(inicio, hayDetector);

  CicloDePose._(this._inicio, this.hayDetector);

  final DateTime _inicio;
  bool hayDetector;
  DateTime? _inicioDeCuenta;
  DateTime? _tomadaEn;
  DateTime? _ultimoVeredicto;
  Veredicto? _ultimo;
  OrigenDeFoto? _origen;

  /// Procesa un cuadro del detector o un tic de reloj sin detector.
  EstadoDePose avanzar(DateTime ahora, {Veredicto? veredicto}) {
    if (_tomadaEn != null) {
      final fase = ahora.difference(_tomadaEn!) >= avisoDeFotoTomada
          ? FaseDePose.terminada
          : FaseDePose.tomada;
      return EstadoDePose(fase: fase, progreso: 1, origen: _origen);
    }

    final desdeInicio = ahora.difference(_inicio);
    if (desdeInicio.isNegative) {
      _inicioDeCuenta = null;
      return const EstadoDePose(fase: FaseDePose.respiro);
    }
    if (desdeInicio < respiroDePose) {
      return const EstadoDePose(fase: FaseDePose.respiro);
    }

    if (veredicto != null) {
      _ultimoVeredicto = ahora;
      _ultimo = veredicto;
    }
    final mudo = _ultimoVeredicto == null
        ? desdeInicio >= respiroDePose + silencioDelDetector
        : ahora.difference(_ultimoVeredicto!) >= silencioDelDetector;
    if (hayDetector && mudo) hayDetector = false;

    final reciente = _ultimoVeredicto != null && !mudo ? _ultimo : null;
    final poseVista =
        !hayDetector || (reciente != null && _sinProblema(reciente));
    if (!poseVista) {
      _inicioDeCuenta = null;
      return EstadoDePose(
        fase: FaseDePose.buscando,
        // El ticker no trae veredicto, pero no debe borrar la última instrucción
        // útil que emitió el detector entre dos cuadros.
        pista: reciente?.pista ?? veredicto?.pista,
        progreso: reciente?.progreso ?? veredicto?.progreso ?? 0,
      );
    }

    _inicioDeCuenta ??= ahora;
    final sostenida = ahora.difference(_inicioDeCuenta!);
    if (sostenida.isNegative) {
      _inicioDeCuenta = ahora;
      return const EstadoDePose(fase: FaseDePose.cuentaAtras, segundos: 3);
    }
    if (sostenida >= cuentaAtrasDePose) {
      return _tomar(ahora, OrigenDeFoto.automatica);
    }

    final restante = cuentaAtrasDePose - sostenida;
    return EstadoDePose(
      fase: FaseDePose.cuentaAtras,
      segundos: (restante.inMilliseconds / 1000).ceil().clamp(1, 3).toInt(),
      progreso: sostenida.inMilliseconds / cuentaAtrasDePose.inMilliseconds,
      pista: 'Quedate así…',
    );
  }

  /// «Tomar ahora»: siempre disponible salvo mientras se muestra «¡Foto tomada!».
  EstadoDePose tomarAhora(DateTime ahora) =>
      _tomadaEn != null ? avanzar(ahora) : _tomar(ahora, OrigenDeFoto.manual);

  /// Reinicia el ciclo tras rechazar localmente la foto.
  void reiniciar() {
    _tomadaEn = null;
    _inicioDeCuenta = null;
    _ultimoVeredicto = null;
    _ultimo = null;
    _origen = null;
  }

  EstadoDePose _tomar(DateTime ahora, OrigenDeFoto origen) {
    _tomadaEn = ahora;
    _origen = origen;
    return EstadoDePose(
      fase: FaseDePose.tomada,
      progreso: 1,
      disparar: true,
      origen: origen,
    );
  }

  /// El juez marca «listo» tras varios cuadros; cualquier cuadro sin problema
  /// permite iniciar la cuenta, que exige sostener la pose durante 3 s.
  bool _sinProblema(Veredicto veredicto) =>
      veredicto.listo || veredicto.progreso > 0;
}
