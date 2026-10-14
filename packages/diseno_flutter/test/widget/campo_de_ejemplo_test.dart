import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_contrasena.dart';
import 'package:aportaya_diseno/atomos/campo_de_seleccion.dart';
import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/campo_o_t_p.dart';
import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets('Campo muestra el ejemplo vacío con el token del tema', (
    tester,
  ) async {
    final controller = TextEditingController();
    addTearDown(controller.dispose);
    await tester.pumpWidget(
      conTema(
        Campo(
          etiqueta: 'Nombre',
          controlador: controller,
          ejemplo: 'Ej.: María',
        ),
      ),
    );

    final textField = tester.widget<TextField>(find.byType(TextField));
    expect(textField.decoration!.hintText, 'Ej.: María');
    expect(textField.decoration!.hintStyle!.color, Tokens.claro.textoDeEjemplo);
    expect(find.text('Ej.: María'), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'María');
    await tester.pump();
    expect(
      tester.widget<EditableText>(find.byType(EditableText)).controller.text,
      'María',
    );
  });

  testWidgets('CampoContrasena pasa el ejemplo al campo de texto', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(
        const CampoContrasena(
          etiqueta: 'Clave',
          ejemplo: 'Mínimo 8 caracteres',
        ),
      ),
    );
    expect(find.text('Mínimo 8 caracteres'), findsOneWidget);
  });

  testWidgets('CampoMonto pasa el ejemplo al campo de importe', (tester) async {
    await tester.pumpWidget(
      conTema(
        CampoMonto(
          etiqueta: 'Monto',
          ejemplo: 'Ej.: 1.240,50',
          onChanged: (_) {},
        ),
      ),
    );
    expect(find.text('Ej.: 1.240,50'), findsOneWidget);
  });

  testWidgets('CampoOTP muestra la cantidad de dígitos esperada', (
    tester,
  ) async {
    await tester.pumpWidget(
      conTema(CampoOTP(ejemplo: '6 dígitos', onCompleto: (_) {})),
    );
    expect(find.text('6 dígitos'), findsOneWidget);
  });

  testWidgets('selección y fecha conservan el ejemplo vacío', (tester) async {
    await tester.pumpWidget(
      conTema(
        Column(
          children: [
            CampoDeSeleccion<String>(
              etiqueta: 'Departamento',
              opciones: const [(valor: 'LP', texto: 'La Paz', detalle: null)],
              onElegida: (_) {},
            ),
            CampoDeFecha(etiqueta: 'Fecha', valor: null, onElegida: (_) {}),
          ],
        ),
      ),
    );
    final decoraciones = tester.widgetList<InputDecorator>(
      find.byType(InputDecorator),
    );
    expect(decoraciones, isNotEmpty);
    expect(decoraciones.every((d) => d.decoration.hintText == ''), isTrue);
  });
}
