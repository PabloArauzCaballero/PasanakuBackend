import 'package:aportaya_diseno/organismos/celebracion_stage.dart';
import 'package:aportaya_diseno/moviles/tour_en_stages.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_detalle_miembro.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  const grupoId = '11111111-1111-4111-8111-111111111111';
  const cupoId = 'cupo-1';

  group('PantallaDetalleMiembro', () {
    testWidgets(
      'muestra datos del participante protegiendo su privacidad cuando no es el usuario actual',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        await tester.pumpWidget(
          conApp(
            const PantallaDetalleMiembro(
              grupoId: grupoId,
              cupoId: cupoId,
              nombre: 'Juan Perez',
              numero: 2,
              turno: null,
              esOrganizador: false,
              soyYo: false,
              estado: 'FIRMADO',
              nombreGrupo: 'Pasanaku Central',
            ),
            dio: dio,
          ),
        );
        await tester.pumpAndSettle();

        // Nombre y cupo visibles
        expect(find.text('Juan Perez'), findsWidgets);
        expect(find.text('Cupo 2'), findsOneWidget);
        expect(find.text('Sin turno todavía'), findsOneWidget);

        // Verificación de identidad oficial Nivel 2
        expect(find.text('Identidad Verificada · Nivel 2 Oficial'), findsOneWidget);

        // Privacidad bancaria: no expone teléfonos ni correos sin enmascarar de terceros
        expect(find.text('Contacto protegido por privacidad'), findsWidgets);
        expect(find.textContaining('+591'), findsNothing);
        expect(find.textContaining('@'), findsNothing);

        // Tarjetas estéticas de Riesgo y Puntos
        expect(find.text('CALIFICACIÓN DE RIESGO'), findsOneWidget);
        expect(find.text('PUNTOS EN JUEGO'), findsOneWidget);
        expect(find.text('HISTORIAL Y MÉTRICAS DE JUEGO'), findsOneWidget);

        // Métricas de historial medido
        expect(find.text('Grupos Activos'), findsOneWidget);
        expect(find.text('Grupos Pasados'), findsOneWidget);
        expect(find.text('Ciclos Completos'), findsOneWidget);
        expect(find.text('Turnos Jugados'), findsOneWidget);
        expect(find.text('Días de Retraso'), findsOneWidget);
        expect(find.text('Impagos'), findsNWidgets(2));

        // Vitrina de trofeos e insignias (estilo GymSheet)
        expect(find.text('INSIGNIAS Y TROFEOS DE HONOR'), findsOneWidget);
        expect(find.text('Primer Pasanaku'), findsOneWidget);
        expect(find.text('Pagador Puntual'), findsOneWidget);
        expect(find.text('Sin Mora 12 Meses'), findsOneWidget);
        expect(find.text('Mesa de Honor'), findsOneWidget);
      },
    );

    testWidgets(
      'muestra turno asignado y contacto enmascarado cuando soyYo es true',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        await tester.pumpWidget(
          conApp(
            const PantallaDetalleMiembro(
              grupoId: grupoId,
              cupoId: cupoId,
              nombre: 'Rosa Quispe',
              numero: 1,
              turno: 3,
              esOrganizador: true,
              soyYo: true,
              estado: 'FIRMADO',
              nombreGrupo: 'Pasanaku Central',
            ),
            dio: dio,
          ),
        );
        await tester.pumpAndSettle();

        expect(find.text('Rosa Quispe'), findsWidgets);
        expect(find.text('Turno 3'), findsOneWidget);
        expect(find.text('Vos (Tu cupo)'), findsOneWidget);
        expect(find.text('Organizador'), findsOneWidget);

        // Al ser mi propio perfil, muestra mi contacto enmascarado
        expect(find.textContaining('+591'), findsOneWidget);
        expect(find.textContaining('@pasanaku.bo'), findsOneWidget);
      },
    );

    testWidgets(
      'al tocar una insignia conseguida se abre la CelebracionStage con rayos y partículas',
      (tester) async {
        tester.view.physicalSize = const Size(412, 3000);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);

        final (:dio, :adaptador) = dioSimulado();
        await tester.pumpWidget(
          conApp(
            const PantallaDetalleMiembro(
              grupoId: grupoId,
              cupoId: cupoId,
              nombre: 'Carlos Arce',
              numero: 1,
              turno: 1,
            ),
            dio: dio,
          ),
        );
        await tester.pumpAndSettle();

        // Toca la insignia conseguida "Primer Pasanaku"
        await tester.tap(find.text('Primer Pasanaku'));
        await tester.pump();
        await tester.pump(const Duration(milliseconds: 300));

        // Debe haberse abierto CelebracionStage
        expect(find.byType(CelebracionStage), findsOneWidget);
        expect(find.text('¡NUEVO LOGRO!'), findsOneWidget);
        expect(find.text('Continuar en el Pasanaku'), findsOneWidget);
      },
    );

    testWidgets(
      'al tocar el banner explicativo se abre el TourEnStages en 4 etapas',
      (tester) async {
        tester.view.physicalSize = const Size(412, 3000);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);

        final (:dio, :adaptador) = dioSimulado();
        await tester.pumpWidget(
          conApp(
            const PantallaDetalleMiembro(
              grupoId: grupoId,
              cupoId: cupoId,
              nombre: 'Carlos Arce',
              numero: 1,
            ),
            dio: dio,
          ),
        );
        await tester.pumpAndSettle();

        // Toca el banner explicativo
        await tester.tap(find.text('¿Cómo funcionan los Puntos y el Riesgo?'));
        await tester.pump();
        await tester.pump(const Duration(milliseconds: 300));

        // Debe haberse abierto TourEnStages
        expect(find.byType(TourEnStages), findsOneWidget);
        expect(find.text('1 Punto por cada Bs en Juego'), findsOneWidget);
        expect(find.text('Siguiente etapa'), findsOneWidget);
      },
    );
  });
}
