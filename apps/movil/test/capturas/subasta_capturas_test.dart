import 'package:aportaya_movil/pantallas/pasanaku/pantalla_subasta.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_subastar_cupo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_subastas.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import '../pasanaku/subastar_cupo_test.dart' show subastaSintetica;
import 'ayudas_de_captura.dart';

/// Evidencia visual de la subasta del cupo (2026-10-30): la lista para todos, el detalle
/// para ofrecer y el formulario del saliente, por ancho y tema. Datos SINTÉTICOS. Se
/// generan con `--update-goldens`; no son goldens de comparación.
const _salida =
    '../../../../docs/trabajo/2026-10-30-traspaso-por-subasta/evidencia/capturas';
const _g = '11111111-1111-4111-8111-111111111111';
const _cupo = '22222222-2222-4222-8222-222222222222';
const _sub = '55555555-5555-4555-8555-555555555555';

const _anchos = {'360': Size(360, 740), '412': Size(412, 915)};

void main() {
  setUpAll(cargarFuentesReales);
  final otra = {
    ...subastaSintetica(soyElSaliente: false),
    'subastaId': '66666666-6666-4666-8666-666666666666',
    'nombreDelGrupo': 'Pasanaku de la feria',
  };
  final pantallas = <String, (Widget, void Function(dynamic))>{
    'lista': (
      const PantallaSubastas(),
      (a) => a.onGet(
        '/entregas/subastas',
        (s) => s.reply(200, {
          'pagina': 0,
          'subastas': [
            subastaSintetica(
              soyElSaliente: false,
              masAlto: '1800.00',
              pujas: 2,
            ),
            otra,
          ],
        }),
        queryParameters: {'limite': 20, 'pagina': 0},
      ),
    ),
    'detalle': (
      const PantallaSubasta(subastaId: _sub),
      (a) => a.onGet(
        '/entregas/subastas/$_sub',
        (s) => s.reply(
          200,
          subastaSintetica(soyElSaliente: false, masAlto: '1800.00', pujas: 2),
        ),
      ),
    ),
    'subastar': (
      const PantallaSubastarCupo(grupoId: _g, cupoId: _cupo),
      (a) => a.onGet(
        '/entregas/subastas',
        (s) => s.reply(200, {'pagina': 0, 'subastas': <Object>[]}),
        queryParameters: {'cupoId': _cupo, 'limite': 20, 'pagina': 0},
      ),
    ),
  };
  for (final MapEntry(key: ancho, value: tam) in _anchos.entries) {
    for (final (brillo, tema) in [
      (Brightness.light, 'claro'),
      (Brightness.dark, 'oscuro'),
    ]) {
      for (final MapEntry(key: nombre, value: (pantalla, responder))
          in pantallas.entries) {
        testWidgets('subasta · $nombre · $ancho dp · $tema', (t) async {
          final (:dio, :adaptador) = dioSimulado();
          responder(adaptador);
          await montarParaCaptura(
            t,
            pantalla,
            dio,
            brillo,
            tam,
            escalaDeTexto: 1.0,
          );
          await asentar(t);
          await fotoDeCaptura(
            t,
            '$_salida/subasta-$nombre-$ancho-$tema.png',
            '$nombre $ancho $tema',
          );
        });
      }
    }
  }
}
