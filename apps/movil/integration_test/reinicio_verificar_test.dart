import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:patrol/patrol.dart';

import '_soporte_reinicio.dart';

void main() {
  patrolTest('02 recupera borrador tras force-stop Android', ($) async {
    final almacen = AlmacenDeEnsayo();
    try {
      await abrirRegistroEnEnsayo($, almacen);
      await $(TextosIdentidad.registroReanudado).waitUntilVisible();
      expect(
        $.tester
            .widget<TextField>(find.byType(TextField).first)
            .controller
            ?.text,
        nombreDeEnsayo,
      );
    } finally {
      await almacen.borrar(claveBorradorDeEnsayo);
    }
  });
}
