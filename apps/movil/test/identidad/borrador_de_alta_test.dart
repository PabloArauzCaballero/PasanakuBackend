import 'dart:convert';

import 'package:aportaya_movil/pantallas/identidad/dominio/borrador_de_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  final ahora = DateTime.utc(2026, 10, 7, 12);

  test('guarda datos reanudables sin secretos, OTP ni archivos KYC', () async {
    final almacen = AlmacenEnMemoria();
    final borrador = BorradorDeAlta(almacen, ahora: () => ahora);
    await borrador.guardar(
      const EstadoAlta(
        paso: PasoAlta.contrato,
        datos: DatosPersonales(
          nombres: 'Rosa',
          apellidos: 'Quispe',
          telefono: '+59178123456',
        ),
        contrasena: 'secreto-no-persistir',
        codigoConfirmado: true,
        rutaAnverso: '/tmp/ci.jpg',
        rutaReverso: '/tmp/reverso.jpg',
        rutaSelfie: '/tmp/selfie.jpg',
        origenDeFondos: 'SALARIO',
      ),
    );

    final serializado = almacen.leer('alta.borrador.v1');
    final json = jsonDecode((await serializado)!);
    expect(serializado, isNot(contains('secreto-no-persistir')));
    expect(serializado, isNot(contains('/tmp/')));
    expect(json['codigoConfirmado'], isNull);
    expect(json['datos']['nombres'], 'Rosa');
    expect(json['perfil']['origenDeFondos'], 'SALARIO');
  });

  test(
    'al reanudar vuelve a pedir OTP y adjuntos desde cualquier paso KYC',
    () async {
      final almacen = AlmacenEnMemoria();
      final borrador = BorradorDeAlta(almacen, ahora: () => ahora);
      await borrador.guardar(
        const EstadoAlta(
          paso: PasoAlta.contrato,
          datos: DatosPersonales(telefono: '+59178123456'),
          codigoConfirmado: true,
          rutaAnverso: '/tmp/ci.jpg',
        ),
      );

      final restaurado = await borrador.restaurar();

      expect(restaurado?.paso, PasoAlta.celular);
      expect(restaurado?.codigoConfirmado, isFalse);
      expect(restaurado?.rutaAnverso, isNull);
      expect(restaurado?.resumido, isTrue);
    },
  );

  test(
    'borra los borradores expirados y no restaura datos personales',
    () async {
      final almacen = AlmacenEnMemoria();
      final guardador = BorradorDeAlta(almacen, ahora: () => ahora);
      await guardador.guardar(
        const EstadoAlta(datos: DatosPersonales(nombres: 'No restaurar')),
      );
      final lector = BorradorDeAlta(
        almacen,
        ahora: () => ahora.add(const Duration(hours: 24, seconds: 1)),
      );

      expect(await lector.restaurar(), isNull);
      expect(await almacen.leer('alta.borrador.v1'), isNull);
    },
  );

  test('conserva centavos como texto y descarta un número legado', () async {
    final almacen = AlmacenEnMemoria();
    final borrador = BorradorDeAlta(almacen, ahora: () => ahora);
    await borrador.guardar(
      const EstadoAlta(
        datos: DatosPersonales(nombres: 'Rosa'),
        montoMensualEstimado: '125.05',
      ),
    );

    final serializado = (await almacen.leer('alta.borrador.v1'))!;
    final json = jsonDecode(serializado) as Map<String, dynamic>;
    expect(json['perfil']['montoMensualEstimado'], '125.05');
    expect((await borrador.restaurar())?.montoMensualEstimado, '125.05');

    (json['perfil'] as Map<String, dynamic>)['montoMensualEstimado'] = 125.05;
    await almacen.guardar('alta.borrador.v1', jsonEncode(json));
    final legado = await borrador.restaurar();
    expect(legado?.datos.nombres, 'Rosa');
    expect(legado?.montoMensualEstimado, isEmpty);
  });

  test('el monto mensual opcional se puede borrar', () {
    const estado = EstadoAlta(montoMensualEstimado: '125.05');
    expect(estado.copiarCon(montoMensualEstimado: '').montoMensualEstimado, '');
  });
}
