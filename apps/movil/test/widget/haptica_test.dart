import 'package:aportaya_diseno/atomos/barra_de_puntos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/moviles/alcance_haptica.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/infraestructura/haptica_flutter.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('el proveedor mapea acciones semánticas al canal de plataforma', (
    tester,
  ) async {
    final argumentos = <Object?>[];
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
      SystemChannels.platform,
      (llamada) async {
        if (llamada.method == 'HapticFeedback.vibrate') {
          argumentos.add(llamada.arguments);
        }
        return null;
      },
    );
    addTearDown(
      () => tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        SystemChannels.platform,
        null,
      ),
    );

    final haptica = HapticaFlutter();
    await haptica.seleccion();
    await haptica.botonPrimario();
    await haptica.avisoDeCredencialesIncorrectas();
    await haptica.exito();
    await haptica.errorQueBloqueaDinero();

    expect(argumentos, [
      'HapticFeedbackType.selectionClick',
      'HapticFeedbackType.lightImpact',
      'HapticFeedbackType.lightImpact',
      'HapticFeedbackType.mediumImpact',
      'HapticFeedbackType.heavyImpact',
    ]);
  });

  testWidgets('solo primarios vibran; elecciones y puntos usan selección', (
    tester,
  ) async {
    final argumentos = <Object?>[];
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
      SystemChannels.platform,
      (llamada) async {
        if (llamada.method == 'HapticFeedback.vibrate') {
          argumentos.add(llamada.arguments);
        }
        return null;
      },
    );
    addTearDown(
      () => tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        SystemChannels.platform,
        null,
      ),
    );

    await tester.pumpWidget(
      AlcanceHaptica(
        haptica: HapticaFlutter(),
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(
            body: Column(
              children: [
                Boton(
                  texto: 'Primario',
                  variante: BotonVariante.primario,
                  onPressed: () {},
                ),
                Boton(
                  texto: 'Secundario',
                  variante: BotonVariante.secundario,
                  onPressed: () {},
                ),
                SelectorSegmentado<String>(
                  opciones: const {'a': 'A', 'b': 'B'},
                  valor: 'a',
                  onChanged: (_) {},
                ),
                BarraDePuntos(total: 2, actual: 0, onElegir: (_) {}),
              ],
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Primario'));
    await tester.pump();
    await tester.tap(find.text('Secundario'));
    await tester.pump();
    await tester.tap(find.text('B'));
    await tester.pump();
    await tester.tap(find.bySemanticsLabel('Lámina 2 de 2'));
    await tester.pump();

    expect(argumentos, [
      'HapticFeedbackType.lightImpact',
      'HapticFeedbackType.selectionClick',
      'HapticFeedbackType.selectionClick',
    ]);
  });
}
