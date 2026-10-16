import 'package:flutter_test/flutter_test.dart';

import 'package:aportaya_movil/pantallas/identidad/dominio/validaciones.dart';

void main() {
  group('validación de partes del nombre', () {
    test('admite tildes, ñ y ü', () {
      expect(errorCampoDeNombre('María Núñez Güemes'), isNull);
    });

    test('admite apóstrofos y guiones en nombres', () {
      expect(errorCampoDeNombre("O'Brien"), isNull);
      expect(errorCampoDeNombre('Ana-María'), isNull);
    });

    test('rechaza números y puntuación ajena al nombre', () {
      expect(errorCampoDeNombre('Juan3'), isNotNull);
      expect(errorCampoDeNombre('Ana!'), isNotNull);
    });

    test('cada parte admite hasta 30 caracteres', () {
      expect(errorCampoDeNombre('A' * 30), isNull);
      expect(errorCampoDeNombre('A' * 31), isNotNull);
    });

    test('el primer nombre es obligatorio y usa el texto Atlas', () {
      expect(errorNombres('', ''), 'Escribí tu primer nombre.');
    });

    test('el nombre combinado supera el límite de 60 si llega a 61', () {
      expect(
        errorNombres('A' * 30, 'B' * 30),
        'Entre los dos nombres no pueden pasar de 60 letras.',
      );
    });

    test('al menos uno de los apellidos debe estar escrito', () {
      expect(errorApellidos('', ''), 'Escribí al menos un apellido.');
      expect(errorApellidos('Mendoza', ''), isNull);
      expect(errorApellidos('', 'Quispe'), isNull);
    });

    test('los apellidos juntos tampoco superan 60 caracteres', () {
      expect(
        errorApellidos('A' * 30, 'B' * 30),
        'Los apellidos juntos no pueden pasar de 60 letras.',
      );
    });
  });
}
