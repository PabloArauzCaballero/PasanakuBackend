import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/juez_de_pose.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/silueta_de_cara.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/trazos_de_cara.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('golden de las tres poses en tema claro y oscuro', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(300, 400);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    for (final (nombre, tokens) in [
      ('claro', Tokens.claro),
      ('oscuro', Tokens.oscuro),
    ]) {
      for (final pose in PoseDeVida.values) {
        await tester.pumpWidget(
          MaterialApp(
            debugShowCheckedModeBanner: false,
            theme: ThemeData(extensions: [tokens]),
            home: Scaffold(
              backgroundColor: Colors.black,
              body: SizedBox.expand(child: SiluetaDeCara(pose: pose)),
            ),
          ),
        );
        await tester.pumpAndSettle();
        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile('goldens/silueta_${nombre}_${pose.name}.png'),
        );
      }
    }
  });

  testWidgets('dibuja las tres poses y espeja el perfil derecho', (
    tester,
  ) async {
    await tester.pumpWidget(
      _escena(const SiluetaDeCara(pose: PoseDeVida.frente)),
    );
    final frente = _pintor(tester);
    expect(frente.pose, PoseDeVida.frente);
    expect(
      frente.contornoNormalizado.getBounds(),
      contornoDeFrente().getBounds(),
    );

    await tester.pumpWidget(
      _escena(const SiluetaDeCara(pose: PoseDeVida.izquierda)),
    );
    final izquierda = _pintor(tester);
    final boundsIzquierda = izquierda.contornoNormalizado.getBounds();
    expect(izquierda.pose, PoseDeVida.izquierda);
    expect(boundsIzquierda.left, contornoDePerfil().getBounds().left);
    expect(boundsIzquierda.right, contornoDePerfil().getBounds().right);

    await tester.pumpWidget(
      _escena(const SiluetaDeCara(pose: PoseDeVida.derecha)),
    );
    final derecha = _pintor(tester);
    final boundsDerecha = derecha.contornoNormalizado.getBounds();
    expect(derecha.pose, PoseDeVida.derecha);
    expect(boundsDerecha.left, 300 - boundsIzquierda.right);
    expect(boundsDerecha.right, 300 - boundsIzquierda.left);
  });

  testWidgets('aplica velo, guía y tinta de cámara de tokens invariables', (
    tester,
  ) async {
    await tester.pumpWidget(
      _escena(const SiluetaDeCara(pose: PoseDeVida.frente)),
    );
    final pintor = _pintor(tester);

    expect(pintor.velo, Tokens.claro.camaraVelo);
    expect(pintor.guia, Tokens.claro.camaraGuia);
    expect(pintor.velo, Tokens.oscuro.camaraVelo);
    expect(pintor.guia, Tokens.oscuro.camaraGuia);
    expect(Tokens.claro.camaraMidiendo, Tokens.oscuro.camaraMidiendo);
    expect(Tokens.claro.camaraLista, Tokens.oscuro.camaraLista);
    expect(Tokens.claro.camaraTinta, Tokens.oscuro.camaraTinta);
    expect(pintor.velo.a, closeTo(0.55, 0.01));
    expect(pintor.guia.a, closeTo(0.7, 0.01));
    expect(Tokens.claro.camaraTinta, const Color(0xFFFFFFFF));
    final silueta = find.byType(SiluetaDeCara);
    expect(
      find.descendant(of: silueta, matching: find.byType(IgnorePointer)),
      findsOneWidget,
    );
    expect(
      find.descendant(of: silueta, matching: find.byType(RepaintBoundary)),
      findsOneWidget,
    );
    final areaVelada = pintor.areaVelada(const Size(300, 400));
    expect(areaVelada.contains(const Offset(150, 184)), isFalse);
    expect(areaVelada.contains(const Offset(5, 5)), isTrue);
  });

  testWidgets('interpola color y grosor en 120 ms; al tomar llega a 5', (
    tester,
  ) async {
    await tester.pumpWidget(
      _escena(const SiluetaDeCara(pose: PoseDeVida.frente, progreso: 0)),
    );
    expect(_pintor(tester).grosor, 3);

    await tester.pumpWidget(
      _escena(const SiluetaDeCara(pose: PoseDeVida.frente, tomada: true)),
    );
    expect(_pintor(tester).grosor, 3);
    await tester.pump(const Duration(milliseconds: 60));
    final medio = _pintor(tester);
    expect(medio.grosor, greaterThan(3));
    expect(medio.grosor, lessThan(5));
    expect(medio.color, isNot(Tokens.claro.camaraMidiendo));

    await tester.pump(const Duration(milliseconds: 60));
    expect(_pintor(tester).grosor, 5);
    expect(_pintor(tester).color, Tokens.claro.camaraLista);
  });

  testWidgets('con reducir movimiento cambia al estado final sin interpolar', (
    tester,
  ) async {
    await tester.pumpWidget(
      _escena(
        const SiluetaDeCara(pose: PoseDeVida.frente, progreso: 0),
        quieto: true,
      ),
    );
    await tester.pumpWidget(
      _escena(
        const SiluetaDeCara(pose: PoseDeVida.frente, tomada: true),
        quieto: true,
      ),
    );
    await tester.pump();

    expect(_pintor(tester).grosor, 5);
    expect(_pintor(tester).color, Tokens.claro.camaraLista);
  });
}

Widget _escena(Widget silueta, {bool quieto = false}) => MaterialApp(
  theme: ThemeData(extensions: const [Tokens.claro]),
  home: MediaQuery(
    data: MediaQueryData(disableAnimations: quieto),
    child: Scaffold(body: SizedBox.expand(child: silueta)),
  ),
);

PintorDeSiluetaDeCara _pintor(WidgetTester tester) =>
    tester
            .widget<CustomPaint>(
              find.byWidgetPredicate(
                (widget) =>
                    widget is CustomPaint &&
                    widget.painter is PintorDeSiluetaDeCara,
              ),
            )
            .painter!
        as PintorDeSiluetaDeCara;
