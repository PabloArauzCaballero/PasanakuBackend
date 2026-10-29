import 'package:aportaya_diseno/organismos/encabezado_de_saldo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ayudas_de_captura.dart';
import '../../comun.dart';

/// Evidencia H5.S2.M7: ocultar el saldo con desenfoque y fundido, y el mismo cambio con
/// movimiento reducido (inmediato). Cuadros por milisegundo, para mirar.
/// `flutter test test/capturas/movimiento --update-goldens`; escribe en `evidencia/movimiento/`.
const _salida =
    '../../../../../docs/trabajo/2026-10-21-rediseno-bancario/evidencia/movimiento/saldo-oculto';
const _cuadros = [0, 40, 80, 120, 160];
const _tamano = Size(360, 300);

Widget _pantalla(bool reducido) => MediaQuery(
  data: MediaQueryData(size: _tamano, disableAnimations: reducido),
  child: const Scaffold(
    body: Padding(
      padding: EdgeInsets.all(Espacio.s4),
      child: EncabezadoDeSaldo(
        etiqueta: 'Disponible',
        etiquetaHablada: 'Saldo disponible',
        monto: '1240.50',
        moneda: 'BOB',
        cifras: [(etiqueta: 'Retenido', monto: '150.00')],
        distintivo: 'En custodia',
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
      testWidgets('saldo oculto · $nombre${reducido ? ' · reducido' : ''}', (
        tester,
      ) async {
        tester.view.physicalSize = _tamano;
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        await montarParaCaptura(
          tester,
          _pantalla(reducido),
          dioSimulado().dio,
          brillo,
          _tamano,
          escalaDeTexto: 1,
        );
        await tester.pumpAndSettle();
        await tester.tap(find.byTooltip('Ocultar saldo'));
        await tester.pump();
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
