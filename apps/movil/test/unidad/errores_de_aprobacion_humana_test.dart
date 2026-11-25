import 'dart:io';

import 'package:aportaya_movil/dominio/errores.dart';
import 'package:flutter_test/flutter_test.dart';

/// APROB-001 (garantia-entregas L2 / grupos L3 / frontends L6): `AP-CU20-02` y `AP-CU69-10` son
/// textos de una aprobación humana. Decisión (2026-11-25): se conservan en el mapa del cliente
/// mientras el servidor los siga emitiendo; se retiran junto con F2.L3.M10 / F2.L2.M06 del servidor.
/// Este test ata las dos cosas: si el contrato del servidor deja de documentarlos, falla y obliga a
/// retirarlos del cliente; si alguien los quita del cliente con el servidor emitiéndolos, también.
void main() {
  const grupos =
      '../../servicios/grupos/src/main/resources/openapi/grupos.yaml';

  test('correcto · mientras el servidor los emite, el cliente los explica', () {
    expect(tieneMensajePropio('AP-CU20-02'), isTrue);
    expect(tieneMensajePropio('AP-CU69-10'), isTrue);
    expect(
      mensajeDe('AP-CU20-02'),
      'Tu cuenta todavía no está habilitada para organizar grupos.',
    );
    expect(mensajeDe('AP-CU69-10'), contains('revisa cada ingreso'));
  });

  test('límite · el contrato de grupos todavía documenta ambos códigos', () {
    final yaml = File(grupos).readAsStringSync();
    expect(yaml, contains('AP-CU20-02'));
    expect(yaml, contains('AP-CU69-10'));
  });

  test(
    'inválido · un código de aprobación que el servidor no emite no tiene texto propio',
    () {
      // AP-CU68-15 y AP-CU90-03 no se emiten: no deben aparecer en el mapa del cliente.
      expect(tieneMensajePropio('AP-CU68-15'), isFalse);
      expect(tieneMensajePropio('AP-CU90-03'), isFalse);
      expect(
        mensajeDe('AP-CU99-99', estado: 422),
        isNot(contains('aprobación')),
      );
    },
  );
}
