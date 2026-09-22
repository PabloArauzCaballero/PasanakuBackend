import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/arranque/pantalla_configuracion_invalida.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// H3.S3.M2 / H5.S3.M2 — `flutter test` no recibe `--dart-define=API=...`
/// (como un build de release sin los defines): `resultadoGateway.valida` es `false`
/// desde que arranca el proceso, exactamente el escenario que hay que probar. Este
/// spec reproduce la elección de `main.dart` y comprueba las dos partes del
/// contrato: se ve la pantalla de bloqueo, y `dioProvider` nunca se crea.
void main() {
  test('sin los defines correctos, la configuración del gateway es inválida', () {
    expect(resultadoGateway.valida, isFalse);
    expect(baseDelGateway, isNull);
  });

  testWidgets(
    'con configuración inválida: se ve la pantalla de bloqueo y no se crea el cliente HTTP',
    (tester) async {
      final container = ProviderContainer();
      addTearDown(container.dispose);

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          // La misma decisión que main.dart: nunca AppAportaYa con configuración inválida.
          child: resultadoGateway.valida
              ? const SizedBox()
              : const PantallaConfiguracionInvalida(),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('AportaYa no puede arrancar'), findsOneWidget);
      // Nunca se lee dioProvider: `container.exists` es false hasta que algo lo lee.
      expect(container.exists(dioProvider), isFalse);
    },
  );

  testWidgets('la pantalla de bloqueo no muestra ninguna URL', (tester) async {
    await tester.pumpWidget(const PantallaConfiguracionInvalida());
    await tester.pumpAndSettle();

    final textos = tester
        .widgetList<Text>(find.byType(Text))
        .map((t) => t.data ?? '')
        .join(' ');
    expect(textos.contains('http://'), isFalse);
    expect(textos.contains('https://'), isFalse);
    expect(textos.toLowerCase().contains('localhost'), isFalse);
  });
}
