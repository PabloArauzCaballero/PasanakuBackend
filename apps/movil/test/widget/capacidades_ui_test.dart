import 'package:aportaya_movil/infraestructura/capacidades.dart';
import 'package:aportaya_movil/pantallas/soporte/boton_segun_capacidad.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// H2.S2.M2 (madre H6.S2.M2) -- con plataforma iOS forzada (acá, con el `grado`
/// directo: `BotonSegunCapacidad` no pregunta la plataforma, solo el grado que le
/// pasan -- ver `capacidades_test.dart` para la resolución por plataforma), el botón
/// de una acción no soportada queda deshabilitado y el motivo es legible tanto en
/// pantalla como por un lector (`Semantics.hint`).
void main() {
  Widget envolver(Widget child) => MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(body: Center(child: child)),
      );

  testWidgets(
    'no soportado: el botón está deshabilitado y el motivo se lee en pantalla',
    (tester) async {
      var presionado = false;
      await tester.pumpWidget(
        envolver(
          BotonSegunCapacidad(
            grado: GradoDeSoporte.noSoportado,
            etiqueta: 'Confirmar con biometría',
            motivoSiNoSoportado:
                'Tu teléfono no tiene biometría disponible en esta versión de la app.',
            alPresionar: () => presionado = true,
          ),
        ),
      );

      final boton = tester.widget<FilledButton>(find.byType(FilledButton));
      expect(boton.onPressed, isNull, reason: 'un puerto no soportado no puede llamarse');

      expect(
        find.text('Tu teléfono no tiene biometría disponible en esta versión de la app.'),
        findsOneWidget,
      );

      await tester.tap(find.byType(FilledButton), warnIfMissed: false);
      await tester.pump();
      expect(presionado, isFalse);

      // El motivo también viaja por Semantics -- lo que lee un lector de pantalla.
      final semantica = tester.getSemantics(find.byType(FilledButton));
      expect(
        semantica.hint,
        'Tu teléfono no tiene biometría disponible en esta versión de la app.',
      );
    },
  );

  testWidgets('soportado: el botón está habilitado y no hay texto de motivo', (
    tester,
  ) async {
    var presionado = false;
    await tester.pumpWidget(
      envolver(
        BotonSegunCapacidad(
          grado: GradoDeSoporte.soportado,
          etiqueta: 'Confirmar con biometría',
          motivoSiNoSoportado: 'no debería verse',
          alPresionar: () => presionado = true,
        ),
      ),
    );

    final boton = tester.widget<FilledButton>(find.byType(FilledButton));
    expect(boton.onPressed, isNotNull);
    expect(find.text('no debería verse'), findsNothing);

    await tester.tap(find.byType(FilledButton));
    await tester.pump();
    expect(presionado, isTrue);
  });

  testWidgets('degradado: sigue habilitado -- degradado no es "no soportado"', (
    tester,
  ) async {
    await tester.pumpWidget(
      envolver(
        BotonSegunCapacidad(
          grado: GradoDeSoporte.degradado,
          etiqueta: 'Recibir avisos',
          motivoSiNoSoportado: 'no debería verse',
          alPresionar: () {},
        ),
      ),
    );
    final boton = tester.widget<FilledButton>(find.byType(FilledButton));
    expect(boton.onPressed, isNotNull);
  });
}
