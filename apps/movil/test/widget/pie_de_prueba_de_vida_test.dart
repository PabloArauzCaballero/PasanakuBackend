import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/juez_de_pose.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/pie_de_prueba_de_vida.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_captura.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  Future<void> montar(
    WidgetTester tester, {
    String? pista,
    bool tomada = false,
    bool finalizada = false,
  }) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          backgroundColor: Paleta.ink,
          body: PieDePruebaDeVida(
            pista: pista,
            progreso: 0.4,
            rechazo: null,
            manual: true,
            tomando: false,
            tomada: tomada,
            finalizada: finalizada,
            onTomarFoto: () {},
            onContinuar: () {},
            onCancelar: () {},
          ),
        ),
      ),
    );
  }

  testWidgets('cuenta deja visible siempre la toma manual', (tester) async {
    await montar(
      tester,
      pista: TextosDeCaptura.instruccionDuranteCuenta(PoseDeVida.frente),
    );
    expect(
      find.text(
        'Poné tu cara dentro del óvalo. Quedate quieto: la foto sale sola.',
      ),
      findsOneWidget,
    );
    expect(find.text('Tomar ahora'), findsOneWidget);
  });

  testWidgets('foto tomada muestra la indicación siguiente', (tester) async {
    await montar(tester, tomada: true);
    expect(find.text('¡Foto tomada!'), findsOneWidget);
    expect(find.text('Seguí la siguiente indicación.'), findsOneWidget);
    expect(find.text('Tomar ahora'), findsNothing);
  });

  testWidgets('fase completa presenta el mensaje y el botón Continuar', (
    tester,
  ) async {
    await montar(tester, finalizada: true);
    expect(find.text('¡Listo! Tus tres fotos están'), findsOneWidget);
    expect(find.text('Continuar'), findsOneWidget);
  });
}
