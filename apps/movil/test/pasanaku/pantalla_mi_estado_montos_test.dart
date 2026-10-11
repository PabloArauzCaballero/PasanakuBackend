import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart'
    as aportes;
import 'package:aportaya_cliente_garantia/aportaya_cliente_garantia.dart'
    as garantia;
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/estado_de_mora.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_mi_estado.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// Hallazgo del carril H: «Se levanta pagando 4.44» (punto, sin moneda) convivía con
/// «Bs 4,44» en la misma pantalla. Todo importe se formatea con `formatearMonto`.
void main() {
  testWidgets('todos los importes salen con moneda y coma decimal', (
    tester,
  ) async {
    const p = 'pppppppp-0000-4000-8000-000000000001';
    const u = 'uuuuuuuu-0000-4000-8000-000000000001';
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          estadoDelParticipanteProvider(p).overrideWith(
            (ref) async => aportes.EstadoDelParticipante(
              alDia: false,
              totalAportado: '0.00',
              deudaVigente: '1240.50',
              porAportar: '4.44',
              obligacionesAbiertas: 1,
              moneda: aportes.EstadoDelParticipanteMonedaEnum.BOB,
            ),
          ),
          restriccionVigenteProvider(u).overrideWith(
            (ref) async => garantia.RestriccionVigente(
              vigente: true,
              montoQueLaLevanta: '1240.50',
            ),
          ),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const PantallaMiEstado(participanteId: p, usuarioId: u),
        ),
      ),
    );
    await tester.pumpAndSettle();

    final textos = tester
        .widgetList<Text>(find.byType(Text))
        .map((t) => t.data ?? '')
        .join(' | ');
    expect(
      RegExp(r'Bs[\s\u00A0]1\.240,50').hasMatch(textos),
      isTrue,
      reason: textos,
    );
    expect(
      RegExp(r'Bs[\s\u00A0]4,44').hasMatch(textos),
      isTrue,
      reason: textos,
    );
    // Ningún importe crudo con punto decimal pegado a una cifra de dos dígitos.
    expect(
      RegExp(r'\d\.\d{2}(?!\d)').hasMatch(textos),
      isFalse,
      reason: textos,
    );
  });
}
