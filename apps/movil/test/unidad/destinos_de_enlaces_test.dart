import 'package:aportaya_movil/navegacion/enlaces_profundos.dart';
import 'package:aportaya_movil/navegacion/ruta_del_aviso.dart';
import 'package:aportaya_movil/pantallas/alianzas/rutas.dart';
import 'package:aportaya_movil/pantallas/billetera/rutas.dart';
import 'package:aportaya_movil/pantallas/identidad/rutas.dart';
import 'package:aportaya_movil/pantallas/notificaciones/rutas.dart';
import 'package:aportaya_movil/pantallas/pasanaku/rutas.dart';
import 'package:aportaya_movil/pantallas/soporte/rutas.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

/// HUECO-001 (F3.L6.M19): el mapa de enlaces se escribía a mano y nadie lo cotejaba con
/// el árbol de rutas, así que `aportaya://aporte` llevaba a una ruta que no existía. Esta
/// prueba resuelve cada destino contra las mismas listas que monta `crearEnrutador`.
List<String> _patrones(List<RouteBase> rutas) => [
  for (final r in rutas)
    if (r is GoRoute) ...[
      r.path,
      ..._patrones(r.routes),
    ] else
      ..._patrones(r.routes),
];

final _arbol = _patrones([
  ...rutasBilletera,
  ...rutasPasanaku,
  ...rutasIdentidad,
  ...rutasSoporte,
  ...rutasDeEntrada,
  ...rutasDeInvitacion,
  ...rutasAlianzas,
  ...rutasNotificaciones,
]);

bool _existe(String ruta) => _arbol.any(
  (p) =>
      RegExp('^${p.replaceAll(RegExp(r':[^/]+'), '[^/]+')}\$').hasMatch(ruta),
);

void main() {
  test(
    'cada destino de enlace existe en el árbol de rutas, o es prefijo de una con parámetro',
    () {
      for (final base in rutasBaseDeEnlaces) {
        expect(
          _existe(base) || _existe('$base/x'),
          isTrue,
          reason: '$base no es una ruta de la app',
        );
      }
    },
  );

  test(
    'correcto: aportaya://aporte abre la pantalla donde se ve y se paga la cuota',
    () {
      final destino = rutaInternaDesde(Uri.parse('aportaya://aporte'))!;
      expect(_existe(destino), isTrue, reason: destino);
      expect(rutaDelAviso('aportaya://aporte'), destino);
    },
  );

  test(
    'límite: un segmento de más en aportaya://aporte no inventa una ruta',
    () {
      final destino = rutaInternaDesde(Uri.parse('aportaya://aporte/123'));
      expect(destino == null || _existe(destino), isTrue, reason: '$destino');
    },
  );

  test('inválido: un host desconocido no se traduce', () {
    expect(rutaInternaDesde(Uri.parse('aportaya://otra-cosa')), isNull);
  });
}
