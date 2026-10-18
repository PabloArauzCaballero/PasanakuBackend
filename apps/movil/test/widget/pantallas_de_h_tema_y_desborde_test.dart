import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:aportaya_movil/pantallas/pasanaku/invitacion_creada.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_crear_grupo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_invitar.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_unirse.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// Carril L · las pantallas que tocó el carril H (invitación creada, detalle de la
/// invitación que se recibe, ingreso, invitar y organizar un grupo), en tema claro y en
/// oscuro, a 360 px de ancho con el texto al 130 %. La pregunta es una sola: ¿se pinta
/// sin un solo desborde de layout y con el tema que se pidió? Un desborde en Flutter es
/// una excepción de render: `tester.takeException()` la devuelve y debe ser nula.
///
/// Sin emulador: widget tests con el tema real del sistema de diseño.
const _codigo =
    '12345678-1234-4234-8234-123456789abc.'
    '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';

const _enlaceLargo =
    'aportaya://unirse/11111111-1111-4111-8111-111111111111.'
    'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';

void main() {
  for (final (brillo, nombreTema) in [
    (Brightness.light, 'claro'),
    (Brightness.dark, 'oscuro'),
  ]) {
    group('360 px · texto 130 % · tema $nombreTema', () {
      Future<void> montar(
        WidgetTester tester,
        Widget pantalla, {
        required dynamic dio,
        bool conSesion = false,
      }) async {
        tester.view.physicalSize = const Size(360, 740);
        tester.view.devicePixelRatio = 1;
        tester.platformDispatcher.textScaleFactorTestValue = 1.3;
        addTearDown(tester.view.reset);
        addTearDown(tester.platformDispatcher.clearAllTestValues);
        final almacen = AlmacenEnMemoria();
        if (conSesion) await almacen.guardar('sesion.acceso', 'a.b.c');
        await tester.pumpWidget(
          ProviderScope(
            retry: (r, e) => null,
            overrides: [
              almacenSeguroProvider.overrideWithValue(almacen),
              dioProvider.overrideWithValue(dio),
            ],
            child: MaterialApp(
              theme: temaDesde(Tokens.claro, Brightness.light),
              darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
              themeMode: brillo == Brightness.dark
                  ? ThemeMode.dark
                  : ThemeMode.light,
              home: pantalla,
            ),
          ),
        );
        await asentar(tester);
      }

      void sinDesborde(WidgetTester tester) {
        final tema = Theme.of(tester.element(find.byType(Scaffold).first));
        expect(tema.brightness, brillo, reason: 'se pidió el tema $nombreTema');
        final escala = MediaQuery.textScalerOf(
          tester.element(find.byType(Scaffold).first),
        );
        expect(escala.scale(10), closeTo(13, 0.001), reason: 'texto al 130 %');
        expect(tester.takeException(), isNull, reason: 'sin desborde ni error');
      }

      testWidgets('invitación creada · enlace largo', (tester) async {
        final (:dio, adaptador: _) = dioSimulado();
        await montar(
          tester,
          Scaffold(
            body: InvitacionCreada(
              resultado: SalidaInvitacion(
                mensaje: 'x',
                enlace: _enlaceLargo,
                expiraEn: DateTime.utc(2026, 10, 13, 15, 30),
              ),
              telefono: '+59170000002',
              alInvitarOtra: () {},
            ),
          ),
          dio: dio,
        );
        expect(find.text('Invitación creada'), findsOneWidget);
        expect(find.text(_enlaceLargo), findsOneWidget);
        sinDesborde(tester);
      });

      testWidgets('detalle de la invitación recibida · grupo y reglamento', (
        tester,
      ) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onPost(
          '/grupos/invitaciones/enlace/consultar',
          (s) => s.reply(200, {
            'grupoId': '11111111-1111-4111-8111-111111111111',
            'nombre': 'Pasanaku de la feria de los jueves por la mañana',
            'montoAporte': '1500.00',
            'moneda': 'BOB',
            'periodicidad': 'QUINCENAL',
            'reglamento':
                'Aportar a tiempo cada quincena. Avisar con anticipación si no '
                'se puede aportar. Respetar el orden de los turnos acordado.',
            'hashReglamento': 'a' * 64,
          }),
          data: Matchers.any,
        );
        await montar(
          tester,
          const PantallaUnirse(codigo: _codigo),
          dio: dio,
          conSesion: true,
        );
        expect(find.text('Unirme al grupo'), findsOneWidget);
        expect(find.text('Reglamento del grupo'), findsOneWidget);
        sinDesborde(tester);
      });

      testWidgets('detalle de la invitación recibida · sin sesión', (
        tester,
      ) async {
        final (:dio, adaptador: _) = dioSimulado();
        await montar(tester, const PantallaUnirse(codigo: _codigo), dio: dio);
        expect(find.text('Crear cuenta'), findsOneWidget);
        sinDesborde(tester);
      });

      testWidgets('detalle de la invitación recibida · error del servidor', (
        tester,
      ) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onPost(
          '/grupos/invitaciones/enlace/consultar',
          (s) => s.reply(409, {'codigo': 'AP-CU69-03'}),
          data: Matchers.any,
        );
        await montar(
          tester,
          const PantallaUnirse(codigo: _codigo),
          dio: dio,
          conSesion: true,
        );
        expect(find.text('Reintentar'), findsOneWidget);
        sinDesborde(tester);
      });

      testWidgets('ingreso (login)', (tester) async {
        final (:dio, adaptador: _) = dioSimulado();
        await montar(tester, const PantallaDeSesion(), dio: dio);
        expect(find.text('+591'), findsWidgets);
        sinDesborde(tester);
      });

      testWidgets('invitar a un contacto · formulario', (tester) async {
        final (:dio, adaptador: _) = dioSimulado();
        await montar(
          tester,
          const PantallaInvitar(
            grupoId: '11111111-1111-4111-8111-111111111111',
          ),
          dio: dio,
        );
        expect(find.text('Invitar a un contacto'), findsOneWidget);
        sinDesborde(tester);
      });

      testWidgets('organizar un grupo · formulario', (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        organizadorHabilitado(adaptador);
        await montar(tester, const PantallaCrearGrupo(), dio: dio);
        expect(find.text('Organizar un grupo'), findsOneWidget);
        sinDesborde(tester);
      });

      testWidgets('organizar un grupo · contrato de organizador', (
        tester,
      ) async {
        final (:dio, :adaptador) = dioSimulado();
        organizadorHabilitado(adaptador, habilitado: false);
        await montar(tester, const PantallaCrearGrupo(), dio: dio);
        expect(find.text('Organizá tu primer grupo'), findsOneWidget);
        expect(find.text('Aceptar y organizar'), findsOneWidget);
        sinDesborde(tester);
      });

      testWidgets('organizar un grupo · grupo creado', (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        organizadorHabilitado(adaptador);
        adaptador.onPost(
          '/grupos',
          (s) => s.reply(201, ejemplo('grupos', 'crearGrupo', 'ok')['cuerpo']),
          data: Matchers.any,
        );
        await montar(tester, const PantallaCrearGrupo(), dio: dio);
        final campos = find.byType(EditableText);
        await tester.enterText(campos.at(0), 'Grupo de prueba');
        await tester.enterText(campos.at(1), '100,00');
        await tester.pump();
        await tester.tap(find.text('Crear grupo').last);
        await asentar(tester);
        expect(find.text('Grupo creado'), findsOneWidget);
        sinDesborde(tester);
      });
    });
  }
}
