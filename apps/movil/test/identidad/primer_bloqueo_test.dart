import 'package:aportaya_movil/pantallas/identidad/dominio/primer_bloqueo.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('primerBloqueo devuelve el primer pendiente en orden de pantalla', () {
    expect(
      primerBloqueo(const [
        (true, 'Listo'),
        (false, 'Falta tu celular.'),
        (false, 'Falta tu correo.'),
      ]),
      'Falta tu celular.',
    );
  });

  test('primerBloqueo ignora requisitos resueltos y motivos vacíos', () {
    expect(primerBloqueo(const [(true, 'Listo'), (false, '  ')]), isNull);
  });
}
