import 'dart:ui';

import 'package:dio/dio.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';

const cuenta = '11111111-1111-4111-8111-111111111111';
const ruta = '/billetera/$cuenta/saldo';

/// Los cuatro estados de la pantalla real de F0, contra los ejemplos del contrato:
/// los mismos JSON que Prism sirve en desarrollo. Una pantalla probada solo en su
/// camino feliz es una pantalla que en la calle se queda en blanco.
/// La pantalla ahora abre con los accesos rápidos + la tarjeta de saldo — más alta
/// que el viewport de prueba por omisión (800×600 lógicos). Un teléfono real (~800×
/// 1700 lógicos) la muestra entera sin scroll; se agranda la superficie de prueba a
/// ese tamaño para que las aserciones de semántica no dependan de qué decidió pintar
/// la Sliver list como "en pantalla" — es lo que un dispositivo real hace de todas
/// formas, no un ajuste artificial para hacer pasar el test.
Future<void> conSuperficieDeTelefono(
  WidgetTester tester,
  Future<void> Function() cuerpo,
) async {
  final original = tester.view.physicalSize;
  final pixelRatioOriginal = tester.view.devicePixelRatio;
  tester.view.physicalSize = const Size(1080, 2280);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(() {
    tester.view.physicalSize = original;
    tester.view.devicePixelRatio = pixelRatioOriginal;
  });
  await cuerpo();
}

void main() {
  testWidgets('cargando: lo dice mientras espera, sin pantalla en blanco', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      ruta,
      (s) => s.reply(
        200,
        ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        delay: const Duration(milliseconds: 80),
      ),
    );
    await tester.pumpWidget(
      conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
    );
    await tester.pump();
    expect(find.bySemanticsLabel('Cargando tu saldo'), findsOneWidget);
    await asentar(tester);
  });

  testWidgets(
    'éxito: muestra el saldo que respondió el contrato, sin recalcular nada',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        ruta,
        (s) => s.reply(
          200,
          ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        ),
      );
      await conSuperficieDeTelefono(tester, () async {
        await tester.pumpWidget(
          conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
        );
        await asentar(tester);
        // El importe se presenta como la maqueta —`Bs 1.240,00`— y se anuncia con su concepto.
        expect(
          find.bySemanticsLabel(
            RegExp(r'^Saldo disponible: (Bs|USD) -?[\d.]+,\d{2}$'),
          ),
          findsOneWidget,
        );
        // Las tres acciones del saldo, con **una sola** principal: la fila de cuatro
        // íconos (incluido «Vales», que nunca tuvo pantalla) se reemplazó por
        // Aportar/Recargar/Retirar, y «Ver aportes pendientes» pasó a la sección de
        // pasanakus, que es donde vive ese dato.
        expect(find.text('Aportar'), findsOneWidget);
        expect(find.text('Recargar'), findsOneWidget);
        expect(find.text('Retirar'), findsOneWidget);
        // Las dos secciones que dan contexto debajo de la cifra.
        expect(find.text('Tus pasanakus'), findsOneWidget);
        expect(find.text('Extracto'), findsOneWidget);
        expect(find.text('Generar extracto'), findsOneWidget);
        expect(
          find.textContaining('No podemos mostrar tus pasanakus'),
          findsOneWidget,
        );
        expect(
          find.textContaining('Todavía no estás en ningún pasanaku'),
          findsNothing,
        );
        // La custodia sigue estando: es lo que responde «¿dónde está mi plata?».
        expect(find.textContaining('cuenta de custodia'), findsOneWidget);
      });
    },
  );

  testWidgets(
    'saldo cero: muestra el importe, recarga y contexto sin ocultar el inicio',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      final cero = {
        'cuentaId': cuenta,
        'disponible': {'monto': '0.00', 'moneda': 'BOB'},
        'retenido': {'monto': '0.00', 'moneda': 'BOB'},
        'alCorteDe': '2026-01-15T10:30:00Z',
      };
      adaptador.onGet(ruta, (s) => s.reply(200, cero));
      await conSuperficieDeTelefono(tester, () async {
        await tester.pumpWidget(
          conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
        );
        await asentar(tester);
        expect(
          find.bySemanticsLabel('Saldo disponible: Bs 0,00'),
          findsOneWidget,
        );
        expect(find.text('Recargar'), findsOneWidget);
        expect(find.text('Tus pasanakus'), findsOneWidget);
        expect(
          find.textContaining('Elegí un período en el extracto'),
          findsWidgets,
        );
      });
    },
  );

  for (final brillo in [Brightness.light, Brightness.dark]) {
    for (final escala in [1.0, 1.4, 2.0]) {
      for (final ancho in [360, 390, 430, 480, 520, 550, 552, 554, 600]) {
        testWidgets(
          'saldo cero a $ancho×760 y texto ${escala * 100} % en $brillo',
          (tester) async {
            final original = tester.view.physicalSize;
            final pixelRatioOriginal = tester.view.devicePixelRatio;
            tester.view.physicalSize = Size(ancho.toDouble(), 760);
            tester.view.devicePixelRatio = 1.0;
            addTearDown(() {
              tester.view.physicalSize = original;
              tester.view.devicePixelRatio = pixelRatioOriginal;
            });
            final (:dio, :adaptador) = dioSimulado();
            adaptador.onGet(
              ruta,
              (s) => s.reply(200, {
                'cuentaId': cuenta,
                'disponible': {'monto': '0.00', 'moneda': 'BOB'},
                'retenido': {'monto': '0.00', 'moneda': 'BOB'},
                'alCorteDe': '2026-01-15T10:30:00Z',
              }),
            );
            await tester.pumpWidget(
              conApp(
                const PantallaDeSaldo(cuentaId: cuenta),
                dio: dio,
                brillo: brillo,
                escalaDeTexto: escala,
              ),
            );
            await asentar(tester);
            expect(
              find.bySemanticsLabel('Saldo disponible: Bs 0,00'),
              findsOneWidget,
            );
            for (final accion in ['Aportar', 'Recargar', 'Retirar']) {
              await tester.ensureVisible(find.text(accion));
              await asentar(tester);
              final parrafo = tester.renderObject<RenderParagraph>(
                find.text(accion),
              );
              expect(parrafo.didExceedMaxLines, isFalse, reason: accion);
            }
            await asentar(tester);
            expect(find.text('Recargar'), findsOneWidget);
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
    testWidgets('saldo con fondos a 360×760 y texto 200 % en $brillo', (
      tester,
    ) async {
      final original = tester.view.physicalSize;
      final pixelRatioOriginal = tester.view.devicePixelRatio;
      tester.view.physicalSize = const Size(360, 760);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(() {
        tester.view.physicalSize = original;
        tester.view.devicePixelRatio = pixelRatioOriginal;
      });
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        ruta,
        (s) => s.reply(
          200,
          ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        ),
      );
      await tester.pumpWidget(
        conApp(
          const PantallaDeSaldo(cuentaId: cuenta),
          dio: dio,
          brillo: brillo,
          escalaDeTexto: 2,
        ),
      );
      await asentar(tester);
      expect(
        find.bySemanticsLabel(RegExp(r'^Saldo disponible: ')),
        findsOneWidget,
      );
      await tester.ensureVisible(find.text('Recargar'));
      await asentar(tester);
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets(
    'error: mensaje humano, reintento a la vista y traza para soporte',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        ruta,
        (s) => s.reply(401, {
          'codigo': 'AP-CU04-01',
          'mensaje': 'tecnico',
          'trazaId': 'traza-401',
        }),
      );
      await tester.pumpWidget(
        conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
      );
      await asentar(tester);
      expect(find.text('Volver a intentar'), findsOneWidget);
      expect(
        find.textContaining('Código de seguimiento: traza-401'),
        findsOneWidget,
      );
      expect(
        find.text('tecnico'),
        findsNothing,
        reason: 'el mensaje del backend no se muestra crudo',
      );
    },
  );

  testWidgets(
    'sin red: no se queda cargando para siempre, y dice que no hay conexión',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        ruta,
        (s) => s.throws(
          0,
          DioException.connectionError(
            requestOptions: RequestOptions(path: ruta),
            reason: 'sin red',
          ),
        ),
      );
      await tester.pumpWidget(
        conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
      );
      await asentar(tester);
      expect(find.text('Volver a intentar'), findsOneWidget);
      expect(find.textContaining('No pudimos conectarnos'), findsOneWidget);
    },
  );

  testWidgets('un 403 no dice más que «no tenés acceso»', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      ruta,
      (s) => s.reply(403, {
        'codigo': 'AP-SEG-03',
        'mensaje': 'detalle',
        'trazaId': 't',
      }),
    );
    await tester.pumpWidget(
      conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
    );
    await asentar(tester);
    expect(find.text('No tenés acceso a esto.'), findsOneWidget);
  });

  testWidgets(
    'reintentar vuelve a pedir el saldo al servidor: nunca se ajusta en memoria',
    (tester) async {
      await conSuperficieDeTelefono(tester, () async {
        final (:dio, :adaptador) = dioSimulado();
        // Primero el gateway falla; después responde. Registrar la ruta de nuevo
        // reemplaza la respuesta anterior.
        adaptador.onGet(
          ruta,
          (s) => s.reply(503, {
            'codigo': 'AP-GW-503',
            'mensaje': '',
            'trazaId': 't',
          }),
        );
        await tester.pumpWidget(
          conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
        );
        await asentar(tester);
        expect(find.text('Volver a intentar'), findsOneWidget);
        adaptador.onGet(
          ruta,
          (s) => s.reply(
            200,
            ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
          ),
        );
        await tester.tap(find.text('Volver a intentar'));
        await asentar(tester);
        expect(
          find.bySemanticsLabel(RegExp(r'^Saldo disponible: ')),
          findsOneWidget,
        );
        expect(find.text('Volver a intentar'), findsNothing);
      });
    },
  );
}
