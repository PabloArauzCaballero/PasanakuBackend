import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_capturas.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets(
    'solo abre el escáner después de cerrar la hoja y muestra la pauta de iOS',
    (tester) async {
      tester.view.physicalSize = const Size(412, 915);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      var llamadasAlEscaner = 0;
      var hojaCerradaAlAbrir = false;

      await tester.pumpWidget(
        ProviderScope(
          overrides: [altaProvider.overrideWith(_AltaEnCapturas.new)],
          child: MaterialApp(
            theme: temaDesde(
              Tokens.claro,
              Brightness.light,
            ).copyWith(platform: TargetPlatform.iOS),
            home: Scaffold(
              body: PasoCapturas(
                onContinuar: () {},
                capturador: (context, cara, {required listas}) async {
                  llamadasAlEscaner++;
                  hojaCerradaAlAbrir = find
                      .text('Antes de escanear')
                      .evaluate()
                      .isEmpty;
                  return null;
                },
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Tomar foto'));
      await tester.pumpAndSettle();
      expect(find.text('Antes de escanear'), findsOneWidget);
      expect(
        find.text(
          'Poné el carnet sobre una mesa lisa y de color oscuro, con buena luz '
          'y sin reflejos. El recuadro lo encuentra solo y dispara cuando lo '
          'tiene entero.',
        ),
        findsOneWidget,
      );
      expect(
        find.text(
          'Si arriba ves «Filtros», dejá «Color» o «Foto». En gris o en '
          'blanco y negro se pierden detalles que necesitamos para verificar '
          'tu carnet.',
        ),
        findsOneWidget,
      );
      expect(llamadasAlEscaner, 0);

      await tester.tap(find.text('Abrir el escáner'));
      expect(llamadasAlEscaner, 0);
      await tester.pumpAndSettle();

      expect(find.text('Antes de escanear'), findsNothing);
      expect(llamadasAlEscaner, 1);
      expect(hojaCerradaAlAbrir, isTrue);
    },
  );
}

class _AltaEnCapturas extends AltaNotifier {
  @override
  EstadoAlta build() => const EstadoAlta(paso: PasoAlta.documento);
}
