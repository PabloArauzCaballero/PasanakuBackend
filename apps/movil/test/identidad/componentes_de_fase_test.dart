import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/bloque_de_formulario.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/cabecera_de_fase.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/tarjeta_de_confianza.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../goldens/cargar_fuentes_de_aplicacion.dart';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('cabecera anuncia fase y título, y expone progreso', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: const Scaffold(
          body: CabeceraDeFase(numero: 2, titulo: 'Tu identidad'),
        ),
      ),
    );

    expect(find.text('FASE 2 DE 4'), findsOneWidget);
    expect(find.text('Tu identidad'), findsOneWidget);
    expect(find.byType(LinearProgressIndicator), findsOneWidget);
    final animacion = tester.widget<TweenAnimationBuilder<double>>(
      find.byType(TweenAnimationBuilder<double>),
    );
    expect(animacion.duration, Movimiento.pagina);
    expect(animacion.curve, Movimiento.llega);
  });

  testWidgets('captura la cabecera integrada de fase 2 a 360 por 800', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        debugShowCheckedModeBanner: false,
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: const Scaffold(
          body: SafeArea(
            child: Padding(
              padding: EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  CabeceraDeFase(numero: 2, titulo: 'Tu identidad'),
                  SizedBox(height: 20),
                  Text('Completa los datos de tu documento.'),
                ],
              ),
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(MaterialApp),
      matchesGoldenFile('goldens/cabecera_fase_2_360.png'),
    );
  });

  testWidgets('bloque y tarjeta de confianza muestran encabezado y copy', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: const Scaffold(
          body: Column(
            children: [
              BloqueDeFormulario(
                icono: Icons.mail_outline,
                titulo: 'Cómo te contactamos',
                hijos: [Text('Correo electrónico')],
              ),
              TarjetaDeConfianza(),
            ],
          ),
        ),
      ),
    );

    expect(find.text('CÓMO TE CONTACTAMOS'), findsOneWidget);
    expect(find.text('Correo electrónico'), findsOneWidget);
    expect(find.byIcon(Icons.lock_outline), findsOneWidget);
    expect(find.text(TarjetaDeConfianza.mensaje), findsOneWidget);
  });

  testWidgets('botón deshabilitado muestra motivo como ayuda bajo el botón', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: const Scaffold(
          body: Boton(
            texto: 'Continuar',
            onPressed: null,
            motivoDeBloqueo: 'Falta tu correo electrónico.',
          ),
        ),
      ),
    );

    expect(find.text('Continuar'), findsOneWidget);
    expect(find.text('Falta tu correo electrónico.'), findsOneWidget);
    expect(
      tester.getTopLeft(find.text('Falta tu correo electrónico.')).dy,
      greaterThan(tester.getTopLeft(find.text('Continuar')).dy),
    );
  });
}
