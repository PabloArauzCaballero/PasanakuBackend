import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/proveedores/tutoriales.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../unidad/tutoriales/comun.dart';
import 'banco.dart';

const _intro = TutorialDefinicion(
  id: guiaDeInicioId,
  version: '1.0.0',
  titulo: 'Guía de inicio',
  descripcion: 'd',
  categoria: 'Pruebas',
  ruta: '/uno',
  minutos: 1,
  dificultad: Dificultad.inicial,
  pasos: [
    PasoDeTutorial(
      id: 'p1',
      titulo: 'Primero',
      descripcion: 'Mirá arriba',
      ancla: 'uno.uno',
    ),
  ],
);

void main() {
  testWidgets('correcto: tras completarla, Perfil la vuelve a lanzar de cero', (
    tester,
  ) async {
    final banco = BancoDeTutoriales(catalogo: [_intro]);
    await banco.almacen.guardar(
      ProgresoDeTutorial(
        tutorialId: guiaDeInicioId,
        version: '1.0.0',
        estado: EstadoDeProgreso.completado,
        indice: 0,
        iniciadoEn: ahoraDePrueba,
        ultimaInteraccion: ahoraDePrueba,
        repeticiones: 1,
      ),
    );
    await tester.pumpWidget(banco.montar());
    await tester.pump();

    late Future<bool> resultado;
    await correr(
      tester,
      () => resultado = banco.contenedor.read(verGuiaDeInicioOtraVezProvider)(),
    );
    expect(await resultado, isTrue);
    expect(banco.contenedor.read(motorDeTutorialesProvider).activo, isTrue);
    final guardado = await banco.almacen.leer();
    expect(guardado.single.estado, EstadoDeProgreso.enProgreso);
  });

  testWidgets(
    'inválido: sin la guía en el catálogo devuelve false y no arranca nada',
    (tester) async {
      final banco = BancoDeTutoriales(catalogo: [tutorialDePrueba('otro')]);
      await tester.pumpWidget(banco.montar());
      await tester.pump();

      final resultado = await banco.contenedor.read(
        verGuiaDeInicioOtraVezProvider,
      )();
      expect(resultado, isFalse);
      expect(banco.contenedor.read(motorDeTutorialesProvider).activo, isFalse);
    },
  );
}
