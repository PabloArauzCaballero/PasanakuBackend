import 'dart:math';

import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/validaciones.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// 2026-10-19: el usuario escribió una contraseña de puros espacios y el login la tomó
/// como válida. Ahora «Ingresar» queda apagado y el campo dice por qué.
void main() {
  group('reglas', () {
    test('correcto: una contraseña común pasa', () {
      expect(contrasenaDeIngresoValida('Clave-Segura-123'), isTrue);
      expect(errorContrasena('Clave-Segura-123'), isNull);
    });

    test('límite: una contraseña nueva no lleva espacios en ningún lado; al '
        'ingresar no se le rechaza a quien ya la tenía', () {
      expect(contrasenaDeIngresoValida('mi clave 2026'), isTrue);
      expect(errorContrasena('Clave segura 123'), isNotNull);
      expect(errorContrasena(' Clave123'), isNotNull);
      expect(errorContrasena('Clave123 '), isNotNull);
    });

    test('la sugerencia siempre es una contraseña válida', () {
      for (var semilla = 0; semilla < 200; semilla++) {
        final s = sugerirContrasena(Random(semilla));
        expect(errorContrasena(s), isNull, reason: s);
        expect(tieneEspacios(s), isFalse, reason: s);
      }
      expect(
        errorEspaciosEnClave('mi clave', sugerencia: 'Turno-4821-Meta'),
        contains('Turno-4821-Meta'),
      );
      expect(errorEspaciosEnClave('MiClave2026', sugerencia: 'x'), isNull);
    });

    test('inválido: puros espacios no son una contraseña', () {
      expect(contrasenaDeIngresoValida('        '), isFalse);
      expect(contrasenaDeIngresoValida(' ' * 20), isFalse);
      expect(errorContrasenaDeIngreso('        '), errorSoloEspacios);
      expect(errorContrasena('         '), errorSoloEspacios);
      // Vacía todavía no es un error: la persona no escribió nada.
      expect(errorContrasenaDeIngreso(''), isNull);
    });
  });

  testWidgets('la pantalla: con ocho espacios «Ingresar» no se habilita y el '
      'campo lo dice', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    await tester.pumpWidget(conApp(const PantallaDeSesion(), dio: dio));
    await tester.pump();
    final campos = find.byType(EditableText);
    await tester.enterText(campos.at(0), '70000001');
    await tester.enterText(campos.at(1), '        ');
    await tester.pump();

    final ingresar = tester.widget<Boton>(
      find.widgetWithText(Boton, 'Ingresar'),
    );
    expect(ingresar.onPressed, isNull);
    expect(find.text(errorSoloEspacios), findsOneWidget);
  });
}
