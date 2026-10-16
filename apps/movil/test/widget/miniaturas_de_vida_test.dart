import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/juez_de_pose.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/miniaturas_de_vida.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('anuncia cada estado y solo permite repetir la rechazada', (
    tester,
  ) async {
    final semantica = tester.ensureSemantics();
    var repetida = false;
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          backgroundColor: Paleta.ink,
          body: MiniaturasDeVida(
            poses: PoseDeVida.values,
            capturas: const {
              CaraDelCarril.selfie: Captura(
                ruta: '/no-existe-selfie.png',
                bytes: 0,
                ancho: 1500,
                alto: 1600,
                sha256Corto: 'test',
              ),
            },
            rechazadas: {CaraDelCarril.perfilIzquierdo},
            onRepetir: (_) => repetida = true,
          ),
        ),
      ),
    );

    expect(find.bySemanticsLabel('Frente: Lista'), findsOneWidget);
    expect(find.bySemanticsLabel('Perfil izquierdo: Repetir'), findsOneWidget);
    expect(find.bySemanticsLabel('Perfil derecho: Pendiente'), findsOneWidget);
    expect(
      find.byWidgetPredicate(
        (widget) =>
            widget is Image &&
            widget.image is ResizeImage &&
            (widget.image as ResizeImage).width == 228,
      ),
      findsOneWidget,
    );
    final repetir = find.bySemanticsLabel('Perfil izquierdo: Repetir');
    expect(tester.getSemantics(repetir).flagsCollection.isButton, isTrue);
    expect(
      tester
          .getSemantics(find.bySemanticsLabel('Perfil derecho: Pendiente'))
          .flagsCollection
          .isButton,
      isFalse,
    );

    await tester.tap(repetir);
    expect(repetida, isTrue);
    semantica.dispose();
  });
}
