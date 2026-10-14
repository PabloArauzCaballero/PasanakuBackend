import 'package:aportaya_movil/pantallas/identidad/dominio/ciclo_de_pose.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/juez_de_pose.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  final inicio = DateTime.utc(2026, 10, 12);
  DateTime en(int milisegundos) =>
      inicio.add(Duration(milliseconds: milisegundos));

  Veredicto? veredicto({bool correcto = false, String? pista}) => correcto
      ? const Veredicto(listo: false, pista: 'Quedate así…', progreso: 0.25)
      : Veredicto(
          listo: false,
          pista: pista ?? 'No vemos tu cara: ponela dentro del marco.',
        );

  test('1. respira durante los primeros 1600 ms, sin cuenta', () {
    final ciclo = CicloDePose(inicio: inicio, hayDetector: true);
    for (final t in [0, 800, 1599]) {
      final estado = ciclo.avanzar(en(t), veredicto: veredicto(correcto: true));
      expect(estado.fase, FaseDePose.respiro);
      expect(estado.segundos, isNull);
    }
  });

  test('2. con detector y pose incorrecta, busca y muestra la pista', () {
    final ciclo = CicloDePose(inicio: inicio, hayDetector: true);
    final estado = ciclo.avanzar(en(1600), veredicto: veredicto());
    expect(estado.fase, FaseDePose.buscando);
    expect(estado.segundos, isNull);
    expect(estado.pista, contains('No vemos tu cara'));
  });

  test('3. pose correcta inicia cuenta atrás en 3', () {
    final ciclo = CicloDePose(inicio: inicio, hayDetector: true);
    final estado = ciclo.avanzar(
      en(2000),
      veredicto: veredicto(correcto: true),
    );
    expect(estado.fase, FaseDePose.cuentaAtras);
    expect(estado.segundos, 3);
    expect(estado.progreso, 0);
  });

  test(
    'un tic de reloj conserva la última pose leída mientras sigue fresca',
    () {
      final ciclo = CicloDePose(inicio: inicio, hayDetector: true);
      ciclo.avanzar(en(2000), veredicto: veredicto(correcto: true));
      final tic = ciclo.avanzar(en(2500));
      expect(tic.fase, FaseDePose.cuentaAtras);
      expect(tic.progreso, greaterThan(0));
      expect(tic.progreso, lessThan(1));
    },
  );

  test('4. pose sostenida tres segundos dispara una sola vez', () {
    final ciclo = CicloDePose(inicio: inicio, hayDetector: true);
    ciclo.avanzar(en(2000), veredicto: veredicto(correcto: true));
    final tomada = ciclo.avanzar(
      en(5000),
      veredicto: veredicto(correcto: true),
    );
    expect(tomada.disparar, isTrue);
    expect(tomada.origen, OrigenDeFoto.automatica);
    expect(ciclo.avanzar(en(5001)).disparar, isFalse);
  });

  test('5. perder la pose borra la cuenta y vuelve a buscar', () {
    final ciclo = CicloDePose(inicio: inicio, hayDetector: true);
    ciclo.avanzar(en(2000), veredicto: veredicto(correcto: true));
    final estado = ciclo.avanzar(en(3500), veredicto: veredicto());
    expect(estado.fase, FaseDePose.buscando);
    expect(estado.segundos, isNull);
    expect(estado.progreso, 0);
  });

  test('6. al recuperar la pose, reinicia la cuenta desde tres', () {
    final ciclo = CicloDePose(inicio: inicio, hayDetector: true);
    ciclo.avanzar(en(2000), veredicto: veredicto(correcto: true));
    ciclo.avanzar(en(3500), veredicto: veredicto());
    final estado = ciclo.avanzar(
      en(4000),
      veredicto: veredicto(correcto: true),
    );
    expect(estado.fase, FaseDePose.cuentaAtras);
    expect(estado.segundos, 3);
    expect(estado.progreso, 0);
  });

  test(
    '7. detector activo sin pose correcta nunca dispara automáticamente',
    () {
      final ciclo = CicloDePose(inicio: inicio, hayDetector: true);
      for (var t = 1600; t <= 60000; t += 1000) {
        final estado = ciclo.avanzar(en(t), veredicto: veredicto());
        expect(estado.disparar, isFalse);
        expect(estado.fase, FaseDePose.buscando);
      }
      expect(ciclo.hayDetector, isTrue);
    },
  );

  test(
    '8. sin detector empieza la cuenta tras el respiro y dispara a los 4600 ms',
    () {
      final ciclo = CicloDePose(inicio: inicio, hayDetector: false);
      final cuenta = ciclo.avanzar(en(1600));
      expect(cuenta.fase, FaseDePose.cuentaAtras);
      expect(cuenta.segundos, 3);
      expect(ciclo.avanzar(en(4599)).disparar, isFalse);
      final tomada = ciclo.avanzar(en(4600));
      expect(tomada.disparar, isTrue);
      expect(tomada.origen, OrigenDeFoto.automatica);
    },
  );

  test(
    '9. detector silencioso dos segundos tras el último veredicto pasa a modo sin detector',
    () {
      final ciclo = CicloDePose(inicio: inicio, hayDetector: true);
      ciclo.avanzar(en(1600), veredicto: veredicto());
      final cuenta = ciclo.avanzar(en(3600));
      expect(ciclo.hayDetector, isFalse);
      expect(cuenta.fase, FaseDePose.cuentaAtras);
      expect(cuenta.segundos, 3);
      expect(ciclo.avanzar(en(6600)).disparar, isTrue);
    },
  );

  test(
    '10. tomar ahora dispara manualmente desde respiro, búsqueda y cuenta',
    () {
      final enRespiro = CicloDePose(inicio: inicio, hayDetector: true);
      final manualRespiro = enRespiro.tomarAhora(en(0));
      expect(manualRespiro.disparar, isTrue);
      expect(manualRespiro.origen, OrigenDeFoto.manual);

      final enBusqueda = CicloDePose(inicio: inicio, hayDetector: true);
      enBusqueda.avanzar(en(1600), veredicto: veredicto());
      expect(enBusqueda.tomarAhora(en(1700)).origen, OrigenDeFoto.manual);

      final enCuenta = CicloDePose(inicio: inicio, hayDetector: true);
      enCuenta.avanzar(en(2000), veredicto: veredicto(correcto: true));
      expect(enCuenta.tomarAhora(en(2100)).origen, OrigenDeFoto.manual);
    },
  );

  test('11. foto tomada permanece 1100 ms y luego termina', () {
    final ciclo = CicloDePose(inicio: inicio, hayDetector: false);
    ciclo.tomarAhora(en(100));
    expect(ciclo.avanzar(en(1199)).fase, FaseDePose.tomada);
    final terminada = ciclo.avanzar(en(1200));
    expect(terminada.fase, FaseDePose.terminada);
    expect(terminada.disparar, isFalse);
    expect(terminada.origen, OrigenDeFoto.manual);
  });

  test(
    '12. reloj que retrocede reinicia la cuenta y no dispara antes de tiempo',
    () {
      final ciclo = CicloDePose(inicio: inicio, hayDetector: true);
      ciclo.avanzar(en(2000), veredicto: veredicto(correcto: true));
      ciclo.avanzar(en(3000), veredicto: veredicto(correcto: true));
      final reiniciada = ciclo.avanzar(
        en(1900),
        veredicto: veredicto(correcto: true),
      );
      expect(reiniciada.fase, FaseDePose.cuentaAtras);
      expect(reiniciada.segundos, 3);
      expect(
        ciclo.avanzar(en(4899), veredicto: veredicto(correcto: true)).disparar,
        isFalse,
      );
      expect(
        ciclo.avanzar(en(4900), veredicto: veredicto(correcto: true)).disparar,
        isTrue,
      );
    },
  );
}
