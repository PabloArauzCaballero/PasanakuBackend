import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/dominio/tutoriales/registro.dart';
import 'package:aportaya_movil/infraestructura/almacen_de_progreso_seguro.dart';
import 'package:aportaya_movil/pantallas/soporte/pantalla_centro_de_ayuda.dart';
import 'package:aportaya_movil/proveedores/tutoriales.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import '../widget/tutoriales/banco.dart';

const _tutorial = TutorialDefinicion(
  id: 'a11y',
  version: '1.0.0',
  titulo: 'Qué es AportaYa',
  descripcion: 'Una vuelta general por la app, sin apuro.',
  categoria: 'Primeros pasos',
  ruta: '/uno',
  minutos: 3,
  dificultad: Dificultad.inicial,
  obligatorio: true,
  pasos: [
    PasoDeTutorial(
      id: 'p1',
      titulo: 'Acá está tu plata',
      descripcion: 'Este es el saldo de tu billetera.',
      ancla: 'uno.uno',
    ),
    PasoDeTutorial(
      id: 'p2',
      titulo: 'Y acá las acciones',
      descripcion: 'Aportar, recargar y retirar.',
      ancla: 'uno.dos',
    ),
  ],
);

/// Accesibilidad bloqueante: contraste AA, área táctil ≥ 48 dp y todo control con
/// etiqueta. `meetsGuideline` es el `axe` del mundo Flutter.
void main() {
  testWidgets(
    'el globo del tutorial, sobre la pantalla real, cumple las guías',
    (tester) async {
      final banco = BancoDeTutoriales(catalogo: const [_tutorial]);
      await tester.pumpWidget(banco.montar());
      await correr(
        tester,
        () => banco.contenedor
            .read(motorDeTutorialesProvider.notifier)
            .iniciar('a11y'),
      );

      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
    },
  );

  testWidgets('el centro de tutoriales cumple las guías', (tester) async {
    tester.view.physicalSize = const Size(1200, 3600);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final contenedor = ProviderContainer(
      retry: (_, _) => null,
      overrides: [
        almacenDeProgresoProvider.overrideWithValue(
          AlmacenDeProgresoSeguro(AlmacenEnMemoria()),
        ),
        registroDeTutorialesProvider.overrideWithValue(
          const RegistroDeTutoriales([_tutorial], {}),
        ),
      ],
    );
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const PantallaCentroDeAyuda(),
        ),
      ),
    );
    await tester.pump();

    await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
    await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
    await expectLater(tester, meetsGuideline(textContrastGuideline));
  });
}
