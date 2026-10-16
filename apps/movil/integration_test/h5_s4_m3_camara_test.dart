import 'dart:io';

import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_camara.dart';
import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

void main() {
  final binding = IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  testWidgets('H5.S4.M3 cámara real, captura evidencia y libera el sensor', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const PantallaDeCamara(cara: CaraDelCarril.anverso),
        ),
      ),
    );

    var lista = false;
    for (var frame = 0; frame < 80; frame++) {
      await tester.pump(const Duration(milliseconds: 250));
      final botones = tester.widgetList<Boton>(find.byType(Boton));
      if (botones.any(
        (boton) => boton.texto == 'Tomar foto' && boton.onPressed != null,
      )) {
        lista = true;
        break;
      }
    }

    expect(
      lista,
      isTrue,
      reason: 'La cámara Android no terminó de inicializarse.',
    );
    expect(find.byType(CameraPreview), findsOneWidget);
    expect(find.text('Tomar foto'), findsOneWidget);
    expect(find.text('Cancelar'), findsOneWidget);

    await binding.convertFlutterSurfaceToImage();
    await tester.pump();
    final bytes = await binding.takeScreenshot('h5-s4-m3-samsung-a06');
    final temporal = File(
      '${Directory.systemTemp.path}/h5-s4-m3-samsung-a06.png',
    );
    await temporal.writeAsBytes(bytes);
    // El driver host recoge esta imagen desde reportData y la guarda en evidencia/.
    // ignore: avoid_print
    print('H5_S4_M3_SCREENSHOT=${temporal.path}; bytes=${bytes.length}');

    await tester.tap(find.text('Cancelar'));
    await tester.pump(const Duration(seconds: 1));
  });
}
