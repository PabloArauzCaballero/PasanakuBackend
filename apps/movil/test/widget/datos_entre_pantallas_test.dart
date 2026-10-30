import 'dart:convert';
import 'dart:typed_data';

import 'package:aportaya_diseno/atomos/avatar.dart';
import 'package:aportaya_movil/navegacion/refrescar_al_cambiar_de_pestana.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/foto_de_perfil.dart';
import 'package:aportaya_movil/pantallas/pasanaku/avatar_del_integrante.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// PNG de 1x1 sintético y válido (un píxel transparente): no es la cara de nadie.
final _png = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==',
);

Avatar _avatar(WidgetTester t) => t.widget<Avatar>(find.byType(Avatar));

Future<void> _montarAvatar(
  WidgetTester t, {
  required bool soyYo,
  required Uint8List? foto,
}) async {
  final (:dio, adaptador: _) = dioSimulado();
  await t.pumpWidget(
    conApp(
      Scaffold(
        body: AvatarDelIntegrante(nombre: 'Ana Pérez', soyYo: soyYo),
      ),
      dio: dio,
      extra: [fotoDePerfilProvider.overrideWith((ref) async => foto)],
    ),
  );
  await asentar(t);
}

/// Escucha la foto (como Inicio y Perfil, que quedan montados detrás del
/// `indexedStack`) y tiene un campo con foco, como el monto de «Aportar».
class _PestanaDeAtras extends ConsumerWidget {
  const _PestanaDeAtras(this.foco);
  final FocusNode foco;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.watch(fotoDePerfilProvider);
    return Column(
      children: [
        TextField(focusNode: foco, autofocus: true),
        TextButton(
          onPressed: () => refrescarAlCambiarDePestana(ref),
          child: const Text('Inicio'),
        ),
      ],
    );
  }
}

void main() {
  group('avatar del integrante', () {
    testWidgets('soy yo y tengo foto: se ve la foto', (t) async {
      await _montarAvatar(t, soyYo: true, foto: _png);
      expect(_avatar(t).imagen, isA<MemoryImage>());
    });

    testWidgets('soy yo sin foto (404): iniciales', (t) async {
      await _montarAvatar(t, soyYo: true, foto: null);
      expect(_avatar(t).imagen, isNull);
    });

    testWidgets('otra persona: iniciales aunque yo tenga foto', (t) async {
      await _montarAvatar(t, soyYo: false, foto: _png);
      expect(_avatar(t).imagen, isNull);
    });
  });

  testWidgets(
    'cambiar de pestaña cierra el teclado y vuelve a pedir lo que la pantalla de atrás escucha',
    (t) async {
      var pedidos = 0;
      final foco = FocusNode();
      addTearDown(foco.dispose);
      final (:dio, adaptador: _) = dioSimulado();
      await t.pumpWidget(
        conApp(
          Scaffold(body: _PestanaDeAtras(foco)),
          dio: dio,
          extra: [
            fotoDePerfilProvider.overrideWith((ref) async {
              pedidos++;
              return null;
            }),
          ],
        ),
      );
      await asentar(t);
      expect(pedidos, 1);
      expect(foco.hasFocus, isTrue);
      expect(t.testTextInput.isVisible, isTrue);

      await t.tap(find.text('Inicio'));
      await asentar(t);

      expect(pedidos, 2);
      expect(foco.hasFocus, isFalse);
      expect(t.testTextInput.isVisible, isFalse);
    },
  );
}
