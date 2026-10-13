import 'package:flutter_test/flutter_test.dart';

import 'package:aportaya_movil/pantallas/identidad/dominio/datos_del_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/validaciones.dart';

void main() {
  group('nombres enviados al contrato', () {
    test('omite el segundo nombre vacío', () {
      expect(const DatosPersonales(primerNombre: 'María').nombres, 'María');
    });

    test('une el primer y segundo nombre', () {
      expect(
        const DatosPersonales(
          primerNombre: 'María',
          segundoNombre: 'Fernanda',
        ).nombres,
        'María Fernanda',
      );
    });

    test('recorta espacios sin cambiar mayúsculas escritas', () {
      expect(const DatosPersonales(primerNombre: '  maría  ').nombres, 'maría');
    });

    test('respeta nombres compuestos en el primer campo', () {
      expect(
        const DatosPersonales(primerNombre: 'José Luis').nombres,
        'José Luis',
      );
    });

    test('copiarCon conserva las cuatro partes al editar otros datos', () {
      const originales = DatosPersonales(
        primerNombre: 'María',
        segundoNombre: 'Fernanda',
        apellidoPaterno: 'De la Cruz',
        apellidoMaterno: 'Quispe',
      );
      final actualizados = originales.copiarCon(telefono: '+59171000000');
      expect(actualizados.nombres, 'María Fernanda');
      expect(actualizados.apellidos, 'De la Cruz Quispe');
    });

    test('omite el apellido materno vacío', () {
      expect(
        const DatosPersonales(apellidoPaterno: 'Mendoza').apellidos,
        'Mendoza',
      );
    });

    test('usa el apellido materno si el paterno está vacío', () {
      expect(
        const DatosPersonales(apellidoMaterno: 'Quispe').apellidos,
        'Quispe',
      );
    });

    test('rechaza que falten ambos apellidos', () {
      expect(errorApellidos('', ''), 'Escribí al menos un apellido.');
    });

    test('respeta apellidos compuestos', () {
      expect(
        const DatosPersonales(apellidoPaterno: 'De la Cruz').apellidos,
        'De la Cruz',
      );
    });

    test('acepta apóstrofo', () {
      expect(errorCampoDeNombre("O'Brien"), isNull);
    });

    test('acepta guion', () {
      expect(errorCampoDeNombre('Ana-María'), isNull);
    });

    test('rechaza dígitos', () {
      expect(errorCampoDeNombre('Juan3'), isNotNull);
    });

    test('rechaza nombres armados que exceden 60 caracteres', () {
      expect(
        errorNombres('A' * 30, 'B' * 30),
        'Entre los dos nombres no pueden pasar de 60 letras.',
      );
    });
  });
}
