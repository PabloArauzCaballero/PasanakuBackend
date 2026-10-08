import 'package:aportaya_movil/dominio/puertos/camara.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/camara_identidad.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_captura.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets(
    'si se deniega la cámara, explica el fallo y permite completar el documento a mano',
    (tester) async {
      var continuaciones = 0;
      await _abrir(
        tester,
        camara: _Camara(alCapturar: () async => null),
        esDocumento: true,
        permitirEscribirAMano: true,
        onContinuar: () => continuaciones++,
      );

      await tester.tap(find.text('Tomar foto'));
      await tester.pumpAndSettle();

      expect(find.text(TextosIdentidad.sinCamara), findsOneWidget);
      expect(find.text(TextosIdentidad.reintentarCaptura), findsOneWidget);
      expect(find.text(TextosIdentidad.elegirDeLasFotos), findsOneWidget);
      expect(find.text(TextosIdentidad.escribirAMano), findsOneWidget);
      expect(continuaciones, 0);

      await tester.tap(find.text(TextosIdentidad.escribirAMano));
      await tester.pumpAndSettle();
      await tester.tap(find.text(TextosIdentidad.continuar));
      expect(continuaciones, 1);
    },
  );

  testWidgets(
    'la prueba de vida no ofrece captura manual, pero admite una foto existente',
    (tester) async {
      var continuaciones = 0;
      final camara = _Camara(
        alCapturar: () async => null,
        elegir: () async => '/tmp/selfie.jpg',
      );
      await _abrir(
        tester,
        camara: camara,
        esDocumento: false,
        permitirEscribirAMano: false,
        onContinuar: () => continuaciones++,
      );

      await tester.tap(find.text('Tomar foto'));
      await tester.pumpAndSettle();
      expect(find.text(TextosIdentidad.sinCamara), findsOneWidget);
      expect(find.text(TextosIdentidad.escribirAMano), findsNothing);

      await tester.tap(
        find.widgetWithText(FilledButton, TextosIdentidad.elegirDeLasFotos),
      );
      await tester.pumpAndSettle();
      expect(camara.fotosElegidas, 1);
      expect(find.text('Continuar'), findsOneWidget);
      await tester.tap(find.text('Continuar'));
      expect(continuaciones, 1);
    },
  );

  testWidgets('una excepción del plugin de cámara se trata como denegación', (
    tester,
  ) async {
    await _abrir(
      tester,
      camara: _Camara(alCapturar: () async => throw StateError('plugin')),
      esDocumento: true,
      permitirEscribirAMano: true,
      onContinuar: () {},
    );

    await tester.tap(find.text('Tomar foto'));
    await tester.pumpAndSettle();
    expect(find.text(TextosIdentidad.sinCamara), findsOneWidget);
  });
}

Future<void> _abrir(
  WidgetTester tester, {
  required Camara camara,
  required bool esDocumento,
  required bool permitirEscribirAMano,
  required VoidCallback onContinuar,
}) async {
  String? rutaActual;
  await tester.pumpWidget(
    ProviderScope(
      overrides: [camaraProvider.overrideWithValue(camara)],
      child: MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          body: StatefulBuilder(
            builder: (context, setState) {
              return SingleChildScrollView(
                child: PasoCaptura(
                  titulo: esDocumento
                      ? 'Frente'
                      : TextosIdentidad.capturarSelfie,
                  esDocumento: esDocumento,
                  permitirEscribirAMano: permitirEscribirAMano,
                  rutaActual: rutaActual,
                  onCapturado: (nuevaRuta) =>
                      setState(() => rutaActual = nuevaRuta),
                  onContinuar: onContinuar,
                ),
              );
            },
          ),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

class _Camara implements Camara {
  _Camara({required this.alCapturar, this.elegir});

  final Future<String?> Function() alCapturar;
  final Future<String?> Function()? elegir;
  var fotosElegidas = 0;

  @override
  Future<String?> capturar({required bool esDocumento}) => alCapturar();

  @override
  Future<String?> elegirDeLasFotos() async {
    fotosElegidas++;
    return elegir?.call();
  }
}
