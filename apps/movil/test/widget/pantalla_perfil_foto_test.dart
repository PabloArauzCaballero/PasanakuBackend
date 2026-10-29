import 'dart:convert';
import 'dart:io';

import 'package:aportaya_movil/dominio/puertos/camara.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/camara_identidad.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_perfil.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

const _foto = '/usuarios/yo/foto-de-perfil';
const _perfil = '/usuarios/yo/perfil';

/// PNG de 1x1 sintético y válido (un píxel transparente): no es la cara de nadie.
final _png = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==',
);

/// La subida lee el archivo con E/S real, que no avanza dentro del reloj simulado de
/// `testWidgets`: se la deja correr con `runAsync` y recién después se asienta.
Future<void> _dejarSubir(WidgetTester tester) async {
  for (var i = 0; i < 10; i++) {
    await tester.runAsync(
      () => Future<void>.delayed(const Duration(milliseconds: 10)),
    );
    await tester.pump();
  }
  await asentar(tester);
}

/// Doble de la cámara: devuelve una foto ya escrita en disco, o `null` (cancelar).
class _CamaraDePrueba implements Camara {
  _CamaraDePrueba(this.ruta);
  final String? ruta;

  @override
  Future<String?> capturar({required bool esDocumento}) async => ruta;

  @override
  Future<String?> elegirDeLasFotos() async => ruta;
}

/// El `dioSimulado` compartido empareja solo por ruta: un `onPut` respondería también
/// el GET de la misma ruta. Acá importa el método, así que se monta, sobre el MISMO
/// Dio (con sus interceptores), un adaptador que también lo compara.
({Dio dio, DioAdapter adaptador}) _dioPorMetodo() {
  final (:dio, adaptador: _) = dioSimulado();
  final adaptador = DioAdapter(
    dio: dio,
    matcher: const UrlRequestMatcher(matchMethod: true),
  );
  _perfilDeAna(adaptador);
  return (dio: dio, adaptador: adaptador);
}

void _perfilDeAna(DioAdapter adaptador) => adaptador.onGet(
  _perfil,
  (s) => s.reply(200, {
    'usuarioId': 'b8000000-0000-4000-8000-000000000077',
    'correo': 'ana@correo.test',
    'correoVerificado': true,
    'nombreCompleto': 'Ana Maria Mendoza',
  }),
);

Future<String> _fotoEnDisco() async {
  final dir = await Directory.systemTemp.createTemp('foto-perfil');
  final f = File('${dir.path}/yo.png');
  await f.writeAsBytes(_png);
  return f.path;
}

CircleAvatar _avatar(WidgetTester tester) => tester.widget<CircleAvatar>(
  find.descendant(
    of: find.byKey(const ValueKey('perfil-avatar')),
    matching: find.byType(CircleAvatar),
  ),
);

void main() {
  testWidgets('límite: sin foto (404) se ven las iniciales y «Agregar foto»', (
    tester,
  ) async {
    final (:dio, :adaptador) = _dioPorMetodo();
    adaptador.onGet(_foto, (s) => s.reply(404, null));

    await tester.pumpWidget(conApp(const PantallaDePerfil(), dio: dio));
    await asentar(tester);

    expect(_avatar(tester).backgroundImage, isNull);
    expect(find.text('AM'), findsOneWidget);
    expect(find.text('Agregar foto'), findsOneWidget);
  });

  testWidgets('correcto: elige una foto, la sube con PUT y la muestra', (
    tester,
  ) async {
    final (:dio, :adaptador) = _dioPorMetodo();
    final ruta = await tester.runAsync(_fotoEnDisco);
    adaptador.onGet(_foto, (s) => s.reply(404, null));
    adaptador.onPut(_foto, (s) => s.reply(204, null), data: Matchers.any);

    await tester.pumpWidget(
      conApp(
        const PantallaDePerfil(),
        dio: dio,
        extra: [camaraProvider.overrideWithValue(_CamaraDePrueba(ruta))],
      ),
    );
    await asentar(tester);
    await tester.tap(find.text('Agregar foto'));
    await asentar(tester);
    await tester.tap(find.byKey(const ValueKey('foto-perfil-galeria')));
    // Desde ahora el servidor ya tiene la foto: la relectura tras el PUT la trae.
    adaptador.onGet(
      _foto,
      (s) => s.reply(
        200,
        _png,
        headers: {
          'content-type': ['image/png'],
        },
      ),
    );
    await _dejarSubir(tester);

    expect(_avatar(tester).backgroundImage, isA<MemoryImage>());
    expect(find.text('Cambiar foto'), findsOneWidget);
    expect(find.byKey(const ValueKey('foto-perfil-error')), findsNothing);
  });

  testWidgets('inválido: si el servidor la rechaza (422) se dice y no se '
      'muestra una foto que no quedó', (tester) async {
    final (:dio, :adaptador) = _dioPorMetodo();
    final ruta = await tester.runAsync(_fotoEnDisco);
    adaptador.onGet(_foto, (s) => s.reply(404, null));
    adaptador.onPut(
      _foto,
      (s) => s.reply(422, {
        'codigo': 'AP-DOM-01',
        'mensaje': 'Ese tipo de archivo no se admite aca: application/pdf',
        'trazaId': 'traza-de-prueba',
      }),
      data: Matchers.any,
    );

    await tester.pumpWidget(
      conApp(
        const PantallaDePerfil(),
        dio: dio,
        extra: [camaraProvider.overrideWithValue(_CamaraDePrueba(ruta))],
      ),
    );
    await asentar(tester);
    await tester.tap(find.text('Agregar foto'));
    await asentar(tester);
    await tester.tap(find.byKey(const ValueKey('foto-perfil-camara')));
    await _dejarSubir(tester);

    expect(find.byKey(const ValueKey('foto-perfil-error')), findsOneWidget);
    expect(_avatar(tester).backgroundImage, isNull);
    expect(find.text('Agregar foto'), findsOneWidget);
  });

  testWidgets('cancelar la cámara no sube nada ni muestra error', (
    tester,
  ) async {
    final (:dio, :adaptador) = _dioPorMetodo();
    adaptador.onGet(_foto, (s) => s.reply(404, null));

    await tester.pumpWidget(
      conApp(
        const PantallaDePerfil(),
        dio: dio,
        extra: [camaraProvider.overrideWithValue(_CamaraDePrueba(null))],
      ),
    );
    await asentar(tester);
    await tester.tap(find.text('Agregar foto'));
    await asentar(tester);
    await tester.tap(find.byKey(const ValueKey('foto-perfil-camara')));
    await asentar(tester);

    expect(find.byKey(const ValueKey('foto-perfil-error')), findsNothing);
    expect(find.text('Agregar foto'), findsOneWidget);
  });
}
