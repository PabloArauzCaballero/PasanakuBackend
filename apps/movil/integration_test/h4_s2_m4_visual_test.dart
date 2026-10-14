import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/campos_de_nombre.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('H4.S2.M4 nombres responden al ancho Android real', (
    tester,
  ) async {
    final controladores = List.generate(4, (_) => TextEditingController());
    final focos = List.generate(4, (_) => FocusNode());
    addTearDown(() {
      for (final controlador in controladores) {
        controlador.dispose();
      }
      for (final foco in focos) {
        foco.dispose();
      }
    });

    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          body: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: CamposDeNombre(
              controladores: controladores,
              focos: focos,
              errores: const {},
              onCambio: (_) {},
            ),
          ),
        ),
      ),
    );

    final campos = find.byType(TextField);
    expect(campos, findsNWidgets(4));
    final ancho = MediaQuery.sizeOf(tester.element(campos.first)).width;
    final posiciones = [
      for (var indice = 0; indice < 4; indice++)
        tester.getRect(campos.at(indice)),
    ];
    if (ancho < 400) {
      expect(posiciones[1].top, greaterThan(posiciones[0].top));
      expect(posiciones[3].top, greaterThan(posiciones[2].top));
    } else {
      expect(posiciones[1].top, equals(posiciones[0].top));
      expect(posiciones[3].top, equals(posiciones[2].top));
      expect(posiciones[2].top, greaterThan(posiciones[1].top));
    }

    // La pausa breve permite guardar una captura Android completa durante la
    // prueba, con barras del sistema incluidas, en cada tamaño configurado.
    // ignore: avoid_print
    print('H4_S2_M4_CAPTURE_READY width=$ancho fields=$posiciones');
    await tester.runAsync(
      () => Future<void>.delayed(const Duration(seconds: 20)),
    );
  });
}
