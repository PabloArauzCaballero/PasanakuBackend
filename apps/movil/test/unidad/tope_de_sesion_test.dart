import 'package:aportaya_movil/dominio/tope_de_sesion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  group('tope absoluto de sesión', () {
    late DateTime ahora;
    late Sesion sesion;
    late List<MotivoDeSalida> cierres;
    late TopeDeSesion tope;

    setUp(() async {
      ahora = DateTime.utc(2026, 10, 12, 12);
      sesion = Sesion(AlmacenEnMemoria(), () => ahora);
      await sesion.abrir(acceso: 'acceso', refresco: 'refresco');
      cierres = [];
      tope = TopeDeSesion(
        sesion: sesion,
        ahora: () => ahora,
        cerrar: ({required motivo}) async {
          cierres.add(motivo);
          await sesion.cerrar(motivo: motivo);
        },
      );
    });

    test('7 h 59 min mantiene activa la sesión', () async {
      ahora = ahora.add(const Duration(hours: 7, minutes: 59));

      await tope.revisar();

      expect(sesion.activa, isTrue);
      expect(cierres, isEmpty);
    });

    test('8 h exactas cierra con motivo caducada', () async {
      ahora = ahora.add(const Duration(hours: 8));

      await tope.revisar();

      expect(sesion.activa, isFalse);
      expect(cierres, [MotivoDeSalida.caducada]);
    });

    test('el reloj atrasado cierra por seguridad', () async {
      ahora = ahora.subtract(const Duration(seconds: 1));

      await tope.revisar();

      expect(cierres, [MotivoDeSalida.caducada]);
    });

    test('revisiones simultáneas ejecutan un solo cierre', () async {
      ahora = ahora.add(const Duration(hours: 8));

      await Future.wait([tope.revisar(), tope.revisar(), tope.revisar()]);

      expect(cierres, [MotivoDeSalida.caducada]);
    });
  });
}
