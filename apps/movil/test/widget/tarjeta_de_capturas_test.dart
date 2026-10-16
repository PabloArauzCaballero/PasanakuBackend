import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/aviso_de_permiso_camara.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  for (final bloqueado in [false, true]) {
    testWidgets(
      'permiso denegado (${bloqueado ? 'bloqueado' : 'reintentable'}) ofrece galería sin captura manual',
      (tester) async {
        var accionCamara = 0;
        var galeria = 0;
        await tester.pumpWidget(
          MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            home: Scaffold(
              body: AvisoDePermisoCamara(
                bloqueadoParaSiempre: bloqueado,
                onAccion: () => accionCamara++,
                onElegirDeLasFotos: () => galeria++,
              ),
            ),
          ),
        );

        expect(
          find.text(bloqueado ? 'Abrir ajustes' : 'Permitir cámara'),
          findsOneWidget,
        );
        expect(find.text('Elegir de la galería'), findsOneWidget);
        expect(find.text('Escribir a mano'), findsNothing);

        await tester.tap(find.text('Elegir de la galería'));
        await tester.tap(
          find.text(bloqueado ? 'Abrir ajustes' : 'Permitir cámara'),
        );
        expect(galeria, 1);
        expect(accionCamara, 1);
      },
    );
  }
}
