import 'dart:io';

import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/seguimiento_del_alta.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

Future<Captura> _crearCaptura(Directory dir, String nombre) async {
  final archivo = File('${dir.path}/$nombre');
  await archivo.writeAsBytes([1, 2, 3]);
  return Captura(
    ruta: archivo.path,
    bytes: 3,
    ancho: 1600,
    alto: 1009,
    sha256Corto: 'corto',
  );
}

Future<CapturasDelExpediente> _crearCinco(Directory dir) async {
  var capturas = const CapturasDelExpediente();
  for (final cara in CapturasDelExpediente.orden) {
    capturas = capturas.conCaptura(
      cara,
      await _crearCaptura(dir, '${cara.name}.jpg'),
    );
  }
  return capturas;
}

void main() {
  test('reemplazar una cara borra solo el archivo anterior', () async {
    final dir = await Directory.systemTemp.createTemp('alta-reemplazo-');
    addTearDown(() => dir.delete(recursive: true));
    final anterior = await _crearCaptura(dir, 'anverso-anterior.jpg');
    final otraCara = await _crearCaptura(dir, 'reverso.jpg');
    final nueva = await _crearCaptura(dir, 'anverso-nuevo.jpg');
    final capturas = CapturasDelExpediente(
      porCara: {
        CaraDelCarril.anverso: anterior,
        CaraDelCarril.reverso: otraCara,
      },
    );

    final reemplazadas = await capturas.reemplazarCaptura(
      CaraDelCarril.anverso,
      nueva,
    );

    expect(await File(anterior.ruta).exists(), isFalse);
    expect(await File(otraCara.ruta).exists(), isTrue);
    expect(await File(nueva.ruta).exists(), isTrue);
    expect(reemplazadas.porCara[CaraDelCarril.anverso]!.ruta, nueva.ruta);
  });

  test(
    'abandonar el alta borra capturas locales y vacía el formulario',
    () async {
      final dir = await Directory.systemTemp.createTemp('alta-abandono-');
      addTearDown(() => dir.delete(recursive: true));
      final capturas = await _crearCinco(dir);
      final archivos = capturas.porCara.values
          .map((c) => File(c.ruta))
          .toList();
      final contenedor = ProviderContainer();
      addTearDown(contenedor.dispose);
      final alta = contenedor.read(altaProvider.notifier);
      for (final entrada in capturas.porCara.entries) {
        await alta.registrarCaptura(entrada.key, entrada.value);
      }

      await alta.abandonar();

      expect(contenedor.read(altaProvider).capturas.porCara, isEmpty);
      expect(archivos.every((archivo) => !archivo.existsSync()), isTrue);
    },
  );

  test(
    'limpiar seguimiento tras subir borra temporales y elimina rutas',
    () async {
      final dir = await Directory.systemTemp.createTemp('alta-subida-limpia-');
      addTearDown(() => dir.delete(recursive: true));
      final capturas = await _crearCinco(dir);
      final archivos = capturas.porCara.values
          .map((c) => File(c.ruta))
          .toList();
      final contenedor = ProviderContainer();
      addTearDown(contenedor.dispose);
      final seguimiento = contenedor.read(seguimientoDelAltaProvider.notifier)
        ..fijar('usuario-temporal', capturas);

      await seguimiento.limpiar();

      expect(contenedor.read(seguimientoDelAltaProvider), isNull);
      expect(archivos.every((archivo) => !archivo.existsSync()), isTrue);
    },
  );

  test('mismo archivo al reemplazar se conserva', () async {
    final dir = await Directory.systemTemp.createTemp('alta-misma-foto-');
    addTearDown(() => dir.delete(recursive: true));
    final captura = await _crearCaptura(dir, 'misma.jpg');
    final capturas = CapturasDelExpediente(
      porCara: {CaraDelCarril.anverso: captura},
    );

    await capturas.reemplazarCaptura(CaraDelCarril.anverso, captura);

    expect(await File(captura.ruta).exists(), isTrue);
  });

  test(
    'la representación de Captura para diagnóstico no revela la ruta',
    () async {
      final dir = await Directory.systemTemp.createTemp('alta-ruta-privada-');
      addTearDown(() => dir.delete(recursive: true));
      final captura = await _crearCaptura(dir, 'privada.jpg');

      expect(captura.toString(), isNot(contains(captura.ruta)));
    },
  );
}
