import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/dominio/puertos/haptica.dart';

const cuenta = '11111111-1111-4111-8111-111111111111';
const ruta = '/billetera/$cuenta/saldo';

class _HapticaDePrueba implements Haptica {
  int selecciones = 0;

  @override
  Future<void> seleccion() async => selecciones++;

  @override
  Future<void> botonPrimario() async {}

  @override
  Future<void> avisoDeCredencialesIncorrectas() async {}

  @override
  Future<void> exito() async {}

  @override
  Future<void> errorQueBloqueaDinero() async {}
}

class _AdaptadorContador implements HttpClientAdapter {
  _AdaptadorContador(this.delegate);

  final HttpClientAdapter delegate;
  int consultas = 0;
  int movimientos = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) {
    if (options.method == 'GET' && options.path.endsWith('/saldo')) {
      consultas++;
    }
    if (options.method == 'GET' && options.path.endsWith('/movimientos')) {
      movimientos++;
    }
    return delegate.fetch(options, requestStream, cancelFuture);
  }

  @override
  void close({bool force = false}) => delegate.close(force: force);
}

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
        expect(find.text('Movimientos'), findsOneWidget);
        // La custodia sigue estando: es lo que responde «¿dónde está mi plata?». Sin nombre de banco:
        // ninguna fuente documentada lo respalda (docs/, contratos).
        expect(find.textContaining('En custodia'), findsOneWidget);
        expect(find.textContaining('Banco Unión'), findsNothing);
      });
    },
  );

  testWidgets(
    'vacío: una cuenta en cero muestra su saldo, cómo recargar y por qué no hay movimientos',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      final cero = {
        'cuentaId': cuenta,
        'disponible': {'monto': '0.00', 'moneda': 'BOB'},
        'retenido': {'monto': '0.00', 'moneda': 'BOB'},
        'alCorteDe': '2026-01-15T10:30:00Z',
      };
      adaptador.onGet(ruta, (s) => s.reply(200, cero));
      adaptador.onGet(
        '/billetera/$cuenta/movimientos',
        (s) => s.reply(200, {'cuentaId': cuenta, 'movimientos': <Object>[]}),
      );
      await tester.pumpWidget(
        conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
      );
      await asentar(tester);
      expect(find.text('Recargar'), findsOneWidget);
      expect(
        find.textContaining('Todavía no tenés movimientos'),
        findsOneWidget,
      );
    },
  );

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
      expect(find.textContaining('Referencia: traza-401'), findsOneWidget);
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
      expect(find.textContaining('No hay conexión'), findsOneWidget);
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
    expect(
      find.text('Esta acción todavía no está disponible para tu cuenta.'),
      findsOneWidget,
    );
  });

  testWidgets(
    'reintentar vuelve a pedir el saldo al servidor: nunca se ajusta en memoria',
    (tester) async {
      await conSuperficieDeTelefono(tester, () async {
        final (:dio, :adaptador) = dioSimulado();
        // «Tus pasanakus» pide sus grupos aparte (2026-10-21): sin grupos, para que
        // el único «Volver a intentar» en pantalla sea el del saldo.
        adaptador.onGet(
          '/grupos/participaciones',
          (s) => s.reply(200, <Object>[]),
        );
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

  testWidgets('refrescar emite haptica de seleccion', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    final contador = _AdaptadorContador(dio.httpClientAdapter);
    dio.httpClientAdapter = contador;
    adaptador.onGet(
      ruta,
      (s) => s.reply(
        200,
        ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
      ),
    );
    final haptica = _HapticaDePrueba();
    await tester.pumpWidget(
      conApp(
        const PantallaDeSaldo(cuentaId: cuenta),
        dio: dio,
        haptica: haptica,
      ),
    );
    await asentar(tester);
    expect(contador.consultas, 1);
    expect(contador.movimientos, 1);
    expect(
      find.bySemanticsLabel(RegExp(r'^Saldo disponible: ')),
      findsOneWidget,
    );

    await tester.drag(find.byType(Scrollable).first, const Offset(0, 420));
    await asentar(tester);

    expect(haptica.selecciones, 1);
    expect(contador.consultas, 2);
    expect(contador.movimientos, 2);
  });

  testWidgets(
    'movimientos del servidor conservan orden y muestran los posteriores juntos',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        ruta,
        (s) => s.reply(
          200,
          ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        ),
      );
      adaptador.onGet(
        '/billetera/$cuenta/movimientos',
        (s) => s.reply(200, {
          'cuentaId': cuenta,
          'movimientos': [
            for (var i = 0; i < 10; i++)
              {
                'id':
                    '00000000-0000-4000-8000-${i.toString().padLeft(12, '0')}',
                'sentido': i.isEven ? 'CREDITO' : 'DEBITO',
                'monto': {'monto': '${i + 1}.00', 'moneda': 'BOB'},
                'saldoDisponiblePosterior': {
                  'monto': '${i + 10}.00',
                  'moneda': 'BOB',
                },
                'saldoRetenidoPosterior': {'monto': '0.00', 'moneda': 'BOB'},
                'glosa': 'Movimiento $i',
                'registradoEn':
                    '2026-10-11T12:${i.toString().padLeft(2, '0')}:00Z',
              },
          ],
        }),
        queryParameters: const {'limite': 100},
      );
      await conSuperficieDeTelefono(tester, () async {
        await tester.pumpWidget(
          conApp(const PantallaDeSaldo(cuentaId: cuenta), dio: dio),
        );
        await asentar(tester);
        expect(find.text('Movimiento 0'), findsOneWidget);
        expect(find.text('Movimiento 7'), findsOneWidget);
        expect(find.text('Movimiento 8'), findsOneWidget);
        expect(find.text('Movimiento 9'), findsOneWidget);
        expect(find.textContaining('Bs 10,00'), findsOneWidget);
        expect(
          tester.getTopLeft(find.text('Movimiento 0')).dy,
          lessThan(tester.getTopLeft(find.text('Movimiento 1')).dy),
        );
        final apariciones = find
            .byType(AparicionEscalonada)
            .evaluate()
            .toList();
        expect(apariciones, hasLength(1));
        expect(
          find.ancestor(
            of: find.text('Tus pasanakus'),
            matching: find.byType(AparicionEscalonada),
          ),
          findsNothing,
          reason: 'la cifra y las secciones no esperan al escalonado del feed',
        );
        final lista = apariciones.single.widget as AparicionEscalonada;
        expect(lista.escalon, const Duration(milliseconds: 30));
        expect(lista.children, hasLength(9));
        expect(lista.children.last, isA<Column>());
      });
    },
  );
}
