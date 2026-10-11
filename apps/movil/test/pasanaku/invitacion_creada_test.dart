import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/pasanaku/invitacion_creada.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

/// Hallazgo del carril H: la pantalla de éxito de CU-69 era texto plano («Invitacion» sin
/// tilde, enlace crudo, un botón sin confirmación). Ahora: título propio con tilde,
/// enlace en caja, vigencia, y «Copiar» que confirma con «Copiado».
void main() {
  const enlace =
      'aportaya://unirse/11111111-1111-4111-8111-111111111111.'
      'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';

  Future<void> montar(WidgetTester tester, {VoidCallback? otra}) =>
      tester.pumpWidget(
        MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(
            body: InvitacionCreada(
              resultado: SalidaInvitacion(
                mensaje: 'x',
                enlace: enlace,
                expiraEn: DateTime.utc(2026, 10, 13, 15, 30),
              ),
              telefono: '+59170000002',
              alInvitarOtra: otra ?? () {},
            ),
          ),
        ),
      );

  testWidgets('muestra título con tilde, a quién va, vigencia y el enlace', (
    tester,
  ) async {
    await montar(tester);
    expect(find.text('Invitación creada'), findsOneWidget);
    expect(find.textContaining('+59170000002'), findsOneWidget);
    expect(find.textContaining('Vence el 13 oct 2026'), findsOneWidget);
    expect(find.text(enlace), findsOneWidget);
  });

  testWidgets('Copiar deja el enlace en el portapapeles y confirma «Copiado»', (
    tester,
  ) async {
    String? copiado;
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
      SystemChannels.platform,
      (call) async {
        if (call.method == 'Clipboard.setData') {
          copiado = (call.arguments as Map)['text'] as String?;
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
    await montar(tester);
    expect(find.text('Copiado'), findsNothing);
    await tester.tap(find.text('Copiar enlace'));
    await tester.pumpAndSettle();
    expect(copiado, enlace);
    expect(find.text('Copiado'), findsOneWidget);
  });

  testWidgets('«Invitar a otra persona» avisa para volver al formulario', (
    tester,
  ) async {
    var veces = 0;
    await montar(tester, otra: () => veces++);
    await tester.tap(find.text('Invitar a otra persona'));
    expect(veces, 1);
  });
}
