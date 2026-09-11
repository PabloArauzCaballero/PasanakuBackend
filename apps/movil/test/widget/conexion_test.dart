import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/errores.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class _ErrorDeRedDePrueba implements Exception, ErrorPresentable {
  @override
  String get mensaje => 'No hay conexión. Te mostramos lo último que vimos.';
  @override
  String? get trazaId => null;
  @override
  bool get sinConexion => true;
}

/// Gate del shell: «con la red caída la app muestra estado, no una pantalla en
/// blanco». `EstadoDePantalla` (que compone cada pantalla del shell) pinta
/// `EstadoError` con el ícono de sin-conexión — nunca deja el `body` vacío.
void main() {
  testWidgets('sin conexión se ve un estado, nunca una pantalla en blanco', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(
            body: EstadoDePantalla<int>(
              valor: AsyncValue<int>.error(
                _ErrorDeRedDePrueba(),
                StackTrace.empty,
              ),
              exito: (_) => const SizedBox(),
              mensajeVacio: 'vacío',
              reintentar: () {},
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(EstadoError), findsOneWidget);
    expect(find.byIcon(Icons.wifi_off_outlined), findsOneWidget);
    expect(
      find.text('No hay conexión. Te mostramos lo último que vimos.'),
      findsOneWidget,
    );
    // El botón de reintento existe: la persona puede actuar, no es un cartel muerto.
    expect(
      find.widgetWithText(FilledButton, 'Volver a intentar'),
      findsOneWidget,
    );
  });
}
