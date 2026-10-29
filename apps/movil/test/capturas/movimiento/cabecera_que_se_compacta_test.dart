import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ayudas_de_captura.dart';
import '../../comun.dart';

/// Evidencia H4.S2.M6: la cabecera grande compactándose a medida que se desplaza el
/// contenido, en claro y oscuro. El cambio sigue al dedo (no tiene tiempo propio), así
/// que los cuadros van por desplazamiento, no por milisegundos.
/// `flutter test test/capturas/movimiento --update-goldens`; escribe en `evidencia/movimiento/`.
/// NO son goldens de comparación: son cuadros para mirar.
const _salida =
    '../../../../../docs/trabajo/2026-10-21-rediseno-bancario/evidencia/movimiento/cabecera';
const _tramos = [0, 15, 30, 45, 120];
const _tamano = Size(360, 400);

Widget _pantalla() => Scaffold(
  body: PantallaConCabecera(
    titulo: 'El turno del grupo',
    cuerpo: ListView(
      padding: const EdgeInsets.all(Espacio.s4),
      children: [
        for (var i = 1; i <= 20; i++)
          Padding(
            padding: const EdgeInsets.only(bottom: Espacio.s3),
            child: Text('Fila de contenido $i'),
          ),
      ],
    ),
  ),
);

void main() {
  setUpAll(cargarFuentesReales);

  for (final (nombre, brillo) in [
    ('claro', Brightness.light),
    ('oscuro', Brightness.dark),
  ]) {
    testWidgets('cabecera que se compacta · $nombre', (tester) async {
      tester.view.physicalSize = _tamano;
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await montarParaCaptura(
        tester,
        _pantalla(),
        dioSimulado().dio,
        brillo,
        _tamano,
        escalaDeTexto: 1,
      );
      await tester.pumpAndSettle();
      var hecho = 0;
      for (final px in _tramos) {
        if (px > hecho) {
          await tester.drag(
            find.text('Fila de contenido 3'),
            Offset(0, -(px - hecho).toDouble()),
            touchSlopY: 0,
          );
          await tester.pumpAndSettle();
          hecho = px;
        }
        await fotoSinExigir(
          tester,
          '$_salida/$nombre-${px.toString().padLeft(3, '0')}px.png',
        );
      }
    });
  }
}
