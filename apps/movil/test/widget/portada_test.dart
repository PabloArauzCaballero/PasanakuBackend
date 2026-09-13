import 'package:aportaya_diseno/atomos/barra_de_puntos.dart';
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

    expect(router.state.uri.toString(), '/registro');
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
    'se puede crear cuenta sin terminar el tour: las acciones no son un peaje',
    (tester) async {
      tester.view.physicalSize = const Size(1170, 2532); // iPhone 13/14, 3x
      tester.view.devicePixelRatio = 3;
      addTearDown(tester.view.reset);

      final router = crearEnrutador();
      await tester.pumpWidget(appCon(router));
      await tester.pumpAndSettle();

      // Sin deslizar ni una lámina, las dos salidas ya están a la vista.
      expect(find.text('Crear mi cuenta'), findsOneWidget);
      expect(find.text('Ya tengo cuenta'), findsOneWidget);
    },
  );

  testWidgets(
    '«Crear mi cuenta» NO monta la barra de pestañas: sin sesión no hay app',
    (tester) async {
      // El alta vivía dentro del shell, así que abrirla montaba Inicio · Grupos ·
      // Movimientos · Perfil y la app parecía haber iniciado sesión sola, con una
      // cuenta que todavía no existía.
      final router = crearEnrutador();
      await tester.pumpWidget(appCon(router));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Crear mi cuenta'));
      await tester.pumpAndSettle();

      expect(router.state.uri.toString(), '/registro');
      expect(
        find.byType(BarraPestanas),
        findsNothing,
        reason: 'el alta no puede traer la barra de pestañas de la app',
      );
    },
  );

  testWidgets('la portada dice qué es esto antes de pedir nada', (tester) async {
    final router = crearEnrutador();
    await tester.pumpWidget(appCon(router));
    await tester.pumpAndSettle();

    expect(find.textContaining('pasanaku'), findsWidgets);
    // Cuatro láminas, una idea por lámina (D-8 de la maqueta).
    expect(find.byType(PageView), findsOneWidget);
    expect(find.byType(BarraDePuntos), findsOneWidget);
    final puntos = tester.widget<BarraDePuntos>(find.byType(BarraDePuntos));
    expect(puntos.total, 4);
  });
}
