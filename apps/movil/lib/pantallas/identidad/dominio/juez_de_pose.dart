import '../../../dominio/puertos/camara_en_vivo.dart';
import 'capturas_del_expediente.dart';

/// Las tres poses de la prueba de vida, en el orden en que se piden.
enum PoseDeVida {
  frente(CaraDelCarril.selfie),
  izquierda(CaraDelCarril.perfilIzquierdo),
  derecha(CaraDelCarril.perfilDerecho);

  const PoseDeVida(this.cara);
  final CaraDelCarril cara;

  static PoseDeVida? de(CaraDelCarril cara) {
    for (final p in values) {
      if (p.cara == cara) return p;
    }
    return null;
  }
}

/// Lo que el juez dice de un cuadro: si ya se puede sacar la foto, qué decirle a la
/// persona y cuánto falta de la pose sostenida (0 a 1).
class Veredicto {
  const Veredicto({
    required this.listo,
    required this.pista,
    this.progreso = 0,
  });
  final bool listo;
  final String pista;
  final double progreso;
}

/// Decide, cuadro a cuadro, si la persona está en la pose pedida: la foto se saca
/// sola cuando la pose se sostiene unos cuadros seguidos, sin tocar nada.
///
/// **Los umbrales son un punto de partida, no valores medidos**, igual que los del
/// escáner de Atlas (`captura-del-carnet.ts`). Se ajustan con las fotos que juntan
/// las altas de TEST, no a ojo. Pedir la pose *sostenida* evita que un giro de paso,
/// o un cuadro borroso a mitad de movimiento, dispare la foto.
class JuezDePose {
  JuezDePose(this.pose);
  final PoseDeVida pose;

  static const cuadrosSostenidos = 4;
  static const giroMaximoDeFrente = 12.0;
  static const inclinacionMaxima = 18.0;
  static const giroMinimoDePerfil = 28.0;
  static const giroMaximoDePerfil = 70.0;
  static const anchoMinimoDeFrente = 0.28;
  static const anchoMinimoDePerfil = 0.22;
  static const descentradoMaximo = 0.22;
  static const ojosAbiertosMinimo = 0.4;

  int _seguidos = 0;

  Veredicto evaluar(LecturaDeRostro? l) {
    final problema = _problema(l);
    if (problema != null) {
      _seguidos = 0;
      return Veredicto(listo: false, pista: problema);
    }
    _seguidos++;
    final progreso = (_seguidos / cuadrosSostenidos).clamp(0.0, 1.0);
    return Veredicto(
      listo: _seguidos >= cuadrosSostenidos,
      pista: 'Quedate así…',
      progreso: progreso,
    );
  }

  /// Vuelve a empezar la cuenta: después de una foto rechazada, por ejemplo.
  void reiniciar() => _seguidos = 0;

  String? _problema(LecturaDeRostro? l) {
    if (l == null || l.rostros == 0) {
      return 'No vemos tu cara: ponela dentro del marco.';
    }
    if (l.rostros > 1) return 'Hay más de una cara: que salgas solo vos.';
    final minimo = pose == PoseDeVida.frente
        ? anchoMinimoDeFrente
        : anchoMinimoDePerfil;
    if (l.ancho < minimo) return 'Acercá un poco el teléfono.';
    if ((l.centroX - 0.5).abs() > descentradoMaximo ||
        (l.centroY - 0.5).abs() > descentradoMaximo) {
      return 'Centrá tu cara en el marco.';
    }
    if (l.inclinacion.abs() > inclinacionMaxima) {
      return 'Mantené la cabeza derecha, sin mirar arriba ni abajo.';
    }
    return switch (pose) {
      PoseDeVida.frente => _deFrente(l),
      PoseDeVida.izquierda => _dePerfil(l.giro, 'izquierda'),
      PoseDeVida.derecha => _dePerfil(-l.giro, 'derecha'),
    };
  }

  String? _deFrente(LecturaDeRostro l) {
    if (l.giro.abs() > giroMaximoDeFrente) return 'Mirá de frente a la cámara.';
    final ojos = l.ojosAbiertos;
    if (ojos != null && ojos < ojosAbiertosMinimo) return 'Abrí bien los ojos.';
    return null;
  }

  /// [hacia] es el giro hacia el lado pedido: negativo es hacia el otro lado.
  String? _dePerfil(double hacia, String lado) {
    if (hacia < giroMinimoDePerfil) return 'Girá la cabeza hacia tu $lado.';
    if (hacia > giroMaximoDePerfil) return 'Giraste de más: volvé un poco.';
    return null;
  }
}
