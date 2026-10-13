import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/aviso_de_permiso_camara.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  for (final bloqueado in [false, true]) {
    testWidgets(
      'prueba de vida explica el permiso (${bloqueado ? 'bloqueado' : 'denegado'})',
      (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            home: Scaffold(
              backgroundColor: Paleta.ink,
              body: AvisoDePermisoCamara(
                soloPruebaDeVida: true,
                bloqueadoParaSiempre: bloqueado,
                onAccion: () {},
                onElegirDeLasFotos: () {},
              ),
            ),
          ),
        );

        expect(find.text('Necesitamos tu cámara'), findsOneWidget);
        expect(
          find.text('Solo se usa para tu prueba de vida.'),
          findsOneWidget,
        );
        expect(
          find.text(
            'La foto queda asociada únicamente a tu expediente. No accedemos a tu galería.',
          ),
          findsOneWidget,
        );
        expect(
          find.text('Abrí los ajustes del teléfono para permitirla.'),
          bloqueado ? findsOneWidget : findsNothing,
        );
        expect(
          find.text(bloqueado ? 'Abrir ajustes' : 'Permitir cámara'),
          findsOneWidget,
        );
        expect(find.text('Elegir de la galería'), findsNothing);
      },
    );
  }
}
