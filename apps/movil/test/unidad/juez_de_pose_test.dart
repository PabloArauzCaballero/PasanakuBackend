import 'package:aportaya_movil/dominio/puertos/camara_en_vivo.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/juez_de_pose.dart';
import 'package:flutter_test/flutter_test.dart';

/// La prueba de vida se dispara sola cuando la pose se sostiene; nunca con un
/// cuadro suelto, ni con la pose equivocada, ni con dos caras.
void main() {
  LecturaDeRostro rostro({
    double giro = 0,
    double ancho = 0.4,
    double centroX = 0.5,
    double centroY = 0.5,
    int rostros = 1,
    double inclinacion = 0,
    double? ojos,
  }) => LecturaDeRostro(
    rostros: rostros,
    centroX: centroX,
    centroY: centroY,
    ancho: ancho,
    giro: giro,
    inclinacion: inclinacion,
    ojosAbiertos: ojos,
  );

  /// Evalúa la misma lectura N veces y devuelve el último veredicto.
  Veredicto sostener(JuezDePose juez, LecturaDeRostro? l, int veces) {
    late Veredicto v;
    for (var i = 0; i < veces; i++) {
      v = juez.evaluar(l);
    }
    return v;
  }

  const n = JuezDePose.cuadrosSostenidos;

  test('de frente, sostenido, dispara; un cuadro menos, no', () {
    expect(sostener(JuezDePose(PoseDeVida.frente), rostro(), n).listo, isTrue);
    expect(
      sostener(JuezDePose(PoseDeVida.frente), rostro(), n - 1).listo,
      isFalse,
    );
  });

  test('un cuadro malo en el medio reinicia la cuenta', () {
    final juez = JuezDePose(PoseDeVida.frente);
    sostener(juez, rostro(), n - 1);
    juez.evaluar(rostro(giro: 30));
    expect(sostener(juez, rostro(), n - 1).listo, isFalse);
    expect(juez.evaluar(rostro()).listo, isTrue);
  });

  test(
    'perfil izquierdo: giro positivo (hacia la izquierda de la persona)',
    () {
      expect(
        sostener(JuezDePose(PoseDeVida.izquierda), rostro(giro: 35), n).listo,
        isTrue,
      );
      final v = sostener(
        JuezDePose(PoseDeVida.izquierda),
        rostro(giro: -35),
        n,
      );
      expect(v.listo, isFalse);
      expect(v.pista, 'Girá la cabeza hacia tu izquierda.');
    },
  );

  test('perfil derecho: giro negativo', () {
    expect(
      sostener(JuezDePose(PoseDeVida.derecha), rostro(giro: -35), n).listo,
      isTrue,
    );
    expect(
      sostener(JuezDePose(PoseDeVida.derecha), rostro(giro: 35), n).listo,
      isFalse,
    );
  });

  test('bordes del giro de perfil: 28 sí, 27 no, 71 es de más', () {
    expect(
      sostener(JuezDePose(PoseDeVida.izquierda), rostro(giro: 28), n).listo,
      isTrue,
    );
    expect(
      sostener(JuezDePose(PoseDeVida.izquierda), rostro(giro: 27), n).listo,
      isFalse,
    );
    final deMas = sostener(
      JuezDePose(PoseDeVida.izquierda),
      rostro(giro: 71),
      n,
    );
    expect(deMas.pista, 'Giraste de más: volvé un poco.');
  });

  test('de frente no acepta la cabeza girada', () {
    final v = sostener(JuezDePose(PoseDeVida.frente), rostro(giro: 13), n);
    expect(v.listo, isFalse);
    expect(v.pista, 'Mirá de frente a la cámara.');
  });

  test(
    'sin cara, dos caras, lejos, descentrado: no dispara y dice por qué',
    () {
      final juez = JuezDePose(PoseDeVida.frente);
      expect(juez.evaluar(null).pista, contains('No vemos tu cara'));
      expect(
        sostener(juez, rostro(rostros: 2), n).pista,
        contains('más de una cara'),
      );
      expect(sostener(juez, rostro(ancho: 0.1), n).pista, contains('Acercá'));
      expect(sostener(juez, rostro(centroX: 0.9), n).pista, contains('Centrá'));
      expect(
        sostener(juez, rostro(inclinacion: 30), n).pista,
        contains('cabeza derecha'),
      );
    },
  );

  test('ojos cerrados de frente no dispara; sin dato de ojos, no se exige', () {
    expect(
      sostener(JuezDePose(PoseDeVida.frente), rostro(ojos: 0.1), n).listo,
      isFalse,
    );
    expect(
      sostener(JuezDePose(PoseDeVida.frente), rostro(ojos: null), n).listo,
      isTrue,
    );
  });

  test('cada pose corresponde a su cara del expediente', () {
    expect(PoseDeVida.de(PoseDeVida.izquierda.cara), PoseDeVida.izquierda);
    expect(PoseDeVida.values.length, 3);
  });
}
