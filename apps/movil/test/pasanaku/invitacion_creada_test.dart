import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/puertos/compartir.dart';
import 'package:aportaya_movil/pantallas/pasanaku/invitacion_creada.dart';
import 'package:aportaya_movil/proveedores/compartir.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// Hallazgo del carril H: la pantalla de éxito de CU-69 era texto plano («Invitacion» sin
/// tilde, enlace crudo, un botón sin confirmación). Ahora: título propio con tilde,
/// enlace en caja, vigencia, y «Copiar» que confirma con «Copiado».
///
/// 2026-10-18: el enlace que se muestra y se comparte es https (la app web), sin la
/// promesa falsa «Le va a llegar», y se manda por WhatsApp, SMS u otra app.
void main() {
  const codigo =
      '11111111-1111-4111-8111-111111111111.'
      'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';
  const delServidor = 'aportaya://unirse/$codigo';
  const web = 'https://app.aportaya.161.97.85.216.sslip.io';
  const enlace = '$web/#/pasanaku/unirse/$codigo';

  Future<_CompartirFalso> montar(
    WidgetTester tester, {
    VoidCallback? otra,
    String canal = 'ENLACE',
    bool abre = true,
  }) async {
    // Con el QR la pantalla es más alta que la superficie por defecto del test, y la lista
    // construye perezosa: se prueba en una superficie de teléfono.
    tester.view.physicalSize = const Size(1080, 2280);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final compartir = _CompartirFalso(abre: abre);
    await tester.pumpWidget(
      ProviderScope(
        overrides: [compartirProvider.overrideWithValue(compartir)],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(
            body: InvitacionCreada(
              resultado: SalidaInvitacion(
                mensaje: 'x',
                enlace: delServidor,
                expiraEn: DateTime.utc(2026, 10, 13, 15, 30),
              ),
              telefono: '+59170000002',
              canal: canal,
              appWeb: web,
              alInvitarOtra: otra ?? () {},
            ),
          ),
        ),
      ),
    );
    return compartir;
  }

  testWidgets('muestra título con tilde, a quién va, vigencia y el enlace', (
    tester,
  ) async {
    await montar(tester);
    expect(find.text('Invitación creada'), findsOneWidget);
    expect(find.textContaining('+59170000002'), findsOneWidget);
    expect(find.textContaining('Vence el 13 oct 2026'), findsOneWidget);
    expect(find.text(enlace), findsOneWidget);
    expect(find.textContaining('Le va a llegar'), findsNothing);
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
    await tester.ensureVisible(find.text('Copiar enlace'));
    await tester.tap(find.text('Copiar enlace'));
    await tester.pumpAndSettle();
    expect(copiado, enlace);
    expect(find.text('Copiado'), findsOneWidget);
  });

  testWidgets(
    'correcto: «Enviar por WhatsApp» abre el chat de ese número con el '
    'enlace https',
    (tester) async {
      final compartir = await montar(tester, canal: 'WHATSAPP');

      await tester.tap(find.text('Enviar por WhatsApp'));
      await tester.pumpAndSettle();

      expect(compartir.llamadas, ['whatsapp +59170000002']);
      expect(compartir.ultimoTexto, contains(enlace));
    },
  );

  testWidgets('límite: el canal elegido va primero y los otros siguen a mano', (
    tester,
  ) async {
    final compartir = await montar(tester, canal: 'SMS');
    final sms = tester.getTopLeft(find.text('Enviar por SMS'));
    final whatsapp = tester.getTopLeft(find.text('Enviar por WhatsApp'));
    expect(sms.dy, lessThan(whatsapp.dy));

    await tester.ensureVisible(find.text('Compartir con otra app'));
    await tester.tap(find.text('Compartir con otra app'));
    await tester.pumpAndSettle();
    expect(compartir.llamadas, ['otra']);
  });

  testWidgets('inválido: si la app no abre, ofrece copiar el enlace', (
    tester,
  ) async {
    await montar(tester, canal: 'SMS', abre: false);

    await tester.tap(find.text('Enviar por SMS'));
    await tester.pumpAndSettle();

    expect(find.textContaining('No pudimos abrir esa app'), findsOneWidget);
  });

  testWidgets('«Invitar a otra persona» avisa para volver al formulario', (
    tester,
  ) async {
    var veces = 0;
    await montar(tester, otra: () => veces++);
    await tester.ensureVisible(find.text('Invitar a otra persona'));
    await tester.tap(find.text('Invitar a otra persona'));
    expect(veces, 1);
  });
}

class _CompartirFalso implements Compartir {
  _CompartirFalso({required this.abre});
  final bool abre;
  final llamadas = <String>[];
  String? ultimoTexto;

  @override
  Future<bool> porWhatsApp({
    required String telefono,
    required String texto,
  }) async {
    llamadas.add('whatsapp $telefono');
    ultimoTexto = texto;
    return abre;
  }

  @override
  Future<bool> porSms({required String telefono, required String texto}) async {
    llamadas.add('sms $telefono');
    ultimoTexto = texto;
    return abre;
  }

  @override
  Future<bool> conOtraApp({required String texto}) async {
    llamadas.add('otra');
    ultimoTexto = texto;
    return abre;
  }
}
