import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/seguimiento_del_alta.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

Captura _captura(String ruta) => Captura(
  ruta: ruta,
  bytes: 102400,
  ancho: 1600,
  alto: 1009,
  sha256Corto: '9f2c1e4a3b7d',
);

void main() {
  test(
    'las cinco capturas siguen disponibles después de reiniciar el alta',
    () {
      final contenedor = ProviderContainer();
      addTearDown(contenedor.dispose);
      final alta = contenedor.read(altaProvider.notifier);
      for (final cara in CapturasDelExpediente.orden) {
        alta.registrarCaptura(cara, _captura('/tmp/${cara.name}.jpg'));
      }

      // El mismo orden que `pantalla_registro.dart` tras `POST /usuarios`.
      contenedor
          .read(seguimientoDelAltaProvider.notifier)
          .fijar('usuario-1', contenedor.read(altaProvider).capturas);
      alta.reiniciar();

      expect(contenedor.read(altaProvider).capturas.porCara, isEmpty);
      final seguimiento = contenedor.read(seguimientoDelAltaProvider)!;
      expect(seguimiento.usuarioId, 'usuario-1');
      expect(seguimiento.capturas.completo, isTrue);
    },
  );

  test('repetir una foto reemplaza solo esa cara', () {
    final contenedor = ProviderContainer();
    addTearDown(contenedor.dispose);
    var capturas = const CapturasDelExpediente();
    for (final cara in CapturasDelExpediente.orden) {
      capturas = capturas.conCaptura(cara, _captura('/tmp/${cara.name}.jpg'));
    }
    final notifier = contenedor.read(seguimientoDelAltaProvider.notifier)
      ..fijar('usuario-1', capturas);

    notifier.reemplazarCaptura(
      CaraDelCarril.selfie,
      _captura('/tmp/nueva.jpg'),
    );

    final porCara = contenedor
        .read(seguimientoDelAltaProvider)!
        .capturas
        .porCara;
    expect(porCara[CaraDelCarril.selfie]!.ruta, '/tmp/nueva.jpg');
    expect(porCara[CaraDelCarril.anverso]!.ruta, '/tmp/anverso.jpg');
  });

  test('sin seguimiento, reemplazar no inventa uno', () {
    final contenedor = ProviderContainer();
    addTearDown(contenedor.dispose);
    contenedor
        .read(seguimientoDelAltaProvider.notifier)
        .reemplazarCaptura(CaraDelCarril.selfie, _captura('/tmp/x.jpg'));
    expect(contenedor.read(seguimientoDelAltaProvider), isNull);
  });
}
