import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/pautas_de_fotos.dart';
import 'package:aportaya_movil/pantallas/identidad/ilustraciones/ilustracion_de_pauta.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('golden de las nueve escenas de ejemplo', (tester) async {
    tester.view.physicalSize = const Size(520, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        debugShowCheckedModeBanner: false,
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          body: GridView.count(
            padding: const EdgeInsets.all(Espacio.s3),
            crossAxisCount: 2,
            childAspectRatio: 2,
            mainAxisSpacing: Espacio.s3,
            crossAxisSpacing: Espacio.s3,
            children: [
              for (final escena in EscenaDePauta.values)
                IlustracionDePauta(escena: escena),
            ],
          ),
        ),
      ),
    );
    await expectLater(
      find.byType(GridView),
      matchesGoldenFile('goldens/ilustracion_de_pauta_escenas.png'),
    );
  });
}
