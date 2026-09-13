import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/app.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

/// **La puerta de entrada.**
///
/// La app arrancaba dentro de la billetera, con un saldo ya puesto: quien la abría por
/// primera vez caía en el tablero de una cuenta que no era suya, sin saber qué era
/// esto ni cómo entrar. Ahora abre en la portada, que explica el producto y ofrece las
/// dos únicas salidas que tiene sentido ofrecer sin sesión.
void main() {
  Widget appCon(GoRouter router) => ProviderScope(
    child: MaterialApp.router(
      theme: temaDesde(Tokens.claro, Brightness.light),
      routerConfig: router,
    ),
  );

  testWidgets('la app abre en la portada, no adentro de la billetera', (
    tester,
  ) async {
    final router = crearEnrutador();
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    expect(router.state.uri.toString(), '/portada');
    expect(find.text('Crear mi cuenta'), findsOneWidget);
    expect(find.text('Ya tengo cuenta'), findsOneWidget);
  });

  testWidgets(
    'sin sesión no hay barra de pestañas: no se ofrecen destinos a los que '
    'todavía no se puede ir',
    (tester) async {
      final router = crearEnrutador();
      await tester.pumpWidget(appCon(router));
      await tester.pumpAndSettle();

      expect(find.byType(BarraPestanas), findsNothing);
    },
  );

  testWidgets('«Ya tengo cuenta» lleva al ingreso, y el ingreso tampoco trae pestañas', (
    tester,
  ) async {
    final router = crearEnrutador();
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Ya tengo cuenta'));
    await tester.pumpAndSettle();

    expect(router.state.uri.toString(), '/ingreso');
    expect(find.byType(BarraPestanas), findsNothing);
  });

  testWidgets('«Crear mi cuenta» lleva al alta', (tester) async {
    final router = crearEnrutador();
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Crear mi cuenta'));
    await tester.pumpAndSettle();

    expect(router.state.uri.toString(), '/identidad/registro');
  });

  testWidgets(
    'arrancando la app de verdad —no solo el enrutador— se llega a la portada',
    (tester) async {
      // Esta es la prueba que faltaba. `AppAportaYa` repetía su propia ruta por
      // defecto; cuando la del enrutador pasó a ser la portada, esa copia siguió
      // mandando a la billetera y la app abría adentro igual, con las pruebas del
      // enrutador en verde. Acá se monta la app entera, apertura incluida.
      await tester.pumpWidget(ProviderScope(child: AppAportaYa()));
      await tester.pumpAndSettle();

      expect(find.text('Crear mi cuenta'), findsOneWidget);
      expect(
        find.text('Tu billetera'),
        findsNothing,
        reason: 'la app no puede abrir dentro de una cuenta que todavía no es de nadie',
      );
    },
  );

  testWidgets(
    'entra entera en un teléfono: la promesa de la custodia se lee sin desplazar',
    (tester) async {
      // Un teléfono chico de verdad. La tercera promesa —«la plata no la tenemos
      // nosotros»— es la que responde «¿y mi dinero?»: si queda cortada contra la
      // barra de acciones, la portada falla justo en lo que vino a hacer.
      tester.view.physicalSize = const Size(1170, 2532); // iPhone 13/14, 3x
      tester.view.devicePixelRatio = 3;
      addTearDown(tester.view.reset);

      final router = crearEnrutador();
      await tester.pumpWidget(appCon(router));
      await tester.pumpAndSettle();

      final custodia = find.text('La plata no la tenemos nosotros');
      expect(custodia, findsOneWidget);

      final caja = tester.getRect(custodia);
      final botonera = tester.getRect(find.text('Crear mi cuenta'));
      expect(
        caja.bottom,
        lessThan(botonera.top),
        reason: 'la tercera promesa queda tapada por la barra de acciones',
      );
    },
  );

  testWidgets('la portada dice qué es esto antes de pedir nada', (tester) async {
    final router = crearEnrutador();
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    expect(find.textContaining('pasanaku'), findsWidgets);
    // Las tres promesas, que son lo que responde «¿por qué te daría mi plata?».
    expect(find.text('Tu rueda, a la vista'), findsOneWidget);
    expect(find.text('El turno no se arregla'), findsOneWidget);
    expect(find.text('La plata no la tenemos nosotros'), findsOneWidget);
  });
}
