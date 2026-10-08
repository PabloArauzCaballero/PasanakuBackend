import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:patrol/patrol.dart';

import '_soporte_reinicio.dart';

void main() {
  patrolTest('01 guarda borrador sintético antes de force-stop Android', (
    $,
  ) async {
    final almacen = AlmacenDeEnsayo();
    await almacen.borrar(claveBorradorDeEnsayo);
    await abrirRegistroEnEnsayo($, almacen);
    await $(TextField).first.enterText(nombreDeEnsayo);
    await $.tester.pump(const Duration(milliseconds: 450));
    await Future<void>.delayed(const Duration(milliseconds: 300));
    expect(await almacen.leer(claveBorradorDeEnsayo), contains(nombreDeEnsayo));
  });
}
