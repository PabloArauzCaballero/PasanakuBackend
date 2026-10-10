import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:flutter_test/flutter_test.dart';

Captura _captura() => const Captura(
  ruta: '/tmp/foto.jpg',
  bytes: 102400,
  ancho: 1600,
  alto: 1009,
  sha256Corto: '9f2c1e4a3b7d',
);

void main() {
  test('vacío: nada completo, falta el anverso primero', () {
    const capturas = CapturasDelExpediente();
    expect(capturas.completo, isFalse);
    expect(capturas.siguientePendiente, CaraDelCarril.anverso);
    expect(capturas.primeraFaltante, 'Falta el anverso de tu carnet.');
  });

  test('nombra la primera que falta, en el orden de Atlas', () {
    var capturas = const CapturasDelExpediente();
    capturas = capturas
        .conCaptura(CaraDelCarril.anverso, _captura())
        .conCaptura(CaraDelCarril.reverso, _captura())
        .conCaptura(CaraDelCarril.selfie, _captura());
    expect(capturas.siguientePendiente, CaraDelCarril.perfilIzquierdo);
    expect(capturas.primeraFaltante, 'Falta la selfie de tu perfil izquierdo.');
  });

  test('completo cuando están las cinco, y no antes', () {
    var capturas = const CapturasDelExpediente();
    for (final cara in CapturasDelExpediente.orden) {
      expect(capturas.completo, isFalse);
      capturas = capturas.conCaptura(cara, _captura());
    }
    expect(capturas.completo, isTrue);
    expect(capturas.siguientePendiente, isNull);
    expect(capturas.primeraFaltante, isNull);
  });

  test('quitar una captura la vuelve a dejar pendiente', () {
    var capturas = const CapturasDelExpediente();
    for (final cara in CapturasDelExpediente.orden) {
      capturas = capturas.conCaptura(cara, _captura());
    }
    capturas = capturas.sinCaptura(CaraDelCarril.reverso);
    expect(capturas.completo, isFalse);
    expect(capturas.tiene(CaraDelCarril.reverso), isFalse);
    expect(capturas.tiene(CaraDelCarril.anverso), isTrue);
  });

  test('valorApi usa guion, no la convención name.toUpperCase()', () {
    expect(CaraDelCarril.perfilIzquierdo.valorApi, 'PERFIL_IZQUIERDO');
    expect(CaraDelCarril.perfilDerecho.valorApi, 'PERFIL_DERECHO');
  });

  test('solo el carnet exige proporción; las tres pruebas de vida no', () {
    expect(CaraDelCarril.anverso.esDocumento, isTrue);
    expect(CaraDelCarril.reverso.esDocumento, isTrue);
    expect(CaraDelCarril.selfie.esDocumento, isFalse);
    expect(CaraDelCarril.perfilIzquierdo.esDocumento, isFalse);
    expect(CaraDelCarril.perfilDerecho.esDocumento, isFalse);
  });
}
