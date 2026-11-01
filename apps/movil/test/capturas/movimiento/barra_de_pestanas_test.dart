import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ayudas_de_captura.dart';
import '../../comun.dart';

/// Evidencia de movimiento H5.S2.M4: cuadros de la píldora de la barra deslizándose de
/// «Inicio» a «Perfil» con resorte crítico, y el mismo cambio con movimiento reducido.
/// `flutter test test/capturas/movimiento --update-goldens`; escribe en `evidencia/movimiento/`.
/// NO son goldens de comparación: son cuadros para mirar (como las capturas de rediseño).
const _salida =
    '../../../../../docs/trabajo/2026-10-21-rediseno-bancario/evidencia/movimiento/barra';
const _cuadros = [0, 40, 80, 120, 160, 240, 400];

const _destinos = [
  (
    texto: 'Inicio',
    icono: IconosDeMarca.inicio,
    iconoActivo: IconosDeMarca.inicio,
    novedades: 0,
  ),
  (
    texto: 'Grupos',
    icono: IconosDeMarca.grupos,
    iconoActivo: IconosDeMarca.grupos,
    novedades: 0,
  ),
  (
    texto: 'Movimientos',
    icono: IconosDeMarca.movimientos,
    iconoActivo: IconosDeMarca.movimientos,
    novedades: 0,
  ),
  (
    texto: 'Perfil',
    icono: IconosDeMarca.perfil,
    iconoActivo: IconosDeMarca.perfil,
    novedades: 0,
  ),
];

Widget _barra(int actual, bool reducido) => MediaQuery(
  data: MediaQueryData(size: const Size(360, 120), disableAnimations: reducido),
  child: Scaffold(
    body: const SizedBox.shrink(),
    bottomNavigationBar: BarraPestanas(
      destinos: _destinos,
      actual: actual,
      onChanged: (_) {},
      central: (
        icono: IconosDeMarca.escanear,
        etiqueta: 'Escanear invitación',
        alTocar: () {},
      ),
    ),
  ),
);

void main() {
  setUpAll(cargarFuentesReales);

  for (final (nombre, brillo) in [
    ('claro', Brightness.light),
    ('oscuro', Brightness.dark),
  ]) {
    for (final reducido in [false, true]) {
      testWidgets('píldora de la barra · $nombre${reducido ? ' · reducido' : ''}', (
        tester,
      ) async {
        tester.view.physicalSize = const Size(360, 120);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        await montarParaCaptura(
          tester,
          _barra(0, reducido),
          dioSimulado().dio,
          brillo,
          const Size(360, 120),
          escalaDeTexto: 1,
        );
        await tester.pumpAndSettle();
        // Mismo árbol, otra pestaña activa: el estado de la barra se conserva y arranca el resorte.
        await montarParaCaptura(
          tester,
          _barra(3, reducido),
          dioSimulado().dio,
          brillo,
          const Size(360, 120),
          escalaDeTexto: 1,
        );
        var transcurrido = 0;
        for (final ms in _cuadros) {
          await tester.pump(Duration(milliseconds: ms - transcurrido));
          transcurrido = ms;
          await fotoSinExigir(
            tester,
            '$_salida/${reducido ? 'reducido' : 'normal'}-$nombre-${ms.toString().padLeft(3, '0')}ms.png',
          );
        }
      });
    }
  }
}
