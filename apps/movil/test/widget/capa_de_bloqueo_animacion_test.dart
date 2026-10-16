import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/vigilante_de_sesion.dart';
import 'package:aportaya_movil/pantallas/identidad/capa_de_bloqueo.dart';
import 'package:aportaya_movil/proveedores/biometria.dart';
import 'package:aportaya_movil/proveedores/bloqueo.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets(
    'candado entra en 220 ms, sale en 160 ms y respeta reduce motion',
    (tester) async {
      final control = ControlDeBloqueo(esWeb: false)
        ..bloquear(MotivoDeBloqueo.segundoPlano);

      Widget montar({required bool reducirMovimiento}) => ProviderScope(
        overrides: [
          bloqueoProvider.overrideWith((_) => control),
          biometriaDisponibleProvider.overrideWith((_) async => false),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: MediaQuery(
            data: MediaQueryData(disableAnimations: reducirMovimiento),
            child: CapaDeBloqueo(
              hijo: const Scaffold(body: Text('Contenido')),
              onIngresarDeNuevo: () {},
            ),
          ),
        ),
      );

      await tester.pumpWidget(montar(reducirMovimiento: false));
      expect(
        tester
            .widget<FadeTransition>(
              find.byKey(const ValueKey('capa-bloqueo-fundido')),
            )
            .opacity
            .value,
        0,
      );
      expect(
        tester
            .widget<ScaleTransition>(
              find.byKey(const ValueKey('capa-bloqueo-escala')),
            )
            .scale
            .value,
        0.92,
      );

      await tester.pump(const Duration(milliseconds: 219));
      expect(
        tester
            .widget<FadeTransition>(
              find.byKey(const ValueKey('capa-bloqueo-fundido')),
            )
            .opacity
            .value,
        greaterThan(0.99),
      );
      expect(
        tester
            .widget<ScaleTransition>(
              find.byKey(const ValueKey('capa-bloqueo-escala')),
            )
            .scale
            .value,
        closeTo(1, 0.001),
      );
      await tester.pump(const Duration(milliseconds: 1));
      expect(
        tester
            .widget<FadeTransition>(
              find.byKey(const ValueKey('capa-bloqueo-fundido')),
            )
            .opacity
            .value,
        1,
      );

      control.desbloquear();
      await tester.pump();
      expect(
        find.byKey(const ValueKey('capa-bloqueo-fundido')),
        findsOneWidget,
      );
      await tester.pump(const Duration(milliseconds: 160));
      expect(
        find.byKey(const ValueKey('capa-bloqueo-fundido')),
        findsOneWidget,
      );
      await tester.pump(const Duration(milliseconds: 1));
      expect(find.byKey(const ValueKey('capa-bloqueo-fundido')), findsNothing);

      control.bloquear(MotivoDeBloqueo.segundoPlano);
      await tester.pumpWidget(montar(reducirMovimiento: true));
      expect(
        find.byKey(const ValueKey('capa-bloqueo-fundido')),
        findsOneWidget,
      );
      expect(
        tester
            .widget<FadeTransition>(
              find.byKey(const ValueKey('capa-bloqueo-fundido')),
            )
            .opacity
            .value,
        1,
      );

      control.desbloquear();
      await tester.pump();
      expect(find.byKey(const ValueKey('capa-bloqueo-fundido')), findsNothing);
    },
  );
}
