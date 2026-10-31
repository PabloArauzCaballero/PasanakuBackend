import 'package:aportaya_movil/pantallas/pasanaku/pantalla_detalle_miembro.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// El perfil del integrante (2026-10-21): solo lo que el servidor devuelve, y solo
/// para quien comparte el grupo. Datos sintéticos.
const _grupo = '11111111-1111-4111-8111-111111111111';
const _beto = '22222222-2222-4222-8222-222222222222';
const _contacto = '/identidad/contactos/grupos/$_grupo/integrantes/$_beto';
const _reputacion = '/reputacion/$_beto/perfil-en-grupo/$_grupo';

Map<String, dynamic> _conHistorial() => {
  'tieneHistorial': true,
  'puntaje': '812.50',
  'nivelDeConfianza': 'CONFIABLE',
  'indicePuntualidad': '96.40',
  'gruposCompletados': 3,
  'gruposAbandonados': 1,
  'incumplimientosAbiertos': 0,
  'antiguedadMeses': 14,
  'insignias': [
    {
      'codigo': 'PAGADOR_PUNTUAL',
      'nombre': 'Pagador puntual',
      'descripcion': 'Pagó a tiempo doce aportes seguidos.',
      'otorgadaEn': '2026-09-01T12:00:00Z',
    },
  ],
};

Map<String, dynamic> _problema(String codigo) => {
  'codigo': codigo,
  'mensaje': 'No.',
  'trazaId': 'traza-prueba',
};

const _pantalla = PantallaDetalleMiembro(
  grupoId: _grupo,
  cupoId: 'cupo-2',
  nombre: 'Beto Mamani',
  numero: 2,
  turno: 3,
  nombreGrupo: 'Pasanaku del barrio',
  usuarioId: _beto,
);

Future<void> _montar(WidgetTester tester, Widget hijo, Dio dio) async {
  await tester.pumpWidget(conApp(hijo, dio: dio));
  await asentar(tester);
}

void main() {
  group('perfil del integrante', () {
    testWidgets('correcto: teléfono y reputación reales; nada inventado', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador
        ..onGet(
          _contacto,
          (s) => s.reply(200, {'telefonoE164': '+59171234567'}),
        )
        ..onGet(_reputacion, (s) => s.reply(200, _conHistorial()));
      await _montar(tester, _pantalla, dio);

      expect(find.text('Beto Mamani'), findsOneWidget);
      expect(find.text('Cupo 2'), findsOneWidget);
      expect(find.text('Turno 3'), findsOneWidget);
      expect(find.text('+59171234567'), findsOneWidget);
      expect(find.text('Confiable'), findsOneWidget);
      // Decisión 2026-10-30 (PLAN.md, «Privacidad del puntaje»): el puntaje exacto
      // solo lo ve su dueño. Esta aserción decía findsOneWidget (regla del
      // 2026-10-21); se invierte porque cambió el requisito, no para que pase.
      expect(find.text('812.50'), findsNothing);
      expect(find.text('Puntaje'), findsNothing);
      expect(find.text('96.40 %'), findsOneWidget);
      expect(find.text('14 meses'), findsOneWidget);
      expect(find.text('Pagador puntual'), findsOneWidget);
      // Lo que la pantalla anterior inventaba ya no existe.
      expect(find.textContaining('Identidad Verificada'), findsNothing);
      expect(find.textContaining('Rango'), findsNothing);
      expect(find.textContaining('Bs '), findsNothing);
      expect(find.textContaining('***'), findsNothing);
    });

    testWidgets('límite: sin historial no hay ceros, lo dice', (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador
        ..onGet(
          _contacto,
          (s) => s.reply(200, {'telefonoE164': '+59171234567'}),
        )
        ..onGet(
          _reputacion,
          (s) => s.reply(200, {
            'tieneHistorial': false,
            'nivelDeConfianza': 'SIN_HISTORIAL',
            'insignias': <Object>[],
          }),
        );
      await _montar(tester, _pantalla, dio);

      expect(find.text('Sin historial'), findsOneWidget);
      expect(find.textContaining('Todavía no tiene historial'), findsOneWidget);
      expect(find.text('Puntaje'), findsNothing);
      expect(find.text('Todavía no ganó insignias.'), findsOneWidget);
    });

    testWidgets(
      'inválido: 403 dice que es solo para el grupo y no ofrece reintentar',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador
          ..onGet(_contacto, (s) => s.reply(403, _problema('AP-SEG-403')))
          ..onGet(_reputacion, (s) => s.reply(403, _problema('AP-SEG-403')));
        await _montar(tester, _pantalla, dio);

        expect(
          find.text('Solo quienes están en el mismo grupo ven estos datos.'),
          findsNWidgets(2),
        );
        expect(find.text('+59171234567'), findsNothing);
        expect(find.text('Volver a intentar'), findsNothing);
      },
    );

    testWidgets(
      'parcial: si falla la reputación, el teléfono sigue y se puede reintentar',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador
          ..onGet(
            _contacto,
            (s) => s.reply(200, {'telefonoE164': '+59171234567'}),
          )
          ..onGet(_reputacion, (s) => s.reply(500, _problema('AP-SIS-500')));
        await _montar(tester, _pantalla, dio);

        expect(find.text('+59171234567'), findsOneWidget);
        expect(find.text('Volver a intentar'), findsOneWidget);
      },
    );

    testWidgets('sin la persona identificada no se pide nada', (tester) async {
      final (:dio, adaptador: _) = dioSimulado();
      await _montar(
        tester,
        const PantallaDetalleMiembro(
          grupoId: _grupo,
          cupoId: 'cupo-2',
          nombre: '',
          numero: 2,
        ),
        dio,
      );

      expect(find.text('Integrante'), findsOneWidget);
      expect(
        find.text('Solo quienes están en el mismo grupo ven estos datos.'),
        findsOneWidget,
      );
    });

    testWidgets('«Volver» cierra el perfil y deja el grupo a la vista', (
      tester,
    ) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador
        ..onGet(
          _contacto,
          (s) => s.reply(200, {'telefonoE164': '+59171234567'}),
        )
        ..onGet(_reputacion, (s) => s.reply(200, _conHistorial()));
      await _montar(
        tester,
        Builder(
          builder: (context) => Scaffold(
            body: Center(
              child: TextButton(
                onPressed: () => Navigator.of(
                  context,
                ).push(MaterialPageRoute<void>(builder: (_) => _pantalla)),
                child: const Text('El grupo'),
              ),
            ),
          ),
        ),
        dio,
      );
      await tester.tap(find.text('El grupo'));
      await asentar(tester);
      expect(find.text('Perfil del integrante'), findsOneWidget);

      await tester.tap(find.byIcon(Icons.arrow_back));
      await asentar(tester);
      expect(find.text('Perfil del integrante'), findsNothing);
      expect(find.text('El grupo'), findsOneWidget);
    });

    testWidgets('a11y: todo lo tocable llega a 48 dp', (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador
        ..onGet(
          _contacto,
          (s) => s.reply(200, {'telefonoE164': '+59171234567'}),
        )
        ..onGet(_reputacion, (s) => s.reply(200, _conHistorial()));
      await _montar(tester, _pantalla, dio);
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
    });
  });
}
